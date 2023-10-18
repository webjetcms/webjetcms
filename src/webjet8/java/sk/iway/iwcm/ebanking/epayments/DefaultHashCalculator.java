package sk.iway.iwcm.ebanking.epayments;

import java.security.KeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import cryptix.provider.key.RawSecretKey;
import cryptix.util.core.Hex;
import xjava.security.Cipher;
import xjava.security.IllegalBlockSizeException;

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
			byte bytesHash[] = hash.digest(toBeEncrypted.getBytes());
			Cipher des = Cipher.getInstance("DES/ECB", "Cryptix");

			des.initEncrypt(new RawSecretKey("DES", privateKey.getBytes()));
			byte bytesSIGN[] = des.crypt(bytesHash, 0, 8);
			sign = Hex.dumpString(bytesSIGN);
		}
		catch (NoSuchAlgorithmException e){sk.iway.iwcm.Logger.error(e);}
		catch (NoSuchProviderException e){sk.iway.iwcm.Logger.error(e);}
		catch (KeyException e){sk.iway.iwcm.Logger.error(e);}
		catch (IllegalBlockSizeException e){sk.iway.iwcm.Logger.error(e);}

		return sign.trim();
	}
}
