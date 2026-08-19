package sk.iway.iwcm.system.elfinder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.jstree.JsTreeMoveItem;
import sk.iway.iwcm.admin.jstree.JsTreeRestController;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.FilePathTools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.multidomain.MultiDomainFilter;

/**
 * REST rozhranie pre zobrazenie stromovej struktury v type pola json
 * http://docs.webjetcms.sk/v2021/#/developer/datatables-editor/field-json
 */
@RestController
@RequestMapping(value = "/admin/rest/elfinder/tree")
@PreAuthorize(value = "@WebjetSecurityService.isAdmin()")
public class DirTreeRestController extends JsTreeRestController<DirTreeItem> {

    @Override
    protected void tree(Map<String, Object> result, JsTreeMoveItem item) {

        // /images/gallery -> /images/{domainAlias}/gallery
        String imagesGalleryRoot = Constants.getString("imagesRootDir")+"/"+Constants.getString("galleryDirName");

        //do not use domain alias for when using external dirs
        if (imagesGalleryRoot.equals(item.getRootFolder()) && Constants.getBoolean("multiDomainEnabled") && FilePathTools.isExternalDirs()==false) {
            String domainAlias = MultiDomainFilter.getDomainAlias(DocDB.getDomain(getRequest()));
            if (Tools.isNotEmpty(domainAlias)) {
                if (imagesGalleryRoot.equals(item.getId())) item.setId(Constants.getString("imagesRootDir") + "/" + domainAlias + "/" + Constants.getString("galleryDirName"));
                item.setRootFolder(Constants.getString("imagesRootDir") + "/" + domainAlias + "/" + Constants.getString("galleryDirName"));
            }
        }

        String parentPath = item.getId();
        if ("-1".equals(parentPath)) parentPath = "/";

        boolean isRoot = "/".equals(parentPath);
        Identity user = getUser();
        boolean showOnlyWritableFolders = Constants.getBoolean("fbrowserShowOnlyWritableFolders") && CloudToolsForCore.isControllerDomain() == false;

        List<String> writableFolderRoots = getFolderRoots(user.getWritableFolders());
        if (writableFolderRoots.isEmpty() && showOnlyWritableFolders) {
            writableFolderRoots.addAll(getFolderRoots(Constants.getStringExecuteMacro("fbrowserDefaultWritableFolders")));
        }
        List<String> alwaysShownFolderRoots = getFolderRoots(Constants.getStringExecuteMacro("fbrowserAlwaysShowFolders"));

        List<DirTreeItem> items;

        String click = getRequest().getParameter("click");
        if ("-1".equals(item.getId()) && isRoot && click != null && click.contains("-root") && user.isFolderWritable("/")) {
            //show Root folder for first call (id is sent as -1 instead of / for first request)
            DirTreeItem rootItem = new DirTreeItem(new IwcmFile(Tools.getRealPath("/")));
            rootItem.setId("/");
            rootItem.setText(getProp().getText("stat_settings.group_id"));
            rootItem.setIcon("ti ti-home");
            rootItem.getState().setLoaded(true);
            rootItem.getState().setOpened(true);
            items = new ArrayList<>();
            items.add(rootItem);
        } else {
            List<IwcmFile> files;
            if (showOnlyWritableFolders == false || isFolderVisible(user, parentPath, writableFolderRoots, alwaysShownFolderRoots)) {
                IwcmFile directory = new IwcmFile(Tools.getRealPath(parentPath));
                files = Arrays.asList(FileTools.sortFilesByName(directory.listFiles(file -> {
                    if (file.isFile()) return false;

                    //System.out.println("path="+file.getVirtualPath()+" isRoot="+isRoot+" isJarPackaging="+file.isJarPackaging());
                    if (isRoot==false && file.isJarPackaging()) return false;

                    if (showOnlyWritableFolders && isFolderVisible(user, file.getVirtualPath(), writableFolderRoots, alwaysShownFolderRoots) == false) return false;

                    return true;
                })));
            } else {
                files = new ArrayList<>();
            }

            boolean loadParents = item.getRootFolder() != null && item.getId().equals( item.getRootFolder() );
            items = files.stream().map(f -> new DirTreeItem(f, loadParents)).collect(Collectors.toList());
            for (DirTreeItem dirTreeItem : items) {
                setFolderState(dirTreeItem, user, showOnlyWritableFolders, item.isWritableOnly(), alwaysShownFolderRoots);
            }

            //
            items = getAllowedFolders(items, item);

            //Prepare parents only if we want local root childs
            if(loadParents) {
                prepareParents(parentPath, items, item.isHideRootParents());
                for (DirTreeItem dirTreeItem : items) {
                    if (parentPath.equals(dirTreeItem.getVirtualPath())) {
                        setFolderState(dirTreeItem, user, showOnlyWritableFolders, item.isWritableOnly(), alwaysShownFolderRoots);
                        break;
                    }
                }
            }
        }

        result.put("result", true);
        result.put("items", items);
    }

    /**
     * Prepare parents for the given treeRootPath, up to virtualPath = "/" (including).
     * Add this parents to the items list.
     * @param treeRootPath
     * @param items
     */
    void prepareParents(String treeRootPath, List<DirTreeItem> items, boolean hideRootParents) {
        //If we are in root, we do not need to prepare parents
        if("/".equals(treeRootPath)) return;

        IwcmFile nextParent = null;
        while(true) {

            if(nextParent == null) {
                nextParent = new IwcmFile(Tools.getRealPath(treeRootPath));
            }

            DirTreeItem newItem = new DirTreeItem(nextParent, true);
            newItem.setChildren(true);
            newItem.getState().setOpened(false);
            newItem.getState().setDisabled(true);
            newItem.setParent(nextParent.getVirtualParent());

            if(hideRootParents == true && treeRootPath.equals(nextParent.getVirtualPath())) {
                // Root for us
                newItem.setParent("#");
                items.add(0, newItem);
                break;
            } else if("/".equals(nextParent.getVirtualPath())) {
                //Root
                newItem.setParent("#");
                newItem.setText(getProp().getText("stat_settings.group_id"));
                newItem.setIcon("ti ti-home-x");
                items.add(0, newItem);
                break;
            } else {
                items.add(0, newItem);
                nextParent = nextParent.getParentFile();
            }
        }
    }

    private static void setFolderState(DirTreeItem item, Identity user, boolean showOnlyWritableFolders, boolean writableOnly, List<String> alwaysShownFolderRoots) {
        item.getState().setDisabled(isFolderSelectable(user, item.getVirtualPath(), showOnlyWritableFolders, writableOnly, alwaysShownFolderRoots) == false);
    }

    static boolean isFolderSelectable(Identity user, String path, boolean showOnlyWritableFolders, boolean writableOnly, List<String> alwaysShownFolderRoots) {
        if (user.isFolderWritable(path)) return true;
        if (writableOnly) return false;
        if (showOnlyWritableFolders == false) return true;
        return isPathInFolderRoots(path, alwaysShownFolderRoots);
    }

    static boolean isFolderVisible(Identity user, String path, List<String> writableFolderRoots, List<String> alwaysShownFolderRoots) {
        if (user.isFolderWritable(path)) return true;

        String normalizedPath = normalizeFolderPath(path);
        for (String folderRoot : writableFolderRoots) {
            if (isSameOrSubfolder(folderRoot, normalizedPath)) return true;
        }
        for (String folderRoot : alwaysShownFolderRoots) {
            if (isSameOrSubfolder(normalizedPath, folderRoot) || isSameOrSubfolder(folderRoot, normalizedPath)) return true;
        }
        return false;
    }

    static List<String> getFolderRoots(String folders) {
        List<String> folderRoots = new ArrayList<>();
        for (String folder : Tools.getTokens(folders, ",\n", true)) {
            String normalizedFolder = normalizeFolderPath(folder);
            if (Tools.isNotEmpty(normalizedFolder)) folderRoots.add(normalizedFolder);
        }
        return folderRoots;
    }

    private static boolean isPathInFolderRoots(String path, List<String> folderRoots) {
        String normalizedPath = normalizeFolderPath(path);
        for (String folderRoot : folderRoots) {
            if (isSameOrSubfolder(normalizedPath, folderRoot)) return true;
        }
        return false;
    }

    private static boolean isSameOrSubfolder(String path, String folderRoot) {
        if (path.equals(folderRoot)) return true;
        if ("/".equals(folderRoot)) return path.startsWith("/");
        return path.startsWith(folderRoot + "/");
    }

    private static String normalizeFolderPath(String path) {
        if (path == null) return "";

        String normalizedPath = path.trim().replace('\\', '/');
        while (normalizedPath.endsWith("*") || normalizedPath.endsWith("+")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }
        while (normalizedPath.length() > 1 && normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }
        return Tools.replace(normalizedPath, "//", "/");
    }

    /**
     * Return only allowed folders. Skip folders whose virtualPath contains any of the skipFolders paths.
     * SkipFolders paths are defined in the skipFoldersConst key.
     * @param itemsToCheck
     * @param origItem
     * @return
     */
    public static List<DirTreeItem> getAllowedFolders( List<DirTreeItem> itemsToCheck, JsTreeMoveItem origItem) {
        if(Tools.isNotEmpty(origItem.getSkipFoldersConst())) {
            String skipFoldersString = Constants.getString(origItem.getSkipFoldersConst());
            String[] skipFolders = Tools.getTokens(skipFoldersString, ",\n", true);

            List<DirTreeItem> allowedFolders = new ArrayList<>();
            for(DirTreeItem entity : itemsToCheck) {
                boolean skip = isSkippedFolder(entity.getVirtualPath(), skipFolders);

                if(skip == false && hasOnlySkippedContent(entity, skipFolders) == false) {
                    allowedFolders.add(entity);
                }
            }

            for(DirTreeItem allowedFolder : allowedFolders) {
                allowedFolder.setChildren( hasAllowedChildren(allowedFolder, skipFolders) );
            }

            return allowedFolders;
        }

        return itemsToCheck;
    }

    /**
     * Retun TRUE if at least one child is allowed. Otherwise return FALSE.
     * Child is allowed if his virtualPath do not contains any of the skipFolders paths.
     * @param item - parent folder
     * @param skipFolders - array of folders paths to skip
     * @return
     */
    private static boolean hasAllowedChildren(DirTreeItem item, String[] skipFolders) {
        IwcmFile directory = new IwcmFile(Tools.getRealPath(item.getVirtualPath()));

        IwcmFile[] subfiles = directory.listFiles();
        for (IwcmFile f : subfiles) {
            if (f.isFile()) continue;

            if(isSkippedFolder(f.getVirtualPath(), skipFolders) == false && hasOnlySkippedContent(f, skipFolders) == false) {
                //At least one is allowed - return true
                return true;
            }
        }

        return false;
    }

    /**
     * Return TRUE when folder content has no visible value after applying skipFolders.
     * Files or at least one allowed subfolder keep the parent folder visible.
     * @param item - parent folder
     * @param skipFolders - array of folders paths to skip
     * @return
     */
    private static boolean hasOnlySkippedContent(DirTreeItem item, String[] skipFolders) {
        IwcmFile directory = new IwcmFile(Tools.getRealPath(item.getVirtualPath()));

        return hasOnlySkippedContent(directory, skipFolders);
    }

    private static boolean hasOnlySkippedContent(IwcmFile directory, String[] skipFolders) {

        IwcmFile[] subfiles = directory.listFiles();
        boolean hasSkippedFolder = false;
        for (IwcmFile f : subfiles) {
            if (f.isFile()) return false;

            if(isSkippedFolder(f.getVirtualPath(), skipFolders) || hasOnlySkippedContent(f, skipFolders)) {
                hasSkippedFolder = true;
            } else {
                return false;
            }
        }

        return hasSkippedFolder;
    }

    private static boolean isSkippedFolder(String virtualPath, String[] skipFolders) {
        if(virtualPath.endsWith("/") == false)
            virtualPath += "/";

        for(int i = 0; i < skipFolders.length; i++) {
            if(virtualPath.contains(skipFolders[i])) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void move(Map<String, Object> result, JsTreeMoveItem item) {
        result.put("result", false);
        result.put("error", getProp().getText("components.jstree.access_denied__group"));
    }

    @Override
    protected void save(Map<String, Object> result, DirTreeItem item) {
        result.put("result", false);
        result.put("error", getProp().getText("components.jstree.access_denied__group"));
    }

    @Override
    protected void delete(Map<String, Object> result, DirTreeItem item) {
        save(result, item);
    }

    @Override
    public boolean checkAccessAllowed(HttpServletRequest request) {
        //prava kontrolujeme hore v cykle
        return true;
    }

}
