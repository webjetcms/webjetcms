import {Tools} from "../../tools/tools";
import AbstractJsTreeOpener from "../abstract-opener/abstract-js-tree-opener";

/**
 *
 */
export class JsTreeFolderOpener extends AbstractJsTreeOpener {

    constructor() {
        super();
        this.idKeyName = 'groupid';
        this.apiUrl = '/admin/rest/groups/parents/';
    }

    /**
     * @description Otvorí nasledujúci node v poradí.
     *              Ak už neexistuje v zozname žiadny ďalší node, otvorý dataTable dokument s našim ID.
     * @returns {void}
     * @public
     */
    next() {
        //console.log("next, _currentId=", this._currentId, "jstree=", this.jstree);
        if (this._currentId < 0) {
            return;
        }
        /** @type {number[]} */
        const store = this.getCurrentStore();
        if (!Tools.empty(store)) {
            /** @type {null|number} */
            const currentParent = this.getNextStoreItem();
            const node = this.jstree.get_node(currentParent);
            //console.log("currentParent=", currentParent, "obj=", node, "store=", this.getCurrentStore());
            if (node != undefined && node != null && typeof node.state != "undefined" && node.state.loaded === true) {
                //nod je uz nahraty, vyvolaj next
                let that = this;
                setTimeout(()=> {
                    that.next();
                }, 100);
            }
            this.openNextNode(currentParent);
        } else {
            //console.log("Done, reseting");
            this.reset();

            let docid = this.query?.["docid"] ?? null;
            //console.log("Mam docid=", docid);
            if (docid != null && docid === "-1") {
                setTimeout(()=>{
                    //console.log("Je to -1, otvaram novu stranku");
                    $("#"+window.jsTreeDocumentOpener.dataTable.DATA.id+"_wrapper div.dt-buttons button.buttons-create").trigger("click");
                }, 300);
            }
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