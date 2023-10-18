package sk.iway.iwcm.ebanking.epayments;

import java.security.KeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Base64;

import org.apache.commons.lang.ArrayUtils;

import cryptix.provider.key.RawSecretKey;
import xjava.security.Cipher;
import xjava.security.IllegalBlockSizeException;

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

			Cipher ecipher = Cipher.getInstance("DES-EDE3/CBC", "Cryptix");
			RawSecretKey desKey = new RawSecretKey("DES-EDE3", key);
			ecipher.initEncrypt(desKey);
			byte[] desCrypt  = ecipher.crypt(shaDigest);

			sign = Base64.getEncoder().encodeToString(desCrypt);
		}
		catch (NoSuchAlgorithmException e){sk.iway.iwcm.Logger.error(e);}
		catch (NoSuchProviderException e){sk.iway.iwcm.Logger.error(e);}
		catch (KeyException e){sk.iway.iwcm.Logger.error(e);}
		catch (IllegalBlockSizeException e){sk.iway.iwcm.Logger.error(e);}

		return sign.trim();
	}
}
