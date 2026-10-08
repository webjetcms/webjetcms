package sk.iway.iwcm.users.devices;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    public record Confirmation(String token, String code) { }

    /** Sends a code to the authenticated account's email, never a client-supplied address. */
    @PostMapping("/{id}/code")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void requestCode(@PathVariable("id") String id, HttpServletRequest request) {
        service.requestCode(UsersDB.getCurrentUser(request), id, request);
    }

    /** Requires exactly one email proof in addition to the authenticated account. */
    @PostMapping("/{id}/confirm")
    public DeviceEntity confirm(@PathVariable("id") String id, @RequestBody Confirmation proof, HttpServletRequest request) {
        if (proof == null || (proof.token() == null) == (proof.code() == null)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        DeviceEntity event = service.confirm(UsersDB.getCurrentUser(request), id,
            proof.code() == null ? proof.token() : proof.code(), proof.code() != null);
        if (event == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return event;
    }

    /** Blocks the reported browser and signs out its known sessions. */
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
