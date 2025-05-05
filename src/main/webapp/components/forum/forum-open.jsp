<%@page import="java.util.List"%><%@page import="sk.iway.iwcm.users.UserDetails"%>
<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.forum.*,sk.iway.iwcm.components.forum.jpa.*,java.util.*,sk.iway.iwcm.*,sk.iway.iwcm.doc.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<%@page import="org.apache.commons.codec.binary.Base64"%><%@page import="sk.iway.iwcm.i18n.Prop"%><%@page import="org.apache.struts.util.ResponseUtils"%>

<%
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
Base64 b64 = new Base64();
if (Tools.isNotEmpty(paramPageParams))
{
	paramPageParams = Tools.replace(paramPageParams, "|", "=");
	String decoded = new String(b64.decode(paramPageParams.getBytes()));
	request.setAttribute("includePageParams", decoded);
}

PageParams pageParams = new PageParams(request);

paramPageParams = (String)request.getAttribute("includePageParams");
String base64encoded = "";
if(Tools.isNotEmpty(paramPageParams)) {
	base64encoded = new String(b64.encode(paramPageParams.getBytes()));
	base64encoded = Tools.URLEncode(Tools.replace(base64encoded, "=", "|"));
}

boolean isAjaxCall = request.getAttribute("docDetails")==null;
if(isAjaxCall)
{
	pageParams = (PageParams) session.getAttribute("forumOpenPageParams");
	if (pageParams == null)
	{
		//nemame session, vrat nieco zmysluplne
		%><iwcm:text key="forum.errorLoadingData"/><%
		return;
	}
}
session.setAttribute("forumOpenPageParams",pageParams);

boolean sortAscending = pageParams.getBooleanValue("sortAscending", true);
if (request.getParameter("sortAscending")!=null) sortAscending = "true".equals(request.getParameter("sortAscending"));


int docId = Tools.getDocId(request);
Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
List forum = null;

forum = ForumDB.getForumFieldsForDoc(request, docId, true, 0, sortAscending, false);

boolean active = ForumDB.isActive(docId);
pageContext.setAttribute("data", forum);
request.setAttribute("data", forum);

boolean isAdmin = false;
boolean canPostNewTopic = true;
ForumGroupEntity forumGroupBean = ForumDB.getForum(docId); //Never is null
if (active==false) {
	canPostNewTopic = false;
	pageContext.setAttribute("forumClosed", "");
}

if (forumGroupBean.isAdmin(user)) {
	canPostNewTopic = true;
	isAdmin = true;
}

	/**
	 *	Strankovanie
	 */

	int MAXIMUM_PAGE_SIZE = 25;
	String pageSizeParam = request.getParameter("pageSize");

	if (Tools.isInteger(pageSizeParam) && Tools.getIntValue(pageSizeParam,1) > 0)
		MAXIMUM_PAGE_SIZE = Tools.getIntValue(pageSizeParam,25);
	else
		MAXIMUM_PAGE_SIZE = pageParams.getIntValue("pageSize", 25);

	int MAXIMUM_PAGE_LINKS = 4;
	int pageCount = 0;
	int pageNumber = 0;
	int dataSize = 0;

	if (pageParams.getBooleanValue("noPaging", false)==false && !(request.getParameter("showAll")!=null || request.getParameter("randomSize")!=null || request.getParameter("noPaging")!=null) )
	{
		List data = (ArrayList)request.getAttribute("data");

		pageNumber = Tools.getIntValue(request.getParameter("page"),0);
		if (pageNumber<0) pageNumber = 0;

		//ak sme nic nenasli, tak je nula stranok, inak aspon jedna
		if (data != null)
		{
			pageCount = data.size() == 0 ? 0 : data.size()/MAXIMUM_PAGE_SIZE;
			dataSize = data.size();
			if (pageCount*MAXIMUM_PAGE_SIZE == dataSize)	// musi takto byt, lebo pri presnych nasobkoch vypise o jednu prazdnu stranu viac
				pageCount--;
		}

		if (pageCount > 0 &&  MAXIMUM_PAGE_SIZE == 0)
			pageCount--;
		if (pageNumber > pageCount)
			pageNumber = 0 ;

		//potrebujeme zabranit ArrayIndexOutOfBoundsException, ak sme na poslednej stranke
		int maximumDataCount = dataSize > (pageNumber+1)*MAXIMUM_PAGE_SIZE ? (pageNumber+1)*MAXIMUM_PAGE_SIZE : dataSize;

		String pageBannerString = Prop.getInstance(request).getText("components.forms.form_detail.displaying",dataSize+"",(pageNumber*MAXIMUM_PAGE_SIZE+1)+"",maximumDataCount+"");
		//obmedzime list pre strankovanie
		if (data != null)
			data = data.subList(pageNumber*MAXIMUM_PAGE_SIZE, maximumDataCount );

		request.setAttribute("pageBannerString",pageBannerString);
		request.setAttribute("data",data);
		pageContext.setAttribute("data", data);
	}

%>

<%
if(!isAjaxCall)
{
	//toto treba vyrendrovat len ak sa to nevola z ajaxu, aby sa to nekaskadovalo dnu do forumContentDiv
%>
	<style type="text/css" media="all">@import url("/components/forum/forum.css");</style>

	<!-- // dialogove okno s funkciami -->
	<jsp:include page="/components/dialog.jsp" />

	<% if (user != null && user.isAdmin()) { %>
	<style type="text/css">
	tr.trDeleted td, tr.trDeleted td a { color: red !important }
	</style>
	<% } %>


	<div id="forumContentDiv">
<%
}
%>
<h3><iwcm:text key="forum.title"/></h3>

<logic:present name="forumClosed">
	<p><span class="forumClosed"><iwcm:text key="components.forum.forum_closed"/>!</span></p>
</logic:present>

<div class="row">
	<div class="col-xs-12 col-sm-12 col-md-12">
		<% if (canPostNewTopic) { %>
			<a href="javascript:openWJDialog('forum', '/components/forum/new.jsp?parent=0&type=open&docid=<%=docId%>&pageParams=<%=base64encoded %>&pageNum=<%=pageNumber %>&language=<%=lng%>',600,'<iwcm:text key="forum.new"/>');" class="btn btn-success"><iwcm:text key="forum.new"/></a>
		<% } %>
	</div>
</div>

<logic:iterate name="data" id="field" type="DocForumEntity" indexId="index">
<div class="media">
  <div class="media-left">
  	<%
        String userImg = Tools.isNotEmpty(field.getAuthorPhoto("")) ? "/thumb" + field.getAuthorPhoto("") + "?w=64&h=64&ip=4" : "/components/forum/images/user.png";

        if (field.getLevel() != 0) { %>
        <a href="#" style="width: 64px; height: 64px; display: block;">&nbsp;</a>
    <% }else{
    %>
    <a href="#">
      <img class="media-object" style="width: 64px; height: 64px;" src="<%=userImg %>" alt=""/>
    </a>
    <% } %>
  </div>
  <div class="media-body">

                <% if (field.getLevel() > 0) { %>

                	<div class="media">
                      <div class="media-left">
                        <a href="#">
                          <img class="media-object" style="width: 64px; height: 64px;" src="<%=userImg %>" alt="">
                        </a>
                      </div>
                      <div class="media-body">
                              <iwcm:text key="forum.author"/>:
                              <logic:notEmpty name="field" property="authorEmail">
                                  <a href="mailto:<bean:write name="field" property="authorEmail"/>"><bean:write name="field" property="authorName"/></a>
                              </logic:notEmpty>
                              <logic:empty name="field" property="authorEmail">
                                  <bean:write name="field" property="authorName"/>
                              </logic:empty>
                              <div class="row">
                                  <div class="col-xs-12 col-sm-12 col-md-12">
                                    <jsp:getProperty name="field" property="question"/>
                                  </div>
                              </div>
                      </div>
                    </div>
				<%}else{%>

							<h4 class="media-heading"><bean:write name="field" property="subject"/>
								<%if (!active || !field.getActive()) {%>
									<img src="/components/forum/images/folder_locked_big.gif" style="border:0px;" align="absbottom"/>
								<%}%>
							</h4>

							<iwcm:text key="forum.author"/>:
							<logic:notEmpty name="field" property="authorEmail">
								<a href="mailto:<bean:write name="field" property="authorEmail"/>"><bean:write name="field" property="authorName"/></a>
							</logic:notEmpty>
							<logic:empty name="field" property="authorEmail">
								<bean:write name="field" property="authorName"/>
							</logic:empty>
<%--
							<bean:write name="field" property="questionDateDisplayDate"/> <bean:write name="field" property="questionDateDisplayTime"/>--%>
						 <%if (field.canPost(forumGroupBean, user)) {%>
							<a href="javascript:openWJDialog('forum', '/components/forum/new.jsp?parent=<bean:write name="field" property="forumId"/>&type=open&docid=<%=docId%>&pageParams=<%=base64encoded %>&pageNum=<%=pageNumber %>');">[<iwcm:text key="forum.reply"/>]</a>
						 <%}%>

					<div class="row">
						<div class="col-xs-12 col-sm-12 col-md-12">
							<jsp:getProperty name="field" property="question"/>
						</div>
					</div>
                <%}%>

  </div>
</div>
</logic:iterate>
<%--
<table border="0" cellspacing="0" cellpadding="0" class="forumTable">
<%if (!active) {%>
 <tr>
	<td colspan="3" align="center"><b><iwcm:text key="components.forum.forum_closed"/>!</b></td>
 </tr>
  <tr>
	<td>&nbsp;</td>
 </tr>
<%}%>
<tr>
  <td class="forumTitle">
	  <iwcm:text key="forum.title"/>
  </td>
 <%if (active) {%>
  <td style="text-align:right;">
	  <a href="javascript:openWJDialog('forum', '/components/forum/new.jsp?parent=0&type=open&docid=<%=docId%>&pageParams=<%=base64encoded %>&pageNum=<%=pageNumber %>');"><iwcm:text key="forum.new"/></a>
  </td>
 <%}%>
</tr>
<tr>
  <td colspan="2">
	<% if (forum.size()==0) { %>
		<div class="forumBlank">
			<iwcm:text key="forum.empty"/>
		</div>
	<% } else { %>
		<table border="0" cellspacing="0" cellpadding="0" class="forumOpenTable">

			<logic:iterate name="data" id="field" type="DocForumEntity" indexId="index">
				<tr<% if (field.isDeleted()) out.print(" class='trDeleted'"); %>>
					<td style="padding-left:<%=(20 * field.getLevel())%>px;" class="forumOpenTableHeader">
							<b><bean:write name="field" property="subject"/></b><br />
							<iwcm:text key="forum.author"/>:
							<logic:notEmpty name="field" property="authorEmail">
								<a href="mailto:<bean:write name="field" property="authorEmail"/>"><bean:write name="field" property="authorName"/></a>
							</logic:notEmpty>
							<logic:empty name="field" property="authorEmail">
								<bean:write name="field" property="authorName"/>
							</logic:empty>
					</td>
					<td align="right" class="forumOpenTableHeader">
							<bean:write name="field" property="questionDateDisplayDate"/> <bean:write name="field" property="questionDateDisplayTime"/><br />
						 <%if (active) {%>
							<a href="javascript:openWJDialog('forum', '/components/forum/new.jsp?parent=<bean:write name="field" property="forumId"/>&type=open&docid=<%=docId%>&pageParams=<%=base64encoded %>&pageNum=<%=pageNumber %>');">[<iwcm:text key="forum.reply"/>]</a>
						 <%}%>
					</td>
				</tr>
				<tr<% if (field.isDeleted()) out.print(" class='trDeleted'"); %>>
					<td colspan="2" style="padding-left:<%=((20 * field.getLevel())+5)%>px;" class="forumOpenQuestion">
						<jsp:getProperty name="field" property="question"/>
					</td>
				</tr>
			</logic:iterate>

		</table>
	<% } %>
  </td>
</tr>
</table>
--%>
<%----------------------- MIESTO NA ZOBRAZENIE STRANKOVANIA -----------------------%>
<%
	//iba ak mame viac ako jednu stranku, tak ma zmysel robit strankovanie
	//pri zrebovani by sa po prechode na novu stranku muselo zrebovat znovu, preto vtedy strankovanie nebude
	if (dataSize>0 && pageCount>0 && pageParams.getBooleanValue("noPaging", false)==false && request.getParameter("randomSize")==null && request.getParameter("noPaging")==null)
	{
		String originalUrl = PathFilter.getOrigPath(request);
		originalUrl+="?"+request.getAttribute("javax.servlet.forward.query_string");
		if (request.getAttribute("javax.servlet.forward.query_string") == null)
			originalUrl="?";
%>
	<div style="float: left;">
		<span class="pagebanner" >
			<%=request.getAttribute("pageBannerString") %>
		</span>

		<%-- lava sipka --%>
		<% originalUrl = originalUrl.replaceAll("&page=[a-z0-9]+",""); %>
		<span class="pageLinks">

			<a href="<%=ResponseUtils.filter(originalUrl+"&page="+(pageNumber-1 < 0 ? 0 : pageNumber -1) )%>" title="dozadu"  class="leftArrow">[&lt;&lt;]</a>

			<%-- odkaz na prvu linku dame vzdy--%>

			<a href="<%=ResponseUtils.filter(originalUrl+"&page=0") %>"  class="pageLink<%=0==pageNumber?" selected":"" %>" >1</a>

			<%-- toto zobrazi medzeru medzi prvou strankou a nasledujucou, ktora sa zobrazi, ak je rozdiel medzi nimi velky--%>
			<% if (pageNumber - MAXIMUM_PAGE_LINKS > 1)
				out.print("<a class=\"pageLink\" href=\""+ResponseUtils.filter(originalUrl+"&page="+(pageNumber-1))+"\">...</a>");%>

			<%-- a samotne zobrazenie liniek --%>
			<%
				for (int pageCounter = pageNumber - MAXIMUM_PAGE_LINKS; pageCounter < pageNumber + MAXIMUM_PAGE_LINKS + 1 ; pageCounter++)
				{
					if (pageCounter <= 0 || pageCounter >= pageCount)
						continue;
					String url = originalUrl.replaceAll("&page=[a-z0-9]+","");
					url += "&page="+pageCounter;
			%>

					<a href="<%=ResponseUtils.filter(url)%>" class="pageLink<%=pageCounter==pageNumber?" selected":"" %>"><%=pageCounter+1 %></a>

			<%
				}
			%>
			<%-- toto zobrazi medzeru medzi poslednou strankou a predchadzajucou, ktora sa zobrazi, ak je rozdiel medzi nimi velky--%>
			<% if (pageNumber + MAXIMUM_PAGE_LINKS + 1 < pageCount) out.print("<a class=\"pageLink\" href=\""+ResponseUtils.filter(originalUrl+"&page="+(pageNumber+1))+"\">...</a>");%>

			<%-- aj odkaz na poslednu linku dame vzdy--%>
			<% originalUrl = originalUrl.replaceAll("&page=[a-z0-9]+","");
			if (pageCount > 0) {%>
			<a href="<%=ResponseUtils.filter(originalUrl+"&page="+pageCount) %>" class="pageLink<%=pageCount==pageNumber?" selected":"" %> last"><%=pageCount+1%></a>
			<%}%>
			<a href="<%=ResponseUtils.filter(originalUrl+"&page="+(pageNumber+1 > pageCount ? pageCount : pageNumber+1)) %>" title="dopredu" class="rightArrow">[&gt;&gt;]</a>
		</span>

	</div>

<%----------------------------- KONIEC STRANKOVANIA -----------------------------%>
<%
}

if(!isAjaxCall)
{
%>
</div>
<%
}
%>
