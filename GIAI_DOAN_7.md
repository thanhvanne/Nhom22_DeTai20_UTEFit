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

## Các bước tiếp theo

QR Check-in → Favorites → Review Video → Manager. Chỉ chuyển bước sau khi build/test bước trước; báo cáo và checkpoint local cập nhật theo tính năng. Không push/merge.
