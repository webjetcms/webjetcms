package sk.iway.iwcm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Tools.getRemoteIP method.
 * Tests various configurations of xForwardedForHeader constant and different
 * X-Forwarded-For header values including proxy chains, unknown values, and edge cases.
 */
class ToolsGetRemoteIPTest {

     @BeforeEach
    void setUp() {
         // Reset constants before each test to ensure clean state
        Constants.setBoolean("serverBeyoundProxy", false);
        Constants.setString("xForwardedForHeader", "x-forwarded-for");
     }

      @Test
    void testGetRemoteIP_noProxy() {
         // When serverBeyoundProxy is false, remoteAddr is returned as-is
        Constants.setBoolean("serverBeyoundProxy", false);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        String ip = Tools.getRemoteIP(request);
        assertEquals("192.168.1.1", ip);
     }

      @Test
    void testGetRemoteIP_proxyWithFirstIP() {
         // When serverBeyoundProxy is true and xForwardedForHeader is "x-forwarded-for"
         // (no ::last suffix), the first valid IP is used
        Constants.setBoolean("serverBeyoundProxy", true);
        Constants.setString("xForwardedForHeader", "x-forwarded-for");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("x-forwarded-for")).thenReturn("203.0.113.50, 70.41.3.18, 150.172.238.178");

        String ip = Tools.getRemoteIP(request);
        assertEquals("203.0.113.50", ip);
     }

      @Test
    void testGetRemoteIP_proxyWithLastIP() {
         // When xForwardedForHeader ends with "::last", the last valid IP is used
        Constants.setBoolean("serverBeyoundProxy", true);
        Constants.setString("xForwardedForHeader", "x-forwarded-for::last");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("x-forwarded-for")).thenReturn("203.0.113.50, 70.41.3.18, 150.172.238.178");

        String ip = Tools.getRemoteIP(request);
        assertEquals("150.172.238.178", ip);
     }

      @Test
    void testGetRemoteIP_proxyWithUnknownValues() {
         // "unknown" values should be skipped, valid IP should be returned
        Constants.setBoolean("serverBeyoundProxy", true);
        Constants.setString("xForwardedForHeader", "x-forwarded-for");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("x-forwarded-for")).thenReturn("unknown, 195.168.35.4, unknown");

        String ip = Tools.getRemoteIP(request);
        assertEquals("195.168.35.4", ip);
     }

      @Test
    void testGetRemoteIP_proxyWithUnknownAndLastIP() {
         // With ::last suffix and unknown values, the last valid IP should be used
        Constants.setBoolean("serverBeyoundProxy", true);
        Constants.setString("xForwardedForHeader", "x-forwarded-for::last");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("x-forwarded-for")).thenReturn("unknown, 195.168.35.4, 10.20.30.40");

        String ip = Tools.getRemoteIP(request);
        assertEquals("10.20.30.40", ip);
     }

      @Test
    void testGetRemoteIP_proxyShortHeader() {
         // x-forwarded-for header too short (< 5 chars) should be ignored, remoteAddr used
        Constants.setBoolean("serverBeyoundProxy", true);
        Constants.setString("xForwardedForHeader", "x-forwarded-for");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getHeader("x-forwarded-for")).thenReturn("abc");

        String ip = Tools.getRemoteIP(request);
        assertEquals("192.168.1.100", ip);
     }

      @Test
    void testGetRemoteIP_proxyHeaderIsNull() {
         // null x-forwarded-for header should be ignored, remoteAddr used
        Constants.setBoolean("serverBeyoundProxy", true);
        Constants.setString("xForwardedForHeader", "x-forwarded-for");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getHeader("x-forwarded-for")).thenReturn(null);

        String ip = Tools.getRemoteIP(request);
        assertEquals("192.168.1.100", ip);
     }

      @Test
    void testGetRemoteIP_proxyAllUnknownValues() {
         // All values are "unknown" or without dots — no valid IP found, remoteAddr used
        Constants.setBoolean("serverBeyoundProxy", true);
        Constants.setString("xForwardedForHeader", "x-forwarded-for");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getHeader("x-forwarded-for")).thenReturn("unknown, unknown, ipv6only");

        String ip = Tools.getRemoteIP(request);
        assertEquals("192.168.1.100", ip);
     }

      @Test
    void testGetRemoteIP_proxySingleValidIP() {
         // Single valid IP in x-forwarded-for
        Constants.setBoolean("serverBeyoundProxy", true);
        Constants.setString("xForwardedForHeader", "x-forwarded-for");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("x-forwarded-for")).thenReturn("85.215.100.50");

        String ip = Tools.getRemoteIP(request);
        assertEquals("85.215.100.50", ip);
     }

      @Test
    void testGetRemoteIP_proxyCustomHeaderName() {
         // Custom header name for x-forwarded-for
        Constants.setBoolean("serverBeyoundProxy", true);
        Constants.setString("xForwardedForHeader", "x-real-ip");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.5");
        when(request.getHeader("x-real-ip")).thenReturn("172.16.0.1");

        String ip = Tools.getRemoteIP(request);
        assertEquals("172.16.0.1", ip);
     }

      @Test
    void testGetRemoteIP_proxyCustomHeaderWithLastSuffix() {
         // Custom header with ::last suffix
        Constants.setBoolean("serverBeyoundProxy", true);
        Constants.setString("xForwardedForHeader", "x-real-ip::last");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.5");
        when(request.getHeader("x-real-ip")).thenReturn("172.16.0.1, 172.16.0.2, 172.16.0.3");

        String ip = Tools.getRemoteIP(request);
        assertEquals("172.16.0.3", ip);
     }

      @Test
    void testGetRemoteIP_proxySpacesInValues() {
         // Values with leading/trailing spaces should be trimmed
        Constants.setBoolean("serverBeyoundProxy", true);
        Constants.setString("xForwardedForHeader", "x-forwarded-for");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("x-forwarded-for")).thenReturn("   192.0.2.1   ,   192.0.2.2   ,   192.0.2.3   ");

        String ip = Tools.getRemoteIP(request);
        assertEquals("192.0.2.1", ip);
     }

      @Test
    void testGetRemoteIP_proxyLastIPWithSpaces() {
         // Last IP with spaces should be trimmed correctly
        Constants.setBoolean("serverBeyoundProxy", true);
        Constants.setString("xForwardedForHeader", "x-forwarded-for::last");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("x-forwarded-for")).thenReturn("192.0.2.1, 192.0.2.2,  192.0.2.99  ");

        String ip = Tools.getRemoteIP(request);
        assertEquals("192.0.2.99", ip);
     }

      @Test
    void testGetRemoteIP_proxyEmptyHeader() {
         // Empty x-forwarded-for header should be ignored
        Constants.setBoolean("serverBeyoundProxy", true);
        Constants.setString("xForwardedForHeader", "x-forwarded-for");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getHeader("x-forwarded-for")).thenReturn("");

        String ip = Tools.getRemoteIP(request);
        assertEquals("192.168.1.100", ip);
     }

      @Test
    void testGetRemoteIP_proxyOnlyDots() {
         // Values with only dots but no valid IP structure should be skipped
        Constants.setBoolean("serverBeyoundProxy", true);
        Constants.setString("xForwardedForHeader", "x-forwarded-for");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getHeader("x-forwarded-for")).thenReturn(". . .");

        String ip = Tools.getRemoteIP(request);
        assertEquals("192.168.1.100", ip);
     }

      @Test
    void testGetRemoteIP_proxyMixedValidAndInvalid() {
         // Mix of valid and invalid IPs, first valid should be returned
        Constants.setBoolean("serverBeyoundProxy", true);
        Constants.setString("xForwardedForHeader", "x-forwarded-for");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("x-forwarded-for")).thenReturn("unknown, .invalid, 203.0.113.100, 198.51.100");

        String ip = Tools.getRemoteIP(request);
        assertEquals("203.0.113.100", ip);
     }

}
