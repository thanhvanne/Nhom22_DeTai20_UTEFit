# UTEFit — Báo cáo validation, 31/10/2026

## 1. Phạm vi và an toàn

- Kiểm kê toàn bộ template, controller, DTO, JavaScript, service ghi dữ liệu và độ dài cột entity liên quan.
- Snapshot trước tác vụ: 120 khai báo form/nhóm control, gồm 115 form và 5 control tách rời; 225 khai báo control (không phải 225 trường duy nhất, có fragment dùng lại và trường ẩn).
- Làm việc trên `main`, không đổi nhánh, không commit/push. Giữ nguyên các thay đổi đã tồn tại của nhóm.
- Không sửa entity/schema Unicode, không chạy SQL/migration, không đọc/ghi dữ liệu SQL Server thực tế. Không chạy script demo được đính kèm.
- Không đổi thuật toán Loyalty/ưu đãi, giá snapshot, trạng thái Payment/Membership trên dữ liệu thật hoặc SePay HMAC/webhook/approval.
- Test tạo dữ liệu chỉ trên H2 in-memory hoặc mock. Browser server riêng cổng 18081, không dùng ứng dụng 8080. Mail được mock; không upload Cloudinary, không phát sinh thanh toán thật.

## 2. Rà soát theo role

| Role | Form/chức năng và trường đầu vào |
|---|---|
| Guest | Đăng ký: họ, tên, email, điện thoại, mật khẩu/xác nhận; đăng nhập; quên/đặt lại mật khẩu; OTP/gửi lại; tìm kiếm/lọc chi nhánh, gói, lớp, PT, đánh giá; phân trang. |
| Member | Hồ sơ, avatar, đổi mật khẩu; chọn chi nhánh/gói, mã ưu đãi, lựa chọn ưu đãi và báo giá; mua/gia hạn/hủy/chọn phương thức qua các ID; đặt/hủy lớp, lịch tuần; gửi/hủy yêu cầu PT, lời nhắn; đánh giá: chi nhánh, PT, sao, nhận xét, ảnh/video; chat; thông báo, yêu thích, QR. |
| PT | Chuyên môn, số năm kinh nghiệm, tiểu sử, avatar; tạo lịch 1:1: hội viên, ngày, giờ, tiêu đề, ghi chú; lọc lịch; tiếp nhận/từ chối yêu cầu; trạng thái điểm danh; chat, lịch sử, thông báo; các ID và trạng thái hành động. |
| Manager | Chọn/lọc chi nhánh thuộc quyền; check-in bằng email/QR, check-out; lịch lớp: lớp, ngày, giờ, phòng, trạng thái và ID. Không tự thêm biểu mẫu hoặc nghiệp vụ nhập hội viên mới. |
| Admin | Lọc/khóa tài khoản, gán chi nhánh PT; tạo Manager, cấp/thu hồi chi nhánh; chi nhánh, loại lớp, lớp, lịch; gói tập và thời hạn/quyền lợi/phạm vi; mã giảm giá; 5 chương trình tự động; các filter thanh toán, đánh giá, check-in; bằng chứng/định dạng/giới hạn đầu vào đối soát. Các kiểm tra quyền và quy trình duyệt cũ giữ nguyên. |

## 3. Lỗi thực tế và thay đổi

- Regex điện thoại cũ cho phép 9–12 chữ số tùy ý; thay bằng tiền tố di động Việt Nam + đúng 10 chữ số, hỗ trợ chuẩn hóa `+84` thành `0`.
- Kiểm tra email cũ chưa buộc domain có dấu chấm/TLD; địa chỉ như `123123@123` không còn qua được frontend hoặc backend. Cho phép dấu `+` và tài khoản `@utefit.test`.
- Một số form/raw `@RequestParam` chưa có hợp đồng giới hạn dùng chung; DTO và service chưa đồng bộ. Bổ sung Bean Validation, kiểm tra raw request trước controller và guard tại service ghi dữ liệu.
- Họ tên trước đây chủ yếu chỉ có NotBlank/Size; bổ sung Unicode chữ/dấu, dấu cách, dấu nối, dấu nháy; chuẩn hóa NFC/khoảng trắng mà không bỏ dấu.
- Chưa thống nhất giới hạn mật khẩu theo byte BCrypt, ID/số vượt kiểu, số nguyên, giới hạn văn bản, ngày/giờ và lỗi dưới field. Bổ sung hợp đồng chung `validation-rules.json` dùng ở Java và JavaScript.
- Đồng bộ kinh nghiệm PT 0–70 năm với service hiện có; không giữ giới hạn DTO 80 mâu thuẫn service.
- Upload chi nhánh từng quảng bá WEBP dù ImageValidator chỉ nhận JPG/PNG/GIF; sửa accept/hướng dẫn đúng các định dạng backend thực sự hỗ trợ.
- Thay popup kiểm tra video bằng lỗi inline/focus. Giữ nguyên giới hạn và kiểm tra nội dung file backend đã có.
- Live validation không trim ngay khi đang gõ: tránh mất dấu cách khi sửa họ tên nhiều từ; chỉ chuẩn hóa giá trị input ở blur/submit.

## 4. Quy tắc hiện hành

| Dữ liệu | Quy tắc |
|---|---|
| Email | Trim, lowercase; tối đa 100 ký tự theo cột, local tối đa 64; domain có ít nhất một dấu chấm, nhãn domain hợp lệ và TLD chữ 2–63; không có khoảng trắng nội bộ; cho phép local có `+`; service đăng ký vẫn kiểm tra trùng. Không kiểm tra DNS trong form. |
| Di động | 10 chữ số với các đầu số 032–039, 052/055/056/058/059, 070/076–079, 081–089, 090–094/096–099; `+84` chuyển về `0`; không nhận thiếu chữ số, chữ hay toàn 0. Không suy đoán nhà mạng từ đầu số sau chuyển mạng. |
| Hotline chi nhánh | Quy tắc riêng: số cố định/di động 10–11 chữ số bắt đầu 0, hotline 1800/1900 hoặc +84; bỏ dấu phân tách thông thường. Không ép hotline thành regex di động cá nhân. |
| Họ/tên | Mỗi trường tối đa 50 ký tự; chữ Unicode, dấu tiếng Việt, dấu cách/nối/nháy; không chỉ số/ký tự vô nghĩa; không sửa mapping NVARCHAR. |
| Mật khẩu | Đăng ký/đổi/reset ít nhất 8 ký tự và tối đa 72 byte UTF-8; Manager mới ít nhất 12 như chính sách cũ; xác nhận phải khớp. Đăng nhập không áp min mới lên tài khoản có sẵn. Không trim/lowercase mật khẩu. |
| Số/ID | ID dương trong Long; sức chứa/giới hạn trong Integer; số nguyên không nhận thập phân, NaN, Infinity, exponent hoặc overflow; kinh nghiệm 0–70; phân trang có giới hạn. |
| Tiền/ưu đãi | Giá VND nguyên dương tối đa 13 chữ số; phần trăm >0 và <100, tối đa 2 số lẻ; lượng mã >=0; giới hạn số tháng/ngày theo chính sách hiện có. Backend vẫn tự tính giá/quyền, không tin giá frontend. |
| Thời gian | Parse ngày/giờ, năm SQL 1–9999; giờ kết thúc sau bắt đầu; ngày theo quy tắc riêng mã giảm giá/chương trình; lịch tập phải trong tương lai. Xung đột, giờ chi nhánh, quyền đặt và lịch cũ tiếp tục do service hiện có kiểm tra. |
| Văn bản | Tên chi nhánh/lớp 150; tên loại lớp/gói 100; địa chỉ/chuyên môn/tiêu đề 255; phòng 100; mô tả 1000 hoặc 1500 theo loại; bio, chat, lời nhắn, nhận xét, ghi chú 2000. Không áp regex tên người cho mô tả tự do. |
| Upload | JPG/PNG/GIF <=5 MiB; MP4/WebM <=50 MiB; frontend kiểm tra sớm, backend tái sử dụng ImageValidator/VideoValidator kiểm tra loại và nội dung. |
| Select/checkbox | Required, ID dương, enum hợp lệ, boolean hợp lệ; quan hệ/chi nhánh đang hoạt động, quyền sở hữu, scope vẫn được kiểm tra tại service. |

Các ràng buộc cụ thể, thông báo tiếng Việt và scope theo route nằm trong `src/main/resources/static/js/validation-rules.json`. Các điều kiện nghiệp vụ chéo đã có không bị thay bằng regex.

## 5. Hiển thị và backend

- Asset validation được nạp qua head/layout chung của Guest, Member, PT, Manager và Admin; không thiết kế lại trang.
- Lỗi đỏ ngay dưới control/wrapper; viền đỏ, cập nhật `aria-invalid`, nối `aria-describedby`, `aria-live`, focus lỗi đầu khi submit. Error trên tab hồ sơ được mở đúng tab.
- Kiểm tra blur/submit; sau khi field đã tương tác, cập nhật khi nhập để sửa lỗi. Không xóa field hợp lệ.
- HTTP JSON/AJAX trả 400 với `message`, `error`, `fieldErrors` cho lỗi request; báo giá/chat/profile AJAX đọc map field thay vì chỉ alert chung.
- MVC dùng `@Valid`/BindingResult và renderer lỗi chung; raw-request không hợp lệ có thể redirect về form với flash errors/giá trị an toàn. Editor 5 chương trình tự động tiếp tục render BindingResult tại chỗ như trước.
- Service ghi DTO gọi FormChecks; đăng ký/reset/profile/Manager/PT/chat và các đường ghi quản trị đều có kiểm tra backend tương ứng. Không đụng CSRF/security config/quyền sở hữu.
- Không lưu mật khẩu/token/OTP trong flash hoặc báo cáo. File input/mật khẩu không tự điền lại sau redirect vì bảo mật trình duyệt; người dùng phải nhập/chọn lại. Giá trị flash được giới hạn 4000 ký tự/trường để tránh payload quá lớn.

## 6. Kiểm thử thực sự đã chạy

| Bộ kiểm thử | Kết quả |
|---|---|
| Maven compile + toàn bộ JUnit | BUILD SUCCESS; 416 test được báo cáo, 415 PASS, 0 FAIL/ERROR, 1 SKIP. |
| Test skip | `CloudinaryIntegrationTest.uploadImageSuccessfully`: test tích hợp Cloudinary bị tắt theo cấu hình; không tuyên bố PASS. |
| Unit Java validation mới | 60 trường hợp InputRulesTest + 15 FormValidationMvcTest, đều PASS trong suite trên. |
| Toàn bộ Node test | 155 PASS, 0 FAIL; gồm 41 test contract validation mới và hồi quy upload/JS hiện có. |
| Playwright/Chrome thực tế | 10 PASS, 0 FAIL trên H2 cổng 18081; Guest, Member, PT, Manager, Admin, mobile 390px; không có pageerror JavaScript. |
| Whitespace diff | `git diff --check` exit 0; Git có cảnh báo CRLF của working tree Windows, không có lỗi whitespace. |

Playwright kiểm tra vị trí lỗi dưới control, màu đỏ/viền đỏ bằng computed style, ARIA, focus, giữ dữ liệu, sửa đúng rồi đăng ký qua backend H2, native `form.submit()` bỏ qua JS bị backend từ chối, họ tên có dấu cách, kinh nghiệm âm/thập phân, hotline/giờ chi nhánh và required. Kết quả/ảnh: `target/validation-browser/results.json`, `target/validation-browser/console-errors.json`, `target/validation-browser/*-invalid.png`. JUnit XML: `target/surefire-reports`.

Chạy lại unit: `mvn test`; JavaScript: `node --test src/test/js/*.test.cjs`.

Chạy browser an toàn ở terminal riêng: `mvn -Dspring-boot.run.main-class=com.utefit.validation.ValidationBrowserApplication spring-boot:test-run`, rồi `node src/test/playwright/validation.cjs` ở terminal khác. Cần module Playwright + Chrome; có thể đặt `PLAYWRIGHT_MODULE` tới module cục bộ. Browser script cố định cổng 18081, fixture chỉ chạy với H2 in-memory và mail mock. Không dùng fixture này để khởi động production.

## 7. Hạn chế và kiểm tra nhanh

- Đây là rà soát source toàn bộ và test tự động có phạm vi xác định; Playwright không chạy mọi tổ hợp dữ liệu/role trên cả 115 form. Không cam kết toán học rằng mọi dữ liệu sai đều được phát hiện.
- Không truy vấn dữ liệu SQL Server hiện hữu nên không kết luận số bản ghi demo sai. Không sửa/xóa hàng loạt. Nếu cần xử lý dữ liệu cũ, lập danh sách read-only và xin duyệt riêng.
- Regex chỉ kiểm tra cấu trúc, không xác minh số điện thoại đang hoạt động/chủ sở hữu hoặc email có nhận thư; quy trình OTP hiện có giữ nguyên.
- JS bị tắt/không tải được: backend vẫn chặn dữ liệu sai; hiển thị inline phía client cần asset JavaScript hoạt động. Upload Cloudinary/OTP mail thật không được kiểm thử trực tiếp.
- Khởi động lại ứng dụng IntelliJ/cổng 8080 để nạp Java mới, sau đó Ctrl+F5. Tôi không dừng hay thay cấu hình ứng dụng 8080 đang chạy.
- Thử đăng ký bằng `123123@123`, `123456789`, mật khẩu `123`; blur/submit phải thấy lỗi dưới đúng ô, không lưu. Sửa sang email demo có dấu +, số hợp lệ/+84, họ tên có dấu; kiểm tra lỗi hết.
- Thử Member đổi hồ sơ/mật khẩu xác nhận; PT kinh nghiệm -1/1.5; Manager email check-in sai; Admin để trống tên/địa chỉ, giờ đóng trước mở, giá âm/số lẻ và phần trăm 100. Không thực hiện thanh toán hoặc thay trạng thái dữ liệu demo chỉ để kiểm thử.

## 8. Danh sách file thay đổi trong tác vụ

Danh sách dưới được so với snapshot trước tác vụ, không gộp các thay đổi có sẵn của nhóm trong `git status`.

### Sửa file có sẵn
- `src/main/java/com/utefit/controller/admin/AdminAutomaticPromotionController.java`
- `src/main/java/com/utefit/controller/member/MemberMembershipController.java`
- `src/main/java/com/utefit/dto/admin/AutomaticPromotionForm.java`
- `src/main/java/com/utefit/dto/admin/BranchFormDto.java`
- `src/main/java/com/utefit/dto/admin/ClassScheduleFormDto.java`
- `src/main/java/com/utefit/dto/admin/FitnessClassFormDto.java`
- `src/main/java/com/utefit/dto/admin/MembershipPlanFormDto.java`
- `src/main/java/com/utefit/dto/auth/ForgotPasswordRequest.java`
- `src/main/java/com/utefit/dto/auth/LoginRequest.java`
- `src/main/java/com/utefit/dto/auth/RegisterRequest.java`
- `src/main/java/com/utefit/dto/auth/ResetPasswordRequest.java`
- `src/main/java/com/utefit/dto/auth/VerifyOtpRequest.java`
- `src/main/java/com/utefit/dto/chat/ChatMessageSendRequest.java`
- `src/main/java/com/utefit/dto/member/MembershipPurchaseRequest.java`
- `src/main/java/com/utefit/dto/member/PasswordChangeRequest.java`
- `src/main/java/com/utefit/dto/member/ProfileUpdateRequest.java`
- `src/main/java/com/utefit/dto/member/ReviewForm.java`
- `src/main/java/com/utefit/dto/trainer/TrainerProfileUpdateRequest.java`
- `src/main/java/com/utefit/dto/trainer/TrainerSearchRequest.java`
- `src/main/java/com/utefit/service/AdminPromotionService.java`
- `src/main/java/com/utefit/service/AutomaticPromotionService.java`
- `src/main/java/com/utefit/service/impl/AdminBranchServiceImpl.java`
- `src/main/java/com/utefit/service/impl/AdminCategoryServiceImpl.java`
- `src/main/java/com/utefit/service/impl/AdminClassServiceImpl.java`
- `src/main/java/com/utefit/service/impl/AdminMembershipPlanServiceImpl.java`
- `src/main/java/com/utefit/service/impl/AdminScheduleServiceImpl.java`
- `src/main/java/com/utefit/service/impl/AuthServiceImpl.java`
- `src/main/java/com/utefit/service/impl/ChatServiceImpl.java`
- `src/main/java/com/utefit/service/impl/PersonalTrainingScheduleServiceImpl.java`
- `src/main/java/com/utefit/service/impl/TrainerRequestServiceImpl.java`
- `src/main/java/com/utefit/service/ManagerAccessService.java`
- `src/main/java/com/utefit/service/ManagerOperationsService.java`
- `src/main/java/com/utefit/service/MemberProfileService.java`
- `src/main/java/com/utefit/service/MemberReviewService.java`
- `src/main/java/com/utefit/service/MembershipPaymentService.java`
- `src/main/java/com/utefit/service/PtProfileService.java`
- `src/main/java/com/utefit/websocket/ChatWebSocketController.java`
- `src/main/resources/templates/admin/branches/form.html`
- `src/main/resources/templates/admin/fragments/layout.html`
- `src/main/resources/templates/fragments/head.html`
- `src/main/resources/templates/guest/trainer-detail.html`
- `src/main/resources/templates/guest/trainers.html`
- `src/main/resources/templates/member/fragments/layout.html`
- `src/main/resources/static/js/admin-promotion-programs.js`
- `src/main/resources/static/js/member-chat.js`
- `src/main/resources/static/js/member-offers.js`
- `src/main/resources/static/js/reviews.js`
- `src/main/resources/static/js/utefit-member-profile.js`
- `src/main/resources/static/js/utefit-staff.js`
- `src/test/js/reviews-upload.test.cjs`

### File mới
- `src/main/java/com/utefit/config/FormValidationConfig.java`
- `src/main/java/com/utefit/validation/ConsistentForm.java`
- `src/main/java/com/utefit/validation/ConsistentFormValidator.java`
- `src/main/java/com/utefit/validation/FormChecks.java`
- `src/main/java/com/utefit/validation/FormValidationAdvice.java`
- `src/main/java/com/utefit/validation/FormValidationInterceptor.java`
- `src/main/java/com/utefit/validation/InputRules.java`
- `src/main/java/com/utefit/validation/InputViolationException.java`
- `src/main/java/com/utefit/validation/RequestChecks.java`
- `src/main/java/com/utefit/validation/ValidationMessages.java`
- `src/main/java/com/utefit/validation/ValidInput.java`
- `src/main/java/com/utefit/validation/ValidInputValidator.java`
- `src/main/resources/templates/fragments/validation.html`
- `src/main/resources/static/js/utefit-validation.js`
- `src/main/resources/static/js/validation-core.js`
- `src/main/resources/static/js/validation-rules.json`
- `src/main/resources/static/css/utefit-validation.css`
- `src/test/java/com/utefit/controller/FormValidationMvcTest.java`
- `src/test/java/com/utefit/validation/InputRulesTest.java`
- `src/test/java/com/utefit/validation/ValidationBrowserApplication.java`
- `src/test/js/validation.test.cjs`
- `src/test/playwright/validation.cjs`
- `docs/VALIDATION_AUDIT_2026-10-31.md`

## 9. Inventory form/field chi tiết

Khai báo template trước thay đổi; `#0` là control tách rời hoặc control fragment được ghép vào form khi render. Fragment có thể lặp ở nhiều trang/role. `_csrf` động đã loại khỏi thống kê. URL Thymeleaf được giữ nguyên để tránh đoán route.

| Template / số thứ tự | Method / action | Field/control |
|---|---|---|
| admin/branches/form.html #1 | POST ${isEdit ? '/admin/branches/' + branchForm.id + '/edit' : '/admin/branches/create'} | name, address, phone, openingTime, closingTime, imageFile, imageUrl, imagePublicId, description, active |
| admin/branches/list.html #1 | GET @{/admin/branches} | keyword, active |
| admin/branches/list.html #2 | POST @{/admin/branches/{id}/toggle-active(id=${b.id})} | (hành động/CSRF) |
| admin/categories/form.html #1 | POST ${isEdit ? '/admin/categories/' + categoryForm.id + '/edit' : '/admin/categories/create'} | requiredBenefit, name, description, active |
| admin/categories/list.html #1 | GET @{/admin/categories} | keyword, active |
| admin/categories/list.html #2 | POST @{/admin/categories/{id}/toggle-active(id=${c.id})} | (hành động/CSRF) |
| admin/checkins/list.html #1 | POST @{/admin/checkins/qr} | branchId, qrToken |
| admin/checkins/list.html #2 | POST @{/admin/checkins} | email, branchId |
| admin/checkins/list.html #3 | GET @{/admin/checkins} | email, branchId, date, open |
| admin/checkins/list.html #4 | POST @{/admin/checkins/{id}/checkout(id=${c.id})} | (hành động/CSRF) |
| admin/checkins/list.html #0 | JS JS / standalone | qr-image |
| admin/classes/form.html #1 | POST ${isEdit ? '/admin/classes/' + classForm.id + '/edit' : '/admin/classes/create'} | name, branchId, categoryId, trainerId, capacity, status, description |
| admin/classes/list.html #1 | GET @{/admin/classes} | keyword, branchId, categoryId, status |
| admin/classes/list.html #2 | POST @{/admin/classes/{id}/cancel(id=${fc.id})} | (hành động/CSRF) |
| admin/fragments/layout.html #1 | POST @{/auth/logout} | (hành động/CSRF) |
| admin/managers.html #1 | POST @{/admin/managers} | name, email, password |
| admin/managers.html #2 | POST @{/admin/managers/{id}/branches/{branchId}/remove(id=${manager.id},branchId=${branch.id})} | (hành động/CSRF) |
| admin/managers.html #3 | POST @{/admin/managers/{id}/branches(id=${manager.id})} | branchId |
| admin/payments/cash-pending.html #1 | POST @{/admin/payments/{id}/confirm-cash(id=${p.id})} | (hành động/CSRF) |
| admin/payments/detail.html #1 | POST @{/admin/payments/{id}/approve-cancellation(id=${payment.id})} | reason, reviewed |
| admin/payments/list.html #1 | GET @{/admin/payments} | keyword, method, status |
| admin/payments/sepay-manual-refund.html #1 | POST @{/admin/payments/sepay-reconciliation/{id}/verify(id=${payment.id})} | gateway, accountNumber, subAccount, paymentReference, amount, bankReference, bankDate, source, evidence, reason, independentlyVerified |
| admin/payments/sepay-manual-refund.html #2 | POST @{/admin/payments/sepay-reconciliation/{id}/refund(id=${payment.id})} | recordId, bankReference, amount, bankDate, source, evidence, recipientVerification, reason, independentlyVerified |
| admin/payments/sepay-manual.html #1 | POST @{/admin/payments/sepay-reconciliation/{id}/confirm-received(id=${payment.id})} | bankReference, independentlyVerified |
| admin/payments/sepay-reconciliation.html #1 | POST @{/admin/payments/sepay-reconciliation/lookup} | date, apiPage |
| admin/plans/form.html #1 | POST ${isEdit ? '/admin/membership-plans/' + planForm.id + '/edit' : '/admin/membership-plans/create'} | benefit, branchIds, _branchIds, accessScope, name, durationQuantity, durationUnit, bonusQuantity, bonusUnit, price, description, active |
| admin/plans/list.html #1 | GET @{/admin/membership-plans} | keyword, active |
| admin/plans/list.html #2 | POST @{/admin/membership-plans/{id}/toggle-active(id=${p.id})} | (hành động/CSRF) |
| admin/promotions/list.html #1 | POST @{/admin/promotions/programs/{kind}/state(kind=${program.kind})} | version, active |
| admin/promotions/list.html #2 | POST @{/admin/promotions/save} | id, code, discountPercent, startDate, endDate, quantity, active, minOrderAmount, maxDiscountAmount, perUserLimit |
| admin/promotions/list.html #3 | POST @{/admin/promotions/{id}/toggle(id=${p.id})} | (hành động/CSRF) |
| admin/promotions/program-form.html #1 | POST @{/admin/promotions/programs/{kind}(kind=${kind})} | version, active, program-kind, percent, discountCap, welcome-limit, minimumMonths, renewalGraceDays, valueThreshold, valueDiscount, premiumThreshold, premiumDiscount, startDate, endDate |
| admin/reviews/list.html #1 | GET @{/admin/reviews} | branchId, rating |
| admin/reviews/list.html #2 | POST @{/admin/reviews/{id}/toggle-visibility(id=${r.id})} | (hành động/CSRF) |
| admin/schedules/form.html #1 | POST ${isEdit ? '/admin/schedules/' + scheduleForm.id + '/edit' : '/admin/schedules/create'} | fitnessClassId, scheduleDate, startTime, endTime, room, active |
| admin/schedules/list.html #1 | GET @{/admin/schedules} | fitnessClassId, branchId, scheduleDate, active |
| admin/schedules/list.html #2 | POST @{/admin/schedules/{id}/toggle-active(id=${s.id})} | (hành động/CSRF) |
| admin/trainers/list.html #1 | GET @{/admin/trainers} | keyword, branchId, active |
| admin/trainers/list.html #2 | POST @{/admin/trainers/{id}/assign-branch(id=${t.id})} | branchId |
| admin/trainers/list.html #3 | POST @{/admin/trainers/{id}/toggle-active(id=${t.id})} | (hành động/CSRF) |
| admin/users/detail.html #1 | POST @{/admin/users/{id}/toggle-lock(id=${user.id})} | (hành động/CSRF) |
| admin/users/list.html #1 | GET @{/admin/users} | keyword, role, status |
| admin/users/list.html #2 | POST @{/admin/users/{id}/toggle-lock(id=${u.id})} | (hành động/CSRF) |
| auth/forgot-password.html #1 | POST @{/forgot-password} | email |
| auth/login.html #1 | POST @{/login} | email, password |
| auth/register.html #1 | POST @{/register} | firstName, lastName, email, phone, password, confirmPassword |
| auth/reset-password.html #1 | POST @{/reset-password} | email, code, newPassword, confirmPassword |
| auth/verify-otp.html #1 | POST @{/verify-otp} | email, code |
| auth/verify-otp.html #2 | POST @{/resend-otp} | email |
| chat/conversation.html #1 | POST  | content |
| fragments/chat-sidebar.html #1 | POST  | (hành động/CSRF) |
| fragments/chat-sidebar.html #0 | JS JS / standalone | ufChatSearch |
| fragments/favorite.html #1 | POST @{/member/favorites/{type}/{id}(type=${type},id=${targetId})} | returnTo |
| fragments/favorite.html #2 | POST @{/member/favorites/{id}/remove(id=${favoriteId})} | returnTo |
| fragments/header.html #1 | POST @{/auth/logout} | (hành động/CSRF) |
| fragments/header.html #2 | POST  | (hành động/CSRF) |
| fragments/header.html #3 | POST  | (hành động/CSRF) |
| fragments/header.html #4 | POST  | (hành động/CSRF) |
| fragments/public-directory.html #1 | GET @{/trainers} | branchId |
| fragments/public-directory.html #2 | GET @{/classes} | branchId |
| fragments/public-directory.html #3 | GET @{/branches} | (hành động/CSRF) |
| fragments/public-directory.html #4 | GET @{/membership-plans} | (hành động/CSRF) |
| fragments/public-directory.html #0 | JS JS / standalone | keyword |
| guest/branches.html #1 | GET @{/branches} | keyword |
| guest/classes.html #1 | GET @{/classes} | keyword, branchId |
| guest/membership-plans.html #1 | GET @{/membership-plans} | keyword |
| guest/reviews.html #1 | GET @{/reviews} | branchId, rating |
| guest/trainers.html #1 | GET @{/trainers} | keyword, branchId |
| manager/fragments/layout.html #1 | POST  | (hành động/CSRF) |
| manager/index.html #1 | GET @{/manager} | branchId |
| manager/index.html #2 | POST @{/manager/checkins} | branchId, email |
| manager/index.html #3 | POST @{/manager/checkins/qr} | branchId, qrToken |
| manager/index.html #4 | POST @{/manager/checkins/{id}/checkout(id=${record.id})} | branchId |
| manager/index.html #5 | POST @{/manager/schedules/{id}/toggle(id=${schedule.id})} | branchId |
| manager/index.html #0 | JS JS / standalone | qr-image |
| manager/schedule-form.html #1 | POST ${editId == null} ? @{/manager/schedules/new} : @{/manager/schedules/{id}/edit(id=${editId})} | branchId, fitnessClassId, scheduleDate, startTime, endTime, room, active |
| member/chat-conversation.html #1 | POST  | content |
| member/classes.html #1 | GET @{/member/classes} | keyword, branchId |
| member/classes.html #2 | POST @{/member/classes/{id}/book(id=${s.id})} | (hành động/CSRF) |
| member/classes.html #3 | POST @{/member/classes/{id}/cancel(id=${s.id})} | (hành động/CSRF) |
| member/favorites.html #1 | POST @{/member/favorites/{id}/remove(id=${favorite.id})} | (hành động/CSRF) |
| member/fragments/chat-sidebar.html #1 | POST  | (hành động/CSRF) |
| member/fragments/chat-sidebar.html #0 | JS JS / standalone | ufChatSearch |
| member/fragments/layout.html #1 | POST  | (hành động/CSRF) |
| member/fragments/notifications.html #1 | POST @{/notifications/read-all} | (hành động/CSRF) |
| member/fragments/notifications.html #2 | POST @{'/notifications/' + ${n.id} + '/read'} | (hành động/CSRF) |
| member/memberships.html #1 | POST @{/member/memberships/{id}/sepay(id=${period.id})} | (hành động/CSRF) |
| member/memberships.html #2 | POST @{/member/memberships/{id}/cash(id=${period.id})} | (hành động/CSRF) |
| member/memberships.html #3 | POST @{/member/memberships/{id}/cancel(id=${period.id})} | (hành động/CSRF) |
| member/memberships.html #4 | POST @{/member/memberships/{id}/renew(id=${period.id})} | (hành động/CSRF) |
| member/memberships.html #5 | POST @{/member/memberships} | branchId, planId, offerSelection, promotionCode |
| member/notifications.html #1 | POST @{/member/notifications/read-all} | (hành động/CSRF) |
| member/notifications.html #2 | POST @{/member/notifications/{id}/read(id=${item.id})} | (hành động/CSRF) |
| member/profile.html #1 | POST @{/member/profile/avatar} | avatar |
| member/profile.html #2 | POST @{/member/profile} | firstName, lastName, phone, member-profile-email |
| member/profile.html #3 | POST @{/member/profile/password} | currentPassword, newPassword, confirmPassword |
| member/pt-schedules.html #1 | GET @{/member/pt-schedules} | from, to |
| member/qr-checkin.html #1 | POST @{/member/qr-checkin} | (hành động/CSRF) |
| member/qr-checkin.html #2 | POST @{/member/qr-checkin/checkout} | (hành động/CSRF) |
| member/review-form.html #1 | POST ${editId == null}                   ? @{/member/reviews/new}                   : @{/member/reviews/{id}/edit(id=${editId})} | branchId, branchId, trainerId, trainerId, rating, comment, image, video, removeVideo |
| member/schedule.html #1 | POST ${slot.cancelUrl} | weekStart |
| member/schedule.html #2 | POST ${slot.completeUrl} | weekStart |
| member/trainer-request-form.html #1 | POST @{/member/trainer-requests} | trainerId, message |
| member/trainer-requests.html #1 | POST @{/member/chats/start/{id}(id=${req.trainerId})} | (hành động/CSRF) |
| member/trainer-requests.html #2 | POST @{/member/trainer-requests/{id}/cancel(id=${req.id})} | (hành động/CSRF) |
| notification/list.html #1 | POST @{/notifications/read-all} | (hành động/CSRF) |
| notification/list.html #2 | POST @{'/notifications/' + ${n.id} + '/read'} | (hành động/CSRF) |
| pt/classes/detail.html #1 | POST @{/pt/classes/{s}/attendance/{r}(s=${schedule.scheduleId},r=${reg.registrationId})} | (hành động/CSRF) |
| pt/classes/list.html #1 | POST ${slot.cancelUrl} | weekStart |
| pt/classes/list.html #2 | POST ${slot.completeUrl} | weekStart |
| pt/fragments/layout.html #1 | POST  | (hành động/CSRF) |
| pt/personal-schedules/create.html #1 | POST @{/pt/personal-schedules/create} | memberId, scheduleDate, startTime, endTime, title, note |
| pt/personal-schedules/list.html #1 | POST ${slot.cancelUrl} | weekStart |
| pt/personal-schedules/list.html #2 | POST ${slot.completeUrl} | weekStart |
| pt/profile/edit.html #1 | POST @{/pt/profile/edit} | specialization, yearsExperience, bio, avatarFile |
| pt/profile/edit.html #2 | POST @{/pt/profile/edit} | avatarFile, specialization, yearsExperience, bio |
| pt/trainer-requests/list.html #1 | POST @{/pt/trainer-requests/{id}/accept(id=${req.id})} | (hành động/CSRF) |
| pt/trainer-requests/list.html #2 | POST @{/pt/trainer-requests/{id}/reject(id=${req.id})} | (hành động/CSRF) |
| pt/trainer-requests.html #1 | POST @{'/pt/trainer-requests/' + ${req.id} + '/accept'} | (hành động/CSRF) |
| pt/trainer-requests.html #2 | POST @{'/pt/trainer-requests/' + ${req.id} + '/reject'} | (hành động/CSRF) |

Tham khảo cấu trúc đầu số: [VNPT](https://vnpt.vn/tu-van/danh-sach-so-dien-thoai-dau-so-vinaphone-moi-nhat-2024), [Viettel](https://www.vietteltelecom.vn/tin-tuc/tin-dich-vu/bi-quyet-chon-sim-so-dep-viettel-ban-co-biet/19762220). Danh sách prefix cấu hình ứng dụng không phải dịch vụ xác minh thuê bao.
