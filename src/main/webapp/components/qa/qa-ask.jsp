<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*"%>

<%@page import="sk.iway.iwcm.doc.DocDB"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%
	PageParams pageParams = new PageParams(request);
	String style =  pageParams.getValue("style", "01");
	String groupName = pageParams.getValue("groupName", "default");
	String toEmail = pageParams.getValue("toEmail", "webform@"+Tools.getServerName(request));
	String forward = pageParams.getValue("forward", DocDB.getInstance().getDocLink(Tools.getIntValue(Tools.getRequestParameter(request, "docid"), -1), request));

	//required obsahuje zoznam povinnych poli
	String required = ","+pageParams.getValue("required","name+email").replace('+', ',')+",";

	//show obsahuje zoznam poli, ktore sa maju zobrazit
	String show = ","+pageParams.getValue("show","name+email").replace('+', ',')+",";

	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);

	Prop prop = Prop.getInstance(request);
%>
<%!
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
%>
<%@page import="sk.iway.iwcm.tags.support.ResponseUtils"%><iwcm:present parameter="qasend">
	<%
	String qasend = Tools.getRequestParameter(request, "qasend");
	if (Tools.isNotEmpty(qasend))
	{
		//nebezpecne znaky odpaz
		if (Tools.isNotEmpty(Constants.getString("searchActionOmitCharacters")))
		{
			qasend = qasend.replaceAll("["+Constants.getString("searchActionOmitCharacters")+"]", " ");
		}
		//inputText = Tools.replace(inputText, "\"", "");
		//inputText = Tools.replace(inputText, "'", "");
		qasend = ResponseUtils.filter(qasend);
	}
	String key = "components.qa.send."+qasend;
	String text = prop.getText(key);
	//toto je doplnene ako XSS ochrana, aby nemohli zadat nieco co nemame v properties subore
	if (key.equals(text)==false)
	{
	%>
	<script type="text/javascript">
			window.alert('<%=text%>');
	</script>
	<% } %>
</iwcm:present>


<script type="text/javascript" src="/components/form/check_form.js"></script>


<% if("01".equals(style)){ %>
<%
if (!Constants.getBoolean("disableWysiwyg"))
{
%>
<%--
<%@include file="/components/_common/cleditor/jquery.cleditor.js.jsp" %>
<script type="text/javascript">
	var textareaId = 'question1';

	function loadClEditorIfReady()
	{
		$("#" + textareaId).cleditor({
			width      : 505,
			controls   : "bold italic underline bullets numbering outdent indent image link icon size color highlight pastetext",
			bodyStyle  : "font: 11px  Arial, Helvetica, sans-serif;"
		});
	}
	$(document).ready(function() {
		window.setTimeout(loadClEditorIfReady, 800);
	});
</script>
--%>
<% } %>
<form action="/qa.add.do" id="qaForm" method="post">
<fieldset>

	<div class="form-group" <%=isShow(show, "name")%>>
		<label for="fromName1" class="control-label">
			<iwcm:text key="components.qa.ask.your_name"/>:
		</label>
		<input type="text" id="fromName1" name="fromName" size="25" maxlength="255" class="form-control <%=isReq(required, "name", "")%>" />
	</div>

	<div class="form-group" <%=isShow(show, "company")%>>
		<label for="fromCompany1" class="control-label">
			<iwcm:text key="components.qa.ask.your_company"/>:
		</label>
		<input  type="text" id="fromCompany1" name="fromCompany" size="25" maxlength="255" class="form-control <%=isReq(required, "company", "")%>" />
	</div>

	<div class="form-group" <%=isShow(show, "phone")%>>
		<label for="fromPhone1" class="control-label">
			<iwcm:text key="components.qa.ask.your_phone"/>:
		</label>
		<input  type="text" id="fromPhone1" name="fromPhone" size="25" maxlength="255" class="form-control <%=isReq(required, "phone", "")%>" />
	</div>

	<div class="form-group" <%=isShow(show, "email")%>>
		<label for="fromEmail1" class="control-label">
			<iwcm:text key="components.qa.ask.your_email"/>:
		</label>
		<input type="text" id="fromEmail1" name="fromEmail" size="25" maxlength="255" class="form-control <%=isReq(required, "email", "email")%>" />
	</div>

	<div>
		<label for="question1" class="control-label">
			<iwcm:text key="components.qa.ask.question"/>:
		</label>
		<textarea id="question1" name="question" rows="5" cols="60" class="form-control required"></textarea>
	</div>

	<div class="checkbox">
		<label for="allowPublishOnWeb1" class="control-label"><input type="checkbox" id="allowPublishOnWeb1" name="allowPublishOnWeb" value="yes" />
			<iwcm:text key="components.qa.ask.allow_publish_on_web"/>
		</label>

	</div>

	<% if (sk.iway.iwcm.system.captcha.Captcha.isRequired("qa", request)) { %>
			<div>
				<% if ("internal".equals(sk.iway.iwcm.Constants.getString("captchaType"))) { %><label for="wjcaptcha1"><iwcm:text key="checkform.captchaLabelText"/>:</label><% } %>
					<jsp:include page="/components/form/captcha.jsp"/>
			</div>
	<% } %>

	<div>

		<input type="submit" class="btn btn-primary" name="b_submit" value="<iwcm:text key="components.qa.ask.send"/>" />
		<input type="hidden" name="toEmail" value="<%=toEmail%>" />
		<input type="hidden" name="groupName" value="<%=groupName%>" />
		<input type="hidden" name="forward" value="<%=forward%>" />
		<!-- kategoriu je mozne mat ako dodatocny select pre vyber podskupiny -->
		<input type="hidden" name="categoryName" value="" />
		<input type="hidden" name="lng" value="<%=lng%>" />
	</div>
</fieldset>
</form>
<%} else if("02".equals(style)){ %>
<form class="form-horizontal" action="/qa.add.do" id="qaForm" method="post">
	<fieldset>


		<div class="form-group" <%=isShow(show, "name")%>>
			<div class="col-md-4">
				<input type="text" id="fromName1" name="fromName" class="form-control input-md <%=isReq(required, "name", "")%>" />
			</div>
		</div>

		<div class="form-group" <%=isShow(show, "company")%>>
			<label class="col-md-4 control-label" for="fromCompany1">
				<iwcm:text key="components.qa.ask.your_company"/>
			</label>
			<div class="col-md-4">
				<input type="text" id="fromCompany1" name="fromCompany" class="form-control input-md <%=isReq(required, "company", "")%>" />
			</div>
		</div>

		<div class="form-group" <%=isShow(show, "phone")%>>
			<label class="control-label" for="fromPhone1">
				<iwcm:text key="components.qa.ask.your_phone"/>
			</label>
			<div class="col-md-4">
				<input type="text" id="fromPhone1" name="fromPhone" class="form-control input-md <%=isReq(required, "phone", "")%>" />
			</div>
		</div>

		<div class="form-group" <%=isShow(show, "email")%>>
			<label class="control-label" for="fromEmail1">
				<iwcm:text key="components.qa.ask.your_email"/>
			</label>
			<div class="col-md-4">
				<input type="text" id="fromEmail1" name="fromEmail" class="form-control input-md <%=isReq(required, "email", "email")%>" />
			</div>
		</div>

		<div class="form-group">
			<label class="control-label" for="question1">
				<iwcm:text key="components.qa.ask.question"/>
			</label>
			<div class="col-md-4">
				<textarea id="question1" name="question" rows="5" class="form-control required"></textarea>
			</div>
		</div>

		<div class="form-group">
			<label class="col-md-4 control-label" for="allowPublishOnWeb1">
				<iwcm:text key="components.qa.ask.allow_publish_on_web"/>
			</label>
			<div class="col-md-4">
				<label class="control-label" for="allowPublishOnWeb1">
					<input type="checkbox" id="allowPublishOnWeb1" name="allowPublishOnWeb" class="form-control" value="yes" />
				</label>
			</div>
		</div>

		<div class="form-group">
			<label class="col-md-4 control-label" for="signup"></label>
			<div class="col-md-4">
				<input type="submit" class="btn btn-success" name="b_submit" value="<iwcm:text key="components.qa.ask.send"/>" />
				<input type="hidden" name="toEmail" value="<%=toEmail%>" />
				<input type="hidden" name="groupName" value="<%=groupName%>" />
				<input type="hidden" name="forward" value="<%=forward%>" />
				<!-- kategoriu je mozne mat ako dodatocny select pre vyber podskupiny -->
				<input type="hidden" name="categoryName" value="" />
				<input type="hidden" name="lng" value="<%=lng%>" />
			</div>
		</div>
	</fieldset>
</form>
<%}%>
