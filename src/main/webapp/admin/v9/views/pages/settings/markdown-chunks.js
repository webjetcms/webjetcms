const markdownParams = new URLSearchParams(window.location.search);
const markdownAction = markdownParams.get("action") === "delete" ? "delete" : "index";
const markdownFolderSelector = new MarkdownFolderSelector(document.getElementById("markdown-folder-selector"), markdownSelectionChanged);
const markdownFolder = document.getElementById("markdownFolder");
const markdownDirectory = document.getElementById("markdownDirectory");
const markdownIncludeSubfolders = document.getElementById("markdownIncludeSubfolders");
const markdownSubmit = document.getElementById("submitBtn");
const markdownSuccessMessage = document.getElementById("succ-msg-" + markdownAction);
const markdownSuccessText = markdownSuccessMessage.textContent;
const markdownRestUrl = "/admin/rest/settings/embedding-chunks";
let markdownStatsRequest = 0;
let markdownSubmitting = false;

markdownIncludeSubfolders.checked = markdownParams.get("includeSubfolders") !== "false";

document.getElementById("action-title-" + markdownAction).classList.remove("d-none");
document.getElementById("queued-label-" + markdownAction).classList.remove("d-none");
if (markdownAction === "index") document.getElementById("markdown-language-help").classList.remove("d-none");

/**
 * Retrieves queue statistics for the selected documentation folder.
 * Stale responses are ignored when the selection changes during a request.
 *
 * @returns {Promise<void>}
 */
async function getStats() {
    const request = ++markdownStatsRequest;
    markdownSubmit.disabled = true;
    document.getElementById("stats-failed").classList.add("d-none");
    ["allDoc", "indexedDoc", "queuedDoc"].forEach(id => document.getElementById(id).textContent = "—");
    if (markdownSubmitting || !markdownFolder.value) return;

    const params = getMarkdownScopeParams();
    try {
        const response = await fetch(markdownRestUrl + "/markdown-stat?" + params.toString(), {
            headers: { "X-CSRF-Token": window.csrfToken }
        });
        if (!response.ok) throw new Error("HTTP " + response.status);
        const data = await response.json();
        if (request !== markdownStatsRequest) return;
        document.getElementById("allDoc").textContent = data.totalDocuments;
        document.getElementById("indexedDoc").textContent = data.indexedDocuments;
        document.getElementById("queuedDoc").textContent = data.queuedDocuments;
        markdownSubmit.disabled = false;
    } catch (error) {
        if (request !== markdownStatsRequest) return;
        document.getElementById("stats-failed").classList.remove("d-none");
        console.error("Failed to load Markdown indexing statistics:", error);
    }
}

/**
 * Queues the selected folder without performing indexing in the HTTP request.
 *
 * @returns {Promise<void>}
 */
async function handleMarkdownIndexing() {
    if (markdownSubmitting || markdownSubmit.disabled || !markdownFolder.value) return;
    markdownSubmitting = true;
    markdownSubmit.disabled = true;
    markdownFolderSelector.disabled = true;
    markdownIncludeSubfolders.disabled = true;
    const waitMessage = document.getElementById("wait-msg");
    markdownSuccessMessage.classList.add("d-none");
    document.getElementById("fail-msg").classList.add("d-none");
    waitMessage.classList.remove("d-none");

    const params = getMarkdownScopeParams();
    try {
        const response = await fetch(markdownRestUrl + "/markdown-action", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded",
                "X-CSRF-Token": window.csrfToken
            },
            body: params.toString()
        });
        if (!response.ok) throw new Error("HTTP " + response.status);
        const count = await response.json();
        if (!Number.isInteger(count) || count < 0) throw new Error("Invalid queued document count");
        markdownSuccessMessage.textContent = markdownSuccessText + " (" + count + ")";
        markdownSuccessMessage.classList.remove("d-none");
    } catch (error) {
        document.getElementById("fail-msg").classList.remove("d-none");
        console.error("Failed to queue Markdown indexing:", error);
    } finally {
        markdownSubmitting = false;
        waitMessage.classList.add("d-none");
        markdownFolderSelector.disabled = false;
        markdownIncludeSubfolders.disabled = false;
        await getStats();
    }
}

/**
 * Builds the shared statistics and queue parameters for the selected directory scope.
 *
 * @returns {URLSearchParams}
 */
function getMarkdownScopeParams() {
    return new URLSearchParams({
        folder: markdownFolder.value,
        directory: markdownDirectory.value,
        includeSubfolders: String(markdownIncludeSubfolders.checked),
        action: markdownAction.toUpperCase()
    });
}

/**
 * Restores the folder selection from URL parameters and loads statistics if a root is available.
 *
 * @returns {Promise<void>} Resolves after initialization and any initial statistics request finish.
 */
async function initMarkdownDialog() {
    const message = document.getElementById("folder-message");
    const initialized = await markdownFolderSelector.initialize(markdownParams.get("folder"), markdownParams.get("directory"), message);
    if (!initialized) return;

    await getStats();
}

markdownSubmit.addEventListener("click", handleMarkdownIndexing);
function markdownSelectionChanged() {
    markdownSuccessMessage.classList.add("d-none");
    document.getElementById("fail-msg").classList.add("d-none");
    getStats();
}
markdownIncludeSubfolders.addEventListener("change", markdownSelectionChanged);
initMarkdownDialog();
