<%@page import="java.util.List"%><%@ page import="sk.iway.iwcm.doc.DocDB" %>
<%@ page import="sk.iway.iwcm.doc.DocDetails" %>
<%@ page import="sk.iway.iwcm.users.*" %>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,java.util.*,sk.iway.iwcm.components.rating.*" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%
Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
if (user == null) return;

//format parametrov: userId, orderBy (title, rating_value, insert_date)
List ratings = RatingDB.getUserRatings(user.getUserId());
if (ratings.size()>0) request.setAttribute("ratings", ratings);
%>

<logic:present name="ratings">
	<table border='1'>
		 <thead>
		   <tr>
		     <td nowrap><b>Názov</b></td>
		     <td nowrap><b>Dátum hodnotenia</b></td>
		     <td nowrap><b>Tvoje hodnotenie</b></td>
		     <td nowrap><b>Priemerné hodnotenie</b></td>
		   </tr>
		 </thead>
		 <tbody>
			 <logic:iterate name="ratings" id="r" type="sk.iway.iwcm.components.rating.RatingBean" indexId="index" >
					  <tr>
							<td><bean:write name="r" property="docTitle"/>&nbsp;</td>
							<td><bean:write name="r" property="insertDate"/> <bean:write name="r" property="insertTime"/></td>
							<td><bean:write name="r" property="ratingValue"/></td>
							<td><bean:write name="r" property="ratingStat"/></td>
					 </tr>
				</logic:iterate>
		 </tbody>
	</table>
</logic:present>

<%
	request.removeAttribute("ratings");
%>