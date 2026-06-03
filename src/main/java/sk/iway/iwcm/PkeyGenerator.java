package sk.iway.iwcm;

import java.security.MessageDigest;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Random;

import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.database.SimpleQuery;

/**
 *  PkeyGenerator.java
 *
 *@Title        webjet
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: jeeff $
 *@version      $Revision: 1.13 $
 *@created      Date: 28.9.2004 15:17:37
 *@modified     $Date: 2008/12/11 08:57:49 $
 */
public class PkeyGenerator
{
	private static final String CONTEXT_NAME = "sk.iway.iwcm.PkeyGenerator";
	private Map<String, PkeyBean> generators;
	private static Random random = new Random();

	/**
	 * Ziskanie instancie
	 * @return
	 */
	public static PkeyGenerator getInstance()
	{
		return(getInstance(false));
	}

	/**
	 * Ziskanie instancie
	 * @param forceRefresh - ak je true, nacitaju sa stavy z DB
	 * @return
	 */
	public static PkeyGenerator getInstance(boolean forceRefresh)
	{
		//		try to get it from server space
		if (forceRefresh == false)
		{
			if (Constants.getServletContext().getAttribute(CONTEXT_NAME) != null)
			{
				PkeyGenerator pkgen = (PkeyGenerator) Constants.getServletContext().getAttribute(CONTEXT_NAME);
				return (pkgen);
			}
		}
		return (new PkeyGenerator());
	}

	/**
	 * Konstruktor, nacita hodnoty z databazy
	 *
	 */
   private PkeyGenerator()
   {
   	generators = new Hashtable<>();
      pkeyReloadAll();
      Constants.getServletContext().setAttribute(CONTEXT_NAME, this);
   }

   /**
    * Ziska aktualnu maximalnu hodnotu v danej tabulke
    * @param table_name
    * @param key_name
    * @return
    */
   private long getMaxValue(String table_name, String key_name)
	{
		if (Tools.isEmpty(table_name) || Tools.isEmpty(key_name))
		{
			return -1;
		}

		String sql = "SELECT MAX(" + key_name + ") FROM " + table_name;
		try{
			return new ComplexQuery().setSql(sql).singleResult(new Mapper<Long>(){
				@Override
				public Long map(ResultSet rs) throws SQLException{
					return Long.valueOf(rs.getLong(1));
				}
			}).longValue();
		}
		catch (Exception e)
		{
			Logger.error(PkeyGenerator.class, "Error executing PkeyGenerator max value, sql=" +sql+" error="+e.getMessage());
			return -1;
		}
	}

   /**
    * Nastavi maximalnu hodnotu v tabulke pkey_generator, pouziva sa iba pri inicializacii
    * @param p
    * @param value
    * @return
    */
   private synchronized boolean setMaxValue(PkeyBean p, long value)
	{
   	try{
   		new SimpleQuery().execute("UPDATE pkey_generator SET value=? WHERE name=?", value, p.getName());
   		return true;
   	}catch (Exception e) {
   		sk.iway.iwcm.Logger.error(e);
   		return false;
		}
	}

   /**
    * Inicializacia klucov
    *
    */
   private void pkeyReloadAll()
	{
   		List<PkeyBean> pkeys = new ComplexQuery().setSql("SELECT * FROM pkey_generator").list(new Mapper<PkeyBean>()
		{
   			@Override
			public PkeyBean map(ResultSet rs) throws SQLException
			{
				PkeyBean p = new PkeyBean();
				p.setName(DB.getDbString(rs, "name"));
				p.setValue(rs.getLong("value"));
				p.setTableName(DB.getDbString(rs, "table_name"));
				p.setTablePkeyName(DB.getDbString(rs, "table_pkey_name"));
				//nastav maximalnu hodnotu na 0, aby sa to pri prvom pouziti fetchlo
				p.setMaxValue(0);
				return p;
			}
		});

   		for (PkeyBean p : pkeys)
		{
			//uloz ho
			generators.put(p.getName(), p);
			Logger.debug(this,"PkeyGenerator:" + p.toString());
		}
	}

   private PkeyBean getPkeyBean(String name)
   {
   	PkeyBean p = generators.get(name);
   	if (p == null)
   	{
   		Logger.error(this,"PkeyGenerator: is null - " + name);
   	}
   	return(p);
   }

   /**
    * Znova nacitanie kluca z DB (alokacia rozsahu)
    * @param keyName
    */
   	void allocate(PkeyBean p)
	{
   		int pkeyGenIncrement = Constants.getInt("pkeyGenIncrement");

		//este nebol inicializovany, skus ziskat max hodnotu z DB a opravit tabulku
		if (p.getMaxValue()==0)
		{
			long maxv = getMaxValue(p.getTableName(), p.getTablePkeyName());
			if (p.getValue() < maxv)
			{
				//uloz novu maximalnu hodnotu do databazy
				setMaxValue(p, maxv);
			}
		}

		int maxRetries = 5;
		long blockIncrement = (long)Constants.getInt("pkeyGenBlockSize") * pkeyGenIncrement;

		for (int attempt = 0; attempt < maxRetries; attempt++)
		{
			Connection db_conn = null;
			try
			{
				long value = -1;
				db_conn = DBPool.getConnection();
				//use atomic UPDATE+read to prevent Galera/multi-node race conditions
				db_conn.setAutoCommit(false);
				try
				{
					value = atomicIncrementAndGet(db_conn, p.getName(), blockIncrement);
					db_conn.commit();
				}
				catch (SQLException ex)
				{
					try { if (db_conn != null) db_conn.rollback(); } catch (Exception rollbackEx) { Logger.error(PkeyGenerator.class, "Rollback failed: " + rollbackEx.getMessage()); }
					throw ex;
				}
				finally
				{
					if (db_conn != null)
					{
						try { db_conn.setAutoCommit(true); } catch (Exception autocommitEx) { /* ignore */ }
						db_conn.close();
						db_conn = null;
					}
				}

				p.setMaxValue(value);
				p.setValue(value - blockIncrement);

				//zaokruhli hodnotu podla incrementu a offsetu
				//int newValue = (((keyName.getValue() / INCREMENT) + 1) * INCREMENT) + INCREMENT_OFFSET;
				long newValue = ((((p.getValue()-1) / pkeyGenIncrement) ) * pkeyGenIncrement) + 1; //NOSONAR
				Logger.debug(this,"ZAOKRUHLENE: " + p.getValue() + "->" + newValue);
				p.setValue(newValue);

				Logger.debug(this,"PkeyGenerator ALLOCATE:" + p.toString());
				return; //NOSONAR - this returns from for cycle
			}
			catch (SQLException ex)
			{
				//detect deadlock (Galera certification conflict): sqlState 40001 or errorCode 1213
				//GALERA: errorCode=1213 (ER_LOCK_DEADLOCK), sqlState=40001 (serialization failure), errorCode=1047 (ER_UNKNOWN_COM_ERROR)
				boolean isDeadlock = "40001".equals(ex.getSQLState()) || ex.getErrorCode() == 1213 || ex.getErrorCode() == 1047 || (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("deadlock"));
				if (isDeadlock && attempt < maxRetries - 1)
				{
					long sleepMs = (long)(50 * Math.pow(2, attempt)) + random.nextInt(50);
					Logger.info(PkeyGenerator.class, "PkeyGenerator deadlock on allocate for " + p.getName() + ", retry " + (attempt + 1) + "/" + maxRetries + ", sleeping " + sleepMs + "ms");
					try { Thread.sleep(sleepMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
				}
				else
				{
					sk.iway.iwcm.Logger.error(ex);
					throw new IllegalStateException("Failed to allocate pkey block for " + p.getName() + " after " + (attempt + 1) + " attempt(s)", ex);
				}
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
				throw new IllegalStateException("Unexpected error while allocating pkey block for " + p.getName(), ex);
			}
			finally
			{
				if (db_conn != null)
				{
					try { db_conn.close(); } catch (Exception closeEx) { /* ignore */ }
				}
			}
		}
	}

	/**
	 * Atomically increment pkey_generator value and return the new value.
	 * Uses database-specific SQL to avoid race conditions between UPDATE and SELECT
	 * (especially on Galera multi-master clusters where another node can modify the row between two statements).
	 *
	 * - MySQL/MariaDB: LAST_INSERT_ID(value+?) ensures connection-local read
	 * - PostgreSQL: UPDATE ... RETURNING value
	 * - MSSQL: UPDATE ... OUTPUT inserted.value
	 * - Oracle: RETURNING value INTO ? via CallableStatement
	 */
	private long atomicIncrementAndGet(Connection db_conn, String name, long blockIncrement) throws SQLException
	{
		long value = -1;

		if (Constants.DB_TYPE == Constants.DB_MSSQL)
		{
			//MSSQL: OUTPUT clause returns updated value atomically
			PreparedStatement ps = db_conn.prepareStatement("UPDATE pkey_generator SET value=value+? OUTPUT inserted.value WHERE name=?");
			try
			{
				ps.setLong(1, blockIncrement);
				ps.setString(2, name);
				ResultSet rs = ps.executeQuery();
				try
				{
					if (rs.next())
					{
						value = rs.getLong(1);
					}
				}
				finally { rs.close(); }
			}
			finally { ps.close(); }
		}
		else if (Constants.DB_TYPE == Constants.DB_PGSQL)
		{
			//PostgreSQL: RETURNING clause returns updated value atomically
			PreparedStatement ps = db_conn.prepareStatement("UPDATE pkey_generator SET value=value+? WHERE name=? RETURNING value");
			try
			{
				ps.setLong(1, blockIncrement);
				ps.setString(2, name);
				ResultSet rs = ps.executeQuery();
				try
				{
					if (rs.next())
					{
						value = rs.getLong(1);
					}
				}
				finally { rs.close(); }
			}
			finally { ps.close(); }
		}
		else if (Constants.DB_TYPE == Constants.DB_ORACLE)
		{
			//Oracle: use RETURNING INTO with CallableStatement
			CallableStatement cs = db_conn.prepareCall("BEGIN UPDATE pkey_generator SET value=value+? WHERE name=? RETURNING value INTO ?; END;");
			try
			{
				cs.setLong(1, blockIncrement);
				cs.setString(2, name);
				cs.registerOutParameter(3, java.sql.Types.BIGINT);
				cs.execute();
				value = cs.getLong(3);
			}
			finally { cs.close(); }
		}
		else
		{
			//MySQL/MariaDB: LAST_INSERT_ID(expr) stores the value per-connection,
			//so concurrent updates from other Galera nodes cannot affect the result
			PreparedStatement ps = db_conn.prepareStatement("UPDATE pkey_generator SET value=LAST_INSERT_ID(value+?) WHERE name=?");
			try
			{
				ps.setLong(1, blockIncrement);
				ps.setString(2, name);
				ps.execute();
			}
			finally { ps.close(); }

			ps = db_conn.prepareStatement("SELECT LAST_INSERT_ID()");
			try
			{
				ResultSet rs = ps.executeQuery();
				try
				{
					if (rs.next())
					{
						value = rs.getLong(1);
					}
				}
				finally { rs.close(); }
			}
			finally { ps.close(); }
		}

		if (value == -1)
		{
			throw new SQLException("Failed to read updated pkey value for: " + name);
		}

		return value;
	}

   /**
    * Ziskanie dalsej hodnoty kluca
    * @param keyName
    * @return
    */
	public static synchronized int getNextValue(String keyName)
	{
		return (int) getNextValueAsLong(keyName);
	}

	public static synchronized long getNextValueAsLong(String keyName)
	{
		long value = 1;
		PkeyGenerator pkGen = PkeyGenerator.getInstance();
		int INCREMENT_OFFSET = Constants.getInt("pkeyGenOffset");
		try
		{
			PkeyBean p = pkGen.getPkeyBean(keyName);
			if (p == null)
			{
				createNewRecord(keyName);
				PkeyGenerator pkg = PkeyGenerator.getInstance();
				pkg.pkeyReloadAll();
				return getNextValueAsLong(keyName);
			}
			int INCREMENT = Constants.getInt("pkeyGenIncrement");

			if ((p.getValue()+INCREMENT+INCREMENT_OFFSET) > p.getMaxValue())
			{
				//alokuj dalsi rozsah
				pkGen.allocate(p);
			}

			//ziskaj nasledujucu hodnotu - pri starte (inicializacii) sa zabezpeci
			//presne nastavenie podla INCREMENTU, takze tu staci zvysovat
			value = p.getValue();
			//jeeff: toto je zle, hodnotu mozeme navysovat len o INCREMENT a OFFSET pridavat len k navratovej hodnote!!
			//value += INCREMENT + INCREMENT_OFFSET;
			value += INCREMENT;
			p.setValue(value);

			//uloz to naspat do Hashtabulky (? je to treba ?)
			pkGen.generators.put(keyName, p);

			Logger.debug(PkeyGenerator.class,"PkeyGenerator NEXT:" + p.toString());
		}
		catch (Exception ex)
		{
			if (ex.getMessage() != null && ex.getMessage().contains("because \"db_conn\" is null"))
			{
				Logger.error(PkeyGenerator.class, "getNextValueAsLong: Database connection is NULL");
			} else {
				sk.iway.iwcm.Logger.error(ex);
			}
		}
		return(value + INCREMENT_OFFSET);
	}

	private static void createNewRecord(String keyName)
	{
		Logger.debug(PkeyGenerator.class,"PkeyGenerator NEXT is NULL");

		//Este neexistuje zaznam v tabulke, vytvor a nacitaj
		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			int i = 1;
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("INSERT INTO pkey_generator (name, value, table_name, table_pkey_name) VALUES (?, 1, ?, ?)");
			ps.setString(i++, keyName);
			ps.setString(i++, keyName);
			ps.setString(i++, keyName+"_id");
			ps.execute();
			ps.close();
			ps = null;
		}
		catch (Exception ex)
		{
			String message = ex.getMessage();
			if (message.contains("doesn't exist")) throw new RuntimeException("Table pkey_generator doesn't exist, please create it.");
			if (ex.getMessage() != null && ex.getMessage().contains("because \"db_conn\" is null"))
			{
				Logger.error(PkeyGenerator.class, "getNextValueAsLong: Database connection is NULL");
			} else {
				sk.iway.iwcm.Logger.error(ex);
			}
		}
		finally
		{
			try
			{
				if (db_conn != null)
					db_conn.close();
				if (ps != null)
					ps.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
	}

	public static String getUniqueKey()
	{
		Object o = new Object();
		String key = Tools.getNow() + "1552" + o.hashCode() + (random.nextInt(Integer.MAX_VALUE));

		byte[] defaultBytes = key.getBytes();
		try
		{
			MessageDigest algorithm = MessageDigest.getInstance("MD5");
			algorithm.reset();
			algorithm.update(defaultBytes);
			byte[] messageDigest = algorithm.digest();

			StringBuilder hexString = new StringBuilder();
			for (int i=0;i<messageDigest.length;i++)
			{
				hexString.append(Integer.toHexString(0xFF & messageDigest[i])); //NOSONAR
			}

			Logger.debug(PkeyGenerator.class, "getUniqueKey: key="+key+" hex="+hexString.toString());

			key = hexString.toString();
		}
		catch(Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		return key;
	}
}
