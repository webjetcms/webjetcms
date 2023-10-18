package sk.iway.iwcm.io;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.AdminTools;
import sk.iway.iwcm.common.FilePathTools;
import sk.iway.iwcm.filebrowser.EditForm;
import sk.iway.iwcm.sync.FileBean;
import sk.iway.iwcm.system.context.ContextFilter;
import sk.iway.iwcm.users.UsersDB;

;
/**
 * IwcmFsFilter.java
 *
 * @Title webjet4
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2008
 * @author $Author: jeeff $
 * @version $Revision: 1.11 $
 * @created Date: 25.6.2008 16:13:33
 * @modified $Date: 2010/02/17 11:35:15 $
 */
public class IwcmFsFilter implements Filter
{
	 @Override
	public void destroy()
	{
		//
	}
	 @Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException
	{
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		String path = request.getRequestURI();
		if (ContextFilter.isRunning(request)) path = ContextFilter.removeContextPath(request.getContextPath(), path);

		//Logger.debug(IwcmFsFilter.class, "path="+path);

		//fix pre VUB - zobrazoval sa im zdrojovy kod pre volania index.JSP
		if (Tools.isNotEmpty(path))
		{
			if (path.toLowerCase().endsWith(".jsp") && path.endsWith(".jsp")==false)
			{
				response.setHeader("Location", path.toLowerCase());
				response.setStatus(301);
				return;
			}
		}


		boolean useDbStorage = IwcmFsDB.useDBStorage(path);
		if (!useDbStorage)
		{
			chain.doFilter(req, res);
			return;
		}

		//kvoli vykonu volame len pre /files/
		if (path.startsWith("/files/"))
		{
			path = Tools.URLDecode(path);

			//mame nan prava?
			EditForm ef = PathFilter.isPasswordProtected(path, request);
			if (ef != null && !ef.isAccessibleFor(UsersDB.getCurrentUser(request)) &&
						PathFilter.doFileForbiddenRedirect(ef, UsersDB.getCurrentUser(request), path, request, response)
			)
			{
				return;
			}
		}

		Integer fatId = IwcmFsDB.getFatIdTable().get(path);
		if (useDbStorage && fatId != null)// ak existuje v storage
		{
			//ziskanie suboru z historie
			long fid = Tools.getLongValue(request.getParameter("fid"), -1);
			int fatIdHistory = -1;
			if (fid > 0)
			{
				Identity user = UsersDB.getCurrentUser(request);
				if (user != null && user.isAdmin())
				{
					List<FileBean> versionList = AdminTools.getVersionList(path);
					//over ci taka verzia existuje
					for (FileBean version : versionList)
					{
						if (version.getLocalLastModified() == fid)
						{
							//nasli sme, mozeme poslat na vystup
							fatIdHistory = (int)fid;
							break;
						}
					}
				}
			}


			File file = new File(Tools.getRealPath(path));
			if (fatIdHistory < 1)
			{
				// ak mozeme zapisovat na disk
				if (Constants.getBoolean("iwfs_writeToDisk"))
				{
					long lastModffied = 0;
					if (IwcmFsDB.getModifiedTable().get(path) != null)
					{
						lastModffied = IwcmFsDB.getModifiedTable().get(path);
					}

					if (file.exists() && round(file.lastModified()) == round(lastModffied))
					{
						// na disku je ten isty subor ako v DB
						chain.doFilter(req, res);
						return;
					}
					else
					{
						if (file.exists())
						{
							if (round(file.lastModified()) < round(lastModffied))
							{
								// v storage je novsi
								IwcmFsDB.writeFileToDisk(file, file, true);

								//nastav rovnaky lastModified
								file.setLastModified(lastModffied);

								chain.doFilter(req, res);
								return;
							}
							if (Constants.getBoolean("iwfs_writeToDB") && round(file.lastModified()) > round(lastModffied))
							{
								// na disku je novsi
								IwcmFsDB.writeFileToDB(file);
								chain.doFilter(req, res);
								return;
							}
						}
						else
						{
							IwcmFsDB.writeFileToDisk(file);
						}
					}
				}
				else
				{
					if (file.exists())
					{
						long lastModffied = 0;
						if (IwcmFsDB.getModifiedTable().get(path) != null)
						{
							lastModffied = IwcmFsDB.getModifiedTable().get(path);
						}
						if (round(file.lastModified()) == round(lastModffied))
						{
							chain.doFilter(req, res);
							return;
						}
					}
				}
			}

			String mimeType = Constants.getServletContext().getMimeType(path.toLowerCase());
			if (Tools.isEmpty(mimeType)) mimeType = "application/octet-stream";
			response.setContentType(mimeType);

         PathFilter.setDownloadHeaders(path, request, response);

			if (fatIdHistory < 1) PathFilter.setStaticContentHeaders(path, UsersDB.getCurrentUser(request), request, response);

			byte data[] = null;
			if (fatIdHistory < 1) data = FileCache.getObject(path);
			if (data != null)
			{
				response.setContentLength(data.length);

				 OutputStream out=response.getOutputStream();
				 out.write(data);
				 out.close();
			}
			else
			{
				IwcmFile iwcmFile = new IwcmFile(file);

				if (fatIdHistory > 0)
				{
					data = IwcmFsDB.writeFileToOutputStreamFromHistory(file, response.getOutputStream(), fatIdHistory);
				}
				else
				{
					//RequestDump dump = new RequestDump(request);
					//Logger.debug(IwcmFsFilter.class, "Serving file "+path+" request:\n"+dump.completeRequestReport());
					if (path.startsWith("/files/") || path.endsWith("mp3") || path.endsWith(".mp4"))
					{
						//toto ani nejde do cache, mp3/4 kvoli range requestom na mp3/mp4 subory
						FilePathTools.writeFileOut(path, request, response);
					}
					else
					{
						response.setContentLength((int) iwcmFile.length());
						data = IwcmFsDB.writeFileToOutputStream(file, response.getOutputStream());
					}
				}

				if (data!=null && fatIdHistory < 1 && data.length > 0)
				{
					FileCache.setObject(path, data);
				}
			}

			return;
		}

		//ak je zapnute storage s disk cache a subor nie je vo FAT tabulke tak vrat 404
		if (useDbStorage && fatId == null && Constants.getBoolean("iwfs_writeToDisk")) {
			String qs = request.getQueryString();
			req.setAttribute("path_filter_query_string", qs);
			req.setAttribute("path_filter_orig_path", path);

			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			request.getRequestDispatcher("/404.jsp").forward(req, res);
			return;
		}

		chain.doFilter(req, res);
	}
	 @Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
		//
	}

	private long round(long i)
	{
		return new BigDecimal(Math.round(new BigDecimal(i).divide(new BigDecimal(100)).doubleValue()))
					.multiply(new BigDecimal(100)).longValue();
	}
}
