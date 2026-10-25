# UTEFit — Checklist SePay Test → Live và rollback

Ngày rà soát: 25/10/2026. Nhánh: `feat/giai-doan-7`. **Kết luận: NO-GO cho Live cho tới khi các cổng kiểm soát dưới đây được xác nhận.** Tài liệu là kế hoạch, không phải lệnh đã thực hiện hay cho phép bật Live/chuyển tiền.

## 1. Bằng chứng và phạm vi

| Mục | Trạng thái | Căn cứ |
|---|---|---|
| Happy-path Test 100.000đ, HMAC → PAID → ACTIVE | Người dùng báo thành công | Chưa có transaction ID/log riêng; agent không chạy lại |
| Compile source SePay | BUILD SUCCESS ở lượt triển khai trước | `test-compile -DskipTests`, không phải Maven test |
| HMAC/JSON test source | Đã có, chưa xác nhận chạy | `SepayWebhookSecurityTest` |
| Duplicate/race, sai tiền/quyền, lỗi DB, CASH/VNPay regression | NOT VERIFIED | Không suy ra từ happy-path |
| Database/schema/cấu hình Live, HTTPS vận hành, rollback drill | NOT VERIFIED | Không kết nối DB hoặc mở secret trong lượt này |
| Live và giao dịch ngân hàng thật | NOT VERIFIED / CHƯA THỰC HIỆN | Agent không bật Live, không gọi giao dịch |

Không chỉnh code nghiệp vụ, không chạy ứng dụng, Playwright, test giao dịch hoặc migration trong lượt checkpoint. Baseline trước SePay: `cb4500f`, tag `checkpoint/before-sepay-20261025`. Checkpoint tính năng: xem commit `feat(sepay): checkpoint VietQR payments after user-verified Test Mode` trong `git log`.

## 2. Những gì code đã có — không đồng nghĩa runtime đã PASS

- HMAC-SHA256 trên `timestamp + '.' + raw body`, so sánh constant-time, lệch tối đa ±300 giây; payload JSON tối đa 32 KiB, từ chối header trùng và JSON không hợp lệ.
- Login/CSRF chỉ ngoại lệ hai POST Webhook. Tạo đơn Member vẫn có CSRF; checkout/status kiểm tra role, trạng thái tài khoản và ownership; đối soát chỉ Admin.
- Snapshot giá/thời hạn/ngân hàng/reference; không tái sử dụng CASH/VNPay PENDING. Mã ngẫu nhiên, UNIQUE, so khớp code/content chính xác, không substring tùy tiện.
- Receipt UNIQUE theo môi trường + SePay ID và bank reference không rỗng; transaction/khóa User → Membership → Payment. PAID và ACTIVE dùng chung transaction/logic kích hoạt hiện có.
- Sai tiền/tài khoản/mã/thời gian/trạng thái → REVIEW; không tự kích hoạt. ACK thành công sau xử lý/lưu bền vững; lỗi tạm thời trả 503.
- Live mặc định tắt; cần flag Live và secret riêng. Test kiểm tra cả JDBC URL và catalog thực tế là QA/Test. **Live chưa có allowlist catalog tương đương: người vận hành bắt buộc xác nhận đúng DB.**

## 3. Cổng A — Database và migration (bắt buộc)

- [ ] Chủ dự án ghi nhận SQL Server instance/catalog Live chính xác, người chịu trách nhiệm và cửa sổ bảo trì; xác nhận thủ công `SELECT @@SERVERNAME, DB_NAME();` bằng kết nối được phép. Không dùng file QA env cho Live.
- [ ] Chọn rõ Live DB: dữ liệu vận hành hợp lệ, không sao chép các Payment PAID/Membership ACTIVE mô phỏng, receipt TEST hoặc tài khoản/fixture QA vào Live. Nếu Live DB đã lẫn dữ liệu test, lập phương án được duyệt trước; không tự DELETE/TRUNCATE hoặc sửa trạng thái tài chính.
- [ ] Backup đầy đủ trước migration, kiểm tra khả năng phục hồi bằng restore sang DB riêng; ghi backup ID/thời gian và vị trí bảo mật. Có kế hoạch transaction-log/PITR phù hợp recovery model, RPO/RTO được chủ dự án chấp thuận.
- [ ] Trên bản sao DB: kiểm tra V701–V705 và schema hiện có; chạy thử **V706 mới**, đo thời gian/lock, kiểm tra row count và tổng doanh thu trước/sau. Không chạy lại tùy tiện các migration tạo dữ liệu cũ.
- [ ] Review `database/migrations/V706__sepay_vietqr.sql`: thay CHECK method bằng CASH/VNPAY/SEPAY, thêm hai bảng; không drop bảng/cột hoặc cập nhật dữ liệu cũ. CHECK phức tạp không nhận diện sẽ dừng/rollback, cần người quản trị review thay vì bỏ guard.
- [ ] Sau khi được duyệt mới áp dụng V706 vào đúng Live DB bằng tài khoản migration riêng; runtime chỉ có quyền DML cần thiết, không sysadmin/db_owner/DDL.
- [ ] Kiểm tra thực tế PK/FK/CHECK được bật và trusted; các bảng tồn tại không có nghĩa migration đầy đủ (script tạo bảng có điều kiện). `ddl-auto=validate` không thay thế kiểm tra UNIQUE/CHECK/index.
- [ ] Xác nhận UNIQUE: `uk_sepay_payment_reference`, `uk_sepay_environment_transaction`, filtered `uk_sepay_bank_reference`, filtered `uk_payments_sepay_reference`; kiểm tra đúng cột/filter và không disabled. Kiểm tra index `idx_sepay_outcome_received`, `idx_sepay_receipt_reference` cùng FK hai bảng → payments.
- [ ] Kiểm kê dữ liệu cũ bằng truy vấn chỉ đọc: nhiều Membership ACTIVE còn hạn hoặc nhiều PENDING trên cùng Member, thiếu snapshot, nhiều Payment PENDING trên cùng Membership, mã trùng và trạng thái bất nhất. Đối soát trước, không tự sửa lỗi 0–6.
- [ ] Runtime chạy profile `sepay`, `ddl-auto=validate`, `spring.sql.init.mode=never`; không bật `qa`/fixture initializer. Kiểm tra mọi biến môi trường/IntelliJ override để tránh `update/create/create-drop` hoặc trỏ nhầm DB.
- [ ] SQL Server TLS có xác minh certificate theo môi trường triển khai; không sao chép `trustServerCertificate=true` từ ví dụ QA mà chưa đánh giá.

## 4. Cổng B — HTTPS và khả dụng

- [ ] Hostname HTTPS ổn định thuộc quyền quản lý, certificate hợp lệ, không dùng URL tunnel tạm cho vận hành. Cloudflare Tunnel định tuyến về `http://127.0.0.1:8080`; profile hiện bind loopback, không cần mở 8080 ra Internet.
- [ ] Tunnel chạy bền vững, tự khởi động lại, máy chủ không sleep; giám sát mất mạng/ứng dụng/SQL Server. IntelliJ tiện demo nhưng không cung cấp SLA nếu máy cá nhân tắt.
- [ ] URL Live chính xác: **`https://<hostname-live>/payments/sepay/webhook/live`**, method POST, JSON. Giữ path Test `/payments/sepay/webhook/test` trên deployment QA riêng; không trỏ webhook Test vào Live DB.
- [ ] Không có redirect HTTP/login, Cloudflare Access/challenge/CAPTCHA hoặc body rewrite trên POST Webhook. Giữ nguyên raw bytes và `X-SePay-Signature`, `X-SePay-Timestamp`. Không tắt WAF/CSRF toàn website; giới hạn ngoại lệ đúng endpoint.
- [ ] Member/Admin truy cập public bằng HTTPS, `SEPAY_COOKIE_SECURE=true`; rà JWT signing key, cookie/SameSite, forwarded headers và origin trust nếu proxy thay đổi. Không đưa secret QA hoặc mật khẩu demo vào Live.
- [ ] Đồng hồ máy/VM đồng bộ NTP; xác nhận timezone nghiệp vụ `Asia/Ho_Chi_Minh`, kiểm tra log UTC/VN để đối chiếu timestamp HMAC và transactionDate.

## 5. Cổng C — Cấu hình Live và SePay Dashboard

**Bảng dưới chỉ là cấu hình mục tiêu sau phê duyệt, không phải các biến đã được đặt trong lượt này.** Không đưa giá trị thật vào Git, ảnh chụp, chat hoặc log.

| Biến | Điều kiện khi chuyển Live được duyệt |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `sepay`, không ghép `qa` |
| `SPRING_DATASOURCE_URL`, `DB_USERNAME`, `DB_PASSWORD` | Đúng catalog Live đã backup/migration; credential runtime riêng |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `validate` |
| `SEPAY_ENVIRONMENT` | `live` |
| `SEPAY_ENABLED`, `SEPAY_LIVE_ENABLED` | Chỉ đặt `true` sau khi duyệt GO; hiện không thay đổi |
| `SEPAY_BANK_CODE` | Mã ngân hàng hỗ trợ VietQR, đúng tài khoản nhận |
| `SEPAY_GATEWAY` | Tên gateway chính xác theo payload SePay, không mặc định rằng luôn bằng bank code |
| `SEPAY_ACCOUNT_NUMBER`, `SEPAY_ACCOUNT_NAME` | Tài khoản và tên chủ tài khoản Live đã chủ dự án xác nhận |
| `SEPAY_LIVE_WEBHOOK_SECRET` | Secret HMAC Live đủ mạnh, tối thiểu 32 ký tự, khác Test, lưu ngoài source |
| `SEPAY_WEBHOOK_SECRET` | Chỉ dùng Test; không tái sử dụng làm secret Live |
| `SEPAY_WEBHOOK_AUTH` | `hmac`, không fallback no-auth/API key |
| `SEPAY_PAYMENT_PREFIX` | `UTE` hoặc tiền tố 2–5 chữ hoa, thống nhất cấu hình nhận diện mã trên Dashboard |
| `SEPAY_PAYMENT_MINUTES` | 5–60 phút, mặc định 15; chủ dự án duyệt cách xử lý tiền/Webhook đến muộn |
| `SEPAY_TEST_QR_PREVIEW` | `false`; QR Test không phải ngân hàng sandbox để chuyển tiền thật |
| `SEPAY_COOKIE_SECURE` | `true` khi Member/Admin dùng HTTPS |

- [ ] Chủ tài khoản xác nhận ngân hàng đã liên kết và hoạt động trong **Live**; kiểm tra đúng account, chủ tài khoản, dịch vụ/hạn mức/phí liên quan. Tên hiển thị trong app không chứng minh quyền sở hữu ngân hàng.
- [ ] Dashboard → Tích hợp → Webhooks: cấu hình riêng Live URL trên, chọn đúng tài khoản cụ thể, giao dịch tiền vào, JSON, HMAC với secret Live, bật retry và cảnh báo.
- [ ] Cấu hình nhận diện mã để trường `code` là nguyên reference backend (tiền tố + 24 ký tự hex), không cắt/đổi mã. Chứng minh payload thực tế tương thích trước GO.
- [ ] Quyết định bộ lọc mã có bỏ giao dịch thiếu mã không: nếu bỏ, ứng dụng không nhận được các khoản đó; phải có đối soát ngân hàng/SePay riêng để không thất lạc tiền.
- [ ] Kiểm tra delivery logs/retry, response 200 + `success:true`, và lỗi 503 được retry. Xác nhận timestamp/chữ ký được cấp mới khi retry lâu hơn cửa sổ 5 phút; không nới guard để chữa sai cấu hình.
- [ ] Không coi nút “Gửi thử” với dữ liệu mẫu không khớp đơn là giao dịch PAID. Webhook Test và Live phải dùng khóa khác nhau về vận hành, kể cả khi biến Test không có trong process Live (code chỉ so sánh các giá trị đang cấu hình).

Tài liệu đối chiếu: [cấu hình Webhook](https://developer.sepay.vn/vi/sepay-webhooks/tao-webhook), [HMAC](https://developer.sepay.vn/vi/sepay-webhooks/xac-thuc). Tài liệu SePay nêu retry tối đa 7 lần trong khoảng 33 phút; vì expiry đơn mặc định 15 phút, retry đến muộn có thể thành REVIEW dù tiền đã vào. Phải duyệt quy trình xử lý, không mặc định mọi retry sẽ tự ACTIVE. Nguồn: [SePay Webhooks](https://developer.sepay.vn/vi/sepay-webhooks).

## 6. Cổng D — QA bảo mật và nghiệp vụ trước GO

Thực hiện trên QA riêng với dữ liệu được phép; các ô dưới đây hiện chưa được agent đánh dấu PASS.

- [ ] Chạy unit HMAC/JSON và bổ sung/xác nhận test transaction/idempotency với SQL Server QA, không chỉ mock: duplicate tuần tự/đồng thời cùng ID; khác ID cùng bank reference; cùng ID nội dung bị đổi.
- [ ] Chữ ký sai/thiếu, body bị sửa, timestamp cũ/tương lai, header lặp, JSON keys lặp, payload quá 32 KiB: 4xx, không có PAID/ACTIVE.
- [ ] Sai account/gateway, tiền ra, sai/thiếu/thừa amount, thiếu/mơ hồ code, đơn hết hạn/trạng thái không hợp lệ: receipt REVIEW/IGNORED đúng lý do, không tự ACTIVE.
- [ ] Tiền đến muộn, hai khoản chuyển cho một đơn, reference không tồn tại: không cấp hai gói/nhân đôi doanh thu; ledger giữ đủ bằng chứng đối soát.
- [ ] Member khác đổi ID không thấy QR/status hoặc tạo đơn; Guest/PT/Manager không được tạo đơn/xem sổ Admin. Member POST thiếu CSRF bị từ chối; polling không thể PAID.
- [ ] Khóa tài khoản/plan/branch sau tạo đơn; concurrent tạo đơn và callback; lỗi/timeout SQL giữa receipt–PAID–ACTIVE: rollback nhất quán, retry khôi phục đúng một lần.
- [ ] Kiểm tra concurrency cả CASH/VNPay cũ nếu dữ liệu lịch sử cho phép nhiều đơn Member: khóa trong SePay không tự chứng minh mọi luồng cũ dùng cùng khóa User. Nếu không đạt, xác định thay đổi tích hợp tối thiểu trước Live, không bỏ qua.
- [ ] CASH, VNPay, Notification, Membership duration snapshot và Statistics regression; doanh thu chỉ cộng Payment PAID, không cộng receipt lần hai.
- [ ] UI trên HTTPS: QR đúng bank/account/amount/reference, copy/reload/poll/expiry đúng; QR đã lưu vẫn có thể chuyển tiền sau expiry nên hướng dẫn Member và đối soát phải rõ.
- [ ] Thử quy trình mất tunnel, restart backend, lỗi DB và phục hồi trên QA; xác nhận không ACK thành công khi không lưu được và không mất giao dịch khi retry.

## 7. Cổng E — Đối soát và giám sát (rủi ro vận hành hiện tại)

**P1 — cần chốt trước nhận tiền thật:** trang `/admin/payments/sepay-reconciliation` hiện chỉ đọc. Chưa có workflow xử lý/đóng đơn hết hạn, phê duyệt ngoại lệ hoặc hoàn tiền; Payment REVIEW vẫn có thể PENDING và chặn đổi phương thức/hủy gói. Đây là giới hạn đã xác định, không tự sửa trong lượt kế hoạch này.

- [ ] Chỉ định người phụ trách đối soát, thời gian phản hồi và cách hỗ trợ Member bị treo đơn; chọn quy trình thủ công có phê duyệt ngoài app hoặc yêu cầu bổ sung chức năng sau. Không “xử lý” bằng SQL ép PAID, xóa receipt hoặc đổi SEPAY sang CASH.
- [ ] Đối soát ít nhất theo ngày: ngân hàng ↔ SePay ↔ receipt ↔ Payment ↔ Membership ↔ doanh thu; cả khoản sai mã/thiếu/thừa/hết hạn/outgoing/conflict. Tiền thật chưa phân bổ vẫn là khoản phải xử lý, không coi là chưa nhận.
- [ ] Quy trình hoàn tiền do người có thẩm quyền xử lý với ngân hàng; ghi bằng chứng, trạng thái nghiệp vụ và tác động Membership theo phê duyệt. Không có auto-refund trong tích hợp hiện tại.
- [ ] Cảnh báo delivery failure/401/503, REVIEW/conflict tăng, PENDING quá hạn, DB lock/deadlock, app/tunnel down; bảo vệ log khỏi body/header/secret/số dư và hạn chế truy cập PII.
- [ ] Chính sách giữ ledger/backup/log, quyền Admin tối thiểu và lưu bằng chứng đã che thông tin nhạy cảm.

## 8. Rollout và rollback không mất tiền/dữ liệu

**P1 — kill switch hiện tại không chỉ chặn đơn mới:** `SEPAY_ENABLED=false` làm tạo đơn bị chặn và Webhook trả 503. Không dùng như “pause checkout” khi còn giao dịch đang bay. `SEPAY_LIVE_ENABLED=false` hoặc đổi mode/secret cũng chặn xử lý Live.

### Trước triển khai

1. [ ] Ghi hash checkpoint, artifact/JDK/config version (không lưu secret vào Git), backup DB và owner phục hồi; diễn tập trên QA. Giữ artifact SePay-compatible để rollback ứng dụng mà vẫn đọc enum SEPAY.
2. [ ] Phê duyệt cách **tạm chặn POST tạo đơn SePay mới nhưng vẫn nhận Webhook/status**: quy tắc reverse proxy đúng đường dẫn Member là một lựa chọn cần thử trước, hoặc một cờ admission riêng trong lần phát triển được duyệt sau. Hiện chưa có cờ riêng trong code. Không tắt toàn bộ `/payments/**`.
3. [ ] Chỉ sau GO đầy đủ mới cấu hình Live và restart có kiểm soát; bước nhận tiền thật xác nhận end-to-end đầu tiên cần người dùng cho phép riêng trong tương lai. Lượt này không thực hiện.

### Khi có sự cố sau rollout

1. [ ] Dừng nhận đơn mới theo phương án admission đã duyệt; thông báo Member, giữ Webhook HTTPS/HMAC/DB hoạt động. Giữ nguyên account/secret/reference của đơn đang xử lý để nhận retry, không đổi tài khoản giữa chừng.
2. [ ] Lưu delivery logs, receipt, Payment/Membership và mốc thời gian an toàn; thống kê đơn PENDING, tiền đã nhận, các REVIEW/conflict. Không ACK giả, không xóa ledger.
3. [ ] Nếu lộ secret, ưu tiên an toàn: phối hợp đổi key cả Dashboard/backend, thu hồi key cũ và lưu hàng đợi giao dịch cần replay; dự kiến gián đoạn, không bật no-auth. Thử quy trình xoay key trên QA trước.
4. [ ] Nếu cần rollback binary, chọn bản **vẫn hiểu SEPAY và schema V706**, test trước; ưu tiên forward-fix được duyệt. Bản `cb4500f` trước SePay không biết enum SEPAY và không nhận callback, không thể rollback mù khi đã có giao dịch.
5. [ ] Giữ V706/hai bảng khi rollback ứng dụng; không tự drop/downgrade constraint hoặc đổi method. Git revert/restore không hoàn tiền hoặc phục hồi DB.
6. [ ] Chỉ sau đối soát hết đơn đang bay và tiền muộn, chủ dự án mới quyết định tắt hoàn toàn SePay/webhook. Hết TTL/retry không đảm bảo không có khoản chuyển muộn; giữ quy trình tiếp nhận/đối soát ngoài app.
7. [ ] Nếu buộc restore DB, trước hết dừng nhận đơn mới và lưu bằng chứng sau thời điểm backup; restore sang DB riêng để đối chiếu, lập kế hoạch replay an toàn theo ID/reference. Restore mù có thể mất receipt/PAID nhưng tiền ngân hàng không quay lại. Không replay payload cũ bằng cách tắt HMAC/timestamp.

## 9. Phê duyệt GO / NO-GO

- [ ] A: Backup/restore drill, catalog Live, migration và constraints được xác nhận.
- [ ] B/C: HTTPS ổn định, bank/account đúng, secret riêng, Dashboard/retry/clock đúng.
- [ ] D: Các ca security/idempotency/race/rollback và regression có bằng chứng PASS; mọi lỗi chặn đã được xử lý theo phạm vi được duyệt.
- [ ] E: Người chịu trách nhiệm, quy trình REVIEW/hoàn tiền/đơn treo và giám sát được chấp thuận.
- [ ] Runbook admission pause và rollback đã diễn tập, không mất callback đang bay.
- [ ] Chủ dự án cho phép bật Live và riêng bước giao dịch thật đầu tiên. Sau bằng chứng thực tế mới ghi Live VERIFIED, không dùng kết quả Test thay thế.

Checkpoint Git không bảo vệ env thật hoặc dữ liệu SQL: lưu chúng riêng bằng cơ chế backup/secret management được phép. Thư mục `.ai/`, `.playwright-mcp/`, `docs/`, `target/`, file env thật và dữ liệu QA không thuộc checkpoint SePay.
