<%@page import="java.util.List"%><%@ page contentType="text/xml; charset=utf-8" pageEncoding="utf-8"
		 import="sk.iway.iwcm.*,sk.iway.iwcm.doc.DocDB,sk.iway.iwcm.doc.DocDetails"%><%@ page
		import="sk.iway.iwcm.doc.GroupDetails" %><%@ page
		import="sk.iway.iwcm.doc.GroupsDB" %><%@page
		import="java.text.DecimalFormat"%><%@ page
		import="java.text.SimpleDateFormat" %><%@ page
		import="java.util.*" %><%@
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@
taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%@
taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %><%@
taglib uri="/WEB-INF/struts-html.tld" prefix="html" %><%@
taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %><%!

private void addPagesFromGroup(int rootGroupId, int level, List<DocDetails> pages, GroupsDB groupsDB, DocDB docDB, HttpServletRequest request)
{
   List pagesInGroup = docDB.getBasicDocDetailsByGroup(rootGroupId, DocDB.ORDER_PRIORITY);

   //System.out.println("addPagesFromGroup: rootGroupId="+rootGroupId+" pagesInGroup="+pagesInGroup.size());

	String abTestingName = Constants.getString("ABTestingName", "");
   DocDetails doc;
   Iterator iter = pagesInGroup.iterator();
	while (iter.hasNext())
	{
		doc = (DocDetails)iter.next();
		if (doc.isAvailable() && doc.isSearchable() && doc.isShowInSitemap(request) && Tools.isEmpty(doc.getExternalLink()) && !doc.getVirtualPath().contains(abTestingName))
		{
			pages.add(doc);
		}
	}

   List subGroups = groupsDB.getGroups(rootGroupId);
   GroupDetails group;
   iter = subGroups.iterator();
   while (iter.hasNext())
   {
      group = (GroupDetails)iter.next();
      if (( "/sk".equals(group.getUrlDirName()) || "/en".equals(group.getUrlDirName()) ) || ( group.isInternal()==false && group.getShowInSitemap(request)!=GroupDetails.MENU_TYPE_HIDDEN) )
      {
      	addPagesFromGroup(group.getGroupId(), level+1, pages, groupsDB, docDB, request);
      }
   }
}

public static SimpleDateFormat sdf;
public static DecimalFormat df;
static
{
   sdf = new SimpleDateFormat("yyyy-MM-dd");
   df = new DecimalFormat("0.0");
}


%><%
//Generuje google sitemap
PageParams pageParams = new PageParams(request);
String rootGroupId = pageParams.getValue("groupId", null);
if (InitServlet.isTypeCloud())
{
	String domainName = Tools.getServerName(request);
	int domainId = GroupsDB.getDomainId(domainName);
	System.out.println("domainName="+domainName+" id="+domainId);
	if (domainId > 0)
	{
		rootGroupId = String.valueOf(domainId);
	}
}

if (rootGroupId==null && Tools.getRequestParameter(request, "groupId")!=null) rootGroupId = Tools.getRequestParameter(request, "groupId");
if (rootGroupId==null && Constants.getBoolean("multiDomainEnabled"))
{
	//skus vydedukovat cez multidomain
	String domain = DocDB.getDomain(request);
	GroupsDB groupsDB = GroupsDB.getInstance();
	List groups = groupsDB.getGroupsAll();
	Iterator iter = groups.iterator();
	GroupDetails group;
	while (iter.hasNext())
	{
		group = (GroupDetails) iter.next();
		if (group.isInternal()==false && group.getParentGroupId() < 1 && domain.equalsIgnoreCase(group.getDomainName()))
		{
			if (rootGroupId==null) rootGroupId = ""+group.getGroupId();
			else rootGroupId += "+"+group.getGroupId();
		}
	}
}
if (rootGroupId==null) rootGroupId = String.valueOf(Constants.getInt("rootGroupId"));

//ziskaj groupsDB
GroupsDB groupsDB = GroupsDB.getInstance();
//ziskaj DocDB
DocDB docDB = DocDB.getInstance();

List pages = new ArrayList();
StringTokenizer st = new StringTokenizer(rootGroupId, ",+ ");
while (st.hasMoreTokens())
{
	int groupId = Tools.getIntValue(st.nextToken(), -1);
	if (groupId > 0) addPagesFromGroup(groupId, 1, pages, groupsDB, docDB, request);
}
request.setAttribute("pages", pages);

Adminlog.add(Adminlog.TYPE_SE_SITEMAP, "Generovana sitemap: rootGroupId="+rootGroupId+" UA="+request.getHeader("User-Agent")+" IP="+Tools.getRemoteIP(request)+" host="+Tools.getRemoteHost(request), -1, -1);
%><?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
<iwcm:iterate id="doc" name="pages" type="sk.iway.iwcm.doc.DocDetails"><%
	String url = docDB.getDocLink(doc.getDocId(), doc.getExternalLink(), request);
	if (url.startsWith("http")==false) url = Tools.getBaseHref(request) + url;

	double priority = 0.5D;
	int len = 0;
	try
	{
		//urci prioritu podla hlbky zanorenia (poctu / v URL)
		String tokens[] = Tools.getTokens(url.substring(url.indexOf("/", 8)), "/");
		len = tokens.length;
		if (len < 1) len = 1;
		if (len > 8) len = 8;
		priority = 1.1D - ((double)len) * 0.1D;

		if (priority > 1.0) priority = 1.0;
		if (priority < 0.2) priority = 0.2;
	}
	catch (Exception ex) {}
%>
   <url>
      <loc><%=url%></loc>
      <lastmod><%=sdf.format(new Date(doc.getDateCreated()))%></lastmod>
      <priority><%=df.format(priority).replace(',', '.')%></priority>
   </url>
</iwcm:iterate>
</urlset>
