package sk.iway.iwcm.components.gallery;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.io.IwcmFile;

@RestController
@RequestMapping(value = "/admin/rest/image-editor")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('menuWebpages|menuFbrowser')")
public class ImageEditorRestController {

    private final GalleryRepository galleryRepository;

    @Autowired
    public ImageEditorRestController(GalleryRepository galleryRepository) {
        this.galleryRepository = galleryRepository;
    }

    @PostMapping("/save-area-of-interest")
    public ResponseEntity<String> saveImageAreaOfInterest(GalleryEntity entity) {
        try {
            GalleryEntity dbEntity = galleryRepository.findByImagePathAndImageNameAndDomainId(entity.getImagePath(), entity.getImageName(), CloudToolsForCore.getDomainId());
            if (dbEntity == null) {
                dbEntity = new GalleryEntity();
                dbEntity.setImagePath(entity.getImagePath());
                dbEntity.setImageName(entity.getImageName());
            }
            dbEntity.setSelectedWidth(entity.getSelectedWidth());
            dbEntity.setSelectedHeight(entity.getSelectedHeight());
            dbEntity.setSelectedX(entity.getSelectedX());
            dbEntity.setSelectedY(entity.getSelectedY());
            galleryRepository.save(dbEntity);

            //update image lastModified to refresh thumb cache
            IwcmFile file = new IwcmFile(Tools.getRealPath(entity.getImagePath() + "/" + entity.getImageName()));
            if (file.exists() && file.isFile()) {
                file.setLastModified(System.currentTimeMillis());
            }

            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            Logger.error(getClass(), ex);
            return ResponseEntity.badRequest().build();
        }
    }

}
