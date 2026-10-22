package sk.iway.iwcm.components.fields_settings.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import jakarta.persistence.Entity;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.fields_settings.jpa.FieldsSettingsEntity;
import sk.iway.iwcm.components.fields_settings.jpa.FieldsSettingsRepository;
import sk.iway.iwcm.components.tabs_settings.jpa.TabsSettingsEntity;
import sk.iway.iwcm.components.tabs_settings.rest.TabsSettingsService;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.editor.appstore.AppManager;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;
import sk.iway.iwcm.system.datatable.annotations.DataTableTab;
import sk.iway.iwcm.system.datatable.annotations.DataTableTabs;
import sk.iway.iwcm.system.datatable.json.LabelValue;

/** Tests settings caching and shared class, field and tab discovery. */
class FieldsSettingsServiceTest {

    private static final String CLASS_NAME = ExampleEntity.class.getName();
    private final FieldsSettingsRepository repository = mock(FieldsSettingsRepository.class);
    private final TabsSettingsService tabsService = mock(TabsSettingsService.class);
    private final FieldsSettingsService service = new FieldsSettingsService(repository, tabsService);
    private final Cache cache = mock(Cache.class);
    private MockedStatic<Cache> caches;
    private MockedStatic<AppManager> apps;

    @BeforeEach
    void setUp() {
        caches = mockStatic(Cache.class);
        caches.when(Cache::getInstance).thenReturn(cache);
        apps = mockStatic(AppManager.class);
        apps.when(AppManager::getPackageNames).thenReturn(List.of(getClass().getPackageName()));
    }

    @AfterEach
    void tearDown() {
        apps.close();
        caches.close();
    }

    /** Settings, including empty results, use a scoped cache and support cluster invalidation. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void cachesAndClearsSettings(boolean empty) {
        String key = service.getCacheKey(CLASS_NAME, 9);
        List<FieldsSettingsEntity> settings = empty ? List.of() : List.of(new FieldsSettingsEntity());
        when(repository.findAllByClassNameAndDomainId(CLASS_NAME, 9)).thenReturn(settings);

        FieldsSettingsEntity[] result = service.getFieldsSettings(CLASS_NAME, 9);

        assertArrayEquals(settings.toArray(FieldsSettingsEntity[]::new), result);
        verify(cache).setObjectSeconds(key, result, 3600, false);
        when(cache.getObject(key, FieldsSettingsEntity[].class)).thenReturn(result);
        assertSame(result, service.getFieldsSettings(CLASS_NAME, 9));
        verify(repository).findAllByClassNameAndDomainId(CLASS_NAME, 9);
        assertNotEquals(key, service.getCacheKey(CLASS_NAME, 17));
        assertNotEquals(key, service.getCacheKey(ColumnsOnlyEntity.class.getName(), 9));

        service.clearCache(key);
        verify(cache).removeObject(key, true);
    }

    /** One discovery result serves both autocomplete lists while preserving their eligibility and wildcard rules. */
    @Test
    void sharesDiscoveryWithCustomFields() {
        assertEquals(List.of(ColumnsOnlyEntity.class.getName(), CLASS_NAME), service.getClassOptions("*"));
        ArgumentCaptor<Object> discovered = ArgumentCaptor.forClass(Object.class);
        verify(cache).setObject(anyString(), discovered.capture(), eq(60));
        when(cache.getObject(anyString(), any())).thenReturn(discovered.getValue());

        assertEquals(List.of(CustomOnlyEntity.class.getName(), CLASS_NAME),
            service.getCustomFieldClassOptions(" FIELDSSETTINGSSERVICETEST "));
        assertEquals(List.of(TemplateDetails.class.getName()), service.getCustomFieldClassOptions("TemplateDetails"));
        assertEquals(List.of(CLASS_NAME), service.getClassOptions(" EXAMPLEENTITY "));
        assertTrue(service.getClassOptions(" ").isEmpty());
        assertTrue(service.getCustomFieldClassOptions(" ").isEmpty());
        assertTrue(service.getCustomFieldClassOptions("*").isEmpty());
        assertTrue(service.getCustomFieldClassOptions("%").isEmpty());
        apps.verify(AppManager::getPackageNames);
    }

    /** Field options exclude IDs, include nested fields, and match translated labels or raw paths. */
    @Test
    void buildsLocalizedFieldOptions() {
        Prop prop = mock(Prop.class);
        when(prop.getText("test.name")).thenReturn("Name");
        when(prop.getText("test.note")).thenReturn("Note");

        List<LabelValue> options = service.getFieldOptions("%", CLASS_NAME, prop);

        assertEquals(List.of("name", "editorFields.note"), options.stream().map(LabelValue::getValue).toList());
        assertEquals(List.of("Name (name)", "Note (editorFields.note)"), options.stream().map(LabelValue::getLabel).toList());
        assertEquals("editorFields.note", service.getFieldOptions(" NOTE ", CLASS_NAME, prop).get(0).getValue());
        assertEquals(1, service.getFieldOptions("EDITORFIELDS.", CLASS_NAME, prop).size());
        assertTrue(service.getFieldOptions("*", "unknown.Class", prop).isEmpty());
    }

    /** Saved tabs override titles and add tabs, while excluding a setting preserves declared tabs only. */
    @Test
    void combinesDeclaredAndSavedTabs() {
        TabsSettingsEntity basic = tab(1L, "basic", "test.renamed");
        TabsSettingsEntity extra = tab(2L, "extra", "test.extra");
        when(tabsService.getTabsSettings(CLASS_NAME, 9)).thenReturn(new TabsSettingsEntity[] {basic, extra});
        Prop prop = mock(Prop.class);
        when(prop.getText("test.renamed")).thenReturn("Renamed");
        when(prop.getText("test.extra")).thenReturn("Extra");

        try (MockedStatic<CloudToolsForCore> domains = mockStatic(CloudToolsForCore.class)) {
            domains.when(CloudToolsForCore::getDomainId).thenReturn(9);
            List<LabelValue> options = service.getTabOptions("*", CLASS_NAME, prop);
            assertEquals(List.of("extra", "basic"), options.stream().map(LabelValue::getValue).toList());
            assertEquals(List.of("Extra (extra)", "Renamed (basic)"), options.stream().map(LabelValue::getLabel).toList());
        }

        assertTrue(service.hasTab(CLASS_NAME, "basic", 9, basic.getId()));
        assertTrue(service.hasTab(CLASS_NAME, "extra", 9));
        assertFalse(service.hasTab(CLASS_NAME, "extra", 9, extra.getId()));
        assertFalse(service.hasTab(CLASS_NAME, "missing", 9));
    }

    /** Inferred tabs honor field overrides only in the domain that owns them. */
    @Test
    void infersTabsUsingDomainFieldSettings() {
        String className = ColumnsOnlyEntity.class.getName();
        FieldsSettingsEntity field = new FieldsSettingsEntity();
        field.setFieldName("name");
        field.setTab("advanced");
        when(repository.findAllByClassNameAndDomainId(className, 9)).thenReturn(List.of(field));
        when(tabsService.getTabsSettings(eq(className), anyInt())).thenReturn(new TabsSettingsEntity[0]);

        assertTrue(service.hasTab(className, "advanced", 9));
        assertFalse(service.hasTab(className, "basic", 9));
        assertTrue(service.hasTab(className, "basic", 17));
        assertFalse(service.hasTab(className, "advanced", 17));
    }

    private static TabsSettingsEntity tab(long id, String tabId, String title) {
        TabsSettingsEntity setting = new TabsSettingsEntity();
        setting.setId(id);
        setting.setTabId(tabId);
        setting.setTitle(title);
        return setting;
    }

    @Entity
    @DataTableTabs(tabs = @DataTableTab(id = "basic", title = "test.basic"))
    static class ExampleEntity {
        @DataTableColumn(inputType = DataTableColumnType.ID)
        private Long id;

        @DataTableColumn(title = "test.name")
        private String name;

        @DataTableColumnNested
        private TestEditorFields editorFields;
    }

    static class TestEditorFields extends BaseEditorFields {
        @DataTableColumn(title = "test.note")
        private String note;
    }

    @Entity
    static class ColumnsOnlyEntity {
        @DataTableColumn(title = "test.name", tab = "basic")
        private String name;
    }

    @Entity
    static class CustomOnlyEntity {
        @DataTableColumnNested
        private BaseEditorFields editorFields;
    }
}
