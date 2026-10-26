:On Error exit
USE [UTEFitLiveDemo];
GO
DECLARE @OperatorApproved BIT=0;
IF DB_NAME() <> N'UTEFitLiveDemo' OR @OperatorApproved <> 1
    THROW 50850, 'Review permissions and approve a private working copy first.', 1;
IF SUSER_ID(N'utefit_live_runtime') IS NULL
    THROW 50851, 'Create a dedicated login using SSMS security UI; never put its password into this script.', 1;
IF IS_SRVROLEMEMBER(N'sysadmin',N'utefit_live_runtime')=1
    THROW 50852, 'Runtime must not be sysadmin.', 1;
IF DATABASE_PRINCIPAL_ID(N'utefit_live_runtime') IS NULL
    CREATE USER [utefit_live_runtime] FOR LOGIN [utefit_live_runtime] WITH DEFAULT_SCHEMA=[dbo];
IF IS_ROLEMEMBER(N'db_owner',N'utefit_live_runtime')=1
    THROW 50853, 'Remove broad ownership permissions before proceeding.', 1;
GRANT CONNECT TO [utefit_live_runtime];
GRANT SELECT, INSERT, UPDATE, DELETE ON SCHEMA::dbo TO [utefit_live_runtime];
DENY INSERT, UPDATE, DELETE, ALTER ON OBJECT::dbo.sepay_deployment_guard TO [utefit_live_runtime];
GRANT SELECT ON OBJECT::dbo.sepay_deployment_guard TO [utefit_live_runtime];
SELECT N'Inspect effective server, role and database permissions before using the login' AS result;
