package sk.iway.iwcm.components.welcome;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.components.welcome.DashboardSettingsDto.Item;

/** Verifies dashboard invariants and the portable settings record limits. */
class DashboardSettingsServiceTest {
    private final DashboardSettingsRepository repository = mock(DashboardSettingsRepository.class);
    private final DashboardSettingsService service = new DashboardSettingsService(repository);

    @Test
    void supportsThirtyTwoInstancesWithoutExceedingTheLayoutRecordLimit() {
        DashboardSettingsDto settings = settings();
        for (int i = 1; i < 32; i++) settings.getItems().add(item(UUID.randomUUID().toString(), "shortcut", "1x1"));

        Map<String, String> records = service.validateAndSerialize(settings, "42");

        assertEquals(33, records.size());
        assertTrue(records.values().stream().allMatch(value -> value.length() <= 2000));
        settings.getItems().add(item("excess", "forms", "1x1"));
        assertThrows(IllegalArgumentException.class, () -> service.save(7, "42", settings));
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsOversizedGlobalAndDomainOptionsBeforeWriting() {
        DashboardSettingsDto settings = settings();
        settings.getItems().get(0).setOptions(Map.of("text", "x".repeat(2000)));
        assertThrows(IllegalArgumentException.class, () -> service.save(7, "42", settings));

        settings.getItems().get(0).setOptions(Map.of());
        settings.getDomainOptions().put("sessions-1", Map.of("text", "x".repeat(2000)));
        assertThrows(IllegalArgumentException.class, () -> service.save(7, "42", settings));
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsUnknownTypesInvalidSizesAndDuplicateInstances() {
        DashboardSettingsDto settings = settings();
        settings.getItems().clear();
        settings.getItems().add(item("bad", "unregistered", "1x1"));
        assertThrows(IllegalArgumentException.class, () -> service.validateAndSerialize(settings, "42"));

        settings.setItems(settings().getItems());
        settings.getItems().get(0).setSize("3x3");
        assertThrows(IllegalArgumentException.class, () -> service.validateAndSerialize(settings, "42"));

        settings.setItems(settings().getItems());
        settings.getItems().add(item("sessions-1", "shortcut", "1x1"));
        assertThrows(IllegalArgumentException.class, () -> service.validateAndSerialize(settings, "42"));

        settings.setItems(settings().getItems());
        settings.getItems().add(item("sessions-2", "sessions", "2x3"));
        assertThrows(IllegalArgumentException.class, () -> service.validateAndSerialize(settings, "42"));
    }

    /** Compact defaults and existing personal size choices both survive a settings round trip. */
    @Test
    void acceptsCompactPreviewSizesWithoutChangingExistingPersonalSizes() {
        Map<String, java.util.List<String>> variants = Map.of(
            "recent-pages", java.util.List.of("3x2", "2x3", "3x3"),
            "referrers", java.util.List.of("2x2", "2x3", "3x3"),
            "publishing", java.util.List.of("2x2", "2x3"),
            "search-terms", java.util.List.of("3x3", "2x3")
        );
        variants.forEach((type, sizes) -> sizes.forEach(size -> {
            DashboardSettingsDto source = settings();
            Item preview = item("preview", type, size);
            preview.setCollapsed(true);
            source.getItems().add(preview);
            when(repository.read(7)).thenReturn(service.validateAndSerialize(source, "42"));

            DashboardSettingsDto loaded = service.load(7, "42");

            assertTrue(loaded.isConfigured(), type + " " + size);
            assertEquals(size, loaded.getItems().get(1).getSize());
            assertTrue(loaded.getItems().get(1).isCollapsed());
        }));
        verify(repository, never()).replace(anyInt(), anyString(), anyMap(), anySet());
    }

    @Test
    void rejectsUnknownDomainOptionOwnerAndInvalidInstanceIds() {
        DashboardSettingsDto settings = settings();
        settings.getDomainOptions().put("other-user-widget", Map.of());
        assertThrows(IllegalArgumentException.class, () -> service.validateAndSerialize(settings, "42"));

        settings.getDomainOptions().clear();
        settings.getItems().get(0).setId("../another.preference");
        assertThrows(IllegalArgumentException.class, () -> service.validateAndSerialize(settings, "42"));
    }

    @Test
    void readsTheSharedLayoutWithOnlyCurrentDomainOptions() {
        DashboardSettingsDto source = settings();
        source.getItems().add(item("form-1", "forms", "3x3"));
        source.getDomainOptions().put("form-1", Map.of("formName", "contact-a"));
        source.setAcknowledgedNewsVersion("2026.18");
        Map<String, String> records = new LinkedHashMap<>(service.validateAndSerialize(source, "42"));
        records.put("overview.domain.84.form-1", "{\"formName\":\"contact-b\"}");
        when(repository.read(7)).thenReturn(records);

        DashboardSettingsDto first = service.load(7, "42");
        DashboardSettingsDto second = service.load(7, "84");

        assertTrue(first.isConfigured());
        assertEquals("contact-a", first.getDomainOptions().get("form-1").get("formName"));
        assertEquals("contact-b", second.getDomainOptions().get("form-1").get("formName"));
        assertEquals("2026.18", second.getAcknowledgedNewsVersion());
        assertEquals(first.getItems().stream().map(Item::getId).toList(), second.getItems().stream().map(Item::getId).toList());
        verify(repository, times(2)).read(7);
    }

    @Test
    void distinguishesMissingConfigurationAndDoesNotOverwriteCorruptRecords() {
        when(repository.read(7)).thenReturn(Map.of());
        assertFalse(service.load(7, "42").isConfigured());

        when(repository.read(7)).thenReturn(Map.of("overview.layout.v1", "{truncated"));
        assertFalse(service.load(7, "42").isConfigured());

        when(repository.read(7)).thenReturn(Map.of("overview.layout.v1", ""));
        assertFalse(service.load(7, "42").isConfigured());

        when(repository.read(7)).thenReturn(Map.of("overview.layout.v1", "{\"version\":1,\"order\":[\"sessions-1\"]}", "overview.widget.sessions-1", "null"));
        assertFalse(service.load(7, "42").isConfigured());
        verify(repository, never()).replace(anyInt(), anyString(), anyMap(), anySet());
    }

    /** Requires the security widget on writes while allowing the UI to retain and upgrade an older layout. */
    @Test
    void requiresSessionsForWritesWithoutDiscardingLegacyLayoutsOnRead() {
        DashboardSettingsDto legacy = new DashboardSettingsDto();
        legacy.getItems().add(item("shortcut-1", "shortcut", "1x1"));
        when(repository.read(7)).thenReturn(service.validateAndSerialize(legacy, "42"));

        DashboardSettingsDto loaded = service.load(7, "42");
        assertTrue(loaded.isConfigured());
        assertEquals("shortcut-1", loaded.getItems().get(0).getId());
        assertThrows(IllegalArgumentException.class, () -> service.save(7, "42", loaded));
        verify(repository, never()).replace(anyInt(), anyString(), anyMap(), anySet());
    }

    @Test
    void returnsConfiguredOnlyAfterSuccessfulPersistence() {
        DashboardSettingsDto settings = settings();
        doThrow(new IllegalStateException("Database is unavailable")).when(repository).replace(anyInt(), anyString(), anyMap(), anySet());

        assertThrows(IllegalStateException.class, () -> service.save(7, "42", settings));
        assertFalse(settings.isConfigured());

        doNothing().when(repository).replace(anyInt(), anyString(), anyMap(), anySet());
        assertTrue(service.save(7, "42", settings).isConfigured());
        verify(repository, times(2)).replace(eq(7), eq("42"), anyMap(), eq(java.util.Set.of("sessions-1")));
    }

    /** Reset returns a new unconfigured profile only after persistence succeeds. */
    @Test
    void resetReturnsCleanDefaultsAndPropagatesPersistenceFailure() {
        DashboardSettingsDto result = service.reset(7);

        assertEquals(1, result.getVersion());
        assertFalse(result.isConfigured());
        assertTrue(result.getItems().isEmpty());
        assertTrue(result.getDomainOptions().isEmpty());
        assertNull(result.getAcknowledgedNewsVersion());
        verify(repository).reset(7);
        verify(repository, never()).read(anyInt());
        verify(repository, never()).replace(anyInt(), anyString(), anyMap(), anySet());

        doThrow(new IllegalStateException("Database is unavailable")).when(repository).reset(7);
        assertThrows(IllegalStateException.class, () -> service.reset(7));
    }

    /** Rejects executable and ambiguous destinations before any settings can be persisted. */
    @Test
    void validatesCustomShortcutDestinationsAndRetainsLegacyMenuOptions() {
        for (String href : java.util.List.of("https://www.webjetcms.sk/docs?a=1#help", "http://intranet.example/", "/admin/v9/?x=1")) {
            DashboardSettingsDto settings = settings();
            Item shortcut = item("custom", "shortcut", "1x1");
            shortcut.setOptions(Map.of("source", "url", "href", href, "title", "Documentation"));
            settings.getItems().add(shortcut);
            assertDoesNotThrow(() -> service.validateAndSerialize(settings, "42"));
        }
        for (String href : java.util.List.of("javascript:alert(1)", "data:text/html,test", "//evil.example/", "/\\evil.example/",
                "https://user:password@example.com/", "https://example.com/\n", "https:example.com", "relative/path")) {
            assertThrows(IllegalArgumentException.class, () -> DashboardSettingsService.validateShortcut(
                Map.of("source", "url", "href", href, "title", "Unsafe")), href);
        }
        assertDoesNotThrow(() -> DashboardSettingsService.validateShortcut(Map.of("href", "/apps/form/admin/")));
        assertThrows(IllegalArgumentException.class, () -> DashboardSettingsService.validateShortcut(Map.of("source", "url", "href", "https://example.com", "title", " ")));
        assertThrows(IllegalArgumentException.class, () -> DashboardSettingsService.validateShortcut(Map.of("source", "other")));
        assertThrows(IllegalArgumentException.class, () -> DashboardSettingsService.validateShortcut(Map.of("href", 123)));
        verifyNoInteractions(repository);
    }

    static DashboardSettingsDto settings() {
        DashboardSettingsDto settings = new DashboardSettingsDto();
        settings.getItems().add(item("sessions-1", "sessions", "2x3"));
        return settings;
    }

    static Item item(String id, String type, String size) {
        Item item = new Item();
        item.setId(id);
        item.setType(type);
        item.setSize(size);
        return item;
    }
}
