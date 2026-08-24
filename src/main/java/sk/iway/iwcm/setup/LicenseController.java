package sk.iway.iwcm.setup;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.system.spring.WebjetBootstrapMode;

@Controller
@ConditionalOnProperty(
    name = WebjetBootstrapMode.PROPERTY_NAME,
    havingValue = WebjetBootstrapMode.LICENSE_RECOVERY_VALUE
)
public class LicenseController {

    @GetMapping("/wjerrorpages/setup/license")
    public String setup(Model model, HttpServletRequest request, HttpServletResponse response) {
        try {
            if (SetupCompletionState.rejectIfCompleted(request, response)) return null;
            //Set initial params into model (!!)
            if (InitServlet.isValid()==false) {
                LicenseActionService.setModel(model, request, response);
                return "/admin/setup/license";
            }
        } catch(Exception e) {
           sk.iway.iwcm.Logger.error(e);
        }

        return null;
    }

    @PostMapping("/wjerrorpages/setup/save-license")
    public String save(@ModelAttribute LicenseFormBean licenseForm, Model model, HttpServletRequest request, HttpServletResponse response) {
        boolean started = false;
        boolean completed = false;
        try {
            started = SetupCompletionState.tryStart(request, response);
            if (started == false) return null;
            if (InitServlet.isValid()==false) {
                String view = LicenseActionService.updateLicense(licenseForm, model, request, response);
                completed = SetupCompletionState.isCompleted(request);
                return view;
            }
        } catch(Exception e) {
           sk.iway.iwcm.Logger.error(e);
        } finally {
            if (started && completed == false) SetupCompletionState.resetAfterFailure(request);
        }

        return null;
    }
}
