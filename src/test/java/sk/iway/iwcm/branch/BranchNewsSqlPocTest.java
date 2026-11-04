package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.components.news.FieldEnum;
import sk.iway.iwcm.components.news.NewsQuery;
import sk.iway.iwcm.components.news.NewsQuery.OrderEnum;
import sk.iway.iwcm.components.news.NewsQuery.SortEnum;
import sk.iway.iwcm.components.news.criteria.DatabaseCriteria;
import sk.iway.iwcm.doc.DataAccessHelper;

/**
 * Executes the P01 effective-source experiment using the real NewsQuery predicate
 * builder and full document projection. No production query or migration is changed.
 * The CTE and SQL assembly below are a MariaDB-only test seam, not the final BranchSql API.
 */
@EnabledIfEnvironmentVariable(named = "WEBJET_P01_JDBC_URL", matches = ".+")
class BranchNewsSqlPocTest
{
    private static final String URL = "jdbc:mariadb://127.0.0.1:33316/webjet_p01";
    private static final String BODY = "<p>Žľťučký HTML &amp; čitateľný obsah</p>".repeat(7000);
    private Connection connection;
    private boolean originalJoin;
    private int originalDbType;

    /** Creates independent scratch copies of the CI schema, never live content tables. */
    @BeforeEach
    void prepare() throws Exception
    {
        originalJoin = Constants.getBoolean("perexGroupUseJoin");
        originalDbType = Constants.DB_TYPE;
        assertEquals(URL, System.getenv("WEBJET_P01_JDBC_URL"));
        connection = DriverManager.getConnection(URL,
            System.getenv().getOrDefault("WEBJET_P01_JDBC_USER", "root"),
            System.getenv().getOrDefault("WEBJET_P01_JDBC_PASSWORD", ""));
        Constants.DB_TYPE = Constants.DB_MYSQL;
        for (String suffix : List.of("live", "work", "atr", "atr_def", "perex"))
            execute("DROP TABLE IF EXISTS p01_news_" + suffix);
        execute("CREATE TABLE p01_news_live LIKE documents");
        execute("CREATE TABLE p01_news_work LIKE documents");
        execute("ALTER TABLE p01_news_work DROP PRIMARY KEY, MODIFY doc_id INT NOT NULL, "
            + "ADD branch_id BIGINT NOT NULL, ADD operation VARCHAR(8) NOT NULL DEFAULT 'UPSERT', "
            + "ADD choice VARCHAR(4) NOT NULL DEFAULT 'WORK', ADD PRIMARY KEY(branch_id,doc_id)");
        execute("CREATE TABLE p01_news_atr LIKE doc_atr");
        execute("CREATE TABLE p01_news_atr_def LIKE doc_atr_def");
        execute("CREATE TABLE p01_news_perex LIKE perex_group_doc");
        document(false, 1, "LIVE unchanged", 10, "1", 30);
        document(false, 2, "LIVE out of filter", 20, "2", 20);
        document(false, 3, "LIVE deleted", 10, "1", 10);
        document(false, 4, "LIVE moved out", 10, "1", 40);
        document(false, 5, "LIVE choice", 10, "1", 50);
        document(true, 2, "WORK changed into filter", 10, "1,2", 20);
        document(true, 3, "WORK deleted", 10, "1", 10);
        document(true, 4, "WORK moved out", 20, "1", 40);
        document(true, 5, "WORK ignored by LIVE choice", 20, "2", 50);
        document(true, 6, "WORK new", 10, "1,2", 20);
        execute("UPDATE p01_news_work SET operation='DELETE' WHERE doc_id=3");
        execute("UPDATE p01_news_work SET choice='LIVE' WHERE doc_id=5");
        execute("INSERT INTO p01_news_work SELECT " + workCopyColumns(99) + " FROM p01_news_work w WHERE branch_id=42 AND doc_id=6");
        execute("UPDATE p01_news_work SET title='OTHER BRANCH' WHERE branch_id=99");
        execute("INSERT INTO p01_news_atr_def (atr_id,atr_name) VALUES (1,'colour'),(2,'size')");
        execute("INSERT INTO p01_news_atr (doc_id,atr_id,value_string) VALUES (1,1,'red'),(1,2,'large'),(2,1,'red'),(2,2,'large')");
        // A normal perex join multiplies an unchanged article even without attributes.
        execute("INSERT INTO p01_news_perex (doc_id,perex_group_id) VALUES (1,1),(1,2),(2,1),(2,2)");
    }

    @AfterEach
    void close() throws Exception
    {
        Constants.setBoolean("perexGroupUseJoin", originalJoin);
        Constants.DB_TYPE = originalDbType;
        if (connection != null) connection.close();
    }

    /** Proves overlay selection before filtering, deterministic paging and full TEXT fidelity. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void effectiveSourceBeforeFiltersCountAndPaging(boolean perexJoin) throws Exception
    {
        Constants.setBoolean("perexGroupUseJoin", perexJoin);
        Plan list = plan(false, false, false, 0, 2);
        assertEquals(List.of(42L, 42L, 10, true, "1", "1,%", "%,1", "%,1,%"), list.arguments());
        assertEquals(List.of(2, 6), ids(list, true));
        assertEquals(List.of(1, 5), ids(plan(false, false, true, 2, 2), false));
        assertEquals(4, count(plan(true, false, true, 0, 2)));
        assertEquals(List.of(2, 6, 1, 5), ids(plan(false, false, true, 0, 20), false));
        assertEquals(List.of(3, 6, 1, 4, 5), ids(plan(false, false, true, 0, 20, 99), false));
        assertEquals(5, scalar("SELECT COUNT(*) FROM p01_news_live"));
        assertEquals("LIVE out of filter", text("SELECT title FROM p01_news_live WHERE doc_id=2"));
        assertEquals(1, scalar("SELECT COUNT(*) FROM p01_news_live WHERE doc_id=3"));
        assertEquals(0, scalar("SELECT COUNT(*) FROM p01_news_live WHERE doc_id=6"));
        System.out.println("P01 effective News source: perexGroupUseJoin=" + perexJoin
            + "; unique IDs [2,6,1,5], pages [2,6]/[1,5], count=4, full UTF-8 TEXT chars=" + BODY.length());
    }

    /** Keeps the existing OR predicate over LIVE attributes without multiplying documents. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void attributesDeduplicateIdsBeforePaging(boolean perexJoin) throws Exception
    {
        Constants.setBoolean("perexGroupUseJoin", perexJoin);
        assertEquals(List.of(2), ids(plan(false, true, false, 0, 1), true));
        assertEquals(List.of(1), ids(plan(false, true, true, 1, 1), false));
        assertEquals(2, count(plan(true, true, true, 0, 1)));
        assertEquals(List.of(2, 1), ids(plan(false, true, true, 0, 20), false));
        // Demonstrates that the fixture really exercises row multiplication in today's SQL.
        assertTrue(scalar("SELECT COUNT(*) FROM p01_news_live d JOIN p01_news_atr a ON a.doc_id=d.doc_id WHERE d.doc_id=1") > 1);
    }

    private Plan plan(boolean count, boolean attributes, boolean noData, int offset, int limit) throws Exception
    {
        return plan(count, attributes, noData, offset, limit, 42);
    }

    /** Reuses today's criteria builder; only the experiment's source and ID-set boundary differ. */
    @SuppressWarnings("unchecked")
    private Plan plan(boolean count, boolean attributes, boolean noData, int offset, int limit, long branch) throws Exception
    {
        NewsQuery query = new NewsQuery()
            .addCriteria(DatabaseCriteria.equal(FieldEnum.GROUP_ID, 10))
            .addCriteria(DatabaseCriteria.equal(FieldEnum.AVAILABLE, true))
            .addCriteria(DatabaseCriteria.tokenizedIds(FieldEnum.PEREX_GROUP, 1));
        if (attributes) query.addCriteria(DatabaseCriteria.or(
            DatabaseCriteria.Atribute.equal("colour", "red"), DatabaseCriteria.Atribute.equal("size", "large")));
        query.addOrder(OrderEnum.PRIORITY, SortEnum.ASC).addOrder(OrderEnum.ID, SortEnum.ASC);
        Method build = NewsQuery.class.getDeclaredMethod("getSelectSql", boolean.class, boolean.class);
        build.setAccessible(true);
        String generated = (String) build.invoke(query, false, noData);
        Field args = NewsQuery.class.getDeclaredField("arguments");
        args.setAccessible(true);
        int from = generated.indexOf(" FROM documents d ");
        int order = generated.indexOf("ORDER BY ");
        assertTrue(from > 0 && order > from, "Update the P01 seam if NewsQuery changes");
        String membership = "SELECT d.doc_id" + generated.substring(from, order)
            .replace(" FROM documents d ", " FROM p01_effective d ")
            .replace("LEFT JOIN perex_group_doc p ON d.doc_id = p.doc_id ", "")
            .replace("doc_atr da", "p01_news_atr da").replace("doc_atr_def dad", "p01_news_atr_def dad")
            + " GROUP BY d.doc_id";
        String projection = generated.substring(0, from)
            .replace(", dad.*, da.value_string, da.value_int, da.value_bool ", " ");
        String sql = effectiveSource() + (count ? "SELECT COUNT(*)" : projection)
            + " FROM p01_effective d LEFT JOIN users u ON u.user_id=d.author_id "
            + "WHERE d.doc_id IN (" + membership + ") "
            + (count ? "" : generated.substring(order) + " LIMIT " + limit + " OFFSET " + offset);
        List<Object> parameters = new ArrayList<>(List.of(branch, branch));
        parameters.addAll((List<Object>) args.get(query));
        return new Plan(sql, parameters);
    }

    private String effectiveSource()
    {
        Set<String> fields = new LinkedHashSet<>(Arrays.stream(FieldEnum.values())
            .map(f -> f.name().toLowerCase(java.util.Locale.ROOT)).toList());
        fields.addAll(Arrays.asList(DataAccessHelper.getDocFields()));
        Set<String> liveFields = Set.of("forum_count", "views_total", "sync_id", "sync_status", "publish_start", "publish_end");
        String live = fields.stream().map(f -> "l." + f).collect(Collectors.joining(","));
        String work = fields.stream().map(f -> liveFields.contains(f)
            ? "CASE WHEN l.doc_id IS NOT NULL THEN l." + f + " ELSE w." + f + " END AS " + f
            : "w." + f).collect(Collectors.joining(","));
        return "WITH p01_effective AS (SELECT " + live + " FROM p01_news_live l WHERE NOT EXISTS "
            + "(SELECT 1 FROM p01_news_work w WHERE w.branch_id=? AND w.doc_id=l.doc_id AND w.choice='WORK') "
            + "UNION ALL SELECT " + work + " FROM p01_news_work w LEFT JOIN p01_news_live l ON l.doc_id=w.doc_id "
            + "WHERE w.branch_id=? AND w.choice='WORK' AND w.operation='UPSERT') ";
    }

    private List<Integer> ids(Plan plan, boolean fullData) throws Exception
    {
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement statement = bind(plan); ResultSet result = statement.executeQuery())
        {
            while (result.next())
            {
                ids.add(result.getInt("doc_id"));
                if (fullData) assertEquals(BODY, result.getString("data"));
                else assertThrows(java.sql.SQLException.class, () -> result.getString("data"));
                for (String field : DataAccessHelper.getDocFields()) assertTrue(result.findColumn(field) > 0);
            }
        }
        assertEquals(ids.size(), new LinkedHashSet<>(ids).size());
        return ids;
    }

    private long count(Plan plan) throws Exception
    {
        try (PreparedStatement statement = bind(plan); ResultSet result = statement.executeQuery())
        {
            assertTrue(result.next());
            return result.getLong(1);
        }
    }

    private PreparedStatement bind(Plan plan) throws Exception
    {
        PreparedStatement statement = connection.prepareStatement(plan.sql());
        for (int i = 0; i < plan.arguments().size(); i++) statement.setObject(i + 1, plan.arguments().get(i));
        return statement;
    }

    private void document(boolean work, int id, String title, int group, String perex, int priority) throws Exception
    {
        String table = work ? "p01_news_work" : "p01_news_live";
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO " + table
            + " (doc_id,title,data,data_asc,navbar,group_id,perex_group,sort_priority,available"
            + (work ? ",branch_id" : "") + ") VALUES (?,?,?,?,?,?,?,?,true" + (work ? ",42" : "") + ")"))
        {
            int index = 1;
            for (Object value : List.of(id, title, BODY, "plain text", title, group, perex, priority)) statement.setObject(index++, value);
            statement.executeUpdate();
        }
    }

    private String workCopyColumns(long branch) throws Exception
    {
        List<String> columns = new ArrayList<>();
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SHOW COLUMNS FROM p01_news_work"))
        {
            while (result.next()) columns.add("branch_id".equals(result.getString(1)) ? Long.toString(branch) : "w." + result.getString(1));
        }
        return String.join(",", columns);
    }

    private void execute(String sql) throws Exception
    {
        try (Statement statement = connection.createStatement()) { statement.execute(sql); }
    }

    private long scalar(String sql) throws Exception
    {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql))
        { assertTrue(result.next()); return result.getLong(1); }
    }

    private String text(String sql) throws Exception
    {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql))
        { assertTrue(result.next()); return result.getString(1); }
    }

    private record Plan(String sql, List<Object> arguments) { }
}
