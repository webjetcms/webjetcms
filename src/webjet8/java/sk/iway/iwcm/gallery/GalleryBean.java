package sk.iway.iwcm.gallery;

import java.util.Arrays;
import java.util.Date;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.io.IwcmFile;

/**
 * GalleryBean.java - informacie o obrazku z DB
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: jeeff jeeff $
 *@version      $Revision: 1.2 $
 *@created      Date: 16.9.2004 23:19:42
 *@modified     $Date: 2004/08/09 08:42:03 $
 */
public class GalleryBean
{
   public GalleryBean()
   {
      //prazdny konstruktor
   }

   private String imagePath="";
   private String imageName="";
   private String shortDescription="";
   private String longDescription="";
   private int imageId=-1;
   private String bigDimension="";
   private String bigLength="";
   private String nextImage="";
   private String prevImage="";
   private String originalImage="";
   private String author="";
   private int sendCount=0;
   private String allowedDomains = "";
	private String[] perexGroup;
	private int selectedX =-1;
	private int selectedY = -1;
	private int selectedWidth =-1;
	private int selectedHeight =-1;
	private Date uploadDate;
	private int sortPriority;
	private boolean cyclicViewing = false;

	private boolean prevNextLinksInitalized = false;
	private void initPrevNextLinks()
	{
		if (prevNextLinksInitalized) return;
		GalleryDB.fillPrevNextLink(this, cyclicViewing);
		prevNextLinksInitalized = true;
	}


	/**
	 * Vrati URL k obrazku ako imagePath+/+imageName
	 * @return
	 */
	public String getImageUrl()
	{
		return getImagePath()+"/"+getImageName();
	}

   public String getImagePath()
   {
      return imagePath;
   }
   public void setImagePath(String imagePath)
   {
      this.imagePath = imagePath;
   }
   public void setImageName(String imageName)
   {
      this.imageName = imageName;
   }
   public String getImageName()
   {
      return imageName;
   }
   public void setShortDescription(String shortDescription)
   {
      this.shortDescription = shortDescription;
   }
   public String getShortDescription()
   {
      return shortDescription;
   }

   public String getDescriptionByLng(String lng, String item)
   {
      return GalleryDB.getDescriptionLng(this.imageId, item, lng);
   }

   public String getAuthor()
	{
		return (this.author == null ? "" : this.author);
	}
	public void setAuthor(String author)
	{
		this.author = author;
	}
	/**
    * vrati short description, tak aby neobsahoval \n
    * @return
    */
   public String getShortDescriptionJS()
   {
      String ret = getShortDescription();

      if (ret==null) return("");

      if (ret.indexOf('\n')>0)
      {
         if (ret.indexOf("<br")!=-1 || ret.indexOf("<p")!=-1 || ret.indexOf("<td")!=-1)
         {
            //je to HTML, \n mozeme nahradit medzerou
            ret = Tools.replace(ret, "\n", " ");
         }
         else
         {
            //nie je to HTML, \n nahradime za <br>
            ret = Tools.replace(ret, "\n", "<br />");
         }
      }

      ret = Tools.replace(ret, "\r", "");
      //sepal z toho uvodzovky
      ret = Tools.replace(ret, "\"", "&quot;");

      return(ret);
   }


   public void setLongDescription(String longDescription)
   {
      this.longDescription = longDescription;
   }
   public String getLongDescription()
   {
      return longDescription;
   }

   public String getLongDescriptionJS()
   {
      String ret = getLongDescription();

      if (ret==null) return("");

      if (ret.indexOf('\n')>0)
      {
         if (ret.indexOf("<br")!=-1 || ret.indexOf("<p")!=-1 || ret.indexOf("<td")!=-1)
         {
            //je to HTML, \n mozeme nahradit medzerou
            ret = Tools.replace(ret, "\n", " ");
         }
         else
         {
            //nie je to HTML, \n nahradime za <br>
            ret = Tools.replace(ret, "\n", "<br />");
         }
      }

      ret = Tools.replace(ret, "\r", "");
      //sepal z toho uvodzovky
      ret = Tools.replace(ret, "\"", "&quot;");

      return(ret);
   }


   public void setImageId(int imageId)
   {
      this.imageId = imageId;
   }
   public int getImageId()
   {
      return imageId;
   }
   public String getBigDimension()
   {
      return bigDimension;
   }
   public void setBigDimension(String bigDimension)
   {
      this.bigDimension = bigDimension;
   }
   public String getBigLength()
   {
      return bigLength;
   }
   public void setBigLength(String bigLength)
   {
      this.bigLength = bigLength;
   }
   public String getNextImage()
   {
   	initPrevNextLinks();
      return nextImage;
   }
   public void setNextImage(String nextImage)
   {
      this.nextImage = nextImage;
   }
   public String getPrevImage()
   {
   	initPrevNextLinks();
      return prevImage;
   }
   public void setPrevImage(String prevImage)
   {
      this.prevImage = prevImage;
   }
   public void setOriginalImage(String originalImage)
   {
      this.originalImage = originalImage;
   }
   public String getOriginalImage()
   {
   	if (originalImage==null && Constants.getBoolean("galleryUseFastLoading"))
   	{
   		originalImage = GalleryDB.getImagePathOriginal(imagePath + "/" + imageName);
   	}

      return originalImage;
   }
	public int getSendCount()
	{
		return sendCount;
	}
	public void setSendCount(int sendCount)
	{
		this.sendCount = sendCount;
	}
	public String getAllowedDomains()
	{
		return allowedDomains;
	}
	public void setAllowedDomains(String allowedDomains)
	{
		this.allowedDomains = allowedDomains;
	}
	public String[] getPerexGroup()
	{
		return perexGroup;
	}
	public void setPerexGroup(String[] perexGroup)
	{
		this.perexGroup = perexGroup;
	}
	public int getSelectedX()
	{
		return selectedX;
	}
	public void setSelectedX(int selectedX)
	{
		this.selectedX = selectedX;
	}
	public int getSelectedY()
	{
		return selectedY;
	}
	public void setSelectedY(int selectedY)
	{
		this.selectedY = selectedY;
	}
	public int getSelectedWidth()
	{
		return selectedWidth;
	}
	public void setSelectedWidth(int selectedWidth)
	{
		this.selectedWidth = selectedWidth;
	}
	public int getSelectedHeight()
	{
		return selectedHeight;
	}
	public void setSelectedHeight(int selectedHeight)
	{
		this.selectedHeight = selectedHeight;
	}

	/**
	 * Returns bean property uploadDate, if it ist null, tries to find file corresponding
	 * to this bean, if found, returns date of last modification of this file, if not found
	 * returns null
	 * @return
	 */
	public Date getUploadDate()
	{
		if (uploadDate == null)
		{
			IwcmFile f = new IwcmFile(Tools.getRealPath(getOriginalImage()));
			if (f != null && f.isFile()) uploadDate = new Date(f.lastModified());
			else uploadDate = new Date();
		}
		return uploadDate == null ? null : (Date) uploadDate.clone();
	}

	public Date getUploadDateNull()
	{
		return this.uploadDate;
	}

	public void setUploadDate(Date uploadDate)
	{
		this.uploadDate = uploadDate == null ? null : (Date) uploadDate.clone();
	}

	public void setSortPriority(int sortPriority)
	{
		this.sortPriority = sortPriority;
	}

	public int getSortPriority()
	{
		return sortPriority;
	}

	public boolean isCyclicViewing()
	{
		return cyclicViewing;
	}

	public void setCyclicViewing(boolean cyclicViewing)
	{
		this.cyclicViewing = cyclicViewing;
	}

	@Override
	public String toString()
	{
		return "GalleryBean [imagePath=" + imagePath + ", imageName=" + imageName + ", shortDescription=" + shortDescription
					+ ", longDescription=" + longDescription + ", imageId=" + imageId + ", bigDimension=" + bigDimension
					+ ", bigLength=" + bigLength + ", nextImage=" + nextImage + ", prevImage=" + prevImage + ", originalImage="
					+ originalImage + ", author=" + author + ", sendCount=" + sendCount + ", allowedDomains=" + allowedDomains
					+ ", perexGroup=" + Arrays.toString(perexGroup) + ", selectedX=" + selectedX + ", selectedY=" + selectedY
					+ ", selectedWidth=" + selectedWidth + ", selectedHeight=" + selectedHeight + ", uploadDate=" + uploadDate
					+ ", sortPriority=" + sortPriority + ", cyclicViewing=" + cyclicViewing
					+ ", prevNextLinksInitalized=" + prevNextLinksInitalized + "]";
	}

}