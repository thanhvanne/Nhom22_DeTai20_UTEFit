# UTEFitDB — kiểm tra quyền và chuẩn bị SePay Live

## Kết luận ngày 26/10/2026

**NO-GO nhận tiền thật ở trạng thái hiện tại.** Không chạy migration, cấp quyền,
approval, khởi động Live hay gửi webhook trong lượt kiểm tra này.

Kết quả truy vấn chỉ đọc bằng Windows Integrated Authentication trên SQL Server local:

| Hạng mục | Kết quả thực tế |
|---|---|
| Login `utefit_live_runtime` | Tồn tại, đang bật, SQL login; không phải sysadmin |
| SQL authentication | Server cho phép mixed authentication |
| User mapping trong UTEFitDB | Chưa có |
| Thử quyền qua `EXECUTE AS LOGIN` | Lỗi 916: không truy cập được UTEFitDB |
| Schema | 28 bảng; bản dựng đã gồm V701–V712 |
| `sepay_deployment_guard` | Không có bản ghi; chưa phê duyệt deployment |
| SePay TEST details/webhook | 0 / 0 |
| Tài khoản demo ACTIVE, enabled | 55 tài khoản `@utefit.test`, mật khẩu demo công khai |
| Payment `DEMO-CASH-*` | 21; không phải bằng chứng nhận tiền ngân hàng |
| Database markers | `DEVELOPMENT_ONLY`, seed `V712-demo55` |

Thử impersonation chỉ xác minh quyền SQL, **không xác minh mật khẩu runtime,
TCP, JDBC TLS hoặc khả năng đăng nhập từ IntelliJ**. Chưa có user mapping nên chưa
thể chứng nhận quyền runtime hiện tại đáp ứng nghiệp vụ.

Không nhập giao dịch, Payment, webhook hay approval từ LiveDemo/QA. Các database đó
không bị thay đổi. Báo cáo so sánh trong `DATABASE_HANDOFF_REPORT.md` là số liệu trước
lần chủ dự án dựng lại UTEFitDB; không dùng số bảng cũ làm kết quả hiện tại.

## 1. Script DBA sử dụng theo thứ tự

Mở SSMS bằng tài khoản DBA độc lập, chọn **UTEFitDB** trên thanh database.
Không chạy các template bằng login ứng dụng. Không chạy lại hai script dựng/seed.

1. `database/operations/UTEFitDB_01_live_preflight.sql`: chỉ SELECT và impersonation
   phiên làm việc; kiểm tra login, mapping, quyền từng bảng/cột guard, approval và demo.
   Không tự chuyển database. Lỗi runtime được xuất bằng mã lỗi, không lộ mật khẩu.
2. Sao lưu database và lưu báo cáo quyền hiện có tại nơi riêng. DBA duyệt
   `UTEFitDB_02_runtime_permissions.template.sql`, rồi mới chủ động đổi `@Apply=1`
   nếu được chủ dự án phê duyệt. Script tạo user mapping và cấp quyền trong transaction;
   không sửa login/password hoặc quyền ở database khác. Mặc định script từ chối chạy.
   Nếu user đã có role/ownership/SID sai, dừng để DBA xử lý riêng, không tự thu hồi quyền.
3. Chạy lại preflight và kết nối bằng runtime qua chính cấu hình JDBC/TLS thực tế.
   Kiểm tra nghiệp vụ trên môi trường được phép trước khi phê duyệt. Ma trận quyền
   dưới đây dựa trên source, chưa được áp dụng hoặc smoke-test trên SQL Server.
4. Xử lý các điều kiện an toàn ở mục 3–5. Sau quyết định riêng của chủ dự án, DBA mới
   duyệt `UTEFitDB_03_live_approval.template.sql`. Điền deployment ID mới, các xác nhận
   và `@Approve=1` **trong SSMS**, không commit bản điền. Template không ghi đè approval
   đã tồn tại và không tự bật bất kỳ cờ SePay nào.

Các template kiểm tra đúng tên UTEFitDB, yêu cầu phiên riêng, dùng XACT_ABORT và
rollback khi lỗi trong transaction. Chúng không phải migration; **không cần migration
mới** cho schema vừa dựng. Chỉ được parse cú pháp bằng ScriptDom, chưa thực thi quyền.
Nếu lỗi trước COMMIT: kiểm tra rollback và chạy lại preflight; không reset dữ liệu.
Sau COMMIT: không dùng reset để hoàn tác. Dừng ứng dụng, DBA thu hồi/chỉnh quyền hoặc
approval theo thay đổi đã duyệt; giữ backup và lịch sử tài chính. Restore toàn bộ chỉ
theo kế hoạch phục hồi tránh làm mất giao dịch phát sinh sau backup.

## 2. Phân quyền chính xác theo source hiện tại

| Chủ thể | Quyền |
|---|---|
| Runtime | CONNECT; SELECT/INSERT trên 27 bảng nghiệp vụ |
| Runtime UPDATE | 25 bảng nghiệp vụ, ngoại trừ `membership_branch_snapshot`, `sepay_payment_details` là snapshot bất biến |
| Runtime DELETE | Chỉ `favorites`, `manager_branch_assignments`, `membership_plan_branches` |
| Runtime guard | Chỉ SELECT `dbo.sepay_deployment_guard`; DENY INSERT/UPDATE/DELETE/ALTER/TAKE OWNERSHIP/REFERENCES |
| DBA | Tạo/mapping login-user, duyệt quyền, backup/restore, DDL/migration và ghi approval sau phê duyệt |

Danh sách từng bảng nằm trong biến `@Required` của preflight/template cấp quyền.
Không cấp `db_owner`, `db_ddladmin`, `db_securityadmin`, sysadmin, quyền sở hữu schema,
CONTROL hoặc DML toàn schema. Template kiểm tra cả quyền UPDATE từng cột guard vì
SQL Server có thể cho column GRANT vượt table DENY. Không DENY CONTROL toàn guard
vì quan hệ phân cấp quyền có thể chặn cả SELECT cần thiết.

Đây là quyền của **toàn bộ tiến trình ứng dụng**. DB không phân biệt người dùng web
Admin/Member; RBAC, CSRF, HMAC, bằng chứng đối soát và transaction vẫn bắt buộc ở backend.
DBA phải kiểm tra thêm quyền kế thừa từ public, role, module ký chứng chỉ/EXECUTE AS
hoặc cơ chế tùy biến nếu tự bổ sung; không cấp đường vòng ghi guard qua stored procedure.
Template mặc định chỉ dành cho deployment sysadmin duyệt, không cấp sysadmin cho runtime.

## 3. Approval và tài khoản demo: không được bỏ qua

`SepayDatabaseGuard` không cấm tên UTEFitDB. Guard kiểm tra catalog thực tế, deployment
ID khớp bản ghi singleton `id=1, environment=LIVE` và không có SePay TEST data.
`SepayProperties` đồng thời kiểm tra profile, cấu hình, TLS origin và các cờ bảo vệ.

**Guard source không tự phát hiện 55 tài khoản demo hay CASH demo.** Vì vậy một
approval thiếu thận trọng có thể vượt kiểm tra kỹ thuật nhưng vẫn không an toàn.
Không xóa guard hay dữ liệu để ép chạy.

Trước nhận tiền thật, chủ dự án phải xử lý toàn bộ tài khoản có mật khẩu công khai,
đặc biệt 10 Admin, cùng tài khoản PT/Manager/Member demo. Template approval bổ sung
chốt từ chối khi còn tài khoản `@utefit.test` ACTIVE + enabled; chủ dự án duyệt cách
vô hiệu hóa/tạo tài khoản thực phù hợp mà giữ lịch sử. Không tự đổi mật khẩu hay
vô hiệu hóa tài khoản trong lượt này. Các tài khoản khác từng chia sẻ mật khẩu cũng
phải được rà soát, không chỉ dựa vào hậu tố email.

21 CASH demo không bị guard SePay TEST chặn nhưng có thể lẫn doanh thu thật. Chủ dự
án phải xác định chính sách báo cáo/demo và dữ liệu nào là thật trước approval;
không tự xóa hay chuyển chúng thành giao dịch SePay. Approval template giữ marker
seed, đánh dấu purpose `LIVE_FINANCIAL` trong transaction để script seed từ chối
chạy lại; đây là thay đổi chỉ xảy ra nếu DBA chủ động thực thi sau phê duyệt.

## 4. IntelliJ: giữ credential hiện có, đổi đúng catalog

Không ghi đè `.local/application-local.properties`. File local có thể định nghĩa
datasource literal, nên chỉ đổi `DB_URL` **chưa chắc đổi URL có hiệu lực**. Dùng
Run → Edit Configurations → Environment variables để đặt trực tiếp:

```text
SPRING_PROFILES_ACTIVE=sepay,sepay-live
SPRING_DATASOURCE_URL=jdbc:sqlserver://<SQL_HOST>:<PORT>;databaseName=UTEFitDB;encrypt=true;trustServerCertificate=false
SPRING_DATASOURCE_USERNAME=utefit_live_runtime
SPRING_DATASOURCE_PASSWORD=<nhập riêng trên máy>
SEPAY_LIVE_DATABASE_NAME=UTEFitDB
SEPAY_LIVE_DEPLOYMENT_ID=<ID mới 16–100 ký tự chữ/số/gạch ngang/gạch dưới, khớp approval>
SEPAY_LIVE_PUBLIC_URL=https://<hostname-hiện-tại>
SEPAY_LIVE_ACCEPT_NEW_PAYMENTS=false
```

URL SQL cần certificate/truststore tin cậy phù hợp hostname. Không tắt TLS hoặc
bật trustServerCertificate để vượt lỗi Live. Hai profile `sepay`, `sepay-live`
không hardcode catalog/truststore, nhưng file `.local` và profile `qa` có cấu hình
máy cũ; **không ghép qa/test vào Live**. Không tự sửa mã dịch vụ hay truststore local.

Giữ đúng giá trị ngân hàng/HMAC đang quản lý riêng: `SEPAY_LIVE_BANK_CODE`,
`SEPAY_LIVE_GATEWAY`, `SEPAY_LIVE_ACCOUNT_NUMBER` (chính), `SEPAY_LIVE_VA_NUMBER`
(VA riêng, bắt buộc BIDV), `SEPAY_LIVE_ACCOUNT_NAME`, `SEPAY_LIVE_WEBHOOK_SECRET`.
Giữ `SEPAY_WEBHOOK_AUTH=hmac`, `SEPAY_PAYMENT_PREFIX=UTE`, thời hạn
`SEPAY_PAYMENT_MINUTES` 5–60 phút. Live secret tối thiểu 32 ký tự, khác Test secret.

Chỉ sau approval riêng mới được đặt `SEPAY_ENABLED=true`, `SEPAY_ENVIRONMENT=live`,
`SEPAY_LIVE_ENABLED=true`, `SEPAY_LIVE_APPROVAL=I_APPROVE_SEPAY_LIVE`.
Các profile Live đặt `ddl-auto=validate`, SQL init never, secure cookies và forwarded
headers. Không bật `SEPAY_LIVE_ACCEPT_NEW_PAYMENTS` trong bước chuẩn bị này.
Đúng DB/profile/approval/cấu hình là điều kiện cần để startup guard cho phép Live;
nhận đơn mới còn yêu cầu accept-new-payments và kiểm tra nghiệp vụ hiện có.

Đối soát tra cứu API tùy chọn: `SEPAY_LIVE_API_LOOKUP_ENABLED`,
`SEPAY_LIVE_API_TOKEN`, `SEPAY_LIVE_BANK_ACCOUNT_ID`. Tra cứu có phân quyền không
tự đánh dấu PAID. Đối soát thủ công dùng công tắc hiện có
`SEPAY_MANUAL_RECONCILIATION_ENABLED` và kiểm tra Admin ACTIVE/enabled trong DB;
không cần danh sách Admin ID. Không gửi credential vào chat hoặc Git.

## 5. Cutover webhook và các dịch vụ

Endpoint giữ nguyên: `https://<hostname>/payments/sepay/webhook/live`.
Chủ tài khoản tự sửa webhook đang có sau thời điểm cutover đã duyệt: HTTPS đúng
origin, tiền vào, đúng main account/VA, HMAC và retry. Không tạo webhook giả hay
chuyển tiền để kiểm tra quyền SQL. URL Quick Tunnel đổi thì phải cập nhật cấu hình
origin/webhook đồng bộ và khởi động lại có kiểm soát.

Trước chuyển webhook, chủ dự án phải rà soát Payment PENDING/REVIEW và retry còn tồn
ở LiveDemo. Webhook mã đơn cũ tới UTEFitDB có thể thành UNKNOWN_REFERENCE/REVIEW,
**không thanh toán đơn cũ bên LiveDemo**. Không sao chép giao dịch/approval, không
replay tự chế, không trả tiền thêm để chữa lỗi. Giữ LiveDemo nguyên trạng để chủ dự
án xử lý đơn cũ bằng quy trình đối soát hiện có. Không chạy đồng thời hai consumer
cho cùng webhook nếu chưa có kế hoạch tránh nhận nhầm và bỏ sót tiền.

| Thành phần | Đã kiểm tra | Cần kiểm tra thực tế |
|---|---|---|
| Membership/Check-in/Review/Manager/Chat | Schema nguồn dùng datasource chung, không có kết nối LiveDemo riêng trong service | Sau cấp quyền: login từng role, xem gói/lớp/check-in/review/chat bằng tài khoản được phép |
| Gmail/OTP | Cấu hình SMTP bên ngoài, không phụ thuộc tên catalog | Chủ máy tự kiểm tra AUTH/TLS/gửi OTP tới email có quyền sử dụng; `.test` không nhận mail thật |
| Cloudinary | Cấu hình bên ngoài, URL media trong DB, không phụ thuộc tên catalog | Upload/xem/xóa theo quyền trên dữ liệu được phép; quota/network/credential chưa xác minh |
| JDBC TLS/runtime | Audit metadata bằng Windows auth thành công | SQL login thật qua đúng JDBC URL/certificate, không dùng tài khoản DBA chạy web |
| SePay | Guard/profile/endpoint được rà soát source | Chưa xác minh webhook Live/giao dịch thật trong lượt này |

Không thay giá trị MAIL/CLOUDINARY/JWT đang dùng. Khi Pull về máy khác, mỗi người
tự nhập secret riêng theo `.env.example`; Spring không tự nạp `.env`. JWT phải được
cấu hình đúng trước login; không chia sẻ JWT secret production cho cả nhóm.

## 6. Quy trình nhóm và GO/NO-GO

Mỗi thành viên dùng **local UTEFitDB của mình**, dựng bằng đúng hai file SQL trong
README, dùng 55 tài khoản demo; SePay/Live mặc định tắt, không chạy ba template Live.
Không trỏ mọi máy thành viên vào DB chứa tiền thật. Cùng tên database không đồng
nghĩa cùng instance/dữ liệu. Một UTEFitDB đã nhận tiền thật không còn là DB reset/seed.
Không thể giữ mật khẩu Admin demo công khai trên máy chủ tài chính mà coi là an toàn.

Chỉ GO khi: backup phục hồi được; runtime mapping/quyền tối thiểu và guard read-only
được DBA xác minh; các credential công khai đã vô hiệu; demo accounting được rà soát;
schema validate/JDBC TLS đạt; deployment ID/approval khớp; HTTPS/HMAC/VA đúng; đơn cũ
đã có phương án; smoke-test dịch vụ cần thiết đạt; chủ tài khoản và người quản trị
phê duyệt riêng việc bật nhận đơn mới. **Hiện chưa đạt các điều kiện đó: NO-GO.**

Rollback vận hành: dừng tạo đơn mới trước, giữ log/audit và kiểm tra tiền thực nhận;
không đổi PAID về PENDING, không hủy hay hoàn tiền tự động. Khi dừng ứng dụng phải
lưu kế hoạch retry/đối soát, không đổi qua lại catalog tùy tiện làm mất nơi tiếp nhận
webhook. Không chạy lại `DB_Khung_1.sql`/`DB_Data_2.sql` để sửa lỗi Live.

## 7. Kiểm thử đã chạy trong lượt này

- Maven compile/test thành công: **21 tests, 0 fail/error**, gồm `DatabaseHandoffTest`,
  `PortableConfigurationTest`, `SepayDatabaseGuardTest`, `SepayLiveSafetyTest`,
  `LiveTunnelSecurityTest`. H2 riêng, không dùng SQL Server để ghi dữ liệu.
- Test mới xác nhận có thể cấu hình catalog UTEFitDB mà vẫn cần approval và không
  tự bật nhận đơn mới. Test cấu hình không tương đương approval trên database thật.
- **42 JavaScript tests PASS**.
- Microsoft SQL Server ScriptDom TSql160Parser: ba script operations đều **0 lỗi
  cú pháp**; chưa chứng nhận thực thi GRANT/DENY/approval trên SQL Server.
- Chưa chạy ứng dụng với runtime login mới, Gmail thật, Cloudinary thật, webhook
  Live hoặc giao dịch ngân hàng. Không tuyên bố các bước đó PASS.
- Giữ nguyên nhánh `feat/giai-doan-7`, mọi thay đổi chưa commit, không stage/commit/push.
