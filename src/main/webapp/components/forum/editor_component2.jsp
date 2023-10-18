<%@page import="java.util.List"%><%@page import="sk.iway.iwcm.forum.ForumSortBy"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,java.io.*,sk.iway.iwcm.editor.*,java.util.*"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%
request.setAttribute("cmpName", "forum");
%>
<jsp:include page="/components/top.jsp"/>

<div class="tab-pane toggle_content tab-pane-fullheight" style="width:790px;">
	<div class="tab-page" id="tabMenu1" style="display: block; width: 790px;">

<%
PageParams pageParams = new PageParams(request);

String sample = "Skupina1\n" +
					" podskupina1\n" +
					" podskupina2\n" +
					"Skupina2\n" +
					" podskupina1\n" +
					" podskupina2\n" +
					" podskupina3";
String structure = request.getParameter("structure");
GroupsDB groupsDB = GroupsDB.getInstance();
DocDB docDB = DocDB.getInstance();
List groups = groupsDB.getGroups(Tools.getIntValue(session.getAttribute("iwcm_group_id").toString(),-1));
if(groups.size()>0) sample="";
for(int i=0;i<groups.size();i++) {
		GroupDetails gd = (GroupDetails)groups.get(i);
		sample += gd.getGroupName()+"\n";
		List docs = docDB.getDocByGroup(gd.getGroupId());
		for(int j=0;j<docs.size();j++) {
			DocDetails dd = (DocDetails)docs.get(j);
			sample += " " + dd.getTitle()+"\n";
		}
}
GroupDetails baseGroupDetails = groupsDB.getGroup(Tools.getIntValue(session.getAttribute("iwcm_group_id").toString(),-1));
int pageSize = Tools.getIntValue(request.getParameter("pageSize"),10);
int pageLinksNum = Tools.getIntValue(request.getParameter("pageLinksNum"),10);
String sortAscending = request.getParameter("sortAscending") != null ? request.getParameter("sortAscending") : "true";
boolean useDelTimeLimit = false;
int delMinutes = 0;
ForumSortBy sortTopicsBy = ForumSortBy.valueOf(Tools.getStringValue(request.getParameter("sortTopicsBy"), ForumSortBy.LastPost.getColumnName()));
if(request.getParameter("useDelTimeLimit")!=null) {
	useDelTimeLimit = true;
	delMinutes = Tools.getIntValue(request.getParameter("delMinutes"),0);
}

if (baseGroupDetails == null)
{
   out.println("CHYBA: zakladny adresar neexistuje");
   return;
}

String zakladnaCesta = baseGroupDetails.getFullPath();

String baseData = "";
if(structure != null)
{
   out.flush();

	sample=structure;
	request.setAttribute("forumCreated","true");
	//najprv vytvori hlavne forum v hlavnej skupine
	GroupDetails actualGroup = baseGroupDetails;
	baseData = "<section><div class=\"container\"><div class=\"row\"><div class=\"col-md-12\"><div class=\"column-content\">!INCLUDE(/components/forum/forum_mb.jsp, type=topics, pageSize="+pageSize+", pageLinksNum="+pageLinksNum+", useDelTimeLimit="+useDelTimeLimit+", delMinutes="+delMinutes+", showSearchBox=true"+ ", sortTopicsBy="+sortTopicsBy+", sortAscending="+sortAscending+",rootGroup=false)!</div></div></div></div></section>";

	//toto vlozime na konci priamo ako kod do stranky
	//if(docDB.getDocByGroup(actualGroup.getGroupId()).size() == 0)//ak uz neexistuje root stranka tak ju vytvor
	//	ulozStrankuSekcie(request, out, actualGroup, actualGroup.getGroupName(),baseData+",rootGroup=true)!");

	BufferedReader br = new BufferedReader(new CharArrayReader(structure.toCharArray()));
	String line;

	while((line=br.readLine())!=null) {
		if(line.startsWith(" ")==false) {
			actualGroup = groupsDB.getGroupByPath(zakladnaCesta+"/"+line);
			System.out.println("actualGroup="+actualGroup+" cesta="+zakladnaCesta+"/"+line);
			if(actualGroup == null) {
				actualGroup = groupsDB.getCreateGroup(zakladnaCesta+"/"+line);
				System.out.println("actualGroup vytvoreny="+actualGroup+" cesta="+zakladnaCesta+"/"+line);
				//hlavna stranka je placeholder
				if (Constants.getBoolean("syncGroupAndWebpageTitle")) ulozStrankuSekcie(request, out, actualGroup, line, baseData, false);
			}
		} else {
			boolean pageExists = false;
			List docs = docDB.getDocByGroup(actualGroup.getGroupId());
			for(int i=0;i<docs.size();i++) {
				if(((DocDetails)docs.get(i)).getTitle().equals(line.substring(1))) {
					pageExists = true;
					break;
				}
			}
			if(!pageExists)
				ulozStrankuSekcie(request, out, actualGroup, line.substring(1),baseData, true);
		}
	}
	%>

	<script type="text/javascript">
	var htmlCode = '<%=baseData%>';
	htmlCode = htmlCode.replace("rootGroup=false)!", "rootGroup=true)!");
	window.parent.insertHtml(htmlCode);
  	try{ window.parent.parent.reloadWebpagesTree(); } catch (e) {}
	try{ window.parent.parent.parent.$('#SomStromcek').jstree(true).refresh(); } catch (e) {}
	function Ok()
	{
		try { window.parent.Cancel(); } catch (e) {}
		return false;
	}

	</script>
	<jsp:include page="/components/bottom.jsp"/>
	<%
	return;
}

%>

<script type="text/javascript">
function Ok()
{
	document.textForm.submit();
}
</script>


<form name="textForm" action="editor_component2.jsp">
<logic:present parameter="useDelTimeLimit" >
<input type="hidden" name="useDelTimeLimit" value="true">
<input type="hidden" name="delMinutes" value="<%=Tools.getIntValue(request.getParameter("delMinutes"),10)%>">
</logic:present>

<input type="hidden" name="pageSize" value="<%=Tools.getIntValue(request.getParameter("pageSize"),10)%>">
<input type="hidden" name="pageLinksNum" value="<%=Tools.getIntValue(request.getParameter("pageLinksNum"),10)%>">
<input type="hidden" name="sortAscending" value="<%=sortAscending%>"/>
<input type="hidden" name="sortTopicsBy" value="<%=sortTopicsBy.name()%>"/>

<table border="0" cellspacing="0" cellpadding="5">
	<label><iwcm:text key="components.forum_editor.writeGroups"/>:</label><br>
	<textarea name="structure" rows="20" cols="60"><%=sample%></textarea>

</table>
</form>
</div>
</div>

<jsp:include page="/components/bottom.jsp"/>

<%!
public void ulozStrankuSekcie(HttpServletRequest request, JspWriter out, GroupDetails group, String title, String data, boolean available) throws Exception
{


   Identity user = (Identity)request.getSession().getAttribute(Constants.USER_KEY);

   EditorForm editorForm = EditorDB.getEditorForm(request, -1, -1, group.getGroupId());
   editorForm.setAuthorId(user.getUserId());
   editorForm.setTempId(group.getTempId());
   editorForm.setPublish("1");
   editorForm.setAvailable(available);
   editorForm.setTitle(title);
   editorForm.setNavbar(title);
   editorForm.setData(data);


   out.println("<br>Ukladam stranku: " + title);
   out.flush();
   int historyId = EditorDB.saveEditorForm(editorForm, request);
   if (historyId > 0)
   {
   	out.println(" [OK]");
   }
   else
   {
   	out.println(" [CHYBA]"+request.getAttribute("err_message"));
   }
}
%>