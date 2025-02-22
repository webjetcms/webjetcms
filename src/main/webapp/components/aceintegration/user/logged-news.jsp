<%@page import="sk.iway.iwcm.users.UsersDB"%>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="windows-1250"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,sk.iway.iwcm.components.basket.*,java.util.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@page import="sk.iway.iwcm.i18n.Prop"%>

<%
    
  String lng = PageLng.getUserLng(request);
  pageContext.setAttribute("lng", lng);
  
  Identity user = UsersDB.getCurrentUser(session);
  if(user != null)
  {

%>

	<div class="md-welcome logged" data-aos="fade-left" data-aos-duration="600">
		<iwcm:write>!INCLUDE(/components/cta/cta_auto_insert.jsp, suffix=banner-prihlaseny)!</iwcm:write>
	</div>


<%}else{%>

	<div class="md-welcome" data-aos="fade-left" data-aos-duration="600">		
		<iwcm:write>!INCLUDE(/components/cta/cta_auto_insert.jsp, suffix=banner-neprihlaseny)!</iwcm:write>
	</div>

   <%--<iwcm:write>!INCLUDE(/components/news/news-velocity.jsp, groupIds=&quot;24&quot;, alsoSubGroups=&quot;false&quot;, publishType=&quot;new&quot;, order=&quot;date&quot;, ascending=&quot;false&quot;, paging=&quot;false&quot;, pageSize=&quot;1&quot;, offset=&quot;0&quot;, perexNotRequired=&quot;false&quot;, loadData=&quot;false&quot;, checkDuplicity=&quot;true&quot;, contextClasses=&quot;&quot;, cacheMinutes=&quot;0&quot;, template=&quot;news.template.dlazdica-1&quot;, perexGroup=&quot;&quot;, perexGroupNot=&quot;&quot;)!</iwcm:write>--%>
<%}%>