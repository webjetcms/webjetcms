package sk.iway.iwcm.doc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.media.MediaRepository;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmFsDB;
import sk.iway.spirit.model.Media;

/**
 * Prepares, validates, publishes and cancels a page's planned files and media.
 *
 * <p>Page revisions remain in the existing history table. Planned media are stored
 * separately under {@link #PLANNED_MEDIA_TABLE}, while the page's initialization flag
 * distinguishes an intentionally empty media set from an uninitialized one.</p>
 *
 * <p>The editor and {@link DocPublishService} control page saves and history completion.
 * Callers enforce page-edit permissions; this service checks destination-folder rights
 * before publishing files. Database writes and file replacements are not one transaction:
 * retryable steps retain planned source data until their live replacements are saved.</p>
 */
@Service
public class PlannedChangesService {
    public static final String PLANNED_MEDIA_TABLE = "planned_documents";
    private final DocDetailsRepository docDetailsRepository;
    private final DocHistoryRepository docHistoryRepository;
    private final MediaRepository mediaRepository;

    public PlannedChangesService(DocDetailsRepository docDetailsRepository, DocHistoryRepository docHistoryRepository, MediaRepository mediaRepository) {
        this.docDetailsRepository = docDetailsRepository;
        this.docHistoryRepository = docHistoryRepository;
        this.mediaRepository = mediaRepository;
    }

    public static PlannedChangesService getInstance() {
        return Tools.getSpringBean("plannedChangesService", PlannedChangesService.class);
    }

    /**
     * Requires an active planned media set before accepting a media operation.
     *
     * @param docId page ID whose initialization flag is checked
     * @throws IllegalArgumentException if planned publishing is disabled or the media set is uninitialized
     */
    public void requirePlannedMedia(int docId) {
        if (!PlannedAccess.enabled() || !docDetailsRepository.isPlannedMediaInitialized(docId)) {
            throw new IllegalArgumentException("Planned media are no longer active; reopen the editor");
        }
    }

    /**
     * Validates an individual planned media edit without inspecting its linked files.
     *
     * <p>Requires initialized planned media and rejects private asset references in
     * localized titles and the media group. File checks run when saving or publishing
     * the page's plan.</p>
     *
     * @param item media values submitted for editing
     * @param docId page ID owning the planned media set
     * @throws IllegalArgumentException if the media set is inactive or a restricted field contains a planned reference
     */
    public void validateMedia(Media item, int docId) {
        requirePlannedMedia(docId);
        rejectFields(item, "mediaTitleSk", "mediaTitleCz", "mediaTitleEn", "mediaTitleDe", "mediaGroup");
    }

    /**
     * Finds the newest history record still marked for publication.
     *
     * @param docId page ID whose pending revisions are searched
     * @return highest pending history ID, or {@code null} when no pending revision exists
     */
    public Long activeHistory(int docId) {
        List<DocHistory> pending = docHistoryRepository.findByDocIdAndPublicableTrueOrderByIdDesc(docId);
        return pending.isEmpty() ? null : pending.get(0).getId();
    }

    /**
     * Initializes planned media from the page's live media once, then returns its pending history ID.
     *
     * <p>Media are scoped to the current domain. Before an uninitialized copy is attempted,
     * any incomplete planned set is deleted. The initialization flag is written only after
     * every copy is saved, allowing a failed attempt to restart from the live set.
     * An already initialized set, including an empty one, is left unchanged.</p>
     *
     * @param docId page ID whose planned media should be initialized
     * @return newest pending history ID, or {@code null} if no revision is scheduled
     * @throws IllegalArgumentException if planned publishing is disabled
     */
    public Long initialize(int docId) {
        if (!PlannedAccess.enabled()) throw new IllegalArgumentException("Planned publishing is disabled");
        if (!docDetailsRepository.isPlannedMediaInitialized(docId)) {
            int domain = CloudToolsForCore.getDomainId();
            deleteMedia(docId, PLANNED_MEDIA_TABLE, domain);
            for (Media original : mediaRepository.findAllByMediaFkIdAndMediaFkTableNameAndDomainId(docId, "documents", domain)) {
                mediaRepository.saveAndFlush(copyMedia(original, PLANNED_MEDIA_TABLE));
            }
            docDetailsRepository.setPlannedMediaInitialized((long) docId, true);
        }
        return activeHistory(docId);
    }

    /**
     * Cancels pending revisions and removes the page's planned media and working files.
     *
     * <p>Clears publication and approval flags on pending history records, deletes planned
     * media in the current domain and clears the initialization flag. Live content and
     * publication backups are retained. Earlier writes are not rolled back if cleanup fails.</p>
     *
     * @param docId page ID whose planned changes should be cancelled
     * @throws IllegalStateException if planned working files cannot be deleted
     */
    public void cancel(int docId) {
        for (DocHistory pending : docHistoryRepository.findByDocIdAndPublicableTrueOrderByIdDesc(docId)) {
            pending.setPublicable(false);
            pending.setAwaitingApprove(null);
            pending.setSyncStatus(2);
            docHistoryRepository.save(pending);
        }
        deleteMedia(docId, PLANNED_MEDIA_TABLE, CloudToolsForCore.getDomainId());
        docDetailsRepository.setPlannedMediaInitialized((long) docId, false);
        deletePlannedFiles(docId);
    }

    /**
     * Prepares and validates an edited revision before the editor saves its scheduled history.
     *
     * <p>Requires a saved page, enabled history, a publication date and, when present,
     * the active history ID. Initializes planned media and restores the live page's URL
     * and folder on the supplied object. Checks restricted fields, references after a simulated
     * rewrite, all staged files, destination permissions and conflicts with other plans.</p>
     *
     * <p>The existing plan is not cancelled here. Initialization and changes to the
     * supplied object can already have happened when a later validation fails.</p>
     *
     * @param doc edited page revision, modified to retain the live URL and folder
     * @param user editor whose write permissions are checked for public destination folders
     * @throws IllegalArgumentException if the revision, files, permissions or destination conflicts prevent planning
     * @throws IllegalStateException if planned working directories cannot be read
     */
    public void prepareSave(DocDetails doc, Identity user) {
        if (doc.getDocId() < 1 || Constants.getBoolean("editorDisableHistory")) {
            throw new IllegalArgumentException("Save an unpublished page before preparing a planned change; history must be enabled");
        }
        Long active = initialize(doc.getDocId());
        if (active != null && active.intValue() != doc.getHistoryId()) {
            throw new IllegalArgumentException("Edit the existing planned version from page history (" + active + ")");
        }
        if (doc.getPublishStart() <= 0) throw new IllegalArgumentException("A publication date is required");
        DocDetails live = docDetailsRepository.findById(doc.getDocId());
        doc.setVirtualPath(live.getVirtualPath());
        doc.setGroupId(live.getGroupId());
        List<Media> planned = plannedMedia(doc.getDocId());
        validateReferenceFields(doc, planned);
        if (hasPrivateReferences(doc, doc.getDocId()) || planned.stream().anyMatch(item -> hasPrivateReferences(item, doc.getDocId()))) {
            throw new IllegalArgumentException(Prop.getInstance().getText("planned.unrewritableReferences"));
        }
        Map<String, String> files = plannedFiles(doc.getDocId());
        preflight(files, user);
        checkConflicts(doc.getDocId(), files);
    }

    /**
     * Deactivates older pending revisions after the replacement history has been saved.
     *
     * @param saved persisted replacement revision; pending records with the same or a higher ID are retained
     */
    public void completeSave(DocHistory saved) {
        for (DocHistory old : docHistoryRepository.findByDocIdAndPublicableTrueOrderByIdDesc(saved.getDocId())) {
            if (old.getId() >= saved.getId()) continue;
            old.setPublicable(false);
            old.setSyncStatus(2);
            docHistoryRepository.save(old);
        }
    }

    private List<Media> plannedMedia(int id) {
        return mediaRepository.findAllByMediaFkIdAndMediaFkTableNameAndDomainId(id, PLANNED_MEDIA_TABLE, CloudToolsForCore.getDomainId());
    }

    /**
     * Rejects planned asset references in page and media fields that are not rewritten on publication.
     *
     * @param doc page revision whose routing and title fields are checked
     * @param planned media whose localized titles and group names are checked
     * @throws IllegalArgumentException if a restricted field contains a planned reference
     */
    private void validateReferenceFields(DocDetails doc, List<Media> planned) {
        rejectFields(doc, "title", "navbar", "externalLink", "virtualPath", "editorVirtualPath", "fileName");
        for (Media item : planned) rejectFields(item, "mediaTitleSk", "mediaTitleCz", "mediaTitleEn", "mediaTitleDe", "mediaGroup");
    }

    /**
     * Replaces this page's planned prefixes in publishable content fields in memory.
     *
     * <p>Covers body and head content, perex fields and custom fields A through T.
     * The caller persists the modified page or history record.</p>
     *
     * @param doc page or history object whose content should use public asset paths
     */
    void rewriteContent(DocBasic doc) {
        rewriteProperties(doc, doc.getDocId(), "data", "dataAsc", "htmlData", "htmlHead", "perexImage", "perexPlace");
        for (char field = 'A'; field <= 'T'; field++) rewriteProperties(doc, doc.getDocId(), "field" + field);
    }

    /**
     * Checks selected bean properties for planned references after decoding HTML entities.
     *
     * @param bean object exposing the named readable properties
     * @param fields property names that must not contain planned references
     * @throws IllegalArgumentException if any selected value contains a planned reference
     */
    private void rejectFields(Object bean, String... fields) {
        BeanWrapper wrapper = new BeanWrapperImpl(bean);
        for (String field : fields) {
            if (PlannedAssetPaths.containsPlannedPath(org.jsoup.parser.Parser.unescapeEntities(String.valueOf(wrapper.getPropertyValue(field)), false))) {
                throw new IllegalArgumentException("Unsupported planned reference in " + field);
            }
        }
    }

    /**
     * Prevents private planned references from entering live data through ordinary editing or restoration.
     *
     * @param bean object whose persisted string properties are inspected
     * @throws IllegalArgumentException with a localized message if a planned reference is found
     */
    public static void rejectPrivateReferences(Object bean) {
        if (hasPrivateReferences(bean)) throw new IllegalArgumentException(Prop.getInstance().getText("planned.privateReferencesNotAllowed"));
    }

    /**
     * Detects planned references in readable string fields annotated with {@code @Column}.
     *
     * <p>Inspects inherited fields as well as fields declared on the supplied class.
     * HTML entities are decoded before matching. Unmapped computed properties are
     * skipped to avoid invoking getters that can perform unrelated database work.</p>
     *
     * @param bean object whose persisted string properties are inspected
     * @return {@code true} if any inspected value contains a planned-directory reference
     */
    public static boolean hasPrivateReferences(Object bean) {
        return hasPrivateReferences(bean, 0);
    }

    /**
     * Checks persisted strings after a simulated prefix rewrite without modifying the bean.
     *
     * @param bean page or media whose persisted string fields are inspected
     * @param docId page whose planned prefix may be rewritten; zero checks the original values
     * @return whether any planned reference remains after the optional rewrite
     */
    private static boolean hasPrivateReferences(Object bean, int docId) {
        BeanWrapper wrapper = new BeanWrapperImpl(bean);
        // Inspect only persisted fields; computed bean getters can perform unrelated database work.
        for (Class<?> type = bean.getClass(); type != null; type = type.getSuperclass()) {
            for (java.lang.reflect.Field field : type.getDeclaredFields()) {
                if (field.getType() != String.class || !field.isAnnotationPresent(jakarta.persistence.Column.class) ||
                    !wrapper.isReadableProperty(field.getName())) continue;
                String value = (String) wrapper.getPropertyValue(field.getName());
                if (docId > 0) value = PlannedAssetPaths.rewrite(value, docId);
                if (PlannedAssetPaths.containsPlannedPath(org.jsoup.parser.Parser.unescapeEntities(String.valueOf(value), false))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Rewrites one page's planned prefixes in the selected string properties.
     *
     * @param bean object whose properties are updated in memory
     * @param docId page ID identifying the prefix to replace
     * @param fields readable and writable string properties to rewrite
     */
    private void rewriteProperties(Object bean, int docId, String... fields) {
        BeanWrapper wrapper = new BeanWrapperImpl(bean);
        for (String field : fields) {
            String value = PlannedAssetPaths.rewrite((String)wrapper.getPropertyValue(field), docId);
            wrapper.setPropertyValue(field, value);
        }
    }

    /**
     * Rejects public destinations already used by another pending page's staged files.
     *
     * <p>With external static-file storage enabled, only plans in the current domain
     * are compared. This check does not lock other plans against concurrent changes.</p>
     *
     * @param docId page being checked, excluded from the comparison
     * @param files mapping from planned source paths to public destination paths
     * @throws IllegalArgumentException if another plan uses the same destination or contains an invalid asset path
     * @throws IllegalStateException if another plan's working files cannot be read
     */
    private void checkConflicts(int docId, Map<String, String> files) {
        if (files.isEmpty()) return;
        for (DocHistory pending : docHistoryRepository.findByPublicableTrue()) {
            if (pending.getDocId() == docId) continue;
            String domain = GroupsDB.getInstance().getGroup(pending.getGroupId()).getDomainName();
            if (!domain.equalsIgnoreCase(CloudToolsForCore.getDomainName()) && Constants.getBoolean("enableStaticFilesExternalDir")) continue;
            Map<String, String> other = plannedFiles(pending.getDocId());
            if (other.values().stream().anyMatch(files::containsValue)) {
                throw new IllegalArgumentException("Another active planned page uses the same public asset: " + pending.getDocId());
            }
        }
    }

    /**
     * Checks file access and storage prerequisites before any staged file is published.
     *
     * <p>Requires writable destination folders for the user, readable regular source files
     * and filesystem storage instead of database storage. The nearest existing destination
     * ancestor must be writable and on the source file's filesystem for atomic replacement.</p>
     *
     * @param files mapping from planned source paths to public destination paths
     * @param user user whose public-folder write permissions are checked
     * @throws IllegalArgumentException if a file, permission or storage check fails
     */
    private void preflight(Map<String, String> files, Identity user) {
        for (Map.Entry<String, String> entry : files.entrySet()) {
            try {
                if (!user.isFolderWritable(entry.getValue().substring(0, entry.getValue().lastIndexOf('/') + 1))) throw new IOException("Asset access denied");
                if (IwcmFsDB.useDBStorage(entry.getKey()) || IwcmFsDB.useDBStorage(entry.getValue())) throw new IOException("Planned assets require shared filesystem storage");
                Path source = Path.of(Tools.getRealPath(entry.getKey()));
                Path target = Path.of(Tools.getRealPath(entry.getValue()));
                if (!Files.isRegularFile(source) || !Files.isReadable(source) || Files.isDirectory(target)) throw new IOException("Missing or invalid planned asset");
                Path parent = target.getParent();
                while (!Files.exists(parent)) parent = parent.getParent();
                if (!Files.isWritable(parent) || !Files.getFileStore(parent).equals(Files.getFileStore(source))) throw new IOException("Asset destination must be writable and on the staging filesystem");
            } catch (IOException exception) {
                throw new IllegalArgumentException("Cannot publish asset " + entry.getValue(), exception);
            }
        }
    }

    /**
     * Publishes staged files and rewrites content before the regular publisher saves the page.
     *
     * <p>Validates restricted fields, destination permissions and conflicts first.
     * Source files remain available for retry; a failure after some replacements does
     * not roll back those public files. This method does not save the page or its media.</p>
     *
     * @param doc revision to publish, modified in memory to use public asset paths
     * @param user user whose public-folder write permissions are checked
     * @throws IllegalArgumentException if reference, file or conflict validation fails
     * @throws IllegalStateException if planned working files cannot be read
     * @throws IOException if file publication or cache invalidation fails
     */
    void preparePublication(DocDetails doc, Identity user) throws IOException {
        validateReferenceFields(doc, plannedMedia(doc.getDocId()));
        Map<String, String> files = plannedFiles(doc.getDocId());
        preflight(files, user);
        checkConflicts(doc.getDocId(), files);
        publishFiles(files, doc.getDocId(), doc.getHistoryId());
        rewriteContent(doc);
    }

    /**
     * Replaces a saved page's live media with its initialized planned set in the current domain.
     *
     * <p>Deletes the old live set and saves copies with public asset paths. Planned media
     * and the initialization flag remain until every live media save succeeds, allowing
     * a failed replacement to be retried. An initialized empty set removes all live media.</p>
     *
     * <p>After successful replacement the flag is cleared before planned media are deleted.
     * If cleanup is retried with the flag already cleared, the live set is left unchanged.</p>
     *
     * @param doc saved page providing the media owner ID and publication timestamp
     */
    void publishMedia(DocDetails doc) {
        if (docDetailsRepository.isPlannedMediaInitialized(doc.getDocId())) {
            List<Media> planned = plannedMedia(doc.getDocId()).stream().map(item -> copyMedia(item, "documents")).toList();
            deleteMedia(doc.getDocId(), "documents", CloudToolsForCore.getDomainId());
            for (Media item : planned) {
                rewriteProperties(item, doc.getDocId(), "mediaLink", "mediaThumbLink", "mediaInfoSk", "mediaInfoCz", "mediaInfoEn", "mediaInfoDe");
                for (char field = 'A'; field <= 'F'; field++) rewriteProperties(item, doc.getDocId(), "field" + field);
                // Page dates can be stored with second precision; media must not appear newer.
                item.setLastUpdate(new Date(doc.getDateCreated() / 1000 * 1000));
                mediaRepository.saveAndFlush(item);
            }
            // A retry after this point must keep the completed live set.
            docDetailsRepository.setPlannedMediaInitialized(doc.getId(), false);
        }
        deleteMedia(doc.getDocId(), PLANNED_MEDIA_TABLE, CloudToolsForCore.getDomainId());
    }

    /**
     * Removes planned working files after the publisher has completed the history record.
     *
     * <p>Publication backups are retained. Cleanup failures are logged instead of propagated
     * so the publisher can finish its remaining cache updates.</p>
     *
     * @param docId page ID whose planned working directories should be removed
     */
    void cleanupPublishedFiles(int docId) {
        try {
            deletePlannedFiles(docId);
        } catch (IllegalStateException exception) {
            // Cleanup failure must not skip refreshing the live page cache.
            Logger.error(PlannedChangesService.class, "Could not clean planned files for page " + docId);
            Logger.error(PlannedChangesService.class, exception);
        }
    }

    private void deleteMedia(int docId, String table, int domain) {
        List<Media> items = mediaRepository.findAllByMediaFkIdAndMediaFkTableNameAndDomainId(docId, table, domain);
        mediaRepository.deleteAll(items);
    }

    /**
     * Creates an unsaved media copy for a different owner table while retaining its page ID.
     *
     * @param original source media; its group list is copied into a separate list
     * @param table destination owner table, such as {@code documents} or {@link #PLANNED_MEDIA_TABLE}
     * @return copy without the source record ID, media ID or editor fields
     */
    private Media copyMedia(Media original, String table) {
        Media copy = new Media();
        BeanUtils.copyProperties(original, copy, "id", "mediaId", "groups", "editorFields");
        copy.setGroups(new ArrayList<>(original.getGroups()));
        copy.setMediaFkTableName(table);
        return copy;
    }

    /**
     * Invalidates WebJET's file cache and removes matching cached thumbnail variants.
     *
     * @param target root-relative public path of the replaced file
     * @throws IOException if cached thumbnail files cannot be listed or deleted
     */
    private void invalidateAsset(String target) throws IOException {
        sk.iway.iwcm.Cache.getInstance().removeObjectStartsWithName(sk.iway.iwcm.io.FileCache.FILE_CACHE_PREFIX + target, true);
        Path cached = Path.of(Tools.getRealPath(Constants.getString("thumbServletCacheDir") + target.substring(1)));
        if (!Files.isDirectory(cached.getParent())) return;
        String name = cached.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0) return;
        String expression = java.util.regex.Pattern.quote(name.substring(0, dot)) + "-\\d+x\\d+[^/]*" + java.util.regex.Pattern.quote(name.substring(dot));
        try (var variants = Files.list(cached.getParent())) {
            for (Path variant : variants.filter(path -> path.getFileName().toString().matches(expression)).toList()) Files.deleteIfExists(variant);
        }
    }

    /**
     * Collects every regular file in the page's planned images and files directories.
     *
     * <p>Includes files not referenced by page content. Missing working directories are
     * skipped, and each discovered source is mapped to a validated public destination.</p>
     *
     * @param docId page ID identifying the planned working directories
     * @return mapping from root-relative planned source paths to decoded public destination paths
     * @throws IllegalArgumentException if a discovered path has an invalid owner or public destination
     * @throws IllegalStateException if a working directory cannot be traversed
     */
    private Map<String, String> plannedFiles(int docId) {
        Map<String, String> files = new LinkedHashMap<>();
        for (String folder : List.of("images", "files")) {
            String prefix = PlannedAssetPaths.ROOT + docId + "/" + folder + "/";
            try {
                Path root = Path.of(Tools.getRealPath(prefix));
                if (!Files.exists(root)) continue;
                try (var paths = Files.walk(root)) {
                    for (Path path : paths.toList()) {
                        String source = prefix + root.relativize(path).toString().replace('\\', '/');
                        if (Files.isRegularFile(path)) files.put(source, PlannedAssetPaths.getPublicPath(source, docId));
                    }
                }
            } catch (IOException | UncheckedIOException exception) {
                throw new IllegalStateException("Cannot read planned files for page " + docId, exception);
            }
        }
        return files;
    }

    /**
     * Deletes the page's planned images and files directories, including their contents.
     *
     * <p>Missing directories are ignored. The separate publication backup directory
     * is retained, and a failure can leave part of a working directory already deleted.</p>
     *
     * @param docId page ID identifying the working directories to remove
     * @throws IllegalStateException if a directory cannot be traversed or an entry cannot be deleted
     */
    private void deletePlannedFiles(int docId) {
        for (String folder : List.of("images", "files")) {
            try {
                Path root = Path.of(Tools.getRealPath(PlannedAssetPaths.ROOT + docId + "/" + folder + "/"));
                if (!Files.exists(root)) continue;
                try (var paths = Files.walk(root)) {
                    for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
                }
            } catch (IOException | UncheckedIOException exception) {
                throw new IllegalStateException("Cannot delete planned files for page " + docId, exception);
            }
        }
    }

    /**
     * Replaces public files from planned sources, backing up existing targets once per history revision.
     *
     * <p>Each source is copied to a temporary file and atomically moved to its public
     * destination, then WebJET caches are invalidated. Planned originals are retained
     * for retry. Atomicity applies to each file, not the complete set; earlier replacements
     * remain published if a later operation fails.</p>
     *
     * @param files mapping from root-relative planned source paths to public destination paths
     * @param docId page ID identifying the protected backup directory
     * @param historyId history revision ID separating backups of successive publications
     * @throws IOException if backup, replacement, temporary-file cleanup or cache invalidation fails
     */
    private void publishFiles(Map<String, String> files, int docId, long historyId) throws IOException {
        for (Map.Entry<String, String> entry : files.entrySet()) {
            Path source = Path.of(Tools.getRealPath(entry.getKey()));
            Path target = Path.of(Tools.getRealPath(entry.getValue()));
            Files.createDirectories(target.getParent());
            Path backup = Path.of(Tools.getRealPath(PlannedAssetPaths.ROOT + docId + "/backup/" + historyId + entry.getValue()));
            if (Files.exists(target) && !Files.exists(backup)) {
                Files.createDirectories(backup.getParent());
                Files.copy(target, backup, StandardCopyOption.COPY_ATTRIBUTES);
                IwcmFsDB.saveFileHistory(new IwcmFile(target.toFile()), false);
            }
            Path temporary = Files.createTempFile(source.getParent(), ".publishing-", ".tmp");
            try {
                Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
                Files.setLastModifiedTime(temporary, FileTime.fromMillis(System.currentTimeMillis()));
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                invalidateAsset(entry.getValue());
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
    }
}
