package sk.iway.css;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmInputStream;
import sk.iway.iwcm.utils.Pair;

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

	List<Pair<String, String>> elements = new LinkedList<Pair<String, String>>();

	public CssParser(IwcmFile css)
	{
		parseCssFile(css);
	}

	private void parseCssFile(IwcmFile css)
	{
		Reader in = null;
		Scanner scanner = null;
		boolean success = false;
		try
		{
			in = new InputStreamReader(new IwcmInputStream(css));
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
			String text = cleanText(scanner.next());

			if (text.contains("@media") || text.contains("/** Editor end **/") || text.contains(".cssParserEnd")) {
				Logger.debug(CssParser.class, "break");
				return false;
			}

			if (text.contains(" ") || text.contains("#") || !text.contains(".")) {
				continue;
			}

			Pair<String, String> result = new Pair<String, String>(parseTag(text), parseClass(text));
			elements.add(result);
        }

		return true;
	}

	private void parseEditorComments(IwcmFile css) {

		File file = new File(css.getAbsolutePath());

		try (FileReader fr = new FileReader(file); BufferedReader br = new BufferedReader(fr);) {
			String line;
			String lastLine = null;
			while ((line = br.readLine()) != null) {

				if (line.contains("/* editor */") && lastLine != null) {
					String text = cleanText(lastLine);
					if (text.contains(" ") || text.contains("#") || !text.contains(".")) {
						continue;
					}
					Pair<String, String> result = new Pair<String, String>(parseTag(text), parseClass(text));
					elements.add(result);
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

	private String cleanText(String text)
	{
		if (text.contains("{")) {
			text = text.substring(0, text.indexOf("{")).trim();
		}
		text = text.trim().replaceAll("\\r\\n|\\r|\\n|\\t", "");

		return text;
	}

	public List<Pair<String, String>> getElements()
	{
		return elements;
	}

	public void setElements(List<Pair<String, String>> elements)
	{
		this.elements = elements;
	}
}