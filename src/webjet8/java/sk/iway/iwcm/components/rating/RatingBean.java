package sk.iway.iwcm.components.rating;

import org.apache.struts.action.ActionForm;

import sk.iway.iwcm.Tools;

/**
 *  RatingBean.java
 *
 *@Title        webjet
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: jeeff $
 *@version      $Revision: 1.7 $
 *@created      Date: 20.9.2004 20:47:07
 *@modified     $Date: 2008/07/14 12:37:06 $
 */
public class RatingBean extends ActionForm
{
	private static final long serialVersionUID = 1L;

	private int ratingId;
	private int docId;
	private int userId;
	private int ratingValue;
	private int totalSum;
	private double ratingValueDouble;
	private long insertDateLong;
	private String insertDate;
	private String insertTime;

	private int totalUsers;
	private int ratingStat;

	private String docTitle;


	/**
	 * @return Returns the docId.
	 */
	public int getDocId()
	{
		return docId;
	}
	/**
	 * @param docId The docId to set.
	 */
	public void setDocId(int docId)
	{
		this.docId = docId;
	}
	/**
	 * @return Returns the insertDate.
	 */
	public String getInsertDate()
	{
		return insertDate;
	}
	/**
	 * @param insertDate The insertDate to set.
	 */
	public void setInsertDate(String insertDate)
	{
		this.insertDate = insertDate;
	}
	/**
	 * @return Returns the ratingId.
	 */
	public int getRatingId()
	{
		return ratingId;
	}
	/**
	 * @param ratingId The ratingId to set.
	 */
	public void setRatingId(int ratingId)
	{
		this.ratingId = ratingId;
	}
	/**
	 * @return Returns the userId.
	 */
	public int getUserId()
	{
		return userId;
	}
	/**
	 * @param userId The userId to set.
	 */
	public void setUserId(int userId)
	{
		this.userId = userId;
	}
	/**
	 * @return Returns the insertTime.
	 */
	public String getInsertTime()
	{
		return insertTime;
	}
	/**
	 * @param insertTime The insertTime to set.
	 */
	public void setInsertTime(String insertTime)
	{
		this.insertTime = insertTime;
	}
	/**
	 * @return Returns the ratingValue.
	 */
	public int getRatingValue()
	{
		return ratingValue;
	}
	/**
	 * @param ratingValue The ratingValue to set.
	 */
	public void setRatingValue(int ratingValue)
	{
		this.ratingValue = ratingValue;
	}
	/**
	 * @return Returns the totalUsers.
	 */
	public int getTotalUsers()
	{
		return totalUsers;
	}
	/**
	 * @param totalUsers The totalUsers to set.
	 */
	public void setTotalUsers(int totalUsers)
	{
		this.totalUsers = totalUsers;
	}

	/**
	 * @return Returns the ratingStat.
	 */
	public int getRatingStat()
	{
		return ratingStat;
	}
	/**
	 * @param ratingStat The ratingStat to set.
	 */
	public void setRatingStat(int ratingStat)
	{
		this.ratingStat = ratingStat;
	}
	/**
	 * @return Returns the docTitle.
	 */
	public String getDocTitle()
	{
		return docTitle;
	}
	/**
	 * @param docTitle The docTitle to set.
	 */
	public void setDocTitle(String docTitle)
	{
		this.docTitle = docTitle;
	}
	public int getTotalSum()
	{
		return totalSum;
	}
	public void setTotalSum(int totalSum)
	{
		this.totalSum = totalSum;
	}
	public double getRatingValueDouble()
	{
		return ratingValueDouble;
	}
	public void setRatingValueDouble(double ratingValueDouble)
	{
		this.ratingValueDouble = ratingValueDouble;
		this.ratingValue = (int)Math.round(ratingValueDouble);
	}
	public long getInsertDateLong()
	{
		return insertDateLong;
	}
	public void setInsertDateLong(long insertDateLong)
	{
		this.insertDateLong = insertDateLong;
		this.insertDate = Tools.formatDate(insertDateLong);
		this.insertTime = Tools.formatTime(insertDateLong);
	}


}
