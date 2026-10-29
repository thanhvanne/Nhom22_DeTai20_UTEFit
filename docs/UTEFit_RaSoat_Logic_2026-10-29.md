# UTEFit — Rà soát logic và hướng hoàn thiện

Ngày rà soát: **29/10/2026**. Mã nguồn tại commit `42f6393` và trạng thái thư mục làm việc trong phiên rà soát.

## 1. Cách đọc kết quả

Báo cáo phân biệt **25 vấn đề có căn cứ từ mã nguồn/kiểm thử**, **7 rủi ro cần kiểm thử bổ sung**, và các đề xuất hoàn thiện. Các vấn đề xác định từ mã nguồn chưa mặc nhiên là các lỗi đã tái hiện trên giao diện. Kết quả chạy kiểm thử được ghi riêng ở cuối báo cáo.

- **P1 — cao:** ảnh hưởng quyền truy cập, thanh toán, lịch tập hoặc tính đúng đắn của lịch sử. Ưu tiên xử lý trước demo chính thức.
- **P2 — vừa:** chức năng thiếu nhất quán, thiếu hoàn thiện hoặc dễ gây lỗi cho người dùng.
- **Rủi ro đồng thời:** có đường thực thi thiếu đồng bộ; cần chạy ca song song trên SQL Server để kết luận tần suất và kết quả thực tế.

Phạm vi: cấu hình phân quyền HTTP/WebSocket; đăng ký, OTP, đổi mật khẩu; gói tập, CASH, SePay, callback VNPay; đặt lớp và lịch PT; quản lý chi nhánh/danh mục/PT; điểm danh, check-in/out và QR; chat, thông báo, đánh giá, upload; truy vấn và ràng buộc SQL. Đã kiểm kê 260 file Java chính, 81 template và 31 file kiểm thử Java. Không sử dụng bản gộp `UTEFit_Full_Source.txt` làm nguồn kết luận khi có file nguồn thực tế.

**Giới hạn:** không thay đổi dữ liệu SQL Server đang sử dụng, không gửi tiền hay webhook thật, không gửi OTP hoặc upload tài nguyên thật để tái hiện. Rà soát này không chứng minh rằng mọi lỗi tiềm ẩn đều đã được tìm thấy.

## 2. Các vấn đề ưu tiên cao

### L01 — Tác vụ dọn đơn có thể đóng gói đang chờ duyệt hủy [P1]

**Vị trí:** [PendingMembershipCleanupScheduler.java:27](E:/Nhom22_DeTai20_UTEFit/src/main/java/com/utefit/service/PendingMembershipCleanupScheduler.java:27), `MembershipPaymentService.approvePendingCancellation()`.

Tác vụ chọn Membership PENDING quá 48 giờ và chỉ giữ lại đơn có Payment PENDING hoặc PAID. Sau khi hội viên yêu cầu hủy SePay/VNPay, Payment là CANCEL_REQUESTED nên bị bỏ qua. Membership chuyển CANCELLED trong khi Payment vẫn CANCEL_REQUESTED. Hàm duyệt hủy yêu cầu Membership PENDING nên Admin không thể hoàn tất bằng luồng bình thường.

**Ca kiểm tra:** tạo đơn hơn 48 giờ, yêu cầu hủy thanh toán ngân hàng, chạy tác vụ dọn đơn rồi duyệt hủy. **Hướng sửa:** chỉ tự đóng đơn thực sự không có giao dịch cần xử lý; giữ CANCEL_REQUESTED và các trường hợp có bằng chứng tiền/đối soát. Khóa và đọc mới dữ liệu trong cùng transaction. Thêm kiểm thử tác vụ dọn đơn kết hợp trạng thái hủy.

### L02 — WebSocket đang mở không được thu hồi quyền theo JWT/tài khoản [P1]

**Vị trí:** [StompSecurityConfig.java:21](E:/Nhom22_DeTai20_UTEFit/src/main/java/com/utefit/config/StompSecurityConfig.java:21), [ChatWebSocketController.java:26](E:/Nhom22_DeTai20_UTEFit/src/main/java/com/utefit/websocket/ChatWebSocketController.java:26), [ChatServiceImpl.java:100](E:/Nhom22_DeTai20_UTEFit/src/main/java/com/utefit/service/impl/ChatServiceImpl.java:100).

JWT được kiểm tra lúc kết nối HTTP. Với các frame STOMP sau đó, interceptor chỉ yêu cầu Principal khác null và destination hợp lệ. Controller tìm lại User nhưng không kiểm tra trạng thái, tokenVersion hay hạn token. `sendMessage()` kiểm tra PT, nhưng không kiểm tra MEMBER còn ACTIVE/enabled. Vì vậy MEMBER đã mở socket có thể tiếp tục gửi sau khi bị khóa; logout, đổi mật khẩu hoặc hết hạn JWT cũng không tự đóng socket. Subscription cũ còn có thể nhận dữ liệu tiếp.

**Ca kiểm tra:** mở socket bằng MEMBER, khóa tài khoản từ Admin rồi gửi `/app/chat.send` trên socket cũ; lặp lại với logout/đổi mật khẩu/hết hạn. **Hướng sửa:** liên kết socket với phiên xác thực và phiên bản token; kiểm tra lại khi SEND/SUBSCRIBE, chặn người gửi không còn hợp lệ và đóng các kết nối khi thu hồi quyền. Cần kiểm tra cả khả năng nhận dữ liệu sau thu hồi, không chỉ gửi.

### L03 — PT xem được tên/email MEMBER chưa có quan hệ huấn luyện [P1]

**Vị trí:** [PtPersonalScheduleController.java:77](E:/Nhom22_DeTai20_UTEFit/src/main/java/com/utefit/controller/pt/PtPersonalScheduleController.java:77), [PtPersonalScheduleViewService.java:24](E:/Nhom22_DeTai20_UTEFit/src/main/java/com/utefit/service/PtPersonalScheduleViewService.java:24), template `pt/personal-schedules/create.html:136`.

GET `/pt/personal-schedules/create?memberId=...` nhận ID tùy ý. Hàm `member(id)` chỉ kiểm tra role MEMBER; không kiểm tra yêu cầu ACCEPTED với PT đang đăng nhập. Form hiển thị tên và email của tài khoản tìm được. POST tạo lịch có kiểm tra quan hệ nhưng không bảo vệ việc đọc thông tin ở GET.

**Ca kiểm tra:** PT A thay memberId bằng ID của hội viên chưa từng gửi yêu cầu cho A. **Hướng sửa:** truyền danh tính PT vào hàm lấy học viên, truy vấn theo cặp PT–MEMBER có quan hệ hợp lệ; trả 403/404 và dùng DTO tối thiểu.

### L04 — Đổi PT/chi nhánh của lớp có thể tạo lịch trùng mà không bị chặn [P1]

**Vị trí:** [AdminClassServiceImpl.java:132](E:/Nhom22_DeTai20_UTEFit/src/main/java/com/utefit/service/impl/AdminClassServiceImpl.java:132).

`updateClass()` cho đổi trainer/branch khi `maxRegistered == 0`, nhưng không kiểm tra lại toàn bộ ClassSchedule của lớp theo PT và chi nhánh mới. Việc đổi quan hệ trên FitnessClass lập tức tác động mọi buổi học. Kiểm tra trùng ở `AdminScheduleServiceImpl` không chạy khi cập nhật theo đường này.

**Ca kiểm tra:** lớp A có buổi 18:00–19:00 chưa ai đăng ký; PT B đã dạy một lớp khác cùng giờ; đổi PT của A sang B. Tương tự, chuyển A đến chi nhánh có phòng cùng tên đã bị chiếm. **Hướng sửa:** mọi thay đổi ảnh hưởng tài nguyên phải đi qua cùng bộ kiểm tra lịch, khóa các tài nguyên liên quan và kiểm tra tất cả buổi tương lai trước khi lưu.

### L05 — Sửa lớp/lịch cũ có thể làm thay đổi lịch sử và bằng chứng đánh giá [P1]

**Vị trí:** [AdminClassServiceImpl.java:154](E:/Nhom22_DeTai20_UTEFit/src/main/java/com/utefit/service/impl/AdminClassServiceImpl.java:154), [AdminScheduleServiceImpl.java:126](E:/Nhom22_DeTai20_UTEFit/src/main/java/com/utefit/service/impl/AdminScheduleServiceImpl.java:126), `ReviewRepository.attendedClasses()` và `MemberReviewService.evidence()`.

Chốt bảo vệ chỉ đếm REGISTERED. Khi người học đã ATTENDED/ABSENT hoặc lịch không còn active, có thể đổi ngày/giờ/lớp hoặc thông tin PT/chi nhánh/danh mục. Lịch sử và bằng chứng review lại đọc quan hệ hiện tại, không có bản chụp tại thời điểm học. Một buổi đã tập với PT A ở chi nhánh X có thể bị diễn giải thành PT B ở Y. Hủy lớp cũng vô hiệu tất cả lịch active, kể cả buổi quá khứ còn chưa điểm danh.

**Ca kiểm tra:** điểm danh ATTENDED, đổi PT/chi nhánh lớp rồi xem lịch sử và danh sách PT đủ điều kiện review. **Hướng sửa:** bất biến hóa dữ liệu buổi đã diễn ra; gắn PT/chi nhánh thực tế và bản chụp cần thiết vào buổi học. Chỉ sửa/hủy buổi tương lai; sửa lịch sử qua quy trình riêng có nhật ký.

### L06 — Có thể tắt chi nhánh còn gói đã mua cho tương lai hoặc lịch PT [P1]

**Vị trí:** [AdminBranchServiceImpl.java:144](E:/Nhom22_DeTai20_UTEFit/src/main/java/com/utefit/service/impl/AdminBranchServiceImpl.java:144), [MembershipRepository.java:30](E:/Nhom22_DeTai20_UTEFit/src/main/java/com/utefit/repository/MembershipRepository.java:30).

`hasActiveMembersForBranch()` chỉ xét gói bắt đầu trước/hôm nay và hết hạn sau/hôm nay. Gói đã PAID nhưng bắt đầu trong tương lai không chặn việc đóng chi nhánh. `checkCanDeactivate()` kiểm tra lịch nhóm, không kiểm tra PersonalTrainingSchedule hoặc lượt check-in chưa checkout. Với gói cũ đa chi nhánh, truy vấn chỉ xét `m.branch.id` nên không phản ánh đầy đủ phạm vi đã bán.

**Ca kiểm tra:** chi nhánh chỉ có kỳ đã thanh toán bắt đầu tuần sau và lịch PT tuần sau, không có lịch nhóm; tắt chi nhánh. **Hướng sửa:** kiểm tra mọi quyền đã cam kết còn hạn và mọi buổi nhóm/PT tương lai, đồng thời có quy trình xử lý người đang tập. Chọn rõ chặn đóng hay cho đóng sau khi chuyển lịch/hoàn tiền có xác nhận.

## 3. Các vấn đề chức năng và tính nhất quán

### L07 — Lịch nhóm không tuân thủ cùng giới hạn thời gian với PT [P2]

**Vị trí:** [AdminScheduleServiceImpl.java:309](E:/Nhom22_DeTai20_UTEFit/src/main/java/com/utefit/service/impl/AdminScheduleServiceImpl.java:309), `ClassScheduleFormDto`, `PersonalTrainingScheduleServiceImpl.createScheduleForMember()`.

Lịch PT chặn ngày/giờ đã qua và ngoài giờ mở cửa. Lịch nhóm chỉ yêu cầu startTime < endTime, không kiểm tra hai điều kiện đó. Có thể tạo buổi active trong quá khứ hoặc 03:00 tại chi nhánh mở 06:00. Chỉnh giờ mở/đóng chi nhánh cũng không rà lại các lịch đã có.

**Hướng sửa:** dùng một chính sách thời gian chung cho buổi nhóm/PT, kiểm tra ở service; cho phép nhập lịch sử qua chức năng riêng nếu cần. **Kiểm thử:** trước mở cửa, sau đóng cửa, đúng ranh giới, đã bắt đầu, và đổi giờ chi nhánh khi còn lịch tương lai.

### L08 — Tắt danh mục/đổi quyền lợi danh mục không xử lý nhất quán các lịch đã đặt [P2]

**Vị trí:** [AdminCategoryServiceImpl.java:68](E:/Nhom22_DeTai20_UTEFit/src/main/java/com/utefit/service/impl/AdminCategoryServiceImpl.java:68), `MembershipBookingPolicyService.revalidateMember()`, `MemberClassBookingService.validateOpenSchedule()`.

Tắt danh mục làm lịch biến mất khỏi danh sách có thể đặt và chặn đặt mới, nhưng đăng ký cũ vẫn REGISTERED vì tác vụ rà quyền không xét `category.active`. Đổi requiredBenefit lại có thể khiến tác vụ hủy hàng loạt các đặt chỗ cũ theo yêu cầu mới. Hai cách sửa danh mục có hệ quả khác nhau mà không có bước đánh giá tác động.

**Hướng sửa:** quy định tắt danh mục chỉ ngừng bán hay hủy dịch vụ; chặn/chốt quyền lợi đã cam kết hoặc xem trước danh sách đặt chỗ bị ảnh hưởng và thông báo. **Kiểm thử:** danh mục có người đã đặt, tắt/bật và đổi GYM sang PT.

### L09 — Lỗi nghiệp vụ hợp lệ có thể thành trang 500 [P2]

**Vị trí:** [MemberClassBookingController.java:84](E:/Nhom22_DeTai20_UTEFit/src/main/java/com/utefit/controller/member/MemberClassBookingController.java:84), `AdminScheduleController.createSchedule()/updateSchedule()`, `ScheduleConflictService`.

Các controller trên chỉ bắt IllegalArgumentException, trong khi bộ kiểm tra trùng lịch ném IllegalStateException. Ví dụ MEMBER có lịch PT rồi đặt lớp nhóm trùng giờ: kiểm tra trùng lớp nhóm ban đầu không bắt được, kiểm tra chéo cuối hàm ném IllegalStateException và thoát ra 500. Tương tự khi Admin xếp lịch trùng PT hoặc sửa lịch đã có người đăng ký. Không thấy bộ xử lý chung chuyển các lỗi này thành lỗi nghiệp vụ phù hợp.

**Hướng sửa:** exception nghiệp vụ thống nhất và ControllerAdvice/handler phù hợp cho HTML và JSON. **Kiểm thử:** yêu cầu không hợp lệ trả thông báo dễ hiểu/400 hoặc 409, không phải 500.

### L10 — Điểm danh xong thì học viên biến mất khỏi danh sách PT [P2]

**Vị trí:** [PtClassServiceImpl.java:70](E:/Nhom22_DeTai20_UTEFit/src/main/java/com/utefit/service/impl/PtClassServiceImpl.java:70), `PtAttendanceService.record()`, template `pt/classes/detail.html`.

Danh sách và số người đăng ký chỉ lấy REGISTERED. Điểm danh chuyển thành ATTENDED/ABSENT nên tải lại trang sẽ mất hàng vừa điểm danh và giảm số đếm. Sau khi xử lý hết, giao diện có thể báo chưa ai đăng ký; PT không có bảng kết quả để kiểm tra và không có luồng sửa điểm danh sai.

**Hướng sửa:** hiển thị danh sách tham gia gồm REGISTERED/ATTENDED/ABSENT, kèm trạng thái và số liệu tách biệt; cho sửa kết quả trong phạm vi quy định với nhật ký. **Kiểm thử:** điểm danh toàn bộ rồi tải lại, kết quả vẫn xem được và tổng số người không mất.

### L11 — Lịch PT đã hủy biến mất khỏi lịch của chính PT [P2]

**Vị trí:** [PersonalTrainingScheduleServiceImpl.java:203](E:/Nhom22_DeTai20_UTEFit/src/main/java/com/utefit/service/impl/PersonalTrainingScheduleServiceImpl.java:203) và `getSchedulesForTrainer():249`.

Hủy buổi đặt active=false. Truy vấn lịch PT của trainer chỉ lấy ActiveTrue, trong khi phía MEMBER lấy cả lịch đã hủy. Bản ghi còn trong DB nhưng PT không xem lại được trong lịch tuần; đây là thiếu lịch sử hiển thị, không phải xóa dữ liệu.

**Hướng sửa:** lịch sử lấy mọi trạng thái trong khoảng ngày và hiển thị CANCELLED; chỉ các truy vấn tìm thời gian bận mới lọc active. **Kiểm thử:** hủy rồi mở lại đúng tuần ở cả hai vai trò.

### L12 — Mở 50 tin gần nhất đánh dấu đã đọc cả tin chưa được tải [P2]

**Vị trí:** [ChatServiceImpl.java:77](E:/Nhom22_DeTai20_UTEFit/src/main/java/com/utefit/service/impl/ChatServiceImpl.java:77) và `MessageRepository.findUnreadMessagesForUserInConversation()`.

Hàm chỉ trả tối đa 50 tin nhưng truy vấn và đánh dấu readAt cho toàn bộ tin chưa đọc của hội thoại. Với 100 tin chưa đọc, 50 tin cũ chưa hiển thị cũng thành đã đọc.

**Hướng sửa:** chỉ đánh dấu các ID đã thực sự được hiển thị, hoặc dùng mốc đọc cuối do client xác nhận. **Kiểm thử:** 100 tin chưa đọc, tải 50, 50 tin còn lại vẫn chưa đọc.

### L13 — Không có cách tải phần lịch sử chat cũ hơn 50 tin [P2]

**Vị trí:** `ChatServiceImpl.getRecentMessages()`, [MessageRepository.java:17](E:/Nhom22_DeTai20_UTEFit/src/main/java/com/utefit/repository/MessageRepository.java:17), `static/js/member-chat.js`, `static/js/chat/conversation.js`.

Service dùng Top50 và giới hạn limit <= 50; các API/giao diện chat đã kiểm tra không có cursor hay nút tải thêm. Tin cũ vẫn ở DB nhưng người dùng không truy cập qua giao diện bình thường.

**Hướng sửa:** phân trang theo cặp sentAt/id hoặc ID ổn định, bổ sung tải tin cũ và giữ vị trí cuộn. **Kiểm thử:** hội thoại 120 tin tải đủ, không trùng/thiếu khi có tin mới đến.

### L14 — Trang thông báo MEMBER chỉ thấy 20 tin nhưng đếm chưa đọc trên toàn bộ [P2]

**Vị trí:** [MemberNotificationService.java:16](E:/Nhom22_DeTai20_UTEFit/src/main/java/com/utefit/service/MemberNotificationService.java:16), [NotificationServiceImpl.java:122](E:/Nhom22_DeTai20_UTEFit/src/main/java/com/utefit/service/impl/NotificationServiceImpl.java:122).

Trang `/member/notifications` gọi API recent, không có phân trang. Khi tin cũ chưa đọc nằm ngoài Top20, bộ đếm vẫn tăng nhưng người dùng không xem từng tin trên trang này. Route dùng chung `/notifications` lấy được toàn bộ, nên dữ liệu chưa mất; vấn đề là chức năng MEMBER không có đường tải tiếp rõ ràng.

**Hướng sửa:** tách API recent cho thanh thông báo và API phân trang cho trang danh sách; có lọc chưa đọc. **Kiểm thử:** 30 tin, tin thứ 21 chưa đọc, vẫn tìm và mở được.

### L15 — Thay avatar MEMBER không dọn ảnh cũ và không dọn ảnh mới khi rollback [P2]

**Vị trí:** [MemberProfileService.java:131](E:/Nhom22_DeTai20_UTEFit/src/main/java/com/utefit/service/MemberProfileService.java:131).

Upload ảnh rồi ghi đè avatarUrl/avatarPublicId, không xóa tài nguyên cũ sau commit và không xóa ảnh vừa upload nếu lưu DB thất bại. PT và review đã có callback dọn tài nguyên nên cách xử lý không đồng nhất.

**Hướng sửa:** áp dụng cùng quy trình dọn sau transaction như `PtProfileService`; ghi log/đưa vào hàng đợi nếu xóa thất bại. **Kiểm thử:** thay ảnh nhiều lần và giả lập lỗi commit, chỉ còn ảnh đúng đang được tham chiếu.

### L16 — Tạo chi nhánh có ảnh cũng có thể để lại tài nguyên mồ côi [P2]

**Vị trí:** [AdminBranchServiceImpl.java:71](E:/Nhom22_DeTai20_UTEFit/src/main/java/com/utefit/service/impl/AdminBranchServiceImpl.java:71).

`createBranch()` upload trước khi lưu DB nhưng không đăng ký dọn ảnh khi rollback. `updateBranch()` đã có callback này. **Hướng sửa:** dùng chung helper quản lý vòng đời tài nguyên. **Kiểm thử:** upload thành công nhưng insert/commit chi nhánh thất bại, ảnh mới được dọn.

### L17 — Khuyến mãi mới dừng ở quản trị mã, chưa áp dụng vào giao dịch [P2 — thiếu chức năng]

**Vị trí:** `AdminPromotionService`, `PromotionRepository`, `MembershipPurchaseRequest`, `MembershipPaymentService.createMembership()`.

Có CRUD mã, thời gian, phần trăm, số lượng; không thấy đường nhập/áp mã ở mua gói, không tính giảm quotedPrice, không trừ số lượng hoặc lưu lượt dùng. Tìm tham chiếu promotion/discount trong Java và luồng MEMBER chỉ thấy phần quản trị/entity/repository.

**Hướng sửa:** nếu đưa vào chức năng đã hoàn thành trong báo cáo, cần bổ sung luồng áp mã, snapshot giá gốc/giảm/thành tiền, lượt dùng và khóa số lượng. Nếu chưa làm, ghi rõ hiện chỉ quản lý danh mục mã. **Kiểm thử:** hết hạn, hết số lượng, áp lặp và hai người dùng suất cuối.

### L18 — Quy ước họ/tên bị đảo giữa các màn hình [P2]

**Vị trí:** `dto/auth/RegisterRequest.java`, `templates/auth/register.html`, [User.java:97](E:/Nhom22_DeTai20_UTEFit/src/main/java/com/utefit/entity/User.java:97), [TrainerRequestServiceImpl.java:97](E:/Nhom22_DeTai20_UTEFit/src/main/java/com/utefit/service/impl/TrainerRequestServiceImpl.java:97), `templates/member/profile.html:60`.

Đăng ký gán firstName cho nhãn “Họ”, lastName cho “Tên”; `getFullName()` ghép firstName + lastName. Trang profile và yêu cầu PT lại ghép lastName + firstName; form profile cũng dùng quy ước ngược với form đăng ký. Nhập “Nguyễn Văn”/“An” có thể hiện thành “An Nguyễn Văn”.

**Hướng sửa:** chốt một quy ước dữ liệu, sửa mọi form/mapper và dùng một hàm fullName; kiểm tra dữ liệu seed/cũ trước khi chuyển đổi, không hoán đổi toàn bộ một cách mù quáng. **Kiểm thử:** đăng ký → profile → chat → yêu cầu PT → review đều cùng tên.

### L19 — Admin tạo được giá lẻ mà SePay từ chối [P2]

**Vị trí:** [MembershipPlanFormDto.java:35](E:/Nhom22_DeTai20_UTEFit/src/main/java/com/utefit/dto/admin/MembershipPlanFormDto.java:35), `AdminMembershipPlanServiceImpl.validatePlanBusinessRules()`, `SepayPaymentService.start()`.

DTO cho phép giá >=0.01 và service chỉ kiểm tra >0. SePay yêu cầu số VND nguyên. Gói giá 100000.50 có thể tạo/mua/đi CASH nhưng không mở được SePay. Ngoài ra thiếu giới hạn chính xác theo numeric(15,2), nên giá quá lớn/nhiều số lẻ có thể lỗi hoặc bị làm tròn ở DB.

**Hướng sửa:** nếu hệ thống chỉ dùng VND nguyên, áp cùng validation trên DTO/service và DB; kiểm tra giới hạn số chữ số. **Kiểm thử:** 0, âm, 0.01, số lẻ, số nguyên và vượt giới hạn.

### L20 — Lỗi gửi OTP ở đăng ký/gửi lại chưa được xử lý như quên mật khẩu [P2]

**Vị trí:** `AuthController.register()`, `AuthController.resend()`, `AuthController.forgot()`, `MailServiceImpl.sendOtp()`.

`forgot()` bắt MailException và trả thông báo, nhưng register/resend chỉ bắt IllegalArgumentException. SMTP hỏng ở hai thao tác này có thể thành 500 thay vì form có thông báo thử lại. Gửi mail đang nằm trong transaction nên cần phân biệt gửi thất bại với mail đã gửi nhưng transaction DB không commit.

**Hướng sửa:** xử lý lỗi SMTP nhất quán, giữ dữ liệu form và thông báo an toàn; cân nhắc outbox khi cần bảo đảm giao nhận. **Kiểm thử:** SMTP không kết nối, timeout và DB rollback sau khi mail được gửi.

### L21 — Xác thực chuyên môn PT bằng chuỗi tự do dễ chặn nhầm hoặc bỏ lọt [P2 — thiết kế]

**Vị trí:** `AdminClassServiceImpl.resolveAndValidateTrainer()`, `PtProfileService.updateMyProfile()`.

Chuyên môn được so khớp bằng contains giữa tên loại lớp và một chuỗi do PT tự sửa; chuỗi có “general”/“đa năng” bỏ qua đối chiếu. Đổi tên danh mục hoặc dùng từ đồng nghĩa có thể làm PT đang phù hợp bị từ chối. PT có thể tự khai General để vượt kiểm tra dù không có quan hệ chứng nhận chuyên môn do Admin quản lý.

**Hướng sửa:** chuẩn hóa quan hệ PT–chuyên môn/danh mục; tách tiểu sử tự khai khỏi chuyên môn được duyệt. **Kiểm thử:** đổi tên danh mục không đổi quyền phân công; sửa bio/chuyên môn tự khai không tự cấp quyền mới.

### L22 — PT chưa được gán chi nhánh vẫn có thể xuất hiện công khai và nhận yêu cầu [P2]

**Vị trí:** `AdminTrainerServiceImpl.toggleActive()`, `TrainerRepository.findPublicTrainerById()/searchPublicTrainers()`, `TrainerRequestServiceImpl.createRequest()`.

Admin có thể bật trainer active khi branch=null; truy vấn public chấp nhận b IS NULL; gửi yêu cầu không đòi branch hoạt động. Tuy nhiên tạo lịch PT lại bắt buộc có branch active. MEMBER có thể trải qua gửi/được chấp nhận rồi không đặt được buổi nào.

**Hướng sửa:** tách hồ sơ công khai và trạng thái sẵn sàng nhận học viên, hoặc chỉ cho nhận yêu cầu khi đã phân chi nhánh; hiển thị rõ trạng thái. **Kiểm thử:** PT active chưa có branch không nhận yêu cầu mới nếu chưa thể phục vụ.

### L23 — Trang lịch sử MEMBER cắt dữ liệu nhưng không báo cho người xem [P2]

**Vị trí:** [MemberHistoryService.java:33](E:/Nhom22_DeTai20_UTEFit/src/main/java/com/utefit/service/MemberHistoryService.java:33).

Review chỉ lấy trang 0 với 10 bản ghi; lịch PT chỉ lấy từ ba năm trước đến một năm sau. Membership/payment/check-in/lớp lại tải toàn bộ. Trang có thể bị hiểu là lịch sử đầy đủ trong khi mỗi loại dữ liệu có giới hạn khác nhau.

**Hướng sửa:** phân trang và lọc ngày nhất quán cho từng nhóm, hiển thị tổng số/phạm vi. **Kiểm thử:** hơn 10 review, PT cũ hơn 3 năm, và dữ liệu nhiều năm.

### L24 — Tham số phân trang quản trị chưa được chuẩn hóa [P2]

**Vị trí:** `AdminScheduleController.listSchedules()`, `AdminScheduleServiceImpl.getSchedules()`, các service quản trị dùng trực tiếp `PageRequest.of(page,size)`.

URL cho phép truyền page âm, size=0 hoặc size rất lớn. Một số service công khai đã chặn/cố định kích thước nhưng quản trị chưa nhất quán. Giá trị không hợp lệ có thể thành 500, còn size lớn tải nhiều dữ liệu và quan hệ không cần thiết.

**Hướng sửa:** validation tập trung page>=0 và size trong tập/giới hạn cho phép; trả 400 hoặc chuẩn hóa. **Kiểm thử:** page=-1, size=0, size cực lớn và phân trang bình thường.

### L25 — Tính năng tra cứu SePay đã tắt vẫn có thể làm ứng dụng không khởi động [P2 — khả năng chịu lỗi]

**Vị trí:** [SepayReconciliationLookup.java:25](E:/Nhom22_DeTai20_UTEFit/src/main/java/com/utefit/service/SepayReconciliationLookup.java:25).

Constructor luôn tạo Java HttpClient, trước khi xét `SEPAY_LIVE_API_LOOKUP_ENABLED=false`. Trong môi trường kiểm thử của phiên này, khởi tạo HttpClient thất bại với `Unable to establish loopback connection`, dẫn đến lỗi tạo bean và kéo theo nhiều integration test không khởi tạo được ứng dụng. Điều này không chứng minh máy triển khai bình thường sẽ lỗi, nhưng cho thấy tính năng tùy chọn đang nằm trên đường khởi động bắt buộc của toàn ứng dụng.

**Hướng sửa:** tạo client trì hoãn khi chức năng được bật/sử dụng, hoặc cấu hình bean có điều kiện và inject một client có thể giả lập trong test; khi chưa bật, trả trạng thái không khả dụng có kiểm soát. **Kiểm thử:** lookup=false và giả lập khởi tạo client lỗi, các chức năng không liên quan vẫn khởi động; lookup=true xử lý lỗi qua thông báo phù hợp.

## 4. Rủi ro cần kiểm thử thêm, không trình bày như lỗi đã tái hiện

| ID | Mức ưu tiên | Bằng chứng và tình huống cần kiểm tra | Hướng xử lý |
|---|---|---|---|
| R01 | P1 | `OtpServiceImpl.issueAndSend()/consume()` đọc OTP, số lần gửi/nhập rồi ghi, không lock hoặc @Version. Hai request có thể cùng dùng OTP hoặc ghi đè số lần sai. | Khóa theo user và loại OTP khi cấp; khóa OTP khi consume, cập nhật điều kiện nguyên tử. Test barrier hai lần consume/cấp cùng lúc. |
| R02 | P1 | `TrainerRequestServiceImpl` dùng exists rồi insert; entity/repository và schema chính không có khóa duy nhất cho yêu cầu đang hiệu lực. Accept/reject/cancel cũng đọc rồi ghi không lock/version. | Unique có điều kiện cho quan hệ hiệu lực phù hợp mô hình SQL Server; khóa cùng request khi chuyển trạng thái. Test gửi kép và accept song song cancel. |
| R03 | P1 | MEMBER đặt lớp khóa User rồi Schedule; Admin sửa/hủy lịch trực tiếp dùng findDetailById, đếm đăng ký rồi cập nhật mà không cùng khóa Schedule. Guard đổi thời gian/sức chứa có thể đọc số lượng cũ. | Thống nhất giao thức khóa giữa đặt, sửa, hủy và đổi sức chứa; refresh sau khi giữ khóa. Test đặt suất cuối cùng đồng thời giảm capacity/đổi/hủy lịch. |
| R04 | P1 | `AdminScheduleServiceImpl` khóa User của PT nhưng truy vấn phòng chỉ đọc. Hai PT khác nhau xếp cùng phòng/giờ không chia sẻ khóa. | Khóa tài nguyên phòng/ngày hoặc khóa lịch tập trung có phạm vi hợp lý; kiểm tra overlap trong vùng được bảo vệ. Test hai transaction cùng phòng, PT khác nhau. |
| R05 | P1 | Tạo lịch PT khóa MEMBER và User PT; `AdminTrainerServiceImpl.assignBranch()/toggleActive()` kiểm tra lịch rồi sửa trainer mà không theo giao thức này. `cancelSchedule()/completeSchedule()` cũng không lock/version bản ghi PT. | Khóa cùng tài nguyên khi chuyển chi nhánh/tắt PT và tạo lịch; chuyển trạng thái có điều kiện/optimistic lock. Test tạo đồng thời chuyển PT, hủy đồng thời thao tác khác. |
| R06 | P1 | `User` không có @Version. Profile, đổi mật khẩu, logout và Admin khóa tài khoản đều có thể tải rồi save toàn entity mà không chung lock. | Kiểm tra SQL update thực tế và test đồng thời để loại trừ lost update của status/enabled/password/tokenVersion; dùng @Version hoặc cập nhật trường riêng có điều kiện. |
| R07 | P2 | Nhiều cột tiếng Việt vẫn varchar trong schema và không Nationalized trong entity: users, trainers, notifications, trainer_requests, một số nội dung lớp/PT. Database mới không chốt collation UTF-8. | Kiểm tra collation thực tế và round-trip tên/nội dung có dấu trên SQL Server. Dùng nvarchar/Nationalized nhất quán hoặc thiết kế UTF-8 rõ ràng. Không kết luận đã mất dấu khi chưa kiểm tra DB. |

Lưu ý R03/R04/R05: có `@Transactional` không tự bảo đảm hai request cùng kiểm tra điều kiện sẽ loại trừ nhau. H2 không đủ để chứng minh mọi hành vi khóa, deadlock hoặc isolation trên SQL Server.

## 5. Các điểm nên hoàn thiện trước khi viết kết luận báo cáo

1. **Thống nhất state machine.** Viết bảng chuyển trạng thái Payment/Membership/Registration/PersonalTrainingSchedule/TrainerRequest; xác định actor, điều kiện và thông báo cho từng chuyển đổi. Tránh mỗi scheduler/service tự suy diễn trạng thái.
2. **Tách ngừng bán và ngừng cung cấp dịch vụ.** Tắt plan/danh mục/chi nhánh/PT phải có quy tắc rõ đối với đơn đã tạo, gói đã trả tiền và lịch đã giữ chỗ. Đặc biệt không để thay đổi danh mục vô tình đổi cam kết cũ.
3. **Bổ sung vòng đời quan hệ PT.** Hiện ACCEPTED được dùng như quyền vô thời hạn và chỉ có hủy khi PENDING. Cần quy định kết thúc/chuyển PT, ảnh hưởng lịch tương lai và quyền chat lịch sử; đây là quyết định nghiệp vụ cần chốt.
4. **Làm rõ dashboard nhiều chi nhánh.** `MemberDashboardService` lấy gói hiện tại đầu tiên, trong khi có thể có nhiều gói ở nhiều chi nhánh. Nên nhóm theo chi nhánh hoặc cho chọn thay vì dùng một số “ngày còn lại” đại diện toàn tài khoản.
5. **Làm rõ đơn SePay hết hạn.** `start()` trả lại giao dịch PENDING cũ kể cả khi đã hết hạn; việc tạo lại hiện phải đi qua hủy/đối soát. Nếu giữ chính sách này, giao diện cần chỉ dẫn đầy đủ, không tự gia hạn QR cũ hay tự đổi sang CASH khi có khả năng đã chuyển tiền.
6. **Giảm truy vấn theo từng hàng.** Danh sách chat thực hiện countUnread + lastMessage cho từng hội thoại; `MemberChatService.ownedConversation()` còn tải toàn danh sách ở mỗi lần poll. Danh sách lịch và thanh toán cũng có truy vấn bổ sung theo từng hàng. Dùng projection/tổng hợp theo batch, đo số query và giới hạn dữ liệu lịch sử.
7. **Hoàn thiện nhật ký.** Thêm người thực hiện, thời gian, trước/sau cho sửa lịch, điểm danh, chuyển PT, thay danh mục/quyền lợi. Lỗi dọn Cloudinary hoặc đẩy realtime đang bị nuốt ở một số nhánh cần log và cơ chế thử lại nếu có yêu cầu đảm bảo giao nhận.
8. **Thống nhất tầng và loại DTO.** Có nhiều service/dashboard/profile trùng mục đích, entity được truyền trực tiếp cho view và một số controller truy cập repository. Đối chiếu yêu cầu mô hình ba tầng của báo cáo; gom phần đang sử dụng, bỏ phần cũ sau khi xác minh tham chiếu. Không gọi mọi mã trùng là lỗi chạy ứng dụng.
9. **Giới hạn và validation nhất quán.** Giới hạn thử đăng nhập theo tài khoản/IP, giới hạn request khi gửi OTP; chuẩn hóa email, độ dài mật khẩu theo giới hạn BCrypt thực tế, phone, giá và trang dữ liệu. Rate limit là bổ sung bảo vệ, không được thay thế kiểm tra OTP năm lần ở server.
10. **Quản lý schema có phiên bản.** Nhiều bộ SQL đầy đủ, migration, live-demo và tài liệu bàn giao cùng tồn tại. Giữ một nguồn schema chuẩn, có kiểm tra drift và bộ hướng dẫn riêng cho dựng mới/nâng cấp; không dùng reset để cập nhật hệ thống có dữ liệu.

## 6. Thứ tự chỉnh sửa và tiêu chí nghiệm thu

| Đợt | Nội dung | Tiêu chí đạt |
|---|---|---|
| 1 | L01–L06, kiểm tra R01–R06 | Không đóng nhầm đơn chờ xử lý; quyền bị thu hồi chặn cả HTTP/socket; PT không đọc học viên ngoài phạm vi; đổi lớp không trùng tài nguyên hay sửa ngược lịch sử. |
| 2 | L07–L14, L18–L20, L22, L24 | Thời gian/danh mục/giá/tên thống nhất; lỗi nghiệp vụ có thông báo; xem lại đủ điểm danh, lịch hủy, chat và thông báo. |
| 3 | L15–L17, L21, L23, L25, R07 và các cải tiến | Không để tài nguyên mồ côi; ghi đúng phạm vi khuyến mãi; dữ liệu tiếng Việt và lịch sử được kiểm tra; có số liệu hiệu năng, nhật ký và cách cô lập dịch vụ tùy chọn. |

Ma trận kiểm thử tối thiểu cần bổ sung: vai trò × chủ sở hữu × trạng thái tài khoản; Membership đã trả/chưa trả/hết hạn/tương lai/hoàn tiền; lịch trước/đúng/sau ranh giới; thay đổi dữ liệu danh mục khi đã bán; callback trùng/đến muộn; thao tác đồng thời; lỗi SMTP/Cloudinary/DB; dữ liệu vượt kích thước một trang.

## 7. Kết quả kiểm chứng trong phiên rà soát

| Nhóm | Kết quả | Ý nghĩa |
|---|---|---|
| Bộ JavaScript hiện có | **44/44 đạt** | Chạy `node --test src/test/js/*.test.cjs`; bao phủ QR, scanner, upload review và màn hình SePay theo các test đã có. |
| Bộ Java hiện có, build trong thư mục đầu ra riêng | **278 test: 78 đạt, 199 lỗi thực thi/khởi tạo, 1 bỏ qua; 0 assertion failure** | Chưa thể kết luận toàn bộ luồng tích hợp đạt. Các lỗi khởi tạo liên quan Java HttpClient/loopback và bean tra cứu SePay; không tính 199 lỗi này thành 199 lỗi nghiệp vụ riêng. |
| Kiểm thử bổ sung cô lập các nhánh nghi vấn | **5/5 xác nhận hành vi hiện tại** | Dùng mock, không truy cập DB/dịch vụ thật. Xác nhận L01, phần service của L02, phần tra cứu của L03, L07 và L12. Đây là bằng chứng ở tầng service, không phải tái hiện trên trình duyệt. |

Mã kiểm chứng: [LogicAuditProbeTest.java](E:/Nhom22_DeTai20_UTEFit/docs/LogicAuditProbeTest.java). Các assertion trong file này mô tả **hành vi lỗi hiện tại**, nên file được lưu ở `docs`, không đưa vào bộ test chính để tránh vô tình bắt hệ thống giữ nguyên lỗi. Khi sửa phải viết lại assertion theo hành vi mong muốn.

Chi tiết năm phép kiểm chứng:

1. Membership có Payment CANCEL_REQUESTED vẫn chuyển CANCELLED khi dọn đơn; phương thức kiểm tra CANCEL_REQUESTED không được gọi.
2. `ChatServiceImpl.sendMessage()` vẫn lưu tin từ MEMBER LOCKED/enabled=false khi quan hệ PT còn hợp lệ. Kiểm chứng socket cũ thực tế vẫn cần test WebSocket bổ sung.
3. Hàm lấy member theo ID trả email mà không nhận danh tính PT hay ràng buộc quan hệ.
4. `createSchedule()` chấp nhận buổi 03:00–04:00 ngày 01/01/2000 tại chi nhánh mở 06:00–22:00.
5. `getRecentMessages()` đánh dấu readAt cho tin nằm ngoài tập kết quả trả về.

Quá trình chạy Java ban đầu gặp lỗi công cụ attach trên Windows. Phiên kiểm chứng dùng Java agent nạp trước và `forkCount=0`, build đầu ra riêng tại `target/logic-audit-build` để tránh `.class` cũ trong `target`. Sau đó bộ test vẫn gặp lỗi loopback nêu trên. Kết quả full suite và probes lưu lần lượt ở `target/review-test-verified.log` và `target/review-probes.log` (thư mục tạm, được Git bỏ qua).

Không sửa source nghiệp vụ, không thay đổi cấu hình DB, không thay đổi dependency. File POM và bản sao test tạm dùng để chạy đã được dọn sau kiểm chứng; chỉ báo cáo và mã kiểm chứng trong `docs` được thêm.

Không xem việc bộ test hiện tại chạy đạt là bằng chứng bác bỏ các nhánh thiếu kiểm thử ở trên. Ngược lại, lỗi khởi tạo do môi trường kiểm thử cũng không được tính thành từng lỗi nghiệp vụ của ứng dụng.

## 8. Đoạn gợi ý dùng trong báo cáo đồ án

> Qua rà soát mã nguồn, nhóm nhận thấy hệ thống đã có các cơ chế quan trọng như phân quyền theo vai trò, kiểm tra quyền sở hữu, snapshot gói tập, xác minh thanh toán, QR dùng một lần và lưu lịch sử giao dịch. Tuy nhiên, một số quy tắc còn chưa nhất quán giữa các luồng chức năng, đặc biệt ở tác vụ dọn đơn, thu hồi phiên WebSocket, thay đổi lịch và bảo toàn lịch sử. Hướng hoàn thiện ưu tiên là thống nhất các phép chuyển trạng thái, đồng bộ kiểm tra tài nguyên, bổ sung kiểm thử đồng thời trên SQL Server và cải thiện khả năng tra cứu lịch sử. Các chức năng chưa nối vào luồng nghiệp vụ hoàn chỉnh, như áp dụng mã khuyến mãi, được ghi nhận đúng phạm vi triển khai thay vì xem là đã hoàn tất.

Đoạn trên mô tả **kết quả rà soát và hướng hoàn thiện**. Chỉ đổi thành “đã khắc phục” khi có thay đổi thực tế và bằng chứng kiểm thử tương ứng.

