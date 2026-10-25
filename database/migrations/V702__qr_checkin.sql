SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF OBJECT_ID('dbo.qr_checkin_tokens', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.qr_checkin_tokens (
        id bigint IDENTITY PRIMARY KEY,
        user_id bigint NOT NULL REFERENCES dbo.users(id),
        token_hash varchar(64) NOT NULL,
        account_version int NOT NULL,
        expires_at datetime2 NOT NULL,
        used_at datetime2 NULL,
        CONSTRAINT uk_qr_token_hash UNIQUE(token_hash)
    );
END;
COMMIT TRANSACTION;
