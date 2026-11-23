package sk.iway.iwcm.components.configuration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.components.configuration.model.ConfPreparedEntity;
import sk.iway.iwcm.components.configuration.model.ConfPreparedRepository;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.jpa.WebJETPersistenceProvider;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Checks history and planned configuration permissions with persisted records in a rolled-back transaction. */
class ConfigurationPreparedControllerTest extends BaseWebjetTest {
    private static LocalContainerEntityManagerFactoryBean factory;
    private static final PageRequest ONE_ROW = PageRequest.of(0, 1);
    private final String prefix = "test_" + UUID.randomUUID();
    private final String visible = prefix + "_visible";
    private final String hidden = visible.toUpperCase(Locale.ROOT);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final Identity user = mock(Identity.class);
    private EntityManager manager;
    private ConfPreparedRepository repository;
    private ConfigurationPreparedController controller;
    private MockedStatic<Constants> constants;
    private MockedStatic<Adminlog> audit;

    @BeforeAll
    static void initializePersistence() {
        factory = new LocalContainerEntityManagerFactoryBean();
        factory.setPersistenceUnitName("configuration-history-test-" + UUID.randomUUID());
        factory.setPersistenceProvider(new WebJETPersistenceProvider());
        factory.setDataSource(DBPool.getInstance().getDataSource("iwcm"));
        factory.setJpaVendorAdapter(new EclipseLinkJpaVendorAdapter());
        factory.setManagedTypes(PersistenceManagedTypes.of(ConfPreparedEntity.class.getName()));
        // The CMS writes history through JDBC with a null planned date.
        factory.setJpaPropertyMap(Map.of("eclipselink.weaving", "false", "eclipselink.ddl-generation", "none",
            "jakarta.persistence.validation.mode", "NONE"));
        factory.afterPropertiesSet();
    }

    @AfterAll
    static void closePersistence() {
        if (factory != null) factory.destroy();
    }

    @BeforeEach
    void setUp() {
        manager = factory.getObject().createEntityManager();
        manager.getTransaction().begin();
        repository = new JpaRepositoryFactory(manager).getRepository(ConfPreparedRepository.class);
        controller = spy(new ConfigurationPreparedController(repository));
        controller.setRequest(request);
        doReturn(user).when(controller).getUser();
        audit = mockStatic(Adminlog.class);
        constants = mockStatic(Constants.class, CALLS_REAL_METHODS);
        constants.when(() -> Constants.getStringExecuteMacro("configEnabledKeys")).thenReturn(prefix);
    }

    @AfterEach
    void tearDown() {
        try {
            if (manager.getTransaction().isActive()) manager.getTransaction().rollback();
            manager.close();
        } finally {
            constants.close();
            audit.close();
        }
    }

    /** History, search, export and totals must all count only names the editor can see. */
    @Test
    void filtersStoredKeysBeforePagingSearchAndExport() {
        save(hidden, null);
        long visibleId = save(visible, null);
        save(visible, new Date());
        request.setParameter("mode", "history");
        request.setParameter("name", visible);
        assertOnlyRow(controller.getAll(ONE_ROW), visibleId);

        request.removeParameter("name");
        request.setParameter("export", "true");
        request.setParameter("searchValue", "^" + prefix + "$");
        var filters = DatatableRestControllerV2.getParamsMap(request);
        assertOnlyRow(controller.findByColumns(filters, ONE_ROW, new ConfPreparedEntity()), visibleId);
        assertTrue(controller.findByColumns(filters, ONE_ROW.next(), new ConfPreparedEntity()).isEmpty());

        request.setParameter("name", hidden);
        assertEquals(0, controller.getAll(ONE_ROW).getTotalElements());
        request.removeParameter("name");
        assertTrue(controller.getAll(ONE_ROW).isEmpty());
    }

    /** Reads and deletes use the persisted key. Only future changes may be removed. */
    @Test
    void authorizesTargetIdForReadsAndDeletesAndPreservesHistory() {
        Date future = new Date(System.currentTimeMillis() + 86_400_000L);
        long hiddenId = save(hidden, future);
        long allowedId = save(visible, future);
        long historyId = save(visible, null);
        request.setParameter("mode", "planned");
        request.setParameter("name", visible);
        assertOnlyRow(controller.getAll(ONE_ROW), allowedId);
        assertEquals(visible, controller.getOne(allowedId).getName());
        assertThrows(ConstraintViolationException.class, () -> controller.getOne(hiddenId));

        ConfPreparedEntity submitted = entry(visible, future);
        assertThrows(ConstraintViolationException.class, () -> controller.delete(hiddenId, submitted));
        assertTrue(repository.existsById(hiddenId));
        RuntimeException historyDenied = assertThrows(RuntimeException.class, () -> controller.delete(historyId, entry(visible, future)));
        assertEquals(controller.getProp().getText("admin.cong_editor.youCanOnlyDeleteFutureRecords"), historyDenied.getMessage());
        assertTrue(repository.existsById(historyId));
        assertEquals(true, controller.delete(allowedId, entry(visible, future)).getBody().get("result"));
        assertFalse(repository.existsById(allowedId));
    }

    /** Restricted users cannot request whole-table summaries or match records through the import lookup. */
    @Test
    void protectsWholeTableOperations() {
        ConfPreparedEntity row = entry(visible, null);
        assertEquals("", controller.getSum(row, new String[] {"id"}));
        var rows = Map.of(1L, row);
        RuntimeException denied = assertThrows(RuntimeException.class, () -> controller.preImportDataEdit(request, rows, "onlyNew"));
        assertEquals(controller.getProp().getText("datatables.error.recordIsNotEditable"), denied.getMessage());
        when(user.isEnabledItem("conf.show_all_variables")).thenReturn(true);
        assertSame(rows, controller.preImportDataEdit(request, rows, "onlyNew"));
    }

    /** Unlimited access comes from the same two exceptions as in the main configuration editor. */
    @Test
    void honorsShowAllAndUnrestrictedConfigurationAndRejectsMissingUser() {
        long id = save(hidden, null);
        request.setParameter("name", hidden);
        request.setParameter("mode", "history");
        when(user.isEnabledItem("conf.show_all_variables")).thenReturn(true);
        assertOnlyRow(controller.getAll(ONE_ROW), id);
        when(user.isEnabledItem("conf.show_all_variables")).thenReturn(false);
        constants.when(() -> Constants.getStringExecuteMacro("configEnabledKeys")).thenReturn("");
        assertOnlyRow(controller.getAll(ONE_ROW), id);
        doReturn(null).when(controller).getUser();
        assertTrue(controller.getAll(ONE_ROW).isEmpty());
        assertFalse(controller.checkItemPerms(new ConfPreparedEntity(), id));
    }

    private long save(String name, Date planned) {
        long id = repository.saveAndFlush(entry(name, planned)).getId();
        manager.clear();
        return id;
    }

    private ConfPreparedEntity entry(String name, Date planned) {
        ConfPreparedEntity row = new ConfPreparedEntity();
        row.setName(name);
        row.setValue(prefix);
        row.setDateChanged(new Date());
        row.setDatePrepared(planned);
        return row;
    }

    private static void assertOnlyRow(Page<ConfPreparedEntity> page, long id) {
        assertEquals(1, page.getTotalElements());
        assertEquals(List.of(id), page.getContent().stream().map(ConfPreparedEntity::getId).toList());
    }
}
