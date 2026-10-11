import { registerWidget } from './registry';
import { node, text, icon, empty } from './widget-utils';

const NEWS_LIFETIME = 30 * 24 * 60 * 60 * 1000;

/**
 * Extracts list-item previews while retaining the complete rendered Markdown for the release detail.
 * Older paragraph announcements remain readable until their translations are converted to lists.
 * @param {import('./registry').WidgetContext} context - Supplies the localized announcement HTML.
 * @returns {{version: string, items: {title: string, description: string}[], html: string}} Release content.
 */
export function releaseNews(context) {
    const html = context.labels.changelog || '';
    const parsed = new DOMParser().parseFromString(html, 'text/html');
    const version = parsed.body.textContent.match(/\b20\d{2}\.\d+(?:\.\d+)?\b/)?.[0] || context.config.releaseVersion || '';
    const items = [...parsed.body.querySelectorAll('li')].filter(item => !item.parentElement.closest('li')).map(item => {
        const copy = item.cloneNode(true);
        const heading = copy.querySelector('b, strong');
        const title = heading?.textContent.trim() || '';
        heading?.remove();
        return { title, description: copy.textContent.trim().replace(/^[–—:-]\s*/, '') };
    });
    if (!items.length) {
        parsed.body.querySelectorAll('br').forEach(br => br.replaceWith(parsed.createTextNode('\n')));
        parsed.body.querySelectorAll('p, h1, h2, h3, h4, h5, h6, blockquote').forEach(block => block.append(parsed.createTextNode('\n\n')));
        for (const paragraph of parsed.body.textContent.replace(/\\n/g, '\n').split(/\n\s*\n/).map(value => value.trim()).filter(Boolean)) {
            items.push({ title: '', description: paragraph });
        }
    }
    return { version, items, html };
}

/**
 * Produces a non-security FNV-1a fingerprint without requiring HTTPS-only Web Crypto APIs.
 * @param {string} content - Complete localized announcement, including formatting changes.
 * @returns {string} Stable 64-bit content fingerprint.
 */
export function newsFingerprint(content) {
    let hash = 14695981039346656037n;
    for (let index = 0; index < content.length; index++) hash = BigInt.asUintN(64, (hash ^ BigInt(content.charCodeAt(index))) * 1099511628211n);
    return hash.toString(16);
}

/**
 * Resolves the signed-in account's thirty-day window for this localized text.
 * Separate language records prevent language switching from repeatedly resetting the badge.
 * @param {string} html - Current announcement HTML.
 * @param {number} [now=Date.now()] - Current time in milliseconds.
 * @returns {{key: string, value: string, changed: boolean, isNew: boolean}} State to persist only when the text changed.
 */
export function newsSeenState(html, now = Date.now()) {
    const key = `dashboard.news.${window.userLng || 'sk'}`;
    const fingerprint = newsFingerprint(html);
    let saved;
    try { saved = JSON.parse(window.currentUser?.adminSettings?.[key] || 'null'); }
    catch (error) { /* Invalid preferences start a new reading window. */ }
    const changed = saved?.fingerprint !== fingerprint || !Number.isFinite(saved?.firstSeen) || saved.firstSeen <= 0;
    const firstSeen = changed ? now : saved.firstSeen;
    return { key, value: JSON.stringify({ fingerprint, firstSeen }), changed, isNew: now < firstSeen + NEWS_LIFETIME };
}

/** Persists only the release-reading record, leaving dashboard layouts and notices unchanged. */
async function saveNewsSeen(state, signal, context) {
    if (!state.changed) return;
    try {
        const response = await fetch('/admin/rest/admin-settings/', {
            method: 'POST', credentials: 'same-origin', signal,
            headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': window.csrfToken || '' },
            body: JSON.stringify({ label: state.key, value: state.value })
        });
        if (!response.ok || await response.json() !== true) throw new Error('Release preferences could not be saved');
        if (!signal.aborted && window.currentUser?.adminSettings) window.currentUser.adminSettings[state.key] = state.value;
    } catch (error) {
        if (!signal.aborted) window.WJ.notifyError?.(text(context, 'news'), text(context, 'saveError'));
    }
}

/** Registers the compact release carousel and its inline, full-width reading view. */
export function registerNewsWidget() {
    registerWidget({
        type: 'news', titleKey: 'admin.dashboard.news.js', icon: 'ti-sparkles', sizes: ['3x2'],
        render({ container, context, signal }) {
            const { version, items, html } = releaseNews(context);
            if (!items.length) { empty(container, context); return; }
            const hero = container.closest('.md-dashboard__hero');
            const welcome = hero?.querySelector('.md-dashboard__welcome');
            const state = newsSeenState(html);
            const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
            let index = Math.floor(Math.random() * items.length);
            let expanded = false;
            let expandTrigger;
            let playing = !reducedMotion.matches;
            let hovering = false;
            let timer;

            const control = (label, glyph, action, className = '') => {
                const button = node('button', `btn btn-sm md-dashboard-widget__news-control ${className}`.trim());
                button.type = 'button';
                button.setAttribute('aria-label', label);
                button.title = label;
                button.append(icon(glyph));
                button.addEventListener('click', action);
                return button;
            };
            const header = node('div', 'md-dashboard-widget__news-header');
            const badge = node('span', 'md-dashboard-widget__news-badge', text(context, 'newsNew'));
            badge.hidden = !state.isNew;
            const versionLink = node('a', 'md-dashboard-widget__news-version', version ? text(context, 'newsVersion', version) : text(context, 'news'));
            header.append(badge, versionLink);
            const close = control(text(context, 'newsCollapse'), 'ti-arrows-minimize', () => setExpanded(false), 'md-dashboard-widget__news-close');
            close.hidden = true;
            header.append(close);

            const preview = node('button', 'md-dashboard-widget__news-preview');
            preview.type = 'button';
            preview.setAttribute('aria-expanded', 'false');
            preview.addEventListener('click', () => setExpanded(true, preview));
            const title = node('span', 'md-dashboard-widget__news-title');
            const description = node('span', 'md-dashboard-widget__news-description');
            preview.append(title, description);
            const full = node('div', 'md-dashboard-widget__news-highlights');
            full.id = 'dashboard-news-detail';
            preview.setAttribute('aria-controls', full.id);
            full.hidden = true;
            // HTML is produced by WebJET's Markdown renderer in overview.pug.
            full.innerHTML = html;
            const navigation = node('div', 'md-dashboard-widget__news-navigation');
            const previous = control(text(context, 'newsPrevious'), 'ti-chevron-left', () => select(index - 1));
            const next = control(text(context, 'newsNext'), 'ti-chevron-right', () => select(index + 1));
            const dots = node('div', 'md-dashboard-widget__news-dots');
            const dotButtons = items.map((item, itemIndex) => {
                const dot = node('button', 'md-dashboard-widget__news-dot');
                dot.type = 'button';
                dot.setAttribute('aria-label', text(context, 'newsItem', itemIndex + 1, items.length));
                dot.addEventListener('click', () => select(itemIndex));
                dots.append(dot);
                return dot;
            });
            const playback = control('', 'ti-player-pause', () => { playing = !playing; schedule(); }, 'md-dashboard-widget__news-playback');
            playback.hidden = items.length < 2;
            navigation.append(previous, dots, next);
            navigation.hidden = items.length < 2;
            const status = node('span', 'visually-hidden');
            status.setAttribute('aria-live', 'polite');
            status.setAttribute('aria-atomic', 'true');
            const actions = node('div', 'md-dashboard-widget__news-actions');
            const details = node('button', 'btn btn-sm md-dashboard-widget__news-toggle');
            details.type = 'button';
            details.setAttribute('aria-controls', full.id);
            details.setAttribute('aria-expanded', 'false');
            details.append(node('span', '', text(context, 'newsMore', items.length)), icon('ti-arrow-right'));
            details.addEventListener('click', () => setExpanded(!expanded));
            const all = node('a', 'md-dashboard-widget__news-more');
            const language = ['sk', 'cs', 'en'].includes(window.userLng) ? window.userLng : window.userLng === 'cz' ? 'cs' : 'en';
            for (const link of [all, versionLink]) {
                link.href = `https://docs.webjetcms.sk/latest/${language}/CHANGELOG`;
                link.target = '_blank'; link.rel = 'noopener';
            }
            all.append(node('span', '', text(context, 'newsChangelog')), icon('ti-git-compare'));
            actions.append(details, all, playback);
            container.append(header, preview, full, navigation, status, actions);

            function select(selected, automatic = false) {
                index = (selected + items.length) % items.length;
                title.textContent = items[index].title;
                title.title = items[index].title;
                title.hidden = !items[index].title;
                description.textContent = items[index].description;
                if (!reducedMotion.matches) preview.animate?.([{ opacity: 0 }, { opacity: 1 }], { duration: 220 });
                dotButtons.forEach((dot, position) => dot.setAttribute('aria-current', String(position === index)));
                if (!automatic) status.textContent = `${text(context, 'newsItem', index + 1, items.length)}: ${items[index].title} ${items[index].description}`;
                schedule();
            }

            function schedule() {
                window.clearTimeout(timer);
                const label = text(context, playing ? 'newsPause' : 'newsPlay');
                playback.setAttribute('aria-label', label); playback.title = label;
                playback.replaceChildren(icon(playing ? 'ti-player-pause' : 'ti-player-play'));
                if (playing && !hovering && !expanded && !document.hidden && items.length > 1 && !signal.aborted) {
                    timer = window.setTimeout(() => select(index + 1, true), 8000);
                }
            }

            function setExpanded(value, trigger = details) {
                if (value) expandTrigger = trigger;
                expanded = value;
                hero?.classList.toggle('is-news-expanded', value);
                if (welcome) welcome.hidden = value;
                preview.hidden = value;
                navigation.hidden = value || items.length < 2;
                playback.hidden = value || items.length < 2;
                full.hidden = !value;
                close.hidden = !value;
                details.setAttribute('aria-expanded', String(value));
                preview.setAttribute('aria-expanded', String(value));
                details.replaceChildren(node('span', '', text(context, value ? 'newsCollapse' : 'newsMore', items.length)), icon(value ? 'ti-arrows-minimize' : 'ti-arrow-right'));
                (value ? close : expandTrigger || details).focus({ preventScroll: true });
                schedule();
            }

            const pointerEnter = () => { hovering = true; schedule(); };
            const pointerLeave = () => { hovering = false; schedule(); };
            const focusIn = event => { if (event.target !== playback) { playing = false; schedule(); } };
            const keyDown = event => { if (event.key === 'Escape' && expanded) { event.preventDefault(); setExpanded(false); } };
            const motionChanged = () => { if (reducedMotion.matches) { playing = false; schedule(); } };
            container.addEventListener('mouseenter', pointerEnter);
            container.addEventListener('mouseleave', pointerLeave);
            container.addEventListener('focusin', focusIn);
            container.addEventListener('keydown', keyDown);
            document.addEventListener('visibilitychange', schedule);
            reducedMotion.addEventListener('change', motionChanged);
            select(index, true);
            saveNewsSeen(state, signal, context);
            return () => {
                window.clearTimeout(timer);
                container.removeEventListener('mouseenter', pointerEnter);
                container.removeEventListener('mouseleave', pointerLeave);
                container.removeEventListener('focusin', focusIn);
                container.removeEventListener('keydown', keyDown);
                document.removeEventListener('visibilitychange', schedule);
                reducedMotion.removeEventListener('change', motionChanged);
                hero?.classList.remove('is-news-expanded');
                if (welcome) welcome.hidden = false;
            };
        }
    });
}
