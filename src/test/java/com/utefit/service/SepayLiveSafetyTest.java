package com.utefit.service;

import com.utefit.config.SepayProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SepayLiveSafetyTest {
    private SepayProperties live() {
        SepayProperties properties = new SepayProperties();
        Map<String, Object> fields = Map.ofEntries(
                Map.entry("enabled", true), Map.entry("environment", "live"), Map.entry("liveEnabled", true),
                Map.entry("webhookAuth", "hmac"), Map.entry("webhookSecret", "test-only-distinct-secret-123456789012"),
                Map.entry("liveWebhookSecret", "live-fixture-only-not-a-real-secret-12345"),
                Map.entry("bankCode", "TestBank"), Map.entry("gateway", "TestBank"), Map.entry("accountNumber", "TEST0001"),
                Map.entry("accountName", "QA"), Map.entry("liveBankCode", "UnitBank"), Map.entry("liveGateway", "UnitBank"),
                Map.entry("liveAccountNumber", "UNIT0001"), Map.entry("liveAccountName", "Unit fixture"),
                Map.entry("paymentPrefix", "UTE"), Map.entry("paymentMinutes", 15), Map.entry("testDatabaseName", "UTEFitQA"),
                Map.entry("liveDatabaseName", "UTEFitLive"), Map.entry("databaseUrl", "jdbc:sqlserver://localhost;databaseName=UTEFitLive;"),
                Map.entry("liveDeploymentId", "unit-deployment-12345678"), Map.entry("liveApproval", "I_APPROVE_SEPAY_LIVE"),
                Map.entry("livePublicUrl", "https://gym.example.invalid"));
        fields.forEach((key, value) -> ReflectionTestUtils.setField(properties, key, value));
        var environment = new MockEnvironment().withProperty("spring.jpa.hibernate.ddl-auto", "validate")
                .withProperty("spring.sql.init.mode", "never").withProperty("utefit.jwt.cookie-secure", "true");
        environment.setActiveProfiles("sepay", "sepay-live");
        ReflectionTestUtils.setField(properties, "springEnvironment", environment);
        return properties;
    }

    @Test
    void validLiveConfigurationUsesOnlyDedicatedBankAndSecret() {
        var properties = live();
        assertDoesNotThrow(properties::requireConfigured);
        assertEquals("UNIT0001", properties.getAccountNumber());
        assertNotEquals(properties.getWebhookSecret(), properties.secret());
    }

    @Test
    void laptopLiveDemoAndQuickTunnelPassExistingGuardsWithoutWeakeningThem() {
        var properties = live();
        ReflectionTestUtils.setField(properties, "liveDatabaseName", "UTEFitLiveDemo");
        ReflectionTestUtils.setField(properties, "databaseUrl", "jdbc:sqlserver://localhost:1433;databaseName=UTEFitLiveDemo;encrypt=true;");
        ReflectionTestUtils.setField(properties, "livePublicUrl", "https://demo-fixture.trycloudflare.com");
        assertDoesNotThrow(properties::requireConfigured);
        assertFalse(properties.acceptingNewPayments());
    }

    @Test
    void utefitDbCanBeConfiguredWithoutEnablingNewPaymentsOrBypassingApproval() {
        var properties = live();
        ReflectionTestUtils.setField(properties, "liveDatabaseName", "UTEFitDB");
        ReflectionTestUtils.setField(properties, "databaseUrl", "jdbc:sqlserver://localhost:1433;databaseName=UTEFitDB;encrypt=true;");
        assertDoesNotThrow(properties::requireConfigured);
        assertFalse(properties.acceptingNewPayments());
        ReflectionTestUtils.setField(properties, "liveApproval", "");
        assertThrows(IllegalStateException.class, properties::requireConfigured);
    }

    @Test
    void liveBidvRequiresSeparateValidVaAndNeverFallsBackToMainAccount() {
        var properties = live();
        ReflectionTestUtils.setField(properties, "liveBankCode", "BIDV");
        ReflectionTestUtils.setField(properties, "liveGateway", "BIDV");
        for (String invalid : new String[]{"", "YOUR_VA", "UNIT0001", "bad account", "A".repeat(51)}) {
            ReflectionTestUtils.setField(properties, "liveVaNumber", invalid);
            assertThrows(IllegalStateException.class, properties::requireConfigured);
        }
        ReflectionTestUtils.setField(properties, "liveVaNumber", "VA_UNIT123");
        assertThrows(IllegalStateException.class, properties::requireConfigured);
        ReflectionTestUtils.setField(properties, "liveVaNumber", "VAUNIT123");
        assertDoesNotThrow(properties::requireConfigured);
        assertEquals("UNIT0001", properties.getAccountNumber());
        assertEquals("VAUNIT123", properties.getVaNumber());
        ReflectionTestUtils.setField(properties, "environment", "test");
        assertEquals("", properties.getVaNumber());
    }

    @Test
    void missingLiveGatesFailClosed() {
        Map<String, Object> invalid = Map.of("liveEnabled", false, "liveApproval", "", "liveDatabaseName", "UTEFitQA",
                "liveDeploymentId", "", "livePublicUrl", "http://gym.example.invalid", "liveAccountNumber", "",
                "databaseUrl", "jdbc:sqlserver://localhost;databaseName=UTEFitDB;", "liveWebhookSecret", "test-only-distinct-secret-123456789012");
        invalid.forEach((field, value) -> {
            var properties = live(); ReflectionTestUtils.setField(properties, field, value);
            assertThrows(IllegalStateException.class, properties::requireConfigured, field);
        });
    }

    @Test
    void wrongProfileUnsafeDdlAndCookieRejected() {
        for (String profile : new String[]{"qa", "test", "sepay"}) {
            var properties = live();
            var environment = (MockEnvironment) ReflectionTestUtils.getField(properties, "springEnvironment");
            environment.setActiveProfiles("sepay", profile);
            assertThrows(IllegalStateException.class, properties::requireConfigured);
        }
        for (var invalid : Map.of("spring.jpa.hibernate.ddl-auto", "update", "spring.sql.init.mode", "always", "utefit.jwt.cookie-secure", "false").entrySet()) {
            var properties = live();
            ((MockEnvironment) ReflectionTestUtils.getField(properties, "springEnvironment")).setProperty(invalid.getKey(), invalid.getValue());
            assertThrows(IllegalStateException.class, properties::requireConfigured);
        }
    }

    @Test
    void oldTestConfigurationStillWorksButCannotPointAtLiveCatalog() {
        var properties = live();
        ReflectionTestUtils.setField(properties, "environment", "test");
        ReflectionTestUtils.setField(properties, "databaseUrl", "jdbc:sqlserver://localhost;databaseName=UTEFitQA;");
        assertDoesNotThrow(properties::requireConfigured);
        assertEquals("TEST0001", properties.getAccountNumber());
        ReflectionTestUtils.setField(properties, "databaseUrl", "jdbc:sqlserver://localhost;databaseName=UTEFitLive;");
        assertThrows(IllegalStateException.class, properties::requireConfigured);
    }

    @Test
    void apiLookupDisabledAndTestModeCannotMakeNetworkRequest() {
        var authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("admin-fixture", "unused",
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
        SepayProperties properties = mock(SepayProperties.class);
        SepayDatabaseGuard guard = mock(SepayDatabaseGuard.class);
        var lookup = new SepayReconciliationLookup(properties, guard);
        assertThrows(IllegalStateException.class, () -> lookup.lookup(java.time.LocalDate.now(), 1));
        ReflectionTestUtils.setField(lookup, "enabled", true);
        when(properties.testMode()).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> lookup.lookup(java.time.LocalDate.now(), 1));
        } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    }
}
