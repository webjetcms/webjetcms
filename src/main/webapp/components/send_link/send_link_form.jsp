<%@page import="sk.iway.iwcm.components.proxy.ProxyDB"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.i18n.*,java.net.*,sk.iway.iwcm.doc.*,java.util.*,java.sql.*,sk.iway.iwcm.system.*,sk.iway.Password"%><%@page import="org.apache.commons.codec.binary.Base64"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%
//Formular na odoslanie stranky/linky

String xssRedirectUrl = ShowDoc.getXssRedirectUrl(request);
if (xssRedirectUrl != null)
{
	response.sendRedirect(Tools.addParameterToUrlNoAmp(xssRedirectUrl, "docid", ""+Tools.getDocId(request)));
	return;
}

PageParams pageParams = new PageParams(request);
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);
Prop prop = Prop.getInstance(Constants.getServletContext(), lng, false);
String sendType = null;
String message = "";

String csrfToken = Password.generatePassword(8);

DocDetails docDet;
String pageLink = request.getParameter("pageLink");
if (request.getParameter("docid") != null && request.getParameter("sendType") != null)
{
	int pageDocId = Tools.getIntValue(request.getParameter("docid"), -1);
	if (pageDocId < 1) return;

	sendType = request.getParameter("sendType");
	if (Constants.getBoolean("disableWysiwyg")) sendType = "link";

	if ( pageDocId > 0 && ("link".equals(sendType) || "page".equals(sendType)) )
	{
		DocDB docDB = DocDB.getInstance();
		docDet = docDB.getDoc(pageDocId);

		if (docDet == null)
		{
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			pageContext.include("/404.jsp");
			return;
		}

		//ochrana pred zobrazenim cohokolvek (kontrola prav, inak by sa ktokolvek dostal ku ktoremukolvek clanku)
		Identity user = UsersDB.getCurrentUser(request);
		if (DocDB.canAccess(docDet, user, true)==false)
		{
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
		if (docDet.isAvailable()==false)
		{
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			pageContext.include("/404.jsp");
			return;
		}
		if (Tools.isNotEmpty(docDet.getVirtualPath()) && docDet.getVirtualPath().startsWith("/system"))
		{
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			pageContext.include("/404.jsp");
			return;
		}

		//aby aspon ako tak fungovali INCLUDE komponenty
		request.setAttribute("docDetails", docDet);
		pageLink = Tools.getServerName(request)+docDet.getDocLink();
		if (pageLink.startsWith("/")){

			pageLink.replaceFirst("/", "");
			}

		pageLink = "http://" + pageLink;

		String qs = request.getParameter("qs");
		if (Tools.isNotEmpty(qs))
		{
			qs = Tools.replace(qs, "|", "=");
			Base64 b64 = new Base64();
			String decoded = new String(b64.decode(qs.getBytes()));

			pageLink = Tools.addParametersToUrlNoAmp(pageLink, decoded);
		}

		if ("link".equals(sendType))
		{
			message += "<p>"+prop.getText("components.send_link.visit_page") +  "<br/><br/>" +
						  "<b>" + docDet.getTitle() +	"</b><br/>" + docDet.getPerex() +
						  "\n</p>\n<p><a href=\""+pageLink+"\">"+pageLink+"</a>\n</p>";
		}
		else if ("page".equals(sendType))
		{
			String data = docDet.getData();
			//asi je nadpis v sablone, pridame
			if (data.toLowerCase().indexOf("<h1")==-1) data = "<h1>"+docDet.getTitle()+"</h1>"+data;

			//odstran JavaScript kod

			data = SearchAction.removeCommands(data);
			data = Tools.replace(data, "onmouse", "atribute1");

			message += "<p>" + prop.getText("components.send_link.page_text") + "</p>\n<p>" +
						"<a href=\"" + pageLink + "\">" + pageLink + "</a>\n</p><br/><hr/>" + data;
		}

		if (Constants.getBoolean("disableWysiwyg"))
		{
			message = SearchAction.htmlToPlain(message);
		}

		request.setAttribute("docData", message);

		session.setAttribute("sendLinkToken-"+csrfToken, "1");
	}
}

if (Tools.isEmpty(pageLink))
{
	return;
}

//nastav hodnoty a nacitaj ich z requestu
String adMessage = prop.getText("components.send_link.default.admessage") + " http://"+Tools.getServerName(request);

String fromName = "";
String fromEmail = "";
String toEmail = "";
String sendDate = "";
String sendTime = "";
String subject = adMessage;


//nastav autora a email podla cookies
Cookie cookies[] = request.getCookies();
if (cookies != null){
	int len = cookies.length;
	int i;
	for (i=0; i<len; i++)
	{
		if ("forumname".equals(cookies[i].getName()))
		{
			fromName = sk.iway.iwcm.Tools.URLDecode(cookies[i].getValue());
		}
		if ("forumemail".equals(cookies[i].getName()))
		{
			//out.println(cookies[i].getValue());
			fromEmail = sk.iway.iwcm.Tools.URLDecode(cookies[i].getValue());
		}
	}
}
if (Tools.isNotEmpty(request.getParameter("fromName"))) fromName = request.getParameter("fromName");
if (Tools.isNotEmpty(request.getParameter("fromEmail"))) fromEmail = request.getParameter("fromEmail");
if (Tools.isNotEmpty(request.getParameter("toEmail"))) toEmail = request.getParameter("toEmail");
if (Tools.isNotEmpty(request.getParameter("subject"))) subject = request.getParameter("subject");
if (Tools.isNotEmpty(request.getParameter("message"))) message = request.getParameter("message");
if (Tools.isNotEmpty(request.getParameter("sendDate"))) sendDate = request.getParameter("sendDate");
if (Tools.isNotEmpty(request.getParameter("sendTime"))) sendTime = request.getParameter("sendTime");

if (Constants.getBoolean("disableWysiwyg"))
{
	message = Tools.replace(message, "<", "&lt;");
	message = Tools.replace(message, ">", "&gt;");
	message = Tools.replace(message, "\n", "<br />");
}

if (Constants.getBoolean("formMailSendPlainText"))
{
	message = SearchAction.htmlToPlain(message);
}

if (Tools.isEmpty(sendDate))
{
	sendDate = Tools.formatDate(new java.util.Date());
}
if (Tools.isEmpty(sendTime))
{
	sendTime = Tools.formatTime(new java.util.Date());
}

if ("sendPage".equals(request.getParameter("act")) && "post".equalsIgnoreCase(request.getMethod()))
{
	//System.out.println(message);

	//uloz meno a email do cookies
	Cookie forumName = new Cookie("forumname", sk.iway.iwcm.Tools.URLEncode(fromName));
	forumName.setPath("/");
	forumName.setMaxAge(60 * 24 * 3600);
	forumName.setHttpOnly(true);
	Cookie forumEmail = new Cookie("forumemail", sk.iway.iwcm.Tools.URLEncode(fromEmail));
	forumEmail.setPath("/");
	forumEmail.setMaxAge(60 * 24 * 3600);
	forumEmail.setHttpOnly(true);

	Tools.addCookie(forumName, response, request);
	Tools.addCookie(forumEmail, response, request);

	//odosli pohladnicu
	String serverRoot = "http://"+Tools.getServerName(request);
	if (sk.iway.iwcm.Constants.getInt("httpServerPort")!=80)
	{
		serverRoot += ":"+sk.iway.iwcm.Constants.getInt("httpServerPort");
	}
	//ochrana proti odosielaniu SPAMu

	//text musi obsahovat linku na nasu domenu
	boolean antispamOK = true;

	String antispamServerName = Constants.getString("antispamServerName");
	if (Tools.isNotEmpty(antispamServerName))
	{
		StringTokenizer st = new StringTokenizer(antispamServerName, " ,;");
		boolean containsDomain = false;
		while (st.hasMoreTokens())
		{
			if (message.indexOf(st.nextToken().trim())!=-1)
			{
				containsDomain = true;
				break;
			}
		}
		if (!containsDomain)
		{
			antispamOK = false;
		}
	}
	else if (message.indexOf(Tools.getServerName(request))==-1)
	{
		antispamOK = false;
	}

	String disclaimer = Constants.getString("sendLinkDisclaimer-"+lng);
	if (disclaimer != null)
	{
		message += disclaimer;
	}
	else
	{
		//		na koniec pridaj reklamny text
	   if (message.indexOf(adMessage)==-1)
	   {
	   	message = message + "<br/><br/><hr/>" + adMessage;
	   }
	}

	if (!SpamProtection.canPost("sendLink",message,request)) antispamOK = false;
	if (request.getCookies()==null) antispamOK = false;

	if (antispamOK)
	{
		String key = "sendLinkToken-"+request.getParameter("token");
	   if (session.getAttribute(key) == null) antispamOK = false;
	   else session.removeAttribute(key);
	}

	boolean captchaOK = Captcha.validateResponse(request, request.getParameter("wjcaptcha"), "send_link");

	boolean ok = false;
	if (antispamOK && captchaOK)
	{
	   ok = SendMail.sendLater(fromName, fromEmail, toEmail, null, null, null, subject, message, serverRoot, null, null);
	}
	if (ok)
	{
		request.setAttribute("pageSend", "ok");
	}
	else
	{
		if (captchaOK == false) request.setAttribute("pageSend", "captcha");
		else request.setAttribute("pageSend", "fail");
	}
}
else
{
	//nastav atribut, ze sa moze zobrazit formular
   request.setAttribute("showForm", "true");
}
%>

<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%@page import="sk.iway.iwcm.system.captcha.Captcha"%>

<%@page import="sk.iway.iwcm.tags.support.ResponseUtils"%>

 		<%@include file="/components/_common/cleditor/jquery.cleditor.js.jsp" %>
 		<script type="text/javascript" src="/components/form/check_form.js"></script>

		<%--
		// zakomentovany, kedze obsahuje document.write a to robi bordel pri nacitani cez AJAX
		<script type="text/javascript" src="/components/calendar/popcalendar.jsp"></script>
		--%>

		<div class="send_linka">
			<iwcm:equal name="pageSend" value="ok">
   				<iwcm:text key="components.send_link.send_ok"/> <%=ResponseUtils.filter(toEmail)%>.
			</iwcm:equal>

			<iwcm:equal name="pageSend" value="fail">
			   <iwcm:text key="components.send_link.send_fail"/>.
			</iwcm:equal>

			<iwcm:equal name="pageSend" value="captcha">
			   <iwcm:text key="captcha.textNotCorrect"/>
			</iwcm:equal>

			<iwcm:present name="showForm">

			   <form action="/components/send_link/send_link_form.jsp" method="post" id="sendLinkForm">
					<div class="container-fluid">
						<div class="row form-group">
							<div class="col-md-3">
								<label for="fromName1"><iwcm:text key="gallery.card.from_name"/>:</label>
							</div>
							<div class="col-md-9">
								<input type="text" id="fromName1" name="fromName" class="required form-control" size="40" maxlength="255" value="<%=ResponseUtils.filter(fromName)%>" />
							</div>
						</div>

						<div class="row form-group">
							<div class="col-md-3">
								<label for="fromEmail1"><iwcm:text key="gallery.card.from_email"/>:</label>
							</div>
							<div class="col-md-9">
								<input type="text" id="fromEmail1" name="fromEmail" class="required email form-control" size="40" maxlength="255" value="<%=ResponseUtils.filter(fromEmail)%>" />
							</div>
						</div>

						<div class="row form-group">
							<div class="col-md-3">
								<label for="toEmail1"><iwcm:text key="gallery.card.to_email"/>:</label>
							</div>
							<div class="col-md-9">
								<input type="text" id="toEmail1" name="toEmail" class="required email form-control" size="40" maxlength="255" />
							</div>
						</div>

						<div class="row form-group">
							<div class="col-md-3">
								<label for="subject1"><iwcm:text key="gallery.card.subject"/>:</label>
							</div>
							<div class="col-md-9">
								<input type="text" id="subject1" name="subject" class="required form-control" size="60" maxlength="255" value="<%=ResponseUtils.filter(subject)%>" />
							</div>
						</div>

						<% if (sk.iway.iwcm.system.captcha.Captcha.isRequired("send_link")) { %>
						<div class="row form-group">
							<div class="col-md-3">
								<label for="wjcaptcha1"><iwcm:text key="captcha.enterTextFromImage"/>:</label>
							</div>
							<div class="col-md-9 captcha">
								<%=sk.iway.iwcm.system.captcha.Captcha.getImage(request) %>
								<%if(!"invisible".equals(Constants.getString("captchaType")) && !"reCaptcha".equals(Constants.getString("captchaType"))){%>
									<br/>
									<input type="text" id="wjcaptcha1" name="wjcaptcha" class="required captcha form-control" size="20" maxlength="255" value="" />
								<%} %>
							</div>
						</div>
						<% } %>
					</div>

					<div<%= "link".equals(sendType) ? " class=\"wysiwygLink\"" : " class=\"wysiwygPage\""%>>
						<textarea name="message" id="wysiwygSendLink" class="wysiwyg" cols="50" rows="18"><iwcm:write name="docData"/></textarea>
					</div>
					<div style="display: none;">
						<input type="hidden" name="pageLink" value="<%=ResponseUtils.filter(pageLink)%>" />
						<input type="hidden" name="token" value="<%=csrfToken%>" />
						<input type="hidden" name="act" value="sendPage" />
						<input type="submit" id="bSubmit" name="bSubmit" value="<iwcm:text key="gallery.card.send"/>" class="submit" />
						<input type="reset" class="reset" name="bReset" onclick="window.close();" value="<iwcm:text key="button.cancel"/>" />
					</div>
				</form>
			</iwcm:present>
		</div>

		<script type="text/javascript">

			var textareaId = 'wysiwygSendLink';

			function loadClEditorIfReady()
			{
				//window.alert(textareaId+" equals2="+("wysiwygSendLink"==textareaId));
				$("#" + textareaId).cleditor({
					width      : 560,
					controls   : "bold italic underline bullets numbering outdent indent image link icon size color highlight pastetext",
					bodyStyle  : "font: 11px  Arial, Helvetica, sans-serif;"
				});
			}
			$(document).ready(function() {
				window.setTimeout(loadClEditorIfReady, 1000);
			});

			function reInitCheckForm()
			{
				try
				{
					checkForm.allreadyInitialized = false;
					checkForm.init();
				}
				catch (e) {}
			}
			window.setTimeout(reInitCheckForm, 5000);
		</script>


