:On Error exit
USE [UTEFitLiveDemo];
GO
DECLARE @OperatorApproved BIT=0;
DECLARE @BackupFile NVARCHAR(4000)=NULL;
IF DB_NAME() <> N'UTEFitLiveDemo' OR @OperatorApproved <> 1 OR @BackupFile IS NULL OR LEN(@BackupFile)<10
    THROW 50870, 'Choose a new private backup path writable by SQL Server service and approve first.', 1;
BACKUP DATABASE [UTEFitLiveDemo] TO DISK=@BackupFile WITH COPY_ONLY,CHECKSUM;
RESTORE VERIFYONLY FROM DISK=@BackupFile WITH CHECKSUM;
SELECT N'Backup verified; still perform an actual restore drill into a separate database before first live payment' AS result;
