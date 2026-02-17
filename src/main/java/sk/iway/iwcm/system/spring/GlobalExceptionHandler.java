package sk.iway.iwcm.system.spring;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sk.iway.iwcm.Logger;

import java.io.IOException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(Exception ex, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Logger.debug(this, "Authorization exception caught: " + ex.getMessage());

        boolean basicAuthEnabled = SpringSecurityConf.isBasicAuthEnabled();

        // For REST/API requests
        if ((request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json")) ||
            request.getRequestURI().contains("/rest/")) {

            if (basicAuthEnabled) {
                // User not authenticated - return 401 to trigger basic auth
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setHeader("WWW-Authenticate", "Basic realm=\"Secure Area\"");
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Authentication required\",\"status\":401}");
                response.flushBuffer();
            } else {
                // User authenticated but not authorized - return 403
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Access Denied\",\"status\":403}");
            }
            return null;
        }

        // For regular web requests
        if (basicAuthEnabled) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("WWW-Authenticate", "Basic realm=\"Secure Area\"");
        }
        return "forward:/403.jsp";
    }
}