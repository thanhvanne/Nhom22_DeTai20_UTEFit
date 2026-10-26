SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    IF OBJECT_ID(N'dbo.sepay_payment_details', N'U') IS NULL
        OR OBJECT_ID(N'dbo.sepay_webhook_transactions', N'U') IS NULL
        THROW 50708, 'Apply and verify V706 before V708.', 1;
    IF OBJECT_ID(N'dbo.sepay_deployment_guard', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.sepay_deployment_guard (
            id INT NOT NULL CONSTRAINT pk_sepay_deployment_guard PRIMARY KEY,
            environment VARCHAR(4) NOT NULL,
            deployment_id VARCHAR(100) NOT NULL,
            approved_at DATETIME2 NOT NULL,
            CONSTRAINT ck_sepay_guard_singleton CHECK(id=1),
            CONSTRAINT ck_sepay_guard_environment CHECK(environment IN ('TEST','LIVE'))
        );
    END;
    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
