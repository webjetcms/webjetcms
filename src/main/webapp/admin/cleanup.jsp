<%@page import="java.util.List"%><%
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
			if ("CVS".equals(files[i].getName()))
			{
				out.println("&nbsp;&nbsp;&nbsp;Deleting DIR: " + Tools.escapeHtml(files[i].getAbsolutePath())+"<br>");
				deleteImpl(files[i], request);
			}
		}
		else
		{
			if ("CVS".equals(rootFile.getName()))
			{
				out.println("&nbsp;&nbsp;&nbsp;Deleting FILE: " + Tools.escapeHtml(files[i].getAbsolutePath())+"<br>");
				deleteImpl(files[i], request);
			}
		}
	}
}

public void delModule(ModuleInfo m, JspWriter out, HttpServletRequest request)
{
	File f = new File(Constants.getServletContext().getRealPath(m.getPath()));
	if (f.isFile() && f.canWrite())
	{
		deleteImpl(f, request);
	}
}

public void delComponent(String path, JspWriter out, HttpServletRequest request) throws IOException
{
	File f = new File(Constants.getServletContext().getRealPath(path));
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
%>
<% System.out.println("-3"); %>
<%@ include file="layout_top.jsp" %>

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
		String cvsPath = Constants.getServletContext().getRealPath("/");
		if (Tools.getRequestParameter(request, "cvsPath")!=null)
		{
			cvsPath = Tools.getRequestParameter(request, "cvsPath");
		}

		delCVS(cvsPath, out, request);
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

	//statistika
	if ("true".equals(Tools.getRequestParameter(request, "statLog")))
	{
		System.out.println("Mazem statistiku");

		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("DELETE FROM stat_browser");
			ps.execute();
			ps.close();

			ps = db_conn.prepareStatement("DELETE FROM stat_country");
			ps.execute();
			ps.close();

			ps = db_conn.prepareStatement("DELETE FROM stat_doc");
			ps.execute();
			ps.close();

			ps = db_conn.prepareStatement("DELETE FROM stat_error");
			ps.execute();
			ps.close();

			ps = db_conn.prepareStatement("DELETE FROM stat_from");
			ps.execute();
			ps.close();

			ps = db_conn.prepareStatement("DELETE FROM stat_searchengine");
			ps.execute();
			ps.close();

			ps = db_conn.prepareStatement("DELETE FROM stat_site_days");
			ps.execute();
			ps.close();

			ps = db_conn.prepareStatement("DELETE FROM stat_site_hours");
			ps.execute();
			ps.close();

			ps = db_conn.prepareStatement("DELETE FROM stat_userlogon");
			ps.execute();
			ps.close();

			ps = db_conn.prepareStatement("DELETE FROM stat_views");
			ps.execute();
			ps.close();

			ps = db_conn.prepareStatement("UPDATE documents SET views_total=0, views_month=0");
			ps.execute();
			ps.close();

			db_conn.close();
			db_conn = null;
			ps = null;

			out.println("Statistics data deleted<br><br>");
		}
		catch (Exception ex)
		{
			out.println("ERROR: Statistics data: "+ex.getMessage()+" <br><br>");
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}
	}
}

request.setAttribute("modules", allModules);
%>

<h1>Vymazanie nepotrebných súborov a modulov</h1>

<form action="cleanup.jsp" method="post">

	<h2>Vymazanie súborov:</h2>
	<input type="checkbox" name="cvs" value="true"> CVS<br>

	<h2>Vymazanie modulov:</h2>
	<logic:iterate name="modules" id="m" type="sk.iway.iwcm.system.ModuleInfo">
		<input type="checkbox" name="m_<bean:write name="m" property="itemKey"/>" value="delete"> <iwcm:text key="<%=m.getNameKey()%>"/> [<bean:write name="m" property="itemKey"/>]<br>
	</logic:iterate>

	<h2>Vymazanie komponent:</h2>
<%
//prescanuj adresar /components na podadresare, ktory existuje vypis
String dirPath = sk.iway.iwcm.Constants.getServletContext().getRealPath("/components/");
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

	<h2>Vymazanie obsahu databáz:</h2>
	<input type='checkbox' name='statLog' value='true'>Štatistika<br>

	<br><br>

	<input type="submit" name="bSubmit" value="Cleanup">
	<input type="hidden" name="doAction" value="cleanup">

</form>

<%@ include file="layout_bottom.jsp" %>
