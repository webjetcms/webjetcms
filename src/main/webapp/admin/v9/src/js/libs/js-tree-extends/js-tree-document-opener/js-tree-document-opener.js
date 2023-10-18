import {Tools} from "../../tools/tools";
import AbstractJsTreeOpener from "../abstract-opener/abstract-js-tree-opener";

/**
 *
 */
export class JsTreeDocumentOpener extends AbstractJsTreeOpener {

    constructor() {
        super();
        this.idKeyName = 'docid';
        this.apiUrl = '/admin/rest/web-pages/parents/';
    }

    /**
     * @description Otvorí nasledujúci node v poradí.
     *              Ak už neexistuje v zozname žiadny ďalší node, otvorý dataTable dokument s našim ID.
     * @returns {void}
     * @public
     */
    next() {
        //console.log("callying next, id=", this._currentId);
        if (this._currentId < 0) {
            return;
        }
        /** @type {number[]} */
        const store = this.getCurrentStore();
        if (Tools.empty(store) && !this.notFound) {
            setTimeout(()=> {
                //console.log("calling openEditor, id=", this._currentId);
                //inak robilo problem otvorenie /admin/v9/webpages/web-pages-list/?groupid=8694&docid=141
                this.openEditor(this._currentId);
                this.reset();
            }, 500);
        } else {
            //console.log("calling openNextNode");
            /** @type {null|number} */
            const currentParent = this.getNextStoreItem();
            this.openNextNode(currentParent);
        }
    }

    /**
     * @description Pripojí zadaný input, ktorý musí byť vložený ako jQuery objekt `$('.input-css-trieda')`
     *              Input zabezpečuje zadanie nového ID
     * @param {jQuery} openerInput jQuery selector for input
     * @param {function} [withNotifyCallback] Callback, ktorý sa vykoná, pri chybnom vstupe alebo nenájdenom dokumente.
     *                                        V callbacku je dostupný argument `value`
     * @returns {void}
     * @public
     */
    inputDataFrom(openerInput, withNotifyCallback) {
        this.bindInput(openerInput, this, withNotifyCallback);
    }
}