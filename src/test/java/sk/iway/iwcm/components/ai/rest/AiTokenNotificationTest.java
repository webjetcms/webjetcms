package sk.iway.iwcm.components.ai.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionRepository;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.jpa.AllowHtmlAttributeConverter;
import sk.iway.iwcm.system.jpa.WebJETPersistenceProvider;
import sk.iway.iwcm.system.jpa.XssAttributeConverter;
import sk.iway.iwcm.test.BaseWebjetTest;
import sk.iway.iwcm.utils.Pair;

/** Verifies notification delivery and conditional marker writes against the configured WebJET database. */
class AiTokenNotificationTest extends BaseWebjetTest {
    private static LocalContainerEntityManagerFactoryBean factory;
    private EntityManager manager;
    private AssistantDefinitionRepository repository;
    private Long assistantId;
    private MockedStatic<Adminlog> audit;

    @BeforeAll
    static void initializePersistence() {
        factory = new LocalContainerEntityManagerFactoryBean();
        factory.setPersistenceUnitName("ai-token-notification-autotest-" + UUID.randomUUID());
        factory.setPersistenceProvider(new WebJETPersistenceProvider());
        factory.setDataSource(DBPool.getInstance().getDataSource("iwcm"));
        factory.setJpaVendorAdapter(new EclipseLinkJpaVendorAdapter());
        factory.setManagedTypes(PersistenceManagedTypes.of(AssistantDefinitionEntity.class.getName(),
            XssAttributeConverter.class.getName(), AllowHtmlAttributeConverter.class.getName()));
        factory.setJpaPropertyMap(Map.of("eclipselink.weaving", "false", "eclipselink.ddl-generation", "none",
            "eclipselink.session.customizer", "sk.iway.webjet.v9.JpaSessionCustomizer",
            "eclipselink.logging.level", "WARNING"));
        factory.afterPropertiesSet();
    }

    @AfterAll
    static void closePersistence() {
        if (factory != null) factory.destroy();
    }

    @BeforeEach
    void createAssistant() {
        audit = mockStatic(Adminlog.class);
        manager = SharedEntityManagerCreator.createSharedEntityManager(factory.getObject());
        JpaRepositoryFactory repositories = new JpaRepositoryFactory(manager);
        TransactionInterceptor transactions = new TransactionInterceptor();
        transactions.setTransactionManager(new JpaTransactionManager(factory.getObject()));
        transactions.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        repositories.addRepositoryProxyPostProcessor((proxy, information) -> proxy.addAdvice(transactions));
        repository = repositories.getRepository(AssistantDefinitionRepository.class);
        AssistantDefinitionEntity assistant = new AssistantDefinitionEntity();
        assistant.setName("token-notification-autotest-" + UUID.randomUUID());
        assistant.setClassName(AiAssistantsService.CHAT_TARGET);
        assistant.setFieldTo("*");
        assistant.setInstructions("Test token notifications without sending email.");
        assistant.setProvider("openai");
        assistant.setAction("live_chat");
        assistant.setDomainId(1);
        assistant.setActive(false);
        assistant.setKeepHtml(false);
        assistant.setUseStreaming(false);
        assistant.setUseTemporal(false);
        assistant.setUserPromptEnabled(false);
        assistant.setLastUpdate(new Date());
        assistant.setTokenLimit(100L);
        assistant.setNotifEmail("token-notification@example.invalid");
        assistantId = repository.saveAndFlush(assistant).getId();
        setState(75L, 100L, 0);
        reload();
    }

    @AfterEach
    void deleteAssistant() {
        try {
            if (manager != null) {
                if (assistantId != null) {
                    repository.deleteById(assistantId);
                }
                manager.close();
            }
        } finally {
            if (audit != null) audit.close();
        }
    }

    /** Applies competing database writes during SMTP delivery and verifies that the final marker respects them. */
    @ParameterizedTest
    @CsvSource({"80,100,0,1,75", "100,100,100,1,100", "0,100,0,1,0", "75,200,0,1,0", "75,0,0,1,0", "75,100,0,2,0"})
    void respectsConcurrentUsageResetLimitAndDomainChanges(long currentUsed, long currentLimit,
            int currentLevel, int currentDomain, int expectedLevel) {
        notifyWithMail(true, false, () -> {
            setState(currentUsed, currentLimit, currentLevel);
            new SimpleQuery().execute("UPDATE ai_assistants SET domain_id = ? WHERE id = ?", currentDomain, assistantId);
        });

        AssistantDefinitionEntity loaded = reload();
        assertEquals(currentUsed, loaded.getUsedTokens());
        assertEquals(currentLimit, loaded.getTokenLimit());
        assertEquals(currentDomain, loaded.getDomainId());
        assertEquals(expectedLevel, loaded.getNotifLevel());
    }

    /** Advances notification state only after successful delivery or queuing; a failed attempt remains eligible. */
    @ParameterizedTest
    @CsvSource({"true,false,75", "false,true,75", "false,false,0"})
    void recordsOnlySentOrQueuedNotifications(boolean sent, boolean queued, int expectedLevel) {
        notifyWithMail(sent, queued, () -> assertEquals(0, new SimpleQuery().forInt(
            "SELECT notif_level FROM ai_assistants WHERE id = ?", assistantId)));

        assertEquals(expectedLevel, reload().getNotifLevel());
    }

    /** A later read observes the repository marker update without manually clearing the persistence caches. */
    @Test
    void readsSentMarkerWithoutManualCacheEviction() {
        notifyWithMail(true, false, () -> {});
        notifyWithMail(true, false, () -> {}, false);

        assertEquals(75, reload().getNotifLevel());
    }

    /** Keeps completed thresholds quiet and sends only the 100% notification when a request crosses both thresholds. */
    @ParameterizedTest
    @CsvSource({"74,0,false,0", "75,75,false,75", "100,100,false,100", "120,0,true,100"})
    void skipsCompletedThresholdsAndSelectsHighestCrossedThreshold(long used, int previousLevel, boolean expectedSend, int expectedLevel) {
        setState(used, 100L, previousLevel);
        reload();
        notifyWithMail(true, false, () -> {}, expectedSend);

        assertEquals(expectedLevel, reload().getNotifLevel());
    }

    private void notifyWithMail(boolean sent, boolean queued, Runnable duringDelivery) {
        notifyWithMail(sent, queued, duringDelivery, true);
    }

    private void notifyWithMail(boolean sent, boolean queued, Runnable duringDelivery, boolean expectedSend) {
        GroupsDB groups = mock(GroupsDB.class);
        when(groups.getDomain(1)).thenReturn("example.invalid");
        Prop prop = mock(Prop.class);
        when(prop.getText(anyString())).thenAnswer(call -> call.getArgument(0));
        try (var tools = mockStatic(Tools.class, CALLS_REAL_METHODS);
             var domains = mockStatic(GroupsDB.class);
             var mail = mockStatic(SendMail.class)) {
            tools.when(() -> Tools.getSpringBean("assistantDefinitionRepository", AssistantDefinitionRepository.class)).thenReturn(repository);
            domains.when(GroupsDB::getInstance).thenReturn(groups);
            mail.when(() -> SendMail.sendCapturingException(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), eq(false)))
                .thenAnswer(call -> {
                    assertFalse(TransactionSynchronizationManager.isActualTransactionActive(), "SMTP delivery must run outside a transaction");
                    assertEquals("components.ai_assistants.token_notification." + (reload().getUsedTokens() >= 100 ? 100 : 75) + ".subject", call.getArgument(6));
                    duringDelivery.run();
                    return Pair.of(sent, null);
                });
            mail.when(() -> SendMail.sendLater(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(queued);

            AiAssistantsService.notifyUsage(assistantId, 1, prop);

            mail.verify(() -> SendMail.sendCapturingException(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), eq(false)), times(expectedSend ? 1 : 0));
            mail.verify(() -> SendMail.sendLater(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()), times(expectedSend && !sent ? 1 : 0));
        }
    }

    private void setState(long used, long limit, int level) {
        new SimpleQuery().execute("UPDATE ai_assistants SET used_tokens = ?, token_limit = ?, notif_level = ? WHERE id = ?",
            used, limit, level, assistantId);
    }

    private AssistantDefinitionEntity reload() {
        manager.clear();
        factory.getObject().getCache().evict(AssistantDefinitionEntity.class, assistantId);
        return repository.findById(assistantId).orElseThrow();
    }
}
