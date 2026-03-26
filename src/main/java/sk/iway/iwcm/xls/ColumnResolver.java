package sk.iway.iwcm.xls;

import jxl.Cell;


public interface ColumnResolver
{
	public Object resolveColumn(Cell[] row, String columnName);
}
