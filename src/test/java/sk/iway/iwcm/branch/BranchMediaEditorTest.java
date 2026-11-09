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
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import sk.iway.iwcm.*;
import sk.iway.iwcm.components.media.*;
import sk.iway.iwcm.doc.*;
import sk.iway.iwcm.editor.service.MediaService;
import sk.iway.iwcm.test.BaseWebjetTest;
import sk.iway.spirit.MediaDB;
import sk.iway.spirit.model.*;

/** Real WORK media CRUD, old-page visibility, category paging and unchanged LIVE relations. */
@EnabledIfEnvironmentVariable(named="WEBJET_P05_DB",matches="true")
class BranchMediaEditorTest extends BaseWebjetTest {
    private static final String DOMAIN="p05-media.example";
    private final Map<String,String> configuration=new LinkedHashMap<>();
    private BranchContext a,b;
    private int root,page,category,liveMedia;
    private String before;

    @BeforeEach void fixture()throws Exception {
        try(Connection c=DBPool.getConnection()) {assertEquals("webjet_p01",c.getCatalog());assertTrue(c.getMetaData().getURL().contains(":33316/"));}
        for(String key:List.of("branchesEnabled","branchSingleWriterConfirmed","branchStorageRoot","multiDomainEnabled","templatesUseRecursiveSystemFolder"))configuration.put(key,Constants.getString(key));
        Constants.setBoolean("branchesEnabled",true);Constants.setBoolean("branchSingleWriterConfirmed",true);Constants.setBoolean("multiDomainEnabled",true);Constants.setBoolean("templatesUseRecursiveSystemFolder",false);
        Files.createDirectories(Path.of("/tmp/webjet-p02-private"));Constants.setString("branchStorageRoot","/tmp/webjet-p02-private");
        category=Math.toIntExact(PkeyGenerator.getNextValueAsLongStrict("media_groups"));liveMedia=Math.toIntExact(PkeyGenerator.getNextValueAsLongStrict("media"));
        try(Connection c=DBPool.getConnection()) {
            c.setAutoCommit(false);root=BranchStore.reserveNative(c,BranchType.GROUP);
            var group=new LinkedHashMap<>(BranchGroupMapper.initialValues(root,DOMAIN));group.put("group_name","autotest P05 media root");group.put("navbar","autotest P05 media root");group.put("url_dir_name","autotest-p05-media");BranchRowWriter.insertGroupWithId(c,BranchContext.live(DOMAIN,1),group);
            page=BranchStore.reserveNative(c,BranchType.DOCUMENT);var doc=new LinkedHashMap<>(BranchDocumentMapper.initialValues(page,1,Instant.parse("2020-01-01T12:00:00Z"),false));
            doc.put("group_id",root);doc.put("title","autotest old LIVE page");doc.put("navbar","autotest old LIVE page");doc.put("data","<p>LIVE</p>");doc.put("virtual_path","/autotest-p05-media/");doc.put("available",true);BranchRowWriter.insertDocumentWithId(c,BranchContext.live(DOMAIN,1),doc);
            try(PreparedStatement s=c.prepareStatement("INSERT INTO media_groups(media_group_id,media_group_name,available_groups) VALUES (?,?,'')")){s.setInt(1,category);s.setString(2,"autotest P05 media category");s.executeUpdate();}
            var media=new LinkedHashMap<>(BranchMediaMapper.initialValues(liveMedia,page,1));media.put("media_title_sk","autotest LIVE media");media.put("media_link","https://example.invalid/live.pdf");media.put("last_update",Timestamp.from(Instant.parse("2020-01-01T11:59:00Z")));BranchRowWriter.insertMediaWithId(c,BranchContext.live(DOMAIN,1),media,List.of(category));c.commit();
        }
        a=branch("autotest P05 media A");b=branch("autotest P05 media B");before=fingerprint();
    }

    @AfterEach void cleanup()throws Exception {
        try(Connection c=DBPool.getConnection();Statement s=c.createStatement()) {
            for(BranchContext context:Arrays.asList(a,b))if(context!=null)for(String table:List.of("branch_media","branch_files","branch_documents","branch_groups","branches"))s.executeUpdate("DELETE FROM "+table+" WHERE "+(table.equals("branches")?"id":"branch_id")+"="+context.branchId());
            s.executeUpdate("DELETE FROM media_group_to_media WHERE media_id="+liveMedia);s.executeUpdate("DELETE FROM media WHERE media_id="+liveMedia);s.executeUpdate("DELETE FROM media_groups WHERE media_group_id="+category);s.executeUpdate("DELETE FROM documents WHERE doc_id="+page);s.executeUpdate("DELETE FROM `groups` WHERE group_id="+root);
        } finally {configuration.forEach(Constants::setString);}
    }

    @Test void editsOldPageMediaWithoutSavingItsOwnerAndKeepsStableIdentity()throws Exception {
        in(a,request->{
            BranchMediaEditor editor=new BranchMediaEditor(request);Media media=editor.load(liveMedia,page);
            media.getEditorFields().setGroups(new Integer[]{category});media.setMediaTitleSk("autotest WORK media");
            media=editor.save(media,page,media.getEditorFields().getBranchHash());assertEquals(liveMedia,media.getMediaId());
            assertEquals(Instant.parse("2020-01-01T12:00:00Z").toEpochMilli(),media.getLastUpdate().getTime());
            assertEquals(1,MediaDB.getMedia(BranchView.current().document(page,false),null).size());
            try(Connection c=DBPool.getConnection()) {assertTrue(BranchStore.readWork(c,a.branchId(),BranchType.DOCUMENT,page).isEmpty());}
            return null;
        });
        in(new BranchContext(a.branchId(),DOMAIN,1,BranchContext.Mode.PREVIEW),request->{assertEquals("autotest WORK media",MediaDB.getMedia(BranchView.current().document(page,false),null).get(0).getMediaTitleSk());return null;});
        in(b,request->{assertEquals("autotest LIVE media",BranchMedia.one(liveMedia).getMediaTitleSk());return null;});
        assertEquals(before,fingerprint());
    }

    @Test void createsUpdatesDeletesOnWorkOnlyPageAndRejectsForeignStaleAndMissingOwners()throws Exception {
        int createdPage;
        var doc=new LinkedHashMap<>(BranchDocumentMapper.initialValues(1,1,Instant.now(),false));doc.put("group_id",root);doc.put("title","autotest WORK-only page");doc.put("navbar","autotest WORK-only page");doc.put("virtual_path","/autotest-p05-media/work.html");doc.put("data","<p>WORK only</p>");
        createdPage=((Number)new BranchStore().create(a,BranchType.DOCUMENT,doc).get("doc_id")).intValue();
        in(a,request->{
            BranchMediaEditor editor=new BranchMediaEditor(request);Media fresh=fresh("autotest new",category);Media saved=editor.save(fresh,createdPage,"new");int id=saved.getMediaId();assertTrue(id>0);
            String stale=saved.getEditorFields().getBranchHash();saved.getEditorFields().setGroups(new Integer[]{category});saved.setMediaSortOrder(50);saved.setMediaTitleSk("autotest edited");saved=editor.save(saved,createdPage,stale);assertEquals(id,saved.getMediaId());assertEquals(50,saved.getMediaSortOrder());
            Media current=saved;current.getEditorFields().setGroups(new Integer[]{category});assertThrows(ResponseStatusException.class,()->editor.save(current,createdPage,stale));
            assertThrows(ResponseStatusException.class,()->editor.load(id,page));assertThrows(ResponseStatusException.class,()->editor.save(fresh,-1,"new"));assertThrows(ResponseStatusException.class,()->editor.save(fresh,Integer.MAX_VALUE,"new"));
            assertTrue(editor.delete(id,createdPage,current.getEditorFields().getBranchHash()));assertNull(BranchMedia.one(id));
            try(Connection c=DBPool.getConnection()) {assertTrue(BranchStore.readLive(c,BranchType.MEDIA,id).isEmpty());assertTrue(BranchStore.readWork(c,a.branchId(),BranchType.MEDIA,id).isEmpty());}
            return null;
        });
        assertEquals(before,fingerprint());
    }

    @Test void categoryFilterCountsBeforePagingAndDeletionDoesNotChangeProduction()throws Exception {
        in(a,request->{
            BranchMediaEditor editor=new BranchMediaEditor(request);editor.save(fresh("autotest with category",category),page,"new");editor.save(fresh("autotest without category"),page,"new");
            request.setParameter("docId",Integer.toString(page));request.setParameter("size","1");request.setParameter("searchEditorFields.groups",Integer.toString(category));
            MediaRestController controller=new MediaRestController(mock(MediaRepository.class),mock(MediaGroupRepository.class));
            controller.setRequest(request);
            var found=controller.getAllItems(PageRequest.of(0,1));assertEquals(2,found.getTotalElements());assertEquals(1,found.getNumberOfElements());
            assertEquals(1,controller.getAllItems(PageRequest.of(1,1)).getNumberOfElements());
            Media old=editor.load(liveMedia,page);assertTrue(editor.delete(liveMedia,page,old.getEditorFields().getBranchHash()));assertNull(BranchMedia.one(liveMedia));
            assertEquals(1,controller.getAllItems(PageRequest.of(0,1)).getTotalElements());
            new MediaService(mock(MediaRepository.class),request).deleteTempMedia();
            assertThrows(IllegalStateException.class,()->new MediaService(mock(MediaRepository.class),request).updateMediaLink("old","new",DOMAIN));
            return null;
        });
        assertEquals(before,fingerprint());
    }

    @Test void noopPreservesUnpublishedLiveCutoffAndInvalidCategoriesAreRejected()throws Exception {
        try(Connection c=DBPool.getConnection();Statement s=c.createStatement()) {s.executeUpdate("UPDATE media SET last_update='2030-01-01 12:00:00' WHERE media_id="+liveMedia);}
        before=fingerprint();
        in(a,request->{
            BranchMediaEditor editor=new BranchMediaEditor(request);Media media=editor.load(liveMedia,page);media.getEditorFields().setGroups(new Integer[]{category});editor.save(media,page,media.getEditorFields().getBranchHash());
            assertTrue(MediaDB.getMedia(BranchView.current().document(page,false),null).isEmpty());
            Media bad=fresh("autotest bad category",Integer.MAX_VALUE);assertThrows(ResponseStatusException.class,()->editor.save(bad,page,"new"));
            Identity user=(Identity)request.getSession().getAttribute(Constants.USER_KEY);user.addDisabledItem("editor_edit_media_all");user.setEditableGroups(Integer.toString(Integer.MAX_VALUE));
            assertThrows(ResponseStatusException.class,()->editor.load(liveMedia,page));return null;
        });
        assertEquals(before,fingerprint());
    }

    private static Media fresh(String title,Integer...categories) {Media m=new Media();m.setMediaTitleSk(title);m.setMediaLink("https://example.invalid/attachment.pdf");m.setMediaThumbLink("");MediaEditorFields fields=new MediaEditorFields();fields.setGroups(categories);fields.setBranchHash("new");m.setEditorFields(fields);return m;}
    private BranchContext branch(String name) {return new BranchContext(new BranchService().create(DOMAIN,name,"disposable P05 media test",1).getId(),DOMAIN,1,BranchContext.Mode.ADMIN);}
    private String fingerprint()throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");try(Connection c=DBPool.getConnection();Statement s=c.createStatement()) {for(String table:List.of("documents","groups","documents_history","perex_group_doc","media","media_group_to_media","media_groups","groups_scheduler","multigroup_mapping","file_atr","dirprop")) {List<String> rows=new ArrayList<>();try(ResultSet r=s.executeQuery("SELECT * FROM `"+table+"`")){while(r.next()){StringBuilder row=new StringBuilder();for(int i=1;i<=r.getMetaData().getColumnCount();i++)row.append(Base64.getEncoder().encodeToString(Objects.toString(r.getString(i),"NULL").getBytes(java.nio.charset.StandardCharsets.UTF_8))).append('|');rows.add(row.toString());}}Collections.sort(rows);for(String row:rows)digest.update(row.getBytes(java.nio.charset.StandardCharsets.UTF_8));}}return HexFormat.of().formatHex(digest.digest());
    }
    private <T>T in(BranchContext context,Call<T> call)throws Exception {
        MockHttpServletRequest request=new MockHttpServletRequest(Constants.getServletContext());request.setAttribute("csrfDisabled",true);Identity user=new Identity();user.setAdmin(true);user.setUserId(1);user.setValid(true);request.getSession().setAttribute(Constants.USER_KEY,user);RequestBean bean=new RequestBean();bean.setRequest(request);bean.setDomain(DOMAIN);
        try(var filter=mockStatic(SetCharacterEncodingFilter.class)) {filter.when(SetCharacterEncodingFilter::getCurrentRequestBean).thenReturn(bean);filter.when(SetCharacterEncodingFilter::getEncoding).thenReturn("UTF-8");try(var scope=BranchContexts.open(request,context)){return call.run(request);}}
    }
    @FunctionalInterface private interface Call<T> {T run(MockHttpServletRequest request)throws Exception;}
}
