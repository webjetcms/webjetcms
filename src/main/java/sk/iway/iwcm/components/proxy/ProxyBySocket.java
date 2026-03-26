package sk.iway.iwcm.components.proxy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Enumeration;
import java.util.StringTokenizer;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.proxy.jpa.ProxyBean;
import sk.iway.iwcm.doc.DocDB;

/**
 *  ProxyBySocket.java - proxy pomocou priameho soketoveho spojenia (nemodifikuje request)
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2008
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      Date: 10.11.2008 14:12:43
 *@modified     $Date: 2009/04/01 09:11:40 $
 */
public class ProxyBySocket
{
	private static final String CRLF = "\r\n";

	protected ProxyBySocket() {
		//utility class
	}

	/**
	 * Vykona proxy podla aktualneho URL
	 * @param req
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	public static void service(ProxyBean proxy, HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException
	{
		Logger.debug(ProxyBySocket.class, "Proxy - service");
		//
		// Connect to "remote" server:
		Socket sock = null;
		OutputStream out = null;
		InputStream in = null;
		String path = null;
		//
		try
		{
			String originalURI = req.getRequestURI();

			String localUrl = ProxyDB.getLocalUrl(proxy, originalURI);
			path = proxy.getRemoteUrl() + originalURI.substring(localUrl.length());

			sock = new Socket(proxy.getRemoteServer(), proxy.getRemotePort()); // !!!!!!!!
			out = new BufferedOutputStream(sock.getOutputStream());
			in = new BufferedInputStream(sock.getInputStream());

			// Build up a HTTP request from pure strings:
			StringBuilder sb = new StringBuilder(200);
			sb.append(req.getMethod());
			sb.append(' ');


			req.setAttribute("path_filter_orig_path", originalURI);
			req.setAttribute("path_filter_proxy_path", path);

			boolean includeIntoPage = Proxy.isIncludeIntoPage(proxy, path);

			String qs = req.getQueryString();

			sb.append(path);
			if (qs != null)
			{
				sb.append('?');
				appendCleaned(sb, qs);
			}
			sb.append(' ');
			sb.append("HTTP/1.0");
			sb.append(CRLF);
			Logger.println(ProxyBySocket.class, sb.toString());
			out.write(sb.toString().getBytes());

			Enumeration<String> en = req.getHeaderNames();
			while (en.hasMoreElements())
			{
				String key = en.nextElement();
				// Filter incoming headers:
				if ("Host".equalsIgnoreCase(key))
				{
					sb.setLength(0);
					sb.append(key);
					sb.append(": ");
					sb.append(proxy.getRemoteServer());
					sb.append(CRLF);
					Logger.println(ProxyBySocket.class, "header: " + key + ": " + sb + " orig=" + req.getHeader(key));
					out.write(sb.toString().getBytes());
				}
				else if ("Connection".equalsIgnoreCase(key) || "If-Modified-Since".equalsIgnoreCase(key) ||
							"If-None-Match".equalsIgnoreCase(key) || "Accept-Encoding".equalsIgnoreCase(key))
				{
					//tieto preskakujem
					Logger.println(ProxyBySocket.class, "header: SKIP " + key + ": " + req.getHeader(key));
				}
				else
				{
					sb.setLength(0);
					sb.append(key);
					sb.append(": ");
					sb.append(req.getHeader(key));
					sb.append(CRLF);
					Logger.println(ProxyBySocket.class, "header: " + key + ": " + req.getHeader(key));
					out.write(sb.toString().getBytes());
				}
			}
			// Finish request header by an empty line
			out.write(CRLF.getBytes());

			// Copy post data
			InputStream inr = req.getInputStream();
			copyStream(inr, out);
			out.flush();
			Logger.println(ProxyBySocket.class, "Remote request finished. Reading answer.");
			// Now we have finished the outgoing request.
			// We'll now see, what is coming back from remote:
			// Get the answer, treat its header and copy the stream data:
			if (treatHeader(proxy, in, req, response))
			{
				Logger.println(ProxyBySocket.class, "getting response");

				if (includeIntoPage)
				{
					//vkladam vystup do WebJETu

					// if ( debugFlag ) res.setContentType("text/plain");
					//out = res.getOutputStream();
					String outputData = fixData(proxy, originalURI, getStreamResponse(in));
					req.setAttribute("proxyOutputData", outputData);

					//ziskaj docid
					//stranka ma nastavenu takuto virtualnu cestu
					int docId = DocDB.getInstance().getVirtualPathDocId(originalURI, DocDB.getDomain(req));
					Logger.debug(ProxyBySocket.class, "Forwarding to: " + originalURI+" ("+docId+")");
					req.getRequestDispatcher("/showdoc.do?docid="+docId).forward(req, response);
				}
				else
				{
					copyStream(in, response.getOutputStream());
				}
			}
			else
			{
				Logger.println(ProxyBySocket.class, "header not treat");
			}
		}
		catch (IOException e)
		{
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Socket opening: " + proxy.getRemoteServer() + ":" + proxy.getRemotePort()+path);
		}
		finally
		{
			try
			{
				// out.close();
				if(in != null)	in.close();
				if(sock != null) sock.close();
			}
			catch (IOException ignore)
			{
				Logger.println(ProxyBySocket.class, "Exception ignore " + ignore);
			}
		}

	}

	/**
	 * Vykona upravy na vyslednom html kode
	 * @param proxy
	 * @param originalURI
	 * @param data
	 * @return
	 */
	private static String fixData(ProxyBean proxy, String originalURI, String data)
	{
		if (Tools.isNotEmpty(proxy.getCropStart()) && Tools.isNotEmpty(proxy.getCropEnd()) &&
			 data.indexOf(proxy.getCropStart())!=-1 && data.indexOf(proxy.getCropEnd())!=-1)
		{
			try
			{

				data = ProxyDB.getCleanBodyIncludeStartNoEnd(data, proxy.getCropStart(), proxy.getCropEnd());
				//odstran zaciatok
				data = data.substring(data.indexOf(">", proxy.getCropStart().length()-1)+1);
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}
		}

		data = replaceLinks(proxy, originalURI, data);
		return data;
	}

	/**
	 * Nahradi linky vo vystupnom HTML subore
	 * @param proxy
	 * @param data
	 * @return
	 */
	private static String replaceLinks(ProxyBean proxy, String originalURI, String data)
	{
		data = data.replace("http://"+proxy.getRemoteServer(), "");
		data = data.replace("https://"+proxy.getRemoteServer(), "");
		data = data.replace("='"+proxy.getRemoteUrl(), "='"+proxy.getLocalUrl());
		data = data.replace("=\""+proxy.getRemoteUrl(), "=\""+proxy.getLocalUrl());

		return data;
	}

	private static void appendCleaned(StringBuilder sb, String str)
	{
		for (int i = 0; i < str.length(); i++)
		{
			char ch = str.charAt(i);
			if (ch == ' ')
				sb.append("%20");
			else
				sb.append(ch);
		}
	}

	/**
	 * Forward and filter header from backend Request.
	 */
	private static boolean treatHeader(ProxyBean proxy, InputStream in, HttpServletRequest req, HttpServletResponse response) throws ServletException
	{
		boolean retval = true;
		byte[] lineBytes = new byte[4096];
		int len;
		String line;
		try
		{
			// Read the first line of the request.
			len = readLine(in, lineBytes);
			if (len == -1 || len == 0) throw new ServletException("No Request found in Data.");

			String line2 = new String(lineBytes, 0, len);
			Logger.println(ProxyBySocket.class, "head: " + line2 + " " + len);

			// We mainly skip the header by the foreign server
			// assuming, that we can handle protocoll mismatch or so!
			//res.setHeader("viaJTTP", "JTTP");
			// Some more headers require special care ....
			boolean firstline = true;
			// Shortcut evaluation skips the read on first time!
			while (firstline || ((len = readLine(in, lineBytes)) > 0))
			{
				line = new String(lineBytes, 0, len);
				int colonPos = line.indexOf(':');
				if (firstline && colonPos == -1)
				{
					// Special first line considerations ...
					String[] headl = wordStr(line);
					Logger.println(ProxyBySocket.class, "head: " + line + " " + headl.length);
					try
					{
						//toto ohandluj nejako inak
						//response.setStatus(Integer.parseInt(headl[1]));
					}
					catch (NumberFormatException ignore)
					{
						Logger.println(ProxyBySocket.class, "ID exception: " + headl);
					}
					catch (Exception panik)
					{
						Logger.println(ProxyBySocket.class, "First line invalid!");
						return true;
					}
				}
				else if (colonPos != -1)
				{
					String head = line.substring(0, colonPos);
					// Skip LWS (what is LWS)
					int i = colonPos + 1;
					while (isLWS(line.charAt(i)))
						i++;
					String value = line.substring(i);
					Logger.println(ProxyBySocket.class, "<" + head + ">=<" + value + ">");
					if (head.equalsIgnoreCase("Location"))
					{
						Logger.println(ProxyBySocket.class, "Original location: " + value);

						if (value.startsWith("http"))
						{
							value = value.substring(value.indexOf('/', 10));
						}
						//urci nove URL
						if (value.startsWith(proxy.getRemoteUrl())) value = proxy.getLocalUrl() + value.substring(proxy.getRemoteUrl().length());

						//dopln nasu cestu
						if (value.charAt(0)!='/') value = proxy.getLocalUrl() + value;

						Logger.println(ProxyBySocket.class, "Redirecting to: " + value);

						response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
						response.setHeader(head, value);

					}
					else if ("Set-Cookie".equalsIgnoreCase(head))
					{
						//Logger.println(ProxyServlet.class, "^- generic.");
						response.setHeader(head, value);
					}
					else if (head.indexOf("Authenticate")!=-1)
					{
						response.setStatus(401);
						response.setHeader(head, value);
						retval = false;
					}
					else
					{
						//Logger.println(ProxyServlet.class, "^- generic.");
						//response.setHeader(head, value);
					}
				}
				// We do not treat multiline continuation Headers here
				// which have not occured anywhere yet.
				firstline = false;
			}
		}
		catch (IOException e)
		{
			Logger.println(ProxyBySocket.class, "Header skip problem:");
			throw new ServletException("Header skip problem: " + e.getMessage());
		}
		Logger.println(ProxyBySocket.class, "--------------");
		return retval;
	}

	/**
	 * Read a RFC2616 line from an InputStream:
	 */
	private static int readLine(InputStream in, byte[] b) throws IOException
	{
		int off2 = 0;
		while (off2 < b.length)
		{
			int r = in.read();
			if (r == -1)
			{
				if (off2 == 0)
					return -1;
				break;
			}
			if (r == 13)
				continue;
			if (r == 10)
				break;
			b[off2] = (byte) r;
			++off2;
		}
		return off2;
	}

	/**
	 * Copy a file from in to out. Sub-classes can override this in order to do
	 * filtering of some sort.
	 */
	private static void copyStream(InputStream in, OutputStream out) throws IOException
	{
		BufferedInputStream bin = new BufferedInputStream(in);
		Logger.debug(ProxyBySocket.class, "REQUEST: ------------------------------");
		StringBuilder sb = new StringBuilder();
		byte[] buff = new byte[8000];
		int len;
		while ((len = bin.read(buff)) != -1)
		{
			out.write(buff, 0, len);
			sb.append(new String(buff, 0, len));
		}
		Logger.debug(ProxyBySocket.class, sb.toString());
		Logger.debug(ProxyBySocket.class, "REQUEST KONIEC-------------------------");
	}

	private static String getStreamResponse(InputStream in) throws IOException
	{
		BufferedInputStream bin = new BufferedInputStream(in);
		Logger.debug(ProxyBySocket.class, "RESPONSE: ------------------------------");
		StringBuilder sb = new StringBuilder();
		byte[] buff = new byte[8000];
		int len;
		while ((len = bin.read(buff)) != -1)
		{
			//System.out.print((char) b);
			//out.write(b);
			sb.append(new String(buff, 0, len));
		}
		Logger.debug(ProxyBySocket.class, sb.toString());
		Logger.debug(ProxyBySocket.class, "RESPONSE KONIEC-------------------------");
		return(sb.toString());
	}

	/**
	 * Split a blank separated string into
	 */
	private static String[] wordStr(String inp)
	{
		StringTokenizer tok = new StringTokenizer(inp, " ");
		int i;
		int n = tok.countTokens();
		String[] res = new String[n];
		for (i = 0; i < n; i++)
			res[i] = tok.nextToken();
		return res;
	}

	/**
	 * Should identify RFC2616 LWS
	 */
	protected static boolean isLWS(char c)
	{
		return c == ' ';
	}

}
