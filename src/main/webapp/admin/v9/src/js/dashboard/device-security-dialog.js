import { node, text, icon } from './widget-utils';
import { isCurrentDevice, browserLabel } from './security-events';

/** Waits for the modal backdrop and focus cleanup before opening another account view. */
export function closeAccountDialog(dialog, action) {
    dialog.signal.addEventListener('abort', () => window.setTimeout(action, 0), { once: true });
    dialog.close();
}

/**
 * Opens the compact device-blocking result and account-security actions.
 * Email links remain read-only until the user explicitly confirms blocking.
 * @param {Object} context - Shared dashboard data, translations and dialog owner.
 * @param {Object|null} securityEvent - Owned device, or null for an unavailable email link.
 * @param {boolean} [report=false] - Whether an explicit in-app action already requested blocking.
 */
export function showDeviceSecurity(context, securityEvent, report = false) {
    if (document.querySelector('.md-dashboard-modal--device-security')) return;
    const dialog = context.dashboard.showDialog(text(context, 'newDevice.secureAccount'));
    dialog.root.classList.add('md-dashboard-modal--device-security');
    dialog.root.querySelector('.modal-dialog').classList.add('modal-dialog-centered', 'modal-dialog-scrollable');
    const twoFactor = (context.data.notices || []).find(notice => notice.id === 'twoFactor');
    let busy = false;
    let error = false;
    const button = (label, action, className) => {
        const control = node('button', className, label);
        control.type = 'button';
        control.addEventListener('click', action);
        return control;
    };

    function render() {
        dialog.body.replaceChildren();
        dialog.footer.replaceChildren();
        dialog.body.setAttribute('aria-busy', String(busy));
        const later = button(text(context, 'newDevice.later'), dialog.close, 'btn btn-link text-secondary');
        dialog.footer.append(later);
        if (!securityEvent) {
            dialog.body.append(node('p', 'mb-0', text(context, 'newDevice.unavailable')));
            return;
        }
        const browser = browserLabel(securityEvent);
        const device = [browser, securityEvent.operatingSystem, securityEvent.ipAddress].filter(Boolean).join(' · ') || '—';
        if (securityEvent.reportedAt) {
            const result = node('div', 'md-dashboard-device-security__result');
            result.setAttribute('role', 'status');
            result.append(icon('ti-circle-check'), node('span', '', text(context, 'newDevice.blockedDevice', device)));
            dialog.body.append(result, node('p', '', text(context, 'newDevice.reported')),
                node('p', 'mb-0', text(context, 'newDevice.secureAdvice')));
            if (twoFactor) {
                const setup = button(text(context, 'sessionEnableTwoFactor'), () => closeAccountDialog(dialog,
                    () => window.WJ.openPopupDialog(twoFactor.action.url)), 'btn btn-white');
                setup.prepend(icon('ti-shield-lock'));
                dialog.footer.append(setup);
            }
            const password = button(text(context, 'newDevice.changePassword'), () => closeAccountDialog(dialog,
                () => window.openProfileDialog(window.currentUser.userId, true)), 'btn btn-primary');
            password.prepend(icon('ti-key'));
            dialog.footer.append(password);
        } else {
            dialog.body.append(node('p', 'md-dashboard-device-security__device', device),
                node('p', 'mb-0', text(context, 'newDevice.reportAdvice')));
            const submit = button(text(context, 'newDevice.report'), block, 'btn btn-red md-dashboard-device-security__report');
            submit.disabled = busy;
            dialog.footer.append(submit);
        }
        if (busy || error) {
            const status = node('p', `mt-3 mb-0${error ? ' text-danger' : ''}`, text(context, error ? 'newDevice.saveError' : 'loading'));
            status.setAttribute('role', error ? 'alert' : 'status');
            dialog.body.append(status);
        }
    }

    async function block() {
        if (busy || dialog.signal.aborted || securityEvent.reportedAt) return;
        busy = true;
        error = false;
        render();
        try {
            const response = await fetch(`/admin/rest/security/login-events/${encodeURIComponent(securityEvent.id)}/report`, {
                method: 'POST', credentials: 'same-origin', signal: dialog.signal,
                headers: { 'X-CSRF-Token': window.csrfToken || '' }
            });
            if (!response.ok) throw new Error('Login reporting failed');
            const updated = await response.json();
            if (updated.id !== securityEvent.id || !updated.reportedAt) throw new Error('Login report was not saved');
            if (dialog.signal.aborted) return;
            securityEvent = { ...securityEvent, ...updated };
            for (const cluster of context.data.currentSessions?.userSessions || []) {
                for (const session of cluster.userSessions || []) {
                    if (session.deviceId === updated.id) { session.deviceConfirmed = false; session.pending = true; }
                }
            }
            context.data.notices = (context.data.notices || []).filter(notice => notice.securityEvent?.id !== updated.id);
            if (context.data.requestedSecurityEvent?.id === updated.id) context.data.requestedSecurityEvent = securityEvent;
            context.dashboard.refreshSessions();
            context.dashboard.refreshLoggedAdmins?.();
            if (isCurrentDevice(context.data, updated.id)) document.forms.namedItem('adminLogoffForm')?.requestSubmit();
        } catch (failure) {
            if (!dialog.signal.aborted) error = true;
        } finally {
            busy = false;
            if (!dialog.signal.aborted) {
                render();
                dialog.footer.querySelector('button')?.focus({ preventScroll: true });
            }
        }
    }

    render();
    if (report && securityEvent && !securityEvent.reportedAt) block();
}
