package sk.iway.iwcm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
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
		"lastUpdate"
	})
	void isValidSqlIdentifierAllowsSimpleIdentifiers(String identifier)
	{
		assertTrue(DB.isValidColumnName(identifier, false));
		assertTrue(DB.isValidColumnName(identifier, true));
	}

	@ParameterizedTest
	@ValueSource(strings = {"d.title", "table_alias.field_1", "DBMS_RANDOM.VALUE"})
	void isValidSqlIdentifierAllowsQualifiedIdentifiersOnlyWhenEnabled(String identifier)
	{
		assertFalse(DB.isValidColumnName(identifier, false));
		assertTrue(DB.isValidColumnName(identifier, true));
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
		assertFalse(DB.isValidColumnName(identifier, false));
		assertFalse(DB.isValidColumnName(identifier, true));
	}

	@Test
	void resolveSqlColumnReferenceBindsColumnsToTrustedQualifiers()
	{
		assertEquals("d.title", DB.fixUntrustedColumnName("title", "d"));
		assertEquals("d.title", DB.fixUntrustedColumnName("d.title", "d"));
		assertEquals("d.title", DB.fixUntrustedColumnName("D.title", "d"));
		assertEquals("d.WJINJECTMARKER99", DB.fixUntrustedColumnName("WJINJECTMARKER99", "d"));
		assertEquals("d.SYSDATE", DB.fixUntrustedColumnName("SYSDATE", "d"));
		assertEquals("i.question_id", DB.fixUntrustedColumnName("i.question_id", "ia", "i"));
	}

	@Test
	void resolveSqlColumnReferenceRejectsQualifierNotPresentInQuery()
	{
		assertNull(DB.fixUntrustedColumnName("fa.virtual_file_name", "file_archiv"));
	}

	@ParameterizedTest
	@NullSource
	@EmptySource
	@ValueSource(strings = {
		"DBMS_RANDOM.VALUE",
		"unknown.title",
		"d.title.extra",
		"title DESC"
	})
	void resolveSqlColumnReferenceRejectsUnknownQualifiersAndSqlExpressions(String identifier)
	{
		assertNull(DB.fixUntrustedColumnName(identifier, "d"));
	}
}
