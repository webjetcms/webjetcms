package sk.iway.iwcm.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.persistence.Query;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;

import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.queries.ReadAllQuery;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.FileBrowserTools;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.system.jpa.JpaTools;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

/**
 *  FileHistoryDB.java - praca s historiou suborov
 *  tiket 13373
 *
 *	DAO class for manipulating with FileHistoryBean
 *
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: mrepasky $
 *@version      $Revision: 1.3 $
 *@created      Date: 17.05.2013 14:40:47
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class FileHistoryDB extends JpaDB<FileHistoryBean>
{
	public FileHistoryDB()
	{
		super(FileHistoryBean.class);
	}

	/**
	 * Retained for binary compatibility. In request context it resolves the authenticated user and delegates
	 * to the secured overload; without an authenticated administrator it fails closed.
	 * @deprecated use {@link #sendFileFromHistory(String, int, Identity, HttpServletResponse)}
	 */
	@Deprecated
	public static boolean sendFileFromHistory(String path, int historyId, HttpServletResponse response)
	{
		RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
		if (requestBean == null || requestBean.getUserId() < 1) return false;
		UserDetails userDetails = UsersDB.getUser(requestBean.getUserId());
		if (userDetails == null) return false;
		return sendFileFromHistory(path, historyId, new Identity(userDetails), response);
	}

	/**
	 * Sends a stored file version after checking its domain and file-browser folder permissions.
	 */
	public static boolean sendFileFromHistory(String path, int historyId, Identity user, HttpServletResponse response)
	{
		if (historyId < 1) return false;
		FileHistoryBean fhb = (new FileHistoryDB()).getById(historyId);
		if (fhb != null && path != null && path.equals(fhb.getFileUrl()))
		{
			IwcmFile file = getFileHistorySourceFile(fhb.getFileUrl(), fhb.getHistoryPath(), historyId,
				fhb.getDomainId(), user);
			if (file != null && file.exists() && file.isFile() && file.canRead())
			{
				try
				{
					//nastav HTTP hlavicky
					String mimeType = Constants.getServletContext().getMimeType(path.toLowerCase());
					if (Tools.isEmpty(mimeType)) mimeType = "application/octet-stream";
					String ext = FileTools.getFileExtension(path);
					if (ext.equals("jsp") || ext.equals("js")) mimeType = "text/plain";

					Logger.debug(FileHistoryDB.class, "setting content type:"+mimeType);

					response.setContentType(mimeType);

					writeFileOut(file, response);

					return true;
				}
				catch (Exception e)
				{
					sk.iway.iwcm.Logger.error(e);
				}
			}
		}
		return false;
	}

	/**
	 * Checks access to file-history metadata in the current domain.
	 */
	public static boolean isFileHistoryAccessible(String fileUrl, Integer domainId, Identity user)
	{
		return getFileHistoryCurrentFile(fileUrl, domainId, user) != null;
	}

	/**
	 * Resolves the current file to the canonical path that was checked by the folder ACL.
	 */
	public static IwcmFile getFileHistoryCurrentFile(String fileUrl, Integer domainId, Identity user)
	{
		int currentDomainId = CloudToolsForCore.getDomainId();
		if (user == null || user.isAdmin() == false || user.isEnabledItem("menuFbrowser") == false ||
			currentDomainId < 1 || domainId == null || domainId.intValue() != currentDomainId ||
			isSafeVirtualPath(fileUrl) == false || fileUrl.endsWith("/")) return null;

		String sourceFolder = getParentFolder(fileUrl);
		if (sourceFolder == null || user.isFolderWritable(sourceFolder) == false) return null;

		CanonicalFile canonicalFile = resolveCanonicalFile(fileUrl, getTopLevelRoot(fileUrl));
		if (canonicalFile == null) return null;

		String canonicalFolder = getParentFolder(canonicalFile.virtualPath);
		if (canonicalFolder == null || user.isFolderWritable(canonicalFolder) == false) return null;
		return canonicalFile.file;
	}

	/**
	 * Checks access to the physical content of one file-history record.
	 */
	public static boolean isFileHistoryContentAccessible(String fileUrl, String historyPath, long historyId,
		Integer domainId, Identity user)
	{
		return getFileHistorySourceFile(fileUrl, historyPath, historyId, domainId, user) != null;
	}

	/**
	 * Resolves the stored history file below the configured history root after all access checks.
	 */
	public static IwcmFile getFileHistorySourceFile(String fileUrl, String historyPath, long historyId,
		Integer domainId, Identity user)
	{
		if (historyId < 1 || getFileHistoryCurrentFile(fileUrl, domainId, user) == null ||
			isSafeVirtualPath(historyPath) == false || historyPath.endsWith("/") == false) return null;

		String historySourceFolder = getHistorySourceFolder(historyPath);
		if (historySourceFolder == null || user.isFolderWritable(historySourceFolder) == false) return null;

		CanonicalFile canonicalFile = resolveCanonicalFile(historyPath + historyId,
			Constants.getString("fileHistoryPath"));
		return canonicalFile == null ? null : canonicalFile.file;
	}

	private static String getParentFolder(String fileUrl)
	{
		String normalizedPath = Tools.replace(fileUrl, "//", "/");
		int slash = normalizedPath.lastIndexOf('/');
		if (slash < 0) return null;
		if (slash == 0) return "/";
		return normalizedPath.substring(0, slash);
	}

	private static String getHistorySourceFolder(String historyPath)
	{
		String historyRoot = Constants.getString("fileHistoryPath");
		if (isSafeVirtualPath(historyRoot) == false) return null;

		while (historyRoot.length() > 1 && historyRoot.endsWith("/"))
		{
			historyRoot = historyRoot.substring(0, historyRoot.length() - 1);
		}

		String normalizedRoot = Tools.replace(historyRoot, "//", "/");
		String normalizedHistoryPath = Tools.replace(historyPath, "//", "/");
		if (normalizedHistoryPath.equals(normalizedRoot + "/")) return "/";
		if (normalizedHistoryPath.startsWith(normalizedRoot + "/") == false) return null;

		String sourceFolder = normalizedHistoryPath.substring(normalizedRoot.length());
		return isSafeVirtualPath(sourceFolder) ? sourceFolder : null;
	}

	private static boolean isSafeVirtualPath(String path)
	{
		if (Tools.isEmpty(path) || path.startsWith("/") == false || path.indexOf('\\') != -1 ||
			FileBrowserTools.hasForbiddenSymbol(path)) return false;

		for (int i = 0; i < path.length(); i++)
		{
			if (Character.isISOControl(path.charAt(i))) return false;
		}
		return true;
	}

	private static String getTopLevelRoot(String virtualPath)
	{
		int slash = virtualPath.indexOf('/', 1);
		return slash < 0 ? "/" : virtualPath.substring(0, slash);
	}

	private static CanonicalFile resolveCanonicalFile(String virtualPath, String rootVirtualPath)
	{
		String realPath = Tools.getRealPath(virtualPath);
		String realRoot = Tools.getRealPath(rootVirtualPath);
		if (Tools.isEmpty(realPath) || Tools.isEmpty(realRoot)) return null;

		String canonicalPath = new IwcmFile(realPath).getCanonicalPath();
		String canonicalRoot = new IwcmFile(realRoot).getCanonicalPath();
		if (Tools.isEmpty(canonicalPath) || Tools.isEmpty(canonicalRoot)) return null;

		String relativeVirtualPath = getRelativeVirtualPath(canonicalPath, canonicalRoot);
		if (relativeVirtualPath == null) return null;

		String normalizedRoot = rootVirtualPath;
		while (normalizedRoot.length() > 1 && normalizedRoot.endsWith("/"))
		{
			normalizedRoot = normalizedRoot.substring(0, normalizedRoot.length() - 1);
		}
		String containedVirtualPath = "/".equals(normalizedRoot) ? relativeVirtualPath :
			"/".equals(relativeVirtualPath) ? normalizedRoot : normalizedRoot + relativeVirtualPath;

		return new CanonicalFile(new IwcmFile(canonicalPath), containedVirtualPath);
	}

	private static String getRelativeVirtualPath(String filePath, String rootPath)
	{
		try
		{
			Path file = Paths.get(filePath).toAbsolutePath().normalize();
			Path root = Paths.get(rootPath).toAbsolutePath().normalize();
			if (file.startsWith(root) == false) return null;

			String relativePath = root.relativize(file).toString().replace(File.separatorChar, '/');
			return Tools.isEmpty(relativePath) ? "/" : "/" + relativePath;
		}
		catch (RuntimeException ex)
		{
			Logger.debug(FileHistoryDB.class, "Invalid canonical file-history path: " + filePath);
			return null;
		}
	}

	private static class CanonicalFile
	{
		private final IwcmFile file;
		private final String virtualPath;

		private CanonicalFile(IwcmFile file, String virtualPath)
		{
			this.file = file;
			this.virtualPath = virtualPath;
		}
	}

	private static void writeFileOut(IwcmFile f, HttpServletResponse res) throws IOException
	{
		IwcmInputStream fis = null;
		ServletOutputStream out = null;
		try
		{
			out = res.getOutputStream();
			byte[] buff = new byte[64000];
			fis = new IwcmInputStream(f);
			int len;
			while ((len = fis.read(buff)) != -1)
			{
				out.write(buff, 0, len);
			}

		}
		finally
		{
			if (fis != null) fis.close();
			if(out != null) out.close();
		}
	}


	public List<FileHistoryBean> findByFileUrl(String fileUrl)
	{
		return JpaTools.findByMatchingProperty(FileHistoryBean.class, "fileUrl", fileUrl);
	}

	public FileHistoryBean findFirstByFileUrl(String fileUrl)
	{
		return JpaTools.findFirstByMatchingProperty(FileHistoryBean.class, "fileUrl", fileUrl);
	}
	public List<FileHistoryBean> findByChangeDate(Date changeDate)
	{
		return JpaTools.findByMatchingProperty(FileHistoryBean.class, "changeDate", changeDate);
	}

	public FileHistoryBean findFirstByChangeDate(Date changeDate)
	{
		return JpaTools.findFirstByMatchingProperty(FileHistoryBean.class, "changeDate", changeDate);
	}
	public List<FileHistoryBean> findByUserId(int userId)
	{
		return JpaTools.findByMatchingProperty(FileHistoryBean.class, "userId", userId);
	}

	public FileHistoryBean findFirstByUserId(int userId)
	{
		return JpaTools.findFirstByMatchingProperty(FileHistoryBean.class, "userId", userId);
	}

	public List<FileHistoryBean> getHistoryByPath(String virtualPath)
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		List<FileHistoryBean> records = new ArrayList<>();
		try{
			ExpressionBuilder builder = new ExpressionBuilder();
			ReadAllQuery dbQuery = new ReadAllQuery(FileHistoryBean.class, builder);
			if(Tools.isNotEmpty(virtualPath))
			{
				Expression expr = builder.get("fileUrl").equal(virtualPath);
				dbQuery.setSelectionCriteria(expr);
			}
			Expression expr1 = builder.get("changeDate");
			List<Expression> expressions = new ArrayList<>();
			expressions.add(expr1);

			dbQuery.setOrderByExpressions(expressions);

			Query query = em.createQuery(dbQuery);
			records = JpaDB.getResultList(query);
		}catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
		}finally{
			em.close();
		}
		return records;
	}
}
