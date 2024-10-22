package sk.iway.iwcm.components.gallery;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.admin.ThymeleafEvent;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.FileBrowserTools;
import sk.iway.iwcm.common.GalleryToolsForCore;
import sk.iway.iwcm.system.spring.events.WebjetEvent;

@Component
public class ImageEditorListener {

    private GalleryRepository galleryRepository;

    @Autowired
    public ImageEditorListener(GalleryRepository galleryRepository) {
        this.galleryRepository = galleryRepository;
    }

    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.admin.ThymeleafEvent' && event.source.page=='image-editor' && event.source.subpage=='index'")
    private void setData(final WebjetEvent<ThymeleafEvent> event) {
        try {

            //validate URL parameters, check if it's not gallery image->replace with o_ version
            String dir = event.getSource().getRequest().getParameter("dir");
            String name = event.getSource().getRequest().getParameter("name");

            if (dir.startsWith("/thumb/")) dir = dir.substring(6);

            name = GalleryToolsForCore.getImagePathOriginal(dir+"/"+name);
            name = name.substring(name.lastIndexOf("/")+1);

            //check valid chars
            if (FileBrowserTools.hasForbiddenSymbol(dir) || FileBrowserTools.hasForbiddenSymbol(name)) {
                dir = "";
                name = "";
            }

            event.getSource().getModel().addAttribute("dir", dir);
            event.getSource().getModel().addAttribute("name", name);

            //get area of interests
            GalleryEntity entity = galleryRepository.findByImagePathAndImageNameAndDomainId(dir, name, CloudToolsForCore.getDomainId());
            if (entity == null) entity = new GalleryEntity();
            if (entity.getSelectedWidth()==null) entity.setSelectedWidth(-1);
            if (entity.getSelectedHeight()==null) entity.setSelectedHeight(-1);
            if (entity.getSelectedX()==null) entity.setSelectedX(-1);
            if (entity.getSelectedY()==null) entity.setSelectedY(-1);
            event.getSource().getModel().addAttribute("entity", entity);

        } catch (Exception ex) {
            Logger.error(getClass(), ex);
        }
    }

}
