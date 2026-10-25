SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF OBJECT_ID(N'dbo.favorites', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.favorites (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        user_id BIGINT NOT NULL,
        target_type VARCHAR(20) NOT NULL,
        target_id BIGINT NOT NULL,
        created_at DATETIME2 NOT NULL,
        CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES dbo.users(id),
        CONSTRAINT ck_favorites_type CHECK (target_type IN ('TRAINER','CLASS','BRANCH')),
        CONSTRAINT uk_favorite_member_target UNIQUE (user_id,target_type,target_id)
    );
END;
COMMIT;
