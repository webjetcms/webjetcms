package sk.iway.iwcm.components.welcome;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.JsonTools;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.admin.ThymeleafEvent;
import sk.iway.iwcm.admin.layout.MenuService;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.system.spring.events.WebjetEvent;
import sk.iway.iwcm.users.UsersDB;

/** Supplies only navigation and identity data needed to render the dashboard shell. */
@Component
public class DashboardListener {

    /** Keeps database-backed previews and security checks in independently loaded REST resources. */
    @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.admin.ThymeleafEvent' && event.source.page=='dashboard'")
    protected void setOverviewData(final WebjetEvent<ThymeleafEvent> event) {
        HttpServletRequest request = event.getSource().getRequest();
        Identity user = UsersDB.getCurrentUser(request);
        if (user == null || !user.isAdmin()) return;
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("dashboardMenu", new MenuService(request).getMenu());
            data.put("userName", user.getFirstName());
            data.put("currentDomain", DocDB.getDomain(request));
            event.getSource().getModel().addAttribute("overviewData", JsonTools.objectToJSON(data));
        } catch (JsonProcessingException exception) {
            Logger.error(DashboardListener.class, exception);
        }
    }
}
