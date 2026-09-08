const unquotedName = /^[\p{L}_$][\p{L}\p{N}_$-]*$/u;
const literal = /^(?:true|false|null|-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?)$/;

/**
 * Reads JSON with single quotes, unquoted names and comments without evaluating code or numbers.
 * Tokens retain the original spelling for lossless formatting; errors refer to the source text.
 * @param {string} text Original field value.
 * @returns {Array<{raw: string, type: string, position: number}>} Validated source tokens.
 */
function readObject(text) {
    const tokens = [];
    let position = 0;
    const fail = (at = position, kind = "invalid") => {
        const error = new SyntaxError(kind);
        error.position = at;
        throw error;
    };
    while (position < text.length) {
        if (/[ \t\r\n]/.test(text[position])) { position++; continue; }
        const start = position;
        const character = text[position++];
        let type = "bare";
        if (character === '"' || character === "'") {
            type = "string";
            let closed = false;
            while (position < text.length) {
                const current = text[position++];
                if (current === character) { closed = true; break; }
                if (current.charCodeAt(0) < 32) fail(position - 1);
                if (current === "\\") {
                    const escape = text[position++];
                    if (escape === "u") {
                        if (!/^[0-9a-fA-F]{4}$/.test(text.slice(position, position + 4))) fail(position);
                        position += 4;
                    } else if (escape == null || !'"\'\\/bfnrt'.includes(escape)) fail(position - 1);
                }
            }
            if (!closed) fail(text.length);
        } else if (character === "/" && text[position] === "/") {
            type = "line-comment";
            position++;
            while (position < text.length && !/[\r\n]/.test(text[position])) position++;
        } else if (character === "/" && text[position] === "*") {
            type = "block-comment";
            const end = text.indexOf("*/", position + 1);
            if (end < 0) fail(text.length);
            position = end + 2;
        } else if ("{}[]:,".includes(character)) {
            type = "punctuation";
        } else {
            while (position < text.length && !/[\s{}\[\]:,"'/]/u.test(text[position])) position++;
        }
        tokens.push({raw: text.slice(start, position), type, position: start});
    }

    const syntax = tokens.filter(token => !token.type.endsWith("comment"));
    let index = 0;
    const expect = raw => {
        if (syntax[index]?.raw !== raw) fail(syntax[index]?.position ?? text.length);
        index++;
    };
    const value = (depth = 0) => {
        const token = syntax[index++];
        if (!token) fail(text.length);
        if (token.type === "string" || (token.type === "bare" && literal.test(token.raw))) return;
        if (token.raw !== "{" && token.raw !== "[") fail(token.position);
        if (depth >= 1000) fail(token.position);
        const object = token.raw === "{";
        const end = object ? "}" : "]";
        if (syntax[index]?.raw === end) { index++; return; }
        do {
            if (object) {
                const name = syntax[index++];
                if (!name || (name.type !== "string" && !(name.type === "bare" && unquotedName.test(name.raw)))) {
                    fail(name?.position ?? text.length);
                }
                expect(":");
            }
            value(depth + 1);
            if (syntax[index]?.raw !== ",") break;
            index++;
        } while (true);
        expect(end);
    };
    value();
    if (index !== syntax.length) fail(syntax[index].position);
    if (syntax[0].raw !== "{") fail(syntax[0].position, "object");
    return tokens;
}

/**
 * Validates the supported JSON extensions without changing the original field value.
 * @param {string} text Raw field value.
 * @param {boolean} required Whether whitespace-only input must be rejected.
 * @returns {{valid: boolean, error?: string, line?: number, column?: number}}
 */
export function validateJsonObject(text, required = false) {
    if (text.trim().length === 0) return required ? {valid: false, error: "required"} : {valid: true};
    try {
        readObject(text);
        return {valid: true};
    } catch (error) {
        if (error.message === "object") return {valid: false, error: "object"};
        const result = {valid: false, error: "invalid"};
        if (error.position != null) {
            const lines = text.slice(0, error.position).split(/\r\n|\r|\n/);
            result.line = lines.length;
            result.column = lines[lines.length - 1].length + 1;
        }
        return result;
    }
}

/**
 * Indents an object using two spaces while preserving strings, comments and numeric spelling.
 * @param {string} text Raw JSON object, optionally using the supported extensions.
 * @returns {string} Formatted text, or unchanged input when empty or invalid.
 */
export function formatJsonObject(text) {
    if (text.trim().length === 0) return text;
    let tokens;
    try { tokens = readObject(text); } catch { return text; }
    let output = "";
    let depth = 0;
    let lineStart = true;
    const write = raw => {
        if (lineStart) output += "  ".repeat(depth);
        output += raw;
        lineStart = false;
    };
    const newline = () => {
        if (!lineStart) output += "\n";
        lineStart = true;
    };
    tokens.forEach((token, index) => {
        const raw = token.raw;
        if (token.type.endsWith("comment")) {
            if (!lineStart) write(" ");
            write(raw);
            newline();
        } else if (raw === "{" || raw === "[") {
            write(raw);
            depth++;
            if (tokens[index + 1]?.raw !== "}" && tokens[index + 1]?.raw !== "]") newline();
        } else if (raw === "}" || raw === "]") {
            depth--;
            if (tokens[index - 1]?.raw !== "{" && tokens[index - 1]?.raw !== "[") newline();
            write(raw);
        } else if (raw === ",") {
            write(",");
            newline();
        } else if (raw === ":") {
            write(": ");
        } else {
            write(raw);
        }
    });
    return lineStart ? output.slice(0, -1) : output;
}
