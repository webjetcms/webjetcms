package sk.iway.iwcm.components.forum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
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

@WebjetComponent("sk.iway.iwcm.components.forum.ForumApp")
@WebjetAppStore(nameKey = "components.forum.title", descKey = "components.forum.desc", itemKey = "cmp_forum", imagePath = "/components/forum/editoricon.png", galleryImages = "/components/forum/", componentPath = "/components/forum/forum.jsp,/components/forum/forum_mb.jsp", customHtml = "/apps/forum/admin/editor-component.html")
@DataTableTabs(tabs = {
        @DataTableTab(id = "basic", title = "components.universalComponentDialog.title", selected = true),
        @DataTableTab(id = "componentIframeWindowTabList", title = "components.forum.zoznam_diskusii")
})
@Getter
@Setter
public class ForumApp extends WebjetComponentAbstract {
    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.forum.select_component", tab = "basic", editor = {
            @DataTableColumnEditor(options = {
                    @DataTableColumnEditorAttr(key = "components.forum.admin.forumType.simple", value = "forum"),
                    @DataTableColumnEditorAttr(key = "components.forum.admin.forumType.mb", value = "forum_mb")
            })
    })
    private String forumType;

    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.forum.sort_by_question_date", tab = "basic", editor = {
            @DataTableColumnEditor(options = {
                    @DataTableColumnEditorAttr(key = "components.forum.sort_by_question_date.asc", value = "true"),
                    @DataTableColumnEditorAttr(key = "components.forum.sort_by_question_date.desc", value = "false")
            })
    })
    private Boolean sortAscending;

    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.forum.sort_topics_by", tab = "basic", editor = {
            @DataTableColumnEditor(options = {
                    @DataTableColumnEditorAttr(key = "components.forum.sortBy.LastPost", value = "LastPost"),
                    @DataTableColumnEditorAttr(key = "components.forum.sortBy.QuestionDate", value = "QuestionDate")
            })
    })
    private Boolean sortTopicsBy;

    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.forum.type", tab = "basic", editor = {
            @DataTableColumnEditor(options = {
                    @DataTableColumnEditorAttr(key = "components.forum.type_iframe", value = "iframe"),
                    @DataTableColumnEditorAttr(key = "components.forum.type_perex", value = "perex"),
                    @DataTableColumnEditorAttr(key = "components.forum.type_none", value = "none"),
                    @DataTableColumnEditorAttr(key = "components.forum.type_normal", value = "normal"),
                    @DataTableColumnEditorAttr(key = "components.forum.start_open", value = "open")
            })
    })
    private String type;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "components.forum.paging", tab = "basic")
    private Boolean noPaging;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.forum.page_size", tab = "basic")
    private Integer pageSize = 10;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.forum.page_links_num", tab = "basic")
    private Integer pageLinksNum = 10;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.forum.page_size", tab = "basic")
    private Integer pageSizeForum = 25;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "components.forum.use_del_time_limit", tab = "basic")
    private Boolean useDelTimeLimit = true;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.forum.del_minutes", tab = "basic")
    private Integer delMinutes = 30;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "components.forum.notify_page_author", tab = "basic")
    private Boolean notifyPageAuthor = false;

    @DataTableColumn(inputType = DataTableColumnType.MULTISELECT, title="components.forum_editor.writeGroups", tab = "basic", filter = true)
    private String[] structure;

    @DataTableColumn(inputType = DataTableColumnType.HIDDEN, tab = "basic")
    Boolean rootGroup=true;

    @DataTableColumn(inputType = DataTableColumnType.IFRAME, tab = "componentIframeWindowTabList")
    private String iframe  = "/apps/forum/admin/";

    @Override
    public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
        Map<String, List<OptionDto>> options = new HashMap<>();
        List<OptionDto> sampleOptions = new ArrayList<>();
        sampleOptions.add(new OptionDto("Skupina1", "Skupina1", null));
        sampleOptions.add(new OptionDto("↳ podskupina1", "↳ podskupina1", null));
        sampleOptions.add(new OptionDto("↳ podskupina2", "↳ podskupina2", null));
        sampleOptions.add(new OptionDto("Skupina2", "Skupina2", null));
        sampleOptions.add(new OptionDto("↳ podskupina1", "↳ podskupina1", null));
        sampleOptions.add(new OptionDto("↳ podskupina2", "↳ podskupina2", null));
        sampleOptions.add(new OptionDto("↳ podskupina3", "↳ podskupina3", null));

        GroupsDB groupsDB = GroupsDB.getInstance();
        DocDB docDB = DocDB.getInstance();
        List<GroupDetails> groups = groupsDB.getGroups(Tools.getIntValue(request.getSession().getAttribute("iwcm_group_id").toString(), -1));

        if (!groups.isEmpty()) sampleOptions.clear();
        for (GroupDetails gd : groups) {
            sampleOptions.add(new OptionDto(gd.getGroupName(), gd.getGroupName(), null));

            List<DocDetails> docs = docDB.getDocByGroup(gd.getGroupId());
            for (DocDetails dd : docs) {
                sampleOptions.add(new OptionDto("↳ " + dd.getTitle(), "↳ " + dd.getTitle(), null));
            }
        }

        options.put("structure", sampleOptions);

        return options;
    }


}