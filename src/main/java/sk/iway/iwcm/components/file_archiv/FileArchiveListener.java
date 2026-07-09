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

/**
 * Thymeleaf event listener for the file archive admin page.
 * Populates the model with accepted file extensions and the initial
 * directory tree JSON used for pre-expanding the folder tree on page load.
 */
@Component
public class FileArchiveListener {

    /**
     * Handles the Thymeleaf rendering event for the file-archive index page.
     * Sets model attributes: accepted file extensions and pre-expanded tree JSON
     * based on the requested "dir" parameter.
     * @param event - the WebjetEvent wrapping the ThymeleafEvent
     */
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
            // Set empty defaults so the frontend does not fail on missing model attributes
            ModelMap model = event.getSource().getModel();
            if (!model.containsAttribute("fileArchiveAcceptedFiles")) {
                model.addAttribute("fileArchiveAcceptedFiles", new String[0]);
            }
        }
    }

    /**
     * Builds the initial jsTree JSON structure by expanding all nodes
     * from the root path down to the requested directory.
     * @param dir - the currently selected directory path
     * @param rootPath - the archive root directory path
     * @return list of DirTreeItem nodes to be serialized as JSON for jsTree
     */
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
        rootItem.setIcon("ti ti-folder-filled");
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
                    IwcmFile[] subdirectories = parentDir.listFiles(f -> f.isDirectory());
                    if (subdirectories != null) {
                        for (IwcmFile child : FileTools.sortFilesByName(subdirectories)) {
                            DirTreeItem childItem = new DirTreeItem(child);
                            childItem.setParent(currentPath.toString());
                            childItem.setChildren(hasSubdirectories(child));
                            childItem.setIcon("ti ti-folder-filled");

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

    /**
     * Checks whether the given directory contains any subdirectories.
     * @param directory - the directory to inspect
     * @return true if the directory has at least one child directory
     */
    private boolean hasSubdirectories(IwcmFile directory) {
        IwcmFile[] children = directory.listFiles(f -> f.isDirectory());
        return children != null && children.length > 0;
    }
}
