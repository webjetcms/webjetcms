<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@page import="sk.iway.iwcm.tags.WriteTag"%><%@
taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<logic:notPresent name="dontCheckAdmin">
	<iwcm:checkLogon admin="true"/>
</logic:notPresent>
<logic:present name="dontCheckAdmin">
	<iwcm:checkLogon />
</logic:present><%

//pomocou parametra id vieme zobrazit staru verziu
if ((request.getParameter("id")==null || request.getParameter("userlngr")==null)) {
	String path = sk.iway.iwcm.PathFilter.getOrigPath(request);
	String v9link = sk.iway.iwcm.admin.layout.MenuService.replaceV9MenuLink(path);
	//System.out.println("path="+path+" v9link="+v9link);
	if (path.equals(v9link)==false && sk.iway.iwcm.Tools.isNotEmpty(v9link)) {
		//System.out.println("v9link="+v9link);

		//response.sendRedirect(v9link);
		//return;
		//presmerovanie na v9 verziu, napr. v editor_component iframe
		%><script>
			var link = "<%=v9link%>";
			var qs = window.location.search;
			if (qs) {
				link += qs;
			}
			var hash = window.location.hash;
			if (hash.length>1) link += hash;
			//console.log("Redirecting to: "+link);
			window.location.href=link;
		</script><%
		return;
	}
}

//otestuj ci existuje nahrada za tuto stranku
String forwardtop = WriteTag.getCustomPageAdmin("/admin/layout_top.jsp", request);
if (forwardtop!=null)
{
	pageContext.include(forwardtop);
}
else
{
	pageContext.include("/admin/layout_top_default.jsp");
}

%>