:On Error exit
USE [UTEFitLiveDemo];
GO
IF DB_NAME() <> N'UTEFitLiveDemo' THROW 50840, 'Wrong database.', 1;
SELECT @@SERVERNAME AS server_name, DB_NAME() AS database_name,
    SERVERPROPERTY('ProductVersion') AS sql_version, DATABASEPROPERTYEX(DB_NAME(),'Collation') AS collation;
SELECT COUNT(*) AS table_count_expected_25 FROM sys.tables WHERE schema_id=SCHEMA_ID('dbo') AND is_ms_shipped=0;
SELECT name FROM sys.tables WHERE schema_id=SCHEMA_ID('dbo') AND is_ms_shipped=0 ORDER BY name;
SELECT id,environment,deployment_id,approved_at FROM dbo.sepay_deployment_guard;
SELECT 'sepay_details' AS source,environment,COUNT_BIG(*) AS rows_count FROM dbo.sepay_payment_details GROUP BY environment
UNION ALL SELECT 'sepay_receipts',environment,COUNT_BIG(*) FROM dbo.sepay_webhook_transactions GROUP BY environment;
SELECT method,status,COUNT_BIG(*) AS row_count,SUM(amount) AS amount_vnd FROM dbo.payments GROUP BY method,status;
SELECT status,COUNT_BIG(*) AS row_count FROM dbo.memberships GROUP BY status;
SELECT OBJECT_NAME(object_id) AS table_name,name,is_unique,has_filter,filter_definition,is_disabled
    FROM sys.indexes WHERE object_id IN (OBJECT_ID('dbo.payments'),OBJECT_ID('dbo.sepay_payment_details'),OBJECT_ID('dbo.sepay_webhook_transactions'))
    AND name IN ('uk_payments_sepay_reference','uk_sepay_payment_reference','uk_sepay_environment_transaction','uk_sepay_bank_reference');
SELECT OBJECT_NAME(indexes.object_id) AS table_name,indexes.name AS index_name,columns.name AS column_name,index_columns.key_ordinal
    FROM sys.indexes indexes JOIN sys.index_columns index_columns ON index_columns.object_id=indexes.object_id AND index_columns.index_id=indexes.index_id
    JOIN sys.columns columns ON columns.object_id=index_columns.object_id AND columns.column_id=index_columns.column_id
    WHERE indexes.name IN ('uk_payments_sepay_reference','uk_sepay_payment_reference','uk_sepay_environment_transaction','uk_sepay_bank_reference')
    ORDER BY table_name,index_name,index_columns.key_ordinal;
SELECT OBJECT_NAME(parent_object_id) AS table_name,name,is_disabled,is_not_trusted,definition FROM sys.check_constraints;
SELECT OBJECT_NAME(parent_object_id) AS table_name,name,is_disabled,is_not_trusted FROM sys.foreign_keys;
SELECT user_id,COUNT(*) AS active_count FROM dbo.memberships
    WHERE status='ACTIVE' AND end_date>=CAST(SYSDATETIME() AS DATE) GROUP BY user_id HAVING COUNT(*)>1;
SELECT environment,transaction_id,outcome,reason,received_at,payment_id FROM dbo.sepay_webhook_transactions
    WHERE outcome='REVIEW' OR conflict_hash IS NOT NULL ORDER BY received_at DESC;
SELECT payment.id,payment.amount,payment.status,details.payment_reference,details.environment,details.expires_at
    FROM dbo.payments payment JOIN dbo.sepay_payment_details details ON details.payment_id=payment.id
    WHERE payment.status='PENDING' ORDER BY details.expires_at;
