package sk.iway.iwcm.ebanking.epayments;

import java.security.KeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import com.google.crypto.tink.subtle.Hex;

/**
 *  DefaultHashCalculator.java
 *
 *		Calculates hashString out of a given String.
 *		Uses a default SHA/DES encrypting combination
 *
 *	used by:
 *		{@link TatraPayInformation}
 *		{@link VubEplatbyInformation}
 *		{@link UniPlatbaInformation}
 *
 *@Title        webjet4_repair
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: murbanec $
 *@version      $Revision: 1.1 $
 *@created      Date: 27.8.2009 18:52:31
 *@modified     $Date: 2009/08/27 17:10:01 $
 */
class DefaultHashCalculator
{
	static String calculateHash(String toBeEncrypted, String privateKey)
	{
		String sign = "";
		try
		{
			MessageDigest hash = MessageDigest.getInstance("SHA-1");
			byte[] bytesHash = hash.digest(toBeEncrypted.getBytes());
			Cipher des = Cipher.getInstance("DES/ECB/NoPadding");

			des.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(privateKey.getBytes(), "DES"));
			byte[] bytesSIGN = des.doFinal(bytesHash, 0, 8);
			sign = Hex.encode(bytesSIGN);
		}
		catch (NoSuchPaddingException|IllegalBlockSizeException|BadPaddingException|KeyException|NoSuchAlgorithmException e){sk.iway.iwcm.Logger.error(e);}

		return sign.trim().toUpperCase();
	}
}
