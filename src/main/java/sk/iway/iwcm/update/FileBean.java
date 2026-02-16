package sk.iway.iwcm.update;

import sk.iway.iwcm.Tools;

/**
 *  FileBean.java - info o subore, kopia sk.iway.updater.FileBean aby sa nemusel pribalovat updater package
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: jeeff $
 *@version      $Revision: 1.1 $
 *@created      $Date: 2004/05/28 14:56:05 $
 *@modified     $Date: 2004/05/28 14:56:05 $
 */
public class FileBean
{
	private String filePath;
	private long lastModified;
	private long localLastModified;
	private long fileSize;
	
	/**
	 * @return Returns the filePath.
	 */
	public String getFilePath()
	{
		return filePath;
	}
	/**
	 * @param filePath The filePath to set.
	 */
	public void setFilePath(String filePath)
	{
		this.filePath = filePath;
	}
	/**
	 * @return Returns the lastModified.
	 */
	public long getLastModified()
	{
		return lastModified;
	}
	
	public String getLastModifiedString()
	{
		return(Tools.formatDate(lastModified) + " " + Tools.formatTime(lastModified));
	}
	
	/**
	 * @param lastModified The lastModified to set.
	 */
	public void setLastModified(long lastModified)
	{
		this.lastModified = lastModified;
	}
	/**
	 * @return Returns the localLastModified.
	 */
	public long getLocalLastModified()
	{
		return localLastModified;
	}
	
	public String getLocalLastModifiedString()
	{
		if (localLastModified == 0)
		{
			return("");
		}
		return(Tools.formatDate(lastModified) + " " + Tools.formatTime(lastModified));
	}
		
	/**
	 * @param localLastModified The localLastModified to set.
	 */
	public void setLocalLastModified(long localLastModified)
	{
		this.localLastModified = localLastModified;
	}
	/**
	 * @return Returns the fileSize.
	 */
	public long getFileSize()
	{
		return fileSize;
	}
	/**
	 * @param fileSize The fileSize to set.
	 */
	public void setFileSize(long fileSize)
	{
		this.fileSize = fileSize;
	}
	
	public String getFileSizeString()
	{
		if (fileSize == 0)
		{
			return("?");
		}
		return(Tools.formatFileSize(this.fileSize));
	}
}
