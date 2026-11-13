package sk.iway.iwcm.components.ai.tools;

import java.nio.charset.StandardCharsets;

import org.springframework.web.util.UriUtils;

import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.system.spring.SpringAppInitializer;

/** Checks the servlet and URL filters the container selected before WebJET received the named request. */
final class AiToolServletRoutes {

    private AiToolServletRoutes() { }

    static boolean permits(HttpServletRequest source, HttpServletRequest target) {
        return permits(source.getServletContext(), applicationPath(source), applicationPath(target), false);
    }

    static boolean mayList(ServletContext context, AiToolRegistry.Entry entry) {
        String source = "/admin/rest/ai/tools/" + entry.definition().name() + "/call";
        return permits(context, source, entry.path(), entry.path().contains("{") || entry.path().contains("*") || entry.path().contains("?"));
    }

    private static boolean permits(ServletContext context, String original, String destination, boolean template) {
        try {
            if (!usesDispatcher(context, original) || (!template && !usesDispatcher(context, destination))) return false;
            for (FilterRegistration filter : context.getFilterRegistrations().values()) {
                for (String pattern : filter.getUrlPatternMappings()) {
                    // Registrations omit dispatcher types. A second pattern on the same filter may be FORWARD-only.
                    // For templates, only a literal prefix shared by every expansion can preclude listing.
                    if (template && (!pattern.endsWith("/*") || pattern.contains("{") || pattern.contains("?"))) continue;
                    if (match(pattern, destination) >= 0 && match(pattern, original) < 0) return false;
                }
            }
            return true;
        } catch (UnsupportedOperationException | IllegalStateException unavailable) {
            return false;
        }
    }

    static boolean usesDispatcher(HttpServletRequest request) {
        String selected = request.getHttpServletMapping().getServletName();
        if (selected != null && !selected.isEmpty()) return SpringAppInitializer.DISPATCHER_NAME.equals(selected);
        try {
            return usesDispatcher(request.getServletContext(), applicationPath(request));
        } catch (UnsupportedOperationException | IllegalStateException unavailable) {
            return false;
        }
    }

    private static String applicationPath(HttpServletRequest request) {
        return UriUtils.decode(request.getRequestURI().substring(request.getContextPath().length()), StandardCharsets.UTF_8);
    }

    private static boolean usesDispatcher(ServletContext context, String path) {
        int mvc = -1;
        int other = -1;
        for (var servlet : context.getServletRegistrations().entrySet()) {
            for (String pattern : servlet.getValue().getMappings()) {
                // "/" is the default servlet mapping, below extensions and all prefix mappings.
                int rank = "/".equals(pattern) ? 0 : match(pattern, path);
                if (SpringAppInitializer.DISPATCHER_NAME.equals(servlet.getKey())) mvc = Math.max(mvc, rank);
                else other = Math.max(other, rank);
            }
        }
        return mvc > other;
    }

    private static int match(String pattern, String path) {
        if (pattern.equals(path) || (pattern.isEmpty() && "/".equals(path))) return Integer.MAX_VALUE;
        if (pattern.endsWith("/*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            if (path.equals(prefix) || path.startsWith(prefix + "/")) return 2 + prefix.length();
        } else if (pattern.startsWith("*.")) {
            int dot = path.lastIndexOf('.');
            if (dot > path.lastIndexOf('/') && path.substring(dot + 1).equals(pattern.substring(2))) return 1;
        }
        return -1;
    }
}
