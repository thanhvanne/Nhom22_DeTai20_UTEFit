:On Error exit
USE [UTEFitLiveDemo];
GO
SET XACT_ABORT ON;
DECLARE @OperatorApproved BIT=0;
DECLARE @VerifiedEmail VARCHAR(100)=NULL;
IF DB_NAME() <> N'UTEFitLiveDemo' OR @OperatorApproved <> 1 OR @VerifiedEmail IS NULL
    THROW 50880, 'Register and verify the intended owner through the website, then approve a private working copy.', 1;
IF COALESCE(IS_ROLEMEMBER(N'db_owner'),0)<>1 AND COALESCE(IS_SRVROLEMEMBER(N'sysadmin'),0)<>1
    THROW 50881, 'Only the deployment DBA/owner may bootstrap Admin.', 1;
BEGIN TRY
    BEGIN TRANSACTION;
    IF EXISTS (SELECT 1 FROM dbo.users WHERE role='ADMIN')
        THROW 50882, 'An Admin already exists. Use normal administration instead of rerunning bootstrap.', 1;
    UPDATE dbo.users SET role='ADMIN',token_version=COALESCE(token_version,0)+1,updated_at=SYSDATETIME()
        WHERE email=@VerifiedEmail AND role='MEMBER' AND status='ACTIVE' AND enabled=1;
    IF @@ROWCOUNT<>1 THROW 50883, 'Expected one verified active owner account. No changes committed.', 1;
    COMMIT;
    SELECT N'Admin bootstrapped. Sign out and sign in again; no password or enabled/status was changed.' AS result;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
