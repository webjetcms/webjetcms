let treeCounter = 0;

function createEmptyValue(mode) {
    const item = { fullPath: "" };
    if (mode.includes("dt-tree-page")) item.docId = -1;
    else if (mode.includes("dt-tree-group")) item.groupId = -1;
    else item.id = -1;
    return [item];
}

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

    connectedCallback() {
        this.render();
        this.dataset.ready = "true";
        this.dispatchEvent(new CustomEvent("webjet-component-ready", { bubbles: true }));
    }

    disconnectedCallback() {
        this._destroyTree();
    }

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

    setValue(value) {
        this._value = normalizeValue(value, this._options.mode || "");
        if (this.isConnected) this.render();
    }

    getValue() {
        return this._value;
    }

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

    _removeItem(index) {
        const mode = this._options.mode || "";
        const item = this._value[index];
        if (mode.startsWith("dt-tree-page")) WJ.dispatchEvent("WJ.jstree.item.remove", item);

        if (mode === "dt-tree-group-null") Object.assign(item, { groupId: -1, fullPath: "", virtualPath: "" });
        else if (mode === "dt-tree-groupid-null") Object.assign(item, { groupId: -1, id: "", virtualPath: "", fullPath: "" });
        else if (mode === "dt-tree-page-null" || mode === "dt-tree-pageid-null") Object.assign(item, { id: -1, docId: -1, fullPath: "", virtualPath: "" });
        else this._value.splice(index, 1);

        this._syncInput();
        this.dispatchEvent(new CustomEvent("webjet-jstree-remove", { bubbles: true, detail: { item, id: item?.docId ?? item?.groupId ?? item?.id } }));
        this.render();
    }

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

    _syncInput(item) {
        const input = this._options.inputElement;
        if (!input) return;
        const mode = this._options.mode || "";
        if (mode.includes("dt-tree-dir-simple")) {
            input.value = this._value[0]?.virtualPath || "";
            $(input).trigger("change");
            WJ.dispatchEvent("WJ.jstree-simple.change", { textInputId: `#${input.id}`, value: input.value });
        } else if (mode.includes("dt-tree-groupid") || mode.includes("dt-tree-pageid")) {
            let id = mode.includes("dt-tree-pageid") ? this._value[0]?.docId : this._value[0]?.groupId;
            let text = this._value[0]?.fullPath || "";
            if (id < 1) {
                id = -1;
                text = input.dataset.textEmpty || text;
            }
            input.dataset.text = text;
            input.value = id;
            $(input).trigger("change");
            WJ.dispatchEvent("WJ.jstree-groupid.change", { textInputId: id, item });
        } else {
            input.value = JSON.stringify(this._value, undefined, 4);
            $(input).trigger("change");
        }
        WJ.dispatchEvent("WJ.jstree.change", { textInputId: `#${input.id}`, item });
    }
}

if (!customElements.get("webjet-dte-jstree")) customElements.define("webjet-dte-jstree", WebjetDteJsTreeElement);

export function createWebjetDteJsTree(options) {
    const component = document.createElement("webjet-dte-jstree");
    component.configure(options);
    return component;
}
