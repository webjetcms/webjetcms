package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.SetCharacterEncodingFilter;

/** Verifies server-owned domain and explicit LIVE operation context for create/list API. */
class BranchRestControllerTest {
    @Test
    void createsWithoutChangingSelectionAndListsOnlyServerDomain() {
        BranchService service = mock(BranchService.class);
        BranchAccess access = mock(BranchAccess.class);
        BranchRestController controller = new BranchRestController(service, access);
        MockHttpServletRequest request = new MockHttpServletRequest();
        BranchContext branch = new BranchContext(12, "a.example.sk", 7, BranchContext.Mode.ADMIN);
        request.setAttribute(BranchContexts.ATTRIBUTE, branch);
        Identity user = mock(Identity.class);
        when(user.getUserId()).thenReturn(7);
        when(access.requireUser(request)).thenReturn(user);
        when(access.requireDomain(request, user)).thenReturn("a.example.sk");
        BranchEntity entity = new BranchEntity();
        entity.setId(42L);
        entity.setName("Campaign");
        entity.setDomainName("a.example.sk");
        entity.setState("DRAFT");
        when(service.create("a.example.sk", "Campaign", "Description", 7)).thenAnswer(invocation -> {
            assertEquals(BranchContext.Mode.LIVE, BranchContexts.current(request).mode());
            return entity;
        });
        when(service.list("a.example.sk", false)).thenAnswer(invocation -> {
            assertEquals(BranchContext.Mode.LIVE, BranchContexts.current(request).mode());
            return List.of(entity);
        });
        try (var requests = mockStatic(SetCharacterEncodingFilter.class)) {
            var response = controller.create(new BranchRestController.Create(" Campaign ", "Description", "a.example.sk"), request);
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertEquals("/admin/rest/branches/42", response.getHeaders().getLocation().toString());
            assertEquals(42L, response.getBody().id());
            assertSame(branch, request.getAttribute(BranchContexts.ATTRIBUTE));
            assertEquals(1, controller.list(false, request).size());
            assertSame(branch, request.getAttribute(BranchContexts.ATTRIBUTE));
            verify(access).requireExpectedDomain("a.example.sk", "a.example.sk");
            verify(access).requireCsrf(request);
        }
    }

    @Test
    void unavailableStorageReturnsConcreteConflictAndRestoresContext() {
        BranchService service = mock(BranchService.class);
        BranchAccess access = mock(BranchAccess.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        Identity user = mock(Identity.class);
        when(user.getUserId()).thenReturn(7);
        when(access.requireUser(request)).thenReturn(user);
        when(access.requireDomain(request,user)).thenReturn("a.example.sk");
        when(service.create("a.example.sk","Campaign",null,7)).thenThrow(new BranchService.UnavailableException("InnoDB required: groups",new java.sql.SQLException()));
        try (var requests = mockStatic(SetCharacterEncodingFilter.class)) {
            ResponseStatusException failure = assertThrows(ResponseStatusException.class, () -> new BranchRestController(service,access).create(
                new BranchRestController.Create("Campaign",null,"a.example.sk"),request));
            assertEquals(HttpStatus.CONFLICT,failure.getStatusCode());
            assertTrue(failure.getReason().contains("InnoDB required: groups"));
            assertNull(request.getAttribute(BranchContexts.ATTRIBUTE));
        }
    }

    @Test
    void invalidNameAndStaleDomainDoNotCreateAnything() {
        BranchService service = mock(BranchService.class);
        BranchAccess access = mock(BranchAccess.class);
        BranchRestController controller = new BranchRestController(service, access);
        MockHttpServletRequest request = new MockHttpServletRequest();
        Identity user = mock(Identity.class);
        when(access.requireUser(request)).thenReturn(user);
        when(access.requireDomain(request, user)).thenReturn("a.example.sk");
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> controller.create(new BranchRestController.Create(" ", null, "a.example.sk"), request)).getStatusCode());
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT)).when(access).requireExpectedDomain("a.example.sk", "b.example.sk");
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> controller.create(new BranchRestController.Create("Campaign", null, "b.example.sk"), request)).getStatusCode());
        verifyNoInteractions(service);
    }
}
