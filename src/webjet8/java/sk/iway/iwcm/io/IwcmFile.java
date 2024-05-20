package sk.iway.iwcm.io;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.apache.commons.lang.StringUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.system.cluster.ClusterDB;

/**
 * IwcmFile.java
 *
 * @Title webjet4
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2008
 * @author $Author: jeeff $
 * @version $Revision: 1.19 $
 * @created Date: 24.6.2008 17:05:58
 * @modified $Date: 2010/01/20 11:14:46 $
 */
public class IwcmFile
{
	private int fatId = -1;
	private String fileName = null;
	private String virtualPath = null;
	private File file = null;
	private boolean isDBStorage = false; // ak je false zapisuje sa na disk,ak
	private boolean isJarPackaging = false; //ak je true je subor v JAR archive

	// true do db storage
	/**
	 * filename je skutocna cesta napr. C:\foo\bar.jpg
	 */
	public IwcmFile(String filename)
	{
		this.fileName = filename;
		virtualPath = IwcmFsDB.getVirtualPath(filename);
		isDBStorage = IwcmFsDB.useDBStorage(virtualPath);
		isJarPackaging = JarPackaging.isJarPackaging(virtualPath);
		if (!isDBStorage)
		{
			file = new File(filename);
		}
	}

	public IwcmFile(String basename, String filename)
	{
		this.fileName = basename + File.separator + filename;
		virtualPath = IwcmFsDB.getVirtualPath(basename + File.separator + filename);
		isDBStorage = IwcmFsDB.useDBStorage(virtualPath);
		isJarPackaging = JarPackaging.isJarPackaging(virtualPath);
		if (!isDBStorage)
		{
			file = new File(basename + File.separator + filename);
		}
	}

	public IwcmFile(File file)
	{
		fileName = file.getPath();
		virtualPath = IwcmFsDB.getVirtualPath(file.getPath());
		isDBStorage = IwcmFsDB.useDBStorage(virtualPath);
		isJarPackaging = JarPackaging.isJarPackaging(virtualPath);
		if (!isDBStorage)
		{
			this.file = file;
		}
	}

	public IwcmFile(IwcmFile parent,String child)
	{
		this(parent.getAbsolutePath(),child);
	}

	public static IwcmFile fromVirtualPath(String path)
	{
		return new IwcmFile(Tools.getRealPath(path));
	}

	public static IwcmFile fromVirtualPath(String path, String fileName)
	{
		return new IwcmFile(Tools.getRealPath(path), fileName);
	}

	public boolean createNewFile() throws IOException
	{
		if (isDBStorage)
		{
			if (isDirectory())
			{
				delete();
				IwcmFsDB.createDirectory(virtualPath);
				return true;
			}
			delete();
			// createnewFile() sa vola v pripadoch kedy nie je obsah suboru
			// dolezity a necita sa z neho, lebo bude o chvilu prepisany,
			// ale potrebujeme aby existoval.
			IwcmFsDB.getFatIdTable().put(virtualPath, 0);
			IwcmFsDB.getModifiedTable().put(virtualPath, (long) 0);
			ClusterDB.addRefresh(IwcmFsDB.class);

			return true;
		}
		else
		{
			return file.createNewFile();
		}
	}

	public boolean isDirectory()
	{
		if (isDBStorage)
		{
			if (exists() && IwcmFsDB.getModifiedTable().get(virtualPath) == 0)
			{
				return true;
			}
			else
			{
				if (fileName.substring(fileName.lastIndexOf(File.separator), fileName.length()).contains(".prilohy") 	//v tomto pripade je to adresar -> WebDav integracia do dokument manazmentu (#8185)
							|| !fileName.substring(fileName.lastIndexOf(File.separator), fileName.length()).contains("."))
				{
					return true;
				}
				return false;
			}
		}
		else if (isJarPackaging)
		{
			return JarPackaging.isDirectory(virtualPath);
		}
		else
		{
			return file.isDirectory();
		}
	}

	public boolean delete()
	{
		if (isDBStorage)
		{
			if (Tools.isEmpty(fileName))
				return true;

			Integer fatIdToDelete = IwcmFsDB.getFatIdTable().get(virtualPath);
			if (fatIdToDelete == null)
			{
				return true;
			}
			Connection db_conn = null;
			PreparedStatement ps = null;
			try
			{
				db_conn = DBPool.getConnection();
				if (IwcmFsDB.useVersioning() && IwcmFsDB.useDBStorage())
				{
					ps = db_conn.prepareStatement("update file_fat set is_deleted = ?, last_modified = ?  where file_fat_id = ? "); // nastavi
																																					// sa
																																					// priznak
																																					// zmazaneho
																																					// suboru
					ps.setBoolean(1, true);
					ps.setLong(2, new java.util.Date().getTime());
					ps.setInt(3, fatIdToDelete.intValue());
					ps.execute();
					ps.close();
				}
				else
				{
					ps = db_conn.prepareStatement("delete from file_fat where file_fat_id = ? "); // zmaze
																															// sa
																															// z
																															// tabuliek
					ps.setInt(1, fatIdToDelete.intValue());
					ps.execute();
					ps.close();

					ps = db_conn.prepareStatement("delete from file_data where file_fat_id = ? ");
					ps.setInt(1, IwcmFsDB.getFatIdTable().get(virtualPath));
					ps.execute();
					ps.close();
				}
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

			IwcmFsDB.getModifiedTable().remove(virtualPath);
			IwcmFsDB.getFatIdTable().remove(virtualPath);

			//ak je to adresar tak rekuzivne zmazeme podadresare
			if (isDirectory())
			{
				for (IwcmFile f:this.listFiles())
				{
					f.delete();
				}
			}


			ClusterDB.addRefresh(IwcmFsDB.class);
			return true;
		}
		else
		{
			if (file.exists() && IwcmFsDB.useVersioning() && IwcmFsDB.useDBStorage())
			{
				IwcmFsDB.writeFileToDB(file);
			}
			return file.delete();
		}
	}

	public boolean exists()
	{
		if (isDBStorage)
		{
			if (IwcmFsDB.getFatIdTable().get(virtualPath) != null)
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		else if (isJarPackaging)
		{
			return JarPackaging.exists(virtualPath);
		}
		else
		{
			return file.exists();
		}
	}

	public String getPath()
	{
		return fileName;
	}

	public int getFatId()
	{
		return fatId;
	}

	public void setFatId(int fatId)
	{
		this.fatId = fatId;
	}

	public String getVirtualPath()
	{
		return Tools.replace(virtualPath, "//", "/");
	}

	public void setVirtualPath(String virtualPath)
	{
		this.virtualPath = virtualPath;
	}

	public IwcmFile[] listFiles(final IwcmFileFilter filter)
	{
		if (!this.isDirectory())
			return new IwcmFile[0];
		if (isDBStorage)
		{
			List<IwcmFile> resultList = new ArrayList<>();

			Set<String> paths = IwcmFsDB.getFatIdTable().keySet();

			int slashesInParent = StringUtils.countMatches(virtualPath, "/");

			for (String s:paths)
			{
				if (s.startsWith(virtualPath))
				{
					int slashesInSubfile = StringUtils.countMatches(s, "/");
					if (slashesInParent +1 != slashesInSubfile)
						continue;
					IwcmFile f = new IwcmFile(Tools.getRealPath(s));
					if (filter.accept(f))
					{
						resultList.add(f);
					}
				}
			}
			return resultList.toArray(new IwcmFile[0]);
		}
		else if (isJarPackaging)
		{
			IwcmFile[] files = JarPackaging.listFiles(virtualPath);
			List<IwcmFile> resultList = new ArrayList<>();
			for (IwcmFile f : files) {
				if (filter.accept(f)) resultList.add(f);
			}

			return resultList.toArray(new IwcmFile[0]);
		}
		else
		{

			File[] files = file.listFiles(new FileFilter()
			{
				@Override
				public boolean accept(File pathname)
				{
				if (filter.accept(new IwcmFile(pathname)))
				{
					return true;
				}
				return false;
				}
			});
			if (files==null) return null;
			IwcmFile[] result = new IwcmFile[files.length];
			int i = 0;
			for (File f : files)
			{
				result[i++] = new IwcmFile(f);
			}
			return result;
		}
	}

	public IwcmFile[] listFiles()
	{
		return listFiles(new IwcmFileFilter(){
			@Override
			public boolean accept(IwcmFile pathname)
			{
				return true;
			}
		});
	}

	public String getName()
	{
		if (isDBStorage)
		{
			return fileName.substring(fileName.lastIndexOf(File.separator) + 1);
		}
		else
		{
			return file.getName();
		}
	}

	public boolean isFile()
	{
		if (isDBStorage)
		{
			if (exists() && IwcmFsDB.getModifiedTable().get(virtualPath) > 0)
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		else if (isJarPackaging)
		{
			return JarPackaging.isFile(virtualPath);
		}
		else
		{
			return file.isFile();
		}
	}

	public boolean mkdir()
	{
		return mkdirs();
	}

	public String getParent()
	{
		String fName = fileName;
		if (isDirectory() && !fName.endsWith(File.separator))
		{
			fName += File.separator;
		}

		return fName.substring(0, fName.lastIndexOf(File.separatorChar));
	}

	/**
	 * Vrati virtualnu cestu k suboru, bez mena suboru
	 * doplnene 5.2. 2009 - kmarton - kvoli rekurzivnej kontrole pouzitia suborov, vo funkcii priamo pouzite "/" ako file
	 * separator kvoli tomu, ze predtym to prechadza cez getVirtualPath a tam su vsetky "\" nahradzane "/" ako cesta v prehliadacoch
	 * @author kmarton
	 * @return String  napr. ak je virtualna path /templates/webjet/temp_2.jsp, tak funkcia vrati /templates/webjet
	 * 					 ak je to adresar napr. /templates/webjet/, vrati /templates
	 */
	public String getVirtualParent()
	{
		String fName = this.getVirtualPath();

		if (isDirectory() && fName.endsWith("/") && fName.length()>1)
		{
			fName = fName.substring(0, (fName.length()-1));
		}
		String result = fName.substring(0, fName.lastIndexOf('/'));
		//ak cesta obsahovala len jedlo '/' napr. /images tak z toho spravi prazdny string, pritom parent je /
		if (result.length()==0)
		{
			return "/";
		}
		else
		{
			return result;
		}
	}

	@JsonIgnore
	public IwcmFile getParentFile()
	{
		if (isDBStorage)
		{
			return new IwcmFile(fileName.substring(0, fileName.lastIndexOf(File.separatorChar)));
		}
		else
		{
			return new IwcmFile(file.getParentFile());
		}
	}

	public boolean mkdirs()
	{
		if (!isDBStorage)
		{
			return file.mkdirs();
		}
		else
		{
			String path = virtualPath;
			if (!path.endsWith("/"))
			{
				path += "/";
			}
			//int length = path.split("/").length;
			StringBuilder rootBuf = new StringBuilder();
			for (String dir : path.split("/"))
			{
				if (Tools.isEmpty(dir))
					continue;
				rootBuf.append('/').append(dir);

				IwcmFsDB.createDirectory(rootBuf.toString());
			}
			return true;
		}
	}

	public String getAbsolutePath()
	{
		if (isDBStorage)
		{
			return getPath();
		}
		else
		{
			return file.getAbsolutePath();
		}
	}

	public long getLength() {
		return length();
	}

	public long length()
	{
		if (isDBStorage)
		{
			try
			{
				long size = new SimpleQuery().forLong("select fsize from file_fat where file_fat_id  = ? ",
						IwcmFsDB.getFatIdTable().get(virtualPath));
				return size;
			}
			catch (Exception e)
			{
				return -1;
			}
		}
		else if (isJarPackaging)
		{
			return JarPackaging.getSize(virtualPath);
		}
		else
		{
			return file.length();
		}
	}

	public long lastModified()
	{
		if (isDBStorage)
		{
			if (IwcmFsDB.getModifiedTable().get(virtualPath) == null)
			{
				return 0;
			}
			return IwcmFsDB.getModifiedTable().get(virtualPath);
		}
		else if (isJarPackaging)
		{
			return JarPackaging.getLastMofified(virtualPath);
		}
		else
		{
			return file.lastModified();
		}
	}

	public boolean setLastModified(long time)
	{
		if (isDBStorage)
		{
			return IwcmFsDB.updateLastModified(virtualPath, time);
		}
		else
		{
			return file.setLastModified(time);
		}
	}

	public boolean renameTo(IwcmFile out)
	{
		if (isDBStorage)
		{
		if (out.isDBStorage)
		{
			if (exists())
			{
				Connection db_conn = null;
				PreparedStatement ps = null;
				ResultSet rs = null;
				try
				{
					db_conn = DBPool.getConnection();
					//premenujeme adresar samotny
					ps = db_conn.prepareStatement("update file_fat  set virtual_path = ? where virtual_path = ? ");
					ps.setString(1, out.getVirtualPath());
					ps.setString(2, virtualPath);
					ps.execute();
					ps.close();


					//mysql oracle
					if (Constants.DB_TYPE == Constants.DB_ORACLE || Constants.DB_TYPE == Constants.DB_MYSQL || Constants.DB_TYPE == Constants.DB_PGSQL)
					{
						ps = db_conn
									.prepareStatement("update file_fat set virtual_path =CONCAT( ?,SUBSTR(virtual_path,?)) where virtual_path like ? ");
						int index = 1;
						ps.setString(index++, out.getVirtualPath());
						ps.setInt(index++, virtualPath.length()+1);
						ps.setString(index++, virtualPath +"/"+ "%");
						ps.executeUpdate();
						ps.close();
					}
					else
					{
						//ina syntax substring a concat
						if (Constants.DB_TYPE == Constants.DB_MSSQL)
						{
							ps = db_conn
										.prepareStatement("update file_fat set virtual_path =  ? + SUBSTRING(virtual_path,?,LEN(virtual_path)-?) where virtual_path like ? ");
							int index = 1;
							ps.setString(index++, out.getVirtualPath());
							ps.setInt(index++, virtualPath.length()+1);
							ps.setInt(index++, virtualPath.length());
							ps.setString(index++, virtualPath+"/" + "%");
							ps.executeUpdate();
							ps.close();
						}
					}

					ps = fixDepth(out.getVirtualPath(),db_conn);

					// vyberieme vsetky subory a podadresare
					ps = db_conn.prepareStatement("select * from file_fat where virtual_path like ? ");
					ps.setString(1,out.getVirtualPath()+"/"+"%");
					rs = ps.executeQuery();

					while (rs.next())
					{
						final String newVirtualPath = rs.getString("virtual_path");

						final String oldPath = virtualPath+newVirtualPath.substring(out.getVirtualPath().length());
						// odstranime stare cesty a vlozime nove
						IwcmFsDB.getFatIdTable().remove(oldPath);
						IwcmFsDB.getModifiedTable().remove(oldPath);

						IwcmFsDB.getFatIdTable().put(newVirtualPath,rs.getInt("file_fat_id"));
						IwcmFsDB.getModifiedTable().put(newVirtualPath,rs.getLong("last_modified") );

					}

					int dataId = IwcmFsDB.getFatIdTable().get(virtualPath);
					long lastModified = IwcmFsDB.getModifiedTable().get(virtualPath);
					//ulozime do hash table novu cestu pre adresar
					IwcmFsDB.getFatIdTable().remove(virtualPath);
					IwcmFsDB.getModifiedTable().remove(virtualPath);
					virtualPath = out.getVirtualPath();
					fileName = out.getPath();
					IwcmFsDB.getFatIdTable().put(virtualPath, dataId);
					IwcmFsDB.getModifiedTable().put(virtualPath, lastModified);
					ClusterDB.addRefresh(IwcmFsDB.class);


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
		}
		else
		{
			IwcmFsDB.writeFileToDisk(new File(this.getAbsolutePath()), new File(out.getAbsolutePath()));
			this.delete();
		}
		}
		else
		{
			if (out.isDBStorage)
			{
				IwcmFsDB.writeFileToDB(file,new File(out.getAbsolutePath()));
				this.delete();
			}
			else
			{
				if(file.renameTo(new File(out.getPath())) == false) return false;
			}
		}
		return true;
	}

	private static PreparedStatement fixDepth(String path, Connection db_conn) throws SQLException
	{
		PreparedStatement ps;

		//opravime depth

		//mysql oracle
		if (Constants.DB_TYPE == Constants.DB_ORACLE || Constants.DB_TYPE == Constants.DB_MYSQL || Constants.DB_TYPE == Constants.DB_PGSQL)
		{
			ps = db_conn
						.prepareStatement("update file_fat set depth = length(virtual_path) - length(replace(virtual_path,'/','')) where virtual_path like ? ");
			ps.setString(1, path + "%");
			ps.execute();
			ps.close();
		}
		else
		{
			ps = db_conn
						.prepareStatement("update file_fat set depth = LEN(virtual_path) - LEN(replace(virtual_path,'/','')) where virtual_path like ? ");
			ps.setString(1, path + "%");
			ps.execute();
			ps.close();
		}

		return ps;
	}

	public boolean canRead()
	{
		if (isDBStorage)
		{
			return true;
		}
		else if (isJarPackaging)
		{
			return exists();
		}
		else
		{
			return file.canRead();
		}
	}

	@Override
	public String toString()
	{
		return getVirtualPath();
	}

	/**
	 * @return
	 */
	public String getCanonicalPath()
	{
		if (isDBStorage)
		{
			return getAbsolutePath();
		}
		else
		{
			try
			{
				return file.getCanonicalPath();
			}
			catch (IOException e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}
		return null;
	}

	public boolean isJarPackaging()
	{
		return isJarPackaging;
	}
}
