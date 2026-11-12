window.domReady.add(function () {
    $.fn.dataTable.Editor.fieldTypes.assistantTools = {
        create(conf) {
            const view = document.querySelector("#assistant-tools-template").content.firstElementChild.cloneNode(true);
            const picker = view.querySelector("select");
            const add = view.querySelector("button");
            const table = view.querySelector("table");
            const removeTemplate = view.querySelector("template").content.firstElementChild;
            picker.id = conf.id;
            conf._input = $(picker);
            conf.assigned = new Set();
            conf.catalog = new Map();

            conf.render = () => {
                picker.replaceChildren();
                for (const name of conf.catalog.keys()) {
                    if (!conf.assigned.has(name)) picker.add(new Option(name, name));
                }
                picker.disabled = add.disabled = picker.options.length === 0;
                if (conf._input.data("selectpicker")) conf._input.selectpicker("refresh");

                const body = table.tBodies[0];
                body.replaceChildren();
                table.tFoot.hidden = conf.assigned.size > 0;
                for (const name of conf.assigned) {
                    const row = body.insertRow();
                    row.insertCell().textContent = name;
                    row.insertCell().textContent = conf.catalog.get(name) ?? "";
                    const remove = removeTemplate.cloneNode(true);
                    remove.setAttribute("aria-label", remove.title + " " + name);
                    row.insertCell().appendChild(remove);
                    remove.addEventListener("click", () => {
                        conf.assigned.delete(name);
                        conf.render();
                        (body.querySelector("button") || add).focus();
                    });
                }
            };
            add.addEventListener("click", () => {
                if (!picker.value) return;
                conf.assigned.add(picker.value);
                conf.render();
                if (add.disabled) table.tBodies[0].querySelector("tr:last-child button").focus();
            });
            return view;
        },
        update(conf, options) {
            conf.catalog = new Map(options.map(option => [option.value, option.label]));
            conf.render();
        },
        set(conf, value) {
            conf.assigned = new Set(value ? value.split(",") : []);
            conf.render();
        },
        get(conf) {
            return Array.from(conf.assigned).join(",");
        },
        enable(conf) {
            conf._input.closest("fieldset").prop("disabled", false);
        },
        disable(conf) {
            conf._input.closest("fieldset").prop("disabled", true);
        }
    };
});
