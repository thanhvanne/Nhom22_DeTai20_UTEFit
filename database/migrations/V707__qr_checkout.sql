SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    IF OBJECT_ID(N'dbo.qr_checkin_tokens', N'U') IS NULL OR OBJECT_ID(N'dbo.checkins', N'U') IS NULL
        THROW 50770, 'Missing QR/checkins prerequisites. Apply existing schema first.', 1;

    IF COL_LENGTH('dbo.qr_checkin_tokens', 'purpose') IS NULL
        ALTER TABLE dbo.qr_checkin_tokens ADD purpose VARCHAR(16) NOT NULL
            CONSTRAINT df_qr_token_purpose DEFAULT ('CHECK_IN') WITH VALUES;
    IF COL_LENGTH('dbo.qr_checkin_tokens', 'checkin_id') IS NULL
        ALTER TABLE dbo.qr_checkin_tokens ADD checkin_id BIGINT NULL;

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id=OBJECT_ID('dbo.qr_checkin_tokens') AND name='fk_qr_token_checkin')
        EXEC(N'ALTER TABLE dbo.qr_checkin_tokens WITH CHECK ADD CONSTRAINT fk_qr_token_checkin
            FOREIGN KEY(checkin_id) REFERENCES dbo.checkins(id);');
    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID('dbo.qr_checkin_tokens') AND name='ck_qr_token_purpose_target')
        EXEC(N'ALTER TABLE dbo.qr_checkin_tokens WITH CHECK ADD CONSTRAINT ck_qr_token_purpose_target CHECK
            ((purpose=''CHECK_IN'' AND checkin_id IS NULL) OR (purpose=''CHECK_OUT'' AND checkin_id IS NOT NULL));');
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.qr_checkin_tokens') AND name='idx_qr_token_checkin')
        EXEC(N'CREATE INDEX idx_qr_token_checkin ON dbo.qr_checkin_tokens(checkin_id) WHERE checkin_id IS NOT NULL;');
    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
