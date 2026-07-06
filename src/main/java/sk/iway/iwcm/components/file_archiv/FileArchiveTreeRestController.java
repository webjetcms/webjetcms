package sk.iway.iwcm.components.file_archiv;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.jstree.JsTreeMoveItem;
import sk.iway.iwcm.admin.jstree.JsTreeRestController;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.elfinder.DirTreeItem;

@RestController
@Datatable
@RequestMapping(value = "/admin/rest/components/archive/tree")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_file_archiv')")
public class FileArchiveTreeRestController extends JsTreeRestController<DirTreeItem> {

    @Override
    protected void tree(Map<String, Object> result, JsTreeMoveItem item) {
        String rootPath = normalizeVirtualPath(FileArchivatorKit.getArchivPath());
        String requestedPath = normalizeVirtualPath(item.getId());
        boolean addRoot = isRootRequest(item.getId());

        if (addRoot) {
            requestedPath = rootPath;
        }

        if (Tools.isEmpty(rootPath) || !isWithinRoot(requestedPath, rootPath)) {
            setError(result, "admin.operationPermissionDenied");
            return;
        }

        IwcmFile rootDirectory = IwcmFile.fromVirtualPath(rootPath);
        IwcmFile directory = IwcmFile.fromVirtualPath(requestedPath);
        if (!rootDirectory.exists() || !rootDirectory.isDirectory()
                || !directory.exists() || !directory.isDirectory()) {
            setError(result, "dirAction.err.dirDoesntExist");
            return;
        }

        if (!isWithinCanonicalRoot(directory, rootDirectory)) {
            setError(result, "admin.operationPermissionDenied");
            return;
        }

        if (addRoot) {
            DirTreeItem rootItem = new DirTreeItem(rootDirectory);
            rootItem.setParent("#");
            rootItem.setChildren(hasSubdirectories(rootDirectory, rootDirectory));
            rootItem.getState().setOpened(true);

            result.put("result", true);
            result.put("items", List.of(rootItem));
            return;
        }

        IwcmFile[] directories = directory.listFiles(
                child -> child.isDirectory() && isWithinCanonicalRoot(child, rootDirectory));
        if (directories == null) {
            setError(result, "admin.operationPermissionDenied");
            return;
        }
        List<DirTreeItem> items = new ArrayList<>();
        for (IwcmFile child : FileTools.sortFilesByName(directories)) {
            DirTreeItem treeItem = new DirTreeItem(child);
            treeItem.setChildren(hasSubdirectories(child, rootDirectory));
            items.add(treeItem);
        }

        result.put("result", true);
        result.put("items", items);
    }

    @Override
    protected void save(Map<String, Object> result, DirTreeItem item) {
        setError(result, "admin.operationPermissionDenied");
    }

    @Override
    public boolean checkAccessAllowed(HttpServletRequest request) {
        return FileArchivatorKit.isArchivEnabled(request);
    }

    @Override
    protected void delete(Map<String, Object> result, DirTreeItem item) {
        setError(result, "admin.operationPermissionDenied");
    }

    @Override
    protected void move(Map<String, Object> result, JsTreeMoveItem item) {
        setError(result, "admin.operationPermissionDenied");
    }

    static String normalizeVirtualPath(String path) {
        if (Tools.isEmpty(path)) {
            return "";
        }

        String decodedPath = Tools.URLDecode(path).replace('\\', '/');
        if (!decodedPath.startsWith("/")) {
            decodedPath = "/" + decodedPath;
        }

        try {
            String normalizedPath = Path.of(decodedPath).normalize().toString().replace(File.separatorChar, '/');
            if (!normalizedPath.startsWith("/")) {
                normalizedPath = "/" + normalizedPath;
            }
            if (normalizedPath.length() > 1 && normalizedPath.endsWith("/")) {
                normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
            }
            return normalizedPath;
        } catch (InvalidPathException ex) {
            return "";
        }
    }

    static boolean isWithinRoot(String path, String rootPath) {
        if (Tools.isEmpty(path) || Tools.isEmpty(rootPath)) {
            return false;
        }
        return path.equals(rootPath) || path.startsWith(rootPath + "/");
    }

    private boolean isRootRequest(String id) {
        return Tools.isEmpty(id) || "0".equals(id) || "-1".equals(id) || "#".equals(id);
    }

    private boolean hasSubdirectories(IwcmFile directory, IwcmFile rootDirectory) {
        IwcmFile[] children = directory.listFiles(
                child -> child.isDirectory() && isWithinCanonicalRoot(child, rootDirectory));
        return children != null && children.length > 0;
    }

    private boolean isWithinCanonicalRoot(IwcmFile directory, IwcmFile rootDirectory) {
        String directoryPath = directory.getCanonicalPath();
        String rootPath = rootDirectory.getCanonicalPath();
        if (Tools.isEmpty(directoryPath) || Tools.isEmpty(rootPath)) {
            return false;
        }

        try {
            Path normalizedDirectory = Path.of(directoryPath).normalize();
            Path normalizedRoot = Path.of(rootPath).normalize();
            return normalizedDirectory.equals(normalizedRoot) || normalizedDirectory.startsWith(normalizedRoot);
        } catch (InvalidPathException ex) {
            return false;
        }
    }

    private void setError(Map<String, Object> result, String messageKey) {
        result.put("result", false);
        result.put("error", getProp().getText(messageKey));
    }
}
