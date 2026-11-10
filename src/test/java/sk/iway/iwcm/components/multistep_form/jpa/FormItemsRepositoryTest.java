package sk.iway.iwcm.components.multistep_form.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.system.jpa.AllowHtmlAttributeConverter;
import sk.iway.iwcm.system.jpa.AllowSafeHtmlAttributeConverter;
import sk.iway.iwcm.system.jpa.WebJETPersistenceProvider;
import sk.iway.iwcm.system.jpa.XssAttributeConverter;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Verifies form item HTML storage through the repository and configured WebJET database. */
class FormItemsRepositoryTest extends BaseWebjetTest {
    private static LocalContainerEntityManagerFactoryBean factory;

    @BeforeAll
    static void initializePersistence() {
        factory = new LocalContainerEntityManagerFactoryBean();
        factory.setPersistenceUnitName("form-items-autotest-" + UUID.randomUUID());
        factory.setPersistenceProvider(new WebJETPersistenceProvider());
        factory.setDataSource(DBPool.getInstance().getDataSource("iwcm"));
        factory.setJpaVendorAdapter(new EclipseLinkJpaVendorAdapter());
        factory.setManagedTypes(PersistenceManagedTypes.of(FormItemEntity.class.getName(), FormStepEntity.class.getName(),
            XssAttributeConverter.class.getName(), AllowSafeHtmlAttributeConverter.class.getName(), AllowHtmlAttributeConverter.class.getName()));
        factory.setJpaPropertyMap(Map.of("eclipselink.weaving", "false", "eclipselink.ddl-generation", "none",
            "eclipselink.session.customizer", "sk.iway.webjet.v9.JpaSessionCustomizer",
            "eclipselink.logging.level", "WARNING"));
        factory.afterPropertiesSet();
    }

    @AfterAll
    static void closePersistence() {
        if (factory != null) factory.destroy();
    }

    /** Persists large HTML, sanitizes an update, and clears the hint through the repository. */
    @Test
    void preservesLargeMultilineHintAfterInsertAndUpdate() {
        String hint = "<p><strong>Nápoveda</strong> k vyplneniu poľa.</p><ul><li>Prvý krok</li><li>Druhý krok</li></ul>".repeat(1000);
        String[][] cases = {
            {hint, hint},
            {"<p onclick=\"alert(1)\"><strong>Safe hint</strong><script>alert(1)</script></p>", "<p><strong>Safe hint</strong></p>"},
            {null, null}
        };
        EntityManager manager = factory.getObject().createEntityManager();
        try {
            manager.getTransaction().begin();
            FormItemsRepository repository = new JpaRepositoryFactory(manager).getRepository(FormItemsRepository.class);
            FormItemEntity item = new FormItemEntity();
            item.setFormName("autotest-hint-" + UUID.randomUUID());
            item.setFieldType("text");
            item.setPlaceholder("");
            item.setItemFormId("name-1");
            item.setStepId(-1L);
            item.setSortPriority(10);
            item.setDomainId(1);
            item.setTrimValue(false);
            item.setShowStat(true);
            item.setChartType("bar_horizontal");
            item.setTopCount(5);
            item.setShowOtherCount(false);
            item.setShowUnanswered(false);
            item.setCompareInsensitive(false);

            for (String[] testCase : cases) {
                item.setHint(testCase[0]);
                repository.saveAndFlush(item);
                manager.clear();
                factory.getObject().getCache().evict(FormItemEntity.class, item.getId());
                item = repository.findById(item.getId()).orElseThrow();
                assertEquals(testCase[1], item.getHint());
            }
        } finally {
            if (manager.getTransaction().isActive()) manager.getTransaction().rollback();
            manager.close();
        }
    }

}
