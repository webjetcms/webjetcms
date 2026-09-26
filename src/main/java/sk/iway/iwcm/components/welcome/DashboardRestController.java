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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.admin.layout.MenuBean;
import sk.iway.iwcm.admin.layout.DocDetailsDto;
import sk.iway.iwcm.admin.layout.MenuService;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.users.UsersDB;

/** Current-user dashboard configuration; no caller-supplied user or domain IDs are accepted. */
@RestController
@RequestMapping("/admin/rest/dashboard")
@PreAuthorize("@WebjetSecurityService.isAdmin()")
public class DashboardRestController {
    private final DashboardSettingsService settingsService;
    private final DashboardRecentPagesService recentPagesService;
    private final DashboardNoticeService noticeService;
    private final DashboardLegacyDataService legacyDataService;

    public DashboardRestController(DashboardSettingsService settingsService, DashboardRecentPagesService recentPagesService,
            DashboardNoticeService noticeService, DashboardLegacyDataService legacyDataService) {
        this.settingsService = settingsService;
        this.recentPagesService = recentPagesService;
        this.noticeService = noticeService;
        this.legacyDataService = legacyDataService;
    }

    @GetMapping("/settings")
    public DashboardSettingsDto getSettings(HttpServletRequest request) {
        return settingsService.load(currentUser(request).getUserId(), domainKey(request));
    }

    @PutMapping("/settings")
    public DashboardSettingsDto putSettings(@RequestBody DashboardSettingsDto settings, HttpServletRequest request) {
        Identity user = currentUser(request);
        DashboardSettingsDto saved = settingsService.save(user.getUserId(), domainKey(request), settings);
        user.setAdminSettings(null);
        return saved;
    }

    /** Resets the authenticated account's shared layout and all domain-specific dashboard options. */
    @DeleteMapping("/settings")
    public DashboardSettingsDto deleteSettings(HttpServletRequest request) {
        Identity user = currentUser(request);
        DashboardSettingsDto reset = settingsService.reset(user.getUserId());
        user.setAdminSettings(null);
        return reset;
    }

    @GetMapping("/menu")
    public List<MenuBean> getMenu(HttpServletRequest request) {
        currentUser(request);
        return new MenuService(request).getMenu();
    }

    /** Loads current warnings independently of content previews and stored widget preferences. */
    @GetMapping("/notices")
    public List<java.util.Map<String, Object>> getNotices(HttpServletRequest request) {
        return noticeService.load(currentUser(request), request);
    }

    /** Called only when the optional original overview is expanded. */
    @GetMapping("/legacy-data")
    public java.util.Map<String, Object> getLegacyData(HttpServletRequest request) {
        return legacyDataService.load(currentUser(request), DocDB.getDomain(request), CloudToolsForCore.getRootGroupId(request));
    }

    @GetMapping("/recent-pages")
    @PreAuthorize("@WebjetSecurityService.hasPermission('menuWebpages')")
    public List<DocDetailsDto> getRecentPages(@RequestParam(defaultValue = "6") int size, HttpServletRequest request) {
        return recentPagesService.getRecentPages(currentUser(request), DocDB.getDomain(request), size);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public java.util.Map<String, String> invalidSettings(IllegalArgumentException exception) {
        return java.util.Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public java.util.Map<String, String> unavailableSettings(IllegalStateException exception) {
        Logger.error(DashboardRestController.class, "Dashboard persistence is unavailable", exception);
        return java.util.Map.of("reason", "settings-unavailable", "error", "Dashboard settings are temporarily unavailable");
    }

    static String domainKey(HttpServletRequest request) {
        return Integer.toString(Math.max(0, CloudToolsForCore.getRootGroupId(request)));
    }

    private Identity currentUser(HttpServletRequest request) {
        Identity user = UsersDB.getCurrentUser(request);
        if (user == null || !user.isAdmin()) throw new AccessDeniedException("Administrator login is required");
        return user;
    }
}
