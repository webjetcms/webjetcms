package sk.iway.iwcm.doc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.database.SimpleQuery;

/**
 *  MultigroupMappingDB.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: rusho $
 *@version      $Revision: 1.3 $
 *@created      Date: 24.8.2010 11:03:45
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class MultigroupMappingDB
{
	public static void newMultigroupMapping(int docId, int masterId, boolean redirect)
	{
		MultigroupMapping mapping = new MultigroupMapping();
		mapping.setDocId(docId);
		mapping.setMasterId(masterId);
		mapping.setRedirect(redirect);
		saveMultigroupMapping(mapping);
	}

	public static void saveMultigroupMapping(MultigroupMapping mapping)
	{
		try
		{
			if(mapping != null && mapping.getDocId() > 0)
			{
				SimpleQuery sq = new SimpleQuery();
				String sql = "SELECT count(*) FROM multigroup_mapping WHERE doc_id = ?";
				int c = sq.forInt(sql, mapping.getDocId());
				if(c == 0)
				{
					sql = "INSERT INTO multigroup_mapping (doc_id, master_id, redirect) VALUES (?, ?, ?)";
					sq.execute(sql, mapping.getDocId(), mapping.getMasterId(), mapping.isRedirect());
				}
				else
				{
					sql = "UPDATE multigroup_mapping SET master_id=?, redirect=? WHERE doc_id = ?";
					sq.execute(sql, mapping.getMasterId(), mapping.isRedirect(), mapping.getDocId());
				}
			}
		}
		catch(Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
	}


	public static MultigroupMapping getMultigroupMapping(int docId)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		MultigroupMapping mapping = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT master_id, redirect FROM multigroup_mapping WHERE doc_id = ?");
			ps.setInt(1, docId);
			rs = ps.executeQuery();
			while (rs.next())
			{
				mapping = new MultigroupMapping();
				mapping.setDocId(docId);
				mapping.setMasterId(rs.getInt("master_id"));
				mapping.setRedirect(rs.getBoolean("redirect"));
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

		return mapping;
	}

	public static List<MultigroupMapping> getSlaveMappings(int masterId)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		List<MultigroupMapping> mappingList = new ArrayList<MultigroupMapping>();
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT doc_id, master_id, redirect FROM multigroup_mapping WHERE master_id = ?");
			ps.setInt(1, masterId);
			rs = ps.executeQuery();
			while (rs.next())
			{
				MultigroupMapping mapping = new MultigroupMapping();
				mapping.setDocId(rs.getInt("doc_id"));
				mapping.setMasterId(rs.getInt("master_id"));
				mapping.setRedirect(rs.getBoolean("redirect"));
				mappingList.add(mapping);
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

		return mappingList;

	}

	public static List<Integer> getSlaveDocIds(int masterId)
	{
		List<Integer> slaveDocIds = new ArrayList<Integer>();
		try
		{
			SimpleQuery sq = new SimpleQuery();
			String sql = "";
			sql = "SELECT doc_id FROM multigroup_mapping WHERE master_id = ?";
			for(Object o : sq.forList(sql, masterId))
			{
				if(o instanceof Number && ((Number)o).intValue() > 0)
				{
					slaveDocIds.add(((Number)o).intValue());
				}

			}
		}
		catch(Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		return slaveDocIds;
	}

	public static int getMasterDocId(int docId)
	{
		try
		{
			SimpleQuery sq = new SimpleQuery();
			String sql = "";
			sql = "SELECT master_id FROM multigroup_mapping WHERE doc_id = ?";
			return sq.forInt(sql, docId);
		}
		catch(Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return -1;
	}

	/**
	 * Returns masterId for docId od docId if there is no master or it's master itself
	 * @param docId
	 * @param returnDocId
	 * @return
	 */
	public static int getMasterDocId(int docId, boolean returnDocId) {
		int masterId = MultigroupMappingDB.getMasterDocId(docId);
        if (masterId < 1) masterId = docId;
		return masterId;
	}

	public static void deleteSlaves(int masterId)
	{
		try
		{
			SimpleQuery sq = new SimpleQuery();
			String sql = "";
			sql = "DELETE FROM multigroup_mapping WHERE master_id = ?";
			sq.execute(sql, masterId);
		}
		catch(Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
	}

	 public static void deleteSlaveDocFromMapping(int docId)
	 {
		  try
		  {
				SimpleQuery sq = new SimpleQuery();
				String sql = "";
				sql = "DELETE FROM multigroup_mapping WHERE doc_id = ?";
				sq.execute(sql, docId);
		  }
		  catch(Exception e)
		  {
				sk.iway.iwcm.Logger.error(e);
		  }
	 }

	public static Map<Integer, Integer> getAllMappings()
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		Map<Integer, Integer> result = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("SELECT doc_id, master_id FROM multigroup_mapping");
			rs = ps.executeQuery();
			while (rs.next())
			{
				if(result == null)
					result = new HashMap<Integer, Integer>();
				result.put(Integer.valueOf(rs.getInt("doc_id")), rs.getInt("master_id"));
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
}
