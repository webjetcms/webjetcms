package sk.iway.iwcm.system.translation;

import java.nio.charset.StandardCharsets;
import java.util.Hashtable;
import java.util.Map;

import org.apache.http.Consts;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.json.JSONArray;
import org.json.JSONObject;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;

/**
 * Zakladna implementacia prekladania textu cez deepl.com
 * dokumentacia: https://www.deepl.com/docs-api
 *
 * vyzaduje nastavenu konf. premennu deepl_auth_key
 */
public class DeepL {

    private static final String CACHE_KEY = "DeepL.translations";

    private DeepL() {
        //utility class
    }

    public static boolean isConfigured() {
        return Tools.isNotEmpty(getAuthKey());
    }

    private static String getAuthKey() {
        String API_KEY = Constants.getString("deepl_auth_key");
        return API_KEY;
    }

    public static String translate(String text, String fromLanguage, String toLanguage) {

        if ("cz".equalsIgnoreCase(toLanguage)) toLanguage = "cs";
        if ("cz".equalsIgnoreCase(fromLanguage)) fromLanguage = "cs";

        Cache cache = Cache.getInstance();
        @SuppressWarnings("unchecked")
        Map<String, String> translationsCache = (Map<String, String>)cache.getObject(CACHE_KEY);
        if (translationsCache==null) {
            translationsCache = new Hashtable<>();
            cache.setObject(CACHE_KEY, translationsCache, 10);
        }

        String translationKey = null;
        String translatedText;
        if (text.length()<500) {
            //do not cache long HTML texts
            translationKey = text + "|" + fromLanguage + "|" + toLanguage;
            translatedText = translationsCache.get(translationKey);
            if (translatedText!=null) return translatedText;
        }

        String apiUrl = Constants.getString("deepl_api_url");

        try{
            //DeepL has a problem with nbsp entity
            text = Tools.replace(text, "&nbsp;", " ");

            String response = Request.Post(apiUrl)
                .bodyForm(Form.form()
                    .add("text", text)
                    .add("source_lang", fromLanguage.toUpperCase())
                    .add("target_lang", toLanguage.toUpperCase())
                    .add("tag_handling", "html")
                    .build(), Consts.UTF_8)
                .setHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
                .setHeader("Authorization", "DeepL-Auth-Key "+getAuthKey())
                .execute().returnContent().asString(StandardCharsets.UTF_8);

            JSONObject json = new JSONObject(response);
            JSONArray translations = json.getJSONArray("translations");
            if (translations.length()>0) {
                translatedText = translations.getJSONObject(0).getString("text");

                if (translationKey != null) translationsCache.put(translationKey, translatedText);

                if (Tools.isNotEmpty(translatedText)) return translatedText;
            }
		} catch (Exception e){
            Logger.error(DeepL.class,"Unable to connect to '" + apiUrl + "'");
            Logger.error(DeepL.class, e);
        }

        return text;
    }

}
