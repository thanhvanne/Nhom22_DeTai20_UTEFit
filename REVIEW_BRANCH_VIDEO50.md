# Review chi nhánh và video 50 MiB

## Phạm vi

Nhánh `feat/giai-doan-7`, mốc khôi phục trước thay đổi `a8798f7` (checkpoint QR). Tracked tree sạch trước khi bắt đầu; `.ai/`, `.playwright-mcp/`, `docs/` giữ nguyên ngoài phạm vi. Không commit/push/merge, không chạy ứng dụng/Playwright, không kết nối hoặc chạy migration SQL Server. **Không cần migration**: dùng lại bảng Review và các URL/media hiện có.

## Review trong chi tiết chi nhánh

- URL: `http://localhost:8080/branches/<id>#branch-reviews`. Trên Tunnel, thay origin localhost bằng hostname HTTPS hiện có. Qua UI: Chi nhánh → chọn chi nhánh → mục **Đánh giá hội viên**.
- Hiển thị điểm trung bình một chữ số thập phân, tổng đánh giá công khai và tối đa **5** đánh giá mới nhất (createdAt DESC, id DESC), ảnh và video HTML5 controls/playsinline/preload=none. Không tự phát video.
- Nút **Xem tất cả đánh giá** dẫn đến `/reviews?branchId=<id>`. `/reviews` (9 mục/trang) và Trang chủ (3 mới nhất) giữ nguyên cách truy vấn/hiển thị.
- Cả summary và danh sách chỉ dùng `visible=true` và đúng branchId. Admin ẩn review thì lần tải lại tiếp theo loại khỏi cả danh sách, tổng và trung bình; không có push tự xóa nội dung trên tab đã mở.
- Tính trên mọi review công khai gắn với chi nhánh, kể cả review PT tại chi nhánh, nhất quán bộ lọc `/reviews?branchId=...`; không tự thêm chính sách duyệt/ẩn mới.
- Tên Member dùng DTO công khai hiện có (firstName, mặc định “Hội viên”), không xuất email/điện thoại/họ hoặc entity User. Có tên PT công khai nếu review nhắm tới PT. Nội dung dùng `th:text`, không render HTML tùy ý.
- Không có review: average null, tổng 0, trạng thái trống rõ ràng, không hiển thị điểm 0/5 giả.
- Hai query cho nhánh có dữ liệu: một aggregate COUNT/AVG trên review công khai; một danh sách giới hạn 5, EntityGraph branch/user/trainer/trainer.user tránh N+1. Không tải toàn bộ review để tính trung bình trong Java. Test kiểm tra số statement thực tế trên H2.
- Guest và Member đều xem được qua quyền public `/branches/**` sẵn có; không sửa quyền tạo/sửa/ẩn review.

Ẩn review chỉ ngừng hiển thị qua website; không thu hồi URL Cloudinary đã được chia sẻ trước đó (chính sách hiện tại giữ nguyên).

## Giới hạn upload

| Tầng | Giới hạn/quy tắc |
|---|---|
| Mỗi video | **50 MiB = 52.428.800 byte**, đúng ngưỡng được nhận, hơn 1 byte bị từ chối |
| Video frontend | File picker MP4/WebM, data-max-bytes và JS kiểm tra size/MIME/extension; chạy cả ở form sửa có target disabled |
| Backend | VideoValidator kiểm tra size, MIME, extension và signature MP4/WebM; Cloudinary vẫn phải xác nhận loại video, kích thước khung, thời lượng, định dạng và bytes hợp lệ |
| Ảnh | ImageValidator **vẫn 5 MiB**, JPEG/PNG/GIF và kiểm tra nội dung; không tăng giới hạn ảnh |
| Multipart từng file | `spring.servlet.multipart.max-file-size=50MB` (Spring DataSize theo 1024) |
| Multipart cả request | `spring.servlet.multipart.max-request-size=70MB` = 70 MiB |

Form hiện có đúng một ảnh và một video; tổng tối đa hợp lệ khoảng 55 MiB + overhead, nên chọn request 70 MiB. Bean ReviewMultipartConfig trước đây hardcode 20/26 MiB, ghi đè properties; nay lấy MultipartProperties đã bind để cấu hình thực tế nhất quán. Profile `qa`/`sepay` không ghi đè hai giới hạn nên kế thừa base; test kiểm tra cả file cấu hình và MultipartConfigElement thực tế. Nếu IntelliJ/env/command line đặt `SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE` hoặc `SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE`, cần kiểm tra override khi chạy; repo không tự thay env thật.

Form hướng dẫn, lỗi 413 và ReviewUploadAdvice đều đổi sang 50/5/70 MiB. Không còn giới hạn/hướng dẫn 20 MB hoặc 26 MB trong source runtime. Các báo cáo lịch sử giai đoạn trước có thể vẫn mô tả mức cũ, không phải cấu hình đang chạy.

## Cloudinary, lỗi và giới hạn bên ngoài

Giữ SDK Cloudinary Java 2.3.0 và luồng `resource_type=video`, MP4/WebM, không đổi videoUrl/publicId của review cũ. Kiểm tra metadata provider tự dùng MAX_BYTES mới. Theo [tài liệu Java chính thức](https://cloudinary.com/documentation/java_image_and_video_upload#java_video_upload), upload thường hỗ trợ tới 100 MB; 50 MiB chưa cần chuyển sang UploadLarge. **Không đồng nghĩa tài khoản hiện tại có quota/file-size limit đủ**, chủ dự án phải kiểm tra Dashboard của mình.

- Upload mới thất bại: transaction rollback, review cũ/ảnh/video cũ không bị thay thế. Rollback dọn media mới; chỉ sau commit mới dọn media bị thay/xóa theo thao tác sửa review hiện có. Không thực hiện xóa asset thật trong lượt phát triển này; test dùng mock.
- Cleanup Cloudinary vẫn best-effort như kiến trúc hiện có: nếu mạng/quota làm cleanup thất bại cần kiểm tra log và đối soát orphan asset; không thể coi SQL transaction là transaction nguyên tử với dịch vụ Cloudinary. Timeout có kết quả upload không xác định cũng cần đối soát, không tự xóa video cũ hoặc ép DB.
- Nếu browser/Tunnel timeout, backend có thể vẫn hoàn tất; mở lại `/member/reviews` để kiểm tra trước khi gửi lại. Kiểm tra giới hạn body proxy >= request hợp lệ, timeout bao phủ cả nhận file + upload/xử lý provider, và dung lượng temp disk.
- SDK hiện dùng `file.getBytes()`: một upload 50 MiB tạo thêm bộ đệm RAM khoảng kích thước đó, cùng overhead HTTP/provider. Theo dõi heap, concurrency và quota khi demo; không tự tăng heap/timeouts hoặc đổi hạ tầng production. Nếu cần nhiều upload đồng thời, đánh giá streaming/chunked riêng thay vì vô hiệu validation.
- Không thêm API key/secret, gọi Cloudinary thật hoặc thay upload preset/account. Chủ dự án tự test 50 MiB trên QA; chưa xác nhận mức này hoạt động thực tế qua Tunnel/account hiện tại.

## Inventory

| File sửa | Mục đích |
|---|---|
| `src/main/java/com/utefit/controller/member/PublicCatalogController.java` | Cấp model Review cho branch detail |
| `src/main/java/com/utefit/service/PublicReviewService.java` | DTO summary/latest, tái sử dụng DTO công khai |
| `src/main/java/com/utefit/repository/ReviewRepository.java` | Aggregate visible theo branch và query giới hạn/EntityGraph |
| `src/main/resources/templates/guest/branch-detail.html` | Section điểm/tổng/danh sách/media/empty/link |
| `src/main/resources/static/css/reviews.css` | Style scoped section chi nhánh, responsive |
| `src/main/java/com/utefit/validation/VideoValidator.java` | MAX_BYTES 50 MiB và lỗi rõ ràng |
| `src/main/java/com/utefit/config/ReviewMultipartConfig.java` | Dùng MultipartProperties thay hardcode |
| `src/main/resources/application.properties` | Chỉ đổi hai multipart properties 50/70MB |
| `src/main/resources/templates/member/review-form.html` | Giới hạn/hướng dẫn và lỗi frontend |
| `src/main/resources/static/js/reviews.js` | Video validation độc lập với bộ lọc PT hiện có |
| `src/main/java/com/utefit/controller/member/ReviewUploadAdvice.java` | Thông báo dung lượng mới |
| `src/main/resources/templates/error/413.html` | Lỗi HTML 50/5/70 MiB |
| `src/test/resources/application.properties` | Multipart trong H2 test khớp cấu hình runtime |
| `src/test/java/com/utefit/validation/VideoValidatorTest.java` | Biên dưới/đúng/vượt MAX_BYTES, MIME/header |
| `src/test/java/com/utefit/service/CloudinaryVideoTest.java` | Biên metadata provider và cleanup |
| `src/test/java/com/utefit/service/ReviewVideoIntegrationTest.java` | Lỗi mới, rollback ảnh/video, regression |

File mới: `src/test/java/com/utefit/service/BranchPublicReviewIntegrationTest.java`, `src/test/java/com/utefit/validation/ReviewUploadConfigurationTest.java`, `src/test/js/reviews-upload.test.cjs` và báo cáo này.

Đã kiểm tra `guest/reviews.html` và CloudinaryServiceImpl: không cần sửa vì đã render video và kiểm tra qua VideoValidator.MAX_BYTES. Không sửa profile sepay, SecurityConfig, entity, migration hoặc source QR/Payment/Membership/Favorites/Manager/Statistics.

## Demo và kiểm thử thực tế

1. IntelliJ Stop → Run lại cấu hình hiện tại trên 8080, giữ DB QA và credentials ngoài source. Không cần migration; không đổi sang DB chính hoặc chạy script V707 lần nữa cho tính năng này.
2. Guest → Chi nhánh → chi nhánh đã có review: kiểm tra điểm/tổng, tối đa 5 review, ảnh/video cũ, nút Xem tất cả giữ đúng branchId. Chọn chi nhánh chưa có review để kiểm tra trạng thái trống.
3. Đăng nhập Member khác: mở cùng trang phải thấy nội dung công khai giống Guest, không thấy thông tin riêng tư của người viết.
4. Admin ẩn một review → reload trang chi nhánh và `/reviews?branchId=...`: review không còn, điểm/tổng cập nhật; tab cũ không được cam kết tự xóa tức thời.
5. Member đủ điều kiện → `/member/reviews` → tạo/sửa: dùng MP4/WebM thật dưới 50 MiB, đúng 52.428.800 byte nếu có file hợp lệ, hơn ngưỡng và file giả MIME/extension. Không chỉ đổi extension hoặc thêm byte rác rồi coi đó là video hợp lệ.
6. Upload video hợp lệ cùng ảnh <=5 MiB, giữ ảnh/video cũ khi không chọn thay; thử ảnh >5 MiB vẫn phải bị chặn. Kiểm tra form sửa cũng có validation video.
7. Nếu Cloudinary/Tunnel lỗi, kiểm tra lại review trước retry. Không dùng SQL để giả trạng thái thành công, không xóa video cũ để chữa lỗi upload.

Boundary tests tự động dùng MultipartFile/metadata giả lập và header container, không thay thế video 50 MiB được provider decode thật.

## Kết quả kiểm tra thực tế

- Maven compile/test: BUILD SUCCESS, 84 test PASS, không failure/error/skipped. Chạy riêng H2 với MODE=MSSQLServer; không kết nối hoặc sửa database SQL Server.
- Trong 84 test Java: 30 test Review/upload/config và 54 test regression QR/Manager.
- Node test: 34/34 PASS, gồm 4 test validation Review mới và 30 test regression QR Member/camera scanner.
- JavaScript syntax check và `git diff --check`: PASS.
- Chưa chạy Playwright, chưa upload Cloudinary thật, chưa kiểm thử video 50 MiB qua Tunnel/iPhone. Các bước này dành cho người dùng kiểm thử trên QA.
- Không cần migration; không commit, push hoặc merge. Giữ HEAD `a8798f7` trên `feat/giai-doan-7`.

## Checkpoint sau khi người dùng kiểm thử

Người dùng xác nhận đã kiểm thử thành công Review tại chi nhánh và video 50 MiB, yêu cầu tạo checkpoint local với message `feat(review): add branch reviews and support 50MiB video uploads`.

Để không đưa lại credential có sẵn vào snapshot mới, bản `application.properties` trong commit dùng biến môi trường cho datasource, mail, JWT và Cloudinary: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `UTEFIT_MAIL_FROM`, `UTEFIT_JWT_SECRET_BASE64`, `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`. Cấu hình local đang chạy được giữ nguyên ngoài staging và sẽ hiện modified sau commit; không dùng `git restore` nếu chưa sao lưu cấu hình local an toàn. Khi checkout ở máy khác, phải cung cấp các biến này qua môi trường/IntelliJ.

Việc này không xóa credential đã tồn tại trong lịch sử Git trước đây; nên thu hồi/đổi credential đã từng được lưu trong repository theo quy trình riêng. Không chỉnh sửa lịch sử hoặc commit QR `a8798f7`, không push/merge, không thay đổi database.
