package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.mock.web.*;
import org.springframework.data.domain.PageRequest;
import sk.iway.iwcm.*;
import sk.iway.iwcm.doc.*;
import sk.iway.iwcm.editor.rest.GetAllItemsDocOptions;
import sk.iway.spirit.MediaDB;

/** Real prepared bytes, media and administrative SQL; no mocked content lists. */
@EnabledIfEnvironmentVariable(named="WEBJET_P03_DB",matches="true")
class BranchResourcesTest extends BranchJpaTest {
    private static final String DOMAIN="p03-resources.example";
    private long branch;private int group,doc,liveOwner,liveMedia,category;
    private Path privateRoot;
    private final Map<String,String> config=new LinkedHashMap<>();
    @BeforeEach void fixture()throws Exception {
        for(String key:List.of("enableStaticFilesExternalDir","branchesEnabled","branchSingleWriterConfirmed","branchStorageRoot","mediaCheckAvailabilityMode","multiDomainEnabled","galleryEnableWatermarking"))config.put(key,Constants.getString(key));
        try(Connection connection=DBPool.getConnection()){assertEquals("webjet_p01",connection.getCatalog());assertTrue(connection.getMetaData().getURL().contains(":33316/"));}
        privateRoot=Files.createTempDirectory("webjet-p03-resources-");Constants.setBoolean("branchesEnabled",true);Constants.setBoolean("branchSingleWriterConfirmed",true);Constants.setBoolean("enableStaticFilesExternalDir",true);Constants.setString("branchStorageRoot",privateRoot.toString());Constants.setBoolean("multiDomainEnabled",true);Constants.setBoolean("galleryEnableWatermarking",false);
        branch=createBranch(BranchTestDomains.ensure(DOMAIN),"P03 resources","prepared bytes",1).getId();
        try(Connection connection=DBPool.getConnection()) {
            connection.setAutoCommit(false);group=BranchStore.reserveNative(connection,BranchType.GROUP);
            var values=new LinkedHashMap<>(BranchGroupMapper.initialValues(group,DOMAIN));values.put("group_name","P03 resources");values.put("url_dir_name","resources");BranchRowWriter.insertGroupWithId(connection,BranchContext.live(DOMAIN,1),values);connection.commit();
        }
        var values=new LinkedHashMap<>(BranchDocumentMapper.initialValues(1,1,Instant.parse("2026-11-04T10:00:00Z"),false));values.put("title","Prepared page");values.put("group_id",group);values.put("available",true);values.put("virtual_path","/resources/page.html");values.put("data","<p>complete HTML</p>");values.put("perex_image","/images/p03-resource.png");
        doc=((Number)new BranchStore().create(admin(),BranchType.DOCUMENT,values).get("doc_id")).intValue();
    }
    @AfterEach void cleanup()throws Exception {
        try(Connection connection=DBPool.getConnection();Statement statement=connection.createStatement()) {
            for(String table:List.of("branch_files","branch_media","branch_documents","branch_groups","branches"))statement.executeUpdate("DELETE FROM "+table+" WHERE "+(table.equals("branches")?"id":"branch_id")+"="+branch);
            statement.executeUpdate("DELETE FROM media_group_to_media WHERE media_id="+liveMedia);
            statement.executeUpdate("DELETE FROM media WHERE media_id="+liveMedia);
            if(category>0)statement.executeUpdate("DELETE FROM media_groups WHERE media_group_id="+category);
            statement.executeUpdate("DELETE FROM documents WHERE doc_id="+liveOwner);
            statement.executeUpdate("DELETE FROM `groups` WHERE group_id="+group);
        } finally {config.forEach(Constants::setString);if(privateRoot!=null)try(var paths=Files.walk(privateRoot)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
    }
    @Test void fileMediaThumbnailAndAdministrativeFilter()throws Exception {
        Path pdf=Files.createTempFile(privateRoot,"proof-",".pdf");Files.writeString(pdf,"%PDF-1.4 P03 prepared attachment");
        var file=new BranchStore().putFile(admin(),"/files/p03-proof.pdf","FILE",BranchSnapshot.absent(),BranchFiles.hash(pdf),Files.size(pdf),"application/pdf",false);
        Path staged=BranchFiles.workPath(branch,((Number)file.get("row_id")).longValue());Files.createDirectories(staged.getParent());Files.move(pdf,staged);
        Path image=Files.createTempFile(privateRoot,"proof-",".png");ImageIO.write(new BufferedImage(64,64,BufferedImage.TYPE_INT_RGB),"png",image.toFile());
        var picture=new BranchStore().putFile(admin(),"/images/p03-resource.png","FILE",BranchSnapshot.absent(),BranchFiles.hash(image),Files.size(image),"image/png",false);
        Path imageBytes=BranchFiles.workPath(branch,((Number)picture.get("row_id")).longValue());Files.createDirectories(imageBytes.getParent());Files.move(image,imageBytes);
        var variant=new BranchStore().putFile(admin(),"/images/s_p03-resource.png","FILE",BranchSnapshot.absent(),BranchFiles.hash(imageBytes),Files.size(imageBytes),"image/png",false);
        Path variantBytes=BranchFiles.workPath(branch,((Number)variant.get("row_id")).longValue());Files.createDirectories(variantBytes.getParent());Files.copy(imageBytes,variantBytes);
        var media=new LinkedHashMap<>(BranchMediaMapper.initialValues(1,doc,BranchDomains.id(DOMAIN)));media.put("media_title_sk","Prepared attachment");media.put("media_link","/files/p03-proof.pdf");media.put("group_ids_snapshot",List.of());new BranchStore().create(admin(),BranchType.MEDIA,media);
        prepareExistingMedia();
        RequestBean bean=new RequestBean();bean.setDomain(DOMAIN);MockHttpServletRequest request=new MockHttpServletRequest(Constants.getServletContext());
        Identity user=new Identity();user.setAdmin(true);user.setUserId(1);request.getSession().setAttribute(Constants.USER_KEY,user);
        try(var lifecycle=mockStatic(SetCharacterEncodingFilter.class)) {
            lifecycle.when(SetCharacterEncodingFilter::getCurrentRequestBean).thenReturn(bean);
            try(var scope=BranchContexts.open(request,preview())) {
                Constants.setString("mediaCheckAvailabilityMode","fast");
                assertTrue(BranchFiles.exists("/files/p03-proof.pdf"));assertEquals(1,MediaDB.getMediaByMediaGroups(request.getSession(),"documents",doc,null,0,true).size());
                assertEquals("/images/s_p03-resource.png",DocDB.getInstance().getDoc(doc).getPerexImageSmall());
                var effective=MediaDB.getMediaByMediaGroups(request.getSession(),"documents",doc,null,0,true).get(0);assertEquals(DocDB.getInstance().getDoc(doc).getDateCreated(),effective.getLastUpdate().getTime());
                MockHttpServletResponse response=new MockHttpServletResponse();request.addParameter("w","16");request.addParameter("h","16");BranchFiles.thumbnail("/images/p03-resource.png",request,response);
                var thumb=ImageIO.read(new java.io.ByteArrayInputStream(response.getContentAsByteArray()));assertEquals(16,thumb.getWidth());assertEquals(16,thumb.getHeight());
            }
            try(var scope=BranchContexts.open(request,admin())) {
                request.addParameter("searchTitle","^Prepared$");var options=new GetAllItemsDocOptions(request);options.setGroupId(group);options.setPageable(PageRequest.of(0,1));assertEquals(0,BranchDocumentQuery.list(options).getTotalElements());
                request.setParameter("searchTitle","Prepared");assertEquals(1,BranchDocumentQuery.list(options).getTotalElements());assertEquals(sk.iway.iwcm.editor.service.WebpagesService.DATA_NOT_LOADED,BranchDocumentQuery.list(options).getContent().get(0).getData());
                request.setParameter("searchAvailable","true");assertEquals(1,BranchDocumentQuery.list(options).getTotalElements());request.setParameter("searchAvailable","false");assertEquals(0,BranchDocumentQuery.list(options).getTotalElements());
                request.removeParameter("searchAvailable");request.setParameter("searchId",Integer.toString(doc));assertEquals(1,BranchDocumentQuery.list(options).getTotalElements());
                request.removeParameter("searchId");request.setParameter("searchEditorFields.statusIcons","available:false");assertEquals(0,BranchDocumentQuery.list(options).getTotalElements());
                request.setParameter("searchEditorFields.statusIcons","available:true");assertEquals(1,BranchDocumentQuery.list(options).getTotalElements());request.removeParameter("searchEditorFields.statusIcons");
                request.setParameter("searchId","range:"+doc);assertEquals(1,BranchDocumentQuery.list(options).getTotalElements());
                request.setParameter("searchId","range:"+(doc+1));assertEquals(0,BranchDocumentQuery.list(options).getTotalElements());
                request.setParameter("searchId","range:-"+doc);assertEquals(1,BranchDocumentQuery.list(options).getTotalElements());
                request.setParameter("searchId","range:-"+(doc-1));assertEquals(0,BranchDocumentQuery.list(options).getTotalElements());request.removeParameter("searchId");
                long savedAt=DocDB.getInstance().getDoc(doc).getDateCreated();
                request.setParameter("searchDateCreated","daterange:"+savedAt);assertEquals(1,BranchDocumentQuery.list(options).getTotalElements());
                request.setParameter("searchDateCreated","daterange:"+(savedAt+1));assertEquals(0,BranchDocumentQuery.list(options).getTotalElements());
                request.setParameter("searchDateCreated","daterange:-"+(savedAt+1));assertEquals(1,BranchDocumentQuery.list(options).getTotalElements());
                request.setParameter("searchDateCreated","daterange:-"+savedAt);assertEquals(0,BranchDocumentQuery.list(options).getTotalElements());request.removeParameter("searchDateCreated");
            }
            chooseMedia("LIVE");
            try(var scope=BranchContexts.open(request,preview())) {
                var selected=MediaDB.getMedia(liveMedia);assertEquals("LIVE attachment",selected.getMediaTitleSk());assertEquals(Instant.parse("2019-01-01T10:00:00Z").toEpochMilli(),selected.getLastUpdate().getTime());
                assertTrue(selected.getGroups().isEmpty());
            }
            chooseMedia("WORK");
            try(var scope=BranchContexts.open(request,preview())) {
                var selected=MediaDB.getMedia(liveMedia);assertEquals("WORK attachment",selected.getMediaTitleSk());assertEquals(Instant.parse("2020-01-01T10:00:00Z").toEpochMilli(),selected.getLastUpdate().getTime());
                assertEquals("P03 existing category",selected.getGroups().get(0).getMediaGroupName());assertEquals(1,MediaDB.getMediaByMediaGroups(request.getSession(),"documents",liveOwner,selected.getGroups(),0,true).size());
            }
            try(Connection connection=DBPool.getConnection();PreparedStatement statement=connection.prepareStatement("UPDATE media SET media_title_sk='Changed LIVE' WHERE media_id=?")){statement.setInt(1,liveMedia);statement.executeUpdate();}
            try(var scope=BranchContexts.open(request,preview())) {assertThrows(BranchDecision.StaleChoiceException.class,()->MediaDB.getMedia(liveMedia));}
            assertFalse(BranchContexts.current().isBranch());assertFalse(BranchFiles.exists("/files/p03-proof.pdf"));
        }
        try(Connection connection=DBPool.getConnection()){assertTrue(BranchStore.readLive(connection,BranchType.DOCUMENT,doc).isEmpty());}
    }
    private void prepareExistingMedia()throws Exception {
        try(Connection connection=DBPool.getConnection()) {
            try(PreparedStatement check=connection.prepareStatement("SELECT COUNT(*) FROM media_groups WHERE media_group_id=1940999");ResultSet result=check.executeQuery()){result.next();assertEquals(0,result.getInt(1));}
            try(PreparedStatement definition=connection.prepareStatement("INSERT INTO media_groups(media_group_id,media_group_name,available_groups) VALUES(1940999,'P03 existing category',?)")){definition.setString(1,Integer.toString(group));definition.executeUpdate();category=1940999;}
            connection.setAutoCommit(false);liveOwner=BranchStore.reserveNative(connection,BranchType.DOCUMENT);
            var page=new LinkedHashMap<>(BranchDocumentMapper.initialValues(liveOwner,1,Instant.parse("2020-01-01T10:00:00Z"),false));page.put("group_id",group);page.put("title","Media owner");page.put("available",true);page.put("virtual_path","/resources/owner.html");BranchRowWriter.insertDocumentWithId(connection,BranchContext.live(DOMAIN,1),page);
            liveMedia=Math.toIntExact(PkeyGenerator.getNextValueAsLongStrict("media"));var medium=new LinkedHashMap<>(BranchMediaMapper.initialValues(liveMedia,liveOwner,BranchDomains.id(DOMAIN)));medium.put("media_title_sk","LIVE attachment");medium.put("media_link","https://example.invalid/attachment.pdf");medium.put("last_update",Timestamp.from(Instant.parse("2019-01-01T10:00:00Z")));BranchRowWriter.insertMediaWithId(connection,BranchContext.live(DOMAIN,1),medium,List.of());connection.commit();
            var original=BranchStore.readLive(connection,BranchType.MEDIA,liveMedia).orElseThrow();var changed=new LinkedHashMap<>(original);changed.put("media_title_sk","WORK attachment");changed.put("group_ids_snapshot",List.of(category));new BranchStore().put(admin(),BranchType.MEDIA,liveMedia,changed,BranchStore.snapshot(BranchType.MEDIA,original).hash(),false);
        }
    }
    private void chooseMedia(String source)throws Exception {
        try(Connection connection=DBPool.getConnection()) {
            var live=BranchStore.readLive(connection,BranchType.MEDIA,liveMedia).orElseThrow();var work=BranchStore.readWork(connection,branch,BranchType.MEDIA,liveMedia).orElseThrow();
            try(PreparedStatement statement=connection.prepareStatement("UPDATE branch_media SET resolved_choice=?,resolved_live_hash=?,resolved_work_hash=? WHERE branch_id=? AND media_id=?")) {statement.setString(1,source);statement.setString(2,BranchStore.snapshot(BranchType.MEDIA,live).hash());statement.setString(3,BranchStore.snapshot(BranchType.MEDIA,work).hash());statement.setLong(4,branch);statement.setInt(5,liveMedia);statement.executeUpdate();}
        }
    }
    private BranchContext admin(){return new BranchContext(branch,DOMAIN,1,BranchContext.Mode.ADMIN);}
    private BranchContext preview(){return new BranchContext(branch,DOMAIN,1,BranchContext.Mode.PREVIEW);}
}
