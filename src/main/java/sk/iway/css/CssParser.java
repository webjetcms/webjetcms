package sk.iway.css;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmInputStream;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class CssParser {

	List<CssDto> elements = new LinkedList<CssDto>();

	public CssParser(IwcmFile css)
	{
		parseCssFile(css);
	}

	private void parseCssFile(IwcmFile css)
	{
		IwcmInputStream is = null;
		Reader in = null;
		Scanner scanner = null;
		boolean success = false;
		try
		{
			is = new IwcmInputStream(css);
			in = new InputStreamReader(is);
			scanner = new Scanner(in);
			scanner.useDelimiter("}");

			success = parse(scanner);
		}
		catch (FileNotFoundException e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		finally {
			try
			{
				if (scanner!=null) scanner.close();
				if (in!=null) in.close();
				if (is!=null) is.close();
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
			}
		}

		if (success==false) {
			parseEditorComments(css);
		}
	}

	private boolean parse(Scanner scanner)
	{
		while (scanner.hasNext()) {
			String line = scanner.next();
			String text = cleanText(line);

			if (text.contains("@media") || text.contains("/** Editor end **/") || text.contains(".cssParserEnd")) {
				return false;
			}

			if (text.contains(" ") || text.contains("#") || !text.contains(".")) {
				continue;
			}

			CssDto dto = new CssDto();
			dto.setTag(parseTag(text));
			dto.setClassName(parseClass(text));
			dto.setTitle(parseTitle(line));
			elements.add(dto);
        }

		return true;
	}

	private void parseEditorComments(IwcmFile css) {

		File file = new File(css.getAbsolutePath());

		try (FileReader fr = new FileReader(file); BufferedReader br = new BufferedReader(fr);) {
			String line;
			String lastLine = null;
			while ((line = br.readLine()) != null) {

				if (line.contains("/* editor") && line.contains(" */") && lastLine != null) {
					String text = cleanText(lastLine);
					if (text.contains(" ") || text.contains("#") || !text.contains(".")) {
						continue;
					}
					CssDto dto = new CssDto();
					dto.setTag(parseTag(text));
					dto.setClassName(parseClass(text));
					dto.setTitle(parseTitle(line));
					elements.add(dto);
				}

				lastLine = line;
			}
		} catch (IOException e) {
			sk.iway.iwcm.Logger.error(e);
		}

	}

	private String parseTag(String element)
	{
		int dotIndex = element.indexOf(".");

		if (dotIndex == 0) {
			return "*";
		}

		return element.substring(0, dotIndex).trim();
	}

	private String parseClass(String element)
	{
		int dotIndex = element.indexOf(".");

		//replace btn.btn-primary na btn btn-primary
		return Tools.replace(element.substring(dotIndex + 1).trim(), ".", " ");
	}

	/**
	 * Parse title from comment in css element (must be on same line as element definition)
	 * example:
	 * .baretest2 { \/* Bare TEST 02 *\/
	 * or use comment with prefix /* title: Your Title *\/
	 * @param element
	 * @return
	 */
	private String parseTitle(String element)
	{
		if (element==null) {
			return null;
		}
		element = element.trim();
		if (element.contains("/* editor")) {
			//use comment with title prefix
			int titleStart = element.indexOf("/* editor title:") + 17;
			int titleEnd = element.indexOf("*/", titleStart);
			if (titleStart >= 0 && titleEnd > titleStart) {
				return element.substring(titleStart, titleEnd).trim();
			}
			return null;
		} else if (element.contains("\n")) {
			//strip first line
			element = element.substring(0, element.indexOf("\n"));
		}

		int commentStart = element.indexOf("/*");
		int commentEnd = element.indexOf("*/");
		if (commentStart >= 0 && commentEnd > commentStart) {
			return element.substring(commentStart + 2, commentEnd).trim();
		}
		return null;
	}

	private String cleanText(String text)
	{
		if (text.contains("{")) {
			text = text.substring(0, text.indexOf("{")).trim();
		}
		text = text.trim().replaceAll("\\r\\n|\\r|\\n|\\t", "");

		return text;
	}

	public List<CssDto> getElements()
	{
		return elements;
	}

	public void setElements(List<CssDto> elements)
	{
		this.elements = elements;
	}
}