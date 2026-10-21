package sk.iway.iwcm.doc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.editor.DocNoteBean;
import sk.iway.iwcm.editor.DocNoteDB;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.media.MediaRepository;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.editor.rest.DocDetailsToDocHistoryMapper;
import sk.iway.iwcm.system.jpa.WebJETPersistenceProvider;
import sk.iway.iwcm.test.BaseWebjetTest;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.spirit.model.Media;
import sk.iway.spirit.model.MediaGroupBean;

/** Exercises publication retries with committed partial writes on the configured WebJET database. */
class PlannedChangesIntegrationTest extends BaseWebjetTest {
    @TempDir
    Path assets;
    private final List<MockedStatic<?>> staticMocks = new ArrayList<>();
    private AnnotationConfigApplicationContext context;
    private DocDetailsRepository docs;
    private DocHistoryRepository history;
    private MediaRepository media;
    private PlannedChangesService service;
    private DocPublishService publisher;
    private DocDB docsCache;
    private GroupsDB groupDB;
    private GroupDetails group;
    private Identity user;
    private DocDetails page;
    private MediaGroupBean mediaGroup;
    private TransactionTemplate transaction;
    private MockedStatic<UsersDB> users;
    private MockedStatic<MultigroupMappingDB> mappings;
    private MockedStatic<DocNoteDB> notes;
    private MockedStatic<DocDB> docCache;

    @BeforeEach
    void setUp() {
        register(mockStatic(Adminlog.class));
        var tools = register(mockStatic(Tools.class, CALLS_REAL_METHODS));
        var access = register(mockStatic(PlannedAccess.class));
        var domains = register(mockStatic(CloudToolsForCore.class));
        var groups = register(mockStatic(GroupsDB.class));
        docCache = register(mockStatic(DocDB.class));
        users = register(mockStatic(UsersDB.class));
        mappings = register(mockStatic(MultigroupMappingDB.class));
        notes = register(mockStatic(DocNoteDB.class));
        var constants = register(mockStatic(Constants.class, CALLS_REAL_METHODS));
        context = new AnnotationConfigApplicationContext(RepositoryConfiguration.class);
        docs = context.getBean(DocDetailsRepository.class);
        history = context.getBean("docHistoryRepository", DocHistoryRepository.class);
        media = context.getBean(MediaRepository.class);
        service = context.getBean(PlannedChangesService.class);
        publisher = publisher(docs);
        transaction = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        user = new Identity();
        user.setUserId(1);
        user.setAdmin(true);
        user.setWritableFolders("*");
        group = new GroupDetails();
        group.setDomainName("planned-autotest.example.com");
        docsCache = mock(DocDB.class);
        when(docsCache.getDocLink(anyInt(), anyString(), isNull())).thenReturn("/autotest-planned");
        docCache.when(DocDB::getInstance).thenReturn(docsCache);
        docCache.when(() -> DocDB.getRootGroupL(anyInt(), any(), anyInt())).thenReturn(new int[] {1, 0, 0});
        notes.when(DocNoteDB::getInstance).thenReturn(mock(DocNoteDB.class));
        constants.when(() -> Constants.getBoolean("webpagesNotifyAutorOnPublish")).thenReturn(false);
        constants.when(() -> Constants.getBoolean("editorDisableHistory")).thenReturn(false);
        groupDB = mock(GroupsDB.class);
        when(groupDB.getGroup(anyInt())).thenReturn(group);
        groups.when(GroupsDB::getInstance).thenReturn(groupDB);
        access.when(PlannedAccess::enabled).thenReturn(true);
        tools.when(() -> Tools.getRealPath(anyString())).thenAnswer(call -> assets.resolve(((String)call.getArgument(0)).substring(1)).toString());
        domains.when(CloudToolsForCore::getDomainId).thenReturn(1);
        domains.when(CloudToolsForCore::getDomainName).thenReturn(group.getDomainName());
        users.when(() -> UsersDB.getUser(anyInt())).thenReturn(user);
        users.when(() -> UsersDB.isFolderWritable(anyString(), anyString())).thenReturn(true);

        page = new DocDetails();
        page.setTitle("autotest-planned-" + UUID.randomUUID());
        page.setData("Live content");
        page.setDataAsc("Live content");
        page.setNavbar(page.getTitle());
        page.setGroupId(1);
        page.setAuthorId(1);
        page.setDateCreated(System.currentTimeMillis());
        page.setVirtualPath("/autotest-planned");
        page = docs.saveAndFlush(page);
        var em = org.springframework.orm.jpa.SharedEntityManagerCreator.createSharedEntityManager(context.getBean(EntityManagerFactory.class));
        mediaGroup = new MediaGroupBean();
        mediaGroup.setMediaGroupName(page.getTitle());
        transaction.executeWithoutResult(status -> em.persist(mediaGroup));
    }

    @AfterEach
    void tearDown() {
        try {
            SimpleQuery cleanup = new SimpleQuery();
            if (page != null && page.getDocId() > 0) {
                cleanup.execute("DELETE FROM media_group_to_media WHERE media_id IN (SELECT media_id FROM media WHERE media_fk_id=? AND media_fk_table_name IN ('documents','planned_documents'))", page.getDocId());
                cleanup.execute("DELETE FROM media WHERE media_fk_id=? AND media_fk_table_name IN ('documents','planned_documents')", page.getDocId());
                cleanup.execute("DELETE FROM documents_history WHERE doc_id=?", page.getDocId());
                cleanup.execute("DELETE FROM documents WHERE doc_id=?", page.getDocId());
            }
            if (mediaGroup != null && mediaGroup.getId() != null) {
                cleanup.execute("DELETE FROM media_groups WHERE media_group_id=?", mediaGroup.getId());
            }
        } finally {
            try {
                if (context != null) context.close();
            } finally {
                for (int i = staticMocks.size() - 1; i >= 0; i--) staticMocks.get(i).close();
            }
        }
    }

    /** A committed partial copy is replaced on retry; later initialization and ordinary saves retain it. */
    @Test
    void initializesMediaOnceAndRecoversPartialCopies() {
        Media original = originalMedia();
        PlannedChangesService interrupted = interruptedService();
        assertThrows(IllegalStateException.class, () -> interrupted.initialize(page.getDocId()));
        assertFalse(docs.isPlannedMediaInitialized(page.getDocId()));
        assertEquals(1, plannedMedia().size());

        // Retrying initialization must replace the partial copy, not duplicate it.
        service.initialize(page.getDocId());
        service.initialize(page.getDocId());
        List<Media> planned = plannedMedia();
        assertEquals(1, planned.size());
        assertNotEquals(original.getId(), planned.get(0).getId());
        assertEquals(mediaGroup.getId(), planned.get(0).getGroups().get(0).getId());
        assertTrue(docs.isPlannedMediaInitialized(page.getDocId()));

        // Ordinary saves cannot overwrite operational initialization state.
        docs.saveAndFlush(page);
        assertTrue(docs.isPlannedMediaInitialized(page.getDocId()));
    }

    /** Failures at page, media and history writes remain retryable without duplicating completed media. */
    @Test
    void retriesPublicationWithoutDuplicatingLiveMedia() {
        Media original = originalMedia();
        long originalId = original.getId();
        service.initialize(page.getDocId());
        List<Media> planned = plannedMedia();
        PlannedChangesService interrupted = interruptedService();
        planned.get(0).setMediaTitleSk("Future");
        media.saveAndFlush(planned.get(0));
        media.saveAndFlush(item(page.getDocId(), PlannedChangesService.PLANNED_MEDIA_TABLE, "Second"));
        DocHistory revision = revision(page);
        revision = history.saveAndFlush(revision);
        long revisionId = revision.getId();

        DocDetailsRepository failingDocs = mock(DocDetailsRepository.class, org.mockito.AdditionalAnswers.delegatesTo(docs));
        doThrow(new IllegalStateException("Injected page save failure")).when(failingDocs).saveAndFlush(any(DocDetails.class));
        ReflectionTestUtils.setField(publisher, "ddr", failingDocs);
        assertThrows(IllegalStateException.class, () -> publish(revisionId));
        assertTrue(history.findById(revisionId).orElseThrow().getPublicable());
        assertEquals(originalId, liveMedia().get(0).getId());
        ReflectionTestUtils.setField(publisher, "ddr", docs);

        // A failed replacement leaves a partial live set but the complete source set survives.
        assertThrows(IllegalStateException.class, () -> publish(publisher, interrupted, revisionId));
        assertEquals(1, liveMedia().size());
        assertEquals(2, plannedMedia().size());
        assertTrue(docs.isPlannedMediaInitialized(page.getDocId()));
        assertTrue(history.findById(revisionId).orElseThrow().getPublicable());

        // A later failure in history completion must keep the already completed live set.
        context.getBean(AtomicBoolean.class).set(true);
        assertThrows(IllegalStateException.class, () -> publish(revisionId));
        List<Media> completedMedia = liveMedia();
        assertEquals(2, completedMedia.size());
        assertTrue(history.findById(revisionId).orElseThrow().getPublicable());
        assertFalse(docs.isPlannedMediaInitialized(page.getDocId()));

        assertNotNull(publish(revisionId));
        assertNull(publish(revisionId));
        List<Media> live = liveMedia();
        assertEquals(2, live.size());
        assertEquals(completedMedia.stream().map(Media::getId).sorted().toList(), live.stream().map(Media::getId).sorted().toList());
        Media futureMedia = live.stream().filter(item -> "Future".equals(item.getMediaTitleSk())).findFirst().orElseThrow();
        assertEquals(mediaGroup.getId(), futureMedia.getGroups().get(0).getId());
        assertNotEquals(planned.get(0).getId(), futureMedia.getId());
        assertFalse(docs.isPlannedMediaInitialized(page.getDocId()));
        assertFalse(history.findById(revisionId).orElseThrow().getPublicable());
        assertTrue(live.get(0).getLastUpdate().getTime() <= docs.findById(page.getDocId()).getDateCreated());
    }

    /** An initialized empty set deliberately removes live media instead of copying them again. */
    @Test
    void publishesIntentionallyEmptyMediaSet() {
        originalMedia();
        service.initialize(page.getDocId());
        transaction.executeWithoutResult(status -> {
            List<Media> removed = plannedMedia();
            removed.forEach(item -> item.getGroups().clear());
            media.flush();
            media.deleteAll(removed);
        });
        service.initialize(page.getDocId());
        assertTrue(plannedMedia().isEmpty());
        DocHistory empty = history.saveAndFlush(revision(page));
        assertNotNull(publish(empty.getId()));
        assertTrue(liveMedia().isEmpty());
    }

    /** Asset and follow-up failures preserve sources; completion publishes every file and retains backups. */
    @Test
    void publishesAssetsOnlyAfterPreflightAndRetainsSourcesUntilCompletion() throws Exception {
        PlannedChangesService interrupted = interruptedService();
        // All staged assets are published; preflight failure leaves all public files unchanged.
        String privateRoot = PlannedAssetPaths.ROOT + page.getDocId();
        Path source = assets.resolve(privateRoot.substring(1) + "/images/photo.webp");
        Path target = assets.resolve("images/photo.webp");
        Files.createDirectories(source.getParent());
        Files.createDirectories(target.getParent());
        Files.writeString(source, "future image");
        Files.writeString(source.resolveSibling("unused.webp"), "unreferenced image");
        Files.writeString(target, "old image");
        users.when(() -> UsersDB.isFolderWritable(anyString(), anyString())).thenReturn(true);
        service.initialize(page.getDocId());
        Media pdf = item(page.getDocId(), PlannedChangesService.PLANNED_MEDIA_TABLE, "PDF");
        pdf.setMediaLink(privateRoot + "/files/spec.pdf");
        media.saveAndFlush(pdf);
        DocHistory older = revision(page);
        older.setPublicable(false);
        older.setData("<img src='" + privateRoot + "/images/photo.webp'>");
        older = history.saveAndFlush(older);
        long olderId = older.getId();
        DocHistory withFiles = revision(page);
        withFiles.setPerexImage(privateRoot + "/images/photo.webp");
        withFiles.setData("<img srcset='" + privateRoot + "/images/photo.webp 1x, " + privateRoot + "/images/photo.webp?v=1634319328 2x'>!INCLUDE(component.jsp, image=" + privateRoot + "/images/missing.webp)!");
        withFiles = history.saveAndFlush(withFiles);
        long withFilesId = withFiles.getId();
        try (var filesystem = mockStatic(sk.iway.iwcm.io.IwcmFsDB.class); var cache = mockStatic(sk.iway.iwcm.Cache.class)) {
            sk.iway.iwcm.Cache cacheInstance = mock(sk.iway.iwcm.Cache.class);
            cache.when(sk.iway.iwcm.Cache::getInstance).thenReturn(cacheInstance);
            Path pdfSource = assets.resolve(privateRoot.substring(1) + "/files/spec.pdf");
            Files.createDirectories(pdfSource.getParent());
            Files.writeString(pdfSource, "future PDF");
            Path unreferenced = pdfSource.resolveSibling("nested/unused.pdf");
            Files.createDirectories(unreferenced.getParent());
            Files.writeString(unreferenced, "unreferenced PDF");
            users.when(() -> UsersDB.isFolderWritable(anyString(), eq("/files/nested/"))).thenReturn(false);
            assertThrows(IllegalArgumentException.class, () -> publish(withFilesId));
            assertEquals("old image", Files.readString(target), "Validate unreferenced assets before copying any file");
            users.when(() -> UsersDB.isFolderWritable(anyString(), eq("/files/nested/"))).thenReturn(true);
            assertThrows(IllegalStateException.class, () -> publish(publisher, interrupted, withFilesId));
            assertEquals(privateRoot + "/files/spec.pdf", plannedMedia().get(0).getMediaLink(),
                "Rewriting the live copy must preserve private source URLs for a retry");
            assertTrue(Files.exists(source));
            assertTrue(Files.exists(unreferenced));
            assertEquals(privateRoot + "/images/photo.webp", history.findById(withFilesId).orElseThrow().getPerexImage());
            context.getBean(AtomicBoolean.class).set(true);
            assertThrows(IllegalStateException.class, () -> publish(withFilesId));
            assertTrue(Files.exists(source), "Keep staged assets until history completion succeeds");

            // Boolean save failures must also keep history pending and staged files available for retry.
            List<Long> completedMediaIds = liveMedia()
                .stream().map(Media::getId).sorted().toList();
            DocDetails slave = new DocDetails();
            slave.setDocId(910083);
            slave.setGroupId(1);
            slave.setVirtualPath("/autotest-planned-copy");
            mappings.when(() -> MultigroupMappingDB.getSlaveDocIds(page.getDocId())).thenReturn(List.of(slave.getDocId()));
            when(docsCache.getBasicDocDetails(slave.getDocId(), false)).thenReturn(slave);
            when(docsCache.getDoc(page.getDocId(), -1, false)).thenAnswer(call ->
                DocDetailsToDocHistoryMapper.INSTANCE.docHistoryToDocDetails(history.findById(withFilesId).orElseThrow()));
            docCache.when(() -> DocDB.saveDoc(any(DocDetails.class))).thenReturn(false, true);
            IllegalStateException slaveFailure = assertThrows(IllegalStateException.class, () -> publish(withFilesId));
            assertTrue(slaveFailure.getMessage().contains(Integer.toString(slave.getDocId())));
            assertTrue(history.findById(withFilesId).orElseThrow().getPublicable());
            assertTrue(Files.exists(source));
            assertTrue(Files.exists(unreferenced));

            DocNoteDB noteDB = mock(DocNoteDB.class);
            DocNoteBean publishedNote = mock(DocNoteBean.class);
            when(noteDB.getDocNote(-1, (int) withFilesId)).thenReturn(mock(DocNoteBean.class));
            when(noteDB.getDocNote(page.getDocId(), -1)).thenReturn(publishedNote);
            when(publishedNote.save()).thenReturn(false, true);
            notes.when(DocNoteDB::getInstance).thenReturn(noteDB);
            IllegalStateException noteFailure = assertThrows(IllegalStateException.class, () -> publish(withFilesId));
            assertTrue(noteFailure.getMessage().contains(Integer.toString(page.getDocId())));
            assertTrue(history.findById(withFilesId).orElseThrow().getPublicable());
            assertTrue(Files.exists(source));
            assertTrue(Files.exists(unreferenced));

            DocDetails published = publish(withFilesId);
            assertNotNull(published);
            verify(publishedNote, times(2)).save();
            docCache.verify(() -> DocDB.saveDoc(any(DocDetails.class)), times(3));
            assertEquals(completedMediaIds, liveMedia()
                .stream().map(Media::getId).sorted().toList(), "Retries must preserve the completed live media set");
            mappings.when(() -> MultigroupMappingDB.getSlaveDocIds(page.getDocId())).thenReturn(List.of());
            notes.when(DocNoteDB::getInstance).thenReturn(mock(DocNoteDB.class));
            assertFalse(published.getData().contains(PlannedAssetPaths.ROOT));
            assertEquals("<img srcset='/images/photo.webp 1x, /images/photo.webp?v=1634319328 2x'>!INCLUDE(component.jsp, image=/images/missing.webp)!", published.getData());
            assertEquals("/images/photo.webp", published.getPerexImage());
            assertEquals("/files/spec.pdf", liveMedia().get(0).getMediaLink());
            assertEquals("future image", Files.readString(target));
            assertEquals("future PDF", Files.readString(assets.resolve("files/spec.pdf")));
            assertEquals("old image", Files.readString(assets.resolve(privateRoot.substring(1) + "/backup/" + withFilesId + "/images/photo.webp")));
            assertEquals("unreferenced image", Files.readString(assets.resolve("images/unused.webp")));
            assertEquals("unreferenced PDF", Files.readString(assets.resolve("files/nested/unused.pdf")));
            assertFalse(Files.exists(source.getParent()));
            assertFalse(Files.exists(pdfSource.getParent()));
            DocHistory publishedHistory = history.findById(withFilesId).orElseThrow();
            assertEquals(published.getData(), publishedHistory.getData());
            assertEquals("/images/photo.webp", publishedHistory.getPerexImage());
            assertEquals("<img src='" + privateRoot + "/images/photo.webp'>", history.findById(olderId).orElseThrow().getData());
            assertNull(publish(withFilesId));
        }
    }

    /** Preparing a replacement keeps the old plan; only a successfully saved newer revision supersedes it. */
    @Test
    void replacesOnlyOlderPlansAfterSave() {
        String privateRoot = PlannedAssetPaths.ROOT + page.getDocId();
        service.initialize(page.getDocId());
        DocHistory future = revision(page);
        future.setPublishStartDate(new Date(System.currentTimeMillis() + 60000));
        future = history.saveAndFlush(future);
        assertNull(publish(future.getId()));
        DocDetails replacement = DocDetailsToDocHistoryMapper.INSTANCE.docHistoryToDocDetails(future);
        replacement.setHistoryId(future.getHistoryId());
        service.prepareSave(replacement, user);
        assertTrue(history.findById(future.getId()).orElseThrow().getPublicable(), "Preparing a replacement must keep the existing plan active");
        DocHistory next = revision(page);
        next.setPublishStartDate(future.getPublishStartDate());
        next.setData("<img src='" + privateRoot + "/images/cancel.webp'>");
        next = history.saveAndFlush(next);
        service.completeSave(next);
        assertFalse(history.findById(future.getId()).orElseThrow().getPublicable());
        assertTrue(history.findById(next.getId()).orElseThrow().getPublicable());
        service.completeSave(future);
        assertTrue(history.findById(next.getId()).orElseThrow().getPublicable(), "An older save must not cancel a newer plan");
    }

    /** Cancellation removes only working assets and planned media, preserving live data, history and backups. */
    @Test
    void cancelsWorkingAssetsWithoutChangingLiveDataOrBackups() throws Exception {
        Media original = originalMedia();
        service.initialize(page.getDocId());
        String privateRoot = PlannedAssetPaths.ROOT + page.getDocId();
        DocHistory next = revision(page);
        next.setData("<img src='" + privateRoot + "/images/cancel.webp'>");
        next = history.saveAndFlush(next);
        Path source = assets.resolve(privateRoot.substring(1) + "/images/cancel.webp");
        Path backup = assets.resolve(privateRoot.substring(1) + "/backup/123/images/photo.webp");
        Files.createDirectories(backup.getParent());
        Files.writeString(backup, "old image");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "cancelled image");
        Path cancelledFile = assets.resolve(privateRoot.substring(1) + "/files/nested/cancel.pdf");
        Files.createDirectories(cancelledFile.getParent());
        Files.writeString(cancelledFile, "cancelled file");
        String liveData = docs.findById(page.getDocId()).getData();
        service.cancel(page.getDocId());
        assertFalse(Files.exists(source.getParent()));
        assertFalse(Files.exists(cancelledFile.getParent().getParent()));
        assertFalse(Files.exists(assets.resolve("images/cancel.webp")));
        assertFalse(Files.exists(assets.resolve("files/nested/cancel.pdf")));
        assertEquals(liveData, docs.findById(page.getDocId()).getData());
        assertEquals(next.getData(), history.findById(next.getId()).orElseThrow().getData());
        assertEquals("old image", Files.readString(backup));
        assertFalse(history.findById(next.getId()).orElseThrow().getPublicable());
        assertNull(publish(next.getId()));
        assertFalse(docs.isPlannedMediaInitialized(page.getDocId()));
        assertEquals(original.getId(), liveMedia().get(0).getId());
        assertTrue(plannedMedia().isEmpty());
    }

    /** Different publisher instances cannot write the same page while the first publication is in progress. */
    @Test
    void preventsOverlappingPublicationAcrossPublisherInstances() throws Exception {
        originalMedia();
        service.initialize(page.getDocId());
        long historyId = history.saveAndFlush(revision(page)).getId();
        CountDownLatch beforePageWrite = new CountDownLatch(1);
        CountDownLatch allowPageWrite = new CountDownLatch(1);
        DocDetailsRepository blockedDocs = mock(DocDetailsRepository.class, org.mockito.AdditionalAnswers.delegatesTo(docs));
        doAnswer(call -> {
            beforePageWrite.countDown();
            assertTrue(allowPageWrite.await(20, TimeUnit.SECONDS), "The test must release the first publisher");
            return docs.saveAndFlush(call.getArgument(0));
        }).when(blockedDocs).saveAndFlush(any(DocDetails.class));
        DocDetailsRepository competingDocs = mock(DocDetailsRepository.class, org.mockito.AdditionalAnswers.delegatesTo(docs));
        DocPublishService firstPublisher = publisher(blockedDocs);
        DocPublishService secondPublisher = publisher(competingDocs);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var first = executor.submit(() -> publishInWorker(firstPublisher, historyId));
            assertTrue(beforePageWrite.await(20, TimeUnit.SECONDS), "The first publisher must reach the write boundary");
            assertNull(publish(secondPublisher, service, historyId));
            verifyNoInteractions(competingDocs);
            assertTrue(history.findById(historyId).orElseThrow().getPublicable());
            allowPageWrite.countDown();
            assertNotNull(first.get(20, TimeUnit.SECONDS));
            assertFalse(history.findById(historyId).orElseThrow().getPublicable());
            assertEquals("Future content", docs.findById(page.getDocId()).getData());
            assertEquals(1, liveMedia().size());
        } finally {
            allowPageWrite.countDown();
            executor.shutdown();
            if (!executor.awaitTermination(20, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                fail("Publisher worker did not terminate before fixture cleanup");
            }
        }
    }

    /** Unrewritable body and media references must not cancel or overwrite the existing plan. */
    @ParameterizedTest
    @CsvSource({"body,foreign", "media,foreign", "body,urlEncoded", "media,urlEncoded", "body,htmlEncoded", "media,htmlEncoded"})
    void rejectsUnrewritableReferencesWithoutCancellingPlan(String target, String variant) {
        originalMedia();
        service.initialize(page.getDocId());
        DocHistory pending = history.saveAndFlush(revision(page));
        DocDetails replacement = DocDetailsToDocHistoryMapper.INSTANCE.docHistoryToDocDetails(pending);
        String root = PlannedAssetPaths.ROOT + page.getDocId();
        String reference = switch (variant) {
            case "foreign" -> PlannedAssetPaths.ROOT + (page.getDocId() + 1) + "/images/foreign.webp";
            case "urlEncoded" -> root.replace("/", "%2F") + "%2Fimages%2Fphoto.webp";
            case "htmlEncoded" -> root.replace("/", "&#47;") + "&#47;images&#47;photo.webp";
            default -> throw new IllegalArgumentException(variant);
        };
        Media planned = plannedMedia().get(0);
        if ("body".equals(target)) replacement.setData("<img src='" + reference + "'>");
        else {
            planned.setMediaLink(reference);
            media.saveAndFlush(planned);
        }
        String submittedBody = replacement.getData();
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> service.prepareSave(replacement, user));
        assertEquals(Prop.getInstance().getText("planned.unrewritableReferences"), error.getMessage());
        assertEquals(submittedBody, replacement.getData());
        assertEquals(pending.getId(), service.activeHistory(page.getDocId()));
        assertEquals(pending.getData(), history.findById(pending.getId()).orElseThrow().getData());
        assertEquals("Live content", docs.findById(page.getDocId()).getData());
        assertEquals(planned.getMediaLink(), plannedMedia().get(0).getMediaLink());
        assertTrue(docs.isPlannedMediaInitialized(page.getDocId()));
    }

    /** Save validation simulates rewriting but retains private URLs for preview and later publication. */
    @Test
    void acceptsOwnReferencesWithoutRewritingSources() {
        originalMedia();
        service.initialize(page.getDocId());
        DocHistory pending = history.saveAndFlush(revision(page));
        DocDetails replacement = DocDetailsToDocHistoryMapper.INSTANCE.docHistoryToDocDetails(pending);
        String root = PlannedAssetPaths.ROOT + page.getDocId();
        String body = "<img src='" + root + "/images/photo.webp?w=200&amp;v=old#view'>";
        String link = root + "/files/future.pdf?download=1#page=2";
        replacement.setData(body);
        Media planned = plannedMedia().get(0);
        planned.setMediaLink(link);
        media.saveAndFlush(planned);
        service.prepareSave(replacement, user);
        assertEquals(body, replacement.getData());
        assertEquals(link, plannedMedia().get(0).getMediaLink());
        assertEquals(pending.getId(), service.activeHistory(page.getDocId()));
        assertEquals(pending.getData(), history.findById(pending.getId()).orElseThrow().getData());
        assertEquals("Live content", docs.findById(page.getDocId()).getData());
    }

    private <T> MockedStatic<T> register(MockedStatic<T> mocked) {
        staticMocks.add(mocked);
        return mocked;
    }

    private DocPublishService publisher(DocDetailsRepository documents) {
        DocPublishService result = new DocPublishService();
        ReflectionTestUtils.setField(result, "ddr", documents);
        ReflectionTestUtils.setField(result, "dhr", context.getBean("publicationHistory", DocHistoryRepository.class));
        return result;
    }

    private Media originalMedia() {
        Media original = item(page.getDocId(), "documents", "Original");
        original.getGroups().add(mediaGroup);
        return media.saveAndFlush(original);
    }

    private List<Media> plannedMedia() {
        return media.findAllByMediaFkIdAndMediaFkTableNameAndDomainId(page.getDocId(), PlannedChangesService.PLANNED_MEDIA_TABLE, 1);
    }

    private List<Media> liveMedia() {
        return media.findAllByMediaFkIdAndMediaFkTableNameAndDomainId(page.getDocId(), "documents", 1);
    }

    private PlannedChangesService interruptedService() {
        MediaRepository failingMedia = mock(MediaRepository.class, org.mockito.AdditionalAnswers.delegatesTo(media));
        doAnswer(call -> {
            media.saveAndFlush(call.getArgument(0));
            throw new IllegalStateException("Injected failure after a committed media copy");
        }).when(failingMedia).saveAndFlush(any(Media.class));
        return new PlannedChangesService(docs, history, failingMedia);
    }

    private DocDetails publish(long historyId) {
        return publish(publisher, service, historyId);
    }

    private DocDetails publish(DocPublishService selectedPublisher, PlannedChangesService selectedService, long historyId) {
        try (var planned = mockStatic(PlannedChangesService.class)) {
            planned.when(PlannedChangesService::getInstance).thenReturn(selectedService);
            return selectedPublisher.copyDHistory(history.findById(historyId).orElseThrow(), docsCache);
        }
    }

    private DocDetails publishInWorker(DocPublishService worker, long historyId) {
        // Mockito static mocks are thread-local: reproduce only the publication infrastructure.
        try (var workerAccess = mockStatic(PlannedAccess.class); var workerGroups = mockStatic(GroupsDB.class);
             var workerTools = mockStatic(Tools.class, CALLS_REAL_METHODS);
             var workerUsers = mockStatic(UsersDB.class); var workerDomains = mockStatic(CloudToolsForCore.class);
             var workerDocs = mockStatic(DocDB.class); var workerAudit = mockStatic(Adminlog.class);
             var workerMappings = mockStatic(MultigroupMappingDB.class); var workerNotes = mockStatic(DocNoteDB.class);
             var workerConstants = mockStatic(Constants.class, CALLS_REAL_METHODS)) {
            workerAccess.when(PlannedAccess::enabled).thenReturn(true);
            workerNotes.when(DocNoteDB::getInstance).thenReturn(mock(DocNoteDB.class));
            workerConstants.when(() -> Constants.getBoolean("webpagesNotifyAutorOnPublish")).thenReturn(false);
            workerDocs.when(() -> DocDB.getRootGroupL(anyInt(), any(), anyInt())).thenReturn(new int[] {1, 0, 0});
            workerTools.when(() -> Tools.getRealPath(anyString())).thenAnswer(call -> assets.resolve(((String)call.getArgument(0)).substring(1)).toString());
            workerGroups.when(GroupsDB::getInstance).thenReturn(groupDB);
            workerUsers.when(() -> UsersDB.getUser(anyInt())).thenReturn(user);
            workerDomains.when(CloudToolsForCore::getDomainId).thenReturn(1);
            workerDomains.when(CloudToolsForCore::getDomainName).thenReturn(group.getDomainName());
            workerDocs.when(DocDB::getInstance).thenReturn(docsCache);
            return publish(worker, service, historyId);
        }
    }

    private static Media item(int docId, String table, String title) {
        Media item = new Media();
        item.setMediaFkId(docId);
        item.setMediaFkTableName(table);
        item.setMediaTitleSk(title);
        item.setMediaSortOrder(20);
        item.setMediaLink("/files/current.pdf");
        item.setDomainId(1);
        item.setGroups(new ArrayList<>());
        return item;
    }

    private static DocHistory revision(DocDetails page) {
        DocHistory revision = DocDetailsToDocHistoryMapper.INSTANCE.docDetailsToDocHistory(page);
        revision.setPublicable(true);
        revision.setActual(false);
        revision.setPublishStartDate(new Date(System.currentTimeMillis() - 1000));
        revision.setData("Future content");
        revision.setSaveDate(new Date());
        return revision;
    }

    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = {DocDetailsRepository.class, MediaRepository.class},
        includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {DocDetailsRepository.class, DocHistoryRepository.class, MediaRepository.class}))
    static class RepositoryConfiguration {
        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory() {
            LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
            factory.setPersistenceUnitName("planned-autotest-" + UUID.randomUUID());
            factory.setPersistenceProvider(new WebJETPersistenceProvider());
            factory.setDataSource(DBPool.getInstance().getDataSource("iwcm"));
            factory.setJpaVendorAdapter(new EclipseLinkJpaVendorAdapter());
            factory.setManagedTypes(PersistenceManagedTypes.of(DocDetails.class.getName(), DocHistory.class.getName(), Media.class.getName(), MediaGroupBean.class.getName(),
                "sk.iway.iwcm.system.jpa.AllowSafeHtmlAttributeConverter", "sk.iway.iwcm.system.jpa.AllowHtmlAttributeConverter",
                "sk.iway.iwcm.system.jpa.CommaSeparatedIntegersConverter", "sk.iway.iwcm.users.jpa.UserBasicDtoConverter"));
            factory.setJpaPropertyMap(Map.of("eclipselink.session.customizer", "sk.iway.webjet.v9.JpaSessionCustomizer", "eclipselink.weaving", "false", "eclipselink.ddl-generation", "none", "eclipselink.logging.level", "WARNING"));
            return factory;
        }

        @Bean
        PlatformTransactionManager transactionManager(EntityManagerFactory factory) { return new JpaTransactionManager(factory); }

        @Bean
        DocHistoryRepository publicationHistory(@org.springframework.beans.factory.annotation.Qualifier("docHistoryRepository") DocHistoryRepository repository, AtomicBoolean failPublication) {
            return (DocHistoryRepository) java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {DocHistoryRepository.class}, (proxy, method, args) -> {
                if (method.getName().equals("updatePublicableAndActual") && failPublication.getAndSet(false)) throw new IllegalStateException("Injected database failure");
                try { return method.invoke(repository, args); }
                catch (java.lang.reflect.InvocationTargetException exception) { throw exception.getCause(); }
            });
        }

        @Bean
        AtomicBoolean failPublication() { return new AtomicBoolean(); }

        @Bean
        PlannedChangesService plannedChangesService(DocDetailsRepository docs, @org.springframework.beans.factory.annotation.Qualifier("publicationHistory") DocHistoryRepository histories, MediaRepository media) {
            return new PlannedChangesService(docs, histories, media);
        }
    }
}
