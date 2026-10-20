USE UTEFitDB;
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
BEGIN TRANSACTION;

    IF EXISTS (SELECT 1 FROM dbo.users)
       OR EXISTS (SELECT 1 FROM dbo.branches)
       OR EXISTS (SELECT 1 FROM dbo.class_categories)
       OR EXISTS (SELECT 1 FROM dbo.membership_plans)
       OR EXISTS (SELECT 1 FROM dbo.trainers)
       OR EXISTS (SELECT 1 FROM dbo.fitness_classes)
       OR EXISTS (SELECT 1 FROM dbo.class_schedules)
       OR EXISTS (SELECT 1 FROM dbo.class_registrations)
       OR EXISTS (SELECT 1 FROM dbo.memberships)
       OR EXISTS (SELECT 1 FROM dbo.payments)
       OR EXISTS (SELECT 1 FROM dbo.reviews)
       OR EXISTS (SELECT 1 FROM dbo.otp_codes)
       OR EXISTS (SELECT 1 FROM dbo.trainer_requests)
BEGIN
        THROW 50002, 'Sample database must be empty.', 1;
END;

    DECLARE @now DATETIME2(7) = SYSDATETIME();
    DECLARE @today DATE = CAST(@now AS DATE);

    DECLARE @password VARCHAR(255) =
    '$2a$10$u3JbAUb0xLv8wBfQwQRwv.IJin5lMfs62HdRpDtURwUG9AeZ4Y0BS';

    -- ========================================================
    -- USERS
    -- ========================================================

INSERT INTO dbo.users (
    first_name,last_name,email,password,
    phone,role,status,enabled,
    token_version,created_at,updated_at
)
VALUES
    ('System','Admin','admin@utefit.vn',@password,
     '0901000001','ADMIN','ACTIVE',1,0,@now,@now),

    ('Quan','Nguyen','admin2@utefit.vn',@password,
     '0901000002','ADMIN','ACTIVE',1,0,@now,@now),

    ('Ha','Tran','admin3@utefit.vn',@password,
     '0901000003','ADMIN','ACTIVE',1,0,@now,@now);

-- 12 PT
DECLARE @i INT = 1;

    WHILE @i <= 12
BEGIN
INSERT INTO dbo.users (
    first_name,last_name,email,password,
    phone,role,status,enabled,
    token_version,created_at,updated_at
)
VALUES (
           CASE @i
               WHEN 1 THEN 'Minh'
               WHEN 2 THEN 'Lan'
               WHEN 3 THEN 'Hung'
               WHEN 4 THEN 'Thao'
               WHEN 5 THEN 'Khoa'
               WHEN 6 THEN 'Mai'
               WHEN 7 THEN 'Duc'
               WHEN 8 THEN 'Linh'
               WHEN 9 THEN 'Phong'
               WHEN 10 THEN 'Nhi'
               WHEN 11 THEN 'Nam'
               ELSE 'Vy'
               END,
           CASE @i % 5
               WHEN 0 THEN 'Nguyen'
                WHEN 1 THEN 'Tran'
                WHEN 2 THEN 'Le'
                WHEN 3 THEN 'Pham'
                ELSE 'Vo'
            END,
           CONCAT('pt',RIGHT(CONCAT('00',@i),2),'@utefit.vn'),
           @password,
           CONCAT('0902',RIGHT(CONCAT('000000',@i),6)),
           'PT',
           CASE WHEN @i = 11 THEN 'LOCKED' ELSE 'ACTIVE' END,
           CASE WHEN @i = 11 THEN 0 ELSE 1 END,
           CASE WHEN @i = 11 THEN 1 ELSE 0 END,
           DATEADD(DAY,-100,@now),
           @now
       );

SET @i += 1;
END;

    -- 60 MEMBERS
    SET @i = 1;

    WHILE @i <= 60
BEGIN
INSERT INTO dbo.users (
    first_name,last_name,email,password,
    phone,role,status,enabled,
    token_version,created_at,updated_at
)
VALUES (
           CONCAT('Member',RIGHT(CONCAT('000',@i),3)),
           CASE @i % 6
               WHEN 0 THEN 'Nguyen'
                WHEN 1 THEN 'Tran'
                WHEN 2 THEN 'Le'
                WHEN 3 THEN 'Pham'
                WHEN 4 THEN 'Vo'
                ELSE 'Bui'
            END,
           CONCAT(
                   'member',
               RIGHT(CONCAT('000',@i),3),
                   '@gmail.com'
           ),
           @password,
           CONCAT('0903',RIGHT(CONCAT('000000',@i),6)),
           'MEMBER',
           CASE
               WHEN @i <= 50 THEN 'ACTIVE'
               WHEN @i <= 55 THEN 'LOCKED'
               ELSE 'UNVERIFIED'
               END,
           CASE WHEN @i <= 50 THEN 1 ELSE 0 END,
           CASE WHEN @i BETWEEN 51 AND 55 THEN 1 ELSE 0 END,
           DATEADD(DAY,-@i,@now),
           @now
       );

SET @i += 1;
END;

    -- ========================================================
    -- BRANCHES
    -- ========================================================

INSERT INTO dbo.branches (
    name,address,phone,
    opening_time,closing_time,
    description,image_url,active,
    created_at,updated_at
)
VALUES
    ('UTEFit Thu Duc',
     '01 Vo Van Ngan, Thu Duc, TP.HCM',
     '02838001001','05:30','22:00',
     'Chi nhanh chinh cua UTEFit.',
     'https://placehold.co/800x500?text=UTEFit+Thu+Duc',
     1,@now,@now),

    ('UTEFit Quan 1',
     '120 Nguyen Hue, Quan 1, TP.HCM',
     '02838001002','06:00','22:00',
     'Phong gym cao cap tai trung tam.',
     'https://placehold.co/800x500?text=UTEFit+Quan+1',
     1,@now,@now),

    ('UTEFit Binh Thanh',
     '200 Dien Bien Phu, Binh Thanh, TP.HCM',
     '02838001003','05:30','23:00',
     'Trang thiet bi hien dai.',
     'https://placehold.co/800x500?text=UTEFit+Binh+Thanh',
     1,@now,@now),

    ('UTEFit Go Vap',
     '45 Quang Trung, Go Vap, TP.HCM',
     '02838001004','06:00','22:00',
     'Phong gym danh cho moi doi tuong.',
     'https://placehold.co/800x500?text=UTEFit+Go+Vap',
     1,@now,@now),

    ('UTEFit Quan 7',
     '88 Nguyen Thi Thap, Quan 7, TP.HCM',
     '02838001005','06:00','22:00',
     'Khong gian tap luyen rong rai.',
     'https://placehold.co/800x500?text=UTEFit+Quan+7',
     1,@now,@now),

    ('UTEFit Tan Binh',
     '150 Cong Hoa, Tan Binh, TP.HCM',
     '02838001006','05:30','22:00',
     'Phong gym hien dai.',
     'https://placehold.co/800x500?text=UTEFit+Tan+Binh',
     1,@now,@now),

    ('UTEFit Quan 10',
     '300 Su Van Hanh, Quan 10, TP.HCM',
     '02838001007','06:00','22:00',
     'Chi nhanh moi.',
     'https://placehold.co/800x500?text=UTEFit+Quan+10',
     1,@now,@now),

    ('UTEFit Quan 3',
     '100 Vo Van Tan, Quan 3, TP.HCM',
     '02838001008','06:00','21:00',
     'Chi nhanh tam ngung.',
     NULL,0,@now,@now),

    ('UTEFit Binh Tan',
     '50 Ten Lua, Binh Tan, TP.HCM',
     '02838001009','06:00','21:00',
     'Chi nhanh tam ngung.',
     NULL,0,@now,@now),

    ('UTEFit Hoc Mon',
     '20 To Ky, Hoc Mon, TP.HCM',
     '02838001010','06:00','21:00',
     'Chi nhanh chuan bi khai truong.',
     NULL,0,@now,@now);

-- ========================================================
-- CLASS CATEGORIES
-- ========================================================

INSERT INTO dbo.class_categories (
    name,description,active,created_at,updated_at
)
VALUES
    ('Yoga','Yoga va thien.',1,@now,@now),
    ('Cardio','Cai thien tim mach.',1,@now,@now),
    ('Strength Training','Tang co va suc manh.',1,@now,@now),
    ('HIIT','Tap luyen cuong do cao.',1,@now,@now),
    ('Pilates','Cai thien co loi.',1,@now,@now),
    ('Zumba','The duc ket hop am nhac.',1,@now,@now),
    ('Boxing','Tap boxing.',1,@now,@now),
    ('CrossFit','Tap luyen tong hop.',1,@now,@now),
    ('Cycling','Dap xe trong nha.',1,@now,@now),
    ('Aerobic Classic','Danh muc cu.',0,@now,@now);

-- ========================================================
-- MEMBERSHIP PLANS
-- ========================================================

INSERT INTO dbo.membership_plans (
    name,description,duration_days,
    price,active,created_at,updated_at
)
VALUES
    ('Trial 7 Days','Tap thu 7 ngay.',7,99000,1,@now,@now),
    ('Basic 1 Month','Goi tap 30 ngay.',30,399000,1,@now,@now),
    ('Student 1 Month','Goi sinh vien.',30,299000,1,@now,@now),
    ('Standard 3 Months','Goi tap 90 ngay.',90,999000,1,@now,@now),
    ('Premium 6 Months','Goi tap 180 ngay.',180,1799000,1,@now,@now),
    ('VIP 12 Months','Goi tap 365 ngay.',365,2999000,1,@now,@now),
    ('Couple 3 Months','Goi cap doi.',90,1799000,1,@now,@now),
    ('Premium Plus','Goi cao cap.',365,3999000,1,@now,@now),
    ('Legacy Basic','Goi cu.',30,199000,0,@now,@now),
    ('Legacy Premium','Goi cao cap cu.',180,1499000,0,@now,@now);

-- ========================================================
-- TRAINERS
-- ========================================================

SET @i = 1;

    WHILE @i <= 12
BEGIN
INSERT INTO dbo.trainers (
    user_id,branch_id,
    specialization,years_experience,
    bio,active,created_at,updated_at
)
VALUES (
           (
               SELECT id FROM dbo.users
               WHERE email = CONCAT(
                       'pt',RIGHT(CONCAT('00',@i),2),'@utefit.vn'
                             )
           ),
           CASE
               WHEN @i = 11 THEN NULL
               ELSE ((@i-1)%7)+1
END,
            CASE
                WHEN @i = 11 THEN 'General Fitness'
                WHEN @i = 12 THEN 'Yoga, Pilates'
                ELSE 'General Fitness'
END,
            (@i%8)+1,
            CONCAT('UTEFit Personal Trainer ',@i),
            CASE WHEN @i = 11 THEN 0 ELSE 1 END,
            @now,@now
        );

        SET @i += 1;
END;

    -- ========================================================
    -- FITNESS CLASSES
    -- 14 OPEN, 4 FULL, 4 COMPLETED, 2 CANCELLED
    -- ========================================================

    SET @i = 1;

    WHILE @i <= 24
BEGIN
        DECLARE @classStatus VARCHAR(20);

        SET @classStatus =
            CASE
                WHEN @i <= 12 THEN 'OPEN'
                WHEN @i <= 16 THEN 'FULL'
                WHEN @i <= 20 THEN 'COMPLETED'
                WHEN @i <= 22 THEN 'CANCELLED'
                ELSE 'OPEN'
END;

INSERT INTO dbo.fitness_classes (
    name,description,capacity,status,
    branch_id,category_id,trainer_id,
    created_at,updated_at
)
VALUES (
           CONCAT(
                   CASE @i%6
                       WHEN 0 THEN 'Yoga'
                    WHEN 1 THEN 'Gym'
                    WHEN 2 THEN 'HIIT'
                    WHEN 3 THEN 'Boxing'
                    WHEN 4 THEN 'Cardio'
                    ELSE 'Pilates'
                END,
                   ' Class ',
               RIGHT(CONCAT('00',@i),2)
            ),
           CONCAT('UTEFit training class number ',@i),
           CASE
               WHEN @classStatus = 'FULL' THEN 8
               ELSE 20
               END,
           @classStatus,
           ((@i-1)%7)+1,
            ((@i-1)%9)+1,
            CASE
                WHEN @i IN (5,23) THEN NULL
                ELSE ((@i-1)%7)+1
END,
            @now,@now
        );

        SET @i += 1;
END;

    -- ========================================================
    -- CLASS SCHEDULES
    -- ========================================================

    SET @i = 1;

    WHILE @i <= 24
BEGIN
INSERT INTO dbo.class_schedules (
    fitness_class_id,schedule_date,
    start_time,end_time,
    room,active,created_at
)
VALUES (
           @i,
           CASE
               WHEN @i BETWEEN 17 AND 20
                   THEN DATEADD(DAY,-10-@i,@today)
               ELSE DATEADD(DAY,@i,@today)
               END,
           '08:00','09:00',
           CONCAT('Room ',@i),
           CASE WHEN @i BETWEEN 21 AND 22 THEN 0 ELSE 1 END,
           @now
       );

SET @i += 1;
END;

    -- ========================================================
    -- MEMBERSHIPS AND PAYMENTS
    -- ACTIVE: 30
    -- PENDING: 6
    -- EXPIRED: 6
    -- CANCELLED: 4
    -- ========================================================

    SET @i = 1;

    WHILE @i <= 46
BEGIN
        DECLARE @memberId BIGINT;
        DECLARE @planId BIGINT;
        DECLARE @branchId BIGINT;
        DECLARE @membershipId BIGINT;
        DECLARE @duration INT;
        DECLARE @price NUMERIC(15,2);
        DECLARE @membershipStatus VARCHAR(20);
        DECLARE @paymentStatus VARCHAR(20);
        DECLARE @paymentMethod VARCHAR(20);
        DECLARE @start DATE;
        DECLARE @end DATE;
        DECLARE @created DATETIME2(7);

SELECT @memberId = id
FROM dbo.users
WHERE email = CONCAT(
        'member',
    RIGHT(CONCAT('000',@i),3),
        '@gmail.com'
              );

SET @planId = ((@i-1)%8)+1;
        SET @branchId = ((@i-1)%7)+1;

SELECT
    @duration = duration_days,
    @price = price
FROM dbo.membership_plans
WHERE id = @planId;

SET @membershipStatus =
            CASE
                WHEN @i <= 30 THEN 'ACTIVE'
                WHEN @i <= 36 THEN 'PENDING'
                WHEN @i <= 42 THEN 'EXPIRED'
                ELSE 'CANCELLED'
END;

        SET @start = NULL;
        SET @end = NULL;

        IF @membershipStatus = 'ACTIVE'
BEGIN
            SET @start = DATEADD(DAY,-2,@today);
            SET @end = DATEADD(DAY,@duration,@start);
END;

        IF @membershipStatus = 'EXPIRED'
BEGIN
            SET @end = DATEADD(DAY,-10,@today);
            SET @start = DATEADD(DAY,-@duration,@end);
END;

        SET @created =
            CASE
                WHEN @membershipStatus = 'EXPIRED'
                    THEN DATEADD(DAY,-@duration-15,@now)
                WHEN @membershipStatus = 'ACTIVE'
                    THEN DATEADD(DAY,-2,@now)
                WHEN @membershipStatus = 'CANCELLED'
                    THEN DATEADD(DAY,-10,@now)
                ELSE DATEADD(HOUR,-2,@now)
END;

INSERT INTO dbo.memberships (
    user_id,plan_id,branch_id,
    start_date,end_date,status,
    created_at,updated_at
)
VALUES (
           @memberId,@planId,@branchId,
           @start,@end,@membershipStatus,
           @created,@now
       );

SET @membershipId = SCOPE_IDENTITY();

        SET @paymentStatus =
            CASE
                WHEN @membershipStatus IN ('ACTIVE','EXPIRED')
                    THEN 'PAID'
                WHEN @membershipStatus = 'PENDING'
                    THEN 'PENDING'
                ELSE 'REFUNDED'
END;

        SET @paymentMethod =
            CASE WHEN @i%2 = 0 THEN 'CASH' ELSE 'VNPAY' END;

INSERT INTO dbo.payments (
    user_id,membership_id,amount,
    method,status,transaction_code,
    created_at,paid_at
)
VALUES (
           @memberId,@membershipId,@price,
           @paymentMethod,@paymentStatus,
           CASE
               WHEN @paymentStatus = 'PENDING' THEN NULL
               WHEN @paymentMethod = 'CASH'
                   THEN CONCAT('CASH-SEED-',@i)
               ELSE CONCAT('VNPAY-SEED-',@i)
               END,
           @created,
           CASE
               WHEN @paymentStatus = 'PENDING' THEN NULL
               ELSE DATEADD(HOUR,1,@created)
               END
       );

SET @i += 1;
END;

    -- FAILED payment attempts for PENDING memberships
INSERT INTO dbo.payments (
    user_id,membership_id,amount,
    method,status,transaction_code,
    created_at,paid_at
)
SELECT
    m.user_id,
    m.id,
    p.price,
    'VNPAY',
    'FAILED',
    CONCAT('VNPAY-FAILED-',m.id),
    DATEADD(DAY,-1,@now),
    NULL
FROM dbo.memberships m
         JOIN dbo.membership_plans p ON m.plan_id = p.id
WHERE m.status = 'PENDING';

-- ========================================================
-- CLASS REGISTRATIONS
-- ========================================================

-- FULL classes: exactly 8 registrations each.
DECLARE @scheduleId INT = 13;
    DECLARE @student INT;

    WHILE @scheduleId <= 16
BEGIN
        SET @student = 1;

        WHILE @student <= 8
BEGIN
INSERT INTO dbo.class_registrations (
    user_id,schedule_id,status,registered_at
)
SELECT
    id,
    @scheduleId,
    'REGISTERED',
    DATEADD(DAY,-1,@now)
FROM dbo.users
WHERE email = CONCAT(
        'member',
    RIGHT(CONCAT('000',@student),3),
        '@gmail.com'
              );

SET @student += 1;
END;

        SET @scheduleId += 1;
END;

    -- OPEN classes: 5 registered students per schedule.
    SET @scheduleId = 1;

    WHILE @scheduleId <= 24
BEGIN
        IF @scheduleId <= 12 OR @scheduleId >= 23
BEGIN
            SET @student = 1;

            WHILE @student <= 5
BEGIN
INSERT INTO dbo.class_registrations (
    user_id,schedule_id,status,registered_at
)
SELECT
    id,
    @scheduleId,
    'REGISTERED',
    DATEADD(DAY,-1,@now)
FROM dbo.users
WHERE email = CONCAT(
        'member',
    RIGHT(CONCAT('000',@student),3),
        '@gmail.com'
              );

SET @student += 1;
END;
END;

        SET @scheduleId += 1;
END;

    -- COMPLETED classes: 6 ATTENDED + 2 ABSENT.
    SET @scheduleId = 17;

    WHILE @scheduleId <= 20
BEGIN
        SET @student = 1;

        WHILE @student <= 8
BEGIN
INSERT INTO dbo.class_registrations (
    user_id,schedule_id,status,registered_at
)
SELECT
    id,
    @scheduleId,
    CASE
        WHEN @student <= 6 THEN 'ATTENDED'
        ELSE 'ABSENT'
        END,
    DATEADD(DAY,-35,@now)
FROM dbo.users
WHERE email = CONCAT(
        'member',
    RIGHT(CONCAT('000',@student),3),
        '@gmail.com'
              );

SET @student += 1;
END;

        SET @scheduleId += 1;
END;

    -- CANCELLED classes: only cancelled registrations.
    SET @scheduleId = 21;

    WHILE @scheduleId <= 22
BEGIN
        SET @student = 1;

        WHILE @student <= 5
BEGIN
INSERT INTO dbo.class_registrations (
    user_id,schedule_id,status,registered_at
)
SELECT
    id,
    @scheduleId,
    'CANCELLED',
    DATEADD(DAY,-5,@now)
FROM dbo.users
WHERE email = CONCAT(
        'member',
    RIGHT(CONCAT('000',@student),3),
        '@gmail.com'
              );

SET @student += 1;
END;

        SET @scheduleId += 1;
END;

    -- ========================================================
    -- REVIEWS
    -- ========================================================

    SET @i = 1;

    WHILE @i <= 30
BEGIN
INSERT INTO dbo.reviews (
    user_id,branch_id,trainer_id,
    rating,comment,visible,
    created_at,updated_at
)
SELECT
    id,
    ((@i-1)%7)+1,
    CASE
    WHEN @i%3 = 0 THEN NULL
    ELSE ((@i-1)%7)+1
END,
            ((@i-1)%5)+1,
            CASE ((@i-1)%5)+1
                WHEN 5 THEN 'Excellent training experience.'
                WHEN 4 THEN 'Good equipment and friendly staff.'
                WHEN 3 THEN 'Overall satisfactory service.'
                WHEN 2 THEN 'Service needs improvement.'
                ELSE 'Not satisfied with the experience.'
END,
            CASE WHEN @i%5 = 0 THEN 0 ELSE 1 END,
            DATEADD(DAY,-@i,@now),
            @now
        FROM dbo.users
        WHERE email = CONCAT(
            'member',
            RIGHT(CONCAT('000',@i),3),
            '@gmail.com'
        );

        SET @i += 1;
END;

    -- ========================================================
    -- OTP CODES
    -- ========================================================

    -- Uses the supplied BCrypt hash as a sample OTP hash.
    -- These are state-testing records, not known usable OTPs.

    SET @i = 1;

    WHILE @i <= 15
BEGIN
        DECLARE @otpCreated DATETIME2(7);
        DECLARE @otpExpired DATETIME2(7);
        DECLARE @otpUsed BIT;
        DECLARE @otpUsedAt DATETIME2(7);

        IF @i <= 5
BEGIN
            SET @otpCreated = DATEADD(MINUTE,-1,@now);
            SET @otpExpired = DATEADD(MINUTE,4,@now);
            SET @otpUsed = 0;
            SET @otpUsedAt = NULL;
END
ELSE IF @i <= 10
BEGIN
            SET @otpCreated = DATEADD(HOUR,-2,@now);
            SET @otpExpired = DATEADD(MINUTE,5,@otpCreated);
            SET @otpUsed = 0;
            SET @otpUsedAt = NULL;
END
ELSE
BEGIN
            SET @otpCreated = DATEADD(DAY,-1,@now);
            SET @otpExpired = DATEADD(MINUTE,5,@otpCreated);
            SET @otpUsed = 1;
            SET @otpUsedAt = DATEADD(MINUTE,2,@otpCreated);
END;

INSERT INTO dbo.otp_codes (
    user_id,code,type,used,
    attempt_count,created_at,
    expired_at,used_at
)
SELECT
    id,
    @password,
    CASE
        WHEN @i%2 = 0 THEN 'REGISTER'
        ELSE 'FORGOT_PASSWORD'
        END,
    @otpUsed,
    CASE
        WHEN @i = 5 THEN 5
        ELSE @i%3
        END,
    @otpCreated,
    @otpExpired,
    @otpUsedAt
FROM dbo.users
WHERE email = CONCAT(
        'member',
    RIGHT(CONCAT('000',@i),3),
        '@gmail.com'
              );

SET @i += 1;
END;

    -- ========================================================
    -- TRAINER REQUESTS
    -- ========================================================

    SET @i = 1;

    WHILE @i <= 30
BEGIN
        DECLARE @requestStatus VARCHAR(20);
        DECLARE @requestedAt DATETIME2(7);

        SET @requestStatus =
            CASE
                WHEN @i <= 12 THEN 'PENDING'
                WHEN @i <= 24 THEN 'ACCEPTED'
                ELSE 'REJECTED'
END;

        SET @requestedAt = DATEADD(DAY,-@i,@now);

INSERT INTO dbo.trainer_requests (
    member_id,trainer_id,message,
    status,requested_at,responded_at
)
SELECT
    id,
    ((@i-1)%10)+1,
    CASE @i%4
    WHEN 0 THEN 'I want to build muscle.'
    WHEN 1 THEN 'I need a weight loss program.'
    WHEN 2 THEN 'I want personal Yoga training.'
    ELSE 'I need help improving endurance.'
END,
            @requestStatus,
            @requestedAt,
            CASE
                WHEN @requestStatus = 'PENDING'
                    THEN NULL
                ELSE DATEADD(HOUR,2,@requestedAt)
END
FROM dbo.users
        WHERE email = CONCAT(
            'member',
            RIGHT(CONCAT('000',@i),3),
            '@gmail.com'
        );

        SET @i += 1;
END;

    -- ========================================================
    -- DATA VALIDATION
    -- ========================================================

    -- 1. FULL classes must have exactly capacity registrations.
    IF EXISTS (
        SELECT 1
        FROM dbo.fitness_classes fc
        JOIN dbo.class_schedules cs
            ON cs.fitness_class_id = fc.id
        LEFT JOIN dbo.class_registrations cr
            ON cr.schedule_id = cs.id
            AND cr.status = 'REGISTERED'
        WHERE fc.status = 'FULL'
        GROUP BY fc.id,fc.capacity
        HAVING COUNT(cr.id) <> fc.capacity
    )
BEGIN
        THROW 50010, 'FULL class capacity mismatch.', 1;
END;

    -- 2. OPEN classes must have available seats.
    IF EXISTS (
        SELECT 1
        FROM dbo.fitness_classes fc
        JOIN dbo.class_schedules cs
            ON cs.fitness_class_id = fc.id
        LEFT JOIN dbo.class_registrations cr
            ON cr.schedule_id = cs.id
            AND cr.status = 'REGISTERED'
        WHERE fc.status = 'OPEN'
        GROUP BY fc.id,fc.capacity
        HAVING COUNT(cr.id) >= fc.capacity
    )
BEGIN
        THROW 50011, 'OPEN class has no available seats.', 1;
END;

    -- 3. CANCELLED classes cannot have active registrations.
    IF EXISTS (
        SELECT 1
        FROM dbo.fitness_classes fc
        JOIN dbo.class_schedules cs
            ON cs.fitness_class_id = fc.id
        JOIN dbo.class_registrations cr
            ON cr.schedule_id = cs.id
        WHERE fc.status = 'CANCELLED'
          AND cr.status = 'REGISTERED'
    )
BEGIN
        THROW 50012, 'CANCELLED class has active registrations.', 1;
END;

    -- 4. ACTIVE memberships must have valid dates.
    IF EXISTS (
        SELECT 1
        FROM dbo.memberships
        WHERE status = 'ACTIVE'
          AND (
              start_date IS NULL
              OR end_date IS NULL
              OR start_date > @today
              OR end_date < @today
          )
    )
BEGIN
        THROW 50013, 'Invalid ACTIVE membership dates.', 1;
END;

    -- 5. No member may have two PENDING memberships.
    IF EXISTS (
        SELECT user_id
        FROM dbo.memberships
        WHERE status = 'PENDING'
        GROUP BY user_id
        HAVING COUNT(*) > 1
    )
BEGIN
        THROW 50014, 'Duplicate PENDING memberships.', 1;
END;

    -- 6. Payment owner must match membership owner.
    IF EXISTS (
        SELECT 1
        FROM dbo.payments p
        JOIN dbo.memberships m
            ON p.membership_id = m.id
        WHERE p.user_id <> m.user_id
    )
BEGIN
        THROW 50015, 'Payment owner mismatch.', 1;
END;

    -- 7. PAID payments must have paid_at.
    IF EXISTS (
        SELECT 1
        FROM dbo.payments
        WHERE status = 'PAID'
          AND paid_at IS NULL
    )
BEGIN
        THROW 50016, 'PAID payment missing paid_at.', 1;
END;

    -- 8. Trainer requests must have valid timestamps.
    IF EXISTS (
        SELECT 1
        FROM dbo.trainer_requests
        WHERE
            (status = 'PENDING' AND responded_at IS NOT NULL)
            OR
            (status IN ('ACCEPTED','REJECTED')
             AND responded_at IS NULL)
            OR
            (responded_at < requested_at)
    )
BEGIN
        THROW 50017, 'Invalid trainer request timestamps.', 1;
END;

    -- 9. No duplicate pending trainer requests.
    IF EXISTS (
        SELECT member_id,trainer_id
        FROM dbo.trainer_requests
        WHERE status = 'PENDING'
        GROUP BY member_id,trainer_id
        HAVING COUNT(*) > 1
    )
BEGIN
        THROW 50018, 'Duplicate PENDING trainer requests.', 1;
END;

    -- 10. Schedules must have valid time ranges.
    IF EXISTS (
        SELECT 1
        FROM dbo.class_schedules
        WHERE start_time >= end_time
    )
BEGIN
        THROW 50019, 'Invalid schedule time range.', 1;
END;

    -- 11. ACTIVE schedules must not overlap for the same PT.
    IF EXISTS (
        SELECT 1
        FROM dbo.class_schedules s1
        JOIN dbo.class_schedules s2
            ON s1.id < s2.id
           AND s1.schedule_date = s2.schedule_date
           AND s1.start_time < s2.end_time
           AND s2.start_time < s1.end_time
        JOIN dbo.fitness_classes f1
            ON f1.id = s1.fitness_class_id
        JOIN dbo.fitness_classes f2
            ON f2.id = s2.fitness_class_id
        WHERE s1.active = 1
          AND s2.active = 1
          AND f1.trainer_id IS NOT NULL
          AND f1.trainer_id = f2.trainer_id
          AND f1.status <> 'CANCELLED'
          AND f2.status <> 'CANCELLED'
    )
BEGIN
        THROW 50020, 'Overlapping trainer schedules.', 1;
END;

    -- 12. ACTIVE schedules must not overlap in the same room.
    IF EXISTS (
        SELECT 1
        FROM dbo.class_schedules s1
        JOIN dbo.class_schedules s2
            ON s1.id < s2.id
           AND s1.schedule_date = s2.schedule_date
           AND s1.start_time < s2.end_time
           AND s2.start_time < s1.end_time
           AND s1.room = s2.room
        JOIN dbo.fitness_classes f1
            ON f1.id = s1.fitness_class_id
        JOIN dbo.fitness_classes f2
            ON f2.id = s2.fitness_class_id
        WHERE s1.active = 1
          AND s2.active = 1
          AND f1.branch_id = f2.branch_id
          AND f1.status <> 'CANCELLED'
          AND f2.status <> 'CANCELLED'
    )
BEGIN
        THROW 50021, 'Overlapping room schedules.', 1;
END;

COMMIT TRANSACTION;

PRINT  '=============================================';
    PRINT 'UTEFIT SAMPLE DATA INSERTED SUCCESSFULLY';
    PRINT '============================================';

END TRY
BEGIN CATCH
IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;

    PRINT ERROR_MESSAGE();
    THROW;
END CATCH;
GO

-- ============================================================
-- FINAL COUNTS
-- ============================================================

SELECT 'users' AS table_name,COUNT(*) total FROM dbo.users
UNION ALL SELECT 'branches',COUNT(*) FROM dbo.branches
UNION ALL SELECT 'class_categories',COUNT(*) FROM dbo.class_categories
UNION ALL SELECT 'membership_plans',COUNT(*) FROM dbo.membership_plans
UNION ALL SELECT 'trainers',COUNT(*) FROM dbo.trainers
UNION ALL SELECT 'fitness_classes',COUNT(*) FROM dbo.fitness_classes
UNION ALL SELECT 'class_schedules',COUNT(*) FROM dbo.class_schedules
UNION ALL SELECT 'memberships',COUNT(*) FROM dbo.memberships
UNION ALL SELECT 'payments',COUNT(*) FROM dbo.payments
UNION ALL SELECT 'class_registrations',COUNT(*) FROM dbo.class_registrations
UNION ALL SELECT 'reviews',COUNT(*) FROM dbo.reviews
UNION ALL SELECT 'otp_codes',COUNT(*) FROM dbo.otp_codes
UNION ALL SELECT 'trainer_requests',COUNT(*) FROM dbo.trainer_requests;
GO