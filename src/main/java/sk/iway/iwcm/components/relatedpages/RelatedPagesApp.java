package sk.iway.iwcm.components.relatedpages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.PerexGroupBean;
import sk.iway.iwcm.editor.rest.ComponentRequest;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.OptionDto;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@WebjetComponent("sk.iway.iwcm.components.relatedpages.RelatedPagesApp")
@WebjetAppStore(nameKey = "components.related-pages.title", 
                descKey = "components.related-pages.desc", 
                itemKey = "cmp_related-pages", 
                imagePath = "/components/related-pages/editoricon.png", 
                galleryImages = "/components/related-pages/", 
                componentPath = "/components/related-pages/related_pages.jsp",
                customHtml = "/apps/related-pages/admin/editor-component.html")
@Getter
@Setter
public class RelatedPagesApp extends WebjetComponentAbstract {

    @DataTableColumn(inputType = DataTableColumnType.JSON, title = "components.news.groupids", tab = "basic", className = "dt-tree-group-array")
    private List<GroupDetails> rootGroups;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, tab = "basic", title = "components.user.root_group_recursive")
    private Boolean rGroupsRecursive = false;

    @DataTableColumn(inputType = DataTableColumnType.RADIO, title = "components.user.title_name", tab = "basic", editor = {
            @DataTableColumnEditor(options = {
                    @DataTableColumnEditorAttr(key = "components.user.root_group", value = "groupName"),
                    @DataTableColumnEditorAttr(key = "components.user.root_group_parent", value = "rootGroupName"),
                    @DataTableColumnEditorAttr(key = "components.user.custom_title", value = "customName")
            })
    })
    private String titleName = "groupName";

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "&nbsp;", tab = "basic")
    private String customName;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.related-pages.results_per_group", tab = "basic")
    private Integer pagesInGroup = 10;

    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, tab = "basic", title = "components.news.perexGroup", editor = {
        @DataTableColumnEditor(attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "editor.tab.filter"),
                @DataTableColumnEditorAttr(key = "unselectedValue", value = "")
        })
    })
    private Integer[] groups;

    @Override
    public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
        Map<String, List<OptionDto>> options = new HashMap<>();
        List<PerexGroupBean> perexGroups = DocDB.getInstance().getPerexGroups(componentRequest.getGroupId());
        List<OptionDto> perexGroupOptions = new ArrayList<>();
        for (PerexGroupBean pg : perexGroups) {
            perexGroupOptions.add(new OptionDto(pg.getPerexGroupName(), "" + pg.getPerexGroupId(), null));
        }
        options.put("groups", perexGroupOptions);

        return options;
    }

}
