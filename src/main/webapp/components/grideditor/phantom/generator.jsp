<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %><%@
page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@ page import="sk.iway.iwcm.users.UsersDB" %>
<%@ page import="sk.iway.iwcm.grideditor.generator.ScreenshotGenerator" %>
<%@ page import="sk.iway.iwcm.grideditor.generator.ScreenShotPropBean" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="sk.iway.iwcm.io.IwcmFile" %>
<%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="menuWebpages"/><%!
public static String getVal(String val, HttpServletRequest req)
{
    return getVal(val,req,null);
}

public static String getVal(String val, HttpServletRequest req, String defaultValue)
{
    if(Tools.isEmpty(Tools.getRequestParameter(req, val)))
    {
        if(Tools.isNotEmpty(defaultValue) )
        {
            return defaultValue;
        }
        return "";
    }
    return Tools.getRequestParameter(req, val);
}
%><%
    Identity userr =  UsersDB.getCurrentUser(request);
    if(userr == null || !userr.isAdmin())
    {
        %><iwcm:text key="error.userNotLogged"/><%
        return;
    }

    boolean error = false;
    boolean send = Tools.getRequestParameter(request, "send") != null;
    boolean bloky = "bloky".equals(Tools.getRequestParameter(request, "dest"));
    String initErrors = "";


    IwcmFile f1 = new IwcmFile(Constants.getString("grideditorPhantomjsPath")+ScreenshotGenerator.getRuntimeFile());
    IwcmFile f2 = new IwcmFile("C:/"+ScreenshotGenerator.getRuntimeFile());
    if(!f1.exists() && !f2.exists())
    {

        initErrors = "Binarny subor "+ScreenshotGenerator.getRuntimeFile()+" nebol najdeny tu: <br><strong>"+"C:/"+ScreenshotGenerator.getRuntimeFile()+"</strong><br>";

        if(Tools.isNotEmpty(Constants.getString("grideditorPhantomjsPath")))
        {
            initErrors += " alebo tu "+Constants.getString("grideditorPhantomjsPath")+ScreenshotGenerator.getRuntimeFile();
        }
        initErrors += " prosim nahrajte ho z umiestnenia: <br><strong>\\\\servis.iway.local\\Web\\interway\\grideditor\\</strong><br>";
        error = true;
    }


    if(!bloky && send && (Tools.getRequestParameter(request, "screenshotGenerateFromUrl") == null || Tools.getRequestParameter(request, "screenshotGenerateFromUrl").length() <1))
    {
        initErrors += "Url musi byt zadana<br>";
        error = true;
    }

    if(!bloky && send && (Tools.getRequestParameter(request, "screenshotSavePath") == null || Tools.getRequestParameter(request, "screenshotSavePath").length() <1))
    {
        initErrors += "Cesta kam mame ulozit obrazok musi byt zadana<br>";
        error = true;
    }

    if(Constants.getString("xsrfUrlExceptionSystem").indexOf("/admin/grideditor/category") == -1)
    {
        initErrors += "Spustite <a href=\"/localconf.jsp\" target=\"_blank\">localconf</a>, je potrebne zinicializovat konf. premennu <em>xsrfUrlExceptionSystem</em> <br>";
        error = true;
    }

    if(Constants.getString("xsrfUrlException").indexOf("/admin/grideditor/category") == -1)
    {
        initErrors += "Spustite <a href=\"/localconf.jsp\" target=\"_blank\">localconf</a>, je potrebne zinicializovat konf. premennu <em>xsrfUrlException</em> <br>";
        error = true;
    }

    if(Tools.isNotEmpty(initErrors))
    {%>
        <div style="border:2px solid red;border-radius: 5px;display: inline-block; padding: 8px;"><%=initErrors%></div>

        <%
    }

if(bloky || !error && send)
{
    //lebo ak je v JAR subore phantomjs ho neviem pouzit, musime vytvorit
    boolean screenshotJsCreated = false;
    boolean screenshotJsDirCreated = false;
    java.io.File screenshotJs = new File(Tools.getRealPath("/components/grideditor/phantom/phantomjs_screenshot.js"));
    if (screenshotJs.exists()==false) {
        if (screenshotJs.getParentFile().exists()==false) {
            screenshotJs.getParentFile().mkdirs();
            screenshotJsDirCreated = true;
        }
        if (screenshotJs.exists()==false)
        {
            screenshotJs.createNewFile();
        }
        FileTools.copyFile(new IwcmFile(screenshotJs), new IwcmFile(screenshotJs));
        screenshotJsCreated = true;
    }


    String screenshotGenerateFromUrl = Tools.getRequestParameter(request, "screenshotGenerateFromUrl");
    String screenshotSavePath = Tools.getRequestParameter(request, "screenshotSavePath");

    if(screenshotGenerateFromUrl.startsWith("/"))
        screenshotGenerateFromUrl = Tools.getBaseHref(request)+ screenshotGenerateFromUrl;

    ScreenShotPropBean sb = new ScreenShotPropBean(screenshotGenerateFromUrl);
    sb.setCookieHtmlData(URLEncoder.encode("test_cookie", "UTF-8"));
    sb.setSaveImageToPath(screenshotSavePath);
    sb.setDebug(true);
    sb.setImageWidth(Tools.getIntValue(Tools.getRequestParameter(request, "width"), 1000));
    sb.setImageHeight(Tools.getIntValue(Tools.getRequestParameter(request, "height"), 600));
    sb.setZoom(Tools.getIntValue(Tools.getRequestParameter(request, "zoom"),1));
    sb.setTimeDelayMilisecond(Tools.getIntValue(Tools.getRequestParameter(request, "timeMilis"),3000));
    if(Tools.getRequestParameter(request, "height_auto") != null)
    {
        sb.setAutoHeigth(true);
    }
    //nastavenie textu
    //IwayProperties ip = new IwayProperties();
   // ip.setProperty("phantomjs.html_data.template", html_data);
    //PropDB.save(ip, "sk", null, null, false);
    request.setAttribute("screenshotProperties",sb);
    if("url".equals(Tools.getRequestParameter(request, "dest")))
    {
        ScreenShotPropBean screenResult = ScreenshotGenerator.generatePreview(sb);
        if(screenResult.isGenerated())
        {
            out.print("Vygenerovane:  "+screenResult.getSaveImageToPath()+"<hr><br>");
        }
        else
        {
            out.print("Pri generovani nastali chyby:");
            for(String err:screenResult.getErrors())
            {
                out.print(err+"<br>");
            }

            for(String err:screenResult.getPhantomErrors())
            {
                out.print(err+"<br>");
            }
        }
    }
    else
    {%>
        <%@include file="generate_image.jsp" %>  <%
    }

    if (screenshotJsCreated) {
        screenshotJs.delete();
        if (screenshotJsDirCreated) screenshotJs.getParentFile().delete();
    }
}

%>
<%=Tools.insertJQuery(request)%>
<script type="text/javascript">
    function hideInputs(){
        document.getElementById('inputsUrls').style.display ='none';
        $('input:checkbox').prop('checked', 'checked');
    }
    function showInputs(){
        document.getElementById('inputsUrls').style.display = 'block';
        $('input:checkbox').prop('checked', false);
    }

</script>
<style type="text/css">
    input {margin-top:6px;
        margin-right: 10px;}
    div, form  {margin:10px;}
    div#inputsUrls {margin: 0px;}
</style>

<form name="phantom_generator" method="post"  >
    <input type="radio" id="radioDest2" name="dest" onclick="hideInputs()" checked class="bloky" value="bloky"><label for="radioDest2">Generovat Grid bloky</label><br>
    <input type="radio" id="radioDest1" name="dest" onclick="showInputs()" value="url"><label for="radioDest1">Generovat nahlad URL</label><br>
    <div id="inputsUrls">
        <input type="text" size="50"  name="screenshotGenerateFromUrl" value="<%=getVal("screenshotGenerateFromUrl",request)%>" placeholder="<%=Tools.getBaseHref(request)%>"> - URL z ktorej generujeme screen<br>
        <input type="text" size="50"  name="screenshotSavePath" value="<%=getVal("screenshotSavePath",request)%>" placeholder="/files/images/phantom/image_hp.jpg"> - cesta kam mame ulozit obrazok.jpg<br>
    </div>
    <input type="text" name="timeMilis" placeholder="<%=3000%>"  value="<%=getVal("timeMilis",request,"3000")%>"> - delay na vygenerovanie stranky (cas na zbehnutie JS skriptov a stiahnutie obrazkov)<br>
    <input type="text" name="width" placeholder="1920" value="<%=getVal("width",request,"1000")%>"> - sirka nahladu<br>
    <input type="text" name="height" placeholder="1080" value="<%=getVal("height",request,"600")%>"> - vyska nahladu&nbsp;&nbsp; <input type="checkbox" <%="on".equals(Tools.getRequestParameter(request, "height_auto")) ? "checked":""%> id="autoHide" name="height_auto"> generovat vysku automaticky<br>
    <input type="text" name="zoom" placeholder="1" value="<%=getVal("zoom",request,"1")%>"> - zoom<br>

    <input type="text" name="docid" placeholder="4" value="<%=getVal("docid",request,"4")%>"> - simulovat docid pre Ninju<br>
    <input type="text" name="forward" size="100" placeholder="/templates/aceintegration/jet/sub-page.jsp" value="<%=getVal("forward",request,"/templates/aceintegration/jet/sub-page.jsp")%>"> - JSP sablona<br>

    <input type="submit" name="submit" value="Generovat"><br>
    <input type="hidden" name="send" value="true"><br>
</form>

<script type="text/javascript">
    hideInputs();
</script>
