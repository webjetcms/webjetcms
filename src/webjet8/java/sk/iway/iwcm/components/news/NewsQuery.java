package sk.iway.iwcm.components.news;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.news.criteria.Criteria;
import sk.iway.iwcm.components.news.criteria.DatabaseCriteria;
import sk.iway.iwcm.components.news.criteria.DatabaseCriteria.CriteriaType;
import sk.iway.iwcm.components.news.criteria.ResultCriteria;
import sk.iway.iwcm.doc.AtrBean;
import sk.iway.iwcm.doc.AtrDocBean;
import sk.iway.iwcm.doc.DataAccessHelper;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.PerexGroupBean;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.utils.Pair;

/**
 * Realizuje DB dotazy pre nove News komponenty. Zaujima ju len DB logika, nezaujima ju aka je to stranka, ci ma hladat len aktivne stranky a spol
 * @author mbocko
 *
 */
@SuppressWarnings({"unused", "unchecked"})
public class NewsQuery
{
	private static final int NOT_SET_INT = Integer.MIN_VALUE;

	private List<DatabaseCriteria> databaseCriteriaList = new LinkedList<DatabaseCriteria>();
	private List<ResultCriteria> resultCriteriaList = new LinkedList<ResultCriteria>();
	private List<Object> arguments = new LinkedList<Object>();
	private List<Pair<OrderEnum, SortEnum>> orderings = new LinkedList<Pair<OrderEnum,SortEnum>>();
	private int limit = NOT_SET_INT;
	private int offset = NOT_SET_INT;

	private int resultCriteriaTotalCount = 0;

	private int pageSize = NOT_SET_INT;
	private int page = NOT_SET_INT;
	private int initialOffset = 0;

	private boolean loadData = false;
	private boolean perexGroupUseJoin = false;

	private boolean useAtributeSearch = false;

	private boolean returnDocdetailsWithAtributes = false;


	/**
	 * vygeneruje DB dotaz na zaklade kriterii
	 * @param count vrati len pocet najdenych stranok
	 * @param noData vrati objekty bez atributu data
	 * @return
	 */
	private String getSelectSql(boolean count, boolean noData)
	{
		StringBuilder sql = new StringBuilder();

		sql.append("SELECT ");
		if (count)
			sql.append("count(d.doc_id) ");
		else
		{

			String docFields = FieldEnum.getFields();
			if (noData) docFields = FieldEnum.getFieldsNoData();
			//TODO: zatial, neskor treba dorobit
			//perexGroupUseJoin = false;//Constants.getBoolean("perexGroupUseJoin") && Tools.isNotEmpty(whereSql) && whereSql.contains("p.perex_group_id");

			sql.append("u.title as u_title, u.first_name, u.last_name, u.email, u.photo, ")
			.append(docFields);

			String[] additionalFields = DataAccessHelper.getDocFields();
			if (additionalFields!=null && additionalFields.length>0) {
				for (String field : additionalFields) {
					sql.append(", d.").append(field);
				}
			}

			if (useAtributeSearch)
			{
				sql.append(", dad.*, da.value_string, da.value_int, da.value_bool ");
			}
		}
		sql.append(" FROM documents d ");

		sql.append("LEFT JOIN users u ON d.author_id=u.user_id ");
		if (Constants.getBoolean("perexGroupUseJoin"))
		{
			sql.append("LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id ");
		}

		if (useAtributeSearch)
		{
			sql.append("LEFT JOIN doc_atr da ON da.doc_id = d.doc_id LEFT JOIN doc_atr_def dad ON dad.atr_id = da.atr_id ");
		}


		boolean addAnd = false;
		if (databaseCriteriaList.size()>0)
		{
			for (DatabaseCriteria criteria : databaseCriteriaList)
			{
				if (!addAnd)
				{
					sql.append("WHERE ");
				}
				else
				{
					sql.append("AND ");
				}
				appendCriteria(criteria, sql);
				addAnd = true;
			}
		}

		if (!count) {
			setOrdering(sql, "d", false);
		}

		// strankovanie
		if (!count && limit!=NOT_SET_INT && Tools.isEmpty(resultCriteriaList))
		{
			// nastavim limity a strankovanie, ale len v pripade ze sa nejedna po select pre pocet dokumentov (tam sa strankovanie nepouzije)
			if (Constants.DB_TYPE==Constants.DB_MYSQL)
			{
				sql.append("LIMIT ");
				//if (offset!=NOT_SET_INT && initialOffset>0)
				{
					sql.append(" ").append((offset>0?offset:0)+initialOffset).append(", ");
				}
				sql.append(limit);
			}
			else if (Constants.DB_TYPE==Constants.DB_ORACLE)
			{
				StringBuilder oraclePaging = new StringBuilder();
				if (offset==NOT_SET_INT && initialOffset==0)
				{
					oraclePaging.append("SELECT * FROM (");
					oraclePaging.append(sql);
					oraclePaging.append(") WHERE ROWNUM <= ").append(limit);
				}
				else
				{
					oraclePaging.append("SELECT * FROM ( SELECT /*+ FIRST_ROWS(").append((offset>0?offset:0) + initialOffset + limit).append(") */ a.*, ROWNUM RNUM FROM (");
					oraclePaging.append(sql);
					oraclePaging.append(") a WHERE ROWNUM <= ").append(limit+initialOffset+(offset>0?offset:0)).append(") WHERE RNUM > ").append((offset>0?offset:0)+initialOffset);
				}
				sql = oraclePaging;
			}
			else if (Constants.DB_TYPE==Constants.DB_MSSQL)
			{
			    if (Constants.getBoolean("mssqlUseOldTopQuery"))
                {
                    //toto je potrebne pre staru verziu MSSQL
                    StringBuilder sqlPaging = new StringBuilder();
                    if (offset==NOT_SET_INT && initialOffset==0)
                    {
                        sqlPaging.append("SELECT TOP ").append(limit).append(sql.substring(6));
                    }
                    else
                    {
                        sqlPaging.append("SELECT * FROM (SELECT TOP ").append(limit).append(" * FROM ( SELECT TOP ").append(limit + (offset>0?offset:0) + initialOffset);
                        sqlPaging.append(sql.substring(6));
                        sqlPaging.append(") AS _subQuery1 ");
                        // pridam reverzny ordering
                        setOrdering(sqlPaging, "_subQuery1", true);
                        // znova zoradim do povodneho radenia
                        sqlPaging.append(") AS _subQuery2 ");
                        setOrdering(sqlPaging, "_subQuery2", false);
                    }
                    sql = sqlPaging;
                }
			    else
                {
                    /* LPA - uprava limit a offset pre MS SQL */
                    sql.append("OFFSET ").append((offset > 0 ? offset : 0) + initialOffset).append(" ROWS ")
                            .append("FETCH NEXT ").append(limit).append(" ROWS ONLY");
                }

				/*
				takto nejak:
					Select * from (
						select top /LIMIT/ * from (
								select top /LIMIT+OFFSET/ d.doc_id,d.title from documents  d order by d.title asc
						) as res1 order by res1.title desc
					) as res2 order by res2.title asc

				 */
			}
		}

		return sql.toString();
	}





	/**
	 * Prida jednotlive kriteria do stringBuildera
	 * @param c
	 * @param sql
	 * @return
	 */
	private StringBuilder appendCriteria(DatabaseCriteria c, StringBuilder sql)
	{
		if (c.getType()==CriteriaType.NOT)
		{
			//je to not
			DatabaseCriteria criteria = (DatabaseCriteria)c.getValue();
			sql.append("NOT ( ");

			appendCriteria(criteria, sql);

			sql.append(") ");
		}
		else if (c.getType()==CriteriaType.OR)
		{
			//je to or
			List<DatabaseCriteria> orCriteriaList = (List<DatabaseCriteria>)c.getValue();
			sql.append("( ");
			boolean isNextOr = false;
			for (DatabaseCriteria orCriteria : orCriteriaList)
			{
				if (isNextOr)
				{
					sql.append("OR ");
				}
				isNextOr = true;
				appendCriteria(orCriteria, sql);
			}
			sql.append(") ");
		}
		else if (c.getType()==CriteriaType.AND)
		{
			//je to and
			List<DatabaseCriteria> andCriteriaList = (List<DatabaseCriteria>)c.getValue();
			sql.append("( ");
			boolean isNextAnd = false;
			for (DatabaseCriteria andCriteria : andCriteriaList)
			{
				if (isNextAnd)
				{
					sql.append("AND ");
				}
				isNextAnd = true;
				appendCriteria(andCriteria, sql);
			}
			sql.append(") ");
		}
		else if (c.isAtributeCriteria())
		{
			// ak mam zadany nazov atributu
			if (Tools.isNotEmpty(c.getAtributeName()))
			{
				sql.append("(dad.atr_name=? AND ");
				arguments.add(c.getAtributeName());
			}
			else
			{
				sql.append("(dad.atr_id=?  AND ");
				arguments.add(c.getAtributeId());
			}
			switch (c.getAtributeType())
			{
				case DatabaseCriteria.ATRIBUTE_TYPE_STRING: sql.append("dad.atr_type=0 AND da.value_string"); break;
				case DatabaseCriteria.ATRIBUTE_TYPE_INTEGER: sql.append("dad.atr_type=1 AND da.value_int"); break;
				case DatabaseCriteria.ATRIBUTE_TYPE_BOOLEAN: sql.append("dad.atr_type=2 AND da.value_bool"); break;
			}

			if (c.getType()==CriteriaType.EQUALS)
			{
				sql.append(" = ?)");
				arguments.add(c.getValue());
			}
			else if (c.getType()==CriteriaType.GREATER_THAN)
			{
				sql.append(" > ?)");
				arguments.add(c.getValue());
			}
			else if (c.getType()==CriteriaType.LESS_THAN)
			{
				sql.append(" < ?)");
				arguments.add(c.getValue());
			}
			else if (c.getType()==CriteriaType.LIKE)
			{
				sql.append(" LIKE ?)");
				String value = (c.isBeforeWildcard()?"%":"") + String.valueOf(c.getValue()) + (c.isAfterWildcard()?"%":"");
				arguments.add(value);
			}
			else if (c.getType()==CriteriaType.NOT_LIKE)
			{
				sql.append(" NOT LIKE ?)");
				String value = (c.isBeforeWildcard()?"%":"") + String.valueOf(c.getValue()) + (c.isAfterWildcard()?"%":"");
				arguments.add(value);
			}
		}
		else if (c.getType()==CriteriaType.IN)
		{
			List<Object> objectList = (List<Object>)c.getValue();
			sql.append(c.getProperty().getDbName()).append(" IN ( ");
			boolean addComma = false;
			for (Object obj : objectList)
			{
				if (addComma)
				{
					sql.append(", ");
				}
				addComma = true;
				//appendCriteria(andCriteria, sql);
				sql.append("?");
				arguments.add(obj);
			}
			sql.append(") ");
		}
		else if (c.getType()==CriteriaType.NOT_IN)
		{
			List<Object> objectList = (List<Object>)c.getValue();
			sql.append(c.getProperty().getDbName()).append(" NOT IN ( ");
			boolean addComma = false;
			for (Object obj : objectList)
			{
				if (addComma)
				{
					sql.append(", ");
				}
				addComma = true;
				sql.append("?");
				arguments.add(obj);
			}
			sql.append(") ");
		}
		else if (c.getType()==CriteriaType.LIKE)
		{
			String arg = (String)c.getValue();
			sql.append(c.getProperty().getDbName()).append(" LIKE ? ");

			if (c.isBeforeWildcard())
				arg = "%"+arg;

			if (c.isAfterWildcard())
				arg = arg+"%";

			arguments.add(arg);
		}
		else if (c.getType()==CriteriaType.NOT_EQUALS && Constants.DB_TYPE==Constants.DB_ORACLE && Tools.isEmpty(String.valueOf(c.getValue())))
		{
			//do nothing - Oracle s tym malo problem
			sql.delete(sql.length()-4, sql.length());
		}
		else if (c.getType() == CriteriaType.DATALENGTH_GREATER || c.getType() == CriteriaType.DATALENGTH_EQUAL) {
			sql.append(" datalength(").append(c.getProperty().getDbName()).append(") ").append(c.getType().getSymbol()).append(" ?");
			arguments.add(c.getValue());
		}
		else if (c.getType() == CriteriaType.LENGTH_GREATER || c.getType() == CriteriaType.LENGTH_EQUAL) {
			sql.append(" length(").append(c.getProperty().getDbName()).append(") ").append(c.getType().getSymbol()).append(" ?");
			arguments.add(c.getValue());
		}
		else
		{
			sql.append(c.getProperty().getDbName()).append(" ").append(c.getType().getSymbol()).append(" ");
			if (c.getValue()!=null)
			{
				arguments.add(c.getValue());
				sql.append("? ");
			}
		}

		return sql;
	}

	/**
	 * Prida zoradenie
	 * @param sql
	 * @param prefix - Prefix tabulky, standardne je to 'd', pri vnorenych selectoch sa to tam nasupuje same
	 * @param reverse - true ak ma vracat opacne sort - pri pagingu pre MSSQL
	 */
	private void setOrdering(StringBuilder sql, String prefix, boolean reverse)
	{
		sql.append("ORDER BY ");
		if (!orderings.isEmpty())
		{
			boolean addComma = false;
			for (Pair<OrderEnum, SortEnum> ordering : orderings)
			{
				if (addComma) sql.append(", ");
				sql.append(ordering.first.getOrderName(prefix))
				.append(/* ak je true, zoradi to cele naopak */reverse?ordering.second.getReverse():ordering.second);
				addComma = true;
			}
		}
		else
		{
			sql.append(OrderEnum.TITLE.getOrderName(prefix))
			.append(/* ak je true, zoradi to cele naopak */reverse?SortEnum.ASC.getReverse():SortEnum.ASC);
		}
	}

	/**
	 * nasetuje parametre do PS
	 * @param ps
	 * @throws SQLException
	 */
	private void setArguments(PreparedStatement ps) throws SQLException
	{
		StringBuilder sb = new StringBuilder();
		if (!arguments.isEmpty())
		{
			int index=1;

			for (Object argument : arguments)
			{
				sb.append(index).append(":").append(argument).append(" ");

				if (argument instanceof Integer)
				{
					Integer val = (Integer)argument;
					ps.setInt(index++, val);
				}
				else if (argument instanceof Double)
				{
					Double val = (Double)argument;
					ps.setDouble(index++, val);
				}
				else if (argument instanceof Long)
				{
					Long val = (Long)argument;
					ps.setLong(index++, val);
				}
				else if (argument instanceof Boolean)
				{
					Boolean val = (Boolean)argument;
					ps.setBoolean(index++, val);
				}
				else if (argument instanceof Timestamp)
				{
					Timestamp val = (Timestamp)argument;
					ps.setTimestamp(index++, val);
				}
				else if (argument instanceof Date)
				{
					Date dateVal = (Date)argument;
					Timestamp val = new Timestamp(dateVal.getTime());
					ps.setTimestamp(index++, val);
				}
				else if (argument instanceof String)
				{
					String val = (String)argument;
					ps.setString(index++, val);
				}
				else if (argument instanceof PerexGroupBean)
				{
					PerexGroupBean val = (PerexGroupBean)argument;
					ps.setInt(index++, val.getPerexGroupId());
				}
				else if (argument instanceof DocDetails)
				{
					DocDetails val = (DocDetails)argument;
					ps.setInt(index++, val.getDocId());
				}
				else if (argument instanceof UserDetails)
				{
					UserDetails val = (UserDetails)argument;
					ps.setInt(index++, val.getUserId());
				}
				else if (argument instanceof GroupDetails)
				{
					GroupDetails val = (GroupDetails)argument;
					ps.setInt(index++, val.getGroupId());
				}
				else
				{
					ps.setObject(index++, argument);
				}
			}
		}

		Logger.debug(NewsQuery.class, "PARAMS: "+sb.toString());

		this.arguments = new LinkedList<Object>();
	}

	/**
	 * Vrati zoznam stranok s aplikovanymi DBCriteriami, aj s ResultCriteriami
	 *
	 * @return zoznam stranok
	 */
	public List<DocDetails> getNewsList()
	{
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		List<DocDetails> result = new LinkedList<DocDetails>();
		try
		{
			db_conn = DBPool.getConnectionReadUncommited();
			String sql = getSelectSql(false, !loadData);
			Logger.debug(this,"sql: "+sql);
			ps = db_conn.prepareStatement(sql);
			setArguments(ps);
			rs = ps.executeQuery();
			DocDetails doc;
			//if (returnDocdetailsWithAtributes) //= new DocDetails();

			while (rs.next())
			{
				// ak sa maju vracat stranky aj s atributmi, musim vytvorit instanciu AtrDocBean
				if (returnDocdetailsWithAtributes) doc = new AtrDocBean();
				else doc = new DocDetails();

				if (Constants.getBoolean("docAuthorLazyLoad"))
				{
					DocDB.getDocDetails(rs, doc, !loadData, true);
				}
				else
				{
					//doc = DocDB.getDocDetails(rs, !loadData);
					DocDB.getDocDetails(rs, doc, !loadData, false);
				}
				//pozrem ci su nejake resultCriteria a ak ano, aplikujem ich
				if (resultCriteriaList.size()>0)
				{

					for (ResultCriteria criteria : resultCriteriaList)
					{
						//ak vyhovuje filtru, pokracuj
						if (criteria.filter(doc))
						{
							resultCriteriaTotalCount++;

							// ak je nastaveny limit aj offset a pocitadlo je viac ako offset a zaroven vo vysledku je menej alebo rovno zaznamov ako ma byt, pridaj
							// ak je nastaveny len limit (ide o prvu stranku zoznamu), a este ichg tam neni tolko, pridaj to tam
							// ak nieje vobec strankovanie, tak ho tam pridaj
							if ((limit>0 && offset+initialOffset>0 && resultCriteriaTotalCount>offset+initialOffset && result.size()<=limit) ||
									(limit>0 && offset+initialOffset<=0 && result.size()<limit) ||
									(limit<=0 && offset+initialOffset<=0))
							{
								result.add(doc);
							}

						}
						else
						{
							continue;
						}
					}
					//resultCriteriaTotalCount = counter;
				}
				else
				{
					result.add(doc);
				}
			}
			rs.close();
			ps.close();
			rs = null;
			ps = null;

			if (returnDocdetailsWithAtributes && result!=null)
			{
				//List<Integer> ids = new LinkedList<Integer>();
				Map<Integer, AtrDocBean> docMap = new HashMap<Integer, AtrDocBean>();
				for (DocDetails d : result)
				{
					docMap.put(d.getDocId(), (AtrDocBean)d);
				}
				String atrSql = "SELECT daf.*, da.doc_id, da.value_string, da.value_int, da.value_bool FROM doc_atr da LEFT JOIN doc_atr_def daf ON da.atr_id=daf.atr_id WHERE da.doc_id IN ("+StringUtils.join(docMap.keySet().iterator(),",")+") ";
				ps = db_conn.prepareStatement(atrSql);
				rs = ps.executeQuery();
				AtrBean atr = null;
				while (rs.next())
				{
					atr = new AtrBean();
					atr.setAtrId(rs.getInt("atr_id"));
					atr.setAtrName(DB.getDbString(rs, "atr_name"));
					atr.setOrderPriority(rs.getInt("order_priority"));
					atr.setAtrDescription(DB.getDbString(rs, "atr_description"));
					atr.setAtrDefaultValue(DB.getDbString(rs, "atr_default_value"));
					atr.setAtrType(rs.getInt("atr_type"));
					atr.setAtrGroup(DB.getDbString(rs, "atr_group"));
					atr.setDocId(rs.getInt("doc_id"));
					atr.setValueString(DB.getDbString(rs, "value_string"));
					atr.setValueNumber(rs.getDouble("value_int"));
					atr.setValueBool(rs.getBoolean("value_bool"));
					atr.setTrueValue(DB.getDbString(rs, "true_value"));
					atr.setFalseValue(DB.getDbString(rs, "false_value"));

					if (docMap.containsKey(atr.getDocId()))
					{
						docMap.get(atr.getDocId()).addAtr(atr);
					}

				}
				rs.close();
				ps.close();
				rs = null;
				ps = null;
				result = new LinkedList<DocDetails>(docMap.values());
			}

			db_conn.close();
			db_conn = null;
		}
		catch (Exception e)
		{
			Logger.debug(getClass(), "Nastala chyba:"+e.getMessage());
			sk.iway.iwcm.Logger.error(e);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		return result;
	}

	/**
	 * Vrati jeden dokument
	 * @param requireSingleResult ak je true, pozaduje z DB prave jeden vysledok
	 * @return
	 */
	public DocDetails getSingleDoc(boolean requireSingleResult)
	{
		List<DocDetails> results = getNewsList();
		if (results!=null && results.size()>0)
		{
			if (requireSingleResult && results.size()>1) throw new IllegalStateException("Multiple results.");
			return results.get(0);
		}
		return null;
	}

	public List<AtrDocBean> getNewsListWithAtributes()
	{
		this.returnDocdetailsWithAtributes = true;
		List<AtrDocBean> result = new LinkedList<AtrDocBean>();
		for (DocDetails d : getNewsList())
		{
			AtrDocBean a = (AtrDocBean)d;
			result.add(a);
		}
		return result;
	}

	/**
	 * Vrati pocet stranok, bez zohladnenia strankovania, neaplikuje ResultCriteria
	 * (nieje mozne ich aplikovat, nemam dostupne objekty na ktore by sa aplikoval)
	 *
	 * @return pocet stranok ktore vyhovuju dotazu
	 * */
	public int getNewsCount()
	{
		if (!Tools.isEmpty(resultCriteriaList) && resultCriteriaTotalCount>0)
		{
			return resultCriteriaTotalCount;
		}
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		int result = 0;
		try
		{
			db_conn = DBPool.getConnectionReadUncommited();
			String sql = getSelectSql(true, loadData);
			Logger.debug(this,"sql: "+sql);

			ps = db_conn.prepareStatement(sql);

			setArguments(ps);
			rs = ps.executeQuery();
			if (rs.next())
			{
				result = rs.getInt(1);
			}
			rs.close();
			ps.close();
			db_conn.close();

			rs = null;
			ps = null;
			db_conn = null;


		}
		catch (Exception e)
		{
			Logger.debug(getClass(), "Nastala chyba:"+e.getMessage());
			sk.iway.iwcm.Logger.error(e);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
		}

		if (initialOffset>0)
		{
			result = result - initialOffset;
		}
		return result;
	}

	/**
	 * Prida Criteria pre Query, mozu to byt DatabaseCriteria aj ResultCriteria
	 * @param criteria
	 * @return
	 */
	public NewsQuery addCriteria(Criteria criteria)
	{
		if (criteria instanceof DatabaseCriteria)
		{
			DatabaseCriteria c = (DatabaseCriteria)criteria;
			if (c.isAtributeCriteria()) this.useAtributeSearch = true;
			this.databaseCriteriaList.add(c);
		}
		else if (criteria instanceof ResultCriteria)
		{
			ResultCriteria c = (ResultCriteria)criteria;
			this.resultCriteriaList.add(c);
		}
		return this;
	}

	/**
	 * Nastavi limit poctu vratenych dokumentov
	 *
	 * @param limit
	 * @return
	 */
	public NewsQuery setLimit(int limit)
	{
		this.limit = limit;
		return this;
	}

	/**
	 * Nastavi offset vysledkov -> pre druhu stranku vysledkov pri velkosti stranky 10, sa nastavi offset 10, limit 10
	 * @param offset
	 * @return
	 */
	public NewsQuery setOffset(int offset)
	{
		this.offset = offset;
		return this;
	}

	/**
	 * ak sa nastavi true, vrati aj docData, default ho nevracia
	 * @param loadData
	 * @return
	 */
	public NewsQuery setLoadData(boolean loadData)
	{
		this.loadData = loadData;
		return this;
	}

	/**
	 * polia podla ktorych je mozne radit vysledky
	 * @author mbocko
	 *
	 */
	public static enum OrderEnum
	{
		TITLE ("title "),
		DATE ("publish_start "),
		ID ("doc_id "),
		PRIORITY ("sort_priority "),
		PLACE ("perex_place "),
		EVENT_DATE ("event_date "),
		SAVE_DATE ("date_created "),
		RATING ("forum_count ");


		private OrderEnum(String orderField)
		{
			this.orderField = orderField;
		}

		private String orderField;

		@Override
		public String toString()
		{
			return getOrderName("d");
		}

		public String getOrderName(String prefix)
		{
			return prefix + "." + orderField;
		}
	}

	/**
	 * smer zoradenia vysledkov
	 * @author mbocko
	 *
	 */
	public static enum SortEnum
	{
		ASC ("ASC "),
		DESC ("DESC ");

		private SortEnum(String sort)
		{
			this.sort = sort;
		}

		private String sort;

		@Override
		public String toString()
		{
			return sort;
		}

		/**
		 * vrati reverzny sort (pri strankovani MSSQL)
		 * @return
		 */
		public SortEnum getReverse()
		{
			if (this==ASC) return DESC;
			else return ASC;
		}
	}

	/**
	 * true ak sa priradenie perex skupiny k dokumentu deje cez spojovaciu tabulku
	 *
	 * @param perexGroupUseJoin
	 * @return
	 */
	public NewsQuery setPerexGroupUseJoin(boolean perexGroupUseJoin)
	{
		this.perexGroupUseJoin = perexGroupUseJoin;
		return this;
	}

	/**
	 * prida stlpec a radenie vysledkov, je mozne pridat viac krat
	 * @param order
	 * @param sort
	 * @return
	 */
	public NewsQuery addOrder(OrderEnum order, SortEnum sort)
	{
		orderings.add(Pair.of(order, sort));
		return this;
	}





	public NewsQuery setPageSize(int pageSize)
	{
		limit = pageSize;
		return this;
	}


	public void setInitialOffset(int initialOffset)
	{
		this.initialOffset = initialOffset;
	}

	public NewsQuery setReturnDocdetailsWithAtributes(boolean returnDocdetailsWithAtributes)
	{
		this.returnDocdetailsWithAtributes = returnDocdetailsWithAtributes;
		return this;
	}





	public NewsQuery setPage(int page)
	{
		if (page<=0) page = 1;
		if (limit>NOT_SET_INT)
		{
			offset = page * limit - limit;
		}
		return this;
	}
}
