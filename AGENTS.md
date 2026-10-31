# Quy trình làm việc của UTEFit

- Nhóm sử dụng main-only: kiểm tra nhánh hiện tại và làm việc trực tiếp trên `main`.
- Không tạo hoặc tiếp tục phát triển trên nhánh tính năng, trừ khi người dùng yêu cầu rõ ràng.
- Trước khi đồng bộ Git, kiểm kê và sao lưu thay đổi tracked/untracked cần thiết; không đưa secret, cấu hình local hoặc build artifacts vào Git.
- Giữ toàn bộ commit của nhóm và kết hợp thay đổi theo nội dung; không giải quyết conflict hàng loạt bằng ours/theirs.
- Không chạy `reset --hard`, `clean -fd` hoặc force push để đồng bộ công việc.
- Chỉ commit/push sau khi người dùng duyệt rõ ràng. Không tự đặt danh tính commit hoặc đổi tài khoản xác thực.
- Cấu hình `user.name`/`user.email` chỉ ở cấp repository, theo thông tin chính xác người phụ trách cung cấp; giữ tác giả và đóng góp thực tế trong lịch sử.
