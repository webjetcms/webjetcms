package sk.iway.iwcm.common;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.io.IwcmFile;

public class GalleryToolsForCore {
    /**
     * Vrati malu verziu obrazku z galerie (ak existuje)
     *
     * @param fullPath
     * @return
     */
    public static String getImagePathSmall(String fullPath)
    {
        String image = getImagePathPrefix("s_", fullPath);
        IwcmFile f = new IwcmFile(Tools.getRealPath(image));
        if (f.exists())
        {
            return (image);
        }
        return (fullPath);
    }

    /**
     * Vrati velku verziu obrazku z galerie (ak existuje)
     *
     * @param fullPath
     * @return
     */
    public static String getImagePathNormal(String fullPath)
    {
        String image = getImagePathPrefix(null, fullPath);
        IwcmFile f = new IwcmFile(Tools.getRealPath(image));
        if (f.exists())
        {
            return (image);
        }
        return (fullPath);
    }

    public static String getImagePathOriginal(String fullPath)
    {
        String image = getImagePathPrefix("o_", fullPath);
        IwcmFile f = new IwcmFile(Tools.getRealPath(image));
        if (f.exists())
        {
            return (image);
        }
        return (fullPath);
    }

    public static String getImagePathPrefix(String prefix, String fullPath)
    {
        String file = null;
        String path = null;
        int pos = fullPath.lastIndexOf('/');
        if (pos == -1)
        {
            file = fullPath;
            path = "/";
        }
        else
        {
            file = fullPath.substring(pos + 1);
            path = fullPath.substring(0, pos + 1);
        }
        if (file.startsWith("s_") || file.startsWith("o_"))
        {
            file = file.substring(2);
        }
        if (Tools.isNotEmpty(prefix))
        {
            file = prefix + file;
        }
        return (path + file);
    }
}
