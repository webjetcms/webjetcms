package sk.iway.iwcm.editor.rest;

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.ui.ModelMap;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.ThymeleafEvent;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.spring.events.WebjetEvent;

/**
 * Add data for component.pug to extend it with custom HTML code
 */
@Component
public class ComponentListener {

    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.admin.ThymeleafEvent' && event.source.page=='webpages' && event.source.subpage=='component'")
    protected void setInitalData(final WebjetEvent<ThymeleafEvent> event) {
        ModelMap model = event.getSource().getModel();
        String includePath = null;
        try {

            HttpServletRequest request = event.getSource().getRequest();

            String className = request.getParameter("name");
            if (Tools.isNotEmpty(className)) {
                Class<?> cl = Class.forName(className);
                if (cl.isAnnotationPresent(WebjetAppStore.class)) {
                    WebjetAppStore appStore = cl.getAnnotation(WebjetAppStore.class);
                    includePath = appStore.customHtml();
                }
            }

        } catch (Exception e) {
            Logger.error(ComponentListener.class, e);
        }

        if (Tools.isEmpty(includePath)) {
            includePath = "/apps/admin/editor-component.html";
        }
        model.addAttribute("appIncludePath", includePath);
    }
}
