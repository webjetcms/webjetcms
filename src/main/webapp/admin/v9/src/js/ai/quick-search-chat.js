import { AiChat } from './ai-chat';
import { node, fetchJson, containNativeScroll } from '../dashboard/widget-utils';

const text = key => WJ.translate(`admin.quickSearch.${key}.js`);

/** Hosts independent, copy-only conversations with the configured chat assistants. */
export class QuickSearchChat {
    constructor() {
        this.conversations = new Map();
        this.assistants = null;
        this.mounted = false;
        this.disposed = false;
        this.scrollController = new AbortController();
        this.element = node('div', 'md-quick-search__chat-host');
        this.assistantRow = node('div', 'md-quick-search__chat-assistant');
        const label = node('label', 'visually-hidden', text('chatAssistant'));
        this.select = node('select', 'form-select');
        this.select.id = 'admin-quick-search-chat-assistant';
        label.htmlFor = this.select.id;
        this.select.addEventListener('change', () => this.selectAssistant(this.select.value));
        this.assistantRow.append(label, this.select);
        this.status = node('div', 'md-quick-search__chat-status');
        this.status.setAttribute('role', 'status');
        this.status.tabIndex = -1;
        this.content = node('div', 'md-quick-search__chat-content');
        this.element.append(this.status, this.content);
    }

    /** Loads assistants on first use and restores the selected conversation on later visits. */
    mount(container) {
        if (this.disposed || (this.mounted && this.element.parentElement === container)) return;
        this.unmount();
        container.appendChild(this.element);
        this.mounted = true;
        if (this.assistants == null && !this.loading && !this.error) this.load();
        else this.render();
        this.focus();
    }

    /** Keeps conversation state and pending responses while the search tab is hidden. */
    unmount() {
        this.chat?.unmount();
        this.element.remove();
        this.mounted = false;
    }

    focus() {
        if (!this.mounted) return;
        const input = this.content.querySelector('.ai-chat-input');
        (input || this.status.querySelector('button') || this.status).focus({ preventScroll: true });
    }

    async load() {
        if (this.disposed || this.loading) return;
        this.loading = true;
        this.error = false;
        this.controller = new AbortController();
        this.render();
        try {
            const assistants = await fetchJson('/admin/rest/ai/assistant/chat-assistants/', this.controller.signal);
            if (this.disposed) return;
            if (!Array.isArray(assistants)) throw new Error('Invalid chat assistant response');
            this.assistants = assistants.filter(assistant => assistant?.action === 'live_chat' && assistant.assistantId != null);
            this.select.replaceChildren(...this.assistants.map(assistant => {
                const option = node('option', '', assistant.description || assistant.providerTitle);
                option.value = assistant.assistantId;
                return option;
            }));
        } catch (error) {
            if (this.disposed) return;
            this.error = true;
        } finally {
            this.loading = false;
            if (!this.disposed && this.mounted) {
                const previousFocus = document.activeElement;
                const keepFocus = !this.element.contains(previousFocus);
                this.render();
                if (keepFocus) previousFocus?.focus({ preventScroll: true });
                else this.focus();
            }
        }
    }

    render() {
        if (!this.mounted || this.disposed) return;
        const hasAssistants = !this.loading && !this.error && this.assistants?.length > 0;
        this.content.hidden = !hasAssistants;
        this.status.hidden = hasAssistants;
        this.status.replaceChildren();
        this.status.setAttribute('aria-busy', String(this.loading));
        if (hasAssistants) {
            this.selectAssistant(this.select.value);
        } else {
            let statusKey = 'chatEmpty';
            if (this.loading) statusKey = 'chatLoading';
            else if (this.error) statusKey = 'chatUnavailable';
            this.status.append(node('p', '', text(statusKey)));
            if (this.error) {
                const retry = node('button', 'btn btn-outline-secondary btn-sm', text('retry'));
                retry.type = 'button';
                retry.addEventListener('click', () => { this.load(); this.focus(); });
                this.status.append(retry);
            }
        }
    }

    /** Retains each assistant's transcript and draft without supplying any page or field context. */
    selectAssistant(id) {
        const assistant = this.assistants?.find(item => String(item.assistantId) === id);
        if (assistant == null || !this.mounted || this.disposed) return;
        this.chat?.unmount();
        this.chat = this.conversations.get(id);
        if (this.chat == null) {
            this.chat = new AiChat({ assistant, composerControls: this.assistantRow, showAttachment: true });
            containNativeScroll(this.chat.element.querySelector('.ai-chat-history'), this.scrollController.signal);
            this.conversations.set(id, this.chat);
        }
        this.chat.mount(this.content);
    }

    /** Cancels pending requests and releases every conversation when the host is destroyed. */
    destroy() {
        if (this.disposed) return;
        this.disposed = true;
        this.controller?.abort();
        this.scrollController.abort();
        for (const chat of this.conversations.values()) chat.destroy();
        this.conversations.clear();
        this.unmount();
    }
}
