package sk.iway.iwcm.branch;

import java.io.IOException;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.sql.*;
import jakarta.servlet.http.*;
import sk.iway.iwcm.*;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.filebrowser.EditForm;
import sk.iway.iwcm.users.UsersDB;

/** Explicit local file overlay; logical paths retain their permissions and staging identities are server-owned. */
public final class BranchFiles {
    private BranchFiles() { }

    public static Path workPath(long branchId, long rowId) {
        if (branchId<=0 || rowId<=0) throw new IllegalArgumentException("Invalid staging identity");
        Path root=Path.of(Constants.getString("branchStorageRoot")).toAbsolutePath().normalize();
        return root.resolve(Long.toString(branchId)).resolve(Long.toString(rowId)).resolve("content");
    }

    /** Lists the effective direct children, including empty directory markers and implied parents. */
    public static List<String> list(String directory) {
        String path=directoryPath(directory);
        String prefix=path.equals("/") ? "/" : path+"/";
        Set<String> result=new TreeSet<>();
        Path live=Path.of(Tools.getRealPath(path));
        if(Files.isDirectory(live)) try(var children=Files.list(live)) {
            children.forEach(child -> result.add(prefix+child.getFileName()));
        } catch(IOException ex) { throw new IllegalStateException("Cannot list LIVE directory",ex); }
        if(BranchContexts.current().isBranch()) for(Map<String,Object> row:BranchView.current().workRows(BranchType.FILE)) {
            String logical=(String)row.get("logical_path");
            if(!logical.startsWith(prefix) || !BranchView.current().selectWork(BranchType.FILE,row,liveSnapshot(logical))) continue;
            String rest=logical.substring(prefix.length()); int slash=rest.indexOf('/');
            if(slash<0) {
                if("DELETE".equals(row.get("operation"))) result.remove(logical);
                else result.add(logical);
            } else if(!"DELETE".equals(row.get("operation"))) result.add(prefix+rest.substring(0,slash));
        }
        if(path.equals("/")) result.removeIf(child -> !child.equals("/images")&&!child.equals("/files"));
        return new ArrayList<>(result);
    }

    public static boolean directoryExists(String directory) {
        String path=directoryPath(directory);
        if(BranchContexts.current().isBranch()) {
            for(Map<String,Object> row:BranchView.current().workRows(BranchType.FILE)) {
                String logical=(String)row.get("logical_path");
                if(path.equals(logical)&&BranchView.current().selectWork(BranchType.FILE,row,liveSnapshot(logical)))
                    return !"DELETE".equals(row.get("operation"))&&"DIRECTORY".equals(row.get("entry_kind"));
            }
        }
        if(Files.isDirectory(Path.of(Tools.getRealPath(path)))) return true;
        if(BranchContexts.current().isBranch()) for(Map<String,Object> row:BranchView.current().workRows(BranchType.FILE)) {
            String logical=(String)row.get("logical_path");
            if(logical.startsWith(path+"/")&&!"DELETE".equals(row.get("operation"))&&BranchView.current().selectWork(BranchType.FILE,row,liveSnapshot(logical))) return true;
        }
        return false;
    }

    private static String directoryPath(String path) {
        if(Set.of("/","/images","/files").contains(path)) return path;
        return BranchFilePaths.normalize(path);
    }

    /** Stores bytes in private staging; the caller retains ownership of the upload source. */
    public static void put(String logicalPath, Path source, HttpServletRequest request) throws IOException {
        put(logicalPath,source,request,logicalPath!=null&&logicalPath.startsWith("/images/")?"image":"file");
    }

    /** Keeps the original upload entry point's type and size policy. */
    public static void put(String logicalPath,Path source,HttpServletRequest request,String uploadType)throws IOException {
        String path=authorize(logicalPath,request,false);
        validateUpload(path,source,request,uploadType);
        if(directoryExists(path)) throw new IllegalArgumentException("A directory already occupies the target path");
        transact(files -> {
            put(files,path,source,request);
        });
    }

    /** Persists an empty directory without creating a LIVE directory or dirprop row. */
    public static void mkdir(String logicalPath,HttpServletRequest request) throws IOException {
        String path=authorize(logicalPath,request,true);
        if(read(path)!=null) throw new IllegalArgumentException("A file already occupies the target path");
        transact(files -> {
            new BranchStore().putFile(files.connection,BranchContexts.current(),path,"DIRECTORY",liveSnapshot(path),null,null,null,false);
        });
    }

    /** Removes only an empty WORK marker; it never deletes the corresponding LIVE directory. */
    public static void removeDirectoryMarker(String logicalPath,HttpServletRequest request)throws IOException {
        String path=authorize(logicalPath,request,true);
        transact(files -> {
            BranchContext context=BranchContexts.current();
            Map<String,Object> row=BranchStore.readWork(files.connection,context.branchId(),BranchType.FILE,path).orElseThrow(()->new IllegalArgumentException("No working directory marker"));
            if(!"DIRECTORY".equals(row.get("entry_kind"))) throw new IllegalArgumentException("The working entry is not a directory marker");
            try(PreparedStatement statement=files.connection.prepareStatement("SELECT logical_path FROM branch_files WHERE branch_id=?")) {
                statement.setLong(1,context.branchId());
                try(ResultSet rows=statement.executeQuery()) {
                    while(rows.next()) if(rows.getString(1).startsWith(path+"/")) throw new IllegalArgumentException("Remove dependent working entries before removing this directory marker");
                }
            }
            try(PreparedStatement statement=files.connection.prepareStatement("DELETE FROM branch_files WHERE row_id=?")) {
                statement.setLong(1,((Number)row.get("row_id")).longValue());statement.executeUpdate();
            }
        });
    }

    /** Deletes a single effective file; directories and recursive operations are unsupported. */
    public static void delete(String logicalPath,HttpServletRequest request) throws IOException {
        String path=authorize(logicalPath,request,false);
        if(directoryExists(path)) throw new IllegalArgumentException("Directory deletion is unavailable in a publishing branch");
        if(read(path)==null) throw new IllegalArgumentException("The file does not exist");
        transact(files -> delete(files,path));
    }

    /** Moves one file in a single WORK transaction, without redirects or metadata hooks. */
    public static void move(String source,String destination,HttpServletRequest request) throws IOException {
        String from=authorize(source,request,false),to=authorize(destination,request,false);
        if(from.equals(to)) return;
        if(directoryExists(from)) throw new IllegalArgumentException("Directory moves are unavailable in a publishing branch");
        Path bytes=read(from);
        if(bytes==null) throw new IllegalArgumentException("The source file does not exist");
        if(directoryExists(to)||read(to)!=null) throw new IllegalArgumentException("The destination already exists");
        validateUpload(to,bytes,request,to.startsWith("/images/")?"image":"file");
        transact(files -> { put(files,to,bytes,request);delete(files,from); });
    }

    private static String authorize(String logicalPath,HttpServletRequest request,boolean directory) {
        String path=BranchFilePaths.normalize(logicalPath);
        if(sk.iway.iwcm.common.FileBrowserTools.hasForbiddenSymbol(path)) throw new IllegalArgumentException("Invalid logical file path");
        BranchContext context=BranchContexts.current();
        BranchAccess access=new BranchAccess();
        Identity user=access.requireUser(request);access.requireCsrf(request);
        if(!context.isBranch()||context.mode()!=BranchContext.Mode.ADMIN||context.userId()!=user.getUserId())
            throw new IllegalStateException("An explicit ADMIN branch context is required");
        String folder=directory?path:path.substring(0,path.lastIndexOf('/'));
        if(!user.isFolderWritable(folder))
            throw new IllegalArgumentException("No write permission for the logical destination");
        if(sk.iway.iwcm.doc.PlannedAssetPaths.containsPlannedPath(path+"/"))
            throw new IllegalArgumentException("Individually planned file storage is read-only in a publishing branch");
        if(sk.iway.iwcm.gallery.GalleryDB.isGalleryFolder(folder))
            throw new IllegalArgumentException("Gallery paths and image variants are read-only in a publishing branch");
        String archive=sk.iway.iwcm.components.file_archiv.FileArchivSupportMethodsService.normalizePath(sk.iway.iwcm.components.file_archiv.FileArchivatorKit.getArchivPath());
        if(Tools.isNotEmpty(archive)&&(path+"/").toLowerCase(Locale.ROOT).startsWith(archive.toLowerCase(Locale.ROOT)))
            throw new IllegalArgumentException("File archive paths are read-only in a publishing branch");
        if(sk.iway.iwcm.io.IwcmFsDB.useDBStorage(path))
            throw new IllegalArgumentException("Database file storage is unavailable in a publishing branch");
        for(String ancestor=folder;ancestor.length()>7;ancestor=ancestor.substring(0,ancestor.lastIndexOf('/'))) {
            if(fileIdentity(ancestor)) throw new IllegalArgumentException("A file path cannot be used as a directory in a publishing branch");
        }
        return path;
    }

    /** A tombstone hides bytes but does not authorize changing a file identity into a directory. */
    private static boolean fileIdentity(String path) {
        if("FILE".equals(liveSnapshot(path).fields().get("entry_kind"))) return true;
        for(Map<String,Object> row:BranchView.current().workRows(BranchType.FILE)) {
            if(!path.equals(row.get("logical_path"))) continue;
            return "FILE".equals(row.get("entry_kind"))
                ||"FILE".equals(BranchSnapshot.fromJson((String)row.get("base_snapshot")).fields().get("entry_kind"));
        }
        return false;
    }

    private static void validateUpload(String path,Path source,HttpServletRequest request,String type)throws IOException {
        String name=path.substring(path.lastIndexOf('/')+1);
        Identity user=UsersDB.getCurrentUser(request);
        if(!Files.isRegularFile(source)) throw new IllegalArgumentException("The upload source is not a regular file");
        if(FileTools.isFileTypeForbiddenForUpload(name)) throw new IllegalArgumentException("The upload file extension is forbidden");
        if(!sk.iway.iwcm.common.UploadFileTools.isFileAllowed(type,name,Files.size(source),user,request))
            throw new IllegalArgumentException("File type or size is not allowed: "+Objects.toString(request.getAttribute("permissionDenied"),"unknown"));
    }

    private static void delete(FileChanges files,String path)throws SQLException,IOException {
        Connection connection=files.connection;
        BranchContext context=BranchContexts.current();
        Map<String,Object> old=BranchStore.readWork(connection,context.branchId(),BranchType.FILE,path).orElse(null);
        BranchSnapshot base=old==null?liveSnapshot(path):BranchSnapshot.fromJson((String)old.get("base_snapshot"));
        if(base.state()==BranchSnapshot.State.ABSENT) {
            try(PreparedStatement statement=connection.prepareStatement("DELETE FROM branch_files WHERE branch_id=? AND logical_path=?")) {
                statement.setLong(1,context.branchId());statement.setString(2,path);statement.executeUpdate();
            }
        } else {
            Map<String,Object> row=new BranchStore().putFile(connection,context,path,"FILE",base,null,null,null,true);
            if(old==null) {
                Path target=workPath(context.branchId(),((Number)row.get("row_id")).longValue());
                Files.createDirectories(target.getParent());
                if(!target.getParent().toRealPath().startsWith(Path.of(Constants.getString("branchStorageRoot")).toRealPath())) throw new IOException("Working path escapes private storage");
                Files.copy(Path.of(Tools.getRealPath(path)),target.resolveSibling("base"),StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void put(FileChanges files,String path,Path source,HttpServletRequest request)throws SQLException,IOException {
        BranchContext context=BranchContexts.current();
        Map<String,Object> old=BranchStore.readWork(files.connection,context.branchId(),BranchType.FILE,path).orElse(null);
        BranchSnapshot base=old==null?liveSnapshot(path):BranchSnapshot.fromJson((String)old.get("base_snapshot"));
        Map<String,Object> row=new BranchStore().putFile(files.connection,context,path,"FILE",base,hash(source),Files.size(source),request.getServletContext().getMimeType(path),false);
        Path target=workPath(context.branchId(),((Number)row.get("row_id")).longValue());
        Files.createDirectories(target.getParent());
        if(!target.getParent().toRealPath().startsWith(Path.of(Constants.getString("branchStorageRoot")).toRealPath()))
            throw new IOException("Working path escapes private storage");
        // Keep the original binary for the later human conflict comparison.
        Path original=target.resolveSibling("base");
        if(old==null&&base.state()!=BranchSnapshot.State.ABSENT) Files.copy(Path.of(Tools.getRealPath(path)),original,StandardCopyOption.REPLACE_EXISTING);
        Path backup=Files.exists(target)?Files.createTempFile(target.getParent(),"previous-",".tmp"):null;
        if(backup!=null) Files.copy(target,backup,StandardCopyOption.REPLACE_EXISTING);
        files.replaced.add(new Path[]{target,backup});
        Path pending=Files.createTempFile(target.getParent(),"upload-",".tmp");
        try { Files.copy(source,pending,StandardCopyOption.REPLACE_EXISTING);Files.move(pending,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
        finally { Files.deleteIfExists(pending); }
    }

    @FunctionalInterface private interface FileOperation { void apply(FileChanges changes)throws SQLException,IOException; }
    private static final class FileChanges {
        private Connection connection;
        private final List<Path[]> replaced=new ArrayList<>();
    }
    private static void transact(FileOperation operation)throws IOException {
        FileChanges changes=new FileChanges();boolean complete=false;
        try {
            new BranchStore().write(BranchContexts.current(),connection -> {
                changes.connection=connection;
                try {operation.apply(changes);return null;}
                catch(IOException ex) {throw new java.io.UncheckedIOException(ex);}
            });
            complete=true;BranchView.invalidate();
        } catch(java.io.UncheckedIOException ex) {throw ex.getCause();}
        catch(SQLException ex) {throw new IOException("Cannot persist working file",ex);}
        finally {
            Collections.reverse(changes.replaced);
            for(Path[] replacement:changes.replaced) {
                if(!complete) {
                    if(replacement[1]==null) Files.deleteIfExists(replacement[0]);
                    else Files.move(replacement[1],replacement[0],StandardCopyOption.REPLACE_EXISTING);
                }
                if(replacement[1]!=null) Files.deleteIfExists(replacement[1]);
            }
        }
    }

    public static String hash(Path path) throws IOException {
        try (var stream=Files.newInputStream(path)) {
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            byte[] buffer=new byte[16384]; int count;
            while ((count=stream.read(buffer))!=-1) digest.update(buffer,0,count);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }

    public static BranchSnapshot liveSnapshot(String logicalPath) {
        String path=BranchFilePaths.normalize(logicalPath);
        Path live=Path.of(Tools.getRealPath(path));
        try {
            if (!Files.exists(live)) return BranchSnapshot.absent();
            return BranchSnapshot.file(BranchSnapshot.State.ACTIVE,path,Files.isDirectory(live)?"DIRECTORY":"FILE",Files.isDirectory(live)?null:hash(live));
        } catch(IOException ex) { throw new IllegalStateException("Cannot inspect LIVE file",ex); }
    }

    /** null is an effective DELETE, never an invitation to retry against LIVE. */
    public static Path read(String logicalPath) {
        String path=BranchFilePaths.normalize(logicalPath);
        if(sk.iway.iwcm.common.FileBrowserTools.hasForbiddenSymbol(path)) throw new IllegalArgumentException("Invalid logical file path");
        BranchContext context=BranchContexts.current();
        if (context.isBranch()) {
            try {
                Optional<Map<String,Object>> stored=BranchView.current().workRows(BranchType.FILE).stream().filter(row->path.equals(row.get("logical_path"))).findFirst();
                if(stored.isPresent()) {
                    Map<String,Object> row=stored.get();
                    boolean work=BranchView.current().selectWork(BranchType.FILE,row,liveSnapshot(path));
                    if(work) {
                        if("DELETE".equals(row.get("operation"))) return null;
                        Path bytes=workPath(context.branchId(),((Number)row.get("row_id")).longValue());
                        if("DIRECTORY".equals(row.get("entry_kind"))) return null;
                        Path real=bytes.toRealPath();
                        Path root=Path.of(Constants.getString("branchStorageRoot")).toRealPath();
                        if(!real.startsWith(root) || !hash(real).equals(row.get("work_content_hash")))
                            throw new IllegalStateException("Missing or corrupt working file");
                        return real;
                    }
                }
            } catch(IOException ex) { throw new IllegalStateException("Cannot read working file",ex); }
        }
        Path live=Path.of(Tools.getRealPath(path));
        return Files.isRegularFile(live)?live:null;
    }

    /**
     * Checks the selected read result, including WORK-only files and DELETE without LIVE fallback.
     * Kept separate from Tools.getRealPath because that resolver is also used for write destinations.
     */
    public static boolean exists(String logicalPath) {
        if(!BranchContexts.current().isBranch() || logicalPath==null || !(logicalPath.startsWith("/images/")||logicalPath.startsWith("/files/")))
            return new sk.iway.iwcm.io.IwcmFile(Tools.getRealPath(logicalPath)).exists();
        return read(logicalPath)!=null;
    }

    public static void serve(String logicalPath,HttpServletRequest request,HttpServletResponse response) throws IOException {
        if(!sk.iway.iwcm.doc.PlannedAccess.isPathAccessible(logicalPath,request,response))return;
        EditForm protection=PathFilter.isPasswordProtected(logicalPath,request,request.getSession());
        if(protection!=null && !protection.isAccessibleFor(UsersDB.getCurrentUser(request))) {response.sendError(403);return;}
        Path file=read(logicalPath);
        if(file==null) {response.sendError(404);return;}
        String mime=request.getServletContext().getMimeType(logicalPath);
        response.setContentType(mime==null?"application/octet-stream":mime);
        response.setHeader("X-Content-Type-Options","nosniff");
        if(!"HEAD".equals(request.getMethod())) Files.copy(file,response.getOutputStream());
    }

    /** Uses the core pixel transformer without gallery writes, in a private branch/content namespace. */
    public static void thumbnail(String logicalPath,HttpServletRequest request,HttpServletResponse response) throws IOException {
        boolean content=logicalPath.startsWith("/images/")||logicalPath.startsWith("/files/");
        String path=content?BranchFilePaths.normalize(logicalPath):logicalPath;
        if(!content && !(path.startsWith("/templates/")&&BranchPreviewUrls.allowed(path))) {response.sendError(404);return;}
        if(!sk.iway.iwcm.doc.PlannedAccess.isPathAccessible(path,request,response))return;
        EditForm protection=PathFilter.isPasswordProtected(path,request,request.getSession());
        if(protection!=null && !protection.isAccessibleFor(UsersDB.getCurrentUser(request))) {response.sendError(403);return;}
        Path source=content?read(path):Path.of(Tools.getRealPath(path));
        if(source!=null&&!Files.isRegularFile(source))source=null;
        if(source==null) {response.sendError(404);return;}
        int width=Tools.getIntValue(request.getParameter("w"),0),height=Tools.getIntValue(request.getParameter("h"),0);
        if(width<0||height<0||width>4096||height>4096||(width==0&&height==0)) {response.sendError(400,"Invalid thumbnail dimensions");return;}
        // Other transformations are rejected until the existing transformer has an explicit adapter.
        for(String option:List.of("ip","c","q","noip")) if(request.getParameter(option)!=null) {response.sendError(400,"Unsupported preview thumbnail option: "+option);return;}
        String ext=FileTools.getFileExtension(path).toLowerCase(java.util.Locale.ROOT);
        if(!List.of("png","jpg","jpeg","gif","webp").contains(ext)) {response.sendError(415);return;}
        Path target=Path.of(Constants.getString("branchStorageRoot")).resolve("thumbs").resolve(Long.toString(BranchContexts.current().branchId()))
            .resolve(hash(source)+"-"+width+"x"+height+"."+ext);
        Files.createDirectories(target.getParent());
        if(!Files.isRegularFile(target)) {
            BufferedImageSize size=imageSize(source);
            double scale=Math.min(width==0?Double.POSITIVE_INFINITY:(double)width/size.width(),height==0?Double.POSITIVE_INFINITY:(double)height/size.height());
            int finalWidth=Math.max(1,(int)Math.round(size.width()*scale)),finalHeight=Math.max(1,(int)Math.round(size.height()*scale));
            // The legacy Java transformer derives its output codec from the input suffix.
            Path typedSource=Files.createTempFile(target.getParent(),"source-","."+ext);
            try {
                Files.copy(source,typedSource,StandardCopyOption.REPLACE_EXISTING);
                int status=sk.iway.iwcm.common.GalleryDBTools.cropAndResize(new sk.iway.iwcm.io.IwcmFile(typedSource.toString()),0,0,0,0,finalWidth,finalHeight,null,true,new sk.iway.iwcm.io.IwcmFile(target.toString()),-1,-1);
                if(status!=0) {Files.deleteIfExists(target);throw new IOException("Thumbnail transformation failed: "+status);}
            } finally {Files.deleteIfExists(typedSource);}
            if(!Files.isRegularFile(target)) throw new IOException("Thumbnail generation failed");
        }
        response.setContentType("image/"+(ext.equals("jpg")?"jpeg":ext));
        if(!"HEAD".equals(request.getMethod())) Files.copy(target,response.getOutputStream());
    }
    private record BufferedImageSize(int width,int height) { }
    private static BufferedImageSize imageSize(Path source)throws IOException {
        try(var input=javax.imageio.ImageIO.createImageInputStream(source.toFile())) {
            var readers=javax.imageio.ImageIO.getImageReaders(input);
            if(!readers.hasNext())throw new IOException("Unsupported preview image format");
            var reader=readers.next();try {reader.setInput(input);return new BufferedImageSize(reader.getWidth(0),reader.getHeight(0));}finally {reader.dispose();}
        }
    }
}
