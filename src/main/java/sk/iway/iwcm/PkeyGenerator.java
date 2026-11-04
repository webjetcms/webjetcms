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
    private long getMaxValue(String name, String table_name, String key_name)
	{
		if (Tools.isEmpty(table_name) || Tools.isEmpty(key_name))
		{
			return -1;
		}

		table_name = fixTableName(name, table_name);
		key_name = fixPkeyName(name, key_name);

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
			Logger.error(PkeyGenerator.class, "Error executing PkeyGenerator max value, name=" + name + ", sql=" +sql+" error="+e.getMessage());
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
		new SimpleQuery().execute("UPDATE pkey_generator SET value=? WHERE name=? AND value<?", value, p.getName(), value);
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
		try { allocate(p, false); }
		catch (SQLException ex) { throw new IllegalStateException("Cannot initialize pkey generator", ex); }
	}

	private void allocate(PkeyBean p, boolean strict) throws SQLException
	{
   		int pkeyGenIncrement = Constants.getInt("pkeyGenIncrement");

		//este nebol inicializovany, skus ziskat max hodnotu z DB a opravit tabulku
		if (p.getMaxValue()==0)
		{
			long maxv = strict ? getMaxValueStrict(p) : getMaxValue(p.getName(), p.getTableName(), p.getTablePkeyName());
			if (p.getValue() < maxv)
			{
				//uloz novu maximalnu hodnotu do databazy
				if (strict) setMaxValueStrict(p, maxv);
				else setMaxValue(p, maxv);
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
				if (ps.executeUpdate() != 1) throw new SQLException("Missing pkey generator row: " + name);
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

    /**
     * Allocates from the same cached ranges as the legacy API, propagating every
     * initialization/allocation error. A missing generator is an installation error.
     * The caller may lose a number when its later transaction fails.
     */
    public static synchronized long getNextValueAsLongStrict(String keyName) throws SQLException
    {
        // Validate even when a cached range remains: never hide a missing DB row.
        try (Connection connection = DBPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT table_name, table_pkey_name FROM pkey_generator WHERE name=?"))
        {
            statement.setString(1, keyName);
            try (ResultSet result = statement.executeQuery())
            {
                if (!result.next()) throw new SQLException("Missing pkey generator row: " + keyName);
                if (!keyName.equals(fixTableName(keyName, result.getString(1)))
                    || !fixPkeyName(keyName, keyName + "_id").equals(result.getString(2)))
                    throw new SQLException("Unexpected pkey generator mapping: " + keyName);
            }
        }
        catch (RuntimeException ex) { throw new SQLException("Cannot initialize generator: " + keyName, ex); }
        try
        {
            PkeyGenerator generator = getInstance();
            PkeyBean bean = generator.getPkeyBean(keyName);
            if (bean == null) throw new SQLException("Generator is absent from the shared cache: " + keyName);
            int increment = Constants.getInt("pkeyGenIncrement");
            int offset = Constants.getInt("pkeyGenOffset");
            if (increment <= 0 || Constants.getInt("pkeyGenBlockSize") <= 0 || offset < 0)
                throw new SQLException("Invalid pkey generator configuration");
            if (bean.getValue() + increment + offset > bean.getMaxValue()) generator.allocate(bean, true);
            long next = Math.addExact(bean.getValue(), increment);
            long id = Math.addExact(next, offset);
            if (id <= 0 || id > Integer.MAX_VALUE) throw new SQLException("Content ID range exhausted: " + keyName);
            bean.setValue(next);
            return id;
        }
        catch (RuntimeException ex) { throw new SQLException("Cannot reserve ID: " + keyName, ex); }
    }

    private long getMaxValueStrict(PkeyBean bean) throws SQLException
    {
        String table = fixTableName(bean.getName(), bean.getTableName());
        String key = fixPkeyName(bean.getName(), bean.getTablePkeyName());
        if (table == null || key == null || !table.matches("[a-zA-Z0-9_]+") || !key.matches("[a-zA-Z0-9_]+"))
            throw new SQLException("Invalid generator mapping");
        try (Connection connection = DBPool.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT MAX(" + key + ") FROM " + table);
             ResultSet result = statement.executeQuery())
        {
            if (!result.next()) throw new SQLException("Cannot read generator maximum");
            return result.getLong(1);
        }
    }

    private void setMaxValueStrict(PkeyBean bean, long minimum) throws SQLException
    {
        try (Connection connection = DBPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "UPDATE pkey_generator SET value=? WHERE name=? AND value<?"))
        {
            statement.setLong(1, minimum);
            statement.setString(2, bean.getName());
            statement.setLong(3, minimum);
            statement.executeUpdate();
        }
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
			ps.setString(i++, fixTableName(keyName, keyName));
			ps.setString(i++, fixPkeyName(keyName, keyName+"_id"));
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

	private static String fixTableName(String keyName, String defaultValue) {
		String tableName = defaultValue;
		if ("basket_browser_id".equals(keyName)) tableName = "basket_item";
		else if ("domain_limit_id".equals(keyName)) tableName = "domain_limits";
		else if ("file_archiv_global_id".equals(keyName)) tableName = "file_archiv";
		else if ("gallery_dimension".equals(keyName)) tableName = "gallery_dimension";
		else if ("MediaGroup".equals(keyName)) tableName = "media_groups";

		return tableName;
	}

	private static String fixPkeyName(String keyName, String defaultValue)
	{
		String pkeyName = defaultValue;
		if ("banner_banners".equals(keyName)) pkeyName = "banner_id";
		else if ("banner_doc".equals(keyName)) pkeyName = "id";
		else if ("banner_gr".equals(keyName)) pkeyName = "id";
		else if ("basket_browser_id".equals(keyName)) pkeyName = "browser_id";
		else if ("captcha_dictionary".equals(keyName)) pkeyName = "id";
		else if ("cookies".equals(keyName)) pkeyName = "cookie_id";
		else if ("documents".equals(keyName)) pkeyName = "doc_id";
		else if ("document_notes".equals(keyName)) pkeyName = "id";
		else if ("domain_limits".equals(keyName)) pkeyName = "domain_limit_id";
		else if ("domain_limit_id".equals(keyName)) pkeyName = "domain_limit_id";
		else if ("domain_redirects".equals(keyName)) pkeyName = "redirect_id";
		else if ("file_archiv_global_id".equals(keyName)) pkeyName = "global_id";
		else if ("gallery".equals(keyName)) pkeyName = "image_id";
		else if ("gallery_dimension".equals(keyName)) pkeyName = "dimension_id";
		else if ("media_groups".equals(keyName)) pkeyName = "media_group_id";
		else if ("rag_index_queue".equals(keyName)) pkeyName = "id";
		else if ("reservation_object_price".equals(keyName)) pkeyName = "object_price_id";
		else if ("reservation_object_times".equals(keyName)) pkeyName = "object_time_id";
		else if ("restaurant_menu".equals(keyName)) pkeyName = "menu_id";
		else if ("restaurant_menu_meals".equals(keyName)) pkeyName = "meals_id";
		else if ("user_perm_groups".equals(keyName)) pkeyName = "group_id";
		else if ("user_perm_groups_perms".equals(keyName)) pkeyName = "perm_id";
		else if ("MediaGroup".equals(keyName)) pkeyName = "media_group_id";

		return pkeyName;
	}
}
