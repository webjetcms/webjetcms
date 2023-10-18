import { JstreeSettings } from '../../libs/js-tree-extends/jstreesettings'
import { WebPagesDatatable } from './web-pages-datatable'

//console.log("web-pages-list.js start");

window.jstreeSettings = new JstreeSettings({
    storageKey: "jstreeSettings_web-pages-list"
});

window.WebPagesDatatable = WebPagesDatatable;

window.domReady.add(function () {
    //inicializujeme na zaciatku aby potom DT vedela pouzit hodnoty

});

/**
 * pridaj spustenie na konci, ma to dole poradie 10,
 * cize inicializuje sa az po nastaveni DT objektov
 */
window.domReady.add(function () {
    //nastavenie jstree
    window.jstreeSettings.bindEvents();
}, 10);