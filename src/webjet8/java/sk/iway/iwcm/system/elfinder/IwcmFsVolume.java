package sk.iway.iwcm.system.elfinder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsItem;
import cn.bluejoe.elfinder.service.FsVolume;
import cn.bluejoe.elfinder.util.MimeTypesUtils;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.AdminTools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.FileBrowserTools;
import sk.iway.iwcm.common.FileIndexerTools;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmFileFilter;
import sk.iway.iwcm.io.IwcmFsDB;
import sk.iway.iwcm.io.IwcmInputStream;
import sk.iway.iwcm.io.IwcmOutputStream;
import sk.iway.iwcm.users.UsersDB;

/**
 *  IwcmFsVolume.java - volume objekt pre elFinder (cn.bluejoe.elfinder)
 *
 *@Title        webjet8
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2015
 *@author       $Author: jeeff jeeff $
 *@version      $Revision: 1.3 $
 *@created      Date: 19.2.2015 19:09:46
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class IwcmFsVolume implements FsVolume
{
	public static final String VOLUME_ID_ACTUAL_PAGE = "iwcm_fs_ap_volume";

	protected String _name; //NOSONAR

	protected IwcmFile _rootDir; //NOSONAR

	public IwcmFsVolume(String name, String rootUrl)
	{
		this._name = name;
		this._rootDir = new IwcmFile(Tools.getRealPath(rootUrl));
	}

	public IwcmFsVolume(String name, IwcmFile rootDir)
	{
		this._name = name;
		this._rootDir = rootDir;
	}

	protected IwcmFile asFile(FsItem fsi)
	{
		return ((IwcmFsItem) fsi).getFile();
	}

	@Override
	public void createFile(FsItem fsi) throws IOException
	{
		IwcmFile file = asFile(fsi);

		if (canWrite(file.getParentFile()))
		{
			if (file.getParentFile().exists() == false) file.getParentFile().mkdirs();
			file.createNewFile();

			//kvoli FSDB musime do suboru nieco zapisat
			byte[] buf = new byte[0];
			ByteArrayInputStream bis = new ByteArrayInputStream(buf);
			IwcmFsDB.writeFiletoDest(bis, new File(file.getAbsolutePath()), 0, true);

			Adminlog.add(Adminlog.TYPE_FILE_CREATE, "elfinder createFile, path=" + file.getVirtualPath(), -1, -1);
		}
	}

	@Override
	public void createFolder(FsItem fsi) throws IOException
	{
		IwcmFsItem fsii = (IwcmFsItem) fsi;
		IwcmFile f = asFile(fsi);

		if (canWrite(f))
		{
			//odstran diakritiku
			if (f.getVirtualPath().startsWith("/files") || f.getVirtualPath().startsWith("/images"))
			{
				String newDir = DB.internationalToEnglish(DocTools.removeCharsDir(f.getName(), true).toLowerCase());
				IwcmFile f2 = new IwcmFile(f.getParentFile(), newDir);
				fsii.setFile(f2);
			}

			fsii.getFile().mkdirs();

			Adminlog.add(Adminlog.TYPE_FILE_CREATE, "elfinder createFolder, path=" + fsii.getFile().getVirtualPath(), -1, -1);
		}
	}

	@Override
	public boolean deleteFile(FsItem fsi) throws IOException
	{
		boolean deleted = false;
		IwcmFile file = asFile(fsi);
		if (!file.isDirectory() && canWrite(file))
		{
			deleted = file.delete();
			//zmaz z indexu (ak existuje)
			String virtualPath = file.getVirtualPath();
			if (virtualPath.startsWith("/files"))
			{
				//vymazanie full text indexu
				FileIndexerTools.deleteIndexedFile(virtualPath);
			}

			Adminlog.add(Adminlog.TYPE_FILE_DELETE, "elfinder deleteFile, path="+virtualPath, -1, -1);
		}
		return deleted;
	}

	@Override
	public boolean deleteFolder(FsItem fsi) throws IOException
	{
		boolean deleted = false;

		Identity user = sk.iway.iwcm.system.elfinder.FsService.getCurrentUser();
		IwcmFile file = asFile(fsi);
		if (user == null || user.isDisabledItem("fbrowser_delete_directory")) {
			//tempfolder vznika pri zipovani suborov, ten je teda povolene vzdy zmazat bez ohladu na prava
			if (file.getName().contains("tempfolder")==false) {
				return false;
			}
		}

		//adresar / nedovolime zmazat, to je nezmysel zmazat cely web
		if (file.isDirectory() && canWrite(file) && "/".equals(file.getVirtualPath())==false)
		{
			//JEEFF FileUtils.deleteDirectory(file);
			deleted = FileTools.deleteDirTree(file);
			Adminlog.add(Adminlog.TYPE_FILE_DELETE, "elfinder deleteFolder, path="+file.getVirtualPath(), -1, -1);
		}

		return deleted;
	}

	private boolean canWrite(IwcmFile file)
	{
		Identity user = sk.iway.iwcm.system.elfinder.FsService.getCurrentUser();
		if (user == null) return false;

		return UsersDB.isFolderWritable(user.getWritableFolders(), file.getVirtualPath());
	}

	@Override
	public boolean exists(FsItem newFile)
	{
		return asFile(newFile).exists();
	}

	protected IwcmFsItem fromFile(IwcmFile file)
	{
		file = new IwcmFile(Tools.getRealPath(file.getVirtualPath()));

		return new IwcmFsItem(this, file);
	}

	@Override
	public FsItem fromPath(String relativePath)
	{
		return fromFile(new IwcmFile(_rootDir, relativePath));
	}

	@Override
	public String getDimensions(FsItem fsi)
	{
		return null;
	}

	@Override
	public long getLastModified(FsItem fsi)
	{
		if(!Constants.getBoolean("elfinderUseFastLoading"))
		{
			long lastModified = asFile(fsi).lastModified();
			//prepocet na JavaScript timestamp
			lastModified = Math.round((double)lastModified / 1000);
			return lastModified;
		}
		return 0;
	}

	@Override
	public String getMimeType(FsItem fsi)
	{
		IwcmFile file = asFile(fsi);
		if (file.isDirectory())
			return "directory";

		//JEEFF String ext = FileUtils.getExtension(file.getName());
		String ext = FileTools.getFileExtension(file.getName());
		if (ext != null && !ext.isEmpty())
		{
			String mimeType = MimeTypesUtils.getMimeType(ext);
			return mimeType == null ? MimeTypesUtils.UNKNOWN_MIME_TYPE : mimeType;
		}

		return MimeTypesUtils.UNKNOWN_MIME_TYPE;
	}

	public String getName()
	{
		return _name;
	}

	@Override
	public String getName(FsItem fsi)
	{
		IwcmFile fsiFile = asFile(fsi);
		return fsiFile.getName();
	}

	@Override
	public FsItem getParent(FsItem fsi)
	{
		IwcmFile fsiFile = asFile(fsi);
		if ("images".equals(fsiFile.getName()))
		{
			//Logger.debug(IwcmFsVolume.class, "Som images");
		}
		IwcmFile parentFile = fsiFile.getParentFile();
		String virtualPath = parentFile.getVirtualPath();
		//Logger.debug(IwcmFsVolume.class, "getParent, file=" + fsiFile.getAbsolutePath() + " parent=" + parentFile.getAbsolutePath()+" virtual="+virtualPath);
		if ("/".equals(virtualPath))
		{
			parentFile = new IwcmFile(Tools.getRealPath("/"));
		}

		return fromFile(parentFile);
	}

	@Override
	public String getPath(FsItem fsi) throws IOException
	{
		String relativePath = "";
		if (InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir"))
		{
			//pre istotu je to takto lebo az pri cloude sa na to prislo ale takto by to malo fungovat dobre aj pre standardny WJ
			IwcmFile file = asFile(fsi);
			relativePath = file.getVirtualPath();
			if ("/".equals(relativePath)) relativePath = "";
		}
		else
		{
			String fullPath = asFile(fsi).getAbsolutePath();
			//String rootPath = _rootDir.getCanonicalPath();
			//if (fullPath.endsWith(File.separator)==false) fullPath = fullPath+File.separator;

			String rootPath = _rootDir.getAbsolutePath();
			if (rootPath.endsWith("/"))
			{
				rootPath = rootPath.substring(0, rootPath.length()-1);
			}

			try
			{
				relativePath = fullPath.substring(rootPath.length());
			}
			catch (Exception e)
			{
				Logger.debug(IwcmFsVolume.class, "fullPath="+fullPath+" rootPath="+rootPath);
				sk.iway.iwcm.Logger.error(e);
			}
		}

		//nepovolime ../
		if (FileBrowserTools.hasForbiddenSymbol(relativePath))
		{
			Adminlog.add(Adminlog.TYPE_XSS, "Pokus o XSS utok - volanie elfinder nepovolenej cesty - "+relativePath, -1, -1);
			return null;
		}

		return relativePath.replace('\\', '/');
	}

	@Override
	public FsItem getRoot()
	{
		return fromFile(_rootDir);
	}

	public IwcmFile getRootDir()
	{
		return _rootDir;
	}

	@Override
	public long getSize(FsItem fsi)
	{
		if(Constants.getBoolean("elfinderUseFastLoading")) return 0;
		IwcmFile file = asFile(fsi);
		long size = file.getLength();
		if (file.isDirectory())
		{
			size = 0;
			//iterate over files in directory and sum their sizes
			IwcmFile[] files = file.listFiles();
			for (IwcmFile f : files)
			{
				size += f.getLength();
			}
		}
		return size;
	}

	@Override
	public String getThumbnailFileName(FsItem fsi)
	{
		return null;
	}

	@Override
	public boolean hasChildFolder(FsItem fsi)
	{
		if(!Constants.getBoolean("elfinderUseFastLoading"))
		{
			return asFile(fsi).isDirectory() && asFile(fsi).listFiles(new IwcmFileFilter()
			{
				@Override
				public boolean accept(IwcmFile arg0)
				{
					return arg0.isDirectory();
				}
			}).length > 0;
		}
		else
			return true;
	}

	@Override
	public boolean isFolder(FsItem fsi)
	{
		return asFile(fsi).isDirectory();
	}

	@Override
	public boolean isRoot(FsItem fsi)
	{
		//boolean isRoot = (_rootDir == asFile(fsi));
		//Logger.debug(IwcmFsVolume.class, "isRoot: " + _rootDir + " " + asFile(fsi) + " " + asFile(fsi).getVirtualPath() + " is="+isRoot);
		//return isRoot;

		return "/".equals(asFile(fsi).getVirtualPath());
	}

	@Override
	public FsItem[] listChildren(FsItem fsi)
	{
		List<FsItem> list = new ArrayList<>();

		IwcmFile fsiFile = asFile(fsi);

		Logger.debug(IwcmFsVolume.class, "listChildrens, virtualPath="+fsiFile.getVirtualPath()+" realPath="+fsiFile.getAbsolutePath());

		IwcmFile[] cs = fsiFile.listFiles();
		if (cs == null)
		{
			Logger.debug(IwcmFsVolume.class, "listChildren - cs is null: "+cs);
			return new FsItem[0];
		}

		boolean fbrowserShowOnlyWritableFolders = Constants.getBoolean("fbrowserShowOnlyWritableFolders");

		Identity user = sk.iway.iwcm.system.elfinder.FsService.getCurrentUser();
		Logger.debug(IwcmFsVolume.class, "listChildren, user="+user+" cs.length="+cs.length+" fbrowserShowOnlyWritableFolders="+fbrowserShowOnlyWritableFolders);
		if (user!=null)
		{
			String path = fsiFile.getVirtualPath();
			if (path.endsWith("/")==false) path = path+"/"; //NOSONAR

			List<IwcmFile> writableFolders = user.getWritableFoldersList();
			String fbrowserAlwaysShowFolders = Constants.getStringExecuteMacro("fbrowserAlwaysShowFolders");
			if (Tools.isNotEmpty(fbrowserAlwaysShowFolders))
			{
				String[] fbrowserAlwaysShowFoldersList = fbrowserAlwaysShowFolders.split("\n");
				if (fbrowserAlwaysShowFoldersList.length > 0) {
					for (String fbrowserAlwaysShowFolder : fbrowserAlwaysShowFoldersList) {
						writableFolders.add(new IwcmFile(Tools.getRealPath(fbrowserAlwaysShowFolder)));
					}
				}
			}

			for (IwcmFile c : cs)
			{
				if (FileBrowserTools.hasForbiddenSymbol(c.getName())) {
					continue;
				}

				if (fbrowserShowOnlyWritableFolders && !isFolderWritable(writableFolders, c.getVirtualPath())) {
						continue;
				}

				list.add(fromFile(c));
			}
		}

		return list.toArray(new FsItem[0]);
	}

	private boolean isFolderWritable(List<IwcmFile> writableFolders, String path) {
		IwcmFile pathFile = new IwcmFile(Tools.getRealPath(path));
		String pathVirtualPath = pathFile.getVirtualPath();
		if (!pathVirtualPath.endsWith("/") && pathFile.isDirectory()) {
			pathVirtualPath += "/";
		}

		for (IwcmFile folder : writableFolders) {
			IwcmFile actualFolder = folder;
			if (!actualFolder.exists()) {
				actualFolder = new IwcmFile(Tools.replace(folder.getAbsolutePath(), "*", ""));
			}

			if (!actualFolder.exists()) {
				continue;
			}

			String folderVirtualPath = actualFolder.getVirtualPath();
			if (!folderVirtualPath.endsWith("/")) {
				folderVirtualPath += "/";
			}

			if (pathFile.isDirectory() && pathVirtualPath.startsWith(folderVirtualPath) || folderVirtualPath.startsWith(pathVirtualPath)) {
					return true;
			}

			if (pathFile.isFile() && pathVirtualPath.startsWith(folderVirtualPath)) {
					return true;
			}
		}
		return false;
	}

	@Override
	public IwcmInputStream openInputStream(FsItem fsi) throws IOException
	{
		return new IwcmInputStream(asFile(fsi));
	}

	@Override
	public IwcmOutputStream openOutputStream(FsItem fsi) throws IOException
	{
		Adminlog.add(Adminlog.TYPE_FILE_SAVE, "elfinder saveFile, path="+asFile(fsi).getVirtualPath(), -1, -1);

		return new IwcmOutputStream(asFile(fsi));
	}

	@Override
	public void rename(FsItem src, FsItem dst) throws IOException
	{
		asFile(src).renameTo(asFile(dst));
		Adminlog.add(Adminlog.TYPE_FILE_EDIT, "elfinder rename, old="+asFile(src).getVirtualPath()+" new="+asFile(dst).getVirtualPath(), -1, -1);
	}

	public void setName(String name)
	{
		_name = name;
	}

	public void setRootDir(IwcmFile rootDir)
	{
		if (!rootDir.exists())
		{
			rootDir.mkdirs();
		}

		_rootDir = rootDir;
	}

	/**
	 * Vrati URL adresu pre subor
	 * @param item
	 * @return
	 */
	public String getVirtualPath(FsItemEx item) throws IOException
	{
		IwcmFile file = asFile(fromPath(item.getPath()));

		return file.getVirtualPath();
	}

	/**
	 * Ak je pre domenu definovany alias suborov vrati /alias pre jednoduche vlozenie do cesty
	 * @return
	 * @deprecated use AdminTools.getDomainNameFileAliasAppend()
	 */
	@Deprecated
	public static String getDomainNameFileAliasAppend()
	{
		return AdminTools.getDomainNameFileAliasAppend();
	}

	/**
	 * For files/dir in /files and /images directories, remove diacritics and convert to lowercase
	 * @param name
	 * @param fsi
	 * @return
	 * @throws IOException
	 */
	public static String removeSpecialChars(String name, FsItemEx fsi) throws IOException {
		if (fsi.getPath().startsWith("/files") || fsi.getPath().startsWith("/images"))
		{
			name = DB.internationalToEnglish(name);
			name = DocTools.removeCharsDir(name, true).toLowerCase();
		}
		return name;
	}
}
