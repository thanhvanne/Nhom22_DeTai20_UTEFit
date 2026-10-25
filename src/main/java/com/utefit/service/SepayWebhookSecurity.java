package com.utefit.service;

import com.utefit.config.SepayProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class SepayWebhookSecurity {
    private final SepayProperties properties;
    private final Clock clock;

    public boolean verify(String signature, String timestamp, byte[] body) {
        if (signature == null || !signature.matches("sha256=[0-9a-fA-F]{64}")
                || timestamp == null || !timestamp.matches("[0-9]{10,11}")) return false;
        long signedAt = Long.parseLong(timestamp);
        long now = clock.instant().getEpochSecond();
        if (signedAt < now - 300 || signedAt > now + 300) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            mac.update((timestamp + ".").getBytes(StandardCharsets.US_ASCII));
            return MessageDigest.isEqual(mac.doFinal(body), HexFormat.of().parseHex(signature.substring(7)));
        } catch (java.security.GeneralSecurityException ex) {
            throw new IllegalStateException("Webhook authentication unavailable", ex);
        }
    }
}
