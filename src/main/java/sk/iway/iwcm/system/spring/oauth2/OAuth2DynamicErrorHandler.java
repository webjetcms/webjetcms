package sk.iway.iwcm.system.spring.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Logger;

import java.io.IOException;

/**
 * Dynamic OAuth2 Error Handler that processes errors during OAuth2 authentication.
 * Based on session attribute decides whether to redirect to admin or user login page.
 */
public class OAuth2DynamicErrorHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {

        // Determine if this is admin or user login
        boolean isAdminLogin = OAuth2LoginHelper.isAdminLogin(request);

        // Get error code from exception
        String errorCode = getErrorCodeFromException(exception);

        Logger.error(OAuth2DynamicErrorHandler.class, "OAuth2 authentication failed - isAdminLogin: " + isAdminLogin + ", error: " + errorCode + ", exception: " + exception.getMessage());
        Adminlog.add(Adminlog.TYPE_USER_LOGON, "OAuth2 authentication failed - isAdminLogin: " + isAdminLogin + ", error: " + errorCode + ", exception: " + exception.getMessage(), -1, -1);

        // Set error and redirect
        OAuth2LoginHelper.handleError(request, response, errorCode, isAdminLogin);
    }

    /**
     * Gets error code from exception for displaying to user
     *
     * @param exception authentication exception
     * @return error code for translation table
     */
    private String getErrorCodeFromException(AuthenticationException exception) {
        if (exception == null) {
            return "oauth2_unknown_error";
        }

        String message = exception.getMessage();
        String exceptionClass = exception.getClass().getSimpleName();

        Logger.debug(OAuth2DynamicErrorHandler.class, "Processing exception: " + exceptionClass + " - " + message);

        // Mapping specific errors to codes
        if (exceptionClass.contains("OAuth2AuthorizationCodeRequestTypeNotSupported")) {
            return "oauth2_provider_not_configured";
        }
        if (exceptionClass.contains("OAuth2AuthenticationException") && message != null) {
            if (message.contains("invalid_token") || message.contains("invalid_grant")) {
                return "oauth2_invalid_token";
            }
            if (message.contains("access_denied")) {
                return "oauth2_access_denied";
            }
            if (message.contains("server_error")) {
                return "oauth2_server_error";
            }
        }
        if (exceptionClass.contains("AuthorizationRequestNotFoundException")) {
            return "oauth2_authorization_request_not_found";
        }
        if (exceptionClass.contains("ClientAuthorizationException")) {
            return "oauth2_client_authorization_failed";
        }

        // General error
        return "oauth2_authentication_failed";
    }
}
