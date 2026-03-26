package sk.iway.iwcm.admin.settings;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.system.datatable.json.LabelValue;
import sk.iway.iwcm.users.UsersDB;

/**
 * #54513
 * Ulozi admin settings, aby sme to nemali len v LocalStoraga ale mohli to mat ulozene v DB a synchronizovane medzi kontami
 */
@RestController
@PreAuthorize("@WebjetSecurityService.isAdmin()")
public class AdminSettingsRestController {

   @PostMapping("/admin/rest/admin-settings/")
   public boolean save(@RequestBody LabelValue settings, final HttpServletRequest request) {
      Identity user = UsersDB.getCurrentUser(request);
      AdminSettingsService ass = new AdminSettingsService(user);
      boolean saveok = ass.saveSettings(settings.getLabel(), settings.getValue());
      return saveok;
   }

}
