-- SQL Server. Additive type widening only; no prices, payments or dates changed.
SET XACT_ABORT ON;
IF @@TRANCOUNT <> 0 THROW 51500,'Use an independent session.',1;
BEGIN TRY
 BEGIN TRANSACTION;
 ALTER TABLE dbo.branches ALTER COLUMN name nvarchar(150) NOT NULL;
 ALTER TABLE dbo.branches ALTER COLUMN address nvarchar(255) NOT NULL;
 ALTER TABLE dbo.branches ALTER COLUMN description nvarchar(1000) NULL;
 ALTER TABLE dbo.membership_plans ALTER COLUMN name nvarchar(100) NOT NULL;
 ALTER TABLE dbo.membership_plans ALTER COLUMN description nvarchar(1500) NULL;
 COMMIT TRANSACTION;
END TRY
BEGIN CATCH
 IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
 THROW;
END CATCH;
