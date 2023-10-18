package sk.iway.iwcm.database;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmOutputStream;

/**
 *  DumpSelectQuery.java
 *
 *  Creates an SQL dump file out of the result of a select statement.
 *  Such a query can even be a result of a query with conditions, restricting
 *  the amount of rows dumped
 *
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 11.3.2011 15:36:28
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class DumpSelectQuery
{
	private final String table;
	private final IwcmFile file;
	private final String sql;
	private final Object[] parameters;
	private StringBuilder insertTemplate;
	private int columnCount;
	private Map<Integer, Integer> columnTypes;
	private List<Integer> numberTypes = Arrays.asList(Types.BIGINT, Types.INTEGER, Types.BIT, Types.BOOLEAN, Types.DECIMAL,
		Types.DOUBLE, Types.FLOAT, Types.TINYINT, Types.NUMERIC, Types.REAL, Types.SMALLINT
	);


	public DumpSelectQuery(String table, IwcmFile file, String sql, Object...parameters)
	{
		this.table = table;
		this.file = file;
		this.sql = sql;
		this.parameters = parameters;
	}

	public void dump() throws IOException
	{
		BufferedOutputStream output = new BufferedOutputStream(new IwcmOutputStream(file));
		dumpTo(output);
		output.close();
	}

	private void dumpTo(BufferedOutputStream output)
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection();
			ps = db_conn.prepareStatement(sql);
			int parameterIndex = 1;
			for (Object param : parameters)
				ps.setObject(parameterIndex++, param);

			rs = ps.executeQuery();

			while (rs.next())
			{
				if (insertTemplate == null)
					createInsertTemplate(rs);
				appendRowTo(output, rs);
			}
			rs.close();
			ps.close();
			db_conn.close();
			rs = null;
			ps = null;
			db_conn = null;
		}
		catch (Exception ex) {sk.iway.iwcm.Logger.error(ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}
			catch (Exception ex2){}
		}
	}

	private void createInsertTemplate(ResultSet rs) throws SQLException
	{
		columnTypes = new HashMap<Integer, Integer>();
		insertTemplate = new StringBuilder();
		insertTemplate.append("INSERT INTO ").append(table).append(" (");
		columnCount = rs.getMetaData().getColumnCount();
		for (int columnIndex = 1 ; columnIndex <= columnCount; columnIndex++)
		{
			columnTypes.put(columnIndex, rs.getMetaData().getColumnType(columnIndex));
			String label = rs.getMetaData().getColumnLabel(columnIndex);
			insertTemplate.append(label).append(',');
		}
		//delete the last comma
		insertTemplate.deleteCharAt(insertTemplate.length() - 1);
		insertTemplate.append(") VALUES(");

		for (int columnIndex = 1 ; columnIndex <= columnCount; columnIndex++)
		{
			insertTemplate.append("${").append(columnIndex).append('}').append(',');
		}
		insertTemplate.deleteCharAt(insertTemplate.length() - 1);
		insertTemplate.append(");\n");
	}

	private void appendRowTo(BufferedOutputStream output, ResultSet rs) throws SQLException, UnsupportedEncodingException, IOException
	{
		String row = insertTemplate.toString();

		for (int columnIndex = 1 ; columnIndex <= columnCount; columnIndex++)
		{
			Object object = rs.getObject(columnIndex);
			//object is a NULL reference - insert without quotes, ignoring its column type
			if (object == null)
			{
				row = row.replace("${"+columnIndex+"}", "NULL");
				continue;
			}
			//insert booleans as 0/1 instead of true/false JDBC returns
			if (Boolean.TRUE.equals(object) || Boolean.FALSE.equals(object))
			{
				int numericValue = Boolean.TRUE.equals(object) ? 1 : 0;
				row = row.replace("${"+columnIndex+"}", String.valueOf(numericValue));
				continue;
			}
			boolean isNumber = isNumber(columnIndex);
			if (isNumber)
			{
				row = row.replace("${"+columnIndex+"}", String.valueOf(object));
			}
			else
			{
				if (object instanceof Clob)
				{
					Clob clob = (Clob)object;
					long length = clob.length();
					object = clob.getSubString(1, (int) length);
				}
				else
					object = rs.getString(columnIndex);
				object = ((String)object).replace("'", "\\'");
				row = row.replace("${"+columnIndex+"}", "'"+object+"'");
			}
		}

		output.write(row.getBytes("UTF-8"));
	}

	private boolean isNumber(int columnIndex)
	{
		return numberTypes.contains(columnTypes.get(columnIndex));
	}
}