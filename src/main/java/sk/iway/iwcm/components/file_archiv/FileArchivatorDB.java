package sk.iway.iwcm.components.file_archiv;

import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.queries.ReadAllQuery;

import sk.iway.iwcm.*;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.database.nestedsets.Node;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.jpa.JpaTools;
import sk.iway.iwcm.system.jpa.JpaTools.Condition;
import sk.iway.iwcm.users.UserGroupDetails;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.iwcm.utils.Pair;

import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

/**
 *  FileArchivatorDB.java
 *
 *	DAO class for manipulating with FileArchivatorBean
 *
 * Title        webjet7
 * Company      Interway s.r.o. (www.interway.sk)
 * Copyright    Interway s.r.o. (c) 2001-2010
 * @author       $Author: prau $
 * @version      $Revision: 1.3 $
 * created      Date: 04.02.2015 15:51:22
 * modified     $Date: 2004/08/16 06:26:11 $
 */
public class FileArchivatorDB extends JpaDB<FileArchivatorBean>
{
	private static final FileArchivatorDB INSTANCE = new FileArchivatorDB();
	protected static String cachePrefix = "fileArchiv-";
	private static final String CONSTANTS_PREFIX = "fileArchiv";
	private static final String CACHE_TIME_KEY = "CacheTime";

	public static FileArchivatorDB getInstance()
	{
		return INSTANCE;
	}

	public FileArchivatorDB()
	{
		super(FileArchivatorBean.class);
	}

	public static List<FileArchivatorBean> getMainAndHistoryFiles(boolean cleanCache, boolean includeAwaiting) {
		List<FileArchivatorBean> mainFiles = getMainFileList(cleanCache, includeAwaiting);

		List<FileArchivatorBean> allFiles = new ArrayList<>();

		for (FileArchivatorBean file : mainFiles) {
			//Add main files
			allFiles.add(file);
			//Add history files of this main file
			List<FileArchivatorBean> historyFiles = getByReferenceId(file.getId());
			if (historyFiles != null && historyFiles.size() > 0) {
				if(includeAwaiting == true) allFiles.addAll(historyFiles);
				else allFiles.addAll( historyFiles.stream().filter(fab -> fab.getUploaded() == -1).toList() );
			}
		}

		return allFiles;
	}

	/** Vrati vysledky podla referencie z cache
	 *
	 * @param referenceId int
	 * @return List<FileArchivatorBean>
	 */
	public static List<FileArchivatorBean> getByReferenceId(Long referenceId)
	{
		String methodName = "getByReferenceId-";
		String cacheKey = cachePrefix+methodName+referenceId;
		Object o = Cache.getInstance().getObject(cacheKey);
		if(o != null) {
			@SuppressWarnings("unchecked")
			List<FileArchivatorBean> list = (List<FileArchivatorBean>) o;
			return list;
		}

		Logger.debug(FileArchivatorDB.class, "Nacitam z databazy: "+cacheKey);
		List<FileArchivatorBean> fabList = JpaTools.findByProperties(FileArchivatorBean.class, Pair.of("referenceId", referenceId));
		Cache.getInstance().setObject(cacheKey, fabList, getCacheTime(methodName));
		return fabList;
	}

	/** Pokusi sa najst v DB hlavne subory s rovnakym hashom. Pozor ! Vracia aj sam seba ;-)
	 *
	 * @param fab FileArchivatorBean
	 * @return List<FileArchivatorBean>
	 */
	public static List<FileArchivatorBean> getByHash(FileArchivatorBean fab)
	{
		return JpaTools.findByProperties(FileArchivatorBean.class, Pair.of("md5", fab.getMd5()), Pair.of("referenceId", -1), Pair.of("domainId", CloudToolsForCore.getDomainId()));
	}

	/** Vrati vsetky aktualne (hlavne) subory z cache + zohladni CloudToolsForCore.getDomainId() ak je zapnute
	 *
	 * @return List<FileArchivatorBean>
	 */
	public static List<FileArchivatorBean> getMainFileList()
    {
        return getMainFileList(false, false);
    }

	public static List<FileArchivatorBean> getMainFileList(boolean cleanCache) {
		return getMainFileList(cleanCache, false);
	}

	@SuppressWarnings("unchecked")
    public static List<FileArchivatorBean> getMainFileList(boolean cleanCache, boolean includeAwaiting)
	{
		String methodName = "getMainFileList-";
		String cacheKey = cachePrefix+methodName+"-domain_id-"+CloudToolsForCore.getDomainId();
		if(!cleanCache)
        {
            Object o = Cache.getInstance().getObject(cacheKey);
            if (o != null)
				return includeAwaiting == true ? (List<FileArchivatorBean>) o : filterAwaitingOut((List<FileArchivatorBean>) o);
        }
		//Logger.debug(FileArchivatorDB.class, "Nacitam z databazy: "+cacheKey);
		// Pair.of("uploaded", -1) - nezobrazujeme subory, ktore sa maju nahrat neskor
		List<FileArchivatorBean> fabList =  JpaTools.findByProperties(FileArchivatorBean.class,Pair.of("referenceId", -1), Pair.of("domainId", CloudToolsForCore.getDomainId()));
		Cache.getInstance().setObject(cacheKey, fabList, getCacheTime(methodName));
		return includeAwaiting == true ? fabList : filterAwaitingOut(fabList);
	}

	private static List<FileArchivatorBean> filterAwaitingOut(List<FileArchivatorBean> input) {
		return input.stream().filter(fab -> fab.getUploaded() == -1).toList();
	}

	/** Vrati vsetky subory, ktore sa mozu nahrat vzhladom na aktualny cas
	 *
	 * @return List<FileArchivatorBean>
	 */
	public static List<FileArchivatorBean> getFilesToUpload()
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		List<FileArchivatorBean> fabList = null;
		try{
			ExpressionBuilder builder = new ExpressionBuilder();
			ReadAllQuery dbQuery = new ReadAllQuery(FileArchivatorBean.class, builder);
			Expression expr = builder.get("uploaded").equal(0);

			Date currentDateTime = new Date();
			expr = expr.and(builder.get("dateUploadLater").lessThanEqual(currentDateTime));

			dbQuery.setSelectionCriteria(expr);

			Query query = em.createQuery(dbQuery);
			fabList = JpaDB.getResultList(query);
		}catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
		}finally{
			em.close();
		}

		return fabList;
	}

	public static List<FileArchivatorBean> getByConditions(Collection<String> domain, Collection<String> product, Collection<String> category,
				Collection<String> productCode, String dirPath_cached, boolean includeSubdirs, boolean asc, Boolean showFile,boolean useCache)
	{
		return getByConditions(null, product, category, productCode, dirPath_cached, includeSubdirs, asc, showFile, null, null, useCache, -1, false);
	}

	/**
	 * DEPRECATED, use getByConditions
	 * @param domain
	 * @param product
	 * @param category
	 * @param productCode
	 * @param dirPath_cached
	 * @param includeSubdirs
	 * @param asc
	 * @param showFile
	 * @param virtualName
	 * @param realName
	 * @param useCache
	 * @return
	 * @deprecated
	 */
	@Deprecated
	public static List<FileArchivatorBean> getByConditions(Collection<String> domain, Collection<String> product, Collection<String> category,
				Collection<String> productCode, String dirPath_cached, boolean includeSubdirs, boolean asc, Boolean showFile,
				String virtualName, String realName, boolean useCache)
	{
		return getByConditions(null, product, category, productCode, dirPath_cached, includeSubdirs, asc, showFile, virtualName, realName, useCache, -1, false);
	}

	public static List<FileArchivatorBean> getByConditions(Collection<String> domain, Collection<String> product, Collection<String> category,
			 Collection<String> productCode, String dirPath_cached, boolean includeSubdirs, boolean asc, Boolean pShowFile,
			 String virtualName, String realName, boolean useCache, int globalId, boolean onlyMain)
	{
		return getByConditions(product, category, productCode, dirPath_cached, includeSubdirs, asc, pShowFile, virtualName, realName, useCache, globalId, onlyMain);
	}

	/**
	 * Podla parametrov vrati vysledky
	 * @param product Collection<String>
	 * @param category Collection<String>
	 * @param productCode Collection<String>
	 * @param dirPath String
	 * @param includeSubdirs vratit aj podpriecink
	 * @param asc poradie zobrazovania
	 * @param pShowFile zobrazovat aj skryte subory
	 * @param virtualName String
	 * @param realName String
	 * @param useCache true / false - vratit z cache
	 * @param globalId int
	 * @param onlyMain iba hlavne subory
	 * @return List<FileArchivatorBean>
	 */
	public static List<FileArchivatorBean> getByConditions(Collection<String> product, Collection<String> category,
                                                           Collection<String> productCode, String dirPath, boolean includeSubdirs, boolean asc, Boolean pShowFile,
                                                           String virtualName, String realName, boolean useCache, int globalId, boolean onlyMain)
    {
        Boolean showFile = pShowFile;
        FileArchivatorSearchBean fabSearch = new FileArchivatorSearchBean();
        fabSearch.setProduct(product);
        fabSearch.setCategory(category);
        fabSearch.setProductCode(productCode);
        fabSearch.setDirPath(dirPath);
        fabSearch.setIncludeSubdirs(includeSubdirs);
        fabSearch.setAsc(asc);
        if(showFile == null)
            showFile = true;
        fabSearch.setShowFile(showFile);
        fabSearch.setVirtualFileName(virtualName);
        fabSearch.setFileName(realName);
        fabSearch.setUseCache(useCache);
        fabSearch.setGlobalId(globalId);
        fabSearch.setOnlyMain(onlyMain);

        return search(fabSearch);
    }

	/** vysledky z databazy bez pouzitia Cache + zohladni CloudToolsForCore.getDomainId() ak je zapnute
	 * @param fabSearch FileArchivatorSearchBean
	 * @return List<FileArchivatorBean>
	 */
	@SuppressWarnings("unchecked")
    public static List<FileArchivatorBean> search(FileArchivatorSearchBean fabSearch)
	{

		String methodName = "getByConditions-";
		String cacheKey = cachePrefix + methodName + getCheckSum(fabSearch.getDomain(), fabSearch.getProduct(), fabSearch.getCategory(), fabSearch.getProductCode()) + fabSearch.getDirPath()
				+ fabSearch.isIncludeSubdirs() + fabSearch.getGlobalId() + fabSearch.isOnlyMain() + fabSearch.getShowFile() + fabSearch.getVirtualFileName() + fabSearch.getFileName() + "-"
				+ fabSearch.isAsc() + "-domain_id-" + CloudToolsForCore.getDomainId() + fabSearch.getNote() + fabSearch.getFieldA() + fabSearch.getFieldB() + fabSearch.getFieldC();

		if(fabSearch.isUseCache())
		{
			Object o = Cache.getInstance().getObject(cacheKey);
			if(o != null)
				return (List<FileArchivatorBean>) o;
		}

		Logger.debug(FileArchivatorDB.class, "Nacitavam z databazy: "+cacheKey);
		List<Condition> conditions = new ArrayList<>();
        if(Constants.getBoolean("fileArchivUseSubStringSearchDomain"))
        {
            if(!Tools.isEmpty(fabSearch.getDomain()))
                conditions.add(filterSubstring("domain", fabSearch.getDomain().toArray()[0].toString() ));
        }
        else
        {
            if (!Tools.isEmpty(fabSearch.getDomain()))
                conditions.add(filterIn("domain", fabSearch.getDomain()));
        }

        if(Constants.getBoolean("fileArchivUseSubStringSearchProduct"))
        {
            if(!Tools.isEmpty(fabSearch.getProduct()))
                conditions.add(filterSubstring("product", fabSearch.getProduct().toArray()[0].toString()));
        }
        else
        {
            if (!Tools.isEmpty(fabSearch.getProduct()))
                conditions.add(filterIn("product", fabSearch.getProduct()));
        }

        if(Constants.getBoolean("fileArchivUseSubStringSearchCategory"))
        {
            if(!Tools.isEmpty(fabSearch.getCategory()))
                conditions.add(filterSubstring("category", fabSearch.getCategory().toArray()[0].toString()));
        }
        else
        {
            if (!Tools.isEmpty(fabSearch.getCategory()))
                conditions.add(filterIn("category", fabSearch.getCategory()));
        }

        if(Constants.getBoolean("fileArchivUseSubStringSearchProductCode"))
        {
            if(!Tools.isEmpty(fabSearch.getProductCode()))
                conditions.add(filterSubstring("product_code", fabSearch.getProductCode().toArray()[0].toString()));
        }
        else
        {
            if (!Tools.isEmpty(fabSearch.getProductCode()))
                conditions.add(filterIn("product_code", fabSearch.getProductCode()));
        }

		//13.9.2018
        if(!Tools.isEmpty(fabSearch.getExcludeCategory()))
            conditions.add(filterNotIn("category", fabSearch.getExcludeCategory()));


		if(fabSearch.getShowFile()!=null)
		{
			if (Constants.DB_TYPE==Constants.DB_PGSQL) {
				conditions.add(filterEquals("show_file", fabSearch.getShowFile().booleanValue()));
			} else {
				if(fabSearch.getShowFile().booleanValue())
					conditions.add(filterEquals("show_file", 1));
				else
					conditions.add(filterEquals("show_file", 0));
			}
		}

		if(Tools.isNotEmpty(fabSearch.getVirtualFileName()))
			conditions.add(filterSubstring("virtual_file_name", fabSearch.getVirtualFileName()));

		if(Tools.isNotEmpty(fabSearch.getFileName()))
			conditions.add(filterSubstring("file_name", fabSearch.getFileName()));

		if(fabSearch.isOnlyMain())
			conditions.add(filterEquals("reference_id", -1));

		if(fabSearch.getGlobalId() >= 0)
			conditions.add(filterEquals("global_id", fabSearch.getGlobalId()));

        if(Tools.isNotEmpty(fabSearch.getNote()))
            conditions.add(filterSubstring("note", fabSearch.getNote()));

        if(Tools.isNotEmpty(fabSearch.getFieldA()))
            conditions.add(filterSubstring("field_a", fabSearch.getFieldA()));

        if(Tools.isNotEmpty(fabSearch.getFieldB()))
            conditions.add(filterSubstring("field_b", fabSearch.getFieldB()));

        if(fabSearch.getFieldC() >= 0)
            conditions.add(filterEquals("field_c", fabSearch.getFieldC()));

        if(fabSearch.getDateInsertFrom() != null || fabSearch.getDateInsertTo() != null)
            conditions.add(filterBetween("date_insert", fabSearch.getDateInsertFrom(), fabSearch.getDateInsertTo()));

		//nezobrazujeme subory, ktore sa maju nahrat neskor
		conditions.add(filterEquals("uploaded", -1));

		conditions.add(filterEquals("domain_id", CloudToolsForCore.getDomainId()));
		//kvoli importu z ineho archivu aby sa zobrazovali subory na multidomain

		List<FileArchivatorBean> fabList = JpaTools.findBy(FileArchivatorBean.class, conditions.toArray(new Condition[]{}));
		filterByDir(fabList, fabSearch.getDirPath(), fabSearch.isIncludeSubdirs());
		//odstranime vzory - tie sa dohladavaju zvlast
		if(fabSearch.isUseCache())//ked nepouzivame cache ideme z filtra a tam vysledky vyhladavania chceme. Vo vypise komponenty nie
			removePattern(fabList);

		fabList.sort(new SortByPriority(fabSearch.isAsc()));

		if(fabSearch.isUseCache())
		{
			Cache.getInstance().setObject(cacheKey, fabList, getCacheTime(methodName));
		}

		return fabList;
	}

	protected static void filterByDir(List<FileArchivatorBean> fabList, String dirPath, boolean includeSubdirs )
	{
		if(!Tools.isEmpty(fabList) && Tools.isNotEmpty(dirPath))
		{
			Iterator<FileArchivatorBean> it = fabList.iterator();
			while(it.hasNext())
			{
				FileArchivatorBean fab = it.next();
				if(Tools.isNotEmpty(fab.getFilePath()) && fab.getFilePath().startsWith(dirPath))
				{
					if(!includeSubdirs && !fab.getFilePath().equals(dirPath))
						it.remove();
				}
				else
				{
					it.remove();
				}
			}
		}
	}

	protected static void removePattern(List<FileArchivatorBean> fabList)
	{
		for(int i=0;i<fabList.size();i++)
		{
			if(Tools.isNotEmpty(fabList.get(i).getReferenceToMain()))
			{
				fabList.remove(i);
				i--; //NOSONAR
			}
		}
	}

	/**
	 * Vrati vzor pre zadany subor
	 * @param url - cesta v tvare archiv/... k suboru
	 * @return FileArchivatorBean
	 */
	@SuppressWarnings("unchecked")
	public static FileArchivatorBean getPatern(String url)
	{
		if(Tools.isEmpty(url))
			return null;

		String methodName = "getPatern-";
		Map<String,FileArchivatorBean> fabPatternTable = new Hashtable<>();
		String cacheKey = cachePrefix+methodName+"domain_id-"+CloudToolsForCore.getDomainId();
		Object o = Cache.getInstance().getObject(cacheKey);
		if(o == null)
		{
			for(FileArchivatorBean fab: FileArchivatorDB.getInstance().getAll())
			{
				if(Tools.isNotEmpty(fab.getReferenceToMain()))
					fabPatternTable.put(fab.getReferenceToMain(), fab);
			}

			Cache.getInstance().setObject(cacheKey, fabPatternTable, getCacheTime(methodName));
			o = fabPatternTable;
		}

		fabPatternTable = (Map<String,FileArchivatorBean>) o;

		return fabPatternTable.get(url);
	}

	public static FileArchivatorBean getByPath(String path, String fileName)
	{
		if(Tools.isNotEmpty(path) && Tools.isNotEmpty(fileName))
		{
			List<FileArchivatorBean> fabList = JpaTools.findByProperties(FileArchivatorBean.class, Pair.of("filePath", path), Pair.of("fileName", fileName));
			if(fabList != null && fabList.size() >= 1)
			{
				return fabList.get(0);
			}
		}
		return null;
	}

   /**
    * Vrati bean podla standardneho WJ url (napr. /files/archiv/subor.pdb) s tym, ze skontroluje platnost beanu a datumov (najde vyhovujuci, ak je ich viac)
	 * Pouziva sa vo vyhladavani pre nahradenie nazvu suboru za virtualny nazov beanu
    * @param url String
    * @return FileArchivatorBean
    */
	public static FileArchivatorBean getByUrl(String url)
	{
		return getByUrl(url, false);
	}

	public static FileArchivatorBean getByUrl(String url, boolean alsoInactive)
	{
		try
		{
			if(Tools.isEmpty(url))
				return null;
			int lastSeparator = url.lastIndexOf('/');
			if (lastSeparator<1) return null;
			//z path odstranujeme prve lomitko, tak to ma palo neviempreco v DB
			String path = url.substring(1, lastSeparator+1);
			String fileName = url.substring(lastSeparator+1);

			if(Tools.isNotEmpty(path) && Tools.isNotEmpty(fileName))
			{
				List<FileArchivatorBean> fabList = JpaTools.findByProperties(FileArchivatorBean.class, Pair.of("filePath", path), Pair.of("fileName", fileName));
				for (FileArchivatorBean fab : fabList)
				{
					if (!alsoInactive && Tools.isFalse(fab.getShowFile()) ) continue;
					if (fab.isValidDates()) return fab;
				}
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		return null;
	}

	/**
	 * vrati hlavne zaznamy so zadanymi globalId
	 *
	 * @param globalId ArrayList<Integer>
	 * @return List<FileArchivatorBean>
	 */
	public static List<FileArchivatorBean> getByGlobalId(List<Integer> globalId)
	{
		if(globalId==null || globalId.isEmpty())
			return null;

		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		List<FileArchivatorBean> fabList = null;
		try{
			ExpressionBuilder builder = new ExpressionBuilder();
			ReadAllQuery dbQuery = new ReadAllQuery(FileArchivatorBean.class, builder);
			Expression expr = builder.get("referenceId").equal(-1);
			Expression expr2 = null;

			for(int i=0; i<globalId.size(); i++)
			{
				if(i==0)
					expr2 = builder.get("globalId").equal(globalId.get(i));
				else
					expr2 = JpaDB.or(expr2, builder.get("globalId").equal(globalId.get(i)));
			}

			expr = expr.and(expr2);

			dbQuery.setSelectionCriteria(expr);

			Query query = em.createQuery(dbQuery);
			fabList = JpaDB.getResultList(query);
		}catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
		}finally{
			em.close();
		}

		return fabList;

		//List<FileArchivatorBean> fabList = JpaTools.findByProperties(FileArchivatorBean.class, Pair.of("globalId", globalId), Pair.of("referenceId", -1));
		//return fabList;
	}

	public static String getCachePrefix()
	{
		return cachePrefix;
	}

	public static String getConstantsPrefix()
	{
		return CONSTANTS_PREFIX;
	}

	protected static String getCheckSum(Collection<String> strCollection_1, Collection<String> strCollection_2, Collection<String> strCollection_3, Collection<String> strCollection_4)
	{
		return FileArchivatorKit.getSecurityHash(getString(strCollection_1)+getString(strCollection_2)+getString(strCollection_3)+getString(strCollection_4));
	}

	protected static String getString(Collection<String> strCollection)
	{
		StringBuilder strBld = new StringBuilder();
		if(strCollection != null && strCollection.size() > 0)
		{
			for(String str:strCollection)
			{
				strBld.append(str);
			}
		}
		return strBld.toString();
	}

	protected static int getCacheTime(String methodName)
	{
		int timeMinutes = Tools.getIntValue(Constants.getInt(CONSTANTS_PREFIX+CACHE_TIME_KEY+"-"+methodName),-1) ;
		if(timeMinutes >= 0 )
			return timeMinutes;

		timeMinutes = Tools.getIntValue(Constants.getInt(CONSTANTS_PREFIX+CACHE_TIME_KEY),-1) ;
		if(timeMinutes >= 0 )
			return timeMinutes;

		return 120;
	}

	/**
	 * vsetky kategorie zvolenej domeny
	 * @return
	 */
	public static List<String> getAllCategories()
    {
		@SuppressWarnings("unchecked")
       List<String> allCategoriesNull = DB.queryForList("SELECT distinct CATEGORY FROM file_archiv WHERE domain_id = ?", CloudToolsForCore.getDomainId());
       //lebo Oracle vracia namiesto prazdneho Stringu NULL a potom to pada na porovnani
       List<String> allCategories = new ArrayList<>();
       for (String category : allCategoriesNull)
       {
          if (category == null) category = "";
          allCategories.add(category);
       }

       return allCategories;
    }

    public static List<String> getAllCategories2(String category1)
    {
		@SuppressWarnings("unchecked")
		List<String> list = DB.queryForList("SELECT distinct FIELD_A FROM file_archiv WHERE CATEGORY LIKE ?",category1);
        return list;
    }

	 /**
	  * vytvori nove kategorie do manazera kategorii
	  * @param category_1 int
	  * @param category_2 String
	  * @param category_3 String
	  * @return boolean
	  */
	  @SuppressWarnings("unchecked")
    public static boolean saveCategories(int category_1, String category_2, String category_3)
	 {
	 	 try
		 {
			  Prop prop = Prop.getInstance();
			  String bezKategorie = prop.getText("components.file_archiv.bez_kategorie");

			  //zisitm ci existuje zaznam s hl. kategoriou, ak nie, vytvorim v categoryNode
			  Node<FileArchivCategoryNodeBean> nodeCategory1 = null;
			  if (category_1 > 0)
			  {
					nodeCategory1 = FileArchivCategoryNodeDB.getCategory(category_1);
					if (nodeCategory1 == null || nodeCategory1.unwrap().getId() < 0)
					{
						 UserGroupDetails userGroupDetails = UserGroupsDB.getInstance().getUserGroup(category_1);
						 if (userGroupDetails != null)
						 {
							  nodeCategory1 = FileArchivCategoryNodeDB
										  .createNode(userGroupDetails.getUserGroupName(), category_1, -2, true);
						 }
					}
			  }
			  if (category_1 > 0)
			  {
					//pre categoryNode musim vytvorit aj ked ju nema definovanu
					if (Tools.isEmpty(category_2))
						 category_2 = bezKategorie;
					if (Tools.isEmpty(category_3))
						 category_3 = bezKategorie;

					//ak neexistuje kategoria 2, musim vytvorit aj v categoryNode
					@SuppressWarnings("rawtypes")
					Node nodeCategory2;
					int nodeCategory2Id = FileArchivCategoryNodeDB.getIdByCategory(String.valueOf(category_1), category_2, null);
					if (nodeCategory2Id > 0)
						 nodeCategory2 = FileArchivCategoryNodeDB.getId(nodeCategory2Id);
					else
						 nodeCategory2 = FileArchivCategoryNodeDB.createSubNode(nodeCategory1, category_2, category_1, -2, true);
					//ak neexistuje kategoria 3, musim vytvorit aj v categoryNode
					if (FileArchivCategoryNodeDB.getIdByCategory(String.valueOf(category_1), category_2, category_3) < 1)
						 FileArchivCategoryNodeDB.createSubNode(nodeCategory2, category_3, category_1, -2, true);
			  }
		 }
	 	 catch(Exception e)
		 {
		 	sk.iway.iwcm.Logger.error(e);
		 	return false;
		 }

	 	 return true;
	 }

	/**
	 * vrati zoznam suborov na zaklade referencie
	 * @param sortByReference reference (fabId) / time (orderId) / priority (priorityId)
	 * @param asc true ak ASC
	 * @return List<FileArchivatorBean>
	 */
	public static List<FileArchivatorBean> getReference(Long referenceId, String sortByReference, boolean asc)
	{
		List<FileArchivatorBean> shorFab = new ArrayList<>(FileArchivatorDB.getByReferenceId(referenceId));

		//musime odstranit
		for(int i = 0; i< shorFab.size(); i++)
		{
			if( Tools.isFalse(shorFab.get(i).getShowFile()) || shorFab.get(i).getUploaded() != -1)
			{
				shorFab.remove(i);
				i--; //NOSONAR
			}
		}

		if(shorFab.size() > 0)
		{
			if("reference".equals(sortByReference))
			{
				if(asc)
					shorFab.sort(Comparator.comparingDouble(FileArchivatorBean::getId));
				else
					shorFab.sort(Comparator.comparingDouble(FileArchivatorBean::getId).reversed());
			}
			else if("orderId".equals(sortByReference))
			{
				if(asc)
					shorFab.sort(Comparator.comparingInt(FileArchivatorBean::getOrderId));
				else
					shorFab.sort(Comparator.comparingInt(FileArchivatorBean::getOrderId).reversed());
			}
			else if("time".equals(sortByReference))
			{
				if(asc)
					shorFab.sort(Comparator.comparing(FileArchivatorBean::getDateInsert));
				else
					shorFab.sort(Comparator.comparing(FileArchivatorBean::getDateInsert).reversed());
			}
			else if("virtual_file_name".equals(sortByReference))
			{
				 if(asc)
					  shorFab.sort(Comparator.comparing(FileArchivatorBean::getVirtualFileName));
				 else
					  shorFab.sort(Comparator.comparing(FileArchivatorBean::getVirtualFileName).reversed());
			}
			else//priority
			{
				if(asc)
					shorFab.sort(Comparator.comparingDouble(FileArchivatorBean::getPriority));
				else
					shorFab.sort(Comparator.comparingDouble(FileArchivatorBean::getPriority).reversed());
			}
		}
		return shorFab;
	}

	/**
	 * vytvori kolekciu na zaklade hodnot include paramteri
	 * @param pageParamName
	 * @param request
	 * @return
	 */
	public static Collection<String> createCollection(String pageParamName, HttpServletRequest request)
	{
		PageParams pageParams = new PageParams(request);
		String[] propertyArray = Tools.getTokens(pageParams.getValue(pageParamName, ""), "+");
		if(propertyArray != null && propertyArray.length > 0)
			return Arrays.asList(propertyArray);
		return null;
	}

	/**
	 * vrati pocet referencii na subor zo zoznamu fabsList
	 */
	public static int getNumberOfReference(List<FileArchivatorBean> fabsList, int referenceId)
	{
		int count = 0;
		if(fabsList != null)
		{
			for(FileArchivatorBean fab:fabsList)
			{
				if(fab.getReferenceId() == referenceId)
					count++;
			}
			return count;
		}
		return -1;
	}

	/**
	 * vymaze fabID zo zoznamu fabsList
	 */
	public static void removeById(List<FileArchivatorBean> fabsList, int fileArchivId)
	{
		if(fabsList != null)
		{
			for (int i = 0; i < fabsList.size(); i++)
			{
				if (fabsList.get(i).getId() == fileArchivId)
				{
					fabsList.remove(i);
					break;
				}
			}
		}
	}

	/**
	 * vrati zoznam nunikatnych hodnot pre stlpec vo file_archiv, okrem null a pradnych + cachuje per domena
	 * @param column - stlpec vo file_archiv
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<String> getDistinctListByProperty(String column)
	{
		if(Tools.isEmpty(column)) return Collections.emptyList();

		String methodName = "getDistinctListByProperty-";
		String cacheKey = cachePrefix+methodName+"-property-"+column+"-domain_id-"+CloudToolsForCore.getDomainId();
		Object o = Cache.getInstance().getObject(cacheKey);
		if(o != null)
			return (List<String>) o;
		Logger.debug(FileArchivatorDB.class, "Nacitam z databazy: "+cacheKey);

		List<String> result =  DB.queryForList("SELECT DISTINCT "+column+" FROM file_archiv WHERE "+column+" IS NOT NULL AND uploaded=-1 AND domain_id="+CloudToolsForCore.getDomainId()+" ORDER BY "+column+" ASC");
		Cache.getInstance().setObject(cacheKey, result, getCacheTime(methodName));

		return result;
	}

	 /**
	  * zoradi zaznamy podla priority/datumu nahratia/virtualneho nazvu/ID
	  * @param fabListCache
	  * @param sortBy
	  * @param asc
	  */
	 public static void sortBy(List<FileArchivatorBean> fabListCache, String sortBy, boolean asc)
	 {
		  if(fabListCache != null && fabListCache.size() > 0)
		  {
				if("priority".equals(sortBy))
				{
					 if(asc)
						  fabListCache.sort(Comparator.comparingDouble(FileArchivatorBean::getPriority));
					 else
						  fabListCache.sort(Comparator.comparingDouble(FileArchivatorBean::getPriority).reversed());
				}
				else if("virtual_file_name".equals(sortBy))
				{
					 if(asc)
						  fabListCache.sort(Comparator.comparing(FileArchivatorBean::getVirtualFileName));
					 else
						  fabListCache.sort(Comparator.comparing(FileArchivatorBean::getVirtualFileName).reversed());
				}
				else //time - default
				{
					 if(asc)
						  fabListCache.sort(Comparator.comparing(FileArchivatorBean::getDateInsert));
					 else
						  fabListCache.sort(Comparator.comparing(FileArchivatorBean::getDateInsert).reversed());
				}

		  }
	 }

	/***************************** BACKWARD COMPATIBILITY ******************************/
	public static List<FileArchivatorBean> getByReferenceId(int referenceId) {
		return getByReferenceId((long) referenceId);
	}

	public static List<FileArchivatorBean> getReference(int referenceId, String sortByReference, boolean asc) {
		return getReference((long) referenceId, sortByReference, asc);
	}

	public static int getCountByReferenceId(Long referenceId)
    {
        JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
        em.getTransaction().begin();
        Query query =  em.createQuery("SELECT COUNT (f) FROM FileArchivatorBean f WHERE f.referenceId = :reference", Long.class);

        query.setParameter("reference",referenceId);
        int count = (int)((long)query.getSingleResult());
        em.getTransaction().commit();
        return count;
    }

}