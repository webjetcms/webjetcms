package sk.iway.iwcm.users.devices;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.users.UsersDB;

/** Verifies that security-event mutations derive ownership from the authenticated request. */
class AdminDeviceRestControllerTest {
    @Test
    void ignoresSubmittedOwnershipAndReturnsTheSavedEvent() {
        var request = new MockHttpServletRequest();
        request.setParameter("userId", "999");
        request.setParameter("domainId", "999");
        var user = mock(Identity.class);
        var service = mock(AdminDeviceService.class);
        var controller = new AdminDeviceRestController(service);
        var event = new DeviceEntity();
        event.setId(42L);
        String id = event.getId().toString();
        when(service.confirm(user, id, "proof", false)).thenReturn(event);
        when(service.report(user, id)).thenReturn(event);
        try (var users = mockStatic(UsersDB.class)) {
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(user);
            assertSame(event, controller.confirm(id, new AdminDeviceRestController.Confirmation("proof", null), request));
            assertSame(event, controller.report(id, request));
            verify(service).confirm(user, id, "proof", false);
            verify(service).report(user, id);
            verifyNoMoreInteractions(service);
        }
    }

    /** Invalid proof is a generic bad request; unavailable report details remain not found. */
    @Test
    void returnsNotFoundWhenServiceReturnsNoEvent() {
        var controller = new AdminDeviceRestController(mock(AdminDeviceService.class));
        var request = new MockHttpServletRequest();
        try (var users = mockStatic(UsersDB.class)) {
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(mock(Identity.class));
            var confirm = assertThrows(ResponseStatusException.class, () -> controller.confirm("42", new AdminDeviceRestController.Confirmation(null, "123456"), request));
            var report = assertThrows(ResponseStatusException.class, () -> controller.report("42", request));
            assertEquals(HttpStatus.BAD_REQUEST, confirm.getStatusCode());
            assertEquals(HttpStatus.NOT_FOUND, report.getStatusCode());
        }
    }

    /** The former ID-only endpoint and ambiguous payloads cannot confirm a device. */
    @Test
    void rejectsMissingOrAmbiguousProofBeforeCallingService() {
        var service = mock(AdminDeviceService.class);
        var controller = new AdminDeviceRestController(service);
        var request = new MockHttpServletRequest();
        for (var proof : new AdminDeviceRestController.Confirmation[] { null,
                new AdminDeviceRestController.Confirmation(null, null),
                new AdminDeviceRestController.Confirmation("token", "123456") }) {
            var error = assertThrows(ResponseStatusException.class, () -> controller.confirm("42", proof, request));
            assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        }
        verifyNoInteractions(service);
    }

    @Test
    void unauthenticatedRequestCannotMutateAnEventEvenWithoutMethodSecurityProxy() {
        var service = new AdminDeviceService(mock(DeviceService.class));
        var controller = new AdminDeviceRestController(service);
        var request = new MockHttpServletRequest();
        try (var users = mockStatic(UsersDB.class)) {
            assertThrows(AccessDeniedException.class, () -> controller.confirm("autotest-event", new AdminDeviceRestController.Confirmation("proof", null), request));
            assertThrows(AccessDeniedException.class, () -> controller.report("autotest-event", request));
        }
    }
}
