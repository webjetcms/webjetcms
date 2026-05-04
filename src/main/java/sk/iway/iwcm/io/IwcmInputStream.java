package sk.iway.iwcm.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Tools;

/**
 * IwcmInputStream.java
 *
 * @Title webjet4
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2008
 * @author $Author: jeeff $
 * @version $Revision: 1.6 $
 * @created Date: 24.6.2008 12:46:59
 * @modified $Date: 2009/05/11 10:10:50 $
 */
public class IwcmInputStream extends InputStream
{
	private File file;
	private boolean isDBStorage = false;
	private boolean isJarPackaging = false;
	private FileInputStream in = null;
	private InputStream is = null;
	private int step = 0;
	private int readedBlockCount = 0;
	private ArrayList<Integer> nodeIds;
	private int fatId=-1;

	public IwcmInputStream(File f) throws FileNotFoundException
	{
		String virtualPath = IwcmFsDB.getVirtualPath(f.getPath());

		isDBStorage = IwcmFsDB.useDBStorage(virtualPath);
		isJarPackaging = JarPackaging.isJarPackaging(virtualPath);
		file = f;
		if (isJarPackaging)
		{
			is = JarPackaging.getInputStream(virtualPath);
		}
		else if (!isDBStorage)
		{
			in = new FileInputStream(f);
		}
	}

	public IwcmInputStream(IwcmFile f) throws FileNotFoundException
	{
		this(new File(f.getPath()));
	}

	public IwcmInputStream(File f,int fatId)
	{
		String virtualPath = IwcmFsDB.getVirtualPath(f.getPath());
		isDBStorage = IwcmFsDB.useDBStorage(virtualPath);
		file=new File(Tools.getRealPath(virtualPath));
		if (fatId > 0) this.fatId=fatId;
	}

	public IwcmInputStream(String virtualPath,int fatId)
	{
		this.fatId=fatId;
		isDBStorage = IwcmFsDB.useDBStorage(virtualPath);
		file=new File(Tools.getRealPath(virtualPath));
	}

	public IwcmInputStream(String filename) throws FileNotFoundException
	{
		this(filename, false);
	}

	public IwcmInputStream(String virtualPath, boolean isVirtualPath) throws FileNotFoundException
	{
		if (isVirtualPath)
		{
			isDBStorage = IwcmFsDB.useDBStorage(virtualPath);
		}
		else
		{
			isDBStorage = IwcmFsDB.useDBStorage(IwcmFsDB.getVirtualPath(virtualPath));
		}
		if (!isDBStorage)
		{
			if (!isVirtualPath)
			{
				in = new FileInputStream(new File(virtualPath));
			}
			else
			{
				in = new FileInputStream(new File(Tools.getRealPath(virtualPath)));
			}
		}
		else
		{
			if (!isVirtualPath)
			{
				file = new File(virtualPath);
			}
			else
			{
				file = new File(Tools.getRealPath(virtualPath));
			}
		}
	}

	@Override
	public int read() throws IOException
	{
		if (isDBStorage)
		{
			if (!loadFile())
			{
				return -1;
			}
			step++;
			return is.read();
		}
		else if (isJarPackaging)
		{
			return is.read();
		}
		else
		{
			return in.read();
		}
	}

	@Override
	public int read(byte[] result) throws IOException
	{
		if (isDBStorage)
		{
			if (!loadFile())
			{
				return -1;
			}
			int bytesRead = is.read(result);
			step += bytesRead;
			return bytesRead;
		}
		else if (isJarPackaging)
		{
			return is.read(result);
		}
		else
		{
			return in.read(result);
		}
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException
	{
		if (isDBStorage)
		{
			if (!loadFile())
			{
				return -1;
			}
			int bytesRead = is.read(b, off, len);
			step += bytesRead;
			//Logger.debug(IwcmInputStream.class, "read step="+step+" bytesRead="+bytesRead);
			return bytesRead;
		}
		else if (isJarPackaging)
		{
			return is.read(b, off, len);
		}
		else
		{
			return in.read(b, off, len);
		}
	}

	@Override// close je potrebne zavolat,inak zostavaju otvorene spojenia do databazy.
	public void close() throws IOException
	{
		if (isDBStorage)
		{
			try
			{
				if (is!=null) is.close();
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}
		else if (isJarPackaging)
		{
			is.close();
		}
		else
		{
			in.close();
		}
	}

	private boolean loadFile() throws IOException //TODO:dynamicka velkost blokov
	{
		if (step >= IwcmFsDB.getBlockSize() || is == null)// uz sme precitali cely blok alebo este ziaden
		{
			String virtualPath = IwcmFsDB.getVirtualPath(file.getPath());
			if (IwcmFsDB.getFatIdTable().get(virtualPath) == null)
				throw new FileNotFoundException(virtualPath);
			Connection db_conn = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try
			{
				db_conn = DBPool.getConnection();

				if (nodeIds == null)
				{
					nodeIds = new ArrayList<Integer>();
					ps = db_conn.prepareStatement("select node_id from file_data where file_fat_id = ? order by node_id asc");
					if (fatId>0)
					{
						ps.setInt(1, fatId);
					}
					else
					{
						ps.setInt(1, IwcmFsDB.getFatIdTable().get(virtualPath));
					}
					rs = ps.executeQuery();
					while (rs.next())
					{
						nodeIds.add(rs.getInt("node_id"));//precitame a ulozime do listu node_id vsetkych blokov daneho suboru
					}
					rs.close();
					rs = null;
					ps.close();
					ps = null;
				}

				if (nodeIds.size()==0) //prazdny subor.
				{
					if (!existsInDb(IwcmFsDB.getFatIdTable().get(virtualPath)))
					{
						db_conn.close();
						db_conn = null;

						IwcmFsDB.loadHashTables(true);
						nodeIds=null;
						return loadFile();
					}
					return false;
				}

				if (readedBlockCount>=nodeIds.size())
				{
					db_conn.close();
					db_conn = null;

					throw new Exception("Nesúhlasi počet blokov, a velkosť suboru.");
				}
				//Logger.debug(IwcmInputStream.class, "Reading block "+readedBlockCount);

				ps = db_conn.prepareStatement("select data from file_data where node_id = ? ");
				ps.setInt(1, nodeIds.get(readedBlockCount));//vyberieme nasleduci blok
				rs = ps.executeQuery();
				if (rs.next())
				{
					InputStream dbis = rs.getBinaryStream("data");
					//precitaj data do buffera a prehod na IS, aby sme mohli zavret DB spojenie
					ByteArrayOutputStream baos = new ByteArrayOutputStream(IwcmFsDB.getBlockSize());
					//nacitaj
					byte buff[] = new byte[64000];
					int readed;
					while ((readed = dbis.read(buff))!=-1)
					{
						baos.write(buff, 0, readed);
					}
					is = new ByteArrayInputStream(baos.toByteArray());
				}
				rs.close();
				ps.close();
				db_conn.close();

				step = 0;

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

			readedBlockCount++;
			if (is == null)
			{
				throw new FileNotFoundException(file.getPath());
			}
		}
		return true;
	}

	public boolean existsInDb(int fatId)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("select file_fat_id from file_fat where file_fat_id = ?");
			ps.setInt(1, fatId);
			rs = ps.executeQuery();
			while (rs.next())
			{
				return true;
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
		return false;
	}
}
