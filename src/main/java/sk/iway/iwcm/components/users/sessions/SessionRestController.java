package sk.iway.iwcm.components.users.sessions;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.system.audit.jpa.AuditLogEntity;
import sk.iway.iwcm.system.audit.jpa.AuditRepository;
import sk.iway.iwcm.users.UsersDB;

/** Session listing, personal login history and logout API for the active-session dialog. */
@RestController
@PreAuthorize("@WebjetSecurityService.isAdmin()")
public class SessionRestController {
    private final SessionService service;
    private final AuditRepository auditRepository;

    public SessionRestController(SessionService service, AuditRepository auditRepository) {
        this.service = service;
        this.auditRepository = auditRepository;
    }

    /** Returns fresh administrator summaries only to callers with the list permission. */
    @GetMapping("/admin/rest/sessions/administrators")
    @PreAuthorize("@WebjetSecurityService.hasPermission('welcomeShowLoggedAdmins')")
    public List<LoggedAdministratorDto> loggedAdministrators(HttpServletRequest request) {
        return service.loggedAdministrators(UsersDB.getCurrentUser(request));
    }

    /**
     * Reads login events from the last thirty days, deriving ownership, period and ordering on the server.
     * Available to signed-in administrators without requiring audit permission.
     *
     * @param request request containing the authenticated user's session
     * @param page zero-based page, clamped to zero for negative values
     * @return up to twenty login audit records in descending creation order
     * @throws AccessDeniedException if no authenticated user is present
     */
    @GetMapping("/admin/rest/sessions/login-history")
    @PreAuthorize("@WebjetSecurityService.isLogged()")
    public Page<AuditLogEntity> history(HttpServletRequest request, @RequestParam(value = "page", defaultValue = "0") int page) {
        Identity user = UsersDB.getCurrentUser(request);
        if (user == null || user.getUserId() <= 0) throw new AccessDeniedException("Login is required");
        Instant end = Instant.now();
        return auditRepository.findAllByLogTypeAndUserIdAndCreateDateBetween(Adminlog.TYPE_USER_LOGON, user.getUserId(),
            Timestamp.from(end.minus(30, ChronoUnit.DAYS)), Timestamp.from(end),
            PageRequest.of(Math.max(0, page), 20, Sort.by(Sort.Direction.DESC, "createDate", "id")));
    }

    /** Ends another session of the current account without requiring administrator-management permission. */
    @PostMapping("/admin/rest/sessions/logout")
    public SessionLogoutResultDto logoutSession(@RequestParam("sessionId") String sessionId, HttpServletRequest request) {
        return service.logoutSession(UsersDB.getCurrentUser(request), request.getSession().getId(), sessionId);
    }

    /** Ends another administrator's sessions; displaying the list does not grant this operation. */
    @PostMapping("/admin/rest/sessions/logout-administrator")
    @PreAuthorize("@WebjetSecurityService.hasPermission('users.edit_admins')")
    public SessionLogoutResultDto logoutAdministrator(@RequestParam("userId") int userId, HttpServletRequest request) {
        return service.logoutAdministrator(UsersDB.getCurrentUser(request), userId);
    }

    /** Reports invalid target selection as a bad request rather than a server failure. */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalidTarget(IllegalArgumentException exception) {
        return Map.of("error", exception.getMessage());
    }
}
