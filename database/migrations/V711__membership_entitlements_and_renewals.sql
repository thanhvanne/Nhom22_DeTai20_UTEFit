SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF OBJECT_ID(N'dbo.sepay_bank_reconciliations', N'U') IS NULL
    THROW 51000, 'Apply V710 before V711.', 1;
IF COL_LENGTH('dbo.memberships', 'quoted_scope') IS NOT NULL
    THROW 51000, 'V711 appears applied. Inspect schema before rerunning.', 1;

ALTER TABLE dbo.membership_plans ADD benefit varchar(20) NULL,
    access_scope varchar(20) NULL;
EXEC(N'ALTER TABLE dbo.membership_plans ADD CONSTRAINT ck_plan_benefit
    CHECK (benefit IS NULL OR benefit IN (''ALL'',''GYM'',''YOGA'',''PT''));');
EXEC(N'ALTER TABLE dbo.membership_plans ADD CONSTRAINT ck_plan_scope
    CHECK (access_scope IS NULL OR access_scope IN (''SINGLE_BRANCH'',''MULTI_BRANCH'',''ALL_BRANCHES''));');
ALTER TABLE dbo.memberships ADD quoted_benefit varchar(20) NULL,
    quoted_scope varchar(20) NULL, renewal_of_id bigint NULL;
EXEC(N'ALTER TABLE dbo.memberships ADD CONSTRAINT fk_membership_renewal
    FOREIGN KEY (renewal_of_id) REFERENCES dbo.memberships(id);');
EXEC(N'ALTER TABLE dbo.memberships ADD CONSTRAINT ck_membership_quoted_benefit
    CHECK (quoted_benefit IS NULL OR quoted_benefit IN (''ALL'',''GYM'',''YOGA'',''PT''));');
EXEC(N'ALTER TABLE dbo.memberships ADD CONSTRAINT ck_membership_quoted_scope
    CHECK (quoted_scope IS NULL OR quoted_scope IN (''SINGLE_BRANCH'',''MULTI_BRANCH'',''ALL_BRANCHES''));');
ALTER TABLE dbo.class_categories ADD required_benefit varchar(20) NULL;
EXEC(N'ALTER TABLE dbo.class_categories ADD CONSTRAINT ck_category_benefit
    CHECK (required_benefit IS NULL OR required_benefit IN (''ALL'',''GYM'',''YOGA'',''PT''));');
CREATE TABLE dbo.membership_plan_branches (
    plan_id bigint NOT NULL REFERENCES dbo.membership_plans(id),
    branch_id bigint NOT NULL REFERENCES dbo.branches(id),
    CONSTRAINT pk_membership_plan_branches PRIMARY KEY (plan_id,branch_id)
);
CREATE TABLE dbo.membership_branch_snapshot (
    membership_id bigint NOT NULL REFERENCES dbo.memberships(id),
    branch_id bigint NOT NULL REFERENCES dbo.branches(id),
    CONSTRAINT pk_membership_branch_snapshot PRIMARY KEY (membership_id,branch_id)
);
EXEC(N'CREATE INDEX ix_membership_access ON dbo.memberships(user_id,status,end_date)
    INCLUDE (start_date,quoted_benefit,quoted_scope);');
COMMIT TRANSACTION;
