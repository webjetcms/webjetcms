package sk.iway.iwcm.components.users.sessions;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.users.UsersDB;

/** Session listing and logout API for the current account and authorized administrator management. */
@RestController
@RequestMapping("/admin/rest")
@PreAuthorize("@WebjetSecurityService.isAdmin()")
public class SessionRestController {
    private final SessionService service;

    public SessionRestController(SessionService service) {
        this.service = service;
    }

    /** Returns fresh administrator summaries only to callers with the list permission. */
    @GetMapping("/sessions/administrators")
    @PreAuthorize("@WebjetSecurityService.hasPermission('welcomeShowLoggedAdmins')")
    public List<LoggedAdministratorDto> loggedAdministrators(HttpServletRequest request) {
        return service.loggedAdministrators(UsersDB.getCurrentUser(request));
    }

    /** Ends another session of the current account without requiring administrator-management permission. */
    @PostMapping("/sessions/logout")
    public SessionLogoutResultDto logoutSession(@RequestParam("sessionId") String sessionId, HttpServletRequest request) {
        return service.logoutSession(UsersDB.getCurrentUser(request), request.getSession().getId(), sessionId);
    }

    /** Ends another administrator's sessions; displaying the list does not grant this operation. */
    @PostMapping("/sessions/logout-administrator")
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
