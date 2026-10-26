-- DBA APPROVAL TEMPLATE. Refuses by default; never run as application login.
-- Review backups, accounts, HTTPS and old outstanding orders before changing flags.
SET NOCOUNT ON;
SET XACT_ABORT ON;
DECLARE @Approve bit=1;
DECLARE @OfflinePreflightReviewed bit=1;
DECLARE @KnownDemoCredentialsRetired bit=1;
DECLARE @OldLiveDemoOrdersReconciled bit=1;
DECLARE @DemoCashAccountingReviewed bit=1;
DECLARE @DeploymentId varchar(128)='UTEFIT_LIVE_UTEFITDB_20261026';
IF DB_NAME()<>N'UTEFitDB' OR @@TRANCOUNT<>0 OR @Approve<>1
    OR @OfflinePreflightReviewed<>1 OR @KnownDemoCredentialsRetired<>1
    OR @OldLiveDemoOrdersReconciled<>1 OR @DemoCashAccountingReviewed<>1
    OR @DeploymentId IS NULL OR LEN(@DeploymentId) NOT BETWEEN 16 AND 100
    OR @DeploymentId COLLATE Latin1_General_100_BIN2 LIKE '%[^A-Za-z0-9_-]%'
    OR @DeploymentId LIKE 'YOUR[_]%' OR @DeploymentId LIKE 'REPLACE[_]%'
 THROW 51430,'No approval. Explicit owner decision, hardened accounts, backup/config/cutover review and a new deployment ID required.',1;
IF COALESCE(IS_SRVROLEMEMBER('sysadmin'),0)<>1
 THROW 51431,'Deployment sysadmin only; runtime must never approve itself.',1;
IF OBJECT_ID(N'dbo.sepay_deployment_guard',N'U') IS NULL
    OR COL_LENGTH('dbo.memberships','quoted_duration_unit') IS NULL
 THROW 51432,'Reviewed schema V701-V712 required.',1;
IF EXISTS(SELECT 1 FROM dbo.users WHERE email LIKE '%@utefit.test' AND enabled=1 AND status='ACTIVE')
 THROW 51433,'Active public demo accounts remain. Do not expose known demo credentials to real-money operations.',1;
IF EXISTS(SELECT 1 FROM dbo.sepay_payment_details WHERE environment='TEST')
    OR EXISTS(SELECT 1 FROM dbo.sepay_webhook_transactions WHERE environment='TEST')
 THROW 51434,'SePay Test records found. Never delete records merely to pass this guard.',1;
DECLARE @switched bit=0;
BEGIN TRY
 BEGIN TRANSACTION;
 EXECUTE AS LOGIN=N'utefit_live_runtime'; SET @switched=1;
 IF COALESCE(HAS_PERMS_BY_NAME('dbo.sepay_deployment_guard','OBJECT','SELECT'),0)<>1
    OR COALESCE(HAS_PERMS_BY_NAME('dbo.sepay_deployment_guard','OBJECT','INSERT'),1)<>0
    OR COALESCE(HAS_PERMS_BY_NAME('dbo.sepay_deployment_guard','OBJECT','UPDATE'),1)<>0
    OR COALESCE(HAS_PERMS_BY_NAME('dbo.sepay_deployment_guard','OBJECT','DELETE'),1)<>0
    OR COALESCE(HAS_PERMS_BY_NAME('dbo.sepay_deployment_guard','OBJECT','ALTER'),1)<>0
    OR COALESCE(HAS_PERMS_BY_NAME('dbo.sepay_deployment_guard','OBJECT','CONTROL'),1)<>0
    OR COALESCE(HAS_PERMS_BY_NAME('dbo.sepay_deployment_guard','OBJECT','TAKE OWNERSHIP'),1)<>0
    OR EXISTS(SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID(N'dbo.sepay_deployment_guard')
        AND COALESCE(HAS_PERMS_BY_NAME('dbo.sepay_deployment_guard','OBJECT','UPDATE',name,'COLUMN'),1)<>0)
    THROW 51435,'Runtime guard permissions are unsafe or not verifiable.',1;
 REVERT; SET @switched=0;
 IF EXISTS(SELECT 1 FROM dbo.sepay_deployment_guard WITH(UPDLOCK,HOLDLOCK))
    THROW 51436,'Approval already exists. Inspect it; never overwrite automatically.',1;
 INSERT dbo.sepay_deployment_guard(id,environment,deployment_id,approved_at)
    VALUES(1,'LIVE',@DeploymentId,SYSDATETIME());
 IF EXISTS(SELECT 1 FROM sys.extended_properties WHERE class=0 AND name=N'UTEFit.DatabasePurpose')
    EXEC sys.sp_updateextendedproperty @name=N'UTEFit.DatabasePurpose',@value=N'LIVE_FINANCIAL';
 ELSE EXEC sys.sp_addextendedproperty @name=N'UTEFit.DatabasePurpose',@value=N'LIVE_FINANCIAL';
 COMMIT TRANSACTION;
 SELECT id,environment,deployment_id,approved_at FROM dbo.sepay_deployment_guard;
 PRINT 'DB approval recorded by DBA. This does not enable application flags or verify a bank transaction.';
END TRY
BEGIN CATCH
 IF @switched=1 REVERT;
 IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
 THROW;
END CATCH;
