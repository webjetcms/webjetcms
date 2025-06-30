if ("/admin/v9/"===window.location.pathname && window.location.search.includes("act=adminScript")) {
    //domReady.add(function (), 900); - order >= 900 puts it as the last script in the queue
    window.domReady.add(function () {
        console.log("Admin script loaded for path");
        //because of the custom container we need to call WJ.notify("warning") instead of WJ.notifyWarning
        WJ.notify("warning", "Produkčné prostredie", "Nachádzate sa na produkčnom prostredí, prosím, buďte opatrní.", 0, [], false, "toast-container-overview");
    }, 900);
}
