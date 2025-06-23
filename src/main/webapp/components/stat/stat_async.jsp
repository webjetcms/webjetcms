<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);

try
{
	//vlozi do stranky JavaScript pre asynchronne zapisovanie statistiky, pouziva sa spolu s rezimom nginxProxyMode
	Integer docId = (Integer)request.getAttribute("doc_id");
	int groupId = Tools.getIntValue((String)request.getAttribute("group_id"), -1);

	if (docId == null || docId.intValue() < 0 || groupId < 0) return;

	long now = Tools.getNow();
	%>
	<script type="text/javascript">
	  (function() {
	    var po = document.createElement('script'); po.type = 'text/javascript'; po.async = true;
	    po.src = '/components/stat/stat_async_ajax.jsp?d=<%=docId.intValue()%>&g=<%=groupId%>&t=<%=now%>';
	    var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(po, s);
	  })();
	</script>
	<%
}
catch (Exception ex)
{
	sk.iway.iwcm.Logger.error(ex);
}
%>