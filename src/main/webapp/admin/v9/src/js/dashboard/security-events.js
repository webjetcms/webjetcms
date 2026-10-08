/**
 * Confirms an owned device and applies the server result to notices and all sessions of that device.
 * @param {Object} data - Shared dashboard bootstrap.
 * @param {number} deviceId - Account-owned device identifier.
 * @param {AbortSignal} signal - Cancels confirmation when its view closes.
 * @param {{code?: string, token?: string}} proof - One-time proof delivered by email.
 * @returns {Promise<Object>} Saved device confirmation.
 */
export async function confirmSecurityEvent(data, deviceId, signal, proof) {
    const response = await fetch(`/admin/rest/security/login-events/${encodeURIComponent(deviceId)}/confirm`, {
        method: 'POST', credentials: 'same-origin', signal,
        headers: { 'X-CSRF-Token': window.csrfToken || '', 'Content-Type': 'application/json' },
        body: JSON.stringify(proof)
    });
    if (!response.ok) throw new Error('Login confirmation failed');
    const event = await response.json();
    if (event.id !== deviceId || !event.confirmedAt) throw new Error('Login confirmation was not saved');
    if (signal.aborted) return event;
    for (const cluster of data.currentSessions?.userSessions || []) {
        for (const session of cluster.userSessions || []) {
            if (session.deviceId === deviceId) session.deviceConfirmed = true;
        }
    }
    data.notices = (data.notices || []).filter(notice => {
        if (notice.securityEvent?.id !== deviceId) return true;
        Object.assign(notice.securityEvent, event);
        return false;
    });
    if (data.requestedSecurityEvent?.id === deviceId) Object.assign(data.requestedSecurityEvent, event);
    return event;
}

/** Matches the server-bound device ID, never a browser label or an IP address. */
export function isCurrentDevice(data, deviceId) {
    if (!data.currentSessions?.currentSessionId || deviceId == null) return false;
    return (data.currentSessions?.userSessions || []).some(cluster => (cluster.userSessions || [])
        .some(session => session.sessionId === data.currentSessions.currentSessionId && session.deviceId === deviceId));
}

/**
 * Mounts the same email-code flow inside a notice or a device row.
 * @param {Object} options - Shared data, device ID, host, abort signal, translation and success callback.
 */
export function showDeviceConfirmation({ data, deviceId, host, signal, translate, onConfirmed }) {
    const form = document.createElement('form');
    form.className = 'md-dashboard-device-confirmation';
    const label = document.createElement('label');
    label.className = 'form-label';
    const labelText = document.createElement('span');
    labelText.className = 'visually-hidden';
    labelText.textContent = translate('newDevice.codeLabel');
    const input = document.createElement('input');
    input.className = 'form-control form-control-sm';
    input.type = 'text';
    input.name = 'deviceConfirmationCode';
    input.inputMode = 'numeric';
    input.autocomplete = 'one-time-code';
    input.pattern = '[0-9]{6}';
    input.maxLength = 6;
    input.required = true;
    label.append(labelText, input);
    const status = document.createElement('p');
    status.className = 'small';
    status.setAttribute('role', 'status');
    const actions = document.createElement('div');
    actions.className = 'd-flex flex-wrap gap-2';
    const verify = document.createElement('button');
    verify.type = 'submit';
    verify.className = 'btn btn-sm btn-primary';
    verify.textContent = translate('newDevice.verifyCode');
    const resend = document.createElement('button');
    resend.type = 'button';
    resend.className = 'btn btn-sm btn-white';
    resend.textContent = translate('newDevice.resendCode');
    actions.append(verify, resend);
    form.append(status, label, actions);
    host.classList.add('has-device-confirmation');
    host.replaceChildren(form);
    let busy = false;
    const setBusy = value => {
        busy = value;
        input.disabled = verify.disabled = resend.disabled = value;
        form.setAttribute('aria-busy', String(value));
    };
    async function sendCode() {
        if (busy || signal.aborted) return;
        setBusy(true);
        status.textContent = '';
        try {
            const response = await fetch(`/admin/rest/security/login-events/${encodeURIComponent(deviceId)}/code`, {
                method: 'POST', credentials: 'same-origin', signal,
                headers: { 'X-CSRF-Token': window.csrfToken || '' }
            });
            if (signal.aborted) return;
            status.textContent = translate(response.ok ? 'newDevice.codeSent'
                : response.status === 429 ? 'newDevice.codeWait' : 'newDevice.codeSendError');
            if (response.ok) input.value = '';
        } catch (error) {
            if (!signal.aborted) status.textContent = translate('newDevice.codeSendError');
        } finally {
            if (!signal.aborted) { setBusy(false); input.focus({ preventScroll: true }); }
        }
    }
    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (busy || signal.aborted || !form.reportValidity()) return;
        const code = input.value;
        setBusy(true);
        status.textContent = '';
        try {
            const updated = await confirmSecurityEvent(data, deviceId, signal, { code });
            if (!signal.aborted) onConfirmed(updated);
        } catch (error) {
            if (!signal.aborted) status.textContent = translate('newDevice.codeInvalid');
        } finally {
            if (!signal.aborted && form.isConnected) { setBusy(false); input.focus({ preventScroll: true }); }
        }
    });
    resend.addEventListener('click', sendCode);
    sendCode();
}
