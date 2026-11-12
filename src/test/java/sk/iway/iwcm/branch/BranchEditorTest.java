package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.security.MessageDigest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import sk.iway.iwcm.*;
import sk.iway.iwcm.doc.*;
import sk.iway.iwcm.editor.service.EditorService;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Exercises the actual authorized WORK editor and verifies production tables byte for byte. */
@EnabledIfEnvironmentVariable(named="WEBJET_P04_DB",matches="true")
class BranchEditorTest extends BaseWebjetTest {
    private static final String DOMAIN="p04-editor.example";
    private final Map<String,String> configuration=new LinkedHashMap<>();
    private BranchContext context;
    private int root,other,page;
    private final List<Integer> liveGroups=new ArrayList<>(),liveDocs=new ArrayList<>();
    private String production;

    @BeforeEach void fixture() throws Exception {
        try(Connection c=DBPool.getConnection()) { assertEquals("webjet_p01",c.getCatalog()); assertTrue(c.getMetaData().getURL().contains(":33316/")); }
        for(String key:List.of("enableStaticFilesExternalDir","branchesEnabled","branchSingleWriterConfirmed","branchStorageRoot","multiDomainEnabled","templatesUseRecursiveSystemFolder","fulltextExecuteApps","linkType")) configuration.put(key,Constants.getString(key));
        Constants.setBoolean("branchesEnabled",true); Constants.setBoolean("branchSingleWriterConfirmed",true);Constants.setBoolean("enableStaticFilesExternalDir",true);
        Files.createDirectories(Path.of("/tmp/webjet-p02-private")); Constants.setString("branchStorageRoot","/tmp/webjet-p02-private");
        Constants.setBoolean("multiDomainEnabled",true); Constants.setBoolean("templatesUseRecursiveSystemFolder",false); Constants.setBoolean("fulltextExecuteApps",true);
        Constants.setInt("linkType",Constants.LINK_TYPE_HTML);
        root=liveGroup("autotest P04 root",0,"autotest-p04"); other=liveGroup("autotest P04 other",root,"other");
        page=liveDoc(root,"/autotest-p04/existing.html");
        try(Connection c=DBPool.getConnection();Statement statement=c.createStatement()) { statement.executeUpdate("UPDATE `groups` SET default_doc_id="+page+" WHERE group_id="+root); }
        var branch=new BranchService().create(BranchTestDomains.ensure(DOMAIN),"autotest P04 editor","disposable integration test",1);
        context=new BranchContext(branch.getId(),DOMAIN,1,BranchContext.Mode.ADMIN);
        production=fingerprint();
    }

    @AfterEach void cleanup() throws Exception {
        try(Connection c=DBPool.getConnection();Statement s=c.createStatement()) {
            if(context!=null) for(String table:List.of("branch_files","branch_media","branch_documents","branch_groups","branches")) s.executeUpdate("DELETE FROM "+table+" WHERE "+(table.equals("branches")?"id":"branch_id")+"="+context.branchId());
            for(int id:liveDocs) s.executeUpdate("DELETE FROM documents WHERE doc_id="+id);
            for(int id:liveGroups) s.executeUpdate("DELETE FROM `groups` WHERE group_id="+id);
        } finally { configuration.forEach(Constants::setString); }
    }

    @Test void createEditMoveDeleteRevertAndIsolation() throws Exception {
        in((request,editor) -> {
            BranchEditor service=new BranchEditor(request);
            GroupDetails group=new GroupDetails(); group.setGroupId(-1); group.setGroupName("autotest P04 new"); group.setNavbar("autotest P04 new"); group.setUrlDirName("new"); group.setParentGroupId(root); group.setDomainName(DOMAIN);
            group=new GroupEditorField().fromGroupDetails(group); group.getEditorFields().setBranchHash("new");
            GroupDetails saved=service.saveGroup(group); int groupId=saved.getGroupId(); assertTrue(groupId>0);
            DocDetails fresh=service.loadDocument(-1,-1,groupId,editor); fresh.setUrlInheritGroup(true); fresh.setTitle("autotest P04 article"); fresh.setNavbar("autotest P04 article"); fresh.setAvailable(true); fresh.setHtmlData("autotest perex");
            fresh.setData("<section class=\"pb-section\"><div class=\"container\"><p>autotest WORK !INCLUDE(/autotest-never-execute.jsp)!</p></div></section>");
            DocDetails created=service.saveDocument(fresh,editor); int id=created.getDocId(); assertTrue(id>0); assertTrue(created.isAvailable()); assertEquals("autotest perex",created.getHtmlData()); assertTrue(created.getData().contains("pb-section"));
            assertEquals(id,BranchView.current().group(groupId).getDefaultDocId());
            created=service.loadDocument(id,-1,groupId,editor); created.setTitle("autotest renamed"); created.setNavbar("autotest renamed");
            created=service.saveDocument(created,editor); assertEquals(id,created.getDocId());
            GroupDetails renamed=new GroupEditorField().fromGroupDetails(BranchView.current().group(groupId)); BranchEditor.attachHash(renamed);
            renamed.setGroupName("autotest renamed folder"); renamed.setNavbar("autotest renamed folder"); renamed.setUrlDirName("renamed"); renamed.getEditorFields().setParentGroupDetails(BranchView.current().group(other));
            renamed=service.saveGroup(renamed); assertEquals(groupId,renamed.getGroupId());
            assertTrue(BranchView.current().document(id,false).getVirtualPath().contains("/other/renamed/"));
            // Removing a non-default page and undoing an existing LIVE change are independent operations.
            DocDetails edit=service.loadDocument(page,-1,root,editor); String initial=edit.getEditorFields().getBranchHash();
            edit.setTitle("autotest modified existing"); edit=service.saveDocument(edit,editor); assertNotEquals(initial,edit.getEditorFields().getBranchHash());
            service.deleteDocument(edit); assertNull(BranchView.current().document(page,false));
            service.revert(BranchType.DOCUMENT,page,hash(BranchType.DOCUMENT,page)); assertEquals("autotest LIVE",BranchView.current().document(page,false).getTitle());
            service.deleteGroup(renamed); assertNull(BranchView.current().group(groupId)); assertNull(BranchView.current().document(id,false));
            try(Connection c=DBPool.getConnection()) { assertTrue(BranchStore.readLive(c,BranchType.DOCUMENT,id).isEmpty()); assertTrue(BranchStore.readLive(c,BranchType.GROUP,groupId).isEmpty()); }
            return null;
        });
        assertEquals(production,fingerprint());
    }

    @Test void rejectsStaleContentCyclesUrlsDefaultsPlansAndRights() throws Exception {
        in((request,editor) -> {
            BranchEditor service=new BranchEditor(request);
            DocDetails first=service.loadDocument(page,-1,root,editor), stale=service.loadDocument(page,-1,root,editor);
            first.setTitle("autotest winner"); service.saveDocument(first,editor);
            stale.setTitle("autotest stale"); assertThrows(ResponseStatusException.class,()->service.saveDocument(stale,editor));
            DocDetails plan=service.loadDocument(page,-1,root,editor); plan.setPublishEndDate(new java.util.Date()); assertThrows(ResponseStatusException.class,()->service.saveDocument(plan,editor));
            DocDetails foreign=service.loadDocument(page,-1,root,editor); foreign.getEditorFields().getGroupDetails().setGroupId(Integer.MAX_VALUE); assertThrows(ResponseStatusException.class,()->service.saveDocument(foreign,editor));
            DocDetails collision=service.loadDocument(-1,-1,root,editor); collision.setGenerateUrlFromTitle(false); collision.setVirtualPath("/autotest-p04/existing.html"); assertThrows(ResponseStatusException.class,()->service.saveDocument(collision,editor));
            GroupDetails cycle=new GroupEditorField().fromGroupDetails(BranchView.current().group(root)); BranchEditor.attachHash(cycle); cycle.getEditorFields().setParentGroupDetails(BranchView.current().group(other)); assertThrows(ResponseStatusException.class,()->service.saveGroup(cycle));
            GroupDetails invalid=new GroupEditorField().fromGroupDetails(BranchView.current().group(other)); BranchEditor.attachHash(invalid); invalid.getEditorFields().setDefaultDocDetails(BranchView.current().document(page,true)); assertThrows(ResponseStatusException.class,()->service.saveGroup(invalid));
            Identity user=(Identity)request.getSession().getAttribute(Constants.USER_KEY); user.setEditableGroups(String.valueOf(other));
            assertThrows(ResponseStatusException.class,()->service.loadDocument(page,-1,root,editor));
            return null;
        });
        assertEquals(production,fingerprint());
    }

    @Test void deletingExistingSubtreeAndRevertingRestoresItsDefaultPage() throws Exception {
        in((request,editor) -> {
            BranchEditor service=new BranchEditor(request);
            // The tree delete button submits its displayed group, without opening the folder editor first.
            GroupDetails folder=BranchView.current().group(root);
            int domainId=context.domainId();
            assertNotNull(folder.getEditorFields().getBranchHash());
            service.deleteGroup(folder); assertNull(BranchView.current().group(root)); assertNull(BranchView.current().document(page,false));
            assertEquals(domainId,context.domainId());
            assertEquals(domainId,sk.iway.iwcm.common.CloudToolsForCore.getDomainId());
            assertEquals(DOMAIN,BranchDomains.name(domainId));
            service.revert(BranchType.GROUP,root,hash(BranchType.GROUP,root));
            assertNotNull(BranchView.current().group(other)); assertNotNull(BranchView.current().document(page,false));
            return null;
        });
        assertEquals(production,fingerprint());
    }

    @Test void firstSaveRejectsAProductionChangeAfterTheFormWasOpened() throws Exception {
        in((request,editor) -> {
            BranchEditor service=new BranchEditor(request);
            DocDetails oldForm=service.loadDocument(page,-1,root,editor);
            try(Connection c=DBPool.getConnection();PreparedStatement statement=c.prepareStatement("UPDATE documents SET title=? WHERE doc_id=?")) {
                statement.setString(1,"autotest concurrent LIVE change"); statement.setInt(2,page); statement.executeUpdate();
                try {
                    oldForm.setTitle("autotest stale overwrite");
                    assertThrows(ResponseStatusException.class,()->service.saveDocument(oldForm,editor));
                    assertTrue(BranchStore.readWork(c,context.branchId(),BranchType.DOCUMENT,page).isEmpty());
                } finally { statement.setString(1,"autotest LIVE"); statement.executeUpdate(); }
            }
            return null;
        });
        assertEquals(production,fingerprint());
    }

    @Test void cachedWorkViewCannotAttachTheHashOfANewerUnseenVersion() throws Exception {
        in((request,editor) -> {
            BranchEditor service=new BranchEditor(request);
            DocDetails first=service.loadDocument(page,-1,root,editor); first.setTitle("autotest displayed version"); service.saveDocument(first,editor);
            DocDetails displayed=service.loadDocument(page,-1,root,editor); String displayedHash=displayed.getEditorFields().getBranchHash();
            try(Connection c=DBPool.getConnection()) {
                Map<String,Object> stored=BranchStore.readWork(c,context.branchId(),BranchType.DOCUMENT,page).orElseThrow();
                Map<String,Object> changed=new LinkedHashMap<>(stored); changed.put("title","autotest unseen newer version");
                new BranchStore().put(context,BranchType.DOCUMENT,page,changed,BranchStore.snapshot(BranchType.DOCUMENT,stored).hash(),false);
            }
            DocDetails lateForm=service.loadDocument(page,-1,root,editor);
            assertEquals("autotest displayed version",lateForm.getTitle()); assertEquals(displayedHash,lateForm.getEditorFields().getBranchHash());
            lateForm.setTitle("autotest overwrite attempt"); assertThrows(ResponseStatusException.class,()->service.saveDocument(lateForm,editor));
            assertEquals("autotest unseen newer version",BranchView.current().document(page,false).getTitle());
            return null;
        });
        assertEquals(production,fingerprint());
    }

    @Test void rejectsForeignDomainSystemAndMultigroupWrites() throws Exception {
        int foreign=liveGroup("autotest foreign",0,"autotest-foreign");
        int system=liveGroup("System",root,"system");
        int systemPage=liveDoc(system,"/autotest-p04/system/shared.html");
        try(Connection c=DBPool.getConnection();Statement statement=c.createStatement()) {
            statement.executeUpdate("UPDATE `groups` SET domain_name='foreign-p04.example' WHERE group_id="+foreign);
            statement.executeUpdate("INSERT INTO multigroup_mapping(doc_id,master_id,redirect) VALUES ("+page+","+page+",0)");
        }
        production=fingerprint();
        try {
            in((request,editor) -> {
                BranchEditor service=new BranchEditor(request);
                DocDetails doc=service.loadDocument(page,-1,root,editor);
                assertThrows(ResponseStatusException.class,()->service.saveDocument(doc,editor));
                assertThrows(ResponseStatusException.class,()->service.loadDocument(-1,-1,foreign,editor));
                assertThrows(ResponseStatusException.class,()->service.loadDocument(-1,-1,system,editor));
                DocDetails shared=service.loadDocument(systemPage,-1,system,editor);
                assertNotNull(shared.getEditorFields().getBranchReadOnlyReason());
                assertThrows(ResponseStatusException.class,()->service.saveDocument(shared,editor));
                assertNotNull(doc.getEditorFields().getBranchReadOnlyReason());
                return null;
            });
            assertEquals(production,fingerprint());
        } finally {
            try(Connection c=DBPool.getConnection();Statement statement=c.createStatement()) { statement.executeUpdate("DELETE FROM multigroup_mapping WHERE doc_id="+page); }
        }
    }

    private String hash(BranchType type,int id) throws SQLException {
        try(Connection connection=DBPool.getConnection()) {
            return BranchStore.change(type,BranchStore.readWork(connection,context.branchId(),type,id).orElseThrow()).work().hash();
        }
    }

    private int liveGroup(String name,int parent,String url) throws Exception {
        try(Connection c=DBPool.getConnection()) { c.setAutoCommit(false); int id=BranchStore.reserveNative(c,BranchType.GROUP); Map<String,Object> values=new LinkedHashMap<>(BranchGroupMapper.initialValues(id,DOMAIN)); values.put("group_name",name); values.put("navbar",name); values.put("parent_group_id",parent); values.put("url_dir_name",url); BranchRowWriter.insertGroupWithId(c,BranchContext.live(DOMAIN,1),values);c.commit();liveGroups.add(id);return id; }
    }
    private int liveDoc(int group,String url) throws Exception {
        try(Connection c=DBPool.getConnection()) { c.setAutoCommit(false); int id=BranchStore.reserveNative(c,BranchType.DOCUMENT);Map<String,Object> values=new LinkedHashMap<>(BranchDocumentMapper.initialValues(id,1,Instant.now(),false));values.put("title","autotest LIVE");values.put("navbar","autotest LIVE");values.put("group_id",group);values.put("virtual_path",url);values.put("data","<p>autotest LIVE</p>");BranchRowWriter.insertDocumentWithId(c,BranchContext.live(DOMAIN,1),values);c.commit();liveDocs.add(id);return id; }
    }
    private String fingerprint() throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        try(Connection c=DBPool.getConnection();Statement s=c.createStatement()) {
            for(String table:List.of("documents","groups","documents_history","perex_group_doc","media","media_group_to_media","groups_scheduler","multigroup_mapping")) {
                List<String> rows=new ArrayList<>();
                try(ResultSet rs=s.executeQuery("SELECT * FROM `"+table+"`")) { while(rs.next()) { StringBuilder row=new StringBuilder(); for(int i=1;i<=rs.getMetaData().getColumnCount();i++) { String raw=rs.getString(i); byte[] value=raw==null?null:raw.getBytes(java.nio.charset.StandardCharsets.UTF_8); row.append(value==null?"NULL":Base64.getEncoder().encodeToString(value)).append('|'); } rows.add(row.toString()); } }
                Collections.sort(rows); for(String row:rows) digest.update(row.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
    private <T>T in(Call<T> callback) throws Exception {
        MockHttpServletRequest request=new MockHttpServletRequest(Constants.getServletContext()); request.setAttribute("csrfDisabled",true);
        Identity user=new Identity();user.setAdmin(true);user.setUserId(1);user.setValid(true);request.getSession().setAttribute(Constants.USER_KEY,user);
        RequestBean bean=new RequestBean();bean.setRequest(request);bean.setDomain(DOMAIN);
        try(var filter=mockStatic(SetCharacterEncodingFilter.class); var templates=mockStatic(TemplatesDB.class)) {
            templates.when(TemplatesDB::getInstance).thenReturn(mock(TemplatesDB.class));
            filter.when(SetCharacterEncodingFilter::getCurrentRequestBean).thenReturn(bean);filter.when(SetCharacterEncodingFilter::getEncoding).thenReturn("UTF-8");
            try(var scope=BranchContexts.open(request,context)) {
                EditorService editor=new EditorService(null,null,null,null,null,request);return callback.run(request,editor);
            }
        }
    }
    @FunctionalInterface private interface Call<T> { T run(MockHttpServletRequest request,EditorService editor) throws Exception; }
}
