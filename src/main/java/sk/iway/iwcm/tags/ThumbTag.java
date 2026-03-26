package sk.iway.iwcm.tags;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.JspTagException;
import jakarta.servlet.jsp.tagext.BodyContent;
import jakarta.servlet.jsp.tagext.BodyTagSupport;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.editor.ThumbServlet;
import sk.iway.iwcm.filebrowser.EditForm;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.users.UsersDB;

/**
 *  ThumbTag.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2013
 *@author       $Author: Branislav Hric $
 *@version      $Revision: 1.3 $
 *@created      Date: 18.4.2013 8:38:28
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class ThumbTag extends BodyTagSupport
{
	private static final long serialVersionUID = -8327273293453282301L;

	private int w = Constants.getInt("imageThumbsWidth");
	private int h = Constants.getInt("imageThumbsHeight");
	private String c = null;
	private int q = -1;
	private int ip = -1;

	@Override
	public void release()
	{
		super.release();
		w = Constants.getInt("imageThumbsWidth");
		h = Constants.getInt("imageThumbsHeight");
		c = null;
		q = -1;
		ip = -1;
	}

	@Override
	public int doAfterBody() throws JspTagException
	{
		BodyContent bc = getBodyContent();
		String imagePath = bc.getString();
		bc.clearBody();
		try
		{
			if(Tools.isNotEmpty(imagePath))
			{
				String thumbExternalServer = Constants.getString("thumbExternalServer");

				HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
				//kontrola prav
				EditForm ef = PathFilter.isPasswordProtected(imagePath.startsWith("/") == false ? "/"+imagePath : imagePath, request, request.getSession());
				Identity user = UsersDB.getCurrentUser(request);
				if (ef != null && (user == null || ef.isAccessibleFor(user)==false))
				{
					getPreviousOut().print("forbidden");
				}
				else
				{
					if (Tools.isEmpty(c) || c.trim().length()!=6) c = "ffffff";
					c = DocTools.removeChars(c).toLowerCase().trim();
					if (c.length()!=6) c = "ffffff";

					if (w > Constants.getInt("imageMaxThumbsWidth") || h > Constants.getInt("imageMaxThumbsHeight") || w < 1 || h < 1 || c.length()!=6)
					{
						getPreviousOut().print(imagePath);
					}
					else
					{
						boolean imageFound = false;
						//ak mame thumb server
						if(Tools.isNotEmpty(thumbExternalServer))
						{
							IwcmFile imageFile = new IwcmFile(Tools.getRealPath(imagePath));
							String realPathSmall = realPathSmall(true, imagePath, w, h, ip, c, q);
							IwcmFile smallImageFile = new IwcmFile(realPathSmall);
							//zisti ci existuje uz obrazok s danymi parametrami v cache a vrat nan odkaz
							if(smallImageFile.exists() && imageFile.lastModified() < smallImageFile.lastModified())
							{
								imageFound = true;
								StringBuilder imgUrl = new StringBuilder("");
								imgUrl.append(thumbExternalServer.endsWith("/") ? thumbExternalServer.substring(0, thumbExternalServer.length()-1) : thumbExternalServer);
								//JaHu CACHE_DIR odstranuje, preto sa to pri finalnom vyskladani preskakuje
								//imgUrl.append(CACHE_DIR.endsWith("/") ? CACHE_DIR.substring(0, CACHE_DIR.length()-1) : CACHE_DIR);
								imgUrl.append(realPathSmall(false, imagePath, w, h, ip, c, q));
								getPreviousOut().print(imgUrl.toString());
							}
						}

						//ak obrazok neexistuje, vygeneruj ho standartne cez /thumb/...
						if(imageFound == false)
						{
							StringBuilder imgUrl = new StringBuilder("/thumb");
							imgUrl.append(imagePath);
							imgUrl.append("?w="+w);
							imgUrl.append("&h="+h);
							if(ip > 0) imgUrl.append("&ip="+ip);
							if(ip==2) imgUrl.append("&c="+c);
							if(ip > 0 && q > 10 && q <= 100) imgUrl.append("&q="+q);
							getPreviousOut().print(imgUrl.toString());
						}
					}
				}
			}
		}
		catch (IOException e)
		{
			throw new JspTagException("ThumbTag: " +	e.getMessage());
		}
		return SKIP_BODY;
	}

	/**
	 * vrati cestu k obrazku v pripade ak ide o obrazov pre bod zaujmu
	 * @param imagePath
	 * @param width
	 * @param height
	 * @return
	 */
	private static String realPathSmall(boolean returnRealPath, String imagePath, int width, int height, int ip, String fillColor, int quality)
	{
		String realPathSmall = returnRealPath ? Tools.getRealPath(Constants.getString("thumbServletCacheDir")+imagePath) : imagePath;
		//uprav cache nazov
		return ThumbServlet.getImagePathCache(realPathSmall, width, height, ip, false, fillColor, quality);
	}

	public int getW()
	{
		return w;
	}

	public void setW(int w)
	{
		this.w = w;
	}

	public int getH()
	{
		return h;
	}

	public void setH(int h)
	{
		this.h = h;
	}

	public int getIp()
	{
		return ip;
	}

	public void setIp(int ip)
	{
		this.ip = ip;
	}

	public String getC()
	{
		return c;
	}

	public void setC(String c)
	{
		this.c = c;
	}

	public int getQ()
	{
		return q;
	}

	public void setQ(int q)
	{
		this.q = q;
	}

}
