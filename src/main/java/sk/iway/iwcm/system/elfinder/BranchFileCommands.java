package sk.iway.iwcm.system.elfinder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.fileupload2.core.FileItem;
import org.json.JSONObject;
import cn.bluejoe.elfinder.controller.executor.AbstractJsonCommandExecutor;
import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsService;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.branch.BranchFiles;
import sk.iway.iwcm.common.FileBrowserTools;

/** Explicit branch commands run before legacy upload, redirect, gallery and metadata hooks. */
public class BranchFileCommands extends AbstractJsonCommandExecutor {
    public static final Set<String> WRITES=Set.of("mkdir","upload","rename","paste","rm");
    public static final Set<String> READS=Set.of("open","tree","parents","ls","info","size","dim","file","tmb","search");

    @Override protected void execute(FsService service,HttpServletRequest request,ServletContext servletContext,JSONObject json) {
        try {
            String cmd=request.getParameter("cmd");
            if("mkdir".equals(cmd)) {
                FsItemEx parent=item(service,request.getParameter("target"));
                if(request.getParameterValues("dirs[]")!=null) throw new IllegalArgumentException("Create one empty directory at a time in a publishing branch");
                String name=name(request.getParameter("name"),parent);
                FsItemEx directory=new FsItemEx(parent,name);
                BranchFiles.mkdir(directory.getPath(),request);
                json.put("added",new Object[]{getFsItemInfo(request,directory)});
            } else if("upload".equals(cmd)) upload(service,request,json);
            else if("rename".equals(cmd)) {
                FsItemEx source=item(service,request.getParameter("target"));
                FsItemEx destination=new FsItemEx(source.getParent(),name(request.getParameter("name"),source));
                if(source.getPath().equals(destination.getPath())) throw new IllegalArgumentException("Choose a different file name");
                requireFile(source);BranchFiles.move(source.getPath(),destination.getPath(),request);
                json.put("added",new Object[]{getFsItemInfo(request,destination)});json.put("removed",new String[]{source.getHash()});
            } else if("paste".equals(cmd)) {
                if(!"1".equals(request.getParameter("cut"))) throw new IllegalArgumentException("File copying is unavailable in a publishing branch; use a single file move");
                FsItemEx source=item(service,single(request.getParameterValues("targets[]")));
                FsItemEx folder=item(service,request.getParameter("dst"));
                if(!folder.isFolder()) throw new IllegalArgumentException("The destination must be a directory");
                requireFile(source);FsItemEx destination=new FsItemEx(folder,source.getName());
                if(source.getPath().equals(destination.getPath())) throw new IllegalArgumentException("The file is already in this directory");
                BranchFiles.move(source.getPath(),destination.getPath(),request);
                json.put("added",new Object[]{getFsItemInfo(request,destination)});json.put("removed",new String[]{source.getHash()});
            } else if("rm".equals(cmd)) {
                FsItemEx source=item(service,single(request.getParameterValues("targets[]")));
                requireFile(source);BranchFiles.delete(source.getPath(),request);json.put("removed",new String[]{source.getHash()});
            } else throw new IllegalArgumentException("This file operation is unavailable in a publishing branch");
        } catch(Exception ex) {json.put("error",Tools.isNotEmpty(ex.getMessage())?ex.getMessage():"The branch file operation failed");}
    }

    @SuppressWarnings("unchecked")
    private void upload(FsService service,HttpServletRequest request,JSONObject json)throws Exception {
        if(request.getParameter("cid")!=null||request.getParameter("chunk")!=null||request.getParameterValues("renames[]")!=null)
            throw new IllegalArgumentException("Chunk uploads and automatic conflict renames are unavailable here; use the administration upload dialog");
        Map<String,FileItem<?>> files=(Map<String,FileItem<?>>)request.getAttribute("MultipartWrapper.files");
        if(files==null||files.size()!=1) throw new IllegalArgumentException("Upload one file at a time in a publishing branch");
        FsItemEx target=item(service,request.getParameter("target"));
        List<FsItemEx> added=new ArrayList<>();
        for(Map.Entry<String,FileItem<?>> entry:files.entrySet()) {
            FileItem<?> upload=entry.getValue();
            try {
                String raw=entry.getKey();
                if(raw.indexOf('/')>=0||raw.indexOf('\\')>=0) throw new IllegalArgumentException("Directory uploads are unavailable in a publishing branch");
                String fileName=name(raw,target);
                if(!sk.iway.iwcm.system.elfinder.FsService.isFileTypeAllowed(target.getFsServiceSelectedType(),fileName)) throw new IllegalArgumentException("File type is not allowed in this picker");
                FsItemEx destination=target.isFolder()?new FsItemEx(target,fileName):target;
                if(!destination.isWritable(destination)) throw new IllegalArgumentException("No write permission for the upload destination");
                if(!sk.iway.iwcm.system.elfinder.FsService.isFileTypeAllowed(target.getFsServiceSelectedType(),destination.getPath())) throw new IllegalArgumentException("File type is not allowed in this picker");
                Path temporary=Files.createTempFile("webjet-branch-upload-",".tmp");
                try(var stream=upload.getInputStream()) {
                    Files.copy(stream,temporary,java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    BranchFiles.put(destination.getPath(),temporary,request);added.add(destination);
                } finally {Files.deleteIfExists(temporary);}
            } finally {upload.delete();}
        }
        json.put("added",files2JsonArray(request,added));
    }

    private FsItemEx item(FsService service,String hash)throws IOException {
        FsItemEx item=findItem(service,hash);
        if(item==null||!(item.getVolume() instanceof BranchFsVolume)||!item.isWritable(item))
            throw new IllegalArgumentException("This location is read-only in a publishing branch");
        return item;
    }
    private String name(String value,FsItemEx item)throws IOException {
        if(Tools.isEmpty(value)||value.contains("/")||value.contains("\\")||FileBrowserTools.hasForbiddenSymbol(value))
            throw new IllegalArgumentException("Invalid file name");
        Identity user=sk.iway.iwcm.system.elfinder.FsService.getCurrentUser();
        String result=IwcmFsVolume.removeSpecialChars(value,item,user);
        if(Tools.isEmpty(result)) throw new IllegalArgumentException("Invalid file name");
        return result;
    }
    private String single(String[] targets) {
        if(targets==null||targets.length!=1) throw new IllegalArgumentException("Select one file in a publishing branch");
        return targets[0];
    }
    private void requireFile(FsItemEx item) {
        if(item.isFolder()) throw new IllegalArgumentException("Renaming, moving and deleting directories is unavailable in a publishing branch");
    }
}
