package sk.iway.iwcm.rag.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.rag.RagIndexAction;
import sk.iway.iwcm.rag.jpa.IndexQueueEntity;
import sk.iway.iwcm.rag.jpa.IndexQueueRepository;
import sk.iway.iwcm.rag.pgvector.EmbeddingChunkRepository;

/**
 * Service for managing the RAG indexing queue.
 * Provides methods to add, remove, and query entries in the rag_index_queue table.
 * Entries are processed asynchronously by {@link RagIndexCronTask}.
 */
@Service
public class IndexQueueService {

    private final IndexQueueRepository queueRepository;
    private final EmbeddingChunkRepository chunkRepository;

    @Autowired
    public IndexQueueService(IndexQueueRepository queueRepository, EmbeddingChunkRepository chunkRepository) {
        this.queueRepository = queueRepository;
        this.chunkRepository = chunkRepository;
    }

    /**
     * Add multiple entities to the indexing queue. Removes any existing queue entries for these entities first.
     * For DELETE actions, only entities that actually have stored chunks are queued.
     * @param entityIds list of entity IDs to queue
     * @param entityType the entity type (e.g. DOCUMENT)
     * @param action the action to perform (INDEX or DELETE)
     * @param domainId the domain ID
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public void addToQueue(List<Integer> entityIds, RagEntityType entityType, RagIndexAction action, int domainId) {
        if(entityIds == null || entityType == null || action == null || domainId <= 0 ) {
            throw new IllegalArgumentException("Invalid parameters for adding to index queue.");
        }

        removeFromQueue(entityIds, entityType, domainId);

        if(RagIndexAction.DELETE.equals(action)) entityIds = filterIdsForRemove(entityIds, entityType, domainId);
        if(entityIds.isEmpty()) return;

        List<IndexQueueEntity> queueList = new ArrayList<>();
        for (Integer entityId : entityIds) {
            queueList.add( preparEntity(entityId, entityType, action, domainId) );
        }

        queueRepository.saveAll(queueList);
    }

    /**
     * Add a single entity to the indexing queue. Removes any existing queue entry for this entity first.
     * @param entityId the entity ID to queue
     * @param entityType the entity type (e.g. DOCUMENT)
     * @param action the action to perform (INDEX or DELETE)
     * @param domainId the domain ID
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public void addToQueue(int entityId, RagEntityType entityType, RagIndexAction action, int domainId) {
        if(entityId < 1 || entityType == null || action == null || domainId <= 0 ) {
            throw new IllegalArgumentException("Invalid parameters for adding to index queue.");
        }

        removeFromQueue(entityId, entityType, domainId);

        queueRepository.save( preparEntity(entityId, entityType, action, domainId) );
    }

    /**
     * Get the list of entity IDs currently queued for the given entity type, action and domain.
     * @param entityType the entity type to filter by
     * @param action the action to filter by
     * @param domainId the domain ID to filter by
     * @return list of queued entity IDs
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public List<Integer> getQueued(RagEntityType entityType, RagIndexAction action, Integer domainId) {
        if(entityType == null || action == null || domainId <= 0 ) {
            throw new IllegalArgumentException("Invalid parameters for adding to index queue.");
        }

        return queueRepository.findExistingEntityIds(entityType, action, domainId);
    }

    /**
     * Filter entity IDs to only those that have existing embedding chunks.
     * Used for DELETE actions to avoid unnecessary queue entries.
     */
    private List<Integer> filterIdsForRemove(List<Integer> entityIds, RagEntityType entityType, int domainId) {
        List<Integer> entityIdsThatHaveChunks = chunkRepository.findDistinctEntityIdsByEntityTypeAndDomainId(entityType, domainId);
        return entityIds.stream()
                .filter(entityIdsThatHaveChunks::contains)
                .toList();
    }

    /**
     * Remove a single entity from the queue to prevent duplicate processing.
     */
    private void removeFromQueue(int entityId, RagEntityType entityType, int domainId) {
        queueRepository.deleteByEntityTypeAndEntityId(entityType, entityId, domainId);
    }

    /**
     * Remove multiple entities from the queue to prevent duplicate processing.
     */
    private void removeFromQueue(List<Integer> entityIds, RagEntityType entityType, int domainId) {
        queueRepository.deleteByEntityTypeAndEntityId(entityType, entityIds, domainId);
    }

    /**
     * Create a new IndexQueueEntity with the given parameters and current timestamp.
     */
    private IndexQueueEntity preparEntity(int entityId, RagEntityType entityType, RagIndexAction action, int domainId) {
        IndexQueueEntity entity = new IndexQueueEntity();
        entity.setEntityType(entityType);
        entity.setEntityId(entityId);
        entity.setAction(action);
        entity.setDomainId(domainId);
        entity.setCreateDate(new Date());

        return entity;
    }
}