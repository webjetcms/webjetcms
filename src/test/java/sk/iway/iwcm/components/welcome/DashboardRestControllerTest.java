package sk.iway.iwcm.components.welcome;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

import tools.jackson.databind.json.JsonMapper;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.users.UsersDB;

/** Verifies server-selected ownership and domain context for dashboard preference requests. */
class DashboardRestControllerTest {
    /** Ensures the HTTP mapper can read and write the settings contract across Jackson versions. */
    @Test
    void runtimeJsonMapperRoundTripsWidgetAndDomainOptions() {
        JsonMapper mapper = JsonMapper.builder().build();
        String json = """
            {"version":1,"configured":true,"items":[
                {"id":"form-1","type":"forms","size":"3x3","collapsed":false,
                 "options":{"days":7,"nested":{"enabled":true}}}],
             "domainOptions":{"form-1":{"formName":"Contact"}},"acknowledgedNewsVersion":null}
            """;

        DashboardSettingsDto settings = mapper.readValue(json, DashboardSettingsDto.class);
        assertEquals(7, settings.getItems().get(0).getOptions().get("days"));
        assertEquals("Contact", settings.getDomainOptions().get("form-1").get("formName"));
        assertEquals(mapper.readTree(json), mapper.readTree(mapper.writeValueAsString(settings)));
    }

    @Test
    void ignoresCallerSuppliedOwnerAndDomainParameters() {
        DashboardSettingsService settings = mock(DashboardSettingsService.class);
        DashboardRestController controller = new DashboardRestController(settings, mock(DashboardRecentPagesService.class), mock(DashboardNoticeService.class), mock(DashboardLegacyDataService.class));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("userId", "99");
        request.setParameter("domainId", "999");
        Identity user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(true);
        when(user.getUserId()).thenReturn(7);
        DashboardSettingsDto input = DashboardSettingsServiceTest.settings();
        when(settings.save(eq(7), eq("42"), same(input))).thenReturn(input);

        try (MockedStatic<UsersDB> users = mockStatic(UsersDB.class);
             MockedStatic<CloudToolsForCore> domains = mockStatic(CloudToolsForCore.class)) {
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(user);
            domains.when(() -> CloudToolsForCore.getRootGroupId(request)).thenReturn(42);

            controller.getSettings(request);
            assertSame(input, controller.putSettings(input, request));

            verify(settings).load(7, "42");
            verify(settings).save(7, "42", input);
            verify(user).setAdminSettings(null);
        }
    }

    @Test
    void unauthenticatedRequestsNeverAccessSettings() {
        DashboardSettingsService settings = mock(DashboardSettingsService.class);
        DashboardRestController controller = new DashboardRestController(settings, mock(DashboardRecentPagesService.class), mock(DashboardNoticeService.class), mock(DashboardLegacyDataService.class));
        MockHttpServletRequest request = new MockHttpServletRequest();
        try (MockedStatic<UsersDB> users = mockStatic(UsersDB.class)) {
            assertThrows(AccessDeniedException.class, () -> controller.getSettings(request));
            assertThrows(AccessDeniedException.class, () -> controller.putSettings(new DashboardSettingsDto(), request));
            assertThrows(AccessDeniedException.class, () -> controller.deleteSettings(request));
            verifyNoInteractions(settings);
        }
    }

    /** Reset ignores forged ownership and clears the current session cache only after success. */
    @Test
    void resetUsesAuthenticatedOwnerAndPreservesCacheOnFailure() {
        DashboardSettingsService settings = mock(DashboardSettingsService.class);
        DashboardRestController controller = new DashboardRestController(settings, mock(DashboardRecentPagesService.class), mock(DashboardNoticeService.class), mock(DashboardLegacyDataService.class));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("userId", "99");
        request.setParameter("domainId", "999");
        Identity user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(true);
        when(user.getUserId()).thenReturn(7);
        DashboardSettingsDto reset = new DashboardSettingsDto();
        when(settings.reset(7)).thenReturn(reset);

        try (MockedStatic<UsersDB> users = mockStatic(UsersDB.class)) {
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(user);

            assertSame(reset, controller.deleteSettings(request));
            verify(settings).reset(7);
            verify(settings, never()).reset(99);
            verify(user).setAdminSettings(null);

            clearInvocations(user);
            when(settings.reset(7)).thenThrow(new IllegalStateException("Database is unavailable"));
            assertThrows(IllegalStateException.class, () -> controller.deleteSettings(request));
            verify(user, never()).setAdminSettings(any());

            when(user.isAdmin()).thenReturn(false);
            clearInvocations(settings);
            assertThrows(AccessDeniedException.class, () -> controller.deleteSettings(request));
            verifyNoInteractions(settings);
        }
    }
}
