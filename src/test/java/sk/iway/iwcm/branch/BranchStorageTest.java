package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.beans.XMLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.PkeyGenerator;
import sk.iway.iwcm.system.UpdateDBBean;
import sk.iway.iwcm.system.UpdateDatabase;
import sk.iway.webjet.v9.V9JpaDBConfig;

/** Opt-in storage proof restricted to the disposable P01 single-writer instance. */
@EnabledIfEnvironmentVariable(named="WEBJET_P02_DB",matches="true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BranchStorageTest extends BranchJpaTest {
    static final String URL="jdbc:mariadb://127.0.0.1:33316/webjet_p01";
    private final List<Long> branches=new ArrayList<>();
    @Autowired BranchRepository metadataRepository;
    @Autowired Validator validator;

    @BeforeEach void configure() throws Exception {
        try(Connection connection=DBPool.getConnection()) { assertEquals("webjet_p01",connection.getCatalog()); assertTrue(connection.getMetaData().getURL().startsWith(URL)); }
        Constants.setBoolean("enableStaticFilesExternalDir",true);
        BranchTestDomains.ensure("p02.example"); BranchTestDomains.ensure("p02.other"); BranchTestDomains.ensure("other.example");
        var requestBean = new sk.iway.iwcm.RequestBean(); requestBean.setDomain("p02.example");
        sk.iway.iwcm.SetCharacterEncodingFilter.setCurrentRequestBean(requestBean);
        Constants.setBoolean("branchesEnabled",true);
        Constants.setBoolean("branchSingleWriterConfirmed",true);Constants.setBoolean("enableStaticFilesExternalDir",true);
        Files.createDirectories(Path.of("/tmp/webjet-p02-private"));
        Constants.setString("branchStorageRoot","/tmp/webjet-p02-private");
    }

    @AfterEach void restore() throws Exception {
        try(Connection connection=connect()) {
            for(long branch:branches) {
                for(String table:List.of("branch_documents","branch_groups","branch_media","branch_files","branches")) {
                    try(PreparedStatement statement=connection.prepareStatement("DELETE FROM "+table+" WHERE "+(table.equals("branches")?"id":"branch_id")+"=?")) {statement.setLong(1,branch);statement.executeUpdate();}
                }
            }
        } finally { Constants.setBoolean("branchesEnabled",false); Constants.setBoolean("branchSingleWriterConfirmed",false); sk.iway.iwcm.SetCharacterEncodingFilter.setCurrentRequestBean(null); }
    }

    /** Runs the real XML update path on existing data and its SQL on an empty current-profile schema. */
    @Test @Order(1) void migrationOnExistingAndCleanProfile() throws Exception {
        UpdateDBBean migration=migration();
        var update=UpdateDatabase.class.getDeclaredMethod("updateDB",UpdateDBBean.class,String.class);
        update.setAccessible(true);
        update.invoke(null,migration,"iwcm"); // The legacy helper returns false even after a successful update.
        try(Connection connection=connect()) { assertEquals(1,count(connection,"SELECT COUNT(*) FROM _db_ WHERE note=\""+migration.getNote()+"\"")); }
        try(Connection connection=connect()) {
            execute(connection,"INSERT IGNORE INTO pkey_generator(name,value,table_name,table_pkey_name) VALUES ('media',1,'media','media_id')");
            BranchReadiness.verify(connection);
            try(Statement statement=connection.createStatement()) {
                statement.execute("CREATE DATABASE IF NOT EXISTS webjet_p02_clean CHARACTER SET utf8mb4");
                try(Connection clean=DriverManager.getConnection("jdbc:mariadb://127.0.0.1:33316/webjet_p02_clean","root","")) {
                    apply(clean,migration.getMysql()); apply(clean,migration.getMysql());
                    try(ResultSet result=clean.createStatement().executeQuery("SHOW TABLES")) {int count=0;while(result.next())count++;assertEquals(5,count);}
                    assertColumns(clean);
                }
            }
            apply(connection,migration.getMysql());
            assertColumns(connection);
            assertEquals(0,count(connection,"SELECT COUNT(*) FROM documents WHERE title='autotest P02 technical'"));
        }
    }

    /** Both the selected color and the default for older callers survive a fresh metadata query. */
    @Test @Order(2) void persistsWorkingVersionColor() {
        var service = branchService();
        long legacyId = branch("P02 default color").branchId();
        assertEquals("cyan", service.find(legacyId, "p02.example").orElseThrow().getColor());
        var colored = createBranch("p02.example", "P02 selected color", null, "lavender", 1);
        branches.add(colored.getId());
        assertEquals("lavender", service.find(colored.getId(), "p02.example").orElseThrow().getColor());
    }

    /** Metadata edits preserve publication state and revision, and cannot cross the domain boundary. */
    @Test @Order(2) void updatesOnlyWorkingVersionMetadata() {
        var service = branchService();
        var original = createBranch("p02.example", "autotest metadata", "Original", "cyan", 1);
        branches.add(original.getId());
        editMetadata(original.getId(), BranchDomains.id("p02.example"), "  autotest renamed  ", "Updated", "lavender");
        var saved = service.find(original.getId(), "p02.example").orElseThrow();
        assertEquals("autotest renamed", saved.getName());
        assertEquals("Updated", saved.getDescription());
        assertEquals("lavender", saved.getColor());
        assertEquals(original.getState(), saved.getState());
        assertEquals(original.getRevision(), saved.getRevision());
        assertEquals(BranchDomains.id("p02.example"), saved.getDomainId());
        assertThrows(RuntimeException.class, () -> editMetadata(original.getId(), BranchDomains.id("other.example"), "No", null, "cyan"));
        assertThrows(ConstraintViolationException.class, () -> editMetadata(original.getId(), BranchDomains.id("p02.example"), "No", null, "x".repeat(17)));
        assertEquals("autotest renamed", service.find(original.getId(), "p02.example").orElseThrow().getName());
        editMetadata(original.getId(), original.getDomainId(), "autotest renamed", "Updated", "future-color");
        assertEquals("future-color", service.find(original.getId(), "p02.example").orElseThrow().getColor());
    }

    /** Publication fields support ordinary JPA persistence as well as the publisher's conditional transition. */
    @Test @Order(2) void persistsPublicationFieldsThroughJpa() {
        var service=branchService();
        var branch=createBranch("p02.example","autotest publication metadata",null,1);
        branches.add(branch.getId());
        var at=java.sql.Timestamp.valueOf("2026-11-12 10:00:00");
        branch.setState("MERGING");branch.setRevision(1L);branch.setPublishAt(at);branch.setScheduledBy(1);
        branch.setMergeStartedAt(at);branch.setMergeFinishedAt(at);branch.setLastError("Previous attempt");
        metadataRepository.saveAndFlush(branch);
        var reloaded=service.find(branch.getId(),branch.getDomainId()).orElseThrow();
        assertEquals("MERGING",reloaded.getState());assertEquals(1L,reloaded.getRevision());
        assertEquals(at,reloaded.getPublishAt());assertEquals(1,reloaded.getScheduledBy());
        assertEquals(at,reloaded.getMergeStartedAt());assertEquals(at,reloaded.getMergeFinishedAt());
        assertEquals("Previous attempt",reloaded.getLastError());
        service.completePublication(branch.getId());
        reloaded=service.find(branch.getId(),branch.getDomainId()).orElseThrow();
        assertEquals("MERGED",reloaded.getState());assertEquals(2L,reloaded.getRevision());assertNull(reloaded.getLastError());
    }

    /** The inherited create flow assigns server identity and cannot insert publication metadata from the client. */
    @Test @Order(2) void standardCreateRejectsOperationalStateFromThePayload() {
        var input=new BranchEntity();input.setId(999L);input.setDomainId(BranchDomains.id("p02.other"));input.setName("autotest standard create");
        input.setState("MERGED");input.setRevision(999L);input.setCreatedBy(999);input.setCreatedAt(new java.sql.Timestamp(1000));
        input.setPublishAt(new java.sql.Timestamp(2000));input.setScheduledBy(999);input.setMergeStartedAt(new java.sql.Timestamp(3000));
        input.setMergeFinishedAt(new java.sql.Timestamp(4000));input.setLastError("forged");
        var controller=editor(metadataRepository);
        var request=new DatatableRequest<Long,BranchEntity>();request.setAction("create");request.setData(Map.of(-1L,input));
        var saved=controller.handleEditor(controller.getRequest(),request).getBody().getData().get(0);
        branches.add(saved.getId());
        var reloaded=branchService().find(saved.getId(),BranchDomains.id("p02.example")).orElseThrow();
        assertEquals("DRAFT",reloaded.getState());assertEquals(0L,reloaded.getRevision());assertEquals(1,reloaded.getCreatedBy());
        assertTrue(reloaded.getCreatedAt().after(new java.sql.Timestamp(1000)));
        assertNull(reloaded.getPublishAt());assertNull(reloaded.getScheduledBy());assertNull(reloaded.getMergeStartedAt());
        assertNull(reloaded.getMergeFinishedAt());assertNull(reloaded.getLastError());
    }

    /** Entity callbacks supply a single CREATE and UPDATE audit for metadata persistence. */
    @Test @Order(2) void metadataUsesTheStandardJpaAuditListener() {
        try(var runtime=mockStatic(sk.iway.iwcm.InitServlet.class,CALLS_REAL_METHODS);
                var audit=mockStatic(sk.iway.iwcm.Adminlog.class)) {
            runtime.when(sk.iway.iwcm.InitServlet::isSpringInitialized).thenReturn(true);
            var branch=createBranch("p02.example","autotest JPA audit",null,1);
            branches.add(branch.getId());
            editMetadata(branch.getId(),branch.getDomainId(),"autotest JPA audit updated",null,"blue");
            audit.verify(()->sk.iway.iwcm.Adminlog.add(eq(sk.iway.iwcm.Adminlog.TYPE_CLIENT_SPECIFIC),startsWith("CREATE:"),eq(branch.getId().intValue()),eq(-1)),times(1));
            audit.verify(()->sk.iway.iwcm.Adminlog.add(eq(sk.iway.iwcm.Adminlog.TYPE_CLIENT_SPECIFIC),startsWith("UPDATE:"),eq(branch.getId().intValue()),eq(-1)),times(1));
        }
    }

    /** Removal in every lifecycle state cascades WORK data and private bytes while preserving the LIVE attachment. */
    @Test @Order(2) void deletesWorkingVersionsAndTheirDataInEveryState() throws Exception {
        String logical="/files/autotest-branch-delete-"+System.nanoTime()+".txt";
        Path live=Path.of(sk.iway.iwcm.Tools.getRealPath(logical));
        Files.createDirectories(live.getParent());Files.writeString(live,"LIVE attachment");
        List<Long> created=new ArrayList<>();
        try {
            for(String state:List.of("DRAFT","SCHEDULED","MERGING","MERGE_FAILED","NEEDS_ATTENTION","MERGED","CANCELLED")) {
                var branch=createBranch("p02.example","autotest delete "+state,null,1);
                branches.add(branch.getId());created.add(branch.getId());
                var context=new BranchContext(branch.getId(),"p02.example",1,BranchContext.Mode.ADMIN);
                var store=new BranchStore();
                var doc=store.create(context,BranchType.DOCUMENT,BranchDocumentMapper.initialValues(1,1,Instant.now(),false));
                store.create(context,BranchType.GROUP,BranchGroupMapper.initialValues(1,"p02.example"));
                store.create(context,BranchType.MEDIA,BranchMediaMapper.initialValues(1,((Number)doc.get("doc_id")).intValue(),1));
                var file=store.putFile(context,logical,"FILE",BranchFiles.liveSnapshot(logical),"a".repeat(64),10L,"text/plain",false);
                Path work=BranchFiles.workPath(branch.getId(),((Number)file.get("row_id")).longValue());
                Files.createDirectories(work.getParent());Files.writeString(work,"WORK bytes");
                Files.writeString(work.resolveSibling("base"),"BASE bytes");
                Files.writeString(work.resolveSibling("publish-backup"),"Backup bytes");
                Path privateDirectory=work.getParent().getParent();
                Path thumbnails=Path.of(Constants.getString("branchStorageRoot")).resolve("thumbs").resolve(Long.toString(branch.getId()));
                Files.createDirectories(thumbnails);Files.writeString(thumbnails.resolve("autotest.png"),"Thumbnail bytes");
                branch=branchService().find(branch.getId(),branch.getDomainId()).orElseThrow();
                branch.setState(state);metadataRepository.saveAndFlush(branch);
                var request=new DatatableRequest<Long,BranchEntity>();request.setAction("remove");request.setData(Map.of(branch.getId(),branch));
                var controller=editor(metadataRepository);
                assertNull(controller.handleEditor(controller.getRequest(),request).getBody().getError(),state);
                assertTrue(branchService().find(branch.getId(),branch.getDomainId()).isEmpty(),state);
                try(Connection connection=connect()) {
                    for(String table:List.of("branch_documents","branch_groups","branch_media","branch_files"))
                        assertEquals(0,count(connection,"SELECT COUNT(*) FROM "+table+" WHERE branch_id="+branch.getId()),state+": "+table);
                }
                assertFalse(Files.exists(privateDirectory),state);
                assertFalse(Files.exists(thumbnails),state);
                assertEquals("LIVE attachment",Files.readString(live),state);
            }
        } finally {
            Files.deleteIfExists(live);
            for(long id:created)BranchFiles.deleteBranch(id);
        }
    }

    /** A domain mismatch cannot remove another website's metadata or private staging directory. */
    @Test @Order(2) void deletionCannotCrossTheDomainBoundary() throws Exception {
        var branch=createBranch("p02.other","autotest foreign deletion",null,1);
        branches.add(branch.getId());
        Path file=BranchFiles.workPath(branch.getId(),1);
        Files.createDirectories(file.getParent());Files.writeString(file,"Foreign WORK bytes");
        try {
            var error=assertThrows(ResponseStatusException.class,()->branchService().delete(branch.getId(),BranchDomains.id("p02.example")));
            assertEquals(404,error.getStatusCode().value());
            assertTrue(branchService().find(branch.getId(),branch.getDomainId()).isPresent());
            assertEquals("Foreign WORK bytes",Files.readString(file));
        } finally { BranchFiles.deleteBranch(branch.getId()); }
    }

    /** Repository and DataTable reads enforce the numeric domain scope. */
    @Test @Order(3) void domainRepositoryScopesDatatableReads() throws Exception {
        var service = branchService();
        var own = createBranch("p02.example", "autotest domain scope", null, 1);
        var otherId = createBranch("p02.other", "autotest foreign domain ID", null, 1);
        var otherName = createBranch("p02.other", "autotest foreign domain name", null, 1);
        branches.addAll(List.of(own.getId(), otherId.getId(), otherName.getId()));
        int domainId = own.getDomainId();
        var factoryBean = new V9JpaDBConfig().entityManagerFactory();
        factoryBean.setPersistenceUnitPostProcessors(unit -> unit.getManagedClassNames().removeIf(name ->
            Files.exists(Path.of("build/classes/java/test", name.replace('.', '/') + ".class"))));
        factoryBean.afterPropertiesSet();
        var factory = factoryBean.getObject();
        assertNotNull(factory);
        try (EntityManager manager = factory.createEntityManager()) {
            BranchRepository repository = new JpaRepositoryFactory(manager).getRepository(BranchRepository.class);
            assertTrue(repository.findFirstByIdAndDomainId(otherId.getId(), domainId).isEmpty());
            assertEquals(List.of(own.getId()), repository.findAllByIdInAndDomainId(List.of(own.getId(), otherId.getId()), domainId)
                .stream().map(BranchEntity::getId).toList());

            var request = new MockHttpServletRequest();
            var access = mock(BranchAccess.class);
            var user = mock(Identity.class);
            when(access.requireUser(request)).thenReturn(user);
            when(access.requireDomain(request, user)).thenReturn("p02.example");
            var controller = new BranchRestController(repository, branchService(), access);
            controller.setRequest(request);
            ReflectionTestUtils.setField(controller, "checkDomainId", true);
            request.addParameter("searchId", own.getId().toString());
            var pageable = PageRequest.of(0, 10);
            assertEquals(List.of(own.getId()), controller.getAllItems(pageable).getContent().stream().map(BranchEntity::getId).toList());
            request.setParameter("searchId", otherId.getId().toString());
            assertTrue(controller.searchItem(Map.of("searchId", otherId.getId().toString()), pageable, new BranchEntity()).isEmpty());
            assertTrue(controller.searchItem(Map.of("searchDomainId", Integer.toString(otherName.getDomainId())), pageable, new BranchEntity()).isEmpty());
            assertNull(controller.getOneItem(otherId.getId()));
            assertNull(controller.getOneItem(otherName.getId()));
        } finally { factory.close(); }
    }

    /** Stores changes in two branches while LIVE content, history and derived indexes remain untouched. */
    @Test @Order(2) void storageIsolationStableIdsAndStaleEdits() throws Exception {
        BranchContext a=branch("P02 A"); BranchContext b=branch("P02 B");
        BranchStore store=new BranchStore();
        long history;
        try(Connection connection=connect()){history=count(connection,"SELECT COUNT(*) FROM documents_history");}
        Map<String,Object> document=new LinkedHashMap<>(BranchDocumentMapper.initialValues(1,1,Instant.parse("2026-11-04T10:00:00Z"),false));
        document.put("title","P02 A page"); document.put("data","<p>"+"content".repeat(45000)+"</p>");document.put("group_id",900001);
        document.put("perex_group",",12,3,12,");
        Map<String,Object> saved=store.create(a,BranchType.DOCUMENT,document);
        int id=((Number)saved.get("doc_id")).intValue();
        assertEquals(",3,12,",saved.get("perex_group"));
        assertEquals(document.get("data"),saved.get("data"));
        Map<String,Object> changed=new LinkedHashMap<>(saved);changed.put("title","P02 edited reserved page");
        Map<String,Object> edited=store.put(a,BranchType.DOCUMENT,id,changed,BranchStore.snapshot(BranchType.DOCUMENT,saved).hash(),false);
        assertEquals(saved.get("row_id"),edited.get("row_id"));
        assertEquals("P02 edited reserved page",edited.get("title"));assertEquals(BranchSnapshot.absent().hash(),edited.get("base_hash"));
        assertThrows(IllegalStateException.class,()->store.put(a,BranchType.DOCUMENT,id,changed,"stale",false));
        Map<String,Object> group=new LinkedHashMap<>(BranchGroupMapper.initialValues(1,"p02.example"));group.put("group_name","P02 group");
        Map<String,Object> newGroup=store.create(a,BranchType.GROUP,group);
        Map<String,Object> media=new LinkedHashMap<>(BranchMediaMapper.initialValues(1,id,1));media.put("group_ids_snapshot",List.of(3,2,3));
        Map<String,Object> newMedia=store.create(a,BranchType.MEDIA,media);
        assertEquals("[2, 3]",newMedia.get("group_ids_snapshot"));
        Map<String,Object> live;
        try(Connection connection=connect()) {live=BranchStore.readLive(connection,BranchType.DOCUMENT,900001).orElseThrow();}
        Map<String,Object> workA=new LinkedHashMap<>(live);workA.put("title","A title");
        Map<String,Object> workB=new LinkedHashMap<>(live);workB.put("title","B title");
        String base=BranchStore.snapshot(BranchType.DOCUMENT,live).hash();
        store.put(a,BranchType.DOCUMENT,900001,workA,base,false);store.put(b,BranchType.DOCUMENT,900001,workB,base,false);
        assertThrows(IllegalStateException.class,()->store.put(b,BranchType.DOCUMENT,900002,workB,"old live hash",false));
        try(Connection connection=connect()) {
            assertTrue(BranchStore.readLive(connection,BranchType.DOCUMENT,id).isEmpty());
            assertTrue(BranchStore.readLive(connection,BranchType.GROUP,((Number)newGroup.get("group_id")).intValue()).isEmpty());
            assertTrue(BranchStore.readLive(connection,BranchType.MEDIA,((Number)newMedia.get("media_id")).intValue()).isEmpty());
            assertEquals("A title",BranchStore.readWork(connection,a.branchId(),BranchType.DOCUMENT,900001).orElseThrow().get("title"));
            assertEquals("B title",BranchStore.readWork(connection,b.branchId(),BranchType.DOCUMENT,900001).orElseThrow().get("title"));
            assertEquals(live.get("title"),BranchStore.readLive(connection,BranchType.DOCUMENT,900001).orElseThrow().get("title"));
            assertEquals(history,count(connection,"SELECT COUNT(*) FROM documents_history"));
            assertEquals(0,count(connection,"SELECT COUNT(*) FROM media_group_to_media WHERE media_id="+newMedia.get("media_id")));
        }
        Map<String,Object> directory=store.putFile(a,"/files/p02/./empty/","DIRECTORY",BranchSnapshot.absent(),null,null,null,false);
        assertEquals("/files/p02/empty",directory.get("logical_path"));assertNull(directory.get("size_bytes"));
        assertEquals("file:/files/p02/empty",BranchStore.change(BranchType.FILE,directory).identity());
        assertTrue(BranchStore.change(BranchType.DOCUMENT,edited).decide(BranchSnapshot.absent()).writeRequired());
        Map<String,Object> deletion=store.put(b,BranchType.DOCUMENT,900001,workB,BranchStore.snapshot(BranchType.DOCUMENT,workB).hash(),true);
        assertEquals(BranchSnapshot.State.TRASH,BranchStore.change(BranchType.DOCUMENT,deletion).work().state());
        assertThrows(IllegalArgumentException.class,()->store.putFile(a,"/files/p02/empty","DIRECTORY",BranchSnapshot.absent(),null,null,null,true));
        store.putFile(a,"/files/p02/a.pdf","FILE",BranchSnapshot.absent(),"a".repeat(64),12L,"application/pdf",false);
        assertFalse(Files.exists(Path.of("src/main/webapp/files/p02/a.pdf")));
        assertThrows(IllegalArgumentException.class,()->store.putFile(a,"/images/gallery/p02.jpg","FILE",BranchSnapshot.absent(),"a".repeat(64),12L,"image/jpeg",false));
    }

    /** Verifies the installed JPA configuration and all scoped repositories against real working rows. */
    @Test @Order(3) void jpaRepositoriesReloadTypedRows() throws Exception {
        BranchContext context=branch("P02 JPA");
        BranchStore store=new BranchStore();
        Map<String,Object> doc=store.create(context,BranchType.DOCUMENT,BranchDocumentMapper.initialValues(1,1,Instant.now(),false));
        Map<String,Object> group=store.create(context,BranchType.GROUP,BranchGroupMapper.initialValues(1,"p02.example"));
        Map<String,Object> medium=new LinkedHashMap<>(BranchMediaMapper.initialValues(1,((Number)doc.get("doc_id")).intValue(),1));medium.put("group_ids_snapshot","[3,2]");
        Map<String,Object> media=store.create(context,BranchType.MEDIA,medium);
        store.putFile(context,"/files/p02-jpa-empty","DIRECTORY",BranchSnapshot.absent(),null,null,null,false);
        var factoryBean=new V9JpaDBConfig().entityManagerFactory();
        // Keep production scanning intact while excluding unrelated @Entity fixtures in the test output.
        factoryBean.setPersistenceUnitPostProcessors(unit -> unit.getManagedClassNames().removeIf(name ->
            Files.exists(Path.of("build/classes/java/test",name.replace('.', '/')+".class"))));
        factoryBean.afterPropertiesSet();
        var factory=factoryBean.getObject();assertNotNull(factory);
        try(EntityManager manager=factory.createEntityManager()) {
            JpaRepositoryFactory repositories=new JpaRepositoryFactory(manager);
            BranchRepository packages=repositories.getRepository(BranchRepository.class);
            assertEquals(context.branchId(),packages.findFirstByIdAndDomainId(context.branchId(),BranchDomains.id("p02.example")).orElseThrow().getId());
            assertTrue(packages.findFirstByIdAndDomainId(context.branchId(),BranchDomains.id("other.example")).isEmpty());
            BranchDocumentEntity reloaded=repositories.getRepository(BranchDocumentRepository.class).findByBranchIdAndDocId(context.branchId(),((Number)doc.get("doc_id")).intValue()).orElseThrow();
            assertEquals(doc.get("title"),reloaded.getTitle());assertNotNull(reloaded.getDateCreated());
            assertEquals(1,repositories.getRepository(BranchGroupRepository.class).findByBranchId(context.branchId()).size());
            assertEquals(((Number)media.get("media_id")).intValue(),repositories.getRepository(BranchMediaRepository.class).findByBranchId(context.branchId()).get(0).getMediaId());
            assertEquals("DIRECTORY",repositories.getRepository(BranchFileRepository.class).findByBranchId(context.branchId()).get(0).getEntryKind());
            manager.getTransaction().begin();
            reloaded.setTitle("P02 JPA saved");
            repositories.getRepository(BranchDocumentRepository.class).save(reloaded);
            BranchGroupEntity groupRow=repositories.getRepository(BranchGroupRepository.class).findByBranchId(context.branchId()).get(0);
            groupRow.setGroupName("P02 JPA group");repositories.getRepository(BranchGroupRepository.class).save(groupRow);
            BranchMediaEntity mediaRow=repositories.getRepository(BranchMediaRepository.class).findByBranchId(context.branchId()).get(0);
            mediaRow.setMediaTitleSk("P02 JPA media");repositories.getRepository(BranchMediaRepository.class).save(mediaRow);
            BranchFileEntity fileRow=repositories.getRepository(BranchFileRepository.class).findByBranchId(context.branchId()).get(0);
            fileRow.setApplyState("PENDING");repositories.getRepository(BranchFileRepository.class).save(fileRow);
            BranchEntity packageRow=packages.findFirstByIdAndDomainId(context.branchId(),BranchDomains.id("p02.example")).orElseThrow();
            packageRow.setDescription("P02 JPA metadata");packages.save(packageRow);
            manager.flush();manager.getTransaction().commit();manager.clear();
            assertEquals("P02 JPA saved",repositories.getRepository(BranchDocumentRepository.class).findByBranchId(context.branchId()).get(0).getTitle());
            assertEquals("P02 JPA group",repositories.getRepository(BranchGroupRepository.class).findByBranchId(context.branchId()).get(0).getGroupName());
            assertEquals("P02 JPA media",repositories.getRepository(BranchMediaRepository.class).findByBranchId(context.branchId()).get(0).getMediaTitleSk());
            assertEquals("P02 JPA metadata",packages.findFirstByIdAndDomainId(context.branchId(),BranchDomains.id("p02.example")).orElseThrow().getDescription());
            assertEquals(1,packages.findAllByDomainId(BranchDomains.id("p02.example")).stream().filter(v->v.getId()==context.branchId()).count());
        }finally{factoryBean.destroy();}
    }

    /** A failing WORK insert rolls back the technical row and never returns an identity. */
    @Test @Order(4) void failedReservationAndReadinessGuards() throws Exception {
        BranchContext context=branch("P02 rollback");BranchStore store=new BranchStore();
        Map<String,Object> invalid=new LinkedHashMap<>(BranchDocumentMapper.initialValues(1,1,Instant.now(),false));invalid.put("title","x".repeat(256));
        long liveBefore,workBefore;
        try(Connection connection=connect()){liveBefore=count(connection,"SELECT COUNT(*) FROM documents");workBefore=count(connection,"SELECT COUNT(*) FROM branch_documents WHERE branch_id="+context.branchId());}
        assertThrows(SQLException.class,()->store.create(context,BranchType.DOCUMENT,invalid));
        try(Connection connection=connect()) {
            assertEquals(liveBefore,count(connection,"SELECT COUNT(*) FROM documents"));assertEquals(workBefore,count(connection,"SELECT COUNT(*) FROM branch_documents WHERE branch_id="+context.branchId()));
            Constants.setBoolean("branchSingleWriterConfirmed",false);assertThrows(IllegalStateException.class,()->BranchReadiness.verify(connection));Constants.setBoolean("branchSingleWriterConfirmed",true);Constants.setBoolean("enableStaticFilesExternalDir",true);
            Constants.setString("branchStorageRoot",Path.of("src/main/webapp").toAbsolutePath().toString());assertThrows(SQLException.class,()->BranchReadiness.verify(connection));Constants.setString("branchStorageRoot","/tmp/webjet-p02-private");
            execute(connection,"ALTER TABLE media_group_to_media ENGINE=MyISAM");
            try{assertThrows(SQLException.class,()->BranchReadiness.verify(connection));}finally{execute(connection,"ALTER TABLE media_group_to_media ENGINE=InnoDB");}
            Constants.setBoolean("branchesEnabled",false);assertThrows(IllegalStateException.class,()->store.create(context,BranchType.DOCUMENT,invalid));
        }
    }

    /** A real SELECT-only account fails the rollback privilege probe; private storage checks reject missing paths. */
    @Test @Order(5) void rejectedWritePrivilegesAndActivation() throws Exception {
        BranchReadiness.verifyActivation();
        try(Connection root=connect()) {
            execute(root,"CREATE USER IF NOT EXISTS 'p02_readonly'@'localhost' IDENTIFIED BY 'p02-disposable-only'");
            execute(root,"GRANT SELECT ON webjet_p01.* TO 'p02_readonly'@'localhost'");
            try(Connection readonly=DriverManager.getConnection(URL,"p02_readonly","p02-disposable-only")) {
                readonly.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                BranchReadiness.verify(readonly);
                readonly.setAutoCommit(false);
                try{assertThrows(SQLException.class,()->BranchReadiness.verifyWritePrivileges(readonly));}finally{readonly.rollback();}
            } finally { execute(root,"DROP USER 'p02_readonly'@'localhost'"); }
        }
        Constants.setString("branchStorageRoot","/tmp/webjet-p02-directory-does-not-exist");
        assertThrows(java.io.IOException.class,BranchReadiness::privateRoot);
    }

    /** Explicit media insertion preserves its identity/categories and a failed WORK insert leaves only a generator gap. */
    @Test @Order(6) void explicitMediaWriterAndFailedWorkInsert() throws Exception {
        BranchContext context=branch("P02 media writer");BranchStore store=new BranchStore();
        Map<String,Object> initial=new LinkedHashMap<>(BranchMediaMapper.initialValues(1,900001,1));
        initial.put("media_title_sk","x".repeat(256));
        assertThrows(SQLException.class,()->store.create(context,BranchType.MEDIA,initial));
        initial.put("media_title_sk","P02 reserved media");initial.put("group_ids_snapshot",List.of(2,3));
        Map<String,Object> saved=store.create(context,BranchType.MEDIA,initial);
        try(Connection connection=connect()) {
            int id=((Number)saved.get("media_id")).intValue();
            assertTrue(BranchStore.readLive(connection,BranchType.MEDIA,id).isEmpty());
            connection.setAutoCommit(false);
            try {
                BranchRowWriter.insertMediaWithId(connection,BranchContext.live("p02.example",1),saved,List.of(3,2,3));
                Map<String,Object> live=BranchStore.readLive(connection,BranchType.MEDIA,id).orElseThrow();
                assertEquals("[2, 3]",live.get("group_ids_snapshot"));
                assertEquals(BranchStore.snapshot(BranchType.MEDIA,saved).hash(),BranchStore.snapshot(BranchType.MEDIA,live).hash());
                assertThrows(SQLException.class,()->BranchRowWriter.insertMediaWithId(connection,BranchContext.live("p02.example",1),saved,List.of(2,3)));
                assertThrows(IllegalArgumentException.class,()->BranchRowWriter.insertMediaWithId(connection,context,saved,List.of(2,3)));
            }finally{connection.rollback();}
        }
    }

    private BranchRestController editor(BranchRepository repository) {
        var request=new MockHttpServletRequest();
        var user=new Identity();user.setAdmin(true);user.setUserId(1);
        request.getSession().setAttribute(Constants.USER_KEY,user);
        var controller=new BranchRestController(repository,branchService(),mock(BranchAccess.class));
        controller.setRequest(request);
        ReflectionTestUtils.setField(controller,"checkDomainId",true);
        ReflectionTestUtils.setField(controller,"validator",validator);
        return controller;
    }

    private BranchEntity editMetadata(long id,int domainId,String name,String description,String color) {
        var entity=new BranchEntity();entity.setId(id);entity.setDomainId(domainId);entity.setName(name);entity.setDescription(description);entity.setColor(color);
        var controller=editor(metadataRepository);
        var request=new DatatableRequest<Long,BranchEntity>();request.setAction("edit");request.setData(Map.of(id,entity));
        return controller.handleEditor(controller.getRequest(),request).getBody().getData().get(0);
    }

    private BranchContext branch(String name) {
        BranchEntity value=createBranch("p02.example",name,null,1);branches.add(value.getId());
        assertTrue(branchService().find(value.getId(),"other.example").isEmpty());
        assertTrue(branchService().list("p02.example",false).stream().anyMatch(b->b.getId().equals(value.getId())));
        return new BranchContext(value.getId(),"p02.example",1,BranchContext.Mode.ADMIN);
    }

    static Connection connect() throws SQLException {Connection connection=DriverManager.getConnection(URL,"root","");connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);return connection;}
    static UpdateDBBean migration() throws Exception {
        try(XMLDecoder decoder=new XMLDecoder(Files.newInputStream(Path.of("src/main/webapp/WEB-INF/sql/autoupdate-webjet9.xml")))) {
            return ((List<?>)decoder.readObject()).stream().filter(UpdateDBBean.class::isInstance).map(UpdateDBBean.class::cast).filter(v->v.getDesc().startsWith("Publishing branches P02")).findFirst().orElseThrow();
        }
    }
    static void apply(Connection connection,String sql) throws SQLException {for(String part:sql.split(";"))if(!part.isBlank())execute(connection,part);}
    static void execute(Connection connection,String sql) throws SQLException {try(Statement statement=connection.createStatement()){statement.execute(sql);}}
    static long count(Connection connection,String sql) throws SQLException {try(Statement statement=connection.createStatement();ResultSet result=statement.executeQuery(sql)){assertTrue(result.next());return result.getLong(1);}}
    static void assertColumns(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet color = statement.executeQuery("SHOW COLUMNS FROM branches LIKE 'color'")) {
            assertTrue(color.next());
            assertEquals("varchar(16)", color.getString("Type"));
            assertEquals("NO", color.getString("Null"));
            assertEquals("cyan", color.getString("Default"));
        }
        try (Statement statement = connection.createStatement();
                ResultSet domainId = statement.executeQuery("SHOW COLUMNS FROM branches LIKE 'domain_id'")) {
            assertTrue(domainId.next());
            assertEquals("NO", domainId.getString("Null"));
        }
        for(BranchType type:List.of(BranchType.DOCUMENT,BranchType.GROUP,BranchType.MEDIA)) {
            var names=new java.util.HashSet<String>();try(Statement statement=connection.createStatement();ResultSet result=statement.executeQuery("SHOW COLUMNS FROM "+type.workTable())){while(result.next())names.add(result.getString(1));}
            assertTrue(names.containsAll(BranchFields.forType(type).columns()));assertTrue(names.containsAll(List.of("branch_id","base_snapshot","base_hash","resolved_choice","apply_state")));
        }
    }
}
