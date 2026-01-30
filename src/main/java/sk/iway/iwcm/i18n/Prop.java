package sk.iway.iwcm.i18n;

import org.apache.commons.lang3.ArrayUtils;
import sk.iway.iwcm.*;
import sk.iway.iwcm.components.translation_keys.jpa.MissingKeysDto;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.system.multidomain.MultiDomainFilter;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  Description of the Class
 *
 *@Title        magma-web
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.8 $
 *@created      Streda, 2002, j�l 17
 *@modified     $Date: 2004/03/18 15:44:59 $
 */
public class Prop
{
	public static final boolean DEFAULT_TEXTS = true;

	public static final boolean CURRENT_TEXTS = false;

	/**
	 *  Description of the Field
	 */
	private static final String CONTEXT_I18N_PROP_HASHTABLE = "context_i18n_prop_hashtable";
	/**
	 *  Description of the Field
	 */
	public static final String SESSION_I18N_PROP_LNG = "session_i18n_prop_lng";

	private IwayProperties properties;

	//pre interne ucely instancie, pretoze po vytvoreni instancie tato uz nemala informaciu o jazyku !!
	private final String language;

	private static ConcurrentHashMap<String, Set<MissingKeysDto>> missingTexts = new ConcurrentHashMap<>();

	private static long lastUpdate;

	private long created = System.currentTimeMillis();

	private Prop(IwayProperties properties, String lng)
	{
		this.properties = properties;
		language = lng;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Prop> getPropHashtabl(ServletContext context)
	{
		Map<String, Prop> propHashtable = null;
		if (context != null) propHashtable = (HashMap<String, Prop>)context.getAttribute(CONTEXT_I18N_PROP_HASHTABLE);
		if (propHashtable == null)
		{
			propHashtable = new HashMap<>();
			if (context!=null) context.setAttribute(CONTEXT_I18N_PROP_HASHTABLE, propHashtable);
		}
		return propHashtable;
	}

	private static Prop getPropFromHashtable(String lng)
	{
		return getPropFromHashtable(lng, Constants.getServletContext());
	}

	private static Prop getPropFromHashtable(String lng, ServletContext servletContext)
	{
		Map<String, Prop> propHashtable = getPropHashtabl(servletContext);
		if (Tools.isEmpty(lng))
		{
			lng = "sk";
		}
		Prop p = propHashtable.get(lng);
		if (p == null)
		{
			if ("ru".equals(lng))
			{
				p = new Prop(new IwayProperties("utf-8"),lng);
			}
			else if ("sk".equals(lng))
			{
				p = new Prop(new IwayProperties("utf-8"),lng);
			}
			else
			{
				p = new Prop(new IwayProperties(),lng);
			}
			propHashtable.put(lng, p);
		}
		return(p);
	}

	/**
	 * @Deprecated neviem na co toto sluzi, nikde sa nepouziva
	 */
	public Prop(ServletContext servletContext, boolean defaults, String lng)
	{
		Logger.println(this,"reading resources ["+Constants.getInstallName()+"]");

		properties = new IwayProperties();

		loadLanguageProp("sk", toFileNamePart("sk"), !defaults, servletContext);
		language = lng;
		ClusterDB.addRefresh(Prop.class);
	}

	private static String toFileNamePart(String lng)
	{
		if (Tools.isEmpty(lng)) lng="sk";
		return "sk".equals(lng) ? "" : "_"+lng;
	}

	private synchronized void loadLanguageProp(String languageKey, String fileLng, boolean loadCustomizedTexts, ServletContext servletContext)
	{
		if (Tools.isEmpty(languageKey)) languageKey = "sk";

		if (getPropFromHashtable(languageKey, servletContext).getProperties().isEmpty()==false)
			return;
		try
		{
			properties = getDefaulProperties(languageKey, fileLng, servletContext);
			if (loadCustomizedTexts && InitServlet.isWebjetConfigured())
			{
				new ComplexQuery().
						setSql("SELECT * FROM " + ConfDB.PROPERTIES_TABLE_NAME + " WHERE lng = ? ORDER BY update_date ASC").
						setParams(languageKey).
						list(new Mapper<Void>(){
							@Override
							public Void map(ResultSet rs) throws SQLException{
								String key = DB.getDbString(rs, "prop_key");
								String value = DB.getDbString(rs, "prop_value");
								if (key != null) key = key.trim();
								properties.setProperty(key, value);
								return null;
							}
						});
			}
		}
		catch (Exception e)
		{
			Logger.error(Prop.class, e.getMessage());
		}
	}


	public static Prop getInstance(HttpServletRequest request)
	{
		return(getInstance(Constants.getServletContext(), request));
	}

	/**
	 *  Gets the text attribute of the Prop class
	 *
	 *@param  servletContext  Description of the Parameter
	 *@param  request         Description of the Parameter
	 *@return                 The text value
	 */
	public static Prop getInstance(ServletContext servletContext, HttpServletRequest request)
	{
		String lng = getLng(request, false);
		boolean refresh = false;

		return(getInstance(servletContext, lng, refresh));
	}

	public static Prop getInstance()
	{
		String lng = getLng(null);
		return getInstance(Constants.getServletContext(), lng, false);
	}

	public static String getLng(HttpSession session)
	{
		if (session == null) {
			RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
			String lng = Constants.getString("defaultLanguage");
			if (rb != null) lng = rb.getLng();
			return lng;
		}

		String lng = (String)Tools.sessionGetAttribute(session, SESSION_I18N_PROP_LNG);
		if (lng==null)
			lng = (String)Tools.sessionGetAttribute(session, "lng");
		if (lng == null)
			lng = Constants.getString("defaultLanguage");
		return lng;
	}

	public static String getLng(HttpServletRequest request, boolean noParameter)
	{
		if (request == null) return Constants.getString("defaultLanguage");
		HttpSession session = request.getSession();
		String lng = null;
		if (noParameter == false) lng = Tools.getParameter(request, "language");
		else if (request.getParameter("__lng")!=null)
		{
			lng = Tools.getParameter(request, "__lng");
		}
		else if (request.getParameter("lng")!=null)
		{
			lng = Tools.getParameter(request, "lng");
		}
		if (lng == null)
		{
			//PageLng
			lng = (String)request.getAttribute("PageLng");
			//Logger.debug(Prop.class, "getInstance1 lng="+lng);
		}
		if (lng==null)
		{
			//najskor musi ist test pre admina, aby sa nemenil jazyk admin casti
			lng = (String)Tools.sessionGetAttribute(session, SESSION_I18N_PROP_LNG);
			//Logger.debug(Prop.class, "getInstance3 lng="+lng);
		}
		if (lng==null)
		{
			//PageLng
			lng = (String)Tools.sessionGetAttribute(session, "lng");
			//Logger.debug(Prop.class, "getInstance2 lng="+lng);
		}
		if (lng == null)
		{
			lng = Constants.getString("defaultLanguage");
			//Logger.debug(Prop.class, "getInstance4 lng="+lng);
		}
		if (request.getAttribute("PageLng")==null)
		{
			if (Tools.sessionGetAttribute(session, SESSION_I18N_PROP_LNG)==null) Tools.sessionSetAttribute(session, SESSION_I18N_PROP_LNG, lng);
		}
		return lng;
	}

	/**
	 * Vrati jazyk pre JS subory pre ckeditor, elfinder a podobne kde sa napr. pre CZ pouziva hodnota CS
	 * @param request
	 * @return
	 */
	public static String getLngForJavascript(HttpServletRequest request)
	{
		String lng = (String)request.getSession().getAttribute(Prop.SESSION_I18N_PROP_LNG);
		if (lng == null) lng = PageLng.getUserLng(request);

		if ("cz".equals(lng)) lng = "cs";
		else if (Tools.isEmpty(lng)) lng = "sk";

		return lng;
	}

	public static Prop getInstance(ServletContext servletContext, HttpSession session)
	{
		String lng = (String)Tools.sessionGetAttribute(session, SESSION_I18N_PROP_LNG);
		if (lng==null)
		{
			//PageLng
			lng = (String)Tools.sessionGetAttribute(session, "lng");
		}
		if (lng == null)
		{
			lng = Constants.getString("defaultLanguage");
		}
		if (Tools.sessionGetAttribute(session, SESSION_I18N_PROP_LNG)==null) Tools.sessionSetAttribute(session, SESSION_I18N_PROP_LNG, lng);

		boolean refresh = false;

		return(getInstance(servletContext, lng, refresh));
	}

	public static Prop getInstance(String lng)
	{
		return(getInstance(Constants.getServletContext(), lng, false));
	}

	/**
	 * Obnovi instanciu Prop objektu ak je refresh nastavene na true
	 * @param refresh
	 * @return
	 */
	public static Prop getInstance(boolean refresh)
	{
		if (refresh)
		{
			Constants.getServletContext().removeAttribute(CONTEXT_I18N_PROP_HASHTABLE);
		}
		return(getInstance(Constants.getServletContext(), "sk", refresh));
	}

	public static Prop getInstance(ServletContext servletContext, String lng, boolean refresh)
	{
		if ("cs".equals(lng)) lng = "cz";

		//ziskaj properties subory
		Prop lngProp = getPropFromHashtable(lng, servletContext);

		//lazy load inych jazykov ako slovenskeho
		if (lngProp.getProperties().isEmpty() || refresh)
		{
			if (refresh)
			{
				lastUpdate = System.currentTimeMillis();
				lngProp.getProperties().clear();
			}
			lngProp.loadLanguageProp(lng, toFileNamePart(lng), DEFAULT_TEXTS, servletContext);
		}

		if (refresh)
		{
			Cache.getInstance().removeObjectStartsWithName("Prop.getDefaulProperties.");
			ClusterDB.addRefresh(Prop.class);
		}

		return(lngProp);
	}

	/**
	 *  Gets the text attribute of the Prop object
	 *
	 *@param  key     Description of the Parameter
	 *@return         The text value
	 */
	public String getText(String key)
	{
		if (properties == null)
		{
			return (key);
		}
		if (Tools.isEmpty(key)) return "";

		String value = properties.getProperty(key, key);

		//hladanie kluca podla prefixu (skupiny sablon, id sablony)
		List<String> prefixes = RequestBean.getTextKeyPrefixes();
		value = checkPrefixedValues(prefixes, key, value);

		//vyhladaj kluc podla domenoveho prefixu (podobne ako konf. premennu)
		value = checkDomainAlias(key, value);

		if (value.equals(key) && key.endsWith(".tooltip")==false && key.startsWith("displaytag.")==false && key.startsWith("[[#{")==false &&
			key.startsWith("components.translation_key.")==false && "&nbsp;".equals(key)==false && "ID".equals(key)==false)
		{
			//chybajuci string, pridaj ho do chybajucich
			missingTexts.putIfAbsent(language, Collections.synchronizedSet(new HashSet<MissingKeysDto>()));

			String urlAddress = "";
			RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
			if (rb != null) urlAddress = rb.getUrl();

			//check if key was allready set as missing, if yes -> just update lastMissing date value
			boolean present = false;
			for(MissingKeysDto missingKey : missingTexts.get(language)) {
				if(missingKey.getKey().equals(key)) {
					present = true;
					missingKey.setLastMissing(new Date());
					if (Tools.isNotEmpty(urlAddress)) missingKey.setUrlAddress(urlAddress);
					break;
				}
			}

			//if key is missing first time, add new MissingKeysDto variable into set of missing keys
			if(!present) {
				missingTexts.get(language).add(new MissingKeysDto(key, new Date(), language, urlAddress));
			}

			//try defaultLanguage property
            String defaultLanguage = Constants.getString("defaultLanguage");
            if (Tools.isNotEmpty(defaultLanguage) && "sk".equalsIgnoreCase(defaultLanguage)==false)
            {
                IwayProperties defaultProp = getRes(defaultLanguage);
                if (defaultProp != null)
                {
                    value = checkPrefixedValues(prefixes, key, defaultProp.getProperty(key, key));
                }
            }

            if (value.equals(key))
            {
                //try default properties
                IwayProperties skProp = getRes("sk");
                if (skProp != null) {
                    value = checkPrefixedValues(prefixes, key, skProp.getProperty(key, key));
                }
            }
		}
		if (value.startsWith("docid:") && value.length()<20)
		{
			try
			{
				int docId = Tools.getIntValue(value.substring(value.indexOf(":")+1), -1);
				if (docId > 0)
				{
					DocDB docDB = DocDB.getInstance();
					DocDetails doc = docDB.getDoc(docId);
					if (doc != null && Tools.isNotEmpty(doc.getData()))
					{
						value = doc.getData();
					}
				}
			}
			catch (Exception ex)
			{
				Logger.error(Prop.class, ex);
			}
		}

        RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
		if (requestBean != null && requestBean.getParameter("showTextKeys") != null && requestBean.isUserAdmin()) {
			value = "[" + key + "] " + value;
		}

		return (value);
	}

    /**
     * Overi, ci existuje kluc s prefixom, vrati sa prvy co sa s prefixom najde (podla poradia v liste prefixov)
     * @param prefixes - zoznam kontrolovanych prefixov
     * @param key - textovy kluc
     * @param currentValue - aktualna hodnota textu (pred kontrolou na prefix, ak sa nenajde, vrati sa toto)
     * @return
     */
	private String checkPrefixedValues(List<String> prefixes, String key, String currentValue)
    {
        String value = currentValue;
        if (prefixes!=null)
        {
            String prefixedValue = null;
            for (String prefix : prefixes)
            {
                prefixedValue = properties.getProperty(prefix+"."+key, prefixedValue);
            }
            if (prefixedValue!=null) value = prefixedValue;
        }

        return value;
	 }


	private String checkDomainAlias(String key, String currentValue)
	{
		if (Constants.isConstantsAliasSearch()==false || key==null) return currentValue;

		RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
		if(requestBean!=null && Tools.isNotEmpty(requestBean.getDomain()))
		{
			String domainAlias = MultiDomainFilter.getDomainAlias(requestBean.getDomain());
			if (Tools.isEmpty(domainAlias))
			{
				domainAlias = requestBean.getDomain();
			}
			if(Tools.isNotEmpty(domainAlias))
			{
				String keyWithAlias=domainAlias+"-"+key;
				//ak taky zaznam s aliasom v hashtabulke existuje, tak sa vrati jeho meno, inak vrati defaultne meno
				String valueWithAlias = properties.getProperty(keyWithAlias, null);
				if (valueWithAlias != null && valueWithAlias.equals(keyWithAlias)==false) return valueWithAlias;
			}
		}
		return currentValue;
	}

	public String getText(String key, String param1)
	{
		return(getTextWithParams(key, param1));
	}

	public String getText(String key, String param1, String param2)
	{
		return(getTextWithParams(key, param1, param2));
	}

	public String getText(String key, String param1, String param2, String param3)
	{
		return(getTextWithParams(key, param1, param2, param3));
	}

	public String getText(String key, String param1, String param2, String param3, String param4)
	{
		return(getTextWithParams(key, param1, param2, param3, param4));
	}

	public String getText(String key, String param1, String param2, String param3, String param4, String param5)
	{
		return(getTextWithParams(key, param1, param2, param3, param4, param5));
	}

	public String getText(String key, String param1, String param2, String param3, String param4, String param5, String param6)
	{
		return(getTextWithParams(key, param1, param2, param3, param4, param5, param6));
	}

	public String getText(String key, String param1, String param2, String param3, String param4, String param5, String param6, String param7)
	{
		return(getTextWithParams(key, param1, param2, param3, param4, param5, param6,param7));
	}

	public String getTextWithParams(String key, String... params)
	{
		String value = getText(key);
		int i = 1;
		for (String param : params)
		{
			value = Tools.replace(value, "!"+i, param);
			value = Tools.replace(value, "{"+i+"}", param);
			i++;
		}
		while (i <= 7)
		{
			//replacni placeholdre, je to do 7 pretoze tak fungovala povodna verzia
			value = Tools.replace(value, "!"+i, "");
			value = Tools.replace(value, "{"+i+"}", "");
			i++;
		}
		return value;
	}

	public Map<String,String> getTextStartingWith(String prefix)
	{
		Map<String,String> result = new HashMap<>();
		for (Map.Entry<String,String> entry : properties.entrySet())
		{
			if (entry.getKey().startsWith(prefix)) result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	public static List<Map.Entry<String, String>> sortByValue(Map<String, String> texts)
	{
		List<Map.Entry<String, String>> list = setToList(texts.entrySet());

		/*Collections.sort(list, new Comparator<Map.Entry<String, String>>()
		{
			@Override
			public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2)
			{
				return o1.getValue().compareTo(o2.getValue());
			}
		});*/
		list.sort((Map.Entry<String, String> o1, Map.Entry<String, String> o2) -> o1.getValue().compareTo(o2.getValue()));

		return list;
	}

	public static List<Map.Entry<String, String>> sortByKey(Map<String, String> texts)
	{
		List<Map.Entry<String, String>> list = setToList(texts.entrySet());

		/*Collections.sort(list, new Comparator<Map.Entry<String, String>>()
		{
			@Override
			public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2)
			{
				return o1.getKey().compareTo(o2.getKey());
			}
		});*/
		list.sort((Map.Entry<String, String> o1, Map.Entry<String, String> o2) -> o1.getKey().compareTo(o2.getKey()));

		return list;
	}

	public String getTextWithSuffix(String key, String suffix){
		String suffixKey = key+"."+suffix;
		String text = suffixKey;

		String textWithSuffix = getText(suffixKey);
		String textWithoutSuffix = getText(key);

		if (Tools.isNotEmpty(textWithSuffix) && !suffixKey.equals(textWithSuffix)){
		    text = textWithSuffix;
        }else if(Tools.isNotEmpty(textWithoutSuffix) && !key.equals(textWithoutSuffix)){
		    text = textWithoutSuffix;
        }

		return text;
	}

	private static List<Map.Entry<String, String>> setToList(Set<Map.Entry<String, String>> set)
	{
		List<Map.Entry<String, String>> list = new ArrayList<>(set);

		return list;
	}

	/**
	 * Staticke ziskanie prekladoveho textu ked nemam request ani session, zvoli naposledy pouzity jazyk
	 * @param key
	 * @param params
	 * @return
	 */
	public static String getTxt(String key, String... params)
	{
		String lng = Constants.getString("defaultLanguage");
		RequestBean reqb = SetCharacterEncodingFilter.getCurrentRequestBean();
		if (reqb != null) lng = reqb.getLng();

		Prop prop = Prop.getInstance(lng);

		String value = prop.getText(key);

		if (params != null)
		{
			int counter = 1;
			for (String param : params)
			{
				value = Tools.replace(value, "!"+counter, param);
				value = Tools.replace(value, "{"+counter+"}", param);
			}
		}

		return value;
	}

	public String getTextProp(String key) {
		if (properties == null) {
			return null;
		}

		String value = properties.getProperty(key, null);

		return value;
	}

	public IwayProperties getProperties()
	{
		return properties;
	}

	public IwayProperties getRes(String lng)
	{
		Prop prop = getPropFromHashtable(lng);
		return prop.getProperties();
	}

	/**
	 * nacitanie len default textov pre jazyk
	 * @param languageKey
	 * @param fileLng
	 * @param servletContext
	 * @return
	 */
	public static IwayProperties getDefaulProperties(String languageKey, String fileLng, ServletContext servletContext)
	{
		//use cache, file should not change frequently
		Cache c = Cache.getInstance();
		String KEY = "Prop.getDefaulProperties."+languageKey+"."+fileLng;
		IwayProperties result = (IwayProperties) c.getObject(KEY);
		if (result!=null) return result;

		final IwayProperties defaultTexts = new IwayProperties("utf-8");
		if (Tools.isEmpty(languageKey)) languageKey = "sk";
		fileLng = (Tools.isEmpty(fileLng) || "sk".equals(fileLng)) ? "" : fileLng.startsWith("_") ? fileLng : "_"+fileLng;

		try
		{
			String fileName;
			fileName = "/text" + fileLng + ".properties";
			// toto loadujem aj z JARu
			IwcmFile f = new IwcmFile(Tools.getRealPath("/WEB-INF/classes"+fileName));
			if (f.exists())
			{
				Logger.println(Prop.class, " -> loading prop [" + languageKey + "]: " + fileName);
				defaultTexts.load(f);
			}
			String propInstallNames = Constants.getString("propInstallNames");
			if (Tools.isEmpty(propInstallNames))
				propInstallNames = Constants.getInstallName();

			//ak je nastavene, skus aj logInstallName
			if (Tools.isNotEmpty(Constants.getLogInstallName())) propInstallNames += ","+Constants.getLogInstallName();
			//aby sa nacitalo text-webjet9.properties
			propInstallNames = Constants.getString("defaultSkin")+","+propInstallNames;

			StringTokenizer st = new StringTokenizer(propInstallNames, ",");
			while (st.hasMoreTokens())
			{
				String installName = st.nextToken();
				// toto loadujem vzdy z classes adresara
				fileName = "/text" + fileLng + "-" + installName + ".properties";
				f = new IwcmFile(Tools.getRealPath("/WEB-INF/classes"+fileName));
				if (f.exists())
				{
					Logger.println(Prop.class, " -> loading prop [" + languageKey + "][" + installName + "]: " + fileName);
					defaultTexts.load(f);
				}
			}
		}
		catch (Exception e)
		{
			Logger.error(e);
		}

		c.setObjectSeconds(KEY, defaultTexts, 10*60, true);

		return defaultTexts;
	}

	/**
	 * Vrati texty zmenene pouzivatelom pre zadany jazyk.
	 * Vracia ich ako zoznam vo forme Kluc => hodnota.
	 *
	 * @param language
	 * @return
	 */
	public static IwayProperties getChangedProperties(String language){
		return getChangedProperties(language, null);
	}


	/**
	 * Vrati texty zmenene pouzivatelom pre zadany jazyk a prefix.
	 * Vracia ich ako zoznam vo forme Kluc => hodnota.
	 *
	 * @param language
	 * @param prefix
	 * @return
	 */
	public static IwayProperties getChangedProperties(String language, String prefix){
		String[] languages = Constants.getArray("languages");
		if (!ArrayUtils.contains(languages, language))
			throw new IllegalArgumentException("Unsupported language: ['"+language+"']");

		final IwayProperties changedTexts = new IwayProperties("utf-8");

		//nacitaj custom nazvy z databazy
		if(Tools.isEmpty(prefix))
		{
			new ComplexQuery().
				setSql("SELECT * FROM " +ConfDB.PROPERTIES_TABLE_NAME+" WHERE lng = ? ORDER BY update_date ASC").
				setParams(language).
				list(new Mapper<Void>(){
					@Override
					public Void map(ResultSet rs) throws SQLException
					{
						String key = DB.getDbString(rs, "prop_key");
						String value = DB.getDbString(rs, "prop_value");
						if (key != null) key = key.trim();
						changedTexts.setProperty(key, value);
						return null;
					}

				});
		}
		else
		{
			new ComplexQuery().
			setSql("SELECT * FROM " +ConfDB.PROPERTIES_TABLE_NAME+" WHERE lng = ? AND prop_key LIKE ? ORDER BY update_date ASC").
			setParams(language, prefix+"%").
			list(new Mapper<Void>(){
				@Override
				public Void map(ResultSet rs) throws SQLException
				{
					String key = DB.getDbString(rs, "prop_key");
					String value = DB.getDbString(rs, "prop_value");
					if (key != null) key = key.trim();
					changedTexts.setProperty(key, value);
					return null;
				}

			});
		}

		//vratime nase texty
		return changedTexts;
	}

	/**
	 * @return Returns the lastUpdate.
	 */
	public boolean shouldBeUpdated()
	{
		return lastUpdate > created;
	}


	/**
	 * Vrati true / false podla toho, ci ma nahrate properties objekty, alebo nie
	 * @return
	 */
	/*
	private boolean isEmpty()
	{
		if (properties == null) return false;

		return properties.isEmpty();
	}

	private void load(File f) throws IOException
	{
		if (properties == null) properties = new IwayProperties();
		properties.load(f);
	}

	private void setProperty(String key, String value)
	{
		if (properties == null) properties = new IwayProperties();
		properties.setProperty(key, value);
	}

	public String getProperty(String key)
	{
		return getProperty(key, key);
	}

	public String getProperty(String key, String defaultValue)
	{
		if (properties == null) return defaultValue;
		return properties.getProperty(key, defaultValue);
	}

	public Enumeration<String> propertyNames()
	{
		return properties.propertyNames();
	}
	*/

	/**
	 * returns set of missing texts for language param
	 * @param lng
	 * @return
	 */
	public static Set<MissingKeysDto> getMissingTexts(String lng) {
		if(missingTexts.containsKey(lng))
			return missingTexts.get(lng);

		return null;
	}

	/**
	 * returns set of missing texts for ALL language's
	 * @return
	 */
	public static Set<MissingKeysDto> getMissingTexts() {
		Set<MissingKeysDto> allMissingTexts = new HashSet<>();

		String[] lngArr = Constants.getArray("languages");
		for(String lng : lngArr)
			if(missingTexts.containsKey(lng))
				allMissingTexts.addAll(missingTexts.get(lng));

		return allMissingTexts;
	}

	/**
	 * Clears all missing texts
	 */
	public static void clearMissingTexts() {
		missingTexts.clear();
	}

	/**
	 * Delete missing text by key and language
	 * @param key
	 * @param lng
	 */
	public static void deleteMissingText(String key, String lng) {
		Set<MissingKeysDto> keysByLng = missingTexts.get(lng);
		for(MissingKeysDto dto : keysByLng) {
			if(dto.getKey().equals(key)) {
				keysByLng.remove(dto);
				break;
			}
		}
	}
}
