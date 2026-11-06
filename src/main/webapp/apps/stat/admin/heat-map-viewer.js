/** Uses the same conservative declaration check as the standalone public-page tracker. */
function hasFixedPixelWidth(container, property, resolvedWidth) {
    const doc = container.ownerDocument;
    const view = doc.defaultView;
    let found = false;
    function declaration(style) {
        const value = style.getPropertyValue(property).trim();
        if (style.getPropertyValue("all")) return false;
        if (!value) return true;
        found ||= Math.abs(parseFloat(value) - resolvedWidth) <= 0.5;
        return /^\d+(?:\.\d+)?px$/.test(value);
    }
    function rules(list) {
        for (const rule of list) {
            if (rule.media && !view.matchMedia(rule.media.mediaText).matches) continue;
            if (rule.constructor.name === "CSSSupportsRule" && !view.CSS.supports(rule.conditionText)) continue;
            if (rule.selectorText && rule.style && (rule.style.getPropertyValue(property) || rule.style.getPropertyValue("all"))) {
                if (rule.selectorText.includes("&") || (container.matches(rule.selectorText) && !declaration(rule.style))) return false;
            }
            if (rule.styleSheet && !rules(rule.styleSheet.cssRules)) return false;
            if (rule.cssRules && !rules(rule.cssRules)) return false;
        }
        return true;
    }
    try {
        if (!declaration(container.style)) return false;
        for (const sheet of [...doc.styleSheets, ...(doc.adoptedStyleSheets || [])]) {
            if (sheet.disabled || (sheet.media.mediaText && !view.matchMedia(sheet.media.mediaText).matches)) continue;
            if (!rules(sheet.cssRules)) return false;
        }
        return found;
    } catch (error) {
        return false;
    }
}

/** Displays click-map tiles over an independently scrolling page preview. */
export class HeatMapViewer {
    constructor(element, texts) {
        this.element = element;
        this.texts = texts;
        this.api = "/admin/rest/stat/heat-map";
        this.params = new URLSearchParams(window.location.search);
        this.docId = Number(this.params.get("docId"));
        this.dateRange = this.params.get("dateRange") || "";
        this.tiles = new Map();
        this.revision = 0;
        this.requestController = new AbortController();
        this.frameReady = false;
        this.previewOnly = false;
        this.reportWidths = [];
        this.height = 800;
        this.width = 0;
        this.groupWidth = 0;
        this.mode = "DOCUMENT";
        this.layoutValid = true;
        this.tileSize = 1024;
        this.nodes = {};
        ["Version", "Width", "Scale", "Opacity", "OpacityValue", "Visible", "Reload", "Frame", "Viewport", "Stage", "StageSize", "Overlay", "Tiles", "Status", "Title", "Back"].forEach(name => {
            this.nodes[name] = element.querySelector("#heatMap" + name);
        });
        this.closeOnEscape = event => {
            if (event.key === "Escape" && window !== window.parent) {
                event.preventDefault();
                window.parent.WJ.closeIframeModal();
            }
        };
    }

    async init() {
        this.nodes.Back.href += "?dateRange=" + encodeURIComponent(this.dateRange);
        document.addEventListener("keydown", this.closeOnEscape);
        this.nodes.Version.addEventListener("change", () => this.loadWidth(true));
        this.nodes.Width.addEventListener("change", () => this.loadWidth());
        this.nodes.Reload.addEventListener("click", () => this.loadWidth(true));
        this.nodes.Scale.addEventListener("change", () => this.resize());
        this.nodes.Opacity.addEventListener("input", () => {
            this.nodes.OpacityValue.textContent = this.nodes.Opacity.value + " %";
            this.nodes.Overlay.style.opacity = Number(this.nodes.Opacity.value) / 100;
        });
        this.nodes.Visible.addEventListener("change", () => {
            this.nodes.Overlay.hidden = !this.nodes.Visible.checked || !this.layoutValid;
            this.nodes.Opacity.disabled = !this.nodes.Visible.checked || !this.layoutValid;
            this.scheduleTiles();
        });
        this.nodes.Frame.addEventListener("load", () => this.frameLoaded());
        this.resizeObserver = new ResizeObserver(() => this.resize());
        this.resizeObserver.observe(this.nodes.Viewport);
        window.addEventListener("pagehide", event => {
            if (!event.persisted) this.destroy();
        });
        if (!Number.isSafeInteger(this.docId) || this.docId < 1) {
            this.status(this.texts.error, true);
            return;
        }
        await this.loadWidth(true);
    }

    setWidths(widths) {
        const reportWidth = this.params.get("width");
        const reportGroup = this.params.get("mode") === "CONTAINER" ? "container:" + reportWidth : reportWidth;
        const requestedValue = (this.nodes.Version.value && this.nodes.Width.value) || reportGroup;
        this.nodes.Width.replaceChildren();
        for (const item of widths) {
            const option = document.createElement("option");
            option.value = item.mode === "CONTAINER" ? "container:" + item.width : String(item.width);
            option.dataset.width = item.width;
            option.dataset.previewWidth = item.previewWidth || item.width;
            option.dataset.mode = item.mode || "DOCUMENT";
            const label = item.mode === "CONTAINER" ? this.texts.container : this.texts.window;
            const clicks = this.previewOnly ? 0 : item.clicks;
            option.textContent = label + " " + item.width + " px · " + this.texts.clicks + ": " + clicks.toLocaleString();
            this.nodes.Width.append(option);
        }
        if ([...this.nodes.Width.options].some(option => option.value === requestedValue)) this.nodes.Width.value = requestedValue;
        this.nodes.Width.disabled = widths.length === 0;
    }

    setVersions(metadata) {
        const versions = metadata.versions || [];
        const select = this.nodes.Version;
        const selectedId = select.value;
        select.replaceChildren(select.options[0]);
        for (const version of versions) {
            select.add(new Option("#" + version.historyId + " · " + WJ.formatDateTimeSeconds(version.effectiveFrom), version.historyId));
        }
        select.value = selectedId;
        const onlyRange = versions.length === 1 ? versions[0].range : null;
        const coversReport = onlyRange && onlyRange.from === metadata.clickRange?.from && onlyRange.until === metadata.clickRange?.until;
        select.parentElement.hidden = !selectedId && (versions.length === 0 || (metadata.source === "history" && coversReport));
    }

    endpoint(path, extra = {}) {
        const params = new URLSearchParams({docId: this.docId, dateRange: this.dateRange, ...extra});
        if (this.nodes.Version.value) params.set("versionId", this.nodes.Version.value);
        return this.api + path + "?" + params.toString();
    }

    async getJson(path, extra = {}) {
        const response = await fetch(this.endpoint(path, extra), {
            credentials: "same-origin", headers: {Accept: "application/json", "X-CSRF-Token": window.csrfToken}, signal: this.requestController.signal
        });
        if (!response.ok) throw new Error("Heat map request failed");
        return response.json();
    }

    status(message, error = false) {
        this.nodes.Status.textContent = message;
        this.nodes.Status.className = "alert " + (error ? "alert-danger" : "alert-info");
        this.nodes.Status.classList.toggle("d-none", !message);
        this.element.setAttribute("aria-busy", String(message === this.texts.loading));
    }

    async loadWidth(refreshWidths = false) {
        const revision = ++this.revision;
        this.requestController.abort();
        this.requestController = new AbortController();
        clearTimeout(this.previewTimer);
        this.frameReady = false;
        this.contentObserver?.disconnect();
        this.nodes.Viewport.classList.add("d-none");
        this.nodes.Reload.disabled = true;
        this.clearTiles();
        this.status(this.texts.loading);
        try {
            if (refreshWidths) {
                this.nodes.Width.disabled = true;
                const widths = await this.getJson("/widths");
                if (revision !== this.revision) return;
                if (!this.nodes.Version.value) this.reportWidths = widths;
                this.previewOnly = Boolean(this.nodes.Version.value) && widths.length === 0;
                const availableWidths = this.previewOnly ? this.reportWidths : widths;
                this.setWidths(availableWidths);
                if (availableWidths.length === 0) {
                    this.status(this.texts.empty);
                    return;
                }
            }
            const selected = this.nodes.Width.selectedOptions[0];
            this.groupWidth = Number(selected.dataset.width);
            this.mode = selected.dataset.mode;
            if (this.previewOnly) {
                this.width = Number(selected.dataset.previewWidth);
                this.openPreview(revision);
                return;
            }
            if (!this.nodes.Version.value) {
                this.params.set("width", this.groupWidth);
                this.params.set("mode", this.mode);
                history.replaceState(null, "", "?" + this.params.toString());
            }
            const metadata = await this.getJson("/metadata", {width: this.groupWidth, mode: this.mode});
            if (revision !== this.revision) return;
            this.setVersions(metadata);
            this.nodes.Title.textContent = metadata.title;
            this.tileSize = metadata.tileSize;
            this.width = metadata.previewWidth || this.groupWidth;
            this.containerSelector = metadata.containerSelector || "";
            this.layoutValid = true;
            const notes = [];
            if (metadata.historicalUnavailable && metadata.source === "current") notes.push(this.texts.fallback);
            if (metadata.multipleVersions) notes.push(this.texts.multiple);
            if (notes.length > 0) WJ.notifyWarning(this.texts.title, notes.join(" "), 5000);
            if (metadata.source === "unavailable") {
                this.status(this.texts.unavailable, true);
                return;
            }
            this.openPreview(revision);
        } catch (error) {
            if (revision === this.revision) this.status(this.texts.error, true);
        } finally {
            if (revision === this.revision) this.nodes.Reload.disabled = false;
        }
    }

    openPreview(revision) {
        this.nodes.Visible.disabled = this.previewOnly;
        this.nodes.Opacity.disabled = this.previewOnly || !this.nodes.Visible.checked;
        this.nodes.Overlay.hidden = this.previewOnly || !this.nodes.Visible.checked;
        this.nodes.Frame.style.width = this.width + "px";
        this.nodes.Stage.style.width = this.width + "px";
        this.nodes.Stage.style.height = this.height + "px";
        this.nodes.Frame.src = this.endpoint("/html/preview", {previewRevision: revision});
        this.previewTimer = setTimeout(() => {
            if (revision === this.revision && !this.frameReady) this.status(this.texts.previewError, true);
        }, 30000);
    }

    frameLoaded() {
        if (!this.nodes.Frame.getAttribute("src")) return;
        try {
            const frameWindow = this.nodes.Frame.contentWindow;
            const doc = frameWindow.document;
            const loadedUrl = new URL(frameWindow.location.href);
            if (loadedUrl.pathname === this.api + "/html/preview" && loadedUrl.searchParams.get("previewRevision") !== String(this.revision)) return;
            clearTimeout(this.previewTimer);
            if (doc.querySelector('meta[name="webjet-heatmap-preview"]')?.content !== String(this.docId)) {
                throw new Error("Unexpected preview document");
            }
            this.frameReady = true;
            this.nodes.Viewport.classList.remove("d-none");
            this.status(this.previewOnly ? this.texts.emptyVersion : "");
            frameWindow.addEventListener("scroll", () => this.scheduleTiles(), {passive: true});
            frameWindow.addEventListener("resize", () => this.scheduleTiles());
            // A header can finish sliding into place after the last scroll or resize event.
            doc.addEventListener("transitionend", () => this.scheduleTiles());
            doc.addEventListener("animationend", () => this.scheduleTiles());
            doc.addEventListener("click", event => event.preventDefault(), true);
            doc.addEventListener("submit", event => event.preventDefault(), true);
            doc.addEventListener("keydown", this.closeOnEscape);
            this.contentObserver?.disconnect();
            this.contentObserver = new ResizeObserver(() => this.scheduleTiles());
            this.contentObserver.observe(doc.documentElement);
            this.resize();
        } catch (error) {
            clearTimeout(this.previewTimer);
            this.frameReady = false;
            this.nodes.Viewport.classList.add("d-none");
            this.status(this.texts.previewError, true);
        }
    }

    resize() {
        if (!this.frameReady) return;
        const scale = this.nodes.Scale.value === "fit"
            ? Math.min(1, Math.max(1, this.nodes.Viewport.clientWidth - 10) / this.width)
            : Number(this.nodes.Scale.value);
        this.height = Math.max(1, Math.floor((this.nodes.Viewport.clientHeight - 10) / scale));
        this.nodes.Frame.style.height = this.height + "px";
        this.nodes.Stage.style.height = this.height + "px";
        this.nodes.Stage.style.transform = "scale(" + scale + ")";
        this.nodes.StageSize.style.width = this.width * scale + "px";
        this.nodes.StageSize.style.height = this.height * scale + "px";
        this.scheduleTiles();
    }

    scheduleTiles() {
        if (this.animationFrame) return;
        this.animationFrame = requestAnimationFrame(() => {
            this.animationFrame = null;
            this.updateTiles();
        });
    }

    /** Finds the common document offset only while the recorded container layout still exists. */
    containerOffset(frameWindow) {
        if (this.mode !== "CONTAINER") return 0;
        if (!this.containerSelector) return null;
        const doc = frameWindow.document.documentElement;
        if (doc.scrollWidth > doc.clientWidth) return null;
        let containers;
        try { containers = frameWindow.document.querySelectorAll(this.containerSelector); } catch (error) { return null; }
        for (const container of containers) {
            const rect = container.getBoundingClientRect();
            const style = frameWindow.getComputedStyle(container);
            const left = rect.left + frameWindow.scrollX;
            if (rect.height <= 0 || Math.round(rect.width) !== this.groupWidth || style.animationName !== "none"
                    || Math.abs(left - (doc.clientWidth - rect.width) / 2) > 0.5) continue;
            const extra = style.boxSizing === "border-box" ? 0 : parseFloat(style.paddingLeft) + parseFloat(style.paddingRight)
                + parseFloat(style.borderLeftWidth) + parseFloat(style.borderRightWidth);
            const fixed = ["max-width", "width"].some(property => {
                const value = style.getPropertyValue(property);
                return value.endsWith("px") && Math.abs(rect.width - parseFloat(value) - extra) <= 0.5
                    && hasFixedPixelWidth(container, property, parseFloat(value));
            });
            if (fixed) return left;
        }
        return null;
    }

    /** Cuts pinned header rectangles out of the overlay in unscaled iframe viewport coordinates. */
    headerMask(frameWindow) {
        if (frameWindow.scrollY === 0) return "";
        const doc = frameWindow.document;
        const headers = [...doc.querySelectorAll('header, nav, [role="banner"], [role="navigation"]')];
        // A template can also pin an unlabelled wrapper at the top of the page.
        headers.push(doc.elementFromPoint(doc.documentElement.clientWidth / 2, 0));

        const candidates = new Set();
        for (const header of headers) {
            for (let element = header; element && !candidates.has(element); element = element.parentElement) {
                candidates.add(element);
            }
        }
        const cutouts = [];
        for (const element of candidates) {
            const style = frameWindow.getComputedStyle(element);
            if (style.position !== "fixed" && style.position !== "sticky") continue;
            const rect = element.getBoundingClientRect();
            if (style.position === "sticky" && (style.top === "auto" || rect.top > parseFloat(style.top) + 0.5)) continue;
            if (style.visibility !== "visible" || style.opacity === "0" || !rect.width || !rect.height) continue;
            cutouts.push(`linear-gradient(black, black) ${rect.left}px ${rect.top}px / ${rect.width}px ${rect.height}px no-repeat`);
        }
        return cutouts.length ? "linear-gradient(black, black) subtract, " + cutouts.join(", ") : "";
    }

    updateTiles() {
        if (!this.frameReady || this.previewOnly) return;
        const frameWindow = this.nodes.Frame.contentWindow;
        const offset = this.containerOffset(frameWindow);
        const wasValid = this.layoutValid;
        this.layoutValid = offset !== null;
        this.nodes.Overlay.hidden = !this.nodes.Visible.checked || !this.layoutValid;
        this.nodes.Opacity.disabled = !this.nodes.Visible.checked || !this.layoutValid;
        if (!this.layoutValid) {
            this.clearTiles();
            this.status(this.texts.containerMissing.replace("{width}", this.groupWidth), true);
            return;
        }
        if (!wasValid) this.status("");
        if (!this.nodes.Visible.checked) return;
        const doc = frameWindow.document.documentElement;
        const left = frameWindow.scrollX;
        const top = frameWindow.scrollY;
        this.nodes.Overlay.style.width = doc.clientWidth + "px";
        this.nodes.Overlay.style.height = doc.clientHeight + "px";
        this.nodes.Overlay.style.mask = this.headerMask(frameWindow);
        this.nodes.Tiles.style.transform = "translate(" + (offset - left) + "px," + -top + "px)";
        const visible = new Set();
        const mapWidth = this.mode === "CONTAINER" ? this.groupWidth : doc.scrollWidth;
        const lastX = Math.floor((Math.min(mapWidth, left + doc.clientWidth - offset) - 1) / this.tileSize);
        const lastY = Math.floor((Math.min(doc.scrollHeight, top + doc.clientHeight) - 1) / this.tileSize);
        for (let y = Math.floor(top / this.tileSize); y <= lastY; y++) {
            for (let x = Math.floor(Math.max(0, left - offset) / this.tileSize); x <= lastX; x++) {
                const key = x + ":" + y;
                visible.add(key);
                if (this.tiles.has(key)) continue;
                const tile = document.createElement("img");
                tile.alt = "";
                tile.width = this.tileSize;
                tile.height = this.tileSize;
                tile.style.left = x * this.tileSize + "px";
                tile.style.top = y * this.tileSize + "px";
                tile.dataset.tile = key;
                const revision = this.revision;
                tile.addEventListener("error", () => {
                    if (revision === this.revision && tile.isConnected) this.status(this.texts.tileError, true);
                }, {once: true});
                tile.src = this.endpoint("/binary/tile", {width: this.groupWidth, mode: this.mode, tileX: x, tileY: y});
                this.tiles.set(key, tile);
                this.nodes.Tiles.append(tile);
            }
        }
        for (const [key, tile] of this.tiles) {
            if (!visible.has(key)) {
                tile.removeAttribute("src");
                tile.remove();
                this.tiles.delete(key);
            }
        }
    }

    clearTiles() {
        for (const tile of this.tiles.values()) tile.removeAttribute("src");
        this.nodes.Tiles.replaceChildren();
        this.tiles.clear();
    }

    destroy() {
        this.revision++;
        this.requestController.abort();
        clearTimeout(this.previewTimer);
        cancelAnimationFrame(this.animationFrame);
        this.resizeObserver.disconnect();
        this.contentObserver?.disconnect();
        document.removeEventListener("keydown", this.closeOnEscape);
        this.clearTiles();
    }
}
