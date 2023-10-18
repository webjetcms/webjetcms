package sk.iway.iwcm.forum;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.upload.FormFile;


/**
 *  Formular pre ulozenie prispevku do fora
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      Sobota, 2003, november 8
 *@modified     $Date: 2003/12/10 22:13:51 $
 */
public class ForumForm extends ActionForm
{
	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;
	private int parentId;
	private String subject;
	private String question;
	private int docId;
	private String authorFullName;
	private String authorEmail;
	private int forumId = -1;
	private boolean confirmed;	
	private boolean sendNotif;
	private int userId;
	private String flag;
	private int statViews;
	private int statReplies;
	private String lastPost;
	private boolean useLoggedUser = true;
	
	private FormFile uploadedFile;
	
	/* (non-Javadoc)
	 * @see org.apache.struts.action.ActionForm#reset(org.apache.struts.action.ActionMapping, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public void reset(ActionMapping arg0, HttpServletRequest arg1)
	{
		confirmed = false;
		sendNotif = false;		
	}
	/**
	 *  Gets the parentId attribute of the ForumForm object
	 *
	 *@return    The parentId value
	 */
	public int getParentId()
	{
		return parentId;
	}

	/**
	 *  Sets the parentId attribute of the ForumForm object
	 *
	 *@param  parentId  The new parentId value
	 */
	public void setParentId(int parentId)
	{
		this.parentId = parentId;
	}

	/**
	 *  Sets the subject attribute of the ForumForm object
	 *
	 *@param  subject  The new subject value
	 */
	public void setSubject(String subject)
	{
		this.subject = subject;
	}

	/**
	 *  Gets the subject attribute of the ForumForm object
	 *
	 *@return    The subject value
	 */
	public String getSubject()
	{
		return subject;
	}

	/**
	 *  Sets the question attribute of the ForumForm object
	 *
	 *@param  question  The new question value
	 */
	public void setQuestion(String question)
	{
		this.question = question;
	}

	/**
	 *  Gets the question attribute of the ForumForm object
	 *
	 *@return    The question value
	 */
	public String getQuestion()
	{
		return question;
	}

	/**
	 *  Sets the docId attribute of the ForumForm object
	 *
	 *@param  docId  The new docId value
	 */
	public void setDocId(int docId)
	{
		this.docId = docId;
	}

	/**
	 *  Gets the docId attribute of the ForumForm object
	 *
	 *@return    The docId value
	 */
	public int getDocId()
	{
		return docId;
	}
	public void setAuthorFullName(String authorFullName)
	{
		this.authorFullName = authorFullName;
	}
	public String getAuthorFullName()
	{
		return authorFullName;
	}
	public void setAuthorEmail(String authorEmail)
	{
		this.authorEmail = authorEmail;
	}
	public String getAuthorEmail()
	{
		return authorEmail;
	}
	public int getForumId()
	{
		return forumId;
	}
	public void setForumId(int forumId)
	{
		this.forumId = forumId;
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
	public boolean isUseLoggedUser() {
		return useLoggedUser;
	}
	public void setUseLoggedUser(boolean useLoggedUser) {
		this.useLoggedUser = useLoggedUser;
	}
	public FormFile getUploadedFile()
	{
		return uploadedFile;
	}
	public void setUploadedFile(FormFile uploadedFile)
	{
		this.uploadedFile = uploadedFile;
	}
}
