package sk.iway.iwcm.components.offline;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.LabelValueDetails;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.RelatedPagesDB;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.system.zip.ZipEntry;
import sk.iway.iwcm.system.zip.ZipOutputStream;

public class OfflineService {
    Map<String, String> downloadedUrls = new Hashtable<>();
	List<LabelValueDetails> needDownloadUrlsList = new ArrayList<>();
	Map<String, String> allreadyHasInDownloadList = new Hashtable<>();
	private static final int BUFFER = 8192;

	public static final String CROP_START = "<!-- offline crop start -->";
	public static final String CROP_END = "<!-- offline crop end -->";

	public void execute(
         Identity user,
         HttpServletRequest request,
         HttpServletResponse response) throws IOException
    {
		Logger.println(OfflineAction.class,"offlineAction");

		//setni usera do servlet contextu, ten sa potom vybera v PathFilter (inak nie je mozne preniest login)
		Constants.getServletContext().setAttribute(Constants.USER_KEY, user);


		Prop prop = Prop.getInstance(request);

		response.setContentType("text/html; charset="+SetCharacterEncodingFilter.selectEncoding(request));
		PrintWriter out = response.getWriter();
		out.println("<html><head><LINK rel='stylesheet' href='/admin/css/style.css'></head><body>");
		out.println("<h3>"+prop.getText("admin.offline.generating_html_please_wait")+"</h3>");
		out.flush();
		for (int i=0; i<100; i++)
		{
			out.println("<!-- generating --------------------------->");
			out.flush();
		}

		//ziskaj si zoznam DocId
		List<DocDetails> documents = new ArrayList<>();
		StringBuilder searchGroups = null;
		GroupsDB groupsDB = GroupsDB.getInstance();
		int groupId = -1;
		if(request.getParameter("groupId")!=null) groupId = Tools.getIntValue(request.getParameter("groupId"), groupId);

//		najdi child grupy
		for (GroupDetails group : groupsDB.getGroupsTree(groupId, true, true))
		{
			if (group != null && group.isInternal()==false)
			{
				if (searchGroups == null)
				{
					searchGroups = new StringBuilder(Integer.toString(group.getGroupId()));
				}
				else
				{
					searchGroups.append("," + group.getGroupId());
				}
			}
		}
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String query = "";
		if (groupId == -1 || searchGroups == null)
			query = "SELECT doc_id, title, virtual_path FROM documents WHERE available=? ORDER BY doc_id";
		else
			query = "SELECT doc_id, title, virtual_path FROM documents WHERE available=? AND group_id IN ("+ searchGroups.toString() +") ORDER BY doc_id";
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(query);
			ps.setBoolean(1, true);
			rs = ps.executeQuery();
			while (rs.next())
			{
				DocDetails doc = new DocDetails();
				doc.setDocId(rs.getInt("doc_id"));
				doc.setTitle(DB.getDbString(rs, "title"));
				doc.setVirtualPath(DB.getDbString(rs, "virtual_path"));
				documents.add(doc);
			}
			rs.close();
			ps.close();
			rs = null;
			ps = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (db_conn != null)
					db_conn.close();
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		//debug
		/*documents = new ArrayList();
		doc = new DocDetails();
		doc.setDocId(4);
		doc.setTitle("main");
		documents.add(doc);*/

		String destination = "/html";

		if(request.getParameter("destination")!=null) destination = request.getParameter("destination");

		if(Tools.isEmpty(destination)){
			destination = "/html";
		}
		else
		{
			destination = destination.trim();
			if(destination.charAt(0) != '/')
				destination = "/"+destination;
		}

		HttpURLConnection.setFollowRedirects(false);

		String data;
		File f;
		OutputStreamWriter osw;

		f = new File(Tools.getRealPath(destination));

		if (Tools.isNotEmpty(PathFilter.getCustomPath()))
		{
			f = new File(PathFilter.getCustomPath() + File.separatorChar + Constants.getInstallName() + File.separatorChar + destination.substring(1));
		}

		if (f.exists())
		{
			//vymaz obsah
			File[] files = f.listFiles();
			int i;
			for (i=0; i<files.length; i++)
			{
				if(files[i].delete() == false)
				   Logger.error(OfflineAction.class, "Unable to delete file: "+files[i].getName());
			}
		}
		else
		{
			if(f.mkdirs() == false)
			{
				out.println("Unable to cretate directory: "+PathFilter.getCustomPath() + File.separatorChar + Constants.getInstallName() + File.separatorChar + "html");
				out.println("<br />Archive creation stopped");
				return;
			}
		}

		String basePath = Tools.getBaseHrefLoopback(request);
		int flushCounter = 0;
		String fileName;

		downloadedUrls = new Hashtable<>();
		needDownloadUrlsList = new ArrayList<>();
		allreadyHasInDownloadList = new Hashtable<>();

		TemplatesDB templatesDB = null;
		DocDB docDB = null;
		TemplateDetails td = null;
		String pageFcieFileName = null;
		File pageFcieFile = null;
		String pageFcieData = null;

		//#11325: vypinam spam protection
   		if(ConfDB.setName("disableSpamProtectionForOffline", "true") && ClusterDB.isServerRunningInClusterMode()) {
   			ClusterDB.addRefresh("sk.iway.iwcm.system.ConfDB-disableSpamProtectionForOffline");
		}

		int counter = 0;
		int size = documents.size();
		for (DocDetails doc : documents)
		{
			counter++;

			//if (doc.getDocId() > 100) continue;

			out.println("["+counter+"/"+size+"] id="+ doc.getDocId() + " title=" + doc.getTitle());
			Logger.println(OfflineAction.class,"["+counter+"/"+size+"] id="+ doc.getDocId() + " title=" + doc.getTitle());

			try
			{
				if (Tools.isEmpty(doc.getVirtualPath()))
				{
					data = downloadUrl(basePath + "/showdoc.do?docid="+doc.getDocId(), request);
				}
				else
				{
					data = downloadUrl(basePath + doc.getVirtualPath(), request);
				}

				if (Tools.isNotEmpty(data))
				{
					//uprav cesty
					data = fixPath(data, basePath);

					/*
					 * /components/_common/javascript/page_functions.js.jsp?language=LNG stiahnem ako page_functions_LNG.js do destination a fixnem linky
					 * ticket #11321
					 */
					if(data.indexOf("/components/_common/javascript/page_functions.js.jsp?language=") != -1)
					{
						if(templatesDB == null) templatesDB = TemplatesDB.getInstance();
						if(docDB == null) docDB = DocDB.getInstance();
						td = templatesDB.getTemplate(docDB.getBasicDocDetails(doc.getDocId(), true).getTempId());
						if(td != null && td.getTempId() > 0)
						{
							pageFcieFileName = "page_functions_"+td.getLng()+".js";
							pageFcieFile = new File(Tools.getRealPath(destination + "/" + pageFcieFileName)); //NOSONAR
							if (Tools.isNotEmpty(PathFilter.getCustomPath()))
								pageFcieFile = new File(PathFilter.getCustomPath() + File.separatorChar + Constants.getInstallName() + File.separatorChar + destination.substring(1) + File.separatorChar + pageFcieFile.getName());

							if(pageFcieFile.exists() == false)
							{
								pageFcieData = downloadUrl(basePath + "/components/_common/javascript/page_functions.js.jsp?language="+td.getLng(), request);
								osw = new OutputStreamWriter(new FileOutputStream(pageFcieFile), SetCharacterEncodingFilter.getEncoding());
								osw.write(pageFcieData);
								osw.close();
							}

							data = Tools.replace(data, "../components/_common/javascript/page_functions.js.jsp?language="+td.getLng(), pageFcieFileName);
							data = Tools.replace(data, "/components/_common/javascript/page_functions.js.jsp?language="+td.getLng(), pageFcieFileName);
						}
					}

					//uloz stranku
					fileName = getFileName("showdoc.do?docid="+doc.getDocId());
					f = new File(Tools.getRealPath(destination + "/" + fileName)); //NOSONAR
					if (Tools.isNotEmpty(PathFilter.getCustomPath()))
					{
						f = new File(PathFilter.getCustomPath() + File.separatorChar + Constants.getInstallName() + File.separatorChar + destination.substring(1) + File.separatorChar + f.getName());
					}

					osw = new OutputStreamWriter(new FileOutputStream(f), SetCharacterEncodingFilter.getEncoding());
					osw.write(data);
					osw.close();

					out.println(" [OK]");

					flushCounter++;
					if (flushCounter%10==0)
					{
						out.println("<script language='javascript'>window.scrollBy(0,1000);</script>");
						out.println("<!-- generating --------------------------->");
						out.println("<!-- generating --------------------------->");
						out.println("<!-- generating --------------------------->");
						out.println("<!-- generating --------------------------->");
						out.flush();
					}

					downloadedUrls.put("/showdoc.do?docid="+doc.getDocId(), destination + "/" + fileName);
					if (Tools.isNotEmpty(doc.getVirtualPath()))
					{
						downloadedUrls.put(doc.getVirtualPath(), destination + "/" + fileName);
					}
				}
			}
			catch (Exception ex2)
			{
				out.println("CHYBA: "+ex2.getMessage());
			}

			out.println("<br>");
		}

		//if (1==1) return(null);

		//nacitaj specialne veci, ktore tam zostali (komponenty)
		String link;
        String newLink;
		LabelValueDetails lvd;
		int i=0;
		while (i < needDownloadUrlsList.size())
		{
			lvd = needDownloadUrlsList.get(i++);
			try
			{
				link = lvd.getLabel();
				if (link.indexOf("rnd=")!=-1)
				{
					//linka ma nahodny parameter, preskoc, lebo sa nam to zacykli
					continue;
				}
				if (link.indexOf("__fp=")!=-1)
				{
					//linka na nejaky stripes form, preskoc
					continue;
				}
				if (link.indexOf("?d-")!=-1 && (link.indexOf("-e=")!=-1 || link.indexOf("-p=")!=-1))
				{
					//linka na nejaky displaytag form, preskoc
					continue;
				}
				if (link.indexOf("forceBrowserDetector=")!=-1)
				{
					//linka na inu vizualnu podobu, preskoc
					continue;
				}
				if (downloadedUrls.get(link)==null && link.startsWith("/admin")==false)
				{
					//este to nie je stiahnute, treba stiahnut
					newLink = lvd.getValue();
					out.println("<br/>NEED ["+i+"/"+needDownloadUrlsList.size()+"]: " + link + " -> " + newLink);
					if (link.startsWith("../")) link = link.substring(2);
					if (!link.startsWith("/")) link = "/" + link; //NOSONAR

					if (link.startsWith("/#") || link.startsWith("/files") || link.startsWith("/images") || link.startsWith("/jscripts") ||
						 link.endsWith(".doc") || link.endsWith(".xls") || link.endsWith(".ppt") || link.endsWith(".pdf"))
					{
						downloadedUrls.put(link, link);
						continue;
					}

					//if (1==1) continue;

					//	otestuj, ci to nie je existujuci subor
					f = new File(Tools.getRealPath(link));
					if (Tools.isNotEmpty(PathFilter.getCustomPath()))
					{
						f = new File(PathFilter.getCustomPath() + File.separatorChar + Constants.getInstallName() + File.separatorChar + link.replace('/', File.separatorChar));
					}
					out.println("checking: " + f.getAbsolutePath());
					if (f.exists())
					{
						downloadedUrls.put(link, newLink);
						out.println(" [EXISTING FILE OK]<br>");
						continue;
					}


					data = downloadUrl(basePath + link, request);
					if (Tools.isNotEmpty(data))
					{
						//	uprav cesty
						data = fixPath(data, basePath);

						//odstran nepotrebny HTML kod
						data = cropData(data);

						f = new File(Tools.getRealPath( destination +"/" + newLink)); //NOSONAR
						if (Tools.isNotEmpty(PathFilter.getCustomPath()))
						{
							f = new File(PathFilter.getCustomPath() + File.separatorChar + Constants.getInstallName() + File.separatorChar + destination.substring(1) + File.separatorChar + f.getName());
						}

						osw = new OutputStreamWriter(new FileOutputStream(f), SetCharacterEncodingFilter.getEncoding());
						osw.write(data);
						osw.close();

						out.println(" [OK]");

						downloadedUrls.put(link, newLink);
					}
					else
					{
						out.println("--> data is empty: " + link);
					}
					out.println("<br>");
				}
				else
				{
					//out.println("UZ MAM: " + link + "<br>");
				}
			}
			catch (Exception ex)
			{
				out.println("<span class='error'>"+ex.getMessage()+"</span>");
				sk.iway.iwcm.Logger.error(ex);
			}
		}

		//#11325: zapinam spam protection
		ConfDB.deleteName("disableSpamProtectionForOffline");
		Constants.deleteConstant("disableSpamProtectionForOffline");

		Constants.getServletContext().removeAttribute(Constants.USER_KEY);

		//ak treba, vygeneruj not_available_on_cd.html
		f = new File(Tools.getRealPath(destination +"/not_available_on_cd.html"));
		if (Tools.isNotEmpty(PathFilter.getCustomPath()))
		{
			f = new File(PathFilter.getCustomPath() + File.separatorChar + Constants.getInstallName() + File.separatorChar + destination.substring(1)+ "/not_available_on_cd.html");
		}

		if (f.exists()==false)
		{
			data = "<html><body>"+prop.getText("components.offline.notAvailableOnCD")+"</body></html>";
			osw = new OutputStreamWriter(new FileOutputStream(f), SetCharacterEncodingFilter.getEncoding());
			osw.write(data);
			osw.close();
		}

		//	ak treba, vygeneruj blank.html
		f = new File(Tools.getRealPath( destination+"/blank.html"));
		if (Tools.isNotEmpty(PathFilter.getCustomPath()))
		{
			f = new File(PathFilter.getCustomPath() + File.separatorChar + Constants.getInstallName() + File.separatorChar + destination.substring(1) + "/blank.html");
		}

		if (f.exists()==false)
		{
			data = "<html><body></body></html>";
			osw = new OutputStreamWriter(new FileOutputStream(f), SetCharacterEncodingFilter.getEncoding());
			osw.write(data);
			osw.close();
		}

		out.println("<hr>" +prop.getText("admin.offline.generate_done")+ "<br><br>");

		String archiveDirs = request.getParameter("archiveDirs");
		String makeZipArchive = request.getParameter("makeZipArchive");
		if (Tools.isNotEmpty(archiveDirs) && Tools.isNotEmpty(makeZipArchive) && "yes".equals(makeZipArchive))
		{
			makeZipArchive(Constants.getServletContext(), out, archiveDirs, basePath);
		}

		out.println("</body></html>");

		HttpURLConnection.setFollowRedirects(true);
   }

	/**
	 * Vrati nazov suboru, pod ktorym bude stranka na disku
	 * @param url
	 * @return
	 */
	private String getFileName(String url)
	{
		String testUrl = url;
		if (testUrl.startsWith("../"))
		{
			testUrl = testUrl.substring(2);
		}
		if (testUrl.startsWith("/")==false)
		{
			testUrl = "/"+testUrl;
		}

		if (testUrl.startsWith("/files/") || testUrl.startsWith("/images/"))
		{
			//je to linka na subor, alebo obrazok
			return(".."+testUrl);
		}

		//otestuj, ci to nie je existujuci subor
		File f = new File(Tools.getRealPath(testUrl));
		if (Tools.isNotEmpty(PathFilter.getCustomPath()))
		{
			f = new File(PathFilter.getCustomPath() + File.separatorChar + Constants.getInstallName() + File.separatorChar + testUrl.replace('/', File.separatorChar));
		}

		Logger.println(OfflineAction.class,"test: " + testUrl + " file="+f.getAbsolutePath() + " ex="+f.exists());

		if (f.exists())
		{
			return(".."+testUrl);
		}

		int index = url.lastIndexOf('/');
		if (index != -1)
		{
			url = url.substring(index+1);
		}

		try
		{
			url = URLDecoder.decode(url, SetCharacterEncodingFilter.getEncoding());
		}
		catch (UnsupportedEncodingException ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		//odstran vsetky zle znaky
		url = DocTools.removeChars(url);

		url = Tools.replace(url, "showdoc.do_docid=", "showdoc_");

		url = DB.internationalToEnglish(url.toLowerCase());

		if (url.indexOf("rnd=")!=-1)
		{
			//stranka s nahodnym parametrom
			url = url.substring(0, url.indexOf("rnd="));
		}

		url = url + ".html";

		return(url);
	}

	/**
	 * Opravi cesty k obrazkom, suborom, inym strankam...
	 * @param data
	 * @return
	 */
	private String fixPath(String data, String baseHref)
	{
		//odstran google analytics
		data = Tools.replace(data, "src=\"http://www.google-analytics.com/urchin.js", "");
		data = Tools.replace(data, "src='http://www.google-analytics.com/urchin.js", "");
		data = Tools.replace(data, "urchinTracker();", "");

		//obrazky
		data = Tools.replace(data, "src='/", "src='../");
		data = Tools.replace(data, "src=\"/", "src=\"../");
		data = Tools.replace(data, "src=\"images", "src=\"../images");
		data = Tools.replace(data, "src='images", "src='../images");
		data = Tools.replace(data, "url(images/", "url(../images/");
		data = Tools.replace(data, "url(/images/", "url(../images/");
		//ak je tam nejaka JS funkcia, tam to treba spravit tiez
		data = Tools.replace(data, "'images/", "'../images/");
		//linka vo Flashi
		data = Tools.replace(data, "VALUE=\"/images/", "VALUE=\"../images/");
		data = Tools.replace(data, "value=\"/images/", "value=\"../images/");
		data = Tools.replace(data, "value='/images/", "value='../images/");
		data = Tools.replace(data, "VALUE=\"images/", "VALUE=\"../images/");
		data = Tools.replace(data, "value=\"flash/", "value=\"../flash/");
		data = Tools.replace(data, "value=\"/flash/", "value=\"../flash/");
		data = Tools.replace(data, "data=\"/images/", "data=\"../images/");
		data = Tools.replace(data, "data='/images/", "data='../images/");

		//flash
		data = Tools.replace(data, "<PARAM NAME=\"Movie\" VALUE=\"/", "<PARAM NAME=\"Movie\" VALUE=\"../");
		data = Tools.replace(data, "<PARAM NAME=\"Src\" VALUE=\"/", "<PARAM NAME=\"Src\" VALUE=\"../");
		data = Tools.replace(data, "/components/_common/preload.swf?path=/", "../");

		//subory
		data = Tools.replace(data, "href=\"/files", "href=\"../files");
		data = Tools.replace(data, "href='/files", "href='../files");

		//javascript
		data = Tools.replace(data, "src=\"jscripts/", "src=\"../jscripts/");
		data = Tools.replace(data, "src='jscripts/", "src='../jscripts/");

		//css
		data = Tools.replace(data, "href='/css", "href='../css");
		data = Tools.replace(data, "href=\"/css", "href=\"../css");
		data = Tools.replace(data, "href=\"css", "href=\"../css");
		data = Tools.replace(data, "href='css", "href='../css");
		data = Tools.replace(data, "@import /css", "@import ../css");
		data = Tools.replace(data, "@import '/css", "@import '../css");
		data = Tools.replace(data, "@import \"/css", "@import \"../css");

		//odkazy
		//Logger.println(OfflineAction.class,"--> ");

		ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());

		// set tidy parameters
		try {
			Document doc = Jsoup.parse(is, SetCharacterEncodingFilter.getEncoding(), baseHref);
			data = extractLinks(doc, data);
		} catch (IOException e) {
			Logger.error(e);
		}

	   //uprav submity formularov
	   data = Tools.replace(data, "action='showdoc.do'", "action='not_available_on_cd.html'");
	   data = Tools.replace(data, "action='/showdoc.do'", "action='not_available_on_cd.html'");
	   data = Tools.replace(data, "action=\"showdoc.do\"", "action=\"not_available_on_cd.html\"");
	   data = Tools.replace(data, "action=\"/showdoc.do\"", "action=\"not_available_on_cd.html\"");

	   //https redirecty v JS
	   data = Tools.replace(data, "if (window.location.href.indexOf(\"http", "if (false && window.location.href.indexOf(\"http");
	   data = Tools.replace(data, "if (window.location.href.indexOf('http", "if (false && window.location.href.indexOf('http");

		return(data);
	}

	/**
	 * Odstrani z HTML kodu vsetko medzi CROP_START a CROP_END
	 * @param data
	 * @return
	 */
	private String cropData(String data)
	{
		int failsafe = 0;
		int start = data.indexOf(CROP_START);
		int end = data.indexOf(CROP_END);

		while (start>0 && end > start && failsafe++<50)
		{
			try
			{
				data = data.substring(0, start) + data.substring(end+CROP_END.length());

				start = data.indexOf(CROP_START, start+1);
				end = data.indexOf(CROP_END, end+1);
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}

		return data;
	}

	@SuppressWarnings("deprecation")
	private String extractLinks(Document doc, String data)
	{
		try
		{
			Elements links = doc.select("a[href]"); // a with href
			for (Element e : links) {
				String link = e.attributes().get("href");

				if (link!=null && Tools.isNotEmpty(link) && link.toLowerCase().startsWith("http")==false && link.toLowerCase().startsWith("mailto")==false)
				{
					Logger.debug(OfflineAction.class, "Extracting link: " + link);

					//	odstran zahradku
					if (link.indexOf('#')>1)
					{
						link = link.substring(0, link.indexOf('#'));
					}

					boolean isLinkDirect = false;
					//otestuj, ci to nie je linka na nejaku skratenu cestu
					DocDB docDB = DocDB.getInstance();

					int docId = docDB.getVirtualPathDocId(link);
					if (docId > 0)
					{
						String newLink = getFileName("showdoc.do?docid="+docId);
						Logger.debug(OfflineAction.class, "Replacing link: link="+link+" newLink="+newLink);
						data = replaceLink(data, link, newLink);
						if (downloadedUrls.containsKey(link)==false)
						{
							//needDownloadUrlsList.add(new LabelValueDetails(link, newLink));

							//je to linka na normalne docId, to nemusim stahovat, stiahne sa automaticky (ako vsetky ostatne docid)
							downloadedUrls.put(link, newLink);
							Logger.println(OfflineAction.class,"link DIRECT: " + link + " new:"+newLink);
						}
						isLinkDirect = true;
					}

					if (isLinkDirect == false)
					{
						//ziskaj docid
						int start = link.indexOf("docid=");
						if (start > 0)
						{
							String tmp = link.substring(start);
							StringTokenizer st = new StringTokenizer(tmp, "=&'\", #");
							if (st.countTokens()<2)
							{
								Logger.println(OfflineAction.class,"zla linka: " + tmp);
							}
							else
							{
								//preskocime docid=
								st.nextToken();
								docId = Tools.getIntValue(st.nextToken(), -1);
								if (docId > 0)
								{
									if (link.startsWith("javascript:"))
									{
										Logger.println(OfflineAction.class,"som JS");
										if (link.indexOf("wjPopup")!=-1)
										{
											Logger.println(OfflineAction.class,"som wjPopup");
											//ok, je to popup, treba to preparsovat a ziskat len linku
											start = link.indexOf("/showdoc.do");
											int end = link.indexOf("'", start+2);

											Logger.println(OfflineAction.class,"start="+start+" end="+end);

											if (start > 0 && end > start)
											{
												link = link.substring(start, end);
												String newLink = getFileName(link);
												Logger.println(OfflineAction.class,"link="+link+" newLink="+newLink);
												data = replaceLink(data, link, newLink);
											}
										}
									}
									else
									{
										String newLink = getFileName(link);
										data = replaceLink(data, link, newLink);
										if (downloadedUrls.get(link)==null)
										{
											addToDownloadList(link, newLink);
											//needDownloadUrlsList.add(new LabelValueDetails(link, newLink));
											Logger.println(OfflineAction.class,"link: " + link + " new:"+newLink);
											if (link.indexOf("2670400")!=-1)
											{
												Logger.println(OfflineAction.class,"===== MAM ====");
												Logger.println(OfflineAction.class,link + "->" + newLink);
												Logger.println(OfflineAction.class,"==============");
											}
										}
									}
								}
							}

						}
						else if (link.toLowerCase().startsWith("javascript")==false && "/".equals(link)==false && link.startsWith("www.")==false)
						{
							//ak to nie je ani javascript ani / (lebo sa potom nahradza </html> ani www.nieco

							//pridaj do zoznamu na download
							String newLink = getFileName(link);
							data = replaceLink(data, link, newLink);
							if (downloadedUrls.get(link)==null)
							{
								addToDownloadList(link, newLink);
								//needDownloadUrlsList.add(new LabelValueDetails(link, newLink));
								Logger.println(OfflineAction.class,"link DIRECT: " + link + " new:"+newLink);
							}
						}
					}
				}
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return(data);
	}

	/**
	 * Ak uz stranka nie je v zozname na download, prida ju do zoznamu
	 * @param link - linka
	 * @param newLink - nove URL stranky
	 */
	private void addToDownloadList(String link, String newLink)
	{
		if (allreadyHasInDownloadList.get(link)==null && link.startsWith("/admin/")==false)
		{
			needDownloadUrlsList.add(new LabelValueDetails(link, newLink));
			allreadyHasInDownloadList.put(link, newLink);
		}
	}

	private String replaceLink(String src, String oldStr, String newStr)
	{
		//pridaj na download
		if (downloadedUrls.get(oldStr)==null)
		{
			addToDownloadList(oldStr, newStr);
			//needDownloadUrlsList.add(new LabelValueDetails(oldStr, newStr));
		}

		if (src == null)
		{
			return (null);
		}
		if (src.indexOf(oldStr) == -1)
		{
			return (src);
		}
		StringBuilder result = new StringBuilder(src.length() + 50);
		int startIndex = 0;
		int endIndex = src.indexOf(oldStr);
		while (endIndex != -1)
		{
			if ((src.charAt(endIndex+oldStr.length())=='\'' ||
				 src.charAt(endIndex+oldStr.length())=='"' ||
				 src.charAt(endIndex+oldStr.length())==' ' ||
				 src.charAt(endIndex+oldStr.length())=='#') &&
				 (src.charAt(endIndex-1)=='\'' ||
				 src.charAt(endIndex-1)=='"' ||
				 src.charAt(endIndex-1)==' ' ||
				 src.charAt(endIndex-1)=='#'))
			{
				//replacujeme len kompletne linky, preto kontrolujeme koniec na '"_
				result.append(src.substring(startIndex, endIndex));
				result.append(newStr);
				startIndex = endIndex + oldStr.length();
				endIndex = src.indexOf(oldStr, startIndex);
			}
			else
			{
				//preskakujeme
				endIndex = src.indexOf(oldStr, endIndex+1);
			}
		}
		result.append(src.substring(startIndex, src.length()));
		return result.toString();
	}



	/**
	 * Vytvori ZIP-archiv Webroot adresara, ak su
	 * @param servletContext
	 * @param mainDirs - korenove adresare, ktore sa maju zalohovat
	 * @param printWriter
	 */
	private void makeZipArchive(ServletContext servletContext, PrintWriter printWriter, String mainDirs, String basePath)
	{
		if ((","+mainDirs+",").indexOf(",html,")==-1)
		{
			//html adresar sa musi archivovat, naviac na zaciatku neexistuje, takze ho tam
			//pouzivatel ani nevie zadat
			mainDirs = mainDirs + ",html";
		}

		printWriter.println("<b>Archiving...</b><br><br>");
		Logger.println(OfflineAction.class,"Archiving..."+mainDirs);
		double time;
		String outFilename;
		String zipFilePath;
		String[] archiveDirs = {"css", "files", "images", "jscripts","html"};
		File f;
		String zipDirPath;

	   try
		{

			f = new File(Tools.getRealPath("/"));
			zipDirPath = Tools.getRealPath("/");

			/*if (Tools.isNotEmpty(PathFilter.getCustomPath()))
			{
				f = new File(PathFilter.getCustomPath() + File.separatorChar + Constants.getInstallName() + File.separatorChar);
				zipDirPath = PathFilter.getCustomPath() + File.separatorChar + Constants.getInstallName() + File.separatorChar;
			}*/

			//Logger.println(OfflineAction.class,"zipDirPath: "+zipDirPath);

	   	SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.getString("dateFormat"));
	   	SimpleDateFormat timeFormat = new SimpleDateFormat(Constants.getString("timeFormat"));
	   	long dateTime = Tools.getNow();
	   	//poskladam nazov suboru
	   	String fileName = "offline-" +Constants.getInstallName() +"-"+ dateFormat.format(new Timestamp(dateTime)) +"-"+ timeFormat.format(new Timestamp(dateTime)) +".zip";

	   	//z nazvu suboru vyhodim zakazane znaky
	   	fileName = DocTools.removeChars(fileName);
	   //	if (!zipDirPath.startsWith("/"))  zipDirPath = "/" + zipDirPath;
	   //	if (!zipDirPath.endsWith("/"))  zipDirPath = zipDirPath + "/";

	   	//poskladam cestu archivu
	   	zipFilePath = zipDirPath + fileName;

	   	//Logger.println(OfflineAction.class,"zipFilePath: "+zipFilePath);

	   	//vytvorime si ZIP file
	      //outFilename = Tools.getRealPath(zipFilePath);
	   	outFilename = zipFilePath;
	      ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(outFilename));

	       //zmeriame cas operacie
	       time = Tools.getNow();

	      if (Tools.isNotEmpty(mainDirs))
	   	{
	   		//rozparsujeme si adresare, ktore sa budu archivovat
		   	archiveDirs = RelatedPagesDB.getTokens(mainDirs, ",");
	   	}

	       //iterujeme po adresaroch a pridavame subory do ZIP-archivu
	       for (int i=0; i<archiveDirs.length; i++)
	       {
	       	getFilesFromDir(Tools.getRealPath("/"+archiveDirs[i]), basePath, servletContext, zipOut, printWriter);
	       }

	      //pridavam index.html pre offline verziu
         String data;

         	fileName = "index-offline.htm";
				data = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Frameset//EN\">"+
						"<HTML><HEAD><TITLE>WebJet</TITLE>"+
						"<META http-equiv=Content-Type content=\"text/html; charset=windows-1250\"></HEAD>"+
						"<FRAMESET frameSpacing=0 frameBorder=0 cols=0,*><FRAME src=\"html/blank.htm\" noResize>"+
						"<FRAME name=RightMain src=\"html/" +getFileName("showdoc.do?docid=4")+ "\" scrolling=yes>"+
						"<NOFRAMES><a href='html/" +getFileName("showdoc.do?docid=4")+ "'>WebJet</a>"+
						"</NOFRAMES></FRAMESET></HTML>";

				//uloz stranku
				f = new File(Tools.getRealPath("/" + fileName)); //NOSONAR
				if (Tools.isNotEmpty(PathFilter.getCustomPath()))
				{
					f = new File(PathFilter.getCustomPath() + File.separatorChar + Constants.getInstallName() + File.separatorChar + f.getName());
				}

				 //Add ZIP entry to output stream.
				 zipOut.putNextEntry(new ZipEntry(f.getAbsolutePath()));
		     	 zipOut.write(data.getBytes());

	          // Complete the entry
	          zipOut.closeEntry();


			//pridavam blank.html pre offline verziu
			data = "<html><body></body></html>";
				fileName = "blank.htm";
				f = new File(Tools.getRealPath("/html/" + fileName));
				if (Tools.isNotEmpty(PathFilter.getCustomPath()))
				{
					f = new File(PathFilter.getCustomPath() + File.separatorChar + Constants.getInstallName() + File.separatorChar+ "html" + File.separatorChar + f.getName());
				}
				 //Add ZIP entry to output stream.
				 zipOut.putNextEntry(new ZipEntry(f.getAbsolutePath()));
		     	 zipOut.write(data.getBytes());

	          // Complete the entry
	          zipOut.closeEntry();

	       // uzavretie ZIP suboru
	       zipOut.close();
	       time = (Tools.getNow() - time)/1000D;
	       printWriter.println("<br><b>Done!<br>Time left: "+time+" sec</b>");
	       printWriter.println("<br>Archive filename: <b>"+fileName+"</b>");
	   }
	   catch (Exception e)
		{
	   	sk.iway.iwcm.Logger.error(e);
		}
	}


	private void getFilesFromDir(String mainDir, String basePath, ServletContext servletContext, ZipOutputStream zipOut, PrintWriter printWriter)
	{
		byte[] buf = new byte[BUFFER];
		FileInputStream in;
		int scrollIndex = 0;

		if (mainDir.endsWith("/"))
		{
			mainDir = mainDir.substring(0, mainDir.length()-1);
		}
		//Logger.println(OfflineAction.class,"mainDir: "+mainDir);

		String realPath = mainDir;
		if (realPath != null)
		{
			File file = new File(realPath);
			int size;
			int i;

			try
			{
				Logger.println(OfflineAction.class,"file name: "+file.getAbsolutePath());
				if (file.isDirectory())
				{
					File[] files = file.listFiles();
					size = files.length;
					for (i=0; i<size; i++)
					{
						file = files[i];

						if (file.isDirectory())
						{
							if (!file.getName().equalsIgnoreCase("cvs"))
							{
								getFilesFromDir(mainDir+"/"+file.getName(), basePath, servletContext, zipOut, printWriter);
							}
						}
						else
						{
							if (file.getName().toLowerCase().indexOf(".cvsignore") == -1 && !file.getName().toLowerCase().endsWith(".zip"))
							{
								if (printWriter != null)
								{
									printWriter.println("Adding file: "+file.getName()+"<br>");
									if (scrollIndex >= 15)
									{
										printWriter.println("<script language='javascript'>window.scrollBy(0,1000);</script>");
										scrollIndex = 0;
									}
									printWriter.flush();
									scrollIndex++;
								}

								if (file.getName().toLowerCase().indexOf(".css") != -1)
								{
									StringBuilder sb = new StringBuilder();
									String data = null;
									in = new FileInputStream(file);
									InputStreamReader inStr = new InputStreamReader(in, Constants.FILE_ENCODING);

									char[] buffer = new char[8000];
									int n = 0;
									while (true)
									{
										 n = inStr.read(buffer);
										 if (n < 1) break;
										 sb.append(buffer, 0, n);
									}
									in.close();
									inStr.close();
									data = sb.toString();

									//uprav cesty
									data = fixPath(data, basePath);

									//Add ZIP entry to output stream.
									zipOut.putNextEntry(new ZipEntry(file.getAbsolutePath()));

					            // Transfer bytes from the file to the ZIP file
									zipOut.write(data.getBytes());

								}
								else
								{
									in = new FileInputStream(realPath + File.separatorChar + file.getName());

									//Add ZIP entry to output stream.
									zipOut.putNextEntry(new ZipEntry(file.getAbsolutePath()));

					            // Transfer bytes from the file to the ZIP file
					            int len;
					            while ((len = in.read(buf)) > 0)
					            {
					            	zipOut.write(buf, 0, len);
					            }
								}
				            // Complete the entry
				            zipOut.closeEntry();
				            in.close();
							}
						}
					}
				}
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}


	}

	/**
	 * Stiahnutie stranky s vyuzitim Jakarta HTTP Client (zvlada POST aj GET)
	 * nemoze sa pouzit nastroj z Tools, tu su customizovane hlavicky
	 * @param basePath
	 * @param req
	 * @return
	 */
	public static String downloadUrl(String basePath, HttpServletRequest req)
	{
		//aby nas neodhlasilo
		if (basePath.contains("/logoff.do")) return "";

		String data = null;

		HttpClient client = HttpClientBuilder.create().setSSLContext(Tools.doNotVerifyCertificates(null)).setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).build();

		Logger.println(OfflineAction.class,basePath);
		String name;
        String value;

	   HttpGet method = new HttpGet(basePath);

      Enumeration<String> e = req.getHeaderNames();
		while (e.hasMoreElements())
		{
			name = e.nextElement();
			value = req.getHeader(name);
			if ("content-length".equalsIgnoreCase(name) || "content-type".equalsIgnoreCase(name) || "accept-encoding".equals(name))
			{
				//skip this headers they are from the original POST request
				continue;
			}

			if ("host".equalsIgnoreCase(name))
			{
				//value = "www2.ing.cz";
				try
				{
					URL url = new URL(basePath);
					value = url.getHost();
				}
				catch (Exception ex)
				{

				}
			}
			if ("User-Agent".equalsIgnoreCase(name))
			{
				value = value + "; WebJET Offline"; //NOSONAR
			}
			method.setHeader(name, value);
			Logger.println(OfflineAction.class,name+": "+value);
		}
		method.setHeader("userInServletContext", "true");
		//nastav dmail header, aby sa negeneroval inline editor
		method.setHeader("dmail", "1");

		try {
			HttpResponse response = client.execute(method);

			// write out the response headers
			Logger.println(OfflineAction.class,"*** Response ***");
			Logger.println(OfflineAction.class,"Status Line: " + response.getStatusLine());
			Header[] responseHeaders = response.getAllHeaders();
			Header h;
			String location = null;
			for (int i=0; i<responseHeaders.length; i++)
			{
				h = responseHeaders[i];
				//Logger.print(this,responseHeaders[i]);
				if (h.getName()!=null && h.getValue() != null)
				{
					Logger.println(OfflineAction.class,h.getName()+": "+h.getValue());
					if (h.getName().equalsIgnoreCase("Location"))
					{
						location = h.getValue();
					}
					//res.setHeader(h.getName(), h.getValue());
				}
			}

			if (location == null)
			{
				try
				{
					StringBuilder sb = new StringBuilder();
					BufferedInputStream is = new BufferedInputStream(response.getEntity().getContent());
					InputStreamReader in = new InputStreamReader(is, SetCharacterEncodingFilter.getEncoding());
					char[] buffer = new char[8000];
					int n = 0;
					while (true)
					{
						 n = in.read(buffer);
						 if (n < 1) break;
						 sb.append(buffer, 0, n);
					}
					in.close();
					data = sb.toString();
				}
				catch (IOException ex)
				{
					sk.iway.iwcm.Logger.error(ex);
				}
			}
			else
			{
				try
				{
					//	uprav location
					String baseHref = Tools.getBaseHrefLoopback(req);
					if (location.startsWith(baseHref))
					{
						location = location.substring(location.indexOf('/', 9));
					}
					int start = location.indexOf(";jsessionid");
					if (start !=-1)
					{
						int end = location.indexOf('?', start);
						if (end > start)
						{
							location = location.substring(0, start) + location.substring(end);
						}
						else
						{
							location = location.substring(0, start);
						}
					}
					data = "<html><body><a href='"+location+"'>"+location+"</a><script language='JavaScript'>window.location.href='"+location+"';</script></body></html>";
				}
				catch (Exception ex)
				{
					data = "<html><body><a href='"+location+"'>"+location+"</a><script language='JavaScript'>window.location.href='"+location+"';</script></body></html>";
				}
			}
		} catch (Exception ex) {
			Logger.error(OfflineAction.class, ex);
		}


		//clean up the connection resources
		method.releaseConnection();
		//method.recycle();

		//Logger.println(OfflineAction.class,data);

		return(data);
	}
}
