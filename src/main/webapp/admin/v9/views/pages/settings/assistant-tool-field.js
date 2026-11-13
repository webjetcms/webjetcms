window.domReady.add(function () {
    const translate = (key, ...args) => WJ.translate("components.ai_assistants.tools." + key + ".js", ...args);
    const normalize = value => value.normalize("NFD").replace(/\p{M}/gu, "").toLocaleLowerCase();
    const pageSize = 8;
    const methodColors = { GET: "success", POST: "primary", PUT: "info", PATCH: "warning", DELETE: "danger" };
    const icon = name => `<i class="ti ti-${name}" aria-hidden="true"></i>`;

    const escape = $.fn.dataTable.util.escapeHtml;

    function row(tag, classes, content) {
        const node = document.createElement(tag);
        node.className = classes;
        if (tag === "button") node.type = "button";
        node.innerHTML = content;
        return node;
    }

    function endpoint(name, tool) {
        const title = `<strong class="d-block">${escape(name)}</strong>`;
        if (!tool) return title;
        const color = methodColors[tool.httpMethod] ?? "secondary";
        return title + `<small class="d-block text-muted"><span class="tool-method badge bg-${color}-subtle text-${color}-emphasis">${escape(tool.httpMethod)}</span> ${escape(tool.path)}</small>`;
    }

    function groupBy(items, keyOf) {
        const groups = new Map();
        for (const item of items) {
            const key = keyOf(item);
            if (!groups.has(key)) groups.set(key, []);
            groups.get(key).push(item);
        }
        return groups;
    }

    $.fn.dataTable.Editor.fieldTypes.assistantTools = {
        create(conf) {
            const view = document.querySelector("#assistant-tools-template").content.firstElementChild.cloneNode(true);
            const search = view.querySelector("input");
            const results = view.querySelector(".tool-results");
            const assignedGroups = view.querySelector(".tool-groups");
            const undo = view.querySelector(".tool-undo");
            const expanded = new Set();
            const browseRows = new Map();
            const catalog = new Map();
            let groups;
            let initial = [], removals = [];
            let assigned = new Set();
            let limit = pageSize;

            search.id = conf.id;
            view.querySelector("label").htmlFor = search.id;
            results.id = conf.id + "-results";
            search.setAttribute("aria-controls", results.id);
            conf._input = $(search);

            function dismissOutside(event) {
                if (event.type === "keyup" && event.key !== "Tab") return;
                const path = event.composedPath();
                if (!path.includes(search) && !path.includes(results)) hideResults();
            }

            function hideResults() {
                document.removeEventListener("click", dismissOutside, true);
                document.removeEventListener("keyup", dismissOutside, true);
                results.hidden = true;
                limit = pageSize;
                browseRows.clear();
            }

            function remove(names, label) {
                names.forEach(name => assigned.delete(name));
                removals.push({ names, label });
                renderAssigned();
                undo.focus();
            }

            function assignedRow(name) {
                const tool = catalog.get(name);
                const label = escape(translate("remove_endpoint") + ": " + name);
                const item = row("div", "d-flex align-items-center gap-2 border-top py-2 ps-3",
                    `<div class="tool-item-text flex-grow-1">${endpoint(name, tool)}<span class="d-block">${escape(tool?.description ?? "")}</span></div>
                    <button type="button" class="btn btn-sm btn-outline-danger" data-remove="${escape(name)}" title="${label}" aria-label="${label}">${icon("trash")}</button>`);
                item.querySelector("button").onclick = () => remove([name], name);
                return item;
            }

            function renderAssigned(focusGroup) {
                const grouped = groupBy(assigned, name => catalog.get(name)?.group ?? null);
                view.querySelector(".tool-count").textContent = translate("counts", grouped.size, assigned.size);
                view.querySelector(".tool-empty").hidden = assigned.size > 0;
                assignedGroups.replaceChildren();
                for (const [key, names] of grouped) {
                    const group = groups.get(key);
                    const label = group?.[0].groupLabel ?? translate("unlisted");
                    const open = expanded.has(key);
                    const count = group ? names.length + " / " + group.length : names.length;
                    const removeLabel = translate("remove_group");
                    const drop = group ? `<button type="button" class="tool-remove-group btn btn-sm btn-outline-danger"
                        aria-label="${escape(removeLabel + ": " + label + " (" + names.length + ")")}">${escape(removeLabel)}</button>` : "";
                    const item = row("div", "list-group-item px-2", `<div class="d-flex align-items-center gap-2${open ? " pb-2" : ""}">
                        <button type="button" class="tool-toggle btn btn-sm btn-link text-body text-start text-decoration-none d-flex align-items-center gap-2"
                            data-group="${escape(key ?? "")}" aria-expanded="${open}">${icon("chevron-right")}
                            <strong class="tool-item-text">${escape(label)}</strong><span class="badge bg-light text-body">${count}</span></button>${drop}</div>`);
                    const toggle = item.querySelector(".tool-toggle");
                    toggle.onclick = () => {
                        if (open) expanded.delete(key);
                        else expanded.add(key);
                        renderAssigned(key);
                    };
                    if (group) item.querySelector(".tool-remove-group").onclick = () => remove(names, label);
                    if (open) item.append(...names.map(assignedRow));
                    assignedGroups.append(item);
                    if (key === focusGroup) toggle.focus();
                }
                const last = removals.at(-1);
                view.querySelector('[role="status"]').textContent = last ? translate("removed", last.label, last.names.length) : "";
                undo.hidden = !last;
            }

            function add(names) {
                names.forEach(name => assigned.add(name));
                // Re-adding part of a removed group leaves only its missing tools to undo.
                for (const removal of removals) removal.names = removal.names.filter(name => !assigned.has(name));
                removals = removals.filter(removal => removal.names.length);
                search.value = "";
                hideResults();
                renderAssigned(catalog.get(names[0])?.group ?? null);
            }

            function browseGroup(group) {
                const { group: key, groupLabel } = group[0];
                if (browseRows.has(key)) return browseRows.get(key);
                const missing = group.map(tool => tool.name).filter(name => !assigned.has(name));
                const writes = group.filter(tool => !["GET", "HEAD", "OPTIONS"].includes(tool.httpMethod)).length;
                const panelId = escape(results.id + "-group-" + browseRows.size);
                const item = row("div", "list-group-item p-0", `<div class="tool-browse-header d-flex flex-wrap align-items-center gap-2 px-2 py-1">
                    <button type="button" class="tool-result tool-browse tool-toggle btn btn-link text-body text-decoration-none text-start d-flex align-items-center gap-2"
                        data-group="${escape(key)}" aria-expanded="false" aria-controls="${panelId}">${icon("chevron-right")}
                        <span class="tool-item-text"><strong class="d-block">${escape(groupLabel)}</strong>
                        <small class="d-block text-muted">${escape(translate("group_details", group.length, writes))}</small></span></button>
                    <div class="d-flex flex-wrap gap-1"><button type="button" class="tool-add-selected btn btn-sm btn-primary" hidden disabled></button>
                    <button type="button" class="tool-add-all btn btn-sm btn-outline-primary" ${missing.length ? "" : "disabled"}>
                        ${escape(missing.length ? translate("add_all", missing.length) : translate("already_added"))}</button></div></div>
                    <div class="tool-choices" id="${panelId}" hidden></div>`);
                const toggle = item.querySelector(".tool-browse");
                const panel = item.querySelector(".tool-choices");
                const subset = item.querySelector(".tool-add-selected");
                const selected = new Set();
                item.querySelector(".tool-add-all").onclick = () => add(missing);
                subset.onclick = () => add([...selected]);
                subset.textContent = translate("add_selected", 0);
                panel.onchange = ({target}) => {
                    if (target.checked) selected.add(target.value);
                    else selected.delete(target.value);
                    subset.textContent = translate("add_selected", selected.size);
                    subset.disabled = !selected.size;
                };
                toggle.onclick = () => {
                    if (!panel.childElementCount) {
                        for (const tool of group) {
                            const added = assigned.has(tool.name);
                            panel.append(row("label", "tool-choice d-flex align-items-center gap-2 border-top py-2 ps-3 pe-2 mb-0",
                                `<input type="checkbox" class="form-check-input flex-shrink-0 mt-0" value="${escape(tool.name)}" ${added ? "checked disabled" : ""}>
                                <span class="tool-item-text flex-grow-1">${endpoint(tool.name, tool)}</span>
                                ${added ? `<small class="text-muted">${escape(translate("already_added"))}</small>` : ""}`));
                        }
                    }
                    panel.hidden = !panel.hidden;
                    toggle.setAttribute("aria-expanded", !panel.hidden);
                    subset.hidden = panel.hidden || !missing.length;
                    if (!panel.hidden) results.scrollTop += toggle.parentElement.getBoundingClientRect().top - results.getBoundingClientRect().top - 1;
                };
                browseRows.set(key, item);
                return item;
            }

            function singleTool(tool) {
                const added = assigned.has(tool.name);
                const action = added ? `<small class="text-muted">${escape(translate("already_added"))}</small>`
                    : `<span class="btn btn-sm btn-outline-primary text-nowrap">${escape(translate("add_count", 1))}</span>`;
                const item = row("button", "tool-result list-group-item list-group-item-action d-flex align-items-center gap-2",
                    `<span class="tool-item-text flex-grow-1">${endpoint(tool.name, tool)}</span>${action}`);
                item.disabled = added;
                item.onclick = () => add([tool.name]);
                return item;
            }

            function showResults() {
                // Closing on blur would move the assigned group between pointer down and click.
                document.addEventListener("click", dismissOutside, true);
                document.addEventListener("keyup", dismissOutside, true);
                results.hidden = false;
                const query = normalize(search.value.trim());
                const words = query.split(/\s+/);
                const matches = text => words.every(word => text.includes(word));
                const items = [...groups.values()].filter(([tool]) => matches(normalize(tool.groupLabel + " " + tool.group)));
                if (query) {
                    for (const tool of catalog.values()) if (matches(tool.search)) items.push(tool);
                }
                results.replaceChildren(...items.slice(0, limit).map(item => Array.isArray(item) ? browseGroup(item) : singleTool(item)));
                if (!items.length) results.append(row("div", "list-group-item text-muted", escape(translate("no_matches"))));
                if (items.length > limit) {
                    const more = row("button", "list-group-item list-group-item-action text-primary", escape(translate("more")));
                    more.onclick = () => {
                        const next = limit;
                        limit += pageSize;
                        showResults();
                        const row = Array.from(results.children).slice(next).find(node => !node.disabled);
                        (row?.querySelector(".tool-browse") ?? row ?? search).focus();
                    };
                    results.append(more);
                }
            }

            search.oninput = () => { limit = pageSize; showResults(); };
            search.onclick = showResults;
            view.onkeydown = event => {
                if (event.target !== search && !results.contains(event.target)) return;
                if (event.key === "Escape" && !results.hidden) {
                    event.preventDefault();
                    event.stopPropagation();
                    search.focus();
                    hideResults();
                } else if (event.key === "ArrowDown" || event.key === "ArrowUp") {
                    event.preventDefault();
                    if (results.hidden) showResults();
                    const controls = Array.from(results.querySelectorAll("button:enabled, input:enabled")).filter(node => !node.closest("[hidden]"));
                    const index = controls.indexOf(event.target) + (event.key === "ArrowDown" ? 1 : -1);
                    (controls[Math.min(index, controls.length - 1)] ?? search).focus();
                } else if (event.key === "Enter") {
                    event.stopPropagation();
                    if (event.target !== search) return;
                    event.preventDefault();
                    if (!results.hidden) {
                        const first = results.querySelector(".tool-result:enabled");
                        first?.focus();
                        first?.click();
                    }
                }
            };
            undo.onclick = () => {
                const last = removals.pop();
                if (!last) return;
                last.names.forEach(name => assigned.add(name));
                renderAssigned(catalog.get(last.names[0])?.group ?? null);
            };

            conf.setCatalog = options => {
                browseRows.clear();
                catalog.clear();
                for (const {original: tool} of options) {
                    if (!tool) continue;
                    const {name, groupLabel, httpMethod, path, description} = tool;
                    catalog.set(name, {...tool, search: normalize([name, groupLabel, httpMethod, path, description].join(" "))});
                }
                groups = groupBy(catalog.values(), tool => tool.group);
                renderAssigned();
                if (!results.hidden) showResults();
            };
            conf.setNames = value => {
                initial = value ? value.split(",").filter(Boolean) : [];
                assigned = new Set(initial);
                removals = [];
                expanded.clear();
                search.value = "";
                hideResults();
                renderAssigned();
            };
            // Restoring an existing selection also restores its serialized value and order.
            conf.getNames = () => [...new Set([...initial.filter(name => assigned.has(name)), ...assigned])].join(",");
            conf.setCatalog(conf.options || []);
            return view;
        },
        update(conf, options) { conf.setCatalog(options); },
        set(conf, value) { conf.setNames(value); },
        get(conf) { return conf.getNames(); },
        enable(conf) { conf._input.closest("fieldset").prop("disabled", false); },
        disable(conf) { conf._input.closest("fieldset").prop("disabled", true); }
    };
});
