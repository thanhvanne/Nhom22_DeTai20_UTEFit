package com.utefit.service;

import com.utefit.config.LoyaltyProperties;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class LoyaltyMigrationContractTest {
    @Test void schemaIsAdditiveAndSeparateFromSeedAndLiveApproval() throws Exception {
        String sql = Files.readString(Path.of("database/migrations/V716__member_loyalty_offers.sql"));
        assertTrue(sql.contains("DB_NAME() <> N'UTEFitDB'"));
        assertTrue(sql.contains("ux_membership_pending_welcome"));
        assertTrue(sql.contains("ck_membership_discount_snapshot"));
        assertFalse(java.util.regex.Pattern.compile("(?im)^\\s*(INSERT|UPDATE|DELETE|DROP|TRUNCATE)\\b").matcher(sql).find());
        assertFalse(sql.contains("sepay_deployment_guard"));
        assertFalse(sql.contains("ddl-auto"));
    }
    @Test void retiredDemoExclusionsScriptCannotWriteOrDeleteAnyData() throws Exception {
        String sql = Files.readString(Path.of("database/operations/UTEFitDB_loyalty_02_demo_exclusions.template.sql"));
        assertTrue(sql.contains("RETIRED: do not run SQL 02"));
        assertTrue(sql.contains("CASH tier spending ignores them"));
        assertFalse(sql.contains("@ApproveReviewedInventory"));
        assertFalse(java.util.regex.Pattern.compile("(?im)^\\s*(INSERT|UPDATE|DELETE|DROP|TRUNCATE|ALTER|CREATE|GRANT|REVOKE)\\b").matcher(sql).find());
    }
    @Test void runtimeTemplateOnlyOffersAnExplicitAdditionalReadGrant() throws Exception {
        String sql = Files.readString(Path.of("database/operations/UTEFitDB_loyalty_03_runtime_permissions.template.sql"));
        assertTrue(sql.contains("@ApproveAdditionalReadPermission bit=0"));
        assertTrue(sql.contains("GRANT SELECT ON OBJECT::dbo.loyalty_payment_exclusions"));
        assertFalse(sql.contains("GRANT INSERT")); assertFalse(sql.contains("GRANT UPDATE"));
        assertFalse(sql.contains("ALTER ROLE")); assertFalse(sql.contains("REVOKE"));
    }
    @Test void configurationFailsClosedOnInvalidThresholdsAndRemainsOffUntilRolloutApproval() {
        var policy = new LoyaltyProperties(); policy.validate(); assertFalse(policy.isEnabled());
        policy.setGoldThreshold(new BigDecimal("100000"));
        assertThrows(IllegalArgumentException.class, policy::validate);
    }

    @Test void dynamicProgramsAreFivePausedRowsWithoutChangingOldDataOrLiveApproval() throws Exception {
        String sql = Files.readString(Path.of("database/migrations/V717__automatic_promotion_management.sql"));
        assertTrue(sql.contains("CREATE TABLE dbo.automatic_promotions"));
        assertTrue(sql.contains("SELECT initial.kind,0,initial.discount_percent"));
        assertTrue(sql.contains("WHERE NOT EXISTS"));
        for (String kind : java.util.List.of("WELCOME", "LOYAL", "VALUE", "SILVER", "GOLD")) assertTrue(sql.contains("('" + kind + "',"));
        assertFalse(java.util.regex.Pattern.compile("(?im)^\\s*(UPDATE|DELETE|DROP|TRUNCATE)\\b").matcher(sql).find());
        assertFalse(sql.contains("sepay_deployment_guard"));
        String permissions = Files.readString(Path.of("database/operations/UTEFitDB_loyalty_04_automatic_program_permissions.template.sql"));
        assertTrue(permissions.contains("@ApproveProgramPermissions bit=0"));
        assertTrue(permissions.contains("GRANT SELECT, UPDATE ON OBJECT::dbo.automatic_promotions"));
        assertFalse(permissions.contains("GRANT INSERT")); assertFalse(permissions.contains("GRANT DELETE"));
        assertFalse(permissions.contains("ALTER ROLE")); assertFalse(permissions.contains("REVOKE"));
    }
}
