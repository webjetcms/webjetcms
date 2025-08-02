package sk.iway.iwcm.components.news.templates.rest;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.ui.ModelMap;

import sk.iway.iwcm.admin.ThymeleafEvent;
import sk.iway.iwcm.components.news.NewsContextMenuItems;
import sk.iway.iwcm.system.spring.events.WebjetEvent;

@Component
public class NewsTemplatesListener {

    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.admin.ThymeleafEvent' && event.source.page=='templates' && event.source.subpage=='news'")
    protected void setInitalData(final WebjetEvent<ThymeleafEvent> event) {

        //Get and set options for context-menu in news-templates
        ModelMap model = event.getSource().getModel();
        model.addAttribute("velocityProperties", NewsContextMenuItems.getVelocityProperties());
        model.addAttribute("docDetailsProperties", NewsContextMenuItems.getDocDetailsProperties());
        model.addAttribute("groupDetailsProperties", NewsContextMenuItems.getGroupDetailsProperties());
        model.addAttribute("pagingProperties", NewsContextMenuItems.getPagingProperties());
    }
}
