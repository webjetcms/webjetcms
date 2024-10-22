package sk.iway.iwcm.system;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspWriter;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.FileBrowserTools;
import sk.iway.iwcm.doc.RelatedPagesDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.zip.ZipEntry;
import sk.iway.iwcm.system.zip.ZipOutputStream;
import sk.iway.iwcm.users.UsersDB;

/**
 *  Archive.java
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: thaber $
 *@version      $Revision: 1.8 $
 *@created      Date: 14.12.2004 14:20:08
 *@modified     $Date: 2009/01/26 15:50:32 $
 */
public class Archive
{
	private static final int BUFFER = 8192;

	/**
	 * Toto sa vola z crontabu raz za den
	 * @param args
	 */
	public static void main(String[] args)
	{
		if (args != null && args.length > 0)
		{
			//adresar, kde sa ulozi zip-archiv
			String zipDirPath = args[0];

			//adresare, ktore sa archivuju (WebRoot = /)
			String archiveDirs = args[1];

			Logger.println(Archive.class,"Archive.makeZipArchive()");
			try
			{
			makeZipArchive(null, Constants.getServletContext(), archiveDirs, zipDirPath, null);
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}
		}
	}



	/**
	 *  Rekurzivne prehlada adresare a najdene subory prida do ZIP-archivu
	 *@param  mainDir - adresar, ktory chceme rekurzivne prehladat
	 *@param  servletContext
	 *@param  zipOut - out stream zip suboru
	 *@param  printWriter
	 *@return
	 */
	private static void getFilesFromDir(String mainDir, ServletContext servletContext, ZipOutputStream zipOut, JspWriter printWriter, String outFileName)
	{
		byte[] buf = new byte[BUFFER];
		FileInputStream in;
		int nameIndex;
		int scrollIndex = 0;

		if (mainDir.endsWith("/"))
		{
			mainDir = mainDir.substring(0, mainDir.length()-1);
		}
		//Logger.println(this,"mainDir: "+mainDir);

		String realPath = Tools.getRealPath(mainDir);
		if (realPath != null)
		{
			File file = new File(realPath);
			int size;
			int i;
			try
			{
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
								getFilesFromDir(mainDir+"/"+file.getName(), servletContext, zipOut, printWriter, outFileName);
							}
						}
						else
						{
							 nameIndex = file.getName().indexOf(DocTools.removeChars(Constants.getInstallName())+"-");

							if (file.getName().toLowerCase().indexOf(".cvsignore") == -1 && !(nameIndex != -1 && file.getName().toLowerCase().endsWith(".zip")))
							{
								if (printWriter != null)
								{
									printWriter.println("Adding file: " + mainDir + "/" + file.getName() + "<br>");
									if (scrollIndex >= 15)
									{
										printWriter.println("<script language='javascript'>window.scrollBy(0,1000);</script>");
										scrollIndex = 0;
									}
									printWriter.flush();
									scrollIndex++;
								}
								in = new FileInputStream(realPath + File.separatorChar + file.getName());
								try
								{
									//Add ZIP entry to output stream.
									zipOut.putNextEntry(new ZipEntry(mainDir + "/" + file.getName()));

					            // Transfer bytes from the file to the ZIP file
					            int len;
					            while ((len = in.read(buf)) > 0)
					            {
					            	zipOut.write(buf, 0, len);
					            }
					            // Complete the entry
					            zipOut.closeEntry();
								}
								finally { in.close(); }
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
	 * Vytvori ZIP-archiv Webroot adresara, ak su
	 *
	 * @param servletContext
	 * @param mainDirs       - korenove adresare, ktore sa maju zalohovat
	 * @param zipDirPath     - cesta k ZIP-suboru
	 * @param printWriter
	 */
	public static void makeZipArchive(HttpServletRequest request, ServletContext servletContext, String mainDirs, String zipDirPath, JspWriter printWriter) throws IOException {

		if (printWriter != null) printWriter.println("<b>Archiving...</b><br><br>");

		if (FileBrowserTools.hasForbiddenSymbol(zipDirPath) || FileBrowserTools.hasForbiddenSymbol(mainDirs)) {
			if (printWriter != null) printWriter.println("<b>Forbidden symbol in path!</b><br><br>");
			return;
		}

		Identity user = UsersDB.getCurrentUser(request);
		if (user == null || user.isAdmin()==false || user.isFolderWritable(zipDirPath)==false) {
			if (printWriter != null) printWriter.println("<b>Access denied!</b><br><br>");
			return;
		}

		double time = 0;
		String[] archiveDirs = { "/" };
		// String outFilename;
		String zipFilePath;
		Prop prop = null;

		try {
			if (Tools.isEmpty(zipDirPath)) {
				zipDirPath = "/files/protected-backup/"; // NOSONAR
			}

			SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.getString("dateFormat"));
			SimpleDateFormat timeFormat = new SimpleDateFormat(Constants.getString("timeFormat"));
			long dateTime = Tools.getNow();
			// poskladam nazov suboru
			String fileName = Constants.getInstallName() + "-" + dateFormat.format(new Timestamp(dateTime)) + "-"
					+ timeFormat.format(new Timestamp(dateTime)) + ".zip";

			// z nazvu suburu vyhodim zakazane znaky
			fileName = DocTools.removeChars(fileName);
			if (!zipDirPath.startsWith("/"))
				zipDirPath = "/" + zipDirPath; // NOSONAR
			if (!zipDirPath.endsWith("/"))
				zipDirPath = zipDirPath + "/"; // NOSONAR

			// poskladam cestu archivu
			zipFilePath = zipDirPath + fileName;

			if (Tools.isNotEmpty(mainDirs)) {
				// rozparsujeme si adresare, ktore sa budu archivovat
				archiveDirs = RelatedPagesDB.getTokens(mainDirs, ",");
			}

			Adminlog.add(Adminlog.TYPE_FILE_CREATE, "ZIP archive, zip=" + zipFilePath + " dir=" +  Tools.join(archiveDirs, ","), -1, -1);

			// vytvorime si ZIP file
			if (servletContext != null) {
				File outFile = new File(Tools.getRealPath(zipFilePath));
				if (outFile.getParentFile().exists() == false) {
					if (!outFile.getParentFile().mkdirs() && printWriter != null) {
						printWriter.println("<br><b>Unable to create dirs: " + zipFilePath + "</b>");
						return;
					}
				}
				ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(outFile));

				// zmeriame cas operacie
				time = Tools.getNow();

				// iterujeme po adresaroch a pridavame subory do ZIP-archivu
				for (int i = 0; i < archiveDirs.length; i++) {
					getFilesFromDir(archiveDirs[i], servletContext, zipOut, printWriter, fileName);
				}

				// uzavretie ZIP suboru
				zipOut.close();
				time = (Tools.getNow() - time) / 1000D;
				if (request != null) {
					prop = Prop.getInstance(servletContext, request);
				}
			}
			if (prop != null) {
				if (printWriter != null)
					printWriter.println("<br><b>" + prop.getText("admin.archive.done") + "!<br>"
							+ prop.getText("admin.archive.elapsed_time") + ": " + time + " sec</b>");
				if (printWriter != null)
					printWriter.println("<br>" + prop.getText("admin.archive.archive_filename") + ": <b><a href='"
							+ zipFilePath + "' target='_blank'>" + zipFilePath + "</a></b>");
			} else {
				if (printWriter != null)
					printWriter.println("<br><b>Done!<br>Elapsed time: " + time + " sec</b>");
				if (printWriter != null)
					printWriter.println("<br>Archive filename: <b><a href='" + zipFilePath + "' target='_blank'>"
							+ zipFilePath + "</a></b>");
			}
		} catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
		}
	}
}
