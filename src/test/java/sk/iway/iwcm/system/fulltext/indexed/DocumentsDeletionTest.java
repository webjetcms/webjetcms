package sk.iway.iwcm.system.fulltext.indexed;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

import java.io.File;

import javax.servlet.ServletContext;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockServletContext;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.system.cluster.ClusterRefresher;
import sk.iway.iwcm.system.fulltext.lucene.LuceneUtils;

class DocumentsDeletionTest
{
    @BeforeAll
    static void initializeIndexPath()
    {
        ServletContext originalContext = Constants.getServletContext();
        if (originalContext == null)
        {
            Constants.setServletContext(new MockServletContext() {
                @Override
                public String getRealPath(String path)
                {
                    String basePath = System.getProperty("webjetTestBasepath", "./src/main/webapp/");
                    return new File(basePath, path).getAbsolutePath();
                }
            });
        }
        try
        {
            // Resolve the normal index path before mocking configuration; no index is opened.
            assertNotNull(LuceneUtils.LUCENE_INDEX);
        }
        finally
        {
            Constants.setServletContext(originalContext);
        }
    }

    @Test
    void deletesByIdWithoutLoadingDocumentOrGroup()
    {
        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<DocumentIndexCleaner> cleaner = mockStatic(DocumentIndexCleaner.class);
                MockedStatic<DocDB> documents = mockStatic(DocDB.class);
                MockedStatic<GroupsDB> groups = mockStatic(GroupsDB.class);
                MockedStatic<DBPool> database = mockStatic(DBPool.class))
        {
            constants.when(() -> Constants.getBoolean("luceneIncrementalDocumentIndexing")).thenReturn(true);
            cleaner.when(() -> DocumentIndexCleaner.delete(any(File.class), eq(42))).thenReturn(true);

            Documents.deleteSingleDocument(42);

            cleaner.verify(() -> DocumentIndexCleaner.delete(any(File.class), eq(42)));
            documents.verifyNoInteractions();
            groups.verifyNoInteractions();
            database.verifyNoInteractions();
        }
    }

    @Test
    void disabledIndexingSkipsDeletion()
    {
        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<DocumentIndexCleaner> cleaner = mockStatic(DocumentIndexCleaner.class);
                MockedStatic<DBPool> database = mockStatic(DBPool.class))
        {
            constants.when(() -> Constants.getBoolean("luceneIncrementalDocumentIndexing")).thenReturn(false);

            Documents.deleteSingleDocument(42);

            cleaner.verifyNoInteractions();
            database.verifyNoInteractions();
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void invalidIdsDoNotTouchIndexes(int docId)
    {
        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<DocumentIndexCleaner> cleaner = mockStatic(DocumentIndexCleaner.class))
        {
            constants.when(() -> Constants.getBoolean("luceneIncrementalDocumentIndexing")).thenReturn(true);
            Documents.deleteSingleDocument(docId);

            cleaner.verifyNoInteractions();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void failedDeletionPropagatesOnlyOnClusterRefresherThread(boolean clusterThread)
    {
        String originalThreadName = Thread.currentThread().getName();
        Thread.currentThread().setName(clusterThread ? ClusterRefresher.THREAD_NAME : "DocumentDeletionTestThread");
        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<DocumentIndexCleaner> cleaner = mockStatic(DocumentIndexCleaner.class))
        {
            constants.when(() -> Constants.getBoolean("luceneIncrementalDocumentIndexing")).thenReturn(true);
            cleaner.when(() -> DocumentIndexCleaner.delete(any(File.class), eq(42))).thenReturn(false);

            if (clusterThread)
            {
                assertThrows(IllegalStateException.class, () -> Documents.deleteSingleDocument(42));
            }
            else
            {
                assertDoesNotThrow(() -> Documents.deleteSingleDocument(42));
            }
            cleaner.verify(() -> DocumentIndexCleaner.delete(any(File.class), eq(42)));
        }
        finally
        {
            Thread.currentThread().setName(originalThreadName);
        }
    }

    @Test
    void unexpectedIndexFailurePropagatesOnClusterRefresherThread()
    {
        String originalThreadName = Thread.currentThread().getName();
        Thread.currentThread().setName(ClusterRefresher.THREAD_NAME);
        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<DocumentIndexCleaner> cleaner = mockStatic(DocumentIndexCleaner.class))
        {
            constants.when(() -> Constants.getBoolean("luceneIncrementalDocumentIndexing")).thenReturn(true);
            cleaner.when(() -> DocumentIndexCleaner.delete(any(File.class), eq(42)))
                    .thenThrow(new IllegalArgumentException("Simulated invalid index path"));

            assertThrows(IllegalStateException.class, () -> Documents.deleteSingleDocument(42));
        }
        finally
        {
            Thread.currentThread().setName(originalThreadName);
        }
    }
}
