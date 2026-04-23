package sk.iway.iwcm.rag.listener;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.rag.RagIndexAction;
import sk.iway.iwcm.rag.jpa.IndexQueueEntity;
import sk.iway.iwcm.rag.jpa.IndexQueueRepository;
import sk.iway.iwcm.rag.service.RagEntityType;
import sk.iway.iwcm.system.spring.events.WebjetEvent;
import sk.iway.iwcm.system.spring.events.WebjetEventType;

/**
 * Listens for document save/delete events and adds entries to the RAG indexing queue.
 * The actual indexing is performed asynchronously by RagIndexCronTask.
 */
@Component
public class DocSaveEventListener {

    public static final RagEntityType ENTITY_TYPE = RagEntityType.DOCUMENT;

    private final IndexQueueRepository queueRepository;

    @Autowired
    public DocSaveEventListener(IndexQueueRepository queueRepository) {
        this.queueRepository = queueRepository;
    }

    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.doc.DocDetails'")
    public void handleDocEvent(final WebjetEvent<DocDetails> event) {
        if (Constants.getBoolean("ragSemanticSearchEnabled") == false) return;

        DocDetails doc = event.getSource();
        if (doc == null || doc.getDocId() < 1) return;

        int docId = doc.getDocId();
        String domainName = DocDB.getInstance().getDomain(docId);
        int domainId = GroupsDB.getDomainId(domainName);

        try {
            if (event.getEventType() == WebjetEventType.AFTER_SAVE || event.getEventType() == WebjetEventType.AFTER_RECOVER) {
                // updated or recovered, re-index
                addToQueue(doc.getDocId(), RagIndexAction.INDEX, domainId);
            } else if (event.getEventType() == WebjetEventType.AFTER_DELETE) {
                // deleted, remove index even when it's soft delete
                addToQueue(doc.getDocId(), RagIndexAction.DELETE, domainId);
            }

        } catch (Exception e) {
            Logger.error(DocSaveEventListener.class, "Error adding doc " + doc.getDocId() + " to RAG queue: " + e.getMessage());
        }
    }

    private void addToQueue(int docId, RagIndexAction action, int domainId) {
        // Remove any previously queued action for this entity (e.g. INDEX before DELETE)
        queueRepository.deleteByEntityTypeAndEntityId(ENTITY_TYPE, docId, domainId); // Yes docId is unique across domains but this dont need to be case for other entity types, so we need domainId in delete query

        IndexQueueEntity queueItem = new IndexQueueEntity();
        queueItem.setEntityType(ENTITY_TYPE);
        queueItem.setEntityId(docId);
        queueItem.setAction(action.name());
        queueItem.setDomainId(domainId);
        queueItem.setCreateDate(new Date());

        queueRepository.save(queueItem);
    }
}
