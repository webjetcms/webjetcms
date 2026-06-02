package sk.iway.iwcm.rag.service;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.rag.pgvector.PgvectorJpaConfig;

/**
 * Cron task that processes the RAG indexing queue.
 * Reads entries from rag_index_queue and indexes/deletes them via SemanticIndexService.
 *
 * Configure in crontab with task: sk.iway.iwcm.rag.service.RagIndexCronTask
 * Recommended schedule: every 5 minutes (minutes: 0/5)
 */
public class RagIndexCronTask {

    public static void main(String[] args) {
        // isRagAvailable() checks also if "ragSemanticSearchEnabled"
        if (PgvectorJpaConfig.isRagAvailable() == false) {
            Logger.debug(RagIndexCronTask.class, "RAG not available, skipping queue processing");
            return;
        }

        try {
            SemanticIndexService indexService = Tools.getSpringBean("semanticIndexService", SemanticIndexService.class);
            if (indexService == null) {
                Logger.error(RagIndexCronTask.class, "SemanticIndexService not available");
                return;
            }

            indexService.processQueue();

        } catch (Exception e) {
            Logger.error(RagIndexCronTask.class, "Error processing RAG indexing queue: " + e.getMessage());
        }
    }
}
