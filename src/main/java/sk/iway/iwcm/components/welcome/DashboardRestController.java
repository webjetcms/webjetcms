package sk.iway.iwcm.components.welcome;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.admin.layout.MenuBean;
import sk.iway.iwcm.admin.layout.MenuService;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.users.UsersDB;

/**
 * Exposes dashboard settings and menu destinations.
 * The settings owner and active domain are resolved from the request.
 */
@RestController
@RequestMapping("/admin/rest/dashboard")
@PreAuthorize("@WebjetSecurityService.isAdmin()")
public class DashboardRestController {
    private final DashboardSettingsService settingsService;

    public DashboardRestController(DashboardSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /**
     * Saves the shared layout and active-domain options, then clears the user's cached administration settings.
     *
     * @param settings dashboard settings to validate and persist
     * @param request request identifying the administrator and active domain
     * @return saved settings with the layout and shortcuts marked as configured
     */
    @PutMapping("/settings")
    public DashboardSettingsDto putSettings(@RequestBody DashboardSettingsDto settings, HttpServletRequest request) {
        Identity user = currentUser(request);
        DashboardSettingsDto saved = settingsService.save(user.getUserId(), domainKey(request), settings);
        user.setAdminSettings(null);
        return saved;
    }

    /**
     * Clears widget preferences across all domains while retaining shortcuts and their migration state.
     * The returned layout is marked as unconfigured so the client can supply default widgets.
     *
     * @param request request identifying the administrator whose settings are reset
     * @return retained shortcuts and configuration flags after the reset
     */
    @DeleteMapping("/settings")
    public DashboardSettingsDto deleteSettings(HttpServletRequest request) {
        Identity user = currentUser(request);
        DashboardSettingsDto reset = settingsService.reset(user.getUserId());
        user.setAdminSettings(null);
        return reset;
    }

    /**
     * Atomically installs a supplied layout while retaining the account's configured shortcuts.
     *
     * @param layout replacement widgets and active-domain options, including default shortcuts for an unconfigured account
     * @param request request identifying the administrator and active domain
     * @return persisted layout combined with the retained shortcuts
     */
    @PutMapping("/settings/reset")
    public DashboardSettingsDto resetSettings(@RequestBody DashboardSettingsDto layout, HttpServletRequest request) {
        Identity user = currentUser(request);
        DashboardSettingsDto reset = settingsService.reset(user.getUserId(), domainKey(request), layout);
        user.setAdminSettings(null);
        return reset;
    }

    @GetMapping("/menu")
    public List<MenuBean> getMenu(HttpServletRequest request) {
        currentUser(request);
        return new MenuService(request).getMenu();
    }

    /**
     * Converts a settings validation failure into the error body of a bad-request response.
     *
     * @param exception validation failure containing a message for the client
     * @return response body containing the validation message
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public java.util.Map<String, String> invalidSettings(IllegalArgumentException exception) {
        return java.util.Map.of("error", exception.getMessage());
    }

    /**
     * Logs a persistence failure and returns a service-unavailable error without exposing internal details.
     *
     * @param exception persistence failure to log
     * @return response body with a stable reason code and a generic error message
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public java.util.Map<String, String> unavailableSettings(IllegalStateException exception) {
        Logger.error(DashboardRestController.class, "Dashboard persistence is unavailable", exception);
        return java.util.Map.of("reason", "settings-unavailable", "error", "Dashboard settings are temporarily unavailable");
    }

    /**
     * Converts the active domain's root group ID to a non-negative settings key.
     *
     * @param request request used to resolve the active domain's root group
     * @return decimal root group ID, or {@code "0"} when the resolved ID is negative
     */
    static String domainKey(HttpServletRequest request) {
        return Integer.toString(Math.max(0, CloudToolsForCore.getRootGroupId(request)));
    }

    private Identity currentUser(HttpServletRequest request) {
        Identity user = UsersDB.getCurrentUser(request);
        if (user == null || !user.isAdmin()) throw new AccessDeniedException("Administrator login is required");
        return user;
    }
}
