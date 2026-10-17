package sk.iway.css;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmInputStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CssParser {

	List<CssDto> elements = new LinkedList<CssDto>();
	private final Map<CssDto, Integer> sourceOrder = new HashMap<>();

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
			in = new InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8);
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
		int ruleIndex = 0;
		while (scanner.hasNext()) {
			String line = scanner.next();
			int order = ruleIndex++;
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
			readMetadata(dto, line);
			elements.add(dto);
			sourceOrder.put(dto, order);
        }

		return true;
	}

	private void parseEditorComments(IwcmFile css) {
        try (Reader reader = new InputStreamReader(new IwcmInputStream(css), java.nio.charset.StandardCharsets.UTF_8);
                Scanner scanner = new Scanner(reader)) {
            parseEditorComments(scanner);
        } catch (IOException e) {
            sk.iway.iwcm.Logger.error(e);
        }
    }

    private void parseEditorComments(Scanner scanner) {
        scanner.useDelimiter("}");
        int ruleIndex = 0;
        while (scanner.hasNext()) {
            String rule = scanner.next();
            int order = ruleIndex++;
            int open = rule.lastIndexOf('{');
            if (open < 0 || !rule.substring(open).contains("/* editor")) continue;
            String selector = rule.substring(0, open);
            selector = selector.substring(Math.max(selector.lastIndexOf('{'), selector.lastIndexOf('}')) + 1).trim();
            // A rule following a media declaration may start on a new line.
            if (selector.contains("\n")) selector = selector.substring(selector.lastIndexOf('\n') + 1).trim();
            if (!selector.matches("[a-zA-Z0-9*-]*(?:\\.[a-zA-Z0-9_-]+)+")) continue;
            String tag = parseTag(selector);
            String classes = parseClass(selector);
            CssDto existing = elements.stream().filter(item -> tag.equals(item.getTag()) && classes.equals(item.getClassName()))
                    .min(Comparator.comparingInt(sourceOrder::get)).orElse(null);
            if (existing != null) {
                if (sourceOrder.get(existing) <= order) continue;
                elements.remove(existing);
                sourceOrder.remove(existing);
            }
            CssDto dto = new CssDto();
            dto.setTag(tag);
            dto.setClassName(classes);
            dto.setTitle(parseTitle(rule));
            readMetadata(dto, rule);
            elements.add(dto);
            sourceOrder.put(dto, order);
        }
        elements.sort(Comparator.comparingInt(sourceOrder::get));
    }

    /** Reads optional style behavior without interpreting CSS declarations or title markup. */
    private void readMetadata(CssDto dto, String rule) {
        dto.setCategory(metadata(rule, "category"));
        dto.setCategoryIcon(metadata(rule, "category-icon"));
        dto.setGroup(metadata(rule, "group"));
        dto.setTargets(metadata(rule, "targets"));
        dto.setBase(metadata(rule, "base"));
        dto.setMembers(metadata(rule, "members"));
        dto.setRequires(metadata(rule, "requires"));
    }

    private String metadata(String rule, String key) {
        Matcher matcher = Pattern.compile("/\\*\\s*editor " + key + ":\\s*(.*?)\\*/", Pattern.DOTALL).matcher(rule);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    /** Parses an in-memory stylesheet, for callers that already have its contents. */
    public CssParser(String css) {
        try (Scanner scanner = new Scanner(css)) {
            scanner.useDelimiter("}");
            if (!parse(scanner)) {
                try (Scanner comments = new Scanner(css)) {
                    parseEditorComments(comments);
                }
            }
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
		element = element.replaceFirst("^\\s*@charset\\s+[^;]+;", "").trim();
		if (element.contains("/* editor title:")) {
			//use comment with title prefix
			int titleStart = element.indexOf("/* editor title:") + 16;
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
			String title = element.substring(commentStart + 2, commentEnd).trim();
			return title.equals("editor") || title.startsWith("editor ") ? null : title;
		}
		return null;
	}

	private String cleanText(String text)
	{
		if (text.contains("{")) {
			text = text.substring(0, text.indexOf("{")).trim();
		}
		text = text.replaceFirst("^\\s*@charset\\s+[^;]+;", "");
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