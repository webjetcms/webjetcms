package sk.iway.iwcm.system.spring.webjet_component;

import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.RedirectView;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;

import java.util.*;

// vracia vhodny view z viewResolverov na zaklade cesty
public class WebjetViewResolver extends WebApplicationObjectSupport implements ViewResolver {

    private String viewFolder;
    private List<ViewResolver> viewResolvers;

    /**
     * Normalizes a path by collapsing multiple consecutive slashes into a single slash.
     * This fixes the double-slash issue in path construction (e.g., "//admin/..." -> "/admin/...").
     */
    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        // Collapse multiple slashes into single slash, but preserve leading slash
        return path.replaceAll("/{2,}", "/");
    }

    public View resolveViewName(@NonNull String viewName, @NonNull Locale locale) throws Exception {
        if (viewResolvers == null) {
            return null;
        }

        if (viewName.startsWith("redirect:")) {
            return new RedirectView(Tools.replace(viewName, "redirect:", ""));
        }

        for (ViewResolver viewResolver : viewResolvers) {

            String viewNameLocal = viewName;

            if (!WebjetInternalResourceViewResolver.class.isAssignableFrom(viewResolver.getClass())
                    && !WebjetFreeMarkerViewResolver.class.isAssignableFrom(viewResolver.getClass())
                    && !ThymeleafViewResolver.class.isAssignableFrom(viewResolver.getClass())) {
                View view = viewResolver.resolveViewName(viewNameLocal, locale);
                if (view != null) {
                    return view;
                }

                continue;
            }

            String prefix;
            String suffix;

            if (viewResolver instanceof WebjetInternalResourceViewResolver wjViewResolver) {
                prefix = wjViewResolver.getPrefix();
                suffix = wjViewResolver.getSuffix();
            }
            else if (viewResolver instanceof ThymeleafViewResolver) {
                prefix = "/";
                suffix = ".html";
            } else {
                WebjetFreeMarkerViewResolver wjViewResolver = (WebjetFreeMarkerViewResolver) viewResolver;
                prefix = wjViewResolver.getPrefix();
                suffix = wjViewResolver.getSuffix();
            }

            // FIX: If the view name ends with .jsp, route it directly to the JSP resolver
            // without applying the .html suffix (which was causing FileNotFoundException)
            if (viewNameLocal.endsWith(".jsp")) {
                if (viewResolver instanceof WebjetInternalResourceViewResolver) {
                    return viewResolver.resolveViewName(viewNameLocal, locale);
                }
                continue;
            }

            if (Tools.isNotEmpty(prefix)) {
                viewNameLocal = prefix + viewNameLocal;
            }

            if (Tools.isNotEmpty(suffix) && !viewNameLocal.contains(suffix)) {
                viewNameLocal = viewNameLocal + suffix;
                // kedze pridavam suffix do viewName, tak uz nie je potrebny vo viewResolveri, kedze ten je sprosty a vlozi suffix do viewName znova
                //wjViewResolver.setSuffix("");
            }

            // cesta s installName
            List<String> paths = getPaths(viewNameLocal);
            for (String path : paths) {
                String normalizedPath = normalizePath(path);
                if (FileTools.isFile(normalizedPath)) {
                    return viewResolver.resolveViewName(Tools.replace(normalizedPath, suffix, ""), locale);
                }
            }

            // FIX: If Thymeleaf suffix (.html) was used and file not found,
            // try the JSP resolver with .jsp suffix as fallback.
            // This handles the case where the actual file is .jsp but the view name
            // doesn't explicitly end with .jsp (e.g., "/admin/skins/webjet8/logon-spring").
            if (viewResolver instanceof ThymeleafViewResolver && suffix != null && suffix.equals(".html")) {
                for (ViewResolver otherResolver : viewResolvers) {
                    if (otherResolver instanceof WebjetInternalResourceViewResolver) {
                        // Try with .jsp suffix
                        String jspPath = viewName;
                        if (Tools.isNotEmpty(prefix)) {
                            jspPath = prefix + viewName;
                        }
                        List<String> jspPaths = getPaths(jspPath);
                        for (String jspPathCandidate : jspPaths) {
                            String jspFile = normalizePath(jspPathCandidate);
                            // Replace .html with .jsp if suffix was .html
                            if (jspFile.endsWith(".html")) {
                                jspFile = jspFile.substring(0, jspFile.length() - 5) + ".jsp";
                            } else {
                                jspFile = jspFile + ".jsp";
                            }
                            if (FileTools.isFile(jspFile)) {
                                Logger.debug(WebjetViewResolver.class, "Falling back to JSP: " + jspFile);
                                return otherResolver.resolveViewName(Tools.replace(jspFile, ".jsp", ""), locale);
                            }
                        }
                        break;
                    }
                }
            }
        }

        Logger.debug(WebjetViewResolver.class, String.format("JSP not found: %s", viewName));
        //throw new Exception(String.format("JSP not found: %s", viewName));

        return null;
    }

    public void setViewResolvers(List<ViewResolver> viewResolvers)
    {
        this.viewResolvers = viewResolvers;
        this.viewResolvers.sort(Comparator.comparing(o -> ((Ordered) o).getOrder()));
    }

    private List<String> getPaths(String viewName) {
        String installName = Constants.getInstallName();
        List<String> result = new ArrayList<>();

        List<String> tokens1 = new ArrayList<>(Arrays.asList(Tools.getTokens(viewName, "/")));
        if (Tools.isNotEmpty(viewFolder)) {
            tokens1.add(tokens1.size() - 1, viewFolder);
        }

        List<String> tokens2 = new ArrayList<>(Arrays.asList(Tools.getTokens(viewName, "/")));
        tokens2.add(1, installName);
        if (Tools.isNotEmpty(viewFolder)) {
            tokens2.add(tokens2.size() - 1, viewFolder);
        }

        // Normalize paths to collapse double slashes (e.g., "//admin/..." -> "/admin/...")
        result.add(normalizePath("/" + Tools.join(tokens2, "/")));
        result.add(normalizePath("/" + Tools.join(tokens1, "/")));

        return result;
    }

    public String getViewFolder() {
        return viewFolder;
    }

    public void setViewFolder(String viewFolder) {
        this.viewFolder = viewFolder;
    }
}
