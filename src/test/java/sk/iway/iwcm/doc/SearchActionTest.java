package sk.iway.iwcm.doc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import sk.iway.iwcm.test.TestRequest;

class SearchActionTest
{
	@ParameterizedTest
	@ValueSource(strings = {
		"title",
		"sort_priority",
		"lastUpdate",
		"sortPriority",
		"publishStart",
		"saveDate",
		"field_a",
		"field123",
		"_internal",
		"d.title",
		"views_total",
		"root_group_l1"
	})
	void getValidatedOrderTypeAllowsSqlIdentifiers(String orderType)
	{
		TestRequest request = new TestRequest();
		request.setParameter("orderType", orderType);

		assertEquals(orderType, SearchAction.getValidatedOrderType("orderType", request, "sort_priority"));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"(SELECT password FROM users)",
		"title DESC",
		"title,doc_id",
		"title;SELECT",
		"title--comment",
		"title/**/DESC",
		"SCORE(10)",
		"d.*",
		"`title`",
		"[title]",
		"1",
		" title",
		"title ",
		"title\n"
	})
	void getValidatedOrderTypeRejectsSqlSyntax(String orderType)
	{
		TestRequest request = new TestRequest();
		request.setParameter("orderType", orderType);

		assertEquals("sort_priority", SearchAction.getValidatedOrderType("orderType", request, "sort_priority"));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"title.",
		".title",
		"d..title",
		"catalog.documents.title",
		"9title",
		"title-name",
		"title$name"
	})
	void getValidatedOrderTypeRejectsInvalidIdentifiers(String orderType)
	{
		TestRequest request = new TestRequest();
		request.setParameter("orderType2", orderType);

		assertNull(SearchAction.getValidatedOrderType("orderType2", request, null));
	}

	@ParameterizedTest
	@ValueSource(strings = {"title", "d.title"})
	void getValidatedOrderTypeAcceptsConfiguredAttribute(String orderType)
	{
		TestRequest request = new TestRequest();
		request.setAttribute("orderType", orderType);

		assertEquals(orderType, SearchAction.getValidatedOrderType("orderType", request, "sort_priority"));
	}

	@Test
	void invalidRequestParameterUsesSafeDefaultInsteadOfConfiguredAttribute()
	{
		TestRequest request = new TestRequest();
		request.setAttribute("orderType", "title");
		request.setParameter("orderType", "(SELECT WJMARK2 FROM dual)");

		assertEquals("sort_priority", SearchAction.getValidatedOrderType("orderType", request, "sort_priority"));
	}

	@Test
	void timeBasedInjectionInSecondaryOrderTypeIsSkipped()
	{
		TestRequest request = new TestRequest();
		String payload = "(SELECT CASE WHEN (SELECT SUBSTRING(password FROM 1 FOR 1) FROM users " +
			"WHERE login LIKE 0x61646d696e) LIKE 0x41 THEN SLEEP(3) ELSE 0 END)";
		request.setParameter("orderType2", payload);

		assertNull(SearchAction.getValidatedOrderType("orderType2", request, null));
	}

	@Test
	void missingAndEmptyOrderTypesUseSafeDefaults()
	{
		TestRequest missingRequest = new TestRequest();
		assertEquals("sort_priority", SearchAction.getValidatedOrderType("orderType", missingRequest, "sort_priority"));
		assertNull(SearchAction.getValidatedOrderType("orderType2", missingRequest, null));

		TestRequest emptyRequest = new TestRequest();
		emptyRequest.setParameter("orderType", "");
		emptyRequest.setParameter("orderType2", "");
		assertEquals("sort_priority", SearchAction.getValidatedOrderType("orderType", emptyRequest, "sort_priority"));
		assertNull(SearchAction.getValidatedOrderType("orderType2", emptyRequest, null));
	}
}
