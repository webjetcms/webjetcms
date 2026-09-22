package sk.iway.iwcm.doc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mockStatic;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.common.CloudToolsForCore;

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

    @Test
    void getByIdCachedDoesNotShareTemplateGroupsBetweenDomains() {
        AtomicInteger currentDomainId = new AtomicInteger(1);
        TemplatesGroupBean firstDomainTemplateGroup = new TemplatesGroupBean();
        firstDomainTemplateGroup.setId(12L);
        TestTemplatesGroupDB database = new TestTemplatesGroupDB(firstDomainTemplateGroup);

        try (MockedStatic<InitServlet> initServlet = mockStatic(InitServlet.class);
                MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class)) {
            initServlet.when(InitServlet::isTypeCloud).thenReturn(true);
            constants.when(() -> Constants.getString("jpaFilterByDomainIdBeanList"))
                    .thenReturn(TemplatesGroupBean.class.getName());
            cloudTools.when(CloudToolsForCore::getDomainId).thenAnswer(invocation -> currentDomainId.get());

            assertSame(firstDomainTemplateGroup, database.getByIdCached(12L));

            currentDomainId.set(2);
            database.setResult(null);
            assertNull(database.getByIdCached(12L));

            currentDomainId.set(1);
            assertSame(firstDomainTemplateGroup, database.getByIdCached(12L));
            assertEquals(2, database.getDatabaseReadCount());
        }
    }

    private static class TestTemplatesGroupDB extends TemplatesGroupDB {

        private TemplatesGroupBean result;
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

        private void setResult(TemplatesGroupBean result) {
            this.result = result;
        }
    }
}
