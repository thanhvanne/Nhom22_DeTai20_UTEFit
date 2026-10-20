USE UTEFitDB;
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

BEGIN TRY
BEGIN TRANSACTION;

    DECLARE @password VARCHAR(255) =
    '$2a$10$u3JbAUb0xLv8wBfQwQRwv.IJin5lMfs62HdRpDtURwUG9AeZ4Y0BS';

    DECLARE @now DATETIME2(7) = SYSDATETIME();

    DECLARE @today DATE = CAST(@now AS DATE);

    -- =============================================
    -- 1. USERS
    -- =============================================

INSERT INTO users (
    first_name,
    last_name,
    email,
    password,
    phone,
    role,
    status,
    enabled,
    token_version,
    created_at
)
VALUES
    -- ADMIN
    ('Admin', 'UTEFit',
     'admin@utefit.vn', @password,
     '0901000001', 'ADMIN', 'ACTIVE', 1, 0, @now),

    ('Quan', 'Nguyen',
     'admin2@utefit.vn', @password,
     '0901000002', 'ADMIN', 'ACTIVE', 1, 0, @now),

    -- PERSONAL TRAINERS
    ('Minh', 'Tran',
     'pt.minh@utefit.vn', @password,
     '0902000001', 'PT', 'ACTIVE', 1, 0, @now),

    ('Lan', 'Nguyen',
     'pt.lan@utefit.vn', @password,
     '0902000002', 'PT', 'ACTIVE', 1, 0, @now),

    ('Hung', 'Le',
     'pt.hung@utefit.vn', @password,
     '0902000003', 'PT', 'ACTIVE', 1, 0, @now),

    ('Thao', 'Pham',
     'pt.thao@utefit.vn', @password,
     '0902000004', 'PT', 'ACTIVE', 1, 0, @now),

    ('Khoa', 'Vo',
     'pt.khoa@utefit.vn', @password,
     '0902000005', 'PT', 'ACTIVE', 1, 0, @now),

    ('Nam', 'Bui',
     'pt.nam@utefit.vn', @password,
     '0902000006', 'PT', 'LOCKED', 0, 1, @now),

    -- MEMBERS
    ('An', 'Nguyen',
     'member01@gmail.com', @password,
     '0903000001', 'MEMBER', 'ACTIVE', 1, 0, @now),

    ('Binh', 'Tran',
     'member02@gmail.com', @password,
     '0903000002', 'MEMBER', 'ACTIVE', 1, 0, @now),

    ('Chi', 'Le',
     'member03@gmail.com', @password,
     '0903000003', 'MEMBER', 'ACTIVE', 1, 0, @now),

    ('Dung', 'Pham',
     'member04@gmail.com', @password,
     '0903000004', 'MEMBER', 'ACTIVE', 1, 0, @now),

    ('Hien', 'Vo',
     'member05@gmail.com', @password,
     '0903000005', 'MEMBER', 'ACTIVE', 1, 0, @now),

    ('Long', 'Bui',
     'member06@gmail.com', @password,
     '0903000006', 'MEMBER', 'ACTIVE', 1, 0, @now),

    ('Mai', 'Hoang',
     'member07@gmail.com', @password,
     '0903000007', 'MEMBER', 'ACTIVE', 1, 0, @now),

    ('Phuc', 'Dang',
     'member08@gmail.com', @password,
     '0903000008', 'MEMBER', 'ACTIVE', 1, 0, @now),

    ('Trang', 'Do',
     'member09@gmail.com', @password,
     '0903000009', 'MEMBER', 'LOCKED', 0, 1, @now),

    ('Tuan', 'Phan',
     'member10@gmail.com', @password,
     '0903000010', 'MEMBER', 'UNVERIFIED', 0, 0, @now),

    ('Vy', 'Huynh',
     'member11@gmail.com', @password,
     '0903000011', 'MEMBER', 'ACTIVE', 1, 0, @now),

    ('Khanh', 'Mai',
     'member12@gmail.com', @password,
     '0903000012', 'MEMBER', 'UNVERIFIED', 0, 0, @now);


-- =============================================
-- 2. BRANCHES
-- =============================================

INSERT INTO branches (
    name,
    address,
    phone,
    opening_time,
    closing_time,
    description,
    image_url,
    active,
    created_at
)
VALUES
    (
        'UTEFit Thu Duc',
        '01 Vo Van Ngan, Thu Duc, TP.HCM',
        '02838001001',
        '05:30',
        '22:00',
        'Chi nhanh chinh cua UTEFit tai Thu Duc.',
        'https://placehold.co/800x500?text=UTEFit+Thu+Duc',
        1,
        @now
    ),
    (
        'UTEFit Quan 1',
        '120 Nguyen Hue, Quan 1, TP.HCM',
        '02838001002',
        '06:00',
        '22:00',
        'Phong gym hien dai tai trung tam thanh pho.',
        'https://placehold.co/800x500?text=UTEFit+Quan+1',
        1,
        @now
    ),
    (
        'UTEFit Binh Thanh',
        '200 Dien Bien Phu, Binh Thanh, TP.HCM',
        '02838001003',
        '05:30',
        '23:00',
        'Chi nhanh voi trang thiet bi hien dai.',
        'https://placehold.co/800x500?text=UTEFit+Binh+Thanh',
        1,
        @now
    ),
    (
        'UTEFit Go Vap',
        '45 Quang Trung, Go Vap, TP.HCM',
        '02838001004',
        '06:00',
        '21:30',
        'Chi nhanh phuc vu khu vuc Go Vap.',
        'https://placehold.co/800x500?text=UTEFit+Go+Vap',
        1,
        @now
    ),
    (
        'UTEFit Quan 7',
        '88 Nguyen Thi Thap, Quan 7, TP.HCM',
        '02838001005',
        '06:00',
        '22:00',
        'Chi nhanh dang tam ngung hoat dong.',
        'https://placehold.co/800x500?text=UTEFit+Quan+7',
        0,
        @now
    );


-- =============================================
-- 3. CLASS CATEGORIES
-- =============================================

INSERT INTO class_categories (
    name,
    description,
    active,
    created_at
)
VALUES
    ('Yoga',
     'Cac lop Yoga tang su deo dai va thu gian.',
     1, @now),

    ('Cardio',
     'Cac lop tap tang suc ben tim mach.',
     1, @now),

    ('Strength Training',
     'Cac lop tap phat trien suc manh co bap.',
     1, @now),

    ('HIIT',
     'Cac bai tap cuong do cao.',
     1, @now),

    ('Pilates',
     'Cac bai tap cai thien tu the va co loi.',
     1, @now),

    ('Zumba',
     'Tap luyen ket hop am nhac va vu dao.',
     1, @now),

    ('Boxing',
     'Tap boxing tang suc manh va phan xa.',
     1, @now),

    ('Aerobic Classic',
     'Danh muc lop tap da ngung hoat dong.',
     0, @now);


-- =============================================
-- 4. MEMBERSHIP PLANS
-- =============================================

INSERT INTO membership_plans (
    name,
    description,
    duration_days,
    price,
    active,
    created_at
)
VALUES
    (
        'Basic 1 Month',
        'Goi tap co ban 30 ngay.',
        30,
        399000,
        1,
        @now
    ),
    (
        'Standard 3 Months',
        'Goi tap tieu chuan 90 ngay.',
        90,
        999000,
        1,
        @now
    ),
    (
        'Premium 6 Months',
        'Goi tap cao cap 180 ngay.',
        180,
        1799000,
        1,
        @now
    ),
    (
        'VIP 12 Months',
        'Goi tap VIP 365 ngay.',
        365,
        2999000,
        1,
        @now
    ),
    (
        'Student 1 Month',
        'Goi tap uu dai danh cho sinh vien.',
        30,
        299000,
        1,
        @now
    ),
    (
        'Trial 7 Days',
        'Goi tap thu 7 ngay.',
        7,
        99000,
        1,
        @now
    ),
    (
        'Legacy 3 Months',
        'Goi tap cu khong con kinh doanh.',
        90,
        799000,
        0,
        @now
    );


-- =============================================
-- 5. TRAINERS
-- =============================================

INSERT INTO trainers (
    user_id,
    branch_id,
    specialization,
    years_experience,
    bio,
    active,
    created_at
)
VALUES
    (
        3, 1,
        'Gym, Bodybuilding',
        5,
        'Chuyen gia huan luyen the hinh va tang co.',
        1, @now
    ),
    (
        4, 1,
        'Yoga, Pilates',
        4,
        'Huan luyen vien Yoga va Pilates.',
        1, @now
    ),
    (
        5, 2,
        'Cardio, HIIT',
        6,
        'Chuyen gia giam mo va cai thien the luc.',
        1, @now
    ),
    (
        6, 3,
        'Zumba, Aerobic',
        3,
        'Huan luyen vien Zumba va Aerobic.',
        1, @now
    ),
    (
        7, 4,
        'Boxing, Strength',
        7,
        'Huan luyen vien Boxing va suc manh.',
        1, @now
    ),
    (
        8, NULL,
        'Gym, Fitness',
        2,
        'PT dang tam ngung hoat dong.',
        0, @now
    );


-- =============================================
-- 6. FITNESS CLASSES
-- =============================================

INSERT INTO fitness_classes (
    name,
    description,
    capacity,
    status,
    branch_id,
    category_id,
    trainer_id,
    created_at
)
VALUES
    (
        'Morning Yoga',
        'Yoga buoi sang cho nguoi moi bat dau.',
        20, 'OPEN',
        1, 1, 2, @now
    ),
    (
        'Power Gym',
        'Lop tap tang co va suc manh.',
        15, 'OPEN',
        1, 3, 1, @now
    ),
    (
        'HIIT Burning',
        'Dot mo voi bai tap cuong do cao.',
        10, 'FULL',
        2, 4, 3, @now
    ),
    (
        'Zumba Dance',
        'Tap Zumba ket hop am nhac.',
        25, 'OPEN',
        3, 6, 4, @now
    ),
    (
        'Boxing Beginner',
        'Boxing danh cho nguoi moi.',
        12, 'OPEN',
        4, 7, 5, @now
    ),
    (
        'Pilates Core',
        'Pilates tap trung vao nhom co loi.',
        15, 'OPEN',
        1, 5, 2, @now
    ),
    (
        'Cardio Challenge',
        'Thu thach Cardio.',
        20, 'CANCELLED',
        2, 2, 3, @now
    ),
    (
        'Yoga Relax',
        'Lop Yoga thu gian da hoan thanh.',
        20, 'COMPLETED',
        1, 1, 2, @now
    ),
    (
        'Strength Advanced',
        'Lop tap suc manh nang cao.',
        15, 'COMPLETED',
        1, 3, 1, @now
    ),
    (
        'Fitness Basic',
        'Lop fitness chua phan cong PT.',
        20, 'OPEN',
        3, 3, NULL, @now
    );


-- =============================================
-- 7. CLASS SCHEDULES
-- =============================================

INSERT INTO class_schedules (
    fitness_class_id,
    schedule_date,
    start_time,
    end_time,
    room,
    active,
    created_at
)
VALUES
    (1, DATEADD(DAY, 1, @today),
     '06:00', '07:00', 'Yoga Room 1', 1, @now),

    (1, DATEADD(DAY, 3, @today),
     '06:00', '07:00', 'Yoga Room 1', 1, @now),

    (2, DATEADD(DAY, 1, @today),
     '08:00', '09:30', 'Gym Room A', 1, @now),

    (3, DATEADD(DAY, 2, @today),
     '17:00', '18:00', 'HIIT Room', 1, @now),

    (4, DATEADD(DAY, 2, @today),
     '18:00', '19:00', 'Dance Room', 1, @now),

    (5, DATEADD(DAY, 3, @today),
     '19:00', '20:30', 'Boxing Room', 1, @now),

    (6, DATEADD(DAY, 4, @today),
     '07:00', '08:00', 'Pilates Room', 1, @now),

    (7, DATEADD(DAY, 5, @today),
     '18:00', '19:00', 'Cardio Room', 0, @now),

    (8, DATEADD(DAY, -5, @today),
     '06:00', '07:00', 'Yoga Room 1', 1, @now),

    (9, DATEADD(DAY, -3, @today),
     '08:00', '09:30', 'Gym Room A', 1, @now),

    (10, DATEADD(DAY, 6, @today),
     '09:00', '10:00', 'Gym Room B', 1, @now),

    (2, DATEADD(DAY, 7, @today),
     '08:00', '09:30', 'Gym Room A', 0, @now);


-- =============================================
-- 8. MEMBERSHIPS
-- =============================================

INSERT INTO memberships (
    user_id,
    plan_id,
    branch_id,
    start_date,
    end_date,
    status,
    created_at
)
VALUES
    -- ACTIVE
    (
        9, 1, 1,
        DATEADD(DAY, -5, @today),
        DATEADD(DAY, 25, @today),
        'ACTIVE', @now
    ),
    (
        10, 2, 1,
        DATEADD(DAY, -15, @today),
        DATEADD(DAY, 75, @today),
        'ACTIVE', @now
    ),
    (
        11, 3, 2,
        DATEADD(DAY, -30, @today),
        DATEADD(DAY, 150, @today),
        'ACTIVE', @now
    ),
    (
        12, 4, 3,
        DATEADD(DAY, -60, @today),
        DATEADD(DAY, 305, @today),
        'ACTIVE', @now
    ),
    (
        13, 5, 4,
        DATEADD(DAY, -2, @today),
        DATEADD(DAY, 28, @today),
        'ACTIVE', @now
    ),

    -- PENDING
    (
        14, 2, 1,
        NULL, NULL,
        'PENDING', @now
    ),
    (
        15, 3, 2,
        NULL, NULL,
        'PENDING', @now
    ),

    -- EXPIRED
    (
        16, 1, 1,
        DATEADD(DAY, -60, @today),
        DATEADD(DAY, -30, @today),
        'EXPIRED', @now
    ),

    -- CANCELLED
    (
        17, 4, 3,
        NULL, NULL,
        'CANCELLED', @now
    ),

    -- ADDITIONAL MEMBERSHIPS
    (
        19, 6, 1,
        DATEADD(DAY, -1, @today),
        DATEADD(DAY, 6, @today),
        'ACTIVE', @now
    ),

    (
        9, 7, 1,
        DATEADD(DAY, -200, @today),
        DATEADD(DAY, -110, @today),
        'EXPIRED', @now
    );


-- =============================================
-- 9. PAYMENTS
-- =============================================

INSERT INTO payments (
    user_id,
    membership_id,
    amount,
    method,
    status,
    transaction_code,
    created_at,
    paid_at
)
VALUES
    -- PAID CASH
    (
        9, 1, 399000,
        'CASH', 'PAID',
        'CASH000001',
        DATEADD(DAY, -5, @now),
        DATEADD(DAY, -5, @now)
    ),
    (
        10, 2, 999000,
        'CASH', 'PAID',
        'CASH000002',
        DATEADD(DAY, -15, @now),
        DATEADD(DAY, -15, @now)
    ),

    -- PAID VNPAY
    (
        11, 3, 1799000,
        'VNPAY', 'PAID',
        'VNPAY000001',
        DATEADD(DAY, -30, @now),
        DATEADD(DAY, -30, @now)
    ),
    (
        12, 4, 2999000,
        'VNPAY', 'PAID',
        'VNPAY000002',
        DATEADD(DAY, -60, @now),
        DATEADD(DAY, -60, @now)
    ),
    (
        13, 5, 299000,
        'VNPAY', 'PAID',
        'VNPAY000003',
        DATEADD(DAY, -2, @now),
        DATEADD(DAY, -2, @now)
    ),

    -- PENDING
    (
        14, 6, 999000,
        'VNPAY', 'PENDING',
        NULL,
        @now,
        NULL
    ),

    (
        15, 7, 1799000,
        'CASH', 'PENDING',
        NULL,
        @now,
        NULL
    ),

    -- EXPIRED MEMBERSHIP PAYMENT
    (
        16, 8, 399000,
        'CASH', 'PAID',
        'CASH000003',
        DATEADD(DAY, -60, @now),
        DATEADD(DAY, -60, @now)
    ),

    -- REFUNDED
    (
        17, 9, 2999000,
        'VNPAY', 'REFUNDED',
        'VNPAY000004',
        DATEADD(DAY, -10, @now),
        DATEADD(DAY, -10, @now)
    ),

    -- FAILED
    (
        14, 6, 999000,
        'VNPAY', 'FAILED',
        'VNPAYFAILED001',
        DATEADD(DAY, -1, @now),
        NULL
    ),

    -- TRIAL
    (
        19, 10, 99000,
        'CASH', 'PAID',
        'CASH000004',
        DATEADD(DAY, -1, @now),
        DATEADD(DAY, -1, @now)
    ),

    -- OLD MEMBERSHIP
    (
        9, 11, 799000,
        'VNPAY', 'PAID',
        'VNPAY000005',
        DATEADD(DAY, -200, @now),
        DATEADD(DAY, -200, @now)
    );


-- =============================================
-- 10. CLASS REGISTRATIONS
-- =============================================

INSERT INTO class_registrations (
    user_id,
    schedule_id,
    status,
    registered_at
)
VALUES
    -- REGISTERED
    (9, 1, 'REGISTERED', @now),
    (10, 1, 'REGISTERED', @now),
    (11, 1, 'REGISTERED', @now),

    (9, 3, 'REGISTERED', @now),
    (12, 3, 'REGISTERED', @now),

    (10, 4, 'REGISTERED', @now),
    (11, 4, 'REGISTERED', @now),
    (12, 4, 'REGISTERED', @now),

    (13, 5, 'REGISTERED', @now),
    (19, 5, 'REGISTERED', @now),

    (9, 6, 'REGISTERED', @now),
    (10, 7, 'REGISTERED', @now),

    -- CANCELLED
    (13, 1, 'CANCELLED', @now),
    (11, 3, 'CANCELLED', @now),
    (12, 5, 'CANCELLED', @now),

    -- ATTENDED
    (
        9, 9, 'ATTENDED',
        DATEADD(DAY, -7, @now)
    ),
    (
        10, 9, 'ATTENDED',
        DATEADD(DAY, -7, @now)
    ),
    (
        11, 10, 'ATTENDED',
        DATEADD(DAY, -5, @now)
    ),

    -- ABSENT
    (
        12, 9, 'ABSENT',
        DATEADD(DAY, -7, @now)
    ),
    (
        13, 10, 'ABSENT',
        DATEADD(DAY, -5, @now)
    );


-- =============================================
-- 11. REVIEWS
-- =============================================

INSERT INTO reviews (
    user_id,
    branch_id,
    trainer_id,
    rating,
    comment,
    visible,
    created_at
)
VALUES
    (
        9, 1, 1, 5,
        'PT rat nhiet tinh, phong tap sach se.',
        1, @now
    ),
    (
        10, 1, 2, 5,
        'Lop Yoga rat tot, giao vien than thien.',
        1, @now
    ),
    (
        11, 2, 3, 4,
        'Phong tap day du thiet bi.',
        1, @now
    ),
    (
        12, 3, 4, 4,
        'Lop Zumba vui va nang dong.',
        1, @now
    ),
    (
        13, 4, 5, 5,
        'PT Boxing huong dan rat chi tiet.',
        1, @now
    ),
    (
        14, 1, NULL, 3,
        'Phong tap dong vao gio cao diem.',
        1, @now
    ),
    (
        15, 2, NULL, 2,
        'Can cai thien dich vu khach hang.',
        1, @now
    ),
    (
        16, 1, 1, 1,
        'Trai nghiem chua tot.',
        0, @now
    ),
    (
        17, 3, NULL, 1,
        'Danh gia da bi an boi quan tri vien.',
        0, @now
    ),
    (
        19, 1, 2, 5,
        'Rat hai long voi buoi tap thu.',
        1, @now
    );


-- =============================================
-- 12. OTP CODES
-- =============================================

INSERT INTO otp_codes (
    user_id,
    code,
    type,
    used,
    attempt_count,
    created_at,
    expired_at,
    used_at
)
VALUES
    -- REGISTER - UNUSED / VALID
    (
        18, '123456',
        'REGISTER',
        0, 0,
        @now,
        DATEADD(MINUTE, 10, @now),
        NULL
    ),

    -- REGISTER - EXPIRED
    (
        20, '654321',
        'REGISTER',
        0, 0,
        DATEADD(HOUR, -1, @now),
        DATEADD(MINUTE, -50, @now),
        NULL
    ),

    -- REGISTER - USED
    (
        9, '111111',
        'REGISTER',
        1, 0,
        DATEADD(DAY, -5, @now),
        DATEADD(DAY, -5, DATEADD(MINUTE, 10, @now)),
        DATEADD(DAY, -5, DATEADD(MINUTE, 2, @now))
    ),

    -- FORGOT PASSWORD - VALID
    (
        10, '222222',
        'FORGOT_PASSWORD',
        0, 0,
        @now,
        DATEADD(MINUTE, 10, @now),
        NULL
    ),

    -- FORGOT PASSWORD - EXPIRED
    (
        11, '333333',
        'FORGOT_PASSWORD',
        0, 2,
        DATEADD(HOUR, -2, @now),
        DATEADD(MINUTE, -110, @now),
        NULL
    ),

    -- FORGOT PASSWORD - USED
    (
        12, '444444',
        'FORGOT_PASSWORD',
        1, 1,
        DATEADD(DAY, -1, @now),
        DATEADD(DAY, -1, DATEADD(MINUTE, 10, @now)),
        DATEADD(DAY, -1, DATEADD(MINUTE, 3, @now))
    ),

    -- MULTIPLE FAILED ATTEMPTS
    (
        13, '555555',
        'FORGOT_PASSWORD',
        0, 5,
        @now,
        DATEADD(MINUTE, 10, @now),
        NULL
    );


COMMIT TRANSACTION;

PRINT '=====================================';
    PRINT 'UTEFit SAMPLE DATA INSERTED SUCCESSFULLY';
    PRINT '=====================================';

END TRY
BEGIN CATCH

IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;

    PRINT 'ERROR INSERTING SAMPLE DATA';
    PRINT ERROR_MESSAGE();

    THROW;

END CATCH;
GO