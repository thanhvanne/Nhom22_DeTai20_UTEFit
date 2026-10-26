# SePay Live readiness — 26/10/2026

> Bản bổ sung BIDV VA hiện tại: `SEPAY_BIDV_VA.md`, biến SEPAY_LIVE_VA_NUMBER và migration nullable V709. Source đã kiểm tra riêng main/subAccount cho đơn VA mới; không backfill hoặc xác nhận đơn cũ. Các nhận xét thiếu VA trong báo cáo pre-live trước đó mô tả phiên bản cũ.

> Runbook laptop/SQL Server local/Cloudflare Quick Tunnel theo phạm vi demo được người dùng chọn: `SEPAY_LIVE_LAPTOP_THUC_TE.md`, kèm script riêng `database/live-demo/`. URL tạm chỉ dành cho demo có giám sát, không thay yêu cầu guard/backup/đối soát hoặc tự cấp approval.

> Pre-live review chỉ đọc và checklist A–H: xem `SEPAY_PRELIVE_VERIFICATION.md`. Tài liệu mới làm rõ giới hạn V708, bằng chứng UI/runtime và việc endpoint Live chưa có dry-run; không coi nút Gửi thử là xác nhận thanh toán hoặc tự cấp GO.

## Kết luận

Chuẩn bị source trên `feat/giai-doan-7`, baseline `2dadc44`, bảo toàn QR `a8798f7`. **Chưa bật Live, chưa Live VERIFIED.** Không chạy app8080/Playwright, chuyển tiền, gọi API ngân hàng/SePay thật, đổi Dashboard, chạy migration, sửa SQL Server, commit/push/merge.

`application.properties` local được giữ nguyên ngoài phạm vi, không restore hoặc stage. File có marker conflict Cloudinary từ trước; `git diff --check` toàn working tree sẽ báo marker này. Không tự sửa hay công bố credential trong file. Chỉ diff các file tính năng mới khi kiểm tra whitespace.

## Source đã hoàn thiện

- Bỏ nút/hướng dẫn VNPay trong Membership. `VnpayPaymentService.start` luôn từ chối trước repository/gateway, kể cả legacy credentials enabled. Giữ enum, lịch sử VNPAY, filter Admin và Return/IPN cũ để drain callback. Không tái dùng đơn CASH/VNPay PENDING cho SePay.
- Test tiếp tục dùng các biến bank/account/HMAC Test cũ. Live chỉ dùng `SEPAY_LIVE_*`, không fallback Test. Hai endpoint/env order/receipt riêng. Nhận Webhook chỉ cần HMAC secret, không cần API token.
- Live fail closed: profile `sepay,sepay-live`, không qa/test; flags, approval, HTTPS origin, cookie Secure, DDL validate/init never; databaseName khai báo khớp catalog JDBC và connection thực; marker DBA phê duyệt; không có order/receipt TEST. Test cũng từ chối DB chứa order/receipt LIVE. Guard áp dụng startup Live, tạo đơn, callback và hiện QR.
- `SEPAY_LIVE_ACCEPT_NEW_PAYMENTS=false` tách dừng tạo đơn mới khỏi callback: POST tạo/mở lại bị chặn nhưng lịch sử và callback cho đơn đã phát hành vẫn xử lý. Không dùng tắt toàn bộ SEPAY_ENABLED làm drain mặc định.
- Giữ snapshot giá/thời hạn/ngân hàng/reference, createdAt/expiresAt, random96bit + prefix/UNIQUE. Backend lấy tiền từ snapshot, VietQR chỉ có bank/account/amount/reference. Không đổi giá hoặc reference sau tạo đơn.
- HMAC raw bytes timestamp ±300s/constant-time, JSON strict/max32KiB, chống header trùng; Live thêm request HTTPS. Chỉ ngoại lệ CSRF/login đúng hai POST Webhook hiện có; không thêm no-auth/API-key fallback.
- Tiền vào đúng account/gateway/reference/snapshot, VND nguyên, đủ eligibility/thời hạn mới PAID/ACTIVE trong cùng transaction. Payload chính thức mặc định VND; nếu gửi thêm currency khác VND thì từ chối. Polling hay bấm nút không thể kích hoạt gói.
- UNIQUE environment+SePayID và filtered bankReference của V706, khóa Member → Membership → Payment. Duplicate ACK sau commit; collision kiểm tra lại sau rollback, lỗi persistence tạm503 để retry. Không phụ thuộc trình duyệt/polling đang mở.
- Thiếu/thừa tiền, sai mã/account, muộn, gói ACTIVE/đơn không PENDING lưu REVIEW/IGNORED/reason. Đơn đã có REVIEW không tự nhận lần chuyển đủ tiếp theo: tránh tổng tiền vượt đơn. Không tự gộp, gán tiền sang đơn khác hoặc hoàn tiền.
- API status ownership bổ sung reconciliationRequired: Member ẩn QR/cảnh báo không chuyển thêm khi cần đối soát. Hết hạn vẫn poll chậm để thấy receipt muộn; PENDING có expired flag, không tự đổi trạng thái tài chính theo đồng hồ UI. Unknown code không thể liên kết an toàn cho Member, chỉ hiện sổ Admin.

## Đối soát Admin và API

Admin → Payment → Đối soát SePay: `/admin/payments/sepay-reconciliation`. Receipt lưu dữ liệu cần đối soát, outcome/reason/conflict, không raw body/secret. Đây không phải lịch sử mọi request HTTP: request chưa xác thực không được ghi giả vào ledger. Xem thêm delivery/error logs trên Dashboard.

POST có CSRF `/admin/payments/sepay-reconciliation/lookup`: ADMIN ở URL và kiểm tra quyền service. API lookup Live mặc định tắt; nhập ngày/trang, chỉ đọc20 giao dịch/lần, giới hạn1 ngày, host HTTPS cố định, không redirect, connect timeout5s/request10s, response256KiB. Chỉ chọn trường giao dịch, `th:text`, không hiển thị provider error/token. Không gọi khi mở trang, không cron, không cập nhật Payment/hoàn tiền.

API v2 ID là UUID, Webhook ID là số, **không ép hai ID thành một**. Đối chiếu reference/account/date/amount/code; nếu thiếu Webhook yêu cầu Dashboard phát lại HMAC hợp lệ. Muộn/không khớp vẫn REVIEW. Chưa có workflow phê duyệt thủ công để giải quyết REVIEW/hoàn tiền; không có nút ép PAID. API thật chưa VERIFIED, Test không gọi API Live.

## Migration và database

Mới: `database/migrations/V708__sepay_live_guard.sql`, chưa chạy. Script transaction/XACT_ABORT, yêu cầu bảng V706 tồn tại, chỉ tạo bảng singleton `dbo.sepay_deployment_guard` gồm id=1, environment, deployment_id, approved_at. **Không insert approval**, không sửa Payment/Membership, không drop hoặc sửa V701–V707. Test cũ không cần marker Live.

DBA tự làm sau phê duyệt:
1. Chọn DB Live riêng (ví dụ UTEFitLive), không dùng database QA hiện tại hoặc dữ liệu PAID/ACTIVE mô phỏng. Không clone TEST receipts sang Live rồi xóa để vượt guard.
2. Backup/PITR/RPO/RTO, thử restore sang bản sao; xác nhận instance/catalog bằng `SELECT @@SERVERNAME, DB_NAME()` trong công cụ được phép.
3. Review/thử V706/V708 trên bản sao SQL Server. Schema đã có thì kiểm tra, không chạy lại migration mù quáng. Kiểm tra UNIQUE đúng cột/filter và enabled: uk_sepay_payment_reference, uk_sepay_environment_transaction, uk_sepay_bank_reference, uk_payments_sepay_reference; FK/CHECK trusted và indexes. Hibernate validate không chứng minh UNIQUE/filter/lock.
4. Chỉ khi duyệt mới chạy V708 lên đúng DB. DBA thêm row id=1/environment=LIVE/deployment_id khớp SEPAY_LIVE_DEPLOYMENT_ID/approved_at giờ duyệt bằng lệnh parameterized riêng, không lưu giá trị môi trường vào migration dùng chung. Không có row → fail closed.
5. Runtime SQL user có DML nghiệp vụ cần thiết và SELECT guard, **không INSERT/UPDATE/DELETE guard, DDL, db_owner**. Kiểm tra TLS certificate, tài khoản migration khác runtime.
6. Kiểm kê dữ liệu cũ nhiều gói ACTIVE/PENDING hoặc Payment chồng phương thức/snapshot thiếu; đối soát trước GO, không tự sửa lỗi cũ. Test lock/deadlock/filtered index/rollback trên bản sao SQL Server: H2 không chứng minh SQL Server.

Rollback V708: additive, có thể giữ bảng. Script lỗi rollback transaction; xác minh backup trước retry. Không restore DB cũ làm mất tiền đã vào hoặc drop receipt sau nhận tiền.

## Cấu hình Test / Live

`.env.sepay-live.example` chỉ placeholder. Spring không tự đọc `.env`: nhập biến môi trường qua IntelliJ Run Configuration hoặc secret manager. Không gửi secret vào chat, Git, ảnh chụp hoặc command arguments. Không chạy QA env cho Live.

Test giữ profile `sepay`, SEPAY_ENVIRONMENT=test, DB QA khớp SEPAY_TEST_DATABASE_NAME và các biến SEPAY_BANK_CODE/GATEWAY/ACCOUNT_NUMBER/ACCOUNT_NAME/WEBHOOK_SECRET đang hoạt động. Không thay cấu hình Test.

| Nhóm Live | Biến/điều kiện |
|---|---|
| Spring | SPRING_PROFILES_ACTIVE=sepay,sepay-live, port8080/bind127.0.0.1; SPRING_JPA_HIBERNATE_DDL_AUTO=validate; SPRING_SQL_INIT_MODE=never |
| DB | SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD; databaseName khớp SEPAY_LIVE_DATABASE_NAME/catalog thực |
| Approval | SEPAY_LIVE_DEPLOYMENT_ID (16–100 chữ/số/_/-) khớp marker; SEPAY_LIVE_APPROVAL=I_APPROVE_SEPAY_LIVE chỉ sau phê duyệt |
| Ngân hàng | SEPAY_LIVE_BANK_CODE, SEPAY_LIVE_GATEWAY, SEPAY_LIVE_ACCOUNT_NUMBER, SEPAY_LIVE_ACCOUNT_NAME |
| Webhook | SEPAY_LIVE_WEBHOOK_SECRET >=32 ký tự, khác Test; SEPAY_WEBHOOK_AUTH=hmac |
| HTTPS | SEPAY_LIVE_PUBLIC_URL chỉ origin HTTPS công khai, không path/query; cookie Secure từ profile Live |
| Đơn hàng | SEPAY_PAYMENT_PREFIX 2–5 chữ hoa; SEPAY_PAYMENT_MINUTES 5–60 (default15); VND nguyên |
| Flags | SEPAY_ENABLED=false, SEPAY_LIVE_ENABLED=false, SEPAY_LIVE_ACCEPT_NEW_PAYMENTS=false mặc định trong mẫu; SEPAY_ENVIRONMENT=live chỉ khi chọn deployment Live |
| API tùy chọn | SEPAY_LIVE_API_LOOKUP_ENABLED=false; khi được duyệt mới đặt SEPAY_LIVE_API_TOKEN quyền chỉ đọc, SEPAY_LIVE_BANK_ACCOUNT_ID UUID đúng ngân hàng |

Mail/JWT/Cloudinary dùng credentials ngoài source dành cho deployment; không dùng key/password fixture QA. Profile Live giảm log Web/Security xuống INFO nhưng vẫn phải rà debug logs/proxy logs bên ngoài.

## Dashboard và HTTPS — chủ dự án tự làm sau

1. Hostname HTTPS ổn định/certificate hợp lệ, Tunnel tới `http://127.0.0.1:8080`, không mở8080 ra Internet. Profile Live dùng forwarded headers framework: chỉ tin tunnel/proxy được kiểm soát vào loopback, không để client Internet tự đưa X-Forwarded-* trực tiếp vào origin. Kiểm tra request.isSecure; không bỏ guard để chữa proxy.
2. Xác nhận ngân hàng/account/chủ tài khoản/gateway đã liên kết **Live**. Nhập Live bank variables ngoài source, so QR với ngân hàng trước chuyển.
3. SePay Dashboard Live → Webhooks → tài khoản cụ thể, tiền vào, JSON, **`https://<hostname-live>/payments/sepay/webhook/live`**. Test vẫn `/payments/sepay/webhook/test` trên QA riêng.
4. Chọn HMAC-SHA256/secret Live khác Test. Proxy giữ raw bytes và X-SePay-Signature/X-SePay-Timestamp. Không API-key/no-auth cho endpoint này; API Bearer token tra cứu là credential khác HMAC.
5. Cấu hình mã prefix như app, hậu tố24 ký tự hex chữ/số. Không bật bỏ qua tiền không có mã nếu muốn ledger nhận đủ khoản sai nội dung; giao dịch bị lọc mất cần đối soát Dashboard/API.
6. Không login redirect/Cloudflare Access/JS challenge trên riêng POST webhook; không tắt CSRF/WAF toàn hệ thống. Bật retry/cảnh báo/NTP. ACK200 success:true có thể là lưu REVIEW, không đồng nghĩa PAID. Mất Webhook xem Dashboard/replay, không chuyển thêm.

## GO và rollback

**Hiện NO-GO để tự bật Live.** Còn chờ SQL Server/schema/quyền thực tế, account/TLS/proxy/secret và người quản trị duyệt.

Sau khi chủ dự án/ngân hàng/SePay cho phép:
1. Hoàn tất backup/migration/marker/SQL Server negative+concurrency tests, tách credentials/data, có người trực đối soát.
2. Đặt enabled/Live enabled/approval trên môi trường đã duyệt, giữ ACCEPT_NEW_PAYMENTS=false khi startup xác minh guard.
3. Sau đủ bằng chứng mới mở ACCEPT_NEW_PAYMENTS=true/restart có kiểm soát; tạo đúng một đơn demo được phép, so bank/account/amount/reference/expiry trước chuyển.
4. Người dùng tự chuyển khoản nhỏ bằng giá gói/demo đã duyệt; không sửa số tiền request/DB để giả thành công. Kiểm tra ngân hàng → delivery HMAC → MATCHED → PAID → ACTIVE → notification/doanh thu, kể cả đã đóng browser.
5. Chậm trạng thái thì không chuyển lại; Dashboard/ledger/API read-only, replay HMAC khi cần. REVIEW xử lý theo quy trình duyệt, không ép PAID bằng CASH. Chỉ có giao dịch thật được kiểm chứng mới ghi Live VERIFIED.

Rollback: tắt ACCEPT_NEW_PAYMENTS, giữ callback/secret/DB/binary để drain đơn đã phát hành, chờ expiry/retry/đối soát. Tắt SEPAY_ENABLED chặn cả callback503, chỉ dùng theo quyết định incident. Secret lộ cần xoay phối hợp Dashboard và app, kiểm kê/replay deliveries. Không rollback binary cũ thiếu guard, drop receipt hoặc restore DB che giao dịch. Git không thay backup ngân hàng/SQL.

## Nguồn đối chiếu

- [SePay HMAC](https://developer.sepay.vn/vi/sepay-webhooks/xac-thuc): ký timestamp.raw_body, cửa sổ5 phút.
- [Payload/ACK](https://developer.sepay.vn/vi/sepay-webhooks/tich-hop-webhook): VND nguyên, ngày giờ Việt Nam; ACK200/201 JSON success:true trong30s. UTEFit transaction ngắn và ACK sau commit; nếu quá tải cần durable queue riêng, không ACK trước lưu.
- [Đối soát](https://developer.sepay.vn/vi/sepay-webhooks/doi-soat-giao-dich), [API v2](https://developer.sepay.vn/vi/sepay-api/v2/giao-dich/danh-sach): tra cứu GET/Bearer/phân trang, UUID khác Webhook.

## Inventory và kiểm thử

File mới: `.env.sepay-live.example`, `database/migrations/V708__sepay_live_guard.sql`, `src/main/resources/application-sepay-live.properties`, `src/main/java/com/utefit/service/SepayReconciliationLookup.java`, `src/test/java/com/utefit/service/SepayLiveSafetyTest.java`, `src/test/java/com/utefit/service/SepayDatabaseGuardTest.java`, `src/test/java/com/utefit/service/SepayPaymentIntegrationTest.java`, `src/test/java/com/utefit/service/SepayReconciliationLookupTest.java`, `src/test/js/sepay-checkout.test.cjs`, báo cáo này.

File sửa: SepayProperties, SepayDatabaseGuard, SepayWebhookController, SepayPaymentService, SepayWebhookPayload, SepayWebhookService, SepayWebhookTransactionRepository, SepayReconciliationController, VnpayPaymentService, MemberMembershipController, memberships.html, sepay-reconciliation.html, sepay-checkout.js, VnpayPaymentServiceTest, VnpayConcurrencyIntegrationTest và hai tài liệu SePay cũ (liên kết cập nhật). Không sửa source QR/Review/Manager/Favorites/Statistics, local properties hoặc migration cũ.

### Kết quả đã chạy

| Kiểm tra | Kết quả |
|---|---|
| Maven compile + 18 suite regression/H2 | BUILD SUCCESS, 141 test, 0 failure/error/skipped; log local `target/sepay-live-verified-tests.log` |
| Compile lại + unit safety/API sau bổ sung chặn credential có ký tự điều khiển | BUILD SUCCESS, 9/9 PASS (5 Live safety, 4 API lookup); có chạy lại test đã có, không cộng thành 150 test riêng biệt |
| Node toàn bộ test JS | 38/38 PASS: QR Member/scanner, Review upload, SePay polling |
| JavaScript syntax / whitespace phần thay đổi mới | PASS; loại file local application.properties có marker từ trước |
| SQL Server, V708/filtered indexes/lock thực tế | NOT VERIFIED, không chạy migration hoặc kết nối SQL Server |
| API SePay/provider/HTTPS Live/ngân hàng thật | NOT VERIFIED, HTTP client dùng mock trong test, không giao dịch thật |

SePay integration gồm19 ca trên H2: snapshot/PENDING/reuse, PAID/ACTIVE, callback lặp và đồng thời (một receipt/notification), thiếu/thừa/sai mã/account/tiền ra/muộn/gói ACTIVE, prior REVIEW không tự gộp tiền, ownership/CSRF/RBAC, HTTPS/secret Test không dùng Live, CASH không xác nhận SEPAY, bank reference conflict, currency sai, activation lỗi rollback toàn bộ rồi retry, drain chỉ dừng POST tạo đơn. API v2 kiểm tra quyền service, GET host cố định/mock, chọn trường trả về, lỗi/credential không bị echo. Guard catalog/marker/data chéo dùng mock JDBC/JPA; không coi đây là bằng chứng SQL Server thật.

Lượt test ban đầu phát hiện fixture VNPay concurrency cũ vẫn gọi tạo đơn mới; đã đổi fixture thành Payment lịch sử trong H2 rồi kiểm tra lại callback cũ thành công. Không nới chốt chặn tạo VNPay để làm test qua. Chưa chạy Playwright hoặc thử giao dịch thật.

Git cuối: vẫn `feat/giai-doan-7`, HEAD `2dadc44`, `a8798f7` là ancestor; không stage/commit. Các file tính năng còn modified/untracked chờ người dùng kiểm thử; `.ai/`, `.playwright-mcp/`, `docs/` và cấu hình local giữ nguyên ngoài phạm vi.
