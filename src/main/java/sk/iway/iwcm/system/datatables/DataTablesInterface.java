package sk.iway.iwcm.system.datatables;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;

import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.io.IwcmFile;

/**
 *  DataTablesInterface.java
 *  Interface class that allows communication with the DataTables Editor.
 *  For example of implementation see package sk.iway.iwcm.components.enumerations
 *
 *
 * Title        webjet8
 * Company      Interway a.s. (www.interway.sk)
 * Copyright    Interway a.s (c) 2001-2018
 * author       $Author: mhruby $
 * version      $Revision: 1.0 $
 * created      Date: 27.03.2018 12:00:00
 * modified     $Date: 27.03.2018 12:0:00 $
 */


public interface DataTablesInterface {

    /**
     * Perform select all
     * @return List<Object>
     */
    List<Object> list(HttpServletRequest request);

    /**
     * Perform insert/update/delete.
     * @param parsedRequest parsed input map that contains input form fields
     * @return list with inserted/edited object. If input is invalid return null.
     */
    List<Object> save(HttpServletRequest request, Map<Integer,Map<String, String>> parsedRequest);

    /**
     * Getter for input errors.
     * @return List of DataTablesFieldError
     */
    List<DataTablesFieldError> getFieldErrors();

    String getError();

    boolean canSave(Identity user);

    /**
     * Hashtabulka uploadnutych suborov, key je unikatny nazov (idecko) suboru, value je URL uploadnuteho suboru
     * @return
     */
    default Hashtable<String, String> getFiles()
    {
        return null;
    }

    /**
     * Default adresar pre upload suborov
     * @return
     */
    default String getUploadDir()
    {
        return "/images/protected/_upload_temp";
    }

    /**
     * Overenie, ci je dany FileItem mozne uploadnut (kontrola typu, velkosti, ...)
     * @param item
     * @return
     */
    default boolean canUpload(FileItem<?> item) { return true; }

    /**
     * Default implementacia uploadu
     * @param request
     */
    default boolean upload(HttpServletRequest request)
    {
        @SuppressWarnings("unchecked")
        Map<String, FileItem<?>> files = (Map<String,FileItem<?>>)request.getAttribute("MultipartWrapper.files");
        for (Map.Entry<String, FileItem<?>> entry : files.entrySet())
        {
            //String name = entry.getKey();
            FileItem<?> item = entry.getValue();

            String randomName = RandomStringUtils.secure().next(10, true, true);
            if (canUpload(item))
            {
                String ext = FileTools.getFileExtension(item.getName());
                String fileUrl = getUploadDir()+"/"+randomName+"."+ext;
                File dir = new File(Tools.getRealPath(getUploadDir()));
                if (dir.exists()==false) dir.mkdirs();
                File f = new File(Tools.getRealPath(fileUrl));

                FileOutputStream fos = null;
                //item.getInputStream()
                try
                {
                    f.createNewFile();
                    fos = new FileOutputStream(f);
                    IOUtils.write(IOUtils.toByteArray(item.getInputStream()), fos);

                    getFiles().put(randomName, fileUrl);

                    return true;
                } catch (IOException e)
                {
                    sk.iway.iwcm.Logger.error(e);
                    if (fos!=null)
                    {
                        try { fos.close(); } catch (IOException e1) { sk.iway.iwcm.Logger.error(e1); }
                    }
                }
            }
        }

        //zmaz z tempu stare subory
        if (getUploadDir().contains("temp") || getUploadDir().contains("tmp"))
        {
            IwcmFile tempDir = new IwcmFile(Tools.getRealPath(getUploadDir()));
            FileTools.deleteDirTree(tempDir, 8 * 60 * 60 * 1000L);
        }
        return false;
    }
}
