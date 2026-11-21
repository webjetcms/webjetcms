package sk.iway.iwcm.system.spring;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.users.UsersDB;

import java.io.IOException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private final AuthenticationTrustResolver authenticationTrustResolver = new AuthenticationTrustResolverImpl();

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public String handleAccessDenied(Exception ex, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Logger.debug(this, "Authorization exception caught: " + ex.getMessage());

        // Missing permissions must not challenge users who are already logged in.
        boolean basicAuthRequired = SpringSecurityConf.isBasicAuthEnabled()
            && authenticationTrustResolver.isAuthenticated(SecurityContextHolder.getContext().getAuthentication()) == false
            && UsersDB.getCurrentUser(request.getSession(false)) == null;
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        // For REST/API requests
        if ((request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json")) ||
            request.getRequestURI().contains("/rest/")) {

            if (basicAuthRequired) {
                // User not authenticated - return 401 to trigger basic auth
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setHeader("WWW-Authenticate", "Basic realm=\"Secure Area\"");
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Authentication required\",\"status\":401}");
                response.flushBuffer();
            } else {
                // Access denied without requesting authentication again.
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Access Denied\",\"status\":403}");
            }
            return null;
        }

        // For regular web requests
        if (basicAuthRequired) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("WWW-Authenticate", "Basic realm=\"Secure Area\"");
        }
        return "forward:/403.jsp";
    }
}
