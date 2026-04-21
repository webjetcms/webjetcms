package sk.iway.iwcm.rag.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.rag.pgvector.RagJpaConfig;
import sk.iway.iwcm.rag.vectorstore.PgVectorStore;
import sk.iway.iwcm.system.spring.components.SpringContext;

/**
 * Cron task that processes the RAG indexing queue.
 * Reads entries from rag_index_queue and indexes/deletes them via SemanticIndexService.
 *
 * Configure in crontab with task: sk.iway.iwcm.rag.service.RagIndexCronTask
 * Recommended schedule: every 5 minutes (minutes: 0/5)
 */
public class RagIndexCronTask {

    private static final int BATCH_SIZE = 100;

    public static void main(String[] args) {
        if (Constants.getBoolean("ragSemanticSearchEnabled") == false) return;

        if (RagJpaConfig.isRagAvailable() == false) {
            Logger.debug(RagIndexCronTask.class, "RAG not available, skipping queue processing");
            return;
        }

        try {
            // Initialize schema on first run
            PgVectorStore vectorStore = SpringContext.getApplicationContext().getBean(PgVectorStore.class);
            if (vectorStore != null && vectorStore.isAvailable() == false) {
                vectorStore.initializeSchema();
            }

            SemanticIndexService indexService = SpringContext.getApplicationContext().getBean(SemanticIndexService.class);
            if (indexService == null) {
                Logger.error(RagIndexCronTask.class, "SemanticIndexService not available");
                return;
            }

            processQueue(indexService);
        } catch (Exception e) {
            Logger.error(RagIndexCronTask.class, "Error processing RAG indexing queue: " + e.getMessage());
        }
    }

    private static void processQueue(SemanticIndexService indexService) {
        List<QueueItem> items = fetchQueueItems();
        if (items.isEmpty()) return;

        Logger.println(RagIndexCronTask.class, "Processing " + items.size() + " RAG queue items");

        for (QueueItem item : items) {
            try {
                if ("DELETE".equals(item.action)) {
                    indexService.deleteDocument(item.entityId);
                } else {
                    indexService.indexDocument(item.entityId);
                }
                // Remove processed item from queue
                deleteQueueItem(item.id);
            } catch (Exception e) {
                Logger.error(RagIndexCronTask.class, "Error processing queue item " + item.id +
                    " (" + item.entityType + "/" + item.entityId + "): " + e.getMessage());
                // Don't delete failed items - they'll be retried on next run
            }
        }
    }

    private static List<QueueItem> fetchQueueItems() {
        List<QueueItem> items = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBPool.getConnection();

            String sql;
            if (Constants.DB_TYPE == Constants.DB_MSSQL) {
                sql = "SELECT TOP " + BATCH_SIZE + " id, entity_type, entity_id, action, domain_id FROM rag_index_queue ORDER BY create_date ASC";
            } else if (Constants.DB_TYPE == Constants.DB_ORACLE) {
                sql = "SELECT id, entity_type, entity_id, action, domain_id FROM rag_index_queue ORDER BY create_date ASC FETCH FIRST " + BATCH_SIZE + " ROWS ONLY";
            } else {
                sql = "SELECT id, entity_type, entity_id, action, domain_id FROM rag_index_queue ORDER BY create_date ASC LIMIT " + BATCH_SIZE;
            }

            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                QueueItem item = new QueueItem();
                item.id = rs.getLong("id");
                item.entityType = rs.getString("entity_type");
                item.entityId = rs.getInt("entity_id");
                item.action = rs.getString("action");
                item.domainId = rs.getInt("domain_id");
                items.add(item);
            }
        } catch (Exception e) {
            Logger.error(RagIndexCronTask.class, "Error fetching queue items: " + e.getMessage());
        } finally {
            try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
        }
        return items;
    }

    private static void deleteQueueItem(long queueItemId) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBPool.getConnection();
            ps = conn.prepareStatement("DELETE FROM rag_index_queue WHERE id = ?");
            ps.setLong(1, queueItemId);
            ps.executeUpdate();
        } catch (Exception e) {
            Logger.error(RagIndexCronTask.class, "Error deleting queue item " + queueItemId + ": " + e.getMessage());
        } finally {
            try
			{
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
        }
    }

    private static class QueueItem {
        long id;
        String entityType;
        int entityId;
        String action;
        int domainId;
    }
}
