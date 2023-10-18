package sk.iway.iwcm.xls;

import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.i18n.Prop;

/**
 *  CSVImport.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2008
 *@author       $Author: bhric $
 *@version      $Revision: 1.1 $
 *@created      Date: 3.10.2008 11:02:37
 *@modified     $Date: 2008/10/27 11:30:40 $
 */
public class CSVImport
{
	protected InputStream in;
	protected HttpServletRequest request;
	protected PrintWriter out;
	protected Prop prop;
	protected int rowCounter;

	public CSVImport(InputStream in, HttpServletRequest request, PrintWriter out)
	{
		this.in = in;
		this.request = request;
		this.out = out;
		prop = Prop.getInstance(request);
		rowCounter = 0;
	}
	protected void doImport(HttpServletRequest request,InputStream in) throws Exception
	{

	}

	public void scrollWindow(int amount)
	{
		if (out == null) return;

		out.println("<script type='text/javascript'>window.scrollBy(0, "+amount+");</script>");
	}

	public void println(String message)
	{
		if (rowCounter>=0) Logger.debug(this,rowCounter+": "+message);
		else Logger.debug(this,message);

		if (out == null) return;

		if (rowCounter>=0) out.print(rowCounter+": ");
		out.println(message+"<br>\n");
		out.flush();

		if (rowCounter % 10 == 0)
		{
			//odscroluj stranku
			out.println("<script type='text/javascript'>window.scrollBy(0, 1000);</script>");
		}
	}

	public void printlnError(String message)
	{
		if (out == null)
		{
			Logger.println(this,message);
			return;
		}
		if (rowCounter>=0) out.print(rowCounter+": ");

		out.println("<span style='color: red;font-weight: bold;'>"+message+"</span><br/>");
		out.flush();

		if (rowCounter % 10 == 0)
		{
			//odscroluj stranku
			out.println("<script type='text/javascript'>window.scrollBy(0, 1000);</script>");
		}
	}
}
