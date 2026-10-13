package sk.iway.iwcm.admin.layout;

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
     * Uses the same cluster records and device page totals as the personal sessions dialog.
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
        Long deviceCount = null;
        int newLoginCount = 0;
        if (Constants.getBoolean("adminNewDeviceDetectionEnabled")) {
            deviceCount = deviceService.getDevices(user, 0).getTotalElements();
            newLoginCount = deviceService.getActiveEvents(user).size();
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(new Status(sessionCount, deviceCount, twoFactorEnabled, newLoginCount));
    }

    /** Account summary; device count is absent when browser recognition is disabled. */
    public record Status(int sessionCount, Long deviceCount, boolean twoFactorEnabled, int newLoginCount) { }
}
