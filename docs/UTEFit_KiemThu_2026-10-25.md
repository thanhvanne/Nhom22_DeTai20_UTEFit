# UTEFit — Báo cáo khảo sát và kiểm thử ngày 25/10/2026

## Kết luận hiện tại

**Chưa đủ cơ sở xác nhận core đã xanh để chuyển sang giai đoạn 7.** Đã đăng nhập và khảo sát trực tiếp Member → PT → Admin bằng Playwright MCP trên Microsoft Edge. Đã mở đủ 13 module Admin; đã thực hiện tìm kiếm, lọc, phân trang và các ca gửi form được nêu trong bảng. Có 3 lỗi HTTP 500 ở luồng quản trị lớp/lịch, một lỗi hiển thị đối tượng review và dashboard PT chưa hiển thị thông tin công việc.

Đây là **báo cáo khảo sát có giới hạn**, không phải xác nhận đã kiểm thử thành công mọi đường đi. Các luồng ghi dữ liệu hợp lệ, thanh toán, hủy, khóa, điểm danh, chat hai chiều và realtime còn BLOCKED/NOT TESTED. Không lấy việc có source code, mở được form, hay dữ liệu seed tồn tại làm bằng chứng PASS cho thao tác lưu thành công.

- Bảng chi tiết: [UTEFit_KiemThu_2026-10-25.csv](UTEFit_KiemThu_2026-10-25.csv).
- Kiểm kê 132 HTTP mapping từ source: [UTEFit_KiemKe_Routes_2026-10-25.csv](UTEFit_KiemKe_Routes_2026-10-25.csv). Đây là danh sách route, không phải 132 chức năng đã PASS; không bao gồm đầy đủ subscription/sự kiện STOMP và scheduler.
- Bằng chứng browser: thư mục `.playwright-mcp/` tại root project. Thời gian trong tên file là UTC; giờ hiển thị server là ICT (UTC+7).
- Bảng CSV có đủ các cột yêu cầu: Role, Module, Chức năng/thao tác, Kết quả thực tế, Trạng thái, Nhận xét logic, Hướng khắc phục; thêm cột Bằng chứng.

| Trạng thái | Số mục khảo sát |
|---|---:|
| PASS — chỉ ca cụ thể ghi trong dòng | 110 |
| FAIL — đã tái hiện trực tiếp | 5 |
| BLOCKED — chưa đủ điều kiện/phạm vi an toàn | 69 |
| NOT TESTED — chưa thực thi ca riêng | 9 |
| Tổng mục theo dõi | 193 |

Một mục có thể là kiểm tra hiển thị, một ca từ chối dữ liệu, hoặc một bước đối chiếu. **Không diễn giải 110 PASS thành 110 chức năng hoàn chỉnh đã kiểm thử đầu-cuối.** Ví dụ “form tạo lớp từ chối dữ liệu rỗng PASS” không có nghĩa “tạo lớp hợp lệ PASS”.

Không kiểm thử Guest thêm trong lượt khảo sát sau yêu cầu dừng Guest. Các lỗi Guest phát hiện trước đó được nhắc riêng ở cuối và không cộng vào bảng 193 mục này.

## Môi trường và an toàn

- Website: `http://localhost:8080`; database cấu hình hiện tại là SQL Server, database `UTEFitDB`. Tài liệu có phần hướng dẫn MySQL; không áp giả định MySQL vào môi trường đang chạy.
- Chỉ sử dụng ba tài khoản test được người dùng cung cấp. Báo cáo không chứa mật khẩu/hash.
- Không sửa source, cấu hình, schema, không triển khai giai đoạn 7; không commit/push. Không chạy build/test tự động có khả năng khởi tạo DB.
- Không chủ động lưu bản ghi nghiệp vụ hợp lệ mới; không hủy/khóa/xác nhận CASH/điểm danh dữ liệu seed. Các POST validation đã chạy được source chặn trước bước lưu. Sau hai ca sửa Class/Schedule bị lỗi, đọc lại thấy capacity vẫn 18 và giờ vẫn 06:00–07:00.
- Login/logout là thao tác có cập nhật trạng thái xác thực theo ứng dụng. Mở chat dùng luồng `getOrCreateConversation` của ứng dụng; lần này giao diện mở hội thoại đã có lịch sử. Không khẳng định toàn DB bất biến vì không có snapshot SQL trước/sau, và scheduler/app có thể hoạt động độc lập.
- Click gửi đăng ký gói Trial cho Member có gói ACTIVE **bị automatic approval review từ chối trước thực thi**, vì có nguy cơ tạo membership/payment. Không thử vòng qua cơ chế chặn. Câu hỏi xác nhận phạm vi các bản ghi `QA_UTEFIT_20261025` đã được gửi và chưa có câu trả lời tại thời điểm lập báo cáo.
- Các mục BLOCKED khác được phân biệt rõ trong CSV: thiếu trạng thái phù hợp, nguy cơ tác động dữ liệu đang dùng, hoặc chưa có fixture QA riêng. Không nói rằng bộ duyệt đã chặn tất cả thao tác.

## Kết quả theo role

| Role | Module | Chức năng/thao tác | Kết quả thực tế | Trạng thái | Nhận xét logic | Hướng khắc phục |
|---|---|---|---|---|---|---|
| Member | Auth | Login/logout | Điều hướng đúng role và về login khi logout | PASS | Ca xác thực thực tế đã chạy | Không yêu cầu sửa cho ca này |
| Member | Dashboard | Xem tổng quan | Gói ACTIVE, lịch sắp tới, thanh toán PAID | PASS | Khớp màn hình lịch sử đã mở | Cần đối chiếu số ngày với quy ước thời hạn |
| Member | Profile | Xem; lưu; avatar; đổi mật khẩu | Xem đúng tài khoản; chưa gửi lưu/upload; không đổi mật khẩu | PASS / BLOCKED | Xem form không chứng minh lưu thành công | Dùng tài khoản/bản ghi QA để test ghi; giữ yêu cầu không đổi mật khẩu |
| Member | Membership | Xem; đăng ký; CASH; hủy | Có một ACTIVE; gửi đăng ký bị auto-review chặn; không có PENDING | PASS / BLOCKED | Không tự tạo trạng thái giả để test CASH/hủy | Cần Member chưa có gói và đơn QA riêng |
| Member | Payment | Xem lịch sử | CASH #1 PAID 399000 VND | PASS | Khớp Admin Payment #1 | Chưa xác nhận chuyển PENDING → PAID |
| Member | Class | Xem, tìm, lọc | Đúng chi nhánh; lịch đã qua và thiếu gói có thông báo | PASS | Chỉ chứng minh hiển thị điều kiện | Còn phải test POST đặt/hủy |
| Member | Class | Đặt/hủy | Hai lịch hợp lệ tương lai đều đã đặt sẵn | BLOCKED | Không hủy đăng ký seed để tạo chỗ test | Tạo lịch QA, đặt/hủy chính đăng ký QA |
| Member | Schedule | Xem/chuyển tuần | Hiển thị đúng lịch 26/10 và 28/10 | PASS | Dữ liệu thống nhất với Admin | Link Chi tiết cần xem lại, nêu bên dưới |
| Member | PT Request | Xem/form gửi; gửi/hủy | #1 PENDING; form đúng PT; không gửi/hủy seed | PASS / BLOCKED | Chưa có quan hệ ACCEPTED với PT01 | Cần request QA và cho phép xử lý |
| Member | PT 1:1 | Xem/lọc ngày | Trạng thái rỗng hợp lý | PASS | Không có lịch để test nội dung buổi tập | Hoàn tất luồng request QA trước |
| Member | Chat | Hộp thư/gửi/nhận | Hộp thư rỗng, chưa có hội thoại được chấp nhận | PASS / BLOCKED | Không được lấy hội thoại của Member khác thay thế | Cần quan hệ QA ACCEPTED |
| Member | Notification | Xem/đánh dấu đọc | Một thông báo đã đọc; chưa đọc 0 | PASS / BLOCKED | Chưa có dữ liệu chuyển unread → read | Tạo thông báo từ luồng QA được phép |
| Member | History/Check-in | Xem lịch sử | Đủ gói, tiền, lớp, review, check-in | PASS | Lượt 22/10 khớp Admin | Chưa xác nhận ghi lịch sử mới |
| Member | Review | Xem/form sửa | Review PT cũ mở sửa lại hiển thị đối tượng chi nhánh | FAIL | F4 | Giữ target hiện tại trong model edit |
| Member | Review | Lưu mới/sửa/upload | Chưa gửi ghi dữ liệu | BLOCKED | Không coi mở form là lưu thành công | Fixture review QA và media test |
| Member | Security | Trái role/ownership review | Admin/PT 403; review #2 edit 404 | PASS | Ca chặn trực tiếp đã chạy | Còn phải test POST ownership riêng |
| PT | Auth | Login/logout | Điều hướng đúng và logout được | PASS | Đã thực thi | Không yêu cầu sửa cho ca này |
| PT | Dashboard | Tổng hợp công việc | Trang hướng dẫn, thiếu pending/upcoming/unread | FAIL | F5 | Bind đúng dữ liệu DTO hiện có |
| PT | Profile | Xem/form sửa | Đúng PT01; branch do Admin quản lý | PASS | Không tự đổi branch từ PT | Lưu/upload chưa test |
| PT | Request | Xem; accept/reject | Thấy Member01 PENDING, Member11 ACCEPTED; chưa xử lý | PASS / BLOCKED | Không đổi trạng thái seed đang dùng | Request QA được phép accept/reject |
| PT | Class | Lịch, chi tiết, Member, ownership | Lịch #1 có 6 Member; lịch #2 trả 403 | PASS | Có Member01 trong đúng lớp | POST điểm danh chưa test |
| PT | Class | Điểm danh có/vắng | Chưa ghi vào đăng ký seed | BLOCKED | Có thể tác động điều kiện review/lịch sử | Buổi QA kết thúc, registration QA |
| PT | Lịch 1:1 | Xem/form tạo; lưu/hủy/hoàn thành | Có lịch seed; mở đúng Member26; chưa ghi | PASS / BLOCKED | Không thay lịch Member11 ngoài tài khoản được cấp | Dùng lịch QA, check-in phù hợp |
| PT | Chat | Lịch sử; gửi/realtime | Thấy 3 tin cũ với Member11; chưa gửi | PASS / BLOCKED | Lịch sử không chứng minh realtime hoạt động | Test hội thoại QA giữa hai phiên |
| PT | Notification | Xem; read/read-all | Thấy thông báo yêu cầu mới; chưa đổi trạng thái | PASS / BLOCKED | Dữ liệu phù hợp request hiện có | Fixture thông báo QA |
| PT | Security | Trái role | Admin/Member đều 403 | PASS | Đã kiểm tra trực tiếp | Ownership chat/POST còn chưa test |
| Admin | Dashboard | KPI | 65 tài khoản, 12 PT, 10 branch, 20 lớp; doanh thu 34275000 | PASS | PASS hiển thị/đối chiếu UI, chưa phải kiểm toán SQL | Đối soát SQL độc lập sau khi có phạm vi đọc phù hợp |
| Admin | User | Danh sách/tìm/lọc/trang/chi tiết | Member01 #16 đúng role/status; không thấy hash | PASS | Các thao tác đọc đều đã thực thi | Khóa/mở khóa BLOCKED |
| Admin | Branch | Đọc/tìm/lọc/trang/form | Hoạt động | PASS | Form tạo/sửa dữ liệu lỗi có phản hồi field | Lưu hợp lệ, media, toggle còn BLOCKED |
| Admin | Trainer | Đọc/tìm/lọc/trang | Hoạt động, đúng branch PT01 | PASS | Có UI assign và toggle | Assign/toggle BLOCKED vì lịch/quan hệ sẵn có |
| Admin | Plan | Đọc/tìm/lọc/trang/form | Hoạt động; sửa giá 0 bị từ chối rõ | PASS | Snapshot giá/thời hạn có trong service | Lưu hợp lệ/toggle còn BLOCKED |
| Admin | Category | Đọc/tìm/lọc/trang/form | Hoạt động; tên rỗng bị từ chối | PASS | Chưa kiểm thử ảnh hưởng khi deactivate | Lưu hợp lệ/toggle còn BLOCKED |
| Admin | Class | Đọc/tìm/lọc/trang/form tạo rỗng | Hoạt động, thiếu field có lỗi rõ | PASS | Chưa test tạo hợp lệ | Fixture lớp QA |
| Admin | Class | Sửa capacity 18 → 1 với 5 đăng ký | HTTP 500; đọc lại vẫn 18 | FAIL | F1b | Xử lý business exception tại form |
| Admin | Class | Hủy | Chưa thực thi | BLOCKED | Ảnh hưởng Member/PT/lịch sử | Chỉ hủy lớp QA được phép |
| Admin | Schedule | Đọc/lọc/trang/form | Hoạt động | PASS | Chưa kiểm tra lưu hợp lệ/đóng/mở ca | Fixture lịch QA |
| Admin | Schedule | Tạo trùng PT | HTTP 500 | FAIL | F1a | Xử lý exception conflict thành lỗi nghiệp vụ |
| Admin | Schedule | Sửa giờ lịch đã có Member | HTTP 500; giờ cũ giữ nguyên | FAIL | F1c | Trả lý do từ chối, giữ form |
| Admin | Payment | Đọc/tìm/lọc/trang/chi tiết/CASH queue | Hoạt động; #1 khớp Member | PASS | Queue #21–25 chưa được xác nhận | Confirm CASH BLOCKED, không giả thu tiền |
| Admin | Promotion | Danh sách/form sửa/validation client | Hoạt động; giảm 0 bị browser chặn | PASS | Không chứng minh POST save hoặc áp mã mua gói | Lưu/toggle BLOCKED; chốt phạm vi hiện tại |
| Admin | Check-in | Đọc/tìm/lọc/trang/validation client | Lượt Member01 khớp; form trống bị chặn | PASS | Chưa chứng minh check-in thành công | Ghi/checkout BLOCKED |
| Admin | Review | Đọc/lọc/trang | Có cả visible và hidden, lọc đúng | PASS | Không đổi moderation seed | Ẩn/hiện BLOCKED |
| Admin | Statistics | KPI/3 biểu đồ | Chart.js có instance và dataset thật | PASS | Tổng top Plan/top Branch cùng 34275000 | Tổng hợp SQL độc lập NOT TESTED |
| Admin | Auth | Logout | Về /login | PASS | Đã thực thi sau khảo sát | Không yêu cầu sửa cho ca này |
| Liên role | Dữ liệu hiện có | Gói/tiền/check-in/request/lịch | Khớp các màn hình đã xem | PASS | Chỉ consistency của dữ liệu seed | Không thay thế E2E mới |
| Liên role | E2E mới | Tạo lịch → đặt → PT thấy → hủy/noti | Chưa đủ fixture/phạm vi ghi | BLOCKED | Không báo PASS bằng số liệu giả | Cấp bộ QA riêng, chạy mỗi bước một ca |

Các dòng gộp trong bảng tóm tắt được tách theo từng thao tác/trạng thái trong CSV. CSV cũng ghi các thao tác chưa chạy như phân trang Member/PT, lọc chat, reload/reconnect và ownership chat; không bỏ chúng khỏi danh sách.

## Lỗi tái hiện trực tiếp

### F1a — P1: tạo lịch trùng PT trả HTTP 500

1. **Hiện tại:** Admin tạo lịch cho Morning Yoga/PT Minh Tran ngày 26/10/2026, 06:15–06:45, phòng QA_UTEFIT_20261025. PT đã có Yoga Relaxation #8 lúc 06:00–07:00. Submit đưa đến Whitelabel HTTP 500.
2. **Sai sót:** luồng từ chối conflict không trả thông báo nghiệp vụ. Không kết luận hệ thống đã cho lưu trùng; bằng chứng chỉ chứng minh xử lý lỗi thất bại.
3. **Vận hành thực tế:** nhân viên cần biết PT đang bận buổi nào và sửa giờ ngay; lỗi 500 khiến họ không biết giao dịch có được lưu hay không.
4. **Đúng nên:** giữ form/giá trị, thông báo PT bận với khoảng thời gian cụ thể, không ghi lịch.
5. **Nguyên nhân/khắc phục:** `ScheduleConflictService.assertTrainerFree` ném IllegalStateException; `AdminScheduleController.createSchedule` chỉ bắt IllegalArgumentException. Chuẩn hóa exception nghiệp vụ và render lại form; không bỏ kiểm tra conflict.
6. **Ảnh hưởng:** Admin trực tiếp; PT và Member gián tiếp nếu việc điều phối bị đình trệ.

Bằng chứng: [.playwright-mcp/page-2026-10-25T07-19-46-266Z.yml](../.playwright-mcp/page-2026-10-25T07-19-46-266Z.yml). Source: `AdminScheduleServiceImpl.java:369`, `AdminScheduleController.java:84`.

### F1b — P1: giảm capacity dưới số người đã đặt trả HTTP 500

1. **Hiện tại:** Yoga Relaxation #8 có 5 đăng ký, capacity 18; submit capacity 1 trả 500. Đọc lại capacity vẫn 18.
2. **Sai sót:** backend chặn theo hướng đúng nhưng lỗi không được chuyển thành phản hồi sử dụng được.
3. **Vận hành thực tế:** không được giảm chỗ dưới số người đã xác nhận; nhân viên cần lý do và số chỗ tối thiểu.
4. **Đúng nên:** từ chối rõ “đã có 5 người”, giữ capacity cũ và form, không mất thông tin.
5. **Nguyên nhân/khắc phục:** `AdminClassServiceImpl.updateClass` ném IllegalStateException; controller chỉ bắt IllegalArgumentException. Sửa xử lý exception của chức năng sửa lớp.
6. **Ảnh hưởng:** Admin; bảo toàn chỗ của Member và danh sách lớp của PT.

Bằng chứng: [lỗi](../.playwright-mcp/page-2026-10-25T07-19-58-392Z.yml), [đọc lại](../.playwright-mcp/page-2026-10-25T07-20-35-897Z.yml). Source: `AdminClassServiceImpl.java:132`, `AdminClassController.java:119`.

### F1c — P1: sửa giờ lịch đã có đăng ký trả HTTP 500

1. **Hiện tại:** lịch #8 có Member, đổi giờ bắt đầu 06:00 → 06:15 rồi lưu trả 500; đọc lại vẫn 06:00–07:00.
2. **Sai sót:** quy tắc “hủy/điều phối trước khi đổi lịch” tồn tại nhưng không được trình bày cho Admin.
3. **Vận hành thực tế:** không âm thầm đổi thời gian mà hội viên đã đặt; cần quy trình hủy/đặt lại hoặc điều phối có thông báo.
4. **Đúng nên:** từ chối tại form, giải thích có người đăng ký và bước xử lý hiện có.
5. **Nguyên nhân/khắc phục:** `updateSchedule` ném IllegalStateException ở nhánh occupied > 0; controller chỉ bắt IllegalArgumentException. Giữ nguyên guard, bổ sung phản hồi nghiệp vụ.
6. **Ảnh hưởng:** Admin, Member đã đăng ký và PT được phân công.

Bằng chứng: [lỗi](../.playwright-mcp/page-2026-10-25T07-20-26-883Z.yml), [lịch giữ nguyên](../.playwright-mcp/page-2026-10-25T07-20-29-746Z.yml). Source: `AdminScheduleServiceImpl.java:126`, `AdminScheduleController.java:126`.

### F4 — P2: form sửa review PT hiển thị nhầm đối tượng

1. **Hiện tại:** review #1 ở danh sách là đánh giá PT Minh Tran. Khi sửa, branch đúng Thu Duc nhưng combobox trainerId bị disabled, không có option PT Minh Tran, hiển thị “Đánh giá chi nhánh (không chọn PT)”.
2. **Sai sót:** đối tượng trên form không khớp review gốc. Chưa submit sửa nên không kết luận DB bị đổi target.
3. **Vận hành thực tế:** người dùng phải biết họ đang sửa đánh giá PT hay cơ sở.
4. **Đúng nên:** hiển thị target gốc bất biến, kể cả hiện tại target không còn trong danh sách đủ điều kiện tạo mới.
5. **Nguyên nhân/khắc phục:** controller edit dùng `eligibleTargets` như tạo mới, template chỉ lặp `targets.trainers`; hidden trainerId và service giữ target cũ nhưng combobox không có option tương ứng. Bổ sung target hiện tại vào model edit hoặc hiển thị target bằng text readonly.
6. **Ảnh hưởng:** Member trực tiếp; Admin moderation và dữ liệu review PT cần giữ nhất quán.

Bằng chứng: [.playwright-mcp/page-2026-10-25T07-06-24-944Z.yml](../.playwright-mcp/page-2026-10-25T07-06-24-944Z.yml) và browser_evaluate DOM của trainerId. Source: `MemberReviewController.java:99`, `templates/member/review-form.html`, `MemberReviewService.editForm/update`.

### F5 — P2: dashboard PT chưa hiển thị thông tin công việc

1. **Hiện tại:** dashboard PT01 chỉ là giới thiệu/hướng dẫn và nhãn PT chung; không có yêu cầu chờ, lịch sắp tới, tin/notification chưa đọc. Trang request riêng có dữ liệu PENDING.
2. **Thiếu sót:** chưa thực hiện vai trò tổng hợp công việc theo tài liệu giai đoạn 5–6.
3. **Vận hành thực tế:** PT cần thấy công việc ưu tiên ngay sau đăng nhập.
4. **Đúng nên:** dùng dữ liệu sẵn có để hiển thị pending/upcoming/unread và link đến các màn hình xử lý.
5. **Nguyên nhân/khắc phục:** controller tải `TrainerDashboardSummaryResponse`, service có pendingRequests/upcomingSchedules/unread counts; template chủ yếu là nội dung tĩnh và tham chiếu một số thông tin hồ sơ. Chỉnh template/model mapping của dashboard hiện tại, không cần một module nghiệp vụ mới.
6. **Ảnh hưởng:** PT trực tiếp, thời gian phản hồi yêu cầu Member gián tiếp.

Bằng chứng: [.playwright-mcp/page-2026-10-25T07-07-55-044Z.yml](../.playwright-mcp/page-2026-10-25T07-07-55-044Z.yml). Source: `PtDashboardController.java:19`, `PtDashboardServiceImpl.getDashboardForTrainer`, `templates/pt/dashboard.html`.

## Rủi ro logic phát hiện từ source — chưa tái hiện bằng ghi dữ liệu

Các mục sau là **phát hiện review source/định hướng kiểm thử**, không nằm trong 5 FAIL runtime. Không được dùng chúng để khẳng định đã tái hiện lỗi trên website.

| ID / ưu tiên | Logic hiện tại và thiếu sót | Vì sao cần xem lại / hành vi đúng | Chỉnh chức năng hiện tại và role ảnh hưởng |
|---|---|---|---|
| S1 / P1 | AdminSchedule validate start < end, branch/class active và conflict; chưa thấy chặn ngày/giờ đã qua hay so giờ mở/đóng cửa. DTO chỉ @NotNull cho ngày. | Lịch đang mở đăng ký nên nằm trong tương lai và giờ hoạt động. Nếu cho nhập lịch sử thì phải phân biệt trạng thái lịch sử, không mở booking như lịch mới. | Bổ sung guard trong validateScheduleBusinessRules cho create/edit/reactivate; dùng cùng quy ước giờ gym như PT 1:1. Admin/PT/Member chịu ảnh hưởng. |
| S2 / P1 | updateClass đổi PT/branch khi maxRegistered = 0 nhưng không kiểm lại toàn bộ lịch hiện có với PT/branch mới. Guard đăng ký không thay thế guard conflict. | Lớp chưa có Member vẫn có thể đã chiếm lịch PT/phòng. Đổi PT có thể tạo trùng lịch; đổi branch có thể làm sai giờ hoạt động/phòng. | Khi thay phân công/branch, kiểm tra toàn bộ active schedule trong cùng transaction và khóa phù hợp. Admin/PT và các booking sau đó bị ảnh hưởng. |
| S3 / P1 | createClass cho phép trainerId null; DTO cho phép tạo OPEN/FULL/CANCELLED/COMPLETED trực tiếp. Schedule cũng có nhánh trainer null; booking không đòi PT. | Có thể cần lớp chưa phân công trong nội bộ, nhưng không nên mở buổi cho Member đặt nếu chưa có người phụ trách. FULL thường gắn sức chứa của từng buổi, không nên vô hiệu hóa mọi buổi chỉ từ nhãn lớp. | Ràng buộc trạng thái OPEN/lịch active phải có PT hợp lệ; chốt rõ status lớp so với tình trạng chỗ từng schedule. Không cần thêm nghiệp vụ mới để sửa guard hiện tại. Admin/PT/Member bị ảnh hưởng. |
| S4 / P1 | Hủy lớp đóng tất cả active schedules và bulk đổi mọi registration REGISTERED thành CANCELLED, không lọc lịch quá khứ. cancelClass chỉ chặn CANCELLED, chưa chặn COMPLETED. UI còn nút Hủy lớp trên một dòng COMPLETED. | Hủy hoạt động tương lai không được viết lại lịch sử đã diễn ra. Đăng ký quá khứ chưa điểm danh có thể bị biến thành “đã hủy” sai ý nghĩa. | Định nghĩa chuyển trạng thái hợp lệ; chỉ hủy các buổi cần hủy, giữ lịch sử/attendance; kiểm tra notification đúng tập Member. Admin/PT/Member/review/history bị ảnh hưởng. |
| S5 / P1 | Category toggle/update active không kiểm tra lịch/đăng ký liên quan; Member booking có kiểm category active. | Tắt category có thể khiến lớp đã công bố còn hiển thị nhưng không đặt được, lịch đã đặt chưa được điều phối. | Guard deactivate khi còn lịch/booking hoặc đồng bộ đóng và thông báo bằng luồng hủy hiện có. Admin/Member/PT bị ảnh hưởng. |
| S6 / P1 | Khóa user thu hồi token và disabled, nhưng không xử lý lịch tương lai của PT. Booking kiểm schedule/branch/category/class nhưng chưa kiểm trạng thái tài khoản PT. Schedule kiểm trainer.active, chưa thấy đầy đủ user ACTIVE/enabled/role trong guard này. | PT bị khóa không thể đăng nhập trong khi lịch vẫn mở; Member có thể đặt buổi không ai đủ quyền vận hành. | Dùng chung điều kiện “PT vận hành được”; khi khóa PT có lịch thì chặn/điều phối/hủy có kiểm soát qua chức năng hiện tại. Admin/PT/Member bị ảnh hưởng. |
| S7 / P2 | Membership create chặn nếu Member có bất kỳ ACTIVE còn hạn, không xét branch trong điều kiện tồn tại. Quyền sử dụng lại theo branch. | Đây có thể là chính sách cố ý “mỗi Member một gói”, nhưng khi quản lý chuỗi sẽ không cho mua gói chi nhánh thứ hai/gia hạn trước. Không tự kết luận là bug nếu scope đã chốt như vậy. | Chốt chính sách và giải thích ngay tại form; nếu cần đa branch/gia hạn thì chỉnh validation và thời hạn trong chức năng mua gói hiện tại. Member/Admin payment bị ảnh hưởng. |
| S8 / P2 | Promotion hiện là CRUD quản trị; form mua gói/DTO/service thanh toán chưa có áp mã. UI có mã expired/future/quantity=0 vẫn gắn ACTIVE. | ACTIVE có thể chỉ là công tắc, không đồng nghĩa đang dùng được. Không được nói mã đã giảm tiền cho Member khi chưa có luồng áp mã. | Làm rõ nhãn bật/tắt so với hiệu lực ngày/số lượng; chốt scope quản lý mã trước khi đánh giá nhu cầu áp mã trong mua gói. Không coi redemption là đã triển khai. Admin/Member/payment bị ảnh hưởng. |
| S9 / P2 | Link Chi tiết ở một số card lịch Member/PT trỏ lại chính trang lịch/danh sách thay vì chi tiết buổi. | Click không đưa tới thêm thông tin nên khó hiểu vai trò nút. | Trỏ đúng màn hình chi tiết hiện có hoặc đổi nhãn/bỏ nút thừa; chưa cần thêm chức năng. Member/PT bị ảnh hưởng. |

Các race condition như hai booking tranh suất cuối, sửa capacity đồng thời booking, đổi PT đồng thời tạo lịch, và khóa tài khoản khi đang gửi message **NOT TESTED**. Source có khóa ở một số luồng nhưng chưa đủ bằng chứng xác nhận an toàn đồng thời cho toàn hệ thống.

## Những hướng đúng đã thấy trong source

Đây là nhận xét kiến trúc/nghiệp vụ, không cộng PASS runtime:

- Booking khóa Member rồi Schedule, kiểm tra gói đúng branch và còn hạn tới ngày học, chống trùng đăng ký, sức chứa và overlap lịch nhóm/1:1.
- Payment chỉ Admin confirm CASH; membership/payment có lock, check ownership/status và lưu snapshot giá/thời hạn. Không có bằng chứng frontend tự set PAID.
- Hủy đơn PENDING giữ lịch sử và đóng payment PENDING; các trạng thái đã thanh toán không được dùng chung luồng hủy đơn chưa thanh toán.
- PT 1:1 kiểm accepted request, membership và giờ mở/đóng cửa; complete yêu cầu đã kết thúc và check-in/out phù hợp.
- Review kiểm bằng chứng check-in hoàn tất/ATTENDED/PT 1:1 hoàn thành; sửa giữ target và không tự hiện lại review đã bị Admin ẩn.
- User lock tăng tokenVersion; tự khóa Admin và dùng unlock bỏ qua OTP đã có guard.
- Branch deactivate có guard membership còn hiệu lực/lịch nhóm tương lai; Trainer chuyển/tắt có guard lịch tương lai.
- Statistics query doanh thu PAID, không lấy PENDING vào doanh thu đã thu. Ba chart thực tế đã đọc: top Plan tổng 34275000 và top Branch tổng 34275000 VND. Chưa chứng minh mọi dữ liệu seed đều hợp lệ theo mọi quy tắc hiện tại.

## Liên kết giữa role và phần chưa hoàn tất

Đã đối chiếu các bản ghi hiện có:

1. Payment #1 ở Member và Admin cùng CASH PAID 399000 VND; Basic 1 Month ACTIVE tại Thu Duc.
2. Check-in #1 ở Member history và Admin cùng 22/10/2026 08:00–10:00 Thu Duc.
3. Request #1 Member01 → PT01 cùng PENDING trên cả hai phía.
4. Schedule #8 giữa Admin và Member khớp ngày/giờ/branch/capacity; PT được gán là Minh Tran, khớp hồ sơ PT01. **Chưa mở chính lịch #8 từ PT để xác nhận bước đó.**
5. Member01 nằm trong danh sách sáu người của Morning Yoga lịch #1 từ màn hình PT.

Chưa chạy đầu-cuối với bản ghi mới: mua gói → CASH → kích hoạt → Admin tạo lịch → Member đặt → PT xem/điểm danh → chat/noti → check-in → review. Những phần này được ghi BLOCKED, không thay bằng nhận xét source.

Để gỡ BLOCKED cần xác nhận phạm vi bộ bản ghi QA, một Member chưa có gói/đơn QA PENDING để kiểm payment, và quyền dùng một cặp Member–PT cho request/chat/lịch mới. Ba tài khoản hiện tại đã được giữ trong ngữ cảnh; **không cần cung cấp lại mật khẩu**. Không yêu cầu hay thử giải BCrypt.

## Lỗi Guest đã phát hiện trước yêu cầu dừng — không chạy lại

- Từ khóa 101 ký tự trên Branch/Plan/Class gây HTTP 500; 100 ký tự Branch trả empty state bình thường.
- Bộ lọc review rating 0 hoặc 6 gây HTTP 500.
- `/images/default-trainer.png` trả 404.
- Trang lớp còn nội dung “Đăng ký lịch lớp sẽ được triển khai ở giai đoạn 4”.
- Trang 404 dùng nội dung/đường quay lại PT cho cả lỗi branch/review.
- Favicon trả 403 quan sát ở nhiều role; P2 giao diện/tài nguyên, không coi là lỗi đăng nhập.

## Thứ tự khắc phục đề xuất

1. **P1:** sửa xử lý business exception ở Class/Schedule để ba ca đã tái hiện trả thông báo nghiệp vụ; giữ nguyên rollback/guard.
2. **P1:** rà chuyển trạng thái lớp/lịch, dữ liệu quá khứ, đổi PT/branch, giờ vận hành và tác động khóa/tắt đối tượng liên quan.
3. **P2:** sửa target form review, bind dashboard PT, nhãn trạng thái/promotion và liên kết chi tiết.
4. Sau khi được phép dùng fixture QA: chạy các thao tác ghi còn BLOCKED theo đúng bảng, mỗi thao tác một ca đại diện, rồi kiểm E2E liên role.
5. Chỉ kết luận đủ điều kiện sang giai đoạn 7 khi core ghi dữ liệu và liên role có bằng chứng thành công; hiện chưa đạt điều kiện xác nhận này.

Không có lỗi P0 được chứng minh trong lượt khảo sát này. Điều đó **không tương đương đã loại trừ mọi P0**, vì payment confirmation, POST ownership, đồng thời và realtime vẫn còn phần chưa kiểm thử.

