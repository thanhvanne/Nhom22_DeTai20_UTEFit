# UTEFit — Giai đoạn 7

Branch: `feat/giai-doan-7`. Baseline: `6833d35`, tag `checkpoint/gd7-before-implementation-20261025`.

## Quy trình an toàn

- Các lỗi giai đoạn 0–6 trong báo cáo 25/10/2026 chưa được sửa trong phạm vi này.
- Maven test dùng H2 in-memory ở `src/test/resources/application.properties`; không kết nối SQL Server đang dùng.
- Demo dùng profile `qa`, cổng 8081, database mới `UTEFitQA_GD7_20261025`. Profile từ chối URL database khác.
- Cấu hình phiên QA cục bộ nằm trong `target/gd7-qa-env.ps1` (Git ignore). Không đưa file này vào commit.
- Chạy QA: dot-source file cấu hình cục bộ, rồi `java -jar target/utefit-0.0.1-SNAPSHOT.jar --spring.profiles.active=qa --spring.config.additional-location=src/main/resources/application-qa.properties`.
- Tài khoản QA: `qa.member@utefit.test`, `qa.member2@utefit.test`, `qa.pt@utefit.test`, `qa.admin@utefit.test`. Mật khẩu lấy từ biến `UTEFIT_QA_PASSWORD` của phiên QA; không ghi vào báo cáo.

## 1. VNPay Sandbox

Mở rộng Payment/Membership/CASH hiện tại, không tạo hệ thống billing khác. Tài liệu chính thức: https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html (API 2.1.0).

- Tạo Payment PENDING theo giá snapshot, ký URL HMAC-SHA512, chỉ gửi tới host Sandbox cố định.
- Member chỉ thanh toán đơn của mình; Membership và pending payment được tuần tự hóa qua lock.
- Gửi lại trong 15 phút dùng lại giao dịch; pending CASH không bị chuyển sang VNPay. Giao dịch VNPay hết hạn chờ đối soát, không tự giả định thất bại để mở giao dịch mới.
- Return URL `/payments/vnpay/return` chỉ hiển thị trạng thái, không kích hoạt gói.
- IPN GET `/payments/vnpay/ipn` không yêu cầu login, nhưng bắt buộc chữ ký, TMN, reference, amount và các trường trạng thái hợp lệ; từ chối tham số lặp.
- IPN cập nhật trong transaction, khóa Membership trước Payment; callback lặp trả `02`, không kích hoạt lại.
- Tiền đã nhận được ghi PAID kể cả đơn có trạng thái cần đối soát; không tự mở lại Membership đã hủy.
- Chặn hủy local khi VNPay còn PENDING để không đua với thanh toán tại cổng.
- CASH dùng lại logic kích hoạt chung; có test regression cho thời hạn snapshot và xác nhận lặp.

### Cấu hình và demo

Đặt `VNPAY_ENABLED=true`, `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`, `VNPAY_RETURN_URL` ngoài source theo `.env.gd7.example`. File mẫu không được ứng dụng tự nạp như dotenv.

Đăng ký IPN URL HTTPS công khai tới `/payments/vnpay/ipn` với VNPay; localhost không nhận được IPN server-to-server từ Internet. Return URL QA là `http://localhost:8081/payments/vnpay/return` nếu môi trường merchant cho phép. Không tắt CSRF toàn hệ thống, không bật môi trường tiền thật.

Member chọn Branch/Plan → tạo gói PENDING → bấm VNPay Sandbox → thanh toán tại Sandbox → Return hiển thị kết quả IPN đã xác nhận hoặc trạng thái chờ. Khi thất bại có thể thử lại từ gói PENDING sau khi IPN đã cập nhật FAILED. Nếu IPN không đến, giữ giao dịch chờ đối soát thay vì giả thành công/thất bại.

### Database và khôi phục

`database/migrations/V701__vnpay.sql` bổ sung hai cột nullable để lưu transaction number và response code của gateway, tương thích Payment cũ. Chưa áp dụng vào `UTEFitDB`. Chạy migration trên bản sao/QA trước khi triển khai dữ liệu chính; script dùng transaction và kiểm tra cột tồn tại. Không tự drop bảng/cột khi rollback code; giữ dữ liệu thanh toán để đối soát.

### Kiểm thử

- Maven test/package: 17 test, 0 failures/errors, 1 Cloudinary integration test skip do thiếu file/credentials.
- Đã test bằng mock/in-memory: tạo pending, URL Sandbox, giá snapshot, ownership, pending CASH, callback sai chữ ký/số tiền/đơn, hủy/thất bại, callback lặp, Return không cập nhật, CASH kích hoạt đúng thời hạn một lần.
- Thanh toán Sandbox thật và IPN công khai: BLOCKED cho đến khi có credentials và endpoint HTTPS của merchant. Không dùng callback giả kích hoạt SQL Server đang sử dụng.
- Playwright QA: tạo Membership PENDING thành công; nút VNPay thiếu cấu hình trả thông báo; IPN không có chữ ký trả `97`. CASH QA tạo yêu cầu → Admin xác nhận → SQL QA xác nhận Payment PAID 100000 VND và Membership ACTIVE. Bằng chứng: `.playwright-mcp/page-2026-10-25T09-02-28-946Z.yml`, `09-02-32-858Z`, `09-03-59-703Z` và snapshot sau xác nhận.

## 2. QR Check-in

- Dùng lại `AdminCheckinService`, không tạo lịch sử check-in song song. QR là token ngẫu nhiên 256 bit, SQL chỉ lưu SHA-256; không chứa email, mật khẩu hoặc JWT. Hết hạn sau 120 giây, một lần sử dụng, làm mới thu hồi mã cũ; kiểm tra tokenVersion của tài khoản.
- Khóa User trước token trong transaction; kiểm tra Member ACTIVE/enabled, Membership và chi nhánh qua service hiện có. Hai lượt quét đồng thời chỉ tạo một check-in. Sai chi nhánh không đốt token. Logout/thu hồi phiên làm QR cũ mất hiệu lực.
- Member: menu tài khoản → QR Check-in → Tạo/làm mới → đưa QR cho Admin. Admin: Check-in → chọn chi nhánh → mở camera hoặc nhập mã tại quầy → Xác nhận QR. Checkout dùng nút cũ.
- Camera dùng BarcodeDetector của trình duyệt; thiết bị không hỗ trợ có thông báo và phương án nhập mã. Quét camera vật lý: BLOCKED trên trình duyệt QA hiện tại; không báo PASS.
- Migration bổ sung `database/migrations/V702__qr_checkin.sql`; chưa chạy trên UTEFitDB. Không sửa lịch sử cũ. Rollback code giữ bảng token (có thể xóa token hết hạn theo chính sách vận hành sau này).
- 29 Maven test, 0 failures/errors, 1 Cloudinary skip; 12 test QR gồm expiry/tamper/replay/refresh/locked/branch/membership/manual conflict/RBAC và hai thread đồng thời. Package PASS sau khi tách JAR chạy khỏi đầu ra build Windows.
- Playwright SQL QA: QR ảnh PNG 300px, sai chi nhánh báo lỗi, đúng chi nhánh tạo check-in #1, dùng lại bị chặn, checkout thành công. Bằng chứng `.playwright-mcp/page-2026-10-25T09-16-57-594Z.yml`, `09-17-36-956Z`, `09-17-45-551Z`, `09-17-59-875Z`, `09-18-22-553Z`.
- Chạy QA bằng `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run-qa.ps1`; script sao chép JAR sang `target/qa-runtime` trước khi chạy để build sau không bị khóa JAR. Cấu hình WebSocket QA dùng origin 8081, không thay cấu hình môi trường chính.

## 3. Favorites

- Member lưu PT, Class, Branch trên danh sách và chi tiết đang có; menu tài khoản → Yêu thích. Có thể mở mục hoặc bỏ lưu. Lớp chưa có trang chi tiết riêng nên mở danh sách lớp của chi nhánh.
- Khóa Member và unique `(user_id,target_type,target_id)` chống trùng/race; service kiểm tra role/trạng thái, quyền sở hữu khi xóa, mục tiêu tồn tại/hoạt động. Xóa Favorite không xóa đối tượng. Mục ngừng hoạt động/mất được giữ như bookmark không khả dụng để Member tự bỏ.
- Target polymorphic không đặt FK tới ba bảng khác nhau; backend xác minh mỗi lần thêm và resolve trạng thái khi đọc. Không tạo Booking/PT Request, không thay sức chứa. Redirect chỉ nhận allowlist nội bộ.
- Migration bổ sung `V703__favorites.sql`, chưa áp dụng UTEFitDB; rollback code giữ bảng, không cần xóa dữ liệu.
- Build/package PASS: 35 tests, 0 failures/errors, 1 skipped. Sáu test mới kiểm tra ba loại, concurrent duplicate, ownership, inactive/deleted, RBAC/CSRF và redirect.
- Playwright SQL QA PASS: lưu Class #1, Trainer #1, Branch #1 từ giao diện; reload còn đủ ba mục; gửi thêm Class cũ không nhân bản; bỏ Branch thành công. SQL QA không phát sinh booking/yêu cầu PT. Bằng chứng `.playwright-mcp/page-2026-10-25T09-25-05-315Z.yml`, `09-25-13-613Z`, `09-25-19-316Z`, `09-25-22-494Z` và snapshot sau bỏ lưu.
- Hồi quy QR: Member xem lịch sử có check-in/checkout #1 tại `/member/history`, snapshot `09-23-27-122Z`.

## 4. Review Video

- Mở rộng Review/MemberReviewService/CloudinaryService hiện có, thêm `video_url`, `video_public_id` nullable qua `V704__review_video.sql`. Không lưu binary SQL. Review cũ và ảnh cũ tương thích.
- MP4/WebM tối đa 20 MB: kiểm tra extension + MIME + cấu trúc header ở backend, sau đó Cloudinary phải decode xác nhận video có width/height/duration/bytes hợp lệ. Không chỉ tin extension hoặc MIME của trình duyệt. Tài liệu: https://cloudinary.com/documentation/java_image_and_video_upload.
- Upload server-side `resource_type=video`, không overwrite. Provider thất bại không cập nhật review. Commit mới dọn video cũ; rollback dọn video mới. Lỗi dọn asset được log để xử lý orphan, không phá transaction đã commit.
- Dùng eligibility/rating/ownership và chống duplicate hiện có trước upload; update video kiểm tra lại bằng chứng trên target gốc. Không cho đổi target, không tự bật visible khi sửa review đã bị ẩn. Ẩn review loại cả URL video khỏi danh sách công khai và trang chủ; URL CDN từng được chia sẻ trước đó không phải URL private/thu hồi tự động.
- Form hiện có cho upload/thay video hoặc chọn xóa video (không đồng thời); Member/Admin xem video trong Review, public chỉ xem review visible. Giới hạn multipart request 26 MB, mỗi file 20 MB; ImageValidator vẫn giới hạn ảnh 5 MB.
- Build toàn bộ PASS: 52 tests, 0 failures/errors, 1 Cloudinary integration skip. Sau bổ sung rollback test và kiểm tra metadata, build/package mục tiêu PASS thêm 18 test video/validator/provider. Test mock không phải bằng chứng upload Cloudinary thật.
- Playwright SQL QA PASS: file text đổi đuôi .mp4 bị từ chối, tạo review không media và sửa nội dung thành công, SQL chỉ có một review. Bằng chứng `.playwright-mcp/page-2026-10-25T09-36-11-910Z.yml`, `09-36-29-035Z`, `09-36-43-679Z`.
- Upload/playback Cloudinary thật: BLOCKED, chưa có credentials QA riêng. Không dùng tài khoản Cloudinary thật hoặc ghép URL giả vào SQL để báo thành công. Cấu hình `QA_CLOUDINARY_CLOUD_NAME`, `QA_CLOUDINARY_API_KEY`, `QA_CLOUDINARY_API_SECRET` ngoài Git trước khi demo video hợp lệ.
- Demo: Member có check-in đã checkout → Đánh giá → Viết/Sửa → chọn MP4/WebM → lưu → xem video; Admin → Đánh giá → ẩn → public không thấy review. Để xóa video, vào Sửa, tick Xóa video hiện tại và không chọn file mới.
- Rollback code có thể giữ hai cột nullable và tài nguyên Cloudinary, không tự xóa media/dữ liệu cũ. Chưa áp dụng migration trên UTEFitDB.

## 5. Manager theo chi nhánh

- Role MANAGER riêng, không dùng ROLE_ADMIN, không mở `/admin/**` cho Manager. Portal `/manager` chỉ vận hành lịch lớp và check-in tại chi nhánh được giao; không được quản lý User/role, Payment, cấu hình hệ thống, Plan/Promotion hay tài chính toàn chuỗi.
- Admin → Manager chi nhánh → tạo tài khoản MỚI → phân công một/nhiều chi nhánh; có thể thu hồi từng chi nhánh. Không chuyển role tài khoản cũ, không đổi mật khẩu tài khoản đang có. Tạo mới yêu cầu mật khẩu 12–72 ký tự, BCrypt server-side.
- `ManagerAccessService` xác minh role/ACTIVE/enabled và assignment từ DB trên mỗi request. Thao tác ghi khóa Manager để tuần tự với grant/revoke; lịch và lớp được khóa/đọc lại trước kiểm tra branch nhằm tránh đổi scope giữa kiểm tra và cập nhật.
- `ManagerOperationsService` chỉ là lớp kiểm soát scope, gọi lại AdminScheduleService/AdminCheckinService/QrCheckinService. Kiểm tra cả branchId, scheduleId/checkinId và lớp thay thế khi sửa lịch. Không nhân bản logic thanh toán, booking, notification hoặc check-in.
- Manager có thể tạo/sửa/hủy/mở lại lịch của lớp tại chi nhánh được giao; việc hủy booking và gửi thông báo vẫn theo nghiệp vụ hiện tại. Không khẳng định các rủi ro lịch cũ 0–6 đã được xử lý.
- `V705__manager_scope.sql` thêm bảng assignment và mở CHECK role hiện có bằng transaction. Chỉ chấp nhận CHECK role đơn giản đã nhận diện; constraint phức tạp sẽ abort thay vì tự xóa mù. CHECK status và account hiện có không đổi. Đã chạy cả V701–V705 trên SQL QA và chạy V705 lần hai thành công; còn nguyên 4 account/1 payment trước khi tạo Manager QA. UTEFitDB chỉ được đọc metadata CHECK, không migration/thay đổi dữ liệu.
- Build/package toàn bộ PASS: 62 tests, 0 failures/errors, 1 integration Cloudinary skip. Chín test Manager kiểm tra role/scope/record ID/class thay thế, account mới, grant trùng, revoke, lock, CSRF, create/update/cancel lịch và check-in dùng chung.
- Playwright SQL QA PASS: Admin tạo `qa.manager@utefit.test` (#5), giao Branch #1; Manager chỉ thấy Branch #1. Fetch Branch #2, Admin Dashboard/Payment/User/Manager, Member/PT endpoints đều 403. Tạo lịch #2, sửa phòng, QR check-in #2/checkout, manual check-in #3, hủy và mở lại lịch. Admin checkout #3 vẫn hoạt động.
- Admin thu hồi Branch #1 → login lại Manager thấy chưa được giao chi nhánh, URL branch #1 trả 403. Test backend xác nhận revoke có hiệu lực ở request tiếp theo với authority cũ. Sau kiểm thử đã giao lại Branch #1 để demo.
- Bằng chứng: `.playwright-mcp/page-2026-10-25T09-45-19-466Z.yml`, `09-45-29-322Z`, `09-45-42-019Z`, `09-46-52-435Z`, `09-47-20-773Z`, `09-48-56-719Z`, `09-49-24-844Z`, `09-49-32-271Z`, `09-50-50-856Z`, `09-51-11-487Z`, `09-51-42-328Z`.
- Demo scope: đăng nhập Manager QA → chỉ Branch #1; thử `/manager?branchId=2` và `/admin/payments` → 403. Đăng nhập Admin QA → thu hồi Branch #1 → Manager không còn truy cập branch đó; giao lại để tiếp tục demo. Mật khẩu nằm trong cấu hình local ignored, không ghi vào hướng dẫn này.
- Lưu ý rollback **quan trọng**: code cũ chưa biết enum MANAGER. Không chạy binary cũ trên database đã có account MANAGER mà chưa lập phương án tương thích. Giữ enum MANAGER và chặn portal khi rollback nghiệp vụ, hoặc trên bản sao DB có backup, chuyển riêng account Manager MỚI sang MEMBER + disabled/LOCKED + tăng tokenVersion sau phê duyệt; không đụng account cũ. Không tự chạy downgrade dữ liệu. UTEFitDB chưa thay đổi nên phiên bản gốc vẫn dùng được với database gốc.

## Kiểm thử liên role

Manager tạo/sửa lịch #2 → Member QA đăng ký qua `/member/classes` → PT QA xem `/pt/classes/2` có đúng một học viên → Manager hủy lịch → SQL registration #1 CANCELLED. Mở lại lịch không phục hồi booking đã hủy. Membership #1 vẫn ACTIVE, Payment #1 vẫn PAID 100000 VND. Bằng chứng `09-47-34-882Z`, `09-47-56-382Z`, `09-48-34-717Z` và truy vấn QA sau hủy.

Admin Statistics sau ba lượt check-in hiển thị 3 lượt và doanh thu PAID 100000 VND, Membership hiệu lực 1; khớp dữ liệu QA. Bằng chứng `09-51-46-877Z`. Review QA đã bị Admin ẩn và public không còn thấy (`09-37-57-192Z`). Video thật/tiền Sandbox thật chưa được kiểm chứng, không tính PASS.

## Tổng kết triển khai (25/10/2026)

| Tính năng | Trạng thái | Đã triển khai | Build | Test | Demo web |
|---|---|---|---|---|---|
| VNPay | PARTIAL | URL Sandbox, Payment pending, Return/IPN, signature/amount/owner/idempotency, activation dùng chung CASH | PASS | Unit/mock PASS; gateway thật BLOCKED | Form/config guard PASS; Sandbox thật BLOCKED |
| QR Check-in | PARTIAL | QR cá nhân 120s, một lần, scanner Admin/Manager, fallback tại quầy | PASS | H2 + SQL QA PASS; camera vật lý BLOCKED | PNG, nhập QR, check-in/checkout PASS |
| Favorites | DONE | PT/Class/Branch, thêm/bỏ/xem/mở, scope Member | PASS | Backend và SQL QA PASS | PASS |
| Review Video | PARTIAL | Upload/thay/xóa, validation, cleanup transaction, moderation | PASS | Mock/provider/validator PASS; Cloudinary thật BLOCKED | File giả/21 MB bị chặn, Review cũ PASS; video thật BLOCKED |
| Manager | DONE trong phạm vi vận hành nêu trên | RBAC riêng, assignments, scope lịch/check-in, Admin cấp/thu hồi | PASS | Backend + SQL QA PASS | PASS |

Không đánh dấu hoàn tất E2E cho các phần cần Sandbox/Cloudinary/camera chưa có điều kiện. Chưa triển khai production. Không tuyên bố các giai đoạn 0–6 đã được kiểm thử đầy đủ.

### Kết quả cuối

- Maven `package` bản release: **BUILD SUCCESS**, 64 tests = **63 PASS, 1 SKIP**, 0 failures/errors. Test skip là Cloudinary image integration cũ thiếu file/credentials.
- `git diff 6833d35 --check`: PASS, không có whitespace error.
- Test dùng H2 in-memory trong chế độ MSSQLServer; đây không phải SQL Server và không thay thế toàn bộ kiểm thử tương thích DB. Luồng web thực tế dùng SQL Server QA riêng trên cổng 8081 đã được khởi động lại bằng JAR mới sau mỗi module.
- Log build local: `target/gd7-favorites-build.log`, `target/gd7-video-build-final.log`, `target/gd7-video-verification.log`, `target/gd7-manager-build.log`, `target/gd7-final-build.log`, `target/gd7-vnpay-concurrency.log`, `target/gd7-release-build.log`. Reports chi tiết ở `target/surefire-reports` (không commit).
- Rà soát concurrency VNPay: entity đã được đọc trước khi chờ lock có thể còn trạng thái cũ trong persistence context. IPN mới refresh Membership/Payment sau khi lấy khóa để không xác nhận lại hoặc ghi đè trạng thái terminal. Test H2 dùng hai transaction/thread đã preload cùng trạng thái PENDING trước barrier: kết quả đúng một `00`, một `02`, một notification, một activation theo thời hạn snapshot. Không gửi callback giả vào SQL Server.
- Sau kiểm thử: Member QA có Membership ACTIVE và CASH PAID 100000 VND; 3 check-in đều đã checkout; Favorite Class/PT không trùng; registration lớp #2 CANCELLED dù lịch đã mở lại; Manager QA có một assignment Branch #1. Không xóa fixture để giả vờ trạng thái sạch.
- Upload 21 MB bị từ chối HTTP 413, không sửa review. Đã thêm trang lỗi tiếng Việt thay Whitelabel, kiểm thử MVC và browser trên JAR mới PASS. Bằng chứng `.playwright-mcp/page-2026-10-25T09-59-21-056Z.yml`. Đây là chỉnh sửa của tính năng video mới, không sửa lỗi nghiệp vụ cũ.

### Ma trận kiểm thử tiêu biểu

| Role / module | Thao tác / điều kiện | Môi trường và bằng chứng | Kết quả |
|---|---|---|---|
| Member / VNPay | Tạo pending, giá snapshot, sinh URL Sandbox | VnpayPaymentServiceTest | PASS (mock) |
| Member / VNPay | Member khác truy cập đơn; pending CASH; retry trùng | VnpayPaymentServiceTest | PASS (mock) |
| Gateway / VNPay | Sai chữ ký/amount/order; callback lặp; failed/cancel; activation một lần | VnpayPaymentServiceTest | PASS (mock) |
| Gateway / VNPay | IPN đồng thời với persistence context đã giữ trạng thái cũ | VnpayConcurrencyIntegrationTest, hai thread/transaction thật | PASS (H2) |
| Gateway / Return | Return không kích hoạt Membership | VnpayPaymentServiceTest | PASS (mock) |
| Gateway / IPN | Không chữ ký, không login → RspCode 97 | Browser QA endpoint | PASS |
| Member / VNPay | Thanh toán Sandbox thành công/thất bại/hủy và IPN Internet | Thiếu merchant credentials và HTTPS công khai | BLOCKED |
| Member → Admin / CASH | Tạo yêu cầu → xác nhận → Payment PAID/Membership ACTIVE | UI + SQL QA, snapshot 09-03-59 | PASS |
| Member / QR | Tạo ảnh QR, expiry, refresh/thu hồi token | UI PNG 300px + QrCheckinIntegrationTest | PASS |
| Admin / QR | Hợp lệ; sai chi nhánh; replay | UI + SQL QA check-in #1 | PASS |
| Admin / QR | Hết hạn/sửa mã/Member khóa/hết gói/branch inactive | QrCheckinIntegrationTest | PASS (H2) |
| Admin / QR | Hai thread đồng thời; open manual check-in; checkout | QrCheckinIntegrationTest | PASS (H2); checkout UI PASS |
| Guest/Member / QR Admin API | Sai quyền/CSRF | MockMvc security | PASS |
| Admin/Manager / QR | Camera thật quét mã từ thiết bị Member | BarcodeDetector không có trên trình duyệt QA | BLOCKED |
| Member / Favorites | Thêm PT/Class/Branch; reload; bỏ lưu | UI + SQL QA, 09-25-22/09-26-00 | PASS |
| Member / Favorites | Duplicate/concurrent/ownership/CSRF/role | FavoriteIntegrationTest; SQL duplicate chỉ một dòng | PASS |
| Member / Favorites | Target inactive/deleted; không tạo booking/request | FavoriteIntegrationTest + SQL QA | PASS |
| Member / Review | Rating/eligibility/ownership/duplicate trước upload | ReviewVideoIntegrationTest | PASS (H2/mock) |
| Member / Video | Đuôi/MIME/header/size giả; provider audio/fail | VideoValidatorTest + CloudinaryVideoTest; UI file giả | PASS |
| Member / Video | Thay/xóa/giữ ảnh; rollback dọn video mới | ReviewVideoIntegrationTest | PASS (mock) |
| Member / Video | Upload MP4/WebM hợp lệ rồi xem playback thật | Thiếu Cloudinary QA credentials | BLOCKED |
| Member / Video | File 21 MB; lỗi 413 có thông báo tiếng Việt | UI JAR cuối + HTTP 413, 09-59-21 | PASS |
| Member/Admin/Public / Review | Sửa Review cũ; ẩn review; public không hiển thị | UI SQL QA + test hidden video | PASS (video thực tế chưa có) |
| Admin / Manager | Tạo account mới; cấp/thu hồi; cấp trùng | UI QA + ManagerIntegrationTest | PASS |
| Manager / RBAC | Login; chỉ branch được giao; Admin/Member/PT URL bị chặn | UI + fetch status 403 + MockMvc | PASS |
| Manager / Scope | Đổi branchId, scheduleId, checkinId, lớp thay thế | H2 tests; branch URL SQL QA 403 | PASS |
| Manager / Lịch | Tạo/sửa/hủy/mở lại; booking chuyển CANCELLED | UI + SQL QA lịch #2 và registration #1 | PASS |
| Manager / Check-in | QR và thủ công; checkout; Admin vẫn thấy cùng lịch sử | UI SQL QA records #2/#3 | PASS |
| Admin / Scope | Thu hồi assignment; request mới mất quyền | H2 authority cũ PASS; web login lại/403 PASS | PASS |
| Member ↔ PT | Member đặt lịch Manager tạo → PT thấy một học viên | UI 09-47-56 và 09-48-34 | PASS |
| Admin / Statistics | 3 check-in, doanh thu 100000, 1 Membership active | UI 09-51-46 + SQL QA | PASS |
| Các module cũ khác | Kiểm thử toàn bộ Chat/Booking/Payment legacy/Guest | Ngoài phạm vi hồi quy mục tiêu lượt này | NOT TESTED đầy đủ |

### Điểm cần hoàn thiện / rủi ro

| Ưu tiên | Hiện trạng / ảnh hưởng | Hướng xử lý |
|---|---|---|
| P1 | VNPay thật chưa được xác nhận, ảnh hưởng Payment/Membership/Revenue | Cấp merchant Sandbox, IPN HTTPS, chạy lại success/fail/cancel/retry/IPN duplicate/amount/signature trên QA; không thanh toán thật |
| P1 | Cloudinary thật chưa được xác nhận, upload/video playback còn BLOCKED | Dùng tài khoản Cloudinary QA riêng; test MP4/WebM hợp lệ, replace/remove, provider failure và moderation trên web |
| P1 triển khai | Database có MANAGER không tương thích enum của binary cũ | Backup/test migration trước deploy; rollback giữ enum/deny quyền hoặc chuẩn bị chuyển riêng account Manager mới về disabled role hợp lệ sau phê duyệt; không tự downgrade dữ liệu |
| P2 | Camera QR chưa được kiểm chứng, native BarcodeDetector không hỗ trợ mọi browser | Dùng fallback nhập mã đã test; demo camera trên thiết bị hỗ trợ và HTTPS/localhost; cân nhắc decoder local cho browser khác sau khi chốt thiết bị |
| P2, quan sát QA nền cũ | Notification đã gửi đúng recipient/booking nhưng một số dấu tiếng Việt thành `?` ở cả CASH và hủy lịch | Mapping Notification cũ không Nationalized; so sánh kiểu cột/collation QA với DB chính, lên migration Unicode riêng sau Stage 7. Chưa sửa vì không thuộc phạm vi lỗi mới |
| P2 vận hành | Cloudinary delete thất bại có thể để orphan; pending VNPay mất IPN cần đối soát | Theo dõi warning, có quy trình dọn/đối soát có kiểm chứng; không tự đổi PAID/FAILED hoặc xóa lịch sử để giải phóng đơn |

Không có bằng chứng để kết luận hệ thống không còn lỗ hổng; các test security ở đây là kiểm tra có mục tiêu, không phải pentest toàn hệ thống.

## Database và áp dụng an toàn

Ba bảng mới: `qr_checkin_tokens`, `favorites`, `manager_branch_assignments`. Bốn cột mới nullable: hai metadata gateway trên `payments`, hai metadata video trên `reviews`. Mở rộng CHECK role để nhận MANAGER, giữ nguyên CHECK status. Không drop bảng hoặc rewrite lịch sử core.

Đã áp dụng V701–V705 **chỉ trên** `UTEFitQA_GD7_20261025`; script chạy theo thứ tự bằng `sqlcmd -b -d UTEFitQA_GD7_20261025 -i <script>`. V705 đã chạy lặp thành công. Các script có transaction; lỗi làm rollback phần script. Cần backup và chạy trên bản sao dữ liệu trước khi triển khai DB chính; dừng ứng dụng ghi trong cửa sổ migration, kiểm tra counts/constraints trước và sau. Nếu constraint role khác mẫu được nhận diện, V705 cố ý BLOCKED để DBA xử lý thay vì xóa constraint chưa hiểu.

Không chạy JAR mới với `ddl-auto=update` trên dữ liệu chính như một cách thay thế migration kiểm soát. Khi triển khai thật nên override `spring.jpa.hibernate.ddl-auto=validate` sau migration đã duyệt. Giữ cột/bảng bổ sung khi rollback code; không tự drop hay phục hồi backup đè lên dữ liệu đang sử dụng. Nếu migration lỗi, xử lý trên bản sao trước; nếu cần restore, phục hồi thành DB khác để đối chiếu, không ghi đè DB nhóm.

## Git, checkpoint và khôi phục

- Nhánh duy nhất được chỉnh sửa: `feat/giai-doan-7`. Trước code tracked tree sạch; `.ai/`, `.playwright-mcp/`, `docs/` là nội dung untracked được giữ nguyên, không đưa vào commit.
- Baseline trước Stage 7: `6833d35`; tag local `checkpoint/gd7-before-implementation-20261025`.
- Checkpoint VNPay/QA: `9cf252d`.
- Checkpoint QR: `3c2bf5b`.
- Checkpoint Favorites: `fcd5392`.
- Checkpoint Review Video: `376a174`.
- Checkpoint Manager: `8322b8e`.
- Checkpoint hardening IPN đồng thời: `9f8ee95`.
- Checkpoint cuối ghi trong `git log` với tên `fix(gd7): clarify oversized uploads and finalize verified QA report` (không thể ghi hash của chính commit vào nội dung commit).
- Không push, merge, reset --hard, clean -fd hoặc thay lịch sử commit cũ. Không sửa `application.properties` gốc; file QA secret ở `target/gd7-qa-env.ps1` được Git ignore.
- Danh sách file tạo/sửa và mục đích: `GIAI_DOAN_7_FILES.md`; có thể kiểm chứng bằng `git diff --name-status 6833d35..HEAD`.

Khôi phục source có kiểm soát: bảo vệ thay đổi mới trước; dùng `git revert <checkpoint>` trên nhánh tính năng để đảo tính năng cụ thể, hoặc `git restore --source=<checkpoint> -- <danh-sach-file-đã-duyệt>` để lấy bản cũ rồi build/test lại. Không tự chạy các lệnh này trong lượt triển khai. Tính năng có phụ thuộc nhau, cần đảo theo thứ tự ngược; riêng Manager phải giải quyết tương thích role DB như đã nêu trước khi revert enum. Git checkpoint không phải backup SQL Server hay Cloudinary.

## Chạy và demo trên máy hiện tại

- Bản cũ ở 8080 không được dùng làm bằng chứng cho code mới và không bị dừng/chỉnh dữ liệu. Bản mới QA ở **http://localhost:8081**.
- Script `scripts/run-qa.ps1` đọc cấu hình local ignored, copy JAR build sang `target/qa-runtime` rồi chạy profile QA. Dừng đúng process QA trước khi chạy lại để tránh trùng cổng; không kill toàn bộ Java/IntelliJ.
- Tài khoản: `qa.member@utefit.test`, `qa.member2@utefit.test`, `qa.pt@utefit.test`, `qa.admin@utefit.test`, `qa.manager@utefit.test`. Mật khẩu được giữ riêng trong biến QA local, không đưa vào báo cáo.
- Cách demo từng module và các biến Sandbox/Cloudinary nằm trong từng mục ở trên. VNPay cần order Membership PENDING mới; không dùng Membership CASH đã ACTIVE làm giao dịch VNPay giả.
- Manager QA đã được giao lại Branch #1. Member có thể tạo QR mới để Admin/Manager quét/nhập. Favorite PT/Class còn sẵn để demo bỏ lưu. Review QA hiện bị ẩn, Admin có thể hiển thị lại hoặc Member sửa sau khi cấu hình video QA.

Các việc còn cần người phụ trách môi trường hỗ trợ: merchant VNPay Sandbox + IPN HTTPS, Cloudinary QA credentials và thiết bị/browser camera phù hợp. Không cần mật khẩu của ba tài khoản SQL Server thật đã cung cấp trước đó; không sử dụng chúng để thay đổi dữ liệu nhóm.
