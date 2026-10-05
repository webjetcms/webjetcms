package sk.iway.iwcm.users.devices;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.users.UsersDB;

/** Account-owned security-event actions; reads arrive with the authenticated overview page. */
@RestController
@RequestMapping("/admin/rest/security/login-events")
@PreAuthorize("@WebjetSecurityService.isAdmin()")
public class AdminDeviceRestController {
    private final AdminDeviceService service;

    public AdminDeviceRestController(AdminDeviceService service) {
        this.service = service;
    }

    /** Confirms a login using the request's account, never a submitted user identifier. */
    @PostMapping("/{id}/confirm")
    public LoginEvent confirm(@PathVariable("id") String id, HttpServletRequest request) {
        return requireEvent(service.confirm(UsersDB.getCurrentUser(request), id));
    }

    /** Forgets the reported browser without terminating sessions or suppressing the warning. */
    @PostMapping("/{id}/report")
    public LoginEvent report(@PathVariable("id") String id, HttpServletRequest request) {
        return requireEvent(service.report(UsersDB.getCurrentUser(request), id));
    }

    /** Hides storage details while allowing the client to retain its confirmed state on failure. */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, String> unavailable(IllegalStateException exception) {
        Logger.error(AdminDeviceRestController.class, "Administrator device persistence is unavailable (" + exception.getClass().getSimpleName() + ")");
        return Map.of("error", "Security events are temporarily unavailable");
    }

    private static LoginEvent requireEvent(LoginEvent event) {
        if (event == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Security event is unavailable");
        return event;
    }
}
