package sk.iway.iwcm.components.news;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.blog.rest.BloggerService;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.LabelValue;

/** Covers News folder discovery when Blog pages use the News component. */
class NewsRestControllerTest {

    /** Excludes blogger roots and nested sections while retaining ordinary News folders. */
    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void automaticDiscoveryExcludesBlogFolders(boolean hasBloggers) {
        GroupDetails news = folder(10, "/News");
        GroupDetails blogRoot = folder(30, "/Blog/blogger");
        GroupDetails section = folder(31, "/Blog/blogger/Section");
        GroupDetails nestedSection = folder(32, "/Blog/blogger/Section/Nested");
        Map<Integer, GroupDetails> folders = Map.of(10, news, 30, blogRoot, 31, section, 32, nestedSection);
        GroupsDB groupsDB = mock(GroupsDB.class);
        Prop prop = mock(Prop.class);
        when(groupsDB.getGroup(anyInt())).thenAnswer(call -> folders.get(call.getArgument(0)));
        when(groupsDB.getGroupsTree(30, true, true)).thenReturn(List.of(blogRoot, section, nestedSection));
        when(prop.getText("config.trash_dir")).thenReturn("/Trash");

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<DocDB> docs = mockStatic(DocDB.class);
                MockedStatic<GroupsDB> groups = mockStatic(GroupsDB.class);
                MockedStatic<Prop> props = mockStatic(Prop.class);
                MockedStatic<CloudToolsForCore> cloud = mockStatic(CloudToolsForCore.class);
                MockedConstruction<SimpleQuery> queries = mockConstruction(SimpleQuery.class, (query, context) -> {
                    when(query.forListInteger(anyString())).thenReturn(hasBloggers ? List.of(321) : List.of());
                    when(query.forListString(anyString())).thenAnswer(call ->
                            ((String) call.getArgument(0)).startsWith("SELECT editable_groups") ? List.of("30") : List.of(
                                    "!INCLUDE(/components/news/news-velocity.jsp, groupIds=10+30, expandGroupIds=true)!",
                                    "!INCLUDE(/components/news/news-velocity.jsp, groupIds=31)!"
                                            + "!INCLUDE(/components/news/news-velocity.jsp, groupIds=32)!"));
                })) {
            constants.when(() -> Constants.getString("newsAdminGroupIds", "")).thenReturn("");
            constants.when(() -> Constants.getString("defaultLanguage")).thenReturn("en");
            docs.when(() -> DocDB.getDomain(any(HttpServletRequest.class))).thenReturn("news.example");
            groups.when(GroupsDB::getInstance).thenReturn(groupsDB);
            props.when(() -> Prop.getInstance("en")).thenReturn(prop);
            cloud.when(() -> CloudToolsForCore.getDomainIdSqlWhere(true)).thenReturn("");

            List<LabelValue> result = NewsRestController.convertIdsToNamePair(
                    "constant:newsAdminGroupIds", null, mock(HttpServletRequest.class));

            assertEquals(hasBloggers ? List.of("10*") : List.of("30*", "31", "32", "10*"),
                    result.stream().map(LabelValue::getValue).toList());
        }
    }

    /** Keeps explicitly configured folders available to integrations that use the shared converter. */
    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void retainsExplicitFolderSelection(boolean useInclude) {
        GroupsDB groupsDB = mock(GroupsDB.class);
        when(groupsDB.getGroup(30)).thenReturn(folder(30, "/Blog/blogger"));
        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<DocDB> docs = mockStatic(DocDB.class);
                MockedStatic<GroupsDB> groups = mockStatic(GroupsDB.class);
                MockedStatic<BloggerService> bloggers = mockStatic(BloggerService.class)) {
            constants.when(() -> Constants.getString("newsAdminGroupIds", "")).thenReturn("30*");
            groups.when(GroupsDB::getInstance).thenReturn(groupsDB);
            String include = useInclude ? "!INCLUDE(/components/news/news-velocity.jsp, groupIds=30, alsoSubGroups=true)!" : null;

            List<LabelValue> result = NewsRestController.convertIdsToNamePair(
                    "constant:newsAdminGroupIds", include, mock(HttpServletRequest.class));

            assertEquals(List.of("30*"), result.stream().map(LabelValue::getValue).toList());
            bloggers.verifyNoInteractions();
        }
    }

    private static GroupDetails folder(int id, String path) {
        GroupDetails group = new GroupDetails();
        group.setGroupId(id);
        group.setFullPath(path);
        return group;
    }
}
