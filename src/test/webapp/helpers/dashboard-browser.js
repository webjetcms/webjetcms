/** Scrolls through the administration's transformed content and waits for the requested card. */
function showWidget(I, id) {
    I.executeScript(id => {
        const scrollbar = window.scrollbarMain;
        scrollbar.setMomentum(0, 0);
        scrollbar.update();
        const top = document.querySelector(`[data-instance-id="${id}"]`).getBoundingClientRect().top;
        if (scrollbar.limit.y > 0) scrollbar.setPosition(0, scrollbar.offset.y + top - 64);
        else window.scrollTo(0, window.scrollY + top - 64);
    }, id);
    return I.waitForFunction(id => {
        const card = document.querySelector(`[data-instance-id="${id}"]`);
        const bounds = card.getBoundingClientRect();
        if (bounds.bottom <= 64 || bounds.top >= window.innerHeight) {
            // Natural-height mobile cards can move the target as preceding widgets finish loading.
            const scrollbar = window.scrollbarMain;
            scrollbar.update();
            if (scrollbar.limit.y > 0) scrollbar.setPosition(0, scrollbar.offset.y + bounds.top - 64);
            else window.scrollTo(0, window.scrollY + bounds.top - 64);
            return false;
        }
        return card.querySelector('.md-dashboard__widget-body').getAttribute('aria-busy') === 'false';
    }, [id], 30);
}

/** Visits pending cards for tests that inspect the complete dashboard, then restores the scroll position. */
async function waitForWidgets(I) {
    await I.waitForElement('.md-dashboard[data-loaded="true"]', 20);
    const { position, pending } = await I.executeScript(() => ({
        position: { smooth: window.scrollbarMain.offset.y, native: window.scrollY },
        pending: [...document.querySelectorAll('.md-dashboard__widget')]
            .filter(card => card.querySelector('.md-dashboard__widget-body').getAttribute('aria-busy') !== 'false')
            .map(card => card.dataset.instanceId)
    }));
    for (const id of pending) await showWidget(I, id);
    await I.executeScript(position => {
        window.scrollbarMain.setMomentum(0, 0);
        window.scrollbarMain.setPosition(0, position.smooth);
        window.scrollTo(0, position.native);
    }, position);
}

module.exports = { showWidget, waitForWidgets };
