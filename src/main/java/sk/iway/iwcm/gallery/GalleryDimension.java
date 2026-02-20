package sk.iway.iwcm.gallery;

import static sk.iway.iwcm.Tools.isEmpty;

import java.util.Date;

import sk.iway.iwcm.io.IwcmFile;

/** 
 * GalleryDimension.java - informacie o galerii
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: bhric $
 *@version      $Revision: 1.3 $
 *@created      Date: 06.11.2009 11:19:42
 *@modified     $Date: 2009/11/19 22:24:21 $
*/
public class GalleryDimension
{
  	private int galleryId;
  	private String galleryPath = "";
  	private String galleryName = "";
  	private String galleryPerex = "";
  	private Date galleryDate;
  	private int galleryViews = 0;
  	private String author = "";
  	private WatermarkSetup watermarkSetup = new WatermarkSetup();

	public GalleryDimension() 
  	{
		//prazdny konstruktor
  	}
  	
  	public String getAuthor()
	{
		return author;
	}
	public void setAuthor(String author)
	{
		this.author = author;
	}
	public int getGalleryId()
   {
   	return galleryId;
   }
   public void setGalleryId(int galleryId)
   {
   	this.galleryId = galleryId;
   }
   public String getGalleryPath()
   {
   	return galleryPath;
   }
   public void setGalleryPath(String galleryPath)
   {
   	this.galleryPath = galleryPath;
   }
   
   public String getGalleryName()
   {
   	return galleryName;
   }
   public void setGalleryName(String galleryName)
   {
   	this.galleryName = galleryName;
   }
   
   public String getGalleryPerex()
   {
   	return galleryPerex;
   }
   public void setGalleryPerex(String galleryPerex)
   {
   	this.galleryPerex = galleryPerex;
   }

	public Date getGalleryDate()
	{
		return galleryDate == null ? null : (Date) galleryDate.clone();
	}

	public void setGalleryDate(Date galleryDate)
	{
		this.galleryDate = galleryDate == null ? null : (Date) galleryDate.clone();
	}

	public int getGalleryViews()
	{
		return galleryViews;
	}

	public void setGalleryViews(int galleryViews)
	{
		this.galleryViews = galleryViews;
	}
	
 	public IwcmFile getWatermark()
	{
 		if (isEmpty(watermarkSetup.getWatermark()))
 			return null; 
		return IwcmFile.fromVirtualPath(watermarkSetup.getWatermark());
	}

	public String getWatermarkPlacement()
	{
		return watermarkSetup.getWatermarkPlacement();
	}

	public int getWatermarkSaturation()
	{
		return watermarkSetup.getWatermarkSaturation();
	}

	public void setWatermark(String watermark)
	{
		watermarkSetup.setWatermark(watermark);
	}

	public void setWatermarkPlacement(String watermarkPlacement)
	{
		watermarkSetup.setWatermarkPlacement(watermarkPlacement);
	}

	public void setWatermarkSaturation(int watermarkSaturation)
	{
		watermarkSetup.setWatermarkSaturation(watermarkSaturation);
	}
	
	@Override
	public String toString()
	{
		return "GalleryDimension [galleryId=" + galleryId + ", galleryPath=" + galleryPath + ", galleryName=" + galleryName
					+ ", galleryPerex=" + galleryPerex + ", galleryDate=" + galleryDate + ", galleryViews=" + galleryViews
					+ ", author=" + author + ", watermarkSetup=" + watermarkSetup + "]";
	}
}