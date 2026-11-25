package sk.iway.iwcm.system.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import java.sql.Connection;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.env.MockEnvironment;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.InitServlet;

class WebjetBootstrapModeDetectorTest {

    private final WebjetBootstrapModeDetector modeDetector = new WebjetBootstrapModeDetector();
    private final MockEnvironment environment = new MockEnvironment();

    @Test
    void validDatabaseConfigurationSelectsProduction() throws Exception {
        Connection connection = mock(Connection.class);
        Map<String, String> databaseValues = Map.of("installName", "dynamicinstall");

        try (MockedStatic<DBPool> dbPool = mockStatic(DBPool.class);
                MockedStatic<InitServlet> initServlet = mockStatic(InitServlet.class)) {
            dbPool.when(() -> DBPool.getConnection("iwcm")).thenReturn(connection);
            initServlet.when(() -> InitServlet.getDatabaseValues(connection)).thenReturn(databaseValues);

            WebjetBootstrapModeDetector.Detection detection = modeDetector.detect(environment);

            assertEquals(WebjetBootstrapMode.PRODUCTION, detection.mode());
            assertEquals("dynamicinstall", detection.springConfiguration().installName());
            initServlet.verify(() -> InitServlet.setContextDbName(null));
        }

        verify(connection).close();
    }

    @Test
    void unavailableDatabaseFailsClosed() {
        try (MockedStatic<DBPool> dbPool = mockStatic(DBPool.class);
                MockedStatic<InitServlet> initServlet = mockStatic(InitServlet.class)) {
            dbPool.when(() -> DBPool.getConnection("iwcm")).thenReturn(null);

            WebjetBootstrapUnavailableException exception = assertThrows(
                WebjetBootstrapUnavailableException.class,
                () -> modeDetector.detect(environment)
            );

            assertTrue(exception.getMessage().contains("database connection is unavailable"));
        }
    }

    @Test
    void emptyOrUnreadableConfigurationFailsClosed() throws Exception {
        Connection connection = mock(Connection.class);

        try (MockedStatic<DBPool> dbPool = mockStatic(DBPool.class);
                MockedStatic<InitServlet> initServlet = mockStatic(InitServlet.class)) {
            dbPool.when(() -> DBPool.getConnection("iwcm")).thenReturn(connection);
            initServlet.when(() -> InitServlet.getDatabaseValues(connection)).thenReturn(Map.of());

            WebjetBootstrapUnavailableException exception = assertThrows(
                WebjetBootstrapUnavailableException.class,
                () -> modeDetector.detect(environment)
            );

            assertTrue(exception.getMessage().contains("configuration table is empty or unavailable"));
        }

        verify(connection).close();
    }

    @Test
    void databasePreflightExceptionFailsClosedAndPreservesCause() {
        IllegalStateException failure = new IllegalStateException("connection failure");

        try (MockedStatic<DBPool> dbPool = mockStatic(DBPool.class);
                MockedStatic<InitServlet> initServlet = mockStatic(InitServlet.class)) {
            dbPool.when(() -> DBPool.getConnection("iwcm")).thenThrow(failure);

            WebjetBootstrapUnavailableException exception = assertThrows(
                WebjetBootstrapUnavailableException.class,
                () -> modeDetector.detect(environment)
            );

            assertSame(failure, exception.getCause());
        }
    }
}
