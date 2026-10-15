package sk.iway.iwcm.users.devices;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.AdditionalAnswers.delegatesTo;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.domain.DeleteSpecification;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.JsonTools;
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
    private MockedStatic<Adminlog> audit;

    @BeforeEach
    void isolateAuditWrites() {
        audit = mockStatic(Adminlog.class);
    }

    @AfterEach
    void releaseAudit() {
        audit.close();
    }

    /** Generated device IDs isolate users and keep a single current notice through reporting and expiry. */
    @Test
    void recognitionAndNoticesRemainScopedToUser() throws Exception {
        try (TestDatabase database = new TestDatabase()) {
            DeviceService service = database.service;
            DeviceEntity first = record(service, USER_ID, database.hash, NOW);
            long id = first.getId();
            assertTrue(id > 0, "The database must generate the device ID");
            assertNull(record(service, USER_ID, database.hash, NOW + 100));
            DeviceEntity secondUser = record(service, OTHER_USER_ID, database.hash, NOW + 100);
            assertNotEquals(first.getId(), secondUser.getId());
            assertEquals(id, service.findByTokenHash(USER_ID, database.hash).getId());
            assertEquals(List.of(id), service.findByIds(USER_ID, Set.of(id, secondUser.getId())).stream().map(DeviceEntity::getId).toList());
            assertEquals(2, database.count());
            assertEquals(1, service.findActive(USER_ID, NOW + 100).size());

            assertNull(service.findEvent(OTHER_USER_ID, id));
            assertNull(service.report(OTHER_USER_ID, id, NOW));
            assertNull(service.confirm(OTHER_USER_ID, id, database.hash, NOW, false));

            service.issueConfirmation(USER_ID, id, database.hash, NOW, false);
            service.confirm(USER_ID, id, database.hash, NOW + 150, false);
            assertNotNull(service.findByIds(USER_ID, Set.of(id)).get(0).getConfirmedAt());
            assertTrue(service.findActive(USER_ID, NOW + 150).isEmpty());
            DeviceEntity reported = service.report(USER_ID, id, NOW + 200);
            assertNull(reported.getConfirmedAt());
            assertEquals(JsonTools.objectToJSON(reported), JsonTools.objectToJSON(service.report(USER_ID, id, NOW + 250)));
            assertTrue(service.findActive(USER_ID, NOW + 250).isEmpty(), "Blocking resolves the notice without deleting the device");

            assertThrows(IllegalStateException.class, () -> record(service, USER_ID, database.hash, NOW + 300));
            database.cleanupOwnDevices(NOW + KNOWN_AGE);
            assertNotNull(service.findEvent(USER_ID, id), "Cleanup must retain blocked devices");
            assertNull(service.findEvent(OTHER_USER_ID, secondUser.getId()), "Cleanup must delete expired unblocked devices");
            assertTrue(service.issueUnblockCode(USER_ID, id, database.hash, NOW + 300));
            assertNotNull(service.unblock(USER_ID, id, database.hash, NOW + 300));
            assertNull(record(service, USER_ID, database.hash, NOW + 301));
            assertNull(service.findEvent(USER_ID, id).getReportedAt());
            assertNotNull(service.findEvent(USER_ID, id).getConfirmedAt());

            DeviceEntity expired = record(service, USER_ID, database.hash, NOW + 301 + KNOWN_AGE);
            assertEquals(first.getId(), expired.getId());
            assertEquals(NOW + 301 + KNOWN_AGE, expired.getCreateDate().toEpochMilli());
            assertEquals(1, database.count(), "Re-detection must update the device rather than insert history");
        }
    }

    /** Notice expiration preserves recognition; deleting the device also removes the email detail. */
    @Test
    void noticeExpirationAndDeviceDeletionShareOneRecord() throws Exception {
        try (TestDatabase database = new TestDatabase()) {
            DeviceService service = database.service;
            DeviceEntity first = record(service, USER_ID, database.hash, NOW);
            long id = first.getId();
            assertEquals(1, service.findActive(USER_ID, NOW + DeviceService.NOTICE_AGE - 1).size());
            assertTrue(service.findActive(USER_ID, NOW + DeviceService.NOTICE_AGE).isEmpty());

            long later = NOW + Duration.ofDays(30).toMillis();
            assertNull(record(service, USER_ID, database.hash, later));
            first.setLastSeen(Instant.ofEpochMilli(later));
            assertEquals(JsonTools.objectToJSON(first), JsonTools.objectToJSON(service.findEvent(USER_ID, id)),
                "A normal login must preserve the notice details");
            DeviceEntity device = database.devices.findById(id).orElseThrow();
            assertEquals(Instant.ofEpochMilli(NOW), device.getCreateDate());
            assertEquals(Instant.ofEpochMilli(later), device.getLastSeen());
            database.devices.deleteById(id);
            assertNull(service.findEvent(USER_ID, id));
            assertEquals(0, database.count());

            DeviceEntity recreated = record(service, USER_ID, database.hash, later + 1);
            assertNotEquals(first.getId(), recreated.getId());
            assertEquals(1, database.count());
        }
    }

    /** Independent persistence factories see confirmation, reporting and refreshed notices without cache eviction. */
    @Test
    void independentPersistenceContextsObserveEachOthersChanges() {
        try (TestDatabase database = new TestDatabase()) {
            DeviceService firstNode = database.service;
            DeviceService secondNode = database.newContext().getBean(DeviceService.class);
            DeviceEntity first = record(firstNode, USER_ID, database.hash, NOW);
            long id = first.getId();
            assertNotNull(secondNode.findEvent(USER_ID, id));
            assertNull(record(secondNode, USER_ID, database.hash, NOW + 100));

            firstNode.issueConfirmation(USER_ID, id, database.hash, NOW, false);
            firstNode.confirm(USER_ID, id, database.hash, NOW + 200, false);
            assertEquals(Instant.ofEpochMilli(NOW + 200), secondNode.findEvent(USER_ID, id).getConfirmedAt());
            assertTrue(secondNode.findActive(USER_ID, NOW + 200).isEmpty());

            secondNode.report(USER_ID, id, NOW + 300);
            DeviceEntity reported = firstNode.findEvent(USER_ID, id);
            assertEquals(Instant.ofEpochMilli(NOW + 300), reported.getReportedAt());
            assertNull(reported.getConfirmedAt());
            assertTrue(firstNode.findActive(USER_ID, NOW + 300).isEmpty(), "Other nodes must also omit blocked-device notices");
            assertThrows(IllegalStateException.class, () -> record(firstNode, USER_ID, database.hash, NOW + 400));
            secondNode.issueUnblockCode(USER_ID, id, database.hash, NOW + 400);
            assertNotNull(firstNode.unblock(USER_ID, id, database.hash, NOW + 450));
            assertNull(record(secondNode, USER_ID, database.hash, NOW + 500));
            assertNull(firstNode.findEvent(USER_ID, id).getReportedAt());
            assertEquals(NOW + 450, secondNode.findEvent(USER_ID, id).getConfirmedAt().toEpochMilli());
            assertEquals(1, database.count());
        }
    }

    /** Confirmation and unblock codes allow a correct fifth attempt and reject consumed proofs. */
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void codeConfirmationAcceptsFifthAttemptAndRejectsConsumedProofs(boolean unblock) {
        try (TestDatabase database = new TestDatabase()) {
            DeviceService service = database.service;
            long id = record(service, USER_ID, database.hash, NOW).getId();
            if (unblock) {
                service.report(USER_ID, id, NOW);
                assertTrue(service.issueUnblockCode(USER_ID, id, database.hash, NOW));
            } else assertTrue(service.issueConfirmation(USER_ID, id, database.hash, NOW, true));
            for (int attempt = 1; attempt < DeviceService.CODE_ATTEMPTS; attempt++) {
                assertNull(unblock ? service.unblock(USER_ID, id, "wrong", NOW + 1)
                    : service.confirm(USER_ID, id, "wrong", NOW + 1, true));
                assertEquals(attempt, service.findEvent(USER_ID, id).getCodeAttempts());
            }
            assertNotNull(unblock ? service.unblock(USER_ID, id, database.hash, NOW + 2)
                : service.confirm(USER_ID, id, database.hash, NOW + 2, true));
            assertEquals(0, service.findEvent(USER_ID, id).getCodeAttempts());
            assertNull(unblock ? service.unblock(USER_ID, id, database.hash, NOW + 3)
                : service.confirm(USER_ID, id, database.hash, NOW + 3, true));
        }
    }

    /** Attempt claims reject foreign accounts, replaced or expired codes and incompatible device states. */
    @Test
    void attemptClaimRequiresCurrentOwnedCodeAndDeviceState() {
        try (TestDatabase database = new TestDatabase()) {
            DeviceService service = database.service;
            DeviceRepository repository = database.devices;
            long id = record(service, USER_ID, database.hash, NOW).getId();
            assertTrue(service.issueConfirmation(USER_ID, id, database.hash, NOW, true));
            Instant expires = service.findEvent(USER_ID, id).getCodeExpires();
            Instant now = Instant.ofEpochMilli(NOW);
            int limit = DeviceService.CODE_ATTEMPTS;

            assertEquals(0, repository.claimCodeAttempt(OTHER_USER_ID, id, database.hash, expires, now, false, limit));
            assertEquals(0, repository.claimCodeAttempt(USER_ID, id, "wrong", expires, now, false, limit));
            assertEquals(0, repository.claimCodeAttempt(USER_ID, id, database.hash, expires.plusMillis(1), now, false, limit));
            assertEquals(0, repository.claimCodeAttempt(USER_ID, id, database.hash, expires, expires, false, limit));
            assertEquals(0, repository.claimCodeAttempt(USER_ID, id, database.hash, expires, now, true, limit));
            assertEquals(0, service.findEvent(USER_ID, id).getCodeAttempts());
            assertEquals(1, repository.claimCodeAttempt(USER_ID, id, database.hash, expires, now, false, limit));

            assertTrue(service.issueConfirmation(USER_ID, id, database.hash, NOW + DeviceService.CODE_RESEND_DELAY, true));
            assertEquals(0, repository.claimCodeAttempt(USER_ID, id, database.hash, expires, now, false, limit),
                "An old request must not count against a reissued code with the same hash");
            expires = service.findEvent(USER_ID, id).getCodeExpires();
            service.report(USER_ID, id, NOW + 1);
            assertEquals(0, repository.claimCodeAttempt(USER_ID, id, database.hash, expires, now, false, limit));
            assertTrue(service.issueUnblockCode(USER_ID, id, database.hash, NOW + 2));
            expires = service.findEvent(USER_ID, id).getCodeExpires();
            assertEquals(0, repository.claimCodeAttempt(USER_ID, id, database.hash, expires, now, false, limit));
            assertEquals(1, repository.claimCodeAttempt(USER_ID, id, database.hash, expires, now, true, limit));
            assertNotNull(service.unblock(USER_ID, id, database.hash, NOW + 3));
            assertEquals(0, repository.claimCodeAttempt(USER_ID, id, database.hash, expires, now, true, limit));
        }
    }

    /** Concurrent guesses share the five-attempt limit for confirmation and blocked-device login. */
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void concurrentCodeGuessesCannotLoseAttempts(boolean unblock) throws Exception {
        try (TestDatabase database = new TestDatabase()) {
            DeviceService service = database.service;
            long id = record(service, USER_ID, database.hash, NOW).getId();
            if (unblock) {
                service.report(USER_ID, id, NOW);
                assertTrue(service.issueUnblockCode(USER_ID, id, database.hash, NOW));
            } else assertTrue(service.issueConfirmation(USER_ID, id, database.hash, NOW, true));

            int requests = 12;
            CountDownLatch loaded = new CountDownLatch(requests);
            DeviceRepository concurrentRepository = mock(DeviceRepository.class, delegatesTo(database.devices));
            doAnswer(invocation -> {
                var snapshot = database.devices.findByUserIdAndId(USER_ID, id);
                loaded.countDown();
                assertTrue(loaded.await(10, TimeUnit.SECONDS), "Every request must read before verification begins");
                return snapshot;
            }).when(concurrentRepository).findByUserIdAndId(USER_ID, id);
            DeviceService concurrentService = new DeviceService(concurrentRepository);
            var executor = Executors.newFixedThreadPool(requests);
            try {
                List<Future<DeviceEntity>> results = new ArrayList<>();
                for (int attempt = 0; attempt < requests; attempt++) {
                    results.add(executor.submit(() -> unblock
                        ? concurrentService.unblock(USER_ID, id, "wrong", NOW + 1)
                        : concurrentService.confirm(USER_ID, id, "wrong", NOW + 1, true)));
                }
                for (Future<DeviceEntity> result : results) assertNull(result.get(15, TimeUnit.SECONDS));
            } finally {
                executor.shutdownNow();
                assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "Verification requests must finish before cleanup");
            }

            assertEquals(DeviceService.CODE_ATTEMPTS, service.findEvent(USER_ID, id).getCodeAttempts());
            assertNull(unblock ? service.unblock(USER_ID, id, database.hash, NOW + 2)
                : service.confirm(USER_ID, id, database.hash, NOW + 2, true));
            verify(concurrentRepository, never()).save(any(DeviceEntity.class));
        }
    }

    /** Retained blocked and confirmed devices remain visible and paged within their owning account. */
    @Test
    void deviceListingIncludesAllStatesWithoutOtherAccounts() {
        try (TestDatabase database = new TestDatabase()) {
            DeviceService service = database.service;
            var other = record(service, OTHER_USER_ID, database.hash, NOW + 1000);
            var own = record(service, USER_ID, database.hash, NOW);
            var pageable = org.springframework.data.domain.PageRequest.of(0, 1,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "lastSeen", "id"));
            for (boolean blocked : new boolean[] { false, true }) {
                if (blocked) service.report(USER_ID, own.getId(), NOW + 200);
                else service.confirmAfterSecondFactor(USER_ID, own.getId(), NOW + 100);
                var page = service.findDevices(USER_ID, pageable);
                assertEquals(1, page.getTotalElements());
                assertEquals(own.getId(), page.getContent().get(0).getId());
                assertNotEquals(other.getId(), page.getContent().get(0).getId());
                assertEquals(blocked, page.getContent().get(0).getReportedAt() != null);
                assertTrue(service.findDevices(USER_ID, pageable.next()).isEmpty());
            }
        }
    }

    private static DeviceEntity record(DeviceService service, int userId, String hash, long now) {
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

        /** Executes the production cleanup predicate only within this test's records. */
        void cleanupOwnDevices(long cutoff) {
            DeviceRepository cleanupRepository = mock(DeviceRepository.class, CALLS_REAL_METHODS);
            doAnswer(invocation -> {
                DeleteSpecification<DeviceEntity> expired = invocation.getArgument(0);
                return devices.delete(expired.and((root, builder) -> builder.equal(root.get("tokenHash"), hash)));
            }).when(cleanupRepository).delete(any(DeleteSpecification.class));
            new DeviceService(cleanupRepository).cleanup(cutoff);
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
