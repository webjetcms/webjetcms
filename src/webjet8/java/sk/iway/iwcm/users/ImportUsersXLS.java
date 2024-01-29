package sk.iway.iwcm.users;

import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import jxl.Cell;
import jxl.Sheet;
import sk.iway.Password;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.UserTools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.xls.ExcelImportJXL;

/**
 *  ImportUsersXLS.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: bhric $
 *@version      $Revision: 1.10 $
 *@created      Date: 23.10.2004 18:44:14
 *@modified     $Date: 2009/10/07 12:26:31 $
 */
public class ImportUsersXLS extends ExcelImportJXL
{
	PreparedStatement ps;
	ResultSet rs;
	Identity user;
	int userId;

	String firstName;
	String lastName;
	String email;
	String loginName;
	String password;

	String autoEmailFormat;
	boolean emailUnique = false;
	boolean allreadyExists;
	boolean generatePassword = false;
	boolean generateLoginName = false;

	boolean authorize = false;
	boolean sendEmail = false;

	protected String[] groupsParams;
	String groups = "1";
	boolean admin = false;
	boolean authEmailSend;

	boolean emailUniqueOverwrite = false;

	/**
	 * Import pouzivatelov z Excelu
	 * @param in
	 * @param request
	 * @param out
	 */
	public ImportUsersXLS(InputStream in, HttpServletRequest request, PrintWriter out)
	{
		super(in, request, out);

		autoEmailFormat = request.getParameter("autoEmailFormat");
		if ("yes".equals(request.getParameter("emailUnique")))
		{
			emailUnique = true;
		}

		if ("yes".equals(request.getParameter("authorize")))
		{
			authorize = true;
		}
		if ("yes".equals(request.getParameter("sendEmail")))
		{
			sendEmail = true;
		}

		if ("yes".equals(request.getParameter("generatePassword")))
		{
			generatePassword = true;
		}

		if ("yes".equals(request.getParameter("generateLoginName")))
		{
			generateLoginName = true;
		}

		if ("yes".equals(request.getParameter("emailUniqueOverwrite")))
		{
			emailUniqueOverwrite = true;
		}

		groupsParams = request.getParameterValues("groups");
		if (groupsParams != null)
		{
			int i;
			int size = groupsParams.length;
			groups = null;
			for (i=0; i<size; i++)
			{
				if (groups == null)
				{
					groups = groupsParams[i];
				}
				else
				{
					groups += "," + groupsParams[i]; //NOSONAR
				}
			}
		}
		if ("yes".equals(request.getParameter("admin")))
		{
			admin = true;
		}

		user = UsersDB.getCurrentUser(request);
	}

	@Override
	protected void saveRow(Connection db_conn, Cell[] row, Sheet sheet, Prop prop) throws Exception
	{
		if (row.length < 1)
		{
			return;
		}

		firstName = getValue(row, "firstName");
		lastName = getValue(row, "lastName");
		email = getValue(row, "email");
		loginName = getValue(row, "loginName");
		password = getValue(row, "password");


		if (Tools.isEmpty(lastName) && Tools.isNotEmpty(email)) lastName = email;

		//meno a priezvisko potrebujem na dogenerovanie ostatnych veci (ak nie su zadane)
		if (Tools.isEmpty(firstName) && Tools.isEmpty(lastName))
		{
			printlnError(prop.getText("users.import.missing_fields"), rowCounter);
			return;
		}

		if (Tools.isEmpty(password) && generatePassword)
		{
			password = Password.generatePassword(5);
		}

		if (Tools.isEmpty(loginName) && generateLoginName)
		{
			/*if (Tools.isNotEmpty(email))
			{
				//pouzi email ako login
				loginName = email;
			}
			else
			{
				//generuj login vo formate lbalat
				if (Tools.isNotEmpty(firstName))
				{
					loginName = DB.internationalToEnglish(firstName).toLowerCase().charAt(0) + DB.internationalToEnglish(lastName).toLowerCase();
				}
				else
				{
					loginName = DB.internationalToEnglish(lastName).toLowerCase();
				}
			}*/
			//kedze sa import pouziva hlavne kvoli mailingom a tu koliduje login, budem ho generovat
			loginName = groups + "-" + rowCounter + "-" + Password.generatePassword(4);
		}

		if (Tools.isEmpty(email) && Tools.isNotEmpty(autoEmailFormat))
		{
			//	email sa vytvara automaticky
			email = autoEmailFormat;
			email = Tools.replace(email, "firstName", DB.internationalToEnglish(firstName).toLowerCase());
			email = Tools.replace(email, "lastName", DB.internationalToEnglish(lastName).toLowerCase());
		}

		if (Tools.isNotEmpty(firstName) && Tools.isNotEmpty(lastName) && Tools.isNotEmpty(email) &&
			 Tools.isNotEmpty(loginName) && Tools.isNotEmpty(password))
		{

			allreadyExists = false;
			//ak treba, skontroluj jedinecnost emailovej adresy
			if (emailUnique)
			{
				//	zisti ci uz taky user v DB nie je
				ps = db_conn.prepareStatement("SELECT user_id, user_groups FROM  users WHERE email=?" + UsersDB.getDomainIdSqlWhere(true));
				ps.setString(1, email);
				rs = ps.executeQuery();
				allreadyExists = false;

				List<UserDetails> allreadyExistsUsers = new ArrayList<>();
				while (rs.next())
				{
					UserDetails usr = new UserDetails();
					usr.setUserId(rs.getInt("user_id"));
					usr.setUserGroupsIds(DB.getDbString(rs, "user_groups"));
					allreadyExistsUsers.add(usr);
					allreadyExists = true;
				}
				rs.close();
				ps.close();

				//fixni im IDecka
				for (UserDetails usr : allreadyExistsUsers)
				{
					UsersDB.addUserGroups(usr, groupsParams);

					//uprav im nastavenia - ziadost SPSS pre hromadny email
					if (emailUniqueOverwrite)
					{
						UserDetails u = UsersDB.getUser(usr.getUserId());
						u.setTitle(getValue(row, "title", u.getTitle()));
						u.setFirstName(firstName);
						u.setLastName(lastName);

						u.setCompany(getValue(row, "company", u.getCompany()));
						u.setAdress(getValue(row, "street", u.getAdress()));
						u.setCity(getValue(row, "city", u.getCity()));
						u.setZip(getValue(row, "zip", u.getZip()));
						u.setCountry(getValue(row, "country", u.getCountry()));
						u.setPhone(getValue(row, "phone", u.getPhone()));

						u.setFieldA(getValue(row, "fieldA", u.getFieldA()));
						u.setFieldB(getValue(row, "fieldB", u.getFieldB()));
						u.setFieldC(getValue(row, "fieldC", u.getFieldC()));
						u.setFieldD(getValue(row, "fieldD", u.getFieldD()));
						u.setFieldE(getValue(row, "fieldE", u.getFieldE()));

						if (Constants.getString("usersPositionList").startsWith("enumeration_") && Tools.isNotEmpty(getValue(row, "position"))) {
							u.setPosition(UserTools.resolveOrCreate("usersPositionList", getValue(row, "position")).getId() + "");
						} else {
							u.setPosition(getValue(row, "position", u.getPosition()));
						}

						Cell cell = getCell(row, "sexMale");
						if (cell != null) u.setSexMale(getBooleanValue(row, "sexMale"));
						u.setPhoto(getValue(row, "photo", u.getPhoto()));
						u.setSignature(getValue(row, "signature", u.getSignature()));

						u.setFax(UserTools.getDepartment(getValue(row, "fax"), getValue(row, "parentFax"), rowCounter));

						u.setDeliveryFirstName(getValue(row, "deliveryFirstName"));
						u.setDeliveryLastName(getValue(row, "deliveryLastName"));
						u.setDeliveryCompany(getValue(row, "deliveryCompany"));
						u.setDeliveryAdress(getValue(row, "deliveryAddress"));
						u.setDeliveryCity(getValue(row, "deliveryCity"));
						u.setDeliveryPsc(getValue(row, "deliveryPSC"));
						u.setDeliveryCountry(getValue(row, "deliveryCountry"));
						u.setDeliveryPhone(getValue(row, "deliveryPhone"));
						if (Tools.isNotEmpty(getValue(row, "userGroups"))) {
							this.groups = "1";
							for (String string : getValue(row, "userGroups").split(",")) {
								this.groups += "," + UserGroupsDB.getInstance().getUserGroupId(string);  //NOSONAR
							}
						}
						u.setUserGroupsIds(groups);
						cell = getCell(row, "parentId");
						if (cell != null) u.setParentId(getIntValue(row, "parentId"));

						UsersDB.saveUser(u);
					}
				}
			}

			if (allreadyExists)
			{
				println(prop.getText("users.import.email_allready_exists", email), rowCounter);
				return;
			}

			//	zisti ci uz taky user v DB nie je
			ps = db_conn.prepareStatement("SELECT user_id FROM  users WHERE login=?" + UsersDB.getDomainIdSqlWhere(true));
			ps.setString(1, loginName);
			rs = ps.executeQuery();
			allreadyExists = false;
			if (rs.next())
			{
				allreadyExists = true;
			}
			rs.close();
			ps.close();

			if (allreadyExists)
			{
				printlnError(prop.getText("users.import.login_allready_exists", loginName), rowCounter);
				return;
			}

			//naimportuj to do DB
			ps = db_conn.prepareStatement("INSERT INTO  users (title, first_name, last_name, login, password, " +
					"is_admin, user_groups, company, adress, city, email, PSC, country, " +
					"phone, authorized, editable_groups, editable_pages, writable_folders, last_logon, module_perms, reg_date, field_a, field_b, field_c, field_d, field_e, password_salt, date_of_birth, position, " +
					"sex_male, photo, signature, forum_rank, rating_rank, allow_login_start, allow_login_end, fax, delivery_first_name, delivery_last_name, delivery_company, delivery_adress, delivery_city, delivery_psc, delivery_country, delivery_phone, parent_id, domain_id) VALUES " +
					"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

			ps.setString(1, getValue(row, "title"));
			ps.setString(2, firstName);
			ps.setString(3, lastName);
			ps.setString(4, loginName);

			String salt = "";
			sk.iway.Password pass = new sk.iway.Password();
			if (Constants.getBoolean("passwordUseHash"))
			{
				salt = PasswordSecurity.generateSalt();
				ps.setString(5, PasswordSecurity.calculateHash(password, salt));
			}
			else
				ps.setString(5, pass.encrypt(password));

			ps.setBoolean(6, admin);

			if (Tools.isNotEmpty(getValue(row, "userGroups"))) {
				this.groups = "1";
				for (String string : getValue(row, "userGroups").split(",")) {
					this.groups += "," + UserGroupsDB.getInstance().getUserGroupId(string);  //NOSONAR
				}
			}

			ps.setString(7, groups);
			ps.setString(8, getValue(row, "company"));
			ps.setString(9, getValue(row, "street"));
			ps.setString(10, getValue(row, "city"));
			ps.setString(11, email);
			ps.setString(12, getValue(row, "zip"));
			ps.setString(13, getValue(row, "country"));

			ps.setString(14, getValue(row, "phone"));
			ps.setBoolean(15, authorize);
			ps.setString(16, getValue(row, "editableGroups"));
			ps.setString(17, getValue(row, "editablePages"));
			ps.setString(18, getValue(row, "writableFolders"));
			java.util.Date date = getDateValue(row, "lastLogon");
			if (date != null) ps.setTimestamp(19, new Timestamp(date.getTime()));
			else ps.setNull(19, Types.TIMESTAMP);
			ps.setString(20, getValue(row, "modulePerms"));
			ps.setTimestamp(21, new Timestamp(Tools.getNow()));
			ps.setString(22, getValue(row, "fieldA"));
			ps.setString(23, getValue(row, "fieldB"));
			ps.setString(24, getValue(row, "fieldC"));
			ps.setString(25, getValue(row, "fieldD"));
			ps.setString(26, getValue(row, "fieldE"));
			ps.setString(27, salt);
			java.util.Date dateOfBirth = getDateValue(row, "dateOfBirth");
			if (dateOfBirth != null) ps.setTimestamp(28, new Timestamp(dateOfBirth.getTime()));
			else ps.setNull(28, Types.TIMESTAMP);

			if (Constants.getString("usersPositionList").startsWith("enumeration_")) {
				ps.setString(29, UserTools.resolveOrCreate("usersPositionList", getValue(row, "position")).getEnumerationDataId() + "");
			} else {
				ps.setString(29, getValue(row, "position"));
			}
			ps.setBoolean(30, getBooleanValue(row, "sexMale"));
			ps.setString(31, getValue(row, "photo"));
			ps.setString(32, getValue(row, "signature"));
			ps.setInt(33, getIntValue(row, "forumRank"));
			ps.setInt(34, getIntValue(row, "rantingRank"));
			date = getDateValue(row, "allowLoginStart");
			if (date != null) ps.setTimestamp(35, new Timestamp(date.getTime()));
			else ps.setNull(35, Types.TIMESTAMP);
			date = getDateValue(row, "allowLoginEnd");
			if (date != null) ps.setTimestamp(36, new Timestamp(date.getTime()));
			else ps.setNull(36, Types.TIMESTAMP);

			//pole FAX pouzivame ako ciselnik oddeleni (sialene rozhodnutie, ale co uz)
			ps.setString(37, UserTools.getDepartment(getValue(row, "fax"), getValue(row, "parentFax"), rowCounter));

			ps.setString(38, getValue(row, "deliveryFirstName"));
			ps.setString(39, getValue(row, "deliveryLastName"));
			ps.setString(40, getValue(row, "deliveryCompany"));
			ps.setString(41, getValue(row, "deliveryAddress"));
			ps.setString(42, getValue(row, "deliveryCity"));
			ps.setString(43, getValue(row, "deliveryPSC"));
			ps.setString(44, getValue(row, "deliveryCountry"));
			ps.setString(45, getValue(row, "deliveryPhone"));
			ps.setInt(46, getIntValue(row, "parentId"));
			ps.setInt(47, CloudToolsForCore.getDomainId());

			ps.execute();
			ps.close();

			userId = -1;
			//ziskaj userId
			ps = db_conn.prepareStatement("SELECT user_id FROM  users WHERE login=?" + UsersDB.getDomainIdSqlWhere(true));
			ps.setString(1, loginName);
			rs = ps.executeQuery();
			allreadyExists = false;
			if (rs.next())
			{
				userId = rs.getInt("user_id");
			}
			rs.close();
			ps.close();

			println(prop.getText("users.import.regSuccess") + ": " + firstName + " " + lastName + "; " + loginName + "; " + password, rowCounter);

			//trigger po zmene udajov pouzivatela - podobne ako u usrLogon
			if ( Tools.isNotEmpty(Constants.getString("userAfterSaveMethod") ))
			{
				try
				{
					String saveMethod = Constants.getString("userAfterSaveMethod");
					String clazzName = saveMethod.substring(0, saveMethod.lastIndexOf('.'));
					String methodName = saveMethod.substring(clazzName.length() + 1);
					Class<?> clazz = Class.forName(clazzName);
					boolean skipWithoutRequest = false;
					try
					{
						Method method = clazz.getMethod(methodName, HttpServletRequest.class, UserDetails.class, UserDetails.class);
						method.invoke(null, request, null, UsersDB.getUser(user.getLogin()));
						skipWithoutRequest = true;
					}
					catch (NoSuchMethodException nsme) {/*do nothing*/}
					if (!skipWithoutRequest)
					{
						Method method = clazz.getMethod(methodName, UserDetails.class, UserDetails.class);
						method.invoke(null, null, UsersDB.getUser(loginName));
					}
				}
				catch (Exception e)
				{
					sk.iway.iwcm.Logger.error(e);
				}
			}

			if (sendEmail && userId > 0)
			{
				authEmailSend = AuthorizeAction.sendInfoEmail(userId, password, user, request);
				if (!authEmailSend)
				{
					printlnError(prop.getText("users.import.authEmailFail", email), rowCounter);
				}
			}

			Adminlog.add(Adminlog.TYPE_USER_INSERT, "Naimportovany novy pouzivatel, userId=" + userId + " login=" + loginName + " firstName=" + firstName + " lastName=" + lastName + " email=" + email, userId, -1);
		}
		else
		{
			printlnError(prop.getText("users.import.missing_fields"), rowCounter);
		}
	}

	/**
	 * Vrati hodnotu z Excelu, ak ale taky stlpec nemame, vrati aktalnu hodnotu (potrebne pre aktualizaciu zaznamov)
	 * @param row
	 * @param name
	 * @param actualValue
	 * @return
	 * @throws Exception
	 */
	private String getValue(Cell[] row, String name, String actualValue) throws Exception
	{
		Cell cell = getCell(row, name);
		if (cell!=null)
		{
			return getValue(row, name);
		}
		return actualValue;
	}

	@Override
	protected void afterImportJob(Prop prop) {
		if (Constants.getBoolean("organisationStructure") == true) {
			List<UserDetails> userDetails = UsersDB.getUsersByGroup(Constants.getInt("organisation_structure.intranet_group",-1)); // intranet
			UserTools.setSuperiorWorker(userDetails); // prepocitaj celu strukturu
		}
	}
}
