import { disposeChatTooltips, initChatTooltips, setChatTooltip } from './chat-tooltips';

/** Keeps an editor chat mounted while its window is moved, maximized or minimized. */
export class EditorChatWindow {
    constructor(dialog, returnFocus, onRestore) {
        this.dialog = dialog;
        this.returnFocus = returnFocus;
        this.onRestore = onRestore;
        this.header = dialog.querySelector('.toast-title');
        this.controller = new AbortController();
        const options = { signal: this.controller.signal };
        this.originalStyle = {};
        for (const name of ['position', 'left', 'top', 'margin']) this.originalStyle[name] = dialog.style[name];

        this.minimizeButton = this._button('components.ai_assistants.chat.minimize.js', 'minus', 'ai-chat-minimize btn', () => this.minimize());
        this.maximizeButton = this._button('datatables.modal.maximize.js', 'arrows-maximize', 'ai-chat-maximize btn', () => this.toggleMaximize());
        this.maximizeButton.setAttribute('aria-pressed', 'false');
        this.restoreButton = this._button('components.ai_assistants.chat.restore.js', 'sparkles', 'ai-chat-restore btn btn-primary', () => this.restore());
        this.restoreButton.hidden = true;
        dialog.append(this.minimizeButton, this.maximizeButton);
        dialog.parentElement.append(this.restoreButton);
        initChatTooltips(dialog);

        this.header.tabIndex = 0;
        this.header.setAttribute('aria-label', WJ.translate('components.ai_assistants.chat.move.js'));
        this.header.addEventListener('pointerdown', event => this._startDrag(event), options);
        this.header.addEventListener('pointermove', event => {
            if (this.drag?.id !== event.pointerId) return;
            this._move(event.clientX - this.drag.x, event.clientY - this.drag.y);
        }, options);
        for (const name of ['pointerup', 'pointercancel', 'lostpointercapture']) {
            this.header.addEventListener(name, () => this._endDrag(), options);
        }
        this.header.addEventListener('keydown', event => {
            if (event.target !== this.header || this.maximized) return;
            const direction = { ArrowLeft: [-1, 0], ArrowRight: [1, 0], ArrowUp: [0, -1], ArrowDown: [0, 1] }[event.key];
            if (!direction) return;
            event.preventDefault();
            event.stopPropagation();
            const bounds = dialog.getBoundingClientRect();
            const step = event.shiftKey ? 80 : 20;
            this._move(bounds.left + direction[0] * step, bounds.top + direction[1] * step);
        }, options);
        dialog.addEventListener('focusin', event => {
            if (event.target !== this.minimizeButton && event.target !== this.maximizeButton) this.lastFocus = event.target;
        }, options);
        window.addEventListener('resize', () => this._keepVisible(), options);
    }

    _button(labelKey, icon, className, callback) {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = className;
        button.setAttribute('aria-label', WJ.translate(labelKey));
        button.innerHTML = `<i class="ti ti-${icon}" aria-hidden="true"></i>`;
        button.addEventListener('click', callback, { signal: this.controller.signal });
        setChatTooltip(button, button.getAttribute('aria-label'));
        return button;
    }

    _startDrag(event) {
        if (this.maximized || event.button !== 0 || !event.isPrimary || event.target.closest('button, a, input, select, textarea')) return;
        event.preventDefault();
        const bounds = this.dialog.getBoundingClientRect();
        this.drag = { id: event.pointerId, x: event.clientX - bounds.left, y: event.clientY - bounds.top };
        this.header.setPointerCapture(event.pointerId);
        this.dialog.classList.add('ai-chat-dragging');
    }

    _endDrag() {
        const pointerId = this.drag?.id;
        this.drag = null;
        if (pointerId != null && this.header.hasPointerCapture(pointerId)) this.header.releasePointerCapture(pointerId);
        this.dialog.classList.remove('ai-chat-dragging');
    }

    /** Constrains the whole window to the viewport, including after a resize. */
    _move(left, top) {
        const bounds = this.dialog.getBoundingClientRect();
        const gap = 8;
        this.position = {
            left: Math.max(gap, Math.min(left, window.innerWidth - bounds.width - gap)),
            top: Math.max(gap, Math.min(top, window.innerHeight - bounds.height - gap))
        };
        Object.assign(this.dialog.style, {
            position: 'fixed', margin: '0', left: `${this.position.left}px`, top: `${this.position.top}px`
        });
    }

    _keepVisible() {
        if (this.position && !this.dialog.hidden && !this.maximized) this._move(this.position.left, this.position.top);
    }

    /** Fills the viewport while retaining the normal window's position for restoration. */
    toggleMaximize() {
        this._endDrag();
        this.maximized = !this.maximized;
        if (this.maximized) {
            this.normalStyle = {};
            for (const name of Object.keys(this.originalStyle)) this.normalStyle[name] = this.dialog.style[name];
            Object.assign(this.dialog.style, { position: 'fixed', left: '0', top: '0', margin: '0' });
        } else {
            Object.assign(this.dialog.style, this.normalStyle);
        }
        this.dialog.classList.toggle('ai-chat-maximized', this.maximized);
        this.header.tabIndex = this.maximized ? -1 : 0;
        const label = WJ.translate(this.maximized ? 'datatables.modal.minimize.js' : 'datatables.modal.maximize.js');
        this.maximizeButton.querySelector('.ti').className = `ti ti-arrows-${this.maximized ? 'minimize' : 'maximize'}`;
        this.maximizeButton.setAttribute('aria-label', label);
        this.maximizeButton.setAttribute('aria-pressed', String(this.maximized));
        setChatTooltip(this.maximizeButton, label);
        this._keepVisible();
        WJ.focusWithoutTooltip(this.maximizeButton);
    }

    minimize() {
        this._endDrag();
        disposeChatTooltips(this.dialog);
        this.dialog.hidden = true;
        this.restoreButton.hidden = false;
        initChatTooltips(this.restoreButton);
        this.returnFocus();
    }

    restore() {
        disposeChatTooltips(this.restoreButton);
        this.restoreButton.hidden = true;
        this.dialog.hidden = false;
        this._keepVisible();
        this.onRestore();
        initChatTooltips(this.dialog);
        const target = this.lastFocus?.isConnected ? this.lastFocus : this.dialog.querySelector('.ai-chat-input');
        target?.focus({ preventScroll: true });
    }

    /** Removes controls and listeners when the shared assistant dialog changes owners. */
    destroy() {
        this._endDrag();
        this.controller.abort();
        disposeChatTooltips(this.minimizeButton);
        disposeChatTooltips(this.maximizeButton);
        disposeChatTooltips(this.restoreButton);
        this.minimizeButton.remove();
        this.maximizeButton.remove();
        this.restoreButton.remove();
        this.dialog.hidden = false;
        this.dialog.classList.remove('ai-chat-maximized');
        this.header.removeAttribute('tabindex');
        this.header.removeAttribute('aria-label');
        Object.assign(this.dialog.style, this.originalStyle);
    }
}
