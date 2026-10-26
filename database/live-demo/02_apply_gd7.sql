:On Error exit
:setvar RepoRoot "F:\documents\LaptrinhWEB\DuanLTW"
USE [UTEFitLiveDemo];
GO
IF DB_NAME() <> N'UTEFitLiveDemo' OR @@TRANCOUNT <> 0
    THROW 50820, 'Use only a dedicated LiveDemo session.', 1;
IF OBJECT_ID(N'dbo.users',N'U') IS NULL OR OBJECT_ID(N'dbo.payments',N'U') IS NULL
    THROW 50821, 'Create the core schema first.', 1;
GO
:r "$(RepoRoot)\database\migrations\V701__vnpay.sql"
GO
:r "$(RepoRoot)\database\migrations\V702__qr_checkin.sql"
GO
:r "$(RepoRoot)\database\migrations\V703__favorites.sql"
GO
:r "$(RepoRoot)\database\migrations\V704__review_video.sql"
GO
:r "$(RepoRoot)\database\migrations\V705__manager_scope.sql"
GO
:r "$(RepoRoot)\database\migrations\V706__sepay_vietqr.sql"
GO
:r "$(RepoRoot)\database\migrations\V707__qr_checkout.sql"
GO
:r "$(RepoRoot)\database\migrations\V708__sepay_live_guard.sql"
GO
IF (SELECT COUNT(*) FROM sys.tables WHERE schema_id=SCHEMA_ID('dbo') AND is_ms_shipped=0) <> 25
    THROW 50822, 'Expected twenty-five tables. Review the migration output.', 1;
SELECT N'Schema complete; no data seeded and no approval granted' AS result;
