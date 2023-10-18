package sk.iway.iwcm.calendar;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;

/**
 *  praca s tabulkou calendar_types
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       not attributable
 *@version      1.0
 *@created      Utorok, 2003, júl 22
 *@modified     $Date: 2003/07/23 15:03:11 $
 */

public class EventTypeDB
{
   private EventTypeDB() {
      //private constructor
   }

   /**
    *  meno tabulky nad ktorou pracujem
    */
   protected static final String DB_NAME = "calendar_types";

   /**
    *  vymazem typ (!iba ak ho nepouziva nijaky event (tabulka calendar)!)
    *  typ nesmie byt zakladnym typom (type_id <= 5)
    *
    *@param  typeId   Description of the Parameter
    *@param  request  Description of the Parameter
    *@return          vratim pocet zmazanych zaznamov, (ak je to rozdne od 1 ->
    *      nastala chyba)
    */
   public static int deleteType(int typeId, HttpServletRequest request)
   {
   	Cache c = Cache.getInstance();
   	String cacheKey = "sk.iway.iwcm.calendar.EventTypeDB.domain=" + CloudToolsForCore.getDomainId();

      if (c.getObject(cacheKey) != null) c.removeObject(cacheKey);

      int ret = -1;
      if (typeId <= 0)
      {
         return -1;
      }

      if (typeId <= 5)
      {
        //zakladny typ -> nemazem
        return -3;
      }

      java.sql.Connection db_conn = null;
      PreparedStatement ps = null;
      ResultSet rs = null;
      try
      {
         db_conn = DBPool.getConnection(request);

         String sql = "SELECT * FROM calendar WHERE type_id=" + typeId +CloudToolsForCore.getDomainIdSqlWhere(true);
         ps = db_conn.prepareStatement(sql);
         rs = ps.executeQuery();
         if (rs.next())
         {
           //nemozem mazat pretoze v tabulke calendar je zaznam ktory pouziva
           //tento typ
           ret = -2;
         }
         else
         {
           ps.close();

           sql = "DELETE FROM " + DB_NAME + " WHERE type_id=" + typeId +CloudToolsForCore.getDomainIdSqlWhere(true);
           ps = db_conn.prepareStatement(sql);
           ret = ps.executeUpdate();
         }
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
            {
               ps.close();
            }
            if (db_conn != null)
            {
               db_conn.close();
            }
         }
         catch (Exception ex)
         {
         }
      }

      return ret;
   }


   public static int updateType(int typeId, String name, HttpServletRequest request)
   {
   	if (typeId < -1 || Tools.isEmpty(name))
      {
         return -1;
      }
   	EventTypeDetails eventType = new EventTypeDetails();
   	eventType.setTypeId(typeId);
   	eventType.setName(name);
   	eventType.setSchvalovatelId(-1);
   	return updateType(eventType);
   }

   /**
    *  add/update noveho typu
    *
    *@param  form     Description of the Parameter
    *@param  request  Description of the Parameter
    *@return          0 ... OK, inak chyba
    */
   public static int updateType(EventTypeDetails eventType)
   {
   	Cache c = Cache.getInstance();
   	String cacheKey = "sk.iway.iwcm.calendar.EventTypeDB.domain=" + CloudToolsForCore.getDomainId();

      if (c.getObject(cacheKey) != null) c.removeObject(cacheKey);

      int ret = -1;
      if (eventType.getTypeId() < -1 || Tools.isEmpty(eventType.getName()))
      {
         return ret;
      }

      java.sql.Connection db_conn = null;
      PreparedStatement ps = null;
      try
      {
         String sql;
         if (eventType.getTypeId() != -1)
         {
            sql = "UPDATE " + DB_NAME + " SET name=?,schvalovatel_id=? WHERE type_id=" + eventType.getTypeId() + " AND domain_id=?";
         }
         else
         {
            sql = "INSERT INTO " + DB_NAME + " (name,schvalovatel_id,domain_id) VALUES (?,?,?)";
         }

         db_conn = DBPool.getConnection();
         ps = db_conn.prepareStatement(sql);
         ps.setString(1, eventType.getName());
         ps.setInt(2, eventType.getSchvalovatelId());
         ps.setInt(3, CloudToolsForCore.getDomainId());
         ps.executeUpdate();
         ret = 0;
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
            {
               ps.close();
            }
            if (db_conn != null)
            {
               db_conn.close();
            }
         }
         catch (Exception ex)
         {
         }
      }

      return ret;
   }

   public static List<EventTypeDetails> getTypes(HttpServletRequest request)
   {
   	return getTypes();
   }

   @SuppressWarnings("unchecked")
   public static List<EventTypeDetails> getTypes()
   {
		List<EventTypeDetails> ret = null;

		Cache c = Cache.getInstance();
		int cacheInMinutes = Constants.getInt("EventTypeDBCacheInMinutes");
   	String cacheKey = "sk.iway.iwcm.calendar.EventTypeDB.domain=" + CloudToolsForCore.getDomainId();

		ret = (List<EventTypeDetails>)c.getObject(cacheKey);
		if (ret != null)
			return ret;
		else
			ret = new ArrayList<>();

      java.sql.Connection db_conn = null;
      PreparedStatement ps = null;
      ResultSet rs = null;
      try
      {
         db_conn = DBPool.getConnection();
         String sql = "SELECT * FROM "+DB_NAME+" WHERE "+CloudToolsForCore.getDomainIdSqlWhere(false)+" ORDER BY type_id";
         ps = db_conn.prepareStatement(sql);
         rs = ps.executeQuery();
         while (rs.next())
         {
            ret.add(fillEventTypeDetails(rs));
         }
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
            {
               rs.close();
            }
            if (ps != null)
            {
               ps.close();
            }
            if (db_conn != null)
            {
               db_conn.close();
            }
         }
         catch (Exception ex)
         {
         	sk.iway.iwcm.Logger.error(ex);
         }
      }

	   c.setObjectSeconds(cacheKey, ret , cacheInMinutes*60, true);

      return (ret);
   }

   public static String getTypeName(int typeId, HttpServletRequest request)
   {
      String ret = "";

      java.sql.Connection db_conn = null;
      PreparedStatement ps = null;
      ResultSet rs = null;
      try
      {
         db_conn = DBPool.getConnection(request);
         String sql = "SELECT name FROM "+DB_NAME+" WHERE type_id="+typeId +CloudToolsForCore.getDomainIdSqlWhere(true);
         ps = db_conn.prepareStatement(sql);
         rs = ps.executeQuery();

         if (rs.next())
         {
            ret = DB.getDbString(rs, "name");
         }
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
            {
               rs.close();
            }
            if (ps != null)
            {
               ps.close();
            }
            if (db_conn != null)
            {
               db_conn.close();
            }
         }
         catch (Exception ex)
         {
         	sk.iway.iwcm.Logger.error(ex);
         }
      }

      return (ret);
   }

   /**
    * naplni EventTypeDetails z ResultSetu
    * @param rs
    * @return
    */
   private static EventTypeDetails fillEventTypeDetails(ResultSet rs)
   {
   	EventTypeDetails event = new EventTypeDetails();
   	try
		{
			event.setTypeId(rs.getInt("type_id"));
			event.setName(rs.getString("name"));
			event.setSchvalovatelId(rs.getInt("schvalovatel_id"));
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		return event;
   }

   /**
    * vrati typ udalosti na zaklade typeId
    * @param typeId
    * @return
    */
   public static EventTypeDetails getTypeById(int typeId)
   {
   	long start = Tools.getNow();
   	try
   	{
   		List<EventTypeDetails> allEvents = getTypes();
   		for (EventTypeDetails e : allEvents)
   		{
   			if (e.getTypeId()==typeId)
   			{
   				long end = Tools.getNow();
   				Logger.debug(EventTypeDB.class, "getTypeById: id="+typeId+" total="+(end-start)+" ms");
   				return e;
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
    * vrati vsetky typy ktore ma schvalovat urcity userId
    * @param userId
    * @return
    */
   public static Map<Integer, EventTypeDetails> getTypeBySchvalovatelId(int userId)
   {
   	Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		Map<Integer, EventTypeDetails> result = new Hashtable<>();
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT * FROM "+DB_NAME+" WHERE schvalovatel_id = ?"+CloudToolsForCore.getDomainIdSqlWhere(true));
			ps.setInt(1, userId);
			rs = ps.executeQuery();
			while (rs.next())
			{
				result.put(rs.getInt("type_id"), fillEventTypeDetails(rs));
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
			}
		}

		return result;
   }

   /**
    * vrati type_id posledneho zaznamu
    * @return
    */
   public static int getLastTypeId()
   {
   	Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT max(type_id) AS type_id FROM "+DB_NAME+" WHERE "+CloudToolsForCore.getDomainIdSqlWhere(false));
			rs = ps.executeQuery();
			if (rs.next())
			{
				return rs.getInt("type_id");
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
			}
		}
		return -1;
   }
}
