package sk.iway.iwcm.components.gallery;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.admin.ThymeleafEvent;
import sk.iway.iwcm.common.FileBrowserTools;
import sk.iway.iwcm.common.GalleryToolsForCore;
import sk.iway.iwcm.system.spring.events.WebjetEvent;

@Component
public class ImageEditorListener {

    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.admin.ThymeleafEvent' && event.source.page=='image-editor' && event.source.subpage=='index'")
    private void setData(final WebjetEvent<ThymeleafEvent> event) {
        try {

            //validate URL parameters, check if it's not gallery image->replace with o_ version
            String dir = event.getSource().getRequest().getParameter("dir");
            String name = event.getSource().getRequest().getParameter("name");

            name = GalleryToolsForCore.getImagePathOriginal(dir+"/"+name);
            name = name.substring(name.lastIndexOf("/")+1);

            //check valid chars
            if (FileBrowserTools.hasForbiddenSymbol(dir) || FileBrowserTools.hasForbiddenSymbol(name)) {
                dir = "";
                name = "";
            }

            event.getSource().getModel().addAttribute("dir", dir);
            event.getSource().getModel().addAttribute("name", name);
        } catch (Exception ex) {
            Logger.error(getClass(), ex);
        }
    }

}
