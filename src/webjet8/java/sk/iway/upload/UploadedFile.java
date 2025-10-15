package sk.iway.upload;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.io.IOUtils;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmInputStream;

/**
 *  UploadedFile.java
 *
 *		Adapter for {@link DiskFileItem}
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 24.1.2011 16:14:33
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class UploadedFile
{

	private DiskFileItem diskFileItem = null;
	private IwcmFile localFile = null;

	public UploadedFile(DiskFileItem file)
	{
		this.diskFileItem = file;
	}

	public String getFilePath()
	{
		if (localFile!=null)
			return localFile.getAbsolutePath();
		if (diskFileItem != null)
			return diskFileItem.getStoreLocation().getAbsolutePath();
		else
			return "";
	}


	public String getContentType()
	{
		return diskFileItem.getContentType();

	}


	public byte[] getFileData() throws FileNotFoundException, IOException
	{
		if (localFile!=null && localFile.exists())
		{
			IOUtils.toByteArray(new IwcmInputStream(localFile));
		}

		return diskFileItem.get();

	}

	public String getFileName()
	{
		return diskFileItem.getName();
	}


	public int getFileSize()
	{
		if (localFile!=null && localFile.exists())
		{
			return Tools.safeLongToInt(new File(localFile.getAbsolutePath()).length());
		}

			return (int)diskFileItem.getSize();

	}


	public InputStream getInputStream() throws FileNotFoundException, IOException
	{
		if (localFile!=null && localFile.exists())
		{
			return new IwcmInputStream(localFile);
		}
		return diskFileItem.getInputStream();

	}

	public void destroy()
	{
		if (localFile!=null && localFile.exists()) localFile.delete();

		try {
			diskFileItem.delete();
		} catch (Exception e) {
			//ignore
		}

	}

	public void setLocalFile(IwcmFile localFile)
	{
		this.localFile = localFile;
	}
}