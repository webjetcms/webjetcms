<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.doc.DocDB" %>
<%@ page import="sk.iway.iwcm.users.UserGroupDetails" %>
<%@ page import="sk.iway.iwcm.users.UserGroupsDB" %>
<%@ page import="sk.iway.iwcm.users.UsersDB" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Arrays" %>
<%@
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@
taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%@
taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %><%@
taglib uri="/WEB-INF/struts-html.tld" prefix="html" %><%@
taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib uri="/WEB-INF/displaytag.tld" prefix="display" %><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%!
////////////////////////////////////////////////////////////
//Formular pre registraciu, alebo zmenu udajov pouzivatela//
////////////////////////////////////////////////////////////

//toto sa pouziva na nastavenie atriutu class pre input pole
//parametre:
//required = zoznam povinnych poli z pageParams
//name = meno pola
//typ pola = dodatocna trieda (email, date, safeChars - viz  check_form_impl.jsp)
public String isReq(String required, String name, String type)
{
	if ((","+required+",").indexOf(","+name+",")!=-1) return "required "+type;
	return type;
}

public String isShow(String show, String name)
{
	if (Tools.isEmpty(show)) return "";
	if ((","+show+",").indexOf(","+name+",")!=-1) return "";
	return " style='display: none;'";
}
%><%
PageParams pageParams = new PageParams(request);
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);
int authorizeEmailDocId = pageParams.getIntValue("authorizeEmailDocId", -1);
if(authorizeEmailDocId > 0 ) request.setAttribute("authorizeEmailDocId", authorizeEmailDocId);

String hash = request.getParameter("hash");
if (Tools.isNotEmpty(hash))
{
	//jedna sa o autorizacnu linku
	pageContext.include("authorize.jsp");
	return;
}

if (Constants.getBoolean("editorEnableXHTML")) pageContext.setAttribute(org.apache.struts.Globals.XHTML_KEY, "true", PageContext.PAGE_SCOPE);




//ak je nastavene, tak je tam docid stranky, ktora sa zobrazi po uspesnom ulozeni udajov
int successDocId = pageParams.getIntValue("successDocId", -1);
String url = "/showdoc.do?docid="+successDocId;
if (Constants.getInt("linkType")==Constants.LINK_TYPE_HTML) url = DocDB.getURLFromDocId(successDocId, request);

//required obsahuje zoznam povinnych poli
String required = ","+pageParams.getValue("required","login,password,firstName,lastName,email").replace('+', ',')+",";

//show obsahuje zoznam poli, ktore sa maju zobrazit
String show = ","+pageParams.getValue("show","").replace('+', ',')+",";

//ak je nastavene na true, pouzije sa AJAX na odoslanie formularu
//cez AJAX nie je mozne odosielat fotku
boolean useAjax = pageParams.getBooleanValue("useAjax", false);

//tu sa vykona ulozenie pouzivatela do databazy
if (request.getParameter("bSubmit") != null)
{
	pageContext.include("/RegUser.action");
}

//legend kluc je zobrazeny podla toho, ci je prihlaseny, alebo nie je prihlaseny pouzivatel
Identity user = UsersDB.getCurrentUser(request);
String legendKey = "components.users.newuser.newuserLegend";
if (user!=null) legendKey = "components.users.newuser.edituserLegend";

//toto je nastavene na hodnotu "true" ak sa udaje uspesne ulozili
String saveOK = (String)request.getAttribute("RegUserActionsaveOK");

if ("true".equals(saveOK))
{
	String messageKey = (String)request.getAttribute("regUserMessageKey");
	if (messageKey == null) messageKey = "components.user.newuser.saveSuccess";
	//ulozenie pouzivatela prebehlo uspesne
%>
	<script type="text/javascript">
	<% if (successDocId>0) { %>
		//window.alert("redirecting to: <%=url%>");
		window.location.href="<%=url%>";
	<% } else { %>
		window.alert("<iwcm:text key="<%=messageKey%>"/>");
	<% } %>
	</script>
<%
}
%>

<style>
#userForm fieldset, #userForm fieldset p{
clear: both;
}

#userForm fieldset p, #userForm fieldset p span{
display: block;
}

#userForm label{
display: block;
width: 150px;
float: left;
cursor: pointer;
cursor: hand;
}

#userForm span label{
width: 50px;
padding: 4px 0px 0px 0px;
}
#userForm input.input{
float: left;
}
</style>
<!-- REGISTRACIA POUZIVATELA ZACIATOK -->
<script type="text/javascript" src="/components/form/check_form.js"></script>
<% if (useAjax) { %>
<%=Tools.insertJQuery(request) %>
<script type="text/javascript" src="/components/_common/javascript/ajax_form_send.js.jsp"></script>
<% } %>
<stripes:useActionBean var="actionBean" beanclass="sk.iway.iwcm.stripes.RegUserAction" />

<div id="regUserFormDiv">
<iwcm:stripForm id="userForm" action="<%=PathFilter.getOrigPathUpload(request)%>" method="post" name="addUserForm" beanclass="sk.iway.iwcm.stripes.RegUserAction">
	<fieldset>
		<legend><iwcm:text key="<%=legendKey %>"/></legend>
		<stripes:errors></stripes:errors>
		<div id="formResult"><div id="ajaxFormResultContainer"></div></div>

		<p<%=isShow(show, "login")%>>
			<stripes:label for="usrLogin" name="usr.login"><iwcm:text key="components.user.newuser.login"/>:</stripes:label>
			<stripes:text class='<%=isReq(required, "login", "")%>' id="usrLogin" name="usr.login" size="40" maxlength="128" />
		</p>
		<p<%=isShow(show, "password")%>>
			<stripes:label for="usrPassword" name="usr.password"><iwcm:text key="components.user.newuser.password"/>:</stripes:label>
			<stripes:password class='<%=isReq(required, "password", "minLen3")%>' id="usrPassword" name="usr.password" size="20" maxlength="64" value='<%=(user != null) ? sk.iway.iwcm.common.UserTools.PASS_UNCHANGED : "" %>' />
		</p>
		<%
			if (user == null) {
		%>
		<p<%=isShow(show, "password2")%>>
			<label for="usrPassword2" name="password2"><iwcm:text key="components.user.newuser.password2"/>:</label>
			<input type="password" class='<%=isReq(required, "password2", "minLen3")%>' autocomplete="off" id="usrPassword2" name="password2" size="20" maxlength="64" />
		</p>
		<%
			// len prihlaseny pouzivatel moze zadat stare heslo
			} else if (user != null && !user.isInUserGroup(Constants.getInt("socialMediaUserGroupId",-1))) {
		%>
		<p>
			<stripes:label for="usrOldPassword" name="usr.oldPassword"><iwcm:text key="components.user.newuser.oldPassword"/></stripes:label>
			<stripes:password id="usrOldPassword" name="usr.oldPassword" size="20" maxlength="64"/>
		</p>
		<%
			}
		%>
		<p<%=isShow(show, "title")%>>
			<stripes:label for="usrTitle" name="usr.title"><iwcm:text key="components.user.newuser.title"/>:</stripes:label>
			<stripes:text class='<%=isReq(required, "title", "")%>' id="usrTitle" name="usr.title" size="8" maxlength="16" />
		</p>
		<p<%=isShow(show, "firstName")%>>
			<stripes:label for="usrFirstName" name="usr.firstName"><iwcm:text key="components.user.newuser.firstName"/>:</stripes:label>
			<stripes:text class='<%=isReq(required, "firstName", "")%>' id="usrFirstName" name="usr.firstName" size="40" maxlength="128" />
		</p>
		<p<%=isShow(show, "lastName")%>>
			<stripes:label for="usrLastName" name="usr.lastName"><iwcm:text key="components.user.newuser.lastName"/>:</stripes:label>
			<stripes:text class='<%=isReq(required, "lastName", "")%>' id="usrLastName" name="usr.lastName" size="40" maxlength="255" />
		</p>
		<p<%=isShow(show, "company")%>>
			<stripes:label for="usrCompany" name="usr.company"><iwcm:text key="user.company"/>:</stripes:label>
			<stripes:text class='<%=isReq(required, "company", "")%>' id="usrCompany" name="usr.company" size="40" maxlength="255" />
		</p>
		<p<%=isShow(show, "email")%>>
			<stripes:label for="usrEmail" name="usr.email"><iwcm:text key="components.user.newuser.email"/>:</stripes:label>
			<stripes:text class='<%=isReq(required, "email", "email")%>' id="usrEmail" name="usr.email" size="40" maxlength="255" />
		</p>
		<p<%=isShow(show, "dateOfBirth")%>>
			<stripes:label for="usrDateOfBirth" name="usr.dateOfBirth"><iwcm:text key="components.user.newuser.dateOfBirth"/>:</stripes:label>
			<stripes:text class='<%=isReq(required, "dateOfBirth", "date")%>' id="usrDateOfBirth" name="usr.dateOfBirth" size="10" maxlength="10" />
		</p>
		<p<%=isShow(show, "sexMale")%>>
			<stripes:label for="usrSexMale" name="usr.sexMale"><iwcm:text key="components.user.newuser.sexMale" />:</stripes:label>
			<span id="usrSexSpan">
				<stripes:radio id="usrSexMale" name="usr.sexMale" value="true" class="input" />
				<label for="usrSexMale"><iwcm:text key="components.user.newuser.male"/></label>

			  	<stripes:radio id="usrSexFemale" name="usr.sexMale" value="false" class="input" />
				<label for="usrSexFemale"><iwcm:text key="components.user.newuser.female"/></label>
			</span>
		</p>
		<p <%if (useAjax) {out.print(" style='display: none;'"); /*cez Ajax nie je mozne odosielat subor*/ } else { out.print(isShow(show, "userImage")); } %>>
		 <stripes:label for="userImageId" name="userImage"><iwcm:text key="components.user.newuser.userImage"/>:</stripes:label>
		 <stripes:file id="userImageId" name="userImage" size="40" />
		</p>
		<iwcm:notEmpty name="actionBean" property="usr.photo">
			<p>
				<stripes:label for="usrPhoto" name="usr.photo"><iwcm:text key="components.user.newuser.photo"/>:</stripes:label>
				<img src="<iwcm:beanWrite name="actionBean" property="usr.photoSmall"/>">
			</p>
		</iwcm:notEmpty>
		<p<%=isShow(show, "signature")%>>
			<stripes:label for="usrSignature" name="usr.signature"><iwcm:text key="components.user.newuser.signature"/>:</stripes:label>
			<stripes:text class='<%=isReq(required, "signature", "")%>' id="usrSignature" name="usr.signature" size="40" maxlength="255" />
		</p>
		<p<%=isShow(show, "adress")%>>
			<stripes:label for="usrAdress" name="usr.adress"><iwcm:text key="components.user.newuser.adress"/>:</stripes:label>
			<stripes:text class='<%=isReq(required, "adress", "")%>' id="usrAdress" name="usr.adress" size="40" maxlength="255" />
		</p>
		<p<%=isShow(show, "city")%>>
			<stripes:label for="usrCity" name="usr.city"><iwcm:text key="components.user.newuser.city"/>:</stripes:label>
			<stripes:text class='<%=isReq(required, "city", "")%>' id="usrCity" name="usr.city" size="40" maxlength="255" />
		</p>
		<p<%=isShow(show, "zip")%>>
			<stripes:label for="usrZip" name="usr.zip"><iwcm:text key="components.user.newuser.zip"/>:</stripes:label>
			<stripes:text class='<%=isReq(required, "zip", "")%>' id="usrZip" name="usr.zip" size="5" maxlength="5" />
		</p>
		<p<%=isShow(show, "country")%>>
			<stripes:label for="usrCountry" name="usr.country"><iwcm:text key="components.user.newuser.country"/>:</stripes:label>
			<stripes:text class='<%=isReq(required, "country", "")%>' id="usrCountry" name="usr.country" size="40" maxlength="255" />
		</p>
		<p<%=isShow(show, "phone")%>>
			<stripes:label for="usrPhone" name="usr.phone"><iwcm:text key="components.user.newuser.phone"/>:</stripes:label>
			<stripes:text class='<%=isReq(required, "phone", "phone")%>' id="usrPhone" name="usr.phone" size="40" maxlength="255" />
		</p>
		<% if (Tools.isNotEmpty(pageParams.getValue("fieldALabel",null))){ %>
		<p>
			<stripes:label for="usr.fieldA" name="usr.fieldA"><%=pageParams.getValue("fieldALabel","poleA") %>:</stripes:label> <stripes:text name="usr.fieldA" />
		</p>
		<%} if (Tools.isNotEmpty(pageParams.getValue("fieldBLabel",null))){ %>
		<p>
			<stripes:label for="usr.fieldB" name="usr.fieldB"><%=pageParams.getValue("fieldBLabel","poleB") %>:</stripes:label> <stripes:text name="usr.fieldB" />
		</p>
		<%} if (Tools.isNotEmpty(pageParams.getValue("fieldCLabel",null))){ %>
		<p>
			<stripes:label for="usr.fieldC" name="usr.fieldC"><%=pageParams.getValue("fieldCLabel","poleC") %>:</stripes:label> <stripes:text name="usr.fieldC" />
		</p>
		<%} if (Tools.isNotEmpty(pageParams.getValue("fieldDLabel",null))){ %>
		<p>
			<stripes:label for="usr.fieldD" name="usr.fieldD"><%=pageParams.getValue("fieldDLabel","poleD") %>:</stripes:label> <stripes:text name="usr.fieldD" />
		</p>
		<%} if (Tools.isNotEmpty(pageParams.getValue("fieldELabel",null))){ %>
		<p>
			<stripes:label for="usr.fieldE" name="usr.fieldE"><%=pageParams.getValue("fieldELabel","poleE") %>:</stripes:label> <stripes:text name="usr.fieldE" />
		</p>
		<%}
		if (Tools.isNotEmpty(pageParams.getValue("groupIdsEditable",null))){
			List<String> positionList = Arrays.asList(pageParams.getValue("groupIdsEditable",null).split(","));
			for (String item: positionList) {
				UserGroupDetails tempGroupDetails = UserGroupsDB.getInstance().getUserGroup(Tools.getIntValue(item,-1));
				if (tempGroupDetails==null  || !tempGroupDetails.isAllowUserEdit())
				    continue;
				pageContext.setAttribute("groupIdEditable", tempGroupDetails);
				%>
				<p>
					<stripes:label for="user_group_id">${groupIdEditable.userGroupNameComment}</stripes:label>
					<input name="user_group_id" id="user_group_id" type="checkbox" value="${groupIdEditable.userGroupId}" <c:if test="${actionBean.usr.isInUserGroup(groupIdEditable.userGroupId)}">checked</c:if>>
				</p>
			<%}
		}%>
		<p>
			<% if(sk.iway.iwcm.system.captcha.Captcha.isRequired("userform")) {%>
				<jsp:include page="/components/form/captcha.jsp" />
			<%}%>
		</p>

		<p>
			<% if (useAjax==false) { %>
			<stripes:submit id="bSubmitId" name="bSubmit"><iwcm:text key="button.save"/></stripes:submit>
			<% } else { %>
			<%=pageParams.persistToSession(request) %>
			<stripes:submit id="bSubmitIdAjax" name="bSubmit" onclick="return invokeWJAjax('regUserFormDiv', 'ajaxFormResultContainer', 'bSubmit', '/RegUserAjax.action');"><iwcm:text key="button.save"/></stripes:submit>
			<% } %>
		</p>
   </fieldset>
</iwcm:stripForm>
</div>
<% if (useAjax) { %>
<script type="text/javascript">
$("#ajaxFormResultContainer").hide("fast");
$("#userForm").attr("enctype", "");
</script>
<% } %>
<!-- REGISTRACIA POUZIVATELA KONIEC -->
