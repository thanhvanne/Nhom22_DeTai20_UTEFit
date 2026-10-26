:On Error exit
USE [UTEFitLiveDemo];
GO
SET XACT_ABORT ON;
DECLARE @OperatorApproved BIT=0;
DECLARE @Price DECIMAL(15,2)=NULL;
DECLARE @DurationDays INT=30;
IF DB_NAME() <> N'UTEFitLiveDemo' OR @OperatorApproved <> 1 OR @Price IS NULL
    OR @Price <= 0 OR @Price <> FLOOR(@Price) OR @DurationDays <= 0
    THROW 50830, 'Set your approved real plan price and approval in a private working copy; no defaults for real money.', 1;
BEGIN TRY
    BEGIN TRANSACTION;
    IF EXISTS (SELECT 1 FROM dbo.branches WHERE name=N'UTEFit Live Demo')
        OR EXISTS (SELECT 1 FROM dbo.membership_plans WHERE name=N'Live Demo Membership')
        THROW 50831, 'Catalog already exists. Inspect or use Admin UI instead of duplicating it.', 1;
    INSERT dbo.branches(active,address,created_at,name,opening_time,closing_time)
        VALUES(1,N'Laptop demo - approved venue',SYSDATETIME(),N'UTEFit Live Demo','06:00','22:00');
    INSERT dbo.membership_plans(active,created_at,duration_days,name,price)
        VALUES(1,SYSDATETIME(),@DurationDays,N'Live Demo Membership',@Price);
    COMMIT;
    SELECT id,name,price,duration_days,active FROM dbo.membership_plans WHERE name=N'Live Demo Membership';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
