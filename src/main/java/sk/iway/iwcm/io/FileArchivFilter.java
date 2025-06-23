package sk.iway.iwcm.io;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.file_archiv.FileArchivSupportMethodsService;
import sk.iway.iwcm.components.file_archiv.FileArchivatorBean;
import sk.iway.iwcm.components.file_archiv.FileArchivatorDB;
import sk.iway.iwcm.components.file_archiv.FileArchivatorKit;
import sk.iway.iwcm.filebrowser.EditForm;
import sk.iway.iwcm.system.context.ContextFilter;
import sk.iway.iwcm.tags.support.ResponseUtils;
import sk.iway.iwcm.users.UsersDB;


public class FileArchivFilter implements Filter
{
	/**
	 * Filter kontrolu dostupnost suboru z file archivu na zaklade datumov platnosti a vidielnosti
	 */
	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;

		String path = request.getRequestURI();
		if(ContextFilter.isRunning(request))
			path = ContextFilter.removeContextPath(request.getContextPath(), path);
		path = Tools.URLDecode(path);

		String faArchivPathPrefix = FileArchivSupportMethodsService.normalizePath( FileArchivatorKit.getArchivPath() );
		if(path.startsWith(faArchivPathPrefix)) {
			if(isPasswordProtected(path,request,response)) {
				return;
			}

			IwcmFile file = new IwcmFile(Tools.getRealPath(path));
			//if file doen't exist, process to PathFilter for standard handling and URL checking
			if (file.exists()) {
				//getByUrl also check for file validity dates and visibility
				FileArchivatorBean validateFile = FileArchivatorDB.getByUrl(path);
				if(validateFile == null)
				{
					Logger.debug(this,"File not found, or is not accsessable or valid dates: "+path);
					String qs=ResponseUtils.filter(request.getQueryString());
					if(qs == null) {
						qs="";
					}
					req.setAttribute("path_filter_query_string",qs);
					req.setAttribute("path_filter_orig_path",path);
					response.setStatus(HttpServletResponse.SC_NOT_FOUND);
					request.getRequestDispatcher("/404.jsp").forward(req,res);
					return;
				} else if (Tools.isFalse(validateFile.getIndexFile())) {
					response.setHeader("X-Robots-Tag","noindex, nofollow");
				}
			}
		}
		chain.doFilter(req,res);
	}

	/**
	 * prebrate z IwcmFsFilter - Overi ci je subor z file archivu zabezpeceny heslom a ci je pristupny pre prihlaseneho pouzivatela
	 */
	private boolean isPasswordProtected(String path, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		EditForm ef = PathFilter.isPasswordProtected(path, request);
		return (ef != null && !ef.isAccessibleFor(UsersDB.getCurrentUser(request)) &&
					PathFilter.doFileForbiddenRedirect(ef, UsersDB.getCurrentUser(request), path, request, response));
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		//nothing to do
	}

	@Override
	public void destroy() {
		//nothing to do
	}
}
