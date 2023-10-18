package sk.iway.iwcm.system.msg;

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


@Entity
@Table(name="admin_message")
public class AdminMessageBean implements Serializable 
{

	private static final long serialVersionUID = -3300244402731194831L;

	@Id
	@GeneratedValue(generator="WJGen_admin_message")
	@TableGenerator(name="WJGen_admin_message",pkColumnValue="admin_message")	
   @Column(name="admin_message_id")
	private int adminMessageId;
	
   @Column(name="create_by_user_id")
	private Integer createByUserId;
	
   @Column(name="create_date")
   @Temporal(TemporalType.TIMESTAMP)
	private Date createDate;
	
   @Column(name="is_readed")
	private Boolean isReaded;
	
   @Column(name="message_text")
	private String messageText;
	
   @Column(name="only_for_logged")
	private Boolean onlyForLogged;
	
   @Column(name="recipient_user_id")
	private Integer recipientUserId;

	public int getAdminMessageId()
	{
		return adminMessageId;
	}

	public void setAdminMessageId(int adminMessageId)
	{
		this.adminMessageId = adminMessageId;
	}

	public Integer getCreateByUserId()
	{
		return createByUserId;
	}

	public void setCreateByUserId(Integer createByUserId)
	{
		this.createByUserId = createByUserId;
	}

	public Date getCreateDate()
	{
		return createDate == null ? null : (Date) createDate.clone();
	}

	public void setCreateDate(Date createDate)
	{
		this.createDate = createDate == null ? null : (Date) createDate.clone();
	}

	public Boolean isReaded()
	{
		return isReaded;
	}

	public void setReaded(Boolean isReaded)
	{
		this.isReaded = isReaded;
	}

	public String getMessageText()
	{
		return messageText;
	}

	public void setMessageText(String messageText)
	{
		this.messageText = messageText;
	}

	public Boolean isOnlyForLogged()
	{
		return onlyForLogged;
	}

	public void setOnlyForLogged(Boolean onlyForLogged)
	{
		this.onlyForLogged = onlyForLogged;
	}

	public Integer getRecipientUserId()
	{
		return recipientUserId;
	}

	public void setRecipientUserId(Integer recipientUserId)
	{
		this.recipientUserId = recipientUserId;
	}

}



