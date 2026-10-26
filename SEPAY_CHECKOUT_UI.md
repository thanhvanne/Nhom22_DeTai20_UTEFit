# Giao diện trạng thái thanh toán SePay

## Phạm vi

Trang `/member/payments/{id}/sepay` chỉ đọc đơn thuộc Member đang đăng nhập.
Không tạo đơn, xác nhận tiền hoặc cộng thời hạn khi mở trang, refresh hay polling.
Không cần migration hoặc biến môi trường mới. Không thay đổi cấu hình Live.

Backend chọn trạng thái hiển thị từ database: PAID, FAILED, REFUNDED có ưu tiên
theo PaymentStatus; với Payment PENDING, có giao dịch cần đối soát thì REVIEW,
quá hạn thì EXPIRED, còn lại PENDING. REVIEW/EXPIRED là trạng thái giao diện,
không thêm enum hoặc ghi lại trạng thái tài chính.

- PENDING: giữ VietQR, snapshot VA và hướng dẫn chuyển khoản hiện có.
- PAID: màn hình thành công, số tiền, mã đơn, thông tin gói, quyền lợi,
  phạm vi chi nhánh, số ngày và thời hạn thực tế đã được ghi nhận.
  Không render khu vực chuyển khoản, số tài khoản hoặc nút sao chép.
- REVIEW/EXPIRED/FAILED/REFUNDED: thông báo riêng, không render QR chuyển khoản.

Kỳ ACTIVE được phân biệt bằng ngày tại múi giờ Asia/Ho_Chi_Minh: CURRENT,
FUTURE hoặc EXPIRED. Kỳ gia hạn tương lai không được ghi là đang sử dụng.
Ngày kết thúc hiển thị là ngày kết thúc **kỳ của đơn này**, không tự tính lại
hoặc cộng thời hạn khác. Quyền lợi/phạm vi/số ngày lấy từ snapshot Membership;
tên gói và tên chi nhánh lấy từ danh mục hiện tại (không có snapshot tên).

## Đồng bộ

JavaScript gọi GET API `/member/payments/{id}/sepay/status` có kiểm tra ownership,
không cache. PENDING/REVIEW kiểm tra mỗi 5 giây; PAID và PENDING đã hết hạn
kiểm tra mỗi 15 giây. Khi backend báo thay đổi màn hình/trạng thái gói/kỳ sử dụng,
ẩn ngay khu vực chuyển khoản, dừng đếm ngược và tải lại cùng trang để server
render thông tin mới. Không dựng màn hình PAID từ query string hoặc đồng hồ máy.
Lỗi mạng/phiên đăng nhập hiện thông báo và nút kiểm tra lại; không giả định PAID.

## Tự kiểm thử giao diện

1. Khởi động lại Spring Boot với cấu hình đang sử dụng để nạp backend mới.
2. Đăng nhập Member sở hữu đơn. Mở `/member/payments/2/sepay` nếu đây là đơn
   đã PAID của tài khoản đó: phải thấy “Thanh toán thành công!”, không có
   tài khoản ngân hàng, QR thanh toán hay nút sao chép. Tài khoản khác phải bị chặn.
3. Refresh: số ngày và ngày hết hạn không thay đổi. Kiểm tra ba nút Gói tập của tôi,
   Đăng ký lớp tập và QR Check-in.
4. Dùng đơn PENDING có sẵn trên QA: giữ nguyên QR và snapshot. Khi backend xác nhận
   hợp lệ bằng quy trình QA đang có, trang tự chuyển sang màn hình thành công.
   Không dùng webhook giả hoặc sửa Payment Live để kiểm tra giao diện.
5. Kỳ gia hạn đã thanh toán nhưng bắt đầu trong tương lai: hiện “chưa bắt đầu”,
   ngày bắt đầu/kết thúc của kỳ gia hạn, số ngày mua thêm; không ghi đang hiệu lực.
6. Với các trạng thái lỗi/đối soát/hoàn tiền có sẵn ở QA: thông báo tương ứng,
   không có hướng dẫn chuyển khoản. Ngắt mạng: hiện lỗi kiểm tra trạng thái và
   cho thử lại, không tự xác nhận tiền.
7. Kiểm tra màn hình điện thoại và desktop. Hiệu ứng dấu tích tôn trọng
   `prefers-reduced-motion`.

## File trong thay đổi này

- `src/main/java/com/utefit/service/SepayPaymentService.java`: DTO chỉ đọc,
  trạng thái trình bày và tóm tắt kỳ tập.
- `src/main/resources/templates/member/sepay-checkout.html`: các màn hình riêng.
- `src/main/resources/static/css/sepay-checkout.css`: CSS riêng responsive.
- `src/main/resources/static/js/sepay-checkout.js`: đồng bộ màn hình từ API.
- `src/test/java/com/utefit/service/SepayPaymentIntegrationTest.java`: render
  PENDING → PAID, reload không ghi dữ liệu, gia hạn tương lai và trạng thái lỗi.
- `src/test/js/sepay-checkout.test.cjs`: polling, chuyển màn hình, retry.
- `SEPAY_CHECKOUT_UI.md`: hướng dẫn này.

Kiểm thử tự động dùng H2/MockMvc và Node, không gọi webhook Live qua mạng hoặc
Cloudinary thật. Việc hiển thị thực tế của Payment #2 và responsive trên thiết bị
cần được chủ hệ thống xác nhận sau khi khởi động lại ứng dụng; không truy cập
database Live để xác nhận trong tác vụ này.
