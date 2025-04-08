package sk.iway.iwcm.components.rating;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@WebjetComponent("sk.iway.iwcm.components.rating.RatingApp")
@WebjetAppStore(nameKey = "components.rating.title", descKey = "components.rating.desc", itemKey = "cmp_rating", imagePath = "/components/rating/editoricon.png", galleryImages = "/components/rating/", componentPath = "/components/rating/rating_form.jsp,/components/rating/rating_page.jsp,/components/rating/rating_top_users.jsp,/components/rating/rating_top_pages.jsp", customHtml = "/apps/rating/admin/editor-component.html")

@Getter
@Setter
public class RatingApp extends WebjetComponentAbstract {

    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.rating.type", tab = "basic", editor = {
            @DataTableColumnEditor(options = {
                    @DataTableColumnEditorAttr(key = "components.rating.rating_form", value = "rating_form"),
                    @DataTableColumnEditorAttr(key = "components.rating.show_rating", value = "rating_page"),
                    @DataTableColumnEditorAttr(key = "components.rating.top_users", value = "rating_top_users"),
                    @DataTableColumnEditorAttr(key = "components.rating.top_docid", value = "rating_top_pages")
            })
    })
    private String ratingType;

    @DataTableColumn(inputType = DataTableColumnType.STATIC_TEXT, tab = "basic", title = "components.rating.rating_form_desc")
    private String form1Description;

    @DataTableColumn(inputType = DataTableColumnType.STATIC_TEXT, tab = "basic", title = "components.rating.show_rating_desc")
    private String form2Description;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "components.rating.check_logon", tab = "basic")
    private Boolean checkLogon = true;

    @DataTableColumn(inputType = DataTableColumnType.JSON, title = "components.rating.rating_doc_id", tab = "basic", className = "dt-tree-page")
    private DocDetails ratingDocId;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.rating.range", tab = "basic")
    private Integer range = 10;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.rating.display_users", tab = "basic")
    private Integer usersLength = 10;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.rating.display_docs", tab = "basic")
    private Integer docsLength = 10;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.rating.period", tab = "basic")
    private Integer period = 7;
}
