package sk.iway.iwcm.doc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class DocDBSqlIdentifierTest
{

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {
		"",
		"field_k DESC",
		"d.field_k",
		"CAST(field_k AS DECIMAL(10, 2))"
	})
	void invalidBasketPriceColumnUsesSafeDefault(String configuredFieldName)
	{
		assertEquals("field_k", DocDB.fixUntrustedColumnName(configuredFieldName, false));
	}

	@Test
	void distinctFieldHelperRejectsSqlSyntaxBeforeQueryExecution()
	{
		assertTrue(DocDB.getFieldDistinctValues("field_a DESC").isEmpty());
	}

	@Test
	void customFieldResolverAcceptsOnlySimpleFieldSuffix()
	{
		assertEquals("d.field_a", DocDB.resolveCustomFieldColumnName("a"));
		assertNull(DocDB.resolveCustomFieldColumnName("a OR 1=1"));
		assertNull(DocDB.resolveCustomFieldColumnName("d.a"));
		assertNull(DocDB.resolveCustomFieldColumnName(null));
	}
}
