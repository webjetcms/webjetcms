package sk.iway.iwcm.doc;

import java.util.Date;

/**
 *  drzi zaznam z tabulky Groups_scheduler
 *
 *@Title        Interway Content Management
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: prau $
 *@version      $Revision: 1.9 $
 *@created      $Date: 2004/03/15 21:34:42 $
 *@modified     $Date: 2016/08/31 12:34:42 $
 */

public class GroupSchedulerDetails extends GroupDetails
{
	private int scheduleId;
	private Date saveDate;
	private int userId;
	private Date whenToPublish;
	
	
	public int getScheduleId() {
		return scheduleId;
	}
	public void setScheduleId(int scheduleId) {
		this.scheduleId = scheduleId;
	}
	public Date getSaveDate() {
		return saveDate;
	}
	public void setSaveDate(Date saveDate) {
		this.saveDate = saveDate;
	}
	public int getUserId() {
		return userId;
	}
	public void setUserId(int userId) {
		this.userId = userId;
	}
	public Date getWhenToPublish() {
		return whenToPublish;
	}
	public void setWhenToPublish(Date whenToPublish) {
		this.whenToPublish = whenToPublish;
	}
	
}
