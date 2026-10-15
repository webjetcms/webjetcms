/** Returns the documentation language for the signed-in administrator. */
export function documentationLanguage() {
    const language = window.userLng === 'cz' ? 'cs' : window.userLng;
    return ['sk', 'cs', 'en'].includes(language) ? language : 'en';
}

/**
 * Builds a documentation route using the origin resolved by the server configuration.
 * @param {string} [path='/'] - Documentation path, optionally including a query or fragment.
 * @returns {string} Absolute localized documentation URL.
 */
export function getDocumentationUrl(path = '/') {
    const route = typeof path === 'string' ? path.trim().replace(/^\/+/, '') : '';
    return `${window.adminHelpContext.documentationBaseUrl}/latest/${documentationLanguage()}/${route}`;
}
