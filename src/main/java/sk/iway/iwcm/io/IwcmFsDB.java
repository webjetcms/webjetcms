package sk.iway.iwcm.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PkeyGenerator;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.AdminTools;
import sk.iway.iwcm.common.FilePathTools;
import sk.iway.iwcm.sync.FileBean;
import sk.iway.iwcm.system.cluster.ClusterDB;

/**
 * IwcmFsDB.java
 *
 * @Title webjet4
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2008
 * @author $Author: jeeff $
 * @version $Revision: 1.17 $
 * @created Date: 24.6.2008 12:47:26
 * @modified $Date: 2009/11/16 08:54:08 $
 */
public class IwcmFsDB
{
	/**
	 * SQL pre zmenu deleted priznaku <code>UPDATE_FILE_FAT_SET_IS_DELETED_WHERE_FILE_FAT_ID</code>
	 */
	private static final String UPDATE_DELETED_FLAG_SQL = "update file_fat set is_deleted = ? where file_fat_id = ?  ";
	/**
	 * Nazov stplca s ID vo fat tabulke <code>FILE_FAT_ID</code>
	 */
	private static final String FAT_ID_COLUMN = "file_fat_id";
	private static Map<String, Integer> virtualPathToFatId ;
	private static Map<String, Long> virtualPathToLastModified;
	private static int cacheTimeInMinutes;
	private static long nextRefreshTime;
	private static boolean useDBStorage;
	private static String dirsInDB;
	//private static final boolean useVersioning;
	private static String tempDir;
	private static int blockSize;

	static
	{
		init();
	}

	public static void init() {
		cacheTimeInMinutes = Tools.getIntValue(Constants.getString("iwfs_timeInCache"), 15);
		useDBStorage=Constants.getBoolean("iwfs_useDB");
		dirsInDB=Constants.getString("iwfs_dirsInDB");
		//useVersioning=Constants.getBoolean("iwfs_useVersioning");
		tempDir=Constants.getString("iwfs_tempDir");
		blockSize=Tools.getIntValue(Constants.getString("iwfs_blockSize") , 1024*1024);
	}

	private  IwcmFsDB()
	{
	}

	/**
	 * ulozi hash tabuliek a do cache, fat_id a last modified pre kazdy subor v
	 * db
	 */
	protected static synchronized void loadHashTables(boolean refresh)
	{
		if (virtualPathToLastModified == null || virtualPathToFatId == null || refresh || nextRefreshTime < System.currentTimeMillis())
		{
			Logger.debug(IwcmFsDB.class, "loading hashtables");

			Map<String, Integer> newVirtualPathToFatId = new Hashtable<>();
			Map<String, Long> newVirtualPathToLastModified = new Hashtable<>();
			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try
			{
				db_conn = DBPool.getConnection();
				ps = db_conn.prepareStatement("SELECT virtual_path, last_modified, file_fat_id FROM file_fat WHERE is_deleted = ? ORDER BY file_fat_id ASC");
				ps.setBoolean(1, false);
				rs = ps.executeQuery();
				while (rs.next())
				{
					newVirtualPathToFatId.put(rs.getString("virtual_path"), rs.getInt(FAT_ID_COLUMN));
					newVirtualPathToLastModified.put(rs.getString("virtual_path"), rs.getLong("last_modified"));
				}
				rs.close();
				ps.close();
				db_conn.close();
				rs = null;
				ps = null;
				db_conn = null;

				nextRefreshTime = System.currentTimeMillis() + (1000L * 60 * cacheTimeInMinutes);

				virtualPathToFatId = newVirtualPathToFatId;
				virtualPathToLastModified = newVirtualPathToLastModified;
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
		}
	}

	/**
	 * vrati tabulku s tabulku s casmi poslednej modifkacie vsetkych suborov,
	 * kluc je virtualna cesta k suboru
	 *
	 * @return
	 */
	public static Map<String, Long> getModifiedTable()
	{
		if (virtualPathToLastModified == null || nextRefreshTime < System.currentTimeMillis())
		{
			loadHashTables(false);
			return virtualPathToLastModified;
		}
		else
		{
			return virtualPathToLastModified;
		}
	}

	/**
	 * vrati tabulku s tabulku s fat_id vsetkych suborov , kluc je virtualna
	 * cesta k suboru
	 *
	 * @return
	 */
	public static Map<String, Integer> getFatIdTable()
	{
		if (virtualPathToFatId == null || nextRefreshTime < System.currentTimeMillis())
		{
			loadHashTables(false);
			return virtualPathToFatId;
		}
		else
		{
			return virtualPathToFatId;
		}
	}

	/**
	 * zapise subor z disku do Db storage
	 *
	 * @param file
	 */
	public static void writeFileToDB(File file)
	{
		writeFileToDB(file, file);
	}

	/**
	 * zapise subor z databazy do OutputStreamu
	 *
	 * @param src
	 * @param out
	 * @throws FileNotFoundException
	 */
	public static byte[] writeFileToOutputStream(File src, OutputStream out) throws FileNotFoundException
	{
		return writeFileToOutputStreamFromHistory(src, out, -1);
	}

	/**
	 * Zapise subor z databazy so zadanym fatIdHistory (ak je -1 zapise aktualnu verziu suboru)
	 * @param src
	 * @param out
	 * @param fatIdHistory
	 * @return
	 * @throws FileNotFoundException
	 */
	public static byte[] writeFileToOutputStreamFromHistory(File src, OutputStream out, int fatIdHistory) throws FileNotFoundException
	{
		byte[] result=new byte[0];

		IwcmInputStream in = null;
		String virtualPath = getVirtualPath(src.getAbsolutePath());
		if (fatIdHistory != -1 && getFatIdTable().get(virtualPath) == null)
			throw new FileNotFoundException(virtualPath);
		try
		{
			long length = -1;

			if (FileCache.useFileCache())
			{
				length=new IwcmFile(src).length();
				if (length<=FileCache.getMaxFileSize())
				{
					result = new byte[(int)length];
				}
			}
			in = new IwcmInputStream(src, fatIdHistory);

			byte[] buffer = new byte[64000];
			int bytesRead = 0;
			int bytesCopied = 0;
			while ((bytesRead = in.read(buffer, 0, 64000)) != -1)
			{
				//Logger.debug(IwcmFsDB.class, "writing "+bytesRead);
				out.write(buffer, 0, bytesRead);
				if (FileCache.useFileCache() && length<=FileCache.getMaxFileSize())
				{
					System.arraycopy(buffer, 0, result, bytesCopied, bytesRead);
					bytesCopied+=bytesRead;
				}

			}
			out.flush();
			out.close();
			out = null;
			in.close();
			in = null;
		}
		catch (SocketException e)
		{
			Logger.error(IwcmFsDB.class, src.getAbsolutePath() + ": " + e.getMessage());
		}
		catch (Exception e)
		{
			Logger.error(IwcmFsDB.class, src.getAbsolutePath());
			sk.iway.iwcm.Logger.error(e);
		}
		finally
		{
			try
			{
				if (out != null)
					out.close();
				if (in != null)
					in.close();
			}
			catch (Exception ex2)
			{
			}
		}
		return result;
	}

	/**
	 * Vytvori prazdny adresar v databaze podla nastavenej virtualPath
	 *
	 * @param virtualPath
	 */
	public static void createDirectory(String virtualPath)
	{
		if (getFatIdTable().get(virtualPath) != null)
		{
			return;
		}
		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn
						.prepareStatement("insert into file_fat (file_fat_id,fsize,virtual_path,is_directory,depth,last_modified) values (?,?,?,?,?,?) ");
			int fatId = PkeyGenerator.getNextValue(FAT_ID_COLUMN);
			ps.setInt(1, fatId);
			ps.setInt(2, 0);
			ps.setString(3, virtualPath);
			ps.setBoolean(4, true);
			ps.setInt(5, IwcmFsDB.getDepth(virtualPath));
			ps.setLong(6, 0);
			ps.execute();
			getFatIdTable().put(virtualPath, fatId);
			getModifiedTable().put(virtualPath, (long) 0);
			ClusterDB.addRefresh(IwcmFsDB.class);
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
	 * vytvori prazdny adresar v db podla adresa na disku
	 *
	 * @param dir
	 */
	public static void writeDirectoryToDB(File dir)
	{
		createDirectory(getVirtualPath(dir.getAbsolutePath()));
	}

	/**
	 * Zapise adresar s disku spolu zo subormi, ak je recursive true, zapise aj
	 * podadresare so subormy.
	 *
	 * @param dir
	 * @param recursive
	 */
	public static void writeDirectoryToDB(File dir, boolean recursive)
	{
		Logger.debug(IwcmFsDB.class, "writing directory: " + dir.getAbsolutePath());
		if (dir.isDirectory())
		{
			writeDirectoryToDB(dir);
			for (File f : dir.listFiles())
			{
				if (recursive && f.isDirectory())
				{
					writeDirectoryToDB(f, recursive);
				}
				else if (f.isDirectory())
					continue;
				if (f.isFile())
				{
					writeFileToDB(f);
				}
			}
		}
		else
		{
			throw new RuntimeException("Not a directory");
		}
	}

	/**
	 * Zapise subor z databazy na disk
	 *
	 * @param f
	 */
	public static void writeFileToDisk(File f)
	{
		writeFileToDisk(f, f);
	}

	/**
	 * Vrati virtualnu cestu
	 *
	 * @param realPath
	 * @return
	 */
	public static String getVirtualPath(String realPath)
	{
		if (InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir"))
		{
			String domainVirtualPath = FilePathTools.getVirtualPathHellper(realPath);
			if (domainVirtualPath != null) return domainVirtualPath;
		}

		if (realPath == null || realPath.length() < Tools.getRealPath("/").length())
		{
			return "/";
		}
		String path = "/" + realPath.substring(Tools.getRealPath("/").length()).replace('\\', '/'); //NOSONAR
		if (path.endsWith("/") && !"/".equals(path))
		{
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}

	/**
	 * vrati hlbku v strome virtualnych adresarov
	 *
	 * @param virtualPath
	 * @return
	 */
	public static int getDepth(String virtualPath)
	{
		int count = 0;
		for (char c : virtualPath.toCharArray())
		{
			if (c == '/')
			{
				count++;
			}
		}
		return count;
	}

	/**
	 * podla cesty zisti ci sa ma pouzit storage alebo nie.
	 *
	 * @param virtualPath
	 *           virtualna cesta!
	 * @return
	 */
	public static boolean useDBStorage(String virtualPath) //NOSONAR
	{
		if (useDBStorage==false) return false;

		if (virtualPath.startsWith("/admin") || virtualPath.endsWith(".jsp")) return false;

		// ak ano, tak este skontrolujeme ci aj dany adresar/subor
		String dirs = dirsInDB;
		for (String d : dirs.split(";"))
		{
			if (d.endsWith("/"))
			{
				if (virtualPath.startsWith(d.substring(0, d.length() - 1)))
				{
					return true;
				}
			}
			else
			{
				if (virtualPath.startsWith(d))
				{
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * zisti ci sa ma pouzit storage
	 *
	 * @return
	 */
	public static boolean useDBStorage()
	{
		return useDBStorage;
	}

	/**
	 * Zapise obsah InputStreamu do file
	 *
	 * @param in
	 *           vstupne data
	 * @param file
	 *           vystupny subor
	 * @param size
	 *           velkost vystupneho suboru
	 */
	public static void writeFiletoDest(InputStream in, File file, int size,boolean closeInStream)
	{
		if (useDBStorage(getVirtualPath(file.getPath())))
		{
			Logger.error(IwcmFsDB.class, "writeFileToDest:" + file.getPath());
			IwcmFile iwFile=new IwcmFile(file);
			iwFile.delete();
			if (!iwFile.getParentFile().exists())
			{
				iwFile.mkdirs();
			}
			Connection db_conn = null;
			PreparedStatement ps = null;

			try
			{
				db_conn = DBPool.getConnection();
				ps = db_conn
							.prepareStatement("insert into file_fat (file_fat_id,fsize,virtual_path,depth,last_modified) values (?,?,?,?,?)");
				int fatId = PkeyGenerator.getNextValue(FAT_ID_COLUMN);
				ps.setInt(1, fatId);
				ps.setLong(2, size);
				ps.setString(3, getVirtualPath(file.getPath()));
				ps.setInt(4, IwcmFsDB.getDepth(getVirtualPath(file.getPath())));
				Date dt=new Date();
				ps.setLong(5,dt.getTime());
				ps.execute();
				ps.close();
				byte[] data = null;
				ps = db_conn.prepareStatement("insert into file_data (file_fat_id,data) values (?,?)");
				long numberOfBlocks = (long) Math.ceil((double) size / (double) getBlockSize());
				for (int i = 0; i < numberOfBlocks; i++)
				{
					data = new byte[getBlockSize()];
					int bytesRead = 0;
					int bytesReadSum=0;
					while (bytesRead != -1 && bytesReadSum < getBlockSize())
					{
						bytesRead=in.read(data, bytesReadSum, getBlockSize()-bytesReadSum);
						bytesReadSum+=bytesRead;
					}
					ps.setInt(1, fatId);
					if (i == numberOfBlocks - 1)
					{
						byte[] tmp = new byte[(int) (getBlockSize() - (numberOfBlocks * getBlockSize() - size))];
						System.arraycopy(data,0, tmp, 0, tmp.length);

						ps.setBytes(2, tmp);
					}
					else
						ps.setBytes(2, data);
					ps.execute();
					data = null;
				}
				getFatIdTable().put(getVirtualPath(file.getPath()), fatId);
				getModifiedTable().put(getVirtualPath(file.getPath()), dt.getTime());
				ClusterDB.addRefresh(IwcmFsDB.class);
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
					sk.iway.iwcm.Logger.error(ex2);
				}
			}
		}
		else
		{
			try
			{
				byte[] buffer = new byte[64000];
				int bytesRead = 0;
				//fos by malo subor vytvorit, mal som ale hlasene, ze to u klienta hadzalo FileNot FoundException preto som pridal implicitne vytvorenie suboru a adresara
				if (file.getParentFile().exists()==false)
				{
					boolean created = file.getParentFile().mkdirs();
					Logger.debug(IwcmFsDB.class, "Parent file doesn't exists, path="+file.getParentFile().getAbsolutePath()+", created="+created);
				}
				if (file.exists()==false)
				{
					boolean created = file.createNewFile();
					Logger.debug(IwcmFsDB.class, "File doesn't exists, path="+file.getAbsolutePath()+", creating="+created);
				}
				FileOutputStream fos = new FileOutputStream(file);
				while ((bytesRead = in.read(buffer, 0, buffer.length)) != -1)
				{
					fos.write(buffer, 0, bytesRead);
				}
				fos.close();

				//ulozim subor aj do historie
				IwcmFsDB.saveFileHistory(new IwcmFile(file), false);
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}
		try
		{
			if (closeInStream) //pouziva sa napr. pri rozbalovani zipu ked sa zapisuju na disk subory, a input stream musi zostat otvoreny
			{
			in.close();
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

	}

	public static void writeFiletoDest(InputStream in, File file, int size)
	{
		writeFiletoDest(in, file, size,true);
	}

	public static void writeFileToDisk(InputStream in, File file, boolean closeInStream)
	{
		try
		{
			byte[] buffer = new byte[64000];
			int bytesRead = 0;
			FileOutputStream fos = new FileOutputStream(file);
			while ((bytesRead = in.read(buffer, 0, buffer.length)) != -1)
			{
				fos.write(buffer, 0, bytesRead);
			}
			fos.close();

			//ulozim subor aj do historie
			IwcmFsDB.saveFileHistory(new IwcmFile(file), false);
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		try
		{
			if (closeInStream) //pouziva sa napr. pri rozbalovani zipu ked sa zapisuju na disk subory, a input stream musi zostat otvoreny
			{
				in.close();
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
	}
	/**
	 * skopiruje subory iba v ramci databazy
	 *
	 * @param src
	 * @param dest
	 */
	public static void copyTo(IwcmFile src, IwcmFile dest)
	{
		if (useDBStorage(src.getVirtualPath()))
		{
			try
			{
				InputStream in = new IwcmInputStream(src);
				String virtualPath = src.getVirtualPath();
				if (getFatIdTable().get(virtualPath) == null) {
					in.close();
					throw new FileNotFoundException(virtualPath);
				}
				writeFiletoDest(in, new File(dest.getPath()), (int) src.length());
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}
	}

	/**
	 * zmeni zadanu cestu na cestu v temp adresari
	 *
	 * @return
	 * @throws IOException
	 */
	public static String getTempFilePath(String path) throws IOException
	{
		String tempDir = getTempDir();
		if (!tempDir.endsWith(File.separator))
		{
			tempDir += File.separator;
		}
		if (!new File(tempDir).exists())
		{
			if(!(new File(tempDir).mkdirs()))
				throw new IOException("Unable to create dir: "+tempDir);
		}
		return tempDir + path.substring(path.lastIndexOf(File.separator) + 1, path.length());
	}

	/**
	 * Vrati temp adresar
	 * @throws IOException
	 */
	public static String getTempDir() throws IOException
	{
		String result = Tools.getRealPath("/WEB-INF/tmp/");
		if (Tools.isNotEmpty(tempDir))
		{
			if (tempDir.startsWith("/"))
			{
				result = Tools.getRealPath(tempDir);
			}
			else
			{
				result = tempDir;
			}
		}

		if (!new File(result).exists())
		{
			if(!(new File(result).mkdirs()))
				throw new IOException("Unable to create dir: "+result);
		}
		return result;
	}

	/**
	 * Zapise subor src z databazy na disk do outFile
	 *
	 * @param src
	 * @param outFile
	 */
	public static void writeFileToDisk(File src, File outFile) {
		writeFileToDisk(src, outFile, false);
	}

	/**
	 * Zapise subor src z databazy na disk do outFile, ak je nastavene forceOwerwrite tak subor prepise bez ohladu na to, ci je novsi
	 * @param src
	 * @param outFile
	 * @param forceOwerwrite
	 */
	public static void writeFileToDisk(File src, File outFile, boolean forceOwerwrite)
	{
		if (useDBStorage(getVirtualPath(src.getPath())))
		{
			try
			{
				byte[] buffer = new byte[getBlockSize()];
				if(!outFile.getParentFile().exists())
					if(!outFile.getParentFile().mkdir())
						throw new IOException("Unable to create dir: "+outFile.getParentFile().getAbsolutePath());
				if(outFile.exists() && (outFile.lastModified() == src.lastModified()) && forceOwerwrite == false)
				{
					//do nothing
				}
				else
				{
					if(!outFile.exists())
						if(!outFile.createNewFile())
							throw new IOException("Unable to create file: "+outFile.getAbsolutePath());
					InputStream in = new IwcmInputStream(src);
					int bytesRead = 0;
					FileOutputStream fos = new FileOutputStream(outFile);
					while ((bytesRead = in.read(buffer, 0, getBlockSize())) != -1)
					{
						fos.write(buffer, 0, bytesRead);
					}
					in.close();
					fos.close();

					//jeeff: pretoze ak to tu nebolo, zapisalo, ale po restarte chcelo ulozit do DB!!!
					outFile.setLastModified(src.lastModified()); //NOSONAR
				}
				//getModifiedTable().put(getVirtualPath(src.getPath()), outFile.lastModified());
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}
	}

	/**
	 * zapise subor z disku (src) do Db storage s virtualnou cestou podla outFile
	 *
	 * @param src
	 * @param outFile
	 */
	public static void writeFileToDB(File src, File outFile)
	{
			Logger.debug(IwcmFsDB.class, "writeFileToDB2:" + outFile.getPath());
			Connection db_conn = null;
			PreparedStatement ps = null;

			int fatId = -1;
			try
			{
				db_conn = DBPool.getConnection();
				if (getFatIdTable().get(getVirtualPath(outFile.getPath())) != null)
				{
					fatId = getFatIdTable().get(getVirtualPath(outFile.getPath()));
				}
				if (fatId > 0)
				{
					if (IwcmFsDB.useVersioning() && IwcmFsDB.useDBStorage())
					{
						ps = db_conn.prepareStatement("update file_fat set is_deleted = ? where file_fat_id = ? "); // nastavi sa priznak zmazaneho suboru
						ps.setBoolean(1, true);
						ps.setInt(2, IwcmFsDB.getFatIdTable().get(getVirtualPath(outFile.getPath())));
						ps.execute();
						ps.close();
					}
					else
					{
						ps = db_conn.prepareStatement("delete from file_fat where file_fat_id = ? "); //zmaze sa z tabuliek
						ps.setInt(1, IwcmFsDB.getFatIdTable().get(getVirtualPath(outFile.getPath())));
						ps.execute();
						ps.close();
						ps = db_conn.prepareStatement("delete from file_data where file_fat_id = ? ");
						ps.setInt(1, IwcmFsDB.getFatIdTable().get(getVirtualPath(outFile.getPath())));
						ps.execute();
						ps.close();
					}
				}
				ps = db_conn
							.prepareStatement("insert into file_fat (file_fat_id,fsize,virtual_path,depth,last_modified) values (?,?,?,?,?)");
				fatId = PkeyGenerator.getNextValue(FAT_ID_COLUMN);
				ps.setInt(1, fatId);
				ps.setLong(2, src.length());
				ps.setString(3, getVirtualPath(outFile.getPath()));
				ps.setInt(4, IwcmFsDB.getDepth(getVirtualPath(outFile.getPath())));
				Date dt=new Date();
				ps.setLong(5,dt.getTime());
				ps.execute();
				ps.close();
				byte[] data = null;
				FileInputStream in = new FileInputStream(src);
				ps = db_conn.prepareStatement("insert into file_data (file_fat_id,data) values (?,?)");
				long numberOfBlocks = (long) Math.ceil((double) src.length() / (double) getBlockSize());
				for (int i = 0; i < numberOfBlocks; i++)
				{
					data = new byte[getBlockSize()];
					in.read(data);
					ps.setInt(1, fatId);
					if (i == numberOfBlocks - 1)
					{
						byte[] tmp = new byte[(int) (getBlockSize() - (numberOfBlocks * getBlockSize() - src.length()))];
						System.arraycopy(data, 0, tmp, 0, tmp.length);

						ps.setBytes(2, tmp);
					}
					else
						ps.setBytes(2, data);
					ps.execute();
					data = null;
				}
				getFatIdTable().put(getVirtualPath(outFile.getPath()), fatId);
				getModifiedTable().put(getVirtualPath(outFile.getPath()), dt.getTime());
				ClusterDB.addRefresh(IwcmFsDB.class);
				ps.close();
				in.close();
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
					sk.iway.iwcm.Logger.error(ex2);
				}
			}
	}

	/**
	 * vrati velkost blokov do ktorych sa ukladaju subory,ak bol subor ulozeny s
	 * inou velkostou ako je aktulne nastavena tak sa neprecita.
	 *
	 * @return
	 */
	public static int getBlockSize()
	{
		return blockSize;
	}

	public static boolean manageVersions(String virtualPath, boolean rollback, boolean update)
	{
		if (getFatIdTable().get(virtualPath) != null)
		{
			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try
			{
				TreeMap<Integer, Boolean> idDeletedTable = new TreeMap<>();
				db_conn = DBPool.getConnection();
				String sql = "select file_fat_id,is_deleted from file_fat where virtual_path = ? order by file_fat_id  desc";
				ps = db_conn.prepareStatement(sql);
				ps.setString(1, virtualPath);
				rs = ps.executeQuery();
				while (rs.next())
				{
					idDeletedTable.put(rs.getInt(FAT_ID_COLUMN), rs.getBoolean("is_deleted"));
				}
				rs.close();
				ps.close();
				Iterator<Integer> it = null;
				if (rollback)
				{
					it = idDeletedTable.descendingKeySet().iterator();
				}
				else
				{

						it = idDeletedTable.keySet().iterator();

				}
				int newFatId = -1;
				int oldFatId = -1;
				while (it.hasNext())
				{
					int fatId = it.next();
					if (!idDeletedTable.get(fatId).booleanValue() && it.hasNext())
					{
						oldFatId = fatId;
						newFatId = it.next();
					}
				}
				if (newFatId < 0 || oldFatId < 0)
				{
					return false;
				}

				ps = db_conn.prepareStatement(UPDATE_DELETED_FLAG_SQL);
				ps.setBoolean(1, true);
				ps.setInt(2, oldFatId);
				ps.execute();
				ps.close();
				ps = db_conn.prepareStatement(UPDATE_DELETED_FLAG_SQL);
				ps.setBoolean(1, false);
				ps.setInt(2, newFatId);
				ps.execute();
				getFatIdTable().put(virtualPath, newFatId);
				getModifiedTable().put(virtualPath, new Date().getTime());
				ClusterDB.addRefresh(IwcmFsDB.class);
				ps.close();
				db_conn.close();
				rs = null;
				ps = null;
				db_conn = null;
				return true;
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
		}
		return false;
	}

	/**
	 * nahradi terajsiu verziu novsou verziou suboru ak existuje
	 *
	 * @param virtualPath
	 * @return
	 */
	public static boolean update(String virtualPath)
	{
		return manageVersions(virtualPath, false, true);
	}

	/**
	 * nahradi terajsiu verziu starsou verziou.
	 *
	 * @param virtualPath
	 * @return
	 */
	public static boolean rollback(String virtualPath)
	{
		return manageVersions(virtualPath, true, false);
	}

	/**
	 * vrati ci sa ma pouzivat verzovanie suborov
	 *
	 * @return
	 */
	public static boolean useVersioning()
	{
		return Constants.getBoolean("iwfs_useVersioning");
	}

	/**
	 * @deprecated - use AdminTools.getVersionList
	 */
	@Deprecated
	public static List<FileBean>getVersionList(String virtualPath)
	{
		return AdminTools.getVersionList(virtualPath);
	}

	public static void replaceActualVersionWithHistory(String virtualPath,int historyFatId)
	{
		int fatId=getFatIdTable().get(virtualPath);

		if (fatId==historyFatId) return;

		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = DBPool.getConnection();

			ps = db_conn.prepareStatement(UPDATE_DELETED_FLAG_SQL);
			ps.setBoolean(1, true);
			ps.setInt(2, fatId);
			ps.execute();
			ps.close();
			ps = db_conn.prepareStatement(UPDATE_DELETED_FLAG_SQL);
			ps.setBoolean(1, false);
			ps.setInt(2, historyFatId);

			ps.execute();

			getFatIdTable().put(virtualPath, historyFatId);
			getModifiedTable().put(virtualPath, new Date().getTime());
			ClusterDB.addRefresh(IwcmFsDB.class);

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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}
	}

	public static void getInstance(boolean refresh)
	{
		Logger.println(IwcmFsDB.class, "getInstance("+refresh+")");
		loadHashTables(true);
	}

	/**
	 * Aktualizacia hodnoty last modified
	 * @param virtualPath
	 * @param time
	 * @return
	 */
	public static boolean updateLastModified(String virtualPath, long time)
	{
		Long vpid = IwcmFsDB.getModifiedTable().get(virtualPath);
		if (vpid == null) return false;

		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("UPDATE file_fat SET last_modified=? WHERE file_fat_id=?");
			ps.setLong(1, time);
			ps.setLong(2, vpid.longValue());
			int updated = ps.executeUpdate();

			ps.close();
			db_conn.close();
			ps = null;
			db_conn = null;

			if (updated > 0) return true;
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
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		return false;
	}

	public static List<IwcmFile> listRecursively(IwcmFile dir)
	{
		return listRecursively(dir, new IwcmFileFilter(){
			@Override
			public boolean accept(IwcmFile pathname){return true;}
		});
	}

	public static List<IwcmFile> listRecursively(IwcmFile dir, IwcmFileFilter filter)
	{
		List<IwcmFile> resultList = new ArrayList<>();
		if (!useDBStorage(dir.getVirtualPath()))
		{
			listRecursivelyInNormalFile(dir, resultList, filter);
		}
		else
		{
			listRecursivelyInDbStorage(dir, resultList, filter);
		}
		return resultList;
	}

	private static void listRecursivelyInDbStorage(IwcmFile dir, List<IwcmFile> resultList, IwcmFileFilter filter)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		int count = IwcmFsDB.getDepth(dir.getVirtualPath());
		try
		{
			db_conn = DBPool.getConnection();
			String sql = "select distinct virtual_path from file_fat where virtual_path like ? and depth >= ? and is_deleted = ? order by virtual_path ";
			ps = db_conn.prepareStatement(sql);
			ps.setString(1, dir + "%");
			ps.setInt(2, count);
			ps.setBoolean(3, false);
			rs = ps.executeQuery();
			while (rs.next())
			{
					IwcmFile file = new IwcmFile(Tools.getRealPath(rs.getString("virtual_path")));
					if (filter.accept(file))
						resultList.add(file);
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
	}

	private static void listRecursivelyInNormalFile(IwcmFile dir, List<IwcmFile> resultList, IwcmFileFilter filter)
	{
		for (IwcmFile file : dir.listFiles())
		{
			if (!filter.accept(file))
				continue;
			if (file.isDirectory())
				listRecursivelyInNormalFile(file, resultList, filter);
			resultList.add(file);
		}
	}

	/**
	 * Ulozi historiu suboru na file systeme, v pripade, ze nie je zapnute DBStorage
	 * @param file
	 * @return true if ok, else false
	 */
	public static boolean saveFileHistory(IwcmFile file, boolean deleted)
	{
		if (file == null || file.getVirtualPath()==null) return false;

		if(useVersioning() && !useDBStorage(file.getVirtualPath()))	//ak je zapnute versiovanie a ak sa subor neuklada do db storage
		{
			String fileHistory = Constants.getString("fileHistoryPath");
			if(fileHistory.charAt(fileHistory.length()-1) == '/')
			{
				fileHistory = fileHistory.substring(0, fileHistory.length()-1);	//odstranim /
			}
			String virtualPath = file.getVirtualPath();

			//imgcache nebudeme ukladat, nema to zmysel
			if (virtualPath.contains("/WEB-INF/imgcache")) return false;

			if (virtualPath.indexOf(fileHistory) == -1)
			{ 	// kvoli zacykleniu - do historie neukladam subory zadresara historie
				FileHistoryBean fhb = new FileHistoryBean();
				fhb.setChangeDate(new Date());
				fhb.setFileUrl(virtualPath);
				RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
				if (rb == null) {
					rb = new RequestBean();
					rb.setUserId(-1);
				}
				fhb.setUserId(rb.getUserId());
				fhb.setDeleted(deleted);
				fhb.setIpAddress(rb.getRemoteIP());
				if(!deleted)
				{
					fhb.setHistoryPath(fileHistory + getVirtualPath(file.getParent()) + "/");
				}
				// zapisanie zaznamu do historie - tabulka file_history
				boolean saveOk = new FileHistoryDB().save(fhb);
				RequestBean.addAuditValue("fileHistoryId", String.valueOf(fhb.getId()));
				RequestBean.addAuditValue("path", fhb.getFileUrl()+"?fHistoryId="+fhb.getId());

				if (saveOk)
				{
					if(!deleted)	//subor kopirujem iba v pripade, ze ide o jeho vytvorenie a editaciu, nie zmazanie
					{
						// ulozim kopiu suboru pod /WEB-INF/filehistory/ alebo cestu
						// urcenu konstantou
						String historyPath = fhb.getHistoryPath() + fhb.getId();
						IwcmFile dest = new IwcmFile(Tools.getRealPath(historyPath));
						boolean copyOk = FileTools.copyFile(file, dest);
						if (copyOk)
						{
							Logger.debug(IwcmFile.class,"Prekopirovanie suboru " + virtualPath + " do "
										+ historyPath + " prebehlo uspesne!");
							return true;
						}
						else
						{
							Logger.debug(IwcmFile.class,"Prekopirovanie suboru " + virtualPath + " do "
										+ historyPath + " nebolo uspesne!");
							return false;
						}
					}
					return true;
				}
				else
				{
					Logger.debug(IwcmFile.class, "Ukladanie zaznamu pre subor " + virtualPath + " sa nezdarilo!");
					return false;
				}
			}
			else return false;	//ak je to subor z historie preskocim a vratim hodnotu false
		}
		else return false;	//ak sa subor uklada do dbStorage preskocim a vratim hodnotu false
	}
}