# UTEFit

Website quản lý chuỗi phòng tập thể thao – **Nhóm 22, Đề tài 20**.

**Công nghệ:** Java 17, Spring Boot 4.1.1, Thymeleaf, Spring Security, SQL Server, Maven.

## 1. Chuẩn bị

- **JDK 17** và **IntelliJ IDEA**.
- **SQL Server** đang chạy tại `localhost:1433` (hoặc sửa cổng trong `application.properties`).
- **Gmail** có bật xác minh 2 bước và đã tạo **App Password** để gửi OTP.

## 2. Tải dự án và tạo database

```bash
git clone https://github.com/thanhvanne/Nhom22_DeTai20_UTEFit.git
cd Nhom22_DeTai20_UTEFit
```

Mở thư mục dự án trong IntelliJ, chọn **Project SDK: JDK 17** và đợi Maven tải thư viện.

Mở **SQL Server Management Studio (SSMS)**, chạy lệnh sau **nếu chưa có database**:

```sql
CREATE DATABASE UTEFitDB;
```

Cấu hình mặc định trong `src/main/resources/application.properties` kết nối tới `localhost:1433`, database `UTEFitDB`. Spring JPA đang dùng `ddl-auto=update` để tạo/cập nhật bảng theo các Entity khi ứng dụng chạy.

## 3. Cấu hình Environment Variables

Trong IntelliJ vào **Run → Edit Configurations → chọn `Nhom22DeTai20UteFitApplication` → Environment variables** và khai báo:

| Biến | Giá trị |
| --- | --- |
| `DB_USERNAME` | Tên đăng nhập SQL Server |
| `DB_PASSWORD` | Mật khẩu SQL Server |
| `MAIL_USERNAME` | Gmail dùng để gửi OTP |
| `MAIL_PASSWORD` | **Gmail App Password** (không phải mật khẩu Gmail thường) |
| `JWT_SECRET_BASE64` | Khóa bí mật JWT dạng Base64 |

Để tạo `JWT_SECRET_BASE64`, mở **PowerShell** và chạy:

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
[Convert]::ToBase64String($bytes)
```

Sao chép chuỗi kết quả vào biến `JWT_SECRET_BASE64`. Các biến môi trường được `application.properties` đọc tự động; **không cần sửa trực tiếp username/password trong code**.

## 4. Chạy ứng dụng

Trong IntelliJ, chạy class:

```text
com.utefit.Nhom22DeTai20UteFitApplication
```

Hoặc dùng Maven Wrapper trên Windows sau khi đã thiết lập các biến môi trường:

```powershell
.\mvnw.cmd spring-boot:run
```

Truy cập: **http://localhost:8080/**
