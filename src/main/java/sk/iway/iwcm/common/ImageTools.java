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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;


import com.luciad.imageio.webp.CompressionType;
import com.luciad.imageio.webp.WebPImageWriterSpi;
import com.luciad.imageio.webp.WebPWriteParam;

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
	/** Image formats recognized by WebJET and their processing capabilities. */
	private record ImageFormat(String extension, boolean transparency, boolean resizable) {}

	private static final List<ImageFormat> IMAGE_FORMATS = List.of(
		new ImageFormat("jpg", false, true),
		new ImageFormat("jpeg", false, true),
		new ImageFormat("gif", true, true),
		new ImageFormat("png", true, true),
		new ImageFormat("svg", false, false),
		new ImageFormat("webp", true, true),
		new ImageFormat("bmp", false, true),
		new ImageFormat("avif", true, true)
	);

	private ImageTools() {

	}

	/**
	 * Maps image-processing results to shared translation keys, regardless of the image format.
	 * @param result 0 for success, 1 for an image already smaller than requested, 2 for a save
	 *               failure, 3 for a processing failure, 4 (or legacy -4) for unreadable/unsupported input
	 * @return translation key, or null for success; unknown codes use the processing-failure message
	 */
	public static String getErrorKey(int result) {
		return switch (result) {
			case 0 -> null;
			case 1 -> "gallery.resizing.error_1";
			case 2 -> "gallery.resizing.error_2";
			case 4, -4 -> "gallery.resizing.error_4";
			default -> "gallery.resizing.error_3";
		};
	}

	/**
	 * Returns true if WebJET preserves transparency when processing this image format.
	 * @param fileName - file name with extension
	 * @return true if file format supports transparency
	 */
	public static boolean supportsTransparency(String fileName)
	{
		ImageFormat format = getImageFormat(fileName);
		return format != null && format.transparency();
	}

	/**
	 * Returns BufferedImage type based on file format.
	 * For transparent images returns TYPE_INT_ARGB to preserve transparency, for others TYPE_INT_RGB.
	 * @param fileName - file name with extension
	 * @return BufferedImage.TYPE_INT_ARGB or BufferedImage.TYPE_INT_RGB
	 */
	public static int getBufferedImageType(String fileName)
	{
		return supportsTransparency(fileName) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
	}

	/**
	 * Writes BufferedImage to file in correct format based on file extension.
	 * For transparent images preserves transparency.
	 * For JPG/JPEG and WebP uses the requested compression quality.
	 * AVIF uses ImageMagick and its configured compression parameters.
	 * @param image - BufferedImage to write
	 * @param fileName - original file name to detect format (e.g. "photo.png")
	 * @param imageQuality - compression quality from 0 to 100; other values use gallery settings
	 * @param outputFile - output IwcmFile to write to
	 * @return 0 on success, 2 on a write failure, 4 when no encoder supports the requested format
	 */
	public static int writeImage(BufferedImage image, String fileName, int imageQuality, IwcmFile outputFile)
	{
		// No AVIF ImageIO writer is bundled; pass the pixels to ImageMagick through a lossless PNG.
		if (isAvif(fileName)) return writeAvifImage(image, outputFile);
		String ext = FileTools.getFileExtension(fileName);
		if (imageQuality < 0 || imageQuality > 100) {
			imageQuality = GalleryDB.getImageQuality(null, image.getWidth());
		}
		if (imageQuality < 0 || imageQuality > 100) {
			imageQuality = 85;
		}

		String format = ext;
		ImageWriteParam iwparam = null;
		if ("jpg".equals(ext) || "jpeg".equals(ext))
		{
			format = "jpg";
			iwparam = new JPEGImageWriteParam(null);
			iwparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			iwparam.setCompressionQuality(imageQuality / 100.0F);
		}



		ImageWriter writer = null;
		if ("webp".equals(ext)) {
			// ImageIO's shared registry may return a provider from another webapp classloader.
			// This writer and its WebP-specific parameters must come from the same deployment.
			writer = new WebPImageWriterSpi().createWriterInstance(null);
		} else {
			Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName(format);
			if (iter.hasNext()) writer = iter.next();
		}
		if (writer == null)
		{
			Logger.error(ImageTools.class, "No suitable ImageWriter found for format: " + format);
			return 4;
		}

		//for WebP set compression quality using writer's default param (WebPWriteParam)
		if ("webp".equals(ext))
		{
			WebPWriteParam writeParam = ((WebPWriteParam) writer.getDefaultWriteParam());
			if (imageQuality == 100) {
				writeParam.setCompressionType(CompressionType.Lossless);
			} else {
				writeParam.setCompressionType(CompressionType.Lossy);
			}
			writeParam.setUseSharpYUV(true);
			writeParam.setCompressionQuality(imageQuality / 100.0F);
			iwparam = writeParam;
		}

		try (IwcmOutputStream out = new IwcmOutputStream(outputFile);
			 ImageOutputStream ios = ImageIO.createImageOutputStream(out))
		{
			writer.setOutput(ios);
			writer.write(null, new IIOImage(image, null, null), iwparam);
			ios.flush();
			return 0;
		}
		catch (Exception ex)
		{
			Logger.error(ex);
			return 2;
		}
		finally
		{
			writer.dispose();
		}
	}

	/**
	 * ImageIO doesnt support writing AVIF directly, so we use ImageMagick for the conversion from PNG temporary files.
	 * Passes BufferedImage pixels to ImageMagick as a lossless PNG, preserving transparency.
	 * The destination is written by the shared conversion flow only after AVIF encoding succeeds.
	 * @param image source pixels
	 * @param outputFile destination in local or database storage
	 * @return 0 on success, or 2 if writing or converting the image fails
	 */
	private static int writeAvifImage(BufferedImage image, IwcmFile outputFile)
	{
		Path temporaryImage = null;
		try {
			temporaryImage = Files.createTempFile("webjet-imagemagick-pixels-", ".png");
			if (!ImageIO.write(image, "png", temporaryImage.toFile())) return 2;
			// The destination may have a temporary suffix, so select AVIF explicitly.
			return executeImageMagick(new IwcmFile(temporaryImage.toFile()), outputFile, List.of("from", "to"), "avif");
		} catch (Exception ex) {
			Logger.error(ex);
			return 2;
		} finally {
			if (temporaryImage != null) {
				try {
					Files.deleteIfExists(temporaryImage);
				} catch (IOException ex) {
					Logger.error(ex);
				}
			}
		}
	}

	/**
	 * Returns the extensions recognized by {@link #isImage(String)}, including non-resizable formats.
	 * @return an unmodifiable list of lowercase extensions without the leading dot
	 */
	public static List<String> getSupportedImageExtensions()
	{
		return IMAGE_FORMATS.stream().map(ImageFormat::extension).toList();
	}

	private static ImageFormat getImageFormat(String fileName)
	{
		String extension = FileTools.getFileExtension(fileName);
		for (ImageFormat format : IMAGE_FORMATS)
		{
			if (format.extension().equalsIgnoreCase(extension)) return format;
		}
		return null;
	}

	/**
	 * Checks whether the file has a supported image extension.
	 * @param fileName file name or path
	 * @return true if the extension is recognized, regardless of letter case
	 */
	public static boolean isImage(String fileName)
	{
		return getImageFormat(fileName) != null;
	}

	/**
	 * Checks whether WebJET supports resizing the image format.
	 * @param fileName file name or path
	 * @return true for supported resizable formats; false for SVG and unknown extensions
	 */
	public static boolean isResizableImage(String fileName)
	{
		ImageFormat format = getImageFormat(fileName);
		return format != null && format.resizable();
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

		//detect format and transparency support based on file extension
		int imageType = getBufferedImageType(imageFile.getName());

		double scaleFactor = (width * 1.0) / (originalImage.getWidth() * 1.0);
		double scaleFactor2 = (height * 1.0) / (originalImage.getHeight() * 1.0);

		int w = (int) (originalImage.getWidth() * scaleFactor);
		int h = (int) (originalImage.getHeight() * scaleFactor2);

		Image resizedImage = originalImage.getScaledInstance(w, h, Image.SCALE_AREA_AVERAGING);
		BufferedImage bufSmall = new BufferedImage(w, h, imageType);
		Graphics2D graphics2D = bufSmall.createGraphics();
		try
		{
			graphics2D.drawImage(resizedImage, 0, 0, null);
		}
		finally
		{
			graphics2D.dispose();
		}

		if (imageFile.exists())
		{
			imageFile.delete();
		}

		return writeImage(bufSmall, imageFile.getName(), -1, imageFile);
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

			if (imageFile.isFile() && isResizableImage(srcUrlLC))
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

		//detect format and transparency support based on file extension
		int imageType = getBufferedImageType(imageFile.getName());

		Image cropedImage = originalImage.getSubimage(startX, startY, width, height);

		BufferedImage bufSmall = new BufferedImage(width, height, imageType);
		Graphics2D g2d = bufSmall.createGraphics();
        try
        {
            g2d.drawImage(cropedImage, 0, 0, null);
        }
        finally
        {
            g2d.dispose();
        }

		return writeImage(bufSmall, imageFile.getName(), -1, imageFile);
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
			String srcUrlLC = imageFile.getName().toLowerCase();

			if (imageFile.isFile() && isResizableImage(srcUrlLC))
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
		if (supportsTransparency(imageFile.getName()))
		{
			args.add("-background");
			args.add("none");
		}
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
	 * Processes an image through ImageMagick and copies the staged output to local or database storage.
	 * Database input is first copied to disk.
	 * @param from source image
	 * @param to destination image; may be the source
	 * @param args arguments with from/to placeholders; the supplied list is not modified
	 * @return 0 on success, 2 on a save or AVIF processing failure, 3 on another native processing
	 *         failure, or -1 on another execution/I/O failure
	 */
	public static int executeImageMagick(IwcmFile from, IwcmFile to, List<String> args) {
		return executeImageMagick(from, to, args, FileTools.getFileExtension(to.getName()));
	}

	/** Uses an explicit output format when encoding pixels into a destination with a different suffix. */
	private static int executeImageMagick(IwcmFile from, IwcmFile to, List<String> args, String outputFormat) {
		boolean avif = "avif".equalsIgnoreCase(outputFormat);
		args = new ArrayList<>(args);
		//args can contains placeholders for from and to file paths because of IwcmFSDB storage, so we need to find out where they are
		int fromIndex = args.indexOf("from");
		int toIndex = args.indexOf("to");

		boolean fromDB = IwcmFsDB.useDBStorage(from.getVirtualPath());
		// AVIF pixel encoding starts with a local PNG, even when the destination is stored in DB.
		if (fromDB || IwcmFsDB.useDBStorage(to.getVirtualPath()))
		{
			String temporaryFrom = null;
			String temporaryTo = null;
			try
			{
				temporaryFrom = fromDB ? IwcmFsDB.getTempFilePath(getTempFilePath(from.getPath())) : from.getAbsolutePath();
				temporaryTo = IwcmFsDB.getTempFilePath(getTempFilePath(to.getPath()));

				//write DB input to disk; the PNG intermediate is already a local file
				if (fromDB) IwcmFsDB.writeFileToDisk(new File(from.getAbsolutePath()), new File(temporaryFrom), true);

				if (fromIndex != -1) args.set(fromIndex, temporaryFrom);
				if (toIndex != -1) args.set(toIndex, temporaryTo);

				int result = executeImageMagickCommand(args, outputFormat);
				if (result == 0)
				{
					// IwcmFile selects the destination storage, including local PNG -> DB AVIF conversion.
					return FileTools.copyFile(new IwcmFile(temporaryTo), to) ? 0 : 2;
				}
				return result;
			}
			catch (IOException e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
			finally
			{
				if (fromDB) deleteImageMagickTemporaryFile(temporaryFrom);
				deleteImageMagickTemporaryFile(temporaryTo);
			}
		}
		else
		{
			//usualy from and to is same file, we need temp to file and then replace to with this temp file
			IwcmFile toTemp = new IwcmFile(getTempFilePath(to.getAbsolutePath()));
			try
			{
				if (!toTemp.getParentFile().exists() && !toTemp.getParentFile().mkdirs()) return 2;
				if (fromIndex != -1) args.set(fromIndex, from.getAbsolutePath());
				if (toIndex != -1) args.set(toIndex, toTemp.getAbsolutePath());
				int result = executeImageMagickCommand(args, outputFormat);
				if (result == 0)
				{
					if (!FileTools.copyFile(toTemp, to)) return 2;
					return 0;
				}
				return result;
			}
			finally
			{
				deleteImageMagickTemporaryFile(toTemp.getAbsolutePath());
			}
		}
		return avif ? 2 : -1;
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

	private static boolean isAvif(String name) {
		return "avif".equalsIgnoreCase(FileTools.getFileExtension(name));
	}

	/**
	 * Selects the first source frame and prepares static, 8-bit sRGB AVIF output.
	 * Call after injecting custom parameters, while the input and output are still plain paths.
	 * @param args command with the executable first, input second and output last
	 */
	private static void addAvifImageMagickParams(List<String> args) {
		String input = args.get(1);
		String decoder = isAvif(input) ? "avif:" : "";
		args.set(1, decoder + input + "[0]");
		args.add(2, "-auto-orient");
		args.set(args.size() - 1, "avif:" + args.get(args.size() - 1));
		args.addAll(args.size() - 1, List.of("+repage", "-colorspace", "sRGB", "-depth", "8"));
	}

	/**
	 * Prepares and runs ImageMagick with stdout and stderr inherited from the server process.
	 * @param args input, operations and output paths
	 * @param outputFormat requested output format; AVIF uses a 60-second timeout
	 * @return 0 on success, 2 on an AVIF failure, 3 for another nonzero exit code, or -1 on another execution failure
	 */
	private static int executeImageMagickCommand(List<String> args, String outputFormat)
	{
		args.add(0, getImageMagickCommand());
		boolean avif = "avif".equalsIgnoreCase(outputFormat);
		Process process = null;
		try
		{
			// Use AVIF output settings even when the input is the lossless PNG from writeImage().
			addImageMagickCustomParams(args, avif ? "avif" : null);
			if (avif) addAvifImageMagickParams(args);

			Logger.debug(ImageTools.class, "LONGCMD:\n" + String.join(" ", args));
			process = new ProcessBuilder(args)
				.redirectOutput(ProcessBuilder.Redirect.INHERIT)
				.redirectError(ProcessBuilder.Redirect.INHERIT)
				.start();
			if (avif) {
				if (!process.waitFor(60, TimeUnit.SECONDS)) throw new IOException("ImageMagick timed out");
			} else {
				process.waitFor();
			}
			// Native exit codes are not WebJET image result codes (1 is a failure here).
			if (process.exitValue() == 0) return 0;
			return avif ? 2 : 3;
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			Logger.error(e);
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		finally
		{
			if (process != null)
			{
				process.destroyForcibly();
			}
		}
		return avif ? 2 : -1;
	}

	/** Deletes staged files after successful processing or failure. */
	private static void deleteImageMagickTemporaryFile(String path) {
		if (path == null) return;
		try {
			Files.deleteIfExists(Path.of(path));
		} catch (IOException ex) {
			Logger.error(ex);
		}
	}

	/**
	 * Injects custom ImageMagick parameters into the command args based on operation type and file format.
	 * Config values can contain two lines separated by newline:
	 * - Line 1: params injected BEFORE the operation (after input file), e.g. -filter Lanczos
	 * - Line 2: params injected AFTER the operation (before output file), e.g. -define png:compression-level=9
	 * If only one line is present, all params are injected before the operation (backward compatible).
	 */
	static void addImageMagickCustomParams(List<String> args)
	{
		addImageMagickCustomParams(args, null);
	}

	static void addImageMagickCustomParams(List<String> args, String outputFormat)
	{
		String operation = null;
		for (String arg : args) {
			if (arg.startsWith("-resize") || arg.startsWith("-crop") || arg.startsWith("-rotate")) {
				operation = arg.substring(1);
				break;
			}
		}

		if (operation == null && outputFormat != null) operation = "resize";
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
		if (outputFormat != null) ext = outputFormat;

		//check custom params by Constants key imageMagickCustomParams_[mode]_[ext], for example imageMagickCustomParams_resize_jpg, then imageMagickCustomParams_resize, then imageMagickCustomParams_jpg then imageMagickCustomParams
		String customParamsKey = "imageMagickCustomParams_" + operation + "_" + ext;
		String customParams = Constants.getString(customParamsKey);

		//remove first line NON WHITESPACE character (because Constants are trimed when reading from DB)
		if (Tools.isNotEmpty(customParamsKey) && customParamsKey.startsWith("---")) customParamsKey = customParamsKey.substring(3);
		if (Tools.isNotEmpty(customParams) && customParams.startsWith("---")) customParams = customParams.substring(3);

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
				customParams = combineCustomParams(customParams, customParamsExt);
			}
		}

		if (Tools.isNotEmpty(customParams)) {
			//split into before-operation (inputParams) and after-operation (outputParams) by newline
			String inputParams;
			String outputParams;
			int newlineIndex = customParams.indexOf('\n');
			if (newlineIndex != -1) {
				inputParams = customParams.substring(0, newlineIndex).trim();
				outputParams = customParams.substring(newlineIndex + 1).trim();
			} else {
				inputParams = customParams.trim();
				outputParams = "";
			}
			if ("---".equals(inputParams)) {
				inputParams = "";
			}
			if ("---".equals(outputParams)) {
				outputParams = "";
			}

			String allParams = (inputParams + " " + outputParams).trim();
			String[] allParamsArray = Tools.getTokens(allParams, " ", true);

			//if contains compressionLevel remove -quality xx param
			for (String customParam : allParamsArray) {
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

			//inject input params after from file path like: magick file.jpg [input params] -resize 100x100! file.jpg
			if (Tools.isNotEmpty(inputParams)) {
				String[] inputParamsArray = Tools.getTokens(inputParams, " ", true);
				int counter = 0;
				for (String customParam : inputParamsArray) {
					args.add(2 + counter, customParam);
					counter++;
				}
			}

			//inject output params before the output file path like: magick file.jpg -resize 100x100! [output params] file.jpg
			if (Tools.isNotEmpty(outputParams)) {
				String[] outputParamsArray = Tools.getTokens(outputParams, " ", true);
				for (String customParam : outputParamsArray) {
					args.add(args.size() - 1, customParam);
				}
			}
		}
	}

	/**
	 * Combines two custom params strings line-by-line. Each string can contain two lines (input params \n output params).
	 * Line 1 of base is combined with line 1 of ext, line 2 of base with line 2 of ext.
	 * @param base - base custom params (can be null/empty)
	 * @param ext - extension-specific custom params (must not be empty)
	 * @return combined custom params string
	 */
	static String combineCustomParams(String base, String ext) {
		if (Tools.isEmpty(base)) {
			return ext;
		}

		String[] baseParts = splitParamLines(base);
		String[] extParts = splitParamLines(ext);

		String combinedInput = combineLine(baseParts[0], extParts[0]);
		String combinedOutput = combineLine(baseParts[1], extParts[1]);

		if (Tools.isEmpty(combinedOutput)) {
			return combinedInput;
		}
		return combinedInput + "\n" + combinedOutput;
	}

	private static String[] splitParamLines(String params) {
		int newlineIndex = params.indexOf('\n');
		if (newlineIndex != -1) {
			return new String[] { params.substring(0, newlineIndex).trim(), params.substring(newlineIndex + 1).trim() };
		}
		return new String[] { params.trim(), "" };
	}

	private static String combineLine(String a, String b) {
		if (a.startsWith("---")) a = a.substring(3);
		if (b.startsWith("---")) b = b.substring(3);

		if (Tools.isEmpty(a)) return Tools.isNotEmpty(b) ? b : "";
		if (Tools.isEmpty(b)) return a;
		return a + " " + b;
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

	static String getImageMagickCommand() {
		return getImageMagicDir() + File.separatorChar + getRuntimeFile();
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

		//detect format and transparency support based on file extension
		int imageType = getBufferedImageType(imageFile.getName());

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
		BufferedImage result = new BufferedImage(neww, newh, imageType);

		Graphics2D g = result.createGraphics();
		g.translate((neww-w)/(double)2, (newh-h)/(double)2);
		g.rotate(angleRad, w/(double)2, h/(double)2);
		g.drawRenderedImage(originalImage, null);

		Image rotatedImage = result.getSubimage(0, 0, result.getWidth(), result.getHeight());

		BufferedImage bufSmall = new BufferedImage(neww, newh, imageType);
		Graphics2D g2d = bufSmall.createGraphics();
        try
        {
            g2d.drawImage(rotatedImage, 0, 0, null);
        }
        finally
        {
            g2d.dispose();
        }

		return writeImage(bufSmall, imageFile.getName(), -1, imageFile);
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

	/**
	 * Sanitize SVG file by removing script elements and on* event attributes
	 * to prevent XSS attacks when the SVG is served to browsers.
	 */
	public static void sanitizeSvgFile(IwcmFile svgFile)
	{
		try {
			String content = FileTools.readFileContent(svgFile.getAbsolutePath());
			Document doc = Jsoup.parse(content, "", Parser.xmlParser());

			// Remove script elements
			doc.select("script").remove();

			// Remove on* event handler attributes from all elements
			for (Element el : doc.select("*")) {
				el.attributes().asList().stream()
					.filter(attr -> attr.getKey().toLowerCase().startsWith("on"))
					.forEach(attr -> el.removeAttr(attr.getKey()));
			}

			// Remove href attributes with javascript: protocol
			for (Element el : doc.select("[href], [xlink:href]")) {
				String href = el.hasAttr("href") ? el.attr("href") : el.attr("xlink:href");
				if (href.replaceAll("\\s", "").toLowerCase().startsWith("javascript:")) {
					el.removeAttr("href");
					el.removeAttr("xlink:href");
				}
			}

			FileTools.saveFileContent(svgFile.getAbsolutePath(), doc.html(), StandardCharsets.UTF_8.name());
		}
		catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
		}
	}

}
