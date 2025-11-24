<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/displaytag.tld" prefix="display" %>
<%@page import="sk.iway.iwcm.Tools"%>
<%@page import="sk.iway.iwcm.doc.DocDB"%>
<%@ page import="sk.iway.iwcm.doc.DocDetails" %>
<iwcm:checkLogon admin="true"/>
<iwcm:menu notName="menuWebpages">
	<%
	response.sendRedirect("/admin/403.jsp");
	if (true) return;
	%>
</iwcm:menu>
<iwcm:menu name="menuWebpages">
<html>
<head>
<meta http-equiv="Content-type" content="text/html;charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>">
</head>

<script type="text/javascript" >
function  syncScroll()
{
	if (window.frames["left"].scrollX!=undefined)
	{
		parent.frames["right"].scrollTo(parent.frames["left"].scrollX,parent.frames["left"].scrollY);
	}
	else if (window.frames["left"].document.documentElement)
	{
    	window.frames["right"].scrollTo(window.frames["left"].document.documentElement.scrollLeft,window.frames["left"].document.documentElement.scrollTop);
  	}
	else
	{
		window.frames["right"].scrollTo(parent.window.frames["left"].document.body.scrollLeft,parent.window.frames["left"].document.body.scrollTop);
	}
}
function  syncScrollRight()
{
	if (parent.frames["right"].scrollX!=undefined)
	{
		window.frames["left"].scrollTo(parent.frames["right"].scrollX,parent.frames["right"].scrollY);
	}
	else if (window.frames["right"].document.documentElement)
	{
		window.frames["left"].scrollTo(window.frames["right"].document.documentElement.scrollLeft,window.frames["right"].document.documentElement.scrollTop);
  	}
	else
	{
		window.frames["left"].scrollTo(parent.window.frames["right"].document.body.scrollLeft,parent.window.frames["right"].document.body.scrollTop);
	}
}
function setupScroll()
{
	window.frames["right"].window.onscroll=syncScrollRight; //firefox
	window.frames["right"].document.body.onscroll=syncScrollRight; //ie
	if (window.frames["right"].document.documentElement) window.frames["right"].document.documentElement.onscroll=syncScrollRight;
	window.frames["left"].window.onscroll=syncScroll;
	window.frames["left"].document.body.onscroll=syncScroll;
	if (window.frames["left"].document.documentElement) window.frames["left"].document.documentElement.onscroll=syncScroll;
}
</script>

<%
int docId = Tools.getIntValue(Tools.getRequestParameter(request, "docid"), -1);
int historyId = Tools.getIntValue(Tools.getRequestParameter(request, "historyid"),-1);
String onlyBody = Tools.getRequestParameter(request, "onlyBody") != null ? "&forwarddoccompare=true" : "";
String domain = DocDB.getInstance().getDomain(docId);
if (Tools.isNotEmpty(domain))
{
	session.setAttribute("preview.editorDomainName", domain);
}

String rightFrameKey = "editor.compafe.history_version";
if (request.getAttribute("isApprove")!=null) rightFrameKey = "editor.compare.to_approve_version";


String framesetSize = "*,*";

//overenie, ci sa nejedna o novu stranku
    {
        DocDB docDB = DocDB.getInstance();
        DocDetails doc = docDB.getDoc(docId);
        if (doc != null)
        {
            DocDetails history = docDB.getDoc(docId, historyId, false);
			if (history != null) {
				//out.println(doc.getDateCreated()+" vs " + history.getDateCreated());
				long abs = Math.abs(doc.getDateCreated() - history.getDateCreated());
				if (abs < (1000))
				{
					request.setAttribute("historyEqualsDoc", "1");
					framesetSize = "*";
				}
			}
        }
    }
%>

<iwcm:present name="isApprove">
	<frameset rows="*,100" cols="*" BORDERCOLOR="#edeff1" border="1" frameborder="1" framespacing="0">
</iwcm:present>

	<frameset cols="<%=framesetSize%>" onload="setupScroll()">
        <iwcm:empty name="historyEqualsDoc">
		<frameset rows="75,*" cols="*" BORDERCOLOR="#edeff1" border="1" frameborder="1" framespacing="0">
		    <frame name="leftTop" src="/admin/doc_compare_top.jsp?textKey=editor.compare.actual_version&docid=<%=docId %>&historyid=<%=historyId %>&actual=true<%=onlyBody%>" scrolling="no" BORDERCOLOR="#edeff1">
			 <frame name="left" id="left"  src="/showdoc.do?docid=<%=Tools.getIntValue(Tools.getRequestParameter(request, "docid"),-1)%>&NO_WJTOOLBAR=true<%=onlyBody%>">
		</frameset>
        </iwcm:empty>
		<frameset rows="75,*" cols="*" BORDERCOLOR="#edeff1"  border="1" frameborder="1" framespacing="0">
			<frame name="rightTop" src="/admin/doc_compare_top.jsp?textKey=<%=rightFrameKey %>&docid=<%=docId %>&historyid=<%=historyId+onlyBody%>" scrolling="no" BORDERCOLOR="#edeff1">
			<frame name="right"  id="right"  src="/showdoc.do?docid=<%=Tools.getIntValue(Tools.getRequestParameter(request, "docid"),-1)%>&historyid=<%=historyId%>&NO_WJTOOLBAR=true<%=onlyBody%>">
		</frameset>
	</frameset>

<iwcm:present name="isApprove">
		<frame name="approveFormId"  id="approveFormId" src="/admin/approve_form.jsp?historyid=<%=Tools.getRequestParameter(request, "historyid")%>" scrolling="no" >
	</frameset>
</iwcm:present>


<noframes><body bgcolor="#FFFFFF" text="#000000">

</body></noframes>
</html>
</iwcm:menu>
