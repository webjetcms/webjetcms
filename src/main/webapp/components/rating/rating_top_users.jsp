<%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@ 
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@ 
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@ 
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@ 
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@ 
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@ 
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@ 
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@ 
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

%>
<%

if (Constants.getBoolean("editorEnableXHTML")) pageContext.setAttribute(org.apache.struts.Globals.XHTML_KEY, "true", PageContext.PAGE_SCOPE);
PageParams pageParams = new PageParams(request);

int usersLength = pageParams.getIntValue("usersLength", 10);
List topUsers = RatingService.getUsersTopList(usersLength);	
if (topUsers.size() > 0)
{		
	request.setAttribute("usersTopList", topUsers);
}
%>

<%@page import="java.util.ArrayList"%>
<%@page import="sk.iway.iwcm.components.rating.RatingService"%>
<%@page import="sk.iway.iwcm.users.UserDetails"%>
<%@page import="sk.iway.iwcm.users.UsersDB"%>
<div class="rating">
	<!--  TOP POUZIVATELIA -->	
	<iwcm:present name="usersTopList">
		<table style="width:400px;">
			 <thead>
			   <tr>
				 <th style="width:30px;">#</th>
				 <th style="width:270px;">Nick</th>
				 <th style="width:50px;">Activity</th>
				 <th style="width:50px;">Rank</th>
			   </tr>
			 </thead>
			 <tbody>
				 <logic:iterate name="usersTopList" id="u" type="sk.iway.iwcm.components.rating.jpa.RatingEntity" indexId="index" >
							<% UserDetails userDet = UsersDB.getUser(u.getUserId());
								 if (userDet != null)
								 {%>	
						  <tr>
								<td><%=index.intValue()+1%>.&nbsp;</td>
								<td><%=userDet.getLogin()%></td>
								<td><bean:write name="u" property="ratingStat"/></td>
								<td>
									<%									
										if(userDet.getRatingRank() < 100)
											out.println("rookie");
										else if(userDet.getRatingRank() >= 100 && userDet.getRatingRank() < 500)
											out.println("cooler");
										else if(userDet.getRatingRank() >= 500)
											out.println("guru");
										out.println("<br/>");									
									%>
								</td>
						 </tr>
							<%}%>	
					</logic:iterate>  
			 </tbody>
		</table>
	</iwcm:present>	
</div>
			
	<%		
		request.removeAttribute("usersTopList");
	%>
