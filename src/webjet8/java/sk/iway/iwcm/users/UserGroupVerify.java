package sk.iway.iwcm.users;

import java.util.Date;

/**
 * bean pre tabulku user_group_verify
 * @author Administrator
 *
 */
public class UserGroupVerify {
	private int id;
	private int userId;
	private String userGroups;
	private String hash;
	private Date createDate;
	private Date verifyDate;
	private String email;
	private String hostname;
	
	public Date getCreateDate() {
		return createDate == null ? null : (Date) createDate.clone();
	}
	public void setCreateDate(Date createDate) {
		this.createDate = createDate == null ? null : (Date) createDate.clone();
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getHash() {
		return hash;
	}
	public void setHash(String hash) {
		this.hash = hash;
	}
	public String getHostname() {
		return hostname;
	}
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getUserGroups() {
		return userGroups;
	}
	public void setUserGroups(String userGroups) {
		this.userGroups = userGroups;
	}
	public int getUserId() {
		return userId;
	}
	public void setUserId(int userId) {
		this.userId = userId;
	}
	public Date getVerifyDate() {
		return verifyDate == null ? null : (Date) verifyDate.clone();
	}
	public void setVerifyDate(Date verifyDate) {
		this.verifyDate = (Date) verifyDate.clone();
	}
}
