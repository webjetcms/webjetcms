package sk.iway.iwcm.components.carouselslider;

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

@WebjetComponent("sk.iway.iwcm.components.carouselslider.CarouselSliderApp")
@WebjetAppStore(
        nameKey = "components.app-carousel_slider.title",
        descKey = "components.app-carousel_slider.desc",
        itemKey = "carousel_slider",
        imagePath = "/components/carousel_slider/editoricon.png",
        galleryImages = "/components/carousel_slider/",
        componentPath = "/components/carousel_slider/carousel_slider.jsp",
        customHtml = "/apps/carousel_slider/admin/editor-component.html"
)
@DataTableTabs(tabs = {
        @DataTableTab(id = "basic", title = "components.carousel_slider.skin", selected = true),
        @DataTableTab(id = "advanced", title = "components.carousel_slider.settings"),
        @DataTableTab(id = "files", title = "components.carousel_slider.files"),
})
@Getter
@Setter
public class CarouselSliderApp extends WebjetComponentAbstract {
    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "basic", title="components.carousel_slider.skin", editor = {
            @DataTableColumnEditor(options = {
                    @DataTableColumnEditorAttr(key = "components.carousel_slider.skin.autoScroller", value = "AutoScroller"),
                    @DataTableColumnEditorAttr(key = "components.carousel_slider.skin.classic", value = "Classic"),
                    @DataTableColumnEditorAttr(key = "components.carousel_slider.skin.fashion", value = "Fashion"),
                    @DataTableColumnEditorAttr(key = "components.carousel_slider.skin.gallery", value = "Gallery"),
                    @DataTableColumnEditorAttr(key = "components.carousel_slider.skin.rotator", value = "Rotator"),
                    @DataTableColumnEditorAttr(key = "components.carousel_slider.skin.simplicity", value = "Simplicity"),
                    @DataTableColumnEditorAttr(key = "components.carousel_slider.skin.stylish", value = "Stylish")
            })
    })
    private String skin = "Classic";

    @DataTableColumn(inputType = DataTableColumnType.STATIC_TEXT, tab = "basic", title="&nbsp;")
    private String styleImageIMG;

    // dalsia strana

    @DataTableColumn(inputType = DataTableColumnType.STATIC_TEXT, tab = "advanced", title = "components.carousel_slider.sliderDimensions")
    private String label;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.carousel_slider.carouselWidth", tab = "advanced")
    private Integer carouselWidth = 900;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.carousel_slider.carouselHeight", tab = "advanced")
    private Integer carouselHeight = 300;

    @DataTableColumn(inputType = DataTableColumnType.STATIC_TEXT, tab = "advanced", title = "components.carousel_slider.thumbQuality")
    private String label2;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.carousel_slider.imageWidth", tab = "advanced")
    private Integer imageWidth = 300;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.carousel_slider.imageHeight", tab = "advanced")
    private Integer imageHeight = 300;

    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "advanced", title = "components.carousel_slider.imgPerSlide", editor = {
            @DataTableColumnEditor(options = {
                    @DataTableColumnEditorAttr(key = "1", value = "1"),
                    @DataTableColumnEditorAttr(key = "2", value = "2"),
                    @DataTableColumnEditorAttr(key = "3", value = "3"),
                    @DataTableColumnEditorAttr(key = "4", value = "4"),
                    @DataTableColumnEditorAttr(key = "5", value = "5"),
                    @DataTableColumnEditorAttr(key = "6", value = "6")
            })
    })
    private Integer imgPerSlide = 4;

    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "advanced", title = "components.carousel_slider.direction", editor = {
        @DataTableColumnEditor(options = {
                @DataTableColumnEditorAttr(key = "components.carousel_slider.direction.horizontal", value = "horizontal"),
                @DataTableColumnEditorAttr(key = "components.carousel_slider.direction.vertical", value = "vertical")
        })
    })
    private String direction;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, tab = "advanced", title="components.carousel_slider.showLightbox")
    private boolean showLightbox = true;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "components.carousel_slider.rowNumber", tab = "advanced")
    private Integer rowNumber = 1;

    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "advanced", title = "components.carousel_slider.navStyle", editor = {
        @DataTableColumnEditor(options = {
            @DataTableColumnEditorAttr(key = "components.carousel_slider.none", value = "none"),
            @DataTableColumnEditorAttr(key = "components.carousel_slider.bullets", value = "bullets"),
        })
    })
    private String nav_style = "bullets";

    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "advanced", title = "components.carousel_slider.displayArrows", editor = {
        @DataTableColumnEditor(options = {
            @DataTableColumnEditorAttr(key = "components.carousel_slider.none", value = "none"),
            @DataTableColumnEditorAttr(key = "components.carousel_slider.always", value = "always"),
            @DataTableColumnEditorAttr(key = "components.carousel_slider.mouseover", value = "mouseover"),
        })
    })
    private String arrow_style = "always";

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, tab = "advanced", title="components.carousel_slider.touchSwipe")
    private boolean touch_swipe = true;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, tab = "advanced", title="components.carousel_slider.randomPlay")
    private boolean random_play;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, tab = "advanced", title="components.carousel_slider.autoplay")
    private boolean autoplay = true;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, tab = "advanced", title="components.carousel_slider.mouseoverPause")
    private boolean pause_on_mouse_over;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, tab = "advanced", title="components.carousel_slider.circular")
    private boolean circular = true;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, tab = "advanced", title="components.carousel_slider.showShadow")
    private boolean show_shadow_bottom;

    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.slider.loop", tab = "advanced", editor = {
            @DataTableColumnEditor(options = {
                    @DataTableColumnEditorAttr(key = "components.slider.loopForever", value = "1"),
                    @DataTableColumnEditorAttr(key = "components.slider.loopEndAfter", value = "2")
            })
    })
    private String display_mode = "1";

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, title = "&nbsp;", tab = "advanced")
    private Integer loop_number = 0;

    @DataTableColumn(inputType = DataTableColumnType.NUMBER, tab = "advanced", title = "components.carousel_slider.interval")
    private Integer autoplay_interval = 5000;

    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, tab = "files", title="&nbsp;", className = "dt-json-editor",editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.components.carouselslider.CarouselSliderItem"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-localJson", value = "true")
            }
        )})
    private String editorData = null;
}