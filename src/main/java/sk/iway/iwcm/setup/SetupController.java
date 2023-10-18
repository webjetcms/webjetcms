package sk.iway.iwcm.setup;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/*
 * 
 */
@Controller
public class SetupController {
    
    @GetMapping("/wjerrorpages/setup/setup") 
    public String setup(Model model, HttpServletRequest request, HttpServletResponse response) {
        try {
            String lng = request.getParameter("language");
            return SetupActionsService.setupAction(model, request, response, lng);
        } catch(Exception e) {
           sk.iway.iwcm.Logger.error(e);
        }

        return null;
    }

    @PostMapping("/wjerrorpages/setup/perform-setup") 
    public String save(@ModelAttribute SetupFormBean setupForm, Model model, HttpServletRequest request, HttpServletResponse response) {
        try {
           return SetupActionsService.setupSaveAction(setupForm, model, request, response);
        } catch(Exception e) {
           sk.iway.iwcm.Logger.error(e);
        }

        return null;
    }
}