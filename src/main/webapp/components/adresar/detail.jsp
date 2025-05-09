<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@ 
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@ 
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@ 
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@ 
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@ 
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@ 
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@ 
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@ 
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%@page import="sk.iway.iwcm.users.UserDetails"%>
<%@page import="org.apache.commons.beanutils.BeanUtils"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%
Identity currUser = UsersDB.getCurrentUser(request);
if (currUser == null)
{
	//user nie je prihlaseny
	%><iwcm:text key="components.docman.user.not_logged"/><%
	return;
}
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);
Prop prop = Prop.getInstance(lng);
String userGroupIds =  "32";
if(Tools.isEmpty(userGroupIds))
{
	%><iwcm:text key="components.adresar.group.nenasiel"/><%
	return;
}
int userId = Tools.getIntValue(request.getParameter("id"),-1);
if(userId == -1)
{
	%><iwcm:text key="components.adresar.user.not_exist"/><%
	return;
}

UserDetails user = UsersDB.getUser(userId);
if(user == null)
{
	%><iwcm:text key="components.adresar.user.not_exist"/><%
	return;
}
else
{
	if(user.isAuthorized() == false)
	{
		%><iwcm:text key="components.adresar.user.not_exist"/><%
		return;
	}
	else
	{
		//skontrolujem aj ci sa uzivatel nachadza v zadanej skupine
		boolean foundOk = false;
		if(Tools.isNotEmpty(userGroupIds) && userGroupIds.indexOf(",") != -1)
			userGroupIds = Tools.replace(userGroupIds, ",", "+");
		String groupIdsList[] = Tools.getTokens(userGroupIds, "+");
		for(String groupId : groupIdsList)
		{
			int groupIdInt = Tools.getIntValue(groupId, -1);
			if(user.isInUserGroup(groupIdInt))
			{
				foundOk = true;
				break;
			}
		}
		if(foundOk == false)
		{
			%><iwcm:text key="components.adresar.user.not_exist"/><%
			return;
		}
	}
}

String fields[] = {"A", "B", "C", "D", "E"};
for (String pismeno : fields)
{	
	String fieldTmp = (String)BeanUtils.getProperty(user, "field"+pismeno);
	if(Tools.isNotEmpty(fieldTmp))
	{
		request.setAttribute("maVolPol","true");
		break;
	}
}


request.setAttribute("cmpName", "adresar");
request.setAttribute("titleKey", user.getFullName());
request.setAttribute("descKey", "");
request.setAttribute("user",user);
%>
<jsp:include page="/components/top-public.jsp"/>
<table border="0" cellspacing="0" cellpadding="0">
	<tr>
		<td valign="top" style="padding-right: 15px;">
			<table border="0" cellspacing="0" cellpadding="1">
				<tr>
					<td colspan="2"><strong><iwcm:text key="useredit.personal_info"/></strong></td>
				</tr>
				<iwcm:notEmpty name="user" property="title">
				<tr>
					<td nowrap="nowrap"><iwcm:text key="user.title"/>:</td>
					<td nowrap="nowrap"><iwcm:strutsWrite name="user" property="title"/></td>
				</tr>
				</iwcm:notEmpty>
				<tr>
					<td nowrap="nowrap" class="requiredField"><iwcm:text key="user.firstName"/>:</td>
					<td nowrap="nowrap"><iwcm:strutsWrite name="user" property="firstName"/></td>
				</tr>
				<tr>
					<td nowrap="nowrap" class="requiredField"><iwcm:text key="user.lastName"/>:</td>
					<td nowrap="nowrap"><iwcm:strutsWrite name="user" property="lastName"/></td>
				</tr>
				<tr>
					<td><iwcm:text key="reguser.sex"/>:</td>
					<iwcm:equal name="user" property="sexMale" value="true">
					<td>
						<iwcm:text key="reguser.male"/>
					</td>
					</iwcm:equal>
					<iwcm:equal name="user" property="sexMale" value="false">
					<td>
						<iwcm:text key="reguser.female"/>
					</td>
					</iwcm:equal>
				</tr>
				<iwcm:notEmpty name="user" property="photo">
				<tr>
					<td valign="top"><iwcm:text key="components.user.photo_in_system"/>:&nbsp;</td>
					<td><img src="<%=Tools.isNotEmpty(user.getPhotoOriginal()) ? "/thumb"+user.getPhotoOriginal()+"?h=150&ip=2" : ""%>"></td>
				</tr>	
				</iwcm:notEmpty>
				<iwcm:notEmpty name="user" property="signature">
				<tr>
					<td valign="top"><iwcm:text key="reguser.signature"/>:</td>
					<td><iwcm:strutsWrite name="user" property="signature"/></td>
				</tr>
				</iwcm:notEmpty>
			</table>
			<iwcm:empty name="user" property="photo"><iwcm:hidden name="user" property="photo"/></iwcm:empty>
		</td>
		<td valign="top">
			<table border="0" cellspacing="0" cellpadding="1">	
				<tr>
					<td colspan="2"><strong><iwcm:text key="user.contact"/></strong></td>
				</tr>
				<iwcm:notEmpty name="user" property="title">
				<tr>
					<td nowrap="nowrap"><iwcm:text key="user.title"/>:</td>
					<td nowrap="nowrap"><iwcm:strutsWrite name="user" property="title"/></td>
				</tr>
				</iwcm:notEmpty>
				<iwcm:notEmpty name="user" property="country">
				<tr>
					<td nowrap="nowrap"><iwcm:text key="user.country"/>:</td>
					<td nowrap="nowrap"><iwcm:strutsWrite name="user" property="country"/></td>
				</tr>
				</iwcm:notEmpty>
				<iwcm:notEmpty name="user" property="allowLoginStart">
				<tr>
					<td nowrap="nowrap" class="requiredField"><iwcm:text key="settings.in_firm_from"/>:</td>
					<td nowrap="nowrap"><iwcm:strutsWrite name="user" property="allowLoginStart"/></td>
				</tr>
				</iwcm:notEmpty>
				<tr>
					<td nowrap="nowrap" class="requiredField"><iwcm:text key="user_details.position"/>:</td>
					<td nowrap="nowrap"><iwcm:strutsWrite name="user" property="position"/></td>
				</tr>
				<iwcm:notEmpty name="user" property="fieldB">
				<tr>
					<td nowrap="nowrap"><iwcm:text key="user.fieldB"/>:</td>
					<td nowrap="nowrap"><iwcm:strutsWrite name="user" property="fieldB"/></td>
				</tr>
				</iwcm:notEmpty>
				<iwcm:notEmpty name="user" property="fieldC">
				<tr>
					<td nowrap="nowrap"><iwcm:text key="user.fieldC"/>:</td>
					<td nowrap="nowrap"><iwcm:strutsWrite name="user" property="fieldC"/></td>
				</tr>
				</iwcm:notEmpty>
				<tr>
					<td nowrap="nowrap"><iwcm:text key="user.parent"/>:</td>
					<td nowrap="nowrap"><%=user.getParentId() > 0 ? UsersDB.getUser(user.getParentId()).getFullName() : prop.getText("user.parent.empty")%></td>
				</tr>
				<tr>
					<td nowrap="nowrap" class="requiredField"><iwcm:text key="user.email"/>:</td>
					<td nowrap="nowrap"><a href="mailto:<iwcm:strutsWrite name="user" property="email"/>"><iwcm:strutsWrite name="user" property="email"/></a></td>
				</tr>
				<iwcm:notEmpty name="user" property="phone">
				<tr>
					<td nowrap="nowrap"><iwcm:text key="user.phone"/>:</td>
					<td nowrap="nowrap"><iwcm:strutsWrite name="user" property="phone"/></td>
				</tr>
				</iwcm:notEmpty>
				<%
				String profilCoChcemAbyVedeli = currUser != null && currUser.getSettings().get("co_chcem_aby_vedeli") != null && currUser.getSettings().get("co_chcem_aby_vedeli").getSvalue1() != null ? currUser.getSettings().get("co_chcem_aby_vedeli").getSvalue1() : "";
				if(Tools.isNotEmpty(profilCoChcemAbyVedeli)){
				%>
				<tr>
					<td colspan="2">Čo chcem, aby ostatní o mne vedeli:<br/><%=profilCoChcemAbyVedeli%></td>
				</tr>
				<%}%>
				<iwcm:notEmpty name="maVolPol">
					<iwcm:notEmpty name="user" property="fieldA">
					<tr>
						<td nowrap="nowrap"><iwcm:text key="user.fieldA"/>:</td>
						<td nowrap="nowrap"><iwcm:strutsWrite name="user" property="fieldA"/></td>
					</tr>
					</iwcm:notEmpty>
					<iwcm:notEmpty name="user" property="fieldD">
					<tr>
						<td nowrap="nowrap"><iwcm:text key="user.fieldD"/>:</td>
						<td nowrap="nowrap"><iwcm:strutsWrite name="user" property="fieldD"/></td>
					</tr>
					</iwcm:notEmpty>
					<iwcm:notEmpty name="user" property="fieldE">
					<tr>
						<td nowrap="nowrap"><iwcm:text key="user.fieldE"/>:</td>
						<td nowrap="nowrap"><iwcm:strutsWrite name="user" property="fieldE"/></td>
					</tr>
					</iwcm:notEmpty>
				</iwcm:notEmpty>
			</table>
		</td>
	</tr>
</table>
<jsp:include page="/components/bottom-public.jsp"/>
<script type="text/javascript">
<!--
	document.getElementById("btnOk").style.display = "none";
	document.getElementById("btnCancel").value = "<iwcm:text key='button.close'/>";
//-->
</script>
