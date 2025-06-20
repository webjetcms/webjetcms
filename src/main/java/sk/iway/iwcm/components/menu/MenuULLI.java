package sk.iway.iwcm.components.menu;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageParams;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.tags.support.ResponseUtils;
import sk.iway.iwcm.users.UsersDB;

//import sk.iway.iwcm.Logger;


/**
 *  MenuULLI.java
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2005
 *@author       $Author: jeeff $
 *@version      $Revision: 1.25 $
 *@created      Date: 18.1.2005 14:10:20
 *@modified     $Date: 2010/02/09 08:38:45 $
 */
public class MenuULLI
{

	private MenuULLI() {
		//private
	}

	public static String doTree(int rootGroup, int maxLevel, int startOffset, HttpServletRequest request)
	{
		return doTree(rootGroup, maxLevel, startOffset, null, null, request);
	}

	public static String doTree(int rootGroup, int maxLevel, int startOffset, String classes, String rootUlId, HttpServletRequest request)
	{
		return doTree(rootGroup, maxLevel, startOffset, classes, rootUlId, -1, request);
	}

	public static String doTree(int rootGroup, int maxLevel, int startOffset, String classes, String rootUlId, int itemMaxLen, HttpServletRequest request)
	{
		return doTree(rootGroup, maxLevel, startOffset, classes, rootUlId, itemMaxLen, false, request);
	}

	public static String doTree(int rootGroup, int maxLevel, int startOffset, String classes, String rootUlId, int itemMaxLen, boolean isSitemap, HttpServletRequest request)
	{
		GroupsDB groupsDB = GroupsDB.getInstance();
		DocDB docDB = DocDB.getInstance();
		int docId = 0;
		boolean generateEmptySpan = false;
		boolean openAllItems = false;
		String spanCharacters[] = new String[0];
		int minLiItems = -1;

		if (Tools.isEmpty(classes)) classes = "full";

		DocDetails doc = (DocDetails)request.getAttribute("docDetails");
		if (Tools.isNotEmpty(request.getParameter("docid")))
		{
			docId = Integer.parseInt(request.getParameter("docid"));
		}
		//ak sa zobrazuje logon stranka, tvar sa v menu ako keby to bola normalna
		if (request.getParameter("origDocId") != null)
		{
			docId = Integer.parseInt(request.getParameter("origDocId"));
		}
		if (request.getAttribute("ulLiForceDocid") != null)
		{
			docId = Tools.getIntValue((String)request.getAttribute("ulLiForceDocid"), -1);
		}

		if (doc != null && docId != doc.getDocId())
		{
			doc = DocDB.getInstance().getBasicDocDetails(docId, false);
		}

		if (request.getAttribute("ulLiOpenAllItems") != null)
		{
			openAllItems = true;
		}
		if (request.getAttribute("ulLiGenerateEmptySpan") != null)
		{
			generateEmptySpan = true;
		}
		if (request.getAttribute("ulLiSpanCharacters")!=null)
		{
			spanCharacters = Tools.getTokens((String)request.getAttribute("ulLiSpanCharacters"), ",", false);
			if (spanCharacters.length > 0) generateEmptySpan = true;
		}
		//minimalny pocet LI poloziek, ktore sa musia vygenerovat
		if (request.getAttribute("ulLiMinLiItems") != null)
		{
			minLiItems = Tools.getIntValue((String)request.getAttribute("ulLiMinLiItems"), -1);
		}

		if (rootGroup == 0 && doc != null) {
			//get root group from doc
			rootGroup = groupsDB.getRoot(doc.getGroupId());
		}

		GroupDetails rootGroupDetails = groupsDB.getGroup(rootGroup);
		if (rootGroupDetails!=null)
		{
			//ak sa robi nejaky substrom, tak nie je mozne spravit posun
			if (rootGroupDetails.getParentGroupId()>0)
			{
				startOffset = 0;
			}
		}

		if (startOffset > 0)
		{
		   //musime sa posunut (robime sekcne menu posunute o level)

			if (doc!=null)
			{
				List<GroupDetails> parents = groupsDB.getParentGroups(doc.getGroupId());
				if (parents.size()>startOffset)
				{
					GroupDetails grp = parents.get(parents.size() - (startOffset+1));
					//Logger.println(MenuULLI.class,"grp="+grp.getGroupIdName());
					rootGroup = grp.getGroupId();
				}
				else
				{
					return("");
				}
			}
			else
			{
				return("");
			}
		}

		HashMap<Integer, String> groupTable = null;
		if (docId > 0)
		{
			int groupId = docDB.getBasicDocDetails(docId, true).getGroupId();
			if (groupId > 0)
			{
				groupTable = new HashMap<Integer, String>();
				//Logger.println(MenuULLI.class,"nastavujem home na: " + groupId);
				groupTable.put(Integer.valueOf(groupId), "homeDir");
				getGroupTree(groupId, groupTable);

				//Logger.println(MenuULLI.class,"Pocet zaznamov: " +groupTable.size());
			}
		}

		List<GroupDetails> treeList = groupsDB.getGroupsAll();
		//Logger.println(MenuULLI.class,"size="+treeList.size()+" root="+rootGroup);

		rootGroupDetails = groupsDB.getGroup(rootGroup);
		if (rootGroupDetails==null)
		{
			return("");
		}

		request.setAttribute("!ROOT_GROUP_ID!", Integer.toString(rootGroupDetails.getGroupId()));
		request.setAttribute("!ROOT_GROUP_DOCID!", Integer.toString(rootGroupDetails.getDefaultDocId()));
		request.setAttribute("!ROOT_GROUP_LINK!", docDB.getDocLink(rootGroupDetails.getDefaultDocId(), request));
		request.setAttribute("!ROOT_GROUP_NAME!", rootGroupDetails.getGroupName());
		request.setAttribute("!ROOT_GROUP_NAVBAR!", rootGroupDetails.getNavbarName());

		//toto sa pouziva v komponente, ktora len vypisuje !ROOT_GROUP_NAME!
		if (request.getAttribute("ulLiSkipMenuGeneration")!=null)
		{
			request.removeAttribute("ulLiSkipMenuGeneration");
			return("");
		}

		PageParams pageParams = new PageParams(request);

	   return(doTree(rootGroup, rootGroup, 1, maxLevel, treeList, groupTable, docId, openAllItems, generateEmptySpan, spanCharacters, minLiItems, classes, rootUlId, request, pageParams, itemMaxLen, isSitemap));
	}

	private static String doTree(int rootGroup, int startRootGroup, int level, int maxLevel, List<GroupDetails> treeList, HashMap<Integer, String> groupTable, int docId, boolean openAllItems, boolean generateEmptySpan, String[] spanCharacters, int minLiItems, String classes, String rootUlId, HttpServletRequest request, PageParams pageParams, int itemMaxLen, boolean isSitemap)
	{
		//Logger.debug(MenuULLI.class, "doTree, root="+rootGroup+" srg="+startRootGroup+" lev="+level+" max="+maxLevel+" doc="+docId+docId+" open="+openAllItems+" empty="+generateEmptySpan+" minLi="+minLiItems+" cls="+classes+" root="+rootUlId+" maxLen="+itemMaxLen);

		StringBuilder out = new StringBuilder();
		StringBuilder outDocs = new StringBuilder();

		if (maxLevel > 0 && level > maxLevel) return(out.toString());

		boolean display = false;
		String styleClassDir;
		String styleClassDoc;
		boolean isExternalLink = false;

		String aParam = "";

		String liAddClass = "";

		String navnarNameUCase = "";
		String navbarName = "";
		int aParamStart;
		int aParamEnd;

		String[] menuInfoDirNames = Tools.getTokens(pageParams.getValue("menuInfoDirName", ""), ",");

		boolean hasLi = false;
		String spacer = "                                                                                                      ";
		try
		{
			spacer = spacer.substring(0, level*3);
		}
		catch (Exception e)
		{
			spacer = "";
		}
		String spacerLi = spacer+"   ";

		DocDB docDB = DocDB.getInstance();
		GroupsDB groupsDB = GroupsDB.getInstance();
		GroupDetails rootGroupDetails = groupsDB.getGroup(rootGroup);
		if (rootGroupDetails==null || rootGroupDetails.getMenuType()==GroupDetails.MENU_TYPE_NOSUB)
		{
			return("");
		}

		if (level == 1)
		{
			//Logger.println(MenuULLI.class, "SOm level 1");
		}

		Identity user = UsersDB.getCurrentUser(request.getSession());
		String isProtected = "";

		String bootstrapUlClasses = pageParams.getValue("ulClasses", "nav navbar-nav");
		String customUlClasses = pageParams.getValue("ulClasses", "");
		String liClasses = pageParams.getValue("liClasses", "");
		if (Tools.isEmpty(liClasses) &&  "bootstrap".equals(classes)) liClasses = "nav-item";
		if (Tools.isNotEmpty(liClasses)) liClasses = liClasses + " ";
		String aClasses = pageParams.getValue("aClasses", "");
		if (Tools.isEmpty(aClasses) &&  "bootstrap".equals(classes)) aClasses = "nav-link";
		if (Tools.isNotEmpty(aClasses)) aClasses = aClasses + " ";

		//tu si plnime outDoc, aby sme vedeli nastavit hasNoChild pre grupu
		if ( (level==1 || ( groupTable != null && groupTable.containsKey(Integer.valueOf(rootGroup))) || openAllItems) && rootGroupDetails.getMenuType(request.getSession()) == GroupDetails.MENU_TYPE_NORMAL && rootGroupDetails.isInternal()==false && pageParams.getBooleanValue("onlyFolders", false)==false)
		{
			for (DocDetails doc : docDB.getBasicDocDetailsByGroup(rootGroup, DocDB.ORDER_PRIORITY))
			{
				isExternalLink = false;
				if (Tools.isNotEmpty(doc.getPasswordProtected()) && DocDB.canAccess(doc, user, false))
					isProtected = " protectedAllow";
				else if (Tools.isNotEmpty(doc.getPasswordProtected())) isProtected = " protected";
				else isProtected = "";

				if ("none".equals(classes)) styleClassDoc = "";
				else if ("basic".equals(classes))
				{
					if (Tools.isNotEmpty(isProtected)) styleClassDoc = " class=\"" + isProtected.trim() + "\"";
					else styleClassDoc = "";
				} else
					styleClassDoc = " class=\"menuULDocClose page" + doc.getDocId() + " ulLiLevel" + level + isProtected + "\"";
				//Logger.println(MenuULLI.class,"test: " + doc.getDocId()+" "+doc.getTitle());
				if (doc.isAvailable() && isShowIn(doc, isSitemap, request) && (doc.getDocId() != rootGroupDetails.getDefaultDocId() || rootGroupDetails.getHtmlHead().indexOf("<DEFAULTPAGESUBMENU") != -1))
				{
					display = false;
					if (groupTable != null)
					{
						//Logger.println(MenuULLI.class,"TEST: " + doc.getGroupId() + " id="+doc.getDocId());
						if (groupTable.containsKey(Integer.valueOf(doc.getGroupId())) || level == 1)
						{
							// pridam classu k LI elementu
							liAddClass = "";
							navbarName = doc.getNavbar();
							String navnarNameUCase1 = navbarName.toUpperCase();
							int liAddClassStart = navnarNameUCase1.indexOf("<LIADDCLASS=");
							if (liAddClassStart != -1)
							{
								try
								{
									int liAddClassEnd = navnarNameUCase1.indexOf(">", liAddClassStart);
									if (liAddClassEnd > liAddClassStart)
									{
										liAddClass = " " + navbarName.substring(liAddClassStart + 13, liAddClassEnd);
										//odstran aParam z textu
										//navbarName = Tools.replace(navbarName, navbarName.substring(liAddClassStart, liAddClassEnd + 1), "");
									}
								} catch (Exception ex)
								{
									//failsafe
								}
							}

							if (docId == doc.getDocId())
							{
								if ("none".equals(classes)) styleClassDoc = "";
								else if ("basic".equals(classes))
									styleClassDoc = " class=\"open" + isProtected + liAddClass + "\"";
								else if ("bootstrap".equals(classes))
									styleClassDoc = " class=\"active" + isProtected + liAddClass + "\"";

								else
									styleClassDoc = " class=\"menuULDocOpen page" + doc.getDocId() + " ulLiLevel" + level + isProtected + liAddClass + "\"";
							}
							//ak mame presmerovanie na externu linku
							if(doc != null && Tools.isNotEmpty(doc.getExternalLink()) && doc.getExternalLink().trim().toLowerCase().startsWith("http"))
							{
								if(Tools.isEmpty(styleClassDoc)) styleClassDoc = " class=\"externalLink\"";
								else styleClassDoc = styleClassDoc.substring(0, styleClassDoc.length()-1)+" externalLink\"";
								isExternalLink = true;
							}
							display = true;
						}
					}
					if (openAllItems)
					{
						display = true;
					}
					if (display)
					{
						aParam = "";
						navbarName = doc.getNavbar();
						navnarNameUCase = navbarName.toUpperCase();
						int liAddClassStart = navnarNameUCase.indexOf("<LIADDCLASS=");
						if (liAddClassStart != -1)
						{
							try
							{
								int liAddClassEnd = navnarNameUCase.indexOf(">", liAddClassStart);
								if (liAddClassEnd > liAddClassStart)
								{
									//odstran aParam z textu
									navbarName = Tools.replace(navbarName, navbarName.substring(liAddClassStart, liAddClassEnd + 1), "");
								}
							} catch (Exception ex)
							{
								//failsafe
							}
						}
						aParamStart = navnarNameUCase.indexOf("<APARAM");
						if (aParamStart != -1)
						{
							try
							{
								aParamEnd = navnarNameUCase.indexOf('>', aParamStart);
								if (aParamEnd > aParamStart)
								{
									aParam = " " + navbarName.substring(aParamStart + 7, aParamEnd - 1);
									//odstran aParam z textu
									navbarName = Tools.replace(navbarName, navbarName.substring(aParamStart, aParamEnd + 1), "");
								}
							} catch (Exception ex)
							{
								//failsafe
							}
						}

						outDocs.append(spacerLi).append("<li").append(styleClassDoc).append("><a "+generateClassParam(aClasses, false)+"href=\"").append(docDB.getDocLink(doc.getDocId(), request)).append('\"').append(aParam).append(isExternalLink ? " target=\"_blank\"" : "").append('>');
						if (generateEmptySpan)
						{
							String spanChar = "";
							if (spanCharacters.length > 0)
							{
								int index = level;
								if (index > spanCharacters.length) index = spanCharacters.length - 1;
								spanChar = spanCharacters[index];
							}
							outDocs.append("<span>").append(spanChar).append("</span>");
						}
						if (itemMaxLen > 0 && itemMaxLen < navbarName.length())
						{
							navbarName = navbarName.substring(0, itemMaxLen);
						}
						outDocs.append(Tools.convertToHtmlTags(ResponseUtils.filter(navbarName)) + "</a></li>\n");
						hasLi = true;
					}
				}
			}
		}

		try
		{
			if (level==1 && Tools.isNotEmpty(rootUlId) && !"bootstrap".equals(classes) && !"basic".equals(classes)) out.append(spacer).append("<ul id=\"").append(rootUlId).append("\">\n");
			else if(level>1 && "bootstrap".equals(classes) && maxLevel!=1) out.append(spacer).append("<ul class=\"dropdown-menu\" aria-labelledby=\"navbarDropdown"+rootGroupDetails.getGroupId()+"\">\n");
			else if(level==1 && "bootstrap".equals(classes)) out.append(spacer).append("<ul class=\""+bootstrapUlClasses+"\">\n");
			else if(level==1 && "basic".equals(classes)) out.append(spacer).append("<ul id=\""+rootUlId+"\" class=\""+customUlClasses+"\">\n");

			else out.append(spacer).append("<ul>\n");

			String sTmp;

			//ak default stranka adresara nie je available adresar nezobrazme
			boolean rootGroupPageAvailable = true;
			DocDetails groupDocDetails = docDB.getBasicDocDetails(rootGroupDetails.getDefaultDocId(), false);
			if (groupDocDetails != null && groupDocDetails.isAvailable()==false) rootGroupPageAvailable = false;

			//level1 musime vzdy akceptovat, moze sa stat ze to nechcem mat v hornom menu, ale submenu uz potrebujem
			if ((rootGroupDetails.isInternal()==false && rootGroupDetails.getMenuType(request.getSession())!=GroupDetails.MENU_TYPE_HIDDEN && rootGroupPageAvailable==true) || level==1)
			{

				for (GroupDetails group : treeList)
				{
					isExternalLink = false;

					//Logger.println(MenuULLI.class,"testing: " + group.getGroupId()+" "+group.getGroupName()+" "+group.getParentGroupId()+" r="+rootGroup);
					if (group.getParentGroupId()!=rootGroup)
					{
						continue;
					}

					if (group.isInternal() || group.getMenuType(request.getSession())==GroupDetails.MENU_TYPE_HIDDEN)
					{
						//Logger.debug(MenuULLI.class, "continuing, internal="+group.isInternal()+" menuType="+group.getMenuType(request.getSession())+" hidden="+GroupDetails.MENU_TYPE_HIDDEN);
						continue;
					}

					if (groupTable != null)
					{
						if (groupTable.containsKey(Integer.valueOf(rootGroup)) || rootGroup == startRootGroup)
						{
							//if ("homeDir".equals(groupTable.get(Integer.valueOf(group.getGroupId()))))
							display = true;
						}
						else
						{
							display = false;
						}
					}
					if (openAllItems)
					{
						display = true;
					}

					//ak default stranka adresara nie je available adresar nezobrazme
					groupDocDetails = docDB.getBasicDocDetails(group.getDefaultDocId(), false);
					if (groupDocDetails != null && groupDocDetails.isAvailable()==false)
					{
						//Logger.debug(MenuULLI.class, "docDetails is not available: "+groupDocDetails.getDocId()+" available="+groupDocDetails.isAvailable());
						continue;
					}
					//Logger.debug(MenuULLI.class, group.getGroupIdName()+" display="+display);

					if (display)
					{
						sTmp = doTree(group.getGroupId(), startRootGroup, level+1, maxLevel, treeList, groupTable, docId, openAllItems, generateEmptySpan, spanCharacters, minLiItems, classes, rootUlId, request, pageParams, itemMaxLen, isSitemap);
						//Logger.println(MenuULLI.class,"group: " + group.getGroupId()+" notEmpty="+Tools.isNotEmpty(sTmp)+" gdefdoc="+group.getDefaultDocId()+" docid="+docId);
						//ak to nie je prazdne, sme dnu, alebo ak je docId rovne default (nema ziadne podadresare a podstranky)
						String hasNoChild = " hasNoChild";
						String bootstrapDropdownClass = "";
						String bootstrapDropdownToggle = "";
						if (hasChilds(group, groupsDB, docDB, request, pageParams, isSitemap))
						{
							hasNoChild = " hasChild";
							if ("bootstrap".equals(classes)) {
								bootstrapDropdownClass = " dropdown ";
								bootstrapDropdownToggle = " dropdown-toggle ";
							}
						}
						if(Tools.isNotEmpty(group.getPasswordProtected()) && group.isAvailableForUser(user)) isProtected = " protectedAllow";
						else if(Tools.isNotEmpty(group.getPasswordProtected())) isProtected = " protected";
				   		else isProtected = "";

						//pridam classu k li
						liAddClass = "";
						navbarName = group.getNavbarName();
						String navnarNameUCase1 = navbarName.toUpperCase();
						int liAddClassStart = navnarNameUCase1.indexOf("<LIADDCLASS=");
						if (liAddClassStart != -1)
						{
							try
							{
								int liAddClassEnd = navnarNameUCase1.indexOf(">", liAddClassStart);
								if (liAddClassEnd > liAddClassStart)
								{
									liAddClass = " " + navbarName.substring(liAddClassStart + 13, liAddClassEnd-1);
									//odstran aParam z textu
									//na nic sa nepouzije, netreba navbarName = Tools.replace(navbarName, navbarName.substring(liAddClassStart, liAddClassEnd + 1), "");
								}
							}
							catch (Exception ex)
							{
								//failsafe
							}
						}

						if (Tools.isNotEmpty(sTmp) || group.getDefaultDocId()==docId || (groupTable!=null && groupTable.containsKey(Integer.valueOf(group.getGroupId()))))
						{

							if ("none".equals(classes)) styleClassDir = "";
						   else if ("basic".equals(classes)) 	styleClassDir = " class=\"open"+isProtected+liAddClass+"\"";
						   else if ("bootstrap".equals(classes)) 	styleClassDir = " class=\""+liClasses+bootstrapDropdownClass+"active "+isProtected+liAddClass+"\"";

						   else styleClassDir = " class=\"menuULDirOpen group"+group.getGroupId()+" ulLiLevel"+level+hasNoChild+"Open"+isProtected+liAddClass+"\"";
							if (openAllItems)
							{
								//zdetekuj, ci cesta k docid vyhovuje adresaru
								if (groupTable!=null && groupTable.containsKey(Integer.valueOf(group.getGroupId())) == false)
								{
									if ("none".equals(classes)) styleClassDir = "";
								   else if ("basic".equals(classes) || "bootstrap".equals(classes))
								   {
								   	if (Tools.isNotEmpty(isProtected)) styleClassDir = " class=\""+liClasses+isProtected.trim()+liAddClass+"\"";
								   	else styleClassDir = generateClassParam(liClasses+bootstrapDropdownClass+liAddClass, true);
								   }
								   else styleClassDir = " class=\"menuULDirClose group"+group.getGroupId()+" ulLiLevel"+level+hasNoChild+isProtected+liAddClass+"\"";
								}
							}
						}
						else
						{
							if ("none".equals(classes)) styleClassDir = "";
						   else if ("basic".equals(classes) || "bootstrap".equals(classes))
						   {
						   	if (Tools.isNotEmpty(isProtected)) styleClassDir = " class=\""+liClasses+bootstrapDropdownClass+isProtected.trim()+liAddClass+"\"";
						   	else styleClassDir = generateClassParam(liClasses+bootstrapDropdownClass+liAddClass, true);
						   }
						   else styleClassDir = " class=\"menuULDirClose group"+group.getGroupId()+" ulLiLevel"+level+hasNoChild+isProtected+liAddClass+"\"";
						}

						navbarName = group.getNavbarName();
						navnarNameUCase = navbarName.toUpperCase();
						liAddClassStart = navnarNameUCase.indexOf("<LIADDCLASS=");
						if (liAddClassStart != -1)
						{
							try
							{
								int liAddClassEnd = navnarNameUCase.indexOf(">", liAddClassStart);
								if (liAddClassEnd > liAddClassStart)
								{
									//odstran aParam z textu
									navbarName = Tools.replace(navbarName, navbarName.substring(liAddClassStart, liAddClassEnd + 1), "");
								}
							}
							catch (Exception ex)
							{
								//failsafe
							}
						}
						aParam = "";
						aParamStart = navnarNameUCase.indexOf("<APARAM");
						if (aParamStart!=-1)
						{
							try
							{
								aParamEnd = navnarNameUCase.indexOf('>', aParamStart);
								if (aParamEnd > aParamStart)
								{
									aParam = " "+navbarName.substring(aParamStart+7, aParamEnd);
									//odstran aParam z textu
									navbarName = Tools.replace(navbarName, navbarName.substring(aParamStart, aParamEnd+1), "");
								}
							}
							catch (Exception ex)
							{
								//failsafe
							}
						}

						//ak mame presmerovanie na externu linku
						if(groupDocDetails != null && Tools.isNotEmpty(groupDocDetails.getExternalLink()) && groupDocDetails.getExternalLink().trim().toLowerCase().startsWith("http"))
						{
							if(Tools.isEmpty(styleClassDir)) styleClassDir = " class=\"externalLink\"";
							else styleClassDir = styleClassDir.substring(0, styleClassDir.length()-1)+" externalLink\"";
							isExternalLink = true;
						}

						boolean menuIncludePerex = pageParams.getBooleanValue("menuIncludePerex", false);
						int menuIncludePerexLevel  = pageParams.getIntValue("menuIncludePerexLevel", 0);

						boolean hasMenuInfoDirName = false;
						if (menuInfoDirNames != null && menuInfoDirNames.length > 0) {
							for (String menuInfoDirName : menuInfoDirNames) {
								if (Tools.isNotEmpty(menuInfoDirName) && menuInfoDirName.equals(navbarName) && group.getDefaultDocId() > 0) {
									DocDetails dd = docDB.getDoc(group.getDefaultDocId());
									if (dd != null) {
										out.append(spacerLi).append("<li class=\"").append(styleClassDir).append(" menu-info-li").append("\">");
										out.append("<div class=\"menu-info\"><span class=\"menu-info-headline\">").append(dd.getTitle()).append("</span>");

										out.append("<div class=\"menu-info-content\">").append(dd.getData()).append("</div>");
										out.append("</div></li>\n");
									}

									hasMenuInfoDirName = true;
								}
							}
						}

						if (!hasMenuInfoDirName) {
							out.append(spacerLi).append("<li").append(styleClassDir).append("><a "+generateClassParam(aClasses+bootstrapDropdownToggle, false));
							if ("bootstrap".equals(classes) && Tools.isNotEmpty(bootstrapDropdownToggle)) out.append("id=\"navbarDropdown"+group.getGroupId()+"\" role=\"button\" data-bs-toggle=\"dropdown\" aria-expanded=\"false\"");
							out.append("href=\"").append(docDB.getDocLink(group.getDefaultDocId(), request)).append('\"').append(aParam).append(isExternalLink ? " target=\"_blank\"" : "").append('>');

							if (generateEmptySpan) {
								String spanChar = "";
								if (spanCharacters.length > 0) {
									int index = level;
									if (index > spanCharacters.length) index = spanCharacters.length;
									spanChar = spanCharacters[index - 1];
								}
								out.append("<span>").append(spanChar).append("</span>");
							}
							if (itemMaxLen > 0 && itemMaxLen < navbarName.length()) {
								navbarName = navbarName.substring(0, itemMaxLen);
							}
							navbarName = ResponseUtils.filter(navbarName);
							navbarName = Tools.convertToHtmlTags(navbarName);

							if (menuIncludePerex && level >= menuIncludePerexLevel) {
								DocDetails dd = docDB.getDoc(group.getDefaultDocId());

								out.append("<span class=\"title\">")
										.append(navbarName)
										.append("</span>")
										.append("<span class=\"perex\">");

								if (dd != null) {
									out.append(dd.getPerex());
								}

								out.append("</span>");
							} else {
								out.append(navbarName);
							}

							out.append("</a>");

							if (Tools.isNotEmpty(sTmp)) {
								out.append("\n").append(sTmp).append(spacerLi);
							}

							out.append("</li>\n");
						}
						hasLi = true;
					}
				}
			}

			if ( (level==1 || ( groupTable != null && groupTable.containsKey(Integer.valueOf(rootGroup))) || openAllItems) && rootGroupDetails.getMenuType(request.getSession()) == GroupDetails.MENU_TYPE_NORMAL && rootGroupDetails.isInternal()==false)
			{
				out.append(outDocs);
			}

			if (level==1 && minLiItems>0)
			{
				//spocitaj pocet li poloziek
				int liCounter = 0;
				int index = out.indexOf("<li");
				int failsafe = 0;
				while (index > 0 && failsafe < 500)
				{
					liCounter++;
					index = out.indexOf("<li", index+1);
				}

				StringBuilder outBuf = new StringBuilder(out);
				if (liCounter < minLiItems)
				{
					while (liCounter < minLiItems)
					{
						outBuf.append(spacer).append("   <li class=\"menuULDummyItem\">&nbsp;</li>\n");
						liCounter++;
					}
				}
				out = outBuf;
			}

			out.append(spacer+"</ul>\n");

			if (hasLi==false)
			{
				//Logger.debug(MenuULLI.class, "has LI=false, out="+out.toString());
			   out = new StringBuilder();
			}

		}
		catch (Exception ex)
		{
			Logger.error(MenuULLI.class, ex);
		}

		//Logger.debug(MenuULLI.class, "out.toString end: "+out.toString());
		return out.toString();
	}

	private static boolean hasChilds(GroupDetails rootGroup, GroupsDB groupsDB, DocDB docDB, HttpServletRequest request, PageParams pageParams, boolean isSitemap)
	{
		if (rootGroup.getMenuType(request.getSession())==GroupDetails.MENU_TYPE_NOSUB) return false;

		for (GroupDetails group : groupsDB.getGroupsAll())
		{
			if (group.getParentGroupId() == rootGroup.getGroupId() && group.getMenuType(request.getSession())!=GroupDetails.MENU_TYPE_HIDDEN && group.isInternal()==false)
			{
				return(true);
			}
		}

		if (rootGroup.getMenuType(request.getSession()) == GroupDetails.MENU_TYPE_NORMAL && pageParams.getBooleanValue("onlyFolders", false)==false)
		{
			for (DocDetails doc : docDB.getBasicDocDetailsByGroup(rootGroup.getGroupId(), -1))
			{
			   if (doc.isAvailable() && isShowIn(doc, isSitemap, request) && doc.getDocId() != rootGroup.getDefaultDocId())
			   {
			   	return(true);
			   }
			}
		}

		return(false);
	}


	/**
	 * K danemu groupID ziska rekurzivne zoznam podskupin (neutriedeny)
	 * @param groupId
	 * @param groupTable
	 */
	private static void getGroupTree(int groupId, HashMap<Integer, String> groupTable)
	{
		GroupsDB groupsDB = GroupsDB.getInstance();
		StringBuilder outStr = new StringBuilder();
		if (groupId > 0 && groupTable != null)
		{
			GroupDetails group = groupsDB.getGroup(groupId);
			if (group != null)
			{
				int rootGroupId = group.getParentGroupId();
				if (rootGroupId > 0)
				{
					groupTable.put(Integer.valueOf(rootGroupId), "");
					for (Map.Entry<Integer, String> e : groupTable.entrySet())
					{
						outStr.append(e.getKey()).append(' ');
					}
					//Logger.println(MenuULLI.class,"Zoznam getGroupTree: " +outStr);
					getGroupTree(rootGroupId, groupTable);
				}
			}
		}
	}

	private static boolean isShowIn(DocDetails doc, boolean isSitemap, HttpServletRequest request) {
		if (isSitemap) return doc.isShowInSitemap(request);
		return doc.isShowInMenu(request);
	}

	/**
	 * Vygeneruje class="classes" parameter len ked classes nie je prazdne
	 * @param classes
	 * @return
	 */
	private static String generateClassParam(String classes, boolean beginWithEmptySpace) {
		if (classes == null || Tools.isEmpty(classes)) return "";
		StringBuilder out = new StringBuilder();
		if (beginWithEmptySpace) out.append(" ");
		out.append("class=\"").append(classes).append("\"");

		return out.toString();
	}
}