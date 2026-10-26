# UTEFit — PRE-LIVE VERIFICATION và checklist A–H

Ngày: 26/10/2026. Nhánh `feat/giai-doan-7`, HEAD `2dadc44`; checkpoint QR `a8798f7` giữ nguyên.

**Kết luận hiện tại: NO-GO cho nhận thanh toán thật.** Source có chốt bảo vệ nhưng database triển khai, ngân hàng, HTTPS và Webhook Live chưa được kiểm chứng. Tài liệu này không cấp phê duyệt hoặc bật bất kỳ công tắc nào.

Lượt này chỉ đọc source/tài liệu/log test có sẵn và viết tài liệu. Không chạy app/test mới, mở trình duyệt, kết nối SQL Server, áp dụng migration, tạo/sửa Webhook, chuyển tiền hoặc stage/commit/push/merge. Không đọc/in giá trị cấu hình local hoặc QA env. Không suy diễn trạng thái runtime từ mặc định trong source.

## 1. Kết quả rà soát V708

Đã đọc toàn bộ `database/migrations/V708__sepay_live_guard.sql` (23 dòng).

| Câu hỏi | Kết luận dựa trên source |
|---|---|
| SQL Server compatible? | Dùng T-SQL: OBJECT_ID/N'U', TRY/CATCH, THROW, DATETIME2, transaction và XACT_ABORT. Không thấy cú pháp MySQL. Chưa parse/chạy trên instance SQL Server đích nên đây là review tĩnh, không phải migration PASS thực tế. |
| Tác động dữ liệu cũ? | Không DELETE/TRUNCATE/DROP/UPDATE/INSERT. Chỉ tạo bảng dbo.sepay_deployment_guard khi chưa tồn tại. Có thể lấy schema lock trong lúc DDL, cần cửa sổ triển khai. |
| Payment/Membership? | Không ghi hoặc đổi trạng thái hai bảng đó, không thay giá/gói/lịch sử. |
| Tự bật Live/phê duyệt? | Không. Bảng mới rỗng; không có seed row, flag hoặc credential. Marker cũ nếu đã có được giữ nguyên, không bị xóa/thu hồi. |
| Chạy một lần an toàn? | Có thể áp dụng có điều kiện sau backup/review đúng DB/thử trên bản sao; không thể bảo đảm vô điều kiện. Script không có USE/kiểm tra DB_NAME nên người chạy phải tự xác định DB. |
| Chạy lại? | Bỏ qua CREATE nếu bảng guard đã có. Không kiểm tra/cải tạo schema bảng cũ, row cũ hoặc quyền. Không chạy đồng thời hai phiên. |
| Phụ thuộc? | Trực tiếp kiểm tra hai bảng dbo.sepay_payment_details và dbo.sepay_webhook_transactions do V706 tạo. Không kiểm tra lịch sử version hoặc đầy đủ cột/index V706. Không trực tiếp đọc V701–V705/V707; binary UTEFit vẫn cần schema các module tương ứng. Không chạy lại V701–V707 chỉ để thêm V708. |
| Quyền? | Tài khoản migration cần quyền DDL/schema phù hợp và xem metadata; runtime chỉ SELECT guard, không được tự tạo/sửa/xóa marker. |

Luồng lỗi: thiếu bảng V706 → THROW trước CREATE; runtime error trong TRY → CATCH rollback transaction đang mở và rethrow. XACT_ABORT hỗ trợ rollback lỗi runtime nhưng không bao phủ mọi lỗi biên dịch/cú pháp. Nguồn: [Microsoft XACT_ABORT](https://learn.microsoft.com/en-us/sql/t-sql/statements/set-xact-abort-transact-sql?view=sql-server-ver17), [Microsoft THROW](https://learn.microsoft.com/en-us/sql/t-sql/language-elements/throw-transact-sql?view=sql-server-ver17).

Khôi phục an toàn do DBA thực hiện khi được phép:
1. Dùng phiên riêng, không chứa transaction/công việc khác đang mở; CATCH dùng ROLLBACK toàn transaction, không savepoint cho transaction ngoài.
2. Nếu lỗi, dừng, lưu mã lỗi không kèm secret. Xác định session/transaction đã kết thúc trước retry; kiểm tra read-only bảng guard, cấu trúc, row, row count/trạng thái tài chính đối chiếu trước/sau.
3. Nếu CREATE đã COMMIT rồi nhưng app lỗi, có thể giữ bảng additive rỗng; không cần drop bảng để rollback binary. Nếu bảng tồn tại sai cấu trúc, DBA lập migration sửa riêng, không sửa V708 hoặc tự drop.
4. Có backup và kết quả thử restore từ trước. Không restore đè sau khi tiền thật đã vào mà chưa có kế hoạch đối soát/PITR, không xóa receipt/Payment để che lỗi.

## 2. Chốt cấu hình Live đối chiếu source

Các file đã đối chiếu: application-sepay-live.properties, application-sepay.properties, SepayProperties, SepayDatabaseGuard, SepayWebhookController/Security/Service, SepayPaymentService, SepayReconciliationLookup, SecurityConfig và SEPAY_LIVE_READINESS.md.

| Cổng | Điều kiện code hiện tại |
|---|---|
| Nhận callback Live | SEPAY_ENABLED và SEPAY_LIVE_ENABLED được phê duyệt/bật, SEPAY_ENVIRONMENT=live, đầy đủ cấu hình hợp lệ. Mặc định cờ Live false. |
| Profile | Có cả sepay và sepay-live; không qa/test. Chỉ chọn profile không tự đổi SEPAY_ENVIRONMENT hoặc bật Live. |
| Schema/security | spring.jpa.hibernate.ddl-auto=validate, spring.sql.init.mode=never, cookie Secure. Không tin file mẫu nếu IntelliJ/env/CLI override khác. |
| Database | SEPAY_LIVE_DATABASE_NAME khớp duy nhất databaseName trong JDBC URL và connection.getCatalog; không mang qa/test hoặc bằng tên Test được cấu hình. Dùng database Live riêng, không đổi kết nối QA hiện tại sang Live. |
| Dữ liệu môi trường | Live không được có order/receipt TEST; Test không được có order/receipt LIVE. Guard này không chứng minh các bảng nghiệp vụ khác không chứa fixture QA. Phải kiểm kê toàn DB riêng. |
| Phê duyệt | SEPAY_LIVE_APPROVAL phải khớp hằng số trong code, SEPAY_LIVE_DEPLOYMENT_ID hợp lệ khớp row id=1/environment=LIVE trong guard. V708 không tạo row này. |
| Ngân hàng | Chỉ dùng SEPAY_LIVE_BANK_CODE/GATEWAY/ACCOUNT_NUMBER/ACCOUNT_NAME; không fallback biến Test. Code không tự liên kết/xác minh chủ tài khoản tại ngân hàng. |
| HMAC | SEPAY_WEBHOOK_AUTH=hmac; SEPAY_LIVE_WEBHOOK_SECRET tối thiểu32 ký tự, không placeholder, khác secret Test nếu cả hai được khai báo. Không có API-key/no-auth fallback. |
| HTTPS | SEPAY_LIVE_PUBLIC_URL là origin HTTPS hợp lệ theo code; callback yêu cầu request.isSecure. Khai báo URL không chứng minh DNS/certificate/Tunnel hoặc host request đúng origin đó. |
| Tạo Payment mới | Tất cả chốt trên và SEPAY_LIVE_ACCEPT_NEW_PAYMENTS được phê duyệt/bật. Member ACTIVE/enabled, đúng owner/gói/chi nhánh và không có gói ACTIVE/xung đột Payment. Giá/thời hạn lấy snapshot. |
| Drain | ACCEPT_NEW_PAYMENTS=false chỉ chặn POST start/reuse; callback và QR của đơn cũ còn hợp lệ có thể tiếp tục. Không phải nút thu hồi mọi QR hoặc ngừng mọi khoản tiền ngân hàng có thể nhận. |

Endpoint cố định trong controller: **POST `/payments/sepay/webhook/live`**, JSON. URL triển khai mặc định context path rỗng: **`https://<hostname-live>/payments/sepay/webhook/live`**. Nếu tự đặt context path phải cộng đúng prefix, không mặc định URL trên còn nguyên. Profile Live bind127.0.0.1/port8080, forwarded headers framework: proxy trust phải do người vận hành bảo vệ, không chỉ dựa việc khai báo HTTPS.

HMAC dùng `timestamp + '.' + raw body`, X-SePay-Signature/X-SePay-Timestamp, constant-time, ±300s; payload32KiB/JSON strict. Chỉ ngoại lệ CSRF/login hai POST callback, không tắt bảo mật toàn app. Đối chiếu [SePay xác thực](https://developer.sepay.vn/vi/sepay-webhooks/xac-thuc).

Callback hợp lệ còn phải khớp tiền vào/account/gateway/reference/amount VND/thời hạn/trạng thái/eligibility. UNIQUE/idempotency và transaction dùng lại V706; cần kiểm chứng SQL Server, không coi H2 là bằng chứng filtered indexes/locking. ACK success:true có thể là đã lưu REVIEW/IGNORED, không chứng minh PAID. Quy tắc ACK: [SePay payload và response](https://developer.sepay.vn/vi/sepay-webhooks/tich-hop-webhook).

### Điểm cần lưu ý trước GO

- Nếu secret Test không được nạp vào process Live, code không thể chứng minh secret Live khác secret QA thực tế. Người quản trị phải kiểm tra tách secret/tài khoản thật; không cung cấp chúng cho agent.
- V708 không xác nhận các UNIQUE/FK/CHECK/index V706; DBA phải kiểm tra đúng cột/filter/enabled/trusted trước GO. Metadata permissions sai cũng có thể làm OBJECT_ID không nhìn thấy bảng.
- Code chưa có dry-run callback Live hoặc định danh payload mô phỏng được bảo đảm không kích hoạt gói. ACCEPT_NEW_PAYMENTS=false không chặn callback thanh toán đơn cũ. Không đưa payload mô phỏng khớp một đơn Live vào endpoint này.
- Payload parser bỏ qua subAccount; QR dùng accountNumber snapshot. Nếu ngân hàng yêu cầu VA hoặc payload accountNumber khác số nhận trên QR, cần xác minh mapping trước GO; không tự coi VA đã hỗ trợ. [SePay tài khoản/VA](https://developer.sepay.vn/vi/sepay-webhooks/tai-khoan-ngan-hang).
- Đối soát: Admin xem ledger tại /admin/payments/sepay-reconciliation; API lookup có quyền ADMIN/CSRF, mặc định tắt, GET chỉ đọc20 giao dịch/trang. API không tự import/kích hoạt. Không có cron đối soát hoặc workflow phê duyệt REVIEW/hoàn tiền; cần người trực và quy trình riêng. [SePay đối soát](https://developer.sepay.vn/vi/sepay-webhooks/doi-soat-giao-dich).
- API client dùng streaming response trong transaction read-only. Không coi request timeout10s là bằng chứng deadline cho toàn bộ đọc body khi provider chậm; API thực tế/tải connection cần kiểm chứng riêng trước bật lookup. Có thể giữ lookup tắt và dùng Dashboard.

## 3. Giao diện và regression — phân biệt bằng chứng

| Mục | Đã xác minh từ source/bằng chứng cũ | Chưa xác minh lượt này |
|---|---|---|
| VNPay trên mua gói | memberships.html không còn nút/form VNPay; service.start luôn từ chối. VNPAY trong lịch sử/filter/result cũ là chủ ý. | HTML runtime sau rebuild/cache trên trình duyệt |
| Member dùng SePay Test QA | Hai form SePay/CASH còn; Test dùng bank/secret QA, endpoint Test, không cần marker V708; test H2 start/reuse→HMAC→PAID/ACTIVE đã có. Test QR preview vẫn không phải QR ngân hàng sandbox để chuyển thật. | DB QA/schema/env/Dashboard hiện tại, thao tác browser thật |
| Admin Payment | Controller/service list/detail/filter còn, enum lịch sử giữ, link ledger và form lookup ADMIN còn. | Mở/filter/xem chi tiết trên website đang chạy; không khẳng định UI PASS chỉ vì đọc source |
| QR Check-in/Check-out, Review | Không có diff các module này so với HEAD; log regression H2 trước có QR/Review/Manager/Favorites. | Camera iPhone, UI/video Cloudinary thực tế sau deployment |

Đã đọc log có sẵn: lượt regression141/141 Java PASS và lượt unit cuối9/9 PASS (có chạy lại, không cộng150 test độc lập). 38 JS PASS là kết quả lượt trước đã ghi trong readiness, không phải chạy mới trong pre-live. Không sửa source để lấy thêm PASS.

## 4. Checklist chủ dự án tự thực hiện A–H

Tất cả checkbox dưới đây **chưa được agent thực hiện**. Phần ngân hàng/SePay yêu cầu chính chủ hoặc người được ủy quyền thao tác trên kênh chính thức.

### Bước A — Chuẩn bị tài khoản ngân hàng Live hợp lệ

- [ ] Bạn xác nhận quyền sử dụng tài khoản để nhận tiền cho UTEFit, đúng chủ thể, được ngân hàng/SePay cho phép và nắm phí/hạn mức/điều kiện dịch vụ.
- [ ] Chọn tài khoản nhận chuyên biệt, không dùng tài khoản giả Test. Ghi nhận nội bộ bank code VietQR, gateway, số nhận, chủ tài khoản; không gửi vào chat hoặc Git.
- [ ] Kiểm tra ngân hàng/loại tài khoản có hỗ trợ luồng dự kiến; nếu bắt buộc VA, dừng để xác minh mapping QR/accountNumber/subAccount trước chọn GO. Không phát sinh chuyển khoản để kiểm tra ở bước này.

**Bạn tự làm tại ngân hàng:** xác nhận quyền sử dụng/kích hoạt dịch vụ/ủy quyền cần thiết; không nhờ agent đăng nhập, nhận OTP hoặc liên kết thay.

### Bước B — Kết nối ngân hàng với SePay Dashboard

- [ ] Bạn vào Dashboard chính thức → Tài khoản ngân hàng → Thêm tài khoản, chọn đúng chế độ Live.
- [ ] Thực hiện liên kết theo hướng dẫn ngân hàng (có thể khác nhau); thông tin đăng nhập/OAuth/OTP chỉ nhập trên kênh chính thức. Nếu đã liên kết, kiểm tra thay vì thêm trùng.
- [ ] Xác nhận trạng thái hoạt động và đúng tài khoản; ghi nhận nhu cầu VA nếu có. Chưa thực hiện bước chuyển tiền nhỏ trong hướng dẫn nhà cung cấp.

Nguồn thao tác ngân hàng: [SePay liên kết tài khoản](https://developer.sepay.vn/vi/sepay-webhooks/tai-khoan-ngan-hang). Trạng thái hoạt động không thay thế bằng chứng UTEFit đã nhận tiền thật.

### Bước C — Máy chủ và HTTPS ổn định

- [ ] Chuẩn bị hostname ổn định, TLS hợp lệ, app8080 chạy bền vững, máy không sleep; Tunnel/proxy chỉ tới loopback, không mở origin ra Internet.
- [ ] Kiểm tra forwarded headers tin cậy, request.isSecure qua HTTPS, cookie Secure, NTP, timezone vận hành; không bỏ guard để chữa proxy.
- [ ] Chỉ POST webhook được miễn login/CSRF; không Cloudflare challenge/redirect/body rewrite ở endpoint này. Giữ CSRF/RBAC phần Member/Admin và không ghi header/body secret vào log.
- [ ] Kiểm tra URL callback đúng path/context path. DNS/HTTPS bình thường không đồng nghĩa HMAC/DB đã PASS.

**Bạn tự làm trên máy chủ/Cloudflare:** cấu hình hostname/Tunnel/trust boundary, chứng chỉ, bảo vệ và giám sát; agent không thay đổi hạ tầng.

### Bước D — Biến môi trường ngoài source

- [ ] Dùng mẫu .env.sepay-live.example làm danh sách, không lưu file env thật vào repo. Spring không tự đọc .env; đặt trong IntelliJ Run Configuration/secret store.
- [ ] Profile và DB: SPRING_PROFILES_ACTIVE, SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD, SPRING_JPA_HIBERNATE_DDL_AUTO, SPRING_SQL_INIT_MODE, SEPAY_LIVE_DATABASE_NAME. Chọn DB Live riêng, không sửa cấu hình QA hiện có.
- [ ] Bank: SEPAY_LIVE_BANK_CODE, SEPAY_LIVE_GATEWAY, SEPAY_LIVE_ACCOUNT_NUMBER, SEPAY_LIVE_ACCOUNT_NAME.
- [ ] Webhook: SEPAY_WEBHOOK_AUTH, SEPAY_LIVE_WEBHOOK_SECRET. Secret Live ngẫu nhiên mạnh, khác Test; Bearer API token không thay HMAC secret.
- [ ] Deployment: SEPAY_ENVIRONMENT, SEPAY_LIVE_PUBLIC_URL, SEPAY_LIVE_DEPLOYMENT_ID, SEPAY_LIVE_APPROVAL. Tham khảo điều kiện code ở mục2; không nhập approval hoặc tạo marker chỉ để thử cho qua guard.
- [ ] Dùng SEPAY_PAYMENT_PREFIX/SEPAY_PAYMENT_MINUTES hợp lệ; kiểm tra JWT/mail/Cloudinary credentials riêng và cookie Secure. SEPAY_COOKIE_SECURE chỉ điều khiển profile sepay; profile sepay-live đặt Secure trực tiếp, vẫn phải rà override cuối cùng.
- [ ] Trong pre-live giữ SEPAY_ENABLED=false, SEPAY_LIVE_ENABLED=false, SEPAY_LIVE_ACCEPT_NEW_PAYMENTS=false; không bật API lookup. Các giá trị cấu hình không tự chứng minh ngân hàng đã liên kết.
- [ ] API lookup chỉ tùy chọn sau duyệt: SEPAY_LIVE_API_LOOKUP_ENABLED, SEPAY_LIVE_API_TOKEN quyền chỉ đọc, SEPAY_LIVE_BANK_ACCOUNT_ID. Không có token vẫn có thể nhận HMAC và đối soát Dashboard.

### Bước E — Backup, kiểm tra, áp dụng V708

- [ ] **Bạn/DBA** kiểm tra instance/catalog thực tế bằng phiên riêng, backup và thử restore sang bản sao. Rà dữ liệu tài chính/QA toàn DB, không chỉ hai bảng SePay.
- [ ] Trên bản sao, xác minh core migrations cần cho binary, V706/schema/index/constraint/quyền; sau đó thử V708 và rollback. Không chạy V708 lên DB chính/QA chỉ để thử.
- [ ] Review toàn bộ V708, xác nhận không có transaction ngoài; chọn cửa sổ phù hợp vì DDL có lock.
- [ ] Sau phê duyệt triển khai schema riêng, bạn/DBA mới áp dụng vào đúng Live DB, lưu kết quả và kiểm tra guard có cấu trúc/constraint đúng, **rỗng nếu mới tạo**. Không đổi Payment/Membership.
- [ ] Chưa cấp phê duyệt nhận tiền. Row guard là thao tác DBA **riêng sau quyết định cho kiểm tra tiếp**, không nằm trong V708. Runtime phải không có quyền ghi guard. Không tạo row hoặc đổi cờ trong lượt pre-live của agent.

Nếu lỗi, theo quy trình mục1; không tự sửa migration cũ/drop bảng/restore đè dữ liệu mới. Thiếu backup, index đúng hoặc chưa thử bản sao → NO-GO.

### Bước F — Tạo Webhook Live đúng HMAC

- [ ] **Bạn tự làm sau khi duyệt cấu hình:** Dashboard → Tích hợp → Webhooks → Thêm webhook.
- [ ] URL https://<hostname-live>/payments/sepay/webhook/live; tiền vào, JSON, tài khoản cụ thể; không chọn tất cả tài khoản.
- [ ] Chọn HMAC-SHA256, secret khớp biến Live; retry và cảnh báo phù hợp. Không No Auth/API Key.
- [ ] Mã nhận diện khớp prefix ứng dụng và24 ký tự hex; tránh bộ lọc làm mất khoản sai nội dung. Giữ webhook tắt trong lúc chưa được phép tiếp nhận; kiểm tra trạng thái sau lưu.

Nguồn: [SePay tạo Webhook](https://developer.sepay.vn/vi/sepay-webhooks/tao-webhook). Agent không thực hiện hoặc tạo Webhook thay bạn.

### Bước G — Kiểm tra kết nối/log, không phát sinh tiền thật

- [ ] Giữ ba cờ nhận tiền/tạo đơn tắt. Bạn kiểm tra DNS/TLS/Tunnel/app và endpoint bằng thao tác được phép; POST JSON khi SePay bị tắt dự kiến503/success:false. Đây chỉ chứng minh đường truyền tới app, **không phải HMAC Live PASS**. GET không được coi là kiểm tra POST.
- [ ] Trên **QA Test riêng**, dùng đúng credentials Test và giao dịch mô phỏng để kiểm tra HMAC/receipt/PAID/ACTIVE; không trỏ Test vào Live DB. Bạn tự kiểm tra Member/Payment/Admin và regression giao diện, không chuyển tiền ngân hàng.
- [ ] Dashboard có chức năng Gửi thử, nhưng payload mẫu có thể dùng id=0; parser hiện từ chối id<=0. Vì vậy400 không tự chứng minh secret sai. Khi cờ tắt,503 xảy ra trước kiểm tra HMAC. Nguồn: [SePay Gửi thử](https://developer.sepay.vn/vi/sepay-webhooks/tao-webhook).
- [ ] **Không dùng Gửi thử/phát lại/mô phỏng vào Live để khớp đơn thật.** Không có dry-run route. Nếu sau này có phê duyệt riêng cho thử kết nối authenticated Live, phải review payload/ảnh hưởng receipt, không có đơn có thể bị khớp và không dùng ngân hàng chuyển tiền; agent chưa thực hiện hoặc phê duyệt bước đó.
- [ ] Đọc delivery status/time/response đã che thông tin nhạy cảm. Phân biệt401 chữ ký,400 JSON/id,404 môi trường sai khi config hợp lệ,503 cờ/cấu hình/DB. Khi cờ tắt không kỳ vọng mọi lỗi đều phân biệt được bởi handler đã dừng sớm.
- [ ] Nếu thiếu Webhook sau này: Dashboard → kiểm tra delivery/retry, đối chiếu ledger và ngân hàng; tùy chọn API read-only. Phát lại là thao tác có thể ghi tài chính, cần đúng giao dịch thật/đúng môi trường/ủy quyền; không tự ép PAID hoặc chuyển thêm.

**Kết quả bước G có thể là PARTIAL:** QA HMAC PASS + HTTPS reachability PASS nhưng Live authenticated delivery chưa VERIFIED. Không được đổi nhãn thành Live VERIFIED để đủ checklist.

### Bước H — GO / NO-GO

Chỉ chủ dự án/người quản trị được quyết định. Hai mức phê duyệt khác nhau:

1. **GO cho kiểm tra kết nối Live có kiểm soát:** backup/schema/DB guard/permission/HTTPS/secret/bank mapping đã xác nhận; có phê duyệt riêng trước tạo marker và bật xử lý callback; vẫn giữ ACCEPT_NEW_PAYMENTS=false. Không mặc định việc này an toàn nếu còn đơn Live cũ vì callback có thể xác nhận chúng.
2. **GO cho giao dịch thật đầu tiên:** tất cả cổng đã đủ bằng chứng, người có quyền ngân hàng/SePay đồng ý, không trộn dữ liệu, SQL Server race/rollback đã kiểm chứng, có người trực đối soát và kế hoạch drain/incident. Chủ dự án mới tự cho tạo đơn/chuyển khoản theo một kế hoạch được duyệt riêng. Live VERIFIED chỉ sau kết quả thật, không trước.

- [ ] Không có blocker cấu hình/schema/VA/quyền hoặc payload mẫu có thể gây kích hoạt nhầm.
- [ ] Backup/restore và kiểm thử SQL Server đã có bằng chứng; runtime không DDL/db_owner/ghi guard.
- [ ] Không có Live secret/bank data trong Git/log/chat; mọi credential đã từng bị lộ được xử lý theo quy trình riêng, không sửa lịch sử Git trong lượt này.
- [ ] Đối soát REVIEW/thiếu Webhook/tiền thiếu/thừa/muộn có người phụ trách, không tự hoàn tiền hoặc sửa SQL.
- [ ] Rollback đã duyệt: tắt ACCEPT_NEW_PAYMENTS để dừng phát hành đơn, giữ callback/DB/secret để drain đơn cũ; không restore DB/drop receipt làm mất tiền. Tắt SEPAY_ENABLED chặn cả callback, chỉ dùng khi incident yêu cầu.

**NO-GO nếu bất kỳ cổng quan trọng chưa có bằng chứng. Hiện tại vẫn NO-GO.** Chưa có kết luận tài khoản ngân hàng thật/DB/HTTPS/Webhook thực tế đã sẵn sàng chỉ từ các Java/JS test.

## 5. Thay đổi trong lượt pre-live

Chỉ tạo tài liệu này và thêm liên kết trong SEPAY_LIVE_READINESS.md. Không sửa Java/JavaScript/template/properties/migration; không đụng các thay đổi chưa commit có sẵn, .ai/, .playwright-mcp/, docs/ hoặc cấu hình local. HEAD và checkpoint giữ nguyên, không stage/commit.
