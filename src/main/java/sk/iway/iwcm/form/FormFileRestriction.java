package sk.iway.iwcm.form;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.ImageTools;
import sk.iway.iwcm.components.upload.XhrFileUploadService;
import sk.iway.iwcm.gallery.ImageInfo;
import sk.iway.iwcm.i18n.Prop;
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

	long maxCombinedSizeInKilobytes;

	int pictureWidth;

	int pictureHeight;

	public boolean isSentFileValid(UploadedFile file)
	{
		return isSentFileValid(file, null) == null;
	}

	public boolean isSentFileValid(IwcmFile file)
	{
		return isSentFileValid(file, null) == null;
	}

	public String isSentFileValid(UploadedFile file, Prop prop) {
		ImageInfo imageInfo = ImageTools.isImage(file.getFileName()) ? new ImageInfo(file) : null;
		return validateFile(file.getFileSize(), file.getFileName(), imageInfo, prop);
	}

	public String isSentFileValid(IwcmFile file, Prop prop) {
		String fileName = XhrFileUploadService.getOriginalFileName(file);
		ImageInfo imageInfo = ImageTools.isImage(fileName) ? new ImageInfo(file) : null;
		return validateFile(file.length(), fileName, imageInfo, prop);
	}

	/**
	 * Validates file restrictions (size, extension, image dimensions).
	 *
	 * @param fileSize the file size in bytes
	 * @param fileName the original file name
	 * @param imageInfo image information if the file is an image, null otherwise
	 * @param prop localization provider
	 * @return error message if validation fails, null if valid
	 */
	private String validateFile(long fileSize, String fileName, ImageInfo imageInfo, Prop prop) {
		if (prop == null) prop = Prop.getInstance();

		if (isBelowMaxSize(fileSize) == false) {
			return prop.getText(
				"components.forms.file_to_big_err",
				fileName,
				FileTools.formatFileSize(fileSize),
				FileTools.formatFileSizeFromKb(maxSizeInKilobytes)
			);
		}

		if (hasAllowedExtension(fileName) == false) {
			return prop.getText(
				"components.forms.bad_file_extension_err",
				fileName,
				allowedExtensions
			);
		}

		if (imageInfo != null) {
			if (hasNeededWidthAndHeight(imageInfo) == false) {
				return prop.getText(
					"components.forms.image_dimensions_err",
					fileName,
					String.valueOf(imageInfo.getWidth()),
					String.valueOf(imageInfo.getHeight()),
					String.valueOf(pictureWidth),
					String.valueOf(pictureHeight)
				);
			}
		}

		return null;
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

	private boolean hasNeededWidthAndHeight(ImageInfo imageInfo)
	{
		return (pictureHeight <= 0 || imageInfo.getHeight() <= pictureHeight)
			&& (pictureWidth <= 0 || imageInfo.getWidth() <= pictureWidth);
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

	public long getMaxCombinedSizeInKilobytes() {
		return maxCombinedSizeInKilobytes;
	}

	public void setMaxCombinedSizeInKilobytes(long maxCombinedSizeInKilobytes) {
		this.maxCombinedSizeInKilobytes = maxCombinedSizeInKilobytes;
	}
}
