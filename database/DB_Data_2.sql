-- UTEFIT DEVELOPMENT DEMO ONLY. Run after the deliberately approved DB_Khung_1.sql.
-- No bank accounts, SePay transactions, webhook payloads, OTPs or Live approval seeded.
-- Idempotent: re-running a completed seed reports existing counts; it does not rewrite history.
SET NOCOUNT ON;
SET XACT_ABORT ON;
IF DB_NAME() NOT IN (N'master',N'UTEFitDB') THROW 51200, 'Do not run this script in LiveDemo, QA or any other database.', 1;
USE [UTEFitDB];
IF @@TRANCOUNT <> 0 THROW 51201, 'Use a new SSMS session.', 1;
IF DB_NAME() <> N'UTEFitDB' THROW 51202, 'Wrong database.', 1;
IF NOT EXISTS (SELECT 1 FROM sys.extended_properties WHERE class=0 AND name=N'UTEFit.DatabasePurpose' AND CONVERT(nvarchar(100),value)=N'DEVELOPMENT_ONLY')
    THROW 51203, 'Missing development-only marker. Use the reviewed schema script, never mark a Live database.', 1;
IF COL_LENGTH('dbo.memberships','quoted_duration_unit') IS NULL THROW 51204, 'V712 schema required.', 1;
IF COL_LENGTH('dbo.payments','cancellation_requested_at') IS NULL THROW 51209,'V714 schema required. Seed does not modify existing Payment statuses or cancellation history.',1;
IF EXISTS(SELECT 1 FROM sys.columns WHERE object_id IN (OBJECT_ID('dbo.branches'),OBJECT_ID('dbo.membership_plans'))
    AND name='name' AND TYPE_NAME(system_type_id)<>'nvarchar')
    THROW 51208,'V713 Unicode catalog schema required. Do not seed Vietnamese into varchar columns.',1;
BEGIN TRY
BEGIN TRANSACTION;
DECLARE @lockResult int;
EXEC @lockResult=sys.sp_getapplock @Resource=N'UTEFit.DemoSeed.v712', @LockMode='Exclusive', @LockOwner='Transaction', @LockTimeout=10000;
IF @lockResult < 0 THROW 51205, 'Cannot acquire seed lock.', 1;
IF EXISTS (SELECT 1 FROM sys.extended_properties WHERE class=0 AND name=N'UTEFit.DemoSeed.Version')
BEGIN
    IF (SELECT COUNT(*) FROM dbo.users WHERE email LIKE '%@utefit.test') <> 55
        THROW 51206, 'Seed marker exists but accounts changed. Inspect; do not overwrite.', 1;
    COMMIT;
    SELECT role,COUNT(*) AS demo_accounts FROM dbo.users WHERE email LIKE '%@utefit.test' GROUP BY role;
    PRINT 'Already seeded. Dates, passwords, financial history and user edits preserved.';
    RETURN;
END;
IF EXISTS (SELECT 1 FROM dbo.users) OR EXISTS (SELECT 1 FROM dbo.branches)
    OR EXISTS (SELECT 1 FROM dbo.payments) OR EXISTS (SELECT 1 FROM dbo.sepay_payment_details)
    OR EXISTS (SELECT 1 FROM dbo.sepay_webhook_transactions) OR EXISTS (SELECT 1 FROM dbo.sepay_bank_reconciliations)
    OR EXISTS (SELECT 1 FROM dbo.sepay_deployment_guard)
    THROW 51207, 'Seed requires empty development business tables. Never merge into existing financial data.', 1;
DECLARE @now datetime2=CONVERT(datetime2,SYSDATETIMEOFFSET() AT TIME ZONE 'SE Asia Standard Time');
DECLARE @today date=CAST(@now AS date);
-- BEGIN DEMO USERS (portable INSERT also used by isolated authentication tests)
INSERT INTO dbo.users(email,password,first_name,last_name,phone,role,status,enabled,token_version,created_at,updated_at) VALUES
('admin01@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','admin01','0000000001','ADMIN','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('admin02@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','admin02','0000000002','ADMIN','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('admin03@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','admin03','0000000003','ADMIN','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('admin04@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','admin04','0000000004','ADMIN','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('admin05@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','admin05','0000000005','ADMIN','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('admin06@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','admin06','0000000006','ADMIN','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('admin07@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','admin07','0000000007','ADMIN','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('admin08@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','admin08','0000000008','ADMIN','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('admin09@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','admin09','0000000009','ADMIN','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('admin10@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','admin10','0000000010','ADMIN','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('member01@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','member01','0000000011','MEMBER','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('member02@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','member02','0000000012','MEMBER','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('member03@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','member03','0000000013','MEMBER','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('member04@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','member04','0000000014','MEMBER','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('member05@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','member05','0000000015','MEMBER','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('member06@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','member06','0000000016','MEMBER','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('member07@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','member07','0000000017','MEMBER','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('member08@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','member08','0000000018','MEMBER','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('member09@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','member09','0000000019','MEMBER','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('member10@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','member10','0000000020','MEMBER','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('member11@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','member11','0000000021','MEMBER','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('member12@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','member12','0000000022','MEMBER','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('member13@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','member13','0000000023','MEMBER','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('member14@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','member14','0000000024','MEMBER','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('member15@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','member15','0000000025','MEMBER','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('member16@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','member16','0000000026','MEMBER','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('member17@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','member17','0000000027','MEMBER','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('member18@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','member18','0000000028','MEMBER','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('member19@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','member19','0000000029','MEMBER','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('member20@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','member20','0000000030','MEMBER','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('pt01@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','pt01','0000000031','PT','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('pt02@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','pt02','0000000032','PT','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('pt03@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','pt03','0000000033','PT','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('pt04@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','pt04','0000000034','PT','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('pt05@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','pt05','0000000035','PT','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('pt06@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','pt06','0000000036','PT','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('pt07@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','pt07','0000000037','PT','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('pt08@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','pt08','0000000038','PT','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('pt09@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','pt09','0000000039','PT','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('pt10@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','pt10','0000000040','PT','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('pt11@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','pt11','0000000041','PT','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('pt12@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','pt12','0000000042','PT','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('pt13@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','pt13','0000000043','PT','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('pt14@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','pt14','0000000044','PT','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('pt15@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','pt15','0000000045','PT','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('pt16@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','pt16','0000000046','PT','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('pt17@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','pt17','0000000047','PT','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('pt18@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','pt18','0000000048','PT','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('pt19@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','pt19','0000000049','PT','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('pt20@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','pt20','0000000050','PT','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('manager01@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','manager01','0000000051','MANAGER','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('manager02@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','manager02','0000000052','MANAGER','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('manager03@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','manager03','0000000053','MANAGER','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('manager04@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','manager04','0000000054','MANAGER','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
('manager05@utefit.test','$2a$10$R6rGlbifygfAoV3RaV1.9uXDQSo2rj/IkzMPhjBraossFx2Nd2sJC','Demo','manager05','0000000055','MANAGER','ACTIVE',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);
-- END DEMO USERS

DECLARE @Branches TABLE(ordinal int PRIMARY KEY,id bigint);
DECLARE @branch int=1, @branchId bigint, @trainer int, @userId bigint;
WHILE @branch <= 5
BEGIN
    INSERT dbo.branches(name,address,phone,active,opening_time,closing_time,created_at,updated_at)
    VALUES(CONCAT(N'UTEFit - Chi nhánh ',@branch),CONCAT(N'Địa chỉ chi nhánh demo ',@branch),CONCAT('000000100',@branch),1,'06:00','22:00',@now,@now);
    SET @branchId=SCOPE_IDENTITY();
    INSERT @Branches VALUES(@branch,@branchId);
    INSERT dbo.manager_branch_assignments(manager_id,branch_id)
        SELECT id,@branchId FROM dbo.users WHERE email=CONCAT('manager0',@branch,'@utefit.test');
    SET @branch+=1;
END;
DECLARE @Trainers TABLE(ordinal int PRIMARY KEY,id bigint,branch_ordinal int);
SET @trainer=1;
WHILE @trainer <= 20
BEGIN
    SET @branch=(@trainer-1)/4+1;
    SELECT @branchId=id FROM @Branches WHERE ordinal=@branch;
    SELECT @userId=id FROM dbo.users WHERE email=CONCAT('pt',RIGHT(CONCAT('0',@trainer),2),'@utefit.test');
    INSERT dbo.trainers(user_id,branch_id,specialization,years_experience,bio,active,created_at,updated_at)
    VALUES(@userId,@branchId,'Gym / Yoga / PT - DEMO',2,'Fictional development trainer, no real personal data.',1,@now,@now);
    INSERT @Trainers VALUES(@trainer,SCOPE_IDENTITY(),@branch);
    SET @trainer+=1;
END;

DECLARE @Kinds TABLE(kind int PRIMARY KEY,label nvarchar(80),quantity int,unit varchar(10),bonus int,bonus_unit varchar(10),benefit varchar(20),price decimal(15,2));
INSERT @Kinds VALUES
(1,N'Gói tập 1 ngày',1,'DAY',0,'DAY','GYM',20000),
(2,N'Gói tập 7 ngày',7,'DAY',0,'DAY','GYM',120000),
(3,N'Gói tập 2 tuần',2,'WEEK',0,'DAY','YOGA',180000),
(4,N'Gói tập 1 tháng',1,'MONTH',0,'DAY','ALL',300000),
(5,N'Gói tập 3 tháng',3,'MONTH',0,'DAY','PT',900000),
(6,N'Gói tập 6 tháng',6,'MONTH',0,'DAY','ALL',1500000),
(7,N'Gói tập 1 năm',1,'YEAR',0,'DAY','GYM',2800000),
(8,N'Gói tập 2 năm',2,'YEAR',0,'DAY','ALL',5600000),
(9,N'Gói tập 3 tháng - Tặng thêm 2 tuần',3,'MONTH',2,'WEEK','PT',950000),
(10,N'Gói tập 6 tháng - Tặng thêm 1 tháng',6,'MONTH',1,'MONTH','ALL',1600000),
(11,N'Gói tập 1 năm - Tặng thêm 2 tháng',1,'YEAR',2,'MONTH','ALL',3200000);
DECLARE @Plans TABLE(branch_ordinal int,kind int,id bigint,PRIMARY KEY(branch_ordinal,kind));
DECLARE @kind int,@quantity int,@unit varchar(10),@bonus int,@bonusUnit varchar(10),@label nvarchar(80),@benefit varchar(20),@catalogPrice decimal(15,2),@boundary date,@planId bigint;
SET @branch=1;
WHILE @branch<=5
BEGIN
    SELECT @branchId=id FROM @Branches WHERE ordinal=@branch;
    SET @kind=1;
    WHILE @kind<=11
    BEGIN
        SELECT @quantity=quantity,@unit=unit,@bonus=bonus,@bonusUnit=bonus_unit,@label=label,@benefit=benefit,@catalogPrice=price FROM @Kinds WHERE kind=@kind;
        SET @boundary=CASE @unit WHEN 'DAY' THEN DATEADD(DAY,@quantity,@today) WHEN 'WEEK' THEN DATEADD(WEEK,@quantity,@today)
            WHEN 'MONTH' THEN DATEADD(MONTH,@quantity,@today) WHEN 'YEAR' THEN DATEADD(YEAR,@quantity,@today) END;
        SET @boundary=CASE @bonusUnit WHEN 'DAY' THEN DATEADD(DAY,@bonus,@boundary) WHEN 'WEEK' THEN DATEADD(WEEK,@bonus,@boundary)
            WHEN 'MONTH' THEN DATEADD(MONTH,@bonus,@boundary) WHEN 'YEAR' THEN DATEADD(YEAR,@bonus,@boundary) END;
        INSERT dbo.membership_plans(name,description,duration_days,price,active,created_at,updated_at,benefit,access_scope,duration_unit,duration_quantity,bonus_unit,bonus_quantity)
        VALUES(@label,N'Gói tập demo áp dụng tại một chi nhánh cố định.',DATEDIFF(DAY,@today,@boundary),
            @catalogPrice,1,@now,@now,@benefit,'SINGLE_BRANCH',@unit,@quantity,@bonusUnit,@bonus);
        SET @planId=SCOPE_IDENTITY();
        INSERT @Plans VALUES(@branch,@kind,@planId);
        INSERT dbo.membership_plan_branches(plan_id,branch_id) VALUES(@planId,@branchId);
        SET @kind+=1;
    END;
    SET @branch+=1;
END;

DECLARE @Spec TABLE(ordinal int IDENTITY PRIMARY KEY,seed_key varchar(30),member_number int,branch_number int,kind int,state varchar(20),previous_key varchar(30));
INSERT @Spec(seed_key,member_number,branch_number,kind,state,previous_key) VALUES
('M02A',2,1,4,'CURRENT',NULL),('M03A',3,1,4,'EXPIRED',NULL),
('M04A',4,1,4,'CURRENT',NULL),('M04NEXT',4,1,4,'FUTURE','M04A'),
('M05A',5,1,4,'CURRENT',NULL),('M05B',5,2,4,'CURRENT',NULL),
('M06A',6,1,4,'PENDING',NULL);
DECLARE @number int=7;
WHILE @number<=20
BEGIN
    INSERT @Spec(seed_key,member_number,branch_number,kind,state)
        VALUES(CONCAT('M',@number),@number,(@number-7)%5+1,CASE @number WHEN 19 THEN 7 WHEN 20 THEN 8 ELSE 4 END,'CURRENT');
    SET @number+=1;
END;
DECLARE @Memberships TABLE(seed_key varchar(30) PRIMARY KEY,id bigint,member_number int,branch_number int,end_date date);
DECLARE @index int=1,@seedKey varchar(30),@state varchar(20),@previousKey varchar(30),@previousId bigint,@start date,@end date,@membershipId bigint,@price decimal(15,2),@paymentId bigint,@paidMoment datetime2,@createdMoment datetime2;
WHILE @index<=(SELECT COUNT(*) FROM @Spec)
BEGIN
    SELECT @seedKey=seed_key,@number=member_number,@branch=branch_number,@kind=kind,@state=state,@previousKey=previous_key FROM @Spec WHERE ordinal=@index;
    SELECT @userId=id FROM dbo.users WHERE email=CONCAT('member',RIGHT(CONCAT('0',@number),2),'@utefit.test');
    SELECT @branchId=id FROM @Branches WHERE ordinal=@branch;
    SELECT @planId=id FROM @Plans WHERE branch_ordinal=@branch AND kind=@kind;
    SELECT @quantity=duration_quantity,@unit=duration_unit,@bonus=bonus_quantity,@bonusUnit=bonus_unit,@price=price,@benefit=benefit FROM dbo.membership_plans WHERE id=@planId;
    SET @previousId=NULL;
    SET @start=DATEADD(DAY,CASE WHEN @state='EXPIRED' THEN -60 ELSE -7 END,@today);
    IF @state='PENDING' SET @start=@today;
    IF @previousKey IS NOT NULL SELECT @previousId=id,@start=DATEADD(DAY,1,end_date) FROM @Memberships WHERE seed_key=@previousKey;
    SET @boundary=CASE @unit WHEN 'DAY' THEN DATEADD(DAY,@quantity,@start) WHEN 'WEEK' THEN DATEADD(WEEK,@quantity,@start)
        WHEN 'MONTH' THEN DATEADD(MONTH,@quantity,@start) WHEN 'YEAR' THEN DATEADD(YEAR,@quantity,@start) END;
    SET @boundary=CASE @bonusUnit WHEN 'DAY' THEN DATEADD(DAY,@bonus,@boundary) WHEN 'WEEK' THEN DATEADD(WEEK,@bonus,@boundary)
        WHEN 'MONTH' THEN DATEADD(MONTH,@bonus,@boundary) WHEN 'YEAR' THEN DATEADD(YEAR,@bonus,@boundary) END;
    SET @end=DATEADD(DAY,-1,@boundary);
    SET @paidMoment=CASE WHEN @state IN ('CURRENT','EXPIRED') THEN DATEADD(HOUR,8,CAST(@start AS datetime2)) WHEN @state='FUTURE' THEN @now ELSE NULL END;
    SET @createdMoment=CASE WHEN @state IN ('CURRENT','EXPIRED') THEN DATEADD(MINUTE,-15,@paidMoment) ELSE @now END;
    INSERT dbo.memberships(user_id,branch_id,plan_id,status,created_at,updated_at,start_date,end_date,quoted_price,quoted_duration_days,
        quoted_benefit,quoted_scope,renewal_of_id,quoted_duration_unit,quoted_duration_quantity,quoted_bonus_unit,quoted_bonus_quantity)
    VALUES(@userId,@branchId,@planId,CASE @state WHEN 'PENDING' THEN 'PENDING' WHEN 'EXPIRED' THEN 'EXPIRED' ELSE 'ACTIVE' END,
        @createdMoment,@now,CASE WHEN @state='PENDING' THEN NULL ELSE @start END,CASE WHEN @state='PENDING' THEN NULL ELSE @end END,
        @price,DATEDIFF(DAY,@start,@boundary),@benefit,'SINGLE_BRANCH',@previousId,@unit,@quantity,@bonusUnit,@bonus);
    SET @membershipId=SCOPE_IDENTITY();
    INSERT @Memberships VALUES(@seedKey,@membershipId,@number,@branch,@end);
    INSERT dbo.membership_branch_snapshot(membership_id,branch_id) VALUES(@membershipId,@branchId);
    INSERT dbo.payments(user_id,membership_id,amount,method,status,created_at,paid_at,transaction_code)
    VALUES(@userId,@membershipId,@price,'CASH',CASE WHEN @state='PENDING' THEN 'PENDING' ELSE 'PAID' END,
        @createdMoment,@paidMoment,CONCAT('DEMO-CASH-',@seedKey));
    SET @index+=1;
END;

DECLARE @Categories TABLE(kind int PRIMARY KEY,id bigint);
DECLARE @CategoriesSpec TABLE(kind int,label varchar(40),benefit varchar(20));
INSERT @CategoriesSpec VALUES(1,'Gym DEMO','GYM'),(2,'Yoga DEMO','YOGA'),(3,'PT DEMO','PT');
SET @kind=1;
WHILE @kind<=3
BEGIN
    INSERT dbo.class_categories(name,description,active,created_at,updated_at,required_benefit)
        SELECT label,'Demo exercise category',1,@now,@now,benefit FROM @CategoriesSpec WHERE kind=@kind;
    INSERT @Categories VALUES(@kind,SCOPE_IDENTITY()); SET @kind+=1;
END;
DECLARE @Schedules TABLE(branch_number int,kind int,future bit,id bigint,PRIMARY KEY(branch_number,kind,future));
DECLARE @classId bigint,@trainerId bigint,@categoryId bigint;
SET @branch=1;
WHILE @branch<=5
BEGIN
    SELECT @branchId=id FROM @Branches WHERE ordinal=@branch;
    SET @kind=1;
    WHILE @kind<=3
    BEGIN
        SELECT @categoryId=id FROM @Categories WHERE kind=@kind;
        SELECT @trainerId=id FROM @Trainers WHERE ordinal=(@branch-1)*4+@kind;
        INSERT dbo.fitness_classes(name,description,branch_id,category_id,trainer_id,capacity,status,created_at,updated_at)
        VALUES(CONCAT('DEMO B',@branch,' class ',@kind),'Development group class',@branchId,@categoryId,@trainerId,10,'OPEN',@now,@now);
        SET @classId=SCOPE_IDENTITY();
        INSERT dbo.class_schedules(fitness_class_id,schedule_date,start_time,end_time,room,active,created_at)
        VALUES(@classId,DATEADD(DAY,-1,@today),TIMEFROMPARTS(8+2*@kind,0,0,0,0),TIMEFROMPARTS(9+2*@kind,0,0,0,0),CONCAT('Room ',@kind),1,@now);
        INSERT @Schedules VALUES(@branch,@kind,0,SCOPE_IDENTITY());
        INSERT dbo.class_schedules(fitness_class_id,schedule_date,start_time,end_time,room,active,created_at)
        VALUES(@classId,DATEADD(DAY,3,@today),TIMEFROMPARTS(8+2*@kind,0,0,0,0),TIMEFROMPARTS(9+2*@kind,0,0,0,0),CONCAT('Room ',@kind),1,@now);
        INSERT @Schedules VALUES(@branch,@kind,1,SCOPE_IDENTITY());
        SET @kind+=1;
    END;
    SET @branch+=1;
END;
INSERT dbo.class_registrations(user_id,schedule_id,status,registered_at)
    SELECT u.id,s.id,'REGISTERED',@now FROM dbo.users u CROSS JOIN @Schedules s WHERE
    (u.email='member02@utefit.test' AND s.branch_number=1 AND s.kind=1 AND s.future=1)
    OR (u.email='member07@utefit.test' AND s.branch_number=1 AND s.kind=2 AND s.future=1);
INSERT dbo.class_registrations(user_id,schedule_id,status,registered_at)
    SELECT u.id,s.id,'ATTENDED',DATEADD(DAY,-2,@now) FROM dbo.users u CROSS JOIN @Schedules s
    WHERE u.email='member02@utefit.test' AND s.branch_number=1 AND s.kind=1 AND s.future=0;
DECLARE @futureDate date=(SELECT DATEADD(DAY,3,m.start_date) FROM dbo.memberships m JOIN @Memberships seed ON seed.id=m.id WHERE seed.seed_key='M04NEXT');
SELECT @classId=fitness_class_id FROM dbo.class_schedules WHERE id=(SELECT id FROM @Schedules WHERE branch_number=1 AND kind=3 AND future=1);
INSERT dbo.class_schedules(fitness_class_id,schedule_date,start_time,end_time,room,active,created_at)
VALUES(@classId,@futureDate,'14:00','15:00','Room 3',1,@now);
INSERT dbo.class_registrations(user_id,schedule_id,status,registered_at)
    SELECT id,SCOPE_IDENTITY(),'REGISTERED',@now FROM dbo.users WHERE email='member04@utefit.test';

INSERT dbo.checkins(user_id,branch_id,check_in_at,check_out_at)
SELECT m.user_id,m.branch_id,DATEADD(HOUR,7+2*b.ordinal,CAST(DATEADD(DAY,-1,@today) AS datetime2)),
    DATEADD(HOUR,8+2*b.ordinal,CAST(DATEADD(DAY,-1,@today) AS datetime2))
FROM dbo.memberships m JOIN @Branches b ON b.id=m.branch_id WHERE m.status='ACTIVE' AND m.start_date<=@today AND m.end_date>=@today;
INSERT dbo.checkins(user_id,branch_id,check_in_at)
SELECT m.user_id,m.branch_id,DATEADD(MINUTE,-30,@now) FROM dbo.memberships m JOIN @Memberships seed ON seed.id=m.id WHERE seed.member_number=20;

INSERT dbo.reviews(user_id,branch_id,trainer_id,rating,comment,visible,created_at,updated_at)
SELECT c.user_id,c.branch_id,t.id,5,'DEMO: fictional gym experience, seeded for development only.',
    CASE WHEN u.email='member20@utefit.test' THEN 0 ELSE 1 END,@now,@now
FROM dbo.checkins c JOIN dbo.users u ON u.id=c.user_id JOIN @Branches b ON b.id=c.branch_id
JOIN @Trainers t ON t.ordinal=(b.ordinal-1)*4+1 WHERE c.check_out_at IS NOT NULL;

SELECT @trainerId=id FROM @Trainers WHERE ordinal=1;
INSERT dbo.trainer_requests(member_id,trainer_id,status,message,requested_at,responded_at)
    SELECT id,@trainerId,'ACCEPTED','DEMO accepted coaching request',@now,@now FROM dbo.users WHERE email IN ('member02@utefit.test','member04@utefit.test');
INSERT dbo.personal_training_schedules(trainer_id,member_id,branch_id,schedule_date,start_time,end_time,status,active,title,note,created_at,updated_at)
    SELECT @trainerId,u.id,b.id,CASE WHEN u.email='member04@utefit.test' THEN DATEADD(DAY,1,@futureDate) ELSE DATEADD(DAY,2,@today) END,
    '16:00','17:00','SCHEDULED',1,'DEMO PT session','Development only',@now,@now
    FROM dbo.users u CROSS JOIN @Branches b WHERE u.email IN ('member02@utefit.test','member04@utefit.test') AND b.ordinal=1;

SELECT @userId=id FROM dbo.users WHERE email='member02@utefit.test';
INSERT dbo.conversations(member_id,trainer_id,created_at,updated_at) VALUES(@userId,@trainerId,@now,@now);
DECLARE @conversationId bigint=SCOPE_IDENTITY();
INSERT dbo.messages(conversation_id,sender_id,content,sent_at)
VALUES(@conversationId,@userId,N'DEMO: Hello trainer.',@now),
    (@conversationId,(SELECT user_id FROM dbo.trainers WHERE id=@trainerId),N'DEMO: Your session is scheduled.',@now);
INSERT dbo.favorites(user_id,target_type,target_id,created_at)
VALUES(@userId,'TRAINER',@trainerId,@now),(@userId,'BRANCH',(SELECT id FROM @Branches WHERE ordinal=1),@now),
    (@userId,'CLASS',@classId,@now);
INSERT dbo.notifications(user_id,type,title,content,is_read,created_at)
SELECT id,'SCHEDULE_REMINDER','UTEFit DEMO account','Fictional development data. No bank transfer is recorded by this seed.',0,@now FROM dbo.users;
INSERT dbo.promotions(code,discount_percent,quantity,start_date,end_date,active,created_at)
VALUES('DEMO5',5,100,DATEADD(DAY,-7,@today),DATEADD(DAY,90,@today),1,@now),
('DEMO10',10,100,DATEADD(DAY,-7,@today),DATEADD(DAY,90,@today),1,@now),
('DEMO20',20,100,DATEADD(DAY,-7,@today),DATEADD(DAY,90,@today),1,@now);

IF (SELECT COUNT(*) FROM dbo.users)<>55 OR (SELECT COUNT(*) FROM dbo.users WHERE role='ADMIN')<>10
    OR (SELECT COUNT(*) FROM dbo.users WHERE role='MEMBER')<>20 OR (SELECT COUNT(*) FROM dbo.users WHERE role='PT')<>20
    OR (SELECT COUNT(*) FROM dbo.users WHERE role='MANAGER')<>5 THROW 51208,'Account count mismatch.',1;
IF EXISTS (SELECT 1 FROM dbo.memberships m WHERE m.status IN ('ACTIVE','EXPIRED')
    AND NOT EXISTS (SELECT 1 FROM dbo.payments p WHERE p.membership_id=m.id AND p.status='PAID'))
    THROW 51209,'Membership/payment mismatch.',1;
IF EXISTS (SELECT 1 FROM dbo.sepay_deployment_guard) OR EXISTS (SELECT 1 FROM dbo.sepay_payment_details)
    OR EXISTS (SELECT 1 FROM dbo.sepay_webhook_transactions) OR EXISTS (SELECT 1 FROM dbo.sepay_bank_reconciliations)
    THROW 51210,'Financial safety tables must stay empty.',1;
EXEC sys.sp_addextendedproperty @name=N'UTEFit.DemoSeed.Version',@value=N'V712-demo55';
COMMIT TRANSACTION;
SELECT role,COUNT(*) AS demo_accounts FROM dbo.users GROUP BY role;
PRINT 'Development seed complete. Demo passwords are development-only. No SePay Live approval or bank data created.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
