# Hủy gói chưa thanh toán — Member và Admin

## Nguyên nhân và hành vi mới

Trước đây `MembershipPaymentService.cancelPendingMembership` từ chối mọi Payment
PENDING thuộc SePay/VNPay. Không có hàng đợi Admin xử lý, nên dù chưa có xác nhận
thu tiền, hội viên vẫn mắc kẹt. Không thể khắc phục bằng cách đánh dấu FAILED đơn
thuần vì chuyển khoản có thể đến muộn.

| Tình trạng | Kết quả |
|---|---|
| Membership PENDING, chưa có Payment | Member hủy ngay; Membership CANCELLED |
| Chỉ có CASH PENDING chưa được xác nhận thu | Hủy ngay, Payment CANCELLED; lưu thời gian/người hủy/lý do |
| SePay/VNPay PENDING | Payment CANCEL_REQUESTED; giữ Membership PENDING để Admin xử lý, ngừng QR/xác nhận tự động |
| Admin duyệt yêu cầu | Payment CANCELLED + Membership CANCELLED trong cùng transaction |
| PAID hoặc Membership đã kích hoạt | Không được hủy bằng luồng chưa thanh toán; dùng quy trình hỗ trợ/hoàn tiền hiện có |
| Callback hợp lệ đến sau yêu cầu hủy/hủy | Không kích hoạt; lưu bằng chứng để đối soát, không giả PAID/REFUNDED |

Đây là **đóng đơn tại UTEFit**, không phải yêu cầu ngân hàng dừng chuyển khoản hay
chứng nhận chưa nhận tiền. Hội viên không được chuyển vào mã cũ nữa. Admin vẫn phải
đối chiếu khoản tiền thực nhận, kể cả khi đã duyệt hủy đơn.

## Khóa, quyền và lịch sử

- Member chỉ hủy đơn của mình, phải MEMBER/ACTIVE/enabled. Chưa có Payment thì
  chủ thao tác là chủ Membership; status và updatedAt của bản ghi cũ được giữ làm
  lịch sử, không xóa Membership.
- Yêu cầu hủy online ghi `cancellation_requested_at` trên các Payment đang chờ;
  người yêu cầu chính là chủ Payment đã được backend kiểm tra. Gửi lặp không tạo
  yêu cầu mới hoặc đổi thời điểm ban đầu.
- Admin qua endpoint `/admin/payments/{id}/approve-cancellation` được bảo vệ bởi
  RBAC/CSRF. Service đọc lại ADMIN/ACTIVE/enabled từ DB trước khi ghi. Bắt buộc lý
  do 10–500 ký tự và xác nhận đã kiểm tra lịch sử. Không có thao tác gán PAID ở đây.
- Audit kết thúc: `cancelled_at`, `cancelled_by`, `cancellation_reason`; không ghi
  đè audit khi duyệt lại. Không xóa Payment hay thay amount/reference/snapshot.
- Cùng thứ tự khóa với các luồng xác nhận tiền: **User → Membership → Payment**,
  refresh trạng thái sau khóa, nhiều Payment khóa theo ID tăng dần. Webhook thắng
  trước thì hủy bị từ chối; yêu cầu hủy thắng trước thì webhook chỉ ghi REVIEW.
- Không tạo CASH/SePay mới trên chính Membership đang yêu cầu hủy. Sau khi hủy
  hoàn tất, lượt mua khác là Membership/Payment mới, không dùng lại mã cũ.

## Callback đến muộn và hoàn tiền

- SePay giữ nguyên raw-body HMAC, Live guard, main account/VA, reference, amount,
  thời gian, UNIQUE transaction/bank reference và chống callback trùng. Receipt
  tiền vào vẫn được lưu bền vững. Sau hủy, receipt là REVIEW với lý do
  `CANCELLED_ORDER_REQUIRES_REFUND_REVIEW`; trong lúc yêu cầu hủy là
  `CANCELLATION_REQUESTED_REVIEW` (các bất thường khác vẫn có lý do riêng).
- VNPay không mở đơn mới. IPN cũ phải qua signature và amount như trước. Callback
  thành công đến muộn được ghi `late_gateway_evidence` trên Payment trong cùng
  transaction (mã ngân hàng, thời gian, số tiền); trạng thái không chuyển PAID.
  Callback giống nhau không nhân đôi audit. Nếu audit đầy thì lỗi/retry, không trả
  success rồi âm thầm bỏ mất bằng chứng. Admin thấy cảnh báo và nội dung audit.
- Polling đọc trạng thái mới, không tự xác nhận tiền; khi CANCEL_REQUESTED/CANCELLED
  không còn QR chuyển khoản. Khi có receipt REVIEW, hiển thị cần đối soát.
- Với SePay, trang xử lý riêng/hoàn tiền hiện có cho phép lưu bằng chứng nhận tiền
  của đơn đã hủy, **không cho kích hoạt lại**. Chỉ sau khi kiểm tra ngân hàng và
  hoàn đủ tiền thực tế mới ghi REFUNDED bằng quy trình bằng chứng/audit cũ. Không
  tạo lệnh chuyển tiền hoặc webhook giả. Thiếu/thừa tiền, sai VA/mã hoặc nhiều
  khoản xung đột vẫn phải điều tra riêng, không ép trạng thái hoặc gán đơn khác.
- Khoản VNPay đến muộn cần Admin làm việc với cổng/ngân hàng và lưu hồ sơ xử lý;
  không có chức năng tự hoàn VNPay. Không đánh dấu đã hoàn nếu chưa hoàn thực tế.

## Database — DBA tự áp dụng sau backup

Migration mới: `database/migrations/V714__pending_payment_cancellation.sql`.
Không sửa migration cũ. Mở SSMS tại đúng database dự kiến, backup và dừng ứng dụng
trước khi áp dụng. Script thêm 5 cột nullable trên `payments`, mở rộng CHECK status
cho CANCEL_REQUESTED/CANCELLED và thêm index danh sách chờ. Không tạo bảng mới,
không UPDATE dữ liệu Payment/Membership, không cấp approval hay bật Live.

`DB_Khung_1.sql` đã nhúng nguyên V714; `DB_Data_2.sql` yêu cầu schema mới nhưng
không tạo cancellation audit giả và không sửa dữ liệu seed đã tồn tại. Database
hiện có **chỉ áp dụng migration**, không reset hoặc chạy lại seed để sửa đơn #22.
Runtime vẫn chỉ DML nghiệp vụ, không cần ALTER hay quyền ghi deployment guard.
Không thêm biến môi trường. Migration và tất cả thao tác nghiệp vụ trên SQL Server
chưa được tự thực thi trong lượt sửa này; Payment #22 và các giao dịch Live giữ nguyên.

Nếu migration thất bại trong transaction: rollback và DBA kiểm tra lỗi trước khi
thử lại. Sau áp dụng, không rollback source về enum cũ khi đã có trạng thái mới;
dừng tạo thao tác mới, giữ audit và có kế hoạch chuyển đổi tương thích, không xóa
giao dịch hoặc dùng reset database để phục hồi.

## Cách test trên giao diện

1. Áp dụng V714 theo quy trình DBA rồi khởi động lại Spring Boot, giữ nguyên cấu
   hình hiện tại; không bật nhận Payment Live mới chỉ để thử hủy.
2. Member mở `/member/memberships`, nhấn **Hủy gói chờ** cho đơn PENDING. Nếu chưa
   có Payment/CASH chưa xác nhận sẽ hủy ngay. SePay/VNPay sẽ báo đã yêu cầu hủy và
   ẩn thao tác thanh toán. `/member/payments` hiển thị trạng thái tiếng Việt.
3. Admin mở `/admin/payments`, nhấn **Yêu cầu hủy của hội viên**, hoặc lọc
   `status=CANCEL_REQUESTED`. Xem Membership, phương thức, mã, tiền, cảnh báo đối soát.
4. Vào **Xem**, kiểm tra lịch sử ngân hàng/đối soát, nhập lý do, xác nhận đã kiểm tra,
   bấm **Duyệt hủy gói chờ**. Không xác nhận đã nhận tiền nếu chưa có bằng chứng.
5. Member tải lại trang: gói đã hủy, có thể mua gói rẻ hơn. Đơn cũ vẫn còn trong
   lịch sử. Đơn PAID không có luồng hủy chưa thanh toán.
6. Nếu tiền thật đến muộn: không chuyển thêm tiền; mở **Đối soát ngân hàng SePay**
   từ chi tiết Payment → **Xử lý riêng/hoàn tiền**, đối chiếu bằng chứng và khoản
   hoàn thực tế. Không gửi payload thử tới webhook Live.

## File thay đổi trong tác vụ

- Entity/enum: `Payment.java`, `PaymentStatus.java`.
- Service: `MembershipPaymentService`, `AdminPaymentService`, `SepayPaymentService`,
  `SepayWebhookService`, `VnpayPaymentService`, `SepayManualReconciliationService`.
- Controller: `MemberMembershipController`, `AdminPaymentController`.
- Repository: `SepayPaymentDetailsRepository`; util: `MembershipDisplay`.
- Templates: member/memberships, member/payments, member/sepay-checkout;
  admin/payments/list, detail, sepay-manual-refund.
- JS: sepay-checkout.js; tests: sepay-checkout.test.cjs,
  SepayPaymentIntegrationTest, SepayManualReconciliationIntegrationTest,
  VnpayPaymentServiceTest, MembershipUpgradeIntegrationTest.
- SQL: V714, DB_Khung_1.sql, DB_Data_2.sql; tài liệu này.
- Đồng bộ snapshot JPA của `database/live-demo/01_schema.sql` để test schema không
  lệch entity hiện tại; đây chỉ là file nguồn, không chạy tạo/sửa LiveDemo.

Không commit/push, không chạy Playwright, không tạo giao dịch ngân hàng hoặc thay
đổi dữ liệu hiện tại. Các thay đổi trước tác vụ được giữ nguyên.

## Kết quả kiểm thử thực tế

- Maven compile + toàn bộ **277 Java tests nội bộ PASS**, 0 fail/error; chạy H2
  riêng. Loại duy nhất `CloudinaryIntegrationTest` vì test đó upload lên dịch vụ
  thật; không coi test ngoài mạng này là PASS. Các test Cloudinary mock/validator
  vẫn chạy trong bộ 277.
- **44 JavaScript tests PASS**; gồm chuyển UI sang yêu cầu hủy/đã hủy, không còn
  QR/countdown và polling chỉ đọc chờ trạng thái Admin/đối soát đến muộn.
- Các ca mới: chưa có Payment; CASH chưa thu; gửi yêu cầu SePay lặp; Admin/Member,
  Admin bị vô hiệu hóa và CSRF; từ chối PAID; nhận webhook HMAC sau hủy và callback
  trùng; webhook cạnh tranh với yêu cầu hủy; mua lại gói rẻ hơn; lỗi sau flush làm
  rollback cả audit/Payment/Membership; VNPay ký hợp lệ đến muộn không ghi PAID;
  ghi bằng chứng và hoàn tiền cho đơn đã hủy mà không kích hoạt lại.
- Regression Membership theo lịch, CASH, SePay Live safety/HMAC/VA/manual,
  QR Check-in/out, Manager, Favorites, Review chạy trong bộ Java trên H2.
- ScriptDom T-SQL parser: DB_Khung_1.sql, DB_Data_2.sql và V714 không có lỗi cú
  pháp. T-SQL snapshot LiveDemo cũng không lỗi sau loại chỉ thị SQLCMD trước parse.
  Đây không phải bằng chứng migration đã chạy thành công trên SQL Server.
- Chưa kiểm thử UI trên trình duyệt thật, chạy migration SQL Server, thực hiện
  hủy #22 hoặc gửi webhook/thanh toán Live. Chủ dự án tự áp dụng và demo theo mục trên.
