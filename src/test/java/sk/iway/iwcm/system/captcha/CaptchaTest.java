package sk.iway.iwcm.system.captcha;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Tests how {@link Captcha} selects the submitted reCAPTCHA response from explicit
 * form data and the current request.
 */
class CaptchaTest {

    /**
     * Verifies that an explicitly submitted response takes precedence without accessing
     * the request parameter or session.
     */
    @Test
    void usesResponseSubmittedByCurrentForm() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        assertEquals("current-form-token", Captcha.getSubmittedReCaptchaResponse(request, "current-form-token"));
        verify(request, never()).getParameter("g-recaptcha-response");
        verify(request, never()).getSession();
    }

    /**
     * Verifies that a missing explicit response falls back to the current request without
     * accessing the session.
     */
    @Test
    void readsResponseFromCurrentRequestWhenNotPassedExplicitly() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("g-recaptcha-response")).thenReturn("request-token");

        assertEquals("request-token", Captcha.getSubmittedReCaptchaResponse(request, null));
        verify(request).getParameter("g-recaptcha-response");
        verify(request, never()).getSession();
    }

    /**
     * Verifies that an empty explicit response falls back to the current request without
     * accessing the session.
     */
    @Test
    void emptyExplicitResponseFallsBackToCurrentRequestWithoutAccessingSession() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("g-recaptcha-response")).thenReturn("request-token");

        assertEquals("request-token", Captcha.getSubmittedReCaptchaResponse(request, ""));
        verify(request).getParameter("g-recaptcha-response");
        verify(request, never()).getSession();
    }
}
