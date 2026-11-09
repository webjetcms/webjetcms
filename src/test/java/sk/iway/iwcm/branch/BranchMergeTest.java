package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.test.util.ReflectionTestUtils;
import sk.iway.iwcm.*;
import sk.iway.iwcm.components.perex_groups.*;
import sk.iway.iwcm.doc.*;
import sk.iway.iwcm.test.BaseWebjetTest;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.webjet.v9.V9JpaDBConfig;

/** Real MariaDB manual publication, stale-input checks and checkpoint recovery on isolated P06 data. */
@EnabledIfEnvironmentVariable(named="WEBJET_P06_DB",matches="true")
class BranchMergeTest extends BaseWebjetTest {
    static final String DOMAIN="iwcm.interway.sk";
    final BranchMergeService service=new BranchMergeService();
    final BranchStore store=new BranchStore();
    final Map<String,String> original=new LinkedHashMap<>();
    Fixture f;
    Identity user;
    static LocalContainerEntityManagerFactoryBean jpa;
    static GenericApplicationContext spring;
    static Object previousSpring,previousFactories;

    @BeforeAll static void initializePersistence()throws Exception {
        try(Connection c=DBPool.getConnection()) {assertEquals("webjet_p01",c.getCatalog());assertTrue(c.getMetaData().getURL().contains(":33316/"));}
        jpa=new V9JpaDBConfig().entityManagerFactory();
        jpa.setPersistenceUnitPostProcessors(unit->{
            unit.getManagedClassNames().removeIf(name->Files.exists(Path.of("build/classes/java/test",name.replace('.','/')+".class")));
            unit.getManagedClassNames().add(sk.iway.iwcm.io.FileHistoryBean.class.getName());
        });
        jpa.afterPropertiesSet();
        spring=new GenericApplicationContext();
        var beans=spring.getBeanFactory();
        beans.registerSingleton("webjet2022EntityManager",jpa.getObject());
        beans.registerSingleton("transactionManager",new JpaTransactionManager(jpa.getObject()));
        var repository=new JpaRepositoryFactoryBean<PerexGroupDocRepository,PerexGroupDocEntity,Long>(PerexGroupDocRepository.class);
        repository.setEntityManager(SharedEntityManagerCreator.createSharedEntityManager(jpa.getObject()));
        repository.setBeanFactory(beans);repository.afterPropertiesSet();
        beans.registerSingleton("perexGroupDocRepository",repository.getObject());
        spring.refresh();
        previousSpring=Constants.getServletContext().getAttribute("springContext");
        Constants.getServletContext().setAttribute("springContext",spring);
        previousFactories=ReflectionTestUtils.getField(DBPool.class,"entityManagerFactories");
        // BaseWebjetTest omits servlet startup; use the real test JPA unit for both cache lookups.
        ReflectionTestUtils.setField(DBPool.class,"entityManagerFactories",Map.of("iwcm",jpa.getObject()));
    }

    @AfterAll static void closePersistence() {
        Constants.getServletContext().setAttribute("springContext",previousSpring);
        ReflectionTestUtils.setField(DBPool.class,"entityManagerFactories",previousFactories);
        if(spring!=null)spring.close();
        if(jpa!=null)jpa.destroy();
    }

    @BeforeEach void setup()throws Exception {
        try(Connection c=DBPool.getConnection()) {assertEquals("webjet_p01",c.getCatalog());assertTrue(c.getMetaData().getURL().contains(":33316/"));}
        for(String key:List.of("branchesEnabled","branchSingleWriterConfirmed","branchStorageRoot","multiDomainEnabled","fulltextExecuteApps","fulltextDataAscMethod","perexGroupUseJoin","editorDisableHistory","iwfs_useVersioning"))original.put(key,Constants.getString(key));
        Constants.setBoolean("branchesEnabled",true);Constants.setBoolean("branchSingleWriterConfirmed",true);Constants.setString("branchStorageRoot","/tmp/webjet-p03-private");Constants.setBoolean("multiDomainEnabled",true);Constants.setBoolean("fulltextExecuteApps",false);Constants.setString("fulltextDataAscMethod","");Constants.setBoolean("perexGroupUseJoin",true);
        Files.createDirectories(Path.of("/tmp/webjet-p03-private"));sk.iway.iwcm.system.Modules.getInstance(null,true);user=new Identity(UsersDB.getUser("tester-p05"));
        assertTrue(user.isAdmin(),"fresh admin");assertTrue(user.isAuthorized(),"fresh authorized");assertTrue(user.isEnabledItem("menuBranches"),"menuBranches enabled");
        RequestBean bean=new RequestBean();bean.setUser(user);bean.setDomain(DOMAIN);SetCharacterEncodingFilter.setCurrentRequestBean(bean);
        f=create("test-"+UUID.randomUUID().toString().substring(0,8),true);
    }
    @AfterEach void cleanup()throws Exception {
        if(f!=null)try(Connection c=DBPool.getConnection();Statement s=c.createStatement()) {
            try(ResultSet histories=s.executeQuery("SELECT history_path,file_history_id FROM file_history WHERE file_url LIKE '"+f.file+"%'")) {
                while(histories.next())Files.deleteIfExists(Path.of(Tools.getRealPath(histories.getString(1)+histories.getInt(2))));
            }
            s.executeUpdate("DELETE FROM file_history WHERE file_url LIKE '"+f.file+"%'");
            String docs=String.join(",",f.pages.stream().map(String::valueOf).toList())+","+f.home;
            for(String table:List.of("branch_documents","branch_groups","branch_media","branch_files"))s.executeUpdate("DELETE FROM "+table+" WHERE branch_id="+f.branch);
            s.executeUpdate("DELETE FROM branches WHERE id="+f.branch);s.executeUpdate("DELETE FROM news_templates WHERE name='p06-autotest-"+f.url.substring("/p06-autotest-".length())+"'");
            s.executeUpdate("DELETE FROM media_group_to_media WHERE media_id IN ("+f.media+","+f.newMedia+")");s.executeUpdate("DELETE FROM media WHERE media_id IN ("+f.media+","+f.newMedia+")");s.executeUpdate("DELETE FROM media_groups WHERE media_group_id="+f.category);s.executeUpdate("DELETE FROM perex_groups WHERE perex_group_id="+f.perex);
            s.executeUpdate("DELETE FROM perex_group_doc WHERE doc_id IN ("+docs+")");s.executeUpdate("DELETE FROM documents_history WHERE doc_id IN ("+docs+")");s.executeUpdate("DELETE FROM documents WHERE doc_id IN ("+docs+")");
            s.executeUpdate("DELETE FROM groups_scheduler WHERE group_id IN ("+f.root+","+f.section+","+f.destination+")");s.executeUpdate("DELETE FROM `groups` WHERE group_id IN ("+f.root+","+f.section+","+f.destination+")");
        }
        original.forEach(Constants::setString);SetCharacterEncodingFilter.setCurrentRequestBean(null);GroupsDB.getInstance(true);DocDB.getInstance(true);sk.iway.iwcm.system.Modules.getInstance(null,true);
    }

    @Test void twentyPagePackagePublishesStableIdsAuthorDatesMediaAndMovedFolder()throws Exception {
        var report=ready();assertEquals(20,report.items().stream().filter(i->i.type().equals("DOCUMENT")).count());
        assertTrue(report.items().stream().anyMatch(i->i.type().equals("FILE")));
        String oldOwner=live(BranchType.DOCUMENT,f.home).toString();
        merge(report,new BranchPublishService());assertEquals("MERGED",detail().state(),detail().lastError());
        assertEquals(oldOwner,live(BranchType.DOCUMENT,f.home).toString(),"Media-only update must not SAVE its older owner");
        assertEquals(f.destination,((Number)live(BranchType.GROUP,f.section).get("parent_group_id")).intValue());
        for(int id:f.pages) {var row=live(BranchType.DOCUMENT,id);assertEquals(1,((Number)row.get("author_id")).intValue());assertEquals(Instant.parse("2020-01-01T12:00:00Z").toEpochMilli(),((java.util.Date)row.get("date_created")).getTime());assertTrue(row.get("title").toString().contains("WORK"));}
        assertEquals(20,count("SELECT COUNT(*) FROM perex_group_doc WHERE perex_group_id="+f.perex));
        assertEquals("autotest P06 WORK media",live(BranchType.MEDIA,f.media).get("media_title_sk"));
        assertEquals(f.pages.get(19).intValue(),((Number)live(BranchType.MEDIA,f.newMedia).get("media_fk_id")).intValue());assertEquals(List.of(f.category),BranchFields.normalizedIds(live(BranchType.MEDIA,f.newMedia).get("group_ids_snapshot")));
        assertEquals("autotest P06 WORK attachment",Files.readString(Path.of(Tools.getRealPath(f.file))));
        long histories=count("SELECT COUNT(*) FROM documents_history WHERE doc_id IN ("+String.join(",",f.pages.stream().map(String::valueOf).toList())+")");
        new BranchPublishService().run(f.branch,user.getUserId());assertEquals(histories,count("SELECT COUNT(*) FROM documents_history WHERE doc_id IN ("+String.join(",",f.pages.stream().map(String::valueOf).toList())+")"));
    }

    @Test void failureMidwayResumesWithoutRepeatingWritesAndRejectsNewerProduction()throws Exception {
        AtomicInteger completed=new AtomicInteger();
        merge(ready(),new BranchPublishService(key->{if(key.startsWith("DOCUMENT:")&&completed.incrementAndGet()==10)throw new IllegalStateException("autotest P06 injected halfway");}));
        assertEquals("MERGE_FAILED",detail().state());assertTrue(detail().items().stream().anyMatch(i->i.applyState().equals("DONE")));
        int first=f.pages.get(0);String firstHash=snapshot(BranchType.DOCUMENT,first).hash();long history=count("SELECT COUNT(*) FROM documents_history WHERE doc_id="+first);
        sql("UPDATE documents SET title='autotest P06 newer production' WHERE doc_id="+first);
        var stale=detail();assertFalse(stale.ready());assertThrows(ResponseStatusException.class,()->service.claim(f.branch,DOMAIN,user,stale.revision(),stale.preparationHash(),true));
        assertEquals("autotest P06 newer production",live(BranchType.DOCUMENT,first).get("title"));
        sql("UPDATE documents SET title='autotest P06 WORK 0' WHERE doc_id="+first);assertEquals(firstHash,snapshot(BranchType.DOCUMENT,first).hash());
        var retry=ready();service.claim(f.branch,DOMAIN,user,retry.revision(),retry.preparationHash(),true);new BranchPublishService().run(f.branch,user.getUserId());
        assertEquals("MERGED",detail().state(),detail().lastError());assertEquals(history,count("SELECT COUNT(*) FROM documents_history WHERE doc_id="+first));
    }

    @Test void standardAuditSeesCommittedDataAndIsNotRepeatedOnResume()throws Exception {
        List<String> audited=new ArrayList<>();
        try(var audit=mockStatic(Adminlog.class)) {
            audit.when(()->Adminlog.add(anyInt(),anyInt(),anyString(),anyInt(),anyInt(),any(Timestamp.class))).thenAnswer(call->{
                assertEquals(user.getUserId(),((Number)call.getArgument(1)).intValue());
                assertEquals(f.branch,((Number)call.getArgument(4)).longValue());
                String description=call.getArgument(2);
                String identity=description.substring(description.indexOf(": ")+2);
                int separator=identity.indexOf(':');
                BranchType type=BranchType.valueOf(identity.substring(0,separator));
                String key=identity.substring(separator+1);
                try(Connection connection=DBPool.getConnection()) {
                    var committed=BranchStore.readWork(connection,f.branch,type,key).orElseThrow();
                    assertEquals("APPLYING",committed.get("apply_state"));
                    assertNotNull(committed.get("applied_hash"),"Audit must follow the committed data checkpoint");
                    assertEquals(committed.get("applied_hash"),BranchPublishRows.liveSnapshot(connection,type,committed).hash());
                }
                audited.add(identity);
                return null;
            });
            AtomicInteger completed=new AtomicInteger();
            merge(ready(),new BranchPublishService(key->{if(key.startsWith("DOCUMENT:")&&completed.incrementAndGet()==10)throw new IllegalStateException("autotest audit after commit");}));
            assertEquals("MERGE_FAILED",detail().state());
            String first="DOCUMENT:"+f.pages.get(0);
            assertEquals(1,Collections.frequency(audited,first));
            var retry=ready();service.claim(f.branch,DOMAIN,user,retry.revision(),retry.preparationHash(),true);
            new BranchPublishService().run(f.branch,user.getUserId());
            assertEquals("MERGED",detail().state(),detail().lastError());
            assertEquals(25,audited.size());assertEquals(25,new HashSet<>(audited).size());
        }
    }

    @Test void failedCheckpointRollsBackContentWithoutCallingAudit()throws Exception {
        int first=f.pages.get(0);String before=snapshot(BranchType.DOCUMENT,first).hash();
        sql("CREATE TRIGGER p06_autotest_checkpoint_failure BEFORE UPDATE ON branch_documents FOR EACH ROW BEGIN IF NEW.branch_id="+f.branch+" AND NEW.doc_id="+first+" AND NEW.apply_phase='DATA' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='autotest checkpoint rollback'; END IF; END");
        try(var audit=mockStatic(Adminlog.class)) {
            merge(ready(),new BranchPublishService());
            assertEquals("MERGE_FAILED",detail().state());
            assertEquals(before,snapshot(BranchType.DOCUMENT,first).hash());
            assertEquals(0,count("SELECT COUNT(*) FROM documents_history WHERE doc_id="+first));
            audit.verify(()->Adminlog.add(eq(Adminlog.TYPE_PAGE_UPDATE),eq(user.getUserId()),
                eq("Publishing package #"+f.branch+": DOCUMENT:"+first),eq(first),eq((int)f.branch),any(Timestamp.class)),never());
        } finally {sql("DROP TRIGGER IF EXISTS p06_autotest_checkpoint_failure");}
    }

    @Test void fileReplacementBeforeCheckpointIsRecognizedOnResume()throws Exception {
        Constants.setBoolean("iwfs_useVersioning",true);
        String originalBytes=Files.readString(Path.of(Tools.getRealPath(f.file)));
        merge(ready(),new BranchPublishService(key->{if(key.startsWith("FILE_REPLACED:"))throw new IllegalStateException("autotest P06 file checkpoint crash");}));
        assertEquals("MERGE_FAILED",detail().state());assertEquals("autotest P06 WORK attachment",Files.readString(Path.of(Tools.getRealPath(f.file))));
        assertEquals(1,count("SELECT COUNT(*) FROM file_history WHERE file_url='"+f.file+"'"));
        assertEquals(user.getUserId(),((Number)query("SELECT user_id FROM file_history WHERE file_url='"+f.file+"'")).intValue());
        assertEquals(1,((Number)query("SELECT domain_id FROM file_history WHERE file_url='"+f.file+"'")).intValue());
        assertEquals(originalBytes,Files.readString(Path.of(Tools.getRealPath(query("SELECT CONCAT(history_path,file_history_id) FROM file_history WHERE file_url='"+f.file+"'").toString()))));
        long modified=Files.getLastModifiedTime(Path.of(Tools.getRealPath(f.file))).toMillis();
        var retry=ready();service.claim(f.branch,DOMAIN,user,retry.revision(),retry.preparationHash(),true);new BranchPublishService().run(f.branch,user.getUserId());
        assertEquals("MERGED",detail().state(),detail().lastError());assertEquals(modified,Files.getLastModifiedTime(Path.of(Tools.getRealPath(f.file))).toMillis());
        assertEquals(1,count("SELECT COUNT(*) FROM file_history WHERE file_url='"+f.file+"'"));
    }

    @Test void choiceImmediatelyChangesPreviewAndStaleChoiceRequiresRecomparison()throws Exception {
        int id=f.pages.get(0);sql("UPDATE documents SET title='autotest P06 concurrent LIVE' WHERE doc_id="+id);
        var report=detail();var item=item(report,BranchType.DOCUMENT,Integer.toString(id));assertTrue(item.conflict());
        report=service.resolve(f.branch,DOMAIN,user,report.revision(),BranchType.DOCUMENT,Integer.toString(id),BranchDecision.Source.LIVE,item.liveHash(),item.workHash());
        try(var scope=BranchContexts.open(null,new BranchContext(f.branch,DOMAIN,1,BranchContext.Mode.PREVIEW))){assertEquals("autotest P06 concurrent LIVE",BranchView.current().document(id,false).getTitle());}
        sql("UPDATE documents SET title='autotest P06 newer LIVE' WHERE doc_id="+id);
        assertFalse(detail().ready());
        try(var scope=BranchContexts.open(null,new BranchContext(f.branch,DOMAIN,1,BranchContext.Mode.PREVIEW))){assertThrows(BranchDecision.StaleChoiceException.class,BranchView::current);}
        var stale=detail();var now=item(stale,BranchType.DOCUMENT,Integer.toString(id));assertThrows(ResponseStatusException.class,()->service.resolve(f.branch,DOMAIN,user,stale.revision(),BranchType.DOCUMENT,Integer.toString(id),BranchDecision.Source.WORK,item.liveHash(),item.workHash()));
        assertTrue(service.resolve(f.branch,DOMAIN,user,stale.revision(),BranchType.DOCUMENT,Integer.toString(id),BranchDecision.Source.WORK,now.liveHash(),now.workHash()).ready());
    }

    @Test void preflightBlocksNewDescendantsUrlCollisionsApprovalAndOutsideFilePlan()throws Exception {
        int added;
        try(Connection c=DBPool.getConnection()){c.setAutoCommit(false);added=BranchStore.reserveNative(c,BranchType.DOCUMENT);var row=doc(added,f.section,"autotest new descendant","/p06-new-descendant-"+added+".html");BranchRowWriter.insertDocumentWithId(c,BranchContext.live(DOMAIN,1),row);c.commit();}
        assertFalse(detail().ready());sql("DELETE FROM documents WHERE doc_id="+added);
        sql("UPDATE documents SET virtual_path='"+f.url+"/moved/page-0.html' WHERE doc_id="+f.home);
        assertFalse(detail().ready());sql("UPDATE documents SET virtual_path='"+f.url+"/' WHERE doc_id="+f.home);
        sql("INSERT INTO groups_approve(group_id,user_id,approve_mode) VALUES ("+f.root+",(SELECT user_id FROM users WHERE login='admin'),0)");
        assertFalse(detail().ready());sql("DELETE FROM groups_approve WHERE group_id="+f.root);
        try(Connection c=DBPool.getConnection()){c.setAutoCommit(false);BranchRowWriter.insertDocumentWithId(c,BranchContext.live(DOMAIN,1),doc(added,f.root,"autotest outside planned page","/p06-outside-"+added+".html"));c.commit();}
        try(Connection c=DBPool.getConnection()){c.setAutoCommit(false);BranchPublishRows.groupHistory(c,live(BranchType.GROUP,f.section),user.getUserId());c.commit();}
        sql("UPDATE groups_scheduler SET awaiting_approve=',1,' WHERE group_id="+f.section);assertFalse(detail().ready());sql("DELETE FROM groups_scheduler WHERE group_id="+f.section);
        sql("UPDATE media_groups SET available_groups='2147483647' WHERE media_group_id="+f.category);assertFalse(detail().ready());sql("UPDATE media_groups SET available_groups='' WHERE media_group_id="+f.category);
        Path staged=Path.of(Tools.getRealPath(PlannedAssetPaths.ROOT+added+f.file));Files.createDirectories(staged.getParent());Files.writeString(staged,"autotest outside pending plan");
        try(Connection c=DBPool.getConnection()){c.setAutoCommit(false);BranchPublishRows.documentHistory(c,live(BranchType.DOCUMENT,added),false);c.commit();}
        sql("UPDATE documents_history SET publicable=true WHERE doc_id="+added);
        var blocked=detail();assertFalse(blocked.ready());assertTrue(blocked.errors().stream().anyMatch(s->s.contains("individuálny plán")),blocked.errors().toString());
        sql("UPDATE documents_history SET publicable=false,awaiting_approve=',1,' WHERE doc_id="+added);assertFalse(detail().ready());
        Files.deleteIfExists(staged);sql("DELETE FROM documents_history WHERE doc_id="+added);sql("DELETE FROM documents WHERE doc_id="+added);
        assertTrue(detail().ready(),detail().errors().toString());
    }

    @Test void subtreeTrashKeepsNestingAndMandatorySnapshotsWhenHistoryDisabled()throws Exception {
        Constants.setBoolean("editorDisableHistory",true);
        // Replace this fixture's WORK edits with the normal branch editor's explicit subtree DELETE set.
        for(String table:List.of("branch_documents","branch_groups","branch_media","branch_files"))sql("DELETE FROM "+table+" WHERE branch_id="+f.branch);
        int nested;
        try(Connection c=DBPool.getConnection()){c.setAutoCommit(false);nested=BranchStore.reserveNative(c,BranchType.GROUP);var g=new LinkedHashMap<>(BranchGroupMapper.initialValues(nested,DOMAIN));g.put("group_name","autotest nested");g.put("parent_group_id",f.section);BranchRowWriter.insertGroupWithId(c,BranchContext.live(DOMAIN,1),g);c.commit();}
        int page=f.pages.get(0);sql("UPDATE documents SET group_id="+nested+" WHERE doc_id="+page);
        BranchContext context=admin();
        for(int group:List.of(f.section,nested)) {var row=live(BranchType.GROUP,group);store.put(context,BranchType.GROUP,group,row,BranchStore.snapshot(BranchType.GROUP,row).hash(),true);}
        for(int id:f.pages.subList(0,18)) {var row=live(BranchType.DOCUMENT,id);store.put(context,BranchType.DOCUMENT,id,row,BranchStore.snapshot(BranchType.DOCUMENT,row).hash(),true);}
        merge(ready(),new BranchPublishService(key->{if(key.startsWith("DOCUMENT:"))throw new IllegalStateException("autotest stop before root");}));
        assertEquals("MERGE_FAILED",detail().state());assertEquals(f.root,live(BranchType.GROUP,f.section).get("parent_group_id"));
        var retry=ready();service.claim(f.branch,DOMAIN,user,retry.revision(),retry.preparationHash(),true);
        new BranchPublishService(key->{if(key.startsWith("BEFORE_TRASH_ROOT:"))try {sql("UPDATE documents SET title='autotest concurrent before root' WHERE doc_id="+page);}catch(SQLException ex){throw new IllegalStateException(ex);}}).run(f.branch,user.getUserId());
        assertEquals("MERGE_FAILED",detail().state());assertEquals(f.root,live(BranchType.GROUP,f.section).get("parent_group_id"));assertEquals("autotest concurrent before root",live(BranchType.DOCUMENT,page).get("title"));
        sql("UPDATE documents SET title='autotest P06 LIVE 0' WHERE doc_id="+page);
        retry=ready();service.claim(f.branch,DOMAIN,user,retry.revision(),retry.preparationHash(),true);new BranchPublishService().run(f.branch,user.getUserId());
        assertEquals("MERGED",detail().state(),detail().lastError());assertEquals(nested,live(BranchType.DOCUMENT,page).get("group_id"));assertEquals(f.section,live(BranchType.GROUP,nested).get("parent_group_id"));
        assertTrue(count("SELECT COUNT(*) FROM documents_history WHERE doc_id="+page+" AND actual=true AND available=true")>0);
        assertEquals(f.root,((Number)query("SELECT parent_group_id FROM groups_scheduler WHERE group_id="+f.section+" AND when_to_publish IS NULL ORDER BY save_date DESC LIMIT 1")).intValue());
        if("true".equals(System.getenv("WEBJET_P06_TRASH_FIXTURE"))) {
            Files.writeString(Path.of("/tmp/webjet-p06-trash.properties"),"branchId="+f.branch+"\nroot="+f.root+"\nsection="+f.section+"\nnested="+nested+"\npage="+page+"\nurl="+f.url+"/\n");f=null;
        } else sql("DELETE FROM `groups` WHERE group_id="+nested);
    }

    @Test void unchangedWorkKeepsNewLiveContentWithoutHistoryOrContentWrite()throws Exception {
        int id=f.pages.get(0);sql("DELETE FROM branch_groups WHERE branch_id="+f.branch);
        sql("UPDATE branch_documents SET title='autotest P06 LIVE 0',navbar='autotest P06 LIVE 0',virtual_path='"+f.url+"/section/page-0.html',perex_group='' WHERE branch_id="+f.branch+" AND doc_id="+id);
        sql("UPDATE documents SET title='autotest P06 new LIVE preserved' WHERE doc_id="+id);
        var report=ready();assertFalse(item(report,BranchType.DOCUMENT,Integer.toString(id)).writeRequired());
        merge(report,new BranchPublishService());assertEquals("MERGED",detail().state(),detail().lastError());
        assertEquals("autotest P06 new LIVE preserved",live(BranchType.DOCUMENT,id).get("title"));assertEquals(0,count("SELECT COUNT(*) FROM documents_history WHERE doc_id="+id));
        assertEquals("NOOP",item(detail(),BranchType.DOCUMENT,Integer.toString(id)).applyPhase());
    }

    @Test void liveChoiceKeepsProductionContentAndRecomputesMovedTreeMetadata()throws Exception {
        int id=f.pages.get(0);sql("UPDATE documents SET title='autotest P06 chosen LIVE' WHERE doc_id="+id);
        var report=detail();var item=item(report,BranchType.DOCUMENT,Integer.toString(id));
        var selected=service.resolve(f.branch,DOMAIN,user,report.revision(),BranchType.DOCUMENT,Integer.toString(id),BranchDecision.Source.LIVE,item.liveHash(),item.workHash());
        assertTrue(selected.ready(),selected.errors().toString());assertTrue(item(selected,BranchType.DOCUMENT,Integer.toString(id)).writeRequired());
        merge(selected,new BranchPublishService());assertEquals("MERGED",detail().state(),detail().lastError());
        var live=live(BranchType.DOCUMENT,id);assertEquals("autotest P06 chosen LIVE",live.get("title"));assertEquals(List.of(),BranchFields.normalizedIds(live.get("perex_group")));
        assertEquals(f.destination,((Number)live.get("root_group_l2")).intValue());assertEquals(f.section,((Number)live.get("root_group_l3")).intValue());
        assertEquals(19,count("SELECT COUNT(*) FROM perex_group_doc WHERE perex_group_id="+f.perex));
    }

    @Test void fileConflictsCoverChangedNewAndDeletedPathsAsWholeItems()throws Exception {
        Constants.setBoolean("iwfs_useVersioning",true);
        String fresh=f.file+".new.txt",deleted=f.file+".delete.txt";
        Path freshLive=Path.of(Tools.getRealPath(fresh)),deletedLive=Path.of(Tools.getRealPath(deleted));
        Path incoming=Files.createTempFile("p06-file-conflict-",".txt");Files.writeString(incoming,"autotest new WORK bytes");
        var created=store.putFile(admin(),fresh,"FILE",BranchSnapshot.absent(),BranchFiles.hash(incoming),Files.size(incoming),"text/plain",false);
        Path work=BranchFiles.workPath(f.branch,((Number)created.get("row_id")).longValue());Files.createDirectories(work.getParent());Files.copy(incoming,work);
        Files.writeString(freshLive,"autotest concurrent new LIVE");Files.writeString(deletedLive,"autotest DELETE BASE");
        var removed=store.putFile(admin(),deleted,"FILE",BranchFiles.liveSnapshot(deleted),null,null,null,true);
        Path removedBytes=BranchFiles.workPath(f.branch,((Number)removed.get("row_id")).longValue());Files.createDirectories(removedBytes.getParent());Files.copy(deletedLive,removedBytes.resolveSibling("base"));
        Files.writeString(deletedLive,"autotest concurrent DELETE LIVE");Files.writeString(Path.of(Tools.getRealPath(f.file)),"autotest concurrent changed LIVE");
        try {
            for(String path:List.of(f.file,fresh,deleted)) {
                var report=detail();var item=item(report,BranchType.FILE,path);assertTrue(item.conflict());
                service.resolve(f.branch,DOMAIN,user,report.revision(),BranchType.FILE,path,path.equals(f.file)?BranchDecision.Source.LIVE:BranchDecision.Source.WORK,item.liveHash(),item.workHash());
            }
            try(var scope=BranchContexts.open(null,new BranchContext(f.branch,DOMAIN,user.getUserId(),BranchContext.Mode.PREVIEW))) {
                assertEquals("autotest concurrent changed LIVE",Files.readString(BranchFiles.read(f.file)));assertEquals("autotest new WORK bytes",Files.readString(BranchFiles.read(fresh)));assertNull(BranchFiles.read(deleted));
            }
            merge(ready(),new BranchPublishService());assertEquals("MERGED",detail().state(),detail().lastError());
            assertEquals("autotest concurrent changed LIVE",Files.readString(Path.of(Tools.getRealPath(f.file))));assertEquals("autotest new WORK bytes",Files.readString(freshLive));assertFalse(Files.exists(deletedLive));
            assertEquals(0,count("SELECT COUNT(*) FROM file_history WHERE file_url='"+f.file+"'"));
            assertEquals(1,count("SELECT COUNT(*) FROM file_history WHERE file_url='"+fresh+"'"));
            assertEquals(1,count("SELECT COUNT(*) FROM file_history WHERE file_url='"+deleted+"' AND deleted=false"));
            assertEquals("autotest concurrent DELETE LIVE",Files.readString(Path.of(Tools.getRealPath(query("SELECT CONCAT(history_path,file_history_id) FROM file_history WHERE file_url='"+deleted+"'").toString()))));
        } finally {Files.deleteIfExists(incoming);Files.deleteIfExists(freshLive);Files.deleteIfExists(deletedLive);}
    }

    @Test void differentDomainMayUseTheSameUrlDuringFrozenPublication()throws Exception {
        int foreign;
        try(Connection c=DBPool.getConnection()){c.setAutoCommit(false);foreign=BranchStore.reserveNative(c,BranchType.GROUP);var g=new LinkedHashMap<>(BranchGroupMapper.initialValues(foreign,"p06-foreign.example"));g.put("group_name","autotest foreign P06");g.put("url_dir_name","foreign-p06");BranchRowWriter.insertGroupWithId(c,BranchContext.live("p06-foreign.example",1),g);c.commit();}
        int page;
        try(Connection c=DBPool.getConnection()){c.setAutoCommit(false);page=BranchStore.reserveNative(c,BranchType.DOCUMENT);BranchRowWriter.insertDocumentWithId(c,BranchContext.live("p06-foreign.example",1),doc(page,foreign,"autotest foreign same URL",f.url+"/moved/page-0.html"));c.commit();}
        try {merge(ready(),new BranchPublishService());assertEquals("MERGED",detail().state(),detail().lastError());}
        finally {sql("DELETE FROM documents WHERE doc_id="+page);sql("DELETE FROM `groups` WHERE group_id="+foreign);}
    }

    @Test void reservedIdentityCannotBeResolvedOverForeignRowAfterPartialPublication()throws Exception {
        merge(ready(),new BranchPublishService(key->{if(key.startsWith("FILE_REPLACED:"))throw new IllegalStateException("autotest stop before new page");}));
        int id=f.pages.get(19);
        try(Connection c=DBPool.getConnection()){c.setAutoCommit(false);var work=BranchStore.readWork(c,f.branch,BranchType.DOCUMENT,id).orElseThrow();BranchRowWriter.insertDocumentWithId(c,BranchContext.live(DOMAIN,1),work);c.commit();}
        var report=detail();assertFalse(report.ready());var item=item(report,BranchType.DOCUMENT,Integer.toString(id));assertTrue(item.conflict());
        assertThrows(ResponseStatusException.class,()->service.resolve(f.branch,DOMAIN,user,report.revision(),BranchType.DOCUMENT,Integer.toString(id),BranchDecision.Source.WORK,item.liveHash(),item.workHash()));
    }

    @Test void finalEffectsFailureResumesWithoutAnyNewContentHistory()throws Exception {
        merge(ready(),new BranchPublishService(key->{if(key.equals("FINAL_EFFECTS"))throw new IllegalStateException("autotest final effects crash");}));
        var failed=detail();assertEquals("MERGE_FAILED",failed.state());assertTrue(failed.items().stream().allMatch(x->"DONE".equals(x.applyState())));
        long history=count("SELECT COUNT(*) FROM documents_history WHERE doc_id="+f.pages.get(0));
        service.claim(f.branch,DOMAIN,user,failed.revision(),failed.preparationHash(),true);new BranchPublishService().run(f.branch,user.getUserId());
        assertEquals("MERGED",detail().state(),detail().lastError());assertEquals(history,count("SELECT COUNT(*) FROM documents_history WHERE doc_id="+f.pages.get(0)));
    }

    @Test void categoryPreflightAndExistingPerexApiPreserveOtherPages()throws Exception {
        sql("DELETE FROM perex_groups WHERE perex_group_id="+f.perex);assertFalse(detail().ready());
        sql("INSERT INTO perex_groups(perex_group_id,perex_group_name,available_groups,domain_id) VALUES ("+f.perex+",'autotest P06 restored category','',1)");
        PerexGroupsService.savePerexGroupsDoc(f.home,new int[]{f.perex});
        long unrelated=((Number)query("SELECT id FROM perex_group_doc WHERE doc_id="+f.home)).longValue();
        merge(ready(),new BranchPublishService());assertEquals("MERGED",detail().state(),detail().lastError());
        assertEquals(21,count("SELECT COUNT(*) FROM perex_group_doc WHERE perex_group_id="+f.perex));
        assertEquals(unrelated,((Number)query("SELECT id FROM perex_group_doc WHERE doc_id="+f.home)).longValue());
        int first=f.pages.get(0);
        long membership=((Number)query("SELECT id FROM perex_group_doc WHERE doc_id="+first)).longValue();
        PerexGroupsService.savePerexGroupsDoc(first,new int[]{f.perex});
        assertEquals(membership,((Number)query("SELECT id FROM perex_group_doc WHERE doc_id="+first)).longValue());
        assertEquals("autotest P06 restored category",query("SELECT perex_group_name FROM perex_groups WHERE perex_group_id="+f.perex));
    }

    @Test @EnabledIfEnvironmentVariable(named="WEBJET_P06_DEMO",matches="true") void prepareRetainedDemo()throws Exception {
        Path manifest=Path.of("/tmp/webjet-p06-manual.properties");assertFalse(Files.exists(manifest),"Reuse retained P06 demo; do not regenerate");
        sql("UPDATE documents SET title='autotest P06 concurrent LIVE title' WHERE doc_id="+f.pages.get(0));
        var report=detail();assertTrue(report.items().stream().anyMatch(BranchMergeService.Item::conflict));
        String data="branchId="+f.branch+"\ngroupId="+f.root+"\nsectionId="+f.section+"\nhomeId="+f.home+"\npageId="+f.pages.get(0)+"\nmediaId="+f.media+"\nnewMediaId="+f.newMedia+"\nfile="+f.file+"\nurl="+f.url+"/\n";
        Files.writeString(manifest,data);Files.createDirectories(Path.of("build/test-results/p06"));Files.writeString(Path.of("build/test-results/p06/demo.properties"),data);f=null;
    }

    @Test @EnabledIfEnvironmentVariable(named="WEBJET_P06_BROWSER_FIXTURE",matches="true") void prepareBrowserPublication()throws Exception {
        Path manifest=Path.of("/tmp/webjet-p06-publish.properties");assertFalse(Files.exists(manifest));
        sql("UPDATE documents SET title='autotest P06 concurrent LIVE title' WHERE doc_id="+f.pages.get(0));
        sql("UPDATE documents SET title='autotest P06 retained LIVE choice' WHERE doc_id="+f.pages.get(1));
        Files.writeString(manifest,"branchId="+f.branch+"\ngroupId="+f.root+"\nsectionId="+f.section+"\nhomeId="+f.home+"\npageId="+f.pages.get(0)+"\nlivePageId="+f.pages.get(1)+"\nmediaId="+f.media+"\nnewMediaId="+f.newMedia+"\nfile="+f.file+"\nurl="+f.url+"/\n");f=null;
    }

    @Test @EnabledIfEnvironmentVariable(named="WEBJET_P06_PARTIAL_FIXTURE",matches="true") void prepareRetainedPartialPublication()throws Exception {
        Path manifest=Path.of("/tmp/webjet-p06-partial.properties");assertFalse(Files.exists(manifest));
        AtomicInteger completed=new AtomicInteger();
        merge(ready(),new BranchPublishService(key->{if(key.startsWith("DOCUMENT:")&&completed.incrementAndGet()==10)throw new IllegalStateException("autotest P06 retained halfway for process restart");}));
        var progress=detail();assertEquals("MERGE_FAILED",progress.state());
        Files.writeString(manifest,"branchId="+f.branch+"\ngroupId="+f.root+"\nhomeId="+f.home+"\npageId="+f.pages.get(0)+"\nmediaId="+f.media+"\nnewMediaId="+f.newMedia+"\nfile="+f.file+"\nurl="+f.url+"/\nfirstHistoryCount="+count("SELECT COUNT(*) FROM documents_history WHERE doc_id="+f.pages.get(0))+"\n");f=null;
    }

    record Fixture(long branch,int root,int section,int destination,int home,List<Integer> pages,int media,int newMedia,int category,int perex,String file,String url) { }
    Fixture create(String suffix,boolean work)throws Exception {
        String url="/p06-autotest-"+suffix;int root,section,home;List<Integer> pages=new ArrayList<>();int category=Math.toIntExact(PkeyGenerator.getNextValueAsLongStrict("media_groups")),media=Math.toIntExact(PkeyGenerator.getNextValueAsLongStrict("media"));
        int template,perex;
        try(Connection c=DBPool.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT temp_id FROM templates WHERE forward='/templates/aceintegration/jet/blank.jsp' LIMIT 1")){assertTrue(r.next());template=r.getInt(1);}
        try(Connection c=DBPool.getConnection()) {
            c.setAutoCommit(false);
            try(PreparedStatement s=c.prepareStatement("INSERT INTO perex_groups(perex_group_name,available_groups,domain_id) VALUES (?,'',1)",Statement.RETURN_GENERATED_KEYS)){s.setString(1,"autotest P06 "+suffix);s.executeUpdate();try(ResultSet r=s.getGeneratedKeys()){r.next();perex=r.getInt(1);}}
            root=BranchStore.reserveNative(c,BranchType.GROUP);section=BranchStore.reserveNative(c,BranchType.GROUP);
            for(int id:List.of(root,section)){var g=new LinkedHashMap<>(BranchGroupMapper.initialValues(id,DOMAIN));g.put("group_name",id==root?"autotest P06 "+suffix:"autotest P06 section");g.put("navbar",g.get("group_name"));g.put("url_dir_name",id==root?url.substring(1):"section");g.put("parent_group_id",id==root?0:root);g.put("temp_id",template);BranchRowWriter.insertGroupWithId(c,BranchContext.live(DOMAIN,1),g);}
            home=BranchStore.reserveNative(c,BranchType.DOCUMENT);var homepage=doc(home,root,"autotest P06 home",url+"/");homepage.put("temp_id",template);homepage.put("data","<h1>autotest P06</h1><div id='p06-menu'>!INCLUDE(/components/menu/menu_ul_li.jsp, rootGroupId="+root+", openAllItems=true, classes=full)!</div> !INCLUDE(/components/news/news-velocity.jsp, groupIds="+root+", alsoSubGroups=true, template=p06-autotest-"+suffix+", publishType=all, perexNotRequired=true, order=priority, ascending=true, pageSize=30, paging=false, includeActualDoc=true)!");BranchRowWriter.insertDocumentWithId(c,BranchContext.live(DOMAIN,1),homepage);
            for(int i=0;i<18;i++){int id=BranchStore.reserveNative(c,BranchType.DOCUMENT);var d=doc(id,section,"autotest P06 LIVE "+i,url+"/section/page-"+i+".html");d.put("sort_priority",i);d.put("temp_id",template);BranchRowWriter.insertDocumentWithId(c,BranchContext.live(DOMAIN,1),d);pages.add(id);}
            try(PreparedStatement s=c.prepareStatement("UPDATE `groups` SET default_doc_id=? WHERE group_id=?")){s.setInt(1,home);s.setInt(2,root);s.executeUpdate();s.setInt(1,pages.get(0));s.setInt(2,section);s.executeUpdate();}
            try(PreparedStatement s=c.prepareStatement("INSERT INTO media_groups(media_group_id,media_group_name,available_groups) VALUES (?,?,'')")){s.setInt(1,category);s.setString(2,"autotest P06 "+suffix);s.executeUpdate();}
            var m=new LinkedHashMap<>(BranchMediaMapper.initialValues(media,home,1));m.put("media_title_sk","autotest P06 LIVE media");m.put("media_link","/files/branch-p06-autotest/"+suffix+".txt");m.put("last_update",Timestamp.from(Instant.parse("2020-01-01T11:00:00Z")));BranchRowWriter.insertMediaWithId(c,BranchContext.live(DOMAIN,1),m,List.of(category));
            try(PreparedStatement s=c.prepareStatement("INSERT INTO news_templates(name,template_html,paging_html,paging_position,engine,domain_id) VALUES (?,?,'',1,'velocity',1)")){
                s.setString(1,"p06-autotest-"+suffix);s.setString(2,"<ol class='p06-news'>#foreach($item in $news)<li data-id='$item.docId' data-author='$item.authorId' data-date='$item.dateCreated'><a href='$actionBean.link($item)'>$item.title</a><span>$item.htmlData</span>#foreach($medium in $MediaDB.getMedia($item, ''))<a class='p06-media' data-media='$medium.mediaId' href='$medium.mediaLink'>$medium.mediaTitleSk</a>#end</li>#end</ol>");s.executeUpdate();
            }c.commit();
        }
        GroupsDB.getInstance(true);DocDB.getInstance(true);
        long branch=new BranchService().create(DOMAIN,"autotest P06 "+suffix,"20 pages, folder move, media and file; P06 only",1).getId();BranchContext context=new BranchContext(branch,DOMAIN,1,BranchContext.Mode.ADMIN);
        var destination=new LinkedHashMap<>(BranchGroupMapper.initialValues(1,DOMAIN));destination.put("group_name","autotest P06 destination");destination.put("url_dir_name","destination");destination.put("parent_group_id",root);destination.put("temp_id",template);
        int newGroup=((Number)store.create(context,BranchType.GROUP,destination).get("group_id")).intValue();
        var moved=live(BranchType.GROUP,section);String groupHash=BranchStore.snapshot(BranchType.GROUP,moved).hash();moved.put("parent_group_id",newGroup);moved.put("url_dir_name","moved");store.put(context,BranchType.GROUP,section,moved,groupHash,false);
        for(int i=0;i<18;i++){int id=pages.get(i);var d=live(BranchType.DOCUMENT,id);String hash=BranchStore.snapshot(BranchType.DOCUMENT,d).hash();d.put("perex_group",","+perex+",");d.put("title","autotest P06 WORK "+i);d.put("navbar","autotest P06 WORK "+i);d.put("virtual_path",url+"/moved/page-"+i+".html");store.put(context,BranchType.DOCUMENT,id,d,hash,false);}
        for(int i=18;i<20;i++){var d=doc(1,section,"autotest P06 WORK "+i,url+"/moved/page-"+i+".html");d.put("temp_id",template);d.put("perex_group",","+perex+",");d.put("sort_priority",i);pages.add(((Number)store.create(context,BranchType.DOCUMENT,d).get("doc_id")).intValue());}
        var m=live(BranchType.MEDIA,media);String mediaHash=BranchStore.snapshot(BranchType.MEDIA,m).hash();m.put("media_title_sk","autotest P06 WORK media");m.put("media_sort_order",0);store.put(context,BranchType.MEDIA,media,m,mediaHash,false);
        var nm=new LinkedHashMap<>(BranchMediaMapper.initialValues(1,pages.get(19),1));nm.put("media_title_sk","autotest P06 new media");nm.put("media_link","/files/branch-p06-autotest/"+suffix+".txt");nm.put("last_update",Timestamp.from(Instant.parse("2020-01-01T12:00:00Z")));nm.put("group_ids_snapshot",List.of(category));int newMedia=((Number)store.create(context,BranchType.MEDIA,nm).get("media_id")).intValue();
        String logical="/files/branch-p06-autotest/"+suffix+".txt";Path target=Path.of(Tools.getRealPath(logical));Files.createDirectories(target.getParent());Files.writeString(target,"autotest P06 BASE attachment");BranchSnapshot base=BranchFiles.liveSnapshot(logical);Path bytes=Files.createTempFile("p06-upload-",".txt");Files.writeString(bytes,"autotest P06 WORK attachment");var file=store.putFile(context,logical,"FILE",base,BranchFiles.hash(bytes),Files.size(bytes),"text/plain",false);Path stage=BranchFiles.workPath(branch,((Number)file.get("row_id")).longValue());Files.createDirectories(stage.getParent());Files.copy(bytes,stage,StandardCopyOption.REPLACE_EXISTING);Files.copy(target,stage.resolveSibling("base"),StandardCopyOption.REPLACE_EXISTING);Files.delete(bytes);
        return new Fixture(branch,root,section,newGroup,home,pages,media,newMedia,category,perex,logical,url);
    }
    Map<String,Object> doc(int id,int group,String title,String path){var d=new LinkedHashMap<>(BranchDocumentMapper.initialValues(id,1,Instant.parse("2020-01-01T12:00:00Z"),false));d.put("group_id",group);d.put("title",title);d.put("navbar",title);d.put("data","<h1>"+title+"</h1>");d.put("html_data","autotest perex "+title);d.put("virtual_path",path);d.put("available",true);d.put("show_in_menu",true);d.put("searchable",true);return d;}
    BranchContext admin(){return new BranchContext(f.branch,DOMAIN,1,BranchContext.Mode.ADMIN);}
    BranchMergeService.Report detail(){return service.detail(f.branch,DOMAIN,user);}
    BranchMergeService.Report ready(){var r=detail();assertTrue(r.ready(),r.errors()+" "+r.items().stream().filter(BranchMergeService.Item::conflict).toList());return r;}
    void merge(BranchMergeService.Report r,BranchPublishService publisher){service.claim(f.branch,DOMAIN,user,r.revision(),r.preparationHash(),false);publisher.run(f.branch,user.getUserId());}
    Map<String,Object> live(BranchType type,int id)throws Exception{try(Connection c=DBPool.getConnection()){return new LinkedHashMap<>(BranchStore.readLive(c,type,id).orElseThrow());}}
    BranchSnapshot snapshot(BranchType type,int id)throws Exception{try(Connection c=DBPool.getConnection()){return BranchPublishRows.liveSnapshot(c,type,Map.of(type.idColumn(),id));}}
    static BranchMergeService.Item item(BranchMergeService.Report r,BranchType t,String key){return r.items().stream().filter(i->i.type().equals(t.name())&&i.objectKey().equals(key)).findFirst().orElseThrow();}
    static void sql(String sql)throws SQLException{try(Connection c=DBPool.getConnection();Statement s=c.createStatement()){s.executeUpdate(sql);}}
    static Object query(String sql)throws SQLException{try(Connection c=DBPool.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){r.next();return r.getObject(1);}}
    static long count(String sql)throws SQLException{return ((Number)query(sql)).longValue();}
}
