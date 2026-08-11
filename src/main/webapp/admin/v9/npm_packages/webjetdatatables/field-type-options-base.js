import WJ from "../../src/js/webjet";

/**
 * Base factory for OPTIONS-style field types (drag-reorderable list of inputs joined by "|").
 *
 * @param {object} config
 * @param {string} config.prefix - CSS class prefix (e.g. "options" or "options-simple")
 * @param {function} config.createRowHtml - (conf) => jQuery row element with inputs
 * @param {function} config.clearRow - (row$) => clears the input values in a row
 * @param {function} config.getRowValue - (row$) => string|null value from one row (null = skip)
 * @param {function} config.parseValue - (part: string) => array of args to pass to createRowHtml
 * @param {function} [config.createHeader] - (conf) => HTML string for header markup (optional)
 * @param {function} config.emptyRowArgs - () => array of args for an empty row
 * @param {boolean} [config.allowEmptyOption] - enable the allowEmptyOption class name behavior
 * @returns field type definition
 */
export function createOptionsFieldType(config) {
    const { prefix, createRowHtml, clearRow, getRowValue, parseValue, createHeader, emptyRowArgs, allowEmptyOption } = config;

    const cls = {
        wrapper: `${prefix}-field-wrapper`,
        inputs: `${prefix}-inputs`,
        row: `${prefix}-input-row`,
        dragging: `${prefix}-dragging`,
        dragHandle: `${prefix}-drag-handle`,
        removeBtn: `${prefix}-remove-btn`,
        addBtn: `${prefix}-add-btn`,
        emptyOption: `${prefix}-empty-option`,
        emptyOptionBtn: `${prefix}-empty-option-btn`
    };

    function isEmptyOptionAllowed(conf) {
        if (allowEmptyOption !== true || typeof conf.className !== "string") return false;
        return conf.className.split(/\s+/).includes("allowEmptyOption");
    }

    function isEmptyOptionSelected(conf) {
        return conf._wrapper.find("." + cls.emptyOptionBtn).prop("checked") === true;
    }

    function setEmptyOptionSelected(conf, selected) {
        conf._wrapper.find("." + cls.emptyOptionBtn).prop("checked", selected);
    }

    function initDragReorder(conf) {
        const container = conf._wrapper.find("." + cls.inputs)[0];

        container.addEventListener("dragover", function (e) {
            e.preventDefault();
            const afterElement = getDragAfterElement(container, e.clientY);
            const dragging = container.querySelector("." + cls.dragging);
            if (dragging) {
                if (afterElement == null) {
                    container.appendChild(dragging);
                } else {
                    afterElement.before(dragging);
                }
            }
        });
    }

    function getDragAfterElement(container, y) {
        const draggableElements = [...container.querySelectorAll('.' + cls.row + ':not(.' + cls.dragging + ')')];
        return draggableElements.reduce((closest, child) => {
            const box = child.getBoundingClientRect();
            const offset = y - box.top - box.height / 2;
            if (offset < 0 && offset > closest.offset) {
                return { offset: offset, element: child };
            } else {
                return closest;
            }
        }, { offset: Number.NEGATIVE_INFINITY }).element;
    }

    function addInputRow(conf, ...args) {
        const row = createRowHtml(conf, ...args);

        row.find("." + cls.removeBtn).on("click", function () {
            //keep at least one row
            if (conf._wrapper.find("." + cls.row).length > 1) {
                row.remove();
            } else {
                clearRow(row);
            }
        });

        //drag and drop reordering
        const rowEl = row[0];
        rowEl.setAttribute("draggable", "false");
        row.find("." + cls.dragHandle).on("mousedown", function () {
            rowEl.setAttribute("draggable", "true");
        });
        row.find("." + cls.dragHandle).on("mouseup", function () {
            rowEl.setAttribute("draggable", "false");
        });
        rowEl.addEventListener("dragstart", function () {
            rowEl.classList.add(cls.dragging);
            rowEl.style.opacity = "0.4";
        });
        rowEl.addEventListener("dragend", function () {
            rowEl.classList.remove(cls.dragging);
            rowEl.style.opacity = "";
            rowEl.setAttribute("draggable", "false");
        });

        conf._wrapper.find("." + cls.inputs).append(row);
    }

    return {
        create: function (conf) {
            const id = $.fn.dataTable.Editor.safeId(conf.id);
            conf._id = id;

            const headerHtml = createHeader ? createHeader(conf) : "";
            const emptyOptionHtml = isEmptyOptionAllowed(conf) ? `
                    <div class="form-check align-self-center ${cls.emptyOption}">
                        <input class="form-check-input ${cls.emptyOptionBtn}" type="checkbox" id="${id}-empty-option">
                        <label class="form-check-label" for="${id}-empty-option">${WJ.translate("datatables.options.addEmptyOption.js")}</label>
                    </div>` : "";

            conf._wrapper = $(`
                <div id="${id}" class="${cls.wrapper}">
                    ${headerHtml}
                    <div class="${cls.inputs}"></div>
                    <div class="d-flex flex-wrap gap-2 mt-2">
                        <button class="btn btn-outline-secondary ${cls.addBtn}" type="button">
                            <i class="ti ti-plus"></i> ${WJ.translate("button.add")}
                        </button>
                        ${emptyOptionHtml}
                    </div>
                </div>
            `);

            conf._wrapper.find("." + cls.addBtn).on("click", function () {
                addInputRow(conf, ...emptyRowArgs());
            });

            //enable drag and drop reordering
            initDragReorder(conf);

            //start with one empty row
            addInputRow(conf, ...emptyRowArgs());

            return conf._wrapper[0];
        },

        get: function (conf) {
            const values = [];
            if (isEmptyOptionAllowed(conf) && isEmptyOptionSelected(conf)) {
                values.push(":");
            }
            conf._wrapper.find("." + cls.row).each(function () {
                const val = getRowValue($(this));
                if (val !== null) {
                    values.push(val);
                }
            });
            return values.join("|");
        },

        set: function (conf, val) {
            conf._wrapper.find("." + cls.inputs).empty();
            setEmptyOptionSelected(conf, false);
            let rowAdded = false;
            if (val && val.length > 0) {
                let parts;

                if (val.indexOf("|") == -1) parts = val.split(",");
                else parts = val.split("|");

                for (let i = 0; i < parts.length; i++) {
                    if (isEmptyOptionAllowed(conf) && parts[i].trim() === ":") {
                        setEmptyOptionSelected(conf, true);
                        continue;
                    }
                    addInputRow(conf, ...parseValue(parts[i]));
                    rowAdded = true;
                }
            }
            if (rowAdded === false) {
                addInputRow(conf, ...emptyRowArgs());
            }
        },

        enable: function (conf) {
            conf._wrapper.find("input, button").prop("disabled", false);
        },

        disable: function (conf) {
            conf._wrapper.find("input, button").prop("disabled", true);
        },

        canReturnSubmit: function (conf, node) {
            return false;
        }
    };
}
