(function () {
    if (typeof window === "undefined" || typeof window.EventSource === "undefined") return;

    var reloadUrl = "http://127.0.0.1:__ADMIN_V9_DEV_RELOAD_PORT__/events";
    var reloadPending = false;
    var eventSource = null;

    function log(message) {
        if (!window.console || typeof window.console.debug !== "function") return;
        window.console.debug("[admin-v9-live-reload] " + message);
    }

    function connect() {
        eventSource = new window.EventSource(reloadUrl);

        eventSource.addEventListener("connected", function () {
            log("connected");
        });

        eventSource.addEventListener("reload", function () {
            if (reloadPending) return;

            reloadPending = true;
            log("reload requested");
            window.location.reload();
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