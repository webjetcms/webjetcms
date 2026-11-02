package sk.iway.iwcm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class DBInConditionTest
{
	private int originalDatabaseType;
	private int originalMaxItems;

	@BeforeEach
	void setUp()
	{
		originalDatabaseType = Constants.DB_TYPE;
		originalMaxItems = Constants.getInt("dbInClauseMaxItems");
		Constants.DB_TYPE = Constants.DB_ORACLE;
		Constants.setInt("dbInClauseMaxItems", 1000);
	}

	@AfterEach
	void tearDown()
	{
		Constants.DB_TYPE = originalDatabaseType;
		Constants.setInt("dbInClauseMaxItems", originalMaxItems);
	}

	@ParameterizedTest
	@CsvSource({"1, 1", "999, 1", "1000, 1", "1001, 2", "2000, 2", "2001, 3"})
	void splitsAtTheLimitWithoutLosingIds(int count, int expectedChunks)
	{
		assertChunks(DB.getInCondition("d.history_id", ids(count)), count, expectedChunks, 1000);
	}

	@ParameterizedTest
	@ValueSource(ints = {Constants.DB_ORACLE, Constants.DB_MSSQL, Constants.DB_MYSQL, Constants.DB_PGSQL})
	void usesConfiguredLimitOnEveryDatabase(int databaseType)
	{
		Constants.DB_TYPE = databaseType;
		Constants.setInt("dbInClauseMaxItems", 2);
		assertEquals("(history_id IN (1,2) OR history_id IN (3,4) OR history_id IN (5))",
			DB.getInCondition("history_id", "1,2,3,4,5"));
	}

	@ParameterizedTest
	@ValueSource(ints = {0, -1})
	void invalidConfiguredLimitUsesDefault(int limit)
	{
		Constants.DB_TYPE = Constants.DB_MYSQL;
		Constants.setInt("dbInClauseMaxItems", limit);
		assertChunks(DB.getInCondition("d.history_id", ids(1001)), 1001, 2, 1000);
	}

	@Test
	void oracleCapsConfiguredLimitAtOneThousand()
	{
		Constants.setInt("dbInClauseMaxItems", 5000);
		assertChunks(DB.getInCondition("d.history_id", ids(2001)), 2001, 3, 1000);
	}

	@ParameterizedTest
	@ValueSource(ints = {Constants.DB_MSSQL, Constants.DB_MYSQL, Constants.DB_PGSQL})
	void otherDatabasesAllowALargerConfiguredLimit(int databaseType)
	{
		Constants.DB_TYPE = databaseType;
		Constants.setInt("dbInClauseMaxItems", 2000);
		assertChunks(DB.getInCondition("d.history_id", ids(2001)), 2001, 2, 2000);
	}

	@ParameterizedTest
	@NullSource
	@EmptySource
	@ValueSource(strings = {" ", "\t\n"})
	void emptyInputMatchesNoRows(String values)
	{
		assertEquals("(1=0)", DB.getInCondition("history_id", values));
	}

	@Test
	void normalizesWhitespaceAndSignedLongIdsWithoutRemovingDuplicates()
	{
		assertEquals("(d.history_id IN (-9223372036854775808,0,7,7,9223372036854775807))",
			DB.getInCondition("d.history_id", " -9223372036854775808, 0, +007, 7, 9223372036854775807\t"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"1,,2", "1,", ",1", "1, ,2", "1,abc", "1,2.5", "1,1e3", "1,NULL", "'1'", "?", "SELECT 1", "1) OR 1=1--", "9223372036854775808"})
	void rejectsMalformedListsAndSqlExpressions(String values)
	{
		assertThrows(IllegalArgumentException.class, () -> DB.getInCondition("history_id", values));
	}

	@ParameterizedTest
	@NullSource
	@EmptySource
	@ValueSource(strings = {" ", "d.history_id DESC", "d.history_id) OR 1=1--", "d..history_id", "COALESCE(history_id,0)"})
	void rejectsInvalidColumnSyntax(String columnName)
	{
		assertThrows(IllegalArgumentException.class, () -> DB.getInCondition(columnName, "1,2"));
	}

	private static String ids(int count)
	{
		return IntStream.rangeClosed(1, count).mapToObj(Integer::toString).collect(Collectors.joining(","));
	}

	private static void assertChunks(String condition, int count, int expectedChunks, int maxItems)
	{
		assertTrue(condition.startsWith("(d.history_id IN ("));
		assertTrue(condition.endsWith("))"));
		Matcher matcher = Pattern.compile("d\\.history_id IN \\(([0-9,]+)\\)").matcher(condition);
		List<String> actualIds = new ArrayList<>();
		int chunks = 0;
		while (matcher.find())
		{
			String[] chunk = matcher.group(1).split(",");
			assertTrue(chunk.length <= maxItems, "Every IN list must respect the configured limit");
			actualIds.addAll(Arrays.asList(chunk));
			chunks++;
		}
		assertEquals(expectedChunks, chunks);
		assertEquals(expectedChunks - 1, condition.split(" OR ", -1).length - 1);
		assertEquals(ids(count), String.join(",", actualIds), "Splitting must preserve every ID in order");
	}
}
