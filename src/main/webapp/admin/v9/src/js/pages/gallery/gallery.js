import { JstreeSettings } from '../../libs/js-tree-extends/jstreesettings'

//console.log("gallery.js start");

window.jstreeSettings = new JstreeSettings({
    storageKey: "jstreeSettings_gallery"
});

/**
 * pridaj spustenie na konci, ma to dole poradie 10,
 * cize inicializuje sa az po nastaveni DT objektov
 */
window.domReady.add(function () {
    //nastavenie jstree
    window.jstreeSettings.bindEvents();
}, 10);