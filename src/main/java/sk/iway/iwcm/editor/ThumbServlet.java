package sk.iway.iwcm.editor;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.SpamProtection;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.GalleryDBTools;
import sk.iway.iwcm.common.WriteTagToolsForCore;
import sk.iway.iwcm.filebrowser.EditForm;
import sk.iway.iwcm.gallery.GalleryBean;
import sk.iway.iwcm.gallery.GalleryDB;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmInputStream;
import sk.iway.iwcm.system.context.ContextFilter;
import sk.iway.iwcm.system.multidomain.MultiDomainFilter;
import sk.iway.iwcm.users.UsersDB;

/**
 *  Vytvara a cachuje nahladove obrazky pre editor
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: $
 *@created      Nedela, 2004, apr 18
 *@modified     $Date: $
 */
@SuppressWarnings({"java:S1075", "java:S1989"})
@WebServlet(name = "thumbServlet", urlPatterns = {"/admin/thumb/*", "/thumb/*", "/tumbn/*"})
public class ThumbServlet extends HttpServlet
{
	private static final String FILL_COLOR_DEFAULT = "ffffff";

	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = -8491779689090189385L;

	private static Pattern pattern = Pattern.compile("[a-z\\-_0-9A-Z]*-(\\d+)x(\\d+)(ip(\\d))?(c([\\da-f]+))?(q(\\d+))?\\.[a-z]{3,4}\\b");

	//public static final String CACHE_DIR = "/WEB-INF/imgcache/";


	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String path = (String) request.getAttribute("path_filter_orig_path");

        if (Tools.isEmpty(path) || path.length()<9)
        {
            sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
            response.setStatus(404);
            response.getWriter().print("<html><body>404 - not found</body></html>");
            return;
        }

		if (path.startsWith("/admin/thumb/")){
			path = path.substring(13);
		}else{
			path = path.substring(7);
		}

		path = Tools.replace(path, "%20", " ");

		getThumbImage(path, request, response);
	}

 //na metodu sa nic neodkazuje nie je potrebna
//	public static void getThumbImage(String imagePath, HttpServletResponse response) throws IOException, ServletException
//	{
//		getThumbImage(imagePath, null, response);
//	}

	public static void getThumbImage(String imagePath, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
	{
		if (Tools.isEmpty(imagePath))
		{
			sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
			response.setStatus(404);
			response.getWriter().print("<html><body>404 - not found</body></html>");
			return;
		}
		if (imagePath.startsWith("//")) imagePath = imagePath.substring(2);
		if (imagePath.startsWith("/")) imagePath = imagePath.substring(1);

		if (ContextFilter.isRunning(request) && imagePath.length()>request.getContextPath().length()+3)
		{
			imagePath = ContextFilter.removeContextPath(request.getContextPath(), "/"+imagePath).substring(1);
		}

		if (Constants.getBoolean("multiDomainEnabled") && Constants.getBoolean("enableStaticFilesExternalDir")==false)
		{
			String origPath = imagePath;
			imagePath = MultiDomainFilter.rewriteUrlToLocal("/"+imagePath, request).substring(1);
			if (FileTools.isFile("/"+imagePath)==false && FileTools.isFile("/"+origPath))
			{
				//if imagePath doesn't exists and origPath yes, use origPath (probably it's not multidomain folder)
				imagePath = origPath;
			}
		}

		String ext = FileTools.getFileExtension(imagePath).toLowerCase();
		if (ext.equals("jpg")==false && ext.equals("jpeg")==false && ext.equals("gif")==false && ext.equals("png")==false && ext.equals("bmp")==false)
		{
			//sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
			response.setStatus(302);
			response.setHeader("Location", "/"+imagePath);
			response.getWriter().print("<html><body>404 - not found</body></html>");
			return;
		}

		if ( (imagePath.startsWith("images/")==false && imagePath.startsWith("files/")==false && imagePath.startsWith("shared/")==false && imagePath.startsWith("video/")==false && imagePath.startsWith("templates/")==false && imagePath.indexOf("grideditor")==-1 && imagePath.indexOf("thumb")==-1 && imagePath.indexOf("placeholder")==-1 && imagePath.indexOf("stock")==-1) || imagePath.indexOf("..")!=-1)
		{
			sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
			response.setStatus(404);
			response.getWriter().print("<html><body>404  - not found</body></html>");
			return;
		}

		//ak nemame zadanu width ako parameter budeme resizovat z maleho obrazku (ak existuje), je to rychlejsie
		int sessionIdIndex = imagePath.indexOf(";jsessionid");
		if (sessionIdIndex != -1)
		{
			imagePath = imagePath.substring(0, sessionIdIndex);
		}

		if (imagePath != null && imagePath.length()>3)
		{
			//pridaj installName (ak cesta neobsahuje) ak je to asset v /templates adresari
         if (imagePath.startsWith("templates/")) imagePath = WriteTagToolsForCore.getCustomPage("/"+imagePath, request).substring(1);

			//kontrolujem prava
			EditForm ef = PathFilter.isPasswordProtected(imagePath.startsWith("/") == false ? "/"+imagePath : imagePath, request, request.getSession());
			Identity user = UsersDB.getCurrentUser(request);
			if (ef != null && (user == null || ef.isAccessibleFor(user)==false))
			{
				if (PathFilter.doFileForbiddenRedirect(ef, user, imagePath.startsWith("/") == false ? "/"+imagePath : imagePath, request, response)) return;
			}

			//Logger.debug(ThumbServlet.class, "imagePath="+imagePath+" realPath"+realPath);

			//nie je v cache, vygeneruj
			int width = Constants.getInt("imageThumbsWidth");
			int height = Constants.getInt("imageThumbsHeight");

			int ip = -1;
			String fillColor = null;
			int q = -1;

			boolean noIp = "true".equals(request.getParameter("noip"));

			//if nie je potrebny triedy, metody referencovane na tuto metodu ho posielaju
			//if (request != null )
			boolean parsedFromName = false;
			if (request.getRequestURI().startsWith("/tumbn") || (request.getParameter("w")==null && imagePath.indexOf("x")!=-1 && imagePath.indexOf("-")!=-1))
			{
				Logger.debug(ThumbServlet.class, "Parsing file name " + imagePath);
				//skus to parsnut
				try
				{
					Matcher matcher = pattern.matcher(imagePath.substring(imagePath.lastIndexOf("/")+1));
					if (matcher.find())
					{

						Logger.debug(ThumbServlet.class, "Group count: "+matcher.groupCount());
						for (int i=1; i <= matcher.groupCount(); i++)
						{
							//out.println(i+": " + matcher.group());
							Logger.debug(ThumbServlet.class, "Matcher: "+i+": " + matcher.group(i));
						}


						if (matcher.groupCount()==8)
						{
							width = Tools.getIntValue(matcher.group(1), width);
							height = Tools.getIntValue(matcher.group(2), height);

							ip = Tools.getIntValue(matcher.group(4), -1);
							fillColor = matcher.group(6);

							q = Tools.getIntValue(matcher.group(8), -1);

							parsedFromName = true;

							//nastav korektny image path na povodny obrazok
							imagePath = imagePath.substring(0, imagePath.lastIndexOf("-")) + "." + ext;
						}
					}
				}
				catch (Exception e)
				{
					sk.iway.iwcm.Logger.error(e);
				}
			}

			String realPath = PathFilter.getRealPath("/"+imagePath);
			IwcmFile imageFile = new IwcmFile(realPath);
			if (imageFile.exists()==false)
			{
				//kukni ci neexistuje presmerovanie k danemu obrazku, pouzitie thumbServletMissingImg_/images/cesta/ibrazok.jpg je stary format,
				//je potrebne pouzit konstantu thumbServletMissingImg kde na kazdom riadku je cesta|cesta-k-obrazku.jpg
				boolean return404 = true;
				String imgDirPath = "/"+imagePath.substring(0, imagePath.lastIndexOf("/")+1);
				String replaceForMissingPath = Constants.getString("thumbServletMissingImg_"+imgDirPath);

				if (Tools.isEmpty(replaceForMissingPath)) {
					//skus novy format, kde v konf. premennej thumbServletMissingImg je na novom riadku definovane URL ktore sa hladaju
					String thumbServletMissingImg = Constants.getString("thumbServletMissingImg");
					String bestMatch = null;
					if (Tools.isNotEmpty(thumbServletMissingImg)) {
						//eviduje vyraz ktory drzime, aby sa urcil best match
						int maxLenght = 0;
						String[] urls = Tools.getTokens(thumbServletMissingImg, "\n");
						if (urls!=null && urls.length>0) {
							for (String pair : urls) {
								if (Tools.isEmpty(pair)) continue;
								String[] pairArray = pair.split("\\|");
								if (pairArray==null || pairArray.length!=2) continue;

								if (imgDirPath.startsWith(pairArray[0]) && pairArray[0].length()>maxLenght) {
									bestMatch = pairArray[1];
									maxLenght = pairArray[0].length();
								}
							}
						}
					}
					if (bestMatch != null) {
						//nafejkuj, ze tento obrazo mame nastaveny a existuje, potom z neho normalne spravi thumb
						imagePath = bestMatch.substring(1); //odstran prve lomitko, taky je format
						realPath = PathFilter.getRealPath("/"+imagePath);
						imageFile = new IwcmFile(realPath);
						if (imageFile.exists()) return404 = false;
					}
				}
				else
				{
					Logger.debug(ThumbServlet.class, "Replace for missing path '"+imgDirPath+"' - '"+replaceForMissingPath+"'");

					IwcmFile replaceForMissingFile = new IwcmFile(PathFilter.getRealPath("/"+replaceForMissingPath));
					if (replaceForMissingFile.exists())
					{
						Logger.debug(ThumbServlet.class, "Replace image found.");

						imageFile = replaceForMissingFile;
						imagePath = replaceForMissingPath;
						realPath = Tools.replace(PathFilter.getRealPath("/"+replaceForMissingPath), "//", "/");
						return404 = false;
					}
					else
					{
						Logger.debug(ThumbServlet.class, "replace image is missing. path="+replaceForMissingFile.getVirtualPath()+";realPath="+replaceForMissingFile.getAbsolutePath());
					}
				}

				//
				if (return404)
				{
					Logger.debug(ThumbServlet.class, "Image doesn't exists, imagePath="+imagePath+" realPath="+realPath+" qs="+request.getQueryString()+" uri="+request.getRequestURI());
					sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
					response.setStatus(404);
					response.getWriter().print("<html><body>404  -  not found</body></html>");
					return;
				}
			}

			//nie je v cache, vygeneruj
			if (parsedFromName == false)
			{
				width = Tools.getIntValue(request.getParameter("w"), width);
				height = Tools.getIntValue(request.getParameter("h"),height);

				ip = Tools.getIntValue(request.getParameter("ip"), -1);
				fillColor = request.getParameter("c");
				q = Tools.getIntValue(request.getParameter("q"), -1);
			}

			if (fillColor==null || Tools.isEmpty(fillColor) || fillColor.trim().length()!=6) fillColor = FILL_COLOR_DEFAULT;
			fillColor = DocTools.removeChars(fillColor).toLowerCase().trim();
			if (fillColor.length()!=6) fillColor = FILL_COLOR_DEFAULT;

			Logger.debug(ThumbServlet.class, "Farba: "+fillColor);
			//osetrenie stavov
			if (width > Constants.getInt("imageMaxThumbsWidth") || height > Constants.getInt("imageMaxThumbsHeight") || width < 1 || height < 1 || fillColor.length()!=6)
			{
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}

			String realPathSmall = realPathSmall(imagePath, width, height, ip, noIp, fillColor, q);

			//skontroluj cache
			IwcmFile smallImageFile = new IwcmFile(realPathSmall);

			boolean fileExists = imageFile.exists() || smallImageFile.exists();

			//Logger.debug(ThumbServlet.class, "imagePath="+imagePath+" small="+realPathSmall+" imageFile="+imageFile.getAbsolutePath()+" exists="+fileExists);

			if (!fileExists)
			{
				realPathSmall = realPathSmall("/components/_common/mime/big_unknown.gif", width, height, -1, noIp, null, q);
				realPath = Tools.getRealPath("/components/_common/mime/big_unknown.gif");
				imageFile = new IwcmFile(realPath);
				smallImageFile = new IwcmFile(realPathSmall);
			}

			String imgPath = null;

			if (smallImageFile.exists() && imageFile.lastModified() < smallImageFile.lastModified())
			{
				//pouzijeme cache
				imgPath = realPathSmall;
			}


			if (imgPath == null)
			{
				boolean canPost = SpamProtection.canPost("ThumbServlet", realPathSmall, request);
				if (canPost == false)
				{
					//pre cloud mame kvoli screenshoteru vynimku
					canPost = CloudToolsForCore.isInternalIp(request);
				}
				if (canPost == false)
				{
					//ak je to admin nekontroluj pocet requestov
					if (user != null && user.isAdmin()) canPost = true;
				}

				//ochrana pred DOS utokom
				if ( canPost == false && (user==null || user.isAdmin()==false) )
				{
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					return;
				}

				String thumbWriteServer = Constants.getString("thumbWriteServer");
				String thumbWriteNodeName = Constants.getString("thumbWriteNodeName");
				if (Tools.isNotEmpty(thumbWriteServer) && Tools.isNotEmpty(thumbWriteNodeName) && thumbWriteNodeName.equals(Constants.getString("clusterMyNodeName"))==false)
				{
					//toto sa deje na sport24 kde public nody nemaju write pravo, musim teda spravit get na CMS nod aby sa obrazok vygeneroval
					//sprav get na thumb servlet
					String url = thumbWriteServer + imagePath + "?" + request.getQueryString();
					downloadFromAdminNode(imagePath, url, request, response);
					return;
				}
				else
				{
					// ziskaj odkaz na originalny obrazok
					int ret = -1;
					if (ip < 1)
					{
						ret = GalleryDB.resizePicture(realPath, realPathSmall, width, height);
					}
					else
					{
						String testPath = "/"+imagePath;

						GalleryBean bean = GalleryDB.getGalleryBean("/"+imagePath, request);

						//ziskajme orig obrazok, kedze v galerii sa nastavuje crop na orig obrazku
						String origPath = GalleryDB.getImagePathOriginal("/"+imagePath);
						IwcmFile origPathFile = new IwcmFile(Tools.getRealPath(origPath));
						if (origPathFile.exists())
						{
							imageFile = origPathFile;
							testPath = origPath;
						}

						int imageQuality = GalleryDB.getImageQuality(request, width);

						int cleft = 0;
						int ctop = 0;
						//w a h urcime na zaklade aktualneho obrazku (akoze zvoleny cely obrazok)
						int[] imageSize = GalleryDBTools.getImageSize(new IwcmFile(PathFilter.getRealPath(testPath)));

						int cwidth = imageSize[0];
						int cheight = imageSize[1];

						int imgwidth = imageSize[0];
						int imgheight = imageSize[1];

						if (noIp == false && bean != null && bean.getSelectedWidth()>5 && bean.getSelectedHeight()>5)
						{
							if (bean.getSelectedX()>0) cleft = bean.getSelectedX();
							if (bean.getSelectedY()>0) ctop = bean.getSelectedY();
							if (bean.getSelectedWidth()>0) cwidth = bean.getSelectedWidth();
							if (bean.getSelectedHeight()>0) cheight = bean.getSelectedHeight();
						}

						if (ip==1)
						{
							//mame zadany len parameter w, h dopocitame podla pomeru stran povodneho vyrezu
							//http://iwcm.interway.sk:8080/thumb/images/gallery/19-24-mesiacov/DSC06443.JPG?w=200&ip=1
							double pomer = (double)cwidth / (double) cheight;
							height = (int)Math.round(width / pomer);

							Logger.debug(ThumbServlet.class, "vypocitany h="+height);

							ret = GalleryDBTools.cropAndResize(imageFile, cwidth, cheight, cleft, ctop, width, height, null, true, smallImageFile, imageQuality, 1);
						}
						else if (ip==2)
						{
							//mame zadany len parameter h, w dopocitame podla pomeru stran povodneho vyrezu
							//http://iwcm.interway.sk:8080/thumb/images/gallery/19-24-mesiacov/DSC06443.JPG?h=200&ip=2
							double pomer = (double)cwidth / (double) cheight;
							width = (int)Math.round(height * pomer);

							Logger.debug(ThumbServlet.class, "vypocitany w="+width);

							ret = GalleryDBTools.cropAndResize(imageFile, cwidth, cheight, cleft, ctop, width, height, null, true, smallImageFile, imageQuality, 2);
						}
						else if (ip == 3)
						{
							//vyrez sa zmesti cely do zvolenej velkosti w a h ale nemusi vyplnat celu velkost (realne je mensi ako zadane w a h)
							//http://iwcm.interway.sk:8080/thumb/images/gallery/19-24-mesiacov/DSC06443.JPG?w=200&h=200&ip=3
							ret = GalleryDBTools.cropAndResize(imageFile, cwidth, cheight, cleft, ctop, width, height, null, true, smallImageFile, imageQuality, 3);
						}
						else if (ip == 4)
						{
							//vyrez sa zmesti cely do zvolenej velkosti w a h je vycentrovany a zvysok je zafarbeny farbou z parametra c (default biela)
							//http://iwcm.interway.sk:8080/thumb/images/gallery/19-24-mesiacov/DSC06443.JPG?w=200&h=200&ip=4&c=ff0000

							//exactSize - musi byt false, inak sa farba nedoplni
							ret = GalleryDBTools.cropAndResize(imageFile, cwidth, cheight, cleft, ctop, width, height, fillColor, false, smallImageFile, imageQuality, 4);
						}
						else if (ip == 5)
						{
							//zmensi vyrez a vycentruj tak, aby bol dodrzany pomer stran pozadovanej velkosti
							//vyrez moze byt 400x300 a my chceme 300x300, cize ho posunieme o 50 doprava a zmensime na 300x300
							//http://iwcm.interway.sk:8080/thumb/images/gallery/19-24-mesiacov/DSC06443.JPG?w=200&h=200&ip=5

							Logger.debug(ThumbServlet.class, "\n\nURCI OREZ\n width="+width+" height="+height+" cleft="+cleft+" ctop="+ctop+" cwidth="+cwidth+" cheight="+cheight);

							//pomer stran urcim ako pomer sirok jednotlivych elementov
							double pomer = (double)cwidth / (double) width;
							int novycheight = (int)Math.round(height * pomer);

							Logger.debug(ThumbServlet.class, "pomer="+pomer+" novycheight="+novycheight);

							if (novycheight > cheight)
							{
								//presiahlo nam to blok, musime cele urcit pomocou height
								Logger.debug(ThumbServlet.class, "Urcujem podla height");

								pomer = (double)cheight / (double) height;
								//urcime novu sirku
								int novycwidth = (int)Math.round(width * pomer);

								int rozdiel = (int)Math.round( ((cwidth - novycwidth) / 2d) );

								cwidth = novycwidth;
								cleft = cleft + rozdiel;
							}
							else
							{
								int rozdiel = (int)Math.round( ((cheight - novycheight) / 2d) );

								cheight = novycheight;
								ctop = ctop + rozdiel;
							}

							Logger.debug(ThumbServlet.class, "OREZ: cleft="+cleft+" ctop="+ctop+" cwidth="+cwidth+" cheight="+cheight);

							ret = GalleryDBTools.cropAndResize(imageFile, cwidth, cheight, cleft, ctop, width, height, null, true, smallImageFile, imageQuality, 5);
						}
						else if (ip == 6)
						{
							//zvoleny vyrez bude komplet vo vysledku ale celkovo sa vyrez zvacsi podla pomeru stran pozadovaneho obrazku
							//http://iwcm.interway.sk:8080/thumb/images/gallery/19-24-mesiacov/DSC06443.JPG?w=200&h=200&ip=6

							Logger.debug(ThumbServlet.class, "\n\nURCI OREZ\n width="+width+" height="+height+" cleft="+cleft+" ctop="+ctop+" cwidth="+cwidth+" cheight="+cheight+" imgwidth="+imgwidth+" imgheight="+imgheight);

							//len pre kontrolu
							double pomerStran = (double)width / (double)height;
							Logger.debug(ThumbServlet.class, "pomerStran="+pomerStran);


							//pomer stran urcim ako pomer sirok jednotlivych elementov
							double pomer = (double)cwidth / (double) width;
							int novycheight = (int)Math.round(height * pomer);
							int novycwidth = cwidth;

							Logger.debug(ThumbServlet.class, "pomer="+pomer+" novycheight="+novycheight);

							if (novycheight < cheight)
							{
								//nepresiahlo nam to blok, musime cele urcit pomocou height (chceme tam mat cely vyznaceny blok)
								Logger.debug(ThumbServlet.class, "Urcujem podla height");

								pomer = (double)cheight / (double) height;
								//urcime novu sirku
								novycwidth = (int)Math.round(width * pomer);
								novycheight = cheight;
							}

							//posunieme left a top o rozdiely
							int posunLeft = (int)Math.round( ((novycwidth - cwidth) / 2d) );
							int posunTop = (int)Math.round( ((novycheight - cheight) / 2d) );

							cleft = cleft - posunLeft;
							ctop = ctop - posunTop;

							if (cleft < 0) cleft = 0;
							if (ctop < 0) ctop = 0;

							cwidth = novycwidth;
							cheight = novycheight;

							Logger.debug(ThumbServlet.class, "OREZ: cleft="+cleft+" ctop="+ctop+" cwidth="+cwidth+" cheight="+cheight);

							if (cwidth + cleft > imgwidth)
							{
								if (cwidth <= imgwidth)
								{
									//posunieme left
									posunLeft = (cwidth+cleft) - imgwidth;
									cleft = cleft - posunLeft;
								}
								else
								{
									//musime cele zmensit
									cwidth = imgwidth;
									cheight = (int)Math.round(cwidth / pomerStran);
								}
							}

							Logger.debug(ThumbServlet.class, "OREZ: cleft="+cleft+" ctop="+ctop+" cwidth="+cwidth+" cheight="+cheight);

							if (cheight + ctop > imgheight)
							{
								if (cheight <= imgheight)
								{
									//posunieme top
									posunTop = (cheight+ctop) - imgheight;
									ctop = ctop - posunTop;
								}
								else
								{
									//musime cele zmensit
									cheight = imgheight;
									cwidth = (int)Math.round(cheight * pomerStran);
								}
							}

							//len pre kontrolu
							double cPomerStran = (double)cwidth / (double)cheight;
							Logger.debug(ThumbServlet.class, "cpomerStran="+cPomerStran);

							Logger.debug(ThumbServlet.class, "OREZ: cleft="+cleft+" ctop="+ctop+" cwidth="+cwidth+" cheight="+cheight);

							ret = GalleryDBTools.cropAndResize(imageFile, cwidth, cheight, cleft, ctop, width, height, null, true, smallImageFile, imageQuality, 6);
						}
						else
						{
							response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
							return;
						}
					}
					Logger.debug(ThumbServlet.class, "ret=" + ret);
					if (ret == 0 || ret == 1)
					{
						imgPath = realPathSmall;
					}
					else
					{
						imgPath = Tools.getRealPath("/components/_common/mime/big_unknown.gif");
					}
				}
			}

			Logger.debug(ThumbServlet.class, "imgPath="+imgPath);
			IwcmFile inFile = null;
         if (imgPath != null)
         {
            inFile = new IwcmFile(imgPath);
         }
         if (inFile != null && inFile.exists())
         {
            //response.setHeader("Pragma", "No-Cache");
            //response.setDateHeader("Expires", 0);
            //response.setHeader("Cache-Control", "no-Cache");

            String mimeType = Constants.getServletContext().getMimeType(imagePath.toLowerCase());
            if (Tools.isEmpty(mimeType)) mimeType = "application/octet-stream";
            response.setContentType(mimeType);

            PathFilter.setStaticContentHeaders(imagePath, UsersDB.getCurrentUser(request), request, response);

            ServletOutputStream out = response.getOutputStream();
            byte[] buff = new byte[64000];
            IwcmInputStream fis = new IwcmInputStream(inFile);
            int len;
            while ((len = fis.read(buff)) != -1)
            {
               out.write(buff, 0, len);
            }
            fis.close();
            //out.close();
            //return;
         }
         else
         {
         	response.setStatus(HttpServletResponse.SC_NOT_FOUND);
         }
		}
	}

	private static boolean downloadFromAdminNode(String imagePath, String url, HttpServletRequest request, HttpServletResponse response)
	{
		if (url.startsWith("http://") || url.startsWith("https://"))
		{
			try
			{
				url = Tools.natUrl(url);

				if (url.startsWith("https://"))
				{
					Tools.doNotVerifyCertificates();
				}

				Logger.debug(Tools.class, "DownloadUrl: " + url);

				//body obsahuje URL adresu, ktoru je treba stiahnut
				HttpURLConnection conn = null;
				URL urlObj = new URL(url);
				conn = (HttpURLConnection) urlObj.openConnection();

				conn.setAllowUserInteraction(false);
				conn.setDoInput(true);
				conn.setDoOutput(false);
				conn.connect();

				String mimeType = Constants.getServletContext().getMimeType("/"+imagePath.toLowerCase());
            if (Tools.isEmpty(mimeType)) mimeType = "application/octet-stream";
            response.setContentType(mimeType);

            PathFilter.setStaticContentHeaders("/"+imagePath, null, request, response);

				BufferedInputStream is = new BufferedInputStream(conn.getInputStream());
				ServletOutputStream os = response.getOutputStream();

				byte[] buffer = new byte[8000];
				int n = 0;
				while (true)
				{
					 n = is.read(buffer);
					 if (n < 1) break;
					 os.write(buffer, 0, n);
				}
				is.close();
				os.close();

				return true;
			}
			catch (Exception ex)
			{
				Logger.error(ThumbServlet.class,"ERROR downloadUrl("+url+")");
				sk.iway.iwcm.Logger.error(ex);
			}
		}
		return false;
	}

	/**
	 * vrati cestu k obrazku v pripade ak ide o obrazov pre bod zaujmu
	 * @param imagePath
	 * @param width
	 * @param height
	 * @return
	 */
	private static String realPathSmall(String imagePath, int width, int height, int ip, boolean noIp, String fillColor, int quality)
	{
		String realPathSmall = Tools.getRealPath(Constants.getString("thumbServletCacheDir")+imagePath);

		return getImagePathCache(realPathSmall, width, height, ip, noIp, fillColor, quality);
	}

	/**
	 * Vrati cestu k suboru v cache upravenu o rozmer, ip, farbu a qualitu
	 * @param realPathSmall
	 * @param width
	 * @param height
	 * @param ip
	 * @param fillColor
	 * @param quality
	 * @return
	 */
	public static String getImagePathCache(String realPathSmall, int width, int height, int ip, String fillColor, int quality) {
		return getImagePathCache(realPathSmall, width, height, ip, false, fillColor, quality);
	}

	/**
	 * Vrati cestu k suboru v cache upravenu o rozmer, ip, farbu a qualitu
	 * @param realPathSmall
	 * @param width
	 * @param height
	 * @param ip
	 * @param noIp
	 * @param fillColor
	 * @param quality
	 * @return
	 */
	public static String getImagePathCache(String realPathSmall, int width, int height, int ip, boolean noIp, String fillColor, int quality)
	{
		//uprav cache nazov
		try
		{
			String qualityName = "";
			if (quality > 10 && quality <=100) qualityName = "q"+quality;

			String fillColorName = "";
			if (Tools.isNotEmpty(fillColor) && FILL_COLOR_DEFAULT.equals(fillColor)==false) fillColorName = "c"+fillColor;

			String ipName = "";
			if (ip>0) ipName = "ip"+ip;
			if (noIp) ipName += "n";

			//najdi poslednu bodku v nazve suboru
			int i = realPathSmall.lastIndexOf('.');
			//ak bodka v nazve nie je, tak vrat rovno subor, je to nezmysel
			if (i == -1) return realPathSmall;

			//nemame zadany parameter ip, toto mozu byt napr. thumb obrazky v admin casti kde sa pouziva len w a h parameter
			realPathSmall = realPathSmall.substring(0, i) + "-"+width+"x"+height+ipName+fillColorName+qualityName+realPathSmall.substring(i);

		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return realPathSmall;
	}
}
