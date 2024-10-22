package sk.iway.iwcm.components.gallery;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.upload.UploadService;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.FileBrowserTools;
import sk.iway.iwcm.common.ImageTools;
import sk.iway.iwcm.gallery.GalleryDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.users.UsersDB;

/**
 * GalleryRestController
 */
@RestController
@Datatable
@RequestMapping(value = "/admin/rest/components/gallery")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('menuGallery')")
public class GalleryRestController extends DatatableRestControllerV2<GalleryEntity, Long> {

    @Autowired
    public GalleryRestController(GalleryRepository repository, HttpServletRequest request) {
        super(repository);
    }

    @Override
    public Page<GalleryEntity> getAllItems(Pageable pageable) {
        //getAll nie je povolene, vrati prazdne data, pouziva sa findBy podla adresara po kliknuti na stromovu strukturu
        return new DatatablePageImpl<>(new ArrayList<>());
    }

    /**
     * Metoda na kontrolu prav pouzivatela
     *
     * @param request
     * @return
     */
    @Override
    public boolean checkAccessAllowed(HttpServletRequest request) {
        Identity currentUser = UsersDB.getCurrentUser(request);
        return currentUser.isEnabledItem("menuGallery");
    }


    /**
     * Prikladova metoda na validaciu dat odoslanych z datatables editora.
     * Metoda je volana pre kazdy odoslaby objekt.
     * Chyby pridava do error objeku pomocou {@link Errors}.rejectValue
     *
     * @param request
     * @param user
     * @param errors
     * @param id
     * @param entity
     */
    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, GalleryEntity> target, Identity user, Errors errors, Long id, GalleryEntity entity) {
        if (!user.isFolderWritable(entity.getImagePath())) {
            errors.rejectValue("errorField.imagePath", "403", Prop.getInstance().getText("user.rights.no_folder_rights"));
        }
        if (entity.getImageName().contains("/") || entity.getImageName().contains("\\") || FileBrowserTools.hasForbiddenSymbol(entity.getImageName())) {
            errors.rejectValue("errorField.imageName", "403", Prop.getInstance().getText("checkform.title.safeChars"));
        }
    }

    /**
     * Vykonanie akcie otocenia obrazka
     */
    @Override
    public boolean processAction(GalleryEntity entity, String action) {
        String imageUrl = entity.getImagePath() + "/" + entity.getImageName();
        boolean isGalleryFolder = GalleryDB.isGalleryFolder(entity.getImagePath());

        if (isGalleryFolder) imageUrl = GalleryDB.getImagePathOriginal(imageUrl);

        int status = -1;

        int rotateAngle = 0;
        if ("rotateRight".equals(action)) rotateAngle = 90;
        if ("rotateLeft".equals(action)) rotateAngle = 270;

        if (rotateAngle!=0) {
            status = ImageTools.rotateImage(imageUrl, rotateAngle);
            if (isGalleryFolder) {
                GalleryDB.resizePicture(Tools.getRealPath(imageUrl), entity.getImagePath());
            }
        }

        return status == 0;
    }

    /**
     * Pri zmazani entity z DB je potrebne zmazat aj subory z disku
     */
    @Override
    public boolean beforeDelete(GalleryEntity entity) {
        UploadService uploadService = new UploadService(entity.getImagePath() + "/" + entity.getImageName(), getRequest());
        uploadService.processDelete();
        //vratime true aj ked sa nepodari zmazat, lebo ak obrazok neexistuje tak sa neda zmazat DB zaznam a ten je podstatny
        return true;
    }

    @Override
    public GalleryEntity insertItem(GalleryEntity entity) {
        //ak sa vola insert a ma nastavenu cestu k obrazku, tak sa jedna o funkciu duplikovania, obrazok musime najskor skopirovat
        if (Tools.isNotEmpty(entity.getImageName())) {
            String originalUrl = GalleryDB.getImagePathOriginal(entity.getImagePath()+"/"+entity.getImageName());

            String name = entity.getDescriptionShortSk();
            if (Tools.isEmpty(name)) name = entity.getDescriptionShortCz();
            if (Tools.isEmpty(name)) name = entity.getDescriptionShortEn();
            if (Tools.isEmpty(name)) name = entity.getDescriptionShortDe();

            if (Tools.isEmpty(name)) {
                name = entity.getImageName();
            } else {
                //remove special chars and add extension
                name = DocTools.removeChars(name, true).toLowerCase()+"."+FileTools.getFileExtension(entity.getImageName());
            }

            if (FileTools.isFile(entity.getImagePath()+"/"+name)) {
                //subor uz existuje, musime zmenit nazov
                int dot = name.lastIndexOf(".");
                if (dot > 0) {
                    for (int i=1; i<100; i++) {

                        String newNameTest = name.substring(0, dot) + i + name.substring(dot);
                        if (FileTools.isFile(entity.getImagePath()+"/"+newNameTest)==false) {
                            name = newNameTest;
                            break;
                        }

                    }
                }
            }

            entity.setImageName(name);
            FileTools.copyFile(new IwcmFile(Tools.getRealPath(originalUrl)), new IwcmFile(Tools.getRealPath(entity.getImagePath()+"/"+name)));
        }

        entity.setId(null);
        GalleryEntity saved = super.insertItem(entity);

        //resize robime az po save, inak by nam to spravilo druhy DB zaznam
        GalleryDB.resizePicture(Tools.getRealPath(entity.getImagePath()+"/"+entity.getImageName()), entity.getImagePath());

        return saved;
    }

    @Override
    public GalleryEntity editItem(GalleryEntity entity, long id) {
        GalleryEntity original = getOne(id);
        GalleryEntity saved = super.editItem(entity, id);

        if (original != null && original.getImageName()!=null && original.getImageName().equals(saved.getImageName())==false) {
            //obrazok bol premenovany, je potrebne presunut na file systeme
            FileTools.moveFile(original.getImagePath()+"/o_"+original.getImageName(), saved.getImagePath()+"/o_"+saved.getImageName());
            FileTools.moveFile(original.getImagePath()+"/"+original.getImageName(), saved.getImagePath()+"/"+saved.getImageName());
            FileTools.moveFile(original.getImagePath()+"/s_"+original.getImageName(), saved.getImagePath()+"/s_"+saved.getImageName());
        }

        return saved;
    }

    private void setLastModified(String path, long lastModified) {
        IwcmFile image = new IwcmFile(Tools.getRealPath(path));
        if (image != null && image.exists()) image.setLastModified(lastModified);
    }

    @Override
    public void afterSave(GalleryEntity entity, GalleryEntity saved) {
        //change image lastmodified to regenerate thumb image if necessary
        long now = Tools.getNow();
        setLastModified(entity.getImagePath()+"/o_"+entity.getImageName(), now);
        setLastModified(entity.getImagePath()+"/"+entity.getImageName(), now);
        setLastModified(entity.getImagePath()+"/s_"+entity.getImageName(), now);
    }

}