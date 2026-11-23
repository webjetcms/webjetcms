package sk.iway.iwcm.users.devices;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.system.stripes.CSRF;
import sk.iway.iwcm.users.UsersDB;

/** Accepts optional location hints only for the caller's login and current administrator session. */
@RestController
public class AdminLoginLocationController {
    private final DeviceService devices;

    public AdminLoginLocationController(DeviceService devices) {
        this.devices = devices;
    }

    @PostMapping("/admin/logon/location/")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void prepare(@RequestBody AdminLoginLocation.Input input, HttpServletRequest request) {
        HttpSession session = checkedSession(request);
        if (UsersDB.getCurrentUser(request) == null) AdminLoginLocation.prepare(session, input);
    }

    /** Fills a missing hint without allowing the browser to choose its target account or device. */
    @PostMapping("/admin/rest/security/login-location")
    @PreAuthorize("@WebjetSecurityService.isAdmin()")
    public Result complete(@RequestBody AdminLoginLocation.Input input, HttpServletRequest request) {
        HttpSession session = checkedSession(request);
        if (!AdminLoginLocation.isEnabled()) return new Result(null);
        String location = AdminLoginLocation.getLocation(session);
        if (location != null) return new Result(location);
        location = AdminLoginLocation.validate(input);
        if (location == null) return new Result(null);
        AdminLoginLocation.setLocation(session, location);
        Object deviceId = session.getAttribute(AdminDeviceService.SESSION_DEVICE_ID);
        Object loginTime = session.getAttribute(AdminLoginLocation.DEVICE_LOGIN_TIME);
        if (deviceId instanceof Long id && loginTime instanceof Long time) {
            try {
                devices.completeLocation(UsersDB.getCurrentUser(request).getUserId(), id, time, location);
            } catch (IllegalStateException exception) {
                Logger.error(AdminLoginLocationController.class, "Cannot complete administrator device location", exception);
            }
        }
        return new Result(location);
    }

    public record Result(String location) { }

    private static HttpSession checkedSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || !CSRF.verifyTokenAjax(session, request.getHeader("X-CSRF-Token")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return session;
    }
}
