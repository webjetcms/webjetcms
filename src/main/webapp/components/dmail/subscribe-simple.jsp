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

	String email = request.getParameter("email");

	//skus najst prihlaseneho pouzivatela
	Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
	if (user!=null)
	{
		//if (email==null) email = user.getEmail();
	}

	if (email==null) email="";
	String dialogMessageKey = null;
	String errors = "";

	if ("true".equals(request.getParameter("save")))
	{
		//skontroluj, ci je vsetko zadane spravne
		if (email.indexOf("@")==-1)
		{
			errors+="<li>"+prop.getText("dmail.subscribe.enter_email")+".</li>";
		}
		//System.out.println("UGID: " + request.getParameterValues("user_group_id"));
		if (request.getParameterValues("user_group_id")==null)
		{
			errors+="<li>"+prop.getText("dmail.subscribe.select_groups")+".</li>";
		}

		if (sk.iway.iwcm.system.stripes.CSRF.verifyTokenAndDeleteIt(request)==false)
		{
			errors+="<li>"+prop.getText("components.csrfError")+".</li>";
		}

		if (Tools.isEmpty(errors))
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
				//System.out.println(emailBodyId);
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
			//System.out.println("status="+status);
			if (status > 0)
			{
				//ok
				dialogMessageKey = "dmail.subscribe.confirm_email_sent";
			}
			else if (status == -2)
			{
				//nepodarilo sa poslat email
				dialogMessageKey = "dmail.subscribe.email_send_error";
			}
			else if (status == -1)
			{
				//nepodarilo sa vytvorit usera
				dialogMessageKey = "dmail.subscribe.db_error";
			}
			else
			{
				//neznama chyba
				dialogMessageKey = "dmail.subscribe.unknown_error";
			}
		}
	}

	//prislo potvrdenie zmeny
	if (request.getParameter("hash")!=null)
	{
		String hash = request.getParameter("hash");
		//System.out.println("mam hash: " + hash);
		UserDetails authorizedUser = UsersDB.authorizeEmail(request, hash);
		if (authorizedUser == null)
		{
			//zadany hash neexistuje, alebo nastala ina chyba
			dialogMessageKey = "dmail.subscribe.error_authorize_email";
		}
		else
		{
			//podarilo sa nam ho nacitat
			email = authorizedUser.getEmail();
			dialogMessageKey = "dmail.subscribe.email_authorized";
		}
	}

	if (Tools.isNotEmpty(errors)) {
		dialogMessageKey = "<strong>"+prop.getText("dmail.subscribe.errors")+":</strong><ul style='color: red'>"+errors+"</ul>";
	}

	if (dialogMessageKey != null) {
		%>
		<div class="modal" tabindex="-1" id="subscribeSimpleModal">
			<div class="modal-dialog">
				<div class="modal-content">
				<div class="modal-header">
					<h5 class="modal-title"><iwcm:text key="components.dmail.modal.title"/></h5>
					<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<iwcm:text key='button.close'/>"></button>
				</div>
				<div class="modal-body">
					<iwcm:text key="<%=dialogMessageKey%>"/>
				</div>
				<div class="modal-footer">
					<button type="button" class="btn btn-secondary" data-bs-dismiss="modal"><iwcm:text key="button.confirm"/></button>
				</div>
				</div>
			</div>
		</div>
		<script type="text/javascript">
			$(document).ready(function() {
				try {
					var myModal = new bootstrap.Modal(document.getElementById('subscribeSimpleModal'), {
						keyboard: false,
						backdrop: true
					})
					myModal.show();
				} catch (e) {
					<%
					out.println("window.alert(\""+Tools.replace(JSEscapeTag.jsEscape(prop.getText(dialogMessageKey)), "<br>", "\\n")+".\");");
					%>
				}
			});
		</script>
		<%
	}
%>

<div class="subscribe-form-simple">
	<form id="subscribeForm" action="<%=PathFilter.getOrigPathDocId(request) %>#subscribeForm" method="post">
		<% boolean hasAnyGroup = false; %>
		<iwcm:iterate id="ugd" name="userGroupsList" type="sk.iway.iwcm.users.UserGroupDetails">
		<% if (ugd.isAllowUserEdit() && ugd.isRequireEmailVerification()) { %>
			<input type="hidden" name="user_group_id" value='<iwcm:strutsWrite name="ugd" property="userGroupId"/>' value='checked'/>
		<% hasAnyGroup = true;  } %>
		</iwcm:iterate>
		<% if (hasAnyGroup) { %>
			<input type="hidden" name="save" value="true" />
			<%=sk.iway.iwcm.system.stripes.CSRF.getCsrfTokenInputFiled(request.getSession())%>
			<div class="input-group mb-3">
				<input name="email" value="<%=ResponseUtils.filter(email)%>" class="form-control" type="text" placeholder="<iwcm:text key="dmail.subscribe.email"/>" aria-label="<iwcm:text key="dmail.subscribe.email"/>"/>
				<button class="btn btn-primary" type="submit"><iwcm:text key="dmail.subscribe.submit"/></button>
			</div>
		<% } %>
	</form>
</div>