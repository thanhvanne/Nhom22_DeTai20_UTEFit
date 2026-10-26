:On Error exit
USE [master];
GO
IF @@TRANCOUNT <> 0 THROW 50800, 'Use a dedicated session without an outer transaction.', 1;
IF CONVERT(INT, SERVERPROPERTY('ProductMajorVersion')) < 15
    THROW 50801, 'This UTF8 bootstrap requires SQL Server 2019 or later. Review before proceeding.', 1;
IF DB_ID(N'UTEFitLiveDemo') IS NOT NULL
    THROW 50802, 'UTEFitLiveDemo already exists. Stop; never drop or overwrite it.', 1;
EXEC(N'CREATE DATABASE [UTEFitLiveDemo] COLLATE Latin1_General_100_CI_AS_SC_UTF8;');
SELECT name, collation_name, state_desc FROM sys.databases WHERE name=N'UTEFitLiveDemo';
GO
