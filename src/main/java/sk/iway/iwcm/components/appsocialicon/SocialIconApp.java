package sk.iway.iwcm.components.appsocialicon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.editor.rest.ComponentRequest;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.DatatableTools;
import sk.iway.iwcm.system.datatable.OptionDto;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@WebjetComponent("sk.iway.iwcm.components.appsocialicon.SocialIconApp")
@WebjetAppStore(
    nameKey = "components.app-social_icon.title",
    descKey = "components.app-social_icon.desc",
    itemKey = "cmp_app-social_icon",
    imagePath = "/components/app-social_icon/editoricon.png",
    galleryImages = "/components/app-social_icon/",
    componentPath = "/components/app-social_icon/social_icon.jsp",
    customHtml = "/apps/app-social_icon/admin/editor-component.html")
@Getter
@Setter
@SuppressWarnings("java:S116")
public class SocialIconApp extends WebjetComponentAbstract {
    @DataTableColumn(inputType = DataTableColumnType.STATIC_TEXT, tab = "basic", title = "components.app-social_icon.editor_components.info")
    private String label;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "basic", title = "components.app-social_icon.editor_components.facebook", className = "dt-style-base64")
    private String facebook_url;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "basic", title = "components.app-social_icon.editor_components.instagram", className = "dt-style-base64")
    private String instagram_url;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "basic", title = "components.app-social_icon.editor_components.linkedin", className = "dt-style-base64")
    private String linkedin_url;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "basic", title = "components.app-social_icon.editor_components.youtube", className = "dt-style-base64")
    private String youtube_url;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "basic", title = "components.app-social_icon.editor_components.twitter", className = "dt-style-base64")
    private String twitter_url;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "basic", title = "components.app-social_icon.editor_components.mail", className = "dt-style-base64")
    private String mail_url;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "basic", title = "components.app-social_icon.editor_components.blog", className = "dt-style-base64")
    private String blog_url;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "basic", title = "components.app-social_icon.editor_components.flickr", className = "dt-style-base64")
    private String flickr_url;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "basic", title = "components.app-social_icon.editor_components.rss", className = "dt-style-base64")
    private String rss_url;

    @DataTableColumn(inputType = DataTableColumnType.IMAGE_RADIO, title = "components.news.visualStyle", tab = "style", className = "image-radio-horizontal image-radio-fullwidth")
    private String style = "01";

    @DataTableColumn(inputType = DataTableColumnType.IMAGE_RADIO, title = "components.app-social_icon.editor_components.align", tab = "style", className = "image-radio-horizontal image-radio-fullwidth")
    private String socialIconAlign = "left";

    @Override
    public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
        Map<String, List<OptionDto>> options = new HashMap<>();

        options.put("style", DatatableTools.getImageRadioOptions("/components/app-social_icon/admin-styles/"));
        options.put("socialIconAlign", DatatableTools.getAlignOptions());

        return options;
    }
}
