function initAiInstructionsField() {
    const textarea = this.field("instructions").input()[0];
    WJ.initTextareaLineNumbers(textarea, {toggleWrapOnDoubleClick: true});
    const wrapper = textarea?.closest(".md-textarea-editor");
    if (!wrapper) return;
    const existing = wrapper.closest(".skill-references");
    const view = existing || document.querySelector("#skill-reference-template").content.firstElementChild.cloneNode(true);
    const drawer = view.querySelector("aside");
    const toggle = view.querySelector(".reference-toggle");
    const search = view.querySelector("input");
    toggle.hidden = this.ids().length > 1;
    if (existing) {
        toggle.setAttribute("aria-expanded", "false");
        drawer.hidden = true;
        search.value = "";
        return;
    }

    function insertText(text) {
        textarea.setRangeText(text, textarea.selectionStart, textarea.selectionEnd, "end");
        textarea.dispatchEvent(new Event("input", {bubbles: true}));
    }

    const tabId = textarea.closest('[role="tabpanel"]')?.getAttribute("aria-labelledby");
    if (tabId) textarea.setAttribute("aria-labelledby", tabId);
    textarea.addEventListener("keydown", event => {
        if (event.key !== "Tab") return;
        event.preventDefault();
        insertText("\t");
    });

    const results = view.querySelector(".reference-results");
    const translate = (key, ...args) => WJ.translate("components.ai_assistants." + key + ".js", ...args);
    const normalize = value => value.normalize("NFD").replace(/\p{M}/gu, "").toLocaleLowerCase();
    const isWrite = tool => !["GET", "HEAD", "OPTIONS"].includes(tool.httpMethod);
    const writingArea = wrapper.closest(".input-group") || wrapper;
    writingArea.before(view);
    view.querySelector(".reference-layout").prepend(writingArea);

    function insertButton(label, token) {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "btn btn-sm btn-link text-start d-block mw-100";
        button.textContent = label;
        button.onclick = () => {
            textarea.focus();
            insertText(token);
            view.querySelector('[role="status"]').textContent = translate("skills.reference_inserted", token);
        };
        return button;
    }

    const table = this.TABLE;
    function render() {
        const skills = table.DATA.jsonOptions?.skillIds || [];
        const groups = new Map();
        for (const {original: tool} of table.DATA.jsonOptions?.toolNames || []) {
            if (!groups.has(tool.group)) groups.set(tool.group, []);
            groups.get(tool.group).push(tool);
        }
        const query = normalize(search.value.trim());
        results.replaceChildren();
        const matchingSkills = skills.filter(skill => normalize(skill.label).includes(query) || String(skill.value) === query);
        if (matchingSkills.length) {
            const heading = document.createElement("strong");
            heading.textContent = view.dataset.skills;
            results.append(heading);
            for (const skill of matchingSkills) {
                results.append(insertButton(skill.label + " (#" + skill.value + ")", "@skill(" + skill.value + ")"));
            }
        }
        let toolsHeading;
        for (const [key, tools] of groups) {
            const label = tools[0].groupLabel;
            const matchesGroup = normalize(label + " " + key).includes(query);
            const matches = tools.filter(tool => matchesGroup ||
                normalize([tool.name, tool.groupLabel, tool.httpMethod, tool.path, tool.description].join(" ")).includes(query));
            if (!matches.length) continue;
            if (view.dataset.skills && !toolsHeading) {
                toolsHeading = document.createElement("strong");
                toolsHeading.textContent = view.dataset.tools;
                results.append(toolsHeading);
            }
            const details = document.createElement("details");
            details.className = "border-top py-2";
            details.open = Boolean(query) || groups.size === 1;
            const summary = document.createElement("summary");
            const writes = tools.filter(isWrite).length;
            summary.textContent = label + " · " + translate("tools.group_details", tools.length, writes);
            details.append(summary, insertButton(translate("skills.insert_group"), "@group(" + key + ")"));
            for (const tool of matches) {
                const button = insertButton(tool.name, "@tool(" + tool.name + ")");
                const path = document.createElement("small");
                path.className = "d-block text-muted";
                path.textContent = (isWrite(tool) ? view.dataset.write : view.dataset.read) + " · " + tool.httpMethod + " " + tool.path;
                button.append(path);
                details.append(button);
            }
            results.append(details);
        }
        if (!results.childElementCount) results.textContent = groups.size || skills.length ? view.dataset.noMatches : view.dataset.empty;
    }

    toggle.onclick = () => {
        drawer.hidden = !drawer.hidden;
        toggle.setAttribute("aria-expanded", String(!drawer.hidden));
        if (!drawer.hidden) { render(); search.focus(); }
    };
    search.oninput = render;
    search.onkeydown = event => {
        if (event.key === "Enter") event.preventDefault();
        if (event.key === "Escape") {
            event.preventDefault();
            event.stopPropagation();
            toggle.click();
            toggle.focus();
        }
    };
}
