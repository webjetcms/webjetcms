package sk.iway.iwcm.components.gallery;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.ThymeleafEvent;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.system.multidomain.MultiDomainFilter;
import sk.iway.iwcm.system.spring.events.WebjetEvent;

@Component
public class GalleryListener {

    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.admin.ThymeleafEvent' && event.source.page=='gallery' && event.source.subpage=='index'")
    private void setData(final WebjetEvent<ThymeleafEvent> event) {
        try {
            //set baseDir to open in jstree
            String baseDir = Constants.getString("imagesRootDir") + "/" + Constants.getString("galleryDirName");
            if (Constants.getString("imagesRootDir").length() > 1) {
                if (Constants.getBoolean("multiDomainEnabled")) {
                    String domainAlias = MultiDomainFilter.getDomainAlias(DocDB.getDomain(event.getSource().getRequest()));
                    if (Tools.isNotEmpty(domainAlias)) {
                        baseDir = Constants.getString("imagesRootDir") + "/" + domainAlias + "/" + Constants.getString("galleryDirName");
                    }
                }
            }

            event.getSource().getModel().addAttribute("baseDir", baseDir);
        } catch (Exception ex) {
            Logger.error(getClass(), ex);
        }
    }

}
