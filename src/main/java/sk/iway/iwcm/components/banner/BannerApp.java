package sk.iway.iwcm.components.banner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.editor.rest.ComponentRequest;
import sk.iway.iwcm.system.datatable.OptionDto;

import jakarta.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.components.banner.model.BannerGroupBean;
import sk.iway.iwcm.system.datatable.annotations.DataTableTab;
import sk.iway.iwcm.system.datatable.annotations.DataTableTabs;

@WebjetComponent("sk.iway.iwcm.components.banner.BannerApp")
@WebjetAppStore(
    nameKey = "components.banner.title",
    descKey = "components.banner.desc",
    itemKey= "menuBanner",
    imagePath = "/components/banner/editoricon.png",
    galleryImages = "/components/banner/",
    componentPath = "/components/banner/banner.jsp",
    customHtml = "/apps/banner/admin/editor-component.html"
)
@DataTableTabs(tabs = {
    @DataTableTab(id = "basic", title = "components.universalComponentDialog.title", selected = true),
    @DataTableTab(id = "advanced", title = "editor.tab.advanced", content = ""),
    @DataTableTab(id = "componentIframeWindowTabList", title = "components.banner.list_of_banners", content = ""),
    @DataTableTab(id = "componentIframeWindowTabStats", title = "components.banner.banners_stat", content = "")
})
@Getter
@Setter
public class BannerApp extends WebjetComponentAbstract {
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        tab = "basic",
        title="components.banner.select_group"
    )
    private String group;

    @DataTableColumn(
        inputType = DataTableColumnType.CHECKBOX,
        title = "components.banner.active",
        tab = "basic",
        editor = {
        @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "unselectedValue", value = "")
            },
            options = {
                @DataTableColumnEditorAttr(key = "", value = "enabled"),
            })
        }
    )
    private String status = "enabled";

    @DataTableColumn(
        inputType = DataTableColumnType.RADIO,
        title = "components.banner.display_mode",
        tab = "basic",
        editor = {
            @DataTableColumnEditor(
                options = {
                    @DataTableColumnEditorAttr(key = "components.banner.display_mode_1", value = "1"),
                    @DataTableColumnEditorAttr(key = "components.banner.display_mode_2", value = "2"),
                    @DataTableColumnEditorAttr(key = "components.banner.display_mode_3", value = "3"),
                    @DataTableColumnEditorAttr(key = "components.banner.display_mode_4", value = "4"),
                    @DataTableColumnEditorAttr(key = "components.banner.display_mode_5", value = "5")
                }
            )
        }
    )
    private String displayMode = "1";

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        tab = "basic",
        title="components.banner.banner_index")
    private Integer bannerIndex;

    @DataTableColumn(
        inputType = DataTableColumnType.STATIC_TEXT,
        tab = "basic",
        title="components.banner.jedinecny_index"
    )
    private String explain;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        tab = "advanced",
        title="components.banner.videoWrapperClass",
        editor = {
            @DataTableColumnEditor(attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "components.video.title") })
        }
    )
    private String videoWrapperClass;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        tab = "advanced",
        title="components.banner.jumbotronVideoClass"
    )
    private String jumbotronVideoClass;

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN_TEXT,
        title = "components.banner.show_in_iframe",
        tab = "advanced",
        editor = {
            @DataTableColumnEditor(attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "components.forum.type_iframe") })
        }
    )
    private Boolean showInIframe = false;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        tab = "advanced",
        title="components.banner.refresh_rate"
    )
    private Integer refreshRate = 0;

    @DataTableColumn(
        inputType = DataTableColumnType.STATIC_TEXT,
        tab = "advanced",
        title="components.banner.refresh_rate_desc"
    )
    private String explain2;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        tab = "advanced",
        title="components.banner.iframe_width")
    private Integer iframeWidth;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        tab = "advanced",
        title="components.banner.iframe_height")
    private Integer iframeHeight;

    @DataTableColumn(inputType = DataTableColumnType.IFRAME, tab = "componentIframeWindowTabList", title="&nbsp;")
    private String iframe  = "/apps/banner/admin/";

    @DataTableColumn(inputType = DataTableColumnType.IFRAME, tab = "componentIframeWindowTabStats", title="&nbsp;")
    private String iframe2  = "/apps/banner/admin/banner-stat/";


    @Override
    public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
        Map<String, List<OptionDto>> options = new HashMap<>();

        List<OptionDto> groupOptions = new ArrayList<>();

        Identity user = (Identity) request.getSession().getAttribute(Constants.USER_KEY);
        List<BannerGroupBean> banners = (List<BannerGroupBean>) BannerDB.getBannerGroupsByUserAllowedCategories(user.getUserId());
        for (BannerGroupBean banner : banners){
            String bannerGroup = banner.getBannerGroup();
            groupOptions.add(new OptionDto(bannerGroup, bannerGroup, null));
        }

        options.put("group", groupOptions);

        return options;
    }

}