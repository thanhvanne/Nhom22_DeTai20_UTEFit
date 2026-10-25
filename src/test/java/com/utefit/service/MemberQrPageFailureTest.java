package com.utefit.service;

import com.utefit.controller.member.MemberQrCheckinController;
import org.junit.jupiter.api.Test;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateProcessingException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MemberQrPageFailureTest {
    private final MemberQrStatusService states = mock(MemberQrStatusService.class);
    private final QrCheckinService tokens = mock(QrCheckinService.class);
    private final MemberQrCheckinController controller = new MemberQrCheckinController(tokens, mock(QrImageService.class), states);
    private final Principal principal = () -> "member@utefit.test";

    @Test
    void previousUnguardedExpressionFailsRenderingWhenStatusModelIsMissing() {
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(new StringTemplateResolver());
        var error = assertThrows(TemplateProcessingException.class, () -> engine.process(
                "<h1>QR Check-in</h1><div th:text=\"${checkinStatus.attendanceState}\"></div><button>Create QR</button>", new Context()));
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        assertTrue(cause.getMessage().contains("attendanceState"));
        assertTrue(cause.getMessage().contains("null"));
    }

    @Test
    void temporaryStatusFailureStillReturnsPageWithVisibleErrorFallback() {
        when(states.status(principal.getName(), null)).thenThrow(new TransientDataAccessResourceException("unit-only"));
        var model = new ExtendedModelMap();
        assertEquals("member/qr-checkin", controller.page(principal, model, new MockHttpServletResponse()));
        assertNotNull(model.get("syncError"));
    }

    @Test
    void statusApiReturns503ForTemporaryDatabaseFailure() {
        when(states.status(principal.getName(), null)).thenThrow(new TransientDataAccessResourceException("unit-only"));
        var error = assertThrows(ResponseStatusException.class,
                () -> controller.status(null, principal, new MockHttpServletResponse()));
        assertEquals(503, error.getStatusCode().value());
    }

    @Test
    void accessDenialIsNotConvertedToSuccessOrUnknownStatus() {
        when(states.status(principal.getName(), null)).thenThrow(new AccessDeniedException("unit-only"));
        assertThrows(AccessDeniedException.class, () -> controller.page(principal, new ExtendedModelMap(), new MockHttpServletResponse()));
        assertThrows(AccessDeniedException.class, () -> controller.status(null, principal, new MockHttpServletResponse()));
    }

    @Test
    void failedIssuanceDoesNotRemovePageControlsOrPretendTokenWasCreated() {
        when(tokens.issue(principal.getName())).thenThrow(new TransientDataAccessResourceException("unit-only"));
        when(states.status(principal.getName(), null)).thenThrow(new TransientDataAccessResourceException("unit-only"));
        var model = new ExtendedModelMap();
        assertEquals("member/qr-checkin", controller.issue(principal, model, new MockHttpServletResponse()));
        assertNotNull(model.get("error"));
        assertNotNull(model.get("syncError"));
        assertNull(model.get("qrImage"));
    }
}
