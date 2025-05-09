<%@page import="java.util.List"%><% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,java.util.*,sk.iway.iwcm.users.*,sk.iway.iwcm.doc.*,sk.iway.iwcm.i18n.*,sk.iway.iwcm.tags.*"%>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<%@page import="org.apache.struts.util.ResponseUtils"%>

<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	Prop prop = Prop.getInstance(sk.iway.iwcm.Constants.getServletContext(), lng, false);

	//Registracia do mailing listu, umozni zadat meno, email a skupiny emailov, ktore chce dostavat
	PageParams pageParams = new PageParams(request);

	UserGroupsDB userGroupsDB = UserGroupsDB.getInstance(sk.iway.iwcm.Constants.getServletContext(), false, DBPool.getDBName(request));
	List userGroupsList = userGroupsDB.getUserGroupsByTypeId(UserGroupDetails.TYPE_EMAIL);
	request.setAttribute("userGroupsList", userGroupsList);

	String title = request.getParameter("title");
	String firstName = request.getParameter("firstName");
	String lastName = request.getParameter("lastName");
	String email = request.getParameter("email");

	//skus najst prihlaseneho pouzivatela
	Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
	if (user!=null)
	{
		if (title==null) title = user.getTitle();
		if (firstName==null) firstName = user.getFirstName();
		if (lastName==null) lastName = user.getLastName();
		if (email==null) email = user.getEmail();
	}

	if (title==null) title="";
	if (firstName==null) firstName="";
	if (lastName==null) lastName="";

	if (email==null) email="";

	if ("true".equals(request.getParameter("save")))
	{
		//skontroluj, ci je vsetko zadane spravne
		String errors = "";
		if (email.indexOf("@")==-1)
		{
			errors+="<li>"+prop.getText("dmail.subscribe.enter_email")+".</li>";
		}
		System.out.println("UGID: " + request.getParameterValues("user_group_id"));
		if (request.getParameterValues("user_group_id")==null)
		{
			errors+="<li>"+prop.getText("dmail.subscribe.select_groups")+".</li>";
		}

		if (sk.iway.iwcm.system.captcha.Captcha.validateResponse(request, request.getParameter("wjcaptcha"), "dmail")==false)
		{
			errors+="<li>"+prop.getText("captcha.textNotCorrect")+".</li>";
		}
		if (sk.iway.iwcm.system.stripes.CSRF.verifyTokenAndDeleteIt(request)==false)
		{
			errors+="<li>"+prop.getText("components.csrfError")+".</li>";
		}

		if (errors.length()>0)
		{
			request.setAttribute("errors", errors);
		}
		else
		{
			String emailBody = null;
			String emailSenderEmail = pageParams.getValue("senderEmail", "webmaster@"+Tools.getServerName(request));
			String emailSenderName = pageParams.getValue("senderName", emailSenderEmail);
			String emailSubject = pageParams.getValue("subject", prop.getText("dmail.subscribe.subject"));

			if (emailSenderEmail.length()>0)
			{
				String confirmUrl = Tools.addParametersToUrl(Tools.getBaseHref(request) + PathFilter.getOrigPathDocId(request), "hash=!HASH!");
				emailBody = prop.getText("dmail.subscribe.bodyNew", confirmUrl);
				emailBody = pageParams.getValue("emailBody", emailBody);

				int emailBodyId = pageParams.getIntValue("emailBodyId", -1);
				System.out.println(emailBodyId);
				if (emailBodyId>0)
				{
					DocDB docDB = DocDB.getInstance();
					DocDetails doc = docDB.getDoc(emailBodyId);

					emailBody = Tools.downloadUrl(Tools.getBaseHrefLoopback(request)+"/showdoc.do?docid="+emailBodyId);
					if (emailBody == null)
					{
						//nastala chyba, pouzijeme priamo text stranky
						emailBody = doc.getData();
					}
					if (Tools.isEmpty(emailSubject))
					{
						emailSubject = doc.getTitle();
					}
				}
			}

			//System.out.println(emailBody);

			emailBody = SendMail.createAbsolutePath(emailBody, request);
			//vsetko je OK, zapiseme to do DB
			int status = UsersDB.subscribeEmail(new UserDetails(request), request, emailBody, emailSenderEmail, emailSenderName, emailSubject);
			System.out.println("status="+status);
			if (status > 0)
			{
				//ok
				out.println("<script type='text/javascript'>window.alert(\""+Tools.replace(JSEscapeTag.jsEscape(prop.getText("dmail.subscribe.confirm_email_sent")), "<br>", "\\n")+".\");</script>");
			}
			else if (status == -2)
			{
				//nepodarilo sa poslat email
				out.println("<script type='text/javascript'>window.alert(\""+prop.getText("dmail.subscribe.email_send_error")+".\");</script>");
			}
			else if (status == -1)
			{
				//nepodarilo sa vytvorit usera
				out.println("<script type='text/javascript'>window.alert(\""+prop.getText("dmail.subscribe.db_error")+".\");</script>");
			}
			else
			{
				//neznama chyba
				out.println("<script type='text/javascript'>window.alert(\""+prop.getText("dmail.subscribe.unknown_error")+".\");</script>");
			}
		}
	}

	//vytvor si string s uz nastavenymi checkboxami
	String userGroups = ",";
	String groups[] = request.getParameterValues("user_group_id");
	if (groups!=null)
	{
		int i;
		for (i = 0; i < groups.length; i++)
		{
			userGroups = userGroups + groups[i] + ",";
		}
	}
	else
	{
		if (user!=null)
		{
			userGroups = ","+user.getUserGroupsIds()+",";
		}
	}

	//prislo potvrdenie zmeny
	if (request.getParameter("hash")!=null)
	{
		String hash = request.getParameter("hash");
		System.out.println("mam hash: " + hash);
		UserDetails authorizedUser = UsersDB.authorizeEmail(request, hash);
		if (authorizedUser == null)
		{
			//zadany hash neexistuje, alebo nastala ina chyba
			out.println("<script type='text/javascript'> window.alert(\""+prop.getText("dmail.subscribe.error_authorize_email")+".\");</script>");
		}
		else
		{
			//podarilo sa nam ho nacitat
			title = authorizedUser.getTitle();
			firstName = authorizedUser.getFirstName();
			lastName = authorizedUser.getLastName();
			email = authorizedUser.getEmail();
			userGroups = ","+authorizedUser.getUserGroupsIds()+",";
			out.println("<script type='text/javascript'> window.alert(\""+prop.getText("dmail.subscribe.email_authorized")+".\");</script>");
		}
	}
%>

<style>
<!--
	#subscribeForm fieldset, #subscribeForm fieldset p
	{
		clear: both;
	}

	#subscribeForm fieldset p, #subscribeForm fieldset p span
	{
		display: block;
	}

	#subscribeForm label
	{
		display: block;
		width: 150px;
		float: left;
		cursor: pointer;
		cursor: hand;
		padding-top: 5px;
	}

	#subscribeForm span label
	{
		width: 50px;
		padding: 4px 0px 0px 0px;
	}

	#subscribeForm input.input
	{
		float: left;
	}
//-->
</style>

<div>
	<iwcm:present name="errors">
		<strong><iwcm:text key="dmail.subscribe.errors"/>:</strong>
		<ul style="color: red">
			<iway:request name="errors"/>
		</ul>
	</iwcm:present>

	<form id="subscribeForm" action="<%=PathFilter.getOrigPathDocId(request) %>" style="padding:0px" method="post">
		<fieldset>
			<p>
				<label for="meno">
					<strong><iwcm:text key="dmail.subscribe.firstName"/>:</strong>
				</label>
				<input type="text" name="firstName" value="<%=ResponseUtils.filter(firstName)%>" id="meno" />
			</p>

			<p>
				<label for="priez">
					<strong><iwcm:text key="dmail.subscribe.lastName"/>:</strong>
				</label>
				<input type="text" name="lastName" value="<%=ResponseUtils.filter(lastName)%>" id="priez" />
			</p>

			<p>
				<label for="mail">
					<strong><iwcm:text key="dmail.subscribe.email"/>:</strong>
				</label>
				<input type="text" name="email" value="<%=ResponseUtils.filter(email)%>" id="mail" />
			</p>
		</fieldset>

		<fieldset>
			<iwcm:iterate id="ugd" name="userGroupsList" type="sk.iway.iwcm.users.UserGroupDetails">
			<% if (ugd.isAllowUserEdit()) { %>
			<p>
				<label for="user_group_id_<iwcm:strutsWrite name="ugd" property="userGroupId"/>">
					<strong><iwcm:strutsWrite name="ugd" property="userGroupName"/></strong>
					<iwcm:notEmpty name="ugd" property="userGroupComment">
						<br/>
						<iwcm:strutsWrite name="ugd" property="userGroupComment"/>
					</iwcm:notEmpty>
				</label>
				<input type="checkbox" name="user_group_id" id="user_group_id_<iwcm:strutsWrite name="ugd" property="userGroupId"/>" value="<iwcm:strutsWrite name="ugd" property="userGroupId"/>"<%
					if (userGroups.indexOf(","+ugd.getUserGroupId()+",")!=-1 || userGroups.length()<2) out.print(" checked='checked'");
					%> />&nbsp;
			</p>

			<% } %>
			</iwcm:iterate>
		</fieldset>

		<fieldset>

			<% if (sk.iway.iwcm.system.captcha.Captcha.isRequired("dmail")) { %>
			<p class="captcha">
				<% if ("internal".equals(sk.iway.iwcm.Constants.getString("captchaType"))) { %><label for="wjcaptcha1"><iwcm:text key="checkform.captchaLabelText"/>:</label><% } %>
					<jsp:include page="/components/form/captcha.jsp"/>
			</p>
			<% } %>

			<p>
				<label>
					<input type="hidden" name="save" value="true" />
					<%=sk.iway.iwcm.system.stripes.CSRF.getCsrfTokenInputFiled(request.getSession())%>
					<input type="submit" name="bSubmit" value="<iwcm:text key="dmail.subscribe.submit"/>" />
				</label>
			</p>
		</fieldset>
	</form>
</div>