package sk.iway.iwcm.components.gallery;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.upload.UploadService;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.FileBrowserTools;
import sk.iway.iwcm.common.ImageTools;
import sk.iway.iwcm.components.perex_groups.PerexGroupsEntity;
import sk.iway.iwcm.components.perex_groups.PerexGroupsRepository;
import sk.iway.iwcm.gallery.GalleryDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.system.spring.NullAwareBeanUtils;
import sk.iway.iwcm.users.UsersDB;

/**
 * GalleryRestController
 */
@RestController
@Datatable
@RequestMapping(value = "/admin/rest/components/gallery")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('menuGallery')")
public class GalleryRestController extends DatatableRestControllerV2<GalleryEntity, Long> {

    private final GalleryRepository repository;
    private final PerexGroupsRepository perexGroupsRepository;
    private final GalleryDimensionRepository gdr;

    @Autowired
    public GalleryRestController(GalleryRepository repository, PerexGroupsRepository perexGroupsRepository, GalleryDimensionRepository gdr, HttpServletRequest request) {
        super(repository);
        this.repository = repository;
        this.perexGroupsRepository = perexGroupsRepository;
        this.gdr = gdr;
    }

    @Override
    public Page<GalleryEntity> getAllItems(Pageable pageable) {
        //getAll nie je povolene, vrati prazdne data, pouziva sa findBy podla adresara po kliknuti na stromovu strukturu
        return new DatatablePageImpl<>(new ArrayList<>());
    }

    @Override
    public GalleryEntity getOneItem(long id) {

        if(isImageEditor()) {
            //in ImageEditor we don't get ID, but we get imagePath and imageName, so load entity by these values
            String imagePath = getRequest().getParameter("dir");
            String imageName = getRequest().getParameter("name");

            GalleryEntity entity = new GalleryEntity();
            if(Tools.isNotEmpty(imagePath) && Tools.isNotEmpty(imageName)) {
                entity = repository.findByImagePathAndImageNameAndDomainId(imagePath, imageName, CloudToolsForCore.getDomainId());
                if(entity == null) {
                    entity = new GalleryEntity();
                    entity.setImagePath(imagePath);
                    entity.setImageName(imageName);
                    entity.setUploadDatetime(new Date());
                    entity.setDomainId(CloudToolsForCore.getDomainId());
                }
                processFromEntity(entity, ProcessItemAction.EDIT);
            }

            if(entity.getId() == null) entity.setId(Long.valueOf(-1));
            return entity;
        }

        return super.getOneItem(id);
    }

    @Override
    public Page<GalleryEntity> searchItem(Map<String, String> params, Pageable pageable, GalleryEntity search) {
        DatatablePageImpl<GalleryEntity> page =  new DatatablePageImpl<>( super.searchItem(params, pageable, search) );

        //this can't be in getOptions method becase getAll is never called
        List<PerexGroupsEntity> perexList = perexGroupsRepository.findAllByDomainIdOrderByPerexGroupNameAsc(CloudToolsForCore.getDomainId());
        page.addOptions("editorFields.perexGroupsIds", perexList, "perexGroupName", "id", false);

        return page;
    }

    @Override
    public GalleryEntity processFromEntity(GalleryEntity entity, ProcessItemAction action) {
        if(entity == null) entity = new GalleryEntity();
        if(entity.getEditorFields() == null) {
            GalleryEditorFields gef = new GalleryEditorFields();
            gef.fromGalleryEntity(entity);
            entity.setEditorFields(gef);
        }
        return entity;
    }

    @Override
    public GalleryEntity processToEntity(GalleryEntity entity, ProcessItemAction action) {
        if(entity != null) {
            GalleryEditorFields gef = entity.getEditorFields();
            if(gef == null) gef = new GalleryEditorFields();
            gef.toGalleryEntity(entity);
        }
        return entity;
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
        // For gallery only
        if(isImageEditor() == false) {
            // DirSimpleGallery aka image path MUST be set
            if (entity.getEditorFields() == null || Tools.isEmpty(entity.getEditorFields().getImagePath()) || entity.getEditorFields().getImagePath().startsWith(getBaseGalleryPath()) == false) {
                // Check if DirSimpleGallery starts with /images/gallery
                errors.rejectValue("errorField.editorFields.dirSimpleGallery", "403", Prop.getInstance().getText("components.gallery.image_path.err", getBaseGalleryPath()));
            }
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

        boolean isImageEditor = isImageEditor();

        // Image editor do not have permission to change imagePath via dirSimpleGallery
        if(isImageEditor) {
            entity.getEditorFields().setImagePath(entity.getImagePath());

            if(entity.getId() > 0) {
                // ITS update
                return editItem(entity, entity.getId());
            }
        }

        //ak sa vola insert a ma nastavenu cestu k obrazku, tak sa jedna o funkciu duplikovania, obrazok musime najskor skopirovat
        if (isImageEditor == false && Tools.isNotEmpty(entity.getImageName())) {
            String originalUrl = GalleryDB.getImagePathOriginal(entity.getImagePath() + "/" + entity.getImageName());

            String name = entity.getDescriptionShortSk();
            if (Tools.isEmpty(name)) name = entity.getDescriptionShortCz();
            if (Tools.isEmpty(name)) name = entity.getDescriptionShortEn();
            if (Tools.isEmpty(name)) name = entity.getDescriptionShortDe();

            if (Tools.isEmpty(name)) {
                name = entity.getImageName();
            } else {
                //remove special chars and add extension
                name = DB.internationalToEnglish(name);
                String ext = FileTools.getFileExtension(entity.getImageName());
                if (Tools.isNotEmpty(ext) && name.endsWith("."+ext)==false) name = name+"."+ext;
                name = DocTools.removeCharsDir(name, true).toLowerCase();
            }

            String newPath = entity.getEditorFields().getImagePath();
            if (FileTools.isFile(newPath + "/" + name)) {
                //subor uz existuje, musime zmenit nazov
                int dot = name.lastIndexOf(".");
                if (dot > 0) {
                    for (int i=1; i<100; i++) {

                        String newNameTest = name.substring(0, dot) + i + name.substring(dot);
                        if (FileTools.isFile(newPath + "/" + newNameTest) == false) {
                            name = newNameTest;
                            break;
                        }

                    }
                }
            }

            entity.setImageName(name);
            FileTools.copyFile(new IwcmFile(Tools.getRealPath(originalUrl)), new IwcmFile(Tools.getRealPath(newPath + "/" + name)));
        }

        entity.setId(null);
        GalleryEntity saved = super.insertItem(entity);

        //resize robime az po save, inak by nam to spravilo druhy DB zaznam
        if(isImageEditor == false) {
            GalleryDB.resizePicture(Tools.getRealPath(saved.getImagePath()+"/"+saved.getImageName()), saved.getImagePath());

            //
            checkAndCreateGallery(saved.getImagePath());
        }

        if (isImageEditor) {
            String name = getRequest().getParameter("name");
            String dir = getRequest().getParameter("dir");
            if (Tools.isNotEmpty(dir) && Tools.isNotEmpty(name)) {

                GalleryEntity original = new GalleryEntity();
                original.setImagePath(dir);
                original.setImageName(name);

                if (original.getImageName().equals(saved.getImageName()) == false || original.getImagePath().equals(saved.getImagePath()) == false) {
                    // Obrazok bol premenovany ALEBO presunuty, je potrebne presunut na file systeme
                    FileTools.moveFile(original.getImagePath()+"/"+original.getImageName(), saved.getImagePath()+"/"+saved.getImageName());
                    if (FileTools.isFile(original.getImagePath()+"/o_"+original.getImageName())) FileTools.moveFile(original.getImagePath()+"/o_"+original.getImageName(), saved.getImagePath()+"/o_"+saved.getImageName());
                    if (FileTools.isFile(original.getImagePath()+"/s_"+original.getImageName())) FileTools.moveFile(original.getImagePath()+"/s_"+original.getImageName(), saved.getImagePath()+"/s_"+saved.getImageName());
                }
            }
        }

        return saved;
    }

    @Override
    public GalleryEntity editItem(GalleryEntity entity, long id) {
        GalleryEntity original = getOne(id);
        GalleryEntity saved = super.editItem(entity, id);

        if (original != null && (original.getImageName().equals(saved.getImageName()) == false || original.getImagePath().equals(saved.getImagePath()) == false)) {
            // Obrazok bol premenovany ALEBO presunuty, je potrebne presunut na file systeme
            FileTools.moveFile(original.getImagePath()+"/"+original.getImageName(), saved.getImagePath()+"/"+saved.getImageName());
            if (FileTools.isFile(original.getImagePath()+"/o_"+original.getImageName())) FileTools.moveFile(original.getImagePath()+"/o_"+original.getImageName(), saved.getImagePath()+"/o_"+saved.getImageName());
            if (FileTools.isFile(original.getImagePath()+"/s_"+original.getImageName())) FileTools.moveFile(original.getImagePath()+"/s_"+original.getImageName(), saved.getImagePath()+"/s_"+saved.getImageName());

            checkAndCreateGallery(saved.getImagePath());

            setForceReload(true);
        }

        return saved;
    }

    private void setLastModified(String path, long lastModified) {
        IwcmFile image = new IwcmFile(Tools.getRealPath(path));
        if (image.exists()) image.setLastModified(lastModified);
    }

    @Override
    public void afterSave(GalleryEntity entity, GalleryEntity saved) {
        //change image lastmodified to regenerate thumb image if necessary
        long now = Tools.getNow();
        setLastModified(entity.getImagePath()+"/"+entity.getImageName(), now);
        setLastModified(entity.getImagePath()+"/o_"+entity.getImageName(), now);
        setLastModified(entity.getImagePath()+"/s_"+entity.getImageName(), now);
    }

    private boolean isImageEditor() {
        return Tools.getBooleanValue(getRequest().getParameter("isImageEditor"), false);
    }

    /**
     * Check if dimension on given destination imagePath exist. If not, found last existing parent and use entity to create whole imagePath dir by dir, so moved image will be in configured dimension.
     * @param destPath
     */
    private void checkAndCreateGallery(String destPath) {
        if(Tools.isEmpty(destPath) || destPath.startsWith(getBaseGalleryPath()) == false) return;

        // Sanitize path
        destPath = DocTools.removeCharsDir(destPath, true).toLowerCase();

        StringBuilder rootPath = new StringBuilder(getBaseGalleryPath());
        int domainId = CloudToolsForCore.getDomainId();
        String author = getUser().getFullName();

        //Check if path exist
        GalleryDimension destDimension = gdr.findFirstByPathAndDomainId(destPath, domainId).orElse(null);
        if(destDimension == null) {
            //Dimension do not exist, find first existing parent path

            //remove prefix and get subfolders of dest path
            String restOfPath = destPath.replaceFirst("^" + getBaseGalleryPath(), "");
            if(restOfPath.startsWith("/")) restOfPath = restOfPath.substring(1);
            String[] subFolders = restOfPath.split("/");

            // Get root parent
            GalleryDimension lastExistingParent = gdr.findFirstByPathAndDomainId(rootPath.toString(), domainId).orElse(null);
            if(lastExistingParent == null) {
                Logger.error(GalleryRestController.class, "Cant crate new demension sub folder, because root folder was not found for domain : " + domainId);
                return;
            }

            boolean found = false;
            for(String subFolder : subFolders) {
                if(Tools.isEmpty(subFolder)) continue;

                rootPath.append("/").append(subFolder);

                if(found == false) {
                    GalleryDimension dimension = gdr.findFirstByPathAndDomainId(rootPath.toString(), CloudToolsForCore.getDomainId()).orElse(null);
                    if(dimension != null) {
                        lastExistingParent = dimension;
                        continue;
                    } else {
                        found = true;
                        //From this point they need to be created
                    }
                }

                // Prepare and save new dimension
                gdr.save( getDeepCopy(lastExistingParent, subFolder, rootPath.toString(), author, domainId) );
            }
        }
    }

    private GalleryDimension getDeepCopy(GalleryDimension source, String name, String rootPath, String author, int domainId) {
        GalleryDimension newDimension = new GalleryDimension();

        NullAwareBeanUtils.copyProperties(source, newDimension);
        newDimension.setId(null); // Set ID to null, so it will be saved as new dimension

        // Custom settings
        newDimension.setName(name);
        newDimension.setAuthor(author);
        newDimension.setPath(rootPath);
        newDimension.setDomainId(domainId);
        newDimension.setDate(new Date());

        return newDimension;
    }

    private String getBaseGalleryPath() {
        return Constants.getString("imagesRootDir") + "/" + Constants.getString("galleryDirName");
    }
}