-- READ ONLY / SESSION IMPERSONATION ONLY. No GRANT, DDL or business-table DML.
-- Open SSMS as deployment DBA and select UTEFitDB explicitly.
SET NOCOUNT ON;
IF DB_NAME() <> N'UTEFitDB' THROW 51400,'Select UTEFitDB explicitly. No database switch performed.',1;
IF @@TRANCOUNT <> 0 THROW 51401,'Use an independent session.',1;
SELECT p.name,p.type_desc,p.is_disabled,IS_SRVROLEMEMBER('sysadmin',p.name) AS is_sysadmin,
    CONVERT(int,SERVERPROPERTY('IsIntegratedSecurityOnly')) AS windows_auth_only
FROM sys.server_principals p WHERE name=N'utefit_live_runtime';
SELECT r.name AS server_role FROM sys.server_role_members rm
JOIN sys.server_principals member ON member.principal_id=rm.member_principal_id
JOIN sys.server_principals r ON r.principal_id=rm.role_principal_id WHERE member.name=N'utefit_live_runtime';
SELECT permission_name,state_desc FROM sys.server_permissions WHERE grantee_principal_id=SUSER_ID(N'utefit_live_runtime');
SELECT name,type_desc,authentication_type_desc,
    CASE WHEN sid=SUSER_SID(N'utefit_live_runtime') THEN 1 ELSE 0 END AS sid_matches
FROM sys.database_principals WHERE name=N'utefit_live_runtime';
SELECT r.name AS database_role FROM sys.database_role_members rm
JOIN sys.database_principals member ON member.principal_id=rm.member_principal_id
JOIN sys.database_principals r ON r.principal_id=rm.role_principal_id WHERE member.name=N'utefit_live_runtime';
SELECT COUNT(*) AS table_count FROM sys.tables WHERE schema_id=SCHEMA_ID(N'dbo') AND is_ms_shipped=0;
SELECT id,environment,LEN(deployment_id) AS deployment_id_length,approved_at FROM dbo.sepay_deployment_guard;
SELECT name,CONVERT(nvarchar(100),value) AS marker FROM sys.extended_properties
WHERE class=0 AND name IN (N'UTEFit.DatabasePurpose',N'UTEFit.DemoSeed.Version');
SELECT
 (SELECT COUNT(*) FROM dbo.users WHERE email LIKE '%@utefit.test' AND enabled=1 AND status='ACTIVE') AS active_demo_users,
 (SELECT COUNT(*) FROM dbo.payments WHERE transaction_code LIKE 'DEMO-CASH-%') AS demo_cash_rows,
 (SELECT COUNT(*) FROM dbo.sepay_payment_details WHERE environment='TEST') AS sepay_test_orders,
 (SELECT COUNT(*) FROM dbo.sepay_webhook_transactions WHERE environment='TEST') AS sepay_test_receipts;
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
DECLARE @switched bit=0;
BEGIN TRY
 EXECUTE AS LOGIN=N'utefit_live_runtime'; SET @switched=1;
 SELECT HAS_DBACCESS(N'UTEFitDB') AS database_access,USER_NAME() AS mapped_user,
    HAS_PERMS_BY_NAME(DB_NAME(),'DATABASE','CONNECT') AS can_connect;
 SELECT table_name,
    HAS_PERMS_BY_NAME(N'dbo.'+table_name,'OBJECT','SELECT') AS can_select,
    HAS_PERMS_BY_NAME(N'dbo.'+table_name,'OBJECT','INSERT') AS can_insert,
    HAS_PERMS_BY_NAME(N'dbo.'+table_name,'OBJECT','UPDATE') AS can_update,
    HAS_PERMS_BY_NAME(N'dbo.'+table_name,'OBJECT','DELETE') AS can_delete,
    needs_update,needs_delete FROM @Required ORDER BY table_name;
 SELECT permission_name AS database_permission FROM sys.fn_my_permissions(NULL,'DATABASE');
 SELECT permission_name AS guard_permission FROM sys.fn_my_permissions(N'dbo.sepay_deployment_guard','OBJECT');
 SELECT columns.name AS guard_column,
    HAS_PERMS_BY_NAME('dbo.sepay_deployment_guard','OBJECT','UPDATE',columns.name,'COLUMN') AS can_update_column
 FROM sys.columns columns WHERE object_id=OBJECT_ID(N'dbo.sepay_deployment_guard');
 REVERT; SET @switched=0;
END TRY
BEGIN CATCH
 IF @switched=1 REVERT;
 SELECT ERROR_NUMBER() AS runtime_preflight_error;
END CATCH;
-- Successful impersonation proves authorization only, NOT password/TCP/TLS login.
