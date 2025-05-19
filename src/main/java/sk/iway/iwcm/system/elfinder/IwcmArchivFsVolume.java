package sk.iway.iwcm.system.elfinder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;

import cn.bluejoe.elfinder.service.FsItem;
import cn.bluejoe.elfinder.service.FsVolume;
import cn.bluejoe.elfinder.util.MimeTypesUtils;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.file_archiv.FileArchivatorBean;
import sk.iway.iwcm.components.file_archiv.FileArchivatorDB;
import sk.iway.iwcm.components.file_archiv.FileArchivatorKit;
import sk.iway.iwcm.components.file_archiv.FileArchivatorSearchBean;
import sk.iway.iwcm.io.IwcmFile;

public class IwcmArchivFsVolume implements FsVolume {

    public static final String VOLUME_ID = "iwcm_archiv_volume";

    protected String _name;

    public IwcmArchivFsVolume(String name)
    {
        this._name = name;
    }

    @Override
    public void createFile(FsItem fsi) throws IOException {
        throw new NotImplementedException();
    }

    @Override
    public void createFolder(FsItem fsi) throws IOException {
        throw new NotImplementedException();
    }

    @Override
    public boolean deleteFile(FsItem fsi) throws IOException {

        return false;
    }

    @Override
    public boolean deleteFolder(FsItem fsi) throws IOException {
        return false;
    }

    @Override
    public boolean exists(FsItem newFile) {
        return false;
    }

    @Override
    public FsItem fromPath(String relativePath) {

        IwcmFile iwFile = new IwcmFile(Tools.getRealPath(relativePath));

        //Logger.debug(this, "iwFile.fromPath("+relativePath+"): ");
        return new IwcmArchivItem(this, iwFile.getVirtualPath(),FileArchivatorDB.getByUrl(relativePath));

    }

    @Override
    public String getDimensions(FsItem fsi) {
        return null;
    }

    @Override
    public long getLastModified(FsItem fsi) {
        IwcmArchivItem item = asArchivGroup(fsi);

        if (item.getFab() != null)
            return  Math.round((double)item.getFab().getDateInsert().getTime() / 1000);
        if(Tools.isNotEmpty(item.getPath()) )
        {
            IwcmFile iwFile = new IwcmFile(Tools.getRealPath(item.getPath()));
            return Math.round((double)iwFile.lastModified() / 1000);
        }

        return 0;
    }

    @Override
    public String getMimeType(FsItem fsi) {
        IwcmArchivItem item = asArchivGroup(fsi);
        if (item.getPath()!=null && item.getPath().indexOf(".") == -1)
            return "directory";

        String ext = FileArchivatorKit.getFileExtension(item.getPath(),true);
        if (ext != null && !ext.isEmpty())
        {
            String mimeType = MimeTypesUtils.getMimeType(ext);
            return mimeType == null ? MimeTypesUtils.UNKNOWN_MIME_TYPE : mimeType;
        }

        return MimeTypesUtils.UNKNOWN_MIME_TYPE;
    }

    @Override
    public String getName() {

        return _name;
    }

    @Override
    public String getName(FsItem fsi) {
        IwcmArchivItem item = asArchivGroup(fsi);
        return item.getName();
    }

    @Override
    public FsItem getParent(FsItem fsi) {
        IwcmArchivItem item = asArchivGroup(fsi);
        //Logger.debug(this, "getParent(): "+asArchivGroup(fsi).getPath()+" fab: "+(item.getFab() != null ? item.getFab().getId():"-"));
        if(item.getFab() != null)
        {
            String path = "";
            if(Tools.isNotEmpty(item.getFab().getFieldA()))//kategoria 2 existuje (a 1 by mala tiez)
            {
                path = asArchivGroup(getRoot()).getPath()+"/"+item.getFab().getCategory()+"/"+item.getFab().getFieldA();
                //Logger.debug(this, "getParent() fab return : " + path );
                return  new IwcmArchivItem(((IwcmArchivFsVolume) fsi.getVolume()), path, null);
            }
            else if(Tools.isEmpty(item.getFab().getFieldA()) && Tools.isNotEmpty(item.getFab().getCategory()))//kategoria 2
            {
                path = asArchivGroup(getRoot()).getPath()+"/"+item.getFab().getCategory();
                //Logger.debug(this, "getParent() fab return : " + path );
                return  new IwcmArchivItem(((IwcmArchivFsVolume) fsi.getVolume()), path, null);
            }
        }
        if(item != null && item.getPath() != null)
        {
            String itemPath = item.getPath();
            if(itemPath.indexOf(".") == -1 && !itemPath.endsWith("/"))
                itemPath += "/";

            String pathWihoutRoot = removeRootFrompath(itemPath);
            if(Tools.isNotEmpty(pathWihoutRoot))
            {
                int indexOfSlash = pathWihoutRoot.indexOf("/");
                int lastIndexOfSlash = pathWihoutRoot.lastIndexOf("/");
                //Logger.debug(this, "getParent() pathWihoutRoot : " + pathWihoutRoot + " [" + indexOfSlash + "," + lastIndexOfSlash + "]");
                if(!pathWihoutRoot.endsWith("/") && pathWihoutRoot.indexOf(".") != -1)
                {
                    String vp = "/"+FileArchivatorKit.getArchivPath()+pathWihoutRoot.substring(0,lastIndexOfSlash+1);
                    if(Tools.isNotEmpty(vp) && vp.endsWith("/"))
                    {
                        vp = vp.substring(0,vp.length()-1);
                    }
                    //vratim retazec medzi archiv "rootom" a poslednou lomkou.
                    return new IwcmArchivItem(((IwcmArchivFsVolume) fsi.getVolume()), vp, FileArchivatorDB.getByUrl(vp));
                }
                else if(indexOfSlash != -1 && lastIndexOfSlash != -1 && lastIndexOfSlash != indexOfSlash )
                {
                    String vp = "/"+FileArchivatorKit.getArchivPath()+pathWihoutRoot.substring(0,pathWihoutRoot.lastIndexOf("/",pathWihoutRoot.length()-2)+1 /*lastIndexOfSlash+1*/);
                    if(Tools.isNotEmpty(vp) && vp.endsWith("/"))
                    {
                        vp = vp.substring(0,vp.length()-1);
                    }
                    //Logger.debug(this, "getParent() priecinok return : " + vp);
                    //vratim retazec medzi archiv "rootom" a predposlednou lomkou.
                    return new IwcmArchivItem(((IwcmArchivFsVolume) fsi.getVolume()), vp, FileArchivatorDB.getByUrl(vp));
                }

            }
        }
        //Logger.debug(this, "getParent() vraciam Root ");
        return getRoot();

    }

    @Override
    public String getPath(FsItem fsi) throws IOException {
        IwcmArchivItem item = asArchivGroup(fsi);
        String path = item.getPath();
        if (path.endsWith("/"))
        {
            path = path.substring(0,path.length()-1);
        }

        //Logger.debug(this,"getPath("+item.getPath()+") return: "+path);
        return path;
    }

    @Override
    public FsItem getRoot() {
        String archivePath = FileArchivatorKit.getArchivPath();
        if(archivePath.endsWith("/"))
        {
            archivePath = archivePath.substring(0,archivePath.length()-1);
        }
        return new IwcmArchivItem(this, "/" + archivePath,FileArchivatorDB.getByUrl(null));
    }

    @Override
    public long getSize(FsItem fsi) {
        return asArchivGroup(fsi).getSortPriority();
    }

    public int getSortPriority(FsItem fsi)
    {
        return asArchivGroup(fsi).getSortPriority();
    }

    @Override
    public String getThumbnailFileName(FsItem fsi) {
        return null;
    }

    @Override
    public boolean hasChildFolder(FsItem fsi) {
        String path = asArchivGroup(fsi).getPath();
        //Logger.debug(this, "hasChildFolder() path: "+path);
        int countMatches = StringUtils.countMatches(removeRootFrompath(path),"/");
        if(path.endsWith(asArchivGroup(getRoot()).getPath()))// je to Root
        {
            //Logger.debug(this,"hasChildFolder("+asArchivGroup(fsi).getPath()+") true");
            return true;
        }
        else if (Tools.isNotEmpty(removeRootFrompath(path)) && countMatches == 0 )
        {
            //Logger.debug(this, "hasChildFolder() countMatches: "+StringUtils.countMatches(removeRootFrompath(path),"/")+" true");
            return true;
        }

        //Logger.debug(this,"hasChildFolder("+path+") false");
        return false;
    }

    @Override
    public boolean isFolder(FsItem fsi) {
        boolean ret = false;
        String actPath = asArchivGroup(fsi).getPath();
        //Logger.debug(this,"isFolder("+actPath+")");
        if(isRoot(fsi))
        {
            ret = true;
        }

        String pathWithoutRoot = removeRootFrompath(actPath);
        if(Tools.isNotEmpty(pathWithoutRoot) && StringUtils.countMatches(pathWithoutRoot,"/") <= 1 && pathWithoutRoot.indexOf(".") == -1)
        {
            //Logger.debug(this,"isFolder ("+actPath+") result potencialne (ano)");
            ret = true;
        }

        //Logger.debug(this,"isFolder ("+actPath+") result("+ret+")");
        return ret;
    }

    private String removeRootFrompath(String path)
    {
        String pathNew = Tools.replace(path,"/"+FileArchivatorKit.getArchivPath(),"");
        pathNew = Tools.replace(pathNew,FileArchivatorKit.getArchivPath(),"");
        pathNew = Tools.replace(pathNew,FileArchivatorKit.getArchivPath().substring(0,FileArchivatorKit.getArchivPath().length()-1),"");
        return pathNew;
    }

    /**Vrati aktualnu kategoriu
     *
     * @param fsi
     * @return
     */
    private String getActualCategory(FsItem fsi, int level)
    {
        String actPath = asArchivGroup(fsi).getPath();
        //Logger.debug(this,"getActualCategory "+level+"("+actPath+" / "+removeRootFrompath(actPath)+")");
        if(isRoot(fsi))
        {
            //Logger.debug(this,"getActualCategory "+level+" return: "+null);
            return null;
        }
        String category = removeRootFrompath(actPath);


        int countSlash = StringUtils.countMatches(category,"/");
        countSlash++;
        //Logger.debug(this,"getActualCategory "+level+" countSlash: "+countSlash);
        if(level == 2)
        {
            if (countSlash == level)//   kategoria1/kategoria2/
            {
                if(category.startsWith("/"))
                {
                    category = category.substring(category.lastIndexOf("/" , category.length()));
                }
                else
                {
                    category = category.substring(category.lastIndexOf("/" ) +1,category.length());
                }
            }
            else
            {
                return null;
            }
        }
        if(countSlash == level && countSlash == 1)//   kategoria1/
        {
            //category = category.substring(0, category.length() - 1);
        }
        else if (level == 1 && countSlash == 2)
        {
            category = category.substring(0, category.indexOf("/"));
        }

        //Logger.debug(this,"getActualCategory "+level+" return: "+category);
        return category;
    }

    @Override
    public boolean isRoot(FsItem fsi) {
        return (("/"+FileArchivatorKit.getArchivPath()).equals(((IwcmArchivItem)fsi).getPath()))
                || (("/"+FileArchivatorKit.getArchivPath()).equals(((IwcmArchivItem)fsi).getPath()+"/"));
    }

    @Override
    public FsItem[] listChildren(FsItem fsi) {
        String helpPath = "";
        List<FsItem> resultList = new ArrayList<FsItem>();
        //Logger.debug(this,"listChildren ("+asArchivGroup(fsi).getPath()+")");
        if(getActualCategory(fsi,1) != null && !"/".equals(asArchivGroup(fsi).getPath()))// vrati true iba ak sme v kategoriach
        {
            //Logger.debug(this,"listChildren("+asArchivGroup(fsi).getPath()+") category");
            //vratime kategorie (ako priecinky) + subory bez kategorie
            FileArchivatorSearchBean fabSearch = new FileArchivatorSearchBean();
            fabSearch.setShowFile(true);
            fabSearch.setIncludeSubdirs(false);
            fabSearch.setOnlyMain(true);
            fabSearch.setCategory(new ArrayList<>(Arrays.asList(getActualCategory(fsi, 1))));//new ArrayList<>(Arrays.asList(category))
            if(getActualCategory(fsi, 2) != null )
            {
                fabSearch.setFieldA(getActualCategory(fsi, 2));
            }
            List<FileArchivatorBean> filesArchiv = FileArchivatorDB.search(fabSearch);
            //pridame vsetky subory + kategorie
            for(FileArchivatorBean fab:filesArchiv )
            {
                String newVirtualpath = "/"+fab.getVirtualPath();
                //musime vratit cestu podla aktualnej kategorie
                resultList.add(new IwcmArchivItem(this, newVirtualpath/*file.getVirtualPath()*/,fab));
            }

            //este musime pridat kategorie 2
            for(String category2 :FileArchivatorDB.getAllCategories2(getActualCategory(fsi,1)))
            {
                if(category2 == null)
                    continue;
                helpPath = asArchivGroup(getRoot()).getPath()+"/"+getActualCategory(fsi,1)+"/"+category2;//+"/"
                //Logger.debug(this,"listChildren add category 2 ("+helpPath+")");
                resultList.add(new IwcmArchivItem(this, helpPath, null));
            }

        }
        else
        {
            // vraciame subory z Roota
            FileArchivatorSearchBean fabSearch = new FileArchivatorSearchBean();
            fabSearch.setShowFile(true);
            fabSearch.setIncludeSubdirs(false);
            fabSearch.setOnlyMain(true);
            fabSearch.setExcludeCategory(FileArchivatorDB.getAllCategories());//new ArrayList<>(Arrays.asList(category))
            List<FileArchivatorBean> filesArchiv = FileArchivatorDB.search(fabSearch);
            //pridame vsetky subory bez kategorie
            for(FileArchivatorBean fab:filesArchiv )
            {
                //IwcmFile file = new IwcmFile(Tools.getRealPath("/"+fab.getVirtualPath()));
                String newVirtualpath = asArchivGroup(fsi).getPath()+fab.getFileName();
                //Logger.debug(this,"listChildren add file("+newVirtualpath+") "+getActualCategory(fsi, 1));
                resultList.add(new IwcmArchivItem(this, newVirtualpath/*file.getVirtualPath()*/, fab));
            }

            for(String category :FileArchivatorDB.getAllCategories())
            {
                //Logger.debug(this,"listChildren add category("+asArchivGroup(getRoot()).getPath()+"/"+category);
                resultList.add(new IwcmArchivItem(this, asArchivGroup(getRoot()).getPath()+"/"+category,null));
            }
        }
        return resultList.toArray(new FsItem[0]);
    }

    @Override
    public InputStream openInputStream(FsItem fsi) throws IOException {
        return null;
    }

    @Override
    public OutputStream openOutputStream(FsItem fsi) throws IOException {
        return null;
    }

    @Override
    public void rename(FsItem src, FsItem dst) throws IOException {

    }

    protected IwcmArchivItem asArchivGroup(FsItem fsi)
    {
        return ((IwcmArchivItem) fsi);
    }
}
