/** Separates device details from location and time, keeping the localized date together. */
function loginNotice(login, locale) {
    const browser = [login.browserName, (login.browserVersion || '').replace(/\.0$/, '')].filter(Boolean).join(' ');
    const timestamp = new Date(login.lastSeen);
    const time = timestamp.toLocaleTimeString(locale, { hour: '2-digit', minute: '2-digit' });
    const when = timestamp.toDateString() === new Date().toDateString()
        ? WJ.translate('admin.userMenu.today.js', time)
        : `${timestamp.toLocaleDateString(locale)} ${time}`;
    const item = document.createElement('li');
    const client = document.createElement('div');
    client.className = 'md-user-menu__notice-client';
    client.textContent = [browser, login.operatingSystem].filter(Boolean).join(' · ');
    const detail = document.createElement('div');
    detail.className = 'md-user-menu__notice-detail';
    detail.textContent = login.location ? `${login.location} · ` : '';
    const date = document.createElement('span');
    date.className = 'text-nowrap';
    date.textContent = when;
    detail.append(date);
    item.append(client, detail);
    return item;
}

/** Refreshes account status on each opening without delaying the Bootstrap dropdown. */
export function initUserMenu() {
    const trigger = document.querySelector('#dropdownMenuUser');
    if (!trigger || trigger.dataset.userInitialized) return;
    trigger.dataset.userInitialized = 'true';
    const panel = document.querySelector('#admin-user-menu');
    const twoFactor = panel.querySelector('.md-user-menu__two-factor');
    const badge = panel.querySelector('.md-user-menu__two-factor-badge');
    const sessions = panel.querySelector('.md-user-menu__session-count');
    const hint = panel.querySelector('.md-user-menu__session-hint');
    const notice = panel.querySelector('.md-user-menu__notice-container');
    const noticeList = panel.querySelector('.md-user-menu__notice-list');
    const locale = (window.userLng === 'cz' ? 'cs' : window.userLng) || 'sk';
    const plurals = new Intl.PluralRules(locale);
    const badges = [sessions, badge].filter(Boolean);
    let refreshing = false;

    twoFactor?.addEventListener('click', () => {
        bootstrap.Dropdown.getOrCreateInstance(trigger).hide();
        trigger.focus({ preventScroll: true });
        WJ.openPopupDialog('/admin/2factorauth.jsp');
    });
    trigger.addEventListener('show.bs.dropdown', async () => {
        if (refreshing) return;
        refreshing = true;
        panel.setAttribute('aria-busy', 'true');
        hint.textContent = '';
        notice.hidden = true;
        noticeList.replaceChildren();
        for (const item of badges) {
            item.textContent = '';
            item.classList.remove('is-enabled', 'is-disabled');
            item.classList.add('is-loading');
            delete item.dataset.enabled;
            item.removeAttribute('title');
        }
        try {
            const response = await fetch('/admin/rest/user-menu', {
                cache: 'no-store', headers: { Accept: 'application/json', 'X-CSRF-Token': window.csrfToken }
            });
            if (!response.ok) throw new Error('Account status unavailable');
            const status = await response.json();
            sessions.textContent = status.sessionCount;
            const others = Math.max(0, status.sessionCount - 1);
            hint.textContent = WJ.translate(status.sessionCount === 0 ? 'admin.userMenu.sessionsHint.js' : others === 0
                ? 'admin.userMenu.thisDevice.js' : `admin.userMenu.otherSessions.${plurals.select(others)}.js`, others);
            if (status.newLogins.length > 0) {
                hint.textContent += ` · ${WJ.translate(`admin.userMenu.newCount.${plurals.select(status.newLogins.length)}.js`, status.newLogins.length)}`;
                for (const login of status.newLogins) {
                    noticeList.append(loginNotice(login, locale));
                }
                notice.hidden = false;
            }
            if (badge) {
                badge.dataset.enabled = String(status.twoFactorEnabled);
                badge.classList.toggle('is-enabled', status.twoFactorEnabled);
                badge.classList.toggle('is-disabled', !status.twoFactorEnabled);
                badge.textContent = WJ.translate(`admin.userMenu.${status.twoFactorEnabled ? 'enabled' : 'disabled'}.js`);
            }
        } catch (error) {
            hint.textContent = WJ.translate('admin.userMenu.unavailable.js');
            for (const item of badges) {
                item.textContent = '—';
                item.title = hint.textContent;
            }
        } finally {
            for (const item of badges) item.classList.remove('is-loading');
            refreshing = false;
            panel.setAttribute('aria-busy', 'false');
        }
    });
}
