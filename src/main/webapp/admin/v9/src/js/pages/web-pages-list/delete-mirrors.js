/** Loads the optional cleanup preview without blocking deletion of the pages themselves. */
function showPageFiles(output, id, url, ids, defaultChecked, setReady) {
    const choice = $("<div>", { class: "delete-page-files form-check mt-3" });
    const input = $("<input>", { type: "checkbox", id, class: "form-check-input", checked: defaultChecked, "aria-describedby": id + "_help" });
    const preview = $("<div>", { class: "form-text text-break", role: "status" }).text(WJ.translate("editor.delete_files.loading.js"));
    choice.append(input,
        $("<label>", { for: id, class: "form-check-label" }).text(WJ.translate("editor.delete_files.choice.js")),
        $("<div>", { id: id + "_help", class: "form-text" }).text(WJ.translate("editor.delete_files.restore.js")),
        preview);
    output.after(choice);

    const waitForCheckedPreview = () => setReady(!input.prop("checked"));
    input.on("change", waitForCheckedPreview);
    waitForCheckedPreview();
    const showError = () => {
        input.prop({ checked: false, disabled: true });
        preview.text(WJ.translate("editor.delete_files.error.js"));
    };
    return $.ajax({ url, method: "POST", data: { ids: ids.join(",") }, dataType: "json" }).done(pages => {
        if (!Array.isArray(pages)) {
            showError();
            return;
        }
        preview.empty();
        const empty = WJ.translate("editor.delete_files.empty.js");
        if (pages.length === 0) preview.text(empty);
        for (const page of pages) {
            const group = $("<div>", { class: "mt-2" });
            if (ids.length > 1) group.append($("<strong>").text(page.fullPath));
            if (page.files.length > 0) {
                const list = $("<ul>", { class: "mb-0" });
                for (const path of page.files) list.append($("<li>").text(path));
                group.append(list);
            } else {
                group.append($("<div>").text(empty));
            }
            preview.append(group);
        }
        if (!pages.some(page => page.files.length > 0)) input.prop({ checked: false, disabled: true });
    }).fail((xhr, status) => {
        if (status !== "abort") showError();
    }).always(() => {
        input.off("change", waitForCheckedPreview);
        setReady(true);
    });
}

/**
 * Adds a direct-mirror preview to a page or folder delete confirmation.
 * Submission remains blocked until the preview has loaded successfully.
 *
 * @param {object} datatable WebJET DataTable whose Editor opens the confirmation.
 * @param {string} url Read-only endpoint accepting the selected IDs.
 * @param {string} itemType "pages" or "groups", used to select the warning translation.
 */
export function bindDeleteMirrors(datatable, url, itemType) {
    const editor = datatable.EDITOR;
    let request = null;
    let fileRequest = null;
    let generation = 0;
    let ready = false;

    const reset = () => {
        generation++;
        ready = false;
        request?.abort();
        request = null;
        fileRequest?.abort();
        fileRequest = null;
    };

    editor.on("initRemove.deleteMirrors close.deleteMirrors", reset);
    editor.on("preSubmit.deleteMirrors", (event, data, action) => {
        if (action !== "remove") return;
        if (!ready) return false;
        if (itemType === "pages" && $("#" + datatable.DATA.id + "_modal .delete-page-files input").is(":enabled:checked")) {
            for (const [id, row] of Object.entries(data.data)) {
                data.data[id] = { ...row, editorFields: { ...row.editorFields, deleteUnusedPageFiles: true } };
            }
        }
    });

    editor.on("open.deleteMirrors", (event, mode, action) => {
        if (action !== "remove") return;
        reset();
        const currentGeneration = generation;
        const modal = $("#" + datatable.DATA.id + "_modal");
        const submitButtons = modal.find(".DTE_Form_Buttons button:not(.btn-close-editor)");
        const setReady = value => {
            if (generation !== currentGeneration) return;
            ready = value;
            submitButtons.prop("disabled", !value);
        };
        setReady(false);

        modal.find(".delete-mirrors-info, .delete-page-files").remove();
        const output = $("<div>", {
            class: "delete-mirrors-info mt-3",
            role: "status",
            "aria-live": "polite"
        }).text(WJ.translate("editor.delete_mirrors.loading.js"));
        modal.find(".DTE_Form_Info .col-sm-7.offset-sm-4").append(output);

        const showError = () => {
            output.addClass("alert alert-danger mb-0").text(WJ.translate("editor.delete_mirrors.error.js"));
        };

        const ids = editor.ids();
        request = $.ajax({
            url: url,
            method: "POST",
            data: { ids: ids.join(",") },
            dataType: "json",
            cache: false,
            timeout: 30000
        }).done(result => {
            if (generation !== currentGeneration) return;
            // DataTable endpoints can return validation errors with HTTP 200.
            if (result == null || !Array.isArray(result.items) || typeof result.hiddenMirrors !== "boolean") {
                showError();
                return;
            }

            if (itemType === "pages" && result.pageFileCleanupAvailable === true) {
                fileRequest = showPageFiles(output, datatable.DATA.id + "_deletePageFiles", url + "/files", ids,
                    result.pageFileCleanupDefaultChecked === true, setReady);
            } else setReady(true);
            output.empty();
            if (result.items.length > 0 || result.hiddenMirrors) {
                output.addClass("alert alert-warning mb-0");
                if (result.items.length > 0) {
                    output.append($("<p>", { class: "mb-2" })
                        .text(WJ.translate("editor.delete_mirrors." + itemType + ".js")));
                    const list = $("<ul>", { class: "mb-0" });
                    result.items.forEach(item => {
                        const domain = item.domainName ? " (" + item.domainName + ")" : "";
                        list.append($("<li>", { class: "text-break" }).text(item.fullPath + domain));
                    });
                    output.append(list);
                }
                if (result.hiddenMirrors) {
                    output.append($("<p>", { class: "mb-0" })
                        .toggleClass("mt-2", result.items.length > 0)
                        .text(WJ.translate("editor.delete_mirrors.hidden.js")));
                }
            } else {
                output.remove();
            }
        }).fail((xhr, status) => {
            if (generation !== currentGeneration || status === "abort") return;
            showError();
        });
    });
}
