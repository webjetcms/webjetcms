package sk.iway.iwcm.users.devices;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.system.jpa.WebJETPersistenceProvider;
import sk.iway.iwcm.test.BaseWebjetTest;

/**
 * Exercises independent Spring Data repository writes with EclipseLink on MySQL/MariaDB.
 * Each test uses an isolated schema with the existing CI users fixture and the actual migration SQL.
 * Requires explicit opt-in with {@code -DwebjetDeviceIntegration=true} in the test JVM and create/drop
 * database permissions. All table mutations run in a verified, newly created UUID schema; each borrowed
 * connection restores its own original catalog before being returned to the pool.
 * Run with {@code ./gradlew test --tests '*DeviceServiceIntegrationTest' -DwebjetDeviceIntegration=true}.
 */
@EnabledIfSystemProperty(named = "webjetDeviceIntegration", matches = "true")
class DeviceServiceIntegrationTest extends BaseWebjetTest {
    private static final long NOW = 1_791_187_200_123L;
    private static final long KNOWN_AGE = Duration.ofDays(90).toMillis();
    private static final String HASH = "0123456789abcdef".repeat(4);

    /** Recognition is per account and tenant, refreshes until expiry and is invalidated by a report. */
    @Test
    void recognitionExpiryAndReportingRemainScopedToAccountAndTenant() throws Exception {
        try (TestDatabase database = new TestDatabase()) {
            DeviceService repository = database.service;
            LoginEvent first = record(repository, 1, 42, HASH, NOW);
            assertNotNull(first);
            assertNull(record(repository, 1, 42, HASH, NOW + 100));
            assertNotNull(record(repository, 2, 42, HASH, NOW + 100));
            assertNotNull(record(repository, 1, 43, HASH, NOW + 100));
            assertEquals(1, repository.findActive(1, 42, NOW + 100).size());

            assertNull(repository.findEvent(2, 42, first.id(), NOW));
            assertNull(repository.findEvent(1, 43, first.id(), NOW));
            assertNull(repository.report(2, 42, first.id(), NOW));
            assertNull(repository.confirm(1, 43, first.id(), NOW));

            repository.report(1, 42, first.id(), NOW + 200);
            assertEquals(1, repository.findActive(1, 42, NOW + 200).size());
            assertNotNull(record(repository, 1, 42, HASH, NOW + 300));
            repository.report(1, 42, first.id(), NOW + 400);
            assertNull(record(repository, 1, 42, HASH, NOW + 500));
            assertNull(record(repository, 2, 42, HASH, NOW + 500));

            assertNotNull(record(repository, 1, 42, HASH, NOW + 500 + KNOWN_AGE));
            assertNull(record(repository, 1, 42, HASH, NOW + 501 + KNOWN_AGE));
        }
    }

    /** Warning expiry and history retention do not depend on cleanup or the recognition retention setting. */
    @Test
    void deadlinesApplyBeforeCleanupAndHistorySurvivesDevicePruning() throws Exception {
        try (TestDatabase database = new TestDatabase()) {
            DeviceService repository = database.service;
            LoginEvent first = record(repository, 1, 42, HASH, NOW);
            assertEquals(1, repository.findActive(1, 42, NOW + DeviceService.NOTICE_AGE - 1).size());
            assertTrue(repository.findActive(1, 42, NOW + DeviceService.NOTICE_AGE).isEmpty());
            assertNotNull(repository.findEvent(1, 42, first.id(), NOW + KNOWN_AGE - 1));
            assertNull(repository.findEvent(1, 42, first.id(), NOW + KNOWN_AGE));

            long thirtyDaysLater = NOW + Duration.ofDays(30).toMillis();
            repository.cleanup(thirtyDaysLater, NOW);
            assertNotNull(repository.findEvent(1, 42, first.id(), thirtyDaysLater));
            assertNotNull(record(repository, 1, 42, HASH, thirtyDaysLater));
            repository.cleanup(NOW + KNOWN_AGE, NOW);
            assertEquals(1, database.count("user_login_events"));
            assertEquals(1, database.count("user_login_devices"));
        }
    }

    /** A rejected event insert leaves the independently saved browser recognition in place. */
    @Test
    void failedEventInsertLeavesDeviceRecognitionSaved() throws Exception {
        try (TestDatabase database = new TestDatabase()) {
            database.execute("CREATE TRIGGER reject_login_event BEFORE INSERT ON user_login_events "
                + "FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Intentional fixture event failure'");
            assertThrows(IllegalStateException.class, () -> record(database.service, 1, 42, HASH, NOW));
            assertEquals(1, database.count("user_login_devices"));
            assertEquals(0, database.count("user_login_events"));
            database.execute("DROP TRIGGER reject_login_event");
            assertNull(record(database.service, 1, 42, HASH, NOW));
            assertEquals(1, database.count("user_login_devices"));
            assertEquals(0, database.count("user_login_events"));
        }
    }

    /** Independent persistence factories see acknowledgments and revocation without local cache eviction. */
    @Test
    void independentPersistenceContextsObserveEachOthersChanges() throws Exception {
        try (TestDatabase database = new TestDatabase()) {
            DeviceService firstNode = database.service;
            DeviceService secondNode = database.newContext().getBean(DeviceService.class);
            LoginEvent first = record(firstNode, 1, 42, HASH, NOW);
            assertNotNull(secondNode.findEvent(1, 42, first.id(), NOW));
            assertNull(record(secondNode, 1, 42, HASH, NOW + 100));

            firstNode.confirm(1, 42, first.id(), NOW + 200);
            assertEquals(NOW + 200, secondNode.findEvent(1, 42, first.id(), NOW + 200).confirmedAt());
            assertTrue(secondNode.findActive(1, 42, NOW + 200).isEmpty());

            secondNode.report(1, 42, first.id(), NOW + 300);
            LoginEvent reported = firstNode.findEvent(1, 42, first.id(), NOW + 300);
            assertEquals(NOW + 300, reported.reportedAt());
            assertNull(reported.confirmedAt());
            assertNotNull(record(firstNode, 1, 42, HASH, NOW + 400));
            assertNull(record(secondNode, 1, 42, HASH, NOW + 500));
        }
    }

    private static LoginEvent record(DeviceService repository, int userId, int domainId, String hash, long now) {
        return repository.recordLogin(userId, domainId, hash, now, now - KNOWN_AGE, "Firefox", "131", "Windows 11", "127.0.0.1");
    }

    /** Owns only an isolated database whose name is generated in this test process. */
    private static class TestDatabase implements AutoCloseable {
        private final Connection connection;
        private final String originalCatalog;
        private final String catalog = "login_device_test_" + UUID.randomUUID().toString().replace("-", "");
        private final List<AnnotationConfigApplicationContext> contexts = new ArrayList<>();
        private DeviceService service;
        private boolean created;

        TestDatabase() throws Exception {
            assertEquals("true", System.getProperty("webjetDeviceIntegration"), "Database integration must be explicitly enabled");
            assertTrue(catalog.matches("login_device_test_[a-f0-9]{32}"), "Only a generated test schema may be created");
            connection = DBPool.getConnection();
            assertNotNull(connection, "The configured test database must be available");
            originalCatalog = connection.getCatalog();
            try {
                String product = connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
                assumeTrue(product.contains("mariadb") || product.contains("mysql"), "The isolated database fixture requires MySQL or MariaDB");
                String fixtures = Files.readString(Path.of(".github/workflows/blank_web_autotest.sql"));
                var users = Pattern.compile("(?ms)^CREATE TABLE `users` \\(.*?^\\) ENGINE=[^;]+;").matcher(fixtures);
                assertTrue(users.find(), "The existing CI users fixture must be available");
                DocumentBuilderFactory xml = DocumentBuilderFactory.newInstance();
                xml.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                var migration = xml.newDocumentBuilder().parse(Path.of("src/main/webapp/WEB-INF/sql/autoupdate-webjet9.xml").toFile());
                String ddl = XPathFactory.newInstance().newXPath().evaluate(
                    "//object[void[@property='desc']/string='user_login_devices - remember authenticated administration browsers']/void[@property='mysql']/string", migration);
                assertFalse(ddl.isBlank(), "The device migration must contain MySQL SQL");
                try (Statement statement = connection.createStatement()) {
                    statement.execute("CREATE DATABASE " + catalog);
                    created = true;
                }
                connection.setCatalog(catalog);
                requireIsolatedCatalog(connection);
                try (Statement statement = connection.createStatement()) {
                    statement.execute(users.group());
                    for (String sql : ddl.split(";")) if (!sql.isBlank()) statement.execute(sql.trim());
                    statement.execute("INSERT INTO users (user_id, login, password) VALUES (1, 'device-test-one', 'disabled'), (2, 'device-test-two', 'disabled')");
                }
                service = newContext().getBean(DeviceService.class);
            } catch (Exception | AssertionError exception) {
                close();
                throw exception;
            }
        }

        /** Each factory acts as an independent application node using the same isolated schema. */
        AnnotationConfigApplicationContext newContext() {
            AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
            contexts.add(context);
            context.registerBean("fixtureDataSource", DataSource.class, () -> new AbstractDataSource() {
                @Override
                public Connection getConnection() throws SQLException {
                    return openIsolatedConnection();
                }

                @Override
                public Connection getConnection(String username, String password) throws SQLException {
                    return openIsolatedConnection();
                }
            });
            context.register(RepositoryConfiguration.class);
            context.refresh();
            return context;
        }

        /** Test-only DDL is always executed after verifying the generated schema is selected. */
        void execute(String sql) throws SQLException {
            requireIsolatedCatalog(connection);
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
        }

        int count(String table) throws SQLException {
            assertTrue(List.of("user_login_devices", "user_login_events").contains(table));
            requireIsolatedCatalog(connection);
            try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
                assertTrue(rows.next());
                return rows.getInt(1);
            }
        }

        /** Verifies both JDBC and server state before any table statements are constructed. */
        private void requireIsolatedCatalog(Connection opened) throws SQLException {
            assertTrue(created, "The fixture must own a successfully created schema before using it");
            assertEquals(catalog, opened.getCatalog(), "The connection must select the generated schema");
            try (Statement statement = opened.createStatement(); ResultSet rows = statement.executeQuery("SELECT DATABASE()")) {
                assertTrue(rows.next());
                assertEquals(catalog, rows.getString(1), "The server must select the generated schema before table mutations");
            }
        }

        /** Restores the catalog captured from this particular borrowed connection when it is closed. */
        private Connection openIsolatedConnection() throws SQLException {
            Connection opened = DBPool.getConnection();
            assertNotNull(opened, "The configured test database must remain available");
            String borrowedCatalog = opened.getCatalog();
            try {
                opened.setCatalog(catalog);
                requireIsolatedCatalog(opened);
                AtomicBoolean closed = new AtomicBoolean();
                return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[] { Connection.class },
                    (proxy, method, args) -> {
                        if (method.getName().equals("close")) {
                            if (closed.compareAndSet(false, true)) restoreAndClose(opened, borrowedCatalog);
                            return null;
                        }
                        if (method.getName().equals("prepareStatement") || method.getName().equals("createStatement")) {
                            assertEquals(catalog, opened.getCatalog(), "Repository statements must remain in the fixture schema");
                        }
                        try {
                            return method.invoke(opened, args);
                        } catch (InvocationTargetException exception) {
                            throw exception.getCause();
                        }
                    });
            } catch (SQLException | RuntimeException | AssertionError exception) {
                restoreAndClose(opened, borrowedCatalog);
                throw exception;
            }
        }

        /** A failed restoration aborts the physical connection instead of returning a changed catalog to the pool. */
        private static void restoreAndClose(Connection opened, String previousCatalog) throws SQLException {
            try {
                opened.setCatalog(previousCatalog);
                assertEquals(previousCatalog, opened.getCatalog(), "The original catalog must be restored before closing");
            } catch (SQLException | RuntimeException | AssertionError exception) {
                opened.abort(Runnable::run);
                throw exception;
            } finally {
                opened.close();
            }
        }

        @Override
        public void close() throws SQLException {
            try {
                for (AnnotationConfigApplicationContext context : contexts) context.close();
                connection.setAutoCommit(true);
                if (created) {
                    assertTrue(catalog.matches("login_device_test_[a-f0-9]{32}"), "Only the generated test schema may be dropped");
                    requireIsolatedCatalog(connection);
                    try (Statement statement = connection.createStatement()) {
                        statement.execute("DROP DATABASE " + catalog);
                    }
                }
            } finally {
                restoreAndClose(connection, originalCatalog);
            }
        }
    }

    /** Uses the same provider and transaction manager wiring as production, without scanning other modules. */
    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = DeviceRepository.class,
        entityManagerFactoryRef = "webjet2022EntityManager", transactionManagerRef = "webjet2022TransactionManager")
    static class RepositoryConfiguration {
        @Bean("webjet2022EntityManager")
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource fixtureDataSource) {
            LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
            factory.setPersistenceUnitName("device-fixture-" + UUID.randomUUID());
            factory.setPersistenceProvider(new WebJETPersistenceProvider());
            factory.setDataSource(fixtureDataSource);
            factory.setJpaVendorAdapter(new EclipseLinkJpaVendorAdapter());
            factory.setPackagesToScan(DeviceEntity.class.getPackageName());
            factory.setJpaPropertyMap(java.util.Map.of(
                "eclipselink.weaving", "false",
                "eclipselink.target-database", "MySQL",
                "eclipselink.ddl-generation", "none",
                "eclipselink.logging.level", "OFF"));
            return factory;
        }

        @Bean("webjet2022TransactionManager")
        PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
            return new JpaTransactionManager(entityManagerFactory);
        }

        @Bean
        DeviceService deviceService(DeviceRepository devices, LoginEventRepository events) {
            return new DeviceService(devices, events);
        }
    }
}
