<%@page import="sk.iway.iwcm.Tools"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.i18n.Prop" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%!

public boolean hasSubfolder(IwcmFile f, Identity user)
{
	IwcmFile files[] = f.listFiles();
	if (files==null) return false;
	boolean canShow =false;
	for (int i=0; i<files.length; i++)
	{
		if ("CVS".equalsIgnoreCase(files[i].getName())) continue;
		canShow = false;
		if(files[i].isDirectory())
		{
			for(IwcmFile file: user.getWritableFoldersList())
			{
				if(file.getVirtualPath().startsWith(files[i].getVirtualPath()))
					canShow = true;
			}
			return canShow || user.isFolderWritable(files[i].getVirtualPath());
		}
	}
	return false;
}

public String printFilesTree(String path, String parentElement, int level, String pathArray[], Identity user, HttpServletRequest request) throws IOException
{
	//System.out.println("printFilesTree("+path+");");
	Prop prop = Prop.getInstance(request);
	IwcmFile files[] = (new IwcmFile(sk.iway.iwcm.Tools.getRealPath(path))).listFiles();
	if (files==null) return "";
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

	StringBuffer out = new StringBuffer();
	boolean canShow = false;
	boolean writable = false;
	String jsMethod="";
	for (int i=0; i<files.length; i++)
	{
		if ("CVS".equalsIgnoreCase(files[i].getName())) continue;
		if(files[i].isDirectory())
		{
			writable = canShow = false;
			//Pre testovanie :  "folderClick('"+path+files[i].getName()+"')";
			jsMethod = "alert('"+prop.getText("user.rights.no_folder_rights")+"')";
			for(IwcmFile file: user.getWritableFoldersList())
			{
				//System.out.print(files[i].getVirtualPath()+" - ");
				if(file.getVirtualPath().startsWith(files[i].getVirtualPath()))
					canShow = true;
               // System.out.print("vritable ? "+user.isFolderWritable(files[i].getVirtualPath())+" ");
               // System.out.println("canShow: "+canShow);
			}

			if(user.isFolderWritable(files[i].getVirtualPath()))
			{
				writable = true;
				jsMethod = "folderClick('"+path+files[i].getName()+"')";
			}

			if(!writable && !canShow)
			{
				//System.out.println("continue: "+files[i].getVirtualPath());
				continue;
			}

			boolean hasSubfolder = hasSubfolder(files[i], user);

			String fileName = Tools.escapeHtml(files[i].getName());

			if (hasSubfolder==false && !canShow)
			{
				out.append("var treeItem_"+level+"_"+i+" = new WebFXTreeItem(\""+fileName+"\", \"javascript:"+jsMethod +"\", null, \"/admin/images/dtree/folder.gif\", \"/admin/images/dtree/folderopen.gif\");");
				out.append(parentElement+".add(treeItem_"+level+"_"+i+");");
			}
			else
			{
				String subData = null;
				if (pathArray.length>level && pathArray[level].equals(files[i].getName()))
				{
					subData = printFilesTree(path+fileName+"/", "treeItem_"+level+"_"+i, level+1, pathArray, user, request);
				}

				if (Tools.isNotEmpty(subData))
				{
					out.append("var treeItem_"+level+"_"+i+" = new WebFXTreeItem(\""+fileName+"\", \"javascript:"+jsMethod +"\", null, \"/admin/images/dtree/folder.gif\", \"/admin/images/dtree/folderopen.gif\");");
					out.append(parentElement+".add(treeItem_"+level+"_"+i+");");
					out.append(subData);
				}
				else
				{
					out.append("var treeItem_"+level+"_"+i+" = new WebFXLoadTreeItem(\""+fileName+"\", \"/admin/skins/webjet6/left_files-tree.jsp?parentId="+i+"&path="+Tools.URLEncode(path+fileName)+"\", \"javascript:"+jsMethod +"\", null, \"/admin/images/dtree/folder.gif\", \"/admin/images/dtree/folderopen.gif\");");
					out.append(parentElement+".add(treeItem_"+level+"_"+i+");");
				}
			}
		}
	}

	return out.toString();
}
%>
<%@ include file="/admin/skins/webjet6/layout_top.jsp" %>
<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%@page import="java.io.IOException"%>

<%@page import="java.util.Arrays"%>
<%@page import="java.util.Comparator"%><style>
#leftFrameBody { overflow: auto;}
</style>
<%@page import="java.util.StringTokenizer"%>
<%@ page import="sk.iway.iwcm.Identity" %>
<%@ page import="sk.iway.iwcm.filebrowser.BrowseAction" %>
<jsp:useBean id="iwcm_useriwcm" scope="session" type="sk.iway.iwcm.Identity"/>
<%
Identity user = (Identity)session.getAttribute("iwcm_useriwcm");
String rootDir = Tools.getRequestParameter(request, "rootDir");
if(Tools.isEmpty(rootDir) || BrowseAction.hasForbiddenSymbol(rootDir))
	rootDir = "/files/";
%>
<script language="JavaScript">
var helpLink = "";
</script>

<script type="text/javascript" src="/admin/scripts/divpopup.js"></script>
<script type="text/javascript" src="/admin/scripts/groupslist.jsp"></script>
<script type="text/javascript" src="/admin/scripts/xtree/xtree-dragdrop.js"></script>
<script type="text/javascript" src="/admin/scripts/xtree/xmlextras.js"></script>
<script type="text/javascript" src="/admin/scripts/xtree/xloadtree.js"></script>
<link type="text/css" rel="stylesheet" href="/admin/scripts/xtree/xtree.css" />
<script type="text/javascript">
//toto treba zadefinovat v stranke po includnuti divpopup.js
//je to offset o ktory sa posuva okno vlavo
leftOffset=-445;
//a toto ofset o ktory sa posuva nadol
topOffset=10;

webFXTreeConfig.loadingText = "<iwcm:text key="groupslist.xtree.loading"/>";

function folderClick(path)
{
	window.opener.setFileBrowserPath(path);
	window.close();
}

var globalCtxNodePath = "";
</script>

<script type="text/javascript">

	/// XP Look
	webFXTreeConfig.rootIcon		= "/admin/images/dtree/base.gif";
	webFXTreeConfig.openRootIcon	= "/admin/images/dtree/base.gif";
	webFXTreeConfig.folderIcon		= "/admin/images/dtree/folder.gif";
	webFXTreeConfig.openFolderIcon	= "/admin/images/dtree/folderopen.gif";
	webFXTreeConfig.fileIcon		= "/admin/images/dtree/folder.gif";
	webFXTreeConfig.lMinusIcon		= "/admin/images/dtree/minusbottom.gif";
	webFXTreeConfig.lPlusIcon		= "/admin/images/dtree/plusbottom.gif";
	webFXTreeConfig.tMinusIcon		= "/admin/images/dtree/minus.gif";
	webFXTreeConfig.tPlusIcon		= "/admin/images/dtree/plus.gif";
	webFXTreeConfig.iIcon			= "/admin/images/dtree/line.gif";
	webFXTreeConfig.lIcon			= "/admin/images/dtree/joinbottom.gif";
	webFXTreeConfig.tIcon			= "/admin/images/dtree/join.gif";
	webFXTreeConfig.blankIcon			= "/admin/images/dtree/blank.png";
	webFXTreeConfig.usePersistence = false;
	webFXTreeConfig.focus = null;

	<%
	Prop prop = Prop.getInstance(request);
	String function = "folderClick('"+rootDir+"')";
	if(!user.isFolderWritable(rootDir))
	{
		function = "alert('"+prop.getText("user.rights.no_folder_rights")+"')";
	}
	%>
	var tree = new WebFXTree("<b><%=rootDir%></b>", "javascript:<%=function%>", null, null, "/admin/images/dtree/base.gif", "/admin/images/dtree/base.gif");

	<%
	String actualPath = rootDir;
	System.out.println(actualPath);
	if (Tools.isNotEmpty(actualPath))
	{
		String pathArray[] = actualPath.split("/");
		out.println(printFilesTree(rootDir, "tree", 1, pathArray, user, request));

		StringTokenizer st = new StringTokenizer(actualPath, "/");
		String path = "";
		while (st.hasMoreTokens())
		{
			path = path + "/" + st.nextToken();
			System.out.println("path="+path);
		}
	}
	else
	{
		//printFilesTree("/", out);
	}
	%>
	document.write(tree);
	try
	{
		var parentTreeItem = tree;
		<%

		if (Tools.isNotEmpty(actualPath))
		{
			String pathArray[] = actualPath.split("/");
			for (int i=0; i<pathArray.length; i++)
			{
			   if (Tools.isNotEmpty(pathArray[i]))
			   {
			   	%>
			   	//window.alert(parentTreeItem.childNodes.length);
			   	for (i=0; i<parentTreeItem.childNodes.length; i++)
			   	{
			   		if (parentTreeItem.childNodes[i].origSText=="<%=pathArray[i]%>")
			   		{
			   			//window.alert(parentTreeItem.childNodes[i].origSText);
			   			parentTreeItem.childNodes[i].expand();
			   			parentTreeItem = parentTreeItem.childNodes[i];
			   		}
			   	}
			   	<%
			   }
			}
		}
		%>
	} catch (e) {}

</script>

<%@ include file="/admin/skins/webjet6/layout_bottom_left.jsp" %>
