/** Links ranked coordinate regions to the single-page preview without changing click collection. */
export class HeatMapHotspots {
    constructor(viewer, preview) {
        this.viewer = viewer;
        this.preview = preview;
        this.nodes = {};
        ["Panel", "Status", "Columns", "List", "Row", "Totals", "Top", "Other", "Result", "Layer", "Box", "Markers"].forEach(name => {
            this.nodes[name] = viewer.element.querySelector("#heatMapHotspot" + name);
        });
        this.nodes.Layer.addEventListener("wheel", event => {
            if (event.ctrlKey) return;
            event.preventDefault();
            const view = this.preview.nodes.Frame.contentWindow;
            const unit = event.deltaMode === 1 ? 16 : event.deltaMode === 2 ? view.innerHeight : 1;
            view.scrollBy(event.deltaX * unit, event.deltaY * unit);
        }, {passive: false});
        this.reset();
    }

    active() {
        return this.viewer.hotspotsVisible && !this.viewer.comparing;
    }

    reset() {
        this.requested = false;
        this.report = null;
        this.selected = -1;
        this.nodes.List.replaceChildren();
        this.nodes.Markers.replaceChildren();
        this.nodes.Totals.hidden = true;
        this.nodes.Columns.hidden = true;
        this.nodes.Result.hidden = true;
        this.nodes.Layer.hidden = true;
        this.nodes.Box.hidden = true;
        this.status(this.viewer.texts.hotspotsLoading);
    }

    status(message, loading = false) {
        this.nodes.Status.textContent = message;
        this.nodes.Status.hidden = !message;
        this.nodes.Panel.setAttribute("aria-busy", String(loading));
    }

    /** Loads once per preview; its request signal also cancels obsolete hotspot replies. */
    async load() {
        const preview = this.preview;
        if (!this.active() || !preview.frameReady || preview.previewOnly || this.requested) return;
        this.requested = true;
        const signal = preview.requestController.signal;
        this.status(this.viewer.texts.hotspotsLoading, true);
        try {
            const report = await this.viewer.getJson("/hotspots", preview.versionId,
                {width: preview.groupWidth, mode: preview.mode}, signal);
            if (signal.aborted) return;
            this.report = report;
            this.populate();
            this.status(report.hotspots.length ? "" : this.viewer.texts.empty);
            this.update();
        } catch (error) {
            if (!signal.aborted) this.status(this.viewer.texts.hotspotsError);
        }
    }

    format(template, values) {
        return template.replace(/\{(\w+)\}/g, (match, key) => values[key] ?? match);
    }

    values(clicks) {
        const total = this.report.totalClicks;
        return {clicks: clicks.toLocaleString(), total: total.toLocaleString(),
            percent: (total > 0 ? 100 * clicks / total : 0).toLocaleString(undefined, {maximumFractionDigits: 1})};
    }

    populate() {
        const {hotspots, totalClicks} = this.report;
        let topClicks = 0;
        const maximum = hotspots[0]?.clicks || 1;
        hotspots.forEach((hotspot, index) => {
            topClicks += hotspot.clicks;
            const name = this.format(this.viewer.texts.hotspot, {rank: index + 1});
            const values = this.values(hotspot.clicks);
            const item = this.nodes.Row.content.firstElementChild.cloneNode(true);
            const button = item.querySelector("button");
            button.setAttribute("aria-label", this.format(this.viewer.texts.hotspotResult, {name, ...values}));
            button.querySelector(".pg-heatmap-hotspot-number").textContent = index + 1;
            button.querySelector(".pg-heatmap-hotspot-name").append(document.createTextNode(name));
            button.querySelector("strong").textContent = values.clicks;
            button.querySelector(".pg-heatmap-hotspot-share").textContent = values.percent + " %";
            button.querySelector(".pg-heatmap-hotspot-bar").style.width = (100 * hotspot.clicks / maximum) + "%";
            button.addEventListener("click", () => this.select(index, true));
            this.nodes.List.append(item);

            const marker = document.createElement("button");
            marker.type = "button";
            marker.className = "pg-heatmap-hotspot-marker";
            marker.textContent = index + 1;
            marker.setAttribute("aria-label", button.getAttribute("aria-label"));
            marker.addEventListener("click", () => this.select(index, false));
            this.nodes.Markers.append(marker);
        });
        this.nodes.Columns.hidden = hotspots.length === 0;
        this.nodes.Totals.hidden = hotspots.length === 0;
        this.nodes.Top.textContent = this.format(this.viewer.texts.hotspotsTop, {count: hotspots.length, ...this.values(topClicks)});
        this.nodes.Other.textContent = this.format(this.viewer.texts.hotspotsOther, this.values(totalClicks - topClicks));
    }

    /** Selecting a list row centers its recorded region without changing the iframe's layout width. */
    select(index, scroll) {
        if (!this.preview.frameReady || !this.preview.layoutValid) return;
        this.viewer.setAreaSelection(false);
        this.selected = index;
        const hotspot = this.report.hotspots[index];
        this.nodes.Result.textContent = this.nodes.List.children[index].querySelector("button").getAttribute("aria-label");
        this.nodes.Result.hidden = false;
        if (scroll) {
            const view = this.preview.nodes.Frame.contentWindow;
            const offset = this.preview.containerOffset(view);
            if (offset !== null) view.scrollTo(hotspot.x + offset - view.document.documentElement.clientWidth / 2,
                hotspot.y - view.document.documentElement.clientHeight / 2);
        }
        this.update();
        this.preview.scheduleTiles();
    }

    /** Keeps markers in viewport coordinates and readable at every preview scale. */
    update() {
        const active = this.active();
        const preview = this.preview;
        this.nodes.Panel.hidden = !active;
        this.viewer.nodes.Hotspots.disabled = this.viewer.comparing;
        const available = active && preview.frameReady && !preview.previewOnly && preview.layoutValid;
        this.nodes.List.querySelectorAll("button").forEach((button, index) => {
            button.disabled = !available;
            button.setAttribute("aria-pressed", String(index === this.selected));
        });
        this.nodes.Layer.hidden = !available || this.viewer.selectingArea || !this.report?.hotspots.length;
        if (preview.previewOnly && active) this.status(this.viewer.texts.emptyWidth);
        if (this.nodes.Layer.hidden) return;
        const view = preview.nodes.Frame.contentWindow;
        const offset = preview.containerOffset(view);
        if (offset === null) {
            this.nodes.Layer.hidden = true;
            return;
        }
        const doc = view.document.documentElement;
        const scale = preview.scale || 1;
        const size = 28 / scale;
        this.nodes.Layer.style.width = doc.clientWidth + "px";
        this.nodes.Layer.style.height = doc.clientHeight + "px";
        const pinned = preview.headerRectangles(view);
        this.report.hotspots.forEach((hotspot, index) => {
            const x = hotspot.x + offset - view.scrollX;
            const y = hotspot.y - view.scrollY;
            const marker = this.nodes.Markers.children[index];
            // Hide covered or off-screen buttons instead of leaving invisible keyboard targets.
            marker.hidden = x < 0 || y < 0 || x >= doc.clientWidth || y >= doc.clientHeight
                || pinned.some(rect => x + size / 2 > rect.left && x - size / 2 < rect.right
                    && y + size / 2 > rect.top && y - size / 2 < rect.bottom);
            marker.setAttribute("aria-pressed", String(index === this.selected));
            Object.assign(marker.style, {left: Math.max(size / 2, Math.min(doc.clientWidth - size / 2, x)) + "px",
                top: Math.max(size / 2, Math.min(doc.clientHeight - size / 2, y)) + "px", width: size + "px", height: size + "px",
                fontSize: 13 / scale + "px", borderWidth: 2 / scale + "px"});
        });
        const selected = this.report.hotspots[this.selected];
        this.nodes.Box.hidden = !selected;
        if (selected) {
            // Mask on a viewport-sized wrapper keeps the region under pinned headers, like the heat layer.
            this.nodes.Layer.style.mask = preview.headerMask(view);
            Object.assign(this.nodes.Box.style, {left: selected.x1 + offset - view.scrollX + "px", top: selected.y1 - view.scrollY + "px",
                width: selected.x2 - selected.x1 + "px", height: selected.y2 - selected.y1 + "px", borderWidth: 2 / scale + "px"});
        } else this.nodes.Layer.style.mask = "";
    }
}
