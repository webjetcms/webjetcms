import { node, icon } from '../dashboard/widget-utils';
import { parseAiResponse } from './ai-response-parser';
import { chatRequestId } from './chat-history-api';
import { setChatTooltip, initChatTooltips, disposeChatTooltips } from './chat-tooltips';

const labels = {
    title: 'components.ai_assistants.chat.preview.title.js',
    pending: 'components.ai_assistants.chat.preview.pending.js',
    preview: 'editor.preview',
    code: 'temp_edit.HTMLCode',
    expand: 'datatables.modal.maximize.js',
    shrink: 'datatables.modal.minimize.js'
};
const text = key => WJ.translate(labels[key]);
const devices = [
    { width: 1280, key: 'pagebuilder.modal.visibility.xl', icon: 'device-desktop' },
    { width: 768, key: 'pagebuilder.modal.visibility.md', icon: 'device-tablet' },
    { width: 375, key: 'pagebuilder.modal.visibility.sm', icon: 'device-mobile' }
];
const editorClasses = /^(?:cke_.*|editorOn|page-builder|page-builder-in-admin|is-edit-mode|is-view-mode|is-toolbar-hidden)$/;

/** Copies presentation attributes without editor state, event handlers or executable content. */
function attributes(element) {
    if (!element) return {};
    const result = {};
    for (const name of ['id', 'class', 'lang', 'dir', 'data-bs-theme']) {
        const value = element.getAttribute(name);
        if (value) result[name] = name === 'class' ? value.split(/\s+/).filter(value => !editorClasses.test(value)).join(' ') : value;
    }
    return result;
}

/** Reads styles and content wrappers from the actual Page Builder or CKEditor document. */
export function editorPreviewContext(editor) {
    if (!editor) return null;
    const pageBuilder = editor.editingMode === 'pageBuilder';
    const doc = pageBuilder ? document.getElementById(`${editor.options.fieldid}-pageBuilderIframe`)?.contentDocument
        : editor.ckEditorInstance?.document?.$;
    const baseUrl = pageBuilder && doc?.URL !== 'about:blank' ? doc?.baseURI : editor.json?.editorFields?.editingModeLink;
    const styles = [];
    for (const element of doc?.head?.querySelectorAll('link[rel="stylesheet"], style') || []) {
        if (element.disabled) continue;
        if (element.id.startsWith('cke') || element.hasAttribute('data-cke')) continue;
        if (element.tagName === 'LINK') {
            const url = new URL(element.href, doc.baseURI);
            if (url.pathname.startsWith('/admin/')) continue;
            styles.push({ href: url.href, media: element.media });
        } else if (!/\.cke_|#cke_|inlineEditorToolbar/.test(element.textContent)) {
            styles.push({ text: element.textContent });
        }
    }
    // Source mode can detach the WYSIWYG iframe; its configured stylesheet list is still available.
    if (!styles.some(style => style.href)) {
        for (const href of editor.ckEditorInstance?.config?.contentsCss || []) {
            const url = new URL(href, window.location.href);
            if (!url.pathname.startsWith('/admin/')) styles.push({ href: url.href });
        }
    }
    const wrappers = [];
    if (pageBuilder) {
        for (let parent = doc?.getElementById('wjInline-docdata'); parent && parent !== doc.body; parent = parent.parentElement) {
            wrappers.unshift({ tag: parent.tagName.toLowerCase(), attributes: attributes(parent) });
        }
    }
    return { baseUrl: new URL(baseUrl || window.location.href, window.location.href).href, styles, wrappers,
        htmlAttributes: attributes(doc?.documentElement), bodyAttributes: attributes(doc?.body) };
}

/** Builds an inert document; the iframe sandbox and CSP remain the execution boundary. */
function previewDocument(html, context) {
    const doc = document.implementation.createHTMLDocument(text('title'));
    const policy = doc.createElement('meta');
    policy.httpEquiv = 'Content-Security-Policy';
    policy.content = "default-src 'none'; script-src 'none'; style-src 'unsafe-inline' http: https:; img-src http: https: data: blob:; font-src http: https: data:; form-action 'none'; base-uri http: https:;";
    doc.head.prepend(policy);
    const base = doc.createElement('base');
    const baseUrl = new URL(context?.baseUrl || window.location.href, window.location.href);
    base.href = ['http:', 'https:'].includes(baseUrl.protocol) ? baseUrl.href : window.location.href;
    doc.head.append(base);
    const setAttributes = (element, values) => {
        for (const [name, value] of Object.entries(values || {})) element.setAttribute(name, value);
    };
    setAttributes(doc.documentElement, context?.htmlAttributes);
    setAttributes(doc.body, context?.bodyAttributes);
    const defaults = doc.createElement('style');
    defaults.textContent = 'html { background: white; } body { margin: 0; } img { max-width: 100%; }';
    if (!context?.styles?.length) defaults.textContent += 'body { padding: 20px; font: 16px/1.5 system-ui, sans-serif; color: #13151b; } table { border-collapse: collapse; } th, td { padding: 8px; border: 1px solid #dddfe6; }';
    doc.head.append(defaults);
    for (const style of context?.styles || []) {
        const element = doc.createElement(style.href ? 'link' : 'style');
        if (style.href) {
            const url = new URL(style.href, base.href);
            if (!['http:', 'https:'].includes(url.protocol)) continue;
            element.rel = 'stylesheet';
            element.href = url.href;
            if (style.media) element.media = style.media;
        } else element.textContent = style.text;
        doc.head.append(element);
    }
    const template = document.createElement('template');
    template.innerHTML = parseAiResponse(html).html || '';
    template.content.querySelectorAll('script, iframe, frame, frameset, object, embed, base, meta, link, template, portal, animate, animateMotion, animateTransform, set').forEach(element => element.remove());
    for (const element of template.content.querySelectorAll('*')) {
        for (const attribute of [...element.attributes]) {
            if (/^on/i.test(attribute.name) || ['href', 'xlink:href', 'action', 'formaction', 'target', 'srcdoc', 'autofocus', 'contenteditable'].includes(attribute.name)) element.removeAttribute(attribute.name);
        }
        if (element.matches('button, input, select, textarea')) element.setAttribute('disabled', '');
    }
    let parent = doc.body;
    for (const wrapper of context?.wrappers || []) {
        const element = doc.createElement(wrapper.tag);
        setAttributes(element, wrapper.attributes);
        parent.append(element);
        parent = element;
    }
    parent.append(template.content);
    return '<!doctype html>\n' + doc.documentElement.outerHTML;
}

/** Owns one lazy HTML preview, its source tab, responsive viewport and expanded view. */
export class HtmlPreview {
    constructor(options) {
        this.options = options;
        this.view = 'preview';
        this.width = devices[0].width;
        this.element = node('details', 'ai-chat-html-response');
        const summary = node('summary');
        summary.append(icon('ti-layout'), node('span', '', text('title')), icon('ti-chevron-down ai-chat-html-chevron'));
        this.toolbar = node('div', 'ai-chat-html-toolbar');
        const tabs = node('div', 'ai-chat-html-tabs');
        tabs.setAttribute('role', 'tablist');
        tabs.setAttribute('aria-label', text('title'));
        const prefix = `ai-preview-${chatRequestId()}`;
        this.viewport = node('div', 'ai-chat-html-preview');
        this.code = node('pre');
        this.codeText = node('code');
        this.code.append(this.codeText);
        this.tabs = ['preview', 'code'].map((view, index) => {
            const button = this.button(text(view), null, () => this.show(view));
            const panel = view === 'preview' ? this.viewport : this.code;
            button.id = `${prefix}-${view}-tab`;
            panel.id = `${prefix}-${view}`;
            button.setAttribute('role', 'tab');
            button.setAttribute('aria-controls', panel.id);
            panel.setAttribute('role', 'tabpanel');
            panel.setAttribute('aria-labelledby', button.id);
            button.addEventListener('keydown', event => {
                if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
                event.preventDefault();
                const next = event.key === 'Home' ? 0 : event.key === 'End' ? 1 : 1 - index;
                this.tabs[next].click();
                this.tabs[next].focus();
            });
            tabs.append(button);
            return button;
        });
        this.devices = node('div', 'ai-chat-html-devices');
        this.deviceButtons = devices.map(device => {
            const button = this.iconButton(WJ.translate(device.key), device.icon, () => {
                this.width = device.width;
                this.resize();
            });
            this.devices.append(button);
            return button;
        });
        this.expand = this.iconButton(text('expand'), 'arrows-maximize', () => this.setExpanded(!this.overlay));
        this.expand.setAttribute('aria-expanded', 'false');
        this.toolbar.append(tabs, this.devices, this.expand);
        this.notice = node('div', 'ai-chat-html-notice', text('pending'));
        this.element.append(summary, this.toolbar, this.notice, this.viewport, this.code);
        this.element.addEventListener('toggle', () => {
            if (!this.element.open && this.overlay) {
                this.setExpanded(false, false);
                summary.focus({ preventScroll: true });
            }
            if (this.element.open) options.onOpen(this);
            if (this.element.open) initChatTooltips(this.element);
            else disposeChatTooltips(this.element);
            this.resume();
        });
        this.element.addEventListener('keydown', event => this.escape(event), true);
        this.observer = new ResizeObserver(() => this.resize());
        this.observer.observe(this.viewport);
        this.show('preview');
    }

    button(label, name, run) {
        const button = node('button', 'btn btn-sm btn-outline-secondary');
        button.type = 'button';
        if (name) button.append(icon(`ti-${name}`));
        button.append(node('span', '', label));
        button.addEventListener('click', run);
        return button;
    }

    iconButton(label, name, run) {
        const button = this.button(label, name, run);
        button.classList.add('ai-chat-icon-button');
        button.querySelector('span').classList.add('visually-hidden');
        button.setAttribute('aria-label', label);
        setChatTooltip(button, label);
        return button;
    }

    update(html, complete) {
        if (this.html !== html) {
            this.html = html;
            this.codeText.textContent = html;
            this.removeFrame();
        }
        const ready = complete && !this.complete;
        this.complete = complete;
        this.notice.hidden = complete;
        this.tabs[0].disabled = !complete;
        this.expand.disabled = !complete;
        if (ready) {
            this.element.open = true;
            this.options.onOpen(this);
        }
        this.resume();
    }

    show(view) {
        this.view = view;
        this.tabs.forEach((tab, index) => {
            const selected = index === (view === 'preview' ? 0 : 1);
            tab.setAttribute('aria-selected', String(selected));
            tab.tabIndex = selected ? 0 : -1;
        });
        this.viewport.hidden = view !== 'preview';
        this.code.hidden = view !== 'code';
        this.devices.hidden = view !== 'preview';
        initChatTooltips(this.element);
        this.resume();
    }

    resume() {
        if (!this.element.open || this.view !== 'preview' || !this.complete || !this.element.isConnected) {
            this.removeFrame();
            return;
        }
        if (!this.frame) {
            const frame = node('iframe');
            frame.title = text('title');
            // Parent access permits keyboard dismissal; generated content cannot execute scripts.
            frame.setAttribute('sandbox', 'allow-same-origin');
            frame.setAttribute('referrerpolicy', 'no-referrer');
            frame.addEventListener('load', () => {
                if (this.frame !== frame) return;
                frame.contentDocument?.addEventListener('keydown', event => this.escape(event));
                frame.contentDocument?.addEventListener('click', event => event.preventDefault());
                frame.contentDocument?.addEventListener('submit', event => event.preventDefault());
            });
            frame.srcdoc = previewDocument(this.html, this.options.getContext?.());
            this.frame = frame;
            this.viewport.append(frame);
        }
        this.resize();
    }

    resize() {
        this.deviceButtons.forEach((button, index) => button.setAttribute('aria-pressed', String(this.width === devices[index].width)));
        const height = this.overlay ? `${Math.max(100, this.element.getBoundingClientRect().bottom - this.toolbar.getBoundingClientRect().bottom - 2)}px` : '';
        this.viewport.style.height = height;
        this.code.style.height = height;
        if (!this.frame || !this.viewport.clientWidth) return;
        const scale = Math.min(1, this.viewport.clientWidth / this.width);
        Object.assign(this.frame.style, { width: `${this.width}px`, height: `${this.viewport.clientHeight / scale}px`,
            transform: `scale(${scale})`, left: `${Math.max(0, (this.viewport.clientWidth - this.width) / 2)}px` });
    }

    /** Expands inside this chat without creating another modal or changing the editor. */
    setExpanded(expanded, focus = true) {
        if (expanded && !this.overlay) {
            this.element.open = true;
            this.placeholder = document.createComment('HTML preview');
            this.element.before(this.placeholder);
            this.overlay = node('div', 'ai-chat-html-expanded');
            const root = this.options.root;
            this.inert = [...root.children].map(element => ({ element, value: element.inert }));
            this.inert.forEach(({ element }) => { element.inert = true; });
            root.append(this.overlay);
            this.overlay.append(this.element);
        } else if (!expanded && this.overlay) {
            this.placeholder.replaceWith(this.element);
            this.overlay.remove();
            this.overlay = null;
            this.inert.forEach(({ element, value }) => { element.inert = value; });
        }
        const label = text(this.overlay ? 'shrink' : 'expand');
        this.expand.querySelector('span').textContent = label;
        this.expand.querySelector('.ti').className = `ti ti-arrows-${this.overlay ? 'minimize' : 'maximize'}`;
        this.expand.setAttribute('aria-label', label);
        setChatTooltip(this.expand, label);
        this.expand.setAttribute('aria-expanded', String(Boolean(this.overlay)));
        initChatTooltips(this.element);
        this.resize();
        if (focus) WJ.focusWithoutTooltip(this.expand);
    }

    escape(event) {
        if (event.key !== 'Escape' || !this.overlay) return;
        event.preventDefault();
        event.stopPropagation();
        this.setExpanded(false);
    }

    removeFrame() {
        this.frame?.remove();
        this.frame = null;
    }

    suspend() {
        this.setExpanded(false, false);
        disposeChatTooltips(this.element);
        this.removeFrame();
    }

    destroy() {
        this.suspend();
        this.element.open = false;
        this.observer.disconnect();
        this.element.remove();
    }
}
