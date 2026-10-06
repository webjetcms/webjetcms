/**
 * Confirms an owned device and applies the server result to notices and all sessions of that device.
 * @param {Object} data - Shared dashboard bootstrap.
 * @param {number} deviceId - Account-owned device identifier.
 * @param {AbortSignal} signal - Cancels confirmation when its view closes.
 * @returns {Promise<Object>} Saved device confirmation.
 */
export async function confirmSecurityEvent(data, deviceId, signal) {
    const response = await fetch(`/admin/rest/security/login-events/${encodeURIComponent(deviceId)}/confirm`, {
        method: 'POST', credentials: 'same-origin', signal,
        headers: { 'X-CSRF-Token': window.csrfToken || '' }
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
