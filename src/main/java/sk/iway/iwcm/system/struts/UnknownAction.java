package sk.iway.iwcm.system.struts;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.ShowDoc;

/**
 * It take unknow Action and change path postfix from .doc to .struts, because we probably allready delete maping from xml struts confing file and replace it with Spring mapping.
 * Spring mapping use .struts postfix because .doc postrfix path could not be catched in Spring.
 */
public class UnknownAction extends Action {

	@Override
	public ActionForward execute(ActionMapping mapping,
	         ActionForm form,
	         HttpServletRequest request,
	         HttpServletResponse response) throws IOException, ServletException {

	  String path = PathFilter.getOrigPath(request);
	  String doShowdocAction = request.getParameter("doShowdocAction");
	  if (Tools.isNotEmpty(doShowdocAction) && ShowDoc.isDoShowdocActionAllowed(doShowdocAction)) path = doShowdocAction;
	  if (path==null) path = request.getRequestURI();

	  request.getRequestDispatcher(Tools.replace(path, ".do", ".struts")).forward(request, response);
	  return null;

   }
}