-- ============================================================
-- UTEFIT - COMPLETE SAMPLE DATA
-- FILE: DB_Data_2.sql
-- DATABASE: UTEFitDB
-- SQL SERVER 2022
--
-- RUN DB_Khung_1.sql BEFORE THIS FILE
--
-- TABLES: 19
-- USERS: 65
-- BRANCHES: 10
-- CLASS CATEGORIES: 10
-- MEMBERSHIP PLANS: 10
-- TRAINERS: 12
-- FITNESS CLASSES: 20
-- CLASS SCHEDULES: 20
-- CLASS REGISTRATIONS: 90
-- MEMBERSHIPS: 35
-- PAYMENTS: 40
-- REVIEWS: 20
-- OTP CODES: 15
-- TRAINER REQUESTS: 35
-- CHECKINS: 20
-- CONVERSATIONS: 10
-- MESSAGES: 20
-- PERSONAL TRAINING SCHEDULES: 20
-- NOTIFICATIONS: 30
-- PROMOTIONS: 12
--
-- NO LOOPS
-- NO AUTOMATIC SAMPLE RECORD GENERATION
-- ============================================================

USE UTEFitDB;
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY

BEGIN TRANSACTION;

DECLARE @now DATETIME2(7) = SYSDATETIME();
DECLARE @today DATE = CAST(@now AS DATE);

DECLARE @password VARCHAR(255) =
'$2a$10$u3JbAUb0xLv8wBfQwQRwv.IJin5lMfs62HdRpDtURwUG9AeZ4Y0BS';

-- ============================================================
-- DATABASE VALIDATION
-- ============================================================

IF (
SELECT COUNT(*)
FROM sys.tables
WHERE schema_id = SCHEMA_ID('dbo')
    ) <> 19
BEGIN
    THROW 50001, 'Expected exactly 19 tables.', 1;
END;

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
OR EXISTS (SELECT 1 FROM dbo.checkins)
OR EXISTS (SELECT 1 FROM dbo.conversations)
OR EXISTS (SELECT 1 FROM dbo.messages)
OR EXISTS (SELECT 1 FROM dbo.personal_training_schedules)
OR EXISTS (SELECT 1 FROM dbo.notifications)
OR EXISTS (SELECT 1 FROM dbo.promotions)
BEGIN
    THROW 50002, 'Database must be empty.', 1;
END;

-- ============================================================
-- 1. USERS - 65
-- ============================================================

INSERT INTO dbo.users (
    first_name,last_name,email,password,phone,
    role,status,enabled,token_version,
    created_at,updated_at
)
VALUES
    ('Quang','Nguyen','admin01@gmail.com',@password,'0901000001','ADMIN','ACTIVE',1,0,DATEADD(DAY,-300,@now),@now),
    ('Ha','Tran','admin02@gmail.com',@password,'0901000002','ADMIN','ACTIVE',1,0,DATEADD(DAY,-280,@now),@now),
    ('Minh','Le','admin03@gmail.com',@password,'0901000003','ADMIN','ACTIVE',1,0,DATEADD(DAY,-260,@now),@now),

    ('Minh','Tran','pt01@gmail.com',@password,'0902000001','PT','ACTIVE',1,0,DATEADD(DAY,-250,@now),@now),
    ('Lan','Le','pt02@gmail.com',@password,'0902000002','PT','ACTIVE',1,0,DATEADD(DAY,-240,@now),@now),
    ('Hung','Pham','pt03@gmail.com',@password,'0902000003','PT','ACTIVE',1,0,DATEADD(DAY,-230,@now),@now),
    ('Thao','Vo','pt04@gmail.com',@password,'0902000004','PT','ACTIVE',1,0,DATEADD(DAY,-220,@now),@now),
    ('Khoa','Nguyen','pt05@gmail.com',@password,'0902000005','PT','ACTIVE',1,0,DATEADD(DAY,-210,@now),@now),
    ('Mai','Bui','pt06@gmail.com',@password,'0902000006','PT','ACTIVE',1,0,DATEADD(DAY,-200,@now),@now),
    ('Duc','Hoang','pt07@gmail.com',@password,'0902000007','PT','ACTIVE',1,0,DATEADD(DAY,-190,@now),@now),
    ('Linh','Dang','pt08@gmail.com',@password,'0902000008','PT','ACTIVE',1,0,DATEADD(DAY,-180,@now),@now),
    ('Phong','Phan','pt09@gmail.com',@password,'0902000009','PT','ACTIVE',1,0,DATEADD(DAY,-170,@now),@now),
    ('Nhi','Ngo','pt10@gmail.com',@password,'0902000010','PT','ACTIVE',1,0,DATEADD(DAY,-160,@now),@now),
    ('Nam','Do','pt11@gmail.com',@password,'0902000011','PT','LOCKED',0,1,DATEADD(DAY,-150,@now),@now),
    ('Vy','Nguyen','pt12@gmail.com',@password,'0902000012','PT','ACTIVE',1,0,DATEADD(DAY,-140,@now),@now),

    ('An','Nguyen','member01@gmail.com',@password,'0903000001','MEMBER','ACTIVE',1,0,DATEADD(DAY,-120,@now),@now),
    ('Binh','Tran','member02@gmail.com',@password,'0903000002','MEMBER','ACTIVE',1,0,DATEADD(DAY,-119,@now),@now),
    ('Chi','Le','member03@gmail.com',@password,'0903000003','MEMBER','ACTIVE',1,0,DATEADD(DAY,-118,@now),@now),
    ('Dung','Pham','member04@gmail.com',@password,'0903000004','MEMBER','ACTIVE',1,0,DATEADD(DAY,-117,@now),@now),
    ('Giang','Vo','member05@gmail.com',@password,'0903000005','MEMBER','ACTIVE',1,0,DATEADD(DAY,-116,@now),@now),
    ('Hieu','Bui','member06@gmail.com',@password,'0903000006','MEMBER','ACTIVE',1,0,DATEADD(DAY,-115,@now),@now),
    ('Huong','Hoang','member07@gmail.com',@password,'0903000007','MEMBER','ACTIVE',1,0,DATEADD(DAY,-114,@now),@now),
    ('Khanh','Dang','member08@gmail.com',@password,'0903000008','MEMBER','ACTIVE',1,0,DATEADD(DAY,-113,@now),@now),
    ('Lam','Phan','member09@gmail.com',@password,'0903000009','MEMBER','ACTIVE',1,0,DATEADD(DAY,-112,@now),@now),
    ('Linh','Ngo','member10@gmail.com',@password,'0903000010','MEMBER','ACTIVE',1,0,DATEADD(DAY,-111,@now),@now),
    ('Long','Do','member11@gmail.com',@password,'0903000011','MEMBER','ACTIVE',1,0,DATEADD(DAY,-110,@now),@now),
    ('Mai','Nguyen','member12@gmail.com',@password,'0903000012','MEMBER','ACTIVE',1,0,DATEADD(DAY,-109,@now),@now),
    ('Minh','Tran','member13@gmail.com',@password,'0903000013','MEMBER','ACTIVE',1,0,DATEADD(DAY,-108,@now),@now),
    ('My','Le','member14@gmail.com',@password,'0903000014','MEMBER','ACTIVE',1,0,DATEADD(DAY,-107,@now),@now),
    ('Nam','Pham','member15@gmail.com',@password,'0903000015','MEMBER','ACTIVE',1,0,DATEADD(DAY,-106,@now),@now),
    ('Nga','Vo','member16@gmail.com',@password,'0903000016','MEMBER','ACTIVE',1,0,DATEADD(DAY,-105,@now),@now),
    ('Ngan','Bui','member17@gmail.com',@password,'0903000017','MEMBER','ACTIVE',1,0,DATEADD(DAY,-104,@now),@now),
    ('Nghia','Hoang','member18@gmail.com',@password,'0903000018','MEMBER','ACTIVE',1,0,DATEADD(DAY,-103,@now),@now),
    ('Ngoc','Dang','member19@gmail.com',@password,'0903000019','MEMBER','ACTIVE',1,0,DATEADD(DAY,-102,@now),@now),
    ('Nhi','Phan','member20@gmail.com',@password,'0903000020','MEMBER','ACTIVE',1,0,DATEADD(DAY,-101,@now),@now),
    ('Phat','Ngo','member21@gmail.com',@password,'0903000021','MEMBER','ACTIVE',1,0,DATEADD(DAY,-100,@now),@now),
    ('Phuc','Do','member22@gmail.com',@password,'0903000022','MEMBER','ACTIVE',1,0,DATEADD(DAY,-99,@now),@now),
    ('Phuong','Nguyen','member23@gmail.com',@password,'0903000023','MEMBER','ACTIVE',1,0,DATEADD(DAY,-98,@now),@now),
    ('Quan','Tran','member24@gmail.com',@password,'0903000024','MEMBER','ACTIVE',1,0,DATEADD(DAY,-97,@now),@now),
    ('Quynh','Le','member25@gmail.com',@password,'0903000025','MEMBER','ACTIVE',1,0,DATEADD(DAY,-96,@now),@now),
    ('Sang','Pham','member26@gmail.com',@password,'0903000026','MEMBER','ACTIVE',1,0,DATEADD(DAY,-95,@now),@now),
    ('Son','Vo','member27@gmail.com',@password,'0903000027','MEMBER','ACTIVE',1,0,DATEADD(DAY,-94,@now),@now),
    ('Tam','Bui','member28@gmail.com',@password,'0903000028','MEMBER','ACTIVE',1,0,DATEADD(DAY,-93,@now),@now),
    ('Thao','Hoang','member29@gmail.com',@password,'0903000029','MEMBER','ACTIVE',1,0,DATEADD(DAY,-92,@now),@now),
    ('Thien','Dang','member30@gmail.com',@password,'0903000030','MEMBER','ACTIVE',1,0,DATEADD(DAY,-91,@now),@now),
    ('Thu','Phan','member31@gmail.com',@password,'0903000031','MEMBER','ACTIVE',1,0,DATEADD(DAY,-90,@now),@now),
    ('Thuy','Ngo','member32@gmail.com',@password,'0903000032','MEMBER','ACTIVE',1,0,DATEADD(DAY,-89,@now),@now),
    ('Tien','Do','member33@gmail.com',@password,'0903000033','MEMBER','ACTIVE',1,0,DATEADD(DAY,-88,@now),@now),
    ('Trang','Nguyen','member34@gmail.com',@password,'0903000034','MEMBER','ACTIVE',1,0,DATEADD(DAY,-87,@now),@now),
    ('Trinh','Tran','member35@gmail.com',@password,'0903000035','MEMBER','ACTIVE',1,0,DATEADD(DAY,-86,@now),@now),
    ('Truc','Le','member36@gmail.com',@password,'0903000036','MEMBER','ACTIVE',1,0,DATEADD(DAY,-85,@now),@now),
    ('Trung','Pham','member37@gmail.com',@password,'0903000037','MEMBER','ACTIVE',1,0,DATEADD(DAY,-84,@now),@now),
    ('Tuan','Vo','member38@gmail.com',@password,'0903000038','MEMBER','ACTIVE',1,0,DATEADD(DAY,-83,@now),@now),
    ('Tung','Bui','member39@gmail.com',@password,'0903000039','MEMBER','ACTIVE',1,0,DATEADD(DAY,-82,@now),@now),
    ('Uyen','Hoang','member40@gmail.com',@password,'0903000040','MEMBER','ACTIVE',1,0,DATEADD(DAY,-81,@now),@now),

    ('Van','Dang','member41@gmail.com',@password,'0903000041','MEMBER','LOCKED',0,1,DATEADD(DAY,-80,@now),@now),
    ('Vi','Phan','member42@gmail.com',@password,'0903000042','MEMBER','LOCKED',0,1,DATEADD(DAY,-79,@now),@now),
    ('Viet','Ngo','member43@gmail.com',@password,'0903000043','MEMBER','LOCKED',0,1,DATEADD(DAY,-78,@now),@now),
    ('Vinh','Do','member44@gmail.com',@password,'0903000044','MEMBER','LOCKED',0,1,DATEADD(DAY,-77,@now),@now),
    ('Vy','Nguyen','member45@gmail.com',@password,'0903000045','MEMBER','LOCKED',0,1,DATEADD(DAY,-76,@now),@now),

    ('Xuan','Tran','member46@gmail.com',@password,'0903000046','MEMBER','UNVERIFIED',0,0,DATEADD(DAY,-5,@now),@now),
    ('Yen','Le','member47@gmail.com',@password,'0903000047','MEMBER','UNVERIFIED',0,0,DATEADD(DAY,-4,@now),@now),
    ('Bao','Pham','member48@gmail.com',@password,'0903000048','MEMBER','UNVERIFIED',0,0,DATEADD(DAY,-3,@now),@now),
    ('Cuong','Vo','member49@gmail.com',@password,'0903000049','MEMBER','UNVERIFIED',0,0,DATEADD(DAY,-2,@now),@now),
    ('Dat','Bui','member50@gmail.com',@password,'0903000050','MEMBER','UNVERIFIED',0,0,DATEADD(DAY,-1,@now),@now);

-- ============================================================
-- 2. BRANCHES - 10
-- ============================================================

INSERT INTO dbo.branches (
    name,address,phone,opening_time,closing_time,
    description,image_url,active,created_at,updated_at
)
VALUES
    ('UTEFit Thu Duc','01 Vo Van Ngan, Thu Duc, TP.HCM','02838001001','05:30','22:00','Chi nhanh chinh voi gym, Yoga va PT.','https://placehold.co/800x500?text=Thu+Duc',1,DATEADD(DAY,-300,@now),@now),
    ('UTEFit Quan 1','120 Nguyen Hue, Quan 1, TP.HCM','02838001002','06:00','22:00','Phong gym cao cap tai trung tam.','https://placehold.co/800x500?text=Quan+1',1,DATEADD(DAY,-290,@now),@now),
    ('UTEFit Binh Thanh','200 Dien Bien Phu, Binh Thanh, TP.HCM','02838001003','05:30','23:00','Chuyen tap suc manh va co bap.','https://placehold.co/800x500?text=Binh+Thanh',1,DATEADD(DAY,-280,@now),@now),
    ('UTEFit Go Vap','45 Quang Trung, Go Vap, TP.HCM','02838001004','06:00','22:00','Khu Boxing va HIIT.','https://placehold.co/800x500?text=Go+Vap',1,DATEADD(DAY,-270,@now),@now),
    ('UTEFit Quan 7','88 Nguyen Thi Thap, Quan 7, TP.HCM','02838001005','06:00','22:00','Lop tap nhom va Zumba.','https://placehold.co/800x500?text=Quan+7',1,DATEADD(DAY,-260,@now),@now),
    ('UTEFit Tan Binh','150 Cong Hoa, Tan Binh, TP.HCM','02838001006','05:30','22:00','Chuyen Pilates va Yoga.','https://placehold.co/800x500?text=Tan+Binh',1,DATEADD(DAY,-250,@now),@now),
    ('UTEFit Quan 10','300 Su Van Hanh, Quan 10, TP.HCM','02838001007','06:00','22:00','Chuyen CrossFit.','https://placehold.co/800x500?text=Quan+10',1,DATEADD(DAY,-240,@now),@now),
    ('UTEFit Quan 3','100 Vo Van Tan, Quan 3, TP.HCM','02838001008','06:00','21:00','Tam ngung de nang cap.',NULL,0,DATEADD(DAY,-230,@now),@now),
    ('UTEFit Binh Tan','50 Ten Lua, Binh Tan, TP.HCM','02838001009','06:00','21:00','Chi nhanh tam ngung.',NULL,0,DATEADD(DAY,-220,@now),@now),
    ('UTEFit Hoc Mon','20 To Ky, Hoc Mon, TP.HCM','02838001010','06:00','21:00','Dang chuan bi khai truong.',NULL,0,DATEADD(DAY,-210,@now),@now);

-- ============================================================
-- 3. CLASS CATEGORIES - 10
-- ============================================================

INSERT INTO dbo.class_categories (
    name,description,active,created_at,updated_at
)
VALUES
    ('Yoga','Yoga va do deo dai.',1,@now,@now),
    ('Cardio','Tim mach va suc ben.',1,@now,@now),
    ('Strength Training','Tap suc manh.',1,@now,@now),
    ('HIIT','Tap cuong do cao.',1,@now,@now),
    ('Pilates','Tap co loi.',1,@now,@now),
    ('Zumba','Tap theo nhac.',1,@now,@now),
    ('Boxing','Ky thuat Boxing.',1,@now,@now),
    ('CrossFit','Tap chuc nang.',1,@now,@now),
    ('Cycling','Dap xe trong nha.',1,@now,@now),
    ('Aerobic Classic','Danh muc ngung hoat dong.',0,@now,@now);

-- ============================================================
-- 4. MEMBERSHIP PLANS - 10
-- ============================================================

INSERT INTO dbo.membership_plans (
    name,description,duration_days,price,
    active,created_at,updated_at
)
VALUES
    ('Trial 7 Days','Goi thu 7 ngay.',7,99000,1,@now,@now),
    ('Basic 1 Month','Goi 30 ngay.',30,399000,1,@now,@now),
    ('Student 1 Month','Goi sinh vien.',30,299000,1,@now,@now),
    ('Standard 3 Months','Goi 90 ngay.',90,999000,1,@now,@now),
    ('Premium 6 Months','Goi 180 ngay.',180,1799000,1,@now,@now),
    ('VIP 12 Months','Goi 365 ngay.',365,2999000,1,@now,@now),
    ('Couple 3 Months','Goi cap doi.',90,1799000,1,@now,@now),
    ('Premium Plus','Goi cao cap.',365,3999000,1,@now,@now),
    ('Legacy Basic','Goi cu.',30,199000,0,@now,@now),
    ('Legacy Premium','Goi cao cap cu.',180,1499000,0,@now,@now);

-- ============================================================
-- 5. TRAINERS - 12
-- ============================================================

INSERT INTO dbo.trainers (
    user_id,branch_id,specialization,
    years_experience,bio,active,created_at,updated_at
)
VALUES
    (4,1,'Yoga, Pilates',6,'Huan luyen Yoga va Pilates.',1,@now,@now),
    (5,2,'Cardio, HIIT',5,'Chuyen gia Cardio.',1,@now,@now),
    (6,3,'Strength Training, CrossFit',8,'Chuyen gia tang co.',1,@now,@now),
    (7,4,'Boxing, HIIT',7,'Huan luyen Boxing.',1,@now,@now),
    (8,5,'Zumba, Cardio',4,'Huan luyen Zumba.',1,@now,@now),
    (9,6,'Pilates, Yoga',6,'Chuyen gia Pilates.',1,@now,@now),
    (10,7,'CrossFit, Strength Training',9,'Chuyen gia CrossFit.',1,@now,@now),
    (11,1,'General Fitness',5,'Huan luyen tong quat.',1,@now,@now),
    (12,2,'General Fitness',4,'Huong dan nguoi moi.',1,@now,@now),
    (13,3,'General Fitness',7,'Tu van tap ca nhan.',1,@now,@now),
    (14,NULL,'General Fitness',3,'PT tam ngung.',0,@now,@now),
    (15,5,'Yoga, Pilates',5,'Huan luyen Yoga.',1,@now,@now);

-- ============================================================
-- 6. FITNESS CLASSES - 20
-- ============================================================

INSERT INTO dbo.fitness_classes (
    name,description,capacity,status,
    branch_id,category_id,trainer_id,
    created_at,updated_at
)
VALUES
    ('Morning Yoga','Yoga buoi sang.',15,'OPEN',1,1,1,@now,@now),
    ('Cardio Starter','Cardio co ban.',20,'OPEN',2,2,2,@now,@now),
    ('Strength Fundamentals','Tap ta co ban.',15,'OPEN',3,3,3,@now,@now),
    ('HIIT Power','HIIT cuong do cao.',12,'OPEN',4,4,4,@now,@now),
    ('Zumba Energy','Zumba theo nhac.',20,'OPEN',5,6,5,@now,@now),
    ('Pilates Core','Pilates co loi.',15,'OPEN',6,5,6,@now,@now),
    ('CrossFit Beginner','CrossFit co ban.',15,'OPEN',7,8,7,@now,@now),
    ('Yoga Relaxation','Yoga thu gian.',18,'OPEN',1,1,1,@now,@now),
    ('Cardio Endurance','Cardio suc ben.',20,'OPEN',2,2,2,@now,@now),
    ('Muscle Building','Phat trien co bap.',15,'OPEN',3,3,3,@now,@now),
    ('Boxing Basics','Boxing co ban.',12,'OPEN',4,7,4,@now,@now),
    ('Pilates Balance','Pilates can bang.',15,'OPEN',5,5,12,@now,@now),

    ('Advanced Yoga','Yoga nang cao.',6,'FULL',1,1,1,@now,@now),
    ('HIIT Burning','HIIT dot mo.',6,'FULL',2,4,2,@now,@now),
    ('Strength Elite','Tap suc manh nang cao.',6,'FULL',3,3,3,@now,@now),

    ('Zumba Summer','Lop da hoan thanh.',20,'COMPLETED',5,6,5,@now,@now),
    ('Pilates Foundation','Lop da hoan thanh.',15,'COMPLETED',6,5,6,@now,@now),
    ('CrossFit Challenge','Lop da hoan thanh.',15,'COMPLETED',7,8,7,@now,@now),

    ('Cardio Weekend','Lop da huy.',20,'CANCELLED',2,2,2,@now,@now),
    ('Boxing Workshop','Lop da huy.',15,'CANCELLED',4,7,4,@now,@now);

-- ============================================================
-- 7. CLASS SCHEDULES - 20
-- ============================================================

INSERT INTO dbo.class_schedules (
    fitness_class_id,schedule_date,
    start_time,end_time,room,active,created_at
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

    (16,DATEADD(DAY,-20,@today),'18:00','19:00','Dance A',0,DATEADD(DAY,-30,@now)),
    (17,DATEADD(DAY,-19,@today),'07:00','08:00','Pilates A',0,DATEADD(DAY,-30,@now)),
    (18,DATEADD(DAY,-18,@today),'18:30','19:30','CrossFit A',0,DATEADD(DAY,-30,@now)),

    (19,DATEADD(DAY,6,@today),'07:30','08:30','Cardio A',0,DATEADD(DAY,-10,@now)),
    (20,DATEADD(DAY,6,@today),'17:00','18:00','Boxing A',0,DATEADD(DAY,-10,@now));

-- ============================================================
-- 8. CLASS REGISTRATIONS - EXACTLY 90
--
-- OPEN REGISTERED: 36
-- OPEN CANCELLED: 18
-- FULL REGISTERED: 18
-- COMPLETED: 12
-- CANCELLED CLASSES: 6
--
-- 36 + 18 + 18 + 12 + 6 = 90
--
-- NO DUPLICATE (USER_ID, SCHEDULE_ID)
-- ============================================================

INSERT INTO dbo.class_registrations (
    user_id,schedule_id,status,registered_at
)
VALUES

-- OPEN SCHEDULE 1 - BRANCH 1 - 6
(16,1,'REGISTERED',DATEADD(DAY,-1,@now)),
(17,1,'REGISTERED',DATEADD(DAY,-1,@now)),
(18,1,'REGISTERED',DATEADD(DAY,-1,@now)),
(19,1,'REGISTERED',DATEADD(DAY,-1,@now)),
(20,1,'REGISTERED',DATEADD(DAY,-1,@now)),
(21,1,'REGISTERED',DATEADD(DAY,-1,@now)),

-- OPEN SCHEDULE 2 - BRANCH 2 - 6
(22,2,'REGISTERED',DATEADD(DAY,-1,@now)),
(23,2,'REGISTERED',DATEADD(DAY,-1,@now)),
(24,2,'REGISTERED',DATEADD(DAY,-1,@now)),
(25,2,'REGISTERED',DATEADD(DAY,-1,@now)),
(26,2,'REGISTERED',DATEADD(DAY,-1,@now)),
(27,2,'REGISTERED',DATEADD(DAY,-1,@now)),

-- OPEN SCHEDULE 3 - BRANCH 3 - 5
(28,3,'REGISTERED',DATEADD(DAY,-1,@now)),
(29,3,'REGISTERED',DATEADD(DAY,-1,@now)),
(30,3,'REGISTERED',DATEADD(DAY,-1,@now)),
(31,3,'REGISTERED',DATEADD(DAY,-1,@now)),
(32,3,'REGISTERED',DATEADD(DAY,-1,@now)),

-- OPEN SCHEDULE 4 - BRANCH 4 - 1
(34,4,'REGISTERED',DATEADD(DAY,-1,@now)),

-- OPEN SCHEDULE 5 - BRANCH 5 - 1
(35,5,'REGISTERED',DATEADD(DAY,-1,@now)),

-- OPEN SCHEDULE 8 - BRANCH 1 - 5
(16,8,'REGISTERED',DATEADD(DAY,-1,@now)),
(17,8,'REGISTERED',DATEADD(DAY,-1,@now)),
(18,8,'REGISTERED',DATEADD(DAY,-1,@now)),
(19,8,'REGISTERED',DATEADD(DAY,-1,@now)),
(20,8,'REGISTERED',DATEADD(DAY,-1,@now)),

-- OPEN SCHEDULE 9 - BRANCH 2 - 5
(22,9,'REGISTERED',DATEADD(DAY,-1,@now)),
(23,9,'REGISTERED',DATEADD(DAY,-1,@now)),
(24,9,'REGISTERED',DATEADD(DAY,-1,@now)),
(25,9,'REGISTERED',DATEADD(DAY,-1,@now)),
(26,9,'REGISTERED',DATEADD(DAY,-1,@now)),

-- OPEN SCHEDULE 10 - BRANCH 3 - 5
(28,10,'REGISTERED',DATEADD(DAY,-1,@now)),
(29,10,'REGISTERED',DATEADD(DAY,-1,@now)),
(30,10,'REGISTERED',DATEADD(DAY,-1,@now)),
(31,10,'REGISTERED',DATEADD(DAY,-1,@now)),
(32,10,'REGISTERED',DATEADD(DAY,-1,@now)),

-- OPEN SCHEDULE 11 - BRANCH 4 - 1
(34,11,'REGISTERED',DATEADD(DAY,-1,@now)),

-- OPEN SCHEDULE 12 - BRANCH 5 - 1
(35,12,'REGISTERED',DATEADD(DAY,-1,@now)),

-- OPEN CANCELLED - 18
-- SCHEDULE 1: 2
(36,1,'CANCELLED',DATEADD(DAY,-2,@now)),
(37,1,'CANCELLED',DATEADD(DAY,-2,@now)),

-- SCHEDULE 4: 2
(38,4,'CANCELLED',DATEADD(DAY,-2,@now)),
(39,4,'CANCELLED',DATEADD(DAY,-2,@now)),

-- SCHEDULE 5: 2
(40,5,'CANCELLED',DATEADD(DAY,-2,@now)),
(41,5,'CANCELLED',DATEADD(DAY,-2,@now)),

-- SCHEDULE 6: 4
(42,6,'CANCELLED',DATEADD(DAY,-2,@now)),
(43,6,'CANCELLED',DATEADD(DAY,-2,@now)),
(44,6,'CANCELLED',DATEADD(DAY,-2,@now)),
(45,6,'CANCELLED',DATEADD(DAY,-2,@now)),

-- SCHEDULE 7: 4
(46,7,'CANCELLED',DATEADD(DAY,-2,@now)),
(47,7,'CANCELLED',DATEADD(DAY,-2,@now)),
(48,7,'CANCELLED',DATEADD(DAY,-2,@now)),
(49,7,'CANCELLED',DATEADD(DAY,-2,@now)),

-- SCHEDULE 11: 2
(50,11,'CANCELLED',DATEADD(DAY,-2,@now)),
(51,11,'CANCELLED',DATEADD(DAY,-2,@now)),

-- SCHEDULE 12: 2
(52,12,'CANCELLED',DATEADD(DAY,-2,@now)),
(53,12,'CANCELLED',DATEADD(DAY,-2,@now)),

-- FULL SCHEDULE 13 - EXACTLY 6
(16,13,'REGISTERED',DATEADD(DAY,-1,@now)),
(17,13,'REGISTERED',DATEADD(DAY,-1,@now)),
(18,13,'REGISTERED',DATEADD(DAY,-1,@now)),
(19,13,'REGISTERED',DATEADD(DAY,-1,@now)),
(20,13,'REGISTERED',DATEADD(DAY,-1,@now)),
(21,13,'REGISTERED',DATEADD(DAY,-1,@now)),

-- FULL SCHEDULE 14 - EXACTLY 6
(22,14,'REGISTERED',DATEADD(DAY,-1,@now)),
(23,14,'REGISTERED',DATEADD(DAY,-1,@now)),
(24,14,'REGISTERED',DATEADD(DAY,-1,@now)),
(25,14,'REGISTERED',DATEADD(DAY,-1,@now)),
(26,14,'REGISTERED',DATEADD(DAY,-1,@now)),
(27,14,'REGISTERED',DATEADD(DAY,-1,@now)),

-- FULL SCHEDULE 15 - EXACTLY 6
(28,15,'REGISTERED',DATEADD(DAY,-1,@now)),
(29,15,'REGISTERED',DATEADD(DAY,-1,@now)),
(30,15,'REGISTERED',DATEADD(DAY,-1,@now)),
(31,15,'REGISTERED',DATEADD(DAY,-1,@now)),
(32,15,'REGISTERED',DATEADD(DAY,-1,@now)),
(33,15,'REGISTERED',DATEADD(DAY,-1,@now)),

-- COMPLETED SCHEDULE 16 - 4
(35,16,'ATTENDED',DATEADD(DAY,-25,@now)),
(36,16,'ATTENDED',DATEADD(DAY,-25,@now)),
(37,16,'ATTENDED',DATEADD(DAY,-25,@now)),
(38,16,'ABSENT',DATEADD(DAY,-25,@now)),

-- COMPLETED SCHEDULE 17 - 4
(39,17,'ATTENDED',DATEADD(DAY,-24,@now)),
(40,17,'ATTENDED',DATEADD(DAY,-24,@now)),
(41,17,'ATTENDED',DATEADD(DAY,-24,@now)),
(42,17,'ABSENT',DATEADD(DAY,-24,@now)),

-- COMPLETED SCHEDULE 18 - 4
(43,18,'ATTENDED',DATEADD(DAY,-23,@now)),
(44,18,'ATTENDED',DATEADD(DAY,-23,@now)),
(45,18,'ATTENDED',DATEADD(DAY,-23,@now)),
(46,18,'ABSENT',DATEADD(DAY,-23,@now)),

-- CANCELLED CLASS 19 - 3
(22,19,'CANCELLED',DATEADD(DAY,-5,@now)),
(23,19,'CANCELLED',DATEADD(DAY,-5,@now)),
(24,19,'CANCELLED',DATEADD(DAY,-5,@now)),

-- CANCELLED CLASS 20 - 3
(34,20,'CANCELLED',DATEADD(DAY,-5,@now)),
(35,20,'CANCELLED',DATEADD(DAY,-5,@now)),
(36,20,'CANCELLED',DATEADD(DAY,-5,@now));

-- ============================================================
-- 9. MEMBERSHIPS - 35
--
-- ACTIVE 20:
-- BRANCH 1: USERS 16-21
-- BRANCH 2: USERS 22-27
-- BRANCH 3: USERS 28-33
-- BRANCH 4: USER 34
-- BRANCH 5: USER 35
--
-- PENDING: USERS 36-40
-- EXPIRED: USERS 41-45
-- CANCELLED: USERS 46-50
-- ============================================================

INSERT INTO dbo.memberships (
    user_id,plan_id,branch_id,
    start_date,end_date,status,
    quoted_price,quoted_duration_days,
    created_at,updated_at
)
VALUES

-- ACTIVE - BRANCH 1
(16,2,1,DATEADD(DAY,-10,@today),DATEADD(DAY,20,@today),'ACTIVE',399000,30,DATEADD(DAY,-11,@now),@now),
(17,3,1,DATEADD(DAY,-12,@today),DATEADD(DAY,18,@today),'ACTIVE',299000,30,DATEADD(DAY,-13,@now),@now),
(18,4,1,DATEADD(DAY,-20,@today),DATEADD(DAY,70,@today),'ACTIVE',999000,90,DATEADD(DAY,-21,@now),@now),
(19,5,1,DATEADD(DAY,-30,@today),DATEADD(DAY,150,@today),'ACTIVE',1799000,180,DATEADD(DAY,-31,@now),@now),
(20,6,1,DATEADD(DAY,-40,@today),DATEADD(DAY,325,@today),'ACTIVE',2999000,365,DATEADD(DAY,-41,@now),@now),
(21,4,1,DATEADD(DAY,-25,@today),DATEADD(DAY,65,@today),'ACTIVE',999000,90,DATEADD(DAY,-26,@now),@now),

-- ACTIVE - BRANCH 2
(22,4,2,DATEADD(DAY,-20,@today),DATEADD(DAY,70,@today),'ACTIVE',999000,90,DATEADD(DAY,-21,@now),@now),
(23,3,2,DATEADD(DAY,-15,@today),DATEADD(DAY,15,@today),'ACTIVE',299000,30,DATEADD(DAY,-16,@now),@now),
(24,5,2,DATEADD(DAY,-40,@today),DATEADD(DAY,140,@today),'ACTIVE',1799000,180,DATEADD(DAY,-41,@now),@now),
(25,6,2,DATEADD(DAY,-60,@today),DATEADD(DAY,305,@today),'ACTIVE',2999000,365,DATEADD(DAY,-61,@now),@now),
(26,4,2,DATEADD(DAY,-18,@today),DATEADD(DAY,72,@today),'ACTIVE',999000,90,DATEADD(DAY,-19,@now),@now),
(27,3,2,DATEADD(DAY,-14,@today),DATEADD(DAY,16,@today),'ACTIVE',299000,30,DATEADD(DAY,-15,@now),@now),

-- ACTIVE - BRANCH 3
(28,4,3,DATEADD(DAY,-20,@today),DATEADD(DAY,70,@today),'ACTIVE',999000,90,DATEADD(DAY,-21,@now),@now),
(29,5,3,DATEADD(DAY,-35,@today),DATEADD(DAY,145,@today),'ACTIVE',1799000,180,DATEADD(DAY,-36,@now),@now),
(30,6,3,DATEADD(DAY,-50,@today),DATEADD(DAY,315,@today),'ACTIVE',2999000,365,DATEADD(DAY,-51,@now),@now),
(31,4,3,DATEADD(DAY,-18,@today),DATEADD(DAY,72,@today),'ACTIVE',999000,90,DATEADD(DAY,-19,@now),@now),
(32,3,3,DATEADD(DAY,-15,@today),DATEADD(DAY,15,@today),'ACTIVE',299000,30,DATEADD(DAY,-16,@now),@now),
(33,4,3,DATEADD(DAY,-22,@today),DATEADD(DAY,68,@today),'ACTIVE',999000,90,DATEADD(DAY,-23,@now),@now),

-- ACTIVE - BRANCH 4 AND 5
(34,5,4,DATEADD(DAY,-35,@today),DATEADD(DAY,145,@today),'ACTIVE',1799000,180,DATEADD(DAY,-36,@now),@now),
(35,6,5,DATEADD(DAY,-70,@today),DATEADD(DAY,295,@today),'ACTIVE',2999000,365,DATEADD(DAY,-71,@now),@now),

-- PENDING
(36,2,1,NULL,NULL,'PENDING',399000,30,DATEADD(HOUR,-2,@now),@now),
(37,3,2,NULL,NULL,'PENDING',299000,30,DATEADD(HOUR,-3,@now),@now),
(38,4,3,NULL,NULL,'PENDING',999000,90,DATEADD(HOUR,-4,@now),@now),
(39,5,4,NULL,NULL,'PENDING',1799000,180,DATEADD(HOUR,-5,@now),@now),
(40,6,5,NULL,NULL,'PENDING',2999000,365,DATEADD(HOUR,-6,@now),@now),

-- EXPIRED
(41,2,1,DATEADD(DAY,-60,@today),DATEADD(DAY,-30,@today),'EXPIRED',399000,30,DATEADD(DAY,-61,@now),@now),
(42,3,2,DATEADD(DAY,-50,@today),DATEADD(DAY,-20,@today),'EXPIRED',299000,30,DATEADD(DAY,-51,@now),@now),
(43,4,3,DATEADD(DAY,-120,@today),DATEADD(DAY,-30,@today),'EXPIRED',999000,90,DATEADD(DAY,-121,@now),@now),
(44,5,4,DATEADD(DAY,-200,@today),DATEADD(DAY,-20,@today),'EXPIRED',1799000,180,DATEADD(DAY,-201,@now),@now),
(45,6,5,DATEADD(DAY,-400,@today),DATEADD(DAY,-35,@today),'EXPIRED',2999000,365,DATEADD(DAY,-401,@now),@now),

-- CANCELLED
(46,2,1,NULL,NULL,'CANCELLED',399000,30,DATEADD(DAY,-12,@now),@now),
(47,3,2,NULL,NULL,'CANCELLED',299000,30,DATEADD(DAY,-11,@now),@now),
(48,4,3,NULL,NULL,'CANCELLED',999000,90,DATEADD(DAY,-10,@now),@now),
(49,5,4,NULL,NULL,'CANCELLED',1799000,180,DATEADD(DAY,-9,@now),@now),
(50,6,5,NULL,NULL,'CANCELLED',2999000,365,DATEADD(DAY,-8,@now),@now);

-- ============================================================
-- 10. PAYMENTS - 40
-- ============================================================

INSERT INTO dbo.payments (
    user_id,membership_id,amount,
    method,status,transaction_code,
    created_at,paid_at
)
VALUES

-- ACTIVE MEMBERSHIP PAYMENTS
(16,1,399000,'CASH','PAID','CASH-SEED-001',DATEADD(HOUR,1,DATEADD(DAY,-11,@now)),DATEADD(DAY,-10,@now)),
(17,2,299000,'VNPAY','PAID','VNPAY-SEED-002',DATEADD(HOUR,1,DATEADD(DAY,-13,@now)),DATEADD(DAY,-12,@now)),
(18,3,999000,'CASH','PAID','CASH-SEED-003',DATEADD(HOUR,1,DATEADD(DAY,-21,@now)),DATEADD(DAY,-20,@now)),
(19,4,1799000,'VNPAY','PAID','VNPAY-SEED-004',DATEADD(HOUR,1,DATEADD(DAY,-31,@now)),DATEADD(DAY,-30,@now)),
(20,5,2999000,'CASH','PAID','CASH-SEED-005',DATEADD(HOUR,1,DATEADD(DAY,-41,@now)),DATEADD(DAY,-40,@now)),
(21,6,999000,'VNPAY','PAID','VNPAY-SEED-006',DATEADD(HOUR,1,DATEADD(DAY,-26,@now)),DATEADD(DAY,-25,@now)),
(22,7,999000,'CASH','PAID','CASH-SEED-007',DATEADD(HOUR,1,DATEADD(DAY,-21,@now)),DATEADD(DAY,-20,@now)),
(23,8,299000,'VNPAY','PAID','VNPAY-SEED-008',DATEADD(HOUR,1,DATEADD(DAY,-16,@now)),DATEADD(DAY,-15,@now)),
(24,9,1799000,'CASH','PAID','CASH-SEED-009',DATEADD(HOUR,1,DATEADD(DAY,-41,@now)),DATEADD(DAY,-40,@now)),
(25,10,2999000,'VNPAY','PAID','VNPAY-SEED-010',DATEADD(HOUR,1,DATEADD(DAY,-61,@now)),DATEADD(DAY,-60,@now)),
(26,11,999000,'CASH','PAID','CASH-SEED-011',DATEADD(HOUR,1,DATEADD(DAY,-19,@now)),DATEADD(DAY,-18,@now)),
(27,12,299000,'VNPAY','PAID','VNPAY-SEED-012',DATEADD(HOUR,1,DATEADD(DAY,-15,@now)),DATEADD(DAY,-14,@now)),
(28,13,999000,'CASH','PAID','CASH-SEED-013',DATEADD(HOUR,1,DATEADD(DAY,-21,@now)),DATEADD(DAY,-20,@now)),
(29,14,1799000,'VNPAY','PAID','VNPAY-SEED-014',DATEADD(HOUR,1,DATEADD(DAY,-36,@now)),DATEADD(DAY,-35,@now)),
(30,15,2999000,'CASH','PAID','CASH-SEED-015',DATEADD(HOUR,1,DATEADD(DAY,-51,@now)),DATEADD(DAY,-50,@now)),
(31,16,999000,'VNPAY','PAID','VNPAY-SEED-016',DATEADD(HOUR,1,DATEADD(DAY,-19,@now)),DATEADD(DAY,-18,@now)),
(32,17,299000,'CASH','PAID','CASH-SEED-017',DATEADD(HOUR,1,DATEADD(DAY,-16,@now)),DATEADD(DAY,-15,@now)),
(33,18,999000,'VNPAY','PAID','VNPAY-SEED-018',DATEADD(HOUR,1,DATEADD(DAY,-23,@now)),DATEADD(DAY,-22,@now)),
(34,19,1799000,'CASH','PAID','CASH-SEED-019',DATEADD(HOUR,1,DATEADD(DAY,-36,@now)),DATEADD(DAY,-35,@now)),
(35,20,2999000,'VNPAY','PAID','VNPAY-SEED-020',DATEADD(HOUR,1,DATEADD(DAY,-71,@now)),DATEADD(DAY,-70,@now)),

-- PENDING CASH
(36,21,399000,'CASH','PENDING',NULL,DATEADD(MINUTE,-90,@now),NULL),
(37,22,299000,'CASH','PENDING',NULL,DATEADD(MINUTE,-150,@now),NULL),
(38,23,999000,'CASH','PENDING',NULL,DATEADD(MINUTE,-210,@now),NULL),
(39,24,1799000,'CASH','PENDING',NULL,DATEADD(MINUTE,-270,@now),NULL),
(40,25,2999000,'CASH','PENDING',NULL,DATEADD(MINUTE,-330,@now),NULL),

-- EXPIRED MEMBERSHIP PAYMENT HISTORY
(41,26,399000,'CASH','PAID','CASH-SEED-026',DATEADD(HOUR,1,DATEADD(DAY,-61,@now)),DATEADD(DAY,-60,@now)),
(42,27,299000,'VNPAY','PAID','VNPAY-SEED-027',DATEADD(HOUR,1,DATEADD(DAY,-51,@now)),DATEADD(DAY,-50,@now)),
(43,28,999000,'CASH','PAID','CASH-SEED-028',DATEADD(HOUR,1,DATEADD(DAY,-121,@now)),DATEADD(DAY,-120,@now)),
(44,29,1799000,'VNPAY','PAID','VNPAY-SEED-029',DATEADD(HOUR,1,DATEADD(DAY,-201,@now)),DATEADD(DAY,-200,@now)),
(45,30,2999000,'CASH','PAID','CASH-SEED-030',DATEADD(HOUR,1,DATEADD(DAY,-401,@now)),DATEADD(DAY,-400,@now)),

-- REFUNDED
(46,31,399000,'VNPAY','REFUNDED','REFUND-031',DATEADD(HOUR,1,DATEADD(DAY,-12,@now)),DATEADD(DAY,-11,@now)),
(47,32,299000,'VNPAY','REFUNDED','REFUND-032',DATEADD(HOUR,1,DATEADD(DAY,-11,@now)),DATEADD(DAY,-10,@now)),
(48,33,999000,'CASH','REFUNDED','REFUND-033',DATEADD(HOUR,1,DATEADD(DAY,-10,@now)),DATEADD(DAY,-9,@now)),
(49,34,1799000,'VNPAY','REFUNDED','REFUND-034',DATEADD(HOUR,1,DATEADD(DAY,-9,@now)),DATEADD(DAY,-8,@now)),
(50,35,2999000,'CASH','REFUNDED','REFUND-035',DATEADD(HOUR,1,DATEADD(DAY,-8,@now)),DATEADD(DAY,-7,@now)),

-- FAILED VNPAY
(36,21,399000,'VNPAY','FAILED','VNPAY-FAILED-001',DATEADD(MINUTE,-110,@now),NULL),
(37,22,299000,'VNPAY','FAILED','VNPAY-FAILED-002',DATEADD(MINUTE,-170,@now),NULL),
(38,23,999000,'VNPAY','FAILED','VNPAY-FAILED-003',DATEADD(MINUTE,-230,@now),NULL),
(39,24,1799000,'VNPAY','FAILED','VNPAY-FAILED-004',DATEADD(MINUTE,-290,@now),NULL),
(40,25,2999000,'VNPAY','FAILED','VNPAY-FAILED-005',DATEADD(MINUTE,-350,@now),NULL);

-- ============================================================
-- 11. REVIEWS - 20
-- ============================================================

INSERT INTO dbo.reviews (
    user_id,branch_id,trainer_id,
    rating,comment,visible,created_at,updated_at
)
VALUES
    (16,1,1,5,N'Huấn luyện viên hướng dẫn rất tận tình.',1,DATEADD(DAY,-1,@now),@now),
    (17,1,1,4,N'Phòng tập sạch sẽ và thiết bị tốt.',1,DATEADD(DAY,-2,@now),@now),
    (18,1,1,5,N'Lớp Yoga rất hiệu quả.',1,DATEADD(DAY,-5,@now),@now),
    (19,1,8,4,N'PT rất nhiệt tình.',1,DATEADD(DAY,-10,@now),@now),
    (20,1,8,5,N'Môi trường tập luyện tốt.',1,DATEADD(DAY,-16,@now),@now),
    (21,1,1,4,N'Lịch tập linh hoạt.',1,DATEADD(DAY,-3,@now),@now),
    (22,2,2,5,N'Cardio cải thiện sức bền.',1,DATEADD(DAY,-10,@now),@now),
    (23,2,2,3,N'Giờ cao điểm hơi đông.',1,DATEADD(DAY,-5,@now),@now),
    (24,2,9,2,N'Cần thêm thiết bị.',1,DATEADD(DAY,-12,@now),@now),
    (25,2,9,1,N'Cần cải thiện bảo trì.',0,DATEADD(DAY,-11,@now),@now),
    (26,2,2,5,N'PT rất chuyên nghiệp.',1,DATEADD(HOUR,-12,@now),@now),
    (27,2,2,4,N'Không gian tập rộng.',1,DATEADD(DAY,-2,@now),@now),
    (28,3,3,3,N'Dịch vụ khá tốt.',1,DATEADD(DAY,-8,@now),@now),
    (29,3,3,2,N'Cần bổ sung dụng cụ.',1,DATEADD(DAY,-7,@now),@now),
    (30,3,10,1,N'Trải nghiệm chưa tốt.',0,DATEADD(DAY,-6,@now),@now),
    (31,3,NULL,5,N'Vị trí thuận tiện.',1,DATEADD(DAY,-5,@now),@now),
    (32,3,NULL,4,N'Nhân viên thân thiện.',1,DATEADD(DAY,-6,@now),@now),
    (33,3,NULL,3,N'Cần cải thiện điều hòa.',1,DATEADD(DAY,-10,@now),@now),
    (34,4,NULL,2,N'Khu thay đồ hơi nhỏ.',1,DATEADD(DAY,-17,@now),@now),
    (35,5,NULL,1,N'Thời gian chờ thiết bị lâu.',0,DATEADD(DAY,-16,@now),@now);

-- ============================================================
-- 12. OTP CODES - 15
--
-- BCrypt hash is for database testing only.
-- Plaintext OTP is unknown.
-- ============================================================

INSERT INTO dbo.otp_codes (
    user_id,code,type,used,
    attempt_count,created_at,expired_at,used_at
)
VALUES
    (16,@password,'FORGOT_PASSWORD',0,0,DATEADD(MINUTE,-1,@now),DATEADD(MINUTE,4,@now),NULL),
    (17,@password,'FORGOT_PASSWORD',0,1,DATEADD(MINUTE,-2,@now),DATEADD(MINUTE,3,@now),NULL),
    (18,@password,'FORGOT_PASSWORD',0,2,DATEADD(MINUTE,-3,@now),DATEADD(MINUTE,2,@now),NULL),
    (19,@password,'FORGOT_PASSWORD',0,3,DATEADD(MINUTE,-4,@now),DATEADD(MINUTE,1,@now),NULL),
    (20,@password,'FORGOT_PASSWORD',1,5,DATEADD(MINUTE,-10,@now),DATEADD(MINUTE,-5,@now),DATEADD(MINUTE,-6,@now)),
    (21,@password,'FORGOT_PASSWORD',0,0,DATEADD(HOUR,-2,@now),DATEADD(MINUTE,-115,@now),NULL),
    (22,@password,'FORGOT_PASSWORD',0,1,DATEADD(HOUR,-3,@now),DATEADD(MINUTE,-175,@now),NULL),
    (23,@password,'FORGOT_PASSWORD',0,2,DATEADD(HOUR,-4,@now),DATEADD(MINUTE,-235,@now),NULL),
    (24,@password,'FORGOT_PASSWORD',0,3,DATEADD(HOUR,-5,@now),DATEADD(MINUTE,-295,@now),NULL),
    (25,@password,'FORGOT_PASSWORD',0,4,DATEADD(HOUR,-6,@now),DATEADD(MINUTE,-355,@now),NULL),
    (61,@password,'REGISTER',0,0,DATEADD(MINUTE,-1,@now),DATEADD(MINUTE,4,@now),NULL),
    (62,@password,'REGISTER',0,1,DATEADD(MINUTE,-2,@now),DATEADD(MINUTE,3,@now),NULL),
    (63,@password,'REGISTER',0,2,DATEADD(MINUTE,-3,@now),DATEADD(MINUTE,2,@now),NULL),
    (64,@password,'REGISTER',0,0,DATEADD(HOUR,-2,@now),DATEADD(MINUTE,-115,@now),NULL),
    (65,@password,'REGISTER',0,1,DATEADD(HOUR,-3,@now),DATEADD(MINUTE,-175,@now),NULL);

-- ============================================================
-- 13. TRAINER REQUESTS - 35
-- ============================================================

INSERT INTO dbo.trainer_requests (
    member_id,trainer_id,message,
    status,requested_at,responded_at
)
VALUES

-- PENDING - 10
(16,1,'Toi muon hoc Yoga.','PENDING',DATEADD(HOUR,-2,@now),NULL),
(17,2,'Toi can Cardio giam can.','PENDING',DATEADD(HOUR,-3,@now),NULL),
(18,3,'Toi muon tang co.','PENDING',DATEADD(HOUR,-4,@now),NULL),
(19,4,'Toi muon tap Boxing.','PENDING',DATEADD(HOUR,-5,@now),NULL),
(20,5,'Toi muon hoc Zumba.','PENDING',DATEADD(HOUR,-6,@now),NULL),
(21,6,'Toi can tap Pilates.','PENDING',DATEADD(HOUR,-7,@now),NULL),
(22,7,'Toi muon tap CrossFit.','PENDING',DATEADD(HOUR,-8,@now),NULL),
(23,8,'Toi can PT tong quat.','PENDING',DATEADD(HOUR,-9,@now),NULL),
(24,9,'Toi can tap ba buoi moi tuan.','PENDING',DATEADD(HOUR,-10,@now),NULL),
(25,10,'Toi muon duoc tu van tang co.','PENDING',DATEADD(HOUR,-11,@now),NULL),

-- ACCEPTED - 10
(26,1,'Toi can Yoga co ban.','ACCEPTED',DATEADD(DAY,-10,@now),DATEADD(HOUR,2,DATEADD(DAY,-10,@now))),
(27,2,'Toi muon giam mo.','ACCEPTED',DATEADD(DAY,-11,@now),DATEADD(HOUR,3,DATEADD(DAY,-11,@now))),
(28,3,'Toi muon tang suc manh.','ACCEPTED',DATEADD(DAY,-12,@now),DATEADD(HOUR,4,DATEADD(DAY,-12,@now))),
(29,4,'Toi can hoc Boxing.','ACCEPTED',DATEADD(DAY,-13,@now),DATEADD(HOUR,2,DATEADD(DAY,-13,@now))),
(30,5,'Toi muon tap Zumba.','ACCEPTED',DATEADD(DAY,-14,@now),DATEADD(HOUR,3,DATEADD(DAY,-14,@now))),
(31,6,'Toi muon hoc Pilates.','ACCEPTED',DATEADD(DAY,-15,@now),DATEADD(HOUR,4,DATEADD(DAY,-15,@now))),
(32,7,'Toi muon hoc CrossFit.','ACCEPTED',DATEADD(DAY,-16,@now),DATEADD(HOUR,2,DATEADD(DAY,-16,@now))),
(33,8,'Toi muon tap gym tong quat.','ACCEPTED',DATEADD(DAY,-17,@now),DATEADD(HOUR,3,DATEADD(DAY,-17,@now))),
(34,9,'Toi can ke hoach tap.','ACCEPTED',DATEADD(DAY,-18,@now),DATEADD(HOUR,4,DATEADD(DAY,-18,@now))),
(35,10,'Toi muon tang can.','ACCEPTED',DATEADD(DAY,-19,@now),DATEADD(HOUR,2,DATEADD(DAY,-19,@now))),

-- REJECTED - 10
(36,2,'Khung gio da kin.','REJECTED',DATEADD(DAY,-8,@now),DATEADD(HOUR,2,DATEADD(DAY,-8,@now))),
(37,3,'Can PT buoi sang.','REJECTED',DATEADD(DAY,-9,@now),DATEADD(HOUR,3,DATEADD(DAY,-9,@now))),
(38,4,'Tap Boxing buoi dem.','REJECTED',DATEADD(DAY,-10,@now),DATEADD(HOUR,4,DATEADD(DAY,-10,@now))),
(39,5,'Tap Zumba ngay nghi.','REJECTED',DATEADD(DAY,-11,@now),DATEADD(HOUR,2,DATEADD(DAY,-11,@now))),
(40,6,'Tap Pilates ngoai gio.','REJECTED',DATEADD(DAY,-12,@now),DATEADD(HOUR,3,DATEADD(DAY,-12,@now))),
(41,7,'Tap CrossFit.','REJECTED',DATEADD(DAY,-13,@now),DATEADD(HOUR,4,DATEADD(DAY,-13,@now))),
(42,8,'Tap gym buoi dem.','REJECTED',DATEADD(DAY,-14,@now),DATEADD(HOUR,2,DATEADD(DAY,-14,@now))),
(43,9,'Huan luyen ngoai gio.','REJECTED',DATEADD(DAY,-15,@now),DATEADD(HOUR,3,DATEADD(DAY,-15,@now))),
(44,10,'Can PT cuoi tuan.','REJECTED',DATEADD(DAY,-16,@now),DATEADD(HOUR,4,DATEADD(DAY,-16,@now))),
(45,12,'Muon hoc Yoga buoi toi.','REJECTED',DATEADD(DAY,-17,@now),DATEADD(HOUR,2,DATEADD(DAY,-17,@now))),

-- CANCELLED - 5
(26,2,'Da thay doi ke hoach Cardio.','CANCELLED',DATEADD(DAY,-20,@now),NULL),
(27,3,'Da tam hoan Strength.','CANCELLED',DATEADD(DAY,-21,@now),NULL),
(28,4,'Da huy yeu cau Boxing.','CANCELLED',DATEADD(DAY,-22,@now),NULL),
(29,5,'Da thay doi muc tieu Zumba.','CANCELLED',DATEADD(DAY,-23,@now),NULL),
(30,6,'Chua co thoi gian Pilates.','CANCELLED',DATEADD(DAY,-24,@now),NULL);

-- ============================================================
-- 14. CHECKINS - 20
-- ============================================================

INSERT INTO dbo.checkins (
    user_id,branch_id,check_in_at,check_out_at
)
VALUES

-- COMPLETED - YESTERDAY
(16,1,DATEADD(HOUR,8,CAST(DATEADD(DAY,-1,@today) AS DATETIME2)),DATEADD(HOUR,10,CAST(DATEADD(DAY,-1,@today) AS DATETIME2))),
(17,1,DATEADD(HOUR,9,CAST(DATEADD(DAY,-1,@today) AS DATETIME2)),DATEADD(HOUR,11,CAST(DATEADD(DAY,-1,@today) AS DATETIME2))),
(18,1,DATEADD(HOUR,10,CAST(DATEADD(DAY,-1,@today) AS DATETIME2)),DATEADD(HOUR,12,CAST(DATEADD(DAY,-1,@today) AS DATETIME2))),
(19,1,DATEADD(HOUR,11,CAST(DATEADD(DAY,-1,@today) AS DATETIME2)),DATEADD(HOUR,13,CAST(DATEADD(DAY,-1,@today) AS DATETIME2))),
(20,1,DATEADD(HOUR,12,CAST(DATEADD(DAY,-1,@today) AS DATETIME2)),DATEADD(HOUR,14,CAST(DATEADD(DAY,-1,@today) AS DATETIME2))),
(21,1,DATEADD(HOUR,13,CAST(DATEADD(DAY,-1,@today) AS DATETIME2)),DATEADD(HOUR,15,CAST(DATEADD(DAY,-1,@today) AS DATETIME2))),
(22,2,DATEADD(HOUR,14,CAST(DATEADD(DAY,-1,@today) AS DATETIME2)),DATEADD(HOUR,16,CAST(DATEADD(DAY,-1,@today) AS DATETIME2))),

-- CURRENTLY CHECKED IN
(23,2,DATEADD(MINUTE,-45,@now),NULL),
(24,2,DATEADD(MINUTE,-40,@now),NULL),
(25,2,DATEADD(MINUTE,-35,@now),NULL),
(26,2,DATEADD(MINUTE,-30,@now),NULL),
(27,2,DATEADD(MINUTE,-25,@now),NULL),

-- HISTORICAL
(28,3,DATEADD(HOUR,8,CAST(DATEADD(DAY,-2,@today) AS DATETIME2)),DATEADD(HOUR,10,CAST(DATEADD(DAY,-2,@today) AS DATETIME2))),
(29,3,DATEADD(HOUR,9,CAST(DATEADD(DAY,-3,@today) AS DATETIME2)),DATEADD(HOUR,11,CAST(DATEADD(DAY,-3,@today) AS DATETIME2))),
(30,3,DATEADD(HOUR,10,CAST(DATEADD(DAY,-4,@today) AS DATETIME2)),DATEADD(HOUR,12,CAST(DATEADD(DAY,-4,@today) AS DATETIME2))),
(31,3,DATEADD(HOUR,11,CAST(DATEADD(DAY,-5,@today) AS DATETIME2)),DATEADD(HOUR,13,CAST(DATEADD(DAY,-5,@today) AS DATETIME2))),
(32,3,DATEADD(HOUR,12,CAST(DATEADD(DAY,-6,@today) AS DATETIME2)),DATEADD(HOUR,14,CAST(DATEADD(DAY,-6,@today) AS DATETIME2))),
(33,3,DATEADD(HOUR,13,CAST(DATEADD(DAY,-7,@today) AS DATETIME2)),DATEADD(HOUR,15,CAST(DATEADD(DAY,-7,@today) AS DATETIME2))),
(34,4,DATEADD(HOUR,14,CAST(DATEADD(DAY,-8,@today) AS DATETIME2)),DATEADD(HOUR,16,CAST(DATEADD(DAY,-8,@today) AS DATETIME2))),
(35,5,DATEADD(HOUR,15,CAST(DATEADD(DAY,-9,@today) AS DATETIME2)),DATEADD(HOUR,17,CAST(DATEADD(DAY,-9,@today) AS DATETIME2)));

-- ============================================================
-- 15. CONVERSATIONS - 10
-- ============================================================

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

-- ============================================================
-- 16. MESSAGES - 20
-- ============================================================

INSERT INTO dbo.messages (
    conversation_id,sender_id,content,sent_at,read_at
)
VALUES
    (1,26,'Chao PT, toi muon bat dau Yoga.',DATEADD(DAY,-8,@now),DATEADD(DAY,-8,@now)),
    (1,4,'Chung ta se bat dau voi Yoga co ban.',DATEADD(HOUR,1,DATEADD(DAY,-8,@now)),DATEADD(HOUR,2,DATEADD(DAY,-8,@now))),
    (1,26,'Cam on PT.',DATEADD(HOUR,3,DATEADD(DAY,-8,@now)),NULL),

    (2,27,'Toi muon giam mo.',DATEADD(DAY,-9,@now),DATEADD(DAY,-9,@now)),
    (2,5,'Chung ta se ket hop Cardio va HIIT.',DATEADD(HOUR,1,DATEADD(DAY,-9,@now)),DATEADD(HOUR,2,DATEADD(DAY,-9,@now))),
    (2,27,'Toi co the tap ba buoi.',DATEADD(HOUR,3,DATEADD(DAY,-9,@now)),NULL),

    (3,28,'Toi muon tang co.',DATEADD(DAY,-10,@now),DATEADD(DAY,-10,@now)),
    (3,6,'Toi se thiet ke lo trinh.',DATEADD(HOUR,1,DATEADD(DAY,-10,@now)),DATEADD(HOUR,2,DATEADD(DAY,-10,@now))),

    (4,29,'Toi chua tung tap Boxing.',DATEADD(DAY,-11,@now),DATEADD(DAY,-11,@now)),
    (4,7,'Chung ta se hoc co ban.',DATEADD(HOUR,1,DATEADD(DAY,-11,@now)),NULL),

    (5,30,'Toi muon tap Zumba.',DATEADD(DAY,-12,@now),DATEADD(DAY,-12,@now)),
    (5,8,'Zumba phu hop voi ban.',DATEADD(HOUR,1,DATEADD(DAY,-12,@now)),NULL),

    (6,31,'Toi muon tap Pilates.',DATEADD(DAY,-13,@now),DATEADD(DAY,-13,@now)),
    (6,9,'Toi se huong dan ban.',DATEADD(HOUR,1,DATEADD(DAY,-13,@now)),NULL),

    (7,32,'Toi muon hoc CrossFit.',DATEADD(DAY,-14,@now),DATEADD(DAY,-14,@now)),
    (7,10,'Chung ta se kiem tra the luc.',DATEADD(HOUR,1,DATEADD(DAY,-14,@now)),NULL),

    (8,33,'Toi can PT tong quat.',DATEADD(DAY,-15,@now),DATEADD(DAY,-15,@now)),
    (8,11,'Toi se lap ke hoach tap.',DATEADD(HOUR,1,DATEADD(DAY,-15,@now)),NULL),

    (9,34,'Toi muon tap bon buoi.',DATEADD(DAY,-16,@now),DATEADD(DAY,-16,@now)),
    (9,12,'Toi se sap xep lich.',DATEADD(HOUR,1,DATEADD(DAY,-16,@now)),NULL),

    (10,35,'Toi muon tang can.',DATEADD(DAY,-17,@now),DATEADD(DAY,-17,@now)),
    (10,13,'Chung ta se ket hop dinh duong.',DATEADD(HOUR,1,DATEADD(DAY,-17,@now)),NULL);

-- ============================================================
-- 17. PERSONAL TRAINING SCHEDULES - 20
-- ============================================================

INSERT INTO dbo.personal_training_schedules (
    member_id,trainer_id,branch_id,
    schedule_date,start_time,end_time,
    title,note,status,active,created_at,updated_at
)
VALUES

-- SCHEDULED - 10
(26,1,1,DATEADD(DAY,1,@today),'08:00','09:00','Yoga ca nhan','Yoga co ban.','SCHEDULED',1,DATEADD(DAY,-2,@now),@now),
(27,2,2,DATEADD(DAY,1,@today),'09:00','10:00','Cardio giam mo','Tap Cardio.','SCHEDULED',1,DATEADD(DAY,-2,@now),@now),
(28,3,3,DATEADD(DAY,1,@today),'10:00','11:00','Strength Training','Tap suc manh.','SCHEDULED',1,DATEADD(DAY,-2,@now),@now),
(29,4,4,DATEADD(DAY,1,@today),'15:00','16:00','Boxing Basic','Ky thuat Boxing.','SCHEDULED',1,DATEADD(DAY,-2,@now),@now),
(30,5,5,DATEADD(DAY,2,@today),'08:00','09:00','Zumba Personal','Tap Zumba.','SCHEDULED',1,DATEADD(DAY,-2,@now),@now),
(31,6,6,DATEADD(DAY,2,@today),'09:00','10:00','Pilates Core','Tap co loi.','SCHEDULED',1,DATEADD(DAY,-2,@now),@now),
(32,7,7,DATEADD(DAY,2,@today),'10:00','11:00','CrossFit Foundation','CrossFit co ban.','SCHEDULED',1,DATEADD(DAY,-2,@now),@now),
(33,8,1,DATEADD(DAY,2,@today),'15:00','16:00','General Fitness','Danh gia the luc.','SCHEDULED',1,DATEADD(DAY,-2,@now),@now),
(34,9,2,DATEADD(DAY,3,@today),'08:00','09:00','Fitness Coaching','Tap toan than.','SCHEDULED',1,DATEADD(DAY,-2,@now),@now),
(35,10,3,DATEADD(DAY,3,@today),'09:00','10:00','Muscle Gain','Tap tang co.','SCHEDULED',1,DATEADD(DAY,-2,@now),@now),

-- COMPLETED - 5
(26,1,1,DATEADD(DAY,-5,@today),'08:00','09:00','Yoga Foundation','Da hoan thanh.','COMPLETED',0,DATEADD(DAY,-9,@now),@now),
(27,2,2,DATEADD(DAY,-4,@today),'09:00','10:00','Cardio Foundation','Da hoan thanh.','COMPLETED',0,DATEADD(DAY,-10,@now),@now),
(28,3,3,DATEADD(DAY,-3,@today),'10:00','11:00','Strength Foundation','Da hoan thanh.','COMPLETED',0,DATEADD(DAY,-11,@now),@now),
(29,4,4,DATEADD(DAY,-2,@today),'15:00','16:00','Boxing Foundation','Da hoan thanh.','COMPLETED',0,DATEADD(DAY,-12,@now),@now),
(30,5,5,DATEADD(DAY,-1,@today),'08:00','09:00','Zumba Foundation','Da hoan thanh.','COMPLETED',0,DATEADD(DAY,-13,@now),@now),

-- CANCELLED - 5
(26,1,1,DATEADD(DAY,4,@today),'14:00','15:00','Yoga Recovery','Da huy.','CANCELLED',0,DATEADD(DAY,-2,@now),DATEADD(HOUR,-2,@now)),
(27,2,2,DATEADD(DAY,4,@today),'15:00','16:00','Cardio Advanced','Da huy.','CANCELLED',0,DATEADD(DAY,-2,@now),DATEADD(HOUR,-3,@now)),
(28,3,3,DATEADD(DAY,5,@today),'14:00','15:00','Strength Advanced','Da huy.','CANCELLED',0,DATEADD(DAY,-1,@now),DATEADD(HOUR,-4,@now)),
(29,4,4,DATEADD(DAY,5,@today),'15:00','16:00','Boxing Advanced','Da huy.','CANCELLED',0,DATEADD(DAY,-1,@now),DATEADD(HOUR,-5,@now)),
(30,5,5,DATEADD(DAY,6,@today),'14:00','15:00','Zumba Advanced','Da huy.','CANCELLED',0,DATEADD(HOUR,-12,@now),DATEADD(HOUR,-6,@now));

-- ============================================================
-- 18. NOTIFICATIONS - 30
-- ============================================================

INSERT INTO dbo.notifications (
    user_id,type,title,content,
    related_id,is_read,read_at,created_at
)
VALUES

-- TRAINER_REQUEST_SENT - 3
(4,'TRAINER_REQUEST_SENT','Yeu cau PT moi','Member An da gui yeu cau.',1,0,NULL,DATEADD(HOUR,-2,@now)),
(5,'TRAINER_REQUEST_SENT','Yeu cau PT moi','Member Binh da gui yeu cau.',2,0,NULL,DATEADD(HOUR,-3,@now)),
(6,'TRAINER_REQUEST_SENT','Yeu cau PT moi','Member Chi da gui yeu cau.',3,1,DATEADD(HOUR,-2,@now),DATEADD(HOUR,-4,@now)),

-- TRAINER_REQUEST_ACCEPTED - 3
(26,'TRAINER_REQUEST_ACCEPTED','Yeu cau duoc chap nhan','PT Minh da chap nhan.',11,1,DATEADD(DAY,-9,@now),DATEADD(DAY,-10,@now)),
(27,'TRAINER_REQUEST_ACCEPTED','Yeu cau duoc chap nhan','PT Lan da chap nhan.',12,0,NULL,DATEADD(DAY,-10,@now)),
(28,'TRAINER_REQUEST_ACCEPTED','Yeu cau duoc chap nhan','PT Hung da chap nhan.',13,1,DATEADD(DAY,-11,@now),DATEADD(DAY,-12,@now)),

-- TRAINER_REQUEST_REJECTED - 2
(36,'TRAINER_REQUEST_REJECTED','Yeu cau bi tu choi','PT khong the nhan lich.',21,0,NULL,DATEADD(DAY,-8,@now)),
(37,'TRAINER_REQUEST_REJECTED','Yeu cau bi tu choi','Khong con lich phu hop.',22,1,DATEADD(DAY,-8,@now),DATEADD(DAY,-9,@now)),

-- PERSONAL_SCHEDULE_CREATED - 3
(26,'PERSONAL_SCHEDULE_CREATED','Lich tap moi','Lich Yoga ca nhan.',1,0,NULL,DATEADD(DAY,-2,@now)),
(27,'PERSONAL_SCHEDULE_CREATED','Lich tap moi','Lich Cardio ca nhan.',2,0,NULL,DATEADD(DAY,-2,@now)),
(28,'PERSONAL_SCHEDULE_CREATED','Lich tap moi','Lich Strength ca nhan.',3,1,DATEADD(HOUR,-1,@now),DATEADD(DAY,-2,@now)),

-- CLASS_SCHEDULE_CANCELLED - 3
(22,'CLASS_SCHEDULE_CANCELLED','Lop da huy','Cardio Weekend da huy.',19,0,NULL,DATEADD(DAY,-1,@now)),
(23,'CLASS_SCHEDULE_CANCELLED','Lop da huy','Cardio Weekend da huy.',19,1,DATEADD(HOUR,-12,@now),DATEADD(DAY,-1,@now)),
(34,'CLASS_SCHEDULE_CANCELLED','Lop da huy','Boxing Workshop da huy.',20,0,NULL,DATEADD(DAY,-1,@now)),

-- MESSAGE_RECEIVED - 6
(26,'MESSAGE_RECEIVED','Tin nhan moi','PT Minh da nhan tin.',2,0,NULL,DATEADD(HOUR,1,DATEADD(DAY,-8,@now))),
(27,'MESSAGE_RECEIVED','Tin nhan moi','PT Lan da nhan tin.',5,1,DATEADD(DAY,-8,@now),DATEADD(HOUR,1,DATEADD(DAY,-9,@now))),
(28,'MESSAGE_RECEIVED','Tin nhan moi','PT Hung da nhan tin.',8,0,NULL,DATEADD(HOUR,1,DATEADD(DAY,-10,@now))),
(29,'MESSAGE_RECEIVED','Tin nhan moi','PT Thao da nhan tin.',10,0,NULL,DATEADD(HOUR,1,DATEADD(DAY,-11,@now))),
(30,'MESSAGE_RECEIVED','Tin nhan moi','PT Khoa da nhan tin.',12,1,DATEADD(DAY,-11,@now),DATEADD(HOUR,1,DATEADD(DAY,-12,@now))),
(31,'MESSAGE_RECEIVED','Tin nhan moi','PT Mai da nhan tin.',14,0,NULL,DATEADD(HOUR,1,DATEADD(DAY,-13,@now))),

-- MEMBERSHIP_ACTIVATED - 5
(16,'MEMBERSHIP_ACTIVATED','Goi tap kich hoat','Basic 1 Month da kich hoat.',1,1,DATEADD(DAY,-9,@now),DATEADD(DAY,-10,@now)),
(17,'MEMBERSHIP_ACTIVATED','Goi tap kich hoat','Student 1 Month da kich hoat.',2,0,NULL,DATEADD(DAY,-12,@now)),
(18,'MEMBERSHIP_ACTIVATED','Goi tap kich hoat','Standard 3 Months da kich hoat.',3,1,DATEADD(DAY,-19,@now),DATEADD(DAY,-20,@now)),
(19,'MEMBERSHIP_ACTIVATED','Goi tap kich hoat','Premium 6 Months da kich hoat.',4,0,NULL,DATEADD(DAY,-30,@now)),
(20,'MEMBERSHIP_ACTIVATED','Goi tap kich hoat','VIP 12 Months da kich hoat.',5,1,DATEADD(DAY,-39,@now),DATEADD(DAY,-40,@now)),

-- PERSONAL_SCHEDULE_CANCELLED - 5
(26,'PERSONAL_SCHEDULE_CANCELLED','Lich tap da huy','Yoga Recovery da huy.',16,0,NULL,DATEADD(HOUR,-2,@now)),
(27,'PERSONAL_SCHEDULE_CANCELLED','Lich tap da huy','Cardio Advanced da huy.',17,1,DATEADD(HOUR,-1,@now),DATEADD(HOUR,-3,@now)),
(28,'PERSONAL_SCHEDULE_CANCELLED','Lich tap da huy','Strength Advanced da huy.',18,0,NULL,DATEADD(HOUR,-4,@now)),
(29,'PERSONAL_SCHEDULE_CANCELLED','Lich tap da huy','Boxing Advanced da huy.',19,1,DATEADD(HOUR,-2,@now),DATEADD(HOUR,-5,@now)),
(30,'PERSONAL_SCHEDULE_CANCELLED','Lich tap da huy','Zumba Advanced da huy.',20,0,NULL,DATEADD(HOUR,-6,@now));

-- ============================================================
-- 19. PROMOTIONS - 12
-- ============================================================

INSERT INTO dbo.promotions (
    code,discount_percent,quantity,
    start_date,end_date,active,created_at
)
VALUES
    ('WELCOME10',10,100,DATEADD(DAY,-10,@today),DATEADD(DAY,30,@today),1,@now),
    ('STUDENT15',15,80,DATEADD(DAY,-5,@today),DATEADD(DAY,60,@today),1,@now),
    ('FITNESS20',20,50,DATEADD(DAY,-3,@today),DATEADD(DAY,20,@today),1,@now),
    ('VIP25',25,20,DATEADD(DAY,-1,@today),DATEADD(DAY,15,@today),1,@now),
    ('WEEKEND5',5,200,DATEADD(DAY,-7,@today),DATEADD(DAY,7,@today),1,@now),
    ('SUMMER30',30,0,DATEADD(DAY,-30,@today),DATEADD(DAY,10,@today),1,@now),
    ('EXPIRED10',10,20,DATEADD(DAY,-60,@today),DATEADD(DAY,-30,@today),1,@now),
    ('EXPIRED20',20,10,DATEADD(DAY,-90,@today),DATEADD(DAY,-15,@today),1,@now),
    ('FUTURE15',15,100,DATEADD(DAY,10,@today),DATEADD(DAY,40,@today),1,@now),
    ('FUTURE25',25,50,DATEADD(DAY,20,@today),DATEADD(DAY,50,@today),1,@now),
    ('DISABLED10',10,100,DATEADD(DAY,-5,@today),DATEADD(DAY,30,@today),0,@now),
    ('DISABLED30',30,30,DATEADD(DAY,-5,@today),DATEADD(DAY,30,@today),0,@now);

-- ============================================================
-- VALIDATION
-- ============================================================

-- ------------------------------------------------------------
-- 1. EXACT ROW COUNTS
-- ------------------------------------------------------------

IF (SELECT COUNT(*) FROM dbo.users) <> 65
BEGIN
    THROW 50101, 'Expected 65 users.', 1;
END;

IF (SELECT COUNT(*) FROM dbo.branches) <> 10
BEGIN
    THROW 50102, 'Expected 10 branches.', 1;
END;

IF (SELECT COUNT(*) FROM dbo.class_categories) <> 10
BEGIN
    THROW 50103, 'Expected 10 class categories.', 1;
END;

IF (SELECT COUNT(*) FROM dbo.membership_plans) <> 10
BEGIN
    THROW 50104, 'Expected 10 membership plans.', 1;
END;

IF (SELECT COUNT(*) FROM dbo.trainers) <> 12
BEGIN
    THROW 50105, 'Expected 12 trainers.', 1;
END;

IF (SELECT COUNT(*) FROM dbo.fitness_classes) <> 20
BEGIN
    THROW 50106, 'Expected 20 fitness classes.', 1;
END;

IF (SELECT COUNT(*) FROM dbo.class_schedules) <> 20
BEGIN
    THROW 50107, 'Expected 20 class schedules.', 1;
END;

IF (SELECT COUNT(*) FROM dbo.class_registrations) <> 90
BEGIN
    THROW 50108, 'Expected exactly 90 class registrations.', 1;
END;

IF (SELECT COUNT(*) FROM dbo.memberships) <> 35
BEGIN
    THROW 50109, 'Expected 35 memberships.', 1;
END;

IF (SELECT COUNT(*) FROM dbo.payments) <> 40
BEGIN
    THROW 50110, 'Expected 40 payments.', 1;
END;

IF (SELECT COUNT(*) FROM dbo.reviews) <> 20
BEGIN
    THROW 50111, 'Expected 20 reviews.', 1;
END;

IF (SELECT COUNT(*) FROM dbo.otp_codes) <> 15
BEGIN
    THROW 50112, 'Expected 15 OTP codes.', 1;
END;

IF (SELECT COUNT(*) FROM dbo.trainer_requests) <> 35
BEGIN
    THROW 50113, 'Expected 35 trainer requests.', 1;
END;

IF (SELECT COUNT(*) FROM dbo.checkins) <> 20
BEGIN
    THROW 50114, 'Expected 20 checkins.', 1;
END;

IF (SELECT COUNT(*) FROM dbo.conversations) <> 10
BEGIN
    THROW 50115, 'Expected 10 conversations.', 1;
END;

IF (SELECT COUNT(*) FROM dbo.messages) <> 22
BEGIN
    THROW 50116, 'Expected 22 messages.', 1;
END;

IF (SELECT COUNT(*) FROM dbo.personal_training_schedules) <> 20
BEGIN
    THROW 50117, 'Expected 20 personal training schedules.', 1;
END;

IF (SELECT COUNT(*) FROM dbo.notifications) <> 30
BEGIN
    THROW 50118, 'Expected 30 notifications.', 1;
END;

IF (SELECT COUNT(*) FROM dbo.promotions) <> 12
BEGIN
    THROW 50119, 'Expected 12 promotions.', 1;
END;

-- ------------------------------------------------------------
-- 2. NO DUPLICATE CLASS REGISTRATIONS
-- ------------------------------------------------------------

IF EXISTS (
    SELECT 1
    FROM dbo.class_registrations
    GROUP BY user_id,schedule_id
    HAVING COUNT(*) > 1
)
BEGIN
    THROW 50201, 'Duplicate class registration.', 1;
END;

-- ------------------------------------------------------------
-- 3. EXACT REGISTRATION STATUS COUNTS
-- ------------------------------------------------------------

IF (
SELECT COUNT(*)
FROM dbo.class_registrations cr
         JOIN dbo.class_schedules cs ON cs.id = cr.schedule_id
         JOIN dbo.fitness_classes fc ON fc.id = cs.fitness_class_id
WHERE fc.status = 'OPEN'
  AND cr.status = 'REGISTERED'
    ) <> 36
BEGIN
    THROW 50202, 'Expected 36 OPEN registrations.', 1;
END;

IF (
SELECT COUNT(*)
FROM dbo.class_registrations cr
         JOIN dbo.class_schedules cs ON cs.id = cr.schedule_id
         JOIN dbo.fitness_classes fc ON fc.id = cs.fitness_class_id
WHERE fc.status = 'OPEN'
  AND cr.status = 'CANCELLED'
    ) <> 18
BEGIN
    THROW 50203, 'Expected 18 cancelled OPEN registrations.', 1;
END;

IF (
SELECT COUNT(*)
FROM dbo.class_registrations cr
         JOIN dbo.class_schedules cs ON cs.id = cr.schedule_id
         JOIN dbo.fitness_classes fc ON fc.id = cs.fitness_class_id
WHERE fc.status = 'FULL'
  AND cr.status = 'REGISTERED'
    ) <> 18
BEGIN
    THROW 50204, 'Expected 18 FULL registrations.', 1;
END;

IF (
SELECT COUNT(*)
FROM dbo.class_registrations cr
         JOIN dbo.class_schedules cs ON cs.id = cr.schedule_id
         JOIN dbo.fitness_classes fc ON fc.id = cs.fitness_class_id
WHERE fc.status = 'COMPLETED'
  AND cr.status IN ('ATTENDED','ABSENT')
    ) <> 12
BEGIN
    THROW 50205, 'Expected 12 completed class registrations.', 1;
END;

IF (
SELECT COUNT(*)
FROM dbo.class_registrations cr
         JOIN dbo.class_schedules cs ON cs.id = cr.schedule_id
         JOIN dbo.fitness_classes fc ON fc.id = cs.fitness_class_id
WHERE fc.status = 'CANCELLED'
  AND cr.status = 'CANCELLED'
    ) <> 6
BEGIN
    THROW 50206, 'Expected 6 cancelled class registrations.', 1;
END;

-- ------------------------------------------------------------
-- 4. FULL CLASSES MUST HAVE EXACT CAPACITY
-- ------------------------------------------------------------

IF EXISTS (
    SELECT 1
    FROM dbo.fitness_classes fc
    LEFT JOIN dbo.class_schedules cs
        ON cs.fitness_class_id = fc.id
    LEFT JOIN dbo.class_registrations cr
        ON cr.schedule_id = cs.id
       AND cr.status = 'REGISTERED'
    WHERE fc.status = 'FULL'
    GROUP BY fc.id,fc.capacity
    HAVING COUNT(cr.id) <> fc.capacity
)
BEGIN
    THROW 50207, 'FULL class capacity mismatch.', 1;
END;

-- ------------------------------------------------------------
-- 5. OPEN CLASSES MUST HAVE AVAILABLE SEATS
-- ------------------------------------------------------------

IF EXISTS (
    SELECT 1
    FROM dbo.fitness_classes fc
    LEFT JOIN dbo.class_schedules cs
        ON cs.fitness_class_id = fc.id
    LEFT JOIN dbo.class_registrations cr
        ON cr.schedule_id = cs.id
       AND cr.status = 'REGISTERED'
    WHERE fc.status = 'OPEN'
    GROUP BY fc.id,fc.capacity
    HAVING COUNT(cr.id) >= fc.capacity
)
BEGIN
    THROW 50208, 'OPEN class has no available seats.', 1;
END;

-- ------------------------------------------------------------
-- 6. REGISTERED MEMBER MUST HAVE VALID ACTIVE MEMBERSHIP
-- ------------------------------------------------------------

IF EXISTS (
    SELECT 1
    FROM dbo.class_registrations cr
    JOIN dbo.class_schedules cs
        ON cs.id = cr.schedule_id
    JOIN dbo.fitness_classes fc
        ON fc.id = cs.fitness_class_id
    WHERE cr.status = 'REGISTERED'
      AND NOT EXISTS (
          SELECT 1
          FROM dbo.memberships m
          WHERE m.user_id = cr.user_id
            AND m.branch_id = fc.branch_id
            AND m.status = 'ACTIVE'
            AND cs.schedule_date BETWEEN m.start_date AND m.end_date
      )
)
BEGIN
    THROW 50209, 'Registered member has no valid branch membership.', 1;
END;

-- ------------------------------------------------------------
-- 7. REGISTERED MEMBER MUST BE ACTIVE
-- ------------------------------------------------------------

IF EXISTS (
    SELECT 1
    FROM dbo.class_registrations cr
    JOIN dbo.users u ON u.id = cr.user_id
    WHERE cr.status = 'REGISTERED'
      AND (
          u.role <> 'MEMBER'
          OR u.status <> 'ACTIVE'
          OR u.enabled = 0
      )
)
BEGIN
    THROW 50210, 'Invalid registered member.', 1;
END;

-- ------------------------------------------------------------
-- 8. CANCELLED CLASSES
-- ------------------------------------------------------------

IF EXISTS (
    SELECT 1
    FROM dbo.fitness_classes fc
    JOIN dbo.class_schedules cs
        ON cs.fitness_class_id = fc.id
    WHERE fc.status = 'CANCELLED'
      AND cs.active = 1
)
BEGIN
    THROW 50211, 'Cancelled class has active schedule.', 1;
END;

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
    THROW 50212, 'Cancelled class has registered members.', 1;
END;

-- ------------------------------------------------------------
-- 9. MEMBERSHIP DATES
-- ------------------------------------------------------------

IF EXISTS (
    SELECT 1
    FROM dbo.memberships
    WHERE status = 'ACTIVE'
      AND (
          start_date IS NULL
          OR end_date IS NULL
          OR start_date > @today
          OR end_date < @today
          OR end_date <= start_date
      )
)
BEGIN
    THROW 50213, 'Invalid ACTIVE membership.', 1;
END;

IF EXISTS (
    SELECT 1
    FROM dbo.memberships
    WHERE status = 'PENDING'
      AND (
          start_date IS NOT NULL
          OR end_date IS NOT NULL
      )
)
BEGIN
    THROW 50214, 'Invalid PENDING membership.', 1;
END;

IF EXISTS (
    SELECT 1
    FROM dbo.memberships
    WHERE status = 'EXPIRED'
      AND (
          start_date IS NULL
          OR end_date IS NULL
          OR end_date >= @today
      )
)
BEGIN
    THROW 50215, 'Invalid EXPIRED membership.', 1;
END;

-- ------------------------------------------------------------
-- 10. MEMBERSHIP SNAPSHOTS
-- ------------------------------------------------------------

IF EXISTS (
    SELECT 1
    FROM dbo.memberships m
    JOIN dbo.membership_plans mp
        ON mp.id = m.plan_id
    WHERE m.quoted_price IS NULL
       OR m.quoted_duration_days IS NULL
       OR m.quoted_price <> mp.price
       OR m.quoted_duration_days <> mp.duration_days
)
BEGIN
    THROW 50216, 'Invalid membership snapshot.', 1;
END;

-- ------------------------------------------------------------
-- 11. PAYMENTS
-- ------------------------------------------------------------

IF EXISTS (
    SELECT 1
    FROM dbo.payments p
    JOIN dbo.memberships m
        ON m.id = p.membership_id
    WHERE p.user_id <> m.user_id
       OR p.amount <> m.quoted_price
       OR p.created_at < m.created_at
)
BEGIN
    THROW 50217, 'Invalid payment relationship.', 1;
END;

IF EXISTS (
    SELECT 1
    FROM dbo.payments
    WHERE status = 'PAID'
      AND (
          paid_at IS NULL
          OR paid_at < created_at
      )
)
BEGIN
    THROW 50218, 'Invalid PAID payment.', 1;
END;

IF EXISTS (
    SELECT 1
    FROM dbo.payments p
    JOIN dbo.memberships m
        ON m.id = p.membership_id
    WHERE p.method = 'CASH'
      AND p.status = 'PENDING'
      AND m.status <> 'PENDING'
)
BEGIN
    THROW 50219, 'Invalid pending CASH payment.', 1;
END;

-- ------------------------------------------------------------
-- 12. TRAINER REQUESTS
-- ------------------------------------------------------------

IF EXISTS (
    SELECT 1
    FROM dbo.trainer_requests
    WHERE status IN ('PENDING','ACCEPTED')
    GROUP BY member_id
    HAVING COUNT(*) > 1
)
BEGIN
    THROW 50220, 'Member has multiple current PT requests.', 1;
END;

IF EXISTS (
    SELECT 1
    FROM dbo.trainer_requests
    WHERE
        (status = 'PENDING' AND responded_at IS NOT NULL)
        OR
        (status IN ('ACCEPTED','REJECTED') AND responded_at IS NULL)
        OR
        (responded_at < requested_at)
)
BEGIN
    THROW 50221, 'Invalid trainer request timestamps.', 1;
END;

-- ------------------------------------------------------------
-- 13. CONVERSATIONS
-- ------------------------------------------------------------

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
BEGIN
    THROW 50222, 'Conversation without ACCEPTED request.', 1;
END;

-- ------------------------------------------------------------
-- 14. MESSAGE SENDERS
-- ------------------------------------------------------------

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
BEGIN
    THROW 50223, 'Invalid message sender.', 1;
END;

-- ------------------------------------------------------------
-- 15. PERSONAL TRAINING RELATIONSHIPS
-- ------------------------------------------------------------

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
BEGIN
    THROW 50224, 'Personal schedule without ACCEPTED PT.', 1;
END;

IF EXISTS (
    SELECT 1
    FROM dbo.personal_training_schedules ps
    JOIN dbo.trainers t
        ON t.id = ps.trainer_id
    WHERE ps.branch_id IS NULL
       OR ps.branch_id <> t.branch_id
)
BEGIN
    THROW 50225, 'Personal training branch mismatch.', 1;
END;

IF EXISTS (
    SELECT 1
    FROM dbo.personal_training_schedules
    WHERE status IS NULL
       OR start_time >= end_time
       OR (status = 'SCHEDULED' AND active = 0)
       OR (status IN ('COMPLETED','CANCELLED') AND active = 1)
)
BEGIN
    THROW 50226, 'Invalid personal training schedule.', 1;
END;

-- ------------------------------------------------------------
-- 16. CHECKINS
-- ------------------------------------------------------------

IF EXISTS (
    SELECT 1
    FROM dbo.checkins
    WHERE check_in_at > @now
       OR check_out_at > @now
       OR (
           check_out_at IS NOT NULL
           AND check_out_at <= check_in_at
       )
)
BEGIN
    THROW 50227, 'Invalid checkin timestamp.', 1;
END;

IF EXISTS (
    SELECT 1
    FROM dbo.checkins c
    WHERE NOT EXISTS (
        SELECT 1
        FROM dbo.memberships m
        WHERE m.user_id = c.user_id
          AND m.branch_id = c.branch_id
          AND m.status = 'ACTIVE'
          AND CAST(c.check_in_at AS DATE)
              BETWEEN m.start_date AND m.end_date
    )
)
BEGIN
    THROW 50228, 'Checkin without valid membership.', 1;
END;

-- ------------------------------------------------------------
-- 17. NOTIFICATIONS
-- ------------------------------------------------------------

IF EXISTS (
    SELECT 1
    FROM dbo.notifications
    WHERE (is_read = 0 AND read_at IS NOT NULL)
       OR (is_read = 1 AND read_at IS NULL)
       OR read_at < created_at
)
BEGIN
    THROW 50229, 'Invalid notification read status.', 1;
END;

-- ------------------------------------------------------------
-- 18. STATUS COVERAGE
-- ------------------------------------------------------------

IF (SELECT COUNT(DISTINCT status) FROM dbo.fitness_classes) <> 4
BEGIN
    THROW 50230, 'Missing fitness class status.', 1;
END;

IF (SELECT COUNT(DISTINCT status) FROM dbo.memberships) <> 4
BEGIN
    THROW 50231, 'Missing membership status.', 1;
END;

IF (SELECT COUNT(DISTINCT status) FROM dbo.payments) <> 4
BEGIN
    THROW 50232, 'Missing payment status.', 1;
END;

IF (SELECT COUNT(DISTINCT status) FROM dbo.trainer_requests) <> 4
BEGIN
    THROW 50233, 'Missing trainer request status.', 1;
END;

IF (
SELECT COUNT(DISTINCT status)
FROM dbo.personal_training_schedules
    ) <> 3
BEGIN
    THROW 50234, 'Missing personal training status.', 1;
END;

IF (SELECT COUNT(DISTINCT type) FROM dbo.notifications) <> 8
BEGIN
    THROW 50235, 'Missing notification type.', 1;
END;

-- ------------------------------------------------------------
-- 19. PROMOTIONS
-- ------------------------------------------------------------

IF EXISTS (
    SELECT 1
    FROM dbo.promotions
    WHERE discount_percent <= 0
       OR discount_percent > 100
       OR quantity < 0
       OR start_date > end_date
)
BEGIN
    THROW 50236, 'Invalid promotion.', 1;
END;

-- ------------------------------------------------------------
-- 20. REVIEWS
-- ------------------------------------------------------------

IF EXISTS (
    SELECT 1
    FROM dbo.reviews
    WHERE rating NOT BETWEEN 1 AND 5
)
BEGIN
    THROW 50237, 'Invalid review rating.', 1;
END;

-- ------------------------------------------------------------
-- 21. OTP
-- ------------------------------------------------------------

IF EXISTS (
    SELECT 1
    FROM dbo.otp_codes
    WHERE expired_at <= created_at
       OR (used = 0 AND used_at IS NOT NULL)
       OR (used = 1 AND used_at IS NULL)
       OR used_at > expired_at
       OR attempt_count NOT BETWEEN 0 AND 5
)
BEGIN
    THROW 50238, 'Invalid OTP record.', 1;
END;

-- ------------------------------------------------------------
-- 22. EMAIL AND PASSWORD
-- ------------------------------------------------------------

IF EXISTS (
    SELECT 1
    FROM dbo.users
    WHERE email NOT LIKE '%@gmail.com'
       OR password <> @password
)
BEGIN
    THROW 50239, 'Invalid sample email or password hash.', 1;
END;

-- ------------------------------------------------------------
-- 23. ACTIVE TRAINERS
-- ------------------------------------------------------------

IF EXISTS (
    SELECT 1
    FROM dbo.fitness_classes fc
    JOIN dbo.trainers t
        ON t.id = fc.trainer_id
    JOIN dbo.users u
        ON u.id = t.user_id
    WHERE fc.status IN ('OPEN','FULL')
      AND (
          t.active = 0
          OR u.enabled = 0
          OR u.status <> 'ACTIVE'
          OR u.role <> 'PT'
          OR t.branch_id <> fc.branch_id
      )
)
BEGIN
    THROW 50240, 'Active class has invalid trainer.', 1;
END;

-- ------------------------------------------------------------
-- 24. PERSONAL SCHEDULE CREATED AFTER PT ACCEPTANCE
-- ------------------------------------------------------------

IF EXISTS (
    SELECT 1
    FROM dbo.personal_training_schedules ps
    JOIN dbo.trainer_requests tr
        ON tr.member_id = ps.member_id
       AND tr.trainer_id = ps.trainer_id
       AND tr.status = 'ACCEPTED'
    WHERE ps.created_at < tr.responded_at
)
BEGIN
    THROW 50241, 'Personal schedule created before PT acceptance.', 1;
END;

-- ------------------------------------------------------------
-- 25. NO OVERLAPPING ACTIVE PERSONAL SCHEDULES
-- ------------------------------------------------------------

IF EXISTS (
    SELECT 1
    FROM dbo.personal_training_schedules a
    JOIN dbo.personal_training_schedules b
        ON a.id < b.id
       AND a.trainer_id = b.trainer_id
       AND a.schedule_date = b.schedule_date
       AND a.start_time < b.end_time
       AND b.start_time < a.end_time
    WHERE a.active = 1
      AND b.active = 1
)
BEGIN
    THROW 50242, 'Overlapping personal training schedules.', 1;
END;

-- ------------------------------------------------------------
-- 26. NO OVERLAPPING ACTIVE CLASS SCHEDULES
-- ------------------------------------------------------------

IF EXISTS (
    SELECT 1
    FROM dbo.class_schedules a
    JOIN dbo.class_schedules b
        ON a.id < b.id
       AND a.schedule_date = b.schedule_date
       AND a.start_time < b.end_time
       AND b.start_time < a.end_time
    JOIN dbo.fitness_classes fa
        ON fa.id = a.fitness_class_id
    JOIN dbo.fitness_classes fb
        ON fb.id = b.fitness_class_id
    WHERE a.active = 1
      AND b.active = 1
      AND (
          fa.trainer_id = fb.trainer_id
          OR (
              fa.branch_id = fb.branch_id
              AND a.room = b.room
          )
      )
)
BEGIN
    THROW 50243, 'Overlapping active class schedules.', 1;
END;

-- ------------------------------------------------------------
-- 27. EXACTLY THREE FULL CLASSES
-- ------------------------------------------------------------

IF (
SELECT COUNT(*)
FROM dbo.fitness_classes
WHERE status = 'FULL'
    ) <> 3
BEGIN
    THROW 50244, 'Expected exactly 3 FULL classes.', 1;
END;

-- ============================================================
-- COMMIT
-- ============================================================

COMMIT TRANSACTION;

PRINT '=============================================';
PRINT 'UTEFIT SAMPLE DATA INSERTED SUCCESSFULLY';
PRINT 'DATABASE: UTEFitDB';
PRINT 'TABLES: 19';
PRINT 'CLASS REGISTRATIONS: 90';
PRINT 'ALL DATA VALIDATIONS PASSED';
PRINT '=============================================';

END TRY
BEGIN CATCH

IF @@TRANCOUNT > 0
BEGIN
ROLLBACK TRANSACTION;
END;

    PRINT '=============================================';
    PRINT 'UTEFIT SAMPLE DATA INSERT FAILED';
    PRINT ERROR_MESSAGE();
    PRINT 'TRANSACTION ROLLED BACK';
    PRINT '=============================================';

    THROW;

END CATCH;
GO

-- ============================================================
-- FINAL TABLE COUNTS
-- ============================================================

SELECT 'users' AS table_name,COUNT(*) AS total FROM dbo.users
UNION ALL SELECT 'branches',COUNT(*) FROM dbo.branches
UNION ALL SELECT 'class_categories',COUNT(*) FROM dbo.class_categories
UNION ALL SELECT 'membership_plans',COUNT(*) FROM dbo.membership_plans
UNION ALL SELECT 'trainers',COUNT(*) FROM dbo.trainers
UNION ALL SELECT 'fitness_classes',COUNT(*) FROM dbo.fitness_classes
UNION ALL SELECT 'class_schedules',COUNT(*) FROM dbo.class_schedules
UNION ALL SELECT 'class_registrations',COUNT(*) FROM dbo.class_registrations
UNION ALL SELECT 'memberships',COUNT(*) FROM dbo.memberships
UNION ALL SELECT 'payments',COUNT(*) FROM dbo.payments
UNION ALL SELECT 'reviews',COUNT(*) FROM dbo.reviews
UNION ALL SELECT 'otp_codes',COUNT(*) FROM dbo.otp_codes
UNION ALL SELECT 'trainer_requests',COUNT(*) FROM dbo.trainer_requests
UNION ALL SELECT 'checkins',COUNT(*) FROM dbo.checkins
UNION ALL SELECT 'conversations',COUNT(*) FROM dbo.conversations
UNION ALL SELECT 'messages',COUNT(*) FROM dbo.messages
UNION ALL SELECT 'personal_training_schedules',COUNT(*) FROM dbo.personal_training_schedules
UNION ALL SELECT 'notifications',COUNT(*) FROM dbo.notifications
UNION ALL SELECT 'promotions',COUNT(*) FROM dbo.promotions;
GO

-- ============================================================
-- CLASS REGISTRATION SUMMARY
-- ============================================================

SELECT
    fc.status AS class_status,
    cr.status AS registration_status,
    COUNT(*) AS total
FROM dbo.class_registrations cr
         JOIN dbo.class_schedules cs
              ON cs.id = cr.schedule_id
         JOIN dbo.fitness_classes fc
              ON fc.id = cs.fitness_class_id
GROUP BY fc.status,cr.status
ORDER BY fc.status,cr.status;
GO

-- ============================================================
-- FITNESS CLASS CAPACITY
-- ============================================================

SELECT
    fc.id,
    fc.name,
    fc.status,
    fc.capacity,
    COUNT(cr.id) AS registered_count,
    fc.capacity - COUNT(cr.id) AS available_seats
FROM dbo.fitness_classes fc
         LEFT JOIN dbo.class_schedules cs
                   ON cs.fitness_class_id = fc.id
         LEFT JOIN dbo.class_registrations cr
                   ON cr.schedule_id = cs.id
                       AND cr.status = 'REGISTERED'
GROUP BY fc.id,fc.name,fc.status,fc.capacity
ORDER BY fc.id;
GO

-- ============================================================
-- MEMBERSHIP SNAPSHOTS
-- ============================================================

SELECT
    m.id,
    u.email,
    b.name AS branch_name,
    mp.name AS plan_name,
    m.status,
    m.quoted_price,
    m.quoted_duration_days,
    m.start_date,
    m.end_date
FROM dbo.memberships m
         JOIN dbo.users u
              ON u.id = m.user_id
         JOIN dbo.branches b
              ON b.id = m.branch_id
         JOIN dbo.membership_plans mp
              ON mp.id = m.plan_id
ORDER BY m.id;
GO

-- ============================================================
-- PAYMENT SUMMARY
-- ============================================================

SELECT
    method,
    status,
    COUNT(*) AS total,
    SUM(amount) AS total_amount
FROM dbo.payments
GROUP BY method,status
ORDER BY method,status;
GO

-- ============================================================
-- TRAINER REQUEST SUMMARY
-- ============================================================

SELECT
    status,
    COUNT(*) AS total
FROM dbo.trainer_requests
GROUP BY status
ORDER BY status;
GO

-- ============================================================
-- PERSONAL TRAINING SUMMARY
-- ============================================================

SELECT
    status,
    COUNT(*) AS total
FROM dbo.personal_training_schedules
GROUP BY status
ORDER BY status;
GO

-- ============================================================
-- NOTIFICATION TYPES
-- ============================================================

SELECT
    type,
    COUNT(*) AS total,
    SUM(CASE WHEN is_read = 1 THEN 1 ELSE 0 END) AS read_count,
    SUM(CASE WHEN is_read = 0 THEN 1 ELSE 0 END) AS unread_count
FROM dbo.notifications
GROUP BY type
ORDER BY type;
GO

-- ============================================================
-- PROMOTIONS
-- ============================================================

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