package sk.iway.iwcm.editor.rest;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.ui.ModelMap;

import sk.iway.iwcm.JsonTools;
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

        //inject component response JSON
        String className = event.getSource().getRequest().getParameter("className");
        String componentJson = "null";
        String requestJson = "null";
        String requestJsonTest = "null";
        if (Tools.isNotEmpty(className)) {
            try {
                ComponentRequest componentRequest = new ComponentRequest();
                componentRequest.setClassName(className);
                componentRequest.setParameters(event.getSource().getRequest().getParameter("parameters"));
                componentRequest.setDocId(Integer.parseInt(event.getSource().getRequest().getParameter("docId")));
                componentRequest.setGroupId(Integer.parseInt(event.getSource().getRequest().getParameter("groupId")));
                componentRequest.setPageTitle(event.getSource().getRequest().getParameter("title"));
                componentRequest.setOriginalComponentName(event.getSource().getRequest().getParameter("originalComponentName"));
                componentRequest.setOriginalJspFileName(event.getSource().getRequest().getParameter("originalJspFileName"));

                Map<String, Object> response = ComponentsService.getComponentResponse(componentRequest, event.getSource().getRequest()   );
                componentJson = JsonTools.objectToJSON(response);
                requestJson = JsonTools.objectToJSON(componentRequest);
            } catch (Exception e) {
                Logger.error(ComponentListener.class, e);
            }
        }
        model.addAttribute("componentJson", componentJson);
        model.addAttribute("requestJson", requestJson);
        model.addAttribute("requestJsonTest", requestJsonTest);
    }
}
