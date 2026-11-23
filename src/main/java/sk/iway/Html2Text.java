package sk.iway;

import java.util.List;
import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 * Converts HTML to plain text
 *
 *@Title        magma-web
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.5 $
 *@created      Piatok, 2003, november 28
 *@modified     $Date: 2005/10/25 06:48:04 $
 */
public class Html2Text
{
	private Document doc;

	public Html2Text(String html) {
		//utility class
		doc = Jsoup.parse(html);
	}

	/**
	 * Converts HTML to plain text, leaving input without angle brackets unchanged.
	 * @param html HTML or plain text; may be null
	 * @return extracted text, unchanged input without angle brackets, or an empty string for null
	 */
	public static String html2text(String html)
	{
		return html2text(html, false);
	}

	/**
	 * Converts HTML to plain text, optionally parsing input without HTML tags.
	 * @param html HTML or plain text; may be null
	 * @param forceHtml true to always parse the input, decoding entities and normalizing whitespace
	 * @return extracted text, unchanged input when parsing is skipped, or an empty string for null
	 */
	public static String html2text(String html, boolean forceHtml)
	{
		if (html == null) return("");
		if (forceHtml || (html.contains("<") && html.contains(">"))) {
			return new Html2Text(html).getText();
		} else {
			return html;
		}
	}

	/**
	 * Returns plain text from HTML
	 * @return
	 */
	public String getText() {
		return doc.text();
	}

	/**
	 * Returns List of texts in HTML by selector (eg h1,h2)
	 * @param jsoup
	 * @param selector
	 * @return
	 */
	public List<String> getTextByElement(String selector) {
		Elements tags = doc.select(selector);
		List<String> texts = new ArrayList<>();
		for (String text : tags.eachText()) {
			texts.add(text);
		}
		return texts;
	}
}
