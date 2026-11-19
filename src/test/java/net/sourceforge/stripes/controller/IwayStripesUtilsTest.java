package net.sourceforge.stripes.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Verifies locale changes through nested servlet request wrappers. */
class IwayStripesUtilsTest extends BaseWebjetTest {

    @Test
    void updatesStripesLocaleInsideMultipleWrappers() throws Exception {
        StripesRequestWrapper stripes = new StripesRequestWrapper(new MockHttpServletRequest());
        stripes.setLocale(Locale.ENGLISH);
        HttpServletRequest request = new HttpServletRequestWrapper(new HttpServletRequestWrapper(stripes));

        try (MockedStatic<PageLng> languages = mockStatic(PageLng.class)) {
            languages.when(() -> PageLng.getUserLngIso("sk")).thenReturn("sk-SK");

            IwayStripesUtils.setLocale(request, "sk");

            assertEquals(Locale.forLanguageTag("sk-SK"), request.getLocale());
        }
    }

    @Test
    void toleratesRequestsWithoutStripesWrapper() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setPreferredLocales(java.util.List.of(Locale.ENGLISH));

        assertDoesNotThrow(() -> IwayStripesUtils.setLocale(new HttpServletRequestWrapper(request), "sk"));
        assertEquals(Locale.ENGLISH, request.getLocale());
    }
}
