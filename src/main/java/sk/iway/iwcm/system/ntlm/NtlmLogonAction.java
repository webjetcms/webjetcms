package sk.iway.iwcm.system.ntlm;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.PartialResultException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.BeanUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import sk.iway.Password;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.LogonTools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UserGroupDetails;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.iwcm.users.UsersDB;

/**
 *  LogonAction.java - prihlasenie usera do systemu pomocou NTLM filtra
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2005
 *@author       $Author: jeeff $
 *@version      $Revision: 1.21 $
 *@created      Date: 4.8.2005 14:04:07
 *@modified     $Date: 2010/01/11 08:15:21 $
 */
@Controller
public class NtlmLogonAction
{
	private static Map<String, String> emptyGroupsTable;
	static
	{
		emptyGroupsTable = new Hashtable<>();
	}

	@ResponseBody
    @RequestMapping("/ntlm/logon.struts")
	public void execute(HttpServletRequest request, HttpServletResponse response)
				throws Exception
	{
		Logger.debug(NtlmLogonAction.class,"NtlmLogonAction");
		try
		{
			if (request instanceof NtlmHttpServletRequest)
			{
				Identity loggedUser = UsersDB.getCurrentUser(request);
				if (loggedUser != null)
				{
					//user je uz zalogovany, asi nema prava tam kde chce ist...
					Logger.debug(NtlmLogonAction.class,"NtlmLogonAction - user je uz lognuty, asi sem nema pristup, admin: " + loggedUser.isAdmin()+" forbiddenURL="+AuthenticationFilter.getForbiddenURL());

					if (AuthenticationFilter.getForbiddenURL()!=null)
					{
						response.sendRedirect(AuthenticationFilter.getForbiddenURL());
					}
					else
					{
						response.sendError(HttpServletResponse.SC_FORBIDDEN);
					}
					return;
				}


				NtlmHttpServletRequest reqAuth = (NtlmHttpServletRequest)request;
				String loginName = reqAuth.getUserPrincipal().getName();

				int index = loginName.indexOf('\\');
				if (index == -1) index = loginName.indexOf('/');
				loginName = (index != -1) ? loginName.substring(index + 1) : loginName;

				//Set groups = reqAuth.getGroups();

				Logger.debug(NtlmLogonAction.class,"NtlmLogonAction - login: " + loginName);

				//nastav default jazyk
				request.getSession().setAttribute(sk.iway.iwcm.i18n.Prop.SESSION_I18N_PROP_LNG, Constants.getString("defaultLanguage"));
				Prop prop = Prop.getInstance(Constants.getServletContext(), request);


				UserDetails user;
				//TODO refaktorovat tak, aby robil normalne redirecty, nie hadzal exception
				try
				{
					user = authentificateUserAgainstLdap(request, response, loginName);
				}
				catch (Exception e)
				{
					return;
				}

				boolean logonSuccess = false;
				String afterLogonUrl = "/";

				String password = user.getPassword();
				if (Constants.getBoolean("passwordUseHash"))
				{
					//v BasicNtlmLogonAction to takto nastavime a ulozime, kedze heslo realne nevieme
					password = user.getLogin();
				}

				if (user.isAdmin())
				{
					//prihlas ho
					Identity identity = new Identity();
					Map<String, String> errors = new Hashtable<>();
					String forward = LogonTools.logon(user.getLogin(), password, identity, errors, request, prop);
					if (forward.compareTo("logon_ok_admin") != 0 || identity == null || identity.isAdmin() == false)
					{
						identity = null;
					}
					else
					{
						identity.setLoginName(user.getLogin());
						identity.setPassword(password);
						identity.setValid(true);
						//je korektne prihlaseny
						LogonTools.setUserToSession(request.getSession(), identity);

						Logger.debug(NtlmLogonAction.class,"NtlmLogonAction: admin prihlaseny");
						if (request.getParameter("admin")!=null)
						{
							afterLogonUrl = "/admin";
						}
						logonSuccess = true;
					}
				}
				else
				{
					List<String> errors = LogonTools.logonUser(request, user.getLogin(), password);
					if (errors.isEmpty())
					{
						//je prihlaseny
						Logger.debug(NtlmLogonAction.class,"NtlmLogonAction: user je prihlaseny");
						logonSuccess = true;
					}
				}

				if (logonSuccess)
				{
					if (request.getParameter("origDocId")!=null)
					{
						afterLogonUrl = "/showdoc.do?docid="+request.getParameter("origDocId");
					}
					String afterLogonRedirect = (String)request.getSession().getAttribute("afterLogonRedirect");
					request.getSession().removeAttribute("afterLogonRedirect");
					request.setAttribute("afterLogonRedirect", afterLogonRedirect);
					if (Tools.isNotEmpty(afterLogonRedirect)) afterLogonUrl = afterLogonRedirect;

					response.sendRedirect(Tools.sanitizeHttpHeaderParam(afterLogonUrl));
					return;
				}
			}
			else
			{
				Logger.debug(NtlmLogonAction.class,"NIE JE TO NtlmHttpServletRequest");
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		if (AuthenticationFilter.getForbiddenURL()!=null)
		{
			response.sendRedirect(AuthenticationFilter.getForbiddenURL());
		}
		else
		{
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
		}
		return;
	}

	public static UserDetails authentificateUserAgainstLdap(HttpServletRequest request, HttpServletResponse response, String loginName) throws IOException, IllegalAccessException, InvocationTargetException, RedirectedException
	{
		//Skontroluj usera voci DB
		loginName = loginName.toLowerCase();
		UserDetails user = UsersDB.getUser(loginName);
		if (user == null)
		{
			//este neexistuje, treba vytvorit
			user = new UserDetails();
			user.setUserId(-1);
			user.setLogin(loginName);
			user.setPassword(Password.generatePassword(10));
		}

		doLdapQuery(user);

		if (Tools.isEmpty(user.getLogin()))
		{
			Logger.debug(NtlmLogonAction.class,"UserLogin je null: forbiddenURL="+AuthenticationFilter.getForbiddenURL());
			if (AuthenticationFilter.getForbiddenURL()!=null)
			{
				response.sendRedirect(AuthenticationFilter.getForbiddenURL());
			}
			else
			{
				response.sendError(HttpServletResponse.SC_FORBIDDEN);
			}
			throw new RedirectedException();
		}

		if (Constants.getBoolean("passwordProtectedAutoIdDontCreateUser")==true) {
			if (Tools.isNotEmpty(Constants.getString("passwordProtectedAutoId")) && isUserInProtectedGroup(user)) {
				logonAndRedirectUserInProtectedGroup(request, response, user);
				throw new RedirectedException();
			}
		}

		//ak nie je prideleny do ziadnej skupiny (a nie je to admin), posli ho prec
		if (Tools.isEmpty(user.getUserGroupsIds()) && user.isAdmin()==false && Constants.getBoolean("ntlmAllowEmptyGroups")==false)
		{
			Logger.debug(NtlmLogonAction.class,"UserGroups je null: forbiddenURL="+AuthenticationFilter.getForbiddenURL());
			if (AuthenticationFilter.getForbiddenURL()!=null)
			{
				response.sendRedirect(AuthenticationFilter.getForbiddenURL());
			}
			else
			{
				response.sendError(HttpServletResponse.SC_FORBIDDEN);
			}
			throw new RedirectedException();
		}

		if (Tools.isEmpty(user.getUserGroupsIds())) user.setUserGroupsIds("");

		user.setAuthorized(true);


		//toto je uz uplna haluz kvoli Transgasu, maju drbnuto nastaveny server
		/*String fn = user.getFirstName();
		Logger.debug(NtlmLogonAction.class, "PRED RECODE: "+fn);
		String fn1 = new String(user.getFirstName().getBytes(), "windows-1250");
		String fn2 = new String(user.getFirstName().getBytes("windows-1250"));
		String fn3 = new String(user.getFirstName().getBytes(), "iso-8859-1");
		String fn4 = new String(user.getFirstName().getBytes("iso-8859-1"));
		String fn5 = new String(user.getFirstName().getBytes("iso-8859-1"), "windows-1250");

		fn = "o:"+fn+" 1:"+fn1+" 2:"+fn2+" 3:"+fn3+" 4:"+fn4+" 5:"+fn5;
		Logger.debug(NtlmLogonAction.class, "RECODED: fn="+fn);*/
		String recode = Constants.getString("ntlmLogonAction.charsetEncoding");
		if (Tools.isNotEmpty(recode)) {
			//weird backward compatibility where user details are wrongly encoded
			user.setFirstName(new String(user.getFirstName().getBytes(), recode));
			user.setLastName(new String(user.getLastName().getBytes(), recode));
			user.setAdress(new String(user.getAdress().getBytes(), recode));
			user.setCity(new String(user.getCity().getBytes(), recode));
			user.setCompany(new String(user.getCompany().getBytes(), recode));
		}

		UserDetails adminUser = null;
		int defaultAdminUserId = Constants.getInt("NTLMDefaultAdminUserId");
		if (user.isAdmin() && user.getUserId()<1 && defaultAdminUserId>0)
		{
			//nacitaj default admin usera
			adminUser = UsersDB.getUser(defaultAdminUserId);
			if (adminUser!=null)
			{
				adminUser.setEmail(user.getEmail());
				adminUser.setTitle(user.getTitle());
				adminUser.setFirstName(user.getFirstName());
				adminUser.setLastName(user.getLastName());
				adminUser.setAdress(user.getAdress());
				adminUser.setCity(user.getCity());
				adminUser.setPSC(user.getPSC());
				adminUser.setCountry(user.getCountry());
				adminUser.setCompany(user.getCompany());
				adminUser.setPhone(user.getPhone());
				adminUser.setUserGroupsIds(user.getUserGroupsIds());
				adminUser.setAdmin(true);

				user = adminUser;
				user.setUserId(-1);
				user.setLogin(loginName);
				user.setPassword(Password.generatePassword(10));
			}
		}

		//dolezite, inak nas neprihlasi
		String password = user.getPassword();
		if (Constants.getBoolean("passwordUseHash"))
		{
			password = user.getLogin();
			user.setPassword(password);
			Logger.debug(NtlmLogonAction.class, "NtlmLogonAction - Pass use hash, nastavujem na "+user.getLogin());
		}

		UsersDB.saveUser(user);

		if (adminUser != null)
		{
			//uloz prava
			copyAdminUserGroupsApprove(defaultAdminUserId, user.getUserId());
			copyUserDisabledItems(defaultAdminUserId, user.getUserId());
		}

		if (user.isAuthorized())
        {
            try
            {
                //prihlasenie pre SPRING / REST
                RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
                if (requestBean != null)
                {
                    AuthenticationManager authenticationManager = requestBean.getSpringBean("authenticationManagerBean", AuthenticationManager.class);
                    if (authenticationManager != null)
                    {
                        final UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(user.getLogin(), password);
                        final Authentication authentication = authenticationManager.authenticate(authRequest);
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }
            catch (Exception ex)
            {
                sk.iway.iwcm.Logger.error(ex);
            }
        }

		return user;
	}

	private static void logonAndRedirectUserInProtectedGroup(HttpServletRequest request, HttpServletResponse response, UserDetails user) throws IllegalAccessException, InvocationTargetException, IOException
	{
		//stranky vidia len pouzivatelia ktory su spravne zalogovany
		String passwordProtectedAutoId = Constants.getString("passwordProtectedAutoId");
		Logger.debug(NtlmLogonAction.class, "passwordProtectedAutoId="+passwordProtectedAutoId);

		Logger.debug(NtlmLogonAction.class, "prihlasujem a nevytvaram v DB");

		//	akoze ho prihlasime a pouzijeme (takychto pouzivatelov v DB nevytvarame)
		user.setUserGroupsIds(passwordProtectedAutoId);
		user.setAuthorized(true);
		Identity userLogged = new Identity();
		BeanUtils.copyProperties(userLogged, user);
		LogonTools.setUserToSession(request.getSession(), userLogged);
		String afterLogonUrl = "/";
		if (request.getParameter("origDocId")!=null)
		{
			afterLogonUrl = "/showdoc.do?docid="+request.getParameter("origDocId");
		}
		response.sendRedirect(Tools.sanitizeHttpHeaderParam(afterLogonUrl));

		if (user.getUserId()>0)
		{
			Logger.debug(NtlmLogonAction.class, "userID="+user.getUserId()+" --- td");
		}
	}


	private static boolean isUserInProtectedGroup(UserDetails user)
	{
		//pouzivatelia, ktory patria (len) do tejto skupiny sa neukladaju do WebJETu
		String passwordProtectedAutoId = Constants.getString("passwordProtectedAutoId");
		return user.isAdmin()==false && passwordProtectedAutoId.equals(user.getUserGroupsIds()) && Constants.getBoolean("NTLMdontCreateAutoIdUser")==true;
	}


	public static void doLdapQuery(UserDetails user)
	{
		String ldapQueryMethod = Constants.getString("NTLMldapQueryMethod");
		if (Tools.isNotEmpty(ldapQueryMethod))
		{
			int i = ldapQueryMethod.lastIndexOf('.');
			String ldapQueryClass = ldapQueryMethod.substring(0, i);
			ldapQueryMethod = ldapQueryMethod.substring(i+1);
			//String
			try
			{
				Class<?> c = Class.forName(ldapQueryClass);
				Object o = c.getDeclaredConstructor().newInstance();
				Method m;
				Class<?>[] parameterTypes = new Class[] {UserDetails.class};
				Object[] arguments = new Object[] {user};
				m = c.getMethod(ldapQueryMethod, parameterTypes);
				m.invoke(o, arguments);
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}
			return;
		}

		DirContext ctx = null;

		String CNUserName = null;

		try
		{
	       Hashtable<String, String> env = new Hashtable<>();

	       boolean ldapUseSslProtocol = Constants.getBoolean("ldapUseSslProtocol");
	       if(ldapUseSslProtocol) env.put(Context.SECURITY_PROTOCOL, "ssl");
	       env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
	       env.put(Context.STATE_FACTORIES, "PersonStateFactory");
	       env.put(Context.OBJECT_FACTORIES, "PersonObjectFactory");
	       env.put(Context.PROVIDER_URL, AuthenticationFilter.getLdapProvider());  // SET YOUR SERVER AND STARTING CONTEXT HERE
	       env.put(Context.SECURITY_PRINCIPAL,  AuthenticationFilter.getLdapUsername()); //"cn=Administrator, o=oracle.local");  // SET USER THAT CAN SEARCH AND MODIFY FULL NAME HERE
	       env.put(Context.SECURITY_CREDENTIALS, AuthenticationFilter.getLdapPassword());  // SET PASSWORD HERE
	       env.put(Context.REFERRAL, "follow");
	       env.put(LdapContext.CONTROL_FACTORIES, "com.sun.jndi.ldap.ControlFactory");

	       ctx = new InitialDirContext(env);

	       // Specify the search filter to match all users with no full name
	       String filter = "(&(objectClass=Person) (&(sAMAccountName="+user.getLogin()+")))"; // "DC=oracle, (&(objectClass=Person) (&(sn=priezvisko)))";
	       // limit returned attributes to those we care about
	       //String[] attrIDs = {"sn", "givenName", "mail"};
	       SearchControls ctls = new SearchControls();
	       //ctls.setReturningAttributes(attrIDs);
	       // comment out next line to limit to one container otherwise it'll walk down the tree
	       ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);

	       // Search for objects using filter and controls
	       NamingEnumeration<SearchResult> answer = ctx.search("", filter, ctls);

	       //String ldapGroups = null;
	       // cycle through result set
	       if (answer.hasMore())
	       {
	           SearchResult sr = answer.next();
	           Attributes attrs = sr.getAttributes();

	           //DEBUG: treba vymazat
	           /*NamingEnumeration all = attrs.getAll();
	           //Logger.println(this,"POCET: " + attrs.size());
	           while (all.hasMoreElements())
	           {
	              Attribute a = (Attribute)all.next();
	              //Logger.println(this,"ENUM: " + a);
	           }*/

	           user.setEmail(getAtrValue(attrs, "mail", user.getEmail()));
	           user.setFirstName(getAtrValue(attrs, "givenName", "")); //user.getFirstName()
	           user.setLastName(getAtrValue(attrs, "sn", "")); //user.getLastName()
	           user.setAdress(getAtrValue(attrs, "streetAddress", user.getAdress()));
	           user.setCity(getAtrValue(attrs, "l", user.getCity()));
	           user.setPSC(getAtrValue(attrs, "postalCode", user.getPSC()));
	           user.setCountry(getAtrValue(attrs, "co", user.getCountry()));
	           user.setCompany(getAtrValue(attrs, "company", user.getCompany()));
	           user.setPhone(getAtrValue(attrs, "telephoneNumber", user.getPhone()));
	           user.setFax(getAtrValue(attrs, "mobile", user.getFax()));

	           CNUserName = getAtrValue(attrs, "cn", "");

	           //ldapGroups = getAtrValue(attrs, "memberOf", null) + ", ";

	           // eDir returns "attribute name : attribute value on get method so strip off up to ": "
	           //attrs.put("fullName", givenName.substring(givenName.indexOf(':')+2) + ' ' + surName.substring(surName.indexOf(':')+2));
	           //ctx.modifyAttributes(sr.getName(), DirContext.REPLACE_ATTRIBUTE, attrs);
	       }
	       else
	       {
	      	 user.setLogin(null);
	      	 return;
	       }

	       // Close the context when we're done
		    ctx.close();
		    ctx = null;

		    /*Logger.debug(NtlmLogonAction.class,"Nastavujem skupiny:\n"+ldapGroups);
	       if (ldapGroups != null)
	       {
	       	String groupsString = null;
				List allGroups = UserGroupsDB.getInstance().getUserGroups();
				Iterator iter = allGroups.iterator();
				UserGroupDetails ugd;
				while (iter.hasNext())
				{
					ugd = (UserGroupDetails)iter.next();
					Logger.debug(NtlmLogonAction.class,"testujem skupinu: " + ugd.getUserGroupName());
					if (ldapGroups.indexOf("CN="+ugd.getUserGroupName()+",")!=-1)
					{
						Logger.debug(NtlmLogonAction.class,"je clenom skupiny");
						if (groupsString == null)
						{
							groupsString = ""+ugd.getUserGroupId();
						}
						else
						{
							groupsString += ","+ugd.getUserGroupId();
						}
					}
				}
				Logger.debug(NtlmLogonAction.class,"nastavujem skupinyIDS: " + groupsString);
				user.setUserGroupsIds(groupsString);

		       if (ldapGroups.indexOf("CN="+Constants.getString("NTLMAdminGroupName")+",")!=-1)
		       {
		       	Logger.debug(NtlmLogonAction.class,"USER JE ADMIN");
					user.setAdmin(true);
		       }
		       else
		       {
		       	Logger.debug(NtlmLogonAction.class,"USER NIE JE ADMIN");
		       	user.setAdmin(false);
		       }
	       }*/

			 StringBuilder groupsString = null;

			 if (Tools.isNotEmpty(Constants.getString("passwordProtectedAutoId")))
			 {
			 	groupsString = new StringBuilder(Constants.getString("passwordProtectedAutoId"));
			 }

			 if (Tools.isEmpty(CNUserName))
			 {
				 CNUserName = user.getLastName() + " " + user.getFirstName();
				 if (Constants.getBoolean("NTLMldapIsSlovak")==true)
				 {
					 CNUserName = user.getFirstName() + " " + user.getLastName();
				 }
				 //odstran medzeru na konci/zaciatku, ked ma zadane len meno
				 CNUserName = DB.internationalToEnglish(CNUserName).toLowerCase().trim();
			 }

			 for (UserGroupDetails ugd : UserGroupsDB.getInstance().getUserGroups())
			 {
				 Logger.debug(NtlmLogonAction.class,"testujem skupinu: " + ugd.getUserGroupName());
				 if (isMemberOf(user.getLogin(), CNUserName, ugd.getUserGroupName(), 1, null))
				 {
					Logger.debug(NtlmLogonAction.class,"je clenom skupiny " + ugd.getUserGroupName());
					if (groupsString == null)
					{
						groupsString = new StringBuilder(String.valueOf(ugd.getUserGroupId()));
					}
					else
					{
						groupsString.append(',').append(ugd.getUserGroupId());
					}
				 }
				 else
				 {
					 Logger.debug(NtlmLogonAction.class,"NIEje clenom skupiny " + ugd.getUserGroupName());
				 }
			 }
			 Logger.debug(NtlmLogonAction.class,"nastavujem skupiny: " + groupsString);
			 if (groupsString != null)
			 {
				 user.setUserGroupsIds(groupsString.toString());

				 if (groupsString.toString().equals(Constants.getString("passwordProtectedAutoId"))==false)
				 {
				 	//ak to nie je autoid neries nastavenie admina, zacovaj z DB
					 //otestuj admina
					 if (isMemberOf(user.getLogin(), CNUserName, Constants.getString("NTLMAdminGroupName"), 1, null)) {
						 Logger.debug(NtlmLogonAction.class, "USER JE ADMIN");
						 user.setAdmin(true);
					 } else {
						 Logger.debug(NtlmLogonAction.class, "USER NIE JE ADMIN");
						 user.setAdmin(false);
					 }
				 }
			 }

			if (Constants.getBoolean("passwordUseHash"))
			{
				user.setPassword(user.getLogin());
			}

	     }
	     catch(NamingException ne)
		  {
	       Logger.error(NtlmLogonAction.class,ne.getMessage());
	       sk.iway.iwcm.Logger.error(ne);
	     }
	     finally
	     {
	   	   if (ctx != null)
				{
					try
					{
						ctx.close();
					}
					catch (Exception e2)
					{
					}
				}
	     }
	}

	/**
	 * Ziska atribut a dekoduje jeho hodnotu, alebo vrati defaultValue
	 * @param attrs
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public static String getAtrValue(Attributes attrs, String name, String defaultValue)
	{
		String ret = defaultValue;
		try
		{
			Attribute a;
         a = attrs.get(name);
         //Logger.println(this,"LDAP: "+name+"="+a);
         if (a!=null)
         {
         	Object o = a.get();

         	Logger.debug(NtlmLogonAction.class, "o="+o.getClass());

	         ret = recodeString((String)o);
         }
         Logger.debug(NtlmLogonAction.class, "getAtrValue("+name+")="+ret);
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		if (ret == null) ret = "";
		return(ret);
	}

	private static String recodeString(String str)
	{
		//je to spravne nemusime robit nic
		if (Tools.isEmpty(Constants.getString("NTLMldapEncoding")) || Constants.getString("NTLMldapEncoding").length()<2)
		{
			return(str);
		}
		if (str == null) return(null);
		try
		{
			String newStr = new String(str.getBytes(Constants.getString("NTLMldapEncoding")));
			Logger.debug(NtlmLogonAction.class, "Encoding string ("+str+") to: "+Constants.getString("NTLMldapEncoding")+" new="+newStr);
			return(newStr);
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return(str);
	}

	public static boolean isMemberOf(String login, String userCN, String groupCN, int actualLevel, DirContext ctx)
	{
		String isMemberOfMethod = Constants.getString("NTLMisMemberOfMethod");
		if (Tools.isNotEmpty(isMemberOfMethod))
		{
			int i = isMemberOfMethod.lastIndexOf('.');
			String isMemberOfClass = isMemberOfMethod.substring(0, i);
			isMemberOfMethod = isMemberOfMethod.substring(i+1);
			//String
			try
			{
				Class<?> c = Class.forName(isMemberOfClass);
				Object o = c.getDeclaredConstructor().newInstance();
				Method m;
				Class<?>[] parameterTypes = new Class[] {String.class, String.class, String.class, int.class, DirContext.class};
				Object[] arguments = new Object[] {login, userCN, groupCN, actualLevel, ctx};
				m = c.getMethod(isMemberOfMethod, parameterTypes);
				return((Boolean)m.invoke(o, arguments));
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}
		}

		//groupCN = getNameFromCNString(groupCN);

		String spacer = "                                                                                                           ";
		try
		{
			spacer = spacer.substring(0, actualLevel*3);
		}
		catch (Exception e)
		{
			spacer = "";
		}

		Logger.debug(NtlmLogonAction.class, spacer+"isMemberOf -------- START userCN="+userCN+" groupCN="+groupCN+" level="+actualLevel);

		//DirContext ctx = null;
		boolean otvorilSomCTX = false;
		String filter = null;
		try
		{
			//	najskor ziskaj pouzivatelov tejto skupiny
			String baseProvider = AuthenticationFilter.getLdapProvider();
			int i = baseProvider.indexOf('/', 10);
			String providerURL = baseProvider.substring(0, i);
			String searchBase = baseProvider.substring(i+1);
			i = searchBase.indexOf('?');
			if (i>0) searchBase = searchBase.substring(0, i);

			boolean groupHasCN = false;
			if (groupCN.indexOf("CN=")!=-1)
			{
				groupHasCN = true;
			}

			filter = "(&(objectClass=*)(CN=" + groupCN + "))";
			if (groupHasCN)
			{
				//filter = "(objectClass=*)";
				searchBase = getBaseFromCNString(groupCN); // "OU=Limuzska Groups,DC=oracle,DC=local";
				filter = "(&(objectClass=*)(CN=" + getNameFromCNString(groupCN) + "))";
			}

			//	over ci to nie je prazdne
			if (emptyGroupsTable.get(filter)!=null)
			{
				Logger.debug(NtlmLogonAction.class, spacer+"toto je prazdne: " + groupCN + " vraciam sa z isMemeberOf");
				return(false);
			}

			Logger.debug(NtlmLogonAction.class, spacer+"provider="+providerURL);

			Hashtable<String, String> env = new Hashtable<>();
			boolean ldapUseSslProtocol = Constants.getBoolean("ldapUseSslProtocol");
			if(ldapUseSslProtocol) env.put(Context.SECURITY_PROTOCOL, "ssl");
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
			env.put(Context.STATE_FACTORIES, "PersonStateFactory");
			env.put(Context.OBJECT_FACTORIES, "PersonObjectFactory");
			env.put(Context.PROVIDER_URL, providerURL); //AuthenticationFilter.getLdapProvider()); // SET YOUR SERVER AND STARTING CONTEXT HERE
			env.put(Context.SECURITY_PRINCIPAL, AuthenticationFilter.getLdapUsername()); //"cn=Administrator, o=oracle.local");  // SET USER THAT CAN SEARCH AND MODIFY FULL NAME HERE
			env.put(Context.SECURITY_CREDENTIALS, AuthenticationFilter.getLdapPassword()); // SET PASSWORD HERE
			env.put(LdapContext.CONTROL_FACTORIES, "com.sun.jndi.ldap.ControlFactory");

			//teraz sprav skupiny
			if (ctx == null)
			{
				Logger.debug(NtlmLogonAction.class, spacer+"Otvaram CTX");
				ctx = new InitialDirContext(env);
				otvorilSomCTX = true;
			}

			Logger.debug(NtlmLogonAction.class, spacer+"searchBase="+searchBase);
			Logger.debug(NtlmLogonAction.class, spacer+"filter="+filter);

			//	     Create the search controls
			SearchControls searchCtls = new SearchControls();
			//Specify the search scope
			searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			String[] returnedAtts = {"member"};
			//	     Specify the attributes to return
			searchCtls.setReturningAttributes(returnedAtts);
			// Search for objects using filter and controls
			NamingEnumeration<SearchResult> answer = ctx.search(searchBase, filter, searchCtls);
			// cycle through result set
			int totalResults = 0;

			List<String> groupNames = new ArrayList<>();
			boolean membersIsEmpty = true;
			if (answer.hasMore())
			{
				SearchResult sr = answer.next();
				//Logger.debug(NtlmLogonAction.class, spacer+"sr=" + sr.getName());
				Attributes attrs = sr.getAttributes();
				if (attrs != null)
				{
					try
					{
						for (NamingEnumeration<? extends Attribute> ae = attrs.getAll(); ae.hasMore();)
						{
							Attribute attr = ae.next();
							//Logger.debug(NtlmLogonAction.class, spacer+"Attribute: " + attr.getID());
							for (NamingEnumeration<?> e = attr.getAll(); e.hasMore(); totalResults++)
							{
								membersIsEmpty = false;
								String name = recodeString((String)e.next());
								String nameLC = DB.internationalToEnglish(name).toLowerCase().trim();
								String userCNLC = DB.internationalToEnglish(userCN).toLowerCase().trim();
								//to druhe je tam kvoli Meno Priezvisko Ing.
								Logger.debug(NtlmLogonAction.class, spacer+" testing: nameLC=" + nameLC + " userCNLC="+userCNLC);
								if ((nameLC+",").indexOf("cn="+userCNLC+",")!=-1 || (nameLC+",").indexOf("cn="+userCNLC+" ")!=-1)
								{
									Logger.debug(NtlmLogonAction.class, spacer+"GRP: " + totalResults + ". " + name);
									Logger.debug(NtlmLogonAction.class, spacer+userCN+"-------------------user najdeny !!!!");
									return(true);
								}
								groupNames.add(name);
							}
						}
					}
					catch (NamingException e)
					{
						Logger.error(NtlmLogonAction.class, spacer+"Problem listing members: " + e);
					}
				}
			}
			if (membersIsEmpty)
			{
				Logger.debug(NtlmLogonAction.class, spacer+"vrateny zoznam je prazdny, davam do cache: " + groupCN);
				emptyGroupsTable.put(filter, groupCN);
			}
			// Close the context when we're done
			//ctx.close();

			totalResults = 0;
			for (String name : groupNames)
			{
				totalResults++;
				Logger.debug(NtlmLogonAction.class, spacer+"GRP: " + totalResults + ". " + name);

				//zavolaj rekurziu
				boolean found = isMemberOf(login, userCN, name, actualLevel+1, ctx);
				if (found)
				{
					return(true);
				}
			}
		}
		catch (NameNotFoundException nfe)
		{
			Logger.debug(NtlmLogonAction.class, spacer+"NFE group="+groupCN+" user="+userCN+" chyba: "+nfe.getMessage());
			Logger.debug(NtlmLogonAction.class, spacer+"davam do cache: " + groupCN);
			emptyGroupsTable.put(filter, groupCN);
		}
		catch (PartialResultException pre)
		{
			Logger.debug(NtlmLogonAction.class, spacer+pre.getMessage()+" PRE group="+groupCN+" user="+userCN);
		}
		catch (Exception e)
		{
			Logger.error(NtlmLogonAction.class, spacer+e);
			sk.iway.iwcm.Logger.error(e);
		}
		finally
		{
			if (otvorilSomCTX && ctx != null)
			{
				try
				{
					Logger.debug(NtlmLogonAction.class, "Zatvaram CTX");
					ctx.close();
				}
				catch (Exception e2)
				{
				}
			}
		}

		Logger.debug(NtlmLogonAction.class, spacer+"isMemberOf -------- END");
		return (false);
	}

	/**
	 * Vrati len hodnotu CN z retazca
	 * @param cnString
	 * @return
	 */
	private static String getNameFromCNString(String cnString)
	{
		int start = cnString.indexOf("CN=");
		int end = cnString.indexOf(',');
		if (start == -1) return(cnString);
		if (end == -1) return(cnString.substring(start+3).trim());
		return(cnString.substring(start+3, end).trim());
	}

	/**
	 * Vrati vsetko okrem CN z retazca
	 * @param cnString
	 * @return
	 */
	private static String getBaseFromCNString(String cnString)
	{
		int start = cnString.indexOf("CN=");
		int end = cnString.indexOf(',');
		if (start == -1) return(cnString);
		if (end == -1) return("");
		return(cnString.substring(end+1).trim());
	}

	private static void copyAdminUserGroupsApprove(int originalUserId, int newUserId)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			Map<Integer, Integer> approveTable = new Hashtable<>();

			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT group_id, approve_mode FROM groups_approve WHERE user_id=?");
			ps.setInt(1, originalUserId);
			rs = ps.executeQuery();
			while (rs.next())
			{
				approveTable.put(rs.getInt("group_id"), rs.getInt("approve_mode"));
			}
			rs.close();
			ps.close();

			ps = db_conn.prepareStatement("DELETE FROM groups_approve WHERE user_id=?");
			ps.setInt(1, newUserId);
			ps.execute();
			ps.close();

			ps = db_conn.prepareStatement("INSERT INTO groups_approve (user_id, group_id, approve_mode) VALUES (?, ?, ?)");
			ps.setInt(1, newUserId);

			for (Map.Entry<Integer, Integer> entry : approveTable.entrySet())
			{
				Integer groupId = entry.getKey();
				Integer approveMode = entry.getValue();
				ps.setInt(2, groupId);
				ps.setInt(3, approveMode);
				ps.execute();

				Logger.debug(NtlmLogonAction.class, "copyAdminUserGroupsApprove userId="+newUserId+" groupId="+groupId+" approveMode="+approveMode);
			}

			ps.close();

			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}
	}

	private static void copyUserDisabledItems(int originalUserId, int newUserId)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			Map<String, String> permsTable = new Hashtable<>();

			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT item_name FROM user_disabled_items WHERE user_id=?");
			ps.setInt(1, originalUserId);
			rs = ps.executeQuery();
			while (rs.next())
			{
				permsTable.put(DB.getDbString(rs, "item_name"), "1");
			}
			rs.close();
			ps.close();

			ps = db_conn.prepareStatement("DELETE FROM user_disabled_items WHERE user_id=?");
			ps.setInt(1, newUserId);
			ps.execute();
			ps.close();

			ps = db_conn.prepareStatement("INSERT INTO user_disabled_items (user_id, item_name) VALUES (?, ?)");
			ps.setInt(1, newUserId);

			for (Map.Entry<String, String> entry : permsTable.entrySet())
			{
				String itemName = entry.getKey();

				ps.setString(2, itemName);
				ps.execute();

				Logger.debug(NtlmLogonAction.class, "copyUserDisabledItems userId="+newUserId+" itemName="+itemName);
			}

			ps.close();

			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}
	}

	/**
	 *
	 * Odstrani zo zadaneho stringu znaky, ktore
	 * by mohli zmenit formatovanie a vyznam
	 * LDAPovskeho filtru
	 *
	 * @param originalString
	 * @return escaped {@link String}
	 */
	public static String escapeLdapString(String originalString)
	{
		StringBuilder escapedString = new StringBuilder( originalString.length() );

		for (int i=0;i<originalString.length();i++)
		{
			Character toBeInvestigated = originalString.charAt(i);
			if ( Character.isLetterOrDigit( toBeInvestigated ) ||
					toBeInvestigated.toString().matches("^["+Constants.getString("ldapLoginChars")+"]$") )
				escapedString.append(toBeInvestigated);

		}

		return escapedString.toString();
	}
}
