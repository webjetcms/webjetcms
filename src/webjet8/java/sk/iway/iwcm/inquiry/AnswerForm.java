package sk.iway.iwcm.inquiry;

import sk.iway.iwcm.DB;
import sk.iway.iwcm.Tools;

/**
 *  AnswerForm.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: jeeff jeeff $
 *@version      $Revision: 1.2 $
 *@created      Date: 22.11.2004 20:41:19
 *@modified     $Date: 2004/08/09 08:42:03 $
 */
public class AnswerForm
{
   private String answerString;
   private String questionString;
   private int answerID;
   private int questionID;
   private int answerClicks=0;
   private double percentage=0;
   //pocet obrazkov, ktore sa generuju ako stlpcek v JSP
   private int images=0;
   private String percentageString;
   private int hours = 24;
   private String group = "default";
   private String imgRootDir = "/images/inquiry/";
   private String answerTextOk;
   private String answerTextFail;
   private String dateFrom;
   private String dateFromTime;
   private String dateTo;
   private String dateToTime;
   private boolean active = true;
   private boolean multiple;
   private int totalClicks;
   private String imagePath;
   private String url;

   public String getUrl() {
   	return url;
   }
   public void setUrl(String url) {
   	this.url = url;
   }
   public String getImagePath()
	{
		return imagePath;
	}
	public void setImagePath(String imagePath)
	{
		this.imagePath = imagePath;
	}
	public int getTotalClicks()
	{
		return totalClicks;
	}
	public void setTotalClicks(int totalClicks)
	{
		this.totalClicks = totalClicks;
	}
	private int[] selectedAnswers = {};

   public int[] getSelectedAnswers()
	{
		return selectedAnswers;
	}
	public void setSelectedAnswers(int[] selectedAnswers)
	{
		this.selectedAnswers = selectedAnswers;
	}

   public boolean isMultiple()
	{
		return multiple;
	}

	public void setMultiple(boolean multiple)
	{
		this.multiple = multiple;
	}

   /**
    * Vrati true, ak su platne datumy pre zobrazenie ankety
    * @return
    */
   public boolean isDateValid()
   {
   	long now = Tools.getNow();
   	long start = 0;
   	long end = Long.MAX_VALUE;

   	if (Tools.isNotEmpty(getDateFrom()))
   	{
   		start = DB.getTimestamp(getDateFrom(), getDateFromTime());
   	}
   	if (Tools.isNotEmpty(getDateTo()))
   	{
   		end = DB.getTimestamp(getDateTo(), getDateToTime());
   	}

   	if (start < now && now < end)
   	{
   		return(true);
   	}
   	return(false);
   }

   public void setAnswerString(String newAnswerString)
   {
      answerString = newAnswerString;
   }

   public String getAnswerString()
   {
      return answerString;
   }

   public void setAnswerID(int newAnswerID)
   {
      answerID = newAnswerID;
   }

   public int getAnswerID()
   {
      return answerID;
   }
   public void setAnswerClicks(int answerClicks)
   {
      this.answerClicks = answerClicks;
   }

   public int getAnswerClicks()
   {
      return answerClicks;
   }

   public void setQuestionID(int newQuestionID)
   {
      questionID = newQuestionID;
   }

   public int getQuestionID()
   {
      return questionID;
   }

   public void setQuestionString(String newQuestionString)
   {
      questionString = newQuestionString;
   }

   public String getQuestionString()
   {
      return questionString;
   }

   public void setPercentage(double newPercentage)
   {
      percentage = newPercentage;
   }

   public double getPercentage()
   {
      return percentage;
   }

   public int getImages()
   {
      return images;
   }
   public void setImages(int images)
   {
      this.images = images;
   }
   public String getImagesBar()
   {
      StringBuilder out= new StringBuilder();
		int i;
      //obrazky su indexovane od 1
      for (i=1; i<=images; i++)
      {
         out.append("<img src='").append(imgRootDir).append(i).append(".gif' style='border:0px' alt='' />");
      }
   	return(out.toString());
   }
   public String getPercentageString()
   {
      return percentageString;
   }
   public void setPercentageString(String percentageString)
   {
      this.percentageString = percentageString;
   }
   public void setHours(int hours)
   {
      this.hours = hours;
   }
   public int getHours()
   {
      return hours;
   }
   public void setGroup(String group)
   {
      this.group = group;
   }
   public String getGroup()
   {
      return group;
   }
   public String getImgRootDir()
   {
      return imgRootDir;
   }
   public void setImgRootDir(String imgRootDir)
   {
      this.imgRootDir = imgRootDir;
   }
   public void setAnswerTextOk(String answerTextOk)
   {
      this.answerTextOk = answerTextOk;
   }
   public String getAnswerTextOk()
   {
      return answerTextOk;
   }
   public void setAnswerTextFail(String answerTextFail)
   {
      this.answerTextFail = answerTextFail;
   }
   public String getAnswerTextFail()
   {
      return answerTextFail;
   }


	/**
	 * @return Returns the dateFrom.
	 */
	public String getDateFrom()
	{
		return dateFrom;
	}
	/**
	 * @param dateFrom The dateFrom to set.
	 */
	public void setDateFrom(String dateFrom)
	{
		this.dateFrom = dateFrom;
	}
	/**
	 * @return Returns the dateFromTime.
	 */
	public String getDateFromTime()
	{
		return dateFromTime;
	}
	/**
	 * @param dateFromTime The dateFromTime to set.
	 */
	public void setDateFromTime(String dateFromTime)
	{
		this.dateFromTime = dateFromTime;
	}
	/**
	 * @return Returns the dateTo.
	 */
	public String getDateTo()
	{
		return dateTo;
	}
	/**
	 * @param dateTo The dateTo to set.
	 */
	public void setDateTo(String dateTo)
	{
		this.dateTo = dateTo;
	}
	/**
	 * @return Returns the dateToTime.
	 */
	public String getDateToTime()
	{
		return dateToTime;
	}
	/**
	 * @param dateToTime The dateToTime to set.
	 */
	public void setDateToTime(String dateToTime)
	{
		this.dateToTime = dateToTime;
	}

	/**
	 * @return Returns the active.
	 */
	public boolean isActive()
	{
		return active;
	}
	/**
	 * @param active The active to set.
	 */
	public void setActive(boolean active)
	{
		this.active = active;
	}
}