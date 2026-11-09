import { HeatMapArea } from "./heat-map-area.js";

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

/** Coordinates one or two page previews with shared width, zoom and overlay controls. */
export class HeatMapViewer {
    constructor(element, texts) {
        this.element = element;
        this.texts = texts;
        this.api = "/admin/rest/stat/heat-map";
        this.params = new URLSearchParams(window.location.search);
        this.docId = Number(this.params.get("docId"));
        this.dateRange = this.params.get("dateRange") || "";
        this.revision = 0;
        this.requestController = new AbortController();
        this.reportWidths = [];
        this.comparing = false;
        this.selectingArea = false;
        this.overlayVisible = true;
        const firstPane = element.querySelector("#heatMapPane");
        const secondPane = firstPane.cloneNode(true);
        secondPane.id = "heatMapComparePane";
        secondPane.querySelectorAll("[id]").forEach(node => { node.id = node.id.replace("heatMap", "heatMapCompare"); });
        secondPane.querySelectorAll("[for]").forEach(node => { node.htmlFor = node.htmlFor.replace("heatMap", "heatMapCompare"); });
        secondPane.hidden = true;
        element.querySelector("#heatMapPreviews").append(secondPane);
        this.nodes = {};
        ["Version", "CompareVersion", "Compare", "Width", "Scale", "Opacity", "OpacityValue", "Visible", "Reload", "Title", "Back", "SelectArea"].forEach(name => {
            this.nodes[name] = element.querySelector("#heatMap" + name);
        });
        this.previews = [new HeatMapPreview(this, firstPane, "heatMap"), new HeatMapPreview(this, secondPane, "heatMapCompare")];
        this.closeOnEscape = event => {
            if (event.key !== "Escape") return;
            const tooltips = $(this.element).find('.pg-heatmap-icon[aria-describedby]').filter((index, node) =>
                document.getElementById(node.getAttribute("aria-describedby"))?.classList.contains("show"));
            if (tooltips.length) {
                event.preventDefault();
                tooltips.tooltip("hide");
                return;
            }
            if (this.selectingArea) {
                event.preventDefault();
                this.setAreaSelection(false);
                WJ.focusWithoutTooltip(this.nodes.SelectArea);
                return;
            }
            if (window !== window.parent) {
                event.preventDefault();
                window.parent.WJ.closeIframeModal();
            }
        };
    }

    async init() {
        this.nodes.Back.href += "?dateRange=" + encodeURIComponent(this.dateRange);
        const iconButtons = $(this.element).find(".pg-heatmap-icon");
        WJ.initTooltip(iconButtons);
        // Let Escape dismiss a visible tooltip before cancelling selection or closing the dialog.
        iconButtons.off("keydown.wjTooltipA11y");
        document.addEventListener("keydown", this.closeOnEscape);
        [this.nodes.Version, this.nodes.CompareVersion].forEach((select, index) => {
            select.addEventListener("change", () => {
                this.updateVersionOptions();
                if (!this.comparing) return this.loadWidth(true);
                const preview = this.previews[index];
                preview.reset(select.value);
                preview.status(this.texts.loading);
                this.updateLayers();
                this.loadPreview(preview, true);
            });
        });
        this.nodes.Compare.addEventListener("click", () => this.setComparison(!this.comparing));
        this.nodes.SelectArea.addEventListener("click", () => this.setAreaSelection(!this.selectingArea));
        this.nodes.Width.addEventListener("change", () => this.loadWidth());
        this.nodes.Reload.addEventListener("click", () => this.loadWidth(true));
        this.nodes.Scale.addEventListener("change", () => this.resize());
        this.nodes.Opacity.addEventListener("input", () => {
            this.nodes.OpacityValue.textContent = this.nodes.Opacity.value + " %";
            this.updateLayers();
        });
        this.nodes.Visible.addEventListener("click", () => {
            this.overlayVisible = !this.overlayVisible;
            this.updateLayers();
            this.activePreviews().forEach(preview => preview.scheduleTiles());
        });
        this.resizeObserver = new ResizeObserver(() => this.resize());
        for (const preview of this.previews) {
            preview.nodes.Frame.addEventListener("load", () => preview.frameLoaded());
            this.resizeObserver.observe(preview.nodes.Viewport);
        }
        window.addEventListener("pagehide", event => {
            if (!event.persisted) this.destroy();
        });
        if (!Number.isSafeInteger(this.docId) || this.docId < 1) {
            this.previews[0].status(this.texts.error, true);
            return;
        }
        await this.loadWidth(true);
    }

    activePreviews() {
        return this.comparing ? this.previews : [this.previews[0]];
    }

    setAreaSelection(enabled) {
        this.selectingArea = enabled;
        this.nodes.SelectArea.setAttribute("aria-pressed", String(enabled));
        this.nodes.SelectArea.classList.toggle("btn-primary", enabled);
        this.nodes.SelectArea.classList.toggle("btn-outline-secondary", !enabled);
        if (!enabled) this.previews.forEach(preview => preview.area.clear());
        this.updateLayers();
    }

    setComparison(enabled) {
        if (enabled && this.nodes.CompareVersion.options.length < 2) return;
        this.comparing = enabled;
        if (enabled) this.setAreaSelection(false);
        this.nodes.Compare.setAttribute("aria-pressed", String(enabled));
        this.nodes.Compare.classList.toggle("btn-primary", enabled);
        this.nodes.Compare.classList.toggle("btn-outline-secondary", !enabled);
        this.element.classList.toggle("pg-heatmap-comparing", enabled);
        this.previews[1].element.hidden = !enabled;
        this.previews.forEach((preview, index) => {
            preview.nodes.VersionLabel.textContent = enabled ? (index === 0 ? this.texts.versionA : this.texts.versionB) : this.texts.version;
            preview.nodes.PaneLabel.hidden = index === 1 && !enabled;
            if (enabled) preview.element.setAttribute("aria-labelledby", preview.nodes.VersionLabel.id);
            else preview.element.removeAttribute("aria-labelledby");
        });
        if (enabled) {
            this.singleVersion = this.nodes.Version.value;
            if (!this.nodes.Version.value) this.nodes.Version.value = this.nodes.CompareVersion.options[0].value;
            if (!this.nodes.CompareVersion.value || this.nodes.CompareVersion.value === this.nodes.Version.value) {
                this.nodes.CompareVersion.value = [...this.nodes.CompareVersion.options].find(option => option.value !== this.nodes.Version.value).value;
            }
        } else {
            this.nodes.Version.value = this.singleVersion || "";
        }
        this.updateVersionOptions();
        this.loadWidth(true);
    }

    /** Excludes the aggregate view and the other pane's version while comparing. */
    updateVersionOptions() {
        for (const option of this.nodes.Version.options) {
            option.disabled = this.comparing && (!option.value || option.value === this.nodes.CompareVersion.value);
            option.hidden = this.comparing && !option.value;
        }
        for (const option of this.nodes.CompareVersion.options) {
            option.disabled = this.comparing && option.value === this.nodes.Version.value;
        }
    }

    setWidths(widths, previewOnly) {
        const reportWidth = this.params.get("width");
        const reportGroup = this.params.get("mode") === "CONTAINER" ? "container:" + reportWidth : reportWidth;
        const requestedValue = this.nodes.Width.value || reportGroup;
        this.nodes.Width.replaceChildren();
        for (const item of widths) {
            const option = document.createElement("option");
            option.value = item.mode === "CONTAINER" ? "container:" + item.width : String(item.width);
            option.dataset.width = item.width;
            option.dataset.previewWidth = item.previewWidth || item.width;
            option.dataset.mode = item.mode || "DOCUMENT";
            const label = item.mode === "CONTAINER" ? this.texts.container : this.texts.window;
            option.textContent = label + " " + item.width + " px";
            if (!this.comparing) option.textContent += " · " + this.texts.clicks + ": " + (previewOnly ? 0 : item.clicks).toLocaleString();
            this.nodes.Width.append(option);
        }
        if ([...this.nodes.Width.options].some(option => option.value === requestedValue)) this.nodes.Width.value = requestedValue;
        this.nodes.Width.disabled = widths.length === 0;
    }

    setVersions(metadata) {
        const versions = metadata.versions || [];
        const select = this.nodes.Version;
        const selectedId = select.value;
        const comparisonId = this.nodes.CompareVersion.value;
        select.replaceChildren(select.options[0]);
        this.nodes.CompareVersion.replaceChildren();
        for (const version of versions) {
            const label = "#" + version.historyId + " · " + WJ.formatDateTimeSeconds(version.effectiveFrom);
            select.add(new Option(label, version.historyId));
            this.nodes.CompareVersion.add(new Option(label, version.historyId));
        }
        select.value = selectedId;
        this.nodes.CompareVersion.value = comparisonId;
        this.nodes.Compare.disabled = versions.length < 2;
        const onlyRange = versions.length === 1 ? versions[0].range : null;
        const coversReport = onlyRange && onlyRange.from === metadata.clickRange?.from && onlyRange.until === metadata.clickRange?.until;
        select.parentElement.hidden = !selectedId && (versions.length === 0 || (metadata.source === "history" && coversReport));
        this.updateVersionOptions();
    }

    endpoint(path, versionId, extra = {}) {
        const params = new URLSearchParams({docId: this.docId, dateRange: this.dateRange, ...extra});
        if (versionId) params.set("versionId", versionId);
        return this.api + path + "?" + params.toString();
    }

    async getJson(path, versionId, extra = {}, signal = this.requestController.signal) {
        const response = await fetch(this.endpoint(path, versionId, extra), {
            credentials: "same-origin", headers: {Accept: "application/json", "X-CSRF-Token": window.csrfToken}, signal
        });
        if (!response.ok) throw new Error("Heat map request failed");
        return response.json();
    }

    async loadWidth(refreshWidths = false) {
        const revision = ++this.revision;
        this.requestController.abort();
        this.requestController = new AbortController();
        const previews = this.activePreviews();
        this.previews.forEach((preview, index) => preview.reset(index === 0 ? this.nodes.Version.value : this.nodes.CompareVersion.value));
        this.nodes.Reload.disabled = true;
        this.nodes.Version.disabled = true;
        this.nodes.CompareVersion.disabled = true;
        previews.forEach(preview => preview.status(this.texts.loading));
        this.updateLayers();
        try {
            if (refreshWidths || previews.some(preview => preview.widthsVersion !== preview.versionId)) {
                this.nodes.Width.disabled = true;
                const versionIds = [...new Set(["", ...previews.map(preview => preview.versionId)])];
                const reports = await Promise.all(versionIds.map(async versionId => [versionId, await this.getJson("/widths", versionId)]));
                if (revision !== this.revision) return;
                const widthsByVersion = new Map(reports);
                this.reportWidths = widthsByVersion.get("");
                previews.forEach(preview => {
                    preview.widths = widthsByVersion.get(preview.versionId);
                    preview.widthsVersion = preview.versionId;
                });
                const previewOnly = previews[0].widths.length === 0;
                const widths = this.comparing || previewOnly ? this.reportWidths : previews[0].widths;
                this.setWidths(widths, previewOnly);
            }
            const selected = this.nodes.Width.selectedOptions[0];
            if (!selected) {
                previews.forEach(preview => {
                    preview.updateSummary(0);
                    preview.status(this.texts.empty);
                });
                return;
            }
            if (!this.comparing && !this.nodes.Version.value) {
                this.params.set("width", selected.dataset.width);
                this.params.set("mode", selected.dataset.mode);
                history.replaceState(null, "", "?" + this.params.toString());
            }
            await Promise.all(previews.map(preview => this.loadPreview(preview)));
        } catch (error) {
            if (revision === this.revision) previews.forEach(preview => preview.status(this.texts.error, true));
        } finally {
            if (revision === this.revision) {
                this.nodes.Reload.disabled = false;
                this.nodes.Version.disabled = false;
                this.nodes.CompareVersion.disabled = false;
            }
        }
    }

    /** Loads one pane without invalidating the other pane's requests or document. */
    async loadPreview(preview, refreshWidths = false) {
        const revision = preview.revision;
        const signal = preview.requestController.signal;
        try {
            if (refreshWidths) {
                const widths = await this.getJson("/widths", preview.versionId, {}, signal);
                if (revision !== preview.revision) return;
                preview.widths = widths;
                preview.widthsVersion = preview.versionId;
            }
            const selected = this.nodes.Width.selectedOptions[0];
            if (!selected) {
                preview.updateSummary(0);
                preview.status(this.texts.empty);
                return;
            }
            preview.groupWidth = Number(selected.dataset.width);
            preview.mode = selected.dataset.mode;
            preview.width = Number(selected.dataset.previewWidth);
            preview.previewOnly = !preview.widths.some(item => item.width === preview.groupWidth && (item.mode || "DOCUMENT") === preview.mode);
            if (preview.previewOnly) {
                preview.updateSummary(0);
            } else {
                const metadata = await this.getJson("/metadata", preview.versionId, {width: preview.groupWidth, mode: preview.mode}, signal);
                if (revision !== preview.revision) return;
                preview.updateSummary(metadata.clicks);
                if (preview === this.previews[0]) {
                    if (!this.comparing) this.setVersions(metadata);
                    this.nodes.Title.textContent = metadata.title;
                }
                preview.tileSize = metadata.tileSize;
                // Container groups can have different representative viewports per version.
                if (!this.comparing) preview.width = metadata.previewWidth || preview.groupWidth;
                preview.containerSelector = metadata.containerSelector || "";
                const notes = [];
                if (metadata.historicalUnavailable && metadata.source === "current") notes.push(this.texts.fallback);
                if (metadata.multipleVersions) notes.push(this.texts.multiple);
                if (notes.length > 0) WJ.notifyWarning(this.texts.title, notes.join(" "), 5000);
                if (metadata.source === "unavailable") {
                    preview.status(this.texts.unavailable, true);
                    return;
                }
            }
            preview.openPreview();
        } catch (error) {
            if (revision === preview.revision) preview.status(this.texts.error, true);
        }
    }

    updateLayers() {
        const previews = this.activePreviews();
        const available = previews.some(preview => preview.frameReady && !preview.previewOnly && preview.layoutValid);
        this.nodes.Visible.disabled = !available;
        this.nodes.Visible.setAttribute("aria-pressed", String(this.overlayVisible));
        this.nodes.Visible.classList.toggle("btn-primary", this.overlayVisible);
        this.nodes.Visible.classList.toggle("btn-outline-secondary", !this.overlayVisible);
        this.nodes.Visible.querySelector(".ti").classList.toggle("ti-eye", this.overlayVisible);
        this.nodes.Visible.querySelector(".ti").classList.toggle("ti-eye-off", !this.overlayVisible);
        this.nodes.SelectArea.disabled = this.comparing || (!available && !this.selectingArea);
        this.nodes.Opacity.disabled = !available || !this.overlayVisible;
        for (const preview of previews) {
            preview.nodes.Overlay.hidden = !preview.frameReady || preview.previewOnly || !this.overlayVisible || !preview.layoutValid;
            preview.nodes.Overlay.style.opacity = Number(this.nodes.Opacity.value) / 100;
            preview.area.render();
        }
    }

    /** Uses one numeric scale and iframe height for all ready comparison panes. */
    resize() {
        const previews = this.activePreviews();
        previews.forEach(preview => { preview.nodes.ScaleSummary.textContent = this.nodes.Scale.selectedOptions[0].textContent; });
        const ready = previews.filter(preview => preview.frameReady);
        if (ready.length === 0) return;
        const scale = this.nodes.Scale.value === "fit"
            ? Math.min(1, ...ready.map(preview => Math.max(1, preview.nodes.Viewport.clientWidth - 10) / preview.width))
            : Number(this.nodes.Scale.value);
        const height = Math.max(1, Math.floor(Math.min(...ready.map(preview => preview.nodes.Viewport.clientHeight - 10)) / scale));
        ready.forEach(preview => preview.resize(scale, height));
    }

    destroy() {
        this.revision++;
        this.requestController.abort();
        this.resizeObserver.disconnect();
        document.removeEventListener("keydown", this.closeOnEscape);
        $(this.element).find(".pg-heatmap-icon").tooltip("dispose");
        this.previews.forEach(preview => preview.reset(""));
    }
}

/** Owns the document, tiles and load state of a single version preview. */
class HeatMapPreview {
    constructor(viewer, element, prefix) {
        this.viewer = viewer;
        this.element = element;
        this.texts = viewer.texts;
        this.tiles = new Map();
        this.widths = [];
        this.height = 800;
        this.width = 0;
        this.tileSize = 1024;
        this.revision = 0;
        this.requestController = new AbortController();
        this.nodes = {};
        ["PaneLabel", "VersionLabel", "Frame", "Viewport", "Stage", "StageSize", "Overlay", "Tiles", "Status", "Summary", "TotalClicks", "ScaleSummary",
            "AreaLayer", "AreaBox", "AreaResult", "AreaCount", "ClearArea"].forEach(name => {
            this.nodes[name] = element.querySelector("#" + prefix + name);
        });
        this.area = new HeatMapArea(this);
    }

    reset(versionId) {
        this.area.clear();
        this.revision++;
        this.requestController.abort();
        this.requestController = new AbortController();
        this.versionId = versionId;
        clearTimeout(this.previewTimer);
        cancelAnimationFrame(this.animationFrame);
        this.animationFrame = null;
        this.contentObserver?.disconnect();
        this.frameReady = false;
        this.layoutValid = true;
        this.nodes.Frame.removeAttribute("src");
        this.nodes.Viewport.classList.add("d-none");
        this.nodes.Summary.hidden = true;
        this.clearTiles();
    }

    status(message, error = false) {
        this.nodes.Status.textContent = message;
        this.nodes.Status.className = "alert " + (error ? "alert-danger" : "alert-info");
        this.nodes.Status.classList.toggle("d-none", !message);
        this.element.setAttribute("aria-busy", String(message === this.texts.loading));
        this.viewer.element.setAttribute("aria-busy", String(this.viewer.activePreviews().some(preview => preview.element.getAttribute("aria-busy") === "true")));
    }

    updateSummary(clicks) {
        this.nodes.TotalClicks.textContent = this.texts.clicks + ": " + clicks.toLocaleString();
        this.nodes.ScaleSummary.textContent = this.viewer.nodes.Scale.selectedOptions[0].textContent;
        this.nodes.Summary.hidden = false;
    }

    openPreview() {
        this.nodes.Frame.style.width = this.width + "px";
        this.nodes.Stage.style.width = this.width + "px";
        this.nodes.Stage.style.height = this.height + "px";
        this.nodes.Frame.src = this.viewer.endpoint("/html/preview", this.versionId, {previewRevision: this.revision});
        const revision = this.revision;
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
            if (loadedUrl.href === "about:blank") return;
            if (loadedUrl.pathname === this.viewer.api + "/html/preview" && loadedUrl.searchParams.get("previewRevision") !== String(this.revision)) return;
            clearTimeout(this.previewTimer);
            if (doc.querySelector('meta[name="webjet-heatmap-preview"]')?.content !== String(this.viewer.docId)) {
                throw new Error("Unexpected preview document");
            }
            this.frameReady = true;
            this.nodes.Viewport.classList.remove("d-none");
            let message = "";
            if (this.previewOnly) message = this.viewer.comparing ? this.texts.emptyWidth : this.texts.emptyVersion;
            this.status(message);
            frameWindow.addEventListener("scroll", () => this.scheduleTiles(), {passive: true});
            frameWindow.addEventListener("resize", () => this.scheduleTiles());
            // A header can finish sliding into place after the last scroll or resize event.
            doc.addEventListener("transitionend", () => this.scheduleTiles());
            doc.addEventListener("animationend", () => this.scheduleTiles());
            doc.addEventListener("click", event => event.preventDefault(), true);
            doc.addEventListener("submit", event => event.preventDefault(), true);
            doc.addEventListener("keydown", this.viewer.closeOnEscape);
            this.contentObserver?.disconnect();
            this.contentObserver = new ResizeObserver(() => this.scheduleTiles());
            this.contentObserver.observe(doc.documentElement);
            this.viewer.resize();
            this.viewer.updateLayers();
        } catch (error) {
            clearTimeout(this.previewTimer);
            this.frameReady = false;
            this.nodes.Viewport.classList.add("d-none");
            this.status(this.texts.previewError, true);
            this.viewer.updateLayers();
        }
    }

    resize(scale, height) {
        this.scale = scale;
        this.height = height;
        this.nodes.Frame.style.height = height + "px";
        this.nodes.Stage.style.height = height + "px";
        this.nodes.Stage.style.transform = "scale(" + scale + ")";
        this.nodes.StageSize.style.width = this.width * scale + "px";
        this.nodes.StageSize.style.height = height * scale + "px";
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

    /** Finds pinned header rectangles in unscaled iframe viewport coordinates. */
    headerRectangles(frameWindow) {
        if (frameWindow.scrollY === 0) return [];
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
            cutouts.push(rect);
        }
        return cutouts;
    }

    /** Cuts pinned headers out of the heat overlay without counting the covered document as visible content. */
    headerMask(frameWindow) {
        const cutouts = this.headerRectangles(frameWindow).map(rect =>
            `linear-gradient(black, black) ${rect.left}px ${rect.top}px / ${rect.width}px ${rect.height}px no-repeat`);
        return cutouts.length ? "linear-gradient(black, black) subtract, " + cutouts.join(", ") : "";
    }

    updateTiles() {
        if (!this.frameReady || this.previewOnly) return;
        const frameWindow = this.nodes.Frame.contentWindow;
        const offset = this.containerOffset(frameWindow);
        const wasValid = this.layoutValid;
        this.layoutValid = offset !== null;
        this.viewer.updateLayers();
        if (!this.layoutValid) {
            this.clearTiles();
            this.status(this.texts.containerMissing.replace("{width}", this.groupWidth), true);
            return;
        }
        if (!wasValid) this.status("");
        if (!this.viewer.overlayVisible) return;
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
                tile.src = this.viewer.endpoint("/binary/tile", this.versionId, {width: this.groupWidth, mode: this.mode, tileX: x, tileY: y});
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

}
