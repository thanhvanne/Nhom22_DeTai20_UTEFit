SET NOCOUNT ON;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;
IF DB_NAME() <> N'UTEFitDB' THROW 51600, 'Select UTEFitDB explicitly. Do not run on LiveDemo or another database.', 1;
IF @@TRANCOUNT <> 0 THROW 51601, 'Use a new SSMS session.', 1;
IF COL_LENGTH('dbo.memberships','promotion_code') IS NULL THROW 51602, 'V715 is required first.', 1;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.memberships','offer_kind') IS NULL ALTER TABLE dbo.memberships ADD offer_kind varchar(24) NULL;
IF COL_LENGTH('dbo.memberships','offer_label') IS NULL ALTER TABLE dbo.memberships ADD offer_label nvarchar(150) NULL;
IF COL_LENGTH('dbo.memberships','discount_amount') IS NULL ALTER TABLE dbo.memberships ADD discount_amount numeric(15,2) NULL;
IF COL_LENGTH('dbo.memberships','pricing_selection') IS NULL ALTER TABLE dbo.memberships ADD pricing_selection varchar(12) NULL;
IF COL_LENGTH('dbo.promotions','min_order_amount') IS NULL ALTER TABLE dbo.promotions ADD min_order_amount numeric(15,2) NULL;
IF COL_LENGTH('dbo.promotions','max_discount_amount') IS NULL ALTER TABLE dbo.promotions ADD max_discount_amount numeric(15,2) NULL;
IF COL_LENGTH('dbo.promotions','per_user_limit') IS NULL ALTER TABLE dbo.promotions ADD per_user_limit int NULL;
IF OBJECT_ID('dbo.loyalty_payment_exclusions','U') IS NULL
BEGIN
    CREATE TABLE dbo.loyalty_payment_exclusions (
        payment_id bigint NOT NULL CONSTRAINT pk_loyalty_payment_exclusions PRIMARY KEY,
        reason nvarchar(250) NOT NULL,
        reviewed_at datetime2 NOT NULL,
        CONSTRAINT fk_loyalty_exclusion_payment FOREIGN KEY(payment_id) REFERENCES dbo.payments(id)
    );
END;
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.memberships') AND name='ix_membership_promotion_user')
    CREATE INDEX ix_membership_promotion_user ON dbo.memberships(promotion_code,user_id,status);
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.memberships') AND name='ux_membership_pending_welcome')
    EXEC(N'CREATE UNIQUE INDEX ux_membership_pending_welcome ON dbo.memberships(user_id) WHERE offer_kind=''WELCOME'' AND status=''PENDING''');
IF NOT EXISTS(SELECT 1 FROM sys.check_constraints WHERE name='ck_promotion_loyalty_conditions')
    EXEC(N'ALTER TABLE dbo.promotions WITH CHECK ADD CONSTRAINT ck_promotion_loyalty_conditions CHECK ((min_order_amount IS NULL OR min_order_amount>0) AND (max_discount_amount IS NULL OR max_discount_amount>0) AND (per_user_limit IS NULL OR per_user_limit>0))');
IF NOT EXISTS(SELECT 1 FROM sys.check_constraints WHERE name='ck_membership_discount_snapshot')
    EXEC(N'ALTER TABLE dbo.memberships WITH CHECK ADD CONSTRAINT ck_membership_discount_snapshot CHECK (discount_amount IS NULL OR (discount_amount>=0 AND original_price>0 AND quoted_price>0 AND original_price-discount_amount=quoted_price))');
COMMIT;
PRINT 'Additive schema only. No Payment, Membership price, Promotion quantity, account or SePay approval data changed.';
PRINT 'Do not start updated Hibernate validate until schema and runtime SELECT on loyalty_payment_exclusions are verified.';
