/**
 * Uses the shared directory tree picker to select a configured Markdown root or its subdirectory.
 * The configured root is kept separate from the relative directory used to scope indexing.
 */
class MarkdownFolderSelector {
    /**
     * Appends the tree picker and backing inputs, initially disabling selection.
     *
     * @param {HTMLElement} container Element that hosts the picker and backing inputs.
     * @param {function(): void} onChange Callback invoked without arguments after selection inputs are updated; its return value is ignored.
     */
    constructor(container, onChange) {
        this.folderInput = document.createElement("input");
        this.folderInput.id = "markdownFolder";
        this.folderInput.type = "hidden";
        this.directoryInput = document.createElement("input");
        this.directoryInput.id = "markdownDirectory";
        this.directoryInput.type = "hidden";
        this.selectionInput = document.createElement("input");
        this.selectionInput.id = "markdownFolderSelection";
        this.selectionInput.type = "hidden";
        this.selectionInput.setAttribute("aria-label", "[[#{settings.embedding-chunks.sourceRoot}]]");
        this.tree = document.createElement("webjet-dte-jstree");
        this.tree.id = "markdownFolderTree";
        this.tree.configure({
            inputElement: this.selectionInput,
            mode: "dt-tree-universal",
            attributes: { "data-dt-field-dt-url": "/admin/rest/settings/embedding-chunks/markdown-folders" },
            value: { fullPath: "" }
        });
        this.tree.addEventListener("webjet-component-ready", () => { this.disabled = this._disabled; });
        container.append(this.folderInput, this.directoryInput, this.selectionInput, this.tree);
        this.disabled = true;

        $(this.selectionInput).on("change", () => {
            const selected = this.tree.getValue()[0];
            this.folderInput.value = selected?.sourceRoot || "";
            this.directoryInput.value = selected?.directory || "";
            onChange();
        });
    }

    /**
     * Loads configured roots and restores an optional selected directory.
     * Falls back to the first root with an empty directory if the requested root is unknown.
     * Enables selection on success and reports missing roots or loading failures in message.
     *
     * @param {string|null} folder Configured root to restore.
     * @param {string|null} directory Root-relative selected directory.
     * @param {HTMLElement} message Element used for loading failures or missing configuration.
     * @returns {Promise<boolean>} Whether an available root was selected.
     */
    async initialize(folder, directory, message) {
        try {
            const response = await fetch("/admin/rest/settings/embedding-chunks/markdown-folders", {
                headers: { "X-CSRF-Token": window.csrfToken }
            });
            if (!response.ok) throw new Error("HTTP " + response.status);
            const folders = await response.json();
            if (folders.length === 0) {
                message.textContent = "[[#{settings.markdown-chunks.noFolders}]]";
                return false;
            }
            const knownRoot = folders.includes(folder);
            this.folderInput.value = knownRoot ? folder : folders[0];
            this.directoryInput.value = knownRoot ? directory || "" : "";
            this.tree.setValue({
                sourceRoot: this.folderInput.value,
                directory: this.directoryInput.value,
                fullPath: this.folderInput.value + (this.directoryInput.value ? "/" + this.directoryInput.value : "")
            });
            this.disabled = false;
            return true;
        } catch (error) {
            message.textContent = "[[#{settings.markdown-chunks.foldersFailed}]]";
            console.error("Failed to load Markdown folders:", error);
            return false;
        }
    }

    /**
     * Sets the disabled state of the backing scope inputs and the tree's current buttons.
     * The stored state is reapplied when the tree component becomes ready.
     *
     * @param {boolean} disabled - Whether folder selection is disabled.
     */
    set disabled(disabled) {
        this._disabled = disabled;
        this.folderInput.disabled = disabled;
        this.directoryInput.disabled = disabled;
        this.tree.querySelectorAll("button").forEach(button => { button.disabled = disabled; });
    }
}
