<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*, sk.iway.iwcm.form.*"%>
<%
int userId = -1;
boolean result = false;
PageParams pageParams = new PageParams(request);
String formName = pageParams.getValue("formName", "");

Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
if (user != null)
{
	userId = user.getUserId();
	if (userId > 0 && Tools.isNotEmpty(formName))
	{
		result = FormDB.checkFormSendOnce(userId, formName);
	}
}

if (!result)
{
	out.println("<style> #formView { display: none } </style>");
}
%>