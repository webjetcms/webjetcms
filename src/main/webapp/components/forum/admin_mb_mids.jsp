<%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.forum.*,java.util.*,sk.iway.iwcm.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<iwcm:checkLogon admin="true" perms="cmp_diskusia"/>
<%@ include file="/admin/layout_top.jsp" %>
<%
//toto je len haluza co podla docId a pID vypise zoznam IDecok prispevkov

int docId = Tools.getIntValue(request.getParameter("docid"), -1);
int parentId = Tools.getIntValue(request.getParameter("pId"), 0);

try
{
	if (parentId > 0)
	{
		List forum = ForumDB.getForumFieldsForDoc(request, docId, true, parentId, true, true);
		Iterator iter = forum.iterator();
		ForumBean fb;
		int i = 0;
		String parentIds = null;
		while (iter.hasNext())
		{
			fb = (ForumBean)iter.next();
			//out.println(i++ + " ");
			//out.println(fb.getSubject()+ ":" + fb.getQuestionHtml() + " <br>");
			out.print(fb.getForumId()+",");

			if (parentIds == null)
			{
			   parentIds = "" + fb.getForumId();
			}
			else
			{
				parentIds += "," + fb.getForumId();
			}
		}

		out.println("<br>");
		out.println("update document_forum SET doc_id=XXX where doc_id="+docId+" AND (parent_id IN ("+parentIds+") OR forum_id="+parentId+")");
	}

}
catch(Exception e)
{
	sk.iway.iwcm.Logger.error(e);
}
%>
<%@ include file="/admin/layout_bottom.jsp" %>