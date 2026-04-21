package sk.iway.iwcm.rag.listener;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.system.spring.events.WebjetEvent;
import sk.iway.iwcm.system.spring.events.WebjetEventType;

/**
 * Listens for document save/delete events and adds entries to the RAG indexing queue.
 * The actual indexing is performed asynchronously by RagIndexCronTask.
 */
@Component
public class DocSaveEventListener {

    private static final String ENTITY_TYPE = "document";

    // Use INSERT IGNORE / ON CONFLICT to avoid duplicates if the same doc is saved multiple times
    // before the cron task processes the queue
    private static final String INSERT_QUEUE_MYSQL =
        "INSERT IGNORE INTO rag_index_queue (entity_type, entity_id, action, domain_id, create_date) VALUES (?, ?, ?, ?, NOW())";

    private static final String INSERT_QUEUE_MSSQL =
        "IF NOT EXISTS (SELECT 1 FROM rag_index_queue WHERE entity_type=? AND entity_id=? AND action=?) " +
        "INSERT INTO rag_index_queue (entity_type, entity_id, action, domain_id, create_date) VALUES (?, ?, ?, ?, GETDATE())";

    private static final String INSERT_QUEUE_PGSQL =
        "INSERT INTO rag_index_queue (entity_type, entity_id, action, domain_id, create_date) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP) " +
        "ON CONFLICT (entity_type, entity_id, action) DO NOTHING";

    private static final String INSERT_QUEUE_ORACLE =
        "MERGE INTO rag_index_queue q USING (SELECT ? entity_type, ? entity_id, ? action FROM dual) s " +
        "ON (q.entity_type=s.entity_type AND q.entity_id=s.entity_id AND q.action=s.action) " +
        "WHEN NOT MATCHED THEN INSERT (entity_type, entity_id, action, domain_id, create_date) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";

    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.doc.DocDetails'")
    public void handleDocEvent(final WebjetEvent<DocDetails> event) {
        if (Constants.getBoolean("ragSemanticSearchEnabled") == false) return;

        DocDetails doc = event.getSource();
        if (doc == null || doc.getDocId() < 1) return;

        int docId = doc.getDocId();
        String domainName = DocDB.getInstance().getDomain(docId);
        int domainId = GroupsDB.getDomainId(domainName);

        try {
            if (event.getEventType() == WebjetEventType.AFTER_SAVE) {
                addToQueue(doc.getDocId(), "INDEX", domainId);
            } else if (event.getEventType() == WebjetEventType.AFTER_DELETE) {
                addToQueue(doc.getDocId(), "DELETE", domainId);
            }
        } catch (Exception e) {
            Logger.error(DocSaveEventListener.class, "Error adding doc " + doc.getDocId() + " to RAG queue: " + e.getMessage());
        }
    }

    private void addToQueue(int docId, String action, int domainId) {
        String sql;
        Object[] params;

        if (Constants.DB_TYPE == Constants.DB_MSSQL) {
            sql = INSERT_QUEUE_MSSQL;
            params = new Object[]{ ENTITY_TYPE, docId, action, ENTITY_TYPE, docId, action, domainId };
        } else if (Constants.DB_TYPE == Constants.DB_ORACLE) {
            sql = INSERT_QUEUE_ORACLE;
            params = new Object[]{ ENTITY_TYPE, docId, action, ENTITY_TYPE, docId, action, domainId };
        } else if (Constants.DB_TYPE == Constants.DB_PGSQL) {
            sql = INSERT_QUEUE_PGSQL;
            params = new Object[]{ ENTITY_TYPE, docId, action, domainId };
        } else {
            // MySQL/MariaDB
            sql = INSERT_QUEUE_MYSQL;
            params = new Object[]{ ENTITY_TYPE, docId, action, domainId };
        }

        new SimpleQuery().execute(sql, params);
    }
}
