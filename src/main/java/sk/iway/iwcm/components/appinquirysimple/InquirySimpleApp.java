package sk.iway.iwcm.components.appinquirysimple;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.PageParams;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.editor.rest.ComponentRequest;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableTab;
import sk.iway.iwcm.system.datatable.annotations.DataTableTabs;

@WebjetComponent("sk.iway.iwcm.components.appinquirysimple.InquirySimpleApp")
@WebjetAppStore(
        nameKey = "components.inquirysimple.title",
        descKey = "components.inquirysimple.desc",
        itemKey = "menuInquiry",
        imagePath = "/components/inquirysimple/editoricon.png",
        galleryImages = "/components/inquirysimple",
        componentPath = "/components/inquirysimple/inquiry.jsp",
        customHtml = "/apps/inquiry-simple/admin/editor-component.html"
)
@DataTableTabs(tabs = {
        @DataTableTab(id = "basic", title = "components.slider.settings", selected = true),
        @DataTableTab(id = "answers", title = "inquiry.answer2"),
})
@Getter
@Setter
public class InquirySimpleApp extends WebjetComponentAbstract {

    /* BASIC TAB */

    @NotBlank
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.inquirysimple.name", tab = "basic")
    private String name;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "components.inquirysimple.active", tab = "basic")
    private boolean active = true;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "components.inquirysimple.multiAnswer", tab = "basic")
    private boolean multiAnswer = false;

    /* ANSWERS TAB */

    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, tab = "answers", title="&nbsp;", className = "dt-json-editor", editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.components.appinquirysimple.AnswerItem"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-localJson", value = "true")
            }
        )})
    private String editorData = null;

    @DataTableColumn(inputType = DataTableColumnType.DISABLED, title = "components.inquiry_simple.id")
    private String formId;

    @Override
    public void initAppEditor(ComponentRequest componentRequest, HttpServletRequest request) {
        PageParams pageParams = new PageParams(componentRequest.getParameters());

        String formId = pageParams.getValue("formId", "");
        if(Tools.isEmpty(formId)) this.formId = UUID.randomUUID().toString();
        else this.formId = formId;

        super.initAppEditor(componentRequest, request);
    }
}