package sk.iway.iwcm.users;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionMapping;

import sk.iway.iwcm.Tools;

/**
 *  Informacie o skupine pouzivatelov
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      Štvrtok, 2002, august 15
 *@modified     $Date: 2004/01/09 18:40:31 $
 */
public class UserGroupDetails
{
	public static final int TYPE_PERMS = 0;
	public static final int TYPE_EMAIL = 1;

	private int userGroupId=-1;
	private String userGroupName;
	private int userGroupType = 0;
	private String userGroupComment;
	private boolean requireApprove = true;
	private int emailDocId = -1;
	private boolean allowUserEdit;
	private boolean requireEmailVerification = false;

	public void reset(ActionMapping mapping, HttpServletRequest request)
	{
		//Logger.println(this,"EditorForm: reset()");
		requireApprove = false;
		allowUserEdit = false;
	}

	public String getUserGroupNameComment()
	{
		if (Tools.isEmpty(userGroupComment)) return userGroupName;
		return userGroupName + " - " + userGroupComment;
	}

	/**
	 *  Gets the userGroupId attribute of the UserGroupDetails object
	 *
	 *@return    The userGroupId value
	 */
	public int getUserGroupId()
	{
		return userGroupId;
	}

	/**
	 *  Sets the userGroupId attribute of the UserGroupDetails object
	 *
	 *@param  userGroupId  The new userGroupId value
	 */
	public void setUserGroupId(int userGroupId)
	{
		this.userGroupId = userGroupId;
	}

	/**
	 *  Sets the userGroupName attribute of the UserGroupDetails object
	 *
	 *@param  userGroupName  The new userGroupName value
	 */
	public void setUserGroupName(String userGroupName)
	{
		this.userGroupName = userGroupName;
	}

	/**
	 *  Gets the userGroupName attribute of the UserGroupDetails object
	 *
	 *@return    The userGroupName value
	 */
	public String getUserGroupName()
	{
		return userGroupName;
	}
	public int getUserGroupType()
	{
		return userGroupType;
	}
	public void setUserGroupType(int userGroupType)
	{
		this.userGroupType = userGroupType;
	}
	public String getUserGroupComment()
	{
		return userGroupComment;
	}
	public void setUserGroupComment(String userGroupComment)
	{
		this.userGroupComment = userGroupComment;
	}

	/**
	 * @return Returns the emailDocId.
	 */
	public int getEmailDocId()
	{
		return emailDocId;
	}
	/**
	 * @param emailDocId The emailDocId to set.
	 */
	public void setEmailDocId(int emailDocId)
	{
		this.emailDocId = emailDocId;
	}
	/**
	 * @return Returns the requireApprove.
	 */
	public boolean isRequireApprove()
	{
		return requireApprove;
	}
	/**
	 * @param requireApprove The requireApprove to set.
	 */
	public void setRequireApprove(boolean requireApprove)
	{
		this.requireApprove = requireApprove;
	}
	/**
	 * @return Returns the allowUserEdit.
	 */
	public boolean isAllowUserEdit()
	{
		return allowUserEdit;
	}
	/**
	 * @param allowUserEdit The allowUserEdit to set.
	 */
	public void setAllowUserEdit(boolean allowUserEdit)
	{
		this.allowUserEdit = allowUserEdit;
	}

	public boolean isRequireEmailVerification()
	{
		return requireEmailVerification;
	}

	public void setRequireEmailVerification(boolean requireEmailVerification)
	{
		this.requireEmailVerification = requireEmailVerification;
	}
}
