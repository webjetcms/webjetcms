package sk.iway.iwcm.system.fulltext.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.util.NumericUtils;
import org.apache.lucene.util.Version;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;

/**
 *  WebJETQueryParser.java - query parser, ktory pozna Numeric fieldy
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2013
 *@author       $Author: jeeff jeeff $
 *@version      $Revision: 1.3 $
 *@created      Date: 3.7.2013 15:20:13
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class WebJETQueryParser extends QueryParser
{
	private static String numericFields[] = {"author_id", "group_id", "temp_id", "sort_priority", "password_protected"};
	
	private boolean isNumericField(String name)
	{
		for (String field : numericFields)
		{
			if (field.equals(name)) return true;
		}
		return false;
	}

	public WebJETQueryParser(Version matchVersion, String f, Analyzer a)
	{
		super(matchVersion, f, a);
	}

	@Override
	public org.apache.lucene.search.Query getRangeQuery(String field, String part1, String part2, boolean inclusive) throws ParseException 
	{
		Logger.debug(WebJETQueryParser.class, "Range query");
		TermRangeQuery query = (TermRangeQuery)super.getRangeQuery(field, part1, part2, inclusive);
		
		if (isNumericField(field)) 
		{
			Logger.debug(WebJETQueryParser.class, "Range query - numeric, name="+field+" low="+query.getLowerTerm()+" up="+query.getUpperTerm());		
			return NumericRangeQuery.newIntRange(field, Tools.getIntValue(query.getLowerTerm(), 0), Tools.getIntValue(query.getUpperTerm(), 0), query.includesLower(), query.includesUpper());
		}
		return query;
	}

	@Override
	protected org.apache.lucene.search.Query getFieldQuery(String field, String queryText, boolean quoted) throws ParseException
	{
		org.apache.lucene.search.Query superQuery = super.getFieldQuery(field, queryText, quoted);
		
		if (isNumericField(field))
		{
			Logger.debug(WebJETQueryParser.class, "Som numeric field: "+field+" text="+queryText);			
			superQuery = new TermQuery(new org.apache.lucene.index.Term(field, NumericUtils.intToPrefixCoded(Tools.getIntValue(queryText, 0))));
		}
		
		return superQuery;
	}
	
}
