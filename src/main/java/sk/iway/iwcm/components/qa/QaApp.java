package sk.iway.iwcm.components.qa;

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
import sk.iway.iwcm.qa.QADB;
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

@WebjetComponent("sk.iway.iwcm.components.qa.QaApp")
@WebjetAppStore(
    nameKey = "components.qa.title",
    descKey = "components.app-social_icon.desc",
    itemKey = "cmp_qa",
    imagePath = "/components/qa/editoricon.png",
    galleryImages = "/components/qa/",
    componentPath = "/components/qa/qa.jsp, /components/qa/qa-ask.jsp",
    customHtml = "/apps/qa/admin/editor-component.html")
@DataTableTabs(tabs = {
    @DataTableTab(id = "basic", title = "components.universalComponentDialog.title", selected = true),
    @DataTableTab(id = "style", title = "components.roots.new.style", content = ""),
    @DataTableTab(id = "componentIframeWindowTabList", title = "qa.roots.new", content = ""),
})
@Getter
@Setter
public class QaApp  extends WebjetComponentAbstract{

    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "basic", title = "components.qa.insert", editor = {
        @DataTableColumnEditor(options = {
            @DataTableColumnEditorAttr(key = "components.qa.questions_answers2", value = "qa"),
            @DataTableColumnEditorAttr(key = "components.qa.question_form", value = "qa-ask")
        })
    })
    private String field;

    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "basic", title = "components.qa.group_name")
    private String groupName;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, tab = "basic", title = "components.qa.page_size")
    private Integer pageSize = 10;

    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "basic", title = "components.qa.sortBy", editor = {
        @DataTableColumnEditor(options = {
            @DataTableColumnEditorAttr(key = "components.qa.sort.byDate", value = "1"),
            @DataTableColumnEditorAttr(key = "components.qa.sort.byPriority", value = "1")
        })
    })
    private Integer sortBy;


    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "basic", title = "components.qa.sortOrder", editor = {
        @DataTableColumnEditor(options = {
            @DataTableColumnEditorAttr(key = "components.qa.sort.asc", value = "asc"),
            @DataTableColumnEditorAttr(key = "components.qa.sort.desc", value = "desc")
        })
    })
    private String sortOrder;


    // second options


    @DataTableColumn(inputType = DataTableColumnType.MULTISELECT, tab = "basic", title = "components.qa.show_fields", editor = {
        @DataTableColumnEditor(options = {
            @DataTableColumnEditorAttr(key = "user.Name", value = "name"),
            @DataTableColumnEditorAttr(key = "user.company", value = "company"),
            @DataTableColumnEditorAttr(key = "user.phone", value = "phone"),
            @DataTableColumnEditorAttr(key = "user.email", value = "email")
        })
    })
    private String show = "name+company+phone+email";

    @DataTableColumn(inputType = DataTableColumnType.MULTISELECT, tab = "basic", title = "components.qa.required_fields", editor = {
        @DataTableColumnEditor(options = {
            @DataTableColumnEditorAttr(key = "user.Name", value = "name"),
            @DataTableColumnEditorAttr(key = "user.company", value = "company"),
            @DataTableColumnEditorAttr(key = "user.phone", value = "phone"),
            @DataTableColumnEditorAttr(key = "user.email", value = "email")
        })
    })
    private String required = "name+email";

    // second iframe

    @DataTableColumn(inputType = DataTableColumnType.IMAGE_RADIO, title = "components.qa.displayType", tab = "style", className = "image-radio-horizontal image-radio-fullwidth")
    private Integer displayType = 1;

    @DataTableColumn(inputType = DataTableColumnType.IMAGE_RADIO, title = "components.qa.displayStyle", tab = "style", className = "image-radio-horizontal image-radio-fullwidth")
    private String style = "01";


    @Override
    public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
        Map<String, List<OptionDto>> options = new HashMap<>();

        List<OptionDto> groupOptions = new ArrayList<>();
        List<LabelValueDetails> groups = QADB.getQAGroups(request);
        for (LabelValueDetails group : groups){
            groupOptions.add(new OptionDto(group.getLabel(), group.getLabel(), null));
        }
        options.put("groupName", groupOptions);

        options.put("displayType", DatatableTools.getImageRadioOptions("/components/qa/admin-display-type/"));
        options.put("style", DatatableTools.getImageRadioOptions("/components/qa/admin-styles/"));
        return options;
    }

    // third iframe
    @DataTableColumn(inputType = DataTableColumnType.IFRAME, tab = "componentIframeWindowTabList", title="&nbsp;")
    private String iframe  = "/components/qa/admin_list.jsp";
    
}
