package sk.iway.iwcm.editor.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.EditorToolsForCore;
import sk.iway.iwcm.doc.AtrBean;
import sk.iway.iwcm.doc.AtrDB;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.datatable.NotifyBean.NotifyType;

public class EditorUtils {

    private EditorUtils() {}

    /**
	 * V HTML kode stranky nahradi medzeru za nedelitelnu medzeru pred spojkami.
	 * Tie sa definuju v konf. premennej editorSingleCharNbsp.
	 * @param editedDoc
	 */
	public static void nonBreakingSpaceReplacement(DocDetails editedDoc) {

		String conjunctions = Constants.getString("editorSingleCharNbsp");
		if (Tools.isNotEmpty(conjunctions)) {
			editedDoc.setData(editedDoc.getData().replaceAll("(?i)(\\s|&nbsp;)(" + conjunctions.replace(',', '|') + ")\\s", "$1$2&nbsp;"));
			//volame dva krat, pretoze sa nenahradzali 2 pismena za sebou, napr. Test a v case
			editedDoc.setData(editedDoc.getData().replaceAll("(?i)(\\s|&nbsp;)(" + conjunctions.replace(',', '|') + ")\\s", "$1$2&nbsp;"));
		}

	}

    /**
	 * Vrati orezany/ocisteny HTML kod:
	 * - definovany <!-- zaciatok textu iwcm editor --> a <!-- koniec textu iwcm editor -->
	 * - definovany konf. premennymi htmlImportStart a htmlImportEnd
	 * @param data
	 * @return
	 */
	public static String getCleanBody(String data) {

		//odstran vsetko pred <body> a po </body>
		String dataLCase = data.toLowerCase();

		int index = dataLCase.indexOf("<!-- zaciatok textu iwcm editor -->");
		if (index >= 0) {
			int index2;
			if (index >= 0) {
				index2 = dataLCase.indexOf(">", index + 1);
				if (index2 > index && index2 < dataLCase.length()) {
					data = data.substring(index2 + 1);
				}
			}

			index = data.toLowerCase().indexOf("<!-- koniec textu iwcm editor -->");
			if (index > 0) {
				data = data.substring(0, index);
			}
		}

		dataLCase = data.toLowerCase();

		//orezanie HTML
		if (Tools.isEmpty(Constants.getString("htmlImportStart")) == false && Tools.isEmpty(Constants.getString("htmlImportEnd")) == false && dataLCase.indexOf(Constants.getString("htmlImportStart").toLowerCase()) >= 0) {
			index = dataLCase.indexOf(Constants.getString("htmlImportStart").toLowerCase());
			int index2;
			if (index >= 0) {
				index2 = dataLCase.indexOf(">", index + 1);
				if (index2 > index && index2 < dataLCase.length()) {
					//odrezeme zaciatok
					data = data.substring(index2 + 1);
				}
			}

			index = data.toLowerCase().indexOf(Constants.getString("htmlImportEnd").toLowerCase());
			if (index > 0) {
				//odrezeme koniec
				data = data.substring(0, index);
			}
		}
		return (data);
	}

	/**
	 * Z textu odstrani neplatne znaky (napr. tlac strany a podobne, co sa moze nachadzat v PDF subore).
	 * Tie sa definuju v konf. premennej editorInvalidCharactersInDecFormat.
	 * @param text
	 * @param prop
	 * @param notify
	 * @return
	 */
    public static String escapeInvalidCharacters(String text, Prop prop, List<NotifyBean> notify) {

		if(Tools.isEmpty(text)) return text;

		String editorInvalidCharactersInDecFormat = Constants.getString("editorInvalidCharactersInDecFormat");
		int[] charsForEscape = null;
		if(Tools.isNotEmpty(editorInvalidCharactersInDecFormat)) charsForEscape = Tools.getTokensInt(editorInvalidCharactersInDecFormat, ",");

		StringBuilder sb = new StringBuilder("");
		char[] textCharArry = text.toCharArray();
		if(textCharArry != null) {
			boolean wasAppend = false;
			for(char textChar : textCharArry) {
				//Logger.debug(EditorService.class, textChar+" is:"+Character.isBmpCodePoint(textChar)+" high="+Character.isHighSurrogate(textChar));
				//if(Character.isISOControl(textChar) == false && Character.isBmpCodePoint(textChar)) sb.append(textChar);
				wasAppend = false;
				if(charsForEscape != null) {
					for(int charForEscape : charsForEscape) {
						if(charForEscape == (textChar)) {
							sb.append(" ");
							wasAppend = true;
							break;
						}
					}
				}

				if (notify != null && Character.isHighSurrogate(textChar)) {
                    NotifyBean notifyBean = new NotifyBean(prop.getText("text.warning"), prop.getText("editor.escapeInvalidChar.notifyText"), NotifyType.WARNING);
					notify.add(notifyBean);
				}

				if(!wasAppend) {
					if(Character.isISOControl(textChar) == false && Character.isHighSurrogate(textChar) == false) sb.append(textChar);
					else sb.append(" ");
				}
			}
		}

		return sb.toString();
	}


	/**
	 * Vrati hodnotu pre full text vyhladavanie - stlpec data_asc v databaze
	 * @param data
	 * @param editedDoc
	 * @param isLucene
	 * @param request
	 * @return
	 */
	public static String getDataAsc(String data, DocDetails editedDoc, boolean isLucene, HttpServletRequest request)
    {
        boolean isRendered = false;
        if (Constants.getBoolean("fulltextExecuteApps") && Tools.isEmpty(editedDoc.getExternalLink()))
        {

            String renderedData = EditorToolsForCore.renderIncludes(editedDoc, true, request);
            if (Tools.isNotEmpty(renderedData))
            {
                //odpaz domeny a blbosti
                String domain = Tools.getBaseHref(request);
                renderedData = Tools.replace(renderedData, domain, "");

                domain = Tools.getBaseHrefLoopback(request);
                renderedData = Tools.replace(renderedData, domain, "");

                if (request!=null)
                {
                    domain = "http://" + request.getServerName();
                    renderedData = Tools.replace(renderedData, domain, "");

                    domain = "https://" + request.getServerName();
                    renderedData = Tools.replace(renderedData, domain, "");

                    domain = "http://" + DocDB.getDomain(request);
                    renderedData = Tools.replace(renderedData, domain, "");

                    domain = "https://" + DocDB.getDomain(request);
                    renderedData = Tools.replace(renderedData, domain, "");

                    domain = ":"+request.getLocalPort()+"/";
                    renderedData = Tools.replace(renderedData, domain, "/");
                }

                domain = ":8080/";
                renderedData = Tools.replace(renderedData, domain, "/");

                domain = ":"+ Constants.getInt("httpServerPort")+"/";
                renderedData = Tools.replace(renderedData, domain, "/");

                data = renderedData;
                isRendered = true;
            }
        }

        String dataAsc;

        if (isLucene || isRendered) dataAsc = data;
        else dataAsc = (DB.internationalToEnglish(data).trim()).toLowerCase();

        dataAsc = EditorToolsForCore.removeHtmlTagsKeepLength(dataAsc);
		dataAsc = Tools.replace(dataAsc, "&nbsp;", "      ");

        if (editedDoc != null)
        {
            //ak tam nie je title, dopln
            String titleAsc = (DB.internationalToEnglish(editedDoc.getTitle()).trim()).toLowerCase();
            //aby spravne hladalo aj v nazvoch suborov len s pouzitim contains
            titleAsc = Tools.replace(titleAsc, "_", " ");
            titleAsc = Tools.replace(titleAsc, "-", " ");
            titleAsc = Tools.replace(titleAsc, ".", " ");
            titleAsc = Tools.replace(titleAsc, "/", " ");
            if (isLucene)
            {
                if (dataAsc.indexOf(editedDoc.getTitle())==-1) dataAsc += " "+editedDoc.getTitle()+"\n";
            }
            else
            {
                if (dataAsc.indexOf(titleAsc)==-1) dataAsc += "<h1>"+titleAsc+"</h1>\n";
            }

            DocDB docDB = DocDB.getInstance();
            //ak treba dopln keywords
            if (Constants.getBoolean("fulltextIncludeKeywords"))
            {
				String[] perexGroupIds = editedDoc.getPerexGroupNames();
				if (perexGroupIds != null)
				{
					StringBuilder keywords = new StringBuilder();
					for (String keyword : perexGroupIds)
					{
						keyword = docDB.convertPerexGroupIdToName(Tools.getIntValue(keyword, -1));
						if (Tools.isEmpty(keyword)) continue;
						if (keyword.startsWith("#") || keyword.startsWith("@") || keyword.startsWith("_")) keyword=keyword.substring(1);
						if (keywords.length()>0)keywords.append(", ");
						keywords.append(keyword);
					}
					if (Tools.isNotEmpty(keywords.toString()))
					{
						if (isLucene) dataAsc += (DB.internationalToEnglish(keywords.toString()).trim()).toLowerCase()+"\n";
						else dataAsc += "<div style='display:none' class='fulltextKeywords'>"+(DB.internationalToEnglish(keywords.toString()).trim()).toLowerCase()+"</div>\n";
					}
				}
            }
            if (Constants.getBoolean("fulltextIncludePerex") && Tools.isNotEmpty(editedDoc.getHtmlData()))
            {
                if (isLucene) dataAsc += (DB.internationalToEnglish(editedDoc.getHtmlData()).trim()).toLowerCase()+"\n";
                else dataAsc += "<div style='display:none' class='fulltextPerex'>"+(DB.internationalToEnglish(editedDoc.getHtmlData()).trim()).toLowerCase()+"</div>\n";
            }
            //ak treba dopln atributy
            if (Constants.getBoolean("fulltextIncludeAttributes"))
            {
				List<AtrBean> attrs = AtrDB.getAtributes(editedDoc.getDocId(), null, null);	//ziskam vsetky atributy pre danu stranku
				if (attrs != null && attrs.isEmpty()==false)
				{
					StringBuilder attributes = new StringBuilder();
					for (AtrBean attr : attrs)
					{
						if (attr == null) continue;
						if(Tools.isNotEmpty(attr.getAtrName()) && Tools.isNotEmpty(attr.getValueString()))
						{
							if (attributes.length()>1) attributes.append(", ");
							attributes.append(attr.getAtrName()).append("=").append(attr.getValueString());
						}
					}

					if (Tools.isNotEmpty(attributes.toString()))
					{
						if (isLucene) dataAsc += (DB.internationalToEnglish(attributes.toString()).trim()).toLowerCase()+"\n";
						else dataAsc += "<div style='display:none' class='fulltextAttributes'>"+(DB.internationalToEnglish(attributes.toString()).trim()).toLowerCase()+"</div>\n";
					}
				}
            }

            //#22131
            if(Tools.isNotEmpty(Constants.getString("fulltextDataAscMethod")))
            {
                String className = "";
                String methodName = "";
                try {
                    className = Constants.getString("fulltextDataAscMethod").substring(0, Constants.getString("fulltextDataAscMethod").lastIndexOf("."));
                    methodName = Constants.getString("fulltextDataAscMethod").substring(Constants.getString("fulltextDataAscMethod").lastIndexOf(".") + 1);

                    Class<?> clazz = Class.forName(className);
                    Method method = clazz.getMethod(methodName, DocDetails.class);
                    String returned = (String)method.invoke(null, editedDoc);
                    if(Tools.isNotEmpty(returned))
                        dataAsc += returned;
                } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException e) {
                    Logger.debug(EditorUtils.class, "ReflectionLoader - " + className + "." + methodName + " exception");
					Logger.error(EditorUtils.class, e);
                }
            }
        }

		dataAsc = Tools.replace(dataAsc, "&nbsp;", "      ");
        dataAsc = Tools.replaceStrings(dataAsc, "searchIndexReplaceStrings", false);

        return dataAsc;
    }
}
