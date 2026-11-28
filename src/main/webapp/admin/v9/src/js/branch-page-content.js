import {node as element} from './dashboard/widget-utils';

/** Lazily loads sandboxed server-rendered pages and synchronizes their scroll positions. */
export function createBranchPageContent(item, branchId, {tr, versionHeader, highlight, refresh}) {
    const root = element('div', 'pg-branch-page-content');
    const panes = [];
    const lifetime = new AbortController();
    let started = false;

    const columns = element('div', 'pg-branch-page-content__versions');
    const versions = item.workOnly ? ['work'] : ['live', 'work'];
    columns.style.setProperty('--branch-merge-columns', versions.length);
    for (const version of versions) {
        const panel = element('section', 'pg-branch-page-content__version');
        panel.dataset.branchPageVersion = version.toUpperCase();
        panel.append(versionHeader(item, version, 'content'));
        const pane = {version, panel};
        if (item[version]?.state === 'ACTIVE') {
            pane.status = element('div', 'pg-branch-page-content__status small text-muted');
            pane.status.setAttribute('role', 'status');
            pane.frame = element('iframe', 'pg-branch-page-content__frame');
            pane.frame.title = `${tr(version)} · ${item.label || item.objectKey}`;
            // Same-origin access is needed for scrolling; scripts, forms and navigation stay sandboxed.
            pane.frame.setAttribute('sandbox', 'allow-same-origin');
            pane.frame.hidden = true;
            panel.append(pane.status, pane.frame);
        } else panel.append(element('p', 'text-muted p-3', tr(`existence.${item[version]?.state || 'ABSENT'}`)));
        panes.push(pane);
        columns.append(panel);
    }
    root.append(columns);

    function position(pane) {
        const window = pane.frame?.contentWindow;
        return {x: window?.scrollX || 0, y: window?.scrollY || 0};
    }

    function scroll(pane, point) {
        if (!pane.loaded) return;
        const window = pane.frame.contentWindow;
        const doc = pane.frame.contentDocument.scrollingElement;
        const target = {x: Math.min(point.x, Math.max(0, doc.scrollWidth - doc.clientWidth)),
            y: Math.min(point.y, Math.max(0, doc.scrollHeight - doc.clientHeight))};
        const current = position(pane);
        if (Math.abs(current.x - target.x) < 1 && Math.abs(current.y - target.y) < 1) return;
        pane.expected = target;
        window.scrollTo({left: target.x, top: target.y, behavior: 'instant'});
    }

    function synchronize(pane) {
        if (!pane.frame.clientHeight) return;
        const point = position(pane);
        if (pane.expected && Math.abs(point.x - pane.expected.x) < 1 && Math.abs(point.y - pane.expected.y) < 1) {
            pane.expected = null;
            return;
        }
        pane.expected = null;
        panes.filter(other => other !== pane).forEach(other => scroll(other, point));
    }

    async function load(pane) {
        if (!pane?.frame || lifetime.signal.aborted) return;
        pane.request?.abort();
        pane.request = new AbortController();
        const signal = pane.request.signal;
        const previous = panes.find(other => other !== pane && other.loaded);
        const point = previous ? position(previous) : position(pane);
        pane.loaded = false;
        pane.frame.hidden = true;
        pane.status.hidden = false;
        pane.status.textContent = tr('loading');
        pane.panel.setAttribute('aria-busy', 'true');
        const query = new URLSearchParams({docId: item.objectKey, version: pane.version.toUpperCase(),
            liveHash: item.liveHash || '', workHash: item.workHash || '', highlight: String(highlight && pane.version === 'work')});
        try {
            const response = await fetch(`/admin/rest/branches/${branchId}/page-content?${query}`, {cache: 'no-store', signal});
            if (!response.ok) throw new Error(tr(response.status === 409 ? 'pageStale' : 'pageError'));
            const html = await response.text();
            if (signal.aborted) return;
            pane.frame.addEventListener('load', () => {
                pane.loaded = true;
                pane.status.hidden = true;
                pane.panel.removeAttribute('aria-busy');
                const doc = pane.frame.contentDocument;
                doc.addEventListener('click', event => {
                    if (event.target.closest('a')) event.preventDefault();
                }, {signal});
                pane.frame.contentWindow.addEventListener('scroll', () => synchronize(pane), {passive: true, signal});
                scroll(pane, point);
            }, {once: true, signal});
            pane.frame.srcdoc = html;
            pane.frame.hidden = false;
        } catch (error) {
            if (signal.aborted) return;
            pane.panel.removeAttribute('aria-busy');
            const retry = element('button', 'btn btn-sm btn-outline-secondary', tr('refresh'));
            retry.type = 'button';
            retry.addEventListener('click', refresh);
            pane.status.replaceChildren(element('p', 'text-danger', error.message), retry);
        }
    }

    return {element: root, load() {
        if (started) return;
        started = true;
        panes.forEach(pane => load(pane));
    }, setHighlight(value) {
        highlight = value;
        load(panes.find(pane => pane.version === 'work'));
    }, dispose() {
        lifetime.abort();
        panes.forEach(pane => pane.request?.abort());
    }};
}
