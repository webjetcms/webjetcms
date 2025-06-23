package sk.iway.iwcm.qa;

import sk.iway.iwcm.Tools;

/**
 *  Description of the Class
 *
 *@Title        magma-web
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.4 $
 *@created      Piatok, 2003, január 17
 *@modified     $Date: 2004/02/25 22:09:55 $
 */
public class QABean
{
	private int qaId;
	private String categoryName;
	private String questionDate;
	private long questionDateLong = 0;
	private String answerDate;
	private long answerDateLong = 0;
	private String question;
	private String answer;
	private String fromName;
	private String fromEmail;
	private String toName;
	private String toEmail;
	private boolean publishOnWeb = false;
	private boolean recode = true;
	private String hash = "";
	private String answerWeb;
	private String groupName = "";
	private boolean allowPublishOnWeb = false;
	private int sortPriority = 0;

	private String fromPhone;
	private String fromCompany;
	private String fieldA;
	private String fieldB;
	private String fieldC;
	private String fieldD;

	/**
	 *  Description of the Method
	 *
	 *@param  input  Description of the Parameter
	 *@return        Description of the Return Value
	 */
	private String recode(String input)
	{
		if (input == null)
		{
			return ("");
		}
		//Logger.println(this,"Recoding: "+input);
		return (input.trim());
	}

	/**
	 *  Gets the qaId attribute of the QABean object
	 *
	 *@return    The qaId value
	 */
	public int getQaId()
	{
		return qaId;
	}

	/**
	 *  Sets the qaId attribute of the QABean object
	 *
	 *@param  qaId  The new qaId value
	 */
	public void setQaId(int qaId)
	{
		this.qaId = qaId;
	}

	/**
	 *  Sets the rootId attribute of the QABean object
	 *
	 *@param  categoryName  The new categoryName value
	 */

	/**
	 *  Gets the rootId attribute of the QABean object
	 *
	 *@param  categoryName  The new categoryName value
	 */

	/**
	 *  Sets the categoryName attribute of the QABean object
	 *
	 *@param  categoryName  The new categoryName value
	 */
	public void setCategoryName(String categoryName)
	{
		this.categoryName = recode(categoryName);
	}

	/**
	 *  Gets the categoryName attribute of the QABean object
	 *
	 *@return    The categoryName value
	 */
	public String getCategoryName()
	{
		return categoryName;
	}

	/**
	 *  Sets the questionDate attribute of the QABean object
	 *
	 *@param  questionDate  The new questionDate value
	 */
	public void setQuestionDate(String questionDate)
	{
		if (questionDate != null)
		{
			//v DB nie je zadany cas, odstran
			questionDate = sk.iway.iwcm.Tools.replace(questionDate, " 0:00:00", "");
		}
		this.questionDate = questionDate;
	}

	/**
	 *  Gets the questionDate attribute of the QABean object
	 *
	 *@return    The questionDate value
	 */
	public String getQuestionDate()
	{
		return questionDate;
	}

	/**
	 *  Sets the answerDate attribute of the QABean object
	 *
	 *@param  answerDate  The new answerDate value
	 */
	public void setAnswerDate(String answerDate)
	{
		if (answerDate != null)
		{
			//v DB nie je zadany cas, odstran
			answerDate = sk.iway.iwcm.Tools.replace(answerDate, " 0:00:00", "");
		}
		this.answerDate = answerDate;
	}

	/**
	 *  Gets the answerDate attribute of the QABean object
	 *
	 *@return    The answerDate value
	 */
	public String getAnswerDate()
	{
		return answerDate;
	}

	/**
	 *  Sets the question attribute of the QABean object
	 *
	 *@param  question  The new question value
	 */
	public void setQuestion(String question)
	{
		this.question = recode(question);
	}

	/**
	 *  Gets the question attribute of the QABean object
	 *
	 *@return    The question value
	 */
	public String getQuestion()
	{
		return question;
	}

	/**
	 *  Sets the answer attribute of the QABean object
	 *
	 *@param  answer  The new answer value
	 */
	public void setAnswer(String answer)
	{
		this.answer = recode(answer);
	}

	/**
	 *  Gets the answer attribute of the QABean object
	 *
	 *@return    The answer value
	 */
	public String getAnswer()
	{
		return answer;
	}

	/**
	 *  Sets the fromName attribute of the QABean object
	 *
	 *@param  fromName  The new fromName value
	 */
	public void setFromName(String fromName)
	{
		this.fromName = recode(fromName);
	}

	/**
	 *  Gets the fromName attribute of the QABean object
	 *
	 *@return    The fromName value
	 */
	public String getFromName()
	{
		return fromName;
	}

	/**
	 *  Sets the fromEmail attribute of the QABean object
	 *
	 *@param  fromEmail  The new fromEmail value
	 */
	public void setFromEmail(String fromEmail)
	{
		this.fromEmail = recode(fromEmail);
	}

	/**
	 *  Gets the fromEmail attribute of the QABean object
	 *
	 *@return    The fromEmail value
	 */
	public String getFromEmail()
	{
		return fromEmail;
	}

	/**
	 *  Sets the toName attribute of the QABean object
	 *
	 *@param  toName  The new toName value
	 */
	public void setToName(String toName)
	{
		this.toName = recode(toName);
	}

	/**
	 *  Gets the toName attribute of the QABean object
	 *
	 *@return    The toName value
	 */
	public String getToName()
	{
		return toName;
	}

	/**
	 *  Sets the toEmail attribute of the QABean object
	 *
	 *@param  toEmail  The new toEmail value
	 */
	public void setToEmail(String toEmail)
	{
		this.toEmail = recode(toEmail);
	}

	/**
	 *  Gets the toEmail attribute of the QABean object
	 *
	 *@return    The toEmail value
	 */
	public String getToEmail()
	{
		return toEmail;
	}

	/**
	 *  Sets the publishOnWeb attribute of the QABean object
	 *
	 *@param  publishOnWeb  The new publishOnWeb value
	 */
	public void setPublishOnWeb(boolean publishOnWeb)
	{
		this.publishOnWeb = publishOnWeb;
	}

	/**
	 *  Gets the publishOnWeb attribute of the QABean object
	 *
	 *@return    The publishOnWeb value
	 */
	public boolean isPublishOnWeb()
	{
		if (allowPublishOnWeb == false)
		{
			publishOnWeb = false;
		}
		return publishOnWeb;
	}

	/**
	 *  Sets the recode attribute of the QABean object
	 *
	 *@param  recode  The new recode value
	 */
	public void setRecode(boolean recode)
	{
		this.recode = recode;
	}

	/**
	 *  Gets the recode attribute of the QABean object
	 *
	 *@return    The recode value
	 */
	public boolean isRecode()
	{
		return recode;
	}

	/**
	 *  Sets the hash attribute of the QABean object
	 *
	 *@param  hash  The new hash value
	 */
	public void setHash(String hash)
	{
		this.hash = hash;
	}

	/**
	 *  Gets the hash attribute of the QABean object
	 *
	 *@return    The hash value
	 */
	public String getHash()
	{
		return hash;
	}

	/**
	 *  Sets the answerWeb attribute of the QABean object
	 *
	 *@param  answerWeb  The new answerWeb value
	 */
	public void setAnswerWeb(String answerWeb)
	{
		this.answerWeb = recode(answerWeb);
	}

	/**
	 *  Gets the answerWeb attribute of the QABean object
	 *
	 *@return    The answerWeb value
	 */
	public String getAnswerWeb()
	{
		if (Tools.isEmpty(answerWeb))
		{
			return (answer);
		}
		return answerWeb;
	}

	/**
	 *  Sets the groupName attribute of the QABean object
	 *
	 *@param  groupName  The new groupName value
	 */
	public void setGroupName(String groupName)
	{
		this.groupName = groupName;
	}

	/**
	 *  Gets the groupName attribute of the QABean object
	 *
	 *@return    The groupName value
	 */
	public String getGroupName()
	{
		return groupName;
	}
	public boolean isAllowPublishOnWeb()
	{
		return allowPublishOnWeb;
	}
	public void setAllowPublishOnWeb(boolean allowPublishOnWeb)
	{
		this.allowPublishOnWeb = allowPublishOnWeb;
	}

	public long getAnswerDateLong()
	{
		return answerDateLong;
	}
	public String getAnswerDateDate()
	{
		if (answerDateLong==0) return("");
		return(Tools.formatDate(answerDateLong));
	}
	public String getAnswerDateTime()
	{
		if (answerDateLong==0) return("");
		return(Tools.formatTime(answerDateLong));
	}

	public void setAnswerDateLong(long answerDateLong)
	{
		this.answerDateLong = answerDateLong;
		if (answerDateLong!=0)
		{
			answerDate = Tools.formatDateTime(answerDateLong);
		}
	}

	public long getQuestionDateLong()
	{
		return questionDateLong;
	}

	public void setQuestionDateLong(long questionDateLong)
	{
		this.questionDateLong = questionDateLong;
		if (questionDateLong!=0)
		{
			questionDate = Tools.formatDateTime(questionDateLong);
		}
	}
	public String getQuestionDateDate()
	{
		if (questionDateLong==0) return("");
		return(Tools.formatDate(questionDateLong));
	}
	public String getQuestionDateTime()
	{
		if (questionDateLong==0) return("");
		return(Tools.formatTime(questionDateLong));
	}

	public void setSortPriority(int sortPriority)
	{
		this.sortPriority = sortPriority;
	}

	public int getSortPriority()
	{
		return sortPriority;
	}

	public String getFromPhone()
	{
		return fromPhone;
	}

	public void setFromPhone(String fromPhone)
	{
		this.fromPhone = fromPhone;
	}

	public String getFromCompany()
	{
		return fromCompany;
	}

	public void setFromCompany(String fromCompany)
	{
		this.fromCompany = fromCompany;
	}

	public String getFieldA()
	{
		return fieldA;
	}

	public void setFieldA(String fieldA)
	{
		this.fieldA = fieldA;
	}

	public String getFieldB()
	{
		return fieldB;
	}

	public void setFieldB(String fieldB)
	{
		this.fieldB = fieldB;
	}

	public String getFieldC()
	{
		return fieldC;
	}

	public void setFieldC(String fieldC)
	{
		this.fieldC = fieldC;
	}

	public String getFieldD()
	{
		return fieldD;
	}

	public void setFieldD(String fieldD)
	{
		this.fieldD = fieldD;
	}

}
