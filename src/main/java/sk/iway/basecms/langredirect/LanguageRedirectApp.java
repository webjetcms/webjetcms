package sk.iway.basecms.langredirect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.layout.LayoutService;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.editor.rest.ComponentRequest;
import sk.iway.iwcm.LabelValueDetails;
import sk.iway.iwcm.system.annotations.DefaultHandler;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.OptionDto;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableTab;
import sk.iway.iwcm.system.datatable.annotations.DataTableTabs;

/**
 * <p>Aplikácia pre presmerovanie hlavnej stránky na podadresár podľa jazyka prehliadača.</p>
 * <p>Detekuje jazyk z HTTP hlavičky {@code Accept-Language} a presmeruje používateľa na príslušný jazykový priečinok.</p>
 * <p>Príklad include:</p>
 * <code>!INCLUDE(sk.iway.basecms.langredirect.LanguageRedirectApp)!</code>
 *
 * Anotácia @WebjetAppStore zabezpečí zobrazenie aplikácie v zozname aplikácií v editore (v AppStore)
 */
@WebjetComponent("sk.iway.basecms.langredirect.LanguageRedirectApp")
@WebjetAppStore(
    nameKey = "apps.langredirect.title",
    descKey = "apps.langredirect.desc",
    imagePath = "ti ti-language",
    galleryImages = "/components/langredirect/screenshot-1.jpg",
    commonSettings = false
)
@DataTableTabs(tabs = {
     @DataTableTab(id = "basic", title = "components.universalComponentDialog.title", selected = true),
     @DataTableTab(id = "advanced", title = "editor.tab.advanced")
})
@Getter
@Setter
public class LanguageRedirectApp extends WebjetComponentAbstract {

    private static final String PATH_SEPARATOR = "/";

    /**
     * Zoznam podporovaných jazykov oddelených čiarkou.
     * Predvolené: sk,cs,en
     */
    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "basic", title = "apps.langredirect.supportedLanguages")
    private String supportedLanguages = "sk,cs,en";

    /**
     * Predvolený jazyk ak sa nepodarí detekovať jazyk prehliadača.
     */
    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "basic", title = "apps.langredirect.defaultLanguage", editor = {
         @DataTableColumnEditor(
            attr = {
                 @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "apps.langredirect.availableLanguages")
             },
            options = {
                 @DataTableColumnEditorAttr(key = "Slovenčina (sk)", value = "sk"),
                 @DataTableColumnEditorAttr(key = "Čeština (cs)", value = "cs"),
                 @DataTableColumnEditorAttr(key = "English (en)", value = "en"),
                 @DataTableColumnEditorAttr(key = "Polski (pl)", value = "pl"),
                 @DataTableColumnEditorAttr(key = "Deutsch (de)", value = "de"),
                 @DataTableColumnEditorAttr(key = "Magyar (hu)", value = "hu")
             }
         )
    })
    private String defaultLanguage = "sk";

    /**
     * Ak je true, presmeruje sa len na koreňovej URL adrese (napr. / alebo /index.html).
     * Ak je false, presmeruje sa vždy pri vstupe do stránky.
     */
    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, tab = "basic", title = "apps.langredirect.rootOnly")
    private boolean rootOnly = true;

    /**
     * Priečinok na webe kde sa má hľadať jazykový podadresár.
     * Napr. ak je nastavené na "/", hľadá sa /sk/, /cs/, /en/.
     */
    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "advanced", title = "apps.langredirect.basePath")
    private String basePath = "/";

    /**
     * Ak je true, presmerovanie sa vykoná len ak používateľ nemá nastavený cookie s jazykom.
     * Umožňuje používateľovi zmeniť jazyk manuálne bez presmerovania.
     */
    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, tab = "advanced", title = "apps.langredirect.respectCookie")
    private boolean respectCookie = true;

      /**
       * Dynamicky získava možnosti pre výber jazyka z konfigurácie dizajnových šablón.
       * Používa LayoutService.getLanguages() rovnako ako v editácii šablón.
       *
       * @param componentRequest Component request kontext
       * @param request HTTP request
       * @return Mapa s možnosťami pre pole defaultLanguage
       */
      @Override
    public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
        Map<String, List<OptionDto>> options = new HashMap<>();

        LayoutService ls = new LayoutService(request);
        List<LabelValueDetails> languages = ls.getLanguages(false, true);

        List<OptionDto> langOptions = new ArrayList<>();
        for (LabelValueDetails lvd : languages) {
            langOptions.add(new OptionDto(lvd.getLabel(), lvd.getValue(), null));
         }
        options.put("defaultLanguage", langOptions);

        return options;
     }

    /**
     * Metóda init sa volá po vytvorení objektu a nastavení parametrov.
     * Presmerovanie sa vykonáva v @DefaultHandler metóde view().
     *
     * @param request
     * @param response
     */
    @Override
    public void init(HttpServletRequest request, HttpServletResponse response) {
        // Redirect is handled in the @DefaultHandler view() method
    }

    /**
     * Detekuje jazyk z HTTP hlavičky Accept-Language.
     *
     * @param request HTTP request
     * @return detekovaný jazyk (sk, cs, en, atď.)
     */
    private String detectLanguage(HttpServletRequest request) {
        String acceptLanguage = request.getHeader("Accept-Language");

        if (Tools.isEmpty(acceptLanguage)) {
            Logger.debug(LanguageRedirectApp.class, "Accept-Language header is empty");
            return defaultLanguage;
        }

        try {
            // Parse the first (highest priority) language
            String[] languages = acceptLanguage.split(",");
            if (languages.length > 0) {
                String lang = languages[0].trim();

                // Remove quality factor (e.g., en-US;q=0.7 -> en-US)
                if (lang.indexOf(';') != -1)
                    lang = lang.split(";")[0].trim();

                // Extract language code from locale (e.g., en-US -> en, sk_SK -> sk)
                if (lang.indexOf('-') != -1)
                    lang = lang.split("-")[0];
                if (lang.indexOf('_') != -1)
                    lang = lang.split("_")[0];

                return lang.toLowerCase();
            }
        } catch (Exception e) {
            Logger.error(LanguageRedirectApp.class, "Error detecting language: " + e.getMessage(), e);
        }

        return defaultLanguage;
    }

    /**
     * Skontroluje či je jazyk v zozname podporovaných jazykov.
     *
     * @param lang detekovaný jazyk
     * @return true ak je jazyk podporovaný
     */
    private boolean isSupportedLanguage(String lang) {
        if (Tools.isEmpty(lang))
            return false;

        String[] supported = supportedLanguages.toLowerCase().split(",");
        for (String s : supported) {
            if (s.trim().equals(lang))
                return true;
        }
        return false;
    }

    /**
     * Default handler - vykoná presmerovanie na jazykovú mutáciu stránky podľa detekovaného jazyka.
     * Ak existuje cookie lng, použije sa jej hodnota. Inak sa detekuje jazyk z Accept-Language hlavičky.
     *
     * @param request HTTP request
     * @return redirect URL na jazykovú mutáciu (napr. /sk/, /en/)
     */
    @DefaultHandler
    public String view(HttpServletRequest request) {
        // Skontroluj, či používateľ nemá nastavený jazykový cookie
        if (respectCookie) {
            String langCookie = Tools.getCookieValue(request.getCookies(), "lng", null);
            if (Tools.isNotEmpty(langCookie)) {
                // Over, či je jazyk v zozname podporovaných jazykov
                if (isSupportedLanguage(langCookie)) {
                    String redirectPath = basePath + langCookie + PATH_SEPARATOR;
                    Logger.debug(LanguageRedirectApp.class, "Redirecting to language from cookie: " + redirectPath);
                    return "redirect:" + redirectPath;
                }
            }
        }

        // Detekuj jazyk z Accept-Language hlavičky
        String detectedLang = detectLanguage(request);

        // Ak rootOnly=true, presmeruj len na koreňovej URL adrese
        String path = request.getRequestURI().toLowerCase();
        String contextPath = request.getContextPath().toLowerCase();
        path = path.replace(contextPath, "");

        if (rootOnly && !"/".equals(path) && !"/index.html".equals(path) && !"".equals(path)) {
            Logger.debug(LanguageRedirectApp.class, "Not root path, skipping redirect: " + path);
            return "";
        }

        String redirectPath = basePath + detectedLang + PATH_SEPARATOR;
        Logger.debug(LanguageRedirectApp.class, "Redirecting to: " + redirectPath + " (detected lang: " + detectedLang + ")");
        return "redirect:" + redirectPath;
    }
}
