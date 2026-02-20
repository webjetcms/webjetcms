package sk.iway.iwcm.filebrowser;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Tools;


public class FileDirBean
{
   protected String name;
   protected String path;
   protected String icon;
   protected String imagesInDir="";
   protected String lastModified = "";
   protected String length;
   private static final DecimalFormat decimalFormat;
   private static final SimpleDateFormat sdf;
   private boolean dirProtected;

   static
   {
      decimalFormat = new DecimalFormat("0.##");
      sdf = new SimpleDateFormat(Constants.getString("dateTimeFormat"));
   }

   public String getName()
   {
      return name;
   }
   public void setName(String name)
   {
      this.name = name;
   }

   public boolean isDirProtected() {
      return dirProtected;
   }

   public void setDirProtected(boolean dirProtected) {
      this.dirProtected = dirProtected;
   }

   public void setPath(String path)
   {
   	path = Tools.replace(path, "//", "/");
      this.path = path;
   }
   public String getPath()
   {
      return path;
   }
   public void setIcon(String icon)
   {
      this.icon = icon;
   }
   public String getIcon()
   {
      return icon;
   }
   public String getImagesInDir()
   {
      return imagesInDir;
   }
   public void setImagesInDir(String imagesInDir)
   {
      this.imagesInDir = imagesInDir;
   }
   //TODO: ulozit aj long
   public void setLastModified(long lastModifiedLong)
   {
      this.lastModified = sdf.format(new java.util.Date(lastModifiedLong)).replace(" ","&nbsp;");
   }
   public String getLastModified()
   {
      return lastModified;
   }
   public String getLastModifiedHtml()
   {
      return lastModified;
   }
   //TODO: ulozit aj double
   public void setLength(double lengthDouble)
   {
      this.length = "";
      if (lengthDouble > (1024 * 1024))
      {
         length = decimalFormat.format(lengthDouble / (1024 * 1024))+ "&nbsp;<span class='lenMB'>MB</span>";
      }
      else if (lengthDouble > 1024)
      {
         length = decimalFormat.format(lengthDouble / 1024)+ "&nbsp;<span class='lenKB'>kB</span>";
      }
      else
      {
         length = decimalFormat.format(lengthDouble)+ "&nbsp;<span class='lenB'>B</span>";
      }
   }
   public String getLength()
   {
      return length;
   }
   public String getLengthHtml()
   {
      return length;
   }

   public boolean isImageEditable()
   {
   	boolean ret = false;
   	if (name == null)
   	{
   		return(false);
   	}
   	String nameLC = name.toLowerCase();
   	if (nameLC.endsWith(".jpg") || nameLC.endsWith(".gif") || nameLC.endsWith(".png"))
   	{
   		ret = true;
   	}
   	
   	return(ret);
   }

   public String getFileType() {
      if (name != null)
         return FileTools.getFileExtension(getName());
      else
         return "";
   }

}