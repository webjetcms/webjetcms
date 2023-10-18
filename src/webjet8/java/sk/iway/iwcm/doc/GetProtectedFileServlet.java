package sk.iway.iwcm.doc;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.filebrowser.EditForm;
import sk.iway.iwcm.io.IwcmInputStream;
import sk.iway.iwcm.system.context.ContextFilter;
import sk.iway.iwcm.users.UserGroupDetails;
import sk.iway.iwcm.users.UserGroupsDB;

/**
 *  Servlet na ziskanie suboru z /files/protected, ktore su chranene heslom
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       not attributable
 *@version      1.0
 *@created      Štvrtok, 2003, júl 3
 *@modified     $Date: 2003/08/19 06:53:43 $
 */

public class GetProtectedFileServlet extends HttpServlet
{
	private static final long serialVersionUID = -6417564199151540202L;
	/**
    *  Description of the Field
    */
   public final static String DIR_NAME = "/files/protected";


   /**
    *  Description of the Method
    *
    *@exception  ServletException  Description of the Exception
    */
   @Override
	public void init() throws ServletException { }

   /**
    *  Description of the Method
    *
    *@param  request               Description of the Parameter
    *@param  response              Description of the Parameter
    *@exception  ServletException  Description of the Exception
    *@exception  IOException       Description of the Exception
    */
   @Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
   {
      String url = request.getRequestURI();
      if (ContextFilter.isRunning(request)) url = ContextFilter.removeContextPath(request.getContextPath(), url);
      //Logger.println(this,"RequestServlet: url="+url);

      if (url == null || url.compareTo("/") == 0)
      {
         getServletContext().getRequestDispatcher("/index.jsp").forward(request, response);
         return;
      }

      request.getSession().setAttribute("afterLogonRedirect", url);

      //Logger.println(this,"-->GetProtectedFileServlet: " + url);

      EditForm ef = PathFilter.isPasswordProtected(url, request);

      if (url.startsWith(DIR_NAME))
      {
      	Logger.println(this,"mam dir name, url="+url);
         //ziskaj identitu
         Identity user = (Identity) request.getSession().getAttribute(Constants.USER_KEY);
         if (user == null)
         {
             PathFilter.doFileForbiddenRedirect(ef, user, url, request, response);
            return;
         }

         //otestuj, ci je subor dostupny pre daneho usera
         if (!user.isAdmin())
         {
            //ziskaj nazov adresara
            String dirName = "";
            try
            {
               int index = url.indexOf("/", DIR_NAME.length() + 1);
               if (index != -1)
               {
                  dirName = url.substring(DIR_NAME.length()+1, index);
               }
            }
            catch (Exception ex)
            {
               sk.iway.iwcm.Logger.error(ex);
            }

             Logger.println(this,"nie som admin, dirName="+dirName);

            if (dirName != null && dirName.length() > 0)
            {
               if (dirName.equals("users"))
               {
                  //je to adresar pre konkretneho usera, skontroluj ci je to prave prihlaseny
               	String loginName = DocTools.removeCharsDir(user.getLoginName()).toLowerCase();
               	loginName = DB.internationalToEnglish(loginName).toLowerCase();
                  if (url.startsWith(DIR_NAME + "/" + dirName + "/" + loginName + "/") == false)
                  {
                     PathFilter.doFileForbiddenRedirect(ef, user, url, request, response);
                     return;
                  }
               }
               else
               {
                  //otestuj, ci user ma pravo na tento adresar
                  UserGroupsDB userGroupsDB = UserGroupsDB.getInstance(getServletContext(), false, DBPool.getDBName(request));
                  UserGroupDetails userGroup = userGroupsDB.getUserGroupDirName(dirName);
                  if (userGroup != null)
                  {
                     if (user.isInUserGroup(userGroup.getUserGroupId()) == false)
                     {
                        //user do danej grupy nepatri, nema tu co hladat
                        request.getSession().setAttribute("password_protected", userGroup.getUserGroupName());
                        if (PathFilter.doFileForbiddenRedirect(ef, user, url, request, response))
                        {
                            return;
                        }
                     }
                  }
                  else
                  {
                     //user do danej grupy nepatri, nema tu co hladat
                     if (PathFilter.doFileForbiddenRedirect(ef, user, url, request, response))
                     {
                         return;
                     }
                  }
               }
            }
         }

         Logger.println(this,"preposielam na vystup, url="+url);

         //preposli to na vystup
         String realPath = Tools.getRealPath(url);
         File inFile = null;
         if (realPath != null)
         {
            inFile = new File(realPath);
         }

         Logger.println(this,"testujem: " + realPath);

         if (inFile != null && inFile.exists())
         {
            //response.setHeader("Pragma", "No-Cache");
            //response.setDateHeader("Expires", 0);
            //response.setHeader("Cache-Control", "no-Cache");

            String mimeType = "application/octet-stream";

            try
				{
					mimeType = getServletContext().getMimeType(url.toLowerCase());
				}
				catch (Exception ex)
				{
					sk.iway.iwcm.Logger.error(ex);
				}

				if (Tools.isEmpty(mimeType)) mimeType = "application/octet-stream";

            //debilne WebSphere nevie zistit mimeType

            response.setContentType(mimeType);
            ServletOutputStream out = response.getOutputStream();
            byte buff[] = new byte[64000];
            IwcmInputStream fis = new IwcmInputStream(realPath);
            int len;
            while ((len = fis.read(buff)) != -1)
            {
               out.write(buff, 0, len);
            }
            fis.close();
            //out.close();

            //Logger.println(this,"hotovo");

            return;
         }
         else
         {
         	Logger.debug(GetProtectedFileServlet.class, "forwarding to 404.jsp");
            getServletContext().getRequestDispatcher("/404.jsp").forward(request, response);
            return;
         }
      }
      else
      {
      	//nacitaj subor a posli na vystup
      	String realPath = Tools.getRealPath(url);
         File inFile = null;
         if (realPath != null)
         {
            inFile = new File(realPath);
         }

         //Logger.println(this,"testujem: " + realPath);

         if (inFile != null && inFile.exists())
         {
            //response.setHeader("Pragma", "No-Cache");
            //response.setDateHeader("Expires", 0);
            //response.setHeader("Cache-Control", "no-Cache");

            String mimeType = "application/octet-stream";

            try
				{
					mimeType = getServletContext().getMimeType(url.toLowerCase());
				}
				catch (Exception ex)
				{
					sk.iway.iwcm.Logger.error(ex);
				}

				if (mimeType == null || mimeType.length()==0)
				{
					mimeType = "application/octet-stream";
				}

            //debilne WebSphere nevie zistit mimeType

            response.setContentType(mimeType);
            ServletOutputStream out = response.getOutputStream();
            byte buff[] = new byte[64000];
            IwcmInputStream fis = new IwcmInputStream(realPath);
            int len;
            while ((len = fis.read(buff)) != -1)
            {
               out.write(buff, 0, len);
            }
            fis.close();
            //out.close();

            //Logger.println(this,"hotovo");

            return;
         }
      }

      Logger.debug(GetProtectedFileServlet.class, "forwarding to 404.jsp");
      getServletContext().getRequestDispatcher("/404.jsp").forward(request, response);

      //Logger.println(this,"subor nenajdeny");
      return;
   }

   /**
    *  Description of the Method
    */
   @Override
	public void destroy() { }

}
