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

/**
 * Customize tab menu in style dialog
 * @param {Object} me - page builder instance
 * @param {JSON} tabMenu - tab menu object
 * @returns {JSON} modified tab menu
 */
window.pbBuildTabMenuXXX = function(me, tabMenu) {

    console.log("pbBuildTabMenu called, me=", me, "tabMenu=", tabMenu);
    //hide first tab
    tabMenu.tabs[0].visible = false;

    //move tab id=10 to the first position
    var items = tabMenu.tabs[1].items;
    const tab10Index = items.findIndex(tab => tab.id === "10");
    if (tab10Index > -1) {
        const [tab10] = items.splice(tab10Index, 1);
        items.unshift(tab10);
    }

    //hide 09,11,12
    items.forEach(tab => {
        if (["09", "11", "12"].includes(tab.id)) {
            tab.visible = false;
        }
    });

    return tabMenu;

};