package sk.iway.iwcm.rag.service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.webjetcms.ai.EmbeddingInputType;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.providers.ProviderCallException;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.rag.RagIndexAction;
import sk.iway.iwcm.rag.embedding.EmbeddingBatchResult;
import sk.iway.iwcm.rag.embedding.EmbeddingService;
import sk.iway.iwcm.rag.indexing.MarkdownContentExtractor;
import sk.iway.iwcm.rag.indexing.MarkdownChunker;
import sk.iway.iwcm.rag.indexing.SlidingWindowChunker;
import sk.iway.iwcm.rag.jpa.IndexQueueEntity;
import sk.iway.iwcm.rag.vectorjpa.EmbeddingChunkEntity;
import sk.iway.iwcm.rag.vectorjpa.EmbeddingChunkRepository;
import sk.iway.iwcm.rag.vectorjpa.EmbeddingChunkStatus;
import sk.iway.iwcm.rag.vectorstore.VectorStore;
import sk.iway.iwcm.system.multidomain.DomainRequestBeanScope;

/** Incrementally indexes Markdown sources and removes deleted files after a successful directory scan. */
@Service
public class MarkdownIndexService {

    /** Storage domain for shared documentation; assistants and token usage retain their owning domain. */
    public static final int SHARED_DOMAIN_ID = 0;

    private final MarkdownSourceService sources;
    private final MarkdownContentExtractor extractor;
    private final MarkdownChunker chunker;
    private final EmbeddingService embeddings;
    private final RagEmbeddingStatService statistics;
    private final EmbeddingChunkRepository repository;
    private final VectorStore vectorStore;
    private final ReentrantLock indexingLock = new ReentrantLock();

    public MarkdownIndexService(MarkdownSourceService sources, MarkdownContentExtractor extractor,
            MarkdownChunker chunker, EmbeddingService embeddings, RagEmbeddingStatService statistics,
            EmbeddingChunkRepository repository, VectorStore vectorStore) {
        this.sources = sources;
        this.extractor = extractor;
        this.chunker = chunker;
        this.embeddings = embeddings;
        this.statistics = statistics;
        this.repository = repository;
        this.vectorStore = vectorStore;
    }

    /**
     * Scans globally configured roots shared by all domains. A failed root is retried on the next run.
     * The caller's domain supplies the assistant and owns usage; only source configuration and storage are shared.
     *
     * @param folder optional configured root to scan, or null to scan all roots
     */
    public void indexConfiguredRoots(String folder) {
        int domainId = CloudToolsForCore.getDomainId();
        RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
        String domainName = requestBean == null ? null : requestBean.getDomain();
        if (indexingLock.tryLock() == false) return;
        try (DomainRequestBeanScope ignored = DomainRequestBeanScope.open(null)) {
            List<String> roots = folder == null || folder.isBlank() ? sources.getRoots() : List.of(sources.requireRoot(folder));
            if (roots.isEmpty() || vectorStore.isAvailable() == false) return;
            if (vectorStore.isAvailableAndInitialized() == false) {
                if (vectorStore.initializeSchema() == false) throw new IllegalStateException("Cannot initialize Markdown vector store");
            }
            AssistantDefinitionEntity assistant = getIndexingAssistant(domainId, domainName);
            for (String root : roots) {
                try {
                    indexRoot(root, assistant, domainName);
                } catch (Exception e) {
                    Logger.error(MarkdownIndexService.class, "Cannot scan Markdown root " + root + ": " + e.getMessage(), e);
                }
            }
        } finally {
            indexingLock.unlock();
        }
    }

    /**
     * Processes a shared file using the queue's accounting domain; failures are propagated for retry.
     * Deletion actions and missing files remove existing chunks from shared storage.
     *
     * @param item queued action with a configured source path, matching entity ID, and positive accounting domain
     * @throws IllegalArgumentException if the queued source, action, or accounting domain is invalid
     * @throws IllegalStateException if indexing is already running or reading, embedding, or vector-store preparation fails
     */
    public void processQueueItem(IndexQueueEntity item) {
        if (indexingLock.tryLock() == false) throw new IllegalStateException("Markdown indexing is already running");
        try (DomainRequestBeanScope ignored = DomainRequestBeanScope.open(null)) {
            if (item == null || item.getDomainId() == null || item.getDomainId() < 1) {
                throw new IllegalArgumentException("Markdown queue items must identify the accounting domain");
            }
            String root = sources.getRoots().stream().filter(configuredRoot -> {
                String relativePath = sources.getRelativePath(configuredRoot, item.getSourcePath());
                return relativePath != null && item.getEntityId() != null
                    && entityId(configuredRoot, relativePath, SHARED_DOMAIN_ID) == item.getEntityId();
            }).findFirst().orElseThrow(() -> new IllegalArgumentException("Invalid queued Markdown source"));
            String path = sources.getRelativePath(root, item.getSourcePath());
            if (item.getAction() == RagIndexAction.DELETE) {
                repository.deleteByEntityTypeAndEntityIdAndDomainId(RagEntityType.MARKDOWN, item.getEntityId(), SHARED_DOMAIN_ID);
                return;
            }
            if (item.getAction() != RagIndexAction.INDEX) throw new IllegalArgumentException("Invalid queued Markdown action");
            if (sources.detectLanguage(root, path) == null) {
                throw new IllegalArgumentException("Markdown source must be inside a supported language folder");
            }
            Path file = sources.resolveFile(root, path);
            String markdown;
            try {
                markdown = Files.readString(file, StandardCharsets.UTF_8);
            } catch (NoSuchFileException e) {
                repository.deleteByEntityTypeAndEntityIdAndDomainId(RagEntityType.MARKDOWN, item.getEntityId(), SHARED_DOMAIN_ID);
                return;
            }
            if (vectorStore.isAvailable() == false) throw new IllegalStateException("Markdown vector store is unavailable");
            if (vectorStore.isAvailableAndInitialized() == false) {
                if (vectorStore.initializeSchema() == false) throw new IllegalStateException("Cannot initialize Markdown vector store");
            }
            // The worker has no request from the user who queued this shared file.
            String domainName = getAccountingDomainName(item.getDomainId());
            AssistantDefinitionEntity assistant = getIndexingAssistant(item.getDomainId(), domainName);
            List<EmbeddingChunkEntity> existing = repository.findByEntityTypeAndEntityIdAndDomainId(RagEntityType.MARKDOWN, item.getEntityId(), SHARED_DOMAIN_ID).stream()
                .filter(chunk -> item.getSourcePath().equals(chunk.getSourcePath())).toList();
            indexFile(root, path, markdown, assistant, existing, domainName);
        } catch (IOException | ProviderCallException e) {
            throw new IllegalStateException("Cannot process queued Markdown source", e);
        } finally {
            indexingLock.unlock();
        }
    }

    /**
     * Resolves and validates the indexing assistant in its accounting domain context.
     *
     * @param domainId domain that owns the assistant
     * @param domainName domain context for provider configuration, or {@code null} for the default context
     * @return assistant with a configured provider and model
     * @throws IllegalStateException if the assistant, provider, or model is missing
     */
    private AssistantDefinitionEntity getIndexingAssistant(int domainId, String domainName) {
        try (DomainRequestBeanScope ignored = DomainRequestBeanScope.open(domainName)) {
            AssistantDefinitionEntity assistant = statistics.getIndexingAssistant(domainId);
            if (assistant == null || assistant.getProvider() == null || assistant.getProvider().isBlank()
                    || assistant.getModel() == null || assistant.getModel().isBlank()) {
                throw new IllegalStateException("RAG indexing assistant is not configured");
            }
            return assistant;
        }
    }

    /**
     * Resolves an accounting domain ID using the configured domain mapping.
     *
     * @param domainId positive accounting domain ID
     * @return domain name, or {@code null} for domain one in a single-domain installation
     * @throws IllegalArgumentException if the ID has no valid accounting domain
     */
    private String getAccountingDomainName(int domainId) {
        if (InitServlet.isTypeCloud() == false && Constants.getBoolean("enableStaticFilesExternalDir") == false) {
            if (domainId == 1) return null;
            throw new IllegalArgumentException("Unknown Markdown accounting domain: " + domainId);
        }
        // Domain IDs can be overridden in configuration, so they are not necessarily group IDs.
        return GroupsDB.getInstance().getAllDomainsList().stream()
            .filter(domain -> GroupsDB.getDomainId(domain) == domainId).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown Markdown accounting domain: " + domainId));
    }

    /**
     * Indexes eligible files and removes stale sources only after the directory scan completes.
     * Individual indexing failures are logged without deleting the affected source as stale.
     *
     * @param root configured documentation root
     * @param assistant assistant used for embeddings and usage accounting
     * @param domainName accounting domain context for provider calls
     * @throws IOException if the root cannot be resolved or its files cannot be scanned or read
     */
    void indexRoot(String root, AssistantDefinitionEntity assistant, String domainName) throws IOException {
        Path directory = sources.resolveRoot(root);
        Map<String, List<EmbeddingChunkEntity>> remaining = repository.findByEntityTypeAndDomainIdAndSourcePathStartingWith(RagEntityType.MARKDOWN, SHARED_DOMAIN_ID, root + "/").stream()
            .filter(chunk -> {
                String path = sources.getRelativePath(root, chunk.getSourcePath());
                return path != null && Long.valueOf(entityId(root, path, SHARED_DOMAIN_ID)).equals(chunk.getEntityId());
            })
            .collect(Collectors.groupingBy(chunk -> sources.getRelativePath(root, chunk.getSourcePath())));
        sources.visitFiles(root, directory, directory, true, (file, sourcePath) -> {
            String markdown = Files.readString(file, StandardCharsets.UTF_8);
            List<EmbeddingChunkEntity> existing = remaining.remove(sourcePath);
            try {
                indexFile(root, sourcePath, markdown, assistant, existing == null ? List.of() : existing, domainName);
            } catch (Exception e) {
                Logger.error(MarkdownIndexService.class, "Cannot index Markdown source " + root + "/" + sourcePath + ": " + e.getMessage(), e);
            }
        });

        // A successful scan permits removing sources that disappeared or no longer have a supported language folder.
        for (List<EmbeddingChunkEntity> chunks : remaining.values()) {
            repository.deleteByEntityTypeAndEntityIdAndDomainId(RagEntityType.MARKDOWN, chunks.get(0).getEntityId(), SHARED_DOMAIN_ID);
        }
    }

    /**
     * Updates one Markdown source, reusing matching vectors and embedding only changed content.
     * Unchanged completed sources are skipped. Empty content removes all source chunks; otherwise
     * replacement affects only the selected provider and model after all vectors are resolved.
     *
     * @param root configured documentation root
     * @param sourcePath root-relative Markdown path
     * @param markdown raw source content
     * @param assistant assistant supplying provider, model, and usage ownership
     * @param existing stored chunks for this source
     * @param domainName accounting domain context for embedding calls
     * @throws ProviderCallException if the embedding provider call fails
     */
    void indexFile(String root, String sourcePath, String markdown, AssistantDefinitionEntity assistant, List<EmbeddingChunkEntity> existing, String domainName) throws ProviderCallException {
        String selectedLanguage = sources.detectLanguage(root, sourcePath);
        if (selectedLanguage == null) throw new IllegalArgumentException("Markdown source must be inside a supported language folder");
        long entityId = entityId(root, sourcePath, SHARED_DOMAIN_ID);
        String text = extractor.extractText(markdown);
        List<SlidingWindowChunker.Chunk> chunks = chunker.chunkWithOffsets(text);
        if (chunks.isEmpty()) {
            repository.deleteByEntityTypeAndEntityIdAndDomainId(RagEntityType.MARKDOWN, entityId, SHARED_DOMAIN_ID);
            return;
        }
        String provider = assistant.getProvider().trim().toLowerCase(Locale.ROOT);
        String model = assistant.getModel();
        int dimensions = embeddings.getDimensions();
        String title = extractor.extractTitle(markdown, sourcePath);
        List<String> inputs = extractor.addHeadingContext(text, title, chunks);
        String sourceHash = hash(markdown);
        List<String> hashes = inputs.stream().map(MarkdownIndexService::hash).toList();
        List<EmbeddingChunkEntity> current = existing.stream()
            .filter(chunk -> provider.equals(chunk.getEmbeddingProvider()) && model.equals(chunk.getEmbeddingModel())).toList();
        if (isUnchanged(current, sourceHash, inputs, hashes, dimensions, selectedLanguage)) return;

        Map<String, float[]> vectors = new HashMap<>(vectorStore.getExistingEmbeddingsByHash(RagEntityType.MARKDOWN.name(), entityId, provider, model, SHARED_DOMAIN_ID));
        Map<String, String> changed = new java.util.LinkedHashMap<>();
        for (int i = 0; i < chunks.size(); i++) {
            float[] cached = vectors.get(hashes.get(i));
            if (cached == null || cached.length != dimensions) changed.put(hashes.get(i), inputs.get(i));
        }
        if (changed.isEmpty() == false) {
            // Shared storage does not change which domain supplies credentials or pays for this API call.
            EmbeddingBatchResult result = embeddings.embedWithUsage(new ArrayList<>(changed.values()), assistant, domainName, EmbeddingInputType.DOCUMENT);
            statistics.recordIndexingTokens(assistant, result.getUsedTokens(), assistant.getDomainId());
            int index = 0;
            for (String chunkHash : changed.keySet()) vectors.put(chunkHash, result.getEmbeddings().get(index++));
        }
        List<float[]> resolved = hashes.stream().map(vectors::get).toList();
        for (float[] vector : resolved) {
            if (vector.length != dimensions) throw new IllegalStateException("Markdown embedding dimension mismatch");
            for (float value : vector) {
                if (Float.isFinite(value) == false) throw new IllegalStateException("Invalid Markdown embedding value");
            }
        }

        List<EmbeddingChunkEntity> rows = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            EmbeddingChunkEntity row = new EmbeddingChunkEntity();
            row.setEntityType(RagEntityType.MARKDOWN);
            row.setEntityId(entityId);
            row.setChunkIndex(i);
            row.setChunkText(inputs.get(i));
            row.setContentHash(hashes.get(i));
            row.setEmbeddingProvider(provider);
            row.setEmbeddingModel(model);
            row.setDimensions(dimensions);
            row.setLanguage(selectedLanguage);
            row.setDomainId(SHARED_DOMAIN_ID);
            row.setSourcePath(root + "/" + sourcePath);
            row.setSourceTitle(title);
            row.setSourceHash(sourceHash);
            row.setStatus(EmbeddingChunkStatus.PENDING);
            row.setCreateDate(new Date());
            rows.add(row);
        }
        // Resolve every vector before replacing the old index; provider failures leave previous content available.
        repository.deleteByEntityTypeAndEntityIdAndEmbeddingProviderAndEmbeddingModelAndDomainId(RagEntityType.MARKDOWN, entityId, provider, model, SHARED_DOMAIN_ID);
        List<EmbeddingChunkEntity> saved = repository.saveAllAndFlush(rows);
        vectorStore.updateEmbeddingBatch(saved.stream().map(EmbeddingChunkEntity::getId).toList(), resolved);
    }

    /**
     * Checks whether stored chunks already match the source, content, language, and vector dimensions.
     *
     * @param current stored chunks for the selected provider and model
     * @param sourceHash hash of the complete raw source
     * @param inputs contextual text to store and embed, in chunk order
     * @param hashes contextual embedding-input hashes in chunk order
     * @param dimensions required vector dimension count
     * @param language detected documentation language
     * @return {@code true} when counts match and every stored chunk is completed with matching metadata
     */
    private boolean isUnchanged(List<EmbeddingChunkEntity> current, String sourceHash, List<String> inputs, List<String> hashes, int dimensions, String language) {
        if (current.size() != hashes.size()) return false;
        for (EmbeddingChunkEntity chunk : current) {
            int index = chunk.getChunkIndex();
            if (index < 0 || index >= hashes.size()
                    || chunk.getStatus() != EmbeddingChunkStatus.COMPLETED || Integer.valueOf(dimensions).equals(chunk.getDimensions()) == false
                    || language.equals(chunk.getLanguage()) == false
                    || inputs.get(index).equals(chunk.getChunkText()) == false
                    || sourceHash.equals(chunk.getSourceHash()) == false || hashes.get(index).equals(chunk.getContentHash()) == false) return false;
        }
        return true;
    }

    /**
     * Derives a stable positive entity ID from the domain, root, and relative path using SHA-256.
     *
     * @param root normalized configured root
     * @param sourcePath root-relative source path
     * @param domainId storage domain; shared Markdown uses zero
     * @return positive identifier derived from the first eight digest bytes
     */
    static long entityId(String root, String sourcePath, int domainId) {
        long id = ByteBuffer.wrap(digest(domainId + "\n" + root + "\n" + sourcePath)).getLong() & Long.MAX_VALUE;
        return id == 0 ? 1 : id;
    }

    /**
     * Computes the SHA-256 content hash used to detect unchanged Markdown and chunks.
     *
     * @param value non-null content encoded as UTF-8
     * @return lowercase hexadecimal SHA-256 digest
     */
    static String hash(String value) {
        return HexFormat.of().formatHex(digest(value));
    }

    private static byte[] digest(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
