package sk.iway.iwcm.components.adresar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AdresarDBTest
{
	@Test
	void buildOrderSqlPreservesValidMultiColumnSort()
	{
		assertEquals(" ORDER BY users.last_name DESC, users.first_name ASC", AdresarDB.buildOrderSql("last_name-desc+first_name-asc"));
	}

	@Test
	void buildOrderSqlSkipsInvalidColumnAndKeepsValidColumns()
	{
		assertEquals(" ORDER BY users.last_name DESC, users.first_name ASC", AdresarDB.buildOrderSql("last_name-desc+email DESC+first_name-asc"));
	}

	@Test
	void buildOrderSqlUsesDefaultWhenAllColumnsAreInvalid()
	{
		assertEquals(" ORDER BY users.last_name,users.first_name", AdresarDB.buildOrderSql("email DESC+(SELECT password FROM users)"));
	}

	@Test
	void buildOrderSqlRejectsOraclePackageFunction()
	{
		assertEquals(" ORDER BY users.last_name ASC", AdresarDB.buildOrderSql("DBMS_RANDOM.VALUE-desc+last_name-asc"));
	}

	@Test
	void buildOrderSqlCanonicalizesTrustedTableQualifier()
	{
		assertEquals(" ORDER BY users.last_name DESC", AdresarDB.buildOrderSql("USERS.last_name-desc"));
	}
}
