package sk.iway.iwcm.rag.service;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.rag.RagIndexAction;
import sk.iway.iwcm.rag.jpa.IndexQueueEntity;
import sk.iway.iwcm.rag.jpa.IndexQueueRepository;
import sk.iway.iwcm.rag.vectorjpa.EmbeddingChunkRepository;

/**
 * Service for managing the RAG indexing queue.
 * Provides methods to add, remove, and query entries in the rag_index_queue table.
 * Entries are processed asynchronously by {@link RagIndexCronTask}.
 * Document queue operations retain integer IDs; Markdown files use {@link MarkdownQueueService}.
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
     * @param entityType the supported queue entity type, {@code DOCUMENT}
     * @param action the action to perform (INDEX or DELETE)
     * @param domainId the domain ID
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public void addToQueue(List<Integer> entityIds, RagEntityType entityType, RagIndexAction action, int domainId) {
        if(entityIds == null || entityIds.stream().anyMatch(id -> id == null || id < 1)
                || entityType != RagEntityType.DOCUMENT || action == null || domainId <= 0 ) {
            throw new IllegalArgumentException("Invalid parameters for adding to index queue.");
        }

        if(entityIds.isEmpty()) return;
        entityIds = entityIds.stream().distinct().toList();
        queueRepository.deleteByEntityTypeAndEntityId(entityType, entityIds.stream().map(Integer::longValue).toList(), domainId);

        if(RagIndexAction.DELETE.equals(action)) entityIds = filterIdsForRemove(entityIds, entityType, domainId);
        if(entityIds.isEmpty()) return;

        queueRepository.saveAll(entityIds.stream().map(id -> prepareEntity(id, entityType, action, domainId)).toList());
    }

    /**
     * Add a single entity to the indexing queue. Removes any existing queue entry for this entity first.
     * @param entityId the entity ID to queue
     * @param entityType the supported queue entity type, {@code DOCUMENT}
     * @param action the action to perform (INDEX or DELETE)
     * @param domainId the domain ID
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public void addToQueue(int entityId, RagEntityType entityType, RagIndexAction action, int domainId) {
        if(entityId < 1 || entityType != RagEntityType.DOCUMENT || action == null || domainId <= 0 ) {
            throw new IllegalArgumentException("Invalid parameters for adding to index queue.");
        }

        queueRepository.deleteByEntityTypeAndEntityId(entityType, (long) entityId, domainId);

        queueRepository.save( prepareEntity(entityId, entityType, action, domainId) );
    }

    /**
     * Get the list of entity IDs currently queued for the given entity type, action and domain.
     * @param entityType the supported queue entity type, {@code DOCUMENT}
     * @param action the action to filter by
     * @param domainId the domain ID to filter by
     * @return list of queued entity IDs
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public List<Integer> getQueued(RagEntityType entityType, RagIndexAction action, Integer domainId) {
        if(entityType != RagEntityType.DOCUMENT || action == null || domainId == null || domainId <= 0 ) {
            throw new IllegalArgumentException("Invalid parameters for getting from index queue.");
        }

        return queueRepository.findExistingEntityIds(entityType, action, domainId).stream().map(Math::toIntExact).toList();
    }

    /**
     * Retains only entity IDs with stored chunks to avoid unnecessary deletion actions.
     *
     * @param entityIds candidate source IDs
     * @param entityType source entity type
     * @param domainId domain whose chunks are checked
     * @return candidate IDs with at least one stored chunk
     */
    private List<Integer> filterIdsForRemove(List<Integer> entityIds, RagEntityType entityType, int domainId) {
        List<Integer> entityIdsThatHaveChunks = chunkRepository.findDistinctEntityIdsByEntityTypeAndDomainId(entityType, domainId);
        return entityIds.stream()
                .filter(entityIdsThatHaveChunks::contains)
                .toList();
    }

    /**
     * Creates a queue entry with the supplied source identity, action, domain, and current timestamp.
     *
     * @param entityId source ID
     * @param entityType source entity type
     * @param action action to perform
     * @param domainId accounting domain
     * @return new unsaved queue entry
     */
    private IndexQueueEntity prepareEntity(int entityId, RagEntityType entityType, RagIndexAction action, int domainId) {
        IndexQueueEntity entity = new IndexQueueEntity();
        entity.setEntityType(entityType);
        entity.setEntityId(entityId);
        entity.setAction(action);
        entity.setDomainId(domainId);
        entity.setCreateDate(new Date());

        return entity;
    }
}
