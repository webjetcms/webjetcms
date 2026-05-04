package sk.iway.iwcm.dmail;

import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import jxl.Cell;
import jxl.Sheet;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.xls.ExcelImportJXL;

/**
 *  EmailExcelImport.java - import pouzivatelov pre priame odoslanie mailu
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2008
 *@author       $Author: murbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 11.2.2008 14:25:24
 *@modified     $Date: 2009/08/31 11:56:27 $
 */
public class EmailExcelImportUnsubscribe extends ExcelImportJXL
{
	Map<String, Integer> odhlaseneEmailsTable;

	public EmailExcelImportUnsubscribe(InputStream in, HttpServletRequest request, PrintWriter out)
	{
		super(in, request, out);

		odhlaseneEmailsTable = EmailDB.getHashtableFromUnsubscribedEmails();
	}

	@Override
	protected void saveRow(Connection db_conn, Cell[] row, Sheet sheet, Prop prop) throws Exception
	{
		if (row.length<1)
		{
			return;
		}

		String email = getValue(row, "email").trim().toLowerCase();
		if (Tools.isNotEmpty(email))
		{
			if (!Tools.isEmail(email)) {
				// validacia na email
				printlnError(prop.getText("components.dmail.xlsImport.notEmail", email));
				return;
			}

			if (odhlaseneEmailsTable.get(email) != null)
			{
				//ochrana pred duplicitnými email adresami
				printlnError(prop.getText("components.dmail.xlsImport.emailAllreadyExists", email));
				return;
			}

			EmailDB.addUnsubscribedEmail(email);
			println(prop.getText("components.dmail.xlsImport.importingEmail")+" " + email);
			if (rowCounter%50==0)
			{
				scrollWindow(1000);
			}
		}
	}
	@Override
	protected void afterImportJob(Prop prop)
	{
		//println("<br/><br/>"+prop.getText("file.import_done")+". <a href='/components/dmail/admin_email_usersframe.jsp'>"+prop.getText("sk.iway.iwcm.dmail.emailExcelImport.zobrazit_zoznam_prijemcov")+"</a>.");
		//scrollWindow(1000);
		//request.getSession().setAttribute("dmailUsers", users);
	}


}
