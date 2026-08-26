package sk.iway.iwcm.doc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SearchActionTest
{
	@Test
	void validRequestParameterIsReturned()
	{
		SearchActionInput input = new SearchActionInput();
		input.setParameter("orderType", "d.title");

		assertEquals("d.title", SearchAction.getValidatedOrderType("orderType", input, "sort_priority", false));
	}

	@Test
	void invalidRequestParameterUsesSafeDefault()
	{
		SearchActionInput input = new SearchActionInput();
		input.setParameter("orderType", "title DESC");

		assertEquals("d.sort_priority", SearchAction.getValidatedOrderType("orderType", input, "sort_priority", false));
	}

	@ParameterizedTest
	@ValueSource(strings = {"title", "d.title"})
	void getValidatedOrderTypeAcceptsConfiguredAttribute(String orderType)
	{
		SearchActionInput input = new SearchActionInput();
		input.setAttribute("orderType", orderType);

		assertEquals("d.title", SearchAction.getValidatedOrderType("orderType", input, "sort_priority", false));
	}

	@Test
	void invalidRequestParameterUsesSafeDefaultInsteadOfConfiguredAttribute()
	{
		SearchActionInput input = new SearchActionInput();
		input.setAttribute("orderType", "title");
		input.setParameter("orderType", "(SELECT WJMARK2 FROM dual)");

		assertEquals("d.sort_priority", SearchAction.getValidatedOrderType("orderType", input, "sort_priority", false));
	}

	@Test
	void timeBasedInjectionInSecondaryOrderTypeIsSkipped()
	{
		SearchActionInput input = new SearchActionInput();
		String payload = "(SELECT CASE WHEN (SELECT SUBSTRING(password FROM 1 FOR 1) FROM users " +
			"WHERE login LIKE 0x61646d696e) LIKE 0x41 THEN SLEEP(3) ELSE 0 END)";
		input.setParameter("orderType2", payload);

		assertNull(SearchAction.getValidatedOrderType("orderType2", input, null, false));
	}

	@Test
	void missingAndEmptyOrderTypesUseSafeDefaults()
	{
		SearchActionInput missingInput = new SearchActionInput();
		assertEquals("d.sort_priority", SearchAction.getValidatedOrderType("orderType", missingInput, "sort_priority", false));
		assertNull(SearchAction.getValidatedOrderType("orderType2", missingInput, null, false));

		SearchActionInput emptyInput = new SearchActionInput();
		emptyInput.setParameter("orderType", "");
		emptyInput.setParameter("orderType2", "");
		assertEquals("d.sort_priority", SearchAction.getValidatedOrderType("orderType", emptyInput, "sort_priority", false));
		assertNull(SearchAction.getValidatedOrderType("orderType2", emptyInput, null, false));
	}

	@Test
	void arbitraryColumnIsBoundToDocumentsAlias()
	{
		SearchActionInput input = new SearchActionInput();
		input.setParameter("orderType", "WJINJECTMARKER99");

		assertEquals("d.WJINJECTMARKER99", SearchAction.getValidatedOrderType("orderType", input, "sort_priority", false));
	}

	@Test
	void oraclePackageFunctionIsRejected()
	{
		SearchActionInput input = new SearchActionInput();
		input.setParameter("orderType", "DBMS_RANDOM.VALUE");
		input.setParameter("orderType2", "DBMS_RANDOM.VALUE");

		assertEquals("d.sort_priority", SearchAction.getValidatedOrderType("orderType", input, "sort_priority", false));
		assertNull(SearchAction.getValidatedOrderType("orderType2", input, null, false));
	}

	@Test
	void legacyOrderNamesAreMappedBeforeAliasBinding()
	{
		assertEquals("d.date_created", SearchAction.resolveOrderType("lastUpdate", false));
		assertEquals("d.sort_priority", SearchAction.resolveOrderType("sortPriority", false));
		assertEquals("d.publish_start", SearchAction.resolveOrderType("publishStart", false));
		assertEquals("d.date_created", SearchAction.resolveOrderType("saveDate", false));
	}

	@Test
	void secondarySaveDateUsesDatabaseColumnName()
	{
		SearchActionInput input = new SearchActionInput();
		input.setParameter("orderType2", "saveDate");

		assertEquals("d.date_created",
			SearchAction.getValidatedOrderType("orderType2", input, null, false));
	}

	@ParameterizedTest
	@ValueSource(strings = {"orderType", "orderType2"})
	void oracleTextScoreRequiresMatchingContainsInCurrentQuery(String orderTypeName)
	{
		SearchActionInput input = new SearchActionInput();
		input.setParameter(orderTypeName, "sortPriority");

		assertEquals("d.sort_priority",
			SearchAction.getValidatedOrderType(orderTypeName, input, null, false));
		assertEquals("SCORE(10)",
			SearchAction.getValidatedOrderType(orderTypeName, input, null, true));
	}

	@Test
	void oracleTextScoreExpressionCannotBeSuppliedDirectly()
	{
		assertNull(SearchAction.resolveOrderType("SCORE(10)", true));
	}

	@Test
	void onlyDocumentsAliasIsAllowed()
	{
		assertNull(SearchAction.resolveOrderType("p.perex_group_id", false));
		assertNull(SearchAction.resolveOrderType("unknown.title", false));
	}
}
