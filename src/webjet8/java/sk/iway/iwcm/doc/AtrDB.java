package sk.iway.iwcm.doc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.LabelValueDetails;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.stat.Column;

/**
 *  Objekt na pracu s atributmi stranky
 *
 *@Title        WebJET 4.0
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.6 $
 *@created      Streda, 2003, október 15
 *@modified     $Date: 2003/12/01 08:27:43 $
 */
public class AtrDB
{
   public static final int TYPE_STRING = 0;
   public static final int TYPE_INT = 1;
   public static final int TYPE_BOOL = 2;
   public static final int TYPE_DOUBLE = 3;

   protected AtrDB() {
      //utility class
   }

   /**
    * vrati zoznam vsetkych atributov (aj nevyplnenych) pre dane docId
    * @param docId
    * @param request
    * @return
    */
    @SuppressWarnings("unchecked")
   public static List<AtrBean> getAtributes(int docId, String group, HttpServletRequest request)
   {
   	//skus vydolovat z requestu (aby sme nemali zbytocne dotazy)
   	String key = "attributes_"+docId+"_"+group;
   	List<AtrBean> ret;
   	if (request != null)
   	{
	   	ret = (List<AtrBean>)request.getAttribute(key);
	   	if (ret != null) return(ret);
   	}

      ret = new ArrayList<>();
      Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
      try
      {
         db_conn = DBPool.getConnection();
         StringBuilder sql =  new StringBuilder("SELECT dad.*, da.doc_id, da.value_string, da.value_int, da.value_bool FROM doc_atr_def dad ").append(
                      "LEFT JOIN doc_atr da ON dad.atr_id = da.atr_id ").append(
                      "AND da.doc_id=? WHERE dad.atr_id>0 ");


         if (group!=null)
         {
         	if (group.indexOf('%')!=-1)
         	{
         		sql.append("AND dad.atr_group LIKE ?  ");
         	}
         	else if (group.indexOf(',')!=-1)
         	{
         		group = DB.removeSlashes(group);
         		StringTokenizer st = new StringTokenizer(group, ",");
         		sql.append("AND dad.atr_group IN ('").append(st.nextToken()).append('\'');
         		while (st.hasMoreTokens())
         		{
         			sql.append(", '").append(st.nextToken()).append('\'');
         		}
         		sql.append(") ");
         		group = null;
         	}
         	else
         	{
         		sql.append("AND dad.atr_group=?  ");
         	}
         }

         sql.append(CloudToolsForCore.getDomainIdSqlWhere(true, "dad")).append(" ");

         sql.append("ORDER BY dad.order_priority ");
         ps = db_conn.prepareStatement(sql.toString());
         ps.setInt(1, docId);
         if (group != null) ps.setString(2, group);
         rs = ps.executeQuery();
         AtrBean atr;
         while (rs.next())
         {
            atr = new AtrBean();
            atr.setAtrId(rs.getInt("atr_id"));
            atr.setAtrName(DB.getDbString(rs, "atr_name"));
            atr.setOrderPriority(rs.getInt("order_priority"));
            atr.setAtrDescription(DB.getDbString(rs, "atr_description"));
            atr.setAtrDefaultValue(DB.getDbString(rs, "atr_default_value"));
            atr.setAtrType(rs.getInt("atr_type"));
            atr.setAtrGroup(DB.getDbString(rs, "atr_group"));
            atr.setDocId(rs.getInt("doc_id"));
            atr.setValueString(DB.getDbString(rs, "value_string"));
            atr.setValueNumber(rs.getDouble("value_int"));
            atr.setValueBool(rs.getBoolean("value_bool"));
            atr.setTrueValue(DB.getDbString(rs, "true_value"));
            atr.setFalseValue(DB.getDbString(rs, "false_value"));
            ret.add(atr);
         }
         rs.close();
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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

      if (request != null)
      {
      	request.setAttribute(key, ret);
      }

      return(ret);
   }

   /**
    * Vrati definicie atributov v zadanej skupine
    * @param request
    * @param groupName
    * @return
    */
   public static List<AtrBean> getAttributes(HttpServletRequest request, String groupName)
   {
   	List<AtrBean> ret = new ArrayList<>();

   	Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM doc_atr_def WHERE atr_group=? "+CloudToolsForCore.getDomainIdSqlWhere(true)+" ORDER BY order_priority");
   		ps.setString(1, groupName);
   		rs = ps.executeQuery();
         AtrBean atr;
         while (rs.next())
         {
            atr = new AtrBean();
            atr.setAtrId(rs.getInt("atr_id"));
            atr.setAtrName(DB.getDbString(rs, "atr_name"));
            atr.setOrderPriority(rs.getInt("order_priority"));
            atr.setAtrDescription(DB.getDbString(rs, "atr_description"));
            atr.setAtrDefaultValue(DB.getDbString(rs, "atr_default_value"));
            atr.setAtrType(rs.getInt("atr_type"));
            atr.setAtrGroup(DB.getDbString(rs, "atr_group"));
            atr.setTrueValue(DB.getDbString(rs, "true_value"));
            atr.setFalseValue(DB.getDbString(rs, "false_value"));
            ret.add(atr);
         }
         rs.close();
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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

   	return(ret);
   }

   /**
    * Vrati List skupin, ktory ako hodnoty obsahuje dalsie Listy s
    * atributmi v danej skupine
    * @param docId
    * @param request
    * @return
    */
   public static List<List<AtrBean>> getAtributes(int docId, HttpServletRequest request)
   {
      List<List<AtrBean>> ret = new ArrayList<>();
      List<AtrBean> atrs;
      for (LabelValueDetails lvd : getAtrGroups(request))
      {
         //Logger.println(this,"lvd->"+lvd.getLabel());
         //ok mame grupu
         atrs = getAtributes(docId, lvd.getLabel(), request);
         ret.add(atrs);
      }

      return(ret);
   }

   /**
    * vrati atribut atrId
    * @param atrId
    * @param request
    * @return
    */
   public static AtrBean getAtrDef(int atrId, HttpServletRequest request)
   {
      AtrBean atr = null;
      Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
      try
      {
         db_conn = DBPool.getConnection(request);
         String sql = "SELECT * FROM doc_atr_def WHERE atr_id=?"+CloudToolsForCore.getDomainIdSqlWhere(true);
         ps = db_conn.prepareStatement(sql);
         ps.setInt(1, atrId);
         rs = ps.executeQuery();
         if (rs.next())
         {
            atr = new AtrBean();
            atr.setAtrId(rs.getInt("atr_id"));
            atr.setAtrName(DB.getDbString(rs, "atr_name"));
            atr.setOrderPriority(rs.getInt("order_priority"));
            atr.setAtrDescription(DB.getDbString(rs, "atr_description"));
            atr.setAtrDefaultValue(DB.getDbString(rs, "atr_default_value"));
            atr.setAtrType(rs.getInt("atr_type"));
            atr.setAtrGroup(DB.getDbString(rs, "atr_group"));
            atr.setTrueValue(DB.getDbString(rs, "true_value"));
            atr.setFalseValue(DB.getDbString(rs, "false_value"));
         }
         rs.close();
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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
      return(atr);
   }

   /**
    * Ziska definiciu atributu podla mena a skupiny
    * @param atrName
    * @param atrGroup	- ak je null, hladam vo vsetkych skupinach
    * @param request
    * @return
    */
   public static AtrBean getAtrDef(String atrName, String atrGroup, HttpServletRequest request)
   {
      AtrBean atr = null;
      Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
      try
      {
         db_conn = DBPool.getConnection(request);
         String sql = "SELECT * FROM doc_atr_def WHERE atr_name=?"+CloudToolsForCore.getDomainIdSqlWhere(true);
         if(Tools.isNotEmpty(atrGroup))
         {
         	sql += " AND atr_group=?";
         }
         ps = db_conn.prepareStatement(sql);
         ps.setString(1, atrName);
         if(Tools.isNotEmpty(atrGroup))
         {
         	ps.setString(2, atrGroup);
         }
         rs = ps.executeQuery();
         if (rs.next())
         {
            atr = new AtrBean();
            atr.setAtrId(rs.getInt("atr_id"));
            atr.setAtrName(DB.getDbString(rs, "atr_name"));
            atr.setOrderPriority(rs.getInt("order_priority"));
            atr.setAtrDescription(DB.getDbString(rs, "atr_description"));
            atr.setAtrDefaultValue(DB.getDbString(rs, "atr_default_value"));
            atr.setAtrType(rs.getInt("atr_type"));
            atr.setAtrGroup(DB.getDbString(rs, "atr_group"));
            atr.setTrueValue(DB.getDbString(rs, "true_value"));
            atr.setFalseValue(DB.getDbString(rs, "false_value"));
         }
         rs.close();
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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

      return(atr);
   }

   /**
    * vrati zoznam skupin atributov
    * @param request
    * @return
    */
   public static List<LabelValueDetails> getAtrGroups(HttpServletRequest request)
   {
      List<LabelValueDetails> groups = new ArrayList<>();
      LabelValueDetails lvd;
      Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
      try
      {
         db_conn = DBPool.getConnection(request);
         String sql = "SELECT distinct atr_group FROM doc_atr_def "+CloudToolsForCore.getDomainIdSqlWhere(true)+" ORDER BY atr_group";
         ps = db_conn.prepareStatement(sql);
         rs = ps.executeQuery();
         while (rs.next())
         {
            lvd = new LabelValueDetails();
            lvd.setLabel(DB.getDbString(rs, "atr_group"));
            lvd.setValue(lvd.getLabel());
            groups.add(lvd);
         }
         rs.close();
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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

      return(groups);
   }

   /**
    * vrati zoznam stranok v danom adresari a danej skupine atributov, pricom kazdy bean ma array list so zoznamom
    * atributov
    * @param dirId - id adresara vo webjete
    * @param includeSub - ak true, vratane podadresarov
    * @param group - meno skupiny atributov
    * @param request
    * @return
    */
   public static List<AtrDocBean> getAtributesTable(int dirId, boolean includeSub, String group, HttpServletRequest request)
   {
      List<AtrDocBean> rows = new ArrayList<>();

      if (group!=null && Tools.isEmpty(group)) group = null;

      Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
      try
      {
         String dirs = null;

         if (dirId>0)
         {
            if (includeSub)
            {
               //najdi podadresare toho adresara
               GroupsDB groupsDB = GroupsDB.getInstance();
               for (GroupDetails groupDetails : groupsDB.getGroupsTree(dirId, true, false))
               {
                  if (groupDetails != null)
                  {
                     if (dirs == null)
                     {
                        dirs = Integer.toString(groupDetails.getGroupId());
                     }
                     else
                     {
                        dirs += "," + groupDetails.getGroupId();
                     }
                  }
               }
            }
            else
            {
               dirs = Integer.toString(dirId);
            }
         }

         db_conn = DBPool.getConnection(request);
         StringBuilder sql = new StringBuilder("SELECT d.*, dad.*, da.value_string, da.value_int, da.value_bool ").append(
                      "FROM documents d, doc_atr_def dad ").append(
                      "LEFT JOIN doc_atr da ON da.atr_id = dad.atr_id ").append(
                      "WHERE d.doc_id=da.doc_id AND d.available="+DB.getBooleanSql(true)+" ");
         if (group!=null)
         {
         	if (group.indexOf('%')!=-1)
         	{
         		sql.append("AND dad.atr_group LIKE ? ");
         	}
         	else if (group.indexOf(',')!=-1)
         	{
         		group = DB.removeSlashes(group);
         		StringTokenizer st = new StringTokenizer(group, ",");
         		sql.append("AND dad.atr_group IN ('").append(st.nextToken()).append('\'');
         		while (st.hasMoreTokens())
         		{
         			sql.append(", '").append(st.nextToken()).append('\'');
         		}
         		sql.append(") ");
         		group = null;
         	}
         	else
         	{
         		sql.append("AND dad.atr_group=? ");
         	}
         }

         if (dirs!=null)
         {
            sql.append("AND d.group_id IN (").append(dirs).append(") ");
         }

         String sqlWhere = (String)request.getAttribute("getAtributesTableSqlWhere");
         if (Tools.isNotEmpty(sqlWhere)) {
            sql.append(sqlWhere).append(" ");
            request.removeAttribute("getAtributesTableSqlWhere");
         }

         sql.append(CloudToolsForCore.getDomainIdSqlWhere(true, "dad")).append(" ");

         sql.append("ORDER BY d.doc_id, d.sort_priority, d.title, dad.order_priority ");

         Logger.debug(AtrDB.class, "sql:"+sql);

         ps = db_conn.prepareStatement(sql.toString());
         //ps.setInt(1, docId);
         if (group!=null)
         {
            ps.setString(1, group);
         }
         rs = ps.executeQuery();
         AtrBean atr = null;
         int lastDocId = -1;
         AtrDocBean atrDocBean = null;
         while (rs.next())
         {
            int docId = rs.getInt("doc_id");

            atr = new AtrBean();
            atr.setDocId(docId);
            atr.setAtrId(rs.getInt("atr_id"));
            atr.setAtrName(DB.getDbString(rs, "atr_name"));
            atr.setOrderPriority(rs.getInt("order_priority"));
            atr.setAtrDescription(DB.getDbString(rs, "atr_description"));
            atr.setAtrDefaultValue(DB.getDbString(rs, "atr_default_value"));
            atr.setAtrType(rs.getInt("atr_type"));
            atr.setAtrGroup(DB.getDbString(rs, "atr_group"));
            atr.setValueString(DB.getDbString(rs, "value_string"));
            atr.setValueNumber(rs.getDouble("value_int"));
            atr.setValueBool(rs.getBoolean("value_bool"));
            atr.setTrueValue(DB.getDbString(rs, "true_value"));
            atr.setFalseValue(DB.getDbString(rs, "false_value"));

            if (lastDocId == -1)
            {
               //zaciatocna inicializacia
               lastDocId = atr.getDocId();

               atrDocBean = new AtrDocBean();
               DocDB.getDocDetails(rs, atrDocBean, false, true);
            }

            if (lastDocId == atr.getDocId())
            {
               if (atrDocBean!=null) atrDocBean.addAtr(atr);
            }
            else
            {
               rows.add(atrDocBean);

               atrDocBean = new AtrDocBean();
               DocDB.getDocDetails(rs, atrDocBean, false, true);

               atrDocBean.addAtr(atr);

               lastDocId = atr.getDocId();
            }
         }

         if (atrDocBean != null)
         {
            rows.add(atrDocBean);
         }

         rs.close();
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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

      return(rows);
   }

   /**
    * Vyhladavanie v strankach podla atributov
    * @param dirId
    * @param includeSub
    * @param group
    * @param request
    * @return
    */
   public static List<AtrDocBean> search(int dirId, boolean includeSub, String group, HttpServletRequest request)
   {
   	//Logger.println(this,"-> VYHLADAVAM grupa="+group);
      List<AtrDocBean> retTable = new ArrayList<>();

      //odfiltruj vysledky
      Enumeration<String> params;
      String param;
      String paramValues[];
      boolean mustRemove;
      List<AtrDocBean> list = getAtributesTable(dirId, includeSub, group, request);
      list = removeMultigroup(request, list);

      for (AtrDocBean row : list)
      {
         Logger.debug(AtrDB.class, "TESTING: "+row.getTitle());
         params = request.getParameterNames();
         mustRemove = false;
         //Logger.println(this,"testing doc:" + row.getDocId()+" "+row.getTitle());
         while (params.hasMoreElements() && mustRemove==false)
         {
            param = params.nextElement();
            if (param.startsWith("atrs_"))
            {
               //ok mame parameter pre vyhladavanie
               paramValues = request.getParameterValues(param);
               if (paramValues!=null && paramValues.length>0)
               {
                  //rozparsuj to
                  if (row.mustRemove(param, paramValues))
                  {
                     //Logger.println(this,"REMOVING: ");
                  	Logger.debug(AtrDB.class, "removing");
                     mustRemove = true;
                  }
               }
            }
         }
         if (mustRemove==false)
         {
         	Logger.debug(AtrDB.class, "adding: "+row.getTitle());
            retTable.add(row);
         }
      }

      return(retTable);
   }

    public static List<AtrDocBean> removeMultigroup(HttpServletRequest request, List<AtrDocBean> list) {

        Map<Integer, List<Integer>> multigroupMappingsIds = readMultigroupMappings(request);
        Set<Integer> addedGroupIds = new HashSet<>();

        List<AtrDocBean> result = list.stream().filter(a -> {
            int masterId = getMasterId(multigroupMappingsIds, a.getDocId());
            if (masterId > 0 && addedGroupIds.contains(masterId)) {
                return false;
            }

            if (masterId > 0) {
                addedGroupIds.add(masterId);
            }

            return true;
        }).collect(Collectors.toList());


        return result;
    }

   /**
    * vrati zoznam vsetkych atributov
    * @param request
    * @return
    */
   public static List<AtrBean> getAllAttributes(HttpServletRequest request)
   {
   	List<AtrBean> ret = new ArrayList<>();
   	try {
   		Connection db_conn = DBPool.getConnection();
   		PreparedStatement ps = db_conn.prepareStatement("SELECT * FROM doc_atr_def "+CloudToolsForCore.getDomainIdSqlWhere(true)+" ORDER BY atr_group ASC, atr_name ASC");
   		ResultSet rs = ps.executeQuery();
         AtrBean atr;
         while (rs.next())
         {
            atr = new AtrBean();
            atr.setAtrId(rs.getInt("atr_id"));
            atr.setAtrName(DB.getDbString(rs, "atr_name"));
            if(atr.getAtrName().equals("")) atr.setAtrName("&nbsp;");
            atr.setOrderPriority(rs.getInt("order_priority"));
            atr.setAtrDescription(DB.getDbString(rs, "atr_description"));
            if(atr.getAtrDescription().equals("")) atr.setAtrDescription("&nbsp;");
            atr.setAtrDefaultValue(DB.getDbString(rs, "atr_default_value"));
            if(atr.getAtrDefaultValue().equals("")) atr.setAtrDefaultValue("&nbsp;");
            atr.setAtrType(rs.getInt("atr_type"));
            atr.setAtrGroup(DB.getDbString(rs, "atr_group"));
            atr.setTrueValue(DB.getDbString(rs, "true_value"));
            if(atr.getTrueValue().equals("")) atr.setTrueValue("&nbsp;");
            atr.setFalseValue(DB.getDbString(rs, "false_value"));
            if(atr.getFalseValue().equals("")) atr.setFalseValue("&nbsp;");
            ret.add(atr);
         }
         rs.close();
         ps.close();
         db_conn.close();
   	} catch(Exception ex) {sk.iway.iwcm.Logger.error(ex);}
   	if (request != null) request.setAttribute("AtrBean", ret);
   	return(ret);
   }

   /**
    * Vrati zoznam vsetkych atributov v zozname objektov typu AtrBean
    *
    * @return
    */
   public static List<AtrBean> getAllAttributes()
   {
   	return(AtrDB.getAttributes(null, -1, null));
   }

   /**
    * Vrati zoznam vsetkych atributov, ktore vyhovuju vstupnym filtracnym podmienkam
    *
    * @param filterFulltext	cast textu, ktora sa vyhladava v nazve a opise atributu
    * @param filterTyp			typ atributu
    * @param filterSkupina		skupina atributu
    * @return
    */
   public static List<AtrBean> getAttributes(String filterFulltext, int filterTyp, String filterSkupina)
   {
   	List<Object> params = new ArrayList<>();

   	StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM doc_atr_def WHERE atr_id > 0 ");

   	if (filterTyp > -1)
   	{
   		sql.append(" AND atr_type = ?");
   		params.add(filterTyp);
   	}

   	if (Tools.isNotEmpty(filterFulltext))
   	{
   		sql.append(" AND (atr_name LIKE ? OR atr_description LIKE ?) ");
   		params.add("%" + filterFulltext + "%");
   		params.add("%" + filterFulltext + "%");
   	}

   	if (Tools.isNotEmpty(filterSkupina))
   	{
   		sql.append(" AND atr_group = ?");
   		params.add(filterSkupina);
   	}

      sql.append(CloudToolsForCore.getDomainIdSqlWhere(true)).append(" ");

   	sql.append(" ORDER BY atr_group ASC, atr_name ASC");

      List<AtrBean> atrs = new ComplexQuery().setSql(sql.toString()).setParams(params.toArray()).list(new Mapper<AtrBean>()
      {
      	@Override
         public AtrBean map(ResultSet rs) throws SQLException
         {
         	AtrBean atr = new AtrBean();

         	atr.setAtrId(rs.getInt("atr_id"));
            atr.setAtrName(DB.getDbString(rs, "atr_name"));
            if(atr.getAtrName().equals(""))
            	atr.setAtrName("&nbsp;");
            atr.setOrderPriority(rs.getInt("order_priority"));
            atr.setAtrDescription(DB.getDbString(rs, "atr_description"));
            if(atr.getAtrDescription().equals(""))
            	atr.setAtrDescription("&nbsp;");
            atr.setAtrDefaultValue(DB.getDbString(rs, "atr_default_value"));
            if(atr.getAtrDefaultValue().equals(""))
            	atr.setAtrDefaultValue("&nbsp;");
            atr.setAtrType(rs.getInt("atr_type"));
            atr.setAtrGroup(DB.getDbString(rs, "atr_group"));
            atr.setTrueValue(DB.getDbString(rs, "true_value"));
            if(atr.getTrueValue().equals(""))
            	atr.setTrueValue("&nbsp;");
            atr.setFalseValue(DB.getDbString(rs, "false_value"));
            if(atr.getFalseValue().equals(""))
            	atr.setFalseValue("&nbsp;");

         	return atr;
         }
      });

		return atrs;
	}

   /**
    * vrati zoznam vsetkych atributov
    * @param request
    */
   public static void updateAttribute(AtrBean attribute, HttpServletRequest request) {
   	Connection db_conn = null;
		PreparedStatement ps = null;
   	try {
   		db_conn = DBPool.getConnection(request);
   		ps = db_conn.prepareStatement("UPDATE doc_atr_def SET ");
   		String [] atrStlpceString= {"atr_name", "atr_description","atr_default_value",
   					"atr_group", "true_value", "false_value"};
   		String [] atrHodnotyString= {attribute.getAtrName(),attribute.getAtrDescription(),
   					attribute.getAtrDefaultValue(), attribute.getAtrGroup(),
   					attribute.getTrueValue(), attribute.getFalseValue()};
   		String [] atrStlpceInt= {"order_priority", "atr_type"};
   		int [] atrHodnotyInt= {attribute.getOrderPriority(), attribute.getAtrType()};


   		for(int i=0; i<atrStlpceString.length;i++) {
   			ps=db_conn.prepareStatement("UPDATE doc_atr_def SET "+atrStlpceString[i]+" = ? WHERE atr_id = ?"+CloudToolsForCore.getDomainIdSqlWhere(true));
   			ps.setString(1,atrHodnotyString[i]);
   			ps.setInt(2, attribute.getAtrId());
   			ps.executeUpdate();

   		}
   		for(int i=0; i<atrStlpceInt.length;i++) {
   			ps=db_conn.prepareStatement("UPDATE doc_atr_def SET "+atrStlpceInt[i]+" = ? WHERE atr_id = ?"+CloudToolsForCore.getDomainIdSqlWhere(true));
   			ps.setInt(1,atrHodnotyInt[i]);
   			ps.setInt(2, attribute.getAtrId());
   			ps.executeUpdate();

   		}

         ps.close();
         db_conn.close();
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
    * zmaze atribut
    * @param request
    */
   public static void deleteAttribute(int id, HttpServletRequest request) {
   	Connection db_conn = null;
		PreparedStatement ps = null;
   	try {
   		db_conn = DBPool.getConnection(request);
   		ps = db_conn.prepareStatement("DELETE FROM doc_atr_def WHERE atr_id = ? "+CloudToolsForCore.getDomainIdSqlWhere(true));
   		ps.setInt(1, id);
   		ps.executeUpdate();
         ps.close();
         db_conn.close();
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
    * prida atribut
    * @param request
    */
   public static void insertAttribute(AtrBean attribute, HttpServletRequest request) {
   	Connection db_conn = null;
		PreparedStatement ps = null;
   	try {
   		db_conn = DBPool.getConnection(request);
   		ps = db_conn.prepareStatement(
   					"INSERT INTO doc_atr_def (atr_name, order_priority, atr_description,atr_default_value," +
   					"atr_type, atr_group, true_value, false_value, domain_id) VALUES(?,?,?,?,?,?,?,?,?)");

   		if(attribute.getAtrName()==null) ps.setNull(1, Types.VARCHAR);
   		else ps.setString(1, attribute.getAtrName());

   		ps.setInt(2, attribute.getOrderPriority());

   		if(attribute.getAtrDescription()==null) ps.setNull(3, Types.VARCHAR);
   		else ps.setString(3, attribute.getAtrDescription());

   		if(attribute.getAtrDefaultValue()==null) ps.setNull(4, Types.VARCHAR);
   		else ps.setString(4, attribute.getAtrDefaultValue());

   		ps.setInt(5, attribute.getAtrType());
   		ps.setString(6, attribute.getAtrGroup());

   		if(attribute.getTrueValue()==null) ps.setNull(7, Types.VARCHAR);
   		else ps.setString(7, attribute.getTrueValue());

   		if(attribute.getFalseValue()==null) ps.setNull(8, Types.VARCHAR);
   		else ps.setString(8, attribute.getFalseValue());

         ps.setInt(9, CloudToolsForCore.getDomainId());

   		ps.executeUpdate();
         ps.close();
         db_conn.close();
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

   public static Map<String, AtrBean> getAttributeMap(int docid,String attributeGroup,HttpServletRequest request)
   {
   	Map<String,AtrBean> attributeMap = new HashMap<>();
   	List<?> attributes = getAtributes(docid, attributeGroup, request);

   	for (Object object : attributes)
   	{
			AtrBean attribute = (AtrBean)object;
			if (attribute.getAtrGroup().equals(attributeGroup))
				attributeMap.put( attribute.getAtrName() , attribute);
		}
   	return attributeMap;
   }

   /**
    * Vrati zoznam vsetkych typov atributov, ktore sa nachadzaju v tabulke doc_atr_def ako objekty Column, kde <br />
    * 	getIntColumn1() je ciselna reprezentacia typu (STRING = 0, INT = 1, BOOL = 2, DOUBLE = 3) a <br />
    * 	getColumn1() vrati nazov typu atributu ("STRING", "INT", "BOOL", "DOUBLE")
    *
    * @return
    */
   public static List<Column> getDistinctTypes()
	{
		List<Column> retList = new ArrayList<>();
		String[] types = {"STRING", "INT", "BOOL", "DOUBLE"};

   	try
   	{
         @SuppressWarnings("unchecked")
   		List<Number> tempList = new SimpleQuery().forList("SELECT DISTINCT atr_type FROM doc_atr_def"+CloudToolsForCore.getDomainIdSqlWhere(true));

   		for (Number type : tempList)
			{
				Column retType = new Column();
				retType.setIntColumn1(type.intValue());
				retType.setColumn1(types[type.intValue()]);

				retList.add(retType);
			}
   	}
   	catch (Exception e)
   	{
   		sk.iway.iwcm.Logger.error(e);
   	}

   	return retList;
	}

   /**
    * Vrati hodnotu atributu podla id stranky a id atributu
    * @param docId id stranky
    * @param atrId id atributu
    * @param request
    * @return AtrBean
    */
   public static AtrBean getAtribute(int docId, int atrId, HttpServletRequest request)
   {
      AtrBean atr = null;
      Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
      try
      {
         db_conn = DBPool.getConnection(request);
         String sql = "SELECT * FROM doc_atr WHERE doc_id=? and atr_id=?";
         ps = db_conn.prepareStatement(sql);
         ps.setInt(1, docId);
         ps.setInt(2, atrId);
         rs = ps.executeQuery();
         if (rs.next())
         {
            atr = new AtrBean();
            atr.setAtrId(rs.getInt("atr_id"));
            atr.setValueString(DB.getDbString(rs, "value_string"));
            atr.setValueInt(rs.getInt("value_int"));
            atr.setValueBool(rs.getBoolean("value_bool"));
         }
         rs.close();
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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
      return(atr);
   }

   public static class MultigroupMapping {
       private int docId;
       private int masterId;

       public int getDocId() {
           return docId;
       }

       public void setDocId(int docId) {
           this.docId = docId;
       }

       public int getMasterId() {
           return masterId;
       }

       public void setMasterId(int masterId) {
           this.masterId = masterId;
       }
   }

   private static Map<Integer, List<Integer>> readMultigroupMappings(HttpServletRequest request) {
       Map<Integer, List<Integer>> results = new HashMap<>();
       Connection db_conn = null;
       PreparedStatement ps = null;
       ResultSet rs = null;
       try
       {
           db_conn = DBPool.getConnection(request);
           String sql = "SELECT * FROM multigroup_mapping";
           ps = db_conn.prepareStatement(sql);
           rs = ps.executeQuery();
           if (rs.next())
           {
               int masterId = rs.getInt("master_id");
               int docId = rs.getInt("doc_id");
               List<Integer> ids = results.containsKey(masterId) ? results.get(masterId) : new ArrayList<>();
               ids.add(docId);

               results.put(masterId, ids);
           }

           rs.close();
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
               sk.iway.iwcm.Logger.error(ex2);
           }
       }
       return (results);
   }

    private static int getMasterId(Map<Integer, List<Integer>> map, int docId) {
        if (map.containsKey(docId)) {
            return docId;
        }

        for (Map.Entry<Integer, List<Integer>> entry : map.entrySet()) {
            int key = entry.getKey();
            List<Integer> value = entry.getValue();

            if (value.stream().anyMatch(id -> id == docId)) {
                return key;
            }
        }

        return 0;
    }
}
