package sk.iway.css;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Covers additive style metadata while preserving legacy selectors and titles. */
class CssParserTest {
    @Test
    void readsAllClientStyles() throws java.io.IOException {
        try (var input = getClass().getResourceAsStream("/css/editor-styles.css")) {
            assertNotNull(input);
            var items = new CssParser(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).getElements();
            assertEquals(95, items.size(), "The complete client catalog must survive parsing");
            assertTrue(items.stream().allMatch(item -> item.getCategory() != null));
            assertEquals("list-square", items.get(0).getClassName());
            assertTrue(items.stream().anyMatch(item -> "Perex".equals(item.getTitle())));
        }
    }

    @Test
    void readsMetadataWithoutMistakingItForATitle() {
        CssDto item = new CssParser(".large { /* editor category: Typography */ /* editor group: Size */ "
                + "/* editor targets: p h1 h2 */ /* editor members: medium compact */ }").getElements().get(0);
        assertNull(item.getTitle(), "Metadata must not become a title");
        assertEquals("Typography", item.getCategory());
        assertEquals("Size", item.getGroup());
        assertEquals("p h1 h2", item.getTargets());
        assertEquals("medium compact", item.getMembers());
    }

    @Test
    void preservesHtmlTitlesAndCompoundClasses() {
        CssDto item = new CssParser("div.alert.warning {\n/* editor title: <b>Warning</b> */\n"
                + "/* editor base: alert */\n/* editor requires: alert */\n}").getElements().get(0);
        assertEquals("div", item.getTag());
        assertEquals("alert warning", item.getClassName());
        assertEquals("<b>Warning</b>", item.getTitle());
        assertEquals("alert", item.getBase());
        assertEquals("alert", item.getRequires());
    }

    @Test
    void keepsTheFirstRuleAfterCharsetAndLegacyInlineTitles() {
        var items = new CssParser("@charset \"UTF-8\";\nul.square { /* Square */ }\n.small { /* Small */ /* editor group: Size */ }").getElements();
        assertEquals(2, items.size());
        assertEquals("square", items.get(0).getClassName());
        assertEquals("Square", items.get(0).getTitle());
        assertEquals("*", items.get(1).getTag());
        assertEquals("Small", items.get(1).getTitle(), "Adding metadata must preserve a legacy title");
        assertEquals("Size", items.get(1).getGroup());
    }

    @Test
    void readsExplicitMetadataAfterMediaRulesWithoutDuplicatingEarlierStyles() {
        var items = new CssParser(".first { /* editor title: First */ } "
                + "@media (min-width: 800px) { .second {\n/* editor title: Second */\n"
                + "/* editor category: Typography */\n/* editor group: Size */\n} }").getElements();
        assertEquals(2, items.size());
        assertEquals("second", items.get(1).getClassName());
        assertEquals("Second", items.get(1).getTitle());
        assertEquals("Typography", items.get(1).getCategory());
        assertEquals("Size", items.get(1).getGroup());
    }
}
