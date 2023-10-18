package sk.iway.iwcm.ebanking.epayments;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.ebanking.Payment;

/**
 *  TatraPayInformation.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: jraska $
 *@version      $Revision: 1.5 $
 *@created      Date: 26.8.2009 14:12:11
 *@modified     $Date: 2009/12/11 14:51:53 $
 */
class TatraPayInformation extends PaymentInformation
{

	TatraPayInformation()
	{
		this.merchantId = Constants.getString("basketPaymentTatraPayMid");
		this.key = getDecrypredKey(Constants.getString("basketPaymentTatraPayKey"));
		this.constantSymbol = Integer.valueOf(Constants.getInt("basketPaymentTatraPayConstantSymbol"));
	}
	@Override
	public String getUrlString()
	{
		String confUrl = Constants.getString("basketPaymentTatraPayUrl");
		if(Tools.isNotEmpty(confUrl))
			return confUrl;
		else
			return "https://moja.tatrabanka.sk/cgi-bin/e-commerce/start/e-commerce.jsp";
		//return "http://epaymentsimulator.monogram.sk/TB_TatraPay.aspx";
	}
	@Override
	public String getReturnEmail()
	{
		return Constants.getString("basketPaymentTatraPayNotificationEmail");
	}

	@Override
	public boolean hasOwnForm()
	{
		return true;
	}

	@Override
	public String generateForm(Payment payment, HttpServletRequest request)
	{
		if (request.getAttribute("RURL") == null)
			throw new IllegalStateException("HttpServletRequest needs to have 'RURL'(return URL) attribute");

		//Timestamp musí byť vo formáte DDMMYYYYHHMISS (DD-deň, MM-mesiac, YYYY-rok, HH-hodina, MI-minúta, SS-sekunda). TIMESTAMP musí byť v intrvale +/- 1 hodina voči UTC (GMT)
		String timestamp = new SimpleDateFormat("ddMMYYYYHHmmss").format(new Date());
		StringBuilder form = new StringBuilder().
				append("<FORM action='"+getUrlString()+"' METHOD='POST' name='payForm'>").
				append("<INPUT type=\"hidden\" NAME=\"MID\" value='"+getMerchantId()+"' />").
				append("<INPUT type=\"hidden\" NAME=\"AMT\" value='"+payment.getAmountString()+"' />").
				append("<INPUT type=\"hidden\" NAME=\"CURR\" value=\"978\" />").//978 = ISO EURO
				append("<INPUT type=\"hidden\" NAME=\"VS\" value='"+payment.getVariableSymbol()+"' />").
				append("<INPUT type=\"hidden\" NAME=\"CS\" value='"+payment.getConstantSymbol()+"' />").
				append("<INPUT type=\"hidden\" NAME=\"RURL\" value='"+request.getAttribute("RURL")+"' />").
				append("<INPUT type=\"hidden\" NAME=\"TIMESTAMP\" value='"+timestamp+"' />").
				append("<INPUT type=\"hidden\" NAME=\"HMAC\" value='"+generateHMAC(getStringToSign(payment, timestamp, request))+"' />");
		if (Tools.isNotEmpty(payment.getSpecificSymbol()))
			form.append("<INPUT type=\"hidden\" NAME=\"SS\" value='"+payment.getSpecificSymbol()+"' />");
		if (Tools.isNotEmpty(getReturnEmail()))
			form.append("<INPUT type=\"hidden\" NAME=\"REM\" value='"+getReturnEmail()+"' />");
		//form.append("<input type=\"submit\" value=\"Vykonať platbu\">");
		form.append("</FORM>");
		return form.toString();
	}

	private String getStringToSign(Payment payment, String timestamp, HttpServletRequest request)
	{
		//stringToSign = MID + AMT + CURR + VS + SS + CS + RURL + REM + TIMESTAMP;

		String mid = getMerchantId();
		String amt = payment.getAmountString();
		String curr = "978";
		String vs = payment.getVariableSymbol();
		String ss = "";
		if(Tools.isNotEmpty(payment.getSpecificSymbol()))
			ss = payment.getSpecificSymbol();
		String cs = payment.getConstantSymbol();
		String rurl = request.getAttribute("RURL").toString();
		String rem = "";
		if(Tools.isNotEmpty(getReturnEmail()))
			rem = getReturnEmail();

		//System.out.println("Key tatra plain: " + paymentInfo.getKey());
		//System.out.println("Key tatra hex: " + bytesToHex(paymentInfo.getKey().getBytes()));

		StringBuilder stringToSign = new StringBuilder().
				append(mid).
				append(amt).
				append(curr).
				append(vs).
				append(ss).
				append(cs).
				append(rurl).
				append(rem).
				append(timestamp);

		return stringToSign.toString();
	}

	private String generateHMAC(String stringToSign)
	{
		String signature = null;
		try
		{
			byte[] keyBytes = getKey().getBytes(); // konverzia do binárneho formátu
			SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "HmacSHA256");
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(keySpec);
			byte[] stringToSignBytes = stringToSign.getBytes();
			byte[] hmacBin = mac.doFinal(stringToSignBytes);
			signature = bytesToHex(hmacBin); // konverzia do hexadecimálneho reťazca
		}
		catch(NoSuchAlgorithmException e){sk.iway.iwcm.Logger.error(e);}
		catch(InvalidKeyException e){sk.iway.iwcm.Logger.error(e);}

		return signature;
	}

	String bytesToHex(byte[] bytes) {
		char[] hexArray = "0123456789ABCDEF".toCharArray();
		char[] hexChars = new char[bytes.length * 2];
		for ( int j = 0; j < bytes.length; j++ ) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	@Override
	public boolean validateBankResponce(HttpServletRequest request)
	{
		if("FAIL".equals(request.getParameter("RES")))
			return false;

		String stringToVerify = getStringToVerify(request);

		String generatedHMAC = generateHMAC(stringToVerify);
		if(generatedHMAC==null || !generatedHMAC.equals(request.getParameter("HMAC")))
			return false;

		return validateECDSA(request, stringToVerify);
	}

	private String getStringToVerify(HttpServletRequest request)
	{
		String amt = request.getParameter("AMT");
		String curr = request.getParameter("CURR");
		String vs = request.getParameter("VS");
		String ss = request.getParameter("SS");
		String cs = request.getParameter("CS");
		String res = request.getParameter("RES");
		String tid = request.getParameter("TID");
		String timestamp = request.getParameter("TIMESTAMP");

		if(amt==null) amt="";
		if(curr==null) curr="";
		if(vs==null) vs="";
		if(ss==null) ss="";
		if(cs==null) cs="";
		if(res==null) res="";
		if(tid==null) tid="";
		if(timestamp==null) timestamp="";

		StringBuilder stringToSign = new StringBuilder().
				append(amt).
				append(curr).
				append(vs).
				append(ss).
				append(cs).
				append(res).
				append(tid).
				append(timestamp);

		return stringToSign.toString();
	}

	private boolean validateECDSA(HttpServletRequest request, String stringToVerify)
	{
		try
		{
			String expandedStringToVerify = stringToVerify + request.getParameter("HMAC");
			String ECDSA = request.getParameter("ECDSA");
			String publicKey = getECDSA(request.getParameter("ECSDA_KEY"));
			if(Tools.isEmpty(publicKey))
				return false;


			X509EncodedKeySpec spec = new X509EncodedKeySpec(DatatypeConverter.parseBase64Binary(publicKey));
			KeyFactory keyFactory = KeyFactory.getInstance("EC");
			PublicKey pKey = keyFactory.generatePublic(spec);

			Signature ecdsaSign = Signature.getInstance("SHA256withECDSA");
			ecdsaSign.initVerify(pKey);
			ecdsaSign.update(expandedStringToVerify.getBytes("UTF-8"));

			if (ecdsaSign.verify(new BigInteger(ECDSA, 16).toByteArray())) {
				return true;
			}
		}
		catch(Exception e) {sk.iway.iwcm.Logger.error(e);}

		return false;
	}

	private String getECDSA(String keyId)
	{
		if(Tools.isEmpty(keyId))
			return null;

		String keysString = Tools.downloadUrl("https://moja.tatrabanka.sk/e-commerce/ecdsa_keys.txt");
		if(Tools.isNotEmpty(keysString))
		{
			String[] keys = keysString.split("KEY_ID: ");
			String result = "";
			for(String key : keys)
			{
				if(Tools.isNotEmpty(key))
				{
					String[] lines = key.split("\\r?\\n");
					if(lines!=null && keyId.equals(lines[0]))
					{
						int begin = 0;
						int end = 0;
						for(int i=0; i<lines.length; i++)
						{
							if("-----BEGIN PUBLIC KEY-----".equals(lines[i]))
								begin = i+1;
							if("-----END PUBLIC KEY-----".equals(lines[i]))
								end = i;
						}
						for(int i=begin; i<end; i++)
							result += lines[i];
						return result;
					}
				}
			}
		}
		return null;
	}
}
