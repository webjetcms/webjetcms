package sk.iway.iwcm.components.file_archiv;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.ui.ModelMap;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.JsonTools;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.ThymeleafEvent;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.elfinder.DirTreeItem;
import sk.iway.iwcm.system.spring.events.WebjetEvent;

@Component
public class FileArchiveListener {

    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.admin.ThymeleafEvent' && event.source.page=='file-archive' && event.source.subpage=='index'")
    private void setData(final WebjetEvent<ThymeleafEvent> event) {
        try {
            ModelMap model = event.getSource().getModel();

            String[] acceptedFiles = Tools.getTokens(Constants.getString("fileArchivAllowExt"), ",");
            model.addAttribute("fileArchiveAcceptedFiles", acceptedFiles);

            String rootPath = FileArchiveTreeRestController.normalizeVirtualPath(FileArchivatorKit.getArchivPath());

            String dir = event.getSource().getRequest().getParameter("dir");
            if (Tools.isEmpty(dir)) {
                dir = rootPath;
            } else {
                dir = FileArchiveTreeRestController.normalizeVirtualPath(dir);
            }

            if (Tools.isNotEmpty(rootPath) && FileArchiveTreeRestController.isWithinRoot(dir, rootPath)) {
                model.addAttribute("treeInitialJson", JsonTools.objectToJSON(getTreeInitialJson(dir, rootPath)));
            }
        } catch (Exception ex) {
            Logger.error(getClass(), ex);
        }
    }

    private List<DirTreeItem> getTreeInitialJson(String dir, String rootPath) {
        List<DirTreeItem> treeInitialJson = new ArrayList<>();

        IwcmFile rootDirectory = IwcmFile.fromVirtualPath(rootPath);
        if (!rootDirectory.exists() || !rootDirectory.isDirectory()) {
            return treeInitialJson;
        }

        // Add root item
        DirTreeItem rootItem = new DirTreeItem(rootDirectory);
        rootItem.setParent("#");
        rootItem.setChildren(hasSubdirectories(rootDirectory));
        rootItem.getState().setOpened(true);
        if (dir.equals(rootPath)) {
            rootItem.getState().setSelected(true);
        }
        treeInitialJson.add(rootItem);

        // Build path segments from root to dir
        if (!dir.equals(rootPath) && dir.startsWith(rootPath + "/")) {
            String relativePath = dir.substring(rootPath.length() + 1);
            String[] segments = relativePath.split("/");
            StringBuilder currentPath = new StringBuilder(rootPath);

            for (int i = 0; i < segments.length; i++) {
                IwcmFile parentDir = IwcmFile.fromVirtualPath(currentPath.toString());
                if (parentDir != null) {
                    IwcmFile[] subdirectories = parentDir.listFiles(IwcmFile::isDirectory);
                    if (subdirectories != null) {
                        for (IwcmFile child : FileTools.sortFilesByName(subdirectories)) {
                            DirTreeItem childItem = new DirTreeItem(child);
                            childItem.setParent(currentPath.toString());
                            childItem.setChildren(hasSubdirectories(child));

                            String childPath = child.getVirtualPath();
                            if (dir.startsWith(childPath + "/") || dir.equals(childPath)) {
                                childItem.getState().setOpened(true);
                            }
                            if (dir.equals(childPath)) {
                                childItem.getState().setSelected(true);
                            }
                            treeInitialJson.add(childItem);
                        }
                    }
                }
                currentPath.append("/").append(segments[i]);
            }
        }

        return treeInitialJson;
    }

    private boolean hasSubdirectories(IwcmFile directory) {
        IwcmFile[] children = directory.listFiles(IwcmFile::isDirectory);
        return children != null && children.length > 0;
    }
}
