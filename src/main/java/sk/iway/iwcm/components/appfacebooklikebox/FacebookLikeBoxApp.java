package sk.iway.iwcm.components.appfacebooklikebox;

import javax.validation.constraints.Min;

import lombok.Getter;
import lombok.Setter;

import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;


@WebjetComponent("sk.iway.iwcm.components.appfacebooklikebox.FacebookLikeBoxApp")
@WebjetAppStore(
    nameKey = "components.app-facebook_like_box.title", 
    descKey = "components.app-facebook_like_box.desc", 
    itemKey = "cmp_app-facebook_like_box", 
    imagePath = "/components/app-facebook_like_box/editoricon.png", 
    galleryImages = "/components/app-facebook_like_box/", 
    componentPath = "/components/app-facebook_like_box/facebook_like_box.jsp"
)

@Getter
@Setter
public class FacebookLikeBoxApp extends WebjetComponentAbstract {

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        tab = "basic",
        title="components.app-facebook_like_box.dataHref")
    private String dataHrefLikeBox = "https://www.facebook.com/interway.sk/";

    @Min(0)
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        tab = "basic",
        title="components.app-facebook_like_box.width")
    private int widthLikeBox = 980;

    @Min(0)
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        tab = "basic",
        title="components.app-facebook_like_box.height")
    private Integer heightLikeBox = null;

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        tab = "basic",
        title="components.app-facebook_like_box.showFaces")
    private boolean showFacesLikeBox = true;

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        tab = "basic",
        title="components.app-facebook_like_box.showPost")
    private boolean showPostLikeBox = true;

}


