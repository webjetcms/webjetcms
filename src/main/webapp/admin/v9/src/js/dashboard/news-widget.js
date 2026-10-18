import { registerWidget } from './registry';
import { node, text, icon } from './widget-utils';
import { releaseNews, newsSeenState, saveNewsSeen } from '../release-news';
import { socialNewsOptions, createSocialNewsClient, filterExternalNews, getExternalNewsState, saveExternalNewsSeen } from './social-news';

const SOCIAL_CHANNELS = [
    { source: 'youtube', title: 'YouTube', icon: 'ti-brand-youtube', href: 'https://www.youtube.com/@webjet-od-interway' },
    { source: 'facebook', title: 'Facebook', icon: 'ti-brand-facebook', href: 'https://www.facebook.com/profile.php?id=100066653358802' }
];

/** Registers release highlights together with the optional, backward-compatible public news feed. */
export function registerNewsWidget() {
    registerWidget({
        type: 'news', titleKey: 'admin.dashboard.news.js', icon: 'ti-sparkles', sizes: ['3x2'],
        render({ container, context, signal }) {
            const release = releaseNews(context.labels.changelog || '', context.config.releaseVersion);
            const releaseItems = release.items.map((item, position) => ({ ...item, id: `release:${position}` }));
            const socialOptions = socialNewsOptions(context.config, window.userLng);
            const client = socialOptions.enabled ? createSocialNewsClient({ ...socialOptions, signal }) : null;
            const hero = container.closest('.md-dashboard__hero');
            const welcome = hero?.querySelector('.md-dashboard__welcome');
            const state = newsSeenState(release.html);
            const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
            let items = [...releaseItems];
            let externalItems = [];
            let dotButtons = [];
            let index = items.length ? Math.floor(Math.random() * items.length) : 0;
            let expanded = false;
            let expandTrigger;
            let playing = !reducedMotion.matches;
            let hovering = false;
            let timer;
            let disposed = false;

            const externalLink = (label, href, className) => {
                const link = node('a', className, label);
                link.href = href;
                link.target = '_blank';
                link.rel = 'noopener noreferrer';
                return link;
            };
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
            const versionLink = externalLink(release.version ? text(context, 'newsVersion', release.version) : text(context, 'news'),
                window.WJ.getDocumentationUrl('/CHANGELOG'), 'md-dashboard-widget__news-version');
            const close = control(text(context, 'newsCollapse'), 'ti-arrows-minimize', () => setExpanded(false), 'md-dashboard-widget__news-close');
            close.hidden = true;
            header.append(badge, versionLink, close);

            const preview = node('button', 'md-dashboard-widget__news-preview');
            preview.type = 'button';
            preview.setAttribute('aria-expanded', 'false');
            preview.addEventListener('click', () => setExpanded(true, preview));
            const title = node('span', 'md-dashboard-widget__news-title');
            const description = node('span', 'md-dashboard-widget__news-description');
            preview.append(title, description);
            const full = node('div', 'md-dashboard-widget__news-detail');
            full.id = 'dashboard-news-detail';
            preview.setAttribute('aria-controls', full.id);
            full.hidden = true;
            const highlights = node('div', 'md-dashboard-widget__news-highlights');
            // Only the bundled, localized release announcement is rendered as HTML.
            highlights.innerHTML = release.html;
            full.append(highlights);
            const external = node('section', 'md-dashboard-widget__news-external');
            external.hidden = true;
            external.setAttribute('aria-busy', 'false');
            const externalHeading = node('h3', '', text(context, 'newsLatest'));
            const cards = node('div', 'md-dashboard-widget__news-cards');
            external.append(externalHeading, cards);
            const channels = node('div', 'md-dashboard-widget__news-channels');
            for (const channel of SOCIAL_CHANNELS) {
                const link = externalLink('', channel.href, 'md-dashboard-widget__news-channel');
                link.append(icon(channel.icon), node('span', '', text(context, channel.source === 'youtube' ? 'newsYoutubeChannel' : 'newsFacebookPage')), icon('ti-external-link'));
                channels.append(link);
            }
            full.append(external, channels);

            const navigation = node('div', 'md-dashboard-widget__news-navigation');
            const previous = control(text(context, 'newsPrevious'), 'ti-chevron-left', () => select(index - 1));
            const next = control(text(context, 'newsNext'), 'ti-chevron-right', () => select(index + 1));
            const dots = node('div', 'md-dashboard-widget__news-dots');
            const playback = control('', 'ti-player-pause', () => { playing = !playing; schedule(); }, 'md-dashboard-widget__news-playback');
            navigation.append(previous, dots, next);
            const status = node('span', 'visually-hidden');
            status.setAttribute('aria-live', 'polite');
            status.setAttribute('aria-atomic', 'true');
            const actions = node('div', 'md-dashboard-widget__news-actions');
            const details = node('button', 'btn btn-sm md-dashboard-widget__news-toggle');
            details.type = 'button';
            details.setAttribute('aria-controls', full.id);
            details.setAttribute('aria-expanded', 'false');
            details.addEventListener('click', () => setExpanded(!expanded));
            const all = externalLink('', window.WJ.getDocumentationUrl('/CHANGELOG'), 'md-dashboard-widget__news-more');
            all.append(node('span', '', text(context, 'newsChangelog')), icon('ti-git-compare'));
            actions.append(details, all, playback);
            container.append(header, preview, full, navigation, status, actions);

            function updateBadge() {
                if (disposed || signal.aborted) return;
                badge.hidden = !(releaseItems.length && state.isNew) && !(client && getExternalNewsState(externalItems, socialOptions.url).isNew);
            }

            /** A response received after collapse must not acknowledge unseen cards. */
            function markRead() {
                if (!client || !expanded || document.hidden || disposed || signal.aborted || !externalItems.length) return;
                saveExternalNewsSeen([...externalItems], socialOptions.url).then(updateBadge);
            }

            function updateControls() {
                const count = releaseItems.length + externalItems.length;
                const label = expanded ? text(context, 'newsCollapse') : count ? text(context, 'newsMore', count) : text(context, 'news');
                details.replaceChildren(node('span', '', label), icon(expanded ? 'ti-arrows-minimize' : 'ti-arrow-right'));
                navigation.hidden = expanded || items.length < 2;
                playback.hidden = expanded || items.length < 2;
            }

            function select(selected, automatic = false) {
                index = items.length ? (selected + items.length) % items.length : 0;
                const item = items[index];
                const channel = SOCIAL_CHANNELS.find(entry => entry.source === item?.source);
                const heading = channel ? `${channel.title}: ${item.title}` : item?.title || '';
                title.textContent = heading || (item ? '' : text(context, 'newsFollow'));
                title.title = title.textContent;
                title.hidden = Boolean(item && !heading);
                description.textContent = item?.description || '';
                if (!reducedMotion.matches) preview.animate?.([{ opacity: 0 }, { opacity: 1 }], { duration: 220 });
                dotButtons.forEach((dot, position) => dot.setAttribute('aria-current', String(position === index)));
                if (!automatic && item) status.textContent = `${text(context, 'newsItem', index + 1, items.length)}: ${heading} ${item.description}`;
                schedule();
            }

            /** Reconciles the carousel without resetting the selected local release item. */
            function rebuildCarousel() {
                const selectedId = items[index]?.id;
                const focusedPosition = dotButtons.indexOf(document.activeElement);
                const focusedId = items[focusedPosition]?.id;
                items = [...releaseItems];
                for (const channel of SOCIAL_CHANNELS) {
                    const latest = externalItems.find(item => item.source === channel.source);
                    if (latest) items.push(latest);
                }
                dots.replaceChildren();
                dotButtons = items.map((item, itemIndex) => {
                    const dot = node('button', 'md-dashboard-widget__news-dot');
                    dot.type = 'button';
                    dot.setAttribute('aria-label', text(context, 'newsItem', itemIndex + 1, items.length));
                    dot.addEventListener('click', () => select(itemIndex));
                    dots.append(dot);
                    return dot;
                });
                const selectedIndex = items.findIndex(item => item.id === selectedId);
                select(selectedIndex < 0 ? 0 : selectedIndex, true);
                updateControls();
                if (focusedPosition >= 0) {
                    const replacement = dotButtons[items.findIndex(item => item.id === focusedId)];
                    (expanded ? close : replacement || preview).focus({ preventScroll: true });
                }
            }

            /** Creates image elements only while the details are expanded. */
            function renderCards() {
                const focusedCard = cards.contains(document.activeElement) ? document.activeElement.closest('.md-dashboard-widget__news-card') : null;
                const focusedId = focusedCard?.dataset.newsId;
                cards.replaceChildren();
                external.hidden = !client || !externalItems.length;
                if (!expanded || !client) return;
                for (const item of externalItems) {
                    const card = externalLink('', item.link, 'md-dashboard-widget__news-card');
                    card.dataset.newsId = item.id;
                    if (item.image) {
                        const image = node('img', 'md-dashboard-widget__news-image');
                        image.alt = '';
                        image.loading = 'lazy';
                        image.crossOrigin = 'anonymous';
                        image.referrerPolicy = 'no-referrer';
                        image.width = 480;
                        image.height = 270;
                        image.addEventListener('error', () => image.remove(), { once: true });
                        image.src = item.image;
                        card.append(image);
                    }
                    const content = node('div', 'md-dashboard-widget__news-card-content');
                    const meta = node('div', 'md-dashboard-widget__news-card-meta');
                    const channel = SOCIAL_CHANNELS.find(entry => entry.source === item.source);
                    meta.append(icon(channel?.icon || 'ti-sparkles'), node('span', '', channel?.title || 'WebJET CMS'));
                    if (item.publishedAt) {
                        const date = node('time', '', new Date(item.publishedAt).toLocaleDateString(window.userLng === 'cz' ? 'cs' : window.userLng || 'sk'));
                        date.dateTime = item.publishedAt;
                        meta.append(date);
                    }
                    content.append(meta, node('h4', '', item.title));
                    if (item.description) content.append(node('p', '', item.description));
                    const action = node('span', 'md-dashboard-widget__news-card-action', text(context, item.source === 'youtube' ? 'newsWatchVideo' : 'newsOpenPost'));
                    action.append(icon('ti-external-link'));
                    content.append(action);
                    card.append(content);
                    cards.append(card);
                }
                if (focusedCard) {
                    const replacement = [...cards.children].find(card => card.dataset.newsId === focusedId);
                    (replacement || close).focus({ preventScroll: true });
                }
            }

            function applyFeed(feed) {
                if (disposed || signal.aborted) return;
                const updated = filterExternalNews(feed, release.version);
                const changed = JSON.stringify(updated) !== JSON.stringify(externalItems);
                externalItems = updated;
                if (changed) {
                    rebuildCarousel();
                    renderCards();
                }
                updateBadge();
                markRead();
            }

            async function refreshFeed(force = false) {
                if (!client || disposed || signal.aborted) return;
                external.setAttribute('aria-busy', 'true');
                try {
                    const feed = await client.refresh({ force });
                    if (disposed || signal.aborted) return;
                    applyFeed(feed);
                } catch (error) {
                    if (disposed || signal.aborted) return;
                    applyFeed([]);
                } finally {
                    if (!disposed && !signal.aborted) external.setAttribute('aria-busy', 'false');
                }
            }

            function schedule() {
                window.clearTimeout(timer);
                const label = text(context, playing ? 'newsPause' : 'newsPlay');
                playback.setAttribute('aria-label', label); playback.title = label;
                playback.replaceChildren(icon(playing ? 'ti-player-pause' : 'ti-player-play'));
                if (playing && !hovering && !expanded && !document.hidden && items.length > 1 && !signal.aborted && !disposed) {
                    timer = window.setTimeout(() => select(index + 1, true), 8000);
                }
            }

            function setExpanded(value, trigger = details) {
                if (value) expandTrigger = trigger;
                expanded = value;
                hero?.classList.toggle('is-news-expanded', value);
                if (welcome) welcome.hidden = value;
                preview.hidden = value;
                full.hidden = !value;
                close.hidden = !value;
                details.setAttribute('aria-expanded', String(value));
                preview.setAttribute('aria-expanded', String(value));
                updateControls();
                if (value) {
                    renderCards();
                    markRead();
                    refreshFeed(true);
                }
                (value ? close : expandTrigger || details).focus({ preventScroll: true });
                schedule();
            }

            const pointerEnter = () => { hovering = true; schedule(); };
            const pointerLeave = () => { hovering = false; schedule(); };
            const focusIn = event => { if (event.target !== playback) { playing = false; schedule(); } };
            const keyDown = event => { if (event.key === 'Escape' && expanded) { event.preventDefault(); setExpanded(false); } };
            const motionChanged = () => { if (reducedMotion.matches) { playing = false; schedule(); } };
            const visibilityChanged = () => { schedule(); markRead(); };
            container.addEventListener('mouseenter', pointerEnter);
            container.addEventListener('mouseleave', pointerLeave);
            container.addEventListener('focusin', focusIn);
            container.addEventListener('keydown', keyDown);
            document.addEventListener('visibilitychange', visibilityChanged);
            reducedMotion.addEventListener('change', motionChanged);
            rebuildCarousel();
            updateBadge();
            if (releaseItems.length) saveNewsSeen(state);
            if (client) {
                const cached = client.readCache();
                if (cached) applyFeed(cached);
                refreshFeed();
            }
            return () => {
                disposed = true;
                window.clearTimeout(timer);
                container.removeEventListener('mouseenter', pointerEnter);
                container.removeEventListener('mouseleave', pointerLeave);
                container.removeEventListener('focusin', focusIn);
                container.removeEventListener('keydown', keyDown);
                document.removeEventListener('visibilitychange', visibilityChanged);
                reducedMotion.removeEventListener('change', motionChanged);
                hero?.classList.remove('is-news-expanded');
                if (welcome) welcome.hidden = false;
            };
        }
    });
}
