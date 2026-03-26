package sk.iway.iwcm.components.appfacebookcomments;

import jakarta.validation.constraints.Min;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@WebjetComponent("sk.iway.iwcm.components.appfacebookcomments.FacebookCommentsApp")
@WebjetAppStore(
    nameKey = "components.app-facebook_comments.title", 
    descKey = "components.app-facebook_comments.desc", 
    itemKey = "cmp_app-facebook_comments", 
    imagePath = "/components/app-facebook_comments/editoricon.png", 
    galleryImages = "/components/app-facebook_comments/", 
    componentPath = "/components/app-facebook_comments/facebook_commnets.jsp"
)
@Getter
@Setter
public class FacebookCommentsApp extends WebjetComponentAbstract {
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        tab = "basic",
        title="components.app-facebook_comments.numberComments",
        editor = {
        @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "placeholder", value = "5")
            }
        )
    })
    private int numberComments = 5;

    @Min(0)
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        tab = "basic",
        title="components.app-facebook_comments.widthComments",
        editor = {
        @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "placeholder", value = "980")
            }
        )
    })
    private int widthComments = 980;
}
