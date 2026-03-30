package sk.iway.iwcm.common;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DebugTimer;
import sk.iway.iwcm.gallery.GalleryDB;
import sk.iway.iwcm.gallery.ImageInfo;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmInputStream;

public class GalleryDBTools {
    //hash mapa pre ukladanie rozmerov obrazkov do cache
    private static Map<String, int[]> imagesDims;

    private GalleryDBTools() {

    }

    /**
     * Vrati velkost zadaneho obrazku
     * @param imageFile - IwcmFile obrazku
     * @return int[0] - sirka, int[1] - vyska, ak nenejade rozmery vracia nulove hodnoty
     */
    public static int[] getImageSize(IwcmFile imageFile)
    {
        int[] dims = {0,0};

        if(imageFile == null)
            return dims;

        String imagesDimsKey = imageFile.getAbsolutePath()+"-"+imageFile.lastModified();

        if(imagesDims == null) imagesDims = new ConcurrentHashMap<String, int[]>();
        dims = imagesDims.get(imagesDimsKey); //skusim ziskat rozmery z cache

        if(dims == null)
        {
            dims = new int[2];

            ImageInfo ii = new ImageInfo();
            try
            {
                IwcmInputStream is = new IwcmInputStream(imageFile);
                ii.setInput(is);
                if (ii.check())
                {
                    dims[0] = ii.getWidth();
                    dims[1] = ii.getHeight();
                }
                is.close();
            }
            catch (Exception ex)
            {
                sk.iway.iwcm.Logger.error(ex);
            }

            imagesDims.put(imagesDimsKey, dims); //ukladam rozmery do cache
        }

        return dims;
    }

    /**
     * Najprv vyreze z obrazka dany rozmer a vyrezu zmeni velkost
     */
    public static int cropAndResize(IwcmFile from, int cwidth, int cheight, int cleft, int ctop, int finalWidth, int finalHeight, String fillColor, boolean exactFinalSize, IwcmFile to, int imageQuality, int ip)
    {
        // comand line :   convert '*.jpg' -crop 120x120+10+5 -resize 105x20  thumbnail%03d.png

        if (!to.getParentFile().exists())
        {
            to.getParentFile().mkdirs();
        }

        //zakladna ochrana pred code injection
        if (fillColor!=null && fillColor.length()!=6) fillColor = null;

        /** Overim, ci na serveri existuje ImageMagic **/
        if (ImageTools.existsImageMagickConvertCommand())
        {
            return cropAndResizeImageMagick(from, cwidth, cheight, cleft, ctop, finalWidth, finalHeight, fillColor, exactFinalSize, to, imageQuality, ip);
        }
        else
        {
            return cropAndResizeJava(from, cwidth, cheight, cleft, ctop, finalWidth, finalHeight, fillColor, exactFinalSize, to, imageQuality, ip);
        }
    }

    private static int cropAndResizeImageMagick(IwcmFile from, int cwidth, int cheight, int cleft, int ctop, int finalWidth, int finalHeight, String fillColor, boolean exactFinalSize, IwcmFile to, int imageQuality, int ip)
    {
        finalWidth = limitMaxSize(finalWidth);
        finalHeight = limitMaxSize(finalHeight);

        List<String> args = new ArrayList<String>();

        args.add("from");
        if (cwidth > 0 && cheight > 0) {
            args.add("-crop");
            args.add(cwidth+"x"+cheight+"+"+cleft+"+"+ctop);
        }
        args.add("-resize");
        if (exactFinalSize) args.add(finalWidth+"x"+finalHeight+"!");
        else  args.add(finalWidth+"x"+finalHeight);

        if (imageQuality > 0 && to.getName().toLowerCase().endsWith(".jpg"))
        {
            args.add("-quality");
            args.add(String.valueOf(imageQuality));
        }

        if (from.getAbsolutePath().endsWith(".gif")) args.add("+repage");

        boolean galleryStripExif = Constants.getBoolean("galleryStripExif");

        if (fillColor == null)
        {
            if (galleryStripExif) args.add("-strip");
            args.add("to");
        }
        else
        {
            args.add("-gravity");
            if (ip == 3) {
                args.add("West");
            } else {
                args.add("Center");
            }
            args.add("-background");
            args.add("#"+fillColor);
            args.add("-extent");
            args.add(finalWidth+"x"+finalHeight);
            if (galleryStripExif) args.add("-strip");
            args.add("to");
        }

        return ImageTools.executeImageMagick(from, to, args);
    }

    private static int cropAndResizeJava(IwcmFile from, int cwidth, int cheight, int cleft, int ctop, int finalWidth, int finalHeight, String fillColor, boolean exactFinalSize, IwcmFile to, int imageQuality, int ip)
    {
        try
        {
            IwcmInputStream iwStream = new IwcmInputStream(from.getPath(), false);
            BufferedImage originalBufferedImage = ImageIO.read(iwStream);
            iwStream.close();

            //detect format and transparency support based on file extension
            boolean supportsTransparency = ImageTools.supportsTransparency(from.getName());
            int imageType = ImageTools.getBufferedImageType(from.getName());

            // Logger.println(GalleryDB.class,"w="+w+" h="+h);
            int scaleType = Image.SCALE_AREA_AVERAGING;
            if (originalBufferedImage.getWidth() > 1000)
            {
                Logger.debug(GalleryDB.class, "smooth resize");
                scaleType = Image.SCALE_SMOOTH;
            }

            if (imageQuality >= 90) scaleType = Image.SCALE_SMOOTH;
            else if (imageQuality >= 85) scaleType = Image.SCALE_AREA_AVERAGING;
            else if (imageQuality >= 80) scaleType = Image.SCALE_REPLICATE;
            else if (imageQuality > 0) scaleType = Image.SCALE_FAST;

            if (originalBufferedImage.getWidth() > 2000)
            {
                // Logger.println(GalleryDB.class,"smooth fast");
                // scaleType = Image.SCALE_FAST;
            }
            int resizeWidth = finalWidth;
            int resizeHeight = finalHeight;
            int offsetW = 0;
            int offsetH = 0;

            /*** Ak 3 alebo 4, potrebujem dopocitat velkost obrazku - ostatne sa doplni bielou farbou (ip=3) alebo farbou fillColor ***/
            if(ip == 3 || ip == 4)
            {
                if(finalWidth > finalHeight)
                {
                    double pomer = (double)cwidth / (double) cheight;
                    resizeWidth = (int)Math.round(finalHeight * pomer);
                    if(ip == 4) offsetW = (finalWidth - resizeWidth)/2;	//vycentrovanie obrazka
                }
                else
                {
                    double pomer = (double)cwidth / (double) cheight;
                    resizeHeight = (int)Math.round(finalWidth / pomer);
                    if(ip == 4) offsetH = (finalHeight - resizeHeight)/2;	//vycentrovanie obrazka
                }
            }

            DebugTimer timer = new DebugTimer("GalleryDBTools.cropAndResizeJava");
            timer.diff("starting: " + scaleType + " cleft="+cleft+" ctop="+ctop+" cwidth="+cwidth+" cheight="+cheight);
            Image smallImage = null;

            /*** Orezanie ***/
            BufferedImage bi;
            if (cwidth > 0 && cheight > 0) {
                smallImage = originalBufferedImage.getSubimage(cleft, ctop, cwidth, cheight);
                bi = new BufferedImage(cwidth, cheight, imageType);
                Graphics2D g = bi.createGraphics();
                g.drawImage(smallImage, 0, 0, null);
                g.dispose();
                timer.diff("croped");
            } else {
                bi = originalBufferedImage;
            }

            /*** Zmena velkosti obrazku ***/
            smallImage = bi.getScaledInstance(resizeWidth, resizeHeight, scaleType);
            timer.diff("scaled");
            BufferedImage bufSmall = new BufferedImage(finalWidth, finalHeight, imageType);
            timer.diff("buf created");
            // bufSmall.getGraphics().drawImage(smallImage, 0, 0, null);
            Graphics2D graphics = bufSmall.createGraphics();
            if(Tools.isNotEmpty(fillColor))	//farba vyplne
            {
                graphics.setPaint ( Color.decode("#"+fillColor) );
                graphics.fillRect ( 0, 0, finalWidth, finalHeight );
            }
            else if (supportsTransparency == false)
            {
                graphics.setPaint ( Color.white );
                graphics.fillRect ( 0, 0, finalWidth, finalHeight );
            }
            graphics.drawImage(smallImage, offsetW, offsetH, null);
            timer.diff("image drawed");
            graphics.dispose();
            timer.diff("disposed");

            timer.diff("write start");
            int writeResult = ImageTools.writeImage(bufSmall, from.getName(), to);
            timer.diff("writed to file");
            if (writeResult != 0) return writeResult;

            return (0);
        }
        catch (javax.imageio.IIOException ex)
        {
            sk.iway.iwcm.Logger.error(ex);
            return (4);
        }
        catch (Exception ex)
        {
            sk.iway.iwcm.Logger.error(ex);
            return (2);
        }
    }

    /**
     * Limituje maximalnu velkost obrazku pre resize na hodnotu 5000 aby nedoslo k zahlteniu disku / CPU
     * @param size
     * @return
     */
    public static int limitMaxSize(int size)
    {
        if (size > 5000) return 5000;
        return size;
    }

    /**
     * Pouzi GalleryToolsForCore.getImagePathPrefix
     * @param prefix
     * @param fullPath
     * @return
     * @deprecated
     */
    @Deprecated
    public static String getImagePathPrefix(String prefix, String fullPath) {
        return GalleryToolsForCore.getImagePathPrefix(prefix,fullPath);
    }

    /**
     * Pouzi Pouzi GalleryToolsForCore.getImagePathSmall
     * @param fullPath
     * @return
     * @deprecated
     */
    @Deprecated
    public static String getImagePathSmall(String fullPath) {
        return GalleryToolsForCore.getImagePathSmall(fullPath);
    }

    /**
     * Pouzi Pouzi GalleryToolsForCore.getImagePathNormal
     * @param fullPath
     * @return
     * @deprecated
     */
    @Deprecated
    public static String getImagePathNormal(String fullPath) {
        return GalleryToolsForCore.getImagePathNormal(fullPath);
    }

    /**
     * Pouzi Pouzi GalleryToolsForCore.getImagePathOriginal
     * @param fullPath
     * @return
     * @deprecated
     */
    @Deprecated
    public static String getImagePathOriginal(String fullPath) {
        return GalleryToolsForCore.getImagePathOriginal(fullPath);
    }
}
