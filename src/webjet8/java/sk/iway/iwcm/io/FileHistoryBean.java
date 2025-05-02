package sk.iway.iwcm.io;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.ActiveRecord;
// import sk.iway.iwcm.ebanking.MoneyLongConverter;


/**
 *  FileHistoryBean.java - >>>POPIS MA<<<<
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: mrepasky $
 *@version      $Revision: 1.3 $
 *@created      Date: 17.05.2013 14:40:45
 *@modified     $Date: 2004/08/16 06:26:11 $
 *JPA generator link: jpa file_history sk.iway.iwcm.io.FileHistoryBean file_url:string change_date:date user_id:int
 */
@Entity
@Table(name="file_history")
public class FileHistoryBean extends ActiveRecord implements Serializable
{
	private static final long serialVersionUID = -1L;

	@Id
	@GeneratedValue(generator="WJGen_file_history")
	@TableGenerator(name="WJGen_file_history",pkColumnValue="file_history")	
	@Column(name="file_history_id")
	private int fileHistoryId;
	@Column(name="file_url")
	String fileUrl;
	@Column(name="change_date")
	@Temporal(TemporalType.TIMESTAMP)
	Date changeDate;
	@Column(name="user_id")
	int userId;
	@Column(name="deleted")
	boolean deleted;
	@Column(name="history_path")
	String historyPath;
	@Column(name="ip_address")
	String ipAddress;
	
	public int getFileHistoryId()
	{
		return fileHistoryId;
	}
	
	public void setFileHistoryId(int fileHistoryId)
	{
		this.fileHistoryId = fileHistoryId;
	}
	
	@Override
	public void setId(int id)
	{
		setFileHistoryId(id);
	}
	
	@Override
	public int getId()
	{
		return getFileHistoryId();
	}
	
	public String getFileUrl()
	{
		if (fileUrl != null) fileUrl = Tools.replace(fileUrl, "//", "/");
		return fileUrl;
	}

	public void setFileUrl(String fileUrl)
	{
		this.fileUrl = fileUrl;
	}

	public Date getChangeDate()
	{
		return changeDate;
	}

	public void setChangeDate(Date changeDate)
	{
		this.changeDate = changeDate;
	}

	public int getUserId()
	{
		return userId;
	}

	public void setUserId(int userId)
	{
		this.userId = userId;
	}

	public boolean isDeleted()
	{
		return deleted;
	}

	public void setDeleted(boolean deleted)
	{
		this.deleted = deleted;
	}
	
	public String getHistoryPath()
	{
		return historyPath;
	}

	public void setHistoryPath(String historyPath)
	{
		this.historyPath = historyPath;
	}
	
	public String getIpAddress()
	{
		return ipAddress;
	}

	public void setIpAddress(String ipAddress)
	{
		this.ipAddress = ipAddress;
	}
}