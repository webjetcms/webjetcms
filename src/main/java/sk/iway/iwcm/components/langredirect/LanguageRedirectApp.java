package sk.iway.iwcm.components.langredirect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PathFilter;
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
 * <code>!INCLUDE(sk.iway.iwcm.components.langredirect.LanguageRedirectApp)!</code>
 *
 * The @WebjetAppStore annotation ensures the application is displayed in the editor application list (AppStore)
 */
@WebjetComponent("sk.iway.iwcm.components.langredirect.LanguageRedirectApp")
@WebjetAppStore(
    nameKey = "apps.app-language-redirect.title",
    descKey = "apps.app-language-redirect.desc",
    imagePath = "ti ti-language",
    galleryImages = "/apps/app-language-redirect/screenshot-1.jpg,/apps/app-language-redirect/screenshot-2.jpg,/apps/app-language-redirect/screenshot-3.jpg",
    commonSettings = false
)
@DataTableTabs(tabs = {
     @DataTableTab(id = "basic", title = "components.universalComponentDialog.title", selected = true),
     @DataTableTab(id = "advanced", title = "editor.tab.advanced")
})
@Getter
@Setter
public class LanguageRedirectApp extends WebjetComponentAbstract {

    /**
     * Default language if browser language detection fails or no mapping is found.
     */
    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "basic", title = "apps.app-language-redirect.defaultLanguage", editor = {
         @DataTableColumnEditor(
            attr = {
                 @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after")
             }
         )
    })
    private String defaultLanguage = "sk";

    /**
     * If true, redirect only on the root URL (for example, / or /index.html).
     * If false, always redirect when entering the page.
     */
    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, tab = "advanced", title = "apps.app-language-redirect.rootOnly")
    private boolean rootOnly = false;

    /**
     * If true, redirection is performed only when the user does not have a language cookie set.
     * This allows users to change language manually without being redirected.
     */
    @DataTableColumn(inputType = DataTableColumnType.CHECKBOX, tab = "advanced", title = "apps.app-language-redirect.respectCookie")
    private boolean respectCookie = true;

    /**
     * Language-to-URL redirect mappings. Up to 8 configurable pairs.
     * Each pair consists of a language selection and a redirect URL.
     * Leave language empty to skip that mapping.
     */
    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "basic", title = "groupedit.language")
    private String mapping1Lang = "";

    @DataTableColumn(inputType = DataTableColumnType.ELFINDER, renderFormat = "dt-format-link", tab = "basic", title = "apps.app-language-redirect.redirect", editor = {
         @DataTableColumnEditor(
            attr = {
                 @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after")
             }
         )
    })
    private String mapping1Url = "";

    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "basic", title = "groupedit.language")
    private String mapping2Lang = "";

    @DataTableColumn(inputType = DataTableColumnType.ELFINDER, renderFormat = "dt-format-link", tab = "basic", title = "apps.app-language-redirect.redirect", editor = {
         @DataTableColumnEditor(
            attr = {
                 @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after")
             }
         )
    })
    private String mapping2Url = "";

    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "basic", title = "groupedit.language")
    private String mapping3Lang = "";

    @DataTableColumn(inputType = DataTableColumnType.ELFINDER, renderFormat = "dt-format-link", tab = "basic", title = "apps.app-language-redirect.redirect", editor = {
         @DataTableColumnEditor(
            attr = {
                 @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after")
             }
         )
    })
    private String mapping3Url = "";

    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "basic", title = "groupedit.language")
    private String mapping4Lang = "";

    @DataTableColumn(inputType = DataTableColumnType.ELFINDER, renderFormat = "dt-format-link", tab = "basic", title = "apps.app-language-redirect.redirect", editor = {
         @DataTableColumnEditor(
            attr = {
                 @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after")
             }
         )
    })
    private String mapping4Url = "";

    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "basic", title = "groupedit.language")
    private String mapping5Lang = "";

    @DataTableColumn(inputType = DataTableColumnType.ELFINDER, renderFormat = "dt-format-link", tab = "basic", title = "apps.app-language-redirect.redirect", editor = {
         @DataTableColumnEditor(
            attr = {
                 @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after")
             }
         )
    })
    private String mapping5Url = "";

    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "basic", title = "groupedit.language")
    private String mapping6Lang = "";

    @DataTableColumn(inputType = DataTableColumnType.ELFINDER, renderFormat = "dt-format-link", tab = "basic", title = "apps.app-language-redirect.redirect", editor = {
         @DataTableColumnEditor(
            attr = {
                 @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after")
             }
         )
    })
    private String mapping6Url = "";

    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "basic", title = "groupedit.language")
    private String mapping7Lang = "";

    @DataTableColumn(inputType = DataTableColumnType.ELFINDER, renderFormat = "dt-format-link", tab = "basic", title = "apps.app-language-redirect.redirect", editor = {
         @DataTableColumnEditor(
            attr = {
                 @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after")
             }
         )
    })
    private String mapping7Url = "";

    @DataTableColumn(inputType = DataTableColumnType.SELECT, tab = "basic", title = "groupedit.language")
    private String mapping8Lang = "";

    @DataTableColumn(inputType = DataTableColumnType.ELFINDER, renderFormat = "dt-format-link", tab = "basic", title = "apps.app-language-redirect.redirect", editor = {
         @DataTableColumnEditor(
            attr = {
                 @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after")
             }
         )
    })
    private String mapping8Url = "";

    /**
     * Dynamically gets language selection options from design template configuration.
     * Uses LayoutService.getLanguages() the same way as template editing.
     * Adds an empty option as the first item for all mapping fields.
     *
     * @param componentRequest Component request context
     * @param request HTTP request
     * @return Map with options for defaultLanguage and all mapping fields
    */
    @Override
    public Map<String, List<OptionDto>> getAppOptions(ComponentRequest componentRequest, HttpServletRequest request) {
        Map<String, List<OptionDto>> options = new HashMap<>();

        LayoutService ls = new LayoutService(request);
        List<LabelValueDetails> languages = ls.getLanguages(false, true);

        // Build options with empty option first, then all languages
        List<OptionDto> langOptions = new ArrayList<>();
        langOptions.add(new OptionDto("", "", null));
        for (LabelValueDetails lvd : languages) {
            langOptions.add(new OptionDto(lvd.getLabel(), lvd.getValue(), null));
         }

        // Add options for defaultLanguage and all 8 mapping fields
        options.put("defaultLanguage", langOptions.subList(1, langOptions.size())); // defaultLanguage should not have empty option

        options.put("mapping1Lang", langOptions);
        options.put("mapping2Lang", langOptions);
        options.put("mapping3Lang", langOptions);
        options.put("mapping4Lang", langOptions);
        options.put("mapping5Lang", langOptions);
        options.put("mapping6Lang", langOptions);
        options.put("mapping7Lang", langOptions);
        options.put("mapping8Lang", langOptions);

        return options;
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
     * Looks up the redirect URL for a given language from the 8 configurable mappings.
     * If no mapping is found for the given language, falls back to searching for defaultLanguage.
     *
     * @param lang the language to search for
     * @return the redirect URL, or null if no mapping is found
     */
    private String getRedirectUrl(String lang) {
        if (lang == null) return null;

        // First, try to find a mapping for the requested language
        String redirectUrl = null;
        if (lang.equals(mapping1Lang) && Tools.isNotEmpty(mapping1Url)) {
            redirectUrl = mapping1Url;
        } else if (lang.equals(mapping2Lang) && Tools.isNotEmpty(mapping2Url)) {
            redirectUrl = mapping2Url;
        } else if (lang.equals(mapping3Lang) && Tools.isNotEmpty(mapping3Url)) {
            redirectUrl = mapping3Url;
        } else if (lang.equals(mapping4Lang) && Tools.isNotEmpty(mapping4Url)) {
            redirectUrl = mapping4Url;
        } else if (lang.equals(mapping5Lang) && Tools.isNotEmpty(mapping5Url)) {
            redirectUrl = mapping5Url;
        } else if (lang.equals(mapping6Lang) && Tools.isNotEmpty(mapping6Url)) {
            redirectUrl = mapping6Url;
        } else if (lang.equals(mapping7Lang) && Tools.isNotEmpty(mapping7Url)) {
            redirectUrl = mapping7Url;
        } else if (lang.equals(mapping8Lang) && Tools.isNotEmpty(mapping8Url)) {
            redirectUrl = mapping8Url;
        }

        return redirectUrl;
    }

    /**
     * Default handler - redirects to the page language variant based on detected language.
     * Checks all 8 configurable language-to-URL mappings first.
     * If no mapping is found, falls back to defaultLanguage.
     * If the lng cookie exists and respectCookie is true, its value is used.
     *
     * @param request HTTP request
     * @return redirect URL to the language variant
     */
    @DefaultHandler
    public String view(HttpServletRequest request) {
        //in component previw in editor do nothing
        if (isEditorPreview(request)) {
            return EMPTY_PAGE;
        }

        // If rootOnly=true, redirect only on the root URL
        String path = PathFilter.getOrigPath(request);

        if (rootOnly && !"/".equals(path) && !"/index.html".equals(path) && !"".equals(path)) {
            Logger.debug(LanguageRedirectApp.class, "Not root path, skipping redirect: " + path);
            return EMPTY_PAGE;
        }

        String lang = null;

        // If respectCookie is true, check for lng cookie
        if (respectCookie) {
            String langCookie = Tools.getCookieValue(request.getCookies(), "lng", null);
            if (Tools.isNotEmpty(langCookie)) {
                lang = langCookie;
            }
        }
        if (Tools.isEmpty(lang)) {
            // Detect language from Accept-Language header
            lang = detectLanguage(request);
        }

        // Look up redirect URL from mappings (with defaultLanguage fallback)
        String redirectUrl = getRedirectUrl(lang);

        if (Tools.isEmpty(redirectUrl)) {
            //if no redirect was found for detected language, try to find redirect for default language
            redirectUrl = getRedirectUrl(defaultLanguage);
        }

        if (Tools.isNotEmpty(redirectUrl)) {
            Logger.debug(LanguageRedirectApp.class, "Redirecting to mapped URL: " + redirectUrl + " (lang: " + lang + ")");
            return "redirect:" + redirectUrl;
        }

        return EMPTY_PAGE;
    }
}
