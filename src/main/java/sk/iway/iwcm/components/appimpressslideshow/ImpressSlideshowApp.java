package sk.iway.iwcm.components.appimpressslideshow;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableTab;
import sk.iway.iwcm.system.datatable.annotations.DataTableTabs;

@WebjetComponent("sk.iway.iwcm.components.appimpressslideshow.ImpressSlideshowApp")
@WebjetAppStore(
    nameKey = "components.app-impress_slideshow.title",
    descKey = "components.app-impress_slideshow.desc",
    itemKey = "cmp_app-impress_slideshow",
    imagePath = "/components/app-impress_slideshow/editoricon.png",
    galleryImages = "/components/app-impress_slideshow/",
    componentPath = "/components/app-impress_slideshow/news.jsp"
)
@DataTableTabs(tabs = {
    @DataTableTab(id = "style", title = "components.news.styleAndSettings", selected = true),
    @DataTableTab(id = "tabLink2", title = "components.news.items"),
})
@Getter
@Setter
public class ImpressSlideshowApp extends WebjetComponentAbstract{

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, tab = "style", title = "editor.table.height")
    private Integer nivoSliderHeight = 400;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, tab = "style", title = "components.app-impress_slideshow.imageWidth")
    private Integer imageWidth = 400;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, tab = "style", title = "components.app-impress_slideshow.imageHeight")
    private Integer imageHeight = 300;

    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, tab = "tabLink2", title="&nbsp;", className = "dt-json-editor",editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.components.appimpressslideshow.ImpressSlideshowItem"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-jsonEditor", value = "true")
            }
        )})
    private String editorData = null;

}