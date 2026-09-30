package sk.iway.iwcm.doc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import sk.iway.iwcm.test.TestRequest;

class SearchActionTest
{
	@Test
	void validRequestParameterIsReturned()
	{
		TestRequest request = new TestRequest();
		request.setParameter("orderType", "d.title");

		assertEquals("d.title", SearchAction.getValidatedOrderType("orderType", request, "sort_priority", false));
	}

	@Test
	void invalidRequestParameterUsesSafeDefault()
	{
		TestRequest request = new TestRequest();
		request.setParameter("orderType", "title DESC");

		assertEquals("d.sort_priority", SearchAction.getValidatedOrderType("orderType", request, "sort_priority", false));
	}

	@ParameterizedTest
	@ValueSource(strings = {"title", "d.title"})
	void getValidatedOrderTypeAcceptsConfiguredAttribute(String orderType)
	{
		TestRequest request = new TestRequest();
		request.setAttribute("orderType", orderType);

		assertEquals("d.title", SearchAction.getValidatedOrderType("orderType", request, "sort_priority", false));
	}

	@Test
	void invalidRequestParameterUsesSafeDefaultInsteadOfConfiguredAttribute()
	{
		TestRequest request = new TestRequest();
		request.setAttribute("orderType", "title");
		request.setParameter("orderType", "(SELECT WJMARK2 FROM dual)");

		assertEquals("d.sort_priority", SearchAction.getValidatedOrderType("orderType", request, "sort_priority", false));
	}

	@Test
	void timeBasedInjectionInSecondaryOrderTypeIsSkipped()
	{
		TestRequest request = new TestRequest();
		String payload = "(SELECT CASE WHEN (SELECT SUBSTRING(password FROM 1 FOR 1) FROM users " +
			"WHERE login LIKE 0x61646d696e) LIKE 0x41 THEN SLEEP(3) ELSE 0 END)";
		request.setParameter("orderType2", payload);

		assertNull(SearchAction.getValidatedOrderType("orderType2", request, null, false));
	}

	@Test
	void missingAndEmptyOrderTypesUseSafeDefaults()
	{
		TestRequest missingRequest = new TestRequest();
		assertEquals("d.sort_priority", SearchAction.getValidatedOrderType("orderType", missingRequest, "sort_priority", false));
		assertNull(SearchAction.getValidatedOrderType("orderType2", missingRequest, null, false));

		TestRequest emptyRequest = new TestRequest();
		emptyRequest.setParameter("orderType", "");
		emptyRequest.setParameter("orderType2", "");
		assertEquals("d.sort_priority", SearchAction.getValidatedOrderType("orderType", emptyRequest, "sort_priority", false));
		assertNull(SearchAction.getValidatedOrderType("orderType2", emptyRequest, null, false));
	}

	@Test
	void arbitraryColumnIsBoundToDocumentsAlias()
	{
		TestRequest request = new TestRequest();
		request.setParameter("orderType", "WJINJECTMARKER99");

		assertEquals("d.WJINJECTMARKER99", SearchAction.getValidatedOrderType("orderType", request, "sort_priority", false));
	}

	@Test
	void oraclePackageFunctionIsRejected()
	{
		TestRequest request = new TestRequest();
		request.setParameter("orderType", "DBMS_RANDOM.VALUE");
		request.setParameter("orderType2", "DBMS_RANDOM.VALUE");

		assertEquals("d.sort_priority", SearchAction.getValidatedOrderType("orderType", request, "sort_priority", false));
		assertNull(SearchAction.getValidatedOrderType("orderType2", request, null, false));
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
		TestRequest request = new TestRequest();
		request.setParameter("orderType2", "saveDate");

		assertEquals("d.date_created",
			SearchAction.getValidatedOrderType("orderType2", request, null, false));
	}

	@ParameterizedTest
	@ValueSource(strings = {"orderType", "orderType2"})
	void oracleTextScoreRequiresMatchingContainsInCurrentQuery(String orderTypeName)
	{
		TestRequest request = new TestRequest();
		request.setParameter(orderTypeName, "sortPriority");

		assertEquals("d.sort_priority",
			SearchAction.getValidatedOrderType(orderTypeName, request, null, false));
		assertEquals("SCORE(10)",
			SearchAction.getValidatedOrderType(orderTypeName, request, null, true));
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
