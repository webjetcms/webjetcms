import { JstreeSettings } from '../../libs/js-tree-extends/jstreesettings'
import { WebPagesDatatable } from './web-pages-datatable'
import { bindDeleteMirrors } from './delete-mirrors'
import { bindMirrorLinks } from './mirror-links'

//console.log("web-pages-list.js start");

window.jstreeSettings = new JstreeSettings({
    storageKey: "jstreeSettings_web-pages-list"
});

// Send the current toggle immediately, without waiting for the settings save request.
window.getJstreeUrl = function() {
    let url = WJ.urlUpdateParam($("#SomStromcek").data("rest-url"), "showBadMirrorings", window.jstreeSettings.isShowBadMirrorings());
    return WJ.urlUpdateParam(url, "showMirrorLinks", true);
};

window.WebPagesDatatable = WebPagesDatatable;
window.bindDeleteMirrors = bindDeleteMirrors;

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
    bindMirrorLinks();
}, 10);
