package sk.iway.iwcm.system.spring;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Removes setup-only authentication that was persisted across an application
 * restart before the production Spring Security chain can restore it.
 */
final class PersistedSetupAuthenticationCleanupFilter extends OncePerRequestFilter {

    private static final String SETUP_AUTHORITY = "ROLE_SETUP";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (containsSetupAuthentication(session)) {
            session.invalidate();
        }

        filterChain.doFilter(request, response);
    }

    private boolean containsSetupAuthentication(HttpSession session) {
        if (session == null) return false;

        Object storedContext = session.getAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
        );
        if (storedContext instanceof SecurityContext securityContext) {
            Authentication authentication = securityContext.getAuthentication();
            return authentication != null
                && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream()
                    .anyMatch(authority -> SETUP_AUTHORITY.equals(authority.getAuthority()));
        }
        return false;
    }
}
