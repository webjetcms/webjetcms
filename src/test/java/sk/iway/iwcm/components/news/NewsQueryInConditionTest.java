package sk.iway.iwcm.components.news;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.components.news.criteria.DatabaseCriteria;

class NewsQueryInConditionTest
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
	@CsvSource({"false, false", "false, true", "true, false", "true, true"})
	void dataAndCountQueriesSplitMembershipAndKeepOtherFilters(boolean count, boolean exclusion)
	{
		NewsQuery news = new NewsQuery();
		List<Integer> ids = IntStream.rangeClosed(1, 1001).boxed().toList();
		news.addCriteria(DatabaseCriteria.equal(FieldEnum.TITLE, "Before"));
		news.addCriteria(exclusion ? DatabaseCriteria.notIn(FieldEnum.GROUP_ID, ids) : DatabaseCriteria.in(FieldEnum.GROUP_ID, ids));
		news.addCriteria(DatabaseCriteria.equal(FieldEnum.AVAILABLE, true));

		String sql = ReflectionTestUtils.invokeMethod(news, "getSelectSql", count, true);
		Matcher lists = Pattern.compile("d.group_id (?:NOT )?IN \\(([^)]+)\\)").matcher(sql);
		int chunks = 0;
		int parameters = 0;
		while (lists.find())
		{
			int size = lists.group(1).split(",").length;
			assertTrue(size <= 1000, "A news IN list must respect Oracle's limit");
			parameters += size;
			chunks++;
		}
		assertEquals(2, chunks);
		assertEquals(1001, parameters);
		assertTrue(sql.contains(exclusion ? ") AND d.group_id NOT IN (?)" : ") OR d.group_id IN (?)"));
		assertTrue(sql.contains("(?)) AND d.available"), "The following filter must apply to the complete membership predicate");
		List<Object> expected = new ArrayList<>();
		expected.add("Before");
		expected.addAll(ids);
		expected.add(true);
		assertEquals(expected, ReflectionTestUtils.getField(news, "arguments"));
	}
}
