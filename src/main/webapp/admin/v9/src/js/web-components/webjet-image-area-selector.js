import Cropper from 'cropperjs';

/**
 * Coordinates accepted from configuration in original-image pixels.
 *
 * @typedef {Object} WebjetImageAreaCoordinateInput
 * @property {number|string|null} [left] - Horizontal offset, preferred over `x` when present.
 * @property {number|string|null} [top] - Vertical offset, preferred over `y` when present.
 * @property {number|string|null} [x] - Horizontal-offset alias.
 * @property {number|string|null} [y] - Vertical-offset alias.
 * @property {number|string|null} [width] - Selection width.
 * @property {number|string|null} [height] - Selection height.
 */

/**
 * Normalized image-area coordinates in original-image pixels.
 *
 * @typedef {Object} WebjetImageAreaCoordinates
 * @property {number} left - Horizontal offset.
 * @property {number} top - Vertical offset.
 * @property {number} width - Selection width.
 * @property {number} height - Selection height.
 */

/**
 * Geometry used to convert between the rendered image and original-image pixels.
 *
 * @typedef {Object} WebjetImageAreaGeometry
 * @property {number} scaleX - Horizontal original-to-rendered scale.
 * @property {number} scaleY - Vertical original-to-rendered scale.
 * @property {number} offsetX - Rendered horizontal image offset within the cropper canvas.
 * @property {number} offsetY - Rendered vertical image offset within the cropper canvas.
 */

/**
 * Configuration for the image-area selector.
 *
 * @typedef {Object} WebjetImageAreaSelectorOptions
 * @property {function(): (string|null|undefined)} [getImageUrl] - Returns the image URL to load when the selector is refreshed.
 * @property {function(): (WebjetImageAreaCoordinateInput|undefined)} [getCoordinates] - Returns the current selection in original-image pixels.
 * @property {function(WebjetImageAreaCoordinates): void} [onChange] - Receives normalized coordinates after the selection changes.
 * @property {Object|null} [labels] - Labels displayed above the coordinate inputs.
 * @property {string} [labels.x=""] - Horizontal-offset label.
 * @property {string} [labels.y=""] - Vertical-offset label.
 * @property {string} [labels.width=""] - Width label.
 * @property {string} [labels.height=""] - Height label.
 * @property {string} [labels.zoom=""] - Zoom label.
 */

const COORDINATE_KEYS = ["left", "top", "width", "height"];

/**
 * Displays and edits an image selection in original-image pixel coordinates.
 */
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

    /**
     * Applies callbacks and labels, then renders immediately when connected.
     *
     * @param {WebjetImageAreaSelectorOptions} [options={}] - Component options.
     * @returns {WebjetImageAreaSelectorElement} The configured element.
     */
    configure(options = {}) {
        this.options = options;
        this._configured = true;
        if (this.isConnected) this.render();
        return this;
    }

    /**
     * Rebuilds the coordinate controls and cropper container.
     */
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

    /**
     * Returns the selector to its loading state and releases the active cropper.
     */
    deactivate() {
        this.dataset.ready = "false";
        this.querySelector(".loading")?.removeAttribute("hidden");
        this.querySelector(".ready")?.setAttribute("hidden", "");
        this._destroyCropper();
    }

    /**
     * Loads the current image and selection when an image URL is available.
     */
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

    /**
     * Initializes the cropper for a loaded image and restores the configured selection.
     *
     * Emits a bubbling, non-cancelable `webjet-component-ready` event without detail
     * after the cropper controls become available.
     *
     * @param {HTMLImageElement} image - Loaded source image.
     */
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

    /**
     * Converts original-image coordinates to rendered geometry and applies the selection.
     *
     * @param {WebjetImageAreaCoordinates} coordinates - Selection in original-image pixels.
     */
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

    /**
     * Converts a cropper selection to original-image coordinates and publishes it.
     *
     * @param {{x: number, y: number, width: number, height: number}|null} detail - Current cropper selection.
     */
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

    /**
     * Calculates the rendered image scale and offset relative to the cropper canvas.
     *
     * @returns {WebjetImageAreaGeometry} The current image geometry, or identity geometry before initialization.
     */
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

    /**
     * Publishes a copy of the normalized coordinates to configured consumers.
     *
     * Invokes the configured `onChange` callback and emits a bubbling, non-cancelable
     * `webjet-area-change` event with `WebjetImageAreaCoordinates` as its detail.
     */
    _publishCoordinates() {
        this.options.onChange?.({ ...this.coordinates });
        this.dispatchEvent(new CustomEvent("webjet-area-change", { detail: { ...this.coordinates }, bubbles: true }));
    }

    /**
     * Coerces and rounds coordinates, accepting `x` and `y` as offset aliases.
     *
     * @param {WebjetImageAreaCoordinateInput} [coordinates={}] - Coordinate values to normalize.
     * @returns {WebjetImageAreaCoordinates} Normalized rounded coordinates.
     */
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
