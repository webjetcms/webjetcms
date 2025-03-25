package sk.iway.iwcm.components.appdate;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@WebjetComponent("sk.iway.iwcm.components.appdate.DateApp")
@WebjetAppStore(
    nameKey = "components.app-date.title",
    descKey = "components.app-date.desc",
    itemKey = "cmp_app-date",
    imagePath = "/components/app-date/editoricon.png",
    galleryImages = "/components/app-date/",
    componentPath = "/components/app-date/meniny.jsp,/components/app-date/last_update.jsp,/components/date/last_update.jsp",
    customHtml = "/apps/app-date/admin/editor-component.html"
)
@Getter
@Setter
public class DateApp extends WebjetComponentAbstract {

    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        tab = "basic",
        title = "components.cloud.apps.insertToYourSite",
        editor = @DataTableColumnEditor(
            options = {
                @DataTableColumnEditorAttr(key = "components.app-date.meniny", value = "meniny"),
                @DataTableColumnEditorAttr(key = "components.date.last_update", value = "last_update"),
                @DataTableColumnEditorAttr(key = "components.date.datum", value = "!DATUM!"),
                @DataTableColumnEditorAttr(key = "components.date.date", value = "!DATE!"),
                @DataTableColumnEditorAttr(key = "components.date.den_datum", value = "!DEN_DATUM!"),
                @DataTableColumnEditorAttr(key = "components.date.den_datum_cz", value = "!DEN_DATUM_CZ!"),
                @DataTableColumnEditorAttr(key = "components.date.day_date", value = "!DAY_DATE!"),
                @DataTableColumnEditorAttr(key = "components.date.time", value = "!TIME!"),
                @DataTableColumnEditorAttr(key = "components.date.year", value = "!YEAR!")
            }
        )
    )
    private String field;

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN_TEXT,
        title = "components.date.display_updated_text",
        tab = "basic"
    )
    private Boolean aktualizovane;

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN_TEXT,
        tab = "basic",
        title = "components.date.display_date"
    )
    private Boolean datum;

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN_TEXT,
        tab = "basic",
        title = "components.date.display_time"
    )
    private Boolean cas;

    @DataTableColumn(
        inputType = DataTableColumnType.CHECKBOX,
        title = "components.date.display_date",
        tab = "basic",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "unselectedValue", value = "short")
                },
                options = {
                    @DataTableColumnEditorAttr(key = "editor.form.sl.yes", value = "long")
                }
            )
        }
    )
    private String format = "long";
}