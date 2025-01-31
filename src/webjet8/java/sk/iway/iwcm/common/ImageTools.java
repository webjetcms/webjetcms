package sk.iway.iwcm.common;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;

import org.apache.commons.lang.ArrayUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.gallery.GalleryDB;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmFsDB;
import sk.iway.iwcm.io.IwcmInputStream;
import sk.iway.iwcm.io.IwcmOutputStream;

/**
 *  Manipulacia s obrazkami
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision$
 *@created      Utorok, 2003, marec 30
 *@modified     $Date$
 */
public class ImageTools
{
	private ImageTools() {

	}

	/**
	 * Returns true if fileName is image file (ends with .jpg, png...)
	 * @param fileName
	 * @return
	 */
	public static boolean isImage(String fileName)
	{
		String ext = FileTools.getFileExtension(fileName);
		return ext.equalsIgnoreCase("jpg") || ext.equalsIgnoreCase("jpeg") || ext.equalsIgnoreCase("gif") || ext.equalsIgnoreCase("png") || ext.equalsIgnoreCase("svg");
	}

	public static int resizeImage(IwcmFile imageFile, int width, int height)
	{
		try
		{
			String srcUrlLC  = imageFile.getName().toLowerCase();

			if (imageFile.isFile() && (srcUrlLC.endsWith(".jpg") || srcUrlLC.endsWith(".jpeg") || srcUrlLC.endsWith(".gif") || srcUrlLC.endsWith(".png")))
			{
				if (GalleryDB.existsImageMagickConvertCommand()){
					return resizeImageImageMagick(imageFile,width,height);
				}
				else{
					return resizeImageGraphics2D(imageFile, width, height);
				}
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return(3);

	}

	/**
	 * @param imageFile
	 * @param width
	 * @param height
	 * @return
	 */
	private static int resizeImageImageMagick(IwcmFile imageFile, int width, int height)
	{
		GalleryDB.stripExif(imageFile.getAbsolutePath());

		String[] args = new String[]{"imagemagick","from","-resize",width+"x"+height+"!","to"};
		return executeImageMagick(imageFile, args);
	}

	/**
	 * @param imageFile
	 * @param width
	 * @param height
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private static int resizeImageGraphics2D(IwcmFile imageFile, int width, int height) throws IOException, FileNotFoundException
	{
		//opener = new Opener();
		//imp = opener.openImage(realPath);
		BufferedImage originalImage = ImageIO.read(new IwcmInputStream(imageFile));

		double scaleFactor = (width * 1.0) / (originalImage.getWidth() * 1.0);
		double scaleFactor2 = (height * 1.0) / (originalImage.getHeight() * 1.0);

		int w = (int) (originalImage.getWidth() * scaleFactor);
		int h = (int) (originalImage.getHeight() * scaleFactor2);

		Image resizedImage = originalImage.getScaledInstance(w, h, Image.SCALE_AREA_AVERAGING);
		BufferedImage bufSmall = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		bufSmall.getGraphics().drawImage(resizedImage, 0, 0, null);
		bufSmall.getGraphics().dispose();


		try
		{
			ImageWriteParam iwparam = new JPEGImageWriteParam(null);
			iwparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT) ;
			iwparam.setCompressionQuality(0.85F);

			//ImageIO.write(bufSmall, format, iwparam, fSmallImg);
			//Jimi.putImage("image/" + type, image, realPathSmall);

			ImageWriter writer = null;
			@SuppressWarnings("rawtypes")
			Iterator iter = ImageIO.getImageWritersByFormatName("jpg");
			if (iter.hasNext()) {
				 writer = (ImageWriter)iter.next();
			}

			if (writer != null) {
				if (imageFile.exists())
				{
					imageFile.delete();
				}

				// Prepare output file
				IwcmOutputStream iwos = new IwcmOutputStream(imageFile);
				ImageOutputStream ios = ImageIO.createImageOutputStream(iwos);
				writer.setOutput(ios);

				// Write the image
				writer.write(null, new IIOImage(bufSmall, null, null), iwparam);

				// Cleanup
				ios.flush();
				writer.dispose();
				ios.close();
				iwos.close();
			}

			return(0);
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
			return(2);
		}
	}

	/**
	 * Resizne obrazok, vrati:
	 * 0-vsetko je OK
	 * 1-obrazok je uz teraz mensi
	 * 2-nepodarilo sa ulozit obrazok
	 * 3-nastala neznama chyba
	 * 4-obrazok je v nepodporovanom formate
	 *
	 * @param srcUrl - url adresa obrazku
	 * @param width  - sirka
	 * @param height - vyska
	 * @return
	 */
	public static int resizeImage(String srcUrl, int width, int height)
	{
		IwcmFile imageFile = new IwcmFile(Tools.getRealPath(srcUrl));
		return resizeImage(imageFile, width, height);
	}

	public static int cropImage(IwcmFile imageFile, int width, int height, int startX, int startY)
	{
		try
		{
			String srcUrlLC = imageFile.getName().toLowerCase();

			if (imageFile.isFile() && (srcUrlLC.endsWith(".jpg") || srcUrlLC.endsWith(".jpeg") || srcUrlLC.endsWith(".gif") || srcUrlLC.endsWith(".png")))
			{
				if (GalleryDB.existsImageMagickConvertCommand()){
					return cropImageImageMagick(imageFile, width, height, startX, startY);
				}
				else{
					return cropImageGraphics2D(imageFile, width, height, startX, startY);
				}
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return(3);

	}

	/**
	 * @param imageFile
	 * @param width
	 * @param height
	 * @param startX
	 * @param startY
	 * @return
	 */
	private static int cropImageImageMagick(IwcmFile imageFile, int width, int height, int startX, int startY)
	{
		GalleryDB.stripExif(imageFile.getAbsolutePath());

		String[] args = new String[]{"imagemagick", "from", "-crop", width+"x"+height+"+"+startX+"+"+startY, "to"};
		return executeImageMagick(imageFile,args);
	}

	/**
	 * @param imageFile
	 * @param width
	 * @param height
	 * @param startX
	 * @param startY
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private static int cropImageGraphics2D(IwcmFile imageFile, int width, int height, int startX, int startY) throws IOException,
				FileNotFoundException
	{
		//double scaleFactor = 0.3;
		//double scaleFactor2 = 0.3;

		BufferedImage originalImage = ImageIO.read(new IwcmInputStream(imageFile));

		Image cropedImage = originalImage.getSubimage(startX, startY, width, height);

		BufferedImage bufSmall = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		bufSmall.getGraphics().drawImage(cropedImage, 0, 0, null);
		bufSmall.getGraphics().dispose();

		try
		{
			ImageWriteParam iwparam = new JPEGImageWriteParam(null);
			iwparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT) ;
			iwparam.setCompressionQuality(0.85F);

			ImageWriter writer = null;
			@SuppressWarnings("rawtypes")
			Iterator iter = ImageIO.getImageWritersByFormatName("jpg");
			if (iter.hasNext()) {
				 writer = (ImageWriter)iter.next();
			}
			if (writer != null) {
				// Prepare output file
				IwcmOutputStream iwos = new IwcmOutputStream(imageFile);
				ImageOutputStream ios = ImageIO.createImageOutputStream(iwos);
				writer.setOutput(ios);

				// Write the image
				writer.write(null, new IIOImage(bufSmall, null, null), iwparam);

				// Cleanup
				ios.flush();
				writer.dispose();
				ios.close();
				iwos.close();
			}

			return(0);
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
			return(2);
		}
	}
	public static int cropImage(String srcUrl, int width, int height, int startX, int startY)
	{
		IwcmFile imageFile = new IwcmFile(Tools.getRealPath(srcUrl));
		return  cropImage(imageFile, width, height, startX, startY);
	}

	public static int rotateImage(IwcmFile imageFile, double angle)
	{

		try
		{
			String srcUrlLC = imageFile.toString().toLowerCase();

			if (imageFile.isFile() && (srcUrlLC.endsWith(".jpg") || srcUrlLC.endsWith(".jpeg") || srcUrlLC.endsWith(".gif") || srcUrlLC.endsWith(".png")))
			{
				if (GalleryDB.existsImageMagickConvertCommand()){
					return rotateImageImageMagick(imageFile, angle);
				}else{
					return rotateImageGraphics2D(imageFile, angle);
				}

			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return(3);

	}


	/**
	 * @param imageFile
	 * @param angle
	 * @return
	 */
	private static int rotateImageImageMagick(IwcmFile imageFile, double angle)
	{
		GalleryDB.stripExif(imageFile.getAbsolutePath());

		String[] args = new String[]{"imagemagick", "from", "-rotate", Double.toString(angle), "to"};
		return executeImageMagick(imageFile,args);
	}

	/**
	 * @param imageFile
	 * @param angle
	 * @param args
	 */
	private static int executeImageMagick(IwcmFile imageFile, String[] args)
	{
		int fromIndex = ArrayUtils.indexOf(args, "from");
		int toIndex = ArrayUtils.indexOf(args, "to");
		if (fromIndex < 0 || toIndex < 0)
		{
			throw new IllegalArgumentException("String argument array parameter must contain 'from' and 'to' ");
		}
		if (IwcmFsDB.useDBStorage(imageFile.getVirtualPath()))
		{
			try
			{
				String temporaryOriginal = IwcmFsDB.getTempFilePath(imageFile.getPath());
				String temporaryProcessed = IwcmFsDB.getTempFilePath(imageFile.getPath() + ".new");
				File temporaryProcessedFile = new File(temporaryProcessed);
				File imageFileFile = new File(imageFile.getAbsolutePath());
				IwcmFsDB.writeFileToDisk(imageFileFile, temporaryProcessedFile);
				args[fromIndex] = temporaryOriginal;
				args[toIndex] = temporaryProcessed;
				int result = GalleryDB.executeImageMagickCommand(args);
				if (result == 0)
				{
					IwcmFsDB.writeFileToDB(temporaryProcessedFile, imageFileFile);
					if (new File(temporaryOriginal).delete() && temporaryProcessedFile.delete())
					{
						return 0;
					}
				}
				else
				{
					return result;
				}
			}
			catch (IOException e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}
		else
		{
			IwcmFile processed = new IwcmFile(imageFile.getAbsolutePath() + ".new");
			args[fromIndex] = imageFile.getAbsolutePath();
			args[toIndex] = processed.getAbsolutePath();
			int result = GalleryDB.executeImageMagickCommand(args);
			if (result == 0)
			{
				FileTools.copyFile(processed, imageFile);
				return processed.delete() ? 0 : -1;
			}
		}
		return -1;
	}

	/**
	 * @param imageFile
	 * @param angle
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private static int rotateImageGraphics2D(IwcmFile imageFile, double angle) throws IOException, FileNotFoundException
	{
		BufferedImage originalImage = ImageIO.read(new IwcmInputStream(imageFile));

		int w = originalImage.getWidth();
		int h = originalImage.getHeight();

		//prepocet na radiany
		double angleRad = angle*(Math.PI*2)/360;

		double sin = Math.abs(Math.sin(angleRad));
		double cos = Math.abs(Math.cos(angleRad));

		int neww = (int)Math.floor(w*cos+h*sin);
		int newh = (int)Math.floor(h*cos+w*sin);


		//debug
/*	Logger.println(this,"*****************************************************");
		Logger.println(this,"image width= " +w+ "\t image height= " +h);
		Logger.println(this,"angle= "+angle+ "\t\t angleRad= " +angleRad);
		Logger.println(this,"sin= " +sin+ "\t\t cos= " +cos);
		Logger.println(this,"new width= " +neww+ "\t\t new height= " +newh);
		Logger.println(this,"*****************************************************");
*/


		//int transparency = originalImage.getColorModel().getTransparency();

		/*GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gd = ge.getDefaultScreenDevice();
		GraphicsConfiguration gc = gd.getDefaultConfiguration();

		BufferedImage result = gc.createCompatibleImage(neww, newh); //, transparency);*/

		//BufferedImage result = originalImage.getScaledInstance(neww, newh, )
		BufferedImage result = new BufferedImage(neww, newh, BufferedImage.TYPE_INT_RGB);

		Graphics2D g = result.createGraphics();
		g.translate((neww-w)/(double)2, (newh-h)/(double)2);
		g.rotate(angleRad, w/(double)2, h/(double)2);
		g.drawRenderedImage(originalImage, null);

		Image rotatedImage = result.getSubimage(0, 0, result.getWidth(), result.getHeight());

		BufferedImage bufSmall = new BufferedImage(neww, newh, BufferedImage.TYPE_INT_RGB);
		bufSmall.getGraphics().drawImage(rotatedImage, 0, 0, null);
		bufSmall.getGraphics().dispose();

		try
		{
			ImageWriteParam iwparam = new JPEGImageWriteParam(null);
			iwparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT) ;
			iwparam.setCompressionQuality(0.85F);

			//ImageIO.write(bufSmall, format, iwparam, fSmallImg);
			//Jimi.putImage("image/" + type, image, realPathSmall);

			ImageWriter writer = null;
			@SuppressWarnings("rawtypes")
			Iterator iter = ImageIO.getImageWritersByFormatName("jpg");
			if (iter.hasNext()) {
				 writer = (ImageWriter)iter.next();
			}
			if (writer != null) {
				// Prepare output file
				IwcmOutputStream iwos = new IwcmOutputStream(imageFile);
				ImageOutputStream ios = ImageIO.createImageOutputStream(iwos);
				writer.setOutput(ios);

				// Write the image
				writer.write(null, new IIOImage(bufSmall, null, null), iwparam);

				// Cleanup
				ios.flush();
				writer.dispose();
				ios.close();
				iwos.close();
			}

			return(0);
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
			return(2);
		}
	}

	public static int rotateImage(String srcUrl, double angle)
	{
		return rotateImage(new IwcmFile(Tools.getRealPath(srcUrl)), angle);
	}

	/**
	 * Skonvertuje fotku do RGB ak je v CMYK, ak je konverzia povolena.
	 *
	 * @param filePath
	 */
	public static void convertCmykToRgb(String filePath)
	{
		if (Constants.getBoolean("galleryConvertCmykToRgb") == false)
			return;

		String imageMagickDir = GalleryDB.getImageMagicDir();
		boolean isCmyk = false;
		boolean identifyExist = false;
		boolean convertExists = false;
		String identifyFile = getIdentifyRuntimeFile();
		String runtimeFile = getMogrifyRuntimeFile();

		try
		{
			if (Tools.isNotEmpty(imageMagickDir))
			{
				//overime si ci existuje convert a identify
				File f = new File(imageMagickDir + File.separatorChar + identifyFile);
				if (f.exists() && f.canRead())
					identifyExist = true;
				else
					Logger.debug(ImageTools.class, "identify command not found");
				File fi = new File(imageMagickDir + File.separatorChar + runtimeFile);
				if (fi.exists() && fi.canRead())
					convertExists = true;
				else
					Logger.debug(ImageTools.class, "converter command not found");
			}

			if (Tools.isNotEmpty(imageMagickDir) && convertExists && identifyExist && Tools.isNotEmpty(Constants.getString("galleryConvertCmykToRgbInputProfilePath")) && Tools.isNotEmpty(Constants.getString("galleryConvertCmykToRgbOutputProfilePath")))
			{
				Logger.debug(GalleryDB.class, "executing image magick: " + imageMagickDir + File.separatorChar + identifyFile);
				Runtime rt = Runtime.getRuntime();

				String[] args = new String[3];




				args[0] = imageMagickDir + File.separatorChar + identifyFile;
				args[1] = "-verbose";
				args[2] = filePath;

				Process proc = rt.exec(args);
				InputStream stderr = proc.getInputStream();//getErrorStream();
				BufferedReader br = new BufferedReader(new InputStreamReader(stderr, Constants.FILE_ENCODING));
				String line = null;
				while ((line = br.readLine()) != null)
				{
					if (line.indexOf("CMYK")>-1)
					{
						isCmyk = true;
						break;
					}
				}
				br.close();

				//ak uploadovany obrazok je v CMYKu, skonvertujeme ho...
				if (isCmyk)
				{
					args = new String[11];

					if (IwcmFsDB.useDBStorage(IwcmFsDB.getVirtualPath(filePath)))
					{
						IwcmFsDB.writeFileToDisk(new File(filePath),new File(IwcmFsDB.getTempFilePath(filePath)));
						args[0] = imageMagickDir + File.separatorChar + runtimeFile;
						args[1] = "-profile";
						args[2] = Constants.getString("galleryConvertCmykToRgbInputProfilePath");
						args[3] = "-profile";
						args[4] = Constants.getString("galleryConvertCmykToRgbOutputProfilePath");
						args[5]	= "-set";
						args[6] = "colorspace";
						args[7] = "sRGB";
						args[8] = "-quality";
						args[9] = "85";
						args[10] = IwcmFsDB.getTempFilePath(filePath);
					}
					else
					{
						args[0] = imageMagickDir + File.separatorChar + runtimeFile;
						args[1] = "-profile";
						args[2] = Constants.getString("galleryConvertCmykToRgbInputProfilePath");
						args[3] = "-profile";
						args[4] = Constants.getString("galleryConvertCmykToRgbOutputProfilePath");
						args[5]	= "-set";
						args[6] = "colorspace";
						args[7] = "sRGB";
						args[8] = "-quality";
						args[9] = "85";
						args[10] = filePath;
					}

					StringBuilder params = new StringBuilder();
					for (int i = 0; i < args.length; i++)
					{
						params.append(' ').append(args[i]);
					}
					Logger.debug(GalleryDB.class, "LONGCMD:\n" + params);
					proc = rt.exec(args);
					stderr = proc.getInputStream();
					br = new BufferedReader(new InputStreamReader(stderr, Constants.FILE_ENCODING));
					while ((line = br.readLine()) != null)
					{
						Logger.debug(GalleryDB.class, line);
					}
					br.close();
					int exitValue = proc.waitFor();
					if (IwcmFsDB.useDBStorage(IwcmFsDB.getVirtualPath(filePath)))
					{
						IwcmFsDB.writeFileToDB(new File(IwcmFsDB.getTempFilePath(IwcmFsDB.getVirtualPath(filePath))), new File(IwcmFsDB.getVirtualPath(filePath)));
						new File(IwcmFsDB.getTempFilePath(IwcmFsDB.getVirtualPath(filePath))).delete();
					}
					Logger.debug(GalleryDB.class, "ExitValue: " + exitValue);
				}
			}

		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
	}

	/**
	 * ziska prikaz programu Identify (sucast Imagemagick)
	 *
	 * @return
	 */
	public static String getIdentifyRuntimeFile()
	{
		String result = "identify";
		if (System.getProperty("os.name").indexOf("Windows") != -1)
		{
			result  = "identify.exe";
		}
		return result ;
	}

	/**
	 * prikaz mogrify funguje rovnako ako convert ale ma ako parameter iba jednu cestu, prepisuje povodny vstupny subor
	 * http://www.imagemagick.org/www/mogrify.html
	 *
	 * @return
	 */
	public static String getMogrifyRuntimeFile()
	{
		String result = "mogrify";
		if (System.getProperty("os.name").indexOf("Windows") != -1)
		{
			result  = "mogrify.exe";
		}
		return result ;
	}
}
