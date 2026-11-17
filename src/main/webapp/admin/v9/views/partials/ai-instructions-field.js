function initAiInstructionsField() {
    const textarea = this.field("instructions").input()[0];
    WJ.initTextareaLineNumbers(textarea, {toggleWrapOnDoubleClick: true});
    const wrapper = textarea?.closest(".md-textarea-editor");
    if (!wrapper) return;
    const existing = wrapper.closest(".skill-references");
    if (existing) {
        existing.resetReferenceDrawer();
        return;
    }

    const view = document.querySelector("#skill-reference-template").content.firstElementChild.cloneNode(true);
    const drawer = view.querySelector("aside");
    const toggle = view.querySelector(".reference-toggle");
    const groupButton = drawer.querySelector(".reference-insert-group");
    const groupField = this.field("toolGroup");
    const groupInput = groupField.input()[0];

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

    const normalize = value => value.normalize("NFD").replace(/\p{M}/gu, "").toLocaleLowerCase();
    const writingArea = wrapper.closest(".input-group") || wrapper;
    writingArea.before(view);
    view.querySelector(".reference-layout").prepend(writingArea);
    WJ.initTooltip($(toggle));

    function insertReference(token) {
        textarea.focus();
        insertText(token);
        view.querySelector('[role="status"]').textContent = WJ.translate("components.ai_assistants.skills.reference_inserted.js", token);
    }

    const table = this.TABLE;
    const toolsPane = drawer.querySelector("#reference-tools");
    const toolsTab = drawer.querySelector("#reference-tab-tools");
    const skillsPane = drawer.querySelector("#reference-skills");
    const highlights = document.createElement("div");
    highlights.className = "md-textarea-editor__highlights";
    highlights.setAttribute("aria-hidden", "true");
    textarea.before(highlights);

    function syncHighlightScroll() {
        highlights.scrollTop = textarea.scrollTop;
        highlights.scrollLeft = textarea.scrollLeft;
    }

    function highlightReferences() {
        const text = textarea.value;
        const fragment = document.createDocumentFragment();
        let end = 0;
        // Match AiInstructionTemplate.LINK. Skill bodies only expand tool and group references.
        for (const match of text.matchAll(/@(tool|group)\([A-Za-z0-9_.$-]+\)|@(skill)\([^()\r\n]+\)/g)) {
            if (match[2] && !skillsPane) continue;
            fragment.append(text.slice(end, match.index));
            const span = document.createElement("span");
            span.dataset.reference = match[1] || match[2];
            span.textContent = match[0];
            fragment.append(span);
            end = match.index + match[0].length;
        }
        fragment.append(text.slice(end));
        highlights.replaceChildren(fragment);
        syncHighlightScroll();
    }

    $(textarea).on("input change", highlightReferences);
    textarea.addEventListener("scroll", syncHighlightScroll);

    const skillSearch = skillsPane?.querySelector("input");
    const filters = drawer.querySelector(".reference-methods");
    groupField.node().querySelector("label").classList.add("visually-hidden");
    let tools = [];
    let selectedGroupKey;
    let method = "";

    function renderReferences(pane, matches, total, fillButton) {
        const results = pane.querySelector(".reference-results");
        const row = pane.querySelector("template").content.firstElementChild;
        results.replaceChildren();
        for (const entry of matches) {
            const button = row.cloneNode(true);
            fillButton(button, entry);
            results.append(button);
        }
        if (!matches.length) results.textContent = total ? view.dataset.noMatches : pane.dataset.empty;
        results.scrollTop = 0;
    }

    function renderTools() {
        const groupTools = selectedGroupKey ? tools.filter(tool => tool.group === selectedGroupKey) : tools;
        const methods = new Set(groupTools.map(tool => tool.httpMethod));
        if (!methods.has(method)) method = "";
        for (const button of filters.children) {
            const verb = button.dataset.method;
            button.hidden = verb !== "" && !methods.has(verb);
            button.setAttribute("aria-pressed", String(verb === method));
        }

        // Unmatched or ambiguous text keeps the method choices available until the group is resolved.
        const matches = selectedGroupKey === null ? [] : groupTools.filter(tool => !method || tool.httpMethod === method);
        renderReferences(toolsPane, matches, tools.length, (button, tool) => {
            button.onclick = () => insertReference("@tool(" + tool.name + ")");
            button.querySelector(".reference-description").textContent = tool.description;
            button.querySelector(".reference-name").textContent = tool.name;
            button.querySelector(".reference-path").textContent = tool.path;
            const badge = button.querySelector(".reference-method");
            badge.dataset.method = tool.httpMethod;
            badge.textContent = tool.httpMethod;
            button.title = tool.description;
        });
    }

    if (skillsPane) {
        function renderSkills() {
            const skills = table.DATA.jsonOptions?.skillNames || [];
            const query = normalize(skillSearch.value.trim());
            const matches = skills.filter(skill => normalize(skill.label + " " + (skill.original || "")).includes(query));
            renderReferences(skillsPane, matches, skills.length, (button, skill) => {
                button.onclick = () => insertReference(skill.value);
                button.querySelector(".reference-name").textContent = skill.label;
                button.querySelector(".reference-excerpt").textContent = skill.original;
            });
        }
        drawer.querySelector("#reference-tab-skills").addEventListener("shown.bs.tab", renderSkills);
        skillSearch.oninput = renderSkills;
    }

    function selectGroup(key) {
        selectedGroupKey = key;
        groupButton.disabled = !key;
        renderTools();
    }

    $(groupInput).on("autocompleteselect", (event, {item}) => selectGroup(item.key));
    groupInput.oninput = () => {
        const value = normalize(groupInput.value.trim());
        if (!value) {
            selectGroup("");
            return;
        }
        const keys = new Set(tools.filter(tool => normalize(tool.groupLabel) === value).map(tool => tool.group));
        selectGroup(keys.size === 1 ? keys.values().next().value : null);
    };
    filters.onclick = event => {
        const button = event.target.closest("button");
        if (!button) return;
        method = button.dataset.method;
        renderTools();
    };
    groupButton.onclick = () => insertReference("@group(" + selectedGroupKey + ")");

    function setDrawerOpen(open) {
        drawer.hidden = !open;
        toggle.setAttribute("aria-expanded", String(open));
    }

    toggle.onclick = () => {
        // Clear both the visible tooltip and any delayed hover or focus trigger.
        const $toggle = $(toggle);
        $toggle.tooltip("dispose").removeAttr("aria-describedby");
        WJ.initTooltip($toggle);
        setDrawerOpen(drawer.hidden);
    };
    drawer.onkeydown = event => {
        if (event.target === groupInput) return;
        if (event.target === skillSearch && event.key === "Enter") event.preventDefault();
        if (event.key === "Escape") {
            event.stopPropagation();
            if (event.defaultPrevented) return;
            event.preventDefault();
            toggle.focus();
            toggle.click();
        }
    };
    view.resetReferenceDrawer = () => {
        highlightReferences();
        drawer.querySelector(".reference-filters").prepend(groupField.node());
        const provider = skillsPane && table.DATA.jsonOptions?.provider?.find(option => option.value === this.val("provider"));
        toggle.hidden = this.ids().length > 1 || (skillsPane && !provider?.original?.includes(this.val("action")));
        if (!toggle.hidden) {
            if (toolsTab) {
                skillSearch.value = "";
                bootstrap.Tab.getOrCreateInstance(toolsTab).show();
            }
            tools = (table.DATA.jsonOptions?.toolNames || []).map(option => option.original);
            groupInput.value = "";
            groupInput.disabled = !tools.length;
            method = "";
            selectGroup("");
        }
        setDrawerOpen(!toggle.hidden);
    };
    if (skillsPane) this.field("provider").input().add(this.field("action").input()).on("change", view.resetReferenceDrawer);
    view.resetReferenceDrawer();
}
