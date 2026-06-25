package sk.iway.iwcm.rag.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.structuremirroring.GroupMirroringServiceV9;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.rag.RagIndexAction;
import sk.iway.iwcm.rag.embedding.EmbeddingBatchResult;
import sk.iway.iwcm.rag.embedding.EmbeddingProvider;
import sk.iway.iwcm.rag.indexing.DocDetailsContentExtractor;
import sk.iway.iwcm.rag.indexing.SlidingWindowChunker;
import sk.iway.iwcm.rag.jpa.IndexQueueEntity;
import sk.iway.iwcm.rag.jpa.IndexQueueRepository;
import sk.iway.iwcm.rag.pgvector.EmbeddingChunkEntity;
import sk.iway.iwcm.rag.pgvector.EmbeddingChunkRepository;
import sk.iway.iwcm.rag.pgvector.EmbeddingChunkStatus;
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
    private final RagEmbeddingStatService ragEmbeddingStatService;
    private final EmbeddingChunkRepository embeddingChunkRepository;

    private final IndexQueueRepository queueRepository;

    private static final String PROCESSING_QUEUE_KEY = "SemanticIndexService.processQueue.running";

    @Autowired
    public SemanticIndexService(DocDetailsContentExtractor contentExtractor,
                                SlidingWindowChunker chunker,
                                EmbeddingProvider embeddingProvider,
                                PgVectorStore vectorStore,
                                IndexQueueRepository queueRepository,
                                RagEmbeddingStatService ragEmbeddingStatService,
                                EmbeddingChunkRepository embeddingChunkRepository) {
        this.contentExtractor = contentExtractor;
        this.chunker = chunker;
        this.embeddingProvider = embeddingProvider;
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

        if (vectorStore.isAvailable() == false) {
            // If vector store is not available (not allowed or available)
            Logger.error(SemanticIndexService.class, "Vector store NOT available, skipping queue processing");
            return; // cannot do this
        } else if(vectorStore.isAvailableAndInitialized() == false) {
            // Vector store is available but not initialized, we can try to initialize it
            if(vectorStore.initializeSchema() == false) {
                // Inicialization failed, error will be logged by vector store, we cannot do this
                Logger.error(SemanticIndexService.class, "Vector store available BUT initialization failed, skipping queue processing");
                return;
            }
        }

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

            do {
                items = queueRepository.findTop500ByOrderByCreateDateAsc();
                if (items.isEmpty()) break;

                List<Long> processedItemIds = new ArrayList<>(items.size());

                // Refresh TTL so the flag doesn't expire during a slow batch (e.g. slow AI provider)
                cache.setObject(PROCESSING_QUEUE_KEY, Boolean.TRUE, 60);

                Logger.println(SemanticIndexService.class, "Processing " + items.size() + " RAG queue items");

                for (IndexQueueEntity item : items) {
                    try {
                        processEntity(item.getAction(), item.getEntityType(), item.getEntityId());
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
     * @param action the action to perform (INDEX or DELETE)
     * @param entityType the type of entity being processed
     * @param entityId the ID of the entity
     */
    private void processEntity(RagIndexAction action, RagEntityType entityType, int entityId) {
        if (entityType == RagEntityType.DOCUMENT) {
            if (action == RagIndexAction.DELETE) {
                embeddingChunkRepository.deleteByEntityTypeAndEntityId(DocDetailsContentExtractor.ENTITY_TYPE, (long) entityId);
            } else if(action == RagIndexAction.INDEX) {
                indexDocument(entityId);
            } else {
                Logger.error(SemanticIndexService.class, "Unsupported rag index action: " + action);
            }
        } else {
            Logger.error(SemanticIndexService.class, "Unsupported entity type: " + entityType);
        }
    }

    /**
     * Index a document from a DocDetails object.
     */
    private void indexDocument(int docId) {
        DocDetails doc = DocDB.getInstance().getDoc(docId);
        if (doc == null) {
            Logger.debug(SemanticIndexService.class, "Document " + docId + " not found, skipping indexing");
            return;
        }

        String entityType = DocDetailsContentExtractor.ENTITY_TYPE.name();
        long entityId = doc.getDocId();

        try {
            // Step 1: Extract text
            String text = contentExtractor.extractText(doc);
            if (Tools.isEmpty(text)) {
                // Empty document, remove any existing embeddings
                embeddingChunkRepository.deleteByEntityTypeAndEntityId(DocDetailsContentExtractor.ENTITY_TYPE, entityId);
                return;
            }

            // Step 2: Chunk
            List<String> chunks = chunker.chunk(text);
            if (chunks.isEmpty()) {
                embeddingChunkRepository.deleteByEntityTypeAndEntityId(DocDetailsContentExtractor.ENTITY_TYPE, entityId);
                return;
            }

            // Step 2.5: Check existing chunks to reuse unchanged embeddings
            String model = embeddingProvider.getDefaultModel();
            int dimensions = embeddingProvider.getDimensions(model);

            Map<String, float[]> existingEmbeddingsByHash = vectorStore.getExistingEmbeddingsByHash(entityType, entityId, model);

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
                EmbeddingBatchResult embeddingResult = embeddingProvider.embedWithUsage(chunksToEmbedTexts, model);

                List<float[]> newEmbeddings = embeddingResult.getEmbeddings();
                if (newEmbeddings.size() != chunksToEmbedTexts.size()) {
                    throw new IllegalStateException("Embedding count mismatch for doc " + entityId + ": expected " + chunksToEmbedTexts.size() + ", got " + newEmbeddings.size());
                }

                ragEmbeddingStatService.recordIndexingTokens(embeddingResult.getUsedTokens());

                for (int i = 0; i < chunksToEmbedIndices.size(); i++) {
                    resolvedEmbeddings[chunksToEmbedIndices.get(i)] = newEmbeddings.get(i);
                }
            }

            // Step 4: Delete old chunks via repository and save new entities
            embeddingChunkRepository.deleteByEntityTypeAndEntityId(DocDetailsContentExtractor.ENTITY_TYPE, entityId);

            String language = GroupMirroringServiceV9.getLanguage(doc.getGroup());
            String domainName = DocDB.getInstance().getDomain(docId);
            int domainId = GroupsDB.getDomainId(domainName);
            if (domainId < 1) domainId = 1; // for non multi domain web GroupDB returns -1 as domainId, but CloudToolsForCore returns 1

            Date now = new Date();
            List<EmbeddingChunkEntity> chunksToStore = new ArrayList<>(chunks.size());
            for (int i = 0; i < chunks.size(); i++) {
                EmbeddingChunkEntity chunk = new EmbeddingChunkEntity();
                chunk.setEntityType(DocDetailsContentExtractor.ENTITY_TYPE);
                chunk.setEntityId(entityId);
                chunk.setChunkIndex(i);
                chunk.setChunkText(chunks.get(i));
                chunk.setContentHash(chunkHashes.get(i));
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

            // Try get domain id
            int domainId = -1;
            GroupDetails group = doc.getGroup();
            if(group != null && Tools.isNotEmpty(group.getDomainName()) ) domainId = GroupsDB.getDomainId(group.getDomainName());
            if(domainId < 1) domainId = CloudToolsForCore.getDomainId();

            String model = embeddingProvider.getDefaultModel();

            // Delete existing chunks for this entity/model and save error marker via repository
            embeddingChunkRepository.deleteByEntityTypeAndEntityIdAndEmbeddingModel(DocDetailsContentExtractor.ENTITY_TYPE, entityId, model);

            String truncatedMessage = e.getMessage() != null && e.getMessage().length() > 500
                ? e.getMessage().substring(0, 500) : e.getMessage();

            EmbeddingChunkEntity errorChunk = new EmbeddingChunkEntity();
            errorChunk.setEntityType(DocDetailsContentExtractor.ENTITY_TYPE);
            errorChunk.setEntityId(entityId);
            errorChunk.setChunkIndex(0);
            errorChunk.setChunkText("ERROR");
            errorChunk.setContentHash("ERROR");
            errorChunk.setEmbeddingModel(model);
            errorChunk.setDimensions(Constants.getInt("ragEmbeddingDimensions"));
            errorChunk.setDomainId(domainId);
            errorChunk.setStatus(EmbeddingChunkStatus.ERROR);
            errorChunk.setErrorMessage(truncatedMessage);
            errorChunk.setCreateDate(new java.util.Date());
            embeddingChunkRepository.save(errorChunk);
        }
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
