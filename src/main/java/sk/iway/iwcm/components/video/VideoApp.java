package sk.iway.iwcm.components.video;

import java.util.Comparator;
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
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@WebjetComponent("sk.iway.iwcm.components.video.VideoApp")
@WebjetAppStore(nameKey = "components.video.title", descKey = "components.video.desc", itemKey = "cmp_video", imagePath = "/components/video/editoricon.png", galleryImages = "/components/video/", componentPath = "/components/video/video_player.jsp", customHtml = "/apps/video/admin/editor-component.html")
@Getter
@Setter
public class VideoApp extends WebjetComponentAbstract {

    @DataTableColumn(inputType = DataTableColumnType.IMAGE_RADIO, title = "&nbsp", tab = "basic", className = "image-radio-horizontal image-radio-fullwidth")
    private String field;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.video_player.file", tab = "basic")
    private String file;

    @DataTableColumn(inputType = DataTableColumnType.RADIO, title = "&nbsp", tab = "basic", editor = @DataTableColumnEditor(options = {
            @DataTableColumnEditorAttr(key = "components.video_player.fixedWidth", value = "fixed"),
            @DataTableColumnEditorAttr(key = "components.video_player.responsiveWidth", value = "responsive")
    }))
    private String widthType = "fixed";

    @DataTableColumn(inputType = DataTableColumnType.RADIO, title = "components.video_player.videoAlign", tab = "basic", editor = @DataTableColumnEditor(options = {
            @DataTableColumnEditorAttr(key = "components.video_player.videoAlign-left", value = "left"),
            @DataTableColumnEditorAttr(key = "components.video_player.videoAlign-center", value = "center"),
            @DataTableColumnEditorAttr(key = "components.video_player.videoAlign-right", value = "right")
    }))
    private String videoAlignYT = "left";

    @DataTableColumn(inputType = DataTableColumnType.RADIO, title = "components.video_player.videoAlign", tab = "basic", editor = @DataTableColumnEditor(options = {
        @DataTableColumnEditorAttr(key = "components.video_player.videoAlign-left", value = "left"),
        @DataTableColumnEditorAttr(key = "components.video_player.videoAlign-center", value = "center"),
        @DataTableColumnEditorAttr(key = "components.video_player.videoAlign-right", value = "right")
    }))
    private String videoAlignVimeo = "left";

    @DataTableColumn(inputType = DataTableColumnType.RADIO, title = "components.video_player.videoAlign", tab = "basic", editor = @DataTableColumnEditor(options = {
        @DataTableColumnEditorAttr(key = "components.video_player.videoAlign-left", value = "left"),
        @DataTableColumnEditorAttr(key = "components.video_player.videoAlign-center", value = "center"),
        @DataTableColumnEditorAttr(key = "components.video_player.videoAlign-right", value = "right")
    }))
    private String videoAlignFacebook = "left";

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.video_player.width", tab = "basic")
    private Integer width = 425;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.video_player.height", tab = "basic")
    private Integer height = 355;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.video_player.widthPercentage", tab = "basic")
    private Integer percentageWidth = 100;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "components.video_player.autoplay", tab = "basic")
    private Boolean autoplay = false;
    
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "components.video_player.showinfo", tab = "basic")
    private Boolean showinfo = false;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "components.video_player.byline", tab = "basic")
    private boolean byline = false;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "components.video_player.branding", tab = "basic")
    private Boolean branding = false;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "components.video_player.fullscreen", tab = "basic")
    private Boolean fullscreen = true;
    
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "components.video_player.controls", tab = "basic")
    private Boolean controls = true;
    
    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "components.video_player.rel", tab = "basic")
    private Boolean rel = true;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "components.video_player.portrait", tab = "basic")
    private boolean portrait = true;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "components.video_player.badge", tab = "basic")
    private boolean badge = true;

    @Override
    public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
        Map<String, List<OptionDto>> options = new HashMap<>();

        List<OptionDto> optionsMap = DatatableTools.getImageRadioOptions("/components/video/admin-styles/");
        optionsMap.sort(Comparator.comparingInt(option -> {
            switch (option.getValue()) {
                case "logo_youtube_color": return 1;
                case "logo_vimeo_color": return 2;
                case "logo_facebook_color": return 3;
                case "logo_video_color": return 3;
                default: return 99;
            }
        }));
        options.put("field", optionsMap);

        return options;
    }
}
