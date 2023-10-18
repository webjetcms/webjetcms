package sk.iway.iwcm.forum;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.gallery.GalleryDB;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

/**
 *  Bean pre forum
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      Sobota, 2003, november 8
 *@modified     $Date: 2003/11/11 15:25:21 $
 */
public class ForumBean
{
   private int forumId;
   private int docId;
   private int parentId;
   //private int clientId;
   private String subject;
   private String question;
   private java.sql.Timestamp questionDate;
   private String questionDateDisplayDate;
   private String questionDateDisplayTime;
   //private int userId;
   private String autorFullName;
   private String autorEmail;
   private String prefix;
   private int level = 0;
   private boolean confirmed;
   private String hashCode;
   private boolean sendNotif;
   private int userId;
	private String flag;
	private int statViews;
	private int statReplies;
	private String lastPost;
	private boolean active;
	private boolean deleted = false;

	private UserDetails author;


	public boolean canDelete(UserDetails user, int delMinutes)
	{
		if (getQuestionDate() == null || user == null || user.getUserId()!=userId) return(false);

		long delTimeLimitMsec = getQuestionDate().getTime() + (delMinutes*60L*1000);
		if(delTimeLimitMsec > Tools.getNow()) return(true);
		return(false);
	}

   /**
    *  Gets the forumId attribute of the ForumBean object
    *
    *@return    The forumId value
    */
   public int getForumId()
   {
      return forumId;
   }

   /**
    *  Sets the forumId attribute of the ForumBean object
    *
    *@param  forumId  The new forumId value
    */
   public void setForumId(int forumId)
   {
      this.forumId = forumId;
   }

   /**
    *  Sets the docId attribute of the ForumBean object
    *
    *@param  docId  The new docId value
    */
   public void setDocId(int docId)
   {
      this.docId = docId;
   }

   /**
    *  Gets the docId attribute of the ForumBean object
    *
    *@return    The docId value
    */
   public int getDocId()
   {
      return docId;
   }

   /**
    *  Sets the parentId attribute of the ForumBean object
    *
    *@param  parentId  The new parentId value
    */
   public void setParentId(int parentId)
   {
      this.parentId = parentId;
   }

   /**
    *  Gets the parentId attribute of the ForumBean object
    *
    *@return    The parentId value
    */
   public int getParentId()
   {
      return parentId;
   }

   /**
    *  Sets the subject attribute of the ForumBean object
    *
    *@param  subject  The new subject value
    */
   public void setSubject(String subject)
   {
      this.subject = subject;
   }

   /**
    *  Gets the subject attribute of the ForumBean object
    *
    *@return    The subject value
    */
   public String getSubject()
   {
      if (subject == null || subject.trim().length() < 1)
      {
         return ("Bez nadpisu");
      }
      return subject;
   }

   /**
    *  Sets the question attribute of the ForumBean object
    *
    *@param  question  The new question value
    */
   public void setQuestion(String question)
   {
   	//odfiltruj nepovolene veci
   	question = Tools.replaceIgnoreCase(question, "script", "<span style='display: none;'>s</span>script");

      this.question = question;
   }

   /**
    *  Gets the question attribute of the ForumBean object
    *
    *@return    The question value
    */
   public String getQuestion()
   {
      return question;
   }

   /**
    *  Gets the questionHtml attribute of the ForumBean object
    *
    *@return    The questionHtml value
    */
   public String getQuestionHtml()
   {
      if (question == null)
      {
         return ("");
      }
      String ret = Tools.replace(question, "<", "&lt;");
      ret = Tools.replace(ret, "\n", "<br>");
      return (ret);
   }


   /**
    *  Sets the questionDate attribute of the ForumBean object
    *
    *@param  questionDate  The new questionDate value
    */
   public void setQuestionDate(java.sql.Timestamp questionDate)
   {
      this.questionDate = questionDate;
   }

   /**
    *  Gets the questionDate attribute of the ForumBean object
    *
    *@return    The questionDate value
    */
   public java.sql.Timestamp getQuestionDate()
   {
      return questionDate;
   }

   /**
    *  Sets the questionDateDisplayDate attribute of the ForumBean object
    *
    *@param  questionDateDisplayDate  The new questionDateDisplayDate value
    */
   public void setQuestionDateDisplayDate(String questionDateDisplayDate)
   {
      this.questionDateDisplayDate = questionDateDisplayDate;
   }

   /**
    *  Gets the questionDateDisplayDate attribute of the ForumBean object
    *
    *@return    The questionDateDisplayDate value
    */
   public String getQuestionDateDisplayDate()
   {
      return questionDateDisplayDate;
   }

   /**
    *  Sets the questionDateDisplayTime attribute of the ForumBean object
    *
    *@param  questionDateDisplayTime  The new questionDateDisplayTime value
    */
   public void setQuestionDateDisplayTime(String questionDateDisplayTime)
   {
      this.questionDateDisplayTime = questionDateDisplayTime;
   }

   /**
    *  Gets the questionDateDisplayTime attribute of the ForumBean object
    *
    *@return    The questionDateDisplayTime value
    */
   public String getQuestionDateDisplayTime()
   {
      return questionDateDisplayTime;
   }

   /**
    *  Sets the autorFullName attribute of the ForumBean object
    *
    *@param  autorFullName  The new autorFullName value
    */
   public void setAutorFullName(String autorFullName)
   {
      this.autorFullName = autorFullName;
   }

   /**
    *  Gets the autorFullName attribute of the ForumBean object
    *
    *@return    The autorFullName value
    */
   public String getAutorFullName()
   {
      return autorFullName;
   }

   /**
    *  Sets the autorEmail attribute of the ForumBean object
    *
    *@param  autorEmail  The new autorEmail value
    */
   public void setAutorEmail(String autorEmail)
   {
      this.autorEmail = autorEmail;
   }

   /**
    *  Gets the autorEmail attribute of the ForumBean object
    *
    *@return    The autorEmail value
    */
   public String getAutorEmail()
   {
      return autorEmail;
   }

   /**
    *  Sets the prefix attribute of the ForumBean object
    *
    *@param  prefix  The new prefix value
    */
   public void setPrefix(String prefix)
   {
      this.prefix = prefix;
   }

   /**
    *  Gets the prefix attribute of the ForumBean object
    *
    *@return    The prefix value
    */
   public String getPrefix()
   {
      return prefix;
   }
   public int getLevel()
   {
      return level;
   }
   public void setLevel(int level)
   {
      this.level = level;
   }
	/**
	 * @return Returns the confirmed.
	 */
	public boolean isConfirmed()
	{
		return confirmed;
	}
	/**
	 * @param confirmed The confirmed to set.
	 */
	public void setConfirmed(boolean confirmed)
	{
		this.confirmed = confirmed;
	}
	/**
	 * @return Returns the hashCode.
	 */
	public String getHashCode()
	{
		return hashCode;
	}
	/**
	 * @param hashCode The hashCode to set.
	 */
	public void setHashCode(String hashCode)
	{
		this.hashCode = hashCode;
	}
	/**
	 * @return Returns the sendNotif.
	 */
	public boolean isSendNotif()
	{
		return sendNotif;
	}
	/**
	 * @param sendNotif The sendNotif to set.
	 */
	public void setSendNotif(boolean sendNotif)
	{
		this.sendNotif = sendNotif;
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
	 * @return Returns the lastPost.
	 */
	public String getLastPost()
	{
		return lastPost;
	}
	/**
	 * @param lastPost The lastPost to set.
	 */
	public void setLastPost(String lastPost)
	{
		this.lastPost = lastPost;
	}
	/**
	 * @return Returns the statReplies.
	 */
	public int getStatReplies()
	{
		return statReplies;
	}
	/**
	 * @param statReplies The statReplies to set.
	 */
	public void setStatReplies(int statReplies)
	{
		this.statReplies = statReplies;
	}
	/**
	 * @return Returns the statViews.
	 */
	public int getStatViews()
	{
		return statViews;
	}
	/**
	 * @param statViews The statViews to set.
	 */
	public void setStatViews(int statViews)
	{
		this.statViews = statViews;
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

	public boolean isDeleted()
	{
		return deleted;
	}

	public void setDeleted(boolean deleted)
	{
		this.deleted = deleted;
	}


	public UserDetails getAuthor()
    {
        if (author == null)
        {
            author = UsersDB.getUser(getUserId());
            if (author == null) author = new UserDetails();
        }

        return author;
    }

    /**
     * Vrati fotku pouzivatela, alebo defaultPhoto ak nema ziadnu zadanu
     * @param defaultPhoto
     * @return
     */
    public String getAuthorPhoto(String defaultPhoto)
    {
        String authorPhoto = getAuthor().getPhoto();

        if(Tools.isEmpty(authorPhoto)) return defaultPhoto;
        return GalleryDB.getImagePathOriginal(authorPhoto);
    }
}
