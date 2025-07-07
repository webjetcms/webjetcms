package sk.iway.iwcm.update;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.mock.web.MockServletContext;

import ch.qos.logback.classic.Level;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.components.perex_groups.PerexGroupsEntity;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.system.ConstantsV9;

import java.io.File;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class DomainIdUpdateServiceTest {

    @BeforeAll
    public static void init()
    {
        Logger.setLevel(Level.ERROR);
        Logger.setLevel("sk.iway.basecms", Level.DEBUG);
        Logger.setLevel("sk.iway.iwcm.admin.upload", Level.TRACE);

        ConstantsV9.clearValuesWebJet9();

        Constants.setServletContext(new MockServletContext("Webjet"){
			@Override
			public String getRealPath(String path)
			{
				return new File(path).getAbsolutePath();
			}
		});
    }

    static Stream<TestCase> htmlVariantsProvider() {
        Map<String, PerexGroupsEntity> domainPerexGroupsMap = new HashMap<>();
        // perexGroupName: "groupA", domainId: 1 -> id: 100
        PerexGroupsEntity groupA1 = new PerexGroupsEntity();
        groupA1.setId(100L);
        groupA1.setPerexGroupName("groupA");
        domainPerexGroupsMap.put("groupA-1", groupA1);

        // perexGroupName: "groupA", domainId: 2 -> id: 200
        PerexGroupsEntity groupA2 = new PerexGroupsEntity();
        groupA2.setId(200L);
        groupA2.setPerexGroupName("groupA");
        domainPerexGroupsMap.put("groupA-2", groupA2);

        // perexGroupName: "groupB", domainId: 1 -> id: 101
        PerexGroupsEntity groupB1 = new PerexGroupsEntity();
        groupB1.setId(101L);
        groupB1.setPerexGroupName("groupB");
        domainPerexGroupsMap.put("groupB-1", groupB1);

        // perexGroupName: "groupB", domainId: 2 -> id: 201
        PerexGroupsEntity groupB2 = new PerexGroupsEntity();
        groupB2.setId(201L);
        groupB2.setPerexGroupName("groupB");
        domainPerexGroupsMap.put("groupB-2", groupB2);

        Map<String, Integer> domainsMap = new HashMap<>();
        domainsMap.put("domain1", 1);
        domainsMap.put("domain2", 2);

        return Stream.of(
            // 1. Only perexGroup
            new TestCase(
                "!INCLUDE(/components/news/news-velocity.jsp, groupIds=&quot;32&quot;, alsoSubGroups=&quot;false&quot;, publishType=&quot;new&quot;, order=&quot;date&quot;, ascending=&quot;false&quot;, paging=&quot;false&quot;, pageSize=&quot;10&quot;, offset=&quot;0&quot;, perexNotRequired=&quot;false&quot;, loadData=&quot;false&quot;, checkDuplicity=&quot;false&quot;, contextClasses=&quot;&quot;, cacheMinutes=&quot;0&quot;, template=&quot;news.template.novinky-2&quot;, perexGroup=&quot;100+101&quot;, perexGroupNot=&quot;&quot;)!",
                "domain2",
                domainsMap,
                domainPerexGroupsMap,
                "!INCLUDE(/components/news/news-velocity.jsp, groupIds=&quot;32&quot;, alsoSubGroups=&quot;false&quot;, publishType=&quot;new&quot;, order=&quot;date&quot;, ascending=&quot;false&quot;, paging=&quot;false&quot;, pageSize=&quot;10&quot;, offset=&quot;0&quot;, perexNotRequired=&quot;false&quot;, loadData=&quot;false&quot;, checkDuplicity=&quot;false&quot;, contextClasses=&quot;&quot;, cacheMinutes=&quot;0&quot;, template=&quot;news.template.novinky-2&quot;, perexGroup=&quot;200+201&quot;, perexGroupNot=&quot;&quot;)!"
            ),
            // 2. Only perexGroupNot
            new TestCase(
                "!INCLUDE(/components/news/news-velocity.jsp, perexGroupNot=&quot;100+101&quot;)!",
                "domain2",
                domainsMap,
                domainPerexGroupsMap,
                "!INCLUDE(/components/news/news-velocity.jsp, perexGroupNot=&quot;200+201&quot;)!"
            ),
            // 3. Both perexGroup and perexGroupNot
            new TestCase(
                "!INCLUDE(/components/news/news-velocity.jsp, perexGroup=&quot;100&quot;, perexGroupNot=&quot;101&quot;)!",
                "domain2",
                domainsMap,
                domainPerexGroupsMap,
                "!INCLUDE(/components/news/news-velocity.jsp, perexGroup=&quot;200&quot;, perexGroupNot=&quot;201&quot;)!"
            ),
            // 4. No perexGroup params (should remain unchanged)
            new TestCase(
                "!INCLUDE(/components/news/news-velocity.jsp, pageSize=&quot;10&quot;)!",
                "domain2",
                domainsMap,
                domainPerexGroupsMap,
                "!INCLUDE(/components/news/news-velocity.jsp, pageSize=&quot;10&quot;)!"
            ),
            // 5. Multiple INCLUDEs in one html
            new TestCase(
                "<div>!INCLUDE(/components/news/news-velocity.jsp, perexGroup=&quot;100&quot;)!</div><div>!INCLUDE(/components/news/news-velocity.jsp, perexGroupNot=&quot;101&quot;)!</div>",
                "domain2",
                domainsMap,
                domainPerexGroupsMap,
                "<div>!INCLUDE(/components/news/news-velocity.jsp, perexGroup=&quot;200&quot;)!</div><div>!INCLUDE(/components/news/news-velocity.jsp, perexGroupNot=&quot;201&quot;)!</div>"
            ),
            // domain1 - no change expected
            new TestCase(
                "!INCLUDE(/components/news/news-velocity.jsp, , perexGroupNot=&quot;&quot;, groupIds=&quot;32&quot;, alsoSubGroups=&quot;false&quot;, perexGroup=&quot;100+101&quot;)!",
                "domain1",
                domainsMap,
                domainPerexGroupsMap,
                "!INCLUDE(/components/news/news-velocity.jsp, , perexGroupNot=&quot;&quot;, groupIds=&quot;32&quot;, alsoSubGroups=&quot;false&quot;, perexGroup=&quot;100+101&quot;)!"
            ),
            //missing quot;
            new TestCase(
                "<div>!INCLUDE(/components/news/news-velocity.jsp, perexGroup=100)!</div><div>!INCLUDE(/components/news/news-velocity.jsp, perexGroupNot=101)!</div>",
                "domain2",
                domainsMap,
                domainPerexGroupsMap,
                "<div>!INCLUDE(/components/news/news-velocity.jsp, perexGroup=200)!</div><div>!INCLUDE(/components/news/news-velocity.jsp, perexGroupNot=201)!</div>"
            )
        );
    }

    @ParameterizedTest
    @MethodSource("htmlVariantsProvider")
    void testGetNewsDocToUpdateVariants(TestCase testCase) {

        Logger.setWJLogLevel("debug");
        Logger.debug("Testing with input HTML: " + testCase.inputHtml);

        // Prepare DocDetails mock
        DocDetails doc = Mockito.mock(DocDetails.class, Mockito.CALLS_REAL_METHODS);
        doc.setData(testCase.inputHtml);
        GroupDetails group = new GroupDetails();
        group.setDomainName(testCase.domainName);

        /*GroupDetails group = Mockito.mock(GroupDetails.class);
        Mockito.when(group.getDomainName()).thenReturn(testCase.domainName); */
        Mockito.when(doc.getGroup()).thenReturn(group);
        //Mockito.when(doc.getData()).thenReturn(testCase.inputHtml);
        List<DocDetails> docs = new ArrayList<>();
        docs.add(doc);

        Logger.debug(DomainIdUpdateServiceTest.class, "testing doc: " + doc.getData());

        // Call method under test
        List<DocDetails> result = DomainIdUpdateService.getNewsDocToUpdate(
            docs,
            testCase.domainsMap,
            testCase.domainPerexGroupsMap,
            null
        );

        Logger.debug("Result: " + (result.isEmpty() ? "empty" : result.get(0).getDataAsc()));

        if (testCase.inputHtml.equals(testCase.expectedHtml)) {
            // Should not be updated
            assertTrue(result.isEmpty() || result.get(0).getData().equals(testCase.expectedHtml));
        } else {
            Logger.debug(DomainIdUpdateServiceTest.class, "Original HTML:\n" + testCase.inputHtml);
            Logger.debug(DomainIdUpdateServiceTest.class, "Expected HTML:\n" + testCase.expectedHtml);
            Logger.debug(DomainIdUpdateServiceTest.class, "Result HTML:\n" + (result.isEmpty() ? "empty" : result.get(0).getData()));

            assertEquals(1, result.size());
            assertEquals(testCase.expectedHtml, result.get(0).getData());
        }
    }

    static class TestCase {
        final String inputHtml;
        final String domainName;
        final Map<String, Integer> domainsMap;
        final Map<String, PerexGroupsEntity> domainPerexGroupsMap;
        final String expectedHtml;

        TestCase(String inputHtml, String domainName, Map<String, Integer> domainsMap, Map<String, PerexGroupsEntity> domainPerexGroupsMap, String expectedHtml) {
            this.inputHtml = inputHtml;
            this.domainName = domainName;
            this.domainsMap = domainsMap;
            this.domainPerexGroupsMap = domainPerexGroupsMap;
            this.expectedHtml = expectedHtml;
        }
    }
}