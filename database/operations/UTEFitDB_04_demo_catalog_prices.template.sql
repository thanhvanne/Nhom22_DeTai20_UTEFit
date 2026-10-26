-- Updates existing DEMO catalog only. No INSERT and no financial/history DML.
-- Backup first. Apply V713 first. Review preview, then deliberately set @Apply=1.
SET NOCOUNT ON;
SET XACT_ABORT ON;
DECLARE @Apply bit=0;
IF DB_NAME()<>N'UTEFitDB' OR @@TRANCOUNT<>0 THROW 51510,'Select UTEFitDB in an independent session.',1;
IF NOT EXISTS(SELECT 1 FROM sys.extended_properties WHERE class=0 AND name=N'UTEFit.DatabasePurpose' AND CONVERT(nvarchar(100),value)=N'DEVELOPMENT_ONLY')
 OR NOT EXISTS(SELECT 1 FROM sys.extended_properties WHERE class=0 AND name=N'UTEFit.DemoSeed.Version' AND CONVERT(nvarchar(100),value)=N'V712-demo55')
 OR EXISTS(SELECT 1 FROM dbo.sepay_deployment_guard)
 OR EXISTS(SELECT 1 FROM dbo.sepay_payment_details WHERE environment='LIVE')
 OR EXISTS(SELECT 1 FROM dbo.sepay_webhook_transactions WHERE environment='LIVE')
 THROW 51511,'Only the seeded DEVELOPMENT_ONLY database without Live data/approval is allowed.',1;
IF EXISTS(SELECT 1 FROM sys.columns WHERE object_id IN (OBJECT_ID('dbo.branches'),OBJECT_ID('dbo.membership_plans'))
 AND name IN ('name','description') AND TYPE_NAME(system_type_id)<>'nvarchar')
 THROW 51512,'Apply reviewed V713 Unicode migration first.',1;
DECLARE @Catalog TABLE(old_label varchar(80),label nvarchar(80),quantity int,unit varchar(10),bonus int,bonus_unit varchar(10),benefit varchar(20),price decimal(15,2));
INSERT @Catalog VALUES
('1 day',N'Gói tập 1 ngày',1,'DAY',0,'DAY','GYM',20000),
('7 days',N'Gói tập 7 ngày',7,'DAY',0,'DAY','GYM',120000),
('2 weeks',N'Gói tập 2 tuần',2,'WEEK',0,'DAY','YOGA',180000),
('1 month',N'Gói tập 1 tháng',1,'MONTH',0,'DAY','ALL',300000),
('3 months',N'Gói tập 3 tháng',3,'MONTH',0,'DAY','PT',900000),
('6 months',N'Gói tập 6 tháng',6,'MONTH',0,'DAY','ALL',1500000),
('1 year',N'Gói tập 1 năm',1,'YEAR',0,'DAY','GYM',2800000),
('2 years',N'Gói tập 2 năm',2,'YEAR',0,'DAY','ALL',5600000),
('3 months + 2 weeks bonus',N'Gói tập 3 tháng - Tặng thêm 2 tuần',3,'MONTH',2,'WEEK','PT',950000),
('6 months + 1 month bonus',N'Gói tập 6 tháng - Tặng thêm 1 tháng',6,'MONTH',1,'MONTH','ALL',1600000),
('1 year + 2 months bonus',N'Gói tập 1 năm - Tặng thêm 2 tháng',1,'YEAR',2,'MONTH','ALL',3200000);
DECLARE @Targets TABLE(plan_id bigint PRIMARY KEY,branch_id bigint,label nvarchar(80),price decimal(15,2));
BEGIN TRY
 BEGIN TRANSACTION;
 DECLARE @lockResult int;
 EXEC @lockResult=sys.sp_getapplock @Resource=N'UTEFit.DemoCatalog.V713',@LockMode='Exclusive',@LockOwner='Transaction',@LockTimeout=10000;
 IF @lockResult<0 THROW 51513,'Cannot acquire catalog lock.',1;
 INSERT @Targets
 SELECT p.id,b.id,c.label,c.price FROM dbo.membership_plans p WITH(UPDLOCK,HOLDLOCK)
 JOIN dbo.membership_plan_branches pb ON pb.plan_id=p.id
 JOIN dbo.branches b ON b.id=pb.branch_id
 CROSS JOIN (VALUES(1),(2),(3),(4),(5)) ordinal(number)
 JOIN @Catalog c ON p.name IN (CONCAT('DEMO B',ordinal.number,' - ',c.old_label),c.label)
 AND p.duration_unit=c.unit AND p.duration_quantity=c.quantity AND p.bonus_unit=c.bonus_unit
 AND p.bonus_quantity=c.bonus AND p.benefit=c.benefit
 WHERE b.name IN (CONCAT('UTEFit Demo Branch ',ordinal.number),CONCAT(N'UTEFit - Chi nhánh ',ordinal.number))
 AND p.access_scope='SINGLE_BRANCH'
 AND (SELECT COUNT(*) FROM dbo.membership_plan_branches existing WHERE existing.plan_id=p.id)=1;
 IF (SELECT COUNT(*) FROM @Targets)<>55
 OR EXISTS(SELECT 1 FROM @Targets GROUP BY branch_id,label HAVING COUNT(*)<>1)
 THROW 51514,'Expected exactly 11 unambiguous seeded plans per branch. Inspect custom edits; never insert replacements.',1;
 SELECT p.id,p.name AS current_name,t.label AS new_name,p.price AS current_price,t.price AS new_price,t.branch_id
 FROM @Targets t JOIN dbo.membership_plans p ON p.id=t.plan_id ORDER BY t.branch_id,p.id;
 IF @Apply=0
 BEGIN
  ROLLBACK TRANSACTION;
  PRINT 'Preview only. No changes. Set @Apply=1 only after review/backup.';
  RETURN;
 END;
 UPDATE p SET name=t.label,price=t.price,description=N'Gói tập demo áp dụng tại một chi nhánh cố định.',updated_at=SYSDATETIME()
 FROM dbo.membership_plans p JOIN @Targets t ON t.plan_id=p.id;
 UPDATE b SET name=CONCAT(N'UTEFit - Chi nhánh ',ordinal.number),updated_at=SYSDATETIME()
 FROM dbo.branches b CROSS JOIN (VALUES(1),(2),(3),(4),(5)) ordinal(number)
 WHERE b.name=CONCAT('UTEFit Demo Branch ',ordinal.number)
 AND EXISTS(SELECT 1 FROM @Targets t WHERE t.branch_id=b.id);
 COMMIT TRANSACTION;
 PRINT 'Catalog updated. Payments, membership snapshots/dates and service configuration unchanged.';
END TRY
BEGIN CATCH
 IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
 THROW;
END CATCH;
