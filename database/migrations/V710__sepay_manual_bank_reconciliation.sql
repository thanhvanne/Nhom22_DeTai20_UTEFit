SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF OBJECT_ID(N'dbo.sepay_payment_details', N'U') IS NULL
    THROW 51000, 'Apply V706 and V708-V709 before V710.', 1;
IF COL_LENGTH('dbo.sepay_payment_details', 'va_number') IS NULL
    THROW 51000, 'V709 is required before V710.', 1;
IF OBJECT_ID(N'dbo.sepay_deployment_guard', N'U') IS NULL
    OR COL_LENGTH('dbo.sepay_webhook_transactions', 'sub_account') IS NULL
    THROW 51000, 'V708 and V709 are required before V710.', 1;
IF OBJECT_ID(N'dbo.sepay_bank_reconciliations', N'U') IS NOT NULL
    THROW 51000, 'V710 table already exists. Inspect schema; do not rerun blindly.', 1;

CREATE TABLE dbo.sepay_bank_reconciliations (
    id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    payment_id bigint NOT NULL REFERENCES dbo.payments(id),
    environment varchar(4) NOT NULL CHECK (environment IN ('TEST','LIVE')),
    gateway varchar(100) NOT NULL,
    account_number varchar(50) NOT NULL,
    sub_account varchar(50) NULL,
    bank_reference varchar(100) NOT NULL,
    amount decimal(15,2) NOT NULL CHECK (amount > 0),
    bank_date datetime2 NOT NULL,
    method varchar(40) NOT NULL CHECK (method IN ('MANUAL_BANK_RECONCILIATION','AUTHENTICATED_WEBHOOK')),
    outcome varchar(20) NOT NULL CHECK (outcome IN ('VERIFIED','APPLIED','REFUNDED','WEBHOOK')),
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
    CONSTRAINT ck_sepay_manual_evidence CHECK (method <> 'MANUAL_BANK_RECONCILIATION'
        OR (environment = 'LIVE' AND verified_by IS NOT NULL AND evidence IS NOT NULL AND reason IS NOT NULL)),
    CONSTRAINT ck_sepay_resolution CHECK (outcome NOT IN ('APPLIED','REFUNDED')
        OR (resolved_by IS NOT NULL AND resolved_at IS NOT NULL)),
    CONSTRAINT ck_sepay_refund_evidence CHECK (outcome <> 'REFUNDED'
        OR (refund_reference IS NOT NULL AND refunded_at IS NOT NULL AND resolution_evidence IS NOT NULL))
);
CREATE UNIQUE INDEX uk_sepay_refund_reference ON dbo.sepay_bank_reconciliations(refund_reference)
    WHERE refund_reference IS NOT NULL;
CREATE INDEX ix_sepay_bank_reconciliation_payment ON dbo.sepay_bank_reconciliations(payment_id,id);

COMMIT TRANSACTION;
