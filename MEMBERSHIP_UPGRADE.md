# Nâng cấp Membership: gia hạn, quyền lợi và nhiều chi nhánh

> Phần mở bán MULTI/ALL và xếp kỳ theo quyền lợi trong tài liệu này đã được thay
> bằng mô hình một chi nhánh + thời hạn lịch tại `MEMBERSHIP_SIMPLE_MODEL.md`.
> V711/snapshot cũ vẫn được giữ để bảo toàn lịch sử; triển khai mới cần đọc V712.

## 1. Phạm vi và an toàn

Triển khai trên `feat/giai-doan-7`, HEAD vẫn `2dadc44`, giữ commit QR `a8798f7` và các thay đổi SePay chưa commit. Không commit/push/merge. Không khởi động ứng dụng Live, không kết nối SQL Server, không chạy migration hay tác động Payment #1 / Membership #2. Không sửa `application.properties`, secret, tài khoản nhận, VA, HMAC, approval hoặc công tắc `SEPAY_LIVE_ACCEPT_NEW_PAYMENTS`.

Thay đổi mở rộng module Membership/Payment, Plan, Category, Booking, Check-in và đối soát hiện có; không tạo module thanh toán hoặc lịch sử Membership thứ hai.

## 2. Mô hình và quy tắc

### Gia hạn bằng các kỳ có lịch sử riêng

- Mỗi lần mua/gia hạn là một Membership PENDING có snapshot giá, số ngày, quyền lợi và phạm vi chi nhánh. Nút Gia hạn tạo/liên kết kỳ mới bằng `renewal_of_id`, không sửa ngày hoặc giao dịch của kỳ cũ.
- PENDING/REVIEW/FAILED không cung cấp quyền sử dụng, không cộng ngày và không giữ slot chính thức.
- Chỉ sau PAID, backend nối kỳ cùng quyền lợi/phạm vi sau ngày cuối đã thanh toán: kỳ mới bắt đầu `lastEnd + 1`, kết thúc `start + durationDays - 1` (ngày cuối bao gồm toàn ngày). Hết hạn/không có kỳ tương đương: bắt đầu ngày ghi nhận thanh toán theo quy tắc hiện tại.
- Ví dụ kỳ đến 24/11/2026, mua thêm 30 ngày: kỳ gia hạn 25/11–24/12/2026. Kỳ cũ vẫn có ngày cuối 24/11 trong lịch sử; giao diện hiển thị quyền lợi liên tục đã thanh toán đến 24/12. Không có hai kỳ tương đương chồng ngày do gia hạn.
- Các kỳ tương lai đã PAID vẫn mang ACTIVE nhưng chưa sử dụng trước `startDate`; giao diện ghi rõ ngày bắt đầu. ACTIVE không có nghĩa được vào tập ngay nếu chưa tới ngày bắt đầu.
- Khóa User → Membership → Payment cho CASH, SePay, đối soát và callback VNPay lịch sử. Các thanh toán cùng Member được tuần tự hóa; callback trùng vẫn không kích hoạt lại kỳ đã ACTIVE. Hai đơn khác nhau cùng loại được nối theo thứ tự xác nhận PAID, không mất ngày.
- Tạo lại đơn cùng plan/phạm vi khi còn một PENDING tương đương sẽ dùng lại PENDING đó, không đổi snapshot/giá/mã giao dịch cũ. Không đổi đơn CASH sang SePay hoặc ngược lại. Muốn tạo đơn khác phải xử lý an toàn đơn cũ đang chờ đối soát.

### Quyền lợi và phạm vi

- Quyền lợi: `GYM`, `YOGA`, `PT`, `ALL`. ALL là toàn bộ quyền lợi; khác với ALL_BRANCHES là phạm vi chi nhánh.
- SINGLE_BRANCH: một chi nhánh chọn lúc mua. Admin có thể cố định một chi nhánh, hoặc để Member chọn một chi nhánh đang hoạt động.
- MULTI_BRANCH: Admin cấu hình tập chi nhánh; Member chọn chi nhánh gốc trong tập đó, được sử dụng tại toàn bộ tập đã snapshot.
- ALL_BRANCHES: mọi chi nhánh đang hoạt động, kể cả chi nhánh mới mở sau này.
- Các kỳ chỉ cộng nối khi cùng quyền lợi và tập phạm vi tương đương: SINGLE A và MULTI chỉ có A tương đương; ALL_BRANCHES không tương đương tập MULTI hữu hạn vì quyền sử dụng các chi nhánh mở thêm khác nhau. Gym và Yoga không cộng chung hoặc cấp nhầm quyền.
- Chỉnh Plan chỉ ảnh hưởng đơn mới. Các cột quoted_* và bảng `membership_branch_snapshot` giữ quyền đã mua. Nếu quyền lợi/phạm vi Plan đã thay đổi, nút gia hạn gói cũ báo rõ và yêu cầu mua gói mới, không âm thầm đổi quyền cũ.
- Legacy có quoted_scope NULL: giữ SINGLE_BRANCH và quyền lợi ALL theo nghiệp vụ cũ, không suy diễn theo Plan vừa bị chỉnh. Kỳ legacy ACTIVE không có Payment được giữ tương thích; nếu đã có Payment thì phải có PAID, không cho Payment FAILED/REFUNDED cấp quyền.

### Lớp tập và PT tương lai

- Admin đặt `requiredBenefit` tại Category hiện có; lớp thuộc Category dùng quyền đó. Để trống giữ tương thích loại lớp cũ (chấp nhận mọi loại gói đã thanh toán đúng phạm vi/ngày). Muốn phân biệt Gym/Yoga/PT phải chọn rõ quyền lợi tại Category, không đoán từ tên lớp.
- Quyền được kiểm tra tại ngày diễn ra lớp: `startDate <= classDate <= endDate`, đúng chi nhánh đang hoạt động và đúng quyền lợi. Không còn yêu cầu chính kỳ đó phải đã bắt đầu ngay hôm nay. Do đó kỳ PAID 25/11–24/12 cho phép đặt lớp 30/11 từ tháng 10.
- Mỗi quyền lợi phải được một Membership cụ thể đáp ứng cùng lúc; không ghép quyền Gym của gói ở A với phạm vi Yoga của gói ở B để cấp Gym ở B.
- Giữ khóa Schedule khi đếm slot/ghi đăng ký, unique Member+Schedule và kiểm tra trùng giờ. Cập nhật lý do chưa đặt được trên trang Member: thiếu quyền lợi/chi nhánh/ngày hoặc gia hạn chưa thanh toán.
- Lịch PT cá nhân 1:1 cũng yêu cầu PT hoặc ALL tại đúng chi nhánh và ngày hẹn. Gửi PT Request không tự tạo quyền tập hoặc giữ chỗ.
- Tạo lịch 1:1 khóa Member trước rồi PT, thống nhất với luồng thanh toán/thu hồi quyền giữ khóa Member; vẫn tuần tự hóa các lịch cùng PT để chặn trùng giờ.

### Check-in/check-out

- Check-in thủ công và QR dùng chung MembershipEligibilityService: có ít nhất một gói đã thanh toán và bao phủ ngày hôm nay tại chi nhánh đang hoạt động. Kỳ gia hạn chưa bắt đầu không dùng để vào tập sớm.
- Được vào cơ sở để sử dụng gói hợp lệ (Gym/Yoga/PT); quyền đặt từng loại lớp được kiểm tra riêng.
- Không sửa camera, TTL 120 giây, token một lần, tokenVersion hoặc chống replay. Check-out giữ ràng buộc lượt check-in gốc và chi nhánh đã vào; không chặn việc ra về vì gói hết hạn/thu hồi trong lúc đang tập. Manager vẫn phải đúng phạm vi quản lý; tài khoản Member bị khóa dùng checkout thủ công đã có tại quầy theo chính sách hiện tại.

### Hoàn tiền và thu hồi chỗ tương lai

- Màn hình đối soát hoàn tiền riêng mở rộng cho PENDING/VERIFIED và PAID có audit APPLIED hoặc AUTHENTICATED_WEBHOOK/WEBHOOK. Admin phải kiểm tra khoản hoàn đủ tiền thực tế đã hoàn tất, mã hoàn riêng, thời gian, đúng người chuyển gốc và bằng chứng ngân hàng. Không tạo lệnh chuyển tiền, không tự hoàn tiền.
- Hoàn xong: Payment REFUNDED, chính kỳ Membership đó CANCELLED, giữ nguyên ngày và lịch sử. Kiểm tra lại lớp/PT tương lai trong cùng transaction; nếu kỳ khác vẫn bao phủ đúng ngày/loại/chi nhánh thì giữ chỗ. Nếu không, hủy đăng ký/lịch tương lai và tạo thông báo Member; lịch PT cá nhân thông báo cả PT. Không sửa lịch đã diễn ra.
- Không dồn lại hoặc tự chuyển ngày các kỳ đã thanh toán phía sau kỳ bị thu hồi. Nếu có khoảng trống quyền lợi thì giữ đúng khoảng trống, hủy chỗ rơi vào khoảng trống; không tặng ngày hoặc dịch lịch sử tài chính.
- Không có nút hủy tùy tiện kỳ PAID mà không xác nhận hoàn thực tế trong luồng này. Refund CASH hoặc PAID legacy không có audit ngân hàng không được giả lập thành SePay; cần quy trình tài chính phù hợp bên ngoài phạm vi xác nhận ngân hàng hiện có.
- Bộ kiểm tra nền mỗi 60 giây kiểm tra các Member có lịch tương lai, xử lý thay đổi quyền lợi/Payment/Membership từ quy trình hợp lệ khác. Dùng cờ scheduling hiện có `utefit.scheduling.enabled`; test tắt cờ này. Lỗi một Member được log bằng ID/loại lỗi và thử lại vòng sau, không chặn các Member khác.

## 3. Migration SSMS — chỉ người quản trị tự áp dụng sau phê duyệt

File: `database/migrations/V711__membership_entitlements_and_renewals.sql`.

1. Dừng ứng dụng demo; giữ nhận đơn mới tắt. Backup đúng database Live đang dùng theo quy trình hiện tại. Không tạo database mới hoặc reset dữ liệu.
2. Trong SSMS chọn đúng database dự kiến. Chạy truy vấn chỉ đọc `SELECT DB_NAME() AS CurrentDatabase;` và kiểm tra V710/bảng `dbo.sepay_bank_reconciliations` đã tồn tại. Không chạy trên UTEFitDB chính hoặc nhầm QA với tiền thật.
3. Rà soát V711 rồi chỉ chạy sau khi được phê duyệt. Script transaction + XACT_ABORT, thêm nullable columns và hai bảng liên kết, không có INSERT/UPDATE/DELETE dữ liệu nghiệp vụ. Không sửa V701–V710.
4. Bổ sung:
   - membership_plans: benefit, access_scope.
   - memberships: quoted_benefit, quoted_scope, renewal_of_id (self FK).
   - class_categories: required_benefit.
   - membership_plan_branches và membership_branch_snapshot: PK theo owner+branch, FK giữ toàn vẹn dữ liệu.
   - CHECK enum và index tra quyền theo Member/status/ngày hết hạn.
5. Script không cập nhật Payment #1, Membership #2, ngày/khoản tiền cũ, approval, HMAC, VA hay cấu hình. Legacy NULL được xử lý tương thích bởi code, không backfill dữ liệu tài chính.
6. Nếu script báo lỗi: không chạy lại mù quáng. Trong chính session kiểm tra `SELECT XACT_STATE(), @@TRANCOUNT;`; nếu còn transaction chưa hoàn tất thì rollback session đó, kiểm tra bảng/cột/index trước khi tiếp tục. Không sửa trạng thái Payment bằng SQL để khắc phục.
7. Khởi động bằng cấu hình Live/profile hiện tại với `ddl-auto=validate` sau khi schema sẵn sàng; không đổi sang auto-update/create. Nếu cần quay lại code: tắt nhận đơn mới, bảo vệ audit, giữ các cột/bảng bổ sung. **Không chạy code cũ cho nhận tiền/booking/check-in** khi đã có quyền MULTI/ALL hoặc kỳ ACTIVE tương lai vì code cũ không hiểu mô hình mới. Không drop các snapshot hoặc khôi phục backup cũ đè khoản tiền mới nhận. Cần đối chiếu tài chính trước rollback.

Chưa chạy/diễn tập V711 trên SQL Server trong lần triển khai này. Hibernate SQLServerDialect export chỉ là kiểm tra metadata không kết nối database, không thay thế kiểm chứng SSMS.

## 4. Cấu hình môi trường

Không cần Environment Variable mới, không tự sửa biến nào. Giữ nguyên cấu hình database/profile, HMAC, VA BIDV, Live deployment guard và manual reconciliation enabled theo môi trường hiện có. `SEPAY_LIVE_ACCEPT_NEW_PAYMENTS=false` vẫn được giữ: code không bypass công tắc, đơn SePay mới vẫn bị chặn khi chưa được người quản trị cho phép. CASH và đối soát đơn cũ dùng quy trình hiện có.

Job kiểm tra quyền sử dụng cờ scheduling đã có của dự án, không tạo profile/cấu hình chạy khác. Không ghi secret vào tài liệu hoặc Git.

## 5. Hướng dẫn kiểm thử giao diện (chỉ QA đã được phép)

1. **Admin Plan:** `/admin/membership-plans` → Tạo/sửa. Tạo gói Gym SINGLE, Yoga MULTI chọn A+B, PT ALL_BRANCHES; đặt giá, 30 ngày. Lưu sai MULTI rỗng/SINGLE chọn nhiều phải bị chặn.
2. **Category:** `/admin/categories` → Sửa Category dùng cho lớp PT/Yoga, chọn PT/YOGA tương ứng. Tạo lớp/lịch tương lai trong module Admin hiện có, capacity nhỏ để kiểm thử. Không thử trên lớp dữ liệu thật của nhóm.
3. **Member:** `/member/memberships` → mua gói, chọn CASH hoặc SePay QA đúng cấu hình. PENDING phải chưa có ngày hiệu lực và chưa được đặt lớp ngoài kỳ hiện tại.
4. **Gia hạn còn hạn:** ghi lại ngày cuối hiện tại, bấm Gia hạn gói. Kiểm tra số ngày, giá hiện hành, ngày dự kiến, đơn PENDING. Xác nhận CASH tại Admin hoặc thanh toán QA hợp lệ. Kiểm tra kỳ mới bắt đầu ngay sau kỳ trước và dòng quyền lợi liên tục đã thanh toán tăng đúng 30 ngày, lịch sử cũ không đổi.
5. **Gia hạn hết hạn:** dùng tài khoản QA có gói EXPIRED phù hợp, không tự sửa ngày dữ liệu Live. Kỳ mới PAID bắt đầu ngày ghi nhận và đủ 30 ngày; không nối vào hạn cũ đã qua.
6. **Lớp tương lai:** mở mục lớp của Member (`/member/classes` theo menu hiện có), chọn lịch sau hạn cũ nhưng trong hạn đã gia hạn. Trước PAID phải bị chặn; sau PAID được đặt, slot chỉ tăng một. Lịch sau hạn mới vẫn bị từ chối.
7. **Chi nhánh/quyền lợi:** SINGLE A không đặt/check-in B; MULTI A+B được dùng B nhưng không C; ALL_BRANCHES được dùng chi nhánh đang hoạt động mới. Gói Gym không đặt lớp PT/Yoga đã gắn requiredBenefit tương ứng; Gym và Yoga khác phạm vi không cấp nhầm quyền chéo.
8. **QR:** Member mở `/member/qr-checkin`; Admin quét ở chi nhánh được gói cho phép. Chi nhánh sai bị chặn. Checkout phải ở chi nhánh của lượt tập gốc, không đổi token/TTL/camera.
9. **Đối soát/gia hạn:** `/admin/payments/sepay-reconciliation` → mở đơn gia hạn → Đã nhận tiền chỉ sau kiểm tra ngân hàng được phép. Đơn chưa đủ điều kiện/expired thiếu bằng chứng vẫn phải báo lỗi, không ép PAID. Không dùng Payment #1 làm test.
10. **Hoàn tiền:** chỉ trên QA và theo quy trình được phép, xác nhận khoản hoàn thực tế đã hoàn tất tại màn hình riêng. Kiểm tra kỳ bị thu hồi, lịch sử còn nguyên, booking tương lai không còn coverage bị hủy và có thông báo. Nếu có gói khác bao phủ thì chỗ vẫn được giữ. Không chuyển tiền thật để thử chức năng.
11. **Đồng thời/idempotency:** hai đơn gia hạn khác nhau cùng quyền/phạm vi được xác nhận đồng thời phải cộng đủ tổng ngày; callback gửi lại chỉ xử lý một lần. Hai Member tranh slot cuối chỉ một người thành công. Các ca tự động đã chạy trên H2; kiểm chứng tương ứng trên SQL Server QA cần dữ liệu và quyền được phê duyệt, không tự gửi webhook giả vào Live.

## 6. File tạo/sửa trong lần nâng cấp Membership này

### Tạo mới
- `src/main/java/com/utefit/enums/MembershipBenefit.java`
- `src/main/java/com/utefit/enums/MembershipScope.java`
- `src/main/java/com/utefit/service/MembershipBookingPolicyService.java`
- `src/main/java/com/utefit/service/MembershipBookingPolicyScheduler.java`
- `src/test/java/com/utefit/service/MembershipUpgradeIntegrationTest.java`
- `database/migrations/V711__membership_entitlements_and_renewals.sql`
- `MEMBERSHIP_UPGRADE.md`

### Sửa
- `src/main/java/com/utefit/entity/Membership.java`
- `src/main/java/com/utefit/entity/MembershipPlan.java`
- `src/main/java/com/utefit/entity/ClassCategory.java`
- `src/main/java/com/utefit/repository/MembershipRepository.java`
- `src/main/java/com/utefit/repository/MembershipPlanRepository.java`
- `src/main/java/com/utefit/repository/ClassRegistrationRepository.java`
- `src/main/java/com/utefit/repository/PersonalTrainingScheduleRepository.java`
- `src/main/java/com/utefit/dto/admin/MembershipPlanFormDto.java`
- `src/main/java/com/utefit/dto/admin/ClassCategoryFormDto.java`
- `src/main/java/com/utefit/controller/admin/AdminMembershipPlanController.java`
- `src/main/java/com/utefit/controller/member/MemberMembershipController.java`
- `src/main/java/com/utefit/controller/admin/SepayReconciliationController.java`
- `src/main/java/com/utefit/service/MembershipPaymentService.java`
- `src/main/java/com/utefit/service/MembershipEligibilityService.java`
- `src/main/java/com/utefit/service/MemberClassBookingService.java`
- `src/main/java/com/utefit/service/AdminCheckinService.java`
- `src/main/java/com/utefit/service/PublicCatalogService.java`
- `src/main/java/com/utefit/service/SepayPaymentService.java`
- `src/main/java/com/utefit/service/SepayWebhookService.java`
- `src/main/java/com/utefit/service/SepayManualReconciliationService.java`
- `src/main/java/com/utefit/service/VnpayPaymentService.java`
- `src/main/java/com/utefit/service/impl/AdminMembershipPlanServiceImpl.java`
- `src/main/java/com/utefit/service/impl/AdminCategoryServiceImpl.java`
- `src/main/java/com/utefit/service/impl/PersonalTrainingScheduleServiceImpl.java`
- `src/main/resources/templates/admin/plans/form.html`
- `src/main/resources/templates/admin/plans/list.html`
- `src/main/resources/templates/admin/categories/form.html`
- `src/main/resources/templates/member/memberships.html`
- `src/main/resources/templates/admin/payments/sepay-manual.html`
- `src/main/resources/templates/admin/payments/sepay-manual-refund.html`
- `src/test/java/com/utefit/service/CashPaymentRegressionTest.java`
- `src/test/java/com/utefit/service/VnpayPaymentServiceTest.java`
- `src/test/java/com/utefit/service/LiveDemoSchemaPreparationTest.java`
- `SEPAY_MANUAL_RECONCILIATION.md`

Các file SePay trên đã có thay đổi chưa commit từ trước; lần này chỉ mở rộng liên quan Membership, không restore hoặc thay thế các thay đổi đó. Test schema tiếp tục kiểm tra bootstrap lịch sử nguyên vẹn, đồng thời nhận diện phần schema bổ sung của V711; không tạo/reset database.

## 7. Kết quả và phần chưa kiểm chứng

Đã chạy compile và test trên H2 in-memory, mock cấu hình/guard tại các ca thanh toán tích hợp, không có gọi mạng ngân hàng/Cloudinary thật.

- Maven compile + bộ Membership/regression: **227 Java tests PASS**, 0 failure/error/skip. Trong đó 18 test Membership mới: gia hạn còn hạn/hết hạn, CASH PENDING/FAILED và SePay REVIEW không mua quyền, SePay lặp, CASH/SePay thanh toán đồng thời, booking tương lai trước/sau PAID, scope SINGLE/MULTI/ALL, không ghép chéo quyền lợi/phạm vi giữa gói, hai Member tranh slot cuối, snapshot bất biến, Admin validation, render form Member/Admin, QR/check-out gốc, hủy/hoàn kỳ với lớp tương lai, giữ booking khi có gói khác và hủy PT cá nhân kèm thông báo Member/PT.
- Regression gồm SePay Live guard, BIDV VA, HMAC, đối soát Admin, CASH, callback VNPay lịch sử, QR và polling Member, Manager, Favorites, Review/50 MiB video.
- **38 JavaScript tests PASS**. Không có JavaScript mới trong tính năng này; dự kiến và thao tác Member render server-side, không dùng frontend làm bằng chứng thanh toán.
- Log ignored: `target/membership-upgrade-release-tests.log`, `target/membership-upgrade-js-tests.log`. Hibernate SQLServerDialect export không kết nối DB và xác nhận 25 entity/27 bảng hiện tại, bootstrap cũ giữ nguyên; V711 chỉ bổ sung schema.
- Test Cloudinary có khả năng upload thật không được chạy; các test video trong allowlist dùng mock. Không chạy Playwright hoặc Spring Boot Live.

Không đánh dấu giao dịch Live, migration SQL Server, giao diện iPhone/Safari hoặc Cloudinary thực tế VERIFIED từ test H2. Nếu thao tác xác nhận hoàn bị lỗi, khoản hoàn ngoài ngân hàng không được đảo ngược bởi rollback database: không chuyển tiền thêm, kiểm tra audit và đối soát lại sau khi khắc phục lỗi. Chỉ coi thao tác ứng dụng thành công sau commit; job kiểm tra quyền cần cờ scheduling hiện có được bật để xử lý thay đổi ngoài luồng tức thời.

Chưa kiểm chứng: áp dụng V711 trên SQL Server QA/Live sau phê duyệt; concurrency/locking SQL Server thực; demo UI trên desktop/mobile; callback và hoàn tiền ngân hàng thực. Các trường hợp này không được giả lập trực tiếp trên dữ liệu Live để báo PASS.
