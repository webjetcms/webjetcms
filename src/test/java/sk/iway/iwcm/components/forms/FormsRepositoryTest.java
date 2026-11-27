package sk.iway.iwcm.components.forms;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.system.jpa.AllowHtmlAttributeConverter;
import sk.iway.iwcm.system.jpa.AllowSafeHtmlAttributeConverter;
import sk.iway.iwcm.system.jpa.AllowSafeHtmlAttributeConverterWithSave;
import sk.iway.iwcm.system.jpa.CommaSeparatedIntegersConverter;
import sk.iway.iwcm.system.jpa.PageTitleAttributeConverter;
import sk.iway.iwcm.system.jpa.WebJETPersistenceProvider;
import sk.iway.iwcm.system.jpa.XssAttributeConverter;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Checks form include lookups against stored page content. */
class FormsRepositoryTest extends BaseWebjetTest {
    private static LocalContainerEntityManagerFactoryBean factory;

    @BeforeAll
    static void initializePersistence() {
        factory = new LocalContainerEntityManagerFactoryBean();
        factory.setPersistenceUnitName("form-includes-test");
        factory.setPersistenceProvider(new WebJETPersistenceProvider());
        factory.setDataSource(DBPool.getInstance().getDataSource("iwcm"));
        factory.setJpaVendorAdapter(new EclipseLinkJpaVendorAdapter());
        factory.setManagedTypes(PersistenceManagedTypes.of(FormsEntity.class.getName(), DocDetails.class.getName(),
            AllowHtmlAttributeConverter.class.getName(), AllowSafeHtmlAttributeConverter.class.getName(),
            AllowSafeHtmlAttributeConverterWithSave.class.getName(), CommaSeparatedIntegersConverter.class.getName(),
            PageTitleAttributeConverter.class.getName(), XssAttributeConverter.class.getName()));
        factory.setJpaPropertyMap(Map.of("eclipselink.weaving", "false", "eclipselink.ddl-generation", "none"));
        factory.afterPropertiesSet();
    }

    @AfterAll
    static void closePersistence() {
        if (factory != null) factory.destroy();
    }

    /** Editor and manual includes match the full name, even when another form shares its prefix. */
    @Test
    void findsQuotedAndUnquotedIncludesByExactName() {
        String name = "contact" + UUID.randomUUID().toString().replace("-", "");
        try (EntityManager manager = factory.getObject().createEntityManager()) {
            manager.getTransaction().begin();
            try {
                FormsRepository forms = new JpaRepositoryFactory(manager).getRepository(FormsRepository.class);
                DocDetails encoded = savePage(manager, "&quot;" + name + "&quot;");
                DocDetails doubleQuoted = savePage(manager, "\"" + name + "\"");
                DocDetails singleQuoted = savePage(manager, "'" + name + "'");
                DocDetails unquoted = savePage(manager, name);
                for (String quote : new String[] {"&quot;", "\"", "'", ""}) {
                    savePage(manager, quote + name + "2" + quote);
                }
                manager.flush();

                String prefix = "%!INCLUDE(sk.iway.iwcm.components.multistep~_form.mvc.MultistepFormApp, formName=";
                Set<Long> expectedIds = Set.of(encoded.getId(), doubleQuoted.getId(), singleQuoted.getId(), unquoted.getId());
                assertEquals(expectedIds, new HashSet<>(forms.findDocIdsByFormInclude(
                    prefix + "&quot;" + name + "&quot;%", prefix + "\"" + name + "\"%",
                    prefix + "'" + name + "'%", prefix + name + ",%")));
            } finally {
                manager.getTransaction().rollback();
            }
        }
    }

    private static DocDetails savePage(EntityManager manager, String nameParameter) {
        DocDetails page = new DocDetails();
        page.setTitle("Form include test");
        page.setNavbar(page.getTitle());
        page.setDateCreated(System.currentTimeMillis());
        page.setData("!INCLUDE(sk.iway.iwcm.components.multistep_form.mvc.MultistepFormApp, formName=" + nameParameter + ", insertDateTime=1)!");
        page.setDataAsc(page.getData());
        manager.persist(page);
        return page;
    }
}
