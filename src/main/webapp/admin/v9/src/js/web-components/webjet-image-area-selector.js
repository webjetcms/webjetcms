import Cropper from 'cropperjs';

const COORDINATE_KEYS = ["left", "top", "width", "height"];

export class WebjetImageAreaSelectorElement extends HTMLElement {
    constructor() {
        super();
        this.options = {};
        this.cropper = null;
        this.cropperImage = null;
        this.selection = null;
        this.imageSize = { width: 0, height: 0 };
        this.coordinates = { left: 0, top: 0, width: 0, height: 0 };
        this.zoom = 100;
        this._configured = false;
        this._inputTimeout = null;
        this._applyingCoordinates = false;
        this._applyTimeout = null;
    }

    connectedCallback() {
        if (this._configured && !this.firstChild) this.render();
    }

    disconnectedCallback() {
        clearTimeout(this._inputTimeout);
        clearTimeout(this._applyTimeout);
        this._destroyCropper();
    }

    configure(options = {}) {
        this.options = options;
        this._configured = true;
        if (this.isConnected) this.render();
        return this;
    }

    render() {
        const labels = this.options.labels || {};
        this.innerHTML = `
            <section class="webjet-image-area-selector">
                <div class="loading"><div class="spinner-border text-primary" role="status"></div></div>
                <div class="ready" hidden>
                    <header class="coordinates">
                        ${this._coordinateInput("left", "x", labels.x)}
                        ${this._coordinateInput("top", "y", labels.y)}
                        ${this._coordinateInput("width", "w", labels.width)}
                        ${this._coordinateInput("height", "h", labels.height)}
                        <label for="zoom" class="coordinate-label">${WJ.escapeHtml(labels.zoom || "")}</label>
                        <input class="form-control coordinate-input" id="zoom" data-zoom type="number" step="25" min="25" max="500" value="100">
                    </header>
                    <div class="cropper-wrapper"><img alt=""></div>
                </div>
            </section>`;
        this.querySelectorAll("[data-coordinate]").forEach(input => input.addEventListener("input", () => {
            clearTimeout(this._inputTimeout);
            this._inputTimeout = setTimeout(() => {
                this.coordinates[input.dataset.coordinate] = Number(input.value) || 0;
                this._publishCoordinates();
                this._setSelection(this.coordinates);
            }, 300);
        }));
        this.querySelector("[data-zoom]").addEventListener("input", event => this._setZoom(Number(event.target.value) || 100));
        this.querySelector(".cropper-wrapper").addEventListener("wheel", event => {
            if (event.ctrlKey) event.preventDefault();
            event.stopPropagation();
        }, { capture: true, passive: false });
    }

    _coordinateInput(key, id, label = "") {
        return `<label for="${id}" class="coordinate-label">${WJ.escapeHtml(label || "")}</label><input class="form-control coordinate-input" id="${id}" data-coordinate="${key}" type="number" value="0">`;
    }

    activate() {
        this.refresh();
    }

    deactivate() {
        this.dataset.ready = "false";
        this.querySelector(".loading")?.removeAttribute("hidden");
        this.querySelector(".ready")?.setAttribute("hidden", "");
        this._destroyCropper();
    }

    refresh() {
        const imageUrl = this.options.getImageUrl?.();
        if (!imageUrl) return;
        this.coordinates = this._normalizeCoordinates(this.options.getCoordinates?.());
        this._updateCoordinateInputs();
        this._destroyCropper();
        const image = this.querySelector("img");
        image.src = WJ.urlUpdateParam(imageUrl, "v", Date.now());
        image.onload = () => this._initializeCropper(image);
        image.onerror = () => {
            this.dataset.ready = "error";
            this.querySelector(".loading")?.setAttribute("hidden", "");
        };
    }

    resetVisibleArea() {
        this.selection?.$render?.();
    }

    _initializeCropper(image) {
        const initialCoordinates = { ...this.coordinates };
        this.imageSize = { width: image.naturalWidth, height: image.naturalHeight };
        this._applyingCoordinates = true;
        this.cropper = new Cropper(image, {
            template: `<cropper-canvas background style="width:${this.imageSize.width}px;height:${this.imageSize.height}px"><cropper-image></cropper-image><cropper-shade hidden></cropper-shade><cropper-handle action="select" plain></cropper-handle><cropper-selection initial-coverage="1" movable resizable><cropper-grid role="grid" bordered covered></cropper-grid><cropper-crosshair centered></cropper-crosshair><cropper-handle action="move" theme-color="rgba(255, 255, 255, 0.35)"></cropper-handle><cropper-handle action="n-resize"></cropper-handle><cropper-handle action="e-resize"></cropper-handle><cropper-handle action="s-resize"></cropper-handle><cropper-handle action="w-resize"></cropper-handle><cropper-handle action="ne-resize"></cropper-handle><cropper-handle action="nw-resize"></cropper-handle><cropper-handle action="se-resize"></cropper-handle><cropper-handle action="sw-resize"></cropper-handle></cropper-selection></cropper-canvas>`
        });
        this.cropperImage = this.cropper.getCropperImage();
        this.selection = this.cropper.getCropperSelection();
        this.cropper.getCropperCanvas()?.addEventListener("actionend", () => {
            if (!this._applyingCoordinates) this._selectionChanged(this.selection);
        });
        this.querySelector(".loading")?.setAttribute("hidden", "");
        this.querySelector(".ready")?.removeAttribute("hidden");
        this.dataset.ready = "true";
        this.cropperImage?.$ready?.(() => requestAnimationFrame(() => {
            if (COORDINATE_KEYS.some(key => initialCoordinates[key] !== 0)) this._setSelection(initialCoordinates);
            else this._applyingCoordinates = false;
        }));
        this.dispatchEvent(new CustomEvent("webjet-component-ready", { bubbles: true }));
    }

    _destroyCropper() {
        this.cropper?.destroy?.();
        this.cropper = null;
        this.cropperImage = null;
        this.selection = null;
    }

    _setSelection(coordinates) {
        if (!this.selection) return;
        const { scaleX, scaleY, offsetX, offsetY } = this._getImageGeometry();
        this._applyingCoordinates = true;
        clearTimeout(this._applyTimeout);
        this.selection.$change(
            coordinates.left * scaleX + offsetX,
            coordinates.top * scaleY + offsetY,
            coordinates.width * Math.abs(scaleX),
            coordinates.height * Math.abs(scaleY)
        );
        this._applyTimeout = setTimeout(() => { this._applyingCoordinates = false; }, 1000);
    }

    _selectionChanged(detail) {
        if (!detail) return;
        const { scaleX, scaleY, offsetX, offsetY } = this._getImageGeometry();
        this.coordinates = this._normalizeCoordinates({
            left: (detail.x - offsetX) / (scaleX || 1),
            top: (detail.y - offsetY) / (scaleY || 1),
            width: detail.width / Math.abs(scaleX || 1),
            height: detail.height / Math.abs(scaleY || 1)
        });
        this._updateCoordinateInputs();
        this._publishCoordinates();
    }

    _updateCoordinateInputs() {
        COORDINATE_KEYS.forEach(key => {
            const input = this.querySelector(`[data-coordinate="${key}"]`);
            if (input) input.value = this.coordinates[key];
        });
    }

    _getImageGeometry() {
        const canvas = this.cropper?.getCropperCanvas?.();
        if (!canvas || !this.cropperImage || !this.imageSize.width || !this.imageSize.height) {
            return { scaleX: 1, scaleY: 1, offsetX: 0, offsetY: 0 };
        }
        const canvasRect = canvas.getBoundingClientRect();
        const imageRect = this.cropperImage.getBoundingClientRect();
        const canvasScaleX = canvas.offsetWidth ? canvasRect.width / canvas.offsetWidth : 1;
        const canvasScaleY = canvas.offsetHeight ? canvasRect.height / canvas.offsetHeight : 1;
        return {
            scaleX: imageRect.width / canvasScaleX / this.imageSize.width,
            scaleY: imageRect.height / canvasScaleY / this.imageSize.height,
            offsetX: (imageRect.left - canvasRect.left) / canvasScaleX,
            offsetY: (imageRect.top - canvasRect.top) / canvasScaleY
        };
    }

    _publishCoordinates() {
        this.options.onChange?.({ ...this.coordinates });
        this.dispatchEvent(new CustomEvent("webjet-area-change", { detail: { ...this.coordinates }, bubbles: true }));
    }

    _normalizeCoordinates(coordinates = {}) {
        return {
            left: Math.round(Number(coordinates.left ?? coordinates.x) || 0),
            top: Math.round(Number(coordinates.top ?? coordinates.y) || 0),
            width: Math.round(Number(coordinates.width) || 0),
            height: Math.round(Number(coordinates.height) || 0)
        };
    }

    _setZoom(value) {
        this.zoom = Math.min(500, Math.max(25, value));
        const canvas = this.cropper?.getCropperCanvas?.();
        if (canvas) {
            canvas.style.transform = `scale(${this.zoom / 100})`;
            canvas.style.transformOrigin = "0 0";
        }
    }
}

if (!customElements.get("webjet-image-area-selector")) customElements.define("webjet-image-area-selector", WebjetImageAreaSelectorElement);
