package sk.iway.iwcm.components.calendar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.PerexGroupBean;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.editor.rest.ComponentRequest;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.OptionDto;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableTab;
import sk.iway.iwcm.system.datatable.annotations.DataTableTabs;

@WebjetComponent("sk.iway.iwcm.components.calendar.NewsCalendarApp")
@WebjetAppStore(
    nameKey = "components.calendarnews.title",
    descKey = "components.calendarnews.desc",
    itemKey = "cmp_calendar",
    variant = "news",
    imagePath = "/components/news-calendar/editoricon.png",
    galleryImages = "/components/news-calendar/",
    componentPath = "/components/news-calendar/news_calendar.jsp"
)
@DataTableTabs(tabs = {
    @DataTableTab(id = "basic", title = "components.universalComponentDialog.title", selected = true)
})
@Getter
@Setter
public class NewsCalendarApp extends WebjetComponentAbstract {
    @DataTableColumn(
        inputType = DataTableColumnType.JSON,
        title="components.news.groupids",
        tab = "basic",
        sortAfter = "editorFields.groupDetails",
        className = "dt-tree-group-array"
    )
    private List<GroupDetails> groupIds;

    @DataTableColumn(
        inputType = DataTableColumnType.CHECKBOX,
        title = "components.calendar_news.zahrnut_podadresare",
        tab = "basic"
    )
    private boolean expandGroupIds = true;

    @DataTableColumn(
        inputType = DataTableColumnType.CHECKBOX,
        tab = "basic",
        title="components.news.perexGroup",
        editor = {
        @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "editor.tab.filter"),
                @DataTableColumnEditorAttr(key = "unselectedValue", value = "")
            }
        )
    })
	private Integer[] perexGroup;

    @Override
    public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
        Map<String, List<OptionDto>> options = new HashMap<>();
        List<PerexGroupBean> perexGroups = DocDB.getInstance().getPerexGroups(componentRequest.getGroupId());
        List<OptionDto> perexGroupOptions = new ArrayList<>();
        for (PerexGroupBean pg : perexGroups) {
            perexGroupOptions.add(new OptionDto(pg.getPerexGroupName(), ""+pg.getPerexGroupId(), null));
        }
        options.put("perexGroup", perexGroupOptions);
        return options;
    }
}
