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
        this.height = 800;
        this.width = 0;
        this.tileSize = 1024;
        this.nodes = {};
        ["Width", "Scale", "Opacity", "OpacityValue", "Visible", "Reload", "Frame", "Viewport", "Stage", "StageSize", "Overlay", "Tiles", "Status", "Notice", "Version", "Title", "Url", "Period", "Back"].forEach(name => {
            this.nodes[name] = element.querySelector("#heatMap" + name);
        });
    }

    async init() {
        this.nodes.Back.href += "?dateRange=" + encodeURIComponent(this.dateRange);
        const dates = this.dateRange.replace(/^daterange:/, "").split("-");
        this.nodes.Period.textContent = dates.filter(value => Number(value) > 0).map(value => WJ.formatDate(Number(value))).join(" – ");
        this.nodes.Width.addEventListener("change", () => this.loadWidth());
        this.nodes.Reload.addEventListener("click", () => this.loadWidth());
        this.nodes.Scale.addEventListener("change", () => this.resize());
        this.nodes.Opacity.addEventListener("input", () => {
            this.nodes.OpacityValue.textContent = this.nodes.Opacity.value + " %";
            this.nodes.Overlay.style.opacity = Number(this.nodes.Opacity.value) / 100;
        });
        this.nodes.Visible.addEventListener("change", () => {
            this.nodes.Overlay.hidden = !this.nodes.Visible.checked;
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
        try {
            const widths = await this.getJson("/widths");
            for (const item of widths) {
                const option = document.createElement("option");
                option.value = item.width;
                option.textContent = item.width + " px · " + this.texts.clicks + ": " + item.clicks.toLocaleString();
                this.nodes.Width.append(option);
            }
            if (widths.length === 0) {
                this.status(this.texts.empty);
                return;
            }
            const requestedWidth = this.params.get("width");
            if (widths.some(item => String(item.width) === requestedWidth)) this.nodes.Width.value = requestedWidth;
            this.nodes.Width.disabled = false;
            await this.loadWidth();
        } catch (error) {
            if (error.name !== "AbortError") this.status(this.texts.error, true);
        }
    }

    endpoint(path, extra = {}) {
        const params = new URLSearchParams({docId: this.docId, dateRange: this.dateRange, ...extra});
        return this.api + path + "?" + params.toString();
    }

    async getJson(path, extra = {}) {
        const response = await fetch(this.endpoint(path, extra), {
            credentials: "same-origin", headers: {Accept: "application/json"}, signal: this.requestController.signal
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

    async loadWidth() {
        const revision = ++this.revision;
        this.requestController.abort();
        this.requestController = new AbortController();
        clearTimeout(this.previewTimer);
        this.frameReady = false;
        this.nodes.Viewport.classList.add("d-none");
        this.nodes.Reload.disabled = true;
        this.nodes.Notice.classList.add("d-none");
        this.nodes.Version.textContent = "";
        this.clearTiles();
        this.status(this.texts.loading);
        this.width = Number(this.nodes.Width.value);
        try {
            const metadata = await this.getJson("/metadata", {width: this.width});
            if (revision !== this.revision) return;
            this.nodes.Title.textContent = metadata.title;
            this.nodes.Url.textContent = metadata.url;
            this.tileSize = metadata.tileSize;
            const notes = [];
            if (metadata.historicalUnavailable && metadata.source === "current") notes.push(this.texts.fallback);
            if (metadata.multipleVersions) notes.push(this.texts.multiple);
            this.nodes.Notice.textContent = notes.join(" ");
            this.nodes.Notice.classList.toggle("d-none", notes.length === 0);
            if (metadata.source === "unavailable") {
                this.status(this.texts.unavailable, true);
                return;
            }
            this.nodes.Version.textContent = (metadata.source === "history"
                ? this.texts.historical + " " + WJ.formatDateTime(metadata.effectiveFrom)
                : this.texts.current) + " · " + this.texts.clicks + ": " + metadata.clicks.toLocaleString();
            this.nodes.Frame.style.width = this.width + "px";
            this.nodes.Stage.style.width = this.width + "px";
            this.nodes.Stage.style.height = this.height + "px";
            this.nodes.Frame.src = this.endpoint("/preview", {previewRevision: revision});
            this.previewTimer = setTimeout(() => {
                if (revision === this.revision && !this.frameReady) this.status(this.texts.previewError, true);
            }, 30000);
            this.params.set("width", this.width);
            history.replaceState(null, "", "?" + this.params.toString());
        } catch (error) {
            if (revision === this.revision) this.status(this.texts.error, true);
        } finally {
            if (revision === this.revision) this.nodes.Reload.disabled = false;
        }
    }

    frameLoaded() {
        if (!this.nodes.Frame.getAttribute("src")) return;
        try {
            const frameWindow = this.nodes.Frame.contentWindow;
            const doc = frameWindow.document;
            const loadedUrl = new URL(frameWindow.location.href);
            if (loadedUrl.pathname === this.api + "/preview" && loadedUrl.searchParams.get("previewRevision") !== String(this.revision)) return;
            clearTimeout(this.previewTimer);
            if (doc.querySelector('meta[name="webjet-heatmap-preview"]')?.content !== String(this.docId)) {
                throw new Error("Unexpected preview document");
            }
            this.frameReady = true;
            this.nodes.Viewport.classList.remove("d-none");
            this.status("");
            frameWindow.addEventListener("scroll", () => this.scheduleTiles(), {passive: true});
            frameWindow.addEventListener("resize", () => this.scheduleTiles());
            doc.addEventListener("click", event => event.preventDefault(), true);
            doc.addEventListener("submit", event => event.preventDefault(), true);
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
            ? Math.min(1, Math.max(1, this.nodes.Viewport.clientWidth - 30) / this.width)
            : Number(this.nodes.Scale.value);
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

    updateTiles() {
        if (!this.frameReady || !this.nodes.Visible.checked) return;
        const frameWindow = this.nodes.Frame.contentWindow;
        const doc = frameWindow.document.documentElement;
        const left = frameWindow.scrollX;
        const top = frameWindow.scrollY;
        this.nodes.Overlay.style.width = doc.clientWidth + "px";
        this.nodes.Overlay.style.height = doc.clientHeight + "px";
        this.nodes.Tiles.style.transform = "translate(" + -left + "px," + -top + "px)";
        const visible = new Set();
        const lastX = Math.floor((Math.min(doc.scrollWidth, left + doc.clientWidth) - 1) / this.tileSize);
        const lastY = Math.floor((Math.min(doc.scrollHeight, top + doc.clientHeight) - 1) / this.tileSize);
        for (let y = Math.floor(top / this.tileSize); y <= lastY; y++) {
            for (let x = Math.floor(left / this.tileSize); x <= lastX; x++) {
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
                tile.src = this.endpoint("/tile", {width: this.width, tileX: x, tileY: y});
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
        this.contentObserver?.disconnect();
    }

    destroy() {
        this.revision++;
        this.requestController.abort();
        clearTimeout(this.previewTimer);
        cancelAnimationFrame(this.animationFrame);
        this.resizeObserver.disconnect();
        this.clearTiles();
    }
}
