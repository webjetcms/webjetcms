<%@page import="sk.iway.iwcm.*"%>
<%@page import="sk.iway.iwcm.filebrowser.BrowseAction"%>
<%@page import="sk.iway.iwcm.io.IwcmFile"%><%@page import="sk.iway.iwcm.tags.support_logic.ResponseUtils"%>

<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%=Tools.insertJQuery(request)%>
<div>
<% String dirName = Tools.getStringValue(Tools.getRequestParameter(request, "dirName"), ""); %>


   	<div class="thumbSelector" style="overflow: auto;">
	   <% if(!dirName.equals("")){
	   				dirName="/"+dirName;
	   			}

		   //String path = "/components/"+Constants.getInstallName()+"/htmlbox/objects"+dirName;

		   IwcmFile dir = new IwcmFile(Tools.getRealPath("/components/"+Constants.getInstallName()+"/htmlbox/objects/"+dirName));
		   if (dir.exists()==false) dir = new IwcmFile(Tools.getRealPath("/components/htmlbox/objects/"+dirName));

	         //prescanuj adresar pre vyber objektu
	         //String rootDirPath = sk.iway.iwcm.Constants.getServletContext().getRealPath("/components/"+Constants.getInstallName()+"/htmlbox/objects"+dirName);
	         String prettyName;
			 String name;
	         String lng = PageLng.getUserLng(request);
		   if (dir.exists() && dir.canRead())
		   {
		      //IwcmFile dir = new IwcmFile(rootDirPath);
		      IwcmFile files[] = FileTools.sortFilesByName(dir.listFiles());

			  if(files != null) {
				int size = files.length;
				IwcmFile f;
				for (int i=0; i<size; i++)
				{
					f = files[i];
					if (f.isFile())
					{
						name = f.getName();
						prettyName = name;


						if (BrowseAction.hasForbiddenSymbol(prettyName))
							continue;

						if(prettyName.substring(prettyName.lastIndexOf('.')).equals(".html")) {
							try
							{
								prettyName = prettyName.substring(0, prettyName.lastIndexOf("."));
								name = name.substring(0, name.lastIndexOf("."));
								prettyName = Tools.escapeHtml(prettyName.replace('_', ' '));
							}
							catch (Exception ex){}

							//String htmlName = f.getName().substring(0, f.getName().lastIndexOf('.')) + ".html";

						%>
						<a class="thumbImage" data-name="<%=name+".html"%> ">
							<%=prettyName %><br/>
							<%
							String pathPlusName = Tools.escapeHtml(dir.getVirtualPath())+"/"+ name;
							IwcmFile imageJPG = new IwcmFile(PathFilter.getRealPath(pathPlusName+".jpg"));
							IwcmFile imagePNG = new IwcmFile(PathFilter.getRealPath(pathPlusName+".png"));
							if(imageJPG.exists()){
							%>
								<img src="<%=pathPlusName%>.jpg?w=500&h=300&ip=6"/>
							<% }else if(imagePNG.exists()){ %>
								<img src="<%=pathPlusName%>.png?w=500&h=300&ip=6"/>
							<% } %>
						</a>
						<%
						}
					}
				 }
		      }
		   }
        %>
     </div>
     <div valign="top" style="display: none;">
		<iframe id="previewWindow" name="previewWindow" align="top" style="display: none; width:1024px; height:391px; bgcolor: silver" src="/components/empty.jsp"></iframe>
     </div>
     	<style type="text/css">
		table.templateSelect { background-color: white; }
		a.thumbImage { font-family: Arial, Verdana, Tahoma, sans-serif; border: 1px solid #ddd; font-size: 13px; color: black; display: block; margin: 10px; padding: 10px; text-decoration: none; cursor: pointer; }
		a.thumbImage:nth-child(even) { background-color: #e9e9e9; }
		a.thumbImage:hover { background-color: #b5e3b0; }
		a.thumbImage img { clear: both; border: 1px solid #eee; max-width:100%; height:auto; margin-top: 10px; }
		a.thumbImage.selected { background-color: #29c01a;color:#fff}
		div.filterOptions { width:100%; padding: 10px; margin-top:-30px;}
		div.filterOptions select{ width: 200px;}
	</style>
	<script>
		$(document).ready(function(){
			$("a.thumbImage").click(function(){
				$("#previewWindow").attr("src", "<%=ResponseUtils.filter(dir.getVirtualPath())%>/"+ $(this).data("name"));

				$("a.thumbImage").removeClass('selected');
				$(this).addClass('selected');
			});
		});
	</script>
</div>
