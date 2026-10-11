import { registerWidget } from './registry';
import { node, text, icon, empty } from './widget-utils';
import { releaseNews, newsSeenState, saveNewsSeen } from '../release-news';

/** Registers the compact release carousel and its inline, full-width reading view. */
export function registerNewsWidget() {
    registerWidget({
        type: 'news', titleKey: 'admin.dashboard.news.js', icon: 'ti-sparkles', sizes: ['3x2'],
        render({ container, context, signal }) {
            const { version, items, html } = releaseNews(context.labels.changelog || '', context.config.releaseVersion);
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
            // The shared release-news module renders the localized Markdown.
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
            for (const link of [all, versionLink]) {
                link.href = window.WJ.getDocumentationUrl('/CHANGELOG');
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
            saveNewsSeen(state);
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
