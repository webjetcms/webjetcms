package sk.iway.iwcm.common;


import java.security.MessageDigest;

import sk.iway.iwcm.Logger;

public class CloneManagerTools {


    /**
     * Ziska security hash zo stringu, pri chybe vrati prazdny string
     * @param input
     * @return
     */
    public static String getSecurityHash(String input)
    {
        String ret = "";
        try{
            String source = input + input.length();
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(source.getBytes());
            byte byteData[] = md.digest();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < byteData.length; i++)
            {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }
            ret = sb.toString();
        }
        catch (Exception e){
            Logger.debug(CloneManagerTools.class, "Problem generating security hash. Cause: " + e.getMessage());
        }
        return ret;
    }

}
