package sk.iway.iwcm.components.translation_keys.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.admin.jstree.JsTreeItem;

class TranslationKeyTreeRestControllerTest {

    private static final Set<String> KEYS = Set.of(
        "components.map.settings",
        "components.map.width.short",
        "editor.field_K.warningText",
        "standalone"
    );

    @Test
    void createsOnlyDirectChildrenForSelectedPrefix() {
        List<JsTreeItem> rootItems = TranslationKeyTreeRestController.getItems(KEYS, "");

        assertEquals(List.of("components", "editor"), rootItems.stream().map(JsTreeItem::getId).toList());
        assertTrue(rootItems.get(0).getChildren());
        assertTrue(rootItems.get(1).getChildren());
        assertEquals(List.of("ti ti-folder-filled", "ti ti-folder-filled"), rootItems.stream().map(JsTreeItem::getIcon).toList());

        List<JsTreeItem> mapItems = TranslationKeyTreeRestController.getItems(KEYS, "components.map");

        assertEquals(List.of("components.map.width"), mapItems.stream().map(JsTreeItem::getId).toList());
        assertFalse(mapItems.get(0).getChildren());
        assertEquals("ti ti-folder-filled", mapItems.get(0).getIcon());
        assertEquals("width", mapItems.get(0).getText());
    }

    @Test
    void filtersPrefixAtSegmentBoundary() {
        assertTrue(TranslationKeyService.isKeyInPrefix("components.map", "components.map"));
        assertTrue(TranslationKeyService.isKeyInPrefix("components.map.width.short", "components.map"));
        assertFalse(TranslationKeyService.isKeyInPrefix("components.mapping", "components.map"));
        assertTrue(TranslationKeyService.isKeyInPrefix("components.mapping", ""));
    }
}
