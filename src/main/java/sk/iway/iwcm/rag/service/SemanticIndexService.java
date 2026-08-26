package sk.iway.iwcm.rag.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.structuremirroring.GroupMirroringServiceV9;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.rag.RagIndexAction;
import sk.iway.iwcm.rag.embedding.EmbeddingBatchResult;
import sk.iway.iwcm.rag.embedding.EmbeddingService;
import sk.iway.iwcm.rag.indexing.DocDetailsContentExtractor;
import sk.iway.iwcm.rag.indexing.SlidingWindowChunker;
import sk.iway.iwcm.rag.jpa.IndexQueueEntity;
import sk.iway.iwcm.rag.jpa.IndexQueueRepository;
import sk.iway.iwcm.rag.pgvector.EmbeddingChunkEntity;
import sk.iway.iwcm.rag.pgvector.EmbeddingChunkRepository;
import sk.iway.iwcm.rag.pgvector.EmbeddingChunkStatus;
import sk.iway.iwcm.rag.vectorstore.PgVectorStore;
import sk.iway.iwcm.system.multidomain.DomainRequestBeanScope;

/**
 * Core service for semantic indexing of documents.
 * Orchestrates: content extraction -> chunking -> embedding -> vector storage.
 */
@Service
public class SemanticIndexService {

    private final DocDetailsContentExtractor contentExtractor;
    private final SlidingWindowChunker chunker;
    private final EmbeddingService embeddingService;
    private final PgVectorStore vectorStore;
    private final RagEmbeddingStatService ragEmbeddingStatService;
    private final EmbeddingChunkRepository embeddingChunkRepository;

    private final IndexQueueRepository queueRepository;

    private static final String PROCESSING_QUEUE_KEY = "SemanticIndexService.processQueue.running";

    @Autowired
    public SemanticIndexService(DocDetailsContentExtractor contentExtractor,
                                SlidingWindowChunker chunker,
                                EmbeddingService embeddingService,
                                PgVectorStore vectorStore,
                                IndexQueueRepository queueRepository,
                                RagEmbeddingStatService ragEmbeddingStatService,
                                EmbeddingChunkRepository embeddingChunkRepository) {
        this.contentExtractor = contentExtractor;
        this.chunker = chunker;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;

        this.queueRepository = queueRepository;
        this.ragEmbeddingStatService = ragEmbeddingStatService;
        this.embeddingChunkRepository = embeddingChunkRepository;
    }

    /**
     * Process all pending items in the RAG indexing queue.
     * Items are fetched in batches of 500 and processed sequentially.
     * Uses a cache-based flag to prevent concurrent execution.
     * Failed items remain in the queue and are retried on the next run.
     */
    public void processQueue() {
        Cache cache = Cache.getInstance();

        // Prevent concurrent execution from another thread/scheduler invocation
        if (cache.getObject(PROCESSING_QUEUE_KEY) != null) {
            Logger.debug(SemanticIndexService.class, "Queue processing already running, skipping");
            return;
        }
        // Set flag with 60-minute TTL as a safety net in case of unexpected termination
        cache.setObject(PROCESSING_QUEUE_KEY, Boolean.TRUE, 60);

        try {
            List<IndexQueueEntity> items;
            Set<Integer> readyDomains = new HashSet<>();
            long lastAttemptedQueueId = 0;
            Long runMaxQueueId = queueRepository.findMaxId();
            if (runMaxQueueId == null) return;

            do {
                items = queueRepository.findTop500ByIdGreaterThanAndIdLessThanEqualOrderByIdAsc(
                    lastAttemptedQueueId,
                    runMaxQueueId
                );
                if (items.isEmpty()) break;
                lastAttemptedQueueId = items.get(items.size() - 1).getId();

                List<Long> processedItemIds = new ArrayList<>(items.size());

                // Refresh TTL so the flag doesn't expire during a slow batch (e.g. slow AI provider)
                cache.setObject(PROCESSING_QUEUE_KEY, Boolean.TRUE, 60);

                Logger.println(SemanticIndexService.class, "Processing " + items.size() + " RAG queue items");

                for (IndexQueueEntity item : items) {
                    try {
                        processEntity(item, readyDomains);
                        processedItemIds.add(item.getId());
                    } catch (Exception e) {
                        Logger.error(SemanticIndexService.class, "Error processing queue item " + item.getId() +
                            " (" + item.getEntityType() + "/" + item.getEntityId() + "): " + e.getMessage());
                        // Don't delete failed items - they'll be retried on next run
                    }
                }

                if (processedItemIds.isEmpty() == false) {
                    try {
                        queueRepository.deleteAllByIdInBatch(processedItemIds);
                    } catch (Exception e) {
                        Logger.error(SemanticIndexService.class, "Batch queue cleanup failed, falling back to row-by-row delete: " + e.getMessage());
                        for (Long queueId : processedItemIds) {
                            try {
                                queueRepository.deleteById(queueId);
                            } catch (Exception deleteEx) {
                                Logger.error(SemanticIndexService.class, "Failed to delete processed queue item " + queueId + ": " + deleteEx.getMessage());
                            }
                        }
                    }
                }

            } while (items.size() == 500);
        } finally {
            cache.removeObject(PROCESSING_QUEUE_KEY);
        }
    }

    /**
     * Route a queue item to the appropriate handler based on entity type and action.
     * The queue domain is authoritative because a deleted entity may no longer be available
     * for resolving its domain.
     * @param item queue item to process
     */
    private void processEntity(IndexQueueEntity item, Set<Integer> readyDomains) {
        if (item == null || item.getDomainId() == null || item.getDomainId() < 1) {
            throw new IllegalArgumentException("RAG queue item has invalid domainId");
        }
        if (item.getEntityId() == null || item.getEntityId() < 1) {
            throw new IllegalArgumentException("RAG queue item has invalid entityId");
        }
        if (item.getEntityType() != RagEntityType.DOCUMENT) {
            throw new IllegalArgumentException("Unsupported RAG entity type: " + item.getEntityType());
        }

        int domainId = item.getDomainId();
        int entityId = item.getEntityId();
        if (item.getAction() == RagIndexAction.DELETE) {
            embeddingChunkRepository.deleteByEntityTypeAndEntityIdAndDomainId(
                DocDetailsContentExtractor.ENTITY_TYPE,
                (long) entityId,
                domainId
            );
        } else if (item.getAction() == RagIndexAction.INDEX) {
            indexDocument(entityId, domainId, readyDomains);
        } else {
            throw new IllegalArgumentException("Unsupported RAG index action: " + item.getAction());
        }
    }

    /**
     * Index a document from a DocDetails object.
     */
    private void indexDocument(int docId, int queueDomainId, Set<Integer> readyDomains) {
        DocDetails doc = DocDB.getInstance().getDoc(docId);
        if (doc == null) {
            Logger.debug(SemanticIndexService.class, "Document " + docId + " not found, skipping indexing");
            return;
        }

        String domainName = DocDB.getInstance().getDomain(docId);
        try (DomainRequestBeanScope ignored = DomainRequestBeanScope.open(domainName)) {
            int documentDomainId = GroupsDB.getDomainId(domainName);
            if (documentDomainId < 1) {
                boolean multiDomainMode = InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir");
                if (multiDomainMode) {
                    throw new IllegalStateException(
                        "Cannot resolve domain for RAG document " + docId + ": domainName=" + domainName
                    );
                }
                documentDomainId = 1;
            }
            if (documentDomainId != queueDomainId) {
                throw new IllegalStateException(
                    "RAG queue/document domain mismatch for doc " + docId +
                    ": queueDomainId=" + queueDomainId + ", documentDomainId=" + documentDomainId
                );
            }

            ensureVectorStoreReady(queueDomainId, readyDomains);
            indexDocument(doc, domainName, queueDomainId);
        }
    }

    private void ensureVectorStoreReady(int domainId, Set<Integer> readyDomains) {
        if (readyDomains.contains(domainId)) return;
        if (vectorStore.isAvailable() == false) {
            throw new IllegalStateException("Vector store is not available for domain " + domainId);
        }
        if (vectorStore.isAvailableAndInitialized() == false) {
            // The schema is shared by all domains, so its dimensions, indexes and legacy
            // migration defaults must never depend on which tenant happens to run first.
            try (DomainRequestBeanScope ignored = DomainRequestBeanScope.open(null)) {
                if (vectorStore.initializeSchema() == false) {
                    throw new IllegalStateException("Vector store initialization failed for domain " + domainId);
                }
            }
        }
        readyDomains.add(domainId);
    }

    private void indexDocument(DocDetails doc, String domainName, int domainId) {
        String entityType = DocDetailsContentExtractor.ENTITY_TYPE.name();
        long entityId = doc.getDocId();
        String provider = normalizeProviderId(Constants.getString("ragEmbeddingProvider"));
        String model = Constants.getString("ragEmbeddingModel");
        int dimensions = Constants.getInt("ragEmbeddingDimensions");
        boolean chunkRowsSaved = false;

        try {
            // Step 1: Extract text
            String text = contentExtractor.extractText(doc);
            if (Tools.isEmpty(text)) {
                // Empty document, remove any existing embeddings
                embeddingChunkRepository.deleteByEntityTypeAndEntityIdAndDomainId(DocDetailsContentExtractor.ENTITY_TYPE, entityId, domainId);
                return;
            }

            // Step 2: Chunk
            List<String> chunks = chunker.chunk(text);
            if (chunks.isEmpty()) {
                embeddingChunkRepository.deleteByEntityTypeAndEntityIdAndDomainId(DocDetailsContentExtractor.ENTITY_TYPE, entityId, domainId);
                return;
            }

            AssistantDefinitionEntity embeddingAssistant = ragEmbeddingStatService.getIndexingAssistant(domainId);
            if (embeddingAssistant == null) {
                throw new IllegalStateException("RAG indexing embedding assistant is not available");
            }
            if (Tools.isEmpty(embeddingAssistant.getProvider())) {
                throw new IllegalStateException("RAG indexing embedding assistant has no provider configured");
            }
            if (Tools.isEmpty(embeddingAssistant.getModel())) {
                throw new IllegalStateException("RAG indexing embedding assistant has no model configured");
            }

            provider = normalizeProviderId(embeddingAssistant.getProvider());
            model = embeddingAssistant.getModel();
            dimensions = embeddingService.getDimensions();

            // Step 2.5: Check existing chunks to reuse unchanged embeddings
            Map<String, float[]> existingEmbeddingsByHash = vectorStore.getExistingEmbeddingsByHash(entityType, entityId, provider, model, domainId);

            // Compute hashes for all new chunks and separate into reusable vs needs-embedding
            List<String> chunkHashes = new ArrayList<>();
            List<Integer> chunksToEmbedIndices = new ArrayList<>();
            List<String> chunksToEmbedTexts = new ArrayList<>();
            float[][] resolvedEmbeddings = new float[chunks.size()][];

            for (int i = 0; i < chunks.size(); i++) {
                String hash = sha256(chunks.get(i));
                chunkHashes.add(hash);

                float[] cached = existingEmbeddingsByHash.get(hash);
                if (cached != null && cached.length == dimensions) {
                    // Reuse existing embedding
                    resolvedEmbeddings[i] = cached;
                } else {
                    // Needs new embedding
                    chunksToEmbedIndices.add(i);
                    chunksToEmbedTexts.add(chunks.get(i));
                }
            }

            Logger.debug(SemanticIndexService.class, "Doc " + entityId + ": " + (chunks.size() - chunksToEmbedTexts.size()) +
                " chunks reused, " + chunksToEmbedTexts.size() + " chunks need embedding");

            // Step 3: Embed only changed chunks
            if (chunksToEmbedTexts.isEmpty() == false) {
                EmbeddingBatchResult embeddingResult = embeddingService.embedWithUsage(
                    chunksToEmbedTexts,
                    embeddingAssistant,
                    domainName
                );

                List<float[]> newEmbeddings = embeddingResult.getEmbeddings();
                if (newEmbeddings.size() != chunksToEmbedTexts.size()) {
                    throw new IllegalStateException("Embedding count mismatch for doc " + entityId + ": expected " + chunksToEmbedTexts.size() + ", got " + newEmbeddings.size());
                }

                ragEmbeddingStatService.recordIndexingTokens(embeddingAssistant, embeddingResult.getUsedTokens(), domainId);

                for (int i = 0; i < chunksToEmbedIndices.size(); i++) {
                    resolvedEmbeddings[chunksToEmbedIndices.get(i)] = newEmbeddings.get(i);
                }
            }

            // Step 4: Replace only chunks created by the current provider and model.
            embeddingChunkRepository.deleteByEntityTypeAndEntityIdAndEmbeddingProviderAndEmbeddingModelAndDomainId(
                DocDetailsContentExtractor.ENTITY_TYPE,
                entityId,
                provider,
                model,
                domainId
            );

            String language = GroupMirroringServiceV9.getLanguage(doc.getGroup());

            Date now = new Date();
            List<EmbeddingChunkEntity> chunksToStore = new ArrayList<>(chunks.size());
            for (int i = 0; i < chunks.size(); i++) {
                EmbeddingChunkEntity chunk = new EmbeddingChunkEntity();
                chunk.setEntityType(DocDetailsContentExtractor.ENTITY_TYPE);
                chunk.setEntityId(entityId);
                chunk.setChunkIndex(i);
                chunk.setChunkText(chunks.get(i));
                chunk.setContentHash(chunkHashes.get(i));
                chunk.setEmbeddingProvider(provider);
                chunk.setEmbeddingModel(model);
                chunk.setDimensions(dimensions);
                chunk.setLanguage(language);
                chunk.setDomainId(domainId);
                chunk.setGroupId(doc.getGroupId());
                chunk.setStatus(EmbeddingChunkStatus.COMPLETED);
                chunk.setCreateDate(now);

                //Set root groups
                int[] rootGroups = DocDB.getRootGroupL(doc.getGroupId(), null, -1);
                chunk.setRootGroupL1(rootGroups[0]);
                chunk.setRootGroupL2(rootGroups[1]);
                chunk.setRootGroupL3(rootGroups[2]);

                chunksToStore.add(chunk);
            }

            // Save entities via JPA (without embedding vector)
            List<EmbeddingChunkEntity> savedChunks = embeddingChunkRepository.saveAllAndFlush(chunksToStore);
            chunkRowsSaved = true;

            // Update embedding vectors via native SQL
            List<Long> savedIds = new ArrayList<>(savedChunks.size());
            List<float[]> embeddingsToStore = new ArrayList<>(savedChunks.size());
            for (int i = 0; i < savedChunks.size(); i++) {
                savedIds.add(savedChunks.get(i).getId());
                embeddingsToStore.add(resolvedEmbeddings[i]);
            }
            vectorStore.updateEmbeddingBatch(savedIds, embeddingsToStore);

            Logger.debug(SemanticIndexService.class, "Indexed doc " + entityId + " with " + chunks.size() + " chunks");
        } catch (Exception e) {
            Adminlog.add(Adminlog.TYPE_SEARCH, "Error indexing doc " + e.getMessage(), entityId, null);
            Logger.error(SemanticIndexService.class, "Error indexing doc " + entityId + ": " + e.getMessage());

            try {
                if (chunkRowsSaved == false) {
                    // No chunk rows exist yet, so store one document-level error marker.
                    embeddingChunkRepository.deleteByEntityTypeAndEntityIdAndEmbeddingProviderAndEmbeddingModelAndDomainId(DocDetailsContentExtractor.ENTITY_TYPE, entityId, provider, model, domainId);

                    String truncatedMessage = e.getMessage() != null && e.getMessage().length() > 500
                        ? e.getMessage().substring(0, 500) : e.getMessage();

                    EmbeddingChunkEntity errorChunk = new EmbeddingChunkEntity();
                    errorChunk.setEntityType(DocDetailsContentExtractor.ENTITY_TYPE);
                    errorChunk.setEntityId(entityId);
                    errorChunk.setChunkIndex(0);
                    errorChunk.setChunkText("ERROR");
                    errorChunk.setContentHash("ERROR");
                    errorChunk.setEmbeddingProvider(provider);
                    errorChunk.setEmbeddingModel(model);
                    errorChunk.setDimensions(dimensions);
                    errorChunk.setDomainId(domainId);
                    errorChunk.setStatus(EmbeddingChunkStatus.ERROR);
                    errorChunk.setErrorMessage(truncatedMessage);
                    errorChunk.setCreateDate(new java.util.Date());
                    embeddingChunkRepository.save(errorChunk);
                }
            } catch (Exception markerException) {
                e.addSuppressed(markerException);
                Logger.error(SemanticIndexService.class, "Error storing indexing failure for doc " + entityId + ": " + markerException.getMessage());
            }

            throw new IllegalStateException("Failed to index document " + entityId, e);
        }
    }

    private static String normalizeProviderId(String provider) {
        return Tools.isEmpty(provider) ? "openai" : provider.trim().toLowerCase(Locale.ROOT);
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
