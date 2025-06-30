<%@page import="java.util.List"%><% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page language="java" pageEncoding="utf-8" import="sk.iway.iwcm.stat.*,sk.iway.iwcm.users.*,java.util.*,sk.iway.iwcm.system.msg.*" %>

<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<iwcm:checkLogon admin="true" perms="menuMessages"/>
<%@page import="sk.iway.iwcm.tags.support.ResponseUtils"%>
<%@ page import="sk.iway.iwcm.Tools" %>

<%
	List sessionList = SessionHolder.getInstance().getList();
	String [] komu = null;
	if(request.getParameterValues("komu")!=null)
	{
		komu=request.getParameterValues("komu");
		for(int i=0;i<komu.length;i++)
		{
			AdminMessageBean am = new AdminMessageBean();
			am.setMessageText(Tools.getRequestParameter(request, "text"));
			am.setRecipientUserId(new Integer(komu[i]));

			if("true".equals(Tools.getRequestParameter(request, "onlyLogged")))
				am.setOnlyForLogged(Boolean.TRUE);
			else
				am.setOnlyForLogged(Boolean.FALSE);

			MessageDB.getInstance(false).saveMessage(session,am);
		}
	}

	List users = new ArrayList();
	int group = Tools.getIntValue(Tools.getRequestParameter(request, "group"),0);

	if(group<0)
		users = UsersDB.getUsers();
	else if(group>0)
		users = UsersDB.getUsersByGroup(group);
	else
		users = UsersDB.getAdmins();

	if("true".equals(Tools.getRequestParameter(request, "onlyLogged")))
	{
		List users2 = new ArrayList();
		for(int j=0;j<users.size();j++)
		{
			UserDetails ud = (UserDetails)users.get(j);

			for(int i=0;i<sessionList.size();i++)
			{
				SessionDetails ses = (SessionDetails)sessionList.get(i);
				try
				{
					if(ses.getLoggedUserId()>0 && ud.getUserId()==ses.getLoggedUserId())
					{
						users2.add(users.get(j));
						break;
					}
				}
				catch (Exception ex3)
				{
					sk.iway.iwcm.Logger.error(ex3);
				}
			}
		}
		users=users2;
	}

	int []ids=new int[0];
	if(komu!=null)
	{
		ids = new int[komu.length];
		for(int j=0;j<komu.length;j++)
			ids[j]=Integer.parseInt(komu[j]);
	}

	Arrays.sort(ids);
	List groups = UserGroupsDB.getInstance().getUserGroups();

	Prop prop = Prop.getInstance(sk.iway.iwcm.Constants.getServletContext(), request);
	request.setAttribute("iconLink", "");
	request.setAttribute("dialogTitle", prop.getText("components.messages.send_message.dialogTitle"));
	request.setAttribute("dialogDesc", prop.getText("components.messages.send_message.dialogMessage"));
%>

<%@ include file="/admin/layout_top_dialog.jsp" %>

<div class="padding10">

<iwcm:present parameter="komu">
	<script type="text/javascript">
	<!--
		window.close();
	//-->
	</script>
</iwcm:present>

<script type="text/javascript">
<!--
	function Ok()
	{
		var form = document.forms['sendMessageForm'];

		if(form.text.value == "" || form.komu.selectedIndex == -1)
			return;

		document.sendMessageForm.submit();
	}

	resizeDialog(510, 430);
//-->
</script>
	<form method="get" action="send_message.jsp" name="filterMessageForm">
		<table>
			<tr>
				<td>
					<select name="group" onchange="filterMessageForm.submit();" style="width: 460px;">
						<option value=""><iwcm:text key="components.messages.send_message.group"/></option>
						<option <%if(0 == group) out.print("selected ");%> value="0"><iwcm:text key="components.messages.send_message.admins"/></option>
						<%
							for(int i=0; i<groups.size(); i++)
							{
								UserGroupDetails ugd = ((UserGroupDetails)groups.get(i));
						%>
								<option <%if(ugd.getUserGroupId()==group) out.print("selected ");%> value="<%=ugd.getUserGroupId()%>">
									<%=ugd.getUserGroupName()%>
								</option>
						<%
							}
						%>
					</select>
				</td>
			</tr>
			<tr>
				<td>
					<label>
						<input type="checkbox" name="onlyLogged" value="true" onclick="document.forms['filterMessageForm'].submit()"
							<%if("true".equals(Tools.getRequestParameter(request, "onlyLogged"))) out.print("checked=\"true\"");%> />
						<iwcm:text key="components.messages.send_message.onlyLogged"/>
					</label>
				</td>
			</tr>
		</table>
	</form>

	<form method="get" action="send_message.jsp" name="sendMessageForm">
		<table>
			<tr>
				<td rowspan="2">
					<iwcm:text key="components.messages.send_message.recipient"/><br>
					<select name="komu" multiple="multiple" style="height:201px; width: 210px;">
						<%
							for(int i=0;i<users.size();i++)
							{
								UserDetails ud = (UserDetails)users.get(i);
						%>
								<option <%if(Arrays.binarySearch(ids,ud.getUserId())>=0) out.print("selected");%> value="<%=ud.getUserId()%>">
									<%=ResponseUtils.filter(ud.getFullName())%>
								</option>
						<%
							}
						%>
					</select>
				</td>
				<td rowspan="2">
					<iwcm:text key="admin.message_popup.text"/><br />
					<textarea name="text" cols="35" style="height:201px;"></textarea>
				</td>
			</tr>
		</table>
	</form>

</div>

<%@ include file="/admin/layout_bottom_dialog.jsp" %>
