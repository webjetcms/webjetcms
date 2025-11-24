<%@page import="java.util.Map"%><%@page import="java.util.List"%><% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %><%@
        page pageEncoding="utf-8" import="sk.iway.iwcm.*,java.util.*" %><%@
        page import="sk.iway.iwcm.grideditor.generator.ScreenshotGenerator" %><%@
        page import="com.google.gson.stream.JsonReader" %><%@
        page import="com.google.gson.stream.JsonToken" %><%@
        page import="sk.iway.iwcm.doc.*" %><%@
        page import="java.net.HttpURLConnection" %><%@
        page import="java.net.URL" %><%@
        page import="java.io.*" %><%@
        page import="java.nio.charset.StandardCharsets" %><%@
        page import="sk.iway.iwcm.users.UsersDB" %><%@
        page import="java.util.ArrayList" %><%@
        page import="sk.iway.iwcm.io.IwcmFile" %>
<%@ page import="sk.iway.iwcm.grideditor.generator.ScreenShotPropBean" %>
<%@ page import="sk.iway.iwcm.i18n.IwayProperties" %>
<%@ page import="sk.iway.iwcm.i18n.PropDB" %>
<%@ page import="java.net.URLEncoder" %>
<%@
        taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
        taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
        taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
        taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
        taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms="menuWebpages"/><%!

    private static String getResponse(InputStream in)
    {
        if(in == null)
        {
            Logger.debug(ScreenshotGenerator.class, "call: getResponse(...) in == null, return null");
            return null;
        }

        String response = null;
        try
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line = reader.readLine();
            response = line;
            while((line=reader.readLine())!=null)
            {
                response += line;
            }
        }
        catch(IOException ioex)
        {
            sk.iway.iwcm.Logger.error(ioex);
        }

        return response;
    }

    private static Object createConnection(Boolean isPost, String url, String output, javax.servlet.http.Cookie cookies[])
    {
        try
        {
            Logger.debug(ScreenshotGenerator.class, "Requested url: "+url);
            Logger.debug(ScreenshotGenerator.class, "output: "+output);
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();///self/service
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json");
            con.setReadTimeout(60*1000);

//            if(addLogin)
//            {
//                con.setRequestProperty("Accept", "application/json");
//                String userCredentials = getUserName()+":"+getPassword();//"username:password";
//                String basicAuth = "Basic " + new String(new Base64().encode(userCredentials.getBytes()));
//                con.setRequestProperty("Authorization", basicAuth);
//            }
            String cookieString="";
            int i;
            int len = cookies.length;
            for (i=0; i<len; i++)
            {
                cookieString += cookies[i].getName()+"="+cookies[i].getValue()+";";
            }

            if(Tools.isNotEmpty(cookieString))
            {
                cookieString.substring(0,cookieString.length()-1);
            }

            con.setRequestProperty("Cookie", cookieString);
            if(isPost == null)
                con.setRequestMethod("DELETE");
            else if(isPost)
                con.setRequestMethod("POST");
            else
                con.setRequestMethod("GET");

            if(Tools.isNotEmpty(output))
            {
//                OutputStreamWriter osw = new OutputStreamWriter(con.getOutputStream());
//                osw.write(new String(output));
//                osw.flush();
//                osw.close();

                DataOutputStream  osw = new DataOutputStream (con.getOutputStream());
                osw.write(output.getBytes(StandardCharsets.UTF_8));
                osw.flush();
                osw.close();
            }

            con.connect();

            Logger.debug(null, "Connected method : "+con.getRequestMethod());
            Logger.debug(null, "Response Code : "+con.getResponseCode());
            Logger.debug(null, "Message : "+con.getResponseMessage());

            if(con.getErrorStream()  != null)//pocas celeho testovania bol null, ale pre istotu to sem dam.
            {
                Logger.debug(null, "getErrorStream() != null Treba precitat! ");
                Logger.debug(null, getResponse(con.getErrorStream()));
            }

            //ak nieco nezbehlo v poriadku
            if(con.getResponseCode() >= 400)
            {
                return null;
            }

            return con.getContent();

        }
        catch(Exception exc)
        {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            exc.printStackTrace(pw);
            //SendMail.send(cloudEmailFromName, Constants.getString("cloudDefaultEmail"), sk.iway.iwcm.common.CloudToolsForCore.getSupportEmail(), "Chyba pri vytvoreni spojenia WS-REST-API", exc.getMessage()+"<br>"+ Tools.replace(sw.toString(), "\n", "\n<br/>") + "<br/><br/>Url: " + url + "<br/>Output: "+output);
            sk.iway.iwcm.Logger.error(exc);
        }
        return null;
    }

    public static List<LabelValueDetails> parseJsonCategoryDir(String json,HttpServletRequest request, JspWriter out, ScreenShotPropBean properties)
    {
        List<LabelValueDetails> resultsString = new ArrayList<LabelValueDetails>();
        JsonReader reader = null;
        String name = "";
        String pomString = "";
        String errors = "";
        String html_data = "";
        String url_data = "";
        String file_path = "";
        int cisloNahladu = 0;
        try
        {
            reader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(json.getBytes())));//"UTF-8"

//            reader.beginArray();
//            while (reader.hasNext())
//            {
            System.out.println("metoda: "+json);
                reader.beginObject();
                while (reader.hasNext())
                {
                    name = reader.nextName();
                    if("groups".equalsIgnoreCase(name))
                    {
                        reader.beginArray();
                        while (reader.hasNext())
                        {// ****
                            reader.beginObject();
                            while (reader.hasNext())
                            {
                                name = reader.nextName();
                                if ("blocks".equalsIgnoreCase(name))
                                {
                                    reader.beginArray();
                                    while (reader.hasNext())
                                    {
                                        html_data = url_data = file_path =  "";
                                        reader.beginObject();
                                        while (reader.hasNext())
                                        {

                                            name = reader.nextName();
                                            if (reader.peek() != JsonToken.NULL)
                                            {
                                                LabelValueDetails lvd = new LabelValueDetails();


                                                if (!"premium".equals(name))
                                                {
                                                    if("content".equals(name))
                                                    {
                                                        pomString = reader.nextString();
                                                        //System.out.println(pomString);
                                                       // FileTools.saveFileContent("/files/pra/test/"+Tools.getNow()+".jsp",pomString, "UTF-8");
                                                        html_data = pomString;
                                                        //lvd = new LabelValueDetails("url",pomString) ;
                                                    }
                                                    else if("filePath".equals(name))
                                                    {
                                                        pomString = reader.nextString();
                                                        file_path = pomString;
                                                        System.out.println("file_path: "+file_path);
                                                        if (file_path.startsWith("/components")) System.out.println("--------- POZOR");
                                                    }
                                                    else if("action".equals(name))
                                                    {

                                                        pomString = reader.nextString();
                                                        //System.out.println(pomString);
                                                        // FileTools.saveFileContent("/files/pra/test/"+Tools.getNow()+".jsp",pomString, "UTF-8");
                                                        if("block".equals(pomString))
                                                        {
                                                            url_data = pomString;
                                                            if(Tools.isNotEmpty(html_data))
                                                            {
                                                                //System.out.println("OK");
                                                                System.out.print("Generujem cislo nahladu: "+cisloNahladu+"<br>");
                                                                out.print("Generujem cislo nahladu: "+cisloNahladu++ +"<br>");
                                                                out.flush();

                                                                //ScreenShotPropBean sb = new ScreenShotPropBean(Tools.getBaseHref(request)+ "/components/grideditor/phantom/phantom_sablona_ajax.jsp");

                                                                String ajaxUrl = "/components/grideditor/phantom/phantom_sablona_ajax.jsp";
                                                                String forward = request.getParameter("forward");
                                                                if (Tools.isNotEmpty(forward)) ajaxUrl = Tools.addParameterToUrlNoAmp(ajaxUrl, "forward", forward);
                                                                String docid = request.getParameter("docid");
                                                                if (Tools.isNotEmpty(docid)) ajaxUrl = Tools.addParameterToUrlNoAmp(ajaxUrl, "docid", docid);

                                                                ScreenShotPropBean sb  = properties;
                                                                sb.setScreenshotUrl(Tools.getBaseHref(request)+ ajaxUrl);
                                                                sb.setCookieHtmlData(URLEncoder.encode("test_cookie","UTF-8"));
                                                                sb.setSaveImageToPath(file_path);
                                                                sb.setDebug(true);
                                                                if(Tools.getRequestParameter(request, "height_auto") != null)
                                                                {
                                                                    sb.setAutoHeigth(true);
                                                                }
                                                                //nastavenie textu
                                                                IwayProperties ip = new IwayProperties();
                                                                ip.setProperty("phantomjs.html_data.template",html_data);
                                                                PropDB.save(UsersDB.getCurrentUser(request), ip,"sk",null,null,false);
                                                                //sb.setTimeDelayMilisecond(1300);
                                                                ScreenshotGenerator.generatePreview(sb);

                                                                List<String> errorsList = sb.getPhantomErrors();
                                                                if (errorsList.isEmpty()==false) {
                                                                    for (String error : errorsList) {
                                                                        out.println("ERROR: "+error+"<br/>");
                                                                    }
                                                                }

                                                                if (file_path.contains("/dist/")) {
                                                                    //skopiruj ho aj do src
                                                                    String srcPath = Tools.replace(file_path, "/dist/", "/src/pug/")+".jpg";
                                                                    FileTools.copyFile(new IwcmFile(Tools.getRealPath(file_path+".jpg")), new IwcmFile(Tools.getRealPath(srcPath)));
                                                                    out.println(srcPath+"<br/>");
                                                                }

                                                                out.println(file_path+".jpg<br/><img src='"+file_path+".jpg' style='max-width: 250px;'/><br/>");
                                                                //if ("/components/grideditor/data/section/S_fotkou/testimonials_02".equals(file_path)) return resultsString;
                                                                //if ("/components/grideditor/data/container/Textove_bloky/content-01_c".equals(file_path)) return resultsString;

                                                                //  Tu ideme generovat nahlad
                                                            }
                                                            else
                                                            {
                                                               // System.out.println("BAD");
                                                            }
                                                        }
                                                        //lvd = new LabelValueDetails("url",pomString) ;
                                                    }
                                                    else
                                                    {
                                                        //System.out.println(name + ": " + reader.nextString());
                                                        reader.nextString();
                                                    }
                                                }
                                                else
                                                {
                                                    //System.out.println(name + ": " + reader.nextBoolean());
                                                    reader.nextBoolean();
                                                }

                                                if(Tools.isNotEmpty(html_data) && Tools.isNotEmpty(url_data))
                                                {
                                                    resultsString.add(new LabelValueDetails(url_data,html_data));
                                                    html_data = url_data = file_path = "";
                                                }
                                                //return "name: "+name;
                                            }
                                            else
                                            {
                                                reader.skipValue();
                                            }
                                        }
                                        reader.endObject();
                                    }
                                    reader.endArray();
                                }
                                else
                                {
                                    reader.skipValue();
                                }
                            }
                            reader.endObject();
                        }
                        reader.endArray();
                    }
                    else
                    {
                        reader.skipValue();
                    }
                }
                reader.endObject();
//            }
//            reader.endArray();
        }catch (Exception e) {
            sk.iway.iwcm.Logger.error(e);
        }

        return resultsString;
    }
%><%//Tools.downloadUrl(Tools.getBaseHref(request)+"/service/grideditor/categories");
    //System.out.println("data:"+ScreenGenerator.getCategoriesServiceData(request));
    //ScreenGenerator.getCategoriesData(request);
    Identity user = UsersDB.getCurrentUser(request);
    if(user == null || !user.isAdmin())
    {
        out.print("Admin neprihlaseny.");
        return;
    }


    ScreenShotPropBean properties = (ScreenShotPropBean)request.getAttribute("screenshotProperties");
    if(true)
    {
        List<LabelValueDetails> listLvd = new ArrayList<LabelValueDetails>();
        JsonReader reader = null;
        String name = "";
        String pomString = "";
        String errors = "";
        try
        {
            //System.out.println("data: "+Tools.downloadUrl(Tools.getBaseHref(request)+"/admin/grideditor/categories",request.getCookies()));//admin
            int docId = Tools.getDocId(request);
            long templateGroupId = -1;
            if (docId > 0) {
                templateGroupId = TemplatesGroupDB.getFromDocId(docId);
            }

            Map<String, String> headers = new Hashtable<String, String>();
            headers.put("Referer", Tools.getBaseHref(request));

            reader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(Tools.downloadUrl(Tools.getBaseHref(request) + "/admin/grideditor/categories?templateGroupId="+templateGroupId, SetCharacterEncodingFilter.getEncoding(), request.getCookies(), 30, headers).getBytes())));//"UTF-8"

            reader.beginArray();
            while (reader.hasNext())
            {
                reader.beginObject();
                while (reader.hasNext())
                {
                    name = reader.nextName();
                    if ("id".equalsIgnoreCase(name))
                    {
                        if (reader.peek() != JsonToken.NULL)
                        {
                            pomString = reader.nextString();
                            // out.print("id: "+pomString);
                            InputStream in = (InputStream) createConnection(true, Tools.getBaseHref(request) + "/admin/grideditor/category?category&categoryDirId=" + pomString+"&templateGroupId="+templateGroupId, "", request.getCookies());

                            String result = getResponse(in);
                            //System.out.println("START xxxxx");
                            //out.print();
                            listLvd = parseJsonCategoryDir(result, request, out, properties);
                            //   out.print("size: "+listLvd.size() );
                            for (LabelValueDetails ld : listLvd)
                            {
                                //   out.print(ld.getLabel()+" -> "+"<br>");
                            }
                            // System.out.println("END xxxxx");

                        }
                        else
                        {
                            reader.skipValue();
                        }
                    }
                    else if ("groups".equalsIgnoreCase(name))
                    {
                        if (reader.peek() != JsonToken.NULL)
                        {
                            reader.beginArray();
                            while (reader.hasNext())
                            {
                                reader.beginObject();
                                while (reader.hasNext())
                                {
                                    name = reader.nextName();
                                    if (!"premium".equals(name))
                                    {
                                        if ("filePath".equals(name))
                                        {
                                            if (reader.peek() != JsonToken.NULL)
                                            {
                                                pomString = reader.nextString();
                                            }
                                            else
                                            {
                                                reader.skipValue();
                                            }
                                        }
                                        else
                                        {
                                            if (reader.peek() != JsonToken.NULL)
                                            {
                                                reader.nextString();
                                            }
                                            else
                                            {
                                                reader.skipValue();
                                            }
                                        }
                                    }
                                    else
                                    {
                                        if (reader.peek() != JsonToken.NULL)
                                        {
                                            reader.nextBoolean();
                                        }
                                        else
                                        {
                                            reader.skipValue();
                                        }
                                    }
                                }
                                reader.endObject();
                            }
                            reader.endArray();
                        }
                        else
                        {
                            reader.skipValue();
                        }
                    }
                    else
                    {
                        reader.skipValue();
                    }
                }
                reader.endObject();
            }
            reader.endArray();
        } catch (Exception e)
        {
            sk.iway.iwcm.Logger.error(e);
        }
    }
%>