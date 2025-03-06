package sk.iway.iwcm.components.inquiry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.LabelValueDetails;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.editor.rest.ComponentRequest;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.inquiry.InquiryDB;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.DatatableTools;
import sk.iway.iwcm.system.datatable.OptionDto;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableTab;
import sk.iway.iwcm.system.datatable.annotations.DataTableTabs;


@WebjetComponent("sk.iway.iwcm.components.inquiry.InquiryApp")
@WebjetAppStore(
    nameKey = "components.inquiry.title",
    descKey = "components.inquiry.desc",
    itemKey= "cmp_inquiry",
    imagePath = "/components/inquiry/editoricon.png",
    galleryImages = "/components/inquiry/",
    componentPath = "/components/inquiry/inquiry.jsp",
    customHtml = "/apps/inquiry/admin/editor-component.html"
)
@DataTableTabs(tabs = {
    @DataTableTab(id = "basic", title = "components.universalComponentDialog.title", selected = true),
    @DataTableTab(id = "styleSelectArea", title = "components.roots.new.style", content = ""),
    @DataTableTab(id = "componentIframeWindowTabList", title = "menu.inquiry", content = ""),
})
@Getter
@Setter
public class InquiryApp extends WebjetComponentAbstract {

    @DataTableColumn(
        inputType = DataTableColumnType.MULTISELECT,
        tab = "basic",
        title="components.tips.select_group"
    )
    private String[] group;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, tab = "basic", title="components.inquiry.imagesLength")
    private int imagesLength = 10;

    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "basic", title="components.inquiry.percentageFormat", editor = {
        @DataTableColumnEditor(
            options = {
                @DataTableColumnEditorAttr(key = "0", value = "0"),
                @DataTableColumnEditorAttr(key = "0.0", value = "0.0")
            }
        )
    })
    private String percentageFormat = "0";

    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        tab = "basic",
        title="components.inquiry.orderBy",
        editor = {
        @DataTableColumnEditor(
            options = {
                @DataTableColumnEditorAttr(key = "components.inquiry.order.answer_id", value = "answer_id"),
                @DataTableColumnEditorAttr(key = "components.inquiry.order.answer_text", value = "answer_text"),
                @DataTableColumnEditorAttr(key = "components.inquiry.order.answer_clicks", value = "answer_clicks")
            }
        )
    })
    private String orderBy = "answer_id";

    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        tab = "basic",
        title = "&nbsp;",
        editor = {
        @DataTableColumnEditor(
            options = {
                @DataTableColumnEditorAttr(key = "components.inquiry.orderAsc", value = "ascending"),
                @DataTableColumnEditorAttr(key = "components.inquiry.orderDesc", value = "descending")
            }
        )
    }
    )
    private String order = "ascending";

    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "basic", title="components.inquiry.width")
    private String width = "100%";

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN_TEXT,
        title = "components.inquiry.random",
        tab = "basic"
    )
    private Boolean random = true;

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN_TEXT,
        title = "components.inquiry.display_total_clicks",
        tab = "basic"
    )
    private Boolean totalClicks = true;;

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN_TEXT,
        title = "components.inquiry.display_vote_results",
        tab = "basic"
    )
    private Boolean displayVoteResults = true;

    @DataTableColumn(
        inputType = DataTableColumnType.IMAGE_RADIO,
        title = "components.roots.new.style",
        tab = "styleSelectArea",
        className = "image-radio-horizontal image-radio-fullwidth"
    )
    private String style = "01";

    @DataTableColumn(
        inputType = DataTableColumnType.IMAGE_RADIO,
        title = "components.catalog.color",
        tab = "styleSelectArea",
        className = "image-radio-horizontal image-radio-fullwidth"
    )
    private String color = "01";

    @DataTableColumn(inputType = DataTableColumnType.IFRAME, tab = "componentIframeWindowTabList", title="&nbsp;")
    private String iframe  = "/components/inquiry/admin_inquiry_list.jsp";

    @Override
    public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
        Map<String, List<OptionDto>> options = new HashMap<>();

        //group options
        List<OptionDto> groupOptions = new ArrayList<>();
        Prop prop = Prop.getInstance(request);
        groupOptions.add(new OptionDto(prop.getText("components.inquiry.fromTemplate"), "fromTemplate", null));
        List<LabelValueDetails> groups = InquiryDB.getQuestionGroupsByUser(request);
        for (LabelValueDetails group : groups){
            groupOptions.add(new OptionDto(group.getLabel(), group.getLabel(), null));
        }
        options.put("group", addCurrentValueToOptions(groupOptions, getGroup()));

        //style & color options
        options.put("style", DatatableTools.getImageRadioOptions("/components/inquiry/admin-styles/"));
        options.put("color", DatatableTools.getImageRadioOptions("/components/inquiry/admin-colors/"));

        return options;
    }

}
