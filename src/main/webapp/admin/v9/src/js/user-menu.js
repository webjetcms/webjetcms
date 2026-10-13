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
    const newLogins = panel.querySelector('.md-user-menu__new-logins');
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
        newLogins.textContent = '';
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
            hint.textContent = status.deviceCount == null
                ? WJ.translate('admin.userMenu.sessionsHint.js')
                : WJ.translate('admin.userMenu.devices.js', status.deviceCount);
            if (status.newLoginCount > 0) newLogins.textContent = WJ.translate('admin.userMenu.newLogins.js', status.newLoginCount);
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
