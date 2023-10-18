package sk.iway.iwcm.system.ntlm;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.apache.commons.collections.CollectionUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.gallery.GalleryDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmOutputStream;
import sk.iway.iwcm.users.PermissionGroupBean;
import sk.iway.iwcm.users.PermissionGroupDB;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UserGroupDetails;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.iwcm.users.UsersDB;

/**
 *  BasicNtlmLogon.java - univerzalne overenie pouzivatela v ActiveDirectory (LDAP)
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2012
 *@author       $Author: jeeff jeeff $
 *@version      $Revision: 1.3 $
 *@created      Date: 15.2.2012 13:54:31
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class BasicNtlmLogon
{
	protected BasicNtlmLogon() {
		//utility class
	}
	/**
	 * LDAP dotaz pre autorizaciu v ActiveDirectory
	 * @param user
	 */
	public static void doLdapQuery(UserDetails user)
	{
		doLdapQuery(user, AuthenticationFilter.getLdapProvider(), AuthenticationFilter.getLdapUsername(), AuthenticationFilter.getLdapPassword());
	}

	/**
	 * LDAP dotaz pre autorizaciu
	 * @param user
	 * @param ldapProviderUrl
	 * @param ldapUsername
	 * @param ldapPassword
	 */
	@SuppressWarnings("unchecked")
	public static void doLdapQuery(UserDetails user, String ldapProviderUrl, String ldapUsername, String ldapPassword)
	{
		DirContext ctx = null;

		try
		{
			Logger.debug(BasicNtlmLogon.class, "Logging user: "+ user.getLogin());

			Hashtable<String, Object> env = new Hashtable<>(); //NOSONAR

			String ldapDomainAppend = Constants.getString("ldapDomainAppend");

			boolean ldapUseSslProtocol = Constants.getBoolean("ldapUseSslProtocol");
			if(ldapUseSslProtocol) env.put(Context.SECURITY_PROTOCOL, "ssl");
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
			env.put(Context.STATE_FACTORIES, "PersonStateFactory");
			env.put(Context.OBJECT_FACTORIES, "PersonObjectFactory");
			env.put(Context.PROVIDER_URL, ldapProviderUrl);  // SET YOUR SERVER AND STARTING CONTEXT HERE
			String ldapSecurityPrincipalDn = Constants.getString("ldapSecurityPrincipalDn"); //napr. cn=!USERNAME!,dc=ad,dc=interway,dc=sk
			if(Tools.isEmpty(ldapSecurityPrincipalDn)) ldapSecurityPrincipalDn = ldapUsername+ldapDomainAppend;
			else ldapSecurityPrincipalDn = Tools.replace(ldapSecurityPrincipalDn, "!USERNAME!", ldapUsername+ldapDomainAppend);
			env.put(Context.SECURITY_PRINCIPAL, ldapSecurityPrincipalDn); //"cn=Administrator, o=oracle.local");  // SET USER THAT CAN SEARCH AND MODIFY FULL NAME HERE
			env.put(Context.SECURITY_CREDENTIALS, ldapPassword);  // SET PASSWORD HERE
			env.put(Context.REFERRAL, "follow");
			env.put(Context.LANGUAGE, "sk-SK");
			env.put(LdapContext.CONTROL_FACTORIES, "com.sun.jndi.ldap.ControlFactory");

			Logger.debug(BasicNtlmLogon.class, "ldap: ldapProviderUrl="+ldapProviderUrl+" ldapSecurityPrincipalDn="+ldapSecurityPrincipalDn+" ldapUsername="+ldapUsername+ldapDomainAppend);

			ctx = new InitialDirContext(env);

			// Specify the search filter to match all users with no full name
			String filter = Constants.getString("ldapFilter");  //"(&(objectClass=Person) (&(sAMAccountName="+user.getLogin()+")))"; // "DC=oracle, (&(objectClass=Person) (&(sn=priezvisko)))";
			filter = Tools.replace(filter, "!USERNAME!", user.getLogin()+ldapDomainAppend);

			// limit returned attributes to those we care about
			//String[] attrIDs = {"sn", "givenName", "mail"};
			SearchControls ctls = new SearchControls();
			//ctls.setReturningAttributes(attrIDs);
			// comment out next line to limit to one container otherwise it'll walk down the tree
			ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);

			String[] retAttrs = Tools.getTokens(Constants.getString("basicNtlmLogonAttrs"), ",");
			ctls.setReturningAttributes(retAttrs);

			// Search for objects using filter and controls
			NamingEnumeration<SearchResult> answer = ctx.search("", filter, ctls);

			StringBuilder ldapGroups = new StringBuilder();
			// cycle through result set
			if (answer.hasMore())
			{
				SearchResult sr = answer.next();
				Attributes attrs = sr.getAttributes();

				//DEBUG: treba vymazat
				/*
				NamingEnumeration allAttrs = attrs.getAll();
				Logger.debug(BasicNtlmLogon.class,"POCET: " + attrs.size());
				while (allAttrs.hasMoreElements())
				{
					Attribute a = (Attribute)allAttrs.next();
					Logger.debug(BasicNtlmLogon.class,"ENUM: " + a);
				}
				*/

				String origFirstName = user.getFirstName();
				String origLastName = user.getLastName();

				if (isAtrAvailable(retAttrs, "mail")) user.setEmail(NtlmLogonAction.getAtrValue(attrs, "mail", user.getEmail()));
				//user.setTitle(NtlmLogonAction.getAtrValue(attrs, "title", user.getTitle()));

				if (isAtrAvailable(retAttrs, "cn"))
				{
					String cn = NtlmLogonAction.getAtrValue(attrs, "cn", null);
					if (Tools.isNotEmpty(cn) && cn.indexOf(",")!=-1)
					{
						try
						{
							user.setTitle(cn.substring(cn.indexOf(",")).trim());
						}
						catch (Exception e)
						{
							sk.iway.iwcm.Logger.error(e);
						}
					}
				}

				if (isAtrAvailable(retAttrs, "givenName")) user.setFirstName(NtlmLogonAction.getAtrValue(attrs, "givenName", user.getFirstName()));
				if (isAtrAvailable(retAttrs, "sn")) user.setLastName(NtlmLogonAction.getAtrValue(attrs, "sn", user.getLastName()));
				if (isAtrAvailable(retAttrs, "streetAddress")) user.setAdress(NtlmLogonAction.getAtrValue(attrs, "streetAddress", user.getAdress()));
				if (isAtrAvailable(retAttrs, "l")) user.setCity(NtlmLogonAction.getAtrValue(attrs, "l", user.getCity()));
				if (isAtrAvailable(retAttrs, "postalCode")) user.setPSC(NtlmLogonAction.getAtrValue(attrs, "postalCode", user.getPSC()));
				if (isAtrAvailable(retAttrs, "co")) user.setCountry(NtlmLogonAction.getAtrValue(attrs, "co", user.getCountry()));
				if (isAtrAvailable(retAttrs, "company")) user.setCompany(NtlmLogonAction.getAtrValue(attrs, "company", user.getCompany()));
				if (isAtrAvailable(retAttrs, "telephoneNumber")) user.setPhone(NtlmLogonAction.getAtrValue(attrs, "telephoneNumber", user.getPhone()));

				if (Tools.isEmpty(user.getLastName())) user.setLastName(origLastName);
				if (Tools.isEmpty(user.getFirstName())) user.setFirstName(origFirstName);

				if (isAtrAvailable(retAttrs, "department")) user.setFieldC(NtlmLogonAction.getAtrValue(attrs, "department", user.getFieldC()));
				if (isAtrAvailable(retAttrs, "description")) user.setSignature(NtlmLogonAction.getAtrValue(attrs, "description", user.getSignature()));
				if (isAtrAvailable(retAttrs, "title")) user.setPosition(NtlmLogonAction.getAtrValue(attrs, "title", user.getPosition()));

				//CNUserName = NtlmLogonAction.getAtrValue(attrs, "cn", "");

				//ldapGroups = NtlmLogonAction.getAtrValue(attrs, "memberOf", null) + ", ";

				if (isAtrAvailable(retAttrs, "memberOf"))
				{
					Attribute memberOf = attrs.get("memberOf");
					if (memberOf != null)
					{
						NamingEnumeration<?> all = memberOf.getAll();
						while (all.hasMoreElements())
						{
							Object o = all.next();

							if (o == null) continue;

							Logger.debug(BasicNtlmLogon.class, "memberOf: "+o.toString());

							if (ldapGroups.length()>0) ldapGroups.append("\n");
							ldapGroups.append(o.toString());
						}
					}
				}

				if (isAtrAvailable(retAttrs, "distinguishedName"))
				{
					//: OU=2,OU=1,OU=mzpsr,DC=mzp,DC=enviro,DC=gov,DC=sk
					//distinguishedName: CN=Cerovska,OU=2,OU=1,OU=mzpsr,DC=mzp,DC=enviro,DC=gov,DC=sk
					String distinguishedName = NtlmLogonAction.getAtrValue(attrs, "distinguishedName", null);
					try
					{
						//ziskaj info o adresari v ktorom je
						String ldapDNSuffix = Constants.getString("ldapDNSuffix");
						if (Tools.isNotEmpty(ldapDNSuffix) && Tools.isNotEmpty(distinguishedName))
						{
							int start = distinguishedName.indexOf(",");
							int end = distinguishedName.indexOf(ldapDNSuffix);
							if (start > 0 && end > start)
							{
								String groupDN = distinguishedName.substring(start+1, end-1);
								Logger.debug(BasicNtlmLogon.class, "Getting groupDN="+groupDN);
								Attributes groupAtrs = ctx.getAttributes(groupDN);
								if (groupAtrs != null)
								{
									String description = NtlmLogonAction.getAtrValue(groupAtrs, "description", null);
									if (Tools.isNotEmpty(description))
									{
										user.setFieldA(description);
									}
								}
							}
						}
					}
					catch (Exception e)
					{
						sk.iway.iwcm.Logger.error(e);
					}
				}

				if (Constants.getBoolean("passwordUseHash"))
				{
					user.setPasswordPlain(user.getLogin());
				}

				if (isAtrAvailable(retAttrs, "thumbnailPhoto"))
				{
					Attribute photo = attrs.get("thumbnailPhoto");
					Logger.debug(BasicNtlmLogon.class, "thumbnailPhoto="+photo);
					if (photo != null)
					{
						byte[] photoBytes = (byte[])photo.get();
						Logger.debug(BasicNtlmLogon.class, "photoBytes.length="+photoBytes.length);
						if (photoBytes.length > 100)
						{
							String BASE_DIR = "/images/gallery/user/";
							String photoURL = BASE_DIR+ DocTools.removeChars(user.getLogin(), true)+".jpg";

							Logger.debug(BasicNtlmLogon.class, "Saving user photo: "+photoURL);

							IwcmFile photoFile = new IwcmFile(Tools.getRealPath(photoURL));
							if(!photoFile.exists())
							{
								//zapis do suboru
								IwcmOutputStream ios = new IwcmOutputStream(photoFile);
								ios.write(photoBytes);
								ios.close();

								//zmaz o_ a s_ fotku
								IwcmFile oPhotoFile = new IwcmFile(Tools.getRealPath(GalleryDB.getImagePathOriginal(photoURL)));
								if (oPhotoFile.exists()) oPhotoFile.delete();

								IwcmFile sPhotoFile = new IwcmFile(Tools.getRealPath(GalleryDB.getImagePathSmall(photoURL)));
								if (sPhotoFile.exists()) sPhotoFile.delete();

								//resizni
								Prop prop = Prop.getInstance();
								Dimension[] dims = GalleryDB.getDimension(BASE_DIR);
								GalleryDB.resizePictureImpl(dims, photoFile.getAbsolutePath(), null, prop, GalleryDB.getResizeMode(BASE_DIR));
							}
							else
							{
								Logger.debug(BasicNtlmLogon.class, "User photo: "+photoURL + " already exists, using existing file");
							}
							user.setPhoto(photoURL);
						}
					}
				}
				if (Tools.isNotEmpty(Constants.getString("ntlmDefaultUserPhoto")) && Tools.isEmpty(user.getPhoto()))
				{
					user.setPhoto(Constants.getString("ntlmDefaultUserPhoto"));
				}

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

			//moznost mat viac administratorskych AD skupin
			String[] ntlmAdminGroupNames = Tools.getTokens(Constants.getString("NTLMAdminGroupName"), ",");
			if (ntlmAdminGroupNames != null && ntlmAdminGroupNames.length > 0)
			{
				user.setAdmin(false);
				for(String groupName : ntlmAdminGroupNames)
				{
					if (ldapGroups.indexOf("CN="+groupName+",")!=-1)
					{
						Logger.debug(BasicNtlmLogon.class,"Je clenom skupiny '"+groupName+"', USER JE ADMIN");
						user.setAdmin(true);
						break;
					}
				}
				if(user.isAdmin() == false) Logger.debug(BasicNtlmLogon.class,"USER NIE JE ADMIN");
			}

			if (retAttrs.length>0)
			{
				Logger.debug(BasicNtlmLogon.class,"Nastavujem skupiny:\n"+ldapGroups.toString());

				//porovnavam najprv nazvy skupin prav
				List<PermissionGroupBean> permissionGroupsAll = (new PermissionGroupDB()).getAll();
				if(permissionGroupsAll != null)
				{
					List<PermissionGroupBean> permissionGroupsAfter = new ArrayList<>();

					for (PermissionGroupBean pg : permissionGroupsAll)
					{
						Logger.debug(BasicNtlmLogon.class, "testujem skupinu prav: " + pg.getTitle());
						if (ldapGroups.indexOf("CN=" + pg.getTitle() + ",") != -1)
						{
							Logger.debug(BasicNtlmLogon.class, "JE clenom skupiny prav");
							permissionGroupsAfter.add(pg);
						}
					}

					//potrebujem userId
					if(user.getUserId() < 1) UsersDB.saveUser(user);
					List<PermissionGroupBean> permissionGroupsBefore = UserGroupsDB.getPermissionGroupsFor(user.getUserId());
					for (PermissionGroupBean permGroup : (Collection<PermissionGroupBean>)CollectionUtils.subtract(permissionGroupsAfter, permissionGroupsBefore))
						UsersDB.addUserToPermissionGroup(user.getUserId(), permGroup.getUserPermGroupId());

					for (PermissionGroupBean permGroup : (Collection<PermissionGroupBean>)CollectionUtils.subtract(permissionGroupsBefore, permissionGroupsAfter))
						UsersDB.deleteUserFromPermissionGroup(user.getUserId(), permGroup.getUserPermGroupId());
				}

				//porovnavam aj pouzivatelske skupiny
				StringBuilder groupsString = new StringBuilder(Constants.getString("passwordProtectedAutoId"));
				for (UserGroupDetails ugd : UserGroupsDB.getInstance().getUserGroups())
				{
					Logger.debug(BasicNtlmLogon.class, "testujem pouzivatelsku skupinu: " + ugd.getUserGroupName());
					if (ldapGroups.indexOf("CN=" + ugd.getUserGroupName() + ",") != -1)
					{
						Logger.debug(BasicNtlmLogon.class, "je clenom pouzivatelskej skupiny");
						if (Tools.isNotEmpty(groupsString)) groupsString.append(",");
						groupsString.append("" + ugd.getUserGroupId());
					}
				}
				Logger.debug(BasicNtlmLogon.class,"nastavujem skupinyIDS: " + groupsString.toString());
				user.setUserGroupsIds(groupsString.toString());
			}
		}
		catch(Exception ne)
		{
			Logger.error(BasicNtlmLogon.class,ne.getMessage());
			sk.iway.iwcm.Logger.error(ne);
			user.setLogin(null);
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
	 * Otestuje, ci dany atribut mame nacitany (a mozeme ho nastavit)
	 * @param atrs
	 * @param name
	 * @return
	 */
	private static boolean isAtrAvailable(String[] atrs, String name)
	{
		if (atrs == null || atrs.length<1) return false;
		for (String atr : atrs)
		{
			if (atr.equals(name)) return true;
		}
		return false;
	}
}
