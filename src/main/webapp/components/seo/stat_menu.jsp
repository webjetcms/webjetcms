<%@page import="java.util.List"%><%@ page import="sk.iway.iwcm.*,sk.iway.iwcm.stat.*,sk.iway.iwcm.doc.*,java.util.*,java.text.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page import="sk.iway.iwcm.users.UsersDB"%>
<iwcm:menu notName="cmp_stat">
<%
	response.sendRedirect("/admin/403.jsp");
	if (1==1) return;
%>
</iwcm:menu>

<%
	String actualPath = PathFilter.getOrigPath(request);
	String suffix = "_new";
	if (Constants.getString("statMode").startsWith("old")) suffix = "";

	//otestuj ci existuje nahrada za tuto stranku
	String forward = "/components/stat/stat_menu-"+Constants.getInstallName()+".jsp";
	java.io.File fForward = new java.io.File(sk.iway.iwcm.Tools.getRealPath(forward));
	if (fForward.exists())
	{
		pageContext.include(forward);
		//return;
	}
	else
	{
%>

<%
	}
	Identity user = UsersDB.getCurrentUser(request);
	if (user == null) return;
	List<GroupDetails> distinctTopics = new ArrayList<GroupDetails>();
	int authorId = Tools.getIntValue(Tools.getRequestParameter(request, "authorId"), -1);
	boolean NO_RECURSIVE = false;
	int userEditableGroups[] = null;
	String userEditableGroupsString = null;	//toto sa bude v pripade, ze sa jedna o bloggera, pouzivat pri vytvarani statistik ako groupIds

	if(authorId == -1)
	{
		userEditableGroups = Tools.getTokensInt(user.getEditableGroups(), ",");
	}
	else	//MRE: pridane kvoli statistike blogera
	{
		userEditableGroupsString = UsersDB.getUser(authorId).getEditableGroups();
		userEditableGroups = Tools.getTokensInt(userEditableGroupsString, ",");

		/*for (int groupId : userEditableGroups)	//ziska rubriky bloggera
			distinctTopics.addAll( GroupsDB.getInstance().findChilds(groupId, NO_RECURSIVE ) );*/
	}

	String startDateString="1.1.2002";
	String endDateString="";

	Date startDate=null;
	Date endDate=null;

	int maxRows = 100;
	String groupIdsQuery = null;
	int groupId = -1;
	if (userEditableGroups!=null && userEditableGroups.length>0 && authorId == -1) groupId = userEditableGroups[0]; //MRE: Pre bloggerov to vzdy hadzalo prvy povoleny adresar

	if (session.getAttribute(Constants.STAT_START_DATE)!=null)
	{
		startDate = (Date)session.getAttribute(Constants.STAT_START_DATE);
	}
	if (session.getAttribute(Constants.STAT_END_DATE)!=null)
	{
		endDate = (Date)session.getAttribute(Constants.STAT_END_DATE);
	}
	if (session.getAttribute(Constants.STAT_MAX_ROWS)!=null)
	{
		maxRows = ((Integer)session.getAttribute(Constants.STAT_MAX_ROWS)).intValue();
	}
	if (session.getAttribute(StatDB.SESSION_GROUP_ID)!=null)
	{
		groupId = ((Integer)session.getAttribute(StatDB.SESSION_GROUP_ID)).intValue();
	}
	if (session.getAttribute(StatDB.SESSION_GROUP_IDS_QUERY)!=null)
	{
		groupIdsQuery = (String)session.getAttribute(StatDB.SESSION_GROUP_IDS_QUERY);
	}

	Calendar cal = Calendar.getInstance();
	cal.setFirstDayOfWeek(Calendar.MONDAY);
	if (endDate==null)
	{
		cal.set(Calendar.HOUR, 23);
		cal.set(Calendar.MINUTE, 58);
		endDate = new Date(cal.getTime().getTime());
	}
	if (startDate==null)
	{
		cal.add(Calendar.MONTH, -1);
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 1);
		startDate = new Date(cal.getTime().getTime());
	}

	//ak mame v request nieco, zmen
	try
	{
		if (Tools.getRequestParameter(request, "startDate")!=null) startDate = new Date(DB.getTimestamp(Tools.getRequestParameter(request, "startDate"), "0:00", sk.iway.iwcm.DBPool.getDBName(request)));
		if (Tools.getRequestParameter(request, "endDate")!=null) endDate = new Date(DB.getTimestamp(Tools.getRequestParameter(request, "endDate"), "23:59", sk.iway.iwcm.DBPool.getDBName(request)));
		if (Tools.getRequestParameter(request, "maxRows")!=null)
		{
			maxRows = Integer.parseInt(Tools.getRequestParameter(request, "maxRows"));
		}
		if (Tools.getRequestParameter(request, "groupId")!=null)
		{
			groupId = Tools.getIntValue(Tools.getRequestParameter(request, "groupId"), -1);
			if (groupId < 1 && userEditableGroups!=null && userEditableGroups.length>0 && authorId == -1) groupId = userEditableGroups[0];	//MRE: Pre bloggerov to vzdy hadzalo prvy povoleny adresar

			//todo: ziskaj rekurzivne id skupin
			groupIdsQuery = StatDB.getRootGroupWhere("group_id", groupId);
		}
	}
	catch (Exception ex)
	{
		sk.iway.iwcm.Logger.error(ex);
	}

	//zarovnaj to na zaciatok a koniec tyzdna
	cal.setTime(startDate);
	if ("true".equals(request.getAttribute("dateMonthFix")))
	{
		//datimu musim zaokruhlit na mesiac
		cal.set(Calendar.DAY_OF_MONTH, 1);
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		startDate = new Date(cal.getTime().getTime());

		cal.add(Calendar.MONTH, 1);
		cal.add(Calendar.DATE, -1);
		cal.set(Calendar.HOUR, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		endDate = new Date(cal.getTime().getTime());
	}
	else
	{
		//toto uz nie je potrebne (statistiku mame primarne v stat_views po zaznamoch)
		/*
		cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		startDate = new Date(cal.getTime().getTime());

		cal.setTime(endDate);
		cal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
		cal.add(Calendar.DAY_OF_YEAR, 1);
		endDate = new Date(cal.getTime().getTime());
		*/
	}

	SimpleDateFormat formatter = new SimpleDateFormat(Constants.getString("dateFormat"));
	startDateString = formatter.format(startDate);
	endDateString = formatter.format(endDate);

	session.setAttribute(Constants.STAT_START_DATE, startDate);
	session.setAttribute(Constants.STAT_END_DATE, endDate);
	session.setAttribute(Constants.STAT_MAX_ROWS, new Integer(maxRows));
	session.setAttribute(StatDB.SESSION_GROUP_ID, new Integer(groupId));
	if (groupIdsQuery!=null)
	{
		session.setAttribute(StatDB.SESSION_GROUP_IDS_QUERY, groupIdsQuery);
	}
	else
	{
		session.removeAttribute(StatDB.SESSION_GROUP_IDS_QUERY);
	}

	// potrebne iba pre stat_errors.jsp
	String urlStringSearch = Tools.getRequestParameter(request, "urlStringSearch");
	if (urlStringSearch == null)
		urlStringSearch = "";
%>

<script type='text/javascript' src='/components/calendar/popcalendar.jsp'></script>
	<form class="globalFilterForm" action="<%=PathFilter.getOrigPath(request)%>" method="get">

		<div class="col-lg-2 col-md-3 col-sm-6 col-xs-12">
			<div class="form-group">
				<label for="statStartDateMenu" class="popisok2 form-label"><iwcm:text key="stat.time"/>:</label>
				<div class="input-group date date-picker">
					<input type="text" size="10" maxlength="12" id="statStartDateMenu" name="startDate" value="<%=startDateString%>" class="form-control" />
					<span class="input-group-btn"><button class="btn default" type="button"><i class="ti ti-calendar"></i></button></span>
				</div>
			</div>
		</div>
		<div class="col-lg-2 col-md-3 col-sm-6 col-xs-12">
			<div class="form-group">
				<label class="control-label display-block">&nbsp;</label>
				<div class="input-group date date-picker">
					<input type="text" size="10" maxlength="12" id="statEndDateMenu" name="endDate" value="<%=endDateString%>"  class="form-control" />
					<span class="input-group-btn"><button class="btn default" type="button"><i class="ti ti-calendar"></i></button></span>
				</div>
			</div>
		</div>



	<%
		if (actualPath.indexOf("stat_errors")!=-1) // potrebne iba pre stat_errors.jsp
		{
	%>
		<div class="col-md-2 col-sm-6">
			<div class="form-group">
				<label for="urlString" class="form-label"><iwcm:text key="searchall.url"/>:</label>
				<input type="text" size="160" maxlength="255" id="urlString" name="urlStringSearch" value="<%=urlStringSearch %>" class="form-control" />
			</div>
		</div>

	<%
		}
	boolean showGroup = true;

	if (actualPath.indexOf("errors")!=-1 ||
		 actualPath.indexOf("userlogon")!=-1 ||
		 actualPath.indexOf("banner")!=-1  ||
		 actualPath.indexOf("position")!=-1 ||
		 actualPath.indexOf("heat_map") != -1 ||
		 actualPath.indexOf("stat_doc_new") != -1)
	{
		showGroup = false;
	}
	if (showGroup)
	{
	%>
		<script type="text/javascript">
		<!--
			var lastSelect = null;
			function setParentGroupId(returnValue)
			{
				if (returnValue.length > 15)
				{
					var groupid = returnValue.substr(0,15);
					var groupname = returnValue.substr(15);
					groupid = groupid.replace(/^[ \t]+|[ \t]+$/gi, "");

					var optionName = new Option(groupname, groupid, true, true)
			      lastSelect.options[lastSelect.length] = optionName;
			  	}
			   else
			   {
			      lastSelect.selectedIndex = 0;
			   }
			}

			function addGroupId(select)
			{
				if (select.value == "")
				{
					lastSelect = select;
					WJ.popup("<iwcm:cp/>/admin/grouptree.jsp", 300, 450);
				}
			}
		//-->
		</script>

		<div class="col-md-2 col-sm-6">
			<div class="form-group">
				<label for="groupId1" class="form-label"><%if(authorId == -1){ %><iwcm:text key="stat_menu.group"/><%}else{ %><iwcm:text key="components.blog.title"/><%} %>:</label>

				<select name="groupId" class="form-control" onchange="addGroupId(this)" id="groupId1">
					<option value="-1"><iwcm:text key="email.groups.all"/></option>
						<%
							GroupsDB groupsDB = GroupsDB.getInstance();
							List rootGroups = null;
							if (userEditableGroups!=null && userEditableGroups.length>0)
							{
								rootGroups = new ArrayList();
								boolean mamSpecialny = true;
								for (int userGroupId : userEditableGroups)
								{
									GroupDetails ugd = groupsDB.getGroup(userGroupId);
									if (ugd != null) rootGroups.add(ugd);
									if (userGroupId == groupId) mamSpecialny = false;
								}
								if(mamSpecialny && groupId > 0)
								{
									GroupDetails group = groupsDB.getGroup(groupId);
									if (group != null) rootGroups.add(0, group);
								}
							}
							else
							{
								rootGroups = groupsDB.getGroups(0);
								if(groupId != -1 && groupsDB.findGroup(rootGroups, groupId) == null)
								{
									GroupDetails group = groupsDB.getGroup(groupId);
									if (group != null) rootGroups.add(0, group);
								}
							}
							Iterator iter = rootGroups.iterator();

							while (iter.hasNext())
							{
								GroupDetails group = (GroupDetails)iter.next();
								out.print("<option value=\""+group.getGroupId()+"\"");
								if (group.getGroupId()==groupId) out.print(" selected='selected'");
								out.print(">"+group.getFullPath()+"</option>");
							}
							if(authorId == -1)
							{
								%>
							<option value=""><iwcm:text key="stat_menu.another"/></option>
						<%	}
						%>

				</select>

			</div>
		</div>

		<%
		boolean showBotsFilter = false;

		if(actualPath.indexOf("stat_days_new")!=-1 || actualPath.indexOf("stat_weeks_new")!=-1 ||
		   actualPath.indexOf("stat_months_new")!=-1 || actualPath.indexOf("stat_hours_new")!=-1 ||
		   actualPath.indexOf("stat_top_new")!=-1 || actualPath.indexOf("stat_country")!=-1 ||
		   actualPath.indexOf("stat_browser")!=-1 || actualPath.indexOf("stat_complete")!=-1)
		{
			showBotsFilter = true;
		}

		if(showBotsFilter){
		%>
		<div class="col-md-2 col-sm-6">
			<div class="form-group">
				<label class="control-label display-block">&nbsp;</label>
				<input type="checkbox" name="withoutBots" value="true" <%if(Tools.isNotEmpty(Tools.getRequestParameter(request, "withoutBots"))) out.print("checked=\"checked\""); %>> <iwcm:text key="stat.withoutBots"/>
			</div>
		</div>
		<%}%>

	<%
	}
	else
	{
	%>
		<input type="hidden" name="groupId" value="<%=groupId%>" />
	<%
	}

	boolean showHeatButton = actualPath.contains("heat_map");
	String message = session.getAttribute("heatMapEnabled") == null ? "Zapni" : "Vypni";
	boolean heatMapOn = session.getAttribute("heatMapEnabled") != null;
	%>
	<input type="hidden" name="authorId" value="<%=authorId%>" />
	<c:if test="<%=showHeatButton %>">
		<input type="hidden" value="<%=heatMapOn %>" name="heatMapOn" />
		<input type="submit" class="button100" value="<%=message %>" />
		<script type="text/javascript">
			function setPage(docId){
				if (!docId) return;
				document.getElementById('document_id').value = docId[0]
			}
		</script>
	</c:if>

			<div class="col-md-2 col-sm-6">
				<div class="form-group">
					<label class="control-label display-block">&nbsp;</label>
					<input type="submit" value="<iwcm:text key="button.setup"/>" class="btn green" onclick="this.form.<%=SetCharacterEncodingFilter.PDF_PRINT_PARAM %>.value='';" />
					<input type="hidden" name="<%=SetCharacterEncodingFilter.PDF_PRINT_PARAM %>" value=""/>

				</div>
			</div>

			<% if (Tools.getRequestParameter(request, "bannerId")!=null){ %>
				<input type="hidden" name="bannerId" value="<%=Tools.getRequestParameter(request, "bannerId") %>" />
			<%} %>
			<% if (Tools.getRequestParameter(request, "doc_id")!=null){ %>
				<input type="hidden" name="doc_id" value="<%=Tools.getRequestParameter(request, "doc_id") %>" />
			<%} %>
			<% if (Tools.getRequestParameter(request, "title")!=null){ %>
				<input type="hidden" name="title" value="<%=Tools.getRequestParameter(request, "title") %>" />
			<%} %>
			<% if (Tools.getRequestParameter(request, "inIframe")!=null){ %>
				<input type="hidden" name="inIframe" value="<%=Tools.getRequestParameter(request, "inIframe") %>" />
			<%} %>
			<% if (Tools.getRequestParameter(request, "hideHeaderFooter")!=null){ %>
				<input type="hidden" name="hideHeaderFooter" value="<%=Tools.getRequestParameter(request, "hideHeaderFooter") %>" />
			<%} %>

</form>
