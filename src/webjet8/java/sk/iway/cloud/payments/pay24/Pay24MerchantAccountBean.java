package sk.iway.cloud.payments.pay24;

import java.io.Serializable;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.ActiveRecord;

/**
 *  Pay24lMerchantAccountBean.java
 *
 *@Title        webjet8
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2017
 *@author       $Author: jeeff prau $
 *@version      $Revision: 1.3 $
 *@created      Date: 15.5.2017 9:41:43
 *@modified     $Date: 2004/08/16 06:26:11 $
 */

@Entity
@Table(name="pay24_merchant_account")
public class Pay24MerchantAccountBean extends ActiveRecord implements Serializable
{
	private static final long serialVersionUID = -1;
	private static String LIVE_URL = "https://admin.24-pay.eu/pay_gate/paygt";
	private static String TEST_URL = "https://doxxsl-staging.24-pay.eu/pay_gate/paygt";


	@Id
	@GeneratedValue(generator="WJGen_pay24_merchant_account")
	@TableGenerator(name="WJGen_pay24_merchant_account",pkColumnName="pay24_merchant_account")
	@Column
	private int id;
	@Column(name="eshop_id")
	private String eshopId;
	@Column(name="pay_key")
	private String key;
	@Column
	private String mid;
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

	public static String getLiveUrl()
	{
		return LIVE_URL;
	}

	public static String getTestUrl()
	{
		return TEST_URL;
	}

	public Pay24MerchantAccountBean(String eshopId, String mid, String key)
	{
		this.eshopId = eshopId;
		this.mid = mid;
		this.key = key;
	}

	/**Ak su vytvorene konstanty, urobi init z nich. Ak nie su, pouzije testovacie data.
	 *
	 */
	public Pay24MerchantAccountBean()
	{

	}

	public String getEshopId()
	{
		return eshopId;
	}

	public void setEshopId(String eshopId)
	{
		this.eshopId = eshopId;
	}

	public String getKey()
	{
		return key;
	}

	public void setKey(String key)
	{
		this.key = key;
	}

	public String getMid()
	{
		return mid;
	}

	public void setMid(String mid)
	{
		this.mid = mid;
	}

	public int getDomainId()
	{
		return domainId;
	}

	public void setDomainId(int domain_id)
	{
		this.domainId = domain_id;
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
		Pay24MerchantAccountBean other = (Pay24MerchantAccountBean) obj;
		if (domainId != other.domainId)
			return false;
		else if (eshopId != other.eshopId)
			return false;
		if (key == null)
		{
			if (other.key != null)
				return false;
		}
		else if (!key.equals(other.key))
			return false;
		if (mid == null)
		{
			if (other.mid != null)
				return false;
		}
		else if (!mid.equals(other.mid))
			return false;
		return true;
	}

	public static boolean is24PayResponse(HttpServletRequest request)
	{
		return !Tools.isAnyEmpty(request.getParameter("Amount"), request.getParameter("Result"));
	}

	public static boolean isSuccess(HttpServletRequest request)
	{
		return "OK".equals(request.getParameter("Result"));
	}

	public static double getAmount(HttpServletRequest request)
	{
		return Tools.getDoubleValue(request.getParameter("Amount"), -1);
	}
	//ak bude metoda generateSign vypisovat chybu napr: java.security.InvalidKeyException: Illegal key size
	//je potrebne pridat JCE (Java Cryptography Extension) do JAVY

	public static String generateSign(String message, String key, String iv)
	{
		try {
			Logger.debug(Pay24MerchantAccountBean.class, "message:"+message+", key:"+key+", iv:"+iv);
			//bcprov is not provided by WebJET CMS Security.addProvider(new BouncyCastleProvider());
			byte[] keyBytes = Hex.decodeHex(key.toCharArray());
			byte[] ivBytes = iv.getBytes();
			SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
			IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
			Cipher encryptCipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
			encryptCipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec);
			byte[] sha1Hash = DigestUtils.sha1(message);
			byte[] encryptedData = encryptCipher.doFinal(sha1Hash);
			return Hex.encodeHexString(encryptedData).substring(0, 32);
		} catch (Exception e) {
			System.out.println("ERROR! " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
}
