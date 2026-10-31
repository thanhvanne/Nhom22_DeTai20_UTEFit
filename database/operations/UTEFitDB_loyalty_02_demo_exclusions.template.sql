SET NOCOUNT ON;
IF DB_NAME() <> N'UTEFitDB' THROW 51610, 'Wrong database. Select UTEFitDB.', 1;
PRINT 'RETIRED: do not run SQL 02 for loyalty rollout. Valid PAID CASH, including seeded demo CASH, counts toward member tiers.';
PRINT 'No exclusions are inserted, updated or deleted. Existing exclusion audit records are preserved; CASH tier spending ignores them.';
PRINT 'Use V716 (if not applied), then review SQL 03 runtime permissions. This file makes no database changes.';
