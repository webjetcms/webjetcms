<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="sk.iway.iwcm.doc.GroupDetails"%>
<%@page import="sk.iway.iwcm.doc.GroupPublisher"%>
<%@page import="sk.iway.iwcm.doc.GroupsDB"%>
<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%@page import="sk.iway.iwcm.editor.EditorForm"%>
<%@page import="sk.iway.iwcm.doc.DocDB"%>
<%@page import="java.sql.ResultSet"%>
<%@page import="java.sql.PreparedStatement"%>
<%@page import="java.sql.Connection"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.Map"%>
<%@page import="sk.iway.iwcm.editor.EditorDB"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%@page import="java.nio.charset.Charset"%>
<%@page import="org.apache.commons.io.IOUtils"%>
<%@page import="sk.iway.iwcm.io.IwcmInputStream"%>
<%@page import="java.io.StringWriter"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.text.DateFormat"%>
<%@page import="java.util.Comparator"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="sk.iway.iwcm.io.IwcmFile"%>
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
<iwcm:checkLogon admin="true" perms="cmp_sync"/>
<%!
Map<Integer, Integer> getOldIds(String table, List<Integer> historyIds)
{
	Map<Integer, Integer> result = new HashMap<Integer, Integer>();

	if(historyIds == null || historyIds.size() == 0)
		return result;

	Connection db_conn = null;
	PreparedStatement ps = null;
	ResultSet rs = null;
	try
	{
		String sql = "";
		if("docs".equals(table))
			sql = "SELECT MAX(history_id) AS result FROM documents_history WHERE history_id < ? AND doc_id = (SELECT doc_id FROM documents_history WHERE history_id = ?)";
		else if("groups".equals(table))
			sql = "SELECT MAX(schedule_id) AS result FROM groups_scheduler WHERE schedule_id < ? AND group_id = (SELECT group_id FROM groups_scheduler WHERE schedule_id = ?)";
		else
			return result;

		db_conn = DBPool.getConnection();
		ps = db_conn.prepareStatement(sql);
		for(Integer i : historyIds)
		{
			ps.setInt(1, i);
			ps.setInt(2, i);
			rs = ps.executeQuery();
			if (rs.next())
			{
				result.put(i, rs.getInt("result"));
			}
		}
		rs.close();
		ps.close();
		db_conn.close();
		rs = null;
		ps = null;
		db_conn = null;
	}
	catch (Exception ex)
	{
		sk.iway.iwcm.Logger.error(ex);
	}
	finally
	{
		try
		{
			if (rs != null)
				rs.close();
			if (ps != null)
				ps.close();
			if (db_conn != null)
				db_conn.close();
		}
		catch (Exception ex2)
		{
		}
	}

	return result;
}
int getIdFromHistory(String table, int historyId)
{
	int result = -1;

	Connection db_conn = null;
	PreparedStatement ps = null;
	ResultSet rs = null;
	try
	{
		String sql = "";
		if("docs".equals(table))
			sql = "SELECT doc_id FROM documents_history WHERE history_id = ?";
		else if("groups".equals(table))
			sql = "SELECT group_id FROM groups_scheduler WHERE schedule_id = ?";
		else
			return result;

		db_conn = DBPool.getConnection();
		ps = db_conn.prepareStatement(sql);
		ps.setInt(1, historyId);
		rs = ps.executeQuery();
		if (rs.next())
		{
			result =  rs.getInt(1);
		}
		rs.close();
		ps.close();
		db_conn.close();
		rs = null;
		ps = null;
		db_conn = null;
	}
	catch (Exception ex)
	{
		sk.iway.iwcm.Logger.error(ex);
	}
	finally
	{
		try
		{
			if (rs != null)
				rs.close();
			if (ps != null)
				ps.close();
			if (db_conn != null)
				db_conn.close();
		}
		catch (Exception ex2)
		{
		}
	}

	return result;
}
%>

<%
Prop prop = Prop.getInstance(sk.iway.iwcm.Constants.getServletContext(), request);
request.setAttribute("dialogTitle", prop.getText("components.import_web_pages.rollback.dialogTitle"));
request.setAttribute("dialogDesc", prop.getText("components.rollback_web_pages.dialogDesc"));
%>
<jsp:include page="/admin/layout_top_dialog.jsp" />
<div class="padding10">

<script type="text/javascript">
	function Ok()
	{
		if (typeof document.rollbackFilesForm !== 'undefined')
			document.rollbackFilesForm.rollbackSubmit.click();
		else
			cancelWindow();
	}
</script>

<%
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);
Identity user = UsersDB.getCurrentUser(request);

if(Tools.isNotEmpty(Tools.getRequestParameter(request, "rollbackSubmit")))
{
	String path = Tools.getParameter(request, "rollback");
	if(Tools.isEmpty(path))
	{
		out.print(prop.getText("components.rollback_web_pages.noSelected"));
		return;
	}

	IwcmFile json = IwcmFile.fromVirtualPath(path);
	if(!json.exists())
	{
		out.print(prop.getText("components.rollback_web_pages.noExist"));
		return;
	}

	List<Integer> historyDocsIds = new ArrayList<Integer>();
	List<Integer> historyGroupsIds = new ArrayList<Integer>();

	StringWriter writer = new StringWriter();
	IwcmInputStream in = new IwcmInputStream(json);
	IOUtils.copy(in, writer, Charset.forName("UTF-8"));
	if(in!=null)
		in.close();
	json.delete();

	JSONArray jsonDocs = new JSONObject(writer.toString()).getJSONArray("docs");
	JSONArray jsonGroups = new JSONObject(writer.toString()).getJSONArray("groups");

	for(int i=0; i<jsonDocs.length(); i++)
		historyDocsIds.add(Tools.getIntValue(jsonDocs.getJSONObject(i).get("historyId").toString(), -1));
	for(int i=0; i<jsonGroups.length(); i++)
		historyGroupsIds.add(Tools.getIntValue(jsonGroups.getJSONObject(i).get("historyGroupId").toString(), -1));

	//k history_id po importe namapuje posledne hisotry_id pred importom
	Map<Integer, Integer> rollbackToDocs = getOldIds("docs", historyDocsIds);
	Map<Integer, Integer> rollbackToGroups = getOldIds("groups", historyGroupsIds);

	//rollback adresarov
	for(Integer i : historyGroupsIds)
	{
		Integer oldId = rollbackToGroups.get(i);
		int groupId = getIdFromHistory("groups", i);
		if(oldId!=null && oldId>0)
		{
			GroupDetails group = GroupPublisher.getGroupFromGroupsHistory(groupId);
			if(GroupsDB.getInstance().save(group))
				out.print("[DIR] "+prop.getText("components.rollback_web_pages.successful")+": " + group.getGroupName() + "<br>");
			else
				out.print("<font color=\"red\">[DIR] "+prop.getText("components.rollback_web_pages.successful")+":</font> " + group.getGroupName() + "<br>");
		}
		else
		{
			if(GroupsDB.deleteGroup(groupId, request))
				out.print("[DIR] "+prop.getText("components.rollback_web_pages.delSuccessful")+": " + groupId + "<br>");
			else
				out.print("<font color=\"red\">[DIR] "+prop.getText("components.rollback_web_pages.delUnsuccessful")+":</font> " + groupId + "<br>");
		}
	}

	if(historyGroupsIds.size()>0)
		out.print("<br>");

	//rollback stranok
	for(Integer i : historyDocsIds)
	{
		Integer oldId = rollbackToDocs.get(i);
		if(oldId!=null && oldId>0)
		{
			EditorForm ef = EditorDB.getEditorForm(request, -1, oldId, -1);
			ef.setPublish("1");
			ef.setAuthorId(user.getUserId());
			if(EditorDB.saveEditorForm(ef, request)>0)
				out.print("[DOC] "+prop.getText("components.rollback_web_pages.successful")+": " + ef.getTitle() + "<br>");
			else
				out.print("<font color=\"red\">[DOC] "+prop.getText("components.rollback_web_pages.successful")+":</font> " + ef.getTitle() + "<br>");
		}
		else
		{
			int docId = getIdFromHistory("docs", i);
			if(DocDB.deleteDoc(docId, request))
				out.print("[DOC] "+prop.getText("components.rollback_web_pages.delSuccessful")+": " + docId + "<br>");
			else
				out.print("<font color=\"red\">[DOC] "+prop.getText("components.rollback_web_pages.delUnsuccessful")+":</font> " + docId + "<br>");
		}
	}

	out.print("<br>"+prop.getText("components.rollback_web_pages.finished"));
}
else
{
	IwcmFile rollbackDir = IwcmFile.fromVirtualPath("/files/protected/backup/");
	IwcmFile[] rollbackVersionsArray = null;
	//ziskanie vsetkych suborov v rollback adresari
	if(rollbackDir.exists())
		rollbackVersionsArray = rollbackDir.listFiles();

	List<IwcmFile> rollbackVersions = new ArrayList<IwcmFile>();

	if(rollbackVersionsArray!=null)
	{
		for(int i=0; i<rollbackVersionsArray.length; i++)
		{
			if(rollbackVersionsArray[i].isFile() && Tools.getRequestParameter(request, "localGroupId").equals(rollbackVersionsArray[i].getName().substring(0, rollbackVersionsArray[i].getName().indexOf('_'))) )
				rollbackVersions.add(rollbackVersionsArray[i]);
		}
	}

	if(rollbackVersions != null && rollbackVersions.size()>0)
	{
		//zosortovanie listu podla datumu
		final DateFormat f = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
		Collections.sort(rollbackVersions, new Comparator<IwcmFile>()
		{
			@Override
			public int compare(IwcmFile o1, IwcmFile o2)
			{
			    try
			    {
			   	 	//nazov suboru v tvare <groupId>_<dd-MM-yyyy_HH-mm-ss>.json
			    	return f.parse(o1.getName().substring(o1.getName().indexOf('_')+1).replace(".json", "")).compareTo(f.parse(o2.getName().substring(o2.getName().indexOf('_')+1).replace(".json", "")));
			    }
			    catch (Exception e)
			    {
			   		sk.iway.iwcm.Logger.error(e);
			   		return -1;
			    }
			}
	  	});%>

		<form action="/components/sync/rollback_documents.jsp" name="rollbackFilesForm">
			<%for(int i=0; i<rollbackVersions.size(); i++)
			{
				String name = rollbackVersions.get(i).getName();
				try{name = Tools.formatDateTime(f.parse(name.substring(name.indexOf('_')+1).replace(".json", "")));}
				catch(Exception e){}
			%>
				<input type="radio" name="rollback" <%if(i!=rollbackVersions.size()-1) out.print("disabled=\"disabled\"");%> value="<%=Tools.escapeHtml(rollbackVersions.get(i).getVirtualPath())%>"><%=Tools.escapeHtml(name)%><br>
			<%}%>
			<input style="display: none;" type="submit" name="rollbackSubmit" class="button50" value="Rollback" />
		</form>
	<%}
	else
		out.print(prop.getText("components.rollback_web_pages.emptyDir") + " /files/protected/backup/");
}
%>

</div>
<%@ include file="/admin/layout_bottom_dialog.jsp" %>
