<%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="org.apache.struts.util.ResponseUtils,sk.iway.iwcm.*,sk.iway.iwcm.common.DocTools,sk.iway.iwcm.doc.DocDB"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<iwcm:checkLogon admin="true" perms="replaceAll"/>
<%@ include file="layout_top.jsp" %>
<%!

public String getGroupIds(String groupIds)
{
	String searchGroups = null;
	StringTokenizer st = new StringTokenizer(groupIds, ",+; ");
	GroupsDB groupsDB = GroupsDB.getInstance();
	List searchGroupsArray;
	GroupDetails group;
	Iterator iter;
	int searchRootGroupId;
	while (st.hasMoreTokens())
	{
		try
		{
			searchRootGroupId = Integer.parseInt(st.nextToken());
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
			continue;
		}

		//najdi child grupy
		searchGroupsArray = groupsDB.getGroupsTree(searchRootGroupId, true, true);
		iter = searchGroupsArray.iterator();
		while (iter.hasNext())
		{
			group = (GroupDetails) iter.next();
			if (group != null)
			{
				if (searchGroups == null)
				{
					searchGroups = "" + group.getGroupId();
				}
				else
				{
					searchGroups += "," + group.getGroupId();
				}
			}
		}
	}
	if ("-1".equals(searchGroups)) searchGroups = null;
	return searchGroups;
}

public List<DocDetails> replaceTextDocuments(String oldText, String newText, String rootGroupIds, boolean replace)
{
	List<DocDetails> docs = new ArrayList<DocDetails>();
	try
	{
		//ziskaj udaje z db
		Connection db_conn = DBPool.getConnection();

		String sql = "SELECT doc_id, data, title, navbar, html_head, html_data, virtual_path, perex_image FROM documents WHERE (data LIKE ? OR title LIKE ? OR navbar LIKE ? OR virtual_path LIKE ? OR perex_image LIKE ?)";

		if (Tools.isNotEmpty(rootGroupIds))
		{
			String expandedGroups = getGroupIds(rootGroupIds);
			if (Tools.isNotEmpty(expandedGroups))
			{
				sql += " AND group_id IN ("+expandedGroups+")";
			}
		}

		PreparedStatement ps = db_conn.prepareStatement(sql);
		ps.setString(1, "%"+oldText+"%");
		ps.setString(2, "%"+oldText+"%");
		ps.setString(3, "%"+oldText+"%");
		ps.setString(4, "%"+ DocTools.removeCharsDir(oldText, true).toLowerCase()+"%");
		ps.setString(5, "%"+oldText+"%");

		ResultSet rs = ps.executeQuery();
		DocDetails doc;
		while (rs.next())
		{
			doc = new DocDetails();
			doc.setDocId(rs.getInt("doc_id"));
			doc.setTitle(DB.getDbString(rs, "title"));
			doc.setData(DB.getDbString(rs, "data"));
			doc.setNavbar(DB.getDbString(rs, "navbar"));
			doc.setHtmlHead(DB.getDbString(rs, "html_head"));
			doc.setHtmlData(DB.getDbString(rs, "html_data"));
			doc.setVirtualPath(DB.getDbString(rs, "virtual_path"));
			doc.setPerexImage(DB.getDbString(rs, "perex_image"));

			docs.add(doc);
		}
		rs.close();
		ps.close();

		//ok, mame zoznam, updatni to
		if (replace)
		{
			Iterator iter = docs.iterator();
			while (iter.hasNext())
			{
				doc = (DocDetails) iter.next();

				Logger.println(this,"updating data in document: "+doc.getTitle());

				doc.setData(DB.replace(doc.getData(), oldText, newText));

				int psCounter = 1;
				ps = db_conn.prepareStatement("UPDATE documents SET data=?, data_asc=?, title=?, navbar=?, html_head=?, html_data=?, virtual_path=?, perex_image=?, sync_status=1 WHERE doc_id=?");

				doc.setData(Tools.replace(doc.getData(), oldText, newText));

				DB.setClob(ps, psCounter++, doc.getData());
				DB.setClob(ps, psCounter++, DB.internationalToEnglish(doc.getData()).toLowerCase());
				ps.setString(psCounter++, Tools.replace(doc.getTitle(), oldText, newText));
				DB.setClob(ps, psCounter++, Tools.replace(doc.getNavbar(), oldText, newText));
				DB.setClob(ps, psCounter++, Tools.replace(doc.getHtmlHead(), oldText, newText));
				DB.setClob(ps, psCounter++, Tools.replace(doc.getHtmlData(), oldText, newText));

				String oldUrl = doc.getVirtualPath();
				String newUrl = Tools.replace(doc.getVirtualPath(), DocTools.removeCharsDir(oldText, true).toLowerCase(), DocTools.removeCharsDir(newText, true).toLowerCase());
				ps.setString(psCounter++, newUrl);
				ps.setString(psCounter++, Tools.replace(doc.getPerexImage() , oldText, newText));
				ps.setInt(psCounter++, doc.getDocId());
				ps.executeUpdate();
				ps.close();

				if (oldUrl.equals(newUrl)==false)
				{
					//pridaj presmerovanie
					UrlRedirectDB.addRedirect(oldUrl, newUrl, null, 301);
				}

				//DocDB.updateDataClob(db_conn, doc.getDocId(), -1, doc.getData(), DB.internationalToEnglish(doc.getData()).toLowerCase());
			}
		}
		db_conn.close();
	}
	catch (Exception ex)
	{
		sk.iway.iwcm.Logger.error(ex);
	}

	return(docs);
}

public List<GroupDetails> replaceTextGroups(String oldText, String newText, String rootGroupIds, boolean replace)
{
	List<GroupDetails> groups = new ArrayList<GroupDetails>();
	try
	{
		//ziskaj udaje z db
		Connection db_conn = DBPool.getConnection();

		String sql = "SELECT group_id, group_name, navbar FROM groups WHERE group_name LIKE ? OR navbar LIKE ?";

		if (Tools.isNotEmpty(rootGroupIds))
		{
			String expandedGroups = getGroupIds(rootGroupIds);
			if (Tools.isNotEmpty(expandedGroups))
			{
				sql += " AND group_id IN ("+expandedGroups+")";
			}
		}

		PreparedStatement ps = db_conn.prepareStatement(sql);
		ps.setString(1, "%"+oldText+"%");
		ps.setString(2, "%"+oldText+"%");
		ResultSet rs = ps.executeQuery();
		GroupDetails group;
		while (rs.next())
		{
			group = new GroupDetails();
			group.setGroupId(rs.getInt("group_id"));
			group.setGroupName(DB.getDbString(rs, "group_name"));
			group.setNavbar(DB.getDbString(rs, "navbar"));

			groups.add(group);
		}
		rs.close();
		ps.close();

		//ok, mame zoznam, updatni to
		if (replace)
		{
			Iterator<GroupDetails> iter = groups.iterator();
			while (iter.hasNext())
			{
				group = iter.next();

				Logger.println(this,"updating data in group: "+group.getGroupIdName());

				int psCounter = 1;
				ps = db_conn.prepareStatement("UPDATE groups SET group_name=?, navbar=? WHERE group_id=?");

				ps.setString(psCounter++, Tools.replace(group.getGroupName(), oldText, newText));
				ps.setString(psCounter++, Tools.replace(group.getNavbarName(), oldText, newText));

				ps.setInt(psCounter++, group.getGroupId());
				ps.executeUpdate();
				ps.close();

				//DocDB.updateDataClob(db_conn, doc.getDocId(), -1, doc.getData(), DB.internationalToEnglish(doc.getData()).toLowerCase());
			}
		}
		db_conn.close();
	}
	catch (Exception ex)
	{
		sk.iway.iwcm.Logger.error(ex);
	}

	return(groups);
}

%>

<%@page import="sk.iway.iwcm.doc.DocDetails"%>
<%@page import="sk.iway.iwcm.doc.GroupDetails"%>

<div id="waitDiv" style="text-align:center; color: red;">
   <iwcm:text key="fbrowser.edit.loading_please_wait"/><br/>
   <img src="/admin/images/loading-anim.gif" />
</div>

<div class="content-wrapper">

	<%
	Identity user = UsersDB.getCurrentUser(request);

	String oldText = Tools.getRequestParameterUnsafe(request, "oldText");
	String newText = Tools.getRequestParameterUnsafe(request, "newText");
	String rootGroupIds = Tools.getRequestParameter(request, "rootGroupIds");
	String email = Tools.getStringValue(Tools.getRequestParameter(request, "email"),"");
	boolean replace = Tools.getIntValue(Tools.getRequestParameter(request, "replaceOccurances"), 0)==1;


	boolean replacing = false;

	if (oldText == null)
	{
	oldText = "";
	newText = "";
	rootGroupIds = "";
	}
	else
	{
		replacing = true;

		out.flush();

	List<DocDetails> replacedPages = replaceTextDocuments(oldText, newText, rootGroupIds, replace);
	request.setAttribute("replacedPages", replacedPages);

	out.flush();

	DocDB.getInstance(true);

	out.flush();

	List<GroupDetails> replacedGroups = replaceTextGroups(oldText, newText, rootGroupIds, replace);
	request.setAttribute("replacedGroups", replacedGroups);

	out.flush();

	GroupsDB.getInstance(true);

	out.flush();
	}

	%>


	<%@page import="sk.iway.iwcm.doc.GroupsDB"%>
	<%@page import="sk.iway.iwcm.system.UrlRedirectDB"%>
	<%@ page import="sk.iway.iwcm.users.UsersDB" %>
	<%@ page import="java.sql.Connection" %>
	<%@ page import="java.sql.PreparedStatement" %>
	<%@ page import="java.sql.ResultSet" %>
	<%@ page import="java.util.ArrayList" %>
	<%@ page import="java.util.Iterator" %>
	<%@ page import="java.util.StringTokenizer" %>

	<p>
	Nahradia sa výskyty slova v tabuľke documents v stĺpcoch s HTML kódom, názvom, navigačnou lištou HTML kódom do hlavičky a v perexe.
	Tiež sa nahradia výskyty v tabuľke groups - názov adresára a navigačná lišta.
	</p>

	<form name="replaceForm" action="<%=PathFilter.getOrigPath(request) %>" method="post">
		ID adresárov (automaticky sa zahrnú aj podadresáre). Viacero ID adresárov oddeľte znakom , (napr 1,5).
		<br/>
		<input type="text" name="rootGroupIds" class="form-control" value="<%=rootGroupIds %>" size="80"/>
		<br/>

	Starý text:
	<br/>
	<textarea name='oldText' class="form-control" rows=8 cols=80><%=ResponseUtils.filter(oldText)%></textarea>
	<br>

	Nový text:
	<br/>
		<textarea name='newText' class="form-control" rows=8 cols=80><%=ResponseUtils.filter(newText)%></textarea>
		<br>

		Email kam sa pošlú výsledky (ak by nastal timeout stránky):
	<br/>
		<input type="text" class="form-control" name="email" value="<%=email %>" size="80"/>
		<br>

		<br/>
		<label><input type="radio" class="form-check-input" name="replaceOccurances" value="0" <%=!replace?"checked=\"checked\"":"" %> />Hľadať</label>
		<br/>
		<label><input type="radio" class="form-check-input" name="replaceOccurances" value="1" <%=replace?"checked=\"checked\"":"" %> />Nahradiť</label>
		<br>
		<br>

		<input type="submit" name="replace" class="btn btn-primary"  value="Hľadať/Nahradiť">
	</form>

	<% if (replacing) { %>
		<iwcm:buff>
			<h2>Výsledok nahradenia</h2>

			ID adresárov: <%=ResponseUtils.filter(rootGroupIds) %>
			<br/>
			Starý text: <%=ResponseUtils.filter(oldText) %>
			<br/>
			Nový text: <%=ResponseUtils.filter(newText) %>
			<br/>
			<br/>


			<iwcm:present name="replacedPages">
				<strong>Uravené stránky:</strong>
				<br>
				<table cellspacing=2 cellpadding=0>
					<tr>
					</tr>
					<tr>
					<th>DocID</th>
					<th><iwcm:text key="editor.title"/></th>
					<th>URL</th>
					</tr>
				<iwcm:iterate name="replacedPages" id="doc" type="sk.iway.iwcm.doc.DocDetails">
					<tr>
					<td><iwcm:strutsWrite name="doc" property="docId"/></td>
					<td><a href="/showdoc.do?docid=<iwcm:strutsWrite name="doc" property="docId"/>" target="_blank"><iwcm:strutsWrite name="doc" property="title"/></a></td>
					<td><iwcm:strutsWrite name="doc" property="virtualPath"/></td>
					</tr>
				</iwcm:iterate>
				</table>
				<br/>
			</iwcm:present>

			<iwcm:present name="replacedPages">
				<strong>Upravené adresáre:</strong>
				<br>
				<table cellspacing=2 cellpadding=0>
					<tr>
					</tr>
					<tr>
					<th>GroupID</th>
					<th><iwcm:text key="editor.title"/></th>
					</tr>
				<iwcm:iterate name="replacedGroups" id="group" type="sk.iway.iwcm.doc.GroupDetails">
					<tr>
					<td><iwcm:strutsWrite name="group" property="groupId"/></td>
					<td><iwcm:strutsWrite name="group" property="groupName"/></td>
					</tr>
				</iwcm:iterate>
				</table>
			</iwcm:present>
		</iwcm:buff>
	<%


		if (Tools.isNotEmpty(email))
		{
			String buffTagOutput = (String)pageContext.getAttribute("buffTagOutput");
			String subject = "";
			if (replace)
				subject="Nahradenie vyskytu "+oldText+" za "+newText;
			else
				subject="Vyhľadanie výskytu "+oldText;
			SendMail.send(email, email, email, subject, buffTagOutput);
		}
	}
	%>
</div>


<%@ include file="layout_bottom.jsp" %>
