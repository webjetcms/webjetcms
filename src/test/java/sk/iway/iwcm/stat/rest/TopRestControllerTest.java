package sk.iway.iwcm.stat.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.stat.Column;

/** Verifies that the shared TOP endpoint uses supplied page metadata without database reads. */
class TopRestControllerTest {
    /** Preserves cached images, titles containing slashes and the original statistics. */
    @Test
    void mapsCachedPageMetadataWithoutDatabaseReads() {
        var first = new Column();
        first.setColumn1("12");
        first.setColumn2("News / Updates");
        first.setColumn6("/Section/News / Updates");
        first.setColumn8("/images/news/cover.jpg");
        first.setIntColumn3(10);
        first.setIntColumn4(4);
        first.setIntColumn5(2);
        var missing = new Column();
        missing.setColumn1("13");
        missing.setColumn8("");
        try (var database = mockStatic(DBPool.class)) {
            var items = new TopRestController().columnsToPageItems(List.of(first, missing));
            assertEquals("News / Updates", items.get(0).getTitle());
            assertEquals("/Section/News / Updates", items.get(0).getName());
            assertEquals("/images/news/cover.jpg", items.get(0).getPerexImage());
            assertEquals(10, items.get(0).getVisits());
            assertEquals(4, items.get(0).getSessions());
            assertEquals(2, items.get(0).getUniqueUsers());
            assertEquals("", items.get(1).getPerexImage());
            database.verifyNoInteractions();
        }
    }
}
