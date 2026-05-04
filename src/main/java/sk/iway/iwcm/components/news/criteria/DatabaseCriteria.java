package sk.iway.iwcm.components.news.criteria;

import java.util.LinkedList;
import java.util.List;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.news.FieldEnum;

/**
 * Reprezentuje Kriteria nad databazou
 * @author mbocko
 *
 */
public class DatabaseCriteria implements Criteria
{

	public static final int ATRIBUTE_TYPE_STRING = 0;
	public static final int ATRIBUTE_TYPE_INTEGER = 1;
	public static final int ATRIBUTE_TYPE_BOOLEAN = 2;

	private CriteriaType type;
	private FieldEnum property;
	private String atributeName;
	private int atributeId;
	private int atributeType = ATRIBUTE_TYPE_STRING;
	private Object value;
	private boolean beforeWildcard = false;
	private boolean afterWildcard = false;
	private boolean atributeCriteria = false;

	public DatabaseCriteria(CriteriaType type, FieldEnum property, Object value, boolean atributeCriteria)
	{
		this.type = type;
		this.property = property;
		this.value = value;
		this.atributeCriteria = atributeCriteria;
	}
	private DatabaseCriteria(CriteriaType type, String atributeName, Object value, int atributeType, boolean beforeWildcard, boolean afterWildcard)
	{
		this.type = type;
		this.atributeName = atributeName;
		this.value = value;
		this.atributeCriteria = true;
		this.atributeType = atributeType;
	}
	private DatabaseCriteria(CriteriaType type, int atributeId, Object value, int atributeType, boolean beforeWildcard, boolean afterWildcard)
	{
		this.type = type;
		this.atributeId = atributeId;
		this.value = value;
		this.atributeCriteria = true;
		this.atributeType = atributeType;
	}

	private DatabaseCriteria(CriteriaType type, FieldEnum property, Object value, boolean beforeWildcard, boolean afterWildcard)
	{
		this.type = type;
		this.property = property;
		this.value = value;
		this.beforeWildcard = beforeWildcard;
		this.afterWildcard = afterWildcard;
	}




	public static DatabaseCriteria equal(FieldEnum property, Object value)
	{
		return new DatabaseCriteria(CriteriaType.EQUALS, property, value, false);
	}

	public static DatabaseCriteria notEqual(FieldEnum property, Object value)
	{
		return new DatabaseCriteria(CriteriaType.NOT_EQUALS, property, value, false);
	}

	public static DatabaseCriteria notEmptyText(FieldEnum property) {
		if (Constants.DB_TYPE == Constants.DB_ORACLE) {
			return new DatabaseCriteria(CriteriaType.LENGTH_GREATER, property, 0, false);
		}

		if (Constants.DB_TYPE == Constants.DB_MSSQL) {
			return new DatabaseCriteria(CriteriaType.DATALENGTH_GREATER, property, 0, false);
		}

		return notEqual(property, "");
	}

	public static DatabaseCriteria less(FieldEnum property, Object value)
	{
		return new DatabaseCriteria(CriteriaType.LESS_THAN, property, value, false);
	}
	public static DatabaseCriteria lessEqual(FieldEnum property, Object value)
	{
		return new DatabaseCriteria(CriteriaType.LESS_THAN_EQUAL, property, value, false);
	}
	
	public static DatabaseCriteria greater(FieldEnum property, Object value)
	{
		return new DatabaseCriteria(CriteriaType.GREATER_THAN, property, value, false);
	}
	public static DatabaseCriteria greaterEqual(FieldEnum property, Object value)
	{
		return new DatabaseCriteria(CriteriaType.GREATER_THAN_EQUAL, property, value, false);
	}

	public static DatabaseCriteria isNull(FieldEnum property)
	{
		return new DatabaseCriteria(CriteriaType.IS_NULL, property, null, false);
	}

	public static DatabaseCriteria isNotNull(FieldEnum property)
	{
		return new DatabaseCriteria(CriteriaType.IS_NOT_NULL, property, null, false);
	}

	public static DatabaseCriteria like(FieldEnum property, String value)
	{
		return new DatabaseCriteria(CriteriaType.LIKE, property, value, false);
	}

	public static DatabaseCriteria endsWith(FieldEnum property, String value)
	{
		return new DatabaseCriteria(CriteriaType.LIKE, property, value, true, false);
	}

	public static DatabaseCriteria startsWith(FieldEnum property, String value)
	{
		return new DatabaseCriteria(CriteriaType.LIKE, property, value, false, true);
	}

	public static DatabaseCriteria contains(FieldEnum property, String value)
	{
		return new DatabaseCriteria(CriteriaType.LIKE, property, value, true, true);
	}
	
	public static DatabaseCriteria notContains(FieldEnum property, String value)
	{
		return new DatabaseCriteria(CriteriaType.NOT_LIKE, property, value, true, true);
	}

	public static DatabaseCriteria in(FieldEnum property, Object...items)
	{
		List<Object> itemList = new LinkedList<Object>();
		if (items!=null)
		{
			for (Object o : items)
			{
				if (o instanceof Iterable)
				{
					@SuppressWarnings("rawtypes")
					Iterable iterableObject = (Iterable)o;
					for (Object oi : iterableObject)
					{
						itemList.add(oi);
					}
				}
				else
				{
					itemList.add(o);
				}
			}
		}
		return new DatabaseCriteria(CriteriaType.IN, property, itemList, false);
	}

	public static DatabaseCriteria notIn(FieldEnum property, Object...items)
	{
		List<Object> itemList = new LinkedList<Object>();
		if (items!=null)
		{
			for (Object o : items)
			{
				if (o instanceof Iterable)
				{
					@SuppressWarnings("rawtypes")
					Iterable iterableObject = (Iterable)o;
					for (Object oi : iterableObject)
					{
						itemList.add(oi);
					}
				}
				else
				{
					itemList.add(o);
				}
			}
		}
		return new DatabaseCriteria(CriteriaType.NOT_IN, property, itemList, false);
	}

	public static DatabaseCriteria not(DatabaseCriteria criteria)
	{
		return new DatabaseCriteria(CriteriaType.NOT, (FieldEnum)null, criteria, criteria.isAtributeCriteria());
	}

	public static DatabaseCriteria isEmpty(FieldEnum property)
	{
		return or(isNull(property), equal(property, ""));
	}

	public static DatabaseCriteria isNotEmpty(FieldEnum property)
	{
		return and(isNotNull(property), notEqual(property, ""));
	}

	public static DatabaseCriteria isNotEmptyText(FieldEnum property)
	{
		return and(isNotNull(property), notEmptyText(property));
	}


	/**
	 * Kriteria ktore sa tykaju atributov. 
	 * 
	 * Atribut je mozne definovat bud podla nazvu alebo podla ID
	 */
	public static class Atribute
	{
		public static DatabaseCriteria equal(String atributeName, String value)
		{
			return new DatabaseCriteria(CriteriaType.EQUALS, atributeName, value, ATRIBUTE_TYPE_STRING, false, false);
		}
		public static DatabaseCriteria equal(String atributeName, int value)
		{
			return new DatabaseCriteria(CriteriaType.EQUALS, atributeName, value, ATRIBUTE_TYPE_INTEGER, false, false);
		}
		public static DatabaseCriteria equal(String atributeName, boolean value)
		{
			return new DatabaseCriteria(CriteriaType.EQUALS, atributeName, value, ATRIBUTE_TYPE_BOOLEAN, false, false);
		}
		public static DatabaseCriteria equal(int atributeId, String value)
		{
			return new DatabaseCriteria(CriteriaType.EQUALS, atributeId, value, ATRIBUTE_TYPE_STRING, false, false);
		}
		public static DatabaseCriteria equal(int atributeId, int value)
		{
			return new DatabaseCriteria(CriteriaType.EQUALS, atributeId, value, ATRIBUTE_TYPE_INTEGER, false, false);
		}
		public static DatabaseCriteria equal(int atributeId, boolean value)
		{
			return new DatabaseCriteria(CriteriaType.EQUALS, atributeId, value, ATRIBUTE_TYPE_BOOLEAN, false, false);
		}
		public static DatabaseCriteria greater(int atributeId, int value)
		{
			return new DatabaseCriteria(CriteriaType.GREATER_THAN, atributeId, value, ATRIBUTE_TYPE_INTEGER, false, false);
		}
		public static DatabaseCriteria greater(String atributeName, int value)
		{
			return new DatabaseCriteria(CriteriaType.GREATER_THAN, atributeName, value, ATRIBUTE_TYPE_INTEGER, false, false);
		}
		public static DatabaseCriteria less(int atributeId, int value)
		{
			return new DatabaseCriteria(CriteriaType.LESS_THAN, atributeId, value, ATRIBUTE_TYPE_INTEGER, false, false);
		}
		public static DatabaseCriteria less(String atributeName, int value)
		{
			return new DatabaseCriteria(CriteriaType.LESS_THAN, atributeName, value, ATRIBUTE_TYPE_INTEGER, false, false);
		}
		public static DatabaseCriteria like(String atributeName, String value)
		{
			return new DatabaseCriteria(CriteriaType.LIKE, atributeName, value, ATRIBUTE_TYPE_STRING, false, false);
		}
		public static DatabaseCriteria like(int atributeId, String value)
		{
			return new DatabaseCriteria(CriteriaType.LIKE, atributeId, value, ATRIBUTE_TYPE_STRING, false, false);
		}
		public static DatabaseCriteria startsWith(String atributeName, String value)
		{
			return new DatabaseCriteria(CriteriaType.LIKE, atributeName, value, ATRIBUTE_TYPE_STRING, false, true);
		}
		public static DatabaseCriteria startsWith(int atributeId, String value)
		{
			return new DatabaseCriteria(CriteriaType.LIKE, atributeId, value, ATRIBUTE_TYPE_STRING, false, true);
		}
		public static DatabaseCriteria endsWith(String atributeName, String value)
		{
			return new DatabaseCriteria(CriteriaType.LIKE, atributeName, value, ATRIBUTE_TYPE_STRING, true, false);
		}
		public static DatabaseCriteria endsWith(int atributeId, String value)
		{
			return new DatabaseCriteria(CriteriaType.LIKE, atributeId, value, ATRIBUTE_TYPE_STRING, true, false);
		}
		public static DatabaseCriteria contains(String atributeName, String value)
		{
			return new DatabaseCriteria(CriteriaType.LIKE, atributeName, value, ATRIBUTE_TYPE_STRING, true, true);
		}
		public static DatabaseCriteria contains(int atributeId, String value)
		{
			return new DatabaseCriteria(CriteriaType.LIKE, atributeId, value, ATRIBUTE_TYPE_STRING, true, true);
		}

		//dorobit IN, NOT IN
	}



	public static DatabaseCriteria or(DatabaseCriteria...orCriterias)
	{
		List<DatabaseCriteria> orCriteriaList = new LinkedList<DatabaseCriteria>();
		boolean containsAtributeCriteria = false;
		if (orCriterias !=null && orCriterias.length>0)
		{
			for (DatabaseCriteria c : orCriterias)
			{
				if (c.atributeCriteria) containsAtributeCriteria = true;
				orCriteriaList.add(c);
			}
		}
		return new DatabaseCriteria(CriteriaType.OR, (FieldEnum)null, orCriteriaList, containsAtributeCriteria);
	}

	public static DatabaseCriteria and(DatabaseCriteria...andCriterias)
	{
		List<DatabaseCriteria> andCriteriaList = new LinkedList<DatabaseCriteria>();
		boolean containsAtributeCriteria = false;
		if (andCriterias !=null && andCriterias.length>0)
		{
			for (DatabaseCriteria c : andCriterias)
			{
				if (c.atributeCriteria) containsAtributeCriteria = true;
				andCriteriaList.add(c);
			}
		}
		return new DatabaseCriteria(CriteriaType.AND, (FieldEnum)null, andCriteriaList, containsAtributeCriteria);
	}

	/**
	 * 
	 * @param property
	 * @param value
	 * @return
	 */
	public static DatabaseCriteria tokenizedIds(FieldEnum property, int value)
	{
		return or(
				equal(property, String.valueOf(value)),
				startsWith(property, String.valueOf(value)+","),
				endsWith(property, ","+String.valueOf(value)),
				contains(property, ","+String.valueOf(value)+",")
				); 
	}

	public enum CriteriaType
	{

		EQUALS("="), 
		NOT_EQUALS("<>"),
		LESS_THAN("<"), 
		GREATER_THAN(">"),
		IS_NULL("IS NULL"),
		IS_NOT_NULL("IS NOT NULL"),
		LIKE("LIKE"),
		NOT_LIKE("NOT LIKE"),
		IN(""),
		NOT_IN(""),
		AND(""),
		OR(""),
		GREATER_THAN_EQUAL(">="),
		LESS_THAN_EQUAL("<="),
		NOT("NOT"),
		DATALENGTH_EQUAL("="),
		DATALENGTH_GREATER(">"),
		IS_EMPTY("IS EMPTY"),
		IS_NOT_EMPTY("IS NOT EMPTY"),
		LENGTH_EQUAL("="),
		LENGTH_GREATER(">");

		CriteriaType(String symbol)
		{
			this.symbol = symbol;
		}

		private String symbol;

		public String getSymbol()
		{
			return symbol;
		}

	}

	public CriteriaType getType()
	{
		return type;
	}

	public FieldEnum getProperty()
	{
		return property;
	}

	public Object getValue()
	{
		return value;
	}

	public boolean isBeforeWildcard()
	{
		return beforeWildcard;
	}

	public boolean isAfterWildcard()
	{
		return afterWildcard;
	}

	public boolean isAtributeCriteria()
	{
		return atributeCriteria;
	}

	public String getAtributeName()
	{
		return atributeName;
	}

	/**
	 * Umoznuje pridat dalsie kriteria ku kriteriam kde value je list kriterii (napr or alebo and)
	 * @param crit
	 * @return
	 */
	public DatabaseCriteria addCriteria(DatabaseCriteria crit)
	{
		if (this.getValue() instanceof List)
		{
			@SuppressWarnings("unchecked")
			List<DatabaseCriteria> critList = (List<DatabaseCriteria>)this.getValue();
			critList.add(crit);
		}
		return this;
	}


	@Override
	public String toString()
	{
		return this.stringValue(0);
	}

	public String stringValue(int intendation)
	{
		StringBuilder sb = new StringBuilder();
		if (this.atributeCriteria && Tools.isNotEmpty(atributeName))
		{
			sb.append("\n");
			for (int offset=0; offset<=intendation; offset++) sb.append("    ");
			sb.append("ATR{");
			if (Tools.isNotEmpty(atributeName))	sb.append(atributeName);
			else sb.append("ID(").append(atributeId).append(")");
			sb.append("} ");
			sb.append(this.type.name());
			if (value!=null)
			{
				sb.append(" [\"").append(String.valueOf(value)).append("\"]");
			}
		}
		else if (this.type == CriteriaType.AND)
		{
			sb.append("\n");
			for (int offset=0; offset<=intendation; offset++) sb.append("    ");
			sb.append("AND [");

			if (this.value !=null && this.value instanceof List) 
			{
				@SuppressWarnings("unchecked")
				List<DatabaseCriteria> critList = (List<DatabaseCriteria>)this.getValue();
				boolean addComma = false;
				for (DatabaseCriteria c : critList)
				{
					if (addComma) sb.append(",\n");
					addComma = true;
					sb.append(c.stringValue(intendation+1));
				}
			}

			sb.append("\n");
			for (int offset=0; offset<=intendation; offset++) sb.append("    ");
			sb.append("]");
		}
		else if (this.type == CriteriaType.OR)
		{
			sb.append("\n");
			for (int offset=0; offset<=intendation; offset++) sb.append("    ");
			sb.append("OR [");

			if (this.value !=null && this.value instanceof List) 
			{
				@SuppressWarnings("unchecked")
				List<DatabaseCriteria> critList = (List<DatabaseCriteria>)this.getValue();
				boolean addComma = false;
				for (DatabaseCriteria c : critList)
				{
					if (addComma) sb.append(", ");
					addComma = true;
					sb.append(c.stringValue(intendation+1));
				}
			}
			sb.append("\n");
			for (int offset=0; offset<=intendation; offset++) sb.append("    ");
			sb.append("]");
		}
		else
		{


			sb.append("\n");
			for (int offset=0; offset<=intendation; offset++) sb.append("    ");
			sb.append(this.property.name());
			sb.append(" ");
			sb.append(this.type.name());
			if (value!=null)
			{
				sb.append(" [\"").append(String.valueOf(value)).append("\"]");
			}
		}
		return sb.toString();
	}
	public int getAtributeId()
	{
		return atributeId;
	}
	public int getAtributeType()
	{
		return atributeType;
	}

}
