package sk.iway.iwcm.system.elfinder;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.FileIndexerTools;
import sk.iway.iwcm.filebrowser.UnusedFile;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.users.UsersDB;

@Service
public class UnusedFilesService implements DisposableBean {

    private static final int ACTIVE_TASK_CACHE_SECONDS = 60 * 60;           // 1 hour
    private static final int RESULT_CACHE_SECONDS = 30 * 60;                // 30 minutes
    private static final long MAX_SCAN_DURATION_MILLIS = 30 * 60 * 1000L;   // 30 minutes
    private static final String CACHE_KEY_PREFIX = "unused-files-task-";
    private static final String LATEST_CACHE_KEY_PREFIX = "unused-files-latest-";

    private final ConcurrentMap<String, FolderOperationState> folderOperations = new ConcurrentHashMap<>();
    private final Object folderOperationsLock = new Object();
    private final Object taskCacheLock = new Object();
    // Fixed pool intentionally limits concurrent scans application-wide. Additional scans queue while
    // already holding their folder registration (acquired synchronously in startScan), which blocks
    // deletes on the same folder until the queued scan runs and finishes - acting as backpressure.
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final ScheduledExecutorService timeoutScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "unused-files-scan-timeout");
        t.setDaemon(true);
        return t;
    });

    public void startScan(String taskId, String directory, boolean includeSubfolders, Identity user, RequestBean requestBean) {
        String normalizedTaskId = normalizeTaskId(taskId);
        String normalizedDirectory = resolveDirectory(directory, user);

        UnusedFilesTask task = new UnusedFilesTask();
        task.id = normalizedTaskId;
        task.directory = normalizedDirectory;
        task.domain = getDomain(requestBean);
        task.includeSubfolders = includeSubfolders;
        task.userId = user.getUserId();
        task.requestBean = copyRequestBean(requestBean, user);
        task.state = TaskState.SCANNING;
        task.startedAt = Tools.getNow();
        task.files = Collections.emptyList();

        ScanOperation scanOperation = acquireScanOperation(
            normalizedDirectory,
            includeSubfolders,
            UUID.randomUUID().toString()
        );
        task.scanOperation = scanOperation;

        try {
            cacheNewTask(task);
        } catch (RuntimeException ex) {
            scanOperation.close();
            throw ex;
        }

        try {
            executor.execute(() -> runScan(task));
            timeoutScheduler.schedule(() -> timeoutScan(task), MAX_SCAN_DURATION_MILLIS, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException ex) {
            try {
                failTask(task, ex);
            } finally {
                scanOperation.close();
            }
        }
    }

    public UnusedFilesTaskDTO getStatus(String taskId, Identity user) {
        UnusedFilesTask task = requireTask(taskId);
        ensureTaskAccess(task, user);
        pruneUnavailableFiles(task);
        return toDto(task);
    }

    public UnusedFilesTaskDTO getLatestTask(String directory, Identity user, RequestBean requestBean) {
        String normalizedDirectory = resolveDirectory(directory, user);
        String domain = getDomain(requestBean);
        String latestCacheKey = getLatestCacheKey(user.getUserId(), domain, normalizedDirectory);
        Cache cache = Cache.getInstance();
        String latestTaskId = cache.getObject(latestCacheKey, String.class);

        if (Tools.isEmpty(latestTaskId)) {
            return null;
        }

        UnusedFilesTask task = cache.getObject(getCacheKey(latestTaskId), UnusedFilesTask.class);
        if (task == null || normalizedDirectory.equals(task.directory) == false || domain.equals(task.domain) == false) {
            synchronized (taskCacheLock) {
                if (latestTaskId.equals(cache.getObject(latestCacheKey, String.class))) {
                    cache.removeObject(latestCacheKey);
                }
            }
            return null;
        }

        ensureTaskAccess(task, user);
        pruneUnavailableFiles(task);
        return toDto(task);
    }

    public List<UnusedFileDTO> getResults(String taskId, Identity user) {
        if (Tools.isEmpty(taskId)) {
            return Collections.emptyList();
        }

        UnusedFilesTask task = requireTask(taskId);
        ensureTaskAccess(task, user);
        pruneUnavailableFiles(task);
        if (task.state != TaskState.READY) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        return new ArrayList<>(task.files);
    }

    /**
     * Removes files that are no longer valid readable files from a completed cached scan.
     */
    private void pruneUnavailableFiles(UnusedFilesTask task) {
        if (task.state != TaskState.READY) {
            return;
        }

        synchronized (task) {
            List<UnusedFileDTO> existingFiles = new ArrayList<>();
            for (UnusedFileDTO file : task.files) {
                try {
                    if (FileTools.isFile(file.getFullPath())) {
                        existingFiles.add(file);
                    }
                } catch (Exception ex) {
                    Logger.error(UnusedFilesService.class, ex);
                }
            }

            if (existingFiles.size() != task.files.size()) {
                task.files = Collections.unmodifiableList(existingFiles);
            }
        }
    }

    public DeleteOperation acquireDeleteOperation(Collection<UnusedFileDTO> files, Identity user) {
        if (user == null || files == null || files.isEmpty()) {
            throw deleteValidationError(HttpStatus.BAD_REQUEST);
        }

        Set<String> folders = new LinkedHashSet<>();
        for (UnusedFileDTO file : files) {
            if (file == null) {
                throw deleteValidationError(HttpStatus.BAD_REQUEST);
            }
            String fullPath = validateDeletePath(file.getFullPath(), user);
            folders.add(getParentFolder(fullPath));
        }

        String token = UUID.randomUUID().toString();
        synchronized (folderOperationsLock) {
            for (String folder : folders) {
                FolderOperationState exactState = folderOperations.get(folder);
                if (exactState != null && exactState.deleteToken != null) {
                    throw folderConflict("elfinder.folder_prop.unused_files.delete_conflict");
                }

                for (Map.Entry<String, FolderOperationState> entry : folderOperations.entrySet()) {
                    for (Boolean recursive : entry.getValue().scans.values()) {
                        if (scanCoversFolder(entry.getKey(), recursive.booleanValue(), folder)) {
                            throw folderConflict("elfinder.folder_prop.unused_files.delete_conflict");
                        }
                    }
                }
            }

            for (String folder : folders) {
                FolderOperationState state = folderOperations.computeIfAbsent(folder, key -> new FolderOperationState());
                state.deleteToken = token;
            }
        }

        return new DeleteOperation(token, folders);
    }

    public boolean deleteFile(String fullPath, Identity user) {
        String normalizedFullPath = validateDeletePath(fullPath, user);
        IwcmFile file = new IwcmFile(Tools.getRealPath(normalizedFullPath));

        if (file.exists() == false) {
            return true;
        }
        if (file.isFile() == false) {
            return false;
        }

        try {
            file.delete();
        } catch (Exception ex) {
            Logger.error(UnusedFilesService.class, ex);
            return false;
        }

        // Do not remove the fulltext entry unless the physical file was actually removed.
        if (file.exists()) {
            return false;
        }

        if (normalizedFullPath.startsWith("/files/")) {
            try {
                FileIndexerTools.deleteIndexedFile(normalizedFullPath);
            } catch (Exception ex) {
                Logger.error(UnusedFilesService.class, ex);
            }
        }
        try {
            Adminlog.add(Adminlog.TYPE_FILE_DELETE, user.getUserId(),
                "Unused files delete, path=" + normalizedFullPath, -1, -1);
        } catch (Exception ex) {
            Logger.error(UnusedFilesService.class, ex);
        }
        return true;
    }

    private void runScan(UnusedFilesTask task) {
        RequestBean previousRequestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
        try {
            SetCharacterEncodingFilter.setCurrentRequestBean(task.requestBean);
            List<UnusedFile> unusedFiles = FileTools.getDirFileUsage(task.directory, null, task.includeSubfolders);
            List<UnusedFileDTO> result = new ArrayList<>(unusedFiles.size());

            long id = 1L;
            for (UnusedFile unusedFile : unusedFiles) {
                if (unusedFile == null) {
                    continue;
                }

                UnusedFileDTO unusedFileDTO = UnusedFileDTO.fromUnusedFile(Long.valueOf(id), unusedFile);
                if (FileTools.isFile(unusedFileDTO.getFullPath()) == false) {
                    continue;
                }

                result.add(unusedFileDTO);
                id++;
            }

            // Volatile write ordering: files and finishedAt are written before state (the last
            // volatile write in finishTask). Readers check state first, so by the Java Memory Model
            // volatile guarantee, they see the preceding writes once state becomes READY.
            task.requestBean = null;
            task.files = Collections.unmodifiableList(result);
            task.finishedAt = Long.valueOf(Tools.getNow());
            if (task.completed.compareAndSet(false, true)) {
                finishTask(task, TaskState.READY);
            }
        } catch (Exception ex) {
            if (task.completed.compareAndSet(false, true)) {
                failTask(task, ex);
            }
        } finally {
            try {
                SetCharacterEncodingFilter.setCurrentRequestBean(previousRequestBean);
            } finally {
                task.scanOperation.close();
            }
        }
    }

    private void timeoutScan(UnusedFilesTask task) {
        if (task.completed.compareAndSet(false, true)) {
            try {
                failTask(task, new RuntimeException("Scan exceeded maximum duration of " +
                    (MAX_SCAN_DURATION_MILLIS / 60000) + " minutes"));
            } finally {
                task.scanOperation.close();
            }
        }
    }

    private ScanOperation acquireScanOperation(String directory, boolean includeSubfolders, String token) {
        synchronized (folderOperationsLock) {
            for (Map.Entry<String, FolderOperationState> entry : folderOperations.entrySet()) {
                if (entry.getValue().deleteToken != null && scanCoversFolder(directory, includeSubfolders, entry.getKey())) {
                    throw folderConflict("elfinder.folder_prop.unused_files.scan_conflict");
                }
            }

            FolderOperationState state = folderOperations.computeIfAbsent(directory, key -> new FolderOperationState());
            state.scans.put(token, Boolean.valueOf(includeSubfolders));
        }
        return new ScanOperation(directory, token);
    }

    /**
     * Returns true when a scan of {@code scanDirectory} (optionally recursive) covers {@code folder}.
     * Package-private and static so the conflict-detection logic can be unit tested in isolation.
     */
    static boolean scanCoversFolder(String scanDirectory, boolean includeSubfolders, String folder) {
        if (scanDirectory.equals(folder)) {
            return true;
        }
        if (includeSubfolders == false) {
            return false;
        }
        String prefix = "/".equals(scanDirectory) ? "/" : scanDirectory + "/";
        return folder.startsWith(prefix);
    }

    private String validateDeletePath(String fullPath, Identity user) {
        if (user == null || Tools.isEmpty(fullPath)) {
            throw deleteValidationError(HttpStatus.BAD_REQUEST);
        }

        String normalizedFullPath;
        try {
            normalizedFullPath = normalizeVirtualPath(fullPath);
        } catch (ResponseStatusException ex) {
            throw deleteValidationError(HttpStatus.BAD_REQUEST);
        }
        if ("/".equals(normalizedFullPath) ||
            UsersDB.isFolderWritable(user.getWritableFolders(), normalizedFullPath) == false) {
            throw deleteValidationError(HttpStatus.FORBIDDEN);
        }

        IwcmFile file = new IwcmFile(Tools.getRealPath(normalizedFullPath));
        if (file.exists() && file.isFile() == false) {
            throw deleteValidationError(HttpStatus.BAD_REQUEST);
        }
        return normalizedFullPath;
    }

    /**
     * Returns the parent directory of the given virtual path.
     * Package-private and static so it can be unit tested in isolation.
     */
    static String getParentFolder(String fullPath) {
        int slash = fullPath.lastIndexOf('/');
        if (slash <= 0) {
            return "/";
        }
        return fullPath.substring(0, slash);
    }

    private String resolveDirectory(String directory, Identity user) {
        if (user == null || Tools.isEmpty(directory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        String normalizedDirectory = normalizeVirtualPath(directory);
        String writableDirectory = normalizedDirectory.endsWith("/") ? normalizedDirectory : normalizedDirectory + "/";
        if (user.isFolderWritable(writableDirectory) == false) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        IwcmFile directoryFile = new IwcmFile(Tools.getRealPath(normalizedDirectory));
        if (directoryFile.exists() == false || directoryFile.isDirectory() == false) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return normalizedDirectory;
    }

    /**
     * Normalizes a virtual CMS path: URL-decodes, replaces backslashes, resolves {@code ..}
     * sequences via {@link Path#normalize()}, ensures a leading slash and strips a trailing slash.
     * Package-private and static so it can be unit tested in isolation.
     */
    static String normalizeVirtualPath(String path) {
        try {
            String decodedPath = Tools.URLDecode(path).replace('\\', '/');
            if (decodedPath.startsWith("/") == false) {
                decodedPath = "/" + decodedPath;
            }

            String normalizedPath = Path.of(decodedPath).normalize().toString().replace(File.separatorChar, '/');
            if (normalizedPath.startsWith("/") == false) {
                normalizedPath = "/" + normalizedPath;
            }
            if (normalizedPath.length() > 1 && normalizedPath.endsWith("/")) {
                normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
            }
            return normalizedPath;
        } catch (InvalidPathException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    private void cacheNewTask(UnusedFilesTask task) {
        synchronized (taskCacheLock) {
            Cache cache = Cache.getInstance();
            if (cache.getObject(getCacheKey(task.id)) != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    Prop.getInstance().getText("elfinder.folder_prop.unused_files.start_failed"));
            }
            cache.setObjectSeconds(getCacheKey(task.id), task, ACTIVE_TASK_CACHE_SECONDS, false);
            cache.setObjectSeconds(
                getLatestCacheKey(task.userId, task.domain, task.directory),
                task.id,
                ACTIVE_TASK_CACHE_SECONDS,
                false
            );
        }
    }

    private void finishTask(UnusedFilesTask task, TaskState state) {
        task.state = state;
        Cache cache = Cache.getInstance();
        String cacheKey = getCacheKey(task.id);
        long expiryTime = Tools.getNow() + (RESULT_CACHE_SECONDS * 1000L);
        // Cache holds tasks by reference in-memory, so identity (==) reliably confirms this is still
        // the active entry and we can just shorten its expiry. If a different object is present
        // (task replaced or expired) we re-put this task instead.
        if (cache.getObject(cacheKey) == task) {
            cache.setObjectExpiryTime(cacheKey, expiryTime);
        } else {
            cache.setObjectSeconds(cacheKey, task, RESULT_CACHE_SECONDS, false);
        }

        String latestCacheKey = getLatestCacheKey(task.userId, task.domain, task.directory);
        synchronized (taskCacheLock) {
            if (task.id.equals(cache.getObject(latestCacheKey, String.class))) {
                cache.setObjectExpiryTime(latestCacheKey, expiryTime);
            }
        }
    }

    private void failTask(UnusedFilesTask task, Exception ex) {
        Logger.error(UnusedFilesService.class, ex);
        task.requestBean = null;
        task.finishedAt = Long.valueOf(Tools.getNow());
        finishTask(task, TaskState.FAILED);
    }

    private UnusedFilesTask requireTask(String taskId) {
        String normalizedTaskId = normalizeTaskId(taskId);
        UnusedFilesTask task = Cache.getInstance().getObject(getCacheKey(normalizedTaskId), UnusedFilesTask.class);
        if (task == null) {
            throw new ResponseStatusException(HttpStatus.GONE,
                Prop.getInstance().getText("elfinder.folder_prop.unused_files.expired"));
        }
        return task;
    }

    private void ensureTaskAccess(UnusedFilesTask task, Identity user) {
        String writableDirectory = task.directory.endsWith("/") ? task.directory : task.directory + "/";
        if (user == null || task.userId != user.getUserId() || user.isFolderWritable(writableDirectory) == false) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private String normalizeTaskId(String taskId) {
        if (Tools.isEmpty(taskId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        try {
            return UUID.fromString(taskId).toString();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    private String getCacheKey(String taskId) {
        return CACHE_KEY_PREFIX + taskId;
    }

    private String getLatestCacheKey(int userId, String domain, String directory) {
        return LATEST_CACHE_KEY_PREFIX + userId + ":" + domain + ":" + directory;
    }

    private String getDomain(RequestBean requestBean) {
        if (requestBean == null || Tools.isEmpty(requestBean.getDomain())) {
            return "";
        }
        return requestBean.getDomain().toLowerCase(Locale.ROOT);
    }

    private RequestBean copyRequestBean(RequestBean source, Identity user) {
        RequestBean copy = new RequestBean();
        copy.setUser(user);
        if (source != null) {
            copy.setDomain(source.getDomain());
            copy.setUrl(source.getUrl());
            copy.setRemoteIP(source.getRemoteIP());
            copy.setUserAgent(source.getUserAgent());
        }
        return copy;
    }

    private UnusedFilesTaskDTO toDto(UnusedFilesTask task) {
        UnusedFilesTaskDTO dto = new UnusedFilesTaskDTO();
        dto.setTaskId(task.id);
        dto.setState(task.state.name());
        dto.setDirectory(task.directory);
        dto.setIncludeSubfolders(Boolean.valueOf(task.includeSubfolders));
        dto.setStartedAt(Long.valueOf(task.startedAt));
        dto.setFinishedAt(task.finishedAt);
        dto.setTotalFiles(Integer.valueOf(task.files.size()));
        return dto;
    }

    private ResponseStatusException folderConflict(String translationKey) {
        return new ResponseStatusException(HttpStatus.CONFLICT, Prop.getInstance().getText(translationKey));
    }

    private ResponseStatusException deleteValidationError(HttpStatus status) {
        return new ResponseStatusException(status,
            Prop.getInstance().getText("elfinder.folder_prop.unused_files.delete_invalid"));
    }

    @Override
    public void destroy() {
        timeoutScheduler.shutdownNow();
        executor.shutdownNow();
        synchronized (folderOperationsLock) {
            folderOperations.clear();
        }
    }

    public final class DeleteOperation implements AutoCloseable {
        private final String token;
        private final Set<String> folders;
        private final AtomicBoolean closed = new AtomicBoolean(false);

        private DeleteOperation(String token, Set<String> folders) {
            this.token = token;
            this.folders = Collections.unmodifiableSet(new LinkedHashSet<>(folders));
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true) == false) {
                return;
            }
            synchronized (folderOperationsLock) {
                for (String folder : folders) {
                    FolderOperationState state = folderOperations.get(folder);
                    if (state != null && token.equals(state.deleteToken)) {
                        state.deleteToken = null;
                        removeEmptyFolderState(folder, state);
                    }
                }
            }
        }
    }

    private final class ScanOperation implements AutoCloseable {
        private final String directory;
        private final String token;
        private final AtomicBoolean closed = new AtomicBoolean(false);

        private ScanOperation(String directory, String token) {
            this.directory = directory;
            this.token = token;
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true) == false) {
                return;
            }
            synchronized (folderOperationsLock) {
                FolderOperationState state = folderOperations.get(directory);
                if (state != null) {
                    state.scans.remove(token);
                    removeEmptyFolderState(directory, state);
                }
            }
        }
    }

    private void removeEmptyFolderState(String folder, FolderOperationState state) {
        if (state.deleteToken == null && state.scans.isEmpty()) {
            folderOperations.remove(folder, state);
        }
    }

    private static class FolderOperationState {
        // Maps scan token -> recursive flag. Always accessed under folderOperationsLock.
        private final Map<String, Boolean> scans = new HashMap<>();
        private String deleteToken;
    }

    private static class UnusedFilesTask {
        private String id;
        private String directory;
        private String domain;
        private boolean includeSubfolders;
        private int userId;
        private RequestBean requestBean;
        private ScanOperation scanOperation;
        private final AtomicBoolean completed = new AtomicBoolean(false);
        private volatile TaskState state;
        private volatile long startedAt;
        private volatile Long finishedAt;
        private volatile List<UnusedFileDTO> files;
    }

    private enum TaskState {
        SCANNING,
        READY,
        FAILED
    }
}
