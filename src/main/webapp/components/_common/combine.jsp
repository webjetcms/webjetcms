<%

String type = "js";
if ("css".equals(Tools.getRequestParameter(request, "t"))) type = "css";


if ("css".equals(type)) sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/css");
else {type="js"; sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/javascript"); }

sk.iway.iwcm.PathFilter.setStaticContentHeaders("/cache/combine."+type, null, request, response);
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%@page import="java.util.StringTokenizer"%><%@page
        import="java.io.File"%><%@page
        import="sk.iway.iwcm.io.IwcmFile"%><%@ page
        import="sk.iway.iwcm.tags.CombineTag" %><%@ page
        import="sk.iway.iwcm.common.WriteTagToolsForCore" %><%@ page
        import="sk.iway.iwcm.common.DocTools" %><%@ page
        import="sk.iway.iwcm.common.FileBrowserTools" %><%@page
        import="sk.iway.iwcm.users.UsersDB"%><%

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

String files = Tools.getRequestParameter(request, "f");
if (Tools.isEmpty(files)) return;

boolean isFromConstant = false;
if (files.startsWith("/")==false)
{
	String constValue = Constants.getString("combine-"+files);
	if (Tools.isNotEmpty(constValue))
	{
	   files = constValue;
	   isFromConstant = true;
	}
}

StringTokenizer st = new StringTokenizer(files, ",\n");

Identity user = UsersDB.getCurrentUser(request);

while (st.hasMoreTokens())
{
	String fileUrl = st.nextToken().trim();

	if (Tools.isEmpty(fileUrl) || fileUrl.startsWith("/")==false) continue;
	if (fileUrl.contains("WEB-INF") || fileUrl.contains("combine.jsp")) continue;
	if ( (user==null || user.isAdmin()==false) && fileUrl.contains("admin")) continue;
	if (FileBrowserTools.hasForbiddenSymbol(fileUrl)) continue;

	//povolene su len pripony js, css a jsp
	String ext = FileTools.getFileExtension(fileUrl);
	if (isFromConstant==false && "js".equals(ext)==false && "css".equals(ext)==false && "jsp".equals(ext)==false)
	{
		Logger.debug(CombineTag.class, "URL (ext) not allowed: "+fileUrl);
	   continue;
	}

	//povolene su len cesty definovane v combineEnabledPaths
	if (isFromConstant==false && DocTools.isUrlAllowed(fileUrl, "combineEnabledPaths", true)==false)
	{
	   Logger.debug(CombineTag.class, "URL not allowed: "+fileUrl);
	   continue;
	}

	boolean customFileWritten = false;
	if (PathFilter.getCustomPath() != null)
	{
		//skontroluj, ci pozadovany subor nie je custom (pre vyvoj)
		IwcmFile f = new IwcmFile(PathFilter.getCustomPath() + File.separatorChar + Constants.getInstallName() + fileUrl.replace('/', File.separatorChar));
		//Logger.println(this,"Checking CUSTOM: "+f.getAbsolutePath());
		if (f.exists() && f.isFile())
		{
			out.println("/* =========================================================================== */");
			out.println("/* fi:"+fileUrl+" */");
			out.println("/* =========================================================================== */");

			//existuje custom subor, posli ho na vystup
			if (fileUrl.toLowerCase().endsWith(".js") || fileUrl.toLowerCase().endsWith(".css"))
			{
				//Logger.println(this,"sending custom page:
				// "+f.getAbsolutePath());
				//je to nejaky obrazok, alebo subor, posli na vystup
				String fileContent = FileTools.readFileContent(fileUrl, SetCharacterEncodingFilter.getEncoding());
				fileContent = Tools.replace(fileContent, "url(../", "url(/");
				fileContent = Tools.replace(fileContent, "url('../", "url('/");
				out.println(fileContent);
				out.println();
				customFileWritten = true;
			}
		}
	}

	if (customFileWritten == false)
	{
		if (fileUrl.endsWith(".js") && (new File(sk.iway.iwcm.Tools.getRealPath(fileUrl+".jsp"))).exists())
		{
			if (isFromConstant || DocTools.isUrlAllowed(fileUrl+".jsp", "combineEnabledJsps", true))
			{
				pageContext.include(fileUrl + ".jsp");
				out.println();
			}
			else
			{
				Logger.debug(CombineTag.class, "URL (jsp) not allowed: "+fileUrl);
			}
		}
		else
		{
			String noCustomFileUrl = fileUrl;
			fileUrl = WriteTagToolsForCore.getCustomPage(fileUrl, request);
			IwcmFile file = new IwcmFile(sk.iway.iwcm.Tools.getRealPath(fileUrl));

			if (file.exists())
			{
				if (fileUrl.toLowerCase().endsWith(".jsp"))
				{
					if (isFromConstant || DocTools.isUrlAllowed(noCustomFileUrl, "combineEnabledJsps", true))
					{
						pageContext.include(fileUrl);
						out.println();
					}
					else
					{
						Logger.debug(CombineTag.class, "URL (jsp) not allowed: "+fileUrl);
					}
				}
				else if (fileUrl.toLowerCase().endsWith(".js") || fileUrl.toLowerCase().endsWith(".css"))
				{
					//over, ci existuje .map subor
					if (fileUrl.toLowerCase().endsWith(".css") && (
						FileTools.isFile(noCustomFileUrl+".map") ||
						FileTools.isFile(fileUrl+".map")
					)) {
						response.addHeader("SourceMap", noCustomFileUrl+".map");
					} else {
						if ((fileUrl.contains("ninja.js") || fileUrl.contains("ninja.min.js")) && FileTools.isFile(noCustomFileUrl+".map") || FileTools.isFile(fileUrl+".map")) {
							//ak ma ninja.js map subor negeneruj koment, lebo potom nesedia cisla riadkov
							response.addHeader("SourceMap", noCustomFileUrl+".map");
						} else {
							out.println("/* =========================================================================== */");
							out.println("/* "+noCustomFileUrl+" */");
							out.println("/* =========================================================================== */");
						}
					}

					//Logger.println(this,"sending custom page:
					// "+f.getAbsolutePath());
					//je to nejaky obrazok, alebo subor, posli na vystup
					String fileContent = FileTools.readFileContent(fileUrl, SetCharacterEncodingFilter.getEncoding());
					fileContent = Tools.replace(fileContent, "url(../", "url(/");
					fileContent = Tools.replace(fileContent, "url('../", "url('/");
					fileContent = Tools.replace(fileContent, "url(\"../", "url(\"/");
					//odstran utf-8 BOM znacku
					if (fileContent.startsWith("\uFEFF")) fileContent = fileContent.replace('\uFEFF',' ');
					//zrus povodny odkaz na sourceMappingURL
					fileContent = Tools.replace(fileContent, "sourceMappingURL", "sourceMappingURLIgnored");
					out.println(fileContent);
					out.println();
				}
			}
			else
			{
				System.out.println("COMBINE.JSP DOESNT EXISTS: "+file.getAbsolutePath());
			}
		}
	}
}
%>