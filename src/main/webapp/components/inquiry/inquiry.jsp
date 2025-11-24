<%@page import="sk.iway.iwcm.tags.WriteTag"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"
import="sk.iway.iwcm.inquiry.*,sk.iway.iwcm.*,java.io.*" %><%@
taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%@
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%

//stranka pre includnutie aknety

//tuto mam parametre pre zobrazenie
PageParams pageParams = new PageParams(request);

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

String group = pageParams.getValue("group", "default");
String style = pageParams.getValue("style", "01");
String color = pageParams.getValue("color", "01");
int imagesLength = pageParams.getIntValue("imagesLength", 10);
String percentageFormat = pageParams.getValue("percentageFormat", "0.0");
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
InquiryBean iBean = InquiryDB.getInquiry(group, imagesLength, percentageFormat, orderBy, orderAsc, request, random);
if (iBean==null || iBean.getCanAnswer()==null || iBean.getAnswers().size()<1)
{
	System.out.println("iBean je null");
   //skus nacitat default anketu
   iBean = InquiryDB.getInquiry("default", imagesLength, percentageFormat, orderBy, orderAsc, request, random);
}

if (displayVoteResults)
{
	request.setAttribute("displayVoteResults", "");
}
else
{
	if (request.getParameter("qID") != null && request.getParameter("aID") != null)
	{
		//out.println("<h2>ZOBRAZIM VYSLEDKY</h2>");
		request.setAttribute("displayResults", "");
	}
}

int qID = -1;
int clicksTotal = 0;

/*  treba predat pageParams aj divku, ktore budeme ajaxovo zobrazovat po zahlasovani
 *  najjednoduchsia moznost je cez session. Ak niekto pride na lepsie riesenie, tak zmente
 *
 *  JRASKA edit: do nazvu atributu aj ID divka pridavam questionID, umozni to tak mat na stranke viac nez jednu anketu
 */

if(iBean!=null && iBean.getAnswers().size()>0)
{
	qID = ((AnswerForm)iBean.getAnswers().get(0)).getQuestionID();
}
session.setAttribute("inquiryPageParams-"+qID,pageParams);

if (iBean!=null && iBean.getCanAnswer()!=null && iBean.getAnswers().size()>1)
{
   iBean.setImgRootDir(imgRootDir);
   request.setAttribute("inquiry", iBean);
   if("01".equals(style)){ %>
      <link type="text/css" href="/components/inquiry/inquiry.css" rel="stylesheet"/>
   <%} else if("02".equals(style)){ %>
      <link type="text/css" href="/components/inquiry/inquiry_bootstrap.css" rel="stylesheet"/>
   <%} else if("03".equals(style)){ %>
      <link type="text/css" href="/components/inquiry/inquiry_bootstrap2.css" rel="stylesheet"/>
   <%}%>
	<%=Tools.insertJQuery(request) %>
	<script type="text/javascript" src="/components/inquiry/ajax_vote.jsp"></script>
   <div class="inquiry" id="resultsDiv-<%=qID %>" style="width:<%=widthStyle%>">
        <jsp:include flush="true" page='<%="/components/inquiry/voteResultsDiv.jsp?qID="+qID %>' />
   </div>
   <%
}

WriteTag.setInlineComponentEditTextKey("components.inquiry.inline.title", request);
%>