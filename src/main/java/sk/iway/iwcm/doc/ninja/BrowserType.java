package sk.iway.iwcm.doc.ninja;

//TODO: MHO nastavit spravne min verziw prehliadacov
//TODO: MHO skontrolovat, ze ci su tu vsetky tipy prehliadacov
public enum BrowserType {
    MSIE("msie", 9),
    CHROME("chrome", 38),
    SAFARI("safari", 6),
    FIREFOX("firefox", 45),
    EDGE("edge", 12),
    ANDROID_BROWSER("android", 4);

    String browser;
    int defaultMinimalVersion;

    BrowserType(String browser, int defaultMinimalVersion) {
        this.browser = browser;
        this.defaultMinimalVersion = defaultMinimalVersion;
    }

    public String getBrowser() {
        return browser;
    }

    public int getDefaultMinimalVersion() {
        return defaultMinimalVersion;
    }
}
