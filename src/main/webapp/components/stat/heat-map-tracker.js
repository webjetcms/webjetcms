(function () {
    "use strict";

    if (window.webjetHeatMapTracker) return;
    const script = document.currentScript || document.getElementById("wj-heatmap-tracker");
    if (!script) return;
    const docId = Number(script.dataset.docId);
    if (!Number.isInteger(docId) || docId < 1) return;

    const prefix = "wj_hm_";
    const maxEvents = 16;
    const maxBytes = 2048;
    const maxAge = 86400;
    const options = "; Path=/; SameSite=Lax" + (location.protocol === "https:" ? "; Secure" : "");

    function cookies() {
        return document.cookie.split(";").map(function (part) {
            const separator = part.indexOf("=");
            return { name: part.slice(0, separator).trim(), value: part.slice(separator + 1) };
        }).filter(function (cookie) { return cookie.name.length > 0; });
    }

    function valueOf(all, name) {
        const cookie = all.find(function (item) { return item.name === name; });
        if (!cookie) return "";
        try { return decodeURIComponent(cookie.value); } catch (error) { return ""; }
    }

    function hasConsent(all) {
        const declineName = script.dataset.declineName;
        const declineValue = script.dataset.declineValue;
        if (declineName && declineValue && valueOf(all, declineName) === declineValue) return false;
        if (script.dataset.allowAll === "true") return true;
        const categories = valueOf(all, script.dataset.cookieName).split("_").filter(Boolean);
        return categories.includes("statisticke") || (categories.length === 0 && script.dataset.beforeConsent === "true");
    }

    function remove(cookie) {
        document.cookie = cookie.name + "=; Max-Age=0" + options;
    }

    function pending(all) {
        return all.filter(function (cookie) { return cookie.name.startsWith(prefix); });
    }

    function checkConsent() {
        const all = cookies();
        if (hasConsent(all)) return true;
        pending(all).forEach(remove);
        return false;
    }

    function capture(event) {
        const all = cookies();
        if (!hasConsent(all)) {
            pending(all).forEach(remove);
            return;
        }
        // A trusted click covers mouse clicks and taps without double-counting pointer events.
        if (!event.isTrusted || event.button !== 0 || event.detail === 0 || !window.crypto || !crypto.getRandomValues) return;
        const width = window.innerWidth;
        const x = Math.round(event.pageX);
        const y = Math.round(event.pageY);
        if (!Number.isInteger(width) || width < 1 || width > 16384 || !Number.isFinite(x) || !Number.isFinite(y)
                || x < 0 || y < 0 || x > 1000000 || y > 1000000) return;
        const now = Math.floor(Date.now() / 1000);
        const random = crypto.getRandomValues(new Uint8Array(16));
        const id = Array.from(random, function (byte) { return byte.toString(16).padStart(2, "0"); }).join("");
        const next = { name: prefix + id, value: ["v1", docId, width, now, x, y].join(".") };
        const queue = pending(all).filter(function (cookie) {
            const time = Number(cookie.value.split(".")[3]);
            if (!Number.isFinite(time) || time < now - maxAge || time > now + 300) {
                remove(cookie);
                return false;
            }
            return true;
        }).sort(function (a, b) { return Number(a.value.split(".")[3]) - Number(b.value.split(".")[3]); });
        queue.push(next);
        let bytes = queue.reduce(function (total, cookie) { return total + cookie.name.length + cookie.value.length + 2; }, 0);
        while (queue.length > maxEvents || bytes > maxBytes) {
            const oldest = queue.shift();
            bytes -= oldest.name.length + oldest.value.length + 2;
            remove(oldest);
        }
        document.cookie = next.name + "=" + next.value + "; Max-Age=" + maxAge + options;
    }

    window.webjetHeatMapTracker = { checkConsent: checkConsent };
    document.addEventListener("click", capture, true);
    document.addEventListener("webjet:cookie-consent", checkConsent);
    window.addEventListener("pageshow", checkConsent);
    window.addEventListener("focus", checkConsent);
    document.addEventListener("visibilitychange", checkConsent);
    checkConsent();
}());
