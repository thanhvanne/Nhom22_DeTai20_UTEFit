# UTEFitDB mới — rà soát khôi phục SePay Live

Ngày kiểm tra: 30/10/2026, múi giờ Việt Nam. Nhánh `main`, HEAD `57f19a7`.

**Kết luận: NO-GO cho bật nhận thanh toán thật lúc này. Có cơ chế phê duyệt UTEFitDB trong dự án; không cần bỏ guard, tạo quy trình mới hoặc đổi sang database khác.** Chưa thực hiện bất kỳ phê duyệt/ghi dữ liệu/cấp quyền/thay cấu hình chạy nào.

## 1. Nguyên nhân ứng dụng dừng

Đã đọc đầy đủ `src/main/java/com/utefit/service/SepayDatabaseGuard.java` và đối chiếu `SepayProperties`, `SepaySchemaSafetyConfig`, cấu hình Live, webhook, checkout, repository, đối soát thủ công và migration liên quan.

Thứ tự của guard Live:

1. `SepayProperties.requireConfigured()` kiểm tra cờ bật, môi trường, ngân hàng/HMAC, profile, HTTPS origin, cookie, schema mode, JDBC databaseName và deployment ID.
2. So catalog thật từ JDBC connection với `SEPAY_LIVE_DATABASE_NAME`.
3. Truy vấn `dbo.sepay_deployment_guard`: phải có đúng một dòng `id=1`, `environment='LIVE'`, `deployment_id` **khớp chính xác** `SEPAY_LIVE_DEPLOYMENT_ID`.
4. Không được có `TEST` trong SePay details hoặc webhook ledger.

Database vừa dựng có bảng guard nhưng **0 dòng**: lỗi tại dòng 35 là đúng thiết kế, không phải JDBC/Hibernate thất bại. Nếu đúng stack trace người dùng cung cấp, các kiểm tra cấu hình/catalog trước điểm này đã đi qua trong lần chạy đó; điều này không chứng minh quyền sở hữu ngân hàng, JDBC certificate, HTTPS thực tế hay nghiệp vụ Live đã được xác minh.

`UTEFit.DatabasePurpose=DEVELOPMENT_ONLY` **không được Java guard đọc trực tiếp**. Đây là extended property của quy trình vận hành/seed. Script phê duyệt sẵn có sẽ đồng thời ghi approval và đổi purpose thành `LIVE_FINANCIAL` trong cùng transaction. Chỉ đổi purpose không làm guard qua; chỉ thêm một dòng guard thủ công có thể làm qua kiểm tra kỹ thuật nhưng bỏ sót điều kiện an toàn. Không làm cả hai theo đường tắt.

`deployment_id` là định danh deployment, không phải secret/mã ngân hàng. Source yêu cầu 16–100 ký tự `[A-Za-z0-9_-]`, không placeholder. Với database dựng mới, DBA/chủ dự án chọn ID mới sau review; dùng cùng chuỗi ở SQL và IntelliJ. Không chép approval từ LiveDemo, không tự dùng ID cũ có sẵn trong template. Đổi hostname tunnel không tự đòi đổi ID.

## 2. Kết quả truy vấn thực tế — chỉ đọc

Kết nối kiểm tra bằng Windows Integrated Authentication, không dùng/mở mật khẩu runtime. Server trả về `LAPTOP-5TEUL3EV\SQLEXPRESS01`, catalog `UTEFitDB`, thời điểm tạo DB `2026-10-30 01:33:07.707` theo SQL Server. Đây không phải chứng nhận endpoint TCP/TLS của IntelliJ.

| Hạng mục | Quan sát |
|---|---|
| Database purpose / seed | `DEVELOPMENT_ONLY` / `V712-demo55` |
| Guard | Có schema singleton; 0 bản ghi approval |
| Schema | 28 bảng dbo; đã kiểm tra 90 cột của Payment, Membership và bốn bảng SePay/guard |
| V709 | Có `sepay_payment_details.va_number`, `sepay_webhook_transactions.sub_account`, nullable đúng thiết kế |
| V710 | Có bảng claim/audit `sepay_bank_reconciliations`, khóa ngoại, UNIQUE và CHECK bằng chứng |
| V711–V712 | Có quyền lợi/phạm vi/renewal, bảng branch snapshot và snapshot thời hạn lịch |
| V714 | Có các cột hủy/late evidence, CHECK trạng thái gồm `CANCEL_REQUESTED`, `CANCELLED`, index queue |
| V715 | Có `promotion_code varchar(50)`, `original_price numeric(15,2)`, `discount_percent numeric(5,2)` trên Membership |
| CHECK / FK | Các mục được kiểm tra đều enabled/trusted; 42 CHECK, không có trigger trong dbo |
| SePay order / webhook / claim | 0 / 0 / 0; không có SePay TEST hoặc LIVE trong các bảng này |
| Payment | CASH PAID: 20, tổng 13.800.000 VND; CASH PENDING: 1, 300.000 VND; cả 21 mang mã `DEMO-CASH-*` |
| Membership | ACTIVE 19, EXPIRED 1, PENDING 1 |
| Tài khoản demo đang hoạt động | 55 `@utefit.test`, enabled=true/status=ACTIVE: ADMIN 10, MANAGER 5, MEMBER 20, PT 20 |
| 19 phép kiểm tra nhất quán | Không phát hiện dòng vi phạm trong phạm vi truy vấn |
| Runtime theo runbook | SQL login `utefit_live_runtime` tồn tại/bật, user mapping đúng SID, không sysadmin, không sở hữu DB/schema/object |
| Quyền runtime hiện tại | Có role `db_datareader`; SELECT trên 27 bảng nghiệp vụ, **không INSERT/UPDATE/DELETE** trên các bảng này |
| Quyền guard runtime | SELECT=1; INSERT/UPDATE/DELETE/ALTER/CONTROL/TAKE OWNERSHIP=0; UPDATE từng cột=0 |
| Quyền DDL runtime đã kiểm tra | CONTROL DB, CREATE TABLE, ALTER ANY USER/ROLE/SCHEMA đều 0; server grant trực tiếp chỉ CONNECT SQL |
| Backup trong msdb | Chỉ thấy bản 18/10/2026, trước lần dựng lại DB, không checksum; chưa thấy bản ghi backup của DB mới |
| Recovery / collation | SIMPLE / SQL_Latin1_General_CP1_CI_AS |

Chưa xác minh IntelliJ thực sự dùng `utefit_live_runtime`: phải kiểm tra riêng username có hiệu lực, không suy ra từ việc Windows audit thành công. Không được dùng tài khoản DBA chạy web để né thiếu quyền.

### UNIQUE/index đã đối chiếu đúng key và filter, đều enabled

| Index | Key / filter |
|---|---|
| `uk_sepay_payment_reference` | details.payment_reference, UNIQUE |
| `uk_payments_sepay_reference` | payments.transaction_code, UNIQUE WHERE method='SEPAY' AND transaction_code IS NOT NULL |
| `uk_sepay_environment_transaction` | environment, transaction_id, UNIQUE |
| `uk_sepay_bank_reference` | environment, gateway, account_number, bank_reference, UNIQUE WHERE bank_reference IS NOT NULL |
| `uk_sepay_bank_claim` | environment, gateway, account_number, bank_reference, UNIQUE trong bảng claim dùng chung |
| `uk_sepay_refund_reference` | refund_reference, UNIQUE WHERE refund_reference IS NOT NULL |

Có PK guard và các index đối soát theo outcome/received_at, payment_reference, payment_id. CHECK guard giới hạn id=1 và environment TEST/LIVE. FK tài chính liên kết đúng Payment/Membership/User; các CHECK SePay về môi trường, số tiền dương, direction/outcome, bằng chứng manual/refund đều được đối chiếu.

19 truy vấn kiểm tra bao gồm: SePay Payment thiếu details/sai phương thức, reference khác transaction_code, sai owner Payment–Membership, nhiều Payment PENDING/PAID cho cùng Membership, snapshot thiếu hoặc lệch số tiền, ACTIVE thiếu PAID, MATCHED thiếu Payment phù hợp, PAID SePay thiếu nguồn xác nhận, trùng SePay ID/bank reference/claim, nhiều MATCHED, conflict fingerprint, expiry và định dạng reference.

**Giới hạn:** SePay tables đang rỗng nên không thấy lỗi không có nghĩa đã kiểm chứng luồng thanh toán thực. Đây là kiểm tra metadata/data chỉ đọc, không chạy SQL Server race/rollback/filtered-index insertion test. Không có bằng chứng chuyển tiền, quyền ngân hàng, webhook delivery, password runtime hoặc SQL TLS certificate. Không kết luận không tồn tại backup ngoài msdb; chủ máy cần cung cấp bằng chứng backup/restore riêng nếu có.

Không thấy thiếu schema SePay cần sửa trong phạm vi kiểm tra hiện tại. Không chạy lại bootstrap, seed, V706–V715 hoặc `ddl-auto=update` chỉ để giải quyết lỗi approval. Hibernate validate cũng không thay kiểm tra index/filter/locking.

## 3. Tính nhất quán của luồng thanh toán hiện có

- Tạo đơn lấy giá/thời hạn từ Membership snapshot, không lấy amount từ frontend. Reference là prefix 2–5 chữ hoa + 24 hex ngẫu nhiên (96 bit); Payment.transactionCode và details.paymentReference dùng cùng mã. Details lưu bank/gateway/main account/VA/expiry tại thời điểm tạo.
- Start/reuse khóa theo User → Membership → Payment, không chiếm Payment CASH/VNPAY đang chờ, không đổi mã/hạn của đơn dùng lại. Phải là Member ACTIVE/enabled, đúng owner và gói/chi nhánh hợp lệ.
- Controller nhận đúng POST JSON `/payments/sepay/webhook/live`, giới hạn raw body 32 KiB, Live cần HTTPS. Chỉ hai POST webhook được miễn CSRF/login; Member/Admin vẫn áp dụng RBAC/CSRF và ownership hiện có.
- HMAC kiểm tra `timestamp + '.' + raw body`, SHA256, constant-time, timestamp ±300 giây, không chấp nhận header chữ ký/timestamp lặp. JSON strict, id nguyên dương, amount VND nguyên dương; không nhận API-key/no-auth thay HMAC.
- Webhook chỉ tự PAID khi tiền vào đúng main account/gateway, một reference đầy đủ trong nội dung, đúng Payment/Membership/owner/environment, amount bằng snapshot; VA payload phải khớp VA snapshot khi có, cấu hình VA cũng phải khớp snapshot.
- Còn cần Payment/Membership PENDING, chưa có REVIEW trước đó, gói/chi nhánh/Member đủ điều kiện; cả giờ xử lý hiện tại và transactionDate phải qua điều kiện thời hạn. Đến sau expiry có thể REVIEW dù tiền ngân hàng tới trước hạn.
- UNIQUE SePay ID/bank reference cùng khóa pessimistic và claim chung bảo vệ xử lý lặp. Cùng ID/bank reference nhưng fingerprint khác lưu conflict, không kích hoạt lại. Khi không có bank reference, bảo vệ ID vẫn tồn tại; không thể tuyên bố chống mọi giao dịch ngân hàng trùng ngữ nghĩa với ID khác mà không có mã tham chiếu.
- Receipt MATCHED, Payment PAID, Membership ACTIVE/thời hạn và thông báo được ghi cùng transaction; lỗi activation rollback. Controller ACK thành công sau khi service transaction trở về; collision được kiểm tra lại sau rollback, lỗi persistence tạm trả 503 để retry. ACK 200 cũng có thể chỉ là REVIEW/IGNORED, không đồng nghĩa đã PAID.
- Polling/QR GET không xác nhận thanh toán. API lookup tùy chọn chỉ đọc, không nhập giao dịch/kích hoạt; UUID API không phải ID số của webhook.
- Đối soát thủ công **đã tồn tại** ở `/admin/payments/sepay-reconciliation`, công tắc `SEPAY_MANUAL_RECONCILIATION_ENABLED` mặc định false. Ghi nhận/kích hoạt/ghi hoàn vẫn cần guard, Admin ACTIVE/enabled được đọc lại từ DB, bằng chứng ngân hàng, claim UNIQUE, trạng thái/thời hạn phù hợp. Không phải cách phê duyệt deployment; không tạo lệnh chuyển tiền/hoàn tiền qua ngân hàng. Không dùng để hợp thức hóa fixtures hoặc sửa PAID bằng SQL.
- Cancellation V714 giữ lịch sử: SePay đã phát hành cần Admin xử lý; webhook vào đơn hủy/chờ hủy sẽ REVIEW, không âm thầm cấp gói. Promotion V715 vẫn dùng quoted price đã chốt.

## 4. Tài liệu hiện hành và các mô tả cũ

Đối với **UTEFitDB mới đang giữ lại**, dùng `UTEFITDB_SEPAY_LIVE_DBA.md` và bộ `database/operations/UTEFitDB_01...03...`; đối chiếu source/migration hiện tại, không dùng kết quả kiểm tra lịch sử như hiện trạng máy hôm nay.

Các tài liệu đã đối chiếu: `SEPAY_INTEGRATION.md`, `SEPAY_LIVE_CHECKLIST.md`, `SEPAY_LIVE_READINESS.md`, `SEPAY_PRELIVE_VERIFICATION.md`, `SEPAY_LIVE_LAPTOP_THUC_TE.md`, `SEPAY_BIDV_VA.md`, `SEPAY_MANUAL_RECONCILIATION.md`, `SEPAY_CHECKOUT_UI.md`, `UTEFITDB_SEPAY_LIVE_DBA.md`; profile/config mẫu, V706/V708/V709/V710 và snapshot Membership V711/V712/V714/V715; bộ bootstrap LiveDemo và scripts operations.

- Nhận xét cũ “chưa hỗ trợ VA” đã được V709/source hiện tại thay thế.
- Nhận xét cũ “chưa có manual reconciliation/admission switch” không còn đúng: source hiện có cả hai.
- Bootstrap LiveDemo cũ nói 25 bảng/V701–V708, không phải yêu cầu chạy lại trên UTEFitDB có 28 bảng.
- Số liệu runtime chưa mapping trong báo cáo 26/10 không còn đúng hôm nay: hiện mapping đúng nhưng chỉ đọc.
- Các đoạn nhắc Payment #1/Membership #2 thuộc sự cố DB cũ. Không áp đặt ý nghĩa đó lên ID trùng số của DB vừa seed lại.

**Cảnh báo quan trọng:** file `database/operations/UTEFitDB_03_live_approval.template.sql` hiện trên đĩa có cả 5 cờ xác nhận bằng `1` và ID cũ `UTEFIT_LIVE_UTEFITDB_20261026`, trái mô tả “refuses by default” ở đầu file/tài liệu. Không chạy nguyên trạng, không coi những giá trị này là phê duyệt của bạn. Chưa sửa file này trong lượt kiểm tra; chỉ bản làm việc riêng của DBA mới được điền sau khi từng điều kiện thực sự đạt.

## 5. Trình tự chuẩn bị — chưa áp dụng

### A. Đóng băng phạm vi và backup trước mọi thay đổi

1. Giữ database UTEFitDB; không reset/seed/copy approval hoặc financial rows từ QA/LiveDemo. Ghi nhận instance/catalog/source version, cấu hình riêng, người giữ backup và người trực đối soát.
2. Nếu còn hệ thống cũ nhận callback, lập cutover trước khi dừng nó. Không giả định tắt app ngăn ngân hàng nhận tiền theo QR đã phát hành.
3. DBA chọn đường dẫn `.bak` mới, ngoài Git, SQL Server service có quyền ghi; bảo quản quyền truy cập backup vì chứa dữ liệu cá nhân/hash. SQL dưới đây là bản điều chỉnh **chỉ tên database** từ backup template sẵn có, mặc định từ chối. Agent chưa chạy:

```sql
USE [UTEFitDB];
SET NOCOUNT ON;
DECLARE @OperatorApproved bit=0;
DECLARE @BackupFile nvarchar(4000)=NULL;
IF DB_NAME()<>N'UTEFitDB' OR @@TRANCOUNT<>0
    OR @OperatorApproved<>1 OR @BackupFile IS NULL OR LEN(@BackupFile)<10
    THROW 50870,'Choose and approve a new private backup path first.',1;
BACKUP DATABASE [UTEFitDB] TO DISK=@BackupFile WITH COPY_ONLY,CHECKSUM;
RESTORE VERIFYONLY FROM DISK=@BackupFile WITH CHECKSUM;
```

Chỉ sau duyệt mới đặt `@OperatorApproved=1` và đường dẫn thật. Không thêm INIT/FORMAT/REPLACE. VERIFYONLY không thay phục hồi thử: restore sang DB kiểm tra riêng, MDF/LDF riêng, không ghi đè UTEFitDB/LiveDemo/QA, không cho app Live trỏ vào bản phục hồi. Xác minh counts/constraints/source compatibility và thời gian phục hồi. DB hiện SIMPLE: chưa có bằng chứng kế hoạch log/PITR; DBA quyết định RPO/RTO, không tự đổi recovery model.

### B. Quyền runtime và dữ liệu demo

1. Chạy lại preflight sẵn có trong phiên DBA chọn đúng UTEFitDB: `database/operations/UTEFitDB_01_live_preflight.sql`.
2. Runtime hiện có `db_datareader`: template 02 sẽ từ chối với 51417 vì **không cho bất kỳ database-role membership**. Không cấp thêm db_owner/db_datawriter. DBA rà quyền/dependencies trước; câu SQL điều chỉnh riêng dưới đây chỉ để xem xét, chưa chạy và không phải phê duyệt Live:

```sql
USE [UTEFitDB];
ALTER ROLE [db_datareader] DROP MEMBER [utefit_live_runtime];
```

3. Sau phê duyệt quyền riêng và backup, DBA dùng bản riêng của `database/operations/UTEFitDB_02_runtime_permissions.template.sql`, chỉ đổi `@Apply` từ 0 thành 1, giữ toàn bộ chốt kiểm tra. Template cấp SELECT/INSERT 27 bảng, UPDATE 25 bảng trừ snapshots bất biến, DELETE chỉ favorites/manager_branch_assignments/membership_plan_branches; guard SELECT-only, hạn chế DDL. Không đổi password/login hoặc quyền LiveDemo.
4. Chạy preflight lại và kiểm tra chính login/runtime qua JDBC/TLS có hiệu lực. Không cần thực hiện giao dịch thật để kiểm tra quyền; diễn tập transaction/idempotency trên SQL Server bản sao cô lập, không gắn webhook ngân hàng.
5. Tài khoản demo: chủ dự án duyệt việc ngừng sử dụng credential công khai và tạo/bảo vệ tài khoản thật qua nghiệp vụ sẵn có; giữ lịch sử. Điều kiện script 03 chính xác là **không còn email `@utefit.test` có cả enabled=1 và status=ACTIVE**. Chỉ đổi mật khẩu không làm điều kiện này hết; không đổi email để né chốt. Không tự bulk UPDATE/DELETE 55 users trong lượt này. Phải giữ lối quản trị hợp lệ, tránh khóa hết Admin trước khi có Admin thật.
6. Rà soát cả credential công khai ngoài hậu tố `.test`; rà ý nghĩa 21 CASH demo và báo cáo doanh thu. Các khoản demo PAID không phải tiền ngân hàng thật; không chuyển thành SEPAY, xóa hay sửa lịch sử để “sạch DB”. Nếu giữ chúng, chủ dự án phải chấp thuận rõ cách phân biệt/demo accounting theo báo cáo hiện có trước Live; chưa có bằng chứng việc này đã hoàn tất.
7. `UTEFitLiveDemo` vẫn tồn tại trên instance. Chưa đọc/thay đổi financial rows của DB đó trong lượt này. Chủ dự án cần đối soát đơn cũ, tiền đã nhận, pending/review và delivery retry với ngân hàng trước cutover. Reference cũ tới UTEFitDB mới sẽ UNKNOWN_REFERENCE/REVIEW, không cập nhật đơn ở DB cũ. Không cho hai consumer độc lập cùng tiếp nhận mà không có kế hoạch tránh bỏ sót/nhận nhầm.

### C. Cấu hình IntelliJ riêng, không bật tiền trong bước chuẩn bị

Không ghi đè `.local/application-local.properties`, không chia sẻ secret hoặc lưu chúng vào Git. Spring không tự nạp `.env`; nhập từng biến trong Run Configuration. Chỉ đổi `DB_URL` có thể không thắng literal trong local properties; dùng `SPRING_DATASOURCE_*` và kiểm tra cấu hình có hiệu lực. Không đoán SQL host/port từ tên instance audit.

```text
SPRING_PROFILES_ACTIVE=sepay,sepay-live
SPRING_DATASOURCE_URL=jdbc:sqlserver://<HOST>:<PORT>;databaseName=UTEFitDB;encrypt=true;trustServerCertificate=false
SPRING_DATASOURCE_USERNAME=utefit_live_runtime
SPRING_DATASOURCE_PASSWORD=<nhập riêng>
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
SPRING_SQL_INIT_MODE=never
SEPAY_ENVIRONMENT=live
SEPAY_LIVE_DATABASE_NAME=UTEFitDB
SEPAY_LIVE_DEPLOYMENT_ID=<ID mới đã thống nhất, khớp SQL>
SEPAY_LIVE_PUBLIC_URL=https://<hostname thực tế>
SEPAY_ENABLED=false
SEPAY_LIVE_ENABLED=false
SEPAY_LIVE_ACCEPT_NEW_PAYMENTS=false
SEPAY_LIVE_APPROVAL=
SEPAY_WEBHOOK_AUTH=hmac
SEPAY_PAYMENT_PREFIX=UTE
SEPAY_PAYMENT_MINUTES=15
SEPAY_TEST_QR_PREVIEW=false
SEPAY_LIVE_API_LOOKUP_ENABLED=false
SEPAY_MANUAL_RECONCILIATION_ENABLED=false
```

Các giá trị `<...>` không thể dùng nguyên văn. Chính sách hiện tại cho phép chuẩn bị UI khi xử lý SePay bị tắt; đây không phải cho phép thu tiền hoặc bỏ guard. Chưa sửa Run Configuration/env của bạn.

Giữ/điền riêng các biến ngân hàng **sau khi tự kiểm tra**:

- `SEPAY_LIVE_BANK_CODE`: định danh dùng tạo QR, 2–32 chữ/số; `SEPAY_LIVE_GATEWAY`: tên gateway thực tế của payload, không tự cho rằng luôn giống bank code.
- `SEPAY_LIVE_ACCOUNT_NUMBER`: tài khoản chính; `SEPAY_LIVE_ACCOUNT_NAME`: chủ tài khoản thật có quyền nhận tiền.
- `SEPAY_LIVE_VA_NUMBER`: VA nhận trong QR nếu dùng VA; bắt buộc riêng và khác main khi BIDV theo source. Các ngân hàng khác có yêu cầu VA từ nhà cung cấp cũng cần đối chiếu thực tế, không chỉ dựa code có cho rỗng.
- `SEPAY_LIVE_WEBHOOK_SECRET`: HMAC Live riêng, ít nhất 32 ký tự, không placeholder, khác Test; nhập ngoài source, không gửi vào chat. Token API lookup không phải HMAC secret.

Prefix được phép 2–5 chữ hoa; thời hạn 5–60 phút. Profile Live đặt port 8080/bind 127.0.0.1, forwarded headers framework, session/JWT Secure, HttpOnly/Lax cho session, WebSocket origin theo Live URL. Không ghép qa/test, không hạ cookie/HTTPS/CSRF. Giữ mail/JWT/Cloudinary riêng như hiện có.

Lưu ý `application.properties` mặc định vẫn có `trustServerCertificate=true`; Java guard **không xác nhận TLS certificate của JDBC**. Cấu hình vận hành Live trong runbook yêu cầu kiểm chứng certificate/truststore/hostname, không chỉ thấy JDBC connect thành công. Audit local lần này dùng TrustServerCertificate để đọc metadata qua Windows auth, không đổi ứng dụng và không phải bằng chứng TLS Live đạt.

### D. Chủ tài khoản xác minh ngân hàng, HTTPS và webhook

1. Tự kiểm tra qua ngân hàng nhận/SePay Dashboard Live: quyền sở hữu/ủy quyền, main account/chủ tài khoản/gateway/VA đúng, tài khoản đang hoạt động và kết nối còn tốt. Không gửi password/OTP/secret cho agent. Không lấy fixture trong test Java làm cấu hình ngân hàng.
2. Đối chiếu mẫu payload/delivery thực tế đã có, đã che riêng tư: accountNumber là main và subAccount phù hợp VA; UTEFit dùng VA snapshot làm số nhận QR. Không tự backfill snapshot đơn cũ cho khớp cấu hình mới. SePay hiện nêu BIDV cần VA; xác minh trực tiếp tài khoản đã liên kết. [Tài khoản/VA chính thức](https://developer.sepay.vn/vi/sepay-webhooks/tai-khoan-ngan-hang).
3. Origin HTTPS công khai đúng chứng chỉ, proxy/tunnel tới loopback; không mở trực tiếp 8080 ra Internet, không cho client ngoài giả forwarded headers. Trình duyệt login/secure cookie/redirect phải đúng origin. URL tunnel đổi thì cập nhật đồng bộ origin và webhook/restart có kiểm soát; Quick Tunnel không chứng minh uptime nhận tiền liên tục.
4. Review webhook hiện có, không tự nhân bản: POST `https://<hostname>/payments/sepay/webhook/live`, application/json, tiền vào, tài khoản/VA cụ thể, HMAC-SHA256, retry/cảnh báo. Không để login/challenge chặn webhook, không tắt security toàn hệ thống. Bộ lọc bỏ mã/prefix có thể làm mất khoản cần REVIEW; duyệt chính sách lọc theo runbook. [Cấu hình webhook chính thức](https://developer.sepay.vn/vi/sepay-webhooks/tao-webhook).
5. Cấu trúc nhận diện: prefix đúng app, hậu tố 24 chữ/số cho reference hex UTEFit. HMAC giữ nguyên bytes JSON và hai headers; kiểm tra giờ/NTP. Công thức timestamp.raw_body và cửa sổ 5 phút phù hợp [tài liệu HMAC SePay](https://developer.sepay.vn/vi/sepay-webhooks/xac-thuc).
6. Không gửi payload mô phỏng khớp đơn Live: source không có dry-run. `ACCEPT_NEW_PAYMENTS=false` vẫn cho callback cập nhật đơn cũ. Nút Gửi thử có thể dùng id=0, trong khi parser UTEFit yêu cầu id>0; 400 khi đó không tự có nghĩa HMAC sai. Chưa gửi bất cứ request test/live nào trong lượt này.

### E. Phê duyệt database mới — quy trình chính xác đã có, CHƯA THỰC HIỆN

Sau khi A–D và SQL Server QA/cutover đạt, chủ dự án cho phép DBA thực hiện riêng. Không có nút Admin web cấp deployment approval.

1. Mở **bản riêng** của `database/operations/UTEFitDB_03_live_approval.template.sql`; kết nối bằng deployment sysadmin, chọn UTEFitDB, phiên riêng `@@TRANCOUNT=0`.
2. Do bản gốc đã điền sẵn, trước khi rà soát trả phần tham số của **bản riêng** về trạng thái chưa duyệt như sau, giữ nguyên toàn bộ phần kiểm tra/transaction phía sau:

```sql
DECLARE @Approve bit=0;
DECLARE @OfflinePreflightReviewed bit=0;
DECLARE @KnownDemoCredentialsRetired bit=0;
DECLARE @OldLiveDemoOrdersReconciled bit=0;
DECLARE @DemoCashAccountingReviewed bit=0;
DECLARE @DeploymentId varchar(128)=NULL;
```

3. Chỉ khi từng xác nhận có bằng chứng thật và người chịu trách nhiệm đồng ý, tự đặt **5 cờ tương ứng thành 1**, điền ID mới 16–100 ký tự theo source, trùng chính xác cấu hình deployment. Không đặt cờ 1 chỉ để hết lỗi; nếu còn một điều kiện chưa đạt thì dừng.
4. Review/chạy **toàn bộ script sẵn có**, không trích riêng INSERT. Nó kiểm tra sysadmin, schema, không demo ACTIVE/enabled, không TEST details/receipts; impersonate runtime để kiểm tra SELECT-only guard và từng cột; lock guard `UPDLOCK,HOLDLOCK`, từ chối nếu đã có bất cứ row; INSERT duy nhất `(1,'LIVE',@DeploymentId,SYSDATETIME())`, update/add purpose `LIVE_FINANCIAL`, COMMIT cùng transaction. Giữ seed marker; có TRY/CATCH/REVERT/ROLLBACK, không tự bật cờ app.
5. Script 03 **không tự chứng minh** backup/restore, DML nghiệp vụ, ngân hàng, TLS hay đối soát đơn cũ; những cờ khai báo là xác nhận trách nhiệm của người chạy, không phải kiểm tra tự động thay thế. Bản 02 phải được xử lý trước vì runtime hiện không ghi được nghiệp vụ.
6. Nếu đã tồn tại approval ngoài dự kiến, dừng và điều tra, không UPDATE/DELETE/ghi đè. Không chép row/approved_at cũ từ LiveDemo.

Sau thực hiện bởi DBA, kiểm tra chỉ đọc:

```sql
USE [UTEFitDB];
SELECT @@SERVERNAME AS server_name,DB_NAME() AS database_name;
SELECT id,environment,deployment_id,approved_at FROM dbo.sepay_deployment_guard;
SELECT name,CONVERT(nvarchar(200),value) AS value
FROM sys.extended_properties
WHERE class=0 AND name IN (N'UTEFit.DatabasePurpose',N'UTEFit.DemoSeed.Version');
SELECT COUNT_BIG(*) AS test_details
FROM dbo.sepay_payment_details WHERE environment='TEST';
SELECT COUNT_BIG(*) AS test_receipts
FROM dbo.sepay_webhook_transactions WHERE environment='TEST';
SELECT COUNT_BIG(*) AS active_public_demo_accounts
FROM dbo.users WHERE email LIKE '%@utefit.test' AND enabled=1 AND status='ACTIVE';
```

Kỳ vọng sau phê duyệt hợp lệ: một row guard LIVE/ID mới khớp, purpose LIVE_FINANCIAL, seed marker giữ nguyên, ba count cuối bằng 0. Chạy preflight lại, xác nhận quyền từng bảng/cột bằng runtime.

### F. Bật xử lý và bật đơn mới là hai quyết định khác nhau

Chỉ sau phê duyệt DB/config/callback riêng, chủ dự án mới tự đặt:

```text
SEPAY_ENABLED=true
SEPAY_ENVIRONMENT=live
SEPAY_LIVE_ENABLED=true
SEPAY_LIVE_APPROVAL=I_APPROVE_SEPAY_LIVE
SEPAY_LIVE_ACCEPT_NEW_PAYMENTS=false
```

Restart bằng runtime đúng catalog/ID. Guard phải qua **nguyên vẹn**, không sửa code. Bước này đã cho phép callback xác thực ghi nhận thanh toán nên không được coi là dry-run, dù chưa mở đơn mới.

Chỉ sau quyết định GO tiếp theo và kế hoạch webhook/cutover/giám sát hoàn tất mới đặt `SEPAY_LIVE_ACCEPT_NEW_PAYMENTS=true`, restart có kiểm soát. Chủ tài khoản/người dùng tự quyết định và thực hiện giao dịch đầu được duyệt; agent không chuyển tiền. Kiểm tra ngân hàng → delivery HMAC → receipt MATCHED → Payment PAID → Membership đúng quyền lợi/thời hạn → thông báo/doanh thu không cộng ledger hai lần. Chưa có kết quả này thì không gọi là Live VERIFIED.

Nếu lỗi: dừng tạo đơn mới bằng admission flag; bảo toàn callback/ledger/DB cho đơn đã phát hành theo quyết định vận hành. Không ép PAID/CASH, không chuyển thêm để chữa đơn cũ, không restore backup cũ đè giao dịch mới, không xóa receipt hoặc bỏ HMAC/guard. Tắt toàn bộ SePay có thể trả 503 callback; chỉ dùng theo kế hoạch incident và đối soát.

## 6. Kiểm thử, bằng chứng và những gì không thay đổi

Đã chạy Maven offline compile/test các suite hiện có, H2 in-memory riêng, mock SePay/config/HTTP; không khởi động app Live, không gọi ngân hàng/SePay API hoặc SQL Server bằng test:

| Suite | PASS |
|---|---:|
| SepayDatabaseGuardTest | 4 |
| SepayLiveSafetyTest | 8 |
| SepayWebhookSecurityTest | 5 |
| SepayPaymentIntegrationTest | 51 |
| SepayManualReconciliationIntegrationTest | 44 |
| SepayReconciliationLookupTest | 4 |
| LiveTunnelSecurityTest | 3 |
| LiveDemoSchemaPreparationTest | 1 |
| Tổng | **120; 0 failure/error/skipped, BUILD SUCCESS** |

Các test bao phủ cấu hình/guard, HMAC, snapshot/VA, ownership/CSRF, duplicate/concurrency/rollback, manual evidence/refund và cancellation. H2/mock không chứng nhận locking/transaction SQL Server thật hay delivery/TLS/ngân hàng Live.

Bằng chứng cục bộ được giữ trong thư mục ignored `.local/sepay-live-readiness/`:

- `audit-readonly.sql`: truy vấn metadata/tổng hợp/nhất quán, SELECT quyền qua EXECUTE AS LOGIN rồi REVERT; không INSERT/UPDATE/DELETE/DDL business hoặc approval.
- `run-readonly-audit.ps1`: runner Windows-auth, ghi JSON local; session SET NOCOUNT/LOCK_TIMEOUT, không sửa cấu hình SQL.
- `database-audit.json`: kết quả tổng hợp/metadata/quyền, không lưu mật khẩu, số tài khoản thật hoặc raw webhook.
- `isolated-tests.log`: log test H2.

**Thay đổi bàn giao trong lượt này:** chỉ thêm báo cáo này và các file audit/log local ignored. Không sửa Java/backend/security/properties/migration/template approval; không đổi DatabasePurpose, role, user, Payment, Membership hoặc dữ liệu SePay; không bật flags, mở tunnel, gửi/replay webhook, stage/commit/push. Mọi thay đổi giao diện đang có được giữ nguyên trên main.

**Dừng chờ chủ dự án duyệt:** backup/restore, kế hoạch tài khoản demo/CASH, quyền runtime, đối soát LiveDemo/cutover, ngân hàng/VA/HTTPS/HMAC, ID deployment và quyết định DBA approval trước; bật callback và nhận đơn mới phải được duyệt riêng sau đó.
