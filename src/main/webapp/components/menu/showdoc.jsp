<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*"%><%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%
//stranka pre includnutie inej stranky
PageParams pageParams = new PageParams(request);
int docId = pageParams.getIntValue("docId", -1);

if(docId == -1)
	docId = Tools.getIntValue(request.getParameter("docid"), -1);

if (docId < 0)
	return;

try
{
	//get requested document
	DocDB docDB = DocDB.getInstance();
	DocDetails doc = docDB.getDoc(docId);
	if (doc != null)
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
		String key = "showdoc."+docId;
		request.setAttribute(key, text);
		%><iwcm:write name="<%=key%>"/><%
		request.removeAttribute(key);
	}
}
catch (Exception e)
{
	sk.iway.iwcm.Logger.error(e);
}

%>