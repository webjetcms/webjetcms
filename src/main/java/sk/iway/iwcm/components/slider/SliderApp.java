package sk.iway.iwcm.components.slider;

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
import sk.iway.iwcm.system.datatable.annotations.DataTableTab;
import sk.iway.iwcm.system.datatable.annotations.DataTableTabs;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@WebjetComponent("sk.iway.iwcm.components.slider.SliderApp")
@WebjetAppStore(nameKey = "components.slider.title", descKey = "components.slider.desc", itemKey = "cmp_slider", imagePath = "/components/slider/editoricon.png", galleryImages = "/components/slider/", componentPath = "/components/slider/slider.jsp", customHtml = "/apps/slider/admin/editor-component.html")
@DataTableTabs(tabs = {
        @DataTableTab(id = "basic", title = "components.universalComponentDialog.title", selected = true),
        @DataTableTab(id = "advanced", title = "datatable.tab.advanced"),
        @DataTableTab(id = "transitions", title = "components.slider.transitions"),
        @DataTableTab(id = "files", title = "components.slider.files"),
})
@Getter
@Setter
public class SliderApp extends WebjetComponentAbstract {
    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "basic", title = "components.menu.class_type", editor = {
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

    @DataTableColumn(inputType = DataTableColumnType.IMAGE_RADIO, title = "components.roots.new.style", tab = "basic", className = "image-radio-horizontal image-radio-fullwidth")
    private String style;

    @Override
    public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
        Map<String, List<OptionDto>> options = new HashMap<>();

        options.put("style", DatatableTools.getImageRadioOptions("/components/carousel_slider/admin-styles/"));
        return options;
    }

    // pokrocile

    @DataTableColumn(inputType = DataTableColumnType.STATIC_TEXT, tab = "advanced", title = "components.carousel_slider.sliderDimensions")
    private String label;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title = "components.slider.fullWidthSlider", tab = "advanced")
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
    private String arrowStyle = "mouseover";

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title = "components.slider.kenBurnsOnSlide", tab = "advanced")
    private boolean kenBurnsOnSlide;

    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.slider.navStyle", tab = "advanced", editor = {
            @DataTableColumnEditor(options = {
                    @DataTableColumnEditorAttr(key = "components.slider.none", value = "none"),
                    @DataTableColumnEditorAttr(key = "components.slider.bullets", value = "bullets"),
                    @DataTableColumnEditorAttr(key = "components.slider.numbers", value = "numbering"),
                    @DataTableColumnEditorAttr(key = "components.slider.thumbnails", value = "thumbnails")
            })
    })
    private String navStyle = "bullets";

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title = "components.slider.navShowThumb", tab = "advanced")
    private boolean showThumbnails = true;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title = "components.slider.touchSwipe", tab = "advanced")
    private boolean touchSwipe = true;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title = "components.slider.randomPlay", tab = "advanced")
    private boolean randomPlay;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title = "components.slider.autoplay", tab = "advanced")
    private boolean autoplay = true;

    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.slider.loop", tab = "advanced", editor = {
            @DataTableColumnEditor(options = {
                    @DataTableColumnEditorAttr(key = "components.slider.loopForever", value = "1"),
                    @DataTableColumnEditorAttr(key = "components.slider.loopEndAfter", value = "2")
            })
    })
    private String displayMode = "1";

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "&nbsp;", tab = "advanced")
    private Integer loopNumber = 0;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.slider.interval", tab = "advanced", editor = @DataTableColumnEditor(options = {
            @DataTableColumnEditorAttr(key = "min", value = "500"),
            @DataTableColumnEditorAttr(key = "step", value = "1")
    }))
    private Integer autoplayInterval = 5000;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title = "components.slider.showTimer", tab = "advanced")
    private boolean showCountdown = true;

    @DataTableColumn(inputType = DataTableColumnType.COLOR, title = "components.slider.timerColor", tab = "advanced")
    private String autoplayCountdownColor = "#ffffff";

    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.slider.timerPosition", tab = "advanced", editor = {
            @DataTableColumnEditor(options = {
                    @DataTableColumnEditorAttr(key = "components.slider.top", value = "top"),
                    @DataTableColumnEditorAttr(key = "components.slider.bottom", value = "bottom")
            })
    })
    private String countdownPosition = "bottom";

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title = "components.slider.transitionOnFirstSlide", tab = "advanced")
    private boolean transitionOnFirstSlide;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title = "components.slider.mouseoverPause", tab = "advanced")
    private boolean pauseOnMouseover;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title = "components.slider.numbering", tab = "advanced")
    private boolean showNumbering;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title = "components.slider.showShadow", tab = "advanced")
    private boolean showShadowBottom = true;

    // prechody

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, tab = "transitions", title = "components.slider.transition_fade", editor = @DataTableColumnEditor(message = "<img alt='FADE' src='/components/slider/transitions/fade.gif' class='tooltipContent'>"))
    private boolean transition_fade;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, tab = "transitions", title = "components.slider.transition_crossFade", editor = @DataTableColumnEditor(message = "<img alt='CROSS FADE' src='/components/slider/transitions/cross_fade.gif' class='tooltipContent'>"))
    private boolean transition_cross_fade;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, tab = "transitions", title = "components.slider.transition_slide", editor = @DataTableColumnEditor(message = "<img alt='SLIDE' src='/components/slider/transitions/slide.gif' class='tooltipContent'>"))
    private boolean transition_slide;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, tab = "transitions", title = "components.slider.transition_elastic", editor = @DataTableColumnEditor(message = "<img alt='ELASTIC' src='/components/slider/transitions/elastic.gif' class='tooltipContent'>"))
    private boolean transition_elastic;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, tab = "transitions", title = "components.slider.transition_slice", editor = @DataTableColumnEditor(message = "<img alt='SLICE' src='/components/slider/transitions/slice.gif' class='tooltipContent'>"))
    private boolean transition_slice;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, tab = "transitions", title = "components.slider.transition_blinds", editor = @DataTableColumnEditor(message = "<img alt='BLINDS' src='/components/slider/transitions/blinds.gif' class='tooltipContent'>"))
    private boolean transition_blinds;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, tab = "transitions", title = "components.slider.transition_blocks", editor = @DataTableColumnEditor(message = "<img alt='BLOCKS' src='/components/slider/transitions/blocks.gif' class='tooltipContent'>"))
    private boolean transition_blocks;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, tab = "transitions", title = "components.slider.transition_shuffle", editor = @DataTableColumnEditor(message = "<img alt='SHUFFLE' src='/components/slider/transitions/shuffle.gif' class='tooltipContent'>"))
    private boolean transition_shuffle;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, tab = "transitions", title = "components.slider.transition_tiles", editor = @DataTableColumnEditor(message = "<img alt='TILES' src='/components/slider/transitions/tiles.gif' class='tooltipContent'>"))
    private boolean transition_tiles;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, tab = "transitions", title = "components.slider.transition_flip", editor = @DataTableColumnEditor(message = "<img alt='FLIP' src='/components/slider/transitions/flip.gif' class='tooltipContent'>"))
    private boolean transition_flip;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, tab = "transitions", title = "components.slider.transition_flipWithZoom", editor = @DataTableColumnEditor(message = "<img alt='FLIP WITH ZOOM' src='/components/slider/transitions/flipWithZoom.gif' class='tooltipContent'>"))
    private boolean transition_flip_with_zoom;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, tab = "transitions", title = "components.slider.transition_3D", editor = @DataTableColumnEditor(message = "<img alt='3D' src='/components/slider/transitions/3D.gif' class='tooltipContent'>"))
    private boolean transition_threed;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, tab = "transitions", title = "components.slider.transition_3DHorizontal", editor = @DataTableColumnEditor(message = "<img alt='3D HORIZONTAL' src='/components/slider/transitions/3Dhorizontal.gif' class='tooltipContent'>"))
    private boolean transition_threed_horizontal;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, tab = "transitions", title = "components.slider.transition_3DWithZoom", editor = @DataTableColumnEditor(message = "<img alt='3D WITH ZOOM' src='/components/slider/transitions/3DwithZoom.gif' class='tooltipContent'>"))
    private boolean transition_threed_with_zoom;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, tab = "transitions", title = "components.slider.transition_3DHorizontalWithZoom", editor = @DataTableColumnEditor(message = "<img alt='3D HORIZONTAL WITH ZOOM' src='/components/slider/transitions/3DflipWithZoom.gif' class='tooltipContent'>"))
    private boolean transition_threed_horizontal_with_zoom;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, tab = "transitions", title = "components.slider.transition_3DFlip", editor = @DataTableColumnEditor(message = "<img alt='3D FLIP' src='/components/slider/transitions/3Dflip.gif' class='tooltipContent'>"))
    private boolean transition_threed_flip;

}