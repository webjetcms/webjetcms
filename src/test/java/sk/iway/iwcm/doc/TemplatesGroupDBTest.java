package sk.iway.iwcm.doc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class TemplatesGroupDBTest {

    @Test
    void getByIdCachedLoadsTemplateGroupOnlyOnce() {
        TemplatesGroupBean templateGroup = new TemplatesGroupBean();
        templateGroup.setId(12L);
        TestTemplatesGroupDB database = new TestTemplatesGroupDB(templateGroup);

        assertSame(templateGroup, database.getByIdCached(12L));
        assertSame(templateGroup, database.getByIdCached(12L));
        assertEquals(1, database.getDatabaseReadCount());
    }

    @Test
    void clearCacheForcesTemplateGroupReload() {
        TemplatesGroupBean templateGroup = new TemplatesGroupBean();
        templateGroup.setId(12L);
        TestTemplatesGroupDB database = new TestTemplatesGroupDB(templateGroup);

        database.getByIdCached(12L);
        database.clearCache();
        database.getByIdCached(12L);

        assertEquals(2, database.getDatabaseReadCount());
    }

    @Test
    void getByIdCachedDoesNotCacheMissingTemplateGroup() {
        TestTemplatesGroupDB database = new TestTemplatesGroupDB(null);

        assertNull(database.getByIdCached(12L));
        assertNull(database.getByIdCached(12L));
        assertEquals(2, database.getDatabaseReadCount());
    }

    private static class TestTemplatesGroupDB extends TemplatesGroupDB {

        private final TemplatesGroupBean result;
        private int databaseReadCount;

        private TestTemplatesGroupDB(TemplatesGroupBean result) {
            this.result = result;
        }

        @Override
        public TemplatesGroupBean getById(Long id) {
            databaseReadCount++;
            return result;
        }

        private int getDatabaseReadCount() {
            return databaseReadCount;
        }
    }
}
