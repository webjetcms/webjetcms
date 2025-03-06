package sk.iway.iwcm.components.menu;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@WebjetComponent("sk.iway.iwcm.components.menu.MenuApp")
@WebjetAppStore(
    nameKey = "components.menu.title",
    descKey = "components.menu.desc",
    itemKey = "cmp_menu",
    imagePath = "/components/menu/editoricon.png",
    galleryImages = "/components/menu/",
    componentPath = "/components/menu/menu_ul_li.jsp"
)
@Getter
@Setter
public class MenuApp extends WebjetComponentAbstract {

    @DataTableColumn(
        inputType = DataTableColumnType.STATIC_TEXT,
        tab = "basic",
        title="components.menu.menu_help_menu"
    )
    private String explain;

    @DataTableColumn(
        inputType = DataTableColumnType.JSON,
        title="stat_settings.group_id",
        tab = "basic",
        sortAfter = "externalLink",
        className = "dt-tree-group"
    )
    private GroupDetails rootGroupId;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        tab = "basic",
        title="components.menu.start_offset")
    private int startOffset = 0;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        tab = "basic",
        title="components.menu.max_level")
    private int maxLevel = -1;

    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        tab = "advanced",
        title="components.menu.class_type",
        editor = {
        @DataTableColumnEditor(
            options = {
                @DataTableColumnEditorAttr(key = "components.menu.class_type.none", value = "none"),
                @DataTableColumnEditorAttr(key = "components.menu.class_type.basic", value = "basic"),
                @DataTableColumnEditorAttr(key = "components.menu.class_type.full", value = "full"),
                @DataTableColumnEditorAttr(key = "components.menu.class_type.bootstrap", value = "bootstrap")
            }
        )
    })
    private String classes = "basic";

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN_TEXT,
        title = "components.menu.generate_empty_span",
        tab = "advanced"
    )
    private Boolean generateEmptySpan;

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN_TEXT,
        title = "components.menu.open_all_items",
        tab = "advanced"
    )
    private Boolean openAllItems;

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN_TEXT,
        title = "components.menu.only_set_variables",
        tab = "advanced"
    )
    private Boolean onlySetVariables;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        tab = "advanced",
        title="components.menu.root_ul_id"
    )
    private String rootUlId = "menu";

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        tab = "advanced",
        title="components.menu.menu_info_dir_name"
    )
    private String menuInfoDirName;

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN_TEXT,
        title = "components.menu.menu_include_perex",
        tab = "advanced"
    )
    private Boolean menuIncludePerex;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        tab = "advanced",
        title="components.menu.menu_include_perex_level"
    )
    private Integer menuIncludePerexLevel = 1;
}
