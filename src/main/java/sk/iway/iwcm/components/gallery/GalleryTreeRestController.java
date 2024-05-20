package sk.iway.iwcm.components.gallery;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.admin.jstree.JsTreeItem;
import sk.iway.iwcm.admin.jstree.JsTreeMoveItem;
import sk.iway.iwcm.admin.jstree.JsTreeRestController;
import sk.iway.iwcm.system.spring.NullAwareBeanUtils;

import java.util.*;

import javax.servlet.http.HttpServletRequest;

/**
 * GalleryTreeRestController
 */
@RestController
@Datatable
@RequestMapping(value = "/admin/rest/components/gallery/tree")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('menuGallery')")
public class GalleryTreeRestController extends JsTreeRestController<GalleryDimension> {

    private final GalleryDimensionRepository repository;
    private final GalleryTreeService galleryTreeService;

    public GalleryTreeRestController(GalleryDimensionRepository repository, GalleryTreeService galleryTreeService) {
        this.repository = repository;
        this.galleryTreeService = galleryTreeService;
    }

    @Override
    protected void tree(Map<String, Object> result, JsTreeMoveItem item) {
        String url = item.getUrl();
        if (Tools.isEmpty(url)) url = Constants.getString("imagesRootDir");

        if (Tools.isNotEmpty(Constants.getString("imagesRootDir")) && url.startsWith(Constants.getString("imagesRootDir"))==false) {
            result.put("result", false);
            result.put("error", getProp().getText("java.GalleryTreeRestController.directory_id_not_in_images", url));
            return;
        }

        if (!FileTools.isDirectory(url)) {
            result.put("result", false);
            result.put("error", getProp().getText("java.GalleryTreeRestController.path_doesnt_exist", url));
            return;
        }

        List<GalleryJsTreeItem> items = galleryTreeService.getItems(url);

        result.put("result", true);
        result.put("items", items);
    }

    @Override
    protected void move(Map<String, Object> result, JsTreeMoveItem item) {
        JsTreeItem original = item.getNode().getOriginal();
        if (original == null) {
            result.put("result", false);
            result.put("error", getProp().getText("java.GroupsTreeRestController.move.json_original_missing"));
            return;
        }

        String virtualPath = original.getVirtualPath();

        Optional<GalleryDimension> firstByPath = repository.findFirstByPathAndDomainId(virtualPath, CloudToolsForCore.getDomainId());
        if (firstByPath.isPresent()) {
            try {
                GalleryDimension galleryDimension = firstByPath.get();
                String originalPath = galleryDimension.getPath();
                String newPath = item.getParent() + "/" + galleryDimension.getNameFromPath();
                galleryDimension.setPath(newPath);
                repository.save(galleryDimension);

                IwcmFile file = new IwcmFile(Tools.getRealPath(virtualPath));
                boolean renamed = file.renameTo(new IwcmFile(Tools.getRealPath(newPath)));
                result.put("result", renamed);

                if (!renamed) {
                    result.put("error", getProp().getText("java.GroupsTreeRestController.move.renamed_failed"));
                    // Vraciam do povodneho adresaru aj DB entitu
                    galleryDimension.setPath(originalPath);
                    repository.save(galleryDimension);
                }

                return;
            } catch (Exception e) {
                Logger.error(GalleryTreeRestController.class, e);
                result.put("result", false);
                result.put("error", getProp().getText("java.GroupsTreeRestController.move.save_failed"));
                return;
            }
        }

        result.put("result", true);
    }

    @Override
    protected void save(Map<String, Object> result, GalleryDimension item) {
        String path = item.getPath();
        checkPathAccess(result, path);
        if (result.containsKey("result") && result.get("result").equals(false)) {
            return;
        }

        galleryDimensionCreateUpdate(result, item);
    }

    @Override
    protected void delete(Map<String, Object> result, GalleryDimension item) {
        String virtualPath = item.getPath();
        checkPathAccess(result, virtualPath);
        if (result.containsKey("result") && result.get("result").equals(false)) {
            return;
        }

        Optional<GalleryDimension> firstByPath = repository.findFirstByPathAndDomainId(virtualPath, CloudToolsForCore.getDomainId());
        firstByPath.ifPresent(repository::delete);

        IwcmFile iwcmFile = new IwcmFile(Tools.getRealPath(virtualPath));
        if (iwcmFile.exists()) {
            boolean deleted = FileTools.deleteDirTree(iwcmFile);
            if (deleted == false) {
                result.put("result", false);
                result.put("error", getProp().getText("java.GalleryRestController.directory_cannot_be_deleted", virtualPath));
            }
        }
        else {
            result.put("result", false);
            result.put("error", getProp().getText("java.GalleryRestController.directory_not_found", virtualPath));
        }

        if (result.containsKey("result") && result.get("result").equals(false)) {
            return;
        }

        result.put("result", true);
    }

    private void checkPathAccess(Map<String, Object> map, String virtualPath) {
        if (!virtualPath.startsWith("/images")) {
            map.put("result", false);
            map.put("error", getProp().getText("java.GalleryTreeRestController.directory_id_not_in_images", virtualPath));
        }
    }

    private void galleryDimensionCreateUpdate(Map<String, Object> result, GalleryDimension item) {
        GalleryDimension galleryDimension = getGalleryDimension(item);

        String directoryName = DB.internationalToEnglish(DocTools.removeChars(item.getName()));
        if (item.getId() == null || item.getId() == 0) {
            String virtualPath = item.getPath() + "/" + directoryName;
            IwcmFile file = new IwcmFile(Tools.getRealPath(virtualPath));
            if (file.exists()) {
                result.put("result", false);
                result.put("error", getProp().getText("java.GalleryTreeRestController.directory_already_exists", virtualPath));
                return;
            }

            file.mkdirs();
            galleryDimension.setPath(file.getVirtualPath());
        }
        else {
            renameDirectory(result, item);
        }

        if (result.containsKey("result") && result.get("result").equals(false)) {
            return;
        }

        galleryDimensionSave(result, galleryDimension);
    }

    private void renameDirectory(Map<String, Object> result, GalleryDimension item) {
        String directoryName = DB.internationalToEnglish(DocTools.removeChars(item.getName()));
        IwcmFile file = new IwcmFile(Tools.getRealPath(item.getPath()));
        if (file.getName().equals(directoryName)) {
            return;
        }

        String virtualPath = file.getParentFile().getVirtualPath() + "/" + directoryName;
        IwcmFile newFile = new IwcmFile(Tools.getRealPath(virtualPath));
        if (newFile.exists()) {
            result.put("result", false);
            result.put("error", getProp().getText("java.GalleryTreeRestController.directory_already_exists", virtualPath));
            return;
        }

        boolean ok = file.renameTo(newFile);
        if (!ok) {
            result.put("result", false);
            result.put("error", getProp().getText("java.GalleryTreeRestController.cannot_rename_to", virtualPath));
        }
    }

    private void galleryDimensionSave(Map<String, Object> result, GalleryDimension galleryDimension) {
        try {
            if (galleryDimension.getId() == 0) {
                galleryDimension.setId(null);
            }
            if (galleryDimension.getDate() == null) {
                galleryDimension.setDate(new Date());
            }
            repository.save(galleryDimension);
            result.put("result", true);
        }
        catch (Exception e) {
            Logger.error(GalleryTreeRestController.class, e);
            result.put("result", false);
            result.put("error", e.getMessage());
        }
    }

    private GalleryDimension getGalleryDimension(GalleryDimension item) {
        Optional<GalleryDimension> galleryDimensionFromDB = getGalleryDimensionFromDB(item);
        GalleryDimension galleryDimension = galleryDimensionFromDB.orElseGet(GalleryDimension::new);

        NullAwareBeanUtils.copyProperties(item, galleryDimension);

        return galleryDimension;
    }

    private Optional<GalleryDimension> getGalleryDimensionFromDB(GalleryDimension item) {
        if (item.getId() != null && item.getId() > 0) {
            return repository.findFirstByIdAndDomainId(item.getId(), CloudToolsForCore.getDomainId());
        }

        if (Tools.isNotEmpty(item.getPath())) {
            return repository.findFirstByPathAndDomainId(item.getPath(), CloudToolsForCore.getDomainId());
        }

        return Optional.empty();
    }

    @Override
    public boolean checkAccessAllowed(HttpServletRequest request) {
        //TODO: kontrola prav
        return true;
    }
}