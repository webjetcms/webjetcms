package sk.iway.iwcm.inquiry;

import java.util.Date;

import sk.iway.iwcm.Tools;



/**
 *  UserVoteBean.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: Miroslav Repaský $
 *@version      $Revision: 1.3 $
 *@created      Date: 16.2.2011 11:10:54
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class UserVoteBean
{
	private int userId;
	private int questionId;
   private int answerId;
   private Date createDate = null;
   private String ip = "";
  	
   public int getUserId()
   {
      return userId;
   }
   public void setUserId(int userId)
   {
      this.userId = userId;
   }
   public int getQuestionId()
   {
      return questionId;
   }
   public void setQuestionId(int questionId)
   {
      this.questionId = questionId;
   }
   public int getAnswerId()
   {
      return answerId;
   }
   public void setAnswerId(int answerId)
   {
      this.answerId = answerId;
   }
   public Date getCreateDate()
   {
      return createDate;
   }
   public void setCreateDate(Date createDate)
   {
      this.createDate = createDate;
   }
   
   public String getCreateDateString()
	{
		if (createDate != null)
		{
			return(Tools.formatDate(createDate));
		}
		return "";
	}

	public String getCreateTimeString()
	{
		if (createDate != null)
		{
			return(Tools.formatTime(createDate));
		}
		return("");
	}
	
	public String getIp()
   {
      return ip;
   }
   public void setIp(String ip)
   {
      this.ip = ip;
   }
	
}
