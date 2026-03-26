package sk.iway.iwcm.users;

import com.fasterxml.jackson.annotation.JsonIgnore;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.ForumTools;
import sk.iway.iwcm.common.GalleryToolsForCore;
import sk.iway.iwcm.components.enumerations.EnumerationDataDB;
import sk.iway.iwcm.components.enumerations.model.EnumerationDataBean;
import sk.iway.iwcm.io.IwcmFile;

import jakarta.servlet.http.HttpServletRequest;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.*;

/**
 *  Drzi info o pouzivatelovi (z tabulky users)
 *
 *@Title        Interway Content Management
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.4 $
 *@created      $Date: 2004/03/08 14:53:59 $
 */
public class UserDetails implements Serializable
{
	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 680672045576197626L;

	protected int userId = -1;
	protected String login = "";
	protected boolean admin = false;

	protected String company = "";
	protected String adress = "";
	protected String email = "";
	protected String country = "";
	protected String phone = "";
	protected String zipCode = "";
	protected boolean authorized;
	protected String userGroupsIds;
	protected String userGroupsNames="";
	protected String city = "";
	protected String password = "";
	protected String oldPassword = "";
	protected String lastLogon = "";
	protected Date lastLogonAsDate = new Date();
	protected String title = "";
	protected String firstName = "";
	protected String lastName = "";
	protected String fieldA = "";
	protected String fieldB = "";
	protected String fieldC = "";
	protected String fieldD = "";
	protected String fieldE = "";
	protected long regDate;
	protected String dateOfBirth;
	protected String photo = "";
	protected String signature = "";
	protected boolean sexMale = true;
	protected int forumRank;
	protected int ratingRank;
	protected String editableGroups;
	protected String editablePages;
	protected String writableFolders;
	protected transient UserGroupVerify userGroupVerify = null;
   // datumy kedy je platne prihlasenie
	protected String allowLoginStart;
	protected String allowLoginEnd;
	protected boolean allowDateLogin = true;
	protected String fax = "";
	protected String deliveryFirstName = "";
	protected String deliveryLastName = "";
	protected String deliveryCompany = "";
	protected String deliveryAdress = "";
	protected String deliveryCity = "";
	protected String deliveryPsc = "";
	protected String deliveryCountry = "";
	protected String deliveryPhone = "";
	protected String position="";
	protected int parentId=0;
	protected String salt;
	protected String mobileDevice;

	protected transient Map<String, SettingsBean> settings = null;
	protected transient Map<String, SettingsAdminBean> adminSettings = null;

	//toto je specialita pre VUB kedy sa merguju nastavenia s default userom a chceme zamedzit zbytocnemu ukladaniu do DB
	//viz IawsDB
	protected boolean settingsDontSave = false;

	/**
     * Prazdny konstruktor
    */
	public UserDetails()
	{

	}

	public UserDetails(ResultSet rs) throws Exception
	{
		UsersDB.fillUserDetails(this, rs);
	}

	/**
     * Konstruktor z HTTP requestu
    */
	public UserDetails(HttpServletRequest request)
	{
		userId = Tools.getIntValue(request.getParameter("userId"), -1);
		login = request.getParameter("login");

		company = request.getParameter("company");
		if (request.getParameter("adress")!=null)
		{
			adress = request.getParameter("adress");
		}
		else
		{
			adress = request.getParameter("address");
		}
		email = request.getParameter("email");
		country = request.getParameter("country");
		phone = request.getParameter("phone");
		if (request.getParameter("PSC")!=null)
		{
			zipCode = request.getParameter("PSC");
		}
		else
		{
			zipCode = request.getParameter("psc");
		}
		city = request.getParameter("city");
		password = request.getParameter("password");

		title = request.getParameter("title");
		firstName = request.getParameter("firstName");
		lastName = request.getParameter("lastName");

		fieldA = request.getParameter("fieldA");
		fieldB = request.getParameter("fieldB");
		fieldC = request.getParameter("fieldC");
		fieldD = request.getParameter("fieldD");
		fieldE = request.getParameter("fieldE");

		dateOfBirth = request.getParameter("dateOfBirth");
		photo = request.getParameter("photo");
		signature = request.getParameter("signature");

		fax = request.getParameter("fax");
		deliveryFirstName = request.getParameter("deliveryFirstName");
		deliveryLastName = request.getParameter("deliveryLastName");
		deliveryCompany = request.getParameter("deliveryCompany");
		deliveryAdress = request.getParameter("deliveryAdress");
		deliveryCity = request.getParameter("deliveryCity");
		deliveryPsc = request.getParameter("deliveryPsc");
		deliveryCountry = request.getParameter("deliveryCountry");
		deliveryPhone = request.getParameter("deliveryPhone");

		position = request.getParameter("position");
		parentId = Tools.getIntValue(request.getParameter("parentId"), 0);

	}

	/**
	 * Vrati true ak je pouzivatel v zadanej skupine
	 * @param userGroupId
	 * @return
	 */
	public boolean isInUserGroup(int userGroupId)
	{
		String test = ","+getUserGroupsIds()+",";
		if (test.indexOf(","+userGroupId+",")!=-1)
		{
			return(true);
		}
		return(false);
	}

	/**
	 * Vrati true ak je pouzivatel v zadanej skupine
	 * @param userGroupName
	 * @return
	 */
	public boolean isInUserGroup(String userGroupName)
	{
		if ((","+getUserGroupsNames()+",").indexOf(","+userGroupName+",")!=-1) return true;
		return false;
	}

	/**
	 * Vrati custom nastavenia pouzivatela (z tabulky user_settings)
	 * @return
	 */
    @JsonIgnore
	public Map<String, SettingsBean> getSettings()
	{
		if (settings == null) settings = UsersDB.getSettings(userId);
		return settings;
	}
    @JsonIgnore
	public Map<String, SettingsBean> getSettingsNotLoad()
	{
		if (settings == null) return new TreeMap<>();
		return settings;
	}

	public void clearSettings()
	{
		settings = null;
	}

	/**
	 * Vrati custom nastavenia admin casti pouzivatela (z tabulky user_settings_admin) v mape, kde key je nazov nastavenia a value je objekt admin nastavenia SettingsAdminBean
	 */
	@JsonIgnore
	public Map<String, SettingsAdminBean> getAdminSettings()
	{
		if (adminSettings == null)
			adminSettings = SettingsAdminDB.getSettings(userId);

		return adminSettings;
	}

	/**
	 *  Gets the userId attribute of the UserDetails object
	 *
	 *@return    The userId value
	 */
	public int getUserId()
	{
		return userId;
	}

	/**
	 *  Sets the userId attribute of the UserDetails object
	 *
	 *@param  userId  The new userId value
	 */
	public void setUserId(int userId)
	{
		this.userId = userId;
	}

	/**
	 *  Gets the fullName attribute of the UserDetails object
	 *
	 *@return    The fullName value
	 */
	@JsonIgnore
	public String getFullName()
	{
		String ret = "";

		if (Constants.getBoolean("fullNameIncludeTitle") && title != null && title.length()>0)
		{
			ret = title + " ";
		}
		if (firstName != null && firstName.length()>0)
		{
			ret += firstName + " ";
		}
		if (lastName != null && lastName.length()>0)
		{
			ret += lastName + " ";
		}

		return(ret.trim());
	}

	/**
	 *  Gets the deliveryFullName attribute of the UserDetails object
	 *
	 *@return    The fullName value
	 */
	@JsonIgnore
	public String getDeliveryFullName()
	{
		String ret = "";

		if (deliveryFirstName != null && deliveryFirstName.length()>0)
		{
			ret += deliveryFirstName + " ";
		}
		if (deliveryLastName != null && deliveryLastName.length()>0)
		{
			ret += deliveryLastName;
		}

		return(ret.trim());
	}

	/**
	 *  Sets the login attribute of the UserDetails object
	 *
	 *@param  login  The new login value
	 */
	public void setLogin(String login)
	{
		this.login = login;
	}

	/**
	 *  Gets the login attribute of the UserDetails object
	 *
	 *@return    The login value
	 */
	public String getLogin()
	{
		return login;
	}

	/**
	 *  Sets the admin attribute of the UserDetails object
	 *
	 *@param  admin  The new admin value
	 */
	public void setAdmin(boolean admin)
	{
		this.admin = admin;
	}

	/**
	 *  Gets the admin attribute of the UserDetails object
	 *
	 *@return    The admin value
	 */
	public boolean isAdmin()
	{
		return admin;
	}

	/**
	 *  Gets the adminString attribute of the UserDetails object
	 *
	 *@return    The adminString value
	 */
	@JsonIgnore
	public String getAdminString()
	{
		String ret = "Admin=";
		if (admin)
		{
			ret = ret + "<b>Áno</b>";
		}
		else
		{
			ret = ret + "Nie";
		}
		return (ret);
	}

	/**
	 *  Sets the company attribute of the UserDetails object
	 *
	 *@param  company  The new company value
	 */
	public void setCompany(String company)
	{
		this.company = company;
	}

	/**
	 *  Gets the company attribute of the UserDetails object
	 *
	 *@return    The company value
	 */
	public String getCompany()
	{
		return company;
	}

	/**
	 *  Sets the adress attribute of the UserDetails object
	 *
	 *@param  adress  The new adress value
	 */
	public void setAdress(String adress)
	{
		this.adress = adress;
	}

	/**
	 *  Gets the adress attribute of the UserDetails object
	 *
	 *@return    The adress value
	 */
	public String getAdress()
	{
		return adress;
	}

	/**
	 *  Sets the email attribute of the UserDetails object
	 *
	 *@param  email  The new email value
	 */
	public void setEmail(String email)
	{
		if (email != null) email = email.toLowerCase();
		this.email = email;
	}

	/**
	 *  Gets the email attribute of the UserDetails object
	 *
	 *@return    The email value
	 */
	public String getEmail()
	{
		return email;
	}

	/**
	 *  Sets the pSC attribute of the UserDetails object
	 *
	 *@param  PSC  The new pSC value
	 */
	public void setPSC(String PSC)
	{
		this.zipCode = PSC;
	}

	/**
	 *  Gets the pSC attribute of the UserDetails object
	 *
	 *@return    The pSC value
	 */
	public String getPSC()
	{
		return zipCode;
	}

	/**
	 *  Sets the country attribute of the UserDetails object
	 *
	 *@param  country  The new country value
	 */
	public void setCountry(String country)
	{
		this.country = country;
	}

	/**
	 *  Gets the country attribute of the UserDetails object
	 *
	 *@return    The country value
	 */
	public String getCountry()
	{
		return country;
	}

	/**
	 *  Sets the phone attribute of the UserDetails object
	 *
	 *@param  phone  The new phone value
	 */
	public void setPhone(String phone)
	{
		this.phone = phone;
	}

	/**
	 *  Gets the phone attribute of the UserDetails object
	 *
	 *@return    The phone value
	 */
	public String getPhone()
	{
		return phone;
	}

	/**
	 *  Sets the authorized attribute of the UserDetails object
	 *
	 *@param  authorized  The new authorized value
	 */
	public void setAuthorized(boolean authorized)
	{
		this.authorized = authorized;
	}

	/**
	 *  Gets the authorized attribute of the UserDetails object
	 *
	 *@return    The authorized value
	 */
	public boolean isAuthorized()
	{
		return authorized;
	}
	public void setUserGroupsIds(String ugi)
	{
		if (ugi==null) this.userGroupsIds="";
		else this.userGroupsIds = ugi;
	}
	public String getUserGroupsIds()
	{
		if (userGroupsIds==null) return "";
		return userGroupsIds;
	}
	public void setUserGroupsNames(String userGroupsNames)
	{
		this.userGroupsNames = userGroupsNames;
	}
	public String getUserGroupsNames()
	{
		return userGroupsNames;
	}
	public void setCity(String city)
	{
		this.city = city;
	}
	public String getCity()
	{
		return city;
	}
	@JsonIgnore
	public String getPassword()
	{
		return password;
	}
	public void setPassword(String password)
	{
		this.password = password;
	}

	/**
	 * Set password
	 * @param password
	 * @deprecated - user directly setPassword, it will be hashed in UsersDB.saveUser
	 */
	@Deprecated
	public void setPasswordPlain(String password)
	{
		this.password = password;
	}

	public void setLastLogon(String lastLogon)
	{
		this.lastLogon = lastLogon;
	}
	public void setLastLogonAsDate(Date lastLogonAsDate)
	{
		this.lastLogonAsDate = lastLogonAsDate == null ? null : (Date) lastLogonAsDate.clone();
	}
	public String getLastLogon()
	{
		return lastLogon;
	}
	public Date getLastLogonAsDate()
	{
		return lastLogonAsDate == null ? null : (Date) lastLogonAsDate.clone();
	}
	public String getTitle()
	{
		return title;
	}
	public void setTitle(String title)
	{
		this.title = title;
	}
	public String getFirstName()
	{
		return firstName;
	}
	public void setFirstName(String firstName)
	{
		this.firstName = firstName;
	}
	public String getLastName()
	{
		return lastName;
	}
	public void setLastName(String lastName)
	{
		this.lastName = lastName;
	}

	public void splitFullName(String fullName)
	{
		if (fullName == null)
		{
			title = "";
			firstName = "";
			lastName = "";
			return;
		}
		StringTokenizer st = new StringTokenizer(fullName);
		if (st.countTokens()<2)
		{
			this.setTitle("");
			this.setFirstName("");
			this.setLastName(fullName);
		}
		else if (st.countTokens()==2)
		{
			this.setTitle("");
			this.setFirstName(st.nextToken());
			this.setLastName(st.nextToken());
		}
		else if (st.countTokens()>2)
		{
			this.setTitle(st.nextToken());
			this.setFirstName(st.nextToken());
			this.setLastName(st.nextToken());
			while (st.hasMoreTokens())
			{
				this.setLastName(this.getLastName() + " " + st.nextToken());
			}
		}
	}


	public String getFieldA()
	{
		return fieldA;
	}
	public void setFieldA(String fieldA)
	{
		this.fieldA = fieldA;
	}
	public String getFieldB()
	{
		return fieldB;
	}
	public void setFieldB(String fieldB)
	{
		this.fieldB = fieldB;
	}
	public String getFieldC()
	{
		return fieldC;
	}
	public void setFieldC(String fieldC)
	{
		this.fieldC = fieldC;
	}
	public String getFieldD()
	{
		return fieldD;
	}
	public void setFieldD(String fieldD)
	{
		this.fieldD = fieldD;
	}
	public String getFieldE()
	{
		return fieldE;
	}
	public void setFieldE(String fieldE)
	{
		this.fieldE = fieldE;
	}



	public long getRegDate()
	{
		return regDate;
	}

	public void setRegDate(long regDate)
	{
		this.regDate = regDate;
	}



	public String getEmailAddress()
	{
		return email;
	}

	public void setEmailAddress(String emailAddress)
	{
		this.email = emailAddress;
	}
	/**
	 * @return Returns the dateOfBirth.
	 */
	public String getDateOfBirth()
	{
		return dateOfBirth;
	}
	/**
	 * @param dateOfBirth The dateOfBirth to set.
	 */
	public void setDateOfBirth(String dateOfBirth)
	{
		this.dateOfBirth = dateOfBirth;
	}
	/**
	 * @return Returns the photo.
	 */
	public String getPhoto()
	{
		return photo;
	}
    @JsonIgnore
	public String getPhotoSmall()
	{
		return GalleryToolsForCore.getImagePathSmall(photo);
	}

	@JsonIgnore
	public String getPhotoNormal()
	{
		return GalleryToolsForCore.getImagePathNormal(photo);
	}

    @JsonIgnore
	public String getPhotoOriginal()
	{
		return GalleryToolsForCore.getImagePathOriginal(photo);
	}
	/**
	 * @param photo The photo to set.
	 */
	public void setPhoto(String photo)
	{
		this.photo = photo;
	}

	/**
	 * @return Returns the sexMale.
	 */
	public boolean isSexMale()
	{
		return sexMale;
	}
	/**
	 * @param sexMale The sexMale to set.
	 */
	public void setSexMale(boolean sexMale)
	{
		this.sexMale = sexMale;
	}
	/**
	 * @return Returns the signature.
	 */
	public String getSignature()
	{
		return signature;
	}
	/**
	 * Vrati signaturu ako HTML kod s replace znackami ([img]...)
	 * @return
	 */
    @JsonIgnore
	public String getSignatureReplaced()
	{
		return ForumTools.replaceSignatureCodes(this);
	}
	/**
	 * @param signature The signature to set.
	 */
	public void setSignature(String signature)
	{
		this.signature = signature;
	}

	public String getRegDateString()
	{
		String ret = "";
		if	(this.regDate > 0)
			ret = Tools.formatDate(this.regDate);

		return ret;
	}
	/**
	 * @return Returns the forumRank.
	 */
    @JsonIgnore
	public int getForumRank()
	{
		return forumRank;
	}
	/**
	 * @param forumRank The forumRank to set.
	 */
	public void setForumRank(int forumRank)
	{
		this.forumRank = forumRank;
	}
	/**
	 * @return Returns the ratingRank.
	 */
    @JsonIgnore
	public int getRatingRank()
	{
		return ratingRank;
	}
	/**
	 * @param ratingRank The ratingRank to set.
	 */
	public void setRatingRank(int ratingRank)
	{
		this.ratingRank = ratingRank;
	}
	/**
	 * @return Returns the editableGroups.
	 */
	public String getEditableGroups()
	{
		return editableGroups;
	}

	/**
	 * Returns editable groups for user, if empty, for multiweb it automatically add root groups for current domain
	 * @param addMultiwebCheck
	 * @return
	 */
	@JsonIgnore
	public String getEditableGroups(boolean addMultiwebCheck) {
		if (addMultiwebCheck && InitServlet.isTypeCloud() && Tools.isEmpty(getEditableGroups())) {
			return CloudToolsForCore.getRootGroupIds();
		}
		return editableGroups;
	}

	/**
	 * @param editableGroups The editableGroups to set.
	 */
	public void setEditableGroups(String editableGroups)
	{
		this.editableGroups = editableGroups;
	}
	/**
	 * @return Returns the editablePages.
	 */
	public String getEditablePages()
	{
		return editablePages;
	}
	/**
	 * @param editablePages The editablePages to set.
	 */
	public void setEditablePages(String editablePages)
	{
		this.editablePages = editablePages;
	}
	/**
	 * @return Returns the writableFolders.
	 */
    @JsonIgnore
	public String getWritableFolders()
	{
		if (writableFolders==null) return null;
		return writableFolders.replace(' ', '\n');
	}

	public List<IwcmFile> getWritableFoldersList()
	{
		List<IwcmFile> writableFoldersList = new ArrayList<>();
		String writableFoldersString = getWritableFolders().trim();
		for (String folder : writableFoldersString.split("\n")) {
			folder = folder.trim();
			if (Tools.isEmpty(folder)) {
				continue;
			}
			writableFoldersList.add(IwcmFile.fromVirtualPath(folder));
		}
		return writableFoldersList;
	}

	/**
	 * @param writableFolders The writableFolders to set.
	 */
	public void setWritableFolders(String writableFolders)
	{
		if (writableFolders!=null) this.writableFolders = writableFolders.replace(' ', '\n');
		else this.writableFolders = null;
	}

	public String getAllowLoginEnd()
	{
		return allowLoginEnd;
	}

	public void setAllowLoginEnd(String allowLoginEnd)
	{
		this.allowLoginEnd = allowLoginEnd;
	}

	public String getAllowLoginStart()
	{
		return allowLoginStart;
	}

	public void setAllowLoginStart(String allowLoginStart)
	{
		this.allowLoginStart = allowLoginStart;
	}

	/**
	 * Pozor musi byt naplnene, nedetekuje samo!!!
	 * @return
	 */
	public boolean isAllowDateLogin()
	{
		return allowDateLogin;
	}

	public void setAllowDateLogin(boolean allowDateLogin)
	{
		this.allowDateLogin = allowDateLogin;
	}

	public void addToGroup(int groupId)
	{
		String ret = getUserGroupsIds();
		if(!isInUserGroup(groupId)) {
			ret += ","+groupId;
		}
		if(ret.startsWith(",")) ret = ret.substring(1);
		setUserGroupsIds(ret);
	}
	public void removeFromGroup(int groupId)
	{
		if(isInUserGroup(groupId))
		{
			String res="";
			StringTokenizer st = new StringTokenizer(getUserGroupsIds(),",");
			StringBuilder resBuf = new StringBuilder(res);
			while(st.hasMoreTokens())
			{
				String group = st.nextToken();
				if(!group.equals(String.valueOf(groupId))) resBuf.append(group).append(',');
			}
			res = resBuf.toString();
			if(res.startsWith(",")) res = res.substring(1);
			if(res.endsWith(",")) res = res.substring(0,res.length()-1);
			if(Tools.isEmpty(res))
			{
				setUserGroupsIds(null);
			}
			else
			{
				setUserGroupsIds(res);
			}
		}
	}

    @JsonIgnore
	public UserGroupVerify getUserGroupVerify() {
		if(userGroupVerify != null)
		{
			return userGroupVerify;
		}
		else
		{
			userGroupVerify = UsersDB.getUserGroupVerify(userId);
		}
		return userGroupVerify;
	}

	public String getDeliveryCity() {
		return deliveryCity;
	}

	public void setDeliveryCity(String deliveryCity) {
		this.deliveryCity = deliveryCity;
	}

	public String getDeliveryCompany() {
		return deliveryCompany;
	}

	public void setDeliveryCompany(String deliveryCompany) {
		this.deliveryCompany = deliveryCompany;
	}

	public String getDeliveryCountry() {
		return deliveryCountry;
	}

	public void setDeliveryCountry(String deliveryCountry) {
		this.deliveryCountry = deliveryCountry;
	}

	public String getDeliveryFirstName() {
		return deliveryFirstName;
	}

	public void setDeliveryFirstName(String deliveryFirstName) {
		this.deliveryFirstName = deliveryFirstName;
	}

	public String getDeliveryLastName() {
		return deliveryLastName;
	}

	public void setDeliveryLastName(String deliveryLastName) {
		this.deliveryLastName = deliveryLastName;
	}

	public String getDeliveryPhone() {
		return deliveryPhone;
	}

	public void setDeliveryPhone(String deliveryPhone) {
		this.deliveryPhone = deliveryPhone;
	}

	public String getDeliveryPsc() {
		return deliveryPsc;
	}

	public void setDeliveryPsc(String deliveryPsc) {
		this.deliveryPsc = deliveryPsc;
	}

	public String getDeliveryAdress() {
		return deliveryAdress;
	}

	public void setDeliveryAdress(String deliveryAdress) {
		this.deliveryAdress = deliveryAdress;
	}

	public String getFax() {
		if (Constants.getString("usersFaxList").startsWith("enumeration_")) {
			if (Tools.isNotEmpty(fax) && Tools.isNumeric(fax)) {
				String pos = fax.trim();
				String res = EnumerationDataDB.getString1(Tools.getIntValue(pos,-1));
				return res;
			}
		}
		return fax;
	}

	public void setFax(String fax) {
		this.fax = fax;
	}

	public void setFaxId(String faxId) {
		if (Tools.isNumeric(faxId) && Constants.getString("usersFaxList").startsWith("enumeration_"))
			this.fax = faxId;
	}

	public String getFaxId() {
		return fax;
	}

	public void setZip(String zip)
	{
		this.zipCode = zip;
	}

	public String getZip()
	{
		return this.zipCode;
	}

	public void setSettings(Map<String, SettingsBean> settings)
	{
		this.settings = settings;
	}

	public void setAdminSettings(Map<String, SettingsAdminBean> adminSettings)
	{
		this.adminSettings = adminSettings;
	}

	public boolean isSettingsDontSave()
	{
		return settingsDontSave;
	}

	public void setSettingsDontSave(boolean settingsDontSave)
	{
		this.settingsDontSave = settingsDontSave;
	}

	public String getPosition() {
	    if (Constants.getString("usersPositionList").startsWith("enumeration_")) {
            if (Tools.isNotEmpty(position) && Tools.isNumeric(position)) {
                String pos = position.trim();
                String res = EnumerationDataDB.getString1(Tools.getIntValue(pos,-1));
                return res;
            }
        }
        return position;
	}

	public String getPositionId() {
	    return position;
    }

    public void setPositionId(String positionId) {
		if (Tools.isNumeric(positionId) && Constants.getString("usersPositionList").startsWith("enumeration_"))
			this.position = positionId;
	}

	public void setPosition(String position) {
        if (Constants.getString("usersPositionList").startsWith("enumeration_")) {
            if (Tools.isNotEmpty(position) && Tools.isNumeric(position) == false) {
                String pos = position.trim();
                int enumerationId = Tools.getIntValue(Constants.getString("usersPositionList").substring(Constants.getString("usersPositionList").indexOf("_") + 1), 0);
                List<EnumerationDataBean> enumerationDataBeanList = EnumerationDataDB.getEnumerationDataBy(pos, enumerationId);
                if (enumerationDataBeanList != null && enumerationDataBeanList.size() > 0) {
                    this.position = String.valueOf(enumerationDataBeanList.get(0).getId());
                } else {
                    this.position = position;
                }
                return;
            }
        }
	    this.position = position;
	}

	public int getParentId()
	{
		return parentId;
	}

	public void setParentId(int parentId)
	{
		this.parentId = parentId;
	}

	@JsonIgnore
	public String getSalt()
	{
		return salt;
	}

	public void setSalt(String salt)
	{
		this.salt = salt;
	}

	@Override
	public String toString()
	{
		// RHR -> uprava na format JSON kvoli tomu ze sa vyuziva v inventory stripes
		StringBuilder toString = new StringBuilder();
		toString.append("{");
		toString.append(" \"UserDetails\" : {");
		toString.append("\"userId\" : \"" + getUserId() + "\",");
		toString.append("\"fax\" : \"" + getFax() + "\",");
		toString.append("\"faxId\" : \"" + getFaxId() + "\",");
		toString.append("\"fullName\" : \"" + getFullName() + "\",");
		toString.append("\"isAdmin\" : \"" + isAdmin() + "\"");
		toString.append("} ");
		toString.append("} ");
		return toString.toString();
	}

    @Override
    public boolean equals(Object obj) {
        return obj instanceof UserDetails && this.getUserId() == ((UserDetails) obj).getUserId();
    }

    /**
	 * Zisti, ci pouzivatel moze zapisovat subory do zadaneho adresara
	 * @param folder
	 * @return
	 */
	public boolean isFolderWritable(String folder)
	{
		return UsersDB.isFolderWritable(getWritableFolders(), folder);
	}

	@JsonIgnore
	public String getMobileDevice()
	{
		return mobileDevice;
	}

	public void setMobileDevice(String mobileDevice)
	{
		this.mobileDevice = mobileDevice;
	}

	public String getOldPassword() {
		return oldPassword;
	}

	public void setOldPassword(String oldPassword) {
		this.oldPassword = oldPassword;
	}
}
