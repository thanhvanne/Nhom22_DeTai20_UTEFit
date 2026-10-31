SET NOCOUNT ON;
IF DB_NAME() <> N'UTEFitDB' THROW 51710, 'Select UTEFitDB explicitly.', 1;
IF OBJECT_ID('dbo.automatic_promotions','U') IS NULL THROW 51711, 'Review/apply V717 first.', 1;
IF DATABASE_PRINCIPAL_ID(N'utefit_live_runtime') IS NULL THROW 51712, 'Existing runtime user required. No login or user is created.', 1;
DECLARE @ApproveProgramPermissions bit=0;
SELECT kind,active,discount_percent,discount_cap,minimum_months,renewal_grace_days,value_threshold,value_discount,
    premium_threshold,premium_discount,start_date,end_date,version,updated_at,updated_by
FROM dbo.automatic_promotions ORDER BY kind;
IF @ApproveProgramPermissions=1
    GRANT SELECT, UPDATE ON OBJECT::dbo.automatic_promotions TO [utefit_live_runtime];
EXECUTE AS USER=N'utefit_live_runtime';
SELECT HAS_PERMS_BY_NAME('dbo.automatic_promotions','OBJECT','SELECT') AS program_select,
       HAS_PERMS_BY_NAME('dbo.automatic_promotions','OBJECT','UPDATE') AS program_update,
       HAS_PERMS_BY_NAME('dbo.automatic_promotions','OBJECT','INSERT') AS program_insert_not_required,
       HAS_PERMS_BY_NAME('dbo.automatic_promotions','OBJECT','DELETE') AS program_delete_not_required,
       HAS_PERMS_BY_NAME('dbo.sepay_deployment_guard','OBJECT','INSERT') AS guard_insert_must_be_zero,
       HAS_PERMS_BY_NAME('dbo.sepay_deployment_guard','OBJECT','UPDATE') AS guard_update_must_be_zero,
       HAS_PERMS_BY_NAME('dbo.sepay_deployment_guard','OBJECT','DELETE') AS guard_delete_must_be_zero;
REVERT;
PRINT 'Preview by default. Grant SELECT/UPDATE only after approval if missing. No existing role, login, permission or guard approval was changed or revoked.';
