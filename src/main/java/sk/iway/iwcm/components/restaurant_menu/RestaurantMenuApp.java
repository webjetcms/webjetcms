package sk.iway.iwcm.components.restaurant_menu;

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

@WebjetComponent("sk.iway.iwcm.components.restaurant_menu.RestaurantMenuApp")
@WebjetAppStore(nameKey = "components.restaurant_menu.title", descKey = "components.restaurant_menu.desc", itemKey = "cmp_restaurant_menu", imagePath = "/components/restaurant_menu/editoricon.png", galleryImages = "/components/restaurant_menu/", componentPath = "/components/restaurant_menu/related_pages.jsp") //customHtml = "/apps/restaurant_menu/admin/editor-component.html"
@Getter
@Setter
public class RestaurantMenuApp extends WebjetComponentAbstract {
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.restaurant_menu.mena", tab = "basic")
    private String mena = "€";

    @DataTableColumn(inputType = DataTableColumnType.IMAGE_RADIO, title = "components.restaurant_menu.visualSettings", tab = "basic", className = "image-radio-horizontal image-radio-fullwidth")
    private String style = "01";

    @Override
    public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
        Map<String, List<OptionDto>> options = new HashMap<>();

        options.put("style", DatatableTools.getImageRadioOptions("/components/restaurant_menu/menu-styles/"));
        return options;
    }
}
