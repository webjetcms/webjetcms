<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%@page import="sk.iway.iwcm.components.blog.BloggerMaker"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<%
Identity user = UsersDB.getCurrentUser(request);
GroupDetails actualGroup = (GroupDetails)request.getAttribute("pageGroupDetails");
boolean canEdit = false;
if (user != null && actualGroup != null) canEdit = BlogTools.isEditable(actualGroup.getGroupId(), user);

/**

	Specialna verzia news komponenty pre blog
	- kazdy odkaz je klikatelny, nemusi mat vyplneny perex.
	- neberu sa dokumenty, ktore su hlavnymi strankami adresarov (pretoze to su news komponenty pre podadresare)

**/
//stranka pre includnutie noviniek
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);

String groupIds = pageParams.getValue("groupIds", "");
//mame to v takomto formate, takze to convertneme
groupIds = groupIds.replace('+', ',');

String perexGroup = pageParams.getValue("perexGroup", "");
//mame to v takomto formate, takze to convertneme
perexGroup = perexGroup.replace('+', ',');

int orderType = DocDB.ORDER_PRIORITY;
String p_order = pageParams.getValue("orderType", "date");
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

boolean ascending = pageParams.getBooleanValue("asc", false);
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
int maxCols = pageParams.getIntValue("cols", 1);
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
else
{
	request.removeAttribute("perexGroup");
}
request.setAttribute("whereSql", whereSql);

//System.out.println("groupIds="+groupIds+" perexGroup="+perexGroup);

SelectionFilter<DocDetails> blogFilter = new SelectionFilter<DocDetails>(){

	public boolean fullfilsConditions(DocDetails doc)
	{
		return GroupsDB.getInstance().getGroup(doc.getGroupId()).getDefaultDocId() != doc.getDocId();
	}
};


if (canEdit)
{
	request.setAttribute("perexOnlyAvailable", "false");
}

docDB.getDocPerex(groupIds, orderType, ascending, publishType, pageSize, "novinky", "pages", request, blogFilter);

int counter=0;
%>

<%@page import="sk.iway.iwcm.SelectionFilter"%>
<%@ page import="sk.iway.iwcm.common.BlogTools" %>
<div class="news">
<%
if (canEdit) pageContext.include("blog_user_toolbar.jsp");
%>

<% if (paging) { %>
	<!-- strankovanie (naraz sa zobrazi iba urceny pocet web stranok) -->
	<logic:present name="pages">
		<div class="news_pages">
			<iwcm:text key="calendar.page"/>:
			<logic:iterate id="page2" name="pages" type="sk.iway.iwcm.LabelValueDetails">
				<jsp:getProperty name="page2" property="value"/>[<jsp:getProperty name="page2" property="label"/>]<%if(page2.getValue().indexOf("<a")!=-1) out.print("</a>");%>&nbsp;
			</logic:iterate>
		</div>
		<hr />
	</logic:present>
	<!-- koniec strankovania -->
<% }

boolean hasPerex;
boolean hasLink;
%>

<style type="text/css">
h2.blogItemNotAvailable a { text-decoration: none; }
</style>

<logic:notEmpty name="novinky">

	<logic:iterate id="doc" name="novinky" type="sk.iway.iwcm.doc.DocDetails">
		<%
			if (counter==0){

				out.println("<div class='blog_content'>");
			}
			counter++;
		%>
			<% if ("left".equals(image)) { %>
				<div class="news_img_left">
					<logic:notEmpty name="doc" property="perexImage">
						<img src="<jsp:getProperty name="doc" property="perexImage"/>" alt="" />
					</logic:notEmpty>
				</div>
			<% } %>

			<div class="news_text">
			<% if ("top".equals(image) && Tools.isEmpty(doc.getPerexImage())==false) { %>
					<div>
						<img src="<jsp:getProperty name="doc" property="perexImage"/>" alt="" />
					</div>
			<% } %>


				<h2<% if (doc.isAvailable()==false) out.print(" style='text-decoration:line-through;' class='blogItemNotAvailable'"); %>>
					<%
					if (date && doc.getPublishStartString()!=null){

						out.println(doc.getPublishStartString() + "  ");
						}
					if (place){

						out.println(doc.getPerexPlace() + " - ");
						}
					hasPerex = false;
					hasLink = false;
					if (doc.getPerex()!=null && doc.getPerex().length()>10)
					{
						hasPerex = true;
					}
					if (doc.getData()!=null && doc.getData().length()>10)
					{
						//ak ma perex a je zadany obsah stranky
						hasLink = true;
						out.print("<a href='"+docDB.getDocLink(doc.getDocId(), doc.getExternalLink(), request)+"'>");
					}
					if ("toplink".equals(image) && Tools.isEmpty(doc.getPerexImage())==false)
					{

						%>
						<div class="newsTablePerexImage">
							<img src="<jsp:getProperty name="doc" property="perexImage"/>" alt="" /><br />
						</div>
						<%
					}
					out.print(doc.getTitle());
					if (hasLink)
					{
						out.println("</a>");
					}

					if (canEdit)
					{
						out.println("<a href='"+Tools.addParameterToUrl(docDB.getDocLink(doc.getDocId(), doc.getExternalLink(), request), "blogEdit", "1")+"'><img src='/admin/skins/webjet6/images/icon/icon-edit.png' border='0'/></a>");
					}
					%>
				</h2>
				<%
				if ("bottom".equals(image) && Tools.isEmpty(doc.getPerexImage())==false){
					%><img src="<jsp:getProperty name="doc" property="perexImage"/>" alt="" /><%
					}

				if (hasPerex)
				{
					%><p class="perex"><jsp:getProperty name="doc" property="perexPre"/></p><%
				}
				else
				{
					%><jsp:getProperty name="doc" property="data"/><%
				}
				%>
			</div>

			<% if ("right".equals(image)) { %>
				<div class="news_img_right">
					<logic:notEmpty name="doc" property="perexImage">
						<img src="<jsp:getProperty name="doc" property="perexImage"/>" alt="" />
					</logic:notEmpty>
				</div>
			<% } %>

			<div class="blog_info">
				<span class="create_date"><% if (Tools.isNotEmpty(doc.getPublishStartString())) out.print(Tools.formatDateTime(doc.getPublishStart())); else out.print(Tools.formatDateTime(doc.getDateCreated())); %></span>
				|
				<span class="author_name"><bean:write name="doc" property="authorName"/></span>
				|
				<span class="forum_responses"><iwcm:text key="components.blog.forumResponses"/>: <bean:write name="doc" property="forumCount"/></span>
			</div>

			<%
				if (counter==maxCols){

					out.println("<div class='clearer'>&nbsp;</div></div>");
					counter = 0;
				}
			%>
	</logic:iterate>
	<%
		//ukonci posledny DIV
		if (maxCols>1 && counter!=0) out.println("<div class='clearer'>&nbsp;</div></div>");
	%>

</logic:notEmpty>
</div>