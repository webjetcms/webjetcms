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
import sk.iway.iwcm.system.datatable.annotations.DataTableTab;
import sk.iway.iwcm.system.datatable.annotations.DataTableTabs;

@WebjetComponent("sk.iway.iwcm.components.restaurant_menu.RestaurantMenuApp")
@WebjetAppStore(
    nameKey = "components.restaurant_menu.title",
    descKey = "components.restaurant_menu.desc",
    itemKey = "cmp_restaurant_menu",
    imagePath = "/components/restaurant_menu/editoricon.png",
    galleryImages = "/components/restaurant_menu/",
    componentPath = "/components/restaurant_menu/menu.jsp")
@Getter
@Setter
@DataTableTabs(tabs = {
    @DataTableTab(id = "basic", title = "components.universalComponentDialog.title", selected = true),
    @DataTableTab(id = "listMealsIframeWindowTab", title = "components.restaurant_menu.mealsList", content = ""),
    @DataTableTab(id = "newMenuIframeWindowTab", title = "components.restaurant_menu.newMenu", content = "")
})
public class RestaurantMenuApp extends WebjetComponentAbstract {
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.restaurant_menu.mena", tab = "basic", className = "image-radio-horizontal image-radio-fullwidth" )
    private String mena = "€";

    @DataTableColumn(inputType = DataTableColumnType.IMAGE_RADIO, title = "components.restaurant_menu.visualSettings", tab = "basic", className = "image-radio-horizontal image-radio-fullwidth")
    private String style = "01";

    @Override
    public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
        Map<String, List<OptionDto>> options = new HashMap<>();

        options.put("style", DatatableTools.getImageRadioOptions("/components/restaurant_menu/menu-styles/"));
        return options;
    }

    @DataTableColumn(inputType = DataTableColumnType.IFRAME, tab = "listMealsIframeWindowTab")
    private String iframe  = "/components/restaurant_menu/admin_list_meals.jsp";

    @DataTableColumn(inputType = DataTableColumnType.IFRAME, tab = "newMenuIframeWindowTab")
    private String iframe2  = "/components/restaurant_menu/admin_new_menu.jsp";
}
