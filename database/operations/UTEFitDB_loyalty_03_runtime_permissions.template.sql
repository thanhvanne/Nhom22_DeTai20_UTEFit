SET NOCOUNT ON;
SET XACT_ABORT ON;
IF DB_NAME() <> N'UTEFitDB' THROW 51620, 'Wrong database. Select UTEFitDB.', 1;
IF OBJECT_ID('dbo.loyalty_payment_exclusions','U') IS NULL THROW 51621, 'V716 schema required.', 1;
IF DATABASE_PRINCIPAL_ID(N'utefit_live_runtime') IS NULL THROW 51622, 'Existing runtime user required; this script never creates or remaps it.', 1;
DECLARE @ApproveAdditionalReadPermission bit=0;
IF @ApproveAdditionalReadPermission=1
    GRANT SELECT ON OBJECT::dbo.loyalty_payment_exclusions TO [utefit_live_runtime];
EXECUTE AS USER=N'utefit_live_runtime';
SELECT SESSIONPROPERTY('ANSI_NULLS') AS ansi_nulls, SESSIONPROPERTY('ANSI_PADDING') AS ansi_padding,
       SESSIONPROPERTY('ANSI_WARNINGS') AS ansi_warnings, SESSIONPROPERTY('ARITHABORT') AS arithabort,
       SESSIONPROPERTY('CONCAT_NULL_YIELDS_NULL') AS concat_null_yields_null,
       SESSIONPROPERTY('QUOTED_IDENTIFIER') AS quoted_identifier,
       SESSIONPROPERTY('NUMERIC_ROUNDABORT') AS numeric_roundabort,
       (SELECT compatibility_level FROM sys.databases WHERE name=DB_NAME()) AS compatibility_level;
SELECT HAS_PERMS_BY_NAME('dbo.loyalty_payment_exclusions','OBJECT','SELECT') AS exclusion_select,
       HAS_PERMS_BY_NAME('dbo.loyalty_payment_exclusions','OBJECT','INSERT') AS exclusion_insert_must_be_zero,
       HAS_PERMS_BY_NAME('dbo.loyalty_payment_exclusions','OBJECT','UPDATE') AS exclusion_update_must_be_zero,
       HAS_PERMS_BY_NAME('dbo.loyalty_payment_exclusions','OBJECT','DELETE') AS exclusion_delete_must_be_zero,
       HAS_PERMS_BY_NAME('dbo.sepay_deployment_guard','OBJECT','SELECT') AS guard_select,
       HAS_PERMS_BY_NAME('dbo.sepay_deployment_guard','OBJECT','INSERT') AS guard_insert_must_be_zero,
       HAS_PERMS_BY_NAME('dbo.sepay_deployment_guard','OBJECT','UPDATE') AS guard_update_must_be_zero,
       HAS_PERMS_BY_NAME('dbo.sepay_deployment_guard','OBJECT','DELETE') AS guard_delete_must_be_zero;
REVERT;
PRINT 'No existing grant, role, guard approval or authentication was changed. Review the effective permission result; do not assume PASS.';
PRINT 'SESSIONPROPERTY describes this SSMS session, not the JDBC pool. Verify the same required filtered-index SET options on a runtime JDBC connection before rollout.';
