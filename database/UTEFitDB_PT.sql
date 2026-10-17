/* =========================================
   UTEFit – Init Database Script (SQL Server)
   Thu UYen – Giai đoạn 2
   ========================================= */

IF DB_ID('UTEFitDB') IS NULL
BEGIN
    PRINT 'Creating database UTEFitDB...';
    CREATE DATABASE UTEFitDB;
END
GO

USE UTEFitDB;
GO

PRINT 'Using database: ' + DB_NAME();
GO


IF OBJECT_ID('dbo.trainers', 'U') IS NOT NULL
BEGIN
    DROP TABLE dbo.trainers;
END
GO

IF OBJECT_ID('dbo.branches', 'U') IS NOT NULL
BEGIN
    DROP TABLE dbo.branches;
END
GO

IF OBJECT_ID('dbo.users', 'U') IS NOT NULL
BEGIN
    DROP TABLE dbo.users;
END
GO


/* =========================================
   3. TẠO BẢNG USERS
   Mapping với entity User.java
   ========================================= */

CREATE TABLE dbo.users (
    id              BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    first_name      NVARCHAR(100)        NOT NULL,
    last_name       NVARCHAR(100)        NOT NULL,
    email           NVARCHAR(255)        NOT NULL,
    phone           NVARCHAR(20)         NULL,
    password        NVARCHAR(255)        NOT NULL,

    avatar_url      NVARCHAR(500)        NULL,
    avatar_public_id NVARCHAR(255)       NULL,

    role            NVARCHAR(20)         NOT NULL,    -- MEMBER / PT / ADMIN
    status          NVARCHAR(20)         NOT NULL,    -- UNVERIFIED / ACTIVE / LOCKED

    enabled         BIT                  NOT NULL CONSTRAINT DF_users_enabled DEFAULT (1),

    created_at      DATETIME2(0)         NOT NULL CONSTRAINT DF_users_created_at DEFAULT (SYSDATETIME()),
    updated_at      DATETIME2(0)         NOT NULL CONSTRAINT DF_users_updated_at DEFAULT (SYSDATETIME())
);
GO

-- Unique email
CREATE UNIQUE INDEX UX_users_email
    ON dbo.users(email);
GO


/* =========================================
   4. TẠO BẢNG BRANCHES
   Mapping với entity Branch.java
   ========================================= */

CREATE TABLE dbo.branches (
    id              BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    name            NVARCHAR(255)        NOT NULL,
    address         NVARCHAR(500)        NOT NULL,
    phone           NVARCHAR(20)         NULL,
    description     NVARCHAR(1000)       NULL,

    image_url       NVARCHAR(500)        NULL,
    image_public_id NVARCHAR(255)        NULL,

    opening_time    TIME(0)              NULL,
    closing_time    TIME(0)              NULL,

    active          BIT                  NOT NULL CONSTRAINT DF_branches_active DEFAULT (1),

    created_at      DATETIME2(0)         NOT NULL CONSTRAINT DF_branches_created_at DEFAULT (SYSDATETIME()),
    updated_at      DATETIME2(0)         NOT NULL CONSTRAINT DF_branches_updated_at DEFAULT (SYSDATETIME())
);
GO


/* =========================================
   5. TẠO BẢNG TRAINERS
   Mapping với entity Trainer.java
   ========================================= */

CREATE TABLE dbo.trainers (
    id               BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,

    user_id          BIGINT               NOT NULL,
    branch_id        BIGINT               NULL,

    specialization   NVARCHAR(255)        NULL,
    years_experience INT                  NULL,
    bio              NVARCHAR(2000)       NULL,

    active           BIT                  NOT NULL CONSTRAINT DF_trainers_active DEFAULT (1),

    created_at       DATETIME2(0)         NOT NULL CONSTRAINT DF_trainers_created_at DEFAULT (SYSDATETIME()),
    updated_at       DATETIME2(0)         NOT NULL CONSTRAINT DF_trainers_updated_at DEFAULT (SYSDATETIME())
);
GO

-- Mỗi user chỉ có 1 trainer profile
CREATE UNIQUE INDEX UX_trainers_user
    ON dbo.trainers(user_id);
GO

-- FK trainers → users
ALTER TABLE dbo.trainers
ADD CONSTRAINT FK_trainers_users
    FOREIGN KEY (user_id)
    REFERENCES dbo.users(id)
    ON DELETE CASCADE;
GO

-- FK trainers → branches
ALTER TABLE dbo.trainers
ADD CONSTRAINT FK_trainers_branches
    FOREIGN KEY (branch_id)
    REFERENCES dbo.branches(id);
GO


/* =========================================
   6. SEED DỮ LIỆU MẪU – BRANCHES
   3 chi nhánh đang hoạt động
   ========================================= */

SET IDENTITY_INSERT dbo.branches ON;
GO

INSERT INTO dbo.branches
    (id, name, address, phone, description,
     image_url, image_public_id,
     opening_time, closing_time,
     active, created_at, updated_at)
VALUES
    (1,
     N'UTEFit Thủ Đức',
     N'Khu phố 6, phường Linh Trung, TP. Thủ Đức',
     N'0901 000 001',
     N'Chi nhánh chính tại khuôn viên ĐH Sư phạm Kỹ thuật TP.HCM.',
     NULL, NULL,
     '06:00', '22:00',
     1, SYSDATETIME(), SYSDATETIME()),

    (2,
     N'UTEFit Quận 1',
     N'123 Lê Lợi, phường Bến Thành, Quận 1, TP. HCM',
     N'0901 000 002',
     N'Chi nhánh trung tâm, thuận tiện cho học viên làm việc tại khu vực trung tâm.',
     NULL, NULL,
     '06:00', '22:00',
     1, SYSDATETIME(), SYSDATETIME()),

    (3,
     N'UTEFit Bình Thạnh',
     N'45 Điện Biên Phủ, phường 15, Bình Thạnh, TP. HCM',
     N'0901 000 003',
     N'Chi nhánh gần các khu dân cư đông đúc, thuận tiện di chuyển.',
     NULL, NULL,
     '06:00', '22:00',
     1, SYSDATETIME(), SYSDATETIME());
GO

SET IDENTITY_INSERT dbo.branches OFF;
GO


/* =========================================
   7. SEED DỮ LIỆU MẪU – USERS
   9 PT ACTIVE + 1 PT LOCKED
   password tạm thời là '123456' (chưa mã hoá)
   ========================================= */

SET IDENTITY_INSERT dbo.users ON;
GO

INSERT INTO dbo.users
    (id, first_name, last_name, email, phone, password,
     avatar_url, avatar_public_id,
     role, status, enabled,
     created_at, updated_at)
VALUES
    -- 1. PT hợp lệ – Yoga
    (1, N'Trang', N'Nguyễn',
     N'trang.yoga@utefit.vn', N'0902 111 001', N'123456',
     NULL, NULL,
     N'PT', N'ACTIVE', 1,
     SYSDATETIME(), SYSDATETIME()),

    -- 2. PT hợp lệ – Gym cơ bản
    (2, N'Minh', N'Lê',
     N'minh.le@utefit.vn', N'0902 111 002', N'123456',
     NULL, NULL,
     N'PT', N'ACTIVE', 1,
     SYSDATETIME(), SYSDATETIME()),

    -- 3. PT hợp lệ – Powerlifting
    (3, N'Khoa', N'Phạm',
     N'khoa.pham@utefit.vn', N'0902 111 003', N'123456',
     NULL, NULL,
     N'PT', N'ACTIVE', 1,
     SYSDATETIME(), SYSDATETIME()),

    -- 4. PT hợp lệ – Boxing
    (4, N'Huy', N'Trần',
     N'huy.tran@utefit.vn', N'0902 111 004', N'123456',
     NULL, NULL,
     N'PT', N'ACTIVE', 1,
     SYSDATETIME(), SYSDATETIME()),

    -- 5. PT hợp lệ – CrossFit
    (5, N'Lan', N'Phan',
     N'lan.phan@utefit.vn', N'0902 111 005', N'123456',
     NULL, NULL,
     N'PT', N'ACTIVE', 1,
     SYSDATETIME(), SYSDATETIME()),

    -- 6. PT hợp lệ – Cardio
    (6, N'Tuấn', N'Hoàng',
     N'tuan.hoang@utefit.vn', N'0902 111 006', N'123456',
     NULL, NULL,
     N'PT', N'ACTIVE', 1,
     SYSDATETIME(), SYSDATETIME()),

    -- 7. PT hợp lệ – Giảm mỡ
    (7, N'Anh', N'Võ',
     N'anh.vo@utefit.vn', N'0902 111 007', N'123456',
     NULL, NULL,
     N'PT', N'ACTIVE', 1,
     SYSDATETIME(), SYSDATETIME()),

    -- 8. PT hợp lệ – Tăng cơ
    (8, N'Quân', N'Đinh',
     N'quan.dinh@utefit.vn', N'0902 111 008', N'123456',
     NULL, NULL,
     N'PT', N'ACTIVE', 1,
     SYSDATETIME(), SYSDATETIME()),

    -- 9. PT hợp lệ – Chưa gán chi nhánh
    (9, N'Thảo', N'Bùi',
     N'thao.bui@utefit.vn', N'0902 111 009', N'123456',
     NULL, NULL,
     N'PT', N'ACTIVE', 1,
     SYSDATETIME(), SYSDATETIME()),

    -- 10. PT KHÔNG HỢP LỆ – user LOCKED (sẽ không hiển thị ở /trainers)
    (10, N'Hùng', N'Ngô',
     N'hung.ngo@utefit.vn', N'0902 111 010', N'123456',
     NULL, NULL,
     N'PT', N'LOCKED', 1,
     SYSDATETIME(), SYSDATETIME());
GO

SET IDENTITY_INSERT dbo.users OFF;
GO


/* =========================================
   8. SEED DỮ LIỆU MẪU – TRAINERS
   9 trainer hợp lệ + 1 trainer bị loại (user LOCKED)
   ========================================= */

SET IDENTITY_INSERT dbo.trainers ON;
GO

INSERT INTO dbo.trainers
    (id, user_id, branch_id,
     specialization, years_experience, bio,
     active, created_at, updated_at)
VALUES
    -- 1. Trang Nguyễn – Yoga – Thủ Đức
    (1, 1, 1,
     N'Yoga & Mobility', 5,
     N'Chuyên về Yoga cơ bản, phục hồi, giãn cơ cho sinh viên và người mới bắt đầu.',
     1, SYSDATETIME(), SYSDATETIME()),

    -- 2. Minh Lê – Gym cơ bản – Thủ Đức
    (2, 2, 1,
     N'Gym cơ bản & giảm mỡ', 3,
     N'Huấn luyện viên thể hình cơ bản, tập trung vào giảm mỡ và cải thiện sức khỏe tổng quát.',
     1, SYSDATETIME(), SYSDATETIME()),

    -- 3. Khoa Phạm – Powerlifting – Quận 1
    (3, 3, 2,
     N'Powerlifting & Strength', 6,
     N'Chuyên về sức mạnh, kỹ thuật squat, bench press, deadlift.',
     1, SYSDATETIME(), SYSDATETIME()),

    -- 4. Huy Trần – Boxing – Quận 1
    (4, 4, 2,
     N'Boxing & Conditioning', 4,
     N'Kinh nghiệm thi đấu Boxing phong trào, phù hợp cho học viên muốn vừa học võ vừa giảm mỡ.',
     1, SYSDATETIME(), SYSDATETIME()),

    -- 5. Lan Phan – CrossFit – Bình Thạnh
    (5, 5, 3,
     N'CrossFit & Functional Training', 5,
     N'Tập trung vào luyện tập chức năng, cải thiện sức bền và thể lực toàn diện.',
     1, SYSDATETIME(), SYSDATETIME()),

    -- 6. Tuấn Hoàng – Cardio – Bình Thạnh
    (6, 6, 3,
     N'Cardio & Endurance', 2,
     N'Phù hợp với học viên muốn cải thiện tim mạch, sức bền, chuẩn bị cho các giải chạy.',
     1, SYSDATETIME(), SYSDATETIME()),

    -- 7. Anh Võ – Giảm mỡ – Thủ Đức
    (7, 7, 1,
     N'Fat Loss & Body Recomposition', 7,
     N'Hướng dẫn chế độ tập luyện và dinh dưỡng cho học viên muốn giảm mỡ an toàn.',
     1, SYSDATETIME(), SYSDATETIME()),

    -- 8. Quân Đinh – Tăng cơ – Quận 1
    (8, 8, 2,
     N'Hypertrophy & Muscle Gain', 4,
     N'Kinh nghiệm làm việc với sinh viên gầy, muốn tăng cân và tăng cơ.',
     1, SYSDATETIME(), SYSDATETIME()),

    -- 9. Thảo Bùi – Yoga – CHƯA GÁN CHI NHÁNH (branch_id NULL, vẫn hợp lệ)
    (9, 9, NULL,
     N'Yoga & Stretching', 3,
     N'Phù hợp cho học viên cần giãn cơ, cải thiện tư thế và giảm stress.',
     1, SYSDATETIME(), SYSDATETIME()),

    -- 10. Hùng Ngô – trainer không hợp lệ do user LOCKED
    (10, 10, 1,
     N'Functional Training', 5,
     N'Tài khoản đang bị khóa – dùng để test filter public trainer.',
     1, SYSDATETIME(), SYSDATETIME());
GO

SET IDENTITY_INSERT dbo.trainers OFF;
GO


/* =========================================
   9. KIỂM TRA NHANH
   ========================================= */

PRINT '===== VERIFY DATA =====';
SELECT COUNT(*) AS total_users    FROM dbo.users;
SELECT COUNT(*) AS total_branches FROM dbo.branches;
SELECT COUNT(*) AS total_trainers FROM dbo.trainers;

PRINT 'Danh sách trainer + điều kiện hiển thị public';
SELECT
    t.id           AS trainer_id,
    u.first_name,
    u.last_name,
    u.role,
    u.status,
    u.enabled,
    t.active       AS trainer_active,
    b.name         AS branch_name,
    b.active       AS branch_active,
    CASE
        WHEN t.active = 0             THEN N'Trainer inactive'
        WHEN u.enabled = 0            THEN N'User disabled'
        WHEN u.status <> N'ACTIVE'    THEN N'User not ACTIVE'
        WHEN u.role   <> N'PT'        THEN N'User not PT'
        WHEN b.id IS NOT NULL AND b.active = 0 THEN N'Branch inactive'
        ELSE N'HỢP LỆ'
    END AS result
FROM dbo.trainers t
JOIN dbo.users u
    ON t.user_id = u.id
LEFT JOIN dbo.branches b
    ON t.branch_id = b.id
ORDER BY t.id;
GO