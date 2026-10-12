import { node } from './dashboard/widget-utils';

const markdownTags = { A: 'a', B: 'strong', I: 'em', BR: 'br', H1: 'h1', H2: 'h2', H3: 'h3', BLOCKQUOTE: 'blockquote', UL: 'ul', LI: 'li' };

/** Copies only formatting produced by WJ.parseMarkdown, never response attributes or active elements. */
function appendMarkdown(parent, source) {
    if (source.nodeType === 3) { parent.append(document.createTextNode(source.textContent)); return; }
    if (source.nodeType !== 1) return;
    if (source.tagName === 'IMG') { parent.append(document.createTextNode(source.getAttribute('alt') || '')); return; }
    if (['DIV', 'SPAN'].includes(source.tagName) && (source.classList.contains('code') || source.classList.contains('code-inline'))) {
        source.querySelectorAll('br').forEach(lineBreak => lineBreak.replaceWith('\n'));
        const code = node('code', '', source.textContent);
        if (source.tagName === 'DIV') {
            const pre = node('pre');
            pre.append(code);
            parent.append(pre);
        } else parent.append(code);
        return;
    }
    const tag = markdownTags[source.tagName];
    if (!tag || source.namespaceURI !== 'http://www.w3.org/1999/xhtml') return;
    const target = node(tag);
    if (tag === 'a') target.setAttribute('href', source.getAttribute('href') || '');
    source.childNodes.forEach(child => appendMarkdown(target, child));
    parent.append(target);
}

/** Resolves links on the configured HTTP(S) origin and upgrades legacy versioned documentation routes. */
export function documentationUrl(value, base) {
    if (typeof value !== 'string' || !value.trim()) return null;
    try {
        const url = new URL(value, base);
        if (!['http:', 'https:'].includes(url.protocol) || url.origin !== new URL(base).origin || url.username || url.password) return null;
        const version = url.pathname.match(/^(\/(?:latest|v[0-9]+))\/(?:index\.(?:html|jsp))?$/);
        if (version && /^#\/(sk|cs|en)(?:\/|$)/.test(url.hash)) {
            return new URL(`${version[1]}${url.hash.slice(1)}`, url.origin).href;
        }
        return url.href;
    } catch (error) { return null; }
}

/** Sanitizes the shared Markdown renderer's output and resolves source-relative documentation links. */
export function documentationMarkdown(value, base) {
    const content = node('div', 'md-quick-search__markdown');
    const template = document.createElement('template');
    template.innerHTML = WJ.parseMarkdown(`\n${typeof value === 'string' ? value : ''}\n\n`, { imgSrcPrefix: null, removeLastBr: true });
    template.content.childNodes.forEach(child => appendMarkdown(content, child));
    while (content.firstChild?.nodeName === 'BR') content.firstChild.remove();
    while (content.lastChild?.nodeName === 'BR') content.lastChild.remove();
    content.querySelectorAll('a').forEach(link => {
        const href = documentationUrl(link.getAttribute('href'), base);
        if (!href) { link.replaceWith(...link.childNodes); return; }
        const url = new URL(href);
        const source = url.pathname.match(/^(.*?)\/(sk|cs|en)\/(.*)\.md$/i);
        if (source) {
            let anchor = url.hash.slice(1);
            try { anchor = decodeURIComponent(anchor); } catch (error) { /* Keep literal percent signs in incomplete fragment escapes. */ }
            if (anchor) url.searchParams.set('id', anchor);
            const section = url.search;
            url.search = '';
            url.hash = `/${source[2]}/${source[3].replace(/(^|\/)README$/i, '$1')}${section}`;
            url.pathname = `${source[1]}/`;
        }
        link.href = documentationUrl(url.href, base);
        link.target = '_blank';
        link.rel = 'noopener noreferrer';
    });
    return content;
}
