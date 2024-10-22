package sk.iway.aceintegration;

import javax.servlet.http.HttpServletRequest;

import org.springframework.ui.Model;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.system.annotations.DefaultHandler;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

/**
 * Test APP to test character encoding in URL parameters
 */
@WebjetComponent("sk.iway.aceintegration.CharacterEncodingApp")
@Setter
@Getter
public class CharacterEncodingApp extends WebjetComponentAbstract {

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN, tab = "basic")
    private boolean jsp = false;

    @DefaultHandler
	public String view(Model model, HttpServletRequest request)
	{
        model.addAttribute("name", request.getParameter("name"));
        model.addAttribute("encoding", request.getCharacterEncoding());
        RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
        if (rb != null) {
            model.addAttribute("rb", rb);
            model.addAttribute("userId", rb.getUserId());
        }
        if (jsp) {
            return "/components/aceintegration/character-encoding-jsp";
        }
		return "/components/aceintegration/character-encoding";
	}

}
