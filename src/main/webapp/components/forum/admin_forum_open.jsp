<%@page import="java.util.List"%><% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.forum.*,java.util.*,sk.iway.iwcm.*,org.apache.struts.util.ResponseUtils" %>

<%@page import="sk.iway.iwcm.doc.*"%>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<iwcm:checkLogon admin="true" perms="cmp_diskusia"/>

<%!
	//vrati true ak sa jedna o message board
	public static boolean dataHasForumText(String data)
	{
		if (data.indexOf("/forum/forum_mb")!=-1 || data.indexOf("forum_mb.jsp")!=-1)
		{
			if (data.indexOf("type=topics") != -1 || data.indexOf("rootGroup=") != -1 || data.indexOf("bbRootGroupId=") != -1)
			{
				return(true);
			}
		}
		else if (data.indexOf("/forum/forum")!=-1)
		{
			if (data.indexOf("type=topics") != -1 || data.indexOf("bbRootGroupId=") != -1)
			{
				return(true);
			}
		}
		return(false);
	}
%>

<%
	request.setAttribute("cmpName", "forum");
	UserGroupsDB userGroupsDB = UserGroupsDB.getInstance();
	List userGroups = userGroupsDB.getUserGroupsByTypeId(UserGroupDetails.TYPE_PERMS);
	if (userGroups != null)
		request.setAttribute("groups", userGroups);

	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);

	int docId = Tools.getIntValue(request.getParameter("docid"), -1);
	int parentId = Tools.getIntValue(request.getParameter("pId"), 0);

	Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
	String action = request.getParameter("act");
	if("delete".equals(action) && request.getParameter("forumId") != null && user != null)
	{
		int forumId = Tools.getIntValue(request.getParameter("forumId"), -1);
		if (forumId > 0)
		{
			boolean del = ForumDB.deleteMessage(forumId, docId, user);
			System.out.println("ForumID: "+forumId+"   result: "+del);
		}
	}

	if("recovery".equals(action) && request.getParameter("forumId")!= null && docId > 0 && user != null)
	{
		ForumDB.recoverMessage(Tools.getIntValue(request.getParameter("forumId"),0), docId, user);
	}

	if("approve".equals(action) && request.getParameter("forumId") != null)
	{
		int forumId = Tools.getIntValue(request.getParameter("forumId"), -1);
		if (forumId > 0)
		{
			boolean update = ForumDB.forumApprove(forumId);
		}
	}

	if("close".equals(action) && request.getParameter("forumId") != null)
	{
		int forumId = Tools.getIntValue(request.getParameter("forumId"), -1);
		if (forumId > 0)
		{
			ForumDB.closeForumTopic(forumId, true);
		}
	}

	if("open".equals(action) && request.getParameter("forumId") != null)
	{
		int forumId = Tools.getIntValue(request.getParameter("forumId"), -1);
		if (forumId > 0)
		{
			ForumDB.closeForumTopic(forumId, false);
		}
	}



	boolean active = false;
	boolean messageBoard = false;
	List<ForumBean> forum = Collections.emptyList();
	ForumGroupBean fgb;

	if (docId > 0)
	{
		fgb = ForumDB.getForum(docId);
		if (fgb == null || fgb.getDocId()!=docId)
		{
			fgb = new ForumGroupBean();
			fgb.setDocId(docId);
			fgb.setActive(true);

			//zisti ci nie sme message board
			DocDetails doc = DocDB.getInstance().getDoc(docId);
			if (doc != null)
			{
				TemplatesDB tempDB = TemplatesDB.getInstance();
				TemplateDetails temp = tempDB.getTemplate(doc.getTempId());
				if (temp == null)
				{
					temp = new TemplateDetails();
				}
				if (dataHasForumText(doc.getData()) || dataHasForumText(temp.getHeaderDocData()) || dataHasForumText(temp.getFooterDocData()) ||
						    dataHasForumText(temp.getMenuDocData()) || dataHasForumText(temp.getRightMenuDocData()) || dataHasForumText(temp.getObjectADocData()) ||
						    dataHasForumText(temp.getObjectBDocData()) || dataHasForumText(temp.getObjectCDocData()) || dataHasForumText(temp.getObjectDDocData()))
				{
					fgb.setMessageBoard(true);
				}
			}
		}

		if(fgb.isMessageBoard())
		{
			if (parentId > 0)
			{
				forum = ForumDB.getForumFieldsForDoc(request, docId, false, parentId, true, true);
			}
			else
			{
				forum = ForumDB.getForumTopics(docId, false, true);
				messageBoard = true;
			}
		}
		else
		{
			forum = ForumDB.getForumFieldsForDoc(request, docId, false, parentId, true, true);
		}

		request.setAttribute("forumGroupForm", fgb);
		request.setAttribute("forum", forum);

		active = ForumDB.isActive(docId);
	}

	int pageSize = 35;
	int pageLinksNum = 5;
	String offset = "0";
	String end = ""+(pageSize);
	request.setAttribute("pageSize", ""+pageSize);
	request.setAttribute("pageLinksNum", ""+pageLinksNum);
	request.setAttribute("pagingLink", "/components/forum/admin_forum_open.jsp");

	String fColor;
%>
<%@page import="sk.iway.iwcm.users.UserGroupsDB"%>
<%@page import="sk.iway.iwcm.users.UserGroupDetails"%>
<%= Tools.insertJQuery(request) %>
<%@ include file="/admin/layout_top_dialog.jsp" %>
<script type="text/javascript">
<!--
	function popupNewForum(parent, docId)
	{
		var options = "toolbar=no,scrollbars=no,resizable=yes,width=350,height=350;"
		editwindow=window.open("/components/forum/new_admin.jsp?parent="+parent+"&docid="+docId,'forumNew',options);
		if (!editwindow.opener) editwindow.opener = self;
		if (window.focus) {editwindow.focus()}
	}

	function editForum(forumId, docId)
	{
		var options = "toolbar=no,scrollbars=no,resizable=yes,width=350,height=350;"
		editwindow=window.open("/components/forum/new_admin.jsp?forumid="+forumId+"&parent="+forumId+"&docid="+docId+"&edit=true",'forumNew',options);
		if (!editwindow.opener) editwindow.opener = self;
		if (window.focus) {editwindow.focus()}
	}

	function delMessage(forumId)
	 {
		 if(window.confirm('<iwcm:text key="components.forum.delete_message_prompt"/> ?') == true)
	     {
	       document.actionForm.act.value="delete";
	       document.actionForm.forumId.value = forumId;
	       document.actionForm.pId.value = <%=parentId%>;
	       document.actionForm.submit();
	     }
	 }

	function recoveryP(forumId)
	{
	 	document.actionForm.act.value="recovery";
	 	document.actionForm.forumId.value=forumId;
	 	document.actionForm.pId.value = <%=parentId%>;
	   document.actionForm.submit();
	}

	 function approveMessage(forumId, parentId)
	 {
	    document.actionForm.act.value="approve";
	    document.actionForm.forumId.value = forumId;
	    document.actionForm.pId.value = parentId;
	    document.actionForm.submit();

	 }

	 function closeTopic(forumId)
	 {
	    document.actionForm.act.value="close";
	    document.actionForm.forumId.value = forumId;
	    document.actionForm.submit();
	 }

	 function openTopic(forumId)
	 {
	    document.actionForm.act.value="open";
	    document.actionForm.forumId.value = forumId;
	    document.actionForm.submit();
	 }


 	function setCalendarPublishStart()
	{
		document.forumGroupForm.dateFrom.value = dialogWin.returnedValue;
		if (document.forumGroupForm.dateFromTime.value=="")
		{
			document.forumGroupForm.dateFromTime.value="6:00";
		}
		//document.forumGroupForm.publicable.checked=true;
	}

	function setTimeStart()
	{
		document.forumGroupForm.dateFromTime.value = dialogWin.returnedValue;
	}

	function setCalendarPublishEnd()
	{
		document.forumGroupForm.dateTo.value = dialogWin.returnedValue;
		if (document.forumGroupForm.dateToTime.value=="")
		{
			document.forumGroupForm.dateToTime.value="23:59";
		}
	}

	function setTimeEnd()
	{
		document.forumGroupForm.dateToTime.value = dialogWin.returnedValue;
	}

	function addGroupId()
	{
		var value = document.forumGroupForm.adminGroups.value;
		if (value == "")
		{
			document.forumGroupForm.adminGroups.value = document.forumGroupForm.userGroups.value;
		}
		else
		{
			document.forumGroupForm.adminGroups.value = value + "+" + document.forumGroupForm.userGroups.value;
		}
	}

	function addGroupIdAddmessages()
	{
		var value = document.forumGroupForm.addmessageGroups.value;
		if (value == "")
		{
			document.forumGroupForm.addmessageGroups.value = document.forumGroupForm.userGroups2.value;
		}
		else
		{
			document.forumGroupForm.addmessageGroups.value = value + "+" + document.forumGroupForm.userGroups2.value;
		}
	}

	function Ok()
	{
		document.forumGroupForm.bSubmit.click();
	}
//-->
</script>

<script type="text/javascript" src="/admin/scripts/modalDialog.js"></script>

<link href="/components/forum/forum-admin.css" type="text/css" rel=stylesheet />
<link type="text/css" rel="stylesheet" media="screen" href="/admin/css/tabpane-luna.css" />

<style type="text/css">
	.dynamic-tab-pane-control .tab-page { height: 360px; }
	tr.trDeleted td, tr.trDeleted td a { color: red !important }
</style>

<div class="box_tab box_tab_thin left">
	<ul class="tab_menu" id="Tabs">
		<li class="first openFirst"><a href="#" onclick="showHideTab('1');" id="tabLink1"><iwcm:text key="components.forum.admin.settingsTab"/></a></li>
	</ul>
</div>

<div class="tab-pane toggle_content" id="tab-pane-1">
	<div class="tab-page" id="tabMenu1" style="display: block; width: 800px;">

	<form method="get" action='/components/forum/admin_forum_open.jsp' name="actionForm">
		<input type="hidden" name="act" />
		<input type="hidden" name="docId" value="<%=ResponseUtils.filter(request.getParameter("docid"))%>" />
		<input type="hidden" name="docid" value="<%=ResponseUtils.filter(request.getParameter("docid"))%>" />
		<input type="hidden" name="forumId" />
		<input type="hidden" name="pId" />
		<input type="hidden" name="pageNum" value="<%=Tools.getIntValue(request.getParameter("pageNum"), 1)%>" />
	</form>

	<logic:present parameter="saveOK">
		<p>
			<center>
				<font color="green" ><b><iwcm:text key="components.forum.save_ok"/>.</b></font>
			</center>
		</p>
		<script type="text/javascript">
		<!--
			window.close();
		//-->
		</script>
	</logic:present>


		<logic:present parameter="saveError">
			<p><center><font color="red" ><b><iwcm:text key="components.forum.save_error"/>.</b></font></center></p>
		</logic:present>

		<html:form method="post" action='/admin.forum.do'>
		 <%-- <table border="0" cellspacing="2" cellpadding="0">
		 	<tr>
		 		<td colspan="2"><strong><iwcm:text key="components.forum.admin.basicInfo"/></strong></td>
		 	</tr>--%>
		 	<fieldset>
		 		<legend><iwcm:text key="components.forum.admin.basicInfo"/>:</legend>
				<div class="col-md-12 col-sm-12">
					<div class="form-group">
						<label class="form-label"><iwcm:text key="components.forum.status"/></label>
						<div class="<iwcm:text key="user.email"/>">
						<%if (active){%>
						 <font color="green"><b><iwcm:text key="components.forum.status_true"/></b></font>
					  <%}else{%>
					  	 <font color="red"><b><iwcm:text key="components.forum.status_false"/></b></font>
					  <%}%>
					  	 </div>
					</div>
				</div>

				<div class="col-md-12 col-sm-12">
					<label class="form-label"><iwcm:text key="components.forum.admin.forumType"/>:</label>
					<div class="form-group">
					   <html:hidden property="messageBoard"/>
				  	   <% if (messageBoard) { %>
				  	   <iwcm:text key="components.forum.admin.forumType.mb"/>
				  	   <% } else { %>
				  	   <iwcm:text key="components.forum.admin.forumType.simple"/>
				  	   <% } %>
					</div>
				</div>




				<div class="col-md-12 col-sm-12">
					<div class="form-group" <% if (messageBoard==false) out.print("style='display: none;'"); %>>
						<label class="form-label"><iwcm:text key="components.forum.admin.advertisementType"/></label>
			  			<html:checkbox property="advertisementType"/>
			  		</div>
			  	</div>
			  	<div class="col-md-4 col-sm-4" >
					<div class="form-group">
						<label class="control-label display-block">&nbsp;</label>
						<html:checkbox property="messageConfirmation"/>
						<label class="form-label"><iwcm:text key="components.forum.confirmation"/></label>
					</div>
				</div>
				<div class="col-md-8 col-sm-8">
					<div class="form-group">
						<label class="form-label"><iwcm:text key="user.email"/>:</label>
						<html:text property="approveEmail" size="30" styleClass="form-control"/>
					</div>
				</div>


				<div class="col-md-12 col-sm-12">
					<div class="form-group">
						<label class="form-label"><iwcm:text key="components.forum.send_notif"/></label>
						<html:text property="notifEmail" size="30" styleClass="form-control" />
					</div>
				</div>
			</fieldset>
			<fieldset>
				<legend><iwcm:text key="user.permissions"/></legend>
				<div class="col-md-12">
					<div class="form-group">
						<label class="form-label"><iwcm:text key="components.forum.user_groups_for_addmessage"/></label>
						<br/>
						<div class="input-group">
							<select name="userGroups2" class="form-control">
					 			<option value=""></option>
					 		  <logic:iterate id="g" name="groups" type="sk.iway.iwcm.users.UserGroupDetails">
								 <option value='<%=g.getUserGroupId()%>'><%=g.getUserGroupName()%></option>
							  </logic:iterate>
							</select>
							<span class="input-group-btn">
							<input type="button" class="btn button50" value="<iwcm:text key="components.news.addgroup"/>" onclick="javascript:addGroupIdAddmessages();"/>
							</span>
						</div>
					</div>
				</div>

				<div class="col-md-12 col-sm-12">
					<div class="form-group">
						<html:text property="addmessageGroups" size="30" styleClass="form-control"/>
					</div>
					<div class="form-group" <% if (messageBoard==false) out.print("style='display: none;'"); %>>
				  	 		<label class="form-label"><iwcm:text key="components.forum.adminGroups"/></label>
							<div class="input-group">
								<select name="userGroups" class="form-control">
									<option value=""></option>
								<logic:iterate id="g" name="groups" type="sk.iway.iwcm.users.UserGroupDetails">
									<option value='<%=g.getUserGroupId()%>'><%=g.getUserGroupName()%></option>
								</logic:iterate>
								</select>
								<span class="input-group-btn">
									<input type="button" class="btn button50" value="<iwcm:text key="components.news.addgroup"/>" onclick="javascript:addGroupId();"/>
								</span>
							</div>
							<br/>
							<html:text property="adminGroups" size="30" styleClass="form-control" />
					</div>
				</div>
			</fieldset>

			<fieldset>
				<legend><iwcm:text key="components.forum.admin.availability"/></legend>
				<div class="col-md-12 col-sm-12">
					<div class="form-group">
						<label class="form-label"><iwcm:text key="components.forum.active"/></label>
						<html:checkbox property="active"/>
					</div>
				</div>

				<div class="col-md-6 col-sm-6">
				  	<div class="form-group">
						<label class="form-label"><iwcm:text key="components.forum.date_from"/></label>
						<div class="input-group date date-picker">
							<html:text property="dateFrom" styleId="dateFromId" maxlength="10" size="10" styleClass="form-control" />
							<span class="input-group-btn"><button class="btn default" type="button"><i class="fa fa-calendar"></i></button></span>
						</div>

					</div>
				</div>

				<div class="col-md-6 col-sm-6">
				  	<div class="form-group">
						<label for="dateFromTimeId"><iwcm:text key="editor.time"/>:</label>
						<div class="input-group">
							<html:text property="dateFromTime" styleClass="timepicker timepicker-24 form-control" styleId="dateFromTimeId" maxlength="5" size="5"/>
							<span class="input-group-btn"><button type="button" class="btn default"><i class="fa fa-clock-o"></i></button></span>
						</div>
					</div>
				</div>




				<div class="col-md-6 col-sm-6">
				  	<div class="form-group">
						<label for="dateToId"><iwcm:text key="components.forum.date_to"/></label>
						<div class="input-group date date-picker">
							<html:text property="dateTo" maxlength="10" size="10" styleClass="form-control" styleId="dateToId" />
							<span class="input-group-btn"><button class="btn default" type="button"><i class="fa fa-calendar"></i></button></span>
						</div>
					</div>
				</div>


				<div class="col-md-6 col-sm-6">
				  	<div class="form-group">
						<label for="dateToTimeId"><iwcm:text key="editor.time"/>:</label>
						<div class="input-group">
							<html:text property="dateToTime" styleId="dateToTimeId" styleClass="timepicker timepicker-24 form-control" maxlength="5" size="5" />
							<span class="input-group-btn"><button type="button" class="btn default"><i class="fa fa-clock-o"></i></button></span>
						</div>
					</div>
				</div>


				<div class="col-md-6 col-sm-6">
				  	<div class="form-group">
						<label for="dateFromTimeId"><iwcm:text key="components.forum.hours_after"/>:</label>
						<html:text property="hoursAfterLastMessage" size="2" maxlength="4" styleClass="form-control" />
					</div>
				</div>

				<div class="col-md-12 col-sm-12" style="display: none;">
				  	<div class="form-group">
				  		<input type="hidden" name="docId" value="<%=ResponseUtils.filter(request.getParameter("docid"))%>" />
				  		<input type="hidden" name="docid" value="<%=ResponseUtils.filter(request.getParameter("docid"))%>" />
				  		<html:hidden property="id"/>
						<input type="submit" name="bSubmit" value='<iwcm:text key="button.save"/>' class="button50" />
					</div>
				</div>
            </fieldset>
		 	<%--<tr>
			  <td><iwcm:text key="components.forum.status"/></td>
			  <td><%if (active){%>
					 <font color="green"><b><iwcm:text key="components.forum.status_true"/></b></font>
				  <%}else{%>
				  	 <font color="red"><b><iwcm:text key="components.forum.status_false"/></b></font>
				  <%}%>
			   </td>
			</tr>
		 	<tr>
			  <td><iwcm:text key="components.forum.admin.forumType"/></td>
			  <td><html:hidden property="messageBoard"/>
			  	   <% if (messageBoard) { %>
			  	   <iwcm:text key="components.forum.admin.forumType.mb"/>
			  	   <% } else { %>
			  	   <iwcm:text key="components.forum.admin.forumType.simple"/>
			  	   <% } %>
			  </td>
			</tr>
			<tr <% if (messageBoard==false) out.print("style='display: none;'"); %>>
			  <td><iwcm:text key="components.forum.admin.advertisementType"/></td>
			  <td><html:checkbox property="advertisementType"/></td>
			</tr>
			<tr>
			  <td><iwcm:text key="components.forum.confirmation"/></td>
			  <td><html:checkbox property="messageConfirmation"/>
			  		<iwcm:text key="user.email"/>: <html:text property="approveEmail" size="30"/>
			  </td>
			</tr>
			<tr>
			  <td><iwcm:text key="components.forum.send_notif"/></td>
			  <td><html:text property="notifEmail" size="30"/></td>
			</tr>
			<tr>
		 		<td colspan="2"><strong><iwcm:text key="user.permissions"/></strong></td>
		 	</tr>
		 	<tr>
	  	 		<td valign="top"><iwcm:text key="components.forum.user_groups_for_addmessage"/></td>
	  	 		<td nowrap="nowrap">
			 		<select name="userGroups2">
			 			<option value=""></option>
			 		  <logic:iterate id="g" name="groups" type="sk.iway.iwcm.users.UserGroupDetails">
						 <option value='<%=g.getUserGroupId()%>'><%=g.getUserGroupName()%></option>
					  </logic:iterate>
					</select><input type="button" class="button50" value="<iwcm:text key="components.news.addgroup"/>" onclick="javascript:addGroupIdAddmessages();"/>
					<br/>
					<html:text property="addmessageGroups" size="30"/>
				</td>
	  	 	</tr>
			<tr <% if (messageBoard==false) out.print("style='display: none;'"); %>>
	  	 		<td valign="top"><iwcm:text key="components.forum.adminGroups"/></td>
	  	 		<td>
			 		<select name="userGroups">
			 			<option value=""></option>
			 		  <logic:iterate id="g" name="groups" type="sk.iway.iwcm.users.UserGroupDetails">
						 <option value='<%=g.getUserGroupId()%>'><%=g.getUserGroupName()%></option>
					  </logic:iterate>
					</select>
					<input type="button" value="<iwcm:text key="components.news.addgroup"/>" onclick="javascript:addGroupId();"/>
					<br/>
					<html:text property="adminGroups" size="30"/>
				</td>
	  	 	</tr>
		 	<tr>
		 		<td colspan="2"><strong><iwcm:text key="components.forum.admin.availability"/></strong></td>
		 	</tr>
			<tr>
			  <td><iwcm:text key="components.forum.active"/></td>
			  <td><html:checkbox property="active"/></td>
			</tr>
			<tr>
			   <td><label for="dateFromId"><iwcm:text key="components.forum.date_from"/></label></td>
				<td nowrap="nowrap">
					<html:text property="dateFrom" styleId="dateFromId" maxlength="10" size="10" styleClass="datepicker" />
					<label for="dateFromTimeId"><iwcm:text key="editor.time"/>:</label>
					<html:text property="dateFromTime" styleClass="timepicker" styleId="dateFromTimeId" maxlength="5" size="5"/>
				</td>
			</tr>
			<tr>
				<td><label for="dateToId"><iwcm:text key="components.forum.date_to"/></label></td>
				<td nowrap="nowrap">
					<html:text property="dateTo" maxlength="10" size="10" styleClass="datepicker" styleId="dateToId" />
					<label for="dateToTimeId"><iwcm:text key="editor.time"/>:</label>
					<html:text property="dateToTime" styleId="dateToTimeId" styleClass="timepicker" maxlength="5" size="5" />
				</td>
			</tr>
			<tr>
				<td width="230"><iwcm:text key="components.forum.hours_after"/></td>
				<td><html:text property="hoursAfterLastMessage" size="2" maxlength="4" /></td>
			</tr>
			<tr style="display: none;">
			  <td>
			  		<input type="hidden" name="docId" value="<%=ResponseUtils.filter(request.getParameter("docid"))%>" />
			  		<input type="hidden" name="docid" value="<%=ResponseUtils.filter(request.getParameter("docid"))%>" />
			  		<html:hidden property="id"/>
			  </td>
			  <td align="right"><input type="submit" name="bSubmit" value='<iwcm:text key="button.save"/>' class="button50" /></td>
			</tr>
		 </table>--%>
		</html:form>

	</div>
</div>

<logic:present parameter="act">
	<script>
	<!--
		document.actionForm.act.value="";
	//-->
	</script>
</logic:present>

<%@ include file="/admin/layout_bottom_dialog.jsp" %>