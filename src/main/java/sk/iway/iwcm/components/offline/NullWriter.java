package sk.iway.iwcm.components.offline;

import java.io.Writer;

/**
 *  NullWriter.java - writer pre Tidy, ktory vsetko ignoruje
 *
 *@Title        webjet
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      Date: 22.11.2004 23:11:14
 *@modified     $Date: 2007/09/07 13:39:30 $
 */
public class NullWriter extends Writer
{
	public NullWriter()
	{
	}

	@Override
	public void close()
	{
	}

	@Override
	public void flush()
	{
	}

	@Override
	public void write(char[] cbuf, int off, int len)
	{
	}
}
