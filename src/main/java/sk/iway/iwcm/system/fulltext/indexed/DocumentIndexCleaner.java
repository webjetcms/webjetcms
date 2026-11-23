package sk.iway.iwcm.system.fulltext.indexed;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.index.IndexFileNameFilter;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.system.fulltext.lucene.IndexSearcherBuilder;

/** Deletes page references without relying on document or language caches. */
final class DocumentIndexCleaner {

    private DocumentIndexCleaner() {
    }

    static boolean delete(File root, int docId) {
        if (docId <= 0) return true;
        try {
            if (Files.readAttributes(root.toPath(), BasicFileAttributes.class).isDirectory() == false) {
                throw new IOException("Lucene index root is not a directory: " + root);
            }
        } catch (NoSuchFileException ex) {
            return true;
        } catch (IOException | RuntimeException ex) {
            Logger.error(DocumentIndexCleaner.class, "Cannot inspect Lucene index root: " + root, ex);
            return false;
        }

        boolean success = true;
        try (DirectoryStream<Path> children = Files.newDirectoryStream(root.toPath())) {
            for (Path child : children) {
                try {
                    if (Files.readAttributes(child, BasicFileAttributes.class).isDirectory() == false) continue;
                    try (Directory directory = new SimpleFSDirectory(child.toFile())) {
                        if (containsIndexFiles(directory) == false) continue;
                        deleteFromIndex(directory, docId);
                    }
                } catch (IOException | RuntimeException ex) {
                    success = false;
                    Logger.error(DocumentIndexCleaner.class, "Cannot clean Lucene index: " + child, ex);
                }
            }
        } catch (IOException | RuntimeException ex) {
            Logger.error(DocumentIndexCleaner.class, "Cannot enumerate Lucene indexes: " + root, ex);
            return false;
        }
        return success;
    }

    private static boolean containsIndexFiles(Directory directory) throws IOException {
        // indexExists() suppresses I/O errors, including corrupt commit files.
        for (String name : directory.listAll()) {
            if (IndexFileNameFilter.getFilter().accept(null, name)) return true;
        }
        return false;
    }

    private static void deleteFromIndex(Directory directory, int docId) throws IOException {
        boolean deletionAttempted = false;
        try (Analyzer analyzer = new KeywordAnalyzer();
                IndexWriter writer = new IndexWriter(directory,
                        new IndexWriterConfig(Version.LUCENE_31, analyzer).setOpenMode(OpenMode.APPEND))) {
            deletionAttempted = true;
            writer.deleteDocuments(new Term("doc_id", Integer.toString(docId)));
            writer.commit();
        } finally {
            // close() may commit pending deletions even after an earlier operation fails.
            if (deletionAttempted) IndexSearcherBuilder.refresh();
        }
    }
}
