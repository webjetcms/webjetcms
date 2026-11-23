/** Resolves an optional login hint without delaying authentication or exposing CMS credentials. */
(() => {
    const script = document.currentScript;
    const lookupUrl = script?.dataset.lookupUrl;
    if (!lookupUrl) return;

    window.webjetLoginLocationRequest = (async () => {
        const controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), 3000);
        try {
            const response = await fetch(lookupUrl, {
                signal: controller.signal,
                credentials: 'omit',
                referrerPolicy: 'no-referrer',
                cache: 'no-store'
            });
            if (!response.ok) return null;
            const result = await response.json();
            if (result.success !== true || !result.city || !result.country_code) return null;
            clearTimeout(timeout);
            const saved = await fetch(script.dataset.saveUrl, {
                method: 'POST',
                credentials: 'same-origin',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': script.dataset.csrf },
                body: JSON.stringify({ city: result.city, countryCode: result.country_code })
            });
            if (!saved.ok) return null;
            return saved.status === 204 ? null : (await saved.json()).location;
        } catch {
            // Geolocation is supplementary; failures must not interrupt login or show an error dialog.
            return null;
        } finally {
            clearTimeout(timeout);
        }
    })();
})();
