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
    let generation = 0;
    let ready = false;

    const reset = () => {
        generation++;
        ready = false;
        request?.abort();
        request = null;
    };

    editor.on("initRemove.deleteMirrors close.deleteMirrors", reset);
    editor.on("preSubmit.deleteMirrors", (event, data, action) => {
        if (action === "remove" && ready === false) return false;
    });

    editor.on("open.deleteMirrors", (event, mode, action) => {
        if (action !== "remove") return;
        reset();
        const currentGeneration = generation;
        const modal = $("#" + datatable.DATA.id + "_modal");
        const submitButtons = modal.find(".DTE_Form_Buttons button:not(.btn-close-editor)");
        submitButtons.prop("disabled", true);

        modal.find(".delete-mirrors-info").remove();
        const output = $("<div>", {
            class: "delete-mirrors-info mt-3",
            role: "status",
            "aria-live": "polite"
        }).text(WJ.translate("editor.delete_mirrors.loading.js"));
        modal.find(".DTE_Form_Info .col-sm-7.offset-sm-4").append(output);

        const showError = () => {
            output.addClass("alert alert-danger mb-0").text(WJ.translate("editor.delete_mirrors.error.js"));
        };

        request = $.ajax({
            url: url,
            data: { ids: editor.ids().join(",") },
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
            ready = true;
            submitButtons.prop("disabled", false);
        }).fail((xhr, status) => {
            if (generation !== currentGeneration || status === "abort") return;
            showError();
        });
    });
}
