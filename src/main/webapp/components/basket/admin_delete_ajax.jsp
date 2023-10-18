<%@page import="java.util.List"%><%-- Maze typy dopravy (nakupneho kosika) z editor_component --%><%@page import="sk.iway.iwcm.doc.DocDetails"%><%@
page import="sk.iway.iwcm.doc.GroupsDB"%><%@
page import="sk.iway.iwcm.doc.GroupDetails"%><%@
page import="java.util.ArrayList"%><%@
page import="sk.iway.iwcm.doc.DocDB"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%><%@
page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.users.UsersDB,sk.iway.iwcm.editor.*,sk.iway.iwcm.i18n.Prop" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms="cmp_basket"/><%

Identity user = UsersDB.getCurrentUser(request);
Prop prop = Prop.getInstance(request);

int docId = Tools.getIntValue(request.getParameter("docId"), -2);

if (user == null || user.isAdmin() == false)
{
	out.print(prop.getText("error.userNotLogged"));
}
else if(docId > 0)
{
	GroupsDB groupsDB = GroupsDB.getInstance();
	String whatWeSearch = Constants.getString("basketTransportGroupName");
	if(Tools.isEmpty(whatWeSearch)) whatWeSearch = "ModeOfTransport"; //vychodzi nazov
	GroupDetails groupDetails = groupsDB.getLocalSystemGroup();
	if (groupDetails==null) //asi to neni cloud alebo multidomain WJ, alebo bezi na localhost (iwcm.interway.sk)
		groupDetails = groupsDB.getGroupByPath("/System");
	if(groupDetails != null)
	{
		String mainToken = groupsDB.getSubgroupsIds(groupDetails.getGroupId());
		String mainToken2 = null;
		String[] tokens = Tools.getTokens(mainToken, ",");
		String[] tokens2 = null;

		if(tokens != null)
		{
			GroupDetails grDt;
			//prechadzame subGroupy
			for(String token:tokens )
			{
				grDt = groupsDB.getGroup(Tools.getIntValue(token,-1));
				if(grDt != null  && grDt.getGroupName().equals(whatWeSearch))
				{
					List<DocDetails> listDocDetails = DocDB.getInstance().getDocByGroup(grDt.getGroupId());
					//prejdeme dokumenty v grupe "whatWeSearch"
					for(DocDetails docDetail:listDocDetails)
					{
						if(docDetail.getDocId() == docId )
						{
							if (DocDB.deleteDoc(docId, request) == true)
							{
								out.print("true reload");
							}
							return;
						}
					}
				}
			}
		}
	}
}
%>