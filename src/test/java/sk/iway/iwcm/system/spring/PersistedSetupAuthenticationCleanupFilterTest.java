package sk.iway.iwcm.system.spring;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

class PersistedSetupAuthenticationCleanupFilterTest {

    private final PersistedSetupAuthenticationCleanupFilter filter =
        new PersistedSetupAuthenticationCleanupFilter();

    @Test
    void invalidatesPersistedSetupAuthoritySession() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = createAuthenticatedSession(request, "setup", "ROLE_SETUP");
        AtomicBoolean chainInvoked = new AtomicBoolean();

        filter.doFilter(request, new MockHttpServletResponse(),
            (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertTrue(session.isInvalid());
        assertTrue(chainInvoked.get());
    }

    @Test
    void preservesProductionAuthenticationEvenWhenUsernameIsSetup() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = createAuthenticatedSession(request, "setup", "ROLE_Group_admin");
        Object storedContext = session.getAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
        );

        filter.doFilter(request, new MockHttpServletResponse(),
            (servletRequest, servletResponse) -> { });

        assertFalse(session.isInvalid());
        assertSame(storedContext, session.getAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
        ));
    }

    @Test
    void doesNotCreateSessionWhenRequestHasNone() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        AtomicBoolean chainInvoked = new AtomicBoolean();

        filter.doFilter(request, new MockHttpServletResponse(),
            (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertNull(request.getSession(false));
        assertTrue(chainInvoked.get());
    }

    private MockHttpSession createAuthenticatedSession(MockHttpServletRequest request,
            String username, String... authorities) {
        SecurityContext securityContext = new SecurityContextImpl(
            UsernamePasswordAuthenticationToken.authenticated(
                username,
                null,
                AuthorityUtils.createAuthorityList(authorities)
            )
        );
        MockHttpSession session = (MockHttpSession) request.getSession();
        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            securityContext
        );
        return session;
    }
}
