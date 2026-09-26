package sk.iway.iwcm.components.welcome;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.users.userdetail.UserDetailsRepository;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.stat.rest.BrowserIdentifierMigrationService;
import sk.iway.iwcm.system.ntlm.AuthenticationFilter;

/** Verifies that security notices retain their conditions and use data-only action descriptors. */
class DashboardNoticeServiceTest {
    @Test
    void includesAllWarningsAndEscapesDynamicHtmlValues() {
        UserDetailsRepository users = mock(UserDetailsRepository.class);
        Identity user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(true);
        when(user.getUserId()).thenReturn(7);
        when(user.getLogin()).thenReturn("tester");
        when(user.isEnabledItem("modUpdate|users.edit_admins")).thenReturn(true);
        Prop prop = mock(Prop.class);
        when(prop.getText(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(prop.getText(eq("system.javaVersionWarningText"), anyString(), anyString()))
            .thenAnswer(invocation -> invocation.getArgument(2));
        when(prop.getText(eq("overview.license.expirationWarning"), anyString())).thenReturn("<a href=\"https://www.webjetcms.sk\">License</a>");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("javaVersion", "8.<script>alert(1)</script>");

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
             MockedStatic<Prop> properties = mockStatic(Prop.class);
             MockedStatic<Tools> tools = mockStatic(Tools.class, CALLS_REAL_METHODS);
             MockedStatic<FileTools> files = mockStatic(FileTools.class);
             MockedStatic<AuthenticationFilter> authentication = mockStatic(AuthenticationFilter.class);
             MockedStatic<BrowserIdentifierMigrationService> migration = mockStatic(BrowserIdentifierMigrationService.class);
             MockedConstruction<IwcmFile> paths = mockConstruction(IwcmFile.class, (file, context) -> when(file.exists()).thenReturn(true))) {
            properties.when(() -> Prop.getInstance(request)).thenReturn(prop);
            tools.when(() -> Tools.getRealPath(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
            constants.when(() -> Constants.getBoolean("2factorAuthEnabled")).thenReturn(true);
            constants.when(() -> Constants.getBoolean("useAmazonSES")).thenReturn(true);
            constants.when(() -> Constants.getInt("javaMinimalVersion")).thenReturn(17);
            constants.when(() -> Constants.getLong("licenseExpiryDate")).thenReturn(System.currentTimeMillis());

            List<Map<String, Object>> result = new DashboardNoticeService(users).load(user, request);

            assertEquals(List.of("twoFactor", "database", "browserMigration", "update", "java", "license", "amazonSes"), result.stream().map(notice -> notice.get("id")).toList());
            assertEquals("8.&lt;script&gt;alert(1)&lt;/script&gt;", result.get(4).get("bodyHtml"));
            assertTrue(result.get(5).get("bodyHtml").toString().startsWith("<a href="));
            assertEquals("popup", ((Map<?, ?>) result.get(0).get("action")).get("type"));
            assertEquals("/admin/v9/settings/stat-browser-migration/", ((Map<?, ?>) result.get(2).get("action")).get("url"));
            assertTrue(result.stream().noneMatch(notice -> notice.containsKey("onclick")));
            verify(users).getMobileDeviceByUserId(7L);
        }
    }

    /** A restricted administrator must not trigger migration or disabled 2FA lookups. */
    @Test
    void permissionChecksSkipRestrictedNoticeSources() {
        UserDetailsRepository users = mock(UserDetailsRepository.class);
        Identity user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(true);
        Prop prop = mock(Prop.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
             MockedStatic<Prop> properties = mockStatic(Prop.class);
             MockedStatic<Tools> tools = mockStatic(Tools.class, CALLS_REAL_METHODS);
             MockedStatic<BrowserIdentifierMigrationService> migration = mockStatic(BrowserIdentifierMigrationService.class);
             MockedConstruction<IwcmFile> paths = mockConstruction(IwcmFile.class)) {
            properties.when(() -> Prop.getInstance(request)).thenReturn(prop);
            tools.when(() -> Tools.getRealPath(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
            constants.when(() -> Constants.getBoolean("statWebJET7Converted")).thenReturn(true);

            assertTrue(new DashboardNoticeService(users).load(user, request).isEmpty());
            verifyNoInteractions(users);
            migration.verifyNoInteractions();
        }
    }

    @Test
    void deniedRequestsDoNotCheckUserSecurityOrDatabaseState() {
        UserDetailsRepository users = mock(UserDetailsRepository.class);
        assertThrows(AccessDeniedException.class, () -> new DashboardNoticeService(users).load(mock(Identity.class), new MockHttpServletRequest()));
        verifyNoInteractions(users);
    }

    @Test
    void understandsModernAndLegacyJavaVersionStrings() {
        assertEquals(17, DashboardNoticeService.javaMajorVersion("17.0.2"));
        assertEquals(25, DashboardNoticeService.javaMajorVersion("25"));
        assertEquals(8, DashboardNoticeService.javaMajorVersion("1.8.0_202"));
        assertEquals(-1, DashboardNoticeService.javaMajorVersion("unknown"));
        assertEquals(-1, DashboardNoticeService.javaMajorVersion(null));
    }
}
