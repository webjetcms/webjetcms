Feature('components.combine');

const galleryCssFiles = [
    '/components/gallery/prettyphoto/css/prettyPhoto.css',
    '/components/gallery/photoswipe/default-skin/default-skin.css',
    '/components/gallery/ajax/galleriffic/galleriffic.css',
    '/components/gallery/ajax/prettyGallery/prettyGallery.css',
    '/components/gallery/ajax/photoSwipe.css'
];

const expectedAssetUrls = [
    '/components/gallery/prettyphoto/images/prettyPhoto/default/sprite.png',
    '/components/gallery/photoswipe/default-skin/default-skin.png',
    '/components/gallery/photoswipe/default-skin/default-skin.svg',
    '/components/gallery/photoswipe/default-skin/preloader.gif',
    '/components/gallery/ajax/galleriffic/loader.gif',
    '/components/gallery/ajax/prettyGallery/previous.gif',
    '/components/gallery/ajax/prettyGallery/next.gif',
    '/components/gallery/ajax/photoSwipe/error.gif'
];

const representativeAssetUrls = [
    '/components/gallery/prettyphoto/images/prettyPhoto/default/sprite.png',
    '/components/gallery/photoswipe/default-skin/default-skin.svg',
    '/components/gallery/ajax/galleriffic/loader.gif',
    '/components/gallery/ajax/prettyGallery/previous.gif',
    '/components/gallery/ajax/photoSwipe/error.gif'
];

Scenario('gallery CSS uses absolute asset URLs when combined', async ({ I }) => {
    const combineUrl = '/components/_common/combine.jsp?t=css&f=' + galleryCssFiles.join(',') + '&v=' + Date.now();
    const headers = { 'x-auth-token': '' };
    const response = await I.sendGetRequest(combineUrl, headers);

    I.assertEqual(response.status, 200, 'Combined gallery CSS must be available');
    I.assertEqual(typeof response.data, 'string', 'Combined gallery CSS response must contain text');

    const css = typeof response.data === 'string' ? response.data : '';
    const relativeUrls = css.match(
        /url\(\s*(['"]?)(?!\/|[a-z][a-z0-9+.-]*:|#)([^'")]+)\1\s*\)/gi
    ) || [];
    I.assertEqual(
        relativeUrls.length,
        0,
        'Combined gallery CSS contains relative URLs: ' + relativeUrls.join(', ')
    );

    for (const assetUrl of expectedAssetUrls) {
        I.assertContain(css, assetUrl, 'Combined CSS must contain ' + assetUrl);
    }

    for (const assetUrl of representativeAssetUrls) {
        const assetResponse = await I.sendGetRequest(assetUrl, headers);
        I.assertEqual(assetResponse.status, 200, 'Referenced asset must be available: ' + assetUrl);
    }
});
