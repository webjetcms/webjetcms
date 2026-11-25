package sk.iway.iwcm.system.spring;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sk.iway.iwcm.Logger;

import java.io.IOException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
        if (request.getRequestURI().contains("/rest/")) {
            Logger.debug(this, "ResponseStatusException caught for REST request: " + ex.getStatusCode() + " " + ex.getReason());

            String body = ex.getReason();
            if (body == null || body.isEmpty()) {
                body = ex.getStatusCode().toString();
            }
            return ResponseEntity.status(ex.getStatusCode()).body(body);
        }

        throw ex;
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(Exception ex, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Logger.debug(this, "Authorization exception caught: " + ex.getMessage());

        if ((request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json")) ||
            request.getRequestURI().contains("/rest/")) {
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Access Denied\",\"status\":403}");
            return null;
        }

        return "forward:/403.jsp";
    }
}
