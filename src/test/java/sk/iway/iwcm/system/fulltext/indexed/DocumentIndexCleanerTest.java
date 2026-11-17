package sk.iway.iwcm.system.fulltext.indexed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DocumentIndexCleanerTest {

    @TempDir
    Path root;

    @Test
    void deletesPageAndForumReferencesFromEveryLanguageWithoutDocumentMetadata() throws IOException {
        createIndex("sk", document(10, "documents"), document(10, "forums"), document(20, "documents"));
        createIndex("en", document(10, "documents"), document(30, "documents"));

        assertTrue(DocumentIndexCleaner.delete(root.toFile(), 10));
        assertEquals(0, count("sk", 10));
        assertEquals(0, count("en", 10));
        assertEquals(1, count("sk", 20));
        assertEquals(1, count("en", 30));
        assertTrue(DocumentIndexCleaner.delete(root.toFile(), 10));
    }

    @Test
    void skipsAbsentIndexesAndDictionaryDirectoriesWithoutCreatingAnIndex() throws IOException {
        Path missingRoot = root.resolve("missing");
        assertTrue(DocumentIndexCleaner.delete(missingRoot.toFile(), 10));
        assertFalse(Files.exists(missingRoot));

        Files.createDirectories(root.resolve("lemmas"));
        Files.write(root.resolve("lemmas/sk.cdb"), new byte[] { 1, 2 });
        Files.createDirectories(root.resolve("synonyms"));
        Files.createDirectories(root.resolve("empty"));
        assertTrue(DocumentIndexCleaner.delete(root.toFile(), 10));
        try (java.util.stream.Stream<Path> files = Files.walk(root)) {
            assertEquals(0, files.filter(path -> path.getFileName().toString().startsWith("segments")).count());
        }
    }

    @Test
    void reportsLockedIndexAndContinuesOtherLanguagesUntilRetrySucceeds() throws IOException {
        createIndex("sk", document(10, "documents"));
        createIndex("en", document(10, "documents"));
        try (Directory directory = new SimpleFSDirectory(root.resolve("sk").toFile());
                Analyzer analyzer = new KeywordAnalyzer();
                IndexWriter writer = new IndexWriter(directory, config(analyzer))) {
            assertFalse(DocumentIndexCleaner.delete(root.toFile(), 10));
            assertEquals(0, count("en", 10));
            assertEquals(1, count("sk", 10));
        }

        assertTrue(DocumentIndexCleaner.delete(root.toFile(), 10));
        assertEquals(0, count("sk", 10));
    }

    @Test
    void reportsCorruptIndexWithoutOverwritingItAndContinuesOtherLanguages() throws IOException {
        createIndex("en", document(10, "documents"));
        Path corrupt = Files.createDirectories(root.resolve("sk")).resolve("segments_1");
        Files.write(corrupt, new byte[] { 1, 2, 3 });

        assertFalse(DocumentIndexCleaner.delete(root.toFile(), 10));
        assertEquals(3, Files.size(corrupt));
        assertEquals(0, count("en", 10));
    }

    private void createIndex(String language, Document... documents) throws IOException {
        Files.createDirectories(root.resolve(language));
        try (Directory directory = new SimpleFSDirectory(root.resolve(language).toFile());
                Analyzer analyzer = new KeywordAnalyzer();
                IndexWriter writer = new IndexWriter(directory, config(analyzer).setOpenMode(OpenMode.CREATE))) {
            for (Document document : documents) writer.addDocument(document);
        }
    }

    private static IndexWriterConfig config(Analyzer analyzer) {
        return new IndexWriterConfig(Version.LUCENE_31, analyzer).setOpenMode(OpenMode.APPEND);
    }

    private static Document document(int docId, String type) {
        Document document = new Document();
        document.add(new Field("doc_id", Integer.toString(docId), Field.Store.YES, Field.Index.ANALYZED));
        document.add(new Field("type", type, Field.Store.YES, Field.Index.ANALYZED));
        return document;
    }

    private int count(String language, int docId) throws IOException {
        try (Directory directory = new SimpleFSDirectory(root.resolve(language).toFile());
                IndexReader reader = IndexReader.open(directory, true)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            return searcher.search(new TermQuery(new Term("doc_id", Integer.toString(docId))), 10).totalHits;
        }
    }
}
