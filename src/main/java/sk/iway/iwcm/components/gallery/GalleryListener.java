package sk.iway.iwcm.components.gallery;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.ui.ModelMap;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.JsonTools;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.ThymeleafEvent;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.system.multidomain.MultiDomainFilter;
import sk.iway.iwcm.system.spring.events.WebjetEvent;

@Component
public class GalleryListener {

    private final GalleryTreeService galleryTreeService;

    public GalleryListener(GalleryTreeService galleryTreeService) {
        this.galleryTreeService = galleryTreeService;
    }

    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.admin.ThymeleafEvent' && event.source.page=='gallery' && event.source.subpage=='index'")
    private void setData(final WebjetEvent<ThymeleafEvent> event) {
        try {
            ModelMap model = event.getSource().getModel();

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

            model.addAttribute("baseDir", baseDir);

            String dir = event.getSource().getRequest().getParameter("dir");
            if (Tools.isEmpty(dir)) dir = baseDir;

            model.addAttribute("treeInitialJson", JsonTools.objectToJSON(getTreeInitialJson(dir)));
        } catch (Exception ex) {
            Logger.error(getClass(), ex);
        }
    }

    /**
     * Recursivelly fill List for tree render
     * @param url
     * @return
     */
    private List<GalleryJsTreeItem> getTreeInitialJson(String url) {
        List<GalleryJsTreeItem> treeInitialJson = new ArrayList<>();
        String[] pathArr = Tools.getTokens(url, "/", true);
        final StringBuilder currentPath = new StringBuilder();
        boolean isRoot = true;
        for (String path : pathArr) {
            currentPath.append("/").append(path);
            List<GalleryJsTreeItem> items = galleryTreeService.getItems(currentPath.toString());
            if (isRoot) {
                items.stream().forEach((item) -> {
                    //root item
                    item.setParent("#");
                });
                isRoot = false;
            } else {
                items.stream().forEach((item) -> {
                    item.setParent(currentPath.toString());
                });
            }
            treeInitialJson.addAll(items);
        }
        //set opened and selected state
        treeInitialJson.stream().forEach((item) -> {
            if (url.startsWith(item.getId())) item.getState().setOpened(true);
            if (url.equals(item.getId())) item.getState().setSelected(true);
        });
        return treeInitialJson;
    }

}
