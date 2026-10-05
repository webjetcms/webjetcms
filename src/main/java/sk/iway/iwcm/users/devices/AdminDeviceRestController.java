package sk.iway.iwcm.users.devices;

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
    public DeviceEntity confirm(@PathVariable("id") String id, HttpServletRequest request) {
        return requireEvent(service.confirm(UsersDB.getCurrentUser(request), id));
    }

    /** Forgets the reported browser without terminating sessions or suppressing the warning. */
    @PostMapping("/{id}/report")
    public DeviceEntity report(@PathVariable("id") String id, HttpServletRequest request) {
        return requireEvent(service.report(UsersDB.getCurrentUser(request), id));
    }

    /**
     * Returns HTTP 503 instead of the default 500.jsp, which changes the status to 404
     * and can expose exception details to administrators. No response body is needed:
     * the client checks the HTTP status and displays its own localized error message.
     *
     * @param exception persistence failure to log with its stack trace
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public void unavailable(IllegalStateException exception) {
        Logger.error(AdminDeviceRestController.class, "Administrator device persistence is unavailable", exception);
    }

    private static DeviceEntity requireEvent(DeviceEntity event) {
        if (event == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Security event is unavailable");
        return event;
    }
}
