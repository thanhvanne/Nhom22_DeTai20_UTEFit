:On Error exit
USE [UTEFitLiveDemo];
GO
SET XACT_ABORT ON;
IF DB_NAME() <> N'UTEFitLiveDemo' THROW 50810, 'Wrong database. Stop.', 1;
IF EXISTS (SELECT 1 FROM sys.tables WHERE is_ms_shipped=0)
    THROW 50811, 'Schema bootstrap requires a completely empty LiveDemo database. Never overwrite existing tables.', 1;
BEGIN TRY
    BEGIN TRANSACTION;
-- BEGIN JPA CORE SNAPSHOT
create table branches (active bit not null, closing_time time, opening_time time, created_at datetime2(7) not null, id bigint identity not null, updated_at datetime2(7), phone varchar(20), name nvarchar(150) not null, description nvarchar(1000), address nvarchar(255) not null, image_public_id varchar(255), image_url varchar(255), primary key (id));
create table checkins (branch_id bigint not null, check_in_at datetime2(7) not null, check_out_at datetime2(7), id bigint identity not null, user_id bigint not null, primary key (id));
create table class_categories (active bit not null, created_at datetime2(7) not null, id bigint identity not null, updated_at datetime2(7), name varchar(100) not null, description varchar(1000), primary key (id));
create table class_registrations (id bigint identity not null, registered_at datetime2(7) not null, schedule_id bigint not null, user_id bigint not null, status varchar(20) not null check ((status in ('REGISTERED','CANCELLED','ATTENDED','ABSENT'))), primary key (id));
create table class_schedules (active bit not null, end_time time not null, schedule_date date not null, start_time time not null, created_at datetime2(7) not null, fitness_class_id bigint not null, id bigint identity not null, room varchar(100), primary key (id));
create table conversations (created_at datetime2(7) not null, id bigint identity not null, member_id bigint not null, trainer_id bigint not null, updated_at datetime2(7), primary key (id));
create table fitness_classes (capacity int not null, branch_id bigint not null, category_id bigint not null, created_at datetime2(7) not null, id bigint identity not null, trainer_id bigint, updated_at datetime2(7), status varchar(20) not null check ((status in ('OPEN','FULL','CANCELLED','COMPLETED'))), name varchar(150) not null, description varchar(1500), primary key (id));
create table membership_plans (active bit not null, duration_days int not null, price numeric(15,2) not null, created_at datetime2(7) not null, id bigint identity not null, updated_at datetime2(7), name nvarchar(100) not null, description nvarchar(1500), primary key (id));
create table memberships (end_date date, quoted_duration_days int, quoted_price numeric(15,2), start_date date, branch_id bigint not null, created_at datetime2(7) not null, id bigint identity not null, plan_id bigint not null, updated_at datetime2(7), user_id bigint not null, status varchar(20) not null check ((status in ('PENDING','ACTIVE','EXPIRED','CANCELLED'))), primary key (id));
create table messages (conversation_id bigint not null, id bigint identity not null, read_at datetime2(7), sender_id bigint not null, sent_at datetime2(7) not null, content nvarchar(2000) not null, primary key (id));
create table notifications (is_read bit not null, created_at datetime2(7) not null, id bigint identity not null, read_at datetime2(7), related_id bigint, user_id bigint not null, type varchar(50) not null check ((type in ('TRAINER_REQUEST_SENT','TRAINER_REQUEST_ACCEPTED','TRAINER_REQUEST_REJECTED','PERSONAL_SCHEDULE_CREATED','CLASS_SCHEDULE_CANCELLED','MESSAGE_RECEIVED','MEMBERSHIP_ACTIVATED','PERSONAL_SCHEDULE_CANCELLED','SCHEDULE_REMINDER'))), title varchar(200) not null, content varchar(2000), primary key (id));
create table otp_codes (attempt_count int, used bit not null, created_at datetime2(7) not null, expired_at datetime2(7) not null, id bigint identity not null, used_at datetime2(7), user_id bigint not null, type varchar(30) not null check ((type in ('REGISTER','FORGOT_PASSWORD'))), code varchar(100) not null, primary key (id));
create table payments (amount numeric(15,2) not null, cancellation_requested_at datetime2(7), cancelled_at datetime2(7), created_at datetime2(7) not null, id bigint identity not null, membership_id bigint, paid_at datetime2(7), user_id bigint not null, gateway_response_code varchar(10), method varchar(20) not null check ((method in ('CASH','VNPAY','SEPAY'))), status varchar(20) not null check ((status in ('PENDING','CANCEL_REQUESTED','CANCELLED','PAID','FAILED','REFUNDED'))), cancelled_by varchar(100), gateway_transaction_no varchar(100), transaction_code varchar(100), cancellation_reason nvarchar(500), late_gateway_evidence nvarchar(2000), primary key (id));
create table personal_training_schedules (active bit not null, end_time time not null, schedule_date date not null, start_time time not null, branch_id bigint, created_at datetime2(7) not null, id bigint identity not null, member_id bigint not null, trainer_id bigint not null, updated_at datetime2(7), status varchar(20) check ((status in ('SCHEDULED','COMPLETED','CANCELLED'))), note varchar(2000), title varchar(255), primary key (id));
create table promotions (active bit not null, discount_percent numeric(5,2) not null, end_date date not null, quantity int not null, start_date date not null, created_at datetime2(7) not null, id bigint identity not null, code varchar(50) not null, primary key (id));
create table reviews (rating int not null, visible bit not null, branch_id bigint not null, created_at datetime2(7) not null, id bigint identity not null, trainer_id bigint, updated_at datetime2(7), user_id bigint not null, image_url nvarchar(500), video_url varchar(500), comment nvarchar(2000), image_public_id varchar(255), video_public_id varchar(255), primary key (id));
create table trainer_requests (id bigint identity not null, member_id bigint not null, requested_at datetime2(7) not null, responded_at datetime2(7), trainer_id bigint not null, status varchar(20) not null check ((status in ('PENDING','ACCEPTED','REJECTED','CANCELLED'))), message varchar(2000), primary key (id));
create table trainers (active bit not null, years_experience int, branch_id bigint, created_at datetime2(7) not null, id bigint identity not null, updated_at datetime2(7), user_id bigint not null, bio varchar(2000), specialization varchar(255), primary key (id));
create table users (enabled bit not null, token_version int, created_at datetime2(7) not null, id bigint identity not null, updated_at datetime2(7), phone varchar(20), role varchar(20) not null check ((role in ('MEMBER','PT','ADMIN','MANAGER'))), status varchar(20) not null check ((status in ('UNVERIFIED','ACTIVE','LOCKED'))), first_name varchar(50) not null, last_name varchar(50) not null, email varchar(100) not null, avatar_public_id varchar(255), avatar_url varchar(255), password varchar(255) not null, primary key (id));
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
    IF (SELECT COUNT(*) FROM sys.tables WHERE is_ms_shipped=0 AND schema_id=SCHEMA_ID('dbo')) <> 19
        THROW 50812, 'Expected nineteen core tables.', 1;
    COMMIT;
    PRINT 'Core schema created. Next apply V701-V708 using 02_apply_gd7.sql. No data or approval inserted.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
