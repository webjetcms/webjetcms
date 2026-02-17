package sk.iway.iwcm.system.context;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;


/**
 *  ContextResponseWrapper.java - wrapper pre moznost fungovania WebJETu v inom ako ROOT contexte (napr. /wj7)
 *  Povodne riesene pre TatraBanku projekt RaiffeisenBank
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2012
 *@author       $Author: jeeff jeeff $
 *@version      $Revision: 1.3 $
 *@created      Date: 20.7.2012 9:12:24
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class ContextResponseWrapper extends HttpServletResponseWrapper
{
	PrintWriter writer;
	private StringWriter strWriter;
	private String redirectURL = null;
	HttpServletResponse origResponse;
	HttpServletRequest origRequest;
	ContextResponseStream outputStream = null;
	boolean debug = false;
	List<Cookie> cookies = null;

	public void writeResponseToOriginalOutput(HttpServletRequest req, boolean removeContextPath) throws IOException
	{
		if (debug) Logger.debug(ContextResponseWrapper.class, "writeResponseToOriginalOutput, contentType="+getContentType()+" os="+outputStream);
		if (getContentType()!=null && (getContentType().startsWith("text") || outputStream == null || getContentType().indexOf("x-javascript")!=-1 || getContentType().indexOf("application/javascript")!=-1))
		{
			if (debug) Logger.debug(ContextResponseWrapper.class, "writeResponseToOriginalOutput, budem replacovat");
			String htmlCode;
			if (outputStream != null)
			{
				htmlCode = outputStream.getBaos().toString(SetCharacterEncodingFilter.getEncoding());
			}
			else
			{
				htmlCode = strWriter.getBuffer().toString();
			}
			writeResponseToOriginalOutput(req, htmlCode, removeContextPath);
		}
		else if (outputStream != null && outputStream.getBaos()!=null)
		{
			if (debug) Logger.debug(ContextResponseWrapper.class, "writeResponseToOriginalOutput, zapisujem priamo");
			origResponse.getOutputStream().write(outputStream.getBaos().toByteArray());
		}
	}

	/**
	 * Zapise HTML kod na povodny vystup s testom na moznost gzip kompresie
	 * @param req
	 * @param htmlCode
	 * @throws IOException
	 */
	private void writeResponseToOriginalOutput(HttpServletRequest req, String htmlCode, boolean removeContextPath) throws IOException
	{
		//zapis vystup
		String outputCode;
		if (removeContextPath == false)
		{
			outputCode = ContextFilter.addContextPath(req.getContextPath(), htmlCode);
		}
		else
		{
			outputCode = ContextFilter.removeContextPath(Constants.getString("contextPathAdmin"), htmlCode);
		}
		//TODO: toto treba ratat ako poset bytov, co pre UTF-8 nie je length
		//byte bytes[] = outputCode.getBytes(SetCharacterEncodingFilter.getEncoding());
		//origResponse.reset();
		//origResponse.setContentLength(bytes.length);
		/*
		if (outputCode.length() < 100000)
		{
			if (debug) Logger.debug(ContextResponseWrapper.class, "setting buffer to size: "+(outputCode.length()+5000));
			origResponse.setBufferSize(outputCode.length()+5000);
		}
		*/
		//String output2 = ""+outputCode;

		int length = outputCode.getBytes(SetCharacterEncodingFilter.getEncoding()).length;
		origResponse.setContentLength(length);
		//origResponse.setHeader("Content-Length", String.valueOf(output2.getBytes("utf-8").length));
		//origResponse.setHeader("CLutf", String.valueOf(output2.getBytes("utf-8").length));
		//origResponse.setHeader("CL2", String.valueOf(outputCode.length()));
		//origResponse.setHeader("CL3", String.valueOf(outputCode.getBytes("").length));
		//origResponse.setHeader("CL4", String.valueOf(outputCode.getBytes().length));
		if (Tools.isNotEmpty(outputCode))
		{
			if (debug) Logger.debug(ContextResponseWrapper.class, "writing data, size="+outputCode.length());

			origResponse.getWriter().write(outputCode);
		}
		//origResponse.getOutputStream().write(bytes);
	}

	@Override
	public void sendRedirect(String url) throws IOException
	{
		if (debug) Logger.println(this,"--> send redirect: "+url);
		redirectURL = url;
		//super.sendRedirect(url);
	}
	@Override
	public void addCookie(Cookie c)
	{
		if (debug) Logger.debug(this,"add cookie: " + c.getName()+" path="+c.getPath()+" value="+c.getValue());
		if (cookies != null) cookies.add(c);
	}

// ----------------------------------------------------------- Constructors


   /**
    * Construct a new response wrapper according to the specified parameters.
    *
    * @param response The servlet response we are wrapping
    */
   public ContextResponseWrapper(HttpServletResponse response,HttpServletRequest request)
   {
       super(response);
       debug = Constants.getBoolean("contextPathDebug");
       origResponse = response;
       //baos = new ByteArrayOutputStream();
       strWriter = new StringWriter();
       writer = new PrintWriter(strWriter);
       origRequest = request;

       cookies = new ArrayList<>();
   }


   /**
    * Swallow any attempt to flush the response buffer.
    */
   @Override
   public void flushBuffer() throws IOException {

   	if (debug) Logger.debug(this,"--> FLUSH BUFFER");

   }


   /**
    * Return a PrintWriter that can be used to accumulate the response data
    * for the included resource.
    *
    * @exception IOException if an I/O error occurs
    */
   @Override
   public PrintWriter getWriter() throws IOException {

       return (writer);

   }
   @Override
	public void setBufferSize(int bufferSize)
	{
		try
		{
			super.setBufferSize(bufferSize);
		}
		catch (Exception e)
		{

		}
	}
	public String getRedirectURL()
	{
		return redirectURL;
	}

	private ContextResponseStream createOutputStream()
	{
		try
		{
			return new ContextResponseStream(origResponse);
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return null;
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException
	{
		if (outputStream == null) outputStream = createOutputStream();
		return (outputStream);
	}

	//setStatus implementation
	protected int statusCode = 0;
	protected String statusMessage = null;
	@Override
	public void setStatus(int sc)
	{
		super.setStatus(sc);
		statusCode = sc;
	}
	public int getStatusCode()
	{
		return statusCode;
	}
	public String getStatusMessage()
	{
		return statusMessage;
	}

	//setCharacterEncoding implementation
	protected String characterEncoding = null;
	@Override
	public void setCharacterEncoding(String charset)
	{
		if (debug) Logger.debug(ContextResponseWrapper.class, "setCharacterEncoding:"+charset);
		super.setCharacterEncoding(charset);
		characterEncoding = charset;
	}
	@Override
	public String getCharacterEncoding()
	{
		if (debug) Logger.debug(ContextResponseWrapper.class, "getCharacterEncoding: "+characterEncoding+" vs "+super.getCharacterEncoding());
		if (characterEncoding == null) return super.getCharacterEncoding();
		return characterEncoding;
	}

	//setContentType implementation
	protected String contentType = null;
	@Override
	public void setContentType(String type)
	{
		if ("text/html".equalsIgnoreCase(type))
		{
			type = "text/html; charset="+SetCharacterEncodingFilter.getEncoding();
		}

		if (debug) Logger.debug(ContextResponseWrapper.class, "setContentType="+type);
		super.setContentType(type);
		//jeeff:tato kontrola je tu kvoli nejakej haluze, ze sa setContentType vola viac krat a niekedy konci nespravne ako text/css (netusim preco)
		if (contentType == null || type.indexOf("; charset=")!=-1 || type.indexOf("application/")!=-1 || type.indexOf("image/")!=-1)
		{
			if (debug) Logger.debug(ContextResponseWrapper.class, "setting contentType="+type);
			contentType = type;
			super.setContentType(type);
		}
	}
	@Override
	public String getContentType()
	{
		if ("text/html".equalsIgnoreCase(contentType))
		{
			contentType = "text/html; charset="+SetCharacterEncodingFilter.getEncoding();
		}

		if (debug) Logger.debug(ContextResponseWrapper.class, "getContentType="+contentType);
		return contentType;
	}

	//setLocale implementation
	Locale locale = null;
	@Override
	public void setLocale(Locale loc)
	{
		super.setLocale(loc);
		locale = loc;
	}
	@Override
	public Locale getLocale()
	{
		if (locale == null) return super.getLocale();
		return locale;
	}

	//sendError implementation
	protected int errorCode=0;
	protected String errorMessage=null;
	@Override
	public void sendError(int sc, String msg) throws IOException
	{
		errorCode = sc;
		errorMessage = msg;
		//super.sendError(sc, msg);
	}

	@Override
	public void sendError(int sc) throws IOException
	{
		errorCode = sc;
		//super.sendError(sc);
	}
	public int getErrorCode()
	{
		return errorCode;
	}
	public String getErrorMessage()
	{
		return errorMessage;
	}

	//setHeader implementation
	protected java.util.Hashtable<String, String> headers = null;

	@Override
	public void setHeader(String name, String value)
	{
		if (debug) Logger.debug(ContextResponseWrapper.class, "setHeader: name="+name+" value="+value);
		if (headers == null) headers = new Hashtable<>();
		headers.put(name, value);
		super.setHeader(name, value);

		if ("content-type".equalsIgnoreCase(name))
		{
			setContentType(value);
		}
	}
	@Override
	public String getHeader(String name)
	{
		if (debug) Logger.debug(ContextResponseWrapper.class, "getHeader: "+name);
		if (headers == null) return null;
		return headers.get(name);
	}

	@Override
	public void addHeader(String name, String value)
	{
		if (debug) Logger.debug(ContextResponseWrapper.class, "addHeader: name="+name+" value="+value);
		if (headers == null) headers = new Hashtable<>();
		headers.put(name, value);
		super.addHeader(name, value);
	}

	@Override
	public void addIntHeader(String name, int value)
	{
		if (debug) Logger.debug(ContextResponseWrapper.class, "addIntHeader: name="+name+" value="+value);
		if (headers == null) headers = new Hashtable<>();
		headers.put(name, String.valueOf(value));
		super.addIntHeader(name, value);
	}

	@Override
	public void setIntHeader(String name, int value)
	{
		if (debug) Logger.debug(ContextResponseWrapper.class, "setIntHeader: name="+name+" value="+value);
		if (headers == null) headers = new Hashtable<>();
		headers.put(name, String.valueOf(value));
		super.setIntHeader(name, value);
	}

	public List<Cookie> getCookies()
	{
		return cookies;
	}
}
