import WJ from "../../src/js/webjet";

/**
 * Base factory for OPTIONS-style field types (drag-reorderable list of inputs joined by "|").
 *
 * @param {object} config
 * @param {string} config.prefix - CSS class prefix (e.g. "options" or "basic-options")
 * @param {function} config.createRowHtml - (conf) => jQuery row element with inputs
 * @param {function} config.clearRow - (row$) => clears the input values in a row
 * @param {function} config.getRowValue - (row$) => string|null value from one row (null = skip)
 * @param {function} config.parseValue - (part: string) => array of args to pass to createRowHtml
 * @param {function} [config.createHeader] - (conf) => jQuery header element (optional)
 * @param {function} config.emptyRowArgs - () => array of args for an empty row
 * @returns field type definition
 */
export function createOptionsFieldType(config) {
    const { prefix, createRowHtml, clearRow, getRowValue, parseValue, createHeader, emptyRowArgs } = config;

    const cls = {
        wrapper: `${prefix}-field-wrapper`,
        inputs: `${prefix}-inputs`,
        row: `${prefix}-input-row`,
        dragging: `${prefix}-dragging`,
        dragHandle: `${prefix}-drag-handle`,
        removeBtn: `${prefix}-remove-btn`,
        addBtn: `${prefix}-add-btn`
    };

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
                    container.insertBefore(dragging, afterElement);
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

            conf._wrapper = $(`
                <div id="${id}" class="${cls.wrapper}">
                    ${headerHtml}
                    <div class="${cls.inputs}"></div>
                    <button class="btn btn-outline-secondary mt-2 ${cls.addBtn}" type="button">
                        <i class="ti ti-plus"></i> ${WJ.translate("button.add")}
                    </button>
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
            if (val && val.length > 0) {
                let parts;
                if (val.indexOf("|") != -1) parts = val.split("|");
                else parts = val.split(",");
                for (let i = 0; i < parts.length; i++) {
                    addInputRow(conf, ...parseValue(parts[i]));
                }
            } else {
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
