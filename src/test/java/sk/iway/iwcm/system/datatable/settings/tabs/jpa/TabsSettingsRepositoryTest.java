package sk.iway.iwcm.system.datatable.settings.tabs.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;

import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.system.jpa.AllowHtmlAttributeConverter;
import sk.iway.iwcm.system.jpa.WebJETPersistenceProvider;
import sk.iway.iwcm.system.jpa.XssAttributeConverter;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Verifies HTML tab content through the real repository and the configured WebJET database. */
class TabsSettingsRepositoryTest extends BaseWebjetTest {
    private static LocalContainerEntityManagerFactoryBean factory;

    @BeforeAll
    static void initializePersistence() {
        factory = new LocalContainerEntityManagerFactoryBean();
        factory.setPersistenceUnitName("tabs-settings-autotest-" + UUID.randomUUID());
        factory.setPersistenceProvider(new WebJETPersistenceProvider());
        factory.setDataSource(DBPool.getInstance().getDataSource("iwcm"));
        factory.setJpaVendorAdapter(new EclipseLinkJpaVendorAdapter());
        factory.setManagedTypes(PersistenceManagedTypes.of(TabsSettingsEntity.class.getName(),
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

    /** Reloads inserted and updated content without either persistence cache masking conversion. */
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
        "Plain text",
        "<p>Text &amp; ďalší text</p>",
        "<div class=\"tab-help\"><iframe src=\"about:blank\"></iframe><webjet-image-area-selector></webjet-image-area-selector></div>"
    })
    void preservesContentAfterInsertAndUpdate(String content) {
        assertFalse(DB.isHtmlAllowed("jpaentity"), "The test requires the default JPA HTML filtering to be active");
        EntityManager manager = factory.getObject().createEntityManager();
        try {
            manager.getTransaction().begin();
            TabsSettingsRepository repository = new JpaRepositoryFactory(manager).getRepository(TabsSettingsRepository.class);
            TabsSettingsEntity settings = new TabsSettingsEntity();
            settings.setClassName("autotest.TabsSettings");
            settings.setTabId("autotest-" + UUID.randomUUID());
            settings.setDomainId(1);
            settings.setTitle("<b>autotest</b>");
            settings.setContent(content);
            settings = repository.saveAndFlush(settings);

            TabsSettingsEntity loaded = reload(manager, repository, settings.getId());
            assertNotSame(settings, loaded);
            assertEquals(content, loaded.getContent());
            assertEquals("&lt;b&gt;autotest&lt;/b&gt;", loaded.getTitle(), "Ordinary text fields must still be escaped");

            String updatedContent = "<p class=\"updated\">Updated tab content</p>";
            loaded.setContent(updatedContent);
            repository.saveAndFlush(loaded);
            assertEquals(updatedContent, reload(manager, repository, loaded.getId()).getContent());
        } finally {
            if (manager.getTransaction().isActive()) manager.getTransaction().rollback();
            manager.close();
        }
    }

    private TabsSettingsEntity reload(EntityManager manager, TabsSettingsRepository repository, Long id) {
        manager.clear();
        factory.getObject().getCache().evict(TabsSettingsEntity.class, id);
        return repository.findById(id).orElseThrow();
    }
}
