package sk.iway.iwcm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class DBTest
{
	@ParameterizedTest
	@ValueSource(strings = {
		"title",
		"sort_priority",
		"_internal",
		"field123",
		"lastUpdate",
		"d.title",
		"table_alias.field_1"
	})
	void isValidSqlIdentifierAllowsSimpleAndQualifiedIdentifiers(String identifier)
	{
		assertTrue(DB.isValidSqlIdentifier(identifier));
	}

	@ParameterizedTest
	@NullSource
	@EmptySource
	@ValueSource(strings = {
		" ",
		"\t",
		"\n",
		"\r",
		"\f",
		"\u00A0",
		"title\u2003",
		"\u202Ftitle",
		"title DESC",
		"title ASC",
		"title--comment",
		"title/*comment*/",
		"title-name",
		"title$name",
		"title+1",
		"title/2",
		"(title)",
		"SCORE(10)",
		"title,doc_id",
		"title;SELECT",
		"'title'",
		"\"title\"",
		"`title`",
		"[title]",
		"d.*",
		".title",
		"title.",
		"d..title",
		"catalog.documents.title",
		"1title",
		"9.title"
	})
	void isValidSqlIdentifierRejectsSqlSyntaxAndMalformedIdentifiers(String identifier)
	{
		assertFalse(DB.isValidSqlIdentifier(identifier));
	}
}
