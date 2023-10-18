<%@page import="org.apache.struts.util.ResponseUtils"%>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.Constants,sk.iway.iwcm.Identity,sk.iway.iwcm.PathFilter,sk.iway.iwcm.Tools" %>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/displaytag.tld" prefix="display" %>

<%@page import="sk.iway.iwcm.doc.DocDB"%>
<%@page import="sk.iway.iwcm.doc.DocDetails"%>
<%@page import="sk.iway.iwcm.doc.GroupDetails"%>
<%@ page import="sk.iway.iwcm.doc.GroupsDB" %>
<%@ page import="sk.iway.iwcm.editor.EditorDB" %>
<%@ page import="sk.iway.iwcm.editor.EditorForm" %>
<%@ page import="sk.iway.iwcm.i18n.Prop" %>
<%@ page import="sk.iway.iwcm.search.DocDetailsSearch" %>
<%@ page import="sk.iway.iwcm.search.SearchResult" %>
<%@ page import="sk.iway.iwcm.search.Searchable" %>
<%@ page import="sk.iway.iwcm.users.UsersDB" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List" %>
<iwcm:menu notName="menuWebpages">
	<%
		response.sendRedirect("/admin/403.jsp");
		if (1==1) return;
	%>
</iwcm:menu>
<%

	Identity user = UsersDB.getCurrentUser(request);
	if (user == null)
		return;

	Prop prop = Prop.getInstance(request);
	String text = Tools.getRequestParameter(request, "text");
	DocDB docDB = DocDB.getInstance();
	GroupsDB groupsDB = GroupsDB.getInstance();

	if ("tatrabanka".equals(Constants.getInstallName())==false)
	{
	   //vyhladanie podla docid, TB toto nechce, lebo to je "non sens"
		if (Tools.isNotEmpty(text) && Tools.getRequestParameter(request, "groupId") == null && text.startsWith("0") == false)
		{
			int docId = Tools.getIntValue(text, -1);
			if (docId > 0)
			{
				DocDetails doc = DocDB.getInstance().getBasicDocDetails(docId, false);
				if (doc != null && EditorDB.isPageEditable(user, new EditorForm(doc)))
				{
					response.sendRedirect("/admin/webpages/?docid=" + docId);
					return;
				}
			}
		}
	}

	String url = Tools.getRequestParameter(request, "url");
	if(Tools.isNotEmpty(url))
		text = url;

	List<SearchResult> searchResults = new ArrayList<SearchResult>();

	String searchClassesString = Constants.getString("searchClasses");
	if(Tools.isEmpty(searchClassesString))
		searchClassesString = "sk.iway.iwcm.search.DocDetailsSearch";

	List<String> searchClasses = Arrays.asList(searchClassesString.split("\\s*,\\s*"));

	List<String> disabledClasses = new ArrayList<String>();

	String filterClassName = Tools.getRequestParameter(request, "filterClassName");

	String delRetUrl = "/admin/searchall.jsp";
	if (Tools.isNotEmpty(text)) delRetUrl = Tools.addParameterToUrl(delRetUrl, "text", text);
	if (Tools.isNotEmpty(filterClassName)) delRetUrl = Tools.addParameterToUrl(delRetUrl, "filterClassName", filterClassName);
	String groupId = Tools.getRequestParameter(request, "groupId");
	if (Tools.isNotEmpty(groupId)) delRetUrl = Tools.addParameterToUrl(delRetUrl, "groupId", groupId);

	//pridaj vsetky datatable parametre
	java.util.Enumeration<String> params = request.getParameterNames();
	while (params.hasMoreElements()) {
		String name = params.nextElement();
		if (name.startsWith("d-")) {
				delRetUrl = Tools.addParameterToUrl(delRetUrl, name, Tools.getRequestParameter(request, name));
		}
	}

	if(searchClasses!=null)
	{
		for(String className : searchClasses)
		{
			if(Tools.isNotEmpty(filterClassName) && !className.equals(filterClassName))
				continue;

			try
			{

				    /*
				    Sakra rado, vies preco si vytvaral interface Searchable? Netreba vsetko robit po Webjetovsky :)

					Class<?> c = Class.forName(className);
					Method method = c.getMethod("search", String.class, HttpServletRequest.class);
					searchResults.addAll((List<SearchResult>) method.invoke(c.newInstance(), text, request));
					*/
				    Class<? extends Searchable> c = (Class<? extends Searchable>)Class.forName(className);
				    Searchable searcher = c.newInstance();
				    if (!searcher.canUse(request))
					{
					    disabledClasses.add(className);
					    continue;
					}
					if (Tools.isNotEmpty(text))
					{
						searchResults.addAll(searcher.search(text, request));
					}
				}

			catch(Exception e){sk.iway.iwcm.Logger.error(e);}
		}
	}

	request.setAttribute("searchResults", searchResults);
%>

<%@ include file="/admin/layout_top.jsp" %>

<div id="waitDiv" style="text-align:center; color: red;">
	<iwcm:text key="fbrowser.edit.loading_please_wait"/><br/>
	<img src="/admin/images/loading-anim.gif" alt="" />
</div>

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

<div class="row title">
    <h1 class="page-title"><i class="glyphicon glyphicon-search"></i><iwcm:text key="searchall.title"/><%=": " + ResponseUtils.filter(text) %></h1>
</div>

<div class="tabbable tabbable-custom tabbable-full-width">
	<ul class="nav nav-tabs">
		<li class="active">
			<a href="#tabMenu1" data-toggle="tab">
				<iwcm:text key="V obsahu"/>
			</a>
		</li>
		<li>
			<a href="#tabMenu2" data-toggle="tab">
				<iwcm:text key="V URL"/>
			</a>
		</li>
	</ul>

	<div class="tab-content">

	    <div id="tabMenu1" class="tab-pane active">

			<form autocomplete="off" name="searchForm" action="searchall.jsp" method="post">

			<div class="col-lg-3 col-sm-6">
				<div class="form-group">
					<label for="text" class="form-label"><iwcm:text key="searchall.text"/>:</label>
					<iwcm:autocomplete url="/admin/skins/webjet6/_doc_autocomplete.jsp" id="text1" name="text" value="<%=text%>" class="form-control" onOptionSelect="showDoc"/>
				</div>
			</div>



				<div class="col-lg-3 col-sm-6">
					<div class="form-group ">
						<label for="filterClassName" class="form-label"><iwcm:text key="editor.link.type"/>:</label>
						<select name="filterClassName" class="form-control" id="filterClassName">
							<option value=""><iwcm:text key="email.groups.all"/></option>

							<%
							for(String className : searchClasses)
							{

								if (disabledClasses.contains(className)) continue;
							%>
								<option value="<%=className%>" <%if(className.equals(filterClassName)){out.print("selected='selected'");}%>><iwcm:text key="<%=className%>"/></option>
							<%}
							%>

						</select>

					</div>
				</div>



			<div class="col-lg-3 col-sm-6">
				<div class="form-group groupIdeSelectWrapper" style="display: none;">
					<label for="groupId1" class="form-label"><iwcm:text key="stat_menu.group"/>:</label>
					<select name="groupId" onchange="addGroupId(this);" id="groupId1" class="form-control">
						<option value="-1"><iwcm:text key="email.groups.all"/></option>
							<%
								List<GroupDetails> rootGroups = DocDetailsSearch.getGroups(request);
								if (rootGroups != null)
								{
									Iterator iter = rootGroups.iterator();
									while (iter.hasNext())
									{
										GroupDetails group = (GroupDetails)iter.next();
										if (group != null)
										{
											out.print("<option value=\""+group.getGroupId()+"\"");
											if (group.getGroupId()==Tools.getIntValue(Tools.getRequestParameter(request, "groupId"), -1)) out.print(" selected='selected'");
											out.print(">"+group.getFullPath()+"</option>");
										}
									}
								}
							%>
						<option value=""><iwcm:text key="stat_menu.another"/></option>
					</select>

				</div>
			</div>
			<div class="col-lg-3 col-sm-6">
				<div class="form-group">
					<label class="form-label col-xs-12 hidden-xs hidden-sm" for="">&nbsp;</label>
					<input type="submit" class="btn green" value="<iwcm:text key='searchall.search' />" />
				</div>
			</div>

			</form>

		</div>

	    <div id="tabMenu2" class="tab-pane">
			<form name="searchForm2" action="searchall.jsp" method="post">

				<div class="col-lg-3 col-sm-6">
					<div class="form-group">
						<label class="form-label" for="url">
							<iwcm:text key="searchall.url" />:
						</label>
						<iwcm:autocomplete url="/admin/skins/webjet6/_doc_autocomplete.jsp" id="url" name="url" value='<%=Tools.getRequestParameter(request, "url")%>' class="form-control" onOptionSelect="showDoc" />
					</div>
				</div>
				<div class="col-lg-3 col-sm-6">
					<div class="form-group">
						<label class="form-label col-xs-12 hidden-xs" for="">&nbsp;</label>
						<input type="submit" name="text" class="btn green" value="<iwcm:text key='searchall.search' />" />
					</div>
				</div>

			</form>
		</div>

	</div>
</div>

<logic:present name="searchResults">
	<script type="text/javascript" src="scripts/divpopup.js"></script>
	<script type="text/javascript">
	<!--
		//toto treba zadefinovat v stranke po includnuti divpopup.js
		//je to offset o ktory sa posuva okno vlavo
		leftOffset=-445;
		//a toto ofset o ktory sa posuva nadol
		topOffset=10;

		//popup sa potom vola:
		//popupDIV(url);

		function select(value)
		{
			//none
		}

		function deleteOK(text,obj,docId)
		{
			if(confirm(text))
			{
				//obj.href=url;
				document.docDelForm.docid.value = docId;
				document.docDelForm.submit();
			}
		}

		function openWebJETEditor(docId)
		{
		   var url = "/admin/editor.do?docid="+docId+"&isPopup=true";
		   var options = "toolbar=no,scrollbars=yes,resizable=yes,width=900,height=600;"
		   popupWindow = window.open(url,"_blank",options);
		}
		function showDoc(event, ui){
			window.location.href = "/admin/editor.do?docid="+ui.item.doc_id
		}
	//-->
	</script>

	<link rel="stylesheet" type="text/css" href="/admin/css/tablesort.css" />
	<link rel="stylesheet" type="text/css" href="css/tablesort.css" />

	<form name="docDelForm" action="/admin/docdel.do" method="post" style="display: none">
		<input type="hidden" name="docid" value=""/>
		<input type="hidden" name="returl" value="<%=delRetUrl%>"/>
		<%=org.apache.struts.taglib.html.FormTag.renderToken(session)%>
	</form>

	<display:table name="searchResults" uid="result" class="sort_table" cellspacing="0" cellpadding="1" pagesize="20" requestURI="<%=PathFilter.getOrigPath(request)%>">
	<%SearchResult sr = (SearchResult)result;%>

		<display:column titleKey="groupslist.approve.pageTitle" sortable="true">
		<%if("sk.iway.iwcm.search.DocDetailsSearch".equals(sr.getType())){
			out.print(sr.getLabel());
		}
		else{%>
			<a href="<%=sr.getLink()%>" target="_blank"><%=sr.getLabel()%></a><br/><%=sr.getText()%>
		<%}
		%>
		</display:column>

		<display:column style="color: #777777;" titleKey="editor.directory" sortable="true" property="location"/>

		<display:column titleKey="groupslist.approve.date" property="date"/>

		<display:column titleKey="editor.link.type" sortProperty="type">
			<iwcm:text key="<%=sr.getType()%>"/>
		</display:column>

		<display:column titleKey="groupslist.approve.tools" style="width:220px;">
			<%
			if("sk.iway.iwcm.search.DocDetailsSearch".equals(sr.getType()))
			{%>
				<a href="/admin/webpages/?docid=<%=sr.getDocId() %>" title='<iwcm:text key="groupslist.edit_web_page"/>'><span class="glyphicon glyphicon-pencil" aria-hidden="true"></span></a>

				<iwcm:menu name="cmp_stat">
					<a href="/apps/stat/admin/top-details/?docId=<%=sr.getDocId() %>" title='<iwcm:text key="groupslist.stat_of_web_page"/>'><span class="glyphicon glyphicon-stats" aria-hidden="true"></span></a>
				</iwcm:menu>

				<a href="<%=docDB.getDocLink(sr.getDocId(), request)%>" target="_blank" title='<iwcm:text key="groupslist.show_web_page"/>'><span class="glyphicon glyphicon-eye-open" aria-hidden="true"></span> </a>

				<a href="javascript:popupDIV('dochistory.jsp?docid=<%=sr.getDocId() %>');" title='<iwcm:text key="groupslist.show_history"/>'><span class="glyphicon glyphicon-time" aria-hidden="true"></span></a>
				<span class="noperms-deletePage">
					<a href="#" title='<iwcm:text key="groupslist.delete_web_page"/>' onclick="deleteOK('<iwcm:text key="groupslist.do_you_really_want_to_delete"/> (<%=sr.getDocId()%>)',this,'<%=sr.getDocId()%>')"><span class="glyphicon glyphicon-remove" aria-hidden="true"></span></a>
				</span>
			<%}%>
		</display:column>

	</display:table>

	<div id="divPopUp" style="position:absolute; width:450px; height:100px; z-index:130; left: 71px; top: 146px; visibility: hidden">
		<table width="450" bgcolor="white" cellspacing="0" cellpadding="0">
			<tr><td align="left" bgcolor="#CCCCFF"><small><iwcm:text key="groupslist.web_page_history"/></small></td><td align="right" bgcolor="#CCCCFF"><a href="javascript:popupHide();"><small><b>[X]</b></small></a></td></tr>
			<tr>
				<td valign="top" colspan="2">
					<iframe src="divpopup-blank.jsp" name="popupIframe" style="border:solid #000000 1px" width="448" height="130" align="left" marginwidth="0" marginheight="0" frameborder="0" scrolling="auto"></iframe>
				</td>
			</tr>
		</table>
	</div>

	<p><small><iwcm:text key="admin.searchAll.limit_note" param1='<%=""+Constants.getInt("searchTextAllLimit")%>'/></small></p>

</logic:present>
<%@ include file="/admin/layout_bottom.jsp" %>

	<script type="text/javascript">
		//var defaultText4text = "<iwcm:text key="searchall.title"/>";
		//manageInputValue("text","init");
	<% if (Tools.isNotEmpty(Tools.getRequestParameter(request, "url"))) { %>
		showHideTab('2');
	<% } %>

        function showHideFolderSelect(){
            if($("select[name=filterClassName]").val()=="sk.iway.iwcm.search.DocDetailsSearch" || $("select[name=filterClassName]").val()==""){
                $(".groupIdeSelectWrapper").fadeIn();
            }else{
                $(".groupIdeSelectWrapper").fadeOut();
            }
        }
		$("select[name=filterClassName]").change(showHideFolderSelect);

        showHideFolderSelect();
	</script>
