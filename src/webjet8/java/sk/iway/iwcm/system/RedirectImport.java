package sk.iway.iwcm.system;

import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.util.Calendar;

import javax.servlet.http.HttpServletRequest;

import jxl.Cell;
import jxl.Sheet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.xls.ExcelImportJXL;

/**
 *  RedirectImport.java
 *
 *@Title        webjet4_repair
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2008
 *@author       $Author: jeeff $
 *@version      $Revision: 1.3 $
 *@created      Date: 29.12.2008 15:13:53
 *@modified     $Date: 2009/06/02 06:39:09 $
 */
public class RedirectImport extends ExcelImportJXL
{
	public RedirectImport(InputStream in, HttpServletRequest request, PrintWriter out)
	{		
		super(in, request, out);
		Logger.println(this,"RedirectImport constructor");
	}
	
	
	protected void saveRow(Connection db_conn, Cell[] row, Sheet sheet, Prop prop) throws Exception
	{
		String oldUrl = getValue(row[0]);
		String newUrl = getValue(row[1]);
		Integer redirectCode = getIntValue(row[2]);
		String domain = "";
		//excel ma 4 stplce,iba ak ide o multidomain sidlo
		if (row.length > 3)
		{			
			domain = getValue(row[2]);
			redirectCode = getIntValue(row[3]);
		}
		
		if (redirectCode<100) redirectCode = 301;
		
		
		if (Tools.areNotEmpty(oldUrl,newUrl) )
		{	
			UrlRedirectBean redirect = null;
			if (Tools.isNotEmpty(domain))
			{
				redirect = UrlRedirectDB.getByOldUrl(oldUrl, domain);
			}
			else
			{
				redirect = UrlRedirectDB.getByOldUrl(oldUrl);
			}
			if (redirect == null)
			{
				redirect = new UrlRedirectBean(); 
			}
			redirect.setDomainName(domain);
			redirect.setNewUrl(newUrl);
			redirect.setOldUrl(oldUrl);
			redirect.setRedirectCode(redirectCode);
			redirect.setInsertDate(Calendar.getInstance().getTime());
			//urls.add(redirect);
			
			boolean ok = redirect.save();
			String status = "OK";
			if (!ok) status = "FAIL";
			
			println(oldUrl+" =&gt; "+newUrl+" "+status);
		}
	}
}
