package sk.iway.iwcm.rag.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.rag.RagIndexAction;
import sk.iway.iwcm.rag.service.IndexQueueService;
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

    private final IndexQueueService indexQueueService;

    @Autowired
    public DocSaveEventListener(IndexQueueService indexQueueService) {
        this.indexQueueService = indexQueueService;
    }

    /**
     * Handle document save, delete and recover events.
     * On save/recover: adds the document to the indexing queue for re-indexing.
     * On delete: adds the document to the queue for embedding removal.
     * @param event the document event containing the DocDetails source
     */
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
                indexQueueService.addToQueue(doc.getDocId(), ENTITY_TYPE, RagIndexAction.INDEX, domainId);
            } else if (event.getEventType() == WebjetEventType.AFTER_DELETE) {
                // deleted, remove index even when it's soft delete
                indexQueueService.addToQueue(doc.getDocId(), ENTITY_TYPE, RagIndexAction.DELETE, domainId);
            }

        } catch (Exception e) {
            Logger.error(DocSaveEventListener.class, "Error adding doc " + doc.getDocId() + " to RAG queue: " + e.getMessage());
        }
    }
}
