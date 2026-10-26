# Danh mục gói tập demo tiếng Việt

## Giá mới cho cả 5 chi nhánh

| Gói | Quyền lợi nội bộ | Giá |
|---|---|---:|
| 1 ngày | GYM | 20.000đ |
| 2 tuần | YOGA | 180.000đ |
| 1 tháng | ALL | 300.000đ |
| 3 tháng | PT | 900.000đ |
| 3 tháng - Tặng thêm 2 tuần | PT | 950.000đ |
| 6 tháng | ALL | 1.500.000đ |
| 1 năm | GYM | 2.800.000đ |
| 1 năm - Tặng thêm 2 tháng | ALL | 3.200.000đ |
| 2 năm | ALL | 5.600.000đ |

Giữ hai loại demo có sẵn để không xóa gói/quan hệ hoặc giảm số lượng 55 gói:
7 ngày GYM = 120.000đ; 6 tháng ALL tặng 1 tháng = 1.600.000đ.
Đây là hai mức bổ sung được lựa chọn cho những gói chưa có giá trong yêu cầu.

## CSDL hiện có: không reset hoặc chạy lại seed để đổi giá

Đã kiểm tra **chỉ đọc** UTEFitDB: 55 gói demo cũ, các cột tên/mô tả chi nhánh/gói và
địa chỉ đang là VARCHAR, collation `SQL_Latin1_General_CP1_CI_AS`. N'...' một mình
không bảo vệ được tiếng Việt khi ghi vào VARCHAR codepage này.

Chuẩn bị các file sau để chủ dự án/DBA tự áp dụng sau backup, dừng ứng dụng:

1. `database/migrations/V713__catalog_unicode.sql`: đổi 5 cột văn bản sang NVARCHAR
   đúng chiều dài hiện có. Không đổi collation toàn database, không sửa tiền, ngày,
   enum, constraint tài chính hoặc dữ liệu Membership. Chuyển Unicode không thể tự
   phục hồi dấu đã mất từ dữ liệu trước đây. Script dùng transaction, không chạy tự động.
2. Chọn đúng **UTEFitDB** trong SSMS; mở
   `database/operations/UTEFitDB_04_demo_catalog_prices.template.sql`. Mặc định
   `@Apply=0`: chỉ xem 55 dòng trước/sau, không ghi thay đổi.
3. Kiểm tra đúng 11 gói mỗi chi nhánh, giá và tên; sau phê duyệt chủ động đặt
   `@Apply=1` rồi Execute. Script chỉ UPDATE danh mục, không INSERT, không sinh gói
   trùng. Dùng transaction và khóa danh mục; từ chối khi có mapping mơ hồ hoặc không
   đủ 55 gói, không tự sửa những gói đã được nhóm tùy biến.
4. Script chỉ cho database có marker DEVELOPMENT_ONLY + V712-demo55, chưa có Live
   approval/giao dịch SePay Live. Nếu database đã chuyển Live, **không xóa marker,
   approval hoặc dữ liệu để vượt chốt**; DBA cần duyệt quy trình cập nhật danh mục
   riêng phù hợp dữ liệu thực. Không chạy trên LiveDemo hoặc QA.
5. Khởi động lại Spring Boot với cấu hình đang dùng, kiểm tra schema validate.
   Hai entity Branch/MembershipPlan đã khai báo Nationalized tương ứng.

**Chưa thực thi V713 hoặc script cập nhật trên SQL Server trong lượt này.** Không
thay đổi secret, file `.local`, Live guard hoặc cấu hình ngân hàng.

Database mới: `DB_Khung_1.sql` đã dùng NVARCHAR ngay từ đầu, tương đương thay đổi
V713; `DB_Data_2.sql` dùng literal N'...', biến NVARCHAR, tên Việt và giá rõ ràng.
Với database vừa dựng bằng bản schema mới không cần chạy V713 riêng trước seed.
Seed vẫn idempotent: chạy lại không sửa giá/dates/Payment cũ, phải dùng script danh
mục ở trên cho database đã seed. Không reset database hiện tại.

## Hiển thị và lịch sử tài chính

- `MembershipDisplay` chỉ chuyển nhãn: GYM → Phòng gym; YOGA → Yoga;
  PT → Huấn luyện viên cá nhân; ALL → Tất cả dịch vụ. Không đổi enum hay quyền.
- Chuyển đúng mẫu tên demo cũ thành tên Việt ngay cả trước khi cập nhật danh mục;
  không tự dịch tên gói do nhóm đặt tùy ý. Sau cập nhật, dữ liệu tên Việt được mọi
  trang dùng trực tiếp, tìm kiếm theo tiếng Việt từ database.
- Tiền dùng locale vi-VN, dạng `20.000đ`, không phụ thuộc locale máy chủ.
- Checkout hiển thị tiền đã định dạng nhưng nút sao chép vẫn lấy số nguyên gốc từ
  backend; không đưa dấu chấm/ký hiệu tiền vào nội dung cần sao chép số tiền.
- Giá mới áp dụng cho lượt mua/gia hạn **mới** sau cập nhật danh mục. Payment
  PAID/PENDING và quotedPrice, kỳ hạn/bonus snapshot đã mua giữ nguyên. Vì vậy lịch
  sử có thể còn giá cũ: đây là bảo toàn tài chính, không phải lỗi hiển thị.
- Không thay đổi quy tắc LocalDate cộng tháng/tuần/năm/thưởng hoặc ngày hiệu lực.

## File thay đổi

- SQL: `DB_Khung_1.sql`, `DB_Data_2.sql`; migration V713; template cập nhật danh mục.
- Java: `entity/Branch.java`, `entity/MembershipPlan.java`,
  `util/MembershipDisplay.java` (dưới `src/main/java/com/utefit`).
- Templates Member: memberships, payments, history, dashboard, sepay-checkout,
  classes, reviews.
- Templates Guest: membership-plans, home, branch-detail, classes, trainers.
- Templates Admin: plans/form, plans/list; payments/cash-pending, detail, list,
  sepay-manual, sepay-manual-refund, sepay-reconciliation; managers;
  checkins/list; classes/list; schedules/list; reviews/list.
- Templates Manager: index; PT: classes/detail. Các trang ngoài Membership chỉ
  đổi biểu thức nhãn/tên/tiền, không đổi nghiệp vụ.
- Tests: `MembershipDisplayTest.java`, `DemoCatalogIntegrationTest.java`.
- Tài liệu: file này và liên kết trong README. Không commit/push.

## Tự kiểm thử sau khi áp dụng SQL

1. `/membership-plans` và trang chủ: tên Việt, tiền có dấu chấm; tìm gói bằng tên Việt.
2. `/member/memberships`: kiểm tra cả 5 chi nhánh, 11 loại mỗi chi nhánh, quyền lợi,
   kỳ đang dùng/sắp dùng và giá gia hạn mới; lịch sử giá cũ không bị viết lại.
3. Chọn gói, tạo đăng ký và yêu cầu CASH trên dữ liệu demo được phép; số tiền phải
   đúng giá snapshot. Không cần bật SePay hoặc tạo giao dịch ngân hàng.
4. Xác nhận CASH bằng Admin được phép; kiểm tra cộng lịch DAY/WEEK/MONTH/YEAR,
   thưởng thêm, kỳ sau nối ngày ở cùng chi nhánh, độc lập giữa các chi nhánh.
5. Mở lịch sử thanh toán/checkout đơn cũ, xác nhận tiền snapshot và ngày không đổi.
6. Kiểm tra điện thoại và desktop. Không dùng QR ngân hàng để chuyển tiền thật.

## Kết quả đã chạy

- Maven compile + **172 Java tests PASS**, không fail/error. Hai test mới kiểm tra
  nhãn/tiền và đọc chính 11 dòng giá từ seed để tạo 55 gói ở 5 chi nhánh trên H2,
  mua gói → Payment CASH đúng giá, PENDING chưa kích hoạt, snapshot không đổi khi
  sửa giá danh mục, xác nhận CASH tính đúng kỳ theo lịch và render trang Member.
- Bộ regression gồm MembershipTerm/MembershipUpgrade (tháng/tuần/năm/thưởng,
  nối kỳ/concurrency), SePay, manual reconciliation, CASH, QR và Review chi nhánh.
- **42 JavaScript tests PASS**. Không cần thay JavaScript của SePay.
- Bốn script SQL liên quan được parse bằng Microsoft SQL Server ScriptDom:
  **0 lỗi cú pháp**. Test bàn giao xác nhận schema nhúng nguyên migration V713.
- Chưa chạy T-SQL migration/cập nhật danh mục trên SQL Server, không reset/seed DB,
  không test camera/điện thoại hoặc Cloudinary/ngân hàng thật. H2 không thay thế
  kiểm thử thực thi T-SQL trên SQL Server sau khi DBA cho phép.
- Nhánh `feat/giai-doan-7`; giữ toàn bộ thay đổi có sẵn, chưa commit/push.
