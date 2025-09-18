package sk.iway.iwcm.common;

import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import cvu.html.HTMLTokenizer;
import cvu.html.TagToken;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.editor.EditorForm;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.multidomain.MultiDomainFilter;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

public class CloudToolsForCore {

    public static final String CLOUD_TEXT_KEY_PREFIX = "cloud.template.";
	public static final Pattern CLOUD_TEXT_PATTERN = Pattern.compile(CLOUD_TEXT_KEY_PREFIX+"[a-zA-Z0-9\\._-]+");

    protected CloudToolsForCore() {
        //utility class
    }

    /**
     * Vrati aktualne meno domeny
     * @return
     */
    public static String getDomainName()
    {
        String domain = "";
        RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
        if (rb != null)
        {
            domain = rb.getDomain();

            //pre niektore adresare musime vratiti default domain, napr. /images/gallery/user/
            if(Constants.getBoolean("enableStaticFilesExternalDir") && Tools.isNotEmpty(Constants.getString("domainIdFixedUrls"))
                    && rb.isUserAdmin())
            {
                if (DocTools.isXssStrictUrlException(rb.getUrl(), "domainIdFixedUrls") ||
                        "/admin/importxls.do".equalsIgnoreCase(rb.getUrl()) && "type=sk.iway.iwcm.users.ImportUsersXLS".equals(rb.getQueryString()))
                {
                    //return rootGroupId;
                    GroupDetails root = GroupsDB.getInstance().getGroup(Constants.getInt("rootGroupId"));
                    if (root != null) {
                        return root.getDomainName();
                    }
                }
            }
        }
        if (Tools.isEmpty(domain)) domain = "unknown";

        return domain.toLowerCase();
    }

    /**
     * Vrati ID domeny pre databazove tabulky delene podla domen (napr. gallery) f
     * ak nie je zapnuty cloud tak automaticky vrati 1
     * @return vrati id root adresara domeny, ak nie je ziadna domena tak vrati 1
     */
    public static int getDomainId()
    {
        String domain = "";
        int domainId = 1;
        if(InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir")==true)
        {
            RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
            if (rb != null)
            {
                //pre niektore URL musime vratit default domain (napr. nastenku na welcome obrazovke), ak je to len multidomain
                if(InitServlet.isTypeCloud()==false && Tools.isNotEmpty(Constants.getString("domainIdFixedUrls")) && rb.isUserAdmin())
                {
                    if(DocTools.isXssStrictUrlException(rb.getUrl(), "domainIdFixedUrls") ||
                            ("/admin/importxls.do".equalsIgnoreCase(rb.getUrl()) && "type=sk.iway.iwcm.users.ImportUsersXLS".equals(rb.getQueryString())))
                    {
                        //domainIdCommon sa pouziva v podmienke v getDomainIdSqlWhere, standardne to nie je nastavene,
                        //moze sa zapnut na 1 alebo inu hodnotu ak chceme mat ine spolocne ID domeny, vtedy nenastavime na 1 ale na danu hodnotu
                        int domainIdFixed = Constants.getInt("domainIdCommon");
                        if (domainIdFixed<1) domainIdFixed = 1;

                        return domainIdFixed;
                    }

                }

                domain = rb.getDomain();
            }
            if (Tools.isNotEmpty(domain))
            {
                domainId = GroupsDB.getDomainId(domain);
            }

            //pre neexistujuce domeny vratime default domenu pri multidomain WJ
            if (domainId == -1 && Constants.getBoolean("enableStaticFilesExternalDir")==true) domainId = 1;
        }
        return domainId;
    }

    /**
     * Vrati prefix a  ".domain_id=[cislo]" pre Cloud WebJet. Bodku pred domain_id prida samo.
     * MBO: ak je vypnuty cloud, zameni vsetky klauzuly domain_id=? za 1=1
     * @param addAnd  ak je true prida " AND " pred domain_id=[cislo]
     * @param tblPrefix tablukovy prefix pred .domain_id
     * @return " AND prefix.domain_id=[cislo] " resp. " prefix.domain_id=[cislo] "
     */
    public static String getDomainIdSqlWhere(boolean addAnd, String tblPrefix)
    {
        // MBO: musi vracat platnu cast klauzuly WHERE alebo "1", pretoze v niektorych SQL kodoch sa kvoli tomu klauzula WHERE pridavala a bez toho by nedavala zmysel
        if (InitServlet.isTypeCloud())
        {
            if (addAnd)
            {
                return " AND "+(Tools.isNotEmpty(tblPrefix) ? tblPrefix+"." : "")+"domain_id="+ getDomainId()+" ";
            }
            else
            {
                return " "+(Tools.isNotEmpty(tblPrefix) ? tblPrefix+"." : "")+"domain_id="+ getDomainId()+" ";
            }
        }
        else if (Constants.getBoolean("enableStaticFilesExternalDir")==true)
        {
            String orAppend = "";
            if (Constants.getInt("domainIdCommon")>0) {
                orAppend = " OR "+(Tools.isNotEmpty(tblPrefix) ? tblPrefix+"." : "")+"domain_id="+Constants.getInt("domainIdCommon");
            }
            if (addAnd)
            {
                return " AND ("+(Tools.isNotEmpty(tblPrefix) ? tblPrefix+"." : "")+"domain_id="+ getDomainId()+orAppend+") ";
            }
            else
            {
                return " ("+(Tools.isNotEmpty(tblPrefix) ? tblPrefix+"." : "")+"domain_id="+ getDomainId()+orAppend+") ";
            }
        }
        else
        {
            if (addAnd)
            {
                return " "; //toto je zbytocne, netreba dat ziadnu podmienku: AND 1=1
            }
            else
            {
                return " 1=1 ";
            }
        }
    }

    /**
     * Vrati retazec domain_id=[cislo] pre Cloud WebJet
     * @param addAnd ak je true prida " AND " pred retazec
     * @return " AND domain_id=[cislo] " resp. " domain_id=[cislo] "
     */
    public static String getDomainIdSqlWhere(boolean addAnd)
    {
        return getDomainIdSqlWhere(addAnd, "");
    }




    /**
     * Odfiltruje s ciarkou oddeleneho zoznamu ID adresarov, ktore nepatria aktualnej domene
     * @param editableGroups
     * @return
     */
    private static String filterGroupIds(String editableGroups)
    {
        try
        {
            if (Tools.isNotEmpty(editableGroups))
            {
                GroupsDB groupsDB = GroupsDB.getInstance();

                int[] groupIds = Tools.getTokensInt(editableGroups, ",");
                if (groupIds != null && groupIds.length > 0)
                {
                    StringBuilder filteredGroups = null;

                    for (int groupId : groupIds)
                    {
                        GroupDetails checkGroup = groupsDB.getGroup(groupId);
                        if (checkGroup != null && checkGroup.getDomainName().equalsIgnoreCase(CloudToolsForCore.getDomainName()))
                        {
                            if (filteredGroups == null) filteredGroups = new StringBuilder(String.valueOf(groupId));
                            else filteredGroups.append(","+groupId);
                        }
                    }

                    if (filteredGroups != null) return filteredGroups.toString();
                }
            }
        }
        catch (Exception e)
        {
            sk.iway.iwcm.Logger.error(e);
        }
        return "";
    }

    /**
     * Odfiltruje so zoznamu adresarov len tie, ktore zacinaju na /images/ alebo /files/
     * @param writableFolders
     * @return
     */
    private static String filterWritableFolders(String writableFolders)
    {
        try
        {
            if (Tools.isNotEmpty(writableFolders))
            {
                String domainAlias = MultiDomainFilter.getDomainAlias(CloudToolsForCore.getDomainName());

                String[] folders = Tools.getTokens(writableFolders, "\n");
                if (folders != null && folders.length > 0)
                {
                    StringBuilder filteredFolders = null;

                    for (String folder : folders)
                    {
                        if (FileBrowserTools.hasForbiddenSymbol(folder)) continue;

                        boolean addFolder = false;
                        if (folder.startsWith("/images/") || folder.startsWith("/files/")) addFolder = true;

                        if ("cloud".equals(Constants.getInstallName())==false && Tools.isNotEmpty(domainAlias))
                        {
                            //nie sme cloud ale multiweb, povol aj podla domain aliasu
                            if (folder.indexOf("/"+domainAlias+"/")!=-1) addFolder = true;
                        }

                        if (addFolder)
                        {
                            if (filteredFolders == null) filteredFolders = new StringBuilder(folder);
                            else filteredFolders.append("\n").append(folder);
                        }
                    }

                    if (filteredFolders != null) return filteredFolders.toString();
                }
            }
        }
        catch (Exception e)
        {
            sk.iway.iwcm.Logger.error(e);
        }
        return "";
    }

    /**
     * Nastavi prava podla domeny, je to tu kvoli tomu, ze to pouziva aj MultiWeb
     * @param user
     * @param rb
     */
    public static void setPermissions(Identity user, RequestBean rb)
    {
        user.setEditablePages("");
        if ("cloud".equals(Constants.getInstallName()))
        {
            user.setEditableGroups("");
        }
        else if (Tools.isNotEmpty(user.getEditableGroups()))
        {
            user.setEditableGroups(filterGroupIds(user.getEditableGroups()));
        }

        String defaultFolders = "/images/*\n/files/*";

        user.setWritableFolders(filterWritableFolders(user.getWritableFolders()));
        if (Tools.isEmpty(user.getWritableFolders()))
        {

            String domainAlias = MultiDomainFilter.getDomainAlias(CloudToolsForCore.getDomainName());
            if ("cloud".equals(Constants.getInstallName())==false && Tools.isNotEmpty(domainAlias))
            {
                //pre multiweb pridame tieto adresare ak ma domena domainAlias
                defaultFolders += "\n/components/"+domainAlias+"/*\n/templates/"+domainAlias+"/*";
            }

            user.setWritableFolders(defaultFolders);
        }

        //vytvor adresare (ak neexistuju)
        IwcmFile f = new IwcmFile(Tools.getRealPath("/images/"));
        if (f.exists()==false) f.mkdirs();

        f = new IwcmFile(Tools.getRealPath("/files/"));
        if (f.exists()==false) f.mkdirs();

        //nastav prava na moduly
        if ("cloud".equals(Constants.getInstallName()))
        {
            //nastav disabled items
            Map<String, String> disabledItemsTable = new Hashtable<>();
            user.setDisabledItemsTable(disabledItemsTable);
            user.addDisabledItem("menuSync");

            user.addDisabledItem("menuConfig");
            user.addDisabledItem("modUpdate");
            user.addDisabledItem("cmp_adminlog");
            user.addDisabledItem("edit_text");
            user.addDisabledItem("cmp_data_deleting");
            user.addDisabledItem("cmp_server_monitoring");
            user.addDisabledItem("cmp_redirects");
            user.addDisabledItem("modRestart");
            user.addDisabledItem("cmp_crontab");
            user.addDisabledItem("make_zip_archive");
            user.addDisabledItem("cmp_attributes");
            user.addDisabledItem("export_offline");
            user.addDisabledItem("cmp_clone_structure");

            user.addDisabledItem("menuGDPRregexp");
            user.addDisabledItem("menuGDPRDelete");

            user.addDisabledItem("menu.users");
            user.addDisabledItem("user.admin.userGroups");
            user.addDisabledItem("users.perm_groups");
            user.addDisabledItem("menu.fbrowser");
            user.addDisabledItem("menu.templates");
        }
        else
        {
            if (isControllerDomain())
            {
                //prvemu hostu a iway || webactive user povolime niektore systemove moduly
            }
            else
            {
                user.addDisabledItem("menuConfig");
                user.addDisabledItem("modUpdate");
                user.addDisabledItem("cmp_adminlog");
                user.addDisabledItem("edit_text");
                user.addDisabledItem("cmp_data_deleting");
                user.addDisabledItem("cmp_server_monitoring");
                //uz je povolene user.addDisabledItem("cmp_redirects");
                user.addDisabledItem("modRestart");
                user.addDisabledItem("cmp_crontab");

                user.addDisabledItem("menuGDPRregexp");
                user.addDisabledItem("menuGDPRDelete");

                //domain limits allow only for first domain
                user.addDisabledItem("cmp_dmail_domainlimits");
                user.addDisabledItem("cmp_response-header");
                user.addDisabledItem("cmp_adminlog_logging");
                user.addDisabledItem("cmp_in-memory-logging");

                user.addDisabledItem("user.admin.userGroups");
                user.addDisabledItem("users.perm_groups");
            }

            user.addDisabledItem("make_zip_archive");
            user.addDisabledItem("cmp_attributes");
            user.addDisabledItem("export_offline");
            user.addDisabledItem("cmp_clone_structure");
            user.addDisabledItem("cmp_stat_seeallgroups");

            String specialPerms = Constants.getString("multiwebSpecialPerms-"+user.getUserId());
            if (Tools.isNotEmpty(specialPerms))
            {
                for (String special : Tools.getTokens(specialPerms, ","))
                {
                    user.removeDisabledItem(special);
                }
            }

            UsersDB.loadDisabledItemsFromDB(user);
        }
    }

    /**
     * In MultiWeb returns true for the first domain (controller) with more permissions
     * @return
     */
    public static boolean isControllerDomain() {
        if (InitServlet.isTypeCloud() == false) return false;
        return CloudToolsForCore.getDomainId() == 1;
    }

    /**
     * Ziska korenove ID adresara aktualne nastavenej domeny
     * @param request
     * @return
     */
    public static int getRootGroupId(HttpServletRequest request)
	{
		if (InitServlet.isTypeCloud()) return CloudToolsForCore.getDomainId();
		//aby nam to fungovalo aj na sablonovom WebJETe
		return GroupsDB.getDomainId(DocDB.getDomain(request));
	}

    /**
	 * Vrati hodnotu z nastaveni (vid. CustomHeaderFooterAction)
	 * @param param
	 * @return
	 */
	public static String getValue(String param)
	{
		int rootGroupId = CloudToolsForCore.getDomainId();
		if (rootGroupId < 1) return null;

		GroupDetails rootGroup = GroupsDB.getInstance().getGroup(rootGroupId);
		if (rootGroup == null) return null;

		return getValue(rootGroup.getFieldC(), param);
	}

	/**
	 * Z dat nastaveni vrati danu hodnotu (vid. CustomHeaderFooterAction)
	 * @param data
	 * @param param
	 * @return
	 */
	public static String getValue(String data, String param)
	{
		return StringUtils.substringBetween(data, param+"=", ";");
	}

    /**
	 * Test ci je mozne, danu web stranku menit, vrati null ak ano, inak vrati originalny doc details (pred zmenou)
	 * @param docId
	 * @return
	 */
	public static DocDetails isPossibleToChangeDoc(int docId)
	{
		if ("cloud".equals(Constants.getInstallName()) && docId>0)
		{
			GroupsDB groupsDB = GroupsDB.getInstance();
			DocDB docDB = DocDB.getInstance();

			DocDetails existing = docDB.getBasicDocDetails(docId, false);
			if (existing != null &&  ( existing.getTitle().equals("Header") || existing.getTitle().equals("Footer") ))
			{
				GroupDetails existingParentGroup = groupsDB.getGroup(existing.getGroupId());
				if (existingParentGroup != null && existingParentGroup.getGroupName().equals("System"))
				{
					//nemozeme menit title ani groupid
					return existing;
				}
			}
		}
		return null;
	}

    /**
	 * Vrati true ak je IP adresa HTTP poziadavky povazovanu za internu
	 *
	 * @param request
	 * @return
	 */
	public static boolean isInternalIp(HttpServletRequest request)
	{
		String remoteIp = Tools.getRemoteIP(request);
		StringTokenizer ips = new StringTokenizer(Constants.getString("cloudCloneAllowedIps"),",");
		while(ips.hasMoreTokens())
		{
			if(remoteIp.startsWith(ips.nextToken()))
			{
				return true;
			}
		}
		return false;
	}

    /**
	 * Vrati pravdepodobneho administratora pre domenu (admina s najnizsim ID)
	 * samotne odfiltrovanie adminov len pre danu domenu je uz v UsersDB takze
	 * tu sa len vyberie ten s najnizsim id.
	 *
	 * @return UserDetails
	 */
	public static UserDetails getAdmin()
	{
		List<UserDetails> admins = UsersDB.getAdmins();
		if (admins!=null)
		{
			if (admins.size()>0)
			{
				//return UsersDB.getUser(min(admins, on(UserDetails.class).getUserId()));
                Optional<UserDetails> admin = admins.stream().min(Comparator.comparing(UserDetails::getUserId));
                if (admin.isPresent()) return admin.get();
			}
		}
		return null;
	}

    /**
	 * Prelozi v texte vsetky kluce zacinajuce na cloud.template. (CLOUD_TEXT_KEY_PREFIX) ktore najde
	 * vrati text s nahradenymi klucami
	 * @param lng jazyk do ktoreho sa to ma prelozit
	 * @param text
	 * @return povodny text s nahradenymi klucami v jazyku predanom ako parameter
	 */
	public static String translate(String lng, String text)
	{
		String htmlCode = text;
		Prop prop = Prop.getInstance(lng);

		if(text.indexOf(CLOUD_TEXT_KEY_PREFIX) == -1)
		{
			StringBuilder sb = new StringBuilder();

			int startIdx = 0;
			//lepsie ako vymenovat vsetky mozne konce kluca, radsej dame len povolene znaky v kluci a akykolvek iny ho ukonci
			Matcher matcher = CLOUD_TEXT_PATTERN.matcher(text);
			//hladame od konca posledneho najdeneho kluca
			while(matcher.find(startIdx))
			{
				//v group mam key
				sb.append(text.substring(startIdx, matcher.start()));
				sb.append(prop.getText(matcher.group()));
				startIdx = matcher.end();
			}
			//doplnime zvysok
			if(startIdx < text.length())
				sb.append(text.substring(startIdx, text.length()));

			htmlCode = sb.toString();
		}

		if ("en".equals(lng) || Tools.isEmpty(lng)) return text;

		return translateText(htmlCode, prop);
	}

	/**
	 * Prelozi cloudove kluce v editor forme v title, data
	 * @param lng jazyk do ktoreho sa ma prekladat
	 * @param ef editorform
	 * @return EditorForm s prelozenymi textami pre cloudove kluce
	 */
	public static EditorForm translate(String lng, EditorForm ef, GroupDetails localGroup)
	{
		if(ef == null) throw new IllegalArgumentException("EditorForm cant be null");
		if (localGroup.getFullPath().indexOf("/System")==-1) ef.setTitle(translate(lng, ef.getTitle()));
		ef.setData(translate(lng, ef.getData()));
		if(Tools.isNotEmpty(ef.getVirtualPath()) && !ef.getVirtualPath().equals("/"))
		{
			//path sa nam potom vytvori podla nazvu stranky uz v danom jazyku... eh, teda snad :)
			ef.setVirtualPath("");
		}
		return ef;
	}

    /**
	 * Prelozi texty v HTML kode - najskor extrahuje textove bloky a tie nasledne prelozi
	 * @param htmlCode
	 * @param prop
	 * @return
	 */
	private static String translateText(String htmlCode, Prop prop)
	{
		//DebugTimer dt = new DebugTimer("CloudToolsForCore.translateText");

		Map<String, String> textyToAdd = new Hashtable<>();
		extractTexts(htmlCode, textyToAdd, null);

		//dt.diff("Extracted");

		Set<Map.Entry<String, String>> entrySet = textyToAdd.entrySet();
		for (Entry<String, String> entry : entrySet)
		{
			String key = entry.getKey();
			String translated = prop.getText(entry.getKey());

			if (key.equals(translated)==false)
			{
				Logger.debug(CloudToolsForCore.class, "Translating, key="+key+" text="+translated+" value="+entry.getValue());
				htmlCode = Tools.replace(htmlCode, entry.getValue(), translated);
			}
		}

		//quickfix
		htmlCode = Tools.replace(htmlCode, "inlineComponentTlačítko", "inlineComponentButton");
		htmlCode = Tools.replace(htmlCode, "showKontakts", "showContacts");

		//dt.diff("done");

		return htmlCode;
	}

    /**
	 * Pouziva sa na extrakciu textov z HTML kodu (najde len textove bloky)
	 * @param htmlCode
	 * @param textyToAdd
	 * @param prop
	 */
	public static void extractTexts(String htmlCode, Map<String, String> textyToAdd, Prop prop)
	{
		HTMLTokenizer htmlTokenizer = new HTMLTokenizer(Tools.replace(htmlCode, "/>", ">").toCharArray());
		@SuppressWarnings("unchecked")
		Enumeration<Object> e = htmlTokenizer.getTokens();
		Object o;

		boolean wasHtmlText = false;

		while (e.hasMoreElements())
		{
			wasHtmlText = true;

			o = e.nextElement();
			if (o instanceof TagToken)
			{
				//tagToken = (TagToken) o;
				//System.out.println("TAG="+tagToken.getName()+" "+tagToken.isEndTag());
			}
			else
			{
				String text = o.toString();
				extractTextAddToList(text, textyToAdd, prop);
			}
		}

		if (wasHtmlText == false)
		{
			//nebol to HTML content ale len napr. nadpis
			extractTextAddToList(htmlCode, textyToAdd, prop);
		}
	}

    /**
	 * Prida najdeny text do listu na dalsie pouzitie
	 * @param text
	 * @param textyToAdd
	 * @param prop
	 */
	private static void extractTextAddToList(String text, Map<String, String> textyToAdd, Prop prop)
	{
		if (Tools.isNotEmpty(text) && text.length()>2 && text.indexOf("!INCLUDE")==-1 && text.startsWith("@import")==false)
		{
			String key = getTextKey(text);
			//Logger.debug(CloudToolsForCore.class, "Text="+text+" key="+key);
			if ((prop==null || prop.getText(key).equals(key)) && textyToAdd.containsKey(key)==false)
			{
				//Logger.debug(CloudToolsForCore.class, "++++adding: "+key+"="+text);
				textyToAdd.put(key, text);
			}
		}
	}

    private static String getTextKey(String text)
	{
		text = DocTools.removeCharsDir(text, false);
		text = text.replace('/', '-');
		text = text.trim();

		return "<cloud.template."+text+">";
	}

    /**
	 * Vrati ci je pouzivany modul basket, cachuje vysledok 5 minut
	 * @param request
	 * @return
	 */
	public static boolean hasShop(HttpServletRequest request)
	{
		int rootGroupId = CloudToolsForCore.getRootGroupId(request);
		String CACHE_KEY = "sk.iway.cloud.hasShop-"+rootGroupId;
		Cache c = Cache.getInstance();
		Boolean hasShop = (Boolean)c.getObject(CACHE_KEY);
		if (hasShop != null) return hasShop.booleanValue();

		//vyhladajme stranky pre nas web v ktorom sa nachadza include products.jsp
		String sqlQuery = "SELECT count(doc_id) FROM documents WHERE doc_id>0 AND root_group_l1 IN ("+GroupsDB.getInstance().expandRootGroupL1(rootGroupId)+") AND (data LIKE '%/components/basket/%' OR data LIKE '%/components/eshop/%')";
		//System.out.println("---------------> " + CloudToolsForCore.getRootGroupId(request));
		//System.out.println(sqlQuery);
		int records = (new SimpleQuery()).forInt(sqlQuery);
		if (records > 0) hasShop = Boolean.TRUE;
		else hasShop = Boolean.FALSE;

		c.setObjectSeconds(CACHE_KEY, hasShop, 5*60, true);

		return hasShop.booleanValue();
	}

    /**
	 * Vrati true ak zadane groupId patri k mojej domene
	 * @param groupId
	 * @return
	 */
	public static boolean isGroupFromMyDomain(int groupId)
	{
		GroupsDB groupsDB = GroupsDB.getInstance();
		GroupDetails group = groupsDB.getGroup(groupId);
		if (group != null && group.getDomainName().equals(CloudToolsForCore.getDomainName())) return true;

		return false;
	}

    /**
	 *  Vrati meno aktualneho Template-u pre Cloud
	 * @param request request
	 * @return String - meno template-u, ak nie sme v Cloude vrati "unknown"
	 */
	public static String getRootTempName(HttpServletRequest request)
	{
		TemplateDetails rootTemp = getRootTemp(request);
		//if (rootTemp != null) return rootTemp.getTempName();
		if (rootTemp != null)
		{
			if(rootTemp.getTempName().lastIndexOf("-") < rootTemp.getTempName().length() && rootTemp.getTempName().lastIndexOf("-") > 0)
				return rootTemp.getTempName().substring(0,rootTemp.getTempName().lastIndexOf("-"));
			return rootTemp.getTempName();
		}

		Logger.debug(CloudToolsForCore.class, "You are not in Cloud, returning \"unknown\" name of template ");
		return "unknown";
	}

    /**
	 * Vrati root sablonu
	 * @param request
	 * @return
	 */
	public static TemplateDetails getRootTemp(HttpServletRequest request)
	{
		int rootGroupId = CloudToolsForCore.getRootGroupId(request);
		GroupDetails rootGroup = GroupsDB.getInstance().getGroup(rootGroupId);
		if (rootGroup != null)
		{
			TemplateDetails rootTemp = TemplatesDB.getInstance().getTemplate(rootGroup.getTempId());
			return rootTemp;
		}
		return null;
	}

    /**
	 * Vrati meno JSP suboru sablony (pouziva sa na podmenene spravanie niecoho podla sablony)
	 * @param request
	 * @return
	 */
	public static String getRootTempJsp(HttpServletRequest request)
	{
		String templateJsp = "";
		TemplateDetails rootTemp = CloudToolsForCore.getRootTemp(request);
		if (rootTemp != null && rootTemp.getForward()!=null) templateJsp = rootTemp.getForward();

		return templateJsp;
	}

    /**
	 * Vrati true ak sa jedna o bootstrap sablonu
	 * @param request
	 * @return
	 */
	public static boolean isBootstrap(HttpServletRequest request)
	{
		String templateJsp = getRootTempJsp(request);

		if (templateJsp.indexOf("bootstrap")!=-1 || templateJsp.indexOf("ublocks")!=-1) return true;

		return false;
	}

    /**
     * Vrati email adresu pre posielanie poziadaviek na support
     * @return
     */
    public static String getSupportEmail()
	{
		return Constants.getString("cloudSupportEmail");
	}

    /**
     * Returns ID of root groups for current domain, eg. 10,15
     * @return
     */
    public static String getRootGroupIds() {
        List<GroupDetails> rootGroups = GroupsDB.getRootGroups();
        StringBuilder sb = new StringBuilder();
        String domain = CloudToolsForCore.getDomainName();
        for (GroupDetails group : rootGroups) {
            if (domain.equals(group.getDomainName())) {
                if (sb.isEmpty()==false) sb.append(",");
                sb.append(group.getGroupId());
            }
        }
        //safety check
        if (sb.isEmpty()) sb.append(Integer.MAX_VALUE);
        return sb.toString();
    }

    /**
     * In MultiWeb for null/-1 groupId returns domainId
     * @param rootGroupId
     * @return
     */
    public static Integer fixRootGroupId(Integer rootGroupId) {
        if (InitServlet.isTypeCloud()) {
            if (rootGroupId==null || rootGroupId<1) {
                return CloudToolsForCore.getDomainId();
            } else {
                GroupDetails group = GroupsDB.getInstance().getGroup(rootGroupId);
                if (group==null || group.getDomainName().equals(CloudToolsForCore.getDomainName())==false) {
                    return CloudToolsForCore.getDomainId();
                }
            }
        }
        return rootGroupId;
    }

    /**
     * Returns domain name by its alias
     * If domain name is not found, returns null.
     * @param domainAlias
     * @return
     */
    public static String getDomainByAlias(String domainAlias) {
        if(Tools.isEmpty(domainAlias)) return null;

        for(String domain : GroupsDB.getInstance().getAllDomainsList()) {
            String foundAlias =  MultiDomainFilter.getDomainAlias(domain);
            if(domainAlias.equals(foundAlias)) return domain;
        }

        //We did not found match
        return null;
    }

    /**
     * Returns domain ID by its alias.
     * If domain name is not found, returns default domain ID using getDomainId().
     * @param domainAlias
     * @return
     */
    public static int getDomainIdByAlias(String domainAlias) {
        int domainId = 1;
        if(InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir")==true) {
            domainId = GroupsDB.getDomainId( getDomainByAlias(domainAlias) );
            if (domainId == -1) domainId = getDomainId();
        }
        return domainId;
    }
}

