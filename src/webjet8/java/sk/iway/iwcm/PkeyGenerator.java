package sk.iway.iwcm;

import java.security.MessageDigest;
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
   		Logger.println(this,"PkeyGenerator:" + p.toString());
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
   private void allocate(PkeyBean p)
	{
   	int INCREMENT = Constants.getInt("pkeyGenIncrement");

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

		try
		{
			long value = -1;
			Connection db_conn = DBPool.getConnection();
			try
			{
				PreparedStatement ps = db_conn.prepareStatement("UPDATE pkey_generator SET value=value+"+(Constants.getInt("pkeyGenBlockSize") * INCREMENT)+" WHERE name=?");
				try
				{
					ps.setString(1, p.getName());
					ps.execute();
					ps.close();

					ps = db_conn.prepareStatement("SELECT max(value) AS value FROM pkey_generator WHERE name=?");
					ps.setString(1, p.getName());
					ResultSet rs = ps.executeQuery();
					try
					{
						while (rs.next())
						{
							value = rs.getLong("value");
						}
					}
					finally { rs.close(); }
				}
				finally { ps.close(); }
			}
			finally { db_conn.close(); }

			p.setMaxValue(value);
			p.setValue(value - (Constants.getInt("pkeyGenBlockSize") * INCREMENT));

			//zaokruhli hodnotu podla incrementu a offsetu
			//int newValue = (((keyName.getValue() / INCREMENT) + 1) * INCREMENT) + INCREMENT_OFFSET;
			long newValue = ((((p.getValue()-1) / INCREMENT) ) * INCREMENT) + 1; //NOSONAR
			Logger.debug(this,"ZAOKRUHLENE: " + p.getValue() + "->" + newValue);
			p.setValue(newValue);

			Logger.debug(this,"PkeyGenerator ALLOCATE:" + p.toString());
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
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
			sk.iway.iwcm.Logger.error(ex);
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
			sk.iway.iwcm.Logger.error(ex);
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
