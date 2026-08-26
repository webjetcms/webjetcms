package sk.iway.iwcm.components.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.admin.jstree.JsTreeItem;

class ConfigurationTreeRestControllerTest {

    private static final List<String> MODULE_PATHS = List.of(
        "apps",
        "apps.basket",
        "apps.gallery",
        "apps.gallery.image-editor",
        "security.oauth2",
        "system.config"
    );

    @Test
    void createsSpecialViewsAndAlphabeticalTechnicalRoots() {
        List<JsTreeItem> items = ConfigurationTreeRestController.getRootItems(MODULE_PATHS, "Changed", "Custom", "All");

        assertEquals(List.of("Changed", "Custom", "All", "apps", "security", "system"), items.stream().map(JsTreeItem::getText).toList());
        assertEquals(ConfigurationTreeRestController.CHANGED_ID, items.get(0).getId());
        assertEquals(ConfigurationTreeRestController.CUSTOM_ID, items.get(1).getId());
        assertTrue(items.get(0).getState().isSelected());
        assertFalse(items.get(1).getState().isSelected());
        assertFalse(items.get(2).getState().isSelected());
        assertEquals(List.of("#", "#", "#", "#", "#", "#"), items.stream().map(JsTreeItem::getParent).toList());
        assertTrue(items.get(3).getChildren());
        assertTrue(items.get(4).getChildren());
        assertTrue(items.get(5).getChildren());
    }

    @Test
    void createsOnlyDirectSelectableChildren() {
        List<JsTreeItem> appItems = ConfigurationTreeRestController.getModuleItems(MODULE_PATHS, "apps");

        assertEquals(List.of("apps.basket", "apps.gallery"), appItems.stream().map(JsTreeItem::getVirtualPath).toList());
        assertFalse(appItems.get(0).getChildren());
        assertTrue(appItems.get(1).getChildren());
        assertEquals("gallery", appItems.get(1).getText());

        List<JsTreeItem> galleryItems = ConfigurationTreeRestController.getModuleItems(MODULE_PATHS, "apps.gallery");
        assertEquals(List.of("apps.gallery.image-editor"), galleryItems.stream().map(JsTreeItem::getVirtualPath).toList());
        assertFalse(galleryItems.get(0).getChildren());
    }

    @Test
    void returnsSearchMatchesWithTheirParents() {
        List<JsTreeItem> items = ConfigurationTreeRestController.getSearchItems(
            MODULE_PATHS,
            "Changed",
            "Custom",
            "All",
            "image-editor",
            "equals"
        );

        assertEquals(
            List.of("apps", "apps.gallery", "apps.gallery.image-editor"),
            items.stream().map(JsTreeItem::getVirtualPath).toList()
        );
        assertEquals("#", items.get(0).getParent());
        assertEquals(items.get(0).getId(), items.get(1).getParent());
        assertEquals(items.get(1).getId(), items.get(2).getParent());
    }

    @Test
    void returnsCustomViewFromServerSearch() {
        List<JsTreeItem> items = ConfigurationTreeRestController.getSearchItems(
            MODULE_PATHS,
            "Changed",
            "Customer variables",
            "All",
            "customer",
            "startwith"
        );

        assertEquals(1, items.size());
        assertEquals(ConfigurationTreeRestController.CUSTOM_ID, items.get(0).getId());
        assertEquals(ConfigurationTreeRestController.TYPE_CUSTOM, items.get(0).getTypeCustom());
        assertEquals("custom", items.get(0).getLiAttr().get("data-configuration-view"));
    }

    @Test
    void usesSafeNamespacedIdsInsteadOfLogicalPaths() {
        String modulePath = "security.oauth2";
        String id = ConfigurationTreeRestController.getModuleNodeId(modulePath);

        assertTrue(id.startsWith(ConfigurationTreeRestController.MODULE_NODE_ID_PREFIX));
        assertNotEquals(modulePath, id);
        assertFalse(id.contains("."));
        assertEquals(modulePath, ConfigurationTreeRestController.getModulePathFromNodeId(id));
        assertNull(ConfigurationTreeRestController.getModulePathFromNodeId("invalid-node"));
        assertNull(ConfigurationTreeRestController.getModulePathFromNodeId(ConfigurationTreeRestController.MODULE_NODE_ID_PREFIX + "%%%"));
        assertNull(ConfigurationTreeRestController.getModulePathFromNodeId(ConfigurationTreeRestController.getModuleNodeId("security..oauth2")));
    }

    @Test
    void supportsAllTreeSearchModesWithoutCaseOrDiacritics() {
        assertTrue(ConfigurationTreeRestController.matchesSearch("Bezpečnosť", "bezpec", "startwith"));
        assertTrue(ConfigurationTreeRestController.matchesSearch("security.oauth2", "oauth2", "endwith"));
        assertTrue(ConfigurationTreeRestController.matchesSearch("OAuth2", "oauth2", "equals"));
        assertTrue(ConfigurationTreeRestController.matchesSearch("apps.gallery", "GALL", "contains"));
        assertFalse(ConfigurationTreeRestController.matchesSearch("apps.gallery", "security", "contains"));
    }
}
