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
import sk.iway.iwcm.io.IwcmFile;

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
        String parentPath = item.getId();
        if ("-1".equals(parentPath)) parentPath = "/";

        boolean isRoot = "/".equals(parentPath);
        Identity user = getUser();

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
            if (isRoot || user.isFolderWritable(parentPath)) {
                IwcmFile directory = new IwcmFile(Tools.getRealPath(parentPath));
                files = Arrays.asList(FileTools.sortFilesByName(directory.listFiles(file -> {
                    if (file.isFile()) return false;

                    //System.out.println("path="+file.getVirtualPath()+" isRoot="+isRoot+" isJarPackaging="+file.isJarPackaging());
                    if (isRoot==false && file.isJarPackaging()) return false;

                    if (user.isFolderWritable(file.getVirtualPath())==false) return false;

                    return true;
                })));
            } else {
                files = new ArrayList<>();
            }

            boolean loadParents = item.getRootFolder() != null && item.getId().equals( item.getRootFolder() );
            items = files.stream().map(f -> new DirTreeItem(f, loadParents)).collect(Collectors.toList());

            //
            items = getAllowedFolders(items, item);

            //Prepare parents only if we want local root childs
            if(loadParents) {
                prepareParents(parentPath, items);
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
    private void prepareParents(String treeRootPath, List<DirTreeItem> items) {
        //Check if we even need to prepare parents
        if(items == null || items.size() == 0) return;

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
            newItem.setIcon("ti ti-folder-x");

            if("/".equals(nextParent.getVirtualPath())) {
                //Root
                newItem.setParent("#");
                items.add(0, newItem);
                break;
            } else {
                items.add(0, newItem);
                nextParent = nextParent.getParentFile();
            }
        }
    }

    /**
     * Return only allowed folders. Skip folders whose virtualPath contains any of the skipFolders paths.
     * SkipFolders paths are defined in the skipFoldersConst key.
     * @param itemsToCheck
     * @param origItem
     * @return
     */
    private List<DirTreeItem> getAllowedFolders( List<DirTreeItem> itemsToCheck, JsTreeMoveItem origItem) {
        if(Tools.isNotEmpty(origItem.getSkipFoldersConst())) {
            String skipFoldersString = Constants.getString(origItem.getSkipFoldersConst());
            String[] skipFolders = Tools.getTokens(skipFoldersString, ",\n", true);

            List<DirTreeItem> allowedFolders = new ArrayList<>();
            for(DirTreeItem entity : itemsToCheck) {
                boolean skip = false;
                String virtualPath = entity.getVirtualPath();
                if(virtualPath.endsWith("/") == false)
                    virtualPath += "/";

                for(int i = 0; i < skipFolders.length; i++) {
                    if(virtualPath.contains(skipFolders[i])) {
                        skip = true;
                        break;
                    }
                }

                if(skip == false) {
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
    private boolean hasAllowedChildren(DirTreeItem item, String[] skipFolders) {
        IwcmFile directory = new IwcmFile(Tools.getRealPath(item.getVirtualPath()));

        IwcmFile[] subfiles = directory.listFiles();
        for (IwcmFile f : subfiles) {
            if (f.isFile()) continue;

            boolean valid = true;
            String virtualPath = f.getVirtualPath();
            if(virtualPath.endsWith("/") == false)
                virtualPath += "/";

            for(int i = 0; i < skipFolders.length; i++) {
                if(virtualPath.contains(skipFolders[i])) {
                    valid = false;
                    break;
                }
            }

            if(valid) {
                //At least one is allowed - return true
                return true;
            }
        }

        return false;
    }

    @Override
    protected void move(Map<String, Object> result, JsTreeMoveItem item) {
        result.put("result", false);
        result.put("error", getProp().getText("components.jstree.access_denied__group"));
        return;
    }

    @Override
    protected void save(Map<String, Object> result, DirTreeItem item) {
        result.put("result", false);
        result.put("error", getProp().getText("components.jstree.access_denied__group"));
        return;
    }

    @Override
    protected void delete(Map<String, Object> result, DirTreeItem item) {
        result.put("result", false);
        result.put("error", getProp().getText("components.jstree.access_denied__group"));
        return;
    }

    @Override
    public boolean checkAccessAllowed(HttpServletRequest request) {
        //prava kontrolujeme hore v cykle
        return true;
    }

}
