-- SQL Server. Preserves existing payments, memberships and bank snapshots.
-- DBA applies after backup with the application stopped. No approval/Live flag changes.
SET XACT_ABORT ON;
IF @@TRANCOUNT <> 0 THROW 51520,'Use an independent session.',1;
BEGIN TRY
 BEGIN TRANSACTION;
 IF COL_LENGTH('dbo.payments','cancellation_requested_at') IS NULL ALTER TABLE dbo.payments ADD cancellation_requested_at datetime2 NULL;
 IF COL_LENGTH('dbo.payments','cancelled_at') IS NULL ALTER TABLE dbo.payments ADD cancelled_at datetime2 NULL;
 IF COL_LENGTH('dbo.payments','cancelled_by') IS NULL ALTER TABLE dbo.payments ADD cancelled_by varchar(100) NULL;
 IF COL_LENGTH('dbo.payments','cancellation_reason') IS NULL ALTER TABLE dbo.payments ADD cancellation_reason nvarchar(500) NULL;
 IF COL_LENGTH('dbo.payments','late_gateway_evidence') IS NULL ALTER TABLE dbo.payments ADD late_gateway_evidence nvarchar(2000) NULL;
 DECLARE @drop nvarchar(max)=N'';
 SELECT @drop=@drop+N'ALTER TABLE dbo.payments DROP CONSTRAINT '+QUOTENAME(name)+N';'
 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID('dbo.payments')
 AND (parent_column_id=COLUMNPROPERTY(OBJECT_ID('dbo.payments'),'status','ColumnId')
     OR definition LIKE '%[[]status[]]%');
 IF @drop<>N'' EXEC sys.sp_executesql @drop;
 ALTER TABLE dbo.payments WITH CHECK ADD CONSTRAINT ck_payment_status_v714
    CHECK(status IN ('PENDING','CANCEL_REQUESTED','CANCELLED','PAID','FAILED','REFUNDED'));
 IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.payments') AND name='ix_payment_cancellation_queue')
    CREATE INDEX ix_payment_cancellation_queue ON dbo.payments(status,created_at) INCLUDE(membership_id,method);
 COMMIT TRANSACTION;
END TRY
BEGIN CATCH
 IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
 THROW;
END CATCH;
