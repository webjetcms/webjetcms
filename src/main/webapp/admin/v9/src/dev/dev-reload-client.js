(function () {
    if (typeof window === "undefined" || typeof window.EventSource === "undefined") return;

    var reloadUrl = "http://127.0.0.1:__ADMIN_V9_DEV_RELOAD_PORT__/events";
    var stylesheetPrefix = "/admin/v9/dist/css/";
    var reloadPending = false;
    var eventSource = null;

    function parsePayload(event) {
        if (!event || typeof event.data !== "string" || event.data.length === 0) return {};

        try {
            return JSON.parse(event.data);
        } catch (error) {
            return {};
        }
    }

    function withReloadVersion(href, version) {
        var url = new window.URL(href, window.location.href);
        url.searchParams.set("wj_reload", String(version));
        return url.toString();
    }

    function refreshStylesheet(link, version) {
        return new Promise(function (resolve) {
            var replacement = link.cloneNode(true);
            var resolvedHref = link.href || link.getAttribute("href");

            replacement.href = withReloadVersion(resolvedHref, version);
            replacement.addEventListener("load", function () {
                link.remove();
                resolve(true);
            }, { once: true });
            replacement.addEventListener("error", function () {
                replacement.remove();
                resolve(false);
            }, { once: true });

            link.parentNode.insertBefore(replacement, link.nextSibling);
        });
    }

    function refreshAdminStyles(version) {
        var links = Array.prototype.slice.call(document.querySelectorAll('link[rel="stylesheet"]')).filter(function (link) {
            var href = link.getAttribute("href") || "";
            return href.indexOf(stylesheetPrefix) !== -1;
        });

        if (links.length === 0) return Promise.resolve(false);

        return Promise.all(links.map(function (link) {
            return refreshStylesheet(link, version);
        })).then(function (results) {
            return results.every(Boolean);
        });
    }

    function fullReload() {
        if (reloadPending) return;

        reloadPending = true;
        log("reload requested");
        window.location.reload();
    }

    function log(message) {
        if (!window.console || typeof window.console.debug !== "function") return;
        window.console.debug("[admin-v9-live-reload] " + message);
    }

    function connect() {
        if (navigator.appVersion.indexOf("Chrome/147") !== -1) { //NOSONAR
            //codecept browser, skip live reload to avoid infinite wait until network idle
            return;
        }

        eventSource = new window.EventSource(reloadUrl);

        eventSource.addEventListener("connected", function () {
            log("connected, navigatorVersion: " + navigator.appVersion);
        });

        eventSource.addEventListener("reload", function (event) {
            var payload = parsePayload(event);

            if (payload.kind === "css") {
                log("css refresh requested");
                refreshAdminStyles(payload.version || Date.now()).then(function (swapped) {
                    if (!swapped) fullReload();
                });
                return;
            }

            fullReload();
        });

        eventSource.onerror = function () {
            log("connection lost, waiting for reconnect");
        };
    }

    window.addEventListener("beforeunload", function () {
        if (eventSource) eventSource.close();
    });

    connect();
})();
