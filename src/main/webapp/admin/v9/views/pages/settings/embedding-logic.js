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
 *
 * @private
 */
function filterFn() {
    const entityType = getActiveTabValue();

    if (typeof embeddingChunksDataTable === "undefined" || typeof WJ === "undefined" || typeof $ !== "function") {
        return;
    }

    embeddingChunksDataTable.setAjaxUrl(
        WJ.urlUpdateParam(embeddingChunksDataTable.getAjaxUrl(), "entityType", entityType.toUpperCase())
    );

    if ("document" === entityType) {
        const rootDirValue = $("#rootDir").val();
        // The switch keeps id "botFilterOut" for compatibility with existing chart logic.
        const includeSubfolders = $("#botFilterOut").is(":checked");

        embeddingChunksDataTable.setAjaxUrl(
            WJ.urlUpdateParam(embeddingChunksDataTable.getAjaxUrl(), "searchRootDir", rootDirValue)
        );
        embeddingChunksDataTable.setAjaxUrl(
            WJ.urlUpdateParam(embeddingChunksDataTable.getAjaxUrl(), "includeSubfolders", includeSubfolders)
        );
    }

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
 * @returns {HTMLDivElement}
 * @private
 */
function _getSubFolderCheck() {
    const colDiv = document.createElement("div");
    colDiv.className = "col-auto";

    const innerDiv = document.createElement("div");
    innerDiv.className = "btn btn-sm custom-control form-switch";
    innerDiv.setAttribute("data-toggle", "tooltip");
    innerDiv.setAttribute("title", "[[#{settings.embedding-chunks.sub-folder}]]");

    const input = document.createElement("input");
    input.id = "botFilterOut";
    input.type = "checkbox";
    input.className = "form-check-input";
    input.value = "true";

    const label = document.createElement("label");
    label.setAttribute("for", "botFilterOut");
    label.className = "form-check-label is-icon-subfolders";

    innerDiv.appendChild(input);
    innerDiv.appendChild(label);
    colDiv.appendChild(innerDiv);

    return colDiv;
}

async function addFilterBasedOnTab() {
    const bonusFilterTab = document.getElementById("pills-bonusFilter-tab");
    if (!bonusFilterTab) {
        return;
    }

    bonusFilterTab.innerHTML = "";

    const entityType = getActiveTabValue();

    let extFilter = document.getElementById("embeddingChunksDataTable_extfilter");
    if (!extFilter) {
        extFilter = document.createElement("div");
        extFilter.id = "embeddingChunksDataTable_extfilter";

        const wrapper = document.createElement("div");
        wrapper.className = "row datatableInit";

        if ("document" === entityType) {
            wrapper.appendChild(await _getRootDirDiv());
            wrapper.appendChild(_getSubFolderCheck());
        }

        extFilter.appendChild(wrapper);
    }

    bonusFilterTab.appendChild(extFilter);

    setTimeout(() => {
        if ("document" === entityType) {
            ChartTools.initGroupIdSelect();
            ChartTools.bindFilter(filterFn);
        }

        // Init tooltips
        WJ.initTooltip($('#embeddingChunksDataTable_extfilter [data-toggle*="tooltip"]'));
        WJ.initTooltip($('#embeddingChunksDataTable_extfilter [data-bs-toggle*="tooltip"]'));
    }, 0);
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

function getHeaderTabs() {
    const tabs = [
        { url: "#document", title: "[[#{menu.web_sites}]]", active: true }
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

function _addParamsBtnUrl(baseUrl) {
    const rootDir = document.getElementById("rootDir");
    const rootDirValue = rootDir ? String(rootDir.value || "") : "";
    const rootDirText = rootDir ? String(rootDir.getAttribute("data-text") || "") : "";

    const url = new URL(baseUrl, window.location.origin);
    if (rootDirValue.length > 0) { url.searchParams.set("rootDir", rootDirValue); }
    if (rootDirText.length > 0) { url.searchParams.set("rootDirText", rootDirText); }

    return url;
}

/**
 * Returns configuration for the "add to index" action.
 *
 * @returns {{url: string, buttonTitleKey: string, tooltip: string}|null}
 * @private
 */
function _getAddIndexButtonConf() {
    const entityType = getActiveTabValue();
    if ("document" === entityType) {
        const url = _addParamsBtnUrl("/admin/v9/settings/doc-chunks/?action=index");
        return {
            url: url.pathname + url.search,
            title: "[[#{settings.doc-chunks.title}]]",
            buttonTitleKey: "[[#{settings.embedding-chunks.start}]]",
            tooltip: "[[#{settings.embedding-chunks.add}]]"
        };
    }

    return null;
}

/**
 * Returns configuration for the "remove from index" action.
 *
 * @returns {{url: string, buttonTitleKey: string, tooltip: string}|null}
 * @private
 */
function _getRemoveIndexButtonConf() {
    const entityType = getActiveTabValue();
    if ("document" === entityType) {
        const url = _addParamsBtnUrl("/admin/v9/settings/doc-chunks/?action=delete");
        return {
            url: url.pathname + url.search,
            title: "[[#{settings.doc-chunks.title}]]",
            buttonTitleKey: "[[#{settings.embedding-chunks.start}]]",
            tooltip: "[[#{settings.embedding-chunks.remove}]]"
        };
    }

    return null;
}

/**
 * Opens an iframe modal for indexing actions.
 *
 * @param {{url: string, buttonTitleKey: string}|null} conf
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
            if (typeof $ === "function") {
                $("#modalIframeIframeElement").contents().find("button#submitBtn").click();
            }
            return false;
        }
    });
}

/**
 * Applies row-selection visibility rule when DataTables helper exists.
 *
 * @param {object} buttonContext
 * @param {object} dt
 * @private
 */
function _initRowUnselectedVisibility(buttonContext, dt) {
    if (
        typeof $ === "function" &&
        $.fn &&
        $.fn.dataTable &&
        $.fn.dataTable.Buttons &&
        typeof $.fn.dataTable.Buttons.showIfRowUnselected === "function"
    ) {
        $.fn.dataTable.Buttons.showIfRowUnselected(buttonContext, dt);
    }
}

function getAddIndexButton() {
    const conf = _getAddIndexButtonConf();

    return {
        text: "<i class=\"ti ti-database-plus\"></i>",
        action: function () {
            const clickConf = _getAddIndexButtonConf();
            _openIndexModal(clickConf);
        },
        init: function (dt) {
            _initRowUnselectedVisibility(this, dt);
        },
        className: "btn btn-sm btn-success",
        attr: {
            title: conf ? conf.tooltip : "",
            "data-toggle": "tooltip"
        }
    };
}

function getRemoveIndexButton() {
    const conf = _getRemoveIndexButtonConf();

    return {
        text: "<i class=\"ti ti-database-minus\"></i>",
        action: function () {
            const clickConf = _getRemoveIndexButtonConf();
            _openIndexModal(clickConf);
        },
        init: function (dt) {
            _initRowUnselectedVisibility(this, dt);
        },
        className: "btn btn-sm btn-danger",
        attr: {
            title: conf ? conf.tooltip : "",
            "data-toggle": "tooltip"
        }
    };
}

async function initPage() {
    // First set filter
    await addFilterBasedOnTab();

    // Tehn call filter
    filterFn();
}