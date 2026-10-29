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
