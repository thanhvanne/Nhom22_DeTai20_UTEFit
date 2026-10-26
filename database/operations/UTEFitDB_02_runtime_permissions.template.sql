-- DBA TEMPLATE ONLY. Defaults to refusal. Does not change server login password.
-- Only UTEFitDB is touched; existing LiveDemo grants remain unchanged.
SET NOCOUNT ON;
SET XACT_ABORT ON;
DECLARE @Apply bit=0;
IF DB_NAME() <> N'UTEFitDB' OR @@TRANCOUNT <> 0 OR @Apply <> 1
 THROW 51410,'Select UTEFitDB, review this template and deliberately set @Apply=1.',1;
IF COALESCE(IS_SRVROLEMEMBER('sysadmin'),0)<>1
 THROW 51411,'A deployment sysadmin must review server-level privileges and execute this template.',1;
IF NOT EXISTS(SELECT 1 FROM sys.server_principals WHERE name=N'utefit_live_runtime' AND type='S' AND is_disabled=0)
 THROW 51412,'Runtime SQL login must already exist and be enabled. Set its secret privately in SSMS.',1;
IF EXISTS(SELECT 1 FROM sys.server_role_members WHERE member_principal_id=SUSER_ID(N'utefit_live_runtime'))
 OR EXISTS(SELECT 1 FROM sys.server_permissions WHERE grantee_principal_id=SUSER_ID(N'utefit_live_runtime')
    AND state IN ('G','W') AND permission_name NOT IN ('CONNECT SQL','VIEW ANY DATABASE','VIEW ANY DEFINITION'))
 OR EXISTS(SELECT 1 FROM sys.databases WHERE name=DB_NAME() AND owner_sid=SUSER_SID(N'utefit_live_runtime'))
 THROW 51413,'Runtime has excessive server privileges or owns this database. Review separately; no automatic revocation.',1;
IF OBJECT_ID(N'dbo.sepay_deployment_guard',N'U') IS NULL
 THROW 51414,'Reviewed schema V701-V712 is required.',1;
DECLARE @Required TABLE(table_name sysname,needs_update bit,needs_delete bit);
INSERT @Required VALUES
(N'branches',1,0),
(N'checkins',1,0),
(N'class_categories',1,0),
(N'class_registrations',1,0),
(N'class_schedules',1,0),
(N'conversations',1,0),
(N'favorites',1,1),
(N'fitness_classes',1,0),
(N'manager_branch_assignments',1,1),
(N'membership_branch_snapshot',0,0),
(N'membership_plan_branches',1,1),
(N'membership_plans',1,0),
(N'memberships',1,0),
(N'messages',1,0),
(N'notifications',1,0),
(N'otp_codes',1,0),
(N'payments',1,0),
(N'personal_training_schedules',1,0),
(N'promotions',1,0),
(N'qr_checkin_tokens',1,0),
(N'reviews',1,0),
(N'sepay_bank_reconciliations',1,0),
(N'sepay_payment_details',0,0),
(N'sepay_webhook_transactions',1,0),
(N'trainer_requests',1,0),
(N'trainers',1,0),
(N'users',1,0);
IF EXISTS(SELECT 1 FROM @Required WHERE OBJECT_ID(N'dbo.'+table_name,N'U') IS NULL)
 THROW 51415,'Required application table missing.',1;
DECLARE @switched bit=0;
BEGIN TRY
 BEGIN TRANSACTION;
 IF DATABASE_PRINCIPAL_ID(N'utefit_live_runtime') IS NULL
    CREATE USER [utefit_live_runtime] FOR LOGIN [utefit_live_runtime] WITH DEFAULT_SCHEMA=[dbo];
 IF NOT EXISTS(SELECT 1 FROM sys.database_principals WHERE name=N'utefit_live_runtime' AND sid=SUSER_SID(N'utefit_live_runtime'))
    THROW 51416,'Orphan/mismatched user SID. DBA must resolve mapping separately.',1;
 IF EXISTS(SELECT 1 FROM sys.database_role_members WHERE member_principal_id=DATABASE_PRINCIPAL_ID(N'utefit_live_runtime'))
    OR EXISTS(SELECT 1 FROM sys.schemas WHERE principal_id=DATABASE_PRINCIPAL_ID(N'utefit_live_runtime'))
    OR EXISTS(SELECT 1 FROM sys.objects WHERE principal_id=DATABASE_PRINCIPAL_ID(N'utefit_live_runtime'))
    THROW 51417,'Runtime must have no database-role membership or object/schema ownership. Review existing grants first.',1;
 GRANT CONNECT TO [utefit_live_runtime];
 DENY CREATE TABLE, CREATE PROCEDURE, CREATE FUNCTION,
CREATE VIEW, CREATE SYNONYM,
ALTER ANY SCHEMA, ALTER ANY ROLE,
     ALTER ANY USER, TAKE OWNERSHIP
TO [utefit_live_runtime];
 DENY ALTER ON SCHEMA::dbo TO [utefit_live_runtime];
 DECLARE @table sysname,@update bit,@delete bit,@sql nvarchar(max);
 DECLARE table_grants CURSOR LOCAL FAST_FORWARD FOR SELECT table_name,needs_update,needs_delete FROM @Required;
 OPEN table_grants; FETCH NEXT FROM table_grants INTO @table,@update,@delete;
 WHILE @@FETCH_STATUS=0
 BEGIN
    SET @sql=N'GRANT SELECT,INSERT'+CASE WHEN @update=1 THEN N',UPDATE' ELSE N'' END
        +CASE WHEN @delete=1 THEN N',DELETE' ELSE N'' END
        +N' ON OBJECT::dbo.'+QUOTENAME(@table)+N' TO [utefit_live_runtime];'
        +CASE WHEN @update=0 THEN N'DENY UPDATE ON OBJECT::dbo.'+QUOTENAME(@table)+N' TO [utefit_live_runtime];' ELSE N'' END
        +CASE WHEN @delete=0 THEN N'DENY DELETE ON OBJECT::dbo.'+QUOTENAME(@table)+N' TO [utefit_live_runtime];' ELSE N'' END;
    EXEC sys.sp_executesql @sql;
    FETCH NEXT FROM table_grants INTO @table,@update,@delete;
 END;
 CLOSE table_grants; DEALLOCATE table_grants;
 GRANT SELECT ON OBJECT::dbo.sepay_deployment_guard TO [utefit_live_runtime];
 DENY INSERT,UPDATE,DELETE,ALTER,TAKE OWNERSHIP,REFERENCES ON OBJECT::dbo.sepay_deployment_guard TO [utefit_live_runtime];
 EXECUTE AS LOGIN=N'utefit_live_runtime'; SET @switched=1;
 IF COALESCE(HAS_PERMS_BY_NAME(DB_NAME(),'DATABASE','CONNECT'),0)<>1
    OR EXISTS(SELECT 1 FROM @Required WHERE COALESCE(HAS_PERMS_BY_NAME(N'dbo.'+table_name,'OBJECT','SELECT'),0)<>1
        OR COALESCE(HAS_PERMS_BY_NAME(N'dbo.'+table_name,'OBJECT','INSERT'),0)<>1
        OR COALESCE(HAS_PERMS_BY_NAME(N'dbo.'+table_name,'OBJECT','UPDATE'),0)<>needs_update
        OR COALESCE(HAS_PERMS_BY_NAME(N'dbo.'+table_name,'OBJECT','DELETE'),0)<>needs_delete)
    THROW 51418,'Effective application permissions do not match the reviewed matrix.',1;
 IF COALESCE(HAS_PERMS_BY_NAME('dbo.sepay_deployment_guard','OBJECT','SELECT'),0)<>1
    OR COALESCE(HAS_PERMS_BY_NAME('dbo.sepay_deployment_guard','OBJECT','INSERT'),1)<>0
    OR COALESCE(HAS_PERMS_BY_NAME('dbo.sepay_deployment_guard','OBJECT','UPDATE'),1)<>0
    OR COALESCE(HAS_PERMS_BY_NAME('dbo.sepay_deployment_guard','OBJECT','DELETE'),1)<>0
    OR COALESCE(HAS_PERMS_BY_NAME('dbo.sepay_deployment_guard','OBJECT','ALTER'),1)<>0
    OR COALESCE(HAS_PERMS_BY_NAME('dbo.sepay_deployment_guard','OBJECT','CONTROL'),1)<>0
    OR COALESCE(HAS_PERMS_BY_NAME('dbo.sepay_deployment_guard','OBJECT','TAKE OWNERSHIP'),1)<>0
    OR EXISTS(SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID(N'dbo.sepay_deployment_guard')
        AND COALESCE(HAS_PERMS_BY_NAME('dbo.sepay_deployment_guard','OBJECT','UPDATE',name,'COLUMN'),1)<>0)
    THROW 51419,'Runtime can mutate/own/control the guard, or cannot prove guard isolation.',1;
 REVERT; SET @switched=0;
 COMMIT TRANSACTION;
 PRINT 'Runtime permissions applied. No deployment approval granted. Run preflight again.';
END TRY
BEGIN CATCH
 IF @switched=1 REVERT;
 IF CURSOR_STATUS('local','table_grants')>=0 CLOSE table_grants;
 IF CURSOR_STATUS('local','table_grants')>=-1 DEALLOCATE table_grants;
 IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
 THROW;
END CATCH;
