-- ============================================================
-- UTEFIT DATABASE - SAMPLE DATA
-- FILE: DB_Data_2.sql
-- DATABASE: UTEFitDB
-- SQL SERVER 2022
-- TOTAL TABLES: 19
--
-- REQUIREMENTS:
-- 1. Run DB_Khung_1.sql first.
-- 2. Database must be empty.
-- 3. All accounts use the supplied BCrypt hash.
-- 4. Every sample record is explicitly declared.
-- 5. No WHILE, loops or generated sample records.
-- ============================================================

USE UTEFitDB;
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
BEGIN TRANSACTION;

    -- ========================================================
    -- CHECK DATABASE
    -- ========================================================

    IF (SELECT COUNT(*) FROM sys.tables
        WHERE schema_id = SCHEMA_ID('dbo')) <> 19
BEGIN
        THROW 50001, 'Expected exactly 19 database tables.', 1;
END;

    IF EXISTS (SELECT 1 FROM dbo.users)
       OR EXISTS (SELECT 1 FROM dbo.branches)
       OR EXISTS (SELECT 1 FROM dbo.trainers)
       OR EXISTS (SELECT 1 FROM dbo.fitness_classes)
       OR EXISTS (SELECT 1 FROM dbo.memberships)
       OR EXISTS (SELECT 1 FROM dbo.payments)
       OR EXISTS (SELECT 1 FROM dbo.conversations)
       OR EXISTS (SELECT 1 FROM dbo.messages)
       OR EXISTS (SELECT 1 FROM dbo.notifications)
       OR EXISTS (SELECT 1 FROM dbo.promotions)
BEGIN
        THROW 50002, 'Database must be empty.', 1;
END;

    DECLARE @now DATETIME2(7) = SYSDATETIME();
    DECLARE @today DATE = CAST(@now AS DATE);

    DECLARE @password VARCHAR(255) =
        '$2a$10$u3JbAUb0xLv8wBfQwQRwv.IJin5lMfs62HdRpDtURwUG9AeZ4Y0BS';

    -- ========================================================
    -- 1. USERS
    --
    -- ADMIN: 3
    -- PT: 12
    -- MEMBER: 50
    -- TOTAL: 65
    --
    -- ID 1-3: ADMIN
    -- ID 4-15: PT
    -- ID 16-65: MEMBER
    -- ========================================================

INSERT INTO dbo.users (
    first_name,
    last_name,
    email,
    password,
    phone,
    role,
    status,
    enabled,
    token_version,
    created_at,
    updated_at
)
VALUES

    -- ================= ADMIN =================

    ('Quang','Nguyen','admin01@gmail.com',@password,'0901000001',
     'ADMIN','ACTIVE',1,0,DATEADD(DAY,-300,@now),@now),

    ('Ha','Tran','admin02@gmail.com',@password,'0901000002',
     'ADMIN','ACTIVE',1,0,DATEADD(DAY,-280,@now),@now),

    ('Minh','Le','admin03@gmail.com',@password,'0901000003',
     'ADMIN','ACTIVE',1,0,DATEADD(DAY,-260,@now),@now),

    -- ================= PERSONAL TRAINERS =================

    ('Minh','Tran','pt01@gmail.com',@password,'0902000001',
     'PT','ACTIVE',1,0,DATEADD(DAY,-250,@now),@now),

    ('Lan','Le','pt02@gmail.com',@password,'0902000002',
     'PT','ACTIVE',1,0,DATEADD(DAY,-240,@now),@now),

    ('Hung','Pham','pt03@gmail.com',@password,'0902000003',
     'PT','ACTIVE',1,0,DATEADD(DAY,-230,@now),@now),

    ('Thao','Vo','pt04@gmail.com',@password,'0902000004',
     'PT','ACTIVE',1,0,DATEADD(DAY,-220,@now),@now),

    ('Khoa','Nguyen','pt05@gmail.com',@password,'0902000005',
     'PT','ACTIVE',1,0,DATEADD(DAY,-210,@now),@now),

    ('Mai','Bui','pt06@gmail.com',@password,'0902000006',
     'PT','ACTIVE',1,0,DATEADD(DAY,-200,@now),@now),

    ('Duc','Hoang','pt07@gmail.com',@password,'0902000007',
     'PT','ACTIVE',1,0,DATEADD(DAY,-190,@now),@now),

    ('Linh','Dang','pt08@gmail.com',@password,'0902000008',
     'PT','ACTIVE',1,0,DATEADD(DAY,-180,@now),@now),

    ('Phong','Phan','pt09@gmail.com',@password,'0902000009',
     'PT','ACTIVE',1,0,DATEADD(DAY,-170,@now),@now),

    ('Nhi','Ngo','pt10@gmail.com',@password,'0902000010',
     'PT','ACTIVE',1,0,DATEADD(DAY,-160,@now),@now),

    ('Nam','Do','pt11@gmail.com',@password,'0902000011',
     'PT','LOCKED',0,1,DATEADD(DAY,-150,@now),@now),

    ('Vy','Nguyen','pt12@gmail.com',@password,'0902000012',
     'PT','ACTIVE',1,0,DATEADD(DAY,-140,@now),@now),

    -- ================= ACTIVE MEMBERS =================

    ('An','Nguyen','member01@gmail.com',@password,'0903000001',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-120,@now),@now),

    ('Binh','Tran','member02@gmail.com',@password,'0903000002',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-119,@now),@now),

    ('Chi','Le','member03@gmail.com',@password,'0903000003',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-118,@now),@now),

    ('Dung','Pham','member04@gmail.com',@password,'0903000004',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-117,@now),@now),

    ('Giang','Vo','member05@gmail.com',@password,'0903000005',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-116,@now),@now),

    ('Hieu','Bui','member06@gmail.com',@password,'0903000006',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-115,@now),@now),

    ('Huong','Hoang','member07@gmail.com',@password,'0903000007',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-114,@now),@now),

    ('Khanh','Dang','member08@gmail.com',@password,'0903000008',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-113,@now),@now),

    ('Lam','Phan','member09@gmail.com',@password,'0903000009',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-112,@now),@now),

    ('Linh','Ngo','member10@gmail.com',@password,'0903000010',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-111,@now),@now),

    ('Long','Do','member11@gmail.com',@password,'0903000011',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-110,@now),@now),

    ('Mai','Nguyen','member12@gmail.com',@password,'0903000012',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-109,@now),@now),

    ('Minh','Tran','member13@gmail.com',@password,'0903000013',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-108,@now),@now),

    ('My','Le','member14@gmail.com',@password,'0903000014',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-107,@now),@now),

    ('Nam','Pham','member15@gmail.com',@password,'0903000015',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-106,@now),@now),

    ('Nga','Vo','member16@gmail.com',@password,'0903000016',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-105,@now),@now),

    ('Ngan','Bui','member17@gmail.com',@password,'0903000017',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-104,@now),@now),

    ('Nghia','Hoang','member18@gmail.com',@password,'0903000018',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-103,@now),@now),

    ('Ngoc','Dang','member19@gmail.com',@password,'0903000019',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-102,@now),@now),

    ('Nhi','Phan','member20@gmail.com',@password,'0903000020',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-101,@now),@now),

    ('Phat','Ngo','member21@gmail.com',@password,'0903000021',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-100,@now),@now),

    ('Phuc','Do','member22@gmail.com',@password,'0903000022',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-99,@now),@now),

    ('Phuong','Nguyen','member23@gmail.com',@password,'0903000023',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-98,@now),@now),

    ('Quan','Tran','member24@gmail.com',@password,'0903000024',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-97,@now),@now),

    ('Quynh','Le','member25@gmail.com',@password,'0903000025',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-96,@now),@now),

    ('Sang','Pham','member26@gmail.com',@password,'0903000026',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-95,@now),@now),

    ('Son','Vo','member27@gmail.com',@password,'0903000027',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-94,@now),@now),

    ('Tam','Bui','member28@gmail.com',@password,'0903000028',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-93,@now),@now),

    ('Thao','Hoang','member29@gmail.com',@password,'0903000029',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-92,@now),@now),

    ('Thien','Dang','member30@gmail.com',@password,'0903000030',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-91,@now),@now),

    ('Thu','Phan','member31@gmail.com',@password,'0903000031',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-90,@now),@now),

    ('Thuy','Ngo','member32@gmail.com',@password,'0903000032',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-89,@now),@now),

    ('Tien','Do','member33@gmail.com',@password,'0903000033',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-88,@now),@now),

    ('Trang','Nguyen','member34@gmail.com',@password,'0903000034',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-87,@now),@now),

    ('Trinh','Tran','member35@gmail.com',@password,'0903000035',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-86,@now),@now),

    ('Truc','Le','member36@gmail.com',@password,'0903000036',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-85,@now),@now),

    ('Trung','Pham','member37@gmail.com',@password,'0903000037',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-84,@now),@now),

    ('Tuan','Vo','member38@gmail.com',@password,'0903000038',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-83,@now),@now),

    ('Tung','Bui','member39@gmail.com',@password,'0903000039',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-82,@now),@now),

    ('Uyen','Hoang','member40@gmail.com',@password,'0903000040',
     'MEMBER','ACTIVE',1,0,DATEADD(DAY,-81,@now),@now),

    -- ================= LOCKED MEMBERS =================

    ('Van','Dang','member41@gmail.com',@password,'0903000041',
     'MEMBER','LOCKED',0,1,DATEADD(DAY,-80,@now),@now),

    ('Vi','Phan','member42@gmail.com',@password,'0903000042',
     'MEMBER','LOCKED',0,1,DATEADD(DAY,-79,@now),@now),

    ('Viet','Ngo','member43@gmail.com',@password,'0903000043',
     'MEMBER','LOCKED',0,1,DATEADD(DAY,-78,@now),@now),

    ('Vinh','Do','member44@gmail.com',@password,'0903000044',
     'MEMBER','LOCKED',0,1,DATEADD(DAY,-77,@now),@now),

    ('Vy','Nguyen','member45@gmail.com',@password,'0903000045',
     'MEMBER','LOCKED',0,1,DATEADD(DAY,-76,@now),@now),

    -- ================= UNVERIFIED MEMBERS =================

    ('Xuan','Tran','member46@gmail.com',@password,'0903000046',
     'MEMBER','UNVERIFIED',0,0,DATEADD(DAY,-5,@now),@now),

    ('Yen','Le','member47@gmail.com',@password,'0903000047',
     'MEMBER','UNVERIFIED',0,0,DATEADD(DAY,-4,@now),@now),

    ('Bao','Pham','member48@gmail.com',@password,'0903000048',
     'MEMBER','UNVERIFIED',0,0,DATEADD(DAY,-3,@now),@now),

    ('Cuong','Vo','member49@gmail.com',@password,'0903000049',
     'MEMBER','UNVERIFIED',0,0,DATEADD(DAY,-2,@now),@now),

    ('Dat','Bui','member50@gmail.com',@password,'0903000050',
     'MEMBER','UNVERIFIED',0,0,DATEADD(DAY,-1,@now),@now);


-- ========================================================
-- 2. BRANCHES
-- 10 BRANCHES
-- 7 ACTIVE + 3 INACTIVE
-- ========================================================

INSERT INTO dbo.branches (
    name,
    address,
    phone,
    opening_time,
    closing_time,
    description,
    image_url,
    active,
    created_at,
    updated_at
)
VALUES

    ('UTEFit Thu Duc',
     '01 Vo Van Ngan, Thu Duc, TP.HCM',
     '02838001001','05:30','22:00',
     'Chi nhanh chinh voi khu gym, Yoga, Cardio va PT ca nhan.',
     'https://placehold.co/800x500?text=UTEFit+Thu+Duc',
     1,@now,@now),

    ('UTEFit Quan 1',
     '120 Nguyen Hue, Quan 1, TP.HCM',
     '02838001002','06:00','22:00',
     'Phong gym cao cap tai trung tam thanh pho.',
     'https://placehold.co/800x500?text=UTEFit+Quan+1',
     1,@now,@now),

    ('UTEFit Binh Thanh',
     '200 Dien Bien Phu, Binh Thanh, TP.HCM',
     '02838001003','05:30','23:00',
     'Co so chuyen ve tap suc manh va phat trien co bap.',
     'https://placehold.co/800x500?text=UTEFit+Binh+Thanh',
     1,@now,@now),

    ('UTEFit Go Vap',
     '45 Quang Trung, Go Vap, TP.HCM',
     '02838001004','06:00','22:00',
     'Khu Boxing va HIIT hien dai.',
     'https://placehold.co/800x500?text=UTEFit+Go+Vap',
     1,@now,@now),

    ('UTEFit Quan 7',
     '88 Nguyen Thi Thap, Quan 7, TP.HCM',
     '02838001005','06:00','22:00',
     'Khong gian rong rai, phu hop cac lop tap nhom.',
     'https://placehold.co/800x500?text=UTEFit+Quan+7',
     1,@now,@now),

    ('UTEFit Tan Binh',
     '150 Cong Hoa, Tan Binh, TP.HCM',
     '02838001006','05:30','22:00',
     'Co so chuyen Pilates, Yoga va tap phuc hoi.',
     'https://placehold.co/800x500?text=UTEFit+Tan+Binh',
     1,@now,@now),

    ('UTEFit Quan 10',
     '300 Su Van Hanh, Quan 10, TP.HCM',
     '02838001007','06:00','22:00',
     'Co so chuyen CrossFit va tap chuc nang.',
     'https://placehold.co/800x500?text=UTEFit+Quan+10',
     1,@now,@now),

    ('UTEFit Quan 3',
     '100 Vo Van Tan, Quan 3, TP.HCM',
     '02838001008','06:00','21:00',
     'Chi nhanh tam ngung de nang cap thiet bi.',
     NULL,0,@now,@now),

    ('UTEFit Binh Tan',
     '50 Ten Lua, Binh Tan, TP.HCM',
     '02838001009','06:00','21:00',
     'Chi nhanh tam ngung hoat dong.',
     NULL,0,@now,@now),

    ('UTEFit Hoc Mon',
     '20 To Ky, Hoc Mon, TP.HCM',
     '02838001010','06:00','21:00',
     'Chi nhanh dang chuan bi khai truong.',
     NULL,0,@now,@now);


-- ========================================================
-- 3. CLASS CATEGORIES
-- ========================================================

INSERT INTO dbo.class_categories (
    name,description,active,created_at,updated_at
)
VALUES

    ('Yoga','Yoga, thien va cai thien do deo dai.',1,@now,@now),
    ('Cardio','Cai thien tim mach va suc ben.',1,@now,@now),
    ('Strength Training','Tap suc manh va phat trien co bap.',1,@now,@now),
    ('HIIT','Tap ngat quang cuong do cao.',1,@now,@now),
    ('Pilates','Tang suc manh co loi.',1,@now,@now),
    ('Zumba','The duc ket hop am nhac.',1,@now,@now),
    ('Boxing','Tap ky thuat Boxing.',1,@now,@now),
    ('CrossFit','Tap luyen chuc nang tong hop.',1,@now,@now),
    ('Cycling','Dap xe trong nha.',1,@now,@now),
    ('Aerobic Classic','Danh muc cu da ngung hoat dong.',0,@now,@now);


-- ========================================================
-- 4. MEMBERSHIP PLANS
-- ========================================================

INSERT INTO dbo.membership_plans (
    name,description,duration_days,price,
    active,created_at,updated_at
)
VALUES

    ('Trial 7 Days','Goi tap thu 7 ngay.',7,99000,1,@now,@now),

    ('Basic 1 Month','Goi tap co ban 30 ngay.',30,399000,1,@now,@now),

    ('Student 1 Month','Goi tap sinh vien 30 ngay.',30,299000,1,@now,@now),

    ('Standard 3 Months','Goi tap 90 ngay.',90,999000,1,@now,@now),

    ('Premium 6 Months','Goi tap cao cap 180 ngay.',180,1799000,1,@now,@now),

    ('VIP 12 Months','Goi VIP 365 ngay.',365,2999000,1,@now,@now),

    ('Couple 3 Months','Goi uu dai cap doi 90 ngay.',90,1799000,1,@now,@now),

    ('Premium Plus','Goi cao cap 365 ngay.',365,3999000,1,@now,@now),

    ('Legacy Basic','Goi cu ngung kinh doanh.',30,199000,0,@now,@now),

    ('Legacy Premium','Goi cao cap cu ngung kinh doanh.',180,1499000,0,@now,@now);


-- ========================================================
-- 5. TRAINERS
-- ========================================================

INSERT INTO dbo.trainers (
    user_id,branch_id,specialization,
    years_experience,bio,active,
    created_at,updated_at
)
VALUES

    (4,1,'Yoga, Pilates',6,
     'Huan luyen Yoga, thien va van dong phuc hoi.',
     1,@now,@now),

    (5,2,'Cardio, HIIT',5,
     'Chuyen gia giam mo va cai thien suc ben.',
     1,@now,@now),

    (6,3,'Strength Training, CrossFit',8,
     'Huan luyen tang co va tap suc manh.',
     1,@now,@now),

    (7,4,'Boxing, HIIT',7,
     'Chuyen gia Boxing va the luc.',
     1,@now,@now),

    (8,5,'Zumba, Cardio',4,
     'Huan luyen Zumba va Cardio theo nhac.',
     1,@now,@now),

    (9,6,'Pilates, Yoga',6,
     'Chuyen gia Pilates va bai tap co loi.',
     1,@now,@now),

    (10,7,'CrossFit, Strength Training',9,
     'Huan luyen CrossFit va tap chuc nang.',
     1,@now,@now),

    (11,1,'General Fitness',5,
     'Huan luyen the luc tong quat.',
     1,@now,@now),

    (12,2,'General Fitness',4,
     'Huong dan nguoi moi bat dau tap gym.',
     1,@now,@now),

    (13,3,'General Fitness',7,
     'Tu van lo trinh tap luyen ca nhan.',
     1,@now,@now),

    (14,NULL,'General Fitness',3,
     'Huan luyen vien tam ngung hoat dong.',
     0,@now,@now),

    (15,5,'Yoga, Pilates',5,
     'Huong dan Yoga va Pilates tai Quan 7.',
     1,@now,@now);


-- ========================================================
-- 6. FITNESS CLASSES
--
-- OPEN: 12
-- FULL: 3
-- COMPLETED: 3
-- CANCELLED: 2
-- TOTAL: 20
-- ========================================================

INSERT INTO dbo.fitness_classes (
    name,description,capacity,status,
    branch_id,category_id,trainer_id,
    created_at,updated_at
)
VALUES

    -- OPEN

    ('Morning Yoga',
     'Yoga buoi sang danh cho nguoi moi.',
     15,'OPEN',1,1,1,@now,@now),

    ('Cardio Starter',
     'Cardio co ban cai thien suc ben.',
     20,'OPEN',2,2,2,@now,@now),

    ('Strength Fundamentals',
     'Ky thuat tap ta co ban.',
     15,'OPEN',3,3,3,@now,@now),

    ('HIIT Power',
     'HIIT cuong do cao.',
     12,'OPEN',4,4,4,@now,@now),

    ('Zumba Energy',
     'Zumba soi dong theo nhac.',
     20,'OPEN',5,6,5,@now,@now),

    ('Pilates Core',
     'Pilates tang suc manh co loi.',
     15,'OPEN',6,5,6,@now,@now),

    ('CrossFit Beginner',
     'CrossFit cho nguoi moi.',
     15,'OPEN',7,8,7,@now,@now),

    ('Yoga Relaxation',
     'Yoga thu gian sau gio lam viec.',
     18,'OPEN',1,1,1,@now,@now),

    ('Cardio Endurance',
     'Cardio tang suc ben.',
     20,'OPEN',2,2,2,@now,@now),

    ('Muscle Building',
     'Tap luyen phat trien co bap.',
     15,'OPEN',3,3,3,@now,@now),

    ('Boxing Basics',
     'Ky thuat Boxing co ban.',
     12,'OPEN',4,7,4,@now,@now),

    ('Pilates Balance',
     'Pilates cai thien kha nang can bang.',
     15,'OPEN',5,5,12,@now,@now),

    -- FULL

    ('Advanced Yoga',
     'Yoga nang cao gioi han 6 hoc vien.',
     6,'FULL',1,1,1,@now,@now),

    ('HIIT Burning',
     'HIIT dot mo gioi han 6 hoc vien.',
     6,'FULL',2,4,2,@now,@now),

    ('Strength Elite',
     'Nhom tap suc manh gioi han 6 hoc vien.',
     6,'FULL',3,3,3,@now,@now),

    -- COMPLETED

    ('Zumba Summer',
     'Khoa Zumba da hoan thanh.',
     20,'COMPLETED',5,6,5,@now,@now),

    ('Pilates Foundation',
     'Khoa Pilates da hoan thanh.',
     15,'COMPLETED',6,5,6,@now,@now),

    ('CrossFit Challenge',
     'Thu thach CrossFit da ket thuc.',
     15,'COMPLETED',7,8,7,@now,@now),

    -- CANCELLED

    ('Cardio Weekend',
     'Lop bi huy do thay doi lich.',
     20,'CANCELLED',2,2,2,@now,@now),

    ('Boxing Workshop',
     'Lop bi huy do bao tri phong.',
     15,'CANCELLED',4,7,4,@now,@now);


-- ========================================================
-- 7. CLASS SCHEDULES
-- ========================================================

INSERT INTO dbo.class_schedules (
    fitness_class_id,schedule_date,
    start_time,end_time,room,
    active,created_at
)
VALUES

    (1,DATEADD(DAY,1,@today),'06:00','07:00','Yoga A',1,@now),
    (2,DATEADD(DAY,1,@today),'07:30','08:30','Cardio A',1,@now),
    (3,DATEADD(DAY,1,@today),'09:00','10:00','Gym A',1,@now),
    (4,DATEADD(DAY,1,@today),'17:00','18:00','HIIT A',1,@now),
    (5,DATEADD(DAY,2,@today),'18:00','19:00','Dance A',1,@now),
    (6,DATEADD(DAY,2,@today),'07:00','08:00','Pilates A',1,@now),
    (7,DATEADD(DAY,2,@today),'18:30','19:30','CrossFit A',1,@now),
    (8,DATEADD(DAY,3,@today),'06:00','07:00','Yoga A',1,@now),
    (9,DATEADD(DAY,3,@today),'07:30','08:30','Cardio A',1,@now),
    (10,DATEADD(DAY,3,@today),'09:00','10:00','Gym A',1,@now),
    (11,DATEADD(DAY,3,@today),'17:00','18:00','Boxing A',1,@now),
    (12,DATEADD(DAY,4,@today),'08:00','09:00','Pilates B',1,@now),

    (13,DATEADD(DAY,5,@today),'06:30','07:30','Yoga VIP',1,@now),
    (14,DATEADD(DAY,5,@today),'18:00','19:00','HIIT VIP',1,@now),
    (15,DATEADD(DAY,5,@today),'09:00','10:00','Gym VIP',1,@now),

    (16,DATEADD(DAY,-20,@today),'18:00','19:00','Dance A',0,@now),
    (17,DATEADD(DAY,-19,@today),'07:00','08:00','Pilates A',0,@now),
    (18,DATEADD(DAY,-18,@today),'18:30','19:30','CrossFit A',0,@now),

    (19,DATEADD(DAY,6,@today),'07:30','08:30','Cardio A',0,@now),
    (20,DATEADD(DAY,6,@today),'17:00','18:00','Boxing A',0,@now);


-- ========================================================
-- 8. CLASS REGISTRATIONS
--
-- FULL CLASSES:
-- Schedule 13: 6/6
-- Schedule 14: 6/6
-- Schedule 15: 6/6
-- ========================================================

INSERT INTO dbo.class_registrations (
    user_id,schedule_id,status,registered_at
)
VALUES

    -- OPEN 1

    (16,1,'REGISTERED',DATEADD(DAY,-3,@now)),
    (17,1,'REGISTERED',DATEADD(DAY,-2,@now)),
    (18,1,'REGISTERED',DATEADD(DAY,-1,@now)),

    -- OPEN 2

    (19,2,'REGISTERED',DATEADD(DAY,-3,@now)),
    (20,2,'REGISTERED',DATEADD(DAY,-2,@now)),
    (21,2,'REGISTERED',DATEADD(DAY,-1,@now)),

    -- OPEN 3

    (22,3,'REGISTERED',DATEADD(DAY,-3,@now)),
    (23,3,'REGISTERED',DATEADD(DAY,-2,@now)),
    (24,3,'REGISTERED',DATEADD(DAY,-1,@now)),

    -- OPEN 4

    (25,4,'REGISTERED',DATEADD(DAY,-3,@now)),
    (26,4,'REGISTERED',DATEADD(DAY,-2,@now)),
    (27,4,'REGISTERED',DATEADD(DAY,-1,@now)),

    -- OPEN 5

    (28,5,'REGISTERED',DATEADD(DAY,-3,@now)),
    (29,5,'REGISTERED',DATEADD(DAY,-2,@now)),
    (30,5,'REGISTERED',DATEADD(DAY,-1,@now)),

    -- OPEN 6

    (31,6,'REGISTERED',DATEADD(DAY,-3,@now)),
    (32,6,'REGISTERED',DATEADD(DAY,-2,@now)),
    (33,6,'REGISTERED',DATEADD(DAY,-1,@now)),

    -- OPEN 7

    (34,7,'REGISTERED',DATEADD(DAY,-3,@now)),
    (35,7,'REGISTERED',DATEADD(DAY,-2,@now)),
    (36,7,'REGISTERED',DATEADD(DAY,-1,@now)),

    -- OPEN 8

    (37,8,'REGISTERED',DATEADD(DAY,-3,@now)),
    (38,8,'REGISTERED',DATEADD(DAY,-2,@now)),
    (39,8,'REGISTERED',DATEADD(DAY,-1,@now)),

    -- OPEN 9

    (40,9,'REGISTERED',DATEADD(DAY,-3,@now)),
    (41,9,'REGISTERED',DATEADD(DAY,-2,@now)),
    (42,9,'REGISTERED',DATEADD(DAY,-1,@now)),

    -- OPEN 10

    (43,10,'REGISTERED',DATEADD(DAY,-3,@now)),
    (44,10,'REGISTERED',DATEADD(DAY,-2,@now)),
    (45,10,'REGISTERED',DATEADD(DAY,-1,@now)),

    -- OPEN 11

    (16,11,'REGISTERED',DATEADD(DAY,-3,@now)),
    (18,11,'REGISTERED',DATEADD(DAY,-2,@now)),
    (20,11,'REGISTERED',DATEADD(DAY,-1,@now)),

    -- OPEN 12

    (17,12,'REGISTERED',DATEADD(DAY,-3,@now)),
    (19,12,'REGISTERED',DATEADD(DAY,-2,@now)),
    (21,12,'REGISTERED',DATEADD(DAY,-1,@now)),

    -- FULL 13

    (16,13,'REGISTERED',DATEADD(DAY,-5,@now)),
    (17,13,'REGISTERED',DATEADD(DAY,-5,@now)),
    (18,13,'REGISTERED',DATEADD(DAY,-4,@now)),
    (19,13,'REGISTERED',DATEADD(DAY,-4,@now)),
    (20,13,'REGISTERED',DATEADD(DAY,-3,@now)),
    (21,13,'REGISTERED',DATEADD(DAY,-3,@now)),

    -- FULL 14

    (22,14,'REGISTERED',DATEADD(DAY,-5,@now)),
    (23,14,'REGISTERED',DATEADD(DAY,-5,@now)),
    (24,14,'REGISTERED',DATEADD(DAY,-4,@now)),
    (25,14,'REGISTERED',DATEADD(DAY,-4,@now)),
    (26,14,'REGISTERED',DATEADD(DAY,-3,@now)),
    (27,14,'REGISTERED',DATEADD(DAY,-3,@now)),

    -- FULL 15

    (28,15,'REGISTERED',DATEADD(DAY,-5,@now)),
    (29,15,'REGISTERED',DATEADD(DAY,-5,@now)),
    (30,15,'REGISTERED',DATEADD(DAY,-4,@now)),
    (31,15,'REGISTERED',DATEADD(DAY,-4,@now)),
    (32,15,'REGISTERED',DATEADD(DAY,-3,@now)),
    (33,15,'REGISTERED',DATEADD(DAY,-3,@now)),

    -- COMPLETED 16

    (16,16,'ATTENDED',DATEADD(DAY,-25,@now)),
    (17,16,'ATTENDED',DATEADD(DAY,-25,@now)),
    (18,16,'ATTENDED',DATEADD(DAY,-25,@now)),
    (19,16,'ABSENT',DATEADD(DAY,-25,@now)),

    -- COMPLETED 17

    (20,17,'ATTENDED',DATEADD(DAY,-24,@now)),
    (21,17,'ATTENDED',DATEADD(DAY,-24,@now)),
    (22,17,'ATTENDED',DATEADD(DAY,-24,@now)),
    (23,17,'ABSENT',DATEADD(DAY,-24,@now)),

    -- COMPLETED 18

    (24,18,'ATTENDED',DATEADD(DAY,-23,@now)),
    (25,18,'ATTENDED',DATEADD(DAY,-23,@now)),
    (26,18,'ATTENDED',DATEADD(DAY,-23,@now)),
    (27,18,'ABSENT',DATEADD(DAY,-23,@now)),

    -- CANCELLED 19

    (28,19,'CANCELLED',DATEADD(DAY,-5,@now)),
    (29,19,'CANCELLED',DATEADD(DAY,-5,@now)),
    (30,19,'CANCELLED',DATEADD(DAY,-5,@now)),

    -- CANCELLED 20

    (31,20,'CANCELLED',DATEADD(DAY,-5,@now)),
    (32,20,'CANCELLED',DATEADD(DAY,-5,@now)),
    (33,20,'CANCELLED',DATEADD(DAY,-5,@now));


-- ========================================================
-- 9. MEMBERSHIPS
--
-- ACTIVE: 20
-- PENDING: 5
-- EXPIRED: 5
-- CANCELLED: 5
-- TOTAL: 35
-- ========================================================

INSERT INTO dbo.memberships (
    user_id,plan_id,branch_id,
    start_date,end_date,status,
    created_at,updated_at
)
VALUES

    -- ACTIVE

    (16,2,1,DATEADD(DAY,-2,@today),DATEADD(DAY,28,@today),
     'ACTIVE',DATEADD(DAY,-2,@now),@now),

    (17,3,2,DATEADD(DAY,-3,@today),DATEADD(DAY,27,@today),
     'ACTIVE',DATEADD(DAY,-3,@now),@now),

    (18,4,3,DATEADD(DAY,-10,@today),DATEADD(DAY,80,@today),
     'ACTIVE',DATEADD(DAY,-10,@now),@now),

    (19,5,4,DATEADD(DAY,-20,@today),DATEADD(DAY,160,@today),
     'ACTIVE',DATEADD(DAY,-20,@now),@now),

    (20,6,5,DATEADD(DAY,-30,@today),DATEADD(DAY,335,@today),
     'ACTIVE',DATEADD(DAY,-30,@now),@now),

    (21,2,6,DATEADD(DAY,-5,@today),DATEADD(DAY,25,@today),
     'ACTIVE',DATEADD(DAY,-5,@now),@now),

    (22,4,7,DATEADD(DAY,-15,@today),DATEADD(DAY,75,@today),
     'ACTIVE',DATEADD(DAY,-15,@now),@now),

    (23,3,1,DATEADD(DAY,-7,@today),DATEADD(DAY,23,@today),
     'ACTIVE',DATEADD(DAY,-7,@now),@now),

    (24,5,2,DATEADD(DAY,-40,@today),DATEADD(DAY,140,@today),
     'ACTIVE',DATEADD(DAY,-40,@now),@now),

    (25,6,3,DATEADD(DAY,-60,@today),DATEADD(DAY,305,@today),
     'ACTIVE',DATEADD(DAY,-60,@now),@now),

    (26,2,4,DATEADD(DAY,-1,@today),DATEADD(DAY,29,@today),
     'ACTIVE',DATEADD(DAY,-1,@now),@now),

    (27,3,5,DATEADD(DAY,-4,@today),DATEADD(DAY,26,@today),
     'ACTIVE',DATEADD(DAY,-4,@now),@now),

    (28,4,6,DATEADD(DAY,-12,@today),DATEADD(DAY,78,@today),
     'ACTIVE',DATEADD(DAY,-12,@now),@now),

    (29,5,7,DATEADD(DAY,-25,@today),DATEADD(DAY,155,@today),
     'ACTIVE',DATEADD(DAY,-25,@now),@now),

    (30,6,1,DATEADD(DAY,-50,@today),DATEADD(DAY,315,@today),
     'ACTIVE',DATEADD(DAY,-50,@now),@now),

    (31,2,2,DATEADD(DAY,-8,@today),DATEADD(DAY,22,@today),
     'ACTIVE',DATEADD(DAY,-8,@now),@now),

    (32,3,3,DATEADD(DAY,-9,@today),DATEADD(DAY,21,@today),
     'ACTIVE',DATEADD(DAY,-9,@now),@now),

    (33,4,4,DATEADD(DAY,-18,@today),DATEADD(DAY,72,@today),
     'ACTIVE',DATEADD(DAY,-18,@now),@now),

    (34,5,5,DATEADD(DAY,-35,@today),DATEADD(DAY,145,@today),
     'ACTIVE',DATEADD(DAY,-35,@now),@now),

    (35,6,6,DATEADD(DAY,-70,@today),DATEADD(DAY,295,@today),
     'ACTIVE',DATEADD(DAY,-70,@now),@now),

    -- PENDING

    (36,2,1,NULL,NULL,'PENDING',DATEADD(HOUR,-2,@now),@now),
    (37,3,2,NULL,NULL,'PENDING',DATEADD(HOUR,-3,@now),@now),
    (38,4,3,NULL,NULL,'PENDING',DATEADD(HOUR,-4,@now),@now),
    (39,5,4,NULL,NULL,'PENDING',DATEADD(HOUR,-5,@now),@now),
    (40,6,5,NULL,NULL,'PENDING',DATEADD(HOUR,-6,@now),@now),

    -- EXPIRED

    (41,2,1,DATEADD(DAY,-60,@today),DATEADD(DAY,-30,@today),
     'EXPIRED',DATEADD(DAY,-61,@now),@now),

    (42,3,2,DATEADD(DAY,-50,@today),DATEADD(DAY,-20,@today),
     'EXPIRED',DATEADD(DAY,-51,@now),@now),

    (43,4,3,DATEADD(DAY,-120,@today),DATEADD(DAY,-30,@today),
     'EXPIRED',DATEADD(DAY,-121,@now),@now),

    (44,5,4,DATEADD(DAY,-200,@today),DATEADD(DAY,-20,@today),
     'EXPIRED',DATEADD(DAY,-201,@now),@now),

    (45,6,5,DATEADD(DAY,-400,@today),DATEADD(DAY,-35,@today),
     'EXPIRED',DATEADD(DAY,-401,@now),@now),

    -- CANCELLED

    (46,2,1,NULL,NULL,'CANCELLED',DATEADD(DAY,-12,@now),@now),
    (47,3,2,NULL,NULL,'CANCELLED',DATEADD(DAY,-11,@now),@now),
    (48,4,3,NULL,NULL,'CANCELLED',DATEADD(DAY,-10,@now),@now),
    (49,5,4,NULL,NULL,'CANCELLED',DATEADD(DAY,-9,@now),@now),
    (50,6,5,NULL,NULL,'CANCELLED',DATEADD(DAY,-8,@now),@now);


-- ========================================================
-- 10. PAYMENTS
--
-- PAID: 25
-- PENDING: 5
-- REFUNDED: 5
-- FAILED: 5
-- TOTAL: 40
-- ========================================================

INSERT INTO dbo.payments (
    user_id,membership_id,amount,
    method,status,transaction_code,
    created_at,paid_at
)
VALUES

    -- ACTIVE MEMBERSHIP PAYMENTS

    (16,1,399000,'CASH','PAID','CASH-SEED-001',
     DATEADD(DAY,-2,@now),DATEADD(DAY,-2,@now)),

    (17,2,299000,'VNPAY','PAID','VNPAY-SEED-002',
     DATEADD(DAY,-3,@now),DATEADD(DAY,-3,@now)),

    (18,3,999000,'CASH','PAID','CASH-SEED-003',
     DATEADD(DAY,-10,@now),DATEADD(DAY,-10,@now)),

    (19,4,1799000,'VNPAY','PAID','VNPAY-SEED-004',
     DATEADD(DAY,-20,@now),DATEADD(DAY,-20,@now)),

    (20,5,2999000,'CASH','PAID','CASH-SEED-005',
     DATEADD(DAY,-30,@now),DATEADD(DAY,-30,@now)),

    (21,6,399000,'VNPAY','PAID','VNPAY-SEED-006',
     DATEADD(DAY,-5,@now),DATEADD(DAY,-5,@now)),

    (22,7,999000,'CASH','PAID','CASH-SEED-007',
     DATEADD(DAY,-15,@now),DATEADD(DAY,-15,@now)),

    (23,8,299000,'VNPAY','PAID','VNPAY-SEED-008',
     DATEADD(DAY,-7,@now),DATEADD(DAY,-7,@now)),

    (24,9,1799000,'CASH','PAID','CASH-SEED-009',
     DATEADD(DAY,-40,@now),DATEADD(DAY,-40,@now)),

    (25,10,2999000,'VNPAY','PAID','VNPAY-SEED-010',
     DATEADD(DAY,-60,@now),DATEADD(DAY,-60,@now)),

    (26,11,399000,'CASH','PAID','CASH-SEED-011',
     DATEADD(DAY,-1,@now),DATEADD(DAY,-1,@now)),

    (27,12,299000,'VNPAY','PAID','VNPAY-SEED-012',
     DATEADD(DAY,-4,@now),DATEADD(DAY,-4,@now)),

    (28,13,999000,'CASH','PAID','CASH-SEED-013',
     DATEADD(DAY,-12,@now),DATEADD(DAY,-12,@now)),

    (29,14,1799000,'VNPAY','PAID','VNPAY-SEED-014',
     DATEADD(DAY,-25,@now),DATEADD(DAY,-25,@now)),

    (30,15,2999000,'CASH','PAID','CASH-SEED-015',
     DATEADD(DAY,-50,@now),DATEADD(DAY,-50,@now)),

    (31,16,399000,'VNPAY','PAID','VNPAY-SEED-016',
     DATEADD(DAY,-8,@now),DATEADD(DAY,-8,@now)),

    (32,17,299000,'CASH','PAID','CASH-SEED-017',
     DATEADD(DAY,-9,@now),DATEADD(DAY,-9,@now)),

    (33,18,999000,'VNPAY','PAID','VNPAY-SEED-018',
     DATEADD(DAY,-18,@now),DATEADD(DAY,-18,@now)),

    (34,19,1799000,'CASH','PAID','CASH-SEED-019',
     DATEADD(DAY,-35,@now),DATEADD(DAY,-35,@now)),

    (35,20,2999000,'VNPAY','PAID','VNPAY-SEED-020',
     DATEADD(DAY,-70,@now),DATEADD(DAY,-70,@now)),

    -- PENDING CASH

    (36,21,399000,'CASH','PENDING',NULL,DATEADD(HOUR,-2,@now),NULL),
    (37,22,299000,'CASH','PENDING',NULL,DATEADD(HOUR,-3,@now),NULL),
    (38,23,999000,'CASH','PENDING',NULL,DATEADD(HOUR,-4,@now),NULL),
    (39,24,1799000,'CASH','PENDING',NULL,DATEADD(HOUR,-5,@now),NULL),
    (40,25,2999000,'CASH','PENDING',NULL,DATEADD(HOUR,-6,@now),NULL),

    -- EXPIRED MEMBERSHIP HISTORY

    (41,26,399000,'CASH','PAID','CASH-SEED-026',
     DATEADD(DAY,-60,@now),DATEADD(DAY,-60,@now)),

    (42,27,299000,'VNPAY','PAID','VNPAY-SEED-027',
     DATEADD(DAY,-50,@now),DATEADD(DAY,-50,@now)),

    (43,28,999000,'CASH','PAID','CASH-SEED-028',
     DATEADD(DAY,-120,@now),DATEADD(DAY,-120,@now)),

    (44,29,1799000,'VNPAY','PAID','VNPAY-SEED-029',
     DATEADD(DAY,-200,@now),DATEADD(DAY,-200,@now)),

    (45,30,2999000,'CASH','PAID','CASH-SEED-030',
     DATEADD(DAY,-400,@now),DATEADD(DAY,-400,@now)),

    -- REFUNDED

    (46,31,399000,'VNPAY','REFUNDED','REFUND-031',
     DATEADD(DAY,-12,@now),DATEADD(DAY,-11,@now)),

    (47,32,299000,'VNPAY','REFUNDED','REFUND-032',
     DATEADD(DAY,-11,@now),DATEADD(DAY,-10,@now)),

    (48,33,999000,'CASH','REFUNDED','REFUND-033',
     DATEADD(DAY,-10,@now),DATEADD(DAY,-9,@now)),

    (49,34,1799000,'VNPAY','REFUNDED','REFUND-034',
     DATEADD(DAY,-9,@now),DATEADD(DAY,-8,@now)),

    (50,35,2999000,'CASH','REFUNDED','REFUND-035',
     DATEADD(DAY,-8,@now),DATEADD(DAY,-7,@now)),

    -- FAILED VNPAY ATTEMPTS

    (36,21,399000,'VNPAY','FAILED','VNPAY-FAILED-001',
     DATEADD(DAY,-1,@now),NULL),

    (37,22,299000,'VNPAY','FAILED','VNPAY-FAILED-002',
     DATEADD(DAY,-1,@now),NULL),

    (38,23,999000,'VNPAY','FAILED','VNPAY-FAILED-003',
     DATEADD(DAY,-1,@now),NULL),

    (39,24,1799000,'VNPAY','FAILED','VNPAY-FAILED-004',
     DATEADD(DAY,-1,@now),NULL),

    (40,25,2999000,'VNPAY','FAILED','VNPAY-FAILED-005',
     DATEADD(DAY,-1,@now),NULL);


-- ========================================================
-- 11. REVIEWS
-- ========================================================

INSERT INTO dbo.reviews (
    user_id,branch_id,trainer_id,
    rating,comment,visible,
    created_at,updated_at
)
VALUES

    (16,1,1,5,N'Huấn luyện viên hướng dẫn rất tận tình.',
     1,DATEADD(DAY,-20,@now),@now),

    (17,2,2,4,N'Phòng tập sạch sẽ và trang thiết bị tốt.',
     1,DATEADD(DAY,-19,@now),@now),

    (18,3,3,5,N'Lớp tập sức mạnh rất hiệu quả.',
     1,DATEADD(DAY,-18,@now),@now),

    (19,4,4,4,N'Boxing vui và nhiều năng lượng.',
     1,DATEADD(DAY,-17,@now),@now),

    (20,5,5,5,N'Zumba rất sôi động, sẽ tiếp tục tham gia.',
     1,DATEADD(DAY,-16,@now),@now),

    (21,6,6,4,N'Pilates phù hợp với người mới.',
     1,DATEADD(DAY,-15,@now),@now),

    (22,7,7,5,N'CrossFit giúp cải thiện sức bền rõ rệt.',
     1,DATEADD(DAY,-14,@now),@now),

    (23,1,1,3,N'Chất lượng lớp tốt nhưng giờ tập chưa linh hoạt.',
     1,DATEADD(DAY,-13,@now),@now),

    (24,2,2,2,N'Phòng tập hơi đông vào giờ cao điểm.',
     1,DATEADD(DAY,-12,@now),@now),

    (25,3,3,1,N'Cần cải thiện khâu bảo trì thiết bị.',
     0,DATEADD(DAY,-11,@now),@now),

    (26,4,4,5,N'PT rất chuyên nghiệp.',
     1,DATEADD(DAY,-10,@now),@now),

    (27,5,5,4,N'Không gian tập rộng và thoáng.',
     1,DATEADD(DAY,-9,@now),@now),

    (28,6,6,3,N'Chất lượng dịch vụ ở mức khá.',
     1,DATEADD(DAY,-8,@now),@now),

    (29,7,7,2,N'Cần bổ sung thêm dụng cụ tập.',
     1,DATEADD(DAY,-7,@now),@now),

    (30,1,1,1,N'Trải nghiệm lần đầu chưa tốt.',
     0,DATEADD(DAY,-6,@now),@now),

    (31,2,NULL,5,N'Vị trí trung tâm rất thuận tiện.',
     1,DATEADD(DAY,-20,@now),@now),

    (32,3,NULL,4,N'Nhân viên thân thiện.',
     1,DATEADD(DAY,-19,@now),@now),

    (33,4,NULL,3,N'Có thể cải thiện hệ thống điều hòa.',
     1,DATEADD(DAY,-18,@now),@now),

    (34,5,NULL,2,N'Khu thay đồ hơi nhỏ.',
     1,DATEADD(DAY,-17,@now),@now),

    (35,6,NULL,1,N'Thời gian chờ thiết bị quá lâu.',
     0,DATEADD(DAY,-16,@now),@now);


-- ========================================================
-- 12. OTP CODES
--
-- BCrypt hash is used for state testing only.
-- These are NOT known usable six-digit OTPs.
-- ========================================================

INSERT INTO dbo.otp_codes (
    user_id,code,type,used,
    attempt_count,created_at,
    expired_at,used_at
)
VALUES

    (16,@password,'FORGOT_PASSWORD',0,0,
     DATEADD(MINUTE,-1,@now),DATEADD(MINUTE,4,@now),NULL),

    (17,@password,'FORGOT_PASSWORD',0,1,
     DATEADD(MINUTE,-2,@now),DATEADD(MINUTE,3,@now),NULL),

    (18,@password,'FORGOT_PASSWORD',0,2,
     DATEADD(MINUTE,-3,@now),DATEADD(MINUTE,2,@now),NULL),

    (19,@password,'FORGOT_PASSWORD',0,3,
     DATEADD(MINUTE,-4,@now),DATEADD(MINUTE,1,@now),NULL),

    (20,@password,'FORGOT_PASSWORD',1,5,
     DATEADD(MINUTE,-10,@now),DATEADD(MINUTE,-5,@now),
     DATEADD(MINUTE,-6,@now)),

    (21,@password,'FORGOT_PASSWORD',0,0,
     DATEADD(HOUR,-2,@now),DATEADD(MINUTE,-115,@now),NULL),

    (22,@password,'FORGOT_PASSWORD',0,1,
     DATEADD(HOUR,-3,@now),DATEADD(MINUTE,-175,@now),NULL),

    (23,@password,'FORGOT_PASSWORD',0,2,
     DATEADD(HOUR,-4,@now),DATEADD(MINUTE,-235,@now),NULL),

    (24,@password,'FORGOT_PASSWORD',0,3,
     DATEADD(HOUR,-5,@now),DATEADD(MINUTE,-295,@now),NULL),

    (25,@password,'FORGOT_PASSWORD',0,4,
     DATEADD(HOUR,-6,@now),DATEADD(MINUTE,-355,@now),NULL),

    (61,@password,'REGISTER',0,0,
     DATEADD(MINUTE,-1,@now),DATEADD(MINUTE,4,@now),NULL),

    (62,@password,'REGISTER',0,1,
     DATEADD(MINUTE,-2,@now),DATEADD(MINUTE,3,@now),NULL),

    (63,@password,'REGISTER',0,2,
     DATEADD(MINUTE,-3,@now),DATEADD(MINUTE,2,@now),NULL),

    (64,@password,'REGISTER',0,0,
     DATEADD(HOUR,-2,@now),DATEADD(MINUTE,-115,@now),NULL),

    (65,@password,'REGISTER',0,1,
     DATEADD(HOUR,-3,@now),DATEADD(MINUTE,-175,@now),NULL);


-- ========================================================
-- 13. TRAINER REQUESTS
--
-- PENDING: 10
-- ACCEPTED: 10
-- REJECTED: 10
--
-- Each member has one request only.
-- ========================================================

INSERT INTO dbo.trainer_requests (
    member_id,trainer_id,message,
    status,requested_at,responded_at
)
VALUES

    -- PENDING

    (16,1,'Toi muon hoc Yoga de cai thien do deo dai.',
     'PENDING',DATEADD(HOUR,-2,@now),NULL),

    (17,2,'Toi can lo trinh Cardio giam can.',
     'PENDING',DATEADD(HOUR,-3,@now),NULL),

    (18,3,'Toi muon tang co va tang suc manh.',
     'PENDING',DATEADD(HOUR,-4,@now),NULL),

    (19,4,'Toi muon bat dau tap Boxing.',
     'PENDING',DATEADD(HOUR,-5,@now),NULL),

    (20,5,'Toi muon duoc huong dan Zumba.',
     'PENDING',DATEADD(HOUR,-6,@now),NULL),

    (21,6,'Toi can bai tap Pilates cho nguoi moi.',
     'PENDING',DATEADD(HOUR,-7,@now),NULL),

    (22,7,'Toi muon duoc huong dan CrossFit.',
     'PENDING',DATEADD(HOUR,-8,@now),NULL),

    (23,8,'Toi can PT huong dan gym tong quat.',
     'PENDING',DATEADD(HOUR,-9,@now),NULL),

    (24,9,'Toi can chuong trinh tap ba buoi moi tuan.',
     'PENDING',DATEADD(HOUR,-10,@now),NULL),

    (25,10,'Toi muon duoc tu van tang co.',
     'PENDING',DATEADD(HOUR,-11,@now),NULL),

    -- ACCEPTED

    (26,1,'Toi can huong dan Yoga co ban.',
     'ACCEPTED',DATEADD(DAY,-10,@now),
     DATEADD(HOUR,2,DATEADD(DAY,-10,@now))),

    (27,2,'Toi muon giam mo bung.',
     'ACCEPTED',DATEADD(DAY,-11,@now),
     DATEADD(HOUR,3,DATEADD(DAY,-11,@now))),

    (28,3,'Toi muon tang suc manh co bap.',
     'ACCEPTED',DATEADD(DAY,-12,@now),
     DATEADD(HOUR,4,DATEADD(DAY,-12,@now))),

    (29,4,'Toi can hoc ky thuat Boxing.',
     'ACCEPTED',DATEADD(DAY,-13,@now),
     DATEADD(HOUR,2,DATEADD(DAY,-13,@now))),

    (30,5,'Toi muon tap Zumba de tang the luc.',
     'ACCEPTED',DATEADD(DAY,-14,@now),
     DATEADD(HOUR,3,DATEADD(DAY,-14,@now))),

    (31,6,'Toi muon cai thien do deo dai voi Pilates.',
     'ACCEPTED',DATEADD(DAY,-15,@now),
     DATEADD(HOUR,4,DATEADD(DAY,-15,@now))),

    (32,7,'Toi muon hoc CrossFit co ban.',
     'ACCEPTED',DATEADD(DAY,-16,@now),
     DATEADD(HOUR,2,DATEADD(DAY,-16,@now))),

    (33,8,'Toi muon tap gym tong quat.',
     'ACCEPTED',DATEADD(DAY,-17,@now),
     DATEADD(HOUR,3,DATEADD(DAY,-17,@now))),

    (34,9,'Toi can ke hoach tap bon buoi moi tuan.',
     'ACCEPTED',DATEADD(DAY,-18,@now),
     DATEADD(HOUR,4,DATEADD(DAY,-18,@now))),

    (35,10,'Toi muon tang can va phat trien co bap.',
     'ACCEPTED',DATEADD(DAY,-19,@now),
     DATEADD(HOUR,2,DATEADD(DAY,-19,@now))),

    -- REJECTED

    (36,2,'Toi muon dat PT vao khung gio da kin.',
     'REJECTED',DATEADD(DAY,-8,@now),
     DATEADD(HOUR,2,DATEADD(DAY,-8,@now))),

    (37,3,'Toi can PT vao buoi sang som.',
     'REJECTED',DATEADD(DAY,-9,@now),
     DATEADD(HOUR,3,DATEADD(DAY,-9,@now))),

    (38,4,'Toi muon tap Boxing ngoai gio mo cua.',
     'REJECTED',DATEADD(DAY,-10,@now),
     DATEADD(HOUR,4,DATEADD(DAY,-10,@now))),

    (39,5,'Toi muon dat lich Zumba vao ngay nghi.',
     'REJECTED',DATEADD(DAY,-11,@now),
     DATEADD(HOUR,2,DATEADD(DAY,-11,@now))),

    (40,6,'Toi can lop Pilates vao khung gio chua ho tro.',
     'REJECTED',DATEADD(DAY,-12,@now),
     DATEADD(HOUR,3,DATEADD(DAY,-12,@now))),

    (41,7,'Toi muon tap CrossFit vao thoi gian PT ban.',
     'REJECTED',DATEADD(DAY,-13,@now),
     DATEADD(HOUR,4,DATEADD(DAY,-13,@now))),

    (42,8,'Toi muon tap gym vao buoi dem.',
     'REJECTED',DATEADD(DAY,-14,@now),
     DATEADD(HOUR,2,DATEADD(DAY,-14,@now))),

    (43,9,'Toi muon huan luyen ngoai gio lam viec.',
     'REJECTED',DATEADD(DAY,-15,@now),
     DATEADD(HOUR,3,DATEADD(DAY,-15,@now))),

    (44,10,'Toi can PT vao cuoi tuan.',
     'REJECTED',DATEADD(DAY,-16,@now),
     DATEADD(HOUR,4,DATEADD(DAY,-16,@now))),

    (45,12,'Toi muon hoc Yoga vao buoi toi muon.',
     'REJECTED',DATEADD(DAY,-17,@now),
     DATEADD(HOUR,2,DATEADD(DAY,-17,@now)));


-- ========================================================
-- 14. CHECKINS
--
-- COMPLETED CHECKINS
-- ACTIVE CHECKINS
-- HISTORICAL CHECKINS
-- ========================================================

INSERT INTO dbo.checkins (
    user_id,branch_id,check_in_at,check_out_at
)
VALUES

    -- COMPLETED

    (16,1,
     DATEADD(HOUR,-5,@now),
     DATEADD(HOUR,-3,@now)),

    (17,2,
     DATEADD(HOUR,-6,@now),
     DATEADD(HOUR,-4,@now)),

    (18,3,
     DATEADD(HOUR,-7,@now),
     DATEADD(HOUR,-5,@now)),

    (19,4,
     DATEADD(HOUR,-8,@now),
     DATEADD(HOUR,-6,@now)),

    (20,5,
     DATEADD(HOUR,-9,@now),
     DATEADD(HOUR,-7,@now)),

    (21,6,
     DATEADD(HOUR,-10,@now),
     DATEADD(HOUR,-8,@now)),

    (22,7,
     DATEADD(HOUR,-11,@now),
     DATEADD(HOUR,-9,@now)),

    -- CURRENTLY CHECKED IN

    (23,1,
     DATEADD(MINUTE,-45,@now),
     NULL),

    (24,2,
     DATEADD(MINUTE,-40,@now),
     NULL),

    (25,3,
     DATEADD(MINUTE,-35,@now),
     NULL),

    (26,4,
     DATEADD(MINUTE,-30,@now),
     NULL),

    (27,5,
     DATEADD(MINUTE,-25,@now),
     NULL),

    -- HISTORICAL

    (28,6,
     DATEADD(DAY,-2,@now),
     DATEADD(MINUTE,90,DATEADD(DAY,-2,@now))),

    (29,7,
     DATEADD(DAY,-3,@now),
     DATEADD(MINUTE,120,DATEADD(DAY,-3,@now))),

    (30,1,
     DATEADD(DAY,-4,@now),
     DATEADD(MINUTE,75,DATEADD(DAY,-4,@now))),

    (31,2,
     DATEADD(DAY,-5,@now),
     DATEADD(MINUTE,100,DATEADD(DAY,-5,@now))),

    (32,3,
     DATEADD(DAY,-6,@now),
     DATEADD(MINUTE,110,DATEADD(DAY,-6,@now))),

    (33,4,
     DATEADD(DAY,-7,@now),
     DATEADD(MINUTE,80,DATEADD(DAY,-7,@now))),

    (34,5,
     DATEADD(DAY,-8,@now),
     DATEADD(MINUTE,95,DATEADD(DAY,-8,@now))),

    (35,6,
     DATEADD(DAY,-9,@now),
     DATEADD(MINUTE,105,DATEADD(DAY,-9,@now)));


-- ========================================================
-- 15. CONVERSATIONS
--
-- Only members with ACCEPTED trainer requests.
-- ========================================================

INSERT INTO dbo.conversations (
    member_id,trainer_id,created_at,updated_at
)
VALUES

    (26,1,DATEADD(DAY,-9,@now),@now),
    (27,2,DATEADD(DAY,-10,@now),@now),
    (28,3,DATEADD(DAY,-11,@now),@now),
    (29,4,DATEADD(DAY,-12,@now),@now),
    (30,5,DATEADD(DAY,-13,@now),@now),
    (31,6,DATEADD(DAY,-14,@now),@now),
    (32,7,DATEADD(DAY,-15,@now),@now),
    (33,8,DATEADD(DAY,-16,@now),@now),
    (34,9,DATEADD(DAY,-17,@now),@now),
    (35,10,DATEADD(DAY,-18,@now),@now);


-- ========================================================
-- 16. MESSAGES
--
-- Multiple messages per conversation.
-- Sender must be member or trainer account.
-- ========================================================

INSERT INTO dbo.messages (
    conversation_id,sender_id,
    content,sent_at,read_at
)
VALUES

    -- CONVERSATION 1

    (1,26,'Chao PT, toi muon bat dau tap Yoga.',
     DATEADD(DAY,-8,@now),DATEADD(DAY,-8,@now)),

    (1,4,'Chao ban, chung ta se bat dau voi Yoga co ban.',
     DATEADD(HOUR,1,DATEADD(DAY,-8,@now)),
     DATEADD(HOUR,2,DATEADD(DAY,-8,@now))),

    (1,26,'Cam on PT, toi rat mong cho buoi tap.',
     DATEADD(HOUR,3,DATEADD(DAY,-8,@now)),NULL),

    -- CONVERSATION 2

    (2,27,'Toi muon giam mo va cai thien suc ben.',
     DATEADD(DAY,-9,@now),DATEADD(DAY,-9,@now)),

    (2,5,'Chung ta se ket hop Cardio va HIIT.',
     DATEADD(HOUR,1,DATEADD(DAY,-9,@now)),
     DATEADD(HOUR,2,DATEADD(DAY,-9,@now))),

    (2,27,'Toi co the tap ba buoi moi tuan.',
     DATEADD(HOUR,3,DATEADD(DAY,-9,@now)),NULL),

    -- CONVERSATION 3

    (3,28,'Toi muon tang co bap.',
     DATEADD(DAY,-10,@now),DATEADD(DAY,-10,@now)),

    (3,6,'Toi se thiet ke lo trinh Strength Training.',
     DATEADD(HOUR,1,DATEADD(DAY,-10,@now)),
     DATEADD(HOUR,2,DATEADD(DAY,-10,@now))),

    -- CONVERSATION 4

    (4,29,'Toi chua tung tap Boxing.',
     DATEADD(DAY,-11,@now),DATEADD(DAY,-11,@now)),

    (4,7,'Chung ta se hoc ky thuat co ban truoc.',
     DATEADD(HOUR,1,DATEADD(DAY,-11,@now)),NULL),

    -- CONVERSATION 5

    (5,30,'Toi muon tap Zumba de giam can.',
     DATEADD(DAY,-12,@now),DATEADD(DAY,-12,@now)),

    (5,8,'Zumba rat phu hop voi muc tieu cua ban.',
     DATEADD(HOUR,1,DATEADD(DAY,-12,@now)),NULL),

    -- CONVERSATION 6

    (6,31,'Toi muon tap Pilates cho co loi.',
     DATEADD(DAY,-13,@now),DATEADD(DAY,-13,@now)),

    (6,9,'Toi se huong dan ban cac dong tac co ban.',
     DATEADD(HOUR,1,DATEADD(DAY,-13,@now)),NULL),

    -- CONVERSATION 7

    (7,32,'Toi muon hoc CrossFit.',
     DATEADD(DAY,-14,@now),DATEADD(DAY,-14,@now)),

    (7,10,'Chung ta se kiem tra the luc truoc.',
     DATEADD(HOUR,1,DATEADD(DAY,-14,@now)),NULL),

    -- CONVERSATION 8

    (8,33,'Toi can PT huong dan gym tong quat.',
     DATEADD(DAY,-15,@now),DATEADD(DAY,-15,@now)),

    (8,11,'Toi se ho tro ban lap ke hoach tap.',
     DATEADD(HOUR,1,DATEADD(DAY,-15,@now)),NULL),

    -- CONVERSATION 9

    (9,34,'Toi muon tap bon buoi moi tuan.',
     DATEADD(DAY,-16,@now),DATEADD(DAY,-16,@now)),

    (9,12,'Toi se sap xep lich tap phu hop.',
     DATEADD(HOUR,1,DATEADD(DAY,-16,@now)),NULL),

    -- CONVERSATION 10

    (10,35,'Toi muon tang can va tang co.',
     DATEADD(DAY,-17,@now),DATEADD(DAY,-17,@now)),

    (10,13,'Chung ta se ket hop tap luyen va dinh duong.',
     DATEADD(HOUR,1,DATEADD(DAY,-17,@now)),NULL);


-- ========================================================
-- 17. PERSONAL TRAINING SCHEDULES
--
-- Only ACCEPTED member-trainer pairs.
-- ACTIVE and INACTIVE schedules.
-- ========================================================

INSERT INTO dbo.personal_training_schedules (
    member_id,trainer_id,
    schedule_date,start_time,end_time,
    title,note,active,
    created_at,updated_at
)
VALUES

    (26,1,DATEADD(DAY,1,@today),'08:00','09:00',
     'Yoga ca nhan',
     'Khoi dong va Yoga co ban.',
     1,@now,@now),

    (27,2,DATEADD(DAY,1,@today),'09:00','10:00',
     'Cardio giam mo',
     'Tap Cardio va theo doi nhip tim.',
     1,@now,@now),

    (28,3,DATEADD(DAY,1,@today),'10:00','11:00',
     'Strength Training',
     'Tap nguc, vai va tay.',
     1,@now,@now),

    (29,4,DATEADD(DAY,1,@today),'15:00','16:00',
     'Boxing Basic',
     'Ky thuat dam va di chuyen.',
     1,@now,@now),

    (30,5,DATEADD(DAY,2,@today),'08:00','09:00',
     'Zumba Personal',
     'Tap theo nhac va cai thien the luc.',
     1,@now,@now),

    (31,6,DATEADD(DAY,2,@today),'09:00','10:00',
     'Pilates Core',
     'Tap co loi va do deo dai.',
     1,@now,@now),

    (32,7,DATEADD(DAY,2,@today),'10:00','11:00',
     'CrossFit Foundation',
     'Ky thuat CrossFit co ban.',
     1,@now,@now),

    (33,8,DATEADD(DAY,2,@today),'15:00','16:00',
     'General Fitness',
     'Danh gia the luc va lap lo trinh.',
     1,@now,@now),

    (34,9,DATEADD(DAY,3,@today),'08:00','09:00',
     'Fitness Coaching',
     'Tap luyen toan than.',
     1,@now,@now),

    (35,10,DATEADD(DAY,3,@today),'09:00','10:00',
     'Muscle Gain',
     'Tang co va cai thien suc manh.',
     1,@now,@now),

    -- COMPLETED HISTORICAL SESSIONS

    (26,1,DATEADD(DAY,-5,@today),'08:00','09:00',
     'Yoga Foundation',
     'Buoi tap dau tien da hoan thanh.',
     0,DATEADD(DAY,-10,@now),@now),

    (27,2,DATEADD(DAY,-4,@today),'09:00','10:00',
     'Cardio Foundation',
     'Buoi tap Cardio da hoan thanh.',
     0,DATEADD(DAY,-9,@now),@now),

    (28,3,DATEADD(DAY,-3,@today),'10:00','11:00',
     'Strength Foundation',
     'Buoi tap suc manh da hoan thanh.',
     0,DATEADD(DAY,-8,@now),@now),

    (29,4,DATEADD(DAY,-2,@today),'15:00','16:00',
     'Boxing Foundation',
     'Buoi tap Boxing da hoan thanh.',
     0,DATEADD(DAY,-7,@now),@now),

    (30,5,DATEADD(DAY,-1,@today),'08:00','09:00',
     'Zumba Foundation',
     'Buoi tap Zumba da hoan thanh.',
     0,DATEADD(DAY,-6,@now),@now);


-- ========================================================
-- 18. NOTIFICATIONS
--
-- ALL 6 ENUM TYPES
-- READ AND UNREAD
-- ========================================================

INSERT INTO dbo.notifications (
    user_id,type,title,content,
    related_id,is_read,read_at,created_at
)
VALUES

    -- TRAINER REQUEST SENT

    (4,'TRAINER_REQUEST_SENT',
     'Yeu cau PT moi',
     'Ban co yeu cau huan luyen moi tu Member An.',
     1,0,NULL,DATEADD(HOUR,-2,@now)),

    (5,'TRAINER_REQUEST_SENT',
     'Yeu cau PT moi',
     'Ban co yeu cau huan luyen moi tu Member Binh.',
     2,0,NULL,DATEADD(HOUR,-3,@now)),

    (6,'TRAINER_REQUEST_SENT',
     'Yeu cau PT moi',
     'Ban co yeu cau huan luyen moi tu Member Chi.',
     3,1,DATEADD(HOUR,-2,@now),DATEADD(HOUR,-4,@now)),

    -- ACCEPTED

    (26,'TRAINER_REQUEST_ACCEPTED',
     'Yeu cau PT da duoc chap nhan',
     'PT Minh da chap nhan yeu cau cua ban.',
     11,1,DATEADD(DAY,-9,@now),DATEADD(DAY,-10,@now)),

    (27,'TRAINER_REQUEST_ACCEPTED',
     'Yeu cau PT da duoc chap nhan',
     'PT Lan da chap nhan yeu cau cua ban.',
     12,0,NULL,DATEADD(DAY,-10,@now)),

    (28,'TRAINER_REQUEST_ACCEPTED',
     'Yeu cau PT da duoc chap nhan',
     'PT Hung da chap nhan yeu cau cua ban.',
     13,1,DATEADD(DAY,-11,@now),DATEADD(DAY,-12,@now)),

    -- REJECTED

    (36,'TRAINER_REQUEST_REJECTED',
     'Yeu cau PT bi tu choi',
     'PT khong the nhan lich theo thoi gian yeu cau.',
     21,0,NULL,DATEADD(DAY,-8,@now)),

    (37,'TRAINER_REQUEST_REJECTED',
     'Yeu cau PT bi tu choi',
     'PT hien khong con lich phu hop.',
     22,1,DATEADD(DAY,-8,@now),DATEADD(DAY,-9,@now)),

    -- PERSONAL SCHEDULE

    (26,'PERSONAL_SCHEDULE_CREATED',
     'Lich tap ca nhan moi',
     'PT da tao lich Yoga ca nhan cho ban.',
     1,0,NULL,DATEADD(HOUR,-1,@now)),

    (27,'PERSONAL_SCHEDULE_CREATED',
     'Lich tap ca nhan moi',
     'PT da tao lich Cardio cho ban.',
     2,0,NULL,DATEADD(HOUR,-1,@now)),

    (28,'PERSONAL_SCHEDULE_CREATED',
     'Lich tap ca nhan moi',
     'PT da tao lich Strength Training cho ban.',
     3,1,DATEADD(MINUTE,-20,@now),DATEADD(HOUR,-1,@now)),

    -- CANCELLED CLASS

    (28,'CLASS_SCHEDULE_CANCELLED',
     'Lop hoc da bi huy',
     'Lop Cardio Weekend da bi huy.',
     19,0,NULL,DATEADD(DAY,-1,@now)),

    (29,'CLASS_SCHEDULE_CANCELLED',
     'Lop hoc da bi huy',
     'Lop Cardio Weekend da bi huy.',
     19,1,DATEADD(HOUR,-12,@now),DATEADD(DAY,-1,@now)),

    (31,'CLASS_SCHEDULE_CANCELLED',
     'Lop hoc da bi huy',
     'Lop Boxing Workshop da bi huy.',
     20,0,NULL,DATEADD(DAY,-1,@now)),

    -- MESSAGE RECEIVED

    (26,'MESSAGE_RECEIVED',
     'Tin nhan moi tu PT',
     'PT Minh da gui tin nhan cho ban.',
     2,0,NULL,DATEADD(DAY,-8,@now)),

    (27,'MESSAGE_RECEIVED',
     'Tin nhan moi tu PT',
     'PT Lan da gui tin nhan cho ban.',
     5,1,DATEADD(DAY,-8,@now),DATEADD(DAY,-9,@now)),

    (28,'MESSAGE_RECEIVED',
     'Tin nhan moi tu PT',
     'PT Hung da gui tin nhan cho ban.',
     8,0,NULL,DATEADD(DAY,-10,@now)),

    (29,'MESSAGE_RECEIVED',
     'Tin nhan moi tu PT',
     'PT Thao da gui tin nhan cho ban.',
     10,0,NULL,DATEADD(DAY,-11,@now)),

    (30,'MESSAGE_RECEIVED',
     'Tin nhan moi tu PT',
     'PT Khoa da gui tin nhan cho ban.',
     12,1,DATEADD(DAY,-11,@now),DATEADD(DAY,-12,@now)),

    (31,'MESSAGE_RECEIVED',
     'Tin nhan moi tu PT',
     'PT Mai da gui tin nhan cho ban.',
     14,0,NULL,DATEADD(DAY,-13,@now));


-- ========================================================
-- 19. PROMOTIONS
--
-- ACTIVE
-- INACTIVE
-- EXPIRED
-- FUTURE
-- OUT OF STOCK
-- ========================================================

INSERT INTO dbo.promotions (
    code,discount_percent,quantity,
    start_date,end_date,active,created_at
)
VALUES

    ('WELCOME10',10.00,100,
     DATEADD(DAY,-10,@today),DATEADD(DAY,30,@today),
     1,@now),

    ('STUDENT15',15.00,80,
     DATEADD(DAY,-5,@today),DATEADD(DAY,60,@today),
     1,@now),

    ('FITNESS20',20.00,50,
     DATEADD(DAY,-3,@today),DATEADD(DAY,20,@today),
     1,@now),

    ('VIP25',25.00,20,
     DATEADD(DAY,-1,@today),DATEADD(DAY,15,@today),
     1,@now),

    ('WEEKEND5',5.00,200,
     DATEADD(DAY,-7,@today),DATEADD(DAY,7,@today),
     1,@now),

    ('SUMMER30',30.00,0,
     DATEADD(DAY,-30,@today),DATEADD(DAY,10,@today),
     1,@now),

    ('EXPIRED10',10.00,20,
     DATEADD(DAY,-60,@today),DATEADD(DAY,-30,@today),
     0,@now),

    ('EXPIRED20',20.00,10,
     DATEADD(DAY,-90,@today),DATEADD(DAY,-15,@today),
     0,@now),

    ('FUTURE15',15.00,100,
     DATEADD(DAY,10,@today),DATEADD(DAY,40,@today),
     1,@now),

    ('FUTURE25',25.00,50,
     DATEADD(DAY,20,@today),DATEADD(DAY,50,@today),
     1,@now),

    ('DISABLED10',10.00,100,
     DATEADD(DAY,-5,@today),DATEADD(DAY,30,@today),
     0,@now),

    ('DISABLED30',30.00,30,
     DATEADD(DAY,-5,@today),DATEADD(DAY,30,@today),
     0,@now);


-- ========================================================
-- DATA VALIDATION
-- ========================================================

-- 1. USERS

IF (SELECT COUNT(*) FROM dbo.users) <> 65
        THROW 50101, 'Expected 65 users.', 1;

IF EXISTS (
        SELECT 1
        FROM dbo.users
        WHERE email NOT LIKE '%@gmail.com'
    )
        THROW 50102, 'Invalid sample email domain.', 1;

    -- 2. FULL CLASS CAPACITY

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
        THROW 50103, 'FULL class capacity mismatch.', 1;

    -- 3. OPEN CLASS CAPACITY

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
        THROW 50104, 'OPEN class has no seats.', 1;

    -- 4. CANCELLED CLASSES

    IF EXISTS (
        SELECT 1
        FROM dbo.fitness_classes fc
        JOIN dbo.class_schedules cs
            ON cs.fitness_class_id = fc.id
        WHERE fc.status = 'CANCELLED'
          AND cs.active = 1
    )
        THROW 50105, 'CANCELLED class has active schedule.', 1;

    -- 5. ACTIVE MEMBERSHIP DATES

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
        THROW 50106, 'Invalid ACTIVE membership.', 1;

    -- 6. PENDING MEMBERSHIP DATES

    IF EXISTS (
        SELECT 1
        FROM dbo.memberships
        WHERE status = 'PENDING'
          AND (
              start_date IS NOT NULL
              OR end_date IS NOT NULL
          )
    )
        THROW 50107, 'Invalid PENDING membership.', 1;

    -- 7. PAYMENT OWNER

    IF EXISTS (
        SELECT 1
        FROM dbo.payments p
        JOIN dbo.memberships m
            ON m.id = p.membership_id
        WHERE p.user_id <> m.user_id
    )
        THROW 50108, 'Payment owner mismatch.', 1;

    -- 8. PAID PAYMENTS

    IF EXISTS (
        SELECT 1
        FROM dbo.payments
        WHERE status = 'PAID'
          AND paid_at IS NULL
    )
        THROW 50109, 'PAID payment missing paid_at.', 1;

    -- 9. TRAINER REQUEST TIMESTAMPS

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
        THROW 50110, 'Invalid trainer request timestamps.', 1;

    -- 10. ONE PENDING REQUEST PER MEMBER

    IF EXISTS (
        SELECT member_id
        FROM dbo.trainer_requests
        WHERE status = 'PENDING'
        GROUP BY member_id
        HAVING COUNT(*) > 1
    )
        THROW 50111, 'Member has multiple PENDING PT requests.', 1;

    -- 11. ONE ACCEPTED PT PER MEMBER

    IF EXISTS (
        SELECT member_id
        FROM dbo.trainer_requests
        WHERE status = 'ACCEPTED'
        GROUP BY member_id
        HAVING COUNT(*) > 1
    )
        THROW 50112, 'Member has multiple ACCEPTED PT requests.', 1;

    -- 12. PERSONAL SCHEDULE OWNERSHIP

    IF EXISTS (
        SELECT 1
        FROM dbo.personal_training_schedules ps
        WHERE NOT EXISTS (
            SELECT 1
            FROM dbo.trainer_requests tr
            WHERE tr.member_id = ps.member_id
              AND tr.trainer_id = ps.trainer_id
              AND tr.status = 'ACCEPTED'
        )
    )
        THROW 50113, 'Personal schedule without accepted PT.', 1;

    -- 13. CONVERSATION OWNERSHIP

    IF EXISTS (
        SELECT 1
        FROM dbo.conversations c
        WHERE NOT EXISTS (
            SELECT 1
            FROM dbo.trainer_requests tr
            WHERE tr.member_id = c.member_id
              AND tr.trainer_id = c.trainer_id
              AND tr.status = 'ACCEPTED'
        )
    )
        THROW 50114, 'Conversation without accepted PT.', 1;

    -- 14. MESSAGE SENDER

    IF EXISTS (
        SELECT 1
        FROM dbo.messages m
        JOIN dbo.conversations c
            ON c.id = m.conversation_id
        JOIN dbo.trainers t
            ON t.id = c.trainer_id
        WHERE m.sender_id <> c.member_id
          AND m.sender_id <> t.user_id
    )
        THROW 50115, 'Invalid message sender.', 1;

    -- 15. CHECKIN TIMESTAMPS

    IF EXISTS (
        SELECT 1
        FROM dbo.checkins
        WHERE check_out_at IS NOT NULL
          AND check_out_at <= check_in_at
    )
        THROW 50116, 'Invalid checkin timestamps.', 1;

    -- 16. PERSONAL TRAINING TIME

    IF EXISTS (
        SELECT 1
        FROM dbo.personal_training_schedules
        WHERE start_time >= end_time
    )
        THROW 50117, 'Invalid personal training time.', 1;

    -- 17. NOTIFICATION READ STATUS

    IF EXISTS (
        SELECT 1
        FROM dbo.notifications
        WHERE
            (is_read = 0 AND read_at IS NOT NULL)
            OR
            (is_read = 1 AND read_at IS NULL)
    )
        THROW 50118, 'Invalid notification read status.', 1;

    -- 18. PROMOTION VALUES

    IF EXISTS (
        SELECT 1
        FROM dbo.promotions
        WHERE discount_percent <= 0
           OR discount_percent > 100
           OR quantity < 0
           OR start_date > end_date
    )
        THROW 50119, 'Invalid promotion.', 1;

    -- 19. REVIEW RATINGS

    IF EXISTS (
        SELECT 1
        FROM dbo.reviews
        WHERE rating < 1 OR rating > 5
    )
        THROW 50120, 'Invalid review rating.', 1;

    -- 20. TRAINERS

    IF EXISTS (
        SELECT 1
        FROM dbo.trainers t
        JOIN dbo.users u
            ON u.id = t.user_id
        WHERE u.role <> 'PT'
    )
        THROW 50121, 'Trainer user must have PT role.', 1;

    -- 21. CHECKIN USERS

    IF EXISTS (
        SELECT 1
        FROM dbo.checkins c
        JOIN dbo.users u
            ON u.id = c.user_id
        WHERE u.role <> 'MEMBER'
    )
        THROW 50122, 'Checkin user must be MEMBER.', 1;

    -- 22. MEMBERSHIP USERS

    IF EXISTS (
        SELECT 1
        FROM dbo.memberships m
        JOIN dbo.users u
            ON u.id = m.user_id
        WHERE u.role <> 'MEMBER'
    )
        THROW 50123, 'Membership user must be MEMBER.', 1;

    -- 23. TRAINER REQUEST MEMBERS

    IF EXISTS (
        SELECT 1
        FROM dbo.trainer_requests tr
        JOIN dbo.users u
            ON u.id = tr.member_id
        WHERE u.role <> 'MEMBER'
    )
        THROW 50124, 'Trainer request owner must be MEMBER.', 1;

    -- 24. NO INACTIVE TRAINER IN ACTIVE CLASSES

    IF EXISTS (
        SELECT 1
        FROM dbo.fitness_classes fc
        JOIN dbo.trainers t
            ON t.id = fc.trainer_id
        WHERE fc.status IN ('OPEN','FULL')
          AND t.active = 0
    )
        THROW 50125, 'Active class uses inactive trainer.', 1;

    -- 25. FULL CLASS COUNT

    IF (SELECT COUNT(*)
        FROM dbo.fitness_classes
        WHERE status = 'FULL') <> 3
        THROW 50126, 'Expected 3 FULL classes.', 1;

-- 26. CASH PENDING MUST HAVE PENDING MEMBERSHIP

IF EXISTS (
        SELECT 1
        FROM dbo.payments p
        JOIN dbo.memberships m
            ON m.id = p.membership_id
        WHERE p.method = 'CASH'
          AND p.status = 'PENDING'
          AND m.status <> 'PENDING'
    )
        THROW 50127, 'Invalid pending CASH payment.', 1;

    -- 27. OTP TIMESTAMPS

    IF EXISTS (
        SELECT 1
        FROM dbo.otp_codes
        WHERE expired_at <= created_at
           OR (used = 0 AND used_at IS NOT NULL)
           OR (used = 1 AND used_at IS NULL)
           OR (used_at > expired_at)
    )
        THROW 50128, 'Invalid OTP timestamps.', 1;

COMMIT TRANSACTION;

PRINT '=============================================';
    PRINT 'UTEFIT SAMPLE DATA INSERTED SUCCESSFULLY';
    PRINT 'DATABASE: UTEFitDB';
    PRINT 'TABLES: 19';
    PRINT '=============================================';

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

SELECT 'users' AS table_name,COUNT(*) AS total FROM dbo.users
UNION ALL
SELECT 'branches',COUNT(*) FROM dbo.branches
UNION ALL
SELECT 'class_categories',COUNT(*) FROM dbo.class_categories
UNION ALL
SELECT 'membership_plans',COUNT(*) FROM dbo.membership_plans
UNION ALL
SELECT 'trainers',COUNT(*) FROM dbo.trainers
UNION ALL
SELECT 'fitness_classes',COUNT(*) FROM dbo.fitness_classes
UNION ALL
SELECT 'class_schedules',COUNT(*) FROM dbo.class_schedules
UNION ALL
SELECT 'class_registrations',COUNT(*) FROM dbo.class_registrations
UNION ALL
SELECT 'memberships',COUNT(*) FROM dbo.memberships
UNION ALL
SELECT 'payments',COUNT(*) FROM dbo.payments
UNION ALL
SELECT 'reviews',COUNT(*) FROM dbo.reviews
UNION ALL
SELECT 'otp_codes',COUNT(*) FROM dbo.otp_codes
UNION ALL
SELECT 'trainer_requests',COUNT(*) FROM dbo.trainer_requests
UNION ALL
SELECT 'checkins',COUNT(*) FROM dbo.checkins
UNION ALL
SELECT 'conversations',COUNT(*) FROM dbo.conversations
UNION ALL
SELECT 'messages',COUNT(*) FROM dbo.messages
UNION ALL
SELECT 'personal_training_schedules',COUNT(*) FROM dbo.personal_training_schedules
UNION ALL
SELECT 'notifications',COUNT(*) FROM dbo.notifications
UNION ALL
SELECT 'promotions',COUNT(*) FROM dbo.promotions;
GO


-- ============================================================
-- CHECK CLASS CAPACITY
-- ============================================================

SELECT
    fc.id,
    fc.name,
    fc.status,
    fc.capacity,
    COUNT(cr.id) AS registered_count,
    fc.capacity - COUNT(cr.id) AS available_seats

FROM dbo.fitness_classes fc

         JOIN dbo.class_schedules cs
              ON cs.fitness_class_id = fc.id

         LEFT JOIN dbo.class_registrations cr
                   ON cr.schedule_id = cs.id
                       AND cr.status = 'REGISTERED'

GROUP BY
    fc.id,
    fc.name,
    fc.status,
    fc.capacity

ORDER BY fc.id;
GO


-- ============================================================
-- CHECK MEMBER - PT RELATIONSHIPS
-- ============================================================

SELECT
    u.email AS member_email,
    CONCAT(pu.first_name,' ',pu.last_name) AS trainer_name,
    tr.status,
    tr.requested_at,
    tr.responded_at

FROM dbo.trainer_requests tr

         JOIN dbo.users u
              ON u.id = tr.member_id

         JOIN dbo.trainers t
              ON t.id = tr.trainer_id

         JOIN dbo.users pu
              ON pu.id = t.user_id

ORDER BY
    tr.status,
    u.email;
GO


-- ============================================================
-- CHECK PROMOTIONS
-- ============================================================

USE UTEFitDB;
GO

SELECT
    id,
    code,
    discount_percent,
    quantity,
    start_date,
    end_date,
    active,

    CASE
        WHEN active = 0 THEN 'INACTIVE'
        WHEN CAST(GETDATE() AS DATE) < start_date THEN 'UPCOMING'
        WHEN CAST(GETDATE() AS DATE) > end_date THEN 'EXPIRED'
        WHEN quantity = 0 THEN 'OUT_OF_STOCK'
        ELSE 'AVAILABLE'
        END AS promotion_status

FROM dbo.promotions

ORDER BY id;
GO