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
 * <p>Application for redirecting the home page to a subdirectory based on browser language.</p>
 * <p>It detects the language from the HTTP {@code Accept-Language} header and redirects the user to the corresponding language folder.</p>
 * <p>Include example:</p>
 * <code>!INCLUDE(sk.iway.basecms.langredirect.LanguageRedirectApp)!</code>
 *
 * The @WebjetAppStore annotation ensures the application is displayed in the editor application list (AppStore)
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
     * Comma-separated list of supported languages.
     * Default: sk,cs,en
     */
    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "basic", title = "apps.langredirect.supportedLanguages")
    private String supportedLanguages = "sk,cs,en";

    /**
        * Default language if browser language detection fails.
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
     * If true, redirect only on the root URL (for example, / or /index.html).
     * If false, always redirect when entering the page.
     */
    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, tab = "basic", title = "apps.langredirect.rootOnly")
    private boolean rootOnly = true;

    /**
     * Website folder where the language subdirectory should be searched.
     * For example, if set to "/", it searches for /sk/, /cs/, /en/.
     */
    @DataTableColumn(inputType = DataTableColumnType.TEXT, tab = "advanced", title = "apps.langredirect.basePath")
    private String basePath = "/";

    /**
     * If true, redirection is performed only when the user does not have a language cookie set.
     * This allows users to change language manually without being redirected.
     */
    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, tab = "advanced", title = "apps.langredirect.respectCookie")
    private boolean respectCookie = true;

    /**
     * Dynamically gets language selection options from design template configuration.
     * Uses LayoutService.getLanguages() the same way as template editing.
     *
     * @param componentRequest Component request context
     * @param request HTTP request
     * @return Map with options for the defaultLanguage field
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
     * The init method is called after object creation and parameter setup.
     * Redirection is performed in the @DefaultHandler view() method.
     *
     * @param request
     * @param response
     */
    @Override
    public void init(HttpServletRequest request, HttpServletResponse response) {
        // Redirect is handled in the @DefaultHandler view() method
    }

    /**
     * Detects language from the HTTP Accept-Language header.
     *
     * @param request HTTP request
     * @return detected language (sk, cs, en, etc.)
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
     * Checks whether the language is in the list of supported languages.
     *
     * @param lang detected language
     * @return true if the language is supported
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
     * Default handler - redirects to the page language variant based on detected language.
     * If the lng cookie exists, its value is used. Otherwise, language is detected from the Accept-Language header.
     *
     * @param request HTTP request
     * @return redirect URL to the language variant (for example, /sk/, /en/)
     */
    @DefaultHandler
    public String view(HttpServletRequest request) {
        // Check whether the user has a language cookie set
        if (respectCookie) {
            String langCookie = Tools.getCookieValue(request.getCookies(), "lng", null);
            if (Tools.isNotEmpty(langCookie)) {
                // Verify whether the language is in the list of supported languages
                if (isSupportedLanguage(langCookie)) {
                    String redirectPath = basePath + langCookie + PATH_SEPARATOR;
                    Logger.debug(LanguageRedirectApp.class, "Redirecting to language from cookie: " + redirectPath);
                    return "redirect:" + redirectPath;
                }
            }
        }

        // Detect language from the Accept-Language header
        String detectedLang = detectLanguage(request);

        // If rootOnly=true, redirect only on the root URL
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
