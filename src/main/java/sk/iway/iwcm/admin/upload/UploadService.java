package sk.iway.iwcm.admin.upload;

import sk.iway.iwcm.*;
import sk.iway.iwcm.common.FileIndexerTools;
import sk.iway.iwcm.common.GalleryToolsForCore;
import sk.iway.iwcm.common.ImageTools;
import sk.iway.iwcm.editor.UploadFileAction;
import sk.iway.iwcm.editor.UploadFileForm;
import sk.iway.iwcm.findexer.FileIndexer;
import sk.iway.iwcm.findexer.ResultBean;
import sk.iway.iwcm.gallery.GalleryDB;
import sk.iway.iwcm.gallery.VideoConvert;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.users.UsersDB;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Sluzby potrebne pre upload suborov, po volani .process() sa vykonaju vsetky WJ operacie typu
 * - generovania obrazkov galerie
 * - full text index
 * - watermarking obrazku
 * - konverzia CMYK na RGB
 * - video konverzia
 */
public class UploadService {

    private String virtualPath;
    private HttpServletRequest request;

    //zoznam zmazanych URL adries suborov pocas procesingu (napr. pri konverzii videa)
    private Set<String> removedUrls;
    //zoznam pridanych URL adries suborov (napr. pri fotogalerii a generovani s_ obrazka)
    private Set<String> addedUrls;


    public UploadService(String virtualPath, HttpServletRequest request) {
        this.virtualPath = virtualPath;
        this.request = request;

        removedUrls = new HashSet<>();
        addedUrls = new HashSet<>();
    }

    public UploadService(String fileKey, String destinationFolder, String fileName, HttpServletRequest request) {

        this(checkLastSlash(destinationFolder)+fileName, request);

        try {
            AdminUploadServlet.moveAndReplaceFile(fileKey, destinationFolder, fileName);

            process();
        }
        catch (Exception ex) {
            Logger.error(UploadService.class, ex);
        }
    }

    private static String checkLastSlash(String folderUrl)
    {
        if (folderUrl.endsWith("/")==false) return folderUrl + "/";
        return folderUrl;
    }

    /**
     * vykonaj vsetky potrebne operacie po uploade suborov
     * - full text index
     * - galeria
     */
    public void process()
    {
        IwcmFile uploadedFile = new IwcmFile(Tools.getRealPath(virtualPath));
        if (uploadedFile.exists()==false) return;

        Identity user = UsersDB.getCurrentUser(request);

        String fileName = uploadedFile.getName();
        String dir = uploadedFile.getVirtualParent();

        //ak je treba, aplikujem vodotlac na obrazky
        GalleryDB.applyWatermarkOnUpload(uploadedFile);

        // ak je to povolene, pokusime sa skonvertovat CMYK obrazok na RGB
        ImageTools.convertCmykToRgb(uploadedFile.getAbsolutePath());

        //ak je to JPG obrazok, skusime ziskat datum vytvorenia fotografie na zaklade EXIF metadat
        Date dateCreated = GalleryDB.getExifDateOriginal(uploadedFile);

        if (VideoConvert.isVideoFile(virtualPath))
        {
            if (virtualPath.endsWith("."+Constants.getString("defaultVideoFormat"))==false)
            {
                //nie je to mp4, treba skonvertovat
                UploadFileForm my_form = new UploadFileForm();
                my_form.setBitRate(Constants.getInt("defaultVideoBitrate"));
                my_form.setVideoWidth(Constants.getInt("defaultVideoWidth"));
                my_form.setVideoHeight(Constants.getInt("defaultVideoHeight"));
                my_form.setKeepOriginalVideo(false);

                /* TODO: integracia do UploadCommandExecutor
                //zmaz povodny added mpg subor
                for (FsItemEx item : added)
                {
                    if (item.getPath().endsWith(fileName))
                    {
                        added.remove(item);
                        break;
                    }
                }
                */

                removedUrls.add(virtualPath);

                String fileURL = VideoConvert.convert(my_form, virtualPath, request);
                Logger.debug(UploadService.class, "Converted video: "+fileURL);
                if (Tools.isNotEmpty(fileURL) && fileURL.lastIndexOf("/")>1)
                {
                    String videoFileName = fileURL.substring(fileURL.lastIndexOf("/") + 1);

                    addedUrls.add(videoFileName);
                    addedUrls.add(Tools.replace(videoFileName, "."+Constants.getString("defaultVideoFormat"), ".jpg"));

                    /* TODO: integracia do UploadCommandExecutor
                    added.add(new FsItemEx(dir, videoFileName));
                    added.add(new FsItemEx(dir, Tools.replace(videoFileName, "."+Constants.getString("defaultVideoFormat"), ".jpg")));
                    */
                }
            }
            else
            {
                try {
                    //pre mp4 vytvorime len screenshot
                    String image = VideoConvert.makeScreenshot(Tools.getRealPath(virtualPath), null);
                    if (image != null)
                    {
                        IwcmFile thumb = new IwcmFile(image);
                        virtualPath = dir + "/" + thumb.getName(); //NOSONAR
                        addedUrls.add(virtualPath);


                        /* TODO: integracia do UploadCommandExecutor
                        String imageFilename = new IwcmFile(image).getName();
                        if (Tools.isEmpty(directory))
                        {
                            added.add(new FsItemEx(dir, imageFilename));
                        }
                        */
                    }
                } catch (Exception ex) {
                    Logger.error(UploadService.class, ex);
                }
            }
        }

        if (GalleryDB.isGalleryFolder(dir))
        {
            //zmaz exif aj z originalneho suboru
            GalleryDB.stripExif(Tools.getRealPath(virtualPath));

            //schvalne musime pouzit prefix, pretoze tak sa nam nevrati original virtualPath ak subor neexistuje
            String imageSmall = GalleryToolsForCore.getImagePathPrefix("s_", virtualPath);
            String imageOriginal = GalleryToolsForCore.getImagePathPrefix("o_", virtualPath);

            IwcmFile originalGalleryFile = new IwcmFile(Tools.getRealPath(imageOriginal));
            if (originalGalleryFile.exists() && virtualPath.equals(imageOriginal)==false) {
                //musime skopirovat obsah do o_ suboru, pretoze nasledne sa resize robi z o_ obrazku (ak existuje)
                FileTools.copyFile(uploadedFile, originalGalleryFile);
            }

            //zmaz o_ obrazok ak existuje

            GalleryDB.resizePicture(Tools.getRealPath(virtualPath), dir);

            addedUrls.add(imageSmall);
            addedUrls.add(imageOriginal);

            /* TODO: integracia do UploadCommandExecutor
            added.add(new FsItemEx(dir, "s_"+fileName));
            added.add(new FsItemEx(dir, "o_"+fileName));
            */
        }
        else if (Constants.getBoolean("imageAlwaysCreateGalleryBean"))
        {
            GalleryDB.setImage(dir, fileName);
        }

        //zapise datum vytvorenia fotografie (ak vieme ziskat)
        if (dateCreated != null) {
            GalleryDB.setUploadDateImage(dir, fileName, dateCreated.getTime());
        }

        //ak existuje adresar files, treba indexovat
        if (FileIndexer.isFileIndexerConfigured())
        {
            List<ResultBean> indexedFiles = new ArrayList<>();
            FileIndexerTools.indexFile(dir + "/" + fileName, indexedFiles, request);
        }

        UploadFileAction.reflectionLoader(request, user, dir + "/" + fileName);
    }

    /**
     * Najde meno pre subor pre zvolenu moznost ponechat obe. Novy subor bude mat nazov subor-xxx.jpg pricom xxx je inkrementalne cislo
     * @param destinationFolder
     * @param fileName
     * @return - nove meno suboru (bez cesty)
     */
    public static synchronized String getKeppBothFileName(String destinationFolder, String fileName) {

        int dot = fileName.lastIndexOf(".");
        if (dot > 0) {
            String nameBeforeDot = fileName.substring(0, dot);
            String suffix = fileName.substring(dot);

            for (int i=1; i<999; i++) {
                String testVirtualPath = nameBeforeDot + "-" + i + suffix;
                if (FileTools.isFile(destinationFolder + testVirtualPath)==false) return testVirtualPath;
            }
        }

        return null;
    }

    /**
     * Vrati set URL adries suborov, ktore boli pocas procesingu uploadu zmazane (napr. mpg subor pri konverzii do mp4)
     */
    public Set<String> getRemovedUrls() {
        return removedUrls;
    }

    /**
     * Vrati set URL adries suborov, ktore boli pocas procesingu pridane (napr. s_ a o_ obrazky vo foto galerii)
     * @return
     */
    public Set<String> getAddedUrls() {
        return addedUrls;
    }

    public String getVirtualPath() {
        return virtualPath;
    }

    public boolean processDelete() {
        IwcmFile uploadedFile = new IwcmFile(Tools.getRealPath(virtualPath));
        if (!uploadedFile.exists()) {
            return false;
        }

        // maze z galerie video a thumbnail
        removeVideo(uploadedFile.getVirtualPath());

        // maze z galerie obrazky
        if (GalleryDB.isGalleryFolder(uploadedFile.getVirtualParent())) {
            removeOriginalBigAndSmallImage();
        }

        //ak existuje adresar files, maze index
        if (FileIndexer.isFileIndexerConfigured()) {
            FileIndexerTools.deleteIndexedFile(virtualPath);
        }

        return true;
    }

    private void removeVideo(String virtualPath) {
        IwcmFile file = new IwcmFile(Tools.getRealPath(changeExtension(virtualPath, Constants.getString("defaultVideoFormat", "mp4"))));
        if (file.exists()) {
            Logger.debug(UploadService.class, String.format("File to delete: video, path: %s", file.getVirtualPath()));
            file.delete();
        }
    }

    private void removeOriginalBigAndSmallImage() {
        //schvalne musime pouzit prefix, pretoze tak sa nam nevrati original virtualPath ak subor neexistuje
        String imageSmall = GalleryToolsForCore.getImagePathPrefix("s_", virtualPath);
        String imageOriginal = GalleryToolsForCore.getImagePathPrefix("o_", virtualPath);

        IwcmFile originalGalleryFile = new IwcmFile(Tools.getRealPath(imageOriginal));
        if (originalGalleryFile.exists()) {
            Logger.debug(UploadService.class, String.format("File to delete: image original, path: %s", originalGalleryFile.getVirtualPath()));
            originalGalleryFile.delete();
        }

        IwcmFile smallGalleryFile = new IwcmFile(Tools.getRealPath(imageSmall));
        if (smallGalleryFile.exists()) {
            Logger.debug(UploadService.class, String.format("File to delete: small original, path: %s", smallGalleryFile.getVirtualPath()));
            smallGalleryFile.delete();
        }

        IwcmFile bigGalleryFile = new IwcmFile(Tools.getRealPath(virtualPath));
        if (bigGalleryFile.exists()) {
            Logger.debug(UploadService.class, String.format("File to delete: big original, path: %s", bigGalleryFile.getVirtualPath()));
            bigGalleryFile.delete();
        }
    }

    private String changeExtension(String virtualPath, String newExtension) {
        return virtualPath.substring(0, virtualPath.lastIndexOf(".") + 1) + newExtension;
    }
}