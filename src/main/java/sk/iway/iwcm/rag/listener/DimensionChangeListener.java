package sk.iway.iwcm.rag.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.components.structuremirroring.SaveListener;
import sk.iway.iwcm.rag.vectorstore.PgVectorStore;
import sk.iway.iwcm.system.ConfDetails;
import sk.iway.iwcm.system.spring.events.WebjetEvent;


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
            if (conf == null) return;

            if ("ragEmbeddingDimensions".equals(conf.getName())) {
                Logger.debug(DimensionChangeListener.class, "conf name=" + conf.getName() + " value=" + conf.getValue());

                // Drop the vector schema to apply new dimensions
                if (vectorStore.dropSchema()) {
                    Logger.info(DimensionChangeListener.class, "Vector schema dropped successfully.");
                } else {
                    Logger.error(DimensionChangeListener.class, "Failed to drop vector schema.");
                }
            }
        } catch (Exception ex) {
            Logger.error(SaveListener.class, ex);
        }
    }
}
