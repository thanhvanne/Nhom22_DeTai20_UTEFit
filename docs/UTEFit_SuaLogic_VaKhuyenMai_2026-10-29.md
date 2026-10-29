# Sửa logic và tích hợp khuyến mãi — 29/10/2026

## Cập nhật database trước khi chạy ứng dụng

Database đang có dữ liệu: chạy `database/migrations/V715__membership_promotion_snapshot.sql` trong đúng database UTEFit của môi trường cần nâng cấp. Migration chỉ thêm ba cột nullable vào memberships: mã ưu đãi, giá gốc và phần trăm giảm. Không reset database hoặc seed lại. Phiên làm việc này chưa áp dụng migration vào database SQL Server đang dùng.

Dựng database phát triển mới: `DB_Khung_1.sql` đã chứa các cột và migration tương ứng. Các guard chống reset vẫn giữ nguyên. Đây không phải chỉ dẫn chạy reset trên database hiện tại.

## Luồng khuyến mãi

- Guest và Member xem `/promotions`, có liên kết từ menu, trang chủ và trang mua gói.
- Chỉ hiện mã active, trong khoảng ngày hiệu lực (bao gồm ngày cuối), còn lượt và có mức giảm hỗ trợ.
- Member nhập mã ở thẻ gói trước khi tạo đơn. Backend khóa mã, kiểm tra điều kiện, tính giá VND nguyên theo HALF_UP và giữ giá sau giảm trên đơn.
- Một đơn dùng một mã. Gửi lặp cùng đơn chờ và cùng mã không trừ lượt lần nữa. Nếu đơn chờ dùng ưu đãi khác, hệ thống yêu cầu xử lý đơn cũ thay vì âm thầm đổi giá.
- Một lượt được giữ khi tạo đơn; đơn chưa thanh toán bị hủy trực tiếp, được duyệt hủy hoặc bị dọn hợp lệ thì trả lượt trong cùng transaction. Đơn đang chờ duyệt hủy tiếp tục giữ lượt. Đơn đã thanh toán/hoàn tiền không tự cấp lại mã.
- CASH và SePay lấy cùng giá đã giảm trong snapshot. Sửa giá gói hoặc mức giảm sau đó không đổi số tiền đơn cũ.
- Mã đã gắn với đơn không được đổi tên; có thể tắt và tạo mã mới. Chưa hỗ trợ đơn miễn phí 100%; quản trị mã mới yêu cầu mức giảm nhỏ hơn 100%.
- Màn hình đơn hiển thị mã, tỷ lệ giảm và giá gốc khi có áp dụng.

## Đối chiếu 25 mục rà soát

| Mục | Thay đổi |
|---|---|
| L01 | Dọn đơn giữ lại CANCEL_REQUESTED; trả lượt mã chỉ khi đơn thực sự chuyển sang hủy. |
| L02 | Handshake ghi nhận ID, tokenVersion và thời điểm hết hạn JWT; inbound và outbound kiểm tra phiên/tài khoản; service từ chối người gửi bị khóa. |
| L03 | Form tạo lịch PT lấy học viên qua quan hệ ACCEPTED với PT đang đăng nhập. |
| L04 | Không đổi PT/chi nhánh/danh mục của lớp đã có lịch; tạo lớp mới để tránh đổi ngầm tài nguyên của các buổi. |
| L05 | Không sửa/tắt buổi đã bắt đầu; không đổi cấu trúc lớp đã có lịch; hủy lớp chỉ tắt/hủy đăng ký buổi tương lai. |
| L06 | Chặn tắt chi nhánh còn quyền lợi đã mua cho tương lai, phạm vi đa chi nhánh, lịch PT hoặc lượt đang tập. |
| L07 | Lịch nhóm phải ở tương lai và trong giờ chi nhánh; đổi giờ mở cửa bị chặn nếu xung đột lịch nhóm/PT đã xếp. |
| L08 | Danh mục đã gắn lớp không được đổi quyền lợi hoặc bật/tắt; tạo danh mục mới để giữ cam kết cũ. |
| L09 | Controller đặt/sửa lịch và các form quản trị bắt lỗi trạng thái nghiệp vụ, đưa thông báo về form. |
| L10 | PT xem cả REGISTERED, ATTENDED và ABSENT; hiển thị trạng thái, không mất hàng sau điểm danh; tổng số không loại người đã điểm danh. |
| L11 | Lịch tuần của PT lấy cả buổi đã hủy để giữ lịch sử. |
| L12 | Chỉ đánh dấu đã đọc các tin được trả về; API tin cũ cũng chỉ đánh dấu tập đang hiển thị. |
| L13 | Có API tải tin cũ theo ID, kiểm tra thành viên hội thoại; hai giao diện chat có nút tải thêm và giữ tin cũ khi poll. |
| L14 | Danh sách thông báo Member có trang trước/sau; API recent vẫn giữ giới hạn riêng. |
| L15 | Avatar Member dọn ảnh cũ sau commit/ảnh mới khi rollback. |
| L16 | Tạo chi nhánh có upload dọn ảnh nếu transaction rollback. |
| L17 | Khuyến mãi công khai, nhập mã khi mua, tính giảm và giữ/trả lượt, lưu snapshot để thanh toán. |
| L18 | Thống nhất firstName là họ/phần đầu, lastName là tên/phần cuối theo form đăng ký hiện tại; sửa profile, yêu cầu PT và tên PT trong danh mục. Không tự hoán đổi dữ liệu cũ. |
| L19 | Giá gói VND nguyên, tối đa 13 chữ số, kiểm tra ở DTO và service. |
| L20 | Đăng ký/gửi lại OTP xử lý MailException như các form khác. |
| L21 | Bỏ suy luận quyền dạy từ chuỗi chuyên môn tự khai; Admin trực tiếp lựa chọn PT là bước duyệt phân công. Chưa triển khai danh mục chứng chỉ chuyên môn. |
| L22 | PT phải được gán chi nhánh trước khi được bật nhận học viên. |
| L23 | Trang lịch sử lấy đủ review và không còn giới hạn PT ba năm trước/một năm sau. Tải toàn lịch sử cần tối ưu thêm khi dữ liệu lớn. |
| L24 | Chuẩn hóa trang quản trị không âm, kích thước trong 1–100. |
| L25 | HttpClient tra cứu SePay chỉ được tạo khi thật sự gọi chức năng và đã qua kiểm tra bật/cấu hình. |

## Chính sách và giới hạn

Việc đổi cấu trúc lớp/danh mục được xử lý theo chính sách bảo toàn lịch sử: chặn sửa đối tượng đã được sử dụng, tạo đối tượng mới khi thay đổi cam kết. Chưa có quy trình chuyển hàng loạt lịch đang sử dụng sang cấu trúc mới.

Bảy rủi ro R01–R07 trong báo cáo rà soát không đồng nghĩa đã được xử lý toàn bộ bởi đợt sửa 25 mục này. Đặc biệt cần kiểm thử đồng thời trên SQL Server trước khi kết luận về tranh chấp tài nguyên, OTP và lost update. Phiên này không tạo giao dịch ngân hàng, không gửi OTP thật và không sửa dữ liệu sản xuất.

## Kiểm thử

Kết quả cuối: **287 test Java: 286 đạt, 0 failure, 0 error, 1 bỏ qua** (Cloudinary thật chưa cấu hình). **44/44 test JavaScript đạt**. Kiểm tra cú pháp hai file chat JavaScript và `git diff --check` đạt.

Có 9 test bổ sung: 4 regression cho hủy đơn/tài khoản khóa/lịch quá khứ/tin chưa đọc và 5 test tích hợp cho áp mã, giữ/trả lượt, tính giá nguyên VND, gửi lặp, mã hết lượt, từ chối đổi mã trên đơn chờ, trang ưu đãi công khai/ô nhập Member và gói tương lai chặn đóng chi nhánh.

Bộ Java chạy với Java agent nạp trước và `forkCount=0` do môi trường Windows hạn chế attach. Kết quả: `target/fix-complete-verification.log`; JavaScript: `target/fix-js-verification.log`. Các test tích hợp dùng H2 riêng; migration SQL Server chưa được áp dụng và không thay thế việc kiểm tra trên database SQL Server trước triển khai.

Hai assertion cũ về chuỗi hiển thị được cập nhật theo tiếng Việt đang dùng trên UI (Ra về và Video đánh giá), giữ nguyên kiểm tra mã HTTP và nội dung chức năng. Bộ kiểm tra schema cũng xác nhận bản snapshot mới tương ứng JPA và migration V715 được nhúng vào script dựng mới.
