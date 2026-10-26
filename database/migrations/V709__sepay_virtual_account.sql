SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    IF OBJECT_ID(N'dbo.sepay_payment_details',N'U') IS NULL
        OR OBJECT_ID(N'dbo.sepay_webhook_transactions',N'U') IS NULL
        THROW 50709, 'Missing SePay V706 schema. Stop and review.', 1;
    IF COL_LENGTH('dbo.sepay_payment_details','va_number') IS NULL
        ALTER TABLE dbo.sepay_payment_details ADD va_number VARCHAR(50) NULL;
    IF COL_LENGTH('dbo.sepay_webhook_transactions','sub_account') IS NULL
        ALTER TABLE dbo.sepay_webhook_transactions ADD sub_account VARCHAR(50) NULL;
    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
