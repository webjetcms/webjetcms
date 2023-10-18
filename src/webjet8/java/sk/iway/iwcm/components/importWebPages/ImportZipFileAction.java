package sk.iway.iwcm.components.importWebPages; //NOSONAR

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.upload.FormFile;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.FileIndexerTools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.editor.EditorForm;
import sk.iway.iwcm.findexer.Excel;
import sk.iway.iwcm.findexer.PDF;
import sk.iway.iwcm.findexer.Word;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmInputStream;
import sk.iway.iwcm.system.zip.ZipEntry;
import sk.iway.iwcm.system.zip.ZipInputStream;



/**
 *
 *  ImportZipFileAction.java - import web stranok zo ZIP archivu, importuju sa
 * HTML, DOC a PDF subory, vratane vytvorenia adresarovej struktury
 *
 *@Title        webjet5
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2005
 *@author       $Author: jeeff $
 *@version      $Revision: 1.12 $
 *@created      Date: 15.3.2005 11:06:40
 *@modified     $Date: 2009/09/10 19:59:12 $
 */
public class ImportZipFileAction extends Action
{

	/**
	 *  Description of the Method
	 *
	 *@param  mapping               Description of the Parameter
	 *@param  form                  Description of the Parameter
	 *@param  request               Description of the Parameter
	 *@param  response              Description of the Parameter
	 *@return                       Description of the Return Value
	 *@exception  IOException       Description of the Exception
	 *@exception  ServletException  Description of the Exception
	 */
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

		ImportZipFileForm my_form = (ImportZipFileForm) form;

		FormFile file = my_form.getUploadFile();
		int parentGroupId = Tools.getIntValue(my_form.getParentGroupIdString(), 0);
		boolean overwriteFiles = false;

		overwriteFiles = my_form.isOverwriteFiles();
		//Logger.debug(this,"overwriteFiles: "+overwriteFiles);
		//Logger.debug(this,"parentGroupId: "+parentGroupId);

		Map<String, String> urlReplaces = new Hashtable<>();

		if (file != null)
		{
			//retrieve the file name
			String fileName = file.getFileName().trim();
			//Logger.debug(this,"we have a file name="+fileName+" content type="+file.getContentType());

			if (fileName != null && fileName.length() > 1)
			{
				fileName = DB.internationalToEnglish(fileName);
				fileName = DocTools.removeChars(fileName);

				//BufferedInputStream buffReader = new BufferedInputStream(file.getInputStream());
				Prop prop = Prop.getInstance(servlet.getServletContext(), request);

				response.setContentType("text/html; charset=windows-1250");
				response.setHeader("Pragma","No-Cache");
			   response.setDateHeader("Expires",0);
			   response.setHeader("Cache-Control","no-Cache");

				PrintWriter out = response.getWriter();
				out.println("<html><head><LINK rel='stylesheet' href='"+request.getContextPath()+"/admin/css/style.css'><META http-equiv='Content-Type' content='text/html; charset=windows-1250'></head><body>");

				//vytvorim si temp adresar pre moj zips
				SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.getString("dateFormat"));
		   	SimpleDateFormat timeFormat = new SimpleDateFormat(Constants.getString("timeFormat"));
		   	long dateTime = Tools.getNow();
		   	//poskladam nazov suboru
		   	String dirName = "tmp_import-"+ dateFormat.format(new Timestamp(dateTime)) +"-"+ timeFormat.format(new Timestamp(dateTime));
		   	dirName = DocTools.removeCharsDir(dirName, false).toLowerCase();

				IwcmFile tempDir = new IwcmFile(Tools.getRealPath("/WEB-INF/tmp/" +dirName+ "/"));
		   	//tempDir = new File(Tools.getRealPath("/WEB-INF/tmp/tmp_import/"));
				if (tempDir.exists() && tempDir.isDirectory())
				{
					boolean delOK = FileTools.deleteDirTree(tempDir);
					Logger.debug(this,"Deleted dir: "+tempDir.getName()+ "   result: "+delOK);
				}

				Logger.debug(this,"Vytvaram adresar: " + tempDir.mkdirs());

				BufferedOutputStream dest = null;
				File outFile = null;

				FileOutputStream fos;

				final int BUFFER = 16*1024;

				String fNameLC = fileName.toLowerCase();

				if (fNameLC.endsWith(".zip"))
				{

					out.println("<h3>" +prop.getText("components.import_web_pages.importing_files")+ "...</h3><br><br>");

					out.flush();
					for (int i=0; i<100; i++)
					{
						out.println("<!-- generating --------------------------->");
						out.flush();
					}

					try
					{

						ZipInputStream zis = new ZipInputStream(file.getInputStream());
						ZipEntry entry;

						//ziskaj pocet zaznamov
						int total = 0;
						while ((entry = zis.getNextEntry()) != null)
						{
							Logger.debug(this,"POCITAM: " + total + " " + entry.getName());
							total ++;
						}

						int count;
						byte[] data = new byte[BUFFER];
						zis.close();
						zis = new ZipInputStream(file.getInputStream());
						String correctedName;

						while ((entry = zis.getNextEntry()) != null)
						{
							// write the files to the disk

							fNameLC = entry.getName().toLowerCase();

							Logger.debug(this,"ENTRY: " + entry.isDirectory() + " lc=" + fNameLC);

							//rozbalim zip-archiv do temp-adresara
							if (entry.isDirectory())
							{
								//vyhodim z nazvu adr. nepovolene znaky
								correctedName = DocTools.removeCharsDir(entry.getName());

								out.println("<b>" +prop.getText("components.import_web_pages.importing_dir")+ ":</b> " +correctedName+ "<br><br>");
								out.flush();
								outFile = new File(tempDir.getAbsolutePath()+ File.separatorChar + correctedName);
								outFile.mkdirs();
							}
							else
							{
								String dName = "";
								String fName = "";
								//vyhodim z nazvu suboru nepovolene znaky
								if (entry.getName().lastIndexOf('/') >= 0)
								{
									dName = entry.getName().substring(0, entry.getName().lastIndexOf('/'));
									dName = DocTools.removeCharsDir(dName);
									fName = entry.getName().substring(entry.getName().lastIndexOf('/')+1);
									fName = DocTools.removeChars(fName);
									correctedName = dName+ "/" +fName;
								}
								else
								{
									correctedName = DocTools.removeChars(entry.getName());
								}

								out.println("<b>" +prop.getText("components.import_web_pages.importing_file")+ ":</b> " +correctedName+ "<br>");
								out.flush();
								//outFile = new File(Tools.getRealPath("/WEB-INF/tmp_import_"+ entry.getName()));
								outFile = new File(tempDir.getAbsolutePath()+ File.separator + correctedName);

								urlReplaces.put("/"+entry.getName().replace('\\', '/'), "/"+correctedName.replace('\\', '/'));

								Logger.debug(this,"Extracting: " + entry + " to: " + outFile.getAbsolutePath());
								File parentDir = outFile.getParentFile();
								if (parentDir.exists()==false)
								{
									boolean status = parentDir.mkdirs();
									Logger.debug(ImportZipFileAction.class, "creating dir:"+parentDir.getAbsolutePath()+" status="+status);
								}
								fos = new FileOutputStream(outFile);
								dest = new BufferedOutputStream(fos, BUFFER);
								while ((count = zis.read(data, 0, BUFFER)) != -1)
								{
									dest.write(data, 0, count);
								}
								dest.flush();
								dest.close();
							}

							scrollWindow(out);

						}
						zis.close();
						zis = null;

						//naimportujeme subory do DB
						importFiles(tempDir, parentGroupId, request, out, prop, overwriteFiles, my_form.getImagesRootDir(), my_form.getFilesRootDir(), urlReplaces);

						//vymazeme tempDir z disku
						//delOK = FileTools.deleteDirTree(tempDir);
						//delOK = tempDir.delete();
					}
					catch (Exception e)
					{
						sk.iway.iwcm.Logger.error(e);
					}
				}
				else if (fNameLC.endsWith(".doc") || fNameLC.endsWith(".xls") || fNameLC.endsWith(".pdf") ||
							fNameLC.endsWith(".htm") || fNameLC.endsWith(".html"))
				{
					String correctedName = DocTools.removeCharsDir(fNameLC);

					outFile = new File(tempDir.getAbsolutePath()+File.separator+ correctedName);

					int count;
					byte[] data = new byte[BUFFER];

					InputStream fis = file.getInputStream();
					fos = new FileOutputStream(outFile);
					dest = new BufferedOutputStream(fos, BUFFER);
					while ((count = fis.read(data, 0, BUFFER)) != -1)
					{
						dest.write(data, 0, count);
					}
					dest.flush();
					dest.close();

					importFiles(tempDir, parentGroupId, request, out, prop, overwriteFiles, my_form.getImagesRootDir(), my_form.getFilesRootDir(), urlReplaces);
				}

				out.println("<hr>"+prop.getText("file.import_done")+"<br/><br/><a href='javascript:window.opener.location.href=\"/admin/v9/webpages/web-pages-list/?refreshLeft=true\";window.close();'>"+prop.getText("button.close")+"</a></body></html>");
				scrollWindow(out);

				GroupsDB.getInstance(true);
				DocDB.getInstance(true);

				return(null);
			}
			else
			{
				response.sendRedirect("/components/import_web_pages/import_zip_file.jsp?error=1");
			}
		}
		else
		{
			response.sendRedirect("/components/import_web_pages/import_zip_file.jsp?error=1");
		}

		return (mapping.findForward("success"));
	}


	/**
	 * Rekurzivne prehlada adresar a naimportuje subory do DB
	 *
	 * @param dir
	 * @return
	 */
	private static boolean importFiles(IwcmFile file, int parentGroupId, HttpServletRequest request, PrintWriter out, Prop prop, boolean overwriteFiles, String imagesRootDir, String filesRootDir, Map<String, String> urlReplaces)
	{
		boolean result = true;

		if (file != null)
		{
			IwcmFile f;
			int size;
			int i;

			try
			{
				if (file.isDirectory())
				{
					IwcmFile[] files = file.listFiles();
					//usortujem pole subory podla nazvu
					Arrays.sort(files,
								new Comparator<IwcmFile>()
								{
								@Override
									public int compare(IwcmFile f1, IwcmFile f2)
									{
										String name1 = f1.getName();
										if (name1.startsWith("index.") || name1.startsWith("default.")) return -1;

										String name2 = f2.getName();
										if (name2.startsWith("index.") || name2.startsWith("default.")) return 1;

										return (name1.compareTo(name2));
									}
								});


					size = files.length;
					for (i=0; i<size; i++)
					{
						f = files[i];
						if (f.isDirectory())
						{
							int newParGrpId = indexDir(f, parentGroupId, request);
							Logger.debug(ImportZipFileAction.class,"Pridavam dir: "+f.getName());
							result = importFiles(f, newParGrpId, request, out, prop, overwriteFiles, imagesRootDir+f.getName()+"/", filesRootDir+f.getName()+"/", urlReplaces);

						}
					}
					for (i=0; i<size; i++)
					{
						f = files[i];
						if (f.isDirectory()==false)
						{
							Logger.debug(ImportZipFileAction.class,"Pridavam file: "+f.getName());
							indexFile(f, parentGroupId, request, out, prop, overwriteFiles, imagesRootDir, filesRootDir, urlReplaces);
						}
					}
				}
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}
		return (result);
	}


	/**
	 * Zaindexuje subor
	 * @param url - url suboru
	 * @param indexedFiles - zoznam do ktoreho sa zapise vysledok
	 * @param request
	 * @return
	 */
	public static boolean indexFile(IwcmFile file, int parentGroupId, HttpServletRequest request, PrintWriter out, Prop prop, boolean overwriteFiles)
	{
		return indexFile(file, parentGroupId, request, out, prop, overwriteFiles, null, null);
	}

	/**
	 * Zaindexuje subor
	 * @param file - subor na indexaciu
	 * @param parentGroupId - id rodicovskeho adresara pre web stranku
	 * @param request
	 * @param out
	 * @param prop
	 * @param overwriteFiles - ak je true budu prepisane existujuce subory
	 * @param imageTargetDirUrl - null alebo URL adresara kam sa skopiruju obrazky (vratane podadresarov)
	 * @return
	 */
	public static boolean indexFile(IwcmFile file, int parentGroupId, HttpServletRequest request, PrintWriter out, Prop prop, boolean overwriteFiles, String imageTargetDirUrl, String fileTargetDirUrl)
	{
		return indexFile(file, parentGroupId, request, out, prop, overwriteFiles, imageTargetDirUrl, fileTargetDirUrl, null);
	}

	public static boolean indexFile(IwcmFile file, int parentGroupId, HttpServletRequest request, PrintWriter out, Prop prop, boolean overwriteFiles, String imageTargetDirUrl, String fileTargetDirUrl, Map<String, String> urlReplaces)
	{
		Identity user = (Identity)request.getSession().getAttribute(Constants.USER_KEY);

		try
		{
			String realPath = file.getAbsolutePath();
			String fileName = file.getName();
			String filePath = "";
			String data = null;

			if (file.getAbsolutePath().indexOf(File.separator+"tmp_import") >= 0)
			{
				filePath = file.getAbsolutePath().substring(file.getAbsolutePath().indexOf(File.separator+ "tmp_import")+1);
				filePath = filePath.substring(filePath.indexOf(File.separator)+1);
			}

			Logger.debug(ImportZipFileAction.class, "absPath: " +file.getAbsolutePath()+ "\n newPath: " +filePath+ "\n\n");

			if (fileName.indexOf('.')==-1)
			{
				return(false);
			}


			String ext = "";
			if (fileName.lastIndexOf('.') > 0)
			{
				ext = fileName.substring(fileName.lastIndexOf('.')).toLowerCase();
			}

			if (".doc".equals(ext))
			{
				data = Word.getText(realPath);
			}
			else if (".xls".equals(ext))
			{
				data = Excel.getText(realPath);
			}
			else if (".pdf".equals(ext))
			{
				data = PDF.getText(realPath);
			}
			else if (".htm".equals(ext) || ".html".equals(ext))
			{
				String tempDataLC = "";
				int startIndex = -1;
				int endIndex = -1;
				StringBuilder str_buff = new StringBuilder();
				IwcmInputStream fis = new IwcmInputStream(file);
				if (fis != null)
				{
					BufferedReader in = new BufferedReader(new InputStreamReader(fis, Constants.FILE_ENCODING));
					String line;
					while ((line = in.readLine()) != null)
					{
						str_buff.append(line);
						str_buff.append('\n');
					}
					in.close();
				}
				fis.close();

				data = str_buff.toString();
				tempDataLC = data.toLowerCase();
				startIndex = tempDataLC.indexOf("<body");
				if (startIndex >= 0)
				{
					endIndex = tempDataLC.indexOf(">", startIndex+1);
					if (endIndex > startIndex && endIndex < tempDataLC.length())
					{
						data = data.substring(endIndex+1);
					}
				}

				startIndex = data.toLowerCase().indexOf("</body");
				if (startIndex > 0)
				{
					data = data.substring(0, startIndex);
				}

				//uprav cesty k obrazkom a suborom
				if (Tools.isNotEmpty(imageTargetDirUrl))
				{
					data = Tools.replace(data, "src=\"./", "src=\""+imageTargetDirUrl);
					data = Tools.replace(data, "src='./", "src='"+imageTargetDirUrl);
				}
				if (Tools.isNotEmpty(fileTargetDirUrl))
				{
					data = Tools.replace(data, "href=\"./", "href=\""+fileTargetDirUrl);
					data = Tools.replace(data, "href='./", "href='"+fileTargetDirUrl);
				}

				//replacni URL
				for (Map.Entry<String, String> e : urlReplaces.entrySet())
				{
					String key = e.getKey();
					String value = e.getValue();
					Logger.debug(ImportZipFileAction.class, "replacing: "+key+"->"+value);
					if (Tools.isNotEmpty(key) && Tools.isNotEmpty(value) && key.equals(value)==false)
					{
						data = Tools.replace(data, key, value);
					}
				}

				//out.println("<b>text suboru:</b> " +htmlData+ "<br><br>");
				//Logger.debug(this,"text suboru: " +htmlData);
			}

			if (Tools.isNotEmpty(imageTargetDirUrl) && (".gif".equals(ext) || ".png".equals(ext) || ".jpg".equals(ext) || ".jpeg".equals(ext)))
			{
				//skopiruj subor do imageTargetDirUrl
				IwcmFile src = file;
				fileName = ext.equals(".jpeg")?fileName.substring(0, fileName.lastIndexOf("."))+".jpg":fileName;
				IwcmFile dest = new IwcmFile(Tools.getRealPath(imageTargetDirUrl+fileName));

				FileTools.copyFile(src, dest);
			}
			if (Tools.isNotEmpty(fileTargetDirUrl) && (".pdf".equals(ext) || ".doc".equals(ext) || ".xls".equals(ext) || ".ppt".equals(ext) || ".zip".equals(ext) || ".exe".equals(ext) || ".rar".equals(ext) || ".xlsx".equals(ext) || ".docx".equals(ext) || ".pptx".equals(ext)))
			{
				//skopiruj subor do fileTargetDirUrl
				IwcmFile src = file;
				IwcmFile dest = new IwcmFile(Tools.getRealPath(fileTargetDirUrl+file.getName()));
				FileTools.copyFile(src, dest);

				//zmen parent adresare a cestu aby sa to spravne zaindexovalo
				parentGroupId = GroupsDB.getInstance().getCreateGroup(fileTargetDirUrl).getGroupId();
				filePath = fileTargetDirUrl+file.getName();
			}

			if (data != null && data.trim().length() > 4)
			{
				//mame text, mozeme zaindexovat

				//	uprav data tak, aby neobsahoval haluzne znaky
				byte[] buff = data.getBytes(Constants.FILE_ENCODING);
				int size = buff.length;
				int i;
				boolean replaced = false;
				for (i=0; i<size; i++)
				{
					if (buff[i]>0 && buff[i]<32 && buff[i]!=10 && buff[i]!=13)
					{
						replaced = true;
						buff[i] = ' ';
					}
				}
				if (replaced)
				{
					Logger.debug(ImportZipFileAction.class,"FileIndexer: replacing bad data in " + fileName);
					data = new String(buff, Constants.FILE_ENCODING);
				}
				if (".htm".equals(ext) || ".html".equals(ext))
				{

				}
				else
				{
					data = Tools.replace(data, "\n", " <br>\n");
				}

				//Logger.debug(this,data);
				Logger.debug(ImportZipFileAction.class,"done, size="+data.length());

				//vytvor Stranku s tymto suborom
				StringTokenizer st = new StringTokenizer(filePath, File.separator+"/");

				String dirName;
				while (st.hasMoreTokens())
				{
					dirName = st.nextToken();
					if (st.hasMoreTokens())
					{
						//adresare preskakujem
					}
					else
					{
						int index = dirName.indexOf('.');
						if (index > 0)
						{
							dirName=dirName.substring(0, index);
						}

						//ak uz existuje stranka, ceknem ci ju mozem prepisat, ak ano zbehne to,
						//alebo ak stranka este neexistuje, zbehne to
						int docID = FileIndexerTools.getFileDocId(dirName, parentGroupId);
						if ((docID > 0 && overwriteFiles) || docID < 0)
						{
							File f = new File(realPath);
							long length = f.length();

							//uz sme na konci, dirName je uz fileName
							Logger.debug(ImportZipFileAction.class,"Vytvaram stranku: " + dirName);
							EditorForm ef = EditorDB.getEditorForm(request, docID, -1, parentGroupId);
							if (docID < 0 || Tools.isEmpty(ef.getTitle()) || prop.getText("editor.newDocumentName").equals(ef.getTitle())) ef.setTitle(dirName);
							ef.setData(data);
							if (Tools.isEmpty(ef.getVirtualPath()) && (".htm".equals(ext) || ".html".equals(ext)))
							{
								//aby sa web stranka volala rovnako ako povodny html subor
								ef.setVirtualPath(fileName);
							}
							if (docID < 0 || Tools.isEmpty(ef.getNavbar()) || prop.getText("editor.newDocumentName").equals(ef.getNavbar())) ef.setNavbar(dirName +  " ["+Tools.formatFileSize(length)+"]");
							ef.setAuthorId(user.getUserId());
							ef.setSearchable(true);
							ef.setAvailable(true);
							ef.setCacheable(false);

							ef.setPublish("1");

							//skus pohladat perexovy obrazok
							String perexImage = searchPerexImage(file, imageTargetDirUrl);
							if (Tools.isNotEmpty(perexImage)) ef.setPerexImage(perexImage);

							out.println(prop.getText("components.clone.creating_page") + " "+dirName);
							int ret = EditorDB.saveEditorForm(ef, request);
							EditorDB.cleanSessionData(request);
							if (ret>0)
							{
								out.println(" [OK]<br/>");
							}
							else
							{
								out.println(" [ERROR code "+ret+"]<br/>");
							}
							scrollWindow(out);
							out.flush();
						}
					}
				}
			}


		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		return(false);
	}

	/**
	 * Vyhlada perexovy obrazok pre zadany HTML subor. Obrazky hlada v adresari
	 * perex s nazvom ako ma html subor ale s priponou gif, png alebo jpg
	 * @param htmlFile
	 * @param imagesRootDir
	 * @return
	 */
	private static String searchPerexImage(IwcmFile htmlFile, String imagesRootDir)
	{
		try
		{
			String fileNameNoExt = htmlFile.getName().substring(0, htmlFile.getName().lastIndexOf('.'));
			String perexFilePath = htmlFile.getParentFile().getAbsolutePath()+File.separator+"perex"+File.separator+fileNameNoExt;
			File perexFile = new File(perexFilePath+".jpg");
			if (perexFile.exists()==false) perexFile = new File(perexFilePath+".gif");
			if (perexFile.exists()==false) perexFile = new File(perexFilePath+".png");

			if (perexFile.exists() && perexFile.canRead() && perexFile.isFile())
			{
				String targetUrl = imagesRootDir+"perex/"+DocTools.removeChars(perexFile.getName());
				boolean ok = FileTools.copyFile(new IwcmFile(perexFile), new IwcmFile(Tools.getRealPath(targetUrl)));
				if (ok) return targetUrl;
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}



		return null;
	}


	/**
	 * Zaindexuje subor
	 * @param url - url suboru
	 * @param indexedFiles - zoznam do ktoreho sa zapise vysledok
	 * @param request
	 * @return
	 */
	public static int indexDir(IwcmFile dir, int parentGroupId, HttpServletRequest request)
	{
		//aby sa vo WebJETe zbytocne nevytvoril adresar
		if (!hasIndexableContent(dir)) return parentGroupId;

		try
		{
			if (dir.isDirectory())
			{
				Logger.debug(ImportZipFileAction.class,"Indexujem dir: "+dir.getName()+ "  parentGroupId: "+parentGroupId);
				String dirName = dir.getName();
				GroupsDB groupsDB = GroupsDB.getInstance();
				GroupDetails group;

				//je to skutocne adresar
				group = FileIndexerTools.findGroup(groupsDB.getGroupsAll(), parentGroupId, dirName);
				if (group == null)
				{
					//vytvor adresar
					group = new GroupDetails();
					group.setGroupName(dirName);
					group.setParentGroupId(parentGroupId);

					group.setSortPriority(groupsDB.getDefaultSortPriority(parentGroupId));

					GroupDetails parentGroup = groupsDB.getGroup(parentGroupId);
					if (parentGroup != null)
					{
						group.setPasswordProtected(parentGroup.getPasswordProtected());
						group.setTempId(parentGroup.getTempId());
						group.setMenuType(parentGroup.getMenuType());
						group.setInternal(parentGroup.isInternal());
						group.setLoggedMenuType(parentGroup.getLoggedMenuType());
					}

					groupsDB.setGroup(group);
				}
				parentGroupId = group.getGroupId();
				//Logger.debug(this,"NOVE parentGroupId: "+parentGroupId);
			}

		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		return(parentGroupId);
	}

	/**
	 * Otestuje ci v zadanom adresari a jeho podadresaroch sa nachadza indexovatelny obsah
	 * (htm, html, doc, xls, pdf...)
	 * @param file
	 * @return
	 */
	private static boolean hasIndexableContent(IwcmFile dir)
	{
		boolean hasIndexableContent = false;

		IwcmFile[] files = dir.listFiles();
		for (IwcmFile f : files)
		{
			if (f.isFile())
			{
				String fileNameLC = f.getName().toLowerCase();
				if (fileNameLC.endsWith(".htm") || fileNameLC.endsWith(".html"))
				{
					hasIndexableContent = true;
				}
			}
			else
			{
				hasIndexableContent = hasIndexableContent(f);
			}

			if (hasIndexableContent) return true;
		}

		return hasIndexableContent;
	}

	private static void scrollWindow(PrintWriter out)
	{
		if (out != null)
		{
			out.println("<script type='text/javascript'>window.scrollBy(0, 1000);</script>");
		}
	}
}
