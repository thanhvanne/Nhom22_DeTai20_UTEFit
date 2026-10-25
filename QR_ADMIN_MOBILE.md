# Admin Check-in responsive và camera Safari

## Phạm vi

Nhánh `feat/giai-doan-7`, mốc trước thay đổi `5174b5b`. Không thay đổi backend QR, token một lần/120 giây, CheckinService, database hay các module thanh toán. Không tự chạy Playwright, Spring Boot hoặc request check-in. Không tạo commit/push trong lượt này.

Nguyên nhân ở source: sidebar 280px `flex-shrink-0` luôn nằm cùng hàng với nội dung, không có breakpoint/menu mobile; topbar và lưới form tiếp tục chiếm ngang. Scanner cũ yêu cầu BarcodeDetector nên không có decoder dự phòng khi API không tồn tại/không hỗ trợ QR.

## Thay đổi

- Layout Admin dùng Bootstrap 5.3 `offcanvas-lg`: dưới 992px sidebar là menu phủ mở/đóng bằng nút, backdrop hoặc Escape; từ 992px giữ sidebar laptop 280px. Dùng bundle Bootstrap sẵn có, không thêm framework UI. CSS scoped Admin, nội dung flex có min-width 0.
- Check-in: form một cột trên mobile/tablet, chia cột lại từ 1200px; padding gọn, field tối thiểu 16px trên mobile tránh tự zoom iOS, nút camera tối thiểu 44px, lịch sử cuộn ngang trong bảng thay vì ép trang.
- Scanner: getUserMedia ưu tiên camera sau, video muted/playsinline, thông báo HTTPS/quyền bị từ chối/camera bận. Ưu tiên BarcodeDetector khi hỗ trợ QR; lỗi hoặc thiếu API thì dùng jsQR 1.4.0 tự host. Thư viện được pin và kiểm tra integrity; xem `src/main/resources/static/vendor/jsqr/README.md`.
- Dừng mọi track và timer khi Dừng, quét được QR, submit, đổi chi nhánh, chuyển tab hoặc rời trang. Xử lý cả trường hợp quyền được cấp sau khi đã đóng scanner, không để camera tiếp tục chạy ngầm hoặc ghi kết quả cũ.
- Dự phòng ảnh QR (tối đa 10 MB, xử lý ngay trên thiết bị), dán mã hoặc form check-in thủ công. Không gửi ảnh lên server. Quét chỉ điền token đúng định dạng; Admin vẫn phải kiểm tra chi nhánh và nhấn Xác nhận QR, backend vẫn quyết định tính hợp lệ.

Nguồn API: [MDN BarcodeDetector](https://developer.mozilla.org/en-US/docs/Web/API/BarcodeDetector), [MDN getUserMedia](https://developer.mozilla.org/en-US/docs/Web/API/MediaDevices/getUserMedia); nguồn decoder: [jsQR](https://github.com/cozmo/jsQR). Không tuyên bố đã kiểm chứng camera Safari trên iPhone thật trong lượt sửa source.

## Khởi động lại và kiểm thử thủ công

1. IntelliJ: dừng Spring Boot đang chạy bằng Stop, giữ cấu hình/profile/database an toàn hiện có và port 8080; chạy lại Run configuration cũ. Không đổi env QA/SePay, không chạy migration. Không cần thay URL Webhook/Tunnel vì các path backend không đổi.
2. Giữ Cloudflare Tunnel về `http://127.0.0.1:8080`; mở URL HTTPS bằng Safari trực tiếp, đăng nhập Admin → Check-in. Reload để tải CSS/JS mới, nếu cache cũ còn thì đóng tab/mở lại hoặc xóa dữ liệu website sau khi lưu công việc.
3. iPhone dọc và ngang: menu mặc định đóng, nội dung rộng toàn màn hình; thử mở menu, đóng bằng X/backdrop, cuộn menu dài. Form/nút đầy đủ, lịch sử chỉ cuộn trong bảng. Android Chrome và laptop 1024/1366px kiểm tra tương tự; desktop sidebar vẫn hiển thị.
4. Chọn chi nhánh → Mở camera → Cho phép. Dùng thiết bị khác hiển thị QR Member mới (120 giây), giữ toàn bộ QR trong khung; sau đọc camera tắt, token được điền, chưa gửi check-in tự động. Chỉ xác nhận dữ liệu test được phép; quét lại token đã dùng/hết hạn phải bị backend từ chối như trước.
5. Thử từ chối quyền: hiển thị hướng dẫn và vẫn nhập mã/chọn ảnh được. Trong Safari, cấp lại quyền Camera tại cài đặt website của hostname Tunnel rồi mở lại scanner. Thử camera đang bận/không có camera, Stop lúc đang hỏi quyền, chuyển tab, khóa màn hình, trở lại và mở lại.
6. Thử đọc ảnh PNG/JPEG QR rõ nét, ảnh QR khác/ảnh lỗi/file quá 10 MB; không được tự check-in hoặc chấp nhận QR ngân hàng. Đổi chi nhánh phải dừng camera và xóa token đã đọc.
7. Khi QR hết hạn trong lúc thao tác, Member làm mới mã rồi Admin quét lại; không tăng TTL hay bỏ quy tắc một lần để làm demo.

## Kiểm tra tự động không cần browser/DB

```text
node --check src/main/resources/static/js/qr-scanner.js
node --test src/test/js/qr-scanner.test.cjs
git diff --check
```

Unit tests mô phỏng DOM/media API: fallback, quyền, race khi đóng/mở, cleanup, ảnh và hash bundle. Không thay thế kiểm thử responsive, cấp quyền và camera thực trên iPhone/Android. Chưa khẳng định PASS trên thiết bị thật.

Kết quả lượt sửa: `node --check` PASS; `node --test src/test/js/qr-scanner.test.cjs` **12/12 PASS**; `git diff --check` PASS. Không chạy Maven/Spring Boot vì chỉ đổi template/static và các lệnh đó không kiểm chứng camera Safari. Không dùng file `target/gd7-qa-env.ps1`, không truy cập database hoặc bật thanh toán.
