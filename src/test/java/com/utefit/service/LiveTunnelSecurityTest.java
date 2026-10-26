package com.utefit.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.ForwardedHeaderFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

class LiveTunnelSecurityTest {
    @Test
    void trustedLoopbackProxyHeadersRestoreHttpsOriginForSecurityAndRedirects() throws Exception {
        var request = new MockHttpServletRequest("POST", "/payments/sepay/webhook/live");
        request.setRemoteAddr("127.0.0.1"); request.setScheme("http"); request.setServerName("localhost"); request.setServerPort(8080);
        request.addHeader("X-Forwarded-Proto", "https");
        request.addHeader("X-Forwarded-Host", "demo-fixture.trycloudflare.com");
        request.addHeader("X-Forwarded-Port", "443");
        new ForwardedHeaderFilter().doFilter(request, new MockHttpServletResponse(), (wrapped, response) -> {
            var http = (HttpServletRequest) wrapped;
            assertTrue(http.isSecure()); assertEquals("https", http.getScheme());
            assertEquals("demo-fixture.trycloudflare.com", http.getServerName()); assertEquals(443, http.getServerPort());
            assertNull(http.getHeader("X-Forwarded-Proto"));
        });
    }

    @Test
    void plainHttpWithoutTrustedProxyDoesNotBecomeSecure() throws Exception {
        var request = new MockHttpServletRequest("POST", "/payments/sepay/webhook/live");
        new ForwardedHeaderFilter().doFilter(request, new MockHttpServletResponse(), (wrapped, response) -> assertFalse(wrapped.isSecure()));
    }

    @Test
    void liveProfileKeepsSecureSessionCookiesAndExactDynamicWebsocketOrigin() throws Exception {
        var properties = new Properties();
        try (var stream = Files.newInputStream(Path.of("src/main/resources/application-sepay-live.properties"))) { properties.load(stream); }
        assertEquals("127.0.0.1", properties.getProperty("server.address"));
        assertEquals("framework", properties.getProperty("server.forward-headers-strategy"));
        assertEquals("true", properties.getProperty("server.servlet.session.cookie.secure"));
        assertEquals("true", properties.getProperty("server.servlet.session.cookie.http-only"));
        assertEquals("lax", properties.getProperty("server.servlet.session.cookie.same-site"));
        assertEquals("${SEPAY_LIVE_PUBLIC_URL:}", properties.getProperty("utefit.websocket.allowed-origins"));
    }
}
