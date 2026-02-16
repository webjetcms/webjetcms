package sk.iway.iwcm.helpers;

import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.io.IwcmFile;

import java.util.Comparator;
import java.util.List;

public class VueHelper {
    private String distPath = "";

    public VueHelper()
    {
    }

    public VueHelper(String path)
    {
        setDistPath(path);
    }

    public void setDistPath(String path)
    {
        distPath = path;
    }

    public String getJsFile(String scriptName)
    {
        return getFile(scriptName, distPath + "js/", ".js");
    }

    public String getCssFile(String scriptName)
    {
        return getFile(scriptName, distPath + "css/", ".css");
    }

    public String getFile(String scriptName, String dirPath, String suffix)
    {
        List<IwcmFile> files = FileTools.getFilesRecursive(new IwcmFile(Tools.getRealPath(dirPath)), f -> f.getName().endsWith(suffix) && f.getName().startsWith(scriptName));
        if(files == null || files.size() <= 0)
        {
            return "";
        }
        files.sort(new Comparator<IwcmFile>(){
            public int compare(IwcmFile f1, IwcmFile f2)
            {
                return - Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
            } });
        return files.get(0).getVirtualPath();
    }
}
