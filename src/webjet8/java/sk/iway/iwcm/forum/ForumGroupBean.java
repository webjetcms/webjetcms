package sk.iway.iwcm.forum;

import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.users.UserDetails;

/**
 *  ForumGroupBean.java - data z tab. forum
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: jeeff $
 *@version      $Revision: 1.8 $
 *@created      Date: 13.12.2004 13:42:34
 *@modified     $Date: 2008/02/06 09:59:51 $
 */
public class ForumGroupBean extends ActionForm
{
	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;
	private int id;
	private int docId;
	private boolean active;
	private String dateFrom;
	private String dateFromTime;
	private String dateTo;
	private String dateToTime;
	private int hoursAfterLastMessage;
	private boolean messageConfirmation;
	private String approveEmail;
	private String notifEmail;
	private boolean messageBoard;
	private boolean advertisementType;
	private String adminGroups;
	private String addmessageGroups;
	
	
	
	/* (non-Javadoc)
	 * @see org.apache.struts.action.ActionForm#reset(org.apache.struts.action.ActionMapping, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public void reset(ActionMapping arg0, HttpServletRequest arg1)
	{
		active = false;
		messageConfirmation = false;
		messageBoard = false;
		advertisementType = false;
	}
	
	/**
	 * Vrati true, ak zadany pouzivatel patri medzi administratorov fora
	 * @param user
	 * @return
	 */
	public boolean isAdmin(UserDetails user)
	{
		if (user == null) return(false);
		String ids = getAdminGroups();
		if(Tools.isNotEmpty(ids)) 
		{
			StringTokenizer st = new StringTokenizer(ids,",+");
			while(st.hasMoreTokens())
			{
				int userGroupId = Tools.getIntValue(st.nextToken(), -1);
				if (userGroupId < 1) continue;
				if(user.isInUserGroup(userGroupId)) return true;
			}
		}
		return false;
	}
	
	/**
	 * Otestuje ci moze pouzivatel pridat prispevok do fora
	 * @param user
	 * @return
	 */
	public boolean canPostMessage(UserDetails user)
	{
		if (Tools.isEmpty(getAddmessageGroups())) return true;
		if (user == null) return(false);
		
		String ids = getAddmessageGroups();
		if(Tools.isNotEmpty(ids)) 
		{
			StringTokenizer st = new StringTokenizer(ids,",+");
			while(st.hasMoreTokens())
			{
				int userGroupId = Tools.getIntValue(st.nextToken(), -1);
				if (userGroupId < 1) continue;
				if(user.isInUserGroup(userGroupId)) return true;
			}
		}
		return false;
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
	 * @return Returns the hoursAfterLastMessage.
	 */
	public int getHoursAfterLastMessage()
	{
		return hoursAfterLastMessage;
	}
	/**
	 * @param hoursAfterLastMessage The hoursAfterLastMessage to set.
	 */
	public void setHoursAfterLastMessage(int hoursAfterLastMessage)
	{
		this.hoursAfterLastMessage = hoursAfterLastMessage;
	}
	/**
	 * @return Returns the id.
	 */
	public int getId()
	{
		return id;
	}
	/**
	 * @param id The id to set.
	 */
	public void setId(int id)
	{
		this.id = id;
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
	 * @return Returns the messageConfirmation.
	 */
	public boolean isMessageConfirmation()
	{
		return messageConfirmation;
	}
	/**
	 * @param messageConfirmation The messageConfirmation to set.
	 */
	public void setMessageConfirmation(boolean messageConfirmation)
	{
		this.messageConfirmation = messageConfirmation;
	}
	/**
	 * @return Returns the approveEmail.
	 */
	public String getApproveEmail()
	{
		return approveEmail;
	}
	/**
	 * @param approveEmail The approveEmail to set.
	 */
	public void setApproveEmail(String approveEmail)
	{
		this.approveEmail = approveEmail;
	}
	/**
	 * @return Returns the notifEmail.
	 */
	public String getNotifEmail()
	{
		return notifEmail;
	}
	/**
	 * @param notifEmail The notifEmail to set.
	 */
	public void setNotifEmail(String notifEmail)
	{
		this.notifEmail = notifEmail;
	}
	/**
	 * @return Returns the messageBoard.
	 */
	public boolean isMessageBoard()
	{
		return messageBoard;
	}
	/**
	 * @param messageBoard The messageBoard to set.
	 */
	public void setMessageBoard(boolean messageBoard)
	{
		this.messageBoard = messageBoard;
	}
	/**
	 * @return Returns the advertisementType.
	 */
	public boolean isAdvertisementType()
	{
		return advertisementType;
	}
	/**
	 * @param advertisementType The advertisementType to set.
	 */
	public void setAdvertisementType(boolean advertisementType)
	{
		this.advertisementType = advertisementType;
	}
	/**
	 * @return Returns the adminGroups.
	 */
	public String getAdminGroups()
	{
		return adminGroups;
	}
	/**
	 * @param adminGroups The adminGroups to set.
	 */
	public void setAdminGroups(String adminGroups)
	{
		this.adminGroups = adminGroups;
	}

	public String getAddmessageGroups()
	{
		return addmessageGroups;
	}

	public void setAddmessageGroups(String addmessageGroups)
	{
		this.addmessageGroups = addmessageGroups;
	}

}
