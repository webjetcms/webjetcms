package sk.iway.iwcm.components.adresar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AdresarDBTest
{
	@Test
	void buildOrderSqlPreservesValidMultiColumnSort()
	{
		assertEquals(" ORDER BY last_name DESC, first_name ASC", AdresarDB.buildOrderSql("last_name-desc+first_name-asc"));
	}

	@Test
	void buildOrderSqlSkipsInvalidColumnAndKeepsValidColumns()
	{
		assertEquals(" ORDER BY last_name DESC, first_name ASC", AdresarDB.buildOrderSql("last_name-desc+email DESC+first_name-asc"));
	}

	@Test
	void buildOrderSqlUsesDefaultWhenAllColumnsAreInvalid()
	{
		assertEquals(" ORDER BY last_name,first_name", AdresarDB.buildOrderSql("email DESC+(SELECT password FROM users)"));
	}
}
