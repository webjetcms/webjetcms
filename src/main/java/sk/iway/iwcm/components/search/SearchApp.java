package sk.iway.iwcm.components.search;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@WebjetComponent("sk.iway.iwcm.components.search.SearchApp")
@WebjetAppStore(
    nameKey = "components.search.title",
    descKey = "components.search.desc",
    itemKey = "cmp_search",
    imagePath = "/components/search/editoricon.png",
    galleryImages = "/components/search/",
    componentPath = "/components/search/search.jsp,/components/search/lucene_search.jsp")
@Getter
@Setter
public class SearchApp extends WebjetComponentAbstract {

    @DataTableColumn(
        inputType = DataTableColumnType.JSON,
        title = "components.news.groupids",
        tab = "basic",
        sortAfter = "editorFields.groupDetails",
        className = "dt-tree-group-array")
    private List<GroupDetails> rootGroup;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "basic", title="components.search.results_per_page", editor = {
        @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "placeholder", value = "components.search.results_per_page")
            }
        )
    })
    private int perpage = 10;

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title = "components.search.check_duplicty",
        tab = "basic"
    )
    private Boolean checkDuplicity;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "components.formsimple.placeholder",
        tab = "basic",
        editor = {
            @DataTableColumnEditor(
                message = "components.formsimple.placeholderComment",
                attr = {
                    @DataTableColumnEditorAttr(key = "placeholder", value = "components.search.title")
                }
            )
        }
    )
    private String inputText = "";

    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        tab = "basic",
        title="components.search.order_by",
        editor = {
        @DataTableColumnEditor(
            options = {
                @DataTableColumnEditorAttr(key = "components.search.relevance", value = "sortPriority"),
                @DataTableColumnEditorAttr(key = "components.search.file_name", value = "title"),
                @DataTableColumnEditorAttr(key = "components.search.file_change", value = "lastUpdate")
            }
        )
    })
    private String orderType = "sortPriority";

    @DataTableColumn(
        inputType = DataTableColumnType.RADIO,
        title = "components.search.search_type",
        tab = "basic",
        editor = {
            @DataTableColumnEditor(
                options = {
                    @DataTableColumnEditorAttr(key = "components.inquiry.orderAsc", value = "asc"),
                    @DataTableColumnEditorAttr(key = "components.inquiry.orderDesc", value = "desc")
                }
            )
        }
    )
    private String order = "asc";

    @DataTableColumn(
        inputType = DataTableColumnType.RADIO,
        title = "editor.paste",
        tab = "basic",
        editor = {
            @DataTableColumnEditor(
                options = {
                    @DataTableColumnEditorAttr(key = "components.search.text_field", value = "form"),
                    @DataTableColumnEditorAttr(key = "components.search.results", value = "results"),
                    @DataTableColumnEditorAttr(key = "components.search.search_complete", value = "complete")
                }
            )
        }
    )
    @JsonProperty("sForm")
    private String sForm = "complete";
}