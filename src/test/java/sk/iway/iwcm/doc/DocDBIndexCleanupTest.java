package sk.iway.iwcm.doc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import gnu.trove.TIntObjectHashMap;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.system.cluster.ClusterRefresher;
import sk.iway.iwcm.system.fulltext.indexed.Documents;

class DocDBIndexCleanupTest
{
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void removesIndexEntryWithoutCachedDocumentOrFolder(boolean cachedDocumentExists) throws Exception
    {
        int docId = 42;
        DocDB docDB = mock(DocDB.class, CALLS_REAL_METHODS);
        TIntObjectHashMap<DocDetails> basicDocuments = new TIntObjectHashMap<>();
        HashMap<Integer, DocDetails> cachedDocuments = new HashMap<>();
        if (cachedDocumentExists)
        {
            DocDetails document = mock(DocDetails.class);
            when(document.getDocId()).thenReturn(docId);
            when(document.getGroupId()).thenReturn(7);
            basicDocuments.put(docId, document);
            cachedDocuments.put(docId, document);
        }
        ReflectionTestUtils.setField(docDB, "basicAllDocsTable", basicDocuments);
        ReflectionTestUtils.setField(docDB, "basicAllDocsInGroupTable", new TIntObjectHashMap<List<DocDetails>>());
        ReflectionTestUtils.setField(docDB, "cachedDocs", cachedDocuments);
        doNothing().when(docDB).readPagesToPublic();

        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        Cache cache = mock(Cache.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        try (MockedStatic<DBPool> database = mockStatic(DBPool.class);
                MockedStatic<DocDB> documentInstances = mockStatic(DocDB.class);
                MockedStatic<Cache> caches = mockStatic(Cache.class);
                MockedStatic<ClusterDB> cluster = mockStatic(ClusterDB.class);
                MockedStatic<Documents> index = mockStatic(Documents.class))
        {
            database.when(DBPool::getConnection).thenReturn(connection);
            documentInstances.when(DocDB::getInstance).thenReturn(docDB);
            caches.when(Cache::getInstance).thenReturn(cache);
            caches.when(() -> Cache.getInstance(true)).thenReturn(cache);

            docDB.updateInternalCaches(docId);

            index.verify(() -> Documents.deleteSingleDocument(docId));
            cluster.verify(() -> ClusterDB.addRefresh(DocDB.class.getName() + "-" + docId));
            assertTrue(basicDocuments.isEmpty());
            assertTrue(cachedDocuments.isEmpty());
            verify(statement).setInt(1, docId);
            verify(connection).close();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void indexFailureWithoutCachedDocumentPropagatesOnlyToClusterRefresher(boolean clusterThread) throws Exception
    {
        int docId = 42;
        DocDB docDB = mock(DocDB.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(docDB, "basicAllDocsTable", new TIntObjectHashMap<DocDetails>());
        ReflectionTestUtils.setField(docDB, "cachedDocs", new HashMap<Integer, DocDetails>());

        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        Cache cache = mock(Cache.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        String originalThreadName = Thread.currentThread().getName();
        Thread.currentThread().setName(clusterThread ? ClusterRefresher.THREAD_NAME : "DocumentCacheCleanupTestThread");
        try (MockedStatic<DBPool> database = mockStatic(DBPool.class);
                MockedStatic<Cache> caches = mockStatic(Cache.class);
                MockedStatic<ClusterDB> cluster = mockStatic(ClusterDB.class);
                MockedStatic<Documents> index = mockStatic(Documents.class))
        {
            database.when(DBPool::getConnection).thenReturn(connection);
            caches.when(Cache::getInstance).thenReturn(cache);
            caches.when(() -> Cache.getInstance(true)).thenReturn(cache);
            index.when(() -> Documents.deleteSingleDocument(docId))
                    .thenThrow(new IllegalStateException("Simulated Lucene write failure"));

            if (clusterThread)
            {
                assertThrows(IllegalStateException.class, () -> docDB.updateInternalCaches(docId));
            }
            else
            {
                assertDoesNotThrow(() -> docDB.updateInternalCaches(docId));
            }

            index.verify(() -> Documents.deleteSingleDocument(docId));
            verify(connection).close();
        }
        finally
        {
            Thread.currentThread().setName(originalThreadName);
        }
    }
}
