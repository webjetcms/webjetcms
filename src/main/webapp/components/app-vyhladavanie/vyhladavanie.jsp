
<%
	sk.iway.iwcm.Encoding
			.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*"%><%@ 
taglib
	prefix="iwcm" uri="/WEB-INF/iwcm.tld"%><%@ 
taglib prefix="iway"
	uri="/WEB-INF/iway.tld"%><%@ 
taglib prefix="display"
	uri="/WEB-INF/displaytag.tld"%><%@ 
taglib prefix="stripes"
	uri="http://stripes.sourceforge.net/stripes.tld"%><%@ 
taglib
	prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	DocDetails doc = (DocDetails)request.getAttribute("docDetails");
	int docId = ((doc==null)? -1 : doc.getDocId());
	String docTitle = ((doc==null)? " " : doc.getTitle());
	String docLink = ((doc==null)? " " : doc.getDocLink());
	
	PageParams pageParams = new PageParams(request);

%>

<%
if(!pageParams.getValue("customSearchId","").equals(' ')){
%>
<script>

if(includeCustomSearchScript !='no'){

var includeCustomSearchScript = 'no';
  (function() {
    var cx = '<%=pageParams.getValue("customSearchId","") %>';
    var gcse = document.createElement('script');
    gcse.type = 'text/javascript';
    gcse.async = true;
    gcse.src = (document.location.protocol == 'https:' ? 'https:' : 'http:') +
        '//cse.google.com/cse.js?cx=' + cx;
    var s = document.getElementsByTagName('script')[0];
    s.parentNode.insertBefore(gcse, s);
  })();}
</script>
<style>
.gsib_a {
    padding-top: 0;
}
input.gsc-search-button, input.gsc-search-button:hover, input.gsc-search-button:focus {
background-image: inherit;
box-sizing: content-box;
}
</style>
<div style="overflow: hidden">
	<gcse:search></gcse:search>
</div>
<%
}
%>
