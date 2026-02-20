package sk.iway.iwcm.form;

import static sk.iway.iwcm.Tools.isEmpty;
import static sk.iway.iwcm.Tools.isInteger;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sk.iway.iwcm.Logger;

/**
 *  FormResultsParser.java
 *  
 *  Parses !INCLUDE configuration supplied by form_results.jsp's config attribute.
 *  Individual columns are separated by | character, whereas its attribute are separated by ~.  	
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 16.6.2010 15:32:28
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class FormResultsParser
{
	
	String originalConfig;
	List<String> columnNames;
	Map<String, String> columnToLabels;
	Map<String, String> columnToWidth;
	
	public FormResultsParser(String originalConfig) throws ParseException
	{
		this.originalConfig = originalConfig;
		columnNames = new ArrayList<String>();
		columnToLabels = new HashMap<String, String>();
		columnToWidth = new HashMap<String, String>();
		parse();
	}

	private void parse() throws ParseException
	{
		Logger.debug(FormResultsParser.class, "Parsing: "+originalConfig);
		for (String columnConfig : originalConfig.split("[|]"))
		{
			if (isEmpty(columnConfig)) continue;
			String[] properties = columnConfig.split("~");
			if (properties.length != 3)
				throw new ParseException("Column configuration malformed: 3 subparts expected: "+columnConfig, originalConfig.indexOf(columnConfig));
			String columnName = properties[0].trim();
			columnNames.add(columnName);
			columnToLabels.put(columnName, properties[1].trim());
			columnToWidth.put(columnName, toCssWidth(properties[2].trim()));
		}
		Logger.debug(FormResultsParser.class, "Results of parsing: columnNames: "+columnNames+" columnLabels: "+columnToLabels.values());
	}

	private String toCssWidth(String wildCard) throws ParseException
	{
		if ("*".equals(wildCard)) return "auto";
		if (isInteger(wildCard)) return wildCard+"px";
		if (wildCard.endsWith("%")) return wildCard;
		throw new ParseException("Unknown column width: "+wildCard, 0);
	}

	public List<String> getColumnNames()
	{
		return new ArrayList<String>(columnNames);
	}
	
	public String getLabelFor(String column)
	{
		return columnToLabels.get(column);
	}
	
	public String getCssWidthFor(String column)
	{
		return columnToWidth.get(column);
	}
}
