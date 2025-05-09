<%@page import="java.util.List"%><%@page import="sk.iway.iwcm.tags.WriteTag"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="java.io.*, java.util.*, sk.iway.iwcm.*, sk.iway.iwcm.filebrowser.*" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%!

public String capitalize(String path)
{
	//capitalize
	path=path.substring(0,1).toUpperCase()+path.substring(1);
	//sanitize
	path=path.replace("-"," ").replace("_"," ");
	return " "+path+" ";
}
public String sanitize(String path)
{
	if ("/".equals(path)) return "";
	//remove slashes at begining of word
	char slash=path.toCharArray()[0];

	while (slash=='/')
	{
		path=path.substring(1);
		slash=path.toCharArray()[0];
	}

	String result="";
	for (String part:path.split("/"))
	{
		result+=capitalize(part)+"/";
	}
	if (result.length()>1)
	{
		result=result.substring(0,result.length()-1);
	}

	return result;
}
%>
<%
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);
PageParams pageParams = new PageParams(request);
String rootDir = pageParams.getValue("rootDir", "/files/");
int pageDocID = pageParams.getIntValue("pageDocID", Tools.getIntValue(request.getParameter("docid"), 4));
String target = pageParams.getValue("target", "_blank");
boolean showActualDir = pageParams.getBooleanValue("showActualDir", true);
boolean dirProtected = false;

List<FileDirBean> dirList = new ArrayList();
List fileList = new ArrayList();
BrowseAction.browseDir(request, dirList, fileList, rootDir);
	for (FileDirBean dir : dirList) {
	    if ("/components/_common/mime/folder_protected.gif".equals(dir.getIcon())) {
	        dir.setIcon("<i class=\"fas fa-lock\"></i>");
			dir.setDirProtected(true);
		} else {
			dir.setIcon("<i class=\"far fa-folder\"></i>");
			dir.setDirProtected(false);
		}
	}
request.setAttribute("dirList", dirList);
request.setAttribute("fileList", fileList);

%>
<script type="text/javascript">
    $(document).ready(function(){
		// prv skontrolujem ci je modal funkcia dostupna
        if (typeof Modal == 'function') {
			// odchytime click
			$("a.siteBrowserClick").click(function() {
				var link = $(this).attr("href");
				// ak je data-protected true a zaroven linka obsahuje permsDenied zavoláme prihlasovaciu stránku
				if($(this).data("protected") == true && link.indexOf("permsDenied") != -1){
					$.get($(this).attr("href"), function(data){
						$(".loginBox").html( data );
						$('.modal-login').modal('show');
					});
					return false;
				}
			});
        }else{
            $("a.siteBrowserClick").click(function() {
                var link = $(this).attr("href");
                // ak je data-protected true a zaroven linka obsahuje permsDenied zavoláme prihlasovaciu stránku
                if($(this).data("protected") == true && link.indexOf("permsDenied") != -1){
                    window.alert("<iwcm:text key="logon.err.unauthorized_group"/>");
                    return false;
                }
            });
		}
    });
</script>

<div class="modal modal-login fade login" tabindex="-1" role="dialog">
	<div class="modal-dialog" role="document" >
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-bs-dismiss="modal" aria-label="Close">
					<span aria-hidden="true">&times;</span>
				</button>
				<h4 class="modal-title"><iwcm:text key="components.site_browser.dirPermissions"/></h4>
			</div>
			<div class="modal-body">
				<div class="loginBox"></div>
			</div>
			<div class="modal-footer">
				<button type="button" class="btn btn-default" data-bs-dismiss="modal"><iwcm:text key="button.cancel"/></button>
			</div>
		</div>
	</div>
</div>

<iwcm:present name="valid" >
	<script type="text/javascript">
			window.alert("<iwcm:text key="fbrowse.alert_message"/>");
	</script>
</iwcm:present>

<script type="text/javascript">
		function noPopup()
		{
			return(false);
		}

		function docLinkClick(linka, file)
		{
			return false;
		}

        function dirLinkClick(linka, dirName)
        {
            return false;
        }
</script>

<div class="site_browse">
	<% if (showActualDir) { %>
		<p id="breadcrumb"><iwcm:text key="fbrowse.dir"/>: <%=("/"+sanitize(((String)request.getAttribute("correctDir")).replace("files","")))%></p>
	<% } %>

	<table class="tabulkaStandard">
		<thead>
			<tr>
				<th class="fN" ><iwcm:text key="fbrowse.title"/></th>
				<th class="fS" ><iwcm:text key="fbrowse.size"/></th>
				<th class="fD" ><iwcm:text key="fbrowse.date"/></th>
			</tr>
		</thead>
		<tbody>
		<iwcm:iterate id="dir" name="dirList" type="sk.iway.iwcm.filebrowser.FileDirBean">
			<tr>
				<td class="fN" >
					<a href="<%=BrowseAction.getDirLink(pageDocID, dir, request)%>" data-protected="<%=dir.isDirProtected()%>" class="siteBrowserClick folder"><jsp:getProperty name="dir" property="icon"/></a>
					<a href="<%=BrowseAction.getDirLink(pageDocID, dir, request)%>" data-protected="<%=dir.isDirProtected()%>" class="siteBrowserClick"><%=sanitize(((sk.iway.iwcm.filebrowser.FileDirBean)pageContext.getAttribute("dir")).getName())%></a>
				</td>
				<td class="fS">
					&lt;<iwcm:text key="fbrowse.dir"/>&gt;
				</td>
				<td class="fD">&nbsp;</td>
			</tr>
		</iwcm:iterate>

		<iwcm:iterate id="file" name="fileList" type="sk.iway.iwcm.filebrowser.FileDirBean">
			<tr>
				<td class="fN" >
					<a href="<%if(request.getAttribute("correctDir")!=null && request.getAttribute("correctDir").toString().length()>1) out.println(request.getAttribute("correctDir"));%>/<jsp:getProperty name="file" property="name"/>" <%if (Tools.isNotEmpty(target)) out.print(" target='"+target+"'");%> class="icon"><%--<img src="<jsp:getProperty name="file" property="icon"/>" alt="<jsp:getProperty name="file" property="name"/>" class="icon" />--%></a>
					<a href="<%if(request.getAttribute("correctDir")!=null && request.getAttribute("correctDir").toString().length()>1) out.println(request.getAttribute("correctDir"));%>/<jsp:getProperty name="file" property="name"/>" <%if (Tools.isNotEmpty(target)) out.print(" target='"+target+"'");%>><jsp:getProperty name="file" property="name"/></a>
				</td>
				<td class="fS" >
					<jsp:getProperty name="file" property="length"/>
				</td>
				<td class="fD" >
					<jsp:getProperty name="file" property="lastModified"/>
				</td>
			</tr>
		</iwcm:iterate>
		</tbody>
	</table>
</div>
<%
WriteTag.setInlineComponentEditTextKey("components.site_browser.addFileOrDirectory", request);
WriteTag.setInlineComponentEditIcon("/components/_common/admin/inline/icon-add_file.png", request);
%>