package sk.iway.iwcm.system.captcha;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;

class CaptchaTest {

    @Test
    void usesResponseSubmittedByCurrentForm() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        assertEquals("current-form-token", Captcha.getSubmittedReCaptchaResponse(request, "current-form-token"));
        verify(request, never()).getParameter("g-recaptcha-response");
        verify(request, never()).getSession();
    }

    @Test
    void readsResponseFromCurrentRequestWhenNotPassedExplicitly() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("g-recaptcha-response")).thenReturn("request-token");

        assertEquals("request-token", Captcha.getSubmittedReCaptchaResponse(request, null));
        verify(request).getParameter("g-recaptcha-response");
        verify(request, never()).getSession();
    }

    @Test
    void emptyExplicitResponseFallsBackToCurrentRequestWithoutAccessingSession() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("g-recaptcha-response")).thenReturn("request-token");

        assertEquals("request-token", Captcha.getSubmittedReCaptchaResponse(request, ""));
        verify(request).getParameter("g-recaptcha-response");
        verify(request, never()).getSession();
    }
}
