<%@page import="java.util.List"%><%sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page language="java" import="java.util.*,java.sql.*,sk.iway.iwcm.forum.*,sk.iway.iwcm.*,java.text.*,sk.iway.iwcm.doc.*" pageEncoding="utf-8" %>

<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/displaytag.tld" prefix="display" %>
<iwcm:checkLogon admin="true" perms="cmp_diskusia"/>

<%@page import="sk.iway.iwcm.users.UserDetails"%>
<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%@ page import="sk.iway.iwcm.system.stripes.CSRF" %>

<jsp:include page="/admin/layout_top.jsp" />

<link rel="stylesheet" type="text/css" href="/admin/css/tablesort.css">

<%!

public static List<ForumBean> filterFormsByUser(UserDetails user, List<ForumBean> allForms)
{
	List<ForumBean> ret = new ArrayList<ForumBean>();

	GroupsDB groupsDB = GroupsDB.getInstance();
	DocDB docDB = DocDB.getInstance();

	int userEditableGroups[] = groupsDB.expandGroupIdsToChilds(Tools.getTokensInt(user.getEditableGroups(), ","), true);
	int userEditablePages[] = Tools.getTokensInt(user.getEditablePages(), ",");
	if ((userEditableGroups == null || userEditableGroups.length<1) && (userEditablePages==null || userEditablePages.length<1))
	{
		//dopln titulky
		for (ForumBean form : allForms)
		{
			DocDetails doc = docDB.getBasicDocDetails(form.getDocId(), false);
			if (doc != null)
			{
				form.setPrefix(doc.getTitle());
			}
		}
		return allForms;
	}

	for (ForumBean form : allForms)
	{
		boolean pridaj = false;
		if (userEditableGroups!=null && userEditableGroups.length>0)
		{
			DocDetails doc = docDB.getBasicDocDetails(form.getDocId(), false);
			if (doc != null)
			{
				for (int groupId : userEditableGroups)
				{
					if (doc.getGroupId()==groupId)
					{
						//sem si ukladame nazov web stranky
						form.setPrefix(doc.getTitle());
						pridaj = true;
						break;
					}
				}
			}
		}
		if (userEditablePages!=null && userEditablePages.length>0)
		{
			for (int docId : userEditablePages)
			{
				if (form.getDocId()==docId)
				{
					DocDetails doc = docDB.getBasicDocDetails(form.getDocId(), false);
					if (doc != null)
					{
						//sem si ukladame nazov web stranky
						form.setPrefix(doc.getTitle());
						pridaj = true;
						break;
					}
				}
			}
		}

		if (pridaj)
		{
			ret.add(form);
		}
	}

	return ret;
}

%>
<%
Identity user = UsersDB.getCurrentUser(request);

java.util.Date startDate = new java.util.Date(new java.util.Date().getTime() - (long)30*24*60*60*1000);
java.util.Date endDate = new java.util.Date();

if(Tools.isNotEmpty(request.getParameter("startDate")))
{
	startDate = new java.util.Date(DB.getTimestamp(request.getParameter("startDate")));
}
if(Tools.isNotEmpty(request.getParameter("endDate")))
{
	endDate = new java.util.Date(DB.getTimestamp(request.getParameter("endDate")));
}
String startDateString = Tools.formatDate(startDate);
String endDateString = Tools.formatDate(endDate);
if("".equals(request.getParameter("startDate")))
{
	startDateString = "";
	startDate = new java.util.Date(0);
}
if("".equals(request.getParameter("endDate")))
{
	endDateString = "";
	endDate = new java.util.Date(new java.util.Date().getTime()+(long)2*24*60*60*1000);
}

if("delete".equals(request.getParameter("act")) && request.getParameter("forumId")!= null && request.getParameter("forumDocId")!= null && CSRF.verifyTokenAndDeleteIt(request))
{
		ForumDB.deleteMessage(Tools.getIntValue(request.getParameter("forumId"),0),
						Tools.getIntValue(request.getParameter("forumDocId"),0),user);
}

if("recovery".equals(request.getParameter("act")) && request.getParameter("forumId")!= null && request.getParameter("forumDocId")!= null && CSRF.verifyTokenAndDeleteIt(request))
{
		ForumDB.recoverMessage(Tools.getIntValue(request.getParameter("forumId"),0),
						Tools.getIntValue(request.getParameter("forumDocId"),0),user);
}

String pathFilterPath = (String)request.getAttribute("path_filter_orig_path");
		PreparedStatement ps;
		ResultSet rs;
		ForumBean fb = null;
		List messages = new ArrayList();

		StringBuffer sql = new StringBuffer("SELECT * FROM document_forum WHERE doc_id > 0"+sk.iway.iwcm.common.CloudToolsForCore.getDomainIdSqlWhere(true)+" AND question_date >= ? AND question_date <= ?");
		int conditionsCounter = 0;

		if ( Tools.isNotEmpty(request.getParameter("article")) ){
			sql.append(" AND doc_id IN(SELECT doc_id FROM documents WHERE LOWER(title) LIKE ?)");
			conditionsCounter++;
		}
		if ( Tools.isNotEmpty(request.getParameter("author")) ){
			sql.append(" AND LOWER(author_name) LIKE ?");
			conditionsCounter++;
		}
		sql.append(" ORDER BY question_date DESC");

		DocDB docDB = DocDB.getInstance();
		try
		{
			java.sql.Connection db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql.toString());
			System.out.println(startDate);
			System.out.println(endDate);
			ps.setTimestamp(1,new Timestamp(DB.getTimestamp(startDateString,"0:00:00")));
			ps.setTimestamp(2,new Timestamp(DB.getTimestamp(endDateString,"23:59:59")));
			if ( Tools.isNotEmpty(request.getParameter("author")) )
				ps.setString(2+conditionsCounter--,"%"+request.getParameter("author").toLowerCase()+"%");

			if ( Tools.isNotEmpty(request.getParameter("article")) )
			   ps.setString(2+conditionsCounter--,"%"+request.getParameter("article").toLowerCase()+"%");

			rs = ps.executeQuery();
			SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.getString("dateFormat"));
			SimpleDateFormat timeFormat = new SimpleDateFormat(Constants.getString("timeFormat"));
			while (rs.next())
			{
				int docId = rs.getInt("doc_id");
				String url = docDB.getDocLink(docId);
				if (url!=null)
				{
					fb = new ForumBean();
					fb.setForumId(rs.getInt("forum_id"));
					fb.setDocId(docId);
					fb.setParentId(rs.getInt("parent_id"));
					fb.setSubject(DB.getDbString(rs, "subject"));
					fb.setQuestion(DB.getDbString(rs, "question"));
					fb.setQuestionDate(rs.getTimestamp("question_date"));
					fb.setQuestionDateDisplayDate(dateFormat.format(rs.getTimestamp("question_date")));
					fb.setQuestionDateDisplayTime(timeFormat.format(rs.getTimestamp("question_date")));
					fb.setAutorFullName(DB.getDbString(rs, "author_name"));
            	fb.setAutorEmail(DB.getDbString(rs, "author_email"));
            	fb.setConfirmed(rs.getBoolean("confirmed"));
            	fb.setHashCode(DB.getDbString(rs, "hash_code"));
            	fb.setSendNotif(rs.getBoolean("send_answer_notif"));
            	fb.setActive(rs.getBoolean("active"));
            	fb.setUserId(rs.getInt("user_id"));
            	fb.setFlag(DB.getDbString(rs, "flag"));
            	fb.setDeleted(rs.getBoolean("deleted"));
            	messages.add(fb);
				}
			}
			rs.close();
			ps.close();
			db_conn.close();
			messages = filterFormsByUser(user, messages);
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		request.setAttribute("messages",messages);
%>

<script type="text/javascript">
	helpLink = "components/forum.jsp&book=components";
</script>

<script type="text/javascript" src="/components/calendar/popcalendar.jsp"></script>
<script type="text/javascript" charset="windows-1250" src="/admin/scripts/dateTime.jsp"></script>

<div class="row title">
    <h1 class="page-title"><i class="fa icon-bubbles"></i><iwcm:text key="components.forum.title"/></h1>
</div>

<div class="tabbable tabbable-custom tabbable-full-width">
	<ul class="nav nav-tabs">
		<li class="active">
			<a href="#tabMenu1" data-toggle="tab">
				<iwcm:text key="components.filter"/>
			</a>
		</li>
	</ul>

	<div class="tab-content">
	    <div id="tabMenu1" class="tab-pane active">

		<form class="globalFilterForm" name="settings" action="admin_diskusia_zoznam.jsp">

			<input type="hidden" name="search" value="true" />

			<div class="col-lg-2 col-md-3 col-sm-6 form-group">
				<label class="form-label" for="startDateId"><iwcm:text key="components.forum.od" />:</label>
				<input type="text" class="form-control" size="10" maxlength="10" name="startDate" id="startDateId" value="<%=startDateString %>" onblur="checkDate(document.settings.startDate);return false" class="poleMini datepicker">
			</div>
			<div class="col-lg-2 col-md-3 col-sm-6 form-group">
				<label class="form-label" for="endDateId"><iwcm:text key="components.forum.do" />:</label>
				<input type="text" class="form-control" size="10" maxlength="10" id="endDateId" name="endDate" value="<%=endDateString %>" onblur="checkDate(document.settings.endDate);return false" class="poleMini datepicker">
			</div>
			<div class="col-lg-2 col-md-3 col-sm-6 form-group">
				<label class="form-label" for="articleId"><iwcm:text key="components.forum.article" />:</label>
				<input type="text" class="form-control" size="10" name="article" id="articleId" value="<%=request.getParameter("article")!=null?org.apache.struts.util.ResponseUtils.filter(request.getParameter("article")):""%>" class="poleMini">
			</div>
			<div class="col-lg-2 col-md-3 col-sm-6">
				<div class="form-group">
					<label class="form-label" for="authorId"><iwcm:text key="components.forum.author" />:</label>
					<input type="text" class="form-control" size="10" name="author" id="authorId" value="<%=request.getParameter("author")!=null?org.apache.struts.util.ResponseUtils.filter(request.getParameter("author")):""%>" class="poleMini">
				</div>
			</div>
			<div class="col-lg-1 col-md-1 col-sm-6">
				<div class="form-group">
					<label class="control-label display-block">&nbsp;</label>
					<input type="submit" class="btn green" value="<iwcm:text key="components.forum.zobrazit" />">
				</div>
			</div>

		</form>

		</div>
	</div>
</div>

<display:table name="messages" uid="message" excludedParams="docid" requestURI="<%=pathFilterPath%>" export="true" class="sort_table" cellspacing="0" cellpadding="1" pagesize="40">
	<%
	if(message != null)
	{
		fb = (ForumBean)message;
		%>
		<display:setProperty name="export.excel.filename" value="Prispevky.xls" />
		<display:setProperty name="export.csv.filename" value="Prispevky.csv" />
		<display:setProperty name="export.xml.filename" value="Prispevky.xml" />
		<display:setProperty name="export.pdf.filename" value="Prispevky.pdf" />
		<display:column titleKey="components.forum.article" class="sort_td" headerClass="sort_thead_td" sortable="true">
			<a href="<%=docDB.getDocLink(fb.getDocId(), request) %>" target="_blank" style="color: red;">
				<bean:write name="message" property="prefix"/>
			</a>
		</display:column>
		<display:column titleKey="components.forum.admin.nazov" class="sort_td" headerClass="sort_thead_td" sortable="true">
			<a href="<%=docDB.getDocLink(fb.getDocId(), request) %>" target="_blank">
				<bean:write name="message" property="subject" />
			</a>
		</display:column>
		<%
		String delStyle = "";
		if(fb.isDeleted())
			delStyle = "color: red;";
		%>
		<display:column titleKey="components.forum.admin.text" class="sort_td" headerClass="sort_thead_td" sortable="true" style="<%=delStyle%>">
			<%
				String text = SearchAction.htmlToPlain(fb.getQuestion());
				if(text != null && text.length()>60)
				{
					out.print(DB.prepareString(text, 60)+"...");
				}
				else
					out.print(text+"&nbsp;");
			%>
		</display:column>
		<display:column property="questionDate" titleKey="components.forum.admin.datum" class="sort_td" headerClass="sort_thead_td" sortable="true" decorator="sk.iway.displaytag.DateTimeDecorator"/>
		<display:column property="autorFullName" titleKey="components.forum.admin.autor" class="sort_td" headerClass="sort_thead_td" sortable="true" />
		<display:column property="autorEmail" titleKey="components.forum.admin.email" class="sort_td" headerClass="sort_thead_td" sortable="true" decorator="sk.iway.displaytag.NbspDecorator"/>
		<display:column titleKey="editor_dir.tools" class="sort_td" headerClass="sort_thead_td" sortable="false">
			<a href="javascript:editForum(<bean:write name="message" property="forumId" />, <%=fb.getDocId() %>)" title='<iwcm:text key="components.banner.edit"/>' class="iconEdit">&nbsp;</a>
			<a href="javascript:editForumSettings(<%=fb.getDocId() %>)" title='<iwcm:text key="user.admin.settings"/>' class="iconForum">&nbsp;</a>
			<%
			if(fb.isDeleted())
			{
			%>
				<a href="javascript:recoveryP(<bean:write name="message" property="forumId" />,<bean:write name="message" property="docId" />)" >
						<img src="/admin/images/filerecovery.gif" border="0" alt='<iwcm:text key="components.banner.recover"/>' title='<iwcm:text key="components.banner.recover"/>'>
				</a>
			<%
			}else{
			%>
				<a href="javascript:deleteP(<bean:write name="message" property="forumId" />,<bean:write name="message" property="docId" />)"  title='<iwcm:text key="components.banner.delete"/>' class="iconDelete">&nbsp;</a>
			<%
			}
			%>
		</display:column>
		<%
	}
	%>
</display:table>

<script type="text/javascript">

function recoveryP(id,docid)
{
 	document.recoveryForm.act.value="recovery";
 	document.recoveryForm.forumId.value=id;
 	document.recoveryForm.forumDocId.value=docid;
    document.recoveryForm.submit();
    document.recoveryForm.act.value="";
}

function deleteP(id,docid)
{
 if (window.confirm("Naozaj chcete zmazať príspevok ?"))
 {
 	document.deleteForm.act.value="delete";
 	document.deleteForm.forumId.value=id;
 	document.deleteForm.forumDocId.value=docid;
    document.deleteForm.submit();
    document.deleteForm.act.value="";
 }
}

function editForum(forumId, docId)
{
	var options = "toolbar=no,scrollbars=no,resizable=yes,width=350,height=350;"
	editwindow=window.open("/components/forum/new_admin.jsp?forumid="+forumId+"&parent="+forumId+"&docid="+docId+"&edit=true",'forumNew',options);
	if (!editwindow.opener) editwindow.opener = self;
	if (window.focus) {editwindow.focus()}
}

function editForumSettings(docId)
{
	var options = "toolbar=no,scrollbars=no,resizable=yes,width=600,height=530;"
	editwindow=window.open("/components/forum/admin_forum_open.jsp?docid="+docId,'forumSettings'+docId,options);
	if (!editwindow.opener) editwindow.opener = self;
	if (window.focus) {editwindow.focus()}
}
</script>

<form action="/components/forum/admin_diskusia_zoznam.jsp" name="deleteForm">
	<input type="hidden" name="startDate" value="<%=startDateString%>"/>
	<input type="hidden" name="endDate" value="<%=endDateString%>"/>
	<input type="hidden" name="act" />
	<input type="hidden" name="forumId" />
	<input type="hidden" name="forumDocId" />
	<input type="hidden" name="article" value="<%=request.getParameter("article")!=null?request.getParameter("article"):"" %>"/>
	<input type="hidden" name="author" value="<%=request.getParameter("author")!=null?org.apache.struts.util.ResponseUtils.filter(request.getParameter("author")):"" %>"/>
    <%=CSRF.getCsrfTokenInputFiled(session)%>
</form>

<form action="/components/forum/admin_diskusia_zoznam.jsp" name="recoveryForm">
	<input type="hidden" name="startDate" value="<%=startDateString%>"/>
	<input type="hidden" name="endDate" value="<%=endDateString%>"/>
	<input type="hidden" name="act" />
	<input type="hidden" name="forumId" />
	<input type="hidden" name="forumDocId" />
	<input type="hidden" name="article" value="<%=request.getParameter("article")!=null?request.getParameter("article"):"" %>"/>
	<input type="hidden" name="author" value="<%=request.getParameter("author")!=null?org.apache.struts.util.ResponseUtils.filter(request.getParameter("author")):"" %>"/>
    <%=CSRF.getCsrfTokenInputFiled(session)%>
</form>

<%@ include file="/admin/layout_bottom.jsp" %>