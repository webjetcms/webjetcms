package sk.iway.cloud.payments.paypal;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import sk.iway.iwcm.database.ActiveRecord;

/**
 *  PayPalMerchantAccountBean.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2014
 *@author       $Author: jeeff mhalas $
 *@version      $Revision: 1.3 $
 *@created      Date: 25.9.2014 10:16:05
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
@Entity
@Table(name="paypal_merchant_account")
public class PayPalMerchantAccountBean extends ActiveRecord implements Serializable
{
	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3340829533291181147L;
	@Id
	@GeneratedValue(generator="WJGen_paypal_merchant_account")
	@TableGenerator(name="WJGen_paypal_merchant_account",pkColumnName="paypal_merchant_account")	
	@Column
	private int id;
	@Column
	private String user;
	@Column
	private String pwd;
	@Column
	private String signature;
	@Column(name="domain_id")
	private int domainId;
	
	@Override
	public int getId()
	{
		return id;
	}

	@Override
	public void setId(int id)
	{
		this.id = id;
	}

	public String getUser()
	{
		return user;
	}

	public void setUser(String user)
	{
		this.user = user;
	}

	public String getPwd()
	{
		return pwd;
	}

	public void setPwd(String pwd)
	{
		this.pwd = pwd;
	}

	public String getSignature()
	{
		return signature;
	}

	public void setSignature(String signature)
	{
		this.signature = signature;
	}

	public int getDomainId()
	{
		return domainId;
	}

	public void setDomainId(int domainId)
	{
		this.domainId = domainId;
	}

	@Override
	public String toString()
	{
		return "PayPalMerchantAccountBean [id=" + id + ", user=" + user + ", pwd=" + pwd + ", signature=" + signature
					+ ", domainId=" + domainId + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + domainId;
		result = prime * result + ((pwd == null) ? 0 : pwd.hashCode());
		result = prime * result + ((signature == null) ? 0 : signature.hashCode());
		result = prime * result + ((user == null) ? 0 : user.hashCode());
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
		PayPalMerchantAccountBean other = (PayPalMerchantAccountBean) obj;
		if (domainId != other.domainId)
			return false;
		if (pwd == null)
		{
			if (other.pwd != null)
				return false;
		}
		else if (!pwd.equals(other.pwd))
			return false;
		if (signature == null)
		{
			if (other.signature != null)
				return false;
		}
		else if (!signature.equals(other.signature))
			return false;
		if (user == null)
		{
			if (other.user != null)
				return false;
		}
		else if (!user.equals(other.user))
			return false;
		return true;
	}
}
