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
        CssDto item = new CssParser(".large { /* editor category: Typography */ /* editor category-icon: ti-typography  */ /* editor group: Size */ "
                + "/* editor targets: p h1 h2 */ /* editor members: medium compact */ }").getElements().get(0);
        assertNull(item.getTitle(), "Metadata must not become a title");
        assertEquals("Typography", item.getCategory());
        assertEquals("ti-typography", item.getCategoryIcon());
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
        assertNull(items.get(1).getCategoryIcon(), "Category icons remain optional");
    }

    @Test
    void readsExplicitMetadataAfterMediaRulesWithoutDuplicatingEarlierStyles() {
        var items = new CssParser(".first { /* editor title: First */ } "
                + "@media (min-width: 800px) { .second {\n/* editor title: Second */\n"
                + "/* editor category: Typography */\n/* editor category-icon: ti-letter-case */\n/* editor group: Size */\n} }").getElements();
        assertEquals(2, items.size());
        assertEquals("second", items.get(1).getClassName());
        assertEquals("Second", items.get(1).getTitle());
        assertEquals("Typography", items.get(1).getCategory());
        assertEquals("ti-letter-case", items.get(1).getCategoryIcon());
        assertEquals("Size", items.get(1).getGroup());
    }

    @Test
    void keepsFallbackMetadataInSourceOrder() {
        var items = new CssParser("/* Catalog heading */\n.early { /* editor category: Borders */ /* editor category-icon: ti-border-none */ }\n"
                + ".later { /* editor category: Borders */ /* editor category-icon: ti-border-all */ }\n"
                + "@media (min-width: 800px) { .media { /* editor category: Borders */ /* editor category-icon: ti-border-top */ } }\n"
                + ".after { /* editor category: Borders */ /* editor category-icon: ti-border-bottom */ }").getElements();
        assertEquals(java.util.List.of("early", "later", "media", "after"), items.stream().map(CssDto::getClassName).toList(),
                "Fallback rules must retain their position before and after media rules");
        assertEquals(java.util.List.of("ti-border-none", "ti-border-all", "ti-border-top", "ti-border-bottom"),
                items.stream().map(CssDto::getCategoryIcon).toList(), "The first explicit category icon must remain first in source order");
    }

    @Test
    void keepsEarlierFallbackMetadataWhenTheSelectorWasAlreadyParsed() {
        var items = new CssParser("/* Catalog heading */\n.border { /* editor title: First */ /* editor category-icon: ti-border-none */ }\n"
                + ".border { /* editor title: Later */ /* editor category-icon: ti-border-all */ }\n"
                + "/** Editor end **/\n.after { /* editor title: After */ }").getElements();
        assertEquals(2, items.size(), "The fallback must not duplicate an already parsed selector");
        assertEquals("border", items.get(0).getClassName());
        assertEquals("First", items.get(0).getTitle(), "The earlier explicit definition must take precedence over its later duplicate");
        assertEquals("ti-border-none", items.get(0).getCategoryIcon());
        assertEquals("after", items.get(1).getClassName());
    }
}
