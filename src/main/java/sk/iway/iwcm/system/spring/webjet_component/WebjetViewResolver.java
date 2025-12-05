package sk.iway.iwcm.system.spring.webjet_component;

import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.RedirectView;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;

import java.util.*;

// vracia vhodny view z viewResolverov na zaklade cesty
public class WebjetViewResolver extends WebApplicationObjectSupport implements ViewResolver {

    private String viewFolder;
    private List<ViewResolver> viewResolvers;

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
                if (FileTools.isFile(path)) {
                    return viewResolver.resolveViewName(Tools.replace(path, suffix, ""), locale);
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

        result.add("/" + Tools.join(tokens2, "/"));
        result.add("/" + Tools.join(tokens1, "/"));

        return result;
    }

    public String getViewFolder() {
        return viewFolder;
    }

    public void setViewFolder(String viewFolder) {
        this.viewFolder = viewFolder;
    }
}