package sk.iway.iwcm.update;

import java.beans.XMLDecoder;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.web.multipart.MultipartFile;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.editor.rest.WebPagesListener;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.stripes.SyncDirAction;
import sk.iway.iwcm.sync.WarningListener;
import sk.iway.iwcm.system.ModuleInfo;
import sk.iway.iwcm.system.Modules;
import sk.iway.iwcm.system.spring.SpringUrlMapping;
import sk.iway.iwcm.system.zip.ZipEntry;
import sk.iway.iwcm.system.zip.ZipInputStream;
import sk.iway.iwcm.tags.support.ResponseUtils;
import sk.iway.iwcm.users.UsersDB;

public class UpdateService {

	private static final String[] COMPONENTS_DIRS = {"/components/", "/apps/"};
	private static final String RESFRESHER = "/admin/update/update_refresher";

	/************************************************** GET VERSIONS LOGIC SECTION *******************************************************************/

	@SuppressWarnings("unchecked")
    public static List<VersionBean> getUpdateVersionsData(HttpServletRequest request) {
        try {
            //HttpServletRequest request = event.getSource().getRequest();
            Identity user = UsersDB.getCurrentUser(request);
            String lng = PageLng.getUserLng(request);

            if (user.isEnabledItem("modUpdate")) {
                List<VersionBean> versions = (List<VersionBean>) downloadObject("type=VersionList", lng);
				if (versions != null) {
					Logger.debug(UpdateService.class,"Mam versions: " + versions.size());

                    return versions;
				}
            }
        }  catch (Exception ex) {
            Logger.error(WebPagesListener.class, ex);
        }

        return (new ArrayList<VersionBean>());
    }

	/************************************************** UPDATE LOGIC SECTION *******************************************************************/

	public static String prepareUpdateFile(MultipartFile uploadFile) throws IOException, ServletException {
		if (Tools.isNotEmpty(uploadFile.getName())) {
			String version = DocTools.removeChars("upload-" + Tools.formatDateTime(Tools.getNow()));
			String versionDir = version;

			FileTools.copyFile(uploadFile.getInputStream(), new IwcmFile(Tools.getRealPath("/WEB-INF/update/" + versionDir + "/archive.dat")));

			return version;
		}

		return null;
	}

	public static void prepareUpdate(String version, HttpServletRequest request, HttpServletResponse response, boolean viaFile) throws IOException, ServletException {
		if(Tools.isEmpty(version)) return;

		String inputVersion;
		String inputVersionDir;

		if(viaFile) {
			//Check version location name formatt
			if(!version.startsWith("upload-")) return;

			inputVersion = version;
			inputVersionDir = version;
		} else {
			inputVersion = version;
			inputVersionDir = version + "-" + Tools.formatDate(Tools.getNow());
		}

		prepareUpdateLogic(inputVersion, inputVersionDir, request, response);
	}

	@SuppressWarnings("unchecked")
	private static void prepareUpdateLogic(String version, String versionDir, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		//Check's first
		if(request == null) {
			SpringUrlMapping.redirectToLogon(response);
			return;
		}

		HttpSession session = request.getSession();
		if (session == null) {
			SpringUrlMapping.redirectToLogon(response);
			return;
		}

		Identity user = (Identity) session.getAttribute(Constants.USER_KEY);
		if (!(user != null && user.isAdmin())) {
			SpringUrlMapping.redirectToLogon(response);
			return;
		}

		Prop prop = Prop.getInstance(request);
		String lng = Prop.getLng(request, false);

		response.setContentType("text/html; charset=windows-1250");
		PrintWriter out = response.getWriter();
		out.println("<html><head><LINK rel='stylesheet' href='"+request.getContextPath()+"/admin/css/style.css'><META http-equiv='Content-Type' content='text/html; charset=windows-1250'></head><body>");
		out.println("<h3>"+prop.getText("update.preparing_update_files")+"</h3>");

		out.flush();
		for (int i = 0; i < 100; i++) {
			out.println("<!-- generating --------------------------->");
			out.flush();
		}

		//stiahni zoznam verzii
		Map<String, String> modules = null;
		try {
			modules = (Map<String, String>)downloadObject("type=ModList", lng);
			if (modules != null) modules.put("cmp__common", "true");
		}
		catch (Exception ex) {
			//padlo stahovanie
		}

		if (modules == null) {
			modules = new Hashtable<>();
			//sme offline, modules pouzijeme podla aktualnej verzie
			Modules mod = Modules.getInstance();
			List<ModuleInfo> modList = mod.getAvailableModules();
			for (ModuleInfo m : modList) {
				modules.put(m.getItemKey(), "true");
				//lebo cmp_dmail ma kluc menuEmail
                String path = m.getLeftMenuLink();
                if (m.getItemKey().startsWith("cmp_") == false && path != null) {
					for (String dir : COMPONENTS_DIRS) {
						if (path.startsWith(dir)) {
							int i = dir.length();
							int j = path.indexOf("/", i+1);
							if (j > i) {
								String name = "cmp_"+path.substring(i, j);
								modules.put(name, "true");
							}
						}
					}
                }
			}
		}

		//pridaj meno instalacie ktore mame nastavene
		modules.put("cmp_" + Constants.getInstallName(), "true");
		if (Tools.isNotEmpty(Constants.getLogInstallName())) modules.put("cmp_"+Constants.getLogInstallName(), "true");

		//out.println("<!-- modlist size: "+modules.size()+" -->");

		out.println("<div id='msgDiv'>" + prop.getText("update.downloading_files_please_wait") + "</div>");
		out.println("<script type='text/javascript' src='/admin/update/timerbar.jsp'></script>");

		//stiahni archiv zo servera
		boolean ok = downloadArchiveFile(version, versionDir, out);
		if (ok) {
			out.println("<script type='text/javascript'>printMessage('msgDiv', '"+prop.getText("update.backing_old_files")+"');</script>");
			updateProgressBar(0, out);
		} else {
			out.println(" <span style='color: red'>Error: can't download archive</span>");
			out.println("</body></html>");
			out.flush();
			return;
		}
		out.println("<br>");

		String archiveRealPath = Tools.getRealPath("/WEB-INF/update/" + versionDir + "/archive.dat");
		File archiveFile = new File(archiveRealPath);

		// Read in the bytes
        int offset = 0;
        int numRead = 0;

		int updateFilesCount = 0;
		try {
			File outFile;
	   		FileOutputStream fos;

	   		InputStream fis = new FileInputStream(archiveFile);

	   		//rozbal si zip subor a prepisuj jednotlive subory
	   		ZipInputStream zis = new ZipInputStream(fis);
	   		ZipEntry entry;
	   		String fileName;
	   		File localFile;
	   		boolean fileEquals;

	   		// ziskaj pocet zaznamov
			int total = 0;
			while ((entry = zis.getNextEntry()) != null) { total ++; }
			zis.close();

			Logger.debug(UpdateService.class,"total: " + total);

			fis.close();
			fis = new FileInputStream(archiveFile);
			zis = new ZipInputStream(fis);

			int lastPercentage = 0;
			int actualPercentage = 0;
			int counter = 0;

			while ((entry = zis.getNextEntry()) != null) {
				fileName = entry.getName();
				if (!fileName.startsWith("/"))
					fileName = "/" + fileName; //NOSONAR

				actualPercentage = (int) ( ((double)counter++ / (double)total) * 100 );
				if (actualPercentage!=lastPercentage) {
					updateProgressBar(actualPercentage, out);
					lastPercentage = actualPercentage;
				}

				if (fileName.indexOf("/CVS/")!=-1 ||
					 fileName.equals(".cvsignore") ||
					 fileName.equals("poolman.xml") ||
					 fileName.equals("web.xml") ||
					 fileName.indexOf("/admin/FCKeditor/editor/_source")==0)
				{
					//je to CVS adresar, preskakujeme
					continue;
				}

				if (fileName.indexOf("/admin/spec/") == 0) {
					//je to adresar a nie je to pre nas
					if (fileName.lastIndexOf('/') > 14 && fileName.indexOf("/admin/spec/" + Constants.getInstallName() + "/") != 0)
						continue;
				}

				//Logger.println(this,"zip: dir=" + entry.isDirectory()+" name="+fileName);
				localFile = new File(Tools.getRealPath(fileName));

				if (entry.isDirectory()) {
					//toto netreba, adresar sa vytvori pri vytvoreni suboru
					/*outFile = new File(servlet.getServletContext().getRealPath("/WEB-INF/update/"+versionDir + fileName));
					if (outFile.exists()==false)
					{
						Logger.println(this,"Vytvaram adresar");
						outFile.mkdirs();
					}*/
				} else {
					//kontroluj prava na komponenty
					boolean isDisabled = false;
					for (String componentDir : COMPONENTS_DIRS) {
						if (fileName.toLowerCase().indexOf(componentDir) == 0) {
							//ziskaj nazov komponenty
							int start = fileName.indexOf(componentDir);
							if (start != -1) {
								start = start + componentDir.length();
								int end = fileName.indexOf("/", start + 1);
								if (end > start) {
									String componentName = fileName.substring(start, end);
									//Logger.println(this,"CMP: " + componentName + " f:" + fileName);

									//skontroluj prava
									if (isModuleAvailable(componentName, modules)==false) {
										//tato komponenta nie je povolena
										Logger.debug(UpdateService.class,"--> DISABLED component: " + componentName + " f:" + fileName);
										isDisabled = true;
										break;
									}
								}
							}
						}
					}

					if (isDisabled) continue;

					//nacitak ZIP entry do pola
					byte[] zipBuff = new byte[(int)entry.getSize()];

					//nacitaj subor 1 do buffera
					offset = 0;
					numRead = 0;
					while (offset < zipBuff.length && (numRead=zis.read(zipBuff, offset, zipBuff.length - offset)) >= 0)
					{
					   offset += numRead;
					}

					if (localFile.exists()) {
						//skontroluj ci sa zmenil (porovnaj obsah)
						fileEquals = fileContentEquals(localFile, zipBuff);

						//Ak su rovnake, nema zmysel to prepisovat
						if (fileEquals) continue;

						//backup subor
					   	outFile = new File(Tools.getRealPath("/WEB-INF/update/" + versionDir + "_backup" + fileName));
					   	if (outFile.getParentFile().exists() == false)
							outFile.getParentFile().mkdirs();

						//vytvor zalohu suboru
						FileTools.copyFile(localFile, outFile);
					}

					//	zapis subor na disk
					outFile = new File(Constants.getServletContext().getRealPath("/WEB-INF/update/" + versionDir + fileName));
					if (outFile.getParentFile().exists() == false)
						outFile.getParentFile().mkdirs();

					fos = new FileOutputStream(outFile);
					fos.write(zipBuff);
					fos.close();
					updateFilesCount++;
				}
			}
			updateProgressBar(100, out);
			zis.close();
			fis.close();
		}
		catch (Exception ex) {
			sk.iway.iwcm.Logger.error(ex);
			out.println(" <span style='color: red'>Error: " + ex.getMessage() + "</span>");
			out.println("</body></html>");
			out.flush();
			return;
		}

		//vymaz archiv
   		File toDel = new File(archiveRealPath);
   		if(!toDel.delete())
   			Logger.error(UpdateService.class, "Unable to delete file/dir: " + archiveRealPath);

   		out.println("<br><br>" + prop.getText("update.update_files_count") + ": " + updateFilesCount);
   		out.println("<br><br><a href='/admin/update/restart?version=" + ResponseUtils.filter(versionDir) + "'>" + prop.getText("update.restart") + "</a>");
		out.println("</body></html>");
		out.flush();

		return;
	}

	private static boolean isModuleAvailable(String directoryName, Map<String, String> modules) {
		if (modules == null) return true;

		String moduleKey = directoryName;

		//allowed modules
		if ("universal_component".equals(moduleKey)) return true;
		if ("cta".equals(moduleKey)) return true;
		if ("webjet9".equals(moduleKey)) return true;
		if ("admin".equals(moduleKey)) return true;

		//fix renames in /apps folder
		if ("enumeration".equals(moduleKey)) moduleKey = "enumerations";
		if ("export-dat".equals(moduleKey)) moduleKey = "export";

		if ("dmail".equals(moduleKey)) moduleKey = "menuEmail";
		if ("forum".equals(moduleKey)) moduleKey = "diskusia";
		if ("gallery".equals(moduleKey)) moduleKey = "menuGallery";
		if ("gdpr".equals(moduleKey)) moduleKey = "menuGdpr";
		if ("import_web_pages".equals(moduleKey)) moduleKey = "importWebPages";
		if ("inquiry".equals(moduleKey)) moduleKey = "menuInquiry";
		if ("messages".equals(moduleKey)) moduleKey = "menuMessages";
		if ("qa".equals(moduleKey)) moduleKey = "menuQa";
		if ("sharing_icons".equals(moduleKey)) moduleKey = "sharedIcons";

		if ("eshop".equals(moduleKey)) moduleKey = "basket";
		if ("news-calendar".equals(moduleKey)) moduleKey = "calendar";

		//test if module is available
		if (modules.get("cmp_"+directoryName) != null || modules.get("cmp_"+Tools.replace(directoryName, "-", "_")) != null ||
			modules.get("cmp_"+moduleKey) != null || modules.get("cmp_"+Tools.replace(moduleKey, "-", "_")) != null ||
			modules.get(moduleKey) != null) return true;
		return false;
	}

	/**
	 * Stiahne remote subor a ulozi ho na disk
	 * @return
	*/
	private static boolean downloadArchiveFile(String version, String versionDir, PrintWriter out) {
		boolean ret = false;

		try {
			File outFile = new File(Tools.getRealPath("/WEB-INF/update/"+versionDir+"/archive.dat"));
			File parentDir = outFile.getParentFile();
			if (!parentDir.exists()) {
				Logger.println(UpdateService.class,"Vytvaram adresare:" + parentDir.getAbsolutePath());
				if(parentDir.mkdirs() == false) return false;
			}

			if (outFile.exists()) return true;

			URLConnection conn = null;

			String updateRemoteServer = Constants.getString("updateRemoteServer");
			updateRemoteServer += "/getObject.do?type=GetArchive&licenseId="+InitServlet.getLicenseId()+"&version="+version+"&JAR="+Constants.getBoolean("enableJspJarPackaging");

			URL urlCon = new URL(updateRemoteServer);
			conn = urlCon.openConnection();
			conn.setAllowUserInteraction(false);
			conn.setDoInput(true);
			conn.setDoOutput(false);
			conn.connect();

			int contentLength = conn.getContentLength();
			int totalRead = 0;
			int lastPercentage = 0;
			int actualPercentage = 0;

			BufferedInputStream is = new BufferedInputStream(conn.getInputStream());
			FileOutputStream fos = new FileOutputStream(outFile);
			byte[] buffer = new byte[640000];
			int n = 0;
			while (true) {
				n = is.read(buffer);

				//	progress bar na stranke
				totalRead+=n;
				actualPercentage = (int) ( ((double)totalRead++ / (double)contentLength) * 100 );
				if (actualPercentage!=lastPercentage) {
					updateProgressBar(actualPercentage, out);
					lastPercentage = actualPercentage;
				}

				Logger.debug(UpdateService.class,"UpdateService write dat file: " + n + " " + actualPercentage+"%");
				if (n < 1) break;
				fos.write(buffer, 0, n);
			}
			fos.close();
			is.close();

			//nastav suboru datum
			//zmena: nechame najnovsi subor, aby to tomcat spoznal a skompiloval
			//outFile.setLastModified(fb.getLastModified());

			ret = true;
		}
		catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
		}

		return ret;
	}

	/**
	 * Zvysenie hodnoty progress baru
	 * @param p
	 * @param out
	 */
	public static void updateProgressBar(int p, PrintWriter out) {
		if (out != null) {
			out.println("<script language='JavaScript'>setCount("+p+");</script>");
			out.println("                                                                         ");
			out.println("                                                                         ");
			out.println("                                                                         ");
			out.flush();
			out.println("                                                                         ");
			out.println("                                                                         ");
			out.println("                                                                         ");
			out.println("                                                                         ");
			out.flush();
			out.println("                                                                         ");
			out.println("                                                                         ");
			out.println("                                                                         ");
			out.flush();
			out.println("                                                                         ");
			out.println("                                                                         ");
			out.println("                                                                         ");
			out.flush();
			out.println("                                                                         ");
			out.println("                                                                         ");
			out.println("                                                                         ");
			out.flush();
			out.println("                                                                         ");
			out.println("                                                                         ");
			out.println("                                                                         ");
			out.flush();
			out.println("                                                                         ");
			out.println("                                                                         ");
			out.println("                                                                         ");
			out.flush();
			out.println("                                                                         ");
			out.println("                                                                         ");
			out.println("                                                                         ");
			out.flush();
			out.println("                                                                         ");
			out.println("                                                                         ");
			out.println("                                                                         ");
			out.flush();
			out.println("                                                                         ");
			out.println("                                                                         ");
			out.println("                                                                         ");
			out.flush();
			out.println("                                                                         ");
			out.println("                                                                         ");
			out.println("                                                                         ");
			out.flush();
			out.println("                                                                         ");
			out.println("                                                                         ");
			out.println("                                                                         ");
			out.flush();
		}
	}

	/**
	 * Porovna obsah 2 suborov
	 * @param f1
	 * @param b2
	 * @return - true ak je ich obsah rovnaky, inak false
	 */
	public static boolean fileContentEquals(File f1, byte[] b2) {
		if (f1 == null || f1.exists() == false)
			return false;

		long f1Size = f1.length();

		if (f1Size != b2.length)
			return false;

		//WEB-INF/lib musi zostat cele, inak sa nebude vediet co sa ma vymazat
		if (f1.getAbsolutePath().indexOf("WEB-INF" + File.separatorChar + "lib") != -1)
			return false;

		try {
			byte[] f1Buff = new byte[(int)f1Size];
			//nacitaj subor 2 a porovnavaj
			FileInputStream fis = new FileInputStream(f1);
			int i;
			int offset = 0;
			int numRead;
			while (offset < f1Buff.length && (numRead=fis.read(f1Buff, offset, f1Buff.length-offset)) >= 0) {
				//porovnaj buffre
				for (i = 0; i < numRead; i++) {
					if (f1Buff[offset + i] != b2[offset + i]){
						Logger.debug(UpdateService.class,"Nasiel som rozdiel na pozicii: " + (offset+i));
						fis.close();
						return false;
					}
				}
			   offset += numRead;
			}
			fis.close();
		} catch (Exception ex) {
			sk.iway.iwcm.Logger.error(ex);
			return false;
		}
		return true;
	}

	/************************************************** RESTART LOGIC SECTION *******************************************************************/

	public static String doRestart(String version, HttpServletRequest request, HttpServletResponse response) throws IOException{
      	Logger.println(UpdateService.class, "RESTART version=" + version);

		Prop prop = Prop.getInstance(request);

		if (version == null) return RESFRESHER;

		response.setContentType("text/html; charset=windows-1250");
		PrintWriter out = response.getWriter();
		out.println("<html><head><meta http-equiv='refresh' content='10; url=update_refresher.jsp'><LINK rel='stylesheet' href='"+request.getContextPath()+"/admin/css/style.css'><META http-equiv='Content-Type' content='text/html; charset=windows-1250'></head><body>");
		out.println("<h3>"+prop.getText("update.restarting_system")+"</h3>");
		out.println(prop.getText("update.restarting_system_note"));
		out.println("");
		out.flush();
		for (int i = 0; i < 100; i++) {
			out.println("<!-- generating --------------------------->");
			out.flush();
		}

		//skopiruj web.xml na web-before_update.xml
		copyFile("/WEB-INF/web.xml", "/WEB-INF/web-before_update.xml");

		//vygeneruj novy web.xml
		@SuppressWarnings("java:S2479")
		String webXML = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\r\n" +
		   	"<web-app xmlns=\"http://java.sun.com/xml/ns/j2ee\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd\" version=\"2.4\">\r\n" +
		   	"  <servlet>\r\n" +
		   	"      <servlet-name>ResponseServlet</servlet-name>\r\n" +
		   	"      <servlet-class>sk.updater.ResponseServlet</servlet-class>\r\n" +
		   	"  </servlet>\r\n" +
		   	"  <servlet>\r\n" +
		   	"     <servlet-name>updaterinit</servlet-name>\r\n" +
		   	"     <servlet-class>sk.updater.InitServlet</servlet-class>\r\n" +
				"		<init-param>\r\n" +
				"       <param-name>version</param-name>\r\n" +
				"       <param-value>"+version+"</param-value>\r\n" +
				"     </init-param>\r\n" +
				"		<init-param>\r\n" +
				"       <param-name>installName</param-name>\r\n" +
				"       <param-value>"+Constants.getInstallName()+"</param-value>\r\n" +
				"     </init-param>\r\n" +
		   	"     <load-on-startup>1</load-on-startup>\r\n" +
		   	"  </servlet>\r\n" +
		   	"  <!-- Standard Action Servlet Mapping -->\r\n" +
		   	"  <servlet-mapping>\r\n" +
		   	"    <servlet-name>ResponseServlet</servlet-name>\r\n" +
		   	"    <url-pattern>/nejakastrasnahaluskalebototorobitomcatuproblem/*</url-pattern>\r\n" +
		   	"  </servlet-mapping>\r\n" +
		   	"</web-app>";

		//zapis sk.updater.* (ak je nove)
		String updaterDir = "/WEB-INF/update/" + version + "/WEB-INF/classes/sk/updater/";
		File dir = new File(Tools.getRealPath(updaterDir));
		if (dir.exists() && dir.isDirectory()) {
			File[] files = dir.listFiles();
			int size = files.length;
			int i;
			for (i = 0; i < size; i++) {
				out.println("Kopirujem: " + files[i].getName());
				copyFile(updaterDir+files[i].getName(), "/WEB-INF/classes/sk/updater/"+files[i].getName());
			}
		}

		//zapis nove web.xml
		FileOutputStream fos = new FileOutputStream(Tools.getRealPath("/WEB-INF/web.xml"));
		fos.write(webXML.getBytes());
		fos.close();

		out.println("</body></html>");
		out.flush();

		UpdateService.restart();

		return null;
	}

	public static boolean restart() {
		boolean ret = true;
		touch("/WEB-INF/classes/sk/iway/iwcm/InitServlet.class");
		touch("/WEB-INF/web.xml");
		touch("/WEB-INF/classes/sk/updater/ResponseServlet.class");
		touch("/WEB-INF/classes/sk/updater/InitServlet.class");

		touch("/WEB-INF/lib/webjet-8.7-SNAPSHOT.jar");
		touch("/WEB-INF/lib/webjet-8.8-SNAPSHOT.jar");
		touch("/WEB-INF/lib/webjet-8.9-SNAPSHOT.jar");
		touch("/WEB-INF/lib/webjet-2021.0-SNAPSHOT.jar");
		touch("/WEB-INF/lib/webjet-2022.0-SNAPSHOT.jar");
		touch("/WEB-INF/lib/webjet-2023.0-SNAPSHOT.jar");
		touch("/WEB-INF/lib/webjet-2024.0-SNAPSHOT.jar");
		touch("/WEB-INF/lib/webjet-2025.0-SNAPSHOT.jar");

		touch("/WEB-INF/lib/wjstripes-1.6.0-SNAPSHOT.jar");
		touch("/WEB-INF/lib/wjcron4j-2.2.5-SNAPSHOT.jar");
		touch("/WEB-INF/lib/wjdisplaytag-1.2-SNAPSHOT.jar");

		Logger.println(UpdateService.class,"RESTART request ret="+ret);
		Logger.error(UpdateService.class,"RESTART request ret="+ret);

		return(ret);
	}

	private static boolean touch(String url) {
		boolean ret = false;
		File f = new File(Tools.getRealPath(url));
		if (f.exists()) {
			Logger.println(UpdateService.class,"RESTART request InitServlet " + f.getAbsolutePath());
			f.setLastModified(Tools.getNow()); //NOSONAR
			ret = true;
		}

		return ret;
	}

	/**
	 * Skopiruje subor
	 * @param fromPath - zdrojova adresa (URL)
	 * @param toPath - cielova adresa (URL)
	 * @return
	 */
	private static boolean copyFile(String fromPath, String toPath) {
		Logger.println(UpdateService.class,"   Copy file: " + fromPath + " -> " + toPath);

		boolean copyied = false;

		try {
			File fromFile = new File(Tools.getRealPath(fromPath));
			File toFile = new File(Tools.getRealPath(toPath));
			File toFileParent = toFile.getParentFile();
			if(toFileParent.exists() == false) {
				if(toFileParent.mkdirs() == false) {
					Logger.println(UpdateService.class, " [FAIL] can't create parent dir");
					return false;
				}
			}

			if(toFile.exists() == false && toFile.createNewFile() == false) {
				Logger.println(UpdateService.class, " [FAIL] can't create new file");
				return false;
			}
			FileInputStream inStream = new FileInputStream(fromFile);
			FileOutputStream out = new FileOutputStream(toFile);

			int c;
			byte[] buff = new byte[150000];
			while ((c = inStream.read(buff)) != -1)
				out.write(buff, 0, c);

			out.close();
			inStream.close();

			copyied = true;
		} catch (Exception ex) {
			sk.iway.iwcm.Logger.error(ex);
		}

		if (copyied)
		   Logger.println(UpdateService.class," [OK]");
		else
			Logger.println(UpdateService.class," [FAIL]");

		return(copyied);
	}

	/************************************************** Shared logic SECTION *******************************************************************/

	/**
    /**
	 * Stiahne a dekoduje objekt zo vzdialeneho servera
	 * @param params
	 * @return
	 */
	@SuppressWarnings("resource")
	private static Object downloadObject(String params, String lng) {
		String updateRemoteServer = Constants.getString("updateRemoteServer");
		updateRemoteServer += "/getObject.do?licenseId=" + InitServlet.getLicenseId() + "&";
		Logger.debug(UpdateService.class,"SYNC: downloadObject url="+updateRemoteServer+params);

		try {
			String data = Tools.downloadUrl(updateRemoteServer + params + "&lng=" + lng + "&UTF8=true&JAR="+Constants.getBoolean("enableJspJarPackaging"), "utf-8");
			data = Tools.replace(data, "sk.iway.updater.", "sk.iway.iwcm.update.");

			InputStream bais = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
			bais = SyncDirAction.checkXmlForAttack(bais);
			XMLDecoder decoder = new XMLDecoder(bais, null, new WarningListener());
			Object obj = decoder.readObject();
			decoder.close();
			bais.close();

			return obj;
		}
		catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
		}

		return null;
	}
}
