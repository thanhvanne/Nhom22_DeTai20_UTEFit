# UTEFit — SePay Live BIDV Virtual Account

Phạm vi: sửa project hiện tại trên `feat/giai-doan-7`, giữ HEAD `2dadc44`, checkpoint QR `a8798f7` và tất cả thay đổi chưa commit. Không tạo project/database, không kết nối SQL Server, không chạy migration, không đổi approval/cờ Live, không gửi request tới ứng dụng8080/Tunnel/SePay/ngân hàng, không tạo giao dịch hoặc commit/push.

## 1. Nguyên nhân và thay đổi

Source trước đây chỉ snapshot account_number, dùng nó cho QR và đối chiếu webhook.accountNumber; parser bỏ qua subAccount. Vì vậy chưa phân biệt tài khoản chính và VA BIDV. BIDV cần VA cho luồng Webhook; tài khoản chính và VA có vai trò khác nhau. Đối chiếu [SePay tài khoản/VA](https://developer.sepay.vn/vi/sepay-webhooks/tai-khoan-ngan-hang), [payload](https://developer.sepay.vn/vi/sepay-webhooks/tich-hop-webhook).

Hiện tại:
- **SEPAY_LIVE_ACCOUNT_NUMBER** vẫn là tài khoản chính, đối chiếu accountNumber.
- **SEPAY_LIVE_VA_NUMBER** là biến mới, nhập VA đã liên kết tại IntelliJ, không hardcode vào Java/HTML/config thực thi.
- Live BIDV (bank code hoặc gateway BIDV) thiếu VA/sai định dạng/VA bằng tài khoản chính/placeholder → fail closed. Các guard profile/catalog/approval/HMAC/cookie/TLS không bị nới.
- Tạo Payment Live mới snapshot cả main account và VA (`va_number`, bất biến qua JPA). Giá, reference prefix+24 hex, expiry và Membership snapshot giữ nguyên.
- URL VietQR dùng **acc=VA snapshot**, không dùng main; UI Member hiển thị VA cùng giá/reference backend. Không lấy VA hoặc amount từ frontend. URL chuẩn theo [SePay QR](https://developer.sepay.vn/vi/sepay-webhooks/tao-qr-va-form-thanh-toan).
- Payload parse riêng accountNumber/subAccount, giới hạn50 ký tự, yêu cầu kiểu string nếu có, không đổi hoa/thường VA. Thiếu/null/rỗng subAccount trở thành giá trị rỗng để đối soát; trường sai kiểu bị400.
- Webhook đã xác thực chỉ MATCHED khi main/gateway đúng, VA payload đúng snapshot và cấu hình, cùng các điều kiện reference/amount/in/time/status/eligibility/idempotency trước đây. Sai VA → REVIEW/WRONG_VIRTUAL_ACCOUNT; snapshot khác cấu hình → REVIEW/VA_SNAPSHOT_CONFIGURATION_MISMATCH. Không tự đổi PAID từ polling hoặc API đối soát.
- Receipt lưu account_number và sub_account; Admin ledger hiển thị cả hai. API lookup chỉ đọc bổ sung trường va nếu provider trả về. Không dùng UUID API làm ID Webhook hoặc tự import/kích hoạt.
- Fingerprint bao gồm VA khi có; payload không có VA giữ công thức fingerprint cũ để không đánh dấu xung đột chỉ do nâng code. Cùng ID/bank-reference nhưng đổi VA được ghi conflict, không kích hoạt thêm. UNIQUE bank reference **không thêm VA vào key**, tránh cùng một giao dịch bị dùng cho hai VA/đơn.

Không coi biến cấu hình hay test H2 là bằng chứng ngân hàng/SePay Live đã VERIFIED. Giá trị ngân hàng bạn cung cấp chỉ xuất hiện trong fixture test theo yêu cầu; source runtime/mẫu cấu hình không hardcode số chính/VA đó.

## 2. Payment id=1 và khoản chuyển vào tài khoản chính

Theo bạn báo: Payment1 PENDING, đã chuyển50.000VND vào main account và chưa có giao dịch tương ứng trên SePay. **Không có Webhook đã xác thực để tự xác nhận gói.** Agent không đọc/sửa/replay/fake/cancel Payment1 hoặc tạo đơn thay thế.

Đơn cũ thiếu snapshot VA:
- Migration để va_number=NULL, không điền VA hiện tại vào lịch sử.
- POST start gặp Payment cũ vẫn dùng chính ID cũ, không tạo Payment thứ hai hoặc đổi phương thức.
- Trang QR của đơn BIDV cũ ẩn QR/số nhận, cảnh báo **không chuyển thêm**, vô hiệu nút copy amount/reference. Không tự đổi giá/reference/expiry/status.
- Kể cả payload VA hợp lệ được gửi cho reference cũ, snapshot không khớp nên không tự kích hoạt; giữ REVIEW để đối soát. Không được dùng điều này làm lý do gửi thử vào đơn thật.

**Bạn/chủ tài khoản cần làm riêng:** đối chiếu tiền vào trên ngân hàng và liên hệ ngân hàng/SePay về khả năng đồng bộ giao dịch main account. Không có giao dịch trong SePay thì không có gì để replay. Không tự tạo sự kiện ngân hàng hoặc mark PAID để chữa. Hiện chưa có workflow giải quyết REVIEW/cancel-and-renew được duyệt trong app; không dùng tài khoản Member khác để né đơn đang treo hoặc chuyển tiền thêm.

## 3. Migration V709 — chưa thực thi

File mới `database/migrations/V709__sepay_virtual_account.sql` chỉ thêm:

| Bảng | Cột | Kiểu |
|---|---|---|
| sepay_payment_details | va_number | VARCHAR(50) NULL |
| sepay_webhook_transactions | sub_account | VARCHAR(50) NULL |

Không INSERT/UPDATE/DELETE, backfill, default, bảng mới hoặc đổi constraint/index cũ. Có TRY/CATCH, XACT_ABORT và transaction, kiểm tra V706 prerequisites; chạy lại bỏ qua cột đã có nhưng không xác nhận cột cũ đúng kiểu. Không sửa V701–V708/approval.

Sau khi **bạn phê duyệt áp dụng schema**, tự backup và mở SSMS đúng **UTEFitLiveDemo** trong phiên riêng không transaction ngoài; kiểm tra `SELECT DB_NAME()` rồi review/chạy riêng V709. Không chạy lại bộ00–08 tạo DB/cấp quyền/approval vì DB này đang tồn tại và có giao dịch. V709 không có USE/DB_NAME guard bên trong nên người chạy chịu trách nhiệm chọn đúng DB. Chưa có sự cho phép nào để agent chạy script.

Kiểm tra chỉ đọc sau khi bạn áp dụng:

```sql
USE [UTEFitLiveDemo];
SELECT DB_NAME() AS expected_UTEFitLiveDemo;
SELECT COL_LENGTH('dbo.sepay_payment_details','va_number') AS expected_50,
       COL_LENGTH('dbo.sepay_webhook_transactions','sub_account') AS expected_50;
SELECT id,status,method,amount FROM dbo.payments WHERE id=1;
SELECT payment_id,va_number FROM dbo.sepay_payment_details WHERE payment_id=1;
```

Payment1 phải giữ nguyên trạng thái/số tiền đã có (nếu không có sự kiện thật được xử lý độc lập), va_number cũ vẫn NULL. Không dùng UPDATE để làm kết quả kiểm tra đúng mong muốn. Khởi động bản mới trước V709 sẽ bị Hibernate validate chặn thiếu cột; không đổi ddl-auto thành update để né migration.

Nếu migration lỗi: dừng, kiểm tra transaction/cột bằng phiên được phép; script rollback lỗi runtime. Nếu script đã COMMIT mà app lỗi, giữ hai cột nullable, không drop hoặc restore đè dữ liệu tài chính phát sinh. Backup không thay kế hoạch đối soát tiền đã nhận.

## 4. IntelliJ — bạn tự cấu hình, không gửi giá trị vào chat

Run → Edit Configurations → cấu hình Live hiện tại → Environment variables:
- Giữ **SEPAY_LIVE_ACCOUNT_NUMBER** bằng main account đã kết nối.
- Thêm **SEPAY_LIVE_VA_NUMBER** bằng VA BIDV đã được SePay nhận giao dịch; giữ đúng hoa/thường, không khoảng trắng.
- Kiểm tra **SEPAY_LIVE_BANK_CODE** cho VietQR và **SEPAY_LIVE_GATEWAY** khớp gateway payload thực tế (BIDV), không đổi main thành VA để cố qua so khớp.
- Giữ HMAC/profile/approval/database/HTTPS/cookie hiện có, không restore application.properties hoặc đổi JDBC DB.
- **Không tự bật SEPAY_LIVE_ACCEPT_NEW_PAYMENTS** vì đổi VA. Không đổi VA giữa các đơn VA đang chờ; snapshot cũ không tự cập nhật và cấu hình đổi có thể đưa callback sang REVIEW.

Chỉ thêm tên biến/placeholder vào .env.sepay-live.example; file mẫu không phải file env thật và Spring không tự nạp .env. Secret HMAC không cần đổi chỉ vì bổ sung VA, trừ khi chủ dự án có quyết định xoay secret riêng.

## 5. Chỉnh Webhook đang có — không tạo Webhook khác

Bạn/chủ tài khoản tự mở **SePay Live → Tích hợp → Webhooks → Sửa Webhook UTEFit đang dùng**:
1. Giữ URL hostname HTTPS hiện tại + `/payments/sepay/webhook/live`, JSON, tiền vào, HMAC-SHA256 và retry.
2. Tài khoản: chọn đúng BIDV main đã liên kết; mở cấu hình VA của tài khoản đó, chọn đúng VA tương ứng biến mới. Không chọn mọi tài khoản/VA hoặc thay main account trong app thành VA.
3. Giữ nhận diện prefix đúng backend và hậu tố24 ký tự chữ/số. Không lọc mất giao dịch cần đối soát; kiểm tra cả bộ lọc mã và VA.
4. Kiểm tra trạng thái kết nối/VA/delivery log hiện có; không bấm Gửi thử/replay vào Payment1 hoặc đơn thật, không gửi payload giả. Không yêu cầu chuyển tiền mới trong lượt kiểm tra này.

SePay lọc VA dựa subAccount; app vẫn tự xác minh lại, không tin bộ lọc Dashboard thay validation backend. Nguồn: [SePay ngân hàng/VA](https://developer.sepay.vn/vi/sepay-webhooks/tai-khoan-ngan-hang), [sửa Webhook](https://developer.sepay.vn/vi/sepay-webhooks/tao-webhook).

## 6. Kiểm tra trước bất kỳ giao dịch thật mới nào

- [ ] Chủ tài khoản xử lý rõ khoản vào main/Payment1; không dùng source mới để hợp thức hóa thiếu Webhook.
- [ ] Bạn review/backup/áp dụng V709, startup validate đúng DB và guard hiện tại vẫn PASS, không bypass.
- [ ] IntelliJ có main và VA riêng, VA/main khác nhau; Dashboard Webhook hiện tại chọn đúng VA, hostname còn hoạt động.
- [ ] Không sửa snapshot/expiry/reference của đơn cũ. Không tạo đơn Live chỉ để thử màn hình trong lượt này.
- [ ] Khi sau này bạn phê duyệt một đơn Live **mới**, QR và số nhận UI phải là VA snapshot, amount/reference đúng backend. Không chuyển vào main như trước; không tự mở cờ hoặc tạo giao dịch bởi agent.
- [ ] Chỉ sự kiện tiền vào thật đã xác minh tất cả điều kiện mới được PAID/ACTIVE. Sai/muộn/duplicate/conflict giữ đối soát; không chuyển thêm để chữa lỗi.

Không có bảo đảm giao dịch50.000VND cũ sẽ tự xuất hiện trong SePay sau thay đổi source. Backend không truy vấn ngân hàng trực tiếp để tự tạo Webhook thiếu.

## 7. Inventory / test

File sửa runtime: `src/main/java/com/utefit/config/SepayProperties.java`, `src/main/java/com/utefit/entity/SepayPaymentDetails.java`, `src/main/java/com/utefit/entity/SepayWebhookTransaction.java`, `src/main/java/com/utefit/service/SepayPaymentService.java`, `src/main/java/com/utefit/service/SepayWebhookPayload.java`, `src/main/java/com/utefit/service/SepayWebhookService.java`, `src/main/java/com/utefit/service/SepayReconciliationLookup.java`, `src/main/resources/templates/member/sepay-checkout.html`, `src/main/resources/templates/admin/payments/sepay-reconciliation.html`, `.env.sepay-live.example`.

Test sửa: `SepayLiveSafetyTest`, `SepayWebhookSecurityTest`, `SepayPaymentIntegrationTest`. File mới: migration V709 và tài liệu này; runbook/readiness hiện có được thêm hướng dẫn VA. Không sửa guard database/HMAC/security config/secure cookies/TLS/QR/Review trong lượt VA.

Test chỉ chạy H2 trong bộ nhớ/MockMvc in-process với secret fixture và database guard mock; đường dẫn Live trong test là controller giả lập, **không phải gọi localhost:8080/Tunnel/database Live**. Fixture dùng hai giá trị bạn yêu cầu để chứng minh mapping, không khởi tạo giao dịch ngân hàng.

Kết quả cuối: **Maven BUILD SUCCESS,166/166 test Java PASS**,0 failure/error/skipped; gồm36 test SePay integration (các ca VA và regression trước đó), guard/HMAC/API/HTTPS và regression CASH/VNPay lịch sử/QR/Review/Manager/Favorites. Log local: `target/sepay-bidv-va-verified-tests.log`. **38/38 test JavaScript PASS**. Test thêm kiểm tra VA đúng/rỗng/null/thiếu/sai/case/khoảng trắng, gateway/main/amount/reference sai, HMAC sai, duplicate/concurrent, VA thay sau snapshot, expiry và legacy order không được retrofit.

Whitespace phần source thay đổi PASS; application.properties local có khác biệt từ trước được giữ nguyên, không stage. Không tìm thấy số main/VA bạn cung cấp trong Java runtime hoặc template; chỉ có fixture test được yêu cầu. SQL Server migration V709, cấu hình Dashboard đang dùng và giao dịch thật **chưa VERIFIED**. Không chạy migration hoặc gửi request tới SePay thật, không Playwright, không tạo Payment ngân hàng hoặc sửa Payment1.
