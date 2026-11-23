package sk.iway.iwcm.admin.settings;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.admin.layout.UserDto;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsTreeService;
import sk.iway.iwcm.editor.service.WebpagesService;
import sk.iway.iwcm.test.BaseWebjetTest;
import sk.iway.iwcm.users.SettingsAdminBean;

/** Verifies shared tree defaults, saved preference precedence and server-side folder ordering. */
class AdminSettingsServiceTest extends BaseWebjetTest {
    private static final String SETTINGS_KEY = AdminSettingsService.WEBPAGES_TREE_SETTINGS_KEY;
    private static final String CONFIG_KEY = "webpagesTreeDefaultSettings";
    private String originalDefaults;
    private Identity user;

    @BeforeEach
    void setUp() {
        originalDefaults = Constants.getString(CONFIG_KEY);
        user = new Identity();
        user.setUserId(42);
        user.setAdminSettings(new HashMap<>());
    }

    @AfterEach
    void tearDown() {
        Constants.setString(CONFIG_KEY, originalDefaults);
    }

    /** A user without preferences receives the same defaults in the browser and the folder sorter. */
    @Test
    void defaultsToAscendingPriorityWithoutChangingUserSettings() {
        AdminSettingsService service = new AdminSettingsService(user);
        assertEquals("priority", service.getJsonValue(SETTINGS_KEY, "treeSortType"));
        assertTrue(service.getJsonBooleanValue(SETTINGS_KEY, "treeSortOrderAsc"));
        assertEquals(4, service.getJsonIntValue(SETTINGS_KEY, "treeWidth"));
        for (String field : List.of("showId", "showPriority", "showPages", "showFoldersDt", "showRealName")) {
            assertEquals("false", service.getJsonValue(SETTINGS_KEY, field));
        }
        assertEquals(service.getValue(SETTINGS_KEY), new UserDto(user).getAdminSettings().get(SETTINGS_KEY));

        GroupDetails first = group("First", 10);
        GroupDetails second = group("Second", 20);
        assertEquals(List.of(first, second), GroupsTreeService.sortGroupsBasedOnUserSettings(user, List.of(second, first)));
        assertTrue(user.getAdminSettings().isEmpty());
    }

    /** Configuration changes affect an existing session until the user saves a personal preference. */
    @Test
    void readsChangedConfigurationWithoutCachingItOnTheUser() {
        AdminSettingsService service = new AdminSettingsService(user);
        service.getValue(SETTINGS_KEY);
        new UserDto(user);
        Constants.setString(CONFIG_KEY, "{\"showId\":true,\"showPriority\":true,\"treeWidth\":300,\"treeSortType\":\"title\",\"treeSortOrderAsc\":false}");

        assertTrue(service.getJsonBooleanValue(SETTINGS_KEY, "showId"));
        assertTrue(service.getJsonBooleanValue(SETTINGS_KEY, "showPriority"));
        assertEquals(300, service.getJsonIntValue(SETTINGS_KEY, "treeWidth"));
        assertEquals("title", WebpagesService.getTreeSortType(user));
        assertFalse(WebpagesService.isTreeSortOrderAsc(user));
        assertEquals(service.getValue(SETTINGS_KEY), new UserDto(user).getAdminSettings().get(SETTINGS_KEY));

        GroupDetails alpha = group("Alpha", 20);
        GroupDetails zulu = group("Zulu", 10);
        assertEquals(List.of(zulu, alpha), GroupsTreeService.sortGroupsBasedOnUserSettings(user, List.of(alpha, zulu)));
        assertTrue(user.getAdminSettings().isEmpty());
    }

    /** Even an empty or partial saved object takes precedence over the complete configured object. */
    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"showId\":false}", "{\"treeSortType\":\"priority\",\"treeSortOrderAsc\":false}"})
    void preservesSavedSettingsWithoutMergingDefaults(String saved) {
        Constants.setString(CONFIG_KEY, "{\"showId\":true,\"treeSortType\":\"title\",\"treeSortOrderAsc\":true}");
        SettingsAdminBean bean = new SettingsAdminBean(user.getUserId(), SETTINGS_KEY, saved);
        user.getAdminSettings().put(SETTINGS_KEY, bean);

        AdminSettingsService service = new AdminSettingsService(user);
        assertEquals(saved, service.getValue(SETTINGS_KEY));
        assertEquals(saved, new UserDto(user).getAdminSettings().get(SETTINGS_KEY));
        assertFalse(service.getJsonBooleanValue(SETTINGS_KEY, "showId"));
        assertFalse(WebpagesService.isTreeSortOrderAsc(user));
        assertEquals("priority", WebpagesService.getTreeSortType(user));
        assertSame(bean, user.getAdminSettings().get(SETTINGS_KEY));
        assertEquals(saved, bean.getValue());
    }

    /** Web page defaults do not change other trees or expose dashboard settings in the shared layout. */
    @Test
    void keepsOtherSettingsUnchanged() {
        String galleryKey = "jstreeSettings_gallery";
        AdminSettingsService service = new AdminSettingsService(user);
        assertNull(service.getValue(galleryKey));
        assertNull(service.getValue("unknown"));
        assertFalse(new UserDto(user).getAdminSettings().containsKey(galleryKey));

        user.getAdminSettings().put(galleryKey, new SettingsAdminBean(42, galleryKey, "{\"treeWidth\":6}"));
        user.getAdminSettings().put("overview.layout.v1", new SettingsAdminBean(42, "overview.layout.v1", "{}"));
        UserDto dto = new UserDto(user);
        assertEquals("{\"treeWidth\":6}", dto.getAdminSettings().get(galleryKey));
        assertFalse(dto.getAdminSettings().containsKey("overview.layout.v1"));
    }

    /** Invalid configuration cannot inject a non-object value into the browser's JSON settings reader. */
    @ParameterizedTest
    @ValueSource(strings = {"", "{broken", "null", "[]", "true"})
    void invalidConfigurationUsesAnEmptyObject(String value) {
        Constants.setString(CONFIG_KEY, value);
        assertEquals("{}", new AdminSettingsService(user).getValue(SETTINGS_KEY));
        assertEquals("{}", new UserDto(user).getAdminSettings().get(SETTINGS_KEY));
        assertTrue(user.getAdminSettings().isEmpty());
    }

    private GroupDetails group(String name, int priority) {
        GroupDetails group = new GroupDetails();
        group.setGroupName(name);
        group.setSortPriority(priority);
        return group;
    }
}
