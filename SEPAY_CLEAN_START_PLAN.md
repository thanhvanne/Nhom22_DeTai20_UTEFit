# UTEFit — DEMO SEPAY LIVE CÓ KIỂM SOÁT, GIỮ NGUYÊN DỮ LIỆU MẪU

Cập nhật theo quyết định mới nhất của chủ dự án, 30/10/2026. Kế hoạch này **thay thế phương án khóa/xóa demo trước đây**. Giữ main; không commit/push. Hiện vẫn **NO-GO để tự bật Live**, không phải từ chối mục tiêu thử một giao dịch thật sau khi có cổng truy cập phù hợp.

## 1. Quyết định được giữ nguyên

- Giữ đủ **55 tài khoản ACTIVE/enabled**: 10 ADMIN, 5 MANAGER, 20 MEMBER, 20 PT. Không bắt buộc tạo Admin thật mới, không khóa/xóa/đổi trạng thái tài khoản mẫu.
- Giữ **21 Payment CASH**, 21 Membership và các quan hệ/dữ liệu mẫu khác. Không archive rồi DELETE, không đổi PAID thành FAILED/REFUNDED để chỉnh số liệu, không reset identity hoặc dựng lại DB.
- Không migrate 7 Payment, Membership, receipt, claim hoặc approval của LiveDemo cũ vào UTEFitDB.
- Có thể dùng một Member demo để thử SePay Live bằng tiền/ngân hàng do chủ máy kiểm soát; **loại tài khoản demo không quyết định giao dịch đó là tiền giả**.
- Không thay Java, SecurityConfig, SepayDatabaseGuard, logic HMAC/amount/account/VA/idempotency/transaction hoặc UI Admin Light/Member Navy/Public Cinematic.
- Các script cũ 05_owner_admin, 06_retire_seed_users, 07_remove_archived_demo_finance trong `.local/sepay-clean-start/` đã được thay bằng **THROW CANCELLED vô điều kiện**. Bản strict approval riêng của kế hoạch cũ cũng bị hủy để tránh chạy nhầm. Không có phần ghi dữ liệu nào của chúng đã được agent thực hiện.
- Kế hoạch cũ được lưu ở `.local/sepay-controlled-demo/superseded-clean-start-plan.md` chỉ để truy vết; không còn là hướng dẫn triển khai.

## 2. Vì sao script 03 hiện tại vẫn chặn?

`database/operations/UTEFitDB_03_live_approval.template.sql` hiện có hai lớp liên quan:
1. Yêu cầu người chạy xác nhận `@KnownDemoCredentialsRetired=1`.
2. Truy vấn thật: còn bất kỳ `email LIKE '%@utefit.test'` có `enabled=1 AND status='ACTIVE'` thì THROW 51433.

Chỉ đặt cờ retired=1 không vượt được truy vấn, đồng thời là xác nhận sai khi giữ demo. Không đổi email, không khóa tượng trưng rồi mở lại, không comment riêng IF/THROW để cấp approval. Mục đích chính sách cũ là tránh credential công khai trở thành quyền Admin trên một ứng dụng nhận tiền công khai.

**Java SepayDatabaseGuard không kiểm tra demo user hoặc DatabasePurpose**; nó kiểm tra cấu hình, catalog, row LIVE/deployment ID và dữ liệu môi trường. Do đó bỏ chốt ở SQL mà không thay bằng kiểm soát truy cập thật sẽ nguy hiểm, dù startup có thể qua.

File 03 gốc hiện còn điền sẵn 5 cờ=1 và ID cũ. **Không chạy nguyên trạng.** Agent giữ file chính thức này không đổi; chế độ CONTROLLED_DEMO dưới đây mới chỉ là bản sửa **đề xuất**, chưa phải chính sách đã triển khai.

## 3. Chính sách đề nghị: OWNER-ONLY CONTROLLED DEMO

Đây là ngoại lệ hữu hạn cho đồ án có giám sát, **không phải chuẩn vận hành Public/Production**. Mật khẩu demo vẫn được coi là công khai. RBAC trong app vẫn cho Admin demo đầy đủ quyền; lớp bổ sung chặn người ngoài trước khi họ vào ứng dụng.

### Phương án phù hợp công cụ hiện có: hai Quick Tunnel tách biệt

```text
Chủ máy + email OTP độc lập
  -> HTTPS UI_URL có --allowed-mail chỉ email chủ máy
  -> 127.0.0.1:8080 (toàn bộ UTEFit, kể cả /login, /admin, /member, /ws)

SePay
  -> HTTPS HOOK_URL không email challenge
  -> 127.0.0.1:8081 (proxy chỉ webhook)
  -> đúng POST /payments/sepay/webhook/live trên 127.0.0.1:8080
  -> backend xác minh HMAC + dữ liệu + transaction
```

Máy có cloudflared 2026.10.3; đã kiểm tra help hỗ trợ `--allowed-mail`. [Cloudflare mô tả email OTP cho Quick Tunnel, không hỗ trợ client không tương tác](https://developers.cloudflare.com/tunnel/get-started/quick-tunnels/). Vì vậy **không đặt challenge email lên HOOK_URL**, cũng không mở toàn app bằng URL webhook.

Các lệnh sau **chưa chạy**, chỉ dành cho chủ máy sau review cấu hình chuẩn bị:
```powershell
node .local/sepay-controlled-demo/webhook-only-proxy.cjs --serve-reviewed
cloudflared tunnel --url http://127.0.0.1:8080 --allowed-mail "<EMAIL_THAT_CHU_MAY_KIEM_SOAT>"
cloudflared tunnel --url http://127.0.0.1:8081
```

- Thay email placeholder bằng mailbox thật, có bảo vệ độc lập; không dùng email/mật khẩu mẫu, không wildcard `*@domain`, không allow Everyone, không chia sẻ OTP/session cổng ngoài.
- Khi Live đang bật, phương án mặc định là **chỉ chủ máy vào UI, giảng viên xem trình chiếu**. Chủ máy có thể đổi giữa 55 tài khoản để trình diễn đầy đủ mà không đổi dữ liệu/quyền.
- Nếu thêm người truy cập UI, họ cũng có thể biết Admin demo và có toàn quyền sau cổng ngoài. Email allowlist nhiều người **không tự phân quyền tài chính theo từng vai trò**. Muốn người khác trực tiếp thao tác từ máy riêng trong lúc Live bật phải chốt thêm chính sách owner-only cho các route tài chính/mutation; chưa triển khai hoặc kiểm chứng phần đó. Không đánh dấu owner-only PASS nếu đang cho cả lớp truy cập.
- Mỗi URL UI mới phải luôn được tạo với email gate. Không dùng URL khó đoán, IP spoofable hoặc việc “không chia sẻ link” làm biện pháp xác thực.
- Spring và proxy phải chỉ listen loopback; không có tunnel/port-forward/ngrok/hostname khác mở thẳng vào 8080, kể cả URL cũ. SQL/runtime credential và tài khoản máy phải được bảo vệ, không mở SQL/web origin trực tiếp ra Internet.
- Nếu dùng named tunnel + Cloudflare Access thay Quick Tunnel, áp dụng cùng nguyên tắc deny-by-default/owner-only và callback-only. Đây là lựa chọn khác, chưa cấu hình trên máy.
- Quick Tunnel không có uptime guarantee; chỉ dùng phiên demo có người giám sát. Không tuyên bố đây là nền tảng nhận tiền liên tục.

### Proxy webhook đã chuẩn bị, chưa khởi chạy

`.local/sepay-controlled-demo/webhook-only-proxy.cjs` là **hạ tầng cục bộ đề xuất**, không sửa ứng dụng:
- Mặc định không listen; chỉ khởi chạy khi chủ máy chủ động dùng `--serve-reviewed`; bind 127.0.0.1:8081, upstream cố định loopback:8080.
- Chỉ cho đúng POST/path Live; /login, /admin, /member, /ws, path encoded/query/đường khác không được chuyển tiếp. Không làm public reverse proxy toàn app.
- Giữ nguyên raw body và hai header chữ ký/timestamp, giới hạn 32 KiB, JSON/identity encoding, từ chối header xác thực lặp. Chỉ kiểm tra cú pháp header, **không thay backend xác minh HMAC**.
- Không chuyển Cookie/Authorization/Forwarded/prefix tùy ý từ Internet; đặt scheme HTTPS cho đoạn proxy được kiểm soát. Header scheme riêng không thay bằng chứng TLS ngoài Internet.
- Không tự ACK thành công; trả kết quả backend, lỗi upstream trả lỗi. Không lưu secret, raw payload hoặc thông tin giao dịch vào log.
- Đã có 6 test thuần hàm PASS, không mở socket hoặc gửi request. **Chưa chứng nhận proxy/Cloudflare/forwarded HTTPS/delivery thật đã hoạt động.**

Source hiện tại không bắt host callback trùng `SEPAY_LIVE_PUBLIC_URL`; callback kiểm tra path, HTTPS và HMAC. Đặt **SEPAY_LIVE_PUBLIC_URL=UI_URL**, origin WebSocket theo UI; Dashboard SePay đặt **HOOK_URL + /payments/sepay/webhook/live**. Hai origin phải khác nhau, không lấy HOOK_URL làm cổng đăng nhập.

## 4. Báo cáo tách tiền demo và SePay Live, không sửa lịch sử/UI

Đã chuẩn bị và chạy `.local/sepay-controlled-demo/revenue-separated.readonly.sql`. Chỉ SELECT và table variable trong phiên; không thêm field/trạng thái DB, không sửa Payment.

Phân loại:
- **DEMO_SEED_CASH**: whitelist 21 Payment ID cụ thể gắn manifest database/seed, owner, Membership, reference và amount; không dựa chỉ vào LIKE DEMO hoặc email.
- **SEPAY_LIVE_CONFIRMED_BY_BACKEND**: Payment SEPAY/PAID, details LIVE/reference khớp, có MATCHED webhook hoặc manual APPLIED phù hợp. EXISTS tránh cộng tiền nhiều lần khi có nhiều receipt.
- SePay PAID thiếu nguồn xác nhận -> NEEDS_REVIEW, không cộng như khoản đã xác nhận.
- SePay chưa PAID/REFUNDED và các phương thức khác được tách riêng; không cộng ledger vào tiền Payment lần nữa.
- Một giao dịch Live mới của Member `@utefit.test` vẫn được tính vào nhóm SePay đã xác nhận; **không lọc doanh thu bằng email/role demo**.
- Không dùng phép trừ cố định 13.800.000đ cho mọi kỳ. Có tham số thời gian Việt Nam [from, to), phân loại từng dòng. Nếu Payment CASH seed được xác nhận qua chức năng demo và mã đổi thành CASH-id, nó vẫn là seed trong manifest; không tự biến thành doanh thu SePay.
- Không dùng lại một Payment seed làm chứng từ thu tiền thật. Nếu manifest thay đổi ngoài các thao tác dự kiến, báo cáo dừng để review.

Kết quả chỉ đọc hiện tại:
| Chỉ tiêu | Kết quả |
|---|---:|
| CASH demo PAID được tách khỏi báo cáo SePay | 20 khoản / 13.800.000đ |
| CASH demo PENDING giữ nguyên | 1 khoản / 300.000đ |
| SePay Live PAID có xác nhận backend | 0đ |
| SePay PAID cần review | 0đ |

**Dashboard Admin hiện tại vẫn cộng mọi Payment PAID**, nên tổng 13.800.000đ ở đó là tổng demo/mixed, không phải doanh thu SePay thật. Báo cáo SQL riêng dùng để trình bày/đối soát tài chính; chưa sửa UI/Statistics Java để tự gắn nhãn hoặc lọc. Nếu muốn có các ô tách ngay trong dashboard, đó là thay đổi riêng cần duyệt sau, giữ phong cách hiện tại.

“Backend confirmed” không thay sao kê ngân hàng độc lập hoặc báo cáo kế toán. Không gọi toàn bộ Payment PAID là tiền ngân hàng đã được người kiểm tra sao kê xác minh.

## 5. Kết quả kiểm tra lại ngày 30/10/2026

| Điều kiện | Bằng chứng / trạng thái |
|---|---|
| Giữ dữ liệu mẫu | Đã đọc lại: 55 users ACTIVE/enabled đúng 4 nhóm; 21 CASH/21 Membership/quan hệ vẫn tồn tại |
| Runtime sau 02 | Ma trận 27 bảng có 0 sai lệch, SID đúng, không db/server role, guard SELECT-only, từng cột UPDATE=0 |
| Schema | 28 bảng; CHECK/FK kiểm tra không disabled/untrusted; không cần dựng lại/migration để giải quyết policy |
| Dữ liệu SePay/approval UTEFitDB | details/webhooks/claims/guard đều 0; purpose DEVELOPMENT_ONLY; chưa phê duyệt |
| Backup mới | Full COPY_ONLY 02:39 ngày 30/10, đúng DB mới; has_backup_checksums=false, không được báo checksum PASS |
| Restore thử | Chủ máy đã xác nhận làm; chưa đối chiếu được tên DB/Messages/CHECKDB; msdb.restorehistory rỗng, không thấy bản restore riêng trên instance này. Không suy ra chắc chắn chưa làm ở nơi khác |
| cloudflared/email gate | Có binary và flag; lúc kiểm tra 0 cloudflared đang chạy, **chưa có cổng owner-only hoạt động được kiểm chứng** |
| HTTPS origin đã lưu | educators-seal-career-mark.trycloudflare.com không resolve; GET /login lỗi DNS 6. Không có TLS/cookie PASS |
| App | Không kết nối được loopback:8080 lúc kiểm tra; chưa chạy app/proxy/tunnel |
| Bank/main/VA | Config đã lưu khớp main/bank/gateway 7/7 snapshot cũ, VA 6 đơn có VA; không đổi bank |
| HMAC | Config hmac, secret Live có mặt/đủ chiều dài và khác 1 Test secret đã lưu; chưa xác minh secret trên Dashboard hoặc delivery mới |
| LiveDemo cũ | 7 PAID/270.000đ, 6 MATCHED + 1 manual APPLIED, không Payment PENDING/REVIEW/conflict tìm thấy; không migrate |
| Quyền lợi/tiền đợt cũ | Chưa có xác nhận tất cả là tự kiểm thử hoặc đã chốt nghĩa vụ với người khác |
| Chính sách controlled demo | Mới là đề xuất; file 03 chính thức vẫn STRICT và chặn demo ACTIVE |

**Điểm cần xử lý ngay trước Run:** cấu hình IntelliJ được chọn đang lưu `SEPAY_ENABLED=true`, `SEPAY_LIVE_ENABLED=true`, `SEPAY_LIVE_ACCEPT_NEW_PAYMENTS=true` và `SEPAY_MANUAL_RECONCILIATION_ENABLED=true`. Chưa thay đổi chúng. Không cấp approval rồi chạy nguyên cấu hình này.

Impersonation SQL không chứng minh password/TCP/JDBC TLS. Audit metadata Windows-auth dùng TrustServerCertificate chỉ cho phiên kiểm tra, không phải chứng nhận Java TLS Live. Backup/restore cần tên/path/kết quả thực tế; có thể dùng bằng chứng bạn đã có, không bắt reset DB.

## 6. Đề xuất sửa chính sách approval, chưa áp dụng

Bản đầy đủ: `.local/sepay-controlled-demo/03_approval-policy.PROPOSED-NOT-APPLIED.sql`.

- Dựa trên script 03 chính thức, giữ nguyên toàn bộ phần kiểm tra TEST, runtime guard permissions, khóa singleton, từ chối approval cũ, INSERT approval + purpose cùng transaction, REVERT/rollback.
- Mặc định **STRICT**, tất cả cờ xác nhận bằng **0**, ID/origins/evidence NULL. STRICT vẫn yêu cầu credential demo retired và không có demo ACTIVE, không nới chính sách cho triển khai public thông thường.
- Chỉ khi DBA/chủ dự án chủ động chọn **CONTROLLED_DEMO** mới thay yêu cầu retired bằng các chốt: owner chấp nhận phạm vi demo; cổng UI độc lập chỉ cho owner đã kiểm chứng; ingress public chỉ webhook; không đường bypass; manual reconciliation tắt; kế hoạch kết thúc phiên/drain đã review; có hồ sơ bằng chứng và hai HTTPS origin khác nhau.
- Chế độ này **bắt buộc KnownDemoCredentialsRetired=0**: không khai sai tài khoản đã bị khóa. Đồng thời kiểm tra 55 tài khoản mẫu vẫn ACTIVE/enabled, đúng 10/5/20/20.
- Vẫn yêu cầu OfflinePreflightReviewed, OldLiveDemoOrdersReconciled, DemoCashAccountingReviewed và Approve theo quyết định thật. DemoCashAccountingReviewed nghĩa là báo cáo tách đã được duyệt, **không còn nghĩa là phải xóa 21 CASH**.
- Deployment ID có prefix UTEFIT_DEMO_ để phân biệt ý định triển khai. ID đề xuất mới, chưa cấu hình/ghi DB:
```text
UTEFIT_DEMO_20261030_19ff276720d04e828afa3d280fce901b
```
- DB purpose sau approval vẫn LIVE_FINANCIAL vì có tiền thật; không dùng Test để ngụy trang. Không có field “đã xác minh network” mới trong backend/DB.
- **SQL chỉ kiểm tra khai báo/định dạng/điều kiện DB, không tự kiểm tra Cloudflare, firewall, mailbox, evidence hay tự hết hạn approval.** Những cờ network chỉ được đặt 1 sau khi có kiểm tra thực tế và người chịu trách nhiệm duyệt. Không coi một chuỗi evidence hoặc PRINT là chứng nhận.
- Nếu gate mất, role ADMIN demo trong app vẫn có quyền; marker DB không bảo vệ được thay cho gate. Không đủ điều kiện thì giữ Approve=0/guard rỗng. Không chỉ bỏ IF demo khỏi file gốc.

Đây là **thay đổi chính sách đề nghị cần bạn/DBA duyệt**, chưa sửa file chính thức, chưa chạy bản đề nghị, chưa tạo approval giả. Không dùng bản gốc đang có cờ=1, không trích riêng INSERT. Java guard không bị sửa/xóa/bỏ qua.

## 7. Kiểm chứng cổng ngoài phải làm trước GO

Thực hiện khi các cờ tiền tắt, không cần gửi webhook/giao dịch:
1. Chủ máy truy cập UI_URL, xác thực mailbox thật qua cổng ngoài rồi login bằng các tài khoản demo cần trình diễn; test cookie Secure, redirect, Member và WebSocket/Chat.
2. Từ trình duyệt sạch/máy khác không được cấp email, UI_URL /login, /admin, /member, /manager, /pt, /ws và cả request POST liên quan phải bị cổng ngoài chặn trước app. Không chỉ kiểm tra trang chủ; không thử thao tác thay dữ liệu.
3. Qua HOOK_URL: GET /login, /admin, /member, /ws và mọi đường không callback phải không tới app. Không tin chỉ ingress rule trên giấy; kiểm tra URL thật, host khác và alternate tunnel.
4. Kiểm tra app/proxy chỉ bind loopback, không public direct origin hoặc tunnel cũ, không để client chọn arbitrary upstream/path. Callback public không có login redirect/email challenge; kiểm tra cấu hình routing không cần ký payload giả.
5. Chủ tài khoản tự đối chiếu webhook URL + HMAC secret + account/VA, giờ/NTP/retry trên Dashboard. Secret không gửi vào chat; không dùng nút gửi thử hay replay khớp đơn Live để chứng minh an toàn.

Chưa thực hiện các negative test trên hạ tầng thật vì hiện không có app/tunnel mới. 6 test thuần hàm không thay bằng chứng này. Kiểm tra HMAC delivery end-to-end với giao dịch thật chỉ có sau khi bạn duyệt thử tiền; không báo Live VERIFIED trước đó.

## 8. Các bước còn thiếu để thử một giao dịch thật

### Chuẩn bị, tiền vẫn tắt
- Chốt owner-only mailbox và cách trình chiếu; bảo vệ mailbox/máy, không chia sẻ phiên cổng ngoài. Không cần khóa 55 user hoặc tạo Admin mới trong DB.
- Xác minh backup/restore hiện có; cung cấp tên DB/Message/CHECKDB hoặc làm lại backup CHECKSUM/restore sang bản riêng nếu cần. Không ghi đè DB hiện tại.
- Tự đặt trong đúng Run Configuration:
```text
SPRING_PROFILES_ACTIVE=sepay,sepay-live
SEPAY_ENVIRONMENT=live
SEPAY_ENABLED=false
SEPAY_LIVE_ENABLED=false
SEPAY_LIVE_ACCEPT_NEW_PAYMENTS=false
SEPAY_MANUAL_RECONCILIATION_ENABLED=false
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
SPRING_SQL_INIT_MODE=never
```
- Giữ bank/main/VA/secret Live hợp lệ. Khởi động chuẩn bị, thiết lập hai tunnel/proxy như mục 3; lấy UI_URL/HOOK_URL, đặt SEPAY_LIVE_PUBLIC_URL=UI_URL và restart chỉ app, giữ tunnel để hostname không đổi.
- Kiểm chứng mục 7, duyệt báo cáo tiền tách; chốt chính sách controlled-demo và hồ sơ bằng chứng trước khi duyệt approval.

### Đóng đợt cũ không migrate
Đối chiếu tổng 7 khoản/270.000đ với ngân hàng/SePay và failed/retrying delivery ngoài DB. Nếu chủ tài khoản xác nhận tất cả là tự kiểm thử, không còn nghĩa vụ với người khác: lập mốc đóng đợt, giữ DB/backup và sổ ngân hàng, ngừng dùng QR cũ; không cần nhập từng giao dịch. Nếu còn quyền lợi/hoàn tiền, chốt phương án hợp lệ riêng, không tự hủy hoặc tự tuyên bố hết nghĩa vụ.

Việc hết Payment pending trong DB không ngăn tiền tới muộn theo QR cũ. Không replay 7 khoản đã kết thúc sang UTEFitDB. Giữ người theo dõi khoản muộn/UNKNOWN_REFERENCE.

### Phê duyệt DB rồi bật callback, chưa mở đơn
Chỉ sau bạn duyệt chính sách sửa và đủ bằng chứng, DBA mới dùng bản 03 đã được duyệt, đúng UTEFitDB/ID mới, từng xác nhận đúng sự thật. Không tự đặt cờ=1. Sau quyết định riêng cho xử lý callback:
```text
SEPAY_ENVIRONMENT=live
SEPAY_LIVE_DATABASE_NAME=UTEFitDB
SEPAY_LIVE_DEPLOYMENT_ID=UTEFIT_DEMO_20261030_19ff276720d04e828afa3d280fce901b
SEPAY_LIVE_APPROVAL=I_APPROVE_SEPAY_LIVE
SEPAY_ENABLED=true
SEPAY_LIVE_ENABLED=true
SEPAY_LIVE_ACCEPT_NEW_PAYMENTS=false
SEPAY_MANUAL_RECONCILIATION_ENABLED=false
```
Restart runtime đúng catalog, guard phải qua nguyên vẹn. Đây đã cho callback xác thực ghi DB, không phải dry-run. Sau duyệt cutover mới dùng HOOK_URL đúng ở Dashboard; không có anonymous URL khác mở toàn app.

### Một đơn mới có giám sát
Chỉ sau GO riêng, bật ACCEPT_NEW_PAYMENTS=true/restart. Chủ máy dùng Member demo phù hợp để tạo **một đơn mới** từ catalog đã duyệt; không chiếm Payment CASH PENDING cũ, không INSERT Payment hoặc thay số tiền bằng SQL. Kiểm tra bank/main/VA/amount/reference/expiry.

Sau khi phát hành đúng đơn, có thể pause lại ACCEPT_NEW_PAYMENTS=false và restart có kiểm soát, giữ callback/tunnel/guard; QR đơn cũ còn hợp lệ theo source. Đây là giới hạn vận hành, **không có hard limit một giao dịch tự động trong code**. Chỉ chủ máy tự chuyển tiền sau cho phép; agent không chuyển.

Backend vẫn phải xác thực HMAC/raw bytes/time, account/VA/gateway, amount/reference/eligibility/expiry, UNIQUE/lock/transaction trước PAID/ACTIVE. Polling, hình QR hoặc lời nói “đã trả” không kích hoạt gói. [HMAC chính thức SePay](https://developer.sepay.vn/vi/sepay-webhooks/xac-thuc).

Sau giao dịch: chủ tài khoản đối chiếu ngân hàng/delivery, kiểm tra PAID/Membership/báo cáo SePay tách. Chậm trạng thái không chuyển lại. Chưa có bằng chứng thì không Live VERIFIED.

### Kết thúc hoặc sự cố
Pause đơn mới, giữ callback cần thiết để drain/đối soát; đóng/revoke truy cập UI khi không giám sát. Nếu gate UI bị mất, đóng ngay đường UI và pause đơn, không để Admin demo mở công khai; khôi phục gate trước tiếp tục. Nếu proxy webhook lộ route UI, đóng đường đó và lập kế hoạch retry/đối soát, không tắt HMAC hoặc ACK giả. Giữ toàn bộ 55 ACTIVE và 21 CASH; không dùng khóa/xóa account làm thủ tục kết thúc phiên.

## 9. Những gì thực sự còn thiếu

1. Owner-only gate và callback-only tunnel/proxy **đang chạy, đã test không có đường bypass**; đây là điều kiện thay thế cho việc retire demo, không phải chỉ một checkbox.
2. Backup/restore drill có bằng chứng đối chiếu; actual runtime JDBC/TLS/cookie.
3. Đổi các cờ đang lưu true sang cấu hình chuẩn bị false, đặc biệt manual reconciliation đang true.
4. HTTPS UI_URL/HOOK_URL mới hợp lệ và Dashboard URL/secret/account/VA được chủ tài khoản đối chiếu.
5. Xác nhận đóng đợt tự kiểm thử cũ hoặc xử lý nghĩa vụ còn lại; không yêu cầu migrate 7 khoản.
6. Duyệt cách trình bày báo cáo tách và bản sửa chính sách 03; sau đó mới duyệt approval DB, callback và đơn mới theo từng bước.

**Không còn yêu cầu**: khóa 55 demo, tạo Admin mới bắt buộc, xóa 21 CASH/Membership, dọn/reset DB hoặc nhập LiveDemo. Không đánh dấu network/restore/HMAC end-to-end PASS khi chưa có bằng chứng.

## 10. File và kiểm tra của lượt cập nhật

- Cập nhật kế hoạch này; vô hiệu hóa các bản 03 riêng/05/06/07 của phương án cũ và thay kiểm tra cũ bằng kiểm tra cancellation.
- Đề xuất mới chỉ nằm trong `.local/sepay-controlled-demo/`: policy SQL mặc định chặn, báo cáo SELECT, proxy chưa chạy, test thuần hàm, ID đề xuất và JSON audit đã che secret.
- Đã chạy preflight 01, truy vấn kiểm kê/restore/ledger/bank mapping, báo cáo SELECT. Chỉ table variable trong phiên khi báo cáo, không business DML.
- 6 test proxy thuần hàm PASS; kiểm tra cancellation/policy/body guard gốc PASS; ScriptDom policy SQL và báo cáo 0 lỗi cú pháp. Không thay chứng nhận SQL approval chạy được, network thực tế hoặc giao dịch Live.
- Không sửa file 03 chính thức, Java/UI/security/migration/Run Configuration hoặc dữ liệu. Không khởi chạy app/proxy/tunnel, POST webhook, gọi API tài chính/chuyển tiền, commit/push hoặc thao tác Git làm đổi trạng thái.

**Dừng chờ duyệt phương án kiểm soát truy cập và chính sách đề nghị; chưa phê duyệt/bật Live.**
