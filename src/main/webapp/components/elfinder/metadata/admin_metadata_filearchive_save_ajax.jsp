<%
    sk.iway.iwcm.Encoding.setResponseEnc(request, response, "application/json");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,java.util.*" %><%@
page import="java.io.File" %><%@
page import="sk.iway.iwcm.components.file_archiv.FileArchivatorKit" %><%@
page import="sk.iway.iwcm.i18n.Prop" %><%@
page import="sk.iway.iwcm.components.file_archiv.FileArchivatorBean" %><%@
page import="sk.iway.iwcm.users.UserDetails" %><%@
page import="sk.iway.iwcm.users.UsersDB" %><%@
page import="java.text.SimpleDateFormat" %><%@
page import="sk.iway.iwcm.components.file_archiv.ResultArchivBean" %><%@
page import="sk.iway.iwcm.components.file_archiv.FileArchivatorDB" %><%@
page import="sk.iway.iwcm.common.FileBrowserTools" %><%@
page import="sk.iway.iwcm.io.IwcmFile" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms="menuFbrowser|menuWebpages"/><%!

public static String addResponseOK(String resultParam, int i)
{
    String result = resultParam;
    result += "\"fab_"+i+"\":";
    result += "\"ok\",";
    return result;
}

public static String addResponseError(String resultParam, int i, List<String> errorsList)
{
    String result = resultParam;
    result += "\"fab_" + i + "\":[";
    for(String er: errorsList)
    {
        String error = er;
        error = Tools.replace(Tools.replace(error,"[", ""),"]","");

        result += "\"<p>"+error+ "</p>\",";
    }
    result = result.substring(0,result.length()-1) + "],";
    return result;
}


%><%
  Identity user = UsersDB.getCurrentUser(request);
  SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
  if(user == null || !user.isAdmin())
  {
      out.print("User not logged");
      return;
  }
  Enumeration e = request.getParameterNames();
  String nameParam ="";
  while(e.hasMoreElements())
  {
   nameParam = (String)e.nextElement();
   //out.print(nameParam+" : "+request.getParameter(nameParam)+"<br>\n");
  }

  // zmazeme subor ktory bol nahraty cez elfinder ale nebol ulozeny a zvalidovany cez file archiv a user zavrel okno.
  if(request.getParameter("deleteTempFiles") != null && !"".equals(request.getParameter("deleteTempFiles")))
  {
      String tmpFiles = request.getParameter("deleteTempFiles");
      for(String filePath:Tools.getTokens(tmpFiles,","))
      {
          //Ak ma nepovoleny symbol, mohli nam podhodit cestu, tak nemazeme.
          if(FileBrowserTools.hasForbiddenSymbol(filePath))
          {
              Logger.debug(null,"admin_metadata_filearchive_save_ajax.jsp -> nepovoleny znak v ceste k suboru: "+filePath+" preskakujem.");
              continue;
          }

          //Ak NEexistuje zmazeme
          String archivFilePath = filePath;
          if(filePath.length() > 2 && filePath.startsWith("/"))
          {
              archivFilePath = archivFilePath.substring(1,archivFilePath.length());
          }
          if(!FileArchivatorKit.existsPathInDB(archivFilePath))
          {
              IwcmFile iwFile = new IwcmFile(Tools.getRealPath(filePath));
              if (iwFile.isFile())
              {
                  Logger.debug("admin_metadata_filearchive_save_ajax.jsp","mazeme subor: "+iwFile.getAbsolutePath());
                  iwFile.delete();
              }
          }
      }
      return;
  }

  String fileName = "";
  String filePath = "";
  String uploadedFilePath = "";
//ziskat fab
String result = "{";
boolean isSuccess = true;
for(int i=0;i<100;i++)
{

    uploadedFilePath = Tools.getParameter(request,"uploadedFilepath_"+i);
    if(Tools.isNotEmpty(uploadedFilePath) && uploadedFilePath.indexOf("/") >= 0)
    {
        FileArchivatorBean fab = new FileArchivatorBean();
        boolean isEdit = false;
        if(Tools.getParameter(request, "fab_id_"+i) != null || FileArchivatorKit.existsPathInDB(uploadedFilePath))
        {
            int editId = Tools.getIntValue(Tools.getParameter(request, "fab_id_"+i),-1);
            if(editId <= 0)
            {
                FileArchivatorBean fabBean = FileArchivatorDB.getByUrl(uploadedFilePath);
                if(fabBean != null)
                {
                    editId = fabBean.getId();
                }
            }
            if(editId > 0)
            {
                fab = FileArchivatorDB.getInstance().getById(editId);
                if(fab == null)
                {
                    fab = new FileArchivatorBean();
                }
                else
                {
                    isEdit = true;
                }
            }
        }
        filePath= uploadedFilePath.substring(1,uploadedFilePath.lastIndexOf("/")+1);
        fileName = uploadedFilePath.substring(uploadedFilePath.lastIndexOf("/")+1,uploadedFilePath.length());

        //FileArchivatorBean fab =
        fab.setFileName(fileName);
        fab.setFilePath(filePath);
        fab.setVirtualFileName(Tools.getParameter(request,"virtual_file_name_"+i));
        fab.setDomain(Tools.getParameter(request,"domain_"+i));
        fab.setProduct(Tools.getParameter(request,"product_"+i));
        fab.setCategory(Tools.getParameter(request,"category_"+i));
        fab.setProductCode(Tools.getParameter(request,"productCode_"+i));
        String shFile = Tools.getParameter(request,"showFile_"+i);
        if(shFile != null && ("true".equalsIgnoreCase(shFile) ||"checked".equalsIgnoreCase(shFile)))
        {
            fab.setShowFile(true);
        }
        else
        {
            fab.setShowFile(false);
        }

        try
        {
            if(request.getParameter("validFrom_" + i) != null)
            {
                fab.setValidFrom(sdf.parse(Tools.getParameter(request, "validFrom_" + i)));
            }

            if(request.getParameter("validTo_" + i) != null)
            {
                fab.setValidTo(sdf.parse(Tools.getParameter(request, "validTo_" + i)));
            }
        }
        catch (Exception ex)
        {
            //sk.iway.iwcm.Logger.error(ex);
        }
        fab.setPriority(Tools.getIntValue(Tools.getParameter(request,"priority_"+i),-1));
        fab.setNote(Tools.getParameter(request,"note_"+i));
        fab.setFieldA(Tools.getParameter(request,"fieldA_"+i));
        fab.setFieldB(Tools.getParameter(request,"fieldB_"+i));

        FileArchivatorKit fak = new FileArchivatorKit(Prop.getInstance(request));
        ResultArchivBean rab = fak.prepareAndValidate(fab, -1, user, isEdit);
        if(rab.isSuccess())
        {
            fab.save();
            result = addResponseOK(result, i)/*+"}"*/;
        }
        else
        {
            result = addResponseError(result , i, rab.getErrors());
            //result = result.substring(0,result.length()) /*+ "}"*/;
            isSuccess = false;
        }
    }
}

    //if(!isSuccess)
   // {
        result = result.substring(0, result.length()-1);
    //}
    result += "}";
//TODO zistit ci je to nove alebo nahrazujuci subor
//zvalidovat
//ulozit
%><%=result%>