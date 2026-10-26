package com.utefit.service;

import com.utefit.config.SepayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.HexFormat;
import static org.junit.jupiter.api.Assertions.*;

class SepayWebhookSecurityTest {
    private static final String SECRET = "unit-only-not-a-sepay-credential-123456";
    private static final Instant NOW = Instant.parse("2026-10-25T09:00:00Z");
    private SepayWebhookSecurity security;

    @BeforeEach
    void setup() {
        SepayProperties properties = new SepayProperties();
        ReflectionTestUtils.setField(properties, "environment", "test");
        ReflectionTestUtils.setField(properties, "webhookSecret", SECRET);
        security = new SepayWebhookSecurity(properties, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private String sign(String timestamp, byte[] body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        mac.update((timestamp + ".").getBytes(StandardCharsets.US_ASCII));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
    }

    @Test
    void rawBytesIncludingUnicodeAndWhitespaceAreSigned() throws Exception {
        byte[] body = "{ \"content\": \"Nội dung Test\" }".getBytes(StandardCharsets.UTF_8);
        String timestamp = Long.toString(NOW.getEpochSecond());
        assertTrue(security.verify(sign(timestamp, body), timestamp, body));
        assertFalse(security.verify(sign(timestamp, body), timestamp, "{}".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void timestampOutsideWindowOrMissingHeadersAreRejected() throws Exception {
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        for (long offset : new long[]{-301, 301}) {
            String timestamp = Long.toString(NOW.plusSeconds(offset).getEpochSecond());
            assertFalse(security.verify(sign(timestamp, body), timestamp, body));
        }
        assertFalse(security.verify(null, null, body));
    }

    @Test
    void validJsonIsParsedWithoutTrustingUnknownExtraFields() {
        String json = "{\"id\":1,\"gateway\":\"TestBank\",\"accountNumber\":\"TEST0001\",\"code\":null,"
                + "\"content\":\"unit-only\",\"transferType\":\"in\",\"transferAmount\":100000,"
                + "\"referenceCode\":\"TESTREF\",\"transactionDate\":\"2026-10-25 16:00:00\",\"paymentId\":999}";
        var payload = SepayWebhookPayload.parse(json.getBytes(StandardCharsets.UTF_8));
        assertEquals(1, payload.id());
        assertEquals("", payload.code());
        assertEquals("100000", payload.amount().toPlainString());
        assertEquals(64, payload.bodyHash().length());
    }

    @Test
    void duplicateJsonKeysAndTrailingObjectsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> SepayWebhookPayload.parse("{\"id\":1,\"id\":2}".getBytes(StandardCharsets.UTF_8)));
        assertThrows(IllegalArgumentException.class, () -> SepayWebhookPayload.parse("{} {}".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void subAccountIsStrictStringAndChangesFingerprintWithoutBreakingLegacyEmptyFingerprints() {
        String json = "{\"id\":1,\"gateway\":\"BIDV\",\"accountNumber\":\"8820666613\",\"code\":null,"
                + "\"content\":\"isolated-unit\",\"transferType\":\"in\",\"transferAmount\":50000,"
                + "\"referenceCode\":\"UNITREF\",\"transactionDate\":\"2026-10-25 16:00:00\"}";
        var legacy = SepayWebhookPayload.parse(json.getBytes(StandardCharsets.UTF_8));
        var blank = SepayWebhookPayload.parse(json.replace("\"code\":null", "\"code\":null,\"subAccount\":\"\"").getBytes(StandardCharsets.UTF_8));
        var va = SepayWebhookPayload.parse(json.replace("\"code\":null", "\"code\":null,\"subAccount\":\"96247SXBBQ\"").getBytes(StandardCharsets.UTF_8));
        assertEquals(legacy.fingerprint(), blank.fingerprint());
        assertNotEquals(blank.fingerprint(), va.fingerprint());
        assertEquals("8820666613", va.accountNumber()); assertEquals("96247SXBBQ", va.subAccount());
        assertThrows(IllegalArgumentException.class, () -> SepayWebhookPayload.parse(json.replace("\"code\":null", "\"code\":null,\"subAccount\":123").getBytes(StandardCharsets.UTF_8)));
    }
}
