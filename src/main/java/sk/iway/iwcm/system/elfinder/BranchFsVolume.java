package sk.iway.iwcm.system.elfinder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsItem;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.branch.BranchFiles;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmInputStream;
import sk.iway.iwcm.io.IwcmOutputStream;

/** Uses logical elFinder identities while resolving each file through the selected branch. */
public class BranchFsVolume extends IwcmFsVolume {
    public BranchFsVolume(String name) { super(name,"/"); }
    private String logical(FsItem item) { return asFile(item).getVirtualPath(); }
    private boolean editorial(String path) { return path.startsWith("/images/")||path.startsWith("/files/"); }
    @Override public boolean exists(FsItem item) {
        String path=logical(item);
        return isFolder(item)||(editorial(path)&&BranchFiles.exists(path));
    }
    @Override public boolean isFolder(FsItem item) {
        String path=logical(item);
        return (path.equals("/")||path.equals("/images")||path.equals("/files")||editorial(path))&&BranchFiles.directoryExists(path);
    }
    @Override public String getMimeType(FsItem item) {
        if(isFolder(item)) return "directory";
        return super.getMimeType(item);
    }
    @Override public FsItem[] listChildren(FsItem item) {
        List<FsItem> result=new ArrayList<>();
        Identity user=FsService.getCurrentUser();
        if(user==null||!isFolder(item)) return new FsItem[0];
        for(String path:BranchFiles.list(logical(item))) {
            boolean visible=!Constants.getBoolean("fbrowserShowOnlyWritableFolders")||user.isFolderWritable(path);
            if(!visible) for(IwcmFile writable:user.getWritableFoldersList()) {
                String folder=writable.getVirtualPath().replace("*","").replace("+","");
                if(folder.startsWith(path+"/")) {visible=true;break;}
            }
            if(visible) result.add(fromFile(new IwcmFile(Tools.getRealPath(path))));
        }
        return result.toArray(new FsItem[0]);
    }
    @Override public boolean hasChildFolder(FsItem item) {
        for(FsItem child:listChildren(item)) if(isFolder(child)) return true;
        return false;
    }
    @Override public long getSize(FsItem item) {
        if(isFolder(item)) return 0;
        Path source=BranchFiles.read(logical(item));
        try {return source==null?0:Files.size(source);} catch(IOException ex) {throw new IllegalStateException(ex);}
    }
    @Override public long getLastModified(FsItem item) {
        if(isFolder(item)) return super.getLastModified(item);
        Path source=BranchFiles.read(logical(item));
        try {return source==null?0:Files.getLastModifiedTime(source).toMillis()/1000;} catch(IOException ex) {throw new IllegalStateException(ex);}
    }
    @Override public IwcmInputStream openInputStream(FsItem item)throws IOException {
        Path source=BranchFiles.read(logical(item));
        if(source==null) throw new IOException("The working file does not exist");
        return new IwcmInputStream(source.toFile());
    }
    // Mutations must go through BranchFileCommands, which validates the whole command before hooks.
    @Override public void createFile(FsItem item)throws IOException {throw unsupported();}
    @Override public void createFolder(FsItem item,FsItemEx info)throws IOException {throw unsupported();}
    @Override public boolean deleteFile(FsItem item)throws IOException {throw unsupported();}
    @Override public boolean deleteFolder(FsItem item)throws IOException {throw unsupported();}
    @Override public IwcmOutputStream openOutputStream(FsItem item)throws IOException {throw unsupported();}
    @Override public void rename(FsItem from,FsItem to)throws IOException {throw unsupported();}
    private IOException unsupported() {return new IOException("Use the supported publishing branch file commands");}
}
