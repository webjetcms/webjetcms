package sk.iway.iwcm.doc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.media.MediaRepository;
import sk.iway.iwcm.editor.rest.DocDetailsToDocHistoryMapper;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmFsDB;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.spirit.model.Media;

/** Coordinates a single planned revision using existing page/history/media tables. */
@Service
public class PlannedChangesService {
    public static final String MEDIA_TABLE = "planned_documents";
    private final DocDetailsRepository documents;
    private final DocHistoryRepository histories;
    private final MediaRepository media;
    private volatile boolean transactionalStorageChecked;
    @PersistenceContext
    private EntityManager entityManager;

    public PlannedChangesService(DocDetailsRepository documents, DocHistoryRepository histories, MediaRepository media) {
        this.documents = documents;
        this.histories = histories;
        this.media = media;
    }

    public static PlannedChangesService getInstance() {
        return Tools.getSpringBean("plannedChangesService", PlannedChangesService.class);
    }

    private void requireTransactionalStorage() {
        if (transactionalStorageChecked) return;
        if (Constants.DB_TYPE == Constants.DB_MYSQL) {
            Number count = (Number)entityManager.createNativeQuery("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('documents','documents_history','media','media_group_to_media') AND ENGINE='InnoDB'").getSingleResult();
            if (count.intValue() != 4) throw new IllegalStateException("Planned publishing requires InnoDB for documents, documents_history, media and media_group_to_media");
        }
        transactionalStorageChecked = true;
    }

    private DocDetails lock(int id) {
        requireTransactionalStorage();
        DocDetails doc = documents.lockForPlannedChange(Long.valueOf(id));
        if (doc == null) throw new IllegalArgumentException("Planned page no longer exists");
        entityManager.refresh(doc);
        return doc;
    }

    @Transactional
    public void lockMedia(int docId, boolean planned, Identity user) {
        if (planned) documents.lockPlannedChanges();
        DocDetails doc = PlannedAccess.requirePage(docId, user);
        doc = lock(doc.getDocId());
        if (planned && (!PlannedAccess.enabled() || !doc.isPlannedMediaInitialized())) {
            throw new IllegalArgumentException("Planned media are no longer active; reopen the editor");
        }
    }

    @Transactional
    public void validateMedia(Media item, int docId, Identity user) {
        DocDetails doc = PlannedAccess.requirePage(docId, user);
        PlannedAssetReferences references = new PlannedAssetReferences(doc.getDocId(), GroupsDB.getInstance().getGroup(doc.getGroupId()).getDomainName(), "validation");
        rewriteProperties(item, references, false, "mediaLink", "mediaThumbLink", "mediaInfoSk", "mediaInfoCz", "mediaInfoEn", "mediaInfoDe", "fieldA", "fieldB", "fieldC", "fieldD", "fieldE", "fieldF");
        rejectFields(item, "mediaTitleSk", "mediaTitleCz", "mediaTitleEn", "mediaTitleDe", "mediaGroup");
        preflight(references.getFiles(), user);
        checkConflicts(doc.getDocId(), references.getFiles());
    }

    public Long activeHistory(int docId) {
        List<DocHistory> pending = histories.findByDocIdAndPublicableTrueOrderByIdDesc(docId);
        return pending.isEmpty() ? null : pending.get(0).getId();
    }

    /** Copies once, including an intentionally empty set. The flag cannot be changed by entity merges. */
    @Transactional
    public Long initialize(int docId, Identity user) {
        if (!PlannedAccess.enabled()) throw new IllegalArgumentException("Planned publishing is disabled");
        DocDetails doc = PlannedAccess.requirePage(docId, user);
        doc = lock(doc.getDocId());
        if (!doc.isPlannedMediaInitialized()) {
            int domain = CloudToolsForCore.getDomainId();
            for (Media original : media.findAllByMediaFkIdAndMediaFkTableNameAndDomainId(doc.getDocId(), "documents", domain)) {
                Media copy = new Media();
                BeanUtils.copyProperties(original, copy, "id", "mediaId", "groups", "editorFields");
                copy.setGroups(new ArrayList<>(original.getGroups()));
                copy.setMediaFkTableName(MEDIA_TABLE);
                media.save(copy);
            }
            documents.setPlannedMediaInitialized(doc.getId(), true);
        }
        return activeHistory(doc.getDocId());
    }

    @Transactional
    public void cancel(int docId, Identity user) {
        DocDetails doc = PlannedAccess.requirePage(docId, user);
        doc = lock(doc.getDocId());
        for (DocHistory pending : histories.findByDocIdAndPublicableTrueOrderByIdDesc(doc.getDocId())) {
            pending.setPublicable(false);
            pending.setAwaitingApprove(null);
            pending.setSyncStatus(2);
            histories.save(pending);
        }
        deleteMedia(doc.getDocId(), MEDIA_TABLE, CloudToolsForCore.getDomainId());
        documents.setPlannedMediaInitialized(doc.getId(), false);
    }

    /** Approves only the currently scheduled revision under the publication lock. */
    @Transactional
    public void approve(long historyId, Identity user) {
        DocHistory history = histories.findById(historyId).orElseThrow();
        PlannedAccess.requirePage(history.getDocId(), user);
        lock(history.getDocId());
        entityManager.refresh(history);
        if (!Boolean.TRUE.equals(history.getPublicable()) || !Long.valueOf(historyId).equals(activeHistory(history.getDocId()))) {
            throw new IllegalArgumentException("The planned version is no longer active");
        }
        history.setApprovedBy(user.getUserId());
        history.setDisapprovedBy(null);
        history.setAwaitingApprove(null);
        history.setActual(false);
        history.setApproveDate(new Date());
        histories.saveAndFlush(history);
    }

    /** Runs inside the editor transaction before a new scheduled snapshot is stored. */
    @Transactional
    public void prepareSave(DocDetails doc, Identity user) {
        if (doc.getDocId() < 1 || Constants.getBoolean("editorDisableHistory")) {
            throw new IllegalArgumentException("Save an unpublished page before preparing a planned change; history must be enabled");
        }
        documents.lockPlannedChanges();
        Long active = initialize(doc.getDocId(), user);
        if (active != null && active.intValue() != doc.getHistoryId()) {
            throw new IllegalArgumentException("Edit the existing planned version from page history (" + active + ")");
        }
        if (doc.getPublishStart() <= 0) throw new IllegalArgumentException("A publication date is required");
        DocDetails live = documents.findById(doc.getDocId());
        doc.setVirtualPath(live.getVirtualPath());
        doc.setGroupId(live.getGroupId());
        List<Media> planned = plannedMedia(doc.getDocId());
        PlannedAssetReferences references = references(doc, planned, "validation", false);
        preflight(references.getFiles(), user);
        checkConflicts(doc.getDocId(), references.getFiles());
        for (DocHistory old : histories.findByDocIdAndPublicableTrueOrderByIdDesc(doc.getDocId())) {
            old.setPublicable(false);
            old.setSyncStatus(2);
            histories.save(old);
        }
    }

    private List<Media> plannedMedia(int id) {
        return media.findAllByMediaFkIdAndMediaFkTableNameAndDomainId(id, MEDIA_TABLE, CloudToolsForCore.getDomainId());
    }

    private PlannedAssetReferences references(DocDetails doc, List<Media> planned, String version, boolean transform) {
        String domain = GroupsDB.getInstance().getGroup(doc.getGroupId()).getDomainName();
        PlannedAssetReferences references = new PlannedAssetReferences(doc.getDocId(), domain, version);
        rewriteProperties(doc, references, transform, "data", "dataAsc", "htmlData", "htmlHead", "perexImage", "perexPlace");
        for (char field = 'A'; field <= 'T'; field++) rewriteProperties(doc, references, transform, "field" + field);
        for (Media item : planned) {
            rewriteProperties(item, references, transform, "mediaLink", "mediaThumbLink", "mediaInfoSk", "mediaInfoCz", "mediaInfoEn", "mediaInfoDe");
            for (char field = 'A'; field <= 'F'; field++) rewriteProperties(item, references, transform, "field" + field);
        }
        rejectFields(doc, "title", "navbar", "externalLink", "virtualPath", "editorVirtualPath", "fileName");
        for (Media item : planned) rejectFields(item, "mediaTitleSk", "mediaTitleCz", "mediaTitleEn", "mediaTitleDe", "mediaGroup");
        return references;
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

    private void rewriteProperties(Object bean, PlannedAssetReferences references, boolean transform, String... fields) {
        BeanWrapper wrapper = new BeanWrapperImpl(bean);
        for (String field : fields) {
            String value = references.rewrite((String)wrapper.getPropertyValue(field));
            if (transform) wrapper.setPropertyValue(field, value);
        }
    }

    private void checkConflicts(int docId, Map<String, String> files) {
        if (files.isEmpty()) return;
        for (DocHistory pending : histories.findByPublicableTrue()) {
            if (pending.getDocId() == docId) continue;
            String domain = GroupsDB.getInstance().getGroup(pending.getGroupId()).getDomainName();
            if (!domain.equalsIgnoreCase(CloudToolsForCore.getDomainName()) && Constants.getBoolean("enableStaticFilesExternalDir")) continue;
            Map<String, String> other = references(DocDetailsToDocHistoryMapper.INSTANCE.docHistoryToDocDetails(pending),
                media.findAllByMediaFkIdAndMediaFkTableName(pending.getDocId(), MEDIA_TABLE), "validation", false).getFiles();
            if (other.values().stream().anyMatch(files::containsValue)) {
                throw new IllegalArgumentException("Another active planned page uses the same public asset: " + pending.getDocId());
            }
        }
    }

    private void preflight(Map<String, String> files, Identity user) {
        for (Map.Entry<String, String> entry : files.entrySet()) {
            try {
                if (!PlannedAccess.canAccess(entry.getKey(), user)) throw new IOException("Asset access denied");
                if (IwcmFsDB.useDBStorage(entry.getKey()) || IwcmFsDB.useDBStorage(entry.getValue())) throw new IOException("Planned assets require shared filesystem storage");
                Path source = PlannedAccess.checkedFile(entry.getKey());
                Path target = PlannedAccess.checkedFile(entry.getValue());
                if (!Files.isRegularFile(source) || !Files.isReadable(source) || Files.isDirectory(target)) throw new IOException("Missing or invalid planned asset");
                Path parent = target.getParent();
                while (!Files.exists(parent)) parent = parent.getParent();
                if (!Files.isWritable(parent) || !Files.getFileStore(parent).equals(Files.getFileStore(source))) throw new IOException("Asset destination must be writable and on the staging filesystem");
            } catch (IOException exception) {
                throw new IllegalArgumentException("Cannot publish asset " + entry.getValue(), exception);
            }
        }
    }

    /** Publishes once under a database lock. File effects are intentionally not rolled back with the DB. */
    @Transactional(rollbackFor = Exception.class, propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW,
        isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public DocDetails publish(long historyId) throws IOException {
        DocHistory history = histories.findById(historyId).orElse(null);
        if (history == null) return null;
        documents.lockPlannedChanges();
        DocDetails live = lock(history.getDocId());
        entityManager.refresh(history);
        if (!Boolean.TRUE.equals(history.getPublicable()) || history.getPublishStart() <= 0 || history.getPublishStart() > Tools.getNow() ||
            Tools.isNotEmpty(history.getAwaitingApprove()) || (history.getDisapprovedBy() != null && history.getDisapprovedBy() > 0) || Boolean.TRUE.equals(history.getIsDelete()) || GroupsDB.getInstance().isInTrash(live.getGroupId())) return null;
        if (!Long.valueOf(historyId).equals(activeHistory(live.getDocId()))) return null;
        UserDetails author = UsersDB.getUser(history.getAuthorId());
        Identity user = author == null ? null : new Identity(author);
        if (!PlannedAccess.canEdit(user, live)) throw new IllegalArgumentException("Planned page author no longer has permission");

        RequestBean previous = SetCharacterEncodingFilter.getCurrentRequestBean();
        RequestBean publication = new RequestBean();
        publication.setDomain(GroupsDB.getInstance().getGroup(live.getGroupId()).getDomainName());
        publication.setUser(user);
        SetCharacterEncodingFilter.setCurrentRequestBean(publication);
        try {
            DocDetails doc = DocDetailsToDocHistoryMapper.INSTANCE.docHistoryToDocDetails(history);
            doc.setVirtualPath(live.getVirtualPath());
            doc.setGroupId(live.getGroupId());
            boolean initialized = live.isPlannedMediaInitialized();
            List<Media> planned = initialized ? plannedMedia(doc.getDocId()) : List.of();
            PlannedAssetReferences references = references(doc, planned, Long.toString(historyId), true);
            preflight(references.getFiles(), user);
            checkConflicts(doc.getDocId(), references.getFiles());
            publishFiles(references.getFiles(), doc.getDocId(), historyId);
            doc.setAvailable(true);
            doc.setPublishAfterStart(false);
            doc.setPerexGroupString(doc.getPerexGroupIdsString(true));
            doc.setRootGroupL1(live.getRootGroupL1());
            doc.setRootGroupL2(live.getRootGroupL2());
            doc.setRootGroupL3(live.getRootGroupL3());
            doc.setFileName(live.getFileName());
            RequestBean.addAuditValue("publishStatus", "Webpage was published");
            documents.saveAndFlush(doc);
            if (initialized) {
                deleteMedia(doc.getDocId(), "documents", CloudToolsForCore.getDomainId());
                for (Media item : planned) {
                    item.setMediaFkTableName("documents");
                    item.setLastUpdate(new Date(doc.getDateCreated()));
                    media.save(item);
                }
                documents.setPlannedMediaInitialized(doc.getId(), false);
            }
            histories.getHisotryIdsByDocIdIn(doc.getDocId()).ifPresent(ids -> histories.updateActual(false, ids));
            histories.updatePublicableAndActual(false, true, history.getAuthorId(), true, history.getHistoryId());
            media.flush();
            histories.flush();
            return doc;
        } finally {
            SetCharacterEncodingFilter.setCurrentRequestBean(previous);
        }
    }

    private void deleteMedia(int docId, String table, int domain) {
        List<Media> items = media.findAllByMediaFkIdAndMediaFkTableNameAndDomainId(docId, table, domain);
        for (Media item : items) item.getGroups().clear();
        media.flush();
        media.deleteAll(items);
        media.flush();
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

    private void publishFiles(Map<String, String> files, int docId, long historyId) throws IOException {
        for (Map.Entry<String, String> entry : files.entrySet()) {
            Path source = PlannedAccess.checkedFile(entry.getKey());
            Path target = PlannedAccess.checkedFile(entry.getValue());
            Files.createDirectories(target.getParent());
            Path backup = PlannedAccess.checkedFile(PlannedAssetPaths.ROOT + docId + "/backup/" + historyId + entry.getValue());
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
