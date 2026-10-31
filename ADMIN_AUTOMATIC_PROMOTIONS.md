# Quản lý chương trình tự động trên Admin — chờ duyệt triển khai UTEFitDB

## Nguyên nhân và nguồn cấu hình

Trước đây Admin chỉ đọc `PromotionCatalogService` và `LoyaltyProperties.enabled` mặc định false nên hiển thị “Chờ kích hoạt”. Không có endpoint ghi cấu hình tự động. Các file application hiện tại không đặt khóa loyalty; process của agent cũng không có UTEFIT_LOYALTY_ENABLED. Không coi đó là bằng chứng đầy đủ về process IntelliJ đang chạy; không đọc/ghi đè biến SePay của IntelliJ.

Bản mới dùng một bảng `automatic_promotions` chứa đúng năm khóa cố định WELCOME/LOYAL/VALUE/SILVER/GOLD. Khi có cấu hình DB, nó ưu tiên hơn properties, kể cả UTEFIT_LOYALTY_ENABLED=false. Không cần cờ tổng mới, không cần restart mỗi lần lưu/bật/tắt. Nếu thiếu một dòng trong bộ cấu hình đã có, loại thiếu không được tự bật theo fallback. Properties chỉ giữ tương thích khi bảng chưa có dòng nào; Admin không thể ghi/tạo loại tùy ý.

Mỗi lần chốt giá đọc một snapshot cấu hình dùng chung cho đánh giá hạng/điều kiện/mức giảm. Cấu hình đã lưu không cache qua restart. Một thao tác Admin có hiệu lực với lần đọc cấu hình của đơn mới sau khi transaction lưu đã commit; không sửa đơn PENDING đã chốt.

## Thứ tự SSMS — chỉ chạy sau khi duyệt và có backup

1. Backup/restore thử và kiểm kê đơn SePay đang mở theo runbook hiện tại. Không reset/seed database. Kiểm tra trên bản restore trước.
2. Nếu chưa có V716: xem/chạy `database/migrations/V716__member_loyalty_offers.sql` và kiểm tra quyền SELECT audit bằng `database/operations/UTEFitDB_loyalty_03_runtime_permissions.template.sql`. Giữ cờ phê duyệt0, chỉ cấp phần còn thiếu sau duyệt. **Không chạy SQL 02 demo exclusions.**
3. Xem/chạy `database/migrations/V717__automatic_promotion_management.sql` trên UTEFitDB. Chỉ thêm bảng cấu hình và các loại còn thiếu, không ghi đè loại hiện có. Năm dòng mặc định active=0 để không tự đổi giá Live. Giá trị mặc định: WELCOME10%/50.000; LOYAL6tháng/10%/grace30ngày; VALUE500.000→50.000/1.000.000→100.000; SILVER5%; GOLD10%. Đọc kết quả SELECT và đối chiếu đủ năm dòng.
4. Xem/chạy `database/operations/UTEFitDB_loyalty_04_automatic_program_permissions.template.sql` với `@ApproveProgramPermissions=0`. Runtime cần SELECT và UPDATE trên `dbo.automatic_promotions`; **không cần INSERT/DELETE**. Nếu thiếu, chỉ sau phê duyệt đặt cờ1 để cấp SELECT/UPDATE. Không revoke hay mở rộng role/login/quyền guard. Kiểm tra kết quả quyền hiệu lực, không tự đánh dấu PASS.
5. Chỉ restart/deploy code mới khi V716 + V717 + quyền runtime đã đạt; giữ `ddl-auto=validate` và toàn bộ cấu hình SePay. Nếu chỉ deploy code mà chưa có V717, Hibernate validate sẽ chặn startup.
6. Đăng nhập Admin → `/admin/promotions` → bật từng chương trình hoặc mở Chỉnh sửa → Lưu. Đây là bước bật ưu đãi thật cho đơn mới, chỉ thực hiện sau khi được duyệt rollout. Không bật Live/ngân hàng mới hoặc sửa giá đơn đang mở.

Chưa thực thi bất kỳ SQL nào trên UTEFitDB trong đợt này. Chưa xác minh schema/quyền SQL Server thật. Migration/constraint/grant cần DBA kiểm tra trên bản restore; H2 không thay thế bằng chứng đó. Không cần sửa V716 hoặc các dòng audit cũ. CASH demo PAID vẫn tính hạng; không trở thành tiền SePay thật.

## Admin chỉnh được gì

- WELCOME: phần trăm, trần giảm bắt buộc, bật/tắt, ngày áp dụng. Giới hạn một lần là bất biến nghiệp vụ, hiển thị readonly; không cho tăng lượt hoặc dùng lại sau mua thật/refund.
- LOYAL: tháng sử dụng tối thiểu, phần trăm, số ngày cho phép gia hạn sau hết hạn. Giữ xác minh cùng plan/branch, gói đã bắt đầu, union ngày không trùng, không cộng thời gian demo.
- VALUE: hai ngưỡng giá gốc và tiền giảm tương ứng; ngưỡng2 > ngưỡng1, giảm2 ≥ giảm1, mỗi mức giảm < ngưỡng của nó. Không cộng hai mức.
- SILVER/GOLD: phần trăm và trần tiền giảm tùy chọn. Không sửa ngưỡng phân hạng 500.000/1.500.000 từ form này. Ngày áp dụng đều tùy chọn, bao gồm ngày cuối theo múi giờ Việt Nam.
- Tất cả: version chống ghi đè cấu hình mới, người/thời điểm lưu, lỗi field/tổng hợp, loading và xác nhận tạm dừng. Từ chối phần trăm0/100/âm, tiền không nguyên hoặc không dương, ngày đảo ngược, trường không phù hợp loại và loại chương trình không thuộc whitelist.

Một đơn một ưu đãi. Mã nhập tay thay thế AUTO, không cộng dồn. Đơn quá nhỏ khiến ứng viên giảm làm tròn giá về0 sẽ không chọn ứng viên đó; giá cuối luôn >0. Backend không nhận giá hoặc hạng từ browser.

Lượt hiển thị là số Membership có snapshot offer_kind/offer_label phù hợp: PAID là ACTIVE/EXPIRED có Payment PAID, đang giữ là PENDING. Refund không còn ở số PAID. TIER cũ phân biệt Silver/Gold bằng nhãn snapshot “Đặc quyền SILVER/GOLD”; đơn cũ thiếu snapshot không được suy đoán đã dùng. Không lấy số lượt làm doanh thu. WELCOME pending vẫn giữ quyền kể cả chương trình vừa tắt; mã vẫn giữ/hoàn lượt qua luồng hiện có.

## Tự thử an toàn

- Trên môi trường cô lập: bật WELCOME10% → Member mới xem gói200.000 thấy180.000; chỉnh15% → đơn mới170.000; tắt → không dùng WELCOME. Nếu chương trình khác tốt hơn, AUTO chọn đúng chương trình tốt nhất, không bắt buộc WELCOME thắng.
- Silver/Gold thử tài khoản có chi tiêu hợp lệ hoặc CASH demo; chỉnh quyền lợi và kiểm tra preview gói. Kiểm tra VALUE tại đúng hai ngưỡng đã lưu; LOYAL với lịch sử thực đã đủ tháng và đúng gói/chi nhánh.
- Tạo PENDING trong test trước khi sửa/tắt, sau đó kiểm tra giá snapshot và QR không đổi. Không chuyển tiền hoặc xác nhận PAID giả trên database thật. Test H2 có mock ngân hàng/HMAC cho PAID/ACTIVE/callback trùng.
- Restart bản cô lập và kiểm tra cấu hình vẫn giữ. Trên Live chỉ thử sau schema/quyền/rollout đã được phê duyệt; không cần đổi môi trường SePay.

## File thuộc đợt nâng cấp Admin

Gốc Java `src/main/java/com/utefit/`:
- Mới: `enums/AutomaticPromotionKind.java`, `entity/AutomaticPromotion.java`, `repository/AutomaticPromotionRepository.java`, `dto/admin/AutomaticPromotionForm.java`, `service/AutomaticPromotionService.java`, `controller/admin/AdminAutomaticPromotionController.java`.
- Sửa: `service/MembershipOfferService.java`, `service/MemberLoyaltyService.java`, `service/PromotionCatalogService.java`, `repository/MembershipRepository.java`, `controller/admin/AdminPromotionController.java`.

Gốc resources `src/main/resources/`:
- Mới: `templates/admin/promotions/program-form.html`, `static/css/utefit-admin-promotions.css`, `static/js/admin-promotion-programs.js`.
- Sửa: `templates/admin/promotions/list.html`; `templates/member/profile.html` chỉ cập nhật câu mô tả quyền lợi động/tạm dừng, không thay bố cục/style/binding form/avatar.

SQL: V717 và template quyền04 ở trên. Không sửa SQL03 hoặc V716. Tài liệu: file này, `LOYALTY_PROMOTIONS_IMPLEMENTATION.md`, `docs/loyalty.properties.example`.

Test: mới `src/test/java/com/utefit/service/AutomaticPromotionManagementIntegrationTest.java`, `src/test/java/com/utefit/service/AutomaticPromotionRestartTest.java`, `src/test/java/com/utefit/service/AutomaticPromotionServiceTest.java`, `src/test/js/admin-promotion-programs.test.cjs`; bổ sung LoyaltySepayIntegrationTest/LoyaltyMigrationContractTest; cập nhật constructor fixture MemberLoyaltyIntegrationTest và hai test schema LiveDemoSchemaPreparationTest/DatabaseHandoffTest để giữ bootstrap cũ nguyên vẹn và triển khai V717 riêng.

Không sửa webhook/HMAC/guard/approval/ngân hàng, không ghi UTEFitDB, không đổi dữ liệu55 tài khoản/21 CASH, không sửa UI PT/Public hoặc Admin khác. Main, không commit/push.

## Kết quả kiểm thử cuối cùng

- Compile và toàn bộ Java suite: **BUILD SUCCESS**, 337 test, 0 failure/error, 1 skipped (Cloudinary integration ngoài hệ thống, không upload thật). Bao gồm bật/tắt/sửa, validation/phân quyền/CSRF/stale version, snapshot, mã cũ, CASH demo tính hạng, QR giá sau giảm, HMAC/PAID/ACTIVE/duplicate, concurrency. Có test bật/tắt chỉ khóa/đọc đúng một cấu hình để tránh đảo thứ tự khóa khi hai Admin sửa hai loại khác nhau.
- `AutomaticPromotionRestartTest` khởi động và đóng toàn bộ Spring Boot application hai lần trên cùng H2 cô lập; lần sau dùng validate, properties.enabled=false, cấu hình15%/active/người sửa/version vẫn được đọc đúng từ DB. Không khởi động lại process IntelliJ/Live.
- JavaScript: **106/106 đạt**; gồm loading, chống submit trùng, xác nhận tạm dừng/hủy xác nhận, bfcache và các test frontend cũ.
- Browser chạy app Spring Boot thật tại loopback18111, DB H2 tách biệt, SePay tắt và tài khoản QA riêng: Admin lưu được cả5loại; WELCOME tắt→NONE, bật15%→gói200.000 còn170.000; Silver7%→186.000; Gold12%→176.000; nhập DEMO5→190.000, không cộng dồn. Sửa100% bị backend từ chối dù bỏ native validation, focus về lỗi và DB vẫn15%. Hủy xác nhận tắt giữ active; xác nhận tắt thành Tạm dừng. Member truy cập Admin trả403.
- Responsive: 18tổ hợp (bảng +5trang sửa ×1440/768/390px) không tràn ngang. Public3kích thước vẫn5nhóm, không lộ email QA và không pageerror. Member Bronze/Silver/Gold +preview mobile không tràn. Đã sửa xung đột CSS khiến card chương trình mobile bị chia đôi và định dạng lại timestamp dễ đọc; không sửa CSS Admin dùng chung.
- Đã xem ảnh desktop/tablet/mobile trong `target/loyalty-qa/final-program-*.png`; dữ liệu trên ảnh là QA/H2, không phải trạng thái Live. Style lint:0FAIL/0WARN. Inspector tự động gặp lỗi môi trường `CDP timeout: Page.enable`, không được coi PASS; dùng screenshot/đo DOM/tương tác browser trực tiếp. Đánh giá nội bộ: Admin Light giữ nguyên, bảng dễ đọc, editor2cột desktop/stack mobile, trạng thái và action rõ; không thêm hero/chart/vanity stats.
- Audit539file đã kiểm kê giữ nguyên byte, chỉ14file cũ thay đổi và16file mới đúng phạm vi; SePay core/config/guard/approval, SQL03/V716, seed và các UI không liên quan không đổi. Chưa chạy migration/GRANT thật hoặc kiểm tra quyền SQL Server; chưa tạo ngân hàng/webhook thật. Các bước đó vẫn chờ duyệt.

Log và JSON chi tiết (ignored): `target/loyalty-qa/all-tests-sanitized.log`, `admin-programs-js-tests.log`, `admin-programs-source-audit.json`, `admin-programs-browser-*.json`.
