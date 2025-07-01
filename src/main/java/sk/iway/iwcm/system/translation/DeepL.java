package sk.iway.iwcm.system.translation;

import java.nio.charset.StandardCharsets;
import java.util.Hashtable;
import java.util.Map;

import org.apache.http.Consts;
import org.apache.http.client.HttpResponseException;
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
public class DeepL extends TranslationEngine {

    private static final String CACHE_KEY = "DeepL.translations";

    public DeepL() {
        // Constructor for DeepL translation engine
    }

    @Override
    public boolean isConfigured() {
        return Tools.isNotEmpty(getAuthKey());
    }

    private static String getAuthKey() {
        String API_KEY = Constants.getString("deepl_auth_key");
        return API_KEY;
    }

    @Override
    public String translate(String text, String fromLanguage, String toLanguage) {

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

        String translationApiUrl = Constants.getString("deepl_api_url");
        String deeplModelType = Constants.getString("deepl_model_type");

        //DeepL has a problem with nbsp entity
        text = Tools.replace(text, "&nbsp;", " ");

        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                String response = Request.Post(translationApiUrl)
                    .bodyForm(Form.form()
                        .add("text", text)
                        .add("source_lang", fromLanguage.toUpperCase())
                        .add("target_lang", toLanguage.toUpperCase())
                        .add("tag_handling", "html")
                        .add("model_type", deeplModelType)
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

                //Succes, break the while
                break;
            } catch (Exception ex) {
                if(ex instanceof HttpResponseException responseException) {
                    if(responseException.getStatusCode() == 429) {
                        //too many requests, apply sleep delay
                        if(applyDelay(attempt)) continue;
                        else break;
                    } else if(responseException.getStatusCode() == 456) {
                        //quota exceeded
                        Logger.error(DeepL.class, "Unable to translete '" + translationApiUrl + "', monthly quota of characters exceeded");
                        return text; //return original text, no translation available
                    } else if(responseException.getStatusCode() == 500) {
                        //internal server error
                        Logger.error(DeepL.class, "Unable to translete '" + translationApiUrl + "', temporal error in DeepL services");
                        return text; //return original text, no translation available
                    }
                }

                Logger.error(DeepL.class,"Unable to connect to '" + translationApiUrl + "'");
                Logger.error(DeepL.class, ex);
                break;
            }
        }

        return text;
    }

    @Override
    public Long numberOfFreeCharacters() {
        String usageApiUrl = Constants.getString("deepl_api_usage_url");

        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                String response = Request.Post(usageApiUrl)
                        .setHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
                        .setHeader("Authorization", "DeepL-Auth-Key "+getAuthKey())
                        .execute().returnContent().asString(StandardCharsets.UTF_8);

                JSONObject json = new JSONObject(response);
                Long characterLimit = json.getLong("character_limit");
                Long characterCount = json.getLong("character_count");
                if(characterLimit != null && characterCount != null) {
                    return characterLimit - characterCount; // returns number of free characters
                } else {
                    Logger.error(DeepL.class, "Invalid response from DeepL API usage endpoint: " + response);
                return Long.valueOf(-1);
                }

            } catch (Exception ex) {
                if(ex instanceof HttpResponseException responseException) {
                    if(responseException.getStatusCode() == 429) {
                        //too many requests, apply sleep delay
                        if(applyDelay(attempt)) continue;
                        else break;
                    }
                }

                Logger.error(DeepL.class, "Unable to get number of litmit/used characters from DeepL", ex);
                return Long.valueOf(-1); // return -1 to indicate an error
            }
        }

        return Long.valueOf(-1);
    }

    @Override
    public String engineName() {
        return "DeepL";
    }
}