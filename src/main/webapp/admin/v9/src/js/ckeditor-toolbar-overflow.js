const openToolbars = new WeakMap();

/** Keeps CKEditor's original toolbar groups in one ribbon, with an overflow palette. */
export class CkEditorToolbarOverflow {
    /**
     * Enhance one rendered editor; its original controls and commands remain authoritative.
     * @param {object} editor CKEditor instance whose toolbar has rendered.
     * @param {object} CKEDITOR CKEditor namespace belonging to the editor's window.
     * @param {{more: string, close: string}} labels Translated labels for the palette controls.
     */
    constructor(editor, CKEDITOR, labels) {
        this.editor = editor;
        this.CKEDITOR = CKEDITOR;
        this.toolbox = editor.ui.space('toolbox')?.$;
        if (!this.toolbox || !editor.toolbox) return;

        this.doc = this.toolbox.ownerDocument;
        this.win = this.doc.defaultView;
        this.top = this.toolbox.closest('.cke_top');
        this.cleanups = [];
        this.panels = new Map();
        this.groups = editor.toolbox.toolbars.map(toolbar => {
            const element = this.doc.getElementById(toolbar.id);
            const anchor = this.doc.createComment('toolbar group');
            element.before(anchor);
            return { toolbar, element, anchor, priority: !!element.querySelector('.cke_button__wjstyles, .cke_combo__styles, .cke_combo__format, .cke_button__bold') };
        });
        if (!this.groups.length) return;

        const host = this.top.closest('#wjInlineCkEditorToolbarOffsetElement, .DTE_Field');
        this.selector = host?.querySelector('.exit-inline-editor');
        this.row = this.doc.createElement('span');
        this.row.className = 'wj-cke-toolbar__groups';
        this.toolbox.append(this.row);
        this.groups.forEach(group => this.row.append(group.element));

        this.button = this.doc.createElement('button');
        this.button.type = 'button';
        this.button.className = 'wj-cke-toolbar__more';
        this.button.textContent = '…';
        this.button.title = labels.more;
        this.button.setAttribute('aria-label', labels.more);
        this.button.setAttribute('aria-haspopup', 'dialog');
        this.button.setAttribute('aria-expanded', 'false');
        this.toolbox.append(this.button);

        this.palette = this.doc.createElement('div');
        this.palette.id = editor.id + '-more-tools';
        this.palette.className = 'wj-cke-overflow cke cke_reset cke_reset_all cke_' + editor.lang.dir;
        this.palette.setAttribute('role', 'dialog');
        this.palette.setAttribute('aria-label', labels.more);
        this.palette.hidden = true;
        this.button.setAttribute('aria-controls', this.palette.id);
        const header = this.doc.createElement('div');
        header.className = 'wj-cke-overflow__header';
        const title = this.doc.createElement('span');
        title.textContent = labels.more;
        this.dismiss = this.doc.createElement('button');
        this.dismiss.type = 'button';
        this.dismiss.className = 'wj-cke-overflow__close';
        this.dismiss.textContent = '×';
        this.dismiss.title = labels.close;
        this.dismiss.setAttribute('aria-label', labels.close);
        header.append(title, this.dismiss);
        this.body = this.doc.createElement('div');
        this.body.className = 'wj-cke-overflow__groups';
        this.palette.append(header, this.body);
        (this.toolbox.closest('.modal, [role="dialog"]') || this.doc.body).append(this.palette);
        this.focusElement = new CKEDITOR.dom.element(this.palette);
        editor.focusManager.add(this.focusElement, true);

        this.top.classList.add('wj-cke-toolbar-top');
        this.toolbox.classList.add('wj-cke-toolbar');
        this.listen(this.button, 'click', () => this.palette.hidden ? this.open() : this.close(true));
        this.listen(this.dismiss, 'click', () => this.close(true));
        // Native CKEditor buttons prevent mousedown from replacing the content selection.
        this.listen(this.palette, 'mousedown', event => {
            if (event.target.closest('.wj-cke-overflow__groups, .wj-cke-overflow__header')) event.preventDefault();
        });
        this.listen(this.doc, 'mousedown', event => {
            if (!this.palette.contains(event.target) && !this.button.contains(event.target) &&
                ![...this.panels.keys()].some(panel => panel.element.$.contains(event.target))) this.close();
        });
        this.listen(this.toolbox, 'keydown', event => this.navigate(event), true);
        this.listen(this.palette, 'keydown', event => this.navigate(event), true);
        // Run before the PageBuilder document-level Escape handler.
        this.listen(this.win, 'keydown', event => {
            if (event.key !== 'Escape' || this.palette.hidden || event.target.closest('.wj-styles, .cke_panel')) return;
            event.preventDefault();
            event.stopImmediatePropagation();
            this.close(true);
        }, true);
        this.listen(this.win, 'resize', () => this.schedule());
        this.listen(this.win, 'scroll', () => this.position(), true);
        try {
            if (this.win.frameElement) {
                this.listen(this.win.parent, 'resize', () => this.schedule());
                this.listen(this.win.parent, 'scroll', () => this.position(), true);
                this.listen(this.win.frameElement.ownerDocument, 'mousedown', () => this.close());
            }
        } catch (error) { /* Cross-origin hosts expose only their own viewport. */ }

        this.originalFocus = editor.toolbox.focus;
        editor.toolbox.focus = () => this.focusGroup(this.groups.find(group => group.element.parentElement === this.row));
        this.on('focus', () => {
            const previous = openToolbars.get(this.doc);
            if (previous && previous !== this) previous.close();
            this.schedule();
        });
        this.on('dialogShow', () => this.close());
        this.on('beforeModeUnload', () => this.close());
        this.on('readOnly', () => this.close());
        this.on('key', event => {
            if (event.data.keyCode === 27 && !this.palette.hidden) {
                this.close(true);
                event.cancel();
            }
        }, 1);
        this.on('beforeCommandExec', event => {
            if (!this.palette.hidden && /FloatingTools$/.test(event.data.name)) return false;
        }, 1);
        this.on('panelShow', event => {
            if (this.palette.hidden) return;
            const panel = event.data;
            if (!this.panels.has(panel)) this.panels.set(panel, panel.element.$.style.zIndex);
            panel.element.setStyle('z-index', Math.max(parseInt(panel.element.$.style.zIndex, 10) || 0, this.layer() + 10));
        });
        this.on('panelHide', event => {
            if (!this.panels.has(event.data)) return;
            event.data.element.$.style.zIndex = this.panels.get(event.data);
            this.panels.delete(event.data);
        });
        const bindContent = () => editor.editable()?.attachListener(editor.document, 'mousedown', event => {
            if (editor.editable().contains(event.data.getTarget())) this.close();
        });
        bindContent();
        this.on('contentDom', bindContent);
        this.observer = new this.win.ResizeObserver(() => this.schedule());
        this.observer.observe(this.top);
        if (this.selector) this.observer.observe(this.selector);
        this.groups.forEach(group => this.observer.observe(group.element));
        this.on('destroy', () => this.destroy(), 1);
        this.layout();
    }

    listen(target, name, callback, capture = false) {
        target.addEventListener(name, callback, capture);
        this.cleanups.push(() => target.removeEventListener(name, callback, capture));
    }

    on(name, callback, priority = 10) {
        const listener = this.editor.on(name, callback, null, null, priority);
        this.cleanups.push(() => listener.removeListener());
    }

    schedule() {
        if (this.frame || this.destroyed) return;
        this.frame = this.win.requestAnimationFrame(() => { this.frame = null; this.layout(); });
    }

    /** Measure actual groups and the mode selector, including custom toolbar configurations. */
    layout() {
        if (!this.toolbox.getClientRects().length) { this.close(); return; }
        const rect = this.toolbox.getBoundingClientRect();
        let end = 8;
        if (this.selector?.getClientRects().length) {
            const selector = this.selector.getBoundingClientRect();
            if (selector.left < rect.right && selector.right > rect.left && selector.top < rect.bottom && selector.bottom > rect.top) end = rect.right - selector.left + 8;
        }
        this.toolbox.style.setProperty('--wj-cke-toolbar-end', end + 'px');
        // Hidden groups remain measurable without cloning controls or moving focused nodes.
        this.palette.classList.toggle('wj-cke-overflow--measure', this.palette.hidden);
        this.groups.forEach(group => {
            const style = this.win.getComputedStyle(group.element);
            group.width = group.element.getBoundingClientRect().width + parseFloat(style.marginLeft) + parseFloat(style.marginRight);
        });
        const height = Math.max(...this.groups.map(group => group.element.offsetHeight));
        this.row.style.minHeight = height + 'px';
        this.palette.classList.remove('wj-cke-overflow--measure');
        const available = this.toolbox.clientWidth - 8 - end;
        let width = this.groups.reduce((sum, group) => sum + group.width, 0);
        const hidden = new Set();
        if (width > available) {
            const order = this.groups.slice().reverse();
            const removal = order.filter(group => !group.priority).concat(order.filter(group => group.priority));
            for (const group of removal) {
                if (width <= available - 38) break;
                hidden.add(group);
                width -= group.width;
            }
        }
        this.button.hidden = hidden.size === 0;
        const changed = this.groups.some(group => hidden.has(group) !== (group.element.parentElement === this.body));
        if (changed) {
            const focused = this.doc.activeElement;
            this.close();
            for (const host of [this.row, this.body]) {
                let next = host.firstElementChild;
                this.groups.filter(group => (hidden.has(group) ? this.body : this.row) === host).forEach(group => {
                    if (group.element !== next) host.insertBefore(group.element, next);
                    next = group.element.nextElementSibling;
                });
            }
            if (focused && this.body.contains(focused)) this.button.focus();
        }
        if (this.button.hidden) this.close();
        this.position();
    }

    layer() {
        let layer = Math.max(this.editor.config.baseFloatZIndex || 10000, 101100);
        for (let node = this.toolbox; node; node = node.parentElement) layer = Math.max(layer, parseInt(this.win.getComputedStyle(node).zIndex, 10) || 0);
        return layer + 100;
    }

    position() {
        if (this.palette.hidden) return;
        const rect = this.button.getBoundingClientRect();
        const toolbar = this.top.getBoundingClientRect();
        let left = 0, right = this.win.innerWidth, bottom = this.win.innerHeight;
        try {
            if (this.win.frameElement) {
                const frame = this.win.frameElement.getBoundingClientRect();
                left = Math.max(0, -frame.left);
                right = Math.min(right, this.win.parent.innerWidth - frame.left);
                bottom = Math.min(bottom, this.win.parent.innerHeight - frame.top);
            }
        } catch (error) { /* Cross-origin hosts retain the iframe bounds. */ }
        this.palette.style.zIndex = this.layer();
        this.palette.style.width = Math.max(0, Math.min(Math.max(340, ...this.groups.map(group => group.width + 24)), right - left - 16)) + 'px';
        this.palette.style.left = Math.max(left + 8, Math.min(rect.right - this.palette.offsetWidth, right - this.palette.offsetWidth - 8)) + 'px';
        const below = bottom - toolbar.bottom - 12;
        const above = toolbar.top - 12;
        const flip = below < 220 && above > below;
        this.body.style.maxHeight = Math.max(40, Math.min(420, (flip ? above : below) - 46)) + 'px';
        this.palette.style.top = (flip ? Math.max(8, toolbar.top - this.palette.offsetHeight - 4) : toolbar.bottom + 4) + 'px';
    }

    open() {
        const previous = openToolbars.get(this.doc);
        if (previous && previous !== this) previous.close();
        this.palette.hidden = false;
        this.button.setAttribute('aria-expanded', 'true');
        openToolbars.set(this.doc, this);
        this.body.scrollTop = 0;
        this.position();
        this.focusGroup(this.groups.find(group => group.element.parentElement === this.body));
    }

    close(focus = false) {
        if (this.palette.hidden) return;
        for (const panel of [...this.panels.keys()]) panel.hide(false);
        const picker = this.editor.wjstylescombo;
        if (picker?.panel && this.palette.contains(picker.panel)) picker.close(false);
        this.palette.hidden = true;
        this.button.setAttribute('aria-expanded', 'false');
        if (openToolbars.get(this.doc) === this) openToolbars.delete(this.doc);
        if (focus) this.button.focus();
    }

    focusGroup(group) {
        const item = group?.toolbar.items.find(item => typeof item.focus === 'function');
        if (item) item.focus();
        else this.button.focus();
    }

    /** Preserve native arrow/command handling; Tab only visits currently visible groups. */
    navigate(event) {
        if (event.target.closest('.wj-styles, .cke_panel')) return;
        if (event.target === this.button && event.key === 'Escape') {
            event.preventDefault();
            event.stopImmediatePropagation();
            this.editor.focus();
            return;
        }
        if (event.target === this.button && ['ArrowDown', 'ArrowUp'].includes(event.key)) {
            event.preventDefault();
            event.stopImmediatePropagation();
            this.open();
            return;
        }
        if (event.key !== 'Tab') return;
        const inPalette = this.palette.contains(event.target);
        const host = inPalette ? this.body : this.row;
        const groups = this.groups.filter(group => group.element.parentElement === host);
        const entries = groups.map(group => ({ element: group.element, focus: () => this.focusGroup(group) }));
        const end = inPalette ? this.dismiss : this.button;
        if (!end.hidden) entries.push({ element: end, focus: () => end.focus() });
        const index = entries.findIndex(entry => entry.element.contains(event.target));
        if (index < 0 || !entries.length) return;
        event.preventDefault();
        event.stopImmediatePropagation();
        entries[(index + (event.shiftKey ? -1 : 1) + entries.length) % entries.length].focus();
    }

    destroy() {
        this.destroyed = true;
        this.close();
        this.win.cancelAnimationFrame(this.frame);
        this.observer.disconnect();
        this.cleanups.forEach(cleanup => cleanup());
        this.editor.focusManager.remove(this.focusElement);
        this.editor.toolbox.focus = this.originalFocus;
        this.groups.forEach(group => { group.anchor.replaceWith(group.element); });
        this.row.remove();
        this.button.remove();
        this.palette.remove();
        this.top.classList.remove('wj-cke-toolbar-top');
        this.toolbox.classList.remove('wj-cke-toolbar');
        this.toolbox.style.removeProperty('--wj-cke-toolbar-end');
    }
}
