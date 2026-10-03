package sk.iway.iwcm.system.audit.rest;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.system.audit.jpa.AuditLogEntity;
import sk.iway.iwcm.system.audit.jpa.AuditRepository;
import sk.iway.iwcm.users.UsersDB;

/** Provides only the signed-in user's login audit records without requiring audit permission. */
@RestController
@RequestMapping("/rest/audit/my-login-history")
@PreAuthorize("@WebjetSecurityService.isLogged()")
public class MyLoginHistoryRestController {
    private final AuditRepository repository;

    public MyLoginHistoryRestController(AuditRepository repository) {
        this.repository = repository;
    }

    /**
     * Reads login events from the last thirty days, deriving ownership, period and ordering on the server.
     *
     * @param request request containing the authenticated user's session
     * @param page zero-based page, clamped to zero for negative values
     * @return up to twenty login audit records in descending creation order
     * @throws AccessDeniedException if no authenticated user is present
     */
    @GetMapping
    public Page<AuditLogEntity> history(HttpServletRequest request, @RequestParam(value = "page", defaultValue = "0") int page) {
        Identity user = UsersDB.getCurrentUser(request);
        if (user == null || user.getUserId() <= 0) throw new AccessDeniedException("Login is required");
        Instant end = Instant.now();
        return repository.findAllByLogTypeAndUserIdAndCreateDateBetween(Adminlog.TYPE_USER_LOGON, user.getUserId(),
            Timestamp.from(end.minus(30, ChronoUnit.DAYS)), Timestamp.from(end),
            PageRequest.of(Math.max(0, page), 20, Sort.by(Sort.Direction.DESC, "createDate", "id")));
    }
}
