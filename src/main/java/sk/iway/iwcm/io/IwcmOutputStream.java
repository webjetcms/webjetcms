package sk.iway.iwcm.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Date;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.PkeyGenerator;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.cluster.ClusterDB;

/**
 * IwcmOutputStream.java
 *
 *@Title webjet4
 *@Company Interway s.r.o. (www.interway.sk)
 *@Copyright Interway s.r.o. (c) 2001-2008
 *@author $Author: jeeff $
 *@version $Revision: 1.5 $
 *@created Date: 24.6.2008 17:25:13
 *@modified $Date: 2009/06/02 06:37:34 $
 */
public class IwcmOutputStream extends OutputStream
{
	private IwcmFile file;
	private int length = 0;// dlzka (velkost) suboru
	private byte[] buffer = null;
	private int bufferStep = 0;
	private FileOutputStream out = null;
	private boolean useDBStorage = false; // ak je false zapisuje sa na disk,ak
	private boolean fileCreated = false;

	// true do db storage
	public IwcmOutputStream(File file) throws IOException
	{
		useDBStorage = IwcmFsDB.useDBStorage(IwcmFsDB.getVirtualPath(file.getPath()));
		if (useDBStorage)
		{
			buffer=new byte[IwcmFsDB.getBlockSize()];
			this.file = new IwcmFile(file);
		}
		else
		{
			//new IwcmFile(file).delete();file.createNewFile();
			if (file.exists()==false) file.createNewFile();
			out = new FileOutputStream(file);
		}
	}

	public IwcmOutputStream(IwcmFile file) throws IOException
	{
		this(new File(file.getPath()));
	}

	public IwcmOutputStream(String filename) throws IOException
	{
		this(filename, false);
	}

	public IwcmOutputStream(String filename, boolean isVirtualPath) throws IOException
	{
		if (isVirtualPath)
		{
			useDBStorage = IwcmFsDB.useDBStorage(filename);
		}
		else
		{
			useDBStorage = IwcmFsDB.useDBStorage(IwcmFsDB.getVirtualPath(filename));
		}
		if (useDBStorage)
		{
			if (isVirtualPath)
			{
				buffer=new byte[IwcmFsDB.getBlockSize()];
				this.file = new IwcmFile(new File(Tools.getRealPath(filename)));
			}
			else
			{
				buffer=new byte[IwcmFsDB.getBlockSize()];
				File f = new File(filename);
				this.file = new IwcmFile(f);
			}
		}
		else
		{
			if (!isVirtualPath)
			{
				File f = new File(filename);
				IwcmFile iwcmFile = new IwcmFile(f);
				if(iwcmFile.exists())
					if(!iwcmFile.delete())
						throw new IOException("Unable to delete file: "+filename);
				//was deleted already, no need to check !exists()
				if(!f.createNewFile())
					throw new IOException("Unable to create file: "+filename);

				out = new FileOutputStream(f);
			}
			else
			{
				File f = new File(Tools.getRealPath(filename));
				IwcmFile iwcmFile = new IwcmFile(f);
				if(iwcmFile.exists())
					if(!iwcmFile.delete())
						throw new IOException("Unable to delete file: "+filename);
				if(!f.createNewFile())
					throw new IOException("Unable to create file: "+filename);
				out = new FileOutputStream(f);
			}
		}
	}

	@Override
	public void write(int in) throws IOException
	{
		if (useDBStorage)
		{
			if (bufferStep < IwcmFsDB.getBlockSize())
			{
				buffer[bufferStep++] = (byte) in;
				length++;
			}
			else
			{
				flushBuffer();
				buffer[bufferStep++] = (byte) in;
				length++;
			}
		}
		else
		{
			out.write(in);
		}
	}

	@Override
	public void write(byte[] in) throws IOException
	{
		if (useDBStorage)
		{
			for (int i = 0; i < in.length; i++)
			{
				if (bufferStep < IwcmFsDB.getBlockSize())
				{
					buffer[bufferStep++] = in[i];
					length++;
				}
				else
				{
					flushBuffer();
					buffer[bufferStep++] = in[i];
					length++;
				}
			}
		}
		else
		{
			out.write(in);
		}
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException
	{
		if (useDBStorage)
		{
			for (int i = off; i < off + len; i++)
			{
				if (bufferStep < IwcmFsDB.getBlockSize())
				{
					buffer[bufferStep++] = b[i];
					length++;
				}
				else
				{
					flushBuffer();
					buffer[bufferStep++] = b[i];
					length++;
				}
			}
		}
		else
		{
			out.write(b, off, len);
		}
	}

	@Override
	public void close() throws IOException
	{
		if (useDBStorage)
		{
			if (buffer == null)
				return;
			Connection db_conn = null;
			PreparedStatement ps = null;

			String virtualPath = IwcmFsDB.getVirtualPath(file.getPath());
			if (IwcmFsDB.getFatIdTable().get(virtualPath) != null)
			{
				file.setFatId(IwcmFsDB.getFatIdTable().get(virtualPath));
			}
			try
			{
				db_conn = DBPool.getConnection();
				if (!fileCreated)
				{
					createFile();//vytvorime subor ak este nebol vytvoreny
				}
				else
				{
					ps = db_conn.prepareStatement("update file_fat set fsize = ? where file_fat_id = ? ");
					int index = 1;
					ps.setInt(index++, length);
					ps.setInt(index++, file.getFatId());
					ps.execute();
					ps.close();
				}
				ps = db_conn.prepareStatement("insert into file_data (file_fat_id,data) values (?,?)");
				ps.setInt(1, file.getFatId());
				byte[] tmp = new byte[bufferStep];//zapiseme obsah buffera
				System.arraycopy(buffer,0, tmp, 0, bufferStep);
				ps.setBytes(2, tmp);
				ps.execute();
				IwcmFsDB.getFatIdTable().put(IwcmFsDB.getVirtualPath(file.getPath()), file.getFatId());
				IwcmFsDB.getModifiedTable().put(IwcmFsDB.getVirtualPath(file.getPath()), file.lastModified());
				ClusterDB.addRefresh(IwcmFsDB.class);
				buffer = null;
				tmp = null;
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
			out.close();

			//ulozim subor aj do historie
			IwcmFsDB.saveFileHistory(file, false);
		}
	}

	@Override
	public void flush() throws IOException
	{
		if (!useDBStorage)
		{
			out.flush();
		}
	}

	private void createFile()
	{
		if (!fileCreated)
		{
			Connection db_conn = null;
			PreparedStatement ps = null;
			try
			{
				if (IwcmFsDB.getFatIdTable().get(IwcmFsDB.getVirtualPath(file.getPath())) != null)
				{
					new IwcmFile(file.getPath()).delete();
				}
				db_conn = DBPool.getConnection();
				ps = db_conn
							.prepareStatement("insert into file_fat (file_fat_id,virtual_path,fsize,depth,last_modified) values (?,?,?,?,?)");
				String virtualPath = IwcmFsDB.getVirtualPath(file.getPath());
				file.setFatId(PkeyGenerator.getNextValue("file_fat_id"));
				ps.setInt(1, file.getFatId());
				ps.setString(2, virtualPath);
				ps.setLong(3, length);
				ps.setInt(4, IwcmFsDB.getDepth(virtualPath));
				Date dt = new Date();
				ps.setLong(5, dt.getTime());
				IwcmFsDB.getModifiedTable().put(IwcmFsDB.getVirtualPath(file.getPath()), dt.getTime());
				ClusterDB.addRefresh(IwcmFsDB.class);
				ps.execute();
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
			fileCreated = true;
		}
	}
	/**
	 * vyvori subor v db ak este nebol a ulozi do db obsah buffera
	 */
	private void flushBuffer()
	{
		if (bufferStep > 0)
		{
			createFile();//vytvrime subor ak este nebol vytvoreny
			Connection db_conn = DBPool.getConnection();
			PreparedStatement ps = null;
			try
			{
				ps = db_conn.prepareStatement("insert into file_data (file_fat_id,data) values (?,?)");
				ps.setInt(1, file.getFatId());
				ps.setBytes(2, buffer);
				ps.execute();
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
			buffer = new byte[IwcmFsDB.getBlockSize()];
			bufferStep = 0;
		}
	}
}
