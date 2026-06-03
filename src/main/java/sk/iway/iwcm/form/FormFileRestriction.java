package sk.iway.iwcm.form;

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

	@Deprecated(forRemoval = true)
	public boolean isSentFileValid(UploadedFile file)
	{
		return isSentFileValid(file, null) == null;
	}

	@Deprecated(forRemoval = true)
	public boolean isSentFileValid(IwcmFile file)
	{
		return isSentFileValid(file, null) == null;
	}

	/**
	 * Validates a file against this restriction's rules.
	 * Returns an error message key if validation fails, or null if valid.
	 * @param file
	 * @param prop
	 * @return
	 */
	public String isSentFileValid(UploadedFile file, Prop prop) {
		return verifyRestrictions(
			file.getFileName(),
			file.getFileSize(),
			null,
			file,
			prop);
	}

	/**
	 * Validates a file against this restriction's rules.
	 * Returns an error message key if validation fails, or null if valid.
	 * @param file
	 * @param prop
	 * @return
	 */
	public String isSentFileValid(IwcmFile file, Prop prop) {
		return verifyRestrictions(
			XhrFileUploadService.getOriginalFileName(file),
			file.length(),
			file,
			null,
			prop
		);
	}

	/**
	 * Validates a file against this restriction's rules.
	 * Returns an error message key if validation fails, or null if valid.
	 */
	private String verifyRestrictions(String fileName, long fileSize, IwcmFile iwcmFile, UploadedFile file, Prop prop) {
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

		if (ImageTools.isImage(fileName)) {
			RestrictionImageSize restrictionImageSize = null;
			if (file != null) {
				restrictionImageSize = hasNeededWidthAndHeight(file);
			}
			else if (iwcmFile != null) {
				restrictionImageSize = hasNeededWidthAndHeight(iwcmFile);
			}
			if (restrictionImageSize == null || restrictionImageSize.valid == false) {
				return prop.getText(
					"components.forms.image_dimensions_err",
					fileName,
					restrictionImageSize != null ? restrictionImageSize.width + "" : "?",
					restrictionImageSize != null ? restrictionImageSize.height + "" : "?",
					pictureWidth + "",
					pictureHeight + ""
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

	private RestrictionImageSize hasNeededWidthAndHeight(UploadedFile file)
	{
		try
		{
			ImageInfo imageInformation = new ImageInfo(file);
			boolean valid = (pictureHeight <= 0 || imageInformation.getHeight() <= pictureHeight)
				&& (pictureWidth <= 0 || imageInformation.getWidth() <= pictureWidth);

			return new RestrictionImageSize(imageInformation.getWidth(), imageInformation.getHeight(), valid);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	private RestrictionImageSize hasNeededWidthAndHeight(IwcmFile file)
	{
		try
		{
			ImageInfo imageInformation = new ImageInfo(file);
			boolean valid = (pictureHeight <= 0 || imageInformation.getHeight() <= pictureHeight)
						&& (pictureWidth <= 0 || imageInformation.getWidth() <= pictureWidth);

			return new RestrictionImageSize(imageInformation.getWidth(), imageInformation.getHeight(), valid);
		}
		catch (Exception e)
		{
			return null;
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

	public long getMaxCombinedSizeInKilobytes() {
		return maxCombinedSizeInKilobytes;
	}

	public void setMaxCombinedSizeInKilobytes(long maxCombinedSizeInKilobytes) {
		this.maxCombinedSizeInKilobytes = maxCombinedSizeInKilobytes;
	}

	private class RestrictionImageSize
	{
		boolean valid;
		int width;
		int height;

		public RestrictionImageSize(int width, int height, boolean valid)
		{
			this.width = width;
			this.height = height;
			this.valid = valid;
		}
	}
}
