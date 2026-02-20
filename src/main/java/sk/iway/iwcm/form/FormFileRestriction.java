package sk.iway.iwcm.form;

import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.gallery.ImageInfo;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.upload.UploadedFile;

/**
 *  FormFileRestriction.java
 *
 *@Title        webjet4_repair
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: murbanec $
 *@version      $Revision: 1.1 $
 *@created      Date: 24.7.2009 12:46:12
 *@modified     $Date: 2009/07/24 13:28:29 $
 */
public class FormFileRestriction
{
	String formName;
	
	String allowedExtensions;
	
	int maxSizeInKilobytes;
	
	int pictureWidth;
	
	int pictureHeight;
	
	public boolean isSentFileValid(UploadedFile file)
	{
		boolean isValid = true;
		isValid &= isBelowMaxSize(file.getFileSize());
		isValid &= hasAllowedExtension(file.getFileName());
		if (isItAPicture(file.getFileName()))
		{
			isValid &= hasNeededWidthAndHeight(file);
		}
		return isValid;
	}

	public boolean isSentFileValid(IwcmFile file)
	{
		boolean isValid = true;
		isValid &= isBelowMaxSize(file.length());
		isValid &= hasAllowedExtension(file.getName());
		if (isItAPicture(file.getName()))
		{
			isValid &= hasNeededWidthAndHeight(file);
		}
		return isValid;
	}

	private boolean isBelowMaxSize(long fileSize)
	{		
		return maxSizeInKilobytes <= 0 || (fileSize/1024) <= maxSizeInKilobytes;
	}

	private boolean hasAllowedExtension(String fileName)
	{
		if (Tools.isEmpty(allowedExtensions))
			return true;

		fileName = fileName.toLowerCase();

		for (String extension : allowedExtensions.toLowerCase().split(","))
		{
			if (fileName.endsWith(extension.trim()))
				return true;
		}
		
		return false;
	}

	private boolean isItAPicture(String fileName)
	{
		fileName = fileName.toLowerCase();

		for (String extension : FileTools.pictureExtensions)
		{
			if (fileName.endsWith(extension))
				return true;
		}
		return false;
	}
	

	private boolean hasNeededWidthAndHeight(UploadedFile file)
	{
		try
		{
			ImageInfo imageInformation = new ImageInfo(file);
			return (pictureHeight <= 0 || imageInformation.getHeight() <= pictureHeight) 
				&& (pictureWidth <= 0 || imageInformation.getWidth() <= pictureWidth);
		}
		catch (Exception e)
		{
			return false;
		}
	}

	private boolean hasNeededWidthAndHeight(IwcmFile file)
	{
		try
		{
			ImageInfo imageInformation = new ImageInfo(file);
			return (pictureHeight <= 0 || imageInformation.getHeight() <= pictureHeight)
						&& (pictureWidth <= 0 || imageInformation.getWidth() <= pictureWidth);
		}
		catch (Exception e)
		{
			return false;
		}
	}
	
	
	public String getFormName()
	{
		return this.formName;
	}

	public FormFileRestriction setFormName(String formName)
	{
		this.formName = formName;
		return this;
	}

	public String getAllowedExtensions()
	{
		return this.allowedExtensions;
	}

	public FormFileRestriction setAllowedExtensions(String allowedExtensions)
	{
		this.allowedExtensions = allowedExtensions;
		return this;
	}

	public int getMaxSizeInKilobytes()
	{
		return this.maxSizeInKilobytes;
	}

	public FormFileRestriction setMaxSizeInKilobytes(int maxSizeInKilobytes)
	{
		this.maxSizeInKilobytes = maxSizeInKilobytes;
		return this;
	}

	public int getPictureWidth()
	{
		return this.pictureWidth;
	}

	public FormFileRestriction setPictureWidth(int pictureWidth)
	{
		this.pictureWidth = pictureWidth;
		return this;
	}

	public int getPictureHeight()
	{
		return this.pictureHeight;
	}

	public FormFileRestriction setPictureHeight(int pictureHeight)
	{
		this.pictureHeight = pictureHeight;
		return this;
	}
}
