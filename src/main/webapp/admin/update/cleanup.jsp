<%@page import="java.util.List"%><%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"
import="sk.iway.iwcm.*,
sk.iway.iwcm.system.*,
java.io.*,
java.util.*,
java.sql.*" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<iwcm:checkLogon admin="true" perms="modUpdate"/>
<%
	response.setHeader("Pragma","No-Cache");
	response.setDateHeader("Expires",0);
	response.setHeader("Cache-Control","no-Cache");
%>
<%!
private boolean deleteImpl(File f, HttpServletRequest request)
{
	if ("iwcm.interway.sk".equals(request.getServerName()) && f.getAbsolutePath().indexOf("stary_pocitac")==-1 && f.getAbsolutePath().indexOf("c:\\temp\\")==-1)
	{
		System.out.println("IWCM DELETE: " + f.getAbsolutePath());
		return(true);
	}
	else
	{
		return(f.delete());
	}
}

public void delCVS(String rootPath, JspWriter out, HttpServletRequest request) throws IOException
{
	File rootFile = new File(rootPath);
	File parentFile = rootFile.getParentFile();
	File files[] = rootFile.listFiles();
	int len = files.length;
	int i;
	for (i=0; i<len; i++)
	{
		if (files[i].isDirectory())
		{
			//zavolaj rekurziu
			delCVS(files[i].getAbsolutePath(), out, request);
			//ak je to adresar CVS, vymaz
			if ("CVS".equals(files[i].getName()) || ".svn".equals(files[i].getName()))
			{
				out.println("&nbsp;&nbsp;&nbsp;Deleting DIR: " + Tools.escapeHtml(files[i].getAbsolutePath())+"<br>");
				IwcmFile file = new IwcmFile(files[i]);
				FileTools.deleteDirTree(file);
			}
		}
		else
		{
			if ("CVS".equals(rootFile.getName()) || files[i].getAbsolutePath().toLowerCase().endsWith(".bak") || files[i].getAbsolutePath().endsWith(".DS_Store"))
			{
				out.println("&nbsp;&nbsp;&nbsp;Deleting FILE: " + Tools.escapeHtml(files[i].getAbsolutePath())+"<br>");
				deleteImpl(files[i], request);
			}
		}
	}
}

public void delModule(ModuleInfo m, JspWriter out, HttpServletRequest request)
{
	File f = new File(sk.iway.iwcm.Tools.getRealPath(m.getPath()));
	if (f.isFile() && f.canWrite())
	{
		deleteImpl(f, request);
	}
}

public void delComponent(String path, JspWriter out, HttpServletRequest request) throws IOException
{
	File f = new File(sk.iway.iwcm.Tools.getRealPath(path));
	if (f.isFile())
	{
		f = f.getParentFile();
	}

	if (f.isDirectory() && f.canWrite())
	{
		//zmaz adresar
		delDir(f.getAbsolutePath(), out, request);
	}
}

private void delDir(String realPath, JspWriter out, HttpServletRequest request) throws IOException
{
	File rootDir = new File(realPath);
	if (rootDir.isDirectory() && rootDir.canWrite())
	{
		//zmaz vsetko v adresari rekurzivne
		File files[] = rootDir.listFiles();
		int len = files.length;
		int i;
		for (i=0; i<len; i++)
		{
			if (files[i].isDirectory())
			{
				//zavolaj rekurziu
				delDir(files[i].getAbsolutePath(), out, request);
				//vymaz adresar (jeho podadresare by uz mali byt vymazane)
				deleteImpl(files[i], request);
			}
			else
			{
				out.println("&nbsp;&nbsp;&nbsp;Deleting FILE: " + Tools.escapeHtml(files[i].getAbsolutePath())+"<br>");
				deleteImpl(files[i], request);
			}
		}

		//zmaz tento adresar
		deleteImpl(rootDir, request);
	}
}
public void delClusterPublic(String rootURL, JspWriter out, HttpServletRequest request) throws IOException
{
	File rootFile = new File(sk.iway.iwcm.Tools.getRealPath(rootURL));
	File files[] = rootFile.listFiles();
	int len = files.length;
	int i;
	for (i=0; i<len; i++)
	{
		if (files[i].isDirectory())
		{
			//zavolaj rekurziu
			delClusterPublic(rootURL+"/"+files[i].getName(), out, request);
			if (rootFile.getName().startsWith("admin"))
			{
				out.println("&nbsp;&nbsp;&nbsp;Deleting DIR: " + Tools.escapeHtml(files[i].getAbsolutePath())+"<br>");
				delDir(files[i].getAbsolutePath(), out, request);
			}
		}
		else
		{
			if (rootFile.getName().startsWith("admin"))
			{
				out.println("&nbsp;&nbsp;&nbsp;Deleting FILE: " + Tools.escapeHtml(files[i].getAbsolutePath())+"<br>");
				deleteImpl(files[i], request);
			}
		}
	}
}
public void delClusterAdmin(String rootURL, JspWriter out, HttpServletRequest request) throws IOException
{
	if (rootURL.startsWith("/admin/update")) return;
	if (rootURL.startsWith("/admin/css")) return;
	if (rootURL.startsWith("/admin/scripts")) return;

	File rootFile = new File(sk.iway.iwcm.Tools.getRealPath(rootURL));
	File files[] = rootFile.listFiles();
	int len = files.length;
	int i;
	for (i=0; i<len; i++)
	{
		if (files[i].isDirectory())
		{
			//zavolaj rekurziu
			delClusterAdmin(rootURL+files[i].getName()+"/", out, request);
		}
		else
		{
			if (files[i].getName().equals("index.jsp") || files[i].getName().equals("logon.jsp") || files[i].getName().indexOf("layout_")!=-1)
			{
				continue;
			}
			out.println("&nbsp;&nbsp;&nbsp;Deleting FILE: " + Tools.escapeHtml(files[i].getAbsolutePath())+"<br>");
			deleteImpl(files[i], request);
		}
	}
	out.println("&nbsp;&nbsp;&nbsp;Deleting DIR: " + Tools.escapeHtml(rootFile.getAbsolutePath())+"<br>");
	if ("/admin/".equals(rootURL)==false) delDir(rootFile.getAbsolutePath(), out, request);
}
%>
<% System.out.println("-3"); %>
<%@ include file="/admin/layout_top.jsp" %>

<%
System.out.println("-2");

Modules modules = Modules.getInstance();
List allModules = modules.getAvailableModules();

System.out.println("-1");

if ("cleanup".equals(Tools.getRequestParameter(request, "doAction")))
{
	System.out.println("0");

	if ("true".equalsIgnoreCase(Tools.getRequestParameter(request, "cvs")))
	{
		out.println("<b>Cleaning CVS files:</b><br>");
		//zrusi vsetky CVS adresare
		String cvsPath = sk.iway.iwcm.Tools.getRealPath("/");
		if (Tools.getRequestParameter(request, "cvsPath")!=null)
		{
			cvsPath = Tools.getRequestParameter(request, "cvsPath");
		}

		delCVS(cvsPath, out, request);
	}

	if ("true".equalsIgnoreCase(Tools.getRequestParameter(request, "cluster")))
	{
		String type = Constants.getString("clusterMyNodeType");
		out.println("<b>Cleaning cluster files "+type+":</b><br>");
		//zrusi vsetky CVS adresare
		if ("public".equals(type))
		{
			delClusterAdmin("/admin/", out, request);
			//delClusterPublic("/components/", out, request);
		}
	}

	System.out.println("1");

	Iterator iter = allModules.iterator();
	ModuleInfo m;
	while (iter.hasNext())
	{
		m = (ModuleInfo)iter.next();
		if (m.isAvailable() && "delete".equals(Tools.getRequestParameter(request, "m_"+m.getItemKey())))
		{
			out.println("<b>Deleting module: " + m.getItemKey() + " path=" + m.getPath() + "</b><br>");

			if (m.getPath().startsWith("/components"))
			{
				delComponent(m.getPath(), out, request);
			}
			else
			{
				delModule(m, out, request);
			}

		}
	}

	System.out.println("2");

	//komponenty
	String components[] = Tools.getRequestParameterValues(request, "componentsDelete");
	if (components != null)
	{
		int size = components.length;
		int i;
		for (i=0; i<size; i++)
		{
			out.println("<b>Deleting component: " + components[i] + "</b><br>");
			delComponent("/components/"+components[i], out, request);
		}
	}

	System.out.println("3");

	//refreshni zoznam modulov
	modules = Modules.getInstance(null, true);
	allModules = modules.getAvailableModules();
}

request.setAttribute("modules", allModules);
%>

<h1>Vymazanie nepotrebných súborov a modulov</h1>

<form action="cleanup.jsp" method="post">

	<h2>Vymazanie súborov:</h2>
	<input type="checkbox" name="cvs" value="true"> CVS &amp; SVN &amp; .bak<br>
	<input type="checkbox" name="cluster" value="true"> Cluster node TYPE=<%=Constants.getString("clusterMyNodeType") %><br>

	<h2>Vymazanie modulov:</h2>
	<iwcm:iterate name="modules" id="m" type="sk.iway.iwcm.system.ModuleInfo">
		<input type="checkbox" name="m_<bean:write name="m" property="itemKey" <% if (m.isAvailable()==false) out.print(" checked='checked'"); %>/>" value="delete"> <iwcm:text key="<%=m.getNameKey()%>"/> [<bean:write name="m" property="itemKey"/>]<br>
	</iwcm:iterate>

	<h2>Vymazanie komponent:</h2>
<%
//prescanuj adresar /components na podadresare, ktory existuje vypis
String dirPath = sk.iway.iwcm.Tools.getRealPath("/components/");
if (dirPath!=null)
{
   String toolbarPath;
   File dir = new File(dirPath);
   File files[] = dir.listFiles();
   int size = files.length;
   int i;
   for (i=0; i<size; i++)
   {
      dir = files[i];
      if (dir.isDirectory())
      {
         if ("CVS".equals(dir.getName()))
         {
            continue;
         }
         out.println("<input type='checkbox' name='componentsDelete' value='"+Tools.escapeHtml(dir.getName())+"'> "+Tools.escapeHtml(dir.getName())+"<br>");
      }//isDir
   }//for
}//dirPath!=null

%>

	<input type="submit" name="bSubmit" value="Cleanup">
	<input type="hidden" name="doAction" value="cleanup">

</form>

<%@ include file="/admin/layout_bottom.jsp" %>
