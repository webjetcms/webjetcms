package sk.iway.iwcm.system.fulltext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.CachingWrapperFilter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.util.Version;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.AdminTools;
import sk.iway.iwcm.system.fulltext.indexed.Indexed;
import sk.iway.iwcm.system.fulltext.lucene.AnalyzerFactory;
import sk.iway.iwcm.system.fulltext.lucene.IndexSearcherBuilder;
import sk.iway.iwcm.system.fulltext.lucene.LuceneUtils;
import sk.iway.iwcm.system.fulltext.lucene.WebJETQueryParser;

/**
 *
 * Query Interface for Lucene
 *
 *
 *@Title webjet7
 *@Company Interway s.r.o. (www.interway.sk)
 *@Copyright Interway s.r.o. (c) 2001-2011
 *@author $Author: jeeff thaber $
 *@version $Revision: 1.3 $
 *@created Date: 7.4.2011 11:19:00
 *@modified $Date: 2004/08/16 06:26:11 $
 */
public class LuceneQuery
{
	private Sort sort;
	private Indexed indexed;
	private IndexSearcher searcher;
	private Query parsedQuery;
	private String language;


	public LuceneQuery(String language)
	{
		this.language = language;
		searcher = IndexSearcherBuilder.build(language);
	}


	public LuceneQuery(Indexed indexed)
	{
		this.indexed = indexed;
		if (indexed != null ){
			searcher = IndexSearcherBuilder.build(indexed.language());
			this.language = indexed.language();
		}
		else
		{
			searcher = IndexSearcherBuilder.build(AdminTools.defaultLanguage());
			this.language = AdminTools.defaultLanguage();
		}
	}

	public List<Document> documents(String query, List<Query> rangeQueries)
	{

			boolean hasCriteria = Tools.isNotEmpty(query);
			if (indexed!=null)
			{
				if(hasCriteria) query += " AND ";
				query += " type:"+indexed.name();
				hasCriteria = true;
			}
			if(hasCriteria) query += " AND ";
			query+= " NOT(data:"+LuceneUtils.EMPTY+")";
			hasCriteria = true;

			try
			{
				QueryParser parser = new WebJETQueryParser(Version.LUCENE_30, Indexed.defaultField(), AnalyzerFactory.getAnalyzer(Version.LUCENE_31,this.language));
				//QueryParser parser = new QueryParser(Version.LUCENE_30, Indexed.defaultField(), new CustomAnalyzer(Version.LUCENE_31,this.language));
				parsedQuery = parser.parse(query);

				/**
				 * MHA 2013-09-25 13:03
				 * hotfix na 14379 komentare 16  a 17, nevedno preco, queryparser nastavi occur pre clause na fora na MUST a potom sa nenajdu ziadne aktuality
				 * pritom pri skladani query sa pred klauzulu s forami ci tiketami dava OR tak neviem preco to takto sparsuje..
				 **/
				if (parsedQuery instanceof BooleanQuery) {
				    BooleanQuery booleanQuery = (BooleanQuery) parsedQuery;
				    BooleanClause[] clauses = booleanQuery.getClauses();
				    for (int i = 0; i < clauses.length; i++) {
				   	 if(clauses[i].getQuery().toString().contains("type:forums") || clauses[i].getQuery().toString().contains("type:tickets"))
				   		 clauses[i].setOccur(BooleanClause.Occur.SHOULD);
				    }
				}

				/*TermQuery groupIdTerm = new TermQuery(new Term("group_id", NumericUtils.intToPrefixCoded(250)));
				BooleanQuery bq = new BooleanQuery();
				bq.add(parsedQuery, BooleanClause.Occur.MUST);
				bq.add(groupIdTerm, BooleanClause.Occur.MUST);
				*/

				TopFieldDocs topFieldDocs = null;

				if (sort == null)
				{
					sort = new Sort();
				}

				if(rangeQueries != null && rangeQueries.size()>0)
				{
					rangeQueries.add(parsedQuery);
					parsedQuery = parsedQuery.combine(rangeQueries.toArray(new Query[]{}));
				}

				Logger.debug(getClass(), "Lucene query: " + parsedQuery.toString());
				topFieldDocs = searcher.search(parsedQuery, new CachingWrapperFilter(new QueryWrapperFilter(parsedQuery)), Tools
							.getIntValue(Constants.getString("luceneResultsLimit"), 100), sort);
				int i = 0;
				if (topFieldDocs.totalHits > 0)
				{
					List<Document> documents = new ArrayList<Document>();
					while (i < topFieldDocs.scoreDocs.length)
					{
						int luceneDocId = topFieldDocs.scoreDocs[i].doc;
						try
						{
							documents.add(searcher.doc(luceneDocId));
						}
						catch (IOException e)
						{
							sk.iway.iwcm.Logger.error(e);
						}
						i++;
					}
					return documents;
				}
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
			finally
			{
				close();
			}
			return Collections.emptyList();

	}

	public List<Document> documents(String query)
	{
		return documents(query, null);
	}

	/**
	 * @param sort
	 *           The sort to set.
	 */
	public void setSort(Sort sort)
	{
		this.sort = sort;
	}
	/**
	 * @return Returns the parsedQuery.
	 */
	public Query getParsedQuery()
	{
		return parsedQuery;
	}


	private void close()
	{
			IndexSearcherBuilder.close(this.language);
	}
}
