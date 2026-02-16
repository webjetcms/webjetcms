package sk.iway.iwcm.gallery;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.editor.UploadFileForm;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFsDB;

/**
 * VideoConvert.java - trieda pre konverziu video suborov
 *
 *@Title webjet4
 *@Company Interway s.r.o. (www.interway.sk)
 *@Copyright Interway s.r.o. (c) 2001-2007
 *@author $Author: jeeff $
 *@version $Revision: 1.18 $
 *@created Date: 7.9.2007 21:24:21
 *@modified $Date: 2009/01/15 10:08:45 $
 */
public class VideoConvert
{

	public static boolean convertToMp4(String sourceUrl, String destinationUrl, int width, int height, int bitRate, boolean keepOrig,
				HttpServletRequest request){
		return  convert("mp4", sourceUrl, destinationUrl, width, height, bitRate, keepOrig, request);
	}

	public static boolean convertToFlv(String sourceUrl, String destinationUrl, int width, int height, int bitRate, boolean keepOrig,
				HttpServletRequest request)
	{
		return convert("flv", sourceUrl, destinationUrl, width, height, bitRate, keepOrig, request);
	}

	public static boolean convert(String format, String sourceUrl, String destinationUrl, int width, int height, int bitRate, boolean keepOrig,
				HttpServletRequest request)
	{
		Dimension[] dims = null;
		String flvFilename = null;
		if (GalleryDB.isGalleryFolder(destinationUrl))
		{
			dims = GalleryDB.getDimension(destinationUrl);
			if ("small".equals(Constants.getString("galleryVideoMode")))
			{
				Dimension[] dimsNew = new Dimension[1];
				dimsNew[0] = new Dimension(dims[0].width, dims[0].height);
				dims = dimsNew;
			}
			else if ("big".equals(Constants.getString("galleryVideoMode")))
			{
				Dimension[] dimsNew = new Dimension[1];
				dimsNew[0] = new Dimension(dims[1].width, dims[1].height);
				dims = dimsNew;
			}
		}
		else
		{
			dims = new Dimension[1];
			dims[0] = new Dimension(width, height);
		}
		try
		{
			String videoPath = Tools.getRealPath(sourceUrl);
			int originalBitRate = bitRate;
			for (int y = 0; y < dims.length; y++)
			{
				Dimension d = dims[y];
				// pre velky rozmer musime mat vacsi bitrate
				if (y > 0)
				{
					double pomer = (double) (dims[y].width * dims[y].height) / (double) (dims[0].width * dims[0].height);
					bitRate = (int) Math.round(originalBitRate * pomer);
				}
				Prop prop = Prop.getInstance(request);
				request.getSession().setAttribute("uploadProgressMessage", prop.getText("gallery.videoconvert.converting"));
				String ffmpegPath = Constants.getString("ffmpegPath").trim();
				File f = new File(ffmpegPath);
				if (f.exists() == false || f.isFile() == false || f.canRead() == false)
					return false;
				flvFilename = Tools.getRealPath(
							destinationUrl.substring(0, destinationUrl.lastIndexOf('.')) + "." + d.width + "x" + d.height
										+ destinationUrl.substring(destinationUrl.lastIndexOf('.'), destinationUrl.length()));
				Runtime rt = Runtime.getRuntime();
				if (IwcmFsDB.useDBStorage(sourceUrl))
				{
					videoPath = IwcmFsDB.getTempFilePath(videoPath);
					flvFilename = IwcmFsDB.getTempFilePath(flvFilename);
				}
				//MBO: #15203 pre konverziu pouzijeme upravene rozmery, ak je niektory neparny, zvacsi sa o 1
				Dimension dimsToConvert = new Dimension(
						(int)( ((int)Math.round(d.getWidth()))%2==1 ? d.getWidth()+1 : d.getWidth() ),
						(int)( ((int)Math.round(d.getHeight()))%2==1 ? d.getHeight()+1 : d.getHeight() ) );

				String args[] = ffmpegCommandLine(videoPath,flvFilename,format,bitRate,dimsToConvert);

				File flvFile=new File(flvFilename);
				if (flvFile.exists()){flvFile.delete();}
				int c = 0;
				String params = "";
				StringBuilder buf = new StringBuilder();
				if (args != null)
				{
					for (int i = 0; i < args.length; i++)
					{
						buf.append(' ').append(args[i]);
					}
					params = buf.toString();
				}
				Logger.println(VideoConvert.class, "LONGCMD:\n" + params);
				long startTime = System.currentTimeMillis();
				long elapsedTime = 0;
				int perc = 1;
				Process proc = rt.exec(args);
				Logger.println(VideoConvert.class, "executed");
				InputStream stderr = proc.getErrorStream();
				BufferedReader br = new BufferedReader(new InputStreamReader(stderr, Constants.FILE_ENCODING));
				String line = null;
				float lengthInSeconds = 0;
				float time = 0;
				long fileSize = new File(videoPath).length();
				SimpleDateFormat formatter = new SimpleDateFormat("mm:ss");
				SimpleDateFormat sd = new SimpleDateFormat("ss");
				while ((line = br.readLine()) != null)
				{
					try
					{
						elapsedTime = System.currentTimeMillis() - startTime;
						request.getSession().setAttribute("elapsedTime", formatter.format(sd.parse(Long.toString(elapsedTime / 1000))));
						Logger.println(VideoConvert.class, line);
						// TODO: ffmpeg na zaciatku vypise dlzhu videa / pocet snimkov,
						// parsovat a dedukovat % hodnotu kompletnosti
						// pripadne vypocitavat zostavajuci cas do konca
						if (line.contains("Duration:"))
						{
							int start = line.indexOf("Duration:");
							String dur = line.substring(start + 8 + 2, start + 1 + 8 + 8 + 1);
							dur = dur.trim();
							String durArray[] = dur.split(":");
							if (durArray!=null && durArray.length==3)
							{
								lengthInSeconds = Tools.getIntValue(durArray[2],0);
								lengthInSeconds += Tools.getIntValue(durArray[1],0)*60;
								lengthInSeconds += Tools.getIntValue(durArray[0],0)*60*60;
							}
							System.out.println("dur="+dur+" lengthInSeconds"+lengthInSeconds);
						}
						if (line.contains("time="))
						{
							int start = line.indexOf("time=") + "time=".length();
							int end = start + 1;
							for (int i = start + 1; i < line.length(); i++)
							{
								if (Character.isWhitespace(line.charAt(i)))
								{
									end = i;
									break;
								}
							}
							try
							{
								time = Math.round(Float.parseFloat(line.substring(start, end)));
							}
							catch (Exception  ex)
							{
								try
								{
									//TODO: skus to sparsovat
									//[20.11 13:16:36 {balat}{4}] frame=   21 fps=  0 q=2.0 size=      83kB time=00:00:00.46 bitrate=1463.1kbits/s
									String timeStr[] = Tools.getTokens(line.substring(start, end), ":");
									if (timeStr.length==3)
									{
										time = Tools.getIntValue(timeStr[0], 0)*3600;
										time += Tools.getIntValue(timeStr[1], 0)*60;
										if (timeStr[2].indexOf('.')==-1) time += Tools.getIntValue(timeStr[2], 0);
										else time += Tools.getIntValue(timeStr[2].substring(0, timeStr[2].indexOf('.')), 0);
									}
								}
								catch (Exception ex2)
								{
									sk.iway.iwcm.Logger.error(ex);
								}
							}
							System.out.println("time="+time);
						}
						if (lengthInSeconds > 0)
						{
							float roundTmp = (float)Math.round((lengthInSeconds - (lengthInSeconds - time)) / (lengthInSeconds / 100.0));
							perc = Math.round(roundTmp);
							request.getSession().setAttribute(
										"remainingTime",
										formatter.format(sd.parseObject(Long.toString(Math.round((0.7 * (fileSize / 10000000.0))
													* ((100 - perc) / 100f))))));
							request.getSession().setAttribute("percentage", perc);
							System.out.println("percentage="+perc);
						}
						request.getSession().setAttribute("uploadProgressMessage", prop.getText("gallery.videoconvert.converting"));
					}
					catch (Exception ex)
					{
						sk.iway.iwcm.Logger.error(ex);
					}
				}
				br.close();
				int exitValue = proc.waitFor();
				Logger.println(VideoConvert.class, "ExitValue: " + exitValue);
				if (format.toLowerCase().equals("flv") || format.toLowerCase().equals("mp4"))
				{
					if (format.toLowerCase().equals("flv"))
					{
						request.getSession().setAttribute("uploadProgressMessage", prop.getText("gallery.videoconvert.indexing"));
						String yamdiPath = Constants.getString("yamdiPath").trim();
						File yamdi = new File(yamdiPath);
						if (f.exists() == false || f.isFile() == false || f.canRead() == false)
							return false;
						String yArgs[] = new String[5];
						int index = 0;
						yArgs[index++] = yamdi.getAbsolutePath();
						yArgs[index++] = "-i";
						yArgs[index++] = flvFilename;
						yArgs[index++] = "-o";
						yArgs[index++] = flvFilename + ".tmp";
						params = "";
						StringBuilder paramsBuf = new StringBuilder(params);
						if (args != null)
						{
							for (int i = 0; i < yArgs.length; i++)
							{
								paramsBuf.append(' ').append(yArgs[i]);
							}
						}
						params = paramsBuf.toString();
						Logger.println(VideoConvert.class, "LONGCMD:\n" + params);
						proc = rt.exec(yArgs);
						Logger.println(VideoConvert.class, "executed");
						stderr = proc.getErrorStream();
						br = new BufferedReader(new InputStreamReader(stderr, Constants.FILE_ENCODING));
						line = null;
						while ((line = br.readLine()) != null)
						{
							Logger.println(VideoConvert.class, line);
							request.getSession().setAttribute("uploadProgressMessage", prop.getText("gallery.videoconvert.indexing"));
						}
						br.close();
						exitValue = proc.waitFor();
						Logger.println(VideoConvert.class, "ExitValue: " + exitValue);

					}
					c = 0;
					String previewArgs[] = new String[14];
					previewArgs[c++] = f.getAbsolutePath();
					previewArgs[c++] = "-i";
					previewArgs[c++] = videoPath;
					previewArgs[c++] = "-an";
					previewArgs[c++] = "-ss";
					previewArgs[c++] = "00:00:03";
					previewArgs[c++] = "-t";
					previewArgs[c++] = "00:00:01";
					previewArgs[c++] = "-r";
					previewArgs[c++] = "1";
					previewArgs[c++] = "-y";
					previewArgs[c++] = "-s";
					previewArgs[c++] = d.width + "x" + d.height;
					previewArgs[c++] = flvFilename.substring(0, flvFilename.lastIndexOf('.')) + "%d.jpg";
					params = "";

					StringBuilder paramsBuf = new StringBuilder(params);

					if (args != null)
					{
						for (int i = 0; i < previewArgs.length; i++)
						{
							paramsBuf.append(' ').append(previewArgs[i]);
						}
					}
					params = paramsBuf.toString();

					Logger.println(VideoConvert.class, "LONGCMD:\n" + params);
					request.getSession()
								.setAttribute("uploadProgressMessage", prop.getText("gallery.videoconvert.creating_thumbnail"));
					// proc=rt.exec(previewArgs);
					proc = rt.exec(previewArgs);
					Logger.println(VideoConvert.class, "executed");
					stderr = proc.getErrorStream();
					br = new BufferedReader(new InputStreamReader(stderr, Constants.FILE_ENCODING));
					line = null;
					while ((line = br.readLine()) != null)
					{
						Logger.println(VideoConvert.class, line);
						request.getSession().setAttribute("uploadProgressMessage",
									prop.getText("gallery.videoconvert.creating_thumbnail"));
					}
					br.close();
					exitValue = proc.waitFor();
					Logger.println(VideoConvert.class, "ExitValue: " + exitValue);
					// ffmpeg -i video.flv -an -ss 00:00:03 -t 00:00:01 -r 1 -y -s
					// 320x240 video%d.jpg

					//zmen .flv.tmp subor na .flv
					File video = new File(flvFilename);

					if (format.toLowerCase().equals("flv"))
					video.delete();
					renameFile(flvFilename + ".tmp", flvFilename);

					video = new File(flvFilename);

					// vytvori to viac obrazkov, chceme len jeden
					renameFile(flvFilename.substring(0, flvFilename.lastIndexOf('.')) + "3.jpg", flvFilename.substring(0, flvFilename
								.lastIndexOf('.'))
								+ ".jpg");
					renameFile(flvFilename.substring(0, flvFilename.lastIndexOf('.')) + "2.jpg", flvFilename.substring(0, flvFilename
								.lastIndexOf('.'))
								+ ".jpg");
					renameFile(flvFilename.substring(0, flvFilename.lastIndexOf('.')) + "1.jpg", flvFilename.substring(0, flvFilename
								.lastIndexOf('.'))
								+ ".jpg");
					String imageName=flvFilename.substring(0, flvFilename.lastIndexOf('.'))+ ".jpg";
					flvFilename = Tools.getRealPath(
								destinationUrl.substring(0, destinationUrl.lastIndexOf('.')) + "." + d.width + "x" + d.height
											+ destinationUrl.substring(destinationUrl.lastIndexOf('.'), destinationUrl.length()));
					// sme galeria?
					String imageUrl = destinationUrl.substring(0, destinationUrl.lastIndexOf('.')) + "." + d.width + "x" + d.height
								+ ".jpg";
					if (IwcmFsDB.useDBStorage(sourceUrl))
					{
						IwcmFsDB.writeFiletoDest(new FileInputStream(video), new File(flvFilename), (int) video.length());// zapiseme
						video.delete();

						//zapiseme do db obrazky
						File image = new File(imageName);
						IwcmFsDB.writeFiletoDest(new FileInputStream(image), new File(Tools.getRealPath(imageUrl)),(int)image.length());
						if(image.delete() == false) return false;
					}
					if (GalleryDB.isGalleryFolder(imageUrl))
					{
						GalleryDB.resizePicture(Tools.getRealPath(imageUrl), imageUrl.substring(0, imageUrl
									.lastIndexOf('/')));
					}
				}
			}

			if(keepOrig==false)
			{
				if(new File(videoPath).delete() == false) return false; // vymazanie orig. video suboru
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		request.getSession().removeAttribute("uploadProgressMessage");
		return true;
	}

	public static boolean convertAudio(String format, String sourceUrl, String destinationUrl, int bitRate, boolean keepOrig, HttpServletRequest request)
	{
		try
		{
			String videoPath = Tools.getRealPath(sourceUrl);

			Prop prop = Prop.getInstance(request);
			request.getSession().setAttribute("uploadProgressMessage", prop.getText("gallery.audioconvert.converting"));
			String ffmpegPath = Constants.getString("ffmpegPath").trim();
			File f = new File(ffmpegPath);
			if (f.exists() == false || f.isFile() == false || f.canRead() == false)
				return false;
			String flvFilename = Tools.getRealPath(destinationUrl);
			Runtime rt = Runtime.getRuntime();
			String args[] = new String[8];
			if (IwcmFsDB.useDBStorage(sourceUrl))
			{
				videoPath = IwcmFsDB.getTempFilePath(videoPath);
				flvFilename = IwcmFsDB.getTempFilePath(flvFilename);
			}
			File flvFile=new File(flvFilename);
			if (flvFile.exists()){
				if(flvFile.delete() == false) return false;
			}
			int c = 0;
			args[c++] = f.getAbsolutePath();
			args[c++] = "-i";
			args[c++] = videoPath;
			args[c++] = "-f";
			args[c++] = format;
			args[c++] = "-ab";
			args[c++] = bitRate + "k";
			args[c++] = flvFilename;
			String params = "";
			StringBuilder buf = new StringBuilder();
			if (args != null)
			{
				for (int i = 0; i < args.length; i++)
				{
					buf.append(' ').append(args[i]);
				}
				params = buf.toString();
			}
			Logger.println(VideoConvert.class, "LONGCMD:\n" + params);
			long startTime = System.currentTimeMillis();
			long elapsedTime = 0;
			int perc = 1;
			Process proc = rt.exec(args);
			Logger.println(VideoConvert.class, "executed");
			InputStream stderr = proc.getErrorStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(stderr, Constants.FILE_ENCODING));
			String line = null;
			float lengthInSeconds = 0;
			float time = 0;
			long fileSize = new File(videoPath).length();
			SimpleDateFormat formatter = new SimpleDateFormat("mm:ss");
			SimpleDateFormat sd = new SimpleDateFormat("ss");
			while ((line = br.readLine()) != null)
			{
				elapsedTime = System.currentTimeMillis() - startTime;
				request.getSession().setAttribute("elapsedTime", formatter.format(sd.parse(Long.toString(elapsedTime / 1000))));
				Logger.println(VideoConvert.class, line);
				// TODO: ffmpeg na zaciatku vypise dlzhu videa / pocet snimkov,
				// parsovat a dedukovat % hodnotu kompletnosti
				// pripadne vypocitavat zostavajuci cas do konca
				if (line.contains("Duration:"))
				{
					int start = line.indexOf("Duration:");
					String dur = line.substring(start + 8 + 2, start + 1 + 8 + 8 + 1);
					dur = dur.trim();
					String durArray[] = dur.split(":");
					if (durArray!=null && durArray.length==3)
					{
						lengthInSeconds = Tools.getIntValue(durArray[2],0);
						lengthInSeconds += Tools.getIntValue(durArray[1],0)*60;
						lengthInSeconds += Tools.getIntValue(durArray[0],0)*60*60;
					}
					System.out.println("dur="+dur+" lengthInSeconds"+lengthInSeconds);
				}
				if (line.contains("time="))
				{
					int start = line.indexOf("time=") + "time=".length();
					int end = start + 1;
					for (int i = start + 1; i < line.length(); i++)
					{
						if (Character.isWhitespace(line.charAt(i)))
						{
							end = i;
							break;
						}
					}
					time = Math.round(Float.parseFloat(line.substring(start, end)));
					System.out.println("time="+time);
				}
				if (lengthInSeconds > 0)
				{
					float roundTmp = (float)Math.round((lengthInSeconds - (lengthInSeconds - time)) / (lengthInSeconds / 100.0));
					perc = Math.round(roundTmp);
					request.getSession().setAttribute(
								"remainingTime",
								formatter.format(sd.parseObject(Long.toString(Math.round((0.7 * (fileSize / 1000000.0))
											* ((100 - perc) / 100f))))));
					request.getSession().setAttribute("percentage", perc);
					System.out.println("percentage="+perc);
				}
				request.getSession().setAttribute("uploadProgressMessage", prop.getText("gallery.audioconvert.converting"));
			}
			br.close();
			int exitValue = proc.waitFor();
			Logger.println(VideoConvert.class, "ExitValue: " + exitValue);

			if (IwcmFsDB.useDBStorage(sourceUrl))
			{
				IwcmFsDB.writeFiletoDest(new FileInputStream(flvFile), new File(Tools.getRealPath(destinationUrl)) , (int) flvFile.length());// zapiseme
				if(flvFile.delete() == false) return false;
			}

			if(keepOrig==false)
			{
				if(new File(videoPath).delete() == false) return false; // vymazanie orig. video suboru
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		request.getSession().removeAttribute("uploadProgressMessage");
		return true;
	}

	public static boolean renameFile(String oldFilePath, String newFilePath)
	{
		boolean renamed = false;
		try
		{
			File renameFile = new File(oldFilePath);
			if (renameFile.exists() == false)
				return false;
			renamed = renameFile.renameTo(new File(newFilePath));
			if (renamed == false)
			{
				File newFile = new File(newFilePath);
				File oldFile = new File(oldFilePath);
				if(newFile.createNewFile() == false) return false;
				FileInputStream inStream = new FileInputStream(oldFile);
				FileOutputStream out = new FileOutputStream(newFile);
				int c;
				byte[] buff = new byte[150000];
				while ((c = inStream.read(buff)) != -1)
				{
					out.write(buff, 0, c);
				}
				out.close();
				inStream.close();
				renamed = true;
				try
				{
					// skus ho zmazat
					oldFile.delete();
				}
				catch (Exception ex)
				{
					sk.iway.iwcm.Logger.error(ex);
				}
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return renamed;
	}

	private static String[] ffmpegCommandLine(String from,String to,String format,int bitRate,java.awt.Dimension d){
		String params = Constants.getString("ffmpegParams");
		String[] command = params.split(" ");
		command = (String[]) ArrayUtils.removeElement(command, " ");
		command = (String[]) ArrayUtils.removeElement(command, "");

		command[ArrayUtils.indexOf(command, "ffmpeg")]  = Constants.getString("ffmpegPath").trim();
		command[ArrayUtils.indexOf(command, "dimension")] =  d.width + "x" + d.height;
		command[ArrayUtils.indexOf(command, "from")] = from;
		command[ArrayUtils.indexOf(command, "to")] = to;
		command[ArrayUtils.indexOf(command, "format")] = format;
		command[ArrayUtils.indexOf(command,"bitrate")]=bitRate+"k";

		return command;
	}

	/**
	 * Skonveruje uploadnuty subor na video, pouziva sa v Editore v uploade a v galerii v uploade
	 * @param my_form
	 * @param fileURL
	 * @param request
	 * @return
	 */
	public static String convert(UploadFileForm my_form, String fileURL, HttpServletRequest request)
	{
		if (my_form.getBitRate()<32) my_form.setBitRate(360);
		if (my_form.getVideoWidth()<32) my_form.setVideoWidth(320);
		if (my_form.getVideoHeight()<32) my_form.setVideoHeight(240);

		if (my_form.getVideoWidth()%8!=0 || my_form.getVideoHeight()%8!=0)
		{
			my_form.setVideoWidth((my_form.getVideoWidth()/8)*8);
			my_form.setVideoHeight((my_form.getVideoHeight()/8)*8);
		}

		String flvUrl = fileURL.substring(0, fileURL.lastIndexOf('.'))+"."+Constants.getString("defaultVideoFormat");

		if (GalleryDB.isGalleryFolder(flvUrl))
		{
			String flvDir = flvUrl.substring(0, flvUrl.lastIndexOf("/"));
			Dimension[] dm=GalleryDB.getDimension(flvDir);
			Dimension dSmall = dm[0];
			Dimension dNormal = dm[1];

			if (dSmall.width%8!=0 || dSmall.height%8!=0 || dNormal.width%8!=0 || dNormal.height%8!=0)
			{
				changeDimension((dSmall.width/8)*8, (dSmall.height/8)*8, (dNormal.width/8)*8, (dNormal.height/8)*8, flvDir);
			}
		}

		VideoConvert.convert( Constants.getString("defaultVideoFormat"),fileURL, flvUrl, my_form.getVideoWidth(), my_form.getVideoHeight(),my_form.getBitRate(),my_form.isKeepOriginalVideo(), request);

		if (GalleryDB.isGalleryFolder(flvUrl))
		{
			Dimension[] dm=GalleryDB.getDimension(flvUrl.substring(0,flvUrl.lastIndexOf('/')));
			if (dm.length>0)
			{
				Dimension min=dm[0];
				for (Dimension d:dm)
				{
					if (d.width<min.width && d.height<min.height)
					{
						min=d;
					}
				}
				flvUrl=fileURL.substring(0, fileURL.lastIndexOf('.'))+"."+min.width+"x"+min.height+"."+Constants.getString("defaultVideoFormat");
				request.setAttribute("imageWidth", Integer.toString(min.width));
				request.setAttribute("imageHeight",Integer.toString(min.height));
			}
		}
		else
		{
			flvUrl=fileURL.substring(0, fileURL.lastIndexOf('.'))+"."+my_form.getVideoWidth()+"x"+my_form.getVideoHeight()+"."+Constants.getString("defaultVideoFormat");
			request.setAttribute("imageWidth", Integer.toString(my_form.getVideoWidth()));
			request.setAttribute("imageHeight",Integer.toString(my_form.getVideoHeight()));
		}

		return flvUrl;
	}

	private static void changeDimension(int smallWidth, int smallHeight, int normalWidth, int normalHeight, String imagePath)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;

		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement("update gallery_dimension set image_width = ?,image_height=?, " +
					"normal_width = ? , normal_height = ? " +
					"where image_path = ? and domain_id = ?");
			int index=1;
			ps.setInt(index++, smallWidth);
			ps.setInt(index++, smallHeight);
			ps.setInt(index++, normalWidth);
			ps.setInt(index++, normalHeight);
			ps.setString(index++, imagePath);
			ps.setInt(index++, CloudToolsForCore.getDomainId());
			ps.execute();


			ps.close();
			db_conn.close();

			ps = null;
			db_conn = null;
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{
			try
			{
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}

	}


	/**
	 * Vrati true ak sa dany subor je mozne povazovat za video subor
	 * @param fileName
	 * @return
	 */
	public static boolean isVideoFile(String fileName)
	{
		if (Tools.isEmpty(fileName)) return false;

		fileName = fileName.toLowerCase();

		if (fileName.indexOf(".mpg")!=-1 || fileName.indexOf(".mpeg")!=-1 ||
		   	    fileName.indexOf(".avi")!=-1 || fileName.indexOf(".qt")!=-1 ||
		   	    fileName.indexOf(".mp4")!=-1 || fileName.indexOf(".wmv")!=-1 ||
		   	    fileName.indexOf(".vob")!=-1 || fileName.indexOf(".mov")!=-1 ||
		   	    fileName.indexOf(".flv")!=-1 || fileName.indexOf(".m4v")!=-1)
		{
			return true;
		}

		return false;
	}

	public static String makeScreenshot(String videoPath, Dimension d) throws IOException, InterruptedException
	{
	    String imageName = null;
	    try
        {
            String flvFilename = videoPath;

            List<String> arguments = new ArrayList<String>();
            String ffMpegPath = Constants.getString("ffmpegPath").trim();

            arguments.add(ffMpegPath);
            arguments.add("-i");
            arguments.add(videoPath);
            arguments.add("-an");
            arguments.add("-ss");
            arguments.add("00:00:03");
            arguments.add("-t");
            arguments.add("00:00:01");
            arguments.add("-r");
            arguments.add("1");
            arguments.add("-y");


            if (d != null)
            {
                arguments.add("-s");
                arguments.add(d.width + "x" + d.height);
            }

            arguments.add(flvFilename.substring(0, flvFilename.lastIndexOf('.')) + "%d.jpg");

            String params = StringUtils.join(arguments, " ");
            String[] args = new String[arguments.size()];

            Logger.println(VideoConvert.class, "LONGCMD:\n" + params);
            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec(arguments.toArray(args));
            Logger.println(VideoConvert.class, "executed");
            InputStream stderr = proc.getErrorStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(stderr, Constants.FILE_ENCODING));
            String line = null;
            while ((line = br.readLine()) != null)
            {
                Logger.println(VideoConvert.class, line);
            }
            br.close();
            int exitValue = proc.waitFor();
            Logger.println(VideoConvert.class, "ExitValue: " + exitValue);
            // ffmpeg -i video.flv -an -ss 00:00:03 -t 00:00:01 -r 1 -y -s
            // 320x240 video%d.jpg


            // vytvori to viac obrazkov, chceme len jeden
            renameFile(flvFilename.substring(0, flvFilename.lastIndexOf('.')) + "3.jpg", flvFilename.substring(0, flvFilename
                    .lastIndexOf('.'))
                    + ".jpg");
            renameFile(flvFilename.substring(0, flvFilename.lastIndexOf('.')) + "2.jpg", flvFilename.substring(0, flvFilename
                    .lastIndexOf('.'))
                    + ".jpg");
            renameFile(flvFilename.substring(0, flvFilename.lastIndexOf('.')) + "1.jpg", flvFilename.substring(0, flvFilename
                    .lastIndexOf('.'))
                    + ".jpg");
            imageName = flvFilename.substring(0, flvFilename.lastIndexOf('.')) + ".jpg";
        }
        catch (Exception ex)
        {
            sk.iway.iwcm.Logger.error(ex);
        }
		return imageName;
	}
}
