package sk.iway.iwcm.common;

import java.io.File;
import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.form.FormMailAction;
import sk.iway.iwcm.io.IwcmFile;

/**
 *  FilePathTools.java - trieda pre preklad ciest k statickym suborom Cloudu
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2013
 *@author       $Author: jeeff jeeff $
 *@version      $Revision: 1.3 $
 *@created      Date: 14.2.2013 15:34:12
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class FilePathTools
{
	private static String normalizeVirtualPath(String virtualPath)
	{
		if (Tools.isEmpty(virtualPath)) return "/";

		if (virtualPath.indexOf("//")!=-1) virtualPath = Tools.replace(virtualPath, "//", "/");
		if (virtualPath.indexOf("\\")!=-1) virtualPath = Tools.replace(virtualPath, "\\", "/");
		if (virtualPath.length()==0 || virtualPath.charAt(0)!='/') virtualPath ='/' + virtualPath;

		return virtualPath;
	}

	/**
	 * Pre zadanu virtual path vrati true ak sa jedna o cestu k externym suborom (/images, /files, /css, /jscripts)
	 * @param virtualPath
	 * @return
	 */
	public static boolean isExternalDir(String virtualPath)
	{
		if (InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir"))
		{
			if (Tools.isNotEmpty(Constants.getString("cloudStaticFilesDir")))
			{
				virtualPath = normalizeVirtualPath(virtualPath);
				String virtualPathLC = virtualPath.toLowerCase();
				//jeeff: toto som zrusil aby sme nemuseli pri uprave CSS menit X webov || virtualPathLC.startsWith("/css") || virtualPathLC.startsWith("/jscripts")
				if (virtualPathLC.startsWith("/images") ||
					 virtualPathLC.startsWith("/files") ||
					 virtualPathLC.startsWith("/shared") ||
					 virtualPath.startsWith(Constants.getString("thumbServletCacheDir")) ||
					 virtualPath.startsWith(FormMailAction.FORM_FILE_DIR) ||
					 //kvoli pentestom, tu mozu byt rozbalovane importy, mali by byt pre kazdy host separe (okrem cloudu, kde to nechceme)
					 ("cloud".equals(Constants.getInstallName())==false && virtualPathLC.startsWith("/web-inf/tmp/"))
					) return true;
			}
		}
		return false;
	}

	/**
	 * Hellper pre ziskanie virtual path z realnej cesty, nepouzivat napriamo, len cez IwcmFsDB.getVirtualPath
	 * @param realPath
	 * @return
	 */
	public static String getVirtualPathHellper(String realPath)
	{
		String domainBaseFolder = getDomainBaseFolder();
		if (realPath.equals(domainBaseFolder))
		{
			return "/";
		}
		if (realPath.startsWith(domainBaseFolder)==false && realPath.contains(File.separatorChar+"shared"))
		{
			//ak realPath obsahuje shared, tak skusme pouzit shared folder
			domainBaseFolder = getDomainBaseFolder("shared");
			//its shared domain base folder, e.g. .../static-files/shared/ (not .../static-files/sharer/shared) so virtualPath is root
			if (domainBaseFolder.equals(realPath)) return "/";
		}
		if (realPath.startsWith(domainBaseFolder))
		{
			String virtualPath = realPath.substring(domainBaseFolder.length());
			virtualPath = virtualPath.replace('\\', '/');
			if (isExternalDir(virtualPath))
			{
				return virtualPath;
			}
		}

		return null;
	}

	/**
	 * Vrati meno adresara pre aktualnu domenu (bez celej cesty)
	 * @return
	 */
	private static String getDomainFolderName(String domain)
	{
		if (Constants.getBoolean("multiDomainEnabled")==false)
		{
			return "defaulthost";
		}

		domain = domain.toLowerCase();

		if (domain.startsWith("www.")) domain = domain.substring(4);

		if (domain.length() > 3 && "cloud".equals(Constants.getInstallName()))
		{
			domain = domain.charAt(0)+File.separator+domain.charAt(1)+File.separator+domain.charAt(2)+File.separator+domain;
		}

		return domain;
	}

	/**
	 * Vrati cestu k adresaru pre aktualnu domenu
	 * @return
	 */
	public static String getDomainBaseFolder()
	{
		return getDomainBaseFolder(CloudToolsForCore.getDomainName());
	}

	/**
	 * Vrati cestu k adresaru pre zadanu domenu
	 * @param domain
	 * @return
	 */
	public static String getDomainBaseFolder(String domain)
	{
		String cloudStaticFilesDir = Constants.getString("cloudStaticFilesDir");
		final String FILE_ROOT_MACRO = "{FILE_ROOT}";
		if (cloudStaticFilesDir.startsWith(FILE_ROOT_MACRO)) {
			String root = Tools.getRealPath("/");
			if (root.endsWith(""+File.separatorChar)==false) root = root+File.separatorChar;

			cloudStaticFilesDir = root + cloudStaticFilesDir.substring(FILE_ROOT_MACRO.length());

			if (cloudStaticFilesDir.endsWith(""+File.separatorChar)==false) cloudStaticFilesDir = cloudStaticFilesDir+File.separatorChar;
		}
		return cloudStaticFilesDir+getDomainFolderName(domain);
	}

	public static boolean writeFileOut(String path, HttpServletRequest req, HttpServletResponse res)
	{
		//pre cloud WebJET posielame inu cestu k suborom
		IwcmFile f = new IwcmFile(Tools.getRealPath(path));
		return writeFileOut(f, req, res);
	}

	/**
	 * Zapise dany subor na vystup vratane riesenia RANGE http hlavicky a odpovede
	 * nastavi korektne aj content-type, riesi if-modified-since hlavicku
	 * @param f
	 * @param req
	 * @param res
	 * @return
	 * @throws IOException
	 */
	public static boolean writeFileOut(IwcmFile f, HttpServletRequest req, HttpServletResponse res)
	{
		if (f.exists() && f.isFile())
		{
			if (f.getName().toLowerCase().endsWith(".jsp") || f.getName().toLowerCase().endsWith(".class"))
			{
				//toto nevieme, nerob nic
			}
			else
			{
					try
					{
						MultipartFileSender.fromFile(f)
						.with(req)
						.with(res)
						.serveResource();

						return true;
					}
					catch (Exception ex)
					{
						if (ex.getMessage().contains("java.io.IOException: Broken pipe"))
						{
							System.err.println(ex.getMessage());
							return true;
						}
						else sk.iway.iwcm.Logger.error(ex);
					}
			}
		}
		return false;
	}
}
