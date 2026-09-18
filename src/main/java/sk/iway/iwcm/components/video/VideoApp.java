package sk.iway.iwcm.components.video;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
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

    private static final Pattern YOUTUBE_TIME = Pattern.compile("(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?");

    @DataTableColumn(inputType = DataTableColumnType.IMAGE_RADIO, title = "&nbsp", tab = "basic", className = "image-radio-horizontal image-radio-fullwidth")
    private String field;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.video_player.file", tab = "basic")
    private String file;

    @DataTableColumn(inputType = DataTableColumnType.ELFINDER, title = "components.video_player.file_video", tab = "basic", className = "dt-app-skip video")
    private String videoFile;

    @DataTableColumn(inputType = DataTableColumnType.RADIO, title = "&nbsp", tab = "basic", editor = @DataTableColumnEditor(options = {
            @DataTableColumnEditorAttr(key = "components.video_player.fixedWidth", value = "fixed"),
            @DataTableColumnEditorAttr(key = "components.video_player.responsiveWidth", value = "responsive")
    }))
    private String widthType = "responsive";

    @DataTableColumn(inputType = DataTableColumnType.RADIO, title = "components.video_player.videoAlign", tab = "basic", editor = @DataTableColumnEditor(options = {
            @DataTableColumnEditorAttr(key = "components.video_player.videoAlign-left", value = "left"),
            @DataTableColumnEditorAttr(key = "components.video_player.videoAlign-center", value = "center"),
            @DataTableColumnEditorAttr(key = "components.video_player.videoAlign-right", value = "right")
    }))
    private String align = "center";

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.video_player.width", tab = "basic")
    private Integer width = 425;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.video_player.height", tab = "basic")
    private Integer height = 355;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.video_player.widthPercentage", tab = "basic")
    private Integer percentageWidth = 100;

    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title = "components.video_player.ratioClass",
        tab = "basic"
    )
    private String ratioClass = "embed-responsive embed-responsive-16by9 ratio ratio-16x9";

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

    /**
     * Converts a YouTube page or share URL to an embed URL. Component settings
     * override matching URL parameters, while the shared playback time is preserved.
     */
    public static String getYoutubeEmbedUrl(String file, String playerParameters) {
        if (file == null || file.isBlank()) return "";

        try {
            URIBuilder source = new URIBuilder(file.trim().replace("&amp;", "&"));
            Map<String, String> parameters = new LinkedHashMap<>();
            source.getQueryParams().forEach(parameter -> parameters.putIfAbsent(parameter.getName(), parameter.getValue()));

            String videoId = parameters.remove("v");
            if (videoId == null) {
                String path = source.getPath();
                if (path == null) return "";
                videoId = path.substring(path.lastIndexOf('/') + 1);
            }
            if (!videoId.matches("[a-zA-Z0-9_-]+")) return "";

            int start = parseYoutubeTime(parameters.remove("start"));
            String time = parameters.remove("t");
            if (start < 0) start = parseYoutubeTime(time);
            if (start >= 0) parameters.put("start", Integer.toString(start));

            URLEncodedUtils.parse(playerParameters, StandardCharsets.UTF_8)
                .forEach(parameter -> parameters.put(parameter.getName(), parameter.getValue()));

            URIBuilder embed = new URIBuilder("//www.youtube.com/embed/" + videoId);
            parameters.forEach(embed::addParameter);
            return embed.toString();
        } catch (URISyntaxException ex) {
            return "";
        }
    }

    private static int parseYoutubeTime(String value) {
        if (value == null || value.isEmpty()) return -1;

        try {
            if (value.matches("\\d+")) return Integer.parseInt(value);

            Matcher matcher = YOUTUBE_TIME.matcher(value);
            if (!matcher.matches()) return -1;

            long seconds = 0;
            for (int group = 1; group <= 3; group++) {
                seconds = seconds * 60 + (matcher.group(group) == null ? 0 : Integer.parseInt(matcher.group(group)));
            }
            return seconds <= Integer.MAX_VALUE ? (int) seconds : -1;
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

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

        String videoClasses = Constants.getString("videoClasses", "");
        if (Tools.isNotEmpty(videoClasses)) {
            List<OptionDto> ratioOptions = parseOptionsFromConfig(request, videoClasses);
            addCurrentValueToOptions(ratioOptions, ratioClass);
            options.put("ratioClass", ratioOptions);
        }

        return options;
    }
}
