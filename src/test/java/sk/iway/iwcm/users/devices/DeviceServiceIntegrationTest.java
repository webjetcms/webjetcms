package sk.iway.iwcm.users.devices;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.system.jpa.WebJETPersistenceProvider;
import sk.iway.iwcm.test.BaseWebjetTest;

/**
 * Exercises Spring Data repository writes on the configured test database.
 * The schema is provided by .github/workflows/blank_web_autotest.sql; each test removes its own devices.
 */
class DeviceServiceIntegrationTest extends BaseWebjetTest {
    private static final long NOW = 1_791_187_200_123L;
    private static final long KNOWN_AGE = Duration.ofDays(90).toMillis();
    /** Negative IDs keep test notices separate from real accounts in the shared test database. */
    private static final int USER_ID = -91001;
    private static final int OTHER_USER_ID = -91002;

    /** Generated device IDs isolate users and keep a single current notice through reporting and expiry. */
    @Test
    void recognitionAndNoticesRemainScopedToUser() {
        try (TestDatabase database = new TestDatabase()) {
            DeviceService service = database.service;
            LoginEvent first = record(service, USER_ID, database.hash, NOW);
            long id = Long.parseLong(first.id());
            assertTrue(id > 0, "The database must generate the device ID");
            assertNull(record(service, USER_ID, database.hash, NOW + 100));
            LoginEvent secondUser = record(service, OTHER_USER_ID, database.hash, NOW + 100);
            assertNotEquals(first.id(), secondUser.id());
            assertEquals(2, database.count());
            assertEquals(1, service.findActive(USER_ID, NOW + 100).size());

            assertNull(service.findEvent(OTHER_USER_ID, id));
            assertNull(service.report(OTHER_USER_ID, id, NOW));
            assertNull(service.confirm(OTHER_USER_ID, id, NOW));

            service.confirm(USER_ID, id, NOW + 150);
            assertTrue(service.findActive(USER_ID, NOW + 150).isEmpty());
            LoginEvent reported = service.report(USER_ID, id, NOW + 200);
            assertNull(reported.confirmedAt());
            assertEquals(reported, service.report(USER_ID, id, NOW + 250));
            assertEquals(1, service.findActive(USER_ID, NOW + 250).size());

            LoginEvent renewed = record(service, USER_ID, database.hash, NOW + 300);
            assertEquals(first.id(), renewed.id());
            assertEquals(NOW + 300, renewed.createdAt());
            assertNull(renewed.confirmedAt());
            assertNull(renewed.reportedAt());
            assertEquals(renewed, service.findEvent(USER_ID, id), "Old links must resolve to the device's current notice");
            assertNull(record(service, OTHER_USER_ID, database.hash, NOW + 300));

            LoginEvent expired = record(service, USER_ID, database.hash, NOW + 300 + KNOWN_AGE);
            assertEquals(first.id(), expired.id());
            assertEquals(NOW + 300 + KNOWN_AGE, expired.createdAt());
            assertEquals(2, database.count(), "Re-detection must update the device rather than insert history");
        }
    }

    /** Notice expiration preserves recognition; deleting the device also removes the email detail. */
    @Test
    void noticeExpirationAndDeviceDeletionShareOneRecord() {
        try (TestDatabase database = new TestDatabase()) {
            DeviceService service = database.service;
            LoginEvent first = record(service, USER_ID, database.hash, NOW);
            long id = Long.parseLong(first.id());
            assertEquals(1, service.findActive(USER_ID, NOW + DeviceService.NOTICE_AGE - 1).size());
            assertTrue(service.findActive(USER_ID, NOW + DeviceService.NOTICE_AGE).isEmpty());

            long later = NOW + Duration.ofDays(30).toMillis();
            assertNull(record(service, USER_ID, database.hash, later));
            assertEquals(first, service.findEvent(USER_ID, id), "A normal login must preserve the notice details");
            DeviceEntity device = database.devices.findById(id).orElseThrow();
            assertEquals(Instant.ofEpochMilli(NOW), device.getCreateDate());
            assertEquals(Instant.ofEpochMilli(later), device.getLastSeen());
            database.devices.deleteById(id);
            assertNull(service.findEvent(USER_ID, id));
            assertEquals(0, database.count());

            LoginEvent recreated = record(service, USER_ID, database.hash, later + 1);
            assertNotEquals(first.id(), recreated.id());
            assertEquals(1, database.count());
        }
    }

    /** Independent persistence factories see confirmation, reporting and refreshed notices without cache eviction. */
    @Test
    void independentPersistenceContextsObserveEachOthersChanges() {
        try (TestDatabase database = new TestDatabase()) {
            DeviceService firstNode = database.service;
            DeviceService secondNode = database.newContext().getBean(DeviceService.class);
            LoginEvent first = record(firstNode, USER_ID, database.hash, NOW);
            long id = Long.parseLong(first.id());
            assertNotNull(secondNode.findEvent(USER_ID, id));
            assertNull(record(secondNode, USER_ID, database.hash, NOW + 100));

            firstNode.confirm(USER_ID, id, NOW + 200);
            assertEquals(NOW + 200, secondNode.findEvent(USER_ID, id).confirmedAt());
            assertTrue(secondNode.findActive(USER_ID, NOW + 200).isEmpty());

            secondNode.report(USER_ID, id, NOW + 300);
            LoginEvent reported = firstNode.findEvent(USER_ID, id);
            assertEquals(NOW + 300, reported.reportedAt());
            assertNull(reported.confirmedAt());
            assertEquals(first.id(), record(firstNode, USER_ID, database.hash, NOW + 400).id());
            assertNull(record(secondNode, USER_ID, database.hash, NOW + 500));
            assertEquals(NOW + 400, secondNode.findEvent(USER_ID, id).createdAt());
            assertEquals(1, database.count());
        }
    }

    private static LoginEvent record(DeviceService service, int userId, String hash, long now) {
        return service.recordLogin(userId, hash, now, now - KNOWN_AGE, "Firefox", "131", "Windows 11", "127.0.0.1");
    }

    /** Shares the configured database and removes only rows created with this test's token hash. */
    private static class TestDatabase implements AutoCloseable {
        private final String hash = UUID.randomUUID().toString().replace("-", "").repeat(2);
        private final List<AnnotationConfigApplicationContext> contexts = new ArrayList<>();
        private final SimpleQuery query = new SimpleQuery();
        private final DeviceService service;
        private final DeviceRepository devices;

        TestDatabase() {
            AnnotationConfigApplicationContext context = newContext();
            service = context.getBean(DeviceService.class);
            devices = context.getBean(DeviceRepository.class);
        }

        /** Each factory acts as an independent application node using the configured database pool. */
        AnnotationConfigApplicationContext newContext() {
            AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
            contexts.add(context);
            context.register(RepositoryConfiguration.class);
            context.refresh();
            return context;
        }

        int count() {
            return query.forInt("SELECT COUNT(*) FROM user_login_devices WHERE token_hash=?", hash);
        }

        @Override
        public void close() {
            try {
                query.execute("DELETE FROM user_login_devices WHERE token_hash=?", hash);
            } finally {
                for (AnnotationConfigApplicationContext context : contexts) context.close();
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
        LocalContainerEntityManagerFactoryBean entityManagerFactory() {
            LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
            factory.setPersistenceUnitName("device-fixture-" + UUID.randomUUID());
            factory.setPersistenceProvider(new WebJETPersistenceProvider());
            factory.setDataSource(DBPool.getInstance().getDataSource("iwcm"));
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
        DeviceService deviceService(DeviceRepository devices) {
            return new DeviceService(devices);
        }
    }
}
