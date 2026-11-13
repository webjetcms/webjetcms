package sk.iway.iwcm.system.translation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.structuremirroring.DocMirroringServiceV9;

/**
 * Vseobecna trieda pre preklad textov, vyzaduje konfiguraciu prekladaca, aktualne je podporovany DeepL
 */
@Getter
@Setter
public class TranslationService {

    private String fromLanguage;
    private String toLanguage;
    private TranslationEngine translationEngine;

    public TranslationService(String fromLanguage, String toLanguage) {
        this.fromLanguage = fromLanguage;
        this.toLanguage = toLanguage;
        //Set lot of time when we have crated instance and just call instance without need of getting engine
        this.translationEngine = getEngineInstance();
    }

    /**
     * Translate text from one language to another using the configured translation engine and languages set in constructor.
     * @param text
     * @return
     */
    public String translate(String text) {
        //Use set translation engine by constructor
        return translate(text, fromLanguage, toLanguage, this.translationEngine);
    }

    /**
     * Translate text from one language to another using the configured translation engine.
     * @param text
     * @param fromLanguage
     * @param toLanguage
     * @return
     */
    public String translate(String text, String fromLanguage, String toLanguage) {
        //Use set translation engine by constructor
        return translate(text, fromLanguage, toLanguage, this.translationEngine);
    }

    /**
     * Translates using the configured languages, reporting provider failures to the caller.
     * Copying without a configured engine and same-language operations retain their existing behavior.
     * @param text source text
     * @return translated text, or source text when no translation is configured or required
     * @throws TranslationException when the configured provider fails
     */
    public String translateOrThrow(String text) {
        return translateOrThrow(text, fromLanguage, toLanguage);
    }

    /**
     * Translates using explicit languages, reporting provider failures to the caller.
     * @param text source text
     * @param fromLanguage source language
     * @param toLanguage target language
     * @return translated text, or source text when no translation is configured or required
     * @throws TranslationException when the configured provider fails
     */
    public String translateOrThrow(String text, String fromLanguage, String toLanguage) {
        return translate(text, fromLanguage, toLanguage, this.translationEngine, true);
    }

    private static TranslationEngine getEngineInstance() {
        String[] engineClasses = Constants.getArray("translationEngineClasses");
        if(engineClasses == null || engineClasses.length == 0) return null;

        List<TranslationEngine> configuredWithoutFreeCharacters = new ArrayList<>();
        for(String engineClass : engineClasses) {
            try {
                Class<?> clazz = Class.forName(engineClass);
                if (TranslationEngine.class.isAssignableFrom(clazz)) {
                    TranslationEngine instance = (TranslationEngine) clazz.getDeclaredConstructor().newInstance();
                    if(instance.isConfigured() == true) {
                        Long freeCharacters = instance.numberOfFreeCharacters();
                        if(freeCharacters == null || freeCharacters <= 0) {
                            // Engine is configured, but has no free characters, we will not use it
                            configuredWithoutFreeCharacters.add(instance);
                        } else {
                            // Engine is configured and has free characters, we can use it
                            return instance;
                        }
                    }
                }
            } catch (Exception e) {
                Logger.error(TranslationService.class, "Error while initializing translation engine: " + engineClass, e);
            }
        }

        //There is no configured translation engine with free characters, we will return the first one without free characters (if exists)
        if(configuredWithoutFreeCharacters.size() > 0)
            return configuredWithoutFreeCharacters.get(0);

        return null;
    }

    private static String translate(String text, String fromLanguage, String toLanguage, TranslationEngine translationEngine) {
        return translate(text, fromLanguage, toLanguage, translationEngine, false);
    }

    private static String translate(String text, String fromLanguage, String toLanguage, TranslationEngine translationEngine, boolean failOnError) {
        try {
            //Handle cz / cs language shortcut
            if ("cz".equalsIgnoreCase(toLanguage)) toLanguage = "cs";
            if ("cz".equalsIgnoreCase(fromLanguage)) fromLanguage = "cs";

            if (Tools.isEmpty(text) || text.contains("autotest")) return text;
            if (Tools.isEmpty(fromLanguage) || Tools.isEmpty(toLanguage) || fromLanguage.equalsIgnoreCase(toLanguage)) return text;

            if(translationEngine == null) {
                Logger.warn(TranslationService.class, "No translation engine was found or configured.");
                return text;
            }

            // Find and replace all !INCLUDE()! with __INCLUDE_PLACEHOLDER_x value (x is number)
            String textToTranslate = text;
            Map<Integer, String> replacedIncludes = new HashMap<>();
            try {
                String regex = "(!INCLUDE\\([^)]+\\)!)";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(textToTranslate);

                int findIncludes = 1;
                while (matcher.find()) {
                    String replaceText = "__INCLUDE_PLACEHOLDER_" + findIncludes + "__";
                    replacedIncludes.put(findIncludes, matcher.group());
                    textToTranslate = textToTranslate.replaceFirst(Pattern.quote(matcher.group()), Matcher.quoteReplacement(replaceText));
                    findIncludes++;
                }
            } catch (Exception ex) {
                //Something went wrong, we will not doc data
                Logger.debug(DocMirroringServiceV9.class, "Error while extracting !INLCUDE()! from doc data.", ex);
                replacedIncludes.clear();
                textToTranslate = text;
            }

            String translatedText;
            if (failOnError) translatedText = translationEngine.translateOrThrow(textToTranslate, fromLanguage, toLanguage);
            else translatedText = translationEngine.translate(textToTranslate, fromLanguage, toLanguage);
            if (Tools.isEmpty(translatedText)) throw new TranslationException("Translation engine returned empty text");
            for(Map.Entry<Integer, String> entry : replacedIncludes.entrySet()) {
                String replaceText = "__INCLUDE_PLACEHOLDER_" + entry.getKey() + "__";
                translatedText = translatedText.replace(replaceText, entry.getValue());
            }
            return translatedText;

        } catch (Exception ex) {
            if (failOnError) {
                if (ex instanceof TranslationException translationException) throw translationException;
                throw new TranslationException("Translation failed", ex);
            }
            Logger.error(TranslationService.class, ex);
        }

        return text;
    }

    public static TranslationEngineInfo getTranslationInfo() {
        TranslationEngine translationEngine = getEngineInstance();

        if(translationEngine == null) return null;

        TranslationEngineInfo info = new TranslationEngineInfo(translationEngine.engineName(), translationEngine.numberOfFreeCharacters());
        return info;
    }
}
