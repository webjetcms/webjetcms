package sk.iway.iwcm.rag.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.rag.vectorstore.VectorStore;
import sk.iway.iwcm.system.ConfDetails;
import sk.iway.iwcm.system.spring.events.WebjetEvent;
import sk.iway.iwcm.system.spring.events.WebjetEventType;


@Component
public class DimensionChangeListener {

    private final VectorStore vectorStore;

    @Autowired
    public DimensionChangeListener(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.system.ConfDetails'")
    public void handleConfSave(final WebjetEvent<ConfDetails> event) {
        ConfDetails conf = event.getSource();
        if (conf == null || event.getEventType() != WebjetEventType.AFTER_SAVE) return;

        try {
            if ("ragEmbeddingDimensions".equals(conf.getName())) {
                Logger.debug(DimensionChangeListener.class, "conf name=" + conf.getName() + " value=" + conf.getValue());

                int dimensions = Tools.getIntValue(conf.getValue(), -1);
                boolean success = vectorStore.resetDimensions(dimensions);
                Adminlog.add(Adminlog.TYPE_SEARCH,
                    "RAG embedding reset and vector index rebuild: ragEmbeddingDimensions=" + dimensions +
                    ", success=" + success, -1, -1);
                if (success) {
                    Logger.info(DimensionChangeListener.class, "All embedding data was deleted and vector dimensions were updated successfully.");
                } else {
                    Logger.error(DimensionChangeListener.class, "Failed to reset embedding data after vector dimensions changed.");
                }
            } else if ("ragSearchDistanceMetric".equals(conf.getName())) {
                Logger.debug(DimensionChangeListener.class, "conf name=" + conf.getName() + " value=" + conf.getValue());

                boolean success = vectorStore.recreateIndex();
                Adminlog.add(Adminlog.TYPE_SEARCH,
                    "RAG vector index rebuild: ragSearchDistanceMetric=" + conf.getValue() +
                    ", success=" + success, -1, -1);
                if (success) {
                    Logger.info(DimensionChangeListener.class, "Vector index recreated successfully after distance metric change.");
                } else {
                    Logger.error(DimensionChangeListener.class, "Failed to recreate vector index after distance metric change.");
                }
            }
        } catch (Exception ex) {
            Logger.error(DimensionChangeListener.class, ex);
            Adminlog.add(Adminlog.TYPE_SEARCH,
                "RAG vector index rebuild failed: " + conf.getName() + "=" + conf.getValue() +
                ", success=false, error=" + ex.getMessage(), -1, -1);
        }
    }
}
