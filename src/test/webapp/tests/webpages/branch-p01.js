Feature('webpages.branch-p01');

Before(({ login }) => {
    login('admin');
});

Scenario('P01 local admin and real aceintegration menu baseline', async ({ I }) => {
    I.amOnPage('/apps/menu/');
    I.waitForElement('#menutest', 20);
    I.see('Anketa', '#menutest');
    I.seeElementInDOM('link[href*="/templates/aceintegration/jet/"]');
    I.seeElement('img[src*="logo-jet.png"]');
    I.saveScreenshot('branch-p01-live-menu.png');

    await I.sendGetRequest('/templates/aceintegration/jet/assets/images/logo-jet.png');
    I.seeResponseCodeIs(200);
    await I.sendGetRequest('/templates/aceintegration/jet/assets/css/ninja.min.css');
    I.seeResponseCodeIs(200);
    await I.sendGetRequest('/components/_common/javascript/jquery.min.js');
    I.seeResponseCodeIs(200);
    await I.sendGetRequest('/thumb/templates/aceintegration/jet/assets/images/logo-jet.png?w=120&h=50&ip=5');
    I.seeResponseCodeIs(200);
});

if (process.env.WEBJET_P01_ADMIN_HOST === 'true') {
    Scenario('P01 WORK gateway keeps the real admin host and session', async ({ I }) => {
        const route = '/admin/branch-p01-autotest.jsp?target=';
        I.amOnPage('/admin/');
        // A real admin link preserves the existing same-origin Referer/CSRF policy for JSP entry points.
        I.executeScript(url => {
            const link = document.createElement('a');
            link.id = 'p01-preview-start';
            link.href = url;
            link.textContent = 'P01 preview';
            link.style.cssText = 'position:fixed;top:100px;left:20px;z-index:999999;background:white;padding:12px';
            document.body.append(link);
        }, route + encodeURIComponent('/__branch-preview/42/home.html'));
        I.forceClick('#p01-preview-start');
        I.waitForElement('#p01-menu', 20);
        I.see('P01 WORK 42 body');
        I.see('P01 NEW section', '#p01-menu');
        I.click('P01 NEW section', '#p01-menu');
        I.waitForText('P01 NEW default body 42', 20);
        I.seeInCurrentUrl('/admin/branch-p01-autotest.jsp?target=');
        // Visibility alone does not prove that authenticated image/thumbnail requests decoded.
        I.waitForFunction(() => {
            const image = document.querySelector('#original');
            const thumb = document.querySelector('#thumbnail');
            return image?.complete && image.naturalWidth === 64 && thumb?.complete && thumb.naturalWidth === 16;
        }, 20);
        const origin = await I.executeScript(() => location.origin);
        I.assertEqual(origin, 'http://iwcm.interway.sk:8086');
        const resources = await I.executeScript(() => performance.getEntriesByType('resource')
            .map(entry => entry.name).filter(name => name.includes('branch-p01-autotest.jsp')));
        I.assertTrue(resources.some(name => name.includes('probe.css')));
        I.assertTrue(resources.some(name => name.includes('probe.js')));
        I.assertTrue(resources.every(name => name.startsWith(origin + route)));
        I.saveScreenshot('branch-p01-admin-work.png');
    });
}
