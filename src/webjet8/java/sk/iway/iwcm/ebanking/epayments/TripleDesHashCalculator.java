package sk.iway.iwcm.ebanking.epayments;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang.ArrayUtils;

/**
 *  TripleDesHashCalculator.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: jraska $
 *@version      $Revision: 1.1 $
 *@created      Date: 28.8.2009 8:00:55
 *@modified     $Date: 2009/09/04 11:20:21 $
 */
public class TripleDesHashCalculator
{
	private TripleDesHashCalculator() {
		//utility class
	}

	public static String calculateHash(String stringToEncrypt, String privateKey)
	{
		String sign="";
		try
		{
			byte[] key=Base64.getDecoder().decode(privateKey);
			byte[] key3=ArrayUtils.subarray(key, 0, 8);
			key=ArrayUtils.addAll(key, key3);

			MessageDigest sha = MessageDigest.getInstance("SHA-1");
			byte[] shaDigest = sha.digest(stringToEncrypt.getBytes());
			shaDigest = ArrayUtils.addAll(shaDigest, new byte[]{(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF});

			byte[] iv = new byte[8];
			IvParameterSpec ivSpec = new IvParameterSpec(iv); //NOSONAR

			Cipher ecipher = Cipher.getInstance("DESede/CBC/NoPadding"); //NOSONAR
			ecipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "DESede"), ivSpec);
			byte[] desCrypt  = ecipher.doFinal(shaDigest);

			sign = Base64.getEncoder().encodeToString(desCrypt);
		}
		catch (NoSuchPaddingException|IllegalBlockSizeException|BadPaddingException|KeyException|NoSuchAlgorithmException|InvalidAlgorithmParameterException e){sk.iway.iwcm.Logger.error(e);}

		return sign.trim();
	}
}
