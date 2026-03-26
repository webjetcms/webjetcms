package sk.iway.iwcm.components.apptestimonials;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.editor.rest.ComponentRequest;
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

@WebjetComponent("sk.iway.iwcm.components.apptestimonials.TestimonialsApp")
@WebjetAppStore(
    nameKey = "components.app-testimonials.title",
    descKey = "components.app-testimonials.desc",
    itemKey = "app-testimonials",
    imagePath = "/components/app-testimonials/editoricon.png",
    componentPath = "/components/app-testimonials/news.jsp",
    customHtml = "/apps/testimonials/admin/editor-component.html"
)
@Getter
@Setter
@DataTableTabs(tabs = {
        @DataTableTab(id = "basic", title = "components.app-testimonials.visualStyle", selected = true),
        @DataTableTab(id = "advanced", title = "components.app-testimonials.styleAndSettings"),
        @DataTableTab(id = "items", title = "components.app-testimonials.items"),
})
public class TestimonialsApp extends WebjetComponentAbstract {

    @DataTableColumn(inputType = DataTableColumnType.IMAGE_RADIO, title = "&nbsp;", tab = "basic", className = "image-radio-horizontal image-radio-fullwidth")
    private String style = "03";

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, tab = "advanced", title="components.app-testimonials.showPhoto")
    private Boolean showPhoto = Boolean.TRUE;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, tab = "advanced", title="components.app-testimonials.showName")
    private Boolean showName = Boolean.TRUE;

    @DataTableColumn(inputType = DataTableColumnType.COLOR, tab = "advanced", title = "components.app-testimonials.nameColor")
    private String nameColor = "#000";

    @DataTableColumn(inputType = DataTableColumnType.COLOR, tab = "advanced", title = "components.app-testimonials.textColor")
    private String textColor = "#000";

    @DataTableColumn(inputType = DataTableColumnType.COLOR, tab = "advanced", title = "components.app-testimonials.backgroundColor")
    private String backgroundColor = "#fff";

    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, tab = "items", title="&nbsp;", className = "dt-json-editor",editor = { @DataTableColumnEditor(
        attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.components.apptestimonials.TestimonialItem"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-localJson", value = "true")
            }
        )})
    private String editorData = null;

    @Override
    public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
        Map<String, List<OptionDto>> options = new HashMap<>();
        options.put("style", DatatableTools.getImageRadioOptions("/components/app-testimonials/admin-styles/"));
        return options;
    }
}
