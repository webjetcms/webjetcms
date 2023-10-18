package sk.iway.iwcm.grideditor.generator;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.io.IwcmFile;


public class ScreenshotGenerator {

    private ScreenshotGenerator() {

    }

    /**
     * Vyvtori nahlad stranky
     * Pozor, potrebne pripojit Phantomjs binarny subor
     * @param properties - parametre pre metodu
     */
    public static ScreenShotPropBean generatePreview(ScreenShotPropBean properties)
    {
        String[] args = new String[11];//4
        args[0] = Constants.getString("grideditorPhantomjsPath")+getRuntimeFile();//phantomjs.exe";
        IwcmFile f1 = new IwcmFile(args[0]);
        IwcmFile f2 = new IwcmFile("C:/"+ScreenshotGenerator.getRuntimeFile());
        if(!f1.exists() && f2.exists())
        {
            f1 = f2;
            args[0] = f2.getAbsolutePath();
        }
        String error = "";
        if(!f1.exists())
        {
            error = "Binarny subor Phantomjs neexistuje nie je mozne generovat nahlady. Subor: " + args[0];
            Logger.debug(ScreenshotGenerator.class, error);
            properties.getErrors().add(error);
        }
        else
        {
            //JS skript na generovanie nahladu
            args[1] = Tools.getRealPath("/components/grideditor/phantom/phantomjs_screenshot.js");
            IwcmFile fileScreenShotJs = new IwcmFile(args[1]);
            if(!fileScreenShotJs.exists())
            {
                error = "JS subor pre Phantomjs neexistuje nie je mozne generovat nahlady. Subor: " + args[1];
                Logger.debug(ScreenshotGenerator.class, error);
                properties.getErrors().add(error);
                properties.setResultNumber(5);
                //return 5;
            }
            //web stranka
            args[2] = (properties.getScreenshotUrl().startsWith("http")) ? properties.getScreenshotUrl() : "http://"+properties.getScreenshotUrl();
            //nazov suboru-nahladu ktory bude vytvoreny
            String generatedScreenshotFileName = convertDomainToImageName(properties.getScreenshotUrl());
            String defaultScreenshotFilePath = Constants.getString("phantomjsFileUrl")+"screenshots/"+generatedScreenshotFileName;//Tools.getRealPath(Constants.getString("imagesRootDir") + "/screenshots/"+file_name);
            String dirRealPath = Tools.getRealPath( properties.getSaveImageToPath());//Tools.getRealPath(Constants.getString("imagesRootDir") + "/screenshots/");

            if (dirRealPath != null)
            {
                IwcmFile screenshotFile = new IwcmFile(dirRealPath);
                if (screenshotFile.getParentFile().getParentFile().getParentFile().exists()==false)
                    screenshotFile.getParentFile().getParentFile().getParentFile().mkdirs();
                if (screenshotFile.getParentFile().getParentFile().exists()==false)
                    screenshotFile.getParentFile().getParentFile().mkdirs();
                if (screenshotFile.getParentFile().exists()==false)
                    screenshotFile.getParentFile().mkdirs();
            }

            args[3] = fixImagePath(properties.getSaveImageToPath() ,defaultScreenshotFilePath);

            args[4] = properties.getImageWidth()+"";//"920";//width
            args[5] = properties.getImageHeight()+"";//"880";//height
            args[6] = properties.getZoom()+"";//"1";//zoom
            args[7] = properties.getTimeDelayMilisecond()+"";//"3000";//delay before screenshot
            //ak potrebujeme nastavit cookie
            args[8] = properties.getCookieHtmlData();
            args[9] = getDomainName(properties.getScreenshotUrl());// domena pre cookies
            args[10] = properties.isAutoHeigth()? "true" : "false";
            try
            {
                Runtime rt = Runtime.getRuntime();
                Process proc = rt.exec(args);
                if(properties.isDebug())
                {
                    InputStream stderr = proc.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(stderr, Constants.FILE_ENCODING));
                    String line = null;
                    while ((line = br.readLine()) != null)
                    {
                        Logger.debug(ScreenshotGenerator.class, "Status for "+properties.getScreenshotUrl()+" : "+line);
                        if(Tools.isNotEmpty(line))
                        {
                            properties.getPhantomErrors().add(line);
                        }
                    }
                    br.close();
                }
                properties.setResultNumber(proc.waitFor());
            }
            catch (Exception ex)
            {
                sk.iway.iwcm.Logger.error(ex);
            }
            Logger.debug(ScreenshotGenerator.class,"exitValue: "+ properties.getResultNumber()+" "+fixImagePath(properties.getSaveImageToPath() ,defaultScreenshotFilePath));
            properties.setSaveImageToPath(fixImagePath(properties.getSaveImageToPath() ,defaultScreenshotFilePath));
            return properties;
        }
        properties.setResultNumber(4);
        return properties;
    }

    /** Ak je cesta relafivna, doplni absolutnu. Ak cesta konci na lomitko, doplni "image.jpg". Ak cesta neobsahuje bodku (priponu), doplni ".jpg"
     *
     * @param imagePath
     * @param defaultPath
     * @return
     */
    private static String fixImagePath(String imagePath,String defaultPath)
    {
        String fixedPath = imagePath;
        if(Tools.isNotEmpty(imagePath))
        {
            if(fixedPath.endsWith("/"))
            {
                fixedPath =  fixedPath+"image.jpg" ;
            }
            else if(fixedPath.indexOf(".") == -1)
            {
                fixedPath = fixedPath + ".jpg";
            }

            if(fixedPath.startsWith("/"))
            {
                fixedPath = Tools.getRealPath(fixedPath);
            }
            return fixedPath;
        }
        return defaultPath;
    }

    public static String convertDomainToImageName (String str)
    {
        return (((str.replace("http://", "")).replace("www.","")).replace("/","")).replace(".","_")+".jpg";
    }

    /**
     *
     * @return vrati meno RuntimeFile-u "phantomjs" a pre Win "phantomjs.exe"
     */
    public static String getRuntimeFile()
    {
        String result = "phantomjs";
        if (System.getProperty("os.name").indexOf("Windows") != -1)
        {
            result  = "phantomjs.exe";
        }
        return result ;
    }

    public static String getDomainName(String url){
        try
        {
            URI uri = new URI(url);
            String domain = uri.getHost();
            if(domain != null)
            {
                return domain.startsWith("www.") ? domain.substring(4) : domain;
            }
        }
        catch (URISyntaxException use)
        {
            sk.iway.iwcm.Logger.error(use);
        }
        return "";
    }
}
