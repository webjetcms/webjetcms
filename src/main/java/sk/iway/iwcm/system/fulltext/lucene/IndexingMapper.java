package sk.iway.iwcm.system.fulltext.lucene;

import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.lucene.index.IndexWriter;

import sk.iway.iwcm.database.Mapper;

/**
 *  IndexingMapper.java
 *  Indexes parsed Documents
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: jeeff thaber $
 *@version      $Revision: 1.3 $
 *@created      Date: 7.4.2011 13:57:11
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
@SuppressWarnings("rawtypes")
public abstract class IndexingMapper implements Mapper
{

	protected IndexWriter writer;
	protected Writer log;
	public IndexingMapper(IndexWriter writer, Writer log)
	{
		this.writer = writer;
		this.log = log;
	}

	public abstract Void map(ResultSet rs) throws SQLException;

}
