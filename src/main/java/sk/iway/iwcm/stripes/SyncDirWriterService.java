package sk.iway.iwcm.stripes;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import net.sourceforge.stripes.action.ActionBeanContext;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.sync.inport.Numbered;

public class SyncDirWriterService {

	// Private constructor to hide the implicit public one
	private SyncDirWriterService() {}
    
	/**
	 * Prepare progress div (via writter), where wi will updating the value (progress)
	 * @param headlineText
	 * @param valueFieldId
	 * @param valueFieldText
	 * @param writer
	 */
    public static void prepareProgress(String headlineText, String valueFieldId, String valueFieldText, PrintWriter writer) { 
		prepareProgress(headlineText, valueFieldId, valueFieldText, "", writer);
	}

	/**
	 * Prepare progress div (via writter), where wi will updating the value (progress)
	 * @param headlineText
	 * @param valueFieldId
	 * @param valueFieldText
	 * @param addClass - specific class to be add to main div
	 * @param writer
	 */
    public static void prepareProgress(String headlineText, String valueFieldId, String valueFieldText, String addClass, PrintWriter writer) {
		if(addClass == null) addClass = "";
		writer.println("<div class='padding10 " + addClass + "'>");
		writer.println("<b>" + headlineText + "</b>");
		writer.println("<p id='" + valueFieldId + "'>" + valueFieldText + "</p>");
		writer.println("</div>");
		writer.flush();
	}

	/**
	 * Update progress value craeted by method {@link #prepareProgress(String, String, String, PrintWriter)}
	 * @param valueFieldId
	 * @param valueFieldText
	 * @param writer
	 */
	public static void updateProgress(String valueFieldId, String valueFieldText, PrintWriter writer) {
		writer.write("<script language='javascript'> $('p#" + valueFieldId + "').text('" + valueFieldText + "'); </script>");
		writer.flush();
	}

	/**
	 * Count how many item datas are in selected
	 * @param <E>
	 * @param selected
	 * @param datas
	 * @param prefix
	 * @return
	 */
	public static <E> int getCountToHandle(Map<String, String> selected, Iterable<Numbered<E>> datas, String prefix) {
		int count = 0;
		for (Numbered<E> remoteFile : datas) {
			if (selected.get(prefix + remoteFile.number) != null) { count++; }
		}
		return count;
	}

	/**
	 * Print status message via writer
	 * @param textKey
	 * @param mainTitle
	 * @param context
	 * @param request
	 */
	public static void printStatusMsg(String textKey, boolean mainTitle, ActionBeanContext context, HttpServletRequest request) {
		try {
			HttpServletResponse response = context.getResponse();
        	PrintWriter writer = response.getWriter();
			writer.println("<div class='statusMsg'>");
			
			if(mainTitle)
				writer.println("<h3>" + Prop.getInstance(request).getText(textKey)+"</h3>");
			else
				writer.println("<b>" + Prop.getInstance(request).getText(textKey)+"</b>");
			
			writer.println("</div>");
			writer.flush();
		} catch (IOException e) {
			sk.iway.iwcm.Logger.error(e);
		}
	}

	public static Map<String, String> getOptionsMap(String prefix, HttpServletRequest request) {
		Map<String, String> optionsMap = new HashMap<>();

		String[] selected = Tools.getTokens(request.getParameter(prefix + "options"), ",");
		if(selected.length < 1) return optionsMap;
		for (String el : selected) { optionsMap.put(el, el); }

		return optionsMap;
	}
}
