package sk.iway.iwcm.components.news;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.components.news.NewsActionBean.PublishType;
import sk.iway.iwcm.components.news.NewsQuery.OrderEnum;
import sk.iway.iwcm.components.news.templates.jpa.NewsTemplatesEntity;
import sk.iway.iwcm.components.news.templates.jpa.NewsTemplatesRepository;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.GroupDetails;
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

@WebjetComponent("sk.iway.iwcm.components.news.NewsApp")

@WebjetAppStore(
    nameKey = "components.news.title",
    descKey = "components.news.desc",
    itemKey = "cmp_news",
    imagePath = "/components/news/editoricon.png",
    galleryImages = "/components/news/screenshot-1.jpg,/components/news/screenshot-2.jpg,/components/news/screenshot-3.jpg,/components/news/screenshot-4.jpg",
    componentPath = "/components/news/news-velocity.jsp",
    customHtml = "/apps/news/admin/editor-component.html"
)

@DataTableTabs(tabs = {
    @DataTableTab(id = "basic", title = "user.admin.settings", selected = true),
    @DataTableTab(id = "templates", title = "components.news.template"),
    @DataTableTab(id = "perex", title = "components.news.perexGroup"),
    @DataTableTab(id = "filter", title = "editor.tab.filter", content = "<div id='filtersDiv'></div>"),
    @DataTableTab(id = "news", title = "components.news.title", content = "<div class='statContainer'><iframe id='newsListIframe' src='about:blank' width='100%' height='519'></iframe><div>"),
})

@Getter
@Setter
@NoArgsConstructor
public class NewsApp extends WebjetComponentAbstract  {

    @JsonIgnore
    private NewsTemplatesRepository newsTemplatesRepository;

    @Autowired
    public NewsApp(NewsTemplatesRepository newsTemplatesRepository) {
        this.newsTemplatesRepository = newsTemplatesRepository;
    }

    @DataTableColumn(inputType = DataTableColumnType.JSON, title="components.news.groupids", tab = "basic", visible=false, filter=false, orderable=false, className = "dt-tree-group-array", editor = {
        @DataTableColumnEditor(attr = {@DataTableColumnEditorAttr(key = "data-dt-json-addbutton", value = "editor.json.addGroup") })
    })
    protected List<GroupDetails> groupIds;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, tab = "basic", title = "components.export.expandGroupIds")
    protected boolean alsoSubGroups = false;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, tab = "basic", title = "components.news.subGroupsDepth")
    protected int subGroupsDepth = -1;

    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "basic", title = "components.news.publishtype",
        editor = @DataTableColumnEditor(
            options = {
                @DataTableColumnEditorAttr(key = "components.news.PUBLISH_NEW", value = "new"),
                @DataTableColumnEditorAttr(key = "components.news.PUBLISH_OLD", value = "old"),
                @DataTableColumnEditorAttr(key = "components.news.PUBLISH_ALL", value = "all"),
                @DataTableColumnEditorAttr(key = "components.news.PUBLISH_NEXT", value = "next"),
                @DataTableColumnEditorAttr(key = "components.news.PUBLISH_VALID", value = "valid")
            },
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before")
            }
        )
    )
    protected String publishType = PublishType.NEW.name();

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, tab = "basic", title = "components.news.noPerexCheck")
    protected boolean perexNotRequired = false;

    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "basic", title = "components.news.doc_mode.title",
        editor = @DataTableColumnEditor(
            options = {
                @DataTableColumnEditorAttr(key = "components.news.doc_mode.all", value = "0"),
                @DataTableColumnEditorAttr(key = "components.news.doc_mode.only", value = "1"),
                @DataTableColumnEditorAttr(key = "components.news.doc_mode.exclude", value = "2")
            }
        )
    )
    protected int docMode = 0;

    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "basic", title = "components.news.ordertype",
        editor = @DataTableColumnEditor(
            options = {
                @DataTableColumnEditorAttr(key = "components.news.ORDER_PRIORITY", value = "priority"),
                @DataTableColumnEditorAttr(key = "components.news.ORDER_DATE", value = "date"),
                @DataTableColumnEditorAttr(key = "components.news.ORDER_EVENT_DATE", value = "event_date"),
                @DataTableColumnEditorAttr(key = "components.news.ORDER_SAVE_DATE", value = "save_date"),
                @DataTableColumnEditorAttr(key = "components.news.ORDER_TITLE", value = "title"),
                @DataTableColumnEditorAttr(key = "components.news.ORDER_PLACE", value = "place"),
                @DataTableColumnEditorAttr(key = "components.news.ORDER_ID", value = "id"),
                @DataTableColumnEditorAttr(key = "components.news.RATING", value = "rating")
            }
        )
    )
    protected String order = OrderEnum.DATE.name();

    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "basic", title = "&nbsp;",
        editor = @DataTableColumnEditor(
            options = {
                @DataTableColumnEditorAttr(key = "components.gallery.sort.asc", value = "true"),
                @DataTableColumnEditorAttr(key = "components.gallery.sort.desc", value = "false")
            },
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after"),
            }
        )
    )
    protected boolean ascending = true;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, tab = "basic", title = "components.news.paging")
    protected boolean paging = false;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, tab = "basic", title = "components.news.pageSize")
    protected int pageSize = 10;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, tab = "basic", title = "components.news.offset")
    protected int offset = 0;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, tab = "basic", title = "components.news.no_data",
        editor = @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before"),
            }
    ))
    protected boolean loadData = false;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, tab = "basic", title = "components.news.check_duplicty")
    protected boolean checkDuplicity;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "basic", title="components.news.contextClasses", className="ai-off")
	protected String contextClasses;

    @DataTableColumn(
        inputType = DataTableColumnType.IMAGE_RADIO,
        title = "&nbsp;",
        tab = "templates",
        className = "image-radio-horizontal image-radio-fullwidth"
    )
    protected String template;

    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, tab = "perex", title="components.news.perexGroup", editor = {
        @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "unselectedValue", value = "")
            }
        )
    })
	protected int[] perexGroup;

    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, tab = "perex", title="components.news.perexGroupNot", editor = {
        @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "unselectedValue", value = "")
            }
        )
    })
	protected int[] perexGroupNot;

    @Override
    public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
        Map<String, List<OptionDto>> options = new HashMap<>();
        List<OptionDto> perexGroupOptions = DocDB.getInstance().getPerexGroupOptions();
        options.put("perexGroup", perexGroupOptions);
        options.put("perexGroupNot", perexGroupOptions);

        List<OptionDto> docFields = new ArrayList<>();
        for(FieldEnum field : FieldEnum.values()) {
            docFields.add(new OptionDto(field.name().replace("_", ""), field.getFieldTypeString(), field.name()));
        }
        options.put("docFields", docFields);

        List<OptionDto> newsTemplates = new ArrayList<>();
        for(NewsTemplatesEntity newsTemplate : newsTemplatesRepository.findAllByDomainId(CloudToolsForCore.getDomainId())) {
            newsTemplates.add( new OptionDto(newsTemplate.getName(), newsTemplate.getName(), newsTemplate.getImagePath()) );
        }
        options.put("template", newsTemplates);

        return options;
    }

    public String[] getContextClassesArr()
	{
		return contextClasses == null ? new String[0] : Tools.getTokens(contextClasses, ",;+|");
	}
}