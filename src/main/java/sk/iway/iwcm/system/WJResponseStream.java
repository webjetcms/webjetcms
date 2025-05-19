package sk.iway.iwcm.system;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;

import sk.iway.iwcm.Logger;

/**
 * WJResponseStream.java
 * 
 * @Title webjet4
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2005
 * @author $Author: jeeff $
 * @version $Revision: 1.2 $
 * @created Date: 14.4.2005 15:24:53
 * @modified $Date: 2005/10/25 06:48:05 $
 */
public class WJResponseStream extends ServletOutputStream
{
	protected ByteArrayOutputStream baos = null;
	protected GZIPOutputStream gzipstream = null;
	protected boolean closed = false;
	protected HttpServletResponse response = null;
	protected ServletOutputStream output = null;

	public WJResponseStream(HttpServletResponse response) throws IOException
	{
		super();
		Logger.println(this,"-->WJResponseStream");
		closed = false;
		this.response = response;
		this.output = response.getOutputStream();
		baos = new ByteArrayOutputStream();
		gzipstream = new GZIPOutputStream(baos);
	}
	@Override
	public void close() throws IOException
	{
		if (closed)
		{
			throw new IOException("This output stream has already been closed");
		}
		gzipstream.finish();
		byte[] bytes = baos.toByteArray();
		//response.addHeader("Content-Length", Integer.toString(bytes.length));
		//response.addHeader("Content-Encoding", "gzip");
		output.write(bytes);
		output.flush();
		output.close();
		closed = true;
	}

	@Override
	public void flush() throws IOException
	{
		if (closed)
		{
			throw new IOException("Cannot flush a closed output stream");
		}
		gzipstream.flush();
	}

	@Override
	public void write(int b) throws IOException
	{
		if (closed)
		{
			throw new IOException("Cannot write to a closed output stream");
		}
		gzipstream.write((byte) b);
	}

	@Override
	public void write(byte b[]) throws IOException
	{
		write(b, 0, b.length);
	}

	@Override
	public void write(byte b[], int off, int len) throws IOException
	{
		Logger.println(this,"writing...");
		if (closed)
		{
			throw new IOException("Cannot write to a closed output stream");
		}
		gzipstream.write(b, off, len);
	}

	public boolean closed()
	{
		return (this.closed);
	}

	public void reset()
	{
		//noop
	}
	@Override
	public boolean isReady()
	{
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public void setWriteListener(WriteListener arg0)
	{
		// TODO Auto-generated method stub
		
	}
}