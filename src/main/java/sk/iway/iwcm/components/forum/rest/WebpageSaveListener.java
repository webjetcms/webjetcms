package sk.iway.iwcm.components.forum.rest;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.system.spring.events.WebjetEvent;
import sk.iway.iwcm.system.spring.events.WebjetEventType;

/**
 * Listener for saving webpages, if there is forum create default forum settings object
 */
@Component
public class WebpageSaveListener {

    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.doc.DocDetails'")
    public void pageSaved(final WebjetEvent<DocDetails> event) {
        if (event.getEventType().equals(WebjetEventType.AFTER_SAVE)) {
            DocDetails saved = event.getSource();
            //create settings for forum after webpage is saved
            ForumGroupService.createForumAfterPage(saved.getDocId(), saved.getData());
        }
    }

}
