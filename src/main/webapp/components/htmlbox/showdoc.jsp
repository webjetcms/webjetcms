<%@page import="sk.iway.iwcm.editor.InlineEditor"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*"%>
<%@ page import="sk.iway.spirit.MediaDB" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%
//stranka pre includnutie inej stranky
PageParams pageParams = new PageParams(request);
int docId = pageParams.getIntValue("docid", -1);

if (docId < 0)	return;

try
{
	//get requested document
	DocDB docDB = DocDB.getInstance();
	DocDetails doc = docDB.getDoc(docId);
	if (doc != null && doc.isAvailable())
	{
		Identity user = null;
		try
		{
			//ziskaj meno lognuteho usera
			if (session.getAttribute(Constants.USER_KEY) != null)
			{
				user = (Identity) session.getAttribute(Constants.USER_KEY);
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		// ------------ HEADER
		String text = doc.getData();
		if (text != null)
		{
			try
			{
				text = ShowDoc.updateCodes(user, text, docId, request, sk.iway.iwcm.Constants.getServletContext());
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}
		}

		//pageContext.getOut().write((String) pageContext.getRequest().getAttribute("doc_header"));
		request.setAttribute("showdocIncludeText", text);

		String includeName = (String)request.getAttribute("writeTagName");
		//System.out.println("getting wjEditorFullpageNews "+request.getAttribute("wjEditorFullpageNews"));
		if (includeName!=null&&includeName.startsWith("doc_") && "true".equals(request.getAttribute("wjEditorFullpageNews"))) includeName="fullpage_"+includeName;

		%><div<%

		if (includeName != null && includeName.startsWith("doc_")==false)
		{
			out.print(InlineEditor.getEditAttrs(request, doc, null, true));
		}
		%>><iwcm:write name="showdocIncludeText"/></div><%
	}
}
catch (Exception e)
{
	sk.iway.iwcm.Logger.error(e);
}

%>