import { MAX_WIDGETS, cloneSettings, normalizeSettings, moveInstanceBefore } from './model';
import { node, icon } from './widget-utils';

/** Owns the provisional widget layout and interactions during an overview edit session. */
export class DashboardEditor {
    constructor(dashboard) {
        this.dashboard = dashboard;
        this.original = cloneSettings(dashboard.settings);
        this.history = [];
        this.reset = false;
        this.onScroll = () => this.positionToolbar();
        this.scrollbar = window.scrollbarMain;
        this.scrollbar?.addListener(this.onScroll);
        window.addEventListener('resize', this.onScroll);
        this.onBeforeUnload = event => {
            if (!this.dirty) return;
            event.preventDefault();
            event.returnValue = '';
        };
        window.addEventListener('beforeunload', this.onBeforeUnload);
        this.onKeydown = event => {
            if (event.key === 'Escape' && this.move) {
                event.preventDefault();
                this.finishMove(false);
            }
            if ((event.ctrlKey || event.metaKey) && !event.shiftKey && event.key.toLowerCase() === 'z' && !event.target.closest('input, textarea, select, [contenteditable="true"], [role="dialog"]')) {
                event.preventDefault();
                this.undo();
            }
        };
        document.addEventListener('keydown', this.onKeydown);
        this.positionToolbar();
    }

    get dirty() { return this.reset || JSON.stringify(this.original) !== JSON.stringify(this.dashboard.settings); }

    /** Saves an undo step in memory and updates only widgets whose render inputs changed. */
    stage(next, reset = this.reset) {
        if (JSON.stringify(next) === JSON.stringify(this.dashboard.settings) && reset === this.reset) return true;
        this.history.push({ settings: cloneSettings(this.dashboard.settings), reset: this.reset });
        this.reset = reset;
        this.dashboard.settings = normalizeSettings(next);
        this.dashboard.overviewStatus.textContent = '';
        this.render();
        return true;
    }

    /** Copies independently saved shortcuts and fixed preferences without committing the widget draft. */
    mergeIndependent(target, saved) {
        const result = cloneSettings(target);
        const independent = item => this.dashboard._region(item) !== 'grid';
        const kept = saved.items.filter(independent);
        result.items = result.items.filter(item => !independent(item) || kept.some(value => value.id === item.id))
            .map(item => independent(item) ? cloneSettings(kept.find(value => value.id === item.id)) : item);
        for (const item of kept) {
            if (!result.items.some(value => value.id === item.id)) result.items.push(cloneSettings(item));
            if (saved.domainOptions[item.id]) result.domainOptions[item.id] = cloneSettings(saved.domainOptions[item.id]);
            else delete result.domainOptions[item.id];
        }
        for (const item of target.items.filter(independent)) {
            if (!kept.some(value => value.id === item.id)) delete result.domainOptions[item.id];
        }
        result.acknowledgedNewsVersion = saved.acknowledgedNewsVersion;
        result.shortcutsConfigured = saved.shortcutsConfigured;
        result.legacyBookmarksHandled = saved.legacyBookmarksHandled;
        result.configured = saved.configured;
        return result;
    }

    sync(saved) {
        this.original = this.mergeIndependent(this.original, saved);
        this.dashboard.settings = this.mergeIndependent(this.dashboard.settings, saved);
        this.history.forEach(step => { step.settings = this.mergeIndependent(step.settings, saved); });
    }

    undo() {
        if (this.dashboard.saving) return false;
        this.finishMove(false);
        const previous = this.history.pop();
        if (!previous) return false;
        this.dashboard.settings = previous.settings;
        this.reset = previous.reset;
        this.clearToast();
        this.render();
        this.dashboard.widgetMoveStatus.textContent = this.dashboard._t('changeUndone', 'The last change was undone.');
        return true;
    }

    /** Animates changed card positions while preserving DOM and mobile reading order. */
    render() {
        const positions = new Map(this.cards().map(card => [card.dataset.instanceId, card.getBoundingClientRect()]));
        this.dashboard._render();
        if (!window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) {
            for (const card of this.cards()) {
                const previous = positions.get(card.dataset.instanceId);
                const current = card.getBoundingClientRect();
                if (previous && (current.left !== previous.left || current.top !== previous.top)) card.animate?.([
                    { transform: `translate(${previous.left - current.left}px, ${previous.top - current.top}px)` }, { transform: 'none' }
                ], { duration: 150, easing: 'ease-out' });
            }
        }
        window.scrollbarMain?.update();
        this.positionToolbar();
    }

    cards() { return [...this.dashboard.layout.querySelectorAll('[data-instance-id]')]; }

    /** Counteracts the administration's transformed scroll content; native scrolling uses CSS sticky. */
    positionToolbar() {
        if (!this.scrollbar) return;
        const toolbar = this.dashboard.toolbar;
        const bounds = toolbar.getBoundingClientRect();
        const top = bounds.top - (this.toolbarShift || 0);
        this.toolbarShift = Math.max(0, Math.min(48 - top, this.dashboard.host.getBoundingClientRect().bottom - top - bounds.height));
        toolbar.style.transform = `translateY(${this.toolbarShift}px)`;
        toolbar.classList.toggle('is-stuck', this.toolbarShift > 0);
    }

    /** Shows an eight-second removal notification; hover and keyboard focus pause its timer. */
    showRemoval(id) {
        this.clearToast();
        const toast = node('div', 'md-dashboard__notice-toast md-dashboard__widget-toast');
        const status = node('span', '', this.dashboard._t('removed', 'Widget removed.'));
        status.setAttribute('role', 'status');
        const undo = node('button', 'btn btn-sm btn-link', this.dashboard._t('undo', 'Undo'));
        undo.type = 'button';
        undo.dataset.dashboardWidgetUndo = 'true';
        const step = this.history.at(-1);
        undo.addEventListener('click', () => {
            const previous = step.settings;
            const restored = previous.items.find(item => item.id === id);
            if (!restored || this.dashboard._instance(id)) return;
            if (this.dashboard.settings.items.length >= MAX_WIDGETS) {
                this.dashboard.overviewStatus.textContent = this.dashboard._t('limit', 'The overview can contain at most 48 widgets.');
                return;
            }
            const next = cloneSettings(this.dashboard.settings);
            const following = previous.items.slice(previous.items.indexOf(restored) + 1).find(item => next.items.some(value => value.id === item.id));
            next.items.splice(following ? next.items.findIndex(item => item.id === following.id) : next.items.length, 0, cloneSettings(restored));
            if (previous.domainOptions[id]) next.domainOptions[id] = cloneSettings(previous.domainOptions[id]);
            this.stage(next);
            this.clearToast();
            this.dashboard._focusInstance(id);
        });
        const close = node('button', 'btn btn-sm btn-link');
        close.type = 'button';
        close.setAttribute('aria-label', this.dashboard._t('close', 'Close'));
        close.append(icon('ti-x'));
        close.addEventListener('click', () => this.clearToast());
        toast.append(status, undo, close);
        document.body.append(toast);
        this.toast = toast;
        let remaining = 8000, started;
        const pause = () => {
            if (this.toastTimer) remaining = Math.max(0, remaining - (Date.now() - started));
            window.clearTimeout(this.toastTimer);
            this.toastTimer = null;
        };
        const resume = () => {
            if (toast.matches(':hover') || toast.contains(document.activeElement)) return;
            started = Date.now();
            this.toastTimer = window.setTimeout(() => this.clearToast(), remaining);
        };
        toast.addEventListener('mouseenter', pause);
        toast.addEventListener('mouseleave', resume);
        toast.addEventListener('focusin', pause);
        toast.addEventListener('focusout', event => { if (!toast.contains(event.relatedTarget)) resume(); });
        toast.addEventListener('keydown', event => { if (event.key === 'Escape') this.clearToast(); });
        resume();
    }

    clearToast() {
        window.clearTimeout(this.toastTimer);
        this.toastTimer = null;
        if (this.toast?.contains(document.activeElement)) this.dashboard.cancelButton.focus({ preventScroll: true });
        this.toast?.remove();
        this.toast = null;
    }

    /** Prepares a visible-content helper and a dashed placeholder at the provisional insertion position. */
    beginMove(id, keyboard = true) {
        if (this.dashboard.saving) return false;
        this.finishMove(false);
        const view = this.dashboard.views.get(id);
        if (!view) return false;
        const grip = view.card.querySelector('.md-dashboard__drag');
        window.bootstrap?.Tooltip?.getInstance(grip)?.hide();
        const helper = this.dashboard._shortcutDragHelper(view);
        // Cloning the card preserves chart elements, but canvas pixels need to be copied separately.
        const canvases = helper.querySelectorAll('canvas');
        view.card.querySelectorAll('canvas').forEach((canvas, index) => {
            if (canvas.width && canvas.height) canvases[index].getContext('2d')?.drawImage(canvas, 0, 0);
        });
        helper.classList.remove('md-dashboard__shortcut-drag-helper');
        helper.classList.add('md-dashboard__widget-drag-helper');
        helper.style.position = 'fixed';
        document.body.append(helper);
        this.move = { id, keyboard, helper, original: cloneSettings(this.dashboard.settings) };
        view.card.classList.add('is-widget-placeholder');
        view.card.dataset.dropLabel = this.dashboard._t('dropWidget', 'Drop widget here');
        grip.setAttribute('aria-pressed', 'true');
        grip.focus({ preventScroll: true });
        this.positionHelper();
        this.announce('widgetMovePosition', '{1}, position {2} of {3} in row {4}. Arrows move, Enter drops, Escape cancels.');
        return true;
    }

    positionHelper() {
        if (!this.move?.keyboard) return;
        const bounds = this.dashboard.views.get(this.move.id).card.getBoundingClientRect();
        Object.assign(this.move.helper.style, { left: `${bounds.left}px`, top: `${bounds.top - 8}px` });
    }

    announce(key, fallback, id = this.move?.id) {
        const card = this.dashboard.views.get(id)?.card;
        if (!card) return;
        const top = card.getBoundingClientRect().top;
        const cards = this.cards();
        const row = cards.filter(value => Math.abs(value.getBoundingClientRect().top - top) < 4);
        const rows = [...new Set(cards.map(value => Math.round(value.getBoundingClientRect().top / 4)))].sort((a, b) => a - b);
        const values = [this.dashboard._title(this.dashboard._instance(id)), row.indexOf(card) + 1, row.length, rows.indexOf(Math.round(top / 4)) + 1];
        this.dashboard.widgetMoveStatus.textContent = values.reduce((text, value, index) => text.replace(`{${index + 1}}`, value), this.dashboard._t(key, fallback, ...values));
    }

    previewMove(beforeId) {
        const next = moveInstanceBefore(this.dashboard.settings.items, this.move.id, beforeId);
        if (next.map(item => item.id).join() === this.dashboard.settings.items.map(item => item.id).join()) return;
        this.dashboard.settings.items = next;
        this.render();
        this.positionHelper();
        this.announce('widgetMovePosition', '{1}, position {2} of {3} in row {4}. Arrows move, Enter drops, Escape cancels.');
    }

    moveKey(event, id) {
        if (![' ', 'Enter', 'Escape', 'ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown', 'Tab'].includes(event.key)) return;
        if (event.key === 'Tab') { this.finishMove(false); return; }
        if (!this.move && ![' ', 'Enter'].includes(event.key)) return;
        event.preventDefault();
        if (!this.move) { this.beginMove(id); return; }
        if (event.key === 'Escape') { this.finishMove(false); return; }
        if (event.key === 'Enter' || event.key === ' ') { this.finishMove(true); return; }
        const cards = this.cards();
        const current = cards.findIndex(card => card.dataset.instanceId === id);
        let target = current + (['ArrowLeft', 'ArrowUp'].includes(event.key) ? -1 : 1);
        if (['ArrowUp', 'ArrowDown'].includes(event.key)) {
            const bounds = cards[current].getBoundingClientRect();
            const candidates = cards.filter(card => event.key === 'ArrowUp' ? card.getBoundingClientRect().top < bounds.top - 4 : card.getBoundingClientRect().top > bounds.top + 4);
            candidates.sort((first, second) => {
                const a = first.getBoundingClientRect(), b = second.getBoundingClientRect();
                return Math.abs(a.top - bounds.top) - Math.abs(b.top - bounds.top) || Math.abs(a.left - bounds.left) - Math.abs(b.left - bounds.left);
            });
            if (candidates.length) target = cards.indexOf(candidates[0]);
        }
        if (target < 0 || target >= cards.length) return;
        this.previewMove(cards[target + (target > current ? 1 : 0)]?.dataset.instanceId || null);
    }

    finishMove(save) {
        const move = this.move;
        if (!move) return;
        this.move = null;
        this.stopPointer?.();
        move.helper.remove();
        const card = this.dashboard.views.get(move.id)?.card;
        card?.classList.remove('is-widget-placeholder');
        card?.querySelector('.md-dashboard__drag').setAttribute('aria-pressed', 'false');
        const next = cloneSettings(this.dashboard.settings);
        this.dashboard.settings = move.original;
        if (save) this.stage(next);
        else this.render();
        this.announce(save ? 'widgetMoved' : 'widgetMoveCancelled', save ? '{1}, position {2} of {3} in row {4}.' : 'Move cancelled. {1}, position {2} of {3} in row {4}.', move.id);
        this.dashboard._focusInstance(move.id);
    }

    /** Uses target midpoints so surrounding cards reflow only after the pointer crosses them. */
    pointerMove(event) {
        const move = this.move;
        Object.assign(move.helper.style, { left: `${event.clientX - move.offsetX}px`, top: `${event.clientY - move.offsetY}px` });
        const cards = this.cards();
        const source = cards.findIndex(card => card.dataset.instanceId === move.id);
        const bounds = cards[source].getBoundingClientRect();
        const target = cards.findIndex((card, index) => {
            if (index === source) return false;
            const rect = card.getBoundingClientRect();
            if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) return false;
            const sameRow = Math.abs(rect.top - bounds.top) < 4;
            const crossed = sameRow ? event.clientX >= rect.left + rect.width / 2 : event.clientY >= rect.top + rect.height / 2;
            return index > source ? crossed : !crossed;
        });
        if (target >= 0) this.previewMove(cards[target + (target > source ? 1 : 0)]?.dataset.instanceId || null);
        this.pointerY = event.clientY;
        if (!this.scrollFrame) this.autoScroll();
    }

    autoScroll() {
        if (!this.move || this.move.keyboard) return;
        const delta = this.pointerY < 110 ? -12 : this.pointerY > window.innerHeight - 60 ? 12 : 0;
        if (delta) {
            if (this.scrollbar) this.scrollbar.setPosition(0, this.scrollbar.offset.y + delta);
            else window.scrollBy(0, delta);
        }
        this.scrollFrame = window.requestAnimationFrame(() => { this.scrollFrame = null; this.autoScroll(); });
    }

    /** Binds one pointer gesture and cleans it up on drop, cancellation or edit-session disposal. */
    pointerDown(event, view) {
        if (event.button !== 0 || this.dashboard.saving) return;
        event.preventDefault();
        this.finishMove(false);
        const origin = { x: event.clientX, y: event.clientY, bounds: view.card.getBoundingClientRect() };
        const listeners = new window.AbortController();
        let started = false;
        this.stopPointer = () => {
            listeners.abort();
            window.cancelAnimationFrame(this.scrollFrame);
            this.scrollFrame = null;
            this.stopPointer = null;
            window.setTimeout(() => { this.suppressClick = false; }, 0);
        };
        document.addEventListener('pointermove', pointer => {
            const dx = pointer.clientX - origin.x, dy = pointer.clientY - origin.y;
            if (!started && Math.hypot(dx, dy) < 8) return;
            if (!started) {
                started = true;
                this.suppressClick = true;
                this.beginMove(view.instance.id, false);
                this.move.offsetX = origin.x - origin.bounds.left;
                this.move.offsetY = origin.y - origin.bounds.top;
            }
            this.pointerMove(pointer);
        }, { signal: listeners.signal });
        document.addEventListener('pointerup', () => {
            this.stopPointer?.();
            this.finishMove(true);
        }, { signal: listeners.signal, once: true });
        document.addEventListener('pointercancel', () => {
            this.stopPointer?.();
            this.finishMove(false);
        }, { signal: listeners.signal, once: true });
    }

    destroy() {
        this.finishMove(false);
        this.stopPointer?.();
        this.clearToast();
        window.removeEventListener('beforeunload', this.onBeforeUnload);
        window.removeEventListener('resize', this.onScroll);
        document.removeEventListener('keydown', this.onKeydown);
        this.scrollbar?.removeListener(this.onScroll);
        this.dashboard.toolbar.style.removeProperty('transform');
        this.dashboard.toolbar.classList.remove('is-stuck');
    }
}
