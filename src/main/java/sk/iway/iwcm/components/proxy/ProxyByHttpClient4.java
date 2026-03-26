package sk.iway.iwcm.components.proxy;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.proxy.jpa.ProxyBean;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.system.context.ContextFilter;
import sk.iway.iwcm.tags.WriteTag;
import sk.iway.iwcm.users.UsersDB;

/**
 *  ProxyByHttpClient.java - proxy vykonane pomocou JakartaHttpClienta, vie modifikovat encoding
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2008
 *@author       $Author: jeeff $
 *@version      $Revision: 1.4 $
 *@created      Date: 10.11.2008 13:43:48
 *@modified     $Date: 2009/09/10 19:59:49 $
 */
@SuppressWarnings("deprecation")
public class ProxyByHttpClient4
{
	//public static String AUTH_STATE_KEY = "ProxyByHttpClient.authStateKey";

	protected ProxyByHttpClient4() {
		//utility class
	}

	public static void service(ProxyBean proxy, HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException
	{
		Logger.debug(ProxyByHttpClient4.class, "ProxyByHttpClient - service");
		String path = null;
		String data = null;
		try
		{
			String originalURI = req.getRequestURI();
			if (ContextFilter.isRunning(req)) originalURI = ContextFilter.removeContextPath(req.getContextPath(), originalURI);

			req.setAttribute("path_filter_orig_path", originalURI);

			String localUrl = ProxyDB.getLocalUrl(proxy, originalURI);
			path = proxy.getRemoteUrl() + originalURI.substring(localUrl.length());

			//path = Tools.replace(path, ".wsp", ".asp");
			Logger.debug(ProxyByHttpClient4.class, "path:"+path);

			req.setAttribute("path_filter_proxy_path", path);

			boolean includeIntoPage = Proxy.isIncludeIntoPage(proxy, path);

			String fullPath = null;
			if (proxy.getRemoteServer().startsWith("http")==false)
			{
				fullPath = "http";
				if (443==proxy.getRemotePort()) {
					fullPath += "s";
				}
				fullPath += "://"+proxy.getRemoteServer() + ":" + proxy.getRemotePort() + path;
			}
			else
			{
				fullPath = proxy.getRemoteServer() + ":" + proxy.getRemotePort() + path;
			}

			//prekoduj na cielove kodovanie
	      	String pathIncludingQuery = fullPath;
			if (req.getQueryString()!=null && req.getQueryString().length()>1)
			{
				pathIncludingQuery = fullPath + "?" + req.getQueryString();
				pathIncludingQuery = Tools.replace(pathIncludingQuery, "{", "%7B");
				pathIncludingQuery = Tools.replace(pathIncludingQuery, "}", "%7D");
			}

			Logger.debug(ProxyByHttpClient4.class, "fullPath:"+fullPath);

			CloseableHttpClient client = null;
			HttpClientContext context = HttpClientContext.create();

			if (client == null)
			{
				client = HttpClients.createDefault();
			}

			if (proxy.getRemoteServer().startsWith("https"))
			{
				//Protocol.registerProtocol("https", new Protocol("https", new EasySSLProtocolSocketFactory(), 443));
			}

			if ("ntlm".equalsIgnoreCase(proxy.getAuthMethod()))
			{
					//client.getParams().setAuthenticationPreemptive(true);
				//CredentialsProvider defaultcreds = new NTCredentials(proxy.getAuthUsername(), proxy.getAuthPassword(), proxy.getAuthHost(), proxy.getAuthDomain());
					//client.getParams().setCredentials(new AuthScope(proxy.getAuthHost(), proxy.getRemotePort(), AuthScope.ANY_REALM), defaultcreds);
				CredentialsProvider credsProvider = new BasicCredentialsProvider();
				credsProvider.setCredentials(AuthScope.ANY, new NTCredentials(proxy.getAuthUsername(), proxy.getAuthPassword(), proxy.getAuthHost(), proxy.getAuthDomain()));
				context.setCredentialsProvider(credsProvider);
				}
			else if ("basic".equalsIgnoreCase(proxy.getAuthMethod()))
			{
				CredentialsProvider credsProvider = new BasicCredentialsProvider();
				credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(proxy.getAuthUsername(), proxy.getAuthPassword()));
				context.setCredentialsProvider(credsProvider);
				}

			if (Tools.isNotEmpty(proxy.getAllowedMethods()))
			{
				if (proxy.getAllowedMethods().toLowerCase().contains(req.getMethod().toLowerCase())==false)
				{
					response.setStatus(HttpServletResponse.SC_FORBIDDEN);
					return;
				}
			}

			//WebJETProxySelector.setProxyForHttpClient(client, fullPath);

			HttpRequestBase method = null;
			if ("GET".equalsIgnoreCase(req.getMethod()))
			{

				Logger.debug(ProxyByHttpClient4.class,"JE TO GET: " + pathIncludingQuery);

				//method = (GetMethod)req.getSession().getAttribute("proxyHttpClientMethod");
				if (method == null)
				{
					method = new HttpGet(pathIncludingQuery);
					//req.getSession().setAttribute("proxyHttpClientMethod", method);
				}
			}
			else
			{
				if (req.getHeader("Content-type")!=null && req.getHeader("Content-type").indexOf("multipart/form-data")!=-1)
				{
					/*
					Logger.debug(ProxyByHttpClient4.class,"JE TO POST (multipart): " + fullPath);
					MultipartPostMethod pmethod = new MultipartPostMethod(fullPath);
					pmethod.setRequestHeader("Content-type", "multipart/form-data;");

					DiskFileUpload fu = new DiskFileUpload();
					// maximum size before a FileUploadException will be thrown
			      fu.setSizeMax(100000000);
			      // maximum size that will be stored in memory
			      fu.setSizeThreshold(0);
			      // the location for saving data that is larger than getSizeThreshold()
			      File uploadDir = new File(Tools.getRealPath("/WEB-INF/tmp/upload-"+Tools.getNow()+"/"));
			      if(uploadDir.mkdirs() == false)
			      	throw new FileUploadException("Unable to create directory "+"/WEB-INF/tmp/upload-"+Tools.getNow()+"/");
			      fu.setRepositoryPath(uploadDir.getPath());

			      List<FileItem> fileItems = fu.parseRequest(req);
			      for (FileItem file : fileItems)
			      {
			      	if (file.isFormField())
			      	{
			      		Logger.debug(ProxyByHttpClient4.class, "Adding parameter: " + file.getFieldName());
			      		String value = file.getString(SetCharacterEncodingFilter.getEncoding());
							Logger.debug(ProxyByHttpClient4.class,"param: " + file.getFieldName()+"="+value);
							//pmethod.addParameter(file.getFieldName(), value);
							StringPart sp = new StringPart(file.getFieldName(), value, proxy.getEncoding());
							//inak nam tam nastavovalo transfer encoding s cim mali ASP uploady problem
							sp.setTransferEncoding(null);
							pmethod.addPart(sp);
			      	}
			      }
			      for (FileItem file : fileItems)
			      {
			      	if (file.isFormField()==false && file instanceof DefaultFileItem)
			      	{
			      		DefaultFileItem dfi = (DefaultFileItem)file;
			      		Logger.debug(ProxyByHttpClient4.class,"Adding FILE: " + file.getFieldName()+"="+dfi.getStoreLocation().getPath());
			      		//pmethod.addParameter(file.getFieldName(), file.getName(), dfi.getStoreLocation());
			      		FilePart fp = new FilePart(file.getFieldName(), file.getName(), dfi.getStoreLocation());
			      		pmethod.addPart(fp);
			      	}
			      }

			      //vymaz subory z tmp adresara

			      method = pmethod;
			      */
				}
				else
				{
					Logger.debug(ProxyByHttpClient4.class,"JE TO POST: " + pathIncludingQuery);

					HttpPost pmethod = new HttpPost(pathIncludingQuery);
					pmethod.setHeader("Content-type", "application/x-www-form-urlencoded; charset="+proxy.getEncoding());

				   Enumeration<String> params = req.getParameterNames();
				   int i;
				   String[] values;
				   String name;
				   List<NameValuePair> nvps = new ArrayList<>();
					while (params.hasMoreElements())
					{
						name = params.nextElement();
						values = req.getParameterValues(name);
						for (i=0; i<values.length; i++)
						{
							Logger.debug(ProxyByHttpClient4.class,"param: " + name+"="+values[i]);
							//pmethod.getParams().setParameter(name, values[i]);
							nvps.add(new BasicNameValuePair(name, values[i]));
						}
					}
					pmethod.setEntity(new UrlEncodedFormEntity(nvps, proxy.getEncoding()));

					method = pmethod;
				}

			}

			if (method != null) {
				//preposli hlavicky
				Enumeration<String> en = req.getHeaderNames();
				while (en.hasMoreElements())
				{
					String key = en.nextElement();
					// Filter incoming headers:
					if ("Host".equalsIgnoreCase(key))
					{
						String value = proxy.getRemoteServer();
						//remove http/s prefix
						if (value.startsWith("http")) {
							value = value.substring(value.indexOf("://")+3);
						}
						Logger.debug(ProxyByHttpClient4.class, "header: " + key + ": " + value);
						method.addHeader(key, value);
					}
					else if ("Connection".equalsIgnoreCase(key) || "If-Modified-Since".equalsIgnoreCase(key) ||
								"If-None-Match".equalsIgnoreCase(key) || "Accept-Encoding".equalsIgnoreCase(key) ||
								"Cookie".equalsIgnoreCase(key) || "content-type".equalsIgnoreCase(key) || "content-length".equalsIgnoreCase(key))
					{
						//tieto preskakujem
						Logger.debug(ProxyByHttpClient4.class, "header: SKIP " + key + ": " + req.getHeader(key));
					}
					else if ("authorization".equalsIgnoreCase(key))
					{
						//nastavujeme dole
						Logger.debug(ProxyByHttpClient4.class, "header: SKIP " + key + ": " + req.getHeader(key));
					}
					else
					{
						String value = req.getHeader(key);
						method.addHeader(key, value);
						Logger.debug(ProxyByHttpClient4.class, "header: " + key + ": " + value);
					}
				}


				Principal principal = req.getUserPrincipal();
				if (principal != null && Tools.isNotEmpty(principal.getName()))
				{
					Logger.debug(ProxyByHttpClient4.class, "header: AUTH_USER_CMS: " + principal.getName());
					method.addHeader("AUTH_USER_CMS", principal.getName());
				}
				else
				{
					Identity user = UsersDB.getCurrentUser(req);
					if (user != null)
					{
						Logger.debug(ProxyByHttpClient4.class, "header: AUTH_USER_CMS: " + user.getLogin());
						method.addHeader("AUTH_USER_CMS", user.getLogin());
					}
				}
			}

			/*
			String authState = (String)req.getSession().getAttribute(AUTH_STATE_KEY);
			Logger.debug(ProxyByHttpClient.class, "authState: "+authState);
			if (authState == null)
			{

			}
			else if ("NTLM".equals(authState))
			{
				String value = "NTLM TlRMTVNTUAABAAAAB4IIogAAAAAAAAAAAAAAAAAAAAAFAs4OAAAADw==";
				Logger.debug(ProxyByHttpClient.class, "header: authorization1: " + value);
				method.addRequestHeader("authorization", value);
			}
			else if (authState.startsWith("NTLM"))
			{
				//String value = Tools.replace(req.getHeader("authorization"), "Negotiate", "NTLM");
				String value = req.getHeader("authorization");
				Logger.debug(ProxyByHttpClient.class, "header: authorization2: " + value);
				method.addRequestHeader("authorization", value);
			}
			*/


			//String responseBody = null;
			CloseableHttpResponse hcResponse = null;
			try{
				//
				hcResponse = client.execute(method, context);
				//responseBody = method.getResponseBodyAsString();
			} catch (IOException ioe){
				Logger.error(ProxyByHttpClient4.class,"Unable to connect to '" + path + "'");
				Logger.error(ProxyByHttpClient4.class,ioe.getMessage());
				sk.iway.iwcm.Logger.error(ioe);
			}

			/**
			Cookie[] cookies = state.getCookies();
	      // Display the cookies
			Logger.debug(ProxyByHttpClient4.class, "STATE Present cookies: ");
	      for (int i = 0; i < cookies.length; i++)
	      {
	      	Logger.debug(ProxyByHttpClient4.class, " - " + cookies[i].toExternalForm());
	      }
	      */

			if (hcResponse != null) {
				Logger.debug(ProxyByHttpClient4.class,"*** Response ***");
				Logger.debug(ProxyByHttpClient4.class,"Status Line: " + hcResponse.getStatusLine());
				Header[] responseHeaders = hcResponse.getAllHeaders();
				Header h;

				for (int i=0; i<responseHeaders.length; i++)
				{
					h = responseHeaders[i];
					//Logger.debug(this,responseHeaders[i]);
					if (h.getName()!=null && h.getValue() != null)
					{
						Logger.debug(ProxyByHttpClient4.class,h.getName()+": "+h.getValue());
					}
					/*
					if (h.getName().indexOf("Authenticate")!=-1)
					{
						response.setStatus(401);
						response.setHeader(h.getName(), h.getValue());
						req.getSession().setAttribute(AUTH_STATE_KEY, h.getValue());
						includeIntoPage = false;
					}
					*/
					if (h.getName().equalsIgnoreCase("Location"))
					{
						String value = h.getValue();
						Logger.debug(ProxyByHttpClient4.class, "Original location: " + value);

						if (value.startsWith("http"))
						{
							value = value.substring(value.indexOf('/', 10));
						}
						//urci nove URL
						if (value.startsWith(proxy.getRemoteUrl())) value = localUrl + value.substring(proxy.getRemoteUrl().length());

						//dopln nasu cestu
						if (value.charAt(0)!='/') value = localUrl + value;

						Logger.debug(ProxyByHttpClient4.class, "Redirecting to: " + value);

						response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
						response.setHeader(h.getName(), value);
					}
				}

				HttpEntity entity = hcResponse.getEntity();

				if (includeIntoPage)
				{
					//vkladam vystup do WebJETu

					StringBuilder sb = new StringBuilder();
					BufferedInputStream is = new BufferedInputStream(entity.getContent());
					InputStreamReader in = new InputStreamReader(is, proxy.getEncoding());
					char[] buffer = new char[8000];
					int n = 0;
					while (true)
					{
						n = in.read(buffer);
						if (n < 1) break;
						sb.append(buffer, 0, n);
					}
					in.close();
					data = sb.toString();

					req.setAttribute("proxyOutputDataNoCrop", data);

					//Logger.debug(ProxyByHttpClient4.class, "Proxy response:\n"+data);

					if (Tools.isNotEmpty(proxy.getCropStart()) && Tools.isNotEmpty(proxy.getCropEnd()))
					{
						try
						{
							if (proxy.isKeepCropEnd()==false) data = ProxyDB.getCleanBodyIncludeStartNoEnd(data, proxy.getCropStart(), proxy.getCropEnd());
							else data = ProxyDB.getCleanBodyIncludeStartEnd(data, proxy.getCropStart(), proxy.getCropEnd());

							if (proxy.isKeepCropStart()==false) data = data.substring(proxy.getCropStart().length());
						}
						catch (Exception ex)
						{
							sk.iway.iwcm.Logger.error(ex);
						}
					}

					req.setAttribute("proxyOutputData", data);

					//ziskaj docid
					//stranka ma nastavenu takuto virtualnu cestu
					DocDB docDB = DocDB.getInstance();

					//kvoli sharepointu kde mapujeme priecinky podla roznych parametrov #16338
					String testURL = originalURI+"?"+req.getQueryString();
					int docId = docDB.getVirtualPathDocId(testURL, DocDB.getDomain(req));
					//musime spravit toto inak sa nam vrati * URL a nikdy sa nevojde do bloku docId < 1
					DocDetails testDoc = docDB.getBasicDocDetails(docId, false);
					if (testDoc != null && testDoc.getVirtualPath().indexOf("*")!=-1) docId = -1;

					Logger.debug(ProxyByHttpClient4.class, "Tested URL "+testURL+" returned docId="+docId);

					if (docId < 1)
					{
						testURL = originalURI+"?RootFolder="+Tools.replace(Tools.URLEncode(req.getParameter("RootFolder")), "_", "%5F");
						docId = docDB.getVirtualPathDocId(testURL, DocDB.getDomain(req));

						testDoc = docDB.getBasicDocDetails(docId, false);
						if (testDoc != null && testDoc.getVirtualPath().indexOf("*")!=-1) docId = -1;

						Logger.debug(ProxyByHttpClient4.class, "Tested URL "+testURL+" returned docId="+docId);
					}
					if (docId < 1)
					{
						testURL = originalURI;
						docId = docDB.getVirtualPathDocId(testURL, DocDB.getDomain(req));
						Logger.debug(ProxyByHttpClient4.class, "Tested URL "+testURL+" returned docId="+docId);
					}

					String wjforward = req.getParameter("wjforward");
					if (Tools.isNotEmpty(wjforward) && wjforward.startsWith("/components") && wjforward.indexOf("search")!=-1 && wjforward.indexOf("ajax")!=-1 && wjforward.endsWith(".jsp"))
					{
						testURL = wjforward;

						testURL = WriteTag.getCustomPage(testURL, req);

						Logger.debug(ProxyByHttpClient4.class, "Forwarding to: " + testURL );
						req.getRequestDispatcher(testURL).forward(req, response);
					}
					else
					{
						Logger.debug(ProxyByHttpClient4.class, "Forwarding to: " + testURL + " (" + docId + ")");
						req.getRequestDispatcher("/showdoc.do?docid="+docId).forward(req, response);
					}
				}
				else
				{
					response.setContentType(entity.getContentType().getValue());

					copyStream(entity.getContent(), response.getOutputStream());
				}

				/*
				*
				state = client.getState();
				req.getSession().setAttribute(STATE_KEY, state);
				*/

				//clean up the connection resources
				hcResponse.close();
			}
			if (method != null) method.releaseConnection();
			client.close();

			req.setAttribute("proxyOutputData", data);
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Socket opening: " + proxy.getRemoteServer() + ":" + proxy.getRemotePort()+path);
		}
	}

	private static void copyStream(InputStream in, OutputStream out) throws IOException
	{
		BufferedInputStream bin = new BufferedInputStream(in);
		Logger.debug(ProxyByHttpClient4.class, "REQUEST: ------------------------------");
		StringBuilder sb = new StringBuilder();
		byte[] buff = new byte[8000];
		int len;
		while ((len = bin.read(buff)) != -1)
		{
			Logger.debug(ProxyByHttpClient4.class, "Writing "+len+" bytes");
			out.write(buff, 0, len);
			sb.append(new String(buff, 0, len));
		}
		Logger.debug(ProxyByHttpClient4.class, sb.toString());
		Logger.debug(ProxyByHttpClient4.class, "REQUEST KONIEC-------------------------");
	}
}
