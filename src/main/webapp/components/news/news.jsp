<%@page import="sk.iway.iwcm.editor.InlineEditor"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,java.io.*"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%
//stranka pre includnutie noviniek
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);

String groupIds = pageParams.getValue("groupIds", "");

//mame to v takomto formate, takze to convertneme
groupIds = groupIds.replace('+', ',');

//ak je nastavene na true beru sa do uvahy aj podadresare
boolean expandGroupIds = pageParams.getBooleanValue("expandGroupIds", true);

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
	else if (p_order.compareTo("rating") == 0)
	{
		orderType = DocDB.ORDER_RATING;
	}
}

boolean ascending = pageParams.getBooleanValue("asc", true);
int publishType = DocDB.PUBLISH_NEW;
String p_publish = pageParams.getValue("publishType", "new");
if (p_publish!=null){
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

int truncate = pageParams.getIntValue("truncate", -1);

boolean noPerexCheck = pageParams.getBooleanValue("noPerexCheck", false);
if (noPerexCheck && publishType < 100)
{
	publishType = publishType + 100;
}

boolean paging = pageParams.getBooleanValue("paging", false);
int pageSize = pageParams.getIntValue("pageSize", 10);
int maxCols = pageParams.getIntValue("cols", 1);
// image moznosti - none, top, bottom
String image = pageParams.getValue("image", "none");
String pagingStyle = pageParams.getValue("pagingStyle", "top");
String newsName = pageParams.getValue("name", "");
String newsStyle = pageParams.getValue("style", "");
boolean perex = pageParams.getBooleanValue("perex", false);
boolean date = pageParams.getBooleanValue("date", false);
boolean place = pageParams.getBooleanValue("place", false);

//ziskaj DocDB
DocDB docDB = DocDB.getInstance();

int actualDocId = -1;
try
{
	actualDocId = Integer.parseInt(Tools.getRequestParameter(request, "docid"));
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

docDB.getDocPerex(groupIds, orderType, ascending, publishType, pageSize, "novinky", "pages", request);

String perexImgClass = "";
String perexLink = "";
int counter=0;
boolean hasPerex;
boolean hasLink;
boolean isUlStyle = false;

if (newsStyle.equals("ul")) {
	isUlStyle = true;
}

if (!isUlStyle) {
	out.print("<div class=\"news" + (Tools.isNotEmpty(newsName) ? " " + newsName + "\">" : "\">" ));
}

if (paging && (pagingStyle.equals("top") || pagingStyle.equals("both"))) { %>
	<!-- strankovanie (naraz sa zobrazi iba urceny pocet web stranok) -->
	<iwcm:present name="pages">
    <div class="news_pages news_pages_top">
      <iwcm:text key="calendar.page"/>:
      <iwcm:present name="prev">
      	${prev.value}${prev.label}</a>
      </iwcm:present>
      <iwcm:iterate id="page2" name="pages" type="sk.iway.iwcm.LabelValueDetails">
        <jsp:getProperty name="page2" property="value"/>[<jsp:getProperty name="page2" property="label"/>]<%if(page2.getValue().indexOf("<a")!=-1) out.print("</a>");%>&nbsp;
      </iwcm:iterate>
      <iwcm:present name="next">
      	${next.value}${next.label}</a>
      </iwcm:present>
    </div>
  </iwcm:present>
	<!-- koniec strankovania -->
<%
}

if (isUlStyle) {
	out.print("<ul class=\"news" + (Tools.isNotEmpty(newsName) ? " " + newsName + "\">" : "\">" ));
}
%>

<iwcm:notEmpty name="novinky">
	<iwcm:iterate id="doc" name="novinky" type="sk.iway.iwcm.doc.DocDetails">
			<%
				perexImgClass = "news_img";

				if (counter==0 && !isUlStyle) {
					%><div class="news_content"><%
				}

				counter++;

				// perexLink - link na perex obrazok
				perexLink = docDB.getDocLink(doc.getDocId(), doc.getExternalLink(), request);

				//ak ma perex a je zadany obsah stranky
				if (doc.getData()!=null && doc.getData().length()>10) {
					hasLink = true;
				}
				else {
					hasLink = false;
				}
				//out.print(new File(sk.iway.iwcm.Tools.getRealPath(doc.getPerexImage())).isFile());
				// overenie existencie suboru na servri
				if (new File(sk.iway.iwcm.Tools.getRealPath(doc.getPerexImage())).isFile()==false) {
					perexImgClass = "news_no_image";
				}
			%>
			<%
			// vlozenie obrazku nad text alebo vlozenie news_no_image odkaz
			if ("top".equals(image) && !isUlStyle) {
			%>
			<iwcm:notEmpty name="doc" property="perexImage">
				<div class="<%=perexImgClass%>">
					<% if (hasLink){ %>
						<a href="<%=perexLink%>">
					<%	}
						if (!perexImgClass.equals("news_no_image")) { %>
						<img src="/thumb<jsp:getProperty name="doc" property="perexImage"/>?w=120&h=90&ip=5" alt="<%=doc.getTitle()%>" />
					<% 	}

						if (hasLink){ %>
						</a>
					<%	} %>
				</div>
			</iwcm:notEmpty>
			<iwcm:empty name="doc" property="perexImage">
				<div class="news_no_image">
				</div>
			</iwcm:empty>
			<% } %>

			<% if (isUlStyle) { %>
				<li>
			<% }
			else { %>
				<div class="news_title">
					<h4>
			<%
			}
			// datum sa vlozi pred nadpis, ked je obrazok pod nadpisom
			if ("bottom".equals(image)) {
					if (date && Tools.isNotEmpty(doc.getPublishStartString()) ) {
						out.println("<span class=\"news_date\">" + doc.getPublishStartString() + "</span> -");
					}

					if (place && Tools.isNotEmpty(doc.getPerexPlace())) {
						out.println("<span class=\"news_place\">" + doc.getPerexPlace() + "</span> -");
					}
			}

			if (hasLink) { %>
				<a href="<%= docDB.getDocLink(doc.getDocId(), doc.getExternalLink(), request) %>"<%=InlineEditor.getEditAttrs(request, doc, "title", true) %>>
				<%
			}

			out.print(doc.getTitle());

			if (hasLink) {
				out.print("</a>");
			}

			if (isUlStyle) { %>
				</li>
			<% }
			else { %>
					</h4>
					<%
					// vlozenie obrazku pod nadpis alebo news_no_image odkaz
					if ("bottom".equals(image)) {
					%>
					<iwcm:notEmpty name="doc" property="perexImage">
						<div class="<%=perexImgClass%>">
							<% if (hasLink){ %>
								<a href="<%=perexLink%>">
							<%	}
							if (!perexImgClass.equals("news_no_image")) { %>
								<img src="<jsp:getProperty name="doc" property="perexImage"/>" alt="<%=doc.getTitle()%>" />
							<% }
							if (hasLink){ %>
								</a>
							<%	} %>
						</div>
					</iwcm:notEmpty>
					<iwcm:empty name="doc" property="perexImage">
						<div class="news_no_image">
						</div>
					</iwcm:empty>
					<% } %>

					<div class="news_text"<%=InlineEditor.getEditAttrs(request, doc, "perexPre", true) %>>
						<%
						// datum sa vlozi pred perex, ked je obrazok vlavo alebo nie je zobrazeny
						if ("top".equals(image) || "none".equals(image)) {
							if (date && Tools.isNotEmpty(doc.getPublishStartString()) ) {
								out.println("<span class=\"news_date\">" + doc.getPublishStartString() + "</span>");
							}

							if (place && Tools.isNotEmpty(doc.getPerexPlace())) {
								out.println("<span class=\"news_place\">" + doc.getPerexPlace() + "</span>");
							}
						}
						hasPerex = false;

						if (doc.getPerex()!=null && doc.getPerex().length()>5){
							hasPerex = true;
						}

						if (hasPerex && perex){
							if(truncate != -1)
							{
								String perexPre = SearchAction.htmlToPlain(doc.getPerexPre());
								if(perexPre.length() > truncate){	//truncate
									int index = perexPre.lastIndexOf(" ", truncate);
									//if je treba kvoli textu, ktory by bol dlhy 120 znakov bez medzery
									if (index > 0) perexPre = perexPre.substring(0, index)+"...";
									else perexPre = perexPre.substring(0, truncate)+"...";
								}
								out.println(perexPre);
							}
							else
							{
							%>
								<jsp:getProperty name="doc" property="perexPre"/>
							<%
							}
						}
						%>
					</div>
				</div>
			<%
			}

			if (counter==maxCols && !isUlStyle) {
				%>
				<div class="clearer">&nbsp;</div></div>
				<%
				counter = 0;
			}
			%>
	</iwcm:iterate>

	<%
	//ukonci posledny DIV
	if (maxCols>1 && counter!=0 && !isUlStyle) {
		%>
		<div class="clearer">&nbsp;</div></div>
		<%
	}
	%>

</iwcm:notEmpty>
<iwcm:empty name="novinky">
	<div class="no_news"><iwcm:text key="components.news.nonews" /></div>
</iwcm:empty>


<%
if (isUlStyle) {
	out.print("</ul>");
}

if (paging && (pagingStyle.equals("bottom") || pagingStyle.equals("both"))) { %>
	<!-- strankovanie (naraz sa zobrazi iba urceny pocet web stranok) -->
	<iwcm:present name="pages">
    <div class="news_pages news_pages_bottom">
      <iwcm:text key="calendar.page"/>:
      <iwcm:present name="prev">
      	${prev.value}${prev.label}</a>
      </iwcm:present>
      <iwcm:iterate id="page2" name="pages" type="sk.iway.iwcm.LabelValueDetails">
        <jsp:getProperty name="page2" property="value"/>[<jsp:getProperty name="page2" property="label"/>]<%if(page2.getValue().indexOf("<a")!=-1) out.print("</a>");%>&nbsp;
      </iwcm:iterate>
      <iwcm:present name="next">
      	${next.value}${next.label}</a>
      </iwcm:present>
    </div>
  </iwcm:present>
	<!-- koniec strankovania -->
<%
}

if (!isUlStyle) {
	out.print("</div>");
}
%>
