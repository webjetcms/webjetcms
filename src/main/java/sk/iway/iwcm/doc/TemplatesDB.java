package sk.iway.iwcm.doc;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.helpers.BeanDiff;
import sk.iway.iwcm.helpers.BeanDiffPrinter;
import sk.iway.iwcm.stat.BrowserDetector;
import sk.iway.iwcm.stat.StatNewDB;
import sk.iway.iwcm.system.cluster.ClusterDB;
import sk.iway.iwcm.users.UserDetails;

/**
 *  Cachuje v pamati zaznamy z tabulky templates
 *
 *@Title        Interway Content Management
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.4 $
 *@created      $Date: 2004/01/24 08:05:42 $
 *@modified     $Date: 2004/01/24 08:05:42 $
 */
public class TemplatesDB extends DB
{
   private List<TemplateDetails> temps;

   	public static TemplatesDB getInstance() {
		return getInstance(false);
	}

   /**
    *  Gets the instance attribute of the TemplatesDB class
    *
    *@param  force_refresh   Description of the Parameter
    *@return                 The instance value
    */
   public static TemplatesDB getInstance(boolean forceRefresh)
   {
      //try to get it from server space
      if (forceRefresh == false)
      {
         //Logger.println(this,"TempDB: getting from server space");
      	TemplatesDB tempDB = ((TemplatesDB) Constants.getServletContext().getAttribute(Constants.A_TEMP_DB));
      	if (tempDB != null)
      	{
      		return tempDB;
      	}
      }
      synchronized (TemplatesDB.class)
		{
			if (forceRefresh)
			{
				TemplatesDB tempDB = new TemplatesDB();
				//	remove
				Constants.getServletContext().removeAttribute(Constants.A_TEMP_DB);
				//save us to server space
				Constants.getServletContext().setAttribute(Constants.A_TEMP_DB, tempDB);

				return tempDB;
			}
			else
			{
				//double check
				TemplatesDB tempDB = (TemplatesDB) Constants.getServletContext().getAttribute(Constants.A_TEMP_DB);
				if (tempDB == null)
				{
					tempDB = new TemplatesDB();
					//	remove
					Constants.getServletContext().removeAttribute(Constants.A_TEMP_DB);
					//save us to server space
					Constants.getServletContext().setAttribute(Constants.A_TEMP_DB, tempDB);

				}
				return tempDB;
			}
		}
   }

	/**
	 * Constructor for the TemplatesDB object
	 *
	 * @param servletContext Description of the Parameter
	 * @param serverName     Description of the Parameter
	 */
	private TemplatesDB() {
		DebugTimer dt = new DebugTimer("TempDB: constructor [" + Constants.getInstallName() + "]");

		try {
			reload();
		} catch (Exception ex) {
			Logger.error(this, "Can't load templates");
			Logger.error(TemplatesDB.class, ex);
		}

		ClusterDB.addRefresh(TemplatesDB.class);

		dt.diff("done");
	}

	/**
	 * Description of the Method
	 *
	 * @param servletContext Description of the Parameter
	 * @exception Exception Description of the Exception
	 */
	private void reload() throws Exception {

		List<TemplateDetails> loadedTemps = new ArrayList<>();
		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			//Nacitaj zoznam tempGroups
			List<TemplatesGroupBean> tempGroupsList = TemplatesGroupDB.getAllTemplatesGroups();
			Map<Long, String> tempGroupsTable = new Hashtable<>();
			for (TemplatesGroupBean tgb : tempGroupsList) {
				tempGroupsTable.put(tgb.getId(), tgb.getName());
			}

			db_conn = DBPool.getConnection();

			String sql;
			sql = "SELECT * FROM templates order by temp_name";
			// sql = "SELECT t.*, COUNT(d.temp_id) as doc_count FROM templates t LEFT JOIN
			// documents d USING (temp_id) GROUP BY temp_id;";
			ps = db_conn.prepareStatement(sql);
			// ps.setInt(1, user.getUserId());
			// Logger.println(this,"sql="+sql);

			rs = ps.executeQuery();

			TemplateDetails tmpDetails;

			while (rs.next()) {
				tmpDetails = new TemplateDetails();
				tmpDetails.setTempId(rs.getInt("temp_id"));
				tmpDetails.setTempName(getDbString(rs, "temp_name"));
				tmpDetails.setForward(getDbString(rs, "forward"));
				tmpDetails.setLng(getDbString(rs, "lng"));
				tmpDetails.setHeaderDocId(rs.getInt("header_doc_id"));
				tmpDetails.setFooterDocId(rs.getInt("footer_doc_id"));
				tmpDetails.setMenuDocId(rs.getInt("menu_doc_id"));
				tmpDetails.setRightMenuDocId(rs.getInt("right_menu_doc_id"));
				tmpDetails.setAfterBodyData(getDbString(rs, "after_body_data"));
				tmpDetails.setCss(getDbString(rs, "css"));
				tmpDetails.setBaseCssPath(getDbString(rs, "base_css_path"));

				tmpDetails.setObjectADocId(rs.getInt("object_a_doc_id"));
				tmpDetails.setObjectBDocId(rs.getInt("object_b_doc_id"));
				tmpDetails.setObjectCDocId(rs.getInt("object_c_doc_id"));
				tmpDetails.setObjectDDocId(rs.getInt("object_d_doc_id"));

				tmpDetails.setAvailableGroups(getDbString(rs, "available_groups"));

				tmpDetails.setTemplateInstallName(getDbString(rs, "template_install_name"));
				tmpDetails.setDisableSpamProtection(rs.getBoolean("disable_spam_protection"));

				// tmpDetails.setPocetPouziti(rs.getInt("doc_count"));

				tmpDetails.setTemplatesGroupId(rs.getLong("templates_group_id"));

				String tempGroupName = null;
				if (tmpDetails.getTemplatesGroupId()!=null) {
					tempGroupName = tempGroupsTable.get(tmpDetails.getTemplatesGroupId());
				}
				if (tempGroupName==null) tempGroupName = "";
				tmpDetails.setTemplatesGroupName(tempGroupName);

				tmpDetails.setInlineEditingMode(getDbString(rs, "inline_editing_mode"));

				loadedTemps.add(tmpDetails);
			}
			rs.close();
			ps.close();
			db_conn.close();
			db_conn = null;
			rs = null;
			ps = null;

			temps = loadedTemps;

		} catch (Exception ex) {
			Logger.error(TemplatesDB.class, ex);
			// backward compatibility - method used to throw an exception
			throw ex;
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			} catch (Exception e) {
				//
			}
		}
	}

   /**
    *  vrati sablonu so zadanym id
    *
    *@param  temp_id  id sablony
    *@return          sablona
    */
   public TemplateDetails getTemplate(int temp_id)
   {
      for (TemplateDetails details : temps)
      {
         if (details.getTempId() == temp_id)
         {
            return (details);
         }
      }
      return (null);
   }

   /**
    *  vrati sablonu so zadanym menom
    *
    *@param tempName id sablony
    *@return sablona
    */

   public TemplateDetails getTemplate(String tempName)
   {
      if (tempName==null || tempName.trim().length()<1) return(null);
      tempName = tempName.trim();
      for (TemplateDetails details : temps)
      {
         if (details.getTempName().equalsIgnoreCase(tempName))
         {
            return (details);
         }
      }
      return (null);
   }

   /**
    * Vrati sablonu na zaklade nazvu a typu zariadenia (browserDeviceType)
    * Sablona musi v nazve obsahovat "device="+bd.getBrowserDeviceType()
    * @param temp
    * @param bd
    * @return
    */
   public TemplateDetails getTemplate(TemplateDetails temp, BrowserDetector bd)
   {
   	if (temp==null) return null;
   	String tempName = temp.getTempName();
      if (tempName==null || tempName.trim().length()<1 || bd == null || bd.getBrowserDeviceType()==null) return(null);

      tempName = tempName.trim();
      for (TemplateDetails details : temps)
      {
         if (details.getTempName().startsWith(tempName+" device=") && details.getTempName().indexOf("device="+bd.getBrowserDeviceType())!=-1)
         {
            return (details);
         }
      }

      if (Tools.isNotEmpty(bd.getBrowserDeviceType()) && "normal".equals(bd.getBrowserDeviceType())==false)
      {
      	synchronized (temps)
			{
      		//double check
      		for (TemplateDetails details : temps)
            {
               if (details.getTempName().startsWith(tempName+" device=") && details.getTempName().toLowerCase().indexOf("device="+bd.getBrowserDeviceType())!=-1)
               {
                  return (details);
               }
            }

	      	try
	      	{
	      		Logger.debug(TemplatesDB.class, "Creating device template, name="+temp.getTempName()+" device="+bd.getBrowserDeviceType());

		      	//vytvor pseudo sablonu pre dane device
		      	TemplateDetails deviceTemp = new TemplateDetails();
		      	BeanUtils.copyProperties(deviceTemp, temp);
		      	deviceTemp.setTempId(-1);
		      	deviceTemp.setTempName(temp.getTempName()+" device="+bd.getBrowserDeviceType());

		      	deviceTemp.setHeaderDocData(getDeviceHeaderFooterMenu(temp.getHeaderDocId(), bd));
		      	deviceTemp.setMenuDocData(getDeviceHeaderFooterMenu(temp.getMenuDocId(), bd));
		      	deviceTemp.setRightMenuDocData(getDeviceHeaderFooterMenu(temp.getRightMenuDocId(), bd));
		      	deviceTemp.setFooterDocData(getDeviceHeaderFooterMenu(temp.getFooterDocId(), bd));

		      	deviceTemp.setObjectADocData(getDeviceHeaderFooterMenu(temp.getObjectADocId(), bd));
		      	deviceTemp.setObjectBDocData(getDeviceHeaderFooterMenu(temp.getObjectBDocId(), bd));
		      	deviceTemp.setObjectCDocData(getDeviceHeaderFooterMenu(temp.getObjectCDocId(), bd));
		      	deviceTemp.setObjectDDocData(getDeviceHeaderFooterMenu(temp.getObjectDDocId(), bd));

		      	//uloz do listu
		      	temps.add(deviceTemp);
		      	return deviceTemp;
	      	}
	      	catch (Exception ex)
	      	{
	      		Logger.error(TemplatesDB.class, ex);
	      	}
			}
      }

      return (null);
   }

   private String getDeviceHeaderFooterMenu(int origDocId, BrowserDetector bd)
   {
   	if (origDocId<1) return null;

   	DocDB docDB = DocDB.getInstance();
   	//tu nemoze byt BasicDocDetails pretoze dole potrebujem origDoc.getData
   	DocDetails origDoc = docDB.getDoc(origDocId);
   	if (origDoc != null)
   	{
   		List<DocDetails> docs = docDB.getBasicDocDetailsByGroup(origDoc.getGroupId(), -1);
   		for (DocDetails doc:docs)
   		{
   			//Logger.debug(TemplatesDB.class, "Testing: "+origDoc.getTitle()+" d="+bd.getBrowserDeviceType()+" vs "+doc.getTitle()+" device="+bd.getBrowserDeviceType()+" p1="+doc.getTitle().startsWith(origDoc.getTitle())+" p2="+(doc.getTitle().toLowerCase().indexOf("device="+bd.getBrowserDeviceType())));
   			if (doc.getTitle().startsWith(origDoc.getTitle()) && doc.getTitle().toLowerCase().indexOf("device="+bd.getBrowserDeviceType())!=-1)
            {
   				//musim ziskat komplet dokument
   				DocDetails docFull = docDB.getDoc(doc.getDocId());
   				if (docFull!=null) return docFull.getData();
   				return origDoc.getData();
            }
   		}
   		return origDoc.getData();
   	}

   	return null;
   }

	/**
	 * Vrati subor na zaklade jeho mena a browser detector
	 * subor musi v nazve obsahovat "device="+bd.getBrowserDeviceType()
	 * @param rootDir
	 * @param forward
	 * @param bd
	 * @return
	 */
	public static File getDeviceTemplateFile(File rootDir, String forward, BrowserDetector bd)
	{
		File forwFile = new File(rootDir.getAbsolutePath()+File.separatorChar+forward.replace('/', File.separatorChar));
		if (forwFile.exists()==false) return(forwFile);

		if (bd!=null && bd.getBrowserDeviceType()!=null)
		{
			String forwName = Tools.replace(forwFile.getName(), ".jsp", "");
			forwName = Tools.replace(forwFile.getName(), ".html", "");
			File[] files = rootDir.listFiles();
			int size = files.length;
			int i;
			for (i=0; i<size; i++)
			{
				if (files[i].getName().startsWith(forwName) && files[i].getName().toLowerCase().indexOf("device="+bd.getBrowserDeviceType())!=-1)
				{
					return(files[i]);
				}
			}
		}
		return(forwFile);
	}


   /**
    *  vrati zoznam vsetkych sablon
    *
    *@return    sablony
    */
   public List<TemplateDetails> getTemplates()
   {
      return (temps);
   }

   /**
    * Vrati sablony dostupne pre zadane groupId
    * @param groupId
    * @return
    */
   public List<TemplateDetails> getTemplates(int groupId)
   {
   	return getTemplates(groupId, -1);
   }

   /**
    * Vrati sablony dostupne pre zadane groupId pricom VZDY bude obsahovat aj sabonu s mustHaveTempId
    * @param groupId
    * @param mustHaveTempId
    * @return
    */
   public List<TemplateDetails> getTemplates(int groupId, int mustHaveTempId)
   {
   	//najskor potrebujeme zoznam parent skupin
   	GroupsDB groupsDB = GroupsDB.getInstance();
   	List<GroupDetails> groups = groupsDB.getParentGroups(groupId);

      List<TemplateDetails> ret = new ArrayList<>();
      for (TemplateDetails temp : temps)
      {
         if (temp.getTempId()<1) continue;
         int[] availableGroups = temp.getAvailableGroupsInt();
         if (availableGroups.length == 0 || isGroupAvailable(availableGroups, groups) || temp.getTempId()==mustHaveTempId)
         {
         	ret.add(temp);
         }
      }

      if (ret.size()==0)
      {
      	ret.add(temps.get(0));
      }

      return (ret);
   }

   /**
    * Odfiltruje zo zoznamu sablon tie, ktore su urcene pre specificky device (v nazve obsahuju device=)
    * viz tiket 7197
    * @param allTemplates
    * @return
    */
   public static List<TemplateDetails> filterDeviceTemplates(List<TemplateDetails> allTemplates)
   {
   	List<TemplateDetails> templates = new ArrayList<>();

   	for (TemplateDetails temp : allTemplates)
   	{
   		if (temp == null || Tools.isEmpty(temp.getTempName()) || temp.getTempName().indexOf("device=")!=-1) continue;

   		templates.add(temp);
   	}

   	return templates;
   }

   private boolean isGroupAvailable(int[] availableGroups, List<GroupDetails> groups)
   {
   	for (int groupId : availableGroups)
   	{
	   	for (GroupDetails group : groups)
	   	{
	   		if (group.getGroupId() == groupId) return true;
	   	}
   	}
   	return false;
   }

   /**
    * Otestuje, ci sa dana sablona pouziva. Ak je zadane -1 tak vzdy vrati ze ano
    * @param tempId
    * @return
    */
   public static boolean isTemplateUsed(int tempId)
   {
   	//novym vraciame ze ano, aby sa nezobrazilo Delete tlacitko
   	if (tempId < 0) return(true);

   	boolean used = false;

   	Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();

			String sql = "SELECT doc_id FROM documents WHERE temp_id=?";
			if (Constants.DB_TYPE==Constants.DB_MYSQL) sql += " LIMIT 0, 1";
			else if (Constants.DB_TYPE==Constants.DB_MSSQL) sql = "SELECT TOP 1 doc_id FROM documents WHERE temp_id=?";
			else if (Constants.DB_TYPE==Constants.DB_ORACLE) sql += " AND rownum<2";

			ps = StatNewDB.prepareStatement(db_conn, sql);
			ps.setInt(1, tempId);
			rs = ps.executeQuery();
			if (rs.next())
			{
				used = true;
			}
			rs.close();
			ps.close();
			rs = null;
			ps = null;
		}
		catch (Exception ex)
		{
			Logger.error(TemplatesDB.class, ex);
		}
		finally
		{
			try
			{
				if (db_conn != null)
					db_conn.close();
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
			}
			catch (Exception ex2)
			{
				//
			}
		}
		return(used);
   }

   /**
    * vrati Hashtable, kde kluc je temp_id a hodnota je pocet stranok, ktore pouzivaju dany template
    * @return Hashtable
    */
   public Map<Integer, Integer> numberOfPages()
   {
		Map<Integer, Integer> table = new Hashtable<>();

		List<DocDetails> allDocs = DocDB.getInstance().getBasicDocDetailsAll();
		for (DocDetails d : allDocs)
		{
			Integer tempId = Integer.valueOf(d.getTempId());
			Integer count = table.get(tempId);
			if (count == null) count = Integer.valueOf(1);
			else count = Integer.valueOf(count.intValue() + 1);

			table.put(tempId, count);
		}

		return(table);
   }

   /**
    * Ulozi sablonu do databazy
    * @param t_form
    * @return
    */
   public boolean saveTemplate(TemplateDetails t_form)
   {
   	Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();

			String sql;

			TemplateDetails oldTemplate = t_form.getTempId() > 0 ? getTemplate(t_form.getTempId()) : null;

			sql = "INSERT INTO templates (temp_name, forward, header_doc_id, footer_doc_id, menu_doc_id, after_body_data, css, lng, right_menu_doc_id, base_css_path, object_a_doc_id, object_b_doc_id, object_c_doc_id, object_d_doc_id, available_groups, template_install_name, disable_spam_protection, templates_group_id, inline_editing_mode) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

			if (t_form.getTempId() > 0)
				sql = "UPDATE templates SET temp_name=?, forward=?, header_doc_id=?, footer_doc_id=?, menu_doc_id=?, after_body_data=?, css=?, lng=?, right_menu_doc_id=?, base_css_path=?, object_a_doc_id=?, object_b_doc_id=?, object_c_doc_id=?, object_d_doc_id=?, available_groups=?, template_install_name=?, disable_spam_protection=?, templates_group_id=?, inline_editing_mode=? WHERE temp_id=?";

			ps = db_conn.prepareStatement(sql);
			ps.setString(1, t_form.getTempName());
			ps.setString(2, t_form.getForward());
			ps.setInt(3, t_form.getHeaderDocId());
			ps.setInt(4, t_form.getFooterDocId());
			ps.setInt(5, t_form.getMenuDocId());
			DB.setClob(ps, 6, t_form.getAfterBodyData());
			ps.setString(7, t_form.getCss());
			ps.setString(8, t_form.getLng());
			ps.setInt(9, t_form.getRightMenuDocId());
			ps.setString(10, t_form.getBaseCssPath());
			ps.setInt(11, t_form.getObjectADocId());
			ps.setInt(12, t_form.getObjectBDocId());
			ps.setInt(13, t_form.getObjectCDocId());
			ps.setInt(14, t_form.getObjectDDocId());
			ps.setString(15, t_form.getAvailableGroups());
			ps.setString(16, t_form.getTemplateInstallName());
			ps.setBoolean(17, t_form.isDisableSpamProtection());
			long tempGroupId = 1; //1==nepriradene
			if (t_form.getTemplatesGroupId()!=null) tempGroupId = t_form.getTemplatesGroupId();
			ps.setLong(18, tempGroupId);
			ps.setString(19, t_form.getInlineEditingMode());

			if (t_form.getTempId() > 0)
			{
				ps.setInt(20, t_form.getTempId());
			}
			ps.execute();
			ps.close();

			if (t_form.getTempId() < 1)
			{
				ps = db_conn.prepareStatement("SELECT max(temp_id) AS temp_id FROM templates WHERE temp_name=?");
				ps.setString(1, t_form.getTempName());
				rs = ps.executeQuery();
				if (rs.next())
				{
					t_form.setTempId(rs.getInt("temp_id"));
				}
				rs.close();
				ps.close();
				rs = null;
			}

			db_conn.close();
			db_conn = null;
			ps = null;

			//refreshni DB
			TemplatesDB.getInstance(true);
			logChanges(t_form, oldTemplate);

			return true;
		}
		catch (Exception ex)
		{
			Logger.error(TemplatesDB.class, ex);
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
				//
			}
		}
		return false;
   }

	private void logChanges(TemplateDetails t_form, TemplateDetails oldTemplate)
	{
		StringBuilder log = new StringBuilder();
		if (oldTemplate != null)
		{
			log.append("Zmenena sablona: ").append(t_form.getTempName());
		   BeanDiff diff = new BeanDiff().setNew(t_form).setOriginal(oldTemplate).
		   	blacklist("footerDocData", "pocetPouziti", "headerDocData", "menuDocData", "objectADocData", "objectBDocData", "objectCDocData", "objectDDocData");
		   log.append('\n').append(new BeanDiffPrinter(diff));

		   Adminlog.add(Adminlog.TYPE_TEMPLATE_UPDATE, log.toString(), t_form.getTempId(), 0);
		}
		else
		{
			log.append("Vytvorena sablona: ").append(t_form.getTempName());
			Adminlog.add(Adminlog.TYPE_TEMPLATE_INSERT, log.toString(), t_form.getTempId(), 0);
		}
	}

   /**
    * Odfiltruje sablony podla prav pouzivatela (pristupne adresare)
    * @param user
    * @param allTemps
    * @return
    */
   public static List<TemplateDetails> filterTemplatesByUser(UserDetails user, List<TemplateDetails> allTemps)
	{
		List<TemplateDetails> ret = new ArrayList<>();

		GroupsDB groupsDB = GroupsDB.getInstance();

		int[] userEditableGroups = groupsDB.expandGroupIdsToChilds(Tools.getTokensInt(user.getEditableGroups(), ","), true);
		if ((userEditableGroups == null || userEditableGroups.length<1)) return allTemps;

		for (TemplateDetails temp : allTemps)
		{
			//toto je automaticky vytvorena device sablona
			if (temp.getTempId()<1) continue;

			boolean pridaj = false;
			if (temp.getAvailableGroupsInt().length==0)
			{
				pridaj = true;
			}
			else if (userEditableGroups.length>0)
			{
				if (Tools.containsOneItem(temp.getAvailableGroupsInt(), userEditableGroups)) pridaj = true;
			}

			if (pridaj) ret.add(temp);
		}

		return ret;
	}

    /**
    * Vrati zoznam pouzitych jazykov v sablonach kvoli filtrovaniu
    */
    public static List<String> getDistinctLngs()
	{
   		return new SimpleQuery().forListString("SELECT DISTINCT lng FROM templates");
	}

   /**
    * Vrati zoznam ulozenych sablon, aby sa nezobrazovali automaticky generovane device templaty v zozname sablon
    * @return
    */
   public List<TemplateDetails> getTemplatesSaved()
   {
   	List<TemplateDetails> all = new ArrayList<>();
   	for (TemplateDetails t : getTemplates())
   	{
   		if (t.getTempId()<1) continue;

   		all.add(t);
   	}

   	return all;
   }

   public boolean remove(int tempId) {

       Connection db_conn = null;
       PreparedStatement ps = null;
       try {
           db_conn = DBPool.getConnection();
           //over ci existuje
           TemplateDetails temp1 = TemplatesDB.getInstance().getTemplate(tempId);
           if (temp1 != null) {
               ps = db_conn.prepareStatement("DELETE FROM templates WHERE temp_id=?");
               ps.setInt(1, tempId);
               ps.execute();
               ps.close();

               Adminlog.add(Adminlog.TYPE_TEMPLATE_DELETE, "Delete template :" + " name= " + temp1.getTempName(), temp1.getTempId(), -1);
               return true;
           }
       } catch (Exception ex) {
           sk.iway.iwcm.Logger.error(ex);
       } finally {
           try
           {
               if (db_conn != null)
                   db_conn.close();
               if (ps != null)
                   ps.close();
           }
           catch (Exception ex2)
           {
			//
           }
       }
       return false;
   }

	public static String getDocData(int docId) {
		if (docId > 0) {
			DocDetails doc = DocDB.getInstance().getDocAndAddToCacheIfNot(docId);
			if (doc != null) {
				return doc.getData();
			}
		}
		return "";
	}
}
