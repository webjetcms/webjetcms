package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockPart;
import org.springframework.mock.web.MockMultipartFile;
import sk.iway.iwcm.*;
import sk.iway.iwcm.system.elfinder.BranchFsVolume;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Verifies actual WORK bytes, markers, conflicts and side-effect isolation on the POC database. */
@EnabledIfEnvironmentVariable(named="WEBJET_P05_DB",matches="true")
class BranchFilesTest extends BaseWebjetTest {
    private static final String DOMAIN="p05-files.example";
    private final Map<String,String> configuration=new LinkedHashMap<>();
    private BranchContext a,b;
    private Path privateRoot,liveRoot,source;
    private String root,originalFingerprint;

    @BeforeEach void fixture()throws Exception {
        try(Connection connection=DBPool.getConnection()) {
            assertEquals("webjet_p01",connection.getCatalog());assertTrue(connection.getMetaData().getURL().contains(":33316/"));
        }
        for(String key:List.of("branchesEnabled","branchSingleWriterConfirmed","branchStorageRoot","multiDomainEnabled","fbrowserShowOnlyWritableFolders","defaultDisableUpload")) configuration.put(key,Constants.getString(key));
        privateRoot=Files.createTempDirectory("webjet-p05-files-");
        Constants.setBoolean("branchesEnabled",true);Constants.setBoolean("branchSingleWriterConfirmed",true);Constants.setString("branchStorageRoot",privateRoot.toString());Constants.setBoolean("multiDomainEnabled",true);Constants.setBoolean("fbrowserShowOnlyWritableFolders",false);Constants.setBoolean("defaultDisableUpload",false);
        a=context(new BranchService().create(DOMAIN,"autotest P05 A","disposable files",1).getId());
        b=context(new BranchService().create(DOMAIN,"autotest P05 B","disposable files",1).getId());
        root="/files/autotest-p05-"+UUID.randomUUID();liveRoot=Path.of(Tools.getRealPath(root));Files.createDirectories(liveRoot);
        Files.writeString(liveRoot.resolve("existing.txt"),"original LIVE bytes");
        source=privateRoot.resolve("upload.txt");Files.writeString(source,"new WORK bytes");
        originalFingerprint=fingerprint();
    }
    @AfterEach void cleanup()throws Exception {
        try(Connection connection=DBPool.getConnection();Statement statement=connection.createStatement()) {
            for(BranchContext context:List.of(a,b)) if(context!=null) for(String table:List.of("branch_files","branch_media","branch_documents","branch_groups","branches"))
                statement.executeUpdate("DELETE FROM "+table+" WHERE "+(table.equals("branches")?"id":"branch_id")+"="+context.branchId());
        } finally {
            configuration.forEach(Constants::setString);
            for(Path directory:new Path[]{privateRoot,liveRoot}) if(directory!=null&&Files.exists(directory)) try(var files=Files.walk(directory)) {
                for(Path path:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);
            }
        }
    }

    @Test void overwriteAndDeleteKeepOtherVersionsAndOriginalBase()throws Exception {
        in(a,request -> {
            String path=root+"/existing.txt";BranchSnapshot original=BranchFiles.liveSnapshot(path);
            BranchFiles.put(path,source,request);assertEquals("new WORK bytes",Files.readString(BranchFiles.read(path)));
            Map<String,Object> first=work(a,path);assertEquals(original.hash(),first.get("base_hash"));
            Path bytes=BranchFiles.workPath(a.branchId(),((Number)first.get("row_id")).longValue());
            assertEquals("original LIVE bytes",Files.readString(bytes.resolveSibling("base")));
            Files.writeString(source,"second WORK bytes");BranchFiles.put(path,source,request);
            Map<String,Object> second=work(a,path);assertEquals(first.get("row_id"),second.get("row_id"));assertEquals(first.get("base_hash"),second.get("base_hash"));
            assertEquals("second WORK bytes",Files.readString(BranchFiles.read(path)));
            BranchFiles.delete(path,request);assertNull(BranchFiles.read(path));assertFalse(BranchFiles.exists(path));assertFalse(BranchFiles.list(root).contains(path));
            return null;
        });
        in(b,request -> {assertEquals("original LIVE bytes",Files.readString(BranchFiles.read(root+"/existing.txt")));return null;});
        assertEquals("original LIVE bytes",Files.readString(liveRoot.resolve("existing.txt")));assertEquals(originalFingerprint,fingerprint());
    }

    @Test void emptyMarkersSurviveReloadAndMovesAreExplicitWorkChanges()throws Exception {
        in(a,request -> {
            String directory=root+"/empty";BranchFiles.mkdir(directory,request);BranchView.invalidate();
            assertTrue(BranchFiles.directoryExists(directory));assertTrue(BranchFiles.list(root).contains(directory));assertFalse(Files.exists(liveRoot.resolve("empty")));
            BranchFsVolume volume=new BranchFsVolume("POC files");assertTrue(volume.isFolder(volume.fromPath(directory)));
            BranchFiles.put(directory+"/new.txt",source,request);
            assertThrows(IllegalArgumentException.class,()->BranchFiles.removeDirectoryMarker(directory,request));
            assertThrows(IllegalArgumentException.class,()->BranchFiles.delete(directory,request));
            assertThrows(IllegalArgumentException.class,()->BranchFiles.move(directory,root+"/other",request));
            BranchFiles.move(directory+"/new.txt",root+"/moved.txt",request);assertFalse(BranchFiles.exists(directory+"/new.txt"));assertTrue(BranchFiles.exists(root+"/moved.txt"));
            BranchFiles.removeDirectoryMarker(directory,request);assertFalse(BranchFiles.directoryExists(directory));
            BranchFiles.delete(root+"/moved.txt",request);assertTrue(workOptional(a,root+"/moved.txt").isEmpty());
            BranchFiles.move(root+"/existing.txt",root+"/renamed.txt",request);assertNull(BranchFiles.read(root+"/existing.txt"));assertNotNull(BranchFiles.read(root+"/renamed.txt"));
            assertEquals("DELETE",work(a,root+"/existing.txt").get("operation"));
            Path deleted=BranchFiles.workPath(a.branchId(),((Number)work(a,root+"/existing.txt").get("row_id")).longValue());
            assertEquals("original LIVE bytes",Files.readString(deleted.resolveSibling("base")));
            return null;
        });
        assertEquals("original LIVE bytes",Files.readString(liveRoot.resolve("existing.txt")));assertEquals(originalFingerprint,fingerprint());
    }

    @Test void deletedLiveFileCannotBeReplacedWithADirectoryMarker()throws Exception {
        in(a,request -> {
            String path=root+"/existing.txt";BranchFiles.delete(path,request);
            Map<String,Object> deleted=work(a,path);assertNull(BranchFiles.read(path));
            assertThrows(IllegalArgumentException.class,()->BranchFiles.mkdir(path,request));
            assertThrows(IllegalArgumentException.class,()->BranchFiles.mkdir(path+"/nested",request));
            assertThrows(IllegalArgumentException.class,()->new BranchStore().putFile(a,path,"DIRECTORY",BranchFiles.liveSnapshot(path),null,null,null,false));
            assertEquals(deleted,work(a,path));assertFalse(BranchFiles.directoryExists(path));
            return null;
        });
        assertEquals("original LIVE bytes",Files.readString(liveRoot.resolve("existing.txt")));assertEquals(originalFingerprint,fingerprint());
    }

    @Test void uploadsCannotUseLiveOrDeletedWorkingFilesAsParentDirectories()throws Exception {
        in(a,request -> {
            String path=root+"/existing.txt",child=path+"/child.txt";
            assertThrows(IllegalArgumentException.class,()->BranchFiles.put(child,source,request));
            BranchFiles.delete(path,request);assertNull(BranchFiles.read(path));
            assertThrows(IllegalArgumentException.class,()->BranchFiles.put(child,source,request));
            // Even when the current LIVE path disappears, its WORK tombstone keeps the FILE identity.
            Files.delete(liveRoot.resolve("existing.txt"));
            try {assertThrows(IllegalArgumentException.class,()->BranchFiles.put(child,source,request));}
            finally {Files.writeString(liveRoot.resolve("existing.txt"),"original LIVE bytes");}
            assertTrue(workOptional(a,child).isEmpty());assertEquals("DELETE",work(a,path).get("operation"));
            return null;
        });
        assertEquals("original LIVE bytes",Files.readString(liveRoot.resolve("existing.txt")));assertEquals(originalFingerprint,fingerprint());
    }

    @Test void rejectsGalleryVariantsTraversalTypesAndPermissionsBeforeWrites()throws Exception {
        in(a,request -> {
            for(String path:List.of("/images/gallery/p05.jpg","/images/gallery/sub/o_p05.jpg","/images/gallery/sub/s_p05.jpg"))
                assertThrows(IllegalArgumentException.class,()->BranchFiles.put(path,source,request));
            assertThrows(IllegalArgumentException.class,()->BranchFiles.put(root+"/bad.jsp",source,request));
            assertThrows(IllegalArgumentException.class,()->BranchFiles.put("/files/protected/planned-changes/1/images/autotest.png",source,request));
            assertThrows(IllegalArgumentException.class,()->BranchFiles.put("/files/../../private/p05.txt",source,request));
            request.removeAttribute("csrfDisabled");assertThrows(org.springframework.web.server.ResponseStatusException.class,()->BranchFiles.put(root+"/csrf.txt",source,request));request.setAttribute("csrfDisabled",true);
            Identity user=(Identity)request.getSession().getAttribute(Constants.USER_KEY);user.setWritableFolders("/files/unrelated/*");
            assertThrows(IllegalArgumentException.class,()->BranchFiles.put(root+"/denied.txt",source,request));
            assertThrows(IllegalArgumentException.class,()->BranchFiles.delete(root+"/existing.txt",request));
            return null;
        });
        try(Connection connection=DBPool.getConnection();PreparedStatement statement=connection.prepareStatement("SELECT COUNT(*) FROM branch_files WHERE branch_id=?")) {
            statement.setLong(1,a.branchId());try(ResultSet rows=statement.executeQuery()) {rows.next();assertEquals(0,rows.getInt(1));}
        }
        assertEquals(originalFingerprint,fingerprint());
    }

    @Test void concurrentLiveEditProducesAFileConflictWithoutOverwritingBase()throws Exception {
        in(a,request -> {
            String path=root+"/existing.txt";BranchFiles.put(path,source,request);
            BranchChange change=BranchStore.change(BranchType.FILE,work(a,path));
            assertFalse(change.decide(BranchFiles.liveSnapshot(path)).conflict());
            Files.writeString(liveRoot.resolve("existing.txt"),"concurrent LIVE edit");
            assertTrue(change.decide(BranchFiles.liveSnapshot(path)).conflict());
            assertEquals(BranchDecision.Source.WORK,change.decide(BranchFiles.liveSnapshot(path)).source());
            assertEquals("new WORK bytes",Files.readString(BranchFiles.read(path)));
            return null;
        });
        assertEquals(originalFingerprint,fingerprint());
    }

    @Test void modernChunkConflictAndCkeditorEntryPointsUseWorkingBytes()throws Exception {
        in(a,request -> {
            class UploadServlet extends sk.iway.iwcm.admin.upload.AdminUploadServlet {
                void upload(MockHttpServletRequest input,MockHttpServletResponse output)throws Exception {super.doPost(input,output);}
            }
            UploadServlet servlet=new UploadServlet();
            request.setParameter("name","existing.txt");request.setParameter("destinationFolder",root+"/");request.setParameter("uploadType","file");request.setParameter("writeDirectlyToDestination","true");
            byte[] incoming="modern upload WORK".getBytes(StandardCharsets.UTF_8);
            request.setParameter("dztotalfilesize",Integer.toString(incoming.length));request.addPart(new MockPart("file","existing.txt",incoming));
            MockHttpServletResponse response=new MockHttpServletResponse();servlet.upload(request,response);
            org.json.JSONObject conflict=new org.json.JSONObject(response.getContentAsString());
            assertTrue(conflict.getBoolean("success"));assertTrue(conflict.getBoolean("exists"));
            String key=conflict.getString("key");
            try {
                org.json.JSONObject replaced=new org.json.JSONObject(new sk.iway.iwcm.admin.upload.AdminUploadController().overwrite(key,root+"/","existing.txt","file",request));
                assertTrue(replaced.getBoolean("success"));assertEquals("modern upload WORK",Files.readString(BranchFiles.read(root+"/existing.txt")));
            } finally {sk.iway.iwcm.admin.upload.AdminUploadServlet.deleteTempFile(key);}
            assertEquals("original LIVE bytes",Files.readString(liveRoot.resolve("existing.txt")));
            return null;
        });
        in(a,request -> {
            // Only directory naming is fixed; the upload entry point, type checks and persistence stay real.
            try(var paths=org.mockito.Mockito.mockStatic(sk.iway.iwcm.common.UploadFileTools.class,org.mockito.Mockito.CALLS_REAL_METHODS)) {
                paths.when(()->sk.iway.iwcm.common.UploadFileTools.getPageUploadSubDir(-1,-1,"autotest P05",null)).thenReturn("/autotest-p05-ckeditor");
                request.setParameter("docId","-1");request.setParameter("groupId","-1");request.setParameter("title","autotest P05");
                java.awt.image.BufferedImage image=new java.awt.image.BufferedImage(16,16,java.awt.image.BufferedImage.TYPE_INT_RGB);
                java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",bytes);
                MockMultipartFile upload=new MockMultipartFile("upload","autotest-p05.png","image/png",bytes.toByteArray());
                MockHttpServletResponse response=new MockHttpServletResponse();
                assertNull(sk.iway.iwcm.editor.UploadFileAction.execute(request,response,upload));
                org.json.JSONObject result=new org.json.JSONObject(response.getContentAsString());assertEquals(1,result.getInt("uploaded"));
                String path=result.getString("url");assertNotNull(BranchFiles.read(path));assertFalse(Files.exists(Path.of(Tools.getRealPath(path))));
                BranchFiles.delete(path,request);
                MockMultipartFile forbidden=new MockMultipartFile("upload","bad.jsp","text/plain","forbidden".getBytes(StandardCharsets.UTF_8));
                assertThrows(IllegalArgumentException.class,()->sk.iway.iwcm.editor.UploadFileAction.execute(request,new MockHttpServletResponse(),forbidden));
                paths.when(()->sk.iway.iwcm.common.UploadFileTools.getPageUploadSubDir(-1,-1,"autotest P05",null)).thenReturn("/gallery/autotest-p05");
                assertThrows(IllegalArgumentException.class,()->sk.iway.iwcm.editor.UploadFileAction.execute(request,new MockHttpServletResponse(),upload));
            }
            return null;
        });
        assertEquals(originalFingerprint,fingerprint());
    }

    private Map<String,Object> work(BranchContext context,String path)throws Exception {return workOptional(context,path).orElseThrow();}
    private Optional<Map<String,Object>> workOptional(BranchContext context,String path)throws Exception {
        try(Connection connection=DBPool.getConnection()) {return BranchStore.readWork(connection,context.branchId(),BranchType.FILE,path);}
    }
    private BranchContext context(long id) {return new BranchContext(id,DOMAIN,1,BranchContext.Mode.ADMIN);}
    private <T>T in(BranchContext context,Call<T> callback)throws Exception {
        MockHttpServletRequest request=new MockHttpServletRequest(Constants.getServletContext());request.setAttribute("csrfDisabled",true);
        Identity user=new Identity();user.setAdmin(true);user.setValid(true);user.setUserId(1);user.setWritableFolders("*");request.getSession().setAttribute(Constants.USER_KEY,user);
        RequestBean bean=new RequestBean();bean.setRequest(request);bean.setDomain(DOMAIN);
        try(var lifecycle=mockStatic(SetCharacterEncodingFilter.class)) {
            lifecycle.when(SetCharacterEncodingFilter::getCurrentRequestBean).thenReturn(bean);
            lifecycle.when(SetCharacterEncodingFilter::getEncoding).thenReturn("UTF-8");
            try(var scope=BranchContexts.open(request,context)) {return callback.apply(request);}
        }
    }
    private String fingerprint()throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        try(Connection connection=DBPool.getConnection();Statement statement=connection.createStatement()) {
            for(String table:List.of("documents","documents_history","media","media_group_to_media","file_atr","dirprop","gallery","url_redirect","file_history")) {
                List<String> values=new ArrayList<>();
                try(ResultSet rows=statement.executeQuery("SELECT * FROM `"+table+"`")) {
                    while(rows.next()) {StringBuilder value=new StringBuilder();for(int i=1;i<=rows.getMetaData().getColumnCount();i++)value.append(Objects.toString(rows.getString(i),"<null>")).append('\u0000');values.add(value.toString());}
                }
                Collections.sort(values);for(String value:values)digest.update(value.getBytes(StandardCharsets.UTF_8));
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
    @FunctionalInterface private interface Call<T> {T apply(MockHttpServletRequest request)throws Exception;}
}
