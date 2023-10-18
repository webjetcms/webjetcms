package sk.iway.iwcm.admin;

import javax.servlet.http.HttpServletRequest;

import org.springframework.ui.ModelMap;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Drzi data pre udalost vyvolanu pri poziadavke na zobrazenie stranky v admin casti /admin/v9/
 */
@Getter
@Setter
@AllArgsConstructor
public class ThymeleafEvent {
    String page;
    String subpage;
    ModelMap model;
    RedirectAttributes redirectAttributes;
    HttpServletRequest request;
}
