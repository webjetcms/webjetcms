package sk.iway.iwcm.system.spring.webjet_component;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
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
public class WebjetViewResolver extends WebApplicationObjectSupport implements ViewResolver, Ordered {

    private List<ViewResolver> viewResolvers;

    /**
    * In Spring Boot, the main DispatcherServlet is automatically configured with its own
    * view resolvers (e.g. InternalResourceViewResolver), which have LOWEST_PRECEDENCE and
    * ALWAYS return a view (forward) even if the file does not exist. In order to use this WebJET
    * resolver, which looks for a real .jsp/.ftl/.html file and returns null if not found (and thus
    * drops other resolvers including forward:/redirect:), must have a higher priority
    * than the default resolvers. Returning LOWEST_PRECEDENCE - 100 will rank it just ahead of them.
    */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 100;
    }

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

    @Override
    public View resolveViewName(@NonNull String viewName, @NonNull Locale locale) throws Exception {
        return resolveViewName(viewName, locale, null);
    }

    View resolveViewName(@NonNull String viewName, @NonNull Locale locale, @Nullable String viewFolder) throws Exception {
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

            if (viewNameLocal.endsWith(".jsp")) {
                if (!(viewResolver instanceof WebjetInternalResourceViewResolver)) {
                    continue;
                }
                if (viewNameLocal.startsWith("forward:")) {
                    return viewResolver.resolveViewName(viewNameLocal, locale);
                }
            }

            if (Tools.isNotEmpty(prefix)) {
                viewNameLocal = prefix + viewNameLocal;
            }

            if (Tools.isNotEmpty(suffix) && !viewNameLocal.endsWith(suffix)) {
                viewNameLocal = viewNameLocal + suffix;
                // kedze pridavam suffix do viewName, tak uz nie je potrebny vo viewResolveri, kedze ten je sprosty a vlozi suffix do viewName znova
                //wjViewResolver.setSuffix("");
            }

            // cesta s installName
            List<String> paths = getPaths(viewNameLocal, viewFolder);
            for (String path : paths) {
                String normalizedPath = normalizePath(path);
                if (FileTools.isFile(normalizedPath)) {
                    String resolverViewName = normalizedPath;
                    if (Tools.isNotEmpty(suffix) && resolverViewName.endsWith(suffix)) {
                        resolverViewName = resolverViewName.substring(0, resolverViewName.length() - suffix.length());
                    }
                    return viewResolver.resolveViewName(resolverViewName, locale);
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
                        List<String> jspPaths = getPaths(jspPath, viewFolder);
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

    private List<String> getPaths(String viewName, @Nullable String viewFolder) {
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
}
