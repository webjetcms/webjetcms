package sk.iway.iwcm.system.fulltext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import net.sourceforge.stripes.controller.StripesFilterIway;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.system.fulltext.indexed.Documents;

class FulltextRebuildTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void keepsExistingDocumentsWhenStripesIsUnavailable(boolean rebuildAll, @TempDir Path indexPath) throws IOException {
        Documents documents = mock(Documents.class);
        when(documents.language()).thenReturn("en");
        when(documents.name()).thenReturn("documents");

        try (var directory = new SimpleFSDirectory(indexPath.toFile());
             var analyzer = new KeywordAnalyzer();
             var constants = mockStatic(Constants.class);
             var caches = mockStatic(Cache.class);
             var stripes = mockStatic(StripesFilterIway.class)) {
            Cache cache = mock(Cache.class);
            caches.when(Cache::getInstance).thenReturn(cache);
            when(cache.getObject("FulltextSearch.getIndexDirectory.en")).thenReturn(directory);
            constants.when(() -> Constants.getBoolean("fulltextExecuteApps")).thenReturn(true);

            try (var writer = new IndexWriter(directory, new IndexWriterConfig(Version.LUCENE_31, analyzer))) {
                Document page = new Document();
                page.add(new Field("type", "documents", Field.Store.NO, Field.Index.NOT_ANALYZED));
                page.add(new Field("doc_id", "42", Field.Store.NO, Field.Index.NOT_ANALYZED));
                writer.addDocument(page);
            }

            assertThrows(IllegalStateException.class, () -> FulltextSearch.index(rebuildAll ? null : documents, null));
            stripes.verify(StripesFilterIway::ensureInitialized);
            try (IndexReader reader = IndexReader.open(directory, true)) {
                var searcher = new IndexSearcher(reader);
                assertEquals(1, searcher.search(new TermQuery(new Term("doc_id", "42")), 1).totalHits);
            }
        }
    }
}
