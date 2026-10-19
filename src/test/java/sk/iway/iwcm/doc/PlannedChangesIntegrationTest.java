package sk.iway.iwcm.doc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
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
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.media.MediaRepository;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.editor.rest.DocDetailsToDocHistoryMapper;
import sk.iway.iwcm.system.jpa.WebJETPersistenceProvider;
import sk.iway.iwcm.test.BaseWebjetTest;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.spirit.model.Media;
import sk.iway.spirit.model.MediaGroupBean;

/** Exercises publication retries with committed partial writes on the configured WebJET database. */
class PlannedChangesIntegrationTest extends BaseWebjetTest {
    @org.junit.jupiter.api.io.TempDir
    java.nio.file.Path assets;
    @Test
    void copiesOnceReplacesWholeSetAndDoesNotConsumeAgain() throws Exception {
        try (var audit = mockStatic(Adminlog.class);
             var access = mockStatic(PlannedAccess.class);
             var domains = mockStatic(CloudToolsForCore.class);
             var groups = mockStatic(GroupsDB.class);
             var docCache = mockStatic(DocDB.class);
             var users = mockStatic(UsersDB.class);
             var context = new AnnotationConfigApplicationContext(RepositoryConfiguration.class)) {
            DocDetailsRepository docs = context.getBean(DocDetailsRepository.class);
            DocHistoryRepository history = context.getBean("docHistoryRepository", DocHistoryRepository.class);
            MediaRepository media = context.getBean(MediaRepository.class);
            PlannedChangesService service = context.getBean(PlannedChangesService.class);
            TransactionTemplate transaction = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
            Identity user = new Identity();
            user.setUserId(1);
            user.setAdmin(true);
            GroupDetails group = new GroupDetails();
            group.setDomainName("planned-autotest.example.com");
            DocDB docsCache = mock(DocDB.class);
            when(docsCache.getDocLink(anyInt(), anyString(), isNull())).thenReturn("/autotest-planned");
            docCache.when(DocDB::getInstance).thenReturn(docsCache);
            GroupsDB groupDB = mock(GroupsDB.class);
            when(groupDB.getGroup(anyInt())).thenReturn(group);
            groups.when(GroupsDB::getInstance).thenReturn(groupDB);
            access.when(PlannedAccess::enabled).thenReturn(true);
            access.when(() -> PlannedAccess.canEdit(any(), any())).thenReturn(true);
            domains.when(CloudToolsForCore::getDomainId).thenReturn(1);
            domains.when(CloudToolsForCore::getDomainName).thenReturn(group.getDomainName());
            users.when(() -> UsersDB.getUser(anyInt())).thenReturn(user);

            DocDetails page = new DocDetails();
            page.setTitle("autotest-planned-" + UUID.randomUUID());
            page.setData("Live content");
            page.setDataAsc("Live content");
            page.setNavbar(page.getTitle());
            page.setGroupId(1);
            page.setAuthorId(1);
            page.setDateCreated(System.currentTimeMillis());
            page.setVirtualPath("/autotest-planned");
            page = docs.saveAndFlush(page);
            final DocDetails savedPage = page;
            access.when(() -> PlannedAccess.requirePage(eq(savedPage.getDocId()), any())).thenAnswer(call -> docs.findById(savedPage.getDocId()));
            SimpleQuery query = new SimpleQuery();
            var em = org.springframework.orm.jpa.SharedEntityManagerCreator.createSharedEntityManager(context.getBean(EntityManagerFactory.class));
            MediaGroupBean mediaGroup = new MediaGroupBean();
            mediaGroup.setMediaGroupName(page.getTitle());
            transaction.executeWithoutResult(status -> em.persist(mediaGroup));
            try {
                Media original = item(page.getDocId(), "documents", "Original");
                original.getGroups().add(mediaGroup);
                original = media.saveAndFlush(original);
                long originalId = original.getId();

                MediaRepository failingMedia = mock(MediaRepository.class, org.mockito.AdditionalAnswers.delegatesTo(media));
                doAnswer(call -> {
                    media.saveAndFlush(call.getArgument(0));
                    throw new IllegalStateException("Injected failure after a committed media copy");
                }).when(failingMedia).saveAndFlush(any(Media.class));
                PlannedChangesService interrupted = new PlannedChangesService(docs, history, failingMedia);
                assertThrows(IllegalStateException.class, () -> interrupted.initialize(savedPage.getDocId(), user));
                assertFalse(docs.findById(page.getDocId()).isPlannedMediaInitialized());
                assertEquals(1, media.findAllByMediaFkIdAndMediaFkTableNameAndDomainId(page.getDocId(), PlannedChangesService.MEDIA_TABLE, 1).size());

                // Retrying initialization must replace the partial copy, not duplicate it.
                service.initialize(page.getDocId(), user);
                service.initialize(page.getDocId(), user);
                List<Media> planned = media.findAllByMediaFkIdAndMediaFkTableNameAndDomainId(page.getDocId(), PlannedChangesService.MEDIA_TABLE, 1);
                assertEquals(1, planned.size());
                assertNotEquals(originalId, planned.get(0).getId());
                assertEquals(mediaGroup.getId(), planned.get(0).getGroups().get(0).getId());
                assertTrue(docs.findById(page.getDocId()).isPlannedMediaInitialized());

                // Ordinary saves cannot overwrite operational initialization state.
                docs.saveAndFlush(savedPage);
                assertTrue(docs.findById(page.getDocId()).isPlannedMediaInitialized());
                planned.get(0).setMediaTitleSk("Future");
                media.saveAndFlush(planned.get(0));
                media.saveAndFlush(item(page.getDocId(), PlannedChangesService.MEDIA_TABLE, "Second"));
                DocHistory revision = revision(page);
                revision = history.saveAndFlush(revision);
                long revisionId = revision.getId();

                // A failed replacement leaves a partial live set but the complete source set survives.
                assertThrows(IllegalStateException.class, () -> interrupted.publish(revisionId));
                assertEquals(1, media.findAllByMediaFkIdAndMediaFkTableNameAndDomainId(page.getDocId(), "documents", 1).size());
                assertEquals(2, media.findAllByMediaFkIdAndMediaFkTableNameAndDomainId(page.getDocId(), PlannedChangesService.MEDIA_TABLE, 1).size());
                assertTrue(docs.findById(page.getDocId()).isPlannedMediaInitialized());
                assertTrue(history.findById(revisionId).orElseThrow().getPublicable());

                // A later failure in history completion must keep the already completed live set.
                context.getBean(java.util.concurrent.atomic.AtomicBoolean.class).set(true);
                assertThrows(IllegalStateException.class, () -> service.publish(revisionId));
                List<Media> completedMedia = media.findAllByMediaFkIdAndMediaFkTableNameAndDomainId(page.getDocId(), "documents", 1);
                assertEquals(2, completedMedia.size());
                assertTrue(history.findById(revisionId).orElseThrow().getPublicable());
                assertFalse(docs.findById(page.getDocId()).isPlannedMediaInitialized());

                assertNotNull(service.publish(revisionId));
                assertNull(service.publish(revisionId));
                List<Media> live = media.findAllByMediaFkIdAndMediaFkTableNameAndDomainId(page.getDocId(), "documents", 1);
                assertEquals(2, live.size());
                assertEquals(completedMedia.stream().map(Media::getId).sorted().toList(), live.stream().map(Media::getId).sorted().toList());
                Media futureMedia = live.stream().filter(item -> "Future".equals(item.getMediaTitleSk())).findFirst().orElseThrow();
                assertEquals(mediaGroup.getId(), futureMedia.getGroups().get(0).getId());
                assertNotEquals(planned.get(0).getId(), futureMedia.getId());
                assertFalse(docs.findById(page.getDocId()).isPlannedMediaInitialized());
                assertFalse(history.findById(revisionId).orElseThrow().getPublicable());
                assertTrue(live.get(0).getLastUpdate().getTime() <= docs.findById(page.getDocId()).getDateCreated());

                service.initialize(page.getDocId(), user);
                transaction.executeWithoutResult(status -> {
                    List<Media> removed = media.findAllByMediaFkIdAndMediaFkTableNameAndDomainId(savedPage.getDocId(), PlannedChangesService.MEDIA_TABLE, 1);
                    removed.forEach(item -> item.getGroups().clear());
                    media.flush();
                    media.deleteAll(removed);
                });
                service.initialize(page.getDocId(), user);
                assertTrue(media.findAllByMediaFkIdAndMediaFkTableNameAndDomainId(page.getDocId(), PlannedChangesService.MEDIA_TABLE, 1).isEmpty());
                DocHistory empty = history.saveAndFlush(revision(page));
                assertNotNull(service.publish(empty.getId()));
                assertTrue(media.findAllByMediaFkIdAndMediaFkTableNameAndDomainId(page.getDocId(), "documents", 1).isEmpty());

                // Only referenced assets are published; preflight failure leaves all public files unchanged.
                String privateRoot = PlannedAssetPaths.ROOT + page.getDocId();
                java.nio.file.Path source = assets.resolve(privateRoot.substring(1) + "/images/photo.webp");
                java.nio.file.Path target = assets.resolve("images/photo.webp");
                java.nio.file.Files.createDirectories(source.getParent());
                java.nio.file.Files.createDirectories(target.getParent());
                java.nio.file.Files.writeString(source, "future image");
                java.nio.file.Files.writeString(source.resolveSibling("unused.webp"), "unused secret");
                java.nio.file.Files.writeString(target, "old image");
                access.when(() -> PlannedAccess.canAccess(anyString(), any())).thenReturn(true);
                access.when(() -> PlannedAccess.checkedFile(anyString())).thenAnswer(call -> assets.resolve(((String)call.getArgument(0)).substring(1)));
                service.initialize(page.getDocId(), user);
                Media pdf = item(page.getDocId(), PlannedChangesService.MEDIA_TABLE, "PDF");
                pdf.setMediaLink(privateRoot + "/files/spec.pdf");
                media.saveAndFlush(pdf);
                DocHistory withFiles = revision(page);
                withFiles.setPerexImage(privateRoot + "/images/photo.webp");
                withFiles.setData("<img srcset='" + privateRoot + "/images/photo.webp 1x, " + privateRoot + "/images/photo.webp?v=1634319328 2x'>");
                withFiles = history.saveAndFlush(withFiles);
                long withFilesId = withFiles.getId();
                try (var filesystem = mockStatic(sk.iway.iwcm.io.IwcmFsDB.class); var cache = mockStatic(sk.iway.iwcm.Cache.class)) {
                    sk.iway.iwcm.Cache cacheInstance = mock(sk.iway.iwcm.Cache.class);
                    cache.when(sk.iway.iwcm.Cache::getInstance).thenReturn(cacheInstance);
                    assertThrows(IllegalArgumentException.class, () -> service.publish(withFilesId));
                    assertEquals("old image", java.nio.file.Files.readString(target));
                    assertTrue(history.findById(withFilesId).orElseThrow().getPublicable());
                    java.nio.file.Path pdfSource = assets.resolve(privateRoot.substring(1) + "/files/spec.pdf");
                    java.nio.file.Files.createDirectories(pdfSource.getParent());
                    java.nio.file.Files.writeString(pdfSource, "future PDF");
                    assertThrows(IllegalStateException.class, () -> interrupted.publish(withFilesId));
                    assertEquals(privateRoot + "/files/spec.pdf", media.findAllByMediaFkIdAndMediaFkTableNameAndDomainId(page.getDocId(), PlannedChangesService.MEDIA_TABLE, 1).get(0).getMediaLink(),
                        "Rewriting the live copy must preserve private source URLs for a retry");
                    DocDetails published = service.publish(withFilesId);
                    assertNotNull(published);
                    assertFalse(published.getData().contains(PlannedAssetPaths.ROOT));
                    assertEquals("<img srcset='/images/photo.webp 1x, /images/photo.webp?v=1634319328 2x'>", published.getData());
                    assertEquals("/images/photo.webp", published.getPerexImage());
                    assertEquals("/files/spec.pdf", media.findAllByMediaFkIdAndMediaFkTableNameAndDomainId(page.getDocId(), "documents", 1).get(0).getMediaLink());
                    assertEquals("future image", java.nio.file.Files.readString(target));
                    assertEquals("future PDF", java.nio.file.Files.readString(assets.resolve("files/spec.pdf")));
                    assertEquals("old image", java.nio.file.Files.readString(assets.resolve(privateRoot.substring(1) + "/backup/" + withFilesId + "/images/photo.webp")));
                    assertFalse(java.nio.file.Files.exists(assets.resolve("images/unused.webp")));
                    assertTrue(java.nio.file.Files.exists(source));
                    assertNull(service.publish(withFilesId));
                }

                service.initialize(page.getDocId(), user);
                DocHistory future = revision(page);
                future.setPublishStartDate(new Date(System.currentTimeMillis() + 60000));
                future = history.saveAndFlush(future);
                assertNull(service.publish(future.getId()));
                DocDetails replacement = DocDetailsToDocHistoryMapper.INSTANCE.docHistoryToDocDetails(future);
                replacement.setHistoryId(future.getHistoryId());
                service.prepareSave(replacement, user);
                assertTrue(history.findById(future.getId()).orElseThrow().getPublicable(), "Preparing a replacement must keep the existing plan active");
                DocHistory next = revision(page);
                next.setPublishStartDate(future.getPublishStartDate());
                next = history.saveAndFlush(next);
                service.completeSave(next);
                assertFalse(history.findById(future.getId()).orElseThrow().getPublicable());
                assertTrue(history.findById(next.getId()).orElseThrow().getPublicable());
                service.completeSave(future);
                assertTrue(history.findById(next.getId()).orElseThrow().getPublicable(), "An older save must not cancel a newer plan");
                service.cancel(page.getDocId(), user);
                assertNull(service.publish(future.getId()));
                assertTrue(media.findAllByMediaFkIdAndMediaFkTableNameAndDomainId(page.getDocId(), PlannedChangesService.MEDIA_TABLE, 1).isEmpty());

                service.initialize(page.getDocId(), user);
                long concurrentId = history.saveAndFlush(revision(page)).getId();
                var start = new java.util.concurrent.CountDownLatch(1);
                var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
                java.util.concurrent.Callable<DocDetails> publish = () -> {
                    try (var workerAccess = mockStatic(PlannedAccess.class); var workerGroups = mockStatic(GroupsDB.class);
                         var workerUsers = mockStatic(UsersDB.class); var workerDomains = mockStatic(CloudToolsForCore.class);
                         var workerDocs = mockStatic(DocDB.class); var workerAudit = mockStatic(Adminlog.class)) {
                        workerAccess.when(() -> PlannedAccess.canEdit(any(), any())).thenReturn(true);
                        workerGroups.when(GroupsDB::getInstance).thenReturn(groupDB);
                        workerUsers.when(() -> UsersDB.getUser(anyInt())).thenReturn(user);
                        workerDomains.when(CloudToolsForCore::getDomainId).thenReturn(1);
                        workerDomains.when(CloudToolsForCore::getDomainName).thenReturn(group.getDomainName());
                        workerDocs.when(DocDB::getInstance).thenReturn(docsCache);
                        start.await();
                        return service.publish(concurrentId);
                    }
                };
                try {
                    var first = executor.submit(publish);
                    var second = executor.submit(publish);
                    start.countDown();
                    int completed = (first.get(20, java.util.concurrent.TimeUnit.SECONDS) == null ? 0 : 1)
                        + (second.get(20, java.util.concurrent.TimeUnit.SECONDS) == null ? 0 : 1);
                    assertEquals(1, completed);
                    assertEquals(1, media.findAllByMediaFkIdAndMediaFkTableNameAndDomainId(page.getDocId(), "documents", 1).size());
                } finally { executor.shutdownNow(); }
            } finally {
                query.execute("DELETE FROM media_group_to_media WHERE media_id IN (SELECT media_id FROM media WHERE media_fk_id=? AND media_fk_table_name IN ('documents','planned_documents'))", savedPage.getDocId());
                query.execute("DELETE FROM media WHERE media_fk_id=? AND media_fk_table_name IN ('documents','planned_documents')", savedPage.getDocId());
                query.execute("DELETE FROM documents_history WHERE doc_id=?", savedPage.getDocId());
                query.execute("DELETE FROM documents WHERE doc_id=?", savedPage.getDocId());
                query.execute("DELETE FROM media_groups WHERE media_group_id=?", mediaGroup.getId());
            }
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
        DocHistoryRepository publicationHistory(@org.springframework.beans.factory.annotation.Qualifier("docHistoryRepository") DocHistoryRepository repository, java.util.concurrent.atomic.AtomicBoolean failPublication) {
            return (DocHistoryRepository) java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {DocHistoryRepository.class}, (proxy, method, args) -> {
                if (method.getName().equals("updatePublicableAndActual") && failPublication.getAndSet(false)) throw new IllegalStateException("Injected database failure");
                try { return method.invoke(repository, args); }
                catch (java.lang.reflect.InvocationTargetException exception) { throw exception.getCause(); }
            });
        }

        @Bean
        java.util.concurrent.atomic.AtomicBoolean failPublication() { return new java.util.concurrent.atomic.AtomicBoolean(); }

        @Bean
        PlannedChangesService plannedChangesService(DocDetailsRepository docs, @org.springframework.beans.factory.annotation.Qualifier("publicationHistory") DocHistoryRepository histories, MediaRepository media) {
            return new PlannedChangesService(docs, histories, media);
        }
    }
}
