package com.utefit.service;

import com.utefit.config.SepayProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.ByteArrayInputStream;
import java.net.http.*;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class SepayReconciliationLookupTest {
    private SepayReconciliationLookup service;
    private HttpClient client;
    private HttpResponse<java.io.InputStream> response;
    private SepayDatabaseGuard guard;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin-fixture", "unused",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        guard = mock(SepayDatabaseGuard.class);
        service = new SepayReconciliationLookup(mock(SepayProperties.class), guard);
        client = mock(HttpClient.class); response = mock(HttpResponse.class);
        ReflectionTestUtils.setField(service, "client", client);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "token", "unit-only-readonly-api-fixture");
        ReflectionTestUtils.setField(service, "bankAccountId", "00000000-0000-0000-0000-000000000001");
        doReturn(response).when(client).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        when(response.statusCode()).thenReturn(200);
    }

    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    @Test
    void lookupOnlyReadsFixedProviderAndDoesNotExposeUnselectedFields() throws Exception {
        when(response.body()).thenReturn(new ByteArrayInputStream("{\"status\":\"success\",\"data\":[{\"id\":\"uuid-fixture\",\"amount_in\":100000,\"unexpected_secret\":\"not-for-ui\"}]}".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        String result = service.lookup(LocalDate.now(SepayPaymentService.ZONE), 1);
        assertTrue(result.contains("uuid-fixture")); assertFalse(result.contains("not-for-ui"));
        var request = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).send(request.capture(), any(HttpResponse.BodyHandler.class));
        assertEquals("GET", request.getValue().method());
        assertEquals("userapi.sepay.vn", request.getValue().uri().getHost());
        assertTrue(request.getValue().uri().getQuery().contains("per_page=20"));
        verify(guard).requireSafeDatabase();
    }

    @Test
    void memberCannotCallServiceEvenOutsideController() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("member-fixture", "unused",
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))));
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.lookup(LocalDate.now(), 1));
        verifyNoInteractions(client, guard);
    }

    @Test
    void providerErrorsAreRedactedAndNeverUpdatePayments() {
        when(response.statusCode()).thenReturn(401);
        when(response.body()).thenReturn(new ByteArrayInputStream("provider private response".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        var error = assertThrows(IllegalStateException.class, () -> service.lookup(LocalDate.now(SepayPaymentService.ZONE), 1));
        assertFalse(error.getMessage().contains("provider private response"));
        assertTrue(error.getMessage().contains("không có Payment"));
    }

    @Test
    void invalidHeaderCredentialIsRejectedWithoutEchoingItOrCallingProvider() {
        ReflectionTestUtils.setField(service, "token", "unit-private-fixture\r\ninvalid");
        var error = assertThrows(IllegalStateException.class, () -> service.lookup(LocalDate.now(SepayPaymentService.ZONE), 1));
        assertFalse(error.getMessage().contains("unit-private-fixture"));
        verifyNoInteractions(client);
    }
}
