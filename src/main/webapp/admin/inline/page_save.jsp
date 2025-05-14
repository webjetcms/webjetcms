<%@page import="org.apache.commons.beanutils.BeanUtils"%><%@page import="sk.iway.iwcm.helpers.RequestDump"%><%
	sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"
	import="sk.iway.iwcm.*,sk.iway.iwcm.users.UsersDB,sk.iway.iwcm.editor.*,sk.iway.iwcm.i18n.Prop"%><%@
taglib prefix="iwcm"
	uri="/WEB-INF/iwcm.tld"%><%@
taglib prefix="iway"
	uri="/WEB-INF/iway.tld"%><%@
taglib prefix="display"
	uri="/WEB-INF/displaytag.tld"%><%@
taglib prefix="stripes"
	uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib
	prefix="c" uri="http://java.sun.com/jsp/jstl/core"%><iwcm:checkLogon admin="true" perms="menuWebpages"/><%!


	private void setField(String name, EditorForm ef, HttpServletRequest request)
	{
		String value = Tools.getStringValue(Tools.getRequestParameterUnsafe(request, name),null);
		if (value != null)
		{
			try
			{
				BeanUtils.setProperty(ef, name, value);
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}
		}
	}

	%><%

Identity user = UsersDB.getCurrentUser(request);
Prop prop = Prop.getInstance(request);

if (user == null || user.isAdmin()==false)
{
	out.print(prop.getText("error.userNotLogged"));
	return;
}

//debugni parametre
RequestDump rd = new RequestDump(request);
System.out.println( rd.completeRequestReport() );

//uloz doc data z json suboru
String json = Tools.getRequestParameterUnsafe(request, "json");
if (Tools.isNotEmpty(json))
{
	System.out.println(json);
	InlineEditor inline = new InlineEditor(json, request);
	inline.save();

	String errMesage = inline.getErrorMessage().trim();
	out.print(errMesage);
	return;
}

//uloz data z formularu (TOTO JE UZ ASI ABANDONED)
int docId = Tools.getIntValue(Tools.getRequestParameter(request, "docid"), -2);
int groupId = Tools.getIntValue(Tools.getRequestParameter(request, "groupid"), -2);

boolean available = Tools.getBooleanValue(Tools.getRequestParameter(request, "available"),false);
String publish =  Tools.getStringValue(Tools.getRequestParameter(request, "publish"),null);

if(docId==0){
	docId=-1;
}

if (docId > 0 || (docId == -1 && groupId > 0))
{
	EditorForm ef = EditorDB.getEditorForm(request, docId, -1, groupId);
	if (ef != null && ef.getDocId()==docId)
	{

		ef.setAvailable(available);

		if(publish != null) ef.setPublish(publish);
		else ef.setPublish("1");

		setField("title", ef, request);
		//setField("passwordProtected", ef, request);
		setField("htmlHead", ef, request);
		setField("htmlData", ef, request);
		setField("perexImage", ef, request);
		setField("virtualPath", ef, request);
		setField("externalLink", ef, request);

		setField("fieldH", ef, request);

		setField("fieldK", ef, request);
		setField("fieldL", ef, request);
		setField("fieldJ", ef, request);
		setField("fieldM", ef, request);
		setField("fieldN", ef, request);
		setField("fieldO", ef, request);
		setField("fieldQ", ef, request);

		ef.setAuthorId(user.getUserId());
		int historyId = EditorDB.saveEditorForm(ef, request);
		if(historyId > 0){
			out.print("ok");
		}else{
			out.print("chyba pri ukladani err:"+historyId);
		}
		EditorDB.cleanSessionData(request);

	}
}


%>
