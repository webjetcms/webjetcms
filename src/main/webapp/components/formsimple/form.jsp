<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="org.apache.commons.beanutils.BeanUtils" %><%@
page import="org.apache.commons.codec.binary.Base64" %><%@
page import="org.apache.commons.codec.binary.StringUtils" %><%@
page import="sk.iway.iwcm.tags.support_logic.ResponseUtils" %><%@
page import="org.json.JSONArray" %><%@
page import="org.json.JSONObject" %><%@
page import="org.jsoup.Jsoup" %><%@
page import="sk.iway.iwcm.PageLng" %><%@
page import="sk.iway.iwcm.PageParams" %><%@
page import="sk.iway.iwcm.Tools" %><%@
page import="sk.iway.iwcm.common.DocTools" %><%@
page import="sk.iway.iwcm.components.enumerations.EnumerationDataDB" %><%@
page import="sk.iway.iwcm.components.enumerations.EnumerationTypeDB" %><%@
page import="sk.iway.iwcm.components.enumerations.model.EnumerationDataBean" %><%@
page import="sk.iway.iwcm.components.enumerations.model.EnumerationTypeBean" %><%@
page import="sk.iway.iwcm.i18n.Prop" %><%@
page import="java.net.URLDecoder" %><%@
page import="java.util.List" %><%@
page import="java.util.Set" %><%@
page import="java.util.HashSet" %><%@
page import="sk.iway.iwcm.form.FormAttributeDB" %><%@
page import="java.util.Map" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%!

    private String replaceFields(String html, String formName, String recipients, JSONObject item, String requiredLabelAdd, boolean isEmailRender, boolean rowView, Set<String> firstTimeHeadingSet, Prop prop)
    {
       html = Tools.replace(html, "${formname}", formName);
       html = Tools.replace(html, "${savedb}", formName);
       html = Tools.replace(html, "${recipients}", recipients);

       if (item != null)
       {
          try
          {
              String fieldType = "unknown";
              //System.out.println("---------------------------- item="+item);
              if (item != null) fieldType = item.getString("fieldType");

              String label = Tools.getStringValue(item.getString("label"), "");

              boolean required = false;
              try {
                 required = "true".equals(item.getString("required"));
              } catch (Exception ex) {
                 try {
                  required = item.getBoolean("required");
                 }
                 catch (Exception ex2) {

                 }
              }

              String value = "";
              if (item.has("value"))
              {
                  value = Tools.getStringValue(item.getString("value"), "");
              }

              String placeholder = "";
              if (item.has("placeholder"))
              {
                  placeholder = Tools.getStringValue(item.getString("placeholder"), "");
                  if (Tools.isNotEmpty(placeholder))
                  {
                     placeholder = ResponseUtils.filter(placeholder);

                     //ak je zadany placeholder a nebol zadany label, tak label schovat
                     if (Tools.isEmpty(Tools.getStringValue(item.getString("labelOriginal"), "")))
                     {
                        if (isEmailRender==false) html = Tools.replace(html, "<label ", "<label class=\"d-none\" ");

                        //pretoze z label sa generuje potom ID/name elementu a potrebujeme polia rozlisovat
                        label = placeholder;
                        if (required)
                        {
                           if (Tools.isNotEmpty(requiredLabelAdd))
                           {
                              placeholder += requiredLabelAdd;
                           }
                        }
                     }
                  }
              }

              String tooltip = "";
              if (item.has("tooltip"))
              {
                  tooltip = Tools.getStringValue(item.getString("tooltip"), "");
                  if (Tools.isNotEmpty(tooltip))
                  {
                     tooltip = ResponseUtils.filter(tooltip);
                     tooltip = " " + Tools.replace(prop.getText("components.formsimple.tooltipCode"), "${label}", tooltip);
                  }
              }

              String labelSanitized = Jsoup.parse(label).text();
              String id = DocTools.removeChars(label, true);
              String classes = "";
              if (required)
              {
                 classes="required ";
                 if (Tools.isNotEmpty(requiredLabelAdd))
                 {
                    //ak label konci na : pridaj required text pred dvojbodku
                    if (label.endsWith(":")) label = label.substring(0, label.lastIndexOf(":")) + requiredLabelAdd + ":";
                    else label += requiredLabelAdd;
                 }
              }

              if (isEmailRender) tooltip = "";

              //System.out.println("html="+html+" label="+label+" placeholder="+placeholder+" id="+id);

              //skus zobrazit nadpis nad pole ak je definovany cez components.formsimple.firstTime.xxx
              String firstTimeHeadingKey = "components.formsimple.firstTimeHeading."+fieldType;
              String firstTimeHeading = prop.getText(firstTimeHeadingKey);
              //System.out.println("firstTimeHeadingKey="+firstTimeHeadingKey+" firstTimeHeading="+firstTimeHeading);
              if (Tools.isNotEmpty(firstTimeHeading) && firstTimeHeading.equals(firstTimeHeadingKey)==false && firstTimeHeadingSet.contains(label)==false)
              {
                  firstTimeHeadingSet.add(label);
                  html = firstTimeHeading+html;
              }

              //iterable - pre skupinu poli
              int iterableSize = 0;
              if (html.contains("${iterable}") && Tools.isNotEmpty(value))
              {
                 StringBuilder iterable = new StringBuilder();
                 String iterableKey = "components.formsimple.iterable."+fieldType;
                 String iterableCode = prop.getText(iterableKey);
                 if (Tools.isNotEmpty(iterableCode) && iterableCode.equals(iterableKey)==false)
                 {
                    String delimiter = " ";
                    if (value.contains("|")) delimiter = "|";
                    else if (value.contains(",")) delimiter = ",";

                    String[] values = Tools.getTokens(value, delimiter, true);
                    int counter = 0;
                    iterableSize = values.length;
                    for (String token : values)
                    {
                       String valueLabel = token;
                       String code = iterableCode;

                       int separator = token.indexOf(":");
                       if (code.contains("${value-label}") && separator>0) {
                           valueLabel = token.substring(0, separator);
                           token = token.substring(separator+1);
                       }

                       code = Tools.replace(code, "${value}", token);
                       code = Tools.replace(code, "${value-label}", valueLabel);
                       code = Tools.replace(code, "${counter}", String.valueOf(counter));

                       iterable.append(code).append("\n");
                       counter++;
                    }
                 }
                 html = Tools.replace(html, "${iterable}", iterable.toString());
              }

              html = Tools.replace(html, "${id}", id);
              html = Tools.replace(html, "${label}", isEmailRender && label.trim().endsWith(":") == false ? label+":" : label);
              html = Tools.replace(html, "${labelSanitized}", labelSanitized);
              html = Tools.replace(html, "${value}", value);
              html = Tools.replace(html, "${valueSanitized}", DocTools.removeChars(value, true));
              html = Tools.replace(html, "${placeholder}", placeholder);
              html = Tools.replace(html, "${classes}", classes);
              html = Tools.replace(html, "${tooltip}", tooltip);

              StringBuilder csError = new StringBuilder();
              csError.append("<div class=\"help-block cs-error cs-error-").append(id);
              if (iterableSize > 0)
              {
                 for (int counter = 0; counter < iterableSize; counter++)
                 {
                    csError.append(" cs-error-").append(id).append("-").append(counter);
                 }
              }
              csError.append("\"></div>");
              html = Tools.replace(html, "${cs-error}", csError.toString());

              //zamena za hodnoty z ciselnika vo forme {enumeration-options|ID_CISELNIKA|MENO_VALUE|MENO_LABEL}
              StringBuilder sb = null;
              List<EnumerationDataBean> options;
              String[] tokens;
              int typeId;
              int i = 0;
              int startInd = html.indexOf("{enumeration-options");
              int endInd;
              if(html.contains("{enumeration-options"))
              {
                 while(startInd != -1 && i++ < 100)
                 {
                    endInd = html.indexOf("}", startInd);
                    if(endInd != -1)
                    {
                       String enumOptions = html.substring(startInd, endInd+1);
                       tokens = Tools.getTokens(html.substring(startInd+1, endInd), "|");
                       if(tokens != null && tokens.length == 4)
                       {
                          typeId = Tools.getIntValue(tokens[1],0);
                          //ziskam data na zaklade ID_CISELNIKA
                          options = EnumerationDataDB.getEnumerationDataByType(typeId);
                          if(options != null && options.size() > 0)
                          {
                             //ak zadame ze value ma byt enumeration_data_id, staci ak zadame v texte "id"
                             if("id".equalsIgnoreCase(tokens[2])) tokens[2] = "enumerationDataId";
                             if("id".equalsIgnoreCase(tokens[3])) tokens[3] = "enumerationDataId";
                             EnumerationTypeBean currentType = EnumerationTypeDB.getEnumerationById(typeId);
                             if(currentType != null && currentType.getEnumerationTypeId() > 0)
                             {
                                //zamena alternativneho nazvu stlpca hodnoty za DB nazov
                                if (tokens[2].equalsIgnoreCase(currentType.getString1Name()))
                                   tokens[2] = "string1";
                                else if (tokens[2].equalsIgnoreCase(currentType.getString2Name()))
                                   tokens[2] = "string2";
                                else if (tokens[2].equalsIgnoreCase(currentType.getString3Name()))
                                   tokens[2] = "string3";
                                else if (tokens[2].equalsIgnoreCase(currentType.getDecimal1Name()))
                                   tokens[2] = "decimal1";
                                else if (tokens[2].equalsIgnoreCase(currentType.getDecimal2Name()))
                                   tokens[2] = "decimal2";
                                else if (tokens[2].equalsIgnoreCase(currentType.getDecimal3Name()))
                                   tokens[2] = "decimal3";
                                //zamena alternativneho nazvu stlpca label za DB nazov
                                if (tokens[3].equalsIgnoreCase(currentType.getString1Name()))
                                   tokens[3] = "string1";
                                else if (tokens[3].equalsIgnoreCase(currentType.getString2Name()))
                                   tokens[3] = "string2";
                                else if (tokens[3].equalsIgnoreCase(currentType.getString3Name()))
                                   tokens[3] = "string3";
                                else if (tokens[3].equalsIgnoreCase(currentType.getDecimal1Name()))
                                   tokens[3] = "decimal1";
                                else if (tokens[3].equalsIgnoreCase(currentType.getDecimal2Name()))
                                   tokens[3] = "decimal2";
                                else if (tokens[3].equalsIgnoreCase(currentType.getDecimal3Name()))
                                   tokens[3] = "decimal3";
                             }
                             for(EnumerationDataBean option : options)
                             {
                                if(BeanUtils.getProperty(option, tokens[3]) != null) //value moze byt teoreticky prazdne, label nie
                                {
                                   if(sb == null) sb = new StringBuilder();
                                   sb.append("<option").append(" value=\"").append(BeanUtils.getProperty(option, tokens[2])).append("\">").append(BeanUtils.getProperty(option, tokens[3])).append("</option>");
                                }
                             }
                             if(sb != null)
                             {
                                html = html.replace(enumOptions, sb.toString());
                                sb = null;
                             }
                          }
                       }
                       startInd = html.indexOf("{enumeration-options", endInd+1);
                    }
                    else //nenasiel som uz nikde
                    {
                       startInd = -1;
                    }
                 }
              }
          }
          catch (Exception ex)
          {
              sk.iway.iwcm.Logger.error(ex);
          }
       }

       //System.out.println("html="+html);
       if (rowView && html.startsWith("</div")==false) {
          //ak to nie je ukoncovaci tag, obal to do div.col
          html = "<div class=\"col\">"+html+"</div>";
       }

       return html;
    }

    private String sanitizeFieldId(String fieldId)
    {
       fieldId = DocTools.removeChars(fieldId, true);

       return fieldId;
    }

%><%

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);

String json = pageParams.getValue("editorData", "W10=");
//System.out.println("---------- DECODED: "+URLDecoder.decode(StringUtils.newStringUtf8(Base64.decodeBase64(json)), "UTF-8"));
String jsonDecoded = URLDecoder.decode(StringUtils.newStringUtf8(Base64.decodeBase64(json)), "UTF-8");
JSONArray itemsList = new JSONArray(jsonDecoded);

String formName = sanitizeFieldId(pageParams.getValue("formName", ""));

if (Tools.isEmpty(formName)) formName = "form-simple";

boolean rowView = pageParams.getBooleanValue("rowView", false);

//out.println("formName="+formName+" recipients="+recipients+" ppFormName="+pageParams.getValue("formName", "Form Simple")+" includePP="+(String)request.getAttribute("includePageParams"));

Prop prop = Prop.getInstance(lng);
String requiredLabelAdd = prop.getText("components.formsimple.requiredLabelAdd");

boolean isEmailRender = request.getAttribute("renderingIncludes")!=null;
Map<String, String> attributes = new FormAttributeDB().load(DocTools.removeChars(formName, true));

String recipients = "";
if (attributes!=null && Tools.isNotEmpty(attributes.get("recipients"))) recipients = attributes.get("recipients");

if (isEmailRender)
{
   %><style type='text/css'>
        div.onlyemail { display: block !important; }
        div.form-control, span.form-control { height: auto !important; word-break: break-all; }
    </style><%
    out.print("<!-- formmail crop form start -->");
    if(attributes.get("emailTextBefore") != null) out.print(ResponseUtils.filter(attributes.get("emailTextBefore")).replaceAll("\\n", "<br/>")+"<br/>");
} else {
   %><style type='text/css'>
      @media (max-width: 576px) {
         form[name*=formMailForm] .col {
            flex: 0 0 auto;
            width: 100%;
         }
      }
    </style><%
}

//tu evidujeme nadpisy nad pole, ktore sme uz raz zobrazili
Set<String> firstTimeHeadingSet = new HashSet<String>();

out.print(replaceFields(prop.getText("components.formsimple.form.start"), formName, recipients, null, requiredLabelAdd, isEmailRender, false, firstTimeHeadingSet, prop));
if (rowView) out.println("<div class=\"row\">");


boolean containsWysiwyg = false;
for(int i = 0; i < itemsList.length(); i++)
{
    JSONObject item = itemsList.getJSONObject(i);

    String fieldType = item.getString("fieldType");

    String input = prop.getText("components.formsimple.input."+fieldType);
    item.put("labelOriginal", item.getString("label"));
    if (Tools.isEmpty(item.getString("label")))
    {
        String label = prop.getText("components.formsimple.label."+fieldType);
        item.put("label", label);
    }

    String html = replaceFields(input, formName, recipients, item, requiredLabelAdd, isEmailRender, rowView, firstTimeHeadingSet, prop);
    if (html.contains("formsimple-wysiwyg")) containsWysiwyg = true;
    if (html.contains("!INCLUDE"))
    {
        %><iwcm:write><%=html%></iwcm:write><%
    }
    else
    {
        out.print(html);
    }
}

if (rowView) out.println("</div>");
out.print(replaceFields(prop.getText("components.formsimple.form.end"), formName, recipients, null, requiredLabelAdd, isEmailRender, false, firstTimeHeadingSet, prop));

if (isEmailRender)
{
    if(attributes.get("emailTextAfter") != null) out.print("<br/>"+ResponseUtils.filter(attributes.get("emailTextAfter")).replaceAll("\\n", "<br/>")+"<br/>");
    out.print("<!-- formmail crop form end -->");
} else {
   if (containsWysiwyg) {
      %>
         <%@include file="/components/_common/cleditor/jquery.cleditor.js.jsp" %>
         <script type="text/javascript">
			$(document).ready(function() {
				window.setTimeout(function() {
               $("textarea.formsimple-wysiwyg").cleditor({
                  width      : "100%",
                  controls   : "bold italic underline bullets numbering outdent indent image link icon size color highlight pastetext",
                  bodyStyle  : "font: 11px  Arial, Helvetica, sans-serif;"
               });
            }, 1000);
			});
         </script>
      <%
   }
}
%>