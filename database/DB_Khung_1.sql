-- ============================================================
-- UTEFIT DATABASE - FULL RESET
-- FILE: DB_Khung_1.sql
-- SQL SERVER
-- TABLES: 13
-- ============================================================

USE master;
GO

IF DB_ID(N'UTEFitDB') IS NOT NULL
BEGIN
    PRINT 'Dropping old UTEFitDB...';

    ALTER DATABASE UTEFitDB
    SET SINGLE_USER
    WITH ROLLBACK IMMEDIATE;

    DROP DATABASE UTEFitDB;
END
GO

CREATE DATABASE UTEFitDB;
GO

USE UTEFitDB;
GO

-- ============================================================
-- 1. USERS
-- ============================================================

CREATE TABLE dbo.users (
                           id BIGINT IDENTITY(1,1) NOT NULL,
                           avatar_public_id VARCHAR(255),
                           avatar_url VARCHAR(255),
                           created_at DATETIME2(7) NOT NULL,
                           email VARCHAR(100) NOT NULL,
                           enabled BIT NOT NULL,
                           first_name VARCHAR(50) NOT NULL,
                           last_name VARCHAR(50) NOT NULL,
                           password VARCHAR(255) NOT NULL,
                           phone VARCHAR(20),
                           role VARCHAR(20) NOT NULL,
                           status VARCHAR(20) NOT NULL,
                           token_version INT,
                           updated_at DATETIME2(7),

                           CONSTRAINT PK_users PRIMARY KEY (id),
                           CONSTRAINT UK_users_email UNIQUE (email),

                           CONSTRAINT CK_users_role CHECK (
                               role IN ('MEMBER','PT','ADMIN')
                               ),

                           CONSTRAINT CK_users_status CHECK (
                               status IN ('UNVERIFIED','ACTIVE','LOCKED')
                               )
);
GO

-- ============================================================
-- 2. BRANCHES
-- ============================================================

CREATE TABLE dbo.branches (
                              id BIGINT IDENTITY(1,1) NOT NULL,
                              active BIT NOT NULL,
                              address VARCHAR(255) NOT NULL,
                              closing_time TIME(7),
                              created_at DATETIME2(7) NOT NULL,
                              description VARCHAR(1000),
                              image_public_id VARCHAR(255),
                              image_url VARCHAR(255),
                              name VARCHAR(150) NOT NULL,
                              opening_time TIME(7),
                              phone VARCHAR(20),
                              updated_at DATETIME2(7),

                              CONSTRAINT PK_branches PRIMARY KEY (id)
);
GO

-- ============================================================
-- 3. CLASS CATEGORIES
-- ============================================================

CREATE TABLE dbo.class_categories (
                                      id BIGINT IDENTITY(1,1) NOT NULL,
                                      active BIT NOT NULL,
                                      created_at DATETIME2(7) NOT NULL,
                                      description VARCHAR(1000),
                                      name VARCHAR(100) NOT NULL,
                                      updated_at DATETIME2(7),

                                      CONSTRAINT PK_class_categories PRIMARY KEY (id)
);
GO

-- ============================================================
-- 4. MEMBERSHIP PLANS
-- ============================================================

CREATE TABLE dbo.membership_plans (
                                      id BIGINT IDENTITY(1,1) NOT NULL,
                                      active BIT NOT NULL,
                                      created_at DATETIME2(7) NOT NULL,
                                      description VARCHAR(1500),
                                      duration_days INT NOT NULL,
                                      name VARCHAR(100) NOT NULL,
                                      price NUMERIC(15,2) NOT NULL,
                                      updated_at DATETIME2(7),

                                      CONSTRAINT PK_membership_plans PRIMARY KEY (id)
);
GO

-- ============================================================
-- 5. TRAINERS
-- ============================================================

CREATE TABLE dbo.trainers (
                              id BIGINT IDENTITY(1,1) NOT NULL,
                              active BIT NOT NULL,
                              bio VARCHAR(2000),
                              created_at DATETIME2(7) NOT NULL,
                              specialization VARCHAR(255),
                              updated_at DATETIME2(7),
                              years_experience INT,
                              branch_id BIGINT,
                              user_id BIGINT NOT NULL,

                              CONSTRAINT PK_trainers PRIMARY KEY (id),

                              CONSTRAINT uk_trainers_user UNIQUE (user_id),

                              CONSTRAINT fk_trainer_branch
                                  FOREIGN KEY (branch_id)
                                      REFERENCES dbo.branches(id),

                              CONSTRAINT fk_trainer_user
                                  FOREIGN KEY (user_id)
                                      REFERENCES dbo.users(id)
);
GO

-- ============================================================
-- 6. FITNESS CLASSES
-- ============================================================

CREATE TABLE dbo.fitness_classes (
                                     id BIGINT IDENTITY(1,1) NOT NULL,
                                     capacity INT NOT NULL,
                                     created_at DATETIME2(7) NOT NULL,
                                     description VARCHAR(1500),
                                     name VARCHAR(150) NOT NULL,
                                     status VARCHAR(20) NOT NULL,
                                     updated_at DATETIME2(7),
                                     branch_id BIGINT NOT NULL,
                                     category_id BIGINT NOT NULL,
                                     trainer_id BIGINT,

                                     CONSTRAINT PK_fitness_classes PRIMARY KEY (id),

                                     CONSTRAINT CK_fitness_classes_status CHECK (
                                         status IN ('OPEN','FULL','CANCELLED','COMPLETED')
                                         ),

                                     CONSTRAINT fk_class_branch
                                         FOREIGN KEY (branch_id)
                                             REFERENCES dbo.branches(id),

                                     CONSTRAINT fk_class_category
                                         FOREIGN KEY (category_id)
                                             REFERENCES dbo.class_categories(id),

                                     CONSTRAINT fk_class_trainer
                                         FOREIGN KEY (trainer_id)
                                             REFERENCES dbo.trainers(id)
);
GO

-- ============================================================
-- 7. CLASS SCHEDULES
-- ============================================================

CREATE TABLE dbo.class_schedules (
                                     id BIGINT IDENTITY(1,1) NOT NULL,
                                     active BIT NOT NULL,
                                     created_at DATETIME2(7) NOT NULL,
                                     end_time TIME(7) NOT NULL,
                                     room VARCHAR(100),
                                     schedule_date DATE NOT NULL,
                                     start_time TIME(7) NOT NULL,
                                     fitness_class_id BIGINT NOT NULL,

                                     CONSTRAINT PK_class_schedules PRIMARY KEY (id),

                                     CONSTRAINT fk_schedule_class
                                         FOREIGN KEY (fitness_class_id)
                                             REFERENCES dbo.fitness_classes(id)
);
GO

-- ============================================================
-- 8. CLASS REGISTRATIONS
-- ============================================================

CREATE TABLE dbo.class_registrations (
                                         id BIGINT IDENTITY(1,1) NOT NULL,
                                         registered_at DATETIME2(7) NOT NULL,
                                         status VARCHAR(20) NOT NULL,
                                         schedule_id BIGINT NOT NULL,
                                         user_id BIGINT NOT NULL,

                                         CONSTRAINT PK_class_registrations PRIMARY KEY (id),

                                         CONSTRAINT uk_registration_user_schedule
                                             UNIQUE (user_id,schedule_id),

                                         CONSTRAINT CK_class_registrations_status CHECK (
                                             status IN (
                                                        'REGISTERED',
                                                        'CANCELLED',
                                                        'ATTENDED',
                                                        'ABSENT'
                                                 )
                                             ),

                                         CONSTRAINT fk_registration_schedule
                                             FOREIGN KEY (schedule_id)
                                                 REFERENCES dbo.class_schedules(id),

                                         CONSTRAINT fk_registration_user
                                             FOREIGN KEY (user_id)
                                                 REFERENCES dbo.users(id)
);
GO

-- ============================================================
-- 9. MEMBERSHIPS
-- ============================================================

CREATE TABLE dbo.memberships (
                                 id BIGINT IDENTITY(1,1) NOT NULL,
                                 created_at DATETIME2(7) NOT NULL,
                                 end_date DATE,
                                 start_date DATE,
                                 status VARCHAR(20) NOT NULL,
                                 updated_at DATETIME2(7),
                                 branch_id BIGINT NOT NULL,
                                 plan_id BIGINT NOT NULL,
                                 user_id BIGINT NOT NULL,

                                 CONSTRAINT PK_memberships PRIMARY KEY (id),

                                 CONSTRAINT CK_memberships_status CHECK (
                                     status IN (
                                                'PENDING',
                                                'ACTIVE',
                                                'EXPIRED',
                                                'CANCELLED'
                                         )
                                     ),

                                 CONSTRAINT fk_membership_branch
                                     FOREIGN KEY (branch_id)
                                         REFERENCES dbo.branches(id),

                                 CONSTRAINT fk_membership_plan
                                     FOREIGN KEY (plan_id)
                                         REFERENCES dbo.membership_plans(id),

                                 CONSTRAINT fk_membership_user
                                     FOREIGN KEY (user_id)
                                         REFERENCES dbo.users(id)
);
GO

-- ============================================================
-- 10. PAYMENTS
-- ============================================================

CREATE TABLE dbo.payments (
                              id BIGINT IDENTITY(1,1) NOT NULL,
                              amount NUMERIC(15,2) NOT NULL,
                              created_at DATETIME2(7) NOT NULL,
                              method VARCHAR(20) NOT NULL,
                              paid_at DATETIME2(7),
                              status VARCHAR(20) NOT NULL,
                              transaction_code VARCHAR(100),
                              membership_id BIGINT,
                              user_id BIGINT NOT NULL,

                              CONSTRAINT PK_payments PRIMARY KEY (id),

                              CONSTRAINT CK_payments_method CHECK (
                                  method IN ('CASH','VNPAY')
                                  ),

                              CONSTRAINT CK_payments_status CHECK (
                                  status IN (
                                             'PENDING',
                                             'PAID',
                                             'FAILED',
                                             'REFUNDED'
                                      )
                                  ),

                              CONSTRAINT fk_payment_membership
                                  FOREIGN KEY (membership_id)
                                      REFERENCES dbo.memberships(id),

                              CONSTRAINT fk_payment_user
                                  FOREIGN KEY (user_id)
                                      REFERENCES dbo.users(id)
);
GO

-- ============================================================
-- 11. REVIEWS
-- ============================================================

CREATE TABLE dbo.reviews (
                             id BIGINT IDENTITY(1,1) NOT NULL,
                             comment VARCHAR(2000),
                             created_at DATETIME2(7) NOT NULL,
                             rating INT NOT NULL,
                             updated_at DATETIME2(7),
                             visible BIT NOT NULL,
                             branch_id BIGINT NOT NULL,
                             trainer_id BIGINT,
                             user_id BIGINT NOT NULL,

                             CONSTRAINT PK_reviews PRIMARY KEY (id),

                             CONSTRAINT fk_review_branch
                                 FOREIGN KEY (branch_id)
                                     REFERENCES dbo.branches(id),

                             CONSTRAINT fk_review_trainer
                                 FOREIGN KEY (trainer_id)
                                     REFERENCES dbo.trainers(id),

                             CONSTRAINT fk_review_user
                                 FOREIGN KEY (user_id)
                                     REFERENCES dbo.users(id)
);
GO

-- ============================================================
-- 12. OTP CODES
-- ============================================================

CREATE TABLE dbo.otp_codes (
                               id BIGINT IDENTITY(1,1) NOT NULL,
                               attempt_count INT,
                               code VARCHAR(100) NOT NULL,
                               created_at DATETIME2(7) NOT NULL,
                               expired_at DATETIME2(7) NOT NULL,
                               type VARCHAR(30) NOT NULL,
                               used BIT NOT NULL,
                               used_at DATETIME2(7),
                               user_id BIGINT NOT NULL,

                               CONSTRAINT PK_otp_codes PRIMARY KEY (id),

                               CONSTRAINT CK_otp_codes_type CHECK (
                                   type IN ('REGISTER','FORGOT_PASSWORD')
                                   ),

                               CONSTRAINT fk_otp_user
                                   FOREIGN KEY (user_id)
                                       REFERENCES dbo.users(id)
);
GO

-- ============================================================
-- 13. TRAINER REQUESTS
-- ============================================================

CREATE TABLE dbo.trainer_requests (
                                      id BIGINT IDENTITY(1,1) NOT NULL,
                                      message VARCHAR(2000),
                                      requested_at DATETIME2(7) NOT NULL,
                                      responded_at DATETIME2(7),
                                      status VARCHAR(20) NOT NULL,
                                      member_id BIGINT NOT NULL,
                                      trainer_id BIGINT NOT NULL,

                                      CONSTRAINT PK_trainer_requests PRIMARY KEY (id),

                                      CONSTRAINT CK_trainer_requests_status CHECK (
                                          status IN ('PENDING','ACCEPTED','REJECTED')
                                          ),

                                      CONSTRAINT FK_trainer_requests_member
                                          FOREIGN KEY (member_id)
                                              REFERENCES dbo.users(id),

                                      CONSTRAINT FK_trainer_requests_trainer
                                          FOREIGN KEY (trainer_id)
                                              REFERENCES dbo.trainers(id)
);
GO

-- ============================================================
-- VALIDATE STRUCTURE
-- ============================================================

DECLARE @tableCount INT;

SELECT @tableCount = COUNT(*)
FROM sys.tables
WHERE schema_id = SCHEMA_ID('dbo');

IF @tableCount <> 13
BEGIN
    THROW 50001, 'Expected exactly 13 tables.', 1;
END;

PRINT '=============================================';
PRINT 'UTEFIT DATABASE CREATED SUCCESSFULLY';
PRINT 'TOTAL TABLES: 13';
PRINT '=============================================';
GO