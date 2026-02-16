package sk.iway.iwcm;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Rozne podporne metody pre sifrovanie/hashovanie
 */
public class CryptoTools {


   /**
    * Vytvori SHA-256 hash zo zadaneho retazca
    * @param toHash
    * @return
    */
   public static String sha256Hash(String toHash) {
      StringBuffer hexString = null;
      MessageDigest md;
      try {
         md = MessageDigest.getInstance("SHA-256");
         md.update(toHash.getBytes());
         byte byteData[] = md.digest();
         StringBuffer sb = new StringBuffer();
         for (int i = 0; i < byteData.length; i++) {
            sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
         }
         hexString = new StringBuffer();
         for (int i = 0; i < byteData.length; i++) {
            String hex = Integer.toHexString(0xff & byteData[i]);
            if (hex.length() == 1)
               hexString.append('0');
            hexString.append(hex);
         }
      } catch (NoSuchAlgorithmException e) {
         sk.iway.iwcm.Logger.error(e);
      }
      if (hexString==null) return null;
      return hexString.toString();
   }

}
