<%@
page import="sk.iway.iwcm.doc.GroupDetails"%><%@
page import="sk.iway.iwcm.doc.GroupsDB"%><%@
page import="java.util.List"%><%
	sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%><%@
page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.users.UsersDB,sk.iway.iwcm.editor.*,sk.iway.iwcm.i18n.Prop" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><iwcm:checkLogon admin="true" perms="cmp_basket"/><%!

public String add(String original, String insert)
{
	int startIndex = original.indexOf("!INCLUDE(/components/basket/products.jsp");
	int endIndex = original.indexOf(")!",startIndex);

	if (startIndex !=-1 && endIndex > startIndex)
	{
		String data = new StringBuilder(original).insert(endIndex, ", "+insert).toString();

		return data;
	}
	return original;
}

%><%
String newShowCategory = "showCategory="+request.getParameter("showCategory");
String newShowSort = "showSort="+request.getParameter("showSort");
String newTestRun = "testRun="+request.getParameter("testRun");

int[] rootIds = Tools.getTokensInt(request.getParameter("rootId"), "+,");
for(int rootId : rootIds)
{
List<GroupDetails> availableGroups = GroupsDB.getInstance().getGroupsTree(rootId, false, true);
String data;
EditorForm ef;

for( GroupDetails gd : availableGroups)
	{
		ef = EditorDB.getEditorForm(request, gd.getDefaultDocId(), -1, gd.getGroupId());
		data = ef.getData();

		if ((data.contains("showCategory=yes")) || (data.contains("showCategory=no")))
		{
			data = Tools.replace(data, "showCategory=yes", newShowCategory);
			data = Tools.replace(data, "showCategory=no", newShowCategory);
		}
		else
		{
			data = add(data,newShowCategory);
		}

		if ((data.contains("showSort=yes")) || (data.contains("showSort=no")))
		{
			data = Tools.replace(data, "showSort=yes", newShowSort);
			data = Tools.replace(data, "showSort=no", newShowSort);
		}
		else
		{
			data = add(data,newShowSort);
		}

		if ((data.contains("testRun=yes")) || (data.contains("testRun=no")))
		{
			data = Tools.replace(data, "testRun=yes", newTestRun);
			data = Tools.replace(data, "testRun=no", newTestRun);
		}
		else
		{
			data = add(data,newTestRun);
		}

		if( !data.equals(ef.getData()) )
			{
				ef.setData(data);
				ef.setPublish("1");
				ef.setAuthorId(UsersDB.getCurrentUser(request).getUserId());
				int historyId = EditorDB.saveEditorForm(ef, request);
				EditorDB.cleanSessionData(request);
			}
	}
}
%>