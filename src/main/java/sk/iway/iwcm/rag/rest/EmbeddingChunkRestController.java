package sk.iway.iwcm.rag.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.rag.RagIndexAction;
import sk.iway.iwcm.rag.vectorjpa.EmbeddingChunkEntity;
import sk.iway.iwcm.rag.vectorjpa.EmbeddingChunkRepository;
import sk.iway.iwcm.rag.vectorjpa.EmbeddingChunkStatus;
import sk.iway.iwcm.rag.service.IndexQueueService;
import sk.iway.iwcm.rag.service.MarkdownQueueService;
import sk.iway.iwcm.rag.service.MarkdownIndexService;
import sk.iway.iwcm.rag.service.RagEmbeddingStatService;
import sk.iway.iwcm.rag.service.RagEntityType;
import sk.iway.iwcm.rag.vectorstore.VectorStore;
import sk.iway.iwcm.rag.vectorstore.VectorStoreDataSourceResolver;
import sk.iway.iwcm.rag.vectorstore.VectorStoreType;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.system.datatable.NotifyBean.NotifyType;
import sk.iway.iwcm.tags.support.ResponseUtils;
import sk.iway.iwcm.system.multidomain.DomainRequestBeanScope;
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
    private final RagEmbeddingStatService ragEmbeddingStatService;
    private final MarkdownQueueService markdownQueueService;

    private final VectorStore vectorStore;

    @Autowired
    public EmbeddingChunkRestController(EmbeddingChunkRepository chunkRepository, IndexQueueService indexQueueService, VectorStore vectorStore, RagEmbeddingStatService ragEmbeddingStatService, MarkdownQueueService markdownQueueService) {
        super(chunkRepository);
        this.chunkRepository = chunkRepository;
        this.indexQueueService = indexQueueService;
        this.vectorStore = vectorStore;
        this.ragEmbeddingStatService = ragEmbeddingStatService;
        this.markdownQueueService = markdownQueueService;
    }

    /**
     * Uses shared domain zero for the Markdown tab and the current domain for other entity types.
     *
     * @return storage domain selected by the request entity type
     */
    @Override
    protected int getDomainId() {
        if (getRequest() != null && RagEntityType.MARKDOWN == RagEntityType.fromString(getRequest().getParameter("entityType"))) {
            return MarkdownIndexService.SHARED_DOMAIN_ID;
        }
        return super.getDomainId();
    }

    /**
     * Loads chunks for the requested source type after preparing the vector schema.
     * Markdown retrieval uses shared configuration and storage.
     *
     * @param pageable requested page and sort order
     * @return matching chunks, or an empty page when the type is invalid or the vector store is unavailable
     */
    @Override
    public Page<EmbeddingChunkEntity> getAllItems(Pageable pageable) {

        RagEntityType ragEntityType = RagEntityType.fromString( getRequest().getParameter("entityType") );
        try (DomainRequestBeanScope scope = ragEntityType == RagEntityType.MARKDOWN ? DomainRequestBeanScope.open(null) : null) {

            if (vectorStore.isAvailable() == false) {
                // If vector store is not available (not allowed or available)
                if (ragEntityType == null) addNotify(new NotifyBean(getProp().getText("settings.embedding-chunks.title"), getProp().getText("components.ai_assistants.provider.not_configured"), NotifyType.ERROR));
                return new DatatablePageImpl<>( new ArrayList<>() );
            }
            if (vectorStore.isAvailableAndInitialized() == false && vectorStore.initializeSchema() == false) {
                return new DatatablePageImpl<>(List.of());
            }

            // Check if entityType is set and valid
            if(ragEntityType == null) return new DatatablePageImpl<>( new ArrayList<>() );

            Page<EmbeddingChunkEntity> page = new DatatablePageImpl<>(super.getAllItemsIncludeSpecSearch(new EmbeddingChunkEntity(), pageable));
            processFromEntity(page, ProcessItemAction.GETALL);
            return page;
        }
    }

    /**
     * Applies column filtering in the shared configuration context for Markdown chunks.
     *
     * @param params request parameters including the source entity type
     * @param pageable requested page and sort order
     * @param search entity holding example-search values
     * @return matching chunks, or an empty page for an invalid source type
     */
    @Override
    public Page<EmbeddingChunkEntity> findByColumns(Map<String, String> params, Pageable pageable, EmbeddingChunkEntity search) {
        // Check if entityType is set and valid
        RagEntityType ragEntityType = RagEntityType.fromString( params.get("entityType") );
        try (DomainRequestBeanScope scope = ragEntityType == RagEntityType.MARKDOWN ? DomainRequestBeanScope.open(null) : null) {
            if(ragEntityType == null) return new DatatablePageImpl<>( new ArrayList<>() );

            return super.findByColumns(params, pageable, search);
        }
    }

    /**
     * Leaves Markdown root and directory constraints to specification filtering.
     *
     * @param params request search parameters
     * @param searchProperties column properties populated by the base controller
     * @param searchWrapped wrapper for the example-search entity
     * @param matcher current example matcher
     * @param isExampleSearch whether example-based matching is active
     * @return base matcher after excluding root and directory parameters
     */
    @Override
    public ExampleMatcher getSearchProperties(Map<String, String> params, Map<String, String> searchProperties, BeanWrapperImpl searchWrapped, ExampleMatcher matcher, boolean isExampleSearch) {
        Map<String, String> columnParams = new HashMap<>(params);
        // The folder selector is scoped exactly in addSpecSearch, independently of the primary database's collation.
        columnParams.remove("sourceRoot");
        columnParams.remove("directory");
        return super.getSearchProperties(columnParams, searchProperties, searchWrapped, matcher, isExampleSearch);
    }

    /**
     * Adds document-group or shared Markdown path constraints before applying base search predicates.
     *
     * @param params request parameters defining source type and folder selection
     * @param predicates mutable collection receiving search predicates
     * @param root embedding chunk query root
     * @param builder criteria builder used to construct predicates
     */
    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<EmbeddingChunkEntity> root, CriteriaBuilder builder) {
        // By entity type - apply additional filtering
        RagEntityType ragEntityType = RagEntityType.fromString( params.get("entityType") );
        if (RagEntityType.MARKDOWN.equals(ragEntityType)) {
            List<String> folders = markdownQueueService.getFolders();
            String selectedFolder = params.get("sourceRoot");
            if (Tools.isNotEmpty(selectedFolder)) {
                if (folders.contains(selectedFolder) == false) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Documentation folder is not configured");
                }
                folders = List.of(selectedFolder);
            }
            String directory;
            try {
                directory = markdownQueueService.requireDirectory(params.get("directory"));
                if (directory.isEmpty() == false && Tools.isEmpty(selectedFolder)) {
                    throw new IllegalArgumentException("A documentation root is required for a subfolder filter");
                }
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
            }
            predicates.add(builder.equal(root.get("entityType"), RagEntityType.MARKDOWN));
            predicates.add(builder.equal(root.get("domainId"), MarkdownIndexService.SHARED_DOMAIN_ID));
            if (folders.isEmpty()) {
                predicates.add(builder.disjunction());
            } else {
                Expression<String> sourcePath = root.get("sourcePath");
                if (VectorStoreDataSourceResolver.resolve().backend() == VectorStoreType.MARIADB) {
                    // File-system paths remain case-sensitive even when the database uses a case-insensitive collation.
                    sourcePath = builder.function("BINARY", String.class, sourcePath);
                }
                boolean includeSubfolders = Tools.getBooleanValue(params.get("includeSubfolders"), true);
                List<Predicate> folderPredicates = new ArrayList<>();
                for (String folder : folders) {
                    String prefix = (folder + "/" + (directory.isEmpty() ? "" : directory + "/"))
                        .replace("!", "!!").replace("%", "!%").replace("_", "!_");
                    Predicate folderPredicate = builder.like(sourcePath, prefix + "%", '!');
                    if (includeSubfolders == false) {
                        folderPredicate = builder.and(folderPredicate, builder.notLike(sourcePath, prefix + "%/%", '!'));
                    }
                    folderPredicates.add(folderPredicate);
                }
                predicates.add(builder.or(folderPredicates.toArray(new Predicate[0])));
            }
        }
        if(RagEntityType.DOCUMENT.equals(ragEntityType)) {
            int rootDir = Tools.getIntValue(params.get("searchRootDir"), -1);
            if (rootDir > 0) {
                boolean includeSubfolders = Tools.getBooleanValue(params.get("includeSubfolders"), false);
                List<Integer> entityIds = getDocIds(rootDir, includeSubfolders).getSecond();
                predicates.add(entityIds.isEmpty() ? builder.disjunction() : root.get("entityId").in(entityIds));
            }
        }

        super.addSpecSearch(params, predicates, root, builder);
    }

    /**
     * Returns globally configured documentation roots for the shared Markdown tab.
     *
     * @return normalized configured documentation roots
     */
    @GetMapping("/markdown-folders")
    public List<String> getMarkdownFolders() {
        return markdownQueueService.getFolders();
    }

    /**
     * Supplies the shared folder picker with configured roots and lazily loaded child directories.
     *
     * @param request tree request containing a root marker or an encoded node ID in {@code id}
     * @return success flag and child items, or a localized scan error
     * @throws ResponseStatusException if the node ID or directory selection is invalid
     */
    @PostMapping("/markdown-folders/tree")
    public Map<String, Object> getMarkdownFolderTree(@RequestBody Map<String, String> request) {
        String id = request.getOrDefault("id", "-1");
        try {
            if (id == null) throw new IllegalArgumentException("Invalid documentation tree node");
            List<Map<String, Object>> items;
            if ("-1".equals(id) || "#".equals(id)) {
                items = markdownQueueService.getFolders().stream()
                    .map(root -> markdownTreeNode(root, "", root, "#", true)).toList();
            } else {
                String[] selection = new String(Base64.getUrlDecoder().decode(id), StandardCharsets.UTF_8).split("\n", -1);
                if (selection.length != 2) throw new IllegalArgumentException("Invalid documentation tree node");
                items = markdownQueueService.getDirectories(selection[0], selection[1]).stream()
                    .map(directory -> markdownTreeNode(directory.root(), directory.directory(), directory.name(), id, directory.children())).toList();
            }
            return Map.of("result", true, "items", items);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid documentation tree node", e);
        } catch (IllegalStateException e) {
            Logger.error(EmbeddingChunkRestController.class, "Cannot load Markdown folders: " + e.getMessage());
            return Map.of("result", false, "error", getProp().getText("settings.markdown-chunks.foldersFailed"));
        }
    }

    private Map<String, Object> markdownTreeNode(String root, String directory, String name, String parent, boolean children) {
        String id = Base64.getUrlEncoder().withoutPadding().encodeToString((root + "\n" + directory).getBytes(StandardCharsets.UTF_8));
        return Map.of("id", id, "parent", parent, "text", ResponseUtils.filter(name),
            "fullPath", directory.isEmpty() ? root : root + "/" + directory,
            "sourceRoot", root, "directory", directory, "children", children, "icon", "ti ti-folder", "state", Map.of());
    }

    /**
     * Returns file, index, and queue counts for a selected Markdown directory scope.
     *
     * @param folder configured documentation root
     * @param action requested {@code INDEX} or {@code DELETE} action
     * @param directory root-relative directory, empty for the root
     * @param includeSubfolders whether to include descendant directories
     * @return counts keyed by {@code totalDocuments}, {@code indexedDocuments}, and {@code queuedDocuments}
     */
    @GetMapping("/markdown-stat")
    public Map<String, Object> getMarkdownStat(@RequestParam("folder") String folder, @RequestParam("action") String action,
            @RequestParam(value = "directory", defaultValue = "") String directory,
            @RequestParam(value = "includeSubfolders", defaultValue = "true") boolean includeSubfolders) {
        try {
            return markdownQueueService.getStats(folder, requireMarkdownAction(action), directory, includeSubfolders);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    /**
     * Enqueues indexing or deletion for the selected documentation scope for later processing.
     *
     * @param folder configured documentation root
     * @param action requested {@code INDEX} or {@code DELETE} action
     * @param directory root-relative directory, empty for the root
     * @param includeSubfolders whether to include descendant directories
     * @return number of unique source files queued
     */
    @PostMapping("/markdown-action")
    public int performMarkdownAction(@RequestParam("folder") String folder, @RequestParam("action") String action,
            @RequestParam(value = "directory", defaultValue = "") String directory,
            @RequestParam(value = "includeSubfolders", defaultValue = "true") boolean includeSubfolders) {
        try {
            return markdownQueueService.enqueue(folder, requireMarkdownAction(action), directory, includeSubfolders);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    private RagIndexAction requireMarkdownAction(String action) {
        RagIndexAction parsed = RagIndexAction.fromString(action);
        if (parsed == null) throw new IllegalArgumentException("Unsupported Markdown indexing action");
        return parsed;
    }


    /**
     * Adds source types and languages appropriate to document or shared Markdown storage.
     *
     * @param page datatable response receiving select options
     */
    @Override
    public void getOptions(DatatablePageImpl<EmbeddingChunkEntity> page) {
        boolean markdown = RagEntityType.MARKDOWN == RagEntityType.fromString(getRequest().getParameter("entityType"));
        try (DomainRequestBeanScope scope = markdown ? DomainRequestBeanScope.open(null) : null) {
            if (vectorStore.isAvailableAndInitialized() == true) {
                int domainId = markdown ? MarkdownIndexService.SHARED_DOMAIN_ID : CloudToolsForCore.getDomainId();
                page.addOptions("entityType", chunkRepository.findDistinctEntityTypes(domainId).stream().map(Enum::name).toList());
            } else {
                // Store initialization is handled by getAllItems.
                page.addOptions("entityType", new ArrayList<>() );
            }

            page.addOptions("language", markdown ? markdownQueueService.getLanguages() : Arrays.asList(Constants.getArray("languages")));

            super.getOptions(page);
        }
    }

    /**
     * Adds pending or error row styling when chunks are returned for listing or searching.
     *
     * @param entity chunk whose row styling may be updated
     * @param action datatable operation producing the chunk
     * @return the supplied chunk with any applicable row class
     */
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
     * Returns the provider and model configured for RAG indexing in the current domain.
     *
     * @param entityType source type; both document and shared Markdown indexing use the current domain's assistant
     * @return configuration map containing {@code provider} and {@code model}, or an empty map when no complete
     *         indexing assistant is configured
     */
    @GetMapping("/current-embedding-configuration")
    public Map<String, String> getCurrentEmbeddingConfiguration(@RequestParam(value = "entityType", defaultValue = "DOCUMENT") String entityType) {
        AssistantDefinitionEntity assistant = ragEmbeddingStatService.getIndexingAssistant();
        if (assistant == null || Tools.isEmpty(assistant.getProvider()) || Tools.isEmpty(assistant.getModel())) {
            return Map.of();
        }

        return Map.of(
            "provider", assistant.getProvider().trim().toLowerCase(Locale.ROOT),
            "model", assistant.getModel()
        );
    }

    /**
     * Queues an indexing or deletion action for searchable documents in an authorized folder scope.
     *
     * @param rootDir root group ID, or {@code -1} for all documents in the current domain
     * @param includeSubfolders whether documents in descendant groups should be included
     * @param action action to perform, expected to be {@code INDEX} or {@code DELETE}
     * @return number of matched documents considered for queueing, {@code 0} when none match, or {@code -1} when
     *         queueing fails
     * @throws AccessDeniedException if the requested group scope is invalid, not editable, or belongs to another
     *         domain
     */
    @PostMapping("/document-action")
    public int performDocumentAction(@RequestParam("rootDir") int rootDir, @RequestParam("includeSubfolders") boolean includeSubfolders, @RequestParam("action") String action) {
        validateDocumentActionRoot(rootDir);

        int domainId = CloudToolsForCore.getDomainId();
        Pair<Integer, List<Integer>> data = getDocIds(rootDir, includeSubfolders);
        if(data.getSecond().isEmpty()) return 0;

        try {
            indexQueueService.addToQueue(
                data.getSecond(),
                RagEntityType.DOCUMENT,
                RagIndexAction.fromString(action),
                domainId
            );
        } catch (Exception e) {
            Logger.error(EmbeddingChunkRestController.class, "Error adding documents to index queue: " + e.getMessage());
            return -1;
        }

        return data.getSecond().size();
    }

    /**
     * Verifies that the current user may manage embeddings for the requested group scope.
     *
     * @param rootDir root group ID, or {@code -1} for every root group in the current domain
     * @throws AccessDeniedException if the scope is invalid, not editable, or belongs to another domain
     */
    private void validateDocumentActionRoot(int rootDir) {
        if (rootDir == -1) {
            for (Integer rootGroupId : getCurrentDomainRootGroupIds()) {
                if (GroupsDB.isGroupEditable(getUser(), rootGroupId) == false) {
                    throw new AccessDeniedException(
                        "User is not allowed to manage RAG embeddings for every group in this domain."
                    );
                }
            }
            return;
        }
        if (rootDir < 1) {
            throw new AccessDeniedException("Invalid root group for RAG embedding management.");
        }

        GroupsDB groupsDB = GroupsDB.getInstance();
        GroupDetails group = groupsDB.findGroup(rootDir);
        if (group == null || GroupsDB.isGroupEditable(getUser(), rootDir) == false) {
            throw new AccessDeniedException("User is not allowed to manage RAG embeddings for this group.");
        }

        if (InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir")) {
            String groupDomain = groupsDB.getDomain(rootDir);
            if (CloudToolsForCore.getDomainName().equalsIgnoreCase(groupDomain) == false) {
                throw new AccessDeniedException("User is not allowed to manage RAG embeddings for another domain.");
            }
        }
    }

    private List<Integer> getCurrentDomainRootGroupIds() {
        return new SimpleQuery().forListInteger(
            "SELECT group_id FROM groups WHERE parent_group_id = 0 AND domain_name = ? AND group_name != 'System'",
            CloudToolsForCore.getDomainName()
        );
    }

    /**
     * Returns statistics about document indexing status for the specified folder.
     *
     * Returns total groups, total documents, already indexed count, and currently queued count.
     * @param rootDir the root directory group ID (-1 for all domain documents)
     * @param includeSubfolders whether to include documents from subfolders
     * @param action the action to check queue status for
     * @return map with keys: totalGroups, totalDocuments, indexedDocuments, queuedDocuments
     * @throws AccessDeniedException if the requested group scope is invalid, not editable, or belongs to another
     *         domain
     */
    @GetMapping("/document-stat")
    public Map<String, Object> getDocumentStat(@RequestParam("rootDir") int rootDir, @RequestParam("includeSubfolders") boolean includeSubfolders, @RequestParam("action") String action) {
        validateDocumentActionRoot(rootDir);

        Pair<Integer, List<Integer>> data = getDocIds(rootDir, includeSubfolders);
        List<Integer> docIds = data.getSecond();
        RagIndexAction ragAction = RagIndexAction.fromString(action);
        int domainId = CloudToolsForCore.getDomainId();
        Set<Integer> indexed = vectorStore.isAvailableAndInitialized() ? getIndexedDocumentIds(ragAction, domainId) : Set.of();
        Set<Integer> queued = ragAction == null ? Set.of()
            : Set.copyOf(indexQueueService.getQueued(RagEntityType.DOCUMENT, ragAction, domainId));
        return Map.of(
            "totalGroups", data.getFirst(),
            "totalDocuments", docIds.size(),
            "indexedDocuments", (int) docIds.stream().filter(indexed::contains).count(),
            "queuedDocuments", (int) docIds.stream().filter(queued::contains).count()
        );
    }

    /**
     * Returns document IDs with stored chunks relevant to the requested action.
     *
     * For {@link RagIndexAction#INDEX}, only chunks created by the currently configured indexing provider and model
     * qualify. For other actions, any stored document chunk in the domain qualifies.
     *
     * @param action action for which indexed documents are being determined
     * @param domainId domain whose chunks should be queried
     * @return matching document IDs
     */
    Set<Integer> getIndexedDocumentIds(RagIndexAction action, Integer domainId) {
        if (RagIndexAction.INDEX.equals(action)) {
            AssistantDefinitionEntity assistant = ragEmbeddingStatService.getIndexingAssistant();
            if (assistant == null || Tools.isEmpty(assistant.getProvider()) || Tools.isEmpty(assistant.getModel())) {
                return Set.of();
            }

            String provider = assistant.getProvider().trim().toLowerCase(Locale.ROOT);
            return chunkRepository
                .findDistinctEntityIdsByEntityTypeAndEmbeddingProviderAndEmbeddingModelAndDomainId(
                    RagEntityType.DOCUMENT,
                    provider,
                    assistant.getModel(),
                    domainId
                )
                .stream().collect(Collectors.toSet());
        }

        return chunkRepository
            .findDistinctEntityIdsByEntityTypeAndDomainId(RagEntityType.DOCUMENT, domainId)
            .stream().collect(Collectors.toSet());
    }

    /**
     * Collects searchable document IDs from the specified folder and optionally its subfolders.
     *
     * @param rootDir the root directory group ID (-1 for all domain documents)
     * @param includeSubfolders whether to include documents from subfolders
     * @return selected group count and searchable document IDs, or zero groups and an empty list for an unresolved scope
     */
    private Pair<Integer, List<Integer>> getDocIds(int rootDir, boolean includeSubfolders) {
        DocDB docDB = DocDB.getInstance();
        GroupsDB groupDB = GroupsDB.getInstance();
        List<Integer> docIds = new ArrayList<>();

        int allGroupCount = 0;

        if(rootDir == -1) {
            // All from domain

            if(includeSubfolders == false) return new Pair<>(allGroupCount, docIds);

            List<Integer> rootGroupsIds = getCurrentDomainRootGroupIds();
            if(rootGroupsIds.isEmpty()) return new Pair<>(0, List.of());
            String idsJoined = rootGroupsIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            docIds = new SimpleQuery().forListInteger("SELECT doc_id FROM documents WHERE root_group_l1 IN (" + idsJoined + ") AND searchable = "+DB.getBooleanSql(true));

            allGroupCount = rootGroupsIds.size();
        } else {
            List<GroupDetails> groups = new ArrayList<>();
            if(includeSubfolders == false) {
                GroupDetails group = groupDB.findGroup(rootDir);
                if(group == null) return new Pair<>(0, List.of());
                groups.add(group);
            } else {
                groups = groupDB.getGroupsTree(rootDir, includeSubfolders, false);
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
