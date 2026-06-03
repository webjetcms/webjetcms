import WJ from "../../src/js/webjet";

/**
 * Custom field type for DataTables Editor - dynamic list of string inputs joined by "|"
 * @returns field type definition
 */
export function typeOptions() {

    let dragSrcRow = null;

    function initDragReorder(conf) {
        const container = conf._wrapper.find(".options-inputs")[0];

        container.addEventListener("dragover", function (e) {
            e.preventDefault();
            const afterElement = getDragAfterElement(container, e.clientY);
            const dragging = container.querySelector(".options-dragging");
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
        const draggableElements = [...container.querySelectorAll('.options-input-row:not(.options-dragging)')];
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

    function addInputRow(conf, value1, value2) {
        const row = $(`
            <div class="input-group mb-2 options-input-row">
                <span class="input-group-text options-drag-handle" style="cursor: grab;"><i class="ti ti-caret-up-down"></i></span>
                <input type="text" class="form-control options-value-1" value="" />
                <input type="text" class="form-control options-value-2" value="" />
                <button class="btn btn-outline-danger options-remove-btn" type="button" title="${WJ.translate("button.delete")}">
                    <i class="ti ti-trash"></i>
                </button>
            </div>
        `);
        row.find(".options-value-1").val(value1 || "");
        row.find(".options-value-2").val(value2 || "");
        row.find(".options-remove-btn").on("click", function () {
            //keep at least one row
            if (conf._wrapper.find(".options-input-row").length > 1) {
                row.remove();
            } else {
                row.find(".options-value-1").val("");
                row.find(".options-value-2").val("");
            }
        });

        //drag and drop reordering
        const rowEl = row[0];
        rowEl.setAttribute("draggable", "false");
        row.find(".options-drag-handle").on("mousedown", function () {
            rowEl.setAttribute("draggable", "true");
        });
        row.find(".options-drag-handle").on("mouseup", function () {
            rowEl.setAttribute("draggable", "false");
        });
        rowEl.addEventListener("dragstart", function () {
            rowEl.classList.add("options-dragging");
            rowEl.style.opacity = "0.4";
        });
        rowEl.addEventListener("dragend", function () {
            rowEl.classList.remove("options-dragging");
            rowEl.style.opacity = "";
            rowEl.setAttribute("draggable", "false");
        });

        conf._wrapper.find(".options-inputs").append(row);
    }

    return {
        create: function (conf) {
            const id = $.fn.dataTable.Editor.safeId(conf.id);
            conf._id = id;

            conf._wrapper = $(`
                <div id="${id}" class="options-field-wrapper">
                    <div class="input-group mb-2 options-input-row-header">
                        <span class="options-label">${WJ.translate("editor.form.sl.name")}</span>
                        <span class="options-value">${WJ.translate("editor.form.sl.value")}</span>
                    </div>
                    <div class="options-inputs"></div>
                    <button class="btn btn-outline-secondary mt-2 options-add-btn" type="button">
                        <i class="ti ti-plus"></i> ${WJ.translate("button.add")}
                    </button>
                </div>
            `);

            conf._wrapper.find(".options-add-btn").on("click", function () {
                addInputRow(conf, "", "");
            });

            //enable drag and drop reordering
            initDragReorder(conf);

            //start with one empty row
            addInputRow(conf, "", "");

            return conf._wrapper[0];
        },

      get: function (conf) {
            const values = [];
            conf._wrapper.find(".options-input-row").each(function () {
                const raw1 = $(this).find(".options-value-1").val();
                const raw2 = $(this).find(".options-value-2").val();

                const val1 = (raw1 == null ? "" : String(raw1)).trim();
                let val2 = (raw2 == null ? "" : String(raw2)).trim();

                if (val1.length > 0 || val2.length > 0) {

                    if (val1.length === 0) {
                        // if only value is set, treat it as both label and value
                        values.push(val2 + ":" + val2);
                    } else {
                        // if value is omitted, default it to label
                        if (val2.length === 0) val2 = val1;
                        values.push(val1 + ":" + val2);
                    }
                }
            });
            return values.join("|");
        },

        set: function (conf, val) {
            conf._wrapper.find(".options-inputs").empty();
            if (val && val.length > 0) {
                let parts;
                if (val.indexOf("|") != -1) parts = val.split("|");
                else parts = val.split(",");
                for (let i = 0; i < parts.length; i++) {
                    const pair = parts[i].split(":");
                    if (pair.length === 1) pair.push(pair[0]); // if only one part, treat it as both label and value
                    addInputRow(conf, pair[0] || "", pair[1] || "");
                }
            } else {
                addInputRow(conf, "", "");
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
