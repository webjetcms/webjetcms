package sk.iway.iwcm.components.appslitslider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

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

@WebjetComponent("sk.iway.iwcm.components.appslitslider.SlitSliderApp")
@WebjetAppStore(
    nameKey = "components.app-slit_slider.title",
    descKey = "components.app-slit_slider.desc",
    itemKey = "app-slit_slider",
    imagePath = "/components/app-slit_slider/editoricon.png",
    galleryImages = "/components/app-slit_slider/",
    componentPath = "/components/app-slit_slider/news.jsp",
    customHtml = "/apps/app-slit_slider/admin/editor-component.html"
)
@Getter
@Setter
@DataTableTabs(tabs = {
        @DataTableTab(id = "basic", title = "components.app-impress_slideshow.editor_components.styleAndSettings", selected = true),
        @DataTableTab(id = "files", title = "components.slider.files"),
})
public class SlitSliderApp extends WebjetComponentAbstract {

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "editor.table.height", tab = "basic")
    private Integer nivoSliderHeight = 500;

    @DataTableColumn(
        inputType = DataTableColumnType.STATIC_TEXT,
        tab = "basic",
        title="components.app-slit_slider.admin.textHeadingSettings",
        editor = { @DataTableColumnEditor( attr = { @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before") } ) }
    )
    private String explain;

    @DataTableColumn(inputType = DataTableColumnType.IMAGE_RADIO, title =  "components.app-slit_slider.admin.fontAlign", tab = "basic", className = "image-radio-horizontal")
    private String headingAlign = "left";

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.app-slit_slider.admin.fontSize", tab = "basic")
    private Integer headingSize = 70;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.app-slit_slider.admin.fontMarginTop", tab = "basic")
    private Integer headingMargin = 0;

    @DataTableColumn(
        inputType = DataTableColumnType.STATIC_TEXT,
        tab = "basic",
        title="components.app-slit_slider.admin.textSubHeadingSettings",
        editor = { @DataTableColumnEditor( attr = { @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before") } ) }
    )
    private String explain2;

    @DataTableColumn(inputType = DataTableColumnType.IMAGE_RADIO, title = "components.app-slit_slider.admin.fontAlign", tab = "basic", className = "image-radio-horizontal")
    private String subHeadingAlign = "left";

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.app-slit_slider.admin.fontSize", tab = "basic")
    private Integer subHeadingSize = 30;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.app-slit_slider.admin.fontMarginTop", tab = "basic")
    private Integer subHeadingMargin = 0;

    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, tab = "files", title="&nbsp;", className = "dt-json-editor",editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.components.appslitslider.SlitSliderItem"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-localJson", value = "true")
            }
        )})
    private String editorData = null;

    @Override
    public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
        Map<String, List<OptionDto>> options = new HashMap<>();

        options.put("headingAlign", DatatableTools.getAlignOptions());
        options.put("subHeadingAlign", DatatableTools.getAlignOptions());

        return options;
    }

}
