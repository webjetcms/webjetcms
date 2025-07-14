package sk.iway.iwcm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import sk.iway.iwcm.components.domainRedirects.DomainRedirectDB;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.test.BaseWebjetTest;

class ToolsTest extends BaseWebjetTest {

    @BeforeAll
    public static void initJpa()
    {
        Constants.setString("jpaAddPackages", "sk.iway.iwcm.components.domainRedirects");
        DBPool.getInstance();
        DBPool.jpaInitialize();

        DocDB.getInstance();
        GroupsDB.getInstance();
    }

    @Test
    void testIsEmail()
    {
        // Valid email addresses
        assertTrue(Tools.isEmail("test@example.com"));
        assertTrue(Tools.isEmail("john.doe@example.co.uk"));
        assertTrue(Tools.isEmail("jane_doe123@example-domain.com"));
        assertTrue(Tools.isEmail("jane_doe123@example-domain.com"));

        assertTrue(Tools.isEmail("a@azet.sk"));
        assertTrue(Tools.isEmail("0@azet.sk"));
        assertTrue(Tools.isEmail("oksi11@i.ua"));
        assertTrue(Tools.isEmail("mail@sl.posta"));
        assertTrue(Tools.isEmail("mail@sl.posta.sk"));

        //Yes, a domain name can start with a number.
        assertTrue(Tools.isEmail("0@00centrum.sk"));
        assertTrue(Tools.isEmail("0@00.com"));
        assertTrue(Tools.isEmail("0@0.com"));

        // Invalid email addresses
        assertFalse(Tools.isEmail("Marek&@gmail.com"));
        assertFalse(Tools.isEmail("peta.miusová@gmail.com"));
        assertFalse(Tools.isEmail("_slavo_niekto@centrum.sk"));
        assertFalse(Tools.isEmail("hasek@domena,cz"));
    }

    @Test
    void testGetServerNameWithoutAlias() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServerName()).thenReturn("example.com");

        String serverName = Tools.getServerName(request, false);
        assertEquals("example.com", serverName);
    }

    @Test
    void testGetServerNameWithAlias() {
        Constants.setBoolean("multiDomainEnabled", true);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServerName()).thenReturn("iwcm.interway.sk");

        String domainAlias = DomainRedirectDB.getDomainFromAlias(request.getServerName());
        System.out.println("domainAlias: " + domainAlias);
        assertEquals(System.getenv("CODECEPT_DEFAULT_DOMAIN_NAME"), domainAlias);

        String serverName = Tools.getServerName(request, true);
        assertEquals(System.getenv("CODECEPT_DEFAULT_DOMAIN_NAME"), serverName);
    }

    @Test
    void testGetServerNameWithForwardedHost() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServerName()).thenReturn("example.com");
        when(request.getHeader("x-forwarded-host")).thenReturn("forwarded.com");
        Constants.setBoolean("serverBeyoundProxy", true);

        String serverName = Tools.getServerName(request, false);
        assertEquals("forwarded.com", serverName);
    }

    @Test
    void testGetServerNameWithPort() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServerName()).thenReturn("example.com:8080");

        String serverName = Tools.getServerName(request, false);
        assertEquals("example.com", serverName);

        when(request.getServerName()).thenReturn("example.com");
        when(request.getHeader("x-forwarded-host")).thenReturn("forwarded-port.com:8000");
        Constants.setBoolean("serverBeyoundProxy", true);

        serverName = Tools.getServerName(request, false);
        assertEquals("forwarded-port.com", serverName);
    }

    @Test
    void testGetServerNameWithNullRequest() {
        String serverName = Tools.getServerName(null, false);
        assertEquals(null, serverName);
    }

    @Test
    void testConvertToHtmlTags_basicReplacements() {
        assertEquals("<div>", Tools.convertToHtmlTags("*|div|*"));
        assertEquals("</div>", Tools.convertToHtmlTags("*||div|*"));
        assertEquals("<span>", Tools.convertToHtmlTags("*|span|*"));
        assertEquals("</span>", Tools.convertToHtmlTags("*||span|*"));
    }

    @Test
    void testConvertToHtmlTags_ampHash47Replacement() {
        assertEquals("Janko&#47;Ferko", Tools.convertToHtmlTags("Janko&amp;#47;Ferko"));
        assertEquals("<div>&#47;</div>", Tools.convertToHtmlTags("*|div|*&amp;#47;*||div|*"));
    }

    @Test
    void testConvertToHtmlTags_htmlModeReplacements() {
        assertEquals("", Tools.convertToHtmlTags("&lt;&#47;div&gt;").trim()); //no opening tag, SafeHtmlConverter will return empty string
        assertEquals("&lt;div&gt;", Tools.convertToHtmlTags("&lt;div&gt;")); //no closing tag, do not convert
        assertEquals("&gt;", Tools.convertToHtmlTags("&gt;"));
        assertEquals("<div></div>", Tools.convertToHtmlTags("&lt;div&gt;&lt;&#47;div&gt;"));
    }

    @Test
    void testConvertToHtmlTags_multipleReplacements() {
        String input = "*|div|*Some text*||div|*";
        String expected = "<div>Some text</div>";
        assertEquals(expected, Tools.convertToHtmlTags(input));
    }

    @Test
    void testConvertToHtmlTags_noReplacement() {
        String input = "plain text";
        assertEquals("plain text", Tools.convertToHtmlTags(input));
    }

}