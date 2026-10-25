SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.payments', 'gateway_transaction_no') IS NULL
    ALTER TABLE dbo.payments ADD gateway_transaction_no varchar(100) NULL;
IF COL_LENGTH('dbo.payments', 'gateway_response_code') IS NULL
    ALTER TABLE dbo.payments ADD gateway_response_code varchar(10) NULL;
COMMIT TRANSACTION;
