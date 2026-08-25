package sk.iway.iwcm.rag.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.structuremirroring.SaveListener;
import sk.iway.iwcm.rag.vectorstore.PgVectorStore;
import sk.iway.iwcm.system.ConfDetails;
import sk.iway.iwcm.system.spring.events.WebjetEvent;
import sk.iway.iwcm.system.spring.events.WebjetEventType;


@Component
public class DimensionChangeListener {

    private final PgVectorStore vectorStore;

    @Autowired
    public DimensionChangeListener(PgVectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.system.ConfDetails'")
    public void handleConfSave(final WebjetEvent<ConfDetails> event) {
        try {
            //Logger.debug(SaveListener.class, "================================================= handleConfSave type=" + event.getEventType() + ", source=" + event.getSource().getClass()+" thread="+Thread.currentThread().getName());
            ConfDetails conf = event.getSource();
            if (conf == null || event.getEventType() != WebjetEventType.AFTER_SAVE) return;

            if ("ragEmbeddingDimensions".equals(conf.getName())) {
                Logger.debug(DimensionChangeListener.class, "conf name=" + conf.getName() + " value=" + conf.getValue());

                int dimensions = Tools.getIntValue(conf.getValue(), -1);
                if (vectorStore.resetDimensions(dimensions)) {
                    Logger.info(DimensionChangeListener.class, "All embedding data was deleted and vector dimensions were updated successfully.");
                } else {
                    Logger.error(DimensionChangeListener.class, "Failed to reset embedding data after vector dimensions changed.");
                }
            } else if ("ragSearchDistanceMetric".equals(conf.getName())) {
                Logger.debug(DimensionChangeListener.class, "conf name=" + conf.getName() + " value=" + conf.getValue());

                if (vectorStore.recreateHnswIndex()) {
                    Logger.info(DimensionChangeListener.class, "HNSW index recreated successfully after distance metric change.");
                } else {
                    Logger.error(DimensionChangeListener.class, "Failed to recreate HNSW index after distance metric change.");
                }
            }
        } catch (Exception ex) {
            Logger.error(SaveListener.class, ex);
        }
    }
}
