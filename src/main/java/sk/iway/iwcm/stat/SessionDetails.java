package sk.iway.iwcm.stat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
/**
 *  drzi data o last doc id
 *
 *@Title        magma-web
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.1.1.1 $
 *@created      Piatok, 2002, máj 31
 *@modified     $Date: 2003/01/28 11:30:13 $
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class SessionDetails
{
	private String sessionId;
	@JsonIgnore
	private int lastDocId=-1;
	@JsonIgnore
	private String lastURL = "";
	private String remoteAddr;
	private long logonTime;
	@JsonIgnore
	private long lastActivity;
	@JsonIgnore
	private String loggedUserName = null;
	private int loggedUserId = -1;
	@JsonIgnore
	private boolean admin = false;
	@JsonIgnore
	private int domainId;
	private String domainName;
	private String browserName;

	public long getLastActivity() {
		return lastActivity;
	}

	@JsonIgnore
	public Date getLastActivityAsDate() {
		return new Date(lastActivity);
	}

	public void setLastActivity(long lastActivity) {
		this.lastActivity = lastActivity;
	}

	public long getLogonTime() {
		return logonTime;
	}

	@JsonIgnore
	public Date getLogonTimeAsDate() {
		return new Date(logonTime);
	}

	public void setLogonTime(long logonTime) {
		this.logonTime = logonTime;
	}
}
