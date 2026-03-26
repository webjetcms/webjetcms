package sk.iway.iwcm.common;

import java.io.File;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.users.UserDetails;

public class ForumTools {
    private static String replaceFirstOccurence(String src, String oldStr, String newStr)
     {
        if (src == null)
         {
             return (null);
         }
         if (src.indexOf(oldStr) == -1)
         {
             return (src);
         }
         StringBuilder result = new StringBuilder(src.length() + 50);
         int startIndex = 0;
         int endIndex = src.indexOf(oldStr);
         if (endIndex != -1)
         {
             result.append(src.substring(startIndex, endIndex));
             result.append(newStr);
             startIndex = endIndex + oldStr.length();
         }
         result.append(src.substring(startIndex, src.length()));
         return result.toString();
     }

    public static String replaceSignatureCodes(UserDetails user)
    {
        if (user == null) return("");

        String signature = user.getSignature();
        if (signature == null) signature = "";

        try
         {
             if (signature.toLowerCase().indexOf("javascript")!=-1) return("");

             signature = Tools.replace(signature, "[img]/images/gallery/signature/", "<img src='");
             signature = Tools.replace(signature, "[/img]", "' />");

             signature = Tools.replace(signature, "[b]", "<strong>");
             signature = Tools.replace(signature, "[/b]", "</strong>");

             //skus najst priamo signaturu na disku
             String fileName = "/images/gallery/signature/s_signature-"+user.getUserId()+".jpg";
             File f = new File(Tools.getRealPath(fileName));
             //Logger.println(ForumSaveAction.class, "replacujem signature: " + f.getAbsolutePath());
             if (f.exists())
             {
                 //robime replace iba prveho
                 fileName = "<img class='signatureImage' src='"+fileName+"'>";
                 signature = replaceFirstOccurence(signature, "[obrazok]", fileName);
                 signature = replaceFirstOccurence(signature, "[obrazek]", fileName);
                 signature = replaceFirstOccurence(signature, "[image]", fileName);
                 //Logger.println(ForumSaveAction.class, "replacnute: "+ fileName);
             }

             //replace na linku
             int start = signature.indexOf("[url=");
             int failsafe = 0;
             while (start != -1 && failsafe < 30)
             {
                 int end = signature.indexOf("[/url]", start);
                 if (end == -1) break;

                 int textStart = signature.indexOf(']', start);

                 String linka = signature.substring(start+5, textStart);
                 linka = linka.replace('\'', ' ');
                 String text = signature.substring(textStart+1, end);

                 if (text.indexOf("<img")==-1 && text.length() > 22) text = text.substring(0, 20) + "...";

                 signature = Tools.replace(signature, signature.substring(start, end+6), "<a href='"+linka+"' target='_blank'>"+text+"</a>");

                 start = signature.indexOf("[url=");
             }

             Logger.debug(ForumTools.class, "signature replacnute: " + signature);
         }
         catch (Exception ex)
         {
             sk.iway.iwcm.Logger.error(ex);
         }


        return(signature);
    }
}
