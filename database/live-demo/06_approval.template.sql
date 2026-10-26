:On Error exit
USE [UTEFitLiveDemo];
GO
SET XACT_ABORT ON;
DECLARE @OperatorApproved BIT=0;
DECLARE @DeploymentId VARCHAR(100)=NULL;
IF DB_NAME() <> N'UTEFitLiveDemo' OR @@TRANCOUNT <> 0 OR @OperatorApproved <> 1
    OR @DeploymentId IS NULL OR LEN(@DeploymentId)<16 OR @DeploymentId LIKE 'YOUR[_]%'
    THROW 50860, 'No approval granted. Requires explicit owner decision and a private configured copy.', 1;
IF COALESCE(IS_ROLEMEMBER(N'db_owner'),0)<>1 AND COALESCE(IS_SRVROLEMEMBER(N'sysadmin'),0)<>1
    THROW 50861, 'Only the deployment DBA/owner may approve.', 1;
BEGIN TRY
    BEGIN TRANSACTION;
    IF EXISTS (SELECT 1 FROM dbo.sepay_deployment_guard WITH (UPDLOCK,HOLDLOCK))
        THROW 50862, 'An approval already exists. Inspect; never overwrite it.', 1;
    IF EXISTS (SELECT 1 FROM dbo.sepay_payment_details WHERE environment='TEST')
        OR EXISTS (SELECT 1 FROM dbo.sepay_webhook_transactions WHERE environment='TEST')
        THROW 50863, 'Test data found. Stop and reconcile; never delete it to pass this guard.', 1;
    INSERT dbo.sepay_deployment_guard(id,environment,deployment_id,approved_at)
        VALUES(1,'LIVE',@DeploymentId,SYSDATETIME());
    COMMIT;
    SELECT id,environment,deployment_id,approved_at FROM dbo.sepay_deployment_guard;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
