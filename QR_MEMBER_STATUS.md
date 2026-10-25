# Đồng bộ trạng thái QR Check-in của Member

Cập nhật nâng cấp sau các lượt sửa lỗi bên dưới: đã bổ sung QR Check-out riêng; xem `QR_CHECKOUT.md` cho logic hiện tại, inventory và migration **V707 cần chủ dự án tự áp dụng trước restart**. Các đoạn “chưa có QR Checkout/không migration” trong báo cáo lịch sử bên dưới chỉ mô tả thời điểm trước nâng cấp này.

## Phạm vi và kiến trúc

- Nhánh `feat/giai-doan-7`; giữ nguyên các thay đổi responsive/camera Admin đang có trong working tree. Không sửa SePay/Payment, không commit/push/merge.
- API mới: `GET /member/qr-checkin/status?tokenId=<id-QR-của-trang>`. Không cần tokenId khi trang chưa tạo mã. Không truyền token bí mật trong URL hoặc nhận userId/email để chọn người xem.
- Tái sử dụng RBAC `/member/**` hiện có và Principal đăng nhập. Service chỉ đọc dữ liệu Member ACTIVE/enabled; ID token không thuộc Member hoặc không tồn tại trả 404, tài khoản sai role/trạng thái bị chặn. JSON chỉ chứa trạng thái, thời gian và chi nhánh của chính Member; `Cache-Control: no-store`.
- Poll mỗi 4 giây, không chồng request, timeout 8 giây; dừng khi ẩn/rời trang và đồng bộ ngay khi quay lại (kể cả Safari back-forward cache). Mất kết nối thì ẩn QR và dừng countdown; luôn giữ nút tạo và nút Thử lại hiển thị. Nếu trạng thái gần nhất xác nhận chưa tập/đã checkout, vẫn cho yêu cầu tạo QR để backend kiểm tra lại. Nếu đang tập/chưa biết trạng thái, khóa tạo mã nhưng vẫn cho thử lại đồng bộ. Hết phiên đăng nhập yêu cầu tải lại/đăng nhập.
- Khi có phiên đang mở: hiển thị “Check-in thành công — Đang tập”, ẩn ảnh/xóa mã trên trang, dừng countdown, khóa tạo mới. Tiếp tục polling để thấy “Đã checkout”; chỉ mở lại nút tạo mã, không khôi phục QR đã dùng.
- QR hết hạn chưa check-in: ẩn ảnh, dừng countdown, hiện “QR hết hạn”, cho tạo mã mới nếu không có phiên đang mở. Countdown dựa thời gian server và thời gian đơn điệu của trình duyệt, không phụ thuộc đồng hồ điện thoại.
- Token cũ được thu hồi khi tạo token mới cũng có `usedAt` theo thiết kế hiện có; do đó API dùng `UNAVAILABLE` cho mã đã dùng/thu hồi, không coi riêng `usedAt` là bằng chứng check-in. Trạng thái đang tập/checkout lấy từ bảng checkins thật. Không migration hoặc sửa dữ liệu lịch sử.

## Chặn tạo QR khi chưa checkout

Ngoài khóa nút ở giao diện, `QrCheckinService.issue` kiểm tra phiên check-in đang mở **sau khi lấy khóa User hiện có**, trước khi thu hồi/tạo token. POST trực tiếp cũng không vượt qua được. Không đổi `consume`, định dạng token, TTL 120 giây, tính một lần, hoặc `AdminCheckinService.checkIn/checkOut` và quy tắc Membership/chi nhánh của chúng.

Endpoint trạng thái chỉ GET, transaction read-only; không tự check-in, checkout, dùng token, gia hạn hay tạo lại mã. Không thêm bảng/cột/index hoặc thay SecurityConfig.

## File trong lượt đồng bộ Member

| File | Thay đổi |
|---|---|
| `src/main/java/com/utefit/controller/member/MemberQrCheckinController.java` | API status, model trạng thái và ID QR vừa tạo |
| `src/main/java/com/utefit/service/MemberQrStatusService.java` | Service chỉ đọc, scope Principal/ownership, expiry và phiên hiện tại/gần nhất |
| `src/main/java/com/utefit/service/QrCheckinService.java` | Chặn issuance khi còn phiên mở; trả thêm ID token cho trang |
| `src/main/java/com/utefit/repository/CheckinRepository.java` | Truy vấn phiên mở/gần nhất của một Member |
| `src/main/java/com/utefit/repository/QrCheckinTokenRepository.java` | Tìm QR theo ID và owner, không khóa ghi |
| `src/main/resources/templates/member/qr-checkin.html` | Vùng trạng thái/chi tiết, QR và nút tạo có ID riêng |
| `src/main/resources/static/js/qr-member.js` | Polling, countdown, cleanup/khôi phục tab và xử lý lỗi |
| `src/test/java/com/utefit/service/MemberQrStatusServiceTest.java` | Unit test trạng thái, scope, expiry và guard issuance |
| `src/test/java/com/utefit/service/QrCheckinIntegrationTest.java` | Thêm lifecycle, API RBAC/ownership và render template trên H2 |
| `src/test/js/qr-member.test.cjs` | Unit test polling/render/countdown/failure/background |

## Tự kiểm thử trên laptop và iPhone

1. IntelliJ Stop → Run lại Spring Boot với cấu hình/database test hiện tại trên 8080; không chạy migration hoặc thay env. Giữ Tunnel HTTPS như cũ. Reload trang Member để tải JS/template mới.
2. Giữ Member `/member/qr-checkin` trên laptop, Admin trên iPhone (hoặc hai phiên đăng nhập riêng). Member tạo mã mới và để trang mở; Admin chọn đúng chi nhánh, quét và xác nhận một lần bằng dữ liệu test được phép.
3. Trong khoảng một chu kỳ 4 giây cộng độ trễ mạng: Member thấy “Check-in thành công — Đang tập”, QR/mã nhập biến mất, không còn countdown, nút tạo bị khóa. Nếu refresh trang vẫn phải thấy đang tập, không tạo mã mới được.
4. Admin checkout bản ghi đó: Member tự thấy “Đã checkout”, thông tin giờ ra, nút tạo mở lại; QR đã dùng vẫn ẩn. Tạo QR mới chỉ khi muốn bắt đầu lượt khác, mã cũ không dùng lại được.
5. Tạo QR nhưng không check-in, chờ 120 giây: “QR hết hạn”, ảnh bị ẩn, countdown dừng, có thể tạo mã mới. Giữ một tab QR cũ rồi tạo mã mới ở tab khác: tab cũ phải phát hiện mã bị thu hồi, không báo check-in thành công giả.
6. Thử check-in thủ công khi Member có trang QR đang mở: trang phải phản ánh phiên đang tập và cấm tạo QR mới. Backend vẫn chặn POST trực tiếp nếu đang có phiên mở.
7. Chuyển tab/khóa iPhone, Admin xử lý rồi quay lại Member: đồng bộ ngay trước khi cho dùng lại QR. Ngắt mạng tạm thời: QR tạm ẩn, không báo thành công giả; khi nối mạng hiển thị trạng thái thật.
8. Dùng tài khoản Member khác thay tokenId trên API: 404, không lộ thông tin. Guest/PT/Admin/Manager không được đọc API Member. Không thay hash/token hoặc dùng SQL để giả lập trạng thái.

Camera Safari thành công ở lượt trước là kết quả người dùng cung cấp. Đồng bộ Member mới chưa được kiểm thử trên thiết bị thật trong lượt sửa này; không chạy Playwright hoặc dùng file QA env chứa secret.

## Kết quả kiểm tra tự động

- Maven compile/test: **BUILD SUCCESS**, 24 test PASS (8 unit `MemberQrStatusServiceTest`, 16 integration `QrCheckinIntegrationTest`, trong đó 12 ca QR cũ và 4 ca bổ sung). Kiểm tra cả render template, status API, scope/role, consume–checkout–tạo lại, expiry và race scan cũ.
- Lệnh Maven chỉ chọn hai test trên, ép datasource `jdbc:h2:mem:qr_status_tests;MODE=MSSQLServer;DB_CLOSE_DELAY=-1`, driver H2 và không profile runtime. Không kết nối/chạy migration trên SQL Server. Log local: `target/qr-status-tests.log`.
- Wrapper `mvnw.cmd` gặp lỗi khởi chạy môi trường; dùng Maven 3.9.16 có sẵn trong cache. Sandbox ban đầu chặn đọc JAR; sau khi được cấp quyền, build/test hoàn tất, không cài công cụ hoặc tải dependency mới.
- `node --check src/main/resources/static/js/qr-member.js`: PASS.
- `node --test src/test/js/qr-member.test.cjs src/test/js/qr-scanner.test.cjs`: **21/21 PASS** (9 ca đồng bộ mới và 12 ca scanner regression).
- `git diff --check`: PASS. Không có thay đổi `AdminCheckinService`, SecurityConfig, entity, migration hay SePay/Payment trong lượt đồng bộ.

Các test này không chứng minh SQL Server concurrency hoặc trải nghiệm Safari/laptop thực tế đã PASS; cần thực hiện checklist thủ công ở trên sau restart.

## Sửa lỗi khu vực chức năng trống sau Checkout — 26/10/2026

### Chẩn đoán

- Đã SELECT chỉ đọc trên đúng `UTEFitQA_GD7_20261025`, không dùng database chính: hai tài khoản Member QA được kiểm tra đều ACTIVE/enabled, `open_count=0`, lượt gần nhất có `check_out_at`. Không cập nhật/xóa dữ liệu hoặc token.
- HTTP bằng tài khoản QA trên server 8080 đang chạy: đăng nhập thành công; đọc HTML trang QR bị ngắt luồng trước khi có `member-checkin-status`, `member-qr-form` và `member-qr-create`. `GET /member/qr-checkin/status` trả **404**, không có JSON trạng thái thành công. Vì vậy không thể coi runtime là bản backend đồng bộ mới đã được khởi động đầy đủ.
- Template cũ đọc trực tiếp `${checkinStatus.attendanceState}`/`${!checkinStatus.canIssue}` mà không bảo vệ model null. Nếu controller đang chạy chưa cung cấp model này, Thymeleaf lỗi giữa lúc render, trả HTML dở dang. Kết quả đúng với hiện tượng có mô tả nhưng không có form/nút. JavaScript chỉ ẩn vùng ảnh QR, không có lệnh ẩn cả panel/form.
- Test độc lập tái hiện biểu thức cũ với model thiếu; integration test kiểm tra template mới thiếu model vẫn trả đủ form, nút và `</html>`. Chưa lấy stacktrace trực tiếp từ console IntelliJ của process đang chạy; phân biệt bằng chứng HTTP/source/test với suy luận lệch binary/template. Cần Stop → Run đầy đủ để đồng bộ bản backend/template/JS.

### Khắc phục

- Template guard null tại mọi chỗ đọc trạng thái, dùng nhánh UNKNOWN có thông báo. Form tạo QR và nút Thử lại không phụ thuộc QR tồn tại, usedAt hoặc checkout; thiếu dữ liệu chỉ tạm khóa hành động cần trạng thái, không loại toàn bộ DOM.
- Controller giữ trang có fallback khi truy vấn trạng thái gặp lỗi database tạm thời; API trả 503 để polling nhận biết lỗi. AccessDenied không bị biến thành trạng thái thành công. Tạo QR gặp lỗi truy cập dữ liệu cũng trả trang với thông báo rõ ràng.
- Nút Thử lại chủ động đồng bộ; không ẩn form vì lỗi mạng/API. Giữ trạng thái sẵn sàng gần nhất từ server/API khi cho tạo QR; backend issuance vẫn kiểm tra phiên mở. 404 của token cho phép thử lại đọc trạng thái chính mình không kèm token cũ; 404 của endpoint hiển thị hướng dẫn restart, không giả dữ liệu JSON thành công.
- Không sửa `MemberQrStatusService`, `QrCheckinService`, `AdminCheckinService` hay repository/entity/schema trong lượt sửa lỗi này. Đã kiểm tra source, chưa có QR Checkout; tiếp tục dùng checkout Admin, không tự bổ sung nghiệp vụ QR Checkout.

### File chỉnh trong lượt sửa lỗi

- `src/main/resources/templates/member/qr-checkin.html`
- `src/main/resources/static/js/qr-member.js`
- `src/main/java/com/utefit/controller/member/MemberQrCheckinController.java`
- `src/test/java/com/utefit/service/QrCheckinIntegrationTest.java`
- `src/test/java/com/utefit/service/MemberQrPageFailureTest.java` (mới)
- `src/test/js/qr-member.test.cjs`
- `QR_MEMBER_STATUS.md`

Script chẩn đoán nằm trong `target/qr-readonly-diagnosis.ps1` bị Git ignore; chỉ parse literal env vào bộ nhớ, không in credentials/hash/token và không thực thi file env. Không dùng Playwright hoặc tự restart server người dùng.

### Cách kiểm tra lại

1. Stop Spring Boot trong IntelliJ, đợi process cũ dừng hẳn; Run cấu hình hiện tại trên 8080. Không chỉ refresh hoặc hot-swap template. Giữ nguyên profile/env/database QA, không chạy migration.
2. Đăng nhập Member QA đã checkout → `/member/qr-checkin`: thấy “Đã checkout”, nút tạo QR và nút Cập nhật trạng thái. DevTools Network phải thấy GET status 200 JSON với `attendanceState=CHECKED_OUT`, `canIssue=true`, `qrState=NONE` khi chưa tạo mã trên trang.
3. Tạo QR mới → xác nhận check-in Admin bằng dữ liệu test được phép: Member hiện Đang tập và khóa tạo mới. Checkout Admin → Member trở lại sẵn sàng trong chu kỳ 4 giây; reload/đăng nhập lại vẫn có đủ form/nút.
4. Mô phỏng offline/chặn API ở DevTools: có thông báo và nút Thử lại; không trống khu vực chức năng. Nếu trạng thái gần nhất là OPEN, tạo mã vẫn bị chặn. Khôi phục mạng → Thử lại, nhận trạng thái thật. Thiếu model/DB tạm lỗi không làm hỏng phần còn lại của HTML.

Kết quả lượt sửa lỗi: Maven **BUILD SUCCESS**, 31 ca Java riêng biệt PASS qua hai lượt chạy chọn lọc (18 integration QR trên H2, 8 unit trạng thái, 5 unit lỗi trang/tái hiện null model). Lượt đầu 30 ca, sau bổ sung test tái hiện chạy lại riêng 5 ca unit; không cộng trùng thành 35. Log local `target/qr-render-fix-tests.log` và `target/qr-null-model-repro-tests.log`; log exception Thymeleaf trong test tái hiện là lỗi được assert có chủ đích, không phải test FAIL. JavaScript **25/25 PASS** (13 đồng bộ Member, 12 scanner regression), syntax và `git diff --check` PASS. Chưa xác nhận UI mới trên runtime 8080 hoặc thiết bị thật sau restart, không có SQL Server mutation/migration.
