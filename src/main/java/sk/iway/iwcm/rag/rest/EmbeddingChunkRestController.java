package sk.iway.iwcm.rag.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.rag.RagIndexAction;
import sk.iway.iwcm.rag.pgvector.EmbeddingChunkEntity;
import sk.iway.iwcm.rag.pgvector.EmbeddingChunkRepository;
import sk.iway.iwcm.rag.pgvector.EmbeddingChunkStatus;
import sk.iway.iwcm.rag.service.IndexQueueService;
import sk.iway.iwcm.rag.service.RagEntityType;
import sk.iway.iwcm.rag.vectorstore.PgVectorStore;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.utils.Pair;

/**
 * REST controller for managing RAG embedding chunks via the admin datatable interface.
 * Provides CRUD operations, document indexing actions, and statistics endpoints.
 * Requires 'embeddingChunks' permission.
 */
@RestController
@RequestMapping("/admin/rest/settings/embedding-chunks")
@PreAuthorize("@WebjetSecurityService.hasPermission('embeddingChunks')")
@Datatable
public class EmbeddingChunkRestController extends DatatableRestControllerV2<EmbeddingChunkEntity, Long> {

    private final EmbeddingChunkRepository chunkRepository;
    private final IndexQueueService indexQueueService;

    private final PgVectorStore vectorStore;

    @Autowired
    public EmbeddingChunkRestController(EmbeddingChunkRepository chunkRepository, IndexQueueService indexQueueService, PgVectorStore vectorStore) {
        super(chunkRepository);
        this.chunkRepository = chunkRepository;
        this.indexQueueService = indexQueueService;
        this.vectorStore = vectorStore;
    }

    @Override
    public Page<EmbeddingChunkEntity> getAllItems(Pageable pageable) {

        if (vectorStore.isAvailable() == false) {
            // If vector store is not available (not allowed or available)
            return new DatatablePageImpl<>( new ArrayList<>() );
        } else if (vectorStore.isAvailableAndInitialized() == false) {
            // Vector store is available but not initialized, we can try to initialize it
            if(vectorStore.initializeSchema() == false) {
                // Inicialization failed, return empty results, error will be logged by vector store
                return new DatatablePageImpl<>( new ArrayList<>() );
            }
        }

        // Check if entityType is set and valid
        RagEntityType ragEntityType = RagEntityType.fromString( getRequest().getParameter("entityType") );
        if(ragEntityType == null) return new DatatablePageImpl<>( new ArrayList<>() );

        Page<EmbeddingChunkEntity> page = new DatatablePageImpl<>(super.getAllItemsIncludeSpecSearch(new EmbeddingChunkEntity(), pageable));
        processFromEntity(page, ProcessItemAction.GETALL);
        return page;
    }

    @Override
    public Page<EmbeddingChunkEntity> findByColumns(Map<String, String> params, Pageable pageable, EmbeddingChunkEntity search) {
        // Check if entityType is set and valid
        RagEntityType ragEntityType = RagEntityType.fromString( params.get("entityType") );
        if(ragEntityType == null) return new DatatablePageImpl<>( new ArrayList<>() );

        return super.findByColumns(params, pageable, search);
    }

    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<EmbeddingChunkEntity> root, CriteriaBuilder builder) {
        // By entity type - apply additional filtering
        RagEntityType ragEntityType = RagEntityType.fromString( params.get("entityType") );
        if(RagEntityType.DOCUMENT.equals(ragEntityType)) {
            int rootDir = Tools.getIntValue(params.get("searchRootDir"), -1);
            if (rootDir > 0) {
                boolean includeSubfolders = Tools.getBooleanValue(params.get("includeSubfolders"), false);
                Pair<Integer, List<Integer>> data = getDocIds(rootDir, includeSubfolders);

                List<Integer> entityIds = data != null ? data.getSecond() : new ArrayList<>();
                if(entityIds.isEmpty()) entityIds.add(-1); // to avoid error with empty list
                predicates.add( builder.and(
                    root.get("entityId").in(entityIds)
                ) );
            }
        }

        super.addSpecSearch(params, predicates, root, builder);
    }


    @Override
    public void getOptions(DatatablePageImpl<EmbeddingChunkEntity> page) {
        if (vectorStore.isAvailableAndInitialized() == true) {
            page.addOptions("entityType", chunkRepository.findDistinctEntityTypes(CloudToolsForCore.getDomainId()).stream().map(Enum::name).toList());
        } else {
            // Store is not available or not initialized (if needed), init is handled by getAlItems
            page.addOptions("entityType", new ArrayList<>() );
        }

        page.addOptions("language", Arrays.asList( Constants.getArray("languages") ));

        super.getOptions(page);
    }

    @Override
    public EmbeddingChunkEntity processFromEntity(EmbeddingChunkEntity entity, ProcessItemAction action) {

        if(ProcessItemAction.GETALL.equals(action) || ProcessItemAction.FIND.equals(action)) {
            if(EmbeddingChunkStatus.PENDING.equals(entity.getStatus()) ) {
                entity.setRowClass("is-pending");
            } else if(EmbeddingChunkStatus.ERROR.equals(entity.getStatus()) ) {
                entity.setRowClass("is-error");
            }
        }

        return entity;
    }

    /**
     * Perform an indexing or deletion action on all documents in the specified folder.
     * Adds matched document IDs to the RAG indexing queue.
     * @param rootDir the root directory group ID (-1 for all domain documents)
     * @param includeSubfolders whether to include documents from subfolders
     * @param action the action to perform ("INDEX" or "DELETE")
     * @return the number of documents queued, or -1 on error
     */
    @PostMapping("/document-action")
    public int performDocumentAction(@RequestParam("rootDir") int rootDir, @RequestParam("includeSubfolders") boolean includeSubfolders, @RequestParam("action") String action) {
        int domainId = CloudToolsForCore.getDomainId();
        Pair<Integer, List<Integer>> data = getDocIds(rootDir, includeSubfolders);
        if(data == null || data.getSecond() == null || data.getSecond().isEmpty()) return 0;

        try {
            indexQueueService.addToQueue(data.getSecond(), RagEntityType.DOCUMENT, RagIndexAction.fromString(action), domainId);
        } catch (Exception e) {
            Logger.error(EmbeddingChunkRestController.class, "Error adding documents to index queue: " + e.getMessage());
            return -1;
        }

        return data.getSecond().size();
    }

    /**
     * Get statistics about document indexing status for the specified folder.
     * Returns total groups, total documents, already indexed count, and currently queued count.
     * @param rootDir the root directory group ID (-1 for all domain documents)
     * @param includeSubfolders whether to include documents from subfolders
     * @param action the action to check queue status for
     * @return map with keys: totalGroups, totalDocuments, indexedDocuments, queuedDocuments
     */
    @GetMapping("/document-stat")
    public Map<String, Object> getDocumentStat(@RequestParam("rootDir") int rootDir, @RequestParam("includeSubfolders") boolean includeSubfolders, @RequestParam("action") String action) {
        Pair<Integer, List<Integer>> data = getDocIds(rootDir, includeSubfolders);
        if (data == null) data = new Pair<>(0, new ArrayList<>());

        Map<String, Object> response = new HashMap<>();
        response.put("totalGroups", data.getFirst());
        response.put("totalDocuments", data.getSecond() != null ? data.getSecond().size() : 0);

        if (vectorStore.isAvailableAndInitialized() == false) {
            // Cannot check indexed status if vector store is not available or not initialized, return 0 for both indexed and queued
            response.put("indexedDocuments", 0);
            response.put("queuedDocuments", 0);
            return response;
        }

        Set<Integer> indexedDocIds = chunkRepository
                .findDistinctEntityIdsByEntityTypeAndDomainId(RagEntityType.DOCUMENT, CloudToolsForCore.getDomainId())
                .stream().collect(Collectors.toSet());

        int indexedCount = 0;
        if (data.getSecond() != null) {
            for (Integer docId : data.getSecond()) {
                if (indexedDocIds.contains(docId)) indexedCount++;
            }
        }
        response.put("indexedDocuments", indexedCount);

        RagIndexAction ragAction = RagIndexAction.fromString(action);
        if (ragAction == null) {
            response.put("queuedDocuments", 0);
            return response;
        }

        Set<Integer> queued = indexQueueService.getQueued(RagEntityType.DOCUMENT, ragAction, CloudToolsForCore.getDomainId())
                .stream().collect(Collectors.toSet());

        int queuedCount = 0;
        if (data.getSecond() != null) {
            for (Integer docId : data.getSecond()) {
                if (queued.contains(docId)) queuedCount++;
            }
        }
        response.put("queuedDocuments", queuedCount);

        return response;
    }

    /**
     * Collect searchable document IDs from the specified folder (and optionally subfolders).
     * @param rootDir the root directory group ID (-1 for all domain documents)
     * @param includeSubfolders whether to include documents from subfolders
     * @return pair of (total group count, list of document IDs), or null if folder not found
     */
    private Pair<Integer, List<Integer>> getDocIds(int rootDir, boolean includeSubfolders) {
        DocDB docDB = DocDB.getInstance();
        GroupsDB groupDB = GroupsDB.getInstance();
        List<Integer> docIds = new ArrayList<>();

        int allGroupCount = 0;

        if(rootDir == -1) {
            // All from domain

            if(includeSubfolders == false) return new Pair<>(allGroupCount, docIds);

            List<Integer> rootGroupsIds = new SimpleQuery().forListInteger("SELECT group_id FROM groups WHERE parent_group_id = 0 AND domain_name = ? AND group_name != 'System'", CloudToolsForCore.getDomainName());
            if(rootGroupsIds.isEmpty()) return null;
            String idsJoined = rootGroupsIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            docIds = new SimpleQuery().forListInteger("SELECT doc_id FROM documents WHERE root_group_l1 IN (" + idsJoined + ") AND searchable = "+DB.getBooleanSql(true));

            allGroupCount = rootGroupsIds.size();
        } else {
            List<GroupDetails> groups = new ArrayList<>();
            if(includeSubfolders == false) {
                GroupDetails group = groupDB.findGroup(rootDir);
                if(group == null) return null;
                else groups.add(group);
            } else {
                groups = groupDB.getGroupsTree(rootDir, includeSubfolders, false);
                if(groups.isEmpty()) return null;
            }

            for(GroupDetails group : groups) {
                for(DocDetails doc : docDB.getDocByGroup(group.getGroupId()) ) {
                    // only searchable documents should be indexed
                    if(doc.isSearchable()) docIds.add(doc.getDocId());
                }
            }

            allGroupCount = groups.size();
        }

        return new Pair<>(allGroupCount, docIds);
    }
}