package sk.iway.iwcm.common;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DebugTimer;
import sk.iway.iwcm.gallery.GalleryDB;
import sk.iway.iwcm.gallery.ImageInfo;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmInputStream;
import sk.iway.iwcm.io.IwcmOutputStream;

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
        String imageMagickDir = GalleryDB.getImageMagicDir();
        // mame ho aj dostupny
        boolean convertExists = false;
        String runtimeFile = GalleryDB.getRuntimeFile();

        if (Tools.isNotEmpty(imageMagickDir))
        {
            File f = new File(imageMagickDir + File.separatorChar + runtimeFile);
            if (f.exists() && f.canRead())
            {
                convertExists = true;
            }
        }

        if (Tools.isNotEmpty(imageMagickDir) && convertExists)
        {

            return cropAndResizeImageMagick(from, cwidth, cheight, cleft, ctop, finalWidth, finalHeight, fillColor, exactFinalSize, to, imageQuality);
        }
        else
        {
            return cropAndResizeJava(from, cwidth, cheight, cleft, ctop, finalWidth, finalHeight, fillColor, exactFinalSize, to, imageQuality, ip);
        }
    }

    private static int cropAndResizeImageMagick(IwcmFile from, int cwidth, int cheight, int cleft, int ctop, int finalWidth, int finalHeight, String fillColor, boolean exactFinalSize, IwcmFile to, int imageQuality)
    {
        finalWidth = limitMaxSize(finalWidth);
        finalHeight = limitMaxSize(finalHeight);

        List<String> args = new ArrayList<String>();

        args.add(""); //toto sa neskor nahradi za convert prikaz v executeImageMagickCommand
        args.add(from.getAbsolutePath());
        args.add("-crop");
        args.add(cwidth+"x"+cheight+"+"+cleft+"+"+ctop);
        args.add("-resize");
        if (exactFinalSize) args.add(finalWidth+"x"+finalHeight+"!");
        else  args.add(finalWidth+"x"+finalHeight);

        if (imageQuality > 0 && to.getName().toLowerCase().endsWith(".jpg"))
        {
            args.add("-quality");
            args.add(String.valueOf(imageQuality));
        }

        if (from.getAbsolutePath().endsWith(".gif")) args.add("+repage");

        if (fillColor == null)
        {
            args.add("-strip");
            args.add(to.getAbsolutePath());
        }
        else
        {
            args.add("-gravity");
            args.add("center");
            args.add("-background");
            args.add("#"+fillColor);
            args.add("-extent");
            args.add(finalWidth+"x"+finalHeight);
            args.add("-strip");
            args.add(to.getAbsolutePath());
        }

        return GalleryDB.executeImageMagickCommand(args.toArray(new String[0]));
    }

    private static int cropAndResizeJava(IwcmFile from, int cwidth, int cheight, int cleft, int ctop, int finalWidth, int finalHeight, String fillColor, boolean exactFinalSize, IwcmFile to, int imageQuality, int ip)
    {
        try
        {
            IwcmInputStream iwStream = new IwcmInputStream(from.getPath(), false);
            BufferedImage originalBufferedImage = ImageIO.read(iwStream);
            iwStream.close();
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

            DebugTimer timer = new DebugTimer("ImageResize");
            timer.diff("starting: " + scaleType + " cleft="+cleft+" ctop="+ctop+" cwidth="+cwidth+" cheight="+cheight);
            Image smallImage = null;

            /*** Orezanie ***/
            smallImage = originalBufferedImage.getSubimage(cleft, ctop, cwidth, cheight);
            BufferedImage bi = new BufferedImage(cwidth, cheight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = bi.createGraphics();
            g.drawImage(smallImage, 0, 0, null);
            timer.diff("croped");

            /*** Zmena velkosti obrazku ***/
            smallImage = bi.getScaledInstance(resizeWidth, resizeHeight, scaleType);
            timer.diff("scaled");
            BufferedImage bufSmall = new BufferedImage(finalWidth, finalHeight, BufferedImage.TYPE_INT_RGB);
            timer.diff("buf created");
            // bufSmall.getGraphics().drawImage(smallImage, 0, 0, null);
            Graphics2D graphics = bufSmall.createGraphics();
            if(Tools.isNotEmpty(fillColor))	//farba vyplne
            {
                graphics.setPaint ( Color.decode("#"+fillColor) );
                graphics.fillRect ( 0, 0, finalWidth, finalHeight );
            }
            else
            {
                graphics.setPaint ( Color.white );
                graphics.fillRect ( 0, 0, finalWidth, finalHeight );
            }
            graphics.drawImage(smallImage, offsetW, offsetH, null);
            timer.diff("image drawed");
            bufSmall.getGraphics().dispose();
            timer.diff("disposed");

            ImageWriteParam iwparam = null;
            // ImageIO.write(bufSmall, format,iwparam, fSmallImg);
            // Jimi.putImage("image/" + type, image, realPathSmall);
            ImageWriter writer = null;

            String format = "jpg";
            if (from.getAbsolutePath().endsWith(".png"))
            {
                format = "png";
            }
            else
            {
                iwparam = new JPEGImageWriteParam(null);
                iwparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                iwparam.setCompressionQuality(0.85F);
            }

            Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName(format);
            if (iter.hasNext())
            {
                writer = iter.next();
            }
            if (writer != null) {
                // Prepare output file
                IwcmOutputStream out = new IwcmOutputStream(to.getPath());
                ImageOutputStream ios = ImageIO.createImageOutputStream(out);
                writer.setOutput(ios);
                timer.diff("write start");
                // Write the image
                writer.write(null, new IIOImage(bufSmall, null, null), iwparam);
                timer.diff("writed to file");
                // Cleanup
                ios.flush();
                writer.dispose();
                ios.close();
                out.close();
            }
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
