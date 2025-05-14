<%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,sk.iway.iwcm.forum.*,sk.iway.iwcm.components.forum.jpa.*, sk.iway.iwcm.users.*,java.util.*"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%
//stranka pre includnutie noviniek

PageParams pageParams = new PageParams(request);

String groupIds = pageParams.getValue("groupIds", "");
//mame to v takomto formate, takze to convertneme
groupIds = groupIds.replace('+', ',');

String perexGroup = pageParams.getValue("perexGroup", "");
//mame to v takomto formate, takze to convertneme
perexGroup = perexGroup.replace('+', ',');

int orderType = DocDB.ORDER_PRIORITY;
String p_order = pageParams.getValue("orderType", "priority");
if (p_order != null)
{
	if (p_order.compareTo("date") == 0)
	{
		orderType = DocDB.ORDER_DATE;
	}
	else if (p_order.compareTo("id") == 0)
	{
		orderType = DocDB.ORDER_ID;
	}
	else if (p_order.compareTo("priority") == 0)
	{
		orderType = DocDB.ORDER_PRIORITY;
	}
	else if (p_order.compareTo("title") == 0)
	{
		orderType = DocDB.ORDER_TITLE;
	}
	else if (p_order.compareTo("place") == 0)
	{
		orderType = DocDB.ORDER_PLACE;
	}
	else if (p_order.compareTo("eventDate") == 0)
	{
		orderType = DocDB.ORDER_EVENT_DATE;
	}
	else if (p_order.compareTo("saveDate") == 0)
	{
		orderType = DocDB.ORDER_SAVE_DATE;
	}
}

boolean ascending = pageParams.getBooleanValue("asc", true);
int publishType = DocDB.PUBLISH_NEW;
String p_publish = pageParams.getValue("publishType", "new");
if (p_publish!=null)
{
	if (p_publish.compareToIgnoreCase("new")==0)
	{
		publishType = DocDB.PUBLISH_NEW;
	}
	else if (p_publish.compareToIgnoreCase("old")==0)
	{
		publishType = DocDB.PUBLISH_OLD;
	}
	else if (p_publish.compareToIgnoreCase("all")==0)
	{
		publishType = DocDB.PUBLISH_ALL;
	}
	else if (p_publish.compareToIgnoreCase("next")==0)
	{
		publishType = DocDB.PUBLISH_NEXT;
	}
}

boolean noPerexCheck = pageParams.getBooleanValue("noPerexCheck", false);
if (noPerexCheck && publishType < 100)
{
	publishType = publishType + 100;
}

boolean paging = pageParams.getBooleanValue("paging", false);
int pageSize = pageParams.getIntValue("pageSize", 10);
int maxCols = 2; // pageParams.getIntValue("cols", 1);
String image = pageParams.getValue("image", "none");
boolean perex = pageParams.getBooleanValue("perex", false);
boolean date = pageParams.getBooleanValue("date", false);
boolean place = pageParams.getBooleanValue("place", false);

//ziskaj DocDB
DocDB docDB = DocDB.getInstance();

int actualDocId = -1;
try
{
	actualDocId = Integer.parseInt(request.getParameter("docid"));
}
catch (Exception ex)
{
	//sk.iway.iwcm.Logger.error(ex);
}
//vyradime zo zobrazenia aktualnu stranku
String whereSql = " AND doc_id NOT IN ("+actualDocId+") ";
if (perexGroup != null && perexGroup.length()>0)
{
	request.setAttribute("perexGroup", perexGroup);
}
request.setAttribute("whereSql", whereSql);

//System.out.println("groupIds="+groupIds+" perexGroup="+perexGroup);

docDB.getDocPerex(groupIds, orderType, ascending, publishType, pageSize, "novinky", "pages", request);

int counter=0;
int totalPhorumNum = 0;

boolean hasPerex;
boolean hasLink;

DocForumEntity fb;
Identity user = (Identity) session.getAttribute(Constants.USER_KEY);

int count = 0;
String pocet = (String)request.getAttribute("pagingListKey");
if(request.getAttribute("count")!=null)
count = Tools.getIntValue((String)request.getAttribute("count"),-1);

%>

<table class="displayMainTopics" border="0" cellpadding="0" cellspacing="0">
<tbody>
<iwcm:iterate id="doc" name="novinky" type="sk.iway.iwcm.doc.DocDetails">
	<%

    totalPhorumNum++;
		if (counter==0)
		{
			out.println(" <tr>");
		}
		counter++;

    hasPerex = false;
    if (doc.getPerex()!=null && doc.getPerex().length()>10)
		{
			hasPerex = true;
		}
    %>
    <td class="fImage"><% out.print("<a href='/showdoc.do?docid="+doc.getDocId()+"'>"); %><img alt="" src="<jsp:getProperty name="doc" property="perexImage"/>" border="0"><%="</a>"%></td>
    <td class="fTextShort">
    <h1><% out.print("<a href='/showdoc.do?docid="+doc.getDocId()+"'>"); out.print(doc.getTitle()); %></a></h1>
    <p>
	<%
   	List topics = null;
   	int totalResults = 0;
   	String key = "components.forum.number_of_topics";
   	topics = ForumDB.getForumTopics(doc.getDocId());
	   if (topics.size() > 0)
		  request.setAttribute("topics", topics);

   	if(topics != null && topics.size() > 0)
	   {
		  // request.setAttribute("pagingList", topics);
		  // if(request.getAttribute("pagingList") != null);

		   if (topics != null && topics.size() > 0)
		   {
			  totalResults = topics.size();
		   }
		}
		%>
		<iwcm:text key="<%=key%>"/>:&nbsp;<%=totalResults%>
	</p>
    <p><span class="datumCas">
	<%
	if (doc!=null && doc.getDocId() > 0)
	{
		fb = ForumDB.getForumStat(doc.getDocId());
		if (fb != null)
		{
			out.println("("+fb.getStatReplies()+" príspevkov)<br>");
			if(fb.getLastPost().length()>0)
				out.println("Dátum posledného príspevku:<br>"+fb.getLastPost()+ "&nbsp;");

			if(user!=null && ForumDB.compareDates(fb.getLastPost(), ForumDB.getUserLastLogon(user.getUserId())))
			{ %>
				<img src="/images/content/forum/icon/icon_newposts_home.gif" border="0">
		<%	}
		}
	}
	%>
	<!--	<img src="/images/content/forum/icon/icon_newposts_home.gif" border="0" height="10" width="14"> -->
	</span></p>

    <p>
    <%
    if (hasPerex)
	  {
		%><jsp:getProperty name="doc" property="perexPre"/><%
	  }
	  %>
    </p>
    </td>
	<%
		if (counter==maxCols)
		{
			out.println("   </tr>");
			counter = 0;
		}
	%>
</iwcm:iterate>
</tbody>
</table>
