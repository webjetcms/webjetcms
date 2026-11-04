/**
 * Separates generated HTML from its explanation, including incomplete streamed code blocks.
 * Extracts body content and preserves the original response for callers that need it.
 * @param {string|null|undefined} wholeText Complete response or text received so far.
 * @returns {{wholeText: string|null|undefined, html: ?string, explanatoryText: ?string}} Parsed response parts.
 */
export function parseAiResponse(wholeText) {
    const parsed = { wholeText, html: null, explanatoryText: null };
    if (wholeText == null || wholeText.trim().length == 0) return parsed;

    const startBlock = wholeText.indexOf("```html");
    if (startBlock != -1) {
        const endBlock = wholeText.indexOf("```", startBlock + 7);
        if (endBlock == -1) {
            // The closing fence may arrive in a later streamed fragment.
            parsed.html = wholeText.substring(startBlock + 7).trim();
        } else {
            parsed.html = wholeText.substring(startBlock + 7, endBlock).trim();
            if (wholeText.length > endBlock + 10) parsed.explanatoryText = wholeText.substring(endBlock + 3).trim();
        }
    } else {
        parsed.html = wholeText;
    }

    if (parsed.explanatoryText != null) {
        parsed.explanatoryText = parsed.explanatoryText.replace(/```markdown/g, "").replace(/```$/g, "").trim();
        parsed.explanatoryText = parsed.explanatoryText.replace(/\\"/g, '"');
        parsed.explanatoryText = parsed.explanatoryText.replace(/&amp;lt;!--/g, "&lt!--").replace(/--&amp;gt;/g, "--&gt;");
    }

    if (parsed.html != null) {
        let bodyStart = parsed.html.indexOf("<body");
        const bodyEnd = parsed.html.indexOf("</body>");
        if (bodyStart != -1 && bodyEnd != -1) {
            bodyStart = parsed.html.indexOf(">", bodyStart);
            parsed.html = parsed.html.substring(bodyStart + 1, bodyEnd).trim();
        }
    }
    return parsed;
}

/**
 * Splits a chat reply into Markdown and HTML source for collapsible display, without changing the saved reply.
 * Keeps explanations outside HTML fences and accepts unfinished fences during streaming.
 * @param {string} text Complete reply or text received so far.
 * @returns {Array<{html: boolean, text: string}>} Ordered display parts.
 */
export function splitChatResponse(text) {
    const startsWithHtml = value => /^\s*(?:<!doctype\s+html\b|<!--|<\/?[a-z][a-z0-9-]*(?:\s|\/?>|$))/i.test(value);
    if (startsWithHtml(text)) return [{ html: true, text }];

    const parts = [];
    const fences = /^[ \t]*(`{3,}|~{3,})[ \t]*([^\r\n]*)\r?$/gm;
    let cursor = 0;
    let opening;
    while ((opening = fences.exec(text)) != null) {
        const marker = opening[1];
        const language = opening[2].trim().toLowerCase();
        const contentStart = text[fences.lastIndex] === '\n' ? fences.lastIndex + 1 : fences.lastIndex;
        const closing = new RegExp('^[ \\t]*' + marker[0] + '{' + marker.length + ',}[ \\t]*\\r?$', 'gm');
        closing.lastIndex = contentStart;
        const ending = closing.exec(text);
        const contentEnd = ending ? ending.index : text.length;
        const end = ending ? closing.lastIndex : text.length;
        const code = text.slice(contentStart, contentEnd);
        if (language === 'html' || language === 'htm' || (!language && startsWithHtml(code))) {
            if (text.slice(cursor, opening.index).trim()) parts.push({ html: false, text: text.slice(cursor, opening.index) });
            parts.push({ html: true, text: code.trim() });
            cursor = end;
        }
        // Skip the entire block so a fence inside another language's example stays literal.
        fences.lastIndex = end;
        if (!ending) break;
    }
    if (text.slice(cursor).trim()) parts.push({ html: false, text: text.slice(cursor) });
    return parts;
}
