package sk.iway.iwcm.rest;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.users.UsersDB;


/**
 *  DocRestController.java
 *  <br>
 *  <br>Returns DocDetails object of requested document(web-page)
 *
 *Title        webjet8
 *Company      Interway s.r.o. (www.interway.sk)
 *Copyright    Interway s.r.o. (c) 2001-2017
 *@author       $Author: jeeff rzapach $
 *@version      $Revision: 1.3 $
 *created      Date: 16.1.2017 14:11:45
 *modified     $Date: 2004/08/16 06:26:11 $
 */

@RestController
public class DocRestController extends sk.iway.iwcm.rest.RestController
{
	/**
	 * Returns DocDetails object for given docId or virtualPath. If virtualPath contains .html it has to be replaced to -html
	 * <br>
	 * <br>Examples:
	 * <br>
	 * <br>1)
	 * <br>URL - /rest/documents/50124
	 * <br>2)
	 * <br>URL - /rest/documents/en/gallery/kitchen
	 * <br>3)
	 * <br>URL - /rest/documents/en/home-html
	 *
	 * @param request - http request
	 * @param param docId, or virtualPath
	 * @return DocDetails object
	 */
	@RequestMapping(path={"/rest/documents/{param}/**"}, method=RequestMethod.GET)
	public DocDetails getDoc(HttpServletRequest request, @PathVariable String param)
	{		
		if(!isIpAddressAllowed(request))
			return null;
		
		int docId = getDocId(param, (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE), request);
		
		DocDetails result = DocDB.getInstance().getDoc(docId);
		if (result != null)
		{
			if (result.isAvailable()==false) return null;

			GroupDetails group = GroupsDB.getInstance().getGroup(result.getGroupId());
			if (group == null || group.isInternal()) return null;

			//kontrola prav
			Identity user = UsersDB.getCurrentUser(request);
			if (DocDB.canAccess(result, user, true) == false)
			{
				return null;
			}
		}
		return result;
	}
	
	/**
	 * param je bud docId, alebo prva cast url stranky
	 * 
	 * @param param - URL adresa stranky alebo docId
	 * @param calledUrl - cele volane URL rest sluzby
	 * @return - normovane docId
	 */
	private int getDocId(String param, String calledUrl, HttpServletRequest request)
	{
		int docId;
		String restOfTheUrl = calledUrl.substring( ("/rest/documents/"+param).length() );
		//String restOfTheUrl = calledUrl.replace("/rest/documents/"+param, "");
		
		if(Tools.isEmpty(restOfTheUrl) || restOfTheUrl.equals("/"))
		{
			docId = Tools.getIntValue(param, -1);
		}
		else
		{
			String docUrl = "/"+param+restOfTheUrl;
			if(!docUrl.contains("-html") && docUrl.charAt(docUrl.length()-1)!='/')
				docUrl += '/';
			docId = DocDB.getDocIdFromURL(docUrl.replace("-html", ".html"), DocDB.getDomain(request));
		}
		
		return docId;
	}
}
