package sk.iway.iwcm.dmail;

import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import jxl.Cell;
import jxl.Sheet;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;
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
public class EmailExcelImport extends ExcelImportJXL
{
	List<UserDetails> users;
	Map<String, Integer> emailsTable;
	Map<String, Integer> odhlaseneEmailsTable;

	@SuppressWarnings("unchecked")
	public EmailExcelImport(InputStream in, HttpServletRequest request, PrintWriter out)
	{
		super(in, request, out);

		users = (List<UserDetails>)request.getSession().getAttribute("dmailUsers");
		if (users == null) users = new ArrayList<UserDetails>();

		emailsTable = new Hashtable<String, Integer>();
		for (UserDetails user : users)
		{
			emailsTable.put(user.getEmail().toLowerCase(), 1);
		}

		odhlaseneEmailsTable = EmailDB.getHashtableFromUnsubscribedEmails();
	}

	@Override
	protected void saveRow(Connection db_conn, Cell[] row, Sheet sheet, Prop prop) throws Exception
	{
		if (row.length<1)
		{
			return;
		}

		String email = getValue(row, "email");
		if (Tools.isNotEmpty(email) && email.indexOf('@')>1)
		{
			email = email.toLowerCase();

			Integer rowValue = emailsTable.get(email.toLowerCase());
			if (rowValue!=null)
			{
				//ochrana pred duplicitnými email adresami
				printlnError(prop.getText("components.dmail.xlsImport.emailAllreadyExists", email));
				return;
			}

			if (odhlaseneEmailsTable.get(email)!=null)
			{
				//ochrana pred odhlasenymi email adresami
				printlnError(prop.getText("components.dmail.xlsImport.emailIsUnsubscribed", email));
				return;
			}

			String firstName = getValue(row, "firstName");
			String lastName = getValue(row, "lastName");
			if (firstName==null) firstName = "";
			if (lastName==null) lastName = "";

			UserDetails usr = UsersDB.getUserByEmail(email);
			if (usr == null)
			{
				usr = new UserDetails();
			}
			//prepiseme data podla excelu aby sme mohli mat custom meno a priezvisko (ine ako mame v databaze)
			usr.setFirstName(firstName);
			usr.setLastName(lastName);
			usr.setEmail(email);

			users.add(usr);
			emailsTable.put(email.toLowerCase(), rowCounter);

			println(prop.getText("components.dmail.xlsImport.importingEmail")+" " + usr.getEmail());
			if (rowCounter%50==0)
			{
				scrollWindow(1000);
			}
		}
	}
	@Override
	protected void afterImportJob(Prop prop)
	{
		println("<br/><br/>"+prop.getText("file.import_done")+". <a href='/components/dmail/admin_email_usersframe.jsp'>"+prop.getText("sk.iway.iwcm.dmail.emailExcelImport.zobrazit_zoznam_prijemcov")+"</a>.");
		scrollWindow(1000);
		request.getSession().setAttribute("dmailUsers", users);
	}


}
