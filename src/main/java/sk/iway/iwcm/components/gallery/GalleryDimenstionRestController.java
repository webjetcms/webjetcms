package sk.iway.iwcm.components.gallery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import org.springframework.data.domain.Pageable;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.AdminTools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.FileBrowserTools;
import sk.iway.iwcm.gallery.GalleryDB;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@Datatable
@RequestMapping(value = "/admin/rest/components/gallery-dimension")
public class GalleryDimenstionRestController extends DatatableRestControllerV2<GalleryDimension, Long> {

    private final GalleryDimensionRepository repository;

    @Autowired
    public GalleryDimenstionRestController(GalleryDimensionRepository repository) {
        super(repository);
        this.repository = repository;
    }

    @Override
    public Page<GalleryDimension> getAllItems(Pageable pageable) {

        int dimensionId = Tools.getIntValue(getRequest().getParameter("dimensionId"), -1);
        List<GalleryDimension> list = new ArrayList<>();

        if(dimensionId != -1) {
            try {
                Optional<GalleryDimension> optional = repository.findFirstByIdAndDomainId(Long.valueOf(dimensionId), CloudToolsForCore.getDomainId());
                if (optional.isPresent()) {
                    GalleryDimension entity = optional.get();
                    list.add(entity);
                }
            } catch (Exception ex) {
                //zaznam uz neexistuje/bol zmazany
            }
        } else {
            //asi sa edituje zaznam, ktory nie je ulozeny v DB
            String path = getRequest().getParameter("path");
            if (Tools.isNotEmpty(path) && FileBrowserTools.hasForbiddenSymbol(path)==false) {
                list.add(getNewEntity(path));
            }
        }

        for(int i = 0; i < list.size(); i++) {

            if(list.get(i).getEditorFields() == null) {
                GalleryDimensionEditorFields editorFields = new GalleryDimensionEditorFields();
                list.get(i).setEditorFields(editorFields);
            }
        }

        return new sk.iway.iwcm.system.datatable.DatatablePageImpl<>(list);
    }

    @Override
    public GalleryDimension insertItem(GalleryDimension entity) {

        //Get entity path (this path we are setting in getOne) and add entity name to path, get entity from DB by path
        //If DB return entity, path is allready in use throw error, or set new path to entity
        String path = getPathForNewEntity(entity.getPath(), entity.getName());
        Identity user = getUser();

        Optional<GalleryDimension> tmp = repository.findFirstByPathAndDomainId(path, CloudToolsForCore.getDomainId());
        if(tmp.isPresent()) {
            throwError("components.gallery.path_error");
            return null;
        }
        if (!user.isFolderWritable(entity.getPath()+"/")) {
            throwError("user.rights.no_folder_rights");
        }

        entity.setPath(path);

        IwcmFile file = new IwcmFile(Tools.getRealPath(entity.getPath()));
        file.mkdirs();

        //domain id and date must by fill, cant be null
        if(entity.getDomainId() == null) {
            int domainId = CloudToolsForCore.getDomainId();
            entity.setDomainId(domainId);
        }

        if(entity.getDate() == null) {
            Date date = new Date(System.currentTimeMillis());
            entity.setDate(date);
        }

        repository.save(entity);
        return entity;
    }

    @Override
    public GalleryDimension editItem(GalleryDimension entity, long id) {
        if (!getUser().isFolderWritable(entity.getPath()+"/")) {
            throwError("user.rights.no_folder_rights");
        }

        GalleryDimension saved = super.editItem(entity, id);

        if (entity.getEditorFields().isForceResizeModeToSubgroups()) {
            GalleryDB.updateDirectoryDimToSubfolders(entity.getPath());
        }

        if (entity.getEditorFields().isForceWatermarkToSubgroups()) {
            List<GalleryDimension> subfolders = repository.findByPathLikeAndDomainId(entity.getPath()+"/%", CloudToolsForCore.getDomainId());
            for (GalleryDimension subfolder : subfolders) {
                subfolder.setWatermark(entity.getWatermark());
                subfolder.setWatermarkPlacement(entity.getWatermarkPlacement());
                subfolder.setWatermarkSaturation(entity.getWatermarkSaturation());
                repository.save(subfolder);
            }
        }

        if (entity.getEditorFields().isRegenerateImages() || entity.getEditorFields().isRegenerateWatermark()) {
            boolean recursive = entity.getEditorFields().isForceResizeModeToSubgroups() || entity.getEditorFields().isForceWatermarkToSubgroups();
            GalleryDB.resizePicturesInDirectory(entity.getPath(), recursive, getProp(), null);
        }

        return saved;
    }

    @Override
    public GalleryDimension getOneItem(long id) {

        GalleryDimension entity = super.getOneItem(id);

        //If entity is null, create entity instance for editor
        if(entity == null) {
            //Get dimension id (who is calling, who is parent gallery)
            int dimensionId = Tools.getIntValue(getRequest().getParameter("dimensionId"), -1);

            //If dimension id is not present, set default path from path parameter
            String parentPath = getRequest().getParameter("path");
            if (Tools.isEmpty(parentPath) || FileBrowserTools.hasForbiddenSymbol(parentPath)) parentPath = "/images/gallery"; //NOSONAR

            String forcePath = null;

            //If dimension id is present it's parent ID, copy properties
            GalleryDimension parentGallery = null;
            if(dimensionId != -1) {
                parentGallery = repository.findFirstByIdAndDomainId(Long.valueOf(dimensionId), CloudToolsForCore.getDomainId()).orElse(null);
                if (parentGallery == null) parentPath = "/images/gallery"; //NOSONAR
            } else {
                //find settings by parentPath recursivelly from repository, so search for /images/gallery/path/subfolder, then /images/gallery/path etc until root
                String[] pathParts = parentPath.split("/");
                for(int i = pathParts.length; i > 0; i--) {
                    String path = String.join("/", Arrays.copyOfRange(pathParts, 0, i));
                    Optional<GalleryDimension> optional = repository.findFirstByPathAndDomainId(path, CloudToolsForCore.getDomainId());
                    if (optional.isPresent()) {
                        parentGallery = optional.get();
                        forcePath = parentPath;
                        break;
                    }
                }
            }

            if (parentGallery != null) {
                entity = getNewEntity(parentGallery.getPath());
                entity.setName("");
                entity.setResizeMode(parentGallery.getResizeMode());
                entity.setImageWidth(parentGallery.getImageWidth());
                entity.setImageHeight(parentGallery.getImageHeight());
                entity.setNormalWidth(parentGallery.getNormalWidth());
                entity.setNormalHeight(parentGallery.getNormalHeight());
                entity.setWatermark(parentGallery.getWatermark());
                entity.setWatermarkPlacement(parentGallery.getWatermarkPlacement());
                entity.setWatermarkSaturation(parentGallery.getWatermarkSaturation());
            } else {
                //tu nemozeme poslat cestu, lebo by sa nastavil nazov, nevieme rozlisit ci sa jedna o edit, alebo o pridanie
                entity = getNewEntity("");
                entity.setPath(parentPath);
            }

            if (forcePath!=null) entity.setPath(forcePath);
        }

        return entity;
    }

    @Override
    public void beforeSave(GalleryDimension entity) {
        IwcmFile file = new IwcmFile(Tools.getRealPath(entity.getPath()));
        file.mkdirs();

        //domain id and date must by fill, cant be null
        if(entity.getDomainId() == null) {
            int domainId = CloudToolsForCore.getDomainId();
            entity.setDomainId(domainId);
        }

        if(entity.getDate() == null) {
            Date date = new Date(System.currentTimeMillis());
            entity.setDate(date);
        }
    }

    @Override
    public boolean beforeDelete(GalleryDimension entity) {
        //zmaz z disku subory a z DB zaznamy o obrazkoch

        Identity user = getUser();
        if (!user.isFolderWritable(entity.getPath()+"/")) {
            throwError("user.rights.no_folder_rights");
        }

        return true;
    }

    @Override
    public boolean deleteItem(GalleryDimension entity, long id) {
        boolean success = GalleryDB.deleteGallery(entity.getPath());
        if(success) {
            setForceReload(true);
        } else {
            throwError("components.gallery.path_delete_error");
        }
        return success;
    }

    /**
     * Returns new GalleryDimension entity with given path
     * @param path
     * @return
     */
    public static GalleryDimension getNewEntity(String path) {
        GalleryDimension entity = new GalleryDimension();
        entity.setId(Long.valueOf(-1));
        entity.setResizeMode("S");
        GalleryDimensionEditorFields editorFields = new GalleryDimensionEditorFields();
        entity.setEditorFields(editorFields);
        entity.setPath(path);
        entity.setDomainId(CloudToolsForCore.getDomainId());
        entity.setDate(new Date());

        int lomka = path.lastIndexOf("/");
        if (lomka > 1 && lomka < path.length()) entity.setName(path.substring(lomka+1));

        return entity;
    }

    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, GalleryDimension> target, Identity user, Errors errors, Long id, GalleryDimension entity) {

        String path = entity.getPath();

        if (target.isInsert()) {
            if (Tools.isEmpty(entity.getName())) {
                errors.rejectValue("errorField.name", "403", getProp().getText("datatables.field.required.error.js"));
                return;
            }

            //for insert we must add entity name to path
            path = getPathForNewEntity(path, entity.getName());
        }

        if (isFolderEditable(path, user)==false) {
            errors.rejectValue("errorField.path", "403", getProp().getText("components.gallery.folderIsNotEditable"));
        }
    }

    /**
     * Returns path for new entity (merging path with new name)
     * @param parent
     * @param name
     * @return
     */
    private static String getPathForNewEntity(String parent, String name) {
        return parent + "/" + DocTools.removeCharsDir(name, true).toLowerCase(); //NOSONAR
    }

    /**
     * Returns true if folder is editable.
     * Some folders like /images/DOMAIN-ALIAS is protected/not editable
     * @param path
     * @return
     */
    public static boolean isFolderEditable(String path, Identity user) {
        String domainAlias = AdminTools.getDomainNameFileAliasAppend();
        if (Tools.isNotEmpty(domainAlias)) {
            if (path.equals("/images"+domainAlias)) {
                return false;
            }
        }
        if (!user.isFolderWritable(path+"/")) {
            return false;
        }
        return true;
    }

}
