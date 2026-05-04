package sk.iway.iwcm.doc;

import jakarta.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.i18n.Prop;

/**
 *  Zobrazi dokumenty v stromovej strukture
 *
 *@Title        Interway Content Management
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.3 $
 *@created      $Date: 2003/06/05 16:05:53 $
 *@modified     $Date: 2003/06/05 16:05:53 $
 */
public class DocTreeAction
{

   public static String doTree(HttpServletRequest request)
   {
      //korenova skupina
      int group_id = Constants.getInt("rootGroupId");
      try
      {
      	 if (request.getAttribute("groupid") != null)
      	 {
      	 	group_id = Integer.parseInt((String) request.getAttribute("groupid"));
      	 }
      	 else
      	 {
      	 	group_id = Integer.parseInt(request.getParameter("groupid"));
      	 }
      }
      catch (Exception ex)
      {

      }

      Identity user = (Identity) request.getSession().getAttribute(Constants.USER_KEY);
      if(!GroupsDB.isGroupEditable(user, group_id))
      {
      	request.setAttribute("err_msg", Prop.getInstance().getText("admin.editor_dir.dontHavePermsForThisDir"));
			return "error_admin";
      }

      GroupsDB groupsDB = GroupsDB.getInstance();
      DocTreeDB docTreeDB = new DocTreeDB(group_id, groupsDB, DBPool.getDBName(request));
      request.setAttribute("navbar", docTreeDB.getNavbar());
      DocTreeDetails det = docTreeDB.getFirst();
      request.setAttribute("root_name", det.getName());
      request.setAttribute("root_url", det.getLink());

      //set data for group tree
      request.setAttribute("tree_list", docTreeDB.getDocs());

      GroupDetails group = groupsDB.getGroup(group_id);

      if (group != null)
      {
         TemplatesDB tempDB = TemplatesDB.getInstance(false);
         TemplateDetails temp = tempDB.getTemplate(group.getTempId());

         if (temp == null)
         {
            request.setAttribute("err_msg", "Požadovaný dokument neexistuje 3");
            return ("error");
         }

         request.setAttribute("doc_header", temp.getHeaderDocData());
         request.setAttribute("doc_footer", temp.getFooterDocData());
         request.setAttribute("doc_menu", temp.getMenuDocData());
         request.setAttribute("after_body", temp.getAfterBodyData());
         request.setAttribute("doc_title", det.getName());
         ShowDoc.updateCodes(request, Constants.getServletContext(), -1);
      }

      String forward="listing";
      String pForward = request.getParameter("forward");
      if (pForward!=null && pForward.endsWith(".jsp"))
      {
         forward = "/templates/"+pForward;
      }

      //ak sa jedna o archiv, bude to pekne v zelenom ;-)
      if (request.getParameter("archiv") != null)
      {
         return ("listing_archiv");
      }

      if (forward.endsWith(".jsp"))
      {
         return (forward);
      }
      else
      {
         return ("listing");
      }
   }
}
