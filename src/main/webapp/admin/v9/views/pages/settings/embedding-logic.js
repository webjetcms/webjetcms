/**
 * Returns active embedding tab value normalized to lowercase.
 *
 * @returns {string|null}
 */
function getActiveTabValue() {
    const activeTab = document.querySelector("#pills-embedding-chunks .nav-link.active");
    if (!activeTab || !activeTab.hash) {
        return null;
    }

    const value = activeTab.hash.trim().toLowerCase();
    return value.length > 0 ? value.replace("#pills-", "") : null;
}

/**
 * Rebuilds DataTable URL query based on selected external filters.
 * Updates index action availability and reloads the table when its API is available.
 *
 * @private
 */
function filterFn() {
    const entityType = getActiveTabValue();

    if (
        entityType == null ||
        typeof embeddingChunksDataTable === "undefined" ||
        typeof WJ === "undefined" ||
        typeof $ !== "function"
    ) {
        return;
    }

    embeddingChunksDataTable.setAjaxUrl(
        WJ.urlUpdateParam(embeddingChunksDataTable.getAjaxUrl(), "entityType", entityType.toUpperCase())
    );

    if ("document" === entityType) {
        const rootDirValue = $("#rootDir").val();
        // The switch keeps id "includeSubfolders" for compatibility with existing chart logic.
        const includeSubfolders = $("#includeSubfolders").is(":checked");

        embeddingChunksDataTable.setAjaxUrl(
            WJ.urlUpdateParam(embeddingChunksDataTable.getAjaxUrl(), "searchRootDir", rootDirValue)
        );
        embeddingChunksDataTable.setAjaxUrl(
            WJ.urlUpdateParam(embeddingChunksDataTable.getAjaxUrl(), "includeSubfolders", includeSubfolders)
        );
    }

    embeddingChunksDataTable.setAjaxUrl(
        WJ.urlUpdateParam(embeddingChunksDataTable.getAjaxUrl(), "sourceRoot", entityType === "markdown" ? $("#markdownFolder").val() || "" : "")
    );
    embeddingChunksDataTable.setAjaxUrl(
        WJ.urlUpdateParam(embeddingChunksDataTable.getAjaxUrl(), "directory", entityType === "markdown" ? $("#markdownDirectory").val() || "" : "")
    );
    if (entityType === "markdown") {
        embeddingChunksDataTable.setAjaxUrl(
            WJ.urlUpdateParam(embeddingChunksDataTable.getAjaxUrl(), "includeSubfolders", $("#markdownIncludeSubfolders").is(":checked"))
        );
    }

    updateIndexButtons();

    if (embeddingChunksDataTable.ajax && typeof embeddingChunksDataTable.ajax.reload === "function") {
        embeddingChunksDataTable.ajax.reload();
    }
}

/**
 * Creates root directory selector element for external filters.
 *
 * @returns {Promise<HTMLDivElement>}
 * @private
 */
async function _getRootDirDiv() {
    const rootDirDiv = document.createElement("div");
    rootDirDiv.className = "rootDirDiv col-auto";
    rootDirDiv.setAttribute("title", "[[#{components.stat.filter.showOnlyFromGroup}]]");
    rootDirDiv.setAttribute("data-bs-toggle", "tooltip");

    const queryString = window.location.search;
    const urlParams = new URLSearchParams(queryString);

    let rootDirId = urlParams.get('rootDir') || '-1';
    let rootDirPath = "[[#{jstree.all_dirs}]]";

    if (rootDirId !== '-1') {
        const urlGroupIdFilter = "/admin/rest/news/news-list/convertIdsToNamePair?ids=" + rootDirId;

        try {
            const response = await fetch(urlGroupIdFilter, {
                method: "POST",
                headers: {
                    "X-Requested-With": "XMLHttpRequest",
                    "X-CSRF-Token": window.csrfToken
                }
            });

            if (!response.ok) {
                throw new Error("HTTP " + response.status);
            }

            const data = await response.json();
            const selectedDir = Array.isArray(data)
                ? data.find(item => String(item.value) === String(rootDirId))
                : null;

            if (selectedDir && selectedDir.label) {
                rootDirPath = selectedDir.label;
            } else {
                rootDirId = '-1';
                rootDirPath = "[[#{jstree.all_dirs}]]";
            }
        } catch (error) {
            console.error("Group filter request failed:", error);
            rootDirId = '-1';
            rootDirPath = "[[#{jstree.all_dirs}]]";
        }
    }

    const input = document.createElement("input");
    input.id = "rootDir";
    input.className = "webjet-dte-jstree";
    input.type = "text";
    input.value = rootDirId;
    input.setAttribute("data-text", rootDirPath);
    input.setAttribute("data-text-empty", "[[#{jstree.all_dirs}]]");

    rootDirDiv.appendChild(input);
    return rootDirDiv;
}

/**
 * Creates include-subfolders switch element for external filters.
 *
 * @param {string} [id="includeSubfolders"] Identifier of the tab-specific switch.
 * @returns {HTMLDivElement}
 * @private
 */
function _getSubFolderCheck(id = "includeSubfolders") {
    const colDiv = document.createElement("div");
    colDiv.className = "col-auto";

    const innerDiv = document.createElement("div");
    innerDiv.className = "btn btn-sm custom-control form-switch";
    innerDiv.setAttribute("data-toggle", "tooltip");
    innerDiv.setAttribute("title", "[[#{settings.embedding-chunks.sub-folder}]]");

    const input = document.createElement("input");
    input.id = id;
    input.type = "checkbox";
    input.className = "form-check-input";
    input.value = "true";
    input.checked = true;
    input.setAttribute("aria-label", "[[#{settings.embedding-chunks.sub-folder}]]");
    if (id === "markdownIncludeSubfolders") {
        input.checked = new URLSearchParams(window.location.search).get("includeSubfolders") !== "false";
    }

    const label = document.createElement("label");
    label.setAttribute("for", id);
    label.className = "form-check-label is-icon-subfolders";

    innerDiv.appendChild(input);
    innerDiv.appendChild(label);
    colDiv.appendChild(innerDiv);

    return colDiv;
}

/**
 * Creates filters for the active tab once and toggles their visibility while preserving selections.
 * Initializes folder pickers, filter change handlers, and tooltips.
 *
 * @returns {Promise<void>} Resolves after filters are ready, or immediately if the filter tab is absent.
 */
async function addFilterBasedOnTab() {
    const bonusFilterTab = document.getElementById("pills-bonusFilter-tab");
    if (!bonusFilterTab) {
        return;
    }

    const entityType = getActiveTabValue();

    let extFilter = document.getElementById("embeddingChunksDataTable_extfilter");
    if (!extFilter) {
        extFilter = document.createElement("div");
        extFilter.id = "embeddingChunksDataTable_extfilter";
        bonusFilterTab.replaceChildren(extFilter);
    }

    if (!extFilter.querySelector('[data-entity-type="' + entityType + '"]')) {
        const wrapper = document.createElement("div");
        wrapper.className = "row datatableInit";
        wrapper.dataset.entityType = entityType;
        extFilter.appendChild(wrapper);

        if ("document" === entityType) {
            wrapper.appendChild(await _getRootDirDiv());
            wrapper.appendChild(_getSubFolderCheck());
            ChartTools.initGroupIdSelect();
            ChartTools.bindFilter(filterFn);
            $("#includeSubfolders").on("change", filterFn);
        } else if ("markdown" === entityType) {
            wrapper.appendChild(await _getMarkdownFolderDiv());
            wrapper.appendChild(_getSubFolderCheck("markdownIncludeSubfolders"));
            $("#markdownIncludeSubfolders").on("change", filterFn);
        }
    }

    extFilter.querySelectorAll("[data-entity-type]").forEach(filter => {
        filter.classList.toggle("d-none", filter.dataset.entityType !== getActiveTabValue());
    });

    WJ.initTooltip($('#embeddingChunksDataTable_extfilter [data-toggle*="tooltip"]'));
    WJ.initTooltip($('#embeddingChunksDataTable_extfilter [data-bs-toggle*="tooltip"]'));
}

/**
 * Creates the tree selector for configured Markdown roots and their subdirectories.
 *
 * @returns {Promise<HTMLDivElement>}
 * @private
 */
async function _getMarkdownFolderDiv() {
    const wrapper = document.createElement("div");
    wrapper.className = "rootDirDiv col-auto";
    const selector = new MarkdownFolderSelector(wrapper, filterFn);

    const message = document.createElement("span");
    message.className = "small text-muted";
    message.setAttribute("role", "status");
    wrapper.appendChild(message);

    const params = new URLSearchParams(window.location.search);
    await selector.initialize(params.get("sourceRoot"), params.get("directory"), message);
    return wrapper;
}

/**
 * Resolves initial tab from URL hash.
 *
 * @returns {string|null}
 * @private
 */
function _getTabFromUrl() {
    const initHash = window.location.hash;
    if (initHash && initHash.startsWith("#pills-")) {
        return initHash.replace("#pills-", "#");
    }
    return null;
}

/**
 * Builds embedding tabs with the URL hash selecting the initial tab when it matches.
 *
 * @returns {{url: string, title: string, active: boolean}[]} Tab definitions, defaulting to the web-page tab.
 */
function getHeaderTabs() {
    const tabs = [
        { url: "#document", title: "[[#{menu.web_sites}]]", active: true },
        { url: "#markdown", title: "[[#{settings.markdown-chunks.tab}]]", active: false }
    ];

    const actualTab = _getTabFromUrl();

    if (actualTab) {
        const matched = tabs.some(tab => tab.url === actualTab);
        if (matched) {
            tabs.forEach(tab => {
                tab.active = tab.url === actualTab;
            });
        }
    }

    return tabs;
}

/**
 * Copies the current web-page folder selection and recursion flag into a dialog URL.
 *
 * @param {string} baseUrl - Dialog URL resolved against the current origin.
 * @returns {URL} URL with available folder filter values applied.
 */
function _addParamsBtnUrl(baseUrl) {
    const rootDir = document.getElementById("rootDir");
    const includeSubfolders = document.getElementById("includeSubfolders");
    const rootDirValue = rootDir ? String(rootDir.value || "") : "";
    const rootDirText = rootDir ? String(rootDir.getAttribute("data-text") || "") : "";

    const url = new URL(baseUrl, window.location.origin);
    if (rootDirValue.length > 0) { url.searchParams.set("rootDir", rootDirValue); }
    if (rootDirText.length > 0) { url.searchParams.set("rootDirText", rootDirText); }
    if (includeSubfolders) { url.searchParams.set("includeSubfolders", String(includeSubfolders.checked)); }

    return url;
}

/**
 * Returns configuration for the "add to index" action.
 *
 * @returns {{url: string, title: string, buttonTitleKey: string}|null}
 * @private
 */
function _getAddIndexButtonConf() {
    const entityType = getActiveTabValue();
    if ("markdown" === entityType) return _getMarkdownIndexButtonConf("index");
    if ("document" === entityType) {
        const url = _addParamsBtnUrl("/admin/v9/settings/doc-chunks/?action=index");
        return {
            url: url.pathname + url.search,
            title: "[[#{settings.doc-chunks.title}]]",
            buttonTitleKey: "[[#{settings.embedding-chunks.start}]]"
        };
    }

    return null;
}

/**
 * Returns configuration for the "remove from index" action.
 *
 * @returns {{url: string, title: string, buttonTitleKey: string}|null}
 * @private
 */
function _getRemoveIndexButtonConf() {
    const entityType = getActiveTabValue();
    if ("markdown" === entityType) return _getMarkdownIndexButtonConf("delete");
    if ("document" === entityType) {
        const url = _addParamsBtnUrl("/admin/v9/settings/doc-chunks/?action=delete");
        return {
            url: url.pathname + url.search,
            title: "[[#{settings.doc-chunks.title}]]",
            buttonTitleKey: "[[#{settings.embedding-chunks.start}]]"
        };
    }

    return null;
}

/**
 * Builds the queue action dialog URL for the selected Markdown folder.
 *
 * @param {string} action Queue action to preview and submit.
 * @returns {{url: string, title: string, buttonTitleKey: string}|null} Modal configuration, or null if no folder is selected.
 * @private
 */
function _getMarkdownIndexButtonConf(action) {
    const folder = document.getElementById("markdownFolder")?.value;
    if (!folder) return null;
    const params = new URLSearchParams({
        action,
        folder,
        directory: document.getElementById("markdownDirectory")?.value || "",
        includeSubfolders: String(document.getElementById("markdownIncludeSubfolders")?.checked !== false)
    });
    return {
        url: "/admin/v9/settings/markdown-chunks/?" + params.toString(),
        title: "[[#{settings.markdown-chunks.title}]]",
        buttonTitleKey: "[[#{settings.embedding-chunks.start}]]"
    };
}

/**
 * Enables bulk index actions when no rows are selected, requiring a selected root on the Markdown tab.
 * Requires the embedding DataTable to be initialized.
 */
function updateIndexButtons() {
    const disabled = embeddingChunksDataTable.rows({ selected: true }).any() ||
        (getActiveTabValue() === "markdown" && !document.getElementById("markdownFolder")?.value);
    embeddingChunksDataTable.buttons(".btnAddIndex, .btnRemoveIndex").enable(!disabled);
}

/**
 * Opens an iframe modal for indexing actions.
 *
 * @param {{url: string, title: string, buttonTitleKey: string}|null} conf Modal configuration; null leaves the modal closed.
 * @private
 */
function _openIndexModal(conf) {
    if (!conf || !conf.url || typeof WJ === "undefined") {
        return;
    }

    WJ.openIframeModal({
        url: conf.url,
        width: 700,
        height: 560,
        title: conf.title,
        buttonTitleKey: conf.buttonTitleKey,
        okclick: function () {
            $("#modalIframeIframeElement").contents().find("button#submitBtn").click();
            return false;
        }
    });
}

/**
 * Creates the DataTable button that opens an indexing dialog for the scope selected at click time.
 *
 * @returns {{text: string, action: function(): void, className: string, attr: Object<string, string>}} Button configuration.
 */
function getAddIndexButton() {
    return {
        text: "<i class=\"ti ti-database-plus\" aria-hidden=\"true\"></i>",
        action: function () {
            const clickConf = _getAddIndexButtonConf();
            _openIndexModal(clickConf);
        },
        className: "btn btn-sm btn-success btnAddIndex",
        attr: {
            title: "[[#{settings.embedding-chunks.add}]]",
            "aria-label": "[[#{settings.embedding-chunks.add}]]",
            "data-toggle": "tooltip"
        }
    };
}

/**
 * Creates the DataTable button that opens an index removal dialog for the scope selected at click time.
 *
 * @returns {{text: string, action: function(): void, className: string, attr: Object<string, string>}} Button configuration.
 */
function getRemoveIndexButton() {
    return {
        text: "<i class=\"ti ti-database-minus\" aria-hidden=\"true\"></i>",
        action: function () {
            const clickConf = _getRemoveIndexButtonConf();
            _openIndexModal(clickConf);
        },
        className: "btn btn-sm btn-danger btnRemoveIndex",
        attr: {
            title: "[[#{settings.embedding-chunks.remove}]]",
            "aria-label": "[[#{settings.embedding-chunks.remove}]]",
            "data-toggle": "tooltip"
        }
    };
}

/**
 * Applies the active tab's filters, action availability, table columns, and editor fields.
 * Requires the embedding DataTable to be initialized and starts reloading it after setup.
 *
 * @returns {Promise<void>} Resolves after tab setup; does not wait for the table reload.
 */
async function initPage() {
    updateIndexButtons();
    // First set filter
    await addFilterBasedOnTab();

    const markdown = getActiveTabValue() === "markdown";
    ["sourcePath", "sourceTitle"].forEach(name => {
        embeddingChunksDataTable.column(name + ":name").visible(markdown, false);
        if (markdown) embeddingChunksDataTable.EDITOR.show(name);
        else embeddingChunksDataTable.EDITOR.hide(name);
    });
    embeddingChunksDataTable.column("entityId:name").visible(!markdown, false);
    if (markdown) embeddingChunksDataTable.EDITOR.hide("entityId");
    else embeddingChunksDataTable.EDITOR.show("entityId");

    filterFn();
}
