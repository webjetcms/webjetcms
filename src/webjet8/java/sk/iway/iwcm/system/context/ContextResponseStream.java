package sk.iway.iwcm.system.context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;

/**
 *  ContextResponseStream.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2012
 *@author       $Author: jeeff jeeff $
 *@version      $Revision: 1.3 $
 *@created      Date: 20.7.2012 11:09:18
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class ContextResponseStream extends ServletOutputStream
{
	protected boolean closed = false;
	protected ByteArrayOutputStream baos = null;
	protected HttpServletResponse originalResponse = null;
	protected ServletOutputStream originalOutputStream = null;

	public ContextResponseStream(HttpServletResponse response) throws IOException
	{
		super();
		closed = false;
		this.originalResponse = response;
		//this.originalOutputStream = response.getOutputStream();
		baos = new ByteArrayOutputStream();
	}
	@Override
	public void close() throws IOException
	{
		if (closed)
		{
			throw new IOException("This output stream has already been closed");
		}		
		closed = true;
	}

	@Override
	public void flush() throws IOException
	{
		if (closed)
		{
			throw new IOException("Cannot flush a closed output stream");
		}
	}

	@Override
	public void write(int b) throws IOException
	{
		if (closed)
		{
			throw new IOException("Cannot write to a closed output stream");
		}
		baos.write((byte) b);
	}

	@Override
	public void write(byte b[]) throws IOException
	{
		write(b, 0, b.length);
	}

	@Override
	public void write(byte b[], int off, int len) throws IOException
	{
		if (closed)
		{
			throw new IOException("Cannot write to a closed output stream");
		}
		baos.write(b, off, len);
	}

	public boolean closed()
	{
		return (this.closed);
	}

	public void reset()
	{
		//noop
	}
	public ByteArrayOutputStream getBaos()
	{
		return baos;
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