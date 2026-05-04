package sk.iway.iwcm.system.fulltext.jobs;

import sk.iway.iwcm.system.fulltext.FulltextSearch;
import sk.iway.iwcm.system.fulltext.indexed.Documents;
import sk.iway.iwcm.system.fulltext.lucene.IndexSearcherBuilder;

/**
 *  DocumentIndexingJob.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: jeeff thaber $
 *@version      $Revision: 1.3 $
 *@created      Date: 20.5.2011 10:56:50
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class DocumentsIndexing
{
	public static void main(String[] args)
	{
		try
		{
			if (args == null || args.length == 0)
			{
				System.err.println("No languages to index. Exiting."); //NOSONAR
				return;
			}

			for (String language:args)
			{
				Documents indexed = new Documents(language);
				FulltextSearch.index(indexed, null);
				IndexSearcherBuilder.refresh();
			}
		}
		catch (Exception e){
			sk.iway.iwcm.Logger.error(e);
		}
	}
}
