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
            let previewImage;
            const failedPreviewImages = new Set();
            // Size containment keeps the CSS-clamped text from changing the measured card height.
            const summaryObserver = window.ResizeObserver ? new window.ResizeObserver(entries => {
                if (disposed || signal.aborted || !expanded) return;
                for (const { target, contentRect } of entries) {
                    const lineHeight = parseFloat(window.getComputedStyle(target).lineHeight);
                    if (contentRect.height > 0 && lineHeight > 0) {
                        const lines = Math.max(1, Math.floor((contentRect.height + 0.1) / lineHeight));
                        target.style.setProperty('--news-summary-lines', String(lines));
                    }
                }
            }) : null;

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
            const sourceLabel = node('span', 'md-dashboard-widget__news-source');
            sourceLabel.hidden = true;
            const close = control(text(context, 'newsCollapse'), 'ti-arrows-minimize', () => setExpanded(false), 'md-dashboard-widget__news-close');
            close.hidden = true;
            header.append(badge, versionLink, sourceLabel, close);

            const preview = node('button', 'md-dashboard-widget__news-preview');
            preview.type = 'button';
            preview.setAttribute('aria-expanded', 'false');
            preview.addEventListener('click', () => setExpanded(true, preview));
            const title = node('span', 'md-dashboard-widget__news-title');
            const description = node('span', 'md-dashboard-widget__news-description');
            const previewText = node('span', 'md-dashboard-widget__news-preview-text');
            previewText.append(title, description);
            preview.append(previewText);
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
            channels.hidden = true;
            for (const channel of SOCIAL_CHANNELS) {
                const link = externalLink('', channel.href, 'md-dashboard-widget__news-channel');
                link.append(icon(channel.icon), node('span', '', text(context, channel.source === 'youtube' ? 'newsYoutubeChannel' : 'newsFacebookPage')), icon('ti-external-link'));
                channels.append(link);
            }
            full.append(external);

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
            actions.append(details, all, playback, channels);
            container.append(header, preview, full, navigation, status, actions);

            function updateBadge() {
                if (disposed || signal.aborted) return;
                badge.hidden = !(releaseItems.length && newsSeenState(release.html).isNew) && !(client && getExternalNewsState(externalItems, socialOptions.url).isNew);
            }

            /** A response received after collapse must not acknowledge unseen cards. */
            function markRead() {
                if (!expanded || document.hidden || disposed || signal.aborted) return;
                const saves = [];
                if (releaseItems.length) saves.push(saveNewsSeen(newsSeenState(release.html)));
                if (client && externalItems.length) saves.push(saveExternalNewsSeen([...externalItems], socialOptions.url));
                Promise.all(saves).then(updateBadge);
            }

            function updateControls() {
                const count = releaseItems.length + externalItems.length;
                const label = expanded ? text(context, 'newsCollapse') : count ? text(context, 'newsMore', count) : text(context, 'news');
                details.replaceChildren(node('span', '', label), icon(expanded ? 'ti-arrows-minimize' : 'ti-arrow-right'));
                navigation.hidden = expanded || items.length < 2;
                playback.hidden = expanded || items.length < 2;
                channels.hidden = !expanded;
                actions.classList.toggle('is-expanded', expanded);
            }

            function select(selected, automatic = false) {
                index = items.length ? (selected + items.length) % items.length : 0;
                const item = items[index];
                const channel = SOCIAL_CHANNELS.find(entry => entry.source === item?.source);
                const heading = channel ? `${channel.title}: ${item.title}` : item?.title || '';
                title.textContent = item?.title || (item ? '' : text(context, 'newsFollow'));
                title.title = title.textContent;
                title.hidden = Boolean(item && !heading);
                description.textContent = item?.description || '';
                updatePreviewMedia();
                if (!reducedMotion.matches) preview.animate?.([{ opacity: 0 }, { opacity: 1 }], { duration: 220 });
                dotButtons.forEach((dot, position) => dot.setAttribute('aria-current', String(position === index)));
                if (!automatic && item) status.textContent = `${text(context, 'newsItem', index + 1, items.length)}: ${heading} ${item.description}`;
                schedule();
            }

            /** Loads only the selected collapsed thumbnail and hides preview metadata in the expanded panel. */
            function updatePreviewMedia() {
                const item = items[index];
                const channel = !expanded && SOCIAL_CHANNELS.find(entry => entry.source === item?.source);
                versionLink.hidden = expanded || Boolean(channel);
                sourceLabel.hidden = !channel;
                sourceLabel.replaceChildren(...(channel ? [icon(channel.icon), node('span', '', channel.title)] : []));
                const imageUrl = !expanded ? item?.image : null;
                if (imageUrl && previewImage?.getAttribute('src') === imageUrl) return;
                previewImage?.remove();
                previewImage = null;
                if (!imageUrl || failedPreviewImages.has(imageUrl)) return;
                const image = node('img', 'md-dashboard-widget__news-preview-image');
                image.alt = '';
                image.crossOrigin = 'anonymous';
                image.referrerPolicy = 'no-referrer';
                image.decoding = 'async';
                image.width = 160;
                image.height = 90;
                image.addEventListener('error', () => {
                    failedPreviewImages.add(imageUrl);
                    image.remove();
                    if (previewImage === image) previewImage = null;
                }, { once: true });
                image.src = imageUrl;
                previewImage = image;
                preview.prepend(image);
            }

            /** Reconciles the carousel without resetting the selected local release item. */
            function rebuildCarousel() {
                const selectedId = items[index]?.id;
                const focusedPosition = dotButtons.indexOf(document.activeElement);
                const focusedId = items[focusedPosition]?.id;
                const socialItems = externalItems.filter(item => SOCIAL_CHANNELS.some(channel => channel.source === item.source)).slice(0, 5);
                items = [...releaseItems, ...socialItems];
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
                summaryObserver?.disconnect();
                cards.replaceChildren();
                external.hidden = !client || !externalItems.length;
                if (!expanded || !client) return;
                for (const item of externalItems) {
                    const card = externalLink('', item.link, 'md-dashboard-widget__news-card');
                    card.dataset.newsId = item.id;
                    card.classList.toggle('is-text-only', !item.image);
                    if (item.image) {
                        const image = node('img', 'md-dashboard-widget__news-image');
                        image.alt = '';
                        image.loading = 'lazy';
                        image.crossOrigin = 'anonymous';
                        image.referrerPolicy = 'no-referrer';
                        image.width = 480;
                        image.height = 270;
                        image.addEventListener('error', () => { image.remove(); card.classList.add('is-text-only'); }, { once: true });
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
                    const summary = item.descriptionLong || item.description;
                    if (summary) {
                        const summaryBox = node('div', 'md-dashboard-widget__news-card-summary');
                        summaryBox.append(node('p', '', summary));
                        content.append(summaryBox);
                        summaryObserver?.observe(summaryBox);
                    }
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
                updatePreviewMedia();
                if (value) {
                    renderCards();
                    markRead();
                    refreshFeed(true);
                } else summaryObserver?.disconnect();
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
            if (client) {
                const cached = client.readCache();
                if (cached) applyFeed(cached);
                refreshFeed();
            }
            return () => {
                disposed = true;
                summaryObserver?.disconnect();
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
