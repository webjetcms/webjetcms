<%@page import="org.json.JSONArray"
%><%@page import="org.json.JSONObject"
%><%@page import="sk.iway.iwcm.Constants"
%><%@page import="sk.iway.iwcm.Identity"
%><%@page import="sk.iway.iwcm.LabelValueDetails"
%><%@page import="sk.iway.iwcm.doc.DocDB"
%><%@page import="sk.iway.iwcm.doc.DocDetails"
%><%@page import="sk.iway.iwcm.doc.TemplateDetails"
%><%@page import="sk.iway.iwcm.doc.TemplatesDB"
%><%@page import="sk.iway.iwcm.doc.groups.GroupsController"
%><%sk.iway.iwcm.Encoding.setResponseEnc(request, response, "application/json");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.i18n.Prop"
%>
<%@ page import="sk.iway.iwcm.users.UsersDB" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.List" %>
<%!

JSONObject getJsonObject(TemplateDetails td, DocDB docDB, Prop prop, Map<Integer, Integer> templateIdToUsedCount)
{
	JSONObject j = new JSONObject();
	try
	{
		DocDetails link = null;
		JSONArray jArray = new JSONArray();
		JSONArray jArrayOdkaz = new JSONArray();
		List<LabelValueDetails> editableGroups = null;

		if(editableGroups!=null)
		{
			if(editableGroups.size()>0)
			{
				for(LabelValueDetails lvd : editableGroups)
				{
					int groupid = Integer.parseInt(lvd.getValue());
					jArrayOdkaz.put(GroupsController.BASE_URL+"/"+groupid);
					jArray.put(lvd.getLabel());
				}
			}
			else
			{
				jArrayOdkaz.put(prop.getText("admin.temp.edit.showForDir.all"));
				jArray.put(prop.getText("admin.temp.edit.showForDir.all"));
			}
			j.put("showForOdkaz", jArrayOdkaz);
			j.put("showFor", jArray);
		}

		j.put("tempId",td.getTempId());
		j.put("tempName", td.getTempName());
		j.put("lng", td.getLng());

		Integer pocetPouziti = null; // = templateIdToUsedCount.get(Integer.valueOf(td.getTempId()));
		if (templateIdToUsedCount != null)
		{
			pocetPouziti = templateIdToUsedCount.get(Integer.valueOf(td.getTempId()));
		}
		if (pocetPouziti == null) pocetPouziti = Integer.valueOf(0);
		j.put("pocetPouziti", pocetPouziti);
		j.put("spamProtectionDisabled", td.isDisableSpamProtection());
		j.put("templatesGroupName", td.getTemplatesGroupName());
		j.put("templatesForwardExists", td.retrieveForwardExist());

		j.put("HTMLSablona", td.getForward());
		j.put("installName", td.getTemplateInstallName());
		j.put("installName", td.getTemplateInstallName());
		j.put("HTMLCode", td.getAfterBodyData());

		link = docDB.getBasicDocDetails(td.getHeaderDocId(), false);
		if(link!= null)
		{
			j.put("header", link.getTitle());
			j.put("headerId", link.getDocId());
		}
		else
		{
			j.put("header", "");
			j.put("headerId", "");
		}

		link = docDB.getBasicDocDetails(td.getFooterDocId(), false);
		if(link!= null)
		{
			j.put("footer", link.getTitle());
			j.put("footerId", link.getDocId());
		}
		else
		{
			j.put("footer", "");
			j.put("footerId", "");
		}

		link = docDB.getBasicDocDetails(td.getMenuDocId(), false);
		if(link!= null)
		{
			j.put("menu", link.getTitle());
			j.put("menuId", link.getDocId());
		}
		else
		{
			j.put("menu", "");
			j.put("menuId", "");
		}

		link = docDB.getBasicDocDetails(td.getRightMenuDocId(), false);
		if(link!= null)
		{
			j.put("rightMenu", link.getTitle());
			j.put("rightMenuId", link.getDocId());
		}
		else
		{
			j.put("rightMenu", "");
			j.put("rightMenuId", "");
		}

		link = docDB.getBasicDocDetails(td.getObjectADocId(), false);
		if(link!= null)
		{
			j.put("objectA", link.getTitle());
			j.put("objectAId", link.getDocId());
		}
		else
		{
			j.put("objectA", "");
			j.put("objectAId", "");
		}

		link = docDB.getBasicDocDetails(td.getObjectBDocId(), false);
		if(link!= null)
		{
			j.put("objectB", link.getTitle());
			j.put("objectBId", link.getDocId());
		}
		else
		{
			j.put("objectB", "");
			j.put("objectBId", "");
		}

		link = docDB.getBasicDocDetails(td.getObjectCDocId(), false);
		if(link!= null)
		{
			j.put("objectC", link.getTitle());
			j.put("objectCId", link.getDocId());
		}
		else
		{
			j.put("objectC", "");
			j.put("objectCId", "");
		}

		link = docDB.getBasicDocDetails(td.getObjectDDocId(), false);
		if(link!= null)
		{
			j.put("objectD", link.getTitle());
			j.put("objectDId", link.getDocId());
		}
		else
		{
			j.put("objectD", "");
			j.put("objectDId", "");
		}

		j.put("baseCSSPath", td.getBaseCssPath());
		j.put("CSSStyle", td.getCss());
	}
	catch(Exception e)
	{
		sk.iway.iwcm.Logger.error(e);
	}
	return j;
}

%><%
Identity user = UsersDB.getCurrentUser(request);
if (user == null) return;

TemplatesDB tempDB = TemplatesDB.getInstance();
List<TemplateDetails> templatesAvailableToUser = TemplatesDB.filterTemplatesByUser(user, tempDB.getTemplatesSaved());

JSONObject json = new JSONObject();
JSONArray jsonArray = new JSONArray();
DocDB docDB = DocDB.getInstance();
Prop prop = Prop.getInstance(request);

Map<Integer, Integer> templateIdToUsedCount = null;
if (Constants.getBoolean("templatesShowUsage"))
{
	templateIdToUsedCount = TemplatesDB.getInstance().numberOfPages();
}

if(templatesAvailableToUser!=null)
{
	for (TemplateDetails td : templatesAvailableToUser)
	{
		jsonArray.put(getJsonObject(td, docDB, prop, templateIdToUsedCount));
	}
}

json.put("data", jsonArray);
out.println(json.toString(3));
%>