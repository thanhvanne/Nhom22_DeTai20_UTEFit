# Đối chiếu và đồng bộ chính sách ảnh chi nhánh UTEFit

> Báo cáo lịch sử của yêu cầu trước. Chính sách hiển thị đã được thay thế theo yêu cầu dùng ảnh AI/demo, không thêm bước duyệt. Xem [BRANCH_IMAGE_DEMO_REPORT.md](BRANCH_IMAGE_DEMO_REPORT.md) để biết cơ chế và kết quả hiện tại. Những mô tả “chưa xác minh” bên dưới không còn là hành vi frontend hiện tại.

Ngày kiểm tra: 30/10/2026. Môi trường thực tế: `http://localhost:8080`, tài khoản demo Admin và Member trong README; Guest dùng context riêng chưa đăng nhập. Không dùng fixture để thay thế kết quả kiểm tra các chi nhánh thật trong môi trường này.

## Kết luận nguyên nhân

Không phải lỗi đường dẫn, CSS, thiếu dữ liệu URL hay quyền xem ảnh. Ở snapshot đầu phiên, cả 5 URL đều trả HTTP 200, `image/jpeg`, tải và giải mã được ở Guest, Member và Admin. Lần kiểm tra cuối ghi nhận URL ID 1 đã đổi sang PNG có biển UTEFIT; 4 URL còn lại giữ nguyên. Cả 5 URL tại lần đọc cuối cũng trả 200 cho đủ 3 vai trò. Chi tiết thay đổi dữ liệu quan sát được nằm ngay sau bảng.

- Admin nhận `Branch` qua `AdminBranchServiceImpl`, trước đây render trực tiếp `b.imageUrl` trong danh sách, `branchForm.imageUrl` trong form; chỉ kiểm tra khác null/rỗng. Crop danh sách cũ 88 × 56, form 180 × 110, `object-fit: cover`.
- `Branch.imageUrl` ánh xạ cột `branches.image_url`; `imagePublicId` là định danh tài nguyên Cloudinary, không phải bằng chứng xác minh ảnh hoặc quyền sử dụng.
- `PublicCatalogService.toBranchView` truyền nguyên `b.getImageUrl()` vào `BranchView.imageUrl`; danh sách và chi tiết Public không thiếu field này. Đối chiếu theo ID, không theo thứ tự: Admin tăng dần ID, Public giảm dần ID.
- Guest dùng `public-directory :: clubMedia`; fragment cũ luôn dựng placeholder kiến trúc, hoàn toàn không có `<img>` cho ảnh chi nhánh. Khi đăng nhập, `/branches` và `/branches/{id}` dùng phần template riêng, cũng luôn có placeholder. Đây là chủ ý không công bố ảnh chưa xác minh, đã ghi trong `static/images/public-directory-assets.md` trước lần sửa này.
- Homepage dùng `public-journey.html`, vẫn render thẳng `b.imageUrl`, nên chưa tuân thủ chính sách trên. Đây là điểm thiếu đồng bộ cần sửa. Homepage chỉ nhận 3 chi nhánh nổi bật theo `featuredBranches().limit(3)`: ID 5, 4, 3; việc không có ID 1, 2 ở gallery là hành vi backend hiện có.
- `Branch`, `BranchFormDto` và `BranchView` không có thông tin xác minh, tác giả, giấy phép hay nguồn xuất bản gốc. Không dùng `active`, đường dẫn chứa `utefit`, upload thành công hoặc HTTP 200 làm bằng chứng xác minh.

## Bảng nghiệm thu theo ID

“Chưa xác minh” bên dưới mô tả việc thiếu bằng chứng, không phải một trạng thái mới được ghi vào database. Các biển hiệu được đọc từ ảnh nguồn ở kích thước gốc; không kết luận chủ sở hữu bản quyền chỉ từ biển hiệu.

| Chi nhánh | Ảnh Admin | Ảnh Public | Nguồn ảnh | Trạng thái xác minh | Cách xử lý |
|---|---|---|---|---|---|
| ID 1 — UTEFit - Chi nhánh 1 | Đầu phiên: PIG GYM; lần đọc cuối: ảnh mặt tiền mang biển UTEFIT. Cả hai đều chỉ là bản xem nguồn chưa xác minh | Placeholder ở danh sách và chi tiết | [Cloudinary ID 1 hiện tại](https://res.cloudinary.com/tburyrz8/image/upload/v1793295123/utefit/branches/cybv6sdzygkvpzgsqh2g.png), 1448 × 1086 | Chưa đủ bằng chứng đúng chi nhánh hoặc quyền sử dụng; có biển UTEFIT cũng chưa chứng minh ảnh thực tế | Không ghi/sửa URL; Member dùng placeholder; homepage không chọn ID này |
| ID 2 — UTEFit - Chi nhánh 2 | Ảnh có biển S’LIFE GYM; xem nguồn có nhãn chưa xác minh | Placeholder ở danh sách và chi tiết | [Cloudinary ID 2](https://res.cloudinary.com/tburyrz8/image/upload/v1793278226/utefit/branches/xamgjfsidfbc0rhyvhhi.jpg), 682 × 449 | Chưa đủ bằng chứng đúng chi nhánh hoặc quyền sử dụng | Giữ URL; Member dùng placeholder; homepage không chọn ID này |
| ID 3 — UTEFit - Chi nhánh 3 | Ảnh có biển THE NEW GYM; xem nguồn có nhãn chưa xác minh | Placeholder ở danh sách và chi tiết | [Cloudinary ID 3](https://res.cloudinary.com/tburyrz8/image/upload/v1793278235/utefit/branches/qoqqti8jvlilah0wxewk.jpg), 678 × 452 | Chưa đủ bằng chứng đúng chi nhánh hoặc quyền sử dụng | Giữ URL; Member và gallery homepage dùng placeholder |
| ID 4 — UTEFit - Chi nhánh 4 | Ảnh có biển FITIGYM; xem nguồn có nhãn chưa xác minh | Placeholder ở danh sách và chi tiết | [Cloudinary ID 4](https://res.cloudinary.com/tburyrz8/image/upload/v1793278244/utefit/branches/zmlyompmz4ulmk1dtpau.jpg), 638 × 480 | Chưa đủ bằng chứng đúng chi nhánh hoặc quyền sử dụng | Giữ URL; Member và gallery homepage dùng placeholder |
| ID 5 — UTEFit - Chi nhánh 5 | Ảnh có biển S’LIFE GYM, dòng 575 Sư Vạn Hạnh; xem nguồn có nhãn chưa xác minh | Placeholder ở danh sách và chi tiết | [Cloudinary ID 5](https://res.cloudinary.com/tburyrz8/image/upload/v1793278254/utefit/branches/nwfgqyo7j8xabno1c737.jpg), 480 × 361 | Chưa đủ bằng chứng đúng chi nhánh hoặc quyền sử dụng | Giữ URL; Member và gallery homepage dùng placeholder |

Lưu ý dữ liệu thay đổi trong lúc kiểm tra: nguồn ID 1 đầu phiên là `https://res.cloudinary.com/tburyrz8/image/upload/v1793278217/utefit/branches/nix4fskixwaxy3ou0gkj.jpg` (596 × 335, biển PIG GYM); lần đọc cuối là URL PNG trong bảng. Phiên làm việc này không gửi form lưu/upload/tắt chi nhánh nào và không ghi database; không thể xác định tác nhân thay đổi chỉ bằng các GET đối chiếu. Giữ cả snapshot cũ và mới trong bằng chứng, không tự hoàn tác dữ liệu. [Ảnh nguồn ID 1 ở lần đọc cuối](target/branch-image-sync/final-id1-source-unverified.png), [Admin danh sách lần cuối](target/branch-image-sync/final-admin-branches-1440.png), [Admin chi tiết lần cuối](target/branch-image-sync/final-admin-detail-1-1440.png).

Cloudinary là nơi lưu/phân phối đang được model trỏ tới, chưa xác định được trang xuất bản gốc, tác giả hoặc giấy phép. Không tự công nhận đây là ảnh thật UTEFit, cũng không tự công nhận đã có giấy phép minh họa. Không dùng lại các ảnh này làm minh họa công khai. Địa chỉ model đang là `Development-only location 1..5`, không cung cấp bằng chứng đối chiếu địa điểm trong ảnh. Không kết luận PNG mới là ảnh chụp thật hoặc ảnh sinh AI chỉ dựa vào hình thức.

## Chính sách sau sửa

`fragments/branch-media.html` là nơi dùng chung cho placeholder danh sách Guest, chi tiết Guest, Khám phá chi nhánh Member, chi tiết Member và gallery homepage. Nhãn phân biệt URL rỗng (“Chưa có ảnh chi nhánh”) với URL có sẵn nhưng thiếu xác minh (“Chưa có ảnh thực tế được xác minh”). Nhánh Public không sinh `<img>` hoặc đưa URL ảnh nguồn vào DOM, nên ảnh không lóe lên trước khi JavaScript chạy, và vẫn an toàn khi tắt JavaScript.

Admin giữ bản xem nguồn để đối chiếu dữ liệu, có caption “Ảnh nguồn · chưa xác minh”, URL chỉ đọc trong mục mở rộng và giải thích rõ upload không đồng nghĩa xác minh. Đây không phải công bố ảnh như cơ sở UTEFit thật. Không thêm nút hoặc field xác minh giả. Bản xem nguồn có crop, tỷ lệ cố định và giới hạn chiều rộng; nếu tải lỗi thì hiện thông báo riêng và vẫn giữ URL để kiểm tra.

Không có ảnh nào đủ điều kiện được công bố như ảnh cơ sở thật trong dữ liệu hiện tại. Không thêm allowlist hoặc suy đoán xác minh từ URL. Khi có bằng chứng đúng ID chi nhánh và quyền sử dụng, cần một thay đổi được duyệt dựa trên nguồn xác minh thực tế để cho phép ảnh tại fragment chung; phạm vi lần này không thêm cơ chế xác minh vào backend/database.

Giữ Public Premium/Cinematic, Member Navy, các route, bộ lọc, liên kết, luồng yêu thích và phần đánh giá. Chỉ placeholder chi tiết Member chuyển chiều cao cố định 360px thành khoảng responsive 240–360px.

## Kết quả kiểm tra

| Kiểm tra | Kết quả |
|---|---|
| URL nguồn ID 1–5, 3 vai trò | Đầu phiên: 15/15 HTTP 200, JPEG hợp lệ. Lần cuối: 15/15 HTTP 200, ID 1 là PNG mới; không gặp 403/404 thực tế |
| Quyền `/admin/branches` | Guest: 302 đến login; Member: 403; Admin: 200 |
| Danh sách Admin/Guest/Member × 1440/768/390px | 9/9 HTTP 200, đủ ID 1–5, không tràn ngang tài liệu |
| Chi tiết từng ID × 3 vai trò × 3 kích thước | 45/45 HTTP 200, ID đúng, không tràn ngang; trong đợt này 15 lượt Admin có URL giống snapshot trước sửa. ID 1 đổi URL ở lần đọc cuối sau đợt đó, đã kiểm tra lại danh sách/chi tiết với 3 vai trò, đều 200 và vẫn đúng chính sách |
| Guest và Member | Không có request tải 5 ảnh Cloudinary chi nhánh trên danh sách/chi tiết/homepage sau sửa |
| Gallery homepage × 3 vai trò × 3 kích thước | ID 5, 4, 3; 0 ảnh chi nhánh chưa xác minh; placeholder có nhãn; phím End đưa gallery đến cuối |
| JavaScript tắt, Guest/Member, 390px | Danh sách, chi tiết ID 1 và homepage đều 200, có thông báo, không tải ảnh chi nhánh |
| Motion thường và reduced motion | Gallery vẫn hoạt động; kiểm tra desktop motion thường với scene được enhance, nhãn vẫn có; không sửa mã animation |
| Giả lập ảnh 403 và 404 bằng interception trong browser | 5/5 preview Admin được thay bằng thông báo lỗi trong mỗi tình huống, URL vẫn giữ; không thay ảnh hoặc dữ liệu thật |
| Kiểm thử Node | 26/26 pass: `branch-media.test.cjs`, `public-directory.test.cjs`, `public-journey.test.cjs` |
| Lỗi JavaScript trên các trang kiểm tra thực tế | Không ghi nhận `pageerror` |

Inspector CLI của skill thiết kế không chạy xong do `CDP timeout: Page.enable`. Đã dùng cùng `probe`/`layout` của skill qua Playwright trên 9 cấu hình danh sách. Tất cả có overflow = 0 và brokenImages = []. Không tuyên bố đạt toàn bộ tiêu chí thiết kế: bộ chấm báo thiếu ảnh raster ở Guest desktop, coi thông báo thiếu ảnh ở mobile là eyebrow, và báo biểu tượng yêu thích/gradient header Member vốn được yêu cầu giữ. Không sửa các đặc điểm đó để đạt điểm hình thức. Đã xem ảnh từng ID qua 9 contact sheet, đồng thời kiểm tra ảnh gốc và gallery trên mobile/desktop. Không thiết kế lại trang, không thêm ảnh stock hoặc ảnh sinh mới.

## Screenshot và bằng chứng

- [Mục lục ảnh trước/sau](target/branch-image-sync/index.html).
- [Admin trước](target/branch-image-sync/before-admin-branches-1440.png) / [Admin sau](target/branch-image-sync/after-admin-branches-1440.png).
- [Guest trước](target/branch-image-sync/before-guest-branches-1440.png) / [Guest sau](target/branch-image-sync/after-guest-branches-1440.png).
- [Member trước](target/branch-image-sync/before-member-branches-1440.png) / [Member sau](target/branch-image-sync/after-member-branches-1440.png).
- [Gallery trước](target/branch-image-sync/before-guest-home-gallery-1440.png) / [Gallery sau](target/branch-image-sync/after-guest-home-gallery-1440.png) / [Gallery mobile](target/branch-image-sync/after-guest-home-gallery-390.png).
- [Đối chiếu ảnh nguồn 5 ID, chưa xác minh](target/branch-image-sync/source-images-unverified.png).
- Dữ liệu kiểm tra thô: [browser-evidence.json](target/branch-image-sync/browser-evidence.json), [inspection-metrics.json](target/branch-image-sync/inspection-metrics.json), [inspection-findings.json](target/branch-image-sync/inspection-findings.json).
- Screenshot chi tiết trước sửa: `before-{guest|member|admin}-detail-{1..5}-1440.png`; sau sửa: `after-{guest|member|admin}-detail-{1..5}-{1440|768|390}.png`, cùng thư mục bằng chứng.

Các artifact trong `target/` là bằng chứng cục bộ, có thể bị xóa khi chạy clean; không phải ảnh cơ sở mới để đưa lên website.

Nhóm `before-*`/`after-*` và contact sheet ghi lại đợt đối chiếu ban đầu khi ID 1 còn ảnh PIG GYM. Nhóm `final-*` ghi lần đọc cuối khi ID 1 đã có URL PNG mới. Dữ liệu thô tách riêng `before`, `afterDetails`, `finalSources`, `finalAccess`, `finalPages`; không trộn hai mốc dữ liệu để khẳng định URL chưa từng thay đổi.

## File thay đổi trong lần này

- Thêm `src/main/resources/templates/fragments/branch-media.html`: chính sách chung và bản xem nguồn Admin.
- Thêm `src/main/resources/static/css/utefit-branch-media.css`, `src/main/resources/static/js/utefit-branch-media.js`: tỷ lệ preview/responsive chi tiết và xử lý tải lỗi.
- Sửa `src/main/resources/templates/admin/branches/list.html`, `src/main/resources/templates/admin/branches/form.html`: dùng bản xem nguồn, nhãn và giải thích.
- Sửa `src/main/resources/templates/fragments/public-directory.html`, `src/main/resources/templates/fragments/public-journey.html`: dùng chính sách chung; homepage ngừng công bố ảnh chưa xác minh.
- Sửa `src/main/resources/templates/guest/branches.html`, `src/main/resources/templates/guest/branch-detail.html`: Member và các vai trò đăng nhập dùng fragment chung, giữ theme.
- Thêm `src/test/js/branch-media.test.cjs`; cập nhật `src/test/js/public-directory.test.cjs`, `src/test/js/public-journey.test.cjs` theo chính sách mới.
- Cập nhật `src/main/resources/static/images/public-directory-assets.md` và báo cáo này; thêm bằng chứng/scripting QA cục bộ trong `target/branch-image-sync/`.

Đã đối chiếu SHA-256 với snapshot đầu phiên: không đổi file Java/backend, database/migration hoặc các file Payment, Membership, QR, Chat, Booking. Không submit form sửa/tạo/tắt chi nhánh, không upload/xóa ảnh, không sửa database hoặc dữ liệu nguồn. Chỉ đăng nhập tài khoản demo để kiểm tra quyền. Không reset, commit, merge hoặc push Git. Các thay đổi sẵn có trong working tree được giữ.

Hoàn tất phần frontend và dừng chờ duyệt. Chưa có kết luận ảnh nào là cơ sở thật đã xác minh.
