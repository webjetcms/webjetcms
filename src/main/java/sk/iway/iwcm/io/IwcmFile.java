package sk.iway.iwcm.io;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.apache.commons.lang3.StringUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.io.s3.S3Path;
import sk.iway.iwcm.io.s3.S3Storage;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.system.cluster.ClusterDB;

/**
 * Represents a CMS path across local, database, packaged JAR and S3 storage.
 * S3 instances retain their domain namespace and client when the request context changes.
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
	private String s3Namespace;
	private S3Storage s3Storage;
	private boolean isDBStorage = false; // ak je false zapisuje sa na disk,ak
	private boolean isJarPackaging = false; //ak je true je subor v JAR archive

	// true do db storage
	/**
	 * Resolves a physical path to its configured CMS storage backend.
	 *
	 * @param filename physical path, for example {@code C:\foo\bar.jpg}
	 */
	public IwcmFile(String filename)
	{
		this.fileName = filename;
		virtualPath = IwcmFsDB.getVirtualPath(filename);
		initializeS3();
		isDBStorage = !isS3() && IwcmFsDB.useDBStorage(virtualPath);
		isJarPackaging = JarPackaging.isJarPackaging(virtualPath);
		if (!isDBStorage)
		{
			file = new File(filename);
		}
	}

	/**
	 * Resolves a physical parent path and child name to their configured storage backend.
	 *
	 * @param basename physical parent directory path
	 * @param filename child path relative to the parent
	 */
	public IwcmFile(String basename, String filename)
	{
		this.fileName = basename + File.separator + filename;
		virtualPath = IwcmFsDB.getVirtualPath(basename + File.separator + filename);
		initializeS3();
		isDBStorage = !isS3() && IwcmFsDB.useDBStorage(virtualPath);
		isJarPackaging = JarPackaging.isJarPackaging(virtualPath);
		if (!isDBStorage)
		{
			file = new File(basename + File.separator + filename);
		}
	}

	/**
	 * Resolves a filesystem path to its configured CMS storage backend.
	 *
	 * @param file physical path used to determine the CMS path and storage
	 */
	public IwcmFile(File file)
	{
		fileName = file.getPath();
		virtualPath = IwcmFsDB.getVirtualPath(file.getPath());
		initializeS3();
		isDBStorage = !isS3() && IwcmFsDB.useDBStorage(virtualPath);
		isJarPackaging = JarPackaging.isJarPackaging(virtualPath);
		if (!isDBStorage)
		{
			this.file = file;
		}
	}

	/**
	 * Creates a child path, retaining the parent's S3 context when the child also routes to S3.
	 * Paths outside S3 are resolved using the current storage configuration.
	 *
	 * @param parent parent directory with its captured storage context
	 * @param child child path relative to the parent
	 */
	public IwcmFile(IwcmFile parent, String child) {
		fileName = parent.getAbsolutePath() + File.separator + child;
		file = new File(fileName);
		if (parent.isS3() && S3Path.usesS3(parent.getVirtualPath() + "/" + child)) {
			virtualPath = S3Path.normalize(parent.getVirtualPath() + "/" + child);
			s3Namespace = parent.s3Namespace;
			s3Storage = parent.s3Storage;
		} else {
			virtualPath = IwcmFsDB.getVirtualPath(fileName);
			initializeS3();
			isDBStorage = !isS3() && IwcmFsDB.useDBStorage(virtualPath);
			isJarPackaging = !isS3() && JarPackaging.isJarPackaging(virtualPath);
		}
	}

	private IwcmFile(String physicalPath, String virtualPath, String namespace, S3Storage storage) {
		this.fileName = physicalPath;
		this.file = new File(physicalPath);
		this.virtualPath = virtualPath;
		this.s3Namespace = namespace;
		this.s3Storage = storage;
	}

	public IwcmFile(Path path) {
		this(path.toAbsolutePath().toString());
	}

	public static IwcmFile fromVirtualPath(String path)
	{
		return new IwcmFile(Tools.getRealPath(path));
	}

	public static IwcmFile fromVirtualPath(String path, String fileName)
	{
		return new IwcmFile(Tools.getRealPath(path), fileName);
	}

	private void initializeS3() {
		if (S3Path.usesS3(virtualPath)) {
			virtualPath = S3Path.normalize(virtualPath);
			s3Namespace = S3Path.namespace(virtualPath);
			s3Storage = S3Storage.getInstance();
		}
	}

	/**
	 * Identifies files routed to S3 without exposing SDK types.
	 *
	 * @return true if this instance has a captured S3 client
	 */
	@JsonIgnore
	public boolean isS3() { return s3Storage != null; }

	/**
	 * Identifies storage that requires a local working copy for native file tools.
	 *
	 * @return true for S3 or database storage
	 */
	@JsonIgnore
	public boolean isRemote() { return isS3() || isDBStorage; }

	String s3Key() { return s3Namespace + S3Path.normalize(virtualPath).substring(1); }
	S3Storage s3Storage() { return s3Storage; }

	/**
	 * Supplies the validator used by HTTP file responses.
	 *
	 * @return quoted S3 ETag, an empty string for missing S3 metadata, or the filename for other backends
	 */
	@JsonIgnore
	public String getETag() {
		if (!isS3()) return getName();
		S3Storage.Metadata metadata = s3Storage.metadata(s3Key());
		return metadata == null ? "" : "\"" + metadata.etag() + "\"";
	}

	private IwcmFile s3Child(String virtual) {
		String physical = Tools.getRealPath(virtual);
		if (S3Path.usesS3(virtual)) return new IwcmFile(physical, virtual, s3Namespace, s3Storage);
		return new IwcmFile(physical);
	}

	/**
	 * Creates an empty file on local or S3 storage, or prepares a database placeholder.
	 * Database storage replaces the existing entry; local and S3 storage preserve an existing target.
	 *
	 * @return whether the backend created or prepared the entry
	 * @throws IOException if local or S3 creation fails
	 */
	public boolean createNewFile() throws IOException
	{
		if (isS3()) return s3Storage.create(s3Key());
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

	/**
	 * Determines whether this path represents a directory in its storage backend.
	 * Database storage also uses the filename to infer directories without directory metadata.
	 *
	 * @return whether the path is treated as a directory
	 */
	public boolean isDirectory()
	{
		if (isS3()) {
			S3Storage.Metadata metadata = s3Storage.metadata(s3Key());
			return metadata != null && metadata.directory() == true;
		}
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

	/**
	 * Deletes this entry using the selected storage backend and its versioning rules.
	 * Local and S3 directories must be empty; database storage also attempts to delete descendants.
	 *
	 * @return backend deletion result; database storage also reports success for a missing entry
	 * @throws java.io.UncheckedIOException if an S3 lookup or deletion fails
	 */
	public boolean delete()
	{
		if (isS3()) return s3Storage.delete(s3Key());
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

	/**
	 * Checks for an entry in the selected backend, including implicit S3 directories.
	 *
	 * @return whether the entry exists
	 * @throws java.io.UncheckedIOException if S3 metadata cannot be read
	 */
	public boolean exists()
	{
		if (isS3()) return s3Storage.metadata(s3Key()) != null;
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

	/**
	 * Lists immediate children accepted by the filter, retaining captured S3 context for remote children.
	 * Local CMS directories also expose existing configured S3 roots beneath them.
	 *
	 * @param filter child filter; null is supported only for S3 listings
	 * @return matching children, an empty array for a non-directory, or null if a local listing fails
	 * @throws java.io.UncheckedIOException if an S3 listing or metadata lookup fails
	 */
	public IwcmFile[] listFiles(final IwcmFileFilter filter)
	{
		if (isS3()) {
			if (!isDirectory()) return new IwcmFile[0];
			List<IwcmFile> children = new ArrayList<>();
			for (S3Storage.Metadata metadata : s3Storage.list(s3Key()).values()) {
				IwcmFile child = s3Child("/" + metadata.key().substring(s3Namespace.length()));
				if (filter == null || filter.accept(child)) children.add(child);
			}
			return children.toArray(new IwcmFile[0]);
		}
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
			if (Constants.getBoolean("s3Enabled") && getCanonicalPath().equals(new IwcmFile(Tools.getRealPath(getVirtualPath())).getCanonicalPath())) {
				java.util.Map<String, IwcmFile> combined = new java.util.LinkedHashMap<>();
				if (files != null) for (File child : files) combined.put(child.getName(), new IwcmFile(child));
				for (String root : S3Path.roots()) {
					int slash = root.lastIndexOf('/');
					String parent = slash == 0 ? "/" : root.substring(0, slash);
					if (!parent.equals(getVirtualPath())) continue;
					IwcmFile child = IwcmFile.fromVirtualPath(root);
					if (child.exists() && (filter == null || filter.accept(child))) combined.put(child.getName(), child);
				}
				return combined.values().toArray(new IwcmFile[0]);
			}
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

	/**
	 * Checks whether the selected backend identifies this entry as a file.
	 *
	 * @return true for a file entry, false for a directory or missing entry
	 * @throws java.io.UncheckedIOException if S3 metadata cannot be read
	 */
	public boolean isFile()
	{
		if (isS3()) {
			S3Storage.Metadata metadata = s3Storage.metadata(s3Key());
			return metadata != null && metadata.directory() == false;
		}
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

	/**
	 * Resolves the parent while retaining captured storage context when both paths use S3.
	 *
	 * @return parent directory wrapper
	 */
	@JsonIgnore
	public IwcmFile getParentFile()
	{
		if (isS3()) return s3Child(getVirtualParent());
		if (isDBStorage)
		{
			return new IwcmFile(fileName.substring(0, fileName.lastIndexOf(File.separatorChar)));
		}
		else
		{
			return new IwcmFile(file.getParentFile());
		}
	}

	/**
	 * Creates the directory and missing ancestors using the selected backend.
	 * S3 writes directory markers so empty directories remain visible.
	 *
	 * @return backend creation result; local and S3 storage return false for an existing target
	 * @throws java.io.UncheckedIOException if an S3 lookup or marker write fails
	 */
	public boolean mkdirs()
	{
		if (isS3()) return s3Storage.mkdirs(s3Key());
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

	/**
	 * Reads the file size from the selected backend.
	 *
	 * @return size in bytes, zero for missing S3 entries and S3 directories, or -1 on a database lookup failure
	 * @throws java.io.UncheckedIOException if S3 metadata cannot be read
	 */
	public long length()
	{
		if (isS3()) {
			S3Storage.Metadata metadata = s3Storage.metadata(s3Key());
			return metadata == null ? 0 : metadata.size();
		}
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

	/**
	 * Reads the modification timestamp from the selected backend.
	 *
	 * @return epoch milliseconds, or zero for missing S3 metadata and S3 directories
	 * @throws java.io.UncheckedIOException if S3 metadata cannot be read
	 */
	public long lastModified()
	{
		if (isS3()) {
			S3Storage.Metadata metadata = s3Storage.metadata(s3Key());
			return metadata == null ? 0 : metadata.modified();
		}
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

	/**
	 * Updates the stored modification timestamp when supported by the backend.
	 *
	 * @param time modification time in epoch milliseconds
	 * @return backend update result, or false for S3 where changing the timestamp is unsupported
	 */
	public boolean setLastModified(long time)
	{
		if (isS3()) return false;
		if (isDBStorage)
		{
			return IwcmFsDB.updateLastModified(virtualPath, time);
		}
		else
		{
			return file.setLastModified(time);
		}
	}

	/**
	 * Moves this entry to the destination using backend-specific rename or copy operations.
	 * Moves involving S3 finish copying the file or tree before starting source deletion;
	 * they are not atomic and failures can leave destination data or partially deleted sources.
	 *
	 * @param out destination path with its own storage context
	 * @return backend move result; legacy database paths can log errors without reporting false
	 * @throws java.io.UncheckedIOException if an S3 operation fails
	 */
	public boolean renameTo(IwcmFile out)
	{
		if (isS3() || out.isS3()) {
			if (isS3() && out.isS3() && s3Storage == out.s3Storage) return s3Storage.rename(s3Key(), out.s3Key());
			// Cross-storage moves must finish the entire copy before deleting any source.
			return FileTools.copyTree(this, out) && (isDirectory() ? FileTools.deleteDirTree(this) : delete());
		}
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

	/**
	 * Checks readability using the backend's available information.
	 * S3 and packaged resources are checked for existence; database storage always returns true.
	 * S3 existence does not guarantee permission to download the object.
	 *
	 * @return local readability, S3 or packaged-resource existence, or true for database storage
	 * @throws java.io.UncheckedIOException if S3 metadata cannot be read
	 */
	public boolean canRead()
	{
		if (isS3()) return exists();
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
