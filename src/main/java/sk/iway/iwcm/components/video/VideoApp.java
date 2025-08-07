package sk.iway.iwcm.components.video;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

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

    @DataTableColumn(inputType = DataTableColumnType.ELFINDER, title = "components.video_player.file_video", tab = "basic", className = "dt-app-skip")
    private String videoFile;

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
    private String align = "left";

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.video_player.width", tab = "basic")
    private Integer width = 425;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.video_player.height", tab = "basic")
    private Integer height = 355;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.video_player.widthPercentage", tab = "basic")
    private Integer percentageWidth = 100;

    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, title = "components.video_player.autoplay", tab = "basic",
        editor = {
            @DataTableColumnEditor(
                attr = {@DataTableColumnEditorAttr(key = "unselectedValue", value = "0")},
                options = {@DataTableColumnEditorAttr(key = "editor.form.sl.yes", value = "1")}
            )
        }
    )
    private Integer autoplay = 0;

    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, title = "components.video_player.showinfo", tab = "basic",
        editor = {
            @DataTableColumnEditor(
                attr = {@DataTableColumnEditorAttr(key = "unselectedValue", value = "0")},
                options = {@DataTableColumnEditorAttr(key = "editor.form.sl.yes", value = "1")}
            )
        }
    )
    private Integer showinfo = 0;

    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, title = "components.video_player.byline", tab = "basic",
        editor = {
            @DataTableColumnEditor(
                attr = {@DataTableColumnEditorAttr(key = "unselectedValue", value = "0")},
                options = {@DataTableColumnEditorAttr(key = "editor.form.sl.yes", value = "1")}
            )
        }
    )
    private Integer byline = 0;

    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, title = "components.video_player.branding", tab = "basic",
        editor = {
            @DataTableColumnEditor(
                attr = {@DataTableColumnEditorAttr(key = "unselectedValue", value = "0")},
                options = {@DataTableColumnEditorAttr(key = "editor.form.sl.yes", value = "1")}
            )
        }
    )
    private Integer branding = 0;

    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, title = "components.video_player.fullscreen", tab = "basic",
        editor = {
            @DataTableColumnEditor(
                attr = {@DataTableColumnEditorAttr(key = "unselectedValue", value = "0")},
                options = {@DataTableColumnEditorAttr(key = "editor.form.sl.yes", value = "1")}
            )
        }
    )
    private Integer fullscreen = 1;

    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, title = "components.video_player.controls", tab = "basic",
        editor = {
            @DataTableColumnEditor(
                attr = {@DataTableColumnEditorAttr(key = "unselectedValue", value = "0")},
                options = {@DataTableColumnEditorAttr(key = "editor.form.sl.yes", value = "1")}
            )
        }
    )
    private Integer controls = 1;

    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, title = "components.video_player.rel", tab = "basic",
        editor = {
            @DataTableColumnEditor(
                attr = {@DataTableColumnEditorAttr(key = "unselectedValue", value = "0")},
                options = {@DataTableColumnEditorAttr(key = "editor.form.sl.yes", value = "1")}
            )
        }
    )
    private Integer rel = 1;

    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, title = "components.video_player.portrait", tab = "basic",
        editor = {
            @DataTableColumnEditor(
                attr = {@DataTableColumnEditorAttr(key = "unselectedValue", value = "0")},
                options = {@DataTableColumnEditorAttr(key = "editor.form.sl.yes", value = "1")}
            )
        }
    )
    private Integer portrait = 1;

    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, title = "components.video_player.badge", tab = "basic",
        editor = {
            @DataTableColumnEditor(
                attr = {@DataTableColumnEditorAttr(key = "unselectedValue", value = "0")},
                options = {@DataTableColumnEditorAttr(key = "editor.form.sl.yes", value = "1")}
            )
        }
    )
    private Integer badge = 1;

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
