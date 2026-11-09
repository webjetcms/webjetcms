/** Selects and counts one document-coordinate rectangle without changing the preview. */
export class HeatMapArea {
    constructor(preview) {
        this.preview = preview;
        this.viewer = preview.viewer;
        this.nodes = preview.nodes;
        const layer = this.nodes.AreaLayer;
        layer.addEventListener("pointerdown", event => {
            if (event.button !== 0 || !this.viewer.selectingArea) return;
            const point = this.pointerPoint(event);
            if (!point) return;
            event.preventDefault();
            this.clear();
            this.start = point;
            this.move(point);
            this.pointerId = event.pointerId;
            layer.setPointerCapture(event.pointerId);
        });
        layer.addEventListener("pointermove", event => {
            if (this.pointerId !== event.pointerId) return;
            this.move(this.pointerPoint(event));
        });
        layer.addEventListener("pointerup", event => {
            if (this.pointerId !== event.pointerId) return;
            this.move(this.pointerPoint(event));
            this.pointerId = null;
            layer.releasePointerCapture(event.pointerId);
            this.start = null;
            if (this.bounds) this.count();
        });
        layer.addEventListener("pointercancel", () => { this.clear(); this.render(); });
        layer.addEventListener("wheel", event => {
            if (!this.viewer.selectingArea || event.ctrlKey) return;
            event.preventDefault();
            const view = this.preview.nodes.Frame.contentWindow;
            const unit = event.deltaMode === 1 ? 16 : event.deltaMode === 2 ? view.innerHeight : 1;
            view.scrollBy(event.deltaX * unit, event.deltaY * unit);
        }, {passive: false});
        this.nodes.ClearArea.addEventListener("click", () => {
            this.clear();
            this.render();
            this.viewer.nodes.SelectArea.focus({preventScroll: true});
        });
    }

    /** Describes the visible content in the same coordinate system used by the stored clicks. */
    geometry() {
        const preview = this.preview;
        if (!preview.frameReady || preview.previewOnly || !preview.layoutValid) return null;
        const view = preview.nodes.Frame.contentWindow;
        const doc = view.document.documentElement;
        const offset = preview.containerOffset(view);
        if (offset === null) return null;
        const pinned = preview.headerRectangles(view);
        // Only full-width headers reserve a strip across the entire preview.
        const inset = Math.max(0, ...pinned.filter(rect => rect.top <= 1 && rect.left <= 1 && rect.right >= doc.clientWidth - 1)
            .map(rect => rect.bottom));
        const sidebars = pinned.filter(rect => rect.top <= inset + 1 && rect.bottom >= doc.clientHeight - 1);
        const insetLeft = Math.max(0, ...sidebars.filter(rect => rect.left <= 1).map(rect => rect.right));
        const right = Math.min(doc.clientWidth, ...sidebars.filter(rect => rect.right >= doc.clientWidth - 1).map(rect => rect.left));
        const left = view.scrollX + insetLeft - offset;
        const top = view.scrollY + inset;
        const geometry = {left, top, inset, insetLeft, width: right - insetLeft, height: doc.clientHeight - inset,
            x1: Math.max(0, left), y1: top,
            x2: Math.min(1000001, preview.mode === "CONTAINER" ? preview.groupWidth : doc.scrollWidth, view.scrollX + right - offset),
            y2: Math.min(1000001, doc.scrollHeight, view.scrollY + doc.clientHeight)};
        return geometry.x2 > geometry.x1 && geometry.y2 > geometry.y1 ? geometry : null;
    }

    pointerPoint(event) {
        const geometry = this.geometry();
        if (!geometry) return null;
        const rect = this.nodes.AreaLayer.getBoundingClientRect();
        const x = geometry.left + (event.clientX - rect.left) / this.preview.scale;
        const y = geometry.top + (event.clientY - rect.top) / this.preview.scale;
        return {x: Math.max(geometry.x1, Math.min(geometry.x2, x)),
            y: Math.max(geometry.y1, Math.min(geometry.y2, y))};
    }

    move(point) {
        if (!point || !this.start) return;
        this.bounds = this.start.x === point.x || this.start.y === point.y ? null : {
            x1: Math.floor(Math.min(this.start.x, point.x)), y1: Math.floor(Math.min(this.start.y, point.y)),
            x2: Math.ceil(Math.max(this.start.x, point.x)), y2: Math.ceil(Math.max(this.start.y, point.y))};
        this.render();
    }

    async count() {
        this.controller?.abort();
        this.controller = new AbortController();
        const signal = this.controller.signal;
        this.nodes.AreaResult.hidden = false;
        this.nodes.AreaCount.textContent = this.viewer.texts.areaLoading;
        this.nodes.AreaResult.setAttribute("aria-busy", "true");
        try {
            const result = await this.viewer.getJson("/area", this.preview.versionId,
                {width: this.preview.groupWidth, mode: this.preview.mode, ...this.bounds}, signal);
            if (signal.aborted) return;
            const percent = result.totalClicks > 0 ? 100 * result.clicks / result.totalClicks : 0;
            this.nodes.AreaCount.textContent = this.viewer.texts.areaResult
                .replace("{clicks}", result.clicks.toLocaleString()).replace("{total}", result.totalClicks.toLocaleString())
                .replace("{percent}", percent.toLocaleString(undefined, {maximumFractionDigits: 1}));
        } catch (error) {
            if (!signal.aborted) this.nodes.AreaCount.textContent = this.viewer.texts.areaError;
        } finally {
            if (!signal.aborted) this.nodes.AreaResult.setAttribute("aria-busy", "false");
        }
    }

    render() {
        if (!this.preview.layoutValid && this.bounds) this.clear();
        const geometry = this.viewer.selectingArea ? this.geometry() : null;
        this.nodes.AreaLayer.hidden = !geometry;
        if (!geometry) return;
        this.nodes.AreaLayer.style.top = geometry.inset + "px";
        this.nodes.AreaLayer.style.left = geometry.insetLeft + "px";
        this.nodes.AreaLayer.style.width = geometry.width + "px";
        this.nodes.AreaLayer.style.height = geometry.height + "px";
        this.nodes.AreaBox.hidden = !this.bounds;
        if (this.bounds) {
            Object.assign(this.nodes.AreaBox.style, {left: (this.bounds.x1 - geometry.left) + "px",
                top: (this.bounds.y1 - geometry.top) + "px", width: (this.bounds.x2 - this.bounds.x1) + "px",
                height: (this.bounds.y2 - this.bounds.y1) + "px"});
        }
    }

    clear() {
        this.controller?.abort();
        if (this.pointerId != null && this.nodes.AreaLayer.hasPointerCapture(this.pointerId)) this.nodes.AreaLayer.releasePointerCapture(this.pointerId);
        this.pointerId = null;
        this.start = null;
        this.bounds = null;
        this.nodes.AreaLayer.hidden = true;
        this.nodes.AreaBox.hidden = true;
        this.nodes.AreaResult.hidden = true;
        this.nodes.AreaResult.setAttribute("aria-busy", "false");
    }
}
