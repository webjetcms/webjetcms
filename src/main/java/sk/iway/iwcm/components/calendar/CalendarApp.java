package sk.iway.iwcm.components.calendar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.calendar.EventTypeDetails;
import sk.iway.iwcm.calendar.EventTypeDB;
import sk.iway.iwcm.editor.rest.ComponentRequest;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.OptionDto;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableTabs;
import sk.iway.iwcm.system.datatable.annotations.DataTableTab;


@WebjetComponent("sk.iway.iwcm.components.calendar.CalendarApp")
@WebjetAppStore(
    nameKey = "components.calendar.title",
    descKey = "components.calendar.desc",
    itemKey = "cmp_calendar",
    imagePath = "/components/calendar/editoricon.png",
    galleryImages = "/components/calendar/",
    componentPath = "/components/calendar/calendar.jsp"
)
@DataTableTabs(tabs = {
    @DataTableTab(id = "basic", title = "components.universalComponentDialog.title", selected = true),
    @DataTableTab(id = "componentIframeWindowTabList", title = "components.calendar.list_of_events", content = ""),
    @DataTableTab(id = "componentIframeWindowTabType", title = "calendar_edit.configType", content = ""),
    @DataTableTab(id = "componentIframeWindowTabRejected", title = "calendar.neschvalene_udalosti", content = ""),
    @DataTableTab(id = "componentIframeWindowTabRecommended", title = "calendar.suggest_evens", content = "")
})
@Getter
@Setter
public class CalendarApp extends WebjetComponentAbstract {
    @DataTableColumn(
        inputType = DataTableColumnType.CHECKBOX,
        tab = "basic",
        title = "calendar.udalosti",
        editor = {
        @DataTableColumnEditor(
            message = "components.calendar.zvolte_typy_udalosti",
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "editor.tab.filter"),
                @DataTableColumnEditorAttr(key = "unselectedValue", value = "")
            }
        )
    })
	private String[] typyNazvy;

    @DataTableColumn(inputType = DataTableColumnType.IFRAME, tab = "componentIframeWindowTabList", title="&nbsp;")
    private String iframeList = "/admin/listevents.do";

    @DataTableColumn(inputType = DataTableColumnType.IFRAME, tab = "componentIframeWindowTabType", title="&nbsp;")
    private String iframeType = "/components/calendar/admin_edit_type.jsp";

    @DataTableColumn(inputType = DataTableColumnType.IFRAME, tab = "componentIframeWindowTabRejected", title="&nbsp;")
    private String iframeRejected = "/components/calendar/admin_neschvalene_udalosti.jsp";

    @DataTableColumn(inputType = DataTableColumnType.IFRAME, tab = "componentIframeWindowTabRecommended", title="&nbsp;")
    private String iframeRecommended = "/components/calendar/admin_suggest_evens.jsp";

    @Override
    public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
        Map<String, List<OptionDto>> options = new HashMap<>();
        List<OptionDto> typesOptions = new ArrayList<>();

        List<EventTypeDetails> events = EventTypeDB.getTypes(request);
        for (EventTypeDetails event : events){
            String eventGroup = event.getName();
            typesOptions.add(new OptionDto(eventGroup, eventGroup, null));
        }
        options.put("typyNazvy", addCurrentValueToOptions(typesOptions, getTypyNazvy()));
        return options;
    }
}
