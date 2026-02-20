package sk.iway.iwcm.editor;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.fasterxml.jackson.annotation.JsonIgnore;

import sk.iway.css.CssDto;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.LabelValueDetails;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.enumerations.EnumerationDataDB;
import sk.iway.iwcm.components.enumerations.model.EnumerationDataBean;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.PerexGroupBean;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.editor.rest.Field;
import sk.iway.iwcm.editor.rest.FieldValue;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.multidomain.MultiDomainFilter;
import sk.iway.iwcm.tags.support.ResponseUtils;

/**
 *  Formular ktory sa pouziva pri editore
 *
 *@Title        Interway Content Management
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.10 $
 *@created      $Date: 2004/02/16 16:44:26 $
 *@modified     $Date: 2004/02/16 16:44:26 $
 */
public class EditorForm implements Serializable
{
	private static final long serialVersionUID = -7904465994457304390L;

	/**
	 *  Description of the Field
	 */
	protected String title = "";
	/**
	 *  Description of the Field
	 */
	private int docId = -1;
	private int copyMediaFrom = -1;
	private String externalLink = "";
	private String data = "";
	private boolean recode;
	private String navbar = "";
	private String dateCreated = "";
	private String publishStart = "";
	private String publishStartTime = "";
	private String publishEnd = "";
	private String publishEndTime = "";
	private boolean searchable;
	private boolean available;
	private boolean cacheable;
	private boolean publicable;
	private int groupId = -1;
	private int tempId = -1;
	private int saveAsNew = 0;
	private String fileName = "";
	private String groupName = "";
	private int sortPriority = 10;
	private int headerDocId = -1;
	private int footerDocId = -1;
	private String publish = "";
	private int menuDocId = -1;
	private int authorId = -1;
	private int approveHistoryId = -1;
	private int[] passwordProtected;
	private String htmlHead = "";
	private String htmlData = "";

	// bolo stlacene edituj web stranku
	private int publicableToZero = -1;

	private String perexImage="";
	private String perexPlace="";
	private String[] perexGroup;
	private boolean showInMenu;
	private String eventDate = "";
	private String eventTime = "";
	private String virtualPath = "";
	private int rightMenuDocId = -1;

	private String fieldA = "";
	private String fieldB = "";
	private String fieldC = "";
	private String fieldD = "";
	private String fieldE = "";
	private String fieldF = "";

	private String fieldG = "";
	private String fieldH = "";
	private String fieldI = "";
	private String fieldJ = "";
	private String fieldK = "";
	private String fieldL = "";

	private boolean disableAfterEnd;

	private String fieldM = "";
	private String fieldN = "";
	private String fieldO = "";
	private String fieldP = "";
	private String fieldQ = "";
	private String fieldR = "";
	private String fieldS = "";
	private String fieldT = "";
	private String domainName;
	private boolean refreshCss;

	private int lastDocId;

	private boolean requireSsl;

	private String note;

	@JsonIgnore
	private GroupDetails group;
	@JsonIgnore
	private GroupDetails localSystemGroup;

	@JsonIgnore
	private TemplateDetails template;
	private String language;

	private int historyId;
	private List<Field> fields;
	private List<Map<String, Object>> templates;

	private String baseCssPath;
	private String tempCssPath;
	private List<Map<String, Object>> docAtrs;

	private List<Map<String, Object>> media;

	@JsonIgnore
	private String contextPath;

	private String cssStyle;

	@JsonIgnore
	private int syncId;

	public EditorForm() {
	}

	public EditorForm(DocDetails doc)
	{
		title = doc.getTitle();
		docId = doc.getDocId();
		externalLink = doc.getExternalLink();
		data = doc.getData();
		navbar = doc.getNavbar();
		dateCreated = doc.getDateCreatedString();
		publishStart = doc.getPublishStartString();
		publishStartTime = doc.getPublishStartTimeString();
		publishEnd = doc.getPublishEndString();
		publishEndTime = doc.getPublishEndTimeString();
		searchable = doc.isSearchable();
		available  = doc.isAvailable();
		cacheable = doc.isCacheable();
		publicable = doc.isPublicable();
		groupId = doc.getGroupId();
		tempId = doc.getTempId();
		sortPriority = doc.getSortPriority();
		headerDocId = doc.getHeaderDocId();
		footerDocId = doc.getFooterDocId();
		publish = "1";
		menuDocId = doc.getMenuDocId();
		rightMenuDocId = doc.getRightMenuDocId();
		authorId = doc.getAuthorId();
		//TODO: osetrit password protected
		setPasswordProtectedString(doc.getPasswordProtected());
		htmlHead = doc.getHtmlHead();
		htmlData = doc.getHtmlData();
		perexImage = doc.getPerexImage();
		perexPlace = doc.getPerexPlace();
		setPerexGroupString(doc.getPerexGroupIdsString());
		showInMenu = doc.isShowInMenu();
		eventDate = doc.getEventDateString();
		eventTime = doc.getEventTimeString();
		virtualPath = doc.getVirtualPath();

		fieldA = doc.getFieldA();
		fieldB = doc.getFieldB();
		fieldC = doc.getFieldC();
		fieldD = doc.getFieldD();
		fieldE = doc.getFieldE();
		fieldF = doc.getFieldF();
		fieldG = doc.getFieldG();
		fieldH = doc.getFieldH();
		fieldI = doc.getFieldI();
		fieldJ = doc.getFieldJ();
		fieldK = doc.getFieldK();
		fieldL = doc.getFieldL();

		disableAfterEnd = doc.isDisableAfterEnd();

		fieldM = doc.getFieldM();
		fieldN = doc.getFieldN();
		fieldO = doc.getFieldO();
		fieldP = doc.getFieldP();
		fieldQ = doc.getFieldQ();
		fieldR = doc.getFieldR();
		fieldS = doc.getFieldS();
		fieldT = doc.getFieldT();

		requireSsl = doc.isRequireSsl();

		syncId = doc.getSyncId();
	}

	public DocDetails toDocDetails()
	{
		DocDetails doc = new DocDetails();

		doc.setTitle(title);
		doc.setDocId(docId);
		doc.setExternalLink(externalLink);
		doc.setData(data);
		doc.setNavbar(navbar);
		//doc.setDateCreatedString(dateCreated);
		doc.setPublishStartString(publishStart);
		doc.setPublishStartTimeString(publishStartTime);
		if (Tools.isNotEmpty(publishStart)) doc.setPublishStart(DB.getTimestamp(publishStart, publishStartTime));
		doc.setPublishEndString(publishEnd);
		doc.setPublishEndTimeString(publishEndTime);
		if (Tools.isNotEmpty(publishEnd)) doc.setPublishEnd(DB.getTimestamp(publishEnd, publishEndTime));
		doc.setSearchable(searchable);
		doc.setAvailable(available);
		doc.setCacheable(cacheable);
		doc.setPublicable(publicable);
		doc.setGroupId(groupId);
		doc.setTempId(tempId);
		doc.setSortPriority(sortPriority);
		doc.setHeaderDocId(headerDocId);
		doc.setFooterDocId(footerDocId);
		doc.setMenuDocId(menuDocId);
		doc.setRightMenuDocId(rightMenuDocId);
		doc.setAuthorId(authorId);
		//TODO: osetrit password protected
		doc.setPasswordProtected(getPasswordProtectedString());
		doc.setHtmlHead(htmlHead);
		doc.setHtmlData(htmlData);
		doc.setPerexImage(perexImage);
		doc.setPerexPlace(perexPlace);
		doc.setPerexGroupString(getPerexGroupString());
		doc.setShowInMenu(showInMenu);
		doc.setEventDateString(eventDate);
		doc.setEventTimeString(eventTime);
		if (Tools.isNotEmpty(eventDate)) doc.setEventDate(DB.getTimestamp(eventDate, eventTime));
		doc.setVirtualPath(virtualPath);

		doc.setFieldA(fieldA);
		doc.setFieldB(fieldB);
		doc.setFieldC(fieldC);
		doc.setFieldD(fieldD);
		doc.setFieldE(fieldE);
		doc.setFieldF(fieldF);
		doc.setFieldG(fieldG);
		doc.setFieldH(fieldH);
		doc.setFieldI(fieldI);
		doc.setFieldJ(fieldJ);
		doc.setFieldK(fieldK);
		doc.setFieldL(fieldL);

		doc.setDisableAfterEnd(disableAfterEnd);

		doc.setFieldM(fieldM);
		doc.setFieldN(fieldN);
		doc.setFieldO(fieldO);
		doc.setFieldP(fieldP);
		doc.setFieldQ(fieldQ);
		doc.setFieldR(fieldR);
		doc.setFieldS(fieldS);
		doc.setFieldT(fieldT);

		doc.setRequireSsl(requireSsl);

		doc.setVirtualPath(getVirtualPath());

		doc.setSyncId(syncId);

		return doc;
	}

	private GroupDetails getLocalSystemGroup() {
		if (localSystemGroup == null && Constants.getBoolean("templatesUseDomainLocalSystemFolder")) {
			localSystemGroup = GroupsDB.getInstance().getLocalSystemGroup();
		}

		return localSystemGroup;
	}

	private TemplateDetails getTemplate() {
		if (template == null) {
			template = TemplatesDB.getInstance().getTemplate(getTempId());
		}

		return template;
	}

	private String getLanguage() {
		if (language == null) {
			TemplateDetails temp = getTemplate();
			language = temp != null ? temp.getLng() : "sk";
		}

		return language;
	}



	public GroupDetails getGroup() {
		return GroupsDB.getInstance().getGroup(getGroupId());
	}

	public void setGroup(GroupDetails group) {
		this.group = group;
	}

	public void setLocalSystemGroup(GroupDetails localSystemGroup) {
		this.localSystemGroup = localSystemGroup;
	}

	public String getDomainName() {
		return domainName;
	}

	public void setDomainName(String domainName) {
		this.domainName = domainName;
	}

	/**
	 *  Description of the Method
	 *
	 *@param  input  Description of the Parameter
	 *@return        Description of the Return Value
	 */
	private String recode(String input)
	{
		if (input == null)
		{
			return ("");
		}
		//Logger.println(this,"Recoding: "+input);
		return (input.trim());
	}

	/**
	 *  Gets the title attribute of the EditorForm object
	 *
	 *@return    The title value
	 */
	public String getTitle()
	{
		return title;
	}

	/**
	 *  Sets the title attribute of the EditorForm object
	 *
	 *@param  newTitle  The new title value
	 */
	public void setTitle(String newTitle)
	{
		if (newTitle == null || newTitle.length() < 1)
		{
			title = "unknown";
		}
		else
		{
			title = recode(newTitle);
		}
	}

	/**
	 *  Sets the docId attribute of the EditorForm object
	 *
	 *@param  newDocId  The new docId value
	 */
	public void setDocId(int newDocId)
	{
		docId = newDocId;
	}

	/**
	 *  Gets the docId attribute of the EditorForm object
	 *
	 *@return    The docId value
	 */
	public int getDocId()
	{
		return docId;
	}

	/**
	 *  Sets the copyMediaFrom attribute of the EditorForm object
	 *
	 *@param  copyMediaFrom  The new value
	 */
	public void setCopyMediaFrom(int copyMediaFrom)
	{
		this.copyMediaFrom = copyMediaFrom;
	}

	/**
	 *  Gets the docId attribute of the EditorForm object
	 *
	 *@return    The docId value
	 */
	public int getCopyMediaFrom()
	{
		return copyMediaFrom;
	}

	/**
	 *  Sets the externalLink attribute of the EditorForm object
	 *
	 *@param  newExternalLink  The new externalLink value
	 */
	public void setExternalLink(String newExternalLink)
	{
		externalLink = recode(newExternalLink);
	}

	/**
	 *  Gets the externalLink attribute of the EditorForm object
	 *
	 *@return    The externalLink value
	 */
	public String getExternalLink()
	{
		return externalLink;
	}

	/**
	 *  Sets the data attribute of the EditorForm object
	 *
	 *@param  newData  The new data value
	 */
	public void setData(String newData)
	{
		data = recode(newData);
	}

	/**
	 *  Gets the data attribute of the EditorForm object
	 *
	 *@return    The data value
	 */
	@JsonIgnore
	public String getDataUntouched()
	{

		// owrapovanie  pre cloud

		if ("cloud".equals(Constants.getInstallName()))
		{
			if (getDocId() < 1 && (Tools.isEmpty(data) || "<p>&nbsp;</p>".equals(data)) && "Footer".equals(getTitle())==false)
			{
				data = "<section>"+
				 "<div class=\"container\">"+
				"<article>"+
				"<h1>"+getTitle()+"</h1>\n<p>Text</p>"+
				"</article>"+
				"</div>"+
				"</section>"+
				"<p>&nbsp;</p>"; // medzera pre pripad ze si uzivatel odstrani sekciu
			}
		}



		return data;
	}

	public String getData() {
		return recodeChars(getDataUntouched());
	}

	private String recodeChars(String docData){
		String result = docData;

		/* toto pre rest sluzbu uz nepotrebujeme
		result = Tools.replace(result, "<textarea", "|textarea");
		result = Tools.replace(result, "</textarea", "|/textarea");
		result = Tools.replace(result, "<TEXTAREA", "|textarea");
		result = Tools.replace(result, "</TEXTAREA", "|/textarea");
		result = Tools.replace(result, "<TextArea", "|textarea");
		result = Tools.replace(result, "</TextArea", "|/textarea");
		result = Tools.replace(result, "&lt;", "|lt;|");
		result = Tools.replace(result, "&LT;", "|LT;|");
		result = Tools.replace(result, "&gt;", "|gt;|");
		result = Tools.replace(result, "&GT;", "|GT;|");
		*/

		if (result .indexOf("!INCLUDE(")!=-1)
		{
			result  = Tools.replace(result , "<article>", "");
			result  = Tools.replace(result , "</article>", "");
		}

		return result;
	}

	/**
	 *  Sets the recode attribute of the EditorForm object
	 *
	 *@param  newRecode  The new recode value
	 */
	public void setRecode(boolean newRecode)
	{
		recode = newRecode;
	}

	/**
	 *  Gets the recode attribute of the EditorForm object
	 *
	 *@return    The recode value
	 */
	public boolean isRecode()
	{
		return recode;
	}

	/**
	 *  Sets the navbar attribute of the EditorForm object
	 *
	 *@param  newNavbar  The new navbar value
	 */
	public void setNavbar(String newNavbar)
	{
		navbar = recode(newNavbar);
	}

	/**
	 *  Gets the navbar attribute of the EditorForm object
	 *
	 *@return    The navbar value
	 */
	public String getNavbar()
	{
		if (navbar == null || navbar.length() < 1)
		{
			return (getTitle());
		}
		return navbar;
	}

	/**
	 *  Sets the dateCreated attribute of the EditorForm object
	 *
	 *@param  newDateCreated  The new dateCreated value
	 */
	public void setDateCreated(String newDateCreated)
	{
		dateCreated = newDateCreated;
	}

	/**
	 *  Gets the dateCreated attribute of the EditorForm object
	 *
	 *@return    The dateCreated value
	 */
	public String getDateCreated()
	{
		return dateCreated;
	}

	/**
	 *  Sets the publishStart attribute of the EditorForm object
	 *
	 *@param  newPublishStart  The new publishStart value
	 */
	public void setPublishStart(String newPublishStart)
	{
		publishStart = newPublishStart;
	}

	/**
	 *  Gets the publishStart attribute of the EditorForm object
	 *
	 *@return    The publishStart value
	 */
	public String getPublishStart()
	{
		if (Tools.isEmpty(publishStart) && Constants.getBoolean("editorAutoFillPublishStart"))
		{
			setPublishStart(Tools.formatDate(Tools.getNow()));
		}

		return publishStart;
	}

	/**
	 *
	 * @param newPublishStartTime
	 */

	public void setPublishStartTime(String newPublishStartTime)
	{
		publishStartTime = newPublishStartTime;
	}

	/**
	 *
	 * @return The publishStartTime Value
	 */

	 public String getPublishStartTime()
	 {
		 if (Tools.isEmpty(publishStartTime) && Tools.isNotEmpty(publishStart) && Constants.getBoolean("editorAutoFillPublishStart"))
		 {
			 publishStartTime = Tools.formatTime(Tools.getNow());
		 }

		 if (Tools.isEmpty(publishStartTime) && Tools.isNotEmpty(publishStart)) {
		 	if (Constants.getBoolean("editorAutoFillPublishStart")) {
				publishStartTime = Tools.formatTime(Tools.getNow());
			}
			else {
				publishStartTime = "6:00";
			}
		 }

		 return publishStartTime;
	 }
	/**
	 *  Sets the publishEnd attribute of the EditorForm object
	 *
	 *@param  newPublishEnd  The new publishEnd value
	 */
	public void setPublishEnd(String newPublishEnd)
	{
		publishEnd = newPublishEnd;
	}

	/**
	 *  Gets the publishEnd attribute of the EditorForm object
	 *
	 *@return    The publishEnd value
	 */
	public String getPublishEnd()
	{
		return publishEnd;
	}

	/**
	 *  Sets the groupId attribute of the EditorForm object
	 *
	 *@param  newGroupId  The new groupId value
	 */
	public void setGroupId(int newGroupId)
	{
		groupId = newGroupId;
	}

	/**
	 *  Gets the groupId attribute of the EditorForm object
	 *
	 *@return    The groupId value
	 */
	public int getGroupId()
	{
		return groupId;
	}

	/**
	 *  Sets the tempId attribute of the EditorForm object
	 *
	 *@param  newTempId  The new tempId value
	 */
	public void setTempId(int newTempId)
	{
		tempId = newTempId;
	}

	/**
	 *  Gets the tempId attribute of the EditorForm object
	 *
	 *@return    The tempId value
	 */
	public int getTempId()
	{
		return tempId;
	}

	/**
	 *  Sets the searchable attribute of the EditorForm object
	 *
	 *@param  newSearchable  The new searchable value
	 */
	public void setSearchable(boolean newSearchable)
	{
		searchable = newSearchable;
	}

	/**
	 *  Gets the searchable attribute of the EditorForm object
	 *
	 *@return    The searchable value
	 */
	public boolean isSearchable()
	{
		return searchable;
	}

	/**
	 *  Sets the available attribute of the EditorForm object
	 *
	 *@param  newAvailable  The new available value
	 */
	public void setAvailable(boolean newAvailable)
	{
		available = newAvailable;
	}

	/**
	 *  Gets the available attribute of the EditorForm object
	 *
	 *@return    The available value
	 */
	public boolean isAvailable()
	{
		return available;
	}

	/**
	 *  Sets the cacheable attribute of the EditorForm object
	 *
	 *@param  newCacheable  The new cacheable value
	 */
	public void setCacheable(boolean newCacheable)
	{
		cacheable = newCacheable;
	}

	/**
	 *  Gets the cacheable attribute of the EditorForm object
	 *
	 *@return    The cacheable value
	 */
	public boolean isCacheable()
	{
		return cacheable;
	}

	/**
	 *
	 * @param newPublicable
	 */

	public void setPublicable(boolean newPublicable)
	{
		publicable = newPublicable;
	}

	/**
	 *
	 * @return publicable
	 */
	public boolean isPublicable()
	{
		return publicable;
	}

	/**
	 *  Sets the saveAsNew attribute of the EditorForm object
	 *
	 *@param  newSaveAsNew  The new saveAsNew value
	 */
	public void setSaveAsNew(int newSaveAsNew)
	{
		saveAsNew = newSaveAsNew;
	}

	/**
	 *  Gets the saveAsNew attribute of the EditorForm object
	 *
	 *@return    The saveAsNew value
	 */
	public int getSaveAsNew()
	{
		return saveAsNew;
	}

	/**
	 *  Sets the fileName attribute of the EditorForm object
	 *
	 *@param  newFileName  The new fileName value
	 */
	public void setFileName(String newFileName)
	{
		fileName = newFileName;
	}

	/**
	 *  Gets the fileName attribute of the EditorForm object
	 *
	 *@return    The fileName value
	 */
	public String getFileName()
	{
		return fileName;
	}

	/**
	 *  Sets the groupName attribute of the EditorForm object
	 *
	 *@param  groupName  The new groupName value
	 */
	public void setGroupName(String groupName)
	{
		this.groupName = recode(groupName);
	}

	/**
	 *  Gets the groupName attribute of the EditorForm object
	 *
	 *@return    The groupName value
	 */
	public String getGroupName()
	{
		return groupName;
	}

	/**
	 *  Sets the groupIdString attribute of the EditorForm object
	 *
	 *@param  groupIdString  The new groupIdString value
	 */
	public void setGroupIdString(String groupIdString)
	{
		try
		{
			int id = Integer.parseInt(groupIdString.trim());
			groupId = id;
			//Logger.println(this,"groupIdString="+groupIdString+" id="+id);
		}
		catch (Exception ex)
		{

		}
	}

	/**
	 *  Gets the groupIdString attribute of the EditorForm object
	 *
	 *@return    The groupIdString value
	 */
	public String getGroupIdString()
	{
		return (Integer.toString(groupId));
	}

	/**
	 *  Sets the sortPriority attribute of the EditorForm object
	 *
	 *@param  sortPriority  The new sortPriority value
	 */
	public void setSortPriority(int sortPriority)
	{
		this.sortPriority = sortPriority;
	}

	/**
	 *  Gets the sortPriority attribute of the EditorForm object
	 *
	 *@return    The sortPriority value
	 */
	public int getSortPriority()
	{
		return sortPriority;
	}

	/**
	 *  Sets the headerDocId attribute of the EditorForm object
	 *
	 *@param  headerDocId  The new headerDocId value
	 */
	public void setHeaderDocId(int headerDocId)
	{
		this.headerDocId = headerDocId;
	}

	/**
	 *  Gets the headerDocId attribute of the EditorForm object
	 *
	 *@return    The headerDocId value
	 */
	public int getHeaderDocId()
	{
		return headerDocId;
	}

	/**
	 *  Sets the footerDocId attribute of the EditorForm object
	 *
	 *@param  footerDocId  The new footerDocId value
	 */
	public void setFooterDocId(int footerDocId)
	{
		this.footerDocId = footerDocId;
	}

	/**
	 *  Gets the footerDocId attribute of the EditorForm object
	 *
	 *@return    The footerDocId value
	 */
	public int getFooterDocId()
	{
		return footerDocId;
	}

	/**
	 *  Sets the publish attribute of the EditorForm object
	 *
	 *@param  publish  The new publish value
	 */
	public void setPublish(String publish)
	{
		this.publish = publish;
	}

	/**
	 *  Gets the publish attribute of the EditorForm object
	 *
	 *@return    The publish value
	 */
	public String getPublish()
	{
		return publish;
	}

	/**
	 *  Sets the menuDocId attribute of the EditorForm object
	 *
	 *@param  menuDocId  The new menuDocId value
	 */
	public void setMenuDocId(int menuDocId)
	{
		this.menuDocId = menuDocId;
	}

	/**
	 *  Gets the menuDocId attribute of the EditorForm object
	 *
	 *@return    The menuDocId value
	 */
	public int getMenuDocId()
	{
		return menuDocId;
	}

	/**
	 *  Sets the authorId attribute of the EditorForm object
	 *
	 *@param  authorId  The new authorId value
	 */
	public void setAuthorId(int authorId)
	{
		this.authorId = authorId;
	}

	/**
	 *  Gets the authorId attribute of the EditorForm object
	 *
	 *@return    The authorId value
	 */
	public int getAuthorId()
	{
		return authorId;
	}

	/**
	 *  Sets the approveHistoryId attribute of the EditorForm object
	 *
	 *@param  approveHistoryId  The new approveHistoryId value
	 */
	public void setApproveHistoryId(int approveHistoryId)
	{
		this.approveHistoryId = approveHistoryId;
	}

	/**
	 *  Gets the approveHistoryId attribute of the EditorForm object
	 *
	 *@return    The approveHistoryId value
	 */
	public int getApproveHistoryId()
	{
		return approveHistoryId;
	}

	/**
	 *  Sets the passwordProtected attribute of the EditorForm object
	 *
	 *@param  passwordProtected  The new passwordProtected value
	 */
	public void setPasswordProtected(int[] passwordProtected)
	{
		this.passwordProtected = passwordProtected;
	}

	/**
	 *  Sets the passwordProtectedString attribute of the EditorForm object
	 *
	 *@param  passwordProtectedString  The new passwordProtectedString value
	 */
	public void setPasswordProtectedString(String passwordProtectedString)
	{
		if (passwordProtectedString == null || passwordProtectedString.length() < 1)
		{
			passwordProtected = new int[0];
			return;
		}
		//najskor zisti pocet
		try
		{
			StringTokenizer st = new StringTokenizer(passwordProtectedString, ",");
			int len = st.countTokens();
			passwordProtected = new int[len];
			int i = 0;
			while (st.hasMoreTokens())
			{
				passwordProtected[i] = Integer.parseInt(st.nextToken());
				i++;
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
	}

	/**
	 *  Gets the passwordProtected attribute of the EditorForm object
	 *
	 *@return    The passwordProtected value
	 */
	public int[] getPasswordProtected()
	{
		return passwordProtected;
	}

	/**
	 *  Gets the passwordProtectedString attribute of the EditorForm object
	 *
	 *@return    The passwordProtectedString value
	 */
	public String getPasswordProtectedString()
	{
		if (passwordProtected == null)
		{
			return(null);
		}
		int size = passwordProtected.length;
		if (size == 0)
		{
			return (null);
		}
		String out = null;
		int i;
		for (i = 0; i < size; i++)
		{
			if (out == null)
			{
				out = Integer.toString(passwordProtected[i]);
			}
			else
			{
				out = out + "," + passwordProtected[i];
			}
		}
		return (out);
	}

	public void setHtmlHead(String htmlHead)
	{
		this.htmlHead = htmlHead;
	}

	/**
	 *  Gets the htmlHeader attribute of the EditorForm object
	 *
	 *@return    The htmlHeader value
	 */
	public String getHtmlHead()
	{
		return htmlHead;
	}

	/**
	 *  Sets the htmlData attribute of the EditorForm object
	 *
	 *@param  htmlData  The new htmlData value
	 */
	public void setHtmlData(String htmlData)
	{
		this.htmlData = htmlData;
	}

	/**
	 *  Gets the htmlData attribute of the EditorForm object
	 *
	 *@return    The htmlData value
	 */
	public String getHtmlData()
	{
		return htmlData;
	}

	public void setPublicableToZero(int newPublicableToZero)
	{
		 publicableToZero = newPublicableToZero;
	}

	public int getPublicableToZero()
	{
		 return publicableToZero;
	}
	public void setPublishEndTime(String publishEndTime)
	{
		this.publishEndTime = publishEndTime;
	}
	public String getPublishEndTime()
	{
		if (Tools.isEmpty(publishEndTime) && Tools.isNotEmpty(publishEnd)) return "23:59";
		return publishEndTime;
	}
	public void setPerexImage(String perexImage)
	{
		if (perexImage == null) perexImage = "";
		//pluskari tam veselo davali nieco ako /thumb/images/gallery/aktuality/domov/2009/03/perex/tatryvysokenebezpecenstvolavin.t.15.3.jpg?w=310&h=310&ip=6
		try
		{
			if (perexImage.startsWith("/thumb/"))
			{
				perexImage = perexImage.substring(6);
				int i = perexImage.indexOf('?');
				if (i>0) perexImage = perexImage.substring(0, i);
			}
			if (perexImage.startsWith("/images/"))
			{
				int i = perexImage.indexOf('?');
				if (i>0) perexImage = perexImage.substring(0, i);
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		this.perexImage = perexImage;
	}
	public String getPerexImage()
	{
		return perexImage;
	}
	public void setPerexPlace(String perexPlace)
	{
		this.perexPlace = perexPlace;
	}
	public String getPerexPlace()
	{
		return perexPlace;
	}
	public void setPerexGroup(String[] perexGroup)
	{
		this.perexGroup = perexGroup;
	}
	public String[] getPerexGroup()
	{
		return perexGroup;
	}

	public String getPerexGroupString()
	{
		String ret = null;
		try
		{
			if (perexGroup!=null)
			{
				int size = perexGroup.length;
				int i;
				for (i=0; i<size; i++)
				{
					if (ret==null) ret = "," + perexGroup[i];
					else ret += "," + perexGroup[i];
				}
				if (ret != null)
				{
					ret += ",";
				}
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		//Logger.println(this,"getPerexGroupString="+ret);
		return(ret);
	}

	public void setPerexGroupString(String perexGroupString)
	{
		if (perexGroupString == null)
		{
			perexGroupString = "";
		}
		StringTokenizer st = new StringTokenizer(perexGroupString, ",");
		try
		{
			perexGroup = new String[st.countTokens()];
			int i=0;
			while (st.hasMoreTokens())
			{
				int pGroupId = Tools.getIntValue(st.nextToken().trim(), -1);
				perexGroup[i++] = Integer.toString(pGroupId);
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
	}

	public boolean isShowInMenu()
	{
		return showInMenu;
	}
	public void setShowInMenu(boolean showInMenu)
	{
		this.showInMenu = showInMenu;
	}
	public void setEventDate(String eventDate)
	{
		this.eventDate = eventDate;
	}
	public String getEventDate()
	{
		return eventDate;
	}
	public void setEventTime(String eventTime)
	{
		this.eventTime = eventTime;
	}
	public String getEventTime()
	{
		return eventTime;
	}
	public String getVirtualPath()
	{
		if (virtualPath == null) virtualPath = "";
		return virtualPath;
	}
	public void setVirtualPath(String virtualPath)
	{
		this.virtualPath = virtualPath;
	}

	public int getRightMenuDocId()
	{
		return rightMenuDocId;
	}
	public void setRightMenuDocId(int rightMenuDocId)
	{
		this.rightMenuDocId = rightMenuDocId;
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
	public String getFieldF()
	{
		return fieldF;
	}
	public void setFieldF(String fieldF)
	{
		this.fieldF = fieldF;
	}
	/**
	 * @return Returns the fieldG.
	 */
	public String getFieldG()
	{
		return fieldG;
	}
	/**
	 * @param fieldG The fieldG to set.
	 */
	public void setFieldG(String fieldG)
	{
		this.fieldG = fieldG;
	}
	/**
	 * @return Returns the fieldH.
	 */
	public String getFieldH()
	{
		return fieldH;
	}
	/**
	 * @param fieldH The fieldH to set.
	 */
	public void setFieldH(String fieldH)
	{
		this.fieldH = fieldH;
	}
	/**
	 * @return Returns the fieldI.
	 */
	public String getFieldI()
	{
		return fieldI;
	}
	/**
	 * @param fieldI The fieldI to set.
	 */
	public void setFieldI(String fieldI)
	{
		this.fieldI = fieldI;
	}
	/**
	 * @return Returns the fieldJ.
	 */
	public String getFieldJ()
	{
		return fieldJ;
	}
	/**
	 * @param fieldJ The fieldJ to set.
	 */
	public void setFieldJ(String fieldJ)
	{
		this.fieldJ = fieldJ;
	}
	/**
	 * @return Returns the fieldK.
	 */
	public String getFieldK()
	{
		return fieldK;
	}
	/**
	 * @param fieldK The fieldK to set.
	 */
	public void setFieldK(String fieldK)
	{
		this.fieldK = fieldK;
	}
	/**
	 * @return Returns the fieldL.
	 */
	public String getFieldL()
	{
		return fieldL;
	}
	/**
	 * @param fieldL The fieldL to set.
	 */
	public void setFieldL(String fieldL)
	{
		this.fieldL = fieldL;
	}

	public boolean isDisableAfterEnd()
	{
		return disableAfterEnd;
	}

	public void setDisableAfterEnd(boolean disableAfterEnd)
	{
		this.disableAfterEnd = disableAfterEnd;
	}

	public String getFieldM()
	{
		return fieldM;
	}

	public void setFieldM(String fieldM)
	{
		this.fieldM = fieldM;
	}

	public String getFieldN()
	{
		return fieldN;
	}

	public void setFieldN(String fieldN)
	{
		this.fieldN = fieldN;
	}

	public String getFieldO()
	{
		return fieldO;
	}

	public void setFieldO(String fieldO)
	{
		this.fieldO = fieldO;
	}

	public String getFieldP()
	{
		return fieldP;
	}

	public void setFieldP(String fieldP)
	{
		this.fieldP = fieldP;
	}

	public String getFieldQ()
	{
		return fieldQ;
	}

	public void setFieldQ(String fieldQ)
	{
		this.fieldQ = fieldQ;
	}

	public String getFieldR()
	{
		return fieldR;
	}

	public void setFieldR(String fieldR)
	{
		this.fieldR = fieldR;
	}

	public String getFieldS()
	{
		return fieldS;
	}

	public void setFieldS(String fieldS)
	{
		this.fieldS = fieldS;
	}

	public String getFieldT()
	{
		return fieldT;
	}

	public void setFieldT(String fieldT)
	{
		this.fieldT = fieldT;
	}

	public boolean isRequireSsl()
	{
		return requireSsl;
	}

	public void setRequireSsl(boolean requireSsl)
	{
		this.requireSsl = requireSsl;
	}

	public int getLastDocId()
	{
		if (lastDocId < 1) return docId;
		return lastDocId;
	}

	public void setLastDocId(int lastDocId)
	{
		this.lastDocId = lastDocId;
	}

	public String getNote()
	{
		return ResponseUtils.filter(getNoteUntouched());
	}

	@JsonIgnore
	public String getNoteUntouched()
	{
		return note;
	}

	public void setNote(String note)
	{
		this.note = note;
	}

	public int getHistoryId() {
		return historyId;
	}

	public void setHistoryId(int historyId) {
		this.historyId = historyId;
	}

	public String getPathForTree() {
		int lastGroupId = 0;

		if(getDocId() != getLastDocId()) {
			lastGroupId = DocDB.getInstance().getBasicDocDetails(getLastDocId(), true).getGroupId();
		}
		else {
			lastGroupId = getGroupId();
		}

		return GroupsDB.getInstance().getParents(lastGroupId);
	}

	public boolean showInMenuHide() {
		return GroupsDB.getInstance().getGroup(getGroupId()).getMenuType() != GroupDetails.MENU_TYPE_NORMAL;
	}

	public List<Map<String, Object>> getDocAtrs() {
		return docAtrs;
	}

	public void setDocAtrs(List<Map<String, Object>> docAtrs) {
		this.docAtrs = docAtrs;
	}

	public List<Map<String, Object>> getMedia() {
		return media;
	}

	public void setMedia(List<Map<String, Object>> media) {
		this.media = media;
	}

	public boolean isRefreshCss() {
		return refreshCss;
	}

	public void setRefreshCss(boolean refreshCss) {
		this.refreshCss = refreshCss;
	}

	public String getContextPath() {
		return contextPath;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public List<Field> getFields() {
		//tu musi byt getInstance aby sa prebral jazyk podla prihlaseneho usera
		Prop prop = Prop.getInstance();
		if (fields == null) {
			fields = new ArrayList<>();
			Method method;
			for (char alphabet = 'A'; alphabet <= 'T'; alphabet++) {

				try {
					Field field = new Field();
					method = this.getClass().getMethod("getField" + alphabet);

					String labelKey = "editor.field_" + Character.toLowerCase(alphabet);
					String label = prop.getText(labelKey);

					String typeKey = labelKey + ".type";
					String type = prop.getText(typeKey);

					FieldType fieldType = FieldType.asFieldType(type);
					List<FieldValue> fieldValues = new ArrayList<>();

					if (!type.equals(typeKey)) {
						if (type.contains("|")) {
							String values = type.substring(type.indexOf(":") + 1);
							for (String value : Tools.getTokens(values, "|")) {
								fieldValues.add(new FieldValue(value, value));
							}
						}

						if (type.startsWith("docsIn_")) {
							//JICH - add
							boolean isNull = false;
							if (type.endsWith("_null")) {
								isNull = true;
								type = type.replace("_null", "");
							}
							//JICH - add end
							String groupId = type.substring(type.indexOf("_") + 1);
							int groupIdInt = Tools.getIntValue(groupId, 0);
							if (groupIdInt > 0) {
								GroupDetails group = GroupsDB.getInstance().getGroup(groupIdInt);
								List<DocDetails> listOfDocs = DocDB.getInstance().getDocByGroup(groupIdInt);
								if (listOfDocs != null) {
									//JICH - add
									if (isNull) {
										fieldValues.add(new FieldValue("", ""));
									}
									//JICH - add end
									for (DocDetails d : listOfDocs) {
										if (group != null && group.getDefaultDocId() != d.getDocId()) {
											fieldValues.add(new FieldValue(d.getTitle(), d.getDocId()));
										}
									}
								}
							}
						}

						if (type.startsWith("enumeration_")) {
							boolean isNull = false;
							if (type.endsWith("_null")) {
								isNull = true;
								type = type.replace("_null", "");
							}

							int enumerationId = Tools.getIntValue(type.substring(type.indexOf("_") + 1), 0);
							if (enumerationId > 0) {
								List<EnumerationDataBean> enumerationDataList = EnumerationDataDB.getEnumerationDataByType(enumerationId);
								if (enumerationDataList != null) {
									if (isNull) {
										fieldValues.add(new FieldValue("", ""));
									}
									for (EnumerationDataBean enumData : enumerationDataList) {
										fieldValues.add(new FieldValue(enumData.getString1(), enumData.getString1()));
									}
								}
							}
						}

						//JICH - add
						if (type.startsWith("custom-dialog")) {
							//System.out.println(type);
							String[] typeArray = type.split(",");
							String dialogScript = "";
							String displayScript = "";
							if (typeArray.length > 1) dialogScript = typeArray[1];
							if (typeArray.length > 2) displayScript = typeArray[2];

							fieldValues.add(new FieldValue(dialogScript, displayScript));
						}
						//JICH - add end
						else if (type.startsWith("file_archiv_link_insert_new")) {
							// PRA: Zobrazi link na vyber suboru z archivu
							FieldValue val = new FieldValue();
							val.setType("file_archiv_link_insert_new");
							fieldValues.add(val);
						}
					}

					if (fieldType == null) {
						fieldType = FieldType.TEXT;
					}

					String value = (String) method.invoke(this);

                    //	TAN: textovym retazcom je mozne zadat maximalnu dlzku znakov v inpute alebo validaciu s odporucanym maximalnym poctom znakov inputu
                    // priklad zadania textoveho kluca: text-120, warningLength-80
					int maxlength = 255;
					int warninglength = 0;
					try
                    {
                        if (type.startsWith("text-")) {

                            if (type.contains(",")) {
                                String[] typeArray = type.split(",");
                                String maxlengthstring = typeArray[0];
                                String warninglengthstring = typeArray[1];

                                maxlengthstring.substring(maxlengthstring.lastIndexOf("-") + 1);
                                warninglengthstring.substring(warninglengthstring.lastIndexOf("-") + 1);

                                maxlength = Integer.parseInt(maxlengthstring.replaceAll("[^0-9]", ""));
                                warninglength = Integer.parseInt(warninglengthstring.replaceAll("[^0-9]", ""));
                            } else {
                                int pomlcka = type.indexOf("-");
                                if (pomlcka > 0) maxlength = Tools.getIntValue(type.substring(pomlcka + 1), 255);
                            }
                        }
                    }
					catch (Exception ex)
                    {
                        sk.iway.iwcm.Logger.error(ex);
                    }

					field.setKey(Character.toLowerCase(alphabet) + "");
					field.setLabel(label);
					field.setValue(value);
					field.setType(fieldType.name().toLowerCase());
					field.setMaxlength(maxlength);
					field.setWarninglength(warninglength);
                    if (warninglength>0) {
						field.setWarningMessage( prop.getText("editor.field_" + Character.toUpperCase(alphabet)+".warningText", String.valueOf(warninglength)));
					}
					if (fieldType != FieldType.TEXT) {
						field.setTypeValues(fieldValues);
					}

					fields.add(field);
				} catch (NoSuchMethodException e) {
					sk.iway.iwcm.Logger.error(e);
				} catch (IllegalAccessException e) {
					sk.iway.iwcm.Logger.error(e);
				} catch (InvocationTargetException e) {
					sk.iway.iwcm.Logger.error(e);
				}
			}
		}

		return fields;
	}

	public List<Map<String, Object>> getTemplates() {
		if (templates == null) {
			templates = new ArrayList<>();
			List<TemplateDetails> templatesBeans = TemplatesDB.filterDeviceTemplates(TemplatesDB.getInstance().getTemplates(getGroupId(), getTempId()));;

			for (TemplateDetails templateDetails : templatesBeans) {
				Map<String, Object> map = new HashMap<>();
				map.put("id", templateDetails.getTempId());
				map.put("name", templateDetails.getTempName());
				templates.add(map);
			}
		}

		return templates;
	}

	public List<String> getDocList() {

		if (getLocalSystemGroup() != null)
		{
			List<DocDetails> localDocsInGroup = DocDB.getInstance().getBasicDocDetailsByGroup(getLocalSystemGroup().getGroupId(), -1);
			return getDocDetailItems(addFromTemlateDoc(localDocsInGroup));
		}

		return getDocDetailItems(addFromTemlateDoc(DocDB.getInstance().getDocByGroup(Constants.getInt("headerFooterGroupId"))));
	}

	public List<String> getMenuList() {
		if (getLocalSystemGroup() != null)
		{
			List<DocDetails> localDocsInGroup = DocDB.getInstance().getBasicDocDetailsByGroup(getLocalSystemGroup().getGroupId(), -1);
			return getDocDetailItems(addFromTemlateDoc(localDocsInGroup));
		}

		return getDocDetailItems(addFromTemlateDoc(DocDB.getInstance().getDocByGroup(Constants.getInt("menuGroupId"))));
	}

	public String getCssStyle() {
		if (cssStyle == null) {
			cssStyle = getEditorCssStyle();
		}

		return cssStyle;
	}

	private List<Map<String, String>> sessionCssParsed;
	public List<Map<String, String>> getSessionCssParsed() {
		if (sessionCssParsed == null)
		{
			sessionCssParsed = new ArrayList<>();

			StringTokenizer stCss = new StringTokenizer(getBaseCssPath(), ",\n");
			while (stCss.hasMoreTokens())
			{
				String cssFile = stCss.nextToken();
				//bootstrap neparsujeme, to by bol masaker
				if (cssFile.contains("bootstrap.min.css") || cssFile.contains("bootstrap.css")) continue;

				//PathFilter.getRealPath je pouzity z dovodu najdenia custom_path pre vyvoj
				IwcmFile file = new IwcmFile(PathFilter.getCustomPathRealPath(cssFile));
				if (!file.exists()) {
					//skus najst subor s pridanym InstallName
					file = new IwcmFile(PathFilter.getCustomPathRealPath(Tools.replace(cssFile, "/templates/", "/templates/"+Constants.getInstallName()+"/")));
				}
				if (!file.exists()) {
				    Logger.error(EditorForm.class, "Css file " + file.getName() + " not exist");
				    continue;
            }
				sk.iway.css.CssParser cssParser = new sk.iway.css.CssParser(file);
				List<CssDto> editorCss = cssParser.getElements();
				try
				{
					//prehod na json format
					for(CssDto element : editorCss)
					{
						Map<String, String> map = new HashMap<>();
						map.put("tag", element.getTag());
						map.put("class", element.getClassName());
						map.put("title", element.getTitle());
						sessionCssParsed.add(map);
					}
				}
				catch (Exception ex)
				{
					sk.iway.iwcm.Logger.error(ex);
				}
			}
		}

		return sessionCssParsed;
	}

	private String getEditorCssStyle()
	{
		String cssStyle = getBaseCssPath();
		String cssStyle2 = getTempCssPath();

		if (Tools.isNotEmpty(cssStyle2)) {
			cssStyle += "," + cssStyle2;
		}

		String editorCss = getEditorCssPath();
		if (editorCss != null) {
			cssStyle += "," + getContextPath() + editorCss;
		}

		cssStyle = Tools.replace(cssStyle, "\n", ",");
		cssStyle = Tools.replace(cssStyle, "\r", "");

		return cssStyle;
	}

	public String getEditorCssPath() {
		String editorCss;
		getBaseCssPathImpl();
		if (baseCssPath.startsWith("/templates/") && baseCssPath.contains("editor.css") == false) {
			// skus automaticky dohladat aj editor.css v danom adresari
			editorCss = baseCssPath.substring(0, baseCssPath.lastIndexOf("/")) + "/editor.css";
			if (FileTools.isFile(editorCss)) {
				return editorCss;
			}
		}

		editorCss = "/css/editor.css";
		if (FileTools.isFile(editorCss)) {
			return editorCss;
		}

		return null;
	}

	private void getBaseCssPathImpl() {
		String defaultBaseCssPath = "/css/page.css";
		if (Tools.isEmpty(baseCssPath)) {
			if (getTemplate() != null) {
				baseCssPath = getTemplate().getBaseCssPath();
			}

			if (Tools.isNotEmpty(baseCssPath) && Constants.getBoolean("multiDomainEnabled") == true && Tools.isNotEmpty(getDomainName())) {
				// baseCssPath = Tools.replace(baseCssPath, "/css/",
				// "/css/"+MultiDomainFilter.getDomainAlias(editForm.getDomainName())+"/");
				baseCssPath = MultiDomainFilter.rewriteUrlToLocal(baseCssPath, MultiDomainFilter.getDomainAlias(getDomainName()));
			}
		}

		if (Tools.isEmpty(baseCssPath)) {
			baseCssPath = defaultBaseCssPath;
		}
	}

	/**
	 * Vrati cestu k CSS suborom vratane editor.css
	 * @return
	 */
	public String getBaseCssPath()
	{
		getBaseCssPathImpl();

		String editorCssPath = getEditorCssPath();
		if (baseCssPath.startsWith("/templates/") && baseCssPath.contains("editor.css") == false && editorCssPath != null && editorCssPath.startsWith("/templates")) {
			baseCssPath += "," + editorCssPath;
		}

		return baseCssPath;
	}

	private String getTempCssPath()
	{
		if (tempCssPath == null) {
			TemplateDetails temp = getTemplate();
			String domainName = getDomainName();
			if (temp != null && temp.getCss() != null && temp.getCss().length() > 1) {
				tempCssPath = temp.getCss();
				if (Constants.getBoolean("multiDomainEnabled") == true && Tools.isNotEmpty(domainName)) {
					String multidomainAlias = MultiDomainFilter.getDomainAlias(domainName);
					if (Tools.isNotEmpty(multidomainAlias)) {
						tempCssPath = Tools.replace(tempCssPath, "/css/", "/css/" + multidomainAlias + "/");
					}
				}
			}
		}
		return tempCssPath;
	}

	private List<String> getDocDetailItems(List<DocDetails> list)
	{
		List<String> result = new ArrayList<>();

		for (DocDetails doc : list) {
			StringBuilder sb = new StringBuilder();
			sb.append(doc.getDocId()).append("|").append(doc.getTitle());
			result.add(sb.toString());
		}

		return result;
	}

	private List<DocDetails> addFromTemlateDoc(List<DocDetails> list)
	{
		DocDetails fromTemplate = new DocDetails();
		fromTemplate.setDocId(-1);
		fromTemplate.setTitle(Prop.getInstance(getLanguage()).getText("editor.fromTemplate"));

		list.add(0, fromTemplate);

		return list;
	}

	public List<LabelValueDetails> getPerexGroups() {
		List<LabelValueDetails> result = new ArrayList<>();
		List<PerexGroupBean> perexGroups =  DocDB.getInstance().getPerexGroups(getGroupId());
		for (PerexGroupBean perexGroup : perexGroups) {
			LabelValueDetails labelValueDetails = new LabelValueDetails();
			labelValueDetails.setLabel(perexGroup.getPerexGroupName());
			labelValueDetails.setInt1(perexGroup.getPerexGroupId());
			result.add(labelValueDetails);
		}

		return result;
	}

	public int getSyncId() {
		return syncId;
	}

	public void setSyncId(int syncId) {
		this.syncId = syncId;
	}
}