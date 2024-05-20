package sk.iway.tags;

import sk.iway.iwcm.DB;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import com.google.crypto.tink.subtle.Hex;

import java.security.MessageDigest;
import java.util.Calendar;

/**
 *  Tag pre platbu pomocou CardPay
 *
 *@Title        magma-web
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      Pondelok, 2004, marec 8
 *@modified     $Date: 2008/07/01 07:57:54 $
 */
public class CardPayTag extends TagSupport
{
	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 7041981705594785263L;

	private String mid;
	private String key;
	private String amount;
	private String vs;
	private String cs;
	private String rurl;
	private String name;
	private String rem;

	@Override
	public void release()
	{
		super.release();
		mid = null;
		key = null;
		amount = null;
		vs = null;
		cs = null;
		rurl = null;
		name = null;
		rem = null;
	}

	/**
	 *  Description of the Method
	 *
	 *@return                   Description of the Return Value
	 *@exception  JspException  Description of the Exception
	 */
	@Override
	public final int doEndTag() throws JspException
	{
		pageContext.removeAttribute("cardPayTagReply");
		pageContext.removeAttribute("cardPayTagShowForm");

		try
		{
			JspWriter out = pageContext.getOut();
			amount = amount.replace(',', '.');
			String retazec = mid + amount + vs + cs + rurl;

			Logger.debug(CardPayTag.class, "CardPayTag");
			Logger.debug(CardPayTag.class, retazec);

			String vsReply = pageContext.getRequest().getParameter("VS");
			String resReply = pageContext.getRequest().getParameter("RES");
			String signReply = pageContext.getRequest().getParameter("SIGN");

			if (vsReply != null && resReply != null && signReply != null)
			{
				//mame odpoved z TB

				String mySign = getSign(out, vsReply + resReply, key);

				if (signReply.equals(mySign))
				{
					Logger.debug(CardPayTag.class, "SEDI SIGN, reply="+resReply);
					pageContext.setAttribute("cardPayTagReply", resReply);
				}
				else
				{
					Logger.debug(CardPayTag.class, "NESEDI SIGN");
					pageContext.setAttribute("cardPayTagReply", "SIGN ERROR");
				}
			}
			else
			{
				//generujeme formular
				pageContext.setAttribute("cardPayTagShowForm", "true");

				//odstran diakritiku
				name = DB.internationalToEnglish(name);
				String ipc = pageContext.getRequest().getRemoteHost();

				String curr = "703"; //SKK
				Calendar cal = Calendar.getInstance();
				if (cal.get(Calendar.YEAR)>=2009) curr = "978"; //EUR

				String toHash = mid + amount + curr + vs + cs + rurl + ipc + name;
				if (Tools.isEmpty(rem)==false)
				{
					//toHash += rem;
				}
				String sign = getSign(out, toHash, key);

				//ok, mozeme vypisat hidden polozky formularu
				out.write("<input type='hidden' name='MID' value='" + mid + "'>\n");
				out.write("<input type='hidden' name='AMT' value='" + amount + "'>\n");
				out.write("<input type='hidden' name='CURR' value='" + curr + "'>\n");
				out.write("<input type='hidden' name='VS' value='" + vs + "'>\n");
				out.write("<input type='hidden' name='CS' value='" + cs + "'>\n");
				out.write("<input type='hidden' name='RURL' value='" + rurl + "'>\n");
				out.write("<input type='hidden' name='SIGN' value='" + sign + "'>\n");
				out.write("<input type='hidden' name='IPC' value='" + ipc + "'>\n");
				out.write("<input type='hidden' name='NAME' value='" + name + "'>\n");

				if (Tools.isEmpty(rem)==false)
				{
					out.write("<input type='hidden' name='REM' value='" + rem + "'>\n");
				}

			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return EVAL_PAGE;
	}

	public static String getSign(JspWriter out, String toHash, String key) throws Exception
	{
		MessageDigest sha = MessageDigest.getInstance("SHA-1");
		byte[] shaDigest = sha.digest(toHash.getBytes());

		//toto je iba vypis
		String shaDigestString = Hex.encode(shaDigest);
		shaDigestString = shaDigestString.substring(0, 16).trim();
		Logger.debug(CardPayTag.class, "toHash = [" + toHash + "]<br>");
		Logger.debug(CardPayTag.class, "SHA-1 hash = [" + shaDigestString + "]<br>");
		//koniec vypisu

		//key = "testep01";

		Cipher ecipher = Cipher.getInstance("DES");
		SecretKeySpec desKey = new SecretKeySpec(key.getBytes(), "DES");
		ecipher.init(Cipher.ENCRYPT_MODE, desKey);
		byte[] desCrypt  = ecipher.doFinal(shaDigest, 0, 16);

		String desCryptString = Hex.encode(desCrypt);
		Logger.debug(CardPayTag.class, "DES = [" + desCryptString + "]<br>");

		String sign = desCryptString.substring(0, 16).trim();
		return(sign.toUpperCase());
	}

	public String getMid()
	{
		return mid;
	}
	public void setMid(String mid)
	{
		this.mid = mid;
	}
	public String getKey()
	{
		return key;
	}
	public void setKey(String key)
	{
		this.key = key;
	}
	public String getAmount()
	{
		return amount;
	}
	public void setAmount(String amount)
	{
		this.amount = amount;
	}
	public String getVs()
	{
		return vs;
	}
	public void setVs(String vs)
	{
		this.vs = vs;
	}
	public String getCs()
	{
		return cs;
	}
	public void setCs(String cs)
	{
		this.cs = cs;
	}
	public String getRurl()
	{
		return rurl;
	}
	public void setRurl(String rurl)
	{
		this.rurl = rurl;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public String getRem()
	{
		return rem;
	}
	public void setRem(String rem)
	{
		this.rem = rem;
	}

}
