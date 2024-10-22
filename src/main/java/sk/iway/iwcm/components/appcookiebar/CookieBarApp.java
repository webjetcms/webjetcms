package sk.iway.iwcm.components.appcookiebar;

import javax.validation.constraints.Min;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@WebjetComponent("sk.iway.iwcm.components.appcookiebar.CookieBarApp")
@WebjetAppStore(
    nameKey = "components.app-cookiebar.title",
    descKey = "components.app-cookiebar.desc",
    itemKey= "cmp_app-cookiebar",
    imagePath = "/components/app-cookiebar/editoricon.png",
    galleryImages = "/components/app-cookiebar/",
    componentPath = "/components/app-cookiebar/cookiebar.jsp"
)
@Getter
@Setter
public class CookieBarApp extends WebjetComponentAbstract {

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        tab = "basic",
        title="components.app-cookiebar.cookiebar_title")
    private boolean checkbox_title = true;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        tab = "basic",
        title="components.app-cookiebar.titleText",
        editor = {
        @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(
                    key = "placeholder",
                    value = "components.app-cookiebar.cookie_title")
            }
        )
    })
    private String cookie_title;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        tab = "basic",
        title="components.app-cookiebar.text",
        className = "wrap",
        editor = {
        @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(
                    key = "placeholder",
                    value = "components.app-cookiebar.cookie_text")
            }
        )
    })
    private String cookie_text;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        tab = "basic",
        title="components.app-cookiebar.buttonTextAccept",
        editor = {
        @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(
                    key = "placeholder",
                    value = "components.app-cookiebar.cookie_ButtonText")
            }
        )
    })
    private String cookie_ButtonText;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        tab = "basic",
        title="components.app-cookiebar.buttonTextDecline",
        editor = {
        @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(
                    key = "placeholder",
                    value = "components.app-cookiebar.cookie_ButtonTextDecline")
            }
        )
    })
    private String cookie_ButtonTextDecline;

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        tab = "basic",
        title="components.app-cookiebar.showLink")
    private boolean showLink;

    @DataTableColumn(
        inputType = DataTableColumnType.COLOR,
        tab = "style",
        title="components.app-cookiebar.background")
    private String color_background;

    @DataTableColumn(
        inputType = DataTableColumnType.COLOR,
        tab = "style",
        title="components.app-cookiebar.titleColor")
    private String color_title;

    @DataTableColumn(
        inputType = DataTableColumnType.COLOR,
        tab = "style",
        title="components.app-cookiebar.textColor")
    private String color_text;

    @DataTableColumn(
        inputType = DataTableColumnType.COLOR,
        tab = "style",
        title="components.app-cookiebar.buttonColor")
    private String color_button;

    @DataTableColumn(
        inputType = DataTableColumnType.COLOR,
        tab = "style",
        title="components.app-cookiebar.buttonTextColor")
    private String color_buttonText;

    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        tab = "style",
        title="components.app-cookiebar.position",
        editor = {
        @DataTableColumnEditor(
            options = {
                @DataTableColumnEditorAttr(
                    key = "components.app-cookiebar.position.top",
                    value = "top"),
                @DataTableColumnEditorAttr(
                    key = "components.app-cookiebar.position.bottom",
                    value = "bottom")
            }
        )
    })
    private String position;

    @Min(0)
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        tab = "style",
        title="components.app-cookiebar.padding-top",
        editor = {
        @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(
                    key = "placeholder",
                    value = "components.app-cookiebar.cookie_title")
            }
        )
    })
    private int padding_top = 25;

    @Min(0)
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        tab = "style",
        title="components.app-cookiebar.padding-bottom")
    private int padding_bottom = 25;

}
