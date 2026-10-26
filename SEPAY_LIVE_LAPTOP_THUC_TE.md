# UTEFit — SePay Live nhận tiền thật trên laptop

> Bổ sung BIDV VA: xem `SEPAY_BIDV_VA.md`. Với UTEFitLiveDemo đã tồn tại, chỉ review/áp dụng V709 khi bạn duyệt; không chạy lại bộ bootstrap/cấp approval. SEPAY_LIVE_ACCOUNT_NUMBER là main, biến mới SEPAY_LIVE_VA_NUMBER là số nhận trong QR cho đơn mới. Không tự xác nhận đơn cũ thiếu VA/Webhook.

Ngày chuẩn bị: 26/10/2026. Branch `feat/giai-doan-7`, HEAD `2dadc44`; giữ QR `a8798f7` và mọi thay đổi SePay chưa commit. Đây là demo có giám sát trên laptop, không phải dịch vụ luôn sẵn sàng. Không yêu cầu VPS, hosting hoặc mua tên miền; không quay lại luồng Test Mode.

**Agent chưa tạo database, chạy migration, tạo approval, bật Live, mở Tunnel, gửi request Webhook Live, kết nối ngân hàng hoặc chuyển tiền.** Chỉ đọc metadata SQL Server và chuẩn bị source/script/test/tài liệu. Các bước ghi trong tài liệu là để bạn tự thực hiện sau phê duyệt.

## 1. Kết quả kiểm tra laptop/source

| Mục | Quan sát thực tế/phạm vi |
|---|---|
| SQL endpoint local | `localhost:1433` lấy từ cấu hình hiện có, không công bố credential |
| SQL Server | SELECT metadata bằng Windows Authentication trả version17.0.1000.7; không dùng SQL password |
| UTEFitDB | ONLINE,22 bảng; không thấy bảng SePay/guard và cột QR purpose/checkin_id trong kiểm tra metadata. Không dùng DB này nhận tiền |
| UTEFitQA_GD7_20261025 | ONLINE,24 bảng; có SePay,4 UNIQUE cần thiết đang enabled (2 filtered), QR Checkout và Review Video; chưa thấy guard V708 |
| UTEFitLiveDemo | Không xuất hiện trong danh sách metadata đã đọc; cần bạn tạo mới, không clone dữ liệu QA |
| Công cụ | cloudflared và sqlcmd đã có trên máy; agent không cài hay chạy Tunnel |
| Source | Payment snapshot/HMAC/expiry/idempotency/RBAC/guard đã có; giữ VNPay chỉ cho lịch sử/callback cũ, không tạo mới |

Lưu ý cú pháp: JDBC dùng `localhost:1433`, SSMS/sqlcmd dùng `localhost,1433` hoặc `tcp:localhost,1433`. Lần kiểm tra metadata đầu dùng sai dạng endpoint nên lỗi53; chuẩn hóa host/port rồi SELECT thành công. `-C` chỉ được dùng cho kết nối metadata loopback này; **không chứng minh certificate SQL của cấu hình Live đã được xác thực**.

Không đọc dữ liệu tài khoản/ngân hàng/Payment thực tế từ hai DB cũ, không sao chép bảng hoặc dữ liệu. Không restore/ghi đè application.properties hoặc chạy target/gd7-qa-env.ps1.

### Phần bổ sung tối thiểu trong lượt này

- Profile Live đặt JSESSIONID Secure/HttpOnly/SameSite=Lax; UTEFIT_AUTH vốn đã Secure/HttpOnly/Lax theo controller và profile.
- WebSocket allowed origins lấy đúng SEPAY_LIVE_PUBLIC_URL thay localhost mặc định. Không dùng wildcard. Payment polling là HTTP cùng origin, không phụ thuộc WebSocket.
- SQL bootstrap mới ở database/live-demo/, không sửa migration V701–V708. Schema core19 bảng xuất từ JPA hiện tại bằng SQLServerDialect với provider **ném lỗi nếu có kết nối DB**; test so snapshot với metadata. Sau đó chạy migration gốc để tạo/hoàn thiện5 bảng giai đoạn7 và guard: tổng25 bảng.
- Không dùng database/DB_Khung_1.sql trực tiếp: file đó DROP UTEFitDB. Không dùng DB_Data_2.sql, QA initializer hoặc dump có dữ liệu để seed Live.

## 2. CẦN BẠN LÀM TRONG SSMS

### 2.1 Nguyên tắc và thứ tự

Mở SSMS, kết nối `localhost,1433` bằng tài khoản DBA được phép. Mỗi file SQL mở Query → **SQLCMD Mode**, dùng phiên riêng không có transaction khác. Script có :On Error exit; **dừng ngay khi lỗi**, không bỏ THROW để chạy tiếp. Sửa RepoRoot trong file02 nếu workspace khác đường dẫn hiện tại.

Không chạy các template *.template.sql nguyên xi: chúng khóa mặc định bằng @OperatorApproved=0 và giá trị cần điền=NULL. Sau khi duyệt, tạo bản làm việc **ngoài repository**, nhập giá trị riêng tại đó; không đưa secret/hash, file .bak hoặc bản SQL chứa dữ liệu riêng vào Git.

| Thứ tự | File | Việc bạn thực hiện / kết quả mong đợi |
|---|---|---|
| 1 | database/live-demo/00_create_database.sql | Sau duyệt, tạo duy nhất UTEFitLiveDemo; nếu đã có thì dừng, không xóa/ghi đè. Yêu cầu SQL Server>=2019 cho UTF8; phiên bản đã quan sát đáp ứng. DB mới ONLINE, collation Latin1_General_100_CI_AS_SC_UTF8 |
| 2 | database/live-demo/01_schema.sql | Chỉ vào DB LiveDemo rỗng; transaction tạo19 bảng core từ Entity, không user/password/Payment. Có sẵn snapshot giá/thời hạn, enum hiện tại và media columns |
| 3 | database/live-demo/02_apply_gd7.sql | Chạy V701→V708 bằng :r, mỗi migration có batch riêng. Sau cùng25 bảng, guard rỗng; Payment/Membership/receipt vẫn rỗng |
| 4 | database/live-demo/04_verify_readonly.sql | Chỉ SELECT schema/index/constraint/guard/tổng trạng thái, không sửa DB. Kiểm tra bảng25,4 UNIQUE đúng cột/filter/enabled, CHECK/FK trusted, không TEST |
| 5 | database/live-demo/03_catalog.template.sql | Bạn duyệt **giá gói thật dùng demo**, thời hạn, địa điểm; hoặc tạo catalog qua Admin sau bootstrap. Chỉ thêm1 chi nhánh và1 gói, không tạo thanh toán. Giá mặc định NULL nên không tự áp số tiền |
| 6 | database/live-demo/05_runtime_permissions.template.sql | Sau tạo login riêng bằng SSMS UI, cấp DML app + SELECT guard, DENY ghi/ALTER guard. Không sysadmin/db_owner/DDL cho runtime |
| 7 | database/live-demo/07_backup.template.sql | Chọn đường dẫn .bak mới bên ngoài repo mà SQL Server service ghi được; COPY_ONLY/CHECKSUM + VERIFYONLY. Không dùng INIT/REPLACE. Thử restore thực tế sang DB riêng trước giao dịch đầu |
| 8 | database/live-demo/08_promote_admin.template.sql | Chỉ nếu chưa có Admin: đăng ký/xác thực email chủ dự án qua UI trước, rồi DBA cấp ADMIN cho đúng1 tài khoản ACTIVE/enabled. Không seed mật khẩu/hash hoặc tự đổi enabled/status. Đăng xuất/đăng nhập lại |
| 9 | database/live-demo/06_approval.template.sql | **Chưa chạy cho đến quyết định phê duyệt riêng.** DBA thêm1 row approval, không tự bật cờ nhận tiền. Không được overwrite row đã có |

File01 là baseline mới cho DB rỗng, không phải migration thay thế để chạy lên DB cũ. Nó gồm một số cột giai đoạn7 hiện tại; V701/V704 có guard tồn tại nên có thể bỏ qua phần đã có.5 bảng favorites/manager/QR/SePay chưa được seed từ JPA vào baseline để V702/V703/V705/V706 tự tạo với CHECK/FK/filtered-index gốc. V707 thêm mục đích QR. V708 thêm guard rỗng.

Restore drill trong SSMS do bạn/DBA thực hiện: Databases → Restore Database → Source Device chọn .bak → Destination đổi thành **UTEFitLiveDemoRestoreCheck**, trang Files chọn file MDF/LDF mới không trùng LiveDemo, không chọn WITH REPLACE/ghi đè. Sau restore, chỉ SELECT kiểm tra schema/row count; không chạy app Live vào DB phục hồi. Không có thao tác restore nào được agent thực hiện.

**Chưa chạy các script trên SQL Server nên chưa báo T-SQL runtime PASS.** Đặc biệt V705/V706 có kiểm tra CHECK constraint; nếu engine/schema không khớp thì phải dừng review, không drop guard hoặc bỏ validation. Các migration riêng không tạo một transaction bao trùm cả chuỗi: nếu V706 lỗi sau V705 đã commit, giữ nguyên DB riêng, xác định version/schema rồi sửa theo quy trình duyệt; không chạy reset.

### 2.2 SQL kiểm tra tối thiểu, chỉ đọc

Bạn có thể mở file04 hoặc chạy riêng:

```sql
USE [UTEFitLiveDemo];
SELECT @@SERVERNAME AS server_name, DB_NAME() AS database_name;
SELECT COUNT(*) AS expected_25 FROM sys.tables
WHERE is_ms_shipped=0 AND schema_id=SCHEMA_ID('dbo');
SELECT id,environment,deployment_id,approved_at FROM dbo.sepay_deployment_guard;
SELECT COUNT(*) AS expected_zero FROM dbo.payments;
SELECT COUNT(*) AS expected_zero FROM dbo.memberships;
SELECT COUNT(*) AS expected_zero FROM dbo.sepay_webhook_transactions;
SELECT COUNT(*) AS expected_zero FROM dbo.sepay_payment_details WHERE environment='TEST';
```

Trước giao dịch đầu: bảng guard rỗng cho đến phê duyệt; không có Payment/Membership giả. Sau giao dịch thật không còn kỳ vọng các bảng tài chính rỗng, không xóa để đưa chúng về0.

DBA kiểm tra quyền runtime sau file05 (đoạn này chỉ đổi execution context rồi SELECT, không ghi dữ liệu):

```sql
USE [UTEFitLiveDemo];
EXECUTE AS USER=N'utefit_live_runtime';
SELECT HAS_PERMS_BY_NAME('dbo.sepay_deployment_guard','OBJECT','SELECT') AS expected_1,
       HAS_PERMS_BY_NAME('dbo.sepay_deployment_guard','OBJECT','INSERT') AS expected_0,
       HAS_PERMS_BY_NAME('dbo.sepay_deployment_guard','OBJECT','UPDATE') AS expected_0,
       HAS_PERMS_BY_NAME('dbo.sepay_deployment_guard','OBJECT','DELETE') AS expected_0,
       HAS_PERMS_BY_NAME(DB_NAME(),'DATABASE','ALTER') AS expected_0;
REVERT;
```

Nếu lỗi trước REVERT, DBA kết thúc phiên/REVERT trước công việc khác. Kiểm tra thêm server-role/grant CONTROL, impersonation, database ownership; table DENY không bảo vệ trước sysadmin/owner. Tạo login bằng SSMS → Security → Logins → New Login, chọn credential mạnh, chỉ map DB LiveDemo; không nhập password vào script/chat. Không tạo/cấp quyền thay bạn trong lượt này.

### 2.3 Dữ liệu nghiệp vụ và tài khoản

Không sao chép users/password hash/OTP/Membership/Payment/receipts/checkins QA. Cấu hình mail hợp lệ rồi đăng ký tài khoản chủ dự án và Member qua /register, tự nhập OTP/email; file08 chỉ bootstrap Admin sau xác thực. Dùng Admin tạo catalog hoặc template03 đã duyệt, giá từ catalog là số tiền thật sẽ thu. Không tạo nhiều đơn Live để thử màn hình.

## 3. CẦN BẠN CẤU HÌNH TRONG INTELLIJ

Run → Edit Configurations → nhân bản cấu hình Spring Boot thành **UTEFit Live Laptop**. Không sửa file application.properties; bỏ lựa chọn tự chia sẻ/lưu cấu hình chứa secret vào project/VCS. Không dùng cấu hình QA hoặc file env QA. Chỉ chạy một app trên8080; Stop app cũ trước Run Live.

Active profiles: **`sepay,sepay-live`** (hoặc SPRING_PROFILES_ACTIVE đúng giá trị này, không khai báo hai nơi mâu thuẫn). Không bật qa/test. Profile không tự bật nhận tiền hoặc tự chọn môi trường Live.

Có thể đặt VM options `-Duser.timezone=Asia/Ho_Chi_Minh` để thời gian ứng dụng nhất quán lịch Việt Nam; giữ giờ Windows/NTP đúng, không chỉnh giờ để vượt expiry/HMAC.

Nhập bằng bảng **Environment variables** của IntelliJ, từng Name/Value để JDBC URL có dấu chấm phẩy không bị tách thành nhiều biến. Không dán chuỗi chứa secret vào chat/terminal/Git. Spring không tự đọc file .env.

| Name | Value/điều kiện bạn tự nhập |
|---|---|
| SPRING_DATASOURCE_URL | `jdbc:sqlserver://localhost:1433;databaseName=UTEFitLiveDemo;encrypt=true;trustServerCertificate=false` |
| SPRING_DATASOURCE_USERNAME / SPRING_DATASOURCE_PASSWORD | Login runtime riêng và password bạn tự tạo ngoài source |
| SPRING_JPA_HIBERNATE_DDL_AUTO | validate |
| SPRING_SQL_INIT_MODE | never |
| SERVER_PORT / SERVER_ADDRESS | 8080 / 127.0.0.1 |
| SEPAY_ENVIRONMENT | live |
| SEPAY_LIVE_DATABASE_NAME | UTEFitLiveDemo |
| SEPAY_LIVE_PUBLIC_URL | Origin HTTPS Quick Tunnel hiện tại, **không slash cuối**, không path/query |
| SEPAY_LIVE_DEPLOYMENT_ID | Định danh triển khai tự chọn16–100 chữ/số/_/-, khớp row guard; không phải secret, không thay mỗi lần đổi hostname |
| SEPAY_LIVE_BANK_CODE | Mã ngân hàng dùng trong URL VietQR, kiểm tra với ngân hàng/SePay |
| SEPAY_LIVE_GATEWAY | Tên gateway thực tế trong payload, không mặc định bằng bank code |
| SEPAY_LIVE_ACCOUNT_NUMBER / SEPAY_LIVE_ACCOUNT_NAME | Số nhận và chủ tài khoản hợp lệ bạn tự nhập; không tự dùng tài khoản Test |
| SEPAY_LIVE_VA_NUMBER | VA BIDV nhận tiền cho đơn mới; giữ ACCOUNT_NUMBER là main để đối chiếu Webhook, xem SEPAY_BIDV_VA.md |
| SEPAY_LIVE_WEBHOOK_SECRET | Secret HMAC Live ngẫu nhiên>=32 ký tự, khác Test, khớp Dashboard |
| SEPAY_WEBHOOK_AUTH | hmac |
| SEPAY_PAYMENT_PREFIX | UTE hoặc prefix2–5 chữ hoa đã cấu hình trên SePay |
| SEPAY_PAYMENT_MINUTES | 5–60, mặc định15; chọn đủ thời gian demo và duyệt chính sách giao dịch muộn |
| SEPAY_TEST_QR_PREVIEW | false; không liên quan việc bật QR Live |
| SEPAY_ENABLED | false khi chuẩn bị; chỉ true sau bạn phê duyệt xử lý callback |
| SEPAY_LIVE_ENABLED | false khi chuẩn bị; chỉ true sau bạn phê duyệt Live |
| SEPAY_LIVE_ACCEPT_NEW_PAYMENTS | false cho tới khi bạn cho phép tạo Payment thật |
| SEPAY_LIVE_APPROVAL | Để trống trước duyệt; sau duyệt khớp chính xác hằng số `I_APPROVE_SEPAY_LIVE` trong source |
| SEPAY_LIVE_API_LOOKUP_ENABLED | false mặc định; tùy chọn bật tra cứu read-only sau khi duyệt |
| SEPAY_LIVE_API_TOKEN / SEPAY_LIVE_BANK_ACCOUNT_ID | Chỉ cần khi bật tra cứu; token chỉ đọc và UUID tài khoản Live, khác HMAC secret |

Cookie JWT đã Secure trong profile Live; SEPAY_COOKIE_SECURE có thể đặt true nhưng không thay thế kiểm tra effective profile. Session cookie cũng Secure/HttpOnly/Lax. WebSocket allowed origins tự lấy SEPAY_LIVE_PUBLIC_URL; nếu có override UTEFIT_WEBSOCKET_ALLOWED_ORIGINS cũ thì bỏ hoặc đặt đúng origin, không `*`.

Ứng dụng còn cần cấu hình mail/JWT/Cloudinary hợp lệ theo source: SPRING_MAIL_USERNAME, SPRING_MAIL_PASSWORD, UTEFIT_MAIL_FROM, UTEFIT_JWT_SECRET_BASE64, CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET (cùng SMTP host/port nếu khác mặc định). Tự nhập credential hợp lệ trong IntelliJ; không tạo key demo yếu hoặc sao chép fixture test. Không bắt bạn gửi các giá trị này vào chat.

Nếu lỗi TLS SQL/PKIX hoặc hostname certificate: cấu hình certificate/truststore hợp lệ cho SQL Server/JVM theo máy. Agent không tự đổi trustServerCertificate=false sang bypass, không thay thiết lập SQL. Kết nối metadata Windows thành công không chứng minh login runtime/TLS JDBC đã PASS.

## 4. Cloudflare Quick Tunnel, session và bảo mật

Lệnh bạn tự chạy trong PowerShell:

```powershell
cloudflared tunnel --url http://localhost:8080
```

Nếu origin được resolve sang ::1 nhưng app bind127.0.0.1 và bị502, dùng `cloudflared tunnel --url http://127.0.0.1:8080` cho đúng loopback IPv4; không đổi server.address thành0.0.0.0 để chữa.

Quick Tunnel không cần tài khoản/domain, hostname thay khi khởi tạo mới, không có bảo đảm uptime. Giữ cửa sổ cloudflared chạy trong buổi demo. Không thêm --allowed-mail vì xác thực email tương tác chặn SePay server-to-server. Đây là hạn chế được chấp nhận cho demo có giám sát, không phải bằng chứng hạ tầng always-on. [Cloudflare Quick Tunnels](https://developers.cloudflare.com/tunnel/get-started/quick-tunnels/)

Quy trình lấy hostname lần đầu không bị vòng lặp:
1. Sau chuẩn bị schema, chạy Live configuration với cả3 cờ tắt; môi trường vẫn live, không dùng Test. Startup chưa yêu cầu approval vì chưa bật xử lý tiền, nhưng schema validate vẫn phải đạt.
2. Mở Tunnel để lấy URL. Đặt origin đó vào SEPAY_LIVE_PUBLIC_URL trong IntelliJ.
3. Restart **chỉ Spring Boot**, giữ tiến trình Tunnel để hostname không đổi. Xác minh UI qua HTTPS; các cờ vẫn tắt.
4. Sau đủ phê duyệt mới thêm marker/cờ theo mục6; restart Spring với Tunnel vẫn giữ nguyên.

ForwardedHeaderFilter khôi phục scheme=https/host/port443 từ proxy. Chỉ tin proxy qua loopback; tiến trình local khác có thể giả header, vì vậy giữ laptop và cổng origin được kiểm soát. Header khai HTTPS không thay bằng chứng TLS public. Không cấu hình CORS wildcard hoặc tắt CSRF: webhook là POST server-to-server miễn CSRF đúng endpoint; Member polling/copy/payment cùng origin, login dùng CSRF session. Đăng nhập qua HTTPS Tunnel, không dùng http://localhost với Secure cookie.

Sau đổi hostname hoặc restart app, đăng nhập lại/mở form mới để lấy session/CSRF mới. Không đặt cookie Domain=.trycloudflare.com hoặc SameSite=None chỉ để dùng lại phiên. Redirect login là path tương đối; SePay không redirect trình duyệt như VNPay, trang QR chỉ polling read-only. Mixed-content/redirect về HTTP/localhost cho thấy forwarded header/effective origin sai, phải sửa proxy/config thay vì hạ bảo mật. Camera QR Safari dùng HTTPS như trước; không sửa scanner.

## 5. CẦN CHỦ TÀI KHOẢN LÀM TRÊN SEPAY LIVE

A/B. Bạn vào Dashboard Live → Tài khoản ngân hàng → Thêm tài khoản (hoặc kiểm tra liên kết đã có), chọn đúng ngân hàng/chủ thể và hoàn tất xác thực qua kênh chính thức. Không cung cấp password/OTP cho agent. Xác nhận trạng thái Hoạt động. [SePay ngân hàng](https://developer.sepay.vn/vi/sepay-webhooks/tai-khoan-ngan-hang)

C. Cấu hình Công ty → Cấu hình chung → Nhận diện/Cấu trúc mã thanh toán: bật nhận diện; prefix đúng SEPAY_PAYMENT_PREFIX, hậu tố tối thiểu=tối đa24, loại **Số và chữ**. UTEFit sinh prefix +24 hex ngẫu nhiên; không dùng ví dụ mã ngắn hoặc kiểu chỉ chữ số. Không thay mẫu khi còn đơn chờ. [SePay cấu trúc mã](https://developer.sepay.vn/vi/sepay-webhooks/cau-hinh-ma-thanh-toan)

D/E/F. Tích hợp → Webhooks → Thêm/Sửa: URL **`https://<TUNNEL_HOSTNAME>/payments/sepay/webhook/live`**, tiền vào, JSON, đúng tài khoản cụ thể/VA cần thiết. Không chọn mọi tài khoản; tránh bộ lọc bỏ mất khoản sai nội dung. [SePay tạo webhook](https://developer.sepay.vn/vi/sepay-webhooks/tao-webhook)

G/H. Chọn HMAC-SHA256, bật retry/cảnh báo, secret khớp SEPAY_LIVE_WEBHOOK_SECRET ngoài source; không API-key/no-auth. Bearer token tra cứu không phải HMAC secret. [SePay xác thực](https://developer.sepay.vn/vi/sepay-webhooks/xac-thuc)

I. Giữ webhook theo trạng thái được chủ dự án duyệt; không tự tạo thêm hoặc bật Webhook. Không dùng nút Gửi thử/payload mô phỏng khớp đơn Live. Bản bổ sung VA yêu cầu V709, snapshot main/VA riêng, đối chiếu accountNumber+subAccount; bạn phải kiểm tra mapping và chỉnh bộ lọc VA của Webhook hiện có theo SEPAY_BIDV_VA.md trước khi cân nhắc giao dịch mới.

## 6. Live approval: bạn/DBA thực hiện riêng sau xác nhận

Cổng cần PASS trước khi tạo Payment thật:
- SQL schema25 bảng/columns/types đúng và4 UNIQUE V706 đủ cột/filter/enabled; FK/CHECK trusted, không Test order/receipt, không fixture tài chính.
- JDBC URL và connection.getCatalog đều UTEFitLiveDemo; runtime không DDL/owner/ghi guard; backup/restore và kiểm tra SQL Server lock/rollback được chấp thuận.
- Profile đúng, schema validate/init never, bank/account/gateway hợp lệ, HMAC riêng, NTP/HTTPS/forwarded cookies đúng.
- Có quyết định người quản trị triển khai và người có quyền nhận tiền; không dùng credential đã lộ hoặc tài khoản mẫu.

Schema V708: row id=1, environment=LIVE, deployment_id khớp SEPAY_LIVE_DEPLOYMENT_ID, approved_at giờ duyệt. Chỉ bạn/DBA có quyền owner phê duyệt; Member/Admin web không có endpoint cấp approval. Template06 mặc định không ghi, không tự tạo row/overwrite. Hằng số approval ở env chỉ là thêm1 chốt, không thay quyền DBA hoặc quyền ngân hàng.

Kiểm tra approval chỉ đọc:

```sql
USE [UTEFitLiveDemo];
SELECT id,environment,deployment_id,approved_at FROM dbo.sepay_deployment_guard;
SELECT COUNT(*) AS expected_zero FROM dbo.sepay_payment_details WHERE environment='TEST';
SELECT COUNT(*) AS expected_zero FROM dbo.sepay_webhook_transactions WHERE environment='TEST';
```

Sau **bạn xác nhận** đủ bằng chứng: tự duyệt template06 trong bản làm việc riêng, nhập đúng deployment id, thực thi bằng DBA; đặt SEPAY_LIVE_APPROVAL, SEPAY_ENABLED=true, SEPAY_LIVE_ENABLED=true nhưng giữ ACCEPT_NEW_PAYMENTS=false. Restart app, startup guard phải qua bằng login runtime. Chưa qua thì dừng, không bỏ chốt.

Bạn có thể tự kiểm tra HTTPS /login bằng trình duyệt trước. Kiểm tra webhook âm tính không chứa giao dịch/chữ ký chỉ khi bạn cho phép:

```powershell
curl.exe -i -X POST "https://<TUNNEL_HOSTNAME>/payments/sepay/webhook/live" -H "Content-Type: application/json" --data-binary "{}"
```

Agent không chạy lệnh này. Không có HMAC nên dự kiến401 khi config Live hợp lệ; nếu cả cờ tắt dự kiến503 trước HMAC. Không có Payment/receipt nào được tạo bởi handler với request này. Đây chỉ là reachability/negative-auth, **không phải xác minh chữ ký Live thành công**. GET callback không phải cách xác minh POST và có thể redirect/deny do Security.

Chỉ sau quyết định **GO cho giao dịch đầu có giám sát** mới đặt SEPAY_LIVE_ACCEPT_NEW_PAYMENTS=true, restart giữ Tunnel, bật webhook đúng URL. Không cho người khác bắt đầu chuyển khoản trước khi các bước này hoàn tất.

## 7. Quy trình giao dịch thật đầu tiên — bạn thực hiện

1. Member hợp lệ đăng nhập origin HTTPS hiện tại, chọn đúng branch/plan có giá đã duyệt; Membership PENDING.
2. Chọn Chuyển khoản VietQR. Backend tạo Payment PENDING/reference riêng, snapshot giá/expiry; không nhập số tiền từ frontend. Không tạo đơn CASH hoặc phương thức khác song song.
3. Kiểm tra QR + tên bank + chủ tài khoản + số nhận + amount + reference trùng nhau. Nếu expiry ít hoặc thông tin sai, **không chuyển**. Không tạo hàng loạt đơn Live để thử QR.
4. Chính người có quyền tự chuyển đúng tiền/nội dung từ app ngân hàng. Agent không điều khiển app, không lấy OTP hoặc tự chuyển.
5. Chủ tài khoản kiểm tra tiền vào ngân hàng, sau đó giao dịch trong SePay Live/delivery HMAC. Không coi ảnh chụp bên chuyển là xác nhận phía nhận.
6. Backend kiểm tra HMAC/raw body/time, in/account/gateway/code/amount/expiry/owner/snapshot và transaction uniqueness; transaction ghi receipt MATCHED, Payment PAID, Membership ACTIVE/thời hạn snapshot, notification. Đóng browser không cản backend xử lý.
7. Member polling thấy PAID/ACTIVE, QR ẩn; Admin Payment/ledger/statistics xác nhận doanh thu chỉ Payment PAID, không cộng receipt thêm lần nữa. Ghi bằng chứng đã che thông tin riêng tư/secret.

Không có nút Đã thanh toán tự kích hoạt; không ép PAID bằng CASH/SQL, không fake receipt. Chỉ sau bằng chứng ngân hàng thật + SePay + trạng thái đúng mới ghi **Live VERIFIED**.

## 8. Sự cố, mất mạng và đối soát khi khởi động lại

| Tình huống | Bạn xử lý / hành vi source |
|---|---|
| Webhook không tới | Không chuyển lại. Kiểm tra tiền vào ngân hàng, SePay giao dịch/delivery URL/retry, app/Tunnel/SQL đang chạy, HTTPS/HMAC/NTP/log đã che secret |
| Laptop hoặc Tunnel tắt | Tiền vẫn có thể vào ngân hàng qua QR cũ. Khởi động lại đúng DB/config, lấy hostname mới, sửa webhook, kiểm tra delivery thất bại; không tạo/chuyển khoản mới để chữa |
| Replay thật sau sự cố | Chủ tài khoản/được ủy quyền dùng Dashboard phát lại đúng giao dịch thật sau đối soát; phải ký timestamp mới, không gửi lại header cũ quá5 phút. Agent không replay |
| Webhook đến muộn | Code kiểm tra cả giờ nhận hiện tại và transactionDate. Dù ngân hàng nhận trong hạn nhưng Webhook tới sau expiry, có thể REVIEW/LATE_OR_INVALID_TRANSACTION_DATE, không tự ACTIVE |
| Sai mã | REVIEW/UNKNOWN_REFERENCE hoặc MISSING_OR_AMBIGUOUS_REFERENCE; không gán cho đơn khác |
| Thiếu/thừa tiền | REVIEW/AMOUNT_MISMATCH; không tự gộp, không yêu cầu chuyển tiếp phần thiếu qua app; đơn có REVIEW không tự nhận lần đủ tiếp theo |
| Duplicate | UNIQUE/idempotency không kích hoạt lần hai. Cùng ID/bank reference đổi dữ liệu ghi conflict để Admin kiểm tra |
| API đối soát | /admin/payments/sepay-reconciliation: lookup tùy chọn chỉ đọc20 mục/trang/ngày. API UUID khác Webhook ID số; không ép ID/import tự PAID. Chưa có token có thể dùng Dashboard |
| Đơn Live hết hạn | Vẫn PENDING+expired để đối soát; hiện chưa có workflow tự gia hạn/cancel-and-renew an toàn. Không sửa expiry/status/giá hoặc tạo Member khác để né đơn cũ |

REVIEW chưa có nút resolve/duyệt kích hoạt/hoàn tiền trong app. Cần người phụ trách ghi sổ đối soát và phương án nghiệp vụ được phê duyệt; không sửa SQL che giao dịch. Nếu chưa làm rõ tiền đã nhận thì dừng demo nhận thêm tiền. Ledger có thể giữ REVIEW lịch sử kể cả đã xử lý bên ngoài; phải có chứng từ đối soát riêng, không DELETE cho sạch màn hình.

## 9. Mỗi lần demo trên laptop

### Bắt đầu
1. Bật nguồn/giữ mạng, tắt sleep trong buổi demo theo chính sách máy. Mở Services/SQL Server Configuration Manager, xác nhận service instance đang RUNNING; mở SSMS kiểm tra đúng UTEFitLiveDemo bằng SELECT, không chạy lại bootstrap/seed/approval.
2. Đọc sổ Payment/ledger/SePay còn pending hoặc cần đối soát từ buổi trước. Chưa rõ thì không mở tạo đơn mới.
3. IntelliJ chọn UTEFit Live Laptop, ACCEPT_NEW_PAYMENTS=false. Giữ approval/callback flag đúng quyết định đã duyệt; lần đầu chưa có approval thì cả cờ nhận tiền tắt như mục4.
4. Run Spring Boot8080 rồi chạy cloudflared theo mục4. Sao chép URL mới, cập nhật SEPAY_LIVE_PUBLIC_URL, restart **app** giữ Tunnel để URL ổn định; profile tự cập nhật WebSocket origin.
5. Chủ tài khoản sửa Webhook Live sang hostname mới, giữ account/secret/prefix đúng. Kiểm tra log/reachability/auth âm tính và guard, đăng nhập lại qua hostname mới. Không bật challenge email chặn SePay.
6. Chỉ sau xác nhận đầu buổi, mở ACCEPT_NEW_PAYMENTS=true/restart và webhook đúng URL; bắt đầu đúng một giao dịch đã duyệt, đối chiếu đầy đủ trước giao dịch tiếp theo.

### Kết thúc
1. Ngừng phát hành đơn bằng ACCEPT_NEW_PAYMENTS=false và restart có kiểm soát; callback vẫn được xử lý cho đơn cũ. Không coi cờ này thu hồi mọi QR đã chụp.
2. Giữ SQL/app/Tunnel hoạt động cho tới khi đơn/delivery được đối soát, kể cả trường hợp muộn. Không có thời gian chờ cố định nào chứng minh không ai chuyển bằng QR cũ nữa; ghi người phụ trách theo dõi ngân hàng sau buổi.
3. Backup mới bằng template07 khi được phép, lưu ngoài Git; giữ receipt/Payment/lịch sử. Tắt webhook chỉ theo quyết định của chủ tài khoản sau đối soát, lưu các delivery cần phục hồi.
4. Sau đó dừng Tunnel và app. Dừng SQL service chỉ khi không ứng dụng khác cần. Buổi sau URL đổi phải cập nhật lại Webhook và origin, không đổi deployment id/secret hoặc recreate DB chỉ vì hostname thay.

## 10. Backup / rollback / GO-NO-GO

Schema lỗi trước tiền thật: dừng script, kiểm tra transaction/schema đã commit ở version nào. Không dùng DROP/RESET; có thể giữ DB riêng và lập phương án DBA, thử lại trên bản sao. Template01 chỉ chạy DB rỗng; không chạy lên DB đã có schema.

Sau tiền thật: source rollback không được làm mất Payment/receipt. Dừng tạo đơn mới, giữ callback/DB/secret để drain, đối soát ngân hàng. Không checkout binary cũ thiếu SePay guard hoặc restore đè backup cũ để xóa phát sinh. Backup VERIFYONLY không thay restore drill; phục hồi thử dùng database khác và không bật app Live vào bản phục hồi để tránh hai bên cùng xử lý.

**Hiện NO-GO cho tự bật nhận tiền.** Còn thiếu DB LiveDemo/schema chạy thực tế, login runtime/TLS/quyền/backup/restore, catalog/tài khoản đã xác thực, approval của bạn/DBA, Tunnel hostname/effective cookies và mapping ngân hàng/HMAC/delivery thực tế. Guard/test đã sẵn sàng để bạn thực hiện các bước trên, không phải giấy phê duyệt thay chủ tài khoản.

Quick Tunnel/laptop không uptime guarantee, nên chỉ GO có giám sát cho demo hữu hạn sau khi bạn chấp nhận rủi ro gián đoạn và có người đối soát. Không tuyên bố phù hợp vận hành nhận tiền liên tục. Không cần thuê hạ tầng khác cho phạm vi tài liệu này.

## 11. Inventory và kết quả kiểm tra

Tạo mới9 SQL ở database/live-demo/ (00–08), LiveDemoSchemaPreparationTest, LiveTunnelSecurityTest và tài liệu này. Sửa application-sepay-live.properties (session/WebSocket origin) và SepayLiveSafetyTest (case UTEFitLiveDemo+QuickTunnel). Không sửa Payment/HMAC/QR/Review/Manager/Favorites hoặc migration đã có trong lượt laptop này.

Unit chuẩn bị ban đầu:10/10 PASS, schema SQLServerDialect xuất ra file target không kết nối DB.

Kết quả cuối:
- Maven compile +20 suite: **BUILD SUCCESS,147 tests PASS,0 failure/error/skipped**; log local `target/live-laptop-verification.log`.
- Node toàn bộ JavaScript: **38/38 PASS** (QR Member/scanner, Review validation, SePay polling).
- Test mới kiểm tra schema snapshot19 bảng khớp24 Entity sau loại5 bảng do migration gốc quản lý, đủ include V701–V708, không có DML seed và provider không cho kết nối DB; không phải T-SQL runtime test.
- Test HTTPS/proxy/session/WebSocket origin và case LiveDemo+trycloudflare đều PASS; không thay chứng minh certificate/cookie/Tunnel thực tế.
- Regression có SePay/HMAC/idempotency/concurrency/rollback, CASH/VNPay lịch sử, QR, Review, Manager/Favorites.
- SQL scripts **chưa thực thi**; ngân hàng/SePay/HTTPS thực tế **chưa VERIFIED**. Không dùng Playwright.

Git cuối: vẫn feat/giai-doan-7/HEAD2dadc44, QR a8798f7 là ancestor. Không stage/commit/push/merge. Application.properties local và các thay đổi SePay có sẵn giữ nguyên; database hiện có chỉ được đọc metadata.
