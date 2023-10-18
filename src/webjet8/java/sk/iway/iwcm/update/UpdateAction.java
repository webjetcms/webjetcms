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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.ResponseUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.JarPackaging;
import sk.iway.iwcm.stripes.SyncDirAction;
import sk.iway.iwcm.sync.WarningListener;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.system.ModuleInfo;
import sk.iway.iwcm.system.Modules;
import sk.iway.iwcm.system.zip.ZipEntry;
import sk.iway.iwcm.system.zip.ZipInputStream;
import sk.iway.iwcm.xls.ImportXLSForm;

/**
 *  UpdateAction.java - vyber a stiahnutie suborov na aktualizaciu systemu
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: jeeff $
 *@version      $Revision: 1.14 $
 *@created      $Date: 2009/01/15 10:09:57 $
 *@modified     $Date: 2009/01/15 10:09:57 $
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class UpdateAction extends Action
{
	//ID licencie, nastavi to sem InitServlet
	private static int licenseId = -1;
	private static final String COMPONENTS_DIR="/components/";

	@Override
	public ActionForward execute(ActionMapping mapping,
	         ActionForm form,
	         HttpServletRequest request,
	         HttpServletResponse response)
	          throws IOException, ServletException
   {
		HttpSession session = request.getSession();
   	if (session == null)
		{
			return (mapping.findForward("logon_admin"));
		}
		Identity user = (Identity) session.getAttribute(Constants.USER_KEY);
		if (!(user != null && user.isAdmin()))
		{
			return (mapping.findForward("logon_admin"));
		}

      Prop prop = Prop.getInstance(servlet.getServletContext(), request);
      String lng = PageLng.getUserLng(request);

      String action = request.getParameter("action");

      //List errors = new ArrayList();
      String forward = "refresh";

      Logger.debug(this,"UpdateAction: action="+action);

      if (action == null)
      {
      	//Nacitaj zoznam z remoteServera a vypis co sa zmenilo
      	forward = "success";

			if (JarPackaging.isJarPackaging("/admin"))
			{
				request.setAttribute("isJarPackaging", "true");
			}
			else
			{
				List versions = (List)downloadObject("type=VersionList", lng);
				if (versions != null)
				{
					Logger.debug(this,"Mam versions: " + versions.size());
					if (versions.size()>0)
					{
						request.setAttribute("updateArrayListVersions", versions);
					}
					else
					{
						request.setAttribute("versionsEmpty", "true");
					}
				}
			}
      }
      else if ("GetArchive".equals(action))
      {
      	String version = request.getParameter("version");
			String versionDir = version + "-"+Tools.formatDate(Tools.getNow());

      	if (version == null)
      	{
      		return (mapping.findForward("refresh"));
      	}

      	if ("upload".equals(version) && form instanceof ImportXLSForm)
      	{
      		ImportXLSForm xlsForm = (ImportXLSForm)form;
      		if (Tools.isNotEmpty(xlsForm.getFile().getFileName()))
      		{
      			version = DocTools.removeChars("upload-"+Tools.formatDateTime(Tools.getNow()));
					versionDir = version;
      			FileTools.copyFile(xlsForm.getFile(), new File(Tools.getRealPath("/WEB-INF/update/"+versionDir+"/archive.dat")));
      			xlsForm.getFile().destroy();
      		}
      	}

			response.setContentType("text/html; charset=windows-1250");
			PrintWriter out = response.getWriter();
			out.println("<html><head><LINK rel='stylesheet' href='"+request.getContextPath()+"/admin/css/style.css'><META http-equiv='Content-Type' content='text/html; charset=windows-1250'></head><body>");
			out.println("<h3>"+prop.getText("update.preparing_update_files")+"</h3>");

			out.flush();
			for (int i=0; i<100; i++)
			{
				out.println("<!-- generating --------------------------->");
				out.flush();
			}

			//stiahni zoznam verzii
			Map<String, String> modules = null;
			try
			{
				modules = (Map<String, String>)downloadObject("type=ModList", lng);
				if (modules != null) modules.put("cmp__common", "true");
			}
			catch (Exception ex)
			{
				//padlo stahovanie
			}

			if (modules == null) {
				modules = new Hashtable<>();
				//sme offline, modules pouzijeme podla aktualnej verzie
				Modules mod = Modules.getInstance();
				List<ModuleInfo> modList = mod.getAvailableModules();
				for (ModuleInfo m : modList)
				{
					modules.put(m.getItemKey(), "true");
					//lebo cmp_dmail ma kluc menuEmail
                    String path = m.getLeftMenuLink();
                    if (m.getItemKey().startsWith("cmp_")==false && path != null && path.startsWith("/components/"))
                    {
                        int i = "/components/".length();
                        int j = path.indexOf("/", i+1);
                        if (j>i)
                        {
                            String name = "cmp_"+path.substring(i, j);
                            modules.put(name, "true");
                        }
                    }
				}
			}

			//pridaj meno instalacie ktore mame nastavene
			modules.put("cmp_"+Constants.getInstallName(), "true");
			if (Tools.isNotEmpty(Constants.getLogInstallName())) modules.put("cmp_"+Constants.getLogInstallName(), "true");

			//out.println("<!-- modlist size: "+modules.size()+" -->");

			out.println("<div id='msgDiv'>"+prop.getText("update.downloading_files_please_wait")+"</div>");
			out.println("<SCRIPT LANGUAGE='JavaScript' src='/admin/scripts/timerbar.jsp'></SCRIPT>");

			//stiahni archiv zo servera
			boolean ok = downloadArchiveFile(version, versionDir, out);
			if (ok)
			{
				out.println("<script type='text/javascript'>printMessage('msgDiv', '"+prop.getText("update.backing_old_files")+"');</script>");
				updateProgressBar(0, out);
			}
			else
			{
				out.println(" <span style='color: red'>Error: can't download archive</span>");
				out.println("</body></html>");
				out.flush();
				return(null);
			}
			out.println("<br>");

			//nastav na 0 a zacni dekompresiu

   		String archiveRealPath = Tools.getRealPath("/WEB-INF/update/"+versionDir+"/archive.dat");
   		File archiveFile = new File(archiveRealPath);
   		byte[] fileBuff = new byte[(int)archiveFile.length()];

   		// Read in the bytes
         int offset = 0;
         int numRead = 0;
         InputStream fis = new FileInputStream(archiveFile);
         int maxBuffer = 64000;
         while (offset < fileBuff.length && (numRead=fis.read(fileBuff, offset, maxBuffer)) >= 0)
         {
         	Logger.debug(UpdateAction.class, "Reading ZIP into mem: "+offset);
             offset += numRead;
             if (maxBuffer > fileBuff.length-offset) maxBuffer = fileBuff.length-offset;
         }
         fis.close();
         fileBuff[0] = 'P';
         fileBuff[1] = 'K';

         // Ensure all the bytes have been read in
         if (offset < fileBuff.length)
         {
            out.println(" <span style='color: red'>Error: Could not completely read file</span>");
 				out.println("</body></html>");
 				out.flush();
 				return(null);
         }

         int updateFilesCount = 0;

			try
			{
				File outFile;
	   		FileOutputStream fos;

	   		ByteArrayInputStream bais = new ByteArrayInputStream(fileBuff);

	   		//rozbal si zip subor a prepisuj jednotlive subory
	   		ZipInputStream zis = new ZipInputStream(bais);
	   		ZipEntry entry;
	   		String fileName;
	   		File localFile;
	   		boolean fileEquals;

	   		// ziskaj pocet zaznamov
				int total = 0;
				while ((entry = zis.getNextEntry()) != null)
				{
					total ++;
				}
				zis.close();

				Logger.debug(this,"total: " + total);

				bais = new ByteArrayInputStream(fileBuff);
				zis = new ZipInputStream(bais);

				int lastPercentage = 0;
				int actualPercentage = 0;
				int counter = 0;

				while ((entry = zis.getNextEntry()) != null)
				{
					fileName = entry.getName();
					if (!fileName.startsWith("/"))
					{
						fileName = "/" + fileName; //NOSONAR
					}

					actualPercentage = (int) ( ((double)counter++ / (double)total) * 100 );
					if (actualPercentage!=lastPercentage)
					{
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

					if (fileName.indexOf("/admin/spec/")==0)
					{
						//je to adresar a nie je to pre nas
						if (fileName.lastIndexOf('/')>14 && fileName.indexOf("/admin/spec/"+Constants.getInstallName()+"/")!=0)
						{
							continue;
						}
					}

					//Logger.println(this,"zip: dir=" + entry.isDirectory()+" name="+fileName);
					localFile = new File(Tools.getRealPath(fileName));

					if (entry.isDirectory())
					{
						//toto netreba, adresar sa vytvori pri vytvoreni suboru
						/*outFile = new File(servlet.getServletContext().getRealPath("/WEB-INF/update/"+versionDir + fileName));
						if (outFile.exists()==false)
						{
							Logger.println(this,"Vytvaram adresar");
							outFile.mkdirs();
						}*/
					}
					else
					{
						//kontroluj prava na komponenty
						if (fileName.toLowerCase().indexOf(COMPONENTS_DIR)==0)
						{
							//ziskaj nazov komponenty
							int start = fileName.indexOf(COMPONENTS_DIR);
							if (start != -1)
							{
								start = start + COMPONENTS_DIR.length();
								int end = fileName.indexOf("/", start+1);
								if (end > start)
								{
									String componentName = fileName.substring(start, end);
									//Logger.println(this,"CMP: " + componentName + " f:" + fileName);

									//skontroluj prava
									if (modules.get("cmp_"+componentName)==null)
									{
										//tato komponenta nie je povolena
										Logger.debug(this,"--> DISABLED component: "+ componentName + " f:" + fileName);
										continue;
									}
								}
							}

						}


						//nacitak ZIP entry do pola
						byte[] zipBuff = new byte[(int)entry.getSize()];

						//nacitaj subor 1 do buffera
						offset = 0;
						numRead = 0;
						while (offset < zipBuff.length && (numRead=zis.read(zipBuff, offset, zipBuff.length-offset)) >= 0)
						{
						   offset += numRead;
						}

						if (localFile.exists())
						{
							//skontroluj ci sa zmenil (porovnaj obsah)
							fileEquals = fileContentEquals(localFile, zipBuff);

							if (fileEquals)
							{
								//su rovnake, nema zmysel to prepisovat
								continue;
							}

							//backup subor
						   outFile = new File(Tools.getRealPath("/WEB-INF/update/"+versionDir+"_backup"+fileName));
						   if (outFile.getParentFile().exists()==false)
							{
								outFile.getParentFile().mkdirs();
							}

							//vytvor zalohu suboru
							FileTools.copyFile(localFile, outFile);
						}

						//	zapis subor na disk
						outFile = new File(servlet.getServletContext().getRealPath("/WEB-INF/update/"+versionDir + fileName));
						if (outFile.getParentFile().exists()==false)
						{
							outFile.getParentFile().mkdirs();
						}
						fos = new FileOutputStream(outFile);
						fos.write(zipBuff);
						fos.close();
						updateFilesCount++;
					}
				}
				updateProgressBar(100, out);
				zis.close();
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
				out.println(" <span style='color: red'>Error: "+ex.getMessage()+"</span>");
				out.println("</body></html>");
				out.flush();
				return(null);
			}

   		//vymaz archiv
   		File toDel = new File(archiveRealPath);
   		if(!toDel.delete())
   			Logger.error(UpdateAction.class, "Unable to delete file/dir: "+archiveRealPath);

   		out.println("<br><br>"+prop.getText("update.update_files_count")+": " + updateFilesCount);

   		out.println("<br><br><a href='update.do?action=restart&version="+ResponseUtils.filter(versionDir)+"'>"+prop.getText("update.restart")+"</a>");
			out.println("</body></html>");
			out.flush();
			return(null);
      }
      else if ("restart".equals(action))
      {
      	String version = request.getParameter("version");
      	Logger.println(this,"RESTART version="+version);

      	if (version == null)
      	{
      		return (mapping.findForward("refresh"));
      	}

      	response.setContentType("text/html; charset=windows-1250");
			PrintWriter out = response.getWriter();
			out.println("<html><head><meta http-equiv='refresh' content='10; url=update_refresher.jsp'><LINK rel='stylesheet' href='"+request.getContextPath()+"/admin/css/style.css'><META http-equiv='Content-Type' content='text/html; charset=windows-1250'></head><body>");
			out.println("<h3>"+prop.getText("update.restarting_system")+"</h3>");
			out.println(prop.getText("update.restarting_system_note"));
			out.println("");
			out.flush();
			for (int i=0; i<100; i++)
			{
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
      	String updaterDir = "/WEB-INF/update/"+version+"/WEB-INF/classes/sk/updater/";
      	File dir = new File(Tools.getRealPath(updaterDir));
      	if (dir.exists() && dir.isDirectory())
      	{
      		File[] files = dir.listFiles();
      		int size = files.length;
      		int i;
      		for (i=0; i<size; i++)
      		{
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

			UpdateAction.restart();

			return(null);
      }

      return (mapping.findForward(forward));
   }

	/**
	 * Stiahne a dekoduje objekt zo vzdialeneho servera
	 * @param params
	 * @return
	 */
	@SuppressWarnings("resource")
	private Object downloadObject(String params, String lng)
	{
		String updateRemoteServer = Constants.getString("updateRemoteServer");
		updateRemoteServer += "/getObject.do?licenseId="+UpdateAction.getLicenseId()+"&";
		Logger.debug(this,"SYNC: downloadObject url="+updateRemoteServer+params);

		try
		{
			String data = Tools.downloadUrl(updateRemoteServer+params+"&lng="+lng);
			data = Tools.replace(data, "sk.iway.updater.", "sk.iway.iwcm.update.");

			//Logger.println(this,"UpdateAction: downloadObject mam data: " + data);

			InputStream bais = new ByteArrayInputStream(data.getBytes());
			bais = SyncDirAction.checkXmlForAttack(bais);
			XMLDecoder decoder = new XMLDecoder(bais, null, new WarningListener());
			Object o = decoder.readObject();
			decoder.close();
			bais.close();

			return(o);
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}


		return(null);
	}

	/**
	 * Stiahne remote subor a ulozi ho na disk
	 * @return
	 */
	private static boolean downloadArchiveFile(String version, String versionDir, PrintWriter out)
	{
		boolean ret = false;

		try
		{

			File outFile = new File(Tools.getRealPath("/WEB-INF/update/"+versionDir+"/archive.dat"));
			File parentDir = outFile.getParentFile();
			if (!parentDir.exists())
			{
				Logger.println(UpdateAction.class,"Vytvaram adresare:" + parentDir.getAbsolutePath());
				if(parentDir.mkdirs() == false) return false;
			}

			if (outFile.exists())
			{
				return(true);
			}

			URLConnection conn = null;

			String updateRemoteServer = Constants.getString("updateRemoteServer");
			updateRemoteServer += "/getObject.do?type=GetArchive&licenseId="+UpdateAction.getLicenseId()+"&version="+version;

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
			while (true)
			{
				n = is.read(buffer);

				//	progress bar na stranke
				totalRead+=n;
				actualPercentage = (int) ( ((double)totalRead++ / (double)contentLength) * 100 );
				if (actualPercentage!=lastPercentage)
				{
					updateProgressBar(actualPercentage, out);
					lastPercentage = actualPercentage;
				}

				Logger.debug(UpdateAction.class,"UpdateAction write dat file: " + n + " " + actualPercentage+"%");
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
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		return(ret);
	}

	/**
	 * Skopiruje subor
	 * @param fromPath - zdrojova adresa (URL)
	 * @param toPath - cielova adresa (URL)
	 * @return
	 */
	private static boolean copyFile(String fromPath, String toPath)
	{
		Logger.println(UpdateAction.class,"   Copy file: " + fromPath + " -> " + toPath);

		boolean copyied = false;

		try
		{
			File fromFile = new File(Tools.getRealPath(fromPath));
			File toFile = new File(Tools.getRealPath(toPath));
			File toFileParent = toFile.getParentFile();
			if(toFileParent.exists()==false)
			{
				if(toFileParent.mkdirs() == false)
				{
					Logger.println(UpdateAction.class, " [FAIL] can't create parent dir");
					return false;
				}
			}
			if(toFile.exists() == false && toFile.createNewFile() == false)
			{
				Logger.println(UpdateAction.class, " [FAIL] can't create new file");
				return false;
			}
			FileInputStream inStream = new FileInputStream(fromFile);
			FileOutputStream out = new FileOutputStream(toFile);

			int c;
			byte[] buff = new byte[150000];
			while ((c = inStream.read(buff)) != -1)
			{
				out.write(buff, 0, c);
			}
			out.close();
			inStream.close();

			copyied = true;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		if (copyied)
		{
		   Logger.println(UpdateAction.class," [OK]");
		}
		else
		{
			Logger.println(UpdateAction.class," [FAIL]");
		}

		return(copyied);
	}

	/**
	 * Zvysenie hodnoty progress baru
	 * @param p
	 * @param out
	 */
	public static void updateProgressBar(int p, PrintWriter out)
	{
		if (out!=null)
		{
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
	 * Vrati ID licencie
	 * @return
	 */
	private static int getLicenseId()
	{
		if (licenseId == -1)
		{
			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try
			{
				db_conn = DBPool.getConnection();
				ps = db_conn.prepareStatement("SELECT * FROM "+ConfDB.CONF_TABLE_NAME+" WHERE name=?");
				ps.setString(1, "updateLicenseId");
				rs = ps.executeQuery();
				if (rs.next())
				{
					licenseId = Tools.getIntValue(DB.getDbString(rs, "value"), -1);
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
				}
			}
		}

		return(licenseId);
	}

	/**
	 * Nastavi ID licencie
	 * @param licenseId
	 */
	public static void setLicenseId(int licenseId)
	{
		if (UpdateAction.licenseId == -1)
		{
			UpdateAction.licenseId = licenseId;
		}
	}

	public static boolean restart()
	{
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

		Logger.println(UpdateAction.class,"RESTART request ret="+ret);
		Logger.error(UpdateAction.class,"RESTART request ret="+ret);

		return(ret);
	}

	private static boolean touch(String url) {
		boolean ret = false;
		File f = new File(Tools.getRealPath(url));
		if (f.exists())
		{
			Logger.println(UpdateAction.class,"RESTART request InitServlet " + f.getAbsolutePath());
			f.setLastModified(Tools.getNow()); //NOSONAR
			ret = true;
		}
		return ret;
	}

	/**
	 * Porovna obsah 2 suborov
	 * @param f1
	 * @param b2
	 * @return - true ak je ich obsah rovnaky, inak false
	 */
	public static boolean fileContentEquals(File f1, byte[] b2)
	{
		if (f1==null || f1.exists()==false)
		{
			return(false);
		}

		long f1Size = f1.length();

		if (f1Size != b2.length)
		{
			return(false);
		}

		//WEB-INF/lib musi zostat cele, inak sa nebude vediet co sa ma vymazat
		if (f1.getAbsolutePath().indexOf("WEB-INF"+File.separatorChar+"lib")!=-1)
		{
			return(false);
		}

		try
		{
			byte[] f1Buff = new byte[(int)f1Size];
			//nacitaj subor 2 a porovnavaj
			FileInputStream fis = new FileInputStream(f1);
			int i;
			int offset = 0;
			int numRead;
			while (offset < f1Buff.length && (numRead=fis.read(f1Buff, offset, f1Buff.length-offset)) >= 0)
			{
				//porovnaj buffre
				for (i=0; i<numRead; i++)
				{
					if (f1Buff[offset+i]!=b2[offset+i])
					{
						Logger.debug(UpdateAction.class,"Nasiel som rozdiel na pozicii: " + (offset+i));
						fis.close();
						return(false);
					}
				}

			   offset += numRead;
			}
			fis.close();
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
			return(false);
		}

		return(true);
	}
}
