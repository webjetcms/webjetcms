<%@page import="sk.iway.iwcm.doc.AtrDB"%>
<%@page import="java.util.List"%><%@page import="java.util.ArrayList"%><%@page import="sk.iway.iwcm.doc.AtrBean"%><%@page import="sk.iway.iwcm.doc.TemplateDetails"%><%@
page import="sk.iway.iwcm.tags.support_logic.ResponseUtils"%><%@
page import="sk.iway.iwcm.doc.GroupDetails"%><%@
page import="sk.iway.iwcm.doc.GroupsDB"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%><%@
page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.users.UsersDB,sk.iway.iwcm.editor.*,sk.iway.iwcm.i18n.Prop" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><iwcm:checkLogon admin="true"/><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="cmp_basket|menuWebpages"/><%

Identity user = UsersDB.getCurrentUser(request);
Prop prop = Prop.getInstance(request);

if (user == null || user.isAdmin()==false)
{
	out.print(prop.getText("error.userNotLogged"));
}

int docId = Tools.getIntValue(request.getParameter("docid"), -2);
if (docId == 0) docId = -1;

int groupId = Tools.getIntValue(request.getParameter("groupid"), -2);
System.out.println("docId="+docId+" groupId="+groupId);

String output="";
boolean newCategory = false;
if(request.getParameter("newCategory") != null )
	newCategory = true;

if(newCategory)
{
	GroupDetails groupDetails = new GroupDetails();
	groupDetails.setParentGroupId(groupId);
	groupDetails.setGroupName(ResponseUtils.filter(request.getParameter("title")));
	groupDetails.setMenuType(GroupDetails.MENU_TYPE_ONLYDEFAULT);

	GroupsDB.getInstance().save(groupDetails);
	//po pridani sa nezobrazovali grupy v menu stranok
	GroupsDB.getInstance(true);

	groupId = groupDetails.getGroupId();
}


if (docId > 0 || (docId == -1 && groupId > 0) )
{
	EditorForm ef = EditorDB.getEditorForm(request, docId, -1, groupId);
	System.out.println("mam rf, docid="+ef.getDocId()+" domain="+ef.getDomainName()+" editable="+EditorDB.isPageEditable(user, ef));
	if (ef != null && ef.getDocId()==docId && EditorDB.isPageEditable(user, ef))
	{
		ef.setAuthorId(user.getUserId());
		ef.setTitle(DB.prepareString(request.getParameter("title"), 255));
		ef.setHtmlData(DB.prepareString(request.getParameter("perex"), 255));
		ef.setNavbar(ef.getTitle());
		ef.setPerexImage(DB.prepareString(request.getParameter("perexImage"), 255));

		ef.setFieldK(DB.prepareString(request.getParameter("fieldK"), 10)); //price
		ef.setFieldJ(DB.prepareString(request.getParameter("fieldJ"), 3)); //currency
		ef.setFieldL(DB.prepareString(request.getParameter("fieldL"), 4)); //vat
		ef.setFieldM(DB.prepareString(request.getParameter("fieldM"), 10)); //old price
		ef.setFieldN(DB.prepareString(request.getParameter("fieldN"), 255)); //ean
		ef.setFieldO(DB.prepareString(request.getParameter("fieldO"), 255)); //manufacturer

		ef.setFieldQ(DB.prepareString(request.getParameter("fieldQ"), 255)); //Label text
		ef.setFieldR(DB.prepareString(request.getParameter("fieldR"), 255)); //Label text color
		ef.setFieldS(DB.prepareString(request.getParameter("fieldS"), 255)); //Label background color

		if(newCategory)
		{
			ef.setData("<h1>"+request.getParameter("title")+"</h1><br> !INCLUDE(/components/basket/bootstrap_products.jsp, groupIds="+groupId+", orderType=priority, asc=yes, publishType=all, paging=yes, pageSize=20, thumbWidth=190, thumbHeight=200)! ");
			output = "groupId="+groupId+";";
		}

		if("cloud".equals(Constants.getInstallName())){
			TemplateDetails templateDetails = sk.iway.iwcm.common.CloudToolsForCore.getRootTemp(request);
			if(templateDetails != null)
			{
				ef.setTempId(templateDetails.getTempId());
			}
		}

		int domainId = sk.iway.iwcm.common.CloudToolsForCore.getDomainId();
		String divider = "";
		StringBuilder sb = new StringBuilder();
		List<String> parameterNames = new ArrayList<String>(request.getParameterMap().keySet());
		for(String parameterName : parameterNames)
		{
			if (parameterName.startsWith("variantName"))
			{
				int counter = Integer.parseInt(parameterName.replaceAll("\\D+",""));
				String variantName = request.getParameter(parameterName);

				if (Tools.isNotEmpty(variantName))
				{
					AtrBean atr = AtrDB.getAtrDef(variantName, domainId + "-varianty", request);

					if (atr == null)
					{
						atr = new AtrBean();
						atr.setAtrName(variantName);
						atr.setAtrGroup(domainId + "-varianty");

						AtrDB.insertAttribute(atr, request);
					}

					String variantValues = request.getParameter("variantValues[" + counter + "]");

					StringBuilder sbAdd = new StringBuilder();

					sbAdd.append(divider);
					sbAdd.append(atr.getAtrName());
					sbAdd.append(":");
					sbAdd.append(variantValues);

					if (sb.length() + sbAdd.length()<255)
					{
						sb.append(sbAdd.toString());
					}

					divider = "|";
				}
			}
		}
		ef.setFieldP(sb.toString());

		ef.setPublish("1");

		int historyId = EditorDB.saveEditorForm(ef, request);
		EditorDB.cleanSessionData(request);
		if (historyId > 0)
		{
			out.print("OK");
			if (docId < 1) out.print(", reload "+output);
			return;
		}
	}
}

out.print(prop.getText("dmail.subscribe.db_error"));
%>