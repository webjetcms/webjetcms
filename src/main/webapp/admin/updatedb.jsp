<%@page import="java.util.List"%><%@page import="sk.iway.iwcm.*"%>
<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="java.sql.*,java.util.*" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/displaytag.tld" prefix="display" %>
<iwcm:checkLogon admin="true" perms="users.edit_admins"/>
<%@ include file="layout_top_popup.jsp" %>

<%
Identity user = UsersDB.getCurrentUser(request);
if ("cloud".equals(Constants.getInstallName())) return;
if (InitServlet.isTypeCloud() && user.getEmail().indexOf("@interway.sk")==-1) return;

String query = Tools.getRequestParameterUnsafe(request, "query");
Collection<LinkedHashMap<String,String>> rows = new ArrayList<LinkedHashMap<String,String>>(100);
String columnNames[] = null;
boolean result = false;

int rowsPerPage = Tools.getIntValue(Tools.getRequestParameter(request, "rowsPerPage"), 100);

String datasource = "iwcm";
if (session!=null&&session.getAttribute("updatedb.datasource")!=null) datasource = (String)session.getAttribute("updatedb.datasource");
if (Tools.isNotEmpty(Tools.getRequestParameter(request, "datasource")))
{
    datasource = Tools.getRequestParameter(request, "datasource");
    session.setAttribute("updatedb.datasource", datasource);
}

if (query!=null && query.trim().length()>0)
{

   Adminlog.add(Adminlog.TYPE_UPDATEDB,"updatedb.jsp: query: " + query,-1,-1);
	query = query.trim();
   Connection spojeni = DBPool.getConnection(datasource);

   DB dbUtil = new DB();

   try
   {
      out.println("<br>-----------------------<br>");
      out.println(Tools.escapeHtml(query));
      out.println("<br>-----------------------<br><br>");

      long start = Tools.getNow();

		String[] pole = query.split(";");
		Statement s;
		int countRecords = 0;
		for (String sql:pole)
		{
			if (Tools.isNotEmpty(sql))
			{
				if (Constants.DB_TYPE == Constants.DB_ORACLE)
				{
					sql = Tools.replace(sql, "|", ";");
					 if (sql.indexOf("TRIGGER")!=-1)
					   {
					   	sql = sql.replace('\n', ' ');
					   	sql = sql.replace('\r', ' ');
					   	sql = sql.replace('\t', ' ');
					   }
				}
			   out.println("Executing: "+Tools.escapeHtml(sql)+"<br>");

	      	s = spojeni.createStatement();
	      	if (sql.toLowerCase().indexOf("select")!=-1 || sql.toLowerCase().indexOf("execute")!=-1 || sql.toLowerCase().indexOf("show")!=-1)
	      	{
		      	ResultSet rs = s.executeQuery(sql);

		      	int i;
		      	int size = 0;
		      	String data;
		      	LinkedHashMap col = null;
		      	rows.clear();
		      	while (rs.next())
		      	{
		      		col = new LinkedHashMap<String,String>();
		      		countRecords++;
		      		if (columnNames == null)
		      		{
			      		//ziskaj nazvy stlpcov
			      		ResultSetMetaData rsmd = rs.getMetaData();
			      		size = rsmd.getColumnCount();
			      		columnNames = new String[size];

			      		for (i=0; i<size; i++)
			      		{
			      			columnNames[i] = rsmd.getColumnName(i+1);
			      		}
		      		}
		      		for (i=0; i<size; i++)
		      		{
		      			data = rs.getString(i+1);
		      			data = Tools.replace(data, "<", "&lt;");
		      			data = Tools.replace(data, ">", "&gt;");
		      			col.put(columnNames[i],data);
		      		}
		      		result = rows.add(col);
		      	}
		      	rs.close();
	      	}
	      	else
	      	{
	      		s.execute(sql);
	      	}
	      	s.close();
      	}
      }

		long end = Tools.getNow();

		out.println("<br/>Total time: "+(end-start)+" ms<br/>");

   }
   catch (Exception ex)
   {
      out.println("<br><br><font color='red'>"+ex.getMessage()+"</font><br><br>");
      sk.iway.iwcm.Logger.error(ex);
   }

	if (spojeni == null) {
		out.print(String.format("<br><br><font color='red'>Spojenie pre datasource %s je null</font><br><br>", datasource));
	}
	else {
		try {
			spojeni.close();
		} catch (SQLException e) {
			sk.iway.iwcm.Logger.error(e);
		}
	}
   request.setAttribute( "rows", rows);
   if(columnNames != null){
   %>

   <display:table class="sort_table" name="rows" uid="row" id="_row" export="true" pagesize="<%=rowsPerPage %>" >
	<%
	LinkedHashMap<String,String> trow = (LinkedHashMap<String,String>)_row;

	for(String colName : columnNames){
	%>
	<display:column title="<%=colName%>" escapeXml="true" sortable="true"><%=trow.get(colName)%></display:column>
	<%} %>
 		<display:setProperty name="export.excel.filename" value="results.xls" />
		<display:setProperty name="export.csv.filename" value="results.csv" />
		<display:setProperty name="export.xml.filename" value="results.xml" />
		<display:setProperty name="export.pdf.filename" value="results.pdf" />
	</display:table>

   <%
   }
   else
   {
   	 out.println("<br><b>Query executed.</b><br><br>");
   }
}
if (query==null) query="";
%>
<%=Tools.insertJQuery(request) %>
<hr>
Query: <%=session!=null&&session.getAttribute("updatedb.datasource")!=null?"   Datasource name: "+datasource:""%><br>
<form action="updatedb.jsp" method="post">
<textarea name="query" cols="80" rows="10" id="query"><%=query%></textarea>
<br>
Datasource: <input type="text" name="datasource" value="<%=datasource%>"/>

<%

/// BEGIN  historia sql prikazov z adminlogu MBO
int userId = Tools.getUserId(request);
Calendar cal = GregorianCalendar.getInstance();
cal.add(Calendar.DATE, -30);
List<AdminlogBean> lastUsed = Adminlog.searchAdminlog(new int[]{Adminlog.TYPE_UPDATEDB} , -1, -1, userId, cal.getTimeInMillis(), 0, "", -1, -1, "", "");
int counter = 0;
if (lastUsed!=null && lastUsed.size()>0)
{
	List<String> uniqueCommands = new ArrayList<>();
	for (AdminlogBean bean : lastUsed)
	{
		if (counter++>100)
			break;
		String command = bean.getDescription();
		if (command.indexOf("updatedb.jsp: query: ")!=-1 && command.indexOf("URI:")!=-1)
		{
			command = (command.substring(("updatedb.jsp: query: ").length(), command.indexOf("URI:"))).trim();
			if (!uniqueCommands.contains(command))
				uniqueCommands.add(command);
		}
		else continue;
	}
	out.print("<select name=\"history\" class=\"lastCommands\" style=\"width:582px;\" >");
	out.print("<option value=\"\" >Last used commands</option>");
	for (String command : uniqueCommands)
	{
		out.print("<option value=\""+command+"\" >"+(command.length()>120?command.substring(0, 119):command)+"</option>");
	}
	out.print("</select>");
}
else
{
	%><p>No last used SQL commands!</p><%
}
/// END historia sql prikazov z adminlogu MBO
%>
<br>
Rows per page: <input type="text" name="rowsPerPage" value="<%=rowsPerPage %>"/>
<br/>
<input id="querySubmit" type="submit">
</form>
<script type="text/javascript">
<!--
$(document).ready(function(){
	$(".lastCommands").change(function(){
		var html = $(this).val();
		$("#query").html(html);
	});
});

$(document).keydown(function(event) {
	 if(event.keyCode == '13' && event.ctrlKey) {
		$("#querySubmit").click();
	 return false;
	 }
});
//-->
</script>


<%@ include file="layout_bottom_popup.jsp" %>
