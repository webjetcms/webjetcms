package sk.iway.iwcm.editor.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
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
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.AdminTools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.FileBrowserTools;
import sk.iway.iwcm.common.FilePathTools;
import sk.iway.iwcm.common.UploadFileTools;
import sk.iway.iwcm.components.file_archiv.FileArchivatorKit;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocHistoryRepository;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.editor.rest.GroupSchedulerDtoRepository;
import sk.iway.iwcm.editor.service.TrashPageMediaFolders.Folder;
import sk.iway.iwcm.filebrowser.FileReferenceScanner;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmFsDB;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.elfinder.UnusedFileDTO;
import sk.iway.iwcm.system.elfinder.UnusedFilesService;

/** Collects files before permanent page deletion from Trash, then checks their usage once for the completed batch. */
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
    private final TrashPageMediaFolders sourceFolders;
    private final Set<Integer> selectedIds = new HashSet<>();
    private final Map<Integer, Set<String>> candidates = new LinkedHashMap<>();
    private final Set<Integer> deletedFolderPages = new HashSet<>();
    private final Set<String> remainingFolderFiles = new HashSet<>();
    private final Map<String, String> publicPaths = new HashMap<>();
    private final Map<String, Path> mediaRoots = new HashMap<>();
    private final Map<MediaMount, Path> resolvedRoots = new HashMap<>();
    private Map<StorageFolder, Integer> folderOwners;
    private Map<Path, MountInfo> mounts;
    private int errors;

    public PageMediaCleanup(UnusedFilesService unusedFiles, DocHistoryRepository pageHistory, GroupSchedulerDtoRepository groupHistory) {
        this.unusedFiles = unusedFiles;
        this.sourceFolders = new TrashPageMediaFolders(pageHistory, groupHistory);
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
     * Checks the user's file browser and deletion permissions and resolves the trashed page's original location.
     * With external storage, the page must resolve to the active domain's storage root.
     * Multidomain installations using external storage also require a known page domain.
     *
     * @param page page considered for cleanup
     * @param user administrator requesting deletion
     * @return true if the page and user meet these conditions
     */
    public boolean canClean(DocDetails page, Identity user) {
        return cleanableFolder(page, user) != null;
    }

    private Folder cleanableFolder(DocDetails page, Identity user) {
        if (user == null || page == null || user.isDisabledItem("menuFbrowser") || user.isDisabledItem("deletePage")) return null;
        if (!sourceFolders.isInTrash(page.getGroupId())) return null;
        Folder source = sourceFolders.forPage(page);
        if (!canCleanStorage(source)) return null;
        if (Constants.getBoolean("galleryUploadDirVirtualPath")) {
            // An empty stored URL can be generated from the Trash path in the page cache.
            DocDetails stored = DocDB.getInstance().getDoc(page.getDocId());
            if (stored == null || Tools.isEmpty(stored.getVirtualPath())) return null;
        }
        return source;
    }

    private boolean canCleanStorage(Folder source) {
        if (source == null) return false;
        String domain = source.domain();
        if (InitServlet.isTypeCloud() && !CloudToolsForCore.getDomainName().equalsIgnoreCase(domain)) return false;
        if (!FilePathTools.isExternalDirs()) return true;
        if (Constants.getBoolean("multiDomainEnabled") && Tools.isEmpty(domain)) return false;
        try {
            return resolvedRoot(domain, "").equals(resolvedRoot(CloudToolsForCore.getDomainName(), ""));
        } catch (IOException ex) {
            Logger.error(PageMediaCleanup.class, ex);
            return false;
        }
    }

    /** Reads page media before the bulk folder delete removes the original upload paths. */
    public Map<Integer, Set<String>> captureFolderPages(List<GroupDetails> folders, Identity user, Connection connection) {
        Map<Integer, Set<String>> files = new HashMap<>();
        if (folders.isEmpty()) return files;
        boolean needsVirtualPath = Constants.getBoolean("galleryUploadDirVirtualPath");
        String sql = "SELECT doc_id, group_id, virtual_path FROM documents WHERE " + DB.getInCondition("group_id", folders.stream().map(GroupDetails::getGroupId).toList());
        try (var query = connection.prepareStatement(sql); var rows = query.executeQuery()) {
            DocDB documents = DocDB.getInstance();
            while (rows.next()) {
                int id = rows.getInt(1);
                DocDetails page = documents.getBasicDocDetails(id, false);
                if (page == null || page.getGroupId() != rows.getInt(2)) continue;
                try {
                    if (needsVirtualPath && Tools.isEmpty(DB.getDbString(rows, "virtual_path"))) {
                        files.put(id, Set.of());
                        continue;
                    }
                    Folder source = sourceFolders.forPage(page);
                    files.put(id, canCleanStorage(source) ? collectCandidates(page, source, user) : Set.of());
                } catch (IOException | SQLException | RuntimeException ex) {
                    Logger.error(PageMediaCleanup.class, ex);
                }
            }
        } catch (SQLException | RuntimeException ex) {
            Logger.error(PageMediaCleanup.class, ex);
        }
        return files;
    }

    /** Removes unused files after SQL deletion. A later folder in the request can contain the last reference. */
    public void deleteFolderFiles(Map<Integer, Set<String>> files, Identity user) {
        try {
            if (!files.isEmpty()) {
                // A page moved out of the folder during deletion still owns its files.
                String sql = "SELECT doc_id FROM documents WHERE " + DB.getInCondition("doc_id", files.keySet());
                files.keySet().removeAll(new SimpleQuery().forListInteger(sql));
            }
            deletedFolderPages.addAll(files.keySet());
            files.values().forEach(remainingFolderFiles::addAll);
            DeletionResult result = deleteUnusedFiles(remainingFolderFiles, deletedFolderPages, user);
            remainingFolderFiles.removeAll(result.removed());
            if (result.errors() > 0) Logger.error(PageMediaCleanup.class, "Errors removing unused files after folder deletion: " + result.errors());
        } catch (RuntimeException ex) {
            Logger.error(PageMediaCleanup.class, ex);
        }
    }

    /**
     * Collects eligible files before the selected page is permanently removed. In a shared folder,
     * only files referenced by the page or its media records become candidates.
     *
     * @param page page in Trash
     * @param user administrator whose folder permissions apply
     */
    public void capture(DocDetails page, Identity user) {
        int id = page.getDocId();
        if (!selectedIds.contains(id) || candidates.containsKey(id)) return;
        try {
            Folder source = cleanableFolder(page, user);
            if (source != null) candidates.put(id, collectCandidates(page, source, user));
        } catch (Exception ex) {
            errors++;
            Logger.error(PageMediaCleanup.class, ex);
        }
    }

    private Set<String> collectCandidates(DocDetails page, Folder source, Identity user) throws IOException, SQLException {
        String domain = source.domain();
        String alias = AdminTools.getDomainNameFileAliasAppend(domain);
        Map<String, Boolean> directories = new LinkedHashMap<>();
        // Moving a folder's default page to Trash can assign a new default. Its original group folder is shared.
        boolean includeGroupFolder = page.getGroupId() != source.groupId() && Constants.getBoolean("elfinderCreateFolderForPages")
            && !Constants.getBoolean("galleryUploadDirVirtualPath");
        for (String prefix : UploadFileTools.PAGE_MEDIA_PREFIXES) {
            directories.put(folder(page, source, prefix, alias), false);
            if (includeGroupFolder) {
                String base = UploadFileTools.getPageUploadSubDir(-1, source.groupId(), null, prefix, alias, source.urlPath());
                directories.put(base, true);
            }
        }
        Set<String> own = new LinkedHashSet<>();
        Set<String> shared = new LinkedHashSet<>();
        for (var entry : directories.entrySet()) {
            String directory = entry.getKey();
            if (!allowedPath(directory) || !user.isFolderWritable(directory + "/")) continue;
            IwcmFile folder = IwcmFile.fromVirtualPath(directory);
            if (!folder.isDirectory()) continue;
            FolderUse use = folderUse(page, source, directory, alias, entry.getValue());
            if (use == FolderUse.MULTIPLE_URLS) continue;
            IwcmFile[] children = folder.listFiles();
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
            own.addAll(FileReferenceScanner.referencedByPage(shared, publicPaths, page.getDocId(), Tools.getStringValue(domain, CloudToolsForCore.getDomainName())));
        }
        return own;
    }

    /** Files eligible for cleanup under one selected page's full path. */
    public record PageFiles(String fullPath, List<String> files) {}

    /** Checks all eligible pages together so a reference from another selected page does not keep a file. */
    public List<PageFiles> preview(List<DocDetails> pages, Identity user) throws IOException, SQLException {
        List<PageFiles> result = new ArrayList<>();
        Set<Integer> ids = new HashSet<>();
        Set<String> files = new LinkedHashSet<>();
        for (DocDetails page : pages) {
            Folder source = sourceFolders.forPage(page);
            List<String> paths = new ArrayList<>(collectCandidates(page, source, user));
            result.add(new PageFiles(source.fullPath() + "/" + page.getTitle(), paths));
            ids.add(page.getDocId());
            files.addAll(paths);
        }
        Set<String> referenced = FileReferenceScanner.referencedElsewhere(files, publicPaths, ids);
        for (PageFiles page : result) {
            page.files().removeIf(referenced::contains);
            page.files().sort(null);
        }
        return result;
    }

    private FolderUse folderUse(DocDetails page, Folder source, String directory, String alias, boolean groupFolder) throws IOException {
        String domain = source.domain();
        StorageFolder folder = storageFolder(directory, domain);
        FolderUse mount = mountUse(folder.physicalPath(), domain);
        if (mount != FolderUse.EXCLUSIVE) return mount;
        if (groupFolder || (!Constants.getBoolean("elfinderCreateFolderForPages") && !Constants.getBoolean("galleryUploadDirVirtualPath"))
                || UploadFileTools.PAGE_MEDIA_PREFIXES.contains(publicPath(directory, alias))
                || GroupsDB.getInstance().getGroup(source.groupId()).getDefaultDocId() == page.getDocId()) return FolderUse.SHARED;
        if (folderOwners == null) folderOwners = findFolderOwners();
        return Integer.valueOf(page.getDocId()).equals(folderOwners.get(folder)) ? FolderUse.EXCLUSIVE : FolderUse.SHARED;
    }

    /** Maps each upload folder to its sole owner, or -1 when several pages upload there. */
    private Map<StorageFolder, Integer> findFolderOwners() {
        Map<StorageFolder, Integer> owners = new HashMap<>();
        try {
            for (DocDetails page : DocDB.getInstance().getBasicDocDetailsAll()) {
                Folder source = sourceFolders.forPage(page);
                // Without a recoverable folder for every owner, require an explicit page reference.
                if (source == null) return Map.of();
                String domain = source.domain();
                String alias = AdminTools.getDomainNameFileAliasAppend(domain);
                for (String prefix : UploadFileTools.PAGE_MEDIA_PREFIXES) {
                    StorageFolder key = storageFolder(folder(page, source, prefix, alias), domain);
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

    private String folder(DocDetails page, Folder source, String prefix, String alias) {
        String path = UploadFileTools.getPageUploadSubDir(page.getDocId(), source.groupId(), page.getTitle(), prefix, alias, source.urlPath());
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

        DeletionResult result = deleteUnusedFiles(files, deleted, user);
        errorCount += result.errors();
        int removed = result.removed().size();
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

    private record DeletionResult(Set<String> removed, int errors) {}

    private DeletionResult deleteUnusedFiles(Set<String> files, Set<Integer> deleted, Identity user) {
        Set<String> removed = new HashSet<>();
        int errorCount = 0;
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
                    if (allowedPath(path) && unusedFiles.deleteFile(path.replace("+", "%2B"), user)) removed.add(path);
                    else errorCount++;
                }
            } catch (Exception ex) {
                errorCount++;
                Logger.error(PageMediaCleanup.class, ex);
            }
        }

        return new DeletionResult(removed, errorCount);
    }
}
