package sk.iway.iwcm.system;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPOutputStream;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import net.sourceforge.stripes.mock.MockHttpServletResponse;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.context.ContextFilter;

/**
 * WJResponseWrapper.java
 *
 * @Title webjet4
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2005
 * @author $Author: jeeff $
 * @version $Revision: 1.3 $
 * @created Date: 14.4.2005 15:24:08
 * @modified $Date: 2007/01/08 14:41:25 $
 */
public class WJResponseWrapper extends HttpServletResponseWrapper
{
	PrintWriter writer;
	private StringWriter strWriter;
	private String redirectURL = null;
	HttpServletResponse origResponse;
	HttpServletRequest origRequest;
	private ByteArrayOutputStream baos = null;

	public void writeResponseToOriginalOutput(HttpServletRequest req) throws IOException
	{
		String htmlCode = strWriter.getBuffer().toString();
		writeResponseToOriginalOutput(req, htmlCode);
	}

	/**
	 * Zapise HTML kod na povodny vystup s testom na moznost gzip kompresie
	 * @param req
	 * @param htmlCode
	 * @throws IOException
	 */
	public void writeResponseToOriginalOutput(HttpServletRequest req, String htmlCode) throws IOException
	{
		if (ContextFilter.isRunning(req)==false && Constants.getBoolean("packagerGzipEnable"))
		{
			String ae = req.getHeader("accept-encoding");
	      if (ae != null && ae.indexOf("gzip") != -1)
	      {
	      	ByteArrayOutputStream out = new ByteArrayOutputStream();
	      	GZIPOutputStream gzipstream = new GZIPOutputStream(out);

	      	byte[] originalBytes = htmlCode.getBytes(SetCharacterEncodingFilter.getEncoding());
	      	gzipstream.write(originalBytes);
	      	gzipstream.finish();

	      	byte[] bytes = out.toByteArray();

	      	origResponse.addHeader("Content-Length", Integer.toString(bytes.length));
	      	origResponse.addHeader("Content-Encoding", "gzip");
	      	origResponse.getOutputStream().write(bytes);
	      	origResponse.getOutputStream().flush();
	      	origResponse.getOutputStream().close();

	      	Logger.debug(WJResponseWrapper.class, "Gzipping output, path="+PathFilter.getOrigPath(req)+"?"+(String)req.getAttribute("path_filter_query_string")+" original="+originalBytes.length+" compressed="+bytes.length+" ratio="+((double)originalBytes.length / (double)bytes.length));

	        return;
	      }
		}

		//zapis vystup
      origResponse.getWriter().write(htmlCode);
	}

	@Override
	public void sendRedirect(String url) throws IOException
	{
		Logger.println(this,"--> send redirect: "+url);
		redirectURL = url;
		//super.sendRedirect(url);
	}
	@Override
	public void addCookie(Cookie c)
	{
		Logger.debug(this,"add cookie: " + c.getName());
		Tools.addCookie(c, origResponse, origRequest);
	}

// ----------------------------------------------------------- Constructors


   /**
    * Construct a new response wrapper according to the specified parameters.
    *
    * @param response The servlet response we are wrapping
    */
   public WJResponseWrapper(HttpServletResponse response, HttpServletRequest req)
   {
		//user mock response to avoid problems with tomcat 11 buffer
		super(new MockHttpServletResponse());
		origResponse = response;
		//baos = new ByteArrayOutputStream();
		strWriter = new StringWriter();
		writer = new PrintWriter(strWriter);
		origRequest = req;
   }

   /**
    * Swallow any attempt to flush the response buffer.
    */
   @Override
   public void flushBuffer() throws IOException {
   		Logger.debug(this,"--> FLUSH BUFFER");
    	// No action is required
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
		// Do not propagate to origResponse - would cause Tomcat 11 to mark it as committed
	}
	public String getRedirectURL()
	{
		return redirectURL;
	}


	/**
	 * Pre vykonavanie dlhych cyklov - zapis prazdneho znaku na orig response aby sa udrzalo spojenie
	 * @throws IOException
	 */
	public void flushOrigResponse() throws IOException
	{
		if (Constants.getBoolean("packagerGzipEnable")==false)
		{
			origResponse.getWriter().print(" ");
			origResponse.getWriter().flush();
		}
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException
	{
		if (baos==null) baos = new ByteArrayOutputStream();
		ServletOutputStream sos = new ServletOutputStream() {

			@Override
			public void write(int b) throws IOException
			{
				baos.write(b);

			}

			@Override
			public boolean isReady()
			{
				return false;
			}

			@Override
			public void setWriteListener(WriteListener arg0)
			{
				//Auto-generated method stub

			}


		};
		return sos;
	}
	public String getOutputOfStreamAsString()
	{
		if (baos!=null)
		{
			try
			{
				return baos.toString(origRequest.getCharacterEncoding());
			}
			catch (UnsupportedEncodingException e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}
		else if (strWriter!=null && strWriter.getBuffer().isEmpty()==false) return strWriter.getBuffer().toString();
		return null;
	}

	@Override
	public boolean isCommitted() {
		return false;
	}

	@Override
	public void reset() {
		strWriter.getBuffer().setLength(0);
	}

	@Override
	public void resetBuffer() {
		//do nothing
	}

	public StringWriter getStrWriter()
	{
		return strWriter;
	}

	public String getStrWriterAsString()
	{
		return strWriter.getBuffer().toString();
	}
}