window.pbCustomOptions = function(options) {
    if (window.location.pathname == "/test-stavov/page-builder/style-test-osk.html") {
        //custom code to modify options on page builder init
        console.log("pbCustomOptions called, options=", options, "href=", window.location.href);
        options.color_swatches = [
            "#ff0000",
            "#00ff00",
            "#0000ff",
            "#ffff00",
            "#00ffff",
        ];
        //disable any color picker, only swatches will be available
        options.color_picker = false;
    }
};

window.pbCustomSettings = function(me) {
    if (window.location.pathname == "/test-stavov/page-builder/style-test-osk.html") {
        //custom code to run on page builder init
        console.log("pbCustomSettings called, me=", me);

        //redefine grid column content selector
        me.grid.column_content = "div.osk-content";
    }
};