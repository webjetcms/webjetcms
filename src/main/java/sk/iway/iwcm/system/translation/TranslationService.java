package sk.iway.iwcm.system.translation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;

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

            if (DeepL.isConfigured()) return DeepL.translate(text, fromLanguage, toLanguage);

        } catch (Exception ex) {
            Logger.error(TranslationService.class, ex);
        }

        return text;
    }

}
