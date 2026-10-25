SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    DECLARE @roleColumn INT = COLUMNPROPERTY(OBJECT_ID('dbo.users'), 'role', 'ColumnId');
    IF @roleColumn IS NULL THROW 50705, 'Missing users.role; migration aborted.', 1;
    DECLARE @name SYSNAME, @definition NVARCHAR(MAX), @normalized NVARCHAR(MAX), @drop NVARCHAR(MAX) = N'';
    DECLARE roleChecks CURSOR LOCAL FAST_FORWARD FOR
        SELECT DISTINCT checks.name, checks.definition
        FROM sys.check_constraints checks
        LEFT JOIN sys.sql_expression_dependencies deps ON deps.referencing_id = checks.object_id
        WHERE checks.parent_object_id = OBJECT_ID('dbo.users')
          AND (checks.parent_column_id = @roleColumn
               OR (deps.referenced_id = OBJECT_ID('dbo.users') AND deps.referenced_minor_id = @roleColumn));
    OPEN roleChecks;
    FETCH NEXT FROM roleChecks INTO @name, @definition;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        SET @normalized = UPPER(@definition);
        SET @normalized = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(@normalized,
            '[',''),']',''),'(',''),')',''),' ',''),CHAR(9),''),CHAR(10),''),CHAR(13),'');
        IF CHARINDEX('ROLE=''ADMIN''', @normalized) = 0 OR CHARINDEX('ROLE=''MEMBER''', @normalized) = 0
            OR CHARINDEX('ROLE=''PT''', @normalized) = 0
            THROW 50706, 'Unknown role constraint; review manually. No changes applied.', 1;
        SET @normalized = REPLACE(REPLACE(REPLACE(REPLACE(@normalized,
            'ROLE=''ADMIN''',''),'ROLE=''MEMBER''',''),'ROLE=''PT''',''),'ROLE=''MANAGER''','');
        SET @normalized = REPLACE(@normalized, 'OR', '');
        IF @normalized <> '' THROW 50707, 'Complex role constraint; review manually. No changes applied.', 1;
        SET @drop = @drop + N'ALTER TABLE dbo.users DROP CONSTRAINT ' + QUOTENAME(@name) + N';';
        FETCH NEXT FROM roleChecks INTO @name, @definition;
    END;
    CLOSE roleChecks;
    DEALLOCATE roleChecks;
    IF @drop <> N'' EXEC sys.sp_executesql @drop;
    ALTER TABLE dbo.users WITH CHECK ADD CONSTRAINT ck_users_role_gd7 CHECK (role IN ('MEMBER','PT','ADMIN','MANAGER'));
    IF OBJECT_ID(N'dbo.manager_branch_assignments', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.manager_branch_assignments (
            id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
            manager_id BIGINT NOT NULL,
            branch_id BIGINT NOT NULL,
            CONSTRAINT fk_manager_assignment_user FOREIGN KEY (manager_id) REFERENCES dbo.users(id),
            CONSTRAINT fk_manager_assignment_branch FOREIGN KEY (branch_id) REFERENCES dbo.branches(id),
            CONSTRAINT uk_manager_branch UNIQUE (manager_id,branch_id)
        );
    END;
    COMMIT;
END TRY
BEGIN CATCH
    IF CURSOR_STATUS('local','roleChecks') >= 0 CLOSE roleChecks;
    IF CURSOR_STATUS('local','roleChecks') >= -1 DEALLOCATE roleChecks;
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
