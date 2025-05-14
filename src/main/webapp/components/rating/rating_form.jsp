<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@ 
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@ 
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@ 
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@ 
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
page import="sk.iway.iwcm.components.rating.jpa.RatingEntity"%><%@
page import="sk.iway.iwcm.components.rating.RatingService"%><%@ 
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

%>
<%@page import="sk.iway.iwcm.stat.StatDB"%>
<%= Tools.insertJQuery(request) %>
<iwcm:script type="text/javascript" src="/components/rating/javascript/jquery.rating.js"></iwcm:script>
<iwcm:script type="text/javascript">
	<!--
	document.write('<link rel="stylesheet" href="/components/rating/css/jquery.rating.css" media="screen" type="text/css">');
	-->
	</iwcm:script>
<%

if (Constants.getBoolean("editorEnableXHTML")) pageContext.setAttribute(sk.iway.iwcm.tags.support_logic.CustomTagUtils.XHTML_KEY, "true", PageContext.PAGE_SCOPE);
PageParams pageParams = new PageParams(request);
boolean checkLogon = pageParams.getBooleanValue("checkLogon", true);
int range = pageParams.getIntValue("range", 10);
session.setAttribute("range", String.valueOf(range));
int ratingDocId = pageParams.getIntValue("ratingDocId", -1);
if (ratingDocId < 1) ratingDocId = Tools.getDocId(request);

System.out.println("request.getRemoteAddr(): "+request.getRemoteAddr());
Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
RatingEntity ratingByDoc = RatingService.getDocIdRating(ratingDocId);
RatingEntity rBeanUserVoted = null;

if (user != null) rBeanUserVoted = RatingService.getRatingByUserByDoc(user.getUserId(), ratingDocId);
else if (checkLogon==false){
	rBeanUserVoted = RatingService.getRatingByUserByDoc(- (int)StatDB.getBrowserId(request, response) , ratingDocId);
}

if (rBeanUserVoted != null) 
{
	request.setAttribute("rBeanUserVoted", rBeanUserVoted);
	%>
	<iwcm:script type="text/javascript">
	<!--
	$(document).ready(function(){
	    $(".ratingForm<%=ratingDocId%>auto-submit-star").attr("disabled","disabled");
	});
	-->
    </iwcm:script>
    <%
	request.setAttribute("isRated", "");							
}

//kedze hlasujeme cez Ajax potrebujeme vediet, ci ma byt prihlasenie, alebo nie
session.setAttribute("rating."+ratingDocId+".checklogon", ""+checkLogon);

if (checkLogon)
{
	if (user == null)
	{				   
		%>
		<iwcm:script type="text/javascript">
		<!--
		$(document).ready(function(){
		    $(".ratingForm<%=ratingDocId%>auto-submit-star").attr("disabled","disabled");
		});
		-->
	    </iwcm:script>
	    <%
		request.setAttribute("notLogged", "");			
	}
}
else
{		
	session.setAttribute("checkLogon", "false");
}
%>	
<form id="ratingForm<%=ratingDocId%>" style="margin: 0px;">
	<iwcm:script type="text/javascript">
	$(function(){
	 $('.ratingForm<%=ratingDocId%>auto-submit-star').rating({
	  callback: function(value, link){
	  	$('#ratingForm<%=ratingDocId%>rate').load('/components/rating/ajax-star-rating.jsp', {ratingDocId: $('#ratingForm<%=ratingDocId%>docId').val(), ratingValue: this.value});
	  }
	  
	 });
	});
	</iwcm:script>
	<table class="ratingTable">
	 <tr>
	  <td valign="top" style="width:250px;">
	    <div id="ratingForm<%=ratingDocId%>rate">
	    <%
	    if(ratingByDoc != null)
	    {
	    	%>
	    	<iwcm:text key="components.rating.hodnotenie" param1='<%=""+ratingByDoc.getRatingValueDouble()%>' param2='<%=""+range%>'/><br/>
		 	<iwcm:text key="components.rating.hlasovalo" param1='<%=""+ratingByDoc.getTotalUsers()%>'/><br/>
	    	<%
	    }
	    %>
	    </div>
	    <div class="ratingText">
	    <%
	    if (rBeanUserVoted!=null)
	    {
	   	 %><iwcm:text key="components.rating.vase_hlasovanie_z" param1='<%=""+rBeanUserVoted.getInsertDate()%>'/><%
	    }
	    else if (checkLogon && user == null)
	    {
	   	 %><iwcm:text key="components.rating.not_logged_user"/><%
	    }
	    else
	    {
	   	 %><iwcm:text key="components.rating.hlasovat"/><%
	    }
	    %>
	    </div>
	    <%
	    for (int i=0; i<range; i++)
		 {%>
		   <input type='radio' name='ratingForm<%=ratingDocId%>ratingValue' class="ratingForm<%=ratingDocId%>auto-submit-star" value='<%=(i+1)%>'<%
		   if (rBeanUserVoted!=null && rBeanUserVoted.getRatingValue()>i) out.print(" checked='checked'");
		   %>/>
	    <%
	    }
	    %>
			<input type='hidden' name='ratingForm<%=ratingDocId%>docId' id="ratingForm<%=ratingDocId%>docId" value='<%=ratingDocId%>' />
			<input type='hidden' name='action' id="action" value='save' />
	  </td>
	 </tr>
	</table>
</form>		
	
	<%
		request.removeAttribute("isRated");
		request.removeAttribute("notLogged");
	%>

