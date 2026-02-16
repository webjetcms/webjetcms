package sk.iway.iwcm.forum;


/**
 *  Bean pre forum vyhladavanie
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: joruz $
 *@version      $Revision: 1.1 $
 *@created      Date: 07.04.2005 15:47:50
 *@modified     $$
 */
public class ForumSearchBean
{
   private int forumId;
   private String forumName;
   private int docId;
   private int parentId;
   private String subject;
   private String question;
   private String autorFullName;
   private String autorEmail;
   private int userId;
	private String flag;
	private String questionDate;
	
  

	/**
	 * @return Returns the questionDate.
	 */
	public String getQuestionDate()
	{
		return questionDate;
	}
	/**
	 * @param questionDate The questionDate to set.
	 */
	public void setQuestionDate(String questionDate)
	{
		this.questionDate = questionDate;
	}
	/**
	 * @return Returns the autorEmail.
	 */
	public String getAutorEmail()
	{
		return autorEmail;
	}
	/**
	 * @param autorEmail The autorEmail to set.
	 */
	public void setAutorEmail(String autorEmail)
	{
		this.autorEmail = autorEmail;
	}
	/**
	 * @return Returns the autorFullName.
	 */
	public String getAutorFullName()
	{
		return autorFullName;
	}
	/**
	 * @param autorFullName The autorFullName to set.
	 */
	public void setAutorFullName(String autorFullName)
	{
		this.autorFullName = autorFullName;
	}
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
	 * @return Returns the flag.
	 */
	public String getFlag()
	{
		return flag;
	}
	/**
	 * @param flag The flag to set.
	 */
	public void setFlag(String flag)
	{
		this.flag = flag;
	}
	/**
	 * @return Returns the forumId.
	 */
	public int getForumId()
	{
		return forumId;
	}
	/**
	 * @param forumId The forumId to set.
	 */
	public void setForumId(int forumId)
	{
		this.forumId = forumId;
	}
	/**
	 * @return Returns the forumName.
	 */
	public String getForumName()
	{
		return forumName;
	}
	/**
	 * @param forumName The forumName to set.
	 */
	public void setForumName(String forumName)
	{
		this.forumName = forumName;
	}
	/**
	 * @return Returns the parentId.
	 */
	public int getParentId()
	{
		return parentId;
	}
	/**
	 * @param parentId The parentId to set.
	 */
	public void setParentId(int parentId)
	{
		this.parentId = parentId;
	}
	
	/**
	 * @return Returns the question.
	 */
	public String getQuestion()
	{
		return question;
	}
	/**
	 * @param question The question to set.
	 */
	public void setQuestion(String question)
	{
		this.question = question;
	}
	/**
	 * @return Returns the subject.
	 */
	public String getSubject()
	{
		return subject;
	}
	/**
	 * @param subject The subject to set.
	 */
	public void setSubject(String subject)
	{
		this.subject = subject;
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
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((autorEmail == null) ? 0 : autorEmail.hashCode());
		result = prime * result + ((autorFullName == null) ? 0 : autorFullName.hashCode());
		result = prime * result + docId;
		result = prime * result + ((flag == null) ? 0 : flag.hashCode());
		result = prime * result + forumId;
		result = prime * result + ((forumName == null) ? 0 : forumName.hashCode());
		result = prime * result + parentId;
		result = prime * result + ((question == null) ? 0 : question.hashCode());
		result = prime * result + ((questionDate == null) ? 0 : questionDate.hashCode());
		result = prime * result + ((subject == null) ? 0 : subject.hashCode());
		result = prime * result + userId;
		return result;
	}
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ForumSearchBean other = (ForumSearchBean) obj;
		if (autorEmail == null)
		{
			if (other.autorEmail != null)
				return false;
		}
		else if (!autorEmail.equals(other.autorEmail))
			return false;
		if (autorFullName == null)
		{
			if (other.autorFullName != null)
				return false;
		}
		else if (!autorFullName.equals(other.autorFullName))
			return false;
		if (docId != other.docId)
			return false;
		if (flag == null)
		{
			if (other.flag != null)
				return false;
		}
		else if (!flag.equals(other.flag))
			return false;
		if (forumId != other.forumId)
			return false;
		if (forumName == null)
		{
			if (other.forumName != null)
				return false;
		}
		else if (!forumName.equals(other.forumName))
			return false;
		if (parentId != other.parentId)
			return false;
		if (question == null)
		{
			if (other.question != null)
				return false;
		}
		else if (!question.equals(other.question))
			return false;
		if (questionDate == null)
		{
			if (other.questionDate != null)
				return false;
		}
		else if (!questionDate.equals(other.questionDate))
			return false;
		if (subject == null)
		{
			if (other.subject != null)
				return false;
		}
		else if (!subject.equals(other.subject))
			return false;
		if (userId != other.userId)
			return false;
		return true;
	}	
}
