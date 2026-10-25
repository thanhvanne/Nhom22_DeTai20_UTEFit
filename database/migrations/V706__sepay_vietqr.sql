SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    DECLARE @methodColumn INT = COLUMNPROPERTY(OBJECT_ID('dbo.payments'), 'method', 'ColumnId');
    IF @methodColumn IS NULL THROW 50760, 'Missing payments.method. Apply prerequisite schema before V706.', 1;
    DECLARE @name SYSNAME, @definition NVARCHAR(MAX), @normalized NVARCHAR(MAX), @drop NVARCHAR(MAX) = N'';
    DECLARE methodChecks CURSOR LOCAL FAST_FORWARD FOR
        SELECT DISTINCT checks.name, checks.definition
        FROM sys.check_constraints checks
        LEFT JOIN sys.sql_expression_dependencies deps ON deps.referencing_id = checks.object_id
        WHERE checks.parent_object_id = OBJECT_ID('dbo.payments')
          AND (checks.parent_column_id = @methodColumn
               OR (deps.referenced_id = OBJECT_ID('dbo.payments') AND deps.referenced_minor_id = @methodColumn));
    OPEN methodChecks;
    FETCH NEXT FROM methodChecks INTO @name, @definition;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        SET @normalized = UPPER(@definition);
        SET @normalized = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(@normalized,
            '[',''),']',''),'(',''),')',''),' ',''),CHAR(9),''),CHAR(10),''),CHAR(13),'');
        IF CHARINDEX('METHOD=''CASH''', @normalized) = 0 OR CHARINDEX('METHOD=''VNPAY''', @normalized) = 0
            THROW 50761, 'Unknown PaymentMethod constraint. Review manually; no changes applied.', 1;
        SET @normalized = REPLACE(REPLACE(REPLACE(@normalized,'METHOD=''CASH''',''),'METHOD=''VNPAY''',''),'METHOD=''SEPAY''','');
        SET @normalized = REPLACE(@normalized, 'OR', '');
        IF @normalized <> '' THROW 50762, 'Complex PaymentMethod constraint. Review manually; no changes applied.', 1;
        SET @drop = @drop + N'ALTER TABLE dbo.payments DROP CONSTRAINT ' + QUOTENAME(@name) + N';';
        FETCH NEXT FROM methodChecks INTO @name, @definition;
    END;
    CLOSE methodChecks;
    DEALLOCATE methodChecks;
    IF @drop <> N'' EXEC sys.sp_executesql @drop;
    ALTER TABLE dbo.payments WITH CHECK ADD CONSTRAINT ck_payments_method_sepay CHECK (method IN ('CASH','VNPAY','SEPAY'));

    IF OBJECT_ID(N'dbo.sepay_payment_details', N'U') IS NULL
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
            CONSTRAINT ck_sepay_details_environment CHECK(environment IN ('TEST','LIVE'))
        );
    END;
    IF OBJECT_ID(N'dbo.sepay_webhook_transactions', N'U') IS NULL
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
            CONSTRAINT ck_sepay_receipt_environment CHECK(environment IN ('TEST','LIVE')),
            CONSTRAINT ck_sepay_receipt_outcome CHECK(outcome IN ('REVIEW','MATCHED','IGNORED')),
            CONSTRAINT ck_sepay_receipt_amount CHECK(amount > 0),
            CONSTRAINT ck_sepay_receipt_direction CHECK(transfer_type IN ('in','out'))
        );
    END;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.payments') AND name='uk_payments_sepay_reference')
        CREATE UNIQUE INDEX uk_payments_sepay_reference ON dbo.payments(transaction_code) WHERE method='SEPAY' AND transaction_code IS NOT NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.sepay_webhook_transactions') AND name='uk_sepay_bank_reference')
        CREATE UNIQUE INDEX uk_sepay_bank_reference ON dbo.sepay_webhook_transactions(environment,gateway,account_number,bank_reference) WHERE bank_reference IS NOT NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.sepay_webhook_transactions') AND name='idx_sepay_outcome_received')
        CREATE INDEX idx_sepay_outcome_received ON dbo.sepay_webhook_transactions(outcome,received_at);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.sepay_webhook_transactions') AND name='idx_sepay_receipt_reference')
        CREATE INDEX idx_sepay_receipt_reference ON dbo.sepay_webhook_transactions(payment_reference);
    COMMIT;
END TRY
BEGIN CATCH
    IF CURSOR_STATUS('local','methodChecks') >= 0 CLOSE methodChecks;
    IF CURSOR_STATUS('local','methodChecks') >= -1 DEALLOCATE methodChecks;
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
