<%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.inquiry.*,sk.iway.iwcm.*,java.io.*,java.util.*" %><%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %><%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %><%
//stranka pre includnutie aknety

//tuto mam parametre pre zobrazenie
PageParams pageParams = new PageParams(request);

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

String group = pageParams.getValue("group", "default");
int imagesLength = pageParams.getIntValue("imagesLength", 10);
String percentageFormat = pageParams.getValue("percentageFormat", "0.0");
String orderBy = pageParams.getValue("orderBy", "answer_id");
String order = pageParams.getValue("order", "ascending");
String width = pageParams.getValue("width", "100%");
String widthStyle = width;
if (widthStyle.endsWith("%")==false) widthStyle = widthStyle + "px";
String imgRootDir = "/components/inquiry/";
boolean displayTotalClicks = pageParams.getBooleanValue("totalClicks", false);

if (group != null) group = group.replace('+', ',');

try
{
   InputStream is = pageContext.getServletContext().getResourceAsStream("/images/inquiry/1.gif");
   if (is!=null)
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

int clicksTotal = 0;


//ak je true budu najskor starsie ankety a potom novsie
boolean listOrderAsc = pageParams.getBooleanValue("listOrderAsc", false);
List inquirys = InquiryDB.getOldInquiry(group, listOrderAsc);

if (inquirys==null || inquirys.size()<1)
{
	%><iwcm:text key="displaytag.basic.msg.empty_list"/><%
	return;
}

Iterator iter = inquirys.iterator();
InquiryBean iBean;
AnswerForm icForm;
while (iter.hasNext())
{
	icForm = (AnswerForm)iter.next();
	iBean = InquiryDB.getInquiry(icForm.getQuestionID(), imagesLength, percentageFormat, orderBy, orderAsc, request);

	if (iBean!=null && iBean.getCanAnswer()!=null && iBean.getAnswers().size()>1)
	{
	   iBean.setImgRootDir(imgRootDir);
	   request.setAttribute("inquiry", iBean);
	%>

   <table cellSpacing='8' cellPadding='0' width='"<%=width%>"' style="width: <%=widthStyle%>"  border='0' class="inquiry">
   <tr>
      <td>
         <strong><bean:write name="inquiry" property="question"/></strong>
         <br />
         <table>
         <logic:iterate id="answer" name="inquiry" property="answers" type="AnswerForm">
         	<tr>
            	<td>
            		<jsp:getProperty name="answer" property="answerString"/>
            	</td>
            	<td nowrap="" >
         			<jsp:getProperty name="answer" property="imagesBar"/>
         			&nbsp;<jsp:getProperty name="answer" property="percentageString"/>%
            	</td>
            </tr>
            <tr>
            	<td><%clicksTotal += answer.getAnswerClicks();%></td>
            <tr>
         </logic:iterate>
         	<%if (displayTotalClicks && request.getAttribute("displayResults")!=null){%>
         	<tr>
            	<td>&nbsp;</td>
            <tr>
         	<tr>
            	<td colspan="2" ><iwcm:text key="components.inquiry.total_clicks"/>: <%=clicksTotal%></td>
            <tr>
           <%}%>
         </table>
      </td>
   </tr>
   </table>
<% }
}
%>