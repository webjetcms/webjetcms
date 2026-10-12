/** Adds a one-time status refresh after the existing two-factor settings popup closes. */
export function initUserMenu() {
    const trigger = document.querySelector('#dropdownMenuUser');
    if (!trigger || trigger.dataset.userInitialized) return;
    trigger.dataset.userInitialized = 'true';
    const panel = document.querySelector('#admin-user-menu');
    const twoFactor = panel.querySelector('.md-user-menu__two-factor');
    if (!twoFactor) return;
    const badge = panel.querySelector('.md-user-menu__two-factor-badge');
    const dropdown = bootstrap.Dropdown.getOrCreateInstance(trigger);
    let popup;
    let refreshing = false;

    twoFactor.addEventListener('click', () => {
        dropdown.hide();
        trigger.focus({ preventScroll: true });
        popup = WJ.openPopupDialog('/admin/2factorauth.jsp');
    });
    trigger.addEventListener('show.bs.dropdown', async () => {
        if (!popup?.closed || refreshing) return;
        const closedPopup = popup;
        refreshing = true;
        panel.setAttribute('aria-busy', 'true');
        try {
            const response = await fetch('/admin/users/2factorauth');
            if (!response.ok) return;
            const status = (await response.text()).trim();
            if (status !== 'true' && status !== '') return;
            const enabled = status === 'true';
            badge.dataset.enabled = String(enabled);
            badge.classList.toggle('is-enabled', enabled);
            badge.classList.toggle('is-disabled', !enabled);
            badge.textContent = WJ.translate(`admin.userMenu.${enabled ? 'enabled' : 'disabled'}.js`);
            if (popup === closedPopup) popup = null;
        } catch (error) {
            // Keep the last displayed status and retry when the menu opens again.
        } finally {
            refreshing = false;
            panel.setAttribute('aria-busy', 'false');
        }
    });
}
