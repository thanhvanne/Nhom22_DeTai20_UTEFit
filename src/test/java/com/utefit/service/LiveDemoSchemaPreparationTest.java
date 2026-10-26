package com.utefit.service;

import jakarta.persistence.Entity;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import static org.junit.jupiter.api.Assertions.*;

class LiveDemoSchemaPreparationTest {
    public static class NoDatabaseConnections implements ConnectionProvider {
        @Override public Connection getConnection() { throw new AssertionError("Schema export must never connect to a database"); }
        @Override public void closeConnection(Connection connection) { throw new AssertionError("Unexpected database connection"); }
        @Override public boolean supportsAggressiveRelease() { return false; }
        @Override public boolean isUnwrappableAs(Class<?> type) { return type.isInstance(this); }
        @Override public <T> T unwrap(Class<T> type) { return type.cast(this); }
    }

    @Test
    void exportCurrentSqlServerSchemaWithoutConnectingOrGeneratingAnyData() throws Exception {
        Path target = Path.of("target", "live-demo-jpa-schema.sql").toAbsolutePath();
        Files.createDirectories(target.getParent());
        var configuration = new Configuration();
        configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.SQLServerDialect");
        configuration.setProperty("hibernate.boot.allow_jdbc_metadata_access", "false");
        configuration.setProperty("hibernate.connection.provider_class", NoDatabaseConnections.class.getName());
        configuration.setProperty("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl");
        configuration.setProperty("hibernate.hbm2ddl.auto", "none");
        configuration.setProperty("jakarta.persistence.schema-generation.database.action", "none");
        configuration.setProperty("jakarta.persistence.schema-generation.scripts.action", "create");
        configuration.setProperty("jakarta.persistence.schema-generation.scripts.create-target", target.toString());
        configuration.setProperty("hibernate.hbm2ddl.schema-generation.script.append", "false");
        configuration.setProperty("hibernate.hbm2ddl.delimiter", ";");
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        var candidates = scanner.findCandidateComponents("com.utefit.entity");
        assertEquals(25, candidates.size());
        for (var candidate : candidates) configuration.addAnnotatedClass(Class.forName(candidate.getBeanClassName()));
        try (var factory = configuration.buildSessionFactory()) { assertTrue(factory.isOpen()); }
        String ddl = Files.readString(target);
        assertEquals(27, ddl.lines().filter(line -> line.stripLeading().startsWith("create table ")).count());
        assertTrue(ddl.contains("quoted_duration_days"));
        assertTrue(ddl.contains("video_public_id"));
        assertTrue(ddl.contains("uk_sepay_environment_transaction"));
        assertFalse(ddl.toLowerCase().contains("insert into"));
        assertFalse(ddl.toLowerCase().contains("drop database"));
        var excluded = java.util.Set.of("favorites", "manager_branch_assignments", "qr_checkin_tokens", "sepay_payment_details", "sepay_webhook_transactions", "sepay_bank_reconciliations", "membership_plan_branches", "membership_branch_snapshot");
        var tablePattern = java.util.regex.Pattern.compile("^(?:create table|alter table) (\\w+)|^create (?:unique )?index \\w+ on (\\w+)");
        String expectedCore = ddl.lines().filter(line -> !line.isBlank()).filter(line -> {
            var matcher = tablePattern.matcher(line);
            assertTrue(matcher.find(), line);
            return !excluded.contains(matcher.group(1) != null ? matcher.group(1) : matcher.group(2));
        }).filter(line -> !line.contains("foreign key (renewal_of_id)"))
                .map(this::withoutV711Columns).collect(java.util.stream.Collectors.joining("\n"));
        String bootstrap = Files.readString(Path.of("database/live-demo/01_schema.sql")).replace("\r\n", "\n");
        String storedCore = bootstrap.substring(bootstrap.indexOf("-- BEGIN JPA CORE SNAPSHOT") + "-- BEGIN JPA CORE SNAPSHOT".length(),
                bootstrap.indexOf("-- END JPA CORE SNAPSHOT")).trim();
        assertEquals(expectedCore, storedCore, "Reviewed SQLServer core snapshot must match current JPA metadata");
        assertEquals(19, storedCore.lines().filter(line -> line.startsWith("create table ")).count());
        String runner = Files.readString(Path.of("database/live-demo/02_apply_gd7.sql"));
        for (int version = 701; version <= 708; version++) assertTrue(runner.contains("V" + version + "__"));
        assertFalse(Files.readString(Path.of("database/live-demo/00_create_database.sql")).toLowerCase().contains("drop database"));
        String migration = Files.readString(Path.of("database/migrations/V711__membership_entitlements_and_renewals.sql"));
        for (String column : new String[]{"quoted_benefit", "quoted_scope", "renewal_of_id", "required_benefit", "membership_plan_branches", "membership_branch_snapshot"}) {
            assertTrue(ddl.contains(column));
            assertTrue(migration.contains(column));
        }
        assertFalse(java.util.regex.Pattern.compile("(?im)^\\s*(INSERT|UPDATE|DELETE|DROP|TRUNCATE)\\b").matcher(migration).find());
        String calendar = Files.readString(Path.of("database/migrations/V712__membership_calendar_terms.sql"));
        for (String column : new String[]{"duration_unit", "duration_quantity", "bonus_unit", "bonus_quantity",
                "quoted_duration_unit", "quoted_duration_quantity", "quoted_bonus_unit", "quoted_bonus_quantity"}) {
            assertTrue(ddl.contains(column)); assertTrue(calendar.contains(column));
        }
        assertFalse(java.util.regex.Pattern.compile("(?im)^\\s*(INSERT|UPDATE|DELETE|DROP|TRUNCATE)\\b").matcher(calendar).find());
        assertTrue(calendar.contains("ROLLBACK TRANSACTION"));
    }

    private String withoutV711Columns(String line) {
        String result = line.replace("renewal_of_id bigint, ", "");
        for (String column : new String[]{"quoted_duration_quantity", "quoted_bonus_quantity", "duration_quantity", "bonus_quantity"})
            result = result.replace(column + " int, ", "");
        for (String column : new String[]{"quoted_duration_unit", "quoted_bonus_unit", "duration_unit", "bonus_unit"})
            result = result.replace(column + " varchar(10) check ((" + column + " in ('DAY','WEEK','MONTH','YEAR'))), ", "");
        for (String column : new String[]{"quoted_benefit", "required_benefit", "benefit"}) {
            result = result.replace(column + " varchar(20) check ((" + column + " in ('ALL','GYM','YOGA','PT'))), ", "");
        }
        for (String column : new String[]{"quoted_scope", "access_scope"}) {
            result = result.replace(column + " varchar(20) check ((" + column + " in ('SINGLE_BRANCH','MULTI_BRANCH','ALL_BRANCHES'))), ", "");
        }
        return result;
    }
}
