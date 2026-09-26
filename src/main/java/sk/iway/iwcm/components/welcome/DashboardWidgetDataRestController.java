package sk.iway.iwcm.components.welcome;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.users.UsersDB;

/** Read-only, authorized projections for dashboard previews. */
@RestController
@RequestMapping("/admin/rest/dashboard/data")
@PreAuthorize("@WebjetSecurityService.isAdmin()")
public class DashboardWidgetDataRestController {
    private final DashboardWidgetDataService service;

    public DashboardWidgetDataRestController(DashboardWidgetDataService service) {
        this.service = service;
    }

    @GetMapping("/{type}")
    public Map<String, Object> getData(@PathVariable String type,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "sessions") String metric,
            @RequestParam(required = false) String formName,
            @RequestParam(required = false) Long campaignId,
            HttpServletRequest request) {
        Identity user = UsersDB.getCurrentUser(request);
        if (user == null || !user.isAdmin()) throw new AccessDeniedException("Administrator login is required");
        return service.load(type, days, metric, formName, campaignId, user, DocDB.getDomain(request), request.getSession().getId());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalidInput(IllegalArgumentException exception) {
        return Map.of("reason", "invalid-configuration");
    }

    @ExceptionHandler(DashboardWidgetDataService.UnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, String> unavailable(DashboardWidgetDataService.UnavailableException exception) {
        return Map.of("reason", exception.getMessage());
    }

    @ExceptionHandler({IllegalStateException.class, DataAccessException.class})
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, String> failedData(RuntimeException exception) {
        Logger.error(DashboardWidgetDataRestController.class, exception);
        return Map.of("reason", "data-unavailable");
    }
}
