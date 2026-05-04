package sk.iway.iwcm.doc.ninja;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.stat.BrowserDetector;

import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

public class UserAgent {
    private Ninja ninja;
    private Map<String, Integer> minimalBrowserVersion = new Hashtable<>();
    BrowserDetector bd = null;

    public UserAgent(Ninja ninja) {
        this.ninja = ninja;

        for (BrowserType browserType : BrowserType.values()) {
            minimalBrowserVersion.put(browserType.getBrowser(), Tools.getIntValue(ninja.getConfig("minBrowserVersion."+browserType), browserType.defaultMinimalVersion));
        }
        Properties config = ninja.getConfig();
        if (config != null) {
            for (Object key : config.keySet()) {
                String keyStr = (String) key;
                if (keyStr.startsWith("minBrowserVersion.") && keyStr.length() > 20) {
                    String browser = keyStr.substring(18);
                    if (minimalBrowserVersion.containsKey(browser)==false) {
                        minimalBrowserVersion.put(browser, Tools.getIntValue(ninja.getConfig(keyStr), 0));
                    }
                }
            }
        }
    }

    /**
     * Funkcia na vratenie verzie prehliadaca. Verzia sa zistuje na zaklade UserAdent
     * @return String - verzia prehliadaca
     */
    public String getBrowserVersion(){
        String browserVersion = getBrowserDetector().getBrowserVersionShort();
        return browserVersion;
    }

    /**
     * Funkcia ktora vrati nazov prehliadaca
     * @return String - nazov prehliadaca:
     * ie
     * chrome
     * safari
     * firefox
     * opera
     * edge
     * webview
     * android browser
     * maxthon
     * blackberry
     */
    public String getBrowserName(){
        String browserName = getBrowserDetector().getBrowserName();
        if (browserName != null) {
            return browserName.toLowerCase();
        }
        return "";
    }

    /**
     * Metoda vracajuca typ zariadenia
     * @return String - ketegoria zariadenia:
     * game console
     * other
     * pda
     * personal computer
     * smart tv
     * smartphone
     * tablet
     * wearable computer
     * prazdny string ak nepozna typ
     */
    public String getDeviceType(){
        String deviceType = getBrowserDetector().getBrowserDeviceType();
        if (deviceType == null) deviceType = "desktop";
        return deviceType.toLowerCase();
    }

    public String getDeviceOS(){
        String deviceOs = getBrowserDetector().getBrowserPlatform();
        if (Tools.isNotEmpty(getBrowserDetector().getBrowserSubplatform())) deviceOs += " "+getBrowserDetector().getBrowserSubplatform();
        return deviceOs.toLowerCase();
    }

    public boolean isBrowserOutdated()
    {
        int browserVersion = getMinimalBrowserVersion(getBrowserName());
        //pre nezdetekovany browser vratime false, zhodni sme sa na tom s MHO ze to je lepsie
        if (browserVersion < 1) return false;
        return Tools.getIntValue(getBrowserVersion(), 999) < browserVersion;
    }

    public int getMinimalBrowserVersion(String browser){
        Integer version =  minimalBrowserVersion.get(browser);
        if (version == null || version <1) return -1;
        return version;
    }

    public boolean isBlind(){
        BrowserDetector instance = BrowserDetector.getInstance(ninja.getRequest());
        boolean isBlind = false;
        if("blind".equals(instance.getBrowserDeviceType())){
            isBlind = true;
        }
        return isBlind;
    }

    private BrowserDetector getBrowserDetector()
    {
        if (bd == null) bd = BrowserDetector.getInstance(ninja.getRequest());
        return bd;
    }
}
