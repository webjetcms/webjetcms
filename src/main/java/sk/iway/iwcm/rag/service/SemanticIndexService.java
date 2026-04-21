package sk.iway.iwcm.rag.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.components.structuremirroring.GroupMirroringServiceV9;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.rag.embedding.EmbeddingProvider;
import sk.iway.iwcm.rag.indexing.DocDetailsContentExtractor;
import sk.iway.iwcm.rag.indexing.SlidingWindowChunker;
import sk.iway.iwcm.rag.vectorstore.PgVectorStore;

/**
 * Core service for semantic indexing of documents.
 * Orchestrates: content extraction -> chunking -> embedding -> vector storage.
 */
@Service
public class SemanticIndexService {

    private final DocDetailsContentExtractor contentExtractor;
    private final SlidingWindowChunker chunker;
    private final EmbeddingProvider embeddingProvider;
    private final PgVectorStore vectorStore;

    @Autowired
    public SemanticIndexService(DocDetailsContentExtractor contentExtractor,
                                SlidingWindowChunker chunker,
                                EmbeddingProvider embeddingProvider,
                                PgVectorStore vectorStore) {
        this.contentExtractor = contentExtractor;
        this.chunker = chunker;
        this.embeddingProvider = embeddingProvider;
        this.vectorStore = vectorStore;
    }

    /**
     * Index a document: extract text, chunk, embed, and store vectors.
     * @param docId the document ID to index
     */
    public void indexDocument(int docId) {
        DocDetails doc = DocDB.getInstance().getDoc(docId);
        if (doc == null) {
            Logger.debug(SemanticIndexService.class, "Document " + docId + " not found, skipping indexing");
            return;
        }

        indexDocument(doc);
    }

    /**
     * Index a document from a DocDetails object.
     */
    public void indexDocument(DocDetails doc) {
        String entityType = DocDetailsContentExtractor.ENTITY_TYPE;
        long entityId = doc.getDocId();

        try {
            // Step 1: Extract text
            String text = contentExtractor.extractText(doc);
            if (text == null || text.isBlank()) {
                // Empty document, remove any existing embeddings
                vectorStore.deleteByEntity(entityType, entityId);
                return;
            }

            // Step 2: Chunk
            List<String> chunks = chunker.chunk(text);
            if (chunks.isEmpty()) {
                vectorStore.deleteByEntity(entityType, entityId);
                return;
            }

            String model = embeddingProvider.getDefaultModel();
            int dimensions = embeddingProvider.getDimensions(model);

            // Step 3: Embed all chunks in a batch
            List<float[]> embeddings = embeddingProvider.embed(chunks, model);
            if (embeddings.size() != chunks.size()) {
                Logger.error(SemanticIndexService.class, "Embedding count mismatch for doc " + entityId +
                    ": expected " + chunks.size() + ", got " + embeddings.size());
                return;
            }

            // Step 4: Delete old chunks and store new ones
            vectorStore.deleteByEntity(entityType, entityId);

            String language = GroupMirroringServiceV9.getLanguage( doc.getGroup() );

            int docId = doc.getDocId();
            String domainName = DocDB.getInstance().getDomain(docId);
            int domainId = GroupsDB.getDomainId(domainName);

            for (int i = 0; i < chunks.size(); i++) {
                String chunkText = chunks.get(i);
                String contentHash = sha256(chunkText);
                float[] embedding = embeddings.get(i);

                vectorStore.store(entityType, entityId, i, chunkText, contentHash,
                    embedding, model, dimensions, language, domainId);
            }

            Logger.debug(SemanticIndexService.class, "Indexed doc " + entityId + " with " + chunks.size() + " chunks");
        } catch (Exception e) {
            Logger.error(SemanticIndexService.class, "Error indexing doc " + entityId + ": " + e.getMessage());
        }
    }

    /**
     * Remove all embeddings for a document.
     */
    public void deleteDocument(int docId) {
        vectorStore.deleteByEntity(DocDetailsContentExtractor.ENTITY_TYPE, docId);
    }

    /**
     * Compute SHA-256 hash of text for deduplication.
     */
    private static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(text.hashCode());
        }
    }
}
