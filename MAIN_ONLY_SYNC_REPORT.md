# UTEFit — chuyển sang quy trình main-only

Ngày thực hiện: 30/10/2026. Toàn bộ source hiện nằm trong working tree của nhánh `main`. Chưa tạo commit trên nhánh, chưa push và chưa đổi danh tính/tài khoản xác thực.

## Kết quả Git

| Mục | Trước | Sau |
|---|---|---|
| Nhánh làm việc | `feat/giai-doan-7` | `main` |
| HEAD | `42f6393e32f5d7eb050807910a4b4e07176e52b9` | `57f19a7594a777c9437d70cc540f997db12272e9` |
| main local | `6833d3522ff6e5cbedc95117e9dc0aecebf97659` | Bằng `origin/main` tại `57f19a7` |
| Commit của feature chưa nằm trên main local | 16 | 0 |
| Commit của feature chưa nằm trên origin/main sau fetch | 0 | 0 |
| Code chưa commit ban đầu | 68 file tracked đã sửa, 69 file untracked | Đã khôi phục đầy đủ trên main; giữ unstaged để duyệt |
| Git conflict / file unmerged | Chưa tích hợp | 0 |
| Commit/push mới do tác vụ tạo trên main | Không | Không |

Đã fetch origin và phát hiện commit mới `57f19a7 — Fix logic and improve system features`, author `thisisuyen <tranuyentu160306@gmail.com>`. Commit này có cha là lịch sử feature đã được tích hợp từ trước. Vì main local là tổ tiên của origin/main, dùng `git switch main` rồi `git merge --ff-only origin/main`: main tiến thêm 17 commit, không tạo merge commit hoặc viết lại lịch sử. Đây là cập nhật từ remote bằng fetch + fast-forward, không dùng pull tạo merge tự động.

Kiểm tra cuối bằng `git ls-remote origin refs/heads/main` vẫn trả `57f19a7594a777c9437d70cc540f997db12272e9`. `main...origin/main` là `0 0`; không còn commit nào từ `feat/giai-doan-7` nằm ngoài main. Nhánh cũ được giữ nguyên làm mốc đối chiếu, không tiếp tục phát triển ở đó.

## Checkpoint và kiểm chứng bảo toàn

Checkpoint cục bộ được Git bỏ qua:

`.local/main-only-checkpoints/2026-10-29T18-12-12-326Z/`

- `files/` và `manifest.json`: sao chép 551 file source/tài liệu/assets cùng SHA-256, gồm 482 file tracked và 69 file untracked; khoảng 9,25 MB. Không có file source nào bị thiếu sau tích hợp.
- `tracked-and-untracked.patch`: patch binary của phần tracked so với HEAD ban đầu; các file untracked được lưu riêng trong `files/` và manifest. `index.patch` lưu trạng thái index ban đầu, vốn không có thay đổi staged.
- `history.bundle`: lịch sử/refs trước khi chuyển nhánh; `history-with-stash.bundle`: thêm origin/main mới và stash. Cả hai đã qua `git bundle verify`.
- Stash giữ nguyên, không drop/pop: `35330f27a3f2d5f6c68dcb9d195ef89eb20de25f`, mô tả `Local checkpoint before main-only integration 2026-10-30`. Stash là snapshot nội bộ trong `refs/stash`, không phải commit source trên main và không được push.
- Chỉ stash đúng 137 đường dẫn đã kiểm kê, không lấy ignored files. `.local/`, `target/`, `.ai/`, `.playwright-mcp/`, `.idea/`, thông tin môi trường thật và build artifacts không được thêm vào index/stash.
- Kiểm tra mẫu private key/token phổ biến trên các file thay đổi không phát hiện token/key theo các mẫu đã quét; đây không phải tuyên bố audit toàn bộ bí mật trong lịch sử Git.
- `integration-audit.json`: không thiếu file local, 68 file chỉ thuộc cập nhật remote giữ nguyên nội dung HEAD, không staged và không unmerged. Text được đối chiếu sau chuẩn hóa LF/CRLF do cấu hình Git Windows; các assets nhị phân được giữ nguyên.
- Bản cuối sẵn sàng review được lưu thêm trong `final/` cùng manifest và patch riêng.

Danh sách 16 commit đã có ở feature nhưng thiếu trên main local ban đầu:

```text
42f6393 merge: integrate PT updates with UTEFit frontend redesign
94c1292 feat: complete UTEFit features and database updates
16a421e feat: update membership payment cancellation and database migrations
be85fdf chore: ignore local secrets and generated artifacts
f80e7af style: redesign UTEFit frontend with unified design system
2dadc44 feat(review): add branch reviews and support 50MiB video uploads
a8798f7 feat(qr): checkpoint mobile scanning, member sync and QR checkout
5174b5b feat(sepay): checkpoint VietQR payments after user-verified Test Mode
cb4500f fix(gd7): clarify oversized uploads and finalize verified QA report
9f8ee95 fix(gd7): refresh locked VNPay state for concurrent IPN
8322b8e feat(gd7): add branch-scoped manager operations and safe role migration
376a174 feat(gd7): extend reviews with validated Cloudinary video lifecycle
fcd5392 feat(gd7): add member favorites with ownership and duplicate protection
3c2bf5b feat(gd7): add single-use QR check-in with transactional validation
9cf252d feat(gd7): integrate VNPay sandbox with verified IPN and isolated QA
2b1a6ce Update PT interface and layout
```

Tất cả hiện đều là tổ tiên của main. Không cherry-pick lại, squash hoặc đổi author. File `commits-main-to-feature.txt` trong checkpoint giữ SHA đầy đủ và author gốc.

## Ghép code và các chỗ cần kết hợp

Git tự ghép sạch 6 file trùng giữa stash và commit mới; không có marker conflict và không dùng lựa chọn ours/theirs hàng loạt. Đã rà theo từng nội dung:

| File/khu vực | Kết quả kết hợp |
|---|---|
| `static/js/member-chat.js` | Giữ màu chat Member Navy từ local; giữ cache lịch sử và nút/API tải tin nhắn cũ từ remote |
| `templates/fragments/header.html` | Giữ cơ chế Guest dùng Public Cinematic; giữ liên kết khuyến mãi mới của remote ở header hiện có |
| `templates/guest/home.html` | Giữ toàn bộ Cinematic/Journey/Finale và assets ảnh; chuyển liên kết khuyến mãi mới vào navigation Cinematic desktop/mobile, tránh chèn một khối trước hero |
| `templates/member/memberships.html` | Giữ Member Navy, luồng mua/gia hạn/thanh toán; giữ trường `promotionCode` và giá/ưu đãi snapshot mới. Đặt liên kết khuyến mãi trong navigation sẵn có, bổ sung ID/label riêng cho từng input theo branch/plan |
| `templates/member/notifications.html` | Giữ giao diện Navy và phân trang `notificationPage` mới từ remote |
| `templates/member/profile.html` | Giữ giao diện Navy và thứ tự/binding firstName/lastName mới từ remote; sửa `label for` tương ứng để khớp input thực tế |
| `templates/member/fragments/layout.html` | Bổ sung mục Khuyến mãi vào menu Khám phá hiện có để tính năng mới truy cập được qua Member Navy |

Admin Light, Member Navy, Public Cinematic, assets và thay đổi ảnh chi nhánh demo từ local đều được giữ. Không sửa backend/business logic mới của đồng đội để làm test xanh. Các file Java/database từ remote giữ nguyên so với HEAD mới.

Thêm `AGENTS.md` và cập nhật README để ghi nhớ main-only, quy tắc checkpoint, không tạo feature branch mặc định và không tự commit/push. Đây là thay đổi hướng dẫn đang chờ review cùng source.

## Compile và test

- Java 17.0.18, Maven 3.9.16: compile và test compile thành công; **286 tests, 0 failures, 0 errors; BUILD SUCCESS**.
- Chạy toàn bộ test suite dự án trừ `CloudinaryIntegrationTest` có upload thật; test Cloudinary dùng mock vẫn chạy. Tests Spring dùng H2 in-memory theo test resources, không chạy migration/seed trên SQL Server thật.
- Toàn bộ 8 file Node test: **85 tests, 85 pass, 0 fail**.
- `git diff --check`: đạt. Không file unmerged, không staged.
- Log trong checkpoint: `maven-test.log`, `node-test.log`.
- Wrapper `mvnw.cmd` gặp lỗi khởi chạy PowerShell của môi trường agent; validation sử dụng chính Maven 3.9.16 đã cache và cấu hình user home/cache cho tiến trình kiểm thử. Không sửa Maven wrapper hoặc cấu hình Java toàn máy.

Commit remote mới có migration `V715__membership_promotion_snapshot.sql`; đã giữ file và hướng dẫn README của đồng đội. Không tự áp dụng migration này, không restart/chạy ứng dụng trên database thật trong tác vụ đồng bộ Git. Compile/test H2 không thay cho kiểm tra triển khai SQL Server.

## Danh tính commit, tài khoản push và đóng góp

Danh tính Git hiện vẫn kế thừa từ `C:/Users/UTE/.gitconfig`:

```text
user.name  = nguyen  quang truong
user.email = quangtruongn57@gmail.com
```

Chưa có cấu hình `user.*` cấp repository. Chưa nhận được thông tin chính xác do Uyên cung cấp nên không tự đặt local identity, không suy đoán từ tác giả commit lịch sử. README có lệnh `git config --local user.name ...` và `git config --local user.email ...` với placeholder rõ ràng, để áp dụng sau khi Uyên xác nhận thông tin.

Author/committer là metadata commit; tài khoản push do credential/SSH quyết định. Fetch thành công không chứng minh người có tên trong metadata là tài khoản đang xác thực. Không đọc token, đổi tài khoản đăng nhập hoặc sửa credential helper.

Giữ nguyên tác giả các commit cũ, bao gồm `2b1a6ce` ghi `Uyen <tranuyentu160306@gmail.com>` và `57f19a7` ghi `thisisuyen <tranuyentu160306@gmail.com>`. Việc Uyên phụ trách commit/push đợt này không chuyển toàn bộ đóng góp giao diện/logic của người khác thành của Uyên. Các thay đổi chưa commit chưa có author metadata riêng; chỉ ghi co-author hoặc tên người đóng góp sau khi thông tin và đóng góp được xác nhận, không tự bịa attribution.

## Trạng thái chờ duyệt

Main đã đồng bộ và giữ đầy đủ source, toàn bộ thay đổi cuối vẫn unstaged. Checkpoint và stash còn nguyên. Cần Uyên cung cấp/xác nhận name/email nếu muốn đặt identity local trước commit; cần người dùng duyệt rõ ràng trước bất kỳ commit/push nào. Không tiếp tục phát triển trên feature branch.
