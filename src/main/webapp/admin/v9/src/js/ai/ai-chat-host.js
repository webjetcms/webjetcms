import { AiChat } from './ai-chat';
import { node, icon, containNativeScroll } from '../dashboard/widget-utils';
import { chatHistoryRequest, chatRequestId, historyText } from './chat-history-api';
import { setChatTooltip, initChatTooltips, disposeChatTooltips } from './chat-tooltips';

const text = key => WJ.translate(`admin.quickSearch.${key}.js`);
const filesText = key => WJ.translate(`components.ai_assistants.chat.files.${key}.js`);
const HISTORY_OPEN_SETTING = 'aiChatHistoryOpen';

/**
 * Hosts conversations, assistant selection, history and files for global or entity chat.
 * Callers supply assistants directly or through loadAssistants(signal), plus optional field context and actions.
 */
export class AiChatHost {
    constructor(options = {}) {
        this.options = options;
        const idPrefix = options.context ? `ai-field-chat-${chatRequestId()}` : 'admin-ai-chat';
        this.conversations = new Set();
        this.lastConversationByAssistant = new Map();
        const savedHistoryOpen = WJ.getAdminSetting(HISTORY_OPEN_SETTING);
        this.historyOpen = savedHistoryOpen == null ? options.historyOpen !== false : savedHistoryOpen !== 'false';
        this.filesOpen = false;
        this.historyItems = [];
        this.historyPage = 0;
        this.historyQuery = '';
        this.deleting = new Set();
        this.selectionVersion = 0;
        this.assistants = null;
        this.mounted = false;
        this.disposed = false;
        this.scrollController = new AbortController();
        // The host owns one assistant selector, moved into the composer of whichever conversation is mounted.
        this.element = node('div', 'ai-chat-host');
        this.element.classList.toggle('ai-chat-host--history-open', this.historyOpen);
        this.assistantRow = node('div', 'ai-chat-host__assistant');
        const label = node('label', 'visually-hidden', text('chatAssistant'));
        this.select = node('select', 'form-select');
        this.select.id = options.context ? `${idPrefix}-assistant` : 'admin-quick-search-chat-assistant';
        label.htmlFor = this.select.id;
        this.select.addEventListener('change', () => this.selectAssistant(this.select.value));
        this.assistantRow.append(label, this.select);
        this.status = node('div', 'ai-chat-host__status');
        this.status.setAttribute('role', 'status');
        this.status.tabIndex = -1;
        this.content = node('div', 'ai-chat-host__content');
        this.main = node('div', 'ai-chat-host__main');
        this.main.append(this.status, this.content);
        // History belongs to the host context, so switching assistants keeps the same search and pagination.
        this.historyPanel = node('aside', 'ai-chat-host__history');
        this.historyPanel.id = `${idPrefix}-history`;
        this.historyPanel.hidden = !this.historyOpen;
        this.historyPanel.setAttribute('aria-label', historyText('title'));
        const heading = node('div', 'ai-chat-host__history-heading');
        this.closeHistory = this.iconButton(historyText('close'), 'layout-sidebar', () => this.toggleHistory(false));
        heading.append(node('strong', '', historyText('title')), this.closeHistory);
        this.newChatButton = this.button(WJ.translate('components.ai_assistants.chat.new.js'), () => {
            this.chat?.newChat();
            if (this.element.clientWidth < 768) this.toggleHistory(false, false);
            this.focus();
        }, 'btn btn-primary ai-chat-host__new');
        this.newChatButton.prepend(icon('ti-plus'));
        this.newChatButton.disabled = true;
        const search = node('div', 'ai-chat-host__history-search');
        const searchLabel = node('label', 'visually-hidden', historyText('search'));
        this.historySearch = node('input', 'form-control form-control-sm');
        this.historySearch.id = `${idPrefix}-history-search`;
        this.historySearch.type = 'search';
        this.historySearch.placeholder = historyText('search');
        this.historySearch.autocomplete = 'off';
        searchLabel.htmlFor = this.historySearch.id;
        this.historySearch.addEventListener('input', () => this.searchHistory());
        search.append(searchLabel, icon('ti-search'), this.historySearch);
        this.historyList = node('div', 'ai-chat-host__history-list');
        this.historyList.setAttribute('aria-live', 'polite');
        this.historyPanel.append(heading, this.newChatButton, search, this.historyList);
        this.historyPanel.addEventListener('keydown', event => {
            if (event.key === 'Escape') {
                event.preventDefault();
                event.stopPropagation();
                this.toggleHistory(false);
            }
        });
        containNativeScroll(this.historyList, this.scrollController.signal);
        // The files panel uses the selected transcript; it does not fetch a separate attachment catalogue.
        this.filesPanel = node('aside', 'ai-chat-host__files');
        this.filesPanel.id = `${idPrefix}-files`;
        this.filesPanel.hidden = true;
        this.filesPanel.setAttribute('aria-label', filesText('title'));
        const filesHeading = node('div', 'ai-chat-host__history-heading');
        filesHeading.append(node('strong', '', filesText('title')),
            this.iconButton(historyText('close'), 'layout-sidebar-right', () => this.toggleFiles(false)));
        const filesSearch = node('div', 'ai-chat-host__history-search');
        const filesSearchLabel = node('label', 'visually-hidden', filesText('search'));
        this.filesSearch = node('input', 'form-control form-control-sm');
        this.filesSearch.id = `${idPrefix}-files-search`;
        this.filesSearch.type = 'search';
        this.filesSearch.placeholder = filesText('search');
        this.filesSearch.autocomplete = 'off';
        filesSearchLabel.htmlFor = this.filesSearch.id;
        this.filesSearch.addEventListener('input', () => this.renderFiles());
        filesSearch.append(filesSearchLabel, icon('ti-search'), this.filesSearch);
        this.filesList = node('div', 'ai-chat-host__history-list');
        this.filesList.setAttribute('aria-live', 'polite');
        this.filesPanel.append(filesHeading, filesSearch, this.filesList);
        this.filesPanel.addEventListener('keydown', event => {
            if (event.key === 'Escape') {
                event.preventDefault();
                event.stopPropagation();
                this.toggleFiles(false);
            }
        });
        containNativeScroll(this.filesList, this.scrollController.signal);
        this.element.append(this.historyPanel, this.main, this.filesPanel);
        if (options.assistants || !options.loadAssistants) this.setAssistants(options.assistants || [], options.assistantId);
    }

    setAssistants(assistants, selectedId) {
        this.assistants = assistants;
        this.select.replaceChildren(...assistants.map(assistant => {
            const option = node('option', '', assistant.description || assistant.providerTitle);
            option.value = assistant.assistantId;
            return option;
        }));
        if (selectedId != null) this.select.value = String(selectedId);
    }

    /** Includes background conversations when the user switches assistants. */
    isPending() {
        return [...this.conversations].some(chat => chat.isPending());
    }

    /** Loads assistants on first use and restores the selected conversation on later visits. */
    mount(container) {
        if (this.disposed || (this.mounted && this.element.parentElement === container)) return;
        this.unmount();
        container.appendChild(this.element);
        this.mounted = true;
        if (this.assistants == null && !this.loading && !this.error) this.load();
        else this.render();
        if (this.historyOpen) this.loadHistory();
        if (this.chat?.state.threadId && !this.chat.state.pending) this.refreshConversation(this.chat);
        initChatTooltips(this.element);
        this.focus();
    }

    /** Keeps conversation state and pending responses while the host is hidden. */
    unmount() {
        this.chat?.unmount();
        disposeChatTooltips(this.element);
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
            const assistants = await this.options.loadAssistants(this.controller.signal);
            if (this.disposed) return;
            if (!Array.isArray(assistants)) throw new Error('Invalid chat assistant response');
            this.setAssistants(assistants.filter(assistant => assistant?.action === 'live_chat' && assistant.assistantId != null));
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
        this.renderFiles();
        const hasAssistants = !this.loading && !this.error && this.assistants?.length > 0;
        if (this.chat?.state.threadId) { this.showChat(this.chat); return; }
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
            const history = this.button(historyText('title'), () => this.toggleHistory());
            history.setAttribute('aria-controls', this.historyPanel.id);
            history.setAttribute('aria-expanded', String(this.historyOpen));
            this.status.append(history);
            if (this.error) this.status.append(this.button(text('retry'), () => { this.load(); this.focus(); }));
        }
    }

    /** Retains each assistant's transcript and draft in this host's context. */
    selectAssistant(id, fresh = false) {
        const assistant = this.assistants?.find(item => String(item.assistantId) === id);
        if (assistant == null || this.disposed) return;
        this.selectionVersion++;
        let chat = fresh ? null : this.lastConversationByAssistant.get(id);
        if (!chat) chat = this.createChat(assistant);
        this.showChat(chat);
        if (this.mounted) this.refreshConversation(chat);
    }

    createChat(assistant) {
        const persistent = assistant.persistentChat === true;
        const options = this.options.getChatOptions?.(assistant) || {};
        const chat = new AiChat({
            assistant, persistent, composerControls: this.assistantRow, showAttachment: true,
            dropTarget: this.element,
            context: this.options.context,
            ...options,
            notice: persistent ? null : historyText('unsaved'),
            getTitle: () => chat.threadInfo?.title || chat.state.messages.find(message => message.role === 'user')?.text
                || (this.historyOpen ? WJ.translate('components.ai_assistants.chat.new.js') : ''),
            getToolbarActions: () => [...this.toolbarActions(chat), ...(options.getToolbarActions?.(chat) || [])],
            onChange: current => {
                if (this.chat === current) this.renderFiles();
                options.onChange?.(current);
            },
            onNewChat: persistent ? current => {
                current.stop();
                const selected = this.assistants?.find(item => item.assistantId === current.assistant.assistantId) || this.assistants?.[0];
                if (selected) this.selectAssistant(String(selected.assistantId), true);
                else { current.state.notice = text('chatEmpty'); current.refresh(); }
            } : undefined,
            onMetadata: () => { if (this.historyOpen) this.loadHistory(); },
            onSettled: current => {
                if (!this.disposed && persistent) this.refreshConversation(current);
                if (!this.disposed && this.historyOpen) this.loadHistory();
            }
        });
        containNativeScroll(chat.element.querySelector('.ai-chat-history'), this.scrollController.signal);
        this.conversations.add(chat);
        return chat;
    }

    showChat(chat) {
        if (this.chat !== chat) this.filesSearch.value = '';
        this.chat?.unmount();
        this.chat = chat;
        this.options.onShowChat?.(chat);
        this.newChatButton.disabled = false;
        const id = String(chat.assistant.assistantId);
        this.lastConversationByAssistant.set(id, chat);
        if (![...this.select.options].some(option => option.value === id)) {
            const option = node('option', '', chat.assistant.description || chat.assistant.providerTitle);
            option.value = id;
            this.select.append(option);
        }
        this.select.value = id;
        this.status.hidden = true;
        this.content.hidden = false;
        if (this.mounted) chat.mount(this.content);
        this.renderHistory();
        this.renderFiles();
    }

    toolbarActions(chat) {
        const actions = [{ id: 'history', label: historyText('title'), icon: 'layout-sidebar', iconOnly: true, position: 'start',
            controls: this.historyPanel.id, expanded: this.historyOpen, run: () => this.toggleHistory() },
            { id: 'files', label: filesText('title'), icon: 'files', iconOnly: true, position: 'end',
                controls: this.filesPanel.id, expanded: this.filesOpen, run: () => this.toggleFiles() }];
        if (chat.state.serverBusy && !chat.state.pending) {
            actions.push({ id: 'server-stop', label: historyText('stop'), icon: 'player-stop', run: async () => {
                try {
                    await chatHistoryRequest(`/threads/${encodeURIComponent(chat.state.threadId)}/stop`, { method: 'POST' });
                    this.refreshConversation(chat);
                } catch (error) { chat.state.notice = error.message; chat.refresh(); }
            } });
        }
        if (chat.threadInfo?.status === 'DELETE_PENDING') {
            actions.push({ id: 'delete-retry', label: historyText('retryDelete'), icon: 'trash',
                disabled: this.deleting.has(chat.state.threadId), run: () => this.deleteThread(chat.threadInfo) });
        }
        return actions;
    }

    /** Updates the history panel, persisting explicit choices but not automatic mobile closures. */
    toggleHistory(open = !this.historyOpen, persist = true) {
        if (open && this.filesOpen && this.element.clientWidth < 768) this.toggleFiles(false);
        this.historyOpen = open;
        if (persist) WJ.setAdminSetting(HISTORY_OPEN_SETTING, String(open));
        this.historyPanel.hidden = !open;
        if (open) initChatTooltips(this.historyPanel);
        else disposeChatTooltips(this.historyPanel);
        this.element.classList.toggle('ai-chat-host--history-open', open);
        this.chat?.refresh();
        this.status.querySelector('[aria-controls]')?.setAttribute('aria-expanded', String(open));
        if (open) {
            this.loadHistory();
            this.historySearch.focus();
        } else {
            this.main.querySelector(`[aria-controls="${this.historyPanel.id}"]`)?.focus();
        }
    }

    toggleFiles(open = !this.filesOpen) {
        if (open && this.historyOpen && this.element.clientWidth < 768) this.toggleHistory(false, false);
        this.filesOpen = open;
        this.filesPanel.hidden = !open;
        if (open) initChatTooltips(this.filesPanel);
        else disposeChatTooltips(this.filesPanel);
        this.chat?.refresh();
        if (open) {
            this.renderFiles();
            this.filesSearch.focus();
        } else {
            this.main.querySelector(`[aria-controls="${this.filesPanel.id}"]`)?.focus();
        }
    }

    /** Lists unique attachments from every turn of the selected conversation, including assistant output files. */
    renderFiles() {
        if (!this.filesOpen || this.disposed) return;
        const chat = this.chat;
        const files = new Map();
        for (const message of chat?.state.messages || []) {
            for (const attachment of message.attachments || []) {
                const key = attachment.file || attachment.url || attachment.id || attachment;
                if (!files.has(key)) files.set(key, { attachment, role: message.role });
            }
        }
        const query = this.filesSearch.value.trim().toLocaleLowerCase();
        const entries = [...files.values()];
        const key = JSON.stringify([query, entries.map(({ attachment, role }) =>
            [attachment.id, attachment.url, attachment.name, attachment.size, attachment.mediaType, role])]);
        if (this.renderedFilesChat === chat && this.renderedFilesKey === key) return;
        this.renderedFilesChat = chat;
        this.renderedFilesKey = key;
        disposeChatTooltips(this.filesList);
        this.filesList.replaceChildren();
        const list = node('ul', 'list-unstyled');
        for (const { attachment, role } of entries) {
            if (query && !attachment.name.toLocaleLowerCase().includes(query)) continue;
            const row = node('li', 'ai-chat-host__file');
            const download = this.button('', () => chat.downloadAttachment(attachment, download), 'ai-chat-host__file-download');
            setChatTooltip(download, attachment.name);
            download.setAttribute('aria-label', WJ.translate('components.ai_assistants.chat.downloadAttachment.js', [attachment.name]));
            const info = node('span', 'ai-chat-host__file-info');
            info.append(node('span', 'ai-chat-host__history-title', attachment.name),
                node('small', '', filesText(role === 'assistant' ? 'generated' : 'uploaded')));
            download.append(icon('ti-file-text'), info, icon('ti-download'));
            row.append(download);
            list.append(row);
        }
        this.filesList.append(list);
        if (!list.children.length) this.filesList.append(node('p', '', filesText(query ? 'noResults' : 'empty')));
        initChatTooltips(this.filesList);
    }

    /** Restarts pagination and cancels stale results as soon as the history query changes. */
    searchHistory() {
        const query = this.historySearch.value.trim();
        if (query === this.historyQuery) return;
        this.historyQuery = query;
        this.historyController?.abort();
        this.historyController = null;
        clearTimeout(this.historySearchTimer);
        this.historyItems = [];
        this.historyPage = 0;
        this.historyHasMore = false;
        this.historyError = null;
        this.historyLoading = true;
        this.renderHistory();
        this.historySearchTimer = setTimeout(() => this.loadHistory(), 300);
    }

    async loadHistory(append = false) {
        if (this.disposed) return;
        clearTimeout(this.historySearchTimer);
        this.historyController?.abort();
        const controller = new AbortController();
        this.historyController = controller;
        this.historyLoading = true;
        this.historyError = null;
        this.renderHistory();
        try {
            const page = append ? this.historyPage + 1 : 0;
            const parameters = new URLSearchParams({ page, ...this.options.context });
            if (this.historyQuery) parameters.set('query', this.historyQuery);
            const result = await chatHistoryRequest(`/threads?${parameters}`, { signal: controller.signal });
            if (controller.signal.aborted || this.disposed) return;
            this.historyItems = append ? [...new Map([...this.historyItems, ...result.items].map(item => [item.id, item])).values()] : result.items;
            this.historyPage = page;
            this.historyHasMore = result.hasMore;
        } catch (error) {
            if (!controller.signal.aborted) this.historyError = error.message;
        } finally {
            if (this.historyController === controller) {
                this.historyLoading = false;
                this.renderHistory();
            }
        }
    }

    renderHistory() {
        if (!this.historyList || this.disposed) return;
        disposeChatTooltips(this.historyList);
        this.historyList.replaceChildren();
        this.historyList.setAttribute('aria-busy', String(Boolean(this.historyLoading)));
        if (this.historyError) {
            this.historyList.append(node('p', '', this.historyError), this.button(historyText('retry'), () => this.loadHistory()));
        }
        const list = node('ul', 'list-unstyled');
        for (const item of this.historyItems) {
            const row = node('li', 'ai-chat-host__history-item');
            const select = this.button('', () => this.openThread(item.id), 'ai-chat-host__history-select');
            setChatTooltip(select, item.title);
            select.append(node('span', 'ai-chat-host__history-title', item.title), node('small', '', item.assistantName || item.provider));
            if (item.id === this.chat?.state.threadId) select.setAttribute('aria-current', 'true');
            const pending = item.status === 'DELETE_PENDING';
            if (pending) select.append(node('small', 'text-danger', historyText('deletePending')));
            if (item.deletionError) select.append(node('small', 'text-danger', historyText(item.deletionError)));
            const remove = this.iconButton(historyText(pending ? 'retryDelete' : 'delete'), pending ? 'refresh' : 'trash', () => this.deleteThread(item));
            remove.disabled = this.deleting.has(item.id);
            remove.setAttribute('aria-label', `${historyText(pending ? 'retryDelete' : 'delete')}: ${item.title}`);
            row.append(select, remove);
            list.append(row);
        }
        this.historyList.append(list);
        if (!this.historyLoading && !this.historyError && this.historyItems.length === 0) {
            this.historyList.append(node('p', '', historyText(this.historyQuery ? 'noResults' : 'empty')));
        }
        if (this.historyHasMore) {
            const more = this.button(historyText('more'), () => this.loadHistory(true));
            more.disabled = this.historyLoading;
            this.historyList.append(more);
        }
        initChatTooltips(this.historyList);
    }

    async openThread(id) {
        const selection = ++this.selectionVersion;
        try {
            let chat = this.findThread(id);
            if (chat) {
                this.refreshConversation(chat);
            } else {
                const detail = await chatHistoryRequest(`/threads/${encodeURIComponent(id)}`, { signal: this.scrollController.signal });
                if (this.disposed || selection !== this.selectionVersion) return;
                if (!this.matchesContext(detail.thread.context)) throw new Error(historyText('requestChanged'));
                const assistant = this.assistants?.find(item => item.assistantId === detail.thread.assistantId) || {
                    assistantId: detail.thread.assistantId, provider: detail.thread.provider,
                    description: detail.thread.assistantName, providerTitle: detail.thread.assistantName
                };
                chat = this.createChat({ ...assistant, persistentChat: true });
                this.applyDetail(chat, detail);
            }
            if (selection !== this.selectionVersion || this.disposed) return;
            this.showChat(chat);
            if (this.element.clientWidth < 768) this.toggleHistory(false, false);
            this.focus();
        } catch (error) {
            if (this.disposed) return;
            this.historyError = error.message;
            this.renderHistory();
        }
    }

    applyDetail(chat, detail) {
        if (!this.matchesContext(detail.thread.context)) throw new Error(historyText('requestChanged'));
        if (!chat.restoreHistory(detail)) return;
        if (this.chat === chat) this.renderFiles();
        clearTimeout(chat.historyRefreshTimer);
        // A stopped stream can settle before the server worker finishes; poll while that work or deletion remains active.
        if (detail.thread.busy || ((detail.thread.status === 'DELETE_PENDING' || chat.deletionRequested) && !detail.thread.deletionError)) {
            chat.historyRefreshTimer = setTimeout(() => {
                if (this.mounted && this.chat === chat) this.refreshConversation(chat);
            }, 1500);
        }
    }

    async refreshConversation(chat) {
        if (!chat.state.threadId || chat.state.pending || chat.state.disposed || this.disposed) return;
        // Replace an older read immediately; it may predate a just-completed turn.
        chat.historyController?.abort();
        const controller = new AbortController();
        chat.historyController = controller;
        try {
            const detail = await chatHistoryRequest(`/threads/${encodeURIComponent(chat.state.threadId)}`, { signal: controller.signal });
            if (!controller.signal.aborted) this.applyDetail(chat, detail);
        } catch (error) {
            if (controller.signal.aborted) return;
            if (error.status === 404 && (chat.deletionRequested || chat.threadInfo?.status === 'DELETE_PENDING')) this.removeChat(chat.state.threadId);
            else if (error.status === 404 && !chat.threadInfo && chat.state.revision == null) {
                chat.state.threadId = null;
                chat.refresh();
            }
            else if (error.status !== 404) { chat.state.notice = error.message; chat.refresh(); }
        }
    }

    async deleteThread(item) {
        if (this.deleting.has(item.id)) return;
        if (item.status !== 'DELETE_PENDING' && !window.confirm(historyText('confirmDelete'))) return;
        this.deleting.add(item.id);
        const chat = this.findThread(item.id);
        if (chat) {
            chat.deletionRequested = true;
            chat.state.readOnlyReason = historyText('deletePending');
            chat.stop();
            chat.refresh();
        }
        this.renderHistory();
        let failure;
        try {
            const result = await chatHistoryRequest(`/threads/${encodeURIComponent(item.id)}`, { method: 'DELETE' });
            if (this.disposed) return;
            if (result.deleted) this.removeChat(item.id);
            else if (chat) await this.refreshConversation(chat);
        } catch (error) {
            if (!this.disposed) {
                if (error.status === 404) { this.removeChat(item.id); return; }
                failure = error.message;
                if (chat) { chat.deletionRequested = false; chat.state.notice = error.message; this.refreshConversation(chat); }
            }
        } finally {
            this.deleting.delete(item.id);
            if (!this.disposed) {
                await this.loadHistory();
                if (failure) { this.historyError = failure; this.renderHistory(); }
                chat?.refresh();
            }
        }
    }

    findThread(id) {
        return [...this.conversations].find(chat => chat.state.threadId === id);
    }

    matchesContext(context) {
        const expected = this.options.context;
        if (!expected) return context == null;
        return context?.entityType === expected.entityType && context.entityId === expected.entityId
            && context.fieldName === expected.fieldName;
    }

    removeChat(id) {
        const chat = this.findThread(id);
        this.historyItems = this.historyItems.filter(item => item.id !== id);
        if (chat) {
            clearTimeout(chat.historyRefreshTimer);
            chat.historyController?.abort();
            this.conversations.delete(chat);
            const assistantId = String(chat.assistant.assistantId);
            if (this.lastConversationByAssistant.get(assistantId) === chat) this.lastConversationByAssistant.delete(assistantId);
            chat.destroy();
            if (this.chat === chat) {
                this.chat = null;
                this.content.replaceChildren();
                const assistant = this.assistants?.find(item => String(item.assistantId) === this.select.value) || this.assistants?.[0];
                if (assistant) this.selectAssistant(String(assistant.assistantId), true);
                else this.render();
            }
        }
        this.renderHistory();
    }

    button(label, run, className = 'btn btn-sm btn-outline-secondary') {
        const button = node('button', className, label);
        button.type = 'button';
        button.addEventListener('click', run);
        return button;
    }

    iconButton(label, name, run) {
        const button = this.button('', run, 'btn btn-sm ai-chat-host__history-action');
        setChatTooltip(button, label);
        button.setAttribute('aria-label', label);
        button.append(icon(`ti-${name}`));
        return button;
    }

    /** Cancels pending requests and releases every conversation when the host is destroyed. */
    destroy() {
        if (this.disposed) return;
        this.disposed = true;
        this.controller?.abort();
        clearTimeout(this.historySearchTimer);
        this.historyController?.abort();
        this.scrollController.abort();
        for (const chat of this.conversations) {
            clearTimeout(chat.historyRefreshTimer);
            chat.historyController?.abort();
            chat.destroy();
        }
        this.conversations.clear();
        this.lastConversationByAssistant.clear();
        this.unmount();
    }
}
