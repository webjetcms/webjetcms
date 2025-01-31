package sk.iway.iwcm.components.gallery;

import java.io.IOException;

import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.gallery.GalleryDB;

public class GalleryService {

    private GalleryService() {}

    /**
     * Create or update gallery entity after file move into new folder
     * @param src
     * @param dst
     * @throws IOException
     */
    public static void createOrUpdateGalleryEntity(FsItemEx src, FsItemEx dst) throws IOException {

        String srcPath = src.getPath();
        String destPath = dst.getPath();

        if (FileTools.isImage(srcPath)==false && FileTools.isImage(destPath)==false) return;

        int i = srcPath.lastIndexOf("/");
        String srcDir = srcPath.substring(0, i);
        String fileName = srcPath.substring(i + 1);

        i = destPath.lastIndexOf("/");
        String destDir = destPath.substring(0, i);
        String destFileName = destPath.substring(i + 1);

        GalleryRepository gr = Tools.getSpringBean("galleryRepository", GalleryRepository.class);

        //GET gallery entity
        GalleryEntity srcEntity = gr.findByImagePathAndImageNameAndDomainId(srcDir, fileName, CloudToolsForCore.getDomainId());

        if(srcEntity != null) {
            //Entity allready exist, change only dir to destDir
            srcEntity.setImagePath(destDir);
            srcEntity.setImageName(destFileName);
            gr.save(srcEntity);
        } else {
            String galleryPath = Constants.getString("imagesRootDir") + "/" + Constants.getString("galleryDirName") + "/";

            //Gallery entity not exist, create new ONLY IF destination is gallery
            if(destPath.startsWith(galleryPath)) {
                GalleryDB.setImage(destDir, destFileName);
            }
        }
    }

    private static String getPixabayCacheKey(String imageName) {
        return "pixabay_image_source_" + imageName;
    }

    /**
     * Save URL of downloaded image from PixaBay for later use
     * @param imageName
     * @param url
     */
    public static void savePixabayImageUrl(String imageName, String url) {
        Cache c = Cache.getInstance();
        //Save into cache image source
        c.setObject(getPixabayCacheKey(imageName), url, 10);
    }

    /**
     * Get URL of downloaded image from PixaBay OR NULL if not found, and remove it from cache
     * @param imageName
     * @return
     */
    public static String getPixabayImageUrl(String imageName, boolean removeFromCache) {
        Cache c = Cache.getInstance();
        String key = getPixabayCacheKey(imageName);
        String imageUrl = c.getObject(key, String.class);
        if (removeFromCache && imageUrl != null) c.removeObject(key);
        return imageUrl;
    }
}