package sk.iway.iwcm.io;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.CacheBean;
import sk.iway.iwcm.CacheListener;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;

/**
 *  FileCache.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: jeeff $
 *@version      $Revision: 1.3 $
 *@created      Date: Feb 13, 2009 1:10:53 PM
 *@modified     $Date: 2009/06/02 06:37:34 $
 */
public class FileCache implements CacheListener
{

	public static final String FILE_CACHE_PREFIX = "FSDB_";

	private static Cache cache=Cache.getInstance();
	private static int maxFileSize=10240;
	private static int maxCacheSize = 10240*20;
	private static boolean useFileCache = false;
	private static int timeInCacheInMinutes = 15;
	private static int actualCacheSize = 0;


	static
	{
		FileCache theCache = new FileCache();
		Cache.subscribe(theCache);
	}

	private static long getKey(String virtualPath)
	{
		if (IwcmFsDB.useDBStorage()) return IwcmFsDB.getFatIdTable().get(virtualPath);

		//aby nam to samo expirovalo po zmene datumu
		IwcmFile f = new IwcmFile(Tools.getRealPath(virtualPath));
		return f.lastModified();
	}

	public static void setObject(String virtualPath, byte[] fileData)
	{
		long fatId=getKey(virtualPath);
		if ((actualCacheSize+fileData.length)<=maxCacheSize && fileData.length < maxFileSize)
		{
			cache.setObject(FILE_CACHE_PREFIX+virtualPath+fatId, fileData, timeInCacheInMinutes);
		}
	}

	public static boolean isInCache(String virtualPath)
	{
		long fatId=getKey(virtualPath);
		if (cache.getObject(FILE_CACHE_PREFIX+virtualPath+fatId)!=null)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	public static byte[] getObject(String name)
	{
		long fatId=getKey(name);
		if (fatId < 1) return null;
		return (byte[])cache.getObject(FILE_CACHE_PREFIX+name+fatId);
	}

	public static byte[] getObject(String name, int fatId)
	{
		return (byte[])cache.getObject(FILE_CACHE_PREFIX+name+fatId);
	}

	public static  void init()
	{
		maxFileSize=Constants.getInt("iwfs_maxFileSize");
		maxCacheSize = Constants.getInt("iwfs_maxCacheSize");
		useFileCache = Constants.getBoolean("iwfs_useFileCache");
		timeInCacheInMinutes = Constants.getInt("iwfs_timeInCacheInMinutes");
	}

	public static int getMaxFileSize()
	{
		return maxFileSize;
	}

	public static void setMaxFileSize(int maxFileSize)
	{
		FileCache.maxFileSize = maxFileSize;
	}

	public static int getMaxCacheSize()
	{
		return maxCacheSize;
	}

	public static void setMaxCacheSize(int maxCacheSize)
	{
		FileCache.maxCacheSize = maxCacheSize;
	}

	public static boolean useFileCache()
	{
		return useFileCache;
	}

	public static void setUseFileCache(boolean useFileCache)
	{
		FileCache.useFileCache = useFileCache;
	}

	public static int getTimeInCacheInMinutes()
	{
		return timeInCacheInMinutes;
	}

	public static void setTimeInCacheInMinutes(int timeInCacheInMinutes)
	{
		FileCache.timeInCacheInMinutes = timeInCacheInMinutes;
	}

	@Override
	public void objectAdded(CacheBean theObject)
	{
		if (theObject.getName().startsWith(FILE_CACHE_PREFIX))
		{
			byte[] data = (byte[])theObject.getObject();
			actualCacheSize += data.length; //NOSONAR
		}
	}

	@Override
	public void objectRemoved(CacheBean theObject)
	{
		if (theObject.getName().startsWith(FILE_CACHE_PREFIX))
		{
			byte[] data = (byte[])theObject.getObject();
			actualCacheSize -= data.length; //NOSONAR
		}
	}
}
