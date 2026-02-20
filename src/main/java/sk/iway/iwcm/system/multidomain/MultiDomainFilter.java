package sk.iway.iwcm.system.multidomain;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Map;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.io.FileCache;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmFsDB;
import sk.iway.iwcm.users.UsersDB;

/**
 *  MultiDomainFilter.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2008
 *@author       $Author: jeeff $
 *@version      $Revision: 1.11 $
 *@created      Date: 24.10.2008 13:48:14
 *@modified     $Date: 2010/01/26 14:09:53 $
 */
public class MultiDomainFilter implements Filter
{
	private static Map<String, String> domainMapping = null;
	private static String[] MULTI_DIRS_LAZY = null; //NOSONAR

	@Override
	public void init(FilterConfig config) throws ServletException
	{
		//
	}

	public static String[] getMultiDirs()
	{
		if (MULTI_DIRS_LAZY == null)
		{
			MULTI_DIRS_LAZY = Constants.getString("multiDomainFolders").split(",");
		}
		//toto tu kvoli performance nechcem - return MULTI_DIRS_LAZY == null ? null : MULTI_DIRS_LAZY.clone();
		return MULTI_DIRS_LAZY;
	}

	/**
	 * Vykona premapovanie suboru na domenovu verziu, napr. http://interway.sk/images/logo.gif zmeni na http://interway.sk/images/interway/logo.gif
	 */
	 @Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
      HttpServletRequest req = (HttpServletRequest)request;
      String path = req.getRequestURI();

      String domainAlias = getDomainAlias(DocDB.getDomain(req));
      String rewrite = rewriteUrlToLocal(path, domainAlias);

      if (path.startsWith("/files/")==false)
	   {
      	//test pre VUB
         boolean useDbStorage = IwcmFsDB.useDBStorage(rewrite);
         Integer fatId = null;
         if (useDbStorage) fatId = IwcmFsDB.getFatIdTable().get(rewrite);
         if (useDbStorage && fatId != null)// ak existuje v storage
   		{
   			String mimeType = Constants.getServletContext().getMimeType(rewrite.toLowerCase());
   			if (Tools.isEmpty(mimeType)) mimeType = "application/octet-stream";
   			response.setContentType(mimeType);

            PathFilter.setDownloadHeaders(rewrite, req, (HttpServletResponse)response);
   			PathFilter.setStaticContentHeaders(rewrite, UsersDB.getCurrentUser(req), req, (HttpServletResponse)response);

   			byte[] data = null;
   			if (FileCache.useFileCache()) data = FileCache.getObject(rewrite, fatId.intValue());
   			if (data != null)
   			{
   				response.setContentLength(data.length);

   				 OutputStream out=response.getOutputStream();
   				 out.write(data);
   				 out.flush();
   				 out.close();
   			}
   			else
   			{
   				File file = new File(Tools.getRealPath(rewrite));

   				IwcmFile iwcmFile = new IwcmFile(file);

   				response.setContentLength((int)iwcmFile.length());

   				data = IwcmFsDB.writeFileToOutputStream(file, response.getOutputStream());
   				if (data.length > 0 && FileCache.useFileCache())
   				{
   					FileCache.setObject(rewrite, data);
   				}
   			}

   			return;
   		}
 	   }

      if (path.startsWith("/images/gallery/user/"))
      {
      	//otestuj ci existuje rewrite subor (pretoze defaultne sa user fotka nahrava do /images/gallery/user bez ohladu na domenu)
      	IwcmFile f = new IwcmFile(Tools.getRealPath(rewrite));
      	if (f.exists()==false)
      	{
      		rewrite = path;
      	}
      }

   	if (Tools.isNotEmpty(domainAlias) && path.equals(rewrite)==false)
   	{
      	//Logger.debug(MultiDomainFilter.class, "rewrite="+rewrite);

      	WrappedRequest wRequest = new WrappedRequest(req, rewrite);

         //wRequest.getRequestDispatcher(wRequest.getServletPath()).forward(wRequest, response);
         chain.doFilter(wRequest, response);

         return;
   	}
     	chain.doFilter(request, response);
	}

	@Override
	public void destroy() {
		//
	}


	/**
	 * Prepise URL na cestu k lokalnemu suboru (alebo ponech tak, ak to nie je multidomain)
	 * @param path
	 * @param req
	 * @return
	 */
	public static String rewriteUrlToLocal(String path, HttpServletRequest req)
	{
		return rewriteUrlToLocal(path, getDomainAlias(DocDB.getDomain(req)));
	}

	/**
	 * Prepise URL na cestu k lokalnemu suboru (alebo ponech tak, ak to nie je multidomain)
	 * @param path
	 * @param domainAlias
	 * @return
	 */
	public static String rewriteUrlToLocal(String path, String domainAlias)
	{
		if (Tools.isNotEmpty(domainAlias))
   	{
			for (String dir : getMultiDirs())
	      {
		      if (path.startsWith("/"+dir+"/"))
		      {
		      	if (path.startsWith("/"+dir+"/"+domainAlias+"/")==false && path.startsWith("/"+dir))
	      		{
		      		return "/"+dir+"/"+domainAlias+path.substring(dir.length()+1);
	      		}

		      }
	      }
   	}
		return path;
	}

	/**
	 * Prepise URL v zadanom texte na vonkajsie hodnoty (bez domain aliasov)
	 * @param text
	 * @param request
	 * @return
	 */
	public static String fixDomainPaths(String text, HttpServletRequest request)
	{
		if (Tools.isEmpty(text)) return text;

		String domainAlias = MultiDomainFilter.getDomainAlias(DocDB.getDomain(request));
		if (Tools.isNotEmpty(domainAlias))
		{
			for (String dir : getMultiDirs())
			{
				text = Tools.replace(text, "\"/"+dir+"/"+domainAlias+"/", "\"/"+dir+"/");
				text = Tools.replace(text, "'/"+dir+"/"+domainAlias+"/", "'/"+dir+"/");
				text = Tools.replace(text, "(/"+dir+"/"+domainAlias+"/", "(/"+dir+"/");

				//toto akceptuje len String so zaciatocnym URL (chcem replace URL)
				if (text.startsWith("/"+dir+"/"+domainAlias+"/"))
				{
					text = Tools.replace(text, "/"+dir+"/"+domainAlias+"/", "/"+dir+"/");
				}
			}
		}
		return text;
	}

	/**
	 * Prepise URL v zadanom texte na vonkajsie hodnoty (bez domain aliasov)
	 * @param text
	 * @param request
	 * @return
	 */
	public static StringBuilder fixDomainPaths(StringBuilder text, HttpServletRequest request)
	{
		if (Tools.isEmpty(text)) return text;

		String domainAlias = MultiDomainFilter.getDomainAlias(DocDB.getDomain(request));
		if (Tools.isNotEmpty(domainAlias))
		{
			for (String dir : getMultiDirs())
			{
				text = Tools.replace(text, "\"/"+dir+"/"+domainAlias+"/", "\"/"+dir+"/");
				text = Tools.replace(text, "'/"+dir+"/"+domainAlias+"/", "'/"+dir+"/");
				text = Tools.replace(text, "(/"+dir+"/"+domainAlias+"/", "(/"+dir+"/");

				//toto akceptuje len String so zaciatocnym URL (chcem replace URL)
				if (text.toString().startsWith("/"+dir+"/"+domainAlias+"/"))
				{
					text = Tools.replace(text, "/"+dir+"/"+domainAlias+"/", "/"+dir+"/");
				}
			}
		}
		return text;
	}

	/**
	 * Vrati alias pre zadanu domenu (aby sa pri vyvoji dali ukladat obrazky do adresarov)
	 * aliasy su v konfiguracii ako kluc domainAlias:DOMENA
	 * @param domain
	 * @return
	 */
	public static String getDomainAlias(String domain)
	{
		if (domain == null) return "";
		if (domainMapping == null)
		{
			domainMapping = new Hashtable<>();
		}
		String alias = domainMapping.get(domain);
		if (alias == null)
		{
			alias = Constants.getString("multiDomainAlias:"+domain);
			if (alias == null) alias = "";
			domainMapping.put(domain, alias);
		}
		return alias;
	}

	/**
	 * Vymaze hash tabulku aliasov (pri zmene konfiguracie)
	 */
	public static void clearDomainAlias()
	{
		domainMapping = new Hashtable<>();
	}

	public static void clearDomainFolders() { MULTI_DIRS_LAZY = null; }
}
