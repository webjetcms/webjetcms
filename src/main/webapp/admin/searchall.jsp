<%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.doc.*,java.util.*"%>
<%@ page import="org.apache.struts.util.ResponseUtils" %>
<%@ page import="sk.iway.iwcm.Tools" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%
//otestuj ci existuje nahrada za tuto stranku
String forward = WriteTag.getCustomPageAdmin("/admin/searchall.jsp", request);
if (forward!=null)
{
	pageContext.forward(forward);
	return;
}
%>
<iwcm:menu notName="menuWebpages">
	<%
		response.sendRedirect("/admin/403.jsp");
		if (1==1) return;
	%>
</iwcm:menu>
<%@ include file="layout_top.jsp" %>
<%
String text = Tools.getRequestParameter(request, "text");
DocDB docDB = DocDB.getInstance();
GroupsDB groupsDB = GroupsDB.getInstance();
if (text == null)
{
   text = "";
}
else
{
   List pages = docDB.searchTextAll(text);
   request.setAttribute("pages", pages);
}
%>
<h3><iwcm:text key="searchall.title"/></h3>
<form name="searchForm" action="searchall.jsp" method="post">
	<iwcm:text key="searchall.text"/>
	<textarea class="input" name='text' rows=5 cols=80><%=text%></textarea>
	<input type="submit" name="bSubmit" class="button50" value="<iwcm:text key="button.ok"/>">
</form>

<logic:present name="pages">

	<SCRIPT LANGUAGE="JavaScript" src="scripts/divpopup.js"></SCRIPT>
	<script language="javascript">
	<!--
	//toto treba zadefinovat v stranke po includnuti divpopup.js
	//je to offset o ktory sa posuva okno vlavo
	leftOffset=-445;
	//a toto ofset o ktory sa posuva nadol
	topOffset=10;

	//popup sa potom vola:
	//popupDIV(url);

	function select(value)
	{
		//none
	}

	  function deleteOK(text,obj,url)
	  {
		 if(confirm(text)){
			obj.href=url;
			}
	  }

	function openWebJETEditor(docId)
	{
	   var url = "editor.do?docid="+docId;
	   var options = "toolbar=no,scrollbars=yes,resizable=yes,width=750,height=500;"
	   popupWindow=window.open(url,"_blank",options);
	}
	//-->
	</script>

	<link rel="stylesheet" type="text/css" href="css/tablesort.css" />

   	<br><br>

	<b><iwcm:text key="searchall.results"/>:</b><br>
	<table class="sort_table" border=0 cellspacing=0 cellpadding=0>
		<tr>
			<td class="sort_thead_td"><iwcm:text key="editor.title"/></td>
			<td class="sort_thead_td"><iwcm:text key="groupslist.approve.authorName"/></td>
			<td class="sort_thead_td"><iwcm:text key="groupslist.approve.date"/></td>
			<td class="sort_thead_td"><iwcm:text key="groupslist.approve.tools"/></td>
		</tr>
		<logic:iterate name="pages" id="doc" type="sk.iway.iwcm.doc.DocDetails">
			<tr>
				<td>
					<a href="<%=docDB.getDocLink(doc.getDocId(), request)%>" target="_blank"><img src="images/icon_preview.gif" width="15" height="16" border=0 ALT="<iwcm:text key="groupslist.show_web_page"/>"></a>&nbsp;
					<jsp:getProperty name="doc" property="sortPriority"/>.&nbsp;
					<logic:notEqual name="doc" property="passwordProtected" value=""><img src="images/lock.gif"></logic:notEqual><a class="groups<jsp:getProperty name="doc" property="available"/>" href="javascript:openWebJETEditor(<jsp:getProperty name="doc" property="docId"/>);"><jsp:getProperty name="doc" property="title"/></a>
					<logic:notEmpty name="doc" property="publishStartString">
						<jsp:getProperty name="doc" property="publishStartString"/> <jsp:getProperty name="doc" property="publishStartTimeString"/>
					</logic:notEmpty>
					<logic:notEmpty name="doc" property="publishEndString">
						<logic:notEmpty name="doc" property="publishStartString">
							-
						</logic:notEmpty>
						<jsp:getProperty name="doc" property="publishEndString"/> <jsp:getProperty name="doc" property="publishEndTimeString"/>
					</logic:notEmpty>
				</td>
				<td>
					&nbsp;<jsp:getProperty name="doc" property="authorName"/>&nbsp;
				</td>
				<td nowrap>
					&nbsp;<jsp:getProperty name="doc" property="dateCreatedString"/>
					<jsp:getProperty name="doc" property="timeCreatedString"/>&nbsp;
				</td>
				<td class="sort_td" style="border-bottom: 0px;" nowrap>
					&nbsp;<a href="javascript:openWebJETEditor(<jsp:getProperty name="doc" property="docId"/>);"><img src="images/icon_edit.gif" width="15" height="16" border=0 ALT="<iwcm:text key="groupslist.edit_web_page"/>"></a>
					&nbsp;<a href='javascript:popupDIV("dochistory.jsp?docid=<jsp:getProperty name="doc" property="docId"/>")'><img src="images/icon_history.gif" width="16" height="16" border=0 ALT="<iwcm:text key="groupslist.show_history"/>"></a>
				</td>
			</tr>
			<tr>
				<td class="sort_td" colspan=4 style="border-bottom: 1px solid #666666; color: #777777;"><%=groupsDB.getNavbarNoHref(doc.getGroupId())%></td>
			</tr>
		</logic:iterate>
	</table>



	<div id="divPopUp" style="position:absolute; width:450px; height:100px; z-index:130; left: 71px; top: 146px; visibility: hidden">
		<table width=450 bgcolor="white" cellspacing=0 cellpadding=0>
		<tr><td align="left" bgcolor="#CCCCFF"><small><iwcm:text key="groupslist.web_page_history"/></small></td><td align="right" bgcolor="#CCCCFF"><a href="javascript:popupHide();"><small><b>[X]</b></small></a></td></tr>
		<tr><td valign="top" colspan=2>
		<iframe src="divpopup-blank.jsp" name="popupIframe" style="border:solid #000000 1px" width="448" height="130" align="left" marginwidth="0" marginheight="0" frameborder="0" scrolling="auto"></iframe>
		</td></tr>
		</table>
	</div>

</logic:present>
<%@ include file="layout_bottom.jsp" %>
