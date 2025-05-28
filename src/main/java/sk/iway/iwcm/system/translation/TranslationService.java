package sk.iway.iwcm.system.translation;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.structuremirroring.DocMirroringServiceV9;

/**
 * Vseobecna trieda pre preklad textov, vyzaduje konfiguraciu prekladaca, aktualne je podporovany DeepL
 */
@Getter
@Setter
@AllArgsConstructor
public class TranslationService {

    private String fromLanguage;
    private String toLanguage;

    public String translate(String text) {
        return translate(text, fromLanguage, toLanguage);
    }

    /**
     * Prelozi zadany text (slovo/veta/HTML kod) zo zdrojoveho do cieloveho jazyka (2 pismenovy kod)
     * @param text
     * @param fromLanguage
     * @param toLanguage
     * @return
     */
    public static String translate(String text, String fromLanguage, String toLanguage) {

        try {

            if (Tools.isEmpty(text) || text.contains("autotest")) return text;
            if (Tools.isEmpty(fromLanguage) || Tools.isEmpty(toLanguage) || fromLanguage.equalsIgnoreCase(toLanguage)) return text;

            if (DeepL.isConfigured()) {

                // FInd and replace all !INCLUDE()! with __INCLUDE_PLACEHOLDER_x value (x is number)
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
                }

                if(replacedIncludes.isEmpty() == true) {
                    //Translate without include replenish, because there is nothing to replace
                    return DeepL.translate(text, fromLanguage, toLanguage);
                } else {
                    try {
                        String translatedText = DeepL.translate(textToTranslate, fromLanguage, toLanguage);
                        for(Map.Entry<Integer, String> entry : replacedIncludes.entrySet()) {
                            String replaceText = "__INCLUDE_PLACEHOLDER_" + entry.getKey() + "__";
                            translatedText = translatedText.replace(replaceText, Matcher.quoteReplacement(entry.getValue()));
                        }
                        return translatedText;

                    } catch (Exception ex) {
                        //Something went wrong, we will not doc data
                        Logger.debug(DocMirroringServiceV9.class, "Error while returning !INLCUDE()! in doc data after translate. Use doc data translation without replacing.", ex);
                        // Translate default text
                        return DeepL.translate(text, fromLanguage, toLanguage);
                    }
                }
            }

        } catch (Exception ex) {
            Logger.error(TranslationService.class, ex);
        }

        return text;
    }
}
