package sk.iway.iwcm.setup;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import sk.iway.iwcm.system.spring.WebjetBootstrapMode;

/*
 * 
 */
@Controller
@ConditionalOnProperty(name = WebjetBootstrapMode.PROPERTY_NAME, havingValue = WebjetBootstrapMode.SETUP_VALUE)
public class SetupController {
    
    @GetMapping("/wjerrorpages/setup/setup") 
    public String setup(Model model, HttpServletRequest request, HttpServletResponse response) {
        try {
            if (SetupCompletionState.rejectIfCompleted(request, response)) return null;
            String lng = request.getParameter("language");
            return SetupActionsService.setupAction(model, request, response, lng);
        } catch(Exception e) {
           sk.iway.iwcm.Logger.error(e);
        }

        return null;
    }

    @PostMapping("/wjerrorpages/setup/perform-setup") 
    public String save(@ModelAttribute SetupFormBean setupForm, Model model, HttpServletRequest request, HttpServletResponse response) {
        boolean started = false;
        boolean completed = false;
        try {
           started = SetupCompletionState.tryStart(request, response);
           if (started == false) return null;
           String view = SetupActionsService.setupSaveAction(setupForm, model, request, response);
           completed = SetupCompletionState.isCompleted(request);
           return view;
        } catch(Exception e) {
           sk.iway.iwcm.Logger.error(e);
        } finally {
           if (started && completed == false) SetupCompletionState.resetAfterFailure(request);
        }

        return null;
    }
}
