<%@page import="sk.iway.iwcm.i18n.Prop"%><%@
page import="sk.iway.iwcm.users.UsersDB"%><%
response.setContentType("text/xml; charset=UTF-8");

response.setHeader("Pragma","No-Cache");
response.setDateHeader("Expires",0);
response.setHeader("Cache-Control","no-Cache");

%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,java.util.*" %><%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><iwcm:checkLogon admin="true"/><%
String parentId = Tools.getRequestParameter(request, "parentId");
String path = Tools.getRequestParameter(request, "path");

if (ContextFilter.isRunning(request))
{
	path = ContextFilter.removeContextPath(request.getContextPath(), path);
}

%><%!

public boolean hasSubdirs(IwcmFile f, Identity user)
{
	IwcmFile files[] = f.listFiles();
	if (files == null) return false;
	for (int i=0; i<files.length; i++)
	{
		if(files[i].isDirectory() && BrowseAction.hasForbiddenSymbol(files[i].getName())==false && user.isFolderWritable(files[i].getVirtualPath())) return true;
	}
	return false;
}

%><?xml version="1.0"?>

<%@page import="sk.iway.iwcm.system.context.ContextFilter"%>
<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%@page import="sk.iway.iwcm.filebrowser.BrowseAction"%><tree>


	<%
	Prop prop = Prop.getInstance(request);
	Identity user = UsersDB.getCurrentUser(request);
	IwcmFile files[] = (new IwcmFile(sk.iway.iwcm.Tools.getRealPath(path))).listFiles();
	//usporiadaj podla nazvu
	Arrays.sort(files,
				new Comparator()
				{
					public int compare(Object o1, Object o2)
					{
						IwcmFile f1 = (IwcmFile) o1;
						IwcmFile f2 = (IwcmFile) o2;
						return (f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase()));
					}
				});

	boolean canShow = false;
	boolean writable = false;
	String jsMethod="";
	for (int i=0; i<files.length; i++)
	{
		writable = canShow = false;
		jsMethod = "alert('"+prop.getText("user.rights.no_folder_rights")+"')";
		for(IwcmFile writableFile: user.getWritableFoldersList())
		{
			//System.out.println("compare: "+writableFile.getVirtualPath()+" vs: "+files[i].getVirtualPath()+" "+writableFile.getVirtualPath().startsWith(files[i].getVirtualPath()));
			if(writableFile.getVirtualPath() != null && files[i].getVirtualPath() != null)
				if(writableFile.getVirtualPath().startsWith(files[i].getVirtualPath()))
					canShow = true;
		}

		if(user.isFolderWritable(files[i].getVirtualPath()))
		{
			writable = true;
			jsMethod = "folderClick('"+Tools.escapeHtml(path+"/"+files[i].getName())+"')";
		}

		if(!writable&& !canShow)
			continue;

		if ("lost+found".equals(files[i].getName())) continue;
		if(files[i].isDirectory() && BrowseAction.hasForbiddenSymbol(files[i].getName())==false)
		{
			String src = "src=\"/admin/dialog-select-dir-tree-ajax.jsp?parentId="+parentId+"-"+i+"&amp;path="+Tools.escapeHtml(path+"/"+files[i].getName())+"\"";
			if (hasSubdirs(files[i], user)==false) src="";
			%><tree text="<%=Tools.escapeHtml(files[i].getName())%>" action="javascript:<%=jsMethod %>" icon="/admin/images/dtree/folder.gif" openIcon="/admin/images/dtree/folderopen.gif" <%=src%> /><%
		}
	}
	%>
</tree>
