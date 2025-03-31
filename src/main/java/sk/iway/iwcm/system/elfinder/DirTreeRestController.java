package sk.iway.iwcm.system.elfinder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

            items = files.stream().map(f -> new DirTreeItem(f)).collect(Collectors.toList());
        }

        result.put("result", true);
        result.put("items", items);
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
