SET NOCOUNT ON;
SET XACT_ABORT ON;
IF DB_NAME() <> N'UTEFitDB' THROW 51700, 'Select UTEFitDB explicitly.', 1;
IF @@TRANCOUNT <> 0 THROW 51701, 'Use a fresh SSMS session.', 1;
IF COL_LENGTH('dbo.memberships','offer_kind') IS NULL OR OBJECT_ID('dbo.loyalty_payment_exclusions','U') IS NULL
    THROW 51702, 'V716 is required first. Do not run seed/reset scripts.', 1;
BEGIN TRANSACTION;
IF OBJECT_ID('dbo.automatic_promotions','U') IS NULL
BEGIN
    CREATE TABLE dbo.automatic_promotions (
        kind varchar(12) NOT NULL CONSTRAINT pk_automatic_promotions PRIMARY KEY,
        active bit NOT NULL,
        discount_percent numeric(5,2) NULL,
        discount_cap numeric(15,2) NULL,
        minimum_months int NULL,
        renewal_grace_days int NULL,
        value_threshold numeric(15,2) NULL,
        value_discount numeric(15,2) NULL,
        premium_threshold numeric(15,2) NULL,
        premium_discount numeric(15,2) NULL,
        start_date date NULL,
        end_date date NULL,
        updated_at datetime2 NOT NULL,
        updated_by varchar(255) NOT NULL,
        version bigint NOT NULL CONSTRAINT df_automatic_promotion_version DEFAULT 0,
        CONSTRAINT ck_automatic_promotion_kind CHECK(kind IN ('WELCOME','LOYAL','VALUE','SILVER','GOLD')),
        CONSTRAINT ck_automatic_promotion_percent CHECK(
            (kind='VALUE' AND discount_percent IS NULL) OR (kind<>'VALUE' AND discount_percent IS NOT NULL AND discount_percent>0 AND discount_percent<100)),
        CONSTRAINT ck_automatic_promotion_cap CHECK(
            (kind='WELCOME' AND discount_cap IS NOT NULL AND discount_cap>0 AND discount_cap=FLOOR(discount_cap)) OR
            (kind IN ('SILVER','GOLD') AND (discount_cap IS NULL OR (discount_cap>0 AND discount_cap=FLOOR(discount_cap)))) OR
            (kind IN ('LOYAL','VALUE') AND discount_cap IS NULL)),
        CONSTRAINT ck_automatic_promotion_loyal CHECK(
            (kind='LOYAL' AND minimum_months IS NOT NULL AND minimum_months BETWEEN 1 AND 1200
                AND renewal_grace_days IS NOT NULL AND renewal_grace_days BETWEEN 0 AND 3650) OR
            (kind<>'LOYAL' AND minimum_months IS NULL AND renewal_grace_days IS NULL)),
        CONSTRAINT ck_automatic_promotion_value CHECK(
            (kind='VALUE' AND value_threshold IS NOT NULL AND value_discount IS NOT NULL AND premium_threshold IS NOT NULL AND premium_discount IS NOT NULL
                AND value_threshold>0 AND value_discount>0 AND value_discount<value_threshold AND premium_threshold>value_threshold
                AND premium_discount>=value_discount AND premium_discount<premium_threshold
                AND value_threshold=FLOOR(value_threshold) AND value_discount=FLOOR(value_discount)
                AND premium_threshold=FLOOR(premium_threshold) AND premium_discount=FLOOR(premium_discount)) OR
            (kind<>'VALUE' AND value_threshold IS NULL AND value_discount IS NULL AND premium_threshold IS NULL AND premium_discount IS NULL)),
        CONSTRAINT ck_automatic_promotion_dates CHECK(start_date IS NULL OR end_date IS NULL OR end_date>=start_date),
        CONSTRAINT ck_automatic_promotion_version CHECK(version>=0)
    );
END;
INSERT dbo.automatic_promotions(kind,active,discount_percent,discount_cap,minimum_months,renewal_grace_days,
    value_threshold,value_discount,premium_threshold,premium_discount,updated_at,updated_by,version)
SELECT initial.kind,0,initial.discount_percent,initial.discount_cap,initial.minimum_months,initial.renewal_grace_days,
    initial.value_threshold,initial.value_discount,initial.premium_threshold,initial.premium_discount,SYSDATETIME(),'MIGRATION_V717',0
FROM (VALUES
    ('WELCOME',CAST(10 AS numeric(5,2)),CAST(50000 AS numeric(15,2)),NULL,NULL,NULL,NULL,NULL,NULL),
    ('LOYAL',10,NULL,6,30,NULL,NULL,NULL,NULL),
    ('VALUE',NULL,NULL,NULL,NULL,500000,50000,1000000,100000),
    ('SILVER',5,NULL,NULL,NULL,NULL,NULL,NULL,NULL),
    ('GOLD',10,NULL,NULL,NULL,NULL,NULL,NULL,NULL)
) initial(kind,discount_percent,discount_cap,minimum_months,renewal_grace_days,value_threshold,value_discount,premium_threshold,premium_discount)
WHERE NOT EXISTS(SELECT 1 FROM dbo.automatic_promotions existing WITH(UPDLOCK,HOLDLOCK) WHERE existing.kind=initial.kind);
COMMIT;
SELECT * FROM dbo.automatic_promotions ORDER BY kind;
PRINT 'Five configuration rows only; initially paused. Existing configurations are never overwritten. No Payment, Membership, User, Promotion code or SePay approval changed.';
PRINT 'Verify SELECT/UPDATE runtime permissions before starting updated Hibernate validate. Admin activates individual programs after deployment review.';
