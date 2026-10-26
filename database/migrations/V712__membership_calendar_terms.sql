SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    IF OBJECT_ID(N'dbo.membership_branch_snapshot', N'U') IS NULL
        THROW 51000, 'Apply V711 before V712.', 1;
    IF COL_LENGTH('dbo.membership_plans', 'duration_unit') IS NOT NULL
        THROW 51000, 'V712 appears applied. Inspect schema before rerunning.', 1;

    ALTER TABLE dbo.membership_plans ADD duration_unit varchar(10) NULL,
        duration_quantity int NULL, bonus_unit varchar(10) NULL, bonus_quantity int NULL;
    ALTER TABLE dbo.memberships ADD quoted_duration_unit varchar(10) NULL,
        quoted_duration_quantity int NULL, quoted_bonus_unit varchar(10) NULL, quoted_bonus_quantity int NULL;
    EXEC(N'ALTER TABLE dbo.membership_plans ADD CONSTRAINT ck_plan_calendar_term CHECK (
        (duration_unit IS NULL AND duration_quantity IS NULL AND bonus_unit IS NULL AND bonus_quantity IS NULL)
        OR (duration_unit IS NOT NULL AND duration_quantity IS NOT NULL AND bonus_unit IS NOT NULL AND bonus_quantity IS NOT NULL
        AND duration_unit IN (''DAY'',''WEEK'',''MONTH'',''YEAR'') AND duration_quantity BETWEEN 1 AND 36600
        AND bonus_unit IN (''DAY'',''WEEK'',''MONTH'',''YEAR'') AND bonus_quantity BETWEEN 0 AND 36600));');
    EXEC(N'ALTER TABLE dbo.memberships ADD CONSTRAINT ck_membership_calendar_term CHECK (
        (quoted_duration_unit IS NULL AND quoted_duration_quantity IS NULL AND quoted_bonus_unit IS NULL AND quoted_bonus_quantity IS NULL)
        OR (quoted_duration_unit IS NOT NULL AND quoted_duration_quantity IS NOT NULL AND quoted_bonus_unit IS NOT NULL AND quoted_bonus_quantity IS NOT NULL
        AND quoted_duration_unit IN (''DAY'',''WEEK'',''MONTH'',''YEAR'') AND quoted_duration_quantity BETWEEN 1 AND 36600
        AND quoted_bonus_unit IN (''DAY'',''WEEK'',''MONTH'',''YEAR'') AND quoted_bonus_quantity BETWEEN 0 AND 36600));');
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
