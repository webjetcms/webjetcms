<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"
import="sk.iway.iwcm.inquiry.*,sk.iway.iwcm.*,org.apache.struts.util.ResponseUtils,java.io.*" %><%@page import="sk.iway.iwcm.users.UsersDB"%><%@
taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%@
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@
taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%

//tuto mam parametre pre zobrazenie
int qidParam = Tools.getIntValue(request.getParameter("qID"),-1);

PageParams pageParams = (PageParams) session.getAttribute("inquiryPageParams-"+qidParam);
if (pageParams == null)
{
	//nemame session, vrat nieco zmysluplne
	%><iwcm:text key="inquiry.errorLoadingData"/><%
	return;
}

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

String group = pageParams.getValue("group", "default");
String style = pageParams.getValue("style", "01");
String color = pageParams.getValue("color", "01");
int imagesLength = pageParams.getIntValue("imagesLength", 10);
String percentageFormat = pageParams.getValue("percentageFormat", "0.0");
//ak nebude nula, tak sa bude zobrazovat ako desatinne cislo...co nabura style="width: ...
percentageFormat = "0";
String orderBy = pageParams.getValue("orderBy", "answer_id");
String order = pageParams.getValue("order", "ascending");
String width = pageParams.getValue("width", "100%");
String widthStyle = width;
if (widthStyle.endsWith("%")==false) widthStyle = widthStyle + "px";
String imgRootDir = "/components/inquiry/";
boolean random = pageParams.getBooleanValue("random", true);
boolean displayTotalClicks = pageParams.getBooleanValue("totalClicks", false);
boolean displayVoteResults = pageParams.getBooleanValue("displayVoteResults", true);


if (group != null) group = group.replace('+', ',');

try
{
 if (sk.iway.iwcm.FileTools.isFile("/images/inquiry/1.gif"))
 {
    imgRootDir = "/images/inquiry/";
 }
}
catch (Exception ex)
{
 sk.iway.iwcm.Logger.error(ex);
}

boolean orderAsc = true;
if ("descending".equalsIgnoreCase(order))
{
 orderAsc = false;
}

if (group.equals("fromTemplate"))
{
 try
 {
    group = "default";
    String afterBodyData = (String)request.getAttribute("after_body");
    if (afterBodyData!=null)
    {
       int start = afterBodyData.indexOf("<inquiry group=\"");
       if (start!=-1)
       {
          start+=15;
          int end = afterBodyData.indexOf("\"", start+1);
          if (end>start)
          {
             group = afterBodyData.substring(start, end);
          }
       }
    }
 }
 catch (Exception ex)
 {
    sk.iway.iwcm.Logger.error(ex);
 }
}
InquiryBean iBean = null;
Cache c = Cache.getInstance();
String cacheKey = "components.inquiry.group."+group;
int cacheInMinutes = Constants.getInt("inquiryGroupCacheInMinutes");
if(cacheInMinutes == -1)
	cacheInMinutes = 10;

if(group.indexOf(",") == -1 && random == false && c.getObject(cacheKey) != null)
{
	iBean = (InquiryBean)c.getObject(cacheKey);
}

Identity user = UsersDB.getCurrentUser(request);
if (user != null && user.isAdmin()) iBean = null;

if(iBean == null)
{
	iBean = InquiryDB.getInquiry(group, imagesLength, percentageFormat, orderBy, orderAsc, request, random);
	if (iBean==null || iBean.getCanAnswer()==null || iBean.getAnswers().size()<1)
	{
		 iBean = null;
		 System.out.println("iBean je null");
		 //skus nacitat default anketu
		 group = "default";
		 cacheKey = "components.inquiry.group."+group;
		 //skusim najprv nacitat z cache
		 if(random == false && c.getObject(cacheKey) != null)
		 {
			iBean = (InquiryBean)c.getObject(cacheKey);
		 }
		 if (user != null && user.isAdmin()) iBean = null;
		 if(iBean == null)
		 {
			 iBean = InquiryDB.getInquiry(group, imagesLength, percentageFormat, orderBy, orderAsc, request, random);
		 }

	}
	//cachuje sa objekt len vtedy ked group neobsahuje viac skupin, random je false a iBena je rozne od null
	if(group.indexOf(",") == -1 && random == false && iBean!=null && iBean.getCanAnswer()!=null && iBean.getAnswers().size()>= 0)
	{
		c.setObjectSeconds(cacheKey, iBean , cacheInMinutes*60, true);
	}
}

if (displayVoteResults)
{
	request.setAttribute("displayResults", "");
}
else
{
	if ("true".equals(request.getParameter("showResults")))
	{
		//out.println("<h2>ZOBRAZIM VYSLEDKY</h2>");
		request.setAttribute("displayResults", "");
	}
}

int qID = -1;
int clicksTotal = 0;

if (iBean!=null && iBean.getCanAnswer()!=null && iBean.getAnswers().size()>= 0)
{
 iBean.setImgRootDir(imgRootDir);
 request.setAttribute("inquiry", iBean);
%>
<% if("01".equals(style)){ %>
       <h3><iwcm:strutsWrite name="inquiry" property="question" filter="false"/></h3>
		<%
			if(!iBean.isMultiple())
			{
		%>
			<div class="inquiryBox">
				<iwcm:iterate id="answer" name="inquiry" property="answers" type="AnswerForm">
					<div class="inquiryAnswer">
						<a href='javascript:anketa("/inquiry.answer.do?qID=<jsp:getProperty name="answer" property="questionID"/>&amp;aID=<jsp:getProperty name="answer" property="answerID"/>", 300, 200,<jsp:getProperty name="answer" property="questionID"/><%
              				if (Tools.isNotEmpty(answer.getUrl())) { out.print(", \"" + ResponseUtils.filter(answer.getUrl()) + "\""); }
              			%>);'>
							<jsp:getProperty name="answer" property="answerString"/>
							<iwcm:present name="displayResults">
							<br />
							<%
								if (Tools.isNotEmpty(answer.getImagePath()))
									out.println("<img src=\"" + answer.getImagePath() + "\" alt=\"\" width=\"100\" />");
							%>
							</iwcm:present>
						</a>
						<iwcm:present name="displayResults">
							<div class="bar_style">
								<div class="bar_fill" style="width:<iwcm:strutsWrite name="answer" property="percentageString"/>%">
								</div>
							</div>
							<span><jsp:getProperty name="answer" property="percentageString"/>%</span>
						</iwcm:present>
						<div class="clearer">&nbsp;</div>
					</div>
					<%clicksTotal += answer.getAnswerClicks();%>
				</iwcm:iterate>
			</div>
		<%
			}
			else
			{
		%>
			<form action="/inquiry.answer.do" name="inquiryAnswerForm">
				<div class="inquiryBox">
				<iwcm:iterate id="answer" name="inquiry" property="answers" type="AnswerForm">
					<div class="inquiryAnswer">
						<label>
							<input type="checkbox" name="selectedAnswers" value="${answer.answerID}"/>
							<jsp:getProperty name="answer" property="answerString"/>
							<iwcm:present name="displayResults">
							<%
								if (Tools.isNotEmpty(answer.getImagePath()))
									out.println("<img src=\"" + answer.getImagePath() + "\" alt=\"\" width=\"100\" />");
							%>
							</iwcm:present>
						</label>
						<iwcm:present name="displayResults">
							<div class="bar_style">
								<div class="bar_fill" style="width:<iwcm:strutsWrite name="answer" property="percentageString"/>%">&nbsp;</div>
							</div>
							<span><jsp:getProperty name="answer" property="percentageString"/>%</span>
						</iwcm:present>
						<div class="clearer">&nbsp;</span></div>
					</div>
				</iwcm:iterate>
				</div>
				<input type="hidden" name="questionID" value="<iwcm:strutsWrite name="answer" property="questionID" />"/>
		      	<input type="button" onclick="anketaMulti(this.form, questionID);" name="maSubmit" value="<iwcm:text key="inquiry.multipleAnswer.submit"/>" class="button100"><br>
			</form>
		<%
			clicksTotal = iBean.getTotalClicksMultiple();
			}
		%>

		<%if (displayTotalClicks){%>
			<div class="total"><iwcm:text key="components.inquiry.total_clicks"/>: <%=clicksTotal %></div>
		<%}%>
		<div class="clearer"></div>
	<%} else if("02".equals(style)){ %>
		<%
			if(!iBean.isMultiple())
			{
		%>
		<div class="span6 inquiryBox">
			<h3><iwcm:strutsWrite name="inquiry" property="question" filter="false"/></h3>
			<iwcm:iterate id="answer" name="inquiry" property="answers" type="AnswerForm">
				<div>
					<iwcm:present name="displayResults">
					<%
						if (Tools.isNotEmpty(answer.getImagePath()))
							out.println("<img src=\"" + answer.getImagePath() + "\" alt=\"\" />");
					%>
					</iwcm:present>
					<strong>
						<a href='javascript:anketa("/inquiry.answer.do?qID=<jsp:getProperty name="answer" property="questionID"/>&amp;aID=<jsp:getProperty name="answer" property="answerID"/>", 300, 200,<jsp:getProperty name="answer" property="questionID"/><%
			  				if (Tools.isNotEmpty(answer.getUrl())) { out.print(", \"" + ResponseUtils.filter(answer.getUrl()) + "\""); }
			  			%>);'>
							<jsp:getProperty name="answer" property="answerString"/>
						</a>
					</strong>
					<iwcm:present name="displayResults">
					<span class="pull-right"><jsp:getProperty name="answer" property="percentageString"/>%</span>
					<div class="progress progress-<% if("01".equals(color)){ %>info<%} else if("02".equals(color)){ %>danger<%} else if("03".equals(color)){ %>success<%} else if("04".equals(color)){ %>warning<%}%> active">
						<div class="bar" style="width: <iwcm:strutsWrite name="answer" property="percentageString"/>%;"></div>
					</div>
					</iwcm:present>
					<%clicksTotal += answer.getAnswerClicks();%>
				</div>
			</iwcm:iterate>
		</div>
		<%
			}
			else
			{
		%>
			<form action="/inquiry.answer.do" name="inquiryAnswerForm">
				<div class="span6 inquiryBox">
					<h3><iwcm:strutsWrite name="inquiry" property="question" filter="false"/></h3>
					<iwcm:iterate id="answer" name="inquiry" property="answers" type="AnswerForm">
						<div>
							<label>
							<iwcm:present name="displayResults">
							<%
								if (Tools.isNotEmpty(answer.getImagePath()))
									out.println("<img src=\"" + answer.getImagePath() + "\" alt=\"\" />");
							%>
							</iwcm:present>
					  				<input type="checkbox" name="selectedAnswers" value="${answer.answerID}"/>
									<jsp:getProperty name="answer" property="answerString"/>
							</label>
							<iwcm:present name="displayResults">
							<span class="pull-right"><jsp:getProperty name="answer" property="percentageString"/>%</span>
							<div class="progress progress-<% if("01".equals(color)){ %>info<%} else if("02".equals(color)){ %>danger<%} else if("03".equals(color)){ %>success<%} else if("04".equals(color)){ %>warning<%}%> active">
								<div class="bar" style="width: <iwcm:strutsWrite name="answer" property="percentageString"/>%;"></div>
							</div>
							</iwcm:present>
						</div>
					</iwcm:iterate>
					<p><input type="hidden" name="questionID" value="<iwcm:strutsWrite name="answer" property="questionID" />"/>
			      	<input type="button" onclick="anketaMulti(this.form, questionID);" name="maSubmit" value="<iwcm:text key="inquiry.multipleAnswer.submit"/>" class="btn btn-large btn-<% if("01".equals(color)){ %>info<%} else if("02".equals(color)){ %>danger<%} else if("03".equals(color)){ %>success<%} else if("04".equals(color)){ %>warning<%}%>"></p>
				</div>
			</form>
		<%
			clicksTotal = iBean.getTotalClicksMultiple();
			}
		%>

		<%if (displayTotalClicks){%>
			<div class="total"><p><iwcm:text key="components.inquiry.total_clicks"/>: <%=clicksTotal %></p></div>
		<%}%>
		<div class="clearer"></div>

	<%} else if("03".equals(style)){ %>

	<%
			if(!iBean.isMultiple())
			{
		%>
		<div class="panel panel-primary">
			<div class="panel-heading">
				<h3 class="panel-title">
					<iwcm:strutsWrite name="inquiry" property="question" filter="false"/>
				</h3>
			</div>
			<div class="panel-body">
				<div id="Main">
					<iwcm:iterate id="answer" name="inquiry" property="answers" type="AnswerForm">
						<div>
							<iwcm:present name="displayResults">
							<%
								if (Tools.isNotEmpty(answer.getImagePath()))
									out.println("<img src=\"" + answer.getImagePath() + "\" alt=\"\" />");
							%>
							</iwcm:present>
							<a href='javascript:anketa("/inquiry.answer.do?qID=<jsp:getProperty name="answer" property="questionID"/>&amp;aID=<jsp:getProperty name="answer" property="answerID"/>", 300, 200,<jsp:getProperty name="answer" property="questionID"/><%
			  				if (Tools.isNotEmpty(answer.getUrl())) { out.print(", \"" + ResponseUtils.filter(answer.getUrl()) + "\""); }
			  				%>);' name="poll_bar" style="
			  				<iwcm:present name="displayResults">
			  				 width: <iwcm:strutsWrite name="answer" property="percentageString"/>%;
			  				 </iwcm:present>
			  				 " class="poll_bar btn btn-default btn-sm btn-<% if("01".equals(color)){ %>info<%} else if("02".equals(color)){ %>danger<%} else if("03".equals(color)){ %>success<%} else if("04".equals(color)){ %>warning<%}%>">
			  				 	<jsp:getProperty name="answer" property="answerString"/>
			  				 </a>
							<iwcm:present name="displayResults">
								<span name="poll_val"><jsp:getProperty name="answer" property="percentageString"/>%</span>
							</iwcm:present>
							<br/>
						</div>
						<%clicksTotal += answer.getAnswerClicks();%>
					</iwcm:iterate>
				</div>
				<%if (displayTotalClicks){%>
					<div class="total"><p><iwcm:text key="components.inquiry.total_clicks"/>: <%=clicksTotal %></p></div>
				<%}%>
			</div>
		</div>
		<%
			}
			else
			{
		%>
		<form action="/inquiry.answer.do" name="inquiryAnswerForm">
		<div class="panel panel-primary">
			<div class="panel-heading">
				<h3 class="panel-title">
					<iwcm:strutsWrite name="inquiry" property="question" filter="false"/>
				</h3>
			</div>
			<div class="panel-body">
				<div id="Main">
					<iwcm:iterate id="answer" name="inquiry" property="answers" type="AnswerForm">
						<div>
							<label class="inquiry_label">
								<iwcm:present name="displayResults">
								<%
									if (Tools.isNotEmpty(answer.getImagePath()))
										out.println("<img src=\"" + answer.getImagePath() + "\" alt=\"\" />");
								%>
								</iwcm:present>
								<input type="checkbox" name="selectedAnswers" value="${answer.answerID}"/>
								<a  name="poll_bar" style="
									<iwcm:present name="displayResults">
									width: <iwcm:strutsWrite name="answer" property="percentageString"/>%;
									</iwcm:present>
									" class="poll_bar btn btn-default btn-sm btn-<% if("01".equals(color)){ %>info<%} else if("02".equals(color)){ %>danger<%} else if("03".equals(color)){ %>success<%} else if("04".equals(color)){ %>warning<%}%>">
									<jsp:getProperty name="answer" property="answerString"/>
								</a>
								<iwcm:present name="displayResults">
									<span name="poll_val"><jsp:getProperty name="answer" property="percentageString"/>%</span>
								</iwcm:present>
								<br/>
							</label>
						</div>
						<%clicksTotal += answer.getAnswerClicks();%>
					</iwcm:iterate>
					<p><input type="hidden" name="questionID" value="<iwcm:strutsWrite name="answer" property="questionID" />"/>
			      	<input type="button" onclick="anketaMulti(this.form, questionID);" name="maSubmit" value="<iwcm:text key="inquiry.multipleAnswer.submit"/>" class="btn btn-large btn-<% if("01".equals(color)){ %>info<%} else if("02".equals(color)){ %>danger<%} else if("03".equals(color)){ %>success<%} else if("04".equals(color)){ %>warning<%}%>"></p>
				</div>
				<%if (displayTotalClicks){%>
					<div class="total"><p><iwcm:text key="components.inquiry.total_clicks"/>: <%=clicksTotal %></p></div>
				<%}%>
			</div>
		</div>
		</form>
		<%
			clicksTotal = iBean.getTotalClicksMultiple();
			}
		%>
	<% } %>
<% } %>