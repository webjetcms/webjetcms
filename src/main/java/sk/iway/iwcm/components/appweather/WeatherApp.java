package sk.iway.iwcm.components.appweather;

import jakarta.validation.constraints.Min;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@WebjetComponent("sk.iway.iwcm.components.appweather.WeatherApp")
@WebjetAppStore(
    nameKey = "components.app-weather.title",
    descKey = "components.app-weather.desc",
    itemKey = "cmp_pocasie", 
    imagePath = "/components/app-weather/editoricon.png",
    galleryImages = "/components/app-weather/",
    componentPath = "/components/app-weather/xml-pocasie.jsp")
@Getter
@Setter
public class WeatherApp extends WebjetComponentAbstract {

    @Min(1)
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        tab = "basic",
        title = "components.pocasie.pocetDni")
    private Integer pocetDni = 8;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        tab = "basic",
        title = "components.pocasie.place")
    private String place = "Bratislava";

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        tab = "basic",
        renderFormat = "dt-format-number--decimal",
        title = "components.map.latitude")
    private Float lat = 48.15f;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        tab = "basic",
        renderFormat = "dt-format-number--decimal",
        title = "components.map.longitude")
    private Float lon = 17.1167f;
}