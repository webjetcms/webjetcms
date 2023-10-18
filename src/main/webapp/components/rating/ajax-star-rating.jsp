<%@page import="sk.iway.iwcm.forum.ForumDB"%>
<%@page import="sk.iway.iwcm.doc.DocDetails"%>
<%@page import="sk.iway.iwcm.doc.DocDB"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" buffer="100kb" autoFlush="false"%><%@page 
import="sk.iway.iwcm.Tools"%><%@ 
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
page import="sk.iway.iwcm.components.rating.RatingDB"%><%@
page import="sk.iway.iwcm.components.rating.RatingBean"%><%@
page import="sk.iway.iwcm.*"%><%@
page import="sk.iway.iwcm.stat.StatDB"%><%
	String range = (String)session.getAttribute("range");
	
	int ratingValue = Tools.getIntValue(request.getParameter("ratingValue"),-1);
    int ratingDocId = Tools.getIntValue(request.getParameter("ratingDocId"),-1);
    
    int userId = - (int)StatDB.getBrowserId(request, response);
    Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
    if (user != null) userId = user.getUserId();
    boolean checklogon = true;
    if ("false".equals(session.getAttribute("rating."+ratingDocId+".checklogon"))) checklogon = false;
    
    if(ratingValue != -1 && ratingDocId != -1)
    {
	    RatingBean ratingByDoc = RatingDB.getDocIdRating(ratingDocId);
	    if(checklogon && userId <= 0)	//ak je checklogon true, skontrolujem, ci je pouzivatel prihlaseny
	    {
	    	%>
	    	
			<script type="text/javascript">
			<!--
				window.alert("<iwcm:text key="components.rating.not_logged_user"/>");
			-->
		   </script>
		   
		   <iwcm:text key="components.rating.hodnotenie" param1="<%=""+ratingByDoc.getRatingValue()%>" param2="<%=""+range%>"/><br/>
		   <iwcm:text key="components.rating.hlasovalo" param1="<%=""+ratingByDoc.getTotalUsers()%>"/><br/>
		   <%
	    }
	    else
	    {
		    RatingBean rBean2 = RatingDB.getRatingByUserByDoc(userId, ratingDocId);
		    
		   if (rBean2 != null && userId != -1) 	//ak pouzivatel este nehlasoval, rBean2 je null
			{
		    	%>
		    	<script type="text/javascript">
				<!--
				alert("<iwcm:text key="components.rating.lutujeme_uz_hlasoval"/>");
				-->
			    </script>
			    
			    <iwcm:text key="components.rating.hodnotenie" param1="<%=""+ratingByDoc.getRatingValue()%>" param2="<%=""+range%>"/><br/>
		 		<iwcm:text key="components.rating.hlasovalo" param1="<%=""+ratingByDoc.getTotalUsers()%>"/><br/>
			    <%
			}
		   else
		   {
			    RatingBean rBean = new RatingBean();
			    rBean.setDocId(ratingDocId);
			    rBean.setUserId(userId);
			    rBean.setRatingValue(ratingValue);
			    RatingDB.saveRating(rBean);
			    ratingByDoc = RatingDB.getDocIdRating(ratingDocId);
			   
			    //Custom pre Plusku/Cloud update documents.forum_count
			    //ziskam si raiting podla DocID 
			    //if(ratingByDoc != null)
			    //{
				 //	int docIdRating = ratingByDoc.getRatingValue();
				 //	ForumDB.setForumCountForDocIds(ratingDocId, docIdRating);
			    //}
			    %>
			    <iwcm:text key="components.rating.hodnotenie" param1="<%=""+ratingByDoc.getRatingValue()%>" param2="<%=""+range%>"/><br/>
		 		<iwcm:text key="components.rating.hlasovalo" param1="<%=""+ratingByDoc.getTotalUsers()%>"/><br/>
		   		<%
			}
	    }
    }
%>