# UTEFit — phân hạng và ưu đãi (CASH demo được tính hạng, chờ duyệt database)

## Trạng thái triển khai

Code backend/frontend đã được triển khai. Chưa chạy migration hoặc ghi dữ liệu lên UTEFitDB, chưa bật ưu đãi tự động trên app thật, chưa thực hiện giao dịch ngân hàng. **Bản quản lý Admin mới dùng V717:** năm cấu hình DB mặc định tạm dừng, Admin bật/sửa từng chương trình sau khi duyệt triển khai; không cần đổi biến môi trường hoặc restart mỗi lần chỉnh. `LoyaltyProperties.enabled` mặc định false chỉ còn là fallback tương thích khi chưa có cấu hình DB, không chặn chương trình đã được Admin bật trong DB. Không restart/deploy bản mới với schema cũ: Hibernate `validate` cần cả V716 và V717. Hướng dẫn chính thức: `ADMIN_AUTOMATIC_PROMOTIONS.md`.

Không đổi biến SePay, ngân hàng, tunnel, HMAC, controller webhook, SepayDatabaseGuard, approval hoặc trạng thái 55 tài khoản. Không nhập 7 giao dịch ở database khác. Không đổi giá/trạng thái 21 Payment mẫu. Không sửa bootstrap/seed hay schema LiveDemo cũ; V716 là bước bổ sung riêng.

## 1. Điều kiện và cách tính hạng

- Hạng được **tính từ lịch sử đã commit**, không nhận hạng/tổng chi từ trình duyệt, không lưu một bộ đếm dễ tăng trùng. Đăng nhập và mở `/member/profile` đọc ngay lịch sử hiện có; không cần mua thêm hoặc kích hoạt một lần thanh toán mới. Cờ `utefit.loyalty.enabled` không chặn tính hạng và chỉ còn điều khiển fallback trước khi có cấu hình DB; sau V717, từng chương trình do Admin quản lý.
- Tổng xét hạng = amount sau giảm của Payment PAID, có paid_at không ở ngày tương lai, số tiền dương, đúng chủ Membership ACTIVE/EXPIRED. **Tất cả CASH hợp lệ, bao gồm CASH demo seed, đều được tính hạng**, kể cả dòng đã có trong `loyalty_payment_exclusions` theo chính sách cũ. SePay chỉ tính khi snapshot môi trường LIVE. TEST không tính.
- Không tính PENDING/FAILED/CANCELLED/CANCEL_REQUESTED/REFUNDED. Một Membership và một reference giao dịch chỉ được tính một lần. Luồng refund hiện tại yêu cầu hoàn toàn bộ và chuyển Payment sang REFUNDED: dòng đó không còn được cộng. Không thêm cơ chế hoàn một phần hoặc tự suy đoán số tiền hoàn khi schema/nghiệp vụ chưa hỗ trợ.
- `LoyaltyPaymentExclusion` và bảng/cột/dữ liệu audit được giữ nguyên, không xóa hoặc sửa. Loại trừ audit vẫn áp dụng cho lịch sử ưu đãi mua thật và cho SePay như trước, **không áp dụng vào tổng xét hạng CASH**. V716 và template quyền SELECT 03 không cần sửa. Script 02 đã ngừng sử dụng, chỉ PRINT thông báo và không còn khả năng INSERT loại trừ.
- Phân biệt riêng hai mục đích: tổng xét hạng theo chính sách đồ án có CASH demo; lịch sử dùng để xác minh khách mới/thời gian gắn bó vẫn loại seed demo chính xác bằng CASH + mã `DEMO-CASH-M...` + email tương ứng. Không đổi điều kiện các chương trình khuyến mãi ngoài tác động hạng mới. Demo có thể đạt Silver/Gold nhưng vẫn chưa có lịch sử mua thật để mất ưu đãi chào mừng; backend tiếp tục chọn đúng một ưu đãi.
- **Tổng Loyalty không phải doanh thu ngân hàng**. Không đổi Payment.method, gateway evidence, báo cáo tài chính hoặc truy vấn doanh thu. CASH demo vẫn là CASH demo, không trở thành tiền SePay LIVE. Không dùng trường tổng Loyalty làm số liệu doanh thu thật.
- VNPay cũ không có snapshot phân biệt sandbox/live đáng tin cậy nên hiện không được tự công nhận là chi tiêu thật. Nếu phát hiện VNPay thật cần đối soát nguồn trước; không tự gán nguồn Live. Đây là giới hạn phân loại, không thay đổi xử lý VNPay hiện có.
- BRONZE <500.000đ; SILVER từ500.000đ đến dưới1.500.000đ; GOLD từ1.500.000đ. Tính theo số tiền sau giảm đã trả, không dùng giá gốc để nâng hạng. Không giảm ngược cho đơn đã chốt.

## 2. Một đơn, một ưu đãi

- AUTO chọn mức tiết kiệm lớn nhất trong các **chương trình tự động** đủ điều kiện; không tự áp mã công khai. Nhập mã sẽ thay thế ưu đãi tự động, kể cả nếu mã tiết kiệm ít hơn; preview hiển thị giá tương ứng. NONE cho phép không dùng ưu đãi tự động.
- Khách mới: 10%, trần50.000đ. Dựa vào lịch sử mua thật, không ngày đăng ký. Đã mua rồi, kể cả hoàn tiền, không thành khách mới lần nữa. Chỉ một đơn WELCOME chờ cho tài khoản; khi đơn này đang chờ thì không tạo đơn khác để tránh tranh chấp “đơn đầu tiên”. Đơn cũ cùng gói trả lại snapshot cũ, không giữ thêm lượt.
- Hủy đơn chưa nhận tiền giải phóng quyền lần đầu, không phải đã dùng nhiều lần: chỉ được hưởng một lần khi mua thật. Đã nhận tiền không được hủy qua luồng chưa thanh toán. Sau refund vẫn giữ lịch sử đã mua.
- SILVER5%, GOLD10%. Ưu đãi theo hạng ở thời điểm backend tạo đơn mới.
- Gắn bó: 10% khi gia hạn. Tính hợp các khoảng ngày sử dụng thực tế của gói đã trả thật, gồm cả ngày đầu/cuối, cắt ở hôm nay; không đếm tương lai/chồng ngày/demo/gói bị hủy/hoàn tiền. Mốc6 tháng là số ngày từ ngày sử dụng hợp lệ đầu tiên đến ngày đó cộng6 tháng lịch, không quy đổi cứng180 ngày. Đơn gia hạn phải cùng plan/branch, gói nguồn đã bắt đầu và đang dùng hoặc hết hạn không quá30 ngày. Gói chỉ được đặt trước cho tương lai không đủ làm nguồn gia hạn gắn bó.
- Giá trị cao: giá gốc từ500.000đ giảm50.000đ; từ1.000.000đ giảm100.000đ; không cộng hai mức.
- Mã: giữ CRUD/DEMO5/DEMO10/DEMO20 hiện có, không seed lại hoặc sửa số lượt hiện có. Thêm tùy chọn giá gốc tối thiểu, trần giảm và lượt/tài khoản. Để trống giữ tính tương thích không giới hạn bổ sung.
- Giá VND nguyên; làm tròn giá sau phần trăm HALF_UP như cơ chế mã trước đây. Giá cuối luôn >0. Backend không nhận amount/discount/tier do client quyết định.

## 3. Giữ lượt, sử dụng, hoàn lượt và snapshot

- Lượt mã bị trừ khi tạo Membership PENDING thành công, cùng transaction. Khóa User → Plan snapshot → Promotion; kiểm tra lượt toàn cục và lượt/tài khoản tại backend. Không trừ lần nữa khi tạo Payment hoặc nhận webhook.
- Các đơn PENDING và đã trả tiền giữ/tính lượt; refund không hoàn lượt mã đã thực sự sử dụng. Lịch sử Membership/Payment là sổ theo dõi, không thêm bảng usage trùng chức năng.
- Chỉ hoàn lượt khi chuyển Membership từ PENDING sang CANCELLED qua các luồng hiện có, chưa có tiền PAID. Cleanup48h chỉ đóng đơn chưa có Payment đang xử lý. Hủy CASH chưa thu có thể ngay; đơn ngân hàng phải qua yêu cầu/duyệt hủy hiện tại.
- **QR hết hạn không tự hoàn lượt**: có thể còn tiền/callback đến muộn. Giữ reservation đến khi đối soát/duyệt hủy. Không thêm scheduler xóa/hủy giao dịch ngân hàng.
- Snapshot Membership gồm original_price, quoted_price, promotion_code, discount_percent (cũ), thêm offer_kind/offer_label/discount_amount/pricing_selection (mới). Đã có đơn chờ thì preview và POST cùng trả giá đã chốt; không tính lại theo giá plan/hạng/chương trình mới.
- Payment.amount vẫn lấy quoted_price, VietQR vẫn lấy Payment.amount. Webhook vẫn kiểm tra HTTPS/HMAC/reference/beneficiary/VA/amount/thời điểm/duplicate như trước, rồi PAID + ACTIVE cùng transaction. Các service SePay xác nhận/đối soát không bị sửa.

## 4. Thứ tự thao tác trong SSMS — CHỈ SAU KHI DUYỆT

1. Backup mới, giữ bản restore thử; kiểm kê các đơn SePay đang mở. Dùng cửa sổ bảo trì/drain theo runbook sẵn có khi triển khai; không sửa snapshot đơn đang mở.
2. DBA chọn đúng **UTEFitDB**, xem rồi chạy `database/migrations/V716__member_loyalty_offers.sql`. Chỉ thêm cột nullable/bảng audit/index/check constraint; không sửa dữ liệu cũ. Giữ `ddl-auto=validate`, không chạy DB_Khung_1.sql/DB_Data_2.sql.
3. **BỎ QUA `database/operations/UTEFitDB_loyalty_02_demo_exclusions.template.sql`**. Không cần chạy, không có bước đánh dấu 21 CASH demo loại trừ. Nếu đã chạy phiên bản cũ, giữ nguyên audit; code mới vẫn cộng CASH PAID hợp lệ vào hạng, không cần DELETE/UPDATE các dòng audit.
4. Chạy `database/operations/UTEFitDB_loyalty_03_runtime_permissions.template.sql` cờ0 để kiểm tra. DBA chỉ sau duyệt đặt `@ApproveAdditionalReadPermission=1` để GRANT SELECT trên bảng audit mới nếu còn thiếu. Code vẫn đọc audit cho điều kiện ưu đãi cũ và SePay. Không thu hồi/sửa grant cũ. Runtime không được INSERT/UPDATE/DELETE bảng audit và vẫn không được ghi guard. Không đánh dấu PASS trước khi xem kết quả quyền hiệu lực.
5. Sau duyệt, chạy `database/migrations/V717__automatic_promotion_management.sql`. Thêm một bảng cấu hình, khởi tạo đủ năm loại với giá trị hiện tại và active=0; không ghi đè cấu hình đã có, không sửa Promotion code, Payment hoặc Membership.
6. Kiểm tra bằng `database/operations/UTEFitDB_loyalty_04_automatic_program_permissions.template.sql` với cờ0. Chỉ sau duyệt mới cấp SELECT/UPDATE trên bảng cấu hình mới nếu thiếu; không cần INSERT/DELETE. Không sửa quyền/role/login hoặc approval cũ.
7. Deploy/restart bản mới sau khi V716/V717 và quyền đã đạt. Giữ nguyên SePay. Kiểm tra startup validate, mã cũ và snapshot đơn đang mở. Năm chương trình mới mặc định tạm dừng, chưa tự thay giá Live.
8. Tại `/admin/promotions`, Admin bật/sửa từng chương trình. **Không cần bật UTEFIT_LOYALTY_ENABLED hoặc restart để chỉnh cấu hình nữa**. DB là nguồn ưu tiên; tắt từng chương trình ngay trong Admin. Xem `ADMIN_AUTOMATIC_PROMOTIONS.md` để thử bằng H2 và rollout được duyệt.

Các điều kiện backup/restore/quyền/tunnel/ngân hàng thật không được công nhận PASS chỉ từ kiểm thử H2. Các SQL Server index/check/grant cần DBA kiểm tra trên bản restore và UTEFitDB trước khi rollout.

V716 đặt SET options cần cho filtered index trong phiên SSMS tạo schema. DBA phải kiểm tra cả phiên JDBC runtime, không lấy kết quả SESSIONPROPERTY trong SSMS để công nhận JDBC đã đạt. Dự án đã dùng filtered indexes ở V706/V707/V710; không đổi connection settings của app trong đợt này. Tham khảo [Microsoft — Required SET options for filtered indexes](https://learn.microsoft.com/en-us/sql/t-sql/statements/create-index-transact-sql?view=sql-server-ver17#required-set-options-for-filtered-indexes).

## 5. Cấu hình và Admin

`LoyaltyProperties` bind `utefit.loyalty.*`; bản tham khảo đủ khóa ở `docs/loyalty.properties.example`. Ngưỡng Bronze/Silver/Gold vẫn từ cấu hình này, không có trường sửa ngưỡng hạng trong Admin. Phần trăm/tháng/ngưỡng VALUE/bật tắt/ngày áp dụng của năm chương trình đọc từ DB V717; properties chỉ fallback khi chưa có dòng cấu hình DB. Không đưa cấu hình loyalty vào file SePay.

Admin Light được giữ nguyên hệ style: bảng chương trình tự động có bật/tắt, trang sửa chuyên biệt từng loại, trạng thái và số đơn PAID/đang giữ có snapshot phù hợp. Có version chống ghi đè khi hai Admin cùng sửa, người/thời điểm sửa, validation backend và CSRF/phân quyền Admin. Các điều kiện mã trong CRUD cũ và số đơn giữ/đã dùng mã được giữ nguyên. Không tạo Promotion code giả hoặc dùng các mã DEMO để đại diện quyền hạng. WELCOME vẫn cố định một lần mỗi tài khoản; điều kiện gia hạn LOYAL cùng plan/branch vẫn được backend xác minh.

## 6. Tự kiểm tra trên web và môi trường cô lập

- Tài khoản không có CASH PAID hợp lệ hoặc SePay LIVE PAID hợp lệ: `/member/profile` thấy BRONZE/tổng0; ba tab/avatar vẫn hoạt động. Tài khoản có CASH demo PAID hiển thị hạng/tổng theo lịch sử đó ngay khi mở profile. `/promotions` không công khai email/tổng chi cho Guest.
- `/member/memberships`: chọn gói, AUTO; xem giá gốc/tên ưu đãi/giảm/giá cuối. Đổi NONE hoặc nhập mã hợp lệ và so giá. Mã sai/hết lượt/hết hạn bị backend từ chối. Preview là GET, không tạo đơn hay giữ lượt. POST mới chốt giá.
- Giá gốc500.000đ: các ưu đãi tự động tốt nhất giảm50.000đ khi đủ điều kiện; 1.000.000đ: giảm100.000đ. Tùy hạng/gắn bó, AUTO có thể chọn quyền lợi bằng hoặc tốt hơn. Nhập mã không cộng dồn.
- Khi tạo đơn, kiểm tra giá trên Membership và QR. PENDING chưa làm tăng tổng/hạng. Thay đổi chương trình/hạng không đổi giá đơn cũ.
- Bronze→Silver→Gold không được chứng minh bằng UPDATE PAID hay xác nhận CASH chưa thu trên database thật. Các bài `MemberLoyaltyIntegrationTest` và `LoyaltySepayIntegrationTest` thực hiện chuyển cấp/paid/webhook trong H2, có mock guard/ngân hàng riêng và không gửi tiền. Trên web Live chỉ dùng tiền thật thực nhận và quy trình xác nhận hợp lệ; ngưỡng tính trên tiền sau giảm, nên đơn gốc500.000đ giảm50.000đ mới cộng450.000đ, chưa tự đủ Silver.
- Với tiền thật, không cố tạo lỗi/hoàn tiền thử trên tài khoản khác. Dùng runbook đối soát hiện có nếu có callback/tiền đến muộn. Các bài concurrency/expiry/refund/idempotency dùng dữ liệu cô lập.
- Bộ ảnh trước/sau và fixture HTML: `target/loyalty-qa/` (ignored). Fixture QA không phải dữ liệu hội viên thật; server xem ảnh chỉ cho GET/HEAD, không nhận POST. Kết quả kiểm thử cuối cùng được ghi riêng tại đó.

### Kiểm tra bằng tài khoản demo sẵn có, không tạo Payment mới

Sau khi schema/quyền/code đã được duyệt và triển khai, đăng nhập lần lượt các email dưới đây bằng mật khẩu hiện có của nhóm, vào `/member/profile` → Tổng quan. Không bấm xác nhận CASH hoặc tạo chuyển khoản chỉ để nâng hạng. Dữ liệu chuẩn từ **source** `database/DB_Data_2.sql` có kết quả:

| Tài khoản | CASH seed được tính | Hạng dự kiến |
|---|---:|---|
| member01@utefit.test | 0đ | BRONZE |
| member02@utefit.test / member03@utefit.test | 300.000đ mỗi người | BRONZE |
| member04@utefit.test | 600.000đ (M04A + M04NEXT đã PAID, kể cả gói bắt đầu tương lai) | SILVER |
| member05@utefit.test | 600.000đ (M05A + M05B) | SILVER |
| member06@utefit.test | 0đ (M06A còn PENDING) | BRONZE |
| member07 đến member18 @utefit.test | 300.000đ mỗi người | BRONZE |
| member19@utefit.test | 2.800.000đ | GOLD |
| member20@utefit.test | 5.600.000đ | GOLD |

21 CASH seed có **20 PAID và 1 PENDING**, không phải toàn bộ 21 đều được cộng. Đây là đối chiếu source seed, chưa phải xác nhận số dư UTEFitDB hiện tại. Nếu tài khoản có giao dịch/hoàn tiền hợp lệ khác, tổng/hạng phải phản ánh lịch sử hiện tại chứ không bị ép về bảng mẫu. Load lại profile không tăng tổng; giá đơn PENDING đã chốt không thay đổi. Nếu đã có audit loại trừ cũ, CASH PAID vẫn được cộng mà không cần sửa audit.

## 7. File code thuộc đợt này

- Mới: `config/LoyaltyProperties.java`, `entity/LoyaltyPaymentExclusion.java`, `repository/LoyaltyPaymentExclusionRepository.java`, `service/MemberLoyaltyService.java`, `service/MembershipOfferService.java`, `service/PromotionCatalogService.java` (đường dẫn gốc `src/main/java/com/utefit`).
- Sửa entity Membership/Promotion và DTO MembershipPurchaseRequest; repository Membership/Promotion/SePayDetails (chỉ thêm query đọc môi trường); MembershipPaymentService (tạo/preview/snapshot giá), PromotionReservationService (refresh dưới lock), AdminPromotionService (validate điều kiện).
- Controller MemberProfile/MemberMembership/PromotionPublic/AdminPromotion: cấp model/quote, không thay route đang có. Thêm GET `/member/memberships/quote` đã bảo vệ bởi quyền Member hiện hành.
- Template member/profile, member/memberships, guest/promotions, admin/promotions/list; CSS utefit-member-profile/utefit-loyalty; JS member-offers. Không sửa JS avatar/profile, shared layout, UI PT hoặc UI khác.
- Test mới MemberLoyaltyIntegrationTest/LoyaltyConcurrencyIntegrationTest/LoyaltySepayIntegrationTest/LoyaltyMigrationContractTest và member-offers.test.cjs. Hai test schema export/handoff được cập nhật để nhận entity mới nhưng vẫn kiểm tra bootstrap cũ nguyên vẹn và V716 triển khai riêng.
- V716 và hai SQL operations nêu trên; tài liệu này và properties example. Chưa commit/push.

## 8. Kết quả kiểm thử triển khai ban đầu

- Compile và toàn bộ Java suite: **BUILD SUCCESS**, 314 test, 0 failure/error, 1 skipped (Cloudinary integration cần cấu hình ngoài, không thực hiện upload thật).
- JavaScript: **103/103 passed**, gồm các test giữ nguyên profile/ảnh và 4 test preview ưu đãi (không gửi amount/CSRF qua GET, lỗi/retry, chống kết quả cũ ghi đè, không chấp nhận giá miễn phí/redirect login).
- H2 có các ca Bronze/Silver/Gold, CASH demo/real, pending/refund, chống cộng trùng, union ngày, điều kiện gia hạn, ưu đãi/mã/cap/lượt, cạnh tranh đồng thời, VietQR giá cuối, HMAC/webhook trùng và các hồi quy hiện có. Chính sách CASH demo của lần triển khai ban đầu đã được thay thế bằng mục 1 ở trên; kết quả kiểm thử bản điều chỉnh được ghi riêng bên dưới.
- Browser: 21 tổ hợp trang/viewport 1440/768/390px không tràn ngang, Gold không có thanh lên hạng giả; 5 nhóm ưu đãi thực và mã hiện hành; chuyển tab giữ draft, header sticky. Preview gói200.000đ: AUTO180.000đ, NONE200.000đ, DEMO5 190.000đ — không cộng dồn.
- Hash audit 61 file bảo vệ (SePay core/config/approval, bootstrap/seed/migration cũ, PT/shared Member layout, JS profile) không đổi. Không phát hiện thay đổi ngoài danh sách cho phép trong baseline kiểm kê.
- Chưa thực thi V716 trên SQL Server thật, chưa kiểm tra quyền hiệu lực/JDBC SET options trên UTEFitDB, chưa thử chuyển khoản thật. Không coi các điều kiện đó là PASS. Dừng chờ duyệt migration/rollout.

Chạy lại các test nghiệp vụ mới bằng PowerShell trong repository (ép database H2, không dùng kết nối thật):

```powershell
.\mvnw.cmd "-Dspring.config.location=file:src/test/resources/application.properties" "-Dspring.datasource.url=jdbc:h2:mem:loyalty_local_qa;MODE=MSSQLServer;DB_CLOSE_DELAY=-1" "-Dspring.datasource.driver-class-name=org.h2.Driver" "-Dspring.datasource.username=sa" "-Dspring.datasource.password=" "-Dspring.profiles.active=" "-DSEPAY_ENABLED=false" "-DSEPAY_LIVE_ENABLED=false" "-DSEPAY_LIVE_ACCEPT_NEW_PAYMENTS=false" "-Dtest=MemberLoyaltyIntegrationTest,LoyaltySepayIntegrationTest,LoyaltyConcurrencyIntegrationTest,LoyaltyMigrationContractTest" test
node --test src/test/js/*.test.cjs
```

## 9. Kiểm thử bản điều chỉnh CASH demo tính hạng — 30/10/2026

- Chỉ sửa `MemberLoyaltyService.java`, `MemberLoyaltyIntegrationTest.java`, `LoyaltyMigrationContractTest.java`, template SQL 02 và tài liệu này. Không sửa V716, entity/repository audit, quyền runtime, controller, frontend hoặc bất kỳ file SePay/báo cáo tài chính nào. Đối chiếu hash 553 file đã kiểm kê: đúng 5 file thay đổi, 548 file còn lại giữ nguyên.
- Compile và hai lượt Maven chọn lọc đều **BUILD SUCCESS**. Tổng 42 test thuộc 5 class liên quan đạt, không failure/error/skipped: MemberLoyaltyIntegrationTest 27, LoyaltyMigrationContractTest 4, PromotionPurchaseIntegrationTest 5, LoyaltyConcurrencyIntegrationTest 3, LoyaltySepayIntegrationTest 3. Log tại `target/loyalty-qa/cash-policy-first-tests.log` và `target/loyalty-qa/new-tests.log`; số liệu từng class tại `target/loyalty-qa/cash-policy-tests.json` (ignored).
- Bổ sung kiểm tra profile đọc trực tiếp lịch sử CASH seed cho Bronze/Silver/Gold và PENDING, không tạo thêm đơn/thanh toán; audit cũ không chặn hạng CASH; REFUNDED/FAILED/CANCELLED/CANCEL_REQUESTED không cộng; thiếu paid_at không cộng; cùng reference không tăng trùng; cờ ưu đãi tự động tắt vẫn tính hạng; seed không tự tạo ngày sử dụng gắn bó hoặc bằng chứng ngân hàng.
- Các kiểm thử cũ vẫn xác nhận một ưu đãi/đơn, snapshot không đổi, hạn/lượt mã, khóa đồng thời, VietQR giá cuối và HMAC/PAID/ACTIVE/callback trùng. Toàn bộ JavaScript hiện có chạy lại: **103/103 đạt**. `git diff --check` cho file sửa không lỗi. Không cần sửa hay chụp lại UI trong đợt chỉ đổi chính sách tính hạng này.
- Chưa kết nối/ghi UTEFitDB, chưa chạy SQL hoặc thử ngân hàng thật, chưa xác nhận tổng từng tài khoản trên database đang chạy. Không tạo PAID giả trên database thật. Nhánh main, chưa commit/push; dừng chờ duyệt schema/quyền/triển khai nếu chưa hoàn tất.

## 10. Nâng cấp quản lý chương trình động trên Admin

Đã thay bảng readonly bằng quản lý DB cho5loại; cấu hình động thắng properties, không cần cờ tổng/restart sau mỗi lần sửa. V717 và template quyền04 cần duyệt trước khi triển khai; mặc định5loại tạm dừng. Hướng dẫn, toàn bộ file, validation và kết quả nằm tại `ADMIN_AUTOMATIC_PROMOTIONS.md`.

Kiểm thử cuối bản quản lý động: compile/toàn bộ Java337test không failure/error,1skipped Cloudinary; JS106/106; browser18layout Admin +3Public không tràn ngang, bật/tắt/sửa và preview chạy qua app thật trên H2 cô lập. Cấu hình đã lưu được giữ qua hai lần khởi động Spring Boot trong test. Không có migration/GRANT/activation hoặc thanh toán thật trên UTEFitDB.
