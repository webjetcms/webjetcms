/**
 * Validates raw JSON without replacing its text representation.
 * @param {string} text Raw field value.
 * @param {boolean} required Whether whitespace-only input must be rejected.
 * @returns {{valid: boolean, error?: string, line?: number, column?: number}}
 */
export function validateJsonObject(text, required = false) {
    if (text.trim().length === 0) return required ? {valid: false, error: "required"} : {valid: true};

    try {
        const value = JSON.parse(text);
        if (value === null || typeof value !== "object" || Array.isArray(value)) {
            return {valid: false, error: "object"};
        }
        return {valid: true};
    } catch (error) {
        const result = {valid: false, error: "invalid"};
        const location = /line (\d+) column (\d+)/i.exec(error.message);
        const position = /position (\d+)/i.exec(error.message);
        if (location) {
            result.line = Number(location[1]);
            result.column = Number(location[2]);
        } else if (position) {
            const precedingLines = text.substring(0, Number(position[1])).split("\n");
            result.line = precedingLines.length;
            result.column = precedingLines[precedingLines.length - 1].length + 1;
        }
        return result;
    }
}

/**
 * Indents a valid JSON object using two spaces, preserving every non-whitespace token.
 * Numeric spelling, duplicate keys, string escapes and key order remain unchanged.
 * @param {string} text Raw JSON object.
 * @returns {string} Formatted text, or unchanged input when empty or invalid.
 */
export function formatJsonObject(text) {
    if (text.trim().length === 0 || !validateJsonObject(text).valid) return text;

    let compact = "";
    let inString = false;
    let escaped = false;
    for (const character of text) {
        if (inString) {
            compact += character;
            if (escaped) escaped = false;
            else if (character === "\\") escaped = true;
            else if (character === '"') inString = false;
        } else if (character === '"') {
            compact += character;
            inString = true;
        } else if (!/[\t\n\r ]/.test(character)) {
            compact += character;
        }
    }

    let output = "";
    let depth = 0;
    for (let index = 0; index < compact.length; index++) {
        const character = compact[index];
        if (inString) {
            output += character;
            if (escaped) escaped = false;
            else if (character === "\\") escaped = true;
            else if (character === '"') inString = false;
        } else if (character === '"') {
            output += character;
            inString = true;
        } else if (character === "{" || character === "[") {
            output += character;
            depth++;
            if (compact[index + 1] !== "}" && compact[index + 1] !== "]") output += "\n" + "  ".repeat(depth);
        } else if (character === "}" || character === "]") {
            depth--;
            if (compact[index - 1] !== "{" && compact[index - 1] !== "[") output += "\n" + "  ".repeat(depth);
            output += character;
        } else if (character === ",") {
            output += ",\n" + "  ".repeat(depth);
        } else if (character === ":") {
            output += ": ";
        } else {
            output += character;
        }
    }
    return output;
}
