package sk.iway.iwcm.rag.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.rag.jpa.IndexQueueEntity;
import sk.iway.iwcm.rag.jpa.IndexQueueRepository;
import sk.iway.iwcm.rag.pgvector.EmbeddingChunkRepository;
import sk.iway.iwcm.rag.pgvector.RagJpaConfig;
import sk.iway.iwcm.rag.service.SemanticIndexService;
import sk.iway.iwcm.rag.vectorstore.PgVectorStore;

/**
 * REST controller for RAG administration operations.
 * Provides endpoints for reindexing documents, checking status, and managing the vector store.
 */
@RestController
@PreAuthorize("@WebjetSecurityService.isAdmin()")
public class RagAdminRestController {

    private final SemanticIndexService indexService;
    private final PgVectorStore vectorStore;
    private final IndexQueueRepository queueRepository;
    private final EmbeddingChunkRepository chunkRepository;

    @Autowired
    public RagAdminRestController(SemanticIndexService indexService,
                                   PgVectorStore vectorStore,
                                   IndexQueueRepository queueRepository,
                                   EmbeddingChunkRepository chunkRepository) {
        this.indexService = indexService;
        this.vectorStore = vectorStore;
        this.queueRepository = queueRepository;
        this.chunkRepository = chunkRepository;
    }

    /**
     * Get RAG system status: availability, queue size, chunk counts.
     */
    @GetMapping("/admin/rest/rag/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", Constants.getBoolean("ragSemanticSearchEnabled"));
        status.put("available", RagJpaConfig.isRagAvailable());
        status.put("vectorStoreAvailable", vectorStore.isAvailable());
        status.put("datasource", RagJpaConfig.getRagDataSourceName());

        // Queue stats
        List<IndexQueueEntity> pendingItems = queueRepository.findTop100ByOrderByCreateDateAsc();
        status.put("queueSize", pendingItems.size());

        // Chunk stats (only if vector store is available)
        if (vectorStore.isAvailable()) {
            status.put("totalChunks", chunkRepository.count());
            status.put("activeChunks", chunkRepository.countByStatus("active"));
            status.put("errorChunks", chunkRepository.countByStatus("error"));
        }

        return status;
    }

    /**
     * Reindex a single document by ID.
     */
    @PostMapping("/admin/rest/rag/reindex")
    public Map<String, Object> reindexDocument(@RequestParam("docId") int docId) {
        Map<String, Object> result = new HashMap<>();

        if (RagJpaConfig.isRagAvailable() == false) {
            result.put("success", false);
            result.put("error", "RAG is not available");
            return result;
        }

        try {
            indexService.indexDocument(docId);
            result.put("success", true);
            result.put("docId", docId);
        } catch (Exception e) {
            Logger.error(RagAdminRestController.class, "Error reindexing document " + docId, e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Queue all documents in a group (folder) for reindexing.
     */
    @PostMapping("/admin/rest/rag/reindex-group")
    public Map<String, Object> reindexGroup(@RequestParam("groupId") int groupId) {
        Map<String, Object> result = new HashMap<>();

        if (RagJpaConfig.isRagAvailable() == false) {
            result.put("success", false);
            result.put("error", "RAG is not available");
            return result;
        }

        try {
            int domainId = CloudToolsForCore.getDomainId();
            GroupDetails group = GroupsDB.getInstance().getGroup(groupId);
            if (group == null) {
                result.put("success", false);
                result.put("error", "Group not found: " + groupId);
                return result;
            }

            List<DocDetails> docs = DocDB.getInstance().getDocByGroup(groupId);
            int queued = 0;
            for (DocDetails doc : docs) {
                try {
                    IndexQueueEntity queueItem = new IndexQueueEntity();
                    queueItem.setEntityType("document");
                    queueItem.setEntityId(doc.getDocId());
                    queueItem.setAction("reindex");
                    queueItem.setDomainId(domainId);
                    queueItem.setCreateDate(new java.util.Date());
                    queueRepository.save(queueItem);
                    queued++;
                } catch (Exception e) {
                    // Duplicate entry - already queued, ignore
                    Logger.debug(RagAdminRestController.class, "Document " + doc.getDocId() + " already in queue");
                }
            }

            result.put("success", true);
            result.put("groupId", groupId);
            result.put("documentsQueued", queued);
        } catch (Exception e) {
            Logger.error(RagAdminRestController.class, "Error queueing group " + groupId, e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Initialize the vector store schema (creates pgvector extension, table, indexes).
     */
    @PostMapping("/admin/rest/rag/init-schema")
    public Map<String, Object> initSchema() {
        Map<String, Object> result = new HashMap<>();

        String dsName = RagJpaConfig.getRagDataSourceName();
        if (dsName == null) {
            result.put("success", false);
            result.put("error", "No pgvector-capable datasource configured");
            return result;
        }

        try {
            vectorStore.initializeSchema();
            result.put("success", true);
        } catch (Exception e) {
            Logger.error(RagAdminRestController.class, "Error initializing schema", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }
}
