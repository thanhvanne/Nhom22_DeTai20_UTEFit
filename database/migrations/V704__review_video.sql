SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.reviews', 'video_url') IS NULL
    ALTER TABLE dbo.reviews ADD video_url VARCHAR(500) NULL;
IF COL_LENGTH('dbo.reviews', 'video_public_id') IS NULL
    ALTER TABLE dbo.reviews ADD video_public_id VARCHAR(255) NULL;
COMMIT;
