package sk.iway.iwcm.admin.layout;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.users.userdetail.UserDetailsRepository;
import sk.iway.iwcm.stat.SessionClusterService;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.iwcm.users.devices.AdminDeviceService;

/** Loads account status only when the administrator opens the user menu. */
@RestController
@PreAuthorize("@WebjetSecurityService.isAdmin()")
public class UserMenuRestController {
    private final UserDetailsRepository userDetailsRepository;
    private final AdminDeviceService deviceService;

    public UserMenuRestController(UserDetailsRepository userDetailsRepository, AdminDeviceService deviceService) {
        this.userDetailsRepository = userDetailsRepository;
        this.deviceService = deviceService;
    }

    /**
     * Uses the same cluster records and outstanding device notices as the dashboard.
     *
     * @param request request identifying the signed-in administrator
     * @return uncached counts and second-factor status without session IDs or credentials
     */
    @GetMapping("/admin/rest/user-menu")
    public ResponseEntity<Status> status(HttpServletRequest request) {
        Identity user = UsersDB.getCurrentUser(request);
        if (user == null || !user.isAdmin()) throw new AccessDeniedException("Administrator login is required");

        int sessionCount = 0;
        for (var node : SessionClusterService.getUserSessionsAllNodes(user.getUserId())) {
            sessionCount += node.path("userSessions").size();
        }
        boolean twoFactorEnabled = Constants.getBoolean("2factorAuthEnabled")
            && Tools.isNotEmpty(userDetailsRepository.getMobileDeviceByUserId((long) user.getUserId()));
        List<LoginNotice> newLogins = !Constants.getBoolean("adminNewDeviceDetectionEnabled") ? List.of() : deviceService.getActiveEvents(user).stream()
            .map(device -> new LoginNotice(device.getBrowserName(), device.getBrowserVersion(), device.getOperatingSystem(),
                device.getLocation(), device.getLastSeen().toEpochMilli()))
            .toList();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(new Status(sessionCount, twoFactorEnabled, newLogins));
    }

    /** Account summary containing only display data for outstanding login notices. */
    public record Status(int sessionCount, boolean twoFactorEnabled, List<LoginNotice> newLogins) { }

    /** Latest successful login details, excluding recognition and verification secrets. */
    public record LoginNotice(String browserName, String browserVersion, String operatingSystem, String location, long lastSeen) { }
}
