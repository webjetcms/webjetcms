package sk.iway.iwcm.components.forum;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableTab;
import sk.iway.iwcm.system.datatable.annotations.DataTableTabs;

@WebjetComponent("sk.iway.iwcm.components.forum.ForumApp")
@WebjetAppStore(nameKey = "components.forum.title", descKey = "components.forum.desc", itemKey = "cmp_forum", imagePath = "/components/forum/editoricon.png", galleryImages = "/components/forum/", componentPath = "/components/forum/forum.jsp", customHtml = "/apps/forum/admin/editor-component.html")
@DataTableTabs(tabs = {
                @DataTableTab(id = "basic", title = "components.universalComponentDialog.title", selected = true),
                @DataTableTab(id = "componentIframe", title = "components.gallery.images", content = "")
})
@Getter
@Setter
public class ForumApp extends WebjetComponentAbstract {
        @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.forum.select_component", tab = "forumSettings", editor = {
                        @DataTableColumnEditor(options = {
                                        @DataTableColumnEditorAttr(key = "components.forum.admin.forumType.simple", value = "forum"),
                                        @DataTableColumnEditorAttr(key = "components.forum.admin.forumType.mb", value = "forum_mb")
                        })
        })
        private String forumType;

        @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.forum.sort_by_question_date", tab = "forumSettings", editor = {
                        @DataTableColumnEditor(options = {
                                        @DataTableColumnEditorAttr(key = "components.forum.sort_by_question_date.asc", value = "true"),
                                        @DataTableColumnEditorAttr(key = "components.forum.sort_by_question_date.desc", value = "false")
                        })
        })
        private Boolean sortAscending;

        @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.forum.sort_topics_by", tab = "forumSettings", editor = {
                        @DataTableColumnEditor(options = {
                                        @DataTableColumnEditorAttr(key = "components.forum.sortBy.LastPost", value = "LastPost"),
                                        @DataTableColumnEditorAttr(key = "components.forum.sortBy.QuestionDate", value = "QuestionDate")
                        })
        })
        private Boolean sortTopicsBy;

        @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.forum.type", tab = "forumSettings", editor = {
                        @DataTableColumnEditor(options = {
                                        @DataTableColumnEditorAttr(key = "components.forum.type_iframe", value = "iframe"),
                                        @DataTableColumnEditorAttr(key = "components.forum.type_perex", value = "perex"),
                                        @DataTableColumnEditorAttr(key = "components.forum.type_none", value = "none"),
                                        @DataTableColumnEditorAttr(key = "components.forum.type_normal", value = "normal"),
                                        @DataTableColumnEditorAttr(key = "components.forum.start_open", value = "open")
                        })
        })
        private String type;

        @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "components.forum.paging", tab = "forumSettings")
        private Boolean usePaging;

        @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.forum.page_size", tab = "forumSettings")
        private Integer pageSize = 10;

        @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.forum.page_links_num", tab = "forumSettings")
        private Integer pageLinksNum = 10;

        @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.forum.page_size", tab = "forumSettings")
        private Integer pageSizeForum = 25;

        @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "components.forum.use_del_time_limit", tab = "forumSettings")
        private Boolean useDelTimeLimit = true;

        @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.forum.del_minutes", tab = "forumSettings")
        private Integer delMinutes = 30;

        @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "components.forum.notify_page_author", tab = "forumSettings")
        private Boolean notifyPageAuthor = false;

}