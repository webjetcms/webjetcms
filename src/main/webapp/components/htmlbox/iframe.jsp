<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%@page import="sk.iway.iwcm.system.context.ContextFilter"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*" %>
<%@page import="sk.iway.iwcm.tags.support.ResponseUtils"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<iwcm:checkLogon admin="true" perms="menuWebpages"/>
<html>
<HEAD>
   <TITLE>IFRAME</TITLE>
   <META http-equiv="Content-Type" content="text/html; charset=windows-1250">
   <%
   String sessionStyles = (String)session.getAttribute("editorCssStyles");
   if (Tools.isNotEmpty(sessionStyles))
   {
      String styles[] = sessionStyles.split(",");
      for (String style : styles)
      {
      	System.out.println("IFRAME style: "+style);
      	out.println("<LINK rel='stylesheet' href='"+style+"'>");
      }
   }
   else
   {
   %>
   <LINK rel="stylesheet" href="<%=Constants.getString("editorPageCss")%>">
   <LINK rel="stylesheet" href="/css/editor.css">
   <% } %>
</HEAD>
<body id="WebJETEditor3Body">

<style type="text/css">
	img.preview {max-width: 100%; display: block; margin: 0; position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);}
</style>

<%
int docId = Tools.getIntValue(Tools.getRequestParameter(request, "docid"), -1);
boolean usePreviewImage = false;

IwcmFile thumbDir = new IwcmFile(Tools.getRealPath("/components/"+Constants.getInstallName()+"/htmlbox/thumbs/"));
	if (thumbDir.exists() && thumbDir.canRead())
	{
		IwcmFile styleFiles[] = thumbDir.listFiles();
		for (IwcmFile file : styleFiles)
		{
			if (file.getName().equals(docId+".png")) {
				usePreviewImage = true;

				break;
			}
		}
	}

try
{
	if (docId > 0)
	{
		//get requested document
		DocDB docDB = DocDB.getInstance();
		DocDetails doc = docDB.getDoc(docId);
		if (doc != null)
		{
			String text = doc.getData();
			if (ContextFilter.isRunning(request)) text = ContextFilter.addContextPath(request.getContextPath(), text);
			if(usePreviewImage)
			{
				out.println("<img src=\"/components/"+Constants.getInstallName()+"/htmlbox/thumbs/"+docId+".png\" class=\"preview\" >");
			}else{
				out.println(text);
			}
			out.println("<textarea id='_iframeHtmlData' style='display: none;'>"+ResponseUtils.filter(text)+"</textarea>");
		}
	}
}
catch (Exception e)
{
	sk.iway.iwcm.Logger.error(e);
}
%>
</body>
</html>
