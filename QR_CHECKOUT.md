# QR Check-in và Check-out — UTEFit

## 1. Phạm vi và mốc khôi phục

Chỉ nhánh `feat/giai-doan-7`. HEAD trước lượt này: `5174b5b`. Các cải tiến QR responsive/Safari/polling/render trước đó chưa commit, đã được bảo vệ trước khi code:

- `target/qr-before-checkout-20261026-source.zip`: 21 file, giữ nguyên đường dẫn tương đối; SHA256 `FCA6B212FC89DF87452BCA4676519E15ECEFD96A18005DE207617EBF910A3891`.
- `target/qr-before-checkout-20261026.patch`: diff tracked so với HEAD; zip chứa cả file mới chưa tracked. Zip khác không có hậu tố `-source` là bản phụ, không dùng để khôi phục đường dẫn.

Bản sao cùng hash đã lưu thêm tại `.ai/checkpoints/qr-before-checkout-20261026/` (zip + patch) để không mất khi Maven clean xóa `target`. Không đưa thư mục `.ai` hoặc các backup này vào commit tự động.

Không tự commit/push/merge/reset/clean, không đọc env QA hoặc chạy server/Playwright trong lượt này. Không chạy migration trên QA hoặc DB chính. SQL Server runtime/demo **chờ chủ dự án áp dụng V707 và restart**; kết quả H2 không thay thế việc kiểm chứng migration SQL Server/camera thật.

## 2. Phân biệt mục đích và xử lý giao dịch

- Cùng bảng `qr_checkin_tokens`, thêm `purpose` (CHECK_IN/CHECK_OUT), FK `checkin_id` chỉ cho CHECK_OUT. Mọi token cũ mặc định CHECK_IN; không chuyển token Check-in thành Checkout dựa vào việc đang có phiên mở.
- CHECK_IN giữ format `UTEFITQR.` + 43 ký tự base64url; CHECK_OUT dùng `UTEFITQO.` + nonce cùng độ dài. Nonce 32 byte SecureRandom, chỉ lưu SHA256. Prefix riêng giúp bản scanner/backend cũ từ chối QR Checkout khi rollback; **backend vẫn kiểm tra purpose lưu trong DB và prefix khớp, không tin frontend**.
- Hai loại giữ TTL 120 giây, một lần, tokenVersion và thu hồi mã chưa dùng khi tạo mới. Không chứa userId/email/JWT trong QR, không ghi raw token vào log. Tạo mã không thay đổi lịch sử vào/ra.
- Tạo CHECK_IN chỉ khi không có phiên mở. Tạo CHECK_OUT chỉ khi có phiên mở của chính Principal Member; token gắn cố định với ID lượt đó. Backend khóa User trước kiểm tra/tạo mã để đồng bộ với quét và checkout thủ công.
- Quét: User → token, kiểm tra còn hạn/chưa dùng/version/tài khoản/purpose. CHECK_IN gọi `AdminCheckinService.checkIn` như cũ. CHECK_OUT kiểm tra đúng Member, đúng ID phiên đang mở và đúng chi nhánh rồi gọi **`AdminCheckinService.checkOut` hiện có**. Không nhân bản logic ghi giờ ra hoặc tạo Checkin mới.
- `checkOut` hiện có ghi `check_out_at` bằng thời gian server, cùng transaction với `used_at` của token; lỗi/race rollback cả hai. Token cũ không được đóng lượt khác sau manual checkout và check-in lại.
- Admin có RBAC và kiểm tra tài khoản hoạt động; Manager dùng `ManagerAccessService.requireBranch` có khóa/quyền hiện tại trong transaction trước consume. Token sai chi nhánh/sai quyền không bị đốt.
- Kết quả hiển thị loại thao tác, tên Member, chi nhánh và giờ server; không hiển thị token. Checkout thủ công Admin/Manager giữ nguyên.

## 3. Quy tắc khi hết hạn gói hoặc khóa tài khoản

- Gói hết hạn trong lúc đang tập không cản tạo/quét QR Checkout: đây là kết thúc lượt đang mở, không phải cấp quyền vào mới. Nhánh bị ngừng hoạt động cũng không bị kiểm tra lại tại service checkout; nếu UI quầy không chọn được nhánh đó, dùng checkout thủ công để xử lý.
- Member bị khóa/disabled/đổi role: không cấp hoặc consume QR tự động, kể cả Checkout. Nhân viên phải xác minh trực tiếp và dùng checkout thủ công, không sửa trạng thái tài khoản/Membership để lách quy tắc. Không dùng phần mềm để ngăn người đang ở phòng tập ra về.
- Đăng xuất/revoke tokenVersion làm QR cũ vô hiệu như trước. Member còn phiên mở sau đăng nhập lại tạo QR Checkout mới.

## 4. Endpoint và UI

| Endpoint | Chức năng |
|---|---|
| GET `/member/qr-checkin` | Trang cá nhân hiện có, trạng thái/nút vào–ra |
| POST `/member/qr-checkin` | Cấp CHECK_IN, Member + CSRF |
| POST `/member/qr-checkin/checkout` | Cấp CHECK_OUT gắn phiên mở, Member + CSRF |
| GET `/member/qr-checkin/status?tokenId=...` | Poll chỉ đọc, ownership; thêm `qrPurpose`, `canCheckout` |
| POST `/admin/checkins/qr` | Cùng scanner xác nhận hai loại QR; Admin + CSRF |
| POST `/manager/checkins/qr` | Cùng scanner, chỉ chi nhánh được giao; Manager + CSRF |

Member đang tập: nút Check-in bị khóa, nút QR Check-out được bật. QR Checkout hợp lệ vẫn hiển thị/countdown trong trạng thái OPEN, không bị nhầm với QR Check-in đã dùng. Sau consume/manual checkout: ẩn QR, dừng countdown, hiển thị “Check-out thành công — Đã rời phòng tập”, mở tạo Check-in mới. Poll 4 giây và retry/fallback render vẫn giữ. Các thuộc tính mới của UI truyền qua model riêng, không truy cập field mới trên record cũ một cách không bảo vệ khi đang triển khai.

Admin giữ responsive/layout hiện có. Manager dùng cùng jsQR tự host và thêm đọc QR từ ảnh như Admin; không gửi ảnh lên server. Cả hai dùng cùng camera/getUserMedia/Safari fallback, không có lựa chọn frontend để ép mục đích token.

## 5. Migration SQL Server — chỉ chủ dự án tự áp dụng

File mới: **`database/migrations/V707__qr_checkout.sql`**; không sửa V701–V706.

1. Stop ứng dụng, xác nhận catalog QA trước, backup DB và lưu source/artifact/config hiện tại (secret ngoài Git). Diễn tập restore backup trên bản sao; không áp dụng vào DB chính khi chưa duyệt.
2. Review V707, tiền đề đã có `checkins` và `qr_checkin_tokens` (V702). Chạy script trên catalog được phép bằng tài khoản migration riêng. Script có transaction/XACT_ABORT/TRY-CATCH rollback.
3. Script bổ sung `purpose VARCHAR(16) NOT NULL DEFAULT CHECK_IN WITH VALUES`, `checkin_id BIGINT NULL`, FK `fk_qr_token_checkin`, CHECK `ck_qr_token_purpose_target`, filtered index `idx_qr_token_checkin`. Các token cũ chỉ bổ sung purpose; không sửa lịch sử Checkin hoặc xóa token.
4. Xác minh PK/FK/CHECK được bật/trusted, đúng cột/kiểu/index/default; script có điều kiện tồn tại không thay thế kiểm tra schema từng được sửa tay. Kiểm tra row count/check_out_at lịch sử không đổi.
5. Chạy IntelliJ với `ddl-auto=validate` và cấu hình/database QA đúng, port 8080. **Không dùng `qa` + ddl-auto=update để tự tạo schema thay migration.** Giữ cấu hình SePay/VNPay/Cloudflare hiện có.

Rollback ứng dụng: bảo vệ mọi thay đổi sau lượt này trước; giải nén snapshot sang thư mục tạm để review, không ghi đè mù. File QR có sẵn lấy từ snapshot (không lấy từ HEAD vì sẽ mất polling/responsive cũ); các tracked file chỉ mới đổi trong lượt này có thể lấy riêng từ `5174b5b` sau khi duyệt. File mới V707/enum/report xử lý từng file có kiểm soát, không clean/reset. Giữ cột/bảng và lịch sử SQL; không drop schema hoặc restore DB mù sau khi đã có giao dịch. QR Checkout prefix riêng sẽ bị bản cũ từ chối; dùng checkout thủ công cho phiên còn mở. Đợi/thu hồi mã đang bay trước thay phiên bản theo kế hoạch vận hành được duyệt.

## 6. Demo sau migration và restart

1. Laptop đăng nhập Member QA có gói hợp lệ → trang QR → Tạo QR Check-in.
2. iPhone Safari HTTPS Tunnel đăng nhập Admin (phiên riêng) → Check-in → chọn chi nhánh → mở camera/cho phép → quét → Xác nhận QR. Xem tên, chi nhánh, giờ và thông báo Check-in thành công.
3. Laptop trong khoảng 4 giây thấy Đang tập, QR vào biến mất; không thể tạo Check-in mới. Bấm **Tạo / làm mới QR Check-out**: QR mới ghi rõ CHECK-OUT, đếm 120 giây; lượt tập vẫn OPEN.
4. Admin cùng camera/chi nhánh quét mã mới → Xác nhận QR. Thấy Check-out thành công. Không cần tìm Member trong bảng.
5. Laptop tự thấy Đã rời phòng tập, QR ra biến mất, countdown dừng; có thể tạo QR Check-in cho lượt sau. Lịch sử có cùng bản ghi được bổ sung giờ ra, không thêm lượt giả.
6. Manager được Admin giao chi nhánh đăng nhập `/manager`, chọn chi nhánh đó và thử cùng luồng. Đổi branchId ngoài scope phải 403; sai chi nhánh của lượt tập phải bị chặn, QR đúng vẫn dùng được.
7. Thử hết hạn/refresh/replay/ảnh/nhập mã/đổi tab/offline/Thử lại và fallback checkout thủ công bằng dữ liệu test được phép. Không tự tạo giao dịch SePay hoặc sửa SQL để mô phỏng.

## 7. File và kiểm thử

### Tạo mới trong lượt Checkout

| File | Mục đích |
|---|---|
| `src/main/java/com/utefit/enums/QrCheckinPurpose.java` | Enum mục đích CHECK_IN/CHECK_OUT |
| `database/migrations/V707__qr_checkout.sql` | Schema bổ sung, không tự thực thi |
| `QR_CHECKOUT.md` | Báo cáo, migration, demo và khôi phục |

### Sửa trong lượt Checkout

| File | Mục đích |
|---|---|
| `src/main/java/com/utefit/entity/QrCheckinToken.java` | Purpose bất biến và FK phiên Checkout |
| `src/main/java/com/utefit/service/QrCheckinService.java` | Issue/consume hai loại, gắn phiên, Result và xác minh Admin |
| `src/main/java/com/utefit/service/MemberQrStatusService.java` | Purpose/canCheckout và vô hiệu mã ra thuộc phiên cũ |
| `src/main/java/com/utefit/service/ManagerOperationsService.java` | Trả Result sau scope check hiện có |
| `src/main/java/com/utefit/controller/member/MemberQrCheckinController.java` | Endpoint cấp Checkout và model có fallback |
| `src/main/java/com/utefit/controller/admin/AdminCheckinController.java` | Quét theo Principal Admin, thông báo đúng thao tác |
| `src/main/java/com/utefit/controller/manager/ManagerController.java` | Thông báo Result trong scope |
| `src/main/resources/templates/member/qr-checkin.html` | Nút và nhãn QR Checkout, giữ null-safe/render fallback |
| `src/main/resources/templates/admin/checkins/list.html` | Nhãn quét chung, giữ responsive và nhập/ảnh |
| `src/main/resources/templates/manager/index.html` | Quét chung, jsQR tự host và ảnh dự phòng |
| `src/main/resources/static/js/qr-member.js` | Polling/đếm ngược QR ra khi OPEN, chuyển nút sau Checkout |
| `src/main/resources/static/js/qr-scanner.js` | Nhận thêm prefix ra, không quyết định thao tác backend |
| `src/test/java/com/utefit/service/QrCheckinIntegrationTest.java` | Lifecycle, expiry, race, scope/RBAC và JSON chỉ đọc |
| `src/test/js/qr-member.test.cjs` | UI nút/QR ra/countdown/cleanup |
| `src/test/js/qr-scanner.test.cjs` | Đọc prefix Checkout qua fallback Safari giả lập |
| `QR_MEMBER_STATUS.md` | Liên kết cập nhật tài liệu sau nâng cấp |

Không sửa `AdminCheckinService`, `Checkin` entity, SecurityConfig, SePay/VNPay/Payment/Membership/Favorites/Review/Statistics hoặc V701–V706. Các repo/layout/CSS/vendor/test source mới từ các lượt QR trước vẫn được giữ, không tạo lại hoặc loại bỏ.

### Kết quả thực tế

| Kiểm tra | Kết quả |
|---|---|
| Maven compile + test chọn lọc | **BUILD SUCCESS — 54/54 PASS**, không skip |
| `QrCheckinIntegrationTest` | 32 PASS: gồm ca cũ và Checkout → lần vào mới, expiry/replay/sai nhánh, chống đổi prefix/purpose/nhầm phiên/Member, concurrency, expired Membership, khóa tài khoản, Admin/Manager/revoke/CSRF/status chỉ đọc |
| `ManagerIntegrationTest` | 9 PASS regression scope/quyền Manager cũ |
| `MemberQrStatusServiceTest` | 8 PASS regression trạng thái/ownership |
| `MemberQrPageFailureTest` | 5 PASS regression lỗi DB/model null/render không trống |
| Node unit tests | **30/30 PASS**: 17 Member và 13 scanner, gồm QR Checkout |
| JavaScript syntax / whitespace | PASS |
| V707 trên SQL Server, schema validate thật | **NOT RUN**, chủ dự án tự review/áp dụng; H2 không kiểm chứng script T-SQL |
| Camera iPhone/Manager browser và E2E thật | **NOT RUN**, không Playwright |

Lệnh Maven chạy offline từ cache có sẵn, chọn đúng bốn test trên, ép JDBC `jdbc:h2:mem:qr_checkout_tests;MODE=MSSQLServer;DB_CLOSE_DELAY=-1`, driver H2 và không active profile QA/runtime. Không mở SQL Server, tạo fixture QA hoặc nhận giao dịch ngân hàng. Log cuối: `target/qr-checkout-tests.log`. Node: `node --test src/test/js/qr-member.test.cjs src/test/js/qr-scanner.test.cjs`.

Một lượt test phát hiện expiry nanosecond bị DB timestamp làm tròn vượt mốc 120 giây một phần mili giây. Đã chuẩn hóa **lúc cấp mã mới** về mili giây rồi cộng đúng 120 giây, khớp API/countdown và độ chính xác SQL/H2; không tăng TTL hoặc sửa timestamp token/lịch sử cũ. Test biên dùng nano cố định để không còn phụ thuộc ngẫu nhiên làm tròn. Lượt chạy cuối 54 ca đã PASS sau chỉnh sửa này.

Git cuối lượt: vẫn `feat/giai-doan-7`, HEAD `5174b5b`, code Checkout ở working tree cùng các cải tiến QR trước đó. Không tạo commit mới tự động, không stage/push/merge. Mốc khôi phục các cải tiến chưa commit là zip + patch nói ở mục 1, không phải chỉ HEAD.
