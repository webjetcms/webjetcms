import $ from 'jquery';
//console.log("Setting jQuery object to window in app.js");
//setnute v DT index.js window.jQuery = $;
//setnute v DT index.js window.$ = $;

$.ajaxSetup({
    headers: {
        'X-CSRF-Token': window.csrfToken,
    },
    statusCode: {
        401: function () {
            WJ.keepSessionShowLogoffMessage();
        },
        403: function () {
            var errorMessage = window.csrfError;
            try {
                WJ.keepSessionShowTokenMessage(errorMessage);
            } catch (e) {
                window.alert(errorMessage);
            }
        },
    },
});

import { Tools } from './libs/tools/tools';
Tools.isDevMode();

import ReadyExtender from './libs/ready-extender/ready-extender';
window.domReady = new ReadyExtender();

import WJ from '../js/webjet.js';

import '../js/plugins/jquery.cookie.js';
import '../js/plugins/modernizr-custom.js';
import 'jstree';
//import * as dtConfig from '../js/datatables-config.js';
import Ninja from '../js/global-functions.js';
import AutoCompleter from '../js/autocompleter.js';

import numeral from 'numeral';
import numeralsk from 'numeral/locales/sk';
import numeralcz from 'numeral/locales/cs';
import numeralde from 'numeral/locales/de';
import moment from 'moment';
import momentsk from 'moment/locale/sk';
import momentcz from 'moment/locale/cs';
import momentde from 'moment/locale/de';
import Scrollbar from 'smooth-scrollbar';
import Quill from 'quill';
import toastr from 'toastr';

/* webjetTranslationService */
import { Translator } from './libs/translator/translator';

window.moment = moment;
window.numeral = numeral;
window.WJ = WJ;
window.Quill = Quill;
window.toastr = toastr;
window.AutoCompleter = AutoCompleter;
window.webjetTranslationService = new Translator();

//import WjPasswordStrength from '../js/passwdstrength.js';
//console.log("WjPasswordStrength=", WjPasswordStrength);

import { WjPasswordStrength } from './libs/wj-password-strength';
global.WjPasswordStrength = WjPasswordStrength;

//console.log("WJ=", WJ);
//console.log("AutoCompleter=", AutoCompleter);

import '../js/datatables-upload.js';

import './ui-config.js';
import 'jquery-ui/ui/widgets/draggable';
import 'jquery-ui/ui/widgets/droppable';
import 'jquery-ui/ui/widgets/autocomplete';
import 'jquery-ui/ui/widgets/selectable';
import 'jquery-ui/ui/widgets/resizable';
import 'jquery-ui/ui/widgets/controlgroup';
import 'jquery-ui/ui/widgets/button';
import 'jquery-ui/ui/widgets/slider';
//import 'jquery-ui/ui/widgets/tooltip'; - set as bsTooltip later

import 'jquery-ui/themes/base/theme.css';
import 'jquery-ui/themes/base/draggable.css';
import 'jquery-ui/themes/base/resizable.css';
import 'jquery-ui/themes/base/slider.css';
//import 'jquery-ui/themes/base/tooltip.css';

import 'bootstrap';
import '../scss/ninja.scss';

//extra css file for inline editing
const createInlineCss = () => {
    return import(/* webpackChunkName: "inline" */ '../scss/inline.scss');
};
window.createInlineCss = createInlineCss;

const bootstrap = (window.bootstrap = require('bootstrap'));
$.fn.bsTooltip = bootstrap.Tooltip.jQueryInterface;
//override UI tooltip with bootstrap tooltip
$.fn.tooltip = bootstrap.Tooltip.jQueryInterface;

//na zaklade https://github.com/snapappointments/bootstrap-select/issues/2505 importovane priamo js a nie dist/js mozno po prechode do stable to bude OK
//tiez musi ist cez require, inak to padalo ze nepozna bootstrap objekt
//require('bootstrap-select/js/bootstrap-select');
//https://gist.github.com/mattymatty76/c996d3b77f298b2ec133be59992df9d4/revisions
require('./plugins/bootstrap-select-v1.14.0-gamma1')
import 'bootstrap-select/dist/css/bootstrap-select.css';

//not used anymore
//require('ajax-bootstrap-select');

import { WebjetJsTree } from './webjet-jstree';
import { JsTreeDocumentOpener, JsTreeFolderOpener } from './libs/js-tree-extends';

window.WebjetJsTree = WebjetJsTree;
window.jsTreeDocumentOpener = new JsTreeDocumentOpener();
window.jsTreeFolderOpener = new JsTreeFolderOpener();

const createPdfMake = () => {
    return import(/* webpackChunkName: "pdfMake" */ 'pdfmake/build/pdfmake');
};
window.createPdfMake = createPdfMake;
const createPdfFonts = () => {
    return import(/* webpackChunkName: "pdfFonts" */ 'pdfmake/build/vfs_fonts');
};
window.createPdfFonts = createPdfFonts;

import * as JSZip from 'jszip';

window.JSZip = JSZip;

import { dataTableInit } from '../../npm_packages/webjetdatatables/index';
window.WJ.DataTable = dataTableInit;
import { CellVisibilityService } from './libs/data-tables-extends/';
window.dataTableCellVisibilityService = new CellVisibilityService();

/* VUE */
import { VueTools } from './libs/tools/vuetools'
VueTools.setup();
window.VueTools = VueTools;

/* DYNAMIC IMPORTS */
const createImageEditor = () => {
    // dynamicky importujem image editor
    return import(/* webpackChunkName: "imageEditor" */ './image-editor');
};
window.imageEditor = createImageEditor;

const createDatatablesCkEditor = () => {
    return import(/* webpackChunkName: "ckeditor" */ './datatables-ckeditor');
};
window.createDatatablesCkEditor = createDatatablesCkEditor;

const importWebPagesDatatable = () => {
    return import(/* webpackChunkName: "web-pages-datatable" */ './pages/web-pages-list/web-pages-datatable');
};
window.importWebPagesDatatable = importWebPagesDatatable;

const createXLSX = () => {
    return import(/* webpackChunkName: "XLSX" */ 'xlsx');
};
window.XLSX = createXLSX;

import * as ChartTools from "./libs/chart/chart-tools.js";
window.ChartTools=ChartTools;

const initAmcharts = () => {
    // dynamicky importujem amchart 5
    return import(/* webpackChunkName: "wjamcharts" */ './libs/chart/amcharts').then((module)=>{
        window.WebjetTheme = module.WebjetTheme

        //Add licence
        if (typeof window.am5license === "string" && window.am5license != null && window.am5license.length > 5) {
            window.am5.addLicense(window.am5license);
        }

        setTimeout(()=>{
            //Add event dispache
            var event = new CustomEvent('WJ.initAmcharts.success', {
                detail: {},
            });
            window.dispatchEvent(event);
        }, 50);
    });
}
window.initAmcharts = initAmcharts;

/**
@deprecated
*/
window.confirmRestart = function()
{
    console.log('Deprecated, use WJ.confirmRestart()');
    return WJ.confirmRestart();
}