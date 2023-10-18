package sk.iway.iwcm.filebrowser;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.FileBrowserTools;
import sk.iway.iwcm.common.FilePathTools;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.users.UsersDB;


/**
 *  Zobrazenie adresara suborov (podobne ako Total Commander)
 *
 *@Title        magma-web
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.9 $
 *@created      Piatok, 2003, december 26
 *@modified     $Date: 2004/02/20 21:38:21 $
 */
public class BrowseAction
{
	protected BrowseAction() {
		//utility class
	}

	//private static Hashtable forbiddenSymbols = null;


	/**
	 *  naplni dirList a fileList
	 *
	 *@param  root
	 *@param  dirL     Description of the Parameter
	 *@param  fileL    Description of the Parameter
	 *@param  webRoot  Description of the Parameter
	 *@param  filter   Description of the Parameter
	 *@param  onlySafeFiles - ak je true, listuju sa len bezpecne subory (teda nie CVS adresare a podobne)
	 */
	public static void fillLists(String root, List<FileDirBean> dirL, List<FileDirBean> fileL, String webRoot, boolean onlySafeFiles)
	{
		fillLists(parseRoot(root, Tools.getRealPath("/")), dirL, fileL, onlySafeFiles, true, null);
	}

	/**
	 * Upravena privatna metoda, ktora uz pracuje s URL adresou a nie cestou na disku
	 * @param rootUrl
	 * @param dirL
	 * @param fileL
	 * @param onlySafeFiles
	 * @param user
	 */
	private static void fillLists(String rootUrl, List<FileDirBean> dirL, List<FileDirBean> fileL, boolean onlySafeFiles, boolean addParentDirLink, Identity user)
	{
		IwcmFile[] arrayfile = null;

		if (FileBrowserTools.hasForbiddenSymbol(rootUrl)) rootUrl = "/";

		//Logger.debug(BrowseAction.class, "fillLists, rootUrl="+rootUrl);
		//Logger.debug(BrowseAction.class, "fillLists, realPath="+Tools.getRealPath(rootUrl));

		IwcmFile actFile = new IwcmFile(Tools.getRealPath(rootUrl));

		//Logger.debug(BrowseAction.class, "fillLists, actFile="+actFile.getAbsolutePath()+" exists="+actFile.exists()+" dir="+actFile.isDirectory()+" read="+actFile.canRead());

		FileDirBean fdb;
		String image = "";
		String ext;

		if (actFile.exists())
		{
			arrayfile = actFile.listFiles();

			//Logger.debug(BrowseAction.class, "arrayfileLen="+arrayfile.length);

			try
			{
				//usortuj to podla abecedy
				Arrays.sort(arrayfile,
					new Comparator<IwcmFile>()
					{
					@Override
						public int compare(IwcmFile f1, IwcmFile f2)
						{
							return (f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase()));
						}
					});
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}

			//Logger.println(this,"newRoot: "+newRoot);

			// zoznam suborov
			if (addParentDirLink && Tools.isNotEmpty(rootUrl) && rootUrl.trim().length()>1)
			{
				fdb = new FileDirBean();
				fdb.setName("..");
				fdb.setPath(goBack(rootUrl));
				image = "/components/_common/mime/folderback.gif";
				fdb.setIcon(image);
				dirL.add(fdb);
			}

			boolean fbrowserShowOnlyWritableFolders = Constants.getBoolean("fbrowserShowOnlyWritableFolders");

			for (int i = 0; i < arrayfile.length; i++)
			{
				//Logger.debug(BrowseAction.class, "arrayfile["+i+"].path="+arrayfile[i].getAbsolutePath()+" read="+arrayfile[i].canRead()+" dir="+arrayfile[i].isDirectory());

				if (user!=null && fbrowserShowOnlyWritableFolders && user.isFolderWritable(rootUrl+"/"+arrayfile[i].getName())==false)
				{
					//Logger.debug(BrowseAction.class, "continue, not writable path="+rootUrl+"/"+arrayfile[i].getName());
					continue;
				}

				if (arrayfile[i].isDirectory())
				{
					fdb = new FileDirBean();
					fdb.setName(arrayfile[i].getName());
					if (rootUrl.equals("/"))
					{
						fdb.setPath("/" + fdb.getName());
					}
					else
					{
						fdb.setPath(rootUrl + "/" + fdb.getName());
					}
					//c:/projekty/webjet
					image = "/components/_common/mime/folder.gif";

					//	testni ci je password protected
					if (PathFilter.isPasswordProtected(fdb.getPath(), null)!=null)
					{
						image = "/components/_common/mime/folder_protected.gif";
					}

					fdb.setIcon(image);

					if (FileBrowserTools.hasForbiddenSymbol(fdb.getName()) == false && fdb.getName().startsWith("._")==false && fdb.getName().equals(".DS_Store")==false)
					{
						dirL.add(fdb);
					}
				}
				else if (arrayfile[i].isFile())
				{
					fdb = new FileDirBean();
					fdb.setName(arrayfile[i].getName());
					fdb.setPath(rootUrl);
					try
					{
						ext = arrayfile[i].getName().substring(arrayfile[i].getName().lastIndexOf('.') + 1, arrayfile[i].getName().length()).toLowerCase();
						ext = ext.trim().toLowerCase();
						IwcmFile f = new IwcmFile(Tools.getRealPath("/components/_common/mime/" + ext + ".gif"));
						if (f.exists())
						{
							image = "/components/_common/mime/" + ext + ".gif";
						}
						else
						{
							image = "/components/_common/mime/default.gif";
						}
					}
					catch (Exception ex)
					{
						sk.iway.iwcm.Logger.error(ex);
					}
					fdb.setIcon(image);
					fdb.setLastModified(arrayfile[i].lastModified());
					fdb.setLength(arrayfile[i].length());

					if (FileBrowserTools.hasForbiddenSymbol(fdb.getName()) == false)
					{
						fileL.add(fdb);
					}
				}
			}
		}
		else
		{
			Logger.println(BrowseAction.class,"directory " + rootUrl + " neexistuje");
		}

	}

	/**
	 *  Description of the Method
	 *
	 *@param  oldRoot  Description of the Parameter
	 *@param  webRoot  Description of the Parameter
	 *@return          Description of the Return Value
	 */
	private static String parseRoot(String oldRoot, String webRoot)
	{
		String newRoot = "/";
		if (oldRoot.startsWith(webRoot))
		{
			try
			{
				newRoot = oldRoot.substring(webRoot.length());
				if (!newRoot.startsWith("/"))
				{
					newRoot = "/" + newRoot;
				}
				newRoot = newRoot.replace('\\', '/');
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}

		}
		else
		{
			//ak by sa to nezacinalo na cestu k adresaru, je to nejaky hack
			//takze to ignorujeme...
		}
		return newRoot;
	}

	/**
	 *  vyjde o adresar vyssie
	 *
	 *@param  newR  ceta
	 *@return       ceta o uroven vyssie
	 */
	private static String goBack(String newR)
	{
		String back = "";
		int index1 = newR.lastIndexOf(File.separatorChar);
		int index2 = newR.lastIndexOf('/');
		int index = -1;

		if (index1 > index2)
		{
			index = index1;
		}
		else
		{
			index = index2;

		}
		if (index < 0)
		{
			return newR;
		}

		back = newR.substring(0, index);
		return back;
	}

	/**
	 * Kontrola, ci v nazve suboru nie je zakazany symbol
	 * @param name
	 * @return
	 * @deprecated - use FileBrowserTools.hasForbiddenSymbol
	 */
	@Deprecated
	public static boolean hasForbiddenSymbol(String name)
	{
		return FileBrowserTools.hasForbiddenSymbol(name);
	}

	/**
	 * Metoda vrati zoznam adresarov a suborov v zadanom adresari, umoznuje browsovanie po uroven adresara rootDir
	 * @param request
	 * @param dirList
	 * @param fileList
	 * @param rootDir
	 */
	public static void browseDir(HttpServletRequest request, List<FileDirBean> dirList, List<FileDirBean> fileList, String rootDir)
	{
		String actualDir = "";
		String webroot = Tools.getRealPath("/");

		Identity user = UsersDB.getCurrentUser(request);

		try
		{
			if (rootDir.length() > 1 && rootDir.endsWith("/"))	rootDir = rootDir.substring(0, rootDir.length()-1);
			if (!rootDir.startsWith("/"))	rootDir = "/"+rootDir;

			//Logger.println(this,"webroot: "+webroot);
			if (Tools.getRealPath(rootDir).startsWith(webroot) || (Tools.isNotEmpty(Constants.getString("cloudStaticFilesDir")) && Tools.getRealPath(rootDir).startsWith(FilePathTools.getDomainBaseFolder())))
			{
				if (Tools.isNotEmpty(request.getParameter("actualDir")))
				{
					actualDir = request.getParameter("actualDir");
					if (!actualDir.startsWith("/"))	actualDir = "/"+actualDir;
					if (actualDir.endsWith("/"))	actualDir = actualDir.substring(0, actualDir.length() - 1);

					if (Tools.isEmpty(actualDir)) actualDir = rootDir;

					//kontrola zakazanych znakov v adrese
					for (String fSymbol : FileBrowserTools.forbiddenSymbols)
					{
						if (actualDir.toLowerCase().indexOf(fSymbol) != -1)
						{
							actualDir = rootDir;
							Logger.println(BrowseAction.class,"POZOR! V URL JE NEPOVOLENY ZNAK! -> "+fSymbol);
						}
					}
					if (actualDir.startsWith("/admin") || actualDir.startsWith("/components") ||
						 actualDir.startsWith("/templates") || actualDir.startsWith("/WEB-INF"))
					{
						actualDir = rootDir;
					}

					if (actualDir.startsWith(rootDir)==false) actualDir = rootDir;

					request.setAttribute("correctDir", actualDir);

					//actualRoot = Tools.getRealPath("/");

					BrowseAction.fillLists(actualDir, dirList, fileList, true, !actualDir.equals(rootDir) , null);

					//nastavenie cesty do session pre inline editaciu komponent
					if (user != null) request.getSession().setAttribute("fbrowse.lastDir", actualDir);
				}
				else
				{
					request.setAttribute("correctDir", rootDir);
					BrowseAction.fillLists(rootDir, dirList, fileList, true, false, null);

					//nastavenie cesty do session pre inline editaciu komponent
					if (user != null) request.getSession().setAttribute("fbrowse.lastDir", rootDir);
				}
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

	}

	/**
	 * Vrati do JSP stranky linku na adresar, alebo linku na prihlasenie (ak je to
	 * zaheslovany adresar a user nie je prihlaseny). See site_browser.jsp
	 * @param pageDocId
	 * @param dir
	 * @param req
	 * @return
	 */
	public static String getDirLink(int pageDocId, FileDirBean dir, HttpServletRequest req)
	{
		String ret = "/showdoc.do?docid="+pageDocId+"&amp;actualDir="+dir.getPath();
		if (Constants.getInt("linkType")==Constants.LINK_TYPE_HTML) ret = PathFilter.getOrigPath(req)+"?actualDir="+dir.getPath();

		String logonLink = getLogonLink(dir.getPath(), req);

		if (logonLink != null)
		{
			return(logonLink);
		}

		return(ret);
	}

	/**
	 * Vrati linku na logon dialog, alebo null, ak je mozne cestu zobrazit.
	 * See site_browser.jsp
	 * @param dir
	 * @param req
	 * @return
	 */
	public static String getLogonLink(String dir, HttpServletRequest req)
	{
		if (dir == null)
		{
			return(null);
		}
		Identity user = (Identity)req.getSession().getAttribute(Constants.USER_KEY);
		EditForm ef = PathFilter.isPasswordProtected(dir, req);
		if (ef != null && ef.isAccessibleFor(user)==false)
		{
			boolean isAccesible = false;
			if (user != null)
			{
				//otestuj, ci je user v skupine pre tento adresar
				int i;
				int size = ef.getPasswordProtected().length;
				for (i=0; i<size; i++)
				{
					if (user.isInUserGroup(ef.getPasswordProtected()[i]))
					{
						isAccesible = true;
						break;
					}
				}
			}

			if (!isAccesible)
			{
				if (ef.getLogonDocId()>0)
				{
					return("/showdoc.do?docid="+ef.getLogonDocId()+"&dontUpdateLastDocId=true");
				}
				else
				{
					return("/components/user/logon.jsp?permsDenied=true");
				}
			}
		}
		return(null);
	}

	/**
	 * Vrati zoznam suborov z daneho korenoveho adresara rekurzivne v podadresaroch
	 * @param request
	 * @param rootDir root dir
	 * @return List {@link FileDirBean}-ov
	 */
	public static List<FileDirBean> searchForFilesRecurse(HttpServletRequest request, String rootDir){
		List<FileDirBean> allFilesList = new ArrayList<>(100);
		List<FileDirBean> fileList = new ArrayList<>();
		List<FileDirBean> dirList = new ArrayList<>();
		BrowseAction.browseDir(request, dirList, fileList, rootDir);
		allFilesList.addAll(fileList);
		for(FileDirBean dir : dirList){
			allFilesList.addAll(searchForFilesRecurse(request,dir.getPath()));
		}

		Collections.sort(allFilesList,new Comparator<FileDirBean>()
		{
			@Override
				public int compare(FileDirBean f1, FileDirBean f2)
				{
					return (f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase()));
				}
		});
		return allFilesList;
	}
}
