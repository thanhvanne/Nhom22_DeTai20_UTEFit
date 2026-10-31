SET NOCOUNT ON;
SET XACT_ABORT ON;
IF DB_NAME() <> N'UTEFitDB' THROW 51800, 'Select UTEFitDB before running this script.', 1;
IF @@TRANCOUNT <> 0 THROW 51801, 'Use an independent session.', 1;
IF OBJECT_ID(N'dbo.users', N'U') IS NULL THROW 51802, 'Missing users table.', 1;
IF COL_LENGTH(N'dbo.users', N'first_name') NOT IN (50, 100)
    OR COL_LENGTH(N'dbo.users', N'last_name') NOT IN (50, 100)
    THROW 51803, 'Unexpected name column length. Review the schema first.', 1;
BEGIN TRY
    BEGIN TRANSACTION;
    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID(N'dbo.users') AND name=N'first_name' AND TYPE_NAME(user_type_id)<>N'nvarchar')
        ALTER TABLE dbo.users ALTER COLUMN first_name nvarchar(50) NOT NULL;
    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID(N'dbo.users') AND name=N'last_name' AND TYPE_NAME(user_type_id)<>N'nvarchar')
        ALTER TABLE dbo.users ALTER COLUMN last_name nvarchar(50) NOT NULL;
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
SELECT name AS column_name, TYPE_NAME(user_type_id) AS data_type, max_length
FROM sys.columns WHERE object_id=OBJECT_ID(N'dbo.users') AND name IN (N'first_name',N'last_name');
PRINT N'Name columns support Unicode. Existing question marks cannot be recovered automatically; update only the verified account using its exact name in the profile form.';
