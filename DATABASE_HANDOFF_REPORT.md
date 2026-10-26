# Báo cáo bàn giao database và Git UTEFit — 26/10/2026

## 1. Đã thực hiện

Chỉ chỉnh source/script/tài liệu và kiểm thử H2. Không commit, stage, push, merge hoặc
reset database. Nhánh vẫn feat/giai-doan-7; HEAD 2dadc44, a8798f7 vẫn là ancestor.
Không đọc/copy giao dịch ngân hàng; không cập nhật Payment #1/#2 hoặc Membership thật.

Hai file SQL hiện có đã được cập nhật, không tạo baseline SQL thay thế:
- database/DB_Khung_1.sql: schema sạch 28 bảng, core JPA được đối chiếu và nhúng
  V701–V712 nguyên nội dung, FK/CHECK/UNIQUE/index và chốt reset kép mặc định tắt.
- database/DB_Data_2.sql: 55 tài khoản, dữ liệu có quan hệ, CASH DEMO, transaction,
  khóa seed và marker idempotency; không tạo Live approval hay lịch sử webhook giả.

## 2. Metadata thực tế — chỉ đọc

Đọc thành công bằng Windows authentication sau khi thử kết nối chỉ đọc.
Chỉ SELECT sys.tables/sys.columns/sys.types/sys.foreign_keys/sys.check_constraints/sys.indexes.
Bản metadata đầy đủ nằm trong target/database-schema-audit.json (ignored, không push).

| Database | Bảng | Cột | FK | CHECK | Index |
|---|---:|---:|---:|---:|---:|
| UTEFitDB | 22 | 183 | 33 | 12 | 33 |
| UTEFitLiveDemo | 28 | 256 | 44 | 34 | 50 |
| UTEFitQA_GD7_20261025 | 24 | 212 | 36 | 18 | 42 |

Không suy ra dữ liệu nghiệp vụ trùng nhau từ metadata. Không đọc số tiền hoặc số dư.
Đối chiếu tập tên bảng/cột với DDL xuất từ JPA hiện tại + deployment guard: chuẩn có
256 cột. UTEFitDB thiếu 73 cột, LiveDemo không thiếu/thừa tên cột, QA thiếu 44 cột;
không có cột ngoài tập chuẩn ở cả ba DB. Không thay thế kiểm tra semantic của constraint.

### UTEFitDB thiếu so với LiveDemo

Bảng: membership_branch_snapshot, membership_plan_branches,
sepay_bank_reconciliations, sepay_deployment_guard, sepay_payment_details,
sepay_webhook_transactions.

Cột trong các bảng đã có:
- class_categories.required_benefit.
- membership_plans: benefit, access_scope, duration_unit, duration_quantity,
  bonus_unit, bonus_quantity.
- memberships: quoted_benefit, quoted_scope, renewal_of_id, quoted_duration_unit,
  quoted_duration_quantity, quoted_bonus_unit, quoted_bonus_quantity.
- qr_checkin_tokens: purpose, checkin_id.

Tương ứng các phần còn thiếu của V706–V712; việc nâng cấp DB có dữ liệu cần kiểm tra
từng prerequisite/index/constraint, không chạy lặp mù các version đã có.

### QA thiếu so với LiveDemo

Bảng: membership_branch_snapshot, membership_plan_branches,
sepay_bank_reconciliations, sepay_deployment_guard.

Cột: required_benefit của category; benefit/scope/calendar của plan;
quoted_benefit/scope/renewal/calendar của memberships; sepay_payment_details.va_number;
sepay_webhook_transactions.sub_account. Không thay đổi QA.

Với các cột có mặt ở cả hai DB so với LiveDemo, không phát hiện khác biệt type,
max_length hoặc nullable trong truy vấn so sánh. Đây không khẳng định toàn bộ
constraint/index hoặc dữ liệu đồng nhất.

## 3. Schema và dữ liệu demo dự kiến

28 bảng gồm 25 entity JPA, 2 collection tables và sepay_deployment_guard.
Statistics là aggregation từ Payment/Membership/Checkin, không thêm bảng trùng nghiệp vụ.

55 users: ADMIN10 + MEMBER20 + PT20 + MANAGER5, email liệt kê đầy đủ trong README.
Mật khẩu demo 123456789 chỉ dùng phát triển; SQL chứa BCrypt từ PasswordEncoder thật,
không có password plaintext trong cột. Không tạo OTP giả; ACTIVE/enabled đúng mô hình
xác thực đăng ký hiện hành. Chính sách tài khoản mới/Live vẫn giữ nguyên.

Dữ liệu dự kiến theo script (chưa execute cả seed trên SQL Server):
- 5 branches, 5 manager assignments, 20 trainer profiles.
- 55 fixed-branch plans (11 mẫu x 5).
- 21 Membership periods và 21 CASH demo Payments, trong đó 1 PENDING.
- 3 categories, 15 classes, 31 schedules, 4 group registrations.
- 19 checkins (18 đóng, 1 mở), 18 reviews (17 public, 1 hidden).
- 2 accepted PT requests, 2 PT sessions, 1 conversation, 2 messages,
  3 favorites, 55 notifications, 3 promotions.
- Không có SePay details/webhook/reconciliation/approval/OTP/QR token được seed.

Ngày hiện tại lấy theo múi giờ Việt Nam; kỳ dùng phép cộng lịch, các khoản CASH cũ
có paid_at/created_at tương ứng ngày bắt đầu; kỳ future trả trước không bị dùng sớm.
Rerun không kéo dài lịch sử. Các gói năm giữ một số dữ liệu khả dụng dài hạn.

## 4. Kiểm thử thực tế

- Maven compile + bộ regression: **261 tests PASS**, 0 failed/error/skipped.
- Node: **42 tests PASS**.
- Block INSERT users chính xác từ DB_Data_2.sql chạy trên H2 schema DBO:
  đúng 55 tài khoản; BCrypt matches và authenticate cho tất cả 55.
- MockMvc login + JWT cookie + dashboard thực tế cho ADMIN/MEMBER/PT/MANAGER PASS.
- Cả 5 Manager kiểm tra đúng chi nhánh và từ chối ID chi nhánh khác PASS.
- User UNVERIFIED/disabled bị từ chối; không tắt OTP PASS.
- Metadata JPA/embedded migrations và cấu hình portable PASS.
- ScriptDom TSql160 (có sẵn trong SSMS22): hai script + 14 phần SQL nhúng,
  **16 đơn vị parse, 0 lỗi cú pháp**. Không execute SQL.
- Regression Membership calendar/queue, future booking, SePay/CASH/manual,
  QR check-in/out, Review, Manager, Favorites PASS trên H2/mocks.

Log local ignored: target/database-handoff-final-tests.log,
target/database-handoff-js-tests.log.

**NOT TESTED/BLOCKED:** execute reset/schema/seed trên SQL Server sạch được phép,
thứ tự DDL thực tế, FK/CHECK/UNIQUE với toàn seed, rerun seed thực tế,
DBCC CHECKCONSTRAINTS, startup validate với UTEFitDB mới và E2E trình duyệt/SMTP/
Cloudinary/ngân hàng thật. Không báo PASS chỉ vì ScriptDom hoặc H2 đã qua.
Chưa được người dùng phê duyệt reset hoặc tạo database thử SQL Server cho tác vụ này.

## 5. Cấu hình và secret

Shared application.properties không còn credential literal, conflict marker hoặc
truststore máy riêng; default UTEFitDB, ddl-auto validate, sql init never.
Không cần SMTP/Cloudinary/ngân hàng để login tài khoản demo.

Bản local cũ được sao lưu nguyên byte ở .local/application-local.properties và được
optional import. Nó bị ignore; không đưa vào Git. Cấu hình Live/environment trên máy
tác giả không bị ghi đè; ứng dụng Live không được restart hay kiểm thử ở đây.
Cần Run configuration development riêng để tác giả chuyển sang UTEFitDB có chủ đích.

Đã rà soát literal trong config/example đang track và một phần lịch sử application.properties.
Phát hiện cần xử lý riêng trước public/push:
- b770fc3: literal Cloudinary API key/secret.
- af94c8d: literal Cloudinary API key/secret, mail password, JWT secret cần rà soát.
- 49bd4e9: literal JWT secret cần rà soát.

Không hiển thị giá trị. Không khẳng định khóa cũ đã bị thu hồi hoặc mọi secret đã được
tìm hết. Chủ tài khoản phải revoke/rotate credential còn hiệu lực, kiểm tra toàn lịch sử,
và phối hợp nhóm nếu cần xử lý history. Chưa rewrite/force push.
Thêm .gitignore/xóa secret khỏi bản mới không giải quyết lịch sử đã commit.

## 6. File của tác vụ này

Sửa: .gitignore, README.md, database/DB_Khung_1.sql, database/DB_Data_2.sql,
src/main/resources/application.properties.

Tạo: .env.example, DATABASE_HANDOFF_REPORT.md,
src/test/java/com/utefit/service/DatabaseHandoffTest.java,
src/test/java/com/utefit/service/PortableConfigurationTest.java,
src/test/java/com/utefit/service/DemoPasswordFixtureTest.java.

Local-only: .local/application-local.properties (giữ cấu hình trước đó).
Không xóa file code của nhóm, SQL legacy, migration hoặc build cache đang dùng.
Các SQL legacy được đánh dấu không thuộc quy trình mới trong README; chưa được
chứng nhận an toàn để chạy. Chỉ hai file DB_Khung_1/DB_Data_2 là entrypoint chuẩn.

## 7. Trước khi push/merge

**HOLD:** cần kiểm thử SQL Server sạch và xử lý lịch sử credential trước khi public.
Không stage toàn bộ bằng git add .; không thêm docs/AI/local env/dump/binary theo quán tính.

Quy trình chi tiết dựng SSMS, biến môi trường, 55 tài khoản, demo bốn role và PR vào main
nằm trong README. Chưa tự commit/push/merge, các checkpoint cũ giữ nguyên.

### Danh sách ứng viên cần review rồi mới stage

Đây là toàn bộ source/config mẫu/migration/tài liệu còn pending từ nhiều tác vụ,
KHÔNG phải tuyên bố đã kiểm tra mọi file là không có secret. docs/ được giữ riêng
để người phụ trách tự xét bản quyền/PII. Chỉ chọn file tương ứng feature đã review.

```text
.env.example
.env.sepay-live.example
.gitignore
MEMBERSHIP_SIMPLE_MODEL.md
MEMBERSHIP_UPGRADE.md
README.md
SEPAY_BIDV_VA.md
SEPAY_CHECKOUT_UI.md
SEPAY_INTEGRATION.md
SEPAY_LIVE_CHECKLIST.md
SEPAY_LIVE_LAPTOP_THUC_TE.md
SEPAY_LIVE_READINESS.md
SEPAY_MANUAL_RECONCILIATION.md
SEPAY_PRELIVE_VERIFICATION.md
database/DB_Data_2.sql
database/DB_Khung_1.sql
database/live-demo/00_create_database.sql
database/live-demo/01_schema.sql
database/live-demo/02_apply_gd7.sql
database/live-demo/03_catalog.template.sql
database/live-demo/04_verify_readonly.sql
database/live-demo/05_runtime_permissions.template.sql
database/live-demo/06_approval.template.sql
database/live-demo/07_backup.template.sql
database/live-demo/08_promote_admin.template.sql
database/migrations/V708__sepay_live_guard.sql
database/migrations/V709__sepay_virtual_account.sql
database/migrations/V710__sepay_manual_bank_reconciliation.sql
database/migrations/V711__membership_entitlements_and_renewals.sql
database/migrations/V712__membership_calendar_terms.sql
src/main/java/com/utefit/config/SepayProperties.java
src/main/java/com/utefit/controller/admin/AdminMembershipPlanController.java
src/main/java/com/utefit/controller/admin/SepayReconciliationController.java
src/main/java/com/utefit/controller/member/MemberMembershipController.java
src/main/java/com/utefit/controller/payment/SepayWebhookController.java
src/main/java/com/utefit/dto/admin/ClassCategoryFormDto.java
src/main/java/com/utefit/dto/admin/MembershipPlanFormDto.java
src/main/java/com/utefit/entity/ClassCategory.java
src/main/java/com/utefit/entity/Membership.java
src/main/java/com/utefit/entity/MembershipPlan.java
src/main/java/com/utefit/entity/SepayBankReconciliation.java
src/main/java/com/utefit/entity/SepayPaymentDetails.java
src/main/java/com/utefit/entity/SepayWebhookTransaction.java
src/main/java/com/utefit/enums/MembershipBenefit.java
src/main/java/com/utefit/enums/MembershipDurationUnit.java
src/main/java/com/utefit/enums/MembershipScope.java
src/main/java/com/utefit/repository/ClassRegistrationRepository.java
src/main/java/com/utefit/repository/MembershipPlanRepository.java
src/main/java/com/utefit/repository/MembershipRepository.java
src/main/java/com/utefit/repository/PersonalTrainingScheduleRepository.java
src/main/java/com/utefit/repository/SepayBankReconciliationRepository.java
src/main/java/com/utefit/repository/SepayPaymentDetailsRepository.java
src/main/java/com/utefit/repository/SepayWebhookTransactionRepository.java
src/main/java/com/utefit/service/AdminCheckinService.java
src/main/java/com/utefit/service/MemberClassBookingService.java
src/main/java/com/utefit/service/MembershipBookingPolicyScheduler.java
src/main/java/com/utefit/service/MembershipBookingPolicyService.java
src/main/java/com/utefit/service/MembershipEligibilityService.java
src/main/java/com/utefit/service/MembershipPaymentService.java
src/main/java/com/utefit/service/PublicCatalogService.java
src/main/java/com/utefit/service/SepayDatabaseGuard.java
src/main/java/com/utefit/service/SepayManualReconciliationService.java
src/main/java/com/utefit/service/SepayPaymentService.java
src/main/java/com/utefit/service/SepayReconciliationLookup.java
src/main/java/com/utefit/service/SepayWebhookPayload.java
src/main/java/com/utefit/service/SepayWebhookService.java
src/main/java/com/utefit/service/VnpayPaymentService.java
src/main/java/com/utefit/service/impl/AdminCategoryServiceImpl.java
src/main/java/com/utefit/service/impl/AdminMembershipPlanServiceImpl.java
src/main/java/com/utefit/service/impl/PersonalTrainingScheduleServiceImpl.java
src/main/java/com/utefit/util/MembershipTerm.java
src/main/resources/application-sepay-live.properties
src/main/resources/application.properties
src/main/resources/static/css/sepay-checkout.css
src/main/resources/static/js/sepay-checkout.js
src/main/resources/templates/admin/categories/form.html
src/main/resources/templates/admin/payments/detail.html
src/main/resources/templates/admin/payments/sepay-manual-refund.html
src/main/resources/templates/admin/payments/sepay-manual.html
src/main/resources/templates/admin/payments/sepay-reconciliation.html
src/main/resources/templates/admin/plans/form.html
src/main/resources/templates/admin/plans/list.html
src/main/resources/templates/guest/home.html
src/main/resources/templates/guest/membership-plans.html
src/main/resources/templates/member/memberships.html
src/main/resources/templates/member/sepay-checkout.html
src/test/java/com/utefit/service/AdminPlanBranchScopeIntegrationTest.java
src/test/java/com/utefit/service/CashPaymentRegressionTest.java
src/test/java/com/utefit/service/DatabaseHandoffTest.java
src/test/java/com/utefit/service/DemoPasswordFixtureTest.java
src/test/java/com/utefit/service/LiveDemoSchemaPreparationTest.java
src/test/java/com/utefit/service/LiveTunnelSecurityTest.java
src/test/java/com/utefit/service/MembershipTermTest.java
src/test/java/com/utefit/service/MembershipUpgradeIntegrationTest.java
src/test/java/com/utefit/service/PortableConfigurationTest.java
src/test/java/com/utefit/service/SepayDatabaseGuardTest.java
src/test/java/com/utefit/service/SepayLiveSafetyTest.java
src/test/java/com/utefit/service/SepayManualReconciliationIntegrationTest.java
src/test/java/com/utefit/service/SepayPaymentIntegrationTest.java
src/test/java/com/utefit/service/SepayReconciliationLookupTest.java
src/test/java/com/utefit/service/SepayWebhookSecurityTest.java
src/test/java/com/utefit/service/VnpayConcurrencyIntegrationTest.java
src/test/java/com/utefit/service/VnpayPaymentServiceTest.java
src/test/js/sepay-checkout.test.cjs
DATABASE_HANDOFF_REPORT.md
```

### Git status tại lúc lập báo cáo

```text
M .gitignore
 M README.md
 M SEPAY_INTEGRATION.md
 M SEPAY_LIVE_CHECKLIST.md
 M database/DB_Data_2.sql
 M database/DB_Khung_1.sql
 M src/main/java/com/utefit/config/SepayProperties.java
 M src/main/java/com/utefit/controller/admin/AdminMembershipPlanController.java
 M src/main/java/com/utefit/controller/admin/SepayReconciliationController.java
 M src/main/java/com/utefit/controller/member/MemberMembershipController.java
 M src/main/java/com/utefit/controller/payment/SepayWebhookController.java
 M src/main/java/com/utefit/dto/admin/ClassCategoryFormDto.java
 M src/main/java/com/utefit/dto/admin/MembershipPlanFormDto.java
 M src/main/java/com/utefit/entity/ClassCategory.java
 M src/main/java/com/utefit/entity/Membership.java
 M src/main/java/com/utefit/entity/MembershipPlan.java
 M src/main/java/com/utefit/entity/SepayPaymentDetails.java
 M src/main/java/com/utefit/entity/SepayWebhookTransaction.java
 M src/main/java/com/utefit/repository/ClassRegistrationRepository.java
 M src/main/java/com/utefit/repository/MembershipPlanRepository.java
 M src/main/java/com/utefit/repository/MembershipRepository.java
 M src/main/java/com/utefit/repository/PersonalTrainingScheduleRepository.java
 M src/main/java/com/utefit/repository/SepayPaymentDetailsRepository.java
 M src/main/java/com/utefit/repository/SepayWebhookTransactionRepository.java
 M src/main/java/com/utefit/service/AdminCheckinService.java
 M src/main/java/com/utefit/service/MemberClassBookingService.java
 M src/main/java/com/utefit/service/MembershipEligibilityService.java
 M src/main/java/com/utefit/service/MembershipPaymentService.java
 M src/main/java/com/utefit/service/PublicCatalogService.java
 M src/main/java/com/utefit/service/SepayDatabaseGuard.java
 M src/main/java/com/utefit/service/SepayPaymentService.java
 M src/main/java/com/utefit/service/SepayWebhookPayload.java
 M src/main/java/com/utefit/service/SepayWebhookService.java
 M src/main/java/com/utefit/service/VnpayPaymentService.java
 M src/main/java/com/utefit/service/impl/AdminCategoryServiceImpl.java
 M src/main/java/com/utefit/service/impl/AdminMembershipPlanServiceImpl.java
 M src/main/java/com/utefit/service/impl/PersonalTrainingScheduleServiceImpl.java
 M src/main/resources/application.properties
 M src/main/resources/static/js/sepay-checkout.js
 M src/main/resources/templates/admin/categories/form.html
 M src/main/resources/templates/admin/payments/detail.html
 M src/main/resources/templates/admin/payments/sepay-reconciliation.html
 M src/main/resources/templates/admin/plans/form.html
 M src/main/resources/templates/admin/plans/list.html
 M src/main/resources/templates/guest/home.html
 M src/main/resources/templates/guest/membership-plans.html
 M src/main/resources/templates/member/memberships.html
 M src/main/resources/templates/member/sepay-checkout.html
 M src/test/java/com/utefit/service/CashPaymentRegressionTest.java
 M src/test/java/com/utefit/service/SepayWebhookSecurityTest.java
 M src/test/java/com/utefit/service/VnpayConcurrencyIntegrationTest.java
 M src/test/java/com/utefit/service/VnpayPaymentServiceTest.java
?? .env.example
?? .env.sepay-live.example
?? MEMBERSHIP_SIMPLE_MODEL.md
?? MEMBERSHIP_UPGRADE.md
?? SEPAY_BIDV_VA.md
?? SEPAY_CHECKOUT_UI.md
?? SEPAY_LIVE_LAPTOP_THUC_TE.md
?? SEPAY_LIVE_READINESS.md
?? SEPAY_MANUAL_RECONCILIATION.md
?? SEPAY_PRELIVE_VERIFICATION.md
?? database/live-demo/
?? database/migrations/V708__sepay_live_guard.sql
?? database/migrations/V709__sepay_virtual_account.sql
?? database/migrations/V710__sepay_manual_bank_reconciliation.sql
?? database/migrations/V711__membership_entitlements_and_renewals.sql
?? database/migrations/V712__membership_calendar_terms.sql
?? docs/
?? src/main/java/com/utefit/entity/SepayBankReconciliation.java
?? src/main/java/com/utefit/enums/MembershipBenefit.java
?? src/main/java/com/utefit/enums/MembershipDurationUnit.java
?? src/main/java/com/utefit/enums/MembershipScope.java
?? src/main/java/com/utefit/repository/SepayBankReconciliationRepository.java
?? src/main/java/com/utefit/service/MembershipBookingPolicyScheduler.java
?? src/main/java/com/utefit/service/MembershipBookingPolicyService.java
?? src/main/java/com/utefit/service/SepayManualReconciliationService.java
?? src/main/java/com/utefit/service/SepayReconciliationLookup.java
?? src/main/java/com/utefit/util/
?? src/main/resources/application-sepay-live.properties
?? src/main/resources/static/css/sepay-checkout.css
?? src/main/resources/templates/admin/payments/sepay-manual-refund.html
?? src/main/resources/templates/admin/payments/sepay-manual.html
?? src/test/java/com/utefit/service/AdminPlanBranchScopeIntegrationTest.java
?? src/test/java/com/utefit/service/DatabaseHandoffTest.java
?? src/test/java/com/utefit/service/DemoPasswordFixtureTest.java
?? src/test/java/com/utefit/service/LiveDemoSchemaPreparationTest.java
?? src/test/java/com/utefit/service/LiveTunnelSecurityTest.java
?? src/test/java/com/utefit/service/MembershipTermTest.java
?? src/test/java/com/utefit/service/MembershipUpgradeIntegrationTest.java
?? src/test/java/com/utefit/service/PortableConfigurationTest.java
?? src/test/java/com/utefit/service/SepayDatabaseGuardTest.java
?? src/test/java/com/utefit/service/SepayLiveSafetyTest.java
?? src/test/java/com/utefit/service/SepayManualReconciliationIntegrationTest.java
?? src/test/java/com/utefit/service/SepayPaymentIntegrationTest.java
?? src/test/java/com/utefit/service/SepayReconciliationLookupTest.java
?? src/test/js/sepay-checkout.test.cjs
```
