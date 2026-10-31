# UTEFit — hướng dẫn bàn giao cho nhóm

> **Bản sửa logic và khuyến mãi 29/10/2026:** database hiện có phải áp dụng
> `database/migrations/V715__membership_promotion_snapshot.sql` trước khi chạy bản mới.
> Không reset/seed lại database đang có dữ liệu. Xem
> [luồng ưu đãi và đối chiếu 25 mục sửa](docs/UTEFit_SuaLogic_VaKhuyenMai_2026-10-29.md).
> Guest/Member xem ưu đãi tại `/promotions`; Member nhập mã ngay trên thẻ mua gói.

Java 17 · Spring Boot 4.1.1 · SQL Server · Thymeleaf · Bootstrap · Spring Security/JWT · WebSocket.
Database **phát triển dùng chung về cấu trúc: UTEFitDB**. Mỗi thành viên dựng database
local riêng; không đồng bộ tiền thật hoặc dùng database Live/QA của tác giả.

> SePay Live dùng UTEFitDB: đọc [hướng dẫn DBA và kết quả kiểm tra quyền](UTEFITDB_SEPAY_LIVE_DBA.md).
> Database vừa seed 55 tài khoản demo **chưa an toàn để nhận tiền thật**; không tự chạy approval.

> Bảng giá và tên gói tiếng Việt: xem [cập nhật danh mục demo](DEMO_CATALOG_VI.md).
> Database cũ cần V713 và script cập nhật danh mục; **không reset hoặc chạy lại seed để đổi giá**.

## 1. Clone/Pull và IntelliJ

1. Clone repository của nhóm và làm việc trực tiếp trên `main` theo quy trình main-only bên dưới. Không tự commit/push thay người phụ trách khi chưa được duyệt.
2. Mở thư mục project bằng IntelliJ, chọn JDK 17, reload Maven.
3. Cài SQL Server và SSMS, bật TCP/IP/cổng SQL Server thích hợp.
4. Chưa chạy ứng dụng trước khi dựng schema. Hibernate mặc định **validate**, không tự cập nhật bảng.
5. Không copy `target/`, `.local/`, `.env`, thư mục AI hoặc cấu hình Run chứa secret từ máy khác.

## 2. Đúng HAI file SQL để dựng môi trường mới

Chỉ dùng:
- `database/DB_Khung_1.sql` — schema đầy đủ, có chốt reset, **28 bảng**.
- `database/DB_Data_2.sql` — dữ liệu demo và đúng 55 tài khoản.

Hai file là T-SQL độc lập, chạy bằng SSMS Execute thông thường, không bật SQLCMD Mode,
không cần `:r`, không cần chạy thêm V701–V712 cho database vừa dựng.
Schema đã nhúng nguyên nội dung 12 migration để giữ đúng constraint/index bảo mật.

### Bước 1 — sao lưu và chủ động xác nhận reset

**Không chạy script dựng lại trên database có dữ liệu cần giữ.** Người quản trị phải
sao lưu UTEFitDB trước, dừng ứng dụng và chắc chắn không có người khác đang sử dụng.

Mở SSMS, kết nối SQL Server, mở `DB_Khung_1.sql`. Mặc định script THROW và không reset.
Chỉ sau khi được phê duyệt, sửa đúng hai biến ở đầu file:

```sql
DECLARE @AllowReset bit = 1;
DECLARE @ResetConfirmation nvarchar(100) = N'RESET_UTEFitDB_AFTER_BACKUP';
```

Chọn context `master` hoặc `UTEFitDB`, chọn **toàn bộ file**, Execute. Không chạy riêng
đoạn DROP/CREATE hay bỏ qua guard. Script chỉ reset tên cố định UTEFitDB và từ chối
context LiveDemo/QA/khác. Không được đổi tên đích thành database Live.

Kết quả mong đợi: `schema_table_count = 28`, marker DEVELOPMENT_ONLY, không có Live approval.
Nếu lỗi, dừng, đọc lỗi; không tiếp tục seed hoặc xóa bảng để chữa. Reset database là
thao tác phá hủy không thể rollback bằng transaction; chỉ bản backup trước đó có thể
khôi phục dữ liệu đã xóa. Các migration bên trong có transaction riêng.

### Bước 2 — seed

Mở `DB_Data_2.sql`, chọn toàn bộ file rồi Execute trong context master/UTEFitDB.
Script chỉ ghi UTEFitDB có marker phát triển và business tables trống. Có transaction,
applock và marker phiên bản. Chạy lại sau khi đã hoàn tất sẽ báo Already seeded,
không đổi mật khẩu, ngày hạn, dữ liệu tài chính hay lựa chọn đã chỉnh.

Kết quả mong đợi: ADMIN=10, MEMBER=20, PT=20, MANAGER=5.
Không có tài khoản ngân hàng, webhook, bank audit, QR token, OTP hoặc approval Live mẫu.
Payments mẫu chỉ là CASH và có mã `DEMO-CASH-*`. Không được hiểu là tiền ngân hàng thật.

### Kiểm tra chỉ đọc sau khi chạy

```sql
USE UTEFitDB;
SELECT DB_NAME() AS database_name;
SELECT COUNT(*) AS table_count FROM sys.tables WHERE schema_id=SCHEMA_ID('dbo');
SELECT role,COUNT(*) AS demo_accounts FROM dbo.users WHERE email LIKE '%@utefit.test' GROUP BY role;
SELECT COUNT(*) AS invalid_active_memberships FROM dbo.memberships m
WHERE status='ACTIVE' AND NOT EXISTS
(SELECT 1 FROM dbo.payments p WHERE p.membership_id=m.id AND p.status='PAID');
SELECT COUNT(*) AS approvals FROM dbo.sepay_deployment_guard;
DBCC CHECKCONSTRAINTS WITH ALL_CONSTRAINTS;
```

Các kết quả mong đợi: 28 bảng, 55 user đúng phân bổ, invalid_active_memberships=0,
approvals=0 và CHECKCONSTRAINTS không trả vi phạm.

## 3. Cấu hình môi trường, không đưa secret vào Git

Trong IntelliJ: Run → Edit Configurations → ứng dụng Spring Boot → Environment variables →
mở **bảng sửa từng biến**. Không dán JDBC URL chứa dấu chấm phẩy vào chuỗi phân tách
biến không được escape. Tham khảo `.env.example`; Spring Boot **không tự đọc file .env**.

Bắt buộc:
- `DB_USERNAME`: SQL login local.
- `DB_PASSWORD`: mật khẩu SQL local, không phải mật khẩu demo.
- `JWT_SECRET_BASE64`: ít nhất 32 byte ngẫu nhiên được Base64.
- `DB_URL` nếu SQL Server không ở localhost:1433.

URL phát triển mặc định:

```text
jdbc:sqlserver://localhost:1433;databaseName=UTEFitDB;encrypt=true;trustServerCertificate=true
```

`trustServerCertificate=true` chỉ cho SQL Server local với chứng chỉ tự ký. Không dùng
để thay thế việc xác minh TLS ở triển khai Live. Không cần đường dẫn keystore/truststore ổ F.

Có thể tạo JWT secret bằng PowerShell trên máy riêng (không gửi kết quả vào chat/Git):

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
[Convert]::ToBase64String($bytes)
$rng.Dispose()
```

Mặc định cổng 8080, SQL init=never, ddl-auto=validate. Chạy main Spring Boot từ IntelliJ,
mở `http://localhost:8080`.

### Cấu hình local đang có trên máy tác giả

Bản application.properties cũ được bảo vệ nguyên byte tại
`.local/application-local.properties` (ignored). Shared configuration import file này
nếu nó tồn tại; không đưa nó vào repository. Vì vậy máy tác giả vẫn giữ override
datasource/credential trước bàn giao, không tự chuyển Live sang UTEFitDB.

Khi tác giả muốn chạy development, hãy dùng Run configuration riêng với DB_URL=UTEFitDB
và các công tắc SePay=false; không sửa cấu hình Live đang hoạt động.
Thành viên clone mới không có file override này nên dùng mặc định UTEFitDB.
File private cũ có dấu conflict phải được chủ máy xem xét riêng; shared file đã sạch.
Không copy file private sang máy nhóm.

### Dịch vụ tùy chọn

- **Email/OTP**: tài khoản seed ACTIVE/enabled nên không cần gửi mail để đăng nhập.
  Đăng ký mới/quên mật khẩu vẫn yêu cầu SMTP thật hoặc mail server phát triển của nhóm.
  Gmail: MAIL_HOST=smtp.gmail.com, MAIL_PORT=587, MAIL_SMTP_AUTH=true, MAIL_STARTTLS=true,
  MAIL_USERNAME/MAIL_PASSWORD App Password/MAIL_FROM tự nhập riêng. Không tắt OTP.
- **Cloudinary**: CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET chỉ cần
  khi demo upload ảnh/video thật. Để trống vẫn chạy chức năng thông thường; upload chưa
  cấu hình sẽ không thành công. Seed không chèn URL video giả hoặc asset thuộc người thật.
- **WebSocket**: dùng cùng origin localhost; khi qua HTTPS, cấu hình origin và secure cookie
  theo profile đang có. Không nới wildcard/CORS/CSRF để demo.
- **SePay/VNPay**: SePay Live/nhận đơn mới/tra cứu/manual reconciliation mặc định false.
  VNPay mới đã bị vô hiệu hóa. Không cần ngân hàng để login, CASH, lớp, QR, review thông thường.
  SePay cần môi trường/credentials/approval riêng, không bật Live trên database có 55 tài khoản demo.
  Không import giao dịch từ UTEFitLiveDemo vào UTEFitDB.
- **QR camera**: localhost trên laptop hoặc HTTPS trên điện thoại; camera không chạy qua
  HTTP LAN thông thường. Giữ token một lần 120 giây và quy tắc checkout hiện có.

## 4. Chính xác 55 tài khoản DEMO

Tất cả có mật khẩu **123456789** (chỉ phát triển), BCrypt tạo bằng PasswordEncoder
thực tế của project. Không lưu plaintext vào cột password. ADMIN/MEMBER/PT/MANAGER
là enum hiện có, Manager có quan hệ phân công chi nhánh, không được cấp ADMIN.

| # | ADMIN | MEMBER | PT | MANAGER |
|---|---|---|---|---|
| 01 | admin01@utefit.test | member01@utefit.test | pt01@utefit.test | manager01@utefit.test |
| 02 | admin02@utefit.test | member02@utefit.test | pt02@utefit.test | manager02@utefit.test |
| 03 | admin03@utefit.test | member03@utefit.test | pt03@utefit.test | manager03@utefit.test |
| 04 | admin04@utefit.test | member04@utefit.test | pt04@utefit.test | manager04@utefit.test |
| 05 | admin05@utefit.test | member05@utefit.test | pt05@utefit.test | manager05@utefit.test |
| 06 | admin06@utefit.test | member06@utefit.test | pt06@utefit.test | — |
| 07 | admin07@utefit.test | member07@utefit.test | pt07@utefit.test | — |
| 08 | admin08@utefit.test | member08@utefit.test | pt08@utefit.test | — |
| 09 | admin09@utefit.test | member09@utefit.test | pt09@utefit.test | — |
| 10 | admin10@utefit.test | member10@utefit.test | pt10@utefit.test | — |
| 11 | — | member11@utefit.test | pt11@utefit.test | — |
| 12 | — | member12@utefit.test | pt12@utefit.test | — |
| 13 | — | member13@utefit.test | pt13@utefit.test | — |
| 14 | — | member14@utefit.test | pt14@utefit.test | — |
| 15 | — | member15@utefit.test | pt15@utefit.test | — |
| 16 | — | member16@utefit.test | pt16@utefit.test | — |
| 17 | — | member17@utefit.test | pt17@utefit.test | — |
| 18 | — | member18@utefit.test | pt18@utefit.test | — |
| 19 | — | member19@utefit.test | pt19@utefit.test | — |
| 20 | — | member20@utefit.test | pt20@utefit.test | — |

Không dùng các tài khoản/mật khẩu này trong Live. Không nhập email .test vào SMTP
công cộng; đây là domain dành cho thử nghiệm. Phone seed bắt đầu 000, chỉ thỏa định dạng,
không phải số thuê bao thật. Không gọi hoặc gửi SMS.

## 5. Dữ liệu và kịch bản demo

- 5 chi nhánh, Manager01 → chi nhánh 1, …, Manager05 → chi nhánh 5.
- 20 PT: pt01–04 ở chi nhánh 1, pt05–08 ở 2, pt09–12 ở 3, pt13–16 ở 4, pt17–20 ở 5.
- 55 gói: mỗi chi nhánh 11 mẫu ngày/tuần/tháng/năm, bao gồm thưởng tuần/tháng.
- Member01: chưa mua gói.
- Member02: một gói ALL đang sử dụng tại chi nhánh 1; có lớp, PT request/session, chat và review.
- Member03: gói đã hết hạn.
- Member04: kỳ đang dùng và kỳ nối tiếp đã PAID tại chi nhánh 1; có lớp PT ở kỳ tương lai.
- Member05: hai gói tại chi nhánh 1 và 2, độc lập ngày hạn.
- Member06: Membership/Payment CASH PENDING, chưa có quyền vào tập.
- Member07–20: các kỳ đang dùng luân phiên chi nhánh; Member19 có gói năm,
  Member20 có gói 2 năm và đang check-in mở để demo checkout.
- Lớp Gym/Yoga/PT, lịch quá khứ và tương lai, booking hợp lệ, check-in đóng/mở,
  review công khai/ẩn, promotion, notification, favorite và chat.
- Statistics là số liệu tổng hợp từ các bảng, **không có bảng Statistics riêng**.
  Doanh thu mẫu là CASH DEMO, không chứng minh thanh toán ngân hàng.

Demo theo thứ tự:
1. ADMIN login → gói tập: thêm gói một chi nhánh, thời hạn/thưởng; quản lý lớp/lịch/PT.
2. MEMBER02/04 → Gói tập của tôi: kỳ hiện tại/tương lai, tổng hạn, mua thêm,
   CASH chờ Admin xác nhận; chỉ PAID mới xếp kỳ.
3. MEMBER05 → hai chi nhánh độc lập. MEMBER06 → thử đặt lớp/check-in phải bị chặn.
4. MEMBER04 → lớp PT tương lai đã được kỳ PAID bao phủ; kiểm tra ngoài hạn/sai quyền lợi.
5. MANAGER01 → chỉ chi nhánh 1, không truy cập chi nhánh 2 bằng ID.
6. PT01 → lớp/lịch, Member02 và chat; không thao tác dữ liệu của PT khác.
7. MEMBER20 → tạo QR Checkout; ADMIN/MANAGER chi nhánh tương ứng quét.
8. MEMBER02 → review, đổi rating, ảnh/video nếu có Cloudinary; ADMIN ẩn và kiểm tra trang public.

Ngày mẫu tính tại lúc seed; chạy lại không kéo dài kỳ đã thanh toán hoặc tự làm mới
lịch sử. Muốn bộ demo theo ngày mới, chỉ reset môi trường phát triển sau backup/phê duyệt,
không chạy UPDATE hàng loạt lên tiền thật.

## 6. Database hiện có và migration

Không reset UTEFitDB đang dùng để cập nhật phiên bản. Giữ các migration V701–V712 để
nâng cấp dữ liệu đang có theo thứ tự, đọc prerequisite và trạng thái đã áp dụng trước.
Sao lưu, kiểm tra trên bản clone được phép, duyệt SQL rồi mới chạy bản chính.
Không sửa migration đã áp dụng, không chạy lại toàn bộ migration trên baseline mới.

Hai baseline mới đã bao gồm cấu trúc V701–V712. Các file
`database/UTEFitDB_PT.sql`, `database/UTEFitDB_FULL.sql` và
`database/live-demo/*` chỉ là tài liệu/script lịch sử, **không thuộc quy trình dựng
database nhóm**. Chưa xóa file của thành viên khác. Không chạy chúng như bước thứ ba.

Đã đọc metadata ba database bằng Windows authentication và SELECT sys catalog,
không đọc/chuyển bản ghi tài chính và không thay đổi database:

| Database | Bảng | Cột | FK | CHECK | Index |
|---|---:|---:|---:|---:|---:|
| UTEFitDB | 22 | 183 | 33 | 12 | 33 |
| UTEFitLiveDemo | 28 | 256 | 44 | 34 | 50 |
| UTEFitQA_GD7_20261025 | 24 | 212 | 36 | 18 | 42 |

UTEFitDB còn thiếu cấu trúc SePay, đối soát, snapshot Membership và thời hạn lịch;
QA cũng chưa đủ cấu trúc mới. Xem DATABASE_HANDOFF_REPORT.md để biết các mục thiếu.
Không khẳng định ba database trùng nhau, không tự nâng cấp/reset database hiện có.
Không chuyển bất kỳ Payment, bank reference hoặc webhook thực nào giữa ba DB.

## 7. Kiểm thử và giới hạn xác minh

Chạy `mvnw.cmd test` trên máy đã cấu hình test dependencies; test resources dùng H2 riêng.
Không chạy CloudinaryIntegrationTest với credentials thật nếu không có phê duyệt upload.

Các test bàn giao:
- Kết quả chạy cuối: Maven BUILD SUCCESS, 261 Java tests PASS; 42 JavaScript tests PASS.
- DatabaseHandoffTest: chạy **đúng block INSERT 55 users** trên H2, xác minh BCrypt,
  authenticate, login/JWT cả bốn role, Manager scope, không tắt OTP.
- PortableConfigurationTest: cấu hình chia sẻ không hardcode secret/đường dẫn máy,
  Cloudinary trống vẫn tạo được bean.
- LiveDemoSchemaPreparationTest: xuất/đối chiếu metadata SQL Server, **không kết nối DB**.
- Regression Membership/SePay/CASH/QR/Review/Manager/Favorites và Node tests.

**Chưa PASS trên SQL Server:** chạy cả script schema+seed, DBCC CHECKCONSTRAINTS,
idempotency seed trên SQL Server, startup ddl-auto=validate trên UTEFitDB đã dựng và
E2E trình duyệt. Người quản trị cần thực hiện sau khi có môi trường được phép.
Không có migration/reset/seed nào được tự chạy trên SQL Server trong tác vụ này.
Đã kiểm tra cú pháp hai file và 14 phần SQL động bằng Microsoft ScriptDom TSql160:
0 lỗi cú pháp; đây không phải bằng chứng đã dựng/seed thành công trên SQL Server.

## 8. Chuẩn bị GitHub, chưa commit/push/merge

### Quy trình main-only

Từ 30/10/2026, nhóm phát triển trực tiếp trên `main`. Trước khi sửa, kiểm tra
`git branch --show-current` và `git status`. Trước khi đồng bộ, sao lưu cả thay đổi
tracked và untracked cần thiết, fetch `origin`, tích hợp code mới rồi chạy compile/test.
Không tiếp tục công việc trên `feat/giai-doan-7`; nhánh cũ có thể giữ để đối chiếu lịch sử.
Không dùng reset/clean phá hủy dữ liệu, force push hoặc chọn ours/theirs hàng loạt.

Uyên phụ trách commit/push đợt đồng bộ này sau khi nhóm duyệt. Chỉ khi Uyên đã cung cấp
chính xác thông tin, cấu hình ở repository này bằng:

```powershell
git config --local user.name "TEN_CHINH_XAC_UYEN_CUNG_CAP"
git config --local user.email "EMAIL_CHINH_XAC_UYEN_CUNG_CAP"
git config --show-origin --get user.name
git config --show-origin --get user.email
```

Các giá trị trên là placeholder hướng dẫn, không chạy nguyên văn. `user.name`/`user.email`
là metadata author/committer của commit mới, không đổi tài khoản GitHub dùng để push.
Tài khoản push do credential/SSH hiện có quyết định; không suy ra từ email commit và
không tự đổi đăng nhập. Giữ tác giả các commit cũ; chỉ thêm ghi nhận đồng tác giả khi
đúng đóng góp thực tế và đã xác nhận thông tin. Không gán toàn bộ lịch sử cho người push.

Review `git status` và stage theo tên file, không `git add .`. Nên commit sau review:
source/template/static/test mới, hai SQL chuẩn, migration mới, tài liệu liên quan,
README, .gitignore và các *.example đã kiểm tra chỉ có placeholder.

Tuyệt đối không push:
`.local/`, `.ai/`, `.playwright-mcp/`, `target/`, log, .env thật, QA env script,
dump/backup SQL Server, JWT/API/HMAC credentials, keystore/truststore/private certificate.
`docs/` chưa được đưa vào commit tự động; chỉ người phụ trách chủ động chọn file
đã rà soát bản quyền/PII/secret. Không push mù toàn bộ file untracked.

**CẢNH BÁO LỊCH SỬ SECRET:** kiểm tra đã phát hiện literal Cloudinary API key/secret
trong các commit b770fc3/af94c8d và các literal mail/JWT cần rà soát trong af94c8d/49bd4e9.
Không in giá trị ở đây. Phải coi credentials còn hiệu lực là đã lộ: chủ tài khoản tự
thu hồi/thay khóa và rà soát toàn lịch sử trước public/push. Xóa khỏi working tree hoặc
thêm .gitignore không xóa được lịch sử. Chưa tự rewrite history hoặc force push.

Tích hợp nhánh an toàn:
1. Review diff/staged diff, rà soát secret và bảo vệ các thay đổi local.
2. Tạo checkpoint local theo feature khi được chủ dự án yêu cầu.
3. Nhóm kiểm thử hai SQL trên SQL Server phát triển được phép và test bốn role.
4. Push nhánh feat/giai-doan-7 khi được duyệt, mở PR vào main.
5. Người phụ trách review/test, giải quyết conflict; không mang config local vào PR.
6. Merge PR sau phê duyệt. Thành viên pull main rồi dựng mới bằng hai file hoặc nâng
   cấp bằng migration phù hợp; không reset database có dữ liệu để cập nhật code.

**Chưa đủ điều kiện khẳng định sẵn sàng public GitHub:** còn kiểm thử SQL Server và
xử lý credentials trong lịch sử. Không có commit/push/merge tự động.
