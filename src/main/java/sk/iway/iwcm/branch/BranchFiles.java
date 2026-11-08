package sk.iway.iwcm.branch;

import java.io.IOException;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.sql.*;
import jakarta.servlet.http.*;
import sk.iway.iwcm.*;
import sk.iway.iwcm.filebrowser.EditForm;
import sk.iway.iwcm.users.UsersDB;

/** Narrow read adapter for local /images and /files; staging names never come from a client. */
public final class BranchFiles {
    private BranchFiles() { }

    public static Path workPath(long branchId, long rowId) {
        if (branchId<=0 || rowId<=0) throw new IllegalArgumentException("Invalid staging identity");
        Path root=Path.of(Constants.getString("branchStorageRoot")).toAbsolutePath().normalize();
        return root.resolve(Long.toString(branchId)).resolve(Long.toString(rowId)).resolve("content");
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
