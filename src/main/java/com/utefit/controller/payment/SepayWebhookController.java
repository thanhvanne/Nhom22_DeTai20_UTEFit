package com.utefit.controller.payment;

import com.utefit.config.SepayProperties;
import com.utefit.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SepayWebhookController {
    public static final String TEST_PATH = "/payments/sepay/webhook/test";
    public static final String LIVE_PATH = "/payments/sepay/webhook/live";
    private static final int MAX_BYTES = 32 * 1024;
    private final SepayProperties properties;
    private final SepayWebhookSecurity security;
    private final SepayWebhookService webhooks;

    @PostMapping(value = {TEST_PATH, LIVE_PATH}, consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> receive(HttpServletRequest request) {
        try {
            properties.requireConfigured();
            if (!properties.testMode() && !request.isSecure()) return rejected(400, "HTTPS required");
            String expectedPath = properties.testMode() ? TEST_PATH : LIVE_PATH;
            if (!request.getRequestURI().equals(request.getContextPath() + expectedPath)) return rejected(404, "Wrong environment");
            if (request.getContentLengthLong() > MAX_BYTES) return rejected(413, "Payload too large");
            if (request.getHeader("Content-Encoding") != null && !request.getHeader("Content-Encoding").equalsIgnoreCase("identity")) {
                return rejected(415, "Unsupported content encoding");
            }
            byte[] body = request.getInputStream().readNBytes(MAX_BYTES + 1);
            if (body.length == 0 || body.length > MAX_BYTES) return rejected(413, "Invalid payload size");
            if (!security.verify(singleHeader(request, "X-SePay-Signature"), singleHeader(request, "X-SePay-Timestamp"), body)) {
                return rejected(401, "Invalid webhook authentication");
            }
            SepayWebhookPayload payload = SepayWebhookPayload.parse(body);
            try {
                webhooks.accept(payload);
            } catch (DataIntegrityViolationException collision) {
                if (!webhooks.duplicate(payload)) return rejected(503, "Temporary persistence error");
            }
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException ex) {
            return rejected(400, "Invalid JSON or transaction fields");
        } catch (IllegalStateException | DataAccessException | org.springframework.transaction.TransactionException ex) {
            return rejected(503, "Webhook temporarily unavailable");
        } catch (IOException ex) {
            return rejected(400, "Unable to read payload");
        }
    }

    private String singleHeader(HttpServletRequest request, String name) {
        var values = Collections.list(request.getHeaders(name));
        return values.size() == 1 ? values.get(0) : null;
    }

    private ResponseEntity<Map<String, Object>> rejected(int status, String message) {
        return ResponseEntity.status(status).body(Map.of("success", false, "message", message));
    }
}
