/**
 * Configuration for the DataTables Editor tree picker.
 *
 * @typedef {Object} WebjetDteJsTreeOptions
 * @property {HTMLInputElement|HTMLTextAreaElement} [inputElement] - Backing form control that receives the selected value and change events.
 * @property {string|null} [dataTableName] - DataTable name used to scope the backing input selector.
 * @property {Object} [dataTable] - DataTable integration object, including optional JSON-field mapping hooks.
 * @property {Object} [dataTable.DATA] - DataTable runtime data.
 * @property {Object|null} [dataTable.DATA.jsonField] - Custom value-mapping hooks.
 * @property {function(WebjetDteJsTreeCompatibilityContext, Object): Object} [dataTable.DATA.jsonField.getItem] - Maps a tree selection to the stored item.
 * @property {function(WebjetDteJsTreeCompatibilityContext, Object): (string|number)} [dataTable.DATA.jsonField.getKey] - Returns the identity used to detect duplicate selections.
 * @property {string} [mode=""] - Tree selection mode encoded by the DataTable field class.
 * @property {Object.<string, string>|null} [attributes] - DataTable field attributes that configure labels, endpoints, roots, and filtering.
 * @property {Object|Object[]|null|string} [value] - Initial item or items; `null` and the empty string represent no selection.
 */

/**
 * Context supplied to custom DataTable JSON-field mapping hooks.
 *
 * @typedef {Object} WebjetDteJsTreeCompatibilityContext
 * @property {Object|undefined} dataTable - Configured DataTable integration object.
 * @property {string|null|undefined} dataTableName - Configured DataTable name.
 * @property {string|undefined} idKey - ID of the backing form control.
 * @property {(Object|string)[]} data - Current selection array.
 * @property {string} click - Current tree selection mode.
 * @property {Object.<string, string>|null|undefined} attr - Configured DataTable field attributes.
 * @property {number|null} index - Index being replaced, or `null` when adding an item.
 * @property {Object|string|null|undefined} grp - Item being replaced, or `null` when adding an item.
 */

let treeCounter = 0;

function createEmptyValue(mode) {
    const item = { fullPath: "" };
    if (mode.includes("dt-tree-page")) item.docId = -1;
    else if (mode.includes("dt-tree-group")) item.groupId = -1;
    else item.id = -1;
    return [item];
}

/**
 * Normalizes a configured value to the component's array representation.
 *
 * Empty non-array modes receive a placeholder item. Existing arrays are retained rather than copied.
 *
 * @param {Object|Object[]|null|string|undefined} value - One item, an item array, or an empty-value sentinel.
 * @param {string} mode - Tree selection mode.
 * @returns {(Object|string)[]} The normalized selection array.
 */
function normalizeValue(value, mode) {
    const array = Array.isArray(value) ? value : (value == null || value === "" ? [] : [value]);
    if (!mode.includes("-array") && array.length === 0) return createEmptyValue(mode);
    return array;
}

function getDisplayPath(item, mode) {
    let path = item?.fullPath || item?.virtualPath || "";
    path = path.replaceAll("&#47;", "/");
    if (item?.domainName && mode.includes("alldomains")) path = `${item.domainName}:${path}`;
    return path;
}

function button(className, icon, label) {
    const element = document.createElement("button");
    element.type = "button";
    element.className = `btn btn-outline-secondary ${className}`;
    element.title = label;
    element.setAttribute("aria-label", label);
    element.innerHTML = `<i class="ti ${icon}" aria-hidden="true"></i>`;
    return element;
}

/**
 * Provides a tree-backed picker for page, group, and filesystem values used by
 * DataTables Editor fields and standalone inputs.
 */
export class WebjetDteJsTreeElement extends HTMLElement {
    constructor() {
        super();
        this._value = [];
        this._options = {};
        this._tree = null;
        this._modal = null;
        this._activeIndex = null;
        this._treeSelectHandler = event => this._processTreeItem(event.detail);
    }

    /**
     * Renders the connected element and emits a bubbling, non-cancelable
     * `webjet-component-ready` event without detail.
     */
    connectedCallback() {
        this.render();
        this.dataset.ready = "true";
        this.dispatchEvent(new CustomEvent("webjet-component-ready", { bubbles: true }));
    }

    disconnectedCallback() {
        this._destroyTree();
    }

    /**
     * Applies component options and renders immediately when the element is connected.
     *
     * @param {WebjetDteJsTreeOptions} options - Component options.
     * @returns {WebjetDteJsTreeElement} The configured element.
     */
    configure(options) {
        this._options = { ...options };
        this._value = normalizeValue(options.value, options.mode || "");
        if (this.isConnected) this.render();
        return this;
    }

    get value() {
        return this._value;
    }

    set value(value) {
        this.setValue(value);
    }

    /**
     * Replaces the selected value and renders immediately when the element is connected.
     *
     * @param {Object|Object[]|null|string} [value] - One item, an item array, or an empty-value sentinel.
     */
    setValue(value) {
        this._value = normalizeValue(value, this._options.mode || "");
        if (this.isConnected) this.render();
    }

    /**
     * Returns the current selection, excluding invalid placeholders in nullable ID modes.
     *
     * @returns {(Object|string)[]} The internal selection array, or an empty array when a nullable ID is not valid.
     */
    getValue() {
        if (this._isNullableIdMode() && !this._hasValidNullableId()) return [];
        return this._value;
    }

    /**
     * Rebuilds the picker rows and optional add control from the current selection.
     */
    render() {
        this._destroyTree();
        this.replaceChildren();

        const section = document.createElement("section");
        const container = document.createElement("div");
        const mode = this._options.mode || "";
        container.className = `dt-tree-container ${mode.includes("dt-tree-group-array-scheduler") ? "dt-tree-group-array-scheduler" : mode.includes("dt-tree-page") ? "dt-tree-page" : "dt-tree-other"}`;

        this._value.forEach((item, index) => container.appendChild(this._renderItem(item, index)));
        section.appendChild(container);

        if (mode.startsWith("dt-tree-group-array") || mode.startsWith("dt-tree-page-array") || mode.startsWith("dt-tree-dir-array") || mode.startsWith("dt-tree-universal-array")) {
            const row = document.createElement("div");
            row.className = `form-group row${this._value.length < 1 ? " dt-tree-container-no-margin-top" : ""}`;
            const col = document.createElement("div");
            col.className = "col-12";
            const add = button("btn-webjet-jstree-add", "ti-plus", this._options.attributes?.["data-dt-json-addbutton"] || WJ.translate("button.add"));
            add.innerHTML += ` <span>${this._options.attributes?.["data-dt-json-addbutton"] || WJ.translate("button.add")}</span>`;
            add.addEventListener("mouseup", () => this._openModal(null));
            col.appendChild(add);
            row.appendChild(col);
            section.appendChild(row);
        }

        this.appendChild(section);

        if (mode.includes("dt-tree-dir-simple")) {
            setTimeout(() => this.querySelector(".input-group input")?.removeAttribute("disabled"), 100);
        }
    }

    _renderItem(item, index) {
        const group = document.createElement("div");
        group.className = "form-group";
        const inputGroup = document.createElement("div");
        inputGroup.className = "input-group";
        const input = document.createElement("input");
        input.type = "text";
        input.className = "form-control";
        input.value = getDisplayPath(item, this._options.mode || "");
        input.disabled = true;
        input.setAttribute("aria-label", this._findLabel());
        inputGroup.appendChild(input);

        const edit = button("btn-webjet-jstree-item-edit", "ti-focus-2", WJ.translate("button.select"));
        edit.addEventListener("click", () => this._openModal(index));
        inputGroup.appendChild(edit);

        if (this._isRemovable()) {
            const remove = button("btn-webjet-jstree-item-remove", "ti-trash", WJ.translate("button.delete"));
            remove.addEventListener("click", () => this._removeItem(index));
            inputGroup.appendChild(remove);
        }
        group.appendChild(inputGroup);
        return group;
    }

    _findLabel() {
        const id = this._options.inputElement?.id;
        const label = id ? document.querySelector(`label[for="${CSS.escape(id)}"]`) : null;
        return label?.textContent?.trim() || this.parentElement?.getAttribute("aria-label") || this.parentElement?.getAttribute("title") || "";
    }

    _isRemovable() {
        const mode = this._options.mode || "";
        return mode.startsWith("dt-tree-group-array") || mode.startsWith("dt-tree-group-null") || mode.startsWith("dt-tree-groupid-null") ||
            mode.startsWith("dt-tree-page-array") || mode.startsWith("dt-tree-page-null") || mode.startsWith("dt-tree-pageid-null") ||
            mode.startsWith("dt-tree-dir-array") || mode.startsWith("dt-tree-universal-array");
    }

    _isNullableIdMode() {
        const mode = this._options.mode || "";
        return mode === "dt-tree-groupid-null" || mode === "dt-tree-pageid-null";
    }

    _hasValidNullableId() {
        const mode = this._options.mode || "";
        const id = mode === "dt-tree-pageid-null" ? this._value[0]?.docId : this._value[0]?.groupId;
        return Number(id) > 0;
    }

    /**
     * Removes an array item or resets a nullable single-value item.
     *
     * Page modes first dispatch a non-bubbling, non-cancelable
     * `WJ.jstree.item.remove` event on `window` with the affected item as its detail.
     * All modes then emit a bubbling, non-cancelable `webjet-jstree-remove` event
     * whose detail contains `item` and its document, group, or generic `id`.
     *
     * @param {number} index - Index of the item to remove or reset.
     */
    _removeItem(index) {
        const mode = this._options.mode || "";
        const item = this._value[index];
        if (mode.startsWith("dt-tree-page")) WJ.dispatchEvent("WJ.jstree.item.remove", item);

        if (mode === "dt-tree-group-null") Object.assign(item, { groupId: -1, fullPath: "", virtualPath: "" });
        else if (mode === "dt-tree-groupid-null") Object.assign(item, { groupId: -1, id: "", virtualPath: "", fullPath: "" });
        else if (mode === "dt-tree-page-null" || mode === "dt-tree-pageid-null") Object.assign(item, { id: -1, docId: -1, fullPath: "", virtualPath: "" });
        else this._value.splice(index, 1);

        this._syncInput(undefined, false);
        this.dispatchEvent(new CustomEvent("webjet-jstree-remove", { bubbles: true, detail: { item, id: item?.docId ?? item?.groupId ?? item?.id } }));
        this.render();
    }

    /**
     * Opens the tree selector to add or replace an item.
     *
     * @param {number|null} index - Existing item index to replace, or `null` to append a new item.
     */
    _openModal(index) {
        this._destroyTree();
        this._activeIndex = index;
        const mode = this._options.mode || "";
        const attrs = this._options.attributes || {};
        const modal = document.createElement("section");
        modal.className = "custom-modal open-custom-modal";
        const wrapper = document.createElement("div");
        wrapper.className = "custom-modal-wrapper";
        const close = button("close-custom-modal", "ti-x", WJ.translate("datatables.modal.close.js"));
        close.addEventListener("click", () => this._closeModal());
        const treeWrapper = document.createElement("div");
        treeWrapper.className = "jsTree-wrapper";
        const treeElement = document.createElement("div");
        treeElement.id = `webjetJsTree-${++treeCounter}`;
        treeElement.dataset.restParamName = "id";
        treeElement.dataset.restRoot = attrs["data-dt-field-root"] || "-1";

        let endpoint = mode.startsWith("dt-tree-dir") ? "/admin/rest/elfinder/tree" : "/admin/rest/groups/tree";
        if (attrs["data-dt-field-dt-url"]) endpoint = attrs["data-dt-field-dt-url"];
        const params = new URLSearchParams({ click: mode });
        if (attrs["data-dt-field-skipFolders"]) params.set("skipFolders", attrs["data-dt-field-skipFolders"]);
        if (attrs["data-dt-field-hideRootParents"]) params.set("hideRootParents", attrs["data-dt-field-hideRootParents"]);
        if (attrs["data-dt-field-root"]) params.set("rootFolder", attrs["data-dt-field-root"]);
        if (attrs["data-dt-field-writableOnly"]) params.set("writableOnly", attrs["data-dt-field-writableOnly"]);
        treeElement.dataset.restUrl = `${endpoint}?${params.toString()}`;
        treeElement.addEventListener("webjet-jstree-select", this._treeSelectHandler);

        treeWrapper.appendChild(treeElement);
        wrapper.append(close, treeWrapper);
        modal.appendChild(wrapper);
        this.querySelector("section")?.appendChild(modal);
        this._modal = modal;
        this._tree = new window.WebjetJsTree(treeElement.id);
        $(close).tooltip();
    }

    _closeModal() {
        this._destroyTree();
        this._modal?.remove();
        this._modal = null;
        this._activeIndex = null;
    }

    _destroyTree() {
        this._tree?.destroy();
        this._tree = null;
    }

    /**
     * Validates and maps a jsTree selection, then updates the component value and backing input.
     * A configured JSON-field `getItem` hook maps the selection before storage.
     *
     * @param {Object} data - Selection detail from the `webjet-jstree-select` event.
     */
    _processTreeItem(data) {
        const mode = this._options.mode || "";
        if (!this._validateSelection(data)) return;
        if (mode.includes("-array") && !this._validateDuplicateSelection(data)) return;

        let item;
        const dataTable = this._options.dataTable;
        const context = this._getCompatibilityContext();
        if (typeof dataTable?.DATA?.jsonField?.getItem === "function") item = dataTable.DATA.jsonField.getItem(context, data);
        else if (mode.startsWith("dt-tree-group")) item = data.node.original.groupDetails;
        else if (mode.startsWith("dt-tree-page")) {
            item = data.node.original.docDetails;
            if (mode.includes("alldomains")) item.fullPath = data.node.original.virtualPath;
        } else item = data.node.original;

        if (mode.includes("-array")) {
            if (this._activeIndex != null) this._value.splice(this._activeIndex, 1, item);
            else this._value.push(item);
        } else {
            this._value.splice(0, this._value.length, item);
        }

        // jsTree continues processing select_node/activate_node after the custom
        // selection event returns. Destroying it here would null its element
        // before that processing finishes.
        setTimeout(() => {
            this._syncInput(item);
            this._closeModal();
            if (this.isConnected) this.render();
        }, 0);
    }

    _validateSelection(data) {
        const mode = this._options.mode || "";
        const icon = data.node.icon || "";
        if (icon.includes("ti ti-folder-x") || (mode.includes("alldomains") && icon.includes("ti ti-home"))) {
            data.instance.open_node(data.node.id);
            return false;
        }
        if (mode.startsWith("dt-tree-group") && !data.node.original.groupDetails) {
            WJ.notifyError("Vyberte adresár", null, 5000);
            return false;
        }
        if (mode.startsWith("dt-tree-page") && !data.node.original.docDetails) {
            WJ.notifyError("Vyberte web stránku", null, 5000);
            return false;
        }
        return true;
    }

    _validateDuplicateSelection(data) {
        const mode = this._options.mode || "";
        let candidate;
        if (mode.startsWith("dt-tree-group")) candidate = data.node.original.groupDetails;
        else if (mode.startsWith("dt-tree-page")) candidate = data.node.original.docDetails;
        else candidate = data.node;
        const getKey = value => {
            if (typeof this._options.dataTable?.DATA?.jsonField?.getKey === "function") {
                return this._options.dataTable.DATA.jsonField.getKey(this._getCompatibilityContext(), value);
            }
            if (mode.startsWith("dt-tree-group")) return value.groupId;
            if (mode.startsWith("dt-tree-page")) return value.docId;
            return value.id;
        };
        const key = getKey(candidate);
        if (this._value.some((value, index) => index !== this._activeIndex && getKey(value) == key)) {
            WJ.notifyError("Zvolená položka sa už v zozname nachádza", null, 5000);
            return false;
        }
        return true;
    }

    /**
     * Builds the context passed to custom DataTable JSON-field mapping hooks.
     *
     * @returns {WebjetDteJsTreeCompatibilityContext} The current compatibility context.
     */
    _getCompatibilityContext() {
        return {
            dataTable: this._options.dataTable,
            dataTableName: this._options.dataTableName,
            idKey: this._options.inputElement?.id,
            data: this._value,
            click: this._options.mode || "",
            attr: this._options.attributes,
            index: this._activeIndex,
            grp: this._activeIndex == null ? null : this._value[this._activeIndex]
        };
    }

    _getTextInputSelector() {
        const inputId = this._options.inputElement?.id;
        const dataTableName = this._options.dataTableName;
        return dataTableName != null ? `#${dataTableName}_modal #${inputId}` : ` #${inputId}`;
    }

    /**
     * Writes the current value to the backing control and publishes selection changes.
     *
     * When configured, the backing control receives a jQuery `change` event.
     * Selection changes dispatch a non-bubbling, non-cancelable
     * `WJ.jstree.change` event on `window`. Depending on the mode, it is preceded by
     * `WJ.jstree-simple.change` with `{ textInputId, value }` or
     * `WJ.jstree-groupid.change` with `{ textInputId, item }`. For compatibility,
     * `textInputId` in `WJ.jstree-groupid.change` contains the selected ID; in the
     * other events it contains the backing input selector.
     *
     * @param {Object} [item] - Selected item included in global event detail.
     * @param {boolean} [emitSelectionEvents=true] - Whether to dispatch the global selection events.
     */
    _syncInput(item, emitSelectionEvents = true) {
        const input = this._options.inputElement;
        if (!input) return;
        const mode = this._options.mode || "";
        const textInputId = this._getTextInputSelector();
        if (mode.includes("dt-tree-dir-simple")) {
            input.value = this._value[0]?.virtualPath || "";
            $(input).trigger("change");
            if (emitSelectionEvents) WJ.dispatchEvent("WJ.jstree-simple.change", { textInputId, value: input.value });
        } else if (mode.includes("dt-tree-groupid") || mode.includes("dt-tree-pageid")) {
            let id;
            if (this._isNullableIdMode() && !this._hasValidNullableId()) {
                input.dataset.text = "";
                input.value = "";
            } else {
                id = mode.includes("dt-tree-pageid") ? this._value[0]?.docId : this._value[0]?.groupId;
                let text = this._value[0]?.fullPath || "";
                if (id < 1) {
                    id = -1;
                    text = input.dataset.textEmpty || text;
                    if (this._value[0]) this._value[0].fullPath = text;
                }
                input.dataset.text = text;
                input.value = id;
            }
            $(input).trigger("change");
            if (emitSelectionEvents) WJ.dispatchEvent("WJ.jstree-groupid.change", { textInputId: id, item });
        } else {
            input.value = JSON.stringify(this._value, undefined, 4);
            $(input).trigger("change");
        }
        if (emitSelectionEvents) WJ.dispatchEvent("WJ.jstree.change", { textInputId, item });
    }
}

if (!customElements.get("webjet-dte-jstree")) customElements.define("webjet-dte-jstree", WebjetDteJsTreeElement);

/**
 * Creates and configures a tree-picker custom element.
 *
 * @param {WebjetDteJsTreeOptions} options - Component options.
 * @returns {WebjetDteJsTreeElement} The configured custom element.
 */
export function createWebjetDteJsTree(options) {
    const component = document.createElement("webjet-dte-jstree");
    component.configure(options);
    return component;
}
