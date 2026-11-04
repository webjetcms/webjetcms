package sk.iway.iwcm.system.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.eclipse.persistence.descriptors.RelationalDescriptor;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.internal.jpa.metamodel.MetamodelImpl;
import org.eclipse.persistence.internal.jpa.querydef.CriteriaBuilderImpl;
import org.eclipse.persistence.internal.jpa.querydef.CriteriaQueryImpl;
import org.eclipse.persistence.internal.sessions.DatabaseSessionImpl;
import org.eclipse.persistence.platform.database.OraclePlatform;
import org.eclipse.persistence.queries.ReadAllQuery;
import org.eclipse.persistence.queries.SQLCall;
import org.eclipse.persistence.sessions.DatabaseLogin;
import org.eclipse.persistence.sessions.DatabaseRecord;
import org.eclipse.persistence.sessions.Project;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import sk.iway.iwcm.Constants;

/** Generates real EclipseLink Oracle SQL without opening a database connection. */
class JpaToolsInConditionTest
{
	public static class Document
	{
		public int id;
		public int groupId;
	}

	private int originalDatabaseType;
	private int originalMaxItems;
	private DatabaseSessionImpl session;
	private CriteriaBuilder builder;
	private CriteriaQueryImpl<Document> criteria;
	private final ExpressionBuilder expressions = new ExpressionBuilder();
	private Path<Integer> group;
	private Path<Integer> id;

	@BeforeEach
	void setUp()
	{
		originalDatabaseType = Constants.DB_TYPE;
		originalMaxItems = Constants.getInt("dbInClauseMaxItems");
		Constants.DB_TYPE = Constants.DB_ORACLE;
		Constants.setInt("dbInClauseMaxItems", 1000);
		DatabaseLogin login = new DatabaseLogin(new OraclePlatform());
		login.setShouldBindAllParameters(true);
		Project project = new Project(login);
		RelationalDescriptor descriptor = new RelationalDescriptor();
		descriptor.setJavaClass(Document.class);
		descriptor.setAlias("Document");
		descriptor.setTableName("documents");
		descriptor.addPrimaryKeyFieldName("doc_id");
		descriptor.addDirectMapping("id", "doc_id");
		descriptor.addDirectMapping("groupId", "group_id");
		project.addDescriptor(descriptor);
		session = (DatabaseSessionImpl) project.createDatabaseSession();
		session.initializeDescriptors();
		MetamodelImpl metamodel = new MetamodelImpl(session);
		metamodel.initialize(Document.class.getClassLoader());
		builder = new CriteriaBuilderImpl(metamodel);
		criteria = (CriteriaQueryImpl<Document>) builder.createQuery(Document.class);
		Root<Document> root = criteria.from(Document.class);
		criteria.select(root);
		group = root.get("groupId");
		id = root.get("id");
	}

	@AfterEach
	void tearDown()
	{
		Constants.DB_TYPE = originalDatabaseType;
		Constants.setInt("dbInClauseMaxItems", originalMaxItems);
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 999, 1000, 1001, 2000, 2001})
	void oracleSqlSplitsPositiveAndNegativePredicates(int size)
	{
		List<Integer> ids = IntStream.rangeClosed(1, size).boxed().toList();
		for (boolean exclusion : new boolean[] {false, true})
		{
			Predicate predicate = exclusion ? JpaTools.notIn(group, ids, builder) : JpaTools.in(group, ids, builder);
			SQLCall call = sql(predicate);
			assertChunks(call, size, (size + 999) / 1000, 1000);
			assertEquals(ids, call.getParameters());
			assertEquals(exclusion, call.getSQLString().contains("NOT"));
		}
	}

	@Test
	void configuredLimitIsSharedAndOracleCapIsPreserved()
	{
		List<Integer> ids = IntStream.rangeClosed(1, 1001).boxed().toList();
		Constants.setInt("dbInClauseMaxItems", 5000);
		assertChunks(sql(JpaTools.in(group, ids, builder)), 1001, 2, 1000);
		Constants.setInt("dbInClauseMaxItems", 2);
		assertChunks(sql(JpaTools.in(group, List.of(1, 2, 3), builder)), 3, 2, 2);
	}

	@Test
	void emptySetsHaveFalsePositiveAndTrueNegativeSemantics()
	{
		Predicate empty = JpaTools.in(group, List.of(), builder);
		SQLCall positive = sql(builder.and(empty, builder.equal(id, 42)));
		assertTrue(positive.getSQLString().contains("WHERE (? = ?)"));
		assertEquals(List.of(1, 0), positive.getParameters());
		SQLCall negative = sql(JpaTools.notIn(group, List.of(), builder));
		assertFalse(negative.getSQLString().contains("WHERE"));
		SQLCall alternative = sql(builder.or(empty, builder.equal(id, 42)));
		assertTrue(alternative.getSQLString().contains("WHERE (doc_id = ?)"));
		assertEquals(List.of(42), alternative.getParameters());
	}

	@Test
	void nullItemsAreNotSilentlyDiscarded()
	{
		Constants.setInt("dbInClauseMaxItems", 2);
		List<Integer> values = Arrays.asList(1, null, 3);
		assertEquals(values, sql(JpaTools.notIn(group, values, builder)).getParameters());
	}

	@Test
	void blogCanReplaceAChunkedGroupFilterWithoutRemovingMixedPredicates()
	{
		Predicate groupFilter = JpaTools.in(group, IntStream.rangeClosed(1, 1001).boxed().toList(), builder);
		Predicate idFilter = builder.equal(id, 42);
		Predicate mixed = builder.or(groupFilter, idFilter);
		Predicate trashExclusion = JpaTools.notIn(group, List.of(7, 9), builder);
		Predicate groupRange = builder.and(builder.greaterThan(group, 1), builder.lessThan(group, 100));
		List<Predicate> predicates = new ArrayList<>(List.of(groupFilter, idFilter, mixed, trashExclusion, groupRange));
		JpaTools.removePredicateWithName("groupId", predicates);
		assertEquals(List.of(idFilter, mixed, trashExclusion, groupRange), predicates);
	}

	@Test
	void eclipseLinkAlreadySplitsBoundRepositoryCollections()
	{
		List<Integer> ids = IntStream.rangeClosed(1, 1001).boxed().toList();
		ReadAllQuery query = new ReadAllQuery(Document.class, expressions.get("groupId").in(expressions.getParameter("ids")));
		query.addArgument("ids", List.class);
		DatabaseRecord row = new DatabaseRecord();
		row.put("ids", ids);
		query.prepareCall(session, row);
		SQLCall call = (SQLCall) query.getCall().clone();
		call.translate(row, null, session);
		assertChunks(call, 1001, 2, 1000);
	}

	private SQLCall sql(Predicate predicate)
	{
		criteria.where(predicate);
		ReadAllQuery query = (ReadAllQuery) criteria.translate();
		DatabaseRecord row = new DatabaseRecord();
		query.prepareCall(session, row);
		SQLCall call = (SQLCall) query.getCall().clone();
		call.translate(row, null, session);
		return call;
	}

	private static void assertChunks(SQLCall call, int values, int expectedChunks, int maxItems)
	{
		Matcher lists = Pattern.compile(" IN \\(([^)]+)\\)").matcher(call.getSQLString());
		int chunks = 0;
		while (lists.find())
		{
			assertTrue(lists.group(1).split(",").length <= maxItems);
			chunks++;
		}
		assertEquals(expectedChunks, chunks);
		assertEquals(values, call.getParameters().size());
		if (expectedChunks > 1) assertTrue(call.getSQLString().contains(" OR "));
	}
}
