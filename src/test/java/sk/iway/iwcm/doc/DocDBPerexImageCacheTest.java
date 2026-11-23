package sk.iway.iwcm.doc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.HashMap;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import gnu.trove.TIntObjectHashMap;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.system.fulltext.indexed.Documents;
import sk.iway.iwcm.system.spring.events.WebjetEventPublisher;

/** Covers image metadata loading and refresh in the existing basic document cache. */
class DocDBPerexImageCacheTest {
    /** Loading, replacing and removing an image must be reflected by the same cached page. */
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "/images/updated.jpg", "" })
    void loadsAndRefreshesPerexImage(String updatedImage) throws Exception {
        var connection = mock(Connection.class);
        var statement = mock(PreparedStatement.class);
        var rows = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(rows);
        when(rows.next()).thenReturn(true, false);
        when(rows.getInt("doc_id")).thenReturn(12);
        when(rows.getInt("group_id")).thenReturn(10);
        when(rows.getString("title")).thenReturn("autotest cached page");
        when(rows.getString("virtual_path")).thenReturn("/autotest.html");
        when(rows.getString("perex_image")).thenReturn("/images/original.jpg");
        when(rows.getTimestamp("date_created")).thenReturn(new Timestamp(12345));
        var documents = mock(DocDB.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(documents, "basicAllDocsTable", new TIntObjectHashMap<DocDetails>());
        ReflectionTestUtils.setField(documents, "cachedDocs", new HashMap<Integer, DocDetails>());
        doNothing().when(documents).readPagesToPublic();

        try (var database = mockStatic(DBPool.class); var groups = mockStatic(GroupsDB.class);
             var events = mockStatic(WebjetEventPublisher.class); var cluster = mockStatic(ClusterDB.class);
             var cache = mockStatic(Cache.class); var index = mockStatic(Documents.class)) {
            database.when(DBPool::getConnection).thenReturn(connection);
            groups.when(GroupsDB::getInstance).thenReturn(mock(GroupsDB.class));
            cache.when(Cache::getInstance).thenReturn(mock(Cache.class));
            assertNotNull(ReflectionTestUtils.invokeMethod(documents, "getDocForUrls", true));
            var page = documents.getBasicDocDetails(12, false);
            assertNotNull(page);
            assertEquals("/images/original.jpg", page.getPerexImage());

            when(rows.next()).thenReturn(true, false);
            when(rows.getString("perex_image")).thenReturn(updatedImage);
            documents.updateInternalCaches(12);
            assertSame(page, documents.getBasicDocDetails(12, false));
            assertEquals(updatedImage == null ? "" : updatedImage, page.getPerexImage());
            assertEquals("", documents.getBasicDocDetails(99, true).getPerexImage());
            var sql = ArgumentCaptor.forClass(String.class);
            verify(connection, times(2)).prepareStatement(sql.capture());
            assertTrue(sql.getAllValues().stream().allMatch(query -> query.contains("perex_image")));
            index.verify(() -> Documents.updateSingleDocument(12));
        }
    }
}
