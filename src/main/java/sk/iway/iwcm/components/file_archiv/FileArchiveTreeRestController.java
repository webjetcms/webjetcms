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
import sk.iway.iwcm.system.elfinder.DirTreeRestController;

/**
 * REST controller providing the jsTree folder structure for the file archive.
 * Supports lazy-loading of subdirectories within the configured archive root path.
 * Directory creation, deletion, and move operations are intentionally disabled.
 */
@RestController
@Datatable
@RequestMapping(value = "/admin/rest/components/archive/tree")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_file_archiv')")
public class FileArchiveTreeRestController extends JsTreeRestController<DirTreeItem> {

    /**
     * Returns the directory tree data for jsTree.
     * For root requests, returns the archive root node with its children flag.
     * For sub-folder requests, returns the immediate child directories.
     * Validates that the requested path is within the archive root to prevent path traversal.
     * @param result - map to populate with "result", "items" or "error"
     * @param item - jsTree move item containing the requested node id
     */
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
            rootItem.setIcon("ti ti-folder-filled");

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
            treeItem.setIcon("ti ti-folder-filled");
            treeItem.setChildren(hasSubdirectories(child, rootDirectory));
            items.add(treeItem);
        }

        //filter items
        JsTreeMoveItem configItem = new JsTreeMoveItem();
        configItem.setSkipFoldersConst("fileArchivInsertLaterDirPath");
        items = DirTreeRestController.getAllowedFolders(items, configItem);

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

    /**
     * Normalizes a virtual path by URL-decoding, converting backslashes to forward slashes,
     * ensuring a leading slash, resolving ".." segments, and removing trailing slashes.
     * @param path - raw virtual path (may be URL-encoded)
     * @return normalized path, or empty string if the path is invalid
     */
    static String normalizeVirtualPath(String path) {
        if (Tools.isEmpty(path)) {
            return "";
        }

        String decodedPath = Tools.URLDecode(path).replace('\\', '/');
        if (!decodedPath.startsWith("/")) {
            decodedPath = "/" + decodedPath; //NOSONAR
        }

        try {
            String normalizedPath = Path.of(decodedPath).normalize().toString().replace(File.separatorChar, '/');
            if (!normalizedPath.startsWith("/")) {
                normalizedPath = "/" + normalizedPath; //NOSONAR
            }
            if (normalizedPath.length() > 1 && normalizedPath.endsWith("/")) {
                normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
            }
            return normalizedPath;
        } catch (InvalidPathException ex) {
            return "";
        }
    }

    /**
     * Checks whether the given path is equal to or a descendant of the root path.
     * @param path - the path to check
     * @param rootPath - the root path boundary
     * @return true if path is within the root
     */
    static boolean isWithinRoot(String path, String rootPath) {
        if (Tools.isEmpty(path) || Tools.isEmpty(rootPath)) {
            return false;
        }
        return path.equals(rootPath) || path.startsWith(rootPath + "/");
    }

    /**
     * Determines if the jsTree request id represents a root-level request.
     * @param id - the jsTree node id
     * @return true for empty, "0", "-1", or "#" ids
     */
    private boolean isRootRequest(String id) {
        return Tools.isEmpty(id) || "0".equals(id) || "-1".equals(id) || "#".equals(id);
    }

    /**
     * Checks whether the given directory has subdirectories that are within the archive root.
     * @param directory - the directory to inspect
     * @param rootDirectory - the archive root directory (used for canonical path validation)
     * @return true if the directory has at least one valid child directory
     */
    private boolean hasSubdirectories(IwcmFile directory, IwcmFile rootDirectory) {
        IwcmFile[] children = directory.listFiles(
                child -> child.isDirectory() && isWithinCanonicalRoot(child, rootDirectory));
        return children != null && children.length > 0;
    }

    /**
     * Validates that the directory's canonical path is within the root directory's canonical path.
     * Prevents path traversal attacks by comparing normalized absolute paths.
     * @param directory - the directory to validate
     * @param rootDirectory - the root directory boundary
     * @return true if the directory is within the root
     */
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

    /**
     * Sets an error response in the result map with a localized message.
     * @param result - the result map to populate
     * @param messageKey - the i18n key for the error message
     */
    private void setError(Map<String, Object> result, String messageKey) {
        result.put("result", false);
        result.put("error", getProp().getText(messageKey));
    }
}
