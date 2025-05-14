<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
session.setMaxInactiveInterval(60*60*2);
%><%@ page pageEncoding="utf-8"  import="java.util.*,sk.iway.iwcm.*,sk.iway.iwcm.forum.*,sk.iway.iwcm.components.forum.jpa.*,java.net.URLDecoder" %>
<%@ page import="sk.iway.iwcm.i18n.Prop" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%!
	public boolean isAdmin(Identity user,int forumId) {
		String ids = ForumDB.getForum(forumId).getAdminGroups();
		if(ids != null ) {
			StringTokenizer st = new StringTokenizer(ids,"+,;");
			while(st.hasMoreTokens())
			{
				if(user.isInUserGroup(Tools.getIntValue(st.nextToken(), -1))) return true;
			}
		}
		return false;
	}
%>
<%
if (session.getAttribute("forum-shown")==null) {
	response.setStatus(HttpServletResponse.SC_NOT_FOUND);
	pageContext.include("/404.jsp");
	return;
}


if (Constants.getBoolean("editorEnableXHTML")) pageContext.setAttribute(sk.iway.iwcm.tags.support_logic.CustomTagUtils.XHTML_KEY, "true", PageContext.PAGE_SCOPE);

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

/* nastavenie zapisovania FLAGu do DB, ak sa allowFlagSetting nastavi na TRUE,
 * bude sa pri pridavani prispevku kontrolovat, ci user patri do pozadovanej skupiny;
 * ak patri, do premennej FLAG v DB sa zapise text v retazci flagStr
 */
boolean allowFlagSetting = false;
int userGroup = -1;
String flagStr = "sticky";

Identity user = (Identity) session.getAttribute(Constants.USER_KEY);
int forumId = Tools.getIntValue(request.getParameter("forumid"), -1);
int docId = Tools.getIntValue(request.getParameter("docid"), -1);
int parentId = Tools.getIntValue(request.getParameter("parent"), 0);

String onfocus = "";

try
{
	if (user != null && allowFlagSetting)
	{
		String[] groups = Tools.getTokens(user.getUserGroupsIds(), ",");
		if(groups!= null)
		{
			for(int i=0; i<groups.length; i++)
			{
				if(userGroup == Tools.getIntValue(groups[i], -1))
					request.setAttribute("setFlag", "true");
			}
		}
	}

	DocForumEntity forumForm = new DocForumEntity();
	forumForm.setParentId(parentId);
	forumForm.setDocId(docId);

	if (forumId>0)
	{
		if (user!=null && user.isAdmin())
		{
			DocForumEntity forumBean = ForumDB.getForumBean(request, forumId);
			forumForm.setAuthorName(forumBean.getAuthorName());
			forumForm.setAuthorEmail(forumBean.getAuthorEmail());
			forumForm.setSubject(forumBean.getSubject());
			forumForm.setQuestion(forumBean.getQuestion());
			forumForm.setId((long) forumId);
			forumForm.setSendAnswerNotif(forumBean.isSendNotif());
		}
	}
	else
	{
		if (parentId > 0)
		{
			DocForumEntity forumBean = ForumDB.getForumBean(request, parentId);
			Prop prop = Prop.getInstance(Constants.getServletContext(), request);
			if (forumBean != null && forumBean.getSubject().startsWith("Re:") == false && forumBean.getParentId() != -1)
			{
				forumForm.setSubject("Re: " + forumBean.getSubject());
			}
			else if (forumBean != null)
			{
				forumForm.setSubject(forumBean.getSubject());

			}
			if (forumBean != null && "true".equals(request.getParameter("isCite")))
			{
				if (Constants.getBoolean("disableWysiwyg"))
				{
					forumForm.setQuestion("\n\n---\n"+forumBean.getAuthorName()+" "+prop.getText("components.forum.bb.write")+":\n"+ SearchAction.htmlToPlain(forumBean.getQuestion()));
				}
				else
				{
			   	forumForm.setQuestion("<p>&nbsp;</p><blockquote><p class='forumQuoteUser'>"+forumBean.getAuthorName()+" "+prop.getText("components.forum.bb.write")+":</p><div class='forumQuote'>"+forumBean.getQuestion()+"</div></blockquote><p>&nbsp;</p>");
				}
			}
		}

		//nastav autora a email
		Cookie cookies[] = request.getCookies();
		if(cookies != null)
		{
			int len = cookies.length;
			int i;
			for (i=0; i<len; i++)
			{
				if ("forumname".equals(cookies[i].getName()))
				{
					forumForm.setAuthorName(Tools.URLDecode(cookies[i].getValue()));
				}
				if ("forumemail".equals(cookies[i].getName()))
				{
					forumForm.setAuthorEmail(Tools.URLDecode(cookies[i].getValue()));
				}
			}
		}
		if (user != null)
		{
			forumForm.setAuthorName(user.getFullName());
			forumForm.setAuthorEmail(user.getEmail());
			//aby sa nedal menit login
			onfocus="blur();";
		}
	}
	request.setAttribute("forumForm", forumForm);
}
catch(Exception ex)
{
	sk.iway.iwcm.Logger.error(ex);
}
request.setAttribute("cmpName", "forum");
request.setAttribute("titleKey", "forum.new.title");
request.setAttribute("descKey", "components.forum.new.insert_new_post");

ForumGroupEntity fgb = ForumDB.getForum(docId, true);
if (fgb == null && Tools.isNotEmpty(Constants.getString("forumDefaultAddmessageGroups")))
{
	fgb = new ForumGroupEntity();
	fgb.setAddmessageGroups(Constants.getString("forumDefaultAddmessageGroups"));
}
if (fgb != null && fgb.canPostMessage(user)==false)
{
	//nema dostatocne prava (kontrola user groups)
	%>
	<iwcm:text key="components.forum.wrong_user_groups_for_post"/>
	<%
	return;
}
%>

<%@page import="sk.iway.iwcm.doc.SearchAction"%>

	<%@include file="/components/_common/cleditor/jquery.cleditor.js.jsp" %>

	<script type="text/javascript" src="/components/form/check_form.js"></script>

	<div class="forum">
		<form:form method="post" modelAttribute="forumForm" action="/apps/forum/saveforum" name="forumForm">

			<div class="form-group">
				<form:label path="authorName"><iwcm:text key="forum.new.name"/>:</form:label>
				<form:input id="authorName" path="authorName" cssClass="required form-control" size="40" maxlength="255" required="required" disabled="true"/>
			</div>

			<div class="form-group">
				<form:label path="authorEmail"><iwcm:text key="forum.new.email"/>:</form:label>
				<form:input id="authorEmail" path="authorEmail" cssClass="email form-control" size="40" maxlength="255" disabled="true"/>
			</div>

			<%if(user!=null && isAdmin(user,docId) && parentId <= 0) {%>
			<div class="form-group">
				<label><iwcm:text key="components.forum.flag"/>:</label>
				<select name="flag" class="form-control">
					<option value=""></option>
					<option value="O"><iwcm:text key="components.forum.announcement"/></option>
					<option value="D"><iwcm:text key="components.forum.sticky"/></option>
				</select>
			</div>
			<%}%>

			<div class="form-group">
				<form:label path="subject"><iwcm:text key="forum.new.subject"/>:</form:label>
				<form:input path="subject" cssClass="required form-control" size="40" maxlength="255"/>
			</div>

                <div class="checkbox">
				<label>
				    <form:checkbox path="sendAnswerNotif"/> <iwcm:text key="components.forum.send_answer_notif"/>
                </label>
			</div>
			<div class="form-group">
				<form:textarea path="question" cssClass="input required wysiwyg form-control" id="wysiwygForum" rows="15" cols="35" />
			</div>

            <div class="form-group">

                <form:hidden path="parentId"/>
                <input type="hidden" name="docid" value="<%=sk.iway.iwcm.tags.support_logic.ResponseUtils.filter(request.getParameter("docid"))%>" />
                <input type="hidden" name="docId" value="<%=sk.iway.iwcm.tags.support_logic.ResponseUtils.filter(request.getParameter("docid"))%>" />
                <input type="hidden" name="parent2" value="<%=sk.iway.iwcm.tags.support_logic.ResponseUtils.filter(request.getParameter("parent2"))%>" />
                <input type="hidden" name="pageNum" value="<%=Tools.getIntValue(request.getParameter("pageNum"), 1)%>" />
                <input type="hidden" name="pageParams" value="<%=sk.iway.iwcm.tags.support_logic.ResponseUtils.filter(Tools.getRequestParameterUnsafe(request, "pageParams"))%>" />
            <%
            if(Tools.isNotEmpty(request.getParameter("type")))
            {
            %>
                <input type="hidden" name="type" value="<%=sk.iway.iwcm.tags.support_logic.ResponseUtils.filter(request.getParameter("type")) %>" />
            <%
            }
            %>
         </div>
		<%
		if(Tools.isNotEmpty(request.getParameter("rootForumId")))
		{
		%>
			<input type="hidden" name="rootForumId" value="<%=sk.iway.iwcm.tags.support_logic.ResponseUtils.filter(request.getParameter("rootForumId")) %>" />
		<%
		}
		%>
		<form:hidden path="id"/>

		<iwcm:present name="setFlag">
			<input type="hidden" name="flag" value="<%=flagStr%>" />
		</iwcm:present>
		<div style="display: none;">
			<input type="submit" id="bSubmit" name="submit" value="<iwcm:text key="forum.new.send"/>"/>
			<input type="button" onclick="javascript:window.close();" name="cancel" value="<iwcm:text key="forum.new.cancel"/>" />
		</div>
	</form:form>
	</div>

	<script type="text/javascript">

		var textareaId = 'wysiwygForum';

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
		<% if (request.getAttribute("isAdmin")==null) { %>
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
		<% } %>

		//Let user SET name and email only in case that this values are empty
		//!! -> this situation arise when NON logged user is adding message's to forum
		var authorName = document.getElementById('authorName');
		var authorEmail = document.getElementById('authorEmail');

		if (!authorName.value) { 
			authorName.removeAttribute('disabled');
		} else {
			authorName.setAttribute('disabled', '');
		}	

		if (!authorEmail.value) { 
			authorEmail.removeAttribute('disabled');
		} else {
			authorEmail.setAttribute('disabled', '');
		}	
	</script>
