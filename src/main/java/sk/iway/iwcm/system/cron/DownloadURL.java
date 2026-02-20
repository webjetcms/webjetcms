package sk.iway.iwcm.system.cron;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.helpers.MailHelper;

/**
 * DownloadURL.java - trieda, ktora sa spusta z crontabu a stahuje zadane
 * URL
 *
 * @Title webjet
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2004
 * @author $Author: jeeff $
 * @version $Revision: 1.5 $
 * @created Date: 15.10.2004 22:18:23
 * @modified $Date: 2005/10/25 06:48:05 $
 */
public class DownloadURL
{
	public static void main(String[] args)
	{
		try
		{
			if (args != null && args.length > 0)
			{
				String url = args[0];
				Date now = new Date();
				Logger.println(DownloadURL.class,now.toString());
				Logger.println(DownloadURL.class,"downloading: " + url);
				int i;
				for (i=0; i<args.length; i++)
				{
					Logger.println(DownloadURL.class,"mam args: ["+i+"]="+args[i]);
				}
				String data = Tools.downloadUrl(url);
				if (data != null) data = data.trim();
				Logger.debug(DownloadURL.class, "CRON VYSTUP: "+data);

				if (data!=null && Tools.isNotEmpty(data) && args.length>=4 && args[1].indexOf('@')!=-1 && args[2].indexOf('@')!=-1)
				{
					Logger.println(DownloadURL.class,"mam data: " + data.length());
					//treba to poslat mailom
					new MailHelper().
						setFromEmail(args[1]).
						setFromName(args[1]).
						addRecipient(args[2]).
						setMessage(data).
						setBaseHref(args[0]).
						setSubject(args[3].replace('_', ' ')).
						send();
				}
				// success
				//Adminlog.add(Adminlog.TYPE_CRON, "DownloadURL CRONJob complete, URL: "+url, -1, -1);
			}
		}
		catch (Exception e)
		{
			StringWriter sw = new StringWriter();
      	e.printStackTrace(new PrintWriter(sw));

			Adminlog.add(Adminlog.TYPE_CRON, "DownloadURL error:"+e.getMessage()+"\n"+sw.toString(), -1, -1);
			sk.iway.iwcm.Logger.error(e);
		}
	}
}