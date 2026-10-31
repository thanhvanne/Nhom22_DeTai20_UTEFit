# Đồng bộ ảnh chi nhánh demo — cơ chế hiện tại và thay đổi frontend

Ngày: 30/10/2026. Báo cáo này thay thế chính sách bắt buộc giữ placeholder cho ảnh thiếu bằng chứng trong báo cáo lần trước, theo yêu cầu mới sử dụng 5 ảnh AI mô phỏng UTEFit làm ảnh demo.

## Nguyên nhân và cơ chế đã xác định trước khi sửa

1. “Ảnh nguồn · chưa xác minh” là chuỗi được viết trực tiếp trong `templates/fragments/branch-media.html` ở lần sửa trước. Không phải giá trị đọc từ database, không phải kết quả backend kiểm tra ảnh. Khi đó Admin vẫn render ảnh nguồn, còn fragment Public/Member/homepage chỉ tạo placeholder, không có `<img>` cho ảnh chi nhánh.
2. `Branch` và `BranchFormDto` có `imageUrl`, `imagePublicId`; `PublicCatalogService.BranchView` có `imageUrl`. Không có trường loại ảnh AI/ảnh thật, xác minh, duyệt, tác giả hoặc giấy phép. `active` là trạng thái hoạt động chi nhánh, không phải trạng thái ảnh.
3. Schema `database/DB_Khung_1.sql`, `database/live-demo/01_schema.sql` và bảng `branches` trong `database/UTEFitDB_PT.sql` cũng chỉ khai báo `image_url`, `image_public_id` cho ảnh, không có cột xác minh ảnh. Không tìm thấy quy trình duyệt ảnh trong controller/service quản lý chi nhánh. Chưa xác nhận metadata trực tiếp của database đang chạy: thử kết nối chỉ đọc không thành công vì thông tin đăng nhập phụ thuộc môi trường của tiến trình app/Windows SSPI. Kết luận về schema dựa trên các file SQL của dự án; kết luận về hành vi backend dựa trên Entity, DTO và service thực tế.
4. Upload hiện tại: form multipart gửi `imageFile`; service upload Cloudinary vào `utefit/branches`, lấy `upload.url()` và `upload.publicId()` rồi lưu theo cơ chế hiện có. Khi cập nhật có file mới, backend thay URL/public ID và dọn tài nguyên cũ sau transaction; khi không chọn file mới, form giữ `imageUrl`/`imagePublicId` bằng hidden input và service giữ/cập nhật URL theo logic sẵn có. Không có bước chờ duyệt ảnh trong luồng này.
5. `PublicCatalogService.toBranchView` đã truyền `b.getImageUrl()` cho danh sách, chi tiết và chi nhánh nổi bật. Sự khác biệt hiển thị nằm ở template, không nằm ở việc thiếu field hoặc lỗi mapping. Cả 5 URL hiện tại đều trả HTTP 200 với Guest/Member/Admin.
6. Homepage chỉ lấy 3 chi nhánh nổi bật do `featuredBranches().limit(3)`, hiện là ID 5, 4, 3. ID 1 không được chọn vào gallery; đây là cơ chế chọn nội dung hiện có, không phải lỗi tải ảnh ID 1.

## Thay đổi frontend

- Giữ một fragment dùng chung, lấy đúng `branch.imageUrl` của chính bản ghi theo ID cho Admin, Public, Member và gallery. URL HTTP/HTTPS hoặc đường dẫn nội bộ được render; URL trống/không phù hợp dùng placeholder. Nếu request/giải mã ảnh thất bại, JS ẩn ảnh hỏng, hiện placeholder/thông báo “Không tải được ảnh chi nhánh” và ẩn caption của ảnh lỗi.
- Bỏ toàn bộ nhãn “chưa xác minh” khỏi thành phần hiện tại. Ảnh demo được ghi “Ảnh minh họa”; alt Public/Member/gallery nêu rõ không phải ảnh chụp cơ sở thực tế. Không thêm field, nút duyệt, trạng thái giả hoặc bước xác minh.
- Giữ 5 tài nguyên Cloudinary cũ đã được nhận diện có biển gym khác trong điều kiện loại trừ của fragment. Đây là ngoại lệ theo nội dung ảnh đã kiểm tra, không phải danh sách chi nhánh được duyệt: nó đối chiếu đường dẫn tài nguyên, không đối chiếu ID chi nhánh. Ảnh PIG GYM cũ của ID 1 cũng được loại nếu xuất hiện lại. Admin vẫn xem được nguồn cũ để thay thế, với nhãn “Ảnh gym khác · không dùng ở Public”; Public/Member/gallery không tải các nguồn này.
- Khi upload ảnh AI thay thế thành công và backend trả URL mới, ảnh tự hiển thị theo cùng cơ chế, không cần thêm thao tác frontend hay duyệt ảnh. Không tự nhận diện AI/gym khác cho một URL hoàn toàn mới; việc gắn nhãn minh họa áp dụng cho bộ ảnh demo theo yêu cầu này. Không dùng kết quả tải 200 để tuyên bố đây là ảnh chụp UTEFit thật.
- Giữ bố cục, Public Premium/Cinematic, Member Navy, route, bộ lọc và animation. Ảnh dùng `object-fit: cover`, frame responsive, caption tương phản rõ. Homepage được thêm assets chung để có cùng caption/fallback.

## Đối chiếu 5 chi nhánh hiện tại

| ID | URL đang lưu / ảnh | Admin | Public và Member | Homepage |
|---|---|---|---|---|
| 1 | [cybv6sdzygkvpzgsqh2g.png](https://res.cloudinary.com/tburyrz8/image/upload/v1793295123/utefit/branches/cybv6sdzygkvpzgsqh2g.png), mặt tiền mang biển UTEFIT | Hiện ảnh, nhãn “Ảnh minh họa” | Danh sách và chi tiết hiện cùng URL, nhãn “Ảnh minh họa” | Không nằm trong 3 bản ghi được backend chọn |
| 2 | [xamgjfsidfbc0rhyvhhi.jpg](https://res.cloudinary.com/tburyrz8/image/upload/v1793278226/utefit/branches/xamgjfsidfbc0rhyvhhi.jpg), S’LIFE GYM | Xem nguồn, thông báo ảnh gym khác | Placeholder, chờ thay ảnh AI của bạn | Không nằm trong 3 bản ghi được chọn |
| 3 | [qoqqti8jvlilah0wxewk.jpg](https://res.cloudinary.com/tburyrz8/image/upload/v1793278235/utefit/branches/qoqqti8jvlilah0wxewk.jpg), THE NEW GYM | Xem nguồn, thông báo ảnh gym khác | Placeholder, chờ thay ảnh AI của bạn | Placeholder cho cùng nguồn bị loại |
| 4 | [zmlyompmz4ulmk1dtpau.jpg](https://res.cloudinary.com/tburyrz8/image/upload/v1793278244/utefit/branches/zmlyompmz4ulmk1dtpau.jpg), FITIGYM | Xem nguồn, thông báo ảnh gym khác | Placeholder, chờ thay ảnh AI của bạn | Placeholder cho cùng nguồn bị loại |
| 5 | [nwfgqyo7j8xabno1c737.jpg](https://res.cloudinary.com/tburyrz8/image/upload/v1793278254/utefit/branches/nwfgqyo7j8xabno1c737.jpg), S’LIFE GYM | Xem nguồn, thông báo ảnh gym khác | Placeholder, chờ thay ảnh AI của bạn | Placeholder cho cùng nguồn bị loại |

Trong phiên này không có 5 file AI đính kèm để upload và không gửi thao tác sửa database. Vì vậy chưa thể báo rằng cả 5 chi nhánh đã có ảnh AI mới. Bạn có thể dùng form upload hiện tại để thay ảnh cho ID 2–5; không cần sửa backend hoặc bổ sung bước duyệt.

## Kiểm tra và giới hạn

- 26/26 Node tests đạt: xử lý ảnh lỗi trước/sau deferred script, giữ ảnh hợp lệ, tích hợp template và regression gallery.
- 25/25 kiểm tra render Thymeleaf thuần đạt: 5 biến thể Admin/directory/gallery/Member/Member detail × URL demo, nguồn gym khác, URL rỗng, scheme không phù hợp và đường dẫn ảnh lỗi. Không khởi động Spring app/database để tạo các fixture này.
- Browser trên `localhost:8080`: 63/63 lượt trả 200 — 3 vai trò × 3 kích thước 1440/768/390 × (danh sách, 5 trang chi tiết hoặc form Admin, homepage). Không có `pageerror`, không tràn ngang tài liệu; ảnh được chờ decode trước khi ghi nhận/chụp, không có ảnh được render nhưng chưa tải thành công trong đợt kiểm tra cuối.
- URL ảnh nguồn hiện tại: 15/15 request trả 200 cho 5 ID × Guest/Member/Admin. Việc ID 2–5 không hiện ở Public là ngoại lệ loại ảnh gym khác, không phải lỗi 403/404 hoặc bước duyệt.
- Giả lập response ảnh 403/404 trong browser: Guest, Member và Admin đều hiện thông báo lỗi, ẩn ảnh hỏng/caption. Không thay URL thật hoặc dữ liệu database.
- Gallery với ảnh demo được kiểm tra bằng HTML render từ chính fragment, có nhãn “Fixture only”, ở cả JS bật/tắt; ảnh và caption đều hiển thị. Trường hợp gallery lỗi cũng hiện placeholder. Đây không phải bằng chứng rằng database đã có ảnh demo cho ID 3–5: gallery thực tế hiện vẫn chứa ba ảnh cũ bị loại như bảng trên.
- Không thực hiện upload end-to-end lên database trong phiên này; giữ nguyên form, tên field, enctype, controller/service và logic lưu. Không báo upload mới thành công khi chưa upload file nào.

## File sửa

- `src/main/resources/templates/fragments/branch-media.html`: dùng URL, nhãn minh họa, ngoại lệ ảnh gym khác và placeholder chung.
- `src/main/resources/static/css/utefit-branch-media.css`: frame/crop/caption và trạng thái fallback.
- `src/main/resources/static/js/utefit-branch-media.js`: fallback tải lỗi cho cả Admin/Public/Member/gallery.
- `src/main/resources/templates/admin/branches/list.html`, `src/main/resources/templates/admin/branches/form.html`: bỏ mô tả chưa xác minh, giải thích cách dùng ảnh demo; không đổi field upload hoặc thao tác lưu.
- `src/main/resources/templates/guest/home.html`: nạp assets ảnh chung.
- `src/test/js/branch-media.test.cjs`, `src/test/js/public-directory.test.cjs`, `src/test/js/public-journey.test.cjs`: cập nhật kiểm thử theo chính sách mới.
- `src/main/resources/static/images/public-directory-assets.md`, báo cáo này và chú thích báo cáo cũ: ghi lại chính sách mới, tránh nhầm tài liệu lịch sử.

Không sửa Java/backend, database/migration, security hoặc file nghiệp vụ Payment/Membership/QR/Chat/Booking. Có đối chiếu SHA-256 với snapshot đầu phiên. Không reset, commit hoặc push Git.

## Bằng chứng cục bộ

- [Admin trước](target/branch-demo-sync/before-admin-branches.png) / [sau](target/branch-demo-sync/after-admin-branches-1440.png).
- [Guest trước](target/branch-demo-sync/before-guest-branches.png) / [sau](target/branch-demo-sync/after-guest-branches-1440.png).
- [Member trước](target/branch-demo-sync/before-member-branches.png) / [sau mobile](target/branch-demo-sync/after-member-branches-390.png).
- [Chi tiết Guest ID 1](target/branch-demo-sync/after-guest-detail-1-390.png) / [chi tiết Member ID 1](target/branch-demo-sync/after-member-detail-1-1440.png).
- [Gallery thực tế](target/branch-demo-sync/after-guest-gallery-390.png) / [fixture gallery có ảnh demo](target/branch-demo-sync/fixture-gallery-nojs-false.png).
- [Kết quả browser theo ID/vai trò/kích thước](target/branch-demo-sync/browser-evidence.json) / [log render template](target/branch-demo-sync/render-check.log).

Các file trong `target/branch-demo-sync/` chỉ là QA cục bộ, có thể bị xóa khi clean. Hoàn tất frontend; dừng chờ duyệt.
