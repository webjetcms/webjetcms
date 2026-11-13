package sk.iway.iwcm.branch;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetProvider;

/** Adapts the explicit P02 projection to the existing core JDBC bean mappers. */
final class BranchRows {
    private BranchRows() { }
    static CachedRowSet result(Map<String, ?> values) throws SQLException {
        CachedRowSet row = RowSetProvider.newFactory().createCachedRowSet();
        RowSetMetaDataImpl metadata = new RowSetMetaDataImpl();
        metadata.setColumnCount(values.size());
        int index = 1;
        for (var entry : values.entrySet()) {
            metadata.setColumnName(index, entry.getKey());
            metadata.setColumnLabel(index, entry.getKey());
            Object value = entry.getValue();
            int type = value instanceof java.util.Date ? Types.TIMESTAMP : value instanceof Boolean ? Types.INTEGER
                : value instanceof Number ? Types.BIGINT : Types.VARCHAR;
            metadata.setColumnType(index++, type);
        }
        row.setMetaData(metadata);
        row.moveToInsertRow(); index = 1;
        for (Object value : values.values()) {
            if (value == null) row.updateNull(index++);
            else {
                Object jdbcValue=value instanceof Boolean bool ? (bool ? 1 : 0) : value;
                if(jdbcValue instanceof java.math.BigDecimal decimal && decimal.scale()<=0) jdbcValue=decimal.longValueExact();
                row.updateObject(index++,jdbcValue);
            }
        }
        row.insertRow(); row.moveToCurrentRow(); row.beforeFirst(); row.next();
        return row;
    }
}
