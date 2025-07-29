package sk.iway.iwcm.system.translation;

import java.nio.charset.StandardCharsets;
import java.util.Hashtable;
import java.util.Map;

import org.apache.http.client.HttpResponseException;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
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
                    .setHeader("Content-Type", "application/json; charset=utf-8")
                    .setHeader("Authorization", "DeepL-Auth-Key "+getAuthKey())
                    .bodyString(getBodyString(text, fromLanguage, toLanguage, deeplModelType), ContentType.APPLICATION_JSON)
                    .execute().returnContent().asString(StandardCharsets.UTF_8);

                JSONObject json = new JSONObject(response);
                JSONArray translations = json.getJSONArray("translations");
                if (translations.length()>0) {
                    translatedText = translations.getJSONObject(0).getString("text");

                    if (translationKey != null) translationsCache.put(translationKey, translatedText);

                    if (Tools.isNotEmpty(translatedText)) {
                        long billedCharacters = translations.getJSONObject(0).has("billed_characters") ? translations.getJSONObject(0).getLong("billed_characters") : 0;
                        auditBilledCharacters(billedCharacters);
                        return translatedText;
                    }
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
                        Logger.error(DeepL.class, "Unable to translate '" + translationApiUrl + "', monthly quota of characters exceeded");
                        return text; //return original text, no translation available
                    } else if(responseException.getStatusCode() == 500) {
                        //internal server error
                        Logger.error(DeepL.class, "Unable to translate '" + translationApiUrl + "', temporal error in DeepL services");
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

    private String getBodyString(String text, String fromLanguage, String toLanguage, String deeplModelType) {
        JSONObject json = new JSONObject();
        json.put("text", new JSONArray().put(text));
        json.put("source_lang", fromLanguage.toUpperCase());
        json.put("target_lang", toLanguage.toUpperCase());
        json.put("tag_handling", "html");
        json.put("model_type", deeplModelType);
        json.put("show_billed_characters", Boolean.TRUE);
        return json.toString();
    }

    @Override
    public Long numberOfFreeCharacters() {
        String CACHE_KEY = "DeepL.freeCharacters";
        Cache cache = Cache.getInstance();
        Long freeCharacters = (Long) cache.getObject(CACHE_KEY);
        if (freeCharacters != null) {
            return freeCharacters; // Return cached value if available
        }

        String usageApiUrl = Constants.getString("deepl_api_usage_url");

        try {
            //if it's a relative URL, prepend the base API URL from deepl_api_url
            if (!usageApiUrl.startsWith("http")) {
                String baseApiUrl = Constants.getString("deepl_api_url");
                baseApiUrl = baseApiUrl.substring(0, baseApiUrl.indexOf('/', 10));
                usageApiUrl = baseApiUrl + usageApiUrl;
            }
        } catch (Exception e) {
            Logger.error(DeepL.class, "Error constructing usage API URL", e);
            return Long.valueOf(-1); // return -1 to indicate an error
        }

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
                    freeCharacters = characterLimit - characterCount;

                    cache.setObjectSeconds(CACHE_KEY, freeCharacters, 60 * 5);
                    auditRemainingCharacters(characterLimit, characterCount);

                    return freeCharacters; // returns number of free characters
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