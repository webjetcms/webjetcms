package sk.iway.iwcm.rag.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.rag.RagIndexAction;
import sk.iway.iwcm.rag.jpa.IndexQueueEntity;
import sk.iway.iwcm.rag.jpa.IndexQueueRepository;
import sk.iway.iwcm.rag.vectorjpa.EmbeddingChunkEntity;
import sk.iway.iwcm.rag.vectorjpa.EmbeddingChunkRepository;
import sk.iway.iwcm.rag.vectorstore.VectorStore;
import sk.iway.iwcm.system.multidomain.DomainRequestBeanScope;

/** Prepares per-file Markdown queue actions for an explicitly configured documentation root. */
@Service
public class MarkdownQueueService {

    private final MarkdownSourceService sources;
    private final IndexQueueRepository queueRepository;
    private final EmbeddingChunkRepository chunkRepository;
    private final RagEmbeddingStatService statistics;
    private final VectorStore vectorStore;

    public MarkdownQueueService(MarkdownSourceService sources, IndexQueueRepository queueRepository,
            EmbeddingChunkRepository chunkRepository, RagEmbeddingStatService statistics, VectorStore vectorStore) {
        this.sources = sources;
        this.queueRepository = queueRepository;
        this.chunkRepository = chunkRepository;
        this.statistics = statistics;
        this.vectorStore = vectorStore;
    }

    public List<String> getFolders() {
        return sources.getRoots();
    }

    /**
     * Returns recognized folder languages for filtering stored Markdown chunks.
     *
     * @return normalized shared language codes
     */
    public List<String> getLanguages() {
        return sources.getLanguages();
    }

    /**
     * Lists immediate eligible child directories using shared source configuration.
     *
     * @param folder configured documentation root
     * @param directory root-relative directory, empty or null for the root
     * @return child directory nodes for lazy loading
     * @throws IllegalStateException if the directory cannot be scanned
     */
    public List<MarkdownSourceService.DirectoryNode> getDirectories(String folder, String directory) {
        try (DomainRequestBeanScope scope = DomainRequestBeanScope.open(null)) {
            return sources.getDirectories(folder, directory);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot scan Markdown documentation folder", e);
        }
    }

    public String requireDirectory(String directory) {
        return sources.requireDirectory(directory);
    }

    /**
     * Counts unique files across the root, including removed and pending-only sources for deletion.
     *
     * @param folder configured documentation root
     * @param action indexing or deletion action
     * @return counts keyed by {@code totalDocuments}, {@code indexedDocuments}, and {@code queuedDocuments}
     */
    public Map<String, Object> getStats(String folder, RagIndexAction action) {
        return getStats(folder, action, "", true);
    }

    /**
     * Counts unique files selected by directory and recursion settings.
     * Indexing counts use the requesting domain's assistant; source storage and pending work are shared.
     *
     * @param folder configured documentation root
     * @param action indexing or deletion action
     * @param directory root-relative directory, empty or null for the root
     * @param includeSubfolders whether to include descendant directories
     * @return counts keyed by {@code totalDocuments}, {@code indexedDocuments}, and {@code queuedDocuments}
     */
    public Map<String, Object> getStats(String folder, RagIndexAction action, String directory, boolean includeSubfolders) {
        int domainId = CloudToolsForCore.getDomainId();
        if (domainId < 1) throw new IllegalArgumentException("Markdown queue requires a positive accounting domainId");
        // Assistant defaults belong to the requesting domain, while documentation sources are global.
        AssistantDefinitionEntity assistant = action == RagIndexAction.INDEX ? statistics.getIndexingAssistant(domainId) : null;
        try (DomainRequestBeanScope scope = DomainRequestBeanScope.open(null)) {
            Selection selection = selectSources(folder, action, directory, includeSubfolders, assistant);
            int pending = selection.queued().stream().filter(item -> item.getAction() == action)
                .map(IndexQueueEntity::getSourcePath).collect(Collectors.toSet()).size();
            return Map.of("totalDocuments", selection.paths().size(), "indexedDocuments", selection.indexedCount(), "queuedDocuments", pending);
        }
    }

    /**
     * Replaces pending actions across the root, including pending-only sources when deleting.
     *
     * @param folder configured documentation root
     * @param action indexing or deletion action
     * @return number of unique source files queued
     */
    @Transactional
    public int enqueue(String folder, RagIndexAction action) {
        return enqueue(folder, action, "", true);
    }

    /**
     * Replaces pending actions within the selected scope across all submitting domains.
     * New entries retain the initiating domain for provider calls and token usage.
     *
     * @param folder configured documentation root
     * @param action indexing or deletion action
     * @param directory root-relative directory, empty or null for the root
     * @param includeSubfolders whether to include descendant directories
     * @return number of unique source files queued
     */
    @Transactional
    public int enqueue(String folder, RagIndexAction action, String directory, boolean includeSubfolders) {
        // Keep the initiating domain for deferred provider calls and their credit statistics.
        int domainId = CloudToolsForCore.getDomainId();
        if (domainId < 1) throw new IllegalArgumentException("Markdown queue requires a positive accounting domainId");
        try (DomainRequestBeanScope scope = DomainRequestBeanScope.open(null)) {
            Selection selection = selectSources(folder, action, directory, includeSubfolders, null);
            List<Long> replacedIds = selection.queued().stream().map(IndexQueueEntity::getId).toList();
            if (replacedIds.isEmpty() == false) queueRepository.deleteAllByIdInBatch(replacedIds);
            Date now = new Date();
            List<IndexQueueEntity> entries = selection.paths().stream().sorted().map(path -> {
                IndexQueueEntity item = new IndexQueueEntity();
                item.setEntityType(RagEntityType.MARKDOWN);
                item.setEntityId(MarkdownIndexService.entityId(selection.root(), path, MarkdownIndexService.SHARED_DOMAIN_ID));
                item.setSourcePath(selection.root() + "/" + path);
                item.setDomainId(domainId);
                item.setAction(action);
                item.setCreateDate(now);
                return item;
            }).toList();
            if (entries.isEmpty() == false) queueRepository.saveAll(entries);
            return entries.size();
        }
    }

    /**
     * Loads shared chunks for a root, initializing the vector schema when required for deletion.
     * An uninitialized store contributes no chunks to indexing statistics.
     *
     * @param root normalized configured documentation root
     * @param action action whose stored sources are being inspected
     * @return shared chunks whose logical paths start with the root prefix
     * @throws IllegalStateException if deletion requires an unavailable store or schema initialization fails
     */
    private List<EmbeddingChunkEntity> getChunks(String root, RagIndexAction action) {
        if (vectorStore.isAvailableAndInitialized() == false) {
            if (action == RagIndexAction.INDEX) return List.of();
            if (vectorStore.isAvailable() == false) throw new IllegalStateException("Markdown vector store is unavailable");
            if (vectorStore.initializeSchema() == false) throw new IllegalStateException("Cannot initialize Markdown vector store");
        }
        return chunkRepository.findByEntityTypeAndDomainIdAndSourcePathStartingWith(RagEntityType.MARKDOWN, MarkdownIndexService.SHARED_DOMAIN_ID, root + "/");
    }

    /**
     * Builds the file selection shared by statistics and queue replacement.
     * Indexing scans existing eligible files; deletion uses stored and queued sources without scanning disk.
     *
     * @param folder configured documentation root
     * @param action indexing or deletion action
     * @param directory root-relative directory, empty or null for the root
     * @param includeSubfolders whether to include descendant directories
     * @param assistant assistant for indexed-count filtering, or {@code null} when no provider filter is needed
     * @return selected paths, indexed source count, and pending actions to replace
     */
    private Selection selectSources(String folder, RagIndexAction action, String directory, boolean includeSubfolders, AssistantDefinitionEntity assistant) {
        if (action == null) throw new IllegalArgumentException("Markdown queue action is required");
        String root = sources.requireRoot(folder);
        String selectedDirectory = sources.requireDirectory(directory);
        Set<String> paths = new HashSet<>();
        if (action == RagIndexAction.INDEX) {
            try {
                paths.addAll(sources.getFiles(root, selectedDirectory, includeSubfolders));
            } catch (IOException e) {
                throw new IllegalStateException("Cannot scan Markdown documentation folder", e);
            }
        }
        Predicate<String> isSelected = action == RagIndexAction.INDEX ? paths::contains
            : path -> sources.isInDirectory(path, selectedDirectory, includeSubfolders);

        String provider = assistant == null || assistant.getProvider() == null || assistant.getModel() == null
            ? null : assistant.getProvider().trim().toLowerCase(Locale.ROOT);
        Set<String> indexed = new HashSet<>();
        // Enqueueing INDEX only needs source files; stored chunks matter for statistics and deletion.
        if (action == RagIndexAction.DELETE || provider != null) {
            for (EmbeddingChunkEntity chunk : getChunks(root, action)) {
                String path = relativePathForEntity(root, chunk.getSourcePath(), chunk.getEntityId());
                if (path == null || isSelected.test(path) == false) continue;
                if (action == RagIndexAction.INDEX && (provider.equals(chunk.getEmbeddingProvider()) == false
                        || assistant.getModel().equals(chunk.getEmbeddingModel()) == false || chunk.getLanguage() == null
                        || chunk.getLanguage().equals(sources.detectLanguage(root, path)) == false)) continue;
                paths.add(path);
                indexed.add(path);
            }
        }

        List<IndexQueueEntity> queued = new ArrayList<>();
        // The target index is shared, so a new action must also replace work submitted by another domain.
        for (IndexQueueEntity item : queueRepository.findByEntityTypeAndSourcePathStartingWith(RagEntityType.MARKDOWN, root + "/")) {
            String path = relativePathForEntity(root, item.getSourcePath(), item.getEntityId());
            if (path == null || isSelected.test(path) == false) continue;
            paths.add(path);
            queued.add(item);
        }
        return new Selection(root, paths, indexed.size(), queued);
    }

    /**
     * Distinguishes configured collections that share the same full source path.
     *
     * @param root configured documentation root
     * @param sourcePath full logical source path
     * @param entityId stored source identifier
     * @return eligible root-relative path if its derived ID matches, otherwise {@code null}
     */
    private String relativePathForEntity(String root, String sourcePath, Long entityId) {
        String path = sources.getRelativePath(root, sourcePath);
        return path != null && Long.valueOf(MarkdownIndexService.entityId(root, path, MarkdownIndexService.SHARED_DOMAIN_ID)).equals(entityId) ? path : null;
    }

    /**
     * Groups the source selection used for queue counts and replacement.
     *
     * @param root normalized configured root
     * @param paths unique root-relative paths selected for the action
     * @param indexedCount number of selected sources with matching stored chunks
     * @param queued pending entries in the selection across accounting domains
     */
    private record Selection(String root, Set<String> paths, int indexedCount, List<IndexQueueEntity> queued) {}
}
