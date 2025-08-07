package sk.iway.iwcm.components.appslider;

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
import sk.iway.iwcm.system.datatable.annotations.DataTableTab;
import sk.iway.iwcm.system.datatable.annotations.DataTableTabs;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@WebjetComponent("sk.iway.iwcm.components.appslider.SliderApp")
@WebjetAppStore(
        nameKey = "components.slider.title",
        descKey = "components.slider.desc",
        itemKey = "cmp_slider",
        imagePath = "/components/slider/editoricon.png",
        galleryImages = "/components/slider/",
        componentPath = "/components/slider/slider.jsp",
        customHtml = "/apps/slider/admin/editor-component.html"
)
@DataTableTabs(tabs = {
        @DataTableTab(id = "basic", title = "components.slider.settings", selected = true),
        @DataTableTab(id = "advanced", title = "datatable.tab.advanced"),
        @DataTableTab(id = "transitions", title = "components.slider.transitions"),
        @DataTableTab(id = "files", title = "components.slider.files"),
})
@Getter
@Setter
public class SliderApp extends WebjetComponentAbstract {
    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "basic", title="components.slider.skin", editor = {
            @DataTableColumnEditor(options = {
                    @DataTableColumnEditorAttr(key = "Classic", value = "Classic"),
                    @DataTableColumnEditorAttr(key = "ContentBox", value = "ContentBox"),
                    @DataTableColumnEditorAttr(key = "Cube", value = "Cube"),
                    @DataTableColumnEditorAttr(key = "Elegant", value = "Elegant"),
                    @DataTableColumnEditorAttr(key = "Events", value = "Events"),
                    @DataTableColumnEditorAttr(key = "FeatureList", value = "FeatureList"),
                    @DataTableColumnEditorAttr(key = "FrontPage", value = "FrontPage"),
                    @DataTableColumnEditorAttr(key = "Gallery", value = "Gallery"),
                    @DataTableColumnEditorAttr(key = "Header", value = "Header"),
                    @DataTableColumnEditorAttr(key = "Lightbox", value = "Lightbox"),
                    @DataTableColumnEditorAttr(key = "TextNavigation", value = "TextNavigation"),
            })
    })
    private String skin;

    @DataTableColumn(inputType = DataTableColumnType.STATIC_TEXT, tab = "basic", title="&nbsp;")
    private String styleImageIMG;

    @Override
    public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
        Map<String, List<OptionDto>> options = new HashMap<>();

        options.put("style", DatatableTools.getImageRadioOptions("/components/carousel_slider/admin-styles/"));
        return options;
    }

    // pokrocile

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "components.slider.fullWidthSlider", tab = "advanced")
    private boolean fullWidthSlider = true;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.slider.sliderWidth", tab = "advanced")
    private Integer sliderWidth = 900;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.slider.sliderHeight", tab = "advanced")
    private Integer sliderHeight = 360;

    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.slider.displayArrows", tab = "advanced", editor = {
            @DataTableColumnEditor(options = {
                    @DataTableColumnEditorAttr(key = "components.slider.none", value = "none"),
                    @DataTableColumnEditorAttr(key = "components.slider.always", value = "always"),
                    @DataTableColumnEditorAttr(key = "components.slider.mouseover", value = "mouseover")
            })
    })
    private String arrow_style = "mouseover";

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "components.slider.kenBurnsOnSlide", tab = "advanced")
    private boolean ken_burns_on_slide;

    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.slider.navStyle", tab = "advanced", editor = {
            @DataTableColumnEditor(options = {
                    @DataTableColumnEditorAttr(key = "components.slider.none", value = "none"),
                    @DataTableColumnEditorAttr(key = "components.slider.bullets", value = "bullets"),
                    @DataTableColumnEditorAttr(key = "components.slider.numbers", value = "numbering"),
                    @DataTableColumnEditorAttr(key = "components.slider.thumbnails", value = "thumbnails")
            })
    })
    private String nav_style = "bullets";

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "components.slider.navShowThumb", tab = "advanced")
    private boolean show_thumbnails = true;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "components.slider.touchSwipe", tab = "advanced")
    private boolean touch_swipe = true;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "components.slider.randomPlay", tab = "advanced")
    private boolean random_play;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "components.slider.autoplay", tab = "advanced")
    private boolean autoplay = true;

    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.slider.loop", tab = "advanced", editor = {
            @DataTableColumnEditor(options = {
                    @DataTableColumnEditorAttr(key = "components.slider.loopForever", value = "1"),
                    @DataTableColumnEditorAttr(key = "components.slider.loopEndAfter", value = "2")
            })
    })
    private String display_mode = "1";

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "&nbsp;", tab = "advanced")
    private Integer loop_number = 0;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.slider.interval", tab = "advanced")
    private Integer autoplay_interval = 5000;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "components.slider.showTimer", tab = "advanced")
    private boolean show_countdown = true;

    @DataTableColumn(inputType = DataTableColumnType.COLOR, title = "components.slider.timerColor", tab = "advanced")
    private String autoplay_countdown_color = "#ffffff";

    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.slider.timerPosition", tab = "advanced", editor = {
            @DataTableColumnEditor(options = {
                    @DataTableColumnEditorAttr(key = "components.slider.top", value = "top"),
                    @DataTableColumnEditorAttr(key = "components.slider.bottom", value = "bottom")
            })
    })
    private String countdown_position = "bottom";

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "components.slider.transitionOnFirstSlide", tab = "advanced")
    private boolean transition_on_first_slide;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "components.slider.mouseoverPause", tab = "advanced")
    private boolean pause_on_mousover;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "components.slider.numbering", tab = "advanced")
    private boolean show_numbering;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, title = "components.slider.showShadow", tab = "advanced")
    private boolean show_shadow_bottom = true;

    // prechody
    @DataTableColumn(inputType = DataTableColumnType.MULTISELECT, tab = "transitions", title = "components.slider.transition_style", editor = {
        @DataTableColumnEditor(options = {
                @DataTableColumnEditorAttr(key = "components.slider.transition_fade", value = "fade"),
                @DataTableColumnEditorAttr(key = "components.slider.transition_crossFade", value = "crossfade"),
                @DataTableColumnEditorAttr(key = "components.slider.transition_slide", value = "slide"),
                @DataTableColumnEditorAttr(key = "components.slider.transition_elastic", value = "elastic"),
                @DataTableColumnEditorAttr(key = "components.slider.transition_slice", value = "slice"),
                @DataTableColumnEditorAttr(key = "components.slider.transition_blinds", value = "blinds"),
                @DataTableColumnEditorAttr(key = "components.slider.transition_blocks", value = "blocks"),
                @DataTableColumnEditorAttr(key = "components.slider.transition_shuffle", value = "shuffle"),
                @DataTableColumnEditorAttr(key = "components.slider.transition_tiles", value = "tiles"),
                @DataTableColumnEditorAttr(key = "components.slider.transition_flip", value = "flip"),
                @DataTableColumnEditorAttr(key = "components.slider.transition_flipWithZoom", value = "flipwithzoom"),
                @DataTableColumnEditorAttr(key = "components.slider.transition_3D", value = "threed"),
                @DataTableColumnEditorAttr(key = "components.slider.transition_3DHorizontal", value = "threedhorizontal"),
                @DataTableColumnEditorAttr(key = "components.slider.transition_3DWithZoom", value = "threedwithzoom"),
                @DataTableColumnEditorAttr(key = "components.slider.transition_3DHorizontalWithZoom", value = "threedhorizontalwithzoom"),
                @DataTableColumnEditorAttr(key = "components.slider.transition_3DFlip", value = "threedflip"),
                @DataTableColumnEditorAttr(key = "components.slider.transition_3DFlipWithZoom", value = "threedflipwithzoom"),
                @DataTableColumnEditorAttr(key = "components.slider.transition_3DTiles", value = "threedtiles"),
                @DataTableColumnEditorAttr(key = "components.slider.transition_kenBurns", value = "kenburns")
            }, attr = {
                @DataTableColumnEditorAttr(key = "separator", value = ",")
        })
    })
    private String transitions_all;

    @DataTableColumn(inputType = DataTableColumnType.STATIC_TEXT, tab = "transitions", title="&nbsp;")
    private String transitionStyleGif;

    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, tab = "files", title="&nbsp;", className = "dt-json-editor",editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.components.appslider.SliderItem"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-localJson", value = "true")
            }
        )})
    private String editorData = null;

}