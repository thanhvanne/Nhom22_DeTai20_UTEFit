-- Apply once to the intended existing database before starting the updated app.
-- Additive migration: does not reset or change existing financial records.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.memberships', 'promotion_code') IS NULL
    ALTER TABLE dbo.memberships ADD promotion_code varchar(50) NULL;
IF COL_LENGTH('dbo.memberships', 'original_price') IS NULL
    ALTER TABLE dbo.memberships ADD original_price numeric(15,2) NULL;
IF COL_LENGTH('dbo.memberships', 'discount_percent') IS NULL
    ALTER TABLE dbo.memberships ADD discount_percent numeric(5,2) NULL;
COMMIT TRANSACTION;
