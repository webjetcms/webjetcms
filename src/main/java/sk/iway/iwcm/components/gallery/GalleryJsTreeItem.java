package sk.iway.iwcm.components.gallery;

import com.fasterxml.jackson.annotation.JsonProperty;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.jstree.JsTreeItem;
import sk.iway.iwcm.admin.jstree.JsTreeItemState;
import sk.iway.iwcm.admin.jstree.JsTreeItemType;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.io.IwcmFile;

import java.util.Optional;

public class GalleryJsTreeItem extends JsTreeItem {
    @JsonProperty("galleryDimension")
    private GalleryDimension galleryDimension;

    public GalleryJsTreeItem(IwcmFile f, String currentDir, GalleryDimensionRepository repository, Identity user) {
        setId(f.getVirtualPath());
        setChildren(f.listFiles(IwcmFile::isDirectory).length > 0);
        setIcon("ti ti-folder-filled");
        setVirtualPath(f.getVirtualPath());
        JsTreeItemState jsTreeItemState = new JsTreeItemState();

        // defaultne otvoreny adresar gallery
        if (currentDir==null || Tools.isEmpty(currentDir)) currentDir = "/images/gallery";
        //remove last /
        if (currentDir.length()>2 && currentDir.endsWith("/")) currentDir = currentDir.substring(0, currentDir.length()-1);

        if (currentDir.startsWith(f.getVirtualPath())) {
            jsTreeItemState.setOpened(true);
            if (currentDir.equals(f.getVirtualPath())) jsTreeItemState.setSelected(true);
        }

        setState(jsTreeItemState);
        setType(JsTreeItemType.GALLERY);

        Optional<GalleryDimension> galleryDimensionOptional = repository.findFirstByPathAndDomainId(getVirtualPath(), CloudToolsForCore.getDomainId());
        if (galleryDimensionOptional.isPresent()) {
            galleryDimension = galleryDimensionOptional.get();
        }
        else {
            galleryDimension = GalleryDimenstionRestController.getNewEntity(f.getVirtualPath());


            setIcon("ti ti-folder");
        }
        if (Tools.isEmpty(galleryDimension.getName())) {
            galleryDimension.setName(f.getName());
        }
        galleryDimension.setPath(f.getVirtualPath());
        setGalleryDimension(galleryDimension);
        setText(galleryDimension.getName());

        if (GalleryDimenstionRestController.isFolderEditable(galleryDimension.getPath(), user)==false) {
            setIcon("ti ti-folder-x");
        }
    }

    public GalleryDimension getGalleryDimension() {
        return galleryDimension;
    }

    public void setGalleryDimension(GalleryDimension galleryDimension) {
        this.galleryDimension = galleryDimension;
    }
}
