package sk.iway.iwcm.admin.settings;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.JsonTools;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.users.SettingsAdminBean;
import sk.iway.iwcm.users.SettingsAdminDB;
import sk.iway.iwcm.users.UserDetails;

/**
 * Podporna trieda pre ziskanie udajov ulozenych v admin_settings tabulke ako (JSON) hodnoty
 */
public class AdminSettingsService {

    public static final String WEBPAGES_TREE_SETTINGS_KEY = "jstreeSettings_web-pages-list";

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
     * Returns a saved preference, or the configured web page tree defaults when no preference exists.
     * Defaults are read without changing the user's settings or persisting a database record.
     *
     * @param key preference key
     * @return saved or default value, or {@code null} for an unknown key
     */
    public String getValue(String key) {
        SettingsAdminBean s = settings.get(key);
        if (s != null) return s.getValue();

        if (WEBPAGES_TREE_SETTINGS_KEY.equals(key)) {
            try {
                return new JSONObject(Constants.getString("webpagesTreeDefaultSettings")).toString();
            } catch (JSONException e) {
                Logger.error(AdminSettingsService.class, "Invalid JSON object in webpagesTreeDefaultSettings", e);
                return "{}";
            }
        }
        return null;
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
