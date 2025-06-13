package sk.iway.iwcm.components.appslitslider;

import java.util.Comparator;
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
@WebjetAppStore(nameKey = "components.app-slit_slider.title", descKey = "components.app-slit_slider.desc", itemKey = "app-slit_slider", imagePath = "/components/app-slit_slider/editoricon.png", galleryImages = "/components/app-slit_slider/", componentPath = "/components/app-slit_slider/news.jsp", customHtml = "/apps/app-slit_slider/admin/editor-component.html")
@Getter
@Setter
@DataTableTabs(tabs = {
        @DataTableTab(id = "basic", title = "components.universalComponentDialog.title", selected = true),
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
    private String headAlign;


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
    private String subHeadingAlign;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.app-slit_slider.admin.fontSize", tab = "basic")
    private Integer subHeadingSize = 30;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.app-slit_slider.admin.fontMarginTop", tab = "basic")
    private Integer subHeadingMargin = 0;

    @DataTableColumn(inputType = DataTableColumnType.IFRAME, tab = "files")
    private String iframe  = "/components/app-slit_slider/add-item.jsp";

    @DataTableColumn(inputType = DataTableColumnType.HIDDEN, tab = "basic", className = "dt-json-editor")
    private String editorData;

    @Override
    public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
        Map<String, List<OptionDto>> options = new HashMap<>();

        List<OptionDto> optionsMap = DatatableTools.getImageRadioOptions("/components/_common/custom_styles/images/align/");
        optionsMap.sort(Comparator.comparingInt(option -> {
            switch (option.getValue()) {
                case "icon_align_left": return 1;
                case "icon_align_center": return 2;
                case "icon_align_right": return 3;
                default: return 99;
            }
        }));
        options.put("headAlign", optionsMap);
        options.put("subHeadingAlign", optionsMap);
        return options;
    }

}
