package sk.iway.iwcm.components.appfacebooklike;

import javax.validation.constraints.Min;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@WebjetComponent("sk.iway.iwcm.components.appfacebooklike.FacebookLikeApp")
@WebjetAppStore(
    nameKey = "components.app-facebook_like.title", 
    descKey = "components.app-facebook_like.desc", 
    itemKey = "cmp_app-facebook_like", 
    imagePath = "/components/app-facebook_like/editoricon.png", 
    galleryImages = "/components/app-facebook_like/", 
    componentPath = "/components/app-facebook_like/facebook_like.jsp"
)
@Getter
@Setter
public class FacebookLikeApp extends WebjetComponentAbstract {

    @DataTableColumn(
        inputType = DataTableColumnType.RADIO,
        title="components.app-facebook_like.lajkovat_popis",
        tab = "basic",
        editor = {
            @DataTableColumnEditor(
                options = {
                    @DataTableColumnEditorAttr(
                        key = "components.app-facebook_like.lajkovat_cely_web",
                        value = "lajkovat_cely_web"),
                    @DataTableColumnEditorAttr(
                        key = "components.app-facebook_like.lajkovat_aktualne",
                        value = "lajkovat_aktualne")
                }
            )
        }
    )
    private String dataHrefLike = "lajkovat_cely_web";

    @Min(0)
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="components.app-facebook_like.width",
        tab = "basic"
    )
    private Integer widthLike = 980;

    @DataTableColumn(
        inputType = DataTableColumnType.RADIO,
        title = "components.app-facebook_like.layout",
        tab = "basic",
        editor = {
            @DataTableColumnEditor(
                options = {
                    @DataTableColumnEditorAttr(
                        key = "components.app-facebook_like.layout.standard",
                        value = "standard"),
                    @DataTableColumnEditorAttr(
                        key = "components.app-facebook_like.layout.button_count",
                        value = "button_count")
                }
            )
        }
    )
    private String layoutLikeButton = "standard";

    @DataTableColumn(
        inputType = DataTableColumnType.RADIO,
        title = "components.app-facebook_like.actionType",
        tab = "basic",
        editor = {
            @DataTableColumnEditor(
                options = {
                    @DataTableColumnEditorAttr(
                        key = "components.app-facebook_like.actionType.like",
                        value = "like"),
                    @DataTableColumnEditorAttr(
                        key = "components.app-facebook_like.actionType.recommend",
                        value = "recommend")
                }
            )
        }
    )
    private String actionLikeButton = "like";






    

    
}
