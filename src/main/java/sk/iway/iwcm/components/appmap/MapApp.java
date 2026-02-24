package sk.iway.iwcm.components.appmap;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableTab;
import sk.iway.iwcm.system.datatable.annotations.DataTableTabs;

@WebjetComponent("sk.iway.iwcm.components.appmap.MapApp")
@WebjetAppStore(
        nameKey = "components.map.title",
        descKey = "components.map.desc",
        //itemKey = "menuWebpages",
        imagePath = "/components/map/editoricon.png",
        galleryImages = "/components/map/",
        componentPath = "/components/map/map.jsp",
        customHtml = "/apps/map/admin/editor-component.html"
)
@DataTableTabs(tabs = {
        @DataTableTab(id = "basic", title = "components.slider.settings", selected = true),
        @DataTableTab(id = "mapSettings", title = "components.map.settings"),
        @DataTableTab(id = "pinSettings", title = "components.map.pin.settings"),
})
@Getter
@Setter
public class MapApp extends WebjetComponentAbstract {

    /* TAB BASIC */
    @DataTableColumn(inputType = DataTableColumnType.STATIC_TEXT, tab = "basic", title="components.map.base_title")
    private String infoText;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "basic", title="components.map.address")
    private String object; // aka address

    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "basic", title="components.map.lat")
    private String latitude;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "basic", title="components.map.lon")
    private String longitude;

    /* TAB MAP SETTINGS */

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, tab = "mapSettings", title="components.map.width.short")
    private Integer widthPercent = 100;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, tab = "mapSettings", title="components.map.height.short")
    private Integer heightPercent = 100;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, tab = "mapSettings", title="components.map.own.size")
    private Boolean sizeInPercent;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, tab = "mapSettings", title="components.map.width")
    private Integer widthPx = 400;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, tab = "mapSettings", title="components.map.height")
    private Integer heightPx = 400;

    @Min(0)
    @Max(21)
    @DataTableColumn(inputType = DataTableColumnType.NUMBER, tab = "mapSettings", title="components.map.zoom")
    private Integer zoom = 13;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, tab = "mapSettings", title="components.map.enable_scrollwheel")
    private Boolean scrollwheel;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, tab = "mapSettings", title="components.map.enable_controls")
    private Boolean showControls = true;

    /* TAB PIN SETTINGS */

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, tab = "pinSettings", title="components.map.show_address")
    private Boolean labelAddress; // aka showAddress

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, tab = "pinSettings", title="components.map.add.own_text")
    private Boolean labelComment;

    @DataTableColumn(inputType = DataTableColumnType.TEXTAREA, tab = "pinSettings", title="components.map.label")
    private String label;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, tab = "pinSettings", title="components.map.offsetY.short")
    private Integer offsetY = 0;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, tab = "pinSettings", title="components.map.offsetX.short")
    private Integer offsetX = 0;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, tab = "pinSettings", title="components.map.close_label")
    private Boolean closeLabel;

    //
    private String mapProvider = Constants.getString("mapProvider", "");
    private String mapKey = Constants.getString("googleMapsApiKey", "");
}
