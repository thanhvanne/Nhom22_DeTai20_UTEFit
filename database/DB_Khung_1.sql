-- UTEFIT DEVELOPMENT REBUILD ONLY. BACK UP UTEFitDB FIRST.
-- 28 tables: reviewed JPA core + embedded V701-V712. No external SQL includes.
-- One batch, no GO: refusing the guard cannot fall through to later batches.
SET NOCOUNT ON;
DECLARE @AllowReset bit = 0;
DECLARE @ResetConfirmation nvarchar(100) = N'';
IF @@TRANCOUNT <> 0 THROW 51100, 'Use a new SSMS session with no transaction.', 1;
IF DB_NAME() NOT IN (N'master', N'UTEFitDB') THROW 51101, 'Wrong session database. Only master or UTEFitDB is allowed.', 1;
IF @AllowReset <> 1 OR @ResetConfirmation <> N'RESET_UTEFitDB_AFTER_BACKUP'
    THROW 51102, 'STOP: back up UTEFitDB, review this file, then deliberately set BOTH reset confirmations.', 1;
USE [master];
BEGIN TRY
    IF DB_ID(N'UTEFitDB') IS NOT NULL
    BEGIN
        ALTER DATABASE [UTEFitDB] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
        DROP DATABASE [UTEFitDB];
    END;
    EXEC(N'CREATE DATABASE [UTEFitDB]');
    EXEC sys.sp_executesql N'USE [UTEFitDB]; SET XACT_ABORT ON; BEGIN TRANSACTION;
-- BEGIN JPA CORE SNAPSHOT
create table branches (active bit not null, closing_time time, opening_time time, created_at datetime2(7) not null, id bigint identity not null, updated_at datetime2(7), phone varchar(20), name nvarchar(150) not null, description nvarchar(1000), address nvarchar(255) not null, image_public_id varchar(255), image_url varchar(255), primary key (id));
create table checkins (branch_id bigint not null, check_in_at datetime2(7) not null, check_out_at datetime2(7), id bigint identity not null, user_id bigint not null, primary key (id));
create table class_categories (active bit not null, created_at datetime2(7) not null, id bigint identity not null, updated_at datetime2(7), name varchar(100) not null, description varchar(1000), primary key (id));
create table class_registrations (id bigint identity not null, registered_at datetime2(7) not null, schedule_id bigint not null, user_id bigint not null, status varchar(20) not null check ((status in (''REGISTERED'',''CANCELLED'',''ATTENDED'',''ABSENT''))), primary key (id));
create table class_schedules (active bit not null, end_time time not null, schedule_date date not null, start_time time not null, created_at datetime2(7) not null, fitness_class_id bigint not null, id bigint identity not null, room varchar(100), primary key (id));
create table conversations (created_at datetime2(7) not null, id bigint identity not null, member_id bigint not null, trainer_id bigint not null, updated_at datetime2(7), primary key (id));
create table fitness_classes (capacity int not null, branch_id bigint not null, category_id bigint not null, created_at datetime2(7) not null, id bigint identity not null, trainer_id bigint, updated_at datetime2(7), status varchar(20) not null check ((status in (''OPEN'',''FULL'',''CANCELLED'',''COMPLETED''))), name varchar(150) not null, description varchar(1500), primary key (id));
create table membership_plans (active bit not null, duration_days int not null, price numeric(15,2) not null, created_at datetime2(7) not null, id bigint identity not null, updated_at datetime2(7), name nvarchar(100) not null, description nvarchar(1500), primary key (id));
create table memberships (promotion_code varchar(50), original_price numeric(15,2), discount_percent numeric(5,2), end_date date, quoted_duration_days int, quoted_price numeric(15,2), start_date date, branch_id bigint not null, created_at datetime2(7) not null, id bigint identity not null, plan_id bigint not null, updated_at datetime2(7), user_id bigint not null, status varchar(20) not null check ((status in (''PENDING'',''ACTIVE'',''EXPIRED'',''CANCELLED''))), primary key (id));
create table messages (conversation_id bigint not null, id bigint identity not null, read_at datetime2(7), sender_id bigint not null, sent_at datetime2(7) not null, content nvarchar(2000) not null, primary key (id));
create table notifications (is_read bit not null, created_at datetime2(7) not null, id bigint identity not null, read_at datetime2(7), related_id bigint, user_id bigint not null, type varchar(50) not null check ((type in (''TRAINER_REQUEST_SENT'',''TRAINER_REQUEST_ACCEPTED'',''TRAINER_REQUEST_REJECTED'',''PERSONAL_SCHEDULE_CREATED'',''CLASS_SCHEDULE_CANCELLED'',''MESSAGE_RECEIVED'',''MEMBERSHIP_ACTIVATED'',''PERSONAL_SCHEDULE_CANCELLED'',''SCHEDULE_REMINDER''))), title varchar(200) not null, content varchar(2000), primary key (id));
create table otp_codes (attempt_count int, used bit not null, created_at datetime2(7) not null, expired_at datetime2(7) not null, id bigint identity not null, used_at datetime2(7), user_id bigint not null, type varchar(30) not null check ((type in (''REGISTER'',''FORGOT_PASSWORD''))), code varchar(100) not null, primary key (id));
create table payments (amount numeric(15,2) not null, created_at datetime2(7) not null, id bigint identity not null, membership_id bigint, paid_at datetime2(7), user_id bigint not null, gateway_response_code varchar(10), method varchar(20) not null check ((method in (''CASH'',''VNPAY'',''SEPAY''))), status varchar(20) not null check ((status in (''PENDING'',''PAID'',''FAILED'',''REFUNDED''))), gateway_transaction_no varchar(100), transaction_code varchar(100), primary key (id));
create table personal_training_schedules (active bit not null, end_time time not null, schedule_date date not null, start_time time not null, branch_id bigint, created_at datetime2(7) not null, id bigint identity not null, member_id bigint not null, trainer_id bigint not null, updated_at datetime2(7), status varchar(20) check ((status in (''SCHEDULED'',''COMPLETED'',''CANCELLED''))), note varchar(2000), title varchar(255), primary key (id));
create table promotions (active bit not null, discount_percent numeric(5,2) not null, end_date date not null, quantity int not null, start_date date not null, created_at datetime2(7) not null, id bigint identity not null, code varchar(50) not null, primary key (id));
create table reviews (rating int not null, visible bit not null, branch_id bigint not null, created_at datetime2(7) not null, id bigint identity not null, trainer_id bigint, updated_at datetime2(7), user_id bigint not null, image_url nvarchar(500), video_url varchar(500), comment nvarchar(2000), image_public_id varchar(255), video_public_id varchar(255), primary key (id));
create table trainer_requests (id bigint identity not null, member_id bigint not null, requested_at datetime2(7) not null, responded_at datetime2(7), trainer_id bigint not null, status varchar(20) not null check ((status in (''PENDING'',''ACCEPTED'',''REJECTED'',''CANCELLED''))), message varchar(2000), primary key (id));
create table trainers (active bit not null, years_experience int, branch_id bigint, created_at datetime2(7) not null, id bigint identity not null, updated_at datetime2(7), user_id bigint not null, bio varchar(2000), specialization varchar(255), primary key (id));
create table users (enabled bit not null, token_version int, created_at datetime2(7) not null, id bigint identity not null, updated_at datetime2(7), phone varchar(20), role varchar(20) not null check ((role in (''MEMBER'',''PT'',''ADMIN'',''MANAGER''))), status varchar(20) not null check ((status in (''UNVERIFIED'',''ACTIVE'',''LOCKED''))), first_name varchar(50) not null, last_name varchar(50) not null, email varchar(100) not null, avatar_public_id varchar(255), avatar_url varchar(255), password varchar(255) not null, primary key (id));
create index idx_checkins_user on checkins (user_id);
create index idx_checkins_branch on checkins (branch_id);
create index idx_checkins_time on checkins (check_in_at);
alter table class_registrations add constraint uk_registration_user_schedule unique (user_id, schedule_id);
alter table conversations add constraint uk_conversation_member_trainer unique (member_id, trainer_id);
alter table promotions add constraint uk_promotions_code unique (code);
alter table trainers add constraint uk_trainers_user unique (user_id);
alter table users add constraint UK6dotkott2kjsp8vw4d0m25fb7 unique (email);
alter table checkins add constraint FK762xn9jq7gjkxxq8uo6acj9xx foreign key (branch_id) references branches;
alter table checkins add constraint FK81gcnk1emrj19gv3x67u4f8bx foreign key (user_id) references users;
alter table class_registrations add constraint fk_registration_schedule foreign key (schedule_id) references class_schedules;
alter table class_registrations add constraint fk_registration_user foreign key (user_id) references users;
alter table class_schedules add constraint fk_schedule_class foreign key (fitness_class_id) references fitness_classes;
alter table conversations add constraint fk_conversation_member foreign key (member_id) references users;
alter table conversations add constraint fk_conversation_trainer foreign key (trainer_id) references trainers;
alter table fitness_classes add constraint fk_class_branch foreign key (branch_id) references branches;
alter table fitness_classes add constraint fk_class_category foreign key (category_id) references class_categories;
alter table fitness_classes add constraint fk_class_trainer foreign key (trainer_id) references trainers;
alter table memberships add constraint fk_membership_branch foreign key (branch_id) references branches;
alter table memberships add constraint fk_membership_plan foreign key (plan_id) references membership_plans;
alter table memberships add constraint fk_membership_user foreign key (user_id) references users;
alter table messages add constraint fk_message_conversation foreign key (conversation_id) references conversations;
alter table messages add constraint fk_message_sender foreign key (sender_id) references users;
alter table notifications add constraint fk_notification_user foreign key (user_id) references users;
alter table otp_codes add constraint FKt9j4to767daef0ws3fiixis0y foreign key (user_id) references users;
alter table payments add constraint fk_payment_membership foreign key (membership_id) references memberships;
alter table payments add constraint fk_payment_user foreign key (user_id) references users;
alter table personal_training_schedules add constraint FK53gn8yc6bhbcn04idkim9ls9b foreign key (branch_id) references branches;
alter table personal_training_schedules add constraint fk_pts_member foreign key (member_id) references users;
alter table personal_training_schedules add constraint fk_pts_trainer foreign key (trainer_id) references trainers;
alter table reviews add constraint fk_review_branch foreign key (branch_id) references branches;
alter table reviews add constraint fk_review_trainer foreign key (trainer_id) references trainers;
alter table reviews add constraint fk_review_user foreign key (user_id) references users;
alter table trainer_requests add constraint FKkfcyiuev78jn0as0f2nrc4lho foreign key (member_id) references users;
alter table trainer_requests add constraint FK7e51s0kebydwotfh1v24gnecx foreign key (trainer_id) references trainers;
alter table trainers add constraint fk_trainer_branch foreign key (branch_id) references branches;
alter table trainers add constraint fk_trainer_user foreign key (user_id) references users;
-- END JPA CORE SNAPSHOT
COMMIT TRANSACTION;';
    -- BEGIN EMBEDDED V701__vnpay.sql
    EXEC sys.sp_executesql N'USE [UTEFitDB];
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH(''dbo.payments'', ''gateway_transaction_no'') IS NULL
    ALTER TABLE dbo.payments ADD gateway_transaction_no varchar(100) NULL;
IF COL_LENGTH(''dbo.payments'', ''gateway_response_code'') IS NULL
    ALTER TABLE dbo.payments ADD gateway_response_code varchar(10) NULL;
COMMIT TRANSACTION;';
    -- END EMBEDDED V701__vnpay.sql
    -- BEGIN EMBEDDED V702__qr_checkin.sql
    EXEC sys.sp_executesql N'USE [UTEFitDB];
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF OBJECT_ID(''dbo.qr_checkin_tokens'', ''U'') IS NULL
BEGIN
    CREATE TABLE dbo.qr_checkin_tokens (
        id bigint IDENTITY PRIMARY KEY,
        user_id bigint NOT NULL REFERENCES dbo.users(id),
        token_hash varchar(64) NOT NULL,
        account_version int NOT NULL,
        expires_at datetime2 NOT NULL,
        used_at datetime2 NULL,
        CONSTRAINT uk_qr_token_hash UNIQUE(token_hash)
    );
END;
COMMIT TRANSACTION;';
    -- END EMBEDDED V702__qr_checkin.sql
    -- BEGIN EMBEDDED V703__favorites.sql
    EXEC sys.sp_executesql N'USE [UTEFitDB];
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF OBJECT_ID(N''dbo.favorites'', N''U'') IS NULL
BEGIN
    CREATE TABLE dbo.favorites (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        user_id BIGINT NOT NULL,
        target_type VARCHAR(20) NOT NULL,
        target_id BIGINT NOT NULL,
        created_at DATETIME2 NOT NULL,
        CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES dbo.users(id),
        CONSTRAINT ck_favorites_type CHECK (target_type IN (''TRAINER'',''CLASS'',''BRANCH'')),
        CONSTRAINT uk_favorite_member_target UNIQUE (user_id,target_type,target_id)
    );
END;
COMMIT;';
    -- END EMBEDDED V703__favorites.sql
    -- BEGIN EMBEDDED V704__review_video.sql
    EXEC sys.sp_executesql N'USE [UTEFitDB];
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH(''dbo.reviews'', ''video_url'') IS NULL
    ALTER TABLE dbo.reviews ADD video_url VARCHAR(500) NULL;
IF COL_LENGTH(''dbo.reviews'', ''video_public_id'') IS NULL
    ALTER TABLE dbo.reviews ADD video_public_id VARCHAR(255) NULL;
COMMIT;';
    -- END EMBEDDED V704__review_video.sql
    -- BEGIN EMBEDDED V705__manager_scope.sql
    EXEC sys.sp_executesql N'USE [UTEFitDB];
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    DECLARE @roleColumn INT = COLUMNPROPERTY(OBJECT_ID(''dbo.users''), ''role'', ''ColumnId'');
    IF @roleColumn IS NULL THROW 50705, ''Missing users.role; migration aborted.'', 1;
    DECLARE @name SYSNAME, @definition NVARCHAR(MAX), @normalized NVARCHAR(MAX), @drop NVARCHAR(MAX) = N'''';
    DECLARE roleChecks CURSOR LOCAL FAST_FORWARD FOR
        SELECT DISTINCT checks.name, checks.definition
        FROM sys.check_constraints checks
        LEFT JOIN sys.sql_expression_dependencies deps ON deps.referencing_id = checks.object_id
        WHERE checks.parent_object_id = OBJECT_ID(''dbo.users'')
          AND (checks.parent_column_id = @roleColumn
               OR (deps.referenced_id = OBJECT_ID(''dbo.users'') AND deps.referenced_minor_id = @roleColumn));
    OPEN roleChecks;
    FETCH NEXT FROM roleChecks INTO @name, @definition;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        SET @normalized = UPPER(@definition);
        SET @normalized = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(@normalized,
            ''['',''''),'']'',''''),''('',''''),'')'',''''),'' '',''''),CHAR(9),''''),CHAR(10),''''),CHAR(13),'''');
        IF CHARINDEX(''ROLE=''''ADMIN'''''', @normalized) = 0 OR CHARINDEX(''ROLE=''''MEMBER'''''', @normalized) = 0
            OR CHARINDEX(''ROLE=''''PT'''''', @normalized) = 0
            THROW 50706, ''Unknown role constraint; review manually. No changes applied.'', 1;
        SET @normalized = REPLACE(REPLACE(REPLACE(REPLACE(@normalized,
            ''ROLE=''''ADMIN'''''',''''),''ROLE=''''MEMBER'''''',''''),''ROLE=''''PT'''''',''''),''ROLE=''''MANAGER'''''','''');
        SET @normalized = REPLACE(@normalized, ''OR'', '''');
        IF @normalized <> '''' THROW 50707, ''Complex role constraint; review manually. No changes applied.'', 1;
        SET @drop = @drop + N''ALTER TABLE dbo.users DROP CONSTRAINT '' + QUOTENAME(@name) + N'';'';
        FETCH NEXT FROM roleChecks INTO @name, @definition;
    END;
    CLOSE roleChecks;
    DEALLOCATE roleChecks;
    IF @drop <> N'''' EXEC sys.sp_executesql @drop;
    ALTER TABLE dbo.users WITH CHECK ADD CONSTRAINT ck_users_role_gd7 CHECK (role IN (''MEMBER'',''PT'',''ADMIN'',''MANAGER''));
    IF OBJECT_ID(N''dbo.manager_branch_assignments'', N''U'') IS NULL
    BEGIN
        CREATE TABLE dbo.manager_branch_assignments (
            id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
            manager_id BIGINT NOT NULL,
            branch_id BIGINT NOT NULL,
            CONSTRAINT fk_manager_assignment_user FOREIGN KEY (manager_id) REFERENCES dbo.users(id),
            CONSTRAINT fk_manager_assignment_branch FOREIGN KEY (branch_id) REFERENCES dbo.branches(id),
            CONSTRAINT uk_manager_branch UNIQUE (manager_id,branch_id)
        );
    END;
    COMMIT;
END TRY
BEGIN CATCH
    IF CURSOR_STATUS(''local'',''roleChecks'') >= 0 CLOSE roleChecks;
    IF CURSOR_STATUS(''local'',''roleChecks'') >= -1 DEALLOCATE roleChecks;
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;';
    -- END EMBEDDED V705__manager_scope.sql
    -- BEGIN EMBEDDED V706__sepay_vietqr.sql
    EXEC sys.sp_executesql N'USE [UTEFitDB];
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    DECLARE @methodColumn INT = COLUMNPROPERTY(OBJECT_ID(''dbo.payments''), ''method'', ''ColumnId'');
    IF @methodColumn IS NULL THROW 50760, ''Missing payments.method. Apply prerequisite schema before V706.'', 1;
    DECLARE @name SYSNAME, @definition NVARCHAR(MAX), @normalized NVARCHAR(MAX), @drop NVARCHAR(MAX) = N'''';
    DECLARE methodChecks CURSOR LOCAL FAST_FORWARD FOR
        SELECT DISTINCT checks.name, checks.definition
        FROM sys.check_constraints checks
        LEFT JOIN sys.sql_expression_dependencies deps ON deps.referencing_id = checks.object_id
        WHERE checks.parent_object_id = OBJECT_ID(''dbo.payments'')
          AND (checks.parent_column_id = @methodColumn
               OR (deps.referenced_id = OBJECT_ID(''dbo.payments'') AND deps.referenced_minor_id = @methodColumn));
    OPEN methodChecks;
    FETCH NEXT FROM methodChecks INTO @name, @definition;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        SET @normalized = UPPER(@definition);
        SET @normalized = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(@normalized,
            ''['',''''),'']'',''''),''('',''''),'')'',''''),'' '',''''),CHAR(9),''''),CHAR(10),''''),CHAR(13),'''');
        IF CHARINDEX(''METHOD=''''CASH'''''', @normalized) = 0 OR CHARINDEX(''METHOD=''''VNPAY'''''', @normalized) = 0
            THROW 50761, ''Unknown PaymentMethod constraint. Review manually; no changes applied.'', 1;
        SET @normalized = REPLACE(REPLACE(REPLACE(@normalized,''METHOD=''''CASH'''''',''''),''METHOD=''''VNPAY'''''',''''),''METHOD=''''SEPAY'''''','''');
        SET @normalized = REPLACE(@normalized, ''OR'', '''');
        IF @normalized <> '''' THROW 50762, ''Complex PaymentMethod constraint. Review manually; no changes applied.'', 1;
        SET @drop = @drop + N''ALTER TABLE dbo.payments DROP CONSTRAINT '' + QUOTENAME(@name) + N'';'';
        FETCH NEXT FROM methodChecks INTO @name, @definition;
    END;
    CLOSE methodChecks;
    DEALLOCATE methodChecks;
    IF @drop <> N'''' EXEC sys.sp_executesql @drop;
    ALTER TABLE dbo.payments WITH CHECK ADD CONSTRAINT ck_payments_method_sepay CHECK (method IN (''CASH'',''VNPAY'',''SEPAY''));

    IF OBJECT_ID(N''dbo.sepay_payment_details'', N''U'') IS NULL
    BEGIN
        CREATE TABLE dbo.sepay_payment_details (
            payment_id BIGINT NOT NULL PRIMARY KEY,
            payment_reference VARCHAR(35) NOT NULL,
            environment VARCHAR(4) NOT NULL,
            bank_code VARCHAR(32) NOT NULL,
            gateway VARCHAR(100) NOT NULL,
            account_number VARCHAR(50) NOT NULL,
            account_name NVARCHAR(150) NOT NULL,
            expires_at DATETIME2 NOT NULL,
            CONSTRAINT fk_sepay_details_payment FOREIGN KEY(payment_id) REFERENCES dbo.payments(id),
            CONSTRAINT uk_sepay_payment_reference UNIQUE(payment_reference),
            CONSTRAINT ck_sepay_details_environment CHECK(environment IN (''TEST'',''LIVE''))
        );
    END;
    IF OBJECT_ID(N''dbo.sepay_webhook_transactions'', N''U'') IS NULL
    BEGIN
        CREATE TABLE dbo.sepay_webhook_transactions (
            id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
            environment VARCHAR(4) NOT NULL,
            transaction_id BIGINT NOT NULL,
            gateway VARCHAR(100) NOT NULL,
            account_number VARCHAR(50) NOT NULL,
            payment_reference VARCHAR(100) NULL,
            bank_reference VARCHAR(100) NULL,
            content NVARCHAR(2000) NULL,
            transfer_type VARCHAR(3) NOT NULL,
            amount DECIMAL(15,2) NOT NULL,
            transaction_date DATETIME2 NOT NULL,
            received_at DATETIME2 NOT NULL,
            body_hash VARCHAR(64) NOT NULL,
            fingerprint VARCHAR(64) NOT NULL,
            outcome VARCHAR(10) NOT NULL,
            reason VARCHAR(100) NOT NULL,
            payment_id BIGINT NULL,
            conflict_transaction_id BIGINT NULL,
            conflict_hash VARCHAR(64) NULL,
            CONSTRAINT fk_sepay_receipt_payment FOREIGN KEY(payment_id) REFERENCES dbo.payments(id),
            CONSTRAINT uk_sepay_environment_transaction UNIQUE(environment,transaction_id),
            CONSTRAINT ck_sepay_receipt_environment CHECK(environment IN (''TEST'',''LIVE'')),
            CONSTRAINT ck_sepay_receipt_outcome CHECK(outcome IN (''REVIEW'',''MATCHED'',''IGNORED'')),
            CONSTRAINT ck_sepay_receipt_amount CHECK(amount > 0),
            CONSTRAINT ck_sepay_receipt_direction CHECK(transfer_type IN (''in'',''out''))
        );
    END;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(''dbo.payments'') AND name=''uk_payments_sepay_reference'')
        CREATE UNIQUE INDEX uk_payments_sepay_reference ON dbo.payments(transaction_code) WHERE method=''SEPAY'' AND transaction_code IS NOT NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(''dbo.sepay_webhook_transactions'') AND name=''uk_sepay_bank_reference'')
        CREATE UNIQUE INDEX uk_sepay_bank_reference ON dbo.sepay_webhook_transactions(environment,gateway,account_number,bank_reference) WHERE bank_reference IS NOT NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(''dbo.sepay_webhook_transactions'') AND name=''idx_sepay_outcome_received'')
        CREATE INDEX idx_sepay_outcome_received ON dbo.sepay_webhook_transactions(outcome,received_at);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(''dbo.sepay_webhook_transactions'') AND name=''idx_sepay_receipt_reference'')
        CREATE INDEX idx_sepay_receipt_reference ON dbo.sepay_webhook_transactions(payment_reference);
    COMMIT;
END TRY
BEGIN CATCH
    IF CURSOR_STATUS(''local'',''methodChecks'') >= 0 CLOSE methodChecks;
    IF CURSOR_STATUS(''local'',''methodChecks'') >= -1 DEALLOCATE methodChecks;
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;';
    -- END EMBEDDED V706__sepay_vietqr.sql
    -- BEGIN EMBEDDED V707__qr_checkout.sql
    EXEC sys.sp_executesql N'USE [UTEFitDB];
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    IF OBJECT_ID(N''dbo.qr_checkin_tokens'', N''U'') IS NULL OR OBJECT_ID(N''dbo.checkins'', N''U'') IS NULL
        THROW 50770, ''Missing QR/checkins prerequisites. Apply existing schema first.'', 1;

    IF COL_LENGTH(''dbo.qr_checkin_tokens'', ''purpose'') IS NULL
        ALTER TABLE dbo.qr_checkin_tokens ADD purpose VARCHAR(16) NOT NULL
            CONSTRAINT df_qr_token_purpose DEFAULT (''CHECK_IN'') WITH VALUES;
    IF COL_LENGTH(''dbo.qr_checkin_tokens'', ''checkin_id'') IS NULL
        ALTER TABLE dbo.qr_checkin_tokens ADD checkin_id BIGINT NULL;

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id=OBJECT_ID(''dbo.qr_checkin_tokens'') AND name=''fk_qr_token_checkin'')
        EXEC(N''ALTER TABLE dbo.qr_checkin_tokens WITH CHECK ADD CONSTRAINT fk_qr_token_checkin
            FOREIGN KEY(checkin_id) REFERENCES dbo.checkins(id);'');
    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(''dbo.qr_checkin_tokens'') AND name=''ck_qr_token_purpose_target'')
        EXEC(N''ALTER TABLE dbo.qr_checkin_tokens WITH CHECK ADD CONSTRAINT ck_qr_token_purpose_target CHECK
            ((purpose=''''CHECK_IN'''' AND checkin_id IS NULL) OR (purpose=''''CHECK_OUT'''' AND checkin_id IS NOT NULL));'');
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(''dbo.qr_checkin_tokens'') AND name=''idx_qr_token_checkin'')
        EXEC(N''CREATE INDEX idx_qr_token_checkin ON dbo.qr_checkin_tokens(checkin_id) WHERE checkin_id IS NOT NULL;'');
    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;';
    -- END EMBEDDED V707__qr_checkout.sql
    -- BEGIN EMBEDDED V708__sepay_live_guard.sql
    EXEC sys.sp_executesql N'USE [UTEFitDB];
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    IF OBJECT_ID(N''dbo.sepay_payment_details'', N''U'') IS NULL
        OR OBJECT_ID(N''dbo.sepay_webhook_transactions'', N''U'') IS NULL
        THROW 50708, ''Apply and verify V706 before V708.'', 1;
    IF OBJECT_ID(N''dbo.sepay_deployment_guard'', N''U'') IS NULL
    BEGIN
        CREATE TABLE dbo.sepay_deployment_guard (
            id INT NOT NULL CONSTRAINT pk_sepay_deployment_guard PRIMARY KEY,
            environment VARCHAR(4) NOT NULL,
            deployment_id VARCHAR(100) NOT NULL,
            approved_at DATETIME2 NOT NULL,
            CONSTRAINT ck_sepay_guard_singleton CHECK(id=1),
            CONSTRAINT ck_sepay_guard_environment CHECK(environment IN (''TEST'',''LIVE''))
        );
    END;
    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;';
    -- END EMBEDDED V708__sepay_live_guard.sql
    -- BEGIN EMBEDDED V709__sepay_virtual_account.sql
    EXEC sys.sp_executesql N'USE [UTEFitDB];
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    IF OBJECT_ID(N''dbo.sepay_payment_details'',N''U'') IS NULL
        OR OBJECT_ID(N''dbo.sepay_webhook_transactions'',N''U'') IS NULL
        THROW 50709, ''Missing SePay V706 schema. Stop and review.'', 1;
    IF COL_LENGTH(''dbo.sepay_payment_details'',''va_number'') IS NULL
        ALTER TABLE dbo.sepay_payment_details ADD va_number VARCHAR(50) NULL;
    IF COL_LENGTH(''dbo.sepay_webhook_transactions'',''sub_account'') IS NULL
        ALTER TABLE dbo.sepay_webhook_transactions ADD sub_account VARCHAR(50) NULL;
    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;';
    -- END EMBEDDED V709__sepay_virtual_account.sql
    -- BEGIN EMBEDDED V710__sepay_manual_bank_reconciliation.sql
    EXEC sys.sp_executesql N'USE [UTEFitDB];
SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF OBJECT_ID(N''dbo.sepay_payment_details'', N''U'') IS NULL
    THROW 51000, ''Apply V706 and V708-V709 before V710.'', 1;
IF COL_LENGTH(''dbo.sepay_payment_details'', ''va_number'') IS NULL
    THROW 51000, ''V709 is required before V710.'', 1;
IF OBJECT_ID(N''dbo.sepay_deployment_guard'', N''U'') IS NULL
    OR COL_LENGTH(''dbo.sepay_webhook_transactions'', ''sub_account'') IS NULL
    THROW 51000, ''V708 and V709 are required before V710.'', 1;
IF OBJECT_ID(N''dbo.sepay_bank_reconciliations'', N''U'') IS NOT NULL
    THROW 51000, ''V710 table already exists. Inspect schema; do not rerun blindly.'', 1;

CREATE TABLE dbo.sepay_bank_reconciliations (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    payment_id bigint NOT NULL REFERENCES dbo.payments(id),
    environment varchar(4) NOT NULL CHECK (environment IN (''TEST'',''LIVE'')),
    gateway varchar(100) NOT NULL,
    account_number varchar(50) NOT NULL,
    sub_account varchar(50) NULL,
    bank_reference varchar(100) NOT NULL,
    amount decimal(15,2) NOT NULL CHECK (amount > 0),
    bank_date datetime2 NOT NULL,
    method varchar(40) NOT NULL CHECK (method IN (''MANUAL_BANK_RECONCILIATION'',''AUTHENTICATED_WEBHOOK'')),
    outcome varchar(20) NOT NULL CHECK (outcome IN (''VERIFIED'',''APPLIED'',''REFUNDED'',''WEBHOOK'')),
    verified_by bigint NULL REFERENCES dbo.users(id),
    verified_at datetime2 NOT NULL,
    evidence nvarchar(2000) NULL,
    reason nvarchar(1000) NULL,
    resolved_by bigint NULL REFERENCES dbo.users(id),
    resolved_at datetime2 NULL,
    refund_reference varchar(100) NULL,
    refunded_at datetime2 NULL,
    resolution_evidence nvarchar(3000) NULL,
    CONSTRAINT uk_sepay_bank_claim UNIQUE (environment,gateway,account_number,bank_reference),
    CONSTRAINT ck_sepay_manual_evidence CHECK (method <> ''MANUAL_BANK_RECONCILIATION''
        OR (environment = ''LIVE'' AND verified_by IS NOT NULL AND evidence IS NOT NULL AND reason IS NOT NULL)),
    CONSTRAINT ck_sepay_resolution CHECK (outcome NOT IN (''APPLIED'',''REFUNDED'')
        OR (resolved_by IS NOT NULL AND resolved_at IS NOT NULL)),
    CONSTRAINT ck_sepay_refund_evidence CHECK (outcome <> ''REFUNDED''
        OR (refund_reference IS NOT NULL AND refunded_at IS NOT NULL AND resolution_evidence IS NOT NULL))
);
CREATE UNIQUE INDEX uk_sepay_refund_reference ON dbo.sepay_bank_reconciliations(refund_reference)
    WHERE refund_reference IS NOT NULL;
CREATE INDEX ix_sepay_bank_reconciliation_payment ON dbo.sepay_bank_reconciliations(payment_id,id);

COMMIT TRANSACTION;';
    -- END EMBEDDED V710__sepay_manual_bank_reconciliation.sql
    -- BEGIN EMBEDDED V711__membership_entitlements_and_renewals.sql
    EXEC sys.sp_executesql N'USE [UTEFitDB];
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF OBJECT_ID(N''dbo.sepay_bank_reconciliations'', N''U'') IS NULL
    THROW 51000, ''Apply V710 before V711.'', 1;
IF COL_LENGTH(''dbo.memberships'', ''quoted_scope'') IS NOT NULL
    THROW 51000, ''V711 appears applied. Inspect schema before rerunning.'', 1;

ALTER TABLE dbo.membership_plans ADD benefit varchar(20) NULL,
    access_scope varchar(20) NULL;
EXEC(N''ALTER TABLE dbo.membership_plans ADD CONSTRAINT ck_plan_benefit
    CHECK (benefit IS NULL OR benefit IN (''''ALL'''',''''GYM'''',''''YOGA'''',''''PT''''));'');
EXEC(N''ALTER TABLE dbo.membership_plans ADD CONSTRAINT ck_plan_scope
    CHECK (access_scope IS NULL OR access_scope IN (''''SINGLE_BRANCH'''',''''MULTI_BRANCH'''',''''ALL_BRANCHES''''));'');
ALTER TABLE dbo.memberships ADD quoted_benefit varchar(20) NULL,
    quoted_scope varchar(20) NULL, renewal_of_id bigint NULL;
EXEC(N''ALTER TABLE dbo.memberships ADD CONSTRAINT fk_membership_renewal
    FOREIGN KEY (renewal_of_id) REFERENCES dbo.memberships(id);'');
EXEC(N''ALTER TABLE dbo.memberships ADD CONSTRAINT ck_membership_quoted_benefit
    CHECK (quoted_benefit IS NULL OR quoted_benefit IN (''''ALL'''',''''GYM'''',''''YOGA'''',''''PT''''));'');
EXEC(N''ALTER TABLE dbo.memberships ADD CONSTRAINT ck_membership_quoted_scope
    CHECK (quoted_scope IS NULL OR quoted_scope IN (''''SINGLE_BRANCH'''',''''MULTI_BRANCH'''',''''ALL_BRANCHES''''));'');
ALTER TABLE dbo.class_categories ADD required_benefit varchar(20) NULL;
EXEC(N''ALTER TABLE dbo.class_categories ADD CONSTRAINT ck_category_benefit
    CHECK (required_benefit IS NULL OR required_benefit IN (''''ALL'''',''''GYM'''',''''YOGA'''',''''PT''''));'');
CREATE TABLE dbo.membership_plan_branches (
    plan_id bigint NOT NULL REFERENCES dbo.membership_plans(id),
    branch_id bigint NOT NULL REFERENCES dbo.branches(id),
    CONSTRAINT pk_membership_plan_branches PRIMARY KEY (plan_id,branch_id)
);
CREATE TABLE dbo.membership_branch_snapshot (
    membership_id bigint NOT NULL REFERENCES dbo.memberships(id),
    branch_id bigint NOT NULL REFERENCES dbo.branches(id),
    CONSTRAINT pk_membership_branch_snapshot PRIMARY KEY (membership_id,branch_id)
);
EXEC(N''CREATE INDEX ix_membership_access ON dbo.memberships(user_id,status,end_date)
    INCLUDE (start_date,quoted_benefit,quoted_scope);'');
COMMIT TRANSACTION;';
    -- END EMBEDDED V711__membership_entitlements_and_renewals.sql
    -- BEGIN EMBEDDED V712__membership_calendar_terms.sql
    EXEC sys.sp_executesql N'USE [UTEFitDB];
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    IF OBJECT_ID(N''dbo.membership_branch_snapshot'', N''U'') IS NULL
        THROW 51000, ''Apply V711 before V712.'', 1;
    IF COL_LENGTH(''dbo.membership_plans'', ''duration_unit'') IS NOT NULL
        THROW 51000, ''V712 appears applied. Inspect schema before rerunning.'', 1;

    ALTER TABLE dbo.membership_plans ADD duration_unit varchar(10) NULL,
        duration_quantity int NULL, bonus_unit varchar(10) NULL, bonus_quantity int NULL;
    ALTER TABLE dbo.memberships ADD quoted_duration_unit varchar(10) NULL,
        quoted_duration_quantity int NULL, quoted_bonus_unit varchar(10) NULL, quoted_bonus_quantity int NULL;
    EXEC(N''ALTER TABLE dbo.membership_plans ADD CONSTRAINT ck_plan_calendar_term CHECK (
        (duration_unit IS NULL AND duration_quantity IS NULL AND bonus_unit IS NULL AND bonus_quantity IS NULL)
        OR (duration_unit IS NOT NULL AND duration_quantity IS NOT NULL AND bonus_unit IS NOT NULL AND bonus_quantity IS NOT NULL
        AND duration_unit IN (''''DAY'''',''''WEEK'''',''''MONTH'''',''''YEAR'''') AND duration_quantity BETWEEN 1 AND 36600
        AND bonus_unit IN (''''DAY'''',''''WEEK'''',''''MONTH'''',''''YEAR'''') AND bonus_quantity BETWEEN 0 AND 36600));'');
    EXEC(N''ALTER TABLE dbo.memberships ADD CONSTRAINT ck_membership_calendar_term CHECK (
        (quoted_duration_unit IS NULL AND quoted_duration_quantity IS NULL AND quoted_bonus_unit IS NULL AND quoted_bonus_quantity IS NULL)
        OR (quoted_duration_unit IS NOT NULL AND quoted_duration_quantity IS NOT NULL AND quoted_bonus_unit IS NOT NULL AND quoted_bonus_quantity IS NOT NULL
        AND quoted_duration_unit IN (''''DAY'''',''''WEEK'''',''''MONTH'''',''''YEAR'''') AND quoted_duration_quantity BETWEEN 1 AND 36600
        AND quoted_bonus_unit IN (''''DAY'''',''''WEEK'''',''''MONTH'''',''''YEAR'''') AND quoted_bonus_quantity BETWEEN 0 AND 36600));'');
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;';
    -- END EMBEDDED V712__membership_calendar_terms.sql
    -- BEGIN EMBEDDED V713__catalog_unicode.sql
    EXEC sys.sp_executesql N'USE [UTEFitDB];
-- SQL Server. Additive type widening only; no prices, payments or dates changed.
SET XACT_ABORT ON;
IF @@TRANCOUNT <> 0 THROW 51500,''Use an independent session.'',1;
BEGIN TRY
 BEGIN TRANSACTION;
 ALTER TABLE dbo.branches ALTER COLUMN name nvarchar(150) NOT NULL;
 ALTER TABLE dbo.branches ALTER COLUMN address nvarchar(255) NOT NULL;
 ALTER TABLE dbo.branches ALTER COLUMN description nvarchar(1000) NULL;
 ALTER TABLE dbo.membership_plans ALTER COLUMN name nvarchar(100) NOT NULL;
 ALTER TABLE dbo.membership_plans ALTER COLUMN description nvarchar(1500) NULL;
 COMMIT TRANSACTION;
END TRY
BEGIN CATCH
 IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
 THROW;
END CATCH;';
    -- END EMBEDDED V713__catalog_unicode.sql
    -- BEGIN EMBEDDED V714__pending_payment_cancellation.sql
    EXEC sys.sp_executesql N'USE [UTEFitDB];
-- SQL Server. Preserves existing payments, memberships and bank snapshots.
-- DBA applies after backup with the application stopped. No approval/Live flag changes.
SET XACT_ABORT ON;
IF @@TRANCOUNT <> 0 THROW 51520,''Use an independent session.'',1;
BEGIN TRY
 BEGIN TRANSACTION;
 IF COL_LENGTH(''dbo.payments'',''cancellation_requested_at'') IS NULL ALTER TABLE dbo.payments ADD cancellation_requested_at datetime2 NULL;
 IF COL_LENGTH(''dbo.payments'',''cancelled_at'') IS NULL ALTER TABLE dbo.payments ADD cancelled_at datetime2 NULL;
 IF COL_LENGTH(''dbo.payments'',''cancelled_by'') IS NULL ALTER TABLE dbo.payments ADD cancelled_by varchar(100) NULL;
 IF COL_LENGTH(''dbo.payments'',''cancellation_reason'') IS NULL ALTER TABLE dbo.payments ADD cancellation_reason nvarchar(500) NULL;
 IF COL_LENGTH(''dbo.payments'',''late_gateway_evidence'') IS NULL ALTER TABLE dbo.payments ADD late_gateway_evidence nvarchar(2000) NULL;
 DECLARE @drop nvarchar(max)=N'''';
 SELECT @drop=@drop+N''ALTER TABLE dbo.payments DROP CONSTRAINT ''+QUOTENAME(name)+N'';''
 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(''dbo.payments'')
 AND (parent_column_id=COLUMNPROPERTY(OBJECT_ID(''dbo.payments''),''status'',''ColumnId'')
     OR definition LIKE ''%[[]status[]]%'');
 IF @drop<>N'''' EXEC sys.sp_executesql @drop;
 ALTER TABLE dbo.payments WITH CHECK ADD CONSTRAINT ck_payment_status_v714
    CHECK(status IN (''PENDING'',''CANCEL_REQUESTED'',''CANCELLED'',''PAID'',''FAILED'',''REFUNDED''));
 IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(''dbo.payments'') AND name=''ix_payment_cancellation_queue'')
    CREATE INDEX ix_payment_cancellation_queue ON dbo.payments(status,created_at) INCLUDE(membership_id,method);
 COMMIT TRANSACTION;
END TRY
BEGIN CATCH
 IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
 THROW;
END CATCH;';
    -- END EMBEDDED V714__pending_payment_cancellation.sql
    -- BEGIN EMBEDDED V715__membership_promotion_snapshot.sql
    EXEC sys.sp_executesql N'USE [UTEFitDB];
-- Apply once to the intended existing database before starting the updated app.
-- Additive migration: does not reset or change existing financial records.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH(''dbo.memberships'', ''promotion_code'') IS NULL
    ALTER TABLE dbo.memberships ADD promotion_code varchar(50) NULL;
IF COL_LENGTH(''dbo.memberships'', ''original_price'') IS NULL
    ALTER TABLE dbo.memberships ADD original_price numeric(15,2) NULL;
IF COL_LENGTH(''dbo.memberships'', ''discount_percent'') IS NULL
    ALTER TABLE dbo.memberships ADD discount_percent numeric(5,2) NULL;
COMMIT TRANSACTION;';
    -- END EMBEDDED V715__membership_promotion_snapshot.sql
    EXEC sys.sp_executesql N'USE [UTEFitDB];
ALTER TABLE dbo.reviews ADD CONSTRAINT ck_demo_review_rating CHECK(rating BETWEEN 1 AND 5);
ALTER TABLE dbo.fitness_classes ADD CONSTRAINT ck_demo_class_capacity CHECK(capacity > 0);
ALTER TABLE dbo.class_schedules ADD CONSTRAINT ck_demo_class_hours CHECK(end_time > start_time);
ALTER TABLE dbo.personal_training_schedules ADD CONSTRAINT ck_demo_pt_hours CHECK(end_time > start_time);
ALTER TABLE dbo.checkins ADD CONSTRAINT ck_demo_checkin_order CHECK(check_out_at IS NULL OR check_out_at >= check_in_at);
ALTER TABLE dbo.memberships ADD CONSTRAINT ck_demo_membership_dates CHECK(start_date IS NULL OR end_date IS NULL OR end_date >= start_date);
ALTER TABLE dbo.membership_plans ADD CONSTRAINT ck_demo_plan_price_days CHECK(price > 0 AND duration_days > 0);
ALTER TABLE dbo.promotions ADD CONSTRAINT ck_demo_promotion CHECK(discount_percent > 0 AND discount_percent <= 100 AND quantity >= 0 AND end_date >= start_date);
CREATE UNIQUE INDEX uk_checkin_one_open ON dbo.checkins(user_id) WHERE check_out_at IS NULL;
EXEC sys.sp_addextendedproperty @name=N''UTEFit.DatabasePurpose'', @value=N''DEVELOPMENT_ONLY'';
IF (SELECT COUNT(*) FROM sys.tables WHERE schema_id=SCHEMA_ID(N''dbo'')) <> 28
    THROW 51103, ''Schema count mismatch. Do not seed.'', 1;
SELECT COUNT(*) AS schema_table_count FROM sys.tables WHERE schema_id=SCHEMA_ID(N''dbo'');
';
    PRINT 'UTEFitDB development schema ready. Run DB_Data_2.sql separately. No Live approval created.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
