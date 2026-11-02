export const historyText = key => {
    const translationKey = `components.ai_assistants.chat.history.${key}.js`;
    const value = WJ.translate(translationKey);
    return value === translationKey && key !== 'unavailable'
        ? WJ.translate('components.ai_assistants.chat.history.unavailable.js') : value;
};
export const chatHistoryUrl = '/admin/rest/ai/chat';

/** Also works on development HTTP origins where crypto.randomUUID is unavailable. */
export function chatRequestId() {
    if (crypto.randomUUID) return crypto.randomUUID();
    const bytes = crypto.getRandomValues(new Uint8Array(16));
    bytes[6] = (bytes[6] & 15) | 64;
    bytes[8] = (bytes[8] & 63) | 128;
    const hex = [...bytes].map(byte => byte.toString(16).padStart(2, '0')).join('');
    return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

/** Uses the same authenticated, CSRF-protected administration connection as AI generation. */
export async function chatHistoryRequest(path, { method = 'GET', signal } = {}) {
    const response = await fetch(chatHistoryUrl + path, {
        method, signal, headers: { Accept: 'application/json', 'X-CSRF-Token': window.csrfToken }
    });
    const data = await response.json().catch(() => null);
    if (!response.ok) checkAiResponse(response, historyText(typeof data?.error === 'string' ? data.error : 'unavailable'));
    if (data == null) throw new Error(historyText('unavailable'));
    return data;
}

/** Accepts only owner-scoped attachment URLs returned by saved chat history. */
export function chatAttachmentUrl(attachment) {
    return /^\/admin\/rest\/ai\/chat\/threads\/[a-f0-9-]{36}\/attachments\/(?:[1-9][0-9]*\.[a-z]+|[a-f0-9-]{36})$/.test(attachment.url || '')
        ? attachment.url : null;
}

/** REST GET requests also require the CSRF header; navigation and image URLs cannot supply it. */
export async function fetchChatAttachment(attachment, signal) {
    const url = chatAttachmentUrl(attachment);
    const message = historyText('attachmentDownloadFailed');
    if (!url) throw new Error(message);
    const response = await fetch(url, {
        signal, credentials: 'same-origin', cache: 'no-store', redirect: 'error',
        headers: { 'X-CSRF-Token': window.csrfToken }
    });
    checkAiResponse(response, message);
    const contentType = response.headers.get('Content-Type')?.split(';')[0].trim().toLowerCase();
    if (contentType !== attachment.mediaType) throw new Error(message);
    const blob = await response.blob();
    if (!blob.size) throw new Error(message);
    return blob;
}

/** Preserves administration session notifications and the HTTP status for callers handling missing chats. */
export function checkAiResponse(response, message) {
    if (response.ok) return;
    if (response.status === 401) WJ.keepSessionShowLogoffMessage();
    else if (response.status === 403) {
        try { WJ.keepSessionShowTokenMessage(window.csrfError); } catch (error) { window.alert(window.csrfError); }
    }
    throw Object.assign(new Error(message), { status: response.status });
}
