<%@page import="java.util.List"%><%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="java.io.IOException"%>
<%@page import="sk.iway.iwcm.editor.EditorForm"%>
<%@page import="sk.iway.iwcm.editor.EditorDB"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.doc.*,sk.iway.iwcm.*,java.util.*" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/datetime.tld" prefix="dt" %>
<%@ include file="layout_top.jsp" %>
<%!
public void regenerateUrl(int rootGroupId, Identity user, JspWriter out, HttpServletRequest request, Prop prop) throws IOException
{
	HttpSession session = request.getSession();
	String spacer = "&nbsp;&nbsp;&nbsp;";

	//ziskaj zoznam stranok v adresari
	List docs = DocDB.getInstance().getBasicDocDetailsByGroup(rootGroupId, -1);
	Iterator iter = docs.iterator();
	DocDetails doc;
	EditorForm ef;
	while (iter.hasNext())
	{
		doc = (DocDetails)iter.next();
		ef = EditorDB.getEditorForm(request, doc.getDocId(), -1, rootGroupId);
		out.println("<strong>"+ef.getTitle()+"</strong> [docid:"+ef.getDocId()+"] - "+ef.getVirtualPath());

		if (ef.getVirtualPath().contains("*"))
		{
		   out.println(" skipping (contains *)<br/>");
		   continue;
		}

		ef.setVirtualPath("");
		//nastav aktualneho usera
		ef.setAuthorId(user.getUserId());
		ef.setPublish("1");

		EditorDB.saveEditorForm(ef, request);

		out.println(" -> " + ef.getVirtualPath()+"<br>");

		if (session.getAttribute("pageSavedToPublic")!=null)
		{
			session.removeAttribute("pageSavedToPublic");
		}
		if (session.getAttribute("pageSaved")!=null)
		{
			session.removeAttribute("pageSaved");
		}
		if (session.getAttribute("approveByUsers")!=null)
		{
			out.println(spacer+"<b>"+prop.getText("editor.approveRequestGet")+"</b>: "+(String)session.getAttribute("approveByUsers")+"<br>");
			session.removeAttribute("approveByUsers");
		}
		if (session.getAttribute("pagePublishDate")!=null)
		{
			out.println(spacer+prop.getText("editor.publish.pagesaved")+(String)session.getAttribute("pagePublishDate")+"<br>");
			session.removeAttribute("pagePublishDate");
		}
		if (session.getAttribute("updatedDocs")!=null)
		{
			//request.setAttribute("updatedDocs", session.getAttribute("updatedDocs"));
			List updatedDocs = (ArrayList)session.getAttribute("updatedDocs");
			out.println(spacer+prop.getText("editor.updatedDocs")+"<br>");
			DocDetails doc2;
			Iterator iter2 = updatedDocs.iterator();
			while (iter2.hasNext())
			{
				doc2 = (DocDetails)iter2.next();
				out.println(spacer+"<a href='/showdoc.do?docid="+doc2.getDocId()+"' target='_blank'>"+doc2.getTitle()+"</a><br>");
			}
			session.removeAttribute("updatedDocs");
		}
		if (session.getAttribute("allreadyUsedVirtualPathDocId")!=null)
		{
			out.println(spacer+"<span class='error'>"+prop.getText("editor.virtual_path_allready_used_in_doc")+": "+session.getAttribute("allreadyUsedVirtualPathDocId")+"</span><br>");
			session.removeAttribute("allreadyUsedVirtualPathDocId");
		}

		out.flush();
	}

	out.flush();

	//rekurzivne sa zavolaj na podadresare
	List subGroups = GroupsDB.getInstance().getGroups(rootGroupId);
	iter = subGroups.iterator();
	GroupDetails group;
	while (iter.hasNext())
	{
		group = (GroupDetails)iter.next();
		regenerateUrl(group.getGroupId(), user, out, request, prop);
	}
	out.flush();
}
%>
<div id="waitDiv" style="text-align:center; color: red;">
   <iwcm:text key="divpopup-blank.wait_please"/><br/>
   <img src="/admin/images/loading-anim.gif">
</div>
<%
out.flush();

Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
Prop prop = Prop.getInstance(Constants.getServletContext(), request);

int rootGroupId = Tools.getIntValue(Tools.getRequestParameter(request, "rootGroupId"), -1);
if (rootGroupId > 0)
{
	//request.setAttribute("dontRefreshDocDB", "true");
	regenerateUrl(rootGroupId, user, out, request, prop);
	DocDB.getInstance(true);
	GroupsDB.getInstance(true);
}
%>

<br><br><br>
<a href="javascript:window.opener.location.reload();window.close();"><iwcm:text key="button.close"/></a>

<%@ include file="layout_bottom.jsp" %>
