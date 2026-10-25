# UTEFit — SePay + VietQR

## 1. Trạng thái và phạm vi

Đã bổ sung source trên `feat/giai-doan-7`, mở rộng Payment/Membership hiện có. Không tạo checkout nghiệp vụ độc lập: Member vẫn đăng ký Membership PENDING tại trang gói hiện tại, sau đó chọn phương thức SePay. Không thay QR Check-in, Favorites, Manager, Review Video hoặc luồng VNPay.

**Cập nhật 25/10/2026:** chủ dự án báo đã tự kiểm thử thành công SePay Test Mode với giao dịch mô phỏng 100.000đ: Webhook HMAC được nhận, Payment PENDING → PAID và Membership PENDING → ACTIVE. Đây là bằng chứng do người dùng cung cấp, agent không chạy lại hoặc xác minh độc lập; chưa có mã giao dịch/log đính kèm để lưu trong báo cáo. Không suy rộng thành PASS cho các ca lỗi, race condition, regression hoặc Live.

Trong lượt triển khai trước, agent chỉ xác nhận compile, không chạy Playwright, ứng dụng, Maven test, migration, mô phỏng giao dịch hoặc chuyển tiền. Các kết quả 64 test trong báo cáo Stage 7 cũ không bao gồm SePay mới. Checklist chuyển Live và rollback: [SEPAY_LIVE_CHECKLIST.md](SEPAY_LIVE_CHECKLIST.md).

Không cần URL HTTPS ngay để hoàn thiện backend. Không có URL/hostname tunnel hardcode trong controller; tạo Tunnel và cấu hình Dashboard sau cũng được.

## 2. Đường dẫn chính xác

| Mục đích | Method | Đường dẫn |
|---|---|---|
| Webhook Test | POST | `/payments/sepay/webhook/test` |
| Webhook Live | POST | `/payments/sepay/webhook/live` |
| Tạo/lấy lại Payment SePay cho Membership của Member | POST + CSRF | `/member/memberships/{membershipId}/sepay` |
| Trang VietQR của Payment thuộc Member | GET | `/member/payments/{paymentId}/sepay` |
| Poll trạng thái, chỉ đọc và kiểm tra ownership | GET | `/member/payments/{paymentId}/sepay/status` |
| Admin xem sổ giao dịch đã xác thực/đối soát | GET | `/admin/payments/sepay-reconciliation` |

Sau khi có Cloudflare Tunnel, Webhook Test là:

```text
https://<hostname-tunnel-cua-ban>/payments/sepay/webhook/test
```

Live, chỉ khi được phép bật:

```text
https://<hostname-https-live>/payments/sepay/webhook/live
```

Backend mặc định `SEPAY_ENABLED=false`, `SEPAY_ENVIRONMENT=test`, `SEPAY_LIVE_ENABLED=false`. Chỉ endpoint đúng môi trường đang cấu hình mới được xử lý; đường dẫn của môi trường còn lại trả lỗi. Không dùng GET mở URL trong browser để kết luận Webhook POST hoạt động.

## 3. Luồng và business rule

1. Dùng Membership PENDING sẵn có, xác minh Member ACTIVE/enabled và ownership; khóa User → Membership trước tạo Payment.
2. Dùng `quotedPrice` và `quotedDurationDays` của Membership. Không nhận amount/account/reference từ frontend. Đơn cũ thiếu snapshot hợp lệ bị chặn để đối soát, không lấy giá hiện tại thay thế.
3. Tạo Payment `method=SEPAY`, `status=PENDING`; lưu cùng transaction với chi tiết ngân hàng/expiry/environment/reference bất biến. Reference dùng tiền tố 2–5 chữ hoa + 24 ký tự hex ngẫu nhiên (96 bit), ví dụ cấu trúc `UTE` + 24 ký tự; không dùng ID tăng dần hoặc substring mơ hồ.
4. Retry tạo đơn khi đã có một SePay PENDING trả lại chính đơn đó. Không đổi expiry/reference/giá. CASH hoặc VNPay PENDING chặn đổi phương thức. Đơn đã có PAID hoặc Member đang có gói ACTIVE còn hạn bị chặn.
5. Hiển thị bank/account/name/amount/reference theo snapshot backend. Ảnh QR dùng `https://vietqr.app/img` với đúng bốn tham số công khai `acc`, `bank`, `amount`, `des`; không có secret/JWT/email/paymentId trong QR URL. Tên chủ tài khoản không gửi sang dịch vụ ảnh. Chính sách referrer `no-referrer`.
6. Member sao chép nội dung/số tiền; polling mỗi 5 giây khi trang đang mở và đơn còn chờ. Lỗi mạng backoff rồi dừng; chuyển tab dừng polling. Nút kiểm tra chỉ GET, không có endpoint “Member xác nhận PAID”.
7. Webhook HMAC hợp lệ mới được parse/xử lý. Chỉ tiền vào đúng tài khoản/gateway, đúng mã đã lưu và xuất hiện thành token đầy đủ duy nhất trong content, đúng amount, còn hạn, đúng trạng thái và đủ eligibility mới PAID.
8. Payment/Membership được khóa và refresh sau khi lấy khóa để không đọc lại trạng thái cũ. Chống kích hoạt gói/thông báo lần hai; kiểm tra không có Membership ACTIVE còn hạn khác trước kích hoạt. Gọi lại `MembershipPaymentService.activateMembership` để giữ quy tắc ngày snapshot và Notification hiện tại.
9. PAID không tự quay về PENDING. Không tự cộng nhiều lần chuyển thiếu thành một khoản đủ; không tự kích hoạt khi chuyển thừa. Không tự hủy SePay PENDING, kể cả hết hạn, vì vẫn có thể nhận tiền muộn cần đối soát.
10. Payment PAID tiếp tục được thống kê doanh thu bởi truy vấn hiện có. Sổ Webhook không cộng doanh thu lần thứ hai. Dữ liệu Test bắt buộc ở DB riêng để không tăng doanh thu hoặc kích hoạt Membership trên database chính bằng tiền mô phỏng.

Hạn thanh toán mặc định 15 phút, cấu hình 5–60 phút. Nếu Webhook đến sau expiry, transactionDate trước lúc tạo đơn/sau expiry hoặc quá xa tương lai, lưu REVIEW thay vì tự kích hoạt; kể cả tiền đã chuyển trước nhưng delivery đến quá muộn, Admin phải đối soát. Không coi khoản đã vào ngân hàng là “không tồn tại” chỉ vì Payment chưa PAID.

## 4. Xác thực, replay, lỗi và đối soát

Theo [tài liệu HMAC chính thức](https://developer.sepay.vn/vi/sepay-webhooks/xac-thuc), backend dùng hai header `X-SePay-Signature`, `X-SePay-Timestamp`. Chữ ký `sha256=<hex>` tính trên **bytes** của timestamp, dấu chấm và raw body; so sánh bằng `MessageDigest.isEqual`. Timestamp chênh tối đa 300 giây với Clock server. Không parse rồi serialize lại trước xác thực; header trùng bị từ chối.

Chỉ nhận JSON không nén, tối đa 32 KiB; chặn duplicate JSON keys, trailing JSON, số tiền không phải số nguyên VND dương, ID sai kiểu và chuỗi vượt giới hạn. Không dùng `accumulated`, `description` hay paymentId do người gửi thêm vào để xác nhận đơn.

Không yêu cầu cookie/JWT người dùng cho hai POST Webhook; CSRF chỉ ngoại lệ đúng hai endpoint POST này. Member POST mua gói vẫn cần CSRF; polling cần login MEMBER và ownership. Manager/PT không có quyền đối soát Admin.

Sổ `sepay_webhook_transactions` lưu dữ liệu kỹ thuật tối thiểu: SePay ID, môi trường, bank/account, bank reference, mã/nội dung, amount/direction/date, outcome/reason, hash body và fingerprint. Không lưu HMAC secret, header signature hoặc toàn bộ JSON/số dư. Dữ liệu content và tên chủ tài khoản dùng Unicode.

- `MATCHED / VERIFIED_AND_ACTIVATED`: Payment PAID + Membership ACTIVE trong cùng transaction.
- `REVIEW`: sai/thiếu/thừa amount, thiếu/sai/mơ hồ reference, tài khoản/gateway không khớp, đến muộn, account/plan/branch không hợp lệ, Membership không PENDING, đã có gói ACTIVE hoặc Payment đã được xử lý bởi giao dịch khác.
- `IGNORED / OUTGOING_TRANSFER`: giao dịch tiền ra, không kích hoạt.
- Duplicate cùng SePay ID được kiểm tra theo `(environment, transaction_id)`. Bank reference không rỗng có UNIQUE riêng theo môi trường/ngân hàng/tài khoản. Hai request đồng thời tranh unique: request thua rollback rồi kiểm tra bản ghi đã commit trước khi ACK.
- Một ID/bank reference gửi lại với nội dung khác chỉ đánh dấu conflict hash/ID để Admin kiểm tra SePay logs, không dùng giao dịch đó thanh toán đơn thứ hai. Bản ghi gốc không bị ghi lại thành một giao dịch tiền vào mới.
- Lỗi DB/transaction tạm thời trả 503 + `success:false` để retry. Sai xác thực trả 401; JSON/fields sai trả 400; quá lớn trả 413. Không ghi body/header secret vào log.
- HTTP 200 + `success:true` chỉ sau khi transaction đã commit hoặc đã xác định có dữ liệu bền vững từ lần xử lý trước. **Dashboard Webhook “thành công” không đồng nghĩa Payment đã PAID**: REVIEW/IGNORED cũng được ACK vì đã lưu để đối soát, tránh retry vô hạn lỗi nghiệp vụ.

Quy tắc ACK và ID retry dựa trên [tài liệu tích hợp Webhook](https://developer.sepay.vn/vi/sepay-webhooks/tich-hop-webhook). Sổ Admin chỉ đọc, không có nút ép PAID/refund/đóng đơn. Những trường hợp REVIEW cần đối chiếu giao dịch ngân hàng/SePay, xử lý kế toán/hoàn tiền có phê duyệt; không đề nghị sửa SQL tùy tiện để làm gói ACTIVE. Quy trình thao tác đối soát tài chính tự động ngoài các bản ghi tối thiểu chưa được bổ sung.

## 5. Cấu hình ngoài source

File mẫu `.env.sepay.example` chỉ chứa placeholder, **Spring không tự nạp dotenv**. Điền Environment variables trong Run Configuration cá nhân của IntelliJ hoặc biến môi trường hệ điều hành; không chia sẻ Run Configuration chứa secret. `.env.sepay`, `.env.sepay.local` được ignore, nhưng không vì vậy mà đặt chúng trong `src/main/resources` hay đóng gói vào JAR.

| Biến | Giá trị / mục đích |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `sepay`; không dùng profile `qa` nếu không muốn runner fixture QA cũ |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `validate`; không update/create schema chính |
| `SPRING_DATASOURCE_URL` | SQL Server URL của DB đã áp dụng schema; Test phải DB QA/Test riêng |
| `DB_USERNAME`, `DB_PASSWORD` | Credential DB riêng, theo cấu hình hiện có |
| `SEPAY_ENABLED` | Mặc định false; true sau khi bạn điền cấu hình và duyệt schema |
| `SEPAY_ENVIRONMENT` | `test` hoặc `live`, mặc định test |
| `SEPAY_LIVE_ENABLED` | Mặc định false; Live cần true rõ ràng |
| `SEPAY_TEST_QR_PREVIEW` | Mặc định false; true cho xem trước QR Test với cấu hình Test đã kiểm tra; KHÔNG chuyển tiền bằng app ngân hàng |
| `SEPAY_BANK_CODE` | Mã bank cho `vietqr.app`, xem danh sách tại `https://vietqr.app/banks.json` |
| `SEPAY_GATEWAY` | Tên gateway đúng payload SePay; có thể khác alias bank code; phải điền cho khớp, không substring |
| `SEPAY_ACCOUNT_NUMBER` | Tài khoản nhận ở môi trường đang chọn, phải khớp webhook và snapshot |
| `SEPAY_ACCOUNT_NAME` | Tên chủ tài khoản công khai trên trang; bạn phải đối chiếu với ngân hàng |
| `SEPAY_WEBHOOK_SECRET` | Secret HMAC Test, tối thiểu 32 ký tự; copy nguyên bản từ cấu hình Webhook hoặc tạo riêng rồi nhập vào Dashboard khi tạo |
| `SEPAY_LIVE_WEBHOOK_SECRET` | Secret HMAC Live riêng, tối thiểu 32 ký tự, không được giống Test |
| `SEPAY_WEBHOOK_AUTH` | `hmac`; không hỗ trợ bật No Auth để bỏ qua xác thực |
| `SEPAY_PAYMENT_PREFIX` | Mặc định `UTE`, 2–5 chữ A–Z; hậu tố cố định 24 ký tự hex |
| `SEPAY_PAYMENT_MINUTES` | Mặc định 15; từ 5 đến 60 |
| `SEPAY_TEST_DATABASE_NAME` | Tên catalog QA/Test chính xác; phải chứa QA/Test, khớp JDBC URL và catalog thực tế của connection |
| `SEPAY_COOKIE_SECURE` | false khi giao diện localhost HTTP; dùng true khi giao diện chính chạy HTTPS Live |

Các biến JWT, Mail, Cloudinary đang có giữ nguyên cách cấu hình. SePay Webhook không cần SePay API access token cho luồng này; HMAC secret không phải mật khẩu ngân hàng. Không cần cấu hình URL Webhook trong Java hay gửi URL cho agent.

## 6. Database — chỉ chuẩn bị script, chưa áp dụng

Migration mới: `database/migrations/V706__sepay_vietqr.sql`. **Không chỉnh V701–V705; không chạy script này trên bất kỳ DB nào trong lượt triển khai.**

- Thêm `SEPAY` vào CHECK của `payments.method`; không thêm/xóa giá trị PaymentStatus.
- Thêm `sepay_payment_details`: khóa chính/FK Payment 1:1, reference unique, snapshot tài khoản/ngân hàng/môi trường/expiry. Không sửa Payment/Membership đang có.
- Thêm `sepay_webhook_transactions`: ledger/audit, unique SePay ID theo môi trường, filtered unique bank reference khi có, index outcome/time và mã đối soát.
- Filtered unique index reference cho riêng Payment SEPAY, không bắt buộc các giao dịch CASH/VNPay cũ đổi mã hoặc đáp ứng unique mới.
- Script dùng transaction + XACT_ABORT. Chỉ thay CHECK method đơn giản nhận diện được; constraint phức tạp/khác mô hình làm abort để DBA duyệt, không drop mù. Không drop bảng/cột/data.
- Khi bạn áp dụng: backup, xác nhận đúng DB/các migration trước đã có (Stage 7 V701–V705 tùy trạng thái schema thực tế), chạy trên bản sao trước; kiểm tra counts/constraints/indexes. Không chạy V706 khi thiếu bảng nền. Các dữ liệu cũ không có chi tiết SePay vẫn đọc qua Payment bình thường.

**Chặn auto-DDL ngoài QA**: `application.properties` cũ đang `ddl-auto=update`. Có entity SePay mới, nên `SepaySchemaSafetyConfig` từ chối update/create trên SQL Server ngoài profile QA được guard sẵn. Đây là bảo vệ tích hợp mới khỏi tự tạo bảng khi bạn bấm Run, kể cả SePay đang disabled. Dùng profile `sepay` sẽ chọn `ddl-auto=validate`; nếu thiếu migration, startup dừng thay vì tự thay database. Không tắt guard bằng cách sửa code để bỏ qua schema.

Khôi phục migration: lỗi trong script rollback transaction. Khi đã có SePay data, giữ bảng/ledger để audit; không tự drop. Binary cũ không biết enum `PaymentMethod.SEPAY`: không revert enum rồi chạy code cũ trên DB có row SEPAY. Ưu tiên giữ code tương thích enum và tắt `SEPAY_ENABLED` khi dừng nhận đơn; lưu ý tắt endpoint làm Webhook retry và có thể bỏ lỡ khoản tiền đang về, cần cửa sổ bảo trì/đối soát đã duyệt. Không tự chuyển SEPAY thành CASH/VNPAY hoặc xóa lịch sử để rollback.

## 7. Chạy IntelliJ tại 8080

1. Bạn duyệt V706 và tự chuẩn bị schema trên database QA riêng (không dùng UTEFitDB để mô phỏng tiền Test). Có thể tái sử dụng DB QA Stage 7 hiện có nếu được phép; không cần tạo hàng loạt fixture mới.
2. Edit Configurations → Spring Boot main hiện tại → Active profiles: `sepay`.
3. Nhập biến môi trường riêng theo bảng trên. SQL URL trỏ DB QA, DB username/password đúng QA; `SEPAY_TEST_DATABASE_NAME` khớp catalog. Điền cấu hình ngân hàng Test/secret Test, sau đó đặt `SEPAY_ENABLED=true`, `SEPAY_ENVIRONMENT=test`, `SEPAY_LIVE_ENABLED=false`.
4. Profile `sepay` dùng cổng **8080**, bind `127.0.0.1`, validate schema, không chạy SQL init. Không bật profile `qa` đồng thời: profile đó có cổng/runner riêng từ lượt trước. Nếu port bận, bạn tự dừng đúng ứng dụng giữ cổng; agent không kill Java/IntelliJ trong lượt này.
5. Run main class trong IntelliJ, mở `http://localhost:8080`. Nếu schema/config thiếu, đọc lỗi và hoàn thiện thủ công; không bật `ddl-auto=update` để lách migration.
6. Backend không cần URL HTTPS để start; tạo Webhook trên SePay sau khi bạn có Tunnel. Chỉ tạo Payment để chạy kịch bản tự động khi Webhook đã sẵn sàng, tránh hết hạn trước lúc kiểm thử.

Chỉ xem trước QR Test nếu thật sự cần: `SEPAY_TEST_QR_PREVIEW=true`, dùng tài khoản Test đã đối chiếu; ảnh QR ngân hàng không tự được ngân hàng biến thành Sandbox. Test xác nhận Membership bằng tính năng mô phỏng trong SePay, tuyệt đối không chuyển tiền thật.

## 8. Dashboard SePay và Cloudflare Tunnel (bạn thực hiện sau)

1. Bật Test mode trên SePay. Tài khoản ngân hàng Live đã liên kết **không tự là tài khoản Test**; tạo/chọn tài khoản Test và kiểm tra đúng bank/account/gateway đã nhập vào UTEFit. Không giả định thông tin placeholder đã được SePay xác nhận.
2. Test mode → cấu hình mã thanh toán: nhận diện bật, prefix `UTE` (hoặc prefix bạn chọn), hậu tố min=24, max=24, kiểu số và chữ, mẫu active. Prefix ứng dụng và Dashboard phải giống nhau. Không đổi mẫu/account khi còn Payment đang chờ mà chưa đối soát.
3. Sau khi Tunnel của bạn trỏ về `http://localhost:8080`, lấy hostname HTTPS; dùng đường dẫn Test ở mục 2. Không đổi cổng app sang 8081 chỉ vì tài liệu QA Stage 7 cũ dùng 8081.
4. Dashboard Test → Tích hợp → Webhooks → thêm: chọn **Tiền vào**, tài khoản nhận cụ thể, Content-Type JSON. Để sổ ghi cả tiền sai nội dung, **không bật bỏ qua giao dịch không mã**, không lọc prefix trước khi gửi. Backend vẫn chỉ tự xác nhận mã đã lưu khớp chặt chẽ.
5. Bảo mật: HMAC-SHA256; secret bằng đúng `SEPAY_WEBHOOK_SECRET`. Bật gửi lại khi lỗi. Không chọn No Auth, kể cả Test. Không dán secret vào URL.
6. Giữ endpoint Webhook không bị Cloudflare Access/login challenge/JS challenge; TLS công khai hợp lệ. Có thể bảo vệ các phần khác theo chính sách triển khai, nhưng không làm endpoint trả redirect login cho SePay.
7. Chỉ sau khi bạn cấu hình xong mới dùng Dashboard gửi thử/mô phỏng. Payload mẫu thiếu trường hoặc không đúng mã đơn sẽ không kích hoạt gói; phản hồi hợp lệ/HTTP 200 có thể chỉ là đã ghi sổ REVIEW, hãy kiểm tra Admin.

Tài liệu: [tạo Webhook](https://developer.sepay.vn/vi/sepay-webhooks/tao-webhook), [cấu trúc mã](https://developer.sepay.vn/vi/sepay-webhooks/cau-hinh-ma-thanh-toan), [Test mode](https://developer.sepay.vn/vi/sepay-webhooks/test-mode/bat-dau-nhanh), [tạo QR](https://developer.sepay.vn/vi/sepay-webhooks/tao-qr-va-form-thanh-toan).

## 9. Checklist bạn tự kiểm thử

Các case dưới đây là checklist kiểm thử, không phải bảng PASS tự động. Chủ dự án đã báo thành công happy-path Test 100.000đ/HMAC/PAID/ACTIVE; những điều kiện khác trong từng case (notification, snapshot, duplicate, quyền, rollback, regression...) vẫn **NOT VERIFIED** nếu chưa có bằng chứng riêng. Agent chưa chạy các case này. Chỉ thao tác dữ liệu được phép trên DB QA và SePay Test mode.

| Case | Kỳ vọng cần bạn xác nhận |
|---|---|
| Member hợp lệ chưa có gói active/pending → chọn Branch/Plan → VietQR | Membership/Payment PENDING, SEPAY, giá snapshot, bank snapshot, reference riêng |
| Bấm tạo lại / reload | Dùng cùng Payment đang PENDING, không tăng số đơn, không đổi giá/expiry/reference |
| Xem/GET status Payment của Member khác hoặc PT/Guest gọi Member URL | Bị chặn; không lộ QR/trạng thái/amount |
| CASH hoặc VNPay đang PENDING → chọn SePay | Không chuyển phương thức, không tạo Payment thứ hai |
| Đơn cũ thiếu giá/thời hạn snapshot | Bị chặn, không dùng giá hiện tại |
| QR preview Test / thông tin copy | Khớp snapshot; không có secret/JWT; không dùng app ngân hàng chuyển thật |
| SePay Test mô phỏng tiền vào đúng account/gateway/reference/amount, còn hạn | Một receipt MATCHED, một Payment PAID, một Membership ACTIVE đúng thời hạn snapshot, Notification hiện có được tạo |
| Replay cùng SePay ID / hai delivery đồng thời | Chỉ một row transaction ID, một activation/thông báo; retry ACK khi đã commit |
| Bank reference giống nhưng SePay ID/code khác | Không trả tiền cho đơn thứ hai; ghi conflict để Admin kiểm tra |
| Thiếu/thừa tiền, sai/mất/mơ hồ code, hai mã trong content | REVIEW, không ACTIVE; giữ amount/reference để đối soát |
| Tiền ra, sai bank/account, account khóa/branch/plan inactive | Không tự kích hoạt; lưu outcome/reason thích hợp khi payload đã xác thực hợp lệ |
| Đến muộn, đơn hết hạn, đơn không PENDING, đã có gói ACTIVE khác | REVIEW, không kích hoạt lần nữa; không tự xóa tiền nhận được |
| Thiếu/sai HMAC, body bị sửa, timestamp quá ±300 giây, header lặp | 401, không tạo receipt hoặc cập nhật Payment |
| Payload lớn hơn 32 KiB, duplicate JSON keys, JSON lỗi, amount sai kiểu | 4xx, không PAID |
| Sai môi trường endpoint, Test URL trỏ DB chính, Live thiếu flag/secret | Bị chặn; không kích hoạt dữ liệu chính bằng mô phỏng |
| Chỉ bấm kiểm tra trạng thái, không có Webhook | Không PAID; hết hạn ẩn QR, không tự đổi Payment FAILED |
| DB lỗi hoặc transaction rollback | 503/không ACK thành công; không có PAID/ACTIVE nửa chừng, retry có thể xử lý sau |
| Admin Payment/Statistics và sổ SePay | Payment SEPAY lọc/xem bình thường; chỉ PAID là doanh thu Membership, không cộng ledger lần nữa |
| CASH/VNPay regression | Tạo/xác nhận CASH và callback VNPay như trước, không bị phương thức mới chiếm đơn |

## 10. Điều kiện chuyển Live

Chưa VERIFIED. Bạn cần hoàn thành test trên bản sao SQL Server, chứng minh schema/unique/lock/race/rollback, đối chiếu bank/account thực đã liên kết, Webhook HTTPS Live + HMAC riêng và quy trình đối soát/hoàn tiền. Review code và migration trước triển khai; không bỏ guard, không dùng payload Test kích hoạt dữ liệu chính.

Sau khi duyệt: chọn database Live đã migration có kiểm soát, cấu hình bank/account/gateway Live, `SEPAY_ENVIRONMENT=live`, `SEPAY_LIVE_ENABLED=true`, `SEPAY_ENABLED=true`, `SEPAY_LIVE_WEBHOOK_SECRET` riêng và Dashboard Live URL `/payments/sepay/webhook/live`. Nếu Member giao diện public HTTPS, dùng cookie Secure và bảo vệ TLS phù hợp. Test webhook cũ không được trỏ sang Live hoặc dùng cùng secret. Live không được agent bật trong lượt này.

## 11. File tạo mới và sửa

| Tạo mới | Mục đích |
|---|---|
| `.env.sepay.example` | Placeholder cấu hình ngoài source |
| `database/migrations/V706__sepay_vietqr.sql` | Bảng snapshot/ledger, UNIQUE/index và CHECK method |
| `src/main/java/com/utefit/config/SepayProperties.java` | Cấu hình fail-closed, tách Test/Live |
| `src/main/java/com/utefit/config/SepaySchemaSafetyConfig.java` | Chặn auto-DDL SQL Server ngoài QA |
| `src/main/java/com/utefit/controller/payment/SepayWebhookController.java` | Hai POST Webhook, raw body, giới hạn payload, ACK/retry |
| `src/main/java/com/utefit/controller/member/SepayMemberController.java` | Chọn phương thức, trang QR, API trạng thái có ownership |
| `src/main/java/com/utefit/controller/admin/SepayReconciliationController.java` | Trang đối soát chỉ Admin |
| `src/main/java/com/utefit/entity/SepayPaymentDetails.java` | Snapshot ngân hàng/reference/expiry theo Payment |
| `src/main/java/com/utefit/entity/SepayWebhookTransaction.java` | Ledger đã xác thực, outcome và conflict |
| `src/main/java/com/utefit/repository/SepayPaymentDetailsRepository.java` | Snapshot và projection định vị đơn |
| `src/main/java/com/utefit/repository/SepayWebhookTransactionRepository.java` | Idempotency và phân trang audit |
| `src/main/java/com/utefit/service/SepayDatabaseGuard.java` | Test catalog thực tế phải QA riêng |
| `src/main/java/com/utefit/service/SepayPaymentService.java` | Tạo/reuse đơn snapshot, URL QR, ownership/status |
| `src/main/java/com/utefit/service/SepayWebhookSecurity.java` | HMAC raw bytes + timestamp constant-time |
| `src/main/java/com/utefit/service/SepayWebhookPayload.java` | JSON strict, validation và fingerprint |
| `src/main/java/com/utefit/service/SepayWebhookService.java` | Receipt transaction/locks, đối chiếu, activation dùng chung |
| `src/main/resources/application-sepay.properties` | IntelliJ 8080, local bind, validate schema, logging an toàn |
| `src/main/resources/static/js/sepay-checkout.js` | Copy/poll read-only, expiry, dừng polling khi ẩn trang |
| `src/main/resources/templates/member/sepay-checkout.html` | UI QR và trạng thái, phân biệt Test/Live |
| `src/main/resources/templates/admin/payments/sepay-reconciliation.html` | UI audit, không nút ép PAID |
| `src/test/java/com/utefit/service/SepayWebhookSecurityTest.java` | Source test HMAC/JSON; chỉ compile, chưa chạy |
| `SEPAY_INTEGRATION.md` | Báo cáo, cấu hình và checklist tự kiểm thử này |

| Đã sửa | Mục đích |
|---|---|
| `.gitignore` | Bỏ qua file env SePay local, không ẩn file mẫu |
| `src/main/java/com/utefit/enums/PaymentMethod.java` | Thêm SEPAY |
| `src/main/java/com/utefit/config/SecurityConfig.java` | Chỉ ngoại lệ hai POST Webhook; giữ CSRF/RBAC còn lại |
| `src/main/java/com/utefit/service/MembershipPaymentService.java` | Chặn hủy local khi SePay PENDING có thể còn nhận tiền |
| `src/main/java/com/utefit/controller/member/MemberMembershipController.java` | Thông báo phương thức mới |
| `src/main/resources/templates/member/memberships.html` | Nút VietQR trong luồng mua hiện có |
| `src/main/resources/templates/member/payments.html` | Mở lại QR/trạng thái của Payment SEPAY |
| `src/main/resources/templates/admin/payments/list.html` | Link sổ đối soát, filter enum tự nhận SEPAY |
| `GIAI_DOAN_7.md` | Liên kết báo cáo SePay, phân biệt với kết quả cũ |
| `GIAI_DOAN_7_FILES.md` | Liên kết inventory SePay riêng |

Không sửa Payment/Membership Entity gốc, VnpayPaymentService/Gateway, AdminPaymentService/CashPaymentAdminController, Statistics, QR Check-in/Favorites/Manager/Review Video hoặc `application.properties` gốc. Dữ liệu mở rộng nằm trong hai bảng mới.

## 12. Kiểm tra và Git

Đã kiểm tra `git status` trước code: nhánh `feat/giai-doan-7`, tracked tree sạch; chỉ `.ai/`, `.playwright-mcp/`, `docs/` untracked được giữ nguyên. Mốc trước SePay: `cb4500f`; tag local `checkpoint/before-sepay-20261025`. Không push/merge/reset/clean, không stage secret/tài liệu thư mục docs.

Chỉ compile main và test source bằng Maven `test-compile -DskipTests`; không gọi Surefire/Maven test, Spring Boot run hay request thanh toán. File test mới không được đánh dấu PASS. Không cài dependency/tool mới. Migration và tương thích SQL Server runtime, security endpoint thực tế, template/polling, SePay Dashboard và giao dịch đều cần bạn tự kiểm thử theo checklist.

Kết quả compile cuối: **BUILD SUCCESS**, 247 main source files và 12 test source files compile thành công. Log local: `target/sepay-final-compile.log`. `git diff --check` không có lỗi whitespace; inventory đã đối chiếu đủ 22 file mới và 10 file sửa của phần SePay. Đây không phải kết quả Maven test hoặc kiểm chứng SQL/Webhook thực tế.

Sau khi chủ dự án báo Test Mode thành công, checkpoint LOCAL được chuẩn bị với thông điệp `feat(sepay): checkpoint VietQR payments after user-verified Test Mode`; xác định hash bằng `git log -1` sau khi commit. Baseline/tag trước SePay giữ nguyên. Checkpoint chỉ chứa allowlist source/migration/cấu hình mẫu/tài liệu gốc, không chứa `.ai/`, `.playwright-mcp/`, `docs/`, env thật hoặc dữ liệu QA. Bổ sung `SEPAY_LIVE_CHECKLIST.md` ngoài inventory triển khai ban đầu. Không push/merge.

Git không thay thế backup SQL/SePay logs. Không triển khai lại binary trước SePay lên DB đã có Payment SEPAY; phải xét enum, callback đang bay và dữ liệu tài chính trước rollback. Xem runbook rollback trong checklist Live thay vì tự restore source/drop bảng.

Theo báo cáo người dùng, cấu hình Test/Webhook đã đủ cho happy-path nói trên; không còn coi toàn bộ Test là BLOCKED. Cấu hình Live, catalog/schema Live thực tế, hostname HTTPS ổn định, thử nghiệm lỗi/đồng thời/rollback và quy trình đối soát vẫn cần chủ dự án kiểm chứng theo checklist. Agent không đọc giá trị secret, không chạy migration hoặc sửa database trong lượt checkpoint. Không có giao dịch thật nào được xác nhận, không có SePay Live VERIFIED.
