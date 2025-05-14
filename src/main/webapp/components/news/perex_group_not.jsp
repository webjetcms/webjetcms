<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.doc.*" %>

<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@
		taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%@
		taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %><iwcm:checkLogon admin="true" perms="cmp_news"/>
<%@page import="sk.iway.iwcm.Tools"%>
<%@ page import="sk.iway.iwcm.PageParams" %>
<%@ page import="sk.iway.iwcm.Constants" %>
<%
request.setAttribute("perexGroupsNot", request.getAttribute("perexGroups"));
%>



<%--<html:select property="perexGroupNot" styleId="real" multiple="true" size="6">--%>
   <%--<iwcm:options collection="perexGroupsNot" property="perexGroupId" labelProperty="perexGroupNameId"/>--%>
<%--</html:select>--%>
<select name="perexGroupNot" multiple="true" size="6">
	<c:forEach items="${perexGroupsNot}" var="option"><%sk.iway.iwcm.doc.PerexGroupBean option = (sk.iway.iwcm.doc.PerexGroupBean)pageContext.getAttribute("option");%>
		<option value="${option.perexGroupId}" <%=((String)request.getAttribute("perexGroupNot")).contains(","+option.getPerexGroupId()+",")?"selected=\"selected\"":"" %>>${option.perexGroupNameId}</option>
	</c:forEach>
</select>




