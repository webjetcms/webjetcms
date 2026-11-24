package sk.iway.iwcm.editor.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.FileBrowserTools;
import sk.iway.iwcm.common.FilePathTools;
import sk.iway.iwcm.common.UploadFileTools;
import sk.iway.iwcm.components.file_archiv.FileArchivatorKit;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.filebrowser.FileReferenceScanner;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmFsDB;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.elfinder.UnusedFileDTO;
import sk.iway.iwcm.system.elfinder.UnusedFilesService;
import sk.iway.iwcm.system.multidomain.MultiDomainFilter;

/** Collects files before selected pages move, then checks their usage once after the selected deletions. */
@Service
@RequestScope
public class PageMediaCleanup {
    /** Only the path for the active storage backend is set. Both paths are case-normalized. */
    private record StorageFolder(Path physicalPath, String databasePath) {}
    /** An empty domain selects the web root. An empty name selects the domain's storage base. */
    private record MediaMount(String domain, String name) {}
    /** A shared root may have hidden cloud owners. Alternate names expose it under both images and files. */
    private record MountInfo(String name, boolean shared, boolean alternateNames) {}
    /**
     * Controls candidate selection. Exclusive folders contribute all eligible files, shared folders
     * require a page reference, and folders exposed under multiple URLs are skipped.
     */
    private enum FolderUse { EXCLUSIVE, SHARED, MULTIPLE_URLS }

    private final UnusedFilesService unusedFiles;
    private final Set<Integer> selectedIds = new HashSet<>();
    private final Map<Integer, Set<String>> candidates = new LinkedHashMap<>();
    private final Map<String, String> publicPaths = new HashMap<>();
    private final Map<String, Path> mediaRoots = new HashMap<>();
    private final Map<Integer, String> groupPaths = new HashMap<>();
    private final Map<Integer, String> groupDomains = new HashMap<>();
    private final Map<MediaMount, Path> resolvedRoots = new HashMap<>();
    private Map<StorageFolder, Integer> folderOwners;
    private Map<Path, MountInfo> mounts;
    private int errors;

    public PageMediaCleanup(UnusedFilesService unusedFiles) {
        this.unusedFiles = unusedFiles;
    }

    /**
     * Registers a page for cleanup before deletion starts.
     *
     * @param id page ID requested for cleanup
     * @return true on the first selection of this ID in the current batch
     */
    public boolean select(int id) {
        return selectedIds.add(id);
    }

    /**
     * Checks the user's file browser and deletion permissions and whether the page is outside Trash.
     * With external storage, the page must resolve to the active domain's storage root.
     * Multidomain installations using external storage also require a known page domain.
     *
     * @param page page considered for cleanup
     * @param user administrator requesting deletion
     * @return true if the page and user meet these conditions
     */
    public boolean canClean(DocDetails page, Identity user) {
        if (user == null || page == null || user.isDisabledItem("menuFbrowser") || user.isDisabledItem("deletePage")) return false;
        if (GroupsDB.getInstance().isInTrash(page.getGroupId())) return false;
        if (!FilePathTools.isExternalDirs()) return true;
        String domain = domainFor(page);
        if (Constants.getBoolean("multiDomainEnabled") && Tools.isEmpty(domain)) return false;
        try {
            return resolvedRoot(domain, "").equals(resolvedRoot(CloudToolsForCore.getDomainName(), ""));
        } catch (IOException ex) {
            Logger.error(PageMediaCleanup.class, ex);
            return false;
        }
    }

    /**
     * Collects eligible files before the selected page's location changes. In a shared folder,
     * only files referenced by the page or its media records become candidates.
     *
     * @param page page in its original folder
     * @param user administrator whose folder permissions apply
     */
    public void capture(DocDetails page, Identity user) {
        int id = page.getDocId();
        if (!selectedIds.contains(id) || candidates.containsKey(id)) return;
        try {
            if (!canClean(page, user)) return;
            String domain = domainFor(page);
            String alias = aliasFor(domain);
            Set<String> directories = new LinkedHashSet<>();
            for (String prefix : UploadFileTools.PAGE_MEDIA_PREFIXES) {
                String path = folder(page, prefix, alias);
                if (allowedPath(path) && user.isFolderWritable(path + "/") && IwcmFile.fromVirtualPath(path).isDirectory()) {
                    directories.add(path);
                }
            }
            Set<String> own = new LinkedHashSet<>();
            Set<String> shared = new LinkedHashSet<>();
            for (String directory : directories) {
                FolderUse use = folderUse(page, directory, alias);
                if (use == FolderUse.MULTIPLE_URLS) continue;
                IwcmFile[] children = IwcmFile.fromVirtualPath(directory).listFiles();
                if (children == null) throw new IOException("Cannot read " + directory);
                for (IwcmFile child : children) {
                    String path = directory + "/" + child.getName();
                    if (!child.isFile() || !allowedPath(path) || !user.isFolderWritable(path)) continue;
                    String url = publicPath(path, alias);
                    if (isReserved(url)) continue;
                    (use == FolderUse.EXCLUSIVE ? own : shared).add(path);
                    publicPaths.put(path, url);
                }
            }
            if (!shared.isEmpty()) {
                own.addAll(FileReferenceScanner.referencedByPage(shared, publicPaths, id, Tools.getStringValue(domain, CloudToolsForCore.getDomainName())));
            }
            candidates.put(id, own);
        } catch (Exception ex) {
            errors++;
            Logger.error(PageMediaCleanup.class, ex);
        }
    }

    private FolderUse folderUse(DocDetails page, String directory, String alias) throws IOException {
        String domain = domainFor(page);
        StorageFolder folder = storageFolder(directory, domain);
        FolderUse mount = mountUse(folder.physicalPath(), domain);
        if (mount != FolderUse.EXCLUSIVE) return mount;
        if ((!Constants.getBoolean("elfinderCreateFolderForPages") && !Constants.getBoolean("galleryUploadDirVirtualPath"))
                || UploadFileTools.PAGE_MEDIA_PREFIXES.contains(publicPath(directory, alias))
                || GroupsDB.getInstance().getGroup(page.getGroupId()).getDefaultDocId() == page.getDocId()) return FolderUse.SHARED;
        if (folderOwners == null) folderOwners = findFolderOwners();
        return Integer.valueOf(page.getDocId()).equals(folderOwners.get(folder)) ? FolderUse.EXCLUSIVE : FolderUse.SHARED;
    }

    /** Maps each upload folder to its sole owner, or -1 when several pages upload there. */
    private Map<StorageFolder, Integer> findFolderOwners() {
        Map<StorageFolder, Integer> owners = new HashMap<>();
        try {
            for (DocDetails page : DocDB.getInstance().getBasicDocDetailsAll()) {
                String domain = domainFor(page);
                String alias = aliasFor(domain);
                for (String prefix : UploadFileTools.PAGE_MEDIA_PREFIXES) {
                    StorageFolder key = storageFolder(folder(page, prefix, alias), domain);
                    owners.merge(key, page.getDocId(), (owner, next) -> owner.equals(next) ? owner : -1);
                }
            }
            return owners;
        } catch (IOException | RuntimeException ex) {
            Logger.error(PageMediaCleanup.class, ex);
            // With incomplete ownership information only individually referenced files qualify.
            return Map.of();
        }
    }

    private String domainFor(DocDetails page) {
        return groupDomains.computeIfAbsent(page.getGroupId(), id -> Tools.getStringValue(GroupsDB.getInstance().getDomain(id), ""));
    }

    private static String aliasFor(String domain) {
        if (!Constants.getBoolean("multiDomainEnabled") || FilePathTools.isExternalDirs()) return "";
        String alias = MultiDomainFilter.getDomainAlias(domain);
        return Tools.isEmpty(alias) ? "" : "/" + alias;
    }

    private StorageFolder storageFolder(String directory, String domain) throws IOException {
        // Database-backed media are keyed by virtual path across domains.
        if (IwcmFsDB.useDBStorage(directory)) return new StorageFolder(null, directory.toLowerCase(Locale.ROOT));
        int slash = directory.indexOf('/', 1);
        String mount = directory.substring(1, slash < 0 ? directory.length() : slash);
        String relative = slash < 0 ? "" : directory.substring(slash + 1);
        // A folder without a domain can be served on the active host.
        if (FilePathTools.isExternalDirs() && domain.isEmpty()) domain = CloudToolsForCore.getDomainName();
        return new StorageFolder(ownershipPath(resolvedRoot(domain, mount).resolve(relative).normalize()), null);
    }

    /** Ancestor mounts can hide owners or expose files under a second public path. */
    private FolderUse mountUse(Path folder, String domain) throws IOException {
        // CloudFilter only exposes documents for the current domain to the ownership scan.
        if (folder == null) return InitServlet.isTypeCloud() ? FolderUse.SHARED : FolderUse.EXCLUSIVE;
        if (mounts == null) mounts = findMounts(domain);
        MountInfo owner = null;
        for (Path part = folder; part != null; part = part.getParent()) {
            MountInfo mount = mounts.get(part);
            if (mount == null) continue;
            // A nested mount or a second URL prefix can expose the file under another public path.
            if (owner != null || mount.alternateNames()) return FolderUse.MULTIPLE_URLS;
            owner = mount;
        }
        return owner != null && owner.shared() && InitServlet.isTypeCloud() ? FolderUse.SHARED : FolderUse.EXCLUSIVE;
    }

    /** Groups the image and file roots of the page domain and configured storage domains by physical location. */
    private Map<Path, MountInfo> findMounts(String domain) throws IOException {
        Map<String, Boolean> domains = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (InitServlet.isTypeCloud() || (FilePathTools.isExternalDirs() && Constants.getBoolean("multiDomainEnabled"))) {
            domains.putAll(GroupsDB.getInstance().getStorageDomainVisibility());
        }
        boolean unknownOwnerDomain = Boolean.FALSE.equals(domains.remove(""));
        domains.putIfAbsent(domain, true);
        Map<Path, MountInfo> names = new HashMap<>();
        for (var host : domains.entrySet()) {
            for (String mount : List.of("images", "files")) {
                Path root = ownershipPath(resolvedRoot(host.getKey(), mount));
                boolean shared = unknownOwnerDomain || !host.getValue();
                names.merge(root, new MountInfo(mount, shared, false), (first, next) ->
                    new MountInfo(first.name(), true, first.alternateNames() || !first.name().equals(next.name())));
            }
        }
        return names;
    }

    private static Path ownershipPath(Path path) {
        // Keep ownership conservative on case-insensitive media volumes.
        return Path.of(path.toString().toLowerCase(Locale.ROOT));
    }

    private Path resolvedRoot(String domain, String mount) throws IOException {
        boolean external = FilePathTools.isExternalDirs();
        MediaMount key = new MediaMount(external ? domain : "", mount);
        Path resolved = resolvedRoots.get(key);
        if (resolved == null) {
            Path path = external ? Path.of(FilePathTools.getDomainBaseFolder(domain)).resolve(mount) : Path.of(Tools.getRealPath("/" + mount));
            resolved = path.toFile().getCanonicalFile().toPath();
            resolvedRoots.put(key, resolved);
        }
        return resolved;
    }

    private String folder(DocDetails page, String prefix, String alias) {
        String groupPath = groupPaths.computeIfAbsent(page.getGroupId(), id -> GroupsDB.getInstance().getURLPath(id));
        String path = UploadFileTools.getPageUploadSubDir(page.getDocId(), page.getGroupId(), page.getTitle(), prefix, alias, groupPath);
        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    /** Removes the storage alias only when it is the first segment below the media root. */
    private static String publicPath(String path, String alias) {
        if (!alias.isEmpty()) {
            for (String root : List.of("/images", "/files")) {
                if (path.equals(root + alias) || path.startsWith(root + alias + "/")) return root + path.substring(root.length() + alias.length());
            }
        }
        return path;
    }

    /** Excludes reserved paths and symlinks below the media root, and detects root changes after capture. */
    private boolean allowedPath(String path) throws IOException {
        String directory = path + "/";
        if (!(directory.startsWith("/images/") || directory.startsWith("/files/")) || path.contains("/.")
                || path.contains("//") || path.contains("\\") || !path.equals(path.stripTrailing()) || FileBrowserTools.hasForbiddenSymbol(path)) return false;
        if (isReserved(path)) return false;
        String virtualRoot = directory.substring(0, directory.indexOf('/', 1));
        Path physical = Path.of(Tools.getRealPath(path));
        Path root = Path.of(Tools.getRealPath(virtualRoot));
        if (!physical.startsWith(root)) return false;
        for (Path part = physical; !part.equals(root); part = part.getParent()) {
            if (Files.isSymbolicLink(part)) return false;
        }
        // The root itself may be a symlink. Its target must stay stable until deletion.
        Path resolvedRoot = root.toFile().getCanonicalFile().toPath();
        Path capturedRoot = mediaRoots.putIfAbsent(virtualRoot, resolvedRoot);
        return capturedRoot == null || capturedRoot.equals(resolvedRoot);
    }

    private static boolean isReserved(String path) {
        String directory = (path + "/").toLowerCase(Locale.ROOT);
        String archive = "/" + FileArchivatorKit.getArchivPath().replaceAll("^/+|/+$", "").toLowerCase(Locale.ROOT) + "/";
        return directory.startsWith(archive) || directory.contains("/protected/");
    }

    /**
     * Deletes unreferenced candidates from selected pages whose deletion completed in this request.
     * Checks remaining references while holding the file deletion lock and consumes the selection.
     *
     * @param user administrator whose file permissions apply
     * @param prop translations for the cleanup summary
     * @param interrupted whether the batch stopped on an error and the summary should identify completed pages
     * @return a summary of removed files, retained files and errors, or {@code null} when there is nothing to report
     */
    public NotifyBean finish(Identity user, Prop prop, boolean interrupted) {
        if (selectedIds.isEmpty()) return null;
        List<Integer> completed = selectedIds.stream().filter(id -> DeleteRequestContext.isDeleted(DocDetails.class, id)).sorted().toList();
        selectedIds.clear();
        Set<Integer> deleted = new HashSet<>();
        Set<String> files = new LinkedHashSet<>();
        for (int id : completed) {
            Set<String> paths = candidates.get(id);
            if (paths == null) continue;
            deleted.add(id);
            files.addAll(paths);
        }
        candidates.clear();
        int errorCount = errors;
        errors = 0;
        if (deleted.isEmpty() && errorCount == 0) return null;

        int removed = 0;
        if (!files.isEmpty() && user != null && user.isEnabledItem("menuFbrowser")) {
            List<UnusedFileDTO> locked = new ArrayList<>();
            for (String path : files) {
                UnusedFileDTO file = new UnusedFileDTO();
                file.setFullPath(path.replace("+", "%2B"));
                locked.add(file);
            }
            try (var operation = unusedFiles.acquireDeleteOperation(locked, user)) {
                Set<String> referenced = FileReferenceScanner.referencedElsewhere(files, publicPaths, deleted);
                for (String path : files) {
                    if (referenced.contains(path)) continue;
                    if (allowedPath(path) && unusedFiles.deleteFile(path.replace("+", "%2B"), user)) removed++;
                    else errorCount++;
                }
            } catch (Exception ex) {
                errorCount++;
                Logger.error(PageMediaCleanup.class, ex);
            }
        }

        String text = prop.getText("editor.delete_files.summary", String.valueOf(removed), String.valueOf(files.size() - removed),
            String.valueOf(errorCount));
        if (interrupted && !completed.isEmpty()) {
            String ids = String.join(", ", completed.stream().map(String::valueOf).toList());
            text = prop.getText("editor.delete_files.completed_pages", ids) + " " + text;
        }
        String title = prop.getText("elfinder.actualPage");
        if (errorCount == 0 && !interrupted) {
            return new NotifyBean(title, text, NotifyBean.NotifyType.INFO, 15000);
        }
        return new NotifyBean(title, text, NotifyBean.NotifyType.WARNING);
    }
}
