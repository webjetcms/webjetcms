package sk.iway.iwcm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sk.iway.iwcm.i18n.Prop;

/**
 *  AdminlogFile.java
 *  Ticket 11595
 *  Zapis Auditlogov do suboru pre potreby Tatrabanky.
 *
 * Zapisuje zaznamy auditLogu do .log suboru. Cesta k suboru sa zaznamenava v konfiguracnej premennej 'auditFileLocation'
 * format zapisu je nasledovny:
 *    datum a cas udalosti - format: yyyy-mm-dd HH:mm:ss|zatial konstanta - SessioID|UserId|konstanta - 0|remoteIp|AuditLogGateway|konstanta nastavuje sa v konfiguracii - browser|konstanta nastavuje sa v konfiguracii - audit|Typ logu|funkcia - totozne s predchadz.zaznamom| textovy popis udalosti |konstanta - OK
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2012
 *@author       $Author:  mbocko $
 *@version      $Revision: 1.3 $
 *@created      Date: 3.8.2012 9:13:21
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class AdminlogFile
{
	private static LinkedList<String> fifo = new LinkedList<String>(); //uklada zaznamy do fifo pre pripad zlyhania zapisu suboru

	private AdminlogFile()	{	} //volaju sa iba staticke metody, nieje ziaduce aby bolo mozne vytvarat instancie

	/**
	 * @param timestamp
	 * @param requestBean
	 * @param logType
	 * @param description
	 */
	public static void write(Timestamp timestamp, RequestBean requestBean, int logType, String description)
	{
		String fileLoc = Constants.getString("auditFileLocation");
		if(fileLoc.length()<5)
		{
			return;
		}
		String logItem = null;
		String logItemsSeparator = Constants.getString("auditLogSeparator");

		Pattern patternToSearch = Pattern.compile("[.][A-Za-z-_]*[.]");
		Matcher matcher = patternToSearch.matcher(fileLoc);
		if(matcher.find())
		{
			String datePattern = matcher.group();
			datePattern = datePattern.substring(1, datePattern.length() - 1);
			String date = new SimpleDateFormat(datePattern).format(timestamp);

			fileLoc = fileLoc.replaceFirst(datePattern, date);
		}
		String logTypeName = Prop.getInstance().getText("components.adminlog."+logType);

		logItem = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timestamp);
		logItem += logItemsSeparator;
		logItem += requestBean.getSessionId(); //sessionID
		logItem += logItemsSeparator;
		int userId = requestBean.getUserId();
		logItem += (userId!=0) ? (String.valueOf(userId)) : ("-1");//vracia userId, ak nieje prihlaseny, vracia -1
		logItem += logItemsSeparator;
		logItem += "0";//tallerId - konstanta
		logItem += logItemsSeparator;
		logItem += requestBean.getRemoteIP();//remoteIp
		logItem += logItemsSeparator;
		logItem += Constants.getString("auditLogGateway")+Constants.getString("clusterMyNodeName");
		logItem += logItemsSeparator;
		logItem += Constants.getString("auditLogClient");
		logItem += logItemsSeparator;
		logItem += Constants.getString("auditLogtype");
		logItem += logItemsSeparator;
		logItem += logTypeName;
		logItem += logItemsSeparator;
		logItem += logTypeName;
		logItem += logItemsSeparator;
		description = description.replaceAll("\n", " ");
		description = description.replaceAll("\r", " ");
		logItem += description;
		logItem += logItemsSeparator;
		logItem += "OK";
		fifo.addLast(logItem);//ulozi zaznam do fifo, aby zostal pristupny v pripade zlyhania zapisu do suboru

		FileOutputStream fos = null;
		OutputStreamWriter osw = null;
		try
		{
			File f = new File(fileLoc);
			if (f.exists()==false) f.createNewFile();

			fos = new FileOutputStream(fileLoc, true);
			//poziadavka z TB, aby sa subor zapisal v specifikovanom kodovani
			osw = new OutputStreamWriter(fos, Constants.getString("auditLogEncoding"));

			for(String row : fifo)
			{
				osw.write(row+"\n");
			}

			osw.close();
			osw = null;
			fos.close();
			fos = null;
			fifo.clear();//ak sa nevyhodi vynimka IOException, vymaze sa obsah FIFO, v opacnom pripade sa pokusa zapisat obsah fifa pri dalsom pokuse
		}
		catch (IOException e)
		{
			Logger.error(AdminlogFile.class, "!!! Nieje mozne zapisovat do suboru: "+fileLoc+"\n");
			for(String row : fifo)
			{
				Logger.debug(AdminlogFile.class, "!!! Adminlog Item: "+row+"\n");
			}
		}
		finally
		{
			if (fos != null) try { fos.close(); } catch (Exception ex) { sk.iway.iwcm.Logger.error(ex); }
			if (osw != null) try { osw.close(); } catch (Exception ex) { sk.iway.iwcm.Logger.error(ex); }
		}
	}
}
