# Membership một chi nhánh và thời hạn theo lịch

## Quy tắc đang triển khai

- Gói mới/chỉnh sửa phải chọn đúng một chi nhánh đang hoạt động. Không có lựa chọn
  MULTI/ALL/Flex trên form. Request tự gửi phạm vi khác vẫn bị backend từ chối.
- Dùng bảng `membership_plan_branches` hiện có với đúng một ID, không tạo bảng mới.
  Gói cũ chưa có chi nhánh cố định hoặc còn phạm vi đa chi nhánh không được bán mới
  cho đến khi Admin chỉnh lại. Không tự đổi dữ liệu cũ để đạt điều kiện này.
- Một Member có thể mua nhiều gói ở nhiều chi nhánh. Mỗi đơn/Payment/kỳ riêng biệt.
- Khi xác nhận PAID hợp lệ, kỳ mới nối sau ngày cuối cùng của các kỳ đã thanh toán
  còn hiệu lực hoặc sắp sử dụng tại cùng chi nhánh đăng ký, bất kể tên gói/quyền lợi.
  Nếu chuỗi đã hết hạn, bắt đầu ngày xác nhận tiền theo quy tắc kích hoạt hiện có.
  Không cộng ngày cho PENDING/REVIEW/FAILED. Khóa User trước Membership/Payment
  được giữ nguyên để tuần tự hóa thanh toán đồng thời, kể cả khác phương thức.
- Kỳ tiếp theo chưa bắt đầu thì chưa có quyền check-in, dù Membership lưu ACTIVE.
  Kỳ tương lai đã thanh toán có thể dùng giữ chỗ lớp đúng chi nhánh, ngày và quyền lợi.
  Chỉ gói PT/ALL mới bao phủ lớp yêu cầu PT. Ngày tập bao gồm cả ngày hết hạn;
  lớp/lịch PT hiện là khoảng giờ trong cùng ngày, quyền phải bao phủ ngày đó.
- Hoàn/hủy một kỳ không tính lại ngày của những kỳ khác. Revalidation hiện có
  kiểm tra/hủy booking tương lai không còn đủ quyền và gửi thông báo.

## Thời hạn và cuối tháng

Snapshot mới lưu riêng số lượng/đơn vị cơ bản và thưởng. Backend sử dụng:

`mốc kết thúc loại trừ = start.plus<unit>(quantity).plus<bonusUnit>(bonusQuantity)`

`endDate bao gồm = mốc kết thúc loại trừ - 1 ngày`

DAY/WEEK/MONTH/YEAR dùng LocalDate.plusDays/plusWeeks/plusMonths/plusYears.
Cộng cơ bản trước, thưởng sau; không quy đổi tháng = 30 ngày hoặc năm = 365 ngày.
Ngày không tồn tại được LocalDate chốt về ngày cuối tháng tại từng bước cộng.

Ví dụ theo quy tắc khoảng ngày [start, mốc kết thúc):

| Bắt đầu | Thời hạn | Ngày hết hạn bao gồm |
|---|---|---|
| 31/01/2026 | 1 tháng | 27/02/2026 (mốc loại trừ 28/02) |
| 31/01/2024 | 1 tháng | 28/02/2024 (mốc loại trừ 29/02) |
| 29/02/2024 | 1 năm | 27/02/2025 |
| 31/01/2026 | 3 tháng + 2 tuần | 13/05/2026 |
| 25/11/2026 | 30 ngày | 24/12/2026 |

Giới hạn backend cho mỗi phần: DAY <= 36600, WEEK <= 5200, MONTH <= 1200,
YEAR <= 100. Cơ bản > 0, thưởng >= 0. Không giới hạn theo số chi nhánh vì mỗi
gói mới chỉ có một chi nhánh.

`duration_days`/`quoted_duration_days` được giữ để tương thích schema và kiểm tra
snapshot ở các gateway cũ. Với cấu hình lịch, duration_days là giá trị tương thích
tại mốc tham chiếu 01/01/2000, KHÔNG được dùng để tính hạn mới. Quyền và ngày hết
hạn mới dùng snapshot lịch; giao diện hiển thị mô tả lịch và ngày thực tế.
Đối soát thủ công cũng dùng cùng phép tính lịch để kiểm tra kỳ đã hết hạn hay chưa.

## Bảo vệ lịch sử

- Không sửa Payment #1/#2, Membership đã PAID, snapshot giá, scope, branchIds,
  ngày bắt đầu/kết thúc hoặc lịch sử tài chính khi deploy/mở trang.
- Nếu kỳ cũ MULTI/ALL còn bao phủ chi nhánh định mua, kỳ mới tại chi nhánh đó
  cũng xếp sau quyền cũ, không chồng kỳ chỉ vì chi nhánh đăng ký gốc khác nhau.
  Hai gói mới SINGLE ở A và B vẫn hoàn toàn độc lập.
- Cột mới nullable: snapshot cũ vẫn tính theo quoted_duration_days, không lấy
  tháng/năm mới của danh mục để tính lại giao dịch cũ.
- Quyền MULTI/ALL cũ vẫn được MembershipEligibilityService công nhận theo snapshot
  cũ. QR check-in/out, capacity, quyền lợi lớp và thông báo dùng logic hiện có.
- Lịch sử nhóm theo chi nhánh đăng ký. Kỳ cũ đa chi nhánh có nhãn quyền cũ;
  nhãn này không giới hạn quyền vào một chi nhánh. Không bán lại mô hình Flex.
- “Tổng hạn tập” là mốc cuối của các kỳ đã thanh toán chưa hủy/hoàn, không khẳng
  định quyền liên tục. Giao diện hiển thị riêng hạn liên tục từ hôm nay, từng kỳ
  và cảnh báo khoảng trống. Không cấp quyền PT cho cả chuỗi chỉ vì một kỳ có PT.

## Migration thủ công

File: `database/migrations/V712__membership_calendar_terms.sql`.

Chưa thực thi trên SQL Server. Cần V711 trước. Migration chỉ ADD tám cột nullable
và hai CHECK constraints; không UPDATE/DELETE/DROP, không thay trạng thái, không
chuyển scope hoặc tạo approval. TRY/CATCH + XACT_ABORT rollback nếu lỗi.

1. Dừng ứng dụng; sao lưu database đích theo quy trình của người quản trị.
2. Mở SSMS, tự chọn đúng database được phê duyệt. Kiểm tra `SELECT DB_NAME();`.
3. Xác nhận V711 đã có và V712 chưa áp dụng; đọc toàn bộ script trước khi chạy.
4. Chỉ khi được phê duyệt, người quản trị tự chạy V712 và kiểm tra đủ cột/constraints.
5. Chạy bản mới với `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` nếu cấu hình triển khai
   chưa đặt validate, để Hibernate không tự sửa schema. Không có biến SePay mới.
6. Sau đó tự chỉnh những gói muốn tiếp tục bán về một chi nhánh cố định bằng UI.
   Không sửa snapshot của Membership đã mua.

Nếu migration lỗi: transaction rollback; kiểm tra lại schema và nguyên nhân trước
khi chạy lại. Không drop cột hoặc restore backup ghi đè tiền đã nhận. Nếu cần quay
lại code cũ: tạm dừng tạo/xác nhận đơn mới, giữ schema bổ sung; không cho code cũ
kích hoạt đơn calendar đang PENDING bằng số ngày tương thích. Chỉ rollback sang
bản hiểu snapshot lịch hoặc xử lý hết các đơn này theo quy trình được duyệt.

## Demo trên QA

1. Admin → Gói tập → Thêm: A, 1 tháng, thưởng 0, quyền GYM. Tạo thêm gói A tên
   khác: 3 tháng tặng 2 tuần; tạo gói B: 1 năm tặng 1 tháng.
2. Member → `/member/memberships`: xem catalog theo chi nhánh và ngày dự kiến.
   Mua gói A; đơn PENDING chưa tăng Tổng hạn. Thanh toán theo quy trình QA hợp lệ.
3. Mua thêm gói thứ hai/ba tại A, xác nhận tiền trên QA. Kỳ sau phải bắt đầu sau
   kỳ trước đúng một ngày; hiện “Sắp sử dụng”, không có quyền check-in sớm.
4. Mua gói B trong lúc A còn hạn: B bắt đầu độc lập theo ngày xác nhận tiền.
5. Dùng gói quyền PT tại kỳ tương lai đã PAID để đặt lớp đúng chi nhánh; thử lớp
   ngoài hạn/sai chi nhánh/sai quyền lợi phải bị từ chối. Check-in chỉ dùng kỳ hiện tại.
6. Hoàn tiền thực tế một kỳ giữa trên QA theo quy trình bằng chứng hiện có: kỳ sau
   không dịch ngày; màn hình cảnh báo khoảng trống, booking không còn quyền bị hủy.
7. Kiểm tra dữ liệu cũ đa chi nhánh vẫn được check-in đúng quyền snapshot cũ.

Không tự tạo giao dịch thật hoặc gửi webhook giả vào Live để demo. Không tự bật
SEPAY_LIVE_ACCEPT_NEW_PAYMENTS, không thay HMAC/VA/Live guard/CSRF.

## File trong tác vụ này

Tạo mới:
- `src/main/java/com/utefit/enums/MembershipDurationUnit.java`
- `src/main/java/com/utefit/util/MembershipTerm.java`
- `database/migrations/V712__membership_calendar_terms.sql`
- `src/test/java/com/utefit/service/MembershipTermTest.java`
- `MEMBERSHIP_SIMPLE_MODEL.md`

Chỉnh sửa:
- `src/main/java/com/utefit/entity/MembershipPlan.java`
- `src/main/java/com/utefit/entity/Membership.java`
- `src/main/java/com/utefit/dto/admin/MembershipPlanFormDto.java`
- `src/main/java/com/utefit/service/impl/AdminMembershipPlanServiceImpl.java`
- `src/main/java/com/utefit/service/MembershipPaymentService.java`
- `src/main/java/com/utefit/service/MembershipEligibilityService.java` (tái sử dụng phép kiểm tra scope cũ khi nối kỳ, không đổi quy tắc cấp quyền)
- `src/main/java/com/utefit/service/PublicCatalogService.java`
- `src/main/java/com/utefit/repository/MembershipPlanRepository.java`
- `src/main/java/com/utefit/controller/member/MemberMembershipController.java`
- `src/main/java/com/utefit/service/SepayPaymentService.java` (mô tả kỳ trên trang chỉ đọc)
- `src/main/java/com/utefit/service/SepayManualReconciliationService.java` (tính hạn calendar)
- `src/main/resources/templates/admin/plans/form.html`
- `src/main/resources/templates/admin/plans/list.html`
- `src/main/resources/templates/member/memberships.html`
- `src/main/resources/templates/member/sepay-checkout.html`
- `src/main/resources/templates/guest/home.html`
- `src/main/resources/templates/guest/membership-plans.html`
- `src/test/java/com/utefit/service/AdminPlanBranchScopeIntegrationTest.java`
- `src/test/java/com/utefit/service/MembershipUpgradeIntegrationTest.java`
- `src/test/java/com/utefit/service/SepayPaymentIntegrationTest.java`
- `src/test/java/com/utefit/service/LiveDemoSchemaPreparationTest.java` (giữ core snapshot cũ, kiểm tra riêng cột bổ sung V712; chỉ xuất DDL ra target, không kết nối DB)
- `MEMBERSHIP_UPGRADE.md` (đánh dấu phần thiết kế cũ đã được thay thế)

Bỏ thành phần chọn đa chi nhánh không còn sử dụng:
- `src/main/resources/static/css/admin-plan-scope.css`
- `src/main/resources/static/js/admin-plan-scope.js`
- `src/test/js/admin-plan-scope.test.cjs`

Các thay đổi chưa commit trước tác vụ vẫn được giữ, ngoài phần UI/logic được thay
thế rõ ràng theo yêu cầu đơn giản hóa. Không thay application.properties hoặc V711.
