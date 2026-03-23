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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;

import sk.iway.Password;
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
		return ext.equalsIgnoreCase("jpg") || ext.equalsIgnoreCase("jpeg") || ext.equalsIgnoreCase("gif") || ext.equalsIgnoreCase("png") || ext.equalsIgnoreCase("svg") || ext.equalsIgnoreCase("webp") || ext.equalsIgnoreCase("bmp");
	}

	/**
	 * Returns true if fileName is image file and can be resized (ends with .jpg, png... but not .svg)
	 * @param fileName
	 * @return
	 */
	public static boolean isResizableImage(String fileName)
	{
		return isImage(fileName) && fileName.toLowerCase().endsWith(".svg") == false;
	}

	public static int resizeImage(IwcmFile imageFile, int width, int height)
	{
		try
		{
			String srcUrlLC  = imageFile.getName().toLowerCase();

			if (imageFile.isFile() && isResizableImage(srcUrlLC))
			{
				if (existsImageMagickConvertCommand()){
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

		List<String> args = new ArrayList<String>();
		args.add("from");
		args.add("-resize");
		args.add(width+"x"+height+"!");
		args.add("to");
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
				if (existsImageMagickConvertCommand()){
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

		List<String> args = new ArrayList<String>();
		args.add("from");
		args.add("-crop");
		args.add(width+"x"+height+"+"+startX+"+"+startY);
		args.add("to");
		return executeImageMagick(imageFile, args);
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
				if (existsImageMagickConvertCommand()){
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

		List<String> args = new ArrayList<String>();
		args.add("from");
		args.add("-rotate");
		args.add(Double.toString(angle));
		args.add("to");
		return executeImageMagick(imageFile, args);
	}

	/**
	 * Executes ImageMagick command. Handles IwcmFSDB storage, if image is stored in DB, it is first written to disk, then processed and finally written back to DB.
	 * @param imageFile
	 * @param args - list of arguments, replaces from and to placeholders with real file paths, if they are present in args
	 * @return
	 */
	public static int executeImageMagick(IwcmFile imageFile, List<String> args)
	{
		return executeImageMagick(imageFile, imageFile, args);
	}

	/**
	 * Executes ImageMagick command. Handles IwcmFSDB storage, if image is stored in DB, it is first written to disk, then processed and finally written back to DB.
	 * @param from
	 * @param to
	 * @param args - list of arguments, replaces from and to placeholders with real file paths, if they are present in args
	 * @return
	 */
	public static int executeImageMagick(IwcmFile from, IwcmFile to, List<String> args) {
		//args can contains placeholders for from and to file paths because of IwcmFSDB storage, so we need to find out where they are
		int fromIndex = args.indexOf("from");
		int toIndex = args.indexOf("to");

		if (IwcmFsDB.useDBStorage(from.getVirtualPath()))
		{
			try
			{
				String temporaryFrom = IwcmFsDB.getTempFilePath(from.getPath());
				String temporaryTo = IwcmFsDB.getTempFilePath(getTempFilePath(to.getPath()));

				//write from file to temp file disk
				IwcmFsDB.writeFileToDisk(new File(from.getAbsolutePath()), new File(temporaryFrom), true);

				if (fromIndex != -1) args.set(fromIndex, temporaryFrom);
				if (toIndex != -1) args.set(toIndex, temporaryTo);

				int result = executeImageMagickCommand(args);
				if (result == 0)
				{
					IwcmFsDB.writeFileToDB(new File(temporaryTo), new File(to.getAbsolutePath()));
					if (new File(temporaryFrom).delete() && new File(temporaryTo).delete())
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
			//usualy from and to is same file, we need temp to file and then replace to with this temp file
			IwcmFile toTemp = new IwcmFile(getTempFilePath(to.getAbsolutePath()));
			if (fromIndex != -1) args.set(fromIndex, from.getAbsolutePath());
			if (toIndex != -1) args.set(toIndex, toTemp.getAbsolutePath());
			int result = executeImageMagickCommand(args);
			if (result == 0)
			{
				FileTools.copyFile(toTemp, to);
				return toTemp.delete() ? 0 : -1;
			}
		}
		return -1;
	}

	/**
	 * Returns temporary file path for given path. If path is /path/to/file.jpg, temp file path will be /path/to/file.tmp-timestamp-random.jpg
	 * @param path
	 * @return
	 */
	private static String getTempFilePath(String path) {
		int dotIndex = path.lastIndexOf('.');
		String append = ".tmp-" + System.currentTimeMillis() + "-" + Password.generatePassword(5);
		if (dotIndex != -1)
		{
			return path.substring(0, dotIndex) + append + path.substring(dotIndex);
		}
		else
		{
			return path + append;
		}
	}

	/**
	 * Executes ImageMagick command with given parameters.
	 * Automatically add path to ImageMagick runtime file as first parameter.
	 * @param args
	 */
	private static int executeImageMagickCommand(List<String> args)
	{
		args.add(0, getImageMagicDir()+File.separatorChar+getRuntimeFile());
		Runtime rt = Runtime.getRuntime();
		Process process = null;
		try
		{
			//inject additional params based on file type and operation
			addImageMagickCustomParams(args);

			StringBuilder params = new StringBuilder();
			for (int i = 0; i < args.size(); i++)
			{
				params.append(' ').append(args.get(i));
			}
			Logger.debug(ImageTools.class, "LONGCMD:\n" + params);

			process= rt.exec(args.toArray(new String[0]));

			InputStream stderr = process.getErrorStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(stderr, Constants.FILE_ENCODING));
			String line = null;
			while ((line = br.readLine()) != null)
			{
				Logger.debug(ImageTools.class, line);
			}
			br.close();

			int ret = process.waitFor();
			//convert vrati 1 namiesto 0 lebo pri -debug hlasi, ze destination subor neexistuje/nepozna .new priponu
			if (ret==1) ret = 0;

			return ret;
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return -1;
	}

	private static void addImageMagickCustomParams(List<String> args)
	{
		String operation = null;
		for (String arg : args) {
			if (arg.startsWith("-resize") || arg.startsWith("-crop") || arg.startsWith("-rotate")) {
				operation = arg.substring(1);
				break;
			}
		}

		if (operation == null) {
			return;
		}

		String ext = "unknown";
		//iterate params, detect image type and check isImageType() method
		for (String arg : args) {
			if (arg.contains(".")) {
				String fileExt = FileTools.getFileExtension(arg);
				if (isImage("file." + fileExt)) {
					ext = fileExt;
					break;
				}
			}
		}
		if ("jpeg".equalsIgnoreCase(ext)) {
			ext = "jpg";
		}

		//check custom params by Constants key imageMagickCustomParams_[mode]_[ext], for example imageMagickCustomParams_resize_jpg, then imageMagickCustomParams_resize, then imageMagickCustomParams_jpg then imageMagickCustomParams
		String customParamsKey = "imageMagickCustomParams_" + operation + "_" + ext;
		String customParams = Constants.getString(customParamsKey);
		if (Tools.isEmpty(customParams)) {
			customParamsKey = "imageMagickCustomParams_" + operation;
			customParams = Constants.getString(customParamsKey);

			if (Tools.isEmpty(customParams)) {
				customParamsKey = "imageMagickCustomParams";
				customParams = Constants.getString(customParamsKey);
			}

			customParamsKey = "imageMagickCustomParams_" + ext;
			String customParamsExt = Constants.getString(customParamsKey);
			if (Tools.isNotEmpty(customParamsExt)) {
				if (Tools.isNotEmpty(customParams)) {
					customParams += " " + customParamsExt;
				}
				else {
					customParams = customParamsExt;
				}
			}
		}

		if (Tools.isNotEmpty(customParams)) {
			String[] customParamsArray = Tools.getTokens(customParams, " ", true);

			//if contains compressionLevel remove -quality xx param
			for (String customParam : customParamsArray) {
				if (customParam.contains("compression-level") || customParam.contains("quality")) {
					//remove -quality xx parameter
					for (int i = 0; i < args.size(); i++) {
						if ("-quality".equals(args.get(i))) {
							args.remove(i); //remove -quality
							if (i < args.size()) {
								args.remove(i); //remove quality value
							}
							break;
						}
					}
				}
			}

			int counter = 0;
			for (String customParam : customParamsArray) {
				//we need to add custom params after from file path like: convert file.jpg [custom params] -resize 100x100! file.jpg
				args.add(2 + counter, customParam);
				counter++;
			}
		}
	}

	/**
	 * Returns ImageMagick runtime file name based on current Operating System. magick/magick.exe
	 * If fallback to v6 convert/convert.exe if magick/magick.exe does not exist.
	 * @return - image magick runtime file by current Operating System
	 */
	private static String getRuntimeFile()
	{
		String runtimeFile = "magick";
		if (System.getProperty("os.name").indexOf("Windows") != -1)
		{
			runtimeFile  = "magick.exe";
		}
		File f = new File(getImageMagicDir() + File.separatorChar + runtimeFile);
		if (f.exists() == false) {
			//falback to ld v6 convert command
			runtimeFile = "convert";
			if (System.getProperty("os.name").indexOf("Windows") != -1)
			{
				runtimeFile  = "convert.exe";
			}
		}
		return runtimeFile;
	}

	private static String getImageMagicDir()
	{
		return Constants.getString("imageMagickDir");
	}

	/**
	 * Checks if ImageMagick convert command exists in configured directory.
	 * @return
	 */
	public static boolean existsImageMagickConvertCommand()
	{
		String imageMagickDir = getImageMagicDir();
		boolean convertExists = false;
		String runtimeFile = getRuntimeFile();

		if (Tools.isNotEmpty(imageMagickDir))
		{
			File f = new File(imageMagickDir + File.separatorChar + runtimeFile);
			if (f.exists() && f.canRead())
			{
				convertExists = true;
			}
		}
		return convertExists;
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

		String imageMagickDir = getImageMagicDir();
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
				Logger.debug(ImageTools.class, "executing image magick: " + imageMagickDir + File.separatorChar + identifyFile);
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
					Logger.debug(ImageTools.class, "LONGCMD:\n" + params);
					proc = rt.exec(args);
					stderr = proc.getInputStream();
					br = new BufferedReader(new InputStreamReader(stderr, Constants.FILE_ENCODING));
					while ((line = br.readLine()) != null)
					{
						Logger.debug(ImageTools.class, line);
					}
					br.close();
					int exitValue = proc.waitFor();
					if (IwcmFsDB.useDBStorage(IwcmFsDB.getVirtualPath(filePath)))
					{
						IwcmFsDB.writeFileToDB(new File(IwcmFsDB.getTempFilePath(IwcmFsDB.getVirtualPath(filePath))), new File(IwcmFsDB.getVirtualPath(filePath)));
						new File(IwcmFsDB.getTempFilePath(IwcmFsDB.getVirtualPath(filePath))).delete();
					}
					Logger.debug(ImageTools.class, "ExitValue: " + exitValue);
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

	/**
	 * Convert image to target format (jpg, png, gif).
	 * If image is already in target format, no conversion is done and null is returned.
	 * @param imageFile
	 * @param targetFormat
	 * @return - new file name (without path) if conversion was done, null otherwise
	 */
	public static String convertImageFormat(IwcmFile imageFile, String targetFormat)
	{
		String imageFileExt = FileTools.getFileExtension(imageFile.getName());
		if (imageFileExt.equalsIgnoreCase(targetFormat)) {
			return null;
		}
		if ("jpeg".equalsIgnoreCase(targetFormat)) {
			targetFormat = "jpg";
		}

		try {
			// Implement the conversion logic using ImageIO or any other library
			IwcmInputStream is = new IwcmInputStream(imageFile);
			BufferedImage image = ImageIO.read(is);
			BufferedImage convertedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
			convertedImage.createGraphics().drawImage(image, 0, 0, null);

			String newFileName = FileTools.getFileNameWithoutExtension(imageFile.getName()) + "." + targetFormat;
			IwcmFile newImageFile = new IwcmFile(imageFile.getParent(), newFileName);
			if (newImageFile.exists()) {
				newImageFile.delete();
			}

			IwcmOutputStream iwos = new IwcmOutputStream(newImageFile);
			ImageOutputStream ios = ImageIO.createImageOutputStream(iwos);
			ImageIO.write(convertedImage, targetFormat, ios);

			ios.flush();
			ios.close();
			iwos.close();
			is.close();

			return newImageFile.getName();
		}
		catch (Exception ex) {
			sk.iway.iwcm.Logger.error(ex);
		}
		return null;
	}
}
