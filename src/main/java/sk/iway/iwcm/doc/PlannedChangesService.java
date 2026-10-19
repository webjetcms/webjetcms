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
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmFsDB;
import sk.iway.spirit.model.Media;

/** Coordinates a single planned revision using existing page/history/media tables. */
@Service
public class PlannedChangesService {
    public static final String MEDIA_TABLE = "planned_documents";
    private final DocDetailsRepository documents;
    private final DocHistoryRepository histories;
    private final MediaRepository media;

    public PlannedChangesService(DocDetailsRepository documents, DocHistoryRepository histories, MediaRepository media) {
        this.documents = documents;
        this.histories = histories;
        this.media = media;
    }

    public static PlannedChangesService getInstance() {
        return Tools.getSpringBean("plannedChangesService", PlannedChangesService.class);
    }

    public DocDetails requirePlannedMedia(int docId, Identity user) {
        DocDetails doc = PlannedAccess.requirePage(docId, user);
        if (!PlannedAccess.enabled() || !doc.isPlannedMediaInitialized()) {
            throw new IllegalArgumentException("Planned media are no longer active; reopen the editor");
        }
        return doc;
    }

    public void validateMedia(Media item, int docId, Identity user) {
        DocDetails doc = requirePlannedMedia(docId, user);
        rejectFields(item, "mediaTitleSk", "mediaTitleCz", "mediaTitleEn", "mediaTitleDe", "mediaGroup");
        Map<String, String> files = plannedFiles(doc.getDocId());
        preflight(files, user);
        checkConflicts(doc.getDocId(), files);
    }

    public Long activeHistory(int docId) {
        List<DocHistory> pending = histories.findByDocIdAndPublicableTrueOrderByIdDesc(docId);
        return pending.isEmpty() ? null : pending.get(0).getId();
    }

    /** Copies once, clearing any incomplete previous attempt before retrying initialization. */
    public Long initialize(int docId, Identity user) {
        if (!PlannedAccess.enabled()) throw new IllegalArgumentException("Planned publishing is disabled");
        DocDetails doc = PlannedAccess.requirePage(docId, user);
        if (!doc.isPlannedMediaInitialized()) {
            int domain = CloudToolsForCore.getDomainId();
            deleteMedia(doc.getDocId(), MEDIA_TABLE, domain);
            for (Media original : media.findAllByMediaFkIdAndMediaFkTableNameAndDomainId(doc.getDocId(), "documents", domain)) {
                media.saveAndFlush(copyMedia(original, MEDIA_TABLE));
            }
            documents.setPlannedMediaInitialized(doc.getId(), true);
        }
        return activeHistory(doc.getDocId());
    }

    public void cancel(int docId, Identity user) {
        DocDetails doc = PlannedAccess.requirePage(docId, user);
        for (DocHistory pending : histories.findByDocIdAndPublicableTrueOrderByIdDesc(doc.getDocId())) {
            pending.setPublicable(false);
            pending.setAwaitingApprove(null);
            pending.setSyncStatus(2);
            histories.save(pending);
        }
        deleteMedia(doc.getDocId(), MEDIA_TABLE, CloudToolsForCore.getDomainId());
        documents.setPlannedMediaInitialized(doc.getId(), false);
        deletePlannedFiles(doc.getDocId());
    }

    /** Validates a new scheduled snapshot without cancelling the existing plan. */
    public void prepareSave(DocDetails doc, Identity user) {
        if (doc.getDocId() < 1 || Constants.getBoolean("editorDisableHistory")) {
            throw new IllegalArgumentException("Save an unpublished page before preparing a planned change; history must be enabled");
        }
        Long active = initialize(doc.getDocId(), user);
        if (active != null && active.intValue() != doc.getHistoryId()) {
            throw new IllegalArgumentException("Edit the existing planned version from page history (" + active + ")");
        }
        if (doc.getPublishStart() <= 0) throw new IllegalArgumentException("A publication date is required");
        DocDetails live = documents.findById(doc.getDocId());
        doc.setVirtualPath(live.getVirtualPath());
        doc.setGroupId(live.getGroupId());
        List<Media> planned = plannedMedia(doc.getDocId());
        validateReferenceFields(doc, planned);
        Map<String, String> files = plannedFiles(doc.getDocId());
        preflight(files, user);
        checkConflicts(doc.getDocId(), files);
    }

    /** Replaces the previous plan only after the new history record has been saved. */
    public void completeSave(DocHistory saved) {
        for (DocHistory old : histories.findByDocIdAndPublicableTrueOrderByIdDesc(saved.getDocId())) {
            if (old.getId() >= saved.getId()) continue;
            old.setPublicable(false);
            old.setSyncStatus(2);
            histories.save(old);
        }
    }

    private List<Media> plannedMedia(int id) {
        return media.findAllByMediaFkIdAndMediaFkTableNameAndDomainId(id, MEDIA_TABLE, CloudToolsForCore.getDomainId());
    }

    private void validateReferenceFields(DocDetails doc, List<Media> planned) {
        rejectFields(doc, "title", "navbar", "externalLink", "virtualPath", "editorVirtualPath", "fileName");
        for (Media item : planned) rejectFields(item, "mediaTitleSk", "mediaTitleCz", "mediaTitleEn", "mediaTitleDe", "mediaGroup");
    }

    void rewriteContent(DocBasic doc) {
        rewriteProperties(doc, doc.getDocId(), "data", "dataAsc", "htmlData", "htmlHead", "perexImage", "perexPlace");
        for (char field = 'A'; field <= 'T'; field++) rewriteProperties(doc, doc.getDocId(), "field" + field);
    }

    private void rejectFields(Object bean, String... fields) {
        BeanWrapper wrapper = new BeanWrapperImpl(bean);
        for (String field : fields) {
            if (PlannedAssetPaths.containsPlannedPath(org.jsoup.parser.Parser.unescapeEntities(String.valueOf(wrapper.getPropertyValue(field)), false))) {
                throw new IllegalArgumentException("Unsupported planned reference in " + field);
            }
        }
    }

    /** Never permit a staged URL to enter live data through ordinary editing or a legacy restore. */
    public static void rejectPrivateReferences(Object bean) {
        if (hasPrivateReferences(bean)) throw new IllegalArgumentException("Use planned publication for private assets");
    }

    /** Includes private references outside the body, such as the perex image and custom fields. */
    public static boolean hasPrivateReferences(Object bean) {
        BeanWrapper wrapper = new BeanWrapperImpl(bean);
        // Inspect only persisted fields; computed bean getters can perform unrelated database work.
        for (Class<?> type = bean.getClass(); type != null; type = type.getSuperclass()) {
            for (java.lang.reflect.Field field : type.getDeclaredFields()) {
                if (field.getType() == String.class && field.isAnnotationPresent(jakarta.persistence.Column.class) &&
                    wrapper.isReadableProperty(field.getName()) && PlannedAssetPaths.containsPlannedPath(
                        org.jsoup.parser.Parser.unescapeEntities(String.valueOf(wrapper.getPropertyValue(field.getName())), false))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void rewriteProperties(Object bean, int docId, String... fields) {
        BeanWrapper wrapper = new BeanWrapperImpl(bean);
        for (String field : fields) {
            String value = PlannedAssetPaths.rewrite((String)wrapper.getPropertyValue(field), docId);
            wrapper.setPropertyValue(field, value);
        }
    }

    private void checkConflicts(int docId, Map<String, String> files) {
        if (files.isEmpty()) return;
        for (DocHistory pending : histories.findByPublicableTrue()) {
            if (pending.getDocId() == docId) continue;
            String domain = GroupsDB.getInstance().getGroup(pending.getGroupId()).getDomainName();
            if (!domain.equalsIgnoreCase(CloudToolsForCore.getDomainName()) && Constants.getBoolean("enableStaticFilesExternalDir")) continue;
            Map<String, String> other = plannedFiles(pending.getDocId());
            if (other.values().stream().anyMatch(files::containsValue)) {
                throw new IllegalArgumentException("Another active planned page uses the same public asset: " + pending.getDocId());
            }
        }
    }

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

    /** Copies staged files and rewrites the page before the regular publisher saves it. */
    void preparePublication(DocDetails doc, Identity user) throws IOException {
        validateReferenceFields(doc, plannedMedia(doc.getDocId()));
        Map<String, String> files = plannedFiles(doc.getDocId());
        preflight(files, user);
        checkConflicts(doc.getDocId(), files);
        publishFiles(files, doc.getDocId(), doc.getHistoryId());
        rewriteContent(doc);
    }

    /** Replaces live media after the page is saved, keeping the source set until all copies succeed. */
    void publishMedia(DocDetails doc) {
        if (documents.findById(doc.getDocId()).isPlannedMediaInitialized()) {
            List<Media> planned = plannedMedia(doc.getDocId()).stream().map(item -> copyMedia(item, "documents")).toList();
            deleteMedia(doc.getDocId(), "documents", CloudToolsForCore.getDomainId());
            for (Media item : planned) {
                rewriteProperties(item, doc.getDocId(), "mediaLink", "mediaThumbLink", "mediaInfoSk", "mediaInfoCz", "mediaInfoEn", "mediaInfoDe");
                for (char field = 'A'; field <= 'F'; field++) rewriteProperties(item, doc.getDocId(), "field" + field);
                // Page dates can be stored with second precision; media must not appear newer.
                item.setLastUpdate(new Date(doc.getDateCreated() / 1000 * 1000));
                media.saveAndFlush(item);
            }
            // A retry after this point must keep the completed live set.
            documents.setPlannedMediaInitialized(doc.getId(), false);
        }
        deleteMedia(doc.getDocId(), MEDIA_TABLE, CloudToolsForCore.getDomainId());
    }

    /** Cleans working files only after the publisher has completed the history record. */
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
        List<Media> items = media.findAllByMediaFkIdAndMediaFkTableNameAndDomainId(docId, table, domain);
        media.deleteAll(items);
    }

    private Media copyMedia(Media original, String table) {
        Media copy = new Media();
        BeanUtils.copyProperties(original, copy, "id", "mediaId", "groups", "editorFields");
        copy.setGroups(new ArrayList<>(original.getGroups()));
        copy.setMediaFkTableName(table);
        return copy;
    }

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

    /** Collects every staged image and file, including assets not referenced by the page. */
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
                        if (Files.isRegularFile(path)) files.put(source, PlannedAssetPaths.target(source, docId));
                    }
                }
            } catch (IOException | UncheckedIOException exception) {
                throw new IllegalStateException("Cannot read planned files for page " + docId, exception);
            }
        }
        return files;
    }

    /** Clears working folders only; publication backups are kept separately. */
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
