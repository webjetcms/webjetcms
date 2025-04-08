package sk.iway.iwcm.components.sitemap;

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

@WebjetComponent("sk.iway.iwcm.components.sitemap.SiteMapApp")
@WebjetAppStore(nameKey = "components.sitemap.title", descKey = "components.sitemap.desc", itemKey = "cmp_sitemap", imagePath = "/components/sitemap/editoricon.gif", galleryImages = "/components/sitemap/", componentPath = "/components/sitemap/ul_li.jsp,/components/sitemap/sitemap.jsp,/components/sitemap/treelist.jsp,/components/sitemap/ul_li_jquery.jsp", customHtml = "/apps/sitemap/admin/editor-component.html")
@Getter
@Setter
public class SiteMapApp extends WebjetComponentAbstract {
    @DataTableColumn(inputType = DataTableColumnType.JSON, title = "components.sitemap.root_group", tab = "basic", className = "dt-tree-group")
    private GroupDetails groupId = new GroupDetails("1");

    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "basic", title = "components.sitemap.map_type", editor = {
            @DataTableColumnEditor(options = {
                    @DataTableColumnEditorAttr(key = "components.sitemap.ul_li", value = "ul_li"),
                    @DataTableColumnEditorAttr(key = "components.sitemap.sitemap", value = "sitemap"),
                    @DataTableColumnEditorAttr(key = "components.sitemap.treelist", value = "treelist"),
                    @DataTableColumnEditorAttr(key = "components.sitemap.ul_li_jquery", value = "ul_li_jquery")
            })
    })
    private String mapType;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.sitemap.max_depth", tab = "basic")
    private Integer maxDepth = 5;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.sitemap.cols_num", tab = "basic")
    private Integer colsNum = 1;

}