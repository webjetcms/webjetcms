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

@Service
public class IndexQueueService {

    private final IndexQueueRepository queueRepository;
    private final EmbeddingChunkRepository chunkRepository;

    @Autowired
    public IndexQueueService(IndexQueueRepository queueRepository, EmbeddingChunkRepository chunkRepository) {
        this.queueRepository = queueRepository;
        this.chunkRepository = chunkRepository;
    }

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

    public void addToQueue(int entityId, RagEntityType entityType, RagIndexAction action, int domainId) {
        if(entityId < 1 || entityType == null || action == null || domainId <= 0 ) {
            throw new IllegalArgumentException("Invalid parameters for adding to index queue.");
        }

        removeFromQueue(entityId, entityType, domainId);

        queueRepository.save( preparEntity(entityId, entityType, action, domainId) );
    }

    public List<Integer> getQueued(RagEntityType entityType, RagIndexAction action, Integer domainId) {
        if(entityType == null || action == null || domainId <= 0 ) {
            throw new IllegalArgumentException("Invalid parameters for adding to index queue.");
        }

        return queueRepository.findExistingEntityIds(entityType, action, domainId);
    }

    private List<Integer> filterIdsForRemove(List<Integer> entityIds, RagEntityType entityType, int domainId) {
        List<Integer> entityIdsThatHaveChunks = chunkRepository.findDistinctEntityIdsByEntityTypeAndDomainId(entityType, domainId);
        return entityIds.stream()
                .filter(entityIdsThatHaveChunks::contains)
                .toList();
    }

    private void removeFromQueue(int entityId, RagEntityType entityType, int domainId) {
        queueRepository.deleteByEntityTypeAndEntityId(entityType, entityId, domainId);
    }

    private void removeFromQueue(List<Integer> entityIds, RagEntityType entityType, int domainId) {
        queueRepository.deleteByEntityTypeAndEntityId(entityType, entityIds, domainId);
    }

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