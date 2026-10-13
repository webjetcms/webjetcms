import { getDocumentationUrl } from './documentation-links';
import { getReleaseNews, newsSeenState, saveNewsSeen } from './release-news';

const text = (key, ...params) => WJ.translate(`admin.help.${key}.js`, ...params);

/** Opens only HTTP(S) support destinations, including the existing bare-host translation values. */
export function supportUrl(value) {
    if (typeof value !== 'string' || !value.trim()) return null;
    try {
        const url = new URL(/^[a-z][a-z\d+.-]*:\/\//i.test(value) ? value : `https://${value.trim()}`);
        return ['https:', 'http:'].includes(url.protocol) && !url.username && !url.password ? url.href : null;
    } catch (error) { return null; }
}

/** Copies diagnostic text on both secure installations and HTTP intranets. */
export async function copySystemInfo(value) {
    try {
        if (navigator.clipboard?.writeText) {
            await navigator.clipboard.writeText(value);
            return;
        }
    } catch (error) { /* HTTP installations and denied clipboard access use the selection fallback. */ }
    const focused = document.activeElement;
    const input = document.createElement('textarea');
    input.value = value;
    input.readOnly = true;
    input.style.cssText = 'position:fixed;top:0;left:0;opacity:0;';
    document.body.append(input);
    input.select();
    try {
        if (!document.execCommand('copy')) throw new Error('Clipboard is unavailable');
    } finally {
        input.remove();
        focused?.focus({ preventScroll: true });
    }
}

/** Installs the header menu without loading dashboard data or creating another feedback dialog. */
export function initHelpMenu() {
    const trigger = document.querySelector('#admin-help-toggle');
    if (!trigger || trigger.dataset.helpInitialized) return;
    trigger.dataset.helpInitialized = 'true';
    const panel = document.querySelector('#admin-help-menu');
    const dropdown = bootstrap.Dropdown.getOrCreateInstance(trigger);
    const feedback = panel.querySelector('.md-help-menu__feedback');

    trigger.addEventListener('show.bs.dropdown', () => {
        panel.querySelectorAll('[data-help-path]').forEach(link => { link.href = getDocumentationUrl(link.dataset.helpPath); });
        const page = panel.querySelector('.md-help-menu__page');
        const helpLink = typeof window.helpLink === 'string' ? window.helpLink.trim() : '';
        page.parentElement.hidden = !helpLink;
        if (helpLink) page.href = getDocumentationUrl(helpLink);
        const subtitle = panel.querySelector('.md-help-menu__page-title');
        subtitle.textContent = document.querySelector('.header-title')?.textContent.trim() || '';
        subtitle.hidden = !subtitle.textContent;

        const support = panel.querySelector('.md-help-menu__support');
        const destination = supportUrl(window.adminHelpContext.supportUrl);
        support.parentElement.hidden = !destination;
        if (destination) support.href = destination;

        const news = getReleaseNews();
        const release = panel.querySelector('.md-help-menu__release');
        release.parentElement.hidden = !news.items.length || !news.version;
        if (news.items.length && news.version) {
            release.href = getDocumentationUrl(`/CHANGELOG-${news.version.slice(0, 4)}?id=_${news.version.replaceAll('.', '')}`);
            panel.querySelector('.md-help-menu__release-title').textContent = text('release', news.version);
            panel.querySelector('.md-help-menu__release-count').textContent = text('releaseCount', news.items.length);
            const state = newsSeenState(news.html);
            panel.querySelector('.md-help-menu__new').hidden = !state.isNew;
            saveNewsSeen(state);
        }
    });
    feedback.addEventListener('click', () => {
        dropdown.hide();
        trigger.focus({ preventScroll: true });
        WJ.showFeedbackDialog({ onBack: () => { dropdown.show(); feedback.focus({ preventScroll: true }); } });
    });
    panel.querySelector('.md-help-menu__copy').addEventListener('click', async event => {
        event.stopPropagation();
        try {
            await copySystemInfo(`WebJET CMS ${window.adminHelpContext.version}`);
            WJ.notifySuccess(text('copied'), '', 5000);
        } catch (error) {
            WJ.notifyError(text('copyError'));
        }
    });
}
