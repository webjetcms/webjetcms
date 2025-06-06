<%@page import="java.util.Map"%><%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="java.util.*, sk.iway.iwcm.*, sk.iway.iwcm.doc.*" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%

Iterator iter;
DocDetails docDetails;
PerexGroupBean pgb;
GroupsDB groupsDB = GroupsDB.getInstance();
List relatedPages;
int perexGroupId;
int docId = -1;
boolean empty;
String[] rootGroupsArray;
String[] perGroups;
String perexGroupName;
StringBuffer strBuf = new StringBuffer();

PageParams pageParams = new PageParams(request);
String groups = pageParams.getValue("groups", null);
String rootGroups = pageParams.getValue("rootGroups", null);
String titleName = pageParams.getValue("titleName", null);
int pagesInGroup = pageParams.getIntValue("pagesInGroup", 10);
boolean rGroupsRecursive = pageParams.getBooleanValue("rGroupsRecursive", false);

String dirName;
String dirNameparent = "";

//vygenerujeme klucove slova podla aktualnej stranky
if (Tools.isEmpty(groups) || "auto".equals(groups) || "0".equals(groups))
{
	DocDB docDB = DocDB.getInstance();
	DocDetails doc = (DocDetails)request.getAttribute("docDetails");

	// suvisiace stranky *start*
	String perexGroupIds[] = doc.getPerexGroup();
	int size = perexGroupIds.length;
	String newPerexGroups = null;
	String perexGroupName2;

	for (int i=0; i<size; i++)
	{
		perexGroupName2 = docDB.convertPerexGroupIdToName(perexGroupIds[i]);
		if (perexGroupName2==null) continue;

		if (newPerexGroups==null) newPerexGroups = ""+perexGroupIds[i];
  		else newPerexGroups += "+" + perexGroupIds[i];
	}
	groups=newPerexGroups;
}

//ziskame nazov skupiny, ktory sa vypise ako nadpis. moze to byt nazov skupiny alebo rodic. skupiny
//a ak mame nastavene rekurz. prehladavanie, doplnim id podskupin do rootGroups
if (Tools.isNotEmpty(rootGroups))
{
	GroupDetails groupDet;
	String newRootGroups = "";
	StringTokenizer st = new StringTokenizer(rootGroups, "+");
	String groupStr = st.nextToken().trim();

	int rgID = Integer.parseInt(groupStr);
	if (rgID > 0)
	{
		groupDet = groupsDB.getGroup(rgID);
		if ("rootGroupName".equals(titleName) && groupDet != null)
		{
		   dirName = groupDet.getGroupName();
		   int parentGroupId = groupDet.getParentGroupId();
		   GroupDetails groupDetParent = groupsDB.getGroup(parentGroupId);
		   if (groupDetParent != null)
		   	  titleName = groupDetParent.getGroupName();
		}
		if ("groupName".equals(titleName) && groupDet != null)
			titleName = groupDet.getGroupName();
	}

	//rekurzivne hladanie podskupin
	if(rGroupsRecursive)
	{
		List grpList = null;
		//rootGroups = "25";
		st = new StringTokenizer(rootGroups, "+");
		while(st.hasMoreTokens())
		{
			grpList = null;
			groupDet = null;
			rgID = Tools.getIntValue(st.nextToken(), -1);
			if(rgID > 0)
			{
				grpList = groupsDB.getGroupsTree(rgID, true, false);
			}
			if (grpList != null)
			{
				iter = grpList.iterator();
				while(iter.hasNext())
				{
					groupDet = (GroupDetails) iter.next();
					newRootGroups += groupDet.getGroupId()+ "+";
				}
			}
		}

		if (Tools.isNotEmpty(newRootGroups))
		{
			if (newRootGroups.endsWith("+"))
				newRootGroups = newRootGroups.substring(0, newRootGroups.length()-1);
			//out.println("OLD: "+rootGroups+ "<br />NEW: " +newRootGroups+ "<br />");
			if (newRootGroups.length() > rootGroups.length())
				rootGroups = newRootGroups;
		}
	}


}

/*
int rgID = -1;//Integer.parseInt(rootGroups);
GroupDetails groupDet = groupsDB.getGroup(rgID);
if (useParentName && groupDet != null)
{
   dirName = groupDet.getGroupName();
   int parentGroupId = groupDet.getParentGroupId();
   GroupDetails groupDetParent = groupsDB.getGroup(parentGroupId);
   if (groupDetParent != null)
   	  dirNameparent = groupDetParent.getGroupName();
}
if (Tools.isEmpty(dirNameparent) && groupDet != null)
	dirNameparent = groupDet.getGroupName();
*/
Map foundPages = new Hashtable();
foundPages.put(request.getParameter("docid"), "true");
GroupDetails groupDetails;
try
{
	if (groups != null && rootGroups != null)
	{
		groups = Tools.replace(groups, "+", ",");
		rootGroups = Tools.replace(rootGroups, "+", ",");
		if (request.getParameter("docid") != null)
		{
			docId = Integer.parseInt(request.getParameter("docid"));
		}
		relatedPages = RelatedPagesDB.getRelatedPagesByGroups(groups, rootGroups, docId);

		if (Tools.isNotEmpty(titleName))
		{
			strBuf.append("<b>"+titleName+"</b><br />");
		}

		empty = true;
		//out.println("relatedPages size: "+relatedPages.size()+"  groupName: "+pgb.getPerexGroupName()+"<br />");

		int count = 0;
		iter = relatedPages.iterator();
		long now = Tools.getNow();
		while(iter.hasNext() && count<pagesInGroup )
		{
			docDetails = (DocDetails) iter.next();

			if (docDetails.getPublishStart() > now || docDetails.isAvailable()==false) continue;

			//out.println(docDetails.getDocId()+"  ");
			if ( !foundPages.containsKey(""+docDetails.getDocId()) )
			{
				strBuf.append("<a href='"+docDetails.getDocLink()+"'>"+docDetails.getTitle().toUpperCase()+"</a><br />");
				empty = false;
				foundPages.put(""+docDetails.getDocId(), "true");
				count++;
			}
		}
		//strBuf.append("<br />");
		if (!empty)
		{
			out.println(strBuf.toString());
		}
		strBuf.delete(0, strBuf.length());
	}
}
catch(Exception ex)
{
	sk.iway.iwcm.Logger.error(ex);
}

%>
