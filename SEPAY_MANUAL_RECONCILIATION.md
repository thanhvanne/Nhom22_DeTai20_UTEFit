# Đối soát ngân hàng thủ công SePay Live

## Phạm vi và an toàn

- Mở rộng **Admin → Payment → Đối soát SePay**, không tạo checkout hoặc webhook thứ hai.
- Không tự xử lý Payment #1 / Membership #2. Không chạy migration, không kết nối SQL Server, không gửi webhook hay giao dịch thật trong quá trình triển khai này.
- Tiền đã vào ngân hàng nhưng không xuất hiện ở SePay phải kiểm tra **trực tiếp ngân hàng nhận / sao kê chính thức**. Ảnh người chuyển không đủ chứng minh.
- Đây là xác nhận bởi Admin đang hoạt động, không phải xác minh ngân hàng tự động. Người xử lý chịu trách nhiệm tính xác thực của bằng chứng, lưu tài liệu gốc ở kho nội bộ được kiểm soát. Không nhập mật khẩu, OTP, secret hoặc toàn bộ sao kê có thông tin khách hàng khác.
- Không bỏ HMAC, không tự tạo/phát lại webhook, không cấp approval Live và không bật nhận đơn mới.

## Cấu hình trước khi sử dụng

Trong IntelliJ → Run → Edit Configurations → Environment variables, người quản trị tự cấu hình:

```text
SEPAY_MANUAL_RECONCILIATION_ENABLED=true
SEPAY_LIVE_ACCEPT_NEW_PAYMENTS=false
```

Công tắc đối soát mặc định `false`: khi chưa bật, không Admin nào được ghi nhận tiền/kích hoạt/ghi hoàn tiền; lịch sử vẫn có thể xem theo quyền Admin. Khi bật, **mọi tài khoản có role ADMIN, status ACTIVE, enabled=true trong database** đều được đối soát; không cần danh sách ID hay cấu hình riêng từng người. Biến danh sách ID trước đây không còn được mã nguồn sử dụng, có thể gỡ khỏi cấu hình IntelliJ. Khởi động lại ứng dụng để áp dụng thay đổi công tắc. Không có thay đổi cấu hình chạy nào được tự áp dụng trong lần sửa phân quyền này.

Tất cả thao tác ghi vẫn phải vượt `SepayDatabaseGuard`: đúng profile Live, database Live, cấu hình HMAC/HTTPS/cookie và deployment approval hiện có. Không phụ thuộc công tắc nhận đơn mới; đối soát đơn cũ được phép khi công tắc đó vẫn `false`. Backend đọc lại và refresh tài khoản từ database ở **mỗi** bước xác minh nhận tiền, kích hoạt gói và xác nhận hoàn tiền; role ADMIN trong session không đủ nếu database đã chuyển role, khóa hoặc vô hiệu hóa tài khoản. MEMBER/PT không được phép. Admin khác có thể tiếp tục xử lý bước sau; audit giữ riêng người xác minh và người kết luận.

## Migration V710 — người quản trị tự áp dụng

File: `database/migrations/V710__sepay_manual_bank_reconciliation.sql`.

1. Dừng ứng dụng, giữ công tắc nhận đơn mới tắt; sao lưu đúng database Live và ghi nhận phiên bản source.
2. Trong SSMS chọn đúng `UTEFitLiveDemo`, xác minh bằng truy vấn chỉ đọc `SELECT DB_NAME();`. Không dùng database chính hoặc QA cho dữ liệu tiền thật.
3. Kiểm tra V706, V708, V709 đã áp dụng. V710 yêu cầu bảng snapshot, bảng deployment guard và hai cột VA/subAccount. Không sửa V701–V709.
4. Rà soát rồi chạy V710 **một lần**, sau khi được người quản trị cho phép. Script tạo duy nhất bảng `sepay_bank_reconciliations`, FK tới Payment/User, unique claim giao dịch và filtered unique mã hoàn tiền; có transaction + XACT_ABORT.
5. V710 không INSERT approval, không đọc/cập nhật Payment/Membership cũ, không backfill giao dịch giả, không thay đổi trạng thái hoặc dữ liệu tài khoản ngân hàng.
6. Nếu thất bại: không chạy lại mù quáng. Kiểm tra `SELECT XACT_STATE(), @@TRANCOUNT;`; nếu session còn transaction lỗi, `ROLLBACK TRANSACTION` trong chính session đó. Kiểm tra bảng/index đã tồn tại trước khi quyết định tiếp. Không sửa Payment bằng SQL.
7. Rollback triển khai: tắt chức năng manual và tắt nhận đơn mới, sao lưu audit mới, quay lại bản source đã bảo vệ. **Giữ bảng audit**, không drop dữ liệu đã ghi nhận tiền/hoàn tiền. Nếu đã có kết quả tài chính, phải đối chiếu trước khi quay lại code cũ; code cũ không hiểu claim thủ công. Không phục hồi backup cũ đè lên các khoản nhận tiền sau backup.

Schema SQL Server được chuẩn bị nhưng chưa chạy/xác minh trên SQL Server thực tế. Test H2 không thay thế diễn tập migration SQL Server do người quản trị thực hiện.

## Thao tác trên giao diện

1. Đăng nhập Admin, mở `/admin/payments/sepay-reconciliation`. Bảng mới hiển thị tối đa 20 đơn Live PENDING hoặc có webhook REVIEW/trang, kèm mã, tiền, TK chính, VA, ngày tạo/hết hạn. Có phân trang riêng với danh sách webhook.
2. Chọn **Xem / xác minh ngân hàng**. Có thể vào từ `/admin/payments/{id}` → **Đối soát ngân hàng SePay**, hoặc `/admin/payments/sepay-reconciliation/{id}`. Trang chi tiết hiển thị snapshot, tối đa 20 webhook gần nhất cùng nguyên nhân, và toàn bộ bản ghi xác minh của đơn. Đơn Test chỉ xem, không xác nhận thủ công.
3. Màn hình chính chỉ có hai thao tác: **Chưa nhận tiền** và **Đã nhận tiền**. Thông tin mã đơn, số tiền, ngân hàng, chủ tài khoản, TK chính, VA, thời gian và trạng thái đều đọc từ database; không nhập lại. **Chưa nhận tiền** là điều hướng GET chỉ đọc, không cập nhật trạng thái, không hủy đơn, không ghi audit giả rằng ngân hàng chắc chắn chưa nhận tiền. Nếu trạng thái đang PENDING thì vẫn PENDING.
4. Đối chiếu trực tiếp ngân hàng nhận với snapshot: đúng tài khoản/VA, số tiền, nội dung mã đơn, thời gian trong hạn và mã tham chiếu **duy nhất do ngân hàng cấp**. Chọn **Đã nhận tiền** để mở popup Bootstrap. Chỉ nhập mã giao dịch ngân hàng và tích xác nhận đã kiểm tra giao dịch thật đúng các thông tin này, không chỉ dựa vào ảnh người chuyển. Không dùng số Payment hoặc mã tùy đặt làm mã ngân hàng.
5. Chọn **Xác nhận và kích hoạt gói**. Backend lấy thông tin đơn từ snapshot, bỏ qua tham số amount/account/VA/reference giả gửi thêm từ frontend; vẫn kiểm tra quyền Admin trong database, Live guard, trạng thái, bằng chứng, trùng bank reference, giá/thời hạn snapshot và REVIEW xung đột. Sau nâng cấp Membership, không chặn toàn bộ gói ACTIVE khác: kỳ cùng quyền lợi/phạm vi được nối sau hạn đã thanh toán, gói quyền lợi khác có thể tồn tại đồng thời. Bước xác minh và kích hoạt được gộp trong **một transaction**; nếu bước sau thất bại, cả audit vừa tạo và Payment/Membership/thông báo đều rollback. Audit vẫn ghi method `MANUAL_BANK_RECONCILIATION`, người xử lý, thời gian và lời xác nhận kiểm tra ngân hàng.
6. **Luồng hoàn tiền riêng**: chọn liên kết nhỏ **Xử lý riêng / xác nhận hoàn tiền thực tế** tới `/admin/payments/sepay-reconciliation/{id}/refund-details`. Các biểu mẫu đầy đủ không còn nằm ở màn hình xác nhận chính. Nếu chưa có bằng chứng, lưu xác minh tiền nhận bằng sao kê/thời điểm thực tế trước, không tự kích hoạt. Nếu cần cấp gói sau đó, trở về màn hình chính để xác nhận bằng đúng mã ngân hàng đã lưu. Nếu không cấp gói, người có quyền hoàn tiền bằng ngân hàng **bên ngoài UTEFit**. Chỉ khi hoàn đã hoàn tất, nhập mã hoàn riêng, đủ số tiền, thời gian, sao kê, bằng chứng đúng người nhận và lý do hoàn/hủy. Xác nhận mới ghi `REFUNDED` + `Membership.CANCELLED`, không tạo lệnh chuyển tiền, không ghi PAID trung gian, không ghi doanh thu.
7. Xem lại audit tại chính URL chi tiết đơn sau khi xử lý. Không có chức năng sửa/xóa bằng chứng hoặc mở lại bản ghi đã kết thúc trên giao diện này.

### Thời gian nhận tiền không thể suy ra từ snapshot

- Snapshot chứa thời gian tạo/hết hạn đơn, **không chứa giờ ngân hàng thực nhận tiền**. Không tự điền giờ tạo đơn thành giờ nhận tiền để vượt expiry.
- Nếu đã có audit VERIFIED đúng mã ngân hàng: dùng lại nguyên thời điểm đã xác minh. Nếu có webhook đã xác thực lưu bền vững, đúng ngân hàng/tài khoản/VA/mã đơn/số tiền: dùng thời điểm giao dịch trong bản ghi đó, audit đánh dấu `TIME_BASIS=AUTHENTICATED_WEBHOOK`.
- Nếu chưa có hai nguồn trên: popup ngắn chỉ áp dụng cho đơn **còn hạn** và Admin phải xác nhận đã kiểm tra giao dịch thực nhận trong cửa sổ thanh toán. Thời điểm ghi nhận dùng lúc Admin xác nhận, lưu rõ `TIME_BASIS=ADMIN_CONFIRMATION` trong evidence/reason. Đây **không phải giờ hạch toán ngân hàng**, không phải bằng chứng tự động từ ngân hàng. Trường `bank_date` hiện có lưu mốc ghi nhận này; Payment.paidAt/Membership dùng ngày ghi nhận theo quy trình hiện có. Nếu cần bảo toàn đúng ngày/giờ ngân hàng (ví dụ qua ngày), dùng xử lý riêng để nhập bằng chứng thời gian thực trước rồi xác nhận tại màn hình chính.
- Đơn đã hết hạn mà chưa có bằng chứng thời gian nhận tiền: báo lỗi rõ và không ghi PAID/audit mới; mở xử lý riêng để kiểm tra sao kê. Nếu tiền thật tới sau hạn, vẫn không kích hoạt. Không có ngoại lệ riêng cho Payment #1.
- Backend không thể chứng minh số tiền thực nhận chỉ từ snapshot. Khi không có webhook, trách nhiệm đối chiếu số tiền/tài khoản/nội dung/thời gian thuộc Admin xác nhận trực tiếp ngân hàng; checkbox không thay thế việc kiểm tra đó.

## Các trường hợp phải dừng

- Thiếu bằng chứng, không có mã ngân hàng duy nhất, sai tài khoản/VA, sai số tiền hoặc sai mã đơn: **không xác nhận**, không tự gán tiền cho đơn khác. Hiện luồng chỉ xác nhận khoản tiền khớp đủ đơn; thiếu/thừa tiền hoặc không biết đơn phải được điều tra riêng, không ép form để vượt kiểm tra.
- Đơn cũ có snapshot VA rỗng: màn hình chính tự dùng TK chính và VA rỗng từ snapshot, không bắt nhập hoặc ép VA mới. Không đổi snapshot. Nếu snapshot yêu cầu VA mà thực tế chuyển vào main, Admin không được xác nhận checkbox; webhook sai VA vẫn bị từ chối, không bypass.
- Tiền tới sau hạn: có thể lưu bằng chứng tiền nhận nhưng không kích hoạt; chỉ luồng xác nhận hoàn đủ tiền thực tế cho phép hủy đơn chờ. Tiền nhận đúng hạn nhưng đối soát trễ có thể cấp gói theo ngày nhận tiền nếu quyền lợi chưa hết hạn. Quyền lợi đã hết hạn cần xử lý hỗ trợ riêng, không sửa snapshot.
- Có REVIEW khác/mâu thuẫn hoặc trùng mã ngân hàng: không kích hoạt/hủy tùy tiện. Không có nút bỏ qua REVIEW.
- Bản ghi APPLIED/REFUNDED không được kích hoạt lại. Luồng hoàn tiền riêng nay chấp nhận cả đơn PENDING đã VERIFIED và đơn PAID có audit APPLIED hoặc WEBHOOK. Chỉ sau khi Admin kiểm tra khoản hoàn đủ tiền thực tế mới đổi Payment thành REFUNDED, Membership thành CANCELLED và kiểm tra lại lớp/PT tương lai. Kỳ lịch sử không bị xóa hoặc đổi ngày; chỉ hủy chỗ không còn được bất kỳ gói hợp lệ nào khác bao phủ, có thông báo. Đơn PAID cũ không có audit nhận tiền phù hợp phải dừng để điều tra, không tạo bằng chứng giả. Chi tiết tại `MEMBERSHIP_UPGRADE.md`.

## Idempotency, webhook đến muộn và transaction

- Khóa theo thứ tự **User → Membership → Payment**, refresh dữ liệu sau khóa. Tiền nhận thủ công được claim bằng UNIQUE `(environment,gateway,account_number,bank_reference)`; gateway/mã ngân hàng được chuẩn hóa uppercase, không so khớp substring.
- Webhook đã xác thực HMAC tiếp tục đi qua controller cũ. Trước khi tự kích hoạt, backend cũng ghi claim vào bảng chung; do đó webhook và đối soát không thể cùng gán một mã ngân hàng cho hai đơn. Kiểm tra sổ webhook lịch sử vẫn được giữ để bảo vệ giao dịch trước V710.
- Webhook đến sau khi đã xác minh thủ công được lưu `REVIEW` với lý do riêng, không tự kích hoạt lại, kể cả webhook thiếu bank reference. Webhook trùng vẫn dùng cơ chế ID/fingerprint hiện có. Không đổi lịch sử webhook thành MATCHED giả để che nguồn xác nhận thủ công.
- Unique mã hoàn tiền chặn dùng một giao dịch hoàn để hủy nhiều đơn. Ghi PAID + kích hoạt + thông báo + kết luận audit cùng transaction; ghi REFUNDED + CANCELLED + audit hoàn cũng cùng transaction. Popup ngắn tạo audit mới và kích hoạt cùng transaction nên lỗi sẽ rollback cả hai. Nếu dùng lại bằng chứng đã lưu ở xử lý riêng, bằng chứng trước đó vẫn còn VERIFIED khi kích hoạt lỗi.
- Các REVIEW liên kết đã được xử lý thủ công vẫn hiển thị để audit; xem kết luận APPLIED/REFUNDED của bản ghi thủ công, không cộng ledger kỹ thuật vào doanh thu.

## Kiểm thử và giới hạn

Test tự động dùng H2 in-memory, user/order riêng, không gọi ngân hàng, API SePay, SQL Server hay endpoint Live bên ngoài. Bao gồm RBAC/CSRF/quyền Admin từ database, nhiều Admin xử lý và ghi audit đúng người, tài khoản bị đổi role/khóa/vô hiệu hóa, công tắc tắt, thiếu bằng chứng, snapshot/VA, trùng giao dịch, chạy đồng thời, webhook đến muộn, HMAC sai, rollback kích hoạt, hoàn tiền và tương thích đơn cũ.

Kết quả thực chạy ngày 26/10/2026:
- Maven compile + 227 Java tests PASS sau nâng cấp Membership, 0 FAIL/ERROR/SKIP; trong đó 43 test đối soát thủ công và 18 test Membership mới. Luồng xác nhận ngắn vẫn kiểm tra snapshot backend, consent/mã ngân hàng bắt buộc, rollback cả audit mới sau khi kích hoạt/thông báo, đơn main không VA, expiry không được suy đoán, tái sử dụng audit VERIFIED, sai amount trong REVIEW, Chưa nhận tiền chỉ đọc, biểu mẫu hoàn tiền riêng, frontend giả amount/account bị bỏ qua, CSRF/RBAC, thời gian nhận đúng hạn từ webhook lưu sẵn và xác nhận đồng thời. Các ca quyền Admin từ database và regression trước đó vẫn PASS.
- 38 JavaScript tests PASS; không sửa JavaScript trong lần triển khai này.
- Regression bao gồm CASH, SePay/VA/HMAC, VNPay đã vô hiệu hóa khởi tạo mới, QR, trạng thái Member, Manager, Favorites và Review.
- Log cục bộ mới nhất (ignored, không commit): `target/membership-upgrade-release-tests.log`, `target/membership-upgrade-js-tests.log`.
- Không chạy Playwright, không kiểm chứng migration trên SQL Server hoặc đối soát ngân hàng thật. Chưa xác nhận triển khai Live từ kết quả H2.

Người quản trị cần kiểm thử form bằng QA riêng và diễn tập migration SQL Server trước khi bật đối soát trên Live. Không lấy Payment #1 làm test. Không chuyển thêm tiền để chữa đơn cũ; chỉ xử lý thủ công sau khi đã có bằng chứng ngân hàng thật đầy đủ và xác nhận của Admin hợp lệ.

Tài liệu đối chiếu SePay: https://developer.sepay.vn/vi/sepay-webhooks — cơ chế webhook và đối soát thủ công được giữ tách biệt.

## File thay đổi trong lần triển khai này

Mới:
- `src/main/java/com/utefit/entity/SepayBankReconciliation.java`: claim giao dịch dùng chung và audit nhận/hoàn tiền.
- `src/main/java/com/utefit/repository/SepayBankReconciliationRepository.java`: truy vấn claim/audit.
- `src/main/java/com/utefit/service/SepayManualReconciliationService.java`: kiểm tra quyền Admin từ database, xác minh, kích hoạt và xác nhận hoàn.
- `src/main/resources/templates/admin/payments/sepay-manual.html`: màn hình chi tiết nằm trong Admin Payment.
- `src/main/resources/templates/admin/payments/sepay-manual-refund.html`: chuyển biểu mẫu dài sang màn hình xử lý riêng/hoàn tiền, không tạo module mới.
- `database/migrations/V710__sepay_manual_bank_reconciliation.sql`: schema bổ sung, chưa áp dụng.
- `src/test/java/com/utefit/service/SepayManualReconciliationIntegrationTest.java`: test H2/MockMvc.
- `SEPAY_MANUAL_RECONCILIATION.md`: hướng dẫn này.

Sửa trên nội dung hiện có, giữ các thay đổi SePay chưa commit:
- `src/main/java/com/utefit/controller/admin/SepayReconciliationController.java`: thêm GET chi tiết và ba POST có CSRF/RBAC.
- `src/main/java/com/utefit/repository/SepayPaymentDetailsRepository.java`: phân trang đơn cần đối soát, fetch snapshot phục vụ giao diện.
- `src/main/java/com/utefit/repository/SepayWebhookTransactionRepository.java`: tra bank reference, REVIEW xung đột và lịch sử đơn.
- `src/main/java/com/utefit/service/SepayWebhookService.java`: dùng chung claim, giữ webhook muộn ở REVIEW.
- `src/main/resources/templates/admin/payments/sepay-reconciliation.html`: bổ sung danh sách đơn PENDING/REVIEW.
- `src/main/resources/templates/admin/payments/detail.html`: liên kết mở lịch sử đối soát từng đơn.
- `src/test/java/com/utefit/service/LiveDemoSchemaPreparationTest.java`: nhận diện thêm entity audit, không thay đổi bootstrap schema cũ.

Không sửa `application.properties`, cấu hình Live hiện tại, migration đã có, QR Check-in/Check-out, Review, Favorites hoặc Manager. Không commit/push/merge; HEAD vẫn `2dadc44`, commit QR `a8798f7` vẫn là ancestor.

### Phạm vi lần đơn giản hóa giao diện

Chỉ cập nhật 6 file: service đối soát, controller đối soát, `sepay-manual.html`, test đối soát và tài liệu này; thêm `sepay-manual-refund.html` bằng cách chuyển biểu mẫu hiện có sang màn hình riêng. Không cần migration mới ngoài V710 vốn đã có. Không thực hiện migration hoặc thao tác với Payment #1. Khởi động lại Spring Boot bằng cấu hình hiện tại rồi mở lại trang đối soát; không đổi công tắc nhận đơn mới.
