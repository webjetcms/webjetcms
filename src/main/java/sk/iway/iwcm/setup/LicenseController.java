package sk.iway.iwcm.setup;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class LicenseController {
    
    @GetMapping("/wjerrorpages/setup/license") 
    public String setup(Model model, HttpServletRequest request, HttpServletResponse response) {
        try {
            //Set initial params into model (!!)
            LicenseActionService.setModel(model, request, response);
            return "/admin/setup/license";
        } catch(Exception e) {
           sk.iway.iwcm.Logger.error(e);
        }

        return null;
    }

    @PostMapping("/wjerrorpages/setup/save-license") 
    public String save(@ModelAttribute LicenseFormBean licenseForm, Model model, HttpServletRequest request, HttpServletResponse response) {
        try {
           return LicenseActionService.updateLicense(licenseForm, model, request, response);
        } catch(Exception e) {
           sk.iway.iwcm.Logger.error(e);
        }

        return null;
    }
}
