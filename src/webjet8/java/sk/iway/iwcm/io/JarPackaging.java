package sk.iway.iwcm.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;

/**
 * JarPackaging.java - trieda pre podporu JarPackagingu (ukladania JSP a statickych suborov do JAR archivov)
 * ticket #34006
 * https://stackoverflow.com/questions/5013917/can-i-serve-jsps-from-inside-a-jar-in-lib-or-is-there-a-workaround
 * https://alexismp.wordpress.com/2010/04/28/web-inflib-jarmeta-infresources/
 * https://www.javacodegeeks.com/2013/08/servlet-3-0-overview.html
 *
 * @author $Author: jeeff $
 */
public class JarPackaging
{
   private static String JAR_LOCATION_ADMIN = null; //NOSONAR
   private static String JAR_LOCATION_COMPONENTS = null; //NOSONAR
   private static String JAR_LOCATION_CORE = null; //NOSONAR

   protected JarPackaging() {
      //utility class
   }

   public static void initialize()
   {
      JAR_LOCATION_COMPONENTS = initializeJarLocation("/META-INF/resources/components/_common/combine.jsp");

      if (Tools.isNotEmpty(JAR_LOCATION_COMPONENTS))
      {
         JAR_LOCATION_ADMIN = initializeJarLocation("/META-INF/resources/admin/v9/dist/views/dashboard/overview.html");
         JAR_LOCATION_CORE = initializeJarLocation("/sk/iway/iwcm/common/AdminTools.class");

         Logger.println(JarPackaging.class, "enableJspJarPackaging=true");
         Constants.setBoolean("enableJspJarPackaging", true);
         return;
      }

      Logger.println(JarPackaging.class, "enableJspJarPackaging=false");
   }

   /**
    * Vrati cestu k JAR suboru obsahujucemu danu cestu
    */
   private static String initializeJarLocation(String path)
   {
      URL u = JarPackaging.class.getResource(path);
      if (u != null)
      {
         //je to nieco ako jar:file:/Users/jeeff/.gradle/caches/modules-2/files-2.1/sk.iway/webjet9admin/8.6-SNAPSHOT/e8615553500ae75b655eb175276b945fe82799bd/webjet9admin-8.6-SNAPSHOT.jar!/META-INF/resources/WEB-INF/build.properties
         String fileLocation = u.getPath();
         if (u.getPath().contains(".jar")==false) return null;

         int i = fileLocation.indexOf(":");
         int j = fileLocation.indexOf(".jar!");
         if (j > i) fileLocation = fileLocation.substring(i+1, j+4);

         //na windowse to vrati cestu ako /c:/nieco co nie je platne, musi to byt c:/nieco
         if (fileLocation.charAt(2) == ':')
         {
            Logger.println(JarPackaging.class, "Detected :, probably windows, fixing path="+path);
            fileLocation = fileLocation.substring(1);
         }

         Logger.println(JarPackaging.class, "Found jarpackaging JAR: path="+path+" fileLocation="+fileLocation);

         return fileLocation;
      }
      return null;
   }

   private static String addBaseFolder(String virtualPath)
   {
      return "/META-INF/resources"+virtualPath;
   }

   private static String getJarFileLocation(String virtualPath)
   {
      if (virtualPath.startsWith("/WEB-INF/classes/")) return JAR_LOCATION_CORE;

      //toto je vynimka, mame to v components kedze je to potrebne aj pre public nod
      if (virtualPath.equals("/WEB-INF/mime.types") || virtualPath.equals("/WEB-INF/build.properties")) return JAR_LOCATION_COMPONENTS;

      if (virtualPath.startsWith("/admin") || virtualPath.startsWith("/WEB-INF/")) return JAR_LOCATION_ADMIN;

      return JAR_LOCATION_COMPONENTS;
   }

   public static boolean isJarPackaging(String virtualPath)
   {
      if (Constants.getBoolean("enableJspJarPackaging")==false) return false;

      Logger.debug(JarPackaging.class, "isJarPackaging("+virtualPath+")");

      if (virtualPath.equals("/components/"+Constants.getInstallName()) || virtualPath.startsWith("/components/"+Constants.getInstallName()+"/")) return false;

      //TODO: doplnit nejake dalsie vynimky podla konf. premennej

      //kvoli performance je to takto natvrdo
      if (virtualPath.startsWith("/admin") || virtualPath.startsWith("/components") || virtualPath.startsWith("/apps") ||
          virtualPath.equals("/WEB-INF/build.properties") || virtualPath.equals("/WEB-INF/sql/autoupdate.xml") || virtualPath.equals("/WEB-INF/sql/autoupdate-webjet9.xml") ||
          virtualPath.startsWith("/WEB-INF/sql/blank_web") || virtualPath.startsWith("/WEB-INF/classes/text") || virtualPath.startsWith("/WEB-INF/fonts/") ||
          virtualPath.equals("/WEB-INF/mime.types") || virtualPath.equals("/WEB-INF/struts-config.xml"))
      {
         //tu nekontrolujeme ci existuje, automaticky je to JarPackaging, az nasledne sa k tomu pridaju fyzicke subory v listFiles
         if ("/components".equals(virtualPath) || "/apps".equals(virtualPath))
         {
            return true;
         }

         //overme ci existuje realny subor, ak ano, tak to nie je jarpackaging
         //musime pouzit File, lebo IwcmFile by bolo nazad zacyklenie!!!
         //TODO: nejaka cache, asi idealne na cele isJarPackaging
         File f = new File(Tools.getRealPath(virtualPath));
         if (f.exists()) return false;

         //subor neexistuje, takze je to v jarku
         return true;
      }

      return false;
   }

   public static IwcmFile[] listFiles(String virtualPath)
   {
      //Logger.debug(JarPackaging.class, "listFiles("+virtualPath+")");

      FileSystem fs = null;
      DirectoryStream<Path> directoryStream = null;
      try
      {
         List<IwcmFile> files = new ArrayList<>();
         String realPath = Tools.getRealPath(virtualPath);

         // Run with JAR file
         Path jarFile = Paths.get(getJarFileLocation(virtualPath));

         Logger.debug(JarPackaging.class, "listFiles("+virtualPath+"), jarFile="+jarFile);

         Set<String> existingFileNames = new HashSet<>();
         ClassLoader loader = null;
         fs = FileSystems.newFileSystem(jarFile, loader);
         try {
            directoryStream = Files.newDirectoryStream(fs.getPath(addBaseFolder(virtualPath)));
            for(Path p: directoryStream)
            {
               Logger.debug(JarPackaging.class,"FileName="+p.getFileName()+" root="+p.getRoot());

               IwcmFile f = new IwcmFile(realPath, p.getFileName().toString());
               files.add(f);
               existingFileNames.add(Tools.replace(p.getFileName().toString(), "/", ""));
            }
         } catch (NotDirectoryException nde) {
            //adresar v JAR neexistuje, nevadi, skusime este realny suborovy system
         }

         //ak je nieco v JarPackagingu pridaj pripadne skutocne adresare na disku, jedna sa hlavne o /components kde su komponenty aj v JARku aj custom na disku
         File physicalDir = new File(Tools.getRealPath(virtualPath));
         if (physicalDir.exists())
         {
            File[] physicalFiles = physicalDir.listFiles();
            if (physicalFiles != null)
            {
               for (File file : physicalFiles)
               {
                  if (existingFileNames.contains(file.getName())) continue;
                  IwcmFile f = new IwcmFile(file);
                  files.add(f);
                  existingFileNames.add(file.getName());
               }
            }
         }

         return files.toArray(new IwcmFile[0]);
      }
      catch (IOException ex)
      {
         sk.iway.iwcm.Logger.error(ex);
      }
      finally
      {
         if (directoryStream != null) try { directoryStream.close(); } catch (IOException ex) { sk.iway.iwcm.Logger.error(ex); }
         if (fs != null) try { fs.close(); } catch (IOException ex) { sk.iway.iwcm.Logger.error(ex); }
      }

      return new IwcmFile[0];
   }

   public static boolean isDirectory(String virtualPath)
   {
      //TODO: overit nejako, ci je to subor alebo adresar
      URL u = JarPackaging.class.getResource("/META-INF/resources"+virtualPath);
      //Logger.debug(JarPackaging.class, "isDirectory("+virtualPath+"), url="+u);
      if (u != null && virtualPath.contains(".")==false) return true;

      if (u == null) {
         //over, ci to nie je skutocny adresar na disku
         File f = new File(Tools.getRealPath(virtualPath));
         if (f.exists() && f.isDirectory()) return true;
      }

      return false;
   }

   public static boolean isFile(String virtualPath)
   {
      //TODO: overit nejako, ci je to subor alebo adresar
      URL u = JarPackaging.class.getResource("/META-INF/resources"+virtualPath);
      Logger.debug(JarPackaging.class, "isFile("+virtualPath+"), url="+u);
      if (u != null && virtualPath.contains(".")) return true;

      return false;
   }

   public static boolean exists(String virtualPath)
   {
      URL u = JarPackaging.class.getResource("/META-INF/resources"+virtualPath);
      Logger.debug(JarPackaging.class, "exists("+virtualPath+"), url="+u);
      if (u != null) return true;

      return false;
   }

   public static InputStream getInputStream(String virtualPath)
   {
      return JarPackaging.class.getResourceAsStream(addBaseFolder(virtualPath));
   }

   public static long getSize(String virtualPath)
   {
      long size = 0;
      JarFile jarFile = null;
      try
      {
         jarFile = new JarFile(getJarFileLocation(virtualPath));
         //po skumani sa vola jarFile.getEntry bez uvodneho lomitka
         size = jarFile.getEntry(addBaseFolder(virtualPath).substring(1)).getSize();
      }
      catch (Exception ex)
      {
         sk.iway.iwcm.Logger.error(ex);
      }
      finally {
         if (jarFile != null) try { jarFile.close(); } catch (Exception ex) { sk.iway.iwcm.Logger.error(ex); }
      }
      return size;
   }

   public static long getLastMofified(String virtualPath)
   {
      long lastmodified = 0;
      JarFile jarFile = null;
      try
      {
         jarFile = new JarFile(getJarFileLocation(virtualPath));
         //po skumani sa vola jarFile.getEntry bez uvodneho lomitka
         ZipEntry entry = jarFile.getEntry(addBaseFolder(virtualPath).substring(1));
         if (entry != null) lastmodified = entry.getTime();
      }
      catch (Exception ex)
      {
         sk.iway.iwcm.Logger.error(ex);
      }
      finally {
         if (jarFile != null) try { jarFile.close(); } catch (Exception ex) { sk.iway.iwcm.Logger.error(ex); }
      }
      return lastmodified;
   }

   public static List<String> getJarLocations()
   {
      List<String> locations = new ArrayList<>();
      locations.add(JAR_LOCATION_ADMIN);
      locations.add(JAR_LOCATION_COMPONENTS);
      locations.add(JAR_LOCATION_CORE);

      return locations;
   }
}
