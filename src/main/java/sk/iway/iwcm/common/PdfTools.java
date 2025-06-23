package sk.iway.iwcm.common;

import static sk.iway.iwcm.Tools.getIntValue;
import static sk.iway.iwcm.Tools.isInteger;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import jakarta.servlet.http.HttpServletRequest;

import org.zefer.pd4ml.PD4Constants;
import org.zefer.pd4ml.PD4ML;
import org.zefer.pd4ml.PD4PageMark;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.Pd4mlOptions;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.SpamProtection;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.context.ContextFilter;

public class PdfTools {

    private static String ORIGINAL_JAVA_VERSION = null;

    private PdfTools() {

    }

    /**
     * Do zadaneho output streamu vygeneruje PDF verziu zadaneho docId
     * @param docId
     * @param request
     * @param output
     * @return
     */
    public static boolean getPdfVersion(int docId, HttpServletRequest request, OutputStream output)
    {
        try
         {
             DocDetails doc = DocDB.getInstance().getDoc(docId);
             if (doc == null) return false;
             GroupDetails group = GroupsDB.getInstance().getGroup(doc.getGroupId());
             if (group != null)
             {
                if (Tools.isNotEmpty(group.getDomainName())) request.getSession().setAttribute("preview.editorDomainName", group.getDomainName());
             }

             String qs = request.getQueryString();
             if (Tools.isEmpty(qs)) qs = "a=1";

             StringBuilder url = new StringBuilder(Tools.getBaseHrefLoopback(request)).append("/showdoc.do?docid=").append(docId).append("&NO_WJTOOLBAR=true&isPdfVersion=true&").append(qs);

             if (request.getParameter("forward")==null) url.append("&forceBrowserDetector=pdfprint");

             //http://testvubcms:8080/showdoc.do?isPdfVersion=true&docid=2401&forward=pdf_cennik.jsp&date=04.01.2010&forIntranet=true

             String data = Tools.downloadUrl(url.toString(), SetCharacterEncodingFilter.getEncoding());

             if (Tools.isEmpty(data)) return false;

             //asi nastalo presmerovanie na https verziu, loopback nefunguje
             if (data.startsWith("<html><body>\n<a href='")) data = "";

             request.setAttribute("docId", Integer.toString(docId));
             renderHtmlCode(data, output, request);

             return true;
         }
         catch (Exception e)
         {
             Logger.error(PdfTools.class, e);
         }

         return false;
    }

    public static void renderHtmlCode(String data, OutputStream output, HttpServletRequest request) throws IOException
    {
        renderHtmlCode(data, output, request, null);
    }

    public static void renderHtmlCode(String data, OutputStream output, HttpServletRequest request, Pd4mlOptions options) throws IOException
    {
        renderHtmlCode(data, output, request, options, true);
    }

    //ing formulare sa generuju 2x zasebou v case mensom ako 1s
    public static void renderHtmlCode(String data, OutputStream output, HttpServletRequest request, Pd4mlOptions options, boolean useSpamProtection) throws IOException
    {
        Prop prop = Prop.getInstance(PageLng.getUserLng(request));

        String editorDomainName = null;
        if (request != null) editorDomainName = (String)request.getSession().getAttribute("preview.editorDomainName");

        if (data == null || data.length()==0 || output == null)
        {
            throw new IllegalArgumentException(prop.getText("html2pdf.error.nodata"));
        }
        if (useSpamProtection && request != null && !SpamProtection.canPost("HtmlToPdfAction", data, request))
        {
            throw new IllegalArgumentException(prop.getText("html2pdf.error.spam"));
        }


        //prehodime <style> @import "nieco" </style> na <link> tag
        //a to tak, ze ich najprv najdeme...
        Pattern styleImportPattern = Pattern.compile("<style(\\s*[a-z]+=['\"].*?['\"]\\s*)>\\s*@import\\s*['\"](.*?)['\"];\\s*</style>");
        Matcher styleImportMatcher = styleImportPattern.matcher(data);
        //pre kazdy takyto pattern
        while(styleImportMatcher.find())
        {
            //skopiruj atributy do noveho <link> tagu
            StringBuilder attributes = new StringBuilder();
            for (int attributeIndex =1;attributeIndex<styleImportMatcher.groupCount() ; attributeIndex++)
                attributes.append(styleImportMatcher.group(attributeIndex));
            //a @import prehod na href="nieco" tag
            String href = styleImportMatcher.group( styleImportMatcher.groupCount() );
            data = styleImportMatcher.replaceAll("<link href=\""+href+"\" "+attributes.toString()+" rel=\"stylesheet\" />");
            styleImportMatcher.reset(data);
        }

        //fixni ?v=XXXX" parameter v obrazkoch
        data = data.replaceAll("\\?v=\\d+\"", "\"");
        data = data.replaceAll("\\?v=\\d+'", "'");

        //fixni cestu k obrazku pri pouziti externeho adresara
        if (FilePathTools.isExternalDir("/images/")) {
            String[] baseDirs = {"/images/", "/files/", "/shared/"};
            for (String baseDir : baseDirs) {
                String realPath = Tools.getRealPath(baseDir);
                data = Tools.replace(data, "src=\""+baseDir, "src=\""+realPath);
                data = Tools.replace(data, "src='"+baseDir, "src='"+realPath);
                data = Tools.replace(data, "url("+baseDir, "url("+realPath);
            }
        }

        if (request!=null && "true".equals(request.getParameter("screen"))==false)
        {
            //najdeme vsetky style, ktore nie su pre media="print" a zakomentujeme ich
             Pattern styleSheetPattern = Pattern.compile("<link.*?/?>", Pattern.CASE_INSENSITIVE);
             Matcher styleSheetMatcher = styleSheetPattern.matcher(data);
             while (styleSheetMatcher.find())
             {
                 if (!styleSheetMatcher.group().contains("media=\"print\"") && !styleSheetMatcher.group().contains("media=\"all\""))
                 {
                    Logger.debug(PdfTools.class, "HtmlToPdfConverter => replacing " + styleSheetMatcher.group());
                     data = data.replace(styleSheetMatcher.group(), "<!--" + styleSheetMatcher.group() + "-->");
                 }
             }
             styleSheetMatcher = Pattern.compile("<style.*?/?>", Pattern.CASE_INSENSITIVE).matcher(data);
             while (styleSheetMatcher.find())
             {
                 if (!styleSheetMatcher.group().contains("media=\"print\"") && !styleSheetMatcher.group().contains("media=\"all\""))
                 {
                    Logger.debug(PdfTools.class, "HtmlToPdfConverter => replacing " + styleSheetMatcher.group());
                     data = data.replace(styleSheetMatcher.group(), "<!--" + styleSheetMatcher.group() + "-->");
                 }
             }
             data = data.replaceAll("media=\"print\"", "media=\"screen\"");
        }
         data = data.replaceAll("media=\"all\"", "media=\"screen\"");
         data = data.replaceAll("class=\"printButton\"", "class=\"printButton\" style=\"display:none;\"");


         fixJavaVersion();
         PD4ML pd4ml = new PD4ML();

         //doplnena moznost pre zabezpecenie PDF dokumentu
         String password = (request != null && request.getAttribute("pdfPassword") != null) ? (String)request.getAttribute("pdfPassword") : null;
         Integer permissions = (request != null && request.getAttribute("pdfPermissions") != null && request.getAttribute("pdfPermissions") instanceof Integer) ? (Integer)request.getAttribute("pdfPermissions") : null;
         if(Tools.isNotEmpty(password) || permissions != null)
         {
             //ak nezadavam svoje heslo, bude pristup bez hesla
             if(Tools.isEmpty(password)) password = "empty";
             //ak nie je definovane povolim vsetko
             if(permissions == null) permissions = 0xffffffff;
             pd4ml.setPermissions(password, permissions, true);
         }

         if (request != null && request.getAttribute("htmlWidth") != null)	pd4ml.setHtmlWidth(Tools.getIntValue((String)request.getAttribute("htmlWidth"), 1024));
         else if (request != null)	pd4ml.setHtmlWidth(getIntValue(request.getParameter("width"), 1024));
         else 	pd4ml.setHtmlWidth(1024);

         pd4ml.enableImgSplit(false);
         pd4ml.generateOutlines(true);
         pd4ml.enableDebugInfo();
         pd4ml.setAuthorName(Constants.getString("pdfAuthorName"));
         if (request != null && isInteger(request.getParameter("insets")))
         {
             int insets = getIntValue(request.getParameter("insets"), 10);
             pd4ml.setPageInsets(new Insets(insets, insets, insets, insets));
         }
         else if(request != null)
         {
             if(request.getAttribute("insets") != null)
             {
                 int insets = getIntValue((String)request.getAttribute("insets"), 0);
                 pd4ml.setPageInsets(new Insets(insets, insets, insets, insets));
             }
             else if(request.getAttribute("inset-top") != null || request.getAttribute("inset-bottom") != null ||
                       request.getAttribute("inset-left") != null || request.getAttribute("inset-right") != null)
             {
                 int insertTop = request.getAttribute("inset-top") != null ? Tools.getIntValue((String)request.getAttribute("inset-top"), 0) : 0;
                 int insertLeft = request.getAttribute("inset-left") != null ? Tools.getIntValue((String)request.getAttribute("inset-left"), 0) : 0;
                 int insertBottom = request.getAttribute("inset-bottom") != null ? Tools.getIntValue((String)request.getAttribute("inset-bottom"), 0) : 0;
                 int insertRight = request.getAttribute("inset-right") != null ? Tools.getIntValue((String)request.getAttribute("inset-right"), 0) : 0;
                 pd4ml.setPageInsets(new Insets(insertLeft, insertTop, insertBottom, insertRight));
             }
         }
         int x = PD4ML.A4.width;
         int y = PD4ML.A4.height;
         if (request != null)
         {
             x = getIntValue(request.getParameter("width"), PD4ML.A4.width);
             y = getIntValue(request.getParameter("height"), PD4ML.A4.height);
             if(request.getAttribute("pageWidth") != null)
                x = Tools.getIntValue((Integer)request.getAttribute("pageWidth"), PD4ML.A4.width);
             if(request.getAttribute("pageHeight") != null)
                y = Tools.getIntValue((Integer)request.getAttribute("pageHeight"), PD4ML.A4.height);
         }
         pd4ml.setPageSize(new Dimension(x, y));

         String conReadonlyDocIds = Constants.getString("htmlToPdfReadonlyDocIds");
         String docId = null;
         //BUGFIX  9091
         if(request != null) docId =(String)request.getAttribute("docId");

         //nastavujem readonly pre zadane docIds
         if(Tools.isNotEmpty(conReadonlyDocIds) && Tools.isNotEmpty(docId))
         {
             Logger.debug(PdfTools.class, "readonly docIds: "+conReadonlyDocIds);
             conReadonlyDocIds = conReadonlyDocIds.replaceAll(";", "+").replaceAll(" ", "+");
             String[] readonlyDocIds = Tools.getTokens(conReadonlyDocIds, "+", true);
             for(String readOnlyDocId : readonlyDocIds)
             {
                 if(readOnlyDocId.equals(docId))
                 {
                     Logger.debug(PdfTools.class, "nastavujem readonly pre docId: "+docId);
                     pd4ml.setPermissions("empty", PD4Constants.AllowContentExtraction+PD4Constants.AllowModify, true);
                     break;
                 }
             }
         }

         if (options != null )
         {
             if (options.isFitPageVertically())
             {
                 pd4ml.fitPageVertically();
             }
             if (options.getHtmlWidth()>0)
             {
                 pd4ml.setHtmlWidth(options.getHtmlWidth());
             }
         }

         if (request != null)
         {
             String headerHtml = (String)request.getAttribute("pdfHeaderHtml");
             if (Tools.isNotEmpty(headerHtml))
             {
                 PD4PageMark header = new PD4PageMark();
                 header.setAreaHeight( Tools.getIntValue((String)request.getAttribute("pdfHeaderHeight"), 30 ));
                 header.setHtmlTemplate( headerHtml );
                 pd4ml.setPageHeader( header );
             }

             String footerHtml = (String)request.getAttribute("pdfFooterHtml");
             if (Tools.isNotEmpty(footerHtml))
             {
                 PD4PageMark footer = new PD4PageMark();
                 footer.setAreaHeight( Tools.getIntValue((String)request.getAttribute("pdfFooterHeight"), -1 ) );
                 footer.setHtmlTemplate( footerHtml );
                 pd4ml.setPageFooter( footer );
             }
         }

         if (request != null) pd4ml.setSessionID(request.getSession().getId());

         URL base;
         String pdfBaseUrl = Constants.getString("pdfBaseUrl");
         if ("NULL".equalsIgnoreCase(pdfBaseUrl))
         {
             //v tomto pripade si PD4ML sam nacita obrazky z file systemu, potrebuje astaveny Context a Request
             pd4ml.useServletContext(Constants.getServletContext());
             if (request != null) pd4ml.useHttpRequest(request, null);

             base = null;

             String contextPath = "";
             if (request != null && ContextFilter.isRunning(request))
             {
                 contextPath = request.getContextPath();
             }

             //pre dynamicke obrazky musime pridat HTTP loopback
             data = Tools.replace(data, "'"+contextPath+"/admin/statchart", "'"+Tools.getBaseHrefLoopback(request)+"/admin/statchart");
             data = Tools.replace(data, "\""+contextPath+"/admin/statchart", "\""+Tools.getBaseHrefLoopback(request)+"/admin/statchart");

             data = Tools.replace(data, "'"+contextPath+"/graph.do", "'"+Tools.getBaseHrefLoopback(request)+"/graph.do");
             data = Tools.replace(data, "\""+contextPath+"/graph.do", "\""+Tools.getBaseHrefLoopback(request)+"/graph.do");

             data = Tools.replace(data, "'"+contextPath+"/thumb/", "'"+Tools.getBaseHrefLoopback(request)+"/thumb/");
             data = Tools.replace(data, "\""+contextPath+"/thumb/", "\""+Tools.getBaseHrefLoopback(request)+"/thumb/");
             if (request != null && ContextFilter.isRunning(request))
             {
                 //toto je tu kvoli _printAsPdf=true kedy je HTML kod bez prefixov
                 data = Tools.replace(data, "'/thumb/", "'"+Tools.getBaseHrefLoopback(request)+"/thumb/");
                 data = Tools.replace(data, "\"/thumb/", "\""+Tools.getBaseHrefLoopback(request)+"/thumb/");
             }

             if (request != null && ContextFilter.isRunning(request))
             {
                 data = Tools.replace(data, "'"+request.getContextPath()+"/", "'/");
                 data = Tools.replace(data, "\""+request.getContextPath()+"/", "\"/");
             }
         }
         else if ("LOOPBACK".equalsIgnoreCase(pdfBaseUrl)) base = new URL(Tools.getBaseHrefLoopback(request));
         else base = new URL(pdfBaseUrl);

         if (base != null && request!=null && ContextFilter.isRunning(request))
         {
             //pridaj ContextPath k HTML kodu
             data = ContextFilter.addContextPath(request.getContextPath(), data);
         }
         try
         {
             String pdfFontDirectory = Constants.getStringExecuteMacro("pdfFontDirectory");
             if (base != null) pdfFontDirectory = Tools.getRealPath(Tools.replace(pdfFontDirectory, "file://", "/"));
             IwcmFile pdfFontDirIwcmFile = new IwcmFile(base != null ? pdfFontDirectory : Tools.replace(pdfFontDirectory, "file://", "/"));
             Logger.debug(PdfTools.class, "FONT PATH: "+pdfFontDirectory);
             if(pdfFontDirIwcmFile.exists() == false)
                 Logger.error(PdfTools.class, "FONT PATH "+pdfFontDirIwcmFile.getAbsolutePath()+" neexistuje!!");

             //pd4ml.useAdobeFontMetrics( true );
             //pd4ml.useTTF( Tools.getStringValue(Constants.getString("pdfFontDirectory"), "C:\\WINDOWS\\Fonts"), true);

             pd4ml.useTTF(pdfFontDirectory, true);
         }
         catch (FileNotFoundException exc)
         {
            Logger.error(PdfTools.class, exc);
             //nic, iba nebude pouzivat default fonty
         }

         //generovanie vo formate rtf
         if(request != null && request.getParameter("renderAsRtf") != null && request.getParameter("renderAsRtf").toLowerCase().equals("true") )
         {
             if(request.getParameter("imgQuality") != null && request.getParameter("imgQuality").toLowerCase().equals("wmf"))
                 pd4ml.outputFormat(PD4Constants.RTF_WMF);	//RTF_WMF - compatibility with WordPad WMF
             else
                 pd4ml.outputFormat(PD4Constants.RTF);	//RTF -  original format (compatible with MS Word and few other editors)
         }


         //nasledujuca funkcia je deprecated, a este k tomu zbytocna, pretoze je defaultne true
         //pd4ml.useAdobeFontMetrics(true);
         if (request!=null)
         {
             //zisti, ci nemame renderovat na obrazky
             boolean renderAsImages = false;
             if(request.getAttribute("renderAsImages") != null && request.getAttribute("renderAsImages") instanceof Boolean)
                 renderAsImages = (Boolean)request.getAttribute("renderAsImages");
             String imagesUrlwithPrefix = "/files/pdf_as_image";
             if(request.getAttribute("imagesUrlwithPrefix") != null)
                 imagesUrlwithPrefix = (String)request.getAttribute("imagesUrlwithPrefix");

             if(!renderAsImages)
             {
                 if (base == null)
                 {
                    pd4ml.render(new StringReader(data),  output);
                 }
                 else if ("true".equals(request.getParameter(SetCharacterEncodingFilter.PDF_PRINT_PARAM)) && "true".equals(request.getParameter(SetCharacterEncodingFilter.PDF_PRINT_PARAM+"No"))==false)
                 {
                     pd4ml.render(new StringReader(data),  output, base, SetCharacterEncodingFilter.getEncoding());
                 }
                 else
                 {
                     //pd4ml.render(new StringReader(data),  output, new URL(Tools.getBaseHrefLoopback(request)), SetCharacterEncodingFilter.encoding);
                     pd4ml.render(new StringReader(data),  output, base, SetCharacterEncodingFilter.getEncoding());
                 }
             }
             else
             {
                 if(request.getAttribute("SetCharacterEncodingFilter") != null && (Boolean)request.getAttribute("SetCharacterEncodingFilter"))
                     pd4ml.overrideDocumentEncoding(SetCharacterEncodingFilter.getEncoding());

                 BufferedImage[] biArray = pd4ml.renderAsImages(new StringReader(data), base, Tools.getIntValue((Integer)request.getAttribute("imageWidth"), PD4ML.A4.width),  Tools.getIntValue((Integer)request.getAttribute("imageHeight"), PD4ML.A4.height));

                 String imgSuff = "png";
                 if(Tools.getParamAttribute("ImageSuffix", request)  != null)
                 {
                     String suffix = Tools.getParamAttribute("ImageSuffix", request).toLowerCase();
                     if(suffix.equals("jpg"))
                         imgSuff = "jpg";

                     if(suffix.equals("tiff"))
                         imgSuff = "tiff";
                 }

                 for(int i = 0; i < biArray.length; i++)
                 {
                     IwcmFile dest = new IwcmFile(Tools.getRealPath(imagesUrlwithPrefix+(biArray.length > 1 ? "_"+i : "")+"."+imgSuff));
                     ImageIO.write(biArray[i], imgSuff, new File(dest.getPath()));
                 }
             }
         }
         else
         {
             pd4ml.render(new StringReader(data), output);
         }

        revertOriginalJavaVersion();

         //toto sa nam proste nejako straca...
         if (request != null && Tools.isNotEmpty(editorDomainName)) request.getSession().setAttribute("preview.editorDomainName", editorDomainName);
    }

    private static void fixJavaVersion() {
        if (ORIGINAL_JAVA_VERSION == null) {
            ORIGINAL_JAVA_VERSION = System.getProperty("java.version");
        }
        //FIX pd4ml wrong Java version detection based on 2nd digit
        String[] versions = Tools.getTokens(ORIGINAL_JAVA_VERSION, ".");
        //replace second digit with number 8
        if (versions.length > 1) {
            versions[1] = "8";
            System.setProperty("java.version", Tools.join(versions, "."));
        }
    }

    private static void revertOriginalJavaVersion() {
        if (ORIGINAL_JAVA_VERSION != null) System.setProperty("java.version", ORIGINAL_JAVA_VERSION);
    }
}
