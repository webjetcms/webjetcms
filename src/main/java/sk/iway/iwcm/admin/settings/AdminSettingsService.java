package sk.iway.iwcm.admin.settings;

import java.util.HashMap;
import java.util.Map;

import sk.iway.iwcm.JsonTools;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.users.SettingsAdminBean;
import sk.iway.iwcm.users.SettingsAdminDB;
import sk.iway.iwcm.users.UserDetails;

/**
 * Podporna trieda pre ziskanie udajov ulozenych v admin_settings tabulke ako (JSON) hodnoty
 */
public class AdminSettingsService {

    private UserDetails user;
    private Map<String, SettingsAdminBean> settings;

    public AdminSettingsService() {
        settings = new HashMap<>();
        user = null;
    }

    public AdminSettingsService(UserDetails user) {
        setUser(user);
    }

    public void setUser(UserDetails user) {
        this.user = user;
        settings = user.getAdminSettings();
    }

    /**
     * Vrati String hodnotu nastaveni pouzivatela so zadanym klucom
     * @param key
     * @return
     */
    public String getValue(String key) {
        SettingsAdminBean s = settings.get(key);
        if (s == null) return null;

        return s.getValue();
    }

    /**
     * Vrati hodnotu z JSON objektu v zadanom kluci a zadanej JSON ceste (napr. kluc.druhy.treti)
     * @param key
     * @param jsonPath
     * @return
     */
    public String getJsonValue(String key, String jsonPath) {

        String json = getValue(key);
        if (json == null) return null;

        return JsonTools.getValue(json, jsonPath);
    }

    /**
     * Vrati boolean hodnotu z JSON objektu v zadanom kluci a zadanej JSON ceste (napr. kluc.druhy.treti)
     * @param key
     * @param jsonPath
     * @return
     */
    public boolean getJsonBooleanValue(String key, String jsonPath) {
        String value = getJsonValue(key, jsonPath);
        return "true".equals(value);
    }

    /**
     * Vrati cislo (alebo -1 ak sa nejedna o cislo) z JSON objektu v zadanom kluci
     * a zadanej JSON ceste (napr. kluc.druhy.treti)
     * @param key
     * @param jsonPath
     * @return
     */
    public int getJsonIntValue(String key, String jsonPath) {
        String value = getJsonValue(key, jsonPath);
        return Tools.getIntValue(value, -1);
    }

    /**
     * Ulozi nastavenia pouzivatela so zadanym klucom a hodnotou
     * @param key
     * @param value
     * @return
     */
    public boolean saveSettings(String key, String value) {
        SettingsAdminBean bean = new SettingsAdminBean(user.getUserId(), key, value);
        boolean saveok = SettingsAdminDB.setSetting(bean);
        //aby sa pri requeste reloadli z DB
        user.setAdminSettings(null);

        return saveok;
    }
}
