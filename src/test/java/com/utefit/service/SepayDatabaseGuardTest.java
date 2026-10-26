package com.utefit.service;

import com.utefit.config.SepayProperties;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class SepayDatabaseGuardTest {
    private SepayProperties properties;
    private EntityManager entityManager;
    private Connection connection;
    private Query count;
    private Query marker;
    private SepayDatabaseGuard guard;

    @BeforeEach
    void setup() throws Exception {
        properties = mock(SepayProperties.class);
        entityManager = mock(EntityManager.class);
        Session session = mock(Session.class);
        connection = mock(Connection.class);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        doAnswer(invocation -> { invocation.<Work>getArgument(0).execute(connection); return null; }).when(session).doWork(any());
        when(properties.getLiveDatabaseName()).thenReturn("UTEFitLive");
        when(properties.getLiveDeploymentId()).thenReturn("approved-unit-deployment");
        when(connection.getCatalog()).thenReturn("UTEFitLive");
        marker = mock(Query.class); count = mock(Query.class);
        when(entityManager.createNativeQuery(contains("SELECT deployment_id"), eq(String.class))).thenReturn(marker);
        when(entityManager.createNativeQuery(startsWith("SELECT (SELECT COUNT"))).thenReturn(count);
        when(marker.getResultList()).thenReturn(List.of("approved-unit-deployment"));
        when(count.getSingleResult()).thenReturn(0L);
        guard = new SepayDatabaseGuard(properties, entityManager);
    }

    @Test void approvedCatalogMarkerAndNoTestRowsRequired() { assertDoesNotThrow(guard::requireSafeDatabase); }
    @Test void actualCatalogMismatchRejected() throws Exception {
        when(connection.getCatalog()).thenReturn("UTEFitDB");
        assertThrows(IllegalStateException.class, guard::requireSafeDatabase);
    }
    @Test void missingOrWrongMarkerRejected() {
        when(marker.getResultList()).thenReturn(List.of());
        assertThrows(IllegalStateException.class, guard::requireSafeDatabase);
        when(marker.getResultList()).thenReturn(List.of("another-deployment"));
        assertThrows(IllegalStateException.class, guard::requireSafeDatabase);
    }
    @Test void copiedTestDataRejectedInLiveCatalog() {
        when(count.getSingleResult()).thenReturn(1L);
        assertThrows(IllegalStateException.class, guard::requireSafeDatabase);
    }
}
