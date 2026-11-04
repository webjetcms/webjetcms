package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;

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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.PkeyGenerator;
import sk.iway.iwcm.system.UpdateDBBean;
import sk.iway.iwcm.system.UpdateDatabase;
import sk.iway.iwcm.test.BaseWebjetTest;
import sk.iway.webjet.v9.V9JpaDBConfig;

/** Opt-in storage proof restricted to the disposable P01 single-writer instance. */
@EnabledIfEnvironmentVariable(named="WEBJET_P02_DB",matches="true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BranchStorageTest extends BaseWebjetTest {
    static final String URL="jdbc:mariadb://127.0.0.1:33316/webjet_p01";
    private final List<Long> branches=new ArrayList<>();

    @BeforeEach void configure() throws Exception {
        try(Connection connection=DBPool.getConnection()) { assertEquals("webjet_p01",connection.getCatalog()); assertTrue(connection.getMetaData().getURL().startsWith(URL)); }
        Constants.setBoolean("branchesEnabled",true);
        Constants.setBoolean("branchSingleWriterConfirmed",true);
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
        } finally { Constants.setBoolean("branchesEnabled",false); Constants.setBoolean("branchSingleWriterConfirmed",false); }
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
            assertEquals(context.branchId(),packages.findByIdAndDomainName(context.branchId(),"p02.example").orElseThrow().getId());
            assertTrue(packages.findByIdAndDomainName(context.branchId(),"other.example").isEmpty());
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
            BranchEntity packageRow=packages.findByIdAndDomainName(context.branchId(),"p02.example").orElseThrow();
            packageRow.setDescription("P02 JPA metadata");packages.save(packageRow);
            manager.flush();manager.getTransaction().commit();manager.clear();
            assertEquals("P02 JPA saved",repositories.getRepository(BranchDocumentRepository.class).findByBranchId(context.branchId()).get(0).getTitle());
            assertEquals("P02 JPA group",repositories.getRepository(BranchGroupRepository.class).findByBranchId(context.branchId()).get(0).getGroupName());
            assertEquals("P02 JPA media",repositories.getRepository(BranchMediaRepository.class).findByBranchId(context.branchId()).get(0).getMediaTitleSk());
            assertEquals("P02 JPA metadata",packages.findByIdAndDomainName(context.branchId(),"p02.example").orElseThrow().getDescription());
            assertEquals(1,packages.findByDomainNameOrderByIdDesc("p02.example").stream().filter(v->v.getId()==context.branchId()).count());
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
            Constants.setBoolean("branchSingleWriterConfirmed",false);assertThrows(IllegalStateException.class,()->BranchReadiness.verify(connection));Constants.setBoolean("branchSingleWriterConfirmed",true);
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

    private BranchContext branch(String name) {
        BranchEntity value=new BranchService().create("p02.example",name,null,1);branches.add(value.getId());
        assertTrue(new BranchService().find(value.getId(),"other.example").isEmpty());
        assertTrue(new BranchService().list("p02.example",false).stream().anyMatch(b->b.getId().equals(value.getId())));
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
        for(BranchType type:List.of(BranchType.DOCUMENT,BranchType.GROUP,BranchType.MEDIA)) {
            var names=new java.util.HashSet<String>();try(Statement statement=connection.createStatement();ResultSet result=statement.executeQuery("SHOW COLUMNS FROM "+type.workTable())){while(result.next())names.add(result.getString(1));}
            assertTrue(names.containsAll(BranchFields.forType(type).columns()));assertTrue(names.containsAll(List.of("branch_id","base_snapshot","base_hash","resolved_choice","apply_state")));
        }
    }
}
