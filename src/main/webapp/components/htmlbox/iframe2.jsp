<%@page import="java.util.List"%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.doc.*, java.util.*" %>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<iwcm:checkLogon admin="true" perms="menuWebpages"/>
<html>
<body>
<div>
<%=Tools.insertJQuery(request)%>
<% 	int dirDocId = Tools.getIntValue(Tools.getRequestParameter(request, "dirDocId"), -1); %>

<%
	if(dirDocId==-1){
		%><p> ERROR: missing parameter docId!</p><%
	}else {
		GroupsDB groupsDB = GroupsDB.getInstance();
	   GroupDetails grpDir = groupsDB.getGroup(dirDocId);

	   DocDB docDB = DocDB.getInstance();
	   List<DocDetails> docList = null;
	   if(grpDir != null)
	   {
		   	docList = docDB.getDocByGroup(grpDir.getGroupId());
		   	if(docList.size() > 0){
		   		for(DocDetails file: docList){
		   			%>

		   				  <a class="thumbImage" data-name="<%=file.getTitle()%>" data-docid="<%=file.getDocId()%>">
		   				  <span><%=file.getTitle()%></span>
							<%if(!(file.getPerexImage().equals(""))){ %>
			            		<img src="<%=file.getPerexImage()%>"/>
			            	<%} %>
			            </a>

		   			<%
		   		}
		   	}
	   }
	}
%>
	<iframe id="WJTempPreview" name="WJTempPreview" align="top" style="display:none;" src="/components/empty.jsp"></iframe>

	<script>

		$(document).ready(function(){
			$("a.thumbImage").click(function(){
				$("a.thumbImage").removeClass('selected');

                $("#WJTempPreview").attr('src','iframe.jsp?docid='+$(this).attr('data-docid'));

				$(this).addClass('selected');
			});
		});
</script>
<style type="text/css">
		a.thumbImage { font-family: Arial, Verdana, Tahoma, sans-serif; border: 1px solid #ddd; font-size: 13px; color: black; display: block; margin: 10px; padding: 10px; text-decoration: none; cursor: pointer; }
		a.thumbImage:nth-child(even) { background-color: #e9e9e9; }
		a.thumbImage:hover { background-color: #b5e3b0; }
		a.thumbImage img { clear: both; border: 1px solid #eee; width:100%; height:auto; margin-top: 10px; }
		a.thumbImage.selected { background-color: #29c01a;color:#fff}
		div.filterOptions { width:100%; padding: 10px; margin-top:-30px;}
		div.filterOptions select{ width: 200px;}
</style>
</div>
</body>
</html>
