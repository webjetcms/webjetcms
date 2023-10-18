<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*, sk.iway.iwcm.doc.*" %>
<%@ taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>
<%@ taglib prefix="iway" uri="/WEB-INF/iway.tld" %>
<%@ taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %>
<%@ taglib prefix="html" uri="/WEB-INF/struts-html.tld" %>
<%@ taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms="menuWebpages"/>
<%@page import="java.util.List"%><%

//DebugTimer dt = new DebugTimer("perex_groups");

int groupId = Tools.getIntValue(Tools.getRequestParameter(request, "groupId"), -1);
DocDetails doc = DocDB.getInstance().getDoc(Tools.getIntValue(Tools.getRequestParameter(request, "docId"), -1));
if(doc != null && doc.getDocId() > 0)
{
	groupId = doc.getGroupId();
	session.setAttribute("groupId", groupId);
}
else if(groupId < 1)
{
	if(session.getAttribute(Constants.SESSION_GROUP_ID) != null)
		groupId = Integer.parseInt((String) session.getAttribute(Constants.SESSION_GROUP_ID));
}

//dt.diff("1");
List<PerexGroupBean> perexGroups = DocDB.getInstance().getPerexGroups(groupId);
//dt.diff("2");
for(PerexGroupBean perexGroup : perexGroups)
{
	StringBuilder sb = new StringBuilder();
	sb.append("<option value='").append(perexGroup.getPerexGroupId()).append("'");
	if (perexGroups.size()<200) sb.append(" title='").append(perexGroup.getPerexGroupNameId()).append("'");
	sb.append(">").append(perexGroup.getPerexGroupNameId()).append("</option>").append("\n");
	//out.println("<option value='"+perexGroup.getPerexGroupId()+"'>"+perexGroup.getPerexGroupNameId()+"</option>");
	out.println(sb.toString());
}
//dt.diff("done");
%>
