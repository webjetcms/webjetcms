<%@page import="java.io.IOException"%>
<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%@page import="sk.iway.iwcm.system.datatable.OptionDto"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@ page import="sk.iway.iwcm.system.WJResponseWrapper" %>
<%@ page import="java.io.StringWriter" %>
<%@ page import="java.io.PrintWriter" %>
<%@ page import="java.util.*" %>
<%@ page import="org.apache.struts.util.ResponseUtils" %>
<%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="modUpdate"/>
<%@ include file="/admin/layout_top.jsp" %>

<%!

private static List<OptionDto> replaces = new ArrayList<>();
static {
	//list
	replaces.add(new OptionDto("ArrayList ", "List ", null));
	replaces.add(new OptionDto("ArrayList\t", "List\t", null));
	replaces.add(new OptionDto("ArrayList<", "List<", null));
	replaces.add(new OptionDto("new List", "new ArrayList", null));
	//API changes
	replaces.add(new OptionDto("Constants.INSTALL_NAME", "Constants.getInstallName()", null));
	replaces.add(new OptionDto("InitServlet.SERVER_START_DATETIME", "InitServlet.getServerStartDatetime()", null));
	replaces.add(new OptionDto("UserForm.PASS_UNCHANGED", "sk.iway.iwcm.common.UserTools.PASS_UNCHANGED", null));
	replaces.add(new OptionDto("Tools.geteratePassword(", "Tools.generatePassword(", null));
	replaces.add(new OptionDto("Password.geteratePassword(", "Password.generatePassword(", null));
	//Hashtable
	replaces.add(new OptionDto("Hashtable ", "Map ", null));
	replaces.add(new OptionDto("Hashtable\t", "Map\t", null));
	replaces.add(new OptionDto("Hashtable<", "Map<", null));
	replaces.add(new OptionDto("new Map(", "new Hashtable(", null));
	replaces.add(new OptionDto("new Map<", "new Hashtable<", null));
	//HashMap
	replaces.add(new OptionDto("HashMap ", "Map ", null));
	replaces.add(new OptionDto("HashMap<", "Map<", null));
	replaces.add(new OptionDto("new Map(", "new HashMap(", null));
	replaces.add(new OptionDto("new Map<", "new HashMap<", null));
	//fix LinkedMap
	replaces.add(new OptionDto("new LinkedMap", "new LinkedHashMap", null));
	replaces.add(new OptionDto("LinkedMap<", "LinkedHashMap<", null));
	replaces.add(new OptionDto("LinkedMap ", "LinkedHashMap ", null));
	//fix Struts links
	replaces.add(new OptionDto("/admin/photogallery.do", "/admin/v9/apps/gallery/", null));

	//logon
	replaces.add(new OptionDto("<html:form action=\"/usrlogon\" focus=\"username\" name=\"logonForm\" styleClass=\"form-horizontal\" type=\"sk.iway.iwcm.LogonForm\">", "<form action=\"/usrlogon.do\" method=\"post\" name=\"logonForm\" class=\"form-horizontal\">", null));
	replaces.add(new OptionDto("<html:text property=\"username\" size=\"16\" maxlength=\"64\" styleClass=\"form-control\" styleId=\"name\" />", "<input type=\"text\" name=\"username\" size=\"16\" maxlength=\"64\" class=\"form-control\" id=\"name\" value=\"<"+"% if (request.getParameter(\"username\")!=null) out.print(org.apache.struts.util.ResponseUtils.filter(request.getParameter(\"username\"))); %"+">\" />", null));
	replaces.add(new OptionDto("<html:password property=\"password\" size=\"16\" maxlength=\"16\" redisplay=\"true\" styleClass=\"form-control\" styleId=\"pass\" />", "<input type=\"password\" name=\"password\" size=\"16\" maxlength=\"16\" class=\"form-control\" id=\"pass\" />", null));
	replaces.add(new OptionDto("<html:hidden property=\"docId\"/>", "<input type=\"hidden\" name=\"docId\" value=\"<"+"%=docId%"+">\"/>", null));
	replaces.add(new OptionDto("<html:form action=\"/usrlogon\" focus=\"username\" name=\"logonForm\" type=\"sk.iway.iwcm.LogonForm\">", "<form action=\"/usrlogon.do\" method=\"post\" name=\"logonForm\">", null));
	replaces.add(new OptionDto("<html:password property=\"password\" size=\"16\" maxlength=\"16\" redisplay=\"true\" styleClass=\"input\" styleId=\"oldpass\" />", "<input type=\"password\" name=\"password\" size=\"16\" maxlength=\"16\" class=\"input\" id=\"oldpass\" />", null));
	replaces.add(new OptionDto("<html:password property=\"newPassword\" size=\"16\" maxlength=\"16\" redisplay=\"true\" styleClass=\"input\" styleId=\"newpass\" />", "<input type=\"password\" name=\"newPassword\" size=\"16\" maxlength=\"16\" class=\"input\" id=\"newpass\" />", null));
	replaces.add(new OptionDto("<html:password property=\"retypeNewPassword\" size=\"16\" maxlength=\"16\" redisplay=\"true\" styleClass=\"input\" styleId=\"retypepass\" />", "<input type=\"password\" name=\"retypeNewPassword\" size=\"16\" maxlength=\"16\" class=\"input\" id=\"retypepass\" />", null));

	//thymeleaf 3.1 unsupported objects - https://www.thymeleaf.org/doc/articles/thymeleaf31whatsnew.html
	replaces.add(new OptionDto("$"+"{#request.", "$"+"{request.", ".html"));
	replaces.add(new OptionDto("$"+"{#session.", "$"+"{session.", ".html"));

	//packages change
	replaces.add(new OptionDto("sk.iway.iwcm.components.monitoring.MonitoringManager", "sk.iway.iwcm.components.monitoring.rest.MonitoringManager", null));

	//export dat in custom 404 file
	replaces.add(new OptionDto("if(\"xml\".equals(format)){", "if(\"xml\".equals(format)||\"rss\".equals(format)){", ".jsp"));
}

private void checkDir(String url, boolean saveFile, boolean compileFile, JspWriter out, HttpServletRequest request, HttpServletResponse response) throws IOException
{
	String realPath = Tools.getRealPath(url);
	IwcmFile rootDir = new IwcmFile(realPath);
	IwcmFile[] files;
	if (rootDir.isDirectory()) {
		files = FileTools.sortFilesByName(rootDir.listFiles());
	} else {
		files = new IwcmFile[1];
		url = url.substring(0, url.lastIndexOf("/"));
		if (Tools.isEmpty(url)) url = "/";
		files[0] = rootDir;
	}
	for (IwcmFile f : files)
	{
		if (f.isDirectory())
		{
			if ("node_modules".equals(f.getName()) || "dist".equals(f.getName())) return;
			checkDir(url+f.getName()+"/", saveFile, compileFile, out, request, response);
		}
		else if (f.getName().endsWith(".jsp") || f.getName().endsWith(".html"))
		{
			String fullUrl = url+f.getName();

			//sam sebe sam, rekurzia pri kompilacii
			if ("/admin/update/update-2023-18.jsp".equals(fullUrl)) continue;

			System.out.println(fullUrl);
			out.println(Tools.escapeHtml(fullUrl));
			out.flush();

			String encoding = "utf-8";
			String content = FileTools.readFileContent(fullUrl, encoding);
			String contentOriginal = content;
			if (content.contains("pageEncoding=\"windows-1250") || content.contains("pageEncoding=\"cp-1250")) {
				out.println("   Varovanie: otváram súbor v kódovaní windows-1250");
				encoding = "windows-1250";
				content = FileTools.readFileContent(fullUrl, encoding);
			}

			StringBuilder foundContent = new StringBuilder();
			boolean hasChange = false;
			//prejdi zaznamy v hashtabulke a nahrad
			for (OptionDto entry : replaces) {
				String from = entry.getLabel();
				String to = entry.getValue();
				Object original = entry.getOriginal();
				if (original != null && original instanceof String) {
					String ext = (String)original;
					if (fullUrl.endsWith(ext)==false) continue;
				}
				//original null je mozne pouzit len na jsp subory
				if (original == null && f.getName().endsWith(".jsp")==false) continue;

				if (content.contains(from)) {
					content = Tools.replace(content, from, to);
					foundContent.append("   Našiel som: ").append(Tools.escapeHtml(from)).append("\n");
					hasChange = true;

					//fix struts form close tag
					if (from.contains("<html:form")) {
						content = Tools.replace(content, "</html:form>", "</form>");
					}
				}
			}

			if (hasChange && content.equals(contentOriginal)==false) {
				//FIX import
				if (content.contains("List ") || content.contains("List\t") || content.contains("List<")) {
					if (content.contains("java.util.List")==false) {
						//pridaj import
						content = "<"+"%@page import=\"java.util.List\"%"+">"+content;
					}
				}
				if (content.contains("Map ") || content.contains("Map\t") || content.contains("Map<")) {
					if (content.contains("java.util.Map")==false) {
						//pridaj import
						content = "<"+"%@page import=\"java.util.Map\"%"+">"+content;
					}
				}

				out.print(foundContent.toString());

				if (saveFile) {
					FileTools.saveFileContent(fullUrl, content, encoding);
					out.println("   <strong>Ukladám súbor</strong>: "+fullUrl);
				} else {
					out.println("   <strong>Súbor obsahuje zmenu, uloženie ale nie je aktivované</strong>");
				}
			}

			if (compileFile && f.getName().endsWith(".jsp")) {
				//dlho trvajuce/zacyklenie kompilacie
				if ("test_jspcompilation.jsp".equals(f.getName())) continue;
				if (f.getVirtualPath().startsWith("/admin/update/")) continue;
				if ("keep_alive_test.jsp".equals(f.getName())) continue;
				//includes
				if ("layout_sidebar.jsp".equals(f.getName())) continue;

				try
				{
					WJResponseWrapper respWrapper = new WJResponseWrapper(response, request);
					request.getRequestDispatcher(fullUrl).include(request, respWrapper);
				}
				catch (Exception ex)
				{
					StringWriter sw = new StringWriter();
					ex.printStackTrace(new PrintWriter(sw));

					String stack = sw.toString();

					if (stack.contains("Unable to compile class for JSP") || stack.contains(") JSP file ["))
					{
						//skompilovalo ale nevedelo najst objekt
						if (stack.contains("cannot be resolved to a variable")) {
							if (stack.contains("Syntax error") || stack.contains("Type mismatch") || stack.contains("is not visible")) {
								//obsahuje aj inu chybu, musime zobrazit
							} else {
								out.println("&nbsp;&nbsp;&nbsp;Neviem nájsť objekt, pravdepodobne sa jedná o include JSP, skontrolujte manuálne, že sú zobrazené len chyby cannot be resolved to a variable.<br/>");
								continue;
							}
						}

						int i = stack.indexOf("Stacktrace:");
						if (i > 20) stack = stack.substring(0, i);

						out.println("CHYBA:<br/>");
						out.println(ResponseUtils.filter(stack));
					}
				}
			}
		}
	}
}


%>

<div class="row title">
	<h1 class="page-title">Upraviť kód v JSP súboroch pre WebJET 2023.18</h1>
</div>

<%
	String subdir = Constants.getInstallName();
	if (Tools.getRequestParameter(request, "subdir")!=null) subdir = Tools.getRequestParameter(request, "subdir");
%>

<form action="update-2023-18.jsp" method="post">
	<div class="container" style="margin-top: 16px">
		<div class="row">
			<div class="col-4">Meno priečinka:</div>
			<div class="col-8">
				<div class="input-group input-group-sm">
					<div class="input-group-prepend">
						<span class="input-group-text" id="basic-addon3">/components/
					</div>
					<input type="text" name="subdir" class="form-control" value="<%=ResponseUtils.filter(subdir)%>"/>
				</div>
			</div>
		</div>
		<div class="row">
			<div class="col-4"></div>
			<div class="col-8">
				Zadajte prázdnu hodnotu alebo znak * pre kontrolu všetkých priečinkov
				<br/>
				Zadajte hodnotu templates pre kontrolu priečinka /templates/ a hodnotu admin pre kontrolu priečinka /admin/.
			</div>
		</div>
		<div class="row">
			<div class="col-4">Nastavenia:</div>
			<div class="col-8">
				<div class="form-check">
					<input class="form-check-input toggle" type="checkbox" value="true" name="saveFile" id="saveFile">
					<label class="form-check-label" for="saveFile">
						Uložiť zmenu v súbore
					</label>
				</div>
			</div>
		</div>
		<div class="row">
			<div class="col-4"></div>
			<div class="col-8">
				<div class="form-check">
					<input class="form-check-input toggle" type="checkbox" value="true" name="compileFile" id="compileFile">
					<label class="form-check-label" for="compileFile">
						Vykonať kontrolu kompilácie - technicky sa spustí/vykoná JSP súbor, verím, že tam nemáte nič čo by len spustením JSP vykonalo niečo zlé (zmazalo dáta)
					</label>
				</div>
			</div>
		</div>
		<div class="row">
			<div class="col-12"><input type="submit" class="btn btn-primary" value="Spustiť"/></div>
		</div>
	<div>
</form>

<% if (Tools.getRequestParameter(request, "subdir")!=null) {
	boolean saveFile = "true".equals(request.getParameter("saveFile"));
	boolean compileFile = "true".equals(request.getParameter("compileFile"));
%>

	<div style="white-space: pre"><%
		if (Tools.isEmpty(subdir) || "*".equals(subdir)) {
			checkDir("/admin/", saveFile, compileFile, out, request, response);
			checkDir("/components/", saveFile, compileFile, out, request, response);
			checkDir("/apps/", saveFile, compileFile, out, request, response);
			checkDir("/templates/", saveFile, compileFile, out, request, response);
		}
		else if ("admin".equals(subdir) || "*".equals(subdir)) {
			checkDir("/admin/", saveFile, compileFile, out, request, response);
		}
		else if ("templates".equals(subdir) || "*".equals(subdir)) {
			checkDir("/templates/", saveFile, compileFile, out, request, response);
		}
		else {
			checkDir("/components/" + subdir + "/", saveFile, compileFile, out, request, response);
			checkDir("/apps/" + subdir + "/", saveFile, compileFile, out, request, response);
		}
		checkDir("/403.jsp", saveFile, compileFile, out, request, response);
		checkDir("/404.jsp", saveFile, compileFile, out, request, response);
		checkDir("/500.jsp", saveFile, compileFile, out, request, response);
		%>
	</div>
	<p>
		Hotovo
	</p>
<% } %>


<%@ include file="/admin/layout_bottom.jsp" %>