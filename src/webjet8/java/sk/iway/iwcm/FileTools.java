package sk.iway.iwcm;

import static sk.iway.iwcm.Tools.isNotEmpty;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.tags.support_logic.FormFile;

import sk.iway.iwcm.common.EditTools;
import sk.iway.iwcm.common.FileBrowserTools;
import sk.iway.iwcm.common.FileIndexerTools;
import sk.iway.iwcm.doc.DebugTimer;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.filebrowser.UnusedFile;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmFileFilter;
import sk.iway.iwcm.io.IwcmFsDB;
import sk.iway.iwcm.io.IwcmInputStream;
import sk.iway.iwcm.io.IwcmOutputStream;
import sk.iway.iwcm.io.JarPackaging;
import sk.iway.iwcm.search.SearchService;
import sk.iway.iwcm.stat.Column;
import sk.iway.iwcm.stat.StatNewDB;
import sk.iway.iwcm.users.UserDetails;

/**
 *  FileTools.java - podporne nastroje pre pracu so subormi
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: jeeff $
 *@version      $Revision: 1.35 $
 *@created      Date: 16.8.2004 10:49:27
 *@modified     $Date: 2010/01/20 11:13:38 $
 */
public class FileTools
{
	protected FileTools() {
		//utility class
	}

	public static final List<String> pictureExtensions = Collections.unmodifiableList(Arrays.asList(
				".jpg", ".jpeg", ".gif", ".bmp", "tiff", ".tif", ".png", ".eps"));

	public static final List<String> videoExtensions = Collections.unmodifiableList(Arrays.asList(
				".mp4", ".wmv", ".mpeg", ".3gp", "mkv"));

	/**
	 * Skopiruje subor src do dest
	 * @param src
	 * @param dest
	 * @return
	 */
	public static boolean copyFile(IwcmFile src, IwcmFile dest)
	{
		boolean ret = false;
		try
		{
			Logger.debug(FileTools.class,"copyFile: " + src.getAbsolutePath() + "->" + dest.getAbsolutePath());
			if (dest.getParentFile().exists()==false)
			{
				Logger.debug(FileTools.class,"   creating dir");
				if(dest.getParentFile().mkdirs() == false) return false;
			}
			if (dest.exists()==false)
			{
				Logger.debug(FileTools.class,"   creating new file");
				if(dest.createNewFile() == false) return false;
			}
			IwcmInputStream is = new IwcmInputStream(src);
			IwcmOutputStream os = new IwcmOutputStream(dest);
			byte[] buff = new byte[8000];
			int size;
			while (true)
			{
				size = is.read(buff);
				if (size < 1) break;
				os.write(buff, 0, size);
			}
			os.close();
			is.close();
			ret = true;
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return(ret);
	}

	/**
	 * POZOR pre normalne kopirovanie pouzite verziu s IwcmFile
	 * @param src
	 * @param dest
	 * @return
	 */
	public static boolean copyFile(File src, File dest)
	{
		boolean ret = false;
		try
		{
			Logger.debug(FileTools.class,"copyFile: " + src.getAbsolutePath() + "->" + dest.getAbsolutePath());
			if (dest.getParentFile().exists()==false)
			{
				Logger.debug(FileTools.class,"   creating dir");
				if(dest.getParentFile().mkdirs() == false) return false;
			}
			if (dest.exists()==false)
			{
				Logger.debug(FileTools.class,"   creating new file");
				if(dest.createNewFile() == false) return false;
			}
			FileInputStream is = new FileInputStream(src);
			try
			{
				FileOutputStream os = new FileOutputStream(dest);
				try
				{
					byte[] buff = new byte[8000];
					int size;
					while (true)
					{
						size = is.read(buff);
						if (size < 1) break;
						os.write(buff, 0, size);
					}
				}
				finally { os.close(); }
			}
			finally { is.close(); }
			ret = true;
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return(ret);
	}

	/**
	 * Copy InputStream to IwcmFile and close InputStream
	 * @param is - InputStream
	 * @param dest
	 * @return
	 */
	public static boolean copyFile(InputStream is, IwcmFile dest)
	{
		boolean ret = false;
		try
		{
			Logger.debug(FileTools.class,"copyFile: MultipartFile/InputStreamSource to " + dest.getAbsolutePath());
			if (dest.getParentFile().exists()==false)
			{
				Logger.debug(FileTools.class,"   creating dir");
				if(dest.getParentFile().mkdirs() == false) return false;
			}
			if (dest.exists()==false)
			{
				Logger.debug(FileTools.class,"   creating new file");
				if(dest.createNewFile() == false) return false;
			}

			try
			{
				IwcmOutputStream os = new IwcmOutputStream(dest);
				try
				{
					byte[] buff = new byte[8000];
					int size;
					while (true)
					{
						size = is.read(buff);
						if (size < 1) break;
						os.write(buff, 0, size);
					}
				}
				finally { os.close(); }
			}
			finally { is.close(); }
			ret = true;
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return(ret);
	}

	/**
	 * Presunie subor z URL adresy orig na dest
	 * @param origUrl
	 * @param destUrl
	 * @return
	 */
	public static boolean moveFile(String origUrl, String destUrl)
	{
		IwcmFile origFile = new IwcmFile(Tools.getRealPath(origUrl));
		IwcmFile destFile = new IwcmFile(Tools.getRealPath(destUrl));

		return moveFile(origFile, destFile);
	}

	/**
	 * Presunie subor, ak sa presun nepodari na urovni FS pokusi sa subor prekopirovat a povodny zmazat
	 * @param origFile
	 * @param destFile
	 * @return
	 */
	public static boolean moveFile(IwcmFile origFile, IwcmFile destFile)
	{
		try
		{
			Logger.debug(FileTools.class, "moveFile "+origFile.getAbsolutePath()+" to "+destFile.getAbsolutePath());

			if (destFile.getParentFile().exists()==false) destFile.getParentFile().mkdirs();

			if (origFile.renameTo(destFile)) return true;

			Logger.debug(FileTools.class, "moveFile "+origFile.getAbsolutePath()+" to "+destFile.getAbsolutePath()+" fail, trying copy");

			//premenovanie sa nepodarilo, subor moze byt locknuty, sprav kopiu
			copyFile(origFile, destFile);
			boolean deleted = origFile.delete();

			Logger.debug(FileTools.class, "moveFile "+origFile.getAbsolutePath()+" deleted="+deleted);

			return deleted;
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return false;
	}

	/**
	 * Rekurzivne vymaze adr. strukturu
	 *
	 * @param file
	 * @return
	 */
    public static boolean deleteDirTree(IwcmFile file)
    {
        return deleteDirTree(file, 0);
    }

    /**
     * Zmaze v adresarovej strukture subory starsie ako zadany pocet milisekund, zmaze aj prazdne adresare
     * @param file
     * @param minFileAge
     * @return
     */
	public static boolean deleteDirTree(IwcmFile file, long minFileAge)
	{
		boolean result = true;

		long minLastModified = Tools.getNow() - minFileAge;

		if (file != null)
		{
			int size;
			int i;

			try
			{
				if (file.isDirectory())
				{
					IwcmFile f;
					IwcmFile[] files = file.listFiles();
					size = files.length;
					for (i=0; i<size; i++)
					{
						f = files[i];
						if (f.isDirectory())
						{
							result = deleteDirTree(f, minFileAge);
						}
						else
						{
						    if (minFileAge<1 || f.lastModified() < minLastModified)
                            {
                                result = f.delete();
                                if (f.getVirtualPath().startsWith("/files"))
                                {
                                    //vymazanie full text indexu
                                    FileIndexerTools.deleteIndexedFile(f.getVirtualPath());
                                }
                            }
                            else
                            {
                                result = false;
                            }
						}
					}
					if (result) result = file.delete();
				}
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}
		return (result);
	}

	public static boolean saveFileContent(String url, String data)
	{
		return saveFileContent(url, data, Constants.FILE_ENCODING);
	}

	/**
	 * Ulozi obsah retazca do suboru zadaneho URL so zadanym kodovanim
	 * @param url - napr. /files/subor.txt
	 * @param data - obsah suboru
	 * @param encoding - kodovanie, napr. windows-1250
	 * @return
	 */
	public static boolean saveFileContent(String url, String data, String encoding)
	{
		boolean ret = false;

		if (encoding == null) encoding = Constants.FILE_ENCODING;

		try
		{
			IwcmFile f = new IwcmFile(Tools.getRealPath(url));

			if (f.getParentFile().exists()==false)
			{
				if(f.getParentFile().mkdirs() == false) return false;
			}

			if (f.exists()==false)
			{
				if(f.createNewFile() == false) return false;
			}

			IwcmOutputStream ios = new IwcmOutputStream(f);
			OutputStreamWriter osw = new OutputStreamWriter( ios, encoding);
			osw.write(data);
			osw.close();
			ios.close();

			ret = true;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		return(ret);
	}

	/**
	 * Vrati URL obrazku s ikonou suboru
	 * @param url
	 * @return
	 */
	public static String getFileIcon(String url)
	{
		String icon = "/components/_common/mime/default.gif";
		try
		{
			String ext = url.substring(url.lastIndexOf('.') + 1).toLowerCase();
			ext = ext.trim().toLowerCase();
			IwcmFile f = new IwcmFile(Tools.getRealPath("/components/_common/mime/" + ext + ".gif"));
			if (f.exists())
			{
				icon = "/components/_common/mime/" + ext + ".gif";
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return(icon);
	}

	private static final DecimalFormat decimalFormat;
	private static final DecimalFormat decimalFormatBezMiest;
	private static final DecimalFormat decimalFormatJednoMiesto;

	static
	{
		decimalFormat = new DecimalFormat("0.##");
		decimalFormatBezMiest = new DecimalFormat("0");
		decimalFormatJednoMiesto = new DecimalFormat("0.#");
	}

	/**
	 * Vrati naformatovanu velkost suboru v B, kB, MB
	 * @param url
	 * @return
	 */
	public static String getFileLength(String url)
	{
		return getFileLength(url, true);
	}

	/**
	 * Vrati naformatovanu velkost suboru v B, kB, MB
	 * @param url
	 * @param exactFormat - ak je nastavene na false, tak iba MB vracia s desatinnymi miestami
	 * @return
	 */
	public static String getFileLength(String url, boolean exactFormat)
	{
		double lengthDouble = 0;
		IwcmFile f = new IwcmFile(Tools.getRealPath(url));

		IwcmFile fCusom = null;
		String customPath = PathFilter.getCustomPath();
		if (customPath != null)
		{
			//skontroluj, ci pozadovany subor nie je custom
			fCusom = new IwcmFile(customPath + File.separatorChar + Constants.getInstallName() + url.replace('/', File.separatorChar));
			if (fCusom.exists() && fCusom.canRead()) f = fCusom;
		}

		if (f.isDirectory() || f.exists()==false) return("");

		lengthDouble = f.length();
		return getFormatFileSize((long)lengthDouble, exactFormat);
	}

	/**
	 * Vrati naformatovanu velkost suboru v B, kB, MB
	 * @param lengthLong
	 * @param exactFormat - ak je nastavene na false, tak iba MB vracia s desatinnymi miestami
	 * @return
	 */
	public static String getFormatFileSize(long lengthLong, boolean exactFormat)
	{
		String length = "";
		if (lengthLong > (1024 * 1024))
		{
			if (exactFormat)
			{
				length = decimalFormat.format(lengthLong / (1024d * 1024d)) + " MB";
				Logger.debug(FileTools.class, "DecimalFormat: "+decimalFormatJednoMiesto.format(lengthLong / (1024d * 1024d)));
			}
			else
			{
				length = decimalFormatJednoMiesto.format(lengthLong / (1024d * 1024d)) + " MB";
			}
		}
		else if (lengthLong > 1024)
		{
			if (exactFormat) length = decimalFormat.format(lengthLong / 1024d) + " kB";
			else length = decimalFormatBezMiest.format(lengthLong / 1024d) + " kB";
		}
		else
		{
			if (exactFormat) length = decimalFormat.format(lengthLong) + " B";
			else length = decimalFormatBezMiest.format(lengthLong) + " B";
		}
		return(length);
	}

	/**
	 * Metoda vrati zoznam stranok (url+nazov) a suborov kde sa nachadza
	 * @param url - url adresa suboru, napr. /images/wjlogo.gif
	 * @return
	 */
	public static List<Column> getFileUsage(String url, UserDetails user) {
		//Back compatibility
		return getFileUsage(url, new Identity(user));
	}

	/**
	 * Metoda vrati zoznam stranok (url+nazov) a suborov kde sa nachadza
	 * @param url - url adresa suboru, napr. /images/wjlogo.gif
	 * @return
	 */
	public static List<Column> getFileUsage(String url, Identity user)
	{
		Logger.debug(FileTools.class, "getFileUsage: url="+url);

		List<DocDetails> pages =  SearchService.searchTextAll(url, "docs", -1, user);
		List<Column> dokumenty = new ArrayList<>();
	   	Column col;

		for(int i=0;i<pages.size();i++)
		{
			DocDetails doc = pages.get(i);
			if(Tools.isNotEmpty(doc.getVirtualPath()) && doc.getVirtualPath().startsWith("/files")) {
				continue;
			}

			col = new Column();
			col.setIntColumn1(doc.getDocId());
			col.setColumn1(doc.getTitle());
			col.setColumn2(doc.getDocLink());
			dokumenty.add(col);
      	}

		List<Column> cesty = new ArrayList<>();
		cesty.addAll(dirRekurzia("/components/"+Constants.getInstallName()+"/", url));
		cesty.addAll(dirRekurzia("/templates/", url));
		cesty.addAll(dirRekurzia("/files/", url));
		cesty.addAll(dirRekurzia("/images/", url));
		cesty.addAll(dokumenty);

		return(cesty);
	}


	private static List<Column> dirRekurzia(String rootURL, String testPath)
	{
		List<Column> foundFiles = new ArrayList<>();

		if (rootURL.endsWith("/")== false) rootURL = rootURL + "/";

		IwcmFile rootDir = new IwcmFile(Tools.getRealPath(rootURL));
		IwcmFile[] files = rootDir.listFiles();
		IwcmFile myFile;
	   	Column col;
		int i;
		for (i=0; i<files.length; i++)
		{
			myFile = files[i];
			if(myFile.getPath().contains("node_modules") == false) {
				if (myFile.isDirectory())
				{
					foundFiles.addAll(dirRekurzia(rootURL + myFile.getName() + "/", testPath));
				}
				else
				{
					//nacitanie suboru
					String data = readFileContent(rootURL + myFile.getName());
					if (data.indexOf(testPath)!=-1)
					{
						col = new Column();
						col.setColumn1(myFile.getName());
						col.setColumn2(rootURL + myFile.getName());
						foundFiles.add(col);
					}
				}
			}
		}

		return(foundFiles);
	}
	/**
	 * Skopiruje subor src do out
	 * @param src
	 * @param out
	 * @return
	 */
	public static boolean copyFile(FormFile src, File out)
	{
		IwcmFile dest=new IwcmFile(out);
		boolean ret = false;
		try
		{
			//Logger.debug(FileTools.class,"copyFile: " + src.getAbsolutePath() + "->" + dest.getAbsolutePath());
			if (dest.getParentFile().exists() == false)
			{
				Logger.debug(FileTools.class,"   creating dir");
				if(dest.getParentFile().mkdirs() == false) return false;
			}
			if (dest.exists()==false)
			{
				Logger.debug(FileTools.class,"   creating new file");
				if(dest.createNewFile() == false) return false;
			}
			InputStream is = src.getInputStream();
			IwcmFsDB.writeFiletoDest(is,out,src.getFileSize());
			ret = true;
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return(ret);
	}

	/**
	 * Skontroluje, ci subor na zadanom URL existuje a je to citatelny subor
	 * @param url
	 * @return
	 */
	public static boolean isFile(String url)
	{
		IwcmFile f = new IwcmFile(Tools.getRealPath(url));
		if (f.exists() && f.isFile() && f.canRead()) return true;

		return false;
	}

    public static boolean isDirectory(String url)
    {
        IwcmFile f = new IwcmFile(Tools.getRealPath(url));
        if (f.exists() && f.isDirectory() && f.canRead()) return true;

        return false;
    }

	public static boolean exists(String url)
	{
		IwcmFile f = new IwcmFile(Tools.getRealPath(url));
		if (f.exists() && f.canRead()) return true;

		return false;
	}

	public static String getFileExtension(String fileName)
	{
		if (!fileName.contains("."))
			return "";
		return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
	}

	public static String getFileNameWithoutExtension(String fileName)
	{
		fileName = fileName.substring( fileName.lastIndexOf(File.separator) + 1);
		if (!fileName.contains("."))
			return fileName;

		return fileName.substring(0, fileName.indexOf('.'));
	}

	public static String getFilePathWithoutExtension(String filePath)
	{
		if (!filePath.contains("."))
			return filePath;

		return filePath.substring(0, filePath.lastIndexOf('.'));
	}


	public static void copyDirectory(IwcmFile from, IwcmFile to) throws IOException
	{
		if (!from.isDirectory() || !to.isDirectory())
			throw new IllegalArgumentException("Invalid function call FileTools.copyDirectory() Both arguments have to be a directory");

		for (IwcmFile child : from.listFiles())
		{
			IwcmFile newChild = new IwcmFile(to.getAbsolutePath(), child.getName());
			try
			{
				if (!child.isDirectory())
				{
					newChild.createNewFile();
					copyFile(child, newChild);
				}
				else
				{
					newChild.mkdir();
					copyDirectory(child, newChild);
				}
			}
			catch (Exception ex)
			{

			}
		}
	}

	public static boolean downloadFile(String url, String localPath)
	{
		return downloadFile(url, localPath, null, 0, -1);
	}

	public static boolean downloadFile(String url, String localPath, String[] reqProperty)
	{
		return downloadFile(url, localPath, reqProperty, 0, -1);
	}

	/**
	 * Stiahne subor so zadanym URL do lokalneho suboru
	 * @param url
	 * @param localPath
	 * @param reqProperty - prida RequestProperty
	 * @return
	 */
	public static boolean downloadFile(String url, String localPath, String[] reqProperty, int redirectCounter, int timeOutSeconds)
	{
		if (url.startsWith("http://") || url.startsWith("https://"))
		{
			try
			{

				url = Tools.natUrl(url);

				if (url.startsWith("https://"))
				{
					Tools.doNotVerifyCertificates();
				}

				Logger.debug(Tools.class, "DownloadUrl: " + url);

				//body obsahuje URL adresu, ktoru je treba stiahnut
				HttpURLConnection conn = null;
				URL urlObj = new URL(url);
				conn = (HttpURLConnection) urlObj.openConnection();

                if(timeOutSeconds > 0)
                {
                    conn.setConnectTimeout(timeOutSeconds*1000);
                    conn.setReadTimeout(timeOutSeconds*1000);
                }

				conn.addRequestProperty("User-Agent", Constants.getString("downloadUrlUserAgent"));

				if(reqProperty != null)
					for (String rp : reqProperty)
					{
						String[] prop = Tools.getTokens(rp, ";");
						if(prop.length > 1)
							conn.setRequestProperty(prop[0],prop[1]);
					}

				conn.setAllowUserInteraction(false);
				conn.setDoInput(true);
				conn.setDoOutput(false);
				conn.connect();

				int responseCode = conn.getResponseCode();
				if ((responseCode / 100) == 3 && redirectCounter < 10) {
					String newLocation = conn.getHeaderField( "Location" );
					return downloadFile(newLocation, localPath, reqProperty, redirectCounter++, timeOutSeconds);
				}

				BufferedInputStream is = new BufferedInputStream(conn.getInputStream());

				IwcmFile localFile = new IwcmFile(Tools.getRealPath(localPath));
				localFile.getParentFile().mkdirs();

				IwcmOutputStream ios = new IwcmOutputStream(localFile);

				byte[] buffer = new byte[8000];
				int n = 0;
				while (true)
				{
					 n = is.read(buffer);
					 if (n < 1) break;
					 ios.write(buffer, 0, n);
				}
				is.close();
				ios.close();

				return true;
			}
			catch (Exception ex)
			{
				Logger.error(Tools.class,"ERROR downloadUrl("+url+")");
				sk.iway.iwcm.Logger.error(ex);
			}
		}
		return false;
	}


	public static boolean isImage(String name)
	{
		if (name == null)
			return false;
		//.jpg is not equal to .JPG
		name = name.toLowerCase();

		for (String extension : pictureExtensions)
			if (name.endsWith(extension))
				return true;

		return false;
	}
	public static boolean isVideoFile(String name)
	{
		if (name == null)
			return false;
		//.jpg is not equal to .JPG
		name = name.toLowerCase();

		for (String extension : videoExtensions)
			if (name.endsWith(extension))
				return true;

		return false;
	}
	/**
	 * Usortuje subory podla mena
	 * @param arrayfile
	 * @return
	 */
	public static IwcmFile[] sortFilesByName(IwcmFile[] arrayfile)
	{
		try
		{
			//usortuj to podla abecedy
			Arrays.sort(arrayfile,
				new Comparator<IwcmFile>()
				{
				@Override
					public int compare(IwcmFile f1, IwcmFile f2)
					{
						return (f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase()));
					}
				});
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		return arrayfile;
	}

	/**
	 * Usortuje subory podla mena
	 * @param files
	 * @return
	 */
	public static List<IwcmFile> sortFilesByName(List<IwcmFile> files)
	{
		try
		{
			//usortuj to podla abecedy
			Collections.sort(files,
				new Comparator<IwcmFile>()
				{
				@Override
					public int compare(IwcmFile f1, IwcmFile f2)
					{
						return (f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase()));
					}
				});
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		return files;
	}

	public static boolean isFileEditable(String url)
	{
		if (Tools.isEmpty(url)) return false;

		String ext = getFileExtension(url).toLowerCase();

		if (isImage(url) || ext.equals("doc") || ext.equals("docx") || ext.equals("xls") || ext.equals("xlsx") || ext.equals("pdf") ||
			ext.equals("zip") || ext.equals("rar") ||
			ext.equals("flv") || ext.equals("avi") || ext.equals("mpg") || ext.equals("mov") || ext.equals("mp4") || ext.equals("mp3"))
		{
			return false;
		}

		boolean editable = true;

		String encoding = Constants.FILE_ENCODING;

		try
		{
			IwcmFile f = new IwcmFile(Tools.getRealPath(url));

			IwcmFile fCusom = null;
			if (Tools.isNotEmpty(PathFilter.getCustomPath()))
			{
				//skontroluj, ci pozadovany subor nie je custom
				fCusom = new IwcmFile(PathFilter.getCustomPath() + File.separatorChar + Constants.getInstallName() + url.replace('/', File.separatorChar));
				if (fCusom.exists() && fCusom.canRead()) f = fCusom;
			}

			if (f.exists() && f.canRead())
			{
				IwcmInputStream is = new IwcmInputStream(f);
				InputStreamReader isr = new InputStreamReader(is, encoding);
				char[] buff = new char[64000];
				int len;
				while ((len = isr.read(buff))!=-1)
				{
					//Logger.debug(FileTools.class, "Reading: "+len+" total: "+contextFile.length());
					String data = new String(buff, 0, len);
					if (EditTools.parseLine(data)==false)
					{
						editable = false;
						break;
					}
				}
				is.close();
				isr.close();
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		//System.out.println("!!!! in fileContent "+url);
   	return editable;
	}

	/**
	 * vrati rekurzivne vsetky subory, ktore vyhovuju vlozenemu filtru
	 *
	 * @param root adresar v ktorom chceme vyhladat subory
	 * @param filter instancia rozhrania filter s podmienkami akceptacie suborov
	 * @return
	 */
	public static List<IwcmFile> getFilesRecursive(IwcmFile root, IwcmFileFilter filter)
	{
		List<IwcmFile> result = new ArrayList<>();
		if (root.exists() && root.isDirectory())
		{
			IwcmFile[] files = root.listFiles();
			for (IwcmFile file : files)
			{
				if (file.isDirectory())
				{
					result.addAll(getFilesRecursive(file, filter));
				}
				if (filter.accept(file))
				{
					result.add(file);
				}
			}
		}
		return result;
	}

	/**
	 * Overi, ci je mozne nahrat dany subor na server
	 * @param user
	 * @param fileName
	 * @return
	 */public static boolean isFileAllowedForUpload(Identity user, String fileName)
	{
		//zmenene z false na true pretoze potom sa zle plnili polia so subormi a padalo to dalej na NPE
		if (fileName == null || Tools.isEmpty(fileName)) return true;

		if (user!=null && user.isAdmin()) return true;

		String ext = FileTools.getFileExtension(fileName);
		if (ext==null) return false;

		ext = ext.toLowerCase();

		if (ext.equals("jsp") || ext.equals("class") || ext.equals("java")) return false;
		if (FileBrowserTools.hasForbiddenSymbol(fileName)) return false;

		return true;
	}

	/**
	 * Na Tomcat8/Java8 sa symlinky interpretuju cez servletContext.getRealPath ako cesty mimo rootu a nie ako root/images,
	 * z /www/tomcat-test/webapps/webjet/images sa stane /mnt/agluster/images co robi potom bordel
	 * tato metoda zabezpeci spatne premenovanie cesty zacinajucu na root
	 * @param path
	 * @return
	 */
	public static String symlinkReplaceToRootPath(String path)
	{
		String symlinkTranslate = Constants.getString("symlinkTranslate");
		if (Tools.isEmpty(symlinkTranslate)) return path;

		String pathOriginal = path;

		try
		{
			String[] paths = Tools.getTokens(symlinkTranslate, "\n");
			if (paths==null || paths.length==0) return path;
			for (String pathPair : paths)
			{
				if (Tools.isEmpty(pathPair)) continue;
				String[] pathPairArray = pathPair.split("\\|");
				if (pathPairArray==null || pathPairArray.length!=2) continue;

				if (path.startsWith(pathPairArray[0])==true && path.startsWith(pathPairArray[1])==false)
				{
					path = Tools.replace(path, pathPairArray[0], pathPairArray[1]);
					Logger.debug(FileTools.class, "symlinkReplaceToRootPath: "+pathOriginal+"->"+path);
				}
			}
		}
		catch (Exception ex)
		{
		}

		return path;
	}

	/**
	 * Vytvori defaultne adresare pre file system (/images,/files,/images/gallery,/images/video)
	 */
	public static void createDefaultStaticContentFolders()
	{
		//ak neexistuju, vytvor foldre pre domenu
		String[] prefixes = {"/images", "/files", "/images/gallery", "/images/video"};
		for (String prefix : prefixes)
		{
			IwcmFile dirFile = new IwcmFile(Tools.getRealPath(prefix));
			if (dirFile.exists()==false)
			{
				boolean ret = dirFile.mkdirs();
				Logger.debug(FileTools.class, "CREATING DIR: "+dirFile.getAbsolutePath()+" ret="+ret);
			}
		}
	}

	public static List<String> getDirsNames(String rootURL) {
	    String root_URL = rootURL;
		List<String> foundDirNames = new ArrayList<>();
		if (root_URL.endsWith("/")== false)
            root_URL = root_URL + "/";
		IwcmFile rootDir = new IwcmFile(Tools.getRealPath(root_URL));
		for (IwcmFile file : rootDir.listFiles()) {
			if (file.isDirectory())
				foundDirNames.add(file.getName());
		}
		Collections.sort(foundDirNames);
		return foundDirNames;
	}

	/**
	 * Nacita obsah suboru na zadanej ceste do retazca
	 * @param url - cesta k suboru typu /files/admin.txt
	 * @return
	 */
	public static String readFileContent(String url)
	{
		return readFileContent(url, Constants.FILE_ENCODING);
	}

	/**
	 * Nacita obsah suboru na zadanom URL do retazca
	 * @param url - cesta k suboru typu /files/admin.txt
	 * @param encoding - kodovanie suboru, napr. windows-1250
	 * @return
	 */
	public static String readFileContent(String url, String encoding)
	{
		if (url == null) return "";

		//Logger.debug(FileTools.class, "Reading file: " + url + " encoding="+encoding);

		StringBuilder contextFile = new StringBuilder();

		if (encoding == null) encoding = Constants.FILE_ENCODING;

		try
		{
			IwcmFile f = new IwcmFile(Tools.getRealPath(url));

			IwcmFile fCusom = null;
			if (isNotEmpty(PathFilter.getCustomPath()))
			{
				//skontroluj, ci pozadovany subor nie je custom
				fCusom = new IwcmFile(PathFilter.getCustomPath() + File.separatorChar + Constants.getInstallName() + url.replace('/', File.separatorChar));
				if (fCusom.exists() && fCusom.canRead()) f = fCusom;
			}

			if (f.exists() && f.canRead())
			{
				InputStream is;
				if (f.isJarPackaging()) is = JarPackaging.getInputStream(url);
				else is = new IwcmInputStream(f);

				InputStreamReader isr = new InputStreamReader(is, encoding);
				char[] buff = new char[64000];
				int len;
				while ((len = isr.read(buff))!=-1)
				{
					//Logger.debug(FileTools.class, "Reading: "+len+" total: "+contextFile.length());
					contextFile.append(buff, 0, len);
				}
				isr.close();
				is.close();
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		//System.out.println("!!!! in fileContent "+url);
   	return(contextFile.toString());
	}

	/**
	 * Metoda vrati zoznam stranok triedy UnusedFile (nazov suboru + virtualna parent cesta) a suborov, ktore nie su pouzivane t.z. nenachadzaju sa na ziadnej stranke<br />
	 * prehladava aj tieto umiestnenia: <br />
	 * 	banner_banners stlpec banner_location a banner_redirect<br />
	 * 	calendar stlpec description<br />
	 *		document_forum stlpec question<br />
	 *		gallery stlpec image_path (iba pre subory v /images)<br />
	 *		media stlpec media_link<br />
	 *		tips_of_the_day stlpec tip_text<br />
	 *
	 * @param rootUrl - url adresa, napr. /files/mac, ktoreho obsah sa prehladava rekurzivne
	 * @param request - request, z ktoreho zistime domeny
	 *
	 * @return
	 */
	public static List<UnusedFile> getDirFileUsage(String rootUrl, HttpServletRequest request)
	{
		DebugTimer dt = new DebugTimer("getDirFileUsage");

		dt.diff("reading files");

		List<UnusedFile> unusedFiles = new ArrayList<>();
		List<Column> allFiles = directoryScan(rootUrl, "*");
		SortedSet<String> unusedFileNames = new TreeSet<>();
		Set<String> toRemove = new HashSet<>();
		for(Column c : allFiles)
		{
			if (c.getColumn2().startsWith("/WEB-INF")) continue;
			if (c.getColumn2().startsWith("/META-INF")) continue;
			if (c.getColumn2().startsWith("/wjerrorpages")) continue;
			if (c.getColumn2().startsWith("/admin")) continue;
			if (c.getColumn2().startsWith("/components") && c.getColumn2().startsWith("/components/"+Constants.getInstallName())==false) continue;

			if(c.getColumn2().indexOf("CVS")==-1 && c.getColumn2().indexOf("/.")==-1)
			{
				unusedFileNames.add(c.getColumn2());
			}
		}

		dt.diff("files done, size="+unusedFileNames.size());

		//#15279 - treba nacitavat z data lebo v data_asc a nenachadzaju "a href" tagy
		StringBuilder sql = new StringBuilder("SELECT data FROM documents d WHERE (data LIKE ?) ");
		if (rootUrl.length()>4 && Constants.DB_TYPE == Constants.DB_MSSQL) sql = new StringBuilder("SELECT data FROM documents d WHERE CONTAINS(data, ?) ");
		else if (rootUrl.length()>4 && Constants.DB_TYPE == Constants.DB_MYSQL) sql = new StringBuilder("SELECT data FROM documents d WHERE MATCH(title, data) AGAINST (? IN BOOLEAN MODE) ");
		sql.append(" AND (virtual_path IS NULL OR virtual_path='' OR virtual_path NOT LIKE '/files/%') AND available = 1");

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			//ziskaj udaje z db
			db_conn = DBPool.getConnection();
			ps = StatNewDB.prepareStatement(db_conn, sql.toString());
			if (rootUrl.length()>4 && Constants.DB_TYPE == Constants.DB_MSSQL) ps.setString(1, "\""+rootUrl+"*\"");
			else if (rootUrl.length()>4 && Constants.DB_TYPE == Constants.DB_MYSQL) ps.setString(1, "\""+rootUrl+"\"");
			else ps.setString(1, "%"+rootUrl+"%");

			toRemove.clear();

			dt.diff("Executing sql="+sql);

			rs = ps.executeQuery();

			dt.diff("Reading database");

			while (rs.next())
			{
				if(unusedFileNames.size() == 0) break;

				String dataAsc = DB.getDbString(rs, "data");
				//WAY faster than dataAsc.indexOf...
				List<String> excerpts = findFilesExcerpts(dataAsc, rootUrl);

				for(String fileName : unusedFileNames)
				{
					if(!unusedFileNames.contains(fileName)) continue;
					for(String excerpt : excerpts)
					{
						if(toRemove.contains(fileName)) continue;
						//Logger.debug(FileTools.class, "Excerpt: "+excerpt);
						if(excerpt.contains(fileName))
						{
							toRemove.add(fileName);
						}
					}
				}
				unusedFileNames.removeAll(toRemove);
				toRemove.clear();
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;

			dt.diff("database readed");
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

		dt.diff("Scanning /components/"+Constants.getInstallName());

		List<Column> jspFiles = directoryScan(rootUrl, "/components/"+Constants.getInstallName()+"/*","/templates/*");
		toRemove.clear();

		for(Column c : jspFiles)
		{
			if (c.getColumn2().startsWith("/WEB-INF")) continue;
			if (c.getColumn2().startsWith("/META-INF")) continue;
			if (c.getColumn2().startsWith("/wjerrorpages")) continue;
			if (c.getColumn2().startsWith("/admin")) continue;

			String jspData = readFileContent(c.getColumn2());
			for(String fileName : unusedFileNames)
			{
				if (jspData.indexOf(fileName) > -1)
				{
					toRemove.add(fileName);
				}
			}
			unusedFileNames.removeAll(toRemove);
			toRemove.clear();
		}

		dt.diff("scanning usage in database (components)");

		toRemove.addAll(checkDbUsage(unusedFileNames, rootUrl, "documents", "external_link", "AND (virtual_path IS NULL OR virtual_path='' OR virtual_path NOT LIKE '/files/%') AND available = 1"));

		toRemove.addAll(checkDbUsage(unusedFileNames, rootUrl, "banner_banners", "banner_location", null));
		toRemove.addAll(checkDbUsage(unusedFileNames, rootUrl, "calendar", "description", null));
		toRemove.addAll(checkDbUsage(unusedFileNames, rootUrl, "document_forum", "question", null));

		if(rootUrl.startsWith("/images/"))	//iba pre subory v images
			toRemove.addAll(checkDbUsage(unusedFileNames, rootUrl, "gallery", "image_path", null));
		toRemove.addAll(checkDbUsage(unusedFileNames, rootUrl, "media", "media_link", null));
		toRemove.addAll(checkDbUsage(unusedFileNames, rootUrl, "tips_of_the_day", "tip_text", null));

		unusedFileNames.removeAll(toRemove);
		toRemove.clear();

		Logger.debug(FileTools.class, "Checking files");

		for(String fileName : unusedFileNames)
		{
			IwcmFile unusedFile = new IwcmFile(Tools.getRealPath(fileName));
			if(unusedFiles.add(new UnusedFile(unusedFile.getName(), unusedFile.getVirtualParent(), unusedFile.lastModified(), unusedFile.length())))
			{
				Logger.debug(FileTools.class, "\t\t" + unusedFile.getVirtualPath() + " added ");
			}
		}

		return unusedFiles;
	}

	private static List<String> findFilesExcerpts(String dataAsc, String fileDirectory)
	{
		List<String> excerpts = new ArrayList<>();
		Pattern pattern = Pattern.compile(fileDirectory+".*?[<>\\n]", Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(dataAsc);

		while (matcher.find())
		{
			excerpts.add(matcher.group(0));
		}

		return excerpts;
	}

	/**
	 * Metoda vrati boolean hodnotu, ze ci sa zadana URL pouziva v casti databazy urcenej parametrami: <br />
	 *
	 * @author jraska
	 *
	 * @param unusedFileNames - mnozina obsahujuca relativne cesty suborov ktore sa budu prehladavat po tom, ako sa z DB nacitaju zaznamy obsahujuce korenovy adresar
	 * @param rootUrl - String, ktory vyhladavame v databaze, ide o relativnu virtualnu cestu ku korenovemu adresaru napr. images/gallery/
	 * @param tableName - nazov tabulky v ktorej sa vyhladava
	 * @param columnName - nazov stlpca v ktorom sa vyhladava
	 * @return
	 */
	private static Set<String> checkDbUsage(Set<String> unusedFileNames, String rootUrl, String tableName, String columnName, String addWhereSql)
	{
		Set<String> toRemove = new HashSet<>();

		String sql = "SELECT "+ columnName + " FROM " + tableName + " WHERE " + columnName + " LIKE ?";

		if (isNotEmpty(addWhereSql)) sql += " " + addWhereSql;

		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			//ziskaj udaje z db
			db_conn = DBPool.getConnection();
			ps = StatNewDB.prepareStatement(db_conn, sql);

			ps.setString(1, "%"+rootUrl+"%");

			toRemove.clear();
			rs = ps.executeQuery();
			while (rs.next())
			{
				String data = DB.getDbString(rs, columnName);
				for(String fileName : unusedFileNames)
				{
					if(data.indexOf(fileName) > -1)
					{
						toRemove.add(fileName);
					}
				}
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

		return toRemove;
	}

	/**
	 * Vyhlada rekurzivne subory, ktore vyhovuju zadanemu pattern-u
	 * @param rootUrl - korenovy adresar z ktoreho zacina vyhladavanie
	 * @param patterns - wild-card patterny (pouzije regex kde "*" nahradi ".*", "?" nahradi ".?" a odescapuje "."). Prefix "-" pred patternom znamena
	 * ze subory nesmu vyhovovat danemu patternu
	 * @return zoznam suborov v <code>Column</code> strukture
	 */
	public static List<Column> directoryScan(String rootUrl, String... patterns)
	{
		Logger.debug(FileTools.class, "directoryScan, rootUrl="+rootUrl);

		List<Column> foundFiles = new ArrayList<>();

		if (rootUrl.endsWith("/")== false)
			rootUrl = rootUrl + "/";

		IwcmFile rootDir = new IwcmFile(Tools.getRealPath(rootUrl));
		IwcmFile[] files = rootDir.listFiles();
		IwcmFile myFile;
	   Column col;
		int i;
		for (i=0; i<files.length; i++)
		{
			myFile = files[i];
			if (myFile.isDirectory())
			{
				foundFiles.addAll(directoryScan(rootUrl + myFile.getName() + "/", patterns));
			}
			else
			{
				//nacitanie suboru
				String filePath = rootUrl + myFile.getName();
				boolean matches = false;
				for(int j=0;j<patterns.length;j++)
				{
					boolean substract = false;
					String pattern = patterns[j].replace(".", "\\.");
					pattern = pattern.replace("*", ".*");
					pattern = pattern.replace("?", ".?");
					if(pattern.charAt(0) == ('-'))
					{
						substract = true;
						pattern = pattern.substring(1);
					}
					if(filePath.matches(pattern) && substract)
					{
						matches = false;
						break;
					}
					if(filePath.matches(pattern))
					{
						matches = true;
					}
					else if(!substract)
					{
						matches = false;
						break;
					}
				}
				if (matches)
				{
					col = new Column();
				   col.setColumn1(myFile.getName());
				   col.setColumn2(filePath);
				   foundFiles.add(col);
				}
			}
		}

		return foundFiles;
	}

    /**
     * Vyhlada rekurzivne dany typ suborov.
     * @param directory
     * @param type
     * @return
     */
	public static List<File> listFilesByType(File directory, String type){
		List<File> ret = new ArrayList<>();
		try {
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(type) || new File(dir, name).isDirectory();
			}
		};
			File[] fList = directory.listFiles(filter);
			if (fList != null) {
				for (File file : fList)
					if (file.isFile())
						ret.add(file);
					else if (file.isDirectory()) {
						ret.addAll(listFilesByType(file, type));
					}
			}
		} catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
		}

		return ret;
	}
}